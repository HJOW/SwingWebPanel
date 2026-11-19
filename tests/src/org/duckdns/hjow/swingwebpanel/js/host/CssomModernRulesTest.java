package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the CSSOM interfaces of the newer rules (DOM-13 follow-up): <code>CSSContainerRule</code>, <code>CSSScopeRule</code>,
 * <code>CSSPropertyRule</code> and <code>CSSCounterStyleRule</code> (and its legacy <code>type</code> 11). A script reaches the very rule objects that the
 * style resolver reads, so a change of a descriptor through the interface is checked on the serialization of the rule.
 * <p>
 * 새 규칙들의 CSSOM 인터페이스 시험(DOM-13 후속): <code>CSSContainerRule</code>, <code>CSSScopeRule</code>, <code>CSSPropertyRule</code>,
 * <code>CSSCounterStyleRule</code>(과 옛 <code>type</code> 11). 스크립트는 스타일 리졸버가 읽는 바로 그 규칙 객체에 닿으므로 인터페이스로 서술자를 바꾼 결과는 규칙의
 * 직렬화로 확인한다.
 */
public class CssomModernRulesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomModernRulesTest() {
    }

    /**
     * DOM-13: an <code>@container</code> rule is a <code>CSSContainerRule</code> (a <code>CSSConditionRule</code>) with the name and the query.
     * <p>
     * DOM-13: <code>@container</code> 규칙은 이름과 질의를 가진 <code>CSSContainerRule</code>(<code>CSSConditionRule</code>)이다.
     */
    public void testContainerRule() {
        PageFixture f = CssomSheetsTest.page("<style>@container card (min-width: 400px) { .a { color: red } }\n@container (width > 100px) { .b { color: blue } }</style>");
        Check.equal("true|true|true|0|card|(min-width: 400px)|card (min-width: 400px)|1", f.scripts.evaluate("var s = document.styleSheets[0]; var r = s.cssRules[0]; "
                + "[r instanceof CSSContainerRule, r instanceof CSSConditionRule, r instanceof CSSGroupingRule, r.type, r.containerName, r.containerQuery, "
                + "r.conditionText, r.cssRules.length].join('|')", "t"), "a named container rule");
        Check.equal("true||(width > 100px)", f.scripts.evaluate("var u = s.cssRules[1]; [u instanceof CSSContainerRule, u.containerName, u.containerQuery].join('|')", "t"),
                "an unnamed container rule has the empty name");
        Check.equal("true|true", f.scripts.evaluate("[r.cssRules[0] instanceof CSSStyleRule, r.cssRules[0].parentRule === r].join('|')", "t"), "its rules");
        Check.equal("[object CSSContainerRule]", f.scripts.evaluate("Object.prototype.toString.call(r)", "t"), "the toStringTag");
        Check.equal("1|2", f.scripts.evaluate("var i = r.insertRule('.c { color: green }', 1); i + '|' + r.cssRules.length", "t"), "insertRule of the grouping rule");
    }

    /**
     * DOM-13: an <code>@scope</code> rule is a <code>CSSScopeRule</code> whose <code>start</code> and <code>end</code> are the selector texts or null.
     * <p>
     * DOM-13: <code>@scope</code> 규칙은 <code>start</code> 와 <code>end</code> 가 선택자 문자열(없으면 null)인 <code>CSSScopeRule</code> 이다.
     */
    public void testScopeRule() {
        PageFixture f = CssomSheetsTest.page("<style>@scope (.card) to (.content) { img { border: 1px solid } }\n@scope (.only) { p { color: red } }\n"
                + "@scope { span { color: blue } }</style>");
        Check.equal("true|true|0|.card|.content|1", f.scripts.evaluate("var s = document.styleSheets[0]; var a = s.cssRules[0]; [a instanceof CSSScopeRule, "
                + "a instanceof CSSGroupingRule, a.type, a.start, a.end, a.cssRules.length].join('|')", "t"), "start and end");
        Check.equal(".only|null", f.scripts.evaluate("var b = s.cssRules[1]; b.start + '|' + b.end", "t"), "no end selector");
        Check.equal("null|null|true", f.scripts.evaluate("var c = s.cssRules[2]; c.start + '|' + c.end + '|' + (c instanceof CSSScopeRule)", "t"), "a scope without a prelude");
        Check.equal("[object CSSScopeRule]", f.scripts.evaluate("Object.prototype.toString.call(a)", "t"), "the toStringTag");
    }

    /**
     * DOM-13: an <code>@starting-style</code> rule is a <code>CSSStartingStyleRule</code>, a grouping rule that holds the starting style rules.
     * <p>
     * DOM-13: <code>@starting-style</code> 규칙은 시작 스타일 규칙을 담은 그룹 규칙 <code>CSSStartingStyleRule</code> 이다.
     */
    public void testStartingStyleRule() {
        PageFixture f = CssomSheetsTest.page("<style>@starting-style { #a { opacity: 0 } }</style><div id=a></div>");
        Check.equal("true|true|0|1", f.scripts.evaluate("var s = document.styleSheets[0]; var r = s.cssRules[0]; [r instanceof CSSStartingStyleRule, "
                + "r instanceof CSSGroupingRule, r.type, r.cssRules.length].join('|')", "t"), "the rule and its contents");
        Check.equal("[object CSSStartingStyleRule]", f.scripts.evaluate("Object.prototype.toString.call(r)", "t"), "the toStringTag");
        Check.equal("true", f.scripts.evaluate("String(r.cssRules[0] instanceof CSSStyleRule && r.cssRules[0].selectorText == '#a')", "t"), "a style rule inside");
        Check.equal("1", f.scripts.evaluate("r.insertRule('#b { opacity: 0.5 }', 1); String(r.cssRules.length - 1)", "t"), "insertRule of the grouping rule");
    }

    /**
     * DOM-13: an <code>@property</code> rule is a <code>CSSPropertyRule</code> with <code>name</code>, <code>syntax</code>, <code>inherits</code>
     * and <code>initialValue</code>.
     * <p>
     * DOM-13: <code>@property</code> 규칙은 <code>name</code>, <code>syntax</code>, <code>inherits</code>, <code>initialValue</code> 를 가진
     * <code>CSSPropertyRule</code> 이다.
     */
    public void testPropertyRule() {
        PageFixture f = CssomSheetsTest.page("<style>@property --x { syntax: '<length>'; inherits: false; initial-value: 0px }\n"
                + "@property --y { syntax: '*'; inherits: true }</style>");
        Check.equal("true|0|--x|<length>|false|0px", f.scripts.evaluate("var s = document.styleSheets[0]; var p = s.cssRules[0]; [p instanceof CSSPropertyRule, "
                + "p.type, p.name, p.syntax, p.inherits, p.initialValue].join('|')", "t"), "a registered property");
        Check.equal("--y|*|true|null", f.scripts.evaluate("var q = s.cssRules[1]; [q.name, q.syntax, q.inherits, String(q.initialValue)].join('|')", "t"),
                "a universal property has no initial value");
        Check.equal("[object CSSPropertyRule]", f.scripts.evaluate("Object.prototype.toString.call(p)", "t"), "the toStringTag");
        Check.equal("false|true", f.scripts.evaluate("[p instanceof CSSStyleRule, p instanceof CSSRule].join('|')", "t"), "it is a CSSRule only");
    }

    /**
     * DOM-13: an <code>@counter-style</code> rule is a <code>CSSCounterStyleRule</code> of type 11 whose descriptors can be read and written.
     * <p>
     * DOM-13: <code>@counter-style</code> 규칙은 서술자를 읽고 쓸 수 있는 종류 11 의 <code>CSSCounterStyleRule</code> 이다.
     */
    public void testCounterStyleRule() {
        PageFixture f = CssomSheetsTest.page("<style>@counter-style thumbs { system: cyclic; symbols: '\\1F44D'; suffix: ' ' }</style>");
        Check.equal("true|11|thumbs|cyclic|", f.scripts.evaluate("var s = document.styleSheets[0]; var c = s.cssRules[0]; [c instanceof CSSCounterStyleRule, "
                + "c.type, c.name, c.system, c.prefix].join('|')", "t"), "the rule and its descriptors");
        Check.equal("\" \"|", f.scripts.evaluate("[c.suffix, c.range].join('|')", "t"), "a written (a string is serialized with double quotes) and an unwritten descriptor");
        Check.equal("11", f.scripts.evaluate("String(CSSRule.COUNTER_STYLE_RULE)", "t"), "the constant");
        Check.equal("\"-\"|true", f.scripts.evaluate("c.suffix = '\"-\"'; [c.suffix, c.cssText.indexOf('suffix: \"-\"') > 0].join('|')", "t"), "a setter replaces the descriptor");
        Check.equal("|true", f.scripts.evaluate("c.suffix = ''; [c.suffix, c.cssText.indexOf('suffix') < 0].join('|')", "t"), "an empty text removes it");
        Check.equal("pointers", f.scripts.evaluate("c.name = 'pointers'; c.name", "t"), "the name can be changed");
        Check.equal("pointers", f.scripts.evaluate("c.name = 'none'; c.name = 'decimal'; c.name = 'a b'; c.name", "t"), "a name that cannot be defined is ignored");
        Check.equal("[object CSSCounterStyleRule]", f.scripts.evaluate("Object.prototype.toString.call(c)", "t"), "the toStringTag");
    }

    /**
     * DOM-13: a <code>@position-try</code> rule (CSS Anchor Positioning 1 section 8.1) is a <code>CSSPositionTryRule</code> of type 0 with a <code>name</code> and a
     * <code>style</code> block that takes only the positioning properties.
     * <p>
     * DOM-13: <code>@position-try</code> 규칙(CSS Anchor Positioning 1 8.1 절)은 종류 0 의 <code>CSSPositionTryRule</code> 이며 <code>name</code> 과 위치 속성만 받는 <code>style</code> 블록을
     * 가진다.
     */
    public void testPositionTryRule() {
        PageFixture f = CssomSheetsTest.page("<style>@position-try --flip { top: 5px; margin: 1px 2px; color: red; width: 9px !important; position-area: bottom }</style>");
        Check.equal("true|0|--flip|5px|1px 2px", f.scripts.evaluate("var s = document.styleSheets[0]; var r = s.cssRules[0]; [r instanceof CSSPositionTryRule, r.type, r.name, "
                + "r.style.top, r.style.margin].join('|')", "t"), "the rule, its name and the accepted declarations");
        Check.equal("|||bottom", f.scripts.evaluate("[r.style.color, r.style.width, r.style.getPropertyValue('color'), r.style.positionArea].join('|')", "t"),
                "a property that is not accepted and !important are dropped");
        Check.equal("true|true", f.scripts.evaluate("[r.style.parentRule === r, r.cssText.indexOf('@position-try --flip {') == 0].join('|')", "t"), "the block knows its rule, the text");
        Check.equal("3px|", f.scripts.evaluate("r.style.setProperty('left', '3px'); r.style.setProperty('color', 'blue'); [r.style.left, r.style.color].join('|')", "t"),
                "setProperty takes a positioning property and refuses the others");
        Check.equal("[object CSSPositionTryRule]", f.scripts.evaluate("Object.prototype.toString.call(r)", "t"), "the toStringTag");
        PageFixture g = CssomSheetsTest.page("<style>@position-try nodash { top: 1px } @position-try --ok { top: 1px }</style>");
        Check.equal("1|--ok", g.scripts.evaluate("var c = document.styleSheets[0].cssRules; [c.length, c[0].name].join('|')", "t"), "a name that is not a dashed identifier drops the rule");
    }

    /**
     * DOM-13: an <code>@page</code> rule is a <code>CSSPageRule</code> with <code>selectorText</code>, <code>style</code> and the margin rules as
     * <code>CSSMarginRule</code>s.
     * <p>
     * DOM-13: <code>@page</code> 규칙은 <code>selectorText</code>, <code>style</code>, 그리고 <code>CSSMarginRule</code> 인 여백 규칙을 가진 <code>CSSPageRule</code> 이다.
     */
    public void testPageRule() {
        PageFixture f = CssomSheetsTest.page("<style>@page :first { margin: 2cm; @top-center { color: red } }</style>");
        Check.equal("true|6|:first|2cm|1", f.scripts.evaluate("var s = document.styleSheets[0]; var p = s.cssRules[0]; [p instanceof CSSPageRule, p.type, "
                + "p.selectorText, p.style.marginTop, p.cssRules.length].join('|')", "t"), "the rule and its declarations");
        Check.equal("true|9|top-center|red", f.scripts.evaluate("var m = p.cssRules[0]; [m instanceof CSSMarginRule, m.type, m.name, m.style.color].join('|')", "t"),
                "the margin rule");
        Check.equal(":left|true", f.scripts.evaluate("p.selectorText = ':left'; [p.selectorText, p.cssText.indexOf('@page :left') == 0].join('|')", "t"),
                "the selector text can be replaced");
        Check.equal("[object CSSPageRule]", f.scripts.evaluate("Object.prototype.toString.call(p)", "t"), "the toStringTag");
    }
}
