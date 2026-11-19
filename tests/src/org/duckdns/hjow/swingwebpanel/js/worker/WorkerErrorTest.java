package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The errors of workers through the real component and a real HTTP server (plan item P20-09-b): an uncaught exception of the worker script
 * (the <code>error</code> event of the <code>Worker</code> object is an <code>ErrorEvent</code> with message, file name and position), a syntax
 * error, an error inside the worker that is handled by <code>self.onerror</code> or an <code>error</code> listener and is then not forwarded, a
 * worker script that cannot be loaded (a plain <code>error</code> event), an error of a timer or a message handler (the worker lives on), the
 * errors of <code>new Worker()</code> (another origin is a <code>SecurityError</code>) and the workers that are not supported.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 서버로 확인하는 worker 의 오류(계획 P20-09-b): worker 스크립트의 잡히지 않은 예외(<code>Worker</code> 객체의
 * <code>error</code> 이벤트는 메시지, 파일 이름, 위치가 있는 <code>ErrorEvent</code>), 구문 오류, <code>self.onerror</code> 나 <code>error</code> 리스너가
 * 처리해 전달되지 않는 worker 안의 오류, 가져올 수 없는 worker 스크립트(일반 <code>error</code> 이벤트), 타이머나 메시지 핸들러의 오류(worker 는
 * 계속 산다), <code>new Worker()</code> 의 오류(다른 오리진은 <code>SecurityError</code>), 지원하지 않는 worker.
 */
