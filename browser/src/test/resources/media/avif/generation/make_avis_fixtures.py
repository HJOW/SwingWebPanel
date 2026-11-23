# -*- coding: utf-8 -*-
"""Makes the animated AVIF fixtures of tests/resources/media/avif/avis (plan item P18-03; FEATURE_MATRIX IMG-09).

Every file is made from an AV1 sequence of tests/resources/media/avif/seq (the sequences that are checked bit for bit against libdav1d) by
the AVIF muxer of ffmpeg (stream copy, so the AV1 data is not changed) and, for some files, by a small byte patch of the container.
The file avis/index.txt records what a decoder must find: the number of frames, the size, the duration of every frame in milliseconds and the
number of plays; these values are read from the container by this script (not by the Java decoder under test).

Usage (from the repository root):  python tests/resources/media/avif/generation/make_avis_fixtures.py
Needs ffmpeg on the PATH.  Not part of the product or the build.

animated AVIF 시험 자료를 만든다. 각 파일은 libdav1d 와 비트 단위로 비교한 seq 의 AV1 시퀀스를 ffmpeg 의 AVIF 먹서로 (스트림 복사) 감싼 것이며
일부는 컨테이너의 바이트를 조금 고친다. avis/index.txt 에는 디코더가 찾아야 할 값(프레임 수, 크기, 프레임별 지속 시간(밀리초), 재생 횟수)이
들어 있으며 이 값은 시험 대상인 Java 디코더가 아니라 이 스크립트가 컨테이너에서 읽는다.
"""
import os
import struct
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', '..'))
SEQ = os.path.join(ROOT, 'tests', 'resources', 'media', 'avif', 'seq')
OUT = os.path.join(ROOT, 'tests', 'resources', 'media', 'avif', 'avis')


def mux(name, color, framerate, alpha=None, loop=0):
    """Wraps the OBU sequences into an animated AVIF with ffmpeg and returns the bytes."""
    path = os.path.join(OUT, name + '.avif')
    cmd = ['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-framerate', str(framerate), '-f', 'obu', '-i', os.path.join(SEQ, color + '.obu')]
    maps = ['-map', '0']
    if alpha:
        cmd += ['-framerate', str(framerate), '-f', 'obu', '-i', os.path.join(SEQ, alpha + '.obu')]
        maps += ['-map', '1']
    cmd += maps + ['-c', 'copy', '-loop', str(loop), '-f', 'avif', path]
    subprocess.check_call(cmd)
    return bytearray(open(path, 'rb').read())


def boxes(d, s, e):
    """Yields (type, start, payload start, end) of the boxes between two positions."""
    p = s
    while p + 8 <= e:
        size, = struct.unpack('>I', d[p:p + 4])
        t = bytes(d[p + 4:p + 8]).decode('latin1')
        h = 8
        if size == 1:
            size, = struct.unpack('>Q', d[p + 8:p + 16])
            h = 16
        elif size == 0:
            size = e - p
        yield t, p, p + h, p + size
        p += size


def find(d, s, e, t):
    for bt, bs, bp, be in boxes(d, s, e):
        if bt == t:
            return bs, bp, be
    return None


def track_info(d):
    """Reads the first track of the movie box: durations, time scale, edit list. Returns a dict."""
    moov = find(d, 0, len(d), 'moov')
    mvhd = find(d, moov[1], moov[2], 'mvhd')
    version = d[mvhd[1]]
    movie_ts, = struct.unpack('>I', d[mvhd[1] + (20 if version == 1 else 12):][:4])
    res = {'movie_ts': movie_ts, 'tracks': []}
    for bt, bs, bp, be in boxes(d, moov[1], moov[2]):
        if bt != 'trak':
            continue
        info = {}
        edts = find(d, bp, be, 'edts')
        if edts:
            elst = find(d, edts[1], edts[2], 'elst')
            v = d[elst[1]]
            flags, = struct.unpack('>I', bytes(d[elst[1]:elst[1] + 4]))
            info['repeat'] = (flags & 1) != 0
            info['elst_pos'] = elst[1]
            info['edit'] = struct.unpack('>Q', d[elst[1] + 8:elst[1] + 16])[0] if v == 1 else struct.unpack('>I', d[elst[1] + 8:elst[1] + 12])[0]
            info['elst_v'] = v
        mdia = find(d, bp, be, 'mdia')
        mdhd = find(d, mdia[1], mdia[2], 'mdhd')
        v = d[mdhd[1]]
        info['ts'], = struct.unpack('>I', d[mdhd[1] + (20 if v == 1 else 12):][:4])
        hdlr = find(d, mdia[1], mdia[2], 'hdlr')
        info['handler'] = bytes(d[hdlr[1] + 8:hdlr[1] + 12]).decode('latin1')
        minf = find(d, mdia[1], mdia[2], 'minf')
        stbl = find(d, minf[1], minf[2], 'stbl')
        stts = find(d, stbl[1], stbl[2], 'stts')
        n, = struct.unpack('>I', d[stts[1] + 4:stts[1] + 8])
        durs = []
        for i in range(n):
            cnt, delta = struct.unpack('>II', d[stts[1] + 8 + 8 * i:stts[1] + 16 + 8 * i])
            durs += [delta] * cnt
        info['durations'] = durs
        res['tracks'].append(info)
    return res


