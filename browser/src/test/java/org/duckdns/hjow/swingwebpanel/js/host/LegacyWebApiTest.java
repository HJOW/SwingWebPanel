package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the small and legacy window, document, node and element members of {@link LegacyWebApi} (plan item P20-01): <code>window.event</code>,
 * <code>external</code>, the window bars, the obsolete document members and colour attributes, the namespace lookups of <code>Node</code> and
 * the new reflected attributes of the obsolete IDL (HTML5 2014 REC section 11.2).
 * <p>
 * {@link LegacyWebApi} 의 작은 멤버와 과거 창, 문서, 노드, 요소 멤버의 시험(계획 P20-01): <code>window.event</code>, <code>external</code>, 창
 * 막대, 폐기된 문서 멤버와 색 속성, <code>Node</code> 의 네임스페이스 조회, 폐기된 IDL(HTML5 2014 REC 11.2절)의 새 반영 속성.
 */
public class LegacyWebApiTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyWebApiTest() {
    }

    /**
     * <code>window.event</code> is the event while a listener or a handler runs and is restored afterwards, also with nested dispatches.
     * <p>
     * <code>window.event</code> 는 리스너나 핸들러가 도는 동안 그 이벤트이고 끝나면 복원된다. 중첩 전달에서도 마찬가지이다.
     */
    public void testWindowEvent() {
        PageFixture f = new PageFixture().load("<button id=b onclick=\"window.attr = (event === window.event) + ',' + event.type\">x</button>"
                + "<div id=d></div>");
        Check.equal("undefined", f.scripts.evaluate("typeof window.event", "t"), "no event outside of a listener");
        f.scripts.evaluate("var b = document.getElementById('b'); window.log = [];"
                + "b.addEventListener('ping', function (e) { window.log.push('in:' + (window.event === e));"
                + "  var inner = new Event('pong'); document.getElementById('d').dispatchEvent(inner); window.log.push('back:' + (window.event === e)); });"
                + "document.getElementById('d').addEventListener('pong', function (e) { window.log.push('pong:' + (window.event === e)); });"
                + "b.dispatchEvent(new Event('ping'));", "t");
        Check.equal("in:true,pong:true,back:true", f.scripts.evaluate("window.log.join(',')", "t"), "nested dispatch restores the outer event");
        Check.equal("undefined", f.scripts.evaluate("typeof window.event", "t"), "restored to undefined after the dispatch");
        f.scripts.evaluate("document.getElementById('b').click()", "t");
        Check.equal("true,click", f.scripts.evaluate("window.attr", "t"), "a content attribute handler sees the same event");
        f.scripts.evaluate("window.event = 5; window.addEventListener('zz', function () { throw new Error('boom'); }); window.dispatchEvent(new Event('zz'));", "t");
        Check.equal("true", f.scripts.evaluate("String(window.event === 5)", "t"), "an error in a listener also restores the value");
    }

    /**
     * <code>external</code>, the window moving methods and the bars.
     * <p>
     * <code>external</code>, 창 이동 메서드, 막대.
     */
    public void testExternalAndBars() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("undefined|0", f.scripts.evaluate("String(window.external.AddSearchProvider('http://a/b')) + '|' + window.external.IsSearchProviderInstalled('http://a/b')",
                "t"), "external");
        Check.equal("undefined", f.scripts.evaluate("String(moveTo(1, 2) || moveBy(1, 1) || resizeTo(9, 9) || resizeBy(1, 1))", "t"), "moving does nothing");
        Check.equal("false,false,false,false,false,false", f.scripts.evaluate("[locationbar, menubar, personalbar, scrollbars, statusbar, toolbar]"
                + ".map(function (b) { return b.visible; }).join(',')", "t"), "all bars are invisible");
    }

    /**
     * Tests of <code>crypto.getRandomValues</code> and <code>crypto.randomUUID</code>.
     * <p>
     * <code>crypto.getRandomValues</code> 와 <code>crypto.randomUUID</code> 의 시험.
     */
    public void testCrypto() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("true|true|36|4|true", f.scripts.evaluate("var a = new Uint8Array(32); var r = crypto.getRandomValues(a); var u = crypto.randomUUID(); "
                + "[r === a, a.some(function (x) { return x !== 0; }), u.length, u.charAt(14), /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(u)].join('|')",
                "t"), "random bytes and a version 4 UUID");
        Check.equal("true|true", f.scripts.evaluate("var w = new Uint32Array(4); crypto.getRandomValues(w); var big = new Uint8Array(65536); crypto.getRandomValues(big); "
                + "[w.some(function (x) { return x !== 0; }), big.some(function (x) { return x !== 0; })].join('|')", "t"), "other integer types and the largest size");
        String probe = "var e = function (fn) { try { fn(); return 'none'; } catch (x) { return x.name; } };";
        Check.equal("TypeMismatchError|TypeMismatchError|QuotaExceededError|TypeError|undefined", f.scripts.evaluate(probe
                + "[e(function () { crypto.getRandomValues(new Float32Array(2)); }), e(function () { crypto.getRandomValues([1, 2]); }), "
                + "e(function () { crypto.getRandomValues(new Uint8Array(65537)); }), e(function () { crypto.getRandomValues(); }), typeof crypto.subtle].join('|')", "t"),
                "the errors, and no subtle");
    }

    /**
     * The members of the 2014 REC <code>Navigator</code>: the constants, the checks of the handler registrations and the application cache
     * object (status <code>UNCACHED</code>, <code>InvalidStateError</code> of <code>update()</code>).
     * <p>
     * 2014 REC <code>Navigator</code> 의 멤버: 상수, 핸들러 등록의 검사, 응용 프로그램 캐시 객체(상태 <code>UNCACHED</code>,
     * <code>update()</code> 의 <code>InvalidStateError</code>).
     */
    public void testNavigatorAndApplicationCache() {
        PageFixture f = new PageFixture().load("http://example.test/p/page.html", "<p>x</p>");
        Check.equal("false|false|undefined", f.scripts.evaluate("[navigator.taintEnabled(), navigator.webdriver, String(navigator.yieldForStorageUpdates())].join('|')", "t"),
                "constants");
        String ok = "http://example.test/h?u=%s";
        Check.equal("undefined|undefined|undefined", f.scripts.evaluate("[String(navigator.registerProtocolHandler('web+foo', '" + ok + "', 'T')), "
                + "String(navigator.registerProtocolHandler('mailto', '/h?u=%s', 'T')), String(navigator.registerContentHandler('application/x-foo', '" + ok
                + "', 'T'))].join('|')", "t"), "valid registrations are accepted");
        String probe = "var e = function (fn) { try { fn(); return 'none'; } catch (x) { return x.name; } };";
        Check.equal("SecurityError|SecurityError|SyntaxError|SecurityError|SecurityError|TypeError", f.scripts.evaluate(probe
                + "[e(function () { navigator.registerProtocolHandler('ftp', '" + ok + "', 'T'); }), e(function () { navigator.registerProtocolHandler('web+Foo', '"
                + ok + "', 'T'); }), e(function () { navigator.registerProtocolHandler('web+foo', 'http://example.test/h', 'T'); }), "
                + "e(function () { navigator.registerProtocolHandler('web+foo', 'http://other.test/h?%s', 'T'); }), "
                + "e(function () { navigator.registerContentHandler('text/html', '" + ok + "', 'T'); }), e(function () { navigator.registerProtocolHandler('web+a'); })].join('|')", "t"),
                "invalid registrations");
        Check.equal("0|true|[object ApplicationCache]|true|true", f.scripts.evaluate("[applicationCache.status, ApplicationCache.UNCACHED === 0, "
                + "Object.prototype.toString.call(applicationCache), applicationCache === window.applicationCache, applicationCache instanceof EventTarget].join('|')", "t"),
                "the application cache object");
        Check.equal("InvalidStateError|InvalidStateError|undefined|null", f.scripts.evaluate(probe + "[e(function () { applicationCache.update(); }), "
                + "e(function () { applicationCache.swapCache(); }), String(applicationCache.abort()), String(applicationCache.onupdateready)].join('|')", "t"),
                "no cache to update");
        PageFixture blank = new PageFixture().load("about:text", "<p>x</p>");
        Check.equal("SecurityError", blank.scripts.evaluate("try { navigator.registerProtocolHandler('web+foo', 'http://example.test/h?%s', 'T'); 'none' } catch (x) { x.name }",
                "t"), "a page without a web origin cannot register");
    }

    /**
     * The obsolete document members: <code>applets</code>, <code>clear</code>, <code>captureEvents</code>, the colour attributes.
     * <p>
     * 폐기된 문서 멤버: <code>applets</code>, <code>clear</code>, <code>captureEvents</code>, 색 속성.
     */
    public void testObsoleteDocumentMembers() {
        PageFixture f = new PageFixture().load("<body text=red bgcolor=#eee link=blue vlink=purple alink=green><applet id=a code=x></applet></body>");
        Check.equal("1,applet,true", f.scripts.evaluate("document.applets.length + ',' + document.applets[0].localName + ',' "
                + "+ (document.applets === document.applets)", "t"), "applets");
        Check.equal("true", f.scripts.evaluate("var c = document.applets; document.body.appendChild(document.createElement('applet')); String(c.length === 2)", "t"),
                "the collection is live");
        Check.equal("red|#eee|blue|purple|green", f.scripts.evaluate("[document.fgColor, document.bgColor, document.linkColor, document.vlinkColor, "
                + "document.alinkColor].join('|')", "t"), "the colour attributes read the body attributes");
        f.scripts.evaluate("document.fgColor = 'navy'; document.linkColor = null;", "t");
        Check.equal("navy||", f.scripts.evaluate("document.body.getAttribute('text') + '|' + document.body.getAttribute('link') + '|' + document.linkColor", "t"),
                "setting writes the body attribute (null as empty)");
        Check.equal("undefined", f.scripts.evaluate("String(document.clear() || document.captureEvents() || document.releaseEvents())", "t"), "no-op methods");
        PageFixture none = new PageFixture().load("<frameset></frameset>");
        Check.equal("", none.scripts.evaluate("document.fgColor", "t"), "no body gives the empty string");
    }

    /**
     * <code>lookupNamespaceURI</code>, <code>lookupPrefix</code> and <code>isDefaultNamespace</code> follow the DOM algorithms.
     * <p>
     * <code>lookupNamespaceURI</code>, <code>lookupPrefix</code>, <code>isDefaultNamespace</code> 가 DOM 알고리즘을 따른다.
     */
    public void testNamespaceLookups() {
        PageFixture f = new PageFixture().load("<div id=d></div>");
        String svg = "http://www.w3.org/2000/svg";
        String html = "http://www.w3.org/1999/xhtml";
        Check.equal(html, f.scripts.evaluate("document.documentElement.lookupNamespaceURI(null)", "t"), "the default namespace of an HTML element");
        Check.equal("null", f.scripts.evaluate("String(document.documentElement.lookupNamespaceURI('svg'))", "t"), "an unbound prefix");
        Check.equal("true,false", f.scripts.evaluate("document.documentElement.isDefaultNamespace('" + html + "') + ',' "
                + "+ document.documentElement.isDefaultNamespace('" + svg + "')", "t"), "isDefaultNamespace");
        Check.equal(svg + "|svg|null", f.scripts.evaluate("var g = document.createElementNS('" + svg + "', 'svg:g'); g.lookupNamespaceURI('svg') + '|' "
                + "+ g.lookupPrefix('" + svg + "') + '|' + g.lookupPrefix('urn:none')", "t"), "an element with a prefix");
        Check.equal("urn:foo|foo|urn:foo", f.scripts.evaluate("var x = document.createElement('div'); "
                + "x.setAttributeNS('http://www.w3.org/2000/xmlns/', 'xmlns:foo', 'urn:foo'); var c = document.createElement('span'); x.appendChild(c); "
                + "x.lookupNamespaceURI('foo') + '|' + c.lookupPrefix('urn:foo') + '|' + c.lookupNamespaceURI('foo')", "t"), "xmlns attributes are inherited");
        Check.equal("urn:d|true", f.scripts.evaluate("var y = document.createElementNS(null, 'p'); y.setAttributeNS('http://www.w3.org/2000/xmlns/', 'xmlns', 'urn:d'); "
                + "y.lookupNamespaceURI(null) + '|' + y.isDefaultNamespace('urn:d')", "t"), "an xmlns attribute without prefix is the default namespace");
        Check.equal(html + "|false", f.scripts.evaluate("var z = document.createElement('p'); z.setAttributeNS('http://www.w3.org/2000/xmlns/', 'xmlns', 'urn:d'); "
                + "z.lookupNamespaceURI(null) + '|' + z.isDefaultNamespace('urn:d')", "t"), "the namespace of the element itself comes before its xmlns attribute");
        Check.equal("null|null", f.scripts.evaluate("var t = document.createTextNode('t'); String(t.lookupNamespaceURI(null)) + '|' + String(t.lookupPrefix('x'))", "t"),
                "a detached text node");
        Check.equal(html, f.scripts.evaluate("document.lookupNamespaceURI(null)", "t"), "a document asks its root element");
    }

    /**
     * The attributes of the obsolete IDL are reflected: the table parts, the body colours, <code>br.clear</code>, <code>hr</code>, the link and
     * script attributes.
     * <p>
     * 폐기된 IDL 의 속성이 반영된다: 표의 부분, body 색, <code>br.clear</code>, <code>hr</code>, 링크와 스크립트 속성.
     */
    public void testObsoleteReflectedAttributes() {
        PageFixture f = new PageFixture().load("<table id=t align=center bgcolor=#fff cellpadding=3 cellspacing=2 frame=box rules=all width=50%>"
                + "<tr id=r align=left bgcolor=red char=. charoff=1 valign=top><td id=c align=right axis=a bgcolor=blue char=, charoff=2 height=9 nowrap valign=bottom width=7>x"
                + "</td></tr></table><a id=a coords='1,2' charset=utf-8 name=n rev=r shape=rect>a</a><br id=br clear=all>"
                + "<hr id=hr align=left color=red noshade size=3 width=20><pre id=pre width=4></pre><ul id=ul compact type=disc></ul>"
                + "<ol id=ol compact></ol><details id=dt name=g></details><iframe id=if allow='x' loading=lazy referrerpolicy=origin></iframe>");
        Check.equal("center,#fff,3,2,box,all,50%", f.scripts.evaluate("var t = document.getElementById('t'); [t.align, t.bgColor, t.cellPadding, t.cellSpacing, t.frame, "
                + "t.rules, t.width].join()", "t"), "table");
        Check.equal("left,red,.,1,top", f.scripts.evaluate("var r = document.getElementById('r'); [r.align, r.bgColor, r.ch, r.chOff, r.vAlign].join()", "t"), "row");
        Check.equal("right|a|blue|,|2|9|true|bottom|7", f.scripts.evaluate("var c = document.getElementById('c'); [c.align, c.axis, c.bgColor, c.ch, c.chOff, c.height, "
                + "c.noWrap, c.vAlign, c.width].join('|')", "t"), "cell");
        Check.equal("1,2|utf-8|n|r|rect", f.scripts.evaluate("var a = document.getElementById('a'); [a.coords, a.charset, a.name, a.rev, a.shape].join('|')", "t"), "anchor");
        Check.equal("all", f.scripts.evaluate("document.getElementById('br').clear", "t"), "br");
        Check.equal("left|red|true|3|20", f.scripts.evaluate("var h = document.getElementById('hr'); [h.align, h.color, h.noShade, h.size, h.width].join('|')", "t"), "hr");
        Check.equal("4|true|disc|true", f.scripts.evaluate("[document.getElementById('pre').width, document.getElementById('ul').compact, "
                + "document.getElementById('ul').type, document.getElementById('ol').compact].join('|')", "t"), "pre, ul, ol");
        Check.equal("g|x|lazy|origin", f.scripts.evaluate("var i = document.getElementById('if'); [document.getElementById('dt').name, i.allow, i.loading, i.referrerPolicy].join('|')",
                "t"), "details and iframe");
        Check.equal("null|null|null", f.scripts.evaluate("[document.getElementById('if').getSVGDocument(), document.createElement('embed').getSVGDocument(), "
                + "document.createElement('object').getSVGDocument()].map(String).join('|')", "t"), "getSVGDocument");
        f.scripts.evaluate("var s = document.createElement('script'); s.htmlFor = 'x'; s.event = 'onclick'; s.integrity = 'sha-1'; s.referrerPolicy = 'no-referrer'; "
                + "var l = document.createElement('link'); l.charset = 'u'; l.rev = 'r'; l.target = 't'; l.sizes = '16x16 32x32'; var m = document.createElement('meta'); "
                + "m.scheme = 'sc'; m.media = 'md'; window.r = [s.getAttribute('for'), s.getAttribute('event'), s.integrity, s.referrerPolicy, l.charset, l.rev, l.target, "
                + "l.sizes.length, l.sizes.contains('32x32'), m.getAttribute('scheme'), m.media].join('|');", "t");
        Check.equal("x|onclick|sha-1|no-referrer|u|r|t|2|true|sc|md", f.scripts.evaluate("window.r", "t"), "script, link, meta");
        Check.equal("a b|2", f.scripts.evaluate("var fm = document.createElement('form'); fm.rel = 'a b'; fm.relList.length === 2 ? fm.rel + '|' + fm.relList.length : 'bad'", "t"),
                "form.relList");
    }
}
