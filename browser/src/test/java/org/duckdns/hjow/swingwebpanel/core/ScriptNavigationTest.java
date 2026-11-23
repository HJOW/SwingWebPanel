package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Navigations that the page starts itself, through the real component and real HTTP (plan items P15-08-c and P19-02): scripts
 * (<code>location.assign</code>, <code>replace</code>, <code>href</code>, <code>reload</code>, <code>history</code>), the policy that
 * ignores blocked targets, and navigations that only change the fragment (no new request, history entries, <code>hashchange</code>,
 * scrolling to the anchor for a clicked link).
 * <p>
 * 페이지가 스스로 시작하는 이동을 실제 컴포넌트와 실제 HTTP 로 확인한다(계획 P15-08-c, P19-02): 스크립트(<code>location.assign</code>,
 * <code>replace</code>, <code>href</code>, <code>reload</code>, <code>history</code>), 차단된 대상을 무시하는 정책, 프래그먼트만 바꾸는 이동(새 요청
 * 없음, 이력 항목, <code>hashchange</code>, 클릭한 링크의 앵커로 스크롤).
 */
public class ScriptNavigationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScriptNavigationTest() {
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
     * Reads a value that a script stored in the sessionStorage.
     * <p>
     * 스크립트가 sessionStorage 에 저장한 값을 읽는다.
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
     * Waits until the component shows an address.
     * <p>
     * 컴포넌트가 주소를 보여 줄 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param url   the address / 주소
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 주소가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Returns the page of a script that starts an action once the page is confirmed (timers wait for confirmation).
     * <p>
     * 페이지가 확정된 뒤 동작을 시작하는 스크립트를 가진 페이지를 반환한다(타이머는 확정을 기다린다).
     *
     * @param action the script statements / 스크립트 문장
     * @return the HTML / HTML
     */
    private static String pageWith(String action) {
        return "<!doctype html><script>setTimeout(function () { " + action + " }, 0);</script><p>page</p>";
    }

