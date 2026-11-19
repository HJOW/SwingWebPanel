package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.html.NamedCharacterReferences;
import org.duckdns.hjow.swingwebpanel.html.ParserControl;
import org.duckdns.hjow.swingwebpanel.html.ParserListenerAdapter;
import org.duckdns.hjow.swingwebpanel.html.ParserNodeData;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link XhtmlParser} (plan item P17-05): the DOM that comes out of well-formed XHTML, namespaces, case sensitivity, the
 * structured errors for malformed input without any fall back, the policy for named character references, CDATA, comments and
 * processing instructions, the notifications to the {@link org.duckdns.hjow.swingwebpanel.html.ParserListener} and the equality of the tree with the one of
 * the HTML parser.
 * <p>
 * {@link XhtmlParser} 의 시험(계획 P17-05): well-formed XHTML 에서 나오는 DOM, 네임스페이스, 대소문자 구분, 폴백 없이 구조화된 오류가 되는 잘못된
 * 입력, 이름 있는 문자 참조 정책, CDATA, 주석, 처리 명령, {@link org.duckdns.hjow.swingwebpanel.html.ParserListener} 통지, HTML 파서의 트리와의 동일성.
 */
public class XhtmlParserTest {

    /**
     * The XHTML namespace.
     * <p>
     * XHTML 네임스페이스.
     */
    private static final String NS = Namespaces.HTML;

    /**
     * The public identifier of XHTML 1.0 Strict.
     * <p>
     * XHTML 1.0 Strict 의 공개 식별자.
     */
    private static final String STRICT_PUBLIC = "-//W3C//DTD XHTML 1.0 Strict//EN";

    /**
     * The system identifier of XHTML 1.0 Strict.
     * <p>
     * XHTML 1.0 Strict 의 시스템 식별자.
     */
    private static final String STRICT_SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";

    /**
     * The DOCTYPE line of XHTML 1.0 Strict.
     * <p>
     * XHTML 1.0 Strict 의 DOCTYPE 줄.
     */
    private static final String STRICT_DOCTYPE = "<!DOCTYPE html PUBLIC \"" + STRICT_PUBLIC + "\" \"" + STRICT_SYSTEM + "\">";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XhtmlParserTest() {
    }

    /**
     * Parses a text.
     * <p>
     * 텍스트를 해석한다.
     *
     * @param xml the XML text / XML 텍스트
     * @return the result / 결과
     */
    private static XhtmlResult parse(String xml) {
        return XhtmlParser.parseDocument(xml, "http://example.org/dir/page.xhtml");
    }

    /**
     * Parses a text that must be well-formed and returns the dump of the tree.
     * <p>
     * well-formed 여야 하는 텍스트를 해석하고 트리의 덤프를 반환한다.
     *
     * @param xml the XML text / XML 텍스트
     * @return the dump / 덤프
     */
    private static String dump(String xml) {
        XhtmlResult r = parse(xml);
        Check.isNull(r.getError(), "no error for " + xml);
        return XhtmlTreeDump.dump(r.getDocument());
    }

    /**
     * Checks that a text is rejected with a structured error on a line.
     * <p>
     * 텍스트가 한 줄의 구조화된 오류로 거부되는지 확인한다.
     *
     * @param xml  the malformed text / 잘못된 텍스트
     * @param line the expected line (1 based) / 기대하는 줄(1부터)
     * @return the result / 결과
     */
    private static XhtmlResult bad(String xml, int line) {
        XhtmlResult r = parse(xml);
        Check.notNull(r.getError(), "error expected for " + xml);
        Check.isFalse(r.isWellFormed(), "not well-formed: " + xml);
        Check.equal(XmlParseError.Kind.NOT_WELL_FORMED, r.getError().getKind(), "kind for " + xml);
        Check.equal(line, r.getError().getLine(), "line for " + xml + " (" + r.getError() + ")");
        Check.isTrue(r.getError().getColumn() > 0, "column known for " + xml);
        Check.isTrue(r.getError().getMessage().length() > 0, "message for " + xml);
        Check.equal(xml, r.getSource(), "the source text is kept for " + xml);
        Check.notNull(r.getDocument(), "document exists after an error");
        return r;
    }

