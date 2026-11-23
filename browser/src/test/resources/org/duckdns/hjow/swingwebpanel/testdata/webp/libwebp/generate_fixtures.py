#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Generates the WebP fixtures of tests/src/org/duckdns/hjow/swingwebpanel/media/image/webp (plan item P18-03) with libwebp, through Pillow, and the
manifest with the pixels that libwebp decodes from them.

WebpDecoder 시험(P18-03)용 WebP 자료를 libwebp(Pillow 경유)로 만들고, libwebp 가 그 파일에서 디코딩한 픽셀의 목록(manifest.txt)을 만든다.

This script is documentation of how the data was made; the build and the tests do not run it. It needs Python 3, numpy and Pillow (with libwebp).
이 스크립트는 자료를 만든 방법을 기록한 것이며 빌드와 시험이 실행하지 않는다. Python 3, numpy, Pillow(libwebp 포함)가 필요하다.

Usage / 사용법:  python generate_fixtures.py <output directory>
Optional / 선택:  set the environment variable FFMPEG to an ffmpeg executable built with libwebp to add the files made with the presets of libwebp
(they use other loop filter strengths and sharpness values than Pillow can select). / 환경 변수 FFMPEG 에 libwebp 가 들어 있는 ffmpeg 실행 파일을
지정하면 libwebp 프리셋으로 만든 파일(Pillow 가 고를 수 없는 루프 필터 강도와 선명도)이 추가된다.

manifest.txt: one fixture per line, fields separated by "|":
    file name | width | height | loop count (0 = forever) | frames
  frames = comma separated  <delay in ms>:<FNV-1a 32 hash (hex) of the width*height*4 RGBA bytes of the whole frame canvas>
  delay is 0 for a still image; for an animation it is the delay of libwebp, where 10 ms or less is replaced by 100 ms (the policy of the decoder).
