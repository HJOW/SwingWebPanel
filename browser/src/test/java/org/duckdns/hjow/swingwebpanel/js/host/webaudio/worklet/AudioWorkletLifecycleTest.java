package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The life of the audio worklet and what it owns (plan items P25-06 and P21): the worklet thread (<code>swingwebpanel-audio-worklet-N</code>) starts with the first <code>addModule()</code> of a
 * context and ends when the context is closed, when an offline rendering completes, when the page is disposed (which also ends the rendering threads), and when the page is replaced while a module is
 * being fetched (the request is cancelled with the page); twenty pages that load a worklet and are disposed leave no thread; the processor of a closed context is not called any more, and the
 * messages of a disposed page are not delivered; disposing twice is harmless.
 * <p>
 * 오디오 worklet 의 수명과 그것이 소유하는 것(계획 P25-06, P21): worklet 스레드(<code>swingwebpanel-audio-worklet-N</code>)는 컨텍스트의 첫 <code>addModule()</code> 에 시작해 컨텍스트가 닫히거나, 오프라인 렌더링이
 * 끝나거나, 페이지가 폐기되거나(렌더링 스레드도 함께 끝난다), 모듈을 가져오는 동안 페이지가 교체되면(요청은 페이지와 함께 취소된다) 끝난다. worklet 을 불러온 페이지 스무 개를 폐기해도 스레드가 남지 않는다.
 * 닫힌 컨텍스트의 프로세서는 더 이상 불리지 않고, 폐기된 페이지의 메시지는 전달되지 않으며, 두 번 폐기해도 해롭지 않다.
 */
public class AudioWorkletLifecycleTest {

    /**
     * The name prefix of the worklet threads.
     * <p>
     * worklet 스레드의 이름 접두사.
     */
    private static final String WORKLET = "swingwebpanel-audio-worklet-";

    /**
     * The name prefix of the rendering threads of real-time contexts.
     * <p>
     * 실시간 컨텍스트의 렌더링 스레드의 이름 접두사.
     */
    private static final String RENDER = "swingwebpanel-audio-render-";

    /**
     * The name prefix of the rendering threads of offline contexts.
     * <p>
     * 오프라인 컨텍스트의 렌더링 스레드의 이름 접두사.
     */
    private static final String OFFLINE = "swingwebpanel-audio-offline-";

    /**
     * A processor that counts and reports.
     * <p>
     * 세고 보고하는 프로세서.
     */
    private static final String TICK = "registerProcessor('tick', class extends AudioWorkletProcessor {\n  constructor() { super(); this.n = 0; }\n"
            + "  process(i, o) { this.n++; this.port.postMessage(this.n); o[0][0].fill(0.1); return true; }\n});\n";

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletLifecycleTest() {
    }

