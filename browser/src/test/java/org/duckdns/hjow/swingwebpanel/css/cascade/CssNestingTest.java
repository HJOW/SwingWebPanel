package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of CSS Nesting (plan item P10-08, CSS Nesting Module, https://www.w3.org/TR/css-nesting-1/): nested style rules with and without the
 * nesting selector <code>&amp;</code>, relative selectors with a leading combinator, selector lists, the specificity of the parent, the source order of
 * the declarations around a nested rule, nested conditional group rules (<code>@media</code>, <code>@supports</code>, <code>@layer</code>) and the
 * recovery from an invalid nested rule.
 * <p>
 * CSS 중첩 시험(계획 P10-08, CSS Nesting Module, https://www.w3.org/TR/css-nesting-1/): <code>&amp;</code> 가 있거나 없는 중첩 스타일 규칙, 앞에 결합자가 있는 상대
 * 선택자, 선택자 목록, 부모의 구체성, 중첩 규칙 둘레의 선언 소스 순서, 중첩된 조건 그룹 규칙(<code>@media</code>, <code>@supports</code>, <code>@layer</code>),
 * 올바르지 않은 중첩 규칙에서의 복구.
 */
public class CssNestingTest {

    /** The color red used by the tests. 시험에서 쓰는 빨강. */
    private static final int RED = 0xFF0000;
    /** The color blue used by the tests. 시험에서 쓰는 파랑. */
    private static final int BLUE = 0x0000FF;
    /** The color green used by the tests. 시험에서 쓰는 초록. */
    private static final int GREEN = 0x008000;
    /** The default color of the body. 본문의 기본 색. */
    private static final int BLACK = 0x000000;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssNestingTest() {
    }

    /**
     * Returns the computed color of the element <code>t</code> of a page.
     * <p>
     * 페이지의 요소 <code>t</code> 의 계산된 색을 반환한다.
     *
     * @param css  the style sheet / 스타일시트
     * @param body the body markup, one element has <code>id=t</code> / 본문 마크업. 요소 하나가 <code>id=t</code> 를 가진다
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
     * A nested rule with a plain selector matches the descendants of the parent; with <code>&amp;</code> it matches what the selector says.
     * <p>
     * 일반 선택자의 중첩 규칙은 부모의 자손과 일치하고 <code>&amp;</code> 가 있으면 선택자가 말하는 대로 일치한다.
     */
    public void testBasicNesting() {
        Check.equal(BLUE, color(".a { color: red; .b { color: blue } }", "<div class=a><p id=t class=b>x</p></div>"), "a nested rule is a descendant of the parent");
        Check.equal(BLACK, color(".a { color: red; .b { color: blue } }", "<div class=b id=t>x</div>"), "the nested rule does not match outside the parent");
        Check.equal(RED, color(".a { color: red; .b { color: blue } }", "<div class=a id=t><p class=b>x</p></div>"), "the parent keeps its own declarations");
        Check.equal(BLUE, color(".a { &.c { color: blue } }", "<div id=t class='a c'>x</div>"), "&.c is the parent with another class");
        Check.equal(BLACK, color(".a { &.c { color: blue } }", "<div id=t class='a'>x</div>"), "&.c needs the class c");
        Check.equal(BLUE, color(".a { & .b { color: blue } }", "<div class=a><span id=t class=b>x</span></div>"), "& .b is a descendant");
        Check.equal(GREEN, color(".b { .a & { color: green } }", "<div class=a><span id=t class=b>x</span></div>"), ".a & puts the parent last");
        Check.equal(BLUE, color(".a { .b { .c { color: blue } } }", "<div class=a><div class=b><p id=t class=c>x</p></div></div>"), "three levels deep");
        Check.equal(BLUE, color(".a { a:hover { color: red } a { color: blue } }", "<div class=a><a id=t href=#>x</a></div>"), "a:hover is a nested rule, not a declaration");
    }

    /**
     * A leading combinator makes the nested selector relative to the parent.
     * <p>
     * 앞의 결합자는 중첩 선택자를 부모에 대한 상대 선택자로 만든다.
     */
    public void testRelativeSelectors() {
        Check.equal(BLUE, color(".a { > .b { color: blue } }", "<div class=a><p id=t class=b>x</p></div>"), "the child combinator");
        Check.equal(BLACK, color(".a { > .b { color: blue } }", "<div class=a><div><p id=t class=b>x</p></div></div>"), "not a deeper descendant");
        Check.equal(BLUE, color(".a { + .b { color: blue } }", "<div class=a></div><p id=t class=b>x</p>"), "the next-sibling combinator");
        Check.equal(BLUE, color(".a { ~ .b { color: blue } }", "<div class=a></div><i></i><p id=t class=b>x</p>"), "the subsequent-sibling combinator");
    }

    /**
     * Selector lists on either side are combined as with <code>:is()</code>.
     * <p>
     * 양쪽의 선택자 목록은 <code>:is()</code> 처럼 합쳐진다.
     */
    public void testSelectorLists() {
        String css = ".a, .x { .b, .c { color: blue } }";
        Check.equal(BLUE, color(css, "<div class=x><p id=t class=c>x</p></div>"), "x then c");
        Check.equal(BLUE, color(css, "<div class=a><p id=t class=b>x</p></div>"), "a then b");
        Check.equal(BLACK, color(css, "<div class=b><p id=t class=a>x</p></div>"), "the order matters");
        Check.equal(BLUE, color(".a { &.b, &.c { color: blue } }", "<div id=t class='a c'>x</div>"), "a list with & in each entry");
    }

    /**
     * The nested rule has the specificity of the parent (the highest of the selector list) plus its own.
     * <p>
     * 중첩 규칙의 구체성은 부모(선택자 목록 중 가장 높은 것)와 자신의 구체성의 합이다.
     */
    public void testSpecificityOfTheParent() {
        Check.equal(BLUE, color("#p { .b { color: blue } } .z .y .b { color: red }", "<div id=p class=z><div class=y><p id=t class=b>x</p></div></div>"),
                "the id of the parent counts: (1,1,0) beats (0,3,0)");
        Check.equal(RED, color(".p { .b { color: blue } } .z #w .b { color: red }", "<div class=z><div id=w class=p><p id=t class=b>x</p></div></div>"),
                "(0,2,0) loses against (1,2,0)");
        Check.equal(BLUE, color("#p, .q { .b { color: blue } } .z .y .b { color: red }", "<div id=p class=z><div class=y><p id=t class=b>x</p></div></div>"),
                "the specificity of a list parent is the highest of its entries, as with :is()");
    }

    /**
     * The declarations after a nested rule are a rule of their own that comes after it in the cascade.
     * <p>
     * 중첩 규칙 뒤의 선언은 캐스케이드에서 그 뒤에 오는 별도의 규칙이다.
     */
    public void testSourceOrder() {
        Check.equal(GREEN, color(".a { color: red; .b { color: blue } color: green }", "<div id=t class=a>x</div>"), "the later declaration of the parent wins");
        Check.equal(BLUE, color(".a { color: red; &.b { color: blue } }", "<div id=t class='a b'>x</div>"), "the nested rule comes after the declarations before it");
        Check.equal(RED, color(".a { & { color: blue } color: red }", "<div id=t class='a'>x</div>"), "a declaration after a nested rule of the same specificity overrides it");
        Check.equal(BLUE, color(".a { &.b { color: blue } color: red }", "<div id=t class='a b'>x</div>"), "a nested rule of higher specificity still wins");
        Check.equal(BLUE, color(".a { color: red; } .a { color: green; &.b { color: blue } }", "<div id=t class='a b'>x</div>"), "against another rule of the same selector");
    }

    /**
     * Conditional group rules nest in a style rule and apply their declarations to the parent.
     * <p>
     * 조건 그룹 규칙은 스타일 규칙 안에 중첩되며 그 선언을 부모에 적용한다.
     */
    public void testNestedGroups() {
        Check.equal(BLUE, color(".a { color: red; @media (min-width: 100px) { color: blue } }", "<div id=t class=a>x</div>"), "a matching @media applies its declarations");
        Check.equal(RED, color(".a { color: red; @media (min-width: 1000px) { color: blue } }", "<div id=t class=a>x</div>"), "a failing @media does not");
        Check.equal(BLUE, color(".a { color: red; @media (min-width: 100px) { .b { color: blue } } }", "<div class=a><p id=t class=b>x</p></div>"),
                "a nested rule inside the nested @media");
        Check.equal(BLUE, color(".a { color: red; @supports (display: grid) { color: blue } }", "<div id=t class=a>x</div>"), "@supports");
        Check.equal(RED, color("@layer base { .a { color: red; @layer inner { color: blue } } }", "<div id=t class=a>x</div>"),
                "@layer inside a nested rule is the sub-layer base.inner, below the rules of base itself");
        Check.equal(GREEN, color(".a { color: red; @layer inner { color: blue } } .a { color: green }", "<div id=t class=a>x</div>"),
                "a layered declaration loses against the unlayered rules, the later one of which wins");
        Check.equal(RED, color("@media (min-width: 100px) { .a { color: red; .b { color: blue } } }", "<div id=t class=a>x</div>"), "nesting inside a top level @media");
        Check.equal(BLUE, color("@media (min-width: 100px) { .a { color: red; .b { color: blue } } }", "<div class=a><p id=t class=b>x</p></div>"), "and its nested rule");
    }

    /**
     * An invalid nested rule is dropped and the rest of the parent is kept; at the top level the nesting selector is <code>:scope</code>.
     * <p>
     * 올바르지 않은 중첩 규칙은 버려지고 부모의 나머지는 유지된다. 최상위에서 중첩 선택자는 <code>:scope</code> 이다.
     */
    public void testRecovery() {
        Check.equal(RED, color(".a { color: red; ~~ { color: blue } }", "<div id=t class=a>x</div>"), "the parent survives an invalid nested selector");
        Check.equal(GREEN, color(".a { color: red; ~~ { color: blue } color: green }", "<div id=t class=a>x</div>"), "and its declarations after it");
        Check.equal(BLUE, color("& { color: blue }", "<div id=t>x</div>"), "a top level & is :scope, the root element, from which color is inherited");
        Check.equal(BLUE, color(":not(&) { color: blue }", "<div id=t>x</div>"), "&amp; inside a function");
    }
}
