package org.duckdns.hjow.swingwebpanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.duckdns.hjow.swingwebpanel.core.BrowserSession;
import org.duckdns.hjow.swingwebpanel.core.DownloadHandler;
import org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException;
import org.duckdns.hjow.swingwebpanel.core.NavigationRequest;
import org.duckdns.hjow.swingwebpanel.core.NetworkPageLoader;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestFtpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestSsl;

/**
 * Integration tests of the component with the network layer: HTTP, HTTPS (with the overridable SSL hook and the plain-HTTP fallback),
 * FTP, class path addresses, cookies, headers, error pages and the download decision (plan items P04-03 to P04-10 and P05 through the
 * public API, contract cases C-PAGE, C-CONTENT, C-HEADERS, C-COOKIE, C-SSL, C-DOWNLOAD). The servers are local test servers.
 * <p>
 * 컴포넌트와 네트워크 계층의 통합 시험: HTTP, HTTPS(재정의 가능한 SSL 훅과 평문 HTTP 재시도), FTP, 클래스패스 주소, 쿠키, 헤더, 오류 페이지,
 * 다운로드 판정(공개 API 를 통한 계획 P04-03~P04-10, P05, 계약 사례 C-PAGE, C-CONTENT, C-HEADERS, C-COOKIE, C-SSL, C-DOWNLOAD).
 * 서버는 로컬 시험 서버이다.
 */
