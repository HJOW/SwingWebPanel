package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Audit of the cleanup contract with a real component, real scripts and a real HTTP server (plan items P21-02, P21-04 and P21-08): work
 * that finishes after <code>clean()</code> or <code>dispose()</code> cannot bring back the old page, its source, its storage or its
 * requests; scripts, timers and requests of a cleaned or disposed page stop for good; and a storm of concurrent navigation, clean and
 * dispose calls from several threads leaves a consistent state without an error on any thread. Everything is measured through the public
 * methods and the request log of the server, never through the internals.
 * <p>
 * 실제 컴포넌트, 실제 스크립트, 실제 HTTP 서버로 정리 계약을 점검한다(계획 P21-02, P21-04, P21-08): <code>clean()</code> 이나
 * <code>dispose()</code> 뒤에 끝난 작업은 이전 페이지, 원문, 저장소, 요청을 되살리지 못하고, 정리했거나 폐기한 페이지의 스크립트, 타이머, 요청은 영원히
 * 멈추며, 여러 스레드에서 동시에 부른 이동·clean·dispose 폭풍 뒤에도 어느 스레드의 오류도 없이 일관된 상태가 남는다. 모두 공개 메서드와 서버의 요청
 * 기록으로 측정하며 내부는 보지 않는다.
 */
public class LifecycleAuditIntegrationTest {

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The components that the case created.
     * <p>
     * 사례가 만든 컴포넌트.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LifecycleAuditIntegrationTest() {
    }

    /**
     * Creates the server (the cases start it after their routes are known).
     * <p>
     * 서버를 만든다(사례가 경로를 정한 뒤 시작한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
    }

    /**
     * Disposes the components and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        // 사례가 만든 컴포넌트를 모두 폐기한다(이미 폐기했어도 안전하다).
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Creates a component that the case disposes at the end.
     * <p>
     * 사례가 끝에 폐기할 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Waits until a condition on the component holds, at most 30 seconds.
     * <p>
     * 컴포넌트에 대한 조건이 성립할 때까지 최대 30초 기다린다.
     *
     * @param p      the component / 컴포넌트
     * @param url    the address that must be confirmed / 확정되어야 하는 주소
     * @param source a text of the source that must be present / 원문에 있어야 하는 텍스트
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel p, String url, String source) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 원문이 도착할 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getContent().indexOf(source) < 0) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "confirmed page");
        Check.contains(p.getContent(), source, "source");
    }

    /**
     * Counts the storage entries of a collection.
     * <p>
     * 저장소 항목 컬렉션의 개수를 센다.
     *
     * @param entries the entries / 항목
     * @return the number / 개수
     */
    private static int size(java.util.Collection<StorageEntry> entries) {
        return entries.size();
    }

