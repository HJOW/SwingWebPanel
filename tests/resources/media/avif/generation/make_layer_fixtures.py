# -*- coding: utf-8 -*-
"""Makes the layered AV1 fixtures of tests/resources/media/avif/layers (plan item P18-03; FEATURE_MATRIX IMG-09).

No encoder in the available ffmpeg builds spatial layers, so the streams are assembled from key frames of tests/resources/media/avif/seq/seq_keyframes.obu
(an encoder output that is checked bit for bit against libdav1d): every temporal unit holds two shown key frames, the first with spatial_id 0 and the second
with spatial_id 1 (an OBU extension header), and the sequence header gets two operating points: operating point 0 decodes both layers (operating_point_idc
0x301), operating point 1 only the layer 0 (0x101). The frames of the two layers are independent pictures of the same size; the stream tests which OBUs are
decoded and which picture is chosen, not the inter-layer prediction.
The reference pictures come from libdav1d (through ffmpeg): -oppoint 0/1 and -alllayers 0/1 give the md5 of the pictures of every layer.
From the OBU streams the script also writes AVIF still image files (an av01 item with the properties a1op and lsel) for the combinations of the decoder tests.

Usage (from the repository root):  python tests/resources/media/avif/generation/make_layer_fixtures.py     (needs ffmpeg on the PATH; not part of the product)

공간 계층이 있는 AV1 시험 자료를 만든다. 쓸 수 있는 ffmpeg 의 인코더 중 공간 계층을 만드는 것이 없으므로 seq_keyframes.obu 의 키 프레임으로 스트림을
조립한다. 시간 단위마다 표시되는 키 프레임 두 개(spatial_id 0, 1)를 두고 시퀀스 헤더에는 동작 지점 둘(0: 두 계층 0x301, 1: 계층 0 만 0x101)을 둔다.
기준 화면은 libdav1d(ffmpeg 경유)의 -oppoint, -alllayers 옵션으로 얻은 계층별 md5 이다. OBU 스트림에서 속성 a1op, lsel 이 있는 AVIF 정지 이미지 파일도
만든다.
"""
import hashlib
import os
import struct
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', '..'))
SEQ = os.path.join(ROOT, 'tests', 'resources', 'media', 'avif', 'seq')
OUT = os.path.join(ROOT, 'tests', 'resources', 'media', 'avif', 'layers')


def leb(d, p):
    v = 0
    for i in range(8):
        b = d[p]
        p += 1
        v |= (b & 0x7F) << (7 * i)
        if not b & 0x80:
            break
    return v, p


def put_leb(v):
    out = bytearray()
    while True:
        b = v & 0x7F
        v >>= 7
        if v:
            out.append(b | 0x80)
        else:
            out.append(b)
            break
    return bytes(out)


def split(d):
    """Splits an OBU stream into (type, raw bytes, payload) tuples."""
    out = []
    p = 0
    while p < len(d):
        s = p
        h = d[p]
        p += 1
        typ = (h >> 3) & 15
        if (h >> 2) & 1:
            p += 1
        size, p = leb(d, p)
        out.append((typ, bytes(d[s:p + size]), bytes(d[p:p + size])))
        p += size
    return out


class Bits(object):
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def read(self, n):
        v = 0
        for _ in range(n):
            v = (v << 1) | ((self.data[self.pos >> 3] >> (7 - (self.pos & 7))) & 1)
            self.pos += 1
        return v


def bits_of(value, n):
    return [(value >> (n - 1 - i)) & 1 for i in range(n)]


def sequence_header_with_layers(payload, idcs):
    """Replaces the operating points of a sequence header by one operating point for every idc of idcs."""
    r = Bits(payload)
    head = [r.read(1) for _ in range(7)]  # seq_profile(3) still_picture reduced_still_picture_header timing_info_present initial_display_delay_present
    assert head[4] == 0 and head[5] == 0 and head[6] == 0, 'timing info or display delay present'
    assert r.read(5) == 0, 'the source has more than one operating point'
    r.read(12)
    level = r.read(5)
    tier = r.read(1) if level > 7 else 0
    start = r.pos
    # 맨 뒤의 1 비트(trailing bits)를 찾는다.
    total = len(payload) * 8
    end = total - 1
    while not (payload[end >> 3] >> (7 - (end & 7))) & 1:
        end -= 1
    bits = list(head) + bits_of(len(idcs) - 1, 5)
    for idc in idcs:
        bits += bits_of(idc, 12) + bits_of(level, 5)
        if level > 7:
            bits += bits_of(tier, 1)
    r2 = Bits(payload)
    r2.pos = start
    while r2.pos < end:
        bits.append(r2.read(1))
    bits.append(1)
    while len(bits) % 8:
        bits.append(0)
    out = bytearray()
    for i in range(0, len(bits), 8):
        v = 0
        for b in bits[i:i + 8]:
            v = (v << 1) | b
        out.append(v)
    return bytes(out)