    /**
     * Counts the live threads whose names start with a prefix.
     * <p>
     * 이름이 접두사로 시작하는 살아 있는 스레드를 센다.
     *
     * @param prefix the prefix / 접두사
     * @return the number of threads / 스레드 수
     */
    private static int threads(String prefix) {
        Thread[] all = new Thread[Thread.activeCount() + 32];
        int n = Thread.enumerate(all);
        int count = 0;
        // 살아 있는 스레드 중 이름이 접두사로 시작하는 것을 센다.
        for (int i = 0; i < n; i++) {
            if (all[i] != null && all[i].isAlive() && all[i].getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Waits until the number of threads with a prefix is a number (or the time is over) and returns the last count.
     * <p>
     * 접두사를 가진 스레드 수가 어떤 수가 될 때까지(또는 시간이 다할 때까지) 기다리고 마지막 수를 반환한다.
     *
     * @param prefix   the prefix / 접두사
     * @param expected the expected number / 기대하는 수
     * @param ms       the longest time to wait / 기다리는 최대 시간
     * @return the last count / 마지막 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static int awaitThreads(String prefix, int expected, long ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        int n = threads(prefix);
        // 스레드 수가 기대한 값이 될 때까지 짧게 쉬며 확인한다.
        while (n != expected && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            n = threads(prefix);
        }
        return n;
    }

    /**
     * A page with the tick module added to a context, and a node whose messages are counted in <code>ticks</code> (the highest count that arrived).
     * <p>
     * 틱 모듈을 컨텍스트에 추가하고, 메시지를 <code>ticks</code>(도착한 가장 큰 수)로 세는 노드를 만든 페이지.
     *
     * @param context the construction of the context / 컨텍스트의 생성
     * @return the page / 페이지
     */
    private static WorkletPage page(String context) {
        WorkletPage p = new WorkletPage();
        p.module("tick.js", TICK);
        p.run("var ctx = " + context + "; var ticks = 0; var ready = false; ctx.audioWorklet.addModule('tick.js').then(function () { var n = new AudioWorkletNode(ctx, 'tick', {numberOfInputs: 0});"
                + "n.port.onmessage = function (e) { ticks = e.data; }; n.connect(ctx.destination); ready = true; });");
        p.waitFor("ready", 20000, "the module and the node");
        return p;
    }

    /**
     * The worklet thread starts with the first <code>addModule()</code> (and not before), and a real-time context that is closed ends it and the rendering thread; the node is not processed any
     * more: the count that the processor reports stops growing.
     * <p>
     * worklet 스레드는 첫 <code>addModule()</code> 에 시작하며(그 전에는 아니다), 닫은 실시간 컨텍스트는 이 스레드와 렌더링 스레드를 끝낸다. 노드는 더 이상 처리되지 않는다: 프로세서가 보고하는 수가 더는
     * 늘지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClosingARealTimeContextEndsTheWorkletThread() throws Exception {
        int base = threads(WORKLET);
        int baseRender = threads(RENDER);
        WorkletPage p = new WorkletPage();
        p.run("var ctx = new AudioContext();");
        Check.equal(base, threads(WORKLET), "creating a context starts no worklet thread");
        Check.equal(base, threads(WORKLET), "nor does reading audioWorklet");
        p.run("ctx.audioWorklet;");
        Check.equal(base, threads(WORKLET), "nor does reading audioWorklet");
        p.module("tick.js", TICK);
        p.run("var ticks = 0; var ready = false; ctx.audioWorklet.addModule('tick.js').then(function () { var n = new AudioWorkletNode(ctx, 'tick', {numberOfInputs: 0});"
                + "n.port.onmessage = function (e) { ticks = e.data; }; n.connect(ctx.destination); ready = true; });");
        p.waitFor("ready && ticks > 10", 30000, "the processor runs");
        Check.equal(base + 1, threads(WORKLET), "addModule starts the worklet thread");
        Check.equal(baseRender + 1, threads(RENDER), "next to the rendering thread of the context");
        p.run("ctx.close();");
        Check.equal(base, awaitThreads(WORKLET, base, 5000), "close() ends the worklet thread");
        Check.equal(baseRender, awaitThreads(RENDER, baseRender, 5000), "and the rendering thread");
        p.pumpFor(200);
        double after = p.number("ticks");
        p.pumpFor(300);
        Check.near(after, p.number("ticks"), 0, "the processor of a closed context is not called any more");
    }

    /**
     * An offline rendering that completes ends the worklet thread of its context (nothing can render again), after the audio that the processor produced was delivered.
     * <p>
     * 끝난 오프라인 렌더링은 그 컨텍스트의 worklet 스레드를 끝낸다(다시 렌더링할 수 없다). 프로세서가 만든 오디오는 전달된 뒤이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnOfflineRenderingEndsTheWorkletThread() throws Exception {
        int base = threads(WORKLET);
        int baseOffline = threads(OFFLINE);
        WorkletPage p = page("new OfflineAudioContext(1, 1280, 8000)");
        Check.equal(base + 1, threads(WORKLET), "the worklet thread exists after addModule");
        p.run("var out = null; ctx.startRendering().then(function (b) { out = b; });");
        p.waitFor("out !== null", 30000, "the rendering");
        Check.near(0.1, p.number("out.getChannelData(0)[1200]"), 1e-6, "the audio of the processor");
        Check.equal(base, awaitThreads(WORKLET, base, 5000), "the worklet thread ended with the rendering");
        Check.equal(baseOffline, awaitThreads(OFFLINE, baseOffline, 5000), "and the thread that rendered");
        p.waitFor("ticks === 10", 20000, "the ten messages of the ten quanta, all delivered");
    }

    /**
     * Disposing a page ends the worklet threads and the rendering threads of all its contexts at once, and a second disposal is harmless; the scripts of the page do not run any more.
     * <p>
     * 페이지를 폐기하면 모든 컨텍스트의 worklet 스레드와 렌더링 스레드가 한꺼번에 끝나고, 두 번째 폐기는 해롭지 않다. 페이지의 스크립트는 더 이상 실행되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposingThePageEndsEverything() throws Exception {
        int base = threads(WORKLET);
        int baseRender = threads(RENDER);
        WorkletPage p = page("new AudioContext()");
        p.run("var ctx2 = new AudioContext(); var ready2 = false; ctx2.audioWorklet.addModule('tick.js').then(function () { var m = new AudioWorkletNode(ctx2, 'tick', {numberOfInputs: 0}); m.connect(ctx2.destination); ready2 = true; });");
        p.waitFor("ready2 && ticks > 5", 30000, "both contexts run");
        Check.equal(base + 2, threads(WORKLET), "a worklet thread for each context");
        Check.equal(baseRender + 2, threads(RENDER), "and a rendering thread");
        p.fixture.scripts.dispose();
        Check.equal(base, awaitThreads(WORKLET, base, 5000), "dispose() ends both worklet threads");
        Check.equal(baseRender, awaitThreads(RENDER, baseRender, 5000), "and both rendering threads");
        p.fixture.scripts.dispose();
        Check.equal(base, threads(WORKLET), "a second dispose() changes nothing");
    }

    /**
     * Twenty pages that load a worklet and run a processor, each disposed after a moment, leave no worklet thread, no rendering thread and no offline thread.
     * <p>
     * worklet 을 불러와 프로세서를 실행하다가 잠시 뒤 폐기한 페이지 스무 개는 worklet 스레드, 렌더링 스레드, 오프라인 스레드를 남기지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testManyPagesLeaveNoThreads() throws Exception {
        int base = threads(WORKLET);
        int baseRender = threads(RENDER);
        int baseOffline = threads(OFFLINE);
        // 페이지를 만들어 잠시 실행하고 폐기하기를 되풀이한다. 짝수 번째는 실시간, 홀수 번째는 오프라인이다.
        for (int i = 0; i < 20; i++) {
            WorkletPage p = page(i % 2 == 0 ? "new AudioContext()" : "new OfflineAudioContext(1, 128000, 8000)");
            if (i % 2 == 1) {
                p.run("ctx.startRendering();");
            }
            p.pumpFor(20);
            p.fixture.scripts.dispose();
        }
        Check.equal(base, awaitThreads(WORKLET, base, 10000), "no worklet thread is left");
        Check.equal(baseRender, awaitThreads(RENDER, baseRender, 10000), "no rendering thread is left");
        Check.equal(baseOffline, awaitThreads(OFFLINE, baseOffline, 10000), "no offline thread is left");
    }

    /**
     * What a processor posts to the port reaches the page only while the page lives: after the disposal the tasks that are still queued are dropped and nothing runs on the disposed page.
     * <p>
     * 프로세서가 포트에 보낸 것은 페이지가 살아 있는 동안에만 페이지에 닿는다: 폐기한 뒤에는 줄 서 있던 작업이 버려지고 폐기된 페이지에서는 아무것도 실행되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNothingReachesADisposedPage() throws Exception {
        WorkletPage p = page("new AudioContext()");
        p.waitFor("ticks > 20", 30000, "the processor runs");
        p.fixture.scripts.dispose();
        p.pumpFor(100);
        String after = p.fixture.scripts.evaluate("String(ticks)", "late");
        p.pumpFor(300);
        Check.equal(after, p.fixture.scripts.evaluate("String(ticks)", "later"), "no message was delivered after the disposal");
    }

    /**
     * Replacing the page while a module is being fetched: the worklet thread ends with the page (the request is cancelled or its result is dropped), within seconds although the server answers only after
     * a long delay, and the new page works.
     * <p>
     * 모듈을 가져오는 동안 페이지를 교체하면: worklet 스레드는 페이지와 함께 끝난다(요청이 취소되거나 결과가 버려진다). 서버가 한참 뒤에야 답해도 몇 초 안에 끝나며 새 페이지는 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReplacingThePageDuringAFetch() throws Exception {
        int base = threads(WORKLET);
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(300, 200);
            }
        });
        SwingWebPanel panel = holder[0];
        TestHttpServer server = new TestHttpServer();
        try {
            server.routeFixed("/slow.js", TestHttpServer.Response.ok("application/javascript", TICK).delay(6000));
            server.routeFixed("/first", TestHttpServer.Response.html("<!doctype html><body><script>var ctx = new OfflineAudioContext(1, 128, 8000); ctx.audioWorklet.addModule('/slow.js').then(function () {"
                    + "sessionStorage.setItem('first', 'loaded'); }, function (e) { sessionStorage.setItem('first', e.name); });</script></body>"));
            server.routeFixed("/second", TestHttpServer.Response.html("<!doctype html><body><script>sessionStorage.setItem('second', 'ok');</script></body>"));
            server.start();
            panel.setPage(server.url("/first"));
            Check.isTrue(server.awaitRequests(2, 20000), "the module was requested");
            Check.equal(base + 1, awaitThreads(WORKLET, base + 1, 5000), "the worklet thread waits for the module");
            long start = System.currentTimeMillis();
            panel.setPage(server.url("/second"));
            Check.equal(base, awaitThreads(WORKLET, base, 12000), "the worklet thread ended with the page");
            long took = System.currentTimeMillis() - start;
            System.out.println("[worklet] the worklet thread ended " + took + " ms after the page was replaced during a fetch (the server needed 6000 ms)");
            long end = System.currentTimeMillis() + 10000;
            // 새 페이지가 값을 저장할 때까지 기다린다.
            String second = null;
            while (second == null && System.currentTimeMillis() < end) {
                // 저장소 항목에서 second 키를 찾는다.
                for (StorageEntry entry : panel.getSessionStorage()) {
                    if ("second".equals(entry.getKey())) {
                        second = entry.getValue();
                    }
                }
                Thread.sleep(10);
            }
            Check.equal("ok", second, "the new page works");
        } finally {
            server.stop();
            panel.dispose();
        }
    }
}
