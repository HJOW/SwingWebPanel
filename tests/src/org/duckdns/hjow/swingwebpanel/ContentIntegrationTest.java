package org.duckdns.hjow.swingwebpanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.core.BrowserSession;
import org.duckdns.hjow.swingwebpanel.core.DownloadHandler;
import org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException;
import org.duckdns.hjow.swingwebpanel.core.NavigationRequest;
import org.duckdns.hjow.swingwebpanel.core.NetworkPageLoader;
import org.duckdns.hjow.swingwebpanel.core.ParsingPageLoader;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The content processors through the real component (plan items P17-01, P17-02, P17-05, P17-06 at the component level): the processor
 * that is chosen for a response (HTML, XHTML, plain text, Markdown, download), what a plain text or XHTML page keeps as its source, that a
 * text file never runs as markup, that an XHTML page runs its scripts with XML rules, and that an XHTML document that is not well-formed
 * shows an error page without losing the received source.
 * <p>
 * 실제 컴포넌트를 통한 콘텐츠 처리기의 시험(컴포넌트 수준의 계획 P17-01, P17-02, P17-05, P17-06): 응답에 고른 처리기(HTML, XHTML, 일반 텍스트,
 * Markdown, 다운로드), 일반 텍스트와 XHTML 페이지가 원문으로 보관하는 것, 텍스트 파일이 마크업으로 실행되지 않는 것, XHTML 페이지가 XML 규칙으로
 * 스크립트를 실행하는 것, well-formed 가 아닌 XHTML 문서가 수신 원문을 잃지 않고 오류 페이지를 보여 주는 것.
 */
public class ContentIntegrationTest {

    /**
     * Panels created by a case, disposed in tearDown.
     * <p>
     * 사례에서 만든 패널. tearDown 에서 폐기한다.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Servers started by a case, stopped in tearDown.
     * <p>
     * 사례에서 시작한 서버. tearDown 에서 중지한다.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContentIntegrationTest() {
    }

    /**
     * Releases everything the case created.
     * <p>
     * 사례가 만든 것을 모두 해제한다.
     */
    public void tearDown() {
        // 만든 패널과 서버를 모두 정리한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        // 시작한 HTTP 서버를 모두 중지한다.
        for (TestHttpServer s : servers) {
            s.stop();
        }
    }

    /**
     * Starts a plain HTTP test server.
     * <p>
     * 평문 HTTP 시험 서버를 시작한다.
     *
     * @return the server / 서버
     * @throws IOException if it cannot start / 시작할 수 없을 때
     */
    private TestHttpServer http() throws IOException {
        TestHttpServer s = new TestHttpServer();
        s.start();
        servers.add(s);
        return s;
    }

