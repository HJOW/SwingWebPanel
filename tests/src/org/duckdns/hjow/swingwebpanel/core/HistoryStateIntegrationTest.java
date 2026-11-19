package org.duckdns.hjow.swingwebpanel.core;

import java.lang.reflect.Field;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FormHost;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * <code>history.pushState</code>, <code>replaceState</code> and <code>popstate</code> through the real component and real HTTP (plan item
 * P20-07): what a single page application router does. Entries added by script have their own URL and state and never request the page; going
 * back and forward between them fires <code>popstate</code> with the state of the entry (and <code>hashchange</code> when the fragment differs)
 * without a request; a reload requests the address of the current entry and keeps its state; going to an entry of another document loads it
 * from the server with the state that the entry stored.
 * <p>
 * <code>history.pushState</code>, <code>replaceState</code>, <code>popstate</code> 를 실제 컴포넌트와 실제 HTTP 로 확인한다(계획 P20-07). 단일 페이지
 * 애플리케이션의 라우터가 하는 일이다. 스크립트가 추가한 항목은 자기 URL과 상태를 갖고 페이지를 요청하지 않는다. 항목 사이를 뒤/앞으로 이동하면
 * 요청 없이 항목의 상태와 함께 <code>popstate</code>(프래그먼트가 다르면 <code>hashchange</code> 도)가 발생한다. 새로고침은 현재 항목의 주소를
 * 요청하고 상태를 유지하며, 다른 문서의 항목으로 가면 항목이 저장한 상태와 함께 서버에서 읽는다.
 */
public class HistoryStateIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HistoryStateIntegrationTest() {
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
        long end = System.currentTimeMillis() + 25000;
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
        long end = System.currentTimeMillis() + 25000;
        // 주소가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Installs a form host that hands the submissions to the session (the component does not connect form submissions by itself yet) and
     * submits a form of the page on the document queue, as the activation of a submit button does.
     * <p>
     * 제출을 세션에 넘기는 폼 호스트를 설치하고(컴포넌트는 아직 폼 제출을 스스로 연결하지 않는다) 제출 단추의 활성화처럼 페이지의 폼을 문서 큐에서
     * 제출한다.
     *
     * @param panel the component / 컴포넌트
     * @param id    the id of the form / 폼의 id
     * @throws Exception on failure / 실패 시
     */
    private static void submitForm(SwingWebPanel panel, String id) throws Exception {
        Field f = SwingWebPanel.class.getDeclaredField("session");
        f.setAccessible(true);
        final BrowserSession session = (BrowserSession) f.get(panel);
        final HtmlPageDocument holder = (HtmlPageDocument) session.getSnapshot().getDocument();
        final String formId = id;
        holder.getDocument().setFormHost(new FormHost() {
            /**
             * Hands the submission to the session.
             * <p>
             * 제출을 세션에 넘긴다.
             *
             * @param submission the submission / 제출 정보
             * @param form       the form / 폼
             */
            public void submit(FormSubmission submission, Element form) {
                session.submitForm(submission);
            }

            /**
             * Ignored.
             * <p>
             * 무시한다.
             *
             * @param control the control / 컨트롤
             * @param message the message / 메시지
             */
            public void reportInvalid(Element control, String message) {
            }

            /**
             * Offers no file chooser.
             * <p>
             * 파일 선택기를 제공하지 않는다.
             *
             * @param input    the input / 입력
             * @param multiple several files / 여러 파일
             * @param accept   the accept attribute / accept 속성
             * @return null / null
             */
            public FileEntry[] chooseFiles(Element input, boolean multiple, String accept) {
                return null;
            }

            /**
             * Ignored.
             * <p>
             * 무시한다.
             *
             * @param element the element / 요소
             */
            public void stateChanged(Element element) {
            }
        });
        Check.isTrue(session.submitDocumentTask(new Runnable() {
            /**
             * Submits the form.
             * <p>
             * 폼을 제출한다.
             */
            public void run() {
                ((HtmlFormElement) holder.getDocument().getElementById(formId)).submit();
            }
        }), "the document queue accepted the task");
    }

