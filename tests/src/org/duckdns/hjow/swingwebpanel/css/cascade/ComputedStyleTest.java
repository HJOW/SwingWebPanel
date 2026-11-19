package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxDeclaration;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the computation of values (plan items P08-04, P08-05): inheritance, inherit/initial/unset, relative units, font keywords, border widths,
 * currentcolor, var() substitution with fallbacks and cycles, display adjustments and the typed accessors of {@link ComputedStyle}.
 * <p>
 * 값 계산 시험(계획 P08-04, P08-05): 상속, inherit/initial/unset, 상대 단위, 글꼴 키워드, 테두리 두께, currentcolor, 대체값과 순환이 있는 var() 치환,
 * display 조정, {@link ComputedStyle} 의 형식별 접근자.
 */
public class ComputedStyleTest {

    /** The registry. / 등록부. */
    private final PropertyRegistry reg = PropertyRegistry.getInstance();

    /** A 1000 by 500 viewport with default fonts. / 기본 글꼴의 1000 x 500 뷰포트. */
    private final LengthContext vp = new LengthContext(16, 16, 19.2, 1000, 500);

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public ComputedStyleTest() {
    }

    /**
     * Computes a style from declaration text such as <code>color: red; margin: 1px</code>. 선언 문자열로 스타일을 계산한다.
     *
     * @param css    declarations / 선언
     * @param parent parent style or null / 부모 스타일 또는 null
     * @param root   true for the root element / 루트 요소이면 true
     * @return the computed style / 계산 스타일
     */
    private ComputedStyle style(String css, ComputedStyle parent, boolean root) {
        CssValue[] cascaded = new CssValue[Prop.COUNT + 1];
        Map<String, List<ComponentValue>> custom = new LinkedHashMap<String, List<ComponentValue>>();
        List<SyntaxDeclaration> decls = CssParser.parseDeclarationList(css, null);
        // 선언을 차례로 적용한다(나중 선언이 이긴다).
        for (int i = 0; i < decls.size(); i++) {
            SyntaxDeclaration d = decls.get(i);
            if (d.isCustomProperty()) {
                custom.put(d.getName(), new ArrayList<ComponentValue>(d.getValue()));
                continue;
            }
            Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
            int r = reg.parseDeclaration(d.getName(), d.getValue(), new ParseEnv(reg, null), out);
            Check.equal(PropertyRegistry.OK, r, "declaration '" + d + "' must parse");
            // 항목 의 항목을 하나씩 처리한다.
            for (Map.Entry<Integer, CssValue> e : out.entrySet()) {
                cascaded[e.getKey().intValue()] = e.getValue();
            }
        }
        return StyleComputer.compute(cascaded, custom, parent, vp, root);
    }

    /**
     * Computes a style with a parent computed from other declarations. 다른 선언으로 계산한 부모가 있는 스타일을 계산한다.
     *
     * @param parentCss parent declarations / 부모 선언
     * @param css       own declarations / 자신의 선언
     * @return the computed style / 계산 스타일
     */
    private ComputedStyle child(String parentCss, String css) {
        return style(css, style(parentCss, null, false), false);
    }

    /**
     * The style without declarations. 선언이 없는 스타일.
     */
    public void testInitialStyle() {
        ComputedStyle s = ComputedStyle.initial();
        Check.equal("inline", s.display(), "display");
        Check.equal("static", s.position(), "position");
        Check.equal("rgb(0, 0, 0)", s.getPropertyValue("color"), "color");
        Check.near(16, s.fontSizePx(), 1e-9, "font size");
        Check.equal(400, s.fontWeight(), "font weight");
        Check.near(0, s.px(Prop.MARGIN_TOP), 1e-9, "margin");
        Check.near(0, s.px(Prop.BORDER_TOP_WIDTH), 1e-9, "border width is zero because the style is none");
        Check.isTrue(Double.isNaN(s.lineHeightPx()), "normal line height has no pixel value");
        Check.near(1, s.number(Prop.OPACITY), 1e-9, "opacity");
        Check.isTrue(s.isAuto(Prop.WIDTH), "width is auto");
        Check.isTrue(Double.isNaN(s.px(Prop.WIDTH)), "auto has no pixel value");
        Check.equal("serif", s.fontFamilies().get(0), "font family");
        Check.equal("", s.getPropertyValue("no-such-property"), "unknown property");
        Check.equal("0px", s.getPropertyValue("margin"), "shorthand with equal sides");
        Check.notNull(s.get(Prop.WIDTH), "values are never null");
        Check.notNull(s.get("width"), "values by name");
        Check.isNull(s.get("margin"), "a shorthand has no value of its own");
    }