    /**
     * P15-08-c: <code>location.assign</code>, <code>href</code> and <code>window.location</code> add a history entry, while
     * <code>location.replace</code> replaces the current one; the new page is requested once.
     * <p>
     * P15-08-c: <code>location.assign</code>, <code>href</code>, <code>window.location</code> 은 이력 항목을 더하고 <code>location.replace</code> 는
     * 현재 항목을 바꾼다. 새 페이지는 한 번 요청한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptNavigationAddsOrReplacesHistoryEntries() throws Exception {
        String[] adding = { "location.assign('/b');", "location.href = '/b';", "window.location = '/b';" };
        // 이력 항목을 더하는 세 가지 방법을 각각 새 컴포넌트에서 확인한다.
        for (int i = 0; i < adding.length; i++) {
            TestHttpServer server = new TestHttpServer();
            SwingWebPanel panel = newPanel();
            try {
                server.routeFixed("/a", TestHttpServer.Response.html(pageWith(adding[i])));
                server.routeFixed("/b", TestHttpServer.Response.html("<!doctype html><p>b</p>"));
                server.start();
                panel.setPage(server.url("/a"));
                awaitPage(panel, server.url("/b"));
                List<String> history = panel.getHistories();
                Check.equal("[about:blank, " + server.url("/a") + ", " + server.url("/b") + "]", history.toString(), "entry added by " + adding[i]);
                Check.equal(1, server.count("/b"), "requested once");
            } finally {
                panel.dispose();
                server.stop();
            }
        }
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/a", TestHttpServer.Response.html(pageWith("location.replace('/b');")));
            server.routeFixed("/b", TestHttpServer.Response.html("<!doctype html><p>b</p>"));
            server.start();
            panel.setPage(server.url("/a"));
            awaitPage(panel, server.url("/b"));
            Check.equal("[about:blank, " + server.url("/b") + "]", panel.getHistories().toString(), "replace keeps the entry count");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-08-c: <code>location.reload()</code> requests the page again without a new entry, and <code>history.back()</code> returns to
     * the previous page.
     * <p>
     * P15-08-c: <code>location.reload()</code> 는 새 항목 없이 페이지를 다시 요청하고 <code>history.back()</code> 은 이전 페이지로 돌아간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReloadAndHistoryBackFromScripts() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/a", TestHttpServer.Response.html(pageWith(
                    "if (!sessionStorage.getItem('n')) { sessionStorage.setItem('n', '1'); location.reload(); } else { location.assign('/b'); }")));
            server.routeFixed("/b", TestHttpServer.Response.html(pageWith("history.back();").replace("page", "b-page")));
            server.start();
            panel.setPage(server.url("/a"));
            // a 를 다시 읽은 뒤 b 로 가고, b 가 뒤로 가기를 하면 a 로 돌아온다. a 는 두 번 이상 요청된다.
            long end = System.currentTimeMillis() + 60000;
            while ((server.count("/b") < 1 || !panel.getHistories().toString().endsWith(server.url("/b") + "]")) && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.isTrue(server.count("/a") >= 2, "reload requested the page again: " + server.count("/a"));
            Check.equal("[about:blank, " + server.url("/a") + ", " + server.url("/b") + "]", panel.getHistories().toString().replaceAll("\\s+", " "),
                    "reload added no entry");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-08-c: targets outside the allowed schemes, and the class path from a network page, are ignored: nothing is requested and the page
     * stays.
     * <p>
     * P15-08-c: 허용된 스킴 밖의 대상과 네트워크 페이지에서의 클래스패스 이동은 무시된다. 아무것도 요청되지 않고 페이지는 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedTargetsAreIgnored() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String action = "var targets = ['javascript:void(0)', 'file:///etc/passwd', 'mailto:someone@example.test', 'data:text/html,hi', "
                    + "'classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html', 'jar:file:/x.jar!/a', 'blob:http://x/y'];"
                    + "for (var i = 0; i < targets.length; i++) { try { location.href = targets[i]; } catch (e) { } }"
                    + "sessionStorage.setItem('done', 'yes');";
            server.routeFixed("/a", TestHttpServer.Response.html(pageWith(action)));
            server.start();
            panel.setPage(server.url("/a"));
            awaitValue(panel, "done", "yes");
            Thread.sleep(300);
            Check.equal(server.url("/a"), panel.getPage(), "the page stays");
            Check.equal("[about:blank, " + server.url("/a") + "]", panel.getHistories().toString(), "no entry was added");
            Check.equal(1, server.totalRequests(), "nothing else was requested");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P19-02: a script that changes <code>location.hash</code> changes the URL and the history without requesting the page again; the
     * window gets <code>hashchange</code>, and <code>history.back()</code> and <code>forward()</code> move between the entries of the
     * same document, again without a request.
     * <p>
     * P19-02: <code>location.hash</code> 를 바꾸는 스크립트는 페이지를 다시 요청하지 않고 URL과 이력을 바꾼다. window 는 <code>hashchange</code> 를 받고
     * <code>history.back()</code>, <code>forward()</code> 는 요청 없이 같은 문서의 항목 사이를 이동한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHashChangeAndHistoryInsideTheSameDocument() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String script = "function f(u) { var i = u.indexOf('#'); return i < 0 ? '' : u.substring(i + 1); }"
                    + "function log(s) { sessionStorage.setItem('hc', (sessionStorage.getItem('hc') || '') + s + ';'); }"
                    + "window.addEventListener('hashchange', function (e) { log(f(e.oldURL) + '>' + f(e.newURL)); });"
                    + "setTimeout(function () { location.hash = 'two'; sessionStorage.setItem('href', location.href); }, 0);"
                    + "setTimeout(function () { history.back(); }, 400);"
                    + "setTimeout(function () { history.forward(); }, 900);";
            server.routeFixed("/a", TestHttpServer.Response.html("<!doctype html><script>" + script + "</script><h1 id=two>two</h1>"));
            server.start();
            panel.setPage(server.url("/a"));
            awaitValue(panel, "hc", ">two;two>;>two;");
            awaitPage(panel, server.url("/a") + "#two");
            Check.equal(server.url("/a") + "#two", value(panel, "href"), "location.href reports the new URL at once");
            Check.equal(1, server.count("/a"), "the page was requested once for the whole sequence");
            Check.equal("[about:blank, " + server.url("/a") + ", " + server.url("/a") + "#two]", panel.getHistories().toString(),
                    "history of the document");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The <code>:target</code> pseudo-class follows the fragment of the document URL: after a fragment navigation the element with that id
     * matches and the one before does not, also when the component goes back in the history (CSS Selectors 3, HTML5 2014 7.8.9).
     * <p>
     * <code>:target</code> 의사 클래스는 문서 URL 의 프래그먼트를 따른다: 프래그먼트 이동 뒤에는 그 id 의 요소가 맞고 앞의 요소는 맞지 않으며, 컴포넌트가
     * 이력에서 뒤로 가도 그렇다(CSS Selectors 3, HTML5 2014 7.8.9).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTargetPseudoClassFollowsTheFragment() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String script = "function c(id) { return getComputedStyle(document.getElementById(id)).color; }"
                    + "function report() { sessionStorage.setItem('t', location.hash + ' a=' + c('a') + ' b=' + c('b')"
                    + " + ' m=' + document.querySelectorAll(':target').length); }"
                    + "window.addEventListener('hashchange', report);"
                    + "setTimeout(function () { report(); location.hash = 'b'; }, 0);";
            server.routeFixed("/t", TestHttpServer.Response.html("<!doctype html><style>p{color:rgb(0,0,0)} :target{color:rgb(255,0,0)}</style>"
                    + "<p id=a>A</p><p id=b>B</p><script>" + script + "</script>"));
            server.start();
            panel.setPage(server.url("/t") + "#a");
            awaitValue(panel, "t", "#b a=rgb(0, 0, 0) b=rgb(255, 0, 0) m=1");
            panel.back();
            awaitValue(panel, "t", "#a a=rgb(255, 0, 0) b=rgb(0, 0, 0) m=1");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P19-02: clicking a link to an anchor of the same page scrolls the viewport to the anchor, adds an entry, and requests nothing; going
     * back through the component scrolls without a request as well.
     * <p>
     * P19-02: 같은 페이지의 앵커로 가는 링크를 클릭하면 뷰포트가 앵커로 스크롤되고 항목이 추가되며 아무것도 요청하지 않는다. 컴포넌트에서 뒤로 가도
     * 요청 없이 이동한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClickingAnAnchorScrollsWithoutRequest() throws Exception {
        TestHttpServer server = new TestHttpServer();
        final SwingWebPanel panel = newPanel();
        final JScrollPane[] pane = new JScrollPane[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Puts the component into a scroll pane with a small viewport.
                 * <p>
                 * 컴포넌트를 작은 뷰포트의 스크롤 창에 넣는다.
                 */
                public void run() {
                    pane[0] = new JScrollPane(panel);
                    pane[0].setSize(300, 100);
                    pane[0].doLayout();
                }
            });
            server.routeFixed("/a", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>"
                    + "<a id=l href='#end' style='display:block;height:40px;width:200px'>go to the end</a>"
                    + "<div style='height:1200px'>space</div><h2 id=end style='margin:0;height:50px'>End</h2><div style='height:600px'>after</div>"));
            server.start();
            panel.setPage(server.url("/a"));
            awaitPage(panel, server.url("/a"));
            long end = System.currentTimeMillis() + 30000;
            // 문서가 배치되어 스크롤할 수 있는 높이가 될 때까지 기다린다.
            while (panel.getPreferredSize().height < 1500 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Clicks the link through the component's own event dispatch.
                 * <p>
                 * 컴포넌트 자신의 이벤트 전달로 링크를 클릭한다.
                 */
                public void run() {
                    panel.setSize(300, panel.getPreferredSize().height);
                    panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 20, 20, 1, false,
                            MouseEvent.BUTTON1));
                }
            });
            awaitPage(panel, server.url("/a") + "#end");
            end = System.currentTimeMillis() + 30000;
            // 뷰포트가 앵커 쪽으로 움직일 때까지 기다린다.
            while (pane[0].getViewport().getViewPosition().y < 1000 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            int y = pane[0].getViewport().getViewPosition().y;
            Check.isTrue(y >= 1200 && y <= 1260, "the viewport scrolled to the anchor: y=" + y);
            Check.equal(1, server.count("/a"), "no new request for the anchor");
            Check.equal("[about:blank, " + server.url("/a") + ", " + server.url("/a") + "#end]", panel.getHistories().toString(), "entry added");
            panel.back();
            awaitPage(panel, server.url("/a"));
            Check.equal(1, server.count("/a"), "going back did not request the page either");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
