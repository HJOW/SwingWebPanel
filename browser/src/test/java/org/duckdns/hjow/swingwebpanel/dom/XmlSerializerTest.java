package org.duckdns.hjow.swingwebpanel.dom;

import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link XmlSerializer} (plan item P20-04, DOM Parsing and Serialization "XML Serialization"): HTML elements with the namespace
 * declaration, void elements, elements of other namespaces with prefixes and default namespaces, generated prefixes for attributes, the
 * <code>xml</code> namespace, escaping, comments, CDATA, processing instructions, document types, fragments, documents and template contents.
 * <p>
 * {@link XmlSerializer} 시험(계획 P20-04, DOM Parsing and Serialization "XML Serialization"): 네임스페이스 선언이 붙는 HTML 요소, void 요소, 접두사와 기본
 * 네임스페이스가 있는 다른 네임스페이스의 요소, 속성의 생성된 접두사, <code>xml</code> 네임스페이스, 이스케이프, 주석, CDATA, 처리 명령, 문서 유형,
 * 조각, 문서, template 내용.
 */
public class XmlSerializerTest {

    /**
     * An XML document for the elements of other namespaces.
     * <p>
     * 다른 네임스페이스의 요소를 위한 XML 문서.
     */
    private Document xml;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XmlSerializerTest() {
    }

    /**
     * Creates the XML document of the case.
     * <p>
     * 사례의 XML 문서를 만든다.
     */
    public void setUp() {
        xml = Document.createXmlDocument("application/xml");
    }

    /**
     * Parses markup as an HTML document.
     * <p>
     * 마크업을 HTML 문서로 해석한다.
     *
     * @param html the markup / 마크업
     * @return the document / 문서
     */
    private static Document html(String html) {
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        new HtmlParser(doc, html).run();
        return doc;
    }

    /**
     * HTML elements get the XHTML namespace declaration once, void elements are written with a space and a slash, other empty elements keep
     * the start and end tags, and the text and the attributes are escaped.
     * <p>
     * HTML 요소는 XHTML 네임스페이스 선언을 한 번만 받고, void 요소는 공백과 슬래시로 쓰며, 다른 빈 요소는 시작·끝 태그를 유지하고, 텍스트와 속성은
     * 이스케이프한다.
     */
    public void testHtmlElements() {
        Document doc = html("<!doctype html><body><p class=\"a&amp;b&quot;c\" title='x\ty'>1 &lt; 2 &gt; 0 &amp; <br><img src=a.png><b></b><i>t</i></p>");
        Element p = doc.getElementsByTagName("p").item(0);
        Check.equal("<p xmlns=\"http://www.w3.org/1999/xhtml\" class=\"a&amp;b&quot;c\" title=\"x&#9;y\">1 &lt; 2 &gt; 0 &amp; <br />"
                + "<img src=\"a.png\" /><b></b><i>t</i></p>", XmlSerializer.serialize(p), "an HTML paragraph");
        Check.equal("<br xmlns=\"http://www.w3.org/1999/xhtml\" />", XmlSerializer.serialize(doc.getElementsByTagName("br").item(0)), "a void element");
        String whole = XmlSerializer.serialize(doc);
        Check.isTrue(whole.startsWith("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head></head><body>"),
                "the document starts with the doctype and the html element: " + whole);
    }

    /**
     * Elements with a prefix: the prefix is declared once and used for the descendants in the same namespace.
     * <p>
     * 접두사가 있는 요소: 접두사는 한 번 선언하고 같은 네임스페이스의 후손에 쓴다.
     */
    public void testPrefixedElements() {
        Element root = xml.createElementNS("urn:x", "a:root");
        root.appendChild(xml.createElementNS("urn:x", "a:child"));
        Element other = xml.createElementNS("urn:y", "b:other");
        root.appendChild(other);
        other.appendChild(xml.createTextNode("t"));
        Check.equal("<a:root xmlns:a=\"urn:x\"><a:child/><b:other xmlns:b=\"urn:y\">t</b:other></a:root>", XmlSerializer.serialize(root), "prefixes");
    }

    /**
     * Elements without a prefix use a default namespace declaration; a child in no namespace undeclares it.
     * <p>
     * 접두사가 없는 요소는 기본 네임스페이스 선언을 쓰며, 네임스페이스가 없는 자식은 그것을 해제한다.
     */
    public void testDefaultNamespaces() {
        Element root = xml.createElementNS("urn:d", "root");
        root.appendChild(xml.createElementNS("urn:d", "same"));
        root.appendChild(xml.createElementNS(null, "none"));
        Element x = xml.createElementNS("urn:e", "x");
        root.appendChild(x);
        Check.equal("<root xmlns=\"urn:d\"><same/><none xmlns=\"\"/><x xmlns=\"urn:e\"/></root>", XmlSerializer.serialize(root), "default namespaces");
        Check.equal("<plain/>", XmlSerializer.serialize(xml.createElementNS(null, "plain")), "an element in no namespace");
    }

