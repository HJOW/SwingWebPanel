package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>document.styleSheets</code> and of the <code>StyleSheet</code>/<code>CSSStyleSheet</code> interfaces (CSSOM, DOM-13): the live list in
 * document order, the properties of a sheet, <code>insertRule</code>/<code>deleteRule</code> with the errors the CSSOM defines, the legacy
 * <code>addRule</code>/<code>removeRule</code>, <code>disabled</code> and <code>media</code>, the identity of a sheet while the document changes, the
 * <code>sheet</code> of the <code>style</code> and <code>link</code> elements, and the rule that a script reads from a sheet of another origin. The
 * tests use the real CSS engine, so a change that a script makes is checked through <code>getComputedStyle</code>.
 * <p>
 * <code>document.styleSheets</code> 와 <code>StyleSheet</code>/<code>CSSStyleSheet</code> 인터페이스의 시험(CSSOM, DOM-13): 문서 순서의 실시간 목록, 시트의 속성,
 * CSSOM 이 정한 오류가 있는 <code>insertRule</code>/<code>deleteRule</code>, 옛 <code>addRule</code>/<code>removeRule</code>, <code>disabled</code> 와 <code>media</code>,
 * 문서가 바뀌는 동안 시트의 동일성, <code>style</code> 과 <code>link</code> 요소의 <code>sheet</code>, 다른 오리진의 시트에서 스크립트가 규칙을 읽는 경우. 시험은 실제 CSS
 * 엔진을 쓰므로 스크립트가 한 변경을 <code>getComputedStyle</code> 로 확인한다.
 */