    /**
     * Inheritance and the CSS-wide keywords. 상속과 CSS 전역 키워드.
     */
    public void testInheritance() {
        ComputedStyle s = child("color: red; font-size: 20px; display: block; margin-top: 5px", "");
        Check.equal("rgb(255, 0, 0)", s.getPropertyValue("color"), "color is inherited");
        Check.near(20, s.fontSizePx(), 1e-9, "font size is inherited");
        Check.equal("inline", s.display(), "display is not inherited");
        Check.near(0, s.px(Prop.MARGIN_TOP), 1e-9, "margin is not inherited");
        ComputedStyle t = child("color: red; display: block; margin-top: 5px", "display: inherit; margin-top: inherit; color: initial");
        Check.equal("block", t.display(), "inherit takes the parent's value");
        Check.near(5, t.px(Prop.MARGIN_TOP), 1e-9, "inherit on a non-inherited property");
        Check.equal("rgb(0, 0, 0)", t.getPropertyValue("color"), "initial overrides inheritance");
        ComputedStyle u = child("color: red; margin-top: 5px", "color: unset; margin-top: unset");
        Check.equal("rgb(255, 0, 0)", u.getPropertyValue("color"), "unset on an inherited property inherits");
        Check.near(0, u.px(Prop.MARGIN_TOP), 1e-9, "unset on a non-inherited property is initial");
        ComputedStyle v = style("display: inherit; color: inherit", null, false);
        Check.equal("inline", v.display(), "inherit without a parent is initial");
    }

    /**
     * Relative units and the order of computation. 상대 단위와 계산 순서.
     */
    public void testRelativeUnits() {
        ComputedStyle s = child("font-size: 10px", "width: 2em");
        Check.near(20, s.px(Prop.WIDTH), 1e-9, "em uses the inherited font size");
        ComputedStyle t = child("font-size: 10px", "font-size: 2em; width: 2em");
        Check.near(20, t.fontSizePx(), 1e-9, "font-size in em is relative to the parent");
        Check.near(40, t.px(Prop.WIDTH), 1e-9, "other lengths use the element's own font size");
        ComputedStyle root = style("font-size: 10px", null, true);
        Check.near(10, root.rootFontSizePx(), 1e-9, "the root font size is recorded");
        ComputedStyle r = style("width: 2rem", root, false);
        Check.near(20, r.px(Prop.WIDTH), 1e-9, "rem uses the root font size");
        ComputedStyle w = style("width: 10vw; height: 10vh; margin-left: 1in", null, false);
        Check.near(100, w.px(Prop.WIDTH), 1e-9, "vw");
        Check.near(50, w.px(Prop.HEIGHT), 1e-9, "vh");
        Check.near(96, w.px(Prop.MARGIN_LEFT), 1e-9, "inch");
        ComputedStyle c = child("font-size: 10px", "width: calc(1em + 5px)");
        Check.near(15, c.px(Prop.WIDTH), 1e-9, "calc without percentages is folded");
        ComputedStyle rootFs = style("font-size: 2rem", null, true);
        Check.near(32, rootFs.fontSizePx(), 1e-9, "rem on the root font size refers to the initial size");
    }

    /**
     * Font size keywords, font weight and line height. 글꼴 크기 키워드, 글꼴 두께, 줄 높이.
     */
    public void testFontValues() {
        Check.near(18, child("", "font-size: large").fontSizePx(), 1e-9, "large");
        Check.near(9, child("", "font-size: xx-small").fontSizePx(), 1e-9, "xx-small");
        Check.near(24, child("font-size: 20px", "font-size: larger").fontSizePx(), 1e-9, "larger");
        Check.near(10, child("font-size: 12px", "font-size: smaller").fontSizePx(), 1e-9, "smaller");
        Check.near(15, child("font-size: 20px", "font-size: 75%").fontSizePx(), 1e-9, "percentage");
        Check.near(14, child("font-size: 20px", "font-size: calc(50% + 4px)").fontSizePx(), 1e-9, "calc with a percentage");
        Check.equal(700, child("", "font-weight: bold").fontWeight(), "bold");
        Check.equal(700, child("font-weight: normal", "font-weight: bolder").fontWeight(), "bolder from normal");
        Check.equal(900, child("font-weight: 700", "font-weight: bolder").fontWeight(), "bolder from bold");
        Check.equal(400, child("font-weight: 700", "font-weight: lighter").fontWeight(), "lighter from bold");
        Check.equal(100, child("font-weight: 400", "font-weight: lighter").fontWeight(), "lighter from normal");
        Check.equal(650, child("", "font-weight: 650").fontWeight(), "number");
        ComputedStyle l = child("font-size: 10px", "line-height: 1.5");
        Check.near(15, l.lineHeightPx(), 1e-9, "unitless line height times the font size");
        Check.near(1.5, l.number(Prop.LINE_HEIGHT), 1e-9, "unitless line height stays a number");
        Check.near(15, child("font-size: 10px", "line-height: 150%").lineHeightPx(), 1e-9, "percentage line height becomes pixels");
        Check.near(20, child("font-size: 10px", "line-height: 2em").lineHeightPx(), 1e-9, "em line height");
        Check.equal(3, style("font-family: \"Times New Roman\", Arial, serif", null, false).fontFamilies().size(), "family count");
        Check.equal("Times New Roman", style("font-family: Times New Roman, serif", null, false).fontFamilies().get(0), "unquoted name words are joined");
    }

