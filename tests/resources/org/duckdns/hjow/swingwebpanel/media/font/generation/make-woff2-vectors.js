// Generates the WOFF2 test files of tests/src/org/duckdns/hjow/swingwebpanel/media/font/Woff2Test.java from a SYNTHETIC TrueType font that this script makes itself (plan item P10-08-e).
// This is a WOFF2 ENCODER written for the tests from the text of the W3C Recommendation "WOFF File Format 2.0" (https://www.w3.org/TR/WOFF2/, last modified 2024-08-05, read
// with tools/webfetch on 2026-11-18); the Brotli compression is done by the reference encoder that ships with Node.js (zlib.brotliCompressSync, Node.js v26.13.0). Run it once
// by hand, the tests and the product do not need Node.js:
//   node tests/resources/org/duckdns/hjow/swingwebpanel/media/font/generation/make-woff2-vectors.js
// Output (../woff2/): synthetic.ttf (the font: 240 glyphs made from a fixed pseudo random sequence, no real font), synthetic-xf.woff2 (glyf/loca transformed, hmtx transformed in
// the proportional part only because one left side bearing of the monospaced part differs from the xMin of its glyph), synthetic-null.woff2 (null transforms of all tables),
// synthetic-meta.woff2 (transformed, with an extended metadata block and a private data block) and index.txt whose lines are "<font> <ttf file> <variant> <woff2 file>".
//
// WHY NO REAL FONT: the fonts that the JRE ships (Lucida Sans, Lucida Bright, Lucida Typewriter of Bigelow & Holmes) may be used with Java only, so no file made from them
// (no WOFF2 file, no Brotli stream of them) is kept in the repository. The tests that need a real font read the font of the JRE that runs them and convert it at test time
// with the Java test encoder Woff2Builder (tests/src/org/duckdns/hjow/swingwebpanel/media/font/Woff2Fonts.java); the table data of such a file is stored in Brotli blocks that are not
// compressed. This script gives the one thing that those files lack: a WOFF2 file whose table data is compressed by the reference Brotli encoder.
//
// HONEST NOTE ABOUT WHAT THIS PROVES: this encoder and the Java decoder (src/org/duckdns/hjow/swingwebpanel/media/font/Woff2.java) were both written from the same specification text by the
// same author, so a misreading of the specification would be in both. The triplet table of section 5.2 is the one place where the encoder uses another form than the
// decoder: here the 128 rows are generated from the rows of the specification table (byte count, bits, deltas, signs), the decoder uses the closed formulas. The tests
// also compare the decoded font with the original font table by table and glyph by glyph. No WOFF2 file from a browser or from another independent encoder was available on
// the machine.
// 이 스크립트는 스크립트가 직접 만드는 합성 TrueType 글꼴로 WOFF2 시험 파일을 만드는 WOFF2 인코더이다(계획 P10-08-e). 규격(W3C 권고안 WOFF2)의 본문을 읽고 시험용으로 썼고 Brotli 압축은
// Node.js 에 들어 있는 참조 인코더가 한다. 실제 글꼴은 쓰지 않는다: JRE 가 담은 글꼴(Bigelow & Holmes 의 Lucida)은 Java 에서만 쓰도록 허락된 것이므로 그것으로 만든 파일(WOFF2 파일,
// Brotli 스트림)은 저장소에 두지 않는다. 실제 글꼴이 필요한 시험은 실행 중인 JRE 의 글꼴을 읽어 시험 때 Java 시험용 인코더 Woff2Builder 로 바꾼다(그 파일의 테이블 데이터는 압축하지 않은
// Brotli 블록에 저장된다). 이 스크립트는 그런 파일에 없는 것, 즉 참조 Brotli 인코더로 테이블 데이터를 압축한 WOFF2 파일을 준다. 같은 사람이 같은 규격 텍스트로 인코더와 디코더를 썼으므로
// 규격을 잘못 읽었다면 둘 다 틀린다(5.2 절의 삼중 표만 인코더가 규격 표의 행으로 만들어 닫힌 식을 쓰는 디코더와 다른 형태를 쓴다). 그래서 시험은 풀린 글꼴을 원본과 테이블별, 글리프별로
// 비교한다. 브라우저나 다른 독립 인코더가 만든 WOFF2 파일은 이 PC 에 없었다.
'use strict';
var fs = require('fs');
var path = require('path');
var zlib = require('zlib');
var c = zlib.constants;

var outDir = path.join(__dirname, '..', 'woff2');
fs.mkdirSync(outDir, { recursive: true });

