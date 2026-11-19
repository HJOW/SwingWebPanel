package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the parser facade and its integration points (plan items P07-01, P07-06, P07-07, P07-08): the listener callbacks for the
 * resources of a document, <code>document.write</code> and pausing for scripts, restarting with another encoding when a late
 * <code>meta</code> element asks for it, document modes, template contents, form owners, script flags, parse errors with positions,
 * robustness against huge and random input.
 * <p>
 * 파서 파사드와 그 연동 지점의 시험(계획 P07-01, P07-06, P07-07, P07-08): 문서가 참조하는 자원에 대한 리스너 콜백, <code>document.write</code> 와
 * 스크립트를 위한 일시 정지, 늦게 나온 <code>meta</code> 요소가 요청하는 다른 인코딩으로의 재시작, 문서 모드, template 내용, 폼 소유자, script 플래그,
 * 위치가 있는 파싱 오류, 거대한 입력과 무작위 입력에 대한 견고성.
 */
public class HtmlParserTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HtmlParserTest() {
    }

    /**
     * A listener that records its callbacks as short strings.
     * <p>
     * 콜백을 짧은 문자열로 기록하는 리스너.
     */
    private static final class Recorder extends ParserListenerAdapter {
        /**
         * The recorded events.
         * <p>
         * 기록된 이벤트.
         */
        final List<String> events = new ArrayList<String>();

        /**
         * Number of elements reported by elementInserted.
         * <p>
         * elementInserted 로 보고된 요소 수.
         */
        int inserted;

        /**
         * Number of elements reported by elementClosed.
         * <p>
         * elementClosed 로 보고된 요소 수.
         */
        int closed;

        /**
         * Records a document mode.
         * <p>
         * 문서 모드를 기록한다.
         *
         * @param mode mode / 모드
         */
        public void documentModeDetermined(int mode) {
            events.add("mode:" + mode);
        }

        /**
         * Counts an inserted element.
         * <p>
         * 삽입된 요소를 센다.
         *
         * @param element element / 요소
         */
        public void elementInserted(Element element) {
            inserted++;
        }

        /**
         * Counts a closed element.
         * <p>
         * 닫힌 요소를 센다.
         *
         * @param element element / 요소
         */
        public void elementClosed(Element element) {
            closed++;
        }

        /**
         * Records a script.
         * <p>
         * 스크립트를 기록한다.
         *
         * @param script  script / 스크립트
         * @param control control / 제어
         */
        public void scriptReady(Element script, ParserControl control) {
            String src = script.getAttribute("src");
            events.add("script:" + (src != null ? src : script.getTextContent()));
        }

        /**
         * Records a style element.
         * <p>
         * style 요소를 기록한다.
         *
         * @param style style / style
         */
        public void styleReady(Element style) {
            events.add("style:" + style.getTextContent());
        }

        /**
         * Records a style sheet link.
         * <p>
         * 스타일시트 링크를 기록한다.
         *
         * @param link link / link
         */
        public void stylesheetFound(Element link) {
            events.add("css:" + link.getAttribute("href"));
        }

        /**
         * Records an image.
         * <p>
         * 이미지를 기록한다.
         *
         * @param img image / img
         */
        public void imageFound(Element img) {
            events.add("img:" + img.getAttribute("src"));
        }

        /**
         * Records the base URL.
         * <p>
         * 기준 URL 을 기록한다.
         *
         * @param href     attribute / 속성
         * @param resolved resolved URL / 해석된 URL
         */
        public void baseHrefChanged(String href, String resolved) {
            events.add("base:" + href + "|" + resolved);
        }

        /**
         * Records a refresh.
         * <p>
         * 새로고침을 기록한다.
         *
         * @param content content / content
         */
        public void metaRefresh(String content) {
            events.add("refresh:" + content);
        }

        /**
         * Records a title.
         * <p>
         * 제목을 기록한다.
         *
         * @param title title / 제목
         */
        public void titleReady(String title) {
            events.add("title:" + title);
        }

        /**
         * Records a declared encoding.
         * <p>
         * 선언된 인코딩을 기록한다.
         *
         * @param encodingName encoding / 인코딩
         */
        public void charsetDeclared(String encodingName) {
            events.add("charset:" + encodingName);
        }
    }

    /**
     * Joins the recorded events with spaces.
     * <p>
     * 기록된 이벤트를 공백으로 잇는다.
     *
     * @param events events / 이벤트
     * @return text / 문자열
     */
    private static String join(List<String> events) {
        StringBuilder sb = new StringBuilder();
        // 이벤트를 순서대로 공백으로 구분해 모은다.
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(events.get(i));
        }
        return sb.toString();
    }

    /**
     * The listener learns about the resources of a document in document order, and the base URL is stored in the document.
     * <p>
     * 리스너가 문서의 자원을 문서 순서대로 알게 되고 기준 URL 이 문서에 저장된다.
     */
    public void testListenerEvents() {
        Recorder r = new Recorder();
        String src = "<!DOCTYPE html><html><head><title>T</title><base href=\"sub/\"><meta http-equiv=refresh content=\"5;url=a\">"
                + "<meta charset=utf-8><link rel=\"Stylesheet\" href=a.css><link rel=icon href=i><style>p{}</style><script src=s.js></script>"
                + "<script>var a;</script></head><body><img src=i.png><p>x</body>";
        Document doc = HtmlParser.parseDocument(src, "http://host/dir/page.html", r, true);
        Check.equal("mode:0 title:T base:sub/|http://host/dir/sub/ refresh:5;url=a charset:UTF-8 css:a.css style:p{} script:s.js script:var a; img:i.png",
                join(r.events), "events");
        Check.equal("http://host/dir/sub/", doc.getBaseUri(), "base URI of the document");
        Check.equal("http://host/dir/page.html", doc.getUrl(), "URL stays");
        Check.isTrue(r.inserted >= 12, "inserted elements are reported: " + r.inserted);
        Check.equal(r.inserted, r.closed, "every inserted element is closed once");
    }

    /**
     * The first base element with an href fixes the base URL; later ones and a missing document mode are handled.
     * <p>
     * href 가 있는 첫 base 요소가 기준 URL 을 정하고, 이후 것은 무시되며, DOCTYPE 이 없으면 quirks 모드가 보고된다.
     */
    public void testBaseAndModes() {
        Recorder r = new Recorder();
        Document doc = HtmlParser.parseDocument("<base><base href=/a/><base href=/b/><p>x", "http://h/x/y", r, true);
        Check.equal("mode:2 base:/a/|http://h/a/", join(r.events), "events without doctype");
        Check.equal("http://h/a/", doc.getBaseUri(), "first base with href wins");
        Check.equal(Document.QUIRKS_MODE, doc.getCompatibilityMode(), "quirks mode");
        Document doc2 = HtmlParser.parseDocument("<!DOCTYPE html>", null, null, true);
        Check.equal(Document.NO_QUIRKS_MODE, doc2.getCompatibilityMode(), "standards mode");
        Document doc3 = HtmlParser.parseDocument("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"x\">", null, null, true);
        Check.equal(Document.LIMITED_QUIRKS_MODE, doc3.getCompatibilityMode(), "limited quirks mode");
        Document doc4 = Document.createHtmlDocument();
        HtmlParser p = new HtmlParser(doc4, "<p>x");
        p.setSrcdoc(true);
        p.run();
        Check.equal(Document.NO_QUIRKS_MODE, doc4.getCompatibilityMode(), "srcdoc documents stay in no-quirks mode");
    }

    /**
     * A script can write markup at the insertion point (document.write): it is parsed right after the script, before the rest.
     * <p>
     * 스크립트가 삽입 지점에 마크업을 쓸 수 있다(document.write): 스크립트 바로 뒤, 나머지 입력 앞에서 파싱된다.
     */
    public void testDocumentWrite() {
        Document doc = Document.createHtmlDocument();
        HtmlParser p = new HtmlParser(doc, "<p>a<script>w</script>c<script>x</script>d");
        p.setListener(new ParserListenerAdapter() {
            /**
             * Writes markup for the first script and nested markup for the second.
             * <p>
             * 첫 스크립트에는 마크업을, 둘째에는 중첩된 마크업을 쓴다.
             *
             * @param script  script / 스크립트
             * @param control control / 제어
             */
            public void scriptReady(Element script, ParserControl control) {
                if ("w".equals(script.getTextContent())) {
                    control.insertAtInsertionPoint("<b>W</b>");
                } else if ("x".equals(script.getTextContent())) {
                    control.insertAtInsertionPoint("<i>X<script>y</script></i>");
                }
            }
        });
        p.run();
        Check.equal("| <html>\n|   <head>\n|   <body>\n|     <p>\n|       \"a\"\n|       <script>\n|         \"w\"\n|       <b>\n|         \"W\"\n"
                + "|       \"c\"\n|       <script>\n|         \"x\"\n|       <i>\n|         \"X\"\n|         <script>\n|           \"y\"\n"
                + "|       \"d\"\n", TreeDump.dumpChildren(doc), "tree after document.write");
    }

    /**
     * The parser can be paused while a script loads, and resumes where it stopped.
     * <p>
     * 스크립트를 불러오는 동안 파서를 멈추고 멈춘 곳에서 다시 시작할 수 있다.
     */
    public void testPauseAndResume() {
        Document doc = Document.createHtmlDocument();
        final HtmlParser p = new HtmlParser(doc, "<p>a<script src=s></script><p>b");
        p.setListener(new ParserListenerAdapter() {
            /**
             * Pauses the parser for the external script.
             * <p>
             * 외부 스크립트를 위해 파서를 멈춘다.
             *
             * @param script  script / 스크립트
             * @param control control / 제어
             */
            public void scriptReady(Element script, ParserControl control) {
                control.pause();
            }
        });
        p.run();
        Check.isTrue(p.isPaused(), "paused");
        Check.isFalse(p.isFinished(), "not finished while paused");
        String partial = TreeDump.dumpChildren(doc);
        Check.isTrue(partial.indexOf("\"b\"") < 0, "the rest is not parsed yet: " + partial);
        Check.isTrue(partial.indexOf("<script>") >= 0, "the script element exists: " + partial);
        p.resume();
        Check.isTrue(p.isFinished(), "finished after resume");
        Check.isTrue(TreeDump.dumpChildren(doc).indexOf("\"b\"") >= 0, "the rest is parsed after resume");
    }

    /**
     * Flags of script elements: parser-inserted in documents, "already started" in fragments.
     * <p>
     * script 요소의 플래그: 문서에서는 parser-inserted, 조각에서는 "already started".
     */
    public void testScriptFlags() {
        Document doc = HtmlParser.parseDocument("<script>a</script>", null, null, true);
        Element s = (Element) doc.getElementsByTagName("script").item(0);
        Check.isTrue(ParserNodeData.scriptFlags(s).isParserInserted(), "parser inserted");
        Check.isFalse(ParserNodeData.scriptFlags(s).isAlreadyStarted(), "not started");
        Element div = doc.createElement("div");
        DocumentFragment f = HtmlParser.parseFragment(div, "<script>b</script>");
        Element fs = (Element) f.getFirstChild();
        Check.isTrue(ParserNodeData.scriptFlags(fs).isAlreadyStarted(), "scripts of fragments never run");
        Check.equal("b", fs.getTextContent(), "script text");
    }

    /**
     * A late meta element restarts the parse with the right encoding; a confirming meta does not.
     * <p>
     * 늦게 나온 meta 요소는 올바른 인코딩으로 파싱을 다시 시작하게 하고, 확인해 주는 meta 는 그렇지 않다.
     *
     * @throws Exception on encoding problems / 인코딩 문제 시
     */
    public void testEncodingRestart() throws Exception {
        StringBuilder pad = new StringBuilder();
        while (pad.length() < 1100) {
            // meta 가 처음 1024 바이트 밖에 있게 공백을 채운다.
            pad.append(' ');
        }
        byte[] head = ("<html><head>" + pad + "<meta charset=euc-kr></head><body><p>").getBytes("US-ASCII");
        byte[] hangul = {(byte) 0xC7, (byte) 0xD1, (byte) 0xB1, (byte) 0xDB};
        byte[] all = new byte[head.length + hangul.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(hangul, 0, all, head.length, hangul.length);
        DecodedText first = EncodingSniffer.decode(all, null);
        Check.equal("windows-1252", first.getEncoding(), "the prescan does not reach the meta element");
        Document doc = Document.createHtmlDocument();
        HtmlParser p = new HtmlParser(doc, first);
        p.run();
        Check.equal("EUC-KR", doc.getCharacterSet(), "character set after restart");
        Check.equal("EUC-KR", p.getDecodedText().getEncoding(), "decoded text after restart");
        Check.isTrue(p.getDecodedText().isCertain(), "certain after restart");
        Element para = (Element) doc.getElementsByTagName("p").item(0);
        Check.equal("한글", para.getTextContent(), "korean text after restart");
        Check.equal(1, doc.getElementsByTagName("head").getLength(), "no leftovers of the first pass");
        Document again = HtmlParser.parseDocument(all, "windows-1252", "http://x/");
        Check.equal("WINDOWS-1252".toLowerCase(), again.getCharacterSet().toLowerCase(), "transport layer beats meta: no restart");
        byte[] same = ("<html><head>" + pad + "<meta charset=windows-1252></head><body>x").getBytes("US-ASCII");
        Document doc2 = Document.createHtmlDocument();
        HtmlParser p2 = new HtmlParser(doc2, EncodingSniffer.decode(same, null));
        p2.run();
        Check.isTrue(p2.getDecodedText().isCertain(), "a confirming meta makes the decision certain");
        Check.equal(1, doc2.getElementsByTagName("meta").getLength(), "parsed once");
    }

    /**
     * A script that already ran prevents a restart.
     * <p>
     * 이미 실행된 스크립트가 있으면 다시 시작하지 않는다.
     *
     * @throws Exception on encoding problems / 인코딩 문제 시
     */
    public void testNoRestartAfterScript() throws Exception {
        StringBuilder pad = new StringBuilder();
        while (pad.length() < 1100) {
            // meta 가 처음 1024 바이트 밖에 있게 공백을 채운다.
            pad.append(' ');
        }
        byte[] b = ("<html><head>" + pad + "<script>a</script><meta charset=euc-kr></head><body>x").getBytes("US-ASCII");
        Document doc = Document.createHtmlDocument();
        HtmlParser p = new HtmlParser(doc, EncodingSniffer.decode(b, null));
        final int[] scripts = {0};
        p.setListener(new ParserListenerAdapter() {
            /**
             * Counts the scripts.
             * <p>
             * 스크립트를 센다.
             *
             * @param script  script / 스크립트
             * @param control control / 제어
             */
            public void scriptReady(Element script, ParserControl control) {
                scripts[0]++;
            }
        });
        p.run();
        Check.equal(1, scripts[0], "the script is reported exactly once");
        Check.equal("windows-1252", p.getDecodedText().getEncoding(), "no restart after a script ran");
    }

    /**
     * Template contents are kept in the side table; form owners are recorded by the parser.
     * <p>
     * template 내용은 보조 표에 보관되고 폼 소유자는 파서가 기록한다.
     */
    public void testTemplateAndFormOwner() {
        Document doc = HtmlParser.parseDocument("<template id=t><p>a<b>b</template><form id=f><input id=i1><div><input id=i2></div></form><input id=i3>", null,
                null, true);
        Element t = doc.getElementById("t");
        Check.isNull(t.getFirstChild(), "the template element has no children");
        DocumentFragment c = ParserNodeData.getTemplateContents(t);
        Check.notNull(c, "template contents");
        Check.equal("p", ((Element) c.getFirstChild()).getLocalName(), "first node of the contents");
        Element f = doc.getElementById("f");
        Check.same(f, ParserNodeData.getFormOwner(doc.getElementById("i1")), "owner of the first input");
        Check.same(f, ParserNodeData.getFormOwner(doc.getElementById("i2")), "owner of the nested input");
        Check.isNull(ParserNodeData.getFormOwner(doc.getElementById("i3")), "input outside the form");
    }

    /**
     * Parse errors are collected with codes and positions, and the limit applies to the stored errors only.
     * <p>
     * 파싱 오류는 코드와 위치와 함께 수집되며 상한은 저장하는 오류에만 적용된다.
     */
    public void testErrors() {
        Document doc = Document.createHtmlDocument();
        HtmlParser p = new HtmlParser(doc, "<!DOCTYPE html>\n<p>a</div>\n<b &amp x=1 x=2>");
        p.run();
        List<String> codes = p.getErrorCodes();
        Check.isTrue(codes.contains("unexpected-end-tag"), "end tag without element: " + codes);
        Check.isTrue(codes.contains("duplicate-attribute"), "duplicate attribute: " + codes);
        ParseError e = null;
        // 첫 unexpected-end-tag 오류의 위치를 찾는다.
        for (int i = 0; i < p.getErrors().size(); i++) {
            if ("unexpected-end-tag".equals(p.getErrors().get(i).getCode())) {
                e = p.getErrors().get(i);
                break;
            }
        }
        Check.notNull(e, "the error object");
        Check.equal(2, e.getLine(), "line of the stray end tag");
        Check.isTrue(e.getColumn() >= 5, "column of the stray end tag");
        Document doc2 = Document.createHtmlDocument();
        HtmlParser p2 = new HtmlParser(doc2, "</a></b></c></d></e></f>");
        p2.setErrorLimit(3);
        p2.run();
        Check.equal(3, p2.getErrors().size(), "stored errors are limited");
        Check.isTrue(p2.getErrorCount() >= 6, "all errors are counted: " + p2.getErrorCount());
        Document doc3 = Document.createHtmlDocument();
        HtmlParser p3 = new HtmlParser(doc3, "<p>ok</p>");
        p3.run();
        Check.equal(1, p3.getErrorCount(), "no errors in a clean document except the missing doctype");
        Check.equal("missing-doctype", p3.getErrorCodes().get(0), "the only error");
    }

    /**
     * A parse listener also gets the parse errors.
     * <p>
     * 파싱 리스너는 파싱 오류도 받는다.
     */
    public void testErrorListener() {
        final List<String> got = new ArrayList<String>();
        HtmlParser p = new HtmlParser(Document.createHtmlDocument(), "<!DOCTYPE html><p>a</div>");
        p.setListener(new ParserListenerAdapter() {
            /**
             * Records the error code.
             * <p>
             * 오류 코드를 기록한다.
             *
             * @param error error / 오류
             */
            public void parseError(ParseError error) {
                got.add(error.getCode());
            }
        });
        p.run();
        Check.equal("[unexpected-end-tag]", got.toString(), "errors seen by the listener");
    }

    /**
     * Deeply nested and very long input is parsed without recursion problems.
     * <p>
     * 아주 깊게 중첩되거나 아주 긴 입력도 재귀 문제 없이 파싱된다.
     */
    public void testHugeInput() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html>");
        // 닫지 않은 div 를 20000 겹으로 중첩한다.
        for (int i = 0; i < 20000; i++) {
            sb.append("<div>");
        }
        sb.append("x");
        Recorder r = new Recorder();
        Document doc = HtmlParser.parseDocument(sb.toString(), null, r, true);
        Check.equal(20000 + 3, r.inserted, "html, head, body and 20000 divs");
        Check.notNull(doc.getDocumentElement(), "document element");
        StringBuilder text = new StringBuilder("<p>");
        // 1MB 쯤 되는 긴 텍스트를 만든다.
        for (int i = 0; i < 100000; i++) {
            text.append("0123456789");
        }
        Document big = HtmlParser.parseDocument(text.toString(), null, null, true);
        Check.equal(1000000, ((Element) big.getElementsByTagName("p").item(0)).getTextContent().length(), "one text node of a million characters");
        StringBuilder fmt = new StringBuilder();
        // 서식 요소를 열고 닫지 않은 채 단락을 반복해 재구성을 많이 일으킨다.
        for (int i = 0; i < 3000; i++) {
            fmt.append("<b><i><u>x<p>");
        }
        Document many = HtmlParser.parseDocument(fmt.toString(), null, null, true);
        Check.notNull(many.getDocumentElement(), "formatting heavy document");
    }

    /**
     * Random tag soup never throws and always yields a consistent tree.
     * <p>
     * 무작위 태그 뒤범벅 입력도 예외 없이 일관된 트리를 만든다.
     */
    public void testRandomInput() {
        String[] pieces = {"<", ">", "/", "<p>", "</p>", "<b>", "</b>", "<table>", "<tr>", "<td>", "</table>", "<div>", "</div>", "<a href=x>", "</a>",
                "<select>", "<option>", "</select>", "<svg>", "</svg>", "<math>", "<mi>", "<template>", "</template>", "<frameset>", "<body>", "<html>",
                "<head>", "<title>", "</title>", "<script>", "</script>", "<style>", "</style>", "<textarea>", "<!--", "-->", "<!DOCTYPE", "&amp;",
                "&#0;", "&", "x", " ", "\n", "=", "\"", "'", "<li>", "<h1>", "<form>", "<input>", "<br/>", "<nobr>", "<![CDATA[", "]]>", "<foreignObject>",
                "<caption>", "<colgroup>", "<col>", "<tbody>", "<th>", "<img src=a>", "<noscript>", "<plaintext>", "<xmp>", "<ruby>", "<rt>"};
        Random rnd = new Random(20261116L);
        // 무작위로 조각을 이어 붙인 입력 300 개를 파싱한다.
        for (int n = 0; n < 300; n++) {
            StringBuilder sb = new StringBuilder();
            int len = 1 + rnd.nextInt(40);
            // 구간에서 고른 조각을 길이만큼 이어 붙인다.
            for (int i = 0; i < len; i++) {
                sb.append(pieces[rnd.nextInt(pieces.length)]);
            }
            String input = sb.toString();
            try {
                Document doc = HtmlParser.parseDocument(input, null, null, rnd.nextBoolean());
                checkConsistent(doc, input);
                Element ctx = doc.createElement(new String[] {"div", "tr", "table", "select", "template", "body", "html", "td", "ul"}[rnd.nextInt(9)]);
                DocumentFragment f = HtmlParser.parseFragment(ctx, input);
                checkConsistent(f, input);
            } catch (RuntimeException ex) {
                Check.fail("exception for input [" + input + "]: " + ex);
            }
        }
    }

    /**
     * Checks that parent and sibling links of a tree are consistent.
     * <p>
     * 트리의 부모와 형제 연결이 일관된지 확인한다.
     *
     * @param root  root node / 루트 노드
     * @param input input for messages / 메시지용 입력
     */
    private static void checkConsistent(Node root, String input) {
        Node prev = null;
        // 자식을 순서대로 훑으며 연결을 확인한다.
        for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
            Check.same(root, c.getParentNode(), "parent of a child for [" + input + "]");
            Check.same(prev, c.getPreviousSibling(), "previous sibling for [" + input + "]");
            checkConsistent(c, input);
            prev = c;
        }
        Check.same(prev, root.getLastChild(), "last child for [" + input + "]");
    }

    /**
     * The namespaces of parsed elements and the encoding label of the document are the standard ones.
     * <p>
     * 파싱된 요소의 네임스페이스와 문서의 인코딩 이름은 표준대로이다.
     */
    public void testNamespaces() {
        Document doc = HtmlParser.parseDocument("<svg><circle/></svg><math><mi/></math><p>", null, null, true);
        Element circle = (Element) doc.getElementsByTagName("circle").item(0);
        Check.notNull(circle, "circle element");
        Check.equal(Namespaces.SVG, circle.getNamespaceURI(), "svg namespace");
        Element mi = (Element) doc.getElementsByTagName("mi").item(0);
        Check.equal(Namespaces.MATHML, mi.getNamespaceURI(), "mathml namespace");
        Element p = (Element) doc.getElementsByTagName("p").item(0);
        Check.equal(Namespaces.HTML, p.getNamespaceURI(), "html namespace");
    }
}