    /**
     * Creates a panel with the standard loaders.
     * <p>
     * 표준 로더를 쓰는 패널을 만든다.
     *
     * @return the panel / 패널
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Waits until the load finished (progress 100 and no "Loading" status), after the address was requested.
     * <p>
     * 로딩이 끝날 때까지(진행률 100, "Loading" 상태 아님) 기다린다.
     *
     * @param p the panel / 패널
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void settle(SwingWebPanel p) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 상태가 끝난 것으로 보일 때까지 짧게 쉬며 기다린다.
        while ((p.getProgress() != 100 || p.getStatusText().startsWith("Loading")) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        // 확정 통지가 늦어지는 경우를 위해 한 번 더 짧게 기다린다.
        Thread.sleep(50);
    }

    /**
     * Reads a value that a script stored in the sessionStorage.
     * <p>
     * 스크립트가 sessionStorage 에 저장한 값을 읽는다.
     *
     * @param p   the panel / 패널
     * @param key the key / 키
     * @return the value or null / 값 또는 null
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
     * P17-02: a text file is shown as text and never runs as markup or script, whatever it contains; other text types (a style sheet) are
     * shown as text as well; the received text stays the source.
     * <p>
     * P17-02: 텍스트 파일은 텍스트로 보이며 무엇이 들어 있든 마크업이나 스크립트로 실행되지 않는다. 다른 텍스트 형식(스타일시트)도 텍스트로 보이고 수신
     * 텍스트가 원문으로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTextFilesNeverRunAsMarkup() throws Exception {
        TestHttpServer s = http();
        String text = "<script>sessionStorage.setItem('ran','yes')</script>\r\n<b onclick=\"x()\">bold?</b>\r\nline 3";
        s.routeFixed("/t.txt", TestHttpServer.Response.ok("text/plain; charset=utf-8", text));
        s.routeFixed("/s.css", TestHttpServer.Response.ok("text/css", "body { color: red }"));
        SwingWebPanel p = panel();
        p.setPage(s.url("/t.txt"));
        settle(p);
        Check.equal(s.url("/t.txt"), p.getPage(), "text page address");
        Check.equal(text, p.getContent(), "the received text is the source, line breaks included");
        Check.equal("text/plain", p.getMimeType(), "plain text");
        Check.isNull(stored(p, "ran"), "the text did not run as a script");
        p.setPage(s.url("/s.css"));
        settle(p);
        Check.equal("body { color: red }", p.getContent(), "a style sheet shows as its text");
        Check.equal("text/plain", p.getMimeType(), "shown as plain text");
    }

    /**
     * P17-05: an XHTML page runs its scripts with the XML document (case-sensitive names, XHTML namespace, content type), the received
     * source and the title are kept, and the same works for text given with <code>setContent</code>.
     * <p>
     * P17-05: XHTML 페이지는 XML 문서(대소문자를 구분하는 이름, XHTML 네임스페이스, 콘텐츠 타입)로 스크립트를 실행하고 수신 원문과 제목을 보관하며,
     * <code>setContent</code> 로 준 텍스트도 마찬가지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testXhtmlPagesRunScriptsWithXmlRules() throws Exception {
        TestHttpServer s = http();
        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>XHTML title</title></head>"
                + "<body><p id=\"p\">hello</p><script>sessionStorage.setItem('info', document.documentElement.namespaceURI + '|' + document.contentType + '|'"
                + " + document.documentElement.localName + '|' + document.getElementById('p').tagName);</script></body></html>";
        xhtml = xhtml.replace("'|'\" + \" + ", "'|' + ");
        s.routeFixed("/x.xhtml", TestHttpServer.Response.ok("application/xhtml+xml", xhtml));
        SwingWebPanel p = panel();
        p.setPage(s.url("/x.xhtml"));
        settle(p);
        Check.equal(xhtml, p.getContent(), "the received XHTML text is the source");
        Check.equal("application/xhtml+xml", p.getMimeType(), "XHTML");
        Check.equal("XHTML title", p.getTitle(), "title");
        Check.equal("http://www.w3.org/1999/xhtml|application/xhtml+xml|html|p", stored(p, "info"),
                "the script saw an XML document: namespace, content type, case-sensitive names");
        SwingWebPanel q = panel();
        q.setContent(xhtml, "application/xhtml+xml; charset=utf-8");
        settle(q);
        Check.equal("about:text", q.getPage(), "setContent address");
        Check.equal(xhtml, q.getContent(), "setContent keeps the text");
        Check.equal("application/xhtml+xml", q.getMimeType(), "setContent MIME type");
        Check.equal("XHTML title", q.getTitle(), "setContent builds the XML document (the title comes from it)");
    }

    /**
     * P17-05: an XHTML document that is not well-formed is not repaired as HTML: the page is an error page that names the place of the
     * error, its scripts do not run, and the received text stays the source; the error page itself can be left again like any page.
     * <p>
     * P17-05: well-formed 가 아닌 XHTML 문서는 HTML 로 복구하지 않는다. 페이지는 오류 위치를 알려 주는 오류 페이지이고 스크립트는 실행되지 않으며
     * 수신 원문은 그대로 남는다. 오류 페이지도 다른 페이지처럼 떠날 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMalformedXhtmlShowsAnErrorPageAndKeepsTheSource() throws Exception {
        TestHttpServer s = http();
        String broken = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body><p>never closed</body>"
                + "<script>sessionStorage.setItem('ran','yes')</script></html>";
        s.routeFixed("/broken.xhtml", TestHttpServer.Response.ok("application/xhtml+xml", broken));
        s.routeFixed("/ok", TestHttpServer.Response.html("<p>ok</p>"));
        SwingWebPanel p = panel();
        p.setPage(s.url("/broken.xhtml"));
        settle(p);
        Check.equal(s.url("/broken.xhtml"), p.getPage(), "the page address is the XHTML address");
        Check.equal(broken, p.getContent(), "the received text is kept as the source");
        Check.isNull(stored(p, "ran"), "scripts of a document that is not well-formed do not run");
        Check.isTrue(p.getTitle().length() > 0, "the error page has a title: " + p.getTitle());
        p.setPage(s.url("/ok"));
        settle(p);
        Check.equal("<p>ok</p>", p.getContent(), "the next page loads normally");
        p.back();
        settle(p);
        Check.equal(s.url("/broken.xhtml"), p.getPage(), "going back returns to the XHTML address");
        Check.equal(broken, p.getContent(), "the source is restored");
    }

    /**
     * P17-01: the response decides the processor: HTML, Markdown, XML that is XHTML by its root element, and a response without a type
     * that is recognized from its first bytes; types that cannot be shown go to the download handler.
     * <p>
     * P17-01: 응답이 처리기를 정한다: HTML, Markdown, 루트 요소로 XHTML 인 XML, 첫 바이트로 알아본 타입 없는 응답. 보여 줄 수 없는 타입은 다운로드
     * 핸들러로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testResponsesChooseTheProcessor() throws Exception {
        TestHttpServer s = http();
        String md = "# Title\n\nsome *text*";
        s.routeFixed("/md", TestHttpServer.Response.ok("text/markdown; charset=utf-8", md));
        String xmlXhtml = "<?xml version=\"1.0\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>from xml</title></head><body/></html>";
        s.routeFixed("/xml", TestHttpServer.Response.ok("application/xml", xmlXhtml));
        s.routeFixed("/untyped-html", new TestHttpServer.Response(200).bytes("", "<!DOCTYPE html><title>t</title><p>x</p>".getBytes("UTF-8")));
        s.routeFixed("/untyped-text", new TestHttpServer.Response(200).bytes("", "just words".getBytes("UTF-8")));
        s.routeFixed("/data.csv", TestHttpServer.Response.ok("text/csv", "a,b\n1,2"));
        s.routeFixed("/bin", new TestHttpServer.Response(200).bytes("application/octet-stream", new byte[] { 0, 1, 2, 3 }));
        s.routeFixed("/untyped-bin", new TestHttpServer.Response(200).bytes("", new byte[] { 0, 1, 2, 3, 4 }));
        SwingWebPanel p = panel();
        p.setPage(s.url("/md"));
        settle(p);
        Check.equal("text/markdown", p.getMimeType(), "Markdown");
        Check.equal(md, p.getContent(), "the Markdown source is the content");
        p.setPage(s.url("/xml"));
        settle(p);
        Check.equal("application/xhtml+xml", p.getMimeType(), "XML with an XHTML root is XHTML");
        Check.equal("from xml", p.getTitle(), "its title");
        p.setPage(s.url("/untyped-html"));
        settle(p);
        Check.equal("text/html", p.getMimeType(), "a response without a type that looks like HTML");
        p.setPage(s.url("/untyped-text"));
        settle(p);
        Check.equal("text/plain", p.getMimeType(), "a response without a type that looks like text");
        // 다운로드 판정은 컴포넌트의 팝업 연결(P11-09) 전이므로 같은 로더를 쓰는 세션에서 핸들러로 관찰한다.
        final List<DownloadRequiredException> downloads = new ArrayList<DownloadRequiredException>();
        CookieStore cookies = new CookieStore();
        BrowserSession session = new BrowserSession(new ParsingPageLoader(new NetworkPageLoader(
                new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader()), cookies)), null, cookies);
        session.setDownloadHandler(new DownloadHandler() {
            /**
             * Records the download.
             * <p>
             * 다운로드를 기록한다.
             *
             * @param request  the request / 요청
             * @param download the description / 설명
             */
            public void downloadRequired(NavigationRequest request, DownloadRequiredException download) {
                downloads.add(download);
            }
        });
        try {
            session.setPage(s.url("/untyped-text"));
            TestSupport.settle(session);
            String shown = session.getPage();
            session.setPage(s.url("/data.csv"));
            TestSupport.settle(session);
            session.setPage(s.url("/bin"));
            TestSupport.settle(session);
            session.setPage(s.url("/untyped-bin"));
            TestSupport.settle(session);
            Check.equal(shown, session.getPage(), "downloads do not replace the page");
            Check.equal(3, downloads.size(), "csv, octet-stream and untyped binary go to the download handler");
        } finally {
            session.dispose();
        }
    }

    /**
     * P17-04, P17-08: every content type is restored from the history with its received source and its type (Markdown is not replaced
     * by the HTML made from it), also through <code>setContent</code>.
     * <p>
     * P17-04, P17-08: 모든 콘텐츠 형식은 이력에서 수신 원문과 형식을 그대로 복원한다(Markdown 은 그것으로 만든 HTML 로 바뀌지 않는다).
     * <code>setContent</code> 로 준 것도 마찬가지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHistoryRestoresEveryContentType() throws Exception {
        TestHttpServer s = http();
        String md = "# Heading\n\n* item *one*\n\n<b>raw</b> `code`";
        String html = "<!doctype html><title>h</title><p>html page</p>";
        String text = "plain\r\ntext <i>not markup</i>";
        String xhtml = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>x</title></head><body><p>x</p></body></html>";
        s.routeFixed("/md", TestHttpServer.Response.ok("text/markdown;charset=UTF-8", md));
        s.routeFixed("/html", TestHttpServer.Response.html(html));
        s.routeFixed("/text", TestHttpServer.Response.ok("text/plain", text));
        s.routeFixed("/xhtml", TestHttpServer.Response.ok("application/xhtml+xml", xhtml));
        String[] paths = { "/md", "/html", "/text", "/xhtml" };
        String[] sources = { md, html, text, xhtml };
        String[] types = { "text/markdown", "text/html", "text/plain", "application/xhtml+xml" };
        SwingWebPanel p = panel();
        // 네 형식을 차례로 열고, 다른 페이지를 거쳐 뒤로 돌아가 원문과 형식이 같은지 확인한다.
        for (int i = 0; i < paths.length; i++) {
            p.setPage(s.url(paths[i]));
            settle(p);
            Check.equal(sources[i], p.getContent(), "source of " + paths[i]);
            p.setContent("<p>other</p>", "text/html");
            settle(p);
            p.back();
            settle(p);
            Check.equal(s.url(paths[i]), p.getPage(), "restored address of " + paths[i]);
            Check.equal(sources[i], p.getContent(), "restored source of " + paths[i]);
            Check.equal(types[i], p.getMimeType(), "restored type of " + paths[i]);
        }
        p.setContent(md, "Text/Markdown; charset=utf-8");
        settle(p);
        p.setContent("<p>other</p>", "text/html");
        settle(p);
        p.back();
        settle(p);
        Check.equal("about:text", p.getPage(), "setContent address after going back");
        Check.equal(md, p.getContent(), "the Markdown given to setContent is restored as it was");
        Check.equal("text/markdown", p.getMimeType(), "and keeps its type");
    }

    /**
     * P17-08: an error page does not keep running the timers of the page that it replaced.
     * <p>
     * P17-08: 오류 페이지는 자기가 대체한 페이지의 타이머를 계속 실행하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorPageStopsThePreviousPagesTimers() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/ticker", TestHttpServer.Response.html("<!doctype html><script>var n = 0; setInterval(function () { n++; sessionStorage.setItem('ticks', String(n)); }, 20);</script>"));
        SwingWebPanel p = panel();
        p.setPage(s.url("/ticker"));
        settle(p);
        long end = System.currentTimeMillis() + 30000;
        // 타이머가 몇 번 돌 때까지 기다린다.
        while ((stored(p, "ticks") == null || Integer.parseInt(stored(p, "ticks")) < 3) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.notNull(stored(p, "ticks"), "the timer ran");
        p.setPage(s.url("/missing"));
        settle(p);
        Check.equal("Error 404", p.getStatusText(), "the error page is shown");
        String before = stored(p, "ticks");
        Thread.sleep(400);
        Check.equal(before, stored(p, "ticks"), "the timer of the replaced page stopped");
    }

    /**
     * Moves the pointer to a point and tells whether the component shows the hand cursor there (the pointer is over a link).
     * <p>
     * 포인터를 한 지점으로 옮기고 컴포넌트가 그곳에서 손 모양 커서를 보이는지(포인터가 링크 위인지) 알려 준다.
     *
     * @param p the component / 컴포넌트
     * @param x the x coordinate / x 좌표
     * @param y the y coordinate / y 좌표
     * @return true over a link / 링크 위이면 true
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static boolean overLink(final SwingWebPanel p, final int x, final int y) throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Delivers a pointer move through the component.
             * <p>
             * 컴포넌트를 통해 포인터 이동을 전달한다.
             */
            public void run() {
                p.dispatchEvent(new java.awt.event.MouseEvent(p, java.awt.event.MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false));
            }
        });
        Thread.sleep(80);
        final boolean[] hand = new boolean[1];
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Reads the cursor on the EDT.
             * <p>
             * EDT 에서 커서를 읽는다.
             */
            public void run() {
                hand[0] = p.getCursor().getType() == java.awt.Cursor.HAND_CURSOR;
            }
        });
        return hand[0];
    }

    /**
     * P17-07: the retry link of a generated error page is an ordinary link: a click notifies the hyperlink listeners and loads the
     * address again through the common navigation path; the server body of an HTTP error stays the source of its page.
     * <p>
     * P17-07: 생성한 오류 페이지의 다시 시도 링크는 보통 링크이다. 클릭하면 하이퍼링크 리스너에 알리고 공통 이동 경로로 주소를 다시 읽는다.
     * HTTP 오류의 서버 본문은 그 페이지의 원문으로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRetryLinkOfAnErrorPageIsAnOrdinaryLink() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/gone", new TestHttpServer.Response(404));
        s.routeFixed("/body404", new TestHttpServer.Response(404).text("text/html", "<p>server body</p>"));
        final SwingWebPanel p = panel();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Sizes the component.
             * <p>
             * 컴포넌트 크기를 정한다.
             */
            public void run() {
                p.setSize(500, 400);
            }
        });
        final List<String> activated = new ArrayList<String>();
        p.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            /**
             * Records activated links.
             * <p>
             * 활성화된 링크를 기록한다.
             *
             * @param event the event / 이벤트
             */
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent event) {
                if (event.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                    activated.add(event.getDescription());
                }
            }
        });
        p.setPage(s.url("/body404"));
        settle(p);
        Check.equal("<p>server body</p>", p.getContent(), "the body of the server is the source of an HTTP error page");
        p.setPage(s.url("/gone"));
        settle(p);
        Check.equal("Error 404", p.getStatusText(), "generated error page");
        Check.equal(1, s.count("/gone"), "requested once so far");
        int foundY = -1;
        // 오류 페이지를 위에서 아래로 훑어 포인터가 링크 위에 있을 때 손 모양 커서가 되는 곳을 찾는다.
        for (int y = 4; y < 360 && foundY < 0; y += 6) {
            if (overLink(p, 150, y)) {
                foundY = y;
            }
        }
        Check.isTrue(foundY >= 0, "the error page has a link that the pointer can find");
        final int clickY = foundY;
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Clicks the link through the component's event dispatch.
             * <p>
             * 컴포넌트의 이벤트 전달로 링크를 클릭한다.
             */
            public void run() {
                p.dispatchEvent(new java.awt.event.MouseEvent(p, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 150,
                        clickY, 1, false, java.awt.event.MouseEvent.BUTTON1));
            }
        });
        long end = System.currentTimeMillis() + 30000;
        // 다시 시도 이동으로 서버가 한 번 더 요청받을 때까지 기다린다.
        while (s.count("/gone") < 2 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(2, s.count("/gone"), "the retry link loaded the address again");
        Check.equal(1, activated.size(), "the hyperlink listeners were told once");
        Check.equal(s.url("/gone"), activated.get(0), "with the address that failed");
    }

    /**
     * An external classic script without an encoding of its own is decoded with the encoding of the document that includes it (here
     * EUC-KR from a <code>meta</code> tag), while the charset of its own response wins when it has one.
     * <p>
     * 자기 인코딩이 없는 외부 일반 스크립트는 그것을 포함한 문서의 인코딩(여기서는 <code>meta</code> 태그의 EUC-KR)으로 디코딩되고, 응답에
     * 자기 charset 이 있으면 그것이 이긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExternalScriptsInheritTheDocumentEncoding() throws Exception {
        TestHttpServer s = http();
        String page = "<!doctype html><meta charset=\"euc-kr\"><script src=\"inherit.js\"></script><script src=\"own.js\"></script>";
        s.routeFixed("/page", new TestHttpServer.Response(200).bytes("text/html", page.getBytes("UTF-8")));
        s.routeFixed("/inherit.js", new TestHttpServer.Response(200).bytes("application/javascript",
                "sessionStorage.setItem('inherit', '한글 문서');".getBytes("EUC-KR")));
        s.routeFixed("/own.js", new TestHttpServer.Response(200).bytes("application/javascript; charset=utf-8",
                "sessionStorage.setItem('own', '자기 문자셋');".getBytes("UTF-8")));
        SwingWebPanel p = panel();
        p.setPage(s.url("/page"));
        settle(p);
        Check.equal("한글 문서", stored(p, "inherit"), "the script took the encoding of the document");
        Check.equal("자기 문자셋", stored(p, "own"), "the charset of the script's own response wins");
    }
}
