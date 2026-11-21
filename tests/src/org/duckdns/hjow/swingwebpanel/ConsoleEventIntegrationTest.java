package org.duckdns.hjow.swingwebpanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The console listener of the component used the way a host application uses it (plan items P24-01 to P24-05): a real component, a real
 * HTTP server, scripts that call <code>console.log</code> and <code>console.error</code> in every place a page runs script, and the
 * listener checked for what it gets (level, text, arguments, page, script, line, origin), where it gets it (the event dispatch thread), in
 * which order, and what it does not get (messages without listener, of an old page, after dispose, with JavaScript disabled).
 * <p>
 * 호스트 응용이 쓰는 방식 그대로 쓴 컴포넌트의 콘솔 리스너 시험(계획 P24-01~P24-05): 실제 컴포넌트, 실제 HTTP 서버, 페이지가 스크립트를 실행하는 모든
 * 곳에서 <code>console.log</code> 와 <code>console.error</code> 를 호출하는 스크립트. 리스너가 무엇을(수준, 문자열, 인수, 페이지, 스크립트, 줄, 주체),
 * 어디서(이벤트 디스패치 스레드), 어떤 순서로 받는지와 받지 않는 것(리스너 없이 쓴 메시지, 이전 페이지의 메시지, 폐기 뒤, JavaScript 를 끈 경우)을 확인한다.
 */
public class ConsoleEventIntegrationTest {

    /**
     * Collects the events, and whether each one arrived on the event dispatch thread.
     * <p>
     * 이벤트와, 각 이벤트가 이벤트 디스패치 스레드에서 도착했는지를 모은다.
     */
    private static final class Collector implements ConsoleListener {

        /** The events in arrival order. / 도착 순서대로의 이벤트. */
        final List<ConsoleEvent> events = new ArrayList<ConsoleEvent>();

        /** Whether every event arrived on the event dispatch thread. / 모든 이벤트가 이벤트 디스패치 스레드에서 도착했는지. */
        boolean allOnEdt = true;

        /**
         * Keeps the event.
         * <p>
         * 이벤트를 보관한다.
         *
         * @param event the event / 이벤트
         */
        public void consoleMessage(ConsoleEvent event) {
            synchronized (this) {
                events.add(event);
                allOnEdt = allOnEdt && SwingUtilities.isEventDispatchThread();
            }
        }

        /**
         * Returns the number of events so far.
         * <p>
         * 지금까지의 이벤트 수를 반환한다.
         *
         * @return the count / 개수
         */
        synchronized int size() {
            return events.size();
        }

        /**
         * Returns a copy of the events.
         * <p>
         * 이벤트의 복사본을 반환한다.
         *
         * @return the events / 이벤트
         */
        synchronized List<ConsoleEvent> copy() {
            return new ArrayList<ConsoleEvent>(events);
        }

        /**
         * Returns the messages of the events joined by "|".
         * <p>
         * 이벤트의 메시지를 "|" 로 이어 반환한다.
         *
         * @return the messages / 메시지
         */
        synchronized String messages() {
            StringBuilder sb = new StringBuilder();
            // 이벤트마다 메시지를 이어 붙인다.
            for (int i = 0; i < events.size(); i++) {
                sb.append(i > 0 ? "|" : "").append(events.get(i).getMessage());
            }
            return sb.toString();
        }
    }

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The component of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private SwingWebPanel panel;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ConsoleEventIntegrationTest() {
    }

