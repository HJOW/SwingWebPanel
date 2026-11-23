package org.duckdns.hjow.swingwebpanel.css;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.PositionTryRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the properties and the rule of the overflow management of CSS Anchor Positioning Level 1 section 6 (plan item P10-08, https://www.w3.org/TR/css-anchor-position-1/,
 * Last-Modified 2026-03-27): <code>position-try-fallbacks</code> (<code>none | [ [&lt;dashed-ident&gt; || &lt;try-tactic&gt;] | &lt;position-area&gt; ]#</code>),
 * <code>position-try-order</code>, the <code>position-try</code> shorthand, <code>position-visibility</code>, <code>anchor-scope</code> of section 2.2, the
 * <code>anchor-center</code> value of the self-alignment properties (4.2), and the <code>@position-try</code> rule (6.4: a dashed name, only the positioning
 * properties, no <code>!important</code>).
 * <p>
 * CSS Anchor Positioning Level 1 6 절 넘침 관리의 속성과 규칙 시험(계획 P10-08): <code>position-try-fallbacks</code>, <code>position-try-order</code>, 단축 속성
 * <code>position-try</code>, <code>position-visibility</code>, 2.2 절의 <code>anchor-scope</code>, 자기 정렬 속성의 <code>anchor-center</code> 값(4.2), <code>@position-try</code>
 * 규칙(6.4: 대시 이름, 위치 속성만, <code>!important</code> 없음).
 */
public class PositionTryPropertiesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PositionTryPropertiesTest() {
    }

    /**
     * Parses a declaration and returns the serialized value of a property, or the empty string if it was not stored.
     * <p>
     * 선언을 파싱해 속성의 직렬화한 값을 반환한다. 저장되지 않았으면 빈 문자열.
     *
     * @param decl the declaration / 선언
     * @param name the property / 속성
     * @return the value / 값
     */
    private static String value(String decl, String name) {
        return CssStyleDeclaration.parse(decl, null, new CssDiagnostics()).getPropertyValue(name);
    }

    /**
     * The entries of <code>position-try-fallbacks</code>: names, tactics (each at most once, any order), position areas, comma separated; <code>none</code> only alone.
     * <p>
     * <code>position-try-fallbacks</code> 의 항목: 이름, 전술(각각 최대 한 번, 순서 무관), 위치 영역을 쉼표로 구분한다. <code>none</code> 은 혼자만.
     */
    public void testFallbacksGrammar() {
        String[] ok = { "none", "flip-block", "flip-block flip-inline", "flip-inline flip-block", "--a", "--a flip-start", "flip-x --a", "top", "top span-left", "span-all",
            "flip-x, --b, bottom right", "flip-block flip-inline flip-start flip-x flip-y" };
        // 올바른 값은 그대로 저장된다.
        for (int i = 0; i < ok.length; i++) {
            Check.equal(ok[i], value("position-try-fallbacks: " + ok[i], "position-try-fallbacks"), "valid: " + ok[i]);
        }
        String[] bad = { "flip-block flip-block", "bogus", "--a --b", "flip-block top", "none, flip-x", "flip-z", "--a top", "flip-x,", "top left right", "" };
        // 올바르지 않은 값은 버려진다.
        for (int i = 0; i < bad.length; i++) {
            Check.equal("", value("position-try-fallbacks: " + bad[i], "position-try-fallbacks"), "invalid: '" + bad[i] + "'");
        }
    }

    /**
     * The grammar of <code>position-try-order</code>, <code>position-visibility</code> and <code>anchor-scope</code>.
     * <p>
     * <code>position-try-order</code>, <code>position-visibility</code>, <code>anchor-scope</code> 의 문법.
     */
    public void testOrderVisibilityAndScope() {
        String[] orders = { "normal", "most-width", "most-height", "most-block-size", "most-inline-size" };
        // 정렬 순서 키워드는 모두 올바르다.
        for (int i = 0; i < orders.length; i++) {
            Check.equal(orders[i], value("position-try-order: " + orders[i], "position-try-order"), "order " + orders[i]);
        }
        Check.equal("", value("position-try-order: most-depth", "position-try-order"), "an unknown order");
        Check.equal("", value("position-try-order: most-width most-height", "position-try-order"), "one order only");
        String[] vis = { "always", "anchors-valid", "anchors-visible", "no-overflow", "anchors-valid no-overflow", "no-overflow anchors-visible anchors-valid" };
        // 가시성 값은 always 하나이거나 세 키워드를 순서에 상관없이 한 번씩 쓴 것이다.
        for (int i = 0; i < vis.length; i++) {
            Check.equal(vis[i], value("position-visibility: " + vis[i], "position-visibility"), "visibility " + vis[i]);
        }
        Check.equal("", value("position-visibility: always no-overflow", "position-visibility"), "always stands alone");
        Check.equal("", value("position-visibility: no-overflow no-overflow", "position-visibility"), "no repetition");
        Check.equal("", value("position-visibility: hidden", "position-visibility"), "an unknown keyword");
        String[] scope = { "none", "all", "--a", "--a, --b" };
        // anchor-scope 는 none, all, 대시 이름 목록이다.
        for (int i = 0; i < scope.length; i++) {
            Check.equal(scope[i], value("anchor-scope: " + scope[i], "anchor-scope"), "scope " + scope[i]);
        }
        Check.equal("", value("anchor-scope: all, --a", "anchor-scope"), "all stands alone");
        Check.equal("", value("anchor-scope: a", "anchor-scope"), "the name must be dashed");
    }

    /**
     * The <code>position-try</code> shorthand: an optional order and the fallbacks; the order is <code>normal</code> when it is left out.
     * <p>
     * <code>position-try</code> 단축 속성: 선택적 순서와 대안. 순서를 생략하면 <code>normal</code>.
     */
    public void testPositionTryShorthand() {
        CssStyleDeclaration d = CssStyleDeclaration.parse("position-try: most-height flip-block, --a", null, new CssDiagnostics());
        Check.equal("most-height", d.getPropertyValue("position-try-order"), "the order of the shorthand");
        Check.equal("flip-block, --a", d.getPropertyValue("position-try-fallbacks"), "the fallbacks of the shorthand");
        Check.equal("most-height flip-block, --a", d.getPropertyValue("position-try"), "the shorthand serializes from its longhands");
        d = CssStyleDeclaration.parse("position-try: --a flip-x", null, new CssDiagnostics());
        Check.equal("normal", d.getPropertyValue("position-try-order"), "no order: normal");
        Check.equal("--a flip-x", d.getPropertyValue("position-try-fallbacks"), "the fallbacks");
        d = CssStyleDeclaration.parse("position-try: most-width none", null, new CssDiagnostics());
        Check.equal("most-width|none", d.getPropertyValue("position-try-order") + "|" + d.getPropertyValue("position-try-fallbacks"), "an order with none");
        d = CssStyleDeclaration.parse("position-try-order: most-width; position-try: --b", null, new CssDiagnostics());
        Check.equal("normal", d.getPropertyValue("position-try-order"), "the shorthand resets the order");
        Check.equal("", value("position-try: most-width", "position-try-fallbacks"), "an order without fallbacks is invalid");
        Check.equal("", value("position-try: most-width", "position-try-order"), "and sets nothing");
        Check.equal("", value("position-try: bogus", "position-try-fallbacks"), "an invalid value");
    }

    /**
     * <code>anchor-center</code> is accepted by the self-alignment properties (and the items properties); <code>place-self</code> sets both.
     * <p>
     * <code>anchor-center</code> 는 자기 정렬 속성(과 items 속성)이 받는다. <code>place-self</code> 는 둘 다 정한다.
     */
    public void testAnchorCenterValue() {
        Check.equal("anchor-center", value("align-self: anchor-center", "align-self"), "align-self");
        Check.equal("anchor-center", value("justify-self: anchor-center", "justify-self"), "justify-self");
        Check.equal("anchor-center", value("align-items: anchor-center", "align-items"), "align-items");
        Check.equal("anchor-center", value("justify-items: anchor-center", "justify-items"), "justify-items");
        CssStyleDeclaration d = CssStyleDeclaration.parse("place-self: anchor-center", null, new CssDiagnostics());
        Check.equal("anchor-center|anchor-center", d.getPropertyValue("align-self") + "|" + d.getPropertyValue("justify-self"), "place-self");
        Check.equal("", value("align-content: anchor-center", "align-content"), "not a content distribution value");
        Check.equal("center", value("align-self: center", "align-self"), "the other values still work");
    }

    /**
     * The ids of the new properties are consecutive and follow <code>image-orientation</code>.
     * <p>
     * 새 속성의 id 는 연속이며 <code>image-orientation</code> 다음이다.
     */
    public void testPropertyIds() {
        Check.equal(312, Prop.POSITION_TRY_FALLBACKS, "position-try-fallbacks");
        Check.equal(313, Prop.POSITION_TRY_ORDER, "position-try-order");
        Check.equal(314, Prop.POSITION_VISIBILITY, "position-visibility");
        Check.equal(315, Prop.ANCHOR_SCOPE, "anchor-scope");
        Check.isTrue(Prop.COUNT >= Prop.ANCHOR_SCOPE, "anchor-scope is a longhand id (later work adds more ids, P10-08-h: 316 and 317)");
    }

    /**
     * Parses a style sheet and returns the <code>@position-try</code> rules in it.
     * <p>
     * 스타일 시트를 파싱해 그 안의 <code>@position-try</code> 규칙을 반환한다.
     *
     * @param css the style sheet / 스타일 시트
     * @return the rules / 규칙
     */
    private static java.util.List<PositionTryRule> rules(String css) {
        StyleSheet s = StyleSheet.parse(css, "http://t.test/s.css", StyleSheet.ORIGIN_AUTHOR, null, new CssDiagnostics());
        java.util.List<PositionTryRule> out = new java.util.ArrayList<PositionTryRule>();
        List<CssRule> all = s.getRules();
        // 규칙 중 위치 대안만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof PositionTryRule) {
                out.add((PositionTryRule) all.get(i));
            }
        }
        return out;
    }

    /**
     * The <code>@position-try</code> rule: the name must be a dashed identifier; only the inset, margin, size and self-alignment properties, <code>position-anchor</code>
     * and <code>position-area</code> are kept (also through shorthands), <code>!important</code> and custom properties are dropped.
     * <p>
     * <code>@position-try</code> 규칙: 이름은 대시 식별자여야 한다. 오프셋, 마진, 크기, 자기 정렬 속성과 <code>position-anchor</code>, <code>position-area</code> 만 남기며(단축 속성도),
     * <code>!important</code> 와 사용자 정의 속성은 버린다.
     */
    public void testPositionTryRule() {
        java.util.List<PositionTryRule> r = rules("@position-try --a { top: 5px; inset-inline: 2px 3px; margin: 1px; width: 10px; max-block-size: 7px; place-self: center start;"
                + " position-anchor: --x; position-area: top left; color: red; display: none; --c: 1; height: 5px !important; align-self: bogus }");
        Check.equal(1, r.size(), "one rule");
        PositionTryRule p = r.get(0);
        Check.equal("--a", p.getName(), "the name");
        Check.equal(CssRule.POSITION_TRY_RULE, p.getType(), "the type");
        Check.equal(0, p.getCssomType(), "the CSSOM type");
        CssStyleDeclaration s = p.getStyle();
        Check.equal("5px", s.getPropertyValue("top"), "top");
        Check.equal("2px|3px", s.getPropertyValue("inset-inline-start") + "|" + s.getPropertyValue("inset-inline-end"), "an inset shorthand");
        Check.equal("1px", s.getPropertyValue("margin-left"), "margin");
        Check.equal("10px", s.getPropertyValue("width"), "width");
        Check.equal("center|start", s.getPropertyValue("align-self") + "|" + s.getPropertyValue("justify-self"), "place-self");
        Check.equal("--x|top left", s.getPropertyValue("position-anchor") + "|" + s.getPropertyValue("position-area"), "position-anchor and position-area");
        Check.equal("|||", s.getPropertyValue("color") + "|" + s.getPropertyValue("display") + "|" + s.getPropertyValue("--c") + "|" + s.getPropertyValue("height"),
                "the properties that are not accepted, the custom property and the !important declaration are dropped");
        Check.isTrue(p.getCssText().startsWith("@position-try --a { "), "the text: " + p.getCssText());
        Check.isFalse(s.setProperty("color", "blue", ""), "setProperty refuses a property that is not accepted");
        Check.isTrue(s.setProperty("left", "3px", ""), "setProperty takes a positioning property");
        Check.isFalse(s.setProperty("right", "3px", "important"), "and refuses !important");
        s.setCssText("color: red; bottom: 1px");
        Check.equal("|1px", s.getPropertyValue("color") + "|" + s.getPropertyValue("bottom"), "cssText keeps the accepted declarations only");
        Check.equal(0, rules("@position-try nodash { top: 1px }").size(), "a name that is not dashed drops the rule");
        Check.equal(0, rules("@position-try { top: 1px }").size(), "a rule without a name is dropped");
        Check.equal(0, rules("@position-try --a --b { top: 1px }").size(), "one name only");
        Check.equal(2, rules("@position-try --a { top: 1px } @position-try --a { top: 2px }").size(), "rules of one name are both kept (the last wins in the cascade)");
        Check.equal(1, rules("@position-try --n {}").size(), "an empty block is a rule");
    }
}