    /**
     * <code>clean()</code> while the main document is still being received: the late response does not bring the page back, nor its source,
     * nor an history entry, nor a changed status.
     * <p>
     * 주 문서를 아직 받는 중에 <code>clean()</code> 을 부르면 늦게 온 응답이 페이지도, 원문도, 이력 항목도, 바뀐 상태도 되살리지 못한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanWhileTheMainDocumentIsStillLoading() throws Exception {
        server.routeFixed("/slow", TestHttpServer.Response.html("<p>late page</p>").delay(1200));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/slow"));
        Check.isTrue(server.awaitRequests(1, 10000), "the request is in flight");
        p.clean();
        String status = p.getStatusText();
        Check.equal("about:blank", p.getPage(), "blank right after clean");
        Thread.sleep(1800);
        Check.equal("about:blank", p.getPage(), "the late response did not bring the page back");
        Check.equal("", p.getContent(), "no source came back");
        Check.equal(1, p.getHistories().size(), "no history entry came back: " + p.getHistories());
        Check.equal(100, p.getProgress(), "progress is final");
        Check.equal(status, p.getStatusText(), "the status did not change");
    }

    /**
     * Work of a page that is still pending when <code>clean()</code> is called — a timer, a slow request whose answer writes storage — never
     * runs its callback, so neither the session storage nor the local storage is filled again.
     * <p>
     * <code>clean()</code> 을 부를 때 아직 대기 중인 페이지의 작업(타이머, 답이 저장소에 쓰는 느린 요청)은 콜백을 실행하지 않으므로 세션 저장소도 로컬
     * 저장소도 다시 채워지지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanStopsPendingTimersAndRequestsOfThePage() throws Exception {
        server.routeFixed("/slowdata", TestHttpServer.Response.ok("text/plain", "data").delay(900));
        server.routeFixed("/work", TestHttpServer.Response.html("<!doctype html><meta charset=utf-8><script>"
                + "setTimeout(function () { sessionStorage.setItem('timer', '1'); localStorage.setItem('timer', '1'); }, 400);"
                + "var x = new XMLHttpRequest(); x.open('GET', '/slowdata');"
                + "x.onload = function () { sessionStorage.setItem('xhr', '1'); localStorage.setItem('xhr', '1'); }; x.send();"
                + "</" + "script>"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/work"));
        await(p, server.url("/work"), "slowdata");
        Check.isTrue(server.awaitRequests(2, 10000), "the slow request started");
        p.clean();
        Thread.sleep(1800);
        Check.equal(0, size(p.getSessionStorage()), "no session storage came back: " + p.getSessionStorage());
        Check.equal(0, size(p.getLocalStorage()), "no local storage came back: " + p.getLocalStorage());
        Check.equal("about:blank", p.getPage(), "still blank");
    }

    /**
     * A page that polls the server every few milliseconds stops polling when it is cleaned, when it is replaced by another page and when
     * the component is disposed; the request log of the server shows that no request arrives after the stop.
     * <p>
     * 몇 밀리초마다 서버를 호출하는 페이지는 정리될 때, 다른 페이지로 교체될 때, 컴포넌트를 폐기할 때 호출을 멈춘다. 서버의 요청 기록에서 멈춘 뒤에는
     * 요청이 오지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPollingStopsForCleanReplaceAndDispose() throws Exception {
        server.routeFixed("/ping", TestHttpServer.Response.ok("text/plain", "pong"));
        server.routeFixed("/poll", TestHttpServer.Response.html("<!doctype html><meta charset=utf-8><script>"
                + "setInterval(function () { var x = new XMLHttpRequest(); x.open('GET', '/ping'); x.send(); }, 20);</" + "script>"));
        server.routeFixed("/quiet", TestHttpServer.Response.html("<p>quiet</p>"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/poll"));
        await(p, server.url("/poll"), "setInterval");
        Check.isTrue(waitForPings(5), "the page polls");
        p.clean();
        assertSilent("after clean");
        p.setPage(server.url("/poll"));
        await(p, server.url("/poll"), "setInterval");
        Check.isTrue(waitForPings(server.count("/ping") + 5), "the page polls again");
        p.setPage(server.url("/quiet"));
        await(p, server.url("/quiet"), "quiet");
        assertSilent("after the page was replaced");
        p.setPage(server.url("/poll"));
        await(p, server.url("/poll"), "setInterval");
        Check.isTrue(waitForPings(server.count("/ping") + 5), "the page polls a third time");
        p.dispose();
        assertSilent("after dispose");
    }

    /**
     * Waits until the server has logged at least a number of pings.
     * <p>
     * 서버가 핑을 적어도 일정 개수 기록할 때까지 기다린다.
     *
     * @param n the number of pings / 핑 개수
     * @return true if reached / 도달했으면 true
     * @throws Exception if interrupted / 인터럽트되면
     */
    private boolean waitForPings(int n) throws Exception {
        long end = System.currentTimeMillis() + 15000;
        // 서버 기록의 핑 수가 목표에 이를 때까지 짧게 쉬며 기다린다.
        while (server.count("/ping") < n && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return server.count("/ping") >= n;
    }

    /**
     * Checks that no ping arrives any more: the number may not grow during half a second after a short settle time.
     * <p>
     * 더 이상 핑이 오지 않는지 확인한다. 잠시 가라앉힌 뒤 0.5초 동안 개수가 늘지 않아야 한다.
     *
     * @param when what happened before / 앞서 일어난 일
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void assertSilent(String when) throws Exception {
        Thread.sleep(300);
        int before = server.count("/ping");
        Thread.sleep(500);
        Check.equal(before, server.count("/ping"), "no ping " + when);
    }

    /**
     * A storm of navigation, <code>setContent</code>, refresh, back, clean and dispose from several threads at once, with pages that run
     * scripts and requests, leaves a consistent final state, raises no unexpected exception on any thread and, once the component is
     * disposed, no more requests arrive.
     * <p>
     * 스크립트와 요청을 실행하는 페이지에 여러 스레드가 동시에 이동, <code>setContent</code>, 새로고침, 뒤로, clean, dispose 를 퍼부어도 일관된 최종 상태가
     * 남고, 어느 스레드에서도 예상 밖 예외가 없으며, 컴포넌트를 폐기한 뒤에는 요청이 더 오지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConcurrentStormLeavesAConsistentState() throws Exception {
        server.routeFixed("/ping", TestHttpServer.Response.ok("text/plain", "pong").delay(5));
        server.routeFixed("/a", TestHttpServer.Response.html("<!doctype html><meta charset=utf-8><p>a</p><script>"
                + "setInterval(function () { var x = new XMLHttpRequest(); x.open('GET', '/ping'); x.send(); sessionStorage.setItem('a', String(Date.now())); }, 15);"
                + "</" + "script>"));
        server.routeFixed("/b", TestHttpServer.Response.html("<!doctype html><meta charset=utf-8><p>b</p><script>"
                + "fetch('/ping').then(function () { localStorage.setItem('b', '1'); });</" + "script>").delay(30));
        server.start();
        final SwingWebPanel p = panel();
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());
        final long end = System.currentTimeMillis() + 2500;
        List<Thread> threads = new ArrayList<Thread>();
        // 세 스레드가 서로 다른 순서로 무작위 연산을 계속 부른다.
        for (int t = 0; t < 3; t++) {
            final int seed = t + 1;
            Thread th = new Thread(new Runnable() {
                /**
                 * Calls random public methods until the time is over and records every error.
                 * <p>
                 * 시간이 다할 때까지 공개 메서드를 무작위로 부르며 오류를 기록한다.
                 */
                public void run() {
                    Random random = new Random(seed);
                    // 정해진 시간 동안 무작위 연산 하나씩 실행한다.
                    while (System.currentTimeMillis() < end) {
                        try {
                            switch (random.nextInt(8)) {
                            case 0:
                                p.setPage(server.url("/a"));
                                break;
                            case 1:
                                p.setPage(server.url("/b"));
                                break;
                            case 2:
                                p.setContent("<p>text</p><script>setTimeout(function () { sessionStorage.setItem('c', '1'); }, 10);</" + "script>", "text/html");
                                break;
                            case 3:
                                p.refresh(random.nextBoolean());
                                break;
                            case 4:
                                p.back();
                                break;
                            case 5:
                                p.forward();
                                break;
                            case 6:
                                p.clean();
                                break;
                            default:
                                p.getHistories();
                                p.getContent();
                                p.getProgress();
                                p.getSessionStorage();
                                break;
                            }
                            Thread.sleep(random.nextInt(8));
                        } catch (InterruptedException e) {
                            return;
                        } catch (Throwable e) {
                            errors.add(e);
                        }
                    }
                }
            }, "storm-" + seed);
            threads.add(th);
            th.start();
        }
        // 세 스레드가 모두 끝날 때까지 기다린다.
        for (Thread th : threads) {
            th.join(20000);
        }
        Check.isTrue(errors.isEmpty(), "no error on any thread: " + errors);
        List<String> history = p.getHistories();
        Check.isTrue(history.size() >= 1, "a history always exists");
        Check.notNull(p.getPage(), "a page is reported");
        p.clean();
        Check.equal("about:blank", p.getPage(), "clean ends in the blank page");
        Check.equal(1, p.getHistories().size(), "clean leaves one history entry");
        p.dispose();
        p.dispose();
        Thread.sleep(500);
        int after = server.totalRequests();
        Thread.sleep(800);
        Check.equal(after, server.totalRequests(), "no request arrives after dispose");
        Check.equal("about:blank", p.getPage(), "disposed component is blank");
        Check.equal("", p.getContent(), "disposed component has no source");
        Check.equal(100, p.getProgress(), "disposed component is idle");
        try {
            p.setPage(server.url("/a"));
            Check.fail("a disposed component refuses navigation");
        } catch (IllegalStateException expected) {
            Check.isTrue(true, "refused");
        }
    }
}
