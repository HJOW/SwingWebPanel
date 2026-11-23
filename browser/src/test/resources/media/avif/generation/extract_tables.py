import re, sys, json
SYM = {}
def enum(names, start=0):
    for i, n in enumerate(names):
        SYM[n] = start + i
enum(['BLOCK_4X4','BLOCK_4X8','BLOCK_8X4','BLOCK_8X8','BLOCK_8X16','BLOCK_16X8','BLOCK_16X16','BLOCK_16X32','BLOCK_32X16','BLOCK_32X32','BLOCK_32X64','BLOCK_64X32','BLOCK_64X64','BLOCK_64X128','BLOCK_128X64','BLOCK_128X128','BLOCK_4X16','BLOCK_16X4','BLOCK_8X32','BLOCK_32X8','BLOCK_16X64','BLOCK_64X16','BLOCK_INVALID'])
enum(['TX_4X4','TX_8X8','TX_16X16','TX_32X32','TX_64X64','TX_4X8','TX_8X4','TX_8X16','TX_16X8','TX_16X32','TX_32X16','TX_32X64','TX_64X32','TX_4X16','TX_16X4','TX_8X32','TX_32X8','TX_16X64','TX_64X16'])
enum(['DCT_DCT','ADST_DCT','DCT_ADST','ADST_ADST','FLIPADST_DCT','DCT_FLIPADST','FLIPADST_FLIPADST','ADST_FLIPADST','FLIPADST_ADST','IDTX','V_DCT','H_DCT','V_ADST','H_ADST','V_FLIPADST','H_FLIPADST'])
enum(['DC_PRED','V_PRED','H_PRED','D45_PRED','D135_PRED','D113_PRED','D157_PRED','D203_PRED','D67_PRED','SMOOTH_PRED','SMOOTH_V_PRED','SMOOTH_H_PRED','PAETH_PRED','UV_CFL_PRED'])
enum(['PARTITION_NONE','PARTITION_HORZ','PARTITION_VERT','PARTITION_SPLIT','PARTITION_HORZ_A','PARTITION_HORZ_B','PARTITION_VERT_A','PARTITION_VERT_B','PARTITION_HORZ_4','PARTITION_VERT_4'])
SYM.update({'SIG_COEF_CONTEXTS_2D':26,'MAX_FRAME_DISTANCE':31,'RESTORE_NONE':0,'RESTORE_WIENER':1,'RESTORE_SGRPROJ':2,'RESTORE_SWITCHABLE':3,'SEG_LVL_ALT_Q':0,'MAX_LOOP_FILTER':63,'MAX_SEGMENTS':8,'SEG_LVL_REF_FRAME':5})
txt = open(sys.argv[1], encoding='utf-8').read().split('\n')
tables = {}
i = 0
pat = re.compile(r'^([A-Za-z_][A-Za-z_0-9]*)\s*((?:\[[^\]]*\]\s*)+)=\s*\{(.*)$')
while i < len(txt):
    m = pat.match(txt[i])
    if not m:
        i += 1
        continue
    name = m.group(1)
    body = re.sub(r'//.*$', '', m.group(3))
    depth = 1 + body.count('{') - body.count('}')
    parts = [body]
    j = i
    while depth > 0 and j + 1 < len(txt):
        j += 1
        l = re.sub(r'//.*$', '', txt[j])
        parts.append(l)
        depth += l.count('{') - l.count('}')
    s = ' '.join(parts)
    # keep text up to the matching close
    d = 1
    k = 0
    for k, c in enumerate(s):
        if c == '{': d += 1
        elif c == '}':
            d -= 1
            if d == 0: break
    s = '{' + s[:k + 1]
    s = re.sub(r'/\*.*?\*/', '', s)
    # recursive descent over the brace structure; leaf expressions (such as 128*128 or SIG_COEF_CONTEXTS_2D + 5) are evaluated
    pos = [0]
    okf = [True]
    def skip_ws():
        while pos[0] < len(s) and s[pos[0]].isspace():
            pos[0] += 1
    def parse_val():
        skip_ws()
        if s[pos[0]] == '{':
            pos[0] += 1
            items = []
            while True:
                skip_ws()
                if s[pos[0]] == '}':
                    pos[0] += 1
                    return items
                items.append(parse_val())
                skip_ws()
                if s[pos[0]] == ',':
                    pos[0] += 1
        j = pos[0]
        while j < len(s) and s[j] not in ',}':
            j += 1
        expr = s[pos[0]:j].strip()
        pos[0] = j
        try:
            return int(eval(expr, {'__builtins__': {}}, dict(SYM)))
        except Exception:
            okf[0] = False
            return expr
    stack = [[parse_val()]]
    ok = okf[0]
    data = stack[0][0] if stack[0] else None
    if name in tables:
        pass
    tables[name] = {'dims': m.group(2).strip(), 'data': data, 'ok': ok, 'line': i + 1}
    i = j + 1

def shape(x):
    if isinstance(x, list):
        if not x:
            return [0]
        shapes = [shape(e) for e in x]
        if any(s != shapes[0] for s in shapes):
            return None
        return [len(x)] + shapes[0]
    return []

def flat(x):
    if isinstance(x, list):
        r = []
        for e in x:
            r += flat(e)
        return r
    return [x]

out = {}
bad = []
for n, t in tables.items():
    sh = shape(t['data'])
    if sh is None:
        bad.append((n, 'ragged'))
        continue
    f = flat(t['data'])
    if not t['ok'] or any(isinstance(v, str) for v in f):
        bad.append((n, 'symbolic ' + str([v for v in f if isinstance(v, str)][:4])))
        continue
    out[n] = {'shape': sh, 'flat': f, 'line': t['line']}
json.dump(out, open(sys.argv[2], 'w'))
print(len(out), 'tables;', len(bad), 'skipped')
for b in bad: print('  skipped', b)
for n in sys.argv[3:]:
    if n in out: print(n, out[n]['shape'], out[n]['flat'][:8])