    /**
     * Waits until the component finished loading (the page is confirmed and its load ended).
     * <p>
     * 컴포넌트가 로딩을 마칠 때까지(페이지가 확정되고 로딩이 끝날 때까지) 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitSettled(SwingWebPanel panel) throws Exception {
        long end = System.currentTimeMillis() + 25000;
        // 상태가 Done, 진행률이 100 이 될 때까지 짧게 쉬며 확인한다.
        while ((panel.getProgress() != 100 || !"Done".equals(panel.getStatusText())) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal("Done", panel.getStatusText(), "the load ended");
    }

    /**
     * Waits until a path of the server has been requested a number of times.
     * <p>
     * 서버의 경로가 정해진 횟수만큼 요청될 때까지 기다린다.
     *
     * @param server the server / 서버
     * @param path   the path / 경로
     * @param count  the number / 횟수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitCount(TestHttpServer server, String path, int count) throws Exception {
        long end = System.currentTimeMillis() + 25000;
        // 요청이 도착할 때까지 짧게 쉬며 확인한다.
        while (server.count(path) < count && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(count, server.count(path), "requests of " + path);
    }

    /**
     * The page of a small router: it logs what it sees to the sessionStorage and, on its first visit of <code>/app/home</code>, pushes two
     * entries, replaces the second one and then walks back and forward through the entries, one step for every <code>popstate</code>.
     * <p>
     * 작은 라우터의 페이지: 본 것을 sessionStorage 에 기록하고, <code>/app/home</code> 을 처음 방문하면 항목 둘을 푸시하고 둘째를 바꾼 뒤
     * <code>popstate</code> 가 올 때마다 한 걸음씩 항목 사이를 뒤로, 앞으로 이동한다.
     *
     * @return the HTML / HTML
     */
    private static String routerPage() {
        return "<!doctype html><title>spa</title><script>"
                + "function log(s) { sessionStorage.setItem('log', (sessionStorage.getItem('log') || '') + s + ';'); }"
                + "var steps = ['back', 'forward', 'go'];"
                + "var moves = { back: function () { history.back(); }, forward: function () { history.forward(); }, go: function () { history.go(1); } };"
                + "window.addEventListener('popstate', function (e) {"
                + "  log('pop:' + JSON.stringify(e.state) + '@' + location.pathname + location.search);"
                + "  var s = steps.shift(); if (s) { setTimeout(moves[s], 20); } });"
                + "log('load:' + location.pathname + location.search + ':' + JSON.stringify(history.state) + ':' + history.length);"
                + "if (location.pathname == '/app/home' && !sessionStorage.getItem('started')) {"
                + "  sessionStorage.setItem('started', '1');"
                + "  setTimeout(function () {"
                + "    history.pushState({ n: 1 }, 'one', '/app/one');"
                + "    history.pushState({ n: 2 }, 'two', '/app/two');"
                + "    history.replaceState({ n: 22 }, 'two again', '/app/two?r=1');"
                + "    log('url:' + location.pathname + location.search + ':' + JSON.stringify(history.state) + ':' + history.length);"
                + "    history.back();"
                + "  }, 0);"
                + "}"
                + "</script><p>router</p>";
    }