    /**
     * Creates the server and the component.
     * <p>
     * 서버와 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        final SwingWebPanel[] made = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component on the EDT. / EDT 에서 컴포넌트를 만든다. */
            public void run() {
                made[0] = new SwingWebPanel();
                made[0].setSize(300, 180);
            }
        });
        panel = made[0];
    }

    /**
     * Disposes the component and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        panel.dispose();
        server.stop();
    }

    /**
     * Waits until the collector has at least a number of events.
     * <p>
     * 수집기에 이벤트가 일정 개수 이상 모일 때까지 기다린다.
     *
     * @param c the collector / 수집기
     * @param n the number / 개수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitEvents(Collector c, int n) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 목표 개수에 이를 때까지 짧게 쉬며 기다린다.
        while (c.size() < n && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(c.size() >= n, "expected " + n + " console events but got " + c.size() + ": " + c.messages());
    }

    /**
     * Registers a new collector on the component.
     * <p>
     * 컴포넌트에 새 수집기를 등록한다.
     *
     * @return the collector / 수집기
     */
    private Collector listen() {
        Collector c = new Collector();
        panel.addConsoleListener(c);
        return c;
    }

    /**
     * Serves a page with a script.
     * <p>
     * 스크립트가 든 페이지를 제공한다.
     *
     * @param path   the path / 경로
     * @param script the script text / 스크립트 텍스트
     */
    private void page(String path, String script) {
        server.routeFixed(path, TestHttpServer.Response.html("<!doctype html>\n<title>t</title>\n<script>\n" + script + "\n</" + "script>"));
    }

    /**
     * Loads a page of the server and waits until the component confirmed it.
     * <p>
     * 서버의 페이지를 불러 컴포넌트가 확정할 때까지 기다린다.
     *
     * @param path the path / 경로
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void load(String path) throws Exception {
        String url = server.url(path);
        panel.setPage(url);
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "confirmed page " + path);
    }

    /**
     * The messages of <code>console.log</code> and <code>console.error</code> reach the listener on the event dispatch thread with the
     * level, the formatted text, the argument texts, the document, the script and the place of the call, in the order they were written.
     * <p>
     * <code>console.log</code> 와 <code>console.error</code> 의 메시지가 수준, 서식을 적용한 문자열, 인수 문자열, 문서, 스크립트, 호출 위치와 함께 쓴
     * 순서대로 이벤트 디스패치 스레드에서 리스너에 도착한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLogAndErrorReachTheListener() throws Exception {
        page("/p", "console.log('hello', 42, {a: 1});\nconsole.error('boom %s %d', 'x', 7);");
        server.start();
        Collector c = listen();
        load("/p");
        awaitEvents(c, 2);
        ConsoleEvent a = c.copy().get(0);
        ConsoleEvent b = c.copy().get(1);
        Check.isTrue(c.allOnEdt, "listener runs on the EDT");
        Check.same(panel, a.getSource(), "source of the event is the component");
        Check.equal(ConsoleEvent.Kind.CONSOLE_CALL, a.getKind(), "kind");
        Check.equal(ConsoleEvent.Level.LOG, a.getLevel(), "log level");
        Check.equal("log", a.getMethod(), "method");
        Check.equal("hello 42 { a: 1 }", a.getMessage(), "message");
        Check.equal("[hello, 42, { a: 1 }]", a.getArguments().toString(), "arguments");
        Check.equal(ConsoleEvent.Origin.DOCUMENT, a.getOrigin(), "origin");
        Check.equal(server.url("/p"), a.getDocumentUrl(), "document");
        Check.equal(server.url("/p"), a.getScriptUrl(), "an inline script has the URL of its document");
        Check.isTrue(a.getLineNumber() > 0 && a.getColumnNumber() > 0, "place of the call: " + a.getLineNumber() + ":" + a.getColumnNumber());
        Check.equal(ConsoleEvent.Level.ERROR, b.getLevel(), "error level");
        Check.equal("error", b.getMethod(), "method of the second");
        Check.equal("boom x 7", b.getMessage(), "format string applied");
        Check.equal("[boom %s %d, x, 7]", b.getArguments().toString(), "arguments are not formatted");
        Check.equal(a.getLineNumber() + 1, b.getLineNumber(), "the second call is one line below");
        Check.equal(a.getSequence() + 1, b.getSequence(), "serial numbers grow by one");
        Check.isTrue(b.getTimestamp() >= a.getTimestamp(), "timestamps do not go back");
        final ConsoleEvent first = a;
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            /** Tries to change the arguments of an event. / 이벤트의 인수를 바꿔 본다. */
            public void run() throws Exception {
                first.getArguments().add("x");
            }
        }, "the arguments of an event are read-only");
    }

    /**
     * Every console method that writes something notifies, with the level of the Console Standard; <code>assert</code> only when it fails.
     * <p>
     * 무언가를 쓰는 모든 콘솔 메서드가 Console Standard 의 수준으로 통지하고, <code>assert</code> 는 실패할 때만 통지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryConsoleMethodNotifiesWithItsLevel() throws Exception {
        page("/m", "console.info('i'); console.warn('w'); console.debug('d'); console.assert(true, 'quiet'); console.assert(false, 'fails %d', 1);\n"
                + "console.table([1]); console.dir({k: 1}); console.group('g'); console.log('inside'); console.groupEnd(); console.count('c');\n"
                + "console.time('t'); console.timeEnd('t'); console.log('last');");
        server.start();
        Collector c = listen();
        load("/m");
        awaitEvents(c, 11);
        List<ConsoleEvent> e = c.copy();
        StringBuilder sb = new StringBuilder();
        // 이벤트마다 "메서드:수준" 을 모아 기대와 비교한다.
        for (int i = 0; i < 11; i++) {
            sb.append(i > 0 ? " " : "").append(e.get(i).getMethod()).append(':').append(e.get(i).getLevel());
        }
        Check.equal("info:INFO warn:WARN debug:DEBUG assert:ERROR table:LOG dir:LOG group:LOG log:LOG count:LOG timeEnd:LOG log:LOG", sb.toString(),
                "methods and levels; the passing assert wrote nothing");
        Check.equal("Assertion failed: fails 1", e.get(3).getMessage(), "failed assert");
        Check.equal("  inside", e.get(7).getMessage(), "indented by the open group");
    }

    /**
     * Registration works like other Swing listeners: nothing is delivered without a listener, the same listener added twice is notified
     * twice, one removal removes one registration, the list is a copy, removing the hyperlink events leaves the console listeners alone,
     * and a null listener is refused.
     * <p>
     * 등록은 다른 Swing 리스너와 같다: 리스너가 없으면 아무것도 전달하지 않고, 같은 리스너를 두 번 등록하면 두 번 통지하고, 제거 한 번은 등록 하나를
     * 제거하며, 목록은 복사본이고, 하이퍼링크 이벤트를 제거해도 콘솔 리스너는 그대로이며, null 리스너는 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRegistrationFollowsSwingRules() throws Exception {
        final Collector late = new Collector();
        // 리스너 없이 쓴 메시지는 나중에 등록한 리스너에 전달되지 않는다. 내용이 바뀔 때까지 짧게 쉬며 기다린다.
        panel.setContent("<script>console.log('before')</script>", "text/html");
        long end = System.currentTimeMillis() + 5000;
        // 원문이 컴포넌트에 반영될 때까지 짧게 쉬며 기다린다.
        while (!"<script>console.log('before')</script>".equals(panel.getContent()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(300);
        panel.addConsoleListener(late);
        panel.addConsoleListener(late);
        Check.equal(2, panel.getConsoleListeners().length, "two registrations");
        ConsoleListener[] copy = panel.getConsoleListeners();
        copy[0] = null;
        Check.equal(2, panel.getConsoleListeners().length, "the list is a copy");
        Check.notNull(panel.getConsoleListeners()[0], "changing the copy changed nothing");
        panel.removeHyperlinkEvents();
        Check.equal(2, panel.getConsoleListeners().length, "removeHyperlinkEvents keeps console listeners");
        panel.setContent("<script>console.log('one')</script>", "text/html");
        awaitEvents(late, 2);
        Check.equal("one|one", late.messages(), "twice, and nothing from before the registration");
        panel.removeConsoleListener(late);
        Check.equal(1, panel.getConsoleListeners().length, "one removal removes one registration");
        panel.setContent("<script>console.log('two')</script>", "text/html");
        awaitEvents(late, 3);
        Thread.sleep(200);
        Check.equal("one|one|two", late.messages(), "once after one removal");
        panel.removeConsoleListener(new Collector());
        Check.equal(1, panel.getConsoleListeners().length, "an unknown listener is ignored");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Adds null. / null 을 등록한다. */
            public void run() throws Exception {
                panel.addConsoleListener(null);
            }
        }, "null add");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Removes null. / null 을 제거한다. */
            public void run() throws Exception {
                panel.removeConsoleListener(null);
            }
        }, "null remove");
    }

    /**
     * A listener that throws does not stop the other listeners or the page.
     * <p>
     * 예외를 던지는 리스너는 다른 리스너나 페이지를 멈추지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailingListenerDoesNotAffectOthers() throws Exception {
        page("/f", "console.log('a'); console.log('b'); sessionStorage.setItem('done', 'yes');");
        server.start();
        final int[] failures = new int[1];
        final Thread.UncaughtExceptionHandler[] old = new Thread.UncaughtExceptionHandler[1];
        // EDT 의 처리되지 않은 예외 처리기를 바꿔 리스너의 예외가 로그를 어지럽히지 않고 세어지게 한다(시험이 끝나면 되돌린다).
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Counts the exceptions of listeners on the EDT. / EDT 에서 리스너의 예외를 센다. */
            public void run() {
                old[0] = Thread.currentThread().getUncaughtExceptionHandler();
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    /** Counts the exception. / 예외를 센다. */
                    public void uncaughtException(Thread t, Throwable e) {
                        failures[0]++;
                    }
                });
            }
        });
        try {
            panel.addConsoleListener(new ConsoleListener() {
                /** Always fails. / 항상 실패한다. */
                public void consoleMessage(ConsoleEvent event) {
                    throw new IllegalStateException("listener failure");
                }
            });
            Collector c = listen();
            load("/f");
            awaitEvents(c, 2);
            Check.equal("a|b", c.messages(), "the second listener got both messages");
            Check.equal(2, failures[0], "the failures reached the thread's handler");
            Check.equal("yes", storage("done"), "the script ran to its end");
        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Restores the handler of the EDT. / EDT 의 처리기를 되돌린다. */
                public void run() {
                    Thread.currentThread().setUncaughtExceptionHandler(old[0]);
                }
            });
        }
    }

    /**
     * Reads a value of the session storage.
     * <p>
     * 세션 저장소의 값을 읽는다.
     *
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private String storage(String key) {
        Collection<StorageEntry> all = panel.getSessionStorage();
        // 키가 같은 항목을 찾는다.
        for (StorageEntry e : all) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * The browser's own reports of script problems notify at the error level with a kind that tells them from <code>console.error</code>: an
     * uncaught exception, a promise that nobody handled (not one that got a handler in time), and a script that could not be loaded; the
     * dialog functions notify too, at the info level.
     * <p>
     * 스크립트 문제에 대한 브라우저 자신의 보고는 <code>console.error</code> 와 구별되는 종류로 오류 수준에 통지한다: 잡히지 않은 예외, 아무도 처리하지 않은
     * promise(제때 처리기를 얻은 것은 제외), 가져오지 못한 스크립트. 대화 상자 함수도 info 수준으로 통지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReportsOfTheBrowserHaveTheirOwnKinds() throws Exception {
        server.routeFixed("/q", TestHttpServer.Response.html("<!doctype html>\n<title>t</title>\n<script src=\"/missing.js\"></" + "script>\n<script>\n"
                + "alert('hi'); confirm('sure?'); prompt('name?');\n"
                + "Promise.reject(new Error('nobody handles me'));\n"
                + "Promise.reject(new Error('handled in time')).catch(function () {});\n"
                + "setTimeout(function () { throw new Error('uncaught in timer'); }, 5);\n"
                + "</" + "script>"));
        server.routeFixed("/missing.js", new TestHttpServer.Response(404));
        server.start();
        Collector c = listen();
        load("/q");
        awaitEvents(c, 6);
        Thread.sleep(300);
        List<ConsoleEvent> e = c.copy();
        StringBuilder kinds = new StringBuilder();
        // 이벤트마다 종류와 수준을 모아 기대와 비교한다.
        for (int i = 0; i < e.size(); i++) {
            kinds.append(i > 0 ? " " : "").append(e.get(i).getKind()).append(':').append(e.get(i).getLevel());
        }
        Check.equal("HOST_ERROR:ERROR DIALOG:INFO DIALOG:INFO DIALOG:INFO UNHANDLED_REJECTION:ERROR UNCAUGHT_EXCEPTION:ERROR", kinds.toString(),
                "kinds and levels: " + c.messages());
        Check.contains(e.get(0).getMessage(), "missing.js", "the script that could not be loaded");
        Check.equal("alert", e.get(1).getMethod(), "alert");
        Check.equal("hi", e.get(1).getMessage(), "alert message");
        Check.equal("confirm", e.get(2).getMethod(), "confirm");
        Check.equal("prompt", e.get(3).getMethod(), "prompt");
        Check.contains(e.get(4).getMessage(), "nobody handles me", "the unhandled rejection");
        Check.contains(e.get(5).getMessage(), "uncaught in timer", "the uncaught exception of a timer");
        Check.isFalse(c.messages().contains("handled in time"), "handled rejections are silent");
    }

    /**
     * Messages come from every place that runs script: an external script (with its own URL and line), an event handler attribute, a
     * timer, a promise job and a module. Order: what the page wrote synchronously comes first, then the microtask, then the timer.
     * <p>
     * 메시지는 스크립트를 실행하는 모든 곳에서 온다: 외부 스크립트(자기 URL 과 줄), 이벤트 핸들러 속성, 타이머, promise 작업, 모듈. 순서: 페이지가 동기적으로
     * 쓴 것이 먼저이고, 그 다음 마이크로태스크, 그 다음 타이머.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryPlaceThatRunsScriptNotifies() throws Exception {
        server.routeFixed("/s.js", TestHttpServer.Response.ok("application/javascript", "\n\nconsole.log('external');"));
        server.routeFixed("/h", TestHttpServer.Response.html("<!doctype html>\n<title>t</title>\n<body onload=\"console.log('onload')\">\n"
                + "<script src=\"/s.js\"></" + "script>\n<script type=\"module\">console.log('module');</" + "script>\n<script>\n"
                + "console.log('sync');\nPromise.resolve().then(function () { console.log('micro'); });\n"
                + "setTimeout(function () { console.log('timer'); }, 150);\n</" + "script>"));
        server.start();
        Collector c = listen();
        load("/h");
        awaitEvents(c, 6);
        List<ConsoleEvent> e = c.copy();
        Check.equal("external|sync|micro|module|onload|timer", c.messages(), "order of the sources: a module is deferred, so it runs after the parser-blocking scripts and their microtasks");
        Check.equal(server.url("/s.js"), e.get(0).getScriptUrl(), "external script URL");
        Check.equal(3, e.get(0).getLineNumber(), "line in the external script");
        Check.equal(server.url("/h"), e.get(0).getDocumentUrl(), "document of the external script");
        Check.isTrue(c.allOnEdt, "all on the EDT");
    }

    /**
     * The messages of a frame and of a worker are marked with who wrote them, and name the document that owns them.
     * <p>
     * 프레임과 worker 의 메시지는 누가 썼는지 표시되고 그것을 가진 문서를 가리킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFramesAndWorkersAreMarked() throws Exception {
        server.routeFixed("/frame", TestHttpServer.Response.html("<script>console.log('in frame')</" + "script>"));
        server.routeFixed("/w.js", TestHttpServer.Response.ok("application/javascript", "console.log('in worker'); console.error('worker error');"));
        server.routeFixed("/top", TestHttpServer.Response.html("<!doctype html><title>t</title><iframe src=\"/frame\"></iframe><script>\n"
                + "console.log('in top'); var w = new Worker('/w.js');\n</" + "script>"));
        server.start();
        Collector c = listen();
        load("/top");
        awaitEvents(c, 4);
        ConsoleEvent top = null;
        ConsoleEvent frame = null;
        ConsoleEvent worker = null;
        ConsoleEvent workerError = null;
        // 메시지로 이벤트를 찾아 주체별로 나눈다.
        for (ConsoleEvent e : c.copy()) {
            if ("in top".equals(e.getMessage())) {
                top = e;
            } else if ("in frame".equals(e.getMessage())) {
                frame = e;
            } else if ("in worker".equals(e.getMessage())) {
                worker = e;
            } else if ("worker error".equals(e.getMessage())) {
                workerError = e;
            }
        }
        Check.notNull(top, "message of the top document");
        Check.notNull(frame, "message of the frame");
        Check.notNull(worker, "message of the worker");
        Check.notNull(workerError, "error of the worker");
        Check.equal(ConsoleEvent.Origin.DOCUMENT, top.getOrigin(), "top");
        Check.equal(ConsoleEvent.Origin.FRAME, frame.getOrigin(), "frame");
        Check.equal(server.url("/frame"), frame.getDocumentUrl(), "document of the frame");
        Check.equal(ConsoleEvent.Origin.WORKER, worker.getOrigin(), "worker");
        Check.equal(server.url("/top"), worker.getDocumentUrl(), "the document that owns the worker");
        Check.equal(server.url("/w.js"), worker.getScriptUrl(), "script of the worker");
        Check.equal(ConsoleEvent.Level.ERROR, workerError.getLevel(), "level of the worker's error");
        Check.equal(ConsoleEvent.Origin.WORKER, workerError.getOrigin(), "origin of the worker's error");
        Check.isTrue(c.allOnEdt, "all on the EDT");
    }

    /**
     * The messages of a shared worker are marked as written by a shared worker, and name the document of the page that connected.
     * <p>
     * 공유 worker 의 메시지는 공유 worker 가 쓴 것으로 표시되고 연결한 페이지의 문서를 가리킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSharedWorkerIsMarked() throws Exception {
        server.routeFixed("/sw.js", TestHttpServer.Response.ok("application/javascript", "console.log('in shared worker');"));
        server.routeFixed("/shared", TestHttpServer.Response.html("<!doctype html><title>t</title><script>\n"
                + "var sw = new SharedWorker('/sw.js'); console.log('in page');\n</" + "script>"));
        server.start();
        Collector c = listen();
        load("/shared");
        awaitEvents(c, 2);
        ConsoleEvent shared = null;
        // 메시지로 공유 worker 의 이벤트를 찾는다.
        for (ConsoleEvent e : c.copy()) {
            if ("in shared worker".equals(e.getMessage())) {
                shared = e;
            }
        }
        Check.notNull(shared, "message of the shared worker");
        Check.equal(ConsoleEvent.Origin.SHARED_WORKER, shared.getOrigin(), "origin");
        Check.equal(server.url("/shared"), shared.getDocumentUrl(), "the page that owns the connection");
        Check.equal(server.url("/sw.js"), shared.getScriptUrl(), "script of the shared worker");
    }

    /**
     * A flood of messages never blocks the script or the event dispatch thread: while the listeners are held up, at most
     * {@link org.duckdns.hjow.swingwebpanel.core.ConsoleDispatcher#MAX_PENDING_EVENTS} messages wait, the rest are dropped and counted, the
     * waiting ones arrive in order, and one notice tells how many were lost.
     * <p>
     * 메시지가 쏟아져도 스크립트나 이벤트 디스패치 스레드를 막지 않는다: 리스너가 막혀 있는 동안 최대
     * {@link org.duckdns.hjow.swingwebpanel.core.ConsoleDispatcher#MAX_PENDING_EVENTS} 개만 기다리고 나머지는 버려 세며, 기다리던 것은 순서대로 도착하고,
     * 알림 하나가 몇 개를 잃었는지 알려 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFloodIsBoundedAndCounted() throws Exception {
        final int max = org.duckdns.hjow.swingwebpanel.core.ConsoleDispatcher.MAX_PENDING_EVENTS;
        page("/flood", "for (var i = 0; i < 5000; i++) { console.log('n' + i); }\nsessionStorage.setItem('done', 'yes');");
        server.start();
        Collector c = listen();
        final CountDownLatch blocked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        // 이벤트 디스패치 스레드를 붙잡아 리스너가 따라가지 못하는 상황을 만든다.
        SwingUtilities.invokeLater(new Runnable() {
            /** Holds the EDT until released. / 풀어 줄 때까지 EDT 를 붙잡는다. */
            public void run() {
                blocked.countDown();
                try {
                    release.await(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        Check.isTrue(blocked.await(10, TimeUnit.SECONDS), "the EDT is held");
        long start = System.currentTimeMillis();
        panel.setPage(server.url("/flood"));
        long end = start + 60000;
        // 스크립트가 끝났다는 표시가 저장될 때까지 기다린다(이벤트 디스패치 스레드는 아직 막혀 있다).
        while (storage("done") == null && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.equal("yes", storage("done"), "the script finished although the EDT was held");
        Check.equal(0, c.size(), "nothing was delivered while the EDT was held");
        release.countDown();
        awaitEvents(c, max + 1);
        Thread.sleep(300);
        List<ConsoleEvent> e = c.copy();
        Check.equal(max + 1, e.size(), "the waiting messages and one notice");
        // 기다리던 메시지가 쓰인 순서대로 도착했는지 확인한다.
        for (int i = 0; i < max; i++) {
            Check.equal("n" + i, e.get(i).getMessage(), "message " + i + " in order");
        }
        ConsoleEvent notice = e.get(max);
        Check.equal(ConsoleEvent.Kind.MESSAGES_DROPPED, notice.getKind(), "the notice");
        Check.equal(ConsoleEvent.Level.WARN, notice.getLevel(), "level of the notice");
        Check.equal(5000 - max, notice.getDroppedCount(), "how many were dropped");
        Check.equal(max, e.get(max - 1).getSequence(), "serial numbers start at one");
        Check.isTrue(notice.getSequence() > e.get(max - 1).getSequence() + 1, "the notice has a later number than the first dropped one");
    }

    /**
     * A message names the page that wrote it: a page that was left cannot write to the listener any more, and the new page's messages
     * name the new page.
     * <p>
     * 메시지는 그것을 쓴 페이지를 가리킨다: 떠난 페이지는 더 이상 리스너에 쓸 수 없고, 새 페이지의 메시지는 새 페이지를 가리킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMessageOfALeftPageNeverAppearsAsTheNewPages() throws Exception {
        page("/a", "console.log('A first'); setTimeout(function () { console.log('A late'); }, 700);");
        page("/b", "console.log('B first');");
        server.start();
        Collector c = listen();
        load("/a");
        awaitEvents(c, 1);
        load("/b");
        awaitEvents(c, 2);
        Thread.sleep(1200);
        Check.equal("A first|B first", c.messages(), "the timer of the left page did not write");
        List<ConsoleEvent> e = c.copy();
        Check.equal(server.url("/a"), e.get(0).getDocumentUrl(), "first page");
        Check.equal(server.url("/b"), e.get(1).getDocumentUrl(), "second page");
    }

    /**
     * <code>clean()</code> keeps the console listeners, <code>dispose()</code> removes them and drops what waits, a disposed component
     * refuses new listeners, and with JavaScript disabled no script runs, so nothing is written.
     * <p>
     * <code>clean()</code> 은 콘솔 리스너를 유지하고, <code>dispose()</code> 는 제거하고 기다리던 것을 버리며, 폐기된 컴포넌트는 새 리스너를 거부하고,
     * JavaScript 를 끄면 스크립트가 실행되지 않으므로 아무것도 쓰이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanDisposeAndJavascriptSetting() throws Exception {
        page("/x", "console.log('x');");
        server.start();
        Collector c = listen();
        panel.setJavascriptEnabled(false);
        load("/x");
        Thread.sleep(400);
        Check.equal(0, c.size(), "no script ran, so nothing was written");
        panel.setJavascriptEnabled(true);
        load("/x");
        awaitEvents(c, 1);
        panel.clean();
        Check.equal(1, panel.getConsoleListeners().length, "clean keeps the listener");
        load("/x");
        awaitEvents(c, 2);
        panel.dispose();
        Check.equal(0, panel.getConsoleListeners().length, "dispose removes the listeners");
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Adds a listener to a disposed component. / 폐기된 컴포넌트에 리스너를 등록한다. */
            public void run() throws Exception {
                panel.addConsoleListener(new Collector());
            }
        }, "add after dispose");
        panel.removeConsoleListener(c);
        Thread.sleep(200);
        Check.equal(2, c.size(), "nothing more arrived");
    }
}
