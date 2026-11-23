package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the rule objects of the CSSOM (DOM-13): <code>CSSRule</code> and its subclasses, the live <code>style</code> declaration block of a rule,
 * <code>selectorText</code>, the grouping rules with their own <code>insertRule</code>/<code>deleteRule</code>, <code>@keyframes</code> with its
 * keyframes, <code>@font-face</code>, <code>@namespace</code>, <code>@layer</code> and the kinds that have no interface of their own. A change through a
 * rule is checked through <code>getComputedStyle</code>, because the rule is the very object that the style resolver reads.
 * <p>
 * CSSOM 의 규칙 객체의 시험(DOM-13): <code>CSSRule</code> 과 그 하위 인터페이스, 규칙의 실시간 <code>style</code> 선언 블록, <code>selectorText</code>, 자신의
 * <code>insertRule</code>/<code>deleteRule</code> 이 있는 그룹 규칙, 키프레임이 있는 <code>@keyframes</code>, <code>@font-face</code>, <code>@namespace</code>,
 * <code>@layer</code>, 자신의 인터페이스가 없는 종류. 규칙은 스타일 리졸버가 읽는 바로 그 객체이므로 규칙을 통한 변경은 <code>getComputedStyle</code> 로 확인한다.
 */
public class CssomRulesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomRulesTest() {
    }

    /**
     * DOM-13: a style rule has the interface, the type, the text and the container links of the CSSOM.
     * <p>
     * DOM-13: 스타일 규칙은 CSSOM 의 인터페이스, 종류, 텍스트, 컨테이너 링크를 가진다.
     */
    public void testStyleRuleBasics() {
        PageFixture f = CssomSheetsTest.page("<style>#d, .c > p { color: red; margin: 1px 2px }</style><div id=d></div>");
        Check.equal("true|true|1|true|true", f.scripts.evaluate("var s = document.styleSheets[0]; var r = s.cssRules[0]; [r instanceof CSSStyleRule, "
                + "r instanceof CSSRule, r.type, r.parentRule === null, r.parentStyleSheet === s].join('|')", "t"), "interface, type and links");
        Check.equal("#d, .c > p", f.scripts.evaluate("r.selectorText", "t"), "selectorText");
        Check.equal("#d, .c > p { color: red; margin: 1px 2px; }", f.scripts.evaluate("r.cssText", "t"), "cssText");
        Check.equal("true|5", f.scripts.evaluate("[r.style === r.style, r.style.length].join('|')", "t"), "the style object is the same every time, length counts longhands");
        Check.equal("[object CSSStyleRule]|[object CSSRuleList]|[object CSSStyleSheet]", f.scripts.evaluate("[r, s.cssRules, s].map(function (o) { "
                + "return Object.prototype.toString.call(o); }).join('|')", "t"), "toStringTag");
        Check.equal("#d, .c > p { color: red; margin: 1px 2px; }", f.scripts.evaluate("r.cssText = 'a { color: blue }'; r.cssText", "t"), "cssText cannot be set");
        Check.equal("true", f.scripts.evaluate("String(r.style.parentRule === r)", "t"), "the declaration block knows its rule");
        Check.equal("TypeError", f.scripts.evaluate("try { Object.getOwnPropertyDescriptor(CSSRule.prototype, 'type').get.call({}) } catch (e) { e.name }", "t"),
                "illegal invocation");
        Check.equal("1|2|3|4|5|7|8|10|12", f.scripts.evaluate("[CSSRule.STYLE_RULE, CSSRule.CHARSET_RULE, CSSRule.IMPORT_RULE, CSSRule.MEDIA_RULE, "
                + "CSSRule.FONT_FACE_RULE, CSSRule.KEYFRAMES_RULE, CSSRule.KEYFRAME_RULE, CSSRule.NAMESPACE_RULE, CSSRule.SUPPORTS_RULE].join('|')", "t"),
                "the constants");
    }

    /**
     * DOM-13: the declaration block of a rule is live: a change reaches the computed style at once, in both directions.
     * <p>
     * DOM-13: 규칙의 선언 블록은 실시간이다: 변경이 계산 스타일에 바로 닿는다.
     */
    public void testRuleStyleIsLive() {
        PageFixture f = CssomSheetsTest.page("<style>#d { color: red; margin: 1px 2px }</style><div id=d></div>");
        Check.equal("rgb(255, 0, 0)|2px", f.scripts.evaluate("var d = document.getElementById('d'); var r = document.styleSheets[0].cssRules[0]; "
                + "getComputedStyle(d).color + '|' + getComputedStyle(d).marginRight", "t"), "initial");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("r.style.color = 'blue'; getComputedStyle(d).color", "t"), "an assignment through the camel-case name");
        Check.equal("rgb(0, 128, 0)|important", f.scripts.evaluate("r.style.setProperty('color', 'green', 'important'); getComputedStyle(d).color + '|' "
                + "+ r.style.getPropertyPriority('color')", "t"), "setProperty with a priority");
        Check.equal("green|important", f.scripts.evaluate("r.style.getPropertyValue('color') + '|' + r.style.getPropertyPriority('color')", "t"), "reading back");
        Check.equal("green|rgb(0, 0, 0)", f.scripts.evaluate("var old = r.style.removeProperty('color'); old + '|' + getComputedStyle(d).color", "t"),
                "removeProperty returns the old value");
        Check.equal("3px 4px|3px", f.scripts.evaluate("r.style.cssText = 'margin: 3px 4px; padding-top: 5px'; r.style.margin + '|' + getComputedStyle(d).marginTop", "t"),
                "cssText replaces the block");
        Check.equal("margin-top|5px", f.scripts.evaluate("r.style.item(0) + '|' + getComputedStyle(d).paddingTop", "t"), "items are longhands");
        Check.equal("5", f.scripts.evaluate("String(r.style.length)", "t"), "length counts longhands");
        Check.equal("3px 4px", f.scripts.evaluate("r.style.setProperty('color', 'not-a-color'); r.style.margin", "t"), "an invalid value changes nothing");
        Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("getComputedStyle(d).color", "t"), "and the style stays as it was");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("r.style = 'color: red'; getComputedStyle(d).color", "t"), "assigning style forwards to cssText");
        Check.equal("url", f.scripts.evaluate("r.style.setProperty('background-image', 'url(img.png)'); "
                + "r.style.getPropertyValue('background-image').indexOf('img.png') > 0 ? 'url' : 'no'", "t"), "a url value");
        Check.equal("[object CSSStyleDeclaration]", f.scripts.evaluate("Object.prototype.toString.call(r.style)", "t"), "the interface");
        Check.equal("true", f.scripts.evaluate("String(document.getElementById('d').style.parentRule === null)", "t"), "an element style has no parent rule");
    }

    /**
     * DOM-13: <code>selectorText</code> can be replaced; invalid text leaves the rule unchanged.
     * <p>
     * DOM-13: <code>selectorText</code> 는 바꿀 수 있고, 올바르지 않은 텍스트는 규칙을 그대로 둔다.
     */
    public void testSelectorTextSetter() {
        PageFixture f = CssomSheetsTest.page("<style>.a { color: red }</style><div id=d class=b></div>");
        Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("var d = document.getElementById('d'); var r = document.styleSheets[0].cssRules[0]; "
                + "getComputedStyle(d).color", "t"), "the selector does not match");
        Check.equal("div.b|rgb(255, 0, 0)", f.scripts.evaluate("r.selectorText = 'div.b'; r.selectorText + '|' + getComputedStyle(d).color", "t"), "a new selector");
        Check.equal("div.b", f.scripts.evaluate("r.selectorText = 'a!b'; r.selectorText", "t"), "an invalid selector is ignored");
        Check.equal("div.b", f.scripts.evaluate("r.selectorText = ''; r.selectorText", "t"), "an empty selector is invalid");
        Check.equal("#d, .zz", f.scripts.evaluate("r.selectorText = '#d,.zz'; r.selectorText", "t"), "a list is canonicalized");
    }

    /**
     * DOM-13: <code>@media</code> and <code>@supports</code> rules are grouping rules with a condition and their own rule list.
     * <p>
     * DOM-13: <code>@media</code> 와 <code>@supports</code> 규칙은 조건과 자신의 규칙 목록을 가진 그룹 규칙이다.
     */
    public void testGroupingRules() {
        PageFixture f = CssomSheetsTest.page("<style>@media print { #d { color: red } }\n@supports (display: grid) { #d { margin-top: 4px } }</style><div id=d></div>");
        Check.equal("true|true|true|4|print|1", f.scripts.evaluate("var s = document.styleSheets[0]; var m = s.cssRules[0]; [m instanceof CSSMediaRule, "
                + "m instanceof CSSConditionRule, m instanceof CSSGroupingRule, m.type, m.conditionText, m.cssRules.length].join('|')", "t"), "the media rule");
        Check.equal("print|print|true|true", f.scripts.evaluate("[m.media.mediaText, m.conditionText, m.cssRules[0] instanceof CSSStyleRule, "
                + "m.cssRules[0].parentRule === m].join('|')", "t"), "its media and rules");
        Check.equal("true|12|(display: grid)|1", f.scripts.evaluate("var u = s.cssRules[1]; [u instanceof CSSSupportsRule, u.type, u.conditionText, u.cssRules.length].join('|')", "t"),
                "the supports rule");
        Check.equal("rgb(0, 0, 0)|4px", f.scripts.evaluate("var d = document.getElementById('d'); getComputedStyle(d).color + '|' + getComputedStyle(d).marginTop", "t"),
                "the print rule does not apply, the supports rule does");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("m.media.mediaText = 'screen'; getComputedStyle(d).color", "t"), "changing the media of the rule applies it");
        Check.equal("1|rgb(0, 0, 255)|2", f.scripts.evaluate("var i = m.insertRule('#d { color: blue }', 1); i + '|' + getComputedStyle(d).color + '|' + m.cssRules.length", "t"),
                "insertRule of a group");
        Check.equal("true", f.scripts.evaluate("String(m.cssRules[1].parentRule === m && m.cssRules[1].parentStyleSheet === s)", "t"), "the container links of the new rule");
        Check.equal("rgb(255, 0, 0)|1", f.scripts.evaluate("m.deleteRule(1); getComputedStyle(d).color + '|' + m.cssRules.length", "t"), "deleteRule of a group");
        Check.equal("HierarchyRequestError", f.scripts.evaluate("try { m.insertRule('@import url(a.css);', 0) } catch (e) { e.name }", "t"), "@import cannot be nested");
        Check.equal("IndexSizeError", f.scripts.evaluate("try { m.insertRule('a { }', 5) } catch (e) { e.name }", "t"), "index of a group");
        Check.equal("IndexSizeError", f.scripts.evaluate("try { m.deleteRule(3) } catch (e) { e.name }", "t"), "delete index of a group");
        Check.equal("none", f.scripts.evaluate("try { m.insertRule('@media (min-width: 1px) { p { color: red } }', 0); 'none' } catch (e) { e.name }", "t"), "nested groups");
        Check.equal("true", f.scripts.evaluate("String(m.cssRules[0] instanceof CSSMediaRule && m.cssRules[0].parentRule === m)", "t"), "nested group is a media rule");
    }

    /**
     * DOM-13: <code>@keyframes</code> with <code>appendRule</code>, <code>deleteRule</code>, <code>findRule</code>, the renaming and the keyframe rules.
     * <p>
     * DOM-13: <code>@keyframes</code> 와 <code>appendRule</code>, <code>deleteRule</code>, <code>findRule</code>, 이름 바꾸기, 키프레임 규칙.
     */
    public void testKeyframesRules() {
        PageFixture f = CssomSheetsTest.page("<style>@keyframes spin { from { opacity: 0 } 50%, 75% { opacity: 0.5 } to { opacity: 1 } }</style>");
        Check.equal("true|7|spin|3|3", f.scripts.evaluate("var s = document.styleSheets[0]; var k = s.cssRules[0]; [k instanceof CSSKeyframesRule, k.type, k.name, "
                + "k.cssRules.length, k.length].join('|')", "t"), "the keyframes rule");
        Check.equal("true|8|0%|true|1", f.scripts.evaluate("var a = k.cssRules[0]; [a instanceof CSSKeyframeRule, a.type, a.keyText, a.parentRule === k, "
                + "a.style.opacity === '0' ? 1 : 0].join('|')", "t"), "the first keyframe");
        Check.equal("50%, 75%|100%", f.scripts.evaluate("k.cssRules[1].keyText + '|' + k.cssRules[2].keyText", "t"), "key texts are percentages");
        Check.equal("true|true", f.scripts.evaluate("[k.findRule('50%, 75%') === k.cssRules[1], k.findRule('to') === k.cssRules[2]].join('|')", "t"), "findRule");
        Check.equal("4|25%", f.scripts.evaluate("k.appendRule('25% { opacity: 0.25 }'); k.cssRules.length + '|' + k.cssRules[3].keyText", "t"), "appendRule");
        Check.equal("4", f.scripts.evaluate("k.appendRule('banana'); k.appendRule('foo { opacity: 1 }'); String(k.cssRules.length)", "t"), "invalid rules are ignored");
        Check.equal("3|null", f.scripts.evaluate("k.deleteRule('25%'); k.cssRules.length + '|' + String(k.findRule('25%'))", "t"), "deleteRule by key text");
        Check.equal("3", f.scripts.evaluate("k.deleteRule('99%'); String(k.cssRules.length)", "t"), "a missing key text is ignored");
        Check.equal("20%|0.4", f.scripts.evaluate("a.keyText = '20%'; a.style.opacity = '0.4'; a.keyText + '|' + a.style.opacity", "t"), "keyText and style are writable");
        Check.equal("SyntaxError", f.scripts.evaluate("try { a.keyText = 'nonsense' } catch (e) { e.name }", "t"), "an invalid keyText");
        Check.equal("pulse", f.scripts.evaluate("k.name = 'pulse'; k.name", "t"), "renaming");
        Check.equal("@keyframes pulse {\n  20% { opacity: 0.4; }\n  50%, 75% { opacity: 0.5; }\n  100% { opacity: 1; }\n}", f.scripts.evaluate("k.cssText", "t"),
                "the serialization follows the changes");
    }

    /**
     * DOM-13: <code>@font-face</code>, <code>@namespace</code>, <code>@layer</code> and the rule kinds without an interface of their own.
     * <p>
     * DOM-13: <code>@font-face</code>, <code>@namespace</code>, <code>@layer</code> 과 자신의 인터페이스가 없는 규칙 종류.
     */
    public void testOtherRuleKinds() {
        PageFixture f = CssomSheetsTest.page("<style>@namespace svg url(http://www.w3.org/2000/svg);\n@font-face { font-family: Foo; src: url(foo.woff) }\n"
                + "@layer base, theme;\n@layer base { p { color: red } }\n@page { margin: 1cm }\n@property --x { syntax: '<length>'; inherits: false; initial-value: 0px }</style>");
        Check.equal("6", f.scripts.evaluate("var s = document.styleSheets[0]; String(s.cssRules.length)", "t"), "all kinds are kept");
        Check.equal("true|10|svg|http://www.w3.org/2000/svg", f.scripts.evaluate("var n = s.cssRules[0]; [n instanceof CSSNamespaceRule, n.type, n.prefix, n.namespaceURI].join('|')", "t"),
                "the namespace rule");
        Check.equal("true|5|Foo", f.scripts.evaluate("var ff = s.cssRules[1]; [ff instanceof CSSFontFaceRule, ff.type, ff.style.fontFamily].join('|')", "t"), "the font face rule");
        Check.equal("true|0|base,theme", f.scripts.evaluate("var ls = s.cssRules[2]; [ls instanceof CSSLayerStatementRule, ls.type, ls.nameList].join('|')", "t"),
                "the layer statement");
        Check.equal("true|true|0|base|1", f.scripts.evaluate("var lb = s.cssRules[3]; [lb instanceof CSSLayerBlockRule, lb instanceof CSSGroupingRule, lb.type, lb.name, lb.cssRules.length].join('|')", "t"),
                "the layer block");
        Check.equal("6|true", f.scripts.evaluate("var pg = s.cssRules[4]; pg.type + '|' + (pg instanceof CSSRule)", "t"), "@page is a plain rule with the legacy type");
        Check.equal("0|true", f.scripts.evaluate("var pr = s.cssRules[5]; pr.type + '|' + (pr.cssText.indexOf('--x') > 0)", "t"), "@property is a plain rule of type 0");
        Check.equal("false", f.scripts.evaluate("String(pr instanceof CSSStyleRule)", "t"), "and not a style rule");
    }

    /**
     * DOM-13: a rule that was deleted no longer changes the page, and a rule inserted by text is attached to its sheet.
     * <p>
     * DOM-13: 지운 규칙은 더 이상 페이지를 바꾸지 않고, 텍스트로 넣은 규칙은 시트에 붙는다.
     */
    public void testDeletedRuleIsDetached() {
        PageFixture f = CssomSheetsTest.page("<style>#d { color: red }</style><div id=d></div>");
        f.scripts.evaluate("var d = document.getElementById('d'); var s = document.styleSheets[0]; var r = s.cssRules[0]; s.deleteRule(0);", "t");
        Check.equal("null|null", f.scripts.evaluate("String(r.parentStyleSheet) + '|' + String(r.parentRule)", "t"), "the links of a deleted rule are cleared");
        Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("r.style.color = 'blue'; getComputedStyle(d).color", "t"), "its style is no longer part of the page");
        Check.equal("true", f.scripts.evaluate("s.insertRule('#d { color: green }'); String(s.cssRules[0].parentStyleSheet === s)", "t"), "an inserted rule is attached");
        Check.equal("rgb(0, 128, 0)", f.scripts.evaluate("getComputedStyle(d).color", "t"), "and applies");
    }
}
