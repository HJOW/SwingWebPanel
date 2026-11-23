package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * XHTML 1.0 documents through the real component (plan item P07-07-c, LEG-12): the prolog and the DOCTYPE of XHTML 1.0 Strict and
 * Transitional, the always standards mode of XML documents, <code>xml:lang</code> with <code>:lang()</code>, the named character
 * references of the XHTML DTDs without a DTD, empty element tags (<code>&lt;br/&gt;</code>, <code>&lt;script src="x.js"/&gt;</code>),
 * CDATA sections in scripts and styles, the <code>&lt;?xml-stylesheet?&gt;</code> instruction, the event handler attributes, a script that
 * cannot use <code>document.write</code>, and the XHTML syntax of a page that is served as <code>text/html</code>.
 * <p>
 * 실제 컴포넌트로 확인하는 XHTML 1.0 문서(계획 P07-07-c, LEG-12): XHTML 1.0 Strict 와 Transitional 의 prolog 와 DOCTYPE, XML 문서의 항상 표준 모드,
 * <code>xml:lang</code> 과 <code>:lang()</code>, DTD 없이 쓰는 XHTML DTD 의 이름 있는 문자 참조, 빈 요소 태그(<code>&lt;br/&gt;</code>,
 * <code>&lt;script src="x.js"/&gt;</code>), 스크립트와 스타일의 CDATA 구역, <code>&lt;?xml-stylesheet?&gt;</code> 처리 명령, 이벤트 핸들러 속성,
 * <code>document.write</code> 를 쓸 수 없는 스크립트, <code>text/html</code> 로 제공된 페이지의 XHTML 문법.
 */
public class LegacyXhtmlPageTest {

    /**
     * The DOCTYPE line of XHTML 1.0 Transitional.
     * <p>
     * XHTML 1.0 Transitional 의 DOCTYPE 줄.
     */
    private static final String TRANSITIONAL = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

    /**
     * The DOCTYPE line of XHTML 1.0 Strict.
     * <p>
     * XHTML 1.0 Strict 의 DOCTYPE 줄.
     */
    private static final String STRICT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The components of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyXhtmlPageTest() {
    }

