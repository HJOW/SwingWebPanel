package org.duckdns.hjow.swingwebpanel.core;

import java.awt.GraphicsEnvironment;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests the life cycle events of a page in a real {@link SwingWebPanel} against a real local HTTP server (HTML Standard "Page lifecycle" and
 * "unloading documents", fetched 2026-11-17; Page Visibility Level 2; see <code>docs/progress/webapi2.md</code>): the order of <code>beforeunload</code>,
 * <code>pagehide</code>, <code>visibilitychange</code> and <code>unload</code> when a page is left by <code>setPage</code>, <code>back</code>,
 * <code>forward</code>, <code>refresh</code>, <code>setContent</code>, <code>clean()</code> and <code>dispose()</code>; the pages that must hear nothing (scripts
 * off, never shown, a navigation inside the same document); and the events that the host reports: the visibility of the component, the focus of the window and
 * the network state. Two witnesses watch every page: the script of the page appends each event it hears to <code>sessionStorage</code> (the <em>JS log</em>,
 * which survives a navigation but not <code>clean()</code>), and a Java listener that the test puts on the window and the document of the shown page (the
 * <em>Java log</em>, which sees the events at the moment the document queue runs them and survives everything).
 * <p>
 * 실제 {@link SwingWebPanel} 에서 실제 로컬 HTTP 서버를 상대로 페이지 수명 이벤트를 시험한다(HTML Standard "Page lifecycle", "unloading documents", 2026-11-17 조회,
 * Page Visibility Level 2, <code>docs/progress/webapi2.md</code> 참조): <code>setPage</code>, <code>back</code>, <code>forward</code>, <code>refresh</code>,
 * <code>setContent</code>, <code>clean()</code>, <code>dispose()</code> 로 페이지를 떠날 때 <code>beforeunload</code>, <code>pagehide</code>,
 * <code>visibilitychange</code>, <code>unload</code> 의 순서, 아무것도 들으면 안 되는 페이지(스크립트를 끈 페이지, 한 번도 보이지 않은 페이지, 같은 문서 안의 이동),
 * 호스트가 알리는 이벤트(컴포넌트의 가시성, 창 포커스, 네트워크 상태). 두 증인이 모든 페이지를 지켜본다: 페이지의 스크립트가 들은 이벤트를
 * <code>sessionStorage</code> 에 덧붙이고(<em>JS 기록</em>, 이동에는 남지만 <code>clean()</code> 에는 지워진다), 시험이 보이는 페이지의 window 와 문서에 붙인 Java
 * 리스너가 문서 큐가 이벤트를 실행하는 순간에 기록한다(<em>Java 기록</em>, 무엇에도 지워지지 않는다).
 */
public class PageLifecycleIntegrationTest {

    /**
     * The longest wait for the page to report in milliseconds.
     * <p>
     * 페이지가 보고하기를 기다리는 가장 긴 시간(밀리초).
     */
    private static final long WAIT_MS = 10000;

    /**
     * What a page that is shown, then left, reports: its start, the load events and the unloading sequence.
     * <p>
     * 보인 뒤 떠나는 페이지가 보고하는 것: 시작, load 이벤트, 언로드 순서.
     */
    private static final String FULL = "start-visible-true,load,pageshow,beforeunload,pagehide,visibilitychange-hidden,unload";

    /**
     * The unloading sequence alone.
     * <p>
     * 언로드 순서만.
     */
    private static final String LEAVE = "beforeunload,pagehide,visibilitychange-hidden,unload";

    /**
     * What a page that is shown reports before it is left.
     * <p>
     * 보이는 페이지가 떠나기 전에 보고하는 것.
     */
    private static final String SHOWN = "start-visible-true,load,pageshow";

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public PageLifecycleIntegrationTest() {
    }

    /**
     * A server, a panel and the two logs of the events of the pages.
     * <p>
     * 서버, 패널, 페이지 이벤트의 두 기록.
     */
    private static final class Rig {
        /**
         * The server; <code>/a</code> and <code>/b</code> are pages that report, a new id for every load.
         * <p>
         * 서버. <code>/a</code> 와 <code>/b</code> 는 보고하는 페이지이며 읽을 때마다 새 id 를 가진다.
         */
        final TestHttpServer server = new TestHttpServer();

        /**
         * The panel under test.
         * <p>
         * 시험 대상 패널.
         */
        SwingWebPanel panel;

        /**
         * The number of the loads of <code>/a</code>, so that every load of a page has its own id (<code>a1</code>, <code>a2</code>, ...).
         * <p>
         * <code>/a</code> 의 읽기 횟수. 페이지를 읽을 때마다 자기 id(<code>a1</code>, <code>a2</code>, ...)를 가진다.
         */
        final AtomicInteger loadsOfA = new AtomicInteger();

