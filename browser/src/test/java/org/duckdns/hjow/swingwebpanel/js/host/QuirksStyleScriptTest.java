package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the quirks of the CSS values (unitless lengths, hashless hex colors) for CSS that is written by script (plan item P07-07-b-11):
 * CSS Values 4 appendix C and CSS Color 4 appendix B say that the quirky values are accepted when CSS is parsed in quirks mode, which
 * includes <code>element.style</code>, the <code>style</code> of a rule, <code>insertRule</code>, and the <code>@supports</code> rule of
 * such a sheet, but not the method <code>CSS.supports()</code>. A document in standards mode keeps the strict grammar everywhere.
 * <p>
 * 스크립트가 쓰는 CSS 의 값 quirk(단위 없는 길이, # 없는 16진수 색) 시험(계획 P07-07-b-11): CSS Values 4 부록 C 와 CSS Color 4 부록 B 는 호환 모드로 CSS 를
 * 파싱할 때 quirky 값을 받는다고 하며, <code>element.style</code>, 규칙의 <code>style</code>, <code>insertRule</code>, 그런 시트의
 * <code>@supports</code> 규칙이 포함되고 <code>CSS.supports()</code> 메서드는 제외된다. 표준 모드 문서는 어디서나 엄격한 문법을 유지한다.
 */
public class QuirksStyleScriptTest {

    /** The page that is in quirks mode (no DOCTYPE). / 호환 모드 페이지(DOCTYPE 없음). */
    private static final String QUIRKS = "<style>#r { height: 1px }</style><div id=d></div>";

    /** The page that is in standards mode. / 표준 모드 페이지. */
    private static final String STANDARDS = "<!doctype html><style>#r { height: 1px }</style><div id=d></div>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public QuirksStyleScriptTest() {
    }

    /**
     * The inline style of an element, written by script, accepts a unitless length in quirks mode and refuses it in standards mode.
     * <p>
     * 스크립트가 쓰는 요소의 인라인 스타일은 호환 모드에서 단위 없는 길이를 받고 표준 모드에서는 거부한다.
     */
    public void testElementStyleLengths() {
        PageFixture q = CssomSheetsTest.page(QUIRKS);
        q.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("100px", q.scripts.evaluate("st.width = '100'; st.width", "t"), "quirks: the property setter");
        Check.equal("5px|7px", q.scripts.evaluate("st.setProperty('margin-top', '5'); st.marginLeft = '7'; st.marginTop + '|' + st.marginLeft", "t"),
                "quirks: setProperty and another property");
        Check.equal("width: 100px; margin-top: 5px; margin-left: 7px;", q.scripts.evaluate("st.cssText", "t"), "quirks: the serialization uses px");
        Check.equal("height: 9px;", q.scripts.evaluate("st.cssText = 'height: 9'; st.cssText", "t"), "quirks: cssText");
        Check.equal("12%", q.scripts.evaluate("st.width = '12%'; st.width = '7 7'; st.width", "t"), "quirks: a refused value keeps the old one");
        PageFixture s = CssomSheetsTest.page(STANDARDS);
        s.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("", s.scripts.evaluate("st.width = '100'; st.width", "t"), "standards: a unitless length is refused");
        Check.equal("", s.scripts.evaluate("st.setProperty('margin-top', '5'); st.marginTop", "t"), "standards: setProperty refuses it too");
        Check.equal("", s.scripts.evaluate("st.cssText = 'height: 9'; st.cssText", "t"), "standards: cssText drops it");
        Check.equal("100px", s.scripts.evaluate("st.width = '100px'; st.width", "t"), "standards: lengths with units are fine");
    }

    /**
     * The hashless hex color is accepted by <code>color</code>, <code>background-color</code> and border colors in quirks mode only.
     * <p>
     * # 없는 16진수 색은 호환 모드에서만 <code>color</code>, <code>background-color</code>, border 색이 받는다.
     */
    public void testElementStyleColors() {
        PageFixture q = CssomSheetsTest.page(QUIRKS);
        q.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("rgb(255, 0, 0)", q.scripts.evaluate("st.color = 'ff0000'; st.color", "t"), "quirks: six digits");
        Check.equal("rgb(255, 0, 0)", q.scripts.evaluate("st.backgroundColor = 'f00'; st.backgroundColor", "t"), "quirks: three digits (an identifier)");
        Check.equal("rgb(0, 0, 240)", q.scripts.evaluate("st.borderTopColor = '0f0'; st.borderTopColor", "t"), "quirks: a dimension is padded to six characters (CSS Color 4 appendix B)");
        Check.equal("", q.scripts.evaluate("st.borderLeftColor = 'ffff'; st.borderLeftColor", "t"), "quirks: four digits are not a color");
        PageFixture s = CssomSheetsTest.page(STANDARDS);
        s.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("", s.scripts.evaluate("st.color = 'ff0000'; st.color", "t"), "standards: refused");
    }

    /**
     * The declaration block of a rule of a quirks mode sheet, the rules that are inserted by script and <code>@supports</code> follow the
     * mode of the sheet; <code>CSS.supports()</code> never accepts unitless lengths.
     * <p>
     * 호환 모드 시트의 규칙 선언 블록, 스크립트가 넣는 규칙, <code>@supports</code> 는 시트의 모드를 따르고 <code>CSS.supports()</code> 는 단위 없는 길이를 받지 않는다.
     */
    public void testRuleStyleInsertRuleAndSupports() {
        PageFixture q = CssomSheetsTest.page(QUIRKS);
        q.scripts.evaluate("var sheet = document.styleSheets[0]; var rs = sheet.cssRules[0].style;", "t");
        Check.equal("20px", q.scripts.evaluate("rs.width = '20'; rs.width", "t"), "quirks: the style of a rule");
        Check.equal("3px", q.scripts.evaluate("sheet.insertRule('#x { margin-top: 3 }', 1); sheet.cssRules[1].style.marginTop", "t"), "quirks: insertRule");
        Check.equal("4px", q.scripts.evaluate("sheet.cssRules[1].style.marginTop = '4'; sheet.cssRules[1].style.marginTop", "t"), "quirks: the inserted rule's style");
        Check.equal("true|false", q.scripts.evaluate("[CSS.supports('width', '10px'), CSS.supports('width', '10')].join('|')", "t"),
                "CSS.supports() does not take quirky lengths even in quirks mode");
        Check.equal("true", q.scripts.evaluate("sheet.insertRule('@supports (width: 10) { #y { margin-top: 3 } }', 2); "
                + "String(sheet.cssRules[2].cssRules.length === 1 && sheet.cssRules[2].conditionText.indexOf('10') > 0)", "t"), "the rule is kept");
        PageFixture s = CssomSheetsTest.page(STANDARDS);
        s.scripts.evaluate("var sheet = document.styleSheets[0]; var rs = sheet.cssRules[0].style;", "t");
        Check.equal("", s.scripts.evaluate("rs.width = '20'; rs.width", "t"), "standards: the style of a rule");
        Check.equal("", s.scripts.evaluate("sheet.insertRule('#x { margin-top: 3 }', 1); sheet.cssRules[1].style.marginTop", "t"), "standards: insertRule");
    }

    /**
     * <code>clip: rect()</code> takes quirky lengths in its arguments in quirks mode (the one function that CSS Values 4 appendix C lists).
     * <p>
     * 호환 모드에서 <code>clip: rect()</code> 는 인수의 quirky length 를 받는다(CSS Values 4 부록 C 가 꼽는 유일한 함수).
     */
    public void testClipRect() {
        PageFixture q = CssomSheetsTest.page(QUIRKS);
        q.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("rect(1px, 2px, 3px, 4px)", q.scripts.evaluate("st.clip = 'rect(1, 2, 3, 4)'; st.clip", "t"), "quirks: commas");
        Check.equal("rect(1px 2px 3px 4px)", q.scripts.evaluate("st.clip = 'rect(1 2 3 4)'; st.clip", "t"), "quirks: spaces");
        Check.equal("rect(1px, auto, 3px, 4px)", q.scripts.evaluate("st.clip = 'rect(1px, auto, 3, 4)'; st.clip", "t"), "quirks: mixed with auto and px");
        Check.equal("", q.scripts.evaluate("st.clip = ''; st.clip = 'rect(1, 2, 3)'; st.clip", "t"), "three arguments are invalid");
        PageFixture s = CssomSheetsTest.page(STANDARDS);
        s.scripts.evaluate("var st = document.getElementById('d').style;", "t");
        Check.equal("", s.scripts.evaluate("st.clip = 'rect(1, 2, 3, 4)'; st.clip", "t"), "standards: refused");
        Check.equal("rect(1px, 2px, 3px, 4px)", s.scripts.evaluate("st.clip = 'rect(1px, 2px, 3px, 4px)'; st.clip", "t"), "standards: with units");
    }
}
