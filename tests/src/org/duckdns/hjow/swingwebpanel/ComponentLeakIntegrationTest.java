package org.duckdns.hjow.swingwebpanel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Whether disposed components leave anything behind, through real components with scripts, timers, stylesheets, images, storage and
 * requests of their own (plan items P21-05, P21-06, P21-07): after <code>dispose()</code> no static or thread root keeps a component
 * reachable (checked with weak references, without any claim about when the garbage collector runs), and the threads of the library do not
 * accumulate with the number of components.
 * <p>
 * 폐기한 컴포넌트가 무언가를 남기는지 스크립트, 타이머, 스타일시트, 이미지, 저장소, 자체 요청이 있는 실제 컴포넌트로 확인하는 시험(계획 P21-05, P21-06,
 * P21-07): <code>dispose()</code> 뒤에는 어떤 정적 루트나 스레드도 컴포넌트를 도달 가능하게 붙잡지 않으며(약한 참조로 확인하고 가비지 컬렉터가
 * 언제 실행되는지는 주장하지 않는다), 라이브러리의 스레드는 컴포넌트 수에 따라 쌓이지 않는다.
 */
public class ComponentLeakIntegrationTest {

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
    public ComponentLeakIntegrationTest() {
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
     * Starts a server with a page that uses everything that a page can leave behind: an interval timer, a timeout, promises, listeners,
     * both storages, a stylesheet with an import, an image, and a request of its own.
     * <p>
     * 페이지가 남길 수 있는 모든 것을 쓰는 페이지가 있는 서버를 시작한다: 반복 타이머, 시간 제한 타이머, 프라미스, 리스너, 두 저장소, import 가
     * 있는 스타일시트, 이미지, 자체 요청.
     *
     * @return the server / 서버
     * @throws Exception if it cannot start / 시작할 수 없으면
     */
    private TestHttpServer serve() throws Exception {
        TestHttpServer server = new TestHttpServer();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(5, 3, BufferedImage.TYPE_INT_RGB), "png", png);
        server.routeFixed("/rich", TestHttpServer.Response.html("<!doctype html><html><head><link rel=stylesheet href='a.css'></head><body>"
                + "<p id=t>text</p><img src='i.png'><script>"
                + "var n = 0; var big = new Array(2000).join('x');"
                + "setInterval(function () { n++; document.getElementById('t').textContent = n + big.length; }, 20);"
                + "setTimeout(function () { sessionStorage.setItem('late', 'yes'); }, 50);"
                + "Promise.resolve(1).then(function (v) { localStorage.setItem('leak-test', String(v)); });"
                + "document.addEventListener('click', function () { n = 0; });"
                + "window.addEventListener('storage', function () { n = 1; });"
                + "var x = new XMLHttpRequest(); x.open('GET', 'data.txt'); x.onload = function () { sessionStorage.setItem('xhr', x.responseText); }; x.send();"
                + "</script></body></html>"));
        server.routeFixed("/a.css", TestHttpServer.Response.ok("text/css", "@import url('b.css'); p { color: green }"));
        server.routeFixed("/b.css", TestHttpServer.Response.ok("text/css", "p { margin: 3px }"));
        server.routeFixed("/i.png", new TestHttpServer.Response(200).bytes("image/png", png.toByteArray()));
        server.routeFixed("/data.txt", TestHttpServer.Response.ok("text/plain", "payload"));
        server.routeFixed("/plain", TestHttpServer.Response.html("<p>plain</p>"));
        server.start();
        servers.add(server);
        return server;
    }