    /**
     * Starts the server.
     * <p>
     * 서버를 시작한다.
     *
     * @throws IOException on failure / 실패 시
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.start();
    }

    /**
     * Releases the components and the server.
     * <p>
     * 컴포넌트와 서버를 해제한다.
     */
    public void tearDown() {
        // 만든 패널을 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Opens a page and waits until it ended loading.
     * <p>
     * 페이지를 열고 로딩이 끝날 때까지 기다린다.
     *
     * @param path the path of the page on the server / 서버의 페이지 경로
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private SwingWebPanel open(String path) throws Exception {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(server.url(path));
        long end = System.currentTimeMillis() + 60000;
        // 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!server.url(path).equals(p.getPage()) || p.getProgress() != 100 || p.getStatusText().startsWith("Loading"))
                && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(200);
        return p;
    }

    /**
     * Reads a value that a script stored in the session storage.
     * <p>
     * 스크립트가 세션 저장소에 넣은 값을 읽는다.
     *
     * @param p   the component / 컴포넌트
     * @param key the key / 키
     * @return the value, or null / 값. 없으면 null
     */
    private static String stored(SwingWebPanel p, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry e : p.getSessionStorage()) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * An XHTML 1.0 Transitional document: it is in standards mode, the elements are in the XHTML namespace with lower-case names, the
     * entities of the XHTML DTD work without a DTD, empty element tags and CDATA sections work, <code>xml:lang</code> is the language
     * of the element for <code>:lang()</code>, and the event handler attributes run.
     * <p>
     * XHTML 1.0 Transitional 문서: 표준 모드이고 요소는 소문자 이름으로 XHTML 네임스페이스에 있으며 XHTML DTD 의 엔티티는 DTD 없이 동작하고 빈 요소 태그와 CDATA 구역이
     * 동작하며 <code>xml:lang</code> 은 <code>:lang()</code> 에서 요소의 언어이고 이벤트 핸들러 속성이 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTransitionalDocument() throws Exception {
        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + TRANSITIONAL + "\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"><head><title>T&amp;T &copy; 2002</title>"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
                + "<style type=\"text/css\">/*<![CDATA[*/ p { color: rgb(7, 7, 7) } p:lang(fr) { color: rgb(4, 5, 6) } /*]]>*/</style>"
                + "<script type=\"text/javascript\">//<![CDATA[\nvar r = []; function add(k, v) { r.push(k + '=' + v); }\n//]]></script></head>"
                + "<body onload=\"add('onload', 'ran'); sessionStorage.setItem('r', r.join(';'));\">"
                + "<p id=\"fr\" xml:lang=\"fr\">bonjour&nbsp;&eacute;t&eacute;<br /></p><p id=\"en\">hello</p>"
                + "<script type=\"text/javascript\">//<![CDATA[\n"
                + "add('compat', document.compatMode); add('ns', document.documentElement.namespaceURI); add('tag', document.getElementById('en').tagName);"
                + "add('local', document.documentElement.localName); add('type', document.contentType); add('nbsp', document.getElementById('fr').textContent.charCodeAt(7));"
                + "add('frcolor', getComputedStyle(document.getElementById('fr')).color); add('encolor', getComputedStyle(document.getElementById('en')).color);"
                + "add('lang', document.getElementById('fr').getAttribute('xml:lang')); add('title', document.title);"
                + "try { document.write('x'); add('write', 'ok'); } catch (e) { add('write', e.name); }\n//]]></script></body></html>";
        server.routeFixed("/t.xhtml", TestHttpServer.Response.ok("application/xhtml+xml; charset=utf-8", xhtml));
        SwingWebPanel p = open("/t.xhtml");
        Check.equal(xhtml, p.getContent(), "the received XHTML is the source");
        String r = stored(p, "r");
        Check.notNull(r, "the scripts ran and the body onload ran");
        Check.isTrue(r.contains("compat=CSS1Compat"), "an XML document is in standards mode: " + r);
        Check.isTrue(r.contains("ns=http://www.w3.org/1999/xhtml"), "the XHTML namespace: " + r);
        Check.isTrue(r.contains("tag=p;"), "the tag name keeps its lower case in an XML document: " + r);
        Check.isTrue(r.contains("local=html"), "local name: " + r);
        Check.isTrue(r.contains("type=application/xhtml+xml"), "the content type: " + r);
        Check.isTrue(r.contains("nbsp=160"), "&nbsp; is U+00A0 without a DTD: " + r);
        Check.isTrue(r.contains("frcolor=rgb(4, 5, 6)"), "xml:lang=fr is the language for :lang(fr): " + r);
        Check.isTrue(r.contains("encolor=rgb(7, 7, 7)"), "the other paragraph is not French: " + r);
        Check.isTrue(r.contains("lang=fr"), "xml:lang as an attribute name: " + r);
        Check.isTrue(r.contains("title=T&T © 2002"), "the title with the references &amp; and &copy;: " + r);
        Check.isTrue(r.contains("write=InvalidStateError"), "document.write is not available in an XML document: " + r);
        Check.isTrue(r.contains("onload=ran"), "the onload handler ran: " + r);
        Check.equal("T&T © 2002", p.getTitle(), "the title of the component");
    }

    /**
     * An XHTML 1.0 Strict document with an external script and an external style sheet through empty element tags, and the
     * <code>xml-stylesheet</code> processing instruction: the files are requested with the right MIME types and apply.
     * <p>
     * 빈 요소 태그로 외부 스크립트와 외부 스타일시트를 가진 XHTML 1.0 Strict 문서와 <code>xml-stylesheet</code> 처리 명령: 파일을 요청해 적용한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExternalFilesAndStylesheetInstruction() throws Exception {
        String xhtml = "<?xml version=\"1.0\"?>\n<?xml-stylesheet type=\"text/css\" href=\"/pi.css\"?>\n" + STRICT + "\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>S</title>"
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/link.css\" />"
                + "<script type=\"text/javascript\" src=\"/ext.js\"></script><script type=\"text/javascript\" src=\"/ext2.js\" /></head>"
                + "<body><p id=\"a\">a</p><p id=\"b\">b</p>"
                + "<script type=\"text/javascript\">sessionStorage.setItem('colors', getComputedStyle(document.getElementById('a')).color + '|'"
                + " + getComputedStyle(document.getElementById('b')).backgroundColor + '|' + window.ext);</script></body></html>";
        server.routeFixed("/s.xhtml", TestHttpServer.Response.ok("application/xhtml+xml", xhtml));
        server.routeFixed("/pi.css", TestHttpServer.Response.ok("text/css", "#a { color: rgb(9, 8, 7) }"));
        server.routeFixed("/link.css", TestHttpServer.Response.ok("text/css", "#b { background-color: rgb(1, 1, 1) }"));
        server.routeFixed("/ext.js", TestHttpServer.Response.ok("application/javascript", "window.ext = 'one';"));
        server.routeFixed("/ext2.js", TestHttpServer.Response.ok("application/javascript", "window.ext += 'two';"));
        SwingWebPanel p = open("/s.xhtml");
        Check.equal(1, server.count("/link.css"), "the link element requested the style sheet");
        Check.equal(1, server.count("/ext.js"), "the script element requested the file");
        Check.equal("rgb(9, 8, 7)|rgb(1, 1, 1)|onetwo", stored(p, "colors") == null ? "" : stored(p, "colors"),
                "the xml-stylesheet instruction and the link element apply; both scripts ran in order (the second one has an empty element tag)");
    }

    /**
     * XHTML syntax in a page that is served as <code>text/html</code> is parsed by the HTML parser: empty element tags of elements that
     * have content are start tags, the XML prolog is a bogus comment, and names are case-insensitive.
     * <p>
     * <code>text/html</code> 로 제공한 페이지의 XHTML 문법은 HTML 파서가 해석한다: 내용이 있는 요소의 빈 요소 태그는 시작 태그이고 XML prolog 는 잘못된 주석이며 이름은 대소문자를
     * 구분하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testXhtmlSyntaxServedAsHtml() throws Exception {
        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + TRANSITIONAL + "\n<HTML xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>H</title></head>"
                + "<BODY><p id=\"p\">a<br />b<img src=\"x.png\" alt=\"x\" /></p><div id=\"d\" /><p id=\"after\">after</p>"
                + "<script type=\"text/javascript\">sessionStorage.setItem('r', document.compatMode + '|' + document.contentType + '|'"
                + " + document.documentElement.tagName + '|' + document.getElementById('d').childNodes.length + '|'"
                + " + (document.getElementById('d').parentNode.id || document.getElementById('d').parentNode.tagName));</script></BODY></HTML>";
        server.routeFixed("/h.html", TestHttpServer.Response.html(xhtml));
        SwingWebPanel p = open("/h.html");
        Check.equal(xhtml, p.getContent(), "the source is kept");
        String r = stored(p, "r");
        Check.notNull(r, "the script ran");
        Check.equal("CSS1Compat|text/html|HTML|2|BODY", r, "limited quirks (the DOCTYPE has a system identifier) in the HTML parser: the tag name is upper case, "
                + "and a div with an empty element tag stays open and holds the next paragraph and the script");
    }
}