    /**
     * An application router: push, replace, traversal between the entries of one document without a request, a reload at an address that was
     * pushed (the state stays), and going back across documents (the entry brings its state).
     * <p>
     * 응용 라우터: push, replace, 요청 없는 한 문서의 항목 사이 이동, 푸시한 주소에서의 새로고침(상태 유지), 문서를 넘어서는 뒤로 가기(항목이 상태를
     * 가져온다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSinglePageApplicationRouter() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String html = routerPage();
            server.routeFixed("/app/home", TestHttpServer.Response.html(html));
            server.routeFixed("/app/one", TestHttpServer.Response.html(html));
            server.routeFixed("/app/two", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/app/home"));
            String first = "load:/app/home:null:2;url:/app/two?r=1:{\"n\":22}:4;pop:{\"n\":1}@/app/one;pop:null@/app/home;pop:{\"n\":1}@/app/one;"
                    + "pop:{\"n\":22}@/app/two?r=1;";
            awaitValue(panel, "log", first);
            awaitPage(panel, server.url("/app/two?r=1"));
            Check.equal("[about:blank, " + server.url("/app/home") + ", " + server.url("/app/one") + ", " + server.url("/app/two?r=1") + "]",
                    panel.getHistories().toString(), "entries added by script");
            Check.equal(1, server.count("/app/home"), "the first document was requested once");
            Check.equal(0, server.count("/app/one") + server.count("/app/two"), "pushed addresses were never requested");
            // 새로고침은 현재 항목(푸시한 주소)을 요청하고 그 항목의 상태를 유지한다. 새 문서는 항목 수를 그대로 본다.
            panel.refresh();
            awaitCount(server, "/app/two", 1);
            awaitValue(panel, "log", first + "load:/app/two?r=1:{\"n\":22}:4;");
            Check.equal(server.url("/app/two?r=1"), panel.getPage(), "the page after the reload");
            awaitSettled(panel);
            // 다른 문서가 된 뒤의 뒤로 가기는 항목의 주소를 서버에서 읽고 그 항목의 상태를 history.state 로 준다.
            panel.back();
            awaitCount(server, "/app/one", 1);
            awaitPage(panel, server.url("/app/one"));
            awaitValue(panel, "log", first + "load:/app/two?r=1:{\"n\":22}:4;load:/app/one:{\"n\":1}:4;");
            awaitSettled(panel);
            // 이력 이동은 캐시 정책을 따르므로 /app/home 은 캐시에서 올 수 있다(요청 횟수는 확인하지 않는다). 문서가 새로 만들어졌음은 load 기록으로 안다.
            panel.back();
            awaitPage(panel, server.url("/app/home"));
            awaitValue(panel, "log", first + "load:/app/two?r=1:{\"n\":22}:4;load:/app/one:{\"n\":1}:4;load:/app/home:null:4;");
            Check.equal("[about:blank, " + server.url("/app/home") + ", " + server.url("/app/one") + ", " + server.url("/app/two?r=1") + "]",
                    panel.getHistories().toString(), "the history survived the reload and the moves");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Navigating to a fragment fires <code>popstate</code> (state null) and then <code>hashchange</code>; going back to the pushed entry
     * restores its state and fires both again, in this order.
     * <p>
     * 프래그먼트로 이동하면 <code>popstate</code>(상태 null)가 발생한 뒤 <code>hashchange</code> 가 발생한다. 푸시한 항목으로 돌아가면 그 항목의 상태를
     * 복원하고 둘이 같은 순서로 다시 발생한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFragmentEntriesAndPushedEntriesTogether() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String script = "function log(s) { sessionStorage.setItem('ev', (sessionStorage.getItem('ev') || '') + s + ';'); }"
                    + "function frag(u) { var i = u.indexOf('#'); return i < 0 ? '-' : u.substring(i); }"
                    + "window.addEventListener('popstate', function (e) { log('pop:' + JSON.stringify(e.state) + ':' + JSON.stringify(history.state)); });"
                    + "window.addEventListener('hashchange', function (e) { log('hash:' + frag(e.oldURL) + '>' + frag(e.newURL)); });"
                    + "setTimeout(function () { history.pushState({ s: 1 }, '', '/f/pushed'); location.hash = 'frag'; log('now:' + location.pathname + location.hash"
                    + " + ':' + JSON.stringify(history.state)); }, 0);"
                    + "setTimeout(function () { history.back(); }, 300);"
                    + "setTimeout(function () { history.forward(); }, 700);";
            server.routeFixed("/f/start", TestHttpServer.Response.html("<!doctype html><script>" + script + "</script><p id=frag>f</p>"));
            server.start();
            panel.setPage(server.url("/f/start"));
            awaitValue(panel, "ev", "now:/f/pushed#frag:null;pop:null:null;hash:->#frag;pop:{\"s\":1}:{\"s\":1};hash:#frag>-;pop:null:null;hash:->#frag;");
            Check.equal(1, server.count("/f/start"), "one request for the whole sequence");
            Check.equal(0, server.count("/f/pushed"), "the pushed address is not requested");
            Check.equal(server.url("/f/pushed#frag"), panel.getPage(), "forward returned to the fragment entry");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An entry that a page replaced with <code>replaceState</code> after a form POST becomes a plain GET entry: reloading it is a GET at
     * the new address, and not a question about sending the form again.
     * <p>
     * 폼 POST 뒤에 페이지가 <code>replaceState</code> 로 바꾼 항목은 일반 GET 항목이 된다. 그 항목을 새로고침하면 폼을 다시 보내겠느냐고 묻지 않고
     * 새 주소로 GET 을 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReplaceStateMakesAPostEntryAGetEntry() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/r/form", TestHttpServer.Response.html("<!doctype html><form id=f method=post action='/r/result'><input name=a value=1></form>"));
            server.route("/r/result", new TestHttpServer.Handler() {
                /**
                 * Answers the POST with a page that rewrites its own entry.
                 * <p>
                 * POST 에 자기 항목을 다시 쓰는 페이지로 답한다.
                 *
                 * @param request the request / 요청
                 * @return the response / 응답
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return TestHttpServer.Response.html("<!doctype html><script>setTimeout(function () { history.replaceState({ ok: 1 }, '', '/r/final');"
                            + " sessionStorage.setItem('done', JSON.stringify(history.state)); }, 0);</script><p>" + request.method + "</p>");
                }
            });
            server.routeFixed("/r/final", TestHttpServer.Response.html("<!doctype html><p>final</p>"));
            server.start();
            panel.setPage(server.url("/r/form"));
            awaitPage(panel, server.url("/r/form"));
            awaitSettled(panel);
            submitForm(panel, "f");
            awaitValue(panel, "done", "{\"ok\":1}");
            awaitPage(panel, server.url("/r/final"));
            Check.equal(1, server.count("/r/result"), "the form was sent once");
            panel.refresh();
            awaitCount(server, "/r/final", 1);
            Check.equal(1, server.count("/r/result"), "the reload did not send the form again");
            Check.equal(server.url("/r/final"), panel.getPage(), "the reload of the replaced entry");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
