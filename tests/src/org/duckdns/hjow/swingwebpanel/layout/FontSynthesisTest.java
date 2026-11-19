package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>font-synthesis-weight</code> and <code>font-synthesis-style</code> (plan item P10-08, CSS Fonts Level 4 section 2.8): when the font has no bold face
 * (or no italic face) the user agent makes one up, and <code>none</code> forbids it so that the regular face is used as it is. A web font is checked through the face
 * list (the faces of the JRE: <code>LucidaSansRegular.ttf</code>), an installed font through the resolver (a family without a real bold or italic face reports the
 * name of its plain face, see {@link FontResolver#restrict}), and the layout through the font of a text box.
 * <p>
 * <code>font-synthesis-weight</code> 와 <code>font-synthesis-style</code> 시험(계획 P10-08, CSS Fonts Level 4 2.8 절): 글꼴에 굵은 면(또는 기울인 면)이 없으면 사용자 에이전트가 만들어
 * 내는데 <code>none</code> 은 이를 금지하여 보통 면을 그대로 쓰게 한다. 웹 글꼴은 면 목록으로(JRE 의 <code>LucidaSansRegular.ttf</code>), 설치된 글꼴은 리졸버로(진짜 굵은 면이나
 * 기울인 면이 없는 패밀리는 보통 면의 이름을 보고한다. {@link FontResolver#restrict} 참조), 레이아웃은 텍스트 상자의 글꼴로 확인한다.
 */
public class FontSynthesisTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontSynthesisTest() {
    }

    /**
     * A loader that serves the files of a map.
     * <p>
     * 맵의 파일을 주는 로더.
     */
    private static final class MapLoader implements WebFontSet.Loader {
        /** The files by URL. URL 별 파일. */
        final Map<String, byte[]> files = new HashMap<String, byte[]>();

        /**
         * Returns a file.
         * <p>
         * 파일을 반환한다.
         *
         * @param absoluteUrl the URL / URL
         * @return the bytes / 바이트
         * @throws IOException never / 발생하지 않음
         */
        public byte[] load(String absoluteUrl) throws IOException {
            return files.get(absoluteUrl);
        }
    }

    /**
     * Reads a font file of the JRE or returns null.
     * <p>
     * JRE 의 글꼴 파일을 읽거나 null 을 반환한다.
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
     * A web font that has only a regular face: the bold and the italic request get synthesized styles, unless the permission is withdrawn.
     * <p>
     * 보통 면만 있는 웹 글꼴: 굵게와 기울임 요청은 합성 스타일을 받는다. 단 허락을 거두면 받지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWebFontFaces() throws Exception {
        byte[] regular = jreFont("LucidaSansRegular.ttf");
        if (regular == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        MapLoader loader = new MapLoader();
        loader.files.put("http://t.test/dir/r.ttf", regular);
        WebFontSet set = new WebFontSet(loader);
        set.setRules(rules("@font-face{font-family:W;font-weight:400;src:url(r.ttf)}"));
        Font plain = set.find("W", 400, false, "x", 20);
        Check.notNull(plain, "the regular request");
        Check.equal(0, plain.getStyle(), "the regular request is plain");
        Check.equal(Font.BOLD, set.find("W", 700, false, "x", 20).getStyle(), "the bold request is synthesized by default");
        Check.equal(Font.ITALIC, set.find("W", 400, true, "x", 20).getStyle(), "the italic request is synthesized by default");
        Check.equal(Font.BOLD, set.find("W", 700, false, "x", 20, true, false).getStyle(), "bold is made up when it is allowed");
        Check.equal(0, set.find("W", 700, false, "x", 20, false, true).getStyle(), "font-synthesis-weight: none uses the regular face as it is");
        Check.equal(0, set.find("W", 400, true, "x", 20, true, false).getStyle(), "font-synthesis-style: none uses the upright face as it is");
        Check.equal(Font.ITALIC, set.find("W", 400, true, "x", 20, false, true).getStyle(), "the weight switch does not touch the style");
        Check.equal(0, set.find("W", 700, true, "x", 20, false, false).getStyle(), "both switches off");
    }

    /**
     * An installed family: the style bits of a family without a real face for them are taken away when the synthesis is forbidden; a family that has the face keeps
     * them. The test finds such a family on the machine and is skipped if there is none.
     * <p>
     * 설치된 패밀리: 합성을 금지하면 진짜 면이 없는 패밀리의 스타일 비트는 빠지고, 면이 있는 패밀리는 그대로 둔다. 시험은 기계에서 그런 패밀리를 찾으며 없으면 건너뛴다.
     */
    public void testInstalledFamilies() {
        // 논리 글꼴은 진짜 굵은 면과 기울인 면으로 풀리므로 합성을 금지해도 비트가 그대로이다.
        Check.equal(Font.BOLD | Font.ITALIC, FontResolver.restrict(Font.DIALOG, Font.BOLD | Font.ITALIC, false, false), "a logical font has the real faces");
        Check.equal(Font.BOLD, FontResolver.restrict(Font.DIALOG, Font.BOLD, false, false), "bold of a logical font");
        // 이 기계에서 굵은 면이 진짜가 아닌 패밀리를 찾는다(보통 면의 이름을 그대로 보고하는 패밀리).
        String[] candidates = {"Lucida Console", "Impact", "Gulim", "Tahoma", "Courier"};
        String noBold = null;
        String noItalic = null;
        // 후보 중 굵은 면 또는 기울인 면이 없는 것을 찾는다.
        for (int i = 0; i < candidates.length; i++) {
            Font plain = new Font(candidates[i], Font.PLAIN, 12);
            if (!plain.getFamily().equalsIgnoreCase(candidates[i])) {
                continue;
            }
            if (noBold == null && plain.getFontName().equals(new Font(candidates[i], Font.BOLD, 12).getFontName())) {
                noBold = candidates[i];
            }
            if (noItalic == null && plain.getFontName().equals(new Font(candidates[i], Font.ITALIC, 12).getFontName())) {
                noItalic = candidates[i];
            }
        }
        if (noBold == null && noItalic == null) {
            Check.skip("no installed family without a real bold or italic face was found");
            return;
        }
        if (noBold != null) {
            Check.equal(0, FontResolver.restrict(noBold, Font.BOLD, false, true), noBold + ": no bold face, the synthesis is forbidden");
            Check.equal(Font.BOLD, FontResolver.restrict(noBold, Font.BOLD, true, true), noBold + ": allowed");
            Check.equal(Font.ITALIC, FontResolver.restrict(noBold, Font.BOLD | Font.ITALIC, false, true), noBold + ": the italic bit is not affected by the weight switch");
        }
        if (noItalic != null) {
            Check.equal(0, FontResolver.restrict(noItalic, Font.ITALIC, true, false), noItalic + ": no italic face, the synthesis is forbidden");
            Check.equal(Font.ITALIC, FontResolver.restrict(noItalic, Font.ITALIC, true, true), noItalic + ": allowed");
        }
    }

    /**
     * The layout: <code>font-synthesis: none</code> (and the longhands) makes the text box of a bold or italic text of a family without those faces use the plain font; the
     * default synthesizes. Tests with an installed family that lacks the faces and is skipped if the machine has none.
     * <p>
     * 레이아웃: <code>font-synthesis: none</code>(과 개별 속성)이면 굵은 면이나 기울인 면이 없는 패밀리의 굵은 글자나 기울인 글자의 텍스트 상자가 보통 글꼴을 쓰고, 기본은
     * 합성한다. 면이 없는 설치된 패밀리로 시험하며 기계에 없으면 건너뛴다.
     */
    public void testLayoutFont() {
        String family = null;
        String[] candidates = {"Lucida Console", "Impact", "Gulim"};
        // 굵은 면이 진짜가 아니고 기울인 면도 진짜가 아닌 패밀리를 찾는다.
        for (int i = 0; i < candidates.length && family == null; i++) {
            Font plain = new Font(candidates[i], Font.PLAIN, 12);
            if (plain.getFamily().equalsIgnoreCase(candidates[i]) && plain.getFontName().equals(new Font(candidates[i], Font.BOLD, 12).getFontName())
                    && plain.getFontName().equals(new Font(candidates[i], Font.ITALIC, 12).getFontName())) {
                family = candidates[i];
            }
        }
        if (family == null) {
            Check.skip("no installed family without bold and italic faces was found");
            return;
        }
        String base = "font-family:'" + family + "';font-weight:bold;font-style:italic;margin:0;";
        LayoutResult r = LayoutTestSupport.layout("<p id=a style=\"" + base + "\">x</p><p id=b style=\"" + base + "font-synthesis:none\">x</p>"
                + "<p id=c style=\"" + base + "font-synthesis:weight\">x</p><p id=d style=\"" + base + "font-synthesis-style:none\">x</p>"
                + "<p id=e style=\"" + base + "font-synthesis-weight:none\">x</p>", 600);
        Check.equal(Font.BOLD | Font.ITALIC, fontStyle(r, "a"), "the default makes up bold and italic");
        Check.equal(0, fontStyle(r, "b"), "font-synthesis: none uses the plain face as it is");
        Check.equal(Font.BOLD, fontStyle(r, "c"), "font-synthesis: weight forbids the italic but allows the bold");
        Check.equal(Font.BOLD, fontStyle(r, "d"), "font-synthesis-style: none forbids the italic");
        Check.equal(Font.ITALIC, fontStyle(r, "e"), "font-synthesis-weight: none forbids the bold");
    }

    /**
     * Returns the style bits of the font of the first text box of an element.
     * <p>
     * 요소의 첫 텍스트 상자의 글꼴 스타일 비트를 반환한다.
     *
     * @param r  the layout / 레이아웃
     * @param id the id of the element / 요소의 id
     * @return the style bits / 스타일 비트
     */
    private static int fontStyle(LayoutResult r, String id) {
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, id), LayoutBox.Type.TEXT, t);
        Check.isFalse(t.isEmpty(), "text of #" + id);
        return t.get(0).getFont().getStyle();
    }
}
