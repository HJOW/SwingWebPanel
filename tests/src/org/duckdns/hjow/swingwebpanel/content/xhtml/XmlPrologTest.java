package org.duckdns.hjow.swingwebpanel.content.xhtml;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link XmlProlog}: the lightweight scan of the XML declaration, the DOCTYPE and the root element start tag, including
 * the cases where the scan has to give up.
 * <p>
 * {@link XmlProlog} 의 시험: XML 선언, DOCTYPE, 루트 요소 시작 태그를 훑는 가벼운 검사. 검사가 포기해야 하는 경우도 포함한다.
 */
public class XmlPrologTest {

    /**
     * The XHTML namespace.
     * <p>
     * XHTML 네임스페이스.
     */
    private static final String NS = XmlProlog.XHTML_NAMESPACE;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XmlPrologTest() {
    }

    /**
     * The XML declaration with both quote styles and missing parts.
     * <p>
     * 두 가지 따옴표와 빠진 부분이 있는 XML 선언.
     */
    public void testDeclaration() {
        XmlProlog p = XmlProlog.scan("<?xml version=\"1.0\" encoding=\"EUC-KR\" standalone=\"yes\"?><a/>");
        Check.equal("1.0", p.getXmlVersion(), "version");
        Check.equal("EUC-KR", p.getXmlEncoding(), "encoding");
        XmlProlog q = XmlProlog.scan("<?xml   version='1.1'\n   encoding = 'utf-8' ?><a/>");
        Check.equal("1.1", q.getXmlVersion(), "single quotes and white space");
        Check.equal("utf-8", q.getXmlEncoding(), "encoding with spaces around =");
        XmlProlog r = XmlProlog.scan("<?xml version=\"1.0\"?><a/>");
        Check.isNull(r.getXmlEncoding(), "no encoding");
        XmlProlog none = XmlProlog.scan("<a/>");
        Check.isNull(none.getXmlVersion(), "no declaration");
        XmlProlog notDecl = XmlProlog.scan("<?xml-stylesheet href=\"a\"?><a/>");
        Check.isNull(notDecl.getXmlVersion(), "xml-stylesheet is not the declaration");
        Check.equal("a", notDecl.getRootName(), "the scan goes on to the root");
        XmlProlog bom = XmlProlog.scan((char) 0xFEFF + "<?xml version=\"1.0\" encoding=\"x\"?><a/>");
        Check.equal("x", bom.getXmlEncoding(), "a leading byte order mark character is skipped");
        Check.isNull(XmlProlog.scan(null).getRootName(), "null is empty");
        Check.isNull(XmlProlog.scan("<?xml version=\"1.0\"").getXmlVersion(), "unterminated declaration");
    }

    /**
     * DOCTYPE variants: none, name only, SYSTEM, PUBLIC, internal subset with tricky content, positions.
     * <p>
     * DOCTYPE 변형: 없음, 이름만, SYSTEM, PUBLIC, 까다로운 내용의 내부 서브셋, 위치.
     */
    public void testDoctype() {
        Check.isFalse(XmlProlog.scan("<a/>").hasDoctype(), "none");
        XmlProlog h = XmlProlog.scan("<!DOCTYPE html><html/>");
        Check.isTrue(h.hasDoctype(), "found");
        Check.equal("html", h.getDoctypeName(), "name");
        Check.isFalse(h.hasExternalId(), "no identifiers");
        Check.isNull(h.getInternalSubset(), "no subset");
        XmlProlog sys = XmlProlog.scan("<!DOCTYPE r SYSTEM 'http://x/r.dtd'><r/>");
        Check.equal("http://x/r.dtd", sys.getSystemId(), "system id");
        Check.isNull(sys.getPublicId(), "no public id");
        Check.isTrue(sys.hasExternalId(), "has an external id");
        String text = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n   \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html/>";
        XmlProlog pub = XmlProlog.scan(text);
        Check.equal("-//W3C//DTD XHTML 1.0 Strict//EN", pub.getPublicId(), "public id");
        Check.equal("http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd", pub.getSystemId(), "system id after a line break");
        Check.equal(0, pub.getDoctypeStart(), "start");
        Check.equal("\n<html/>", text.substring(pub.getDoctypeEnd()), "end is just after the closing bracket");
        // 따옴표, 주석, 처리 명령 안의 ']' 와 '>' 는 서브셋의 끝이 아니다.
        String tricky = "<!DOCTYPE r [ <!ENTITY a \"x]>y\"> <!-- ] > --> <?pi ] ?> <!ENTITY b 'z]'> ]><r/>";
        XmlProlog t = XmlProlog.scan(tricky);
        Check.isTrue(t.hasDoctype(), "tricky subset");
        Check.equal(" <!ENTITY a \"x]>y\"> <!-- ] > --> <?pi ] ?> <!ENTITY b 'z]'> ", t.getInternalSubset(), "subset text");
        Check.equal("r", t.getRootName(), "root after the subset");
        XmlProlog both = XmlProlog.scan("<!DOCTYPE r SYSTEM \"r.dtd\" [<!ENTITY e \"1\">]><r/>");
        Check.equal("r.dtd", both.getSystemId(), "system id");
        Check.equal("<!ENTITY e \"1\">", both.getInternalSubset(), "subset");
        XmlProlog pre = XmlProlog.scan("<?xml version=\"1.0\"?>\n<!-- c --><?pi x?>\n  <!DOCTYPE r><r/>");
        Check.isTrue(pre.hasDoctype(), "after declaration, comment and instruction");
    }

