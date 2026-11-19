package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the glyph alternates of a web font (plan item P10-08-h, CSS Fonts Level 4 sections 6.8, 6.9 and 6.12, WD 12 November 2026): the names of
 * <code>font-variant-alternates</code> are looked up in the <code>@font-feature-values</code> of the family, the tags reach the <code>GSUB</code> table of
 * the font (single, alternate and extension lookups) through {@link GlyphSubstitution}, and the text is measured and painted with the alternate glyphs.
 * The font is {@link SyntheticGsubFont}: the advance of a glyph tells which one is used.
 * <p>
 * 웹 글꼴의 대체 글리프 시험(계획 P10-08-h, CSS Fonts Level 4 6.8, 6.9, 6.12 절): <code>font-variant-alternates</code> 의 이름을 그 패밀리의 <code>@font-feature-values</code> 에서
 * 찾고, 태그가 {@link GlyphSubstitution} 을 거쳐 글꼴의 <code>GSUB</code> 표(단일, 대체, 확장 조회)에 닿으며, 텍스트를 대체 글리프로 재고 그린다. 글꼴은 {@link SyntheticGsubFont}
 * 이며 글리프의 advance 로 어느 것을 썼는지 안다.
 */
public class FontFeatureValuesLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontFeatureValuesLayoutTest() {
    }

    /**
     * {@link GlyphSubstitution} maps the characters to the glyphs that the features select, and changes nothing without such a feature.
     * <p>
     * {@link GlyphSubstitution} 은 기능이 고르는 글리프로 문자를 대응시키고, 그런 기능이 없으면 아무것도 바꾸지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGlyphSubstitution() throws Exception {
        byte[] ttf = SyntheticGsubFont.build();
        Font plain = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf)).deriveFont(100f);
        Check.near(100, advance(plain, "A"), 0.5, "the plain A is 1000 units wide");
        Check.near(70, advance(plain, "B"), 0.5, "the plain B is 700 units wide");
        Check.near(30, advance(with(ttf, "ss01", 1), "A"), 0.5, "ss01: single substitution format 2 to glyph 2");
        Check.near(30, advance(with(ttf, "salt", 1), "A"), 0.5, "salt 1: the first alternate");
        Check.near(50, advance(with(ttf, "salt", 2), "A"), 0.5, "salt 2: the second alternate");
        Check.near(100, advance(with(ttf, "salt", 3), "A"), 0.5, "salt 3: no such alternate, the glyph stays");
        Check.near(20, advance(with(ttf, "ss02", 1), "B"), 0.5, "ss02: an extension lookup of a single substitution format 1");
        Check.near(30, advance(with(ttf, "swsh", 1), "A"), 0.5, "swsh shares the alternate lookup");
        Map<String, Integer> none = new TreeMap<String, Integer>();
        none.put("ss05", Integer.valueOf(1));
        Check.isTrue(GlyphSubstitution.apply(ttf, none) == null, "a feature that the font has not changes nothing");
        none.put("ss01", Integer.valueOf(0));
        Check.isTrue(GlyphSubstitution.apply(ttf, none) == null, "a feature turned off changes nothing");
        Check.isTrue(GlyphSubstitution.apply(new byte[] { 0, 1, 0, 0, 0, 1 }, none) == null, "a broken font is not read");
    }

    /**
     * Returns the font with one feature applied.
     * <p>
     * 기능 하나를 적용한 글꼴을 반환한다.
     *
     * @param ttf   the font / 글꼴
     * @param tag   the feature / 기능
     * @param value the value / 값
     * @return the font at 100px / 100px 글꼴
     * @throws Exception on failure / 실패 시
     */
    private static Font with(byte[] ttf, String tag, int value) throws Exception {
        Map<String, Integer> f = new TreeMap<String, Integer>();
        f.put(tag, Integer.valueOf(value));
        byte[] out = GlyphSubstitution.apply(ttf, f);
        return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(out != null ? out : ttf)).deriveFont(100f);
    }

    /**
     * Measures a text.
     * <p>
     * 텍스트를 잰다.
     *
     * @param f the font / 글꼴
     * @param s the text / 텍스트
     * @return the advance / advance
     */
    private static double advance(Font f, String s) {
        return LayoutTestSupport.advance(f, s);
    }

    /**
     * The layout of a page with <code>@font-face</code>, <code>@font-feature-values</code> and <code>font-variant-alternates</code>: every function value
     * selects its feature with the indexes of its name, <code>font-feature-settings</code> wins, an undefined name and another family do nothing.
     * <p>
     * <code>@font-face</code>, <code>@font-feature-values</code>, <code>font-variant-alternates</code> 가 있는 페이지의 배치: 함수 값마다 이름의 번호로 기능을 고르고,
     * <code>font-feature-settings</code> 가 이기며, 정의되지 않은 이름과 다른 패밀리는 아무것도 하지 않는다.
     */
    public void testLayoutWithFeatureValues() {
        String css = "@font-face{font-family:G;src:url(g.ttf)} @font-face{font-family:Other;src:url(g.ttf)}"
                + " @font-feature-values G { @styleset { one: 1; two: 2; } @stylistic { second: 2; first: 1; } @swash { s: 1; } @character-variant { cv: 3 2; } }"
                + " @font-feature-values G { font-display: optional; @stylistic { first: 2; } }"
                + " p{margin:0;font:100px G,monospace}";
        String body = "<p id=plain>A</p><p id=ss style='font-variant-alternates:styleset(one)'>A</p>"
                + "<p id=s2 style='font-variant-alternates:stylistic(second)'>A</p><p id=s1 style='font-variant-alternates:stylistic(first)'>A</p>"
                + "<p id=ffs style='font-feature-settings:\"salt\" 2'>A</p><p id=win style='font-variant-alternates:styleset(one);font-feature-settings:\"ss01\" 0'>A</p>"
                + "<p id=b style='font-variant-alternates:styleset(two)'>B</p><p id=undef style='font-variant-alternates:styleset(nothing)'>A</p>"
                + "<p id=other style='font-family:Other;font-variant-alternates:styleset(one)'>A</p><p id=sw style='font-variant-alternates:swash(s)'>A</p>"
                + "<p id=list style='font-variant-alternates:styleset(nothing, one)'>A</p>";
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + body + "</body></html>",
                "http://t.test/dir/page.html", null, false);
        CssEngine engine = new CssEngine(doc, null, new MediaEnvironment(800, 600));
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
        final byte[] ttf = SyntheticGsubFont.build();
        WebFontSet set = new WebFontSet(new WebFontSet.Loader() {
            /** Returns the synthetic font for every URL. / 모든 URL 에 합성 글꼴을 준다. */
            public byte[] load(String absoluteUrl) {
                return ttf;
            }
        });
        set.setRules(faces);
        LayoutResult r = LayoutEngine.layout(doc, new CssStyleSource(engine), null, 800, 600, set);
        Check.near(100, width(r, doc, "plain"), 0.5, "without alternates");
        Check.near(30, width(r, doc, "ss"), 0.5, "styleset(one) turns ss01 on");
        Check.near(50, width(r, doc, "s2"), 0.5, "stylistic(second) is salt 2");
        Check.near(50, width(r, doc, "s1"), 0.5, "a later @font-feature-values redefines first as 2");
        Check.near(50, width(r, doc, "ffs"), 0.5, "font-feature-settings alone");
        Check.near(100, width(r, doc, "win"), 0.5, "font-feature-settings wins over the alternates");
        Check.near(20, width(r, doc, "b"), 0.5, "styleset(two) turns ss02 on");
        Check.near(100, width(r, doc, "undef"), 0.5, "an undefined name does nothing");
        Check.near(100, width(r, doc, "other"), 0.5, "the names are defined for the family G only");
        Check.near(30, width(r, doc, "sw"), 0.5, "swash(s) is swsh 1");
        Check.near(30, width(r, doc, "list"), 0.5, "the defined names of a list still apply");
        Check.equal("optional", set.defaultDisplay("G"), "font-display of @font-feature-values is the default of the faces of G (4.9.1)");
        Check.equal("auto", set.defaultDisplay("Other"), "another family keeps auto");
    }

    /**
     * Returns the width of the text of an element.
     * <p>
     * 요소 텍스트의 너비를 반환한다.
     *
     * @param r   the layout / 레이아웃
     * @param doc the document / 문서
     * @param id  the id / id
     * @return the width / 너비
     */
    private static double width(LayoutResult r, Document doc, String id) {
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(r.getBoxes(doc.getElementById(id)).get(0), LayoutBox.Type.TEXT, t);
        Check.isFalse(t.isEmpty(), "text of #" + id);
        return t.get(0).getWidth();
    }
}