var KNOWN = ['cmap', 'head', 'hhea', 'hmtx', 'maxp', 'name', 'OS/2', 'post', 'cvt ', 'fpgm', 'glyf', 'loca', 'prep', 'CFF ', 'VORG', 'EBDT', 'EBLC', 'gasp', 'hdmx', 'kern',
  'LTSH', 'PCLT', 'VDMX', 'vhea', 'vmtx', 'BASE', 'GDEF', 'GPOS', 'GSUB', 'EBSC', 'JSTF', 'MATH', 'CBDT', 'CBLC', 'COLR', 'CPAL', 'SVG ', 'sbix', 'acnt', 'avar', 'bdat', 'bloc',
  'bsln', 'cvar', 'fdsc', 'feat', 'fmtx', 'fvar', 'gvar', 'hsty', 'just', 'lcar', 'mort', 'morx', 'opbd', 'prop', 'trak', 'Zapf', 'Silf', 'Glat', 'Gloc', 'Feat', 'Sill'];

// ---------------------------------------------------------------------------------------------------- the triplet table of section 5.2
// One row per flag value 0..127: how many data bytes, X bits, Y bits, the amounts added to the X and Y values, the signs of X and Y (0 = not applicable).
// 플래그 값 0~127 의 행: 데이터 바이트 수, X 비트, Y 비트, X 와 Y 값에 더하는 양, X 와 Y 의 부호(0 은 해당 없음).
function tripletRows() {
  var rows = [];
  var i;
  var signs = [[-1, -1], [1, -1], [-1, 1], [1, 1]];
  var base10 = [0, 0, 256, 256, 512, 512, 768, 768, 1024, 1024];
  for (i = 0; i < 10; i++) { rows.push({ index: i, data: 1, xBits: 0, yBits: 8, dxBase: 0, dyBase: base10[i], xSign: 0, ySign: (i % 2 === 0 ? -1 : 1) }); }
  for (i = 10; i < 20; i++) { rows.push({ index: i, data: 1, xBits: 8, yBits: 0, dxBase: base10[i - 10], dyBase: 0, xSign: (i % 2 === 0 ? -1 : 1), ySign: 0 }); }
  for (i = 20; i < 84; i++) {
    var j = i - 20;
    rows.push({ index: i, data: 1, xBits: 4, yBits: 4, dxBase: 1 + 16 * Math.floor(j / 16), dyBase: 1 + 16 * (Math.floor(j / 4) % 4), xSign: signs[j % 4][0], ySign: signs[j % 4][1] });
  }
  for (i = 84; i < 120; i++) {
    var k = i - 84;
    rows.push({ index: i, data: 2, xBits: 8, yBits: 8, dxBase: 1 + 256 * Math.floor(k / 12), dyBase: 1 + 256 * Math.floor((k % 12) / 4), xSign: signs[k % 4][0], ySign: signs[k % 4][1] });
  }
  for (i = 120; i < 124; i++) { rows.push({ index: i, data: 3, xBits: 12, yBits: 12, dxBase: 0, dyBase: 0, xSign: signs[(i - 120) % 4][0], ySign: signs[(i - 120) % 4][1] }); }
  for (i = 124; i < 128; i++) { rows.push({ index: i, data: 4, xBits: 16, yBits: 16, dxBase: 0, dyBase: 0, xSign: signs[(i - 124) % 4][0], ySign: signs[(i - 124) % 4][1] }); }
  return rows;
}
var ROWS = tripletRows();

function fitsAxis(bits, base, sign, v) {
  if (bits === 0) { return v === 0; }
  var mag = Math.abs(v);
  if (v !== 0 && (v < 0 ? -1 : 1) !== sign) { return false; }
  return mag >= base && mag <= base + (1 << bits) - 1;
}

// Encodes a point's delta with the row of the fewest data bytes: returns {index, bytes}. 점의 변화량을 데이터 바이트가 가장 적은 행으로 인코딩한다.
function encodeDelta(dx, dy) {
  var best = null;
  ROWS.forEach(function (r) {
    if (!fitsAxis(r.xBits, r.dxBase, r.xSign, dx) || !fitsAxis(r.yBits, r.dyBase, r.ySign, dy)) { return; }
    if (best === null || r.data < best.data) { best = r; }
  });
  if (best === null) { throw new Error('no triplet row for ' + dx + ',' + dy); }
  var xv = best.xBits ? Math.abs(dx) - best.dxBase : 0;
  var yv = best.yBits ? Math.abs(dy) - best.dyBase : 0;
  var bytes;
  var i = best.index;
  if (i < 10) { bytes = [yv]; }
  else if (i < 20) { bytes = [xv]; }
  else if (i < 84) { bytes = [(xv << 4) | yv]; }
  else if (i < 120) { bytes = [xv, yv]; }
  else if (i < 124) { bytes = [xv >> 4, ((xv & 15) << 4) | (yv >> 8), yv & 255]; }
  else { bytes = [xv >> 8, xv & 255, yv >> 8, yv & 255]; }
  return { index: i, bytes: bytes };
}

