#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Creates the AVIF / AV1 test inputs of tests/resources/media/avif (plan item P18-03).

Needs: Python 3 with Pillow (with AVIF support, libavif) and numpy, and an ffmpeg build with libaom-av1, librav1e, libsvtav1 (encoders) and
libdav1d (decoder). The product code does not need any of them; the results are committed.

usage: make_fixtures.py <ffmpeg executable> <output directory (tests/resources/media/avif)>

Output:
  av1/<name>.obu    raw AV1 temporal unit (OBU stream) made by an encoder
  av1/index.txt     name|width|height|bitDepth|numPlanes|subX|subY|md5 of the planes decoded by libdav1d|features
                    (planes are written as 8 bit bytes, or as 16 bit little endian words if the bit depth is above 8)
  avif/<name>.avif  files written by Pillow (libavif) or by ffmpeg
  avif/<name>.png   the same file decoded by Pillow (libavif + dav1d) to RGBA; the reference of the colour conversion
  avif/index.txt    name|width|height|description
Every stream uses a fixed random seed for its source image, so that the generation is repeatable with the same encoder versions.
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
AVIF = os.path.join(OUT, 'avif')
TMP = os.path.join(OUT, '_tmp')
for d in (AV1, AVIF, TMP):
    os.makedirs(d, exist_ok=True)


def make_image(w, h, seed, kind='mix'):
    """A synthetic picture: gradients, noise, rectangles, lines and an ellipse (so that all prediction and transform tools are used)."""
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
    """A picture with few flat colours and text, like a screenshot (palette mode)."""
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
        d.text((random.randint(0, w - 50), random.randint(0, h - 12)), 'AV1 palette %d' % i, fill=random.choice(cols))
    return im


def run(args):
    r = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        raise RuntimeError('command failed: %s\n%s' % (' '.join(args), r.stderr.decode('utf-8', 'replace')[-2000:]))
    return r


def features(obu):
    """The coding tools that the sequence/frame header of a stream announces (from ffmpeg's trace_headers)."""
    r = subprocess.run([FFMPEG, '-hide_banner', '-i', obu, '-c', 'copy', '-bsf:v', 'trace_headers', '-f', 'null', '-'],
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    t = r.stderr.decode('utf-8', 'replace')

    def val(name):
        m = re.search(r'\b' + re.escape(name) + r'\s+[01]+ = (-?\d+)', t)
        return int(m.group(1)) if m else None
    f = []
    for name in ('base_q_idx', 'use_128x128_superblock', 'allow_screen_content_tools', 'allow_intrabc', 'using_qmatrix', 'segmentation_enabled',
                 'delta_q_present', 'delta_lf_present', 'tile_cols_log2', 'tile_rows_log2', 'cdef_bits', 'reduced_tx_set', 'apply_grain',
                 'use_superres', 'enable_intra_edge_filter', 'enable_filter_intra'):
        v = val(name)
        if v is not None:
            f.append('%s=%d' % (name, v))
    lr = [val('lr_type[%d]' % i) for i in range(3)]
    if any(v is not None for v in lr):
        f.append('lr_type=%s' % '/'.join(str(v) for v in lr))
    lf = [val('loop_filter_level[%d]' % i) for i in range(2)]
    f.append('loop_filter_level=%s' % '/'.join(str(v) for v in lf))
    return ' '.join(f)


index = []


def stream(name, src, pix, encoder, extra=(), decode=True):
    """Encodes one picture with an encoder, decodes it with libdav1d and records the md5 of the planes."""
    tmp_png = os.path.join(TMP, name + '.png')
    src.save(tmp_png)
    obu = os.path.join(AV1, name + '.obu')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-i', tmp_png, '-frames:v', '1', '-pix_fmt', pix] + encoder + list(extra) +
        ['-f', 'obu', obu])
    w, h = src.size
    feats = features(obu)
    if not decode:
        index.append('%s|%d|%d|0|0|0|0|-|%s' % (name, w, h, feats))
        return
    yuv = os.path.join(TMP, name + '.yuv')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-c:v', 'libdav1d', '-i', obu, '-pix_fmt', pix, '-f', 'rawvideo', yuv])
    data = open(yuv, 'rb').read()
    bd = 8
    if '10' in pix:
        bd = 10
    if '12' in pix:
        bd = 12
    planes = 1 if pix == 'gray' else 3
    subx, suby = 1, 1
    if '444' in pix:
        subx, suby = 0, 0
    elif '422' in pix:
        subx, suby = 1, 0
    elif pix == 'gray':
        subx, suby = 1, 1
    index.append('%s|%d|%d|%d|%d|%d|%d|%s|%s' % (name, w, h, bd, planes, subx, suby, hashlib.md5(data).hexdigest(), feats))


