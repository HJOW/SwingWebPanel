package org.duckdns.hjow.swingwebpanel.content;

import org.duckdns.hjow.swingwebpanel.content.error.ErrorPageBuilder;
import org.duckdns.hjow.swingwebpanel.content.text.PlainTextDocumentBuilder;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlResult;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks that the documents built by this layer (XHTML, plain text, generated error pages) go through the project's CSS engine like
 * documents that come from the HTML parser: the user agent style sheet applies to elements of an XML document in the XHTML namespace (and
 * only to them, because XML names are case sensitive), the plain text <code>pre</code> keeps line breaks and wraps, and a
 * <code>style</code> element of an XHTML page is collected.
 * <p>
 * 이 계층이 만든 문서(XHTML, 일반 텍스트, 생성 오류 페이지)가 HTML 파서에서 온 문서처럼 프로젝트의 CSS 엔진을 거치는지 확인한다: 사용자 에이전트
 * 스타일시트가 XHTML 네임스페이스에 있는 XML 문서의 요소에 적용되고(XML 이름은 대소문자를 구분하므로 그 요소에만), 일반 텍스트의 <code>pre</code> 는 줄 바꿈을
 * 유지하며 줄바꿈하고, XHTML 페이지의 <code>style</code> 요소가 수집된다.
 */
public class ContentDocumentsStyleTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContentDocumentsStyleTest() {
    }

    /**
     * Creates the CSS engine of a document with its style sheets collected.
     * <p>
     * 문서의 CSS 엔진을 만들고 스타일시트를 모은다.
     *
     * @param doc the document / 문서
     * @return the engine / 엔진
     */
    private static CssEngine engine(Document doc) {
        CssEngine e = new CssEngine(doc, null, new MediaEnvironment(800, 600));
        e.loadStyleSheets();
        return e;
    }

    /**
     * Returns the computed value of a property of the first element with a tag name.
     * <p>
     * 태그 이름이 같은 첫 요소의 속성 계산 값을 반환한다.
     *
     * @param e        the engine / 엔진
     * @param doc      the document / 문서
     * @param tag      the tag name / 태그 이름
     * @param property the property / 속성
     * @return the value / 값
     */
    private static String value(CssEngine e, Document doc, String tag, String property) {
        Element el = doc.getElementsByTagName(tag).item(0);
        Check.notNull(el, "element " + tag);
        return e.getComputedStyle(el).getPropertyValue(property);
    }

    /**
     * An XHTML document is styled by the user agent style sheet exactly like the same markup parsed as HTML.
     * <p>
     * XHTML 문서는 같은 마크업을 HTML 로 해석한 것과 똑같이 사용자 에이전트 스타일시트의 적용을 받는다.
     */
    public void testXhtmlGetsUserAgentStyles() {
        String ns = "http://www.w3.org/1999/xhtml";
        String body = "<head><title>t</title><style>p { color: red }</style></head><body><h1>x</h1><p>p</p><b>b</b><pre>q</pre></body></html>";
        XhtmlResult x = XhtmlParser.parseDocument("<html xmlns=\"" + ns + "\">" + body, null);
        Check.isNull(x.getError(), "well-formed: " + x.getError());
        Document html = HtmlParser.parseDocument("<html>" + body);
        CssEngine xe = engine(x.getDocument());
        CssEngine he = engine(html);
        String[][] checks = {{"h1", "display"}, {"p", "display"}, {"b", "font-weight"}, {"pre", "white-space"}, {"head", "display"}, {"style", "display"},
            {"title", "display"}, {"body", "margin-top"}, {"p", "color"}, {"h1", "font-size"}};
        // 같은 요소의 계산 값이 HTML 과 XHTML 에서 같은지 확인한다. 값 자체도 알려진 것이어야 한다.
        for (int i = 0; i < checks.length; i++) {
            Check.equal(value(he, html, checks[i][0], checks[i][1]), value(xe, x.getDocument(), checks[i][0], checks[i][1]),
                    checks[i][0] + " " + checks[i][1] + " is the same in XHTML and in HTML");
        }
        Check.equal("block", value(xe, x.getDocument(), "p", "display"), "p is a block");
        Check.equal("none", value(xe, x.getDocument(), "head", "display"), "head is not displayed");
        Check.equal("none", value(xe, x.getDocument(), "style", "display"), "style is not displayed");
        Check.equal("pre", value(xe, x.getDocument(), "pre", "white-space"), "pre keeps white space");
        Check.equal(1, xe.getStyleSheets().size(), "the style element of the XHTML page was collected");
        Check.isTrue(value(xe, x.getDocument(), "p", "color").indexOf("255") >= 0, "and its rule applies: " + value(xe, x.getDocument(), "p", "color"));
        // XML 이름은 대소문자를 구분한다: 대문자 요소는 사용자 에이전트 스타일의 p 선택자와 맞지 않는다.
        XhtmlResult upper = XhtmlParser.parseDocument("<html xmlns=\"" + ns + "\"><body><P>p</P></body></html>", null);
        Check.isNull(upper.getError(), "well-formed");
        CssEngine ue = engine(upper.getDocument());
        Element p = upper.getDocument().getDocumentElement().getFirstChild().getFirstChild() instanceof Element
                ? (Element) upper.getDocument().getDocumentElement().getFirstChild().getFirstChild() : null;
        Check.notNull(p, "the P element");
        Check.equal("inline", ue.getComputedStyle(p).getPropertyValue("display"), "P is not the block element p (case sensitive)");
    }

    /**
     * The plain text page keeps line breaks, wraps long lines and is a block.
     * <p>
     * 일반 텍스트 페이지는 줄 바꿈을 유지하고, 긴 줄을 줄바꿈하며, 블록이다.
     */
    public void testPlainTextStyle() {
        Document d = PlainTextDocumentBuilder.build("line one\n  line two\n");
        CssEngine e = engine(d);
        Check.equal("pre-wrap", value(e, d, "pre", "white-space"), "line breaks and spaces are kept");
        Check.equal("break-word", value(e, d, "pre", "overflow-wrap"), "word-wrap is an alias of overflow-wrap");
        Check.equal("block", value(e, d, "pre", "display"), "block");
    }

    /**
     * The generated error pages are styled: the heading is a block, the detail block keeps its lines, and the page is parsed to the same
     * tree twice (deterministic).
     * <p>
     * 생성한 오류 페이지에 스타일이 적용된다: 제목줄은 블록이고, 상세 블록은 줄을 유지하며, 페이지는 두 번 해석해도 같은 트리이다(결정적).
     */
    public void testErrorPageStyle() {
        Document d = HtmlParser.parseDocument(ErrorPageBuilder.http("http://example.org/", 404, null).getHtml());
        CssEngine e = engine(d);
        Check.equal("block", value(e, d, "h1", "display"), "heading is a block");
        Check.equal(1, e.getStyleSheets().size(), "the style of the page is collected");
        Check.equal("none", value(e, d, "head", "display"), "head is not displayed");
        Check.equal("none", value(e, d, "title", "display"), "title is not displayed");
        Check.equal("sans-serif", e.getComputedStyle(d.getElementsByTagName("body").item(0)).fontFamilies().get(0), "the family of the page style");
    }
}