// ---------------------------------------------------------------------------------------------------- variable-length numbers (section 3.1)
function base128(v) {
  var out = [v & 0x7F];
  v = Math.floor(v / 128);
  while (v > 0) { out.unshift((v & 0x7F) | 0x80); v = Math.floor(v / 128); }
  return out;
}
function u255(v) {
  if (v < 253) { return [v]; }
  if (v < 506) { return [255, v - 253]; }
  if (v < 759) { return [254, v - 506]; }
  return [253, v >> 8, v & 255];
}

// ---------------------------------------------------------------------------------------------------- reading the TrueType glyphs
function parseSfnt(buf) {
  var n = buf.readUInt16BE(4);
  var tables = {};
  for (var i = 0; i < n; i++) {
    var e = 12 + 16 * i;
    var tag = buf.toString('latin1', e, e + 4);
    var off = buf.readUInt32BE(e + 8);
    var len = buf.readUInt32BE(e + 12);
    tables[tag] = buf.slice(off, off + len);
  }
  return { flavor: buf.readUInt32BE(0), tables: tables };
}

function parseGlyph(g) {
  var r = { nc: 0 };
  if (g.length === 0) { return r; }
  r.nc = g.readInt16BE(0);
  r.box = [g.readInt16BE(2), g.readInt16BE(4), g.readInt16BE(6), g.readInt16BE(8)];
  var p = 10;
  var i;
  if (r.nc === 0) {
    if (r.box.some(function (v) { return v !== 0; })) { throw new Error('a glyph without contours has a box'); }
    return r;
  }
  if (r.nc > 0) {
    r.endPts = [];
    for (i = 0; i < r.nc; i++) { r.endPts.push(g.readUInt16BE(p)); p += 2; }
    var total = r.endPts[r.nc - 1] + 1;
    var il = g.readUInt16BE(p); p += 2;
    r.instr = g.slice(p, p + il); p += il;
    var flags = [];
    while (flags.length < total) {
      var f = g[p++];
      flags.push(f);
      if (f & 8) { var rep = g[p++]; for (var k = 0; k < rep; k++) { flags.push(f); } }
    }
    r.flags = flags;
    r.xs = []; r.ys = [];
    var x = 0; var y = 0;
    for (i = 0; i < total; i++) {
      var f1 = flags[i];
      if (f1 & 2) { var dx = g[p++]; x += (f1 & 16) ? dx : -dx; } else if (!(f1 & 16)) { x += g.readInt16BE(p); p += 2; }
      r.xs.push(x);
    }
    for (i = 0; i < total; i++) {
      var f2 = flags[i];
      if (f2 & 4) { var dy = g[p++]; y += (f2 & 32) ? dy : -dy; } else if (!(f2 & 32)) { y += g.readInt16BE(p); p += 2; }
      r.ys.push(y);
    }
    return r;
  }
  // composite glyph 합성 글리프
  var comp = [];
  var more;
  var haveInstr = false;
  do {
    var cf = g.readUInt16BE(p);
    var size = 4 + ((cf & 1) ? 4 : 2) + ((cf & 8) ? 2 : (cf & 0x40) ? 4 : (cf & 0x80) ? 8 : 0);
    // the whole component record (flags word, glyph index, arguments, scale) is copied as it is. 구성 요소 기록 전체를 그대로 복사한다.
    comp.push(g.slice(p, p + size));
    p += size;
    if (cf & 0x100) { haveInstr = true; }
    more = (cf & 0x20) !== 0;
  } while (more);
  r.comp = Buffer.concat(comp);
  if (haveInstr) { var cl = g.readUInt16BE(p); p += 2; r.instr = g.slice(p, p + cl); }
  return r;
}

