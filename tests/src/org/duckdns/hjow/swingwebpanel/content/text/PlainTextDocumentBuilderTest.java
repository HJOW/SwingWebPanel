package org.duckdns.hjow.swingwebpanel.content.text;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link PlainTextDocumentBuilder} (plan item P17-02): the shape of the document, that nothing in the text is interpreted
 * (tags, scripts, references), the preservation of the line structure, the replacement of NUL and the argument rules.
 * <p>
 * {@link PlainTextDocumentBuilder} 의 시험(계획 P17-02): 문서의 모양, 텍스트의 어떤 것도 해석되지 않는 것(태그, 스크립트, 참조), 줄 구조의 보존, NUL 의
 * 대체, 인수 규칙.
 */
public class PlainTextDocumentBuilderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PlainTextDocumentBuilderTest() {
    }

    /**
     * Returns the <code>pre</code> element of a plain text document.
     * <p>
     * 일반 텍스트 문서의 <code>pre</code> 요소를 반환한다.
     *
     * @param doc the document / 문서
     * @return the element / 요소
     */
    private static Element pre(Document doc) {
        Element html = doc.getDocumentElement();
        Element body = (Element) html.getFirstChild().getNextSibling();
        return (Element) body.getFirstChild();
    }

    /**
     * The document has the shape html, head, body, pre with a text node, a style that keeps and wraps lines, and the properties of the
     * page.
     * <p>
     * 문서는 html, head, body, pre 와 텍스트 노드의 모양이고, 줄을 유지하며 줄바꿈하는 스타일과 페이지 속성이 있다.
     */
    public void testShape() {
        Document d = PlainTextDocumentBuilder.build("Hello\nWorld\n", "http://example.org/a.txt");
        Check.equal("text/plain", d.getContentType(), "content type");
        Check.isTrue(d.isHtmlDocument(), "an HTML document, as for browsers");
        Check.equal("http://example.org/a.txt", d.getUrl(), "URL");
        Check.equal(Document.NO_QUIRKS_MODE, d.getCompatibilityMode(), "mode");
        Element html = d.getDocumentElement();
        Check.equal("html", html.getLocalName(), "root");
        Check.equal(2, html.getChildNodes().getLength(), "head and body");
        Check.equal("head", ((Element) html.getFirstChild()).getLocalName(), "head");
        Check.isNull(html.getFirstChild().getFirstChild(), "the head is empty (no title)");
        Element body = (Element) html.getLastChild();
        Check.equal("body", body.getLocalName(), "body");
        Check.equal(1, body.getChildNodes().getLength(), "only the pre element");
        Element pre = (Element) body.getFirstChild();
        Check.equal("pre", pre.getLocalName(), "pre");
        Check.equal("word-wrap: break-word; white-space: pre-wrap;", pre.getAttribute("style"), "style keeps the line breaks and wraps long lines");
        Check.equal(PlainTextDocumentBuilder.PRE_STYLE, pre.getAttribute("style"), "constant");
        Check.equal(1, pre.getChildNodes().getLength(), "one text node");
        Check.equal(Node.TEXT_NODE, pre.getFirstChild().getNodeType(), "text node");
        Check.equal("Hello\nWorld\n", pre.getTextContent(), "text");
        Check.equal("about:blank", PlainTextDocumentBuilder.build("x").getUrl(), "the default URL stays");
        Check.equal("text/css", PlainTextDocumentBuilder.build("a{}", null, "text/css").getContentType(), "other text types report their own type");
        Check.equal("text/plain", PlainTextDocumentBuilder.build("a", null, null).getContentType(), "null type means plain text");
    }

    /**
     * Markup, script elements, character references and an XML declaration are plain data: nothing becomes an element or a node of
     * another kind.
     * <p>
     * 마크업, script 요소, 문자 참조, XML 선언은 단순한 데이터이다: 어느 것도 요소나 다른 종류의 노드가 되지 않는다.
     */
    public void testNothingIsInterpreted() {
        String hostile = "<?xml version=\"1.0\"?><!DOCTYPE html><html><script>alert(1)</script><b onclick=\"x()\">bold</b>&amp;&lt;&#65;<!-- c --></html></pre><pre>";
        Document d = PlainTextDocumentBuilder.build(hostile);
        Check.equal(0, d.getElementsByTagName("script").getLength(), "no script element");
        Check.equal(0, d.getElementsByTagName("b").getLength(), "no b element");
        Check.equal(4, d.getElementsByTagName("*").getLength(), "only html, head, body and pre");
        Element pre = pre(d);
        Check.equal(1, pre.getChildNodes().getLength(), "the pre has exactly one child");
        Check.equal(Node.TEXT_NODE, pre.getFirstChild().getNodeType(), "which is a text node");
        Check.equal(hostile, pre.getTextContent(), "with the unchanged text");
        Check.isNull(d.getDoctype(), "no doctype node from the text");
        Check.equal(1, d.getChildNodes().getLength(), "the document has only the root element");
    }

    /**
     * The line structure: all line ends become LF, nothing else changes (leading and trailing newlines, blank lines, spaces and tabs
     * stay), NUL becomes U+FFFD.
     * <p>
     * 줄 구조: 모든 줄 끝은 LF 가 되고 다른 것은 바뀌지 않으며(앞뒤의 줄 바꿈, 빈 줄, 공백, 탭은 그대로), NUL 은 U+FFFD 가 된다.
     */
    public void testLineStructure() {
        Check.equal("a\nb\nc\nd", PlainTextDocumentBuilder.normalize("a\r\nb\rc\nd"), "CR LF, CR and LF");
        Check.equal("\n\n a\t b \n\n", PlainTextDocumentBuilder.normalize("\r\n\r\n a\t b \r\n\r\n"), "leading and trailing newlines, blank lines, spaces, tabs");
        Check.equal("\n\n\n", PlainTextDocumentBuilder.normalize("\r\r\r"), "three CR are three line ends");
        Check.equal("\n\n", PlainTextDocumentBuilder.normalize("\n\r"), "LF CR is two line ends");
        Check.equal("a" + (char) 0xFFFD + "b", PlainTextDocumentBuilder.normalize("a" + (char) 0 + "b"), "NUL");
        Check.equal("", PlainTextDocumentBuilder.normalize(""), "empty");
        Check.equal("", PlainTextDocumentBuilder.normalize(null), "null is empty");
        String same = "no change\nneeded";
        Check.same(same, PlainTextDocumentBuilder.normalize(same), "the same string is returned if nothing changes");
        String korean = "한글 😀 text";
        Check.equal(korean, pre(PlainTextDocumentBuilder.build(korean)).getTextContent(), "non-ASCII text and a surrogate pair");
        Check.equal("x\ny", pre(PlainTextDocumentBuilder.build("x\r\ny")).getTextContent(), "built documents are normalized");
        Check.equal("", pre(PlainTextDocumentBuilder.build(null)).getTextContent(), "null text is an empty page");
        Check.equal("", pre(PlainTextDocumentBuilder.build("")).getTextContent(), "empty text is an empty page");
    }

    /**
     * Large text stays one text node and builds fast.
     * <p>
     * 큰 텍스트도 텍스트 노드 하나로 남고 빠르게 만들어진다.
     */
    public void testLargeText() {
        StringBuilder sb = new StringBuilder();
        // 한 줄에 100자씩 5만 줄(약 5 MB)을 만든다.
        for (int i = 0; i < 50000; i++) {
            sb.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n");
        }
        long t = System.currentTimeMillis();
        Document d = PlainTextDocumentBuilder.build(sb.toString());
        Check.equal(1, pre(d).getChildNodes().getLength(), "one text node");
        Check.equal(sb.length(), pre(d).getTextContent().length(), "complete text");
        Check.isTrue(System.currentTimeMillis() - t < 5000, "built in time");
    }

    /**
     * A prepared document can be filled, and only an empty one.
     * <p>
     * 준비한 문서를 채울 수 있고, 비어 있는 문서만 가능하다.
     */
    public void testPopulate() {
        Document prepared = new Document(true, "text/plain");
        PlainTextDocumentBuilder.populate(prepared, "abc");
        Check.equal("abc", pre(prepared).getTextContent(), "populated");
        final Document again = prepared;
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Fills a document twice.
             * <p>
             * 문서를 두 번 채운다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                PlainTextDocumentBuilder.populate(again, "x");
            }
        }, "not empty");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Fills null.
             * <p>
             * null 을 채운다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                PlainTextDocumentBuilder.populate(null, "x");
            }
        }, "null document");
    }
}