def layer_obu(raw, spatial_id):
    """Adds an OBU extension header with spatial_id to an OBU that has none."""
    assert not (raw[0] >> 2) & 1
    return bytes([raw[0] | 4, spatial_id << 3]) + raw[1:]


def key_frames():
    """Returns (sequence header payload, [frame OBU (raw)] of the key frames, [their md5 of the planes])."""
    d = open(os.path.join(SEQ, 'seq_keyframes.obu'), 'rb').read()
    obus = split(d)
    seq = None
    frames = []
    for i, (typ, raw, payload) in enumerate(obus):
        if typ == 1 and seq is None:
            seq = payload
        if typ == 1:
            frames.append(obus[i + 1][1])  # key frame follows its sequence header
    return seq, frames


def stream(seq, tus):
    """tus: list of (frame for layer 0, frame for layer 1). Returns the OBU stream."""
    out = bytearray()
    shdr = sequence_header_with_layers(seq, [0x301, 0x101])
    for n, (f0, f1) in enumerate(tus):
        out += b'\x12\x00'  # temporal delimiter
        if n == 0:
            out += bytes([0x0A]) + put_leb(len(shdr)) + shdr
        out += layer_obu(f0, 0) + layer_obu(f1, 1)
    return bytes(out)


def dav1d_md5(path, oppoint, all_layers):
    """md5 of every picture libdav1d outputs for an operating point (all_layers: also the lower layers)."""
    cmd = ['ffmpeg', '-hide_banner', '-loglevel', 'error', '-c:v', 'libdav1d', '-oppoint', str(oppoint), '-alllayers', str(all_layers), '-f', 'obu', '-i', path,
           '-fps_mode', 'passthrough', '-f', 'rawvideo', '-pix_fmt', 'yuv420p', '-']
    data = subprocess.check_output(cmd)
    size = 96 * 64 * 3 // 2
    assert len(data) % size == 0
    return [hashlib.md5(data[i:i + size]).hexdigest() for i in range(0, len(data), size)]


def box(t, *parts):
    body = b''.join(parts)
    return struct.pack('>I', 8 + len(body)) + t + body


def fullbox(t, version, flags, *parts):
    return box(t, struct.pack('>I', (version << 24) | flags), *parts)


def av1c(seq):
    """The AV1CodecConfigurationRecord with the sequence header of the stream."""
    return bytes([0x81, 0x00, 0x0C, 0x00]) + bytes([0x0A]) + put_leb(len(seq)) + seq


def avif(seq, tu, props):
    """An AVIF file with one av01 item (the OBUs of tu) and the properties [(type, payload, essential)]."""
    ipco = box(b'ipco', box(b'ispe', struct.pack('>III', 0, 96, 64)), box(b'av1C', av1c(seq)), *[box(t, p) for t, p, e in props])
    assoc = [(1, 0), (2, 1)]
    for i, (t, p, e) in enumerate(props):
        assoc.append((3 + i, 1 if e else 0))
    ipma = fullbox(b'ipma', 0, 0, struct.pack('>I', 1), struct.pack('>HB', 1, len(assoc)), b''.join(struct.pack('>B', (e << 7) | i) for i, e in assoc))
    iprp = box(b'iprp', ipco, ipma)
    infe = fullbox(b'infe', 2, 0, struct.pack('>HH', 1, 0), b'av01', b'\x00')
    iinf = fullbox(b'iinf', 0, 0, struct.pack('>H', 1), infe)
    hdlr = fullbox(b'hdlr', 0, 0, struct.pack('>I', 0), b'pict', b'\x00' * 12, b'\x00')
    pitm = fullbox(b'pitm', 0, 0, struct.pack('>H', 1))
    ftyp = box(b'ftyp', b'avif', struct.pack('>I', 0), b'avif', b'mif1', b'miaf')
    # iloc 의 위치(offset)는 meta 상자의 크기를 알아야 하므로 자리표시자로 한 번 만들어 크기를 잰 뒤 다시 만든다.
    def make_meta(base):
        iloc = fullbox(b'iloc', 0, 0, bytes([0x44, 0x00]), struct.pack('>H', 1), struct.pack('>HHH', 1, 0, 1), struct.pack('>II', base, len(tu)))
        return fullbox(b'meta', 0, 0, hdlr, pitm, iloc, iinf, iprp)

    meta = make_meta(make_meta(0).__len__() + len(ftyp) + 8)
    return ftyp + meta + box(b'mdat', tu)