def expected(d):
    """The values a decoder must find: frame delays (ms) and the number of plays (0 = forever)."""
    t = track_info(d)
    c = t['tracks'][0]
    delays = [int((x * 1000 + c['ts'] // 2) // c['ts']) for x in c['durations']]
    if c['repeat']:
        plays = 0
    else:
        total = float(sum(c['durations'])) / c['ts']
        plays = max(1, int(round((float(c['edit']) / t['movie_ts']) / total)))
    return delays, plays


def seq_info(name):
    """Looks up a sequence in seq/index.txt: (frames, width, height)."""
    for line in open(os.path.join(SEQ, 'index.txt')).read().splitlines():
        if line.startswith(name + '|'):
            f = line.split('|')
            w, h = f[6].split(',')[0].split(':')[0].split('x')
            return int(f[5]), int(w), int(h)
    raise Exception(name)


def main():
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    rows = []

    def add(name, data, color, alpha, note):
        open(os.path.join(OUT, name + '.avif'), 'wb').write(bytes(data))
        frames, w, h = seq_info(color)
        delays, plays = expected(data)
        assert len(delays) == frames, (name, len(delays), frames)
        rows.append('|'.join([name, color, alpha or '-', str(frames), '%dx%d' % (w, h), ','.join(str(x) for x in delays), str(plays), note]))

    add('avis_default', mux('avis_default', 'seq_default', 25), 'seq_default', None, 'plain sequence at 25 fps, repeat forever')
    add('avis_altref', mux('avis_altref', 'seq_altref', 12), 'seq_altref', None, 'hidden alternative reference frames and show_existing_frame')
    add('avis_tiles', mux('avis_tiles', 'seq_tiles', 8), 'seq_tiles', None, 'several tiles per frame')
    add('avis_odd', mux('avis_odd', 'seq_odd', 10), 'seq_odd', None, 'odd frame size 130x76')
    add('avis_10bit', mux('avis_10bit', 'seq_10bit', 10), 'seq_10bit', None, '10 bit')
    add('avis_grain', mux('avis_grain', 'seq_grain', 10), 'seq_grain', None, 'film grain')
    add('avis_alpha', mux('avis_alpha', 'seq_444', 15, 'seq_gray'), 'seq_444', 'seq_gray', '4:4:4 colour track with a monochrome alpha track')
    once = mux('avis_once', 'seq_10bit', 10, loop=1)
    add('avis_once', once, 'seq_10bit', None, 'edit list without the repeat flag: one play')
    # 편집 목록의 반복 플래그를 끄고 지속 시간을 3배로 고쳐 세 번 재생하는 파일을 만든다(길이가 같은 바이트 치환).
    plays3 = bytearray(once)
    t = track_info(plays3)['tracks'][0]
    pos = t['elst_pos']
    assert t['elst_v'] == 0
    plays3[pos + 8:pos + 12] = struct.pack('>I', t['edit'] * 3)
    add('avis_plays3', plays3, 'seq_10bit', None, 'edit list of three times the duration of the samples: three plays')
    # meta 상자를 free 로 바꾸어 기본 항목이 없는 파일을 만든다(같은 길이).
    nometa = bytearray(mux('avis_nometa', 'seq_basic8', 20))
    for bt, bs, bp, be in boxes(nometa, 0, len(nometa)):
        if bt == 'meta':
            nometa[bs + 4:bs + 8] = b'free'
    add('avis_nometa', nometa, 'seq_basic8', None, 'no meta box: the file has only the track')
    # 크기가 바뀌는 시퀀스: 시퀀스는 지원하지 않으므로 첫 프레임인 기본 항목이 정지 이미지로 나와야 한다.
    try:
        varsize = mux('avis_varsize', 'seq_resize_fixed', 10)
        add('avis_varsize', varsize, 'seq_resize_fixed', None, 'frames of different sizes: the decoder falls back to the primary item')
    except Exception as e:
        print('varsize not made:', e)
    open(os.path.join(OUT, 'index.txt'), 'w').write('# name|colour sequence|alpha sequence|frames|WxH|delay of every frame (ms)|plays (0 = forever)|description\n' + '\n'.join(rows) + '\n')
    print('\n'.join(rows))


main()
