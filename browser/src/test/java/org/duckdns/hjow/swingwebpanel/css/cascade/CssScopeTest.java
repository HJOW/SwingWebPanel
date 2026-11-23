package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>@scope</code> (plan item P10-08, CSS Cascading and Inheritance Level 6, https://www.w3.org/TR/css-cascade-6/, section 2): the scoping
 * roots, the rules that only apply below a root, <code>:scope</code> and <code>&amp;</code> for the root itself, the scope limit (<code>to (...)</code>)
 * with <code>:scope</code> inside it, the scoping proximity that lets the nearer root win over the source order, the implicit root without a prelude, and
 * the recovery from an invalid prelude.
 * <p>
 * <code>@scope</code> 시험(계획 P10-08, CSS Cascading and Inheritance Level 6, https://www.w3.org/TR/css-cascade-6/, 2 절): 범위 루트, 루트 아래에서만 적용되는 규칙, 루트
 * 자신을 가리키는 <code>:scope</code> 와 <code>&amp;</code>, 범위 한계(<code>to (...)</code>)와 그 안의 <code>:scope</code>, 소스 순서보다 가까운 루트가 이기는 범위
 * 근접도, 전문 없는 암시적 루트, 올바르지 않은 전문에서의 복구.
 */
public class CssScopeTest {

    /** The color red used by the tests. 시험에서 쓰는 빨강. */
    private static final int RED = 0xFF0000;
    /** The color blue used by the tests. 시험에서 쓰는 파랑. */
    private static final int BLUE = 0x0000FF;
    /** The color green used by the tests. 시험에서 쓰는 초록. */
    private static final int GREEN = 0x008000;
    /** The default color. 기본 색. */
    private static final int BLACK = 0x000000;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssScopeTest() {
    }

    /**
     * Returns the computed color of the element <code>t</code> of a page.
     * <p>
     * 페이지의 요소 <code>t</code> 의 계산된 색을 반환한다.
     *
     * @param css  the style sheet / 스타일시트
     * @param body the body markup / 본문 마크업
     * @return the color as 0xRRGGBB / 0xRRGGBB 색
     */
    private static int color(String css, String body) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + body + "</body></html>",
                "http://t.test/page.html", null, true);
        CssEngine engine = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        engine.loadStyleSheets();
        return engine.getComputedStyle(doc.getElementById("t")).color(Prop.COLOR) & 0xFFFFFF;
    }

    /**
     * Rules in <code>@scope (.card)</code> apply to the descendants of the root, not to the root itself and not outside.
     * <p>
     * <code>@scope (.card)</code> 안의 규칙은 루트의 자손에 적용되며 루트 자신과 바깥에는 적용되지 않는다.
     */
    public void testBelowTheRoot() {
        String css = "@scope (.card) { p { color: blue } }";
        Check.equal(BLUE, color(css, "<div class=card><p id=t>x</p></div>"), "a child of the root");
        Check.equal(BLUE, color(css, "<div class=card><div><p id=t>x</p></div></div>"), "a deeper descendant");
        Check.equal(BLACK, color(css, "<div><p id=t>x</p></div>"), "outside every root");
        Check.equal(BLACK, color("@scope (.card) { div { color: blue } }", "<div class=card id=t>x</div>"), "the root itself is not below the root");
    }

    /**
     * <code>:scope</code> and <code>&amp;</code> name the root itself; a selector that names it may also match the root.
     * <p>
     * <code>:scope</code> 와 <code>&amp;</code> 는 루트 자신을 가리킨다. 그것을 쓴 선택자는 루트와도 일치할 수 있다.
     */
    public void testScopeSelector() {
        Check.equal(BLUE, color("@scope (.card) { :scope { color: blue } }", "<div class=card id=t>x</div>"), ":scope is the root");
        Check.equal(BLUE, color("@scope (.card) { & { color: blue } }", "<div class=card id=t>x</div>"), "& is the root");
        Check.equal(BLUE, color("@scope (.card) { :scope > p { color: blue } }", "<div class=card><p id=t>x</p></div>"), ":scope > p");
        Check.equal(BLACK, color("@scope (.card) { :scope > p { color: blue } }", "<div class=card><div><p id=t>x</p></div></div>"), "p is not a child of the root");
        Check.equal(BLUE, color("@scope (.card) { .a & { color: blue } }", "<div class=a><div class=card id=t>x</div></div>"), "a selector with the root after an ancestor");
        Check.equal(RED, color(".card { color: red } @scope (.card) { & { color: blue } }", "<div class=card id=t>x</div>"),
                "& in @scope is :where(:scope) with zero specificity (CSS Nesting 3.3), so the class selector wins");
        Check.equal(BLUE, color(".card { color: red } @scope (.card) { :scope { color: blue } }", "<div class=card id=t>x</div>"),
                "an explicit :scope has the specificity of a pseudo-class, equal to the class: the scoped rule wins by proximity");
    }

    /**
     * The limit <code>to (...)</code> removes the elements that match it and their descendants from the scope; <code>:scope</code> in the
     * limit is the root.
     * <p>
     * 한계 <code>to (...)</code> 는 그것과 일치하는 요소와 그 자손을 범위에서 뺀다. 한계 안의 <code>:scope</code> 는 루트이다.
     */
    public void testScopeLimit() {
        String css = "@scope (.card) to (.foot) { p { color: blue } }";
        Check.equal(BLUE, color(css, "<div class=card><p id=t>x</p></div>"), "above the limit");
        Check.equal(BLACK, color(css, "<div class=card><div class=foot><p id=t>x</p></div></div>"), "below the limit element");
        Check.equal(BLUE, color(css, "<div class=card><div class=foot></div><p id=t>x</p></div>"), "a sibling of the limit is in the scope");
        Check.equal(BLACK, color("@scope (.card) to (.foot) { div { color: blue } }", "<div class=card><div class=foot id=t>x</div></div>"),
                "the limit element itself is out");
        Check.equal(BLACK, color("@scope (.card) to (:scope > .foot) { p { color: blue } }", "<div class=card><div class=foot><p id=t>x</p></div></div>"),
                ":scope in the limit is the root: a child .foot of the root limits");
        Check.equal(BLUE, color("@scope (.card) to (:scope > .foot) { p { color: blue } }", "<div class=card><div><div class=foot><p id=t>x</p></div></div></div>"),
                "a deeper .foot is not a child of the root, so it does not limit");
        Check.equal(BLUE, color("@scope (.card) to (.foot) { p { color: blue } }", "<div class=foot><div class=card><p id=t>x</p></div></div>"),
                "a limit above the root does not matter");
    }

    /**
     * With equal specificity the rule whose root is nearer to the element wins, even if it comes earlier in the source.
     * <p>
     * 구체성이 같으면 소스에서 앞서 있어도 루트가 요소에 더 가까운 규칙이 이긴다.
     */
    public void testProximity() {
        String css = "@scope (.near) { p { color: blue } } @scope (.far) { p { color: red } }";
        Check.equal(BLUE, color(css, "<div class=far><div class=near><p id=t>x</p></div></div>"), "the nearer root .near wins over the later rule");
        Check.equal(RED, color(css, "<div class=near><div class=far><p id=t>x</p></div></div>"), "now .far is nearer");
        Check.equal(BLUE, color("@scope (.a) { p { color: blue } } p { color: red }", "<div class=a><p id=t>x</p></div>"),
                "a scoped rule beats an unscoped rule of the same specificity that comes later");
        Check.equal(RED, color("@scope (.a) { p { color: blue } } .a p { color: red }", "<div class=a><p id=t>x</p></div>"), "but not one of higher specificity");
        Check.equal(RED, color("@scope (.a) { p { color: blue } p { color: red } }", "<div class=a><p id=t>x</p></div>"), "ordinary order inside a scope");
    }

    /**
     * A scope without a start selector has the root element as its root, and nests with the cascade layers and conditions.
     * <p>
     * 시작 선택자가 없는 범위는 루트 요소를 루트로 가지며 캐스케이드 레이어, 조건과 중첩된다.
     */
    public void testImplicitRootAndNesting() {
        Check.equal(BLUE, color("@scope { p { color: blue } }", "<div><p id=t>x</p></div>"), "no prelude: below the root element");
        Check.equal(BLUE, color("@media (min-width: 100px) { @scope (.a) { p { color: blue } } }", "<div class=a><p id=t>x</p></div>"), "@scope inside @media");
        Check.equal(BLUE, color("@scope (.a) { @media (min-width: 100px) { p { color: blue } } }", "<div class=a><p id=t>x</p></div>"), "@media inside @scope");
        Check.equal(GREEN, color("@scope (.a) { p { color: blue } } @scope (.a) { p { color: green } }", "<div class=a><p id=t>x</p></div>"), "later scope, same proximity");
        Check.equal(BLUE, color("@scope (.a) { @scope (.b) { p { color: blue } } }", "<div class=a><div class=b><p id=t>x</p></div></div>"), "nested scopes");
        Check.equal(BLACK, color("@scope (.a) { @scope (.b) { p { color: blue } } }", "<div class=b><div class=a><p id=t>x</p></div></div>"),
                "the root of an inner @scope must be below the root of the outer one");
    }

    /**
     * An invalid prelude drops the rule and the rest of the sheet is kept.
     * <p>
     * 올바르지 않은 전문은 규칙을 버리고 시트의 나머지는 유지된다.
     */
    public void testRecovery() {
        Check.equal(RED, color("@scope (~~) { p { color: blue } } p { color: red }", "<div class=a><p id=t>x</p></div>"), "an invalid selector");
        Check.equal(RED, color("@scope (.a) junk { p { color: blue } } p { color: red }", "<div class=a><p id=t>x</p></div>"), "trailing junk");
    }
}