"""
import os
import random
import struct
import sys

import numpy as np
from PIL import Image, ImageDraw


def fnv(data):
    """FNV-1a 32 bit hash of bytes / 바이트의 FNV-1a 32비트 해시."""
    h = 0x811C9DC5
    for b in data:
        h = ((h ^ b) * 16777619) & 0xFFFFFFFF
    return h


def photo(w, h, seed, alpha=False):
    """A photo-like picture: gradients, shapes, lines and noise / 사진 같은 그림."""
    rng = np.random.RandomState(seed)
    y, x = np.mgrid[0:h, 0:w]
    r = (255 * x / max(w - 1, 1)).astype(np.float64)
    g = (255 * y / max(h - 1, 1)).astype(np.float64)
    b = 128 + 100 * np.sin(x / 7.0) * np.cos(y / 5.0)
    arr = np.stack([r, g, b], axis=-1)
    arr += rng.normal(0, 6, arr.shape)
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGB')
    d = ImageDraw.Draw(img)
    d.ellipse([w * 0.1, h * 0.15, w * 0.6, h * 0.8], fill=(240, 240, 250), outline=(10, 10, 10))
    d.rectangle([w * 0.65, h * 0.1, w * 0.85, h * 0.7], fill=(20, 30, 40))
    d.line([0, h - 1, w - 1, 0], fill=(200, 0, 200), width=2)
    d.polygon([(w * 0.3, h * 0.9), (w * 0.5, h * 0.55), (w * 0.7, h * 0.95)], fill=(250, 200, 20))
    if alpha:
        img = img.convert('RGBA')
        a = (255 * np.clip(1.2 - np.hypot(x - w / 2.0, y - h / 2.0) / (0.6 * max(w, h)), 0, 1)).astype(np.uint8)
        a[:max(h // 6, 1), :max(w // 5, 1)] = 0
        img.putalpha(Image.fromarray(a, 'L'))
    return img


def noise(w, h, seed, alpha=False):
    """Random pixels / 무작위 픽셀."""
    rng = np.random.RandomState(seed)
    return Image.fromarray(rng.randint(0, 256, (h, w, 4 if alpha else 3), dtype=np.uint8), 'RGBA' if alpha else 'RGB')


def flat(w, h, seed):
    """Large flat areas with a few small shapes / 큰 단색 영역과 작은 도형."""
    img = Image.new('RGB', (w, h), (200, 60, 50))
    d = ImageDraw.Draw(img)
    d.rectangle([w // 4, h // 4, w * 3 // 4, h * 3 // 4], fill=(40, 180, 70))
    d.ellipse([2, 2, 9, 9], fill=(30, 40, 220))
    return img


def stripes(w, h, seed):
    """Smooth vertical and horizontal gradients, favouring the V/H/TM predictors / V, H, TM 예측에 유리한 매끄러운 줄무늬."""
    y, x = np.mgrid[0:h, 0:w]
    arr = np.zeros((h, w, 3), dtype=np.float64)
    arr[:, :, 0] = np.where((x // 16) % 2 == 0, 40 + 3 * y, 200 - 2 * y)
    arr[:, :, 1] = np.where((y // 16) % 2 == 0, 30 + 3 * x, 220 - 2 * x)
    arr[:, :, 2] = 100 + 60 * np.sin(x / 11.0)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGB')


def palette_img(w, h, colors, seed, alpha=False):
    """A picture with exactly `colors` different colours in blocks and lines / 정확히 colors 가지 색의 그림."""
    rng = np.random.RandomState(seed)
    table = rng.randint(0, 256, (colors, 4 if alpha else 3), dtype=np.uint8)
    if alpha:
        table[:, 3] = np.where(np.arange(colors) % 3 == 0, 255, rng.randint(0, 256, colors))
    y, x = np.mgrid[0:h, 0:w]
    idx = ((x // 3) + 2 * (y // 5) + (x * y) // 17) % colors
    idx[::7, :] = (idx[::7, :] + 1) % colors
    return Image.fromarray(table[idx], 'RGBA' if alpha else 'RGB')


def pattern(w, h, seed):
    """Repeating tiles with small variations: long backward references and cache hits / 반복 타일: 긴 역참조와 캐시 적중."""
    rng = np.random.RandomState(seed)
    tile = rng.randint(0, 256, (8, 12, 3), dtype=np.uint8)
    arr = np.tile(tile, (h // 8 + 1, w // 12 + 1, 1))[:h, :w].copy()
    arr[rng.randint(0, h, 40), rng.randint(0, w, 40)] = rng.randint(0, 256, (40, 3), dtype=np.uint8)
    return Image.fromarray(arr, 'RGB')


def mixed(w, h, seed):
    """Regions of very different statistics, so that the encoder uses several prefix code groups / 통계가 다른 영역들."""
    rng = np.random.RandomState(seed)
    img = photo(w, h, seed)
    arr = np.array(img)
    arr[: h // 3, : w // 2] = rng.randint(0, 256, (h // 3, w // 2, 3), dtype=np.uint8)
    arr[h // 3: 2 * h // 3, w // 2:] = arr[h // 3: 2 * h // 3, w // 2:] // 64 * 64
    arr[2 * h // 3:, : w // 3] = (arr[2 * h // 3:, : w // 3] > 128) * 255
    return Image.fromarray(arr, 'RGB')


def clip_rect_rgba(w, h, boxes, seed):
    """Frames made of a transparent canvas with coloured boxes / 투명 캔버스 위의 색 상자."""
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for box, color in boxes:
        d.rectangle(box, fill=color)
    return img


def riff(chunks):
    """Builds a RIFF WEBP file from (fourcc, payload) pairs / (fourcc, 내용) 쌍으로 RIFF WEBP 파일을 만든다."""
    body = b'WEBP'
    for fourcc, payload in chunks:
        body += fourcc + struct.pack('<I', len(payload)) + payload + (b'\0' if len(payload) & 1 else b'')
    return b'RIFF' + struct.pack('<I', len(body)) + body


def chunks_of(data):
    """Splits a WebP file into (fourcc, payload) pairs / WebP 파일을 (fourcc, 내용) 쌍으로 나눈다."""
    out = []
    p = 12
    while p + 8 <= len(data):
        fourcc = data[p:p + 4]
        size = struct.unpack('<I', data[p + 4:p + 8])[0]
        out.append((fourcc, data[p + 8:p + 8 + size]))
        p += 8 + size + (size & 1)
    return out


def u24(v):
    return struct.pack('<I', v)[:3]


def vp8x(flags, w, h):
    return (b'VP8X', struct.pack('<B3x', flags) + u24(w - 1) + u24(h - 1))


def anmf(x, y, w, h, duration, dispose, no_blend, sub_chunks):
    """An ANMF chunk / ANMF 청크."""
    flags = (1 if dispose else 0) | (2 if no_blend else 0)
    payload = u24(x // 2) + u24(y // 2) + u24(w - 1) + u24(h - 1) + u24(duration) + struct.pack('<B', flags)
    for fourcc, p in sub_chunks:
        payload += fourcc + struct.pack('<I', len(p)) + p + (b'\0' if len(p) & 1 else b'')
    return (b'ANMF', payload)


def encode_bytes(img, **kw):
    import io
    buf = io.BytesIO()
    img.save(buf, 'WEBP', **kw)
    return buf.getvalue()


def reference(path):
    """Decodes with libwebp and returns (w, h, loop, [(delay, hash)]) / libwebp 로 디코딩해 기준값을 만든다."""
    im = Image.open(path)
    w, h = im.size
    loop = im.info.get('loop', 0)
    n = getattr(im, 'n_frames', 1)
    frames = []
    for i in range(n):
        im.seek(i)
        im.load()
        d = im.info.get('duration', 0) if n > 1 else 0
        if n > 1 and d <= 10:
            d = 100
        frames.append((d, fnv(im.convert('RGBA').tobytes())))
    return w, h, loop, frames


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    files = {}

    # ---- lossy images / 손실 영상 ----
    for (w, h, q, m, name) in [(96, 64, 75, 4, 'photo_96x64_q75_m4'), (97, 65, 30, 6, 'photo_97x65_q30_m6'), (33, 17, 100, 4, 'photo_33x17_q100'),
                               (130, 40, 50, 0, 'photo_130x40_q50_m0'), (200, 120, 85, 6, 'photo_200x120_q85_m6'), (64, 64, 5, 4, 'photo_64x64_q5')]:
        files['lossy_' + name] = encode_bytes(photo(w, h, w * h), lossless=False, quality=q, method=m)
    for (w, h) in [(1, 1), (1, 3), (3, 1), (2, 2), (15, 15), (16, 16), (17, 17), (31, 5), (5, 31)]:
        files['lossy_photo_%dx%d' % (w, h)] = encode_bytes(photo(w, h, w + 7 * h), lossless=False, quality=80, method=4)
    files['lossy_flat_96x80'] = encode_bytes(flat(96, 80, 1), lossless=False, quality=60, method=4)
    files['lossy_flat_96x80_q100'] = encode_bytes(flat(96, 80, 1), lossless=False, quality=100, method=6)
    files['lossy_stripes_80x48'] = encode_bytes(stripes(80, 48, 2), lossless=False, quality=70, method=4)
    files['lossy_stripes_80x48_m0'] = encode_bytes(stripes(80, 48, 2), lossless=False, quality=40, method=0)
    files['lossy_noise_40x40_q100'] = encode_bytes(noise(40, 40, 3), lossless=False, quality=100, method=4)
    files['lossy_noise_40x40_q10'] = encode_bytes(noise(40, 40, 3), lossless=False, quality=10, method=2)
    files['lossy_pattern_90x60'] = encode_bytes(pattern(90, 60, 4), lossless=False, quality=75, method=4)

    # ---- lossy images with the presets of libwebp (loop filter sharpness and strength), through ffmpeg if the FFMPEG variable names it ----
    ffmpeg = os.environ.get('FFMPEG')
    if ffmpeg:
        import subprocess
        import tempfile
        text_img = Image.new('RGB', (160, 96), (255, 255, 255))
        td = ImageDraw.Draw(text_img)
        for i in range(0, 88, 12):
            for j in range(0, 152, 10):
                if (i * 7 + j * 3) % 5 < 2:
                    td.rectangle([j, i, j + 5, i + 7], fill=(0, 0, 0))
        for src_name, src_img, presets in [('photo', photo(112, 80, 1), ('picture', 'photo', 'drawing', 'icon', 'text')),
                                           ('text', text_img, ('default', 'drawing', 'text'))]:
            with tempfile.TemporaryDirectory() as tmp:
                png = os.path.join(tmp, 'in.png')
                src_img.save(png)
                for preset in presets:
                    dst = os.path.join(tmp, 'out.webp')
                    subprocess.run([ffmpeg, '-y', '-loglevel', 'error', '-i', png, '-c:v', 'libwebp', '-lossless', '0', '-quality', '70', '-preset',
                                    preset, '-frames:v', '1', dst], check=True)
                    with open(dst, 'rb') as f:
                        files['lossy_ffmpeg_%s_%s' % (src_name, preset)] = f.read()

    # ---- lossy with alpha / 알파가 있는 손실 영상 ----
    for (w, h, aq, m, name) in [(48, 48, 100, 4, 'a100'), (48, 48, 50, 4, 'a50'), (64, 40, 100, 0, 'a100_m0'), (65, 41, 20, 6, 'a20_m6'),
                                (1, 1, 100, 4, 'one')]:
        files['lossy_alpha_%s' % name] = encode_bytes(photo(w, h, 5 + w, True), lossless=False, quality=75, alpha_quality=aq, method=m, exact=True)
    files['lossy_alpha_noise'] = encode_bytes(noise(37, 29, 6, True), lossless=False, quality=80, alpha_quality=100, method=4, exact=True)
    files['lossy_alpha_palette'] = encode_bytes(palette_img(60, 40, 5, 7, True), lossless=False, quality=80, alpha_quality=100, method=4, exact=True)

    # ---- lossless images, all encoder efforts / 무손실 영상, 모든 압축 노력 ----
    for m in range(7):
        files['lossless_photo_96x64_m%d' % m] = encode_bytes(photo(96, 64, 11), lossless=True, quality=100 if m >= 4 else 50, method=m)
    for q in (0, 100):
        files['lossless_photo_80x60_q%d' % q] = encode_bytes(photo(80, 60, 12), lossless=True, quality=q, method=6)
    for (w, h) in [(1, 1), (1, 9), (9, 1), (2, 3), (17, 17), (33, 5)]:
        files['lossless_photo_%dx%d' % (w, h)] = encode_bytes(photo(w, h, w + h), lossless=True, quality=100, method=6)
    for n in (2, 3, 4, 5, 15, 16, 17, 100, 255, 256):
        files['lossless_palette_%d' % n] = encode_bytes(palette_img(50, 30, n, n), lossless=True, quality=100, method=6)
    files['lossless_palette_4_w3'] = encode_bytes(palette_img(3, 40, 4, 99), lossless=True, quality=100, method=6)
    files['lossless_palette_16_w19'] = encode_bytes(palette_img(19, 21, 16, 98), lossless=True, quality=100, method=6)
    files['lossless_noise_32x32'] = encode_bytes(noise(32, 32, 13), lossless=True, quality=100, method=6)
    files['lossless_pattern_120x80'] = encode_bytes(pattern(120, 80, 14), lossless=True, quality=100, method=6)
    files['lossless_pattern_120x80_m0'] = encode_bytes(pattern(120, 80, 14), lossless=True, quality=20, method=0)
    files['lossless_stripes_80x48'] = encode_bytes(stripes(80, 48, 15), lossless=True, quality=100, method=6)
    files['lossless_mixed_200x150'] = encode_bytes(mixed(200, 150, 16), lossless=True, quality=100, method=6)
    files['lossless_mixed_200x150_m4'] = encode_bytes(mixed(200, 150, 16), lossless=True, quality=75, method=4)
    files['lossless_alpha_photo_50x40'] = encode_bytes(photo(50, 40, 18, True), lossless=True, quality=100, method=6, exact=True)
    files['lossless_alpha_photo_50x40_noexact'] = encode_bytes(photo(50, 40, 18, True), lossless=True, quality=100, method=4, exact=False)
    files['lossless_alpha_noise'] = encode_bytes(noise(30, 20, 19, True), lossless=True, quality=100, method=6, exact=True)
    files['lossless_alpha_palette_9'] = encode_bytes(palette_img(40, 30, 9, 20, True), lossless=True, quality=100, method=6, exact=True)

    # ---- containers made by hand around libwebp bitstreams / libwebp 비트스트림을 손으로 감싼 컨테이너 ----
    lossy = chunks_of(encode_bytes(photo(40, 30, 21), lossless=False, quality=70, method=4))[-1]
    lossless = chunks_of(encode_bytes(photo(40, 30, 22), lossless=True, quality=100, method=4))[-1]
    files['container_simple_lossy'] = riff([lossy])
    files['container_simple_lossless'] = riff([lossless])
    files['container_vp8x_meta'] = riff([vp8x(0x20 | 0x08 | 0x04, 40, 30), (b'ICCP', b'not a real profile!'), (b'ABCD', b'odd'), lossy, (b'EXIF', b'Exif\0\0II*\0'),
                                         (b'XMP ', b'<x/>')])
    files['container_unknown_before'] = riff([vp8x(0, 40, 30), (b'ZZZZ', b'1234567'), lossless])

    # ---- animations from libwebp's encoder / libwebp 애니메이션 인코더 ----
    frames = []
    for i in range(6):
        im = Image.new('RGBA', (64, 48), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        d.rectangle([4 + 8 * i, 6 + 3 * i, 24 + 8 * i, 26 + 3 * i], fill=(220, 40 + 30 * i, 40, 255))
        d.ellipse([30, 10, 55, 40], outline=(20, 20, 220, 180), width=3)
        frames.append(im)
    files['anim_lossless_boxes'] = encode_bytes(frames[0], save_all=True, append_images=frames[1:], duration=[40, 80, 120, 40, 200, 60], loop=3,
                                                lossless=True, quality=100, method=4)
    files['anim_lossless_minsize'] = encode_bytes(frames[0], save_all=True, append_images=frames[1:], duration=50, loop=0, lossless=True,
                                                  quality=100, method=6, minimize_size=True)
    files['anim_lossy_boxes'] = encode_bytes(frames[0], save_all=True, append_images=frames[1:], duration=[30, 30, 30, 30, 30, 30], loop=0,
                                             lossless=False, quality=80, method=4)
    ph = [photo(48, 40, 30 + i, False) for i in range(5)]
    files['anim_lossy_photos'] = encode_bytes(ph[0], save_all=True, append_images=ph[1:], duration=70, loop=2, lossless=False, quality=60, method=4)
    files['anim_mixed'] = encode_bytes(frames[0], save_all=True, append_images=frames[1:], duration=33, loop=0, lossless=False, quality=70,
                                       allow_mixed=True, method=4, kmin=1, kmax=3)
    fade = []
    for i in range(5):
        im = Image.new('RGBA', (40, 40), (30, 200, 60, 40 + 50 * i))
        ImageDraw.Draw(im).ellipse([5, 5, 35, 35], fill=(240, 30, 30, 255 - 40 * i))
        fade.append(im)
    files['anim_alpha_fade'] = encode_bytes(fade[0], save_all=True, append_images=fade[1:], duration=60, loop=0, lossless=True, quality=100,
                                            method=4, exact=True)
    files['anim_alpha_fade_lossy'] = encode_bytes(fade[0], save_all=True, append_images=fade[1:], duration=60, loop=0, lossless=False, quality=90,
                                                  alpha_quality=100, method=4, exact=True)
    files['anim_two_frames_zero_delay'] = encode_bytes(frames[0], save_all=True, append_images=frames[1:2], duration=[0, 5], loop=1, lossless=True)

    # ---- animations assembled by hand: offsets, blend and dispose methods, background colour, unknown chunks ----
    def lossless_chunk(img):
        return chunks_of(encode_bytes(img, lossless=True, quality=100, method=4, exact=True))[-1]

    def lossy_frame(img, aq=100):
        parts = chunks_of(encode_bytes(img, lossless=False, quality=85, alpha_quality=aq, method=4, exact=True))
        return [p for p in parts if p[0] in (b'ALPH', b'VP8 ')]

    base = Image.new('RGBA', (60, 40), (10, 120, 200, 255))
    box_a = clip_rect_rgba(20, 14, [((0, 0, 19, 13), (250, 20, 20, 255)), ((5, 4, 12, 9), (0, 0, 0, 0))], 1)
    box_b = clip_rect_rgba(16, 10, [((0, 0, 15, 9), (30, 250, 30, 128))], 2)
    box_c = clip_rect_rgba(30, 22, [((2, 2, 27, 19), (250, 250, 20, 200)), ((10, 8, 20, 14), (20, 20, 250, 90))], 3)
    for dispose1 in (False, True):
        for blend2 in (False, True):
            name = 'anim_hand_d%d_b%d' % (int(dispose1), int(blend2))
            files[name] = riff([vp8x(0x02 | 0x10, 60, 40), (b'ANIM', struct.pack('<I', 0xFF336699) + struct.pack('<H', 4)),
                                anmf(0, 0, 60, 40, 100, False, False, [lossless_chunk(base)]),
                                anmf(10, 8, 20, 14, 50, dispose1, False, [lossless_chunk(box_a)]),
                                anmf(14, 12, 16, 10, 70, False, not blend2, [lossless_chunk(box_b)]),
                                anmf(20, 16, 30, 22, 90, True, False, [lossless_chunk(box_c), (b'JUNK', b'xyz')]),
                                anmf(0, 0, 60, 40, 20, False, False, [lossless_chunk(base)])])
    files['anim_hand_lossy_alpha'] = riff([vp8x(0x02 | 0x10, 60, 40), (b'ANIM', struct.pack('<I', 0) + struct.pack('<H', 0)),
                                           anmf(0, 0, 60, 40, 100, False, False, lossy_frame(base)),
                                           anmf(10, 8, 30, 22, 50, False, False, lossy_frame(photo(30, 22, 40, True))),
                                           anmf(4, 2, 24, 18, 60, True, False, lossy_frame(photo(24, 18, 41, True), 50)),
                                           anmf(30, 20, 30, 20, 10, False, False, lossy_frame(photo(30, 20, 42, True)))])
    files['anim_hand_odd_offsets'] = riff([vp8x(0x02 | 0x10, 33, 21), (b'ANIM', struct.pack('<I', 0x80FFFFFF) + struct.pack('<H', 1)),
                                           anmf(0, 0, 33, 21, 40, True, True, [lossless_chunk(photo(33, 21, 43, True))]),
                                           anmf(30, 18, 3, 3, 40, False, False, [lossless_chunk(clip_rect_rgba(3, 3, [((0, 0, 2, 2), (255, 0, 0, 100))], 4))]),
                                           anmf(0, 0, 1, 1, 40, False, False, [lossless_chunk(clip_rect_rgba(1, 1, [((0, 0, 0, 0), (0, 255, 0, 255))], 5))])])

    names = sorted(files)
    with open(os.path.join(out, 'manifest.txt'), 'w') as mf:
        for name in names:
            path = os.path.join(out, name + '.webp')
            with open(path, 'wb') as f:
                f.write(files[name])
            w, h, loop, frames_ref = reference(path)
            mf.write('%s.webp|%d|%d|%d|%s\n' % (name, w, h, loop, ','.join('%d:%x' % (d, hv) for d, hv in frames_ref)))
    print('wrote %d fixtures, %d bytes' % (len(names), sum(len(v) for v in files.values())))


if __name__ == '__main__':
    main()