// ---------------------------------------------------------------------------------------------------- the transformed glyf table (section 5.1)
function transformGlyf(sfnt) {
  var head = sfnt.tables.head;
  var loca = sfnt.tables.loca;
  var glyf = sfnt.tables.glyf;
  var indexFormat = head.readInt16BE(50);
  var numGlyphs = sfnt.tables.maxp.readUInt16BE(4);
  var nContour = []; var nPoints = []; var flagS = []; var glyphS = []; var compS = []; var bboxS = []; var instrS = [];
  var bitmap = new Array(4 * Math.floor((numGlyphs + 31) / 32)).fill(0);
  var overlapBits = new Array(Math.floor((numGlyphs + 7) / 8)).fill(0);
  var anyOverlap = false;
  var xMins = [];
  var stats = { simple: 0, composite: 0, empty: 0, explicitBox: 0, rowsUsed: {} };
  for (var g = 0; g < numGlyphs; g++) {
    var a = indexFormat === 0 ? loca.readUInt16BE(2 * g) * 2 : loca.readUInt32BE(4 * g);
    var b = indexFormat === 0 ? loca.readUInt16BE(2 * g + 2) * 2 : loca.readUInt32BE(4 * g + 4);
    var gl = parseGlyph(glyf.slice(a, b));
    nContour.push((gl.nc >> 8) & 255, gl.nc & 255);
    if (gl.nc === 0) { stats.empty++; xMins.push(0); continue; }
    xMins.push(gl.box[0]);
    var explicit = false;
    if (gl.nc > 0) {
      stats.simple++;
      var start = 0;
      gl.endPts.forEach(function (e) { Array.prototype.push.apply(nPoints, u255(e - start + 1)); start = e + 1; });
      var px = 0; var py = 0;
      for (var i = 0; i < gl.xs.length; i++) {
        var t = encodeDelta(gl.xs[i] - px, gl.ys[i] - py);
        stats.rowsUsed[t.index] = true;
        flagS.push(((gl.flags[i] & 1) ? 0 : 0x80) | t.index);
        Array.prototype.push.apply(glyphS, t.bytes);
        px = gl.xs[i]; py = gl.ys[i];
      }
      Array.prototype.push.apply(glyphS, u255(gl.instr.length));
      Array.prototype.push.apply(instrS, gl.instr);
      var mn = [Math.min.apply(null, gl.xs), Math.min.apply(null, gl.ys), Math.max.apply(null, gl.xs), Math.max.apply(null, gl.ys)];
      explicit = mn.some(function (v, k) { return v !== gl.box[k]; });
      if (gl.flags[0] & 0x40) { overlapBits[g >> 3] |= 0x80 >> (g & 7); anyOverlap = true; }
    } else {
      stats.composite++;
      explicit = true;
      Array.prototype.push.apply(compS, gl.comp);
      if (gl.instr) { Array.prototype.push.apply(glyphS, u255(gl.instr.length)); Array.prototype.push.apply(instrS, gl.instr); }
    }
    if (explicit) {
      stats.explicitBox++;
      bitmap[g >> 3] |= 0x80 >> (g & 7);
      gl.box.forEach(function (v) { bboxS.push((v >> 8) & 255, v & 255); });
    }
  }
  var bbox = bitmap.concat(bboxS);
  var parts = [nContour, nPoints, flagS, glyphS, compS, bbox, instrS];
  var header = Buffer.alloc(36);
  header.writeUInt16BE(0, 0);
  header.writeUInt16BE(anyOverlap ? 1 : 0, 2);
  header.writeUInt16BE(numGlyphs, 4);
  header.writeUInt16BE(indexFormat, 6);
  parts.forEach(function (p, i) { header.writeUInt32BE(p.length, 8 + 4 * i); });
  var bufs = [header].concat(parts.map(function (p) { return Buffer.from(p); }));
  if (anyOverlap) { bufs.push(Buffer.from(overlapBits)); }
  return { data: Buffer.concat(bufs), xMins: xMins, numGlyphs: numGlyphs, indexFormat: indexFormat, stats: stats };
}

// The transformed hmtx table (section 5.4) when every left side bearing equals the xMin of its glyph, else null.
// 모든 왼쪽 여백이 글리프의 xMin 과 같을 때 변환된 hmtx 테이블(5.4 절), 아니면 null.
function transformHmtx(sfnt, xMins) {
  var hmtx = sfnt.tables.hmtx;
  var numMetrics = sfnt.tables.hhea.readUInt16BE(34);
  var numGlyphs = xMins.length;
  var propOk = true; var monoOk = true;
  var i;
  for (i = 0; i < numMetrics; i++) { if (hmtx.readInt16BE(4 * i + 2) !== xMins[i]) { propOk = false; } }
  for (i = numMetrics; i < numGlyphs; i++) { if (hmtx.readInt16BE(4 * numMetrics + 2 * (i - numMetrics)) !== xMins[i]) { monoOk = false; } }
  var flags = (propOk ? 1 : 0) | (monoOk ? 2 : 0);
  if (flags === 0) { return null; }
  var out = [flags];
  for (i = 0; i < numMetrics; i++) { out.push(hmtx[4 * i], hmtx[4 * i + 1]); }
  if (!propOk) { for (i = 0; i < numMetrics; i++) { out.push(hmtx[4 * i + 2], hmtx[4 * i + 3]); } }
  if (!monoOk) { for (i = numMetrics; i < numGlyphs; i++) { var o = 4 * numMetrics + 2 * (i - numMetrics); out.push(hmtx[o], hmtx[o + 1]); } }
  return { data: Buffer.from(out), flags: flags };
}