        /**
         * The number of the loads of <code>/b</code>.
         * <p>
         * <code>/b</code> 의 읽기 횟수.
         */
        final AtomicInteger loadsOfB = new AtomicInteger();

        /**
         * The number of the loads of <code>/v</code>.
         * <p>
         * <code>/v</code> 의 읽기 횟수.
         */
        final AtomicInteger loadsOfV = new AtomicInteger();

        /**
         * The Java log: <code>id-type</code> of every event that the Java listeners heard, in the order of their dispatch.
         * <p>
         * Java 기록: Java 리스너가 들은 모든 이벤트의 <code>id-type</code>. 전달된 순서.
         */
        final List<String> java = Collections.synchronizedList(new ArrayList<String>());

        /**
         * Starts the server and creates the panel on the event dispatch thread.
         * <p>
         * 서버를 시작하고 이벤트 디스패치 스레드에서 패널을 만든다.
         *
         * @throws Exception on failure / 실패 시
         */
        Rig() throws Exception {
            server.route("/a", new TestHttpServer.Handler() {
                /**
                 * Answers with a reporting page that has the next id of <code>a</code>.
                 * <p>
                 * <code>a</code> 의 다음 id 를 가진 보고하는 페이지로 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the page / 페이지
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return page("a" + loadsOfA.incrementAndGet(), request.query != null && request.query.indexOf("slow") >= 0);
                }
            });
            server.route("/b", new TestHttpServer.Handler() {
                /**
                 * Answers with a reporting page that has the next id of <code>b</code>.
                 * <p>
                 * <code>b</code> 의 다음 id 를 가진 보고하는 페이지로 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the page / 페이지
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return page("b" + loadsOfB.incrementAndGet(), false);
                }
            });
            // 문서에서만 visibilitychange 를 듣는 페이지(window 에는 언로드 이벤트 리스너가 없다).
            server.route("/v", new TestHttpServer.Handler() {
                /**
                 * Answers with a page whose only unloading listener is a <code>visibilitychange</code> listener at the document.
                 * <p>
                 * 유일한 언로드 리스너가 문서의 <code>visibilitychange</code> 리스너인 페이지로 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the page / 페이지
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    String id = "v" + loadsOfV.incrementAndGet();
                    return TestHttpServer.Response.html("<!doctype html><title>" + id + "</title><script>"
                            + "function L(s) { try { sessionStorage.setItem('L', (sessionStorage.getItem('L') || '') + '" + id + "-' + s + '|'); } catch (e) { } }"
                            + "window.addEventListener('pageshow', function () { L('pageshow'); });"
                            + "document.addEventListener('visibilitychange', function () { L('visibilitychange-' + document.visibilityState); });"
                            + "</" + "script><p>" + id + "</p>").header("Cache-Control", "no-store");
                }
            });
            // 이미지 요청을 오래 붙잡아 load 이벤트를 늦추는 경로.
            server.routeFixed("/slow.png", new TestHttpServer.Response(404).delay(2500).text("text/plain", "slow"));
            server.routeFixed("/plain", TestHttpServer.Response.html("<!doctype html><title>plain</title><p>no script</p>"));
            server.start();
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Creates the panel.
                 * <p>
                 * 패널을 만든다.
                 */
                public void run() {
                    panel = new SwingWebPanel();
                    panel.setSize(300, 180);
                }
            });
        }

        /**
         * Builds the response with a reporting page; it is not cached, so that going back loads the page again with a new id.
         * <p>
         * 보고하는 페이지의 응답을 만든다. 캐시하지 않으므로 뒤로 가도 새 id 로 다시 읽는다.
         *
         * @param id   the id of this load of the page / 이 읽기의 페이지 id
         * @param slow true to add an image that the server holds back, so that <code>load</code> is delayed / 서버가 붙잡는 이미지를 더해 <code>load</code> 를 늦추려면 true
         * @return the response / 응답
         */
        TestHttpServer.Response page(String id, boolean slow) {
            return TestHttpServer.Response.html("<!doctype html><title>" + id + "</title><script>" + script(id) + "</" + "script><p>" + id + "</p>"
                    + (slow ? "<img src='/slow.png' alt='slow'>" : "")).header("Cache-Control", "no-store");
        }

        /**
         * Stops the server and disposes the panel.
         * <p>
         * 서버를 멈추고 패널을 폐기한다.
         */
        void close() {
            try {
                panel.dispose();
            } catch (RuntimeException e) {
                // 시험이 이미 폐기했을 수 있다.
            }
            server.stop();
        }

