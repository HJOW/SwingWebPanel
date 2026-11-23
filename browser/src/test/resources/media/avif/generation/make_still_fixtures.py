#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Makes more single frame AV1 streams of tests/resources/media/avif/av1 (plan item P18-03): intra block copy variants and super resolution.

usage: python -I make_still_fixtures.py <ffmpeg executable> <directory tests/resources/media/avif>

Needs Python 3 with Pillow and numpy and an ffmpeg with the encoder libaom-av1 and the decoder libdav1d (the reference decoder).  The product
code and the tests do not need any of them; the streams and the md5 of the planes that libdav1d decodes are committed (av1/<name>.obu and the
lines of av1/index.txt, whose format is described in make_fixtures.py).  The pictures use fixed random seeds.
"""
import hashlib
import os
import re
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

FFMPEG = sys.argv[1]
OUT = sys.argv[2]
AV1 = os.path.join(OUT, 'av1')
TMP = os.path.join(OUT, 'generation', 'tmp')
os.makedirs(TMP, exist_ok=True)


def make_image(w, h, seed):
    """A synthetic natural looking picture (gradients, noise, shapes)."""
    rng = np.random.RandomState(seed)
    y, x = np.mgrid[0:h, 0:w]
    img = np.zeros((h, w, 3), dtype=np.float64)
    img[..., 0] = 255 * x / max(w - 1, 1)
    img[..., 1] = 255 * y / max(h - 1, 1)
    img[..., 2] = 128 + 100 * np.sin(x / 7.0) * np.cos(y / 11.0)
    img += rng.normal(0, 3, img.shape)
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    for _ in range(6):
        x0 = rng.randint(0, w)
        y0 = rng.randint(0, h)
        d.rectangle([x0, y0, x0 + rng.randint(5, 30), y0 + rng.randint(5, 30)], fill=tuple(int(v) for v in rng.randint(0, 255, 3)))
    for i in range(5):
        d.line([rng.randint(0, w), rng.randint(0, h), rng.randint(0, w), rng.randint(0, h)],
               fill=tuple(int(v) for v in rng.randint(0, 255, 3)), width=1 + i % 3)
    d.ellipse([w // 4, h // 4, w // 2, h // 2], outline=(255, 255, 255), width=2)
    return im


def make_screen(w, h, seed):
    """A screenshot like picture: flat colours, repeated text and rectangles (intra block copy finds the repetitions)."""
    import random
    random.seed(seed)
    im = Image.new('RGB', (w, h), (255, 255, 255))
    d = ImageDraw.Draw(im)
    cols = [(0, 0, 0), (200, 30, 30), (30, 90, 200), (20, 150, 60), (240, 200, 20), (120, 120, 120)]
    for _ in range(20):
        x = random.randint(0, w - 30)
        y = random.randint(0, h - 16)
        d.rectangle([x, y, x + random.randint(8, 50), y + random.randint(6, 24)], fill=random.choice(cols))
    for i in range(14):
        d.text((random.randint(0, w - 50), random.randint(0, h - 12)), 'AV1 intrabc %d' % (i % 4), fill=random.choice(cols))
    # repeated blocks so that copying earlier parts of the picture pays
    for i in range(0, h - 24, 40):
        for j in range(0, w - 60, 90):
            d.text((j + 4, i + 4), 'Hello copy', fill=(0, 0, 0))
            d.rectangle([j + 70, i + 6, j + 80, i + 16], fill=(30, 90, 200))
    return im


def run(args):
    r = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        raise RuntimeError('command failed: %s\n%s' % (' '.join(args), r.stderr.decode('utf-8', 'replace')[-2000:]))
    return r


def features(obu):
    """The coding tools that the headers of a stream announce (from ffmpeg's trace_headers)."""
    r = subprocess.run([FFMPEG, '-hide_banner', '-i', obu, '-c', 'copy', '-bsf:v', 'trace_headers', '-f', 'null', '-'], stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    t = r.stderr.decode('utf-8', 'replace')
    out = []
    for name in ('base_q_idx', 'use_128x128_superblock', 'allow_screen_content_tools', 'allow_intrabc', 'tile_cols_log2', 'tile_rows_log2',
                 'enable_superres', 'use_superres', 'coded_denom', 'cdef_bits', 'reduced_tx_set'):
        m = re.search(r'\b' + re.escape(name) + r'\s+[01]+ = (-?\d+)', t)
        if m:
            out.append('%s=%s' % (name, m.group(1)))
    lr = []
    for i in range(3):
        m = re.search(r'\blr_type\[%d\]\s+[01]+ = (-?\d+)' % i, t)
        lr.append(m.group(1) if m else 'None')
    out.append('lr_type=%s' % '/'.join(lr))
    return ' '.join(out)


AOM = ['-c:v', 'libaom-av1', '-still-picture', '1']
lines = {}
order = []


def stream(name, src, pix, extra, scale_to=None):
    """Encodes one picture, decodes it with libdav1d and records the md5 of the planes (8 bit bytes or 16 bit words)."""
    png = os.path.join(TMP, name + '.png')
    src.save(png)
    obu = os.path.join(AV1, name + '.obu')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-i', png, '-frames:v', '1', '-pix_fmt', pix] + AOM + list(extra) + ['-f', 'obu', obu])
    yuv = os.path.join(TMP, name + '.yuv')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-c:v', 'libdav1d', '-i', obu, '-pix_fmt', pix, '-f', 'rawvideo', yuv])
    data = open(yuv, 'rb').read()
    os.remove(yuv)
    bd = 12 if '12' in pix else (10 if '10' in pix else 8)
    planes = 1 if pix.startswith('gray') else 3
    subx, suby = 1, 1
    if '444' in pix:
        subx, suby = 0, 0
    elif '422' in pix:
        subx, suby = 1, 0
    w, h = src.size
    lines[name] = '%s|%d|%d|%d|%d|%d|%d|%s|%s' % (name, w, h, bd, planes, subx, suby, hashlib.md5(data).hexdigest(), features(obu))
    order.append(name)


SCREEN = ['tune-content=screen:enable-intrabc=1']
# intra block copy: formats, sizes, tools that interact with it (the 128 superblock changes the delay rules, tiles limit the copy area)
stream('ibc_444', make_screen(160, 112, 31), 'yuv444p', ['-crf', '20', '-cpu-used', '1', '-aom-params', SCREEN[0]])
stream('ibc_422', make_screen(160, 112, 32), 'yuv422p', ['-crf', '20', '-cpu-used', '1', '-aom-params', SCREEN[0]])
stream('ibc_10bit', make_screen(160, 112, 33), 'yuv420p10le', ['-crf', '20', '-cpu-used', '1', '-aom-params', SCREEN[0]])
stream('ibc_12bit', make_screen(256, 160, 60), 'yuv420p12le', ['-crf', '30', '-cpu-used', '2', '-aom-params', SCREEN[0]])
stream('ibc_gray', make_screen(160, 112, 61), 'gray', ['-crf', '20', '-cpu-used', '1', '-aom-params', SCREEN[0]])
stream('ibc_sb128', make_screen(320, 240, 36), 'yuv420p', ['-crf', '20', '-cpu-used', '1', '-aom-params', SCREEN[0] + ':sb-size=128'])
stream('ibc_tiles', make_screen(320, 240, 37), 'yuv420p', ['-crf', '20', '-cpu-used', '1', '-tiles', '2x2', '-aom-params', SCREEN[0]])
stream('ibc_odd', make_screen(203, 117, 38), 'yuv420p', ['-crf', '25', '-cpu-used', '1', '-aom-params', SCREEN[0]])
stream('ibc_lossless', make_screen(160, 96, 60), 'yuv420p', ['-crf', '0', '-cpu-used', '2', '-aom-params', SCREEN[0] + ':lossless=1'])
stream('ibc_big', make_screen(640, 400, 40), 'yuv420p', ['-crf', '30', '-cpu-used', '2', '-aom-params', SCREEN[0]])
stream('ibc_lq', make_screen(192, 128, 42), 'yuv420p', ['-crf', '55', '-cpu-used', '2', '-aom-params', SCREEN[0]])

# super resolution: only SVT-AV1 can code it (ffmpeg's libaom wrapper has no option for it); the key frame of every stream is coded at a reduced
# width (the denominator of the ratio is 9..16 eighths) and upscaled by the decoder; the loop restoration and CDEF work on the upscaled picture
def svt_stream(name, src, pix, params, crf='35', preset='6'):
    """Like stream(), with the SVT-AV1 encoder (the only 8 and 10 bit 4:2:0 encoder of this PC that codes super resolution)."""
    png = os.path.join(TMP, name + '.png')
    src.save(png)
    obu = os.path.join(AV1, name + '.obu')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-i', png, '-frames:v', '1', '-pix_fmt', pix, '-c:v', 'libsvtav1', '-crf', crf,
         '-preset', preset, '-svtav1-params', params, '-f', 'obu', obu])
    yuv = os.path.join(TMP, name + '.yuv')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-c:v', 'libdav1d', '-i', obu, '-pix_fmt', pix, '-f', 'rawvideo', yuv])
    data = open(yuv, 'rb').read()
    os.remove(yuv)
    # SVT-AV1 may code a slightly larger picture than the source (it pads the size to a multiple of 8): the size is the decoded one
    r = subprocess.run([FFMPEG, '-hide_banner', '-i', obu], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    m = re.search(r'Video: av1.*?, (\d+)x(\d+)', r.stderr.decode('utf-8', 'replace'))
    w, h = int(m.group(1)), int(m.group(2))
    bd = 10 if '10' in pix else 8
    lines[name] = '%s|%d|%d|%d|3|1|1|%s|%s' % (name, w, h, bd, hashlib.md5(data).hexdigest(), features(obu))
    order.append(name)


def superres(denom):
    return 'superres-mode=1:superres-denom=%d:superres-kf-mode=1:superres-kf-denom=%d' % (denom, denom)


for d in range(9, 17):
    svt_stream('sr_d%d' % d, make_image(160, 120, 70 + d), 'yuv420p', superres(d))
svt_stream('sr_odd_d9', make_image(203, 117, 80), 'yuv420p', superres(9))
svt_stream('sr_odd_d13', make_image(161, 121, 81), 'yuv420p', superres(13))
svt_stream('sr_10bit_d11', make_image(160, 120, 82), 'yuv420p10le', superres(11))
svt_stream('sr_wide_d14', make_image(400, 240, 83), 'yuv420p', superres(14), crf='30')
svt_stream('sr_lr_d12', make_image(256, 192, 84), 'yuv420p', superres(12) + ':enable-restoration=1', crf='45')
svt_stream('sr_nocdef_d10', make_image(160, 120, 85), 'yuv420p', superres(10) + ':enable-cdef=0:enable-restoration=1', crf='40')
svt_stream('sr_tiles_d12', make_image(320, 192, 86), 'yuv420p', superres(12) + ':tile-columns=1:tile-rows=1')
svt_stream('sr_big_d15', make_image(640, 360, 87), 'yuv420p', superres(15), crf='35')

# merge into the index (an existing line of the same name is replaced)
index_path = os.path.join(AV1, 'index.txt')
old = open(index_path, encoding='utf-8').read().split('\n')
result = []
for line in old:
    if line and line.split('|')[0] not in lines:
        result.append(line)
for n in order:
    result.append(lines[n])
with open(index_path, 'w', encoding='utf-8', newline='\n') as f:
    f.write('\n'.join(result) + '\n')
print('wrote %d streams' % len(order))
