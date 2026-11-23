package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of constructable style sheets (CSSOM, "Constructing a CSSStyleSheet", DOM-13): <code>new CSSStyleSheet(options)</code>, <code>replace()</code>,
 * <code>replaceSync()</code>, <code>document.adoptedStyleSheets</code> (the array is observable: <code>push</code> adopts at once), and the cascade
 * order of the adopted sheets after the sheets of the document.
 * <p>
 * 생성 가능한 스타일 시트의 시험(CSSOM "Constructing a CSSStyleSheet", DOM-13): <code>new CSSStyleSheet(options)</code>, <code>replace()</code>,
 * <code>replaceSync()</code>, <code>document.adoptedStyleSheets</code>(배열은 관찰 가능: <code>push</code> 가 바로 채택한다), 채택한 시트가 문서의 시트 다음에 놓이는
 * 캐스케이드 순서.
 */
public class CssomConstructableTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomConstructableTest() {
    }

    /**
     * DOM-13: a constructed sheet has no owner, no <code>href</code> and no rules, and takes <code>media</code> and <code>disabled</code> from the options.
     * <p>
     * DOM-13: 생성한 시트는 소유자, <code>href</code>, 규칙이 없고 옵션에서 <code>media</code> 와 <code>disabled</code> 를 받는다.
     */
    public void testConstructedSheetProperties() {
        PageFixture f = CssomSheetsTest.page("<p id=p>x</p>");
        Check.equal("true|true|null|null|null|0|text/css|false", f.scripts.evaluate("var s = new CSSStyleSheet(); [s instanceof CSSStyleSheet, s instanceof StyleSheet, "
                + "String(s.ownerNode), String(s.href), String(s.parentStyleSheet), s.cssRules.length, s.type, s.disabled].join('|')", "t"), "an empty sheet");
        Check.equal("print|true", f.scripts.evaluate("var o = new CSSStyleSheet({ media: 'print', disabled: true }); o.media.mediaText + '|' + o.disabled", "t"), "options");
        Check.equal("TypeError", f.scripts.evaluate("try { CSSStyleSheet() } catch (e) { e.name }", "t"), "the constructor needs new");
        Check.equal("0|1", f.scripts.evaluate("s.cssRules.length + '|' + (s.insertRule('p { color: red }'), s.cssRules.length)", "t"), "insertRule works on a constructed sheet");
        Check.equal("SyntaxError", f.scripts.evaluate("try { s.insertRule('@import url(x.css);') } catch (e) { e.name }", "t"), "@import cannot be inserted");
        Check.equal("1|p", f.scripts.evaluate("var b = new CSSStyleSheet({ baseURL: 'http://x.test/base/' }); "
                + "b.replaceSync('p { background-image: url(a.png) }'); b.cssRules.length + '|' + b.cssRules[0].selectorText", "t"), "the baseURL option is accepted");
        Check.equal("NotAllowedError", f.scripts.evaluate("try { new CSSStyleSheet({ baseURL: 'http://[' }) } catch (e) { e.name }", "t"), "an invalid baseURL");
    }

    /**
     * DOM-13: <code>replaceSync</code> and <code>replace</code> replace the rules of a constructed sheet; <code>@import</code> rules are ignored, and a
     * sheet that is not constructed refuses.
     * <p>
     * DOM-13: <code>replaceSync</code> 와 <code>replace</code> 는 생성 시트의 규칙을 바꾼다. <code>@import</code> 규칙은 무시하며 생성하지 않은 시트는 거부한다.
     */
    public void testReplace() {
        PageFixture f = CssomSheetsTest.page("<style>a { color: red }</style><p>x</p>");
        Check.equal("2|a|b", f.scripts.evaluate("var s = new CSSStyleSheet(); s.replaceSync('@import url(x.css); a { color: blue } b { margin: 0 }'); "
                + "s.cssRules.length + '|' + s.cssRules[0].selectorText + '|' + s.cssRules[1].selectorText", "t"), "replaceSync ignores @import");
        Check.equal("1|p", f.scripts.evaluate("s.replaceSync('p { color: green }'); s.cssRules.length + '|' + s.cssRules[0].selectorText", "t"), "and replaces the old rules");
        Check.equal("0", f.scripts.evaluate("s.replaceSync(''); String(s.cssRules.length)", "t"), "an empty text");
        Check.equal("NotAllowedError", f.scripts.evaluate("try { document.styleSheets[0].replaceSync('a{}') } catch (e) { e.name }", "t"), "a sheet of the document");
        Check.equal("true|1", f.scripts.evaluate("var pr = s.replace('i { color: red }'); [pr instanceof Promise, s.cssRules.length].join('|')", "t"), "replace returns a promise");
        f.scripts.evaluate("window.res = 'pending'; pr.then(function (v) { window.res = (v === s) + ':' + v.cssRules[0].selectorText; });", "t");
        Check.equal("true:i", f.scripts.evaluate("window.res", "t"), "that resolves with the sheet");
        f.scripts.evaluate("window.rej = 'pending'; document.styleSheets[0].replace('a{}').then(null, function (e) { window.rej = e.name; });", "t");
        Check.equal("NotAllowedError", f.scripts.evaluate("window.rej", "t"), "replace of a sheet of the document rejects");
        Check.equal("TypeError", f.scripts.evaluate("try { s.replaceSync() } catch (e) { e.name }", "t"), "the text is required");
    }

    /**
     * DOM-13: adopted style sheets apply after the sheets of the document, in the order of the array, and follow later changes of the array and of the sheets.
     * <p>
     * DOM-13: 채택한 스타일 시트는 문서의 시트 다음에 배열 순서로 적용되며 이후의 배열과 시트의 변경을 따른다.
     */
    public void testAdoptedStyleSheets() {
        PageFixture f = CssomSheetsTest.page("<style>#d { color: red; margin-top: 1px }</style><div id=d></div>");
        Check.equal("true|0|true", f.scripts.evaluate("var d = document.getElementById('d'); var a = new CSSStyleSheet(); a.replaceSync('#d { color: blue }'); "
                + "[Array.isArray(document.adoptedStyleSheets), document.adoptedStyleSheets.length, document.adoptedStyleSheets === document.adoptedStyleSheets].join('|')", "t"),
                "the array is the same empty array every time");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("getComputedStyle(d).color", "t"), "before adoption");
        Check.equal("1|rgb(0, 0, 255)|1px", f.scripts.evaluate("document.adoptedStyleSheets = [a]; document.adoptedStyleSheets.length + '|' + getComputedStyle(d).color + '|' "
                + "+ getComputedStyle(d).marginTop", "t"), "an adopted sheet wins over the document sheets of the same specificity");
        Check.equal("1", f.scripts.evaluate("String(document.styleSheets.length)", "t"), "adopted sheets are not in document.styleSheets");
        Check.equal("rgb(0, 128, 0)", f.scripts.evaluate("a.insertRule('#d { color: green }', 1); getComputedStyle(d).color", "t"), "a rule inserted into an adopted sheet applies");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("a.disabled = true; getComputedStyle(d).color", "t"), "a disabled adopted sheet does not apply");
        Check.equal("rgb(0, 128, 0)", f.scripts.evaluate("a.disabled = false; getComputedStyle(d).color", "t"), "and applies again");
        f.scripts.evaluate("var b = new CSSStyleSheet(); b.replaceSync('#d { color: orange }');", "t");
        Check.equal("2|rgb(255, 165, 0)", f.scripts.evaluate("document.adoptedStyleSheets.push(b); document.adoptedStyleSheets.length + '|' + getComputedStyle(d).color", "t"),
                "push adopts at once, the last sheet wins");
        Check.equal("rgb(0, 128, 0)", f.scripts.evaluate("document.adoptedStyleSheets = [b, a]; getComputedStyle(d).color", "t"), "the order of the array is the cascade order");
        Check.equal("true|true", f.scripts.evaluate("[document.adoptedStyleSheets[0] === b, document.adoptedStyleSheets[1] === a].join('|')", "t"), "the elements are the sheets");
        Check.equal("rgb(255, 165, 0)", f.scripts.evaluate("document.adoptedStyleSheets.pop(); getComputedStyle(d).color", "t"), "pop removes a sheet");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("document.adoptedStyleSheets = []; getComputedStyle(d).color", "t"), "an empty array adopts nothing");
        Check.equal("NotAllowedError", f.scripts.evaluate("try { document.adoptedStyleSheets = [document.styleSheets[0]] } catch (e) { e.name }", "t"),
                "a sheet of the document (not constructed) cannot be adopted: NotAllowedError of the CSSOM");
        Check.equal("TypeError", f.scripts.evaluate("try { document.adoptedStyleSheets = [{}] } catch (e) { e.name }", "t"), "nor can an arbitrary object");
        Check.equal("rgb(255, 165, 0)", f.scripts.evaluate("document.adoptedStyleSheets = [a, b]; document.adoptedStyleSheets.splice(0, 1); getComputedStyle(d).color", "t"),
                "splice is observed");
    }
}