        /**
         * Returns the JS log: what the scripts of the pages appended to their <code>sessionStorage</code>, as <code>id-event</code>.
         * <p>
         * JS 기록을 반환한다: 페이지의 스크립트가 <code>sessionStorage</code> 에 덧붙인 것. <code>id-event</code> 형식.
         *
         * @return the entries in order / 순서대로의 항목
         */
        List<String> js() {
            List<String> out = new ArrayList<String>();
            // 복사한 저장소 항목 중 기록 키의 값을 이어 붙인다.
            for (StorageEntry entry : panel.getSessionStorage()) {
                if ("L".equals(entry.getKey())) {
                    // 세로줄로 끝나는 항목을 하나씩 나눈다.
                    for (String part : entry.getValue().split("\\|")) {
                        if (part.length() > 0) {
                            out.add(part);
                        }
                    }
                }
            }
            return out;
        }

        /**
         * Returns what a log has for one page, without the id and joined with commas.
         * <p>
         * 기록에서 한 페이지의 것을 id 를 빼고 쉼표로 이어 반환한다.
         *
         * @param log the log / 기록
         * @param id  the id of the page / 페이지 id
         * @return the events of the page / 페이지의 이벤트
         */
        private static String of(List<String> log, String id) {
            StringBuilder out = new StringBuilder();
            List<String> copy;
            synchronized (log) {
                copy = new ArrayList<String>(log);
            }
            // 페이지 id 로 시작하는 항목만 모은다.
            for (String s : copy) {
                if (s.startsWith(id + "-")) {
                    if (out.length() > 0) {
                        out.append(',');
                    }
                    out.append(s.substring(id.length() + 1));
                }
            }
            return out.toString();
        }

        /**
         * Returns what the scripts of a page reported.
         * <p>
         * 페이지의 스크립트가 보고한 것을 반환한다.
         *
         * @param id the id of the page / 페이지 id
         * @return the events joined with commas / 쉼표로 이은 이벤트
         */
        String js(String id) {
            return of(js(), id);
        }

        /**
         * Returns what the Java listeners heard at a page.
         * <p>
         * Java 리스너가 페이지에서 들은 것을 반환한다.
         *
         * @param id the id of the page / 페이지 id
         * @return the events joined with commas / 쉼표로 이은 이벤트
         */
        String java(String id) {
            return of(java, id);
        }

        /**
         * Waits until the scripts reported the given event, and fails if it does not come.
         * <p>
         * 스크립트가 주어진 이벤트를 보고할 때까지 기다리고 오지 않으면 실패한다.
         *
         * @param report the report, such as <code>a1-pageshow</code> / 보고. 예: <code>a1-pageshow</code>
         * @throws Exception on interruption / 중단되면
         */
        void await(String report) throws Exception {
            long deadline = System.currentTimeMillis() + WAIT_MS;
            // 마감까지 보고가 도착하는지 계속 확인한다.
            while (!js().contains(report)) {
                if (System.currentTimeMillis() > deadline) {
                    Check.fail("the report '" + report + "' did not arrive; the JS log is " + js());
                }
                Thread.sleep(20);
            }
        }

        /**
         * Waits until the page with the given id is shown <em>and confirmed</em>: its scripts reported <code>pageshow</code> and the panel reports its address.
         * The scripts hear <code>pageshow</code> a moment before the component confirms the page, and a navigation that starts in between never shows (and so never
         * unloads) that page; a test that navigates on must therefore wait for the confirmation too. The address is <code>/a</code>, <code>/b</code> or
         * <code>/v</code> by the first letter of the id.
         * <p>
         * 주어진 id 의 페이지가 보이고 <em>확정</em>될 때까지 기다린다: 스크립트가 <code>pageshow</code> 를 보고하고 패널이 그 주소를 보고한다. 스크립트는 컴포넌트가
         * 페이지를 확정하기 조금 전에 <code>pageshow</code> 를 듣고, 그 사이에 시작한 이동은 그 페이지를 보이지도(따라서 언로드하지도) 않는다. 그래서 이동을 이어 가는
         * 시험은 확정도 기다려야 한다. 주소는 id 의 첫 글자에 따라 <code>/a</code>, <code>/b</code>, <code>/v</code> 이다.
         *
         * @param id the id of the page, such as <code>a1</code> / 페이지 id. 예: <code>a1</code>
         * @throws Exception on interruption / 중단되면
         */
        void shown(String id) throws Exception {
            await(id + "-pageshow");
            awaitPage(server.url("/" + id.charAt(0)));
        }

        /**
         * Waits until a page reported an event more often than a count.
         * <p>
         * 페이지가 어떤 이벤트를 주어진 횟수보다 더 보고할 때까지 기다린다.
         *
         * @param report the report / 보고
         * @param more   the count that must be exceeded / 넘어야 하는 횟수
         * @throws Exception on interruption / 중단되면
         */
        void awaitMoreThan(String report, int more) throws Exception {
            long deadline = System.currentTimeMillis() + WAIT_MS;
            // 마감까지 같은 보고의 개수가 늘어나는지 확인한다.
            while (count(js(), report) <= more) {
                if (System.currentTimeMillis() > deadline) {
                    Check.fail("the report '" + report + "' did not come more than " + more + " times; the JS log is " + js());
                }
                Thread.sleep(20);
            }
        }