// ---------------------------------------------------------------------------------------------------- the file
function brotli(data) {
  var params = {};
  params[c.BROTLI_PARAM_MODE] = c.BROTLI_MODE_FONT;
  params[c.BROTLI_PARAM_QUALITY] = 11;
  params[c.BROTLI_PARAM_LGWIN] = 22;
  params[c.BROTLI_PARAM_SIZE_HINT] = data.length;
  return zlib.brotliCompressSync(data, { params: params });
}

function pad4(buf) {
  var n = (4 - (buf.length & 3)) & 3;
  return n ? Buffer.concat([buf, Buffer.alloc(n)]) : buf;
}

function woff2(fontBuf, transform, extras) {
  var sfnt = parseSfnt(fontBuf);
  var tags = Object.keys(sfnt.tables).sort(function (a, b) { return a < b ? -1 : a > b ? 1 : 0; });
  // The encoder must remove the DSIG table: the reconstructed tables are not bit for bit the same, so the signature would be invalid (section 5). 인코더는 DSIG 를 지워야 한다.
  tags = tags.filter(function (t) { return t !== 'DSIG'; });
  // loca must follow glyf directly. loca 는 glyf 바로 뒤에 온다.
  tags = tags.filter(function (t) { return t !== 'loca'; });
  tags.splice(tags.indexOf('glyf') + 1, 0, 'loca');
  // bit 11 of the flags of the head table: the font was subjected to a lossless modifying transform. head 의 flags 11 번 비트.
  var head = Buffer.from(sfnt.tables.head);
  head.writeUInt16BE(head.readUInt16BE(16) | 0x0800, 16);
  sfnt.tables.head = head;
  var gl = transform ? transformGlyf(sfnt) : null;
  var hm = transform ? transformHmtx(sfnt, gl.xMins) : null;
  var dir = []; var stream = [];
  tags.forEach(function (tag) {
    var t = sfnt.tables[tag];
    var idx = KNOWN.indexOf(tag);
    var version = 0; var transformed = null;
    if (transform && tag === 'glyf') { transformed = gl.data; } else if (transform && tag === 'loca') { transformed = Buffer.alloc(0); }
    else if (transform && tag === 'hmtx' && hm) { transformed = hm.data; version = 1; }
    else if (!transform && (tag === 'glyf' || tag === 'loca')) { version = 3; }
    var flagByte = (idx >= 0 ? idx : 63) | (version << 6);
    dir.push(flagByte);
    if (idx < 0) { Array.prototype.push.apply(dir, [tag.charCodeAt(0), tag.charCodeAt(1), tag.charCodeAt(2), tag.charCodeAt(3)]); }
    Array.prototype.push.apply(dir, base128(t.length));
    if (transformed !== null) { Array.prototype.push.apply(dir, base128(transformed.length)); stream.push(transformed); } else { stream.push(t); }
  });
  var compressed = brotli(Buffer.concat(stream));
  var sfntSize = 12 + 16 * tags.length;
  tags.forEach(function (tag) { sfntSize += (sfnt.tables[tag].length + 3) & ~3; });
  var meta = null; var metaOrig = 0; var priv = null;
  if (extras) {
    var xml = Buffer.from('<?xml version="1.0" encoding="UTF-8"?><metadata version="1.0"><uniqueid id="swingwebpanel.woff2.test"/><description><text lang="en">Test font</text><text lang="ko">시험 글꼴</text></description></metadata>', 'utf8');
    metaOrig = xml.length;
    meta = brotli(xml);
    priv = Buffer.from('private data of the test 시험 비공개 데이터', 'utf8');
  }
  var dirBuf = Buffer.from(dir);
  var body = [dirBuf, compressed];
  var offset = 48 + dirBuf.length + compressed.length;
  var metaOffset = 0; var privOffset = 0;
  if (meta) {
    var padC = (4 - (offset & 3)) & 3;
    body.push(Buffer.alloc(padC)); offset += padC;
    metaOffset = offset; body.push(meta); offset += meta.length;
    var padM = (4 - (offset & 3)) & 3;
    body.push(Buffer.alloc(padM)); offset += padM;
    privOffset = offset; body.push(priv); offset += priv.length;
  }
  var padE = (4 - (offset & 3)) & 3;
  body.push(Buffer.alloc(padE)); offset += padE;
  var h = Buffer.alloc(48);
  h.write('wOF2', 0, 'latin1');
  h.writeUInt32BE(sfnt.flavor, 4);
  h.writeUInt32BE(offset, 8);
  h.writeUInt16BE(tags.length, 12);
  h.writeUInt16BE(0, 14);
  h.writeUInt32BE(sfntSize, 16);
  h.writeUInt32BE(compressed.length, 20);
  h.writeUInt16BE(1, 24);
  h.writeUInt16BE(0, 26);
  h.writeUInt32BE(metaOffset, 28);
  h.writeUInt32BE(meta ? meta.length : 0, 32);
  h.writeUInt32BE(metaOrig, 36);
  h.writeUInt32BE(privOffset, 40);
  h.writeUInt32BE(priv ? priv.length : 0, 44);
  return { file: Buffer.concat([h].concat(body)), info: gl ? { stats: gl.stats, hmtxFlags: hm ? hm.flags : -1, indexFormat: gl.indexFormat, numGlyphs: gl.numGlyphs } : null };
}


