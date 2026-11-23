// Generates the Brotli test vectors of tests/src/org/duckdns/hjow/swingwebpanel/media/font/BrotliTest.java with the reference Brotli encoder that ships with Node.js
// (zlib.brotliCompressSync, Node.js v26.13.0 on the machine where the vectors were made, 2026-11-18). Run it once by hand; the tests and the product do not need Node.js:
//   node tests/resources/org/duckdns/hjow/swingwebpanel/media/font/generation/make-brotli-vectors.js
// Output (next to this directory, in ../brotli/; no font of the JRE is used, see the comment of the vector "structured"): <name>.raw (the input), <name>-<variant>.br (the compressed stream) and index.txt
// whose lines are "<name> <raw file> <br file> <mode> <quality> <lgwin>". The raw inputs are made deterministically here (no random numbers from the system).
// 이 스크립트는 Node.js 에 들어 있는 참조 Brotli 인코더(zlib.brotliCompressSync)로 시험 자료를 만든다. 사람이 한 번 실행해 결과를 저장소에 두며 시험과 제품은 Node.js 를 쓰지 않는다.
'use strict';
var fs = require('fs');
var path = require('path');
var zlib = require('zlib');
var c = zlib.constants;

var outDir = path.join(__dirname, '..', 'brotli');
fs.mkdirSync(outDir, { recursive: true });

// A small deterministic pseudo random generator (xorshift32). 작은 결정적 의사 난수 생성기.
function rng(seed) {
  var s = seed >>> 0;
  return function () {
    s ^= s << 13; s >>>= 0; s ^= s >>> 17; s ^= s << 5; s >>>= 0;
    return s;
  };
}

var words = ['the', 'of', 'and', 'to', 'in', 'is', 'that', 'for', 'it', 'as', 'was', 'with', 'be', 'by', 'on', 'not', 'he', 'this', 'are', 'or', 'his', 'from', 'at',
  'which', 'but', 'have', 'an', 'had', 'they', 'you', 'were', 'their', 'one', 'all', 'we', 'can', 'her', 'has', 'there', 'been', 'if', 'more', 'when', 'will', 'would',
  'who', 'so', 'no', 'Information', 'Download', 'Welcome', 'Copyright', 'Contact', 'Search', 'System', 'Service', 'Business', 'Government', 'University', 'Software',
  'international', 'development', 'management', 'application', 'community', 'technology', '<div class="', '</div>', '<a href="http://www.', '.com/', ' &amp; ', '\n', '. ', ', '];

function text(n, seed) {
  var r = rng(seed);
  var parts = [];
  var len = 0;
  while (len < n) {
    var w = words[r() % words.length];
    if (r() % 7 === 0) { w = w.charAt(0).toUpperCase() + w.slice(1); }
    if (r() % 31 === 0) { w = w.toUpperCase(); }
    parts.push(w);
    len += w.length + 1;
  }
  return Buffer.from(parts.join(' ').slice(0, n), 'utf8');
}

function randomBytes(n, seed) {
  var r = rng(seed);
  var b = Buffer.alloc(n);
  for (var i = 0; i < n; i++) { b[i] = r() & 255; }
  return b;
}

function repeated(n) {
  var b = Buffer.alloc(n);
  var pat = 'abcabcabd';
  for (var i = 0; i < n; i++) { b[i] = pat.charCodeAt(i % pat.length); }
  return b;
}

function samples(n, seed) {
  // 16 bit little endian samples of a slowly changing signal: good for the signed context mode. 천천히 변하는 신호의 16 비트 표본.
  var r = rng(seed);
  var b = Buffer.alloc(n * 2);
  var v = 0;
  for (var i = 0; i < n; i++) {
    v += (r() % 41) - 20;
    b.writeInt16LE(Math.max(-32768, Math.min(32767, v)), i * 2);
  }
  return b;
}

function utf8(n) {
  var parts = [];
  var s = '한국어 문장입니다. 안녕하세요 세계! Привет мир. こんにちは世界。 ';
  while (parts.join('').length < n) { parts.push(s); }
  return Buffer.from(parts.join('').slice(0, n), 'utf8');
}

var index = [];
function vector(name, data, variants) {
  fs.writeFileSync(path.join(outDir, name + '.raw'), data);
  variants.forEach(function (v) {
    var params = {};
    params[c.BROTLI_PARAM_MODE] = v.mode;
    params[c.BROTLI_PARAM_QUALITY] = v.quality;
    params[c.BROTLI_PARAM_LGWIN] = v.lgwin;
    params[c.BROTLI_PARAM_SIZE_HINT] = data.length;
    var br = zlib.brotliCompressSync(data, { params: params });
    var file = name + '-' + v.mode + '-q' + v.quality + '-w' + v.lgwin + '.br';
    fs.writeFileSync(path.join(outDir, file), br);
    index.push([name, name + '.raw', file, v.mode, v.quality, v.lgwin].join(' '));
  });
}

var G = c.BROTLI_MODE_GENERIC;
var T = c.BROTLI_MODE_TEXT;
var F = c.BROTLI_MODE_FONT;

