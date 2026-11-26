package org.duckdns.hjow.swingwebpanel.core.frames;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * What a page with frames leaves behind (plan item P20-02, stage g; P21-03, P21-04, P21-05, P21-07, P22-09 for frames): the scripts, timers
 * and requests of frame documents end when the page is replaced, when the component is cleaned or disposed, and when the frame element is
 * removed; a slow frame document is cancelled and never runs; and after <code>dispose()</code> a component whose pages had frames (with
 * their own timers, requests and nested frames) is no longer reachable from any thread or static field (weak references, with no claim
 * about when the garbage collector runs) and the threads of the library do not grow with the number of pages.
 * <p>
 * 프레임이 있는 페이지가 무엇을 남기는지 확인한다(계획 P20-02 단계 g. 프레임에 대한 P21-03, P21-04, P21-05, P21-07, P22-09): 프레임 문서의 스크립트, 타이머,
 * 요청은 페이지가 교체되거나 컴포넌트가 clean/dispose 되거나 프레임 요소가 제거될 때 끝나고, 느린 프레임 문서는 취소되어 실행되지 않으며,
 * <code>dispose()</code> 뒤에는 프레임(자기 타이머, 요청, 중첩된 프레임이 있는)이 있던 페이지를 쓴 컴포넌트가 어떤 스레드나 정적 필드로도 도달되지 않고
 * (약한 참조로 확인하며 가비지 컬렉터가 언제 실행되는지는 주장하지 않는다) 라이브러리의 스레드는 페이지 수에 따라 늘지 않는다.
 */
public class FrameLifecycleTest {

    /**
     * Servers created by the case.
     * <p>
     * 사례가 만든 서버.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FrameLifecycleTest() {
    }

    /**
     * Stops the servers of the case.
     * <p>
     * 사례의 서버를 중지한다.
     */
    public void tearDown() {
        // 시작한 서버를 모두 중지한다.
        for (TestHttpServer s : servers) {
            s.stop();
        }
    }

    /**
     * Starts a server with a page whose frames keep working: an <code>iframe</code> with an interval that requests <code>/tick</code>, a
     * nested frame inside it with its own interval, and a <code>frameset</code>-free second frame.
     * <p>
     * 프레임이 계속 일하는 페이지가 있는 서버를 시작한다: <code>/tick</code> 을 요청하는 반복 타이머가 있는 <code>iframe</code>, 그 안에서 자기 반복 타이머를
     * 가진 중첩 프레임, 그리고 둘째 프레임.
     *
     * @return the server / 서버
     * @throws Exception if it cannot start / 시작할 수 없으면
     */
    private TestHttpServer serve() throws Exception {
        TestHttpServer server = new TestHttpServer();
        String tick = "<script>var n=0;setInterval(function(){var x=new XMLHttpRequest();x.open('GET','/tick');x.send();"
                + "localStorage.setItem('frame-tick',String(n++))},30)</script>";
        server.routeFixed("/inner", TestHttpServer.Response.html("<body>" + tick + "inner</body>"));
        server.routeFixed("/outer", TestHttpServer.Response.html("<body>" + tick + "<iframe src=\"/inner\" width=\"50\" height=\"30\"></iframe></body>"));
        server.routeFixed("/page", TestHttpServer.Response.html("<!DOCTYPE html><html><body><iframe src=\"/outer\" width=\"150\" height=\"80\"></iframe>"
                + "<iframe id=\"second\" srcdoc=\"" + tick.replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;") + "\" width=\"50\" height=\"30\"></iframe>"
                + "<script>setInterval(function(){var x=new XMLHttpRequest();x.open('GET','/tick');x.send()},30)</script></body></html>"));
        server.routeFixed("/tick", TestHttpServer.Response.ok("text/plain", "t"));
        server.routeFixed("/other", TestHttpServer.Response.html("<body>other</body>"));
        server.routeFixed("/slow", TestHttpServer.Response.html("<body><script>sessionStorage.setItem('slow-ran','yes')</script>slow</body>").delay(3000));
        server.routeFixed("/slowpage", TestHttpServer.Response.html("<!DOCTYPE html><html><body><iframe src=\"/slow\" width=\"100\" height=\"50\"></iframe></body></html>"));
        server.start();
        servers.add(server);
        return server;
    }