        /**
         * Waits until the panel shows the address with a finished load (a page without a script cannot report).
         * <p>
         * 패널이 주소를 보여 주고 읽기가 끝날 때까지 기다린다(스크립트 없는 페이지는 보고할 수 없다).
         *
         * @param url the address / 주소
         * @throws Exception on interruption / 중단되면
         */
        void awaitPage(String url) throws Exception {
            long deadline = System.currentTimeMillis() + WAIT_MS;
            // 주소가 확정되고 진행률이 100 이 될 때까지 기다린다.
            while (!(url.equals(panel.getPage()) && panel.getProgress() == 100)) {
                if (System.currentTimeMillis() > deadline) {
                    Check.fail("the page " + url + " did not finish; it is " + panel.getPage() + " at " + panel.getProgress());
                }
                Thread.sleep(20);
            }
        }

        /**
         * Returns the session of the panel (the component keeps it in a private field).
         * <p>
         * 패널의 세션을 반환한다(컴포넌트가 비공개 필드에 가지고 있다).
         *
         * @return the session / 세션
         * @throws Exception on reflection failure / 리플렉션 실패 시
         */
        BrowserSession session() throws Exception {
            Field field = SwingWebPanel.class.getDeclaredField("session");
            field.setAccessible(true);
            return (BrowserSession) field.get(panel);
        }

        /**
         * Runs a task on the document queue of the shown page with its script host and waits for it.
         * <p>
         * 보이는 페이지의 문서 큐에서 스크립트 호스트와 함께 작업을 실행하고 기다린다.
         *
         * @param task the task / 작업
         * @throws Exception on failure / 실패 시
         */
        void onPage(final PageTask task) throws Exception {
            final BrowserSession session = session();
            final Throwable[] failure = new Throwable[1];
            final CountDownLatch done = new CountDownLatch(1);
            session.submitDocumentTask(new Runnable() {
                /**
                 * Finds the script host of the shown page and gives it to the task.
                 * <p>
                 * 보이는 페이지의 스크립트 호스트를 찾아 작업에 준다.
                 */
                public void run() {
                    try {
                        Field field = HtmlPageDocument.class.getDeclaredField("runtime");
                        field.setAccessible(true);
                        PageRuntime runtime = (PageRuntime) field.get(session.getSnapshot().getDocument());
                        task.run(runtime.scripts());
                    } catch (Throwable error) {
                        failure[0] = error;
                    } finally {
                        done.countDown();
                    }
                }
            });
            Check.isTrue(done.await(10, TimeUnit.SECONDS), "the document queue ran the task");
            Check.isNull(failure[0], "the task failed: " + failure[0]);
        }

        /**
         * Puts the Java listeners on the window and the document of the shown page; they log <code>id-type</code> into the Java log.
         * <p>
         * 보이는 페이지의 window 와 문서에 Java 리스너를 붙인다. 리스너는 <code>id-type</code> 을 Java 기록에 남긴다.
         *
         * @param id the id of the shown page / 보이는 페이지의 id
         * @throws Exception on failure / 실패 시
         */
        void attach(final String id) throws Exception {
            onPage(new PageTask() {
                /**
                 * Registers the recorder.
                 * <p>
                 * 기록 리스너를 등록한다.
                 *
                 * @param scripts the script host of the page / 페이지의 스크립트 호스트
                 */
                public void run(final PageScripts scripts) {
                    EventListener recorder = new EventListener() {
                        /**
                         * Records the event; <code>visibilitychange</code> with the state of the document at that moment.
                         * <p>
                         * 이벤트를 기록한다. <code>visibilitychange</code> 는 그 순간의 문서 상태와 함께 기록한다.
                         *
                         * @param event the event / 이벤트
                         */
                        public void handleEvent(Event event) {
                            if ("visibilitychange".equals(event.getType())) {
                                java.add(id + "-visibilitychange-" + (scripts.getLifecycle().isDocumentVisible() ? "visible" : "hidden"));
                            } else {
                                java.add(id + "-" + event.getType());
                            }
                        }
                    };
                    String[] types = { "beforeunload", "pagehide", "pageshow", "unload", "focus", "blur", "online", "offline", "hashchange", "popstate" };
                    // window 이벤트마다 기록 리스너를 붙인다.
                    for (String type : types) {
                        scripts.getWindow().addEventListener(type, recorder, false, false, false);
                    }
                    scripts.getDocument().addEventListener("visibilitychange", recorder, false, false, false);
                }
            });
        }

