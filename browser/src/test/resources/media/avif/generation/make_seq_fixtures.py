#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Makes the AV1 sequences (several frames, with inter frames) of tests/resources/media/avif/seq (plan item P18-03) and records the md5 of every
frame that libdav1d decodes from them.

usage: python make_seq_fixtures.py <ffmpeg executable> <directory tests/resources/media/avif> [name,name,...]

Needs Python 3 and an ffmpeg with the encoder libaom-av1 (or librav1e / libsvtav1) and the decoder libdav1d (the reference decoder). The product
code and the tests need none of them; the streams (seq/<name>.obu) and the lines of seq/index.txt are committed.
seq/index.txt: name|bitDepth|numPlanes|subX|subY|number of frames|WxH:md5 of every frame, separated by commas|coding tools of the stream
(the md5 is that of the planes of one frame: 8 bit samples as bytes, other depths as 16 bit little endian words; the planes in the order
Y, U, V).  The names given as the third argument are made again, the other lines of the index are kept.
"""
import hashlib
import os
import re
import subprocess
import sys

FFMPEG = sys.argv[1]
FFPROBE = FFMPEG.replace('ffmpeg', 'ffprobe')
OUT = sys.argv[2]
SEQ = os.path.join(OUT, 'seq')
TMP = os.path.join(OUT, 'generation', 'tmp')
os.makedirs(SEQ, exist_ok=True)
os.makedirs(TMP, exist_ok=True)
ONLY = sys.argv[3].split(',') if len(sys.argv) > 3 else None


def run(args):
    r = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        raise RuntimeError('command failed: %s\n%s' % (' '.join(args), r.stderr.decode('utf-8', 'replace')[-2000:]))
    return r


def features(obu):
    """The coding tools that the headers of the stream announce (ffmpeg's trace_headers), as a summary of the frames."""
    r = subprocess.run([FFMPEG, '-hide_banner', '-i', obu, '-c', 'copy', '-bsf:v', 'trace_headers', '-f', 'null', '-'], stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    t = r.stderr.decode('utf-8', 'replace')
    out = []
    for name in ('enable_interintra_compound', 'enable_masked_compound', 'enable_warped_motion', 'enable_dual_filter', 'enable_order_hint',
                 'enable_jnt_comp', 'enable_ref_frame_mvs', 'enable_superres', 'enable_cdef', 'enable_restoration'):
        m = re.search(r'\b' + name + r'\s+[01]+ = (-?\d+)', t)
        if m and m.group(1) != '0':
            out.append(name)
    types = [int(v) for v in re.findall(r'\bframe_type\s+[01]+ = (\d)', t)]
    out.append('frames=%s' % ''.join(str(v) for v in types))
    for name in ('show_existing_frame', 'reference_select', 'skip_mode_present', 'allow_warped_motion', 'use_ref_frame_mvs', 'is_motion_mode_switchable',
                 'primary_ref_frame', 'segmentation_enabled', 'delta_q_present', 'allow_intrabc', 'use_superres'):
        vals = sorted(set(re.findall(r'\b' + name + r'\s+[01]+ = (-?\d+)', t)))
        if vals and vals != ['0']:
            out.append('%s=%s' % (name, '/'.join(vals)))
    gm = sorted(set(re.findall(r'\bis_global\s+[01]+ = (\d)', t)))
    if '1' in gm:
        out.append('global_motion')
    return ' '.join(out)


lines = {}
order = []


def sequence(name, source, size, nframes, pix, encoder, extra, rate=10, vf=None):
    """Encodes a lavfi source with an encoder, decodes it with libdav1d and records the md5 of every frame."""
    if ONLY is not None and name not in ONLY:
        return
    w, h = size
    obu = os.path.join(SEQ, name + '.obu')
    vfargs = ['-vf', vf] if vf else []
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-f', 'lavfi', '-i', '%s=size=%dx%d:rate=%d' % (source, w, h, rate)] + vfargs +
        ['-frames:v', str(nframes), '-pix_fmt', pix] + encoder + list(extra) + ['-f', 'obu', obu])
    yuv = os.path.join(TMP, name + '.yuv')
    # the sizes of the frames (they differ if the encoder resizes frames) and the planes of all frames decoded by libdav1d
    probe = subprocess.run([FFPROBE, '-v', 'error', '-show_entries', 'frame=width,height', '-of', 'csv=p=0', obu], stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
    sizes = [tuple(int(v) for v in l.strip().split(',')) for l in probe.stdout.decode('utf-8').splitlines() if l.strip()]
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-c:v', 'libdav1d', '-i', obu, '-noautoscale', '-pix_fmt', pix, '-f', 'rawvideo', yuv])
    data = open(yuv, 'rb').read()
    os.remove(yuv)
    bd = 12 if '12' in pix else (10 if '10' in pix else 8)
    planes = 1 if pix.startswith('gray') else 3
    subx, suby = 1, 1
    if '444' in pix:
        subx, suby = 0, 0
    elif '422' in pix:
        subx, suby = 1, 0
    bytes_per = 1 if bd == 8 else 2
    entries = []
    pos = 0
    # frames in output order, each with its own size
    for (fw, fh) in sizes:
        if planes == 1:
            frame_size = fw * fh * bytes_per
        else:
            cw = (fw + subx) >> subx
            ch = (fh + suby) >> suby
            frame_size = (fw * fh + 2 * cw * ch) * bytes_per
        entries.append('%dx%d:%s' % (fw, fh, hashlib.md5(data[pos:pos + frame_size]).hexdigest()))
        pos += frame_size
    assert pos == len(data), '%s: the frame sizes do not add up to the decoded data (%d of %d bytes)' % (name, pos, len(data))
    count = len(entries)
    lines[name] = '%s|%d|%d|%d|%d|%d|%s|%s' % (name, bd, planes, subx, suby, count, ','.join(entries), features(obu))
    order.append(name)
    print(name, count, 'frames', features(obu))


AOM = ['-c:v', 'libaom-av1']
# the inter tools of libaom that the sequences switch off (to bring up the decoder tool by tool)
NO_TOOLS = ('enable-obmc=0:enable-warped-motion=0:enable-global-motion=0:enable-interintra-comp=0:enable-masked-comp=0:enable-dist-wtd-comp=0:'
            'enable-dual-filter=0:enable-ref-frame-mvs=0:enable-smooth-interintra=0:enable-diff-wtd-comp=0:enable-interinter-wedge=0:'
            'enable-interintra-wedge=0:enable-onesided-comp=0')
NO_FILTERS = 'enable-cdef=0:enable-restoration=0'
LAG0 = ['-lag-in-frames', '0']

def tools(**on):
    """The libaom option string with all inter tools off except the given ones (name=1)."""
    names = ['obmc', 'warped-motion', 'global-motion', 'interintra-comp', 'masked-comp', 'dist-wtd-comp', 'dual-filter', 'ref-frame-mvs',
             'smooth-interintra', 'diff-wtd-comp', 'interinter-wedge', 'interintra-wedge', 'onesided-comp']
    opts = []
    for n in names:
        key = n.replace('-', '_')
        opts.append('enable-%s=%d' % (n, on.get(key, 0)))
    return ':'.join(opts)


FILTERS_OFF = 'enable-cdef=0:enable-restoration=0'
SRC = ['testsrc2', 'mandelbrot', 'rgbtestsrc']

# --- single reference prediction (lag 0: every frame is coded in display order, only LAST is useful) -------------------------------------
sequence('seq_basic', 'testsrc2', (96, 64), 3, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '4'] + LAG0 + ['-aom-params', tools() + ':' + FILTERS_OFF])
sequence('seq_basic8', 'testsrc2', (128, 96), 8, 'yuv420p', AOM, ['-crf', '28', '-cpu-used', '4'] + LAG0 + ['-aom-params', tools() + ':' + FILTERS_OFF])
# the loop filters of inter frames (deblocking with the reference and mode deltas, CDEF, loop restoration)
sequence('seq_filters', 'testsrc2', (160, 120), 6, 'yuv420p', AOM, ['-crf', '38', '-cpu-used', '3'] + LAG0 + ['-aom-params', tools()])
sequence('seq_filters_mandel', 'mandelbrot', (128, 96), 6, 'yuv420p', AOM, ['-crf', '45', '-cpu-used', '2'] + LAG0 + ['-aom-params', tools()])
# switchable interpolation filters, dual filters
sequence('seq_dual', 'testsrc2', (128, 96), 6, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '2'] + LAG0 + ['-aom-params', tools(dual_filter=1) + ':' + FILTERS_OFF])
# motion vectors projected from earlier frames
sequence('seq_refmvs', 'testsrc2', (128, 96), 8, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '3'] + LAG0 + ['-aom-params', tools(ref_frame_mvs=1) + ':' + FILTERS_OFF])
# hidden frames, show_existing_frame, compound prediction of two references (average)
sequence('seq_altref', 'testsrc2', (128, 96), 12, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '12', '-auto-alt-ref', '1'] +
         ['-aom-params', tools() + ':' + FILTERS_OFF])

