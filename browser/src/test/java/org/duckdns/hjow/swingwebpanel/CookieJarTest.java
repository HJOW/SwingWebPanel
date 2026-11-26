package org.duckdns.hjow.swingwebpanel;

import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The cookie jar that components share and the removal of cookies (plan item P31-04, API contract 5.24, cases C-COOKIEJAR-1 to 4): real
 * components and a real HTTP server that sets a cookie and reports the cookies it receives.
 * <p>
 * 컴포넌트가 공유하는 쿠키 저장소와 쿠키 삭제(계획 P31-04, API 계약 5.24, 사례 C-COOKIEJAR-1~4): 실제 컴포넌트와, 쿠키를 설정하고 받은 쿠키를
 * 알려 주는 실제 HTTP 서버를 쓴다.
 */
public class CookieJarTest {

    /** The test server. / 시험 서버. */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CookieJarTest() {
    }

    /**
     * Starts a server whose <code>/set</code> sets a cookie and whose <code>/echo</code> shows the received cookie header as the title.
     * <p>
     * <code>/set</code> 은 쿠키를 설정하고 <code>/echo</code> 는 받은 쿠키 헤더를 제목으로 보여 주는 서버를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.route("/set", new TestHttpServer.Handler() {
            /** Sets the cookie named by the query. / 질의가 정한 이름의 쿠키를 설정한다. */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.html("<!doctype html><title>set</title>").header("Set-Cookie", request.query + "=1; Path=/");
            }
        });
        server.route("/echo", new TestHttpServer.Handler() {
            /** Reports the cookie header of the request. / 요청의 쿠키 헤더를 알려 준다. */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                String cookie = request.header("Cookie");
                return TestHttpServer.Response.html("<!doctype html><title>got:" + (cookie == null ? "" : cookie) + "</title>"
                        + "<script>document.title += '|js:' + document.cookie;</" + "script>");
            }
        });
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
     * Creates a component on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 컴포넌트를 만든다.
     *
     * @param jar the shared jar, or null for a component with its own store / 공유 저장소. 자기 저장소를 가진 컴포넌트이면 null
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private static SwingWebPanel create(final CookieJar jar) throws Exception {
        final SwingWebPanel[] made = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                made[0] = jar == null ? new SwingWebPanel() : new SwingWebPanel(jar);
                made[0].setSize(300, 200);
            }
        });
        return made[0];
    }

    /**
     * Loads a path and returns the title of the page once it is complete.
     * <p>
     * 경로를 읽고, 다 읽은 페이지의 제목을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param path  the path with its query / 질의가 붙은 경로
     * @return the title / 제목
     * @throws Exception if interrupted / 인터럽트되면
     */
    private String load(SwingWebPanel panel, String path) throws Exception {
        String url = server.url(path);
        panel.setPage(url);
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "confirmed page " + path);
        // 되울림 페이지는 스크립트가 제목 뒤에 document.cookie 를 붙인다. 그 제목이 컴포넌트에 반영될 때까지 기다린다.
        while (path.startsWith("/echo") && panel.getTitle().indexOf("|js:") < 0 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return panel.getTitle();
    }

    /**
     * C-COOKIEJAR-1: components created with the same jar share their cookies (requests and <code>document.cookie</code>); a component
     * without a jar and a component with another jar see none of them.
     * <p>
     * C-COOKIEJAR-1: 같은 저장소로 만든 컴포넌트는 쿠키를 공유한다(요청과 <code>document.cookie</code>). 저장소 없이 만든 컴포넌트와 다른 저장소로
     * 만든 컴포넌트에는 보이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_COOKIEJAR_1_SharedBetweenComponents() throws Exception {
        CookieJar jar = new CookieJar();
        SwingWebPanel a = create(jar);
        SwingWebPanel b = create(jar);
        SwingWebPanel alone = create(null);
        SwingWebPanel other = create(new CookieJar());
        try {
            Check.same(jar, a.getCookieJar(), "the jar of the component");
            Check.isNull(alone.getCookieJar(), "a component with its own store has no jar");
            Check.equal("set", load(a, "/set?login"), "the first component stores the cookie");
            Check.equal("login=1", jar.getCookies(), "the jar has the cookie");
            Check.equal("login=1", a.getCookies(), "the first component reports it");
            Check.equal("login=1", b.getCookies(), "the second component reports it too");
            Check.equal("got:login=1|js:login=1", load(b, "/echo"), "the second component sends the cookie and its scripts see it");
            Check.equal("got:|js:", load(alone, "/echo"), "a component with its own store does not have it");
            Check.equal("got:|js:", load(other, "/echo"), "a component with another jar does not have it");
            Check.equal("", alone.getCookies(), "the separate store is empty");
        } finally {
            a.dispose();
            b.dispose();
            alone.dispose();
            other.dispose();
        }
    }

    /**
     * C-COOKIEJAR-2: disposing a component does not clear the shared jar, the disposed component reports no cookies, and a component
     * that is created with the jar afterwards has them. A component with its own store still loses its cookies at dispose.
     * <p>
     * C-COOKIEJAR-2: 컴포넌트를 폐기해도 공유 저장소는 비워지지 않고, 폐기된 컴포넌트는 쿠키가 없다고 알려 주며, 그 뒤에 같은 저장소로 만든
     * 컴포넌트는 쿠키를 가진다. 자기 저장소를 가진 컴포넌트는 지금처럼 폐기할 때 쿠키를 잃는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_COOKIEJAR_2_DisposeKeepsTheJar() throws Exception {
        CookieJar jar = new CookieJar();
        SwingWebPanel a = create(jar);
        SwingWebPanel b = create(jar);
        SwingWebPanel c = null;
        try {
            load(a, "/set?kept");
            a.dispose();
            Check.equal("", a.getCookies(), "a disposed component reports the empty state");
            Check.equal("kept=1", jar.getCookies(), "the jar keeps the cookie");
            Check.equal("got:kept=1|js:kept=1", load(b, "/echo"), "the other component still sends it");
            b.dispose();
            c = create(jar);
            Check.equal("got:kept=1|js:kept=1", load(c, "/echo"), "a component created later has it");
        } finally {
            a.dispose();
            b.dispose();
            if (c != null) {
                c.dispose();
            }
        }
    }

    /**
     * C-COOKIEJAR-3: <code>clearCookies()</code> removes the cookies while <code>clean()</code> keeps them; on a shared jar both
     * <code>CookieJar.clear()</code> and <code>clearCookies()</code> of one component remove them for all components.
     * <p>
     * C-COOKIEJAR-3: <code>clean()</code> 은 쿠키를 유지하고 <code>clearCookies()</code> 는 지운다. 공유 저장소에서는
     * <code>CookieJar.clear()</code> 와 한 컴포넌트의 <code>clearCookies()</code> 모두 모든 컴포넌트의 쿠키를 지운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_COOKIEJAR_3_ClearingCookies() throws Exception {
        CookieJar jar = new CookieJar();
        SwingWebPanel a = create(jar);
        SwingWebPanel b = create(jar);
        SwingWebPanel alone = create(null);
        try {
            load(alone, "/set?own");
            alone.clean();
            Check.equal("own=1", alone.getCookies(), "clean() keeps the cookies");
            alone.clearCookies();
            Check.equal("", alone.getCookies(), "clearCookies() removes them");
            Check.equal("got:|js:", load(alone, "/echo"), "the next request has no cookie");
            load(a, "/set?one");
            Check.equal("one=1", b.getCookies(), "shared");
            b.clearCookies();
            Check.equal("", jar.getCookies(), "clearCookies() of one component empties the shared jar");
            Check.equal("got:|js:", load(a, "/echo"), "the other component has no cookie either");
            load(a, "/set?two");
            jar.clear();
            Check.equal("", a.getCookies(), "CookieJar.clear() removes the cookies of all components");
            Check.equal("got:|js:", load(b, "/echo"), "no cookie after clearing the jar");
        } finally {
            a.dispose();
            b.dispose();
            alone.dispose();
        }
    }

    /**
     * C-COOKIEJAR-4: a missing jar is rejected, and <code>clearCookies()</code> on a disposed component throws.
     * <p>
     * C-COOKIEJAR-4: 없는 저장소는 거부하고, 폐기된 컴포넌트의 <code>clearCookies()</code> 는 예외를 던진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_COOKIEJAR_4_Errors() throws Exception {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Creates a component with a missing jar. / 없는 저장소로 컴포넌트를 만들어 본다. */
            public void run() throws Exception {
                new SwingWebPanel((CookieJar) null);
            }
        }, "a null jar is rejected");
        final SwingWebPanel p = create(new CookieJar());
        p.dispose();
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Clears the cookies of a disposed component. / 폐기된 컴포넌트의 쿠키를 지워 본다. */
            public void run() throws Exception {
                p.clearCookies();
            }
        }, "clearCookies() after dispose() throws");
    }
}