    /**
     * Border widths, colors and currentcolor. 테두리 두께, 색, currentcolor.
     */
    public void testBordersAndColors() {
        ComputedStyle s = child("", "border: 2px solid red");
        Check.near(2, s.px(Prop.BORDER_LEFT_WIDTH), 1e-9, "width");
        Check.equal("solid", s.keyword(Prop.BORDER_LEFT_STYLE), "style");
        Check.equal(0xFFFF0000, s.color(Prop.BORDER_LEFT_COLOR), "color");
        ComputedStyle t = child("", "border-top-width: 10px; border-top-style: none");
        Check.near(0, t.px(Prop.BORDER_TOP_WIDTH), 1e-9, "no style, no width");
        ComputedStyle k = child("", "border-style: solid; border-top-width: thin; border-right-width: medium; border-bottom-width: thick");
        Check.near(1, k.px(Prop.BORDER_TOP_WIDTH), 1e-9, "thin");
        Check.near(3, k.px(Prop.BORDER_RIGHT_WIDTH), 1e-9, "medium");
        Check.near(5, k.px(Prop.BORDER_BOTTOM_WIDTH), 1e-9, "thick");
        ComputedStyle c = child("", "color: #00ff00; border-top-color: currentcolor");
        Check.isTrue(c.isKeyword(Prop.BORDER_TOP_COLOR, "currentcolor"), "currentcolor stays a keyword");
        Check.equal(0xFF00FF00, c.color(Prop.BORDER_TOP_COLOR), "and resolves against color");
        Check.equal("rgb(0, 255, 0)", c.getPropertyValue("border-top-color"), "resolved value");
        ComputedStyle d = child("color: blue", "color: currentcolor");
        Check.equal(0xFF0000FF, d.color(Prop.COLOR), "color: currentcolor inherits");
        Check.equal("rgb(255, 0, 0)", child("", "background-color: red").getPropertyValue("background-color"), "colors serialize as rgb");
        Check.equal(0, child("", "outline-color: invert").color(Prop.OUTLINE_COLOR), "a non-color value gives 0");
        Check.equal(0.5, child("", "opacity: 50%").number(Prop.OPACITY), "opacity percentage");
        Check.equal(1.0, child("", "opacity: 2").number(Prop.OPACITY), "opacity is clamped");
    }

    /**
     * Percentages stay symbolic and resolve on demand. 백분율은 기호로 남고 필요할 때 계산된다.
     */
    public void testPercentages() {
        ComputedStyle s = child("", "width: 50%; margin-left: calc(100% - 10px); height: 25px");
        Check.isTrue(s.get(Prop.WIDTH) instanceof CssPercentage, "percentage stays");
        Check.isTrue(Double.isNaN(s.px(Prop.WIDTH)), "px() does not resolve percentages");
        Check.near(100, s.resolve(Prop.WIDTH, 200), 1e-9, "resolve a percentage");
        Check.near(190, s.resolve(Prop.MARGIN_LEFT, 200), 1e-9, "resolve a calc with a percentage");
        Check.near(25, s.resolve(Prop.HEIGHT, 200), 1e-9, "resolve a length");
        Check.isTrue(Double.isNaN(s.resolve(Prop.TOP, 200)), "auto cannot be resolved");
        Check.isTrue(s.get(Prop.OPACITY) instanceof CssNumber, "numbers are CssNumber");
    }

