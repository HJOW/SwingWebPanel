package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>CSS</code> namespace object (DOM-13): <code>CSS.supports</code> in the two forms of CSS Conditional Rules Level 3 and
 * <code>CSS.escape</code> with the cases of the CSSOM, and <code>getComputedStyle(element, pseudoElement)</code> with the pseudo-element argument.
 * <p>
 * <code>CSS</code> 네임스페이스 객체의 시험(DOM-13): CSS Conditional Rules Level 3 의 두 형태의 <code>CSS.supports</code>, CSSOM 의 사례를 가진 <code>CSS.escape</code>,
 * 의사 요소 인수가 있는 <code>getComputedStyle(element, pseudoElement)</code>.
 */
public class CssomNamespaceTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomNamespaceTest() {
    }

    /**
     * DOM-13: <code>CSS.supports(property, value)</code>.
     * DOM-13: <code>CSS.supports(property, value)</code> 의 선언 형태.
     * DOM-13: <code>CSS.supports(property, value)</code>.
     */
    public void testSupportsDeclaration() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("object|[object CSS]|function|function", f.scripts.evaluate("[typeof CSS, Object.prototype.toString.call(CSS), typeof CSS.supports, typeof CSS.escape].join('|')", "t"),
                "the namespace object");
        Check.equal("true|true|true", f.scripts.evaluate("[CSS.supports('display', 'grid'), CSS.supports('color', 'red'), CSS.supports('margin', '1px 2px')].join('|')", "t"),
                "supported declarations");
        Check.equal("false|false|false|false", f.scripts.evaluate("[CSS.supports('display', 'nonsense'), CSS.supports('no-such-property', 'x'), CSS.supports('color', ''), "
                + "CSS.supports('color', '   ')].join('|')", "t"), "unsupported declarations");
        Check.equal("true|true", f.scripts.evaluate("[CSS.supports('COLOR', 'red'), CSS.supports('--my-var', 'anything')].join('|')", "t"), "case insensitive names, custom properties");
        Check.equal("false", f.scripts.evaluate("String(CSS.supports('color', 'red !important'))", "t"), "a priority is not part of a value");
    }

    /**
     * DOM-13: <code>CSS.supports(condition)</code>, with and without the outer parentheses.
     * <p>
     * DOM-13: <code>CSS.supports(condition)</code>. 바깥 괄호가 있는 것과 없는 것.
     */
    public void testSupportsCondition() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("true|true|true", f.scripts.evaluate("[CSS.supports('(display: grid)'), CSS.supports('display: grid'), "
                + "CSS.supports('(display: grid) and (color: red)')].join('|')", "t"), "declarations and conjunctions");
        Check.equal("true|true|false", f.scripts.evaluate("[CSS.supports('(display: grid) or (foo: bar)'), CSS.supports('not (display: nonsense)'), "
                + "CSS.supports('(display: grid) and (foo: bar)')].join('|')", "t"), "or, not and a false conjunction");
        Check.equal("true|false", f.scripts.evaluate("[CSS.supports('selector(a > b)'), CSS.supports('selector(a >> )')].join('|')", "t"), "selector()");
        Check.equal("false|false", f.scripts.evaluate("[CSS.supports('nonsense'), CSS.supports('')].join('|')", "t"), "text that is no condition");
        Check.equal("TypeError", f.scripts.evaluate("try { CSS.supports() } catch (e) { e.name }", "t"), "the argument is required");
    }

    /**
     * DOM-13: <code>CSS.escape</code> follows the serialization of an identifier of the CSSOM.
     * <p>
     * DOM-13: <code>CSS.escape</code> 는 CSSOM 의 식별자 직렬화를 따른다.
     */
    public void testEscape() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("\\30 a|-\\30 |\\-|a\\ b|a\\.b\\#c|\\31 23abc", f.scripts.evaluate("[CSS.escape('0a'), CSS.escape('-0'), CSS.escape('-'), CSS.escape('a b'), "
                + "CSS.escape('a.b#c'), CSS.escape('123abc')].join('|')", "t"), "digits, hyphen, space and punctuation");
        Check.equal("||é한|a_b-c", f.scripts.evaluate("[CSS.escape(''), '', CSS.escape('é한'), CSS.escape('a_b-c')].join('|')", "t"),
                "empty, non ASCII and the characters that need no escape");
        Check.equal("\\1 |\\7f |" + (char) 0xFFFD, f.scripts.evaluate("[CSS.escape('\\u0001'), CSS.escape('\\u007f'), CSS.escape('\\u0000')].join('|')", "t"), "control characters and NUL");
        Check.equal("TypeError", f.scripts.evaluate("try { CSS.escape() } catch (e) { e.name }", "t"), "the argument is required");
        Check.equal("true", f.scripts.evaluate("String(document.querySelector('#' + CSS.escape('p1')) === null)", "t"), "a usable selector");
    }

    /**
     * DOM-13: <code>getComputedStyle(element, pseudoElement)</code> returns the style of the pseudo-element.
     * <p>
     * DOM-13: <code>getComputedStyle(element, pseudoElement)</code> 는 의사 요소의 스타일을 반환한다.
     */
    public void testComputedStyleOfPseudoElements() {
        PageFixture f = CssomSheetsTest.page("<style>#d { color: blue } #d::before { content: 'x'; color: red; display: block } #d:after { margin-top: 7px }</style><div id=d>t</div>");
        Check.equal("rgb(255, 0, 0)|block|rgb(0, 0, 255)", f.scripts.evaluate("var d = document.getElementById('d'); var b = getComputedStyle(d, '::before'); "
                + "[b.color, b.display, getComputedStyle(d).color].join('|')", "t"), "the ::before style");
        Check.equal("7px|7px", f.scripts.evaluate("[getComputedStyle(d, ':after').marginTop, getComputedStyle(d, '::after').marginTop].join('|')", "t"),
                "the single and double colon forms");
        Check.equal("rgb(0, 0, 255)|inline", f.scripts.evaluate("var m = getComputedStyle(d, '::marker'); m.color + '|' + m.display", "t"),
                "a pseudo-element no rule targets inherits from the element");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("getComputedStyle(d, null).color", "t"), "null means the element");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("getComputedStyle(d, '').color", "t"), "the empty string means the element");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("getComputedStyle(d, 'before').color", "t"), "a name without a colon is not a pseudo-element selector");
        Check.equal("|0", f.scripts.evaluate("var h = getComputedStyle(d, ':hover'); h.color + '|' + h.length", "t"), "an invalid pseudo-element gives an empty declaration");
        Check.equal("|", f.scripts.evaluate("getComputedStyle(d, '::nonsense').color + '|' + getComputedStyle(d, ':before-x').display", "t"), "unknown names too");
    }
}