vector('empty', Buffer.alloc(0), [{ mode: G, quality: 11, lgwin: 22 }, { mode: G, quality: 0, lgwin: 16 }]);
vector('one', Buffer.from('X'), [{ mode: G, quality: 11, lgwin: 22 }]);
vector('hello', Buffer.from('hello, hello, hello world'), [{ mode: G, quality: 5, lgwin: 16 }, { mode: T, quality: 11, lgwin: 10 }]);
vector('english', text(6000, 12345), [
  { mode: T, quality: 11, lgwin: 22 }, { mode: T, quality: 9, lgwin: 16 }, { mode: T, quality: 5, lgwin: 18 }, { mode: G, quality: 1, lgwin: 14 },
  { mode: T, quality: 0, lgwin: 10 }, { mode: T, quality: 11, lgwin: 24 }]);
vector('random', randomBytes(4000, 777), [{ mode: G, quality: 11, lgwin: 22 }, { mode: G, quality: 1, lgwin: 16 }]);
vector('repeat', repeated(30000), [{ mode: G, quality: 11, lgwin: 22 }, { mode: G, quality: 3, lgwin: 12 }]);
vector('samples', samples(8000, 99), [{ mode: G, quality: 11, lgwin: 22 }, { mode: F, quality: 9, lgwin: 18 }]);
vector('utf8', utf8(5000), [{ mode: T, quality: 11, lgwin: 22 }, { mode: T, quality: 7, lgwin: 16 }]);
// A long text: several meta-blocks and block types at the higher qualities. 긴 텍스트: 높은 품질에서는 메타 블록과 블록 종류가 여러 개.
vector('long', text(90000, 4242), [{ mode: T, quality: 11, lgwin: 22 }, { mode: T, quality: 6, lgwin: 20 }]);

// A large binary input that is made here and is not a real font: 12 byte records (a growing identifier, a type from a small set, two coordinates that move slowly and a flag
// word) followed by a pool of short words, 150 000 bytes in all. It is compressed in the font mode (the mode that WOFF2 encoders use) so that the decoder meets a long input with
// many repetitions, distances beyond the first meta-block and the binary statistics of table data. The fonts of the JRE (Lucida of Bigelow & Holmes) may be used with Java only,
// so no Brotli stream of a real font is kept in the repository: the test of the real fonts does not compare compressed font bytes (Node.js cannot compress the font of the JRE
// that runs the test, and the tests do not depend on Node.js); it reads the font file of the JRE at test time and goes through the WOFF2 decoder with uncompressed blocks.
// 실제 글꼴이 아닌 직접 만든 큰 이진 입력: 12 바이트 기록(커지는 식별자, 작은 집합의 종류, 천천히 움직이는 좌표 둘, 플래그 단어)과 짧은 낱말 모음, 모두 150,000 바이트. WOFF2 인코더가
// 쓰는 글꼴 모드로 압축해 디코더가 반복이 많은 긴 입력, 첫 메타 블록을 넘는 거리, 테이블 데이터 같은 이진 통계를 만나게 한다. JRE 의 글꼴(Bigelow & Holmes 의 Lucida)은 Java 에서만 쓰도록
// 허락된 것이므로 실제 글꼴의 Brotli 스트림은 저장소에 두지 않는다: 실제 글꼴 시험은 압축한 글꼴 바이트를 비교하지 않고(Node.js 는 시험을 실행하는 JRE 의 글꼴을 압축할 수 없고 시험은
// Node.js 에 의존하지 않는다) 시험 때 JRE 의 글꼴 파일을 읽어 압축하지 않은 블록의 WOFF2 로 디코더를 거친다.
function structured(n, seed) {
  var r = rng(seed);
  var b = Buffer.alloc(n);
  var pool = ['glyph', 'contour', 'advance', 'bearing', 'kerning', 'ligature', 'hinting', 'outline', 'metrics', 'compound'];
  var records = Math.floor((n * 3 / 4) / 12);
  var id = 0; var x = 500; var y = 300;
  for (var i = 0; i < records; i++) {
    id += 1 + (r() % 3 === 0 ? r() % 5 : 0);
    x = (x + (r() % 41) - 20) & 0xFFFF;
    y = (y + (r() % 41) - 20) & 0xFFFF;
    b.writeUInt32BE(id, i * 12);
    b.writeUInt16BE(r() % 7, i * 12 + 4);
    b.writeUInt16BE(x, i * 12 + 6);
    b.writeUInt16BE(y, i * 12 + 8);
    b.writeUInt16BE(r() % 11 === 0 ? 0x8000 | (r() & 255) : 0, i * 12 + 10);
  }
  var pos = records * 12;
  while (pos < n) {
    var w = pool[r() % pool.length] + (r() % 4 === 0 ? String(r() % 100) : '') + ' ';
    b.write(w, pos, 'latin1');
    pos += w.length;
  }
  return b.slice(0, n);
}
vector('structured', structured(150000, 31337), [{ mode: F, quality: 11, lgwin: 22 }, { mode: F, quality: 5, lgwin: 16 }]);


fs.writeFileSync(path.join(outDir, 'index.txt'), index.join('\n') + '\n');
console.log('wrote ' + index.length + ' vectors to ' + outDir);
