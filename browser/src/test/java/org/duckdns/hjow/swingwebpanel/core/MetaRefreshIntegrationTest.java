package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The timed redirect of <code>meta http-equiv=refresh</code> through the real component and real HTTP (plan item P07-07-c, LEG-15; HTML5
 * REC 2014-10-28 section 4.2.5.3): the page is replaced in the history after the document has loaded, a refresh without an address loads
 * the page again, the first refresh wins, the redirect does not depend on JavaScript, it is cancelled when the page is left, an address that
 * the scheme policy blocks is not followed, and a refresh in a frame navigates the frame.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 <code>meta http-equiv=refresh</code> 의 예약된 이동(계획 P07-07-c, LEG-15, HTML5 REC 2014-10-28 §4.2.5.3): 문서가 로드된 뒤 페이지가
 * 이력에서 바뀌고, 주소 없는 refresh 는 페이지를 다시 읽고, 첫 refresh 가 이기고, 이동은 JavaScript 와 무관하고, 페이지를 떠나면 취소되고, 스킴 정책이 막는 주소는
 * 따라가지 않으며, 프레임 안의 refresh 는 프레임을 이동시킨다.
 */
public class MetaRefreshIntegrationTest {

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
    public MetaRefreshIntegrationTest() {
    }

    /**
     * Starts the server with the pages.
     * <p>
     * 페이지가 있는 서버를 시작한다.
     *
     * @throws IOException on failure / 실패 시
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.start();
        server.routeFixed("/a", TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=/b\"><p>a</p>"));
        server.routeFixed("/b", TestHttpServer.Response.html("<!doctype html><title>B</title><p>b</p><script>sessionStorage.setItem('b', location.href + '|' + (window.parent !== window));</script>"));
        server.routeFixed("/c", TestHttpServer.Response.html("<!doctype html><title>C</title><p>c</p>"));
        server.routeFixed("/first", TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=/b\"><meta http-equiv=\"refresh\" content=\"0;url=/c\"><p>x</p>"));
        server.routeFixed("/slow", TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"2;url=/b\"><p>slow</p>"));
        server.routeFixed("/js", TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=/b\"><p>js</p><script>sessionStorage.setItem('js', 'ran');</script>"));
        server.routeFixed("/blocked1", TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=javascript:alert(1)\"><p>1</p>"));
        server.routeFixed("/blocked2", TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=file:///c:/x.html\"><p>2</p>"));
        server.routeFixed("/relative", TestHttpServer.Response.html("<!doctype html><base href=\"/dir/\"><meta http-equiv=\"refresh\" content=\"0;url=target\"><p>r</p>"));
        server.routeFixed("/dir/target", TestHttpServer.Response.html("<!doctype html><p>target</p>"));
        server.routeFixed("/frame", TestHttpServer.Response.html("<!doctype html><title>P</title><iframe id=\"f\" src=\"/a\" width=\"100\" height=\"50\"></iframe><p>parent</p>"));
        server.routeFixed("/sandboxed", TestHttpServer.Response.html("<!doctype html><title>S</title><iframe id=\"f\" src=\"/a\" sandbox></iframe>"));
        server.route("/same", new TestHttpServer.Handler() {
            /**
             * Answers with a page that refreshes itself.
             * <p>
             * 자신을 새로고침하는 페이지로 응답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.html("<!doctype html><meta http-equiv=\"refresh\" content=\"1\"><title>same</title><p>same</p>");
            }
        });
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
     * Creates a component.
     * <p>
     * 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Waits until the component shows an address and the load ended.
     * <p>
     * 컴포넌트가 주소를 보여 주고 로딩이 끝날 때까지 기다린다.
     *
     * @param p       the component / 컴포넌트
     * @param url     the address / 주소
     * @param timeout the longest wait in milliseconds / 가장 오래 기다릴 시간(밀리초)
     * @return true if the address was shown / 주소를 보여 주었으면 true
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static boolean await(SwingWebPanel p, String url, long timeout) throws Exception {
        long end = System.currentTimeMillis() + timeout;
        // 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while (System.currentTimeMillis() < end) {
            if (url.equals(p.getPage()) && p.getProgress() == 100 && !p.getStatusText().startsWith("Loading")) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
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
     * A refresh with an address replaces the page after the document has loaded: the history keeps one entry for the two pages, and the
     * scripts of the first page do not matter.
     * <p>
     * 주소가 있는 refresh 는 문서가 로드된 뒤 페이지를 바꾼다: 두 페이지에 이력 항목은 하나이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshWithAnAddressReplacesThePage() throws Exception {
        SwingWebPanel p = panel();
        p.setPage(server.url("/a"));
        Check.isTrue(await(p, server.url("/b"), 30000), "the page a refreshed to b: " + p.getPage());
        Vector<String> history = p.getHistories();
        Check.equal(2, history.size(), "about:blank and one entry for both pages: " + history);
        Check.equal(server.url("/b"), history.get(1), "the entry is the target (the redirect replaces the entry)");
        Check.equal(1, server.count("/a"), "a was requested once");
        Check.equal(1, server.count("/b"), "b was requested once");
        p.back();
        Check.isTrue(await(p, "about:blank", 30000), "going back leaves both pages: " + p.getPage());
    }

    /**
     * The first valid refresh wins over later ones.
     * <p>
     * 처음의 올바른 refresh 가 뒤의 것보다 이긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFirstRefreshWins() throws Exception {
        SwingWebPanel p = panel();
        p.setPage(server.url("/first"));
        Check.isTrue(await(p, server.url("/b"), 30000), "the first meta element won: " + p.getPage());
        Thread.sleep(500);
        Check.equal(0, server.count("/c"), "the second one was not used");
    }

    /**
     * The redirect does not depend on JavaScript: it works when scripting is off, and the scripts of the page did not run.
     * <p>
     * 이동은 JavaScript 와 무관하다: 스크립트를 끄고도 동작하며 페이지의 스크립트는 실행되지 않았다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshWorksWithoutJavaScript() throws Exception {
        SwingWebPanel p = panel();
        p.setJavascriptEnabled(false);
        p.setPage(server.url("/js"));
        Check.isTrue(await(p, server.url("/b"), 30000), "refreshed with scripting off: " + p.getPage());
        Check.isNull(stored(p, "js"), "the scripts of the first page did not run");
        Check.isNull(stored(p, "b"), "nor did those of the second page");
    }

    /**
     * A refresh without an address loads the same page again, with replacement of the history entry, until the page is left.
     * <p>
     * 주소가 없는 refresh 는 페이지를 다시 읽고 이력 항목을 바꾸며 페이지를 떠날 때까지 되풀이한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshWithoutAnAddressLoadsThePageAgain() throws Exception {
        SwingWebPanel p = panel();
        p.setPage(server.url("/same"));
        long end = System.currentTimeMillis() + 30000;
        // 서버가 같은 페이지를 두 번째로 요청받을 때까지 기다린다.
        while (server.count("/same") < 3 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.isTrue(server.count("/same") >= 3, "the page was requested again and again: " + server.count("/same"));
        Check.equal(2, p.getHistories().size(), "one entry (the redirect replaces the entry): " + p.getHistories());
        p.setPage(server.url("/c"));
        Check.isTrue(await(p, server.url("/c"), 30000), "left the page");
        int count = server.count("/same");
        Thread.sleep(2500);
        Check.equal(count, server.count("/same"), "no request after the page was left");
    }

    /**
     * A refresh that has not come due is cancelled when the page is left or the component is disposed.
     * <p>
     * 아직 때가 되지 않은 refresh 는 페이지를 떠나거나 컴포넌트를 폐기하면 취소된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPendingRefreshIsCancelled() throws Exception {
        SwingWebPanel p = panel();
        p.setPage(server.url("/slow"));
        Check.isTrue(await(p, server.url("/slow"), 30000), "loaded");
        p.setPage(server.url("/c"));
        Check.isTrue(await(p, server.url("/c"), 30000), "left the page");
        Thread.sleep(3000);
        Check.equal(server.url("/c"), p.getPage(), "still on c");
        Check.equal(0, server.count("/b"), "b was never requested");
        SwingWebPanel q = panel();
        q.setPage(server.url("/slow"));
        Check.isTrue(await(q, server.url("/slow"), 30000), "loaded in the second component");
        q.dispose();
        Thread.sleep(3000);
        Check.equal(0, server.count("/b"), "a disposed component does not follow the refresh");
    }

    /**
     * The waiting time counts from the loading of the document: the redirect does not come before the seconds of the element.
     * <p>
     * 기다리는 시간은 문서 로딩부터 센다: 이동은 요소의 초보다 일찍 일어나지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshWaitsForItsSeconds() throws Exception {
        SwingWebPanel p = panel();
        long start = System.currentTimeMillis();
        p.setPage(server.url("/slow"));
        Check.isTrue(await(p, server.url("/slow"), 30000), "loaded");
        long loaded = System.currentTimeMillis();
        Thread.sleep(800);
        Check.equal(server.url("/slow"), p.getPage(), "still on the page before the two seconds passed: " + (System.currentTimeMillis() - loaded));
        Check.isTrue(await(p, server.url("/b"), 30000), "redirected: " + p.getPage());
        Check.isTrue(System.currentTimeMillis() - loaded >= 1800, "after about two seconds from the load: " + (System.currentTimeMillis() - loaded) + " since " + (loaded - start));
    }

    /**
     * Addresses that the scheme policy blocks are not followed, and a relative address is resolved against the <code>base</code> element.
     * <p>
     * 스킴 정책이 막는 주소는 따라가지 않고 상대 주소는 <code>base</code> 요소로 해석한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedAndRelativeAddresses() throws Exception {
        SwingWebPanel p = panel();
        p.setPage(server.url("/blocked1"));
        Check.isTrue(await(p, server.url("/blocked1"), 30000), "loaded");
        Thread.sleep(800);
        Check.equal(server.url("/blocked1"), p.getPage(), "javascript: is not followed");
        p.setPage(server.url("/blocked2"));
        Check.isTrue(await(p, server.url("/blocked2"), 30000), "loaded");
        Thread.sleep(800);
        Check.equal(server.url("/blocked2"), p.getPage(), "file: is not followed");
        p.setPage(server.url("/relative"));
        Check.isTrue(await(p, server.url("/dir/target"), 30000), "resolved against the base element: " + p.getPage());
    }

    /**
     * A page that is given with <code>setContent</code> can refresh to an absolute address (it has no address of its own to resolve a
     * relative one against, and no address to load again).
     * <p>
     * <code>setContent</code> 로 준 페이지는 절대 주소로 새로고침할 수 있다(상대 주소를 해석할 자신의 주소도, 다시 읽을 주소도 없다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSetContentPages() throws Exception {
        SwingWebPanel p = panel();
        p.setContent("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=" + server.url("/b") + "\"><p>text</p>", "text/html");
        Check.isTrue(await(p, server.url("/b"), 30000), "an absolute address: " + p.getPage());
        SwingWebPanel q = panel();
        q.setContent("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=/b\"><p>text</p>", "text/html");
        Check.isTrue(await(q, "about:text", 30000), "set");
        Thread.sleep(800);
        Check.equal("about:text", q.getPage(), "a relative address cannot be resolved");
        SwingWebPanel r = panel();
        r.setContent("<!doctype html><meta http-equiv=\"refresh\" content=\"0\"><p>text</p>", "text/html");
        Check.isTrue(await(r, "about:text", 30000), "set");
        Thread.sleep(800);
        Check.equal("about:text", r.getPage(), "no address to load again");
    }

    /**
     * A refresh in a frame navigates the frame, not the page that has the frame; a sandboxed frame without <code>allow-scripts</code>
     * does not refresh.
     * <p>
     * 프레임 안의 refresh 는 프레임이 있는 페이지가 아니라 프레임을 이동시킨다. <code>allow-scripts</code> 없는 샌드박스 프레임은 새로고침하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshInAFrame() throws Exception {
        SwingWebPanel p = panel();
        p.setPage(server.url("/frame"));
        Check.isTrue(await(p, server.url("/frame"), 30000), "loaded");
        long end = System.currentTimeMillis() + 30000;
        // 프레임이 /b 로 이동할 때까지 기다린다.
        while (server.count("/b") < 1 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.equal(1, server.count("/b"), "the frame loaded b");
        Check.equal(server.url("/frame"), p.getPage(), "the page of the frame stays");
        SwingWebPanel q = panel();
        int before = server.count("/b");
        q.setPage(server.url("/sandboxed"));
        Check.isTrue(await(q, server.url("/sandboxed"), 30000), "loaded");
        Thread.sleep(1500);
        Check.equal(before, server.count("/b"), "the sandboxed frame did not refresh");
    }
}
