package org.duckdns.hjow.swingwebpanel.core;

import java.util.Map;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Counts what repeated use of the component leaves behind (plan item P21-07): after many cycles of creating a component, loading a page
 * with sub resources and a request that is still running, cleaning and disposing, no connection stays open on the server and the number of
 * threads of the product (<code>swingwebpanel-...</code>) does not grow with the number of components. The memory side of the same
 * question — a cleaned or disposed component can be collected without the help of a finalizer — is checked by
 * <code>ComponentLeakIntegrationTest</code> with weak references; the sound output lines are closed by every kind of leaving the page
 * (<code>AudioIntegrationTest</code>). The garbage collector runs when it wants to, so nothing here depends on the moment of a collection.
 * <p>
 * 컴포넌트를 되풀이해 쓴 뒤 무엇이 남는지 센다(계획 P21-07): 컴포넌트를 만들고, 하위 리소스와 아직 실행 중인 요청이 있는 페이지를 읽고, clean 하고,
 * 폐기하는 주기를 여러 번 되풀이한 뒤에도 서버에 열린 연결이 남지 않고 제품의 스레드(<code>swingwebpanel-...</code>) 수가 컴포넌트 수에 따라 늘지 않는다.
 * 같은 질문의 메모리 쪽(정리하거나 폐기한 컴포넌트가 finalizer 의 도움 없이 수거될 수 있는가)은 <code>ComponentLeakIntegrationTest</code> 가 약한 참조로
 * 확인하고, 소리 출력 라인은 페이지를 떠나는 모든 방법에서 닫힌다(<code>AudioIntegrationTest</code>). 가비지 컬렉터는 원할 때 실행되므로 여기서는 수거
 * 시점에 기대지 않는다.
 */
public class ResourceCountsIntegrationTest {

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ResourceCountsIntegrationTest() {
    }

    /**
     * Starts the server with a page that has sub resources and a slow request.
     * <p>
     * 하위 리소스와 느린 요청이 있는 페이지의 서버를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/app", TestHttpServer.Response.html("<!doctype html><meta charset=utf-8><link rel=stylesheet href=/s.css><p>app</p>"
                + "<script src=/s.js></script><script>var x = new XMLHttpRequest(); x.open('GET', '/slow'); x.send();"
                + "setInterval(function () { var y = new XMLHttpRequest(); y.open('GET', '/s.css'); y.send(); }, 40);</" + "script>"));
        server.routeFixed("/s.css", TestHttpServer.Response.ok("text/css", "p{color:#f00}"));
        server.routeFixed("/s.js", TestHttpServer.Response.ok("application/javascript", "var loaded = true;"));
        server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(600));
        server.start();
    }

    /**
     * Stops the server.
     * <p>
     * 서버를 멈춘다.
     */
    public void tearDown() {
        server.stop();
    }

    /**
     * Counts the live threads of the product.
     * <p>
     * 제품의 살아 있는 스레드를 센다.
     *
     * @return the number / 개수
     */
    private static int productThreads() {
        int n = 0;
        // 모든 스레드 중 이름이 제품의 접두사로 시작하는 것을 센다.
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            if (e.getKey().isAlive() && e.getKey().getName().startsWith("swingwebpanel-")) {
                n++;
            }
        }
        return n;
    }

    /**
     * One cycle: a component loads the page, may be cleaned, and is disposed while the slow request and the polling still run.
     * <p>
     * 한 주기: 컴포넌트가 페이지를 읽고, 정리될 수 있으며, 느린 요청과 폴링이 아직 실행되는 동안 폐기된다.
     *
     * @param cleanFirst true to call clean() before dispose() / dispose() 전에 clean() 을 부르려면 true
     * @throws Exception on failure / 실패 시
     */
    private void cycle(boolean cleanFirst) throws Exception {
        SwingWebPanel p = new SwingWebPanel();
        try {
            p.setPage(server.url("/app"));
            long end = System.currentTimeMillis() + 30000;
            // 페이지가 확정되고 로딩이 끝날 때까지 기다린다.
            while ((!server.url("/app").equals(p.getPage()) || p.getProgress() != 100) && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            Check.equal(server.url("/app"), p.getPage(), "the page was confirmed");
            if (cleanFirst) {
                p.clean();
            }
        } finally {
            p.dispose();
        }
    }

    /**
     * Fifteen cycles leave no open connection and no growth of the product threads.
     * <p>
     * 열다섯 번의 주기 뒤에 열린 연결도, 제품 스레드의 증가도 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRepeatedCyclesLeaveNoSocketsAndNoGrowingThreads() throws Exception {
        cycle(false);
        cycle(true);
        Thread.sleep(1500);
        int baseline = productThreads();
        // 컴포넌트를 만들고 읽고 정리하고 폐기하는 주기를 되풀이한다.
        for (int i = 0; i < 15; i++) {
            cycle(i % 3 == 0);
        }
        long end = System.currentTimeMillis() + 20000;
        // 서버에 열린 연결이 모두 닫힐 때까지 기다린다.
        while (server.getOpenConnections() > 0 && System.currentTimeMillis() < end) {
            Thread.sleep(50);
        }
        Check.equal(0, server.getOpenConnections(), "no connection stays open on the server");
        end = System.currentTimeMillis() + 30000;
        // 쉬는 스레드가 정리되어 수가 기준 근처로 돌아올 때까지 기다린다.
        while (productThreads() > baseline + 2 && System.currentTimeMillis() < end) {
            Thread.sleep(100);
        }
        int after = productThreads();
        Check.isTrue(after <= baseline + 2, "the product threads did not grow with the components: baseline " + baseline + ", after " + after);
        int requests = server.totalRequests();
        Thread.sleep(600);
        Check.equal(requests, server.totalRequests(), "the disposed pages send no more requests");
    }
}
