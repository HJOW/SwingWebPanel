import sys, os, struct
from PIL import Image
REF = sys.argv[1]; MINE = sys.argv[2]; OUT = sys.argv[3]; ONLY = sys.argv[4] if len(sys.argv) > 4 else None
os.makedirs(OUT, exist_ok=True)
def load_ref(p):
    b = open(p, 'rb').read()
    w, h = struct.unpack('<II', b[:8])
    return Image.frombytes('RGBA', (w, h), b[8:])
rows = []
for f in sorted(os.listdir(REF)):
    if not f.endswith('.rgba'): continue
    name = f[:-5]
    if ONLY and ONLY not in name: continue
    mp = os.path.join(MINE, name + '.png')
    if not os.path.exists(mp):
        rows.append((name, 999, 1, 'missing')); continue
    a = load_ref(os.path.join(REF, f)); b = Image.open(mp).convert('RGBA')
    if a.size != b.size:
        rows.append((name, 999, 1, 'size %s vs %s' % (a.size, b.size))); continue
    tot = 0; bad = 0; mx = 0
    n = a.size[0] * a.size[1]
    out = Image.new('RGBA', (a.size[0] * 3 + 8, a.size[1]), (200, 200, 255, 255))
    diff = Image.new('RGBA', a.size)
    pa = a.load(); pb = b.load(); pd = diff.load()
    for y in range(a.size[1]):
        for x in range(a.size[0]):
            r1 = pa[x, y]; r2 = pb[x, y]
            d = 0
            for bg in (255, 0):
                c1 = [(r1[i] * r1[3] + bg * (255 - r1[3])) / 255.0 for i in range(3)]
                c2 = [(r2[i] * r2[3] + bg * (255 - r2[3])) / 255.0 for i in range(3)]
                d = max(d, max(abs(c1[i] - c2[i]) for i in range(3)))
            d = max(d, abs(r1[3] - r2[3]))
            tot += d; mx = max(mx, d)
            if d > 40: bad += 1
            v = min(255, int(d * 2))
            pd[x, y] = (v, 0, 0, 255)
    rows.append((name, tot / n, bad / n, 'max %d' % mx))
    bg = Image.new('RGBA', a.size, (255, 255, 255, 255))
    out.paste(Image.alpha_composite(bg, a), (0, 0)); out.paste(Image.alpha_composite(bg, b), (a.size[0] + 4, 0)); out.paste(diff, (2 * a.size[0] + 8, 0))
    out.save(os.path.join(OUT, name + '.png'))
rows.sort(key=lambda r: -r[2])
print('%-28s %8s %8s  %s' % ('case', 'meanerr', 'bad%', 'note'))
for r in rows: print('%-28s %8.3f %8.3f  %s' % (r[0], r[1], r[2] * 100, r[3]))
