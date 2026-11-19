package org.duckdns.hjow.swingwebpanel.media.font;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the WOFF2 decoder (plan item P10-08-e, W3C Recommendation "WOFF File Format 2.0", https://www.w3.org/TR/WOFF2/, last modified 2024-08-05). Three kinds of
 * input are used. (1) Files that <code>generation/make-woff2-vectors.js</code> made from a synthetic TrueType font with a WOFF2 encoder written for the tests and the
 * reference Brotli encoder of Node.js (transformed <code>glyf</code>/<code>loca</code>/<code>hmtx</code>, the same font without transforms, a file with extended metadata
 * and private data), and the TrueType fonts of the JRE that runs the test, converted at test time by {@link Woff2Builder} (no file made from them is kept in the
 * repository, see {@link Woff2Fonts}): the decoded font must equal the original font table by table, glyph by glyph (points, flags, instructions, composites, boxes)
 * and, for the JRE fonts, through Java2D in the outlines and advances of hundreds of characters; the checksums and the <code>checkSumAdjustment</code> must be right.
 * The synthetic font is also encoded by an independent encoder, fontTools (<code>generation/make-woff2-fonttools.py</code>), and those files must decode to it too. (2) Synthetic fonts that {@link Woff2Builder}
 * encodes with Brotli stored blocks: every one of the 128 triplet rows with the smallest, a middle and the largest value, all forms of <code>255UInt16</code>,
 * composite glyphs with every kind of scale, instructions, explicit boxes, overlap flags, short and long <code>loca</code>, the <code>hmtx</code> transform flags, custom
 * and explicit tags. (3) Invalid files: every rule that the specification gives for a decoder to reject (and every truncation and random byte changes) must end in an
 * {@link IOException} and never in another exception. The encoders and the decoder were written from the same specification text, which the class comment of
 * {@link Woff2} states as the limit of this evidence.
 * <p>
 * WOFF2 디코더(계획 P10-08-e, W3C 권고안 "WOFF File Format 2.0", https://www.w3.org/TR/WOFF2/, 최종 수정 2024-08-05)의 시험. 입력은 세 가지이다. (1) <code>generation/make-woff2-vectors.js</code> 가
 * 시험용으로 쓴 WOFF2 인코더와 Node.js 의 참조 Brotli 인코더로 합성 TrueType 글꼴에서 만든 파일(변환된 <code>glyf</code>/<code>loca</code>/<code>hmtx</code>, 변환 없는 같은 글꼴,
 * 확장 메타데이터와 비공개 데이터가 있는 파일)과 시험을 실행하는 JRE 의 TrueType 글꼴을 시험 때 {@link Woff2Builder} 로 바꾼 것(그것으로 만든 파일은 저장소에 두지 않는다.
 * {@link Woff2Fonts} 참고): 풀린 글꼴은 원본과 테이블별, 글리프별(점, 플래그, 명령, 합성, 상자)로, JRE 글꼴은 Java2D 를 통해 수백 글자의 윤곽선과 진행 폭까지 같아야 하고 체크섬과
 * <code>checkSumAdjustment</code> 가 맞아야 한다. 합성 글꼴은 독립 인코더 fontTools(<code>generation/make-woff2-fonttools.py</code>)로도
 * 인코딩하며 그 파일들도 합성 글꼴로 풀려야 한다. (2) {@link Woff2Builder} 가 Brotli 저장 블록으로 인코딩한 합성 글꼴: 128 개 삼중 행 각각의 가장 작은 값, 중간 값, 가장 큰 값, <code>255UInt16</code> 의 모든
 * 형태, 모든 종류의 크기 조정이 있는 합성 글리프, 명령, 명시한 상자, 겹침 플래그, 짧고 긴 <code>loca</code>, <code>hmtx</code> 변환 플래그, 사용자 정의 태그와 명시한 태그. (3) 잘못된 파일: 규격이
 * 디코더가 거부하라고 한 모든 규칙(과 모든 잘림, 무작위 바이트 변경)은 다른 예외가 아니라 {@link IOException} 으로 끝나야 한다. 인코더와 디코더를 같은 규격 텍스트로 썼다는 것이 이 증거의 한계이며
 * {@link Woff2} 의 클래스 설명에 적었다.
 */
public class Woff2Test {

    /** The directory of the vectors on the class path. 클래스패스의 시험 자료 디렉터리. */
    private static final String DIR = "/org/duckdns/hjow/swingwebpanel/media/font/woff2/";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public Woff2Test() {
    }

    // ------------------------------------------------------------------------------------------------------ helpers

    /**
     * Reads a resource of the vectors.
     * <p>
     * 시험 자료의 리소스를 읽는다.
     *
     * @param name the file name / 파일 이름
     * @return the bytes / 바이트
     * @throws IOException if it cannot be read / 읽을 수 없으면
     */
    private static byte[] resource(String name) throws IOException {
        InputStream in = Woff2Test.class.getResourceAsStream(DIR + name);
        Check.notNull(in, "the resource " + name);
        try {
            return readAll(in);
        } finally {
            in.close();
        }
    }

    /**
     * Reads a stream to its end.
     * <p>
     * 스트림을 끝까지 읽는다.
     *
     * @param in the stream / 스트림
     * @return the bytes / 바이트
     * @throws IOException if reading fails / 읽기에 실패하면
     */
    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        // 끝까지 읽는다.
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Reads a big-endian unsigned 16 bit number.
     * <p>
     * 빅 엔디언 부호 없는 16비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    private static int u16(byte[] b, int i) {
        return ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
    }

    /**
     * Reads a big-endian signed 16 bit number.
     * <p>
     * 빅 엔디언 부호 있는 16비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    private static int i16(byte[] b, int i) {
        return (short) u16(b, i);
    }

    /**
     * Reads a big-endian unsigned 32 bit number.
     * <p>
     * 빅 엔디언 부호 없는 32비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    private static long u32(byte[] b, int i) {
        return ((long) u16(b, i) << 16) | u16(b, i + 2);
    }

    /**
     * Reads the tables of an sfnt font in the order of its directory and checks the structure: sorted tags, 4-byte aligned offsets, ranges inside the file, the
     * checksum of every table (the head table with a zero <code>checkSumAdjustment</code>) and the checksum of the whole font.
     * <p>
     * sfnt 글꼴의 테이블을 디렉터리 순서로 읽고 구조를 검사한다: 정렬된 태그, 4 바이트 경계 오프셋, 파일 안의 범위, 모든 테이블의 체크섬(head 테이블은 <code>checkSumAdjustment</code> 를 0 으로),
     * 글꼴 전체의 체크섬.
     *
     * @param sfnt   the font / 글꼴
     * @param flavor the expected sfnt version / 기대하는 sfnt 버전
     * @return the tables by tag / 태그별 테이블
     */
    private static Map<String, byte[]> tables(byte[] sfnt, long flavor) {
        Check.equal(flavor, u32(sfnt, 0), "the sfnt version");
        int n = u16(sfnt, 4);
        int entrySelector = 0;
        // 테이블 수 이하의 가장 큰 2 의 거듭제곱을 구한다.
        while ((1 << (entrySelector + 1)) <= n) {
            entrySelector++;
        }
        Check.equal((1 << entrySelector) * 16, u16(sfnt, 6), "searchRange");
        Check.equal(entrySelector, u16(sfnt, 8), "entrySelector");
        Check.equal(n * 16 - (1 << entrySelector) * 16, u16(sfnt, 10), "rangeShift");
        Map<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        String last = null;
        // 디렉터리 항목마다 위치, 정렬, 체크섬을 검사한다.
        for (int i = 0; i < n; i++) {
            int e = 12 + 16 * i;
            String tag = new String(sfnt, e, 4, java.nio.charset.Charset.forName("ISO-8859-1"));
            Check.isTrue(last == null || last.compareTo(tag) < 0, "tags are sorted: " + last + " < " + tag);
            last = tag;
            int offset = (int) u32(sfnt, e + 8);
            int length = (int) u32(sfnt, e + 12);
            Check.isTrue(offset % 4 == 0 && offset >= 12 + 16 * n && offset + length <= sfnt.length, "the table " + tag + " is aligned and inside the file");
            byte[] t = new byte[length];
            System.arraycopy(sfnt, offset, t, 0, length);
            map.put(tag, t);
            byte[] c = t.clone();
            if ("head".equals(tag)) {
                c[8] = 0;
                c[9] = 0;
                c[10] = 0;
                c[11] = 0;
            }
            Check.equal(Woff2.checksum(c, 0, c.length), u32(sfnt, e + 4), "the checksum of the table " + tag);
        }
        if (map.containsKey("head")) {
            Check.equal(0xB1B0AFBAL, Woff2.checksum(sfnt, 0, sfnt.length), "the checksum of the whole font is 0xB1B0AFBA (checkSumAdjustment)");
        }
        return map;
    }

    /**
     * Parses the glyph at a place of a glyph table into the model (a simple glyph's overlap flag is the bit of its first flag).
     * <p>
     * 글리프 테이블의 한 곳의 글리프를 모델로 파싱한다(단순 글리프의 겹침 플래그는 첫 플래그의 비트).
     *
     * @param b   the glyph table / 글리프 테이블
     * @param off the start of the glyph / 글리프의 시작
     * @param len the length of the glyph / 글리프의 길이
     * @return the glyph with its box in <code>box</code> / <code>box</code> 에 상자가 든 글리프
     */
    private static Woff2Builder.Glyph parseGlyph(byte[] b, int off, int len) {
        return Woff2Builder.parseGlyph(b, off, len);
    }

    /**
     * Parses all glyphs of an sfnt font.
     * <p>
     * sfnt 글꼴의 모든 글리프를 파싱한다.
     *
     * @param t the tables of the font / 글꼴의 테이블
     * @return the glyphs / 글리프
     */
    private static List<Woff2Builder.Glyph> glyphs(Map<String, byte[]> t) {
        byte[] head = t.get("head");
        byte[] loca = t.get("loca");
        byte[] glyf = t.get("glyf");
        int n = u16(t.get("maxp"), 4);
        int format = u16(head, 50);
        Check.equal((n + 1) * (format == 0 ? 2 : 4), loca.length, "the length of loca");
        List<Woff2Builder.Glyph> list = new ArrayList<Woff2Builder.Glyph>();
        // 위치 테이블의 인접한 두 오프셋으로 글리프마다 파싱한다.
        for (int i = 0; i < n; i++) {
            long a = format == 0 ? 2L * u16(loca, 2 * i) : u32(loca, 4 * i);
            long z = format == 0 ? 2L * u16(loca, 2 * i + 2) : u32(loca, 4 * i + 4);
            list.add(parseGlyph(glyf, (int) a, (int) (z - a)));
        }
        return list;
    }

    /**
     * Checks that a parsed glyph equals what the model says the decoder must produce.
     * <p>
     * 파싱한 글리프가 모델이 디코더가 만들어야 한다고 하는 것과 같은지 검사한다.
     *
     * @param expected the model / 모델
     * @param actual   the parsed glyph / 파싱한 글리프
     * @param what     what is compared / 비교 대상
     */
    private static void sameGlyph(Woff2Builder.Glyph expected, Woff2Builder.Glyph actual, String what) {
        Check.equal(expected.kind, actual.kind, what + ": the kind of glyph");
        if (expected.kind == 0) {
            return;
        }
        Check.equal(expected.effectiveBox(), actual.box, what + ": the bounding box");
        Check.equal(expected.instr, actual.instr == null ? new byte[0] : actual.instr, what + ": the instructions");
        if (expected.kind == 1) {
            Check.equal(expected.endPts, actual.endPts, what + ": the end points");
            Check.equal(expected.xs, actual.xs, what + ": x");
            Check.equal(expected.ys, actual.ys, what + ": y");
            Check.isTrue(Arrays.equals(expected.on, actual.on), what + ": on-curve flags");
            Check.equal(expected.overlap ? 1 : 0, actual.overlap ? 1 : 0, what + ": the overlap flag");
        } else {
            Check.equal(expected.comp, actual.comp, what + ": the components");
        }
    }

    /**
     * Compares the glyphs of the model with the glyphs of the decoded font.
     * <p>
     * 모델의 글리프를 풀린 글꼴의 글리프와 비교한다.
     *
     * @param b      the builder that holds the model / 모델을 가진 빌더
     * @param sfnt   the decoded font / 풀린 글꼴
     * @param prefix the description / 설명
     */
    private static void checkModel(Woff2Builder b, byte[] sfnt, String prefix) {
        Map<String, byte[]> t = tables(sfnt, b.flavor);
        List<Woff2Builder.Glyph> got = glyphs(t);
        Check.equal(b.glyphs.size(), got.size(), prefix + ": the number of glyphs");
        // 글리프마다 모델과 비교한다.
        for (int i = 0; i < got.size(); i++) {
            sameGlyph(b.glyphs.get(i), got.get(i), prefix + " glyph " + i);
        }
    }

    /**
     * Checks that a file is rejected with an IOException (and no other exception).
     * <p>
     * 파일이 IOException 으로(다른 예외가 아니라) 거부되는지 검사한다.
     *
     * @param file     the file / 파일
     * @param fragment a part of the message, or null / 메시지의 일부. 없으면 null
     * @param why      what is wrong with the file / 파일의 무엇이 잘못인지
     */
    private static void rejected(byte[] file, String fragment, String why) {
        try {
            Woff2.toSfnt(file);
        } catch (IOException e) {
            if (fragment != null) {
                Check.contains(e.getMessage(), fragment, why + ": the reason");
            }
            return;
        }
        Check.fail(why + ": the file was accepted");
    }

    /**
     * Builds a font with a rich set of glyphs: empty, simple (on/off points, several contours, instructions, overlap, explicit box), contour and point counts of every
     * <code>255UInt16</code> form, large instruction lengths, large deltas and composite glyphs with every kind of scale.
     * <p>
     * 풍부한 글리프 집합을 가진 글꼴을 만든다: 빈 글리프, 단순 글리프(곡선 위/밖 점, 여러 윤곽선, 명령, 겹침, 명시한 상자), 모든 <code>255UInt16</code> 형태의 윤곽선·점 수, 큰 명령 길이, 큰
     * 변화량, 모든 종류의 크기 조정이 있는 합성 글리프.
     *
     * @param indexFormat the loca format / loca 형식
     * @return the builder / 빌더
     */
    private static Woff2Builder richFont(int indexFormat) {
        Woff2Builder b = new Woff2Builder();
        b.indexFormat = indexFormat;
        List<Woff2Builder.Glyph> g = b.glyphs;
        g.add(Woff2Builder.Glyph.empty());
        g.add(Woff2Builder.Glyph.simple(new int[] { 2 }, new int[] { 0, 100, 50 }, new int[] { 0, 0, 80 }, new boolean[] { true, true, false }));
        Woff2Builder.Glyph two = Woff2Builder.Glyph.simple(new int[] { 3, 7 }, new int[] { 10, 90, 90, 10, 30, 70, 70, 30 }, new int[] { 10, 10, 90, 90, 30, 30, 70, 70 },
                new boolean[] { true, false, true, false, true, true, false, false });
        two.instr = new byte[] { 0x40, 0x01, 0x02, 0x03, (byte) 0xB0 };
        two.overlap = true;
        g.add(two);
        Woff2Builder.Glyph boxed = Woff2Builder.Glyph.simple(new int[] { 1 }, new int[] { 5, 25 }, new int[] { 5, 45 }, new boolean[] { true, true });
        boxed.box = new int[] { -10, -20, 200, 300 };
        g.add(boxed);
        g.add(staircase(800, 1));
        Woff2Builder.Glyph twoBig = staircaseTwo();
        g.add(twoBig);
        // 합성 글리프: 구성 요소 셋(단어 인수와 크기 하나, XY 크기, 2x2 행렬과 명령 플래그).
        byte[] comp = { 0x00, 0x2B, 0x00, 0x01, 0x00, 0x10, 0x00, 0x20, 0x40, 0x00, 0x00, 0x62, 0x00, 0x02, 0x05, 0x06, 0x40, 0x00, 0x20, 0x00, 0x01, (byte) 0x82, 0x00, 0x03, 0x07, 0x08, 0x40,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00 };
        g.add(Woff2Builder.Glyph.composite(comp, new int[] { 0, 0, 100, 80 }, new byte[] { 0x11, 0x22, 0x33 }));
        g.add(Woff2Builder.Glyph.composite(new byte[] { 0x00, 0x02, 0x00, 0x02, 0x05, 0x06 }, new int[] { 5, 6, 7, 8 }, new byte[0]));
        Woff2Builder.Glyph longInstr = Woff2Builder.Glyph.simple(new int[] { 3 }, new int[] { 0, 10, 10, 0 }, new int[] { 0, 0, 10, 10 }, new boolean[] { true, true, true, true });
        longInstr.instr = new byte[1000];
        // 명령 길이가 255UInt16 의 모든 형태를 쓰도록 바이트 값을 채운다.
        for (int i = 0; i < longInstr.instr.length; i++) {
            longInstr.instr[i] = (byte) (i * 7);
        }
        g.add(longInstr);
        Woff2Builder.Glyph mid = Woff2Builder.Glyph.simple(new int[] { 3 }, new int[] { 0, 10, 10, 0 }, new int[] { 0, 0, 10, 10 }, new boolean[] { true, true, true, true });
        mid.instr = new byte[300];
        g.add(mid);
        Woff2Builder.Glyph mid2 = Woff2Builder.Glyph.simple(new int[] { 3 }, new int[] { 0, 10, 10, 0 }, new int[] { 0, 0, 10, 10 }, new boolean[] { true, true, true, true });
        mid2.instr = new byte[600];
        g.add(mid2);
        g.add(Woff2Builder.Glyph.simple(new int[] { 0 }, new int[] { 7 }, new int[] { -9 }, new boolean[] { false }));
        g.add(Woff2Builder.Glyph.simple(new int[] { 3 }, new int[] { 0, 300, 1500, -20000 }, new int[] { 0, -700, 2000, 20000 }, new boolean[] { true, false, true, false }));
        return b;
    }

    /**
     * Builds a simple glyph with many points on a staircase (one contour).
     * <p>
     * 계단 모양으로 늘어선 많은 점이 있는 단순 글리프를 만든다(윤곽선 하나).
     *
     * @param points the number of points / 점 수
     * @param seed   changes the shape / 모양을 바꾸는 값
     * @return the glyph / 글리프
     */
    private static Woff2Builder.Glyph staircase(int points, int seed) {
        int[] xs = new int[points];
        int[] ys = new int[points];
        boolean[] on = new boolean[points];
        // 점을 계단으로 놓는다.
        for (int i = 0; i < points; i++) {
            xs[i] = (i % 10) * 7 * seed;
            ys[i] = i * 3;
            on[i] = i % 3 != 0;
        }
        return Woff2Builder.Glyph.simple(new int[] { points - 1 }, xs, ys, on);
    }

    /**
     * Builds a simple glyph with a contour of 300 points and a contour of 600 points (the point counts need the 255 and the 254 form of <code>255UInt16</code>).
     * <p>
     * 점 300 개의 윤곽선과 점 600 개의 윤곽선이 있는 단순 글리프를 만든다(점 수가 <code>255UInt16</code> 의 255 형태와 254 형태를 필요로 한다).
     *
     * @return the glyph / 글리프
     */
    private static Woff2Builder.Glyph staircaseTwo() {
        int n = 900;
        int[] xs = new int[n];
        int[] ys = new int[n];
        boolean[] on = new boolean[n];
        // 두 윤곽선의 점을 놓는다.
        for (int i = 0; i < n; i++) {
            xs[i] = (i % 17) * 5 - 40 + (i >= 300 ? 400 : 0);
            ys[i] = (i / 17) * 4 - (i >= 300 ? 100 : 0);
            on[i] = i % 2 == 0;
        }
        return Woff2Builder.Glyph.simple(new int[] { 299, 899 }, xs, ys, on);
    }

    // ------------------------------------------------------------------------------------------------------ reference files and real fonts

    /**
     * The WOFF2 files that <code>generation/make-woff2-vectors.js</code> made from the synthetic font <code>synthetic.ttf</code> with the reference Brotli encoder of
     * Node.js (transformed tables, null transforms, extended metadata and private data) decode to that font: the structure and the checksums are right, every table
     * apart from <code>glyf</code> and <code>loca</code> has the same bytes, the head table differs only in <code>checkSumAdjustment</code> and bit 11 of its flags
     * (which the encoder sets) and every glyph has the same points, flags, instructions, composite records and box. The synthetic font has no usable character map, so
     * its outlines are not compared through Java2D (the real fonts are, in {@link #testJreFontsDecodeToTheOriginal()}).
     * <p>
     * <code>generation/make-woff2-vectors.js</code> 가 합성 글꼴 <code>synthetic.ttf</code> 로 Node.js 의 참조 Brotli 인코더를 써서 만든 WOFF2 파일(변환한 테이블, 널 변환, 확장 메타데이터와
     * 비공개 데이터)이 그 글꼴로 풀린다: 구조와 체크섬이 맞고 <code>glyf</code>, <code>loca</code> 를 뺀 모든 테이블의 바이트가 같으며 head 테이블은 <code>checkSumAdjustment</code> 와
     * flags 의 11 번 비트(인코더가 켬)만 다르고 모든 글리프의 점, 플래그, 명령, 합성 기록, 상자가 같다. 합성 글꼴에는 쓸 수 있는 문자 대응표가 없으므로 Java2D 로 윤곽선을 비교하지 않는다(실제 글꼴은
     * {@link #testJreFontsDecodeToTheOriginal()} 에서 비교한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReferenceFilesDecodeToTheSyntheticFont() throws Exception {
        String[] lines = new String(resource("index.txt"), "UTF-8").trim().split("\n");
        Check.equal(3, lines.length, "the index lists the files");
        Map<String, byte[]> byVariant = new LinkedHashMap<String, byte[]>();
        // 색인의 파일마다 풀고 구조를 검사한 뒤 원본 합성 글꼴과 비교한다.
        for (int i = 0; i < lines.length; i++) {
            String[] f = lines[i].trim().split(" ");
            Check.equal(4, f.length, "the index line " + lines[i]);
            byte[] original = resource(f[1]);
            byte[] file = resource(f[3]);
            Check.isTrue(Woff2.isWoff2(file), f[3] + " has the WOFF2 signature");
            byte[] sfnt = Woff2.toSfnt(file);
            byVariant.put(f[2], sfnt);
            sameFont(original, sfnt, "null".equals(f[2]), f[3]);
        }
        Check.equal(byVariant.get("xf"), byVariant.get("meta"), "the file with metadata and private data decodes to the same font");
    }

    /**
     * WOFF2 files that an independent encoder made from the synthetic font: fontTools 4.68.0 (<code>fontTools.ttLib.woff2</code> with brotli 1.2.1, made by
     * <code>generation/make-woff2-fonttools.py</code>) with transformed <code>glyf</code>/<code>loca</code> and <code>hmtx</code>, with transformed <code>glyf</code>/<code>loca</code>
     * only, and without transforms. That encoder chooses its own triplet encodings, flag runs, bounding box bitmap and Brotli settings, so these files check the
     * decoder against an encoder that its author did not write. They must decode to the synthetic font like the files of the reference script; the time stamps of
     * the head table are ignored because fontTools converts the arbitrary small values of the synthetic font when it writes the table.
     * <p>
     * 독립 인코더가 합성 글꼴로 만든 WOFF2 파일: fontTools 4.68.0(<code>fontTools.ttLib.woff2</code>, brotli 1.2.1, <code>generation/make-woff2-fonttools.py</code> 가 만듦)으로
     * <code>glyf</code>/<code>loca</code> 와 <code>hmtx</code> 를 변환한 것, <code>glyf</code>/<code>loca</code> 만 변환한 것, 변환하지 않은 것. 그 인코더는 삼중 인코딩, 플래그 반복, 경계 상자
     * 비트맵, Brotli 설정을 스스로 고르므로 이 파일들은 디코더를 그 작성자가 쓰지 않은 인코더로 확인한다. 참조 스크립트의 파일처럼 합성 글꼴로 풀려야 한다. fontTools 가 head 테이블을 쓸 때
     * 합성 글꼴의 작은 임의 시각 값을 바꾸므로 head 의 시각은 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFontToolsFilesDecodeToTheSyntheticFont() throws Exception {
        byte[] original = resource("synthetic.ttf");
        String[] files = { "synthetic-fonttools-xf.woff2", "synthetic-fonttools-glyf.woff2", "synthetic-fonttools-null.woff2" };
        // 파일마다 풀어 원본 합성 글꼴과 비교한다(마지막 파일만 변환하지 않았다).
        for (int i = 0; i < files.length; i++) {
            byte[] file = resource(files[i]);
            Check.isTrue(Woff2.isWoff2(file), files[i] + " has the WOFF2 signature");
            sameFont(original, Woff2.toSfnt(file), i == 2, true, files[i]);
        }
    }

    /**
     * The TrueType fonts of the JRE that runs the test, converted at test time by the test encoder {@link Woff2Builder} (transformed tables, null transforms,
     * extended metadata and private data; the table data is stored in Brotli blocks that are not compressed), decode to fonts whose tables and glyphs equal the original
     * ones and whose outlines and advances are the same through Java2D. No file made from these fonts is kept in the repository because they may be used with Java only
     * (see {@link Woff2Fonts}); the test is skipped when the JRE has none of them.
     * <p>
     * 시험을 실행하는 JRE 의 TrueType 글꼴을 시험 때 시험용 인코더 {@link Woff2Builder} 로 바꾼 것(변환한 테이블, 널 변환, 확장 메타데이터와 비공개 데이터. 테이블 데이터는 압축하지 않은
     * Brotli 블록에 저장)이 테이블과 글리프가 원본과 같고 Java2D 를 통한 윤곽선과 진행 폭이 같은 글꼴로 풀린다. 이 글꼴은 Java 에서만 쓰도록 허락된 것이어서 그것으로 만든 파일을 저장소에 두지
     * 않는다({@link Woff2Fonts} 참고). JRE 에 그런 글꼴이 하나도 없으면 시험을 건너뛴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testJreFontsDecodeToTheOriginal() throws Exception {
        String[] names = { "LucidaBrightDemiBold.ttf", "LucidaTypewriterBold.ttf", "LucidaSansRegular.ttf" };
        int found = 0;
        // JRE 글꼴마다 세 가지 WOFF2 파일을 만들어 풀고 원본과 비교한다.
        for (int i = 0; i < names.length; i++) {
            byte[] original = Woff2Fonts.jreFont(names[i]);
            if (original == null) {
                continue;
            }
            found++;
            byte[] xf = Woff2.toSfnt(Woff2Fonts.woff2(original, true));
            byte[] nul = Woff2.toSfnt(Woff2Fonts.woff2(original, false));
            byte[] meta = Woff2.toSfnt(Woff2Fonts.woff2WithMetadata(original));
            sameFont(original, xf, false, names[i] + " transformed");
            sameFont(original, nul, true, names[i] + " without transforms");
            Check.equal(xf, meta, names[i] + ": the file with metadata and private data decodes to the same font");
            compareOutlines(original, xf, names[i] + " transformed");
            compareOutlines(original, nul, names[i] + " without transforms");
        }
        if (found == 0) {
            Check.skip("the JRE has none of the fonts " + Arrays.asList(names) + " in lib/fonts");
        }
    }

    /**
     * Checks that a decoded font equals the original font: the same tables in the same order, the same bytes for every table apart from <code>glyf</code> and
     * <code>loca</code>, a head table that differs only in <code>checkSumAdjustment</code> and the bit 11 of its flags (set by the encoder), the same loca format and
     * the same glyphs; the glyph and location tables are copied byte for byte when they were not transformed.
     * <p>
     * 풀린 글꼴이 원본과 같은지 검사한다: 같은 순서의 같은 테이블, <code>glyf</code> 와 <code>loca</code> 를 뺀 모든 테이블의 같은 바이트, <code>checkSumAdjustment</code> 와 flags 의
     * 11 번 비트(인코더가 켬)만 다른 head 테이블, 같은 loca 형식과 같은 글리프. 변환하지 않았으면 글리프와 위치 테이블은 바이트 그대로 복사된다.
     *
     * @param original      the original font / 원본 글꼴
     * @param decoded       the decoded font / 풀린 글꼴
     * @param nullTransform true when <code>glyf</code> and <code>loca</code> were not transformed / <code>glyf</code> 와 <code>loca</code> 를 변환하지 않았으면 true
     * @param what          the description / 설명
     */
    private static void sameFont(byte[] original, byte[] decoded, boolean nullTransform, String what) {
        sameFont(original, decoded, nullTransform, false, what);
    }

    /**
     * Checks that a decoded font equals the original font like {@link #sameFont(byte[], byte[], boolean, String)}, and optionally ignores the
     * <code>created</code> and <code>modified</code> time stamps of the head table (an encoder that reads and writes the head table may convert them).
     * <p>
     * {@link #sameFont(byte[], byte[], boolean, String)} 처럼 풀린 글꼴이 원본과 같은지 검사하되, 선택하면 head 테이블의 <code>created</code> 와 <code>modified</code> 시각을
     * 무시한다(head 테이블을 읽고 다시 쓰는 인코더는 그것을 바꿀 수 있다).
     *
     * @param original      the original font / 원본 글꼴
     * @param decoded       the decoded font / 풀린 글꼴
     * @param nullTransform true when <code>glyf</code> and <code>loca</code> were not transformed / <code>glyf</code> 와 <code>loca</code> 를 변환하지 않았으면 true
     * @param ignoreTimes   true to ignore the time stamps of the head table / head 테이블의 시각을 무시하려면 true
     * @param what          the description / 설명
     */
    private static void sameFont(byte[] original, byte[] decoded, boolean nullTransform, boolean ignoreTimes, String what) {
        Map<String, byte[]> orig = tables(original, 0x00010000L);
        Map<String, byte[]> dec = tables(decoded, 0x00010000L);
        List<String> expectedTags = new ArrayList<String>(orig.keySet());
        // 규격은 인코더가 DSIG 를 지우라고 하므로 풀린 글꼴에는 없다.
        expectedTags.remove("DSIG");
        Check.equal(expectedTags, new ArrayList<String>(dec.keySet()), what + ": the tables");
        // 태그마다: head, glyf, loca 를 뺀 테이블은 바이트가 같아야 한다.
        for (String tag : expectedTags) {
            if ("head".equals(tag)) {
                byte[] a = orig.get(tag).clone();
                byte[] b = dec.get(tag).clone();
                Check.equal(a.length, b.length, what + ": the length of head");
                Check.equal((a[16] | 0x08) & 0xFF, b[16] & 0xFF, what + ": head flags bit 11 is set by the encoder");
                a[8] = a[9] = a[10] = a[11] = 0;
                b[8] = b[9] = b[10] = b[11] = 0;
                a[16] |= 0x08;
                // 시각을 무시하면 created(20~27)와 modified(28~35)를 양쪽 모두 0 으로 둔다.
                if (ignoreTimes) {
                    Arrays.fill(a, 20, 36, (byte) 0);
                    Arrays.fill(b, 20, 36, (byte) 0);
                }
                Check.equal(a, b, what + ": the head table apart from checkSumAdjustment and flags bit 11");
            } else if (!"glyf".equals(tag) && !"loca".equals(tag)) {
                Check.equal(orig.get(tag), dec.get(tag), what + ": the table " + tag);
            }
        }
        List<Woff2Builder.Glyph> a = glyphs(orig);
        List<Woff2Builder.Glyph> b = glyphs(dec);
        Check.equal(a.size(), b.size(), what + ": the number of glyphs");
        Check.equal(u16(orig.get("head"), 50), u16(dec.get("head"), 50), what + ": the loca format");
        // 글리프마다 점, 플래그, 명령, 합성 기록, 상자를 비교한다.
        for (int g = 0; g < a.size(); g++) {
            sameGlyph(a.get(g), b.get(g), what + " glyph " + g);
        }
        // 변환하지 않은 파일은 글리프와 위치 테이블을 그대로 복사해야 한다.
        if (nullTransform) {
            Check.equal(orig.get("glyf"), dec.get("glyf"), what + ": glyf without transform is copied");
            Check.equal(orig.get("loca"), dec.get("loca"), what + ": loca without transform is copied");
        }
    }

    /**
     * Compares the outlines and advances of the characters of two fonts through Java2D (<code>Font.createFont</code>).
     * <p>
     * 두 글꼴의 글자의 윤곽선과 진행 폭을 Java2D(<code>Font.createFont</code>)로 비교한다.
     *
     * @param original the original font / 원본 글꼴
     * @param decoded  the decoded font / 풀린 글꼴
     * @param what     the description / 설명
     * @throws Exception on failure / 실패 시
     */
    private static void compareOutlines(byte[] original, byte[] decoded, String what) throws Exception {
        Font a = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(original)).deriveFont(64f);
        Font b = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(decoded)).deriveFont(64f);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        int[][] ranges = { { 0x20, 0x17F }, { 0x370, 0x3FF }, { 0x400, 0x45F }, { 0x2013, 0x2022 }, { 0x20AC, 0x20AC } };
        int compared = 0;
        // 여러 범위의 글자마다 윤곽선과 진행 폭을 비교한다.
        for (int r = 0; r < ranges.length; r++) {
            for (int cp = ranges[r][0]; cp <= ranges[r][1]; cp++) {
                if (!a.canDisplay(cp)) {
                    Check.isFalse(b.canDisplay(cp), what + ": U+" + Integer.toHexString(cp) + " is not displayable in both");
                    continue;
                }
                Check.isTrue(b.canDisplay(cp), what + ": U+" + Integer.toHexString(cp) + " is displayable in the decoded font");
                String s = new String(Character.toChars(cp));
                GlyphVector ga = a.createGlyphVector(frc, s);
                GlyphVector gb = b.createGlyphVector(frc, s);
                Check.equal(ga.getGlyphCode(0), gb.getGlyphCode(0), what + ": the glyph code of U+" + Integer.toHexString(cp));
                Check.near(ga.getGlyphMetrics(0).getAdvance(), gb.getGlyphMetrics(0).getAdvance(), 0.0, what + ": the advance of U+" + Integer.toHexString(cp));
                sameShape(ga.getGlyphOutline(0), gb.getGlyphOutline(0), what + ": the outline of U+" + Integer.toHexString(cp));
                compared++;
            }
        }
        Check.isTrue(compared > 150, what + ": enough characters were compared: " + compared);
    }

    /**
     * Checks that two shapes have the same path segments.
     * <p>
     * 두 도형의 경로 선분이 같은지 검사한다.
     *
     * @param a    the first shape / 첫째 도형
     * @param b    the second shape / 둘째 도형
     * @param what the description / 설명
     */
    private static void sameShape(java.awt.Shape a, java.awt.Shape b, String what) {
        PathIterator pa = a.getPathIterator(null);
        PathIterator pb = b.getPathIterator(null);
        float[] ca = new float[6];
        float[] cb = new float[6];
        // 두 경로를 선분 단위로 같이 걷는다.
        while (!pa.isDone() && !pb.isDone()) {
            int ta = pa.currentSegment(ca);
            int tb = pb.currentSegment(cb);
            Check.equal(ta, tb, what + ": the segment type");
            Check.isTrue(Arrays.equals(ca, cb), what + ": the segment coordinates");
            pa.next();
            pb.next();
        }
        Check.isTrue(pa.isDone() && pb.isDone(), what + ": the number of segments");
    }

    // ------------------------------------------------------------------------------------------------------ synthetic fonts

    /**
     * Every one of the 128 triplet rows decodes: the point of a glyph is written with the row (forced) at its smallest, a middle and its largest value, with the
     * second point returning to the origin, and every glyph of the decoded font equals the model.
     * <p>
     * 128 개 삼중 행이 모두 풀린다: 글리프의 점을 행(강제)으로 가장 작은 값, 중간 값, 가장 큰 값으로 쓰고 두 번째 점이 원점으로 돌아오게 하며, 풀린 글꼴의 모든 글리프가 모델과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAllTripletRowsAreDecoded() throws Exception {
        Woff2Builder b = new Woff2Builder();
        b.glyphs.add(Woff2Builder.Glyph.empty());
        Set<Integer> rows = new HashSet<Integer>();
        // 행마다 세 값(가장 작은 값, 가장 큰 값, 중간 값)으로 글리프를 만든다.
        for (int row = 0; row < 128; row++) {
            Woff2Builder.Row w = Woff2Builder.ROWS[row];
            Check.equal(row, w.index, "the row index");
            // 행마다 가장 작은 값, 가장 큰 값, 중간 값 세 가지로 시험한다.
            for (int variant = 0; variant < 3; variant++) {
                int vx = w.xBits == 0 ? 0 : value(w.xBits, w.dxBase, variant);
                int vy = w.yBits == 0 ? 0 : value(w.yBits, w.dyBase, variant);
                int dx = w.xBits == 0 ? 0 : w.xSign * vx;
                int dy = w.yBits == 0 ? 0 : w.ySign * vy;
                Check.isTrue(Woff2Builder.rowFits(w, dx, dy), "the values fit row " + row);
                Woff2Builder.Glyph g = Woff2Builder.Glyph.simple(new int[] { 1 }, new int[] { dx, 0 }, new int[] { dy, 0 }, new boolean[] { variant != 1, true });
                g.forced = new int[] { row, -1 };
                b.glyphs.add(g);
                rows.add(row);
            }
        }
        Check.equal(128, rows.size(), "all 128 rows are used");
        Woff2Builder.Result r = b.build();
        byte[] sfnt = Woff2.toSfnt(r.file);
        checkModel(b, sfnt, "triplet rows");
        // 같은 모델을 변환 없이 담아도 같은 글리프가 나와야 한다(모델과 파서의 일치).
        b.transformGlyf = false;
        checkModel(b, Woff2.toSfnt(b.build().file), "plain glyf");
    }

    /**
     * Returns the smallest (0), the largest (1) or a middle (2) value of an axis, at most 32767 (a TrueType delta is a signed 16 bit number).
     * <p>
     * 축의 가장 작은 값(0), 가장 큰 값(1), 중간 값(2)을 반환한다. 최대 32767(TrueType 변화량은 부호 있는 16비트 수).
     *
     * @param bits    the number of bits of the axis / 축의 비트 수
     * @param base    the amount added / 더하는 양
     * @param variant 0, 1 or 2 / 0, 1, 2
     * @return the value / 값
     */
    private static int value(int bits, int base, int variant) {
        int max = base + (1 << bits) - 1;
        int v = variant == 0 ? base : (variant == 1 ? max : base + (1 << bits) / 2);
        return Math.min(v, 32767);
    }

    /**
     * Simple and composite glyphs of every form decode like the model: with the long and the short <code>loca</code> format, with and without the transform.
     * <p>
     * 모든 형태의 단순·합성 글리프가 모델대로 풀린다: 긴/짧은 <code>loca</code> 형식, 변환이 있을 때와 없을 때.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSimpleAndCompositeGlyphs() throws Exception {
        // 두 loca 형식(짧은 형식과 긴 형식)을 모두 시험한다.
        for (int format = 0; format <= 1; format++) {
            Woff2Builder b = richFont(format);
            checkModel(b, Woff2.toSfnt(b.build().file), "format " + format);
            b.transformGlyf = false;
            checkModel(b, Woff2.toSfnt(b.build().file), "format " + format + " without transform");
        }
        Woff2Builder b = richFont(1);
        byte[] sfnt = Woff2.toSfnt(b.build().file);
        Map<String, byte[]> t = tables(sfnt, 0x00010000L);
        Check.equal(4 * (b.glyphs.size() + 1), t.get("loca").length, "the long loca");
        // 글리프 데이터는 모두 4 바이트 경계에서 시작한다.
        for (int i = 0; i <= b.glyphs.size(); i++) {
            Check.equal(0, u32(t.get("loca"), 4 * i) % 4, "glyph " + i + " starts on a 4-byte boundary");
        }
        Check.equal(0, t.get("glyf").length % 4, "the glyph table is padded to 4 bytes");
    }

    /**
     * The short <code>loca</code> format cannot address more than 131070 bytes of glyphs: the file is rejected; with the long format the same glyphs decode.
     * <p>
     * 짧은 <code>loca</code> 형식은 글리프 131070 바이트를 넘게 가리킬 수 없다: 파일은 거부된다. 긴 형식에서는 같은 글리프가 풀린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testShortLocaOverflow() throws Exception {
        Woff2Builder b = new Woff2Builder();
        b.glyphs.add(Woff2Builder.Glyph.empty());
        Random rnd = new Random(7);
        // 점이 많은 글리프를 충분히 만들어 글리프 테이블이 131070 바이트를 넘게 한다.
        for (int g = 0; g < 20; g++) {
            int n = 4000;
            int[] xs = new int[n];
            int[] ys = new int[n];
            boolean[] on = new boolean[n];
            int x = 0;
            int y = 0;
            // 점마다 무작위 걸음으로 좌표를 옮긴다(범위를 벗어나지 않게 방향을 돌린다).
            for (int i = 0; i < n; i++) {
                x += (x > 8000 ? -1 : (x < -8000 ? 1 : (rnd.nextBoolean() ? 1 : -1))) * (1 + rnd.nextInt(255));
                y += (y > 8000 ? -1 : (y < -8000 ? 1 : (rnd.nextBoolean() ? 1 : -1))) * (1 + rnd.nextInt(255));
                xs[i] = x;
                ys[i] = y;
                on[i] = rnd.nextBoolean();
            }
            b.glyphs.add(Woff2Builder.Glyph.simple(new int[] { n - 1 }, xs, ys, on));
        }
        b.indexFormat = 1;
        byte[] sfnt = Woff2.toSfnt(b.build().file);
        Check.isTrue(tables(sfnt, 0x00010000L).get("glyf").length > 131070, "the glyph table is larger than the short format can address");
        checkModel(b, sfnt, "many points");
        b.indexFormat = 0;
        rejected(b.build().file, "short loca", "the glyph table is too large for the short loca format");
    }

    /**
     * The <code>hmtx</code> transform: the left side bearings that the flags leave out come from the glyph boxes (flags 1, 2 and 3, and fewer long metrics than glyphs);
     * a table without the transform is copied.
     * <p>
     * <code>hmtx</code> 변환: 플래그가 생략한 왼쪽 여백은 글리프 상자에서 온다(플래그 1, 2, 3 그리고 글리프보다 적은 긴 지표). 변환 없는 테이블은 복사한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHmtxTransform() throws Exception {
        int[] metrics = { -1, 5 };
        // 긴 지표 수가 전부일 때와 일부일 때, 플래그 세 가지(와 변환 없음)를 시험한다.
        for (int m = 0; m < metrics.length; m++) {
            for (int flags = 0; flags <= 3; flags++) {
                Woff2Builder b = richFont(1);
                b.numMetrics = metrics[m];
                b.hmtxFlags = flags;
                Woff2Builder.Result r = b.build();
                byte[] sfnt = Woff2.toSfnt(r.file);
                Map<String, byte[]> t = tables(sfnt, 0x00010000L);
                int n = b.glyphs.size();
                int nm = metrics[m] < 0 ? n : metrics[m];
                byte[] hmtx = t.get("hmtx");
                Check.equal(4 * nm + 2 * (n - nm), hmtx.length, "the length of hmtx, flags " + flags);
                int p = 0;
                // 지표를 읽어 진행 폭(500 + 인덱스)과 왼쪽 여백(상자의 xMin)을 확인한다.
                for (int i = 0; i < n; i++) {
                    // (글리프마다 지표 하나, 긴 지표가 있는 글리프는 진행 폭도 읽는다)
                    if (i < nm) {
                        Check.equal(500 + i, u16(hmtx, p), "advance " + i + ", flags " + flags);
                        p += 2;
                    }
                    Check.equal(b.glyphs.get(i).effectiveBox()[0], i16(hmtx, p), "left side bearing " + i + ", flags " + flags);
                    p += 2;
                }
                if (flags != 0) {
                    byte[] stream = r.stream;
                    int expectedLength = 1 + 2 * nm + ((flags & 1) == 0 ? 2 * nm : 0) + ((flags & 2) == 0 ? 2 * (n - nm) : 0);
                    Check.equal(expectedLength, r.lengths.get("hmtx").intValue(), "the length of the transformed hmtx, flags " + flags);
                    Check.equal(flags, stream[r.offsets.get("hmtx")] & 0xFF, "the flags byte of the transformed hmtx");
                }
            }
        }
    }

    /**
     * Tables that are not transformed are copied as they are, in any order of the directory; known tags may be written with an explicit tag (flag value 63), tags that
     * are not known are written with it; the font is an sfnt with sorted tables, right checksums and the <code>OTTO</code> and <code>true</code> flavors work.
     * <p>
     * 변환하지 않은 테이블은 디렉터리 순서와 상관없이 그대로 복사한다. 알려진 태그를 명시한 태그(플래그 값 63)로 써도 되고 알려지지 않은 태그는 그렇게 쓴다. 글꼴은 정렬된 테이블과 맞는 체크섬을 가진
     * sfnt 이며 <code>OTTO</code>, <code>true</code> flavor 도 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOtherTablesAndFlavors() throws Exception {
        Woff2Builder b = richFont(1);
        byte[] cff = new byte[37];
        // 알려진 태그(cmap, CFF )와 알려지지 않은 태그(Test)의 테이블을 더하고 cmap 은 명시한 태그로 쓴다.
        for (int i = 0; i < cff.length; i++) {
            cff[i] = (byte) (i * 11 + 3);
        }
        b.extraTags.add("cmap");
        b.extraData.add(cff);
        b.extraTags.add("Test");
        b.extraData.add(new byte[] { 1, 2, 3 });
        b.extraTags.add("zzzz");
        b.extraData.add(new byte[0]);
        b.explicitTags.add("cmap");
        b.explicitTags.add("head");
        long[] flavors = { 0x00010000L, 0x74727565L, 0x4F54544FL };
        // 세 flavor 를 시험한다.
        for (int i = 0; i < flavors.length; i++) {
            b.flavor = flavors[i];
            Woff2Builder.Result r = b.build();
            byte[] sfnt = Woff2.toSfnt(r.file);
            Map<String, byte[]> t = tables(sfnt, flavors[i]);
            Check.equal(cff, t.get("cmap"), "the cmap table");
            Check.equal(new byte[] { 1, 2, 3 }, t.get("Test"), "the table with an unknown tag");
            Check.equal(0, t.get("zzzz").length, "an empty table");
            Check.equal(9, t.size(), "the number of tables");
        }
        // 알려진 태그 코드(name)로 쓴 추가 테이블이 있어도 글리프는 같다.
        Woff2Builder c = richFont(1);
        c.extraTags.add("name");
        c.extraData.add(new byte[8]);
        checkModel(c, Woff2.toSfnt(c.build().file), "extra table");
    }

    // ------------------------------------------------------------------------------------------------------ invalid files

    /**
     * The checks of the header and of the file layout of section 3: signature, length field, table count, collections, extraneous data, ranges.
     * <p>
     * 3 절의 헤더와 파일 배치 검사: 서명, 길이 필드, 테이블 수, 컬렉션, 불필요한 데이터, 범위.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeaderAndLayoutRules() throws Exception {
        Woff2Builder b = richFont(1);
        byte[] ok = b.build().file;
        Woff2.toSfnt(ok);
        byte[] f = ok.clone();
        f[3] = '3';
        rejected(f, "not a WOFF2", "a wrong signature");
        rejected(new byte[10], "not a WOFF2", "a short file");
        rejected(Arrays.copyOf(ok, 47), "not a WOFF2", "a file shorter than the header");
        f = ok.clone();
        Woff2Builder.put32(f, 8, ok.length + 4);
        rejected(f, "length field", "a length field that is larger than the file");
        f = ok.clone();
        Woff2Builder.put32(f, 8, ok.length - 4);
        rejected(f, "length field", "a length field that is smaller than the file");
        f = ok.clone();
        Woff2Builder.put16(f, 12, 0);
        rejected(f, "table count", "no tables");
        f = ok.clone();
        Woff2Builder.put16(f, 12, 5000);
        rejected(f, "table count", "too many tables");
        f = ok.clone();
        Woff2Builder.put32(f, 4, 0x74746366L);
        rejected(f, "collections", "a font collection");
        f = ok.clone();
        Woff2Builder.put32(f, 4, 0x12345678L);
        rejected(f, "flavor", "an unknown flavor");
        f = ok.clone();
        Woff2Builder.put32(f, 20, 0);
        rejected(f, "compressed data", "no compressed data");
        f = ok.clone();
        Woff2Builder.put32(f, 20, ok.length);
        rejected(f, "compressed data", "compressed data beyond the file");
        // 파일 끝의 불필요한 데이터: 길이 필드를 맞춰도 거부한다. 패딩은 0 이어야 한다.
        f = Arrays.copyOf(ok, ok.length + 4);
        Woff2Builder.put32(f, 8, f.length);
        rejected(f, "extraneous", "four extra bytes after the last block");
        // 마지막 블록 뒤에 패딩이 있으면 그 바이트를 0 이 아닌 값으로 바꿔 거부되는지 본다(패딩이 없는 파일이면 건너뛴다).
        Woff2Builder.Result r = b.build();
        int end = 48 + r.dir.length + Woff2Builder.stored(r.stream).length;
        if (ok.length > end) {
            f = ok.clone();
            f[ok.length - 1] = 1;
            rejected(f, "extraneous", "a padding byte that is not zero");
        }
    }

    /**
     * The extended metadata and the private data blocks must follow the compressed data at 4-byte boundaries with zero padding; they are not interpreted.
     * <p>
     * 확장 메타데이터와 비공개 데이터 블록은 압축 데이터 뒤에 0 패딩을 두고 4 바이트 경계에서 이어져야 한다. 해석하지는 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMetadataAndPrivateDataLayout() throws Exception {
        Woff2Builder b = richFont(1);
        Woff2Builder.Result r = b.build();
        byte[] xml = "<metadata version=\"1.0\"/>".getBytes("UTF-8");
        byte[] meta = Woff2Builder.stored(xml);
        byte[] priv = { 9, 8, 7, 6, 5 };
        byte[] both = Woff2Builder.assemble(b.flavor, r.numTables, r.dir, Woff2Builder.stored(r.stream), meta, priv);
        byte[] plain = Woff2.toSfnt(r.file);
        Check.equal(plain, Woff2.toSfnt(both), "metadata and private data do not change the font");
        Check.equal(plain, Woff2.toSfnt(Woff2Builder.assemble(b.flavor, r.numTables, r.dir, Woff2Builder.stored(r.stream), null, priv)), "private data without metadata");
        Check.equal(plain, Woff2.toSfnt(Woff2Builder.assemble(b.flavor, r.numTables, r.dir, Woff2Builder.stored(r.stream), meta, null)), "metadata without private data");
        byte[] f = both.clone();
        Woff2Builder.put32(f, 28, u32(both, 28) + 4);
        rejected(f, null, "a metadata block that does not follow the compressed data");
        f = both.clone();
        Woff2Builder.put32(f, 28, u32(both, 28) + 1);
        rejected(f, null, "a metadata block that is not aligned");
        f = both.clone();
        Woff2Builder.put32(f, 40, u32(both, 40) + 4);
        rejected(f, null, "a private data block that does not follow the metadata");
        f = both.clone();
        Woff2Builder.put32(f, 32, 100000);
        rejected(f, null, "a metadata block beyond the file");
        f = both.clone();
        Woff2Builder.put32(f, 44, 100000);
        rejected(f, null, "a private data block beyond the file");
        f = both.clone();
        Woff2Builder.put32(f, 32, 0);
        rejected(f, null, "a metadata offset without a length");
        // 압축 블록 뒤 패딩을 0 이 아닌 값으로 바꾸면 거부한다(패딩이 있을 때).
        int compressedEnd = 48 + r.dir.length + Woff2Builder.stored(r.stream).length;
        int metaAt = (int) u32(both, 28);
        if (metaAt > compressedEnd) {
            f = both.clone();
            f[compressedEnd] = 1;
            rejected(f, "padding", "padding between the blocks that is not zero");
        }
    }

    /**
     * The table directory of section 4.1: <code>UIntBase128</code> with a leading zero, longer than five bytes or above 2^32-1, unknown transform versions, the
     * <code>loca</code> table of a transformed font, duplicates, and the glyf/loca pairing.
     * <p>
     * 4.1 절의 테이블 디렉터리: 앞이 0 인 <code>UIntBase128</code>, 다섯 바이트보다 길거나 2^32-1 보다 큰 것, 모르는 변환 버전, 변환된 글꼴의 <code>loca</code> 테이블, 중복, glyf/loca 짝.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTableDirectoryRules() throws Exception {
        Woff2Builder b = richFont(1);
        Woff2Builder.Result r = b.build();
        // head 의 origLength(54)를 앞이 0 인 형태로 다시 쓴다: 항목의 첫 바이트 뒤가 길이이다.
        byte[] dir = r.dir;
        byte[] zero = Woff2Builder.concat(new byte[] { dir[0], (byte) 0x80, 0x36 }, Arrays.copyOfRange(dir, 2, dir.length));
        rejected(withDirectory(b, r, zero, r.numTables), "leading zero", "a UIntBase128 with a leading zero");
        byte[] six = Woff2Builder.concat(new byte[] { dir[0], (byte) 0x81, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x36 }, Arrays.copyOfRange(dir, 2, dir.length));
        rejected(withDirectory(b, r, six, r.numTables), "UIntBase128", "a UIntBase128 of six bytes");
        byte[] big = Woff2Builder.concat(new byte[] { dir[0], (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F }, Arrays.copyOfRange(dir, 2, dir.length));
        rejected(withDirectory(b, r, big, r.numTables), "UIntBase128", "a UIntBase128 above 2^32-1");
        byte[] dirEnd = { dir[0], (byte) 0x90 };
        rejected(withDirectory(b, r, dirEnd, r.numTables), null, "a directory that ends inside a number");
        byte[] v = dir.clone();
        v[0] |= 1 << 6;
        rejected(withDirectory(b, r, v, r.numTables), "version", "head with a transform version");
        // glyf 항목을 찾아 변환 버전을 1 과 2 로 바꾼다(항목: 플래그 한 바이트 + 길이들).
        int glyfEntry = entryOffset(r.dir, 4);
        for (int version = 1; version <= 2; version++) {
            byte[] g = r.dir.clone();
            g[glyfEntry] = (byte) ((g[glyfEntry] & 0x3F) | (version << 6));
            rejected(withDirectory(b, r, g, r.numTables), "version", "glyf with the transform version " + version);
        }
        // hmtx 는 버전 0 과 1 만 정의되어 있다.
        Woff2Builder h = richFont(1);
        h.hmtxFlags = 3;
        Woff2Builder.Result hr = h.build();
        int hmtxEntry = entryOffset(hr.dir, 3);
        // 정의되지 않은 변환 버전 2 와 3 을 시험한다.
        for (int version = 2; version <= 3; version++) {
            byte[] g = hr.dir.clone();
            g[hmtxEntry] = (byte) ((g[hmtxEntry] & 0x3F) | (version << 6));
            rejected(withDirectory(h, hr, g, hr.numTables), "version", "hmtx with the transform version " + version);
        }
        // loca 의 변환 길이는 항상 0 이어야 한다.
        int locaEntry = entryOffset(r.dir, 5);
        byte[] l = r.dir.clone();
        Check.equal(0, l[locaEntry + 2], "the loca transform length is zero");
        l[locaEntry + 2] = 1;
        rejected(withDirectory(b, r, l, r.numTables), null, "loca with a transform length");
        // loca 의 origLength 가 글리프 수와 맞지 않으면 거부한다.
        byte[] o = r.dir.clone();
        o[locaEntry + 1] = (byte) (o[locaEntry + 1] + 4);
        rejected(withDirectory(b, r, o, r.numTables), "loca length", "loca with a length that does not match the glyph count");
        // 같은 태그가 둘이면 거부한다.
        Woff2Builder d = richFont(1);
        d.extraTags.add("head");
        d.extraData.add(new byte[54]);
        rejected(d.build().file, "twice", "a table twice");
        // glyf 만 있고 loca 가 없으면, loca 가 glyf 앞에 있으면 거부한다.
        Woff2Builder n = richFont(1);
        n.omitLoca = true;
        rejected(n.build().file, "glyf and loca", "glyf without loca");
        Woff2Builder f = richFont(1);
        f.locaFirst = true;
        rejected(f.build().file, "follow glyf", "loca before glyf");
        // glyf 는 변환하고 loca 는 변환하지 않으면(버전 3) 거부한다.
        // loca 항목을 버전 3(변환 없음)으로 바꾸면 변환 길이 바이트도 없어진다.
        byte[] m = Woff2Builder.concat(Arrays.copyOf(r.dir, locaEntry + 2), Arrays.copyOfRange(r.dir, locaEntry + 3, r.dir.length));
        m[locaEntry] = (byte) ((m[locaEntry] & 0x3F) | (3 << 6));
        rejected(withDirectory(b, r, m, r.numTables), "both transformed", "a transformed glyf with a loca without transform");
    }

    /**
     * Returns the offset of a directory entry (the entries of the standard tables are 1 flag byte plus one or two length numbers of one byte or more).
     * <p>
     * 디렉터리 항목의 오프셋을 반환한다(표준 테이블 항목은 플래그 한 바이트와 길이 수 하나 또는 둘).
     *
     * @param dir   the directory / 디렉터리
     * @param index the index of the entry / 항목 인덱스
     * @return the offset / 오프셋
     */
    private static int entryOffset(byte[] dir, int index) {
        int p = 0;
        // 항목을 index 번째까지 건너뛴다: 플래그의 알려진 태그 코드와 변환 버전으로 길이 수의 개수를 안다.
        for (int i = 0; i < index; i++) {
            int flags = dir[p] & 0xFF;
            int code = flags & 0x3F;
            int version = flags >> 6;
            p++;
            if (code == 63) {
                p += 4;
            }
            p = skip128(dir, p);
            boolean transformed = (code == 10 || code == 11) ? version == 0 : (code == 3 && version == 1);
            if (transformed) {
                p = skip128(dir, p);
            }
        }
        return p;
    }

    /**
     * Skips a <code>UIntBase128</code>.
     * <p>
     * <code>UIntBase128</code> 하나를 건너뛴다.
     *
     * @param b the array / 배열
     * @param p the position / 위치
     * @return the position after it / 그 뒤의 위치
     */
    private static int skip128(byte[] b, int p) {
        // 높은 비트가 꺼진 바이트까지 건너뛴다.
        while ((b[p] & 0x80) != 0) {
            p++;
        }
        return p + 1;
    }

    /**
     * Builds a file with another directory.
     * <p>
     * 다른 디렉터리로 파일을 만든다.
     *
     * @param b         the builder / 빌더
     * @param r         the pieces / 조각
     * @param dir       the directory / 디렉터리
     * @param numTables the table count of the header / 헤더의 테이블 수
     * @return the file / 파일
     */
    private static byte[] withDirectory(Woff2Builder b, Woff2Builder.Result r, byte[] dir, int numTables) {
        return Woff2Builder.assemble(b.flavor, numTables, dir, Woff2Builder.stored(r.stream), null, null);
    }

    /**
     * The rules of the transformed glyph table of section 5.1 that a decoder must enforce: an empty glyph with a box, a composite glyph without a box, an invalid
     * contour count, a contour without points, stream sizes that exceed the table, an invalid index format, a point outside of the 16 bit range, cut-off streams.
     * <p>
     * 디코더가 지켜야 하는 5.1 절 변환된 글리프 테이블의 규칙: 상자가 있는 빈 글리프, 상자가 없는 합성 글리프, 잘못된 윤곽선 수, 점이 없는 윤곽선, 테이블을 넘는 스트림 크기, 잘못된 인덱스 형식,
     * 16비트 범위를 벗어난 점, 잘린 스트림.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGlyphTableRules() throws Exception {
        Woff2Builder b = richFont(1);
        Woff2Builder.Result r = b.build();
        int glyf = r.offsets.get("glyf");
        int bitmapAt = streamStart(r.stream, glyf, 5);
        byte[] s = r.stream.clone();
        s[bitmapAt] |= (byte) 0x80;
        rejected(b.withStream(r, s), "empty glyph", "an empty glyph with a bounding box");
        s = r.stream.clone();
        s[bitmapAt] &= (byte) ~0x02;
        rejected(b.withStream(r, s), "no bounding box", "a composite glyph without a bounding box");
        s = r.stream.clone();
        int contours = streamStart(r.stream, glyf, 0);
        s[contours] = (byte) 0xFF;
        s[contours + 1] = (byte) 0xFE;
        rejected(b.withStream(r, s), "contour count", "a contour count of -2");
        s = r.stream.clone();
        s[streamStart(r.stream, glyf, 1)] = 0;
        rejected(b.withStream(r, s), "without points", "a contour without points");
        s = r.stream.clone();
        s[glyf + 7] = 9;
        rejected(b.withStream(r, s), "index format", "an index format of 9");
        s = r.stream.clone();
        Woff2Builder.put32(s, glyf + 8 + 4 * 3, 0x7FFFFFFFL);
        rejected(b.withStream(r, s), "stream sizes", "a stream size larger than the table");
        s = r.stream.clone();
        Woff2Builder.put32(s, glyf + 8, 0xFFFFFFFFL);
        rejected(b.withStream(r, s), "stream sizes", "a stream size of 4 GiB");
        // 플래그 스트림이 모자라면(점보다 적은 바이트) 거부한다.
        s = r.stream.clone();
        int flagSize = (int) u32(r.stream, glyf + 8 + 4 * 2);
        Woff2Builder.put32(s, glyf + 8 + 4 * 2, flagSize - 3);
        rejected(b.withStream(r, s), null, "a flag stream that is too short");
        // 글리프 수를 늘리면 nContour 스트림이 모자라 거부한다.
        s = r.stream.clone();
        Woff2Builder.put16(s, glyf + 4, b.glyphs.size() + 50);
        rejected(b.withStream(r, s), null, "more glyphs than the streams hold");
        // 변환된 glyf 가 36 바이트 헤더보다 짧으면 거부한다.
        Woff2Builder.Glyph far = Woff2Builder.Glyph.simple(new int[] { 1 }, new int[] { 30000, 60000 }, new int[] { 0, 0 }, new boolean[] { true, true });
        Woff2Builder c = new Woff2Builder();
        c.glyphs.add(far);
        rejected(c.build().file, "16 bit range", "a point beyond the 16 bit range");
        Woff2Builder d = new Woff2Builder();
        d.glyphs.add(Woff2Builder.Glyph.simple(new int[] { 1 }, new int[] { -30000, -60000 }, new int[] { 0, 0 }, new boolean[] { true, true }));
        rejected(d.build().file, "16 bit range", "a point below the 16 bit range");
        // 삼중 데이터가 모자란 경우(글리프 스트림을 줄임)도 거부한다.
        s = r.stream.clone();
        int glyphSize = (int) u32(r.stream, glyf + 8 + 4 * 3);
        Woff2Builder.put32(s, glyf + 8 + 4 * 3, glyphSize / 2);
        rejected(b.withStream(r, s), null, "a glyph stream that is too short");
        // 변환된 hmtx 의 플래그가 올바르지 않으면 거부한다.
        Woff2Builder h = richFont(1);
        h.hmtxFlags = 3;
        Woff2Builder.Result hr = h.build();
        int hmtx = hr.offsets.get("hmtx");
        int[] badFlags = { 0, 4, 0x80, 0x07, 0xFC };
        // 올바르지 않은 플래그 값마다 거부되는지 본다.
        for (int i = 0; i < badFlags.length; i++) {
            byte[] t = hr.stream.clone();
            t[hmtx] = (byte) badFlags[i];
            rejected(h.withStream(hr, t), "hmtx", "hmtx transform flags " + badFlags[i]);
        }
        // glyf 없이 변환된 hmtx 는 거부한다.
        Woff2Builder x = richFont(1);
        x.transformGlyf = false;
        x.hmtxFlags = 3;
        rejected(x.build().file, "transformed glyf", "a transformed hmtx without a transformed glyf");
    }

    /**
     * Returns the start of a stream of the transformed glyph table: 0 nContour, 1 nPoints, 2 flag, 3 glyph, 4 composite, 5 bounding box, 6 instruction.
     * <p>
     * 변환된 글리프 테이블의 스트림 시작을 반환한다: 0 nContour, 1 nPoints, 2 flag, 3 glyph, 4 composite, 5 경계 상자, 6 명령.
     *
     * @param stream the table data / 테이블 데이터
     * @param glyf   the offset of the glyph table / 글리프 테이블의 오프셋
     * @param index  the index of the stream / 스트림 인덱스
     * @return the position / 위치
     */
    private static int streamStart(byte[] stream, int glyf, int index) {
        int p = glyf + 36;
        // 앞 스트림의 크기를 더한다.
        for (int i = 0; i < index; i++) {
            p += (int) u32(stream, glyf + 8 + 4 * i);
        }
        return p;
    }

    /**
     * The sum of the table lengths must equal the size of the decompressed data, and tables that are larger than the limit are rejected before anything is
     * decompressed.
     * <p>
     * 테이블 길이의 합은 풀린 데이터의 크기와 같아야 하고, 한도보다 큰 테이블은 아무것도 풀기 전에 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizesMustMatch() throws Exception {
        Woff2Builder b = richFont(1);
        Woff2Builder.Result r = b.build();
        rejected(b.withStream(r, Woff2Builder.concat(r.stream, new byte[] { 0 })), null, "one byte more than the tables hold");
        rejected(b.withStream(r, Arrays.copyOf(r.stream, r.stream.length - 1)), null, "one byte less than the tables hold");
        Woff2Builder big = richFont(1);
        big.extraTags.add("Test");
        big.extraData.add(new byte[10]);
        Woff2Builder.Result br = big.build();
        byte[] dir = Arrays.copyOf(br.dir, br.dir.length - 1);
        dir = Woff2Builder.concat(dir, Woff2Builder.base128(20L * 1024 * 1024));
        rejected(withDirectory(big, br, dir, br.numTables), "larger than the limit", "tables larger than the limit");
    }

    /**
     * A file that is cut at any length is rejected with an IOException (never another exception and never a hang).
     * <p>
     * 어떤 길이에서 잘라도 파일은 IOException 으로 거부된다(다른 예외도, 멈춤도 없다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedFilesAreRejected() throws Exception {
        Woff2Builder b = richFont(1);
        b.hmtxFlags = 3;
        byte[] file = b.build().file;
        // 마지막 바이트 직전까지 한 바이트씩 잘라 본다.
        for (int cut = 0; cut < file.length; cut++) {
            rejected(Arrays.copyOf(file, cut), null, "the file cut to " + cut + " bytes");
        }
        byte[] real = resource("synthetic-xf.woff2");
        // 참조 인코더로 압축한 파일은 몇몇 길이에서만 잘라 본다(압축 데이터 전체를 모두 시험하지 않는다).
        for (int cut = 0; cut < real.length; cut += real.length / 97 + 1) {
            rejected(Arrays.copyOf(real, cut), null, "the reference file cut to " + cut + " bytes");
        }
    }

    /**
     * Random changes of bytes in a file (the header, the directory and the table data) never produce another exception than an IOException (or a valid font) and
     * never make the decoder run out of memory or loop.
     * <p>
     * 파일(헤더, 디렉터리, 테이블 데이터)의 바이트를 무작위로 바꿔도 IOException(또는 올바른 글꼴) 말고 다른 예외가 나오지 않고 디코더가 메모리가 부족해지거나 되풀이하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRandomByteChangesNeverCrash() throws Exception {
        Woff2Builder b = richFont(1);
        b.hmtxFlags = 3;
        byte[] file = b.build().file;
        Random rnd = new Random(20261118L);
        int accepted = 0;
        // 한 번에 1~3 바이트를 무작위 값으로 바꿔 디코딩한다.
        for (int i = 0; i < 4000; i++) {
            byte[] f = file.clone();
            int changes = 1 + rnd.nextInt(3);
            // 정한 수만큼 무작위 위치의 바이트를 무작위 값으로 바꾼다.
            for (int k = 0; k < changes; k++) {
                f[rnd.nextInt(f.length)] = (byte) rnd.nextInt(256);
            }
            try {
                Woff2.toSfnt(f);
                accepted++;
            } catch (IOException e) {
                // 거부는 올바른 결과이다.
            }
        }
        Check.isTrue(accepted < 4000, "some of the damaged files are rejected: accepted " + accepted);
        // 모든 바이트를 하나씩 반전해 본다(구조 필드 전체).
        for (int i = 0; i < file.length; i++) {
            byte[] f = file.clone();
            f[i] ^= (byte) 0xFF;
            try {
                Woff2.toSfnt(f);
            } catch (IOException e) {
                // 거부는 올바른 결과이다.
            }
        }
    }

    /**
     * The decoder tells WOFF2 files from other font files by the signature; TrueType and WOFF 1.0 data is not WOFF2.
     * <p>
     * 디코더는 서명으로 WOFF2 파일을 다른 글꼴 파일과 구별한다. TrueType 과 WOFF 1.0 데이터는 WOFF2 가 아니다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSignatureDetection() throws Exception {
        Check.isTrue(Woff2.isWoff2(resource("synthetic-xf.woff2")), "a WOFF2 file");
        Check.isFalse(Woff2.isWoff2(null), "null");
        Check.isFalse(Woff2.isWoff2(new byte[] { 'w', 'O', 'F', 'F' }), "WOFF 1.0");
        Check.isFalse(Woff2.isWoff2(new byte[] { 0, 1, 0, 0 }), "TrueType");
        Check.isFalse(Woff2.isWoff2(new byte[2]), "too short");
        rejected(new byte[] { 'w', 'O', 'F', 'F', 0, 0, 0, 0 }, "not a WOFF2", "a WOFF 1.0 header");
    }
}
