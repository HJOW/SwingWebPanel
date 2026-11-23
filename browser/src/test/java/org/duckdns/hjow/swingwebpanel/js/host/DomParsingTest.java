package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>DOMParser</code> and <code>XMLSerializer</code> in scripts (plan item P20-04): the HTML and the XML types, the document that
 * a script gets (type, URL, scripting disabled, own tree), the <code>parsererror</code> document of malformed XML, the errors for bad
 * arguments, and the XML serialization of parsed and of page nodes. The page itself is never changed.
 * <p>
 * 스크립트의 <code>DOMParser</code> 와 <code>XMLSerializer</code> 시험(계획 P20-04): HTML 과 XML 타입, 스크립트가 받는 문서(타입, URL, 스크립트 비활성, 자기
 * 트리), 잘못된 XML 의 <code>parsererror</code> 문서, 잘못된 인수의 오류, 해석한 노드와 페이지 노드의 XML 직렬화. 페이지 자체는 바뀌지 않는다.
 */
public class DomParsingTest {

    /**
     * The fixture of the page of the case.
     * <p>
     * 사례 페이지의 도구.
     */
    private PageFixture fixture;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DomParsingTest() {
    }

    /**
     * Loads a small page before each case.
     * <p>
     * 사례마다 작은 페이지를 로드한다.
     */
    public void setUp() {
        fixture = new PageFixture();
        fixture.load("http://example.test/dir/page.html", "<!doctype html><body><p id=x class=c>page</p></body>");
    }

    /**
     * Evaluates a script that must not throw and returns its completion value as text.
     * <p>
     * 던지지 않아야 하는 스크립트를 실행해 완료 값을 문자열로 반환한다.
     *
     * @param source the script / 스크립트
     * @return the text / 문자열
     */
    private String run(String source) {
        String result = fixture.scripts.evaluate(source, "test");
        Check.notNull(result, "the script threw: " + source + " " + fixture.console);
        return result;
    }

    /**
     * Returns the text of the error that a statement throws.
     * <p>
     * 문장이 던지는 오류의 내용을 반환한다.
     *
     * @param statement the statement / 문장
     * @return <code>name</code> of the error, or "no error" / 오류의 <code>name</code>. 오류가 없으면 "no error"
     */
    private String errorOf(String statement) {
        return run("(function () { try { " + statement + "; return 'no error'; } catch (e) { return e.name; } })()");
    }

    /**
     * text/html: a new HTML document with its own tree, the URL of the page, scripts that do not run and noscript content that is markup.
     * <p>
     * text/html: 자기 트리를 가진 새 HTML 문서이며 URL은 페이지의 URL이고, 스크립트는 실행되지 않으며 noscript 내용은 마크업이다.
     */
    public void testHtml() {
        Check.equal("true:text/html:http://example.test/dir/page.html:T:true", run("var d = new DOMParser().parseFromString("
                + "'<!doctype html><title>T</title><p id=a>x<script>window.ran = 1<\\/script><noscript><b>nb</b></noscript>', 'text/html');"
                + " (d instanceof Document) + ':' + d.contentType + ':' + d.URL + ':' + d.title + ':' + (d !== document)"), "the document");
        Check.equal("undefined:nb:x", run("typeof window.ran + ':' + d.querySelector('noscript b').textContent + ':' + d.getElementById('a').firstChild.data"),
                "scripts do not run, noscript is markup");
        Check.equal("page:true:null", run("document.getElementById('x').textContent + ':' + (d.getElementById('x') === null) + ':' + document.getElementById('a')"),
                "the page and the parsed document do not share nodes");
        Check.equal("html:2", run("var e = new DOMParser().parseFromString('', 'text/html'); e.documentElement.localName + ':' + e.documentElement.childNodes.length"),
                "an empty text still makes html, head and body");
        Check.equal("true", run("String(Object.getPrototypeOf(d.body.firstChild) === Object.getPrototypeOf(document.getElementById('x')))"),
                "the new nodes use the interfaces of the page");
    }