// ---------------------------------------------------------------------------------------------------- the synthetic TrueType font
// A small deterministic pseudo random generator (xorshift32). 작은 결정적 의사 난수 생성기.
function rng(seed) {
  var s = seed >>> 0;
  return function () {
    s ^= s << 13; s >>>= 0; s ^= s >>> 17; s ^= s << 5; s >>>= 0;
    return s;
  };
}

function bytes(r, n) {
  var b = [];
  for (var i = 0; i < n; i++) { b.push(r() & 255); }
  return b;
}

// A simple glyph: one to four contours of one to thirty points; the steps are of every size class so that the triplet rows of every byte count are used.
// 단순 글리프: 윤곽선 1~4 개, 점 1~30 개. 걸음의 크기를 여러 등급으로 해 바이트 수가 다른 삼중 행을 모두 쓴다.
function simpleGlyph(r) {
  var sizes = [3, 15, 63, 255, 1100, 4000, 20000, 0];
  var contours = 1 + r() % 4;
  var xs = []; var ys = []; var on = []; var endPts = [];
  var x = (r() % 400) - 200;
  var y = (r() % 400) - 200;
  for (var c = 0; c < contours; c++) {
    var np = 1 + r() % 30;
    for (var p = 0; p < np; p++) {
      var cx = sizes[r() % sizes.length];
      var cy = sizes[r() % sizes.length];
      var dx = cx === 0 ? 0 : r() % (cx + 1);
      var dy = cy === 0 ? 0 : r() % (cy + 1);
      if (r() & 1) { dx = -dx; }
      if (r() & 1) { dy = -dy; }
      if (Math.abs(x + dx) > 32000) { dx = -dx; }
      if (Math.abs(y + dy) > 32000) { dy = -dy; }
      x += dx; y += dy;
      xs.push(x); ys.push(y); on.push(r() % 3 !== 0);
    }
    endPts.push(xs.length - 1);
  }
  var box = [Math.min.apply(null, xs), Math.min.apply(null, ys), Math.max.apply(null, xs), Math.max.apply(null, ys)];
  if (r() % 8 === 0) { box[2] += 1; }
  return { kind: 1, endPts: endPts, xs: xs, ys: ys, on: on, box: box, instr: r() % 4 === 0 ? bytes(r, 1 + r() % 20) : [], overlap: r() % 20 === 0 };
}

// The TrueType record of a simple glyph with repeated flags. 반복 플래그를 쓴 단순 글리프의 TrueType 기록.
function encodeSimple(g) {
  var out = [];
  function w16(v) { out.push((v >> 8) & 255, v & 255); }
  w16(g.endPts.length);
  g.box.forEach(w16);
  g.endPts.forEach(w16);
  w16(g.instr.length);
  g.instr.forEach(function (b) { out.push(b); });
  var flags = []; var xb = []; var yb = [];
  var px = 0; var py = 0;
  for (var i = 0; i < g.xs.length; i++) {
    var dx = g.xs[i] - px; var dy = g.ys[i] - py;
    var f = g.on[i] ? 1 : 0;
    if (dx === 0) { f |= 0x10; } else if (Math.abs(dx) <= 255) { f |= 0x02 | (dx > 0 ? 0x10 : 0); xb.push(Math.abs(dx)); } else { xb.push((dx >> 8) & 255, dx & 255); }
    if (dy === 0) { f |= 0x20; } else if (Math.abs(dy) <= 255) { f |= 0x04 | (dy > 0 ? 0x20 : 0); yb.push(Math.abs(dy)); } else { yb.push((dy >> 8) & 255, dy & 255); }
    if (i === 0 && g.overlap) { f |= 0x40; }
    flags.push(f);
    px = g.xs[i]; py = g.ys[i];
  }
  var k = 0;
  while (k < flags.length) {
    var j = k + 1;
    while (j < flags.length && flags[j] === flags[k] && j - k < 256) { j++; }
    if (j - k >= 2) { out.push(flags[k] | 8, j - k - 1); } else { out.push(flags[k]); }
    k = j;
  }
  Array.prototype.push.apply(out, xb);
  Array.prototype.push.apply(out, yb);
  return Buffer.from(out);
}