    /**
     * Broken DOCTYPE declarations are flagged and stop the scan.
     * <p>
     * 깨진 DOCTYPE 선언은 표시되고 검사를 멈춘다.
     */
    public void testMalformedDoctype() {
        String[] bad = {"<!DOCTYPE", "<!DOCTYPE >", "<!DOCTYPEhtml>", "<!DOCTYPE r [ <!ENTITY a \"x\" ]", "<!DOCTYPE r SYSTEM>", "<!DOCTYPE r PUBLIC \"p\">",
            "<!DOCTYPE r SYSTEM \"unterminated>", "<!DOCTYPE r [ <!-- unterminated ]>", "<!DOCTYPE r [ ] junk>", "<!DOCTYPE r"};
        // 깨진 선언마다 표시되고 루트 요소까지 가지 않아야 한다.
        for (int i = 0; i < bad.length; i++) {
            XmlProlog p = XmlProlog.scan(bad[i] + "<r/>");
            Check.isTrue(p.isDoctypeMalformed(), "malformed: " + bad[i]);
            Check.isFalse(p.hasDoctype(), "not a complete doctype: " + bad[i]);
            Check.isNull(p.getRootName(), "scan stopped: " + bad[i]);
            Check.equal(0, p.getDoctypeStart(), "start of the broken declaration: " + bad[i]);
        }
    }

    /**
     * The root element: name, default namespace, prefix declarations, attributes in any quote style, and the XHTML test.
     * <p>
     * 루트 요소: 이름, 기본 네임스페이스, 접두사 선언, 어떤 따옴표든 속성, XHTML 판정.
     */
    public void testRootElement() {
        XmlProlog a = XmlProlog.scan("<html xmlns=\"" + NS + "\" lang='en'>");
        Check.equal("html", a.getRootName(), "name");
        Check.equal(NS, a.getRootNamespace(), "namespace");
        Check.isTrue(a.isXhtmlRoot(), "XHTML root");
        XmlProlog b = XmlProlog.scan("<h:html xmlns:h='" + NS + "' xmlns='urn:other'/>");
        Check.equal(NS, b.getRootNamespace(), "the prefix decides");
        Check.isTrue(b.isXhtmlRoot(), "prefixed XHTML root");
        Check.isFalse(XmlProlog.scan("<html/>").isXhtmlRoot(), "no namespace");
        Check.isFalse(XmlProlog.scan("<svg xmlns=\"http://www.w3.org/2000/svg\"/>").isXhtmlRoot(), "another namespace");
        Check.isFalse(XmlProlog.scan("<HTML xmlns=\"" + NS + "\"/>").isXhtmlRoot(), "case sensitive");
        Check.isFalse(XmlProlog.scan("<body xmlns=\"" + NS + "\"/>").isXhtmlRoot(), "another element");
        Check.equal("r", XmlProlog.scan("<r\n  a = \"1\"\n  xmlns=\"u\"\n/>").getRootName(), "attributes on several lines");
        Check.equal("u", XmlProlog.scan("<r\n  a = \"1\"\n  xmlns=\"u\"\n/>").getRootNamespace(), "namespace after spaced attributes");
        Check.isNull(XmlProlog.scan("text").getRootName(), "no element");
        Check.isNull(XmlProlog.scan("<!-- unterminated").getRootName(), "unterminated comment");
        Check.isNull(XmlProlog.scan("<?pi unterminated").getRootName(), "unterminated instruction");
    }
}
