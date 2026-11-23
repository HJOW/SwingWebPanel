package org.duckdns.hjow.swingwebpanel.css;

import java.util.LinkedHashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the scrolling properties (plan item P10-08, CSS Scroll Snap Level 1 https://www.w3.org/TR/css-scroll-snap-1/, CSS Overscroll Behavior Level 1,
 * CSS Scrollbars Level 1 https://www.w3.org/TR/css-scrollbars-1/): the grammar of <code>scroll-snap-type</code>, <code>scroll-snap-align</code>,
 * <code>scroll-snap-stop</code>, <code>scroll-padding-*</code>, <code>scroll-margin-*</code> (physical and logical), <code>overscroll-behavior-*</code>,
 * <code>scrollbar-width</code>, <code>scrollbar-color</code>, <code>scrollbar-gutter</code> and <code>touch-action</code>, the shorthands, the computed values, the
 * mapping of the logical properties to the sides under a writing mode and a direction, and the inheritance. The engine has no scroll bars of its own (the host scroll pane
 * scrolls the page) and does not snap, so these properties have computed values only.
 * <p>
 * 스크롤 속성 시험(계획 P10-08, CSS Scroll Snap Level 1, CSS Overscroll Behavior Level 1, CSS Scrollbars Level 1): <code>scroll-snap-type</code>, <code>scroll-snap-align</code>,
 * <code>scroll-snap-stop</code>, <code>scroll-padding-*</code>, <code>scroll-margin-*</code>(물리와 논리), <code>overscroll-behavior-*</code>, <code>scrollbar-width</code>,
 * <code>scrollbar-color</code>, <code>scrollbar-gutter</code>, <code>touch-action</code> 의 문법, 단축 속성, 계산 값, 쓰기 모드와 방향에 따른 논리 속성의 변 대응, 상속.
 * 엔진에는 자체 스크롤 막대가 없고(호스트의 스크롤 창이 페이지를 스크롤한다) 스냅하지 않으므로 이 속성들은 계산 값만 가진다.
 */
public class ScrollPropertiesTest {

    /** The registry under test. / 시험 대상 등록부. */
    private final PropertyRegistry reg = PropertyRegistry.getInstance();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScrollPropertiesTest() {
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
     * Returns the computed style of the element <code>t</code> of a page that has the style.
     * <p>
     * 스타일이 있는 페이지의 요소 <code>t</code> 의 계산 스타일을 반환한다.
     *
     * @param css  the style sheet / 스타일시트
     * @param body the body markup / 본문 마크업
     * @return the computed style / 계산 스타일
     */
    private static ComputedStyle style(String css, String body) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + body + "</body></html>", "http://t.test/page.html",
                null, true);
        CssEngine e = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        e.loadStyleSheets();
        return e.getComputedStyle(doc.getElementById("t"));
    }

    /**
     * <code>scroll-snap-type</code>: <code>none</code>, an axis with an optional strictness, and the invalid forms.
     * <p>
     * <code>scroll-snap-type</code>: <code>none</code>, 축과 선택적인 엄격도, 올바르지 않은 형태.
     */
    public void testSnapType() {
        String[] ok = {"none", "x", "y", "block", "inline", "both", "x mandatory", "y proximity", "block mandatory", "inline proximity", "both mandatory"};
        // 올바른 값은 그대로 계산되어야 한다.
        for (int i = 0; i < ok.length; i++) {
            Check.equal(ok[i], v("scroll-snap-type", ok[i], "scroll-snap-type"), ok[i]);
        }
        String[] bad = {"mandatory", "x y", "diagonal", "x mandatory proximity", "none mandatory", "x, y"};
        // 올바르지 않은 값은 선언 전체가 버려져야 한다.
        for (int i = 0; i < bad.length; i++) {
            Check.equal("INVALID", v("scroll-snap-type", bad[i], "scroll-snap-type"), "invalid: " + bad[i]);
        }
        Check.equal("none", style("", "<div id=t></div>").getPropertyValue("scroll-snap-type"), "the initial value");
        Check.equal("y mandatory", style("#t{scroll-snap-type:y mandatory}", "<div id=t></div>").getPropertyValue("scroll-snap-type"), "a computed value");
    }

    /**
     * <code>scroll-snap-align</code> takes one or two keywords and <code>scroll-snap-stop</code> <code>normal</code> or <code>always</code>.
     * <p>
     * <code>scroll-snap-align</code> 은 키워드 하나나 둘을, <code>scroll-snap-stop</code> 은 <code>normal</code> 이나 <code>always</code> 를 받는다.
     */
    public void testSnapAlignAndStop() {
        String[] ok = {"none", "start", "end", "center", "start end", "center none", "end center"};
        // 올바른 값은 그대로 계산되어야 한다.
        for (int i = 0; i < ok.length; i++) {
            Check.equal(ok[i], v("scroll-snap-align", ok[i], "scroll-snap-align"), ok[i]);
        }
        Check.equal("INVALID", v("scroll-snap-align", "top", "scroll-snap-align"), "top is not an alignment");
        Check.equal("INVALID", v("scroll-snap-align", "start end center", "scroll-snap-align"), "at most two keywords");
        Check.equal("always", v("scroll-snap-stop", "always", "scroll-snap-stop"), "always");
        Check.equal("INVALID", v("scroll-snap-stop", "sometimes", "scroll-snap-stop"), "an unknown stop");
        Check.equal("normal", style("", "<div id=t></div>").getPropertyValue("scroll-snap-stop"), "the initial value of the stop");
        Check.equal("none", style("", "<div id=t></div>").getPropertyValue("scroll-snap-align"), "the initial value of the alignment");
    }

    /**
     * <code>scroll-padding</code> takes lengths, percentages and <code>auto</code> (not negative); <code>scroll-margin</code> takes lengths only (negative allowed); the shorthands
     * take one to four values.
     * <p>
     * <code>scroll-padding</code> 은 길이, 백분율, <code>auto</code>(음수 불가)를, <code>scroll-margin</code> 은 길이만(음수 가능) 받으며 단축 속성은 값 한 개에서 네 개까지 받는다.
     */
    public void testPaddingAndMargin() {
        Check.equal("auto", v("scroll-padding", "auto", "scroll-padding-top"), "auto padding");
        Check.equal("10%", v("scroll-padding", "10%", "scroll-padding-left"), "a percentage padding");
        Check.equal("1px", v("scroll-padding", "1px 2px 3px 4px", "scroll-padding-top"), "top");
        Check.equal("2px", v("scroll-padding", "1px 2px 3px 4px", "scroll-padding-right"), "right");
        Check.equal("3px", v("scroll-padding", "1px 2px 3px 4px", "scroll-padding-bottom"), "bottom");
        Check.equal("4px", v("scroll-padding", "1px 2px 3px 4px", "scroll-padding-left"), "left");
        Check.equal("2px", v("scroll-padding", "1px 2px", "scroll-padding-left"), "two values: the left follows the right");
        Check.equal("INVALID", v("scroll-padding", "-1px", "scroll-padding-top"), "a negative padding");
        Check.equal("-4px", v("scroll-margin", "-4px", "scroll-margin-top"), "a negative margin");
        Check.equal("INVALID", v("scroll-margin", "10%", "scroll-margin-top"), "a margin cannot be a percentage");
        Check.equal("INVALID", v("scroll-margin", "auto", "scroll-margin-top"), "a margin cannot be auto");
        Check.equal("INVALID", v("scroll-padding-top", "1px 2px", "scroll-padding-top"), "a longhand takes one value");
        ComputedStyle s = style("#t{font-size:20px;scroll-margin:1em 2px;scroll-padding:10% auto}", "<div id=t></div>");
        Check.equal("20px", s.getPropertyValue("scroll-margin-top"), "em is computed to px");
        Check.equal("2px", s.getPropertyValue("scroll-margin-left"), "the left margin");
        Check.equal("10%", s.getPropertyValue("scroll-padding-top"), "a percentage stays a percentage");
        Check.equal("auto", s.getPropertyValue("scroll-padding-left"), "auto stays");
        Check.equal("0px", style("", "<div id=t></div>").getPropertyValue("scroll-margin-top"), "the initial margin is 0");
        Check.equal("auto", style("", "<div id=t></div>").getPropertyValue("scroll-padding-top"), "the initial padding is auto");
    }

    /**
     * The logical scroll margins and paddings: the shorthands set both ends, and under a writing mode and a direction they stand for the physical sides.
     * <p>
     * 논리 scroll-margin/scroll-padding: 단축 속성은 양 끝을 정하고, 쓰기 모드와 방향에 따라 물리 변을 가리킨다.
     */
    public void testLogicalMapping() {
        Check.equal("1px", v("scroll-margin-block", "1px 2px", "scroll-margin-block-start"), "block start");
        Check.equal("2px", v("scroll-margin-block", "1px 2px", "scroll-margin-block-end"), "block end");
        Check.equal("3px", v("scroll-margin-inline", "3px", "scroll-margin-inline-end"), "one value sets both ends");
        Check.equal("4px", v("scroll-padding-inline", "4px 5px", "scroll-padding-inline-start"), "padding inline start");
        Check.equal("INVALID", v("scroll-padding-block", "-1px", "scroll-padding-block-start"), "a negative logical padding");
        ComputedStyle h = style("#t{scroll-margin-block-start:1px;scroll-margin-block-end:2px;scroll-margin-inline-start:3px;scroll-margin-inline-end:4px}", "<div id=t></div>");
        Check.equal("1px", h.getPropertyValue("scroll-margin-top"), "horizontal: block-start is the top");
        Check.equal("2px", h.getPropertyValue("scroll-margin-bottom"), "block-end is the bottom");
        Check.equal("3px", h.getPropertyValue("scroll-margin-left"), "inline-start is the left");
        Check.equal("4px", h.getPropertyValue("scroll-margin-right"), "inline-end is the right");
        ComputedStyle rtl = style("#t{direction:rtl;scroll-margin-inline-start:3px;scroll-padding-inline-end:6px}", "<div id=t></div>");
        Check.equal("3px", rtl.getPropertyValue("scroll-margin-right"), "rtl: inline-start is the right");
        Check.equal("6px", rtl.getPropertyValue("scroll-padding-left"), "rtl: inline-end is the left");
        ComputedStyle v = style("#t{writing-mode:vertical-rl;scroll-margin-block-start:1px;scroll-margin-inline-start:3px;scroll-padding-block-end:7px}", "<div id=t></div>");
        Check.equal("1px", v.getPropertyValue("scroll-margin-right"), "vertical-rl: block-start is the right");
        Check.equal("3px", v.getPropertyValue("scroll-margin-top"), "vertical-rl: inline-start is the top");
        Check.equal("7px", v.getPropertyValue("scroll-padding-left"), "vertical-rl: block-end is the left");
        ComputedStyle order = style("#t{scroll-margin-block-start:1px;scroll-margin-top:9px}", "<div id=t></div>");
        Check.equal("9px", order.getPropertyValue("scroll-margin-top"), "the later declaration wins between the logical and the physical name");
        ComputedStyle order2 = style("#t{scroll-margin-top:9px;scroll-margin-block-start:1px}", "<div id=t></div>");
        Check.equal("1px", order2.getPropertyValue("scroll-margin-top"), "and the other way round");
        Check.equal("1px", order2.getPropertyValue("scroll-margin-block-start"), "the logical name reads the physical side");
        Check.isTrue(Prop.SCROLL_MARGIN_BLOCK_START < Prop.COUNT, "the logical ids are inside the property table");
    }

    /**
     * <code>overscroll-behavior</code> sets the two axes.
     * <p>
     * <code>overscroll-behavior</code> 는 두 축을 정한다.
     */
    public void testOverscroll() {
        Check.equal("contain", v("overscroll-behavior", "contain", "overscroll-behavior-x"), "one value sets x");
        Check.equal("contain", v("overscroll-behavior", "contain", "overscroll-behavior-y"), "and y");
        Check.equal("none", v("overscroll-behavior", "contain none", "overscroll-behavior-y"), "two values: x then y");
        Check.equal("INVALID", v("overscroll-behavior", "scroll", "overscroll-behavior-x"), "an unknown value");
        Check.equal("auto", style("", "<div id=t></div>").getPropertyValue("overscroll-behavior-x"), "the initial value");
    }

    /**
     * <code>scrollbar-width</code>, <code>scrollbar-color</code> (two colors or <code>auto</code>; inherited) and <code>scrollbar-gutter</code>.
     * <p>
     * <code>scrollbar-width</code>, <code>scrollbar-color</code>(두 색 또는 <code>auto</code>. 상속됨), <code>scrollbar-gutter</code>.
     */
    public void testScrollbars() {
        Check.equal("thin", v("scrollbar-width", "thin", "scrollbar-width"), "thin");
        Check.equal("none", v("scrollbar-width", "none", "scrollbar-width"), "none");
        Check.equal("INVALID", v("scrollbar-width", "10px", "scrollbar-width"), "a length is not allowed");
        Check.equal("stable both-edges", v("scrollbar-gutter", "stable both-edges", "scrollbar-gutter"), "stable both-edges");
        Check.equal("stable", v("scrollbar-gutter", "stable", "scrollbar-gutter"), "stable");
        Check.equal("INVALID", v("scrollbar-gutter", "both-edges", "scrollbar-gutter"), "both-edges needs stable");
        Check.equal("INVALID", v("scrollbar-color", "red", "scrollbar-color"), "one color is invalid");
        ComputedStyle s = style("#p{scrollbar-color:#f00 #00f}", "<div id=p><div id=t></div></div>");
        Check.equal("rgb(255, 0, 0) rgb(0, 0, 255)", s.getPropertyValue("scrollbar-color"), "scrollbar-color is inherited, and the colors are computed");
        Check.equal("auto", style("#p{scrollbar-width:thin}", "<div id=p><div id=t></div></div>").getPropertyValue("scrollbar-width"), "scrollbar-width is not inherited");
        Check.equal("auto", style("#p{scroll-snap-type:x mandatory}", "<div id=p><div id=t></div></div>").getPropertyValue("scrollbar-gutter"), "the initial gutter");
        Check.equal("none", style("#p{scroll-snap-type:x mandatory}", "<div id=p><div id=t></div></div>").getPropertyValue("scroll-snap-type"), "scroll-snap-type is not inherited");
    }

    /**
     * <code>touch-action</code>: the keywords and the combinations of the pan and pinch values.
     * <p>
     * <code>touch-action</code>: 키워드와 pan, pinch 값의 조합.
     */
    public void testTouchAction() {
        String[] ok = {"auto", "none", "manipulation", "pan-x", "pan-y", "pan-x pan-y", "pan-left pan-down", "pan-x pinch-zoom", "pinch-zoom pan-y"};
        // 올바른 조합은 그대로 계산되어야 한다.
        for (int i = 0; i < ok.length; i++) {
            Check.equal(ok[i], v("touch-action", ok[i], "touch-action"), ok[i]);
        }
        Check.equal("INVALID", v("touch-action", "pan-x pan-left", "touch-action"), "two horizontal values");
        Check.equal("INVALID", v("touch-action", "none pan-x", "touch-action"), "none cannot combine");
    }

    /**
     * None of them is unsupported for <code>@supports</code> and none is parse-only by mistake: the names are known longhands or shorthands.
     * <p>
     * <code>@supports</code> 에서 지원하지 않는 것으로 나오는 것이 없고, 이름은 알려진 개별 속성이나 단축 속성이다.
     */
    public void testKnownNames() {
        String[] longhands = {"scroll-snap-type", "scroll-snap-align", "scroll-snap-stop", "scroll-padding-top", "scroll-margin-left", "scroll-margin-block-start",
            "scroll-padding-inline-end", "overscroll-behavior-x", "scrollbar-width", "scrollbar-color", "scrollbar-gutter", "touch-action"};
        // 이름마다 개별 속성으로 등록되어 있는지 확인한다.
        for (int i = 0; i < longhands.length; i++) {
            Check.notNull(reg.byName(longhands[i]), longhands[i] + " is a longhand");
        }
        String[] shorthands = {"scroll-padding", "scroll-margin", "scroll-margin-block", "scroll-margin-inline", "scroll-padding-block", "scroll-padding-inline", "overscroll-behavior"};
        // 단축 속성도 등록되어 있는지 확인한다.
        for (int i = 0; i < shorthands.length; i++) {
            Check.notNull(reg.getShorthand(shorthands[i]), shorthands[i] + " is a shorthand");
        }
        Check.isTrue(org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition.evaluate("(scroll-snap-type: x mandatory)"), "@supports knows scroll-snap-type");
        Check.isTrue(org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition.evaluate("(scroll-margin-inline: 1px)"), "@supports knows the logical shorthand");
        Check.isFalse(org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition.evaluate("(scroll-snap-type: sideways)"), "and rejects a wrong value");
    }
}