# overlapped block motion compensation, local warped motion, global motion
sequence('seq_obmc', 'testsrc2', (128, 96), 6, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '2'] + LAG0 + ['-aom-params', tools(obmc=1) + ':' + FILTERS_OFF])
sequence('seq_warp', 'mandelbrot', (128, 96), 6, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '1'] + LAG0 +
         ['-aom-params', tools(warped_motion=1, obmc=1) + ':' + FILTERS_OFF])
sequence('seq_global', 'testsrc2', (128, 96), 8, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '1'] + LAG0 +
         ['-aom-params', tools(global_motion=1) + ':' + FILTERS_OFF], vf='rotate=a=t*0.4:c=black')
# inter-intra prediction (smooth, wedge) and masked compound prediction (wedge, difference weighted), distance weighted compound prediction
sequence('seq_interintra', 'testsrc2', (128, 96), 8, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '1'] + LAG0 +
         ['-aom-params', tools(interintra_comp=1, smooth_interintra=1, interintra_wedge=1) + ':' + FILTERS_OFF])
sequence('seq_masked', 'testsrc2', (128, 96), 12, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '1', '-lag-in-frames', '12', '-auto-alt-ref', '1'] +
         ['-aom-params', tools(masked_comp=1, diff_wtd_comp=1, interinter_wedge=1) + ':' + FILTERS_OFF])