def main():
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    seq, frames = key_frames()
    assert len(frames) == 3
    k0, k4, k8 = frames
    rows = []
    # 시간 단위 하나(계층 0 = 키 프레임 0, 계층 1 = 키 프레임 4)와 둘(둘째는 8, 0)을 만든다.
    for name, tus in (('layers_1tu', [(k0, k4)]), ('layers_2tu', [(k0, k4), (k8, k0)])):
        data = stream(seq, tus)
        path = os.path.join(OUT, name + '.obu')
        open(path, 'wb').write(data)
        highest = dav1d_md5(path, 0, 0)
        every = dav1d_md5(path, 0, 1)
        lower = dav1d_md5(path, 1, 0)
        n = len(tus)
        assert len(highest) == n and len(every) == 2 * n and len(lower) == n, (len(highest), len(every), len(lower))
        # -alllayers 1 의 출력은 시간 단위마다 계층 0, 계층 1 순이다.
        layer0 = [every[2 * i] for i in range(n)]
        layer1 = [every[2 * i + 1] for i in range(n)]
        assert highest == layer1 and lower == layer0, name
        rows.append('|'.join([name, str(n), '96x64', ','.join(layer0), ','.join(layer1)]))
        if name == 'layers_1tu':
            # 기본 항목이 이 스트림의 시간 단위인 AVIF 정지 이미지 파일들: 속성 a1op(필수), lsel 의 조합.
            tu = data[2:]  # 시간 지시자를 뺀 시퀀스 헤더와 두 계층의 프레임
            shdr = sequence_header_with_layers(seq, [0x301, 0x101])
            for fname, props in (('item_default', []), ('item_op1', [(b'a1op', b'\x01', 1)]), ('item_op0', [(b'a1op', b'\x00', 1)]),
                                 ('item_lsel0', [(b'lsel', b'\x00\x00', 0)]), ('item_lsel1', [(b'lsel', b'\x00\x01', 0)]),
                                 ('item_lselall', [(b'lsel', b'\xff\xff', 0)]), ('item_lsel2', [(b'lsel', b'\x00\x02', 0)]),
                                 ('item_op5', [(b'a1op', b'\x05', 1)]), ('item_op1_lsel1', [(b'a1op', b'\x01', 1), (b'lsel', b'\x00\x01', 0)]),
                                 ('item_op1_lsel0', [(b'a1op', b'\x01', 1), (b'lsel', b'\x00\x00', 0)]),
                                 ('item_lsel9', [(b'lsel', b'\x00\x09', 0)])):
                open(os.path.join(OUT, fname + '.avif'), 'wb').write(avif(shdr, tu, props))
    # 두 시간 단위의 계층 스트림을 ffmpeg 의 AVIF 먹서로 감싼 이미지 시퀀스(샘플마다 두 계층)도 만든다.
    subprocess.check_call(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-framerate', '10', '-f', 'obu', '-i', os.path.join(OUT, 'layers_2tu.obu'),
                           '-c', 'copy', '-f', 'avif', os.path.join(OUT, 'avis_layers.avif')])
    open(os.path.join(OUT, 'index.txt'), 'w').write(
        '# name|temporal units|WxH|md5 of the layer 0 pictures (yuv420p planes, libdav1d)|md5 of the layer 1 pictures\n' + '\n'.join(rows) + '\n')
    print('\n'.join(rows))


main()