    /**
     * Waits until the page of the component is an address and its progress is complete.
     * <p>
     * 컴포넌트의 페이지가 어떤 주소가 되고 진행률이 완료될 때까지 기다린다.
     *
     * @param p   the component / 컴포넌트
     * @param url the address / 주소
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitLoaded(SwingWebPanel p, String url) throws Exception {
        long end = System.currentTimeMillis() + 40000;
        // 페이지가 확정되고 프레임까지 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "the page was confirmed");
        Check.equal(100, p.getProgress(), "the page and its frames finished loading");
    }

    /**
     * Waits until the requests of a path keep a count for a while (the timers that sent them have stopped), and returns that count.
     * <p>
     * 한 경로의 요청 수가 한동안 변하지 않을 때까지(요청을 보내던 타이머가 멈출 때까지) 기다리고 그 수를 반환한다.
     *
     * @param server the server / 서버
     * @param path   the path / 경로
     * @return the stable count / 안정된 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static int settledCount(TestHttpServer server, String path) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int last = server.count(path);
        // 1000 밀리초 동안 수가 그대로이면 멈춘 것으로 본다(타이머 주기는 30 밀리초이다). 바쁜 컴퓨터에서 서버가 이미 도착한 요청을 늦게 처리해도 멈춘 것으로 잘못 보지 않도록 창을 넉넉히 둔다.
        while (System.currentTimeMillis() < end) {
            Thread.sleep(1000);
            int now = server.count(path);
            if (now == last) {
                return now;
            }
            last = now;
        }
        return last;
    }

    /**
     * Counts the live threads that the library started (their names start with <code>swingwebpanel-</code>).
     * <p>
     * 라이브러리가 시작한 살아 있는 스레드(이름이 <code>swingwebpanel-</code> 로 시작)를 센다.
     *
     * @return the number of threads / 스레드 수
     */
    private static int libraryThreads() {
        int n = 0;
        // 모든 스레드의 이름을 훑어 라이브러리의 것을 센다.
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            if (e.getKey().isAlive() && e.getKey().getName().startsWith("swingwebpanel-")) {
                n++;
            }
        }
        return n;
    }

    /**
     * Replacing the page stops the scripts of all its frames, nested ones included: the ticks of the server stop coming.
     * <p>
     * 페이지를 교체하면 중첩된 프레임을 포함한 모든 프레임의 스크립트가 멈춘다: 서버로 오는 tick 이 끊긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReplacingThePageStopsTheScriptsOfAllFrames() throws Exception {
        TestHttpServer server = serve();
        SwingWebPanel p = FrameTestSupport.newPanel(400, 300);
        try {
            p.setPage(server.url("/page"));
            awaitLoaded(p, server.url("/page"));
            // 네 문서(최상위, outer, inner, srcdoc)의 타이머가 모두 일해 tick 이 쌓일 때까지 기다린다.
            Check.isTrue(server.awaitRequests(40, 30000), "the frames are ticking");
            p.setPage(server.url("/other"));
            awaitLoaded(p, server.url("/other"));
            int stable = settledCount(server, "/tick");
            Thread.sleep(1000);
            Check.equal(stable, server.count("/tick"), "no frame of the replaced page ticks any more");
        } finally {
            p.dispose();
        }
    }

    /**
     * <code>clean()</code> and <code>dispose()</code> stop the scripts of the frames as well.
     * <p>
     * <code>clean()</code> 과 <code>dispose()</code> 도 프레임의 스크립트를 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanAndDisposeStopTheScriptsOfAllFrames() throws Exception {
        TestHttpServer server = serve();
        SwingWebPanel p = FrameTestSupport.newPanel(400, 300);
        try {
            p.setPage(server.url("/page"));
            awaitLoaded(p, server.url("/page"));
            Check.isTrue(server.awaitRequests(40, 30000), "the frames are ticking");
            p.clean();
            Check.equal("about:blank", p.getPage(), "clean() shows the empty page");
            int stable = settledCount(server, "/tick");
            Thread.sleep(1000);
            Check.equal(stable, server.count("/tick"), "no frame ticks after clean()");
            p.setPage(server.url("/page"));
            awaitLoaded(p, server.url("/page"));
            int before = server.count("/tick");
            Check.isTrue(server.awaitRequests(server.totalRequests() + 40, 30000), "the frames tick again after a new page");
            Check.isTrue(server.count("/tick") > before, "the new page ticks");
            p.dispose();
            int afterDispose = settledCount(server, "/tick");
            Thread.sleep(1000);
            Check.equal(afterDispose, server.count("/tick"), "no frame ticks after dispose()");
        } finally {
            p.dispose();
        }
    }

    /**
     * Removing a frame element ends the scripts of that frame and of the frames inside it, while the rest of the page goes on.
     * <p>
     * 프레임 요소를 제거하면 그 프레임과 그 안의 프레임의 스크립트가 끝나고, 페이지의 나머지는 계속 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRemovingAFrameElementEndsItsScripts() throws Exception {
        TestHttpServer server = new TestHttpServer();
        servers.add(server);
        SwingWebPanel p = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/tick", TestHttpServer.Response.ok("text/plain", "t"));
            // 프레임은 첫 HTTP 응답을 받은 뒤에만 부모에게 제거 가능하다고 알린다.
            server.routeFixed("/gone", TestHttpServer.Response.html("<body><script>var notified=false;setInterval(function(){"
                    + "var x=new XMLHttpRequest();x.open('GET','/gone-tick');"
                    + "x.onload=function(){if(!notified&&x.status===200){notified=true;parent.postMessage('frame-tick','*')}};x.send()},30)"
                    + "</script></body>"));
            server.routeFixed("/gone-tick", TestHttpServer.Response.ok("text/plain", "t"));
            server.routeFixed("/page", TestHttpServer.Response.html("<!DOCTYPE html><html><body><script>"
                    + "window.addEventListener('message',function(e){var frame=document.getElementById('f');"
                    + "if(e.data!=='frame-tick'||!frame)return;document.body.removeChild(frame);sessionStorage.setItem('removing','yes');"
                    + "setInterval(function(){var x=new XMLHttpRequest();x.open('GET','/tick');x.send()},30)})</script>"
                    + "<iframe id=\"f\" src=\"/gone\" width=\"50\" height=\"30\"></iframe></body></html>"));
            server.start();
            p.setPage(server.url("/page"));
            FrameTestSupport.awaitStored(p, "removing", "yes");
            Check.isTrue(server.count("/gone-tick") > 0, "the frame ticked while it was part of the page");
            int stable = settledCount(server, "/gone-tick");
            Thread.sleep(1000);
            Check.equal(stable, server.count("/gone-tick"), "the removed frame no longer ticks");
            long end = System.currentTimeMillis() + 20000;
            // 페이지의 나머지 스크립트는 계속 일한다.
            while (server.count("/tick") < 5 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.isTrue(server.count("/tick") >= 5, "the rest of the page keeps working");
        } finally {
            p.dispose();
        }
    }

    /**
     * A frame document that is still being fetched when the page is replaced is cancelled: its script never runs, the new page is not held
     * up by it, and the connection is closed.
     * <p>
     * 페이지를 교체할 때 아직 가져오는 중인 프레임 문서는 취소된다: 그 스크립트는 실행되지 않고, 새 페이지는 그것 때문에 지연되지 않으며, 연결은 닫힌다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSlowFrameDocumentIsCancelledWhenThePageMovesOn() throws Exception {
        TestHttpServer server = serve();
        SwingWebPanel p = FrameTestSupport.newPanel(400, 300);
        try {
            p.setPage(server.url("/slowpage"));
            Check.isTrue(server.awaitRequests(2, 30000), "the page and its slow frame were requested");
            long start = System.currentTimeMillis();
            p.setPage(server.url("/other"));
            awaitLoaded(p, server.url("/other"));
            Check.isTrue(System.currentTimeMillis() - start < 2500, "the new page was not held up by the slow frame");
            // 느린 응답이 도착할 시간이 지난 뒤에도 그 문서의 스크립트는 실행되지 않았다.
            Thread.sleep(3500);
            Check.equal(null, FrameTestSupport.stored(p, "slow-ran"), "the cancelled frame document never ran");
            long end = System.currentTimeMillis() + 10000;
            // 서버 쪽 연결이 모두 닫힐 때까지 기다린다.
            while (server.getOpenConnections() > 0 && System.currentTimeMillis() < end) {
                Thread.sleep(50);
            }
            Check.equal(0, server.getOpenConnections(), "no connection stays open");
        } finally {
            p.dispose();
        }
    }

    /**
     * Uses one component with the page that has frames, moves on, cleans and disposes it; only a weak reference leaves this method.
     * <p>
     * 컴포넌트 하나로 프레임이 있는 페이지를 쓰고 이동하고 clean 하고 dispose 한다. 이 메서드를 떠나는 것은 약한 참조뿐이다.
     *
     * @param server the server / 서버
     * @return a weak reference to the component / 컴포넌트의 약한 참조
     * @throws Exception on failure / 실패 시
     */
    private static WeakReference<SwingWebPanel> useAndDispose(TestHttpServer server) throws Exception {
        SwingWebPanel p = FrameTestSupport.newPanel(400, 300);
        p.setPage(server.url("/page"));
        awaitLoaded(p, server.url("/page"));
        // 프레임의 타이머와 요청이 몇 번 돌 시간을 주고, 다른 페이지로 이동했다가 되돌아온 뒤 clean 하고 폐기한다.
        Thread.sleep(150);
        p.setPage(server.url("/other"));
        awaitLoaded(p, server.url("/other"));
        p.back();
        awaitLoaded(p, server.url("/page"));
        Thread.sleep(100);
        TestSupport.flushEdt();
        p.clean();
        p.dispose();
        return new WeakReference<SwingWebPanel>(p);
    }

    /**
     * Components whose pages had frames (with timers, requests and nested frames) can be collected after <code>dispose()</code>, and the
     * threads of the library do not accumulate with the number of components or pages.
     * <p>
     * 페이지에 프레임(타이머, 요청, 중첩된 프레임이 있는)이 있던 컴포넌트는 <code>dispose()</code> 뒤에 수거될 수 있고, 라이브러리의 스레드는 컴포넌트나 페이지
     * 수에 따라 쌓이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposedComponentsWithFramesAreCollectableAndThreadsDoNotAccumulate() throws Exception {
        TestHttpServer server = serve();
        List<WeakReference<SwingWebPanel>> refs = new ArrayList<WeakReference<SwingWebPanel>>();
        // 첫 컴포넌트로 공용 스레드가 만들어지게 한 뒤 여럿을 쓰고 폐기한다.
        refs.add(useAndDispose(server));
        for (int i = 0; i < 5; i++) {
            refs.add(useAndDispose(server));
        }
        long end = System.currentTimeMillis() + 60000;
        int left = refs.size();
        // 모두 지워질 때까지 GC 를 요청하며 짧게 쉰다. GC 의 시점은 보장되지 않으므로 시간 제한을 둔다.
        while (left > 0 && System.currentTimeMillis() < end) {
            System.gc();
            System.runFinalization();
            Thread.sleep(100);
            left = 0;
            // 아직 지워지지 않은 참조를 센다.
            for (WeakReference<SwingWebPanel> r : refs) {
                if (r.get() != null) {
                    left++;
                }
            }
        }
        Check.equal(0, left, "all disposed components were collected (a root keeps " + left + " of " + refs.size() + " reachable)");
        end = System.currentTimeMillis() + 30000;
        int threads = libraryThreads();
        // 놀던 스레드가 끝나 남은 스레드가 공용 스레드 수 이하가 될 때까지 짧게 쉬며 기다린다.
        while (threads > 6 && System.currentTimeMillis() < end) {
            Thread.sleep(250);
            threads = libraryThreads();
        }
        Check.isTrue(threads <= 6, "only the shared threads stay after the idle time: " + threads);
    }

    /**
     * Disposing the component while a frame document is still being fetched leaves nothing running: no connection, no document, no script.
     * <p>
     * 프레임 문서를 아직 가져오는 중에 컴포넌트를 폐기하면 아무것도 실행 중으로 남지 않는다: 연결도, 문서도, 스크립트도 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposeDuringAFrameLoadLeavesNothingRunning() throws Exception {
        TestHttpServer server = serve();
        SwingWebPanel p = FrameTestSupport.newPanel(400, 300);
        p.setPage(server.url("/slowpage"));
        Check.isTrue(server.awaitRequests(2, 30000), "the page and its slow frame were requested");
        p.dispose();
        Thread.sleep(3500);
        long end = System.currentTimeMillis() + 10000;
        // 서버 쪽 연결이 모두 닫힐 때까지 기다린다.
        while (server.getOpenConnections() > 0 && System.currentTimeMillis() < end) {
            Thread.sleep(50);
        }
        Check.equal(0, server.getOpenConnections(), "no connection stays open after dispose()");
        Check.equal(null, FrameTestSupport.stored(p, "slow-ran"), "the frame document never ran");
        Check.equal("about:blank", p.getPage(), "the disposed component shows the empty page");
    }
}
