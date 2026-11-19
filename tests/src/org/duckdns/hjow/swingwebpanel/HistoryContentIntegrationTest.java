package org.duckdns.hjow.swingwebpanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestFtpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Back, forward and refresh over every kind of page through the real component (plan items P19-10, P19-02, P19-01): the four
 * <code>setContent</code> types (<code>about:text</code> entries restored from the stored source), an HTTP page that must not be cached,
 * HTTP 404 and 500 pages, a classpath page and its 404, an FTP listing and file, and a connection failure. Every move must show the same
 * URL, source, MIME type and status as the first visit, refreshes must not add entries, and a refresh of a stored-source page must not
 * touch the network.
 * <p>
 * 실제 컴포넌트로 모든 종류의 페이지에 대한 뒤로/앞으로/새로고침을 확인하는 시험(계획 P19-10, P19-02, P19-01): <code>setContent</code> 의 네 가지
 * 형식(저장한 원문으로 복원하는 <code>about:text</code> 항목), 캐시하면 안 되는 HTTP 페이지, HTTP 404/500 페이지, classpath 페이지와 그 404,
 * FTP 목록과 파일, 연결 실패. 모든 이동은 처음 방문과 같은 URL, 원문, MIME 타입, 상태를 보여 주어야 하고, 새로고침은 항목을 추가하지 않으며,
 * 저장한 원문으로 만드는 페이지의 새로고침은 네트워크를 건드리지 않아야 한다.
 */
public class HistoryContentIntegrationTest {

    /**
     * What a page must show when it is current.
     * <p>
     * 페이지가 현재일 때 보여 주어야 하는 것.
     */
    private static final class Expect {
        /** The address of the page. / 페이지의 주소. */
        final String url;
        /** The exact source or null when only a part is checked. / 정확한 원문. 일부만 확인하면 null. */
        final String content;
        /** A text that the source must contain, or null. / 원문에 들어 있어야 하는 텍스트. 없으면 null. */
        final String contains;
        /** The MIME type or null when it is not checked. / MIME 타입. 확인하지 않으면 null. */
        final String mime;
        /** The beginning of the status text. / 상태 메시지의 앞부분. */
        final String status;

        /**
         * Creates an expectation.
         * <p>
         * 기대를 만든다.
         *
         * @param url      the address / 주소
         * @param content  the exact source or null / 정확한 원문 또는 null
         * @param contains a part of the source or null / 원문의 일부 또는 null
         * @param mime     the MIME type or null / MIME 타입 또는 null
         * @param status   the beginning of the status text / 상태 메시지의 앞부분
         */
        Expect(String url, String content, String contains, String mime, String status) {
            this.url = url;
            this.content = content;
            this.contains = contains;
            this.mime = mime;
            this.status = status;
        }
    }

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * HTTP servers created by the case.
     * <p>
     * 사례가 만든 HTTP 서버.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * FTP servers created by the case.
     * <p>
     * 사례가 만든 FTP 서버.
     */
    private final List<TestFtpServer> ftpServers = new ArrayList<TestFtpServer>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HistoryContentIntegrationTest() {
    }