public class NetworkIntegrationTest {

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
     * FTP servers started by a case, stopped in tearDown.
     * <p>
     * 사례에서 시작한 FTP 서버. tearDown 에서 중지한다.
     */
    private final List<TestFtpServer> ftpServers = new ArrayList<TestFtpServer>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public NetworkIntegrationTest() {
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
        // 시작한 FTP 서버를 모두 중지한다.
        for (TestFtpServer f : ftpServers) {
            f.stop();
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
     * Creates a panel with the standard network loader.
     * <p>
     * 표준 네트워크 로더를 쓰는 패널을 만든다.
     *
     * @return the panel / 패널
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Waits until the load finished (progress 100 and no "Loading" status).
     * <p>
     * 로딩이 끝날 때까지(진행률 100, "Loading" 상태 아님) 기다린다.
     *
     * @param p the panel / 패널
     * @param startUrl a URL the load was started for, to see that the load began / 로딩을 시작한 URL
     */
    private static void settle(SwingWebPanel p, String startUrl) {
        long end = System.currentTimeMillis() + 20000;
        // 상태가 끝난 것으로 보일 때까지 짧게 쉬며 기다린다.
        while ((p.getProgress() != 100 || p.getStatusText().startsWith("Loading")) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
        Check.isTrue(p.getProgress() == 100 && !p.getStatusText().startsWith("Loading"), "load of " + startUrl + " must finish");
    }

    /**
     * C-PAGE, C-CONTENT: an HTTP page is shown with its source unchanged (line ends and non-ASCII text included).
     * <p>
     * C-PAGE, C-CONTENT: HTTP 페이지가 원문 그대로(줄 끝과 비ASCII 문자 포함) 표시된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpPageKeepsReceivedSource() throws Exception {
        TestHttpServer s = http();
        String body = "<!DOCTYPE html>\r\n<html>\r\n<head><title>t</title></head>\r\n<body>\u00e9 \ud55c\uae00 <b>bold</b>\r\n\r\n</body></html>";
        s.routeFixed("/a", new TestHttpServer.Response(200).text("text/html; charset=UTF-8", body));
        SwingWebPanel p = panel();
        p.setPage(s.url("/a"));
        settle(p, s.url("/a"));
        Check.equal(s.url("/a"), p.getPage(), "URL");
        Check.equal(body, p.getContent(), "getContent returns the received source unchanged");
        Check.equal("text/html", p.getMimeType(), "MIME type");
        Check.equal("Done", p.getStatusText(), "status");
        Check.equal(2, p.getHistories().size(), "history");
        Check.equal("swingwebpanel/1.0", s.last("/a").header("user-agent"), "default user agent is sent");
    }

    /**
     * C-UA-4, C-HEADERS: user agent and extra headers reach the server; later values overwrite earlier ones.
     * <p>
     * C-UA-4, C-HEADERS: User-Agent 와 추가 헤더가 서버에 도달하고 나중 값이 이전 값을 덮어쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUserAgentAndHeadersAreSent() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/a", new TestHttpServer.Response(200).text("text/plain", "x"));
        SwingWebPanel p = panel();
        p.setUserAgent("Probe/3.1");
        Map<String, String> h = new HashMap<String, String>();
        h.put("Accept-Language", "ko");
        h.put("X-Test", "one");
        p.addHeaders(h);
        Map<String, String> h2 = new HashMap<String, String>();
        h2.put("x-test", "two");
        h2.put("User-Agent", "Evil/1");
        p.addHeaders(h2);
        p.setPage(s.url("/a"));
        settle(p, s.url("/a"));
        TestHttpServer.Request r = s.last("/a");
        Check.equal("Probe/3.1", r.header("user-agent"), "user agent");
        Check.equal("ko", r.header("accept-language"), "extra header");
        Check.equal("two", r.header("x-test"), "overwritten header");
        Check.equal("x", p.getContent(), "content");
        Check.equal("text/plain", p.getMimeType(), "MIME type");
    }

    /**
     * C-PAGE, REQ-03: HTTP errors with a body keep the server's body; errors without a body get a generated page. No exception reaches the caller.
     * <p>
     * C-PAGE, REQ-03: 본문이 있는 HTTP 오류는 서버 본문을 유지하고, 본문이 없으면 생성한 페이지를 보여 준다. 호출자에게 예외는 전달되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpErrorPages() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/missing", new TestHttpServer.Response(404).text("text/html", "<h1>custom 404 body</h1>"));
        s.routeFixed("/boom", new TestHttpServer.Response(500));
        SwingWebPanel p = panel();
        p.setPage(s.url("/missing"));
        settle(p, "missing");
        Check.equal("<h1>custom 404 body</h1>", p.getContent(), "the server body is kept for a 404");
        Check.equal("Error 404", p.getStatusText(), "status of a 404");
        Check.equal(s.url("/missing"), p.getPage(), "URL of the error page");
        p.setPage(s.url("/boom"));
        settle(p, "boom");
        Check.contains(p.getContent(), "500", "generated page mentions the status");
        Check.contains(p.getContent(), "Internal Server Error", "generated page mentions the reason");
        Check.equal("Error 500", p.getStatusText(), "status of a 500");
        Check.equal(3, p.getHistories().size(), "error pages are history entries");
    }

    /**
     * C-HIST-5: redirects end in one history entry with the final URL.
     * <p>
     * C-HIST-5: 리디렉션은 최종 URL 한 건의 이력으로 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectKeepsFinalUrlOnly() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/old", new TestHttpServer.Response(302).header("Location", "/new"));
        s.routeFixed("/new", new TestHttpServer.Response(200).text("text/html", "<p>new</p>"));
        SwingWebPanel p = panel();
        p.setPage(s.url("/old"));
        settle(p, "old");
        Check.equal(s.url("/new"), p.getPage(), "final URL");
        Check.equal("<p>new</p>", p.getContent(), "content of the final page");
        Check.equal(2, p.getHistories().size(), "one entry for the redirected navigation");
        Check.equal(s.url("/new"), p.getHistories().get(1), "history entry is the final URL");
    }

    /**
     * C-COOKIE: cookies are stored per component, sent back, kept by clean() and forgotten by dispose().
     * <p>
     * C-COOKIE: 쿠키는 컴포넌트별로 저장되어 다시 전송되고, clean() 후에도 유지되며 dispose() 로 잊는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCookiesAreStoredSentAndKeptByClean() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/set", new TestHttpServer.Response(200).text("text/plain", "set").header("Set-Cookie", "sid=abc; Path=/"));
        s.routeFixed("/echo", new TestHttpServer.Response(200).text("text/plain", "echo"));
        SwingWebPanel p = panel();
        SwingWebPanel other = panel();
        p.setPage(s.url("/set"));
        settle(p, "set");
        Check.equal("sid=abc", p.getCookies(), "getCookies");
        Check.equal("", other.getCookies(), "another component has its own cookie store");
        p.setPage(s.url("/echo"));
        settle(p, "echo");
        Check.equal("sid=abc", s.last("/echo").header("cookie"), "the cookie is sent back");
        p.clean();
        Check.equal("sid=abc", p.getCookies(), "clean keeps cookies");
        p.dispose();
        Check.equal("", p.getCookies(), "dispose forgets cookies");
    }

    /**
     * C-REFRESH: refresh(true) bypasses caches (no-cache request headers); a normal refresh does not.
     * <p>
     * C-REFRESH: refresh(true) 는 캐시를 우회하고(no-cache 요청 헤더) 일반 새로고침은 그렇지 않다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testForcedRefreshSendsNoCacheHeaders() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/a", new TestHttpServer.Response(200).text("text/plain", "x"));
        SwingWebPanel p = panel();
        p.setPage(s.url("/a"));
        settle(p, "a");
        p.refresh();
        settle(p, "a");
        Check.equal(2, s.count("/a"), "the page was requested again");
        Check.isNull(s.last("/a").header("pragma"), "a normal refresh sends no Pragma");
        p.refresh(true);
        settle(p, "a");
        Check.equal(3, s.count("/a"), "forced refresh requests again");
        Check.equal("no-cache", s.last("/a").header("cache-control"), "forced refresh sends Cache-Control: no-cache");
        Check.equal(2, p.getHistories().size(), "refreshing adds no history entries");
    }

    /**
     * C-PAGE-5: addresses with forbidden schemes never reach the network.
     * <p>
     * C-PAGE-5: 금지된 스킴의 주소는 네트워크에 닿지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedSchemeDoesNotTouchTheNetwork() throws Exception {
        TestHttpServer s = http();
        SwingWebPanel p = panel();
        p.setPage("file://nonlocal.invalid/path");
        settle(p, "file");
        Check.equal("Error 400", p.getStatusText(), "status");
        Check.equal(0, s.totalRequests(), "no request was made");
        Check.contains(p.getContent(), "not supported", "error page text");
    }

    /**
     * Failure to connect gives an error page, not an exception.
     * <p>
     * 연결 실패는 예외가 아니라 오류 페이지가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConnectionFailureBecomesErrorPage() throws Exception {
        TestHttpServer s = http();
        int port = s.getPort();
        s.stop();
        SwingWebPanel p = panel();
        p.setPage("http://127.0.0.1:" + port + "/nothing");
        settle(p, "refused");
        Check.contains(p.getContent(), "Connection failed", "error page");
        Check.equal("http://127.0.0.1:" + port + "/nothing", p.getPage(), "the requested URL is shown");
    }

    /**
     * C-SSL-1: the protected SSL hook is called for HTTPS connections and may install trust for that connection only.
     * <p>
     * C-SSL-1: protected SSL 훅은 HTTPS 연결마다 호출되며 그 연결에만 신뢰를 설치할 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOverriddenSslHookIsCalledForHttps() throws Exception {
        TestHttpServer s = new TestHttpServer(TestSsl.serverContext());
        s.start();
        servers.add(s);
        s.routeFixed("/secure", new TestHttpServer.Response(200).text("text/html", "<p>secure</p>"));
        final int[] calls = new int[1];
        SwingWebPanel p = new SwingWebPanel() {
            private static final long serialVersionUID = 1L;

            /**
             * Counts the call and trusts the test certificate for this connection.
             * <p>
             * 호출을 세고 이 연결에 시험 인증서를 신뢰시킨다.
             *
             * @param c connection / 연결
             * @throws IOException if the trust cannot be installed / 신뢰를 설치할 수 없을 때
             */
            protected void configureSsl(HttpsURLConnection c) throws IOException {
                calls[0]++;
                try {
                    c.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }
        };
        panels.add(p);
        String url = "https://localhost:" + s.getPort() + "/secure";
        p.setPage(url);
        settle(p, url);
        Check.equal(url, p.getPage(), "URL");
        Check.equal("<p>secure</p>", p.getContent(), "content");
        Check.isTrue(calls[0] >= 1, "the SSL hook was called");
    }

    /**
     * C-SSL-2: if the HTTPS connection fails, one plain HTTP attempt is made for the same address (port 443 becomes 80, other ports stay).
     * <p>
     * C-SSL-2: HTTPS 연결이 실패하면 같은 주소로 평문 HTTP 를 한 번 시도한다(443 은 80 으로, 다른 포트는 유지).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsFailureFallsBackToHttp() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/a", new TestHttpServer.Response(200).text("text/html", "<p>plain</p>"));
        SwingWebPanel p = panel();
        p.setPage("https://localhost:" + s.getPort() + "/a");
        settle(p, "fallback");
        Check.equal("http://localhost:" + s.getPort() + "/a", p.getPage(), "the page URL is the actual http URL");
        Check.equal("<p>plain</p>", p.getContent(), "content of the plain http page");
    }

    /**
     * REQ: FTP directories are listed as a page with links, ../ first; files are shown; failures are 404 pages.
     * <p>
     * REQ: FTP 디렉터리는 링크가 있는 페이지로 보이고(../ 가 맨 위), 파일은 표시되며, 실패는 404 페이지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFtpListingFileAndFailure() throws Exception {
        TestFtpServer ftp = new TestFtpServer().addDir("/pub").addDir("/pub/sub").addFile("/pub/readme.txt", "hello ftp".getBytes("UTF-8"));
        ftp.start();
        ftpServers.add(ftp);
        SwingWebPanel p = panel();
        p.setPage(ftp.url("/pub/"));
        settle(p, "ftp dir");
        Check.equal(ftp.url("/pub/"), p.getPage(), "URL");
        Check.equal("text/html", p.getMimeType(), "listing MIME type");
        Check.contains(p.getContent(), "readme.txt", "file name is listed");
        Check.contains(p.getContent(), "sub/", "directory link is listed");
        Check.isTrue(p.getContent().indexOf("../") >= 0 && p.getContent().indexOf("../") < p.getContent().indexOf("readme.txt"), "../ comes first");
        p.setPage(ftp.url("/pub/readme.txt"));
        settle(p, "ftp file");
        Check.equal("hello ftp", p.getContent(), "FTP text file content");
        p.setPage(ftp.url("/nothing/"));
        settle(p, "ftp missing");
        Check.equal("Error 404", p.getStatusText(), "an FTP failure is a 404 page");
    }

    /**
     * REQ: classpath:// addresses are read through the class loader; missing resources give a 404 page.
     * <p>
     * REQ: classpath:// 주소는 클래스 로더로 읽고, 없는 리소스는 404 페이지가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClasspathPage() throws Exception {
        SwingWebPanel p = panel();
        p.setPage("classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html");
        settle(p, "classpath");
        Check.equal("classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html", p.getPage(), "URL");
        Check.contains(p.getContent(), "classpath resource", "content");
        Check.equal("text/html", p.getMimeType(), "MIME type from the file name");
        p.setPage("classpath://org/duckdns/hjow/swingwebpanel/testdata/none.html");
        settle(p, "classpath missing");
        Check.equal("Error 404", p.getStatusText(), "missing resource");
    }

    /**
     * C-DOWNLOAD-1..3: an attachment or a type that cannot be shown leaves the page, URL and history untouched and is handed to the
     * download handler.
     * <p>
     * C-DOWNLOAD-1..3: 첨부 파일이나 보여 줄 수 없는 형식은 페이지, URL, 이력을 바꾸지 않고 다운로드 핸들러에 전달된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDownloadDecisionDoesNotReplaceThePage() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/page", new TestHttpServer.Response(200).text("text/html", "<p>page</p>"));
        s.routeFixed("/attach", new TestHttpServer.Response(200).text("text/html", "<p>x</p>").header("Content-Disposition", "attachment; filename=a.html"));
        s.routeFixed("/bin", new TestHttpServer.Response(200).bytes("application/octet-stream", new byte[] { 1, 2, 3 }));
        final List<DownloadRequiredException> received = new ArrayList<DownloadRequiredException>();
        CookieStore cookies = new CookieStore();
        BrowserSession session = new BrowserSession(new NetworkPageLoader(
                new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader()), cookies), null, cookies);
        session.setDownloadHandler(new DownloadHandler() {
            /**
             * Records the download.
             * <p>
             * 다운로드를 기록한다.
             *
             * @param request  navigation request / 이동 요청
             * @param download description of the download / 다운로드 설명
             */
            public void downloadRequired(NavigationRequest request, DownloadRequiredException download) {
                received.add(download);
            }
        });
        try {
            session.setPage(s.url("/page"));
            TestSupport.settle(session);
            session.setPage(s.url("/attach"));
            TestSupport.settle(session);
            session.setPage(s.url("/bin"));
            TestSupport.settle(session);
            Check.equal(s.url("/page"), session.getPage(), "the page is unchanged by downloads");
            Check.equal("<p>page</p>", session.getContent(), "the content is unchanged by downloads");
            Check.equal(2, session.getHistories().size(), "history is unchanged by downloads");
            Check.equal(100, session.getProgress(), "progress is not stuck");
            Check.equal(2, received.size(), "both downloads reached the handler");
            Check.equal(DownloadRequiredException.Reason.ATTACHMENT, received.get(0).getReason(), "first reason");
            Check.contains(received.get(0).getContentDisposition(), "a.html", "file name hint");
            Check.equal(DownloadRequiredException.Reason.NOT_DISPLAYABLE, received.get(1).getReason(), "second reason");
        } finally {
            session.dispose();
        }
    }

    /**
     * Without a handler the download is only reported in the status text and nothing else changes.
     * <p>
     * 핸들러가 없으면 다운로드는 상태 메시지로만 알려지고 다른 것은 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDownloadWithoutHandlerIsReportedInStatus() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/zip", new TestHttpServer.Response(200).bytes("application/zip", new byte[] { 'P', 'K' }));
        // 컴포넌트는 이제 항상 다운로드 관리자를 가지므로(DownloadIntegrationTest), 핸들러가 없는 세션으로 확인한다.
        CookieStore cookies = new CookieStore();
        BrowserSession session = new BrowserSession(new NetworkPageLoader(
                new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader()), cookies), null, cookies);
        try {
            session.setPage(s.url("/zip"));
            TestSupport.settle(session);
            Check.equal("about:blank", session.getPage(), "page unchanged");
            Check.contains(session.getStatusText(), "Download required", "status");
            Check.equal(1, session.getHistories().size(), "history unchanged");
        } finally {
            session.dispose();
        }
    }

    /**
     * An image address is wrapped into a small generated document that refers to the image.
     * <p>
     * 이미지 주소는 이미지를 가리키는 작은 생성 문서로 감싼다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImageAddressIsWrappedInADocument() throws Exception {
        TestHttpServer s = http();
        s.routeFixed("/pic.png", new TestHttpServer.Response(200).bytes("image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G' }));
        SwingWebPanel p = panel();
        p.setPage(s.url("/pic.png"));
        settle(p, "image");
        Check.equal("text/html", p.getMimeType(), "generated document");
        Check.contains(p.getContent(), "<img src=\"" + s.url("/pic.png") + "\">", "the image is referenced");
    }

    /**
     * A page loaded without Content-Type is sniffed: HTML when it looks like HTML, otherwise plain text; the charset of a meta tag is honored.
     * <p>
     * Content-Type 없이 읽은 페이지는 내용으로 판별한다: HTML 처럼 보이면 HTML, 아니면 일반 텍스트. meta 태그의 문자셋을 따른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSniffingAndMetaCharset() throws Exception {
        TestHttpServer s = http();
        String html = "<html><head><meta charset=\"euc-kr\"></head><body>\ud55c\uae00</body></html>";
        s.routeFixed("/k", new TestHttpServer.Response(200).bytes("", html.getBytes("EUC-KR")));
        s.routeFixed("/t", new TestHttpServer.Response(200).bytes("", "just text".getBytes("UTF-8")));
        SwingWebPanel p = panel();
        p.setPage(s.url("/k"));
        settle(p, "k");
        Check.equal(html, p.getContent(), "EUC-KR page decoded with the meta charset");
        Check.equal("text/html", p.getMimeType(), "sniffed as HTML");
        p.setPage(s.url("/t"));
        settle(p, "t");
        Check.equal("text/plain", p.getMimeType(), "sniffed as plain text");
    }
}
