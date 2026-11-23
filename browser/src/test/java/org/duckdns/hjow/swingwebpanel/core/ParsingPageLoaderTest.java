package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ParsingPageLoader} (plan items P07, P17-02, P17-04): the DOM tree is attached to the snapshots of HTML, Markdown and plain
 * text pages, the received source text stays unchanged, the title comes from the parsed document and other types get no document.
 * <p>
 * {@link ParsingPageLoader} 시험(계획 P07, P17-02, P17-04): HTML, Markdown, 일반 텍스트 페이지의 스냅샷에 DOM 트리가 붙고, 수신 원문은 바뀌지 않으며,
 * 제목은 해석한 문서에서 오고, 그 밖의 형식에는 문서가 붙지 않는다.
 */
public class ParsingPageLoaderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ParsingPageLoaderTest() {
    }

    /**
     * Returns the DOM document of a snapshot.
     * <p>
     * 스냅샷의 DOM 문서를 반환한다.
     *
     * @param s the snapshot / 스냅샷
     * @return the document / 문서
     */
    private static Document docOf(PageSnapshot s) {
        Check.notNull(s.getDocument(), "the snapshot has a document");
        Check.isTrue(s.getDocument() instanceof HtmlPageDocument, "it is a DOM document");
        return ((HtmlPageDocument) s.getDocument()).getDocument();
    }

    /**
     * P07: an HTML page gets a DOM tree with the services installed; the source text and the title are right.
     * <p>
     * P07: HTML 페이지는 서비스가 설치된 DOM 트리를 받고 원문과 제목이 올바르다.
     */
    public void testHtmlIsParsed() {
        String source = "<!doctype html>\r\n<title>My &amp; title</title><p id=a>hi<b>x";
        PageSnapshot s = ParsingPageLoader.attach(PageSnapshot.loaded("http://h.test/", source, "text/html", "http://h.test/", 200), true);
        Check.equal(source, s.getContent(), "the received text is unchanged (CRLF kept)");
        Check.equal("My & title", s.getTitle(), "title of the document");
        Document d = docOf(s);
        Element a = d.getElementById("a");
        Check.notNull(a, "element with id");
        Check.equal("P", a.getTagName(), "tag name");
        Check.notNull(d.querySelector("p b"), "the selector engine is installed");
        Check.equal("http://h.test/", d.getUrl(), "document URL");
        Check.equal("<b>x</b>", a.getInnerHtml().substring(2), "markup support is installed");
    }

    /**
     * P17-04: Markdown is converted for the DOM only; the snapshot keeps the Markdown.
     * <p>
     * P17-04: Markdown 은 DOM 용으로만 변환하고 스냅샷은 Markdown 을 유지한다.
     */
    public void testMarkdownConvertedForDomOnly() {
        PageSnapshot s = ParsingPageLoader.attach(PageSnapshot.text("# Heading\n\n<script>alert(1)</script>", "text/markdown"), true);
        Check.equal("# Heading\n\n<script>alert(1)</script>", s.getContent(), "getContent() returns the Markdown");
        Document d = docOf(s);
        Check.notNull(d.querySelector("h1"), "heading element");
        Check.isNull(d.querySelector("script"), "raw HTML in Markdown is not executed");
    }

    /**
     * P17-02: plain text is shown as text, never as markup.
     * <p>
     * P17-02: 일반 텍스트는 마크업이 아니라 텍스트로 보인다.
     */
    public void testPlainTextIsNotMarkup() {
        String text = "<b>not markup</b>\nline 2 & <script>x</script>";
        PageSnapshot s = ParsingPageLoader.attach(PageSnapshot.text(text, "text/plain"), true);
        Check.equal(text, s.getContent(), "source unchanged");
        Document d = docOf(s);
        Check.isNull(d.querySelector("b"), "no element from the text");
        Check.isNull(d.querySelector("script"), "no script element");
        Check.equal(text, d.querySelector("pre").getTextContent(), "the pre element holds the text unchanged");
    }

    /**
     * Attribute names that are not XML Names (<code>@click</code>, <code>:class</code>) are kept by the HTML parser.
     * <p>
     * XML Name 이 아닌 속성 이름(<code>@click</code>, <code>:class</code>)도 HTML 파서가 보존한다.
     */
    public void testNonXmlAttributeNamesAreKept() {
        String src = "<div id=d @click=\"go()\" :class=\"c\" v-on:foo.bar=z data-x=1>t</div>";
        Document d = docOf(ParsingPageLoader.attach(PageSnapshot.text(src, "text/html"), true));
        Element e = d.getElementById("d");
        Check.equal("go()", e.getAttribute("@click"), "@click");
        Check.equal("c", e.getAttribute(":class"), ":class");
        Check.equal("z", e.getAttribute("v-on:foo.bar"), "v-on:foo.bar");
        Check.equal("1", e.getAttribute("data-x"), "ordinary attribute");
        Check.equal(5, e.getAttributes().getLength(), "no attribute was dropped");
    }

    /**
     * Other types and snapshots that already have a document are left alone.
     * <p>
     * 그 밖의 형식과 이미 문서가 있는 스냅샷은 그대로 둔다.
     */
    public void testOtherTypesAndExistingDocuments() {
        PageSnapshot bin = PageSnapshot.loaded("http://h.test/x", "data", "application/octet-stream", "http://h.test/x", 200);
        Check.isTrue(ParsingPageLoader.attach(bin, true) == bin, "unknown types keep their snapshot");
        PageSnapshot withDoc = ParsingPageLoader.attach(PageSnapshot.text("<p>a</p>", "text/html"), true);
        Check.isTrue(ParsingPageLoader.attach(withDoc, true) == withDoc, "an existing document is kept");
    }

    /**
     * P07: the scripting flag changes how noscript is parsed, and broken markup never fails.
     * <p>
     * P07: scripting 플래그는 noscript 해석을 바꾸며 깨진 마크업도 실패하지 않는다.
     */
    public void testScriptingFlagAndBrokenMarkup() {
        String src = "<body><noscript><p id=n>x</p></noscript></body>";
        Document on = docOf(ParsingPageLoader.attach(PageSnapshot.text(src, "text/html"), true));
        Document off = docOf(ParsingPageLoader.attach(PageSnapshot.text(src, "text/html"), false));
        Check.isNull(on.getElementById("n"), "with scripting the noscript content is text");
        Check.notNull(off.getElementById("n"), "without scripting the noscript content is parsed");
        PageSnapshot broken = ParsingPageLoader.attach(PageSnapshot.text("<table><tr><td>a<td>b</p></i><<", "text/html"), true);
        Check.notNull(docOf(broken).querySelector("td"), "broken markup still gives a tree");
    }
}
