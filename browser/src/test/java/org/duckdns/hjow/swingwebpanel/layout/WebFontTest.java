package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the web fonts (plan item P10-05, CSS Fonts Level 3 <code>@font-face</code>, WOFF 1.0): the WOFF decoder (round trip of a real
 * font, corrupt and unsupported files), the selection of a face (family, source order, <code>format()</code> hints, <code>local()</code>,
 * <code>unicode-range</code>, weight and style matching, pending and failed faces) and the use of the font by the layout (while the font
 * is pending the next family is used, afterwards the web font). The font files are the TrueType fonts that the JRE ships in
 * <code>lib/fonts</code>; a test is skipped (with that reason) if the JRE has no such file.
 * <p>
 * 웹 글꼴 시험(계획 P10-05, CSS Fonts Level 3 <code>@font-face</code>, WOFF 1.0): WOFF 디코더(실제 글꼴의 왕복, 손상되었거나 지원하지 않는 파일), 면의 선택(패밀리, 소스 순서,
 * <code>format()</code> 힌트, <code>local()</code>, <code>unicode-range</code>, 굵기와 스타일 맞춤, 대기 중이거나 실패한 면), 레이아웃의 글꼴 사용(글꼴이 대기 중인 동안은
 * 다음 패밀리, 그 뒤는 웹 글꼴). 글꼴 파일은 JRE 가 <code>lib/fonts</code> 에 담은 TrueType 글꼴이며 JRE 에 그런 파일이 없으면 시험은 그 이유로 건너뛴다.
 */
