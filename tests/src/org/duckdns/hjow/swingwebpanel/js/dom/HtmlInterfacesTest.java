package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the element interfaces of {@link HtmlMoreInterfaces} (plan item P20-01): every tag gets the interface of the specification, the
 * interface has the chain <code>HTMLElement</code>, <code>Element</code>, <code>Node</code>, and the reflecting attributes read and write the
 * content attributes with the right conversion (string, URL against the document, boolean, integer, unsigned integer).
 * <p>
 * {@link HtmlMoreInterfaces} 의 요소 인터페이스 시험(계획 P20-01): 모든 태그가 규격의 인터페이스를 받고, 인터페이스는
 * <code>HTMLElement</code>, <code>Element</code>, <code>Node</code> 사슬을 가지며, 반영 속성이 콘텐츠 속성을 올바른 변환(문자열, 문서 기준 URL, 불리언, 정수,
 * 부호 없는 정수)으로 읽고 쓴다.
 */
public class HtmlInterfacesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HtmlInterfacesTest() {
    }

    /**
     * Every tag of the table gets its interface, and the interface objects form the prototype chain.
     * <p>
     * 표의 모든 태그가 자신의 인터페이스를 받고 인터페이스 객체가 프로토타입 사슬을 이룬다.
     */
    public void testTagsGetTheirInterfaces() {
        DomTestEnv e = new DomTestEnv("<!doctype html><body></body>");
        String[][] expected = { { "dl", "HTMLDListElement" }, { "data", "HTMLDataElement" }, { "time", "HTMLTimeElement" },
            { "ins", "HTMLModElement" }, { "del", "HTMLModElement" }, { "details", "HTMLDetailsElement" }, { "dialog", "HTMLDialogElement" },
            { "map", "HTMLMapElement" }, { "area", "HTMLAreaElement" }, { "param", "HTMLParamElement" }, { "embed", "HTMLEmbedElement" },
            { "object", "HTMLObjectElement" }, { "track", "HTMLTrackElement" }, { "menu", "HTMLMenuElement" },
            { "menuitem", "HTMLMenuItemElement" }, { "keygen", "HTMLKeygenElement" }, { "picture", "HTMLPictureElement" },
            { "caption", "HTMLTableCaptionElement" }, { "col", "HTMLTableColElement" }, { "colgroup", "HTMLTableColElement" },
            { "thead", "HTMLTableSectionElement" }, { "tbody", "HTMLTableSectionElement" }, { "tfoot", "HTMLTableSectionElement" },
            { "font", "HTMLFontElement" }, { "basefont", "HTMLBaseFontElement" }, { "dir", "HTMLDirectoryElement" },
            { "applet", "HTMLAppletElement" }, { "marquee", "HTMLMarqueeElement" } };
        // 표의 태그마다 만든 요소의 인터페이스 이름과 프로토타입 사슬을 확인한다.
        for (int i = 0; i < expected.length; i++) {
            String tag = expected[i][0];
            String name = expected[i][1];
            Check.equal("[object " + name + "]", e.eval("Object.prototype.toString.call(document.createElement('" + tag + "'))"), tag);
            Check.equal("true", e.eval("var el = document.createElement('" + tag + "'); el instanceof " + name
                    + " && el instanceof HTMLElement && el instanceof Element && el instanceof Node"), "chain of " + tag);
            Check.equal("true", e.eval("Object.getPrototypeOf(" + name + ") === HTMLElement"), "interface object of " + name);
        }
        Check.equal("[object HTMLUnknownElement]", e.eval("Object.prototype.toString.call(document.createElement('bgsound'))"),
                "an obsolete element without interface stays unknown");
    }

    /**
     * The markup of a page gives the same interfaces (the parser creates the elements, not only <code>createElement</code>).
     * <p>
     * 페이지의 마크업도 같은 인터페이스를 준다(<code>createElement</code> 뿐 아니라 파서가 만든 요소도).
     */
    public void testParsedElementsGetTheirInterfaces() {
        DomTestEnv e = new DomTestEnv("<!doctype html><body><dl id=a><dt>t<dd>d</dl><details id=b open><summary>s</summary>x</details>"
                + "<map id=c name=m><area id=d href=x.html alt=A shape=rect coords='0,0,1,1'></map><time id=f datetime=2014-10-28>t</time>"
                + "<table><caption id=g>c</caption><colgroup id=h span=2><col id=i></colgroup><thead id=j></thead><tbody id=k></tbody></table></body>");
        String[][] expected = { { "a", "HTMLDListElement" }, { "b", "HTMLDetailsElement" }, { "c", "HTMLMapElement" }, { "d", "HTMLAreaElement" },
            { "f", "HTMLTimeElement" }, { "g", "HTMLTableCaptionElement" }, { "h", "HTMLTableColElement" }, { "i", "HTMLTableColElement" },
            { "j", "HTMLTableSectionElement" }, { "k", "HTMLTableSectionElement" } };
        // 파서가 만든 요소의 인터페이스 이름을 확인한다.
        for (int i = 0; i < expected.length; i++) {
            Check.equal("[object " + expected[i][1] + "]", e.eval("Object.prototype.toString.call(document.getElementById('" + expected[i][0] + "'))"),
                    expected[i][0]);
        }
        Check.equal("true", e.eval("document.getElementById('b').open"), "details open is true for the attribute");
        Check.equal("2", e.eval("document.getElementById('h').span"), "colgroup span");
        Check.equal("2014-10-28", e.eval("document.getElementById('f').dateTime"), "time dateTime reflects datetime");
        Check.equal("A", e.eval("document.getElementById('d').alt"), "area alt");
        Check.equal("rect", e.eval("document.getElementById('d').shape"), "area shape");
        Check.equal("0,0,1,1", e.eval("document.getElementById('d').coords"), "area coords");
        Check.equal("m", e.eval("document.getElementById('c').name"), "map name");
    }

    /**
     * The reflecting attributes: strings, booleans, integers, unsigned integers and URLs.
     * <p>
     * 반영 속성: 문자열, 불리언, 정수, 부호 없는 정수, URL.
     */
    public void testReflectingAttributes() {
        DomTestEnv e = new DomTestEnv("<!doctype html><body></body>");
        Check.equal("false", e.eval("document.createElement('details').open"), "boolean false by default");
        Check.equal("true:", e.eval("var d = document.createElement('details'); d.open = true; d.hasAttribute('open') + ':' + d.getAttribute('open')"),
                "setting a boolean property sets the attribute to the empty string");
        Check.equal("false", e.eval("var d2 = document.createElement('details'); d2.open = true; d2.open = false; d2.hasAttribute('open')"),
                "clearing the property removes the attribute");
        Check.equal("x|x", e.eval("var t = document.createElement('time'); t.dateTime = 'x'; t.getAttribute('datetime') + '|' + t.dateTime"),
                "the property name differs from the attribute name");
        Check.equal("3", e.eval("var c = document.createElement('col'); c.span = 3; c.getAttribute('span')"), "unsigned integer");
        Check.equal("1", e.eval("var c2 = document.createElement('col'); c2.span"), "the default of span is read as 1 or the attribute value");
        Check.equal("c.png", e.eval("var o = document.createElement('object'); o.setAttribute('data', 'c.png'); o.getAttribute('data')"),
                "attribute is kept as written");
        Check.equal("true", e.eval("var o2 = document.createElement('object'); o2.setAttribute('data', 'c.png'); o2.data.indexOf('c.png') > 0 "
                + "&& o2.data !== 'c.png'"), "a URL property is resolved against the document");
        Check.equal("5", e.eval("var f = document.createElement('basefont'); f.size = 5; f.getAttribute('size')"), "integer");
        Check.equal("red", e.eval("var f2 = document.createElement('font'); f2.color = 'red'; f2.getAttribute('color')"), "font color");
        Check.equal("true", e.eval("var m = document.createElement('marquee'); m.scrollAmount = 7; m.trueSpeed = true; "
                + "m.getAttribute('scrollamount') === '7' && m.hasAttribute('truespeed')"), "marquee reflects");
        Check.equal("true", e.eval("var k = document.createElement('track'); k.default = true; k.kind = 'subtitles'; "
                + "k.hasAttribute('default') && k.getAttribute('kind') === 'subtitles'"), "track reflects, also the word default");
        Check.equal("true", e.eval("var em = document.createElement('embed'); em.src = 'e.swf'; em.type = 'application/x-shockwave-flash'; "
                + "em.getAttribute('src') === 'e.swf' && em.getAttribute('type') === 'application/x-shockwave-flash'"), "embed reflects");
        Check.equal("true", e.eval("var ar = document.createElement('area'); ar.noHref = true; ar.hasAttribute('nohref')"), "area noHref");
    }

    /**
     * The document members that were missing: <code>links</code> (a and area with href, live), <code>anchors</code>, <code>plugins</code>,
     * <code>getElementsByName</code>, <code>lastModified</code>, <code>designMode</code>, <code>dir</code>, and the editing command queries
     * (supported, but not enabled without an editing selection).
     * <p>
     * 없던 document 멤버: <code>links</code>(href 가 있는 a 와 area, 라이브), <code>anchors</code>, <code>plugins</code>, <code>getElementsByName</code>,
     * <code>lastModified</code>, <code>designMode</code>, <code>dir</code>, 편집 명령 질의(지원하지만 편집 선택이 없으면 사용할 수 없다).
     */
    public void testDocumentMembers() {
        DomTestEnv e = new DomTestEnv("<!doctype html><html dir=rtl><body><a href=x>1</a><a name=n>2</a><a>3</a><map name=m><area href=y alt=a>"
                + "<area alt=b></map><embed src=e.swf><input name=q><select name=q></select><div name=q></div></body></html>");
        Check.equal("2", e.eval("document.links.length"), "a with href and area with href (not the a without href, not the area without href)");
        Check.equal("1", e.eval("document.anchors.length"), "a with name");
        Check.equal("n", e.eval("document.anchors[0].name || document.anchors[0].getAttribute('name')"), "the anchor");
        Check.equal("1", e.eval("document.plugins.length"), "plugins is the embeds");
        Check.equal("true", e.eval("document.plugins.length === document.embeds.length"), "plugins equals embeds");
        Check.equal("3", e.eval("document.getElementsByName('q').length"), "elements with name q (any HTML element)");
        Check.equal("INPUT", e.eval("document.getElementsByName('q')[0].tagName"), "document order");
        Check.equal("3", e.eval("var l = document.links; var a = document.createElement('a'); a.setAttribute('href', 'z'); "
                + "document.body.appendChild(a); l.length"), "links is live");
        Check.equal("true", e.eval("/^[0-9]{2}[/][0-9]{2}[/][0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2}$/.test(document.lastModified)"), "lastModified format");
        Check.equal("off", e.eval("document.designMode"), "designMode");
        Check.equal("on:off", e.eval("document.designMode = 'on'; var on = document.designMode; document.designMode = 'off'; on + ':' + document.designMode"),
                "designMode switches editing on and off");
        Check.equal("rtl", e.eval("document.dir"), "dir of the root element");
        Check.equal("ltr:ltr", e.eval("document.dir = 'ltr'; document.dir + ':' + document.documentElement.getAttribute('dir')"), "dir is written");
        Check.equal("false,true,false,false,", e.eval("[document.execCommand('bold'), document.queryCommandSupported('bold'), "
                + "document.queryCommandEnabled('bold'), document.queryCommandState('bold'), document.queryCommandValue('bold')].join(',')"),
                "bold is supported, but not enabled without an editing selection");
    }

    /**
     * The URL utilities of the hyperlink elements: the parts of an absolute URL, the default ports, a missing or unusable <code>href</code>,
     * the setters and <code>toString()</code>, for <code>a</code> and <code>area</code>.
     * <p>
     * 하이퍼링크 요소의 URL 유틸리티: 절대 URL 의 부분, 기본 포트, 없거나 쓸 수 없는 <code>href</code>, 설정자, <code>toString()</code>. <code>a</code> 와
     * <code>area</code> 모두.
     */
    public void testHyperlinkUrlUtilities() {
        DomTestEnv e = new DomTestEnv("<!doctype html><body><a id=a href=\"http://user:pw@example.com:8080/p/q.html?x=1&amp;y=2#frag\">a</a>"
                + "<a id=d href=\"https://example.com:443/x\">d</a><a id=n>n</a><map name=m><area id=r href=\"http://example.org/dir/\" alt=r></map></body>");
        String[][] expected = { { "protocol", "http:" }, { "username", "user" }, { "password", "pw" }, { "host", "example.com:8080" },
            { "hostname", "example.com" }, { "port", "8080" }, { "pathname", "/p/q.html" }, { "search", "?x=1&y=2" }, { "hash", "#frag" },
            { "origin", "http://example.com:8080" } };
        // 부분마다 읽은 값을 확인한다.
        for (int i = 0; i < expected.length; i++) {
            Check.equal(expected[i][1], e.eval("document.getElementById('a')." + expected[i][0]), expected[i][0]);
        }
        Check.equal("|example.com|/x|https:|https://example.com", e.eval("var d = document.getElementById('d'); [d.port, d.host, d.pathname, d.protocol, d.origin].join('|')"),
                "the default port is left out");
        Check.equal(",,,,,", e.eval("var n = document.getElementById('n'); [n.protocol, n.host, n.pathname, n.search, n.hash, n.origin].join(',')"),
                "an anchor without href has empty parts");
        Check.equal("", e.eval("var n2 = document.getElementById('n'); n2.hash = 'x'; n2.getAttribute('href') || ''"), "a write without href is ignored");
        Check.equal("http://example.org/dir/|example.org|/dir/", e.eval("var r = document.getElementById('r'); r.href + '|' + r.hostname + '|' + r.pathname"),
                "area has the same utilities");
        Check.equal("http://user:pw@example.com:8080/p/q.html?x=1&y=2#z", e.eval("var a2 = document.getElementById('a'); a2.hash = 'z'; a2.href"), "hash setter");
        Check.equal("?q=1", e.eval("var a3 = document.getElementById('a'); a3.search = '?q=1'; a3.search"), "search setter");
        Check.equal("/n/m", e.eval("var a4 = document.getElementById('a'); a4.pathname = 'n/m'; a4.pathname"), "pathname setter adds the slash");
        Check.equal("example.com:81", e.eval("var a5 = document.getElementById('a'); a5.port = '81'; a5.host"), "port setter");
        Check.equal("https:", e.eval("var a6 = document.getElementById('a'); a6.protocol = 'https'; a6.protocol"), "protocol setter");
        Check.equal("other.org:81", e.eval("var a7 = document.getElementById('a'); a7.hostname = 'other.org'; a7.host"), "hostname setter keeps the port");
        Check.equal("", e.eval("var a8 = document.getElementById('a'); a8.hash = ''; a8.hash"), "an empty hash removes the fragment");
        Check.equal("true", e.eval("var a9 = document.getElementById('a'); String(a9) === a9.href"), "toString is the href");
    }
}