// A composite glyph of one to three components of the glyphs before it, with every kind of arguments and scale and sometimes instructions.
// 앞의 글리프를 한 개에서 세 개 쓰는 합성 글리프. 모든 종류의 인수와 크기 조정이 있고 가끔 명령이 있다.
function compositeGlyph(r, index) {
  var count = 1 + r() % 3;
  var out = [];
  function w16(v) { out.push((v >> 8) & 255, v & 255); }
  var box = [-(r() % 100), -(r() % 100), 200 + r() % 400, 300 + r() % 400];
  w16(0xFFFF);
  box.forEach(w16);
  var haveInstr = false;
  for (var j = 0; j < count; j++) {
    var last = j === count - 1;
    var flags = 0x0002;
    var words = r() & 1;
    if (words) { flags |= 0x0001; }
    var scale = r() % 4;
    if (scale === 1) { flags |= 0x0008; } else if (scale === 2) { flags |= 0x0040; } else if (scale === 3) { flags |= 0x0080; }
    if (!last) { flags |= 0x0020; }
    if (last && r() % 3 === 0) { flags |= 0x0100; haveInstr = true; }
    w16(flags);
    w16(r() % index);
    if (words) { w16(r() & 0xFFFF); w16(r() & 0xFFFF); } else { out.push(r() & 255, r() & 255); }
    var extra = scale === 0 ? 0 : (scale === 1 ? 2 : (scale === 2 ? 4 : 8));
    Array.prototype.push.apply(out, bytes(r, extra));
  }
  if (haveInstr) {
    var il = 1 + r() % 10;
    w16(il);
    Array.prototype.push.apply(out, bytes(r, il));
  }
  return { kind: 2, buffer: Buffer.from(out), box: box };
}

function checksum(buf) {
  var sum = 0;
  for (var i = 0; i < buf.length; i += 4) {
    var w = 0;
    for (var k = 0; k < 4; k++) { w = w * 256 + (i + k < buf.length ? buf[i + k] : 0); }
    sum = (sum + w) % 4294967296;
  }
  return sum;
}

// Puts tables together to an sfnt file with the checksums of the tables and the checkSumAdjustment of the head table. 테이블을 체크섬과 checkSumAdjustment 가 있는 sfnt 파일로 맞춘다.
function buildSfnt(tableMap) {
  var tags = Object.keys(tableMap).sort(function (a, b) { return a < b ? -1 : a > b ? 1 : 0; });
  var n = tags.length;
  var entrySelector = 0;
  while ((1 << (entrySelector + 1)) <= n) { entrySelector++; }
  var searchRange = (1 << entrySelector) * 16;
  var total = 12 + 16 * n;
  tags.forEach(function (t) { total += (tableMap[t].length + 3) & ~3; });
  var f = Buffer.alloc(total);
  f.writeUInt32BE(0x00010000, 0);
  f.writeUInt16BE(n, 4);
  f.writeUInt16BE(searchRange, 6);
  f.writeUInt16BE(entrySelector, 8);
  f.writeUInt16BE(n * 16 - searchRange, 10);
  var pos = 12 + 16 * n;
  var headAt = -1;
  tags.forEach(function (t, i) {
    var data = Buffer.from(tableMap[t]);
    if (t === 'head') { data.writeUInt32BE(0, 8); headAt = pos; }
    data.copy(f, pos);
    f.write(t, 12 + 16 * i, 4, 'latin1');
    f.writeUInt32BE(checksum(data), 12 + 16 * i + 4);
    f.writeUInt32BE(pos, 12 + 16 * i + 8);
    f.writeUInt32BE(tableMap[t].length, 12 + 16 * i + 12);
    pos += (data.length + 3) & ~3;
  });
  f.writeUInt32BE((0xB1B0AFBA - checksum(f) + 4294967296) % 4294967296, headAt + 8);
  return f;
}