    /**
     * Custom properties and var(). 사용자 정의 속성과 var().
     */
    public void testVariables() {
        ComputedStyle s = child("", "--x: 10px; width: var(--x)");
        Check.near(10, s.px(Prop.WIDTH), 1e-9, "var() substitution");
        Check.equal("10px", s.getCustomProperty("--x").toCssText(), "the custom property value");
        Check.near(5, child("", "width: var(--none, 5px)").px(Prop.WIDTH), 1e-9, "fallback");
        Check.isTrue(child("", "width: 7px; width: var(--none)").isAuto(Prop.WIDTH), "an invalid var() makes the declaration unset");
        ComputedStyle h = child("--c: red", "color: var(--c)");
        Check.equal(0xFFFF0000, h.color(Prop.COLOR), "custom properties are inherited");
        ComputedStyle m = child("", "--m: 3px; margin: var(--m) 2px");
        Check.near(3, m.px(Prop.MARGIN_TOP), 1e-9, "var() in a shorthand, top");
        Check.near(2, m.px(Prop.MARGIN_RIGHT), 1e-9, "var() in a shorthand, right");
        ComputedStyle n = child("", "--a: 1px; --b: var(--a) 2px; margin: var(--b) var(--b)");
        Check.near(1, n.px(Prop.MARGIN_TOP), 1e-9, "nested custom properties");
        Check.near(2, n.px(Prop.MARGIN_RIGHT), 1e-9, "nested custom properties, second value");
        ComputedStyle cyc = child("", "--a: var(--b); --b: var(--a); width: var(--a, 7px)");
        Check.near(7, cyc.px(Prop.WIDTH), 1e-9, "a cycle makes both properties invalid, so the fallback is used");
        Check.isNull(cyc.getCustomProperty("--a"), "cyclic property removed");
        ComputedStyle self = child("", "--a: var(--a); width: var(--a, 9px)");
        Check.near(9, self.px(Prop.WIDTH), 1e-9, "self reference");
        ComputedStyle over = child("--c: red", "--c: blue; color: var(--c)");
        Check.equal(0xFF0000FF, over.color(Prop.COLOR), "own declaration overrides the inherited custom property");
        ComputedStyle fb = child("", "color: var(--none, var(--also-none, green))");
        Check.equal(0xFF008000, fb.color(Prop.COLOR), "nested fallbacks");
        ComputedStyle inFn = child("--w: 20px", "width: calc(var(--w) + 5px)");
        Check.near(25, inFn.px(Prop.WIDTH), 1e-9, "var() inside a function");
        ComputedStyle empty = child("", "--e: ; margin-top: var(--e, 4px)");
        Check.near(0, empty.px(Prop.MARGIN_TOP), 1e-9, "an empty value is valid but makes the declaration invalid here");
        Check.equal("20px", child("--w: 20px", "").getPropertyValue("--w"), "getPropertyValue of a custom property");
        Check.equal(1, child("--w: 20px", "").customPropertyNames().size(), "custom property names");
    }

    /**
     * Display, float and position interplay. display, float, position 의 상호 작용.
     */
    public void testDisplayAdjustments() {
        Check.equal("block", child("", "float: left").display(), "a float is blockified");
        Check.equal("block", child("", "position: absolute; display: inline-block").display(), "absolute positioning blockifies");
        Check.equal("flex", child("", "position: fixed; display: inline-flex").display(), "inline-flex becomes flex");
        Check.equal("table", child("", "float: right; display: inline-table").display(), "inline-table becomes table");
        Check.equal("block", child("", "float: left; display: table-cell").display(), "table-cell becomes block");
        Check.equal("block", style("display: inline", null, true).display(), "the root element is blockified");
        Check.equal("inline", child("", "position: relative").display(), "relative positioning does not blockify");
        Check.equal("none", child("", "float: left; display: none").display(), "none stays");
        Check.equal("none", child("", "position: absolute; float: left").keyword(Prop.FLOAT), "absolute positioning cancels float");
        Check.equal("auto", child("", "overflow-x: hidden").keyword(Prop.OVERFLOW_Y), "visible becomes auto when the other axis scrolls");
        Check.equal("hidden", child("", "overflow-x: clip; overflow-y: hidden").keyword(Prop.OVERFLOW_X), "clip becomes hidden");
        Check.equal("visible", child("", "overflow: clip visible").keyword(Prop.OVERFLOW_Y), "visible stays next to clip");
    }

    /**
     * Value comparison for invalidation. 무효화를 위한 값 비교.
     */
    public void testSameValues() {
        ComputedStyle a = child("", "color: red; margin: 1px");
        ComputedStyle b = child("", "margin: 1px; color: red");
        ComputedStyle c = child("", "color: red; margin: 2px");
        Check.isTrue(a.sameValues(b), "the same declarations in another order");
        Check.isFalse(a.sameValues(c), "different margin");
        Check.isFalse(a.sameValues(null), "null");
        Check.isFalse(child("", "--x: 1").sameValues(child("", "--x: 2")), "different custom property");
        Check.isTrue(child("", "--x: 1").sameValues(child("", "--x: 1")), "equal custom property");
    }
}