    /**
     * A typical XHTML 1.0 Strict page: DOCTYPE with its identifiers, XML declaration that is not a node, namespace declarations as
     * attributes, <code>xml:lang</code> in the XML namespace, empty element tags, entity references and the document properties.
     * <p>
     * 전형적인 XHTML 1.0 Strict 페이지: 식별자가 있는 DOCTYPE, 노드가 아닌 XML 선언, 속성이 되는 네임스페이스 선언, XML 네임스페이스의
     * <code>xml:lang</code>, 빈 요소 태그, 엔티티 참조, 문서 속성.
     */
    public void testStrictDocument() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + STRICT_DOCTYPE + "\n"
                + "<html xmlns=\"" + NS + "\" xml:lang=\"en\" lang=\"en\"><head><title>T &amp; U</title></head>"
                + "<body><p class=\"a b\">Hi<br/>there</p></body></html>\n";
        XhtmlResult r = parse(xml);
        Check.isNull(r.getError(), "well-formed");
        Check.isTrue(r.isWellFormed(), "isWellFormed");
        Check.isTrue(r.isXhtmlRoot(), "XHTML root");
        Check.equal("<!DOCTYPE html \"" + STRICT_PUBLIC + "\" \"" + STRICT_SYSTEM + "\">\n"
                + "<h|html>\n  @lang=\"en\"\n  @xml:lang{xml}=\"en\"\n  @xmlns{xmlns}=\"" + NS + "\"\n"
                + "  <h|head>\n    <h|title>\n      \"T & U\"\n"
                + "  <h|body>\n    <h|p>\n      @class=\"a b\"\n      \"Hi\"\n      <h|br>\n      \"there\"\n", XhtmlTreeDump.dump(r.getDocument()), "tree");
        Document d = r.getDocument();
        Check.isFalse(d.isHtmlDocument(), "not an HTML document (names are case sensitive)");
        Check.equal("application/xhtml+xml", d.getContentType(), "content type");
        Check.equal("http://example.org/dir/page.xhtml", d.getUrl(), "document URL");
        Check.equal(Document.NO_QUIRKS_MODE, d.getCompatibilityMode(), "XML documents are in no-quirks mode");
        Check.equal("CSS1Compat", d.getCompatMode(), "compat mode");
        Check.equal("UTF-8", d.getCharacterSet(), "character set");
        Check.equal(xml, r.getSource(), "the received source is kept unchanged");
        Check.equal("html", d.getDocumentElement().getLocalName(), "document element");
        Check.equal("html", d.getDoctype().getName(), "doctype name");
        Check.equal(STRICT_PUBLIC, d.getDoctype().getPublicId(), "doctype public id");
        Check.equal(STRICT_SYSTEM, d.getDoctype().getSystemId(), "doctype system id");
        Check.equal(1, r.getRefusedReferences().size(), "the external identifier is listed as named by the document (and never loaded)");
        Check.equal(STRICT_SYSTEM, r.getRefusedReferences().get(0), "the system identifier of the DOCTYPE");
    }

    /**
     * XML names are case sensitive: nothing is lower-cased, and a start tag and an end tag that differ in case are an error (the HTML
     * parser would accept it).
     * <p>
     * XML 이름은 대소문자를 구분한다: 아무것도 소문자로 바뀌지 않고, 대소문자만 다른 시작 태그와 종료 태그는 오류이다(HTML 파서라면 받아들인다).
     */
    public void testCaseSensitivity() {
        XhtmlResult r = parse("<HTML xmlns=\"" + NS + "\"><BODY ID=\"x\" onClick=\"y\"><P>text</P></BODY></HTML>");
        Check.isNull(r.getError(), "well-formed");
        Element root = r.getDocument().getDocumentElement();
        Check.equal("HTML", root.getLocalName(), "name keeps its case");
        Check.isTrue(r.isXhtmlRoot() == false, "an element named HTML is not the XHTML root html (case sensitive)");
        Element body = (Element) root.getFirstChild();
        Check.equal("BODY", body.getLocalName(), "BODY");
        Check.equal("x", body.getAttribute("ID"), "attribute names keep their case");
        Check.isNull(body.getAttribute("id"), "id is another name");
        Check.equal("y", body.getAttribute("onClick"), "onClick");
        Check.isNull(body.getAttribute("onclick"), "onclick is another name");
        bad("<html xmlns=\"" + NS + "\">\n<body>\n<p>text</P>\n</body>\n</html>", 3);
    }

    /**
     * Malformed documents are errors with the position of the first problem; there is never an HTML-style recovery.
     * <p>
     * 잘못된 문서는 첫 문제의 위치가 있는 오류이다. HTML 식의 복구는 없다.
     */
    public void testMalformedDocuments() {
        String h = "<html xmlns=\"" + NS + "\">";
        bad(h + "\n<body>\n<p>text\n</body>\n</html>", 4);
        bad("<?xml version=\"1.0\"?>\n<a>\n<b x='1' x='2'/>\n</a>", 3);
        bad("<a>\n<b class=x/>\n</a>", 2);
        bad("<a>\n<br>\n</a>", 3);
        bad("<a/><b/>", 1);
        bad("text<a/>", 1);
        bad("<a:b/>", 1);
        bad("<a>\n<!-- a -- b -->\n</a>", 2);
        bad("<a b=\"<\"/>", 1);
        bad("<a b=\"x & y\"/>", 1);
        bad("<a>\n]]>\n</a>", 2);
        bad("<a>&#0;</a>", 1);
        bad("<a>&#xD800;</a>", 1);
        bad("<a>\n" + (char) 1 + "\n</a>", 2);
        bad("<a>\n<b>", 2);
        bad("", 1);
        bad(" <?xml version=\"1.0\"?><a/>", 1);
        bad("<a>&nbsp;</a>", 1);
        bad("<a>\n&foo;\n</a>", 2);
        bad("<!DOCTYPE html [ <!ENTITY e \"x\" ]><html/>", 1);
        XhtmlResult r = bad(h + "<body><p>ok</p><p></b></body></html>", 1);
        // 오류 앞까지 만든 부분 트리는 문서에 남지만 호출자는 이를 쓰지 않는다.
        Check.notNull(r.getDocument().getDocumentElement(), "partial tree before the error");
        Check.isTrue(r.getError().getSourceLine().indexOf("</b>") >= 0, "source line of the error: " + r.getError().getSourceLine());
    }

    /**
     * The source line and column of an error refer to the original text even if the DOCTYPE had to be rewritten for the parser.
     * <p>
     * DOCTYPE 을 파서를 위해 다시 썼더라도 오류의 소스 줄과 열은 원문 텍스트를 가리킨다.
     */
    public void testErrorPositionAfterDoctypeRewrite() {
        String head = STRICT_DOCTYPE + "<html xmlns=\"" + NS + "\"><body><p>&nbsp;&nbsp;&nbsp;&copy;</p><p>a</q></body></html>";
        XhtmlResult r = parse(head);
        Check.notNull(r.getError(), "error");
        int at = head.indexOf("</q>");
        Check.equal(1, r.getError().getLine(), "line");
        Check.isTrue(r.getError().getColumn() >= at + 1 && r.getError().getColumn() <= at + 6, "column " + r.getError().getColumn() + " refers to the original text near " + (at + 1));
        // 줄 바꿈이 있는 DOCTYPE 뒤의 줄 번호도 원문과 같다.
        String multi = "<!DOCTYPE html PUBLIC \"" + STRICT_PUBLIC + "\"\n   \"" + STRICT_SYSTEM + "\">\n<html xmlns=\"" + NS + "\">\n<body>\n<q></p>\n</body></html>";
        XhtmlResult m = parse(multi);
        Check.notNull(m.getError(), "error");
        Check.equal(5, m.getError().getLine(), "line after a DOCTYPE that spans two lines");
    }

    /**
     * Named references: with the DOCTYPE of an XHTML DTD the HTML names work in text and in attribute values, other documents need a
     * declaration, and what the document declares itself wins.
     * <p>
     * 이름 있는 참조: XHTML DTD 의 DOCTYPE 이 있으면 HTML 이름이 본문과 속성 값에서 동작하고, 다른 문서는 선언이 필요하며, 문서가 직접 선언한 것이
     * 우선한다.
     */
    public void testNamedReferences() {
        String body = "<html xmlns=\"" + NS + "\"><body><p title=\"a&nbsp;b&amp;c\">&nbsp;&copy;&eacute;&hearts;&amp;&lt;&gt;&quot;&apos;&#x41;&#66;</p></body></html>";
        XhtmlResult r = parse(STRICT_DOCTYPE + body);
        Check.isNull(r.getError(), "XHTML 1.0 Strict DOCTYPE: " + r.getError());
        Element p = (Element) r.getDocument().getDocumentElement().getFirstChild().getFirstChild();
        String expected = "" + (char) 0xA0 + (char) 0xA9 + (char) 0xE9 + (char) 0x2665 + "&<>\"'AB";
        Check.equal(expected, p.getTextContent(), "text of the references");
        Check.equal("a" + (char) 0xA0 + "b&c", p.getAttribute("title"), "attribute value with a named reference");
        Check.equal(1, p.getChildNodes().getLength(), "the references leave a single text node");
        // 시스템 식별자만으로 알려진 DTD 를 지정해도 같다.
        XhtmlResult s = parse("<!DOCTYPE html SYSTEM \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" + body);
        Check.isNull(s.getError(), "system identifier of a W3C DTD only: " + s.getError());
        // 대문자 이름과 두 코드 포인트로 된 참조도 HTML 문자 참조 표를 따른다.
        String math = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN\" \"http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd\">"
                + "<html xmlns=\"" + NS + "\"><body>&AMP;&LT;&NotEqualTilde;</body></html>";
        XhtmlResult m = parse(math);
        Check.isNull(m.getError(), "MathML DTD: " + m.getError());
        Check.equal("&<" + NamedCharacterReferences.lookup("NotEqualTilde;"), m.getDocument().getDocumentElement().getTextContent(), "uppercase and two code point names");
        Check.equal(2, NamedCharacterReferences.lookup("NotEqualTilde;").length(), "NotEqualTilde is two code units long");
        // 알려진 DTD 라도 표에 없는 이름은 선언되지 않은 엔티티이다.
        notWellFormed(parse(STRICT_DOCTYPE + "<html xmlns=\"" + NS + "\"><body>&foo;</body></html>"), "unknown name with a known DTD");
        // 다른 문서에서는 미리 정의된 다섯 개 밖의 이름이 모두 오류이다.
        notWellFormed(parse("<!DOCTYPE html>" + body), "HTML5 DOCTYPE has no entity files");
        notWellFormed(parse(body), "no DOCTYPE");
        notWellFormed(parse("<!DOCTYPE html SYSTEM \"http://example.org/x.dtd\">" + body), "unknown DTD");
        notWellFormed(parse("<!DOCTYPE html [ ]>" + body), "internal subset only");
        // 문서가 직접 선언한 엔티티가 우선한다. 내부 서브셋만 있는 문서에서도 선언한 엔티티는 동작한다.
        XhtmlResult own = parse("<!DOCTYPE html PUBLIC \"" + STRICT_PUBLIC + "\" \"" + STRICT_SYSTEM + "\" [ <!ENTITY nbsp \"X\"> <!ENTITY brand \"Acme &amp; Co\"> ]>"
                + "<html xmlns=\"" + NS + "\"><body>[&nbsp;][&brand;][&eacute;]</body></html>");
        Check.isNull(own.getError(), "own declarations: " + own.getError());
        Check.equal("[X][Acme & Co][" + (char) 0xE9 + "]", own.getDocument().getDocumentElement().getTextContent(), "own definition wins, others come from the DTD");
        XhtmlResult sub = parse("<!DOCTYPE a [<!ENTITY e \"val\">]><a>&e;</a>");
        Check.isNull(sub.getError(), "internal subset only");
        Check.equal("val", sub.getDocument().getDocumentElement().getTextContent(), "internal entity");
        Check.equal("a", sub.getDocument().getDoctype().getName(), "doctype name");
    }

    /**
     * CDATA sections, comments and processing instructions become nodes (before, inside and after the root element); the XML
     * declaration does not.
     * <p>
     * CDATA 섹션, 주석, 처리 명령은 노드가 된다(루트 요소의 앞, 안, 뒤). XML 선언은 노드가 아니다.
     */
    public void testCdataCommentsAndInstructions() {
        Check.equal("<!--before-->\n<?pi1 data?>\n<r>\n  <!--c1-->\n  <![CDATA[<b>&amp;]]>\n  \"t\"\n  <?pi2 ?>\n<!--after-->\n",
                dump("<?xml version=\"1.0\"?><!--before--><?pi1 data?><r><!--c1--><![CDATA[<b>&amp;]]>t<?pi2?></r><!--after-->"), "tree");
        Check.equal("<r>\n  \"a\"\n  <![CDATA[]]>\n  \"b\"\n", dump("<r>a<![CDATA[]]>b</r>"), "empty CDATA section splits the text");
        Check.equal("<?xml-stylesheet type=\"text/css\" href=\"s.css\"?>\n<r>\n", dump("<?xml-stylesheet type=\"text/css\" href=\"s.css\"?><r/>"),
                "xml-stylesheet instruction is a node (not interpreted by this layer)");
        // 텍스트는 한 노드로 모이고 줄 끝은 LF 로 정규화된다.
        Check.equal("<r>\n  \"a\\nb\\nc&d\"\n", dump("<r>a\r\nb\rc&amp;d</r>"), "line ends are normalized by the XML parser, references merged into one text node");
    }

    /**
     * Namespaces: prefixes, default namespace, foreign elements inside XHTML, namespaced attributes and the declarations themselves.
     * <p>
     * 네임스페이스: 접두사, 기본 네임스페이스, XHTML 안의 외부 요소, 네임스페이스가 있는 속성, 선언 자체.
     */
    public void testNamespaces() {
        String xml = "<h:html xmlns:h=\"" + NS + "\" xmlns:xl=\"" + Namespaces.XLINK + "\"><h:body>"
                + "<svg xmlns=\"" + Namespaces.SVG + "\" viewBox=\"0 0 1 1\"><a xl:href=\"#x\"/></svg><plain xmlns=\"\"/></h:body></h:html>";
        Check.equal("<h|h:html>\n  @xmlns:h{xmlns}=\"" + NS + "\"\n  @xmlns:xl{xmlns}=\"" + Namespaces.XLINK + "\"\n  <h|h:body>\n"
                + "    <svg|svg>\n      @viewBox=\"0 0 1 1\"\n      @xmlns{xmlns}=\"" + Namespaces.SVG + "\"\n      <svg|a>\n        @xl:href{xlink}=\"#x\"\n"
                + "    <plain>\n      @xmlns{xmlns}=\"\"\n", dump(xml), "tree (an xmlns=\"\" declaration stays an attribute)");
        XhtmlResult r = parse(xml);
        Element root = r.getDocument().getDocumentElement();
        Check.equal("h", root.getPrefix(), "prefix");
        Check.equal("html", root.getLocalName(), "local name");
        Check.equal(NS, root.getNamespaceURI(), "namespace");
        Check.isTrue(r.isXhtmlRoot(), "a prefixed html element in the XHTML namespace is the XHTML root");
        Check.isFalse(parse("<html/>").isXhtmlRoot(), "html without a namespace is not XHTML");
        Check.isFalse(parse("<a xmlns=\"" + NS + "\"/>").isXhtmlRoot(), "another root element is generic XML");
    }

    /**
     * The elements are the same nodes that the HTML parser makes for the same markup (class, namespace, names, attributes), and the
     * <code>template</code> contents and the parser flags of <code>script</code> are set as the HTML parser does.
     * <p>
     * 요소는 같은 마크업에 대해 HTML 파서가 만드는 것과 같은 노드이다(클래스, 네임스페이스, 이름, 속성). <code>template</code> 내용과
     * <code>script</code> 의 파서 플래그도 HTML 파서처럼 지정된다.
     */
    public void testSameTreeAsHtmlParser() {
        String markup = "<!DOCTYPE html><html><head><title>t</title><script>var a = 1 &lt; 2;</script><style>p { color: red }</style></head>"
                + "<body id=\"b\"><p class=\"x y\" data-k=\"v\">one<br/>two <b>bold</b></p><ul><li>a</li><li>b</li></ul>"
                + "<template><p>in</p></template><!--c--><img src=\"a.png\" alt=\"\"/></body></html>";
        // HTML 파서는 script/style 의 내용을 원문으로 읽으므로 비교용 마크업에서는 이스케이프를 되돌린다.
        String htmlMarkup = markup.replace("&lt;", "<");
        Document html = HtmlParser.parseDocument(htmlMarkup);
        XhtmlResult x = parse(markup.replace("<html>", "<html xmlns=\"" + NS + "\">"));
        Check.isNull(x.getError(), "XHTML well-formed: " + x.getError());
        Check.equal(XhtmlTreeDump.dump(html, true), XhtmlTreeDump.dump(x.getDocument(), true), "same tree");
        Element xs = (Element) x.getDocument().getElementsByTagName("script").item(0);
        Check.equal(html.getElementsByTagName("script").item(0).getClass(), xs.getClass(), "same element class");
        Check.isTrue(ParserNodeData.scriptFlags(xs).isParserInserted(), "script is parser inserted");
        Check.isFalse(ParserNodeData.scriptFlags(xs).isAlreadyStarted(), "script has not started");
        Element template = (Element) x.getDocument().getElementsByTagName("template").item(0);
        Check.isNull(template.getFirstChild(), "template has no children of its own");
        DocumentFragment content = ParserNodeData.getTemplateContents(template);
        Check.notNull(content, "template contents exist");
        Check.equal("p", ((Element) content.getFirstChild()).getLocalName(), "the child is in the template contents");
        Check.equal(content.getOwnerDocument(), x.getDocument(), "owned by the document");
        Check.equal("var a = 1 < 2;", xs.getTextContent(), "script text after reference expansion");
    }

    /**
     * Events recorded from the parser listener.
     * <p>
     * 파서 리스너에서 기록한 이벤트.
     */
    private static final class Recorder extends ParserListenerAdapter {
        /**
         * The events.
         * <p>
         * 이벤트들.
         */
        final List<String> events = new ArrayList<String>();

        /**
         * Records the document mode.
         * <p>
         * 문서 모드를 기록한다.
         *
         * @param mode mode / 모드
         */
        public void documentModeDetermined(int mode) {
            events.add("mode:" + mode);
        }

        /**
         * Records an inserted element.
         * <p>
         * 삽입된 요소를 기록한다.
         *
         * @param element the element / 요소
         */
        public void elementInserted(Element element) {
            events.add("ins:" + element.getLocalName());
        }

        /**
         * Records a closed element.
         * <p>
         * 닫힌 요소를 기록한다.
         *
         * @param element the element / 요소
         */
        public void elementClosed(Element element) {
            events.add("close:" + element.getLocalName());
        }

        /**
         * Records a script, its text and flag, and tries the parser control.
         * <p>
         * 스크립트와 그 텍스트, 플래그를 기록하고 파서 제어를 시험한다.
         *
         * @param script  the script / 스크립트
         * @param control the control / 제어
         */
        public void scriptReady(Element script, ParserControl control) {
            events.add("script:" + script.getTextContent() + "|" + script.getAttribute("src") + "|" + ParserNodeData.scriptFlags(script).isParserInserted());
            control.pause();
            control.insertAtInsertionPoint("<b>ignored</b>");
            events.add("control:" + control.isFragment() + "|" + (control.getDocument() == script.getOwnerDocument()));
        }

        /**
         * Records a style.
         * <p>
         * style 을 기록한다.
         *
         * @param style the style / style
         */
        public void styleReady(Element style) {
            events.add("style:" + style.getTextContent());
        }

        /**
         * Records a style sheet link.
         * <p>
         * 스타일시트 link 를 기록한다.
         *
         * @param link the link / link
         */
        public void stylesheetFound(Element link) {
            events.add("stylesheet:" + link.getAttribute("href"));
        }

        /**
         * Records an image.
         * <p>
         * 이미지를 기록한다.
         *
         * @param img the image / 이미지
         */
        public void imageFound(Element img) {
            events.add("img:" + img.getAttribute("src"));
        }

        /**
         * Records the base URL.
         * <p>
         * 기준 URL 을 기록한다.
         *
         * @param href     the attribute / 속성
         * @param resolved the resolved URL / 해석한 URL
         */
        public void baseHrefChanged(String href, String resolved) {
            events.add("base:" + href + "|" + resolved);
        }

        /**
         * Records a refresh.
         * <p>
         * 새로고침을 기록한다.
         *
         * @param content the content / content
         */
        public void metaRefresh(String content) {
            events.add("refresh:" + content);
        }

        /**
         * Records a title.
         * <p>
         * 제목을 기록한다.
         *
         * @param title the title / 제목
         */
        public void titleReady(String title) {
            events.add("title:" + title);
        }
    }

    /**
     * The listener gets the same notifications in the same order as it gets from the HTML parser for the same elements.
     * <p>
     * 리스너는 같은 요소에 대해 HTML 파서에서 받는 것과 같은 통지를 같은 순서로 받는다.
     */
    public void testListenerNotifications() {
        String xml = "<html xmlns=\"" + NS + "\"><head><title>T1</title><title>T2</title><base href=\"sub/\"/>"
                + "<meta http-equiv=\"Refresh\" content=\"5;url=x\"/><link rel=\"Stylesheet foo\" href=\"a.css\"/><link rel=\"icon\" href=\"i\"/>"
                + "<style>p{}</style><script>var a=1;</script><script src=\"x.js\"></script></head><body><img src=\"a.png\"/></body></html>";
        XhtmlParser p = new XhtmlParser();
        Recorder rec = new Recorder();
        p.setListener(rec);
        XhtmlResult r = p.parse(xml, "http://example.org/dir/page.xhtml");
        Check.isNull(r.getError(), "well-formed");
        String[] expected = {"mode:0", "ins:html", "ins:head", "ins:title", "close:title", "title:T1", "ins:title", "close:title", "ins:base",
            "base:sub/|http://example.org/dir/sub/", "close:base", "ins:meta", "refresh:5;url=x", "close:meta", "ins:link", "stylesheet:a.css", "close:link",
            "ins:link", "close:link", "ins:style", "close:style", "style:p{}", "ins:script", "close:script", "script:var a=1;|null|true",
            "control:false|true", "ins:script", "close:script", "script:|x.js|true", "control:false|true", "close:head", "ins:body", "ins:img", "img:a.png",
            "close:img", "close:body", "close:html"};
        Check.equal(join(expected), join(rec.events.toArray(new String[0])), "notifications");
        Check.equal("http://example.org/dir/sub/", r.getDocument().getBaseUri(), "the first base element fixes the base URL");
        Check.same(r.getDocument(), p.getDocument(), "the parser hands out its document");
        // 한 파서는 한 번만 해석한다.
        Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Parses a second time.
             * <p>
             * 두 번째로 해석한다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                XhtmlParser used = new XhtmlParser();
                used.parse("<a/>", null);
                used.parse("<a/>", null);
            }
        }, "a parser parses once");
    }

    /**
     * Checks that a result is a well-formedness error.
     * <p>
     * 결과가 well-formed 오류인지 확인한다.
     *
     * @param r       the result / 결과
     * @param message the failure description / 실패 설명
     */
    private static void notWellFormed(XhtmlResult r, String message) {
        Check.notNull(r.getError(), message);
        Check.equal(XmlParseError.Kind.NOT_WELL_FORMED, r.getError().getKind(), message);
    }

    /**
     * Joins strings with line feeds for readable failure messages.
     * <p>
     * 읽기 좋은 실패 메시지를 위해 문자열을 줄 바꿈으로 잇는다.
     *
     * @param parts the strings / 문자열
     * @return the joined text / 이은 문자열
     */
    private static String join(String[] parts) {
        StringBuilder sb = new StringBuilder();
        // 문자열을 한 줄씩 잇는다.
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]).append('\n');
        }
        return sb.toString();
    }

    /**
     * Arguments, a prepared document and the other use rules of the parser.
     * <p>
     * 인수, 준비한 문서, 파서의 그 밖의 사용 규칙.
     */
    public void testUsageRules() {
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Parses null.
             * <p>
             * null 을 해석한다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                XhtmlParser.parseDocument((String) null, null);
            }
        }, "null text");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Uses an HTML document.
             * <p>
             * HTML 문서를 쓴다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                new XhtmlParser(Document.createHtmlDocument());
            }
        }, "HTML document refused");
        Document prepared = Document.createXmlDocument("application/xhtml+xml");
        XhtmlParser p = new XhtmlParser(prepared);
        XhtmlResult r = p.parse("<a/>", null);
        Check.same(prepared, r.getDocument(), "a prepared document is filled");
        Check.equal("about:blank", prepared.getUrl(), "no URL given, the default stays");
        // 앞의 바이트 순서 표시 문자는 건너뛰고 원문에는 그대로 남는다.
        String withBom = (char) 0xFEFF + "<r>x</r>";
        XhtmlResult b = XhtmlParser.parseDocument(withBom, null);
        Check.isNull(b.getError(), "a leading byte order mark character is not content");
        Check.equal(withBom, b.getSource(), "but it stays in the source");
        XhtmlResult be = XhtmlParser.parseDocument((char) 0xFEFF + "<r>\n<b></c></r>", null);
        Check.equal(2, be.getError().getLine(), "line");
        Check.isTrue(be.getError().getColumn() >= 5 && be.getError().getColumn() <= 8, "column counts the skipped character: " + be.getError().getColumn());
        Check.contains(be.getError().toString(), "NOT_WELL_FORMED at 2:", "toString");
    }

    /**
     * Deeply nested but allowed documents are built without recursion problems.
     * <p>
     * 깊지만 허용되는 문서는 재귀 문제 없이 만들어진다.
     */
    public void testDeepNesting() {
        StringBuilder sb = new StringBuilder("<d xmlns=\"" + NS + "\">");
        // 500 단계의 중첩 요소를 만든다(기본 상한 512 이하).
        for (int i = 0; i < 500; i++) {
            sb.append("<e>");
        }
        sb.append("x");
        // 닫는 태그도 500 개 쓴다.
        for (int i = 0; i < 500; i++) {
            sb.append("</e>");
        }
        sb.append("</d>");
        XhtmlResult r = parse(sb.toString());
        Check.isNull(r.getError(), "500 levels are fine");
        Node n = r.getDocument().getDocumentElement();
        int depth = 0;
        // 가장 안쪽까지 내려가며 깊이를 센다.
        while (n.getFirstChild() instanceof Element) {
            n = n.getFirstChild();
            depth++;
        }
        Check.equal(500, depth, "depth");
        Check.equal("x", n.getTextContent(), "innermost text");
    }
    /**
     * Several threads parse different documents at the same time (the parser factory is shared) and each gets its own correct tree.
     * <p>
     * 여러 스레드가 서로 다른 문서를 동시에 해석해도(파서 팩터리는 공유한다) 각자 자기의 올바른 트리를 얻는다.
     *
     * @throws Exception if a thread is interrupted / 스레드가 중단되면
     */
    public void testConcurrentParses() throws Exception {
        final int n = 8;
        final String[] results = new String[n];
        final Throwable[] failures = new Throwable[n];
        Thread[] threads = new Thread[n];
        // 스레드마다 번호가 다른 문서를 여러 번 해석한다.
        for (int i = 0; i < n; i++) {
            final int id = i;
            threads[i] = new Thread() {
                /**
                 * Parses documents that contain the number of the thread.
                 * <p>
                 * 스레드 번호가 든 문서를 해석한다.
                 */
                public void run() {
                    try {
                        String last = null;
                        // 같은 스레드에서 반복해 해석하며 마지막 결과를 보관한다.
                        for (int k = 0; k < 30; k++) {
                            XhtmlResult r = XhtmlParser.parseDocument("<r xmlns=\"" + NS + "\"><i n=\"" + id + "\">" + id + "</i></r>", null);
                            last = r.getError() != null ? "ERROR " + r.getError() : r.getDocument().getDocumentElement().getTextContent();
                        }
                        results[id] = last;
                    } catch (Throwable t) {
                        failures[id] = t;
                    }
                }
            };
            threads[i].start();
        }
        // 모든 스레드가 끝나기를 기다린 뒤 결과를 확인한다.
        for (int i = 0; i < n; i++) {
            threads[i].join();
            Check.isNull(failures[i], "thread " + i + " failed: " + failures[i]);
            Check.equal(String.valueOf(i), results[i], "tree of thread " + i);
        }
    }
}
