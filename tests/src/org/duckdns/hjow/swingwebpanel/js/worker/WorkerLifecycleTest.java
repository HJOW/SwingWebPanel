package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.Iterator;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The life cycle of workers through the real component and a real HTTP server (plan item P20-09-b): <code>terminate()</code> stops a worker
 * that is in an endless loop and no callback runs afterwards (also not for messages that were already on their way), a page navigation,
 * <code>clean()</code>, <code>dispose()</code> and switching JavaScript off terminate every worker of the page and the number of worker threads
 * returns to the baseline, a page that spawns workers in a loop hits the limit of {@link WorkerLimits#MAX_WORKERS_PER_PAGE} with an exception,
 * repeated creation and termination does not accumulate threads, the worker threads are named daemon threads, and a worker that is terminated
 * while its script is still being fetched runs nothing.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 서버로 확인하는 worker 의 수명(계획 P20-09-b): <code>terminate()</code> 는 무한 반복 중인 worker 를 멈추고 그 뒤에는 어떤 콜백도
 * 실행되지 않는다(이미 오는 중이던 메시지도 마찬가지). 페이지 이동, <code>clean()</code>, <code>dispose()</code>, JavaScript 끄기는 페이지의 모든 worker 를
 * 종료하고 worker 스레드 수는 기준으로 돌아온다. 반복문으로 worker 를 만드는 페이지는 {@link WorkerLimits#MAX_WORKERS_PER_PAGE} 한도에서 예외를 만난다.
 * 반복 생성과 종료는 스레드를 쌓지 않는다. worker 스레드는 이름 있는 데몬 스레드이다. 스크립트를 가져오는 동안 종료된 worker 는 아무것도 실행하지 않는다.
 */
public class WorkerLifecycleTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerLifecycleTest() {
    }

    /**
     * Registers the scripts that the cases use.
     * <p>
     * 사례가 쓰는 스크립트를 등록한다.
     *
     * @param server the server / 서버
     */
    private static void routes(TestHttpServer server) {
        server.routeFixed("/loop.js", WorkerTestSupport.js("onmessage = function () { postMessage('looping'); for (;;) {} };"));
        server.routeFixed("/tick.js", WorkerTestSupport.js("setInterval(function () { postMessage('tick'); }, 5);"));
        server.routeFixed("/idle.js", WorkerTestSupport.js("onmessage = function () {};"));
        server.routeFixed("/other", TestHttpServer.Response.html(WorkerTestSupport.page("put('other', 'loaded');")));
    }

    /**
     * The page of the termination cases: two workers that are in an endless loop and one that sends a message every few milliseconds.
     * <p>
     * 종료 사례의 페이지: 무한 반복 중인 worker 둘과 몇 밀리초마다 메시지를 보내는 worker 하나.
     *
     * @return the page markup / 페이지 마크업
     */
    private static String busyPage() {
        return WorkerTestSupport.page(
                "var loops = [new Worker('/loop.js'), new Worker('/loop.js')]; var ticker = new Worker('/tick.js'); var ticks = 0; var started = 0;"
                + "loops[0].onmessage = loops[1].onmessage = function () { started++; if (started === 2) { put('loops', 'running'); } };"
                + "loops[0].postMessage('go'); loops[1].postMessage('go');"
                + "ticker.onmessage = function () { ticks++; if (ticks === 3) { put('ticker', 'running'); } };");
    }

    /**
     * Waits until the busy page has all three workers running.
     * <p>
     * 바쁜 페이지의 worker 셋이 모두 실행 중이 될 때까지 기다린다.
     *
     * @param panel  the component / 컴포넌트
     * @param before the number of worker threads before / 이전의 worker 스레드 수
     * @throws Exception on failure / 실패 시
     */
    private static void awaitBusy(SwingWebPanel panel, int before) throws Exception {
        Check.equal("running", WorkerTestSupport.await(panel, "loops"), "both endless loops started");
        Check.equal("running", WorkerTestSupport.await(panel, "ticker"), "the ticker started");
        Check.equal(before + 3, WorkerTestSupport.workerThreads(), "one thread per worker");
    }

    /**
     * <code>terminate()</code> stops a worker in an endless loop and one that sends messages, at once: nothing arrives afterwards.
     * <p>
     * <code>terminate()</code> 는 무한 반복 중인 worker 와 메시지를 보내는 worker 를 즉시 멈춘다. 그 뒤에는 아무것도 도착하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTerminateStopsAnEndlessLoopAndSilencesTheWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(busyPage().replace("</script>",
                    "setTimeout(function () { loops[0].terminate(); loops[1].terminate(); ticker.terminate(); put('stopped', ticks);"
                    + " setTimeout(function () { put('later', ticks); }, 600); }, 500);</script>")));
            server.start();
            panel.setPage(server.url("/page"));
            awaitBusy(panel, before);
            String stopped = WorkerTestSupport.await(panel, "stopped");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the threads of the terminated workers ended, also the one of the endless loop");
            Check.equal(stopped, WorkerTestSupport.await(panel, "later"), "no tick arrived after terminate()");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Messages that the worker had already posted when the page terminated it are not delivered: the page terminates the worker in the handler
     * of the fifth message of a burst of one thousand.
     * <p>
     * 페이지가 종료했을 때 worker 가 이미 보낸 메시지는 전달되지 않는다: 페이지는 천 개의 메시지 뭉치 중 다섯 번째 메시지의 핸들러에서 worker 를
     * 종료한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNothingIsDeliveredAfterTerminateEvenForMessagesOnTheirWay() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/burst.js", WorkerTestSupport.js(
                    "onmessage = function () { for (var i = 0; i < 1000; i++) { postMessage(i); } };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/burst.js'); var n = 0;"
                    + "w.onmessage = function (e) { n++; if (n === 5) { w.terminate(); put('stopAt', n); setTimeout(function () { put('final', n); }, 600); } };"
                    + "w.postMessage('go');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("5", WorkerTestSupport.await(panel, "stopAt"), "terminated in the handler of the fifth message");
            Check.equal("5", WorkerTestSupport.await(panel, "final"), "the messages that were already on their way never ran a callback");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Opening another page ends all workers of the previous page, also the one that is stuck in an endless loop.
     * <p>
     * 다른 페이지를 열면 이전 페이지의 모든 worker 가 끝난다. 무한 반복에 빠진 worker 도 마찬가지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationTerminatesAllWorkers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(busyPage()));
            server.start();
            panel.setPage(server.url("/page"));
            awaitBusy(panel, before);
            panel.setPage(server.url("/other"));
            Check.equal("loaded", WorkerTestSupport.await(panel, "other"), "the next page ran");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the worker threads ended with the page: "
                    + WorkerTestSupport.workerThreads());
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>clean()</code> ends all workers.
     * <p>
     * <code>clean()</code> 은 모든 worker 를 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanTerminatesAllWorkers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(busyPage()));
            server.start();
            panel.setPage(server.url("/page"));
            awaitBusy(panel, before);
            panel.clean();
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the worker threads ended with clean(): "
                    + WorkerTestSupport.workerThreads());
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>dispose()</code> ends all workers.
     * <p>
     * <code>dispose()</code> 는 모든 worker 를 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposeTerminatesAllWorkers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(busyPage()));
            server.start();
            panel.setPage(server.url("/page"));
            awaitBusy(panel, before);
            panel.dispose();
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the worker threads ended with dispose(): "
                    + WorkerTestSupport.workerThreads());
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Switching JavaScript off ends all workers of the page.
     * <p>
     * JavaScript 를 끄면 페이지의 모든 worker 가 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSwitchingJavascriptOffTerminatesAllWorkers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(busyPage()));
            server.start();
            panel.setPage(server.url("/page"));
            awaitBusy(panel, before);
            panel.setJavascriptEnabled(false);
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the worker threads ended when JavaScript was switched off: "
                    + WorkerTestSupport.workerThreads());
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page that creates workers in a loop gets a <code>QuotaExceededError</code> after the limit and does not exhaust the threads; after
     * one worker is terminated another can be created.
     * <p>
     * 반복문으로 worker 를 만드는 페이지는 한도 뒤에 <code>QuotaExceededError</code> 를 받고 스레드를 고갈시키지 않는다. worker 하나를 종료하면 다른
     * worker 를 만들 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWorkerLimitPerPage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var made = 0, err = ''; var ws = [];"
                    + "for (var i = 0; i < 40; i++) { try { ws.push(new Worker('/idle.js')); made++; } catch (e) { err = e.name + ':' + e.code; break; } }"
                    + "put('limit', made + ':' + err);"
                    + "ws[0].terminate(); var again;"
                    + "try { ws.push(new Worker('/idle.js')); again = 'created'; } catch (e) { again = e.name; }"
                    + "put('again', again);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("16:QuotaExceededError:22", WorkerTestSupport.await(panel, "limit"), "the 17th worker is refused with an exception");
            Check.equal("created", WorkerTestSupport.await(panel, "again"), "a terminated worker frees its place");
            // 종료한 worker 의 스레드가 끝나는 데 잠시 걸리므로 한도 이하가 될 때까지 기다린다.
            int settled = WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before + 16, 10000);
            Check.isTrue(settled - before <= 16, "never more worker threads than the limit once the terminated one ended: " + (settled - before));
        } finally {
            panel.dispose();
            server.stop();
        }
        Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "all threads ended with the component");
    }

    /**
     * Creating and terminating workers again and again leaves no threads behind.
     * <p>
     * worker 를 계속 만들고 종료해도 남는 스레드가 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRepeatedCreateAndTerminateLeavesNoThreads() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "for (var i = 0; i < 60; i++) { var w = new Worker('/idle.js'); if (i % 2 === 0) { w.postMessage('x'); } w.terminate(); }"
                    + "put('done', 'created and terminated 60');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("created and terminated 60", WorkerTestSupport.await(panel, "done"), "the loop ran without a limit error");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "no thread is left: " + WorkerTestSupport.workerThreads());
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The thread of a worker is a daemon thread named <code>swingwebpanel-worker-N</code>, and exactly one thread belongs to a worker.
     * <p>
     * worker 의 스레드는 <code>swingwebpanel-worker-N</code> 이라는 이름의 데몬 스레드이며 worker 하나에 스레드 하나가 속한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWorkerThreadIsANamedDaemon() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            routes(server);
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/idle.js'); w.onmessage = function () {}; w.postMessage('x'); put('created', 'yes');")));
            server.start();
            panel.setPage(server.url("/page"));
            WorkerTestSupport.await(panel, "created");
            long end = System.currentTimeMillis() + 10000;
            // 스레드가 만들어질 때까지 기다린다.
            while (WorkerTestSupport.workerThreads() <= before && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            int found = 0;
            Iterator<Map.Entry<Thread, StackTraceElement[]>> it = Thread.getAllStackTraces().entrySet().iterator();
            // 이름이 맞는 스레드가 데몬인지 확인한다.
            while (it.hasNext()) {
                Thread t = it.next().getKey();
                if (t.getName().startsWith("swingwebpanel-worker-")) {
                    found++;
                    Check.isTrue(t.isDaemon(), "the thread " + t.getName() + " is a daemon");
                    Check.isTrue(t.getName().substring("swingwebpanel-worker-".length()).matches("[0-9]+"), "a number follows the prefix: " + t.getName());
                }
            }
            Check.equal(before + 1, found, "one thread for the one worker");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A worker that is terminated while its script is still being fetched runs nothing, fires no event, and its thread ends.
     * <p>
     * 스크립트를 가져오는 동안 종료된 worker 는 아무것도 실행하지 않고 이벤트도 발생시키지 않으며 스레드가 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTerminateWhileTheScriptIsFetched() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/slow.js", WorkerTestSupport.js("postMessage('ran');").delay(800));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/slow.js'); var events = [];"
                    + "w.onmessage = function (e) { events.push('message'); }; w.onerror = function () { events.push('error'); };"
                    + "w.terminate(); setTimeout(function () { put('events', events.length + ':' + events.join(',')); }, 1500);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("0:", WorkerTestSupport.await(panel, "events"), "no message and no error from a terminated worker");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the thread ended");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A worker in an endless loop blocks neither the EDT of the component nor the page nor another worker: the EDT answers at once while
     * the loop runs, the timers of the page keep ticking, and a second worker answers every message.
     * <p>
     * 무한 반복 중인 worker 는 컴포넌트의 EDT 도, 페이지도, 다른 worker 도 막지 않는다: 반복이 실행되는 동안 EDT 가 곧바로 응답하고, 페이지의 타이머는 계속
     * 돌며, 두 번째 worker 는 모든 메시지에 답한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEndlessLoopBlocksNeitherTheEdtNorThePageNorAnotherWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            routes(server);
            server.routeFixed("/echo.js", WorkerTestSupport.js("onmessage = function (e) { postMessage('echo ' + e.data); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var loop = new Worker('/loop.js'); var echo = new Worker('/echo.js'); var ticks = 0, echoes = 0;"
                    + "loop.onmessage = function () { put('looping', 'yes'); }; loop.postMessage('go');"
                    + "echo.onmessage = function (e) { echoes++; };"
                    + "setInterval(function () { ticks++; echo.postMessage(ticks); }, 20);"
                    + "setTimeout(function () { put('page', ticks + ':' + echoes); }, 800);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("yes", WorkerTestSupport.await(panel, "looping"), "the endless loop is running");
            long start = System.currentTimeMillis();
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    return;
                }
            });
            Check.isTrue(System.currentTimeMillis() - start < 3000, "the EDT answers while a worker loops forever");
            String page = WorkerTestSupport.await(panel, "page");
            int ticks = Integer.parseInt(page.substring(0, page.indexOf(':')));
            int echoes = Integer.parseInt(page.substring(page.indexOf(':') + 1));
            Check.isTrue(ticks >= 5 && echoes >= 3, "the timers of the page and the other worker keep running: " + page);
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An endless loop that nobody terminates is aborted by the limits of the script task of the component (steps and time): the page gets an
     * <code>error</code> event of the worker object that says so, and the worker lives on and answers the next message.
     * <p>
     * 아무도 종료하지 않은 무한 반복은 컴포넌트의 스크립트 작업 한도(단계 수와 시간)에 걸려 중단된다: 페이지는 worker 객체의 <code>error</code> 이벤트로 그 사실을
     * 알게 되고, worker 는 계속 살아 다음 메시지에 답한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEndlessLoopIsAbortedByTheTaskLimitAndTheWorkerLivesOn() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "onmessage = function (e) { if (e.data === 'loop') { for (;;) {} } postMessage('alive after the abort'); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js');"
                    + "w.onerror = function (e) { put('abort', (e.message.indexOf('aborted') >= 0) + ':' + (e instanceof ErrorEvent)); e.preventDefault(); w.postMessage('next'); };"
                    + "w.onmessage = function (e) { put('next', e.data); };"
                    + "w.postMessage('loop');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("true:true", WorkerTestSupport.await(panel, "abort"), "the task of the endless loop was aborted and the page was told");
            Check.equal("alive after the abort", WorkerTestSupport.await(panel, "next"), "the worker answers the next message");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