public class CssomSheetsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomSheetsTest() {
    }

    /**
     * Loads a page with the real CSS engine whose linked sheets come from a map.
     * <p>
     * 링크한 시트를 맵에서 가져오는 실제 CSS 엔진으로 페이지를 로드한다.
     *
     * @param html   the page / 페이지
     * @param sheets the linked sheets by absolute URL / 절대 URL 별 링크한 시트
     * @return the fixture / 시험 도구
     */
    static PageFixture page(String html, final Map<String, String> sheets) {
        PageFixture f = new PageFixture().load(html);
        CssEngine engine = new CssEngine(f.document, new StyleSheetLoader() {
            /**
             * Reads a sheet from the map.
             * <p>
             * 맵에서 시트를 읽는다.
             *
             * @param url the URL / URL
             * @return the sheet / 시트
             * @throws IOException if the map has none / 맵에 없으면
             */
            public LoadedStyleSheet load(String url) throws IOException {
                String t = sheets == null ? null : sheets.get(url);
                if (t == null) {
                    throw new IOException("404 " + url);
                }
                return new LoadedStyleSheet(t, url);
            }
        }, new MediaEnvironment());
        engine.loadStyleSheets();
        f.env.cssEngine = engine;
        return f;
    }

    /**
     * Loads a page without linked sheets.
     * <p>
     * 링크한 시트가 없는 페이지를 로드한다.
     *
     * @param html the page / 페이지
     * @return the fixture / 시험 도구
     */
    static PageFixture page(String html) {
        return page(html, null);
    }

    /**
     * DOM-13: <code>document.styleSheets</code> lists the style sheets in document order with the properties of the CSSOM.
     * <p>
     * DOM-13: <code>document.styleSheets</code> 는 스타일 시트를 문서 순서로 CSSOM 의 속성과 함께 나열한다.
     */
    public void testStyleSheetsListAndProperties() {
        Map<String, String> files = new HashMap<String, String>();
        files.put("http://example.test/dir/a.css", "p { color: green }");
        PageFixture f = page("<style id=s1 title=T>#d { color: red }</style><link id=l1 rel=stylesheet href=a.css media='screen, print'>"
                + "<style id=s2 media=print>p { margin: 1px }</style><div id=d></div>", files);
        Check.equal("3", f.scripts.evaluate("String(document.styleSheets.length)", "t"), "one sheet per style element and loaded link");
        Check.equal("true|true|true", f.scripts.evaluate("[document.styleSheets instanceof StyleSheetList, document.styleSheets[0] instanceof CSSStyleSheet, "
                + "document.styleSheets[0] instanceof StyleSheet].join('|')", "t"), "interfaces");
        Check.equal("text/css|null|T|true|true", f.scripts.evaluate("var a = document.styleSheets[0]; [a.type, String(a.href), a.title, "
                + "a.ownerNode === document.getElementById('s1'), a.parentStyleSheet === null].join('|')", "t"), "an inline sheet");
        Check.equal("http://example.test/dir/a.css|true|screen, print|2", f.scripts.evaluate("var b = document.styleSheets[1]; [b.href, "
                + "b.ownerNode === document.getElementById('l1'), b.media.mediaText, b.media.length].join('|')", "t"), "a linked sheet");
        Check.equal("print|false", f.scripts.evaluate("var c = document.styleSheets[2]; [c.media.mediaText, c.disabled].join('|')", "t"), "media attribute");
        Check.equal("true|true|true", f.scripts.evaluate("[document.styleSheets.item(0) === document.styleSheets[0], document.styleSheets.item(3) === null, "
                + "document.styleSheets === document.styleSheets].join('|')", "t"), "item, range and identity");
        Check.equal("true|true", f.scripts.evaluate("[document.getElementById('s1').sheet === a, document.getElementById('l1').sheet === b].join('|')", "t"),
                "style.sheet and link.sheet");
        Check.equal("true", f.scripts.evaluate("String(Array.prototype.every.call(document.styleSheets, function (s) { return s instanceof CSSStyleSheet; }))", "t"),
                "the list is array-like");
        Check.equal("", f.scripts.evaluate("document.styleSheets[0].media.mediaText", "t"), "no media attribute gives the empty media list");
    }

    /**
     * DOM-13: <code>insertRule</code> adds a rule that the style resolver applies at once, and <code>deleteRule</code> takes it away again.
     * <p>
     * DOM-13: <code>insertRule</code> 은 스타일 리졸버가 바로 적용하는 규칙을 더하고 <code>deleteRule</code> 은 다시 없앤다.
     */
    public void testInsertAndDeleteRule() {
        PageFixture f = page("<style>#d { color: red }</style><div id=d></div>");
        Check.equal("rgb(255, 0, 0)|1", f.scripts.evaluate("var d = document.getElementById('d'); var s = document.styleSheets[0]; "
                + "getComputedStyle(d).color + '|' + s.cssRules.length", "t"), "initial");
        Check.equal("1|rgb(0, 0, 255)|2", f.scripts.evaluate("var i = s.insertRule('#d { color: blue }', 1); i + '|' + getComputedStyle(d).color + '|' "
                + "+ s.cssRules.length", "t"), "a later rule wins and insertRule returns the index");
        Check.equal("0|rgb(0, 128, 0)", f.scripts.evaluate("s.insertRule('#d { color: green !important }'); s.cssRules[0].cssText.indexOf('green') >= 0 ? "
                + "'0|' + getComputedStyle(d).color : 'x'", "t"), "the index defaults to 0 and !important wins");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("s.deleteRule(0); getComputedStyle(d).color", "t"), "deleting the rule restores the older one");
        Check.equal("rgb(255, 0, 0)|1", f.scripts.evaluate("s.deleteRule(1); getComputedStyle(d).color + '|' + s.cssRules.length", "t"), "delete again");
        Check.equal("undefined", f.scripts.evaluate("String(s.deleteRule(0)); typeof s.cssRules[0]", "t"), "the list follows the deletions");
        Check.equal("0", f.scripts.evaluate("String(s.cssRules.length)", "t"), "empty");
    }

    /**
     * DOM-13: the errors of <code>insertRule</code> and <code>deleteRule</code> are <code>DOMException</code>s with the names the CSSOM defines.
     * <p>
     * DOM-13: <code>insertRule</code> 과 <code>deleteRule</code> 의 오류는 CSSOM 이 정한 이름의 <code>DOMException</code> 이다.
     */
    public void testInsertRuleErrors() {
        PageFixture f = page("<style>a { color: red }</style>");
        f.scripts.evaluate("var s = document.styleSheets[0]; function err(fn) { try { fn(); return 'none'; } catch (e) { return e.name + '|' + (e instanceof DOMException); } }", "t");
        Check.equal("SyntaxError|true", f.scripts.evaluate("err(function () { s.insertRule('not a rule') })", "t"), "text that is not a rule");
        Check.equal("SyntaxError|true", f.scripts.evaluate("err(function () { s.insertRule('a { } b { }') })", "t"), "two rules");
        Check.equal("SyntaxError|true", f.scripts.evaluate("err(function () { s.insertRule('a!b { color: red }') })", "t"), "an invalid selector");
        Check.equal("SyntaxError|true", f.scripts.evaluate("err(function () { s.insertRule('@unknown foo { }') })", "t"), "an unknown at-rule");
        Check.equal("IndexSizeError|true", f.scripts.evaluate("err(function () { s.insertRule('b { color: red }', 2) })", "t"), "an index beyond the end");
        Check.equal("IndexSizeError|true", f.scripts.evaluate("err(function () { s.insertRule('b { color: red }', -1) })", "t"), "a negative index");
        Check.equal("HierarchyRequestError|true", f.scripts.evaluate("err(function () { s.insertRule('@import url(x.css);', 1) })", "t"),
                "@import after a style rule");
        Check.equal("HierarchyRequestError|true", f.scripts.evaluate("err(function () { s.insertRule('@namespace svg url(http://www.w3.org/2000/svg);', 1) })", "t"),
                "@namespace after a style rule");
        Check.equal("InvalidStateError|true", f.scripts.evaluate("err(function () { s.insertRule('@namespace svg url(http://www.w3.org/2000/svg);', 0) })", "t"),
                "@namespace into a sheet that has other rules");
        Check.equal("IndexSizeError|true", f.scripts.evaluate("err(function () { s.deleteRule(1) })", "t"), "deleteRule beyond the end");
        Check.equal("TypeError", f.scripts.evaluate("try { s.insertRule() } catch (e) { e.name }", "t"), "the rule is required");
        Check.equal("none", f.scripts.evaluate("err(function () { s.insertRule('@media print { a { color: blue } }', 0) })", "t"), "a group rule is fine");
        Check.equal("2", f.scripts.evaluate("String(s.cssRules.length)", "t"), "the failed insertions changed nothing");
        // 가져오기 규칙은 앞에 서문 규칙만 있으면 넣을 수 있다.
        PageFixture g = page("<style>@import url(x.css);\n@layer base;\na { color: red }</style>");
        g.scripts.evaluate("var t = document.styleSheets[0]; function err2(fn) { try { fn(); return 'none'; } catch (e) { return e.name; } }", "t");
        Check.equal("none", g.scripts.evaluate("err2(function () { t.insertRule('@import url(y.css);', 1) })", "t"), "@import after an @import");
        Check.equal("HierarchyRequestError", g.scripts.evaluate("err2(function () { t.insertRule('b { color: red }', 0) })", "t"), "a style rule before an @import");
    }

    /**
     * DOM-13: the legacy <code>addRule</code> and <code>removeRule</code>, and the <code>rules</code> alias.
     * <p>
     * DOM-13: 옛 <code>addRule</code>, <code>removeRule</code> 과 <code>rules</code> 별칭.
     */
    public void testLegacyRuleMethods() {
        PageFixture f = page("<style>#d { color: red }</style><div id=d></div>");
        Check.equal("-1|2|rgb(0, 0, 255)", f.scripts.evaluate("var d = document.getElementById('d'); var s = document.styleSheets[0]; "
                + "var r = s.addRule('#d', 'color: blue'); r + '|' + s.rules.length + '|' + getComputedStyle(d).color", "t"), "addRule appends");
        Check.equal("0|true", f.scripts.evaluate("s.addRule('p', 'margin: 0', 0); s.cssRules[0].selectorText === 'p' ? '0|' + (s.rules === s.cssRules) : 'x'", "t"),
                "the index of addRule and the alias");
        Check.equal("1|rgb(255, 0, 0)", f.scripts.evaluate("s.removeRule(); s.removeRule(1); s.cssRules.length + '|' + getComputedStyle(d).color", "t"),
                "removeRule defaults to the first rule");
    }

    /**
     * DOM-13: <code>disabled</code> switches a sheet off and on, and the media list decides whether the sheet applies.
     * <p>
     * DOM-13: <code>disabled</code> 는 시트를 껐다 켜고 미디어 목록은 시트가 적용되는지를 정한다.
     */
    public void testDisabledAndMedia() {
        PageFixture f = page("<style id=s>#d { color: red }</style><div id=d></div>");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("var d = document.getElementById('d'); var s = document.styleSheets[0]; getComputedStyle(d).color", "t"),
                "applies");
        Check.equal("true|rgb(0, 0, 0)", f.scripts.evaluate("s.disabled = true; s.disabled + '|' + getComputedStyle(d).color", "t"), "disabled stops the rules");
        Check.equal("true", f.scripts.evaluate("String(document.getElementById('s').disabled)", "t"), "style.disabled follows the sheet");
        Check.equal("false|rgb(255, 0, 0)", f.scripts.evaluate("s.disabled = false; s.disabled + '|' + getComputedStyle(d).color", "t"), "enabled again");
        Check.equal("print|1|rgb(0, 0, 0)", f.scripts.evaluate("s.media.mediaText = 'print'; s.media.mediaText + '|' + s.media.length + '|' + getComputedStyle(d).color", "t"),
                "a print-only sheet does not apply on screen");
        Check.equal("print, screen|2|rgb(255, 0, 0)", f.scripts.evaluate("s.media.appendMedium('screen'); s.media.mediaText + '|' + s.media.length + '|' "
                + "+ getComputedStyle(d).color", "t"), "appendMedium");
        Check.equal("print, screen|2", f.scripts.evaluate("s.media.appendMedium('PRINT'); s.media.mediaText + '|' + s.media.length", "t"), "no duplicates");
        Check.equal("screen|screen|rgb(255, 0, 0)", f.scripts.evaluate("s.media.deleteMedium('print'); s.media.mediaText + '|' + s.media.item(0) + '|' "
                + "+ getComputedStyle(d).color", "t"), "deleteMedium");
        Check.equal("NotFoundError", f.scripts.evaluate("try { s.media.deleteMedium('tv') } catch (e) { e.name }", "t"), "deleting a missing medium");
        Check.equal("none|screen", f.scripts.evaluate("var r; try { s.media.deleteMedium('a, b'); r = 'none' } catch (e) { r = e.name } r + '|' + s.media.mediaText", "t"),
                "text that is not a media query does nothing");
        Check.equal("null|0|", f.scripts.evaluate("s.media.mediaText = ''; String(s.media.item(0)) + '|' + s.media.length + '|' + s.media.mediaText", "t"),
                "an empty media list");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("getComputedStyle(d).color", "t"), "and it matches every media");
        Check.equal("tv|[object MediaList]", f.scripts.evaluate("s.media = 'tv'; s.media.mediaText + '|' + Object.prototype.toString.call(s.media)", "t"),
                "assigning media forwards to mediaText");
        Check.equal("tv", f.scripts.evaluate("String(s.media)", "t"), "the stringifier");
    }

    /**
     * DOM-13: a sheet stays the same object (and keeps the rules that a script inserted) while other style elements come and go; a style element
     * whose text changes gets a new sheet.
     * <p>
     * DOM-13: 다른 style 요소가 오가는 동안 시트는 같은 객체로 남고(스크립트가 넣은 규칙도 유지), 텍스트가 바뀐 style 요소는 새 시트를 받는다.
     */
    public void testSheetIdentityWhileTheDocumentChanges() {
        PageFixture f = page("<style id=a>#d { color: red }</style><div id=d></div>");
        f.scripts.evaluate("var d = document.getElementById('d'); var s0 = document.styleSheets[0]; s0.insertRule('#d { color: blue }', 1);", "t");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("getComputedStyle(d).color", "t"), "the inserted rule applies");
        f.scripts.evaluate("var st = document.createElement('style'); st.id = 'b'; st.textContent = '#d { margin-top: 3px }'; document.head.appendChild(st);", "t");
        Check.equal("2|true|2", f.scripts.evaluate("document.styleSheets.length + '|' + (document.styleSheets[0] === s0) + '|' + s0.cssRules.length", "t"),
                "adding a style element keeps the first sheet and its rules");
        Check.equal("rgb(0, 0, 255)|3px", f.scripts.evaluate("getComputedStyle(d).color + '|' + getComputedStyle(d).marginTop", "t"), "both apply");
        f.scripts.evaluate("var s1 = document.styleSheets[1];", "t");
        f.scripts.evaluate("document.getElementById('a').remove();", "t");
        Check.equal("1|true", f.scripts.evaluate("document.styleSheets.length + '|' + (document.styleSheets[0] === s1)", "t"), "removing a style element");
        Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("getComputedStyle(d).color", "t"), "its rules no longer apply");
        f.scripts.evaluate("document.getElementById('b').textContent = '#d { color: green }';", "t");
        Check.equal("false|rgb(0, 128, 0)", f.scripts.evaluate("(document.styleSheets[0] === s1) + '|' + getComputedStyle(d).color", "t"),
                "new text, new sheet object");
    }

    /**
     * DOM-13: the sheet of a <code>link</code> to another origin cannot be read by a script (<code>SecurityError</code>), while its effect on the page
     * and the sheet of the same origin are not affected.
     * <p>
     * DOM-13: 다른 오리진을 가리키는 <code>link</code> 의 시트는 스크립트가 읽을 수 없지만(<code>SecurityError</code>) 페이지에 대한 효과나 같은 오리진의 시트는 영향이 없다.
     */
    public void testCrossOriginSheetIsNotReadable() {
        Map<String, String> files = new HashMap<String, String>();
        files.put("http://example.test/dir/same.css", "#d { color: red }");
        files.put("http://other.test/x.css", "#d { color: green }");
        PageFixture f = page("<link rel=stylesheet href=same.css><link rel=stylesheet href='http://other.test/x.css'><div id=d></div>", files);
        Check.equal("2|1", f.scripts.evaluate("document.styleSheets.length + '|' + document.styleSheets[0].cssRules.length", "t"), "the same origin can be read");
        Check.equal("SecurityError|true", f.scripts.evaluate("try { document.styleSheets[1].cssRules } catch (e) { e.name + '|' + (e instanceof DOMException) }", "t"),
                "cssRules of another origin");
        Check.equal("SecurityError", f.scripts.evaluate("try { document.styleSheets[1].insertRule('a{}') } catch (e) { e.name }", "t"), "insertRule of another origin");
        Check.equal("SecurityError", f.scripts.evaluate("try { document.styleSheets[1].rules } catch (e) { e.name }", "t"), "rules of another origin");
        Check.equal("http://other.test/x.css|false", f.scripts.evaluate("var o = document.styleSheets[1]; o.href + '|' + o.disabled", "t"),
                "the properties that are not rules stay readable");
        Check.equal("rgb(0, 128, 0)", f.scripts.evaluate("getComputedStyle(document.getElementById('d')).color", "t"), "the sheet still applies to the page");
        Check.equal("rgb(255, 0, 0)", f.scripts.evaluate("o.disabled = true; getComputedStyle(document.getElementById('d')).color", "t"),
                "and it can be disabled");
    }

    /**
     * DOM-13: an <code>@import</code> rule is a <code>CSSImportRule</code> with the imported sheet, which knows its owner rule and parent sheet.
     * <p>
     * DOM-13: <code>@import</code> 규칙은 가져온 시트를 가진 <code>CSSImportRule</code> 이며 가져온 시트는 자신의 소유 규칙과 부모 시트를 안다.
     */
    public void testImportedSheet() {
        Map<String, String> files = new HashMap<String, String>();
        files.put("http://example.test/dir/imp.css", "#d { color: green }");
        PageFixture f = page("<style>@import url(imp.css) screen;\n#d { margin-top: 2px }</style><div id=d></div>", files);
        Check.equal("2|3", f.scripts.evaluate("var s = document.styleSheets[0]; var r = s.cssRules[0]; s.cssRules.length + '|' + r.type", "t"), "the import rule");
        Check.equal("true|http://example.test/dir/imp.css|screen|true", f.scripts.evaluate("[r instanceof CSSImportRule, r.href, r.media.mediaText, "
                + "r.styleSheet instanceof CSSStyleSheet].join('|')", "t"), "its properties");
        Check.equal("true|true|http://example.test/dir/imp.css|1", f.scripts.evaluate("var i = r.styleSheet; [i.ownerRule === r, i.parentStyleSheet === s, "
                + "i.href, i.cssRules.length].join('|')", "t"), "the imported sheet");
        Check.equal("rgb(0, 128, 0)|2px", f.scripts.evaluate("var d = document.getElementById('d'); getComputedStyle(d).color + '|' + getComputedStyle(d).marginTop", "t"),
                "both sheets apply");
        Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("r.media.mediaText = 'print'; getComputedStyle(d).color", "t"), "the media of the import rule decides");
        Check.equal("rgb(0, 128, 0)", f.scripts.evaluate("r.media.mediaText = 'all'; getComputedStyle(d).color", "t"), "and back");
        Check.equal("rgb(0, 0, 0)", f.scripts.evaluate("s.deleteRule(0); getComputedStyle(d).color", "t"), "deleting the import rule removes its sheet");
        Check.equal("null", f.scripts.evaluate("String(i.parentStyleSheet)", "t"), "which forgets its parent");
    }
}