sequence('seq_jnt', 'testsrc2', (128, 96), 12, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '0', '-lag-in-frames', '12', '-auto-alt-ref', '1'] +
         ['-aom-params', tools(dist_wtd_comp=1) + ':' + FILTERS_OFF])
# everything on (the default of libaom)
sequence('seq_default', 'testsrc2', (160, 120), 12, 'yuv420p', AOM, ['-crf', '32', '-cpu-used', '1', '-lag-in-frames', '12', '-auto-alt-ref', '1'])
sequence('seq_default_mandel', 'mandelbrot', (160, 120), 12, 'yuv420p', AOM, ['-crf', '36', '-cpu-used', '2', '-lag-in-frames', '12', '-auto-alt-ref', '1'])

# segmentation (adaptive quantization), delta q and delta loop filter levels
sequence('seq_aq', 'testsrc2', (160, 120), 8, 'yuv420p', AOM, ['-crf', '32', '-cpu-used', '2', '-lag-in-frames', '8', '-aq-mode', '1'])
sequence('seq_aq2', 'mandelbrot', (160, 120), 8, 'yuv420p', AOM, ['-crf', '36', '-cpu-used', '2', '-lag-in-frames', '8', '-aq-mode', '2'])
sequence('seq_deltaq', 'testsrc2', (160, 120), 8, 'yuv420p', AOM, ['-crf', '32', '-cpu-used', '2', '-lag-in-frames', '8', '-aom-params',
                                                                       'deltaq-mode=3:delta-lf-mode=1'])
