import sys, os, struct, math
from PIL import Image
# usage: mkrefs.py <chrome out dir> <mine dir> <cases dir> <refs out dir> <index file>
OUT, MINE, CASES, REFS, INDEX = sys.argv[1:6]
os.makedirs(REFS, exist_ok=True)
lines = []
for f in sorted(os.listdir(CASES)):
    if not f.endswith('.svg'):
        continue
    name = f[:-4]
    rp = os.path.join(OUT, f + '.rgba')
    mp = os.path.join(MINE, f + '.png')
    if not os.path.exists(rp) or not os.path.exists(mp):
        print('skip', name)
        continue
    b = open(rp, 'rb').read()
    w, h = struct.unpack('<II', b[:8])
    a = Image.frombytes('RGBA', (w, h), b[8:])
    m = Image.open(mp).convert('RGBA')
    if a.size != m.size:
        print('size mismatch', name, a.size, m.size)
        continue
    a.save(os.path.join(REFS, name + '.png'), optimize=True)
    pa = a.load(); pm = m.load()
    tot = 0; bad = 0
    for y in range(h):
        for x in range(w):
            r1 = pa[x, y]; r2 = pm[x, y]
            d = abs(r1[3] - r2[3])
            for bg in (255, 0):
                c1 = [(r1[i] * r1[3] + bg * (255 - r1[3])) / 255.0 for i in range(3)]
                c2 = [(r2[i] * r2[3] + bg * (255 - r2[3])) / 255.0 for i in range(3)]
                d = max(d, max(abs(c1[i] - c2[i]) for i in range(3)))
            tot += d
            if d > 40: bad += 1
    mean = tot / (w * h); badp = 100.0 * bad / (w * h)
    ml = max(1.5, round(mean * 1.6 + 0.05, 2)); bl = max(1.0, round(badp * 1.6 + 0.05, 2))
    lines.append('%s %.2f %.2f' % (name, ml, bl))
open(INDEX, 'w', encoding='utf-8', newline='\n').write(
    '# name  maxMeanError  maxBadPercent  (thresholds for tests: SvgReferenceTest compares with the Chrome renderings in ../refs)\n' + '\n'.join(lines) + '\n')
print(len(lines), 'refs')