        /**
         * Evaluates a script in the shown page on the document queue.
         * <p>
         * 보이는 페이지에서 스크립트를 문서 큐로 평가한다.
         *
         * @param code the script / 스크립트
         * @throws Exception on failure / 실패 시
         */
        void evaluate(final String code) throws Exception {
            onPage(new PageTask() {
                /**
                 * Evaluates the script.
                 * <p>
                 * 스크립트를 평가한다.
                 *
                 * @param scripts the script host of the page / 페이지의 스크립트 호스트
                 */
                public void run(PageScripts scripts) {
                    scripts.evaluate(code, "lifecycle-test");
                }
            });
        }
    }

    /**
     * A task that runs on the document queue with the script host of the shown page.
     * <p>
     * 보이는 페이지의 스크립트 호스트와 함께 문서 큐에서 실행되는 작업.
     */
    private interface PageTask {
        /**
         * Runs the task.
         * <p>
         * 작업을 실행한다.
         *
         * @param scripts the script host of the page / 페이지의 스크립트 호스트
         */
        void run(PageScripts scripts);
    }

    /**
     * Returns the script that reports the events of a page to <code>sessionStorage</code> (the key <code>L</code>, entries closed by a vertical bar): the window
     * events, the visibility changes and the state at the start.
     * <p>
     * 페이지의 이벤트를 <code>sessionStorage</code> 에 보고하는 스크립트를 반환한다(키 <code>L</code>, 항목은 세로줄로 끝난다): window 이벤트, 가시성 변화, 시작할 때의 상태.
     *
     * @param id the id of the page / 페이지 id
     * @return the script text / 스크립트 문자열
     */
    private static String script(String id) {
        return "function L(s) { try { sessionStorage.setItem('L', (sessionStorage.getItem('L') || '') + '" + id + "-' + s + '|'); } catch (e) { } }"
                + "L('start-' + document.visibilityState + '-' + navigator.onLine);"
                + "['load', 'pageshow', 'beforeunload', 'pagehide', 'unload', 'focus', 'blur', 'online', 'offline', 'hashchange', 'popstate'].forEach(function (t) {"
                + " window.addEventListener(t, function () { L(t); }); });"
                + "document.addEventListener('visibilitychange', function () { L('visibilitychange-' + document.visibilityState); });";
    }