AOM = ['-c:v', 'libaom-av1', '-still-picture', '1']

# --- AV1 streams -------------------------------------------------------------------------------------------------------------------------
base = make_image(64, 48, 1)
stream('aom_420_q30', base, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '4'])
stream('aom_420_q55_filters', make_image(160, 120, 2), 'yuv420p', AOM, ['-crf', '55', '-cpu-used', '2'])
stream('aom_420_odd_size', make_image(33, 17, 3), 'yuv420p', AOM, ['-crf', '25', '-cpu-used', '4'])
stream('aom_420_1x1', make_image(1, 1, 4), 'yuv420p', AOM, ['-crf', '25', '-cpu-used', '4'])
stream('aom_444_q30', make_image(96, 64, 5), 'yuv444p', AOM, ['-crf', '30', '-cpu-used', '4'])
stream('aom_422_q30', make_image(96, 64, 6), 'yuv422p', AOM, ['-crf', '30', '-cpu-used', '4'])
stream('aom_mono_q30', make_image(96, 64, 7), 'gray', AOM, ['-crf', '30', '-cpu-used', '4'])
stream('aom_10bit_420', make_image(96, 64, 8), 'yuv420p10le', AOM, ['-crf', '30', '-cpu-used', '4'])
stream('aom_12bit_444', make_image(64, 64, 9), 'yuv444p12le', AOM, ['-crf', '30', '-cpu-used', '4'])
stream('aom_lossless_444', make_image(48, 40, 10), 'yuv444p', AOM, ['-lossless', '1', '-cpu-used', '6'])
stream('aom_tiles_2x2', make_image(160, 128, 11), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '6', '-tiles', '2x2'])
stream('aom_tile_groups', make_image(160, 128, 27), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '6', '-tiles', '2x2', '-aom-params', 'num-tile-groups=2'])
stream('aom_sb128', make_image(160, 128, 12), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '6', '-aom-params', 'sb-size=128'])
stream('aom_palette_444', make_screen(160, 96, 13), 'yuv444p', AOM, ['-crf', '20', '-cpu-used', '1', '-aom-params', 'tune-content=screen:enable-intrabc=0'])
stream('aom_palette_420', make_screen(160, 96, 14), 'yuv420p', AOM, ['-crf', '20', '-cpu-used', '1', '-aom-params', 'tune-content=screen:enable-intrabc=0'])
stream('aom_deltaq_lf', make_image(320, 240, 15), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '6', '-aom-params', 'deltaq-mode=3:delta-lf-mode=1'])
stream('aom_lr_320x240', make_image(320, 240, 41), 'yuv420p', AOM, ['-crf', '35', '-cpu-used', '1'])
stream('aom_qm', make_image(96, 64, 17), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '4', '-aom-params', 'enable-qm=1:qm-min=0:qm-max=15'])
stream('aom_reduced_tx', make_image(96, 64, 18), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '4', '-aom-params', 'reduced-tx-type-set=1'])
stream('aom_no_edge_filter', make_image(96, 64, 19), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '4', '-aom-params', 'enable-intra-edge-filter=0'])
stream('aom_no_tx64', make_image(160, 128, 20), 'yuv420p', AOM, ['-crf', '40', '-cpu-used', '4', '-aom-params', 'enable-tx64=0'])
stream('aom_restoration_only', make_image(160, 128, 21), 'yuv420p', AOM, ['-crf', '45', '-cpu-used', '3', '-aom-params', 'enable-cdef=0:enable-restoration=1'])
stream('aom_cdef_only', make_image(160, 128, 22), 'yuv420p', AOM, ['-crf', '45', '-cpu-used', '3', '-aom-params', 'enable-cdef=1:enable-restoration=0'])
stream('rav1e_420', make_image(96, 64, 23), 'yuv420p', ['-c:v', 'librav1e'], ['-qp', '100', '-speed', '5'])
stream('svt_420', make_image(96, 64, 24), 'yuv420p', ['-c:v', 'libsvtav1'], ['-crf', '30', '-preset', '6'])
# unsupported coding tools: the decoder must refuse them with UNSUPPORTED_FORMAT (md5 '-': no reference picture)
stream('aom_intrabc', make_screen(192, 128, 25), 'yuv420p', AOM, ['-crf', '20', '-cpu-used', '1', '-aom-params', 'tune-content=screen:enable-intrabc=1'],
       decode=False)
