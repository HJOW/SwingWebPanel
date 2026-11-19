package org.duckdns.hjow.swingwebpanel.css;

import java.util.LinkedHashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the properties of containment, scrolling, color scheme, text wrapping and font variants that the engine parses (plan item P10-08): the
 * grammar of every longhand, the shorthands <code>container</code>, <code>contain-intrinsic-size</code>, <code>scroll-padding</code>,
 * <code>scroll-margin</code>, <code>overscroll-behavior</code>, <code>text-wrap</code>, <code>text-emphasis</code>, and that none of them is reported
 * as unsupported.
 * <p>
 * 엔진이 파싱하는 격리, 스크롤, 색 구성표, 텍스트 줄바꿈, 글꼴 변형 속성 시험(계획 P10-08): 모든 개별 속성의 문법, 단축 속성 <code>container</code>,
 * <code>contain-intrinsic-size</code>, <code>scroll-padding</code>, <code>scroll-margin</code>, <code>overscroll-behavior</code>, <code>text-wrap</code>,
 * <code>text-emphasis</code>, 그리고 그중 어느 것도 지원하지 않는 것으로 보고되지 않음.
 */
public class ContainmentPropertiesTest {

    /** The registry under test. / 시험 대상 등록부. */
    private final PropertyRegistry reg = PropertyRegistry.getInstance();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContainmentPropertiesTest() {
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
     * The container properties and the <code>container</code> shorthand.
     * <p>
     * 컨테이너 속성과 <code>container</code> 단축 속성.
     */
    public void testContainer() {
        Check.equal("inline-size", v("container-type", "inline-size", "container-type"), "container-type");
        Check.equal("INVALID", v("container-type", "block", "container-type"), "an unknown container type");
        Check.equal("card sidebar", v("container-name", "card sidebar", "container-name"), "several names");
        Check.equal("none", v("container-name", "none", "container-name"), "none");
        Check.equal("card", v("container", "card / inline-size", "container-name"), "the name of the shorthand");
        Check.equal("inline-size", v("container", "card / inline-size", "container-type"), "the type of the shorthand");
        Check.equal("size", v("container", "a b / size", "container-type"), "several names and a type");
        Check.equal("initial", v("container", "card", "container-type"), "a name alone resets the type");
        Check.equal("INVALID", v("container", "/ size", "container-type"), "a missing name");
        Check.equal("INVALID", v("container", "card / foo", "container-type"), "an invalid type");
        Check.equal("hidden", v("content-visibility", "hidden", "content-visibility"), "content-visibility");
        Check.equal("auto 100px", v("contain-intrinsic-height", "auto 100px", "contain-intrinsic-height"), "contain-intrinsic-height: auto length");
        Check.equal("50px", v("contain-intrinsic-size", "50px 20px", "contain-intrinsic-width"), "contain-intrinsic-size sets the width");
        Check.equal("20px", v("contain-intrinsic-size", "50px 20px", "contain-intrinsic-height"), "and the height");
        Check.equal("50px", v("contain-intrinsic-size", "50px", "contain-intrinsic-height"), "one value is for both");
    }

    /**
     * The properties of scrolling.
     * <p>
     * 스크롤 속성.
     */
    public void testScrolling() {
        Check.equal("y mandatory", v("scroll-snap-type", "y mandatory", "scroll-snap-type"), "scroll-snap-type");
        Check.equal("center", v("scroll-snap-align", "center", "scroll-snap-align"), "scroll-snap-align");
        Check.equal("start end", v("scroll-snap-align", "start end", "scroll-snap-align"), "scroll-snap-align with two values");
        Check.equal("10px", v("scroll-padding", "10px", "scroll-padding-left"), "scroll-padding");
        Check.equal("2px", v("scroll-margin", "1px 2px", "scroll-margin-right"), "scroll-margin takes the second value for the right side");
        Check.equal("contain", v("overscroll-behavior", "contain none", "overscroll-behavior-x"), "overscroll-behavior x");
        Check.equal("none", v("overscroll-behavior", "contain none", "overscroll-behavior-y"), "overscroll-behavior y");
        Check.equal("thin", v("scrollbar-width", "thin", "scrollbar-width"), "scrollbar-width");
        Check.equal("auto", v("scrollbar-color", "auto", "scrollbar-color"), "scrollbar-color auto");
        Check.equal("INVALID", v("scrollbar-color", "red", "scrollbar-color"), "scrollbar-color needs two colors");
        Check.equal("stable both-edges", v("scrollbar-gutter", "stable both-edges", "scrollbar-gutter"), "scrollbar-gutter");
        Check.equal("pan-x pinch-zoom", v("touch-action", "pan-x pinch-zoom", "touch-action"), "touch-action");
    }

    /**
     * The properties of color scheme, text and fonts.
     * <p>
     * 색 구성표, 텍스트, 글꼴 속성.
     */
    public void testTextAndFonts() {
        Check.equal("light dark", v("color-scheme", "light dark", "color-scheme"), "color-scheme");
        Check.equal("dark only", v("color-scheme", "dark only", "color-scheme"), "color-scheme only");
        Check.equal("nowrap", v("text-wrap", "nowrap", "text-wrap-mode"), "text-wrap: nowrap sets the mode");
        Check.equal("initial", v("text-wrap", "nowrap", "text-wrap-style"), "and resets the style to its initial value");
        Check.equal("balance", v("text-wrap", "balance", "text-wrap-style"), "text-wrap: balance");
        Check.equal("nowrap", v("text-wrap", "nowrap balance", "text-wrap-mode"), "both parts in a shorthand");
        Check.equal("none", v("hanging-punctuation", "none", "hanging-punctuation"), "hanging-punctuation");
        Check.equal("filled circle", v("text-emphasis", "filled circle", "text-emphasis-style"), "text-emphasis style");
        Check.equal("inter-word", v("text-justify", "inter-word", "text-justify"), "text-justify");
        Check.equal("common-ligatures no-contextual", v("font-variant-ligatures", "common-ligatures no-contextual", "font-variant-ligatures"), "ligatures");
        Check.equal("tabular-nums slashed-zero", v("font-variant-numeric", "tabular-nums slashed-zero", "font-variant-numeric"), "numeric");
        Check.equal("super", v("font-variant-position", "super", "font-variant-position"), "position");
        // font-synthesis 는 네 개별 속성의 단축 속성이다(CSS Fonts 4 2.8): 나열한 종류는 auto, 나머지는 none.
        Check.equal("auto", v("font-synthesis", "weight style", "font-synthesis-weight"), "font-synthesis: weight style, weight");
        Check.equal("auto", v("font-synthesis", "style weight", "font-synthesis-style"), "any order, style");
        Check.equal("none", v("font-synthesis", "weight style", "font-synthesis-small-caps"), "an unlisted kind is none");
        Check.equal("none", v("font-synthesis", "weight style", "font-synthesis-position"), "also position");
        Check.equal("none", v("font-synthesis", "none", "font-synthesis-weight"), "none turns the weight off");
        Check.equal("none", v("font-synthesis", "none", "font-synthesis-style"), "and the style");
        Check.equal("INVALID", v("font-synthesis", "weight weight", "font-synthesis-weight"), "a kind twice is invalid");
        Check.equal("INVALID", v("font-synthesis", "none weight", "font-synthesis-weight"), "none with another value is invalid");
        Check.equal("INVALID", v("font-synthesis", "bold", "font-synthesis-weight"), "an unknown kind is invalid");
        Check.equal("none", v("font-synthesis-style", "none", "font-synthesis-style"), "the longhand style");
        Check.equal("\"wght\" 600", v("font-variation-settings", "\"wght\" 600", "font-variation-settings"), "font-variation-settings");
    }

    /**
     * The names are known to the registry as longhands and none is reported as unsupported.
     * <p>
     * 이름은 등록부에 개별 속성으로 등록되어 있고 지원하지 않는 것으로 보고되지 않는다.
     */
    public void testKnownNames() {
        String[] names = {"container-type", "container-name", "content-visibility", "contain-intrinsic-width", "scroll-snap-type", "scroll-padding-top",
            "scroll-margin-left", "overscroll-behavior-x", "scrollbar-width", "scrollbar-gutter", "touch-action", "color-scheme", "text-wrap-mode",
            "text-wrap-style", "hyphenate-character", "text-emphasis-color", "font-variant-ligatures", "font-optical-sizing"};
        // 모든 이름이 개별 속성으로 등록되어 있는지 확인한다.
        for (int i = 0; i < names.length; i++) {
            Check.notNull(reg.byName(names[i]), names[i] + " is a longhand");
        }
        Check.notNull(reg.getShorthand("container"), "container is a shorthand");
        Check.notNull(reg.getShorthand("text-wrap"), "text-wrap is a shorthand");
        Check.equal(Prop.CONTAINER_TYPE, reg.byName("container-type").getId(), "the id constant");
    }
}
