package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the methods of <code>CSSStyleDeclaration</code> against the CSSOM (section 6.7, DOM-13) for the inline style of an element and for the
 * declaration block of a rule: the case rules of the names, the priority argument, <code>null</code> arguments, shorthands, <code>cssFloat</code>, the indexed
 * access, the serialization, custom properties, and the read-only computed style.
 * <p>
 * 요소의 인라인 스타일과 규칙의 선언 블록에 대해 <code>CSSStyleDeclaration</code> 의 메서드를 CSSOM(6.7 절, DOM-13)과 대조하는 시험: 이름의 대소문자 규칙, 우선순위 인수,
 * <code>null</code> 인수, 단축 속성, <code>cssFloat</code>, 인덱스 접근, 직렬화, 사용자 정의 프로퍼티, 읽기 전용 계산 스타일.
 */
public class CssomDeclarationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomDeclarationTest() {
    }

    /**
     * DOM-13: property names are case insensitive, custom property names are case sensitive.
     * <p>
     * DOM-13: 프로퍼티 이름은 대소문자를 구분하지 않고 사용자 정의 프로퍼티 이름은 구분한다.
     */
    public void testNameCase() {
        PageFixture f = CssomSheetsTest.page("<style>#d { color: red }</style><div id=d></div>");
        f.scripts.evaluate("var st = document.getElementById('d').style; var rs = document.styleSheets[0].cssRules[0].style;", "t");
        Check.equal("red|red|red|", f.scripts.evaluate("st.setProperty('COLOR', 'red'); [st.getPropertyValue('Color'), st.color, st.getPropertyValue('color'), "
                + "st.getPropertyValue('colour')].join('|')", "t"), "an inline style: names are case insensitive");
        Check.equal("blue|blue", f.scripts.evaluate("rs.setProperty('Color', 'blue'); rs.getPropertyValue('COLOR') + '|' + rs.color", "t"), "a rule style too");
        Check.equal("x||x", f.scripts.evaluate("st.setProperty('--Foo', 'x'); [st.getPropertyValue('--Foo'), st.getPropertyValue('--foo'), "
                + "st.removeProperty('--Foo')].join('|')", "t"), "custom property names are case sensitive");
        Check.equal("", f.scripts.evaluate("st.getPropertyValue('--Foo')", "t"), "removed");
        Check.equal("y|y|1", f.scripts.evaluate("rs.setProperty('--bar', 'y'); [rs.getPropertyValue('--bar'), rs.cssText.indexOf('--bar: y') >= 0 ? 'y' : 'n', "
                + "rs.length - rs.length + 1].join('|')", "t"), "custom property in a rule");
    }

    /**
     * DOM-13: the priority argument and the <code>null</code> arguments of <code>setProperty</code>.
     * <p>
     * DOM-13: <code>setProperty</code> 의 우선순위 인수와 <code>null</code> 인수.
     */
    public void testPriorityAndNull() {
        PageFixture f = CssomSheetsTest.page("<div id=d></div>");
        f.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("important|red", f.scripts.evaluate("st.setProperty('color', 'red', 'IMPORTANT'); st.getPropertyPriority('color') + '|' + st.getPropertyValue('color')", "t"),
                "the priority is case insensitive");
        Check.equal("|green", f.scripts.evaluate("st.setProperty('color', 'green'); st.getPropertyPriority('color') + '|' + st.getPropertyValue('color')", "t"),
                "the default priority is empty");
        Check.equal("green|", f.scripts.evaluate("st.setProperty('color', 'blue', 'bogus'); st.getPropertyValue('color') + '|' + st.getPropertyPriority('color')", "t"),
                "an invalid priority leaves the declaration unchanged");
        Check.equal("|0", f.scripts.evaluate("st.setProperty('color', null); st.getPropertyValue('color') + '|' + st.length", "t"),
                "a null value removes the property");
        Check.equal("blue|", f.scripts.evaluate("st.setProperty('color', 'red', undefined); st.setProperty('color', 'blue', null); st.getPropertyValue('color') + '|' + st.getPropertyPriority('color')", "t"),
                "undefined and null priorities are empty");
        Check.equal("TypeError", f.scripts.evaluate("try { st.setProperty('color') } catch (e) { e.name }", "t"), "setProperty needs two arguments");
        Check.equal("", f.scripts.evaluate("st.color = null; st.color", "t"), "assigning null clears the property");
    }

    /**
     * DOM-13: shorthands, <code>removeProperty</code>, <code>cssFloat</code>, <code>item</code>, indexed access and <code>cssText</code>.
     * <p>
     * DOM-13: 단축 속성, <code>removeProperty</code>, <code>cssFloat</code>, <code>item</code>, 인덱스 접근, <code>cssText</code>.
     */
    public void testShorthandsAndSerialization() {
        PageFixture f = CssomSheetsTest.page("<div id=d></div>");
        f.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("1px 2px|4|1px|2px", f.scripts.evaluate("st.margin = '1px 2px'; [st.getPropertyValue('margin'), st.length, st.marginTop, st.marginRight].join('|')", "t"),
                "a shorthand is expanded and read back");
        Check.equal("", f.scripts.evaluate("st.removeProperty('margin-top'); st.getPropertyValue('margin')", "t"), "a shorthand with a missing longhand reads as empty");
        Check.equal("|0|1px 2px", f.scripts.evaluate("st.margin = '1px 2px'; var old = st.removeProperty('margin'); [st.getPropertyValue('margin-left'), st.length, old].join('|')", "t"),
                "removing a shorthand removes its longhands and returns the old text");
        Check.equal("left|left|left", f.scripts.evaluate("st.cssFloat = 'left'; [st.float, st.cssFloat, st.getPropertyValue('float')].join('|')", "t"), "cssFloat and float");
        Check.equal("float|float||", f.scripts.evaluate("[st.item(0), st[0], st.item(1), st.item(-1)].join('|')", "t"), "item, indexed access and a missing item");
        Check.equal("float: left; color: red !important;", f.scripts.evaluate("st.setProperty('color', 'red', 'important'); st.cssText", "t"),
                "cssText serializes the declarations with their priority");
        Check.equal("margin: 1px 2px; width: 3px;", f.scripts.evaluate("st.cssText = 'margin: 1px 2px; width: 3px; bogus: 1; color: not-a-color'; st.cssText", "t"),
                "cssText replaces the block and drops the invalid declarations");
        Check.equal("", f.scripts.evaluate("st.cssText = ''; st.cssText", "t"), "an empty text");
    }

    /**
     * DOM-13: a computed style is a read-only declaration with the resolved values and the custom properties.
     * <p>
     * DOM-13: 계산 스타일은 해석된 값과 사용자 정의 프로퍼티를 가진 읽기 전용 선언이다.
     */
    public void testComputedDeclaration() {
        PageFixture f = CssomSheetsTest.page("<style>#d { --Main: 12px; margin: 3px; color: blue }</style><div id=d></div>");
        Check.equal("12px||3px|rgb(0, 0, 255)", f.scripts.evaluate("var cs = getComputedStyle(document.getElementById('d')); [cs.getPropertyValue('--Main'), "
                + "cs.getPropertyValue('--main'), cs.getPropertyValue('margin'), cs.getPropertyValue('color')].join('|')", "t"),
                "values, shorthands and custom properties");
        Check.equal("true", f.scripts.evaluate("String(cs.length > 50 && typeof cs.item(0) === 'string' && cs.item(0).length > 0)", "t"), "the computed style lists the properties");
        Check.equal("", f.scripts.evaluate("cs.getPropertyPriority('color')", "t"), "no priority in a computed style");
        Check.equal("[object CSSStyleDeclaration]", f.scripts.evaluate("Object.prototype.toString.call(cs)", "t"), "the interface");
        Check.equal("true", f.scripts.evaluate("String(cs.parentRule === null)", "t"), "a computed style has no parent rule");
    }
}