public class WorkerErrorTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerErrorTest() {
    }

    /**
     * An exception that the worker script throws reaches the page as a cancelable <code>ErrorEvent</code> at the <code>Worker</code> object
     * (message, script URL, position, no error value); the worker is not ended by it and still answers a message.
     * <p>
     * worker 스크립트가 던진 예외는 페이지에서 <code>Worker</code> 객체의 취소 가능한 <code>ErrorEvent</code>(메시지, 스크립트 URL, 위치, 오류 값 없음)로
     * 도착한다. worker 는 이것으로 끝나지 않으며 메시지에 계속 답한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUncaughtErrorOfTheScriptReachesTheWorkerObject() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/boom.js", WorkerTestSupport.js(
                    "onmessage = function (e) { postMessage('alive:' + e.data); };"
                    + "throw new Error('boom');"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/boom.js');"
                    + "w.onerror = function (e) { put('err', [(e instanceof ErrorEvent), (e.message.indexOf('boom') >= 0), e.filename, (e.lineno >= 0),"
                    + "  (e.colno >= 0), (e.error === null), e.cancelable, e.type, (e.target === w)].join('|')); e.preventDefault(); w.postMessage('after'); };"
                    + "w.onmessage = function (e) { put('msg', e.data); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("true|true|" + server.url("/boom.js") + "|true|true|true|true|error|true", WorkerTestSupport.await(panel, "err"),
                    "an ErrorEvent with the message and the script URL");
            Check.equal("alive:after", WorkerTestSupport.await(panel, "msg"), "the worker survived the error of its script");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A syntax error of the worker script is an <code>ErrorEvent</code> of the worker object with a message and the position of the error.
     * <p>
     * worker 스크립트의 구문 오류는 메시지와 오류 위치가 있는 worker 객체의 <code>ErrorEvent</code> 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSyntaxErrorOfTheScript() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/bad.js", WorkerTestSupport.js("var ok = 1;\nvar x = ;\n"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/bad.js');"
                    + "w.onerror = function (e) { put('err', [(e instanceof ErrorEvent), (e.message.length > 0), e.filename, e.lineno].join('|')); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("true|true|" + server.url("/bad.js") + "|2", WorkerTestSupport.await(panel, "err"), "the position of the syntax error");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An error inside the worker goes first to <code>self.onerror</code> (five arguments; true handles it), then to the <code>error</code>
     * listeners of the global scope (<code>preventDefault()</code> handles it); only an error that nobody handled is forwarded to the page.
     * A handler of a message that throws does not end the worker.
     * <p>
     * worker 안의 오류는 먼저 <code>self.onerror</code>(인수 다섯 개, true 면 처리됨)로, 이어서 전역 범위의 <code>error</code> 리스너
     * (<code>preventDefault()</code> 로 처리됨)로 간다. 아무도 처리하지 않은 오류만 페이지로 전달된다. 던지는 메시지 핸들러는 worker 를 끝내지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorsHandledInsideTheWorkerAreNotForwarded() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "onerror = function (message, source, line, col, error) {"
                    + " postMessage('onerror:' + (message.indexOf('x1') >= 0) + ':' + (error instanceof Error) + ':' + (typeof line) + ':' + (typeof source)); return true; };"
                    + "function listener(e) { postMessage('listener:' + (e instanceof ErrorEvent) + ':' + (e.message.indexOf('x2') >= 0) + ':' + e.cancelable"
                    + " + ':' + (e.error instanceof Error)); e.preventDefault(); }"
                    + "onmessage = function (e) {"
                    + " if (e.data === 'fin') { postMessage('fin'); return; }"
                    + " if (e.data === 'x2') { onerror = null; addEventListener('error', listener); }"
                    + " if (e.data === 'x3') { removeEventListener('error', listener); }"
                    + " throw new Error(e.data);"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var log = []; var forwarded = [];"
                    + "w.onerror = function (e) { forwarded.push(e.message); };"
                    + "w.onmessage = function (e) { log.push(e.data); if (e.data === 'fin') { put('log', log.join(',')); put('forwarded', forwarded.length + ':' + forwarded.join(';')); } };"
                    + "w.postMessage('x1'); w.postMessage('x2'); w.postMessage('x3'); w.postMessage('fin');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("onerror:true:true:number:string,listener:true:true:true:true,fin", WorkerTestSupport.await(panel, "log"),
                    "onerror got five arguments, then the listener got an ErrorEvent with the thrown value");
            String forwarded = WorkerTestSupport.await(panel, "forwarded");
            Check.isTrue(forwarded.startsWith("1:") && forwarded.indexOf("x3") > 0 && forwarded.indexOf("x1") < 0 && forwarded.indexOf("x2") < 0,
                    "only the error that nobody handled reached the page: " + forwarded);
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A worker script that cannot be loaded (a 404) fires a plain <code>error</code> event (not an <code>ErrorEvent</code>) at the
     * <code>Worker</code> object, and the worker never runs (no thread is left).
     * <p>
     * 가져올 수 없는 worker 스크립트(404)는 <code>Worker</code> 객체에 (<code>ErrorEvent</code> 가 아닌) 일반 <code>error</code> 이벤트를 발생시키며
     * worker 는 실행되지 않는다(남는 스레드가 없다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMissingScriptFiresAPlainErrorEvent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/missing.js');"
                    + "w.onerror = function (e) { put('fail', [(e instanceof ErrorEvent), (e instanceof Event), e.type, (e.target === w)].join('|')); };"
                    + "w.onmessage = function () { put('unexpected', 'message'); }; w.postMessage('hi');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("false|true|error|true", WorkerTestSupport.await(panel, "fail"), "a plain error event");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the thread of the failed worker ended");
            Check.isNull(WorkerTestSupport.value(panel, "unexpected"), "no message came from a worker that never ran");
            Check.equal(1, server.count("/missing.js"), "the script was requested once");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An error of a timer callback is reported to the page and the interval keeps running; the worker still receives messages.
     * <p>
     * 타이머 콜백의 오류는 페이지에 보고되고 interval 은 계속 돈다. worker 는 계속 메시지를 받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorOfATimerIsReportedAndTheWorkerLivesOn() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var ticks = 0;"
                    + "var id = setInterval(function () { ticks++; if (ticks === 2) { throw new Error('tick two'); } if (ticks === 4) { clearInterval(id); postMessage('ticks:' + ticks); } }, 5);"
                    + "onmessage = function (e) { postMessage('echo:' + e.data); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var errors = [];"
                    + "w.onerror = function (e) { errors.push(e.message); put('errors', errors.length + ':' + (errors[0].indexOf('tick two') >= 0)); e.preventDefault(); };"
                    + "w.onmessage = function (e) { put('m_' + e.data.split(':')[0], e.data); };"
                    + "w.postMessage('hello');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("1:true", WorkerTestSupport.await(panel, "errors"), "the error of the timer callback was reported once");
            Check.equal("ticks:4", WorkerTestSupport.await(panel, "m_ticks"), "the interval kept running after the error");
            Check.equal("echo:hello", WorkerTestSupport.await(panel, "m_echo"), "and the worker still gets messages");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The errors of the constructor: no argument, no <code>new</code>, a wrong <code>type</code>, a script of another origin (another port, a
     * <code>data:</code> URL, <code>javascript:</code>, <code>file:</code>) is a <code>SecurityError</code>; a module worker fires an
     * <code>error</code> event (not supported); <code>SharedWorker</code> exists (tested in WorkerSharedTest).
     * <p>
     * 생성자의 오류: 인수 없음, <code>new</code> 없음, 잘못된 <code>type</code>, 다른 오리진(다른 포트, <code>data:</code> URL, <code>javascript:</code>,
     * <code>file:</code>)의 스크립트는 <code>SecurityError</code>. 모듈 worker 는 <code>error</code> 이벤트가 발생한다(지원하지 않음).
     * <code>SharedWorker</code> 는 WorkerSharedTest 에서 시험한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConstructorErrorsAndUnsupportedWorkers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            other.routeFixed("/w.js", WorkerTestSupport.js("postMessage('from other origin');"));
            other.start();
            server.routeFixed("/w.js", WorkerTestSupport.js("postMessage('ok');"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "put('errors', [err(function () { new Worker(); }), err(function () { Worker('/w.js'); }),"
                    + " err(function () { new Worker('/w.js', { type: 'bogus' }); }),"
                    + " err(function () { new Worker('" + other.url("/w.js") + "'); }), err(function () { new Worker('data:text/javascript,1'); }),"
                    + " err(function () { new Worker('javascript:1'); }), err(function () { new Worker('file:///x.js'); }),"
                    + " err(function () { new Worker('//127.0.0.1:1/x.js'); }), typeof SharedWorker].join('|'));"
                    + "var m = new Worker('/w.js', { type: 'module' });"
                    + "m.onerror = function (e) { put('module', [(e instanceof ErrorEvent), e.type].join('|')); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("TypeError:undefined|TypeError:undefined|TypeError:undefined|SecurityError:18|SecurityError:18|SecurityError:18|SecurityError:18|SecurityError:18|function",
                    WorkerTestSupport.await(panel, "errors"), "constructor errors; SharedWorker is not available");
            Check.equal("false|error", WorkerTestSupport.await(panel, "module"), "a module worker is refused with an error event");
            Check.equal(0, other.totalRequests(), "the script of another origin was never requested");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "no worker thread is left");
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
    }
}