stream('aom_film_grain', make_image(64, 48, 26), 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '4', '-aom-params', 'film-grain-test=1'], decode=False)

with open(os.path.join(AV1, 'index.txt'), 'w', encoding='utf-8', newline='\n') as f:
    f.write('# name|width|height|bitDepth|numPlanes|subX|subY|md5 of the planes decoded by libdav1d|header features (generated by make_fixtures.py)\n')
    f.write('\n'.join(index) + '\n')

# --- AVIF files (Pillow / libavif) -------------------------------------------------------------------------------------------------------
avif_index = []


def avif(name, im, desc, **kw):
    path = os.path.join(AVIF, name + '.avif')
    im.save(path, **kw)
    ref = Image.open(path).convert('RGBA')
    ref.save(os.path.join(AVIF, name + '.png'))
    avif_index.append('%s|%d|%d|%s' % (name, ref.size[0], ref.size[1], desc))


rgb = make_image(64, 48, 31).convert('RGB')
avif('pil_420', rgb, 'RGB, 4:2:0, quality 60 (libavif defaults: BT.601 matrix, quantizer matrices)', quality=60, subsampling='4:2:0')
avif('pil_444', rgb, 'RGB, 4:4:4, quality 80', quality=80, subsampling='4:4:4')
avif('pil_422', rgb, 'RGB, 4:2:2, quality 70', quality=70, subsampling='4:2:2')
avif('pil_400', rgb.convert('L'), 'grey, 4:0:0, quality 70', quality=70, subsampling='4:0:0')
avif('pil_lossless', rgb, 'RGB, lossless (identity matrix)', quality=100, subsampling='4:4:4')
rgba = np.array(make_image(64, 48, 32).convert('RGBA'))
yy, xx = np.mgrid[0:rgba.shape[0], 0:rgba.shape[1]]
rgba[..., 3] = np.clip(255.0 * xx / rgba.shape[1], 0, 255).astype(np.uint8)
avif('pil_alpha', Image.fromarray(rgba), 'RGBA with a horizontal alpha ramp (auxiliary alpha item)', quality=70)
avif('pil_alpha_lossless', Image.fromarray(rgba), 'RGBA lossless', quality=100, subsampling='4:4:4')

with open(os.path.join(AVIF, 'index.txt'), 'w', encoding='utf-8', newline='\n') as f:
    f.write('# name|width|height|description (generated by make_fixtures.py with Pillow/libavif; the png is the libavif decoding)\n')
    f.write('\n'.join(avif_index) + '\n')
print('written', len(index), 'streams and', len(avif_index), 'avif files')