    /**
     * Uses one component to the full and disposes it; only a weak reference leaves this method.
     * <p>
     * 컴포넌트 하나를 충분히 쓰고 폐기한다. 이 메서드를 떠나는 것은 약한 참조뿐이다.
     *
     * @param server the server / 서버
     * @param disposeIt true to call dispose, false to call only clean / dispose 를 부르면 true, clean 만 부르면 false
     * @return a weak reference to the component / 컴포넌트의 약한 참조
     * @throws Exception on failure / 실패 시
     */
    private static WeakReference<SwingWebPanel> useAndDispose(TestHttpServer server, boolean disposeIt) throws Exception {
        SwingWebPanel p = new SwingWebPanel();
        p.setPage(server.url("/rich"));
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!server.url("/rich").equals(p.getPage()) || p.getProgress() != 100 || !"Done".equals(p.getStatusText()))
                && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("Done", p.getStatusText(), "the rich page finished loading");
        // 타이머와 요청이 몇 번 돌 시간을 주고, 같은 컴포넌트로 다른 페이지로 이동한 뒤 이력을 한 칸 되돌아간다.
        Thread.sleep(150);
        p.setPage(server.url("/plain"));
        end = System.currentTimeMillis() + 30000;
        // 두 번째 페이지가 확정될 때까지 짧게 쉬며 기다린다.
        while (!server.url("/plain").equals(p.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        p.back();
        Thread.sleep(100);
        TestSupport.flushEdt();
        p.clean();
        if (disposeIt) {
            p.dispose();
        }
        return new WeakReference<SwingWebPanel>(p);
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
     * Asks for garbage collection until all references are cleared or the time is over.
     * <p>
     * 모든 참조가 지워지거나 시간이 다할 때까지 가비지 컬렉션을 요청한다.
     *
     * @param refs       the weak references / 약한 참조
     * @param timeoutMs  how long to try / 시도할 시간
     * @return the number of references that are still not cleared / 아직 지워지지 않은 참조의 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static int collect(List<WeakReference<SwingWebPanel>> refs, long timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
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
        return left;
    }

    /**
     * Several components that were used and disposed can all be collected, and the threads of the library do not accumulate.
     * <p>
     * 쓰고 폐기한 컴포넌트 여럿이 모두 수거될 수 있고, 라이브러리의 스레드는 쌓이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposedComponentsAreCollectableAndThreadsDoNotAccumulate() throws Exception {
        TestHttpServer server = serve();
        // 첫 컴포넌트로 공용 스레드(시계, 정리 스레드)가 만들어지게 한 뒤 기준 개수를 잰다.
        List<WeakReference<SwingWebPanel>> refs = new ArrayList<WeakReference<SwingWebPanel>>();
        refs.add(useAndDispose(server, true));
        int count = 8;
        // 컴포넌트를 여럿 쓰고 폐기한다.
        for (int i = 0; i < count; i++) {
            refs.add(useAndDispose(server, true));
        }
        int left = collect(refs, 40000);
        Check.equal(0, left, "all disposed components were collected (a root keeps " + left + " of " + refs.size() + " reachable)");
        // 놀고 있는 읽기/문서/리소스 스레드는 몇 초 뒤 끝난다. 끝난 뒤 남은 라이브러리 스레드는 컴포넌트 수와 무관해야 한다.
        long end = System.currentTimeMillis() + 30000;
        int threads = libraryThreads();
        // 놀던 스레드가 끝나 남은 스레드가 공용 스레드 수 이하가 될 때까지 짧게 쉬며 기다린다.
        while (threads > 6 && System.currentTimeMillis() < end) {
            Thread.sleep(250);
            threads = libraryThreads();
        }
        Check.isTrue(threads <= 6, "only the shared threads stay after the idle time: " + threads);
    }

    /**
     * A component that was cleaned but not disposed is collectable too once nobody references it.
     * <p>
     * clean 만 하고 폐기하지 않은 컴포넌트도 아무도 참조하지 않으면 수거될 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanedComponentIsCollectableWithoutDispose() throws Exception {
        TestHttpServer server = serve();
        List<WeakReference<SwingWebPanel>> refs = new ArrayList<WeakReference<SwingWebPanel>>();
        refs.add(useAndDispose(server, false));
        int left = collect(refs, 40000);
        Check.equal(0, left, "the cleaned component was collected without dispose()");
    }
}