    /**
     * Attributes in a namespace get the prefix that the document has or a generated one (ns1, ns2); the xml namespace needs no declaration.
     * <p>
     * 네임스페이스에 속한 속성은 문서가 가진 접두사나 만든 접두사(ns1, ns2)를 받으며, xml 네임스페이스는 선언이 필요 없다.
     */
    public void testAttributeNamespaces() {
        Element e = xml.createElementNS(null, "e");
        e.setAttributeNS("urn:attr", "p:x", "1");
        e.setAttributeNS("urn:attr2", "y", "2");
        e.setAttributeNS("urn:attr3", "z", "3");
        e.setAttributeNS(Namespaces.XML, "xml:lang", "en");
        e.setAttribute("plain", "v");
        Check.equal("<e xmlns:p=\"urn:attr\" p:x=\"1\" xmlns:ns1=\"urn:attr2\" ns1:y=\"2\" xmlns:ns2=\"urn:attr3\" ns2:z=\"3\" xml:lang=\"en\" plain=\"v\"/>",
                XmlSerializer.serialize(e), "attribute prefixes");
        Element xmlElement = xml.createElementNS(Namespaces.XML, "xml:foo");
        Check.equal("<xml:foo/>", XmlSerializer.serialize(xmlElement), "an element in the xml namespace");
    }

    /**
     * Declarations that the document has are kept and not repeated.
     * <p>
     * 문서가 가진 선언은 유지되고 반복되지 않는다.
     */
    public void testExistingDeclarations() {
        Element root = xml.createElementNS("urn:d", "root");
        root.setAttributeNS(Namespaces.XMLNS, "xmlns", "urn:d");
        root.setAttributeNS(Namespaces.XMLNS, "xmlns:p", "urn:p");
        Element child = xml.createElementNS("urn:p", "p:c");
        root.appendChild(child);
        Check.equal("<root xmlns=\"urn:d\" xmlns:p=\"urn:p\"><p:c/></root>", XmlSerializer.serialize(root), "declarations are written once");
    }

    /**
     * Comments, CDATA sections, processing instructions, document types, fragments and documents.
     * <p>
     * 주석, CDATA 섹션, 처리 명령, 문서 유형, 조각, 문서.
     */
    public void testOtherNodes() {
        Check.equal("<!--a--b-->", XmlSerializer.serialize(xml.createComment("a--b")), "comment");
        Check.equal("<![CDATA[x < y]]>", XmlSerializer.serialize(xml.createCDATASection("x < y")), "CDATA");
        Check.equal("<?target some data?>", XmlSerializer.serialize(xml.createProcessingInstruction("target", "some data")), "processing instruction");
        Check.equal("a &amp; b &lt; c &gt; d", XmlSerializer.serialize(xml.createTextNode("a & b < c > d")), "text");
        Check.equal("<!DOCTYPE html>", XmlSerializer.serialize(xml.createDocumentType("html", "", "")), "html doctype");
        Check.equal("<!DOCTYPE a PUBLIC \"-//P//\" \"http://s\">", XmlSerializer.serialize(xml.createDocumentType("a", "-//P//", "http://s")), "public doctype");
        Check.equal("<!DOCTYPE a SYSTEM 'x\"y'>", XmlSerializer.serialize(xml.createDocumentType("a", "", "x\"y")), "system doctype with a quotation mark");
        DocumentFragment f = xml.createDocumentFragment();
        f.appendChild(xml.createElementNS(null, "one"));
        f.appendChild(xml.createTextNode("t"));
        f.appendChild(xml.createComment("c"));
        Check.equal("<one/>t<!--c-->", XmlSerializer.serialize(f), "fragment");
        xml.appendChild(xml.createProcessingInstruction("pi", "d"));
        xml.appendChild(xml.createElementNS(null, "root"));
        Check.equal("<?pi d?><root/>", XmlSerializer.serialize(xml), "document");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Serializes null.
             * <p>
             * null 을 직렬화한다.
             */
            public void run() {
                XmlSerializer.serialize(null);
            }
        }, "null node");
    }

    /**
     * The contents of a template element are serialized in its place.
     * <p>
     * template 요소의 내용이 그 자리에 직렬화된다.
     */
    public void testTemplateContents() {
        Document doc = html("<!doctype html><body><template id=t><p>in</p></template><span>after</span>");
        Element template = doc.getElementById("t");
        Check.equal("<template xmlns=\"http://www.w3.org/1999/xhtml\" id=\"t\"><p>in</p></template>", XmlSerializer.serialize(template), "template contents");
    }
}
