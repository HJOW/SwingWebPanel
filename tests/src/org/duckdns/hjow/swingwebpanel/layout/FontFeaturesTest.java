package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the font feature properties (plan item P10-08, CSS Fonts Level 4, https://www.w3.org/TR/css-fonts-4/): the <code>font-variant</code> shorthand with the
 * six <code>font-variant-*</code> longhands, the <code>font</code> shorthand that resets them, the feature tags that the properties and
 * <code>font-feature-settings</code> give with the precedence of the specification, and the effects that this engine reproduces itself (small and petite capitals,
 * superscript and subscript, full width forms, <code>font-synthesis</code>).
 * <p>
 * 글꼴 기능 속성 시험(계획 P10-08, CSS Fonts Level 4): 여섯 개의 <code>font-variant-*</code> 개별 속성이 있는 <code>font-variant</code> 단축 속성, 그것들을 초기화하는
 * <code>font</code> 단축 속성, 속성과 <code>font-feature-settings</code> 가 규격의 우선순위로 주는 기능 태그, 그리고 이 엔진이 직접 재현하는 효과(작은 대문자와 petite 대문자,
 * 위첨자와 아래첨자, 전각 형태, <code>font-synthesis</code>).
 */
public class FontFeaturesTest {

    /** The registry under test. / 시험 대상 등록부. */
    private final PropertyRegistry reg = PropertyRegistry.getInstance();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontFeaturesTest() {
    }

    /**
     * Parses a declaration and returns the text of one longhand, or the name of the result code.
     * <p>
     * 선언을 파싱해 개별 속성 하나의 문자열 또는 결과 코드 이름을 반환한다.
     *
     * @param name     the property / 속성
     * @param value    the value / 값
     * @param longhand the longhand to read / 읽을 개별 속성
     * @return the text, or UNKNOWN, UNSUPPORTED, INVALID, MISSING / 문자열 또는 결과 코드 이름
     */
    private String v(String name, String value, String longhand) {
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int r = reg.parse(name, value, out);
        if (r == PropertyRegistry.UNKNOWN) {
            return "UNKNOWN";
        }
        if (r == PropertyRegistry.UNSUPPORTED) {
            return "UNSUPPORTED";
        }
        if (r == PropertyRegistry.INVALID) {
            return "INVALID";
        }
        CssValue x = out.get(Integer.valueOf(reg.byName(longhand).getId()));
        return x == null ? "MISSING" : x.toCssText();
    }

    /**
     * Returns the features of the element <code>p</code> that has the style.
     * <p>
     * 스타일을 가진 요소 <code>p</code> 의 기능을 반환한다.
     *
     * @param style the style / 스타일
     * @return the features / 기능
     */
    private static FontFeatures features(String style) {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;" + style + "'>x</p>", 600);
        ComputedStyle s = LayoutTestSupport.box(r, "p").getStyle();
        return FontFeatures.of(s);
    }

    /**
     * The <code>font-variant</code> shorthand sets the six longhands: the keywords go to their own longhand in any order, an unused longhand gets its initial
     * value, <code>normal</code> resets everything, <code>none</code> turns the ligatures off, and a wrong keyword drops the declaration.
     * <p>
     * <code>font-variant</code> 단축 속성은 여섯 개별 속성을 정한다: 키워드는 순서와 상관없이 자기 개별 속성으로 가고, 쓰지 않은 개별 속성은 초기값이 되며,
     * <code>normal</code> 은 모두 초기화하고, <code>none</code> 은 합자를 끄고, 틀린 키워드는 선언을 버린다.
     */
    public void testShorthand() {
        String all = "small-caps common-ligatures tabular-nums full-width super historical-forms";
        Check.equal("small-caps", v("font-variant", all, "font-variant-caps"), "caps");
        Check.equal("common-ligatures", v("font-variant", all, "font-variant-ligatures"), "ligatures");
        Check.equal("tabular-nums", v("font-variant", all, "font-variant-numeric"), "numeric");
        Check.equal("full-width", v("font-variant", all, "font-variant-east-asian"), "east asian");
        Check.equal("super", v("font-variant", all, "font-variant-position"), "position");
        Check.equal("historical-forms", v("font-variant", all, "font-variant-alternates"), "alternates");
        Check.equal("super", v("font-variant", "super tabular-nums small-caps", "font-variant-position"), "any order");
        Check.equal("initial", v("font-variant", "small-caps", "font-variant-ligatures"), "an unused longhand is reset to its initial value");
        Check.equal("initial", v("font-variant", "normal", "font-variant-caps"), "normal resets the caps");
        Check.equal("initial", v("font-variant", "normal", "font-variant-numeric"), "normal resets the numerals");
        Check.equal("none", v("font-variant", "none", "font-variant-ligatures"), "none turns the ligatures off");
        Check.equal("initial", v("font-variant", "none", "font-variant-caps"), "none leaves the caps normal");
        Check.equal("common-ligatures no-contextual", v("font-variant", "common-ligatures no-contextual", "font-variant-ligatures"), "several keywords of one longhand");
        Check.equal("INVALID", v("font-variant", "small-caps wide", "font-variant-caps"), "an unknown keyword drops the declaration");
        Check.equal("INVALID", v("font-variant", "small-caps all-small-caps", "font-variant-caps"), "two values of one longhand are invalid");
        Check.equal("small-caps", v("font-variant-caps", "small-caps", "font-variant-caps"), "the longhand has its own name");
        Check.equal("stylistic(fancy) historical-forms", v("font-variant-alternates", "stylistic(fancy) historical-forms", "font-variant-alternates"), "alternates functions");
        Check.equal("styleset(a, b)", v("font-variant-alternates", "styleset(a, b)", "font-variant-alternates"), "a list inside a function");
        Check.equal("INVALID", v("font-variant-alternates", "stylistic(1)", "font-variant-alternates"), "the name must be an identifier");
    }

    /**
     * The <code>font</code> shorthand sets <code>font-variant-caps</code> and resets the other variant properties and the feature properties.
     * <p>
     * <code>font</code> 단축 속성은 <code>font-variant-caps</code> 를 정하고 나머지 변형 속성과 기능 속성을 초기화한다.
     */
    public void testFontShorthand() {
        Check.equal("small-caps", v("font", "italic small-caps bold 14px serif", "font-variant-caps"), "the caps of font");
        Check.equal("initial", v("font", "14px serif", "font-variant-ligatures"), "font resets the ligatures");
        Check.equal("initial", v("font", "14px serif", "font-variant-position"), "font resets the position");
        Check.equal("initial", v("font", "14px serif", "font-feature-settings"), "font resets the feature settings");
        Check.equal("INVALID", v("font", "all-small-caps 14px serif", "font-variant-caps"), "only normal and small-caps are allowed in font");
    }

    /**
     * The features of the properties: the keywords become tags, the unset features have the defaults, and an invalid tag in the settings is ignored.
     * <p>
     * 속성이 주는 기능: 키워드는 태그가 되고, 정해지지 않은 기능은 기본값이며, 설정의 올바르지 않은 태그는 무시한다.
     */
    public void testTags() {
        FontFeatures none = features("");
        Check.equal(1, none.value("liga"), "ligatures are on by default");
        Check.equal(0, none.value("smcp"), "small capitals are off by default");
        Check.equal("normal", none.caps(), "no caps variant");
        Check.equal("normal", none.position(), "no position variant");
        FontFeatures v = features("font-variant:small-caps tabular-nums no-common-ligatures discretionary-ligatures slashed-zero full-width sub");
        Check.equal(1, v.value("smcp"), "small-caps");
        Check.equal(1, v.value("tnum"), "tabular-nums");
        Check.equal(0, v.value("liga"), "no-common-ligatures turns liga off");
        Check.equal(0, v.value("clig"), "and clig");
        Check.equal(1, v.value("dlig"), "discretionary-ligatures");
        Check.equal(1, v.value("zero"), "slashed-zero");
        Check.equal(1, v.value("fwid"), "full-width");
        Check.equal(1, v.value("subs"), "sub");
        Check.equal("small-caps", v.caps(), "the caps variant");
        Check.equal("sub", v.position(), "the position variant");
        Check.isTrue(v.fullWidth(), "full width");
        Check.equal(0, features("font-variant-ligatures:none").value("calt"), "none turns the contextual alternates off too");
        Check.equal(0, features("font-kerning:none").value("kern"), "font-kerning: none turns kern off");
        Check.equal("all-small-caps", features("font-variant-caps:all-small-caps").caps(), "all-small-caps is smcp and c2sc");
        Check.equal("petite-caps", features("font-variant-caps:petite-caps").caps(), "petite-caps");
        Check.equal("all-petite-caps", features("font-variant-caps:all-petite-caps").caps(), "all-petite-caps");
        Check.equal("unicase", features("font-variant-caps:unicase").caps(), "unicase");
        Check.equal("titling-caps", features("font-variant-caps:titling-caps").caps(), "titling-caps");
        Check.equal("super", features("font-variant-position:super").position(), "super");
        Check.equal(1, features("font-variant-alternates:historical-forms").value("hist"), "historical-forms");
    }

    /**
     * <code>font-feature-settings</code> is applied after the properties and wins: a tag with a value, <code>on</code>, <code>off</code>, the default value 1, a later entry
     * that replaces an earlier one, and entries that are not four characters long.
     * <p>
     * <code>font-feature-settings</code> 는 속성 뒤에 적용되어 이긴다: 값이 있는 태그, <code>on</code>, <code>off</code>, 기본값 1, 앞 항목을 대체하는 뒤 항목, 네 글자가 아닌 항목.
     */
    public void testFeatureSettings() {
        Check.equal(1, features("font-feature-settings:\"smcp\"").value("smcp"), "a tag alone is on");
        Check.equal("small-caps", features("font-feature-settings:\"smcp\"").caps(), "smcp is small capitals");
        Check.equal("all-small-caps", features("font-feature-settings:\"smcp\",\"c2sc\"").caps(), "smcp and c2sc");
        Check.equal(3, features("font-feature-settings:\"salt\" 3").value("salt"), "a value");
        Check.equal(0, features("font-feature-settings:\"liga\" off").value("liga"), "off");
        Check.equal(1, features("font-feature-settings:\"liga\" on").value("liga"), "on");
        Check.equal(0, features("font-feature-settings:\"liga\" 0").value("liga"), "0 is off");
        Check.equal(2, features("font-feature-settings:\"ss01\" 1, \"ss01\" 2").value("ss01"), "a later entry replaces an earlier one");
        Check.equal("normal", features("font-variant-caps:small-caps;font-feature-settings:\"smcp\" 0").caps(), "the settings win over font-variant-caps");
        Check.equal("small-caps", features("font-variant-caps:normal;font-feature-settings:\"smcp\"").caps(), "and can turn a feature on");
        Check.equal("INVALID", v("font-feature-settings", "smcp", "font-feature-settings"), "a tag has to be a string");
        Check.equal("super", features("font-feature-settings:\"sups\"").position(), "sups");
        Check.isTrue(features("font-feature-settings:\"fwid\"").fullWidth(), "fwid");
        Check.equal(0, features("font-feature-settings:\"abcde\"").value("abcde"), "a tag of five characters is not stored");
    }

    /**
     * <code>font-synthesis</code> decides whether small capitals and the position variants may be made up; the engine has no such glyphs, so it makes all of them up
     * when allowed.
     * <p>
     * <code>font-synthesis</code> 는 작은 대문자와 위치 변형을 만들어 내도 되는지 정한다. 엔진에는 그런 글리프가 없으므로 허용되면 모두 만들어 낸다.
     */
    public void testSynthesisPermission() {
        LayoutResult r = LayoutTestSupport.layout("<p id=a>x</p><p id=b style='font-synthesis:none'>x</p><p id=c style='font-synthesis:weight style'>x</p>"
                + "<p id=d style='font-synthesis:small-caps'>x</p><p id=e style='font-synthesis:position small-caps'>x</p>", 600);
        Check.isTrue(FontFeatures.mayRealize(LayoutTestSupport.box(r, "a").getStyle(), "small-caps"), "the default allows all");
        Check.isFalse(FontFeatures.mayRealize(LayoutTestSupport.box(r, "b").getStyle(), "small-caps"), "none forbids small-caps");
        Check.isFalse(FontFeatures.mayRealize(LayoutTestSupport.box(r, "b").getStyle(), "position"), "none forbids position");
        Check.isFalse(FontFeatures.mayRealize(LayoutTestSupport.box(r, "c").getStyle(), "small-caps"), "weight and style do not allow small-caps");
        Check.isTrue(FontFeatures.mayRealize(LayoutTestSupport.box(r, "d").getStyle(), "small-caps"), "small-caps allows small-caps");
        Check.isFalse(FontFeatures.mayRealize(LayoutTestSupport.box(r, "d").getStyle(), "position"), "but not position");
        Check.isTrue(FontFeatures.mayRealize(LayoutTestSupport.box(r, "e").getStyle(), "position"), "position is allowed");
        Check.equal("normal", FontFeatures.of(null).caps(), "a missing style asks for nothing");
        Check.isTrue(Prop.FONT_VARIANT_CAPS > 0, "the caps property id");
    }

    /**
     * Returns the text fragments of the first line of the element <code>p</code>.
     * <p>
     * 요소 <code>p</code> 의 첫 줄의 텍스트 조각을 반환한다.
     *
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the text fragments / 텍스트 조각
     */
    private static List<LayoutBox> fragments(String style, String text) {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;" + style + "'>" + text + "</p>", 600);
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        // 첫 줄의 텍스트 조각을 모은다.
        for (LayoutBox c : LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0).getChildren()) {
            if (c.getType() == LayoutBox.Type.TEXT) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Returns the width of the first line.
     * <p>
     * 첫 줄의 너비를 반환한다.
     *
     * @param style the style / 스타일
     * @param text  the text / 텍스트
     * @return the width / 너비
     */
    private static double width(String style, String text) {
        double w = 0;
        // 조각의 너비를 더한다.
        for (LayoutBox f : fragments(style, text)) {
            w += f.getWidth();
        }
        return w;
    }

    /**
     * Returns the text of the first line as it is drawn.
     * <p>
     * 첫 줄의 텍스트를 그려지는 대로 반환한다.
     *
     * @param style the style / 스타일
     * @param text  the text / 텍스트
     * @return the text of all fragments / 모든 조각의 텍스트
     */
    private static String joined(String style, String text) {
        StringBuilder sb = new StringBuilder();
        // 조각의 텍스트를 이어 붙인다.
        for (LayoutBox b : fragments(style, text)) {
            sb.append(b.getText());
        }
        return sb.toString();
    }

    /**
     * Small capitals come from the property and from the settings alike; petite capitals are made up the same way; <code>font-synthesis: none</code> shows the
     * text as it is.
     * <p>
     * 작은 대문자는 속성에서도 설정에서도 나오고, petite 대문자도 같은 방식으로 만들어 내며, <code>font-synthesis: none</code> 이면 글자를 있는 그대로 보여 준다.
     */
    public void testCapsEffects() {
        double cw = LayoutTestSupport.charWidth();
        double small = cw + 3 * cw * 0.8 + cw;
        Check.near(small, width("font-variant:small-caps", "Abc d"), 0.7, "the shorthand gives small capitals");
        Check.near(small, width("font-variant-caps:small-caps", "Abc d"), 0.7, "the longhand");
        Check.near(small, width("font-feature-settings:\"smcp\"", "Abc d"), 0.7, "smcp in the settings");
        Check.near(small, width("font-variant-caps:petite-caps", "Abc d"), 0.7, "petite capitals are made up like small capitals");
        Check.near(4 * cw * 0.8 + cw, width("font-feature-settings:\"smcp\",\"c2sc\"", "ABC D"), 0.7, "smcp and c2sc shrink the capitals as well");
        Check.near(5 * cw, width("font-variant-caps:small-caps;font-synthesis:none", "Abc d"), 0.01, "font-synthesis: none shows the text as it is");
        Check.near(5 * cw, width("font-variant-caps:small-caps;font-feature-settings:\"smcp\" off", "Abc d"), 0.01, "the settings can turn it off");
        Check.near(5 * cw, width("font-variant-caps:titling-caps", "Abc d"), 0.01, "titling capitals have no effect in this engine");
        Check.near(5 * cw, width("font-variant-ligatures:none;font-variant-numeric:tabular-nums", "Abc d"), 0.01, "ligatures and numerals have no effect");
    }

    /**
     * Superscript and subscript text is smaller (70 percent) and moved from the baseline: up by a third of the font size, down by a fifth.
     * <p>
     * 위첨자와 아래첨자 글자는 더 작고(70 %) 기준선에서 옮겨진다: 글꼴 크기의 3분의 1만큼 올리고 5분의 1만큼 내린다.
     */
    public void testPosition() {
        double cw = LayoutTestSupport.charWidth();
        LayoutBox plain = fragments("", "ab").get(0);
        LayoutBox sup = fragments("font-variant-position:super", "ab").get(0);
        LayoutBox sub = fragments("font-variant-position:sub", "ab").get(0);
        Check.near(2 * cw * 0.7, sup.getWidth(), 0.7, "the superscript is 70 percent of the size");
        Check.near(2 * cw * 0.7, sub.getWidth(), 0.7, "the subscript is 70 percent of the size");
        // 기준선(위쪽 + ascent)의 위치로 올림과 내림을 비교한다.
        double plainBase = plain.ly + plain.getAscent();
        double supBase = sup.ly + sup.getAscent();
        double subBase = sub.ly + sub.getAscent();
        double size = plain.getFont().getSize2D();
        Check.near(plainBase - size / 3, supBase, 1.0, "the superscript is raised by a third of the font size");
        Check.near(plainBase + size / 5, subBase, 1.0, "the subscript is lowered by a fifth of the font size");
        Check.near(2 * cw * 0.7, fragments("font-feature-settings:\"sups\"", "ab").get(0).getWidth(), 0.7, "sups in the settings");
        Check.near(2 * cw, fragments("font-variant-position:super;font-synthesis:none", "ab").get(0).getWidth(), 0.01, "font-synthesis: none keeps the text as it is");
        Check.near(2 * cw, fragments("font-variant-position:super;font-synthesis:small-caps", "ab").get(0).getWidth(), 0.01, "font-synthesis without position too");
        double mixed = width("", "ab") + width("font-variant-position:super", "cd");
        Check.isTrue(mixed > 0, "both are laid out");
    }

    /**
     * Full width forms replace the ASCII characters by the full width ones (U+FF01 to U+FF5E), which are twice as wide in a monospaced font; the space and the other
     * characters are kept, and the source offsets stay valid.
     * <p>
     * 전각 형태는 ASCII 문자를 전각 문자(U+FF01 ~ U+FF5E)로 바꾼다. 고정폭 글꼴에서는 두 배 넓다. 공백과 나머지 문자는 그대로 두며 원본 위치는 유지된다.
     */
    public void testFullWidth() {
        List<LayoutBox> f = fragments("font-variant-east-asian:full-width", "Ab 1");
        StringBuilder text = new StringBuilder();
        // 조각의 텍스트를 이어 붙인다.
        for (LayoutBox b : f) {
            text.append(b.getText());
        }
        Check.equal("Ａｂ １", text.toString(), "the letters and the digit are full width, the space is kept");
        Check.equal("Ａｂ １", joined("font-feature-settings:\"fwid\"", "Ab 1"), "fwid in the settings");
        Check.equal("Ab 1", joined("font-variant-east-asian:proportional-width", "Ab 1"), "proportional-width changes nothing");
        Check.equal("Ab 1", joined("font-variant-east-asian:jis78 ruby", "Ab 1"), "glyph variants have no effect");
    }
}
