package org.duckdns.hjow.swingwebpanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of the HTTP cache as seen through the public component (plan items P19-03, P19-04, P19-06, P01-09, P01-15): a second navigation to
 * a fresh page does not reach the server, <code>refresh()</code> follows the cache rules, <code>refresh(true)</code> bypasses them, history
 * moves use the stored copy, a 304 keeps showing the stored content, <code>clean()</code> empties the cache and components do not share
 * responses.
 * <p>
 * 공개 컴포넌트로 본 HTTP 캐시 시험(계획 P19-03, P19-04, P19-06, P01-09, P01-15): 신선한 페이지로의 두 번째 이동은 서버에 닿지 않고,
 * <code>refresh()</code> 는 캐시 규칙을 따르며 <code>refresh(true)</code> 는 우회하고, 이력 이동은 저장본을 쓰며, 304 가 와도 저장된 내용이
 * 계속 보이고, <code>clean()</code> 은 캐시를 비우고, 컴포넌트는 응답을 공유하지 않는다.
 */
public class CacheIntegrationTest {

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Server of the case.
     * <p>
     * 사례의 서버.
     */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CacheIntegrationTest() {
    }

    /**
     * Starts the server before each case.
     * <p>
     * 사례마다 서버를 시작한다.
     *
     * @throws IOException if the server cannot start / 서버를 시작할 수 없으면
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.start();
    }

    /**
     * Disposes the panels and stops the server.
     * <p>
     * 패널을 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        // 만든 패널을 모두 정리한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Creates a panel that is disposed after the case.
     * <p>
     * 사례가 끝나면 폐기되는 패널을 만든다.
     *
     * @return panel / 패널
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Waits until a panel shows a URL and has finished loading.
     * <p>
     * 패널이 URL 을 보여 주고 로딩을 마칠 때까지 기다린다.
     *
     * @param p   panel / 패널
     * @param url expected page URL / 기대하는 페이지 URL
     */
    private static void waitFor(SwingWebPanel p, String url) {
        long end = System.currentTimeMillis() + 30000;
        // 기대한 페이지가 확정되고 진행이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || p.getStatusText().startsWith("Loading"))
                && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
        Check.equal(url, p.getPage(), "the page must be shown");
        Check.equal(100, p.getProgress(), "the load must be finished");
    }

    /**
     * Registers a cacheable page with a Cache-Control value and an ETag.
     * <p>
     * Cache-Control 값과 ETag 가 있는 캐시 가능한 페이지를 등록한다.
     *
     * @param path         path / 경로
     * @param cacheControl Cache-Control value or null / Cache-Control 값 또는 null
     * @param body         body / 본문
     */
    private void page(String path, String cacheControl, String body) {
        TestHttpServer.Response r = TestHttpServer.Response.html(body).header("ETag", "\"" + path + "\"");
        if (cacheControl != null) {
            r.header("Cache-Control", cacheControl);
        }
        server.routeFixed(path, r);
    }

