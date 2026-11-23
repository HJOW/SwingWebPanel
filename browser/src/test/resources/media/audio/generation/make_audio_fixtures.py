#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Makes the audio test inputs of tests/resources/media/audio (plan item P18-11): small files that real encoders (ffmpeg with libmp3lame, the native
AAC encoder, libvorbis, libopus, flac and the PCM/ADPCM encoders) wrote from synthetic signals, and the 16-bit PCM that ffmpeg's own decoders produce
from them (the reference).

usage: python make_audio_fixtures.py <ffmpeg executable> <directory tests/resources/media/audio> [name,name,...]

Needs Python 3 and an ffmpeg with the encoders named above (the product code and the tests need none of it: the encoded files, the reference PCM and
the index are committed).  Only synthetic signals are used (sweeps, noise bursts, silence, near full scale tones, a very quiet tone and clicks), so
no copyrighted audio is in the repository.

index.txt, one line per fixture, fields separated by '|':
  name | path of the file below the audio directory | mime type | group | kind | sample rate | channels | layout | frames | expected | reference | note
  kind       exact   the decoded 16-bit PCM has to be bit for bit the same as ffmpeg's; reference = md5:<md5 of the s16le bytes>.  Integer samples wider than
                     16 bits (24 and 32 bit PCM, 24 bit FLAC) are narrowed by ffmpeg's resampler by truncation, the product rounds half up (documented policy);
                     the reference is then computed from ffmpeg's full precision (s32le) decoding with that rounding.  (with more than two
                     channels, which the product mixes down, the reference is the FLAC form like the one of 'lossy' and the downmix is compared)
             lossy   the decoded PCM has to be within the tolerance of the group; reference = flac:<FLAC file(s) of the s16le PCM>: one file with all
                     channels (one or two channels) or one mono file per channel, joined with '+' (more channels, which the product mixes down to stereo)
             refuse  ffmpeg can decode it, the product does not support it (the test expects an UNSUPPORTED_FORMAT error); reference = md5:..
  layout     the speaker roles of the channels of the reference, in order (ffmpeg's channel order), separated by ','
  frames     the number of sample frames of the reference (what ffmpeg's decoder outputs)
  expected   the number of sample frames that the product has to output: the length of the source signal (scaled to the output rate) for the
             containers that say where the signal starts and ends (LAME tag of MP3, edit list of MP4, granule positions of Ogg, pre-skip and end trimming
             of Opus), else the number of frames of ffmpeg.  Where ffmpeg's number differs, only the common part is compared.
  note       how the file was made and what the stream contains (coding tools seen in the stream where the script can read them)
The names given as the third argument are made again; the other lines of the index are kept.
"""
import array
import gzip
import hashlib
import math
import os
import re
import struct
import subprocess
import sys

FFMPEG = sys.argv[1]
FFPROBE = FFMPEG.replace('ffmpeg', 'ffprobe')
OUT = sys.argv[2]
TMP = os.path.join(OUT, 'generation', 'tmp')
ONLY = sys.argv[3].split(',') if len(sys.argv) > 3 else None
os.makedirs(TMP, exist_ok=True)

MIMES = {'mp3': 'audio/mpeg', 'aac': 'audio/aac', 'm4a': 'audio/mp4', 'ogg': 'audio/ogg', 'oga': 'audio/ogg', 'flac': 'audio/flac', 'opus': 'audio/ogg',
         'wav': 'audio/wav', 'aiff': 'audio/aiff', 'aifc': 'audio/aiff', 'mp2': 'audio/mpeg', 'au': 'audio/basic', 'webm': 'audio/webm', 'mka': 'audio/webm'}

# speaker roles of ffmpeg's channel layouts, in ffmpeg's channel order (FL FR FC LFE BL BR FLC FRC BC SL SR ...)
ROLES = {'mono': 'FC', 'stereo': 'FL,FR', '2.1': 'FL,FR,LFE', '3.0': 'FL,FR,FC', '3.0(back)': 'FL,FR,BC', '4.0': 'FL,FR,FC,BC', 'quad': 'FL,FR,BL,BR',
         'quad(side)': 'FL,FR,SL,SR', '3.1': 'FL,FR,FC,LFE', '5.0': 'FL,FR,FC,BL,BR', '5.0(side)': 'FL,FR,FC,SL,SR', '4.1': 'FL,FR,FC,LFE,BC',
         '5.1': 'FL,FR,FC,LFE,BL,BR', '5.1(side)': 'FL,FR,FC,LFE,SL,SR', '6.0': 'FL,FR,FC,BC,SL,SR', '6.0(front)': 'FL,FR,FLC,FRC,SL,SR',
         'hexagonal': 'FL,FR,FC,BL,BR,BC', '6.1': 'FL,FR,FC,LFE,BC,SL,SR', '6.1(back)': 'FL,FR,FC,LFE,BL,BR,BC', '7.0': 'FL,FR,FC,BL,BR,SL,SR',
         '7.0(front)': 'FL,FR,FC,FLC,FRC,SL,SR', '7.1': 'FL,FR,FC,LFE,BL,BR,SL,SR', '7.1(wide)': 'FL,FR,FC,LFE,BL,BR,FLC,FRC',
         '7.1(wide-side)': 'FL,FR,FC,LFE,FLC,FRC,SL,SR'}


def run(args, **kw):
    r = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kw)
    if r.returncode != 0:
        raise RuntimeError('command failed: %s\n%s' % (' '.join(args), r.stderr.decode('utf-8', 'replace')[-1500:]))
    return r


# ---------------------------------------------------------------------------------------------------------------------------------------
# the synthetic signals
# ---------------------------------------------------------------------------------------------------------------------------------------
def lcg(seed):
    """Deterministic white noise in [-1, 1) (a linear congruential generator, so the signal is the same on every machine)."""
    s = seed & 0xFFFFFFFF
    while True:
        s = (s * 1664525 + 1013904223) & 0xFFFFFFFF
        yield s / 2147483648.0 - 1.0


def gap_channel(rate, n, c):
    """A short tone, a long digital silence and then a loud burst (noise and a tone): an encoder spends few bits on the silence and fills the bit reservoir,
    so the burst starts with large main_data_begin values (the case in which a seek has to decode more than a few frames to refill the reservoir)."""
    x = [0.0] * n
    rnd = lcg(777 + 31 * c)
    a1 = int(n * 0.04)
    a2 = int(n * 0.74)
    f1 = 440.0 * (1.0 + 0.5 * c)
    for i in range(a1):
        x[i] = 0.3 * math.sin(2 * math.pi * f1 * i / rate) * min(1.0, (a1 - 1 - i) / 80.0)
    f2 = 1000.0 * (1.0 + 0.3 * c)
    for i in range(a2, n):
        e = min(1.0, (i - a2) / 40.0, (n - 1 - i) / 40.0)
        x[i] = e * (0.33 * next(rnd) + 0.5 * math.sin(2 * math.pi * f2 * i / rate))
    return x


def one_channel(rate, n, c, first):
    """One channel: a sweep, a noise burst, silence, a loud tone near full scale, a very quiet tone and clicks (transients); the channels differ."""
    x = [0.0] * n
    rnd = lcg(12345 + 7919 * c)

    def sect(a, b):
        return int(n * a), int(n * b)

    nyq = rate / 2.0
    # 0.00-0.30: a logarithmic sweep (the first 12 % is the same in all channels so that stereo files have a mono-compatible part)
    a0, a1 = sect(0.0, 0.30)
    f0, f1 = 90.0 * (1.0 + 0.31 * c), nyq * (0.86 - 0.05 * (c % 3))
    if c % 2 == 1:
        f0, f1 = f1, f0
    k = f1 / f0
    length = (a1 - a0) / float(rate)
    for i in range(a0, a1):
        t = (i - a0) / float(rate)
        ph = 2 * math.pi * f0 * length / math.log(k) * (k ** (t / length) - 1.0)
        x[i] = 0.5 * math.sin(ph)
    if first is not None:
        s0, s1 = sect(0.0, 0.12)
        for i in range(s0, s1):
            x[i] = first[i]
    # 0.30-0.40: a burst of white noise with raised cosine edges
    b0, b1 = sect(0.30, 0.40)
    for i in range(b0, b1):
        e = math.sin(math.pi * (i - b0) / float(b1 - b0))
        x[i] = 0.33 * e * next(rnd)
    # 0.40-0.48: digital silence
    # 0.48-0.62: two tones that add up to just below full scale
    d0, d1 = sect(0.48, 0.62)
    fa, fb = 1000.0 * (1.0 + 0.23 * c), 2300.0 * (1.0 + 0.11 * c)
    fa, fb = min(fa, nyq * 0.8), min(fb, nyq * 0.9)
    for i in range(d0, d1):
        e = min(1.0, (i - d0) / 40.0, (d1 - 1 - i) / 40.0)
        x[i] = e * (0.62 * math.sin(2 * math.pi * fa * i / rate) + 0.365 * math.sin(2 * math.pi * fb * i / rate + 0.7))
    # 0.62-0.75: a very quiet tone (about -48 dBFS) with a trace of noise
    e0, e1 = sect(0.62, 0.75)
    fe = min(523.25 * (1.0 + 0.19 * c), nyq * 0.7)
    for i in range(e0, e1):
        x[i] = 0.004 * math.sin(2 * math.pi * fe * i / rate) + 0.0008 * next(rnd)
    # 0.75-1.00: a quiet tone with a click (a short decaying noise burst) every 12 ms: transients that make the encoders switch to short blocks
    g0, g1 = sect(0.75, 1.0)
    period = max(8, int(rate * 0.012))
    ff = min(3000.0 * (1.0 + 0.07 * c), nyq * 0.8)
    for i in range(g0, g1):
        x[i] = 0.05 * math.sin(2 * math.pi * ff * i / rate)
    p = g0 + period // 2
    while p < g1:
        for j in range(min(64, g1 - p)):
            x[p + j] += 0.7 * math.exp(-j / 14.0) * next(rnd)
        p += period
    # a short fade out at the very end
    for j in range(min(48, n)):
        x[n - 1 - j] *= j / 48.0
    return x


def signal(rate, channels, frames, profile='mix'):
    """The interleaved float samples of a signal (list of per channel lists)."""
    chans = []
    for c in range(channels):
        if profile == 'gap':
            chans.append(gap_channel(rate, frames, c))
        else:
            chans.append(one_channel(rate, frames, c, chans[0] if c > 0 else None))
    return chans


_signals = {}


def source_file(rate, channels, frames, gain=1.0, profile='mix'):
    """Writes (once) the raw f32le file of a signal; returns its path."""
    key = (rate, channels, frames, int(gain * 1000), profile)
    path = os.path.join(TMP, 'sig_%d_%d_%d_%d_%s.f32' % key)
    if key not in _signals:
        chans = signal(rate, channels, frames, profile)
        buf = bytearray()
        for i in range(frames):
            for c in range(channels):
                buf += struct.pack('<f', chans[c][i] * gain)
        with open(path, 'wb') as f:
            f.write(bytes(buf))
        _signals[key] = chans
    return path


LAYOUT_OF = {1: 'mono', 2: 'stereo', 3: '3.0', 4: '4.0', 5: '5.0', 6: '5.1', 7: '6.1', 8: '7.1'}


# ---------------------------------------------------------------------------------------------------------------------------------------
# reading what is in the encoded streams (coding tools), to document the coverage of the files
# ---------------------------------------------------------------------------------------------------------------------------------------
def ogg_packets(data):
    """The packets of the first logical stream of an Ogg file."""
    pos, cur, packets, serial = 0, bytearray(), [], None
    while pos + 27 <= len(data) and data[pos:pos + 4] == b'OggS':
        nseg = data[pos + 26]
        lacing = data[pos + 27:pos + 27 + nseg]
        ser = struct.unpack_from('<I', data, pos + 14)[0]
        body = pos + 27 + nseg
        if serial is None:
            serial = ser
        for lv in lacing:
            if ser == serial:
                cur += data[body:body + lv]
            body += lv
            if lv < 255:
                if ser == serial:
                    packets.append(bytes(cur))
                    cur = bytearray()
        pos = body
    return packets


def opus_note(path):
    data = open(path, 'rb').read()
    pk = ogg_packets(data)[2:]
    cfg, codes, stereo = {}, {}, set()
    for p in pk:
        if not p:
            continue
        t = p[0]
        cfg[t >> 3] = cfg.get(t >> 3, 0) + 1
        codes[t & 3] = codes.get(t & 3, 0) + 1
        stereo.add((t >> 2) & 1)
    modes = set()
    for c in cfg:
        modes.add('SILK' if c < 12 else ('hybrid' if c < 16 else 'CELT'))
    sizes = [2.5, 5, 10, 20, 40, 60]
    return 'opus packets=%d modes=%s configs=%s frame_count_codes=%s tiny_packets=%d' % (
        len(pk), '/'.join(sorted(modes)), ','.join(str(c) for c in sorted(cfg)), ','.join(str(c) for c in sorted(codes)),
        sum(1 for p in pk if len(p) <= 2))


def mp3_note(path):
    """Header and side information statistics of an MP3 file (MPEG version, bit rates, channel modes, block types, mixed blocks)."""
    d = open(path, 'rb').read()
    pos = 0
    if d[:3] == b'ID3':
        size = ((d[6] & 127) << 21) | ((d[7] & 127) << 14) | ((d[8] & 127) << 7) | (d[9] & 127)
        pos = 10 + size
    br1 = [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0]
    br2 = [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0]
    sr = {3: [44100, 48000, 32000], 2: [22050, 24000, 16000], 0: [11025, 12000, 8000]}
    frames, versions, bits, modes, btypes, mixed, ms, isb, scs, pre, nframes = 0, set(), set(), set(), set(), 0, 0, 0, set(), 0, 0
    while pos + 4 <= len(d):
        h = (d[pos] << 24) | (d[pos + 1] << 16) | (d[pos + 2] << 8) | d[pos + 3]
        if (h >> 21) != 0x7FF:
            pos += 1
            continue
        ver, layer, prot = (h >> 19) & 3, (h >> 17) & 3, (h >> 16) & 1
        bi, si, pad, mode, mext = (h >> 12) & 15, (h >> 10) & 3, (h >> 9) & 1, (h >> 6) & 3, (h >> 4) & 3
        if ver == 1 or layer != 1 or bi in (0, 15) or si == 3:
            pos += 1
            continue
        mpeg1 = ver == 3
        rate = sr[ver][si]
        kbps = (br1 if mpeg1 else br2)[bi]
        flen = (144000 * kbps // rate if mpeg1 else 72000 * kbps // rate) + pad
        versions.add({3: 'MPEG-1', 2: 'MPEG-2', 0: 'MPEG-2.5'}[ver])
        bits.add(kbps)
        modes.add(['stereo', 'joint', 'dual', 'mono'][mode])
        nframes += 1
        if mode == 1:
            ms += (mext >> 1) & 1
            isb += mext & 1
        # side information: skip the crc, read the per granule and channel fields
        sp = pos + 4 + (2 if not prot else 0)
        nch = 1 if mode == 3 else 2
        sidelen = (17 if nch == 1 else 32) if mpeg1 else (9 if nch == 1 else 17)
        bs = int.from_bytes(d[sp:sp + sidelen], 'big')
        total = sidelen * 8
        pos2 = 0

        def take(n):
            nonlocal pos2
            v = (bs >> (total - pos2 - n)) & ((1 << n) - 1)
            pos2 += n
            return v
        if mpeg1:
            take(9)
            take(5 if nch == 1 else 3)
            for _ in range(nch):
                take(4)
            ng = 2
        else:
            take(8)
            take(1 if nch == 1 else 2)
            ng = 1
        for _g in range(ng):
            for _c in range(nch):
                take(12)
                take(9)
                take(8)
                scs.add(take(4 if mpeg1 else 9))
                if take(1):
                    bt = take(2)
                    btypes.add(bt)
                    if take(1):
                        mixed += 1
                    take(10)
                    take(9)
                else:
                    btypes.add(0)
                    take(15)
                    take(7)
                if mpeg1:
                    pre += take(1)
                else:
                    pass
                take(1)
                take(1)
        pos += flen
    return 'mp3 %s kbps=%s modes=%s ms_frames=%d intensity_frames=%d block_types=%s mixed_granules=%d preflag=%d frames=%d' % (
        '/'.join(sorted(versions)), ','.join(str(b) for b in sorted(bits)), '/'.join(sorted(modes)), ms, isb, ''.join(str(b) for b in sorted(btypes)),
        mixed, pre, nframes)


# ---------------------------------------------------------------------------------------------------------------------------------------
# the fixtures
# ---------------------------------------------------------------------------------------------------------------------------------------
def strip_flac_padding(path):
    """Removes the 8 KiB PADDING metadata block that ffmpeg's FLAC muxer writes (the references are stored as small as possible); the audio is untouched."""
    d = open(path, 'rb').read()
    assert d[:4] == b'fLaC'
    pos, blocks = 4, []
    while True:
        h = d[pos]
        ln = int.from_bytes(d[pos + 1:pos + 4], 'big')
        blocks.append((h & 127, d[pos + 4:pos + 4 + ln]))
        pos += 4 + ln
        if h >> 7:
            break
    kept = [b for b in blocks if b[0] != 1]
    out = bytearray(b'fLaC')
    for i, (typ, body) in enumerate(kept):
        out.append((0x80 if i == len(kept) - 1 else 0) | typ)
        out += len(body).to_bytes(3, 'big') + body
    open(path, 'wb').write(bytes(out) + d[pos:])


lines = {}
order = []
total = [0, 0]


def probe(path, entries):
    r = run([FFPROBE, '-v', 'error', '-select_streams', 'a:0', '-show_entries', 'stream=' + entries, '-of', 'default=nw=1', path])
    out = {}
    for ln in r.stdout.decode('utf-8').splitlines():
        if '=' in ln:
            k, v = ln.split('=', 1)
            out[k] = v
    return out


def fixture(name, group, ext, rate, channels, frames, enc, kind='lossy', fmt=None, dec=(), subdir=None, note='', mime=None, layout=None, info=None, gapless=None, expected=None, gain=None, profile='mix'):
    """Encodes a synthetic signal, decodes the result with ffmpeg and records the reference.

    enc:    the output options of ffmpeg (codec, bit rate, container options ...)
    dec:    input options for the reference decoding (for example a decoder name)
    layout: the channel layout name of the source (default by the number of channels)
    """
    if ONLY is not None and name not in ONLY:
        return
    layout = layout or LAYOUT_OF[channels]
    if gain is None:
        gain = 0.5 if (channels > 2 and kind == 'lossy') else 1.0
    src = source_file(rate, channels, frames, gain, profile)
    sub = subdir or group
    os.makedirs(os.path.join(OUT, sub), exist_ok=True)
    rel = '%s/%s.%s' % (sub, name, ext)
    path = os.path.join(OUT, rel)
    fmtargs = ['-f', fmt] if fmt else []
    try:
        run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-f', 'f32le', '-ar', str(rate), '-ch_layout', layout, '-i', src] + list(enc) + fmtargs + [path])
        pcm = os.path.join(TMP, name + '.s16')
        run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y'] + list(dec) + ['-i', path, '-f', 's16le', '-acodec', 'pcm_s16le', pcm])
    except RuntimeError as e:
        print('FAILED %s: %s' % (name, str(e)[:600]))
        return
    p = probe(path, 'sample_rate,channels,channel_layout')
    orate, ochan = int(p['sample_rate']), int(p['channels'])
    olayout = p.get('channel_layout', 'unknown')
    if olayout not in ROLES:
        olayout = LAYOUT_OF.get(ochan, '')
    roles = ROLES.get(olayout)
    if roles is None:
        raise RuntimeError('%s: unknown channel layout %r' % (name, olayout))
    raw = open(pcm, 'rb').read()
    sfmt = probe(path, 'sample_fmt')['sample_fmt']
    if kind == 'exact' and sfmt in ('s32', 's32p'):
        # integer samples wider than 16 bits: round half up like the product (ffmpeg's own narrowing truncates)
        wide = os.path.join(TMP, name + '.s32')
        run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y'] + list(dec) + ['-i', path, '-f', 's32le', '-acodec', 'pcm_s32le', wide])
        w = array.array('i')
        w.frombytes(open(wide, 'rb').read())
        if sys.byteorder != 'little':
            w.byteswap()
        n16 = array.array('h', [max(-32768, min(32767, (v + 32768) >> 16)) for v in w])
        if sys.byteorder != 'little':
            n16.byteswap()
        raw = n16.tobytes()
    nframes = len(raw) // (2 * ochan)
    if gapless is None:
        # the containers that carry the start and the end of the signal: Ogg Vorbis (granule positions), Opus (pre-skip, granule positions or DiscardPadding), MP4
        # (edit list; not the fragmented file, whose moov has none)
        gapless = (name.startswith('vorbis_') and ext == 'ogg') or name.startswith('opus_') or (ext == 'm4a' and 'frag' not in name)
    if expected is None:
        expected = int(round(frames * orate / float(rate))) if gapless else nframes
    refs = []
    if kind == 'lossy' or ochan > 2:
        os.makedirs(os.path.join(OUT, 'ref'), exist_ok=True)
        # the reference is stored as FLAC (lossless, much smaller than gzip for audio; the product's FLAC decoder is verified bit for bit against ffmpeg in
        # the same test class).  The product decoder mixes more than two channels down to stereo, so a multi channel reference is one mono FLAC per channel.
        samples = array.array('h')
        samples.frombytes(raw)
        if sys.byteorder != 'little':
            samples.byteswap()
        groups = [list(range(ochan))] if ochan <= 2 else [[k] for k in range(ochan)]
        for gi, chs in enumerate(groups):
            part = array.array('h')
            for i in range(nframes):
                for k in chs:
                    part.append(samples[i * ochan + k])
            if sys.byteorder != 'little':
                part.byteswap()
            partraw = part.tobytes()
            rawpath = os.path.join(TMP, '%s.%d.s16' % (name, gi))
            open(rawpath, 'wb').write(partraw)
            refrel = 'ref/%s.flac' % name if len(groups) == 1 else 'ref/%s.%d.flac' % (name, gi)
            run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', str(orate), '-ch_layout', LAYOUT_OF[len(chs)], '-i', rawpath,
                 '-c:a', 'flac', '-compression_level', '12', '-f', 'flac', os.path.join(OUT, refrel)])
            strip_flac_padding(os.path.join(OUT, refrel))
            back = os.path.join(TMP, name + '.back.s16')
            run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-i', os.path.join(OUT, refrel), '-f', 's16le', '-acodec', 'pcm_s16le', back])
            if open(back, 'rb').read() != partraw:
                raise RuntimeError('%s: the FLAC copy of the reference differs' % name)
            refs.append(refrel)
        ref = 'flac:' + '+'.join(refs)
    else:
        ref = 'md5:' + hashlib.md5(raw).hexdigest()
    extra = ''
    if info:
        extra = info(path)
    n = note + (' ; signal level -6 dB so that no decoded channel clips' if gain != 1.0 else '') + (' ; ' + extra if extra else '')
    size = os.path.getsize(path)
    refsize = sum(os.path.getsize(os.path.join(OUT, r)) for r in refs)
    total[0] += size
    total[1] += refsize
    lines[name] = '|'.join([name, rel, mime or MIMES[ext], group, kind, str(orate), str(ochan), roles, str(nframes), str(expected), ref, n])
    if name not in order:
        order.append(name)
    print('%-34s %7d bytes  ref %7d  %d x %d @ %d  %s' % (name, size, refsize, nframes, ochan, orate, kind))