// A font of 240 glyphs: empty glyphs, simple and composite glyphs, 200 long horizontal metrics, a short loca table, the other tables with arbitrary content (known tags, a tag
// that is not known and a DSIG table that the encoder must drop). 글리프 240 개의 글꼴: 빈 글리프, 단순·합성 글리프, 긴 가로 지표 200 개, 짧은 loca, 내용이 임의인 다른 테이블(알려진 태그,
// 알려지지 않은 태그, 인코더가 지워야 하는 DSIG).
function syntheticFont() {
  var GLYPHS = 240;
  var METRICS = 200;
  var r = rng(20261118);
  var recs = []; var xmins = [];
  for (var i = 0; i < GLYPHS; i++) {
    var buf;
    var xmin = 0;
    if (i === 0 || i % 17 === 5) {
      buf = Buffer.alloc(0);
    } else if (i > 8 && r() % 5 === 0) {
      var cg = compositeGlyph(r, i);
      buf = cg.buffer; xmin = cg.box[0];
    } else {
      var sg = simpleGlyph(r);
      buf = encodeSimple(sg); xmin = sg.box[0];
    }
    recs.push(buf); xmins.push(xmin);
  }
  var glyf = []; var offsets = [0]; var len = 0;
  recs.forEach(function (b) {
    glyf.push(b);
    var padded = (b.length + 3) & ~3;
    if (padded > b.length) { glyf.push(Buffer.alloc(padded - b.length)); }
    len += padded;
    offsets.push(len);
  });
  var glyfBuf = Buffer.concat(glyf);
  var loca = Buffer.alloc(2 * (GLYPHS + 1));
  offsets.forEach(function (o, i) { loca.writeUInt16BE(o / 2, 2 * i); });
  var hmtx = Buffer.alloc(4 * METRICS + 2 * (GLYPHS - METRICS));
  for (var g = 0; g < GLYPHS; g++) {
    var lsb = xmins[g] + (g === GLYPHS - 1 ? 1 : 0);
    if (g < METRICS) { hmtx.writeUInt16BE(300 + r() % 900, 4 * g); hmtx.writeInt16BE(lsb, 4 * g + 2); } else { hmtx.writeInt16BE(lsb, 4 * METRICS + 2 * (g - METRICS)); }
  }
  var head = Buffer.alloc(54);
  head.writeUInt32BE(0x00010000, 0); head.writeUInt32BE(0x00010000, 4); head.writeUInt32BE(0x5F0F3CF5, 12); head.writeUInt16BE(0x000B, 16); head.writeUInt16BE(1000, 18);
  head.writeInt16BE(2, 48); head.writeInt16BE(0, 50);
  var hhea = Buffer.alloc(36);
  hhea.writeUInt32BE(0x00010000, 0); hhea.writeInt16BE(800, 4); hhea.writeInt16BE(-200, 6); hhea.writeUInt16BE(METRICS, 34);
  var maxp = Buffer.alloc(32);
  maxp.writeUInt32BE(0x00010000, 0); maxp.writeUInt16BE(GLYPHS, 4);
  return buildSfnt({ 'OS/2': Buffer.from(bytes(r, 96)), cmap: Buffer.from(bytes(r, 72)), 'cvt ': Buffer.from(bytes(r, 40)), fpgm: Buffer.from(bytes(r, 30)), prep: Buffer.from(bytes(r, 25)),
    name: Buffer.from(bytes(r, 120)), post: Buffer.from(bytes(r, 32)), Test: Buffer.from(bytes(r, 9)), DSIG: Buffer.from(bytes(r, 20)), glyf: glyfBuf, loca: loca, hmtx: hmtx, head: head,
    hhea: hhea, maxp: maxp });
}

var font = syntheticFont();
fs.writeFileSync(path.join(outDir, 'synthetic.ttf'), font);
var index = [];
[['xf', true, false], ['null', false, false], ['meta', true, true]].forEach(function (v) {
  var r = woff2(font, v[1], v[2]);
  var file = 'synthetic-' + v[0] + '.woff2';
  fs.writeFileSync(path.join(outDir, file), r.file);
  index.push(['synthetic', 'synthetic.ttf', v[0], file].join(' '));
  console.log(file + ' ' + r.file.length + ' bytes' + (r.info ? ' ' + JSON.stringify({ glyphs: r.info.numGlyphs, simple: r.info.stats.simple, composite: r.info.stats.composite,
    empty: r.info.stats.empty, explicitBox: r.info.stats.explicitBox, triplet: Object.keys(r.info.stats.rowsUsed).length, hmtx: r.info.hmtxFlags, loca: r.info.indexFormat }) : ''));
});
fs.writeFileSync(path.join(outDir, 'index.txt'), index.join('\n') + '\n');
console.log('wrote synthetic.ttf (' + font.length + ' bytes) and ' + index.length + ' WOFF2 files to ' + outDir);
