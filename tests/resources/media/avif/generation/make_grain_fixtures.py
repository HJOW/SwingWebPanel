"""Makes the film grain streams of the AV1 tests (plan item P18-03) and records the planes that libdav1d decodes from them.

Usage: python -I make_grain_fixtures.py <ffmpeg executable> <directory tests/resources/media/avif>

It needs only Python 3 and ffmpeg built with libaom-av1 (encoder: the film grain test vectors of libaom) and libdav1d (reference decoder, applies
the film grain).  The product code and the test run do not need any of them; the results are committed (av1/grain_*.obu and av1/index.txt).
Every stream is one intra frame with the film grain parameters of one of the 16 test vectors of libaom (-aom-params film-grain-test=N), so that
many combinations of the parameters (points of the scaling functions, chroma scaling from luma, auto-regressive lags, overlap, clipping to the
restricted range, the seeds) are covered; the pictures differ in the bit depth, the chroma format and the size.
"""
import hashlib
import os
import re
import subprocess
import sys

FFMPEG = sys.argv[1]
ROOT = sys.argv[2]
AV1 = os.path.join(ROOT, 'av1')
TMP = os.path.join(ROOT, 'generation', 'tmp')
os.makedirs(TMP, exist_ok=True)


def run(args):
    r = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        raise RuntimeError('command failed: %s\n%s' % (' '.join(args), r.stderr.decode('utf-8', 'replace')[-2000:]))
    return r


def features(obu):
    r = subprocess.run([FFMPEG, '-hide_banner', '-i', obu, '-c', 'copy', '-bsf:v', 'trace_headers', '-f', 'null', '-'], stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    t = r.stderr.decode('utf-8', 'replace')
    out = []
    for name in ('apply_grain', 'num_y_points', 'chroma_scaling_from_luma', 'num_cb_points', 'num_cr_points', 'ar_coeff_lag', 'overlap_flag',
                 'clip_to_restricted_range', 'grain_scale_shift', 'grain_scaling_minus_8'):
        m = re.search(r'\b' + re.escape(name) + r'\s+[01]+ = (-?\d+)', t)
        if m:
            out.append('%s=%s' % (name, m.group(1)))
    return ' '.join(out)


lines = {}
order = []


def stream(name, vector, w, h, pix, source, extra_params=''):
    obu = os.path.join(AV1, name + '.obu')
    params = 'film-grain-test=%d' % vector
    if extra_params:
        params += ':' + extra_params
    enc = [FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-f', 'lavfi', '-i', '%s=size=%dx%d:rate=1' % (source, w, h), '-frames:v', '1',
           '-pix_fmt', pix, '-c:v', 'libaom-av1', '-crf', '30', '-cpu-used', '4', '-aom-params', params, '-f', 'obu', obu]
    run(enc)
    yuv = os.path.join(TMP, name + '.yuv')
    run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-c:v', 'libdav1d', '-i', obu, '-pix_fmt', pix, '-f', 'rawvideo', yuv])
    data = open(yuv, 'rb').read()
    bd = 12 if '12' in pix else (10 if '10' in pix else 8)
    planes = 1 if pix.startswith('gray') else 3
    subx, suby = 1, 1
    if '444' in pix:
        subx, suby = 0, 0
    elif '422' in pix:
        subx, suby = 1, 0
    if planes == 1:
        subx, suby = 1, 1
    md5 = hashlib.md5(data).hexdigest()
    lines[name] = '%s|%d|%d|%d|%d|%d|%d|%s|%s' % (name, w, h, bd, planes, subx, suby, md5, 'film_grain_test=%d %s' % (vector, features(obu)))
    order.append(name)
    os.remove(yuv)


# the 16 parameter sets of libaom on a small 8 bit 4:2:0 picture
for n in range(1, 17):
    stream('grain_vec%02d' % n, n, 64, 48, 'yuv420p', 'testsrc2')
# other formats and sizes: odd sizes, 4:4:4, 4:2:2, 10 and 12 bit, gray, a second content
stream('grain_odd_61x45', 4, 61, 45, 'yuv420p', 'mandelbrot')
stream('grain_444', 7, 64, 48, 'yuv444p', 'testsrc2')
stream('grain_422', 9, 66, 50, 'yuv422p', 'mandelbrot')
stream('grain_10bit', 3, 64, 48, 'yuv420p10le', 'testsrc2')
stream('grain_12bit_444', 5, 64, 48, 'yuv444p12le', 'mandelbrot')
stream('grain_gray', 2, 64, 48, 'gray', 'testsrc2')
stream('grain_large', 11, 200, 150, 'yuv420p', 'mandelbrot')

# the existing film grain stream gets its reference md5 (it was recorded without one while the decoder refused grain)
index_path = os.path.join(AV1, 'index.txt')
old = open(index_path, encoding='utf-8').read().split('\n')
result = []
yuv = os.path.join(TMP, 'aom_film_grain.yuv')
run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-c:v', 'libdav1d', '-i', os.path.join(AV1, 'aom_film_grain.obu'), '-pix_fmt', 'yuv420p', '-f', 'rawvideo', yuv])
film_md5 = hashlib.md5(open(yuv, 'rb').read()).hexdigest()
os.remove(yuv)
for line in old:
    if line.startswith('grain_'):
        continue
    if line.startswith('aom_film_grain|'):
        f = line.split('|')
        f[3], f[4], f[5], f[6], f[7] = '8', '3', '1', '1', film_md5
        line = '|'.join(f)
    if line:
        result.append(line)
for name in order:
    result.append(lines[name])
with open(index_path, 'w', encoding='utf-8', newline='\n') as f:
    f.write('\n'.join(result) + '\n')
print('wrote %d grain streams, aom_film_grain md5 %s' % (len(order), film_md5))