    /**
     * Releases everything the case created.
     * <p>
     * 사례가 만든 것을 모두 해제한다.
     */
    public void tearDown() {
        // 만든 패널을 모두 폐기한다.
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
     * Waits until the page matches the expectation and the load ended, then checks every field.
     * <p>
     * 페이지가 기대와 맞고 로딩이 끝날 때까지 기다린 뒤 모든 항목을 확인한다.
     *
     * @param p     the component / 컴포넌트
     * @param e     the expectation / 기대
     * @param where a description for failure messages / 실패 메시지용 설명
     */
    private static void await(SwingWebPanel p, Expect e, String where) {
        long end = System.currentTimeMillis() + 30000;
        // 기대한 주소·원문·상태가 되고 진행이 100 이 될 때까지 짧게 쉬며 기다린다.
        while (!matches(p, e) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
        Check.equal(e.url, p.getPage(), where + ": URL");
        if (e.content != null) {
            Check.equal(e.content, p.getContent(), where + ": the source is the stored one");
        }
        if (e.contains != null) {
            Check.contains(p.getContent(), e.contains, where + ": source");
        }
        if (e.mime != null) {
            Check.equal(e.mime, p.getMimeType(), where + ": MIME type");
        }
        Check.isTrue(p.getStatusText().startsWith(e.status), where + ": status '" + p.getStatusText() + "' starts with '" + e.status + "'");
        Check.equal(100, p.getProgress(), where + ": progress");
    }

    /**
     * Tells whether the component already shows the expected page.
     * <p>
     * 컴포넌트가 이미 기대한 페이지를 보여 주는지 알려 준다.
     *
     * @param p the component / 컴포넌트
     * @param e the expectation / 기대
     * @return true if the page is shown and the load ended / 페이지가 보이고 로딩이 끝났으면 true
     */
    private static boolean matches(SwingWebPanel p, Expect e) {
        return e.url.equals(p.getPage()) && p.getProgress() == 100 && p.getStatusText().startsWith(e.status)
                && (e.content == null || e.content.equals(p.getContent())) && (e.contains == null || p.getContent().indexOf(e.contains) >= 0);
    }

    /**
     * Joins the history of a component with commas.
     * <p>
     * 컴포넌트의 이력을 쉼표로 이어 붙인다.
     *
     * @param p the component / 컴포넌트
     * @return the URLs / URL
     */
    private static String urls(SwingWebPanel p) {
        StringBuilder sb = new StringBuilder();
        // 이력의 URL 을 순서대로 이어 붙인다.
        for (String u : p.getHistories()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(u);
        }
        return sb.toString();
    }

    /**
     * Every kind of page can be left and come back to: back and forward show the first visit again, history moves of a page that must not be
     * cached ask the server again, refreshes add no entries, and the refresh of a stored-source page does not use the network.
     * <p>
     * 모든 종류의 페이지를 떠났다가 돌아올 수 있다: 뒤로/앞으로는 처음 방문을 다시 보여 주고, 캐시하면 안 되는 페이지의 이력 이동은 서버에 다시 묻고,
     * 새로고침은 항목을 추가하지 않으며, 저장한 원문으로 만드는 페이지의 새로고침은 네트워크를 쓰지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackForwardAndRefreshOverEveryKindOfPage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        server.routeFixed("/h", TestHttpServer.Response.html("<p>http</p>").header("Cache-Control", "no-store"));
        server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/html", "<h1>custom 404 body</h1>"));
        server.routeFixed("/boom", new TestHttpServer.Response(500));
        server.start();
        servers.add(server);
        TestFtpServer ftp = new TestFtpServer().addDir("/pub").addFile("/pub/readme.txt", "hello ftp".getBytes("UTF-8"));
        ftp.start();
        ftpServers.add(ftp);
        TestHttpServer closed = new TestHttpServer();
        closed.start();
        int closedPort = closed.getPort();
        closed.stop();
        String refused = "http://127.0.0.1:" + closedPort + "/nothing";

        String markdown = "# Title\n\n*text* and `code`";
        String plain = "a <b>b</b>\r\nline2";
        String html = "<p>hello</p><script>document.title='x'</script>";
        String xhtml = "<?xml version=\"1.0\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>x</title></head><body><p>x</p></body></html>";
        String classpath = "classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html";
        Expect[] pages = new Expect[] {
            new Expect("about:text", markdown, null, "text/markdown", "Done"),
            new Expect("about:text", plain, null, "text/plain", "Done"),
            new Expect("about:text", html, null, "text/html", "Done"),
            new Expect("about:text", xhtml, null, "application/xhtml+xml", "Done"),
            new Expect(server.url("/h"), "<p>http</p>", null, "text/html", "Done"),
            new Expect(server.url("/missing"), "<h1>custom 404 body</h1>", null, null, "Error 404"),
            new Expect(server.url("/boom"), null, "Internal Server Error", null, "Error 500"),
            new Expect(classpath, null, "classpath resource", "text/html", "Done"),
            new Expect("classpath://org/duckdns/hjow/swingwebpanel/testdata/none.html", null, null, null, "Error 404"),
            new Expect(ftp.url("/pub/"), null, "readme.txt", "text/html", "Done"),
            new Expect(ftp.url("/pub/readme.txt"), "hello ftp", null, null, "Done"),
            new Expect(refused, null, "Connection failed", null, "Error")
        };
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);

        // 첫 방문: 앞의 네 개는 setContent, 나머지는 주소 이동이다.
        p.setContent(markdown, "text/markdown");
        await(p, pages[0], "first visit 0");
        p.setContent(plain, "text/plain");
        await(p, pages[1], "first visit 1");
        p.setContent(html, "text/html");
        await(p, pages[2], "first visit 2");
        p.setContent(xhtml, "application/xhtml+xml");
        await(p, pages[3], "first visit 3");
        // 나머지 페이지는 주소로 이동한다.
        for (int i = 4; i < pages.length; i++) {
            p.setPage(pages[i].url);
            await(p, pages[i], "first visit " + i);
        }
        Check.equal(pages.length + 1, p.getHistories().size(), "every page is one entry (blank page first): " + urls(p));
        Check.equal(1, server.count("/h"), "the page was requested once so far");

        // 뒤로: 마지막 페이지에서 첫 페이지까지 한 칸씩 되돌아가며 처음 방문과 같은지 확인한다.
        for (int i = pages.length - 2; i >= 0; i--) {
            p.back();
            await(p, pages[i], "back to " + i);
        }
        p.back();
        await(p, new Expect("about:blank", "", null, null, ""), "back to the blank page");
        Check.equal(0, p.getHistories().indexOf("about:blank"), "the first entry is the blank page");
        Check.equal(2, server.count("/h"), "going back to a page that must not be cached asked the server again");

        // 앞으로: 첫 페이지에서 마지막 페이지까지 한 칸씩 나아가며 다시 확인한다.
        for (int i = 0; i < pages.length; i++) {
            p.forward();
            await(p, pages[i], "forward to " + i);
        }
        Check.equal(3, server.count("/h"), "going forward asked the server again");
        Check.equal(pages.length + 1, p.getHistories().size(), "history moves add no entries");

        // 새로고침: 항목을 추가하지 않고 같은 페이지를 다시 보여 준다.
        p.refresh();
        await(p, pages[pages.length - 1], "refresh of the failure page");
        Check.equal(pages.length + 1, p.getHistories().size(), "refresh adds no entry");
        p.back(7);
        await(p, pages[4], "back(7) to the http page");
        Check.equal(4, server.count("/h"), "back(7) asked the server");
        p.refresh();
        await(p, pages[4], "refresh of the http page");
        Check.equal(5, server.count("/h"), "a refresh of a page that must not be cached asked the server");
        p.refresh(true);
        await(p, pages[4], "forced refresh of the http page");
        Check.equal(6, server.count("/h"), "a forced refresh asked the server");
        Check.equal("no-cache", server.last("/h").header("Cache-Control"), "a forced refresh sends no-cache");
        Check.equal(pages.length + 1, p.getHistories().size(), "refreshes add no entries: " + urls(p));
        // 저장한 원문으로 만드는 페이지의 새로고침은 네트워크를 쓰지 않는다.
        p.back(3);
        await(p, pages[1], "back(3) to the plain text page");
        int before = server.totalRequests();
        p.refresh();
        await(p, pages[1], "refresh of the plain text page");
        p.refresh(true);
        await(p, pages[1], "forced refresh of the plain text page");
        Check.equal(before, server.totalRequests(), "refreshing a stored-source page does not touch the network");
        Check.equal(pages.length + 1, p.getHistories().size(), "still the same number of entries");
        p.forward(2);
        await(p, pages[3], "forward(2) to the xhtml page");
    }

    /**
     * A navigation after going back deletes the entries in front and the deleted pages cannot come back; a stored-source page that was
     * left by <code>setPage</code> is restored from its source and not from its URL.
     * <p>
     * 뒤로 간 뒤의 이동은 앞쪽 항목을 지우고 지워진 페이지는 돌아올 수 없다. <code>setPage</code> 로 떠난 저장 원문 페이지는 URL 이 아니라 원문으로 복원된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNewNavigationAfterBackDeletesForwardPages() throws Exception {
        TestHttpServer server = new TestHttpServer();
        server.routeFixed("/a", TestHttpServer.Response.html("<p>a</p>"));
        server.routeFixed("/b", TestHttpServer.Response.html("<p>b</p>"));
        server.start();
        servers.add(server);
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        Expect text = new Expect("about:text", "<p>text</p>", null, "text/html", "Done");
        Expect a = new Expect(server.url("/a"), "<p>a</p>", null, "text/html", "Done");
        Expect b = new Expect(server.url("/b"), "<p>b</p>", null, "text/html", "Done");
        p.setContent("<p>text</p>", "text/html");
        await(p, text, "text");
        p.setPage(a.url);
        await(p, a, "a");
        p.setPage(b.url);
        await(p, b, "b");
        p.back(2);
        await(p, text, "back(2) to the text page");
        // 뒤로 간 상태에서 새 이동을 하면 앞쪽(a, b)이 지워진다.
        p.setContent("<p>other</p>", "text/html");
        await(p, new Expect("about:text", "<p>other</p>", null, "text/html", "Done"), "other");
        Check.equal("about:blank,about:text,about:text", urls(p), "the forward entries were deleted");
        p.forward();
        await(p, new Expect("about:text", "<p>other</p>", null, "text/html", "Done"), "forward with nothing in front does nothing");
        p.back();
        await(p, text, "back to the first text page");
        p.setPage(a.url);
        await(p, a, "a again");
        p.back();
        await(p, text, "the text page comes back from its source");
        Check.equal("<p>text</p>", p.getContent(), "the original source");
    }
}