    /**
     * Counts the entries that are equal to a text.
     * <p>
     * 어떤 문자열과 같은 항목의 수를 센다.
     *
     * @param log  the entries / 항목
     * @param text the entry to count / 셀 항목
     * @return the number / 개수
     */
    private static int count(List<String> log, String text) {
        int n = 0;
        // 같은 항목을 센다.
        for (String s : log) {
            if (s.equals(text)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Leaving a page by <code>setPage</code> runs the unloading sequence in the order of the specification, once, and before the next page reports; the script of the
     * page and a Java listener hear the same.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationRunsTheUnloadingSequence() throws Exception {
        Rig r = new Rig();
        try {
            r.panel.setPage(r.server.url("/a"));
            r.shown("a1");
            Check.equal(SHOWN, r.js("a1"), "a page that is shown reports its start, load and pageshow");
            r.attach("a1");
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            Check.equal(FULL, r.js("a1"), "the page that was left heard beforeunload, pagehide, visibilitychange (hidden) and unload, in this order");
            Check.equal(LEAVE, r.java("a1"), "a Java listener heard the same sequence");
            // 다음 페이지의 보고는 떠난 페이지의 순서가 끝난 뒤에 시작한다.
            List<String> js = r.js();
            Check.isTrue(js.indexOf("a1-unload") < js.indexOf("b1-start-visible-true"), "the unload of the old page comes before the new page starts: " + js);
            // 떠난 페이지의 이벤트는 새 페이지의 것에 섞이지 않는다.
            Check.equal(SHOWN, r.js("b1"), "the new page reports only its own events");
        } finally {
            r.close();
        }
    }

    /**
     * Going back, going forward, refreshing and <code>setContent</code> leave the shown page the same way, and every load of a page is a page of its own.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackForwardRefreshAndSetContent() throws Exception {
        Rig r = new Rig();
        try {
            r.panel.setPage(r.server.url("/a"));
            r.shown("a1");
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            // 뒤로: b1 이 떠나고 a2 가 읽힌다.
            r.panel.back();
            r.shown("a2");
            Check.equal(FULL, r.js("b1"), "back leaves the page with the unloading sequence");
            // 앞으로: a2 가 떠나고 b2 가 읽힌다.
            r.panel.forward();
            r.shown("b2");
            Check.equal(FULL, r.js("a2"), "forward leaves the page with the unloading sequence");
            // 새로고침: b2 가 떠나고 b3 가 읽힌다.
            r.panel.refresh();
            r.shown("b3");
            Check.equal(FULL, r.js("b2"), "refresh leaves the page with the unloading sequence");
            r.panel.refresh(true);
            r.shown("b4");
            Check.equal(FULL, r.js("b3"), "a forced refresh leaves the page too");
            // setContent: b4 가 떠나고 about:text 의 페이지가 읽힌다. 이 페이지는 Java 기록으로 본다.
            r.panel.setContent("<!doctype html><script>" + script("c1") + "</" + "script><p>content</p>", "text/html");
            r.awaitPage("about:text");
            Check.equal(FULL, r.js("b4"), "setContent leaves the page");
            r.attach("c1");
            r.panel.setPage("about:blank");
            r.awaitPage("about:blank");
            Check.equal(LEAVE, r.java("c1"), "a page of setContent is unloaded when it is left");
        } finally {
            r.close();
        }
    }

    /**
     * <code>clean()</code> and <code>dispose()</code> run the unloading sequence before they return (they wait for the handlers a short time), also when they are
     * called on the event dispatch thread, and a sequence that ran does not run again.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanAndDisposeUnloadTheShownPage() throws Exception {
        final Rig r = new Rig();
        try {
            r.panel.setPage(r.server.url("/a"));
            r.shown("a1");
            r.attach("a1");
            r.panel.clean();
            // clean 이 돌아왔을 때 언로드 순서가 이미 끝나 있어야 한다(저장소는 clean 이 지우므로 Java 기록으로 본다).
            Check.equal(LEAVE, r.java("a1"), "clean() returned after the sequence of the shown page");
            Check.equal("about:blank", r.panel.getPage(), "and the page is blank");
            Check.equal(0, r.js().size(), "clean() cleared the sessionStorage of the page");
            // 이벤트 디스패치 스레드에서 불러도 막히지 않고 순서가 실행된다.
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            r.attach("b1");
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Cleans on the event dispatch thread.
                 * <p>
                 * 이벤트 디스패치 스레드에서 clean 한다.
                 */
                public void run() {
                    r.panel.clean();
                }
            });
            Check.equal(LEAVE, r.java("b1"), "clean() on the event dispatch thread runs the sequence as well");
            // 빈 페이지에서 clean 을 다시 불러도 아무것도 더 들리지 않는다.
            int before = r.java.size();
            r.panel.clean();
            Check.equal(before, r.java.size(), "cleaning a blank page reports nothing");
            r.panel.setPage(r.server.url("/a"));
            r.shown("a2");
            r.attach("a2");
            r.panel.dispose();
            Check.equal(LEAVE, r.java("a2"), "dispose() returned after the sequence of the shown page");
            int disposed = r.java.size();
            r.panel.dispose();
            Check.equal(disposed, r.java.size(), "a second dispose() reports nothing");
        } finally {
            r.close();
        }
    }

    /**
     * A page that is not scripted hears nothing: not with the scripts switched off before the load, and not when they are switched off while the page is shown.
     * After they are switched on again the next page works as usual.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptsOffHearNothing() throws Exception {
        final Rig r = new Rig();
        try {
            r.panel.setJavascriptEnabled(false);
            r.panel.setPage(r.server.url("/a"));
            r.awaitPage(r.server.url("/a"));
            r.panel.setPage(r.server.url("/b"));
            r.awaitPage(r.server.url("/b"));
            Check.equal("[]", r.js().toString(), "nothing is reported by pages whose scripts are off");
            // 스크립트를 켜면 다음 페이지부터 정상 동작한다.
            r.panel.setJavascriptEnabled(true);
            r.panel.setPage(r.server.url("/a"));
            r.shown("a2");
            r.attach("a2");
            // 보이는 동안 스크립트를 끄면 이 페이지는 떠날 때 아무것도 듣지 않는다.
            r.panel.setJavascriptEnabled(false);
            r.panel.setPage(r.server.url("/plain"));
            r.awaitPage(r.server.url("/plain"));
            Check.equal(SHOWN, r.js("a2"), "a page whose scripts were switched off while it was shown hears nothing when it is left");
            Check.equal("", r.java("a2"), "not even a Java listener hears it");
            r.panel.setJavascriptEnabled(true);
            r.panel.setPage(r.server.url("/b"));
            r.shown("b2");
            r.attach("b2");
            r.panel.clean();
            Check.equal(LEAVE, r.java("b2"), "the scripts that are on again work for the next page");
        } finally {
            r.close();
        }
    }

    /**
     * A page whose only listener for the end of the page is a <code>visibilitychange</code> listener at the document (the usual way to save state when the user
     * leaves) hears the change to hidden when it is left; it is not skipped as a page without listeners.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDocumentVisibilityListenerHearsTheLeave() throws Exception {
        final Rig r = new Rig();
        try {
            r.panel.setPage(r.server.url("/v"));
            r.shown("v1");
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            Check.equal("pageshow,visibilitychange-hidden", r.js("v1"), "the document heard visibilitychange when the page was left");
        } finally {
            r.close();
        }
    }

    /**
     * A page that was never shown (its <code>load</code> was still waiting for an image when the user went on) hears none of the unloading events: the
     * <code>pageshow</code> that comes after <code>load</code> is what makes a page one that can be unloaded (this component's documented simplification of the
     * specification, which fires <code>unload</code> for such a document too).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageThatWasNeverShownHearsNothing() throws Exception {
        Rig r = new Rig();
        try {
            r.panel.setPage(r.server.url("/a?slow"));
            // 스크립트가 시작했지만 이미지 때문에 load 는 아직 오지 않았다.
            r.await("a1-start-visible-true");
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            Check.equal("start-visible-true", r.js("a1"), "the page whose load never came heard none of the unloading events and fired neither load nor pageshow");
        } finally {
            r.close();
        }
    }

    /**
     * A navigation inside the document (a fragment of <code>setPage</code>, <code>location.hash</code>, <code>history.pushState</code>) is not the end of the page:
     * no unloading event fires, the page is not requested again, and the sequence runs once when the page is really left.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSameDocumentNavigationDoesNotUnload() throws Exception {
        Rig r = new Rig();
        try {
            r.panel.setPage(r.server.url("/a"));
            r.shown("a1");
            r.attach("a1");
            // 프래그먼트 이동은 문서를 다시 읽지 않고 hashchange 만 일으킨다.
            r.panel.setPage(r.server.url("/a#section"));
            r.awaitMoreThan("a1-hashchange", 0);
            r.panel.setPage(r.server.url("/a#other"));
            r.awaitMoreThan("a1-hashchange", 1);
            // 이력 이동도 같은 문서 안이면 문서가 그대로다.
            r.panel.back();
            r.awaitMoreThan("a1-hashchange", 2);
            // 스크립트가 일으키는 이동: 해시 변경과 pushState.
            r.evaluate("location.hash = 'script'; history.pushState(null, '', '?pushed');");
            r.awaitMoreThan("a1-hashchange", 3);
            Check.equal(1, r.server.count("/a"), "no move inside the document requested it again");
            // 프래그먼트 이동은 popstate 와 hashchange 를 이 순서로 일으킨다(pushState 자체는 popstate 를 일으키지 않는다).
            String moves = "popstate,hashchange,popstate,hashchange,popstate,hashchange,popstate,hashchange";
            Check.equal("start-visible-true,load,pageshow," + moves, r.js("a1"), "the moves fired popstate and hashchange and nothing of the unloading sequence");
            Check.equal(moves, r.java("a1"), "a Java listener heard the same");
            // 진짜로 떠날 때 순서가 한 번 실행된다.
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            Check.equal("start-visible-true,load,pageshow," + moves + "," + LEAVE, r.js("a1"), "the sequence runs once when the page is left");
        } finally {
            r.close();
        }
    }

    /**
     * The host reports the visibility of the component (shown or hidden on the screen), the focus of the window and the network state to the page, and a page that is
     * loaded while the component is hidden starts hidden and is told when the component is shown. It needs a screen; without one it skips itself. A real window
     * also moves the focus by itself, so the case looks at the focus events that it causes and counts the others by what the page reports.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHostVisibilityFocusAndNetworkState() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            Check.skip("no screen: the component cannot be shown on a window");
        }
        final Rig r = new Rig();
        final JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Puts the panel in a window and shows it.
                 * <p>
                 * 패널을 창에 넣고 보여 준다.
                 */
                public void run() {
                    frame[0] = new JFrame("lifecycle");
                    frame[0].getContentPane().add(r.panel);
                    frame[0].setSize(320, 240);
                    frame[0].setLocation(40, 40);
                    frame[0].setVisible(true);
                }
            });
            r.panel.setPage(r.server.url("/a"));
            r.shown("a1");
            r.attach("a1");
            // 창을 숨기면 문서가 hidden 이 되고 다시 보이면 visible 이 된다.
            setFrameVisible(frame[0], false);
            r.await("a1-visibilitychange-hidden");
            setFrameVisible(frame[0], true);
            r.await("a1-visibilitychange-visible");
            // 창 포커스: 컴포넌트의 포커스 리스너에 포커스 이벤트를 전달한다(실제 창이 포커스를 옮기는 이벤트가 섞여도 센다).
            int focuses = count(r.js(), "a1-focus");
            focus(r.panel, true);
            r.awaitMoreThan("a1-focus", focuses);
            int blurs = count(r.js(), "a1-blur");
            focus(r.panel, false);
            r.awaitMoreThan("a1-blur", blurs);
            // 네트워크 상태.
            r.panel.setOnline(false);
            r.await("a1-offline");
            r.panel.setOnline(true);
            r.await("a1-online");
            String js = r.js("a1").replaceAll(",(focus|blur)(?=,|$)", "");
            Check.equal("start-visible-true,load,pageshow,visibilitychange-hidden,visibilitychange-visible,offline,online", js,
                    "the events of the host in the order they happened (focus and blur left out)");
            String java = r.java("a1").replaceAll("(^|,)(focus|blur)(?=,|$)", "").replaceAll("^,", "");
            Check.equal("visibilitychange-hidden,visibilitychange-visible,offline,online", java, "a Java listener heard the same");
            // 숨겨진 동안 읽은 페이지는 hidden 으로 시작하고, 보이게 되면 알려 준다.
            setFrameVisible(frame[0], false);
            r.await("a1-visibilitychange-hidden");
            r.panel.setPage(r.server.url("/b"));
            r.shown("b1");
            Check.contains(r.js("b1"), "start-hidden-true,load,pageshow", "a page loaded while the component is hidden starts hidden");
            setFrameVisible(frame[0], true);
            r.await("b1-visibilitychange-visible");
            // 오프라인으로 읽은 페이지도 같다.
            r.panel.setOnline(false);
            r.panel.setPage(r.server.url("/a"));
            r.shown("a2");
            Check.contains(r.js("a2"), "start-visible-false", "a page loaded while the host is offline starts offline");
            r.panel.setOnline(true);
            r.await("a2-online");
            // 떠난 페이지는 호스트의 변화를 더 듣지 않는다.
            r.panel.setPage(r.server.url("/b"));
            r.shown("b2");
            String before = r.js("a2");
            setFrameVisible(frame[0], false);
            focus(r.panel, true);
            r.panel.setOnline(false);
            r.await("b2-offline");
            Check.equal(before, r.js("a2"), "a page that was left hears nothing more");
        } finally {
            r.close();
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Closes the window.
                 * <p>
                 * 창을 닫는다.
                 */
                public void run() {
                    if (frame[0] != null) {
                        frame[0].dispose();
                    }
                }
            });
        }
    }

    /**
     * A change of the host while the page is still loading cannot be announced then (the page has not finished loading); it is announced right after
     * <code>pageshow</code>, so that the page ends up knowing the real state. It needs a screen; without one it skips itself.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHostChangeDuringTheLoadIsAnnouncedAfterPageshow() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            Check.skip("no screen: the component cannot be shown on a window");
        }
        final Rig r = new Rig();
        final JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Puts the panel in a window and shows it.
                 * <p>
                 * 패널을 창에 넣고 보여 준다.
                 */
                public void run() {
                    frame[0] = new JFrame("lifecycle during load");
                    frame[0].getContentPane().add(r.panel);
                    frame[0].setSize(320, 240);
                    frame[0].setLocation(60, 60);
                    frame[0].setVisible(true);
                }
            });
            r.panel.setPage(r.server.url("/a?slow"));
            r.await("a1-start-visible-true");
            // 이미지 때문에 load 가 아직 오지 않은 동안 창을 숨기고 연결을 끊는다.
            setFrameVisible(frame[0], false);
            r.panel.setOnline(false);
            r.await("a1-pageshow");
            r.await("a1-visibilitychange-hidden");
            r.await("a1-offline");
            List<String> js = r.js();
            Check.isTrue(js.indexOf("a1-pageshow") < js.indexOf("a1-visibilitychange-hidden") && js.indexOf("a1-pageshow") < js.indexOf("a1-offline"),
                    "the changes of the load time were announced after pageshow: " + js);
        } finally {
            r.close();
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Closes the window.
                 * <p>
                 * 창을 닫는다.
                 */
                public void run() {
                    if (frame[0] != null) {
                        frame[0].dispose();
                    }
                }
            });
        }
    }

    /**
     * Shows or hides a window on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 창을 보이거나 숨긴다.
     *
     * @param frame   the window / 창
     * @param visible true to show it / 보이려면 true
     * @throws Exception on failure / 실패 시
     */
    private static void setFrameVisible(final JFrame frame, final boolean visible) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Changes the visibility.
             * <p>
             * 가시성을 바꾼다.
             */
            public void run() {
                frame.setVisible(visible);
            }
        });
    }

    /**
     * Gives the focus listeners of a component a focus event, as the AWT does when the component gains or loses the keyboard focus.
     * <p>
     * AWT 가 컴포넌트가 키보드 포커스를 얻거나 잃을 때 하는 것처럼 컴포넌트의 포커스 리스너에 포커스 이벤트를 준다.
     *
     * @param component the component / 컴포넌트
     * @param gained    true for a gain / 얻었으면 true
     * @throws Exception on failure / 실패 시
     */
    private static void focus(final SwingWebPanel component, final boolean gained) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Delivers the event to the listeners.
             * <p>
             * 이벤트를 리스너에 전달한다.
             */
            public void run() {
                FocusEvent event = new FocusEvent(component, gained ? FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST);
                // 등록된 포커스 리스너에 AWT 가 하는 대로 이벤트를 준다.
                for (FocusListener l : component.getFocusListeners()) {
                    if (gained) {
                        l.focusGained(event);
                    } else {
                        l.focusLost(event);
                    }
                }
            }
        });
    }
}