    /**
     * XML types: the tree with namespaces, entities and attributes, the content type and a round trip through XMLSerializer.
     * <p>
     * XML 타입: 네임스페이스, 엔티티, 속성이 있는 트리, 콘텐츠 타입, XMLSerializer 를 거친 왕복.
     */
    public void testXml() {
        Check.equal("root:urn:r:1:text & more:text/xml:true", run("var x = new DOMParser().parseFromString("
                + "'<root xmlns=\"urn:r\" a=\"1\"><child>text &amp; more</child></root>', 'text/xml'); var r = x.documentElement;"
                + " r.localName + ':' + r.namespaceURI + ':' + r.getAttribute('a') + ':' + r.firstChild.textContent + ':' + x.contentType + ':' + (x instanceof Document)"),
                "an XML document");
        Check.equal("<root xmlns=\"urn:r\" a=\"1\"><child>text &amp; more</child></root>", run("new XMLSerializer().serializeToString(x)"), "round trip of the document");
        Check.equal("<child xmlns=\"urn:r\">text &amp; more</child>", run("new XMLSerializer().serializeToString(r.firstChild)"), "an inner element keeps its namespace");
        Check.equal("svg:http://www.w3.org/2000/svg:rect", run("var s = new DOMParser().parseFromString('<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\"><rect/></svg>', 'image/svg+xml');"
                + " s.documentElement.localName + ':' + s.documentElement.namespaceURI + ':' + s.documentElement.firstChild.localName"), "image/svg+xml");
        Check.equal("html:application/xhtml+xml", run("var h = new DOMParser().parseFromString('<html xmlns=\"http://www.w3.org/1999/xhtml\"><body><p>hi</p></body></html>',"
                + " 'application/xhtml+xml'); h.documentElement.localName + ':' + h.contentType"), "application/xhtml+xml");
        Check.equal("true", run("String(new DOMParser().parseFromString('<a><!--c--><?pi d?>text<![CDATA[cd]]></a>', 'application/xml').documentElement.textContent.indexOf('cd') > 0)"),
                "comments, processing instructions and CDATA parse");
    }

    /**
     * Malformed XML gives a document with one parsererror element that describes the error; the same for a bad entity.
     * <p>
     * 잘못된 XML 은 오류를 설명하는 parsererror 요소 하나가 든 문서를 준다. 잘못된 엔티티도 마찬가지이다.
     */
    public void testMalformedXml() {
        Check.equal("parsererror:http://www.mozilla.org/newlayout/xml/parsererror.xml:1:true", run("var bad = new DOMParser().parseFromString('<root><unclosed></root>', 'application/xml');"
                + " bad.documentElement.localName + ':' + bad.documentElement.namespaceURI + ':' + bad.childNodes.length + ':' + (bad.documentElement.textContent.indexOf('error on line') === 0)"),
                "the error document");
        Check.equal("parsererror", run("new DOMParser().parseFromString('<a>&undefined;</a>', 'text/xml').documentElement.localName"), "an undefined entity");
        Check.equal("parsererror", run("new DOMParser().parseFromString('', 'text/xml').documentElement.localName"), "an empty text is not an XML document");
        Check.equal("a", run("new DOMParser().parseFromString('<a/>', 'text/xml').documentElement.localName"), "a well-formed text still works afterwards");
    }

    /**
     * An unsupported type, a missing argument and calling the constructors without new are TypeErrors.
     * <p>
     * 지원하지 않는 type, 인수 누락, new 없는 생성자 호출은 TypeError 이다.
     */
    public void testArgumentErrors() {
        Check.equal("TypeError", errorOf("new DOMParser().parseFromString('<a/>', 'text/plain')"), "unsupported type");
        Check.equal("TypeError", errorOf("new DOMParser().parseFromString('<a/>', 'TEXT/HTML')"), "the type is case sensitive");
        Check.equal("TypeError", errorOf("new DOMParser().parseFromString('<a/>')"), "missing type");
        Check.equal("TypeError", errorOf("DOMParser()"), "DOMParser needs new");
        Check.equal("TypeError", errorOf("XMLSerializer()"), "XMLSerializer needs new");
        Check.equal("TypeError", errorOf("new XMLSerializer().serializeToString({})"), "not a node");
        Check.equal("TypeError", errorOf("new XMLSerializer().serializeToString()"), "missing node");
        Check.equal("[object DOMParser]:[object XMLSerializer]", run("Object.prototype.toString.call(new DOMParser()) + ':' + Object.prototype.toString.call(new XMLSerializer())"), "tags");
    }

    /**
     * XMLSerializer serializes the nodes of the page without changing them: an HTML element gets the XHTML namespace, a document the doctype.
     * <p>
     * XMLSerializer 는 페이지의 노드를 바꾸지 않고 직렬화한다: HTML 요소는 XHTML 네임스페이스를, 문서는 doctype 을 받는다.
     */
    public void testSerializePageNodes() {
        Check.equal("<p xmlns=\"http://www.w3.org/1999/xhtml\" id=\"x\" class=\"c\">page</p>", run("new XMLSerializer().serializeToString(document.getElementById('x'))"),
                "an HTML element");
        Check.equal("true", run("var all = new XMLSerializer().serializeToString(document); String(all.indexOf('<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">') === 0)"),
                "the document");
        Check.equal("page", run("document.getElementById('x').textContent"), "the page did not change");
        Check.equal("a&lt;b|<br xmlns=\"http://www.w3.org/1999/xhtml\" />", run("new XMLSerializer().serializeToString(document.createTextNode('a<b')) + '|'"
                + " + new XMLSerializer().serializeToString(document.createElement('br'))"), "a text node and a void element");
        Check.equal("<!--c--><![CDATA[x]]>", run("var xd = new DOMParser().parseFromString('<r/>', 'text/xml'); new XMLSerializer().serializeToString(xd.createComment('c'))"
                + " + new XMLSerializer().serializeToString(xd.createCDATASection('x'))"), "comment and CDATA");
    }
}