def flush_index():
    path = os.path.join(OUT, 'index.txt')
    old = {}
    olds = []
    if os.path.exists(path):
        for ln in open(path, encoding='utf-8').read().splitlines():
            if ln.strip() and not ln.startswith('#'):
                olds.append(ln.split('|')[0])
                old[ln.split('|')[0]] = ln
    merged = []
    for nm in olds:
        merged.append(lines.get(nm, old[nm]))
    for nm in order:
        if nm not in old:
            merged.append(lines[nm])
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('# name|file|mime|group|kind|rate|channels|layout|frames|expected|reference|note  (see generation/make_audio_fixtures.py)\n')
        f.write('\n'.join(merged) + '\n')


def ms(rate, millis):
    """The number of sample frames of a signal of that many milliseconds."""
    return int(rate * millis / 1000.0)


def define_all():
    # ---- MP3 (libmp3lame) ----
    L = ['-c:a', 'libmp3lame']
    fixture('mp3_cbr128_44100_js', 'mp3', 'mp3', 44100, 2, ms(44100, 200), L + ['-b:a', '128k'], fmt='mp3', info=mp3_note, note='lame CBR 128k joint stereo, ID3v2 + Xing/LAME', gapless=True)
    fixture('mp3_cbr320_48000_st', 'mp3', 'mp3', 48000, 2, ms(48000, 100), L + ['-b:a', '320k', '-joint_stereo', '0'], fmt='mp3', info=mp3_note, note='lame CBR 320k forced stereo', gapless=True)
    fixture('mp3_v0_44100_js', 'mp3', 'mp3', 44100, 2, ms(44100, 100), L + ['-q:a', '0'], fmt='mp3', info=mp3_note, note='lame VBR V0 joint stereo (Xing header)', gapless=True)
    fixture('mp3_v2_32000_mono', 'mp3', 'mp3', 32000, 1, ms(32000, 200), L + ['-q:a', '2'], fmt='mp3', info=mp3_note, note='lame VBR V2 mono, MPEG-1 32 kHz', gapless=True)
    fixture('mp3_v6_22050_mono', 'mp3', 'mp3', 22050, 1, ms(22050, 250), L + ['-q:a', '6'], fmt='mp3', info=mp3_note, note='lame VBR V6 mono, MPEG-2 (LSF) 22.05 kHz', gapless=False)
    fixture('mp3_cbr64_24000_st', 'mp3', 'mp3', 24000, 2, ms(24000, 150), L + ['-b:a', '64k'], fmt='mp3', info=mp3_note, note='lame CBR 64k stereo, MPEG-2 24 kHz', gapless=False)
    fixture('mp3_cbr16_8000_mono', 'mp3', 'mp3', 8000, 1, ms(8000, 400), L + ['-b:a', '16k'], fmt='mp3', info=mp3_note, note='lame CBR 16k mono, MPEG-2.5 8 kHz', gapless=False)
    fixture('mp3_cbr24_11025_mono', 'mp3', 'mp3', 11025, 1, ms(11025, 400), L + ['-b:a', '24k'], fmt='mp3', info=mp3_note, note='lame CBR 24k mono, MPEG-2.5 11.025 kHz', gapless=False)
    fixture('mp3_cbr24_12000_mono', 'mp3', 'mp3', 12000, 1, ms(12000, 400), L + ['-b:a', '24k'], fmt='mp3', info=mp3_note, note='lame CBR 24k mono, MPEG-2.5 12 kHz', gapless=False)
    fixture('mp3_cbr96_44100_mono', 'mp3', 'mp3', 44100, 1, ms(44100, 100), L + ['-b:a', '96k'], fmt='mp3', info=mp3_note, note='lame CBR 96k mono, MPEG-1 44.1 kHz', gapless=True)
    fixture('mp3_cbr128_48000_js', 'mp3', 'mp3', 48000, 2, ms(48000, 120), L + ['-b:a', '128k'], fmt='mp3', info=mp3_note, note='lame CBR 128k joint stereo, MPEG-1 48 kHz', gapless=True)
    fixture('mp3_cbr96_32000_st', 'mp3', 'mp3', 32000, 2, ms(32000, 140), L + ['-b:a', '96k'], fmt='mp3', info=mp3_note, note='lame CBR 96k stereo, MPEG-1 32 kHz', gapless=True)
    fixture('mp3_cbr32_44100_st', 'mp3', 'mp3', 44100, 2, ms(44100, 120), L + ['-b:a', '32k'], fmt='mp3', info=mp3_note, note='lame CBR 32k stereo, MPEG-1 44.1 kHz (very low rate: many empty lines)', gapless=True)
    fixture('mp3_v2_44100_st_gap', 'mp3', 'mp3', 44100, 2, ms(44100, 700), L + ['-q:a', '2'], fmt='mp3', info=mp3_note, gapless=True, profile='gap',
            note='lame VBR V2 stereo with a long digital silence before a loud burst: many small frames, then large main_data_begin values (a seek has to decode '
                 'more than 8 frames in front of the target to refill the bit reservoir)')
    fixture('mp3_cbr192_44100_notag','mp3', 'mp3', 44100, 2, ms(44100, 100), L + ['-b:a', '192k', '-write_xing', '0', '-id3v2_version', '0'], fmt='mp3', info=mp3_note,
            note='lame CBR 192k, no ID3v2, no Xing/LAME header (the stream starts with the first MPEG frame; no gapless information)')
    fixture('mp3_cbr48_16000_id3', 'mp3', 'mp3', 16000, 2, ms(16000, 150),
            L + ['-b:a', '48k', '-write_id3v1', '1', '-id3v2_version', '3', '-metadata', 'title=Test tone', '-metadata', 'artist=SwingWebPanel',
                 '-metadata', 'comment=' + ('padding text ' * 200)], fmt='mp3', info=mp3_note, gapless=True, expected=2929,
            note='lame CBR 48k MPEG-2 16 kHz with a large ID3v2.3 tag and an ID3v1 tag; the Lavc tag of the muxer declares a delay of 47 and a padding of 1056 frames '
                 '(7 frames of 576 minus 576 and 527 = 2929, not the 2400 of the source) and ffmpeg trims only the start')

    # ---- AAC (ffmpeg's native encoder) ----
    A = ['-c:a', 'aac']
    fixture('aac_adts_44100_st_128k', 'aac', 'aac', 44100, 2, ms(44100, 200), A + ['-b:a', '128k'], fmt='adts', note='aac ADTS 44.1 kHz stereo 128k (default tools: M/S auto, IS, PNS, TNS)')
    fixture('aac_m4a_44100_st_96k', 'aac', 'm4a', 44100, 2, ms(44100, 120), A + ['-b:a', '96k'], fmt='ipod', note='aac in M4A (ipod muxer: edit list) 96k stereo')
    fixture('aac_m4a_48000_mono_64k', 'aac', 'm4a', 48000, 1, ms(48000, 120), A + ['-b:a', '64k'], fmt='mp4', note='aac in MP4 mono 64k')
    fixture('aac_adts_22050_mono_48k', 'aac', 'aac', 22050, 1, ms(22050, 200), A + ['-b:a', '48k'], fmt='adts', note='aac ADTS 22.05 kHz mono 48k')
    fixture('aac_adts_8000_mono_16k', 'aac', 'aac', 8000, 1, ms(8000, 400), A + ['-b:a', '16k'], fmt='adts', note='aac ADTS 8 kHz mono 16k')
    fixture('aac_m4a_32000_st_128k', 'aac', 'm4a', 32000, 2, ms(32000, 120), A + ['-b:a', '128k'], fmt='ipod', note='aac in M4A 32 kHz stereo 128k')
    fixture('aac_adts_44100_st_320k', 'aac', 'aac', 44100, 2, ms(44100, 120), A + ['-b:a', '320k'], fmt='adts', note='aac ADTS stereo 320k (large quantized values: escape codes of codebook 11)')
    fixture('aac_adts_44100_mono_24k', 'aac', 'aac', 44100, 1, ms(44100, 150), A + ['-b:a', '24k'], fmt='adts', note='aac ADTS mono 24k')
    fixture('aac_adts_44100_st_ms', 'aac', 'aac', 44100, 2, ms(44100, 100), A + ['-b:a', '160k', '-aac_ms', '1'], fmt='adts', note='aac ADTS stereo with M/S forced')
    fixture('aac_adts_44100_st_plain', 'aac', 'aac', 44100, 2, ms(44100, 100), A + ['-b:a', '128k', '-aac_is', '0', '-aac_pns', '0', '-aac_tns', '0', '-aac_ms', '0', '-aac_coder', 'twoloop'],
            fmt='adts', note='aac ADTS stereo without IS/PNS/TNS/M-S (twoloop coder)')
    fixture('aac_adts_44100_st_lowrate', 'aac', 'aac', 44100, 2, ms(44100, 100), A + ['-b:a', '40k', '-aac_coder', 'fast'], fmt='adts', note='aac ADTS stereo 40k, fast coder: IS and PNS are likely at this rate')
    fixture('aac_adts_16000_mono_pce', 'aac', 'aac', 16000, 1, ms(16000, 200), A + ['-b:a', '32k', '-aac_pce', '1'], fmt='adts', note='aac ADTS mono with a program config element')
    fixture('aac_m4a_44100_mono_frag', 'aac', 'm4a', 44100, 1, ms(44100, 100), A + ['-b:a', '64k', '-movflags', 'frag_keyframe+empty_moov+default_base_moof'], fmt='mp4', expected=5434,
            note='aac in fragmented MP4 (moof/traf/trun) mono; no edit list, the last sample is 314 frames long (4410 + 1024 priming = 5434 frames); ffmpeg ignores the '
                 'sample durations and outputs all 6144 decoded frames')
    fixture('aac_m4a_44100_mono_faststart', 'aac', 'm4a', 44100, 1, ms(44100, 100), A + ['-b:a', '64k', '-movflags', '+faststart'], fmt='ipod', note='aac in M4A with the moov box before mdat, mono')
    for sr in (96000, 88200, 64000, 24000, 12000, 11025, 7350):
        fixture('aac_adts_%d_mono' % sr, 'aac', 'aac', sr, 1, 2560, A + ['-b:a', '32k' if sr < 64000 else '96k'], fmt='adts',
                note='aac ADTS mono at %d Hz (sampling frequency index)' % sr)
    for ch, nm in ((3, '3.0'), (4, '4.0'), (6, '5.1'), (8, '7.1')):
        # the 5.1 file is an ADTS stream, the others are MP4 files
        fixture('aac_%s_16000_%dch' % ('adts' if ch == 6 else 'm4a', ch), 'aac', 'aac' if ch == 6 else 'm4a', 16000, ch, 2048, A + ['-b:a', '%dk' % (24 * ch)],
                fmt='adts' if ch == 6 else 'mp4', layout=nm, note='aac %s with the channel layout %s (channel configuration %d)' % ('ADTS' if ch == 6 else 'in MP4', nm, ch))

    # ---- Vorbis (libvorbis) ----
    V = ['-c:a', 'libvorbis']
    fixture('vorbis_q3_44100_st', 'vorbis', 'ogg', 44100, 2, ms(44100, 200), V + ['-q:a', '3'], fmt='ogg', note='libvorbis quality 3 stereo')
    fixture('vorbis_q5_32000_st', 'vorbis', 'ogg', 32000, 2, ms(32000, 140), V + ['-q:a', '5'], fmt='ogg', note='libvorbis quality 5 stereo 32 kHz')
    fixture('vorbis_q0_22050_mono', 'vorbis', 'ogg', 22050, 1, ms(22050, 200), V + ['-q:a', '0'], fmt='ogg', note='libvorbis quality 0 mono 22.05 kHz')
    fixture('vorbis_qm1_8000_mono', 'vorbis', 'ogg', 8000, 1, ms(8000, 400), V + ['-q:a', '-1'], fmt='ogg', note='libvorbis quality -1 mono 8 kHz')
    fixture('vorbis_q10_44100_st', 'vorbis', 'ogg', 44100, 2, ms(44100, 80), V + ['-q:a', '10'], fmt='ogg', note='libvorbis quality 10 stereo')
    fixture('vorbis_b64_44100_mono', 'vorbis', 'ogg', 44100, 1, ms(44100, 150), V + ['-b:a', '64k'], fmt='ogg', note='libvorbis managed bit rate 64k mono')
    fixture('vorbis_q4_16000_3ch', 'vorbis', 'ogg', 16000, 3, 2048, V + ['-q:a', '4'], fmt='ogg', layout='3.0', note='libvorbis 3 channels (L C R)')
    fixture('vorbis_q4_16000_51', 'vorbis', 'ogg', 16000, 6, 2048, V + ['-q:a', '4'], fmt='ogg', layout='5.1', note='libvorbis 5.1 (L C R BL BR LFE in Vorbis order)')
    fixture('vorbis_q4_16000_71', 'vorbis', 'ogg', 16000, 8, 2048, V + ['-q:a', '4'], fmt='ogg', layout='7.1', note='libvorbis 7.1')
    fixture('vorbis_q6_48000_webm', 'webm', 'webm', 48000, 2, ms(48000, 100), V + ['-q:a', '6'], fmt='webm', note='libvorbis quality 6 stereo 48 kHz in WebM (Matroska)')

    # ---- FLAC ----
    F = ['-c:a', 'flac', '-sample_fmt', 's16']
    fixture('flac_c5_44100_st16', 'flac', 'flac', 44100, 2, 4200, F + ['-compression_level', '5'], kind='exact', fmt='flac', note='flac level 5 stereo 16 bit')
    fixture('flac_c0_44100_st16', 'flac', 'flac', 44100, 2, 2400, F + ['-compression_level', '0'], kind='exact', fmt='flac', note='flac level 0 (fixed predictors, 1152 sample blocks)')
    fixture('flac_c8_48000_st16', 'flac', 'flac', 48000, 2, 2500, F + ['-compression_level', '8'], kind='exact', fmt='flac', note='flac level 8')
    fixture('flac_c5_44100_mono24', 'flac', 'flac', 44100, 1, 4200, F + ['-sample_fmt', 's32', '-bits_per_raw_sample', '24'], kind='exact', fmt='flac', note='flac 24 bit mono')
    fixture('flac_c5_96000_st24', 'flac', 'flac', 96000, 2, 1000, F + ['-sample_fmt', 's32', '-bits_per_raw_sample', '24'], kind='exact', fmt='flac', note='flac 24 bit 96 kHz stereo')
    fixture('flac_c5_8000_mono16', 'flac', 'flac', 8000, 1, 4200, F, kind='exact', fmt='flac', note='flac 8 kHz mono')
    fixture('flac_c5_16000_51', 'flac', 'flac', 16000, 6, 500, F, kind='exact', fmt='flac', layout='5.1', note='flac 5.1')
    fixture('flac_c5_16000_71_24', 'flac', 'flac', 16000, 8, 300, F + ['-sample_fmt', 's32', '-bits_per_raw_sample', '24'], kind='exact', fmt='flac', layout='7.1', note='flac 7.1 24 bit')
    fixture('flac_ogg_44100_st', 'flac', 'oga', 44100, 2, 1800, F, kind='exact', fmt='ogg', mime='audio/ogg', note='flac in Ogg (Ogg FLAC)')
    fixture('flac_blk192_44100_st', 'flac', 'flac', 44100, 2, 1000, F + ['-frame_size', '192'], kind='exact', fmt='flac', note='flac with 192 sample blocks')
    for mode in ('indep', 'left_side', 'right_side', 'mid_side'):
        fixture('flac_%s_44100_st' % mode, 'flac', 'flac', 44100, 2, 600, F + ['-ch_mode', mode], kind='exact', fmt='flac', note='flac channel assignment %s' % mode)
    fixture('flac_fixed_22050_st', 'flac', 'flac', 22050, 2, 1500, F + ['-lpc_type', 'fixed', '-compression_level', '3'], kind='exact', fmt='flac', note='flac with fixed predictors only')

    # ---- Opus (libopus); the reference decoder is libopus too ----
    O = ['-c:a', 'libopus']
    # the 16 bit API of libopus soft clips loud signals; the float API (what the browsers use and what the product decodes like) does not, so the reference
    # is decoded in float and converted to 16 bits by ffmpeg
    ro = ['-request_sample_fmt', 'flt', '-c:a', 'libopus']
    fixture('opus_audio_128k_48000_st', 'opus', 'opus', 48000, 2, ms(48000, 100), O + ['-b:a', '128k'], fmt='ogg', dec=ro, info=opus_note, note='libopus audio 128k stereo (CELT)')
    fixture('opus_audio_32k_48000_st', 'opus', 'opus', 48000, 2, ms(48000, 100), O + ['-b:a', '32k'], fmt='ogg', dec=ro, info=opus_note, note='libopus audio 32k stereo')
    fixture('opus_voip_16k_16000_mono', 'opus', 'opus', 16000, 1, ms(16000, 150), O + ['-application', 'voip', '-b:a', '16k'], fmt='ogg', dec=ro, info=opus_note,
            note='libopus voip 16k mono (SILK wideband)')
    fixture('opus_voip_12k_8000_mono', 'opus', 'opus', 8000, 1, ms(8000, 150), O + ['-application', 'voip', '-b:a', '12k'], fmt='ogg', dec=ro, info=opus_note,
            note='libopus voip 12k mono (SILK narrowband)')
    fixture('opus_voip_24k_24000_st', 'opus', 'opus', 24000, 2, ms(24000, 100), O + ['-application', 'voip', '-b:a', '24k'], fmt='ogg', dec=ro, info=opus_note,
            note='libopus voip 24k stereo (SILK/hybrid)')
    fixture('opus_voip_40k_48000_mono', 'opus', 'opus', 48000, 1, ms(48000, 100), O + ['-application', 'voip', '-b:a', '40k'], fmt='ogg', dec=ro, info=opus_note,
            note='libopus voip 40k mono full band (hybrid)')
    fixture('opus_lowdelay_96k_48000_st', 'opus', 'opus', 48000, 2, ms(48000, 80), O + ['-application', 'lowdelay', '-b:a', '96k', '-frame_duration', '2.5'], fmt='ogg', dec=ro,
            info=opus_note, note='libopus lowdelay 2.5 ms frames stereo (CELT only)')
    fixture('opus_frame60_48k_48000_st', 'opus', 'opus', 48000, 2, ms(48000, 120), O + ['-b:a', '48k', '-frame_duration', '60'], fmt='ogg', dec=ro, info=opus_note,
            note='libopus 60 ms packets (several frames per packet)')
    fixture('opus_frame120_20k_16000_mono', 'opus', 'opus', 16000, 1, ms(16000, 240), O + ['-application', 'voip', '-b:a', '20k', '-frame_duration', '120'], fmt='ogg', dec=ro,
            info=opus_note, note='libopus 120 ms packets (voip: SILK frames, code 3)')
    fixture('opus_fec_24k_16000_mono', 'opus', 'opus', 16000, 1, ms(16000, 150), O + ['-application', 'voip', '-b:a', '24k', '-fec', '1', '-packet_loss', '20'], fmt='ogg', dec=ro,
            info=opus_note, note='libopus voip with in-band FEC (LBRR frames inside the packets)')
    fixture('opus_dtx_16k_16000_mono', 'opus', 'opus', 16000, 1, ms(16000, 150), O + ['-application', 'voip', '-b:a', '16k', '-dtx', '1'], fmt='ogg', dec=ro, info=opus_note,
            note='libopus voip with discontinuous transmission (tiny packets during the silence)')
    fixture('opus_audio_96k_48000_51', 'opus', 'opus', 48000, 6, ms(48000, 60), O + ['-b:a', '192k', '-mapping_family', '1'], fmt='ogg', layout='5.1', dec=ro, info=opus_note,
            note='libopus 5.1 with channel mapping family 1')
    fixture('opus_audio_64k_48000_webm_native', 'webm', 'webm', 48000, 2, ms(48000, 100), O + ['-b:a', '64k'], fmt='webm', note='libopus in WebM; the reference is decoded by the native opus decoder of ffmpeg (a second reference decoder)')

    # ---- WAV ----
    W = lambda c: ['-c:a', c]
    fixture('wav_u8_8000_mono', 'wav', 'wav', 8000, 1, 1500, W('pcm_u8'), kind='exact', fmt='wav', note='WAV PCM 8 bit unsigned')
    fixture('wav_s16_44100_st', 'wav', 'wav', 44100, 2, 1500, W('pcm_s16le'), kind='exact', fmt='wav', note='WAV PCM 16 bit')
    fixture('wav_s24_48000_st', 'wav', 'wav', 48000, 2, 1000, W('pcm_s24le'), kind='exact', fmt='wav', note='WAV PCM 24 bit (WAVE_FORMAT_EXTENSIBLE)')
    fixture('wav_s32_44100_st', 'wav', 'wav', 44100, 2, 800, W('pcm_s32le'), kind='exact', fmt='wav', note='WAV PCM 32 bit')
    fixture('wav_f32_44100_st', 'wav', 'wav', 44100, 2, 800, W('pcm_f32le'), kind='exact', fmt='wav', note='WAV IEEE float 32 bit')
    fixture('wav_f64_22050_mono', 'wav', 'wav', 22050, 1, 1000, W('pcm_f64le'), kind='exact', fmt='wav', note='WAV IEEE float 64 bit')
    fixture('wav_alaw_8000_mono', 'wav', 'wav', 8000, 1, 2000, W('pcm_alaw'), kind='exact', fmt='wav', note='WAV A-law')
    fixture('wav_mulaw_8000_mono', 'wav', 'wav', 8000, 1, 2000, W('pcm_mulaw'), kind='exact', fmt='wav', note='WAV mu-law')
    fixture('wav_alaw_22050_st', 'wav', 'wav', 22050, 2, 1200, W('pcm_alaw'), kind='exact', fmt='wav', note='WAV A-law stereo')
    fixture('wav_ima_44100_st', 'wav', 'wav', 44100, 2, 2500, W('adpcm_ima_wav'), kind='exact', fmt='wav', note='WAV IMA ADPCM stereo (blocks of 505 frames)')
    fixture('wav_ima_8000_mono', 'wav', 'wav', 8000, 1, 3000, W('adpcm_ima_wav'), kind='exact', fmt='wav', note='WAV IMA ADPCM mono 8 kHz')
    fixture('wav_ms_22050_mono', 'wav', 'wav', 22050, 1, 3000, W('adpcm_ms'), kind='exact', fmt='wav', note='WAV Microsoft ADPCM mono')
    fixture('wav_ms_44100_st', 'wav', 'wav', 44100, 2, 2500, W('adpcm_ms'), kind='exact', fmt='wav', note='WAV Microsoft ADPCM stereo')
    fixture('wav_s16_16000_51', 'wav', 'wav', 16000, 6, 500, W('pcm_s16le'), kind='exact', fmt='wav', layout='5.1', note='WAV PCM 16 bit 5.1 (WAVE_FORMAT_EXTENSIBLE with a speaker mask)')
    fixture('wav_s16_16000_3ch', 'wav', 'wav', 16000, 3, 600, W('pcm_s16le'), kind='exact', fmt='wav', layout='3.0', note='WAV PCM 16 bit 3 channels')
    fixture('wav_s16_16000_71', 'wav', 'wav', 16000, 8, 400, W('pcm_s16le'), kind='exact', fmt='wav', layout='7.1', note='WAV PCM 16 bit 7.1')
    fixture('wav_s16_44100_chunks', 'wav', 'wav', 44100, 2, 800, W('pcm_s16le') + ['-write_bext', '1', '-write_peak', 'on', '-metadata', 'title=Chunks', '-metadata', 'artist=SwingWebPanel'],
            kind='exact', fmt='wav', note='WAV with LIST/INFO, bext and PEAK chunks before the data chunk')
    fixture('wav_s16_44100_rf64', 'wav', 'wav', 44100, 2, 800, W('pcm_s16le') + ['-rf64', 'always'], kind='exact', fmt='wav', note='RF64 (ds64 chunk, sizes 0xFFFFFFFF)')

    # ---- AIFF / AIFF-C ----
    fixture('aiff_s16_44100_st', 'aiff', 'aiff', 44100, 2, 1500, W('pcm_s16be'), kind='exact', fmt='aiff', note='AIFF PCM 16 bit')
    fixture('aiff_s8_8000_mono', 'aiff', 'aiff', 8000, 1, 1500, W('pcm_s8'), kind='exact', fmt='aiff', note='AIFF PCM 8 bit')
    fixture('aiff_s24_48000_st', 'aiff', 'aiff', 48000, 2, 1000, W('pcm_s24be'), kind='exact', fmt='aiff', note='AIFF PCM 24 bit')
    fixture('aiff_s32_44100_st', 'aiff', 'aiff', 44100, 2, 800, W('pcm_s32be'), kind='exact', fmt='aiff', note='AIFF PCM 32 bit')
    fixture('aifc_sowt_44100_st', 'aiff', 'aifc', 44100, 2, 1500, W('pcm_s16le'), kind='exact', fmt='aiff', note='AIFF-C sowt (little endian 16 bit)')
    fixture('aifc_fl32_44100_st', 'aiff', 'aifc', 44100, 2, 800, W('pcm_f32be'), kind='exact', fmt='aiff', note='AIFF-C fl32')
    fixture('aifc_fl64_22050_mono', 'aiff', 'aifc', 22050, 1, 1000, W('pcm_f64be'), kind='exact', fmt='aiff', note='AIFF-C fl64')
    fixture('aifc_alaw_8000_mono', 'aiff', 'aifc', 8000, 1, 2000, W('pcm_alaw'), kind='exact', fmt='aiff', note='AIFF-C alaw')
    fixture('aifc_ulaw_8000_mono', 'aiff', 'aifc', 8000, 1, 2000, W('pcm_mulaw'), kind='exact', fmt='aiff', note='AIFF-C ulaw')
    fixture('aifc_raw_8000_mono', 'aiff', 'aifc', 8000, 1, 1500, W('pcm_u8'), kind='exact', fmt='aiff', note='AIFF-C raw (unsigned 8 bit)')
    fixture('aifc_ima4_44100_st', 'aiff', 'aifc', 44100, 2, 2500, W('adpcm_ima_qt'), kind='exact', fmt='aiff', note='AIFF-C ima4 (QuickTime IMA ADPCM, blocks of 64 frames)')
    fixture('aiff_s16_16000_51', 'aiff', 'aiff', 16000, 6, 500, W('pcm_s16be'), kind='exact', fmt='aiff', layout='5.1', note='AIFF PCM 16 bit 6 channels')

    # ---- AU ----
    fixture('au_s16_44100_st', 'au', 'au', 44100, 2, 1500, W('pcm_s16be'), kind='exact', fmt='au', note='AU PCM 16 bit')
    fixture('au_s8_8000_mono', 'au', 'au', 8000, 1, 1500, W('pcm_s8'), kind='exact', fmt='au', note='AU PCM 8 bit')
    fixture('au_s24_48000_st', 'au', 'au', 48000, 2, 1000, W('pcm_s24be'), kind='exact', fmt='au', note='AU PCM 24 bit')
    fixture('au_s32_44100_st', 'au', 'au', 44100, 2, 800, W('pcm_s32be'), kind='exact', fmt='au', note='AU PCM 32 bit')
    fixture('au_f32_44100_st', 'au', 'au', 44100, 2, 800, W('pcm_f32be'), kind='exact', fmt='au', note='AU float 32 bit')
    fixture('au_f64_22050_mono', 'au', 'au', 22050, 1, 1000, W('pcm_f64be'), kind='exact', fmt='au', note='AU float 64 bit')
    fixture('au_mulaw_8000_mono', 'au', 'au', 8000, 1, 2000, W('pcm_mulaw'), kind='exact', fmt='au', note='AU mu-law')
    fixture('au_alaw_8000_mono', 'au', 'au', 8000, 1, 2000, W('pcm_alaw'), kind='exact', fmt='au', note='AU A-law')

    # ---- formats that ffmpeg decodes but the product does not: the product has to reject them with UNSUPPORTED_FORMAT, not play noise ----
    fixture('wav_yamaha_adpcm_8000_mono', 'wav', 'wav', 8000, 1, 2000, W('adpcm_yamaha'), kind='refuse', fmt='wav', note='WAV Yamaha ADPCM (format tag 0x20): not supported')
    fixture('wav_wmav2_44100_st', 'wav', 'wav', 44100, 2, 4000, W('wmav2'), kind='refuse', fmt='wav', note='WAV with Windows Media Audio 2 (format tag 0x161): not supported')
    fixture('mp2_cbr128_44100_st', 'mp3', 'mp2', 44100, 2, 4000, ['-c:a', 'mp2', '-b:a', '128k'], kind='refuse', fmt='mp2', mime='audio/mpeg',
            note='MPEG-1 Layer II: recognized as MPEG audio but only Layer III is decoded')
    fixture('alac_m4a_44100_st', 'aac', 'm4a', 44100, 2, 4000, W('alac'), kind='refuse', fmt='ipod', note='Apple Lossless in M4A: the MP4 file has no AAC track')


define_all()
flush_index()
print('total: %d bytes of encoded files, %d bytes of reference PCM' % (total[0], total[1]))
# the temporary signals and decodes are not kept
for f in os.listdir(TMP):
    os.remove(os.path.join(TMP, f))
os.rmdir(TMP)