    /**
     * P19-03: a second navigation to a fresh page is answered from the cache; the received text is returned unchanged.
     * <p>
     * P19-03: 신선한 페이지로의 두 번째 이동은 캐시에서 답하며 수신 원문은 그대로 반환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSecondNavigationUsesCache() throws Exception {
        page("/p", "max-age=60", "<p>cached\r\ncontent</p>");
        SwingWebPanel p = panel();
        String url = server.url("/p");
        p.setPage(url);
        waitFor(p, url);
        p.setPage(url);
        waitFor(p, url);
        Check.equal(1, server.count("/p"), "the second navigation did not reach the server");
        Check.equal("<p>cached\r\ncontent</p>", p.getContent(), "the received text is unchanged");
    }

    /**
     * P01-09/P19-06: refresh() follows the cache rules and refresh(true) goes to the server with no-cache headers.
     * <p>
     * P01-09/P19-06: refresh() 는 캐시 규칙을 따르고 refresh(true) 는 no-cache 헤더로 서버에 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshAndForcedRefresh() throws Exception {
        page("/r", "max-age=60", "<p>r</p>");
        SwingWebPanel p = panel();
        String url = server.url("/r");
        p.setPage(url);
        waitFor(p, url);
        p.refresh();
        waitFor(p, url);
        Check.equal(1, server.count("/r"), "a normal refresh of a fresh page uses the cache");
        p.refresh(true);
        waitFor(p, url);
        Check.equal(2, server.count("/r"), "a forced refresh reaches the server");
        Check.equal("no-cache", server.last("/r").header("Cache-Control"), "with a no-cache request header");
        p.refresh();
        waitFor(p, url);
        Check.equal(2, server.count("/r"), "the forced response is stored and fresh again");
    }

    /**
     * P19-01/P19-06: back and forward use the stored copies even when the pages are stale; a new navigation does not.
     * <p>
     * P19-01/P19-06: 뒤로/앞으로 가기는 페이지가 오래되었어도 저장본을 쓰며 새 이동은 그렇지 않다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHistoryMovesUseStoredCopies() throws Exception {
        server.routeFixed("/a", TestHttpServer.Response.html("<p>a</p>"));
        server.routeFixed("/b", TestHttpServer.Response.html("<p>b</p>"));
        SwingWebPanel p = panel();
        String a = server.url("/a");
        String b = server.url("/b");
        p.setPage(a);
        waitFor(p, a);
        p.setPage(b);
        waitFor(p, b);
        p.back();
        waitFor(p, a);
        Check.equal("<p>a</p>", p.getContent(), "page a after back");
        Check.equal(1, server.count("/a"), "back used the stored copy of a");
        p.forward();
        waitFor(p, b);
        Check.equal(1, server.count("/b"), "forward used the stored copy of b");
        p.setPage(a);
        waitFor(p, a);
        Check.equal(2, server.count("/a"), "a new navigation to a stale page asks the server again");
    }

    /**
     * P19-04: a 304 answer to the conditional refresh keeps showing the stored content instead of an empty page.
     * <p>
     * P19-04: 조건부 새로고침에 304 가 와도 빈 페이지가 아니라 저장된 내용이 계속 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNotModifiedKeepsContent() throws Exception {
        server.route("/n", new TestHttpServer.Handler() {
            /**
             * Answers 304 to a matching If-None-Match.
             * <p>
             * If-None-Match 가 맞으면 304 로 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if ("\"tag\"".equals(request.header("If-None-Match"))) {
                    return new TestHttpServer.Response(304).header("ETag", "\"tag\"");
                }
                return TestHttpServer.Response.html("<p>stored page</p>").header("ETag", "\"tag\"").header("Cache-Control", "max-age=0");
            }
        });
        SwingWebPanel p = panel();
        String url = server.url("/n");
        p.setPage(url);
        waitFor(p, url);
        p.refresh();
        waitFor(p, url);
        Check.equal(2, server.count("/n"), "the refresh sent a conditional request");
        Check.equal("\"tag\"", server.last("/n").header("If-None-Match"), "with the validator");
        Check.equal("<p>stored page</p>", p.getContent(), "the stored content is still shown");
    }

    /**
     * P01-15: clean() removes the cached responses of the component.
     * <p>
     * P01-15: clean() 은 컴포넌트의 저장된 응답을 지운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanEmptiesCache() throws Exception {
        page("/c", "max-age=60", "<p>c</p>");
        SwingWebPanel p = panel();
        String url = server.url("/c");
        p.setPage(url);
        waitFor(p, url);
        p.clean();
        waitFor(p, "about:blank");
        p.setPage(url);
        waitFor(p, url);
        Check.equal(2, server.count("/c"), "after clean() the page is fetched again");
    }

    /**
     * P19-03: two components never share cached responses.
     * <p>
     * P19-03: 두 컴포넌트는 저장된 응답을 공유하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testComponentsDoNotShareCaches() throws Exception {
        page("/s", "max-age=60", "<p>s</p>");
        SwingWebPanel one = panel();
        SwingWebPanel two = panel();
        String url = server.url("/s");
        one.setPage(url);
        waitFor(one, url);
        two.setPage(url);
        waitFor(two, url);
        Check.equal(2, server.count("/s"), "each component fetched the page itself");
        two.clean();
        waitFor(two, "about:blank");
        one.setPage(url);
        waitFor(one, url);
        Check.equal(2, server.count("/s"), "the clean() of one component did not empty the cache of the other");
    }
}
