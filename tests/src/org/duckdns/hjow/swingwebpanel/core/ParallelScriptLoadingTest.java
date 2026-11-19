package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The parallel loading of external scripts through the real component and real HTTP (plan item P15-04-a): the requests of async, defer and
 * parser-blocking scripts are in flight at the same time, the order of execution follows the HTML Standard, and a response that arrives
 * after the navigation was replaced never runs.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 외부 스크립트 병렬 로딩을 확인하는 시험(계획 P15-04-a): async, defer, 파서 차단 스크립트의 요청이 동시에 진행되고,
 * 실행 순서는 HTML 표준을 따르며, 이동이 교체된 뒤에 도착한 응답은 실행되지 않는다.
 */
public class ParallelScriptLoadingTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ParallelScriptLoadingTest() {
    }

    /**
     * Creates the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Reads a value that a script stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 값을 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String value(SwingWebPanel panel, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until a stored value equals the expected one.
     * <p>
     * 저장된 값이 기대한 값이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param key      the key / 키
     * @param expected the value / 기대 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitValue(SwingWebPanel panel, String key, String expected) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (!expected.equals(value(panel, key)) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(expected, value(panel, key), "stored value " + key);
    }

    /**
     * Waits until the component confirmed a page.
     * <p>
     * 컴포넌트가 페이지를 확정할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param url   the URL / URL
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 페이지가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * A script that appends its name to the stored order.
     * <p>
     * 자기 이름을 저장된 순서에 덧붙이는 스크립트.
     *
     * @param name the name / 이름
     * @return the script text / 스크립트 텍스트
     */
    private static String marker(String name) {
        return "sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+'" + name + ",');";
    }

    /**
     * P15-04-a: the requests of an async, a defer and a parser-blocking script are in flight at the same time (a rendezvous that only opens
     * when all three arrived), and the scripts run in the allowed order.
     * <p>
     * P15-04-a: async, defer, 파서 차단 스크립트의 요청이 동시에 진행되고(셋이 모두 도착해야 열리는 집합 지점), 스크립트는 허용된 순서로 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRequestsOfAsyncDeferAndBlockingScriptsOverlap() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch arrived = new CountDownLatch(3);
        SwingWebPanel panel = newPanel();
        try {
            String[] names = { "async", "defer", "blocking" };
            // 세 스크립트가 모두 도착해야 응답하는 경로를 등록한다. 순차 요청이면 영원히 열리지 않는다.
            for (int i = 0; i < names.length; i++) {
                final String name = names[i];
                server.route("/" + name + ".js", new TestHttpServer.Handler() {
                    /**
                     * Waits for the other requests and answers.
                     * <p>
                     * 다른 요청을 기다린 뒤 응답한다.
                     *
                     * @param request the request / 요청
                     * @return the script / 스크립트
                     * @throws Exception if interrupted / 인터럽트되면
                     */
                    public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                        arrived.countDown();
                        arrived.await(10, TimeUnit.SECONDS);
                        return TestHttpServer.Response.ok("application/javascript", marker(name));
                    }
                });
            }
            String html = "<!doctype html><script async src='async.js'></script><script defer src='defer.js'></script>"
                    + "<script src='blocking.js'></script><script>" + marker("inline") + "</script>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            Check.equal(0, arrived.getCount(), "all three requests were in flight together");
            String order = "," + value(panel, "order");
            Check.isTrue(order.indexOf(",blocking,") < order.indexOf(",inline,"), "blocking script before the next inline script: " + order);
            Check.isTrue(order.indexOf(",inline,") < order.indexOf(",defer,"), "defer after the parser: " + order);
            Check.isTrue(order.indexOf(",async,") >= 0, "the async script ran: " + order);
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-04-b: an async script whose response is ready runs while the parser still waits for a slow parser-blocking script (the page is
     * not confirmed yet), and the blocking script then keeps its place before the following inline script.
     * <p>
     * P15-04-b: 응답이 준비된 async 스크립트는 파서가 느린 파서 차단 스크립트를 기다리는 동안(페이지가 아직 확정되지 않은 상태) 실행되고,
     * 차단 스크립트는 뒤따르는 인라인 스크립트보다 앞의 자리를 지킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReadyAsyncRunsWhileParserWaitsForSlowBlockingScript() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch slowEntered = new CountDownLatch(1);
        final CountDownLatch slowRelease = new CountDownLatch(1);
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/fast.js", TestHttpServer.Response.ok("application/javascript", marker("async")));
            server.route("/slow.js", new TestHttpServer.Handler() {
                /**
                 * Holds the blocking script until the test releases it.
                 * <p>
                 * 시험이 풀어 줄 때까지 차단 스크립트를 붙잡는다.
                 *
                 * @param request the request / 요청
                 * @return the script / 스크립트
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    slowEntered.countDown();
                    slowRelease.await(30, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("application/javascript", marker("blocking"));
                }
            });
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><script async src='fast.js'></script>"
                    + "<script src='slow.js'></script><script>" + marker("inline") + "</script>"));
            server.start();
            panel.setPage(server.url("/page"));
            Check.isTrue(slowEntered.await(60, TimeUnit.SECONDS), "the blocking request is pending");
            awaitValue(panel, "order", "async,");
            Check.equal("about:blank", panel.getPage(), "the page is not confirmed while the parser waits");
            slowRelease.countDown();
            awaitPage(panel, server.url("/page"));
            Check.equal("async,blocking,inline,", value(panel, "order"), "the blocking script kept its place");
        } finally {
            slowRelease.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-04-b: the page is confirmed after DOMContentLoaded without waiting for a slow async script; the script runs when its response
     * arrives and the load event follows it.
     * <p>
     * P15-04-b: 페이지는 느린 async 스크립트를 기다리지 않고 DOMContentLoaded 뒤에 확정된다. 스크립트는 응답이 도착하면 실행되고 load 이벤트가
     * 그 뒤를 따른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageAppearsBeforeSlowAsyncScriptAndLoadFollows() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        SwingWebPanel panel = newPanel();
        try {
            server.route("/slow.js", new TestHttpServer.Handler() {
                /**
                 * Holds the async script until the test releases it.
                 * <p>
                 * 시험이 풀어 줄 때까지 async 스크립트를 붙잡는다.
                 *
                 * @param request the request / 요청
                 * @return the script / 스크립트
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(30, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("application/javascript", marker("slow"));
                }
            });
            String html = "<!doctype html><script>document.addEventListener('DOMContentLoaded',()=>{" + marker("dcl")
                    + "});window.addEventListener('load',()=>{" + marker("load") + "});</script><script async src='slow.js'></script>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            Check.isTrue(entered.await(60, TimeUnit.SECONDS), "the async request is pending");
            awaitPage(panel, server.url("/page"));
            Check.equal("dcl,", value(panel, "order"), "confirmed after DOMContentLoaded, before the async script and load");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
            release.countDown();
            awaitValue(panel, "order", "dcl,slow,load,");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-04-b: a script that a script inserts into the document is fetched through the same request layer, runs when its response
     * arrives (not inside the inserting script) and holds the load event back.
     * <p>
     * P15-04-b: 스크립트가 문서에 삽입한 스크립트는 같은 요청 계층으로 가져오고, 응답이 도착하면(삽입한 스크립트 안이 아니라) 실행되며 load 이벤트를
     * 붙잡는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDynamicExternalScriptThroughTheComponent() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/dyn.js", TestHttpServer.Response.ok("application/javascript", marker("dynamic")));
            String html = "<!doctype html><body><script>window.addEventListener('load',()=>{" + marker("load") + "});"
                    + "const s=document.createElement('script');s.src='dyn.js';document.body.appendChild(s);" + marker("inserter") + "</script></body>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitValue(panel, "order", "inserter,dynamic,load,");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-04-a: an async response that arrives after the navigation was replaced does not run in the new page, and the new navigation is not
     * held back by the old page's pending script request.
     * <p>
     * P15-04-a: 이동이 교체된 뒤 도착한 async 응답은 새 페이지에서 실행되지 않고, 새 이동은 이전 페이지의 대기 중인 스크립트 요청에 붙잡히지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLateAsyncResponseOfAReplacedNavigationNeverRuns() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        SwingWebPanel panel = newPanel();
        try {
            server.route("/late.js", new TestHttpServer.Handler() {
                /**
                 * Holds the response until the test releases it.
                 * <p>
                 * 시험이 풀어 줄 때까지 응답을 붙잡는다.
                 *
                 * @param request the request / 요청
                 * @return the script / 스크립트
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(15, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("application/javascript", marker("late"));
                }
            });
            server.routeFixed("/a", TestHttpServer.Response.html("<!doctype html><script async src='late.js'></script><p>a</p>"));
            server.routeFixed("/b", TestHttpServer.Response.html("<!doctype html><script>" + marker("b") + "</script><p>b</p>"));
            server.start();
            panel.setPage(server.url("/a"));
            Check.isTrue(entered.await(60, TimeUnit.SECONDS), "the async request is pending");
            panel.setPage(server.url("/b"));
            awaitPage(panel, server.url("/b"));
            release.countDown();
            Thread.sleep(300);
            Check.equal("b,", value(panel, "order"), "the late response of the replaced page did not run");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }
}