public class WebFontTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebFontTest() {
    }

    /**
     * Reads a TrueType font file that the JRE ships, or returns null if there is none.
     * <p>
     * JRE 가 담은 TrueType 글꼴 파일을 읽는다. 없으면 null.
     *
     * @param name the file name / 파일 이름
     * @return the bytes or null / 바이트 또는 null
     * @throws IOException if reading fails / 읽기가 실패하면
     */
    private static byte[] jreFont(String name) throws IOException {
        File f = new File(new File(System.getProperty("java.home"), "lib/fonts"), name);
        if (!f.isFile()) {
            return null;
        }
        InputStream in = new FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일을 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Parses a style sheet and returns its <code>@font-face</code> rules.
     * <p>
     * 스타일 시트를 해석해 그 <code>@font-face</code> 규칙을 반환한다.
     *
     * @param css the style sheet text / 스타일 시트 문자열
     * @return the rules / 규칙
     */
    private static List<FontFaceRule> rules(String css) {
        StyleSheet s = StyleSheet.parse(css, "http://t.test/dir/s.css", StyleSheet.ORIGIN_AUTHOR, null, new CssDiagnostics());
        List<FontFaceRule> out = new ArrayList<FontFaceRule>();
        // 규칙 중 글꼴 규칙만 모은다.
        for (CssRule r : s.getRules()) {
            if (r instanceof FontFaceRule) {
                out.add((FontFaceRule) r);
            }
        }
        return out;
    }

    /**
     * A loader with files that the test puts in a map: a missing entry is a pending request, a null value is a failure.
     * <p>
     * 시험이 맵에 넣은 파일을 주는 로더: 없는 항목은 진행 중인 요청이고 null 값은 실패이다.
     */
    private static final class MapLoader implements WebFontSet.Loader {
        /** The files by URL. URL 별 파일. */
        final Map<String, byte[]> files = new HashMap<String, byte[]>();
        /** The URLs that were asked for. 요청한 URL. */
        final List<String> asked = new ArrayList<String>();

        /**
         * Returns a file, null while it is not in the map.
         * <p>
         * 파일을 반환한다. 맵에 없는 동안은 null.
         *
         * @param absoluteUrl the URL / URL
         * @return the bytes or null / 바이트 또는 null
         * @throws IOException if the entry says that the request failed / 항목이 요청 실패를 뜻하면
         */
        public byte[] load(String absoluteUrl) throws IOException {
            asked.add(absoluteUrl);
            if (files.containsKey(absoluteUrl) && files.get(absoluteUrl) == null) {
                throw new IOException("404 " + absoluteUrl);
            }
            return files.get(absoluteUrl);
        }
    }

    /**
     * A real TrueType font survives the round trip through WOFF: the rebuilt file has the same tables and is read by the JRE as the same
     * font.
     * <p>
     * 실제 TrueType 글꼴은 WOFF 를 거치는 왕복에서 유지된다: 다시 만든 파일은 같은 테이블을 가지며 JRE 는 같은 글꼴로 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWoffRoundTripOfARealFont() throws Exception {
        byte[] ttf = jreFont("LucidaSansRegular.ttf");
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        byte[] woff = Woff.fromSfnt(ttf);
        Check.isTrue(Woff.isWoff(woff), "signature");
        Check.isTrue(woff.length < ttf.length, "the tables are compressed: " + woff.length + " < " + ttf.length);
        byte[] back = Woff.toSfnt(woff);
        Font a = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf));
        Font b = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(back));
        Check.equal(a.getFontName(), b.getFontName(), "the same font");
        Check.equal(a.getNumGlyphs(), b.getNumGlyphs(), "the same glyphs");
        Check.isTrue(Woff.isSfnt(back), "the rebuilt file is an sfnt");
    }

    /**
     * Corrupt, truncated, inconsistent and unsupported files are refused with an IOException (never an unchecked exception).
     * <p>
     * 손상되었거나 잘렸거나 일관되지 않거나 지원하지 않는 파일은 IOException 으로 거부한다(검사하지 않는 예외가 아니다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWoffRejectsBadFiles() throws Exception {
        byte[] ttf = jreFont("LucidaSansRegular.ttf");
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        byte[] woff = Woff.fromSfnt(ttf);
        byte[][] bad = new byte[6][];
        bad[0] = new byte[] { 'w', 'O', 'F', '2', 0, 0, 0, 0 };
        bad[1] = new byte[10];
        byte[] cut = new byte[woff.length - 10];
        System.arraycopy(woff, 0, cut, 0, cut.length);
        bad[2] = cut;
        byte[] lie = woff.clone();
        lie[8] ^= 1;
        bad[3] = lie;
        byte[] corrupt = woff.clone();
        // 첫 테이블 데이터의 중간을 망가뜨린다(압축된 테이블이면 풀 때 실패하거나 길이가 달라진다).
        for (int i = woff.length / 2; i < woff.length / 2 + 40; i++) {
            corrupt[i] = (byte) 0xFF;
        }
        bad[4] = corrupt;
        byte[] tooMany = woff.clone();
        tooMany[12] = (byte) 0xFF;
        bad[5] = tooMany;
        // 파일마다 IOException 으로 거부하는지 확인한다(손상 사례는 우연히 올바를 수도 있어 풀리면 sfnt 인지만 본다).
        for (int i = 0; i < bad.length; i++) {
            try {
                byte[] r = Woff.toSfnt(bad[i]);
                Check.isTrue(i == 4 && r.length > 0, "file " + i + " must not be accepted");
            } catch (IOException expected) {
                Check.isTrue(expected.getMessage() != null, "a message for file " + i);
            }
        }
        Check.isTrue(Woff.isWoff2(bad[0]), "the WOFF 2 signature is detected");
    }

    /**
     * Sources are tried in order: a <code>format()</code> hint that is not supported (embedded-opentype) skips the source without a request, a
     * source that fails (404, garbage bytes) falls through to the next one, a pending source keeps the face pending, and a loaded face
     * gives the font of the file at the requested size.
     * <p>
     * 소스는 차례로 시도한다: 지원하지 않는 <code>format()</code> 힌트(embedded-opentype)는 요청 없이 소스를 건너뛰고, 실패한 소스(404, 쓰레기 바이트)는 다음 소스로 넘어가며, 대기 중인
     * 소스는 면을 대기 상태로 두고, 읽힌 면은 요청한 크기의 파일 글꼴을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSourceOrderAndFormats() throws Exception {
        byte[] ttf = jreFont("LucidaSansRegular.ttf");
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        Font expected = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf));
        MapLoader loader = new MapLoader();
        WebFontSet set = new WebFontSet(loader);
        set.setRules(rules("@font-face{font-family:'My Web';src:url(a.eot) format('embedded-opentype'),url(missing.ttf),url(garbage.bin),url(good.woff) format('woff'),url(never.ttf)}"));
        Check.isTrue(set.hasFamily("my web"), "the family is declared (case-insensitive)");
        Check.isFalse(set.hasFamily("Arial"), "another family is not");
        Check.isNull(set.find("My Web", 400, false, "abc", 16), "pending: the first usable source has not arrived");
        Check.isTrue(set.isPending("My Web"), "the face is pending");
        Check.isFalse(loader.asked.contains("http://t.test/dir/a.eot"), "the embedded-opentype source was skipped without a request");
        Check.isTrue(loader.asked.contains("http://t.test/dir/missing.ttf"), "the first usable source was requested");
        loader.files.put("http://t.test/dir/missing.ttf", null);
        loader.files.put("http://t.test/dir/garbage.bin", "this is no font".getBytes("US-ASCII"));
        Check.isNull(set.find("My Web", 400, false, "abc", 16), "the next source is pending");
        loader.files.put("http://t.test/dir/good.woff", Woff.fromSfnt(ttf));
        Font f = set.find("My Web", 400, false, "abc", 16);
        Check.notNull(f, "loaded");
        Check.equal(expected.getFontName(), f.getFontName(), "the font of the file");
        Check.near(16, f.getSize2D(), 0.001, "the requested size");
        Check.isFalse(loader.asked.contains("http://t.test/dir/never.ttf"), "later sources are not requested");
        Check.isNull(set.find("Other", 400, false, "abc", 16), "not a web family");
    }

    /**
     * A <code>format('woff2')</code> source is requested and decoded (Brotli, transformed glyph table) and gives the same font as the original TrueType file, and a
     * damaged WOFF2 file counts as a failed source so that the next source is tried. The WOFF2 file is made at test time from a TrueType font of the JRE that runs the
     * test (no file made from those fonts is kept in the repository, see <code>org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts</code>); the test is skipped if the JRE has none.
     * <p>
     * <code>format('woff2')</code> 소스는 요청하고 디코딩하며(Brotli, 변환된 글리프 테이블) 원본 TrueType 파일과 같은 글꼴을 주고, 손상된 WOFF2 파일은 실패한 소스로 세어 다음 소스를
     * 시도한다. WOFF2 파일은 시험 때 시험을 실행하는 JRE 의 TrueType 글꼴로 만든다(그 글꼴로 만든 파일은 저장소에 두지 않는다. <code>org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts</code> 참고).
     * JRE 에 그런 글꼴이 없으면 시험을 건너뛴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWoff2SourceIsDecoded() throws Exception {
        byte[] original = jreFont("LucidaBrightDemiBold.ttf");
        // 그 글꼴이 없으면 JRE 의 다른 글꼴을 쓴다.
        if (original == null) {
            original = jreFont("LucidaSansRegular.ttf");
        }
        if (original == null) {
            Check.skip("the JRE has no lib/fonts/LucidaBrightDemiBold.ttf or LucidaSansRegular.ttf to convert to WOFF2");
            return;
        }
        byte[] woff2 = org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts.woff2(original, true);
        Check.isTrue(WebFontSet.formatSupported("woff2") && WebFontSet.formatSupported("WOFF2"), "the woff2 format hint is supported");
        byte[] sfnt = org.duckdns.hjow.swingwebpanel.media.font.Woff2.toSfnt(woff2);
        Font decoded = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(sfnt));
        MapLoader loader = new MapLoader();
        WebFontSet set = new WebFontSet(loader);
        set.setRules(rules("@font-face{font-family:'W2';src:url(a.woff2) format('woff2'),url(never.ttf)}"));
        Check.isNull(set.find("W2", 400, false, "abc", 16), "pending: the woff2 file has not arrived");
        Check.isTrue(loader.asked.contains("http://t.test/dir/a.woff2"), "the woff2 source was requested");
        loader.files.put("http://t.test/dir/a.woff2", woff2);
        Font f = set.find("W2", 400, false, "abc", 16);
        Check.notNull(f, "the woff2 face is loaded");
        Check.equal(decoded.getFontName(), f.getFontName(), "the font of the decoded file");
        Check.near(16, f.getSize2D(), 0.001, "the requested size");
        Check.isFalse(loader.asked.contains("http://t.test/dir/never.ttf"), "the later source was not requested");
        Check.equal(Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(original)).getFontName(), f.getFontName(), "the same font as the original file");
        // 손상된 WOFF2 는 실패한 소스로 세고 다음 소스를 시도한다.
        MapLoader second = new MapLoader();
        second.files.put("http://t.test/dir/bad.woff2", java.util.Arrays.copyOf(woff2, woff2.length / 2));
        second.files.put("http://t.test/dir/good.ttf", sfnt);
        WebFontSet set2 = new WebFontSet(second);
        set2.setRules(rules("@font-face{font-family:'Bad';src:url(bad.woff2) format('woff2'),url(good.ttf)}"));
        Font g = set2.find("Bad", 400, false, "abc", 16);
        Check.notNull(g, "the next source is used after the damaged woff2 file failed");
        Check.isTrue(second.asked.contains("http://t.test/dir/bad.woff2") && second.asked.contains("http://t.test/dir/good.ttf"), "both sources were requested");
        Check.equal(decoded.getFontName(), g.getFontName(), "the font of the fallback source");
    }

    /**
     * Weight and style matching (CSS Fonts 3 5.2), <code>unicode-range</code> and the synthetic styles: two faces of one family, the
     * bold request gets the heavy face, the light request the regular one, an italic request takes a synthetic italic, a text outside the
     * range does not use the face, and <code>local()</code> uses an installed family.
     * <p>
     * 굵기와 스타일 맞춤(CSS Fonts 3 5.2), <code>unicode-range</code>, 합성 스타일: 한 패밀리의 두 면에서 굵게 요청은 무거운 면을, 가벼운 요청은 보통 면을 얻고,
     * 이탤릭 요청은 합성 이탤릭을 받으며, 범위 밖의 텍스트는 그 면을 쓰지 않고, <code>local()</code> 은 설치된 패밀리를 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMatchingRangesAndLocal() throws Exception {
        byte[] regular = jreFont("LucidaSansRegular.ttf");
        byte[] bold = jreFont("LucidaSansDemiBold.ttf");
        if (regular == null || bold == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf and LucidaSansDemiBold.ttf");
            return;
        }
        Font fr = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(regular));
        Font fb = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bold));
        Check.isFalse(fr.getFontName().equals(fb.getFontName()), "two different fonts");
        MapLoader loader = new MapLoader();
        loader.files.put("http://t.test/dir/r.ttf", regular);
        loader.files.put("http://t.test/dir/b.ttf", bold);
        WebFontSet set = new WebFontSet(loader);
        set.setRules(rules("@font-face{font-family:W;font-weight:400;src:url(r.ttf)} @font-face{font-family:W;font-weight:700;src:url(b.ttf)}"
                + "@font-face{font-family:Caps;src:url(r.ttf);unicode-range:U+41-5A,U+4??}"
                + "@font-face{font-family:Loc;src:local('Dialog'),url(r.ttf)}"));
        Check.equal(fb.getFontName(), set.find("W", 700, false, "x", 20).getFontName(), "bold request: the heavy face");
        Check.equal(fr.getFontName(), set.find("W", 400, false, "x", 20).getFontName(), "regular request");
        Check.equal(fr.getFontName(), set.find("W", 300, false, "x", 20).getFontName(), "light request: the nearest lighter or heavier is the regular face");
        Check.equal(fb.getFontName(), set.find("W", 900, false, "x", 20).getFontName(), "very bold request: the heaviest face");
        Font it = set.find("W", 400, true, "x", 20);
        Check.isTrue((it.getStyle() & Font.ITALIC) != 0, "the italic request on faces without italic is synthesized");
        Check.notNull(set.find("Caps", 400, false, "ABC", 16), "text inside the range uses the face");
        Check.isNull(set.find("Caps", 400, false, "abc", 16), "text outside the range does not");
        Check.isNull(set.find("Caps", 400, false, "ABc", 16), "a segment that is partly outside does not (documented limit)");
        Font loc = set.find("Loc", 400, false, "x", 20);
        Check.notNull(loc, "local() uses an installed family");
        int[][] ranges = WebFontSet.parseRanges("U+41-5A, U+4??, U+26");
        Check.equal(3, ranges.length, "three ranges");
        Check.equal(0x41, ranges[0][0], "range 1 low");
        Check.equal(0x5A, ranges[0][1], "range 1 high");
        Check.equal(0x400, ranges[1][0], "wildcard low");
        Check.equal(0x4FF, ranges[1][1], "wildcard high");
        Check.equal(0x26, ranges[2][1], "single code point");
        Check.isNull(WebFontSet.parseRanges("bogus"), "an invalid range means all characters");
    }

    /**
     * The layout uses the web font: while the font is pending the text takes the next family (monospace), once it arrived the web
     * font; the text is measured with it. A family that no rule declares is not affected, and the web fonts of one layout do not reach
     * the next layout without them.
     * <p>
     * 레이아웃은 웹 글꼴을 쓴다: 글꼴이 대기 중인 동안 텍스트는 다음 패밀리(monospace)를 쓰고 도착한 뒤에는 웹 글꼴을 쓰며 텍스트는 그것으로 잰다. 어떤 규칙도 선언하지 않은
     * 패밀리는 영향이 없고, 한 레이아웃의 웹 글꼴은 웹 글꼴이 없는 다음 레이아웃에 닿지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLayoutUsesTheWebFont() throws Exception {
        byte[] ttf = jreFont("LucidaSansRegular.ttf");
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        Font expected = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf));
        String css = "@font-face{font-family:Web;src:url(w.ttf)} #a{font-family:Web,monospace;font-size:20px} #b{font-family:monospace;font-size:20px}";
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + LayoutTestSupport.RESET.replaceAll("</?style>", "") + css
                + "</style></head><body><p id=a>Hamburgefonstiv</p><p id=b>Hamburgefonstiv</p></body></html>", "http://t.test/dir/page.html", null, false);
        CssEngine engine = new CssEngine(doc, null, new MediaEnvironment(400, 300));
        engine.loadStyleSheets();
        List<FontFaceRule> faces = new ArrayList<FontFaceRule>();
        // 문서의 스타일 시트에서 글꼴 규칙을 모은다.
        for (StyleSheet s : engine.getStyleSheets()) {
            for (CssRule r : s.getRules()) {
                if (r instanceof FontFaceRule) {
                    faces.add((FontFaceRule) r);
                }
            }
        }
        Check.equal(1, faces.size(), "the page declares one font face");
        MapLoader loader = new MapLoader();
        WebFontSet set = new WebFontSet(loader);
        set.setRules(faces);
        LayoutResult pending = LayoutEngine.layout(doc, new CssStyleSource(engine), null, 400, 300, set);
        LayoutBox ta = textOf(pending, doc, "a");
        Check.isFalse(expected.getFontName().equals(ta.getFont().getFontName()), "pending: the next family is used: " + ta.getFont().getFontName());
        loader.files.put("http://t.test/dir/w.ttf", ttf);
        LayoutResult loaded = LayoutEngine.layout(doc, new CssStyleSource(engine), null, 400, 300, set);
        LayoutBox la = textOf(loaded, doc, "a");
        Check.equal(expected.getFontName(), la.getFont().getFontName(), "loaded: the web font");
        Check.near(20, la.getFont().getSize2D(), 0.001, "at the size of the style");
        Check.near(LayoutTestSupport.advance(la.getFont(), "Hamburgefonstiv"), la.getWidth(), 0.5, "the text is measured with the web font");
        Check.isFalse(la.getWidth() == ta.getWidth(), "the width changed with the font");
        LayoutBox lb = textOf(loaded, doc, "b");
        Check.isFalse(expected.getFontName().equals(lb.getFont().getFontName()), "a family that no rule declares is not affected");
        LayoutResult without = LayoutEngine.layout(doc, new CssStyleSource(engine), null, 400, 300);
        Check.isFalse(expected.getFontName().equals(textOf(without, doc, "a").getFont().getFontName()), "the next layout without web fonts does not see them");
    }

    /**
     * Returns the first text box of the element with an id.
     * <p>
     * id 를 가진 요소의 첫 텍스트 상자를 반환한다.
     *
     * @param r   the layout / 레이아웃
     * @param doc the document / 문서
     * @param id  the id / id
     * @return the text box / 텍스트 상자
     */
    private static LayoutBox textOf(LayoutResult r, Document doc, String id) {
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(r.getBoxes(doc.getElementById(id)).get(0), LayoutBox.Type.TEXT, t);
        Check.isFalse(t.isEmpty(), "text of #" + id);
        return t.get(0);
    }
}