# other pixel formats and bit depths, tiles, 128x128 superblocks, odd sizes
sequence('seq_444', 'testsrc2', (96, 64), 6, 'yuv444p', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6'])
sequence('seq_422', 'testsrc2', (96, 64), 6, 'yuv422p', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6'])
sequence('seq_10bit', 'testsrc2', (96, 64), 6, 'yuv420p10le', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6'])
sequence('seq_12bit_444', 'testsrc2', (96, 64), 6, 'yuv444p12le', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6'])
sequence('seq_gray', 'testsrc2', (96, 64), 6, 'gray', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6'])
sequence('seq_tiles', 'testsrc2', (320, 192), 6, 'yuv420p', AOM, ['-crf', '34', '-cpu-used', '4', '-lag-in-frames', '6', '-tiles', '2x2'])
sequence('seq_sb128', 'testsrc2', (320, 192), 6, 'yuv420p', AOM, ['-crf', '34', '-cpu-used', '4', '-lag-in-frames', '6', '-aom-params', 'sb-size=128'])
sequence('seq_odd', 'testsrc2', (131, 77), 6, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6'])
sequence('seq_lossless', 'testsrc2', (96, 64), 4, 'yuv420p', AOM, ['-crf', '0', '-cpu-used', '4', '-lag-in-frames', '4', '-aom-params', 'lossless=1'])
# frames of another size than the references (the references are scaled), several key frames
SVT = ['-c:v', 'libsvtav1']

sequence('seq_resize', 'testsrc2', (160, 120), 10, 'yuv420p', SVT, ['-crf', '30', '-preset', '6', '-g', '8', '-svtav1-params', 'resize-mode=2'])
sequence('seq_resize_fixed', 'testsrc2', (160, 120), 10, 'yuv420p', SVT, ['-crf', '30', '-preset', '6', '-g', '8', '-svtav1-params',
                                                                           'resize-mode=1:resize-denom=12'])
sequence('seq_keyframes', 'testsrc2', (96, 64), 10, 'yuv420p', AOM, ['-crf', '32', '-cpu-used', '4', '-lag-in-frames', '4', '-g', '4'])
# film grain on inter frames (the parameters of a later frame come from a reference frame)
sequence('seq_grain', 'testsrc2', (96, 64), 8, 'yuv420p', AOM, ['-crf', '32', '-cpu-used', '4', '-lag-in-frames', '6', '-aom-params',
                                                                   'film-grain-test=2'])
# screen content (palette in inter frames)
sequence('seq_screen', 'testsrc2', (128, 96), 6, 'yuv420p', AOM, ['-crf', '30', '-cpu-used', '3', '-lag-in-frames', '6', '-aom-params',
                                                                     'tune-content=screen'])

# super resolution on inter frames (the reference frames are upscaled), other encoders (SVT-AV1, rav1e), error resilient frames, a larger picture
SVTP = ['-crf', '34', '-preset', '6', '-g', '12', '-svtav1-params']
sequence('seq_superres', 'testsrc2', (160, 120), 8, 'yuv420p', SVT, SVTP + ['superres-mode=1:superres-denom=12:superres-kf-mode=1:superres-kf-denom=12'])
sequence('seq_superres_random', 'mandelbrot', (160, 120), 10, 'yuv420p', SVT, SVTP + ['superres-mode=2:superres-kf-mode=2'])
sequence('seq_svt', 'testsrc2', (160, 120), 12, 'yuv420p', SVT, ['-crf', '30', '-preset', '4', '-g', '16'])
sequence('seq_svt_10bit', 'mandelbrot', (128, 96), 10, 'yuv420p10le', SVT, ['-crf', '30', '-preset', '6', '-g', '16'])
sequence('seq_rav1e', 'testsrc2', (128, 96), 12, 'yuv420p', ['-c:v', 'librav1e'], ['-qp', '80', '-speed', '4', '-g', '16'])
sequence('seq_errres', 'testsrc2', (96, 64), 8, 'yuv420p', AOM, ['-crf', '32', '-cpu-used', '4', '-lag-in-frames', '4', '-aom-params', 'error-resilient=1'])
sequence('seq_big', 'testsrc2', (320, 240), 16, 'yuv420p', AOM, ['-crf', '34', '-cpu-used', '3', '-lag-in-frames', '16', '-auto-alt-ref', '1'])

if ONLY is None or ONLY:
    index_path = os.path.join(SEQ, 'index.txt')
    old = []
    if os.path.exists(index_path):
        old = [l for l in open(index_path, encoding='utf-8').read().split('\n') if l and not l.startswith('#')]
    result = [l for l in old if l.split('|')[0] not in lines]
    result += [lines[n] for n in order]
    with open(index_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('# name|bitDepth|numPlanes|subX|subY|frames|WxH:md5 of every frame|tools (see make_seq_fixtures.py)\n')
        f.write('\n'.join(result) + '\n')
    print('wrote %d sequences' % len(order))
