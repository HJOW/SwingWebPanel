package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The global scope of a dedicated worker through the real component and a real HTTP server (plan item P20-09-b): the script runs in a realm
 * of its own, so nothing of the page is visible (no <code>window</code>, <code>document</code>, storage, <code>history</code>, nodes, canvas
 * contexts, <code>Worker</code>, WebGL, <code>navigator.gpu</code>, no Java), what a worker has is there (<code>self</code>, <code>name</code>,
 * <code>location</code>, <code>navigator</code>, <code>importScripts</code>, the timers, <code>structuredClone</code>,
 * <code>performance.now()</code>, <code>fetch</code>, <code>XMLHttpRequest</code>), a global variable of the page is not seen by the worker
 * and the other way round, <code>importScripts</code> runs scripts synchronously in the global scope and reports the failures of its
 * scripts, and the timers of a worker keep their order and their cancellation.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 서버로 확인하는 전용 worker 의 전역 범위(계획 P20-09-b): 스크립트는 자기 realm 에서 실행되므로 페이지의 것은 아무것도 보이지
 * 않고(<code>window</code>, <code>document</code>, 저장소, <code>history</code>, 노드, 캔버스 컨텍스트, <code>Worker</code>, WebGL,
 * <code>navigator.gpu</code>, Java 없음), worker 가 가져야 할 것은 있으며(<code>self</code>, <code>name</code>, <code>location</code>,
 * <code>navigator</code>, <code>importScripts</code>, 타이머, <code>structuredClone</code>, <code>performance.now()</code>, <code>fetch</code>,
 * <code>XMLHttpRequest</code>), 페이지의 전역 변수는 worker 에 보이지 않고 그 반대도 마찬가지이며, <code>importScripts</code> 는 스크립트를
 * 전역 범위에서 동기적으로 실행하고 실패를 보고하며, worker 의 타이머는 순서와 취소를 지킨다.
 */
public class WorkerGlobalScopeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerGlobalScopeTest() {
    }

    /**
     * The worker cannot reach the page: every page object is missing, a page variable is not visible, and a worker variable is not visible
     * to the page; the members of a worker global scope are present and the user agent of the component is the one of the worker.
     * <p>
     * worker 는 페이지에 닿을 수 없다: 페이지의 모든 객체가 없고, 페이지 변수는 보이지 않으며, worker 변수도 페이지에 보이지 않는다. worker 전역 범위의
     * 멤버는 있고 worker 의 User-Agent 는 컴포넌트의 것이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTheWorkerSeesNoPageObjects() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            panel.setUserAgent("worker-test-agent/9");
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var workerVar = 'w';"
                    + "var names = ['window', 'document', 'localStorage', 'sessionStorage', 'history', 'alert', 'confirm', 'prompt', 'open', 'frames', 'parent',"
                    + " 'top', 'Node', 'Element', 'HTMLElement', 'Document', 'DOMParser', 'XMLSerializer', 'Image', 'Audio', 'Option', 'MutationObserver',"
                    + " 'CanvasRenderingContext2D', 'HTMLCanvasElement', 'WebGLRenderingContext', 'WebGL2RenderingContext', 'Worker', 'SharedWorker',"
                    + " 'Range', 'Selection', 'getComputedStyle', 'requestAnimationFrame', 'MouseEvent', 'KeyboardEvent', 'MessageChannel_not', 'Packages', 'java',"
                    + " 'javax', 'Java', 'Storage', 'screen', 'matchMedia', 'getSelection', 'postMessage_page_only'];"
                    + "var missing = [];"
                    + "for (var i = 0; i < names.length; i++) { if (typeof self[names[i]] !== 'undefined') { missing.push(names[i]); } }"
                    + "var present = ['self', 'name', 'location', 'navigator', 'importScripts', 'setTimeout', 'clearTimeout', 'setInterval', 'clearInterval',"
                    + " 'queueMicrotask', 'structuredClone', 'performance', 'fetch', 'XMLHttpRequest', 'Headers', 'Request', 'Response', 'FormData', 'Blob',"
                    + " 'URL', 'URLSearchParams', 'TextEncoder', 'TextDecoder', 'AbortController', 'Event', 'EventTarget', 'ErrorEvent', 'MessageEvent',"
                    + " 'DOMException', 'close', 'postMessage', 'addEventListener', 'removeEventListener', 'dispatchEvent', 'onmessage', 'onerror',"
                    + " 'BroadcastChannel', 'JSON', 'Promise', 'Map', 'Symbol', 'Proxy', 'ArrayBuffer'].filter(function (n) { return typeof self[n] === 'undefined' && !(n in self); });"
                    + "postMessage({ leaked: missing.join(','), absent: present.join(','), selfIsThis: self === this && self === globalThis_ok(),"
                    + " agent: navigator.userAgent, name: self.name, href: location.href, origin: location.origin, pathname: location.pathname,"
                    + " gpu: typeof navigator.gpu, wvar: typeof pageVar, tag: Object.prototype.toString.call(self),"
                    + " isTarget: (self instanceof EventTarget), perf: typeof performance.now(), concurrency: navigator.hardwareConcurrency,"
                    + " protoName: Object.getPrototypeOf(self) === Object.getPrototypeOf(Object.getPrototypeOf(self)) });"
                    + "function globalThis_ok() { return (function () { return this; })(); }"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var pageVar = 'p';"
                    + "var w = new Worker('/w.js', { name: 'the-name' });"
                    + "w.onmessage = function (e) { var d = e.data; put('shape', [d.leaked, d.absent, d.selfIsThis, d.agent, d.name, d.href.indexOf('/w.js') > 0,"
                    + "  d.origin === location.origin, d.pathname, d.gpu, d.wvar, d.tag, d.isTarget, d.perf, d.concurrency].join('|')); put('pageSeesWorkerVar', typeof workerVar); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("||true|worker-test-agent/9|the-name|true|true|/w.js|undefined|undefined|[object DedicatedWorkerGlobalScope]|true|number|1",
                    WorkerTestSupport.await(panel, "shape"), "nothing of the page is visible; what a worker has is there");
            Check.equal("undefined", WorkerTestSupport.await(panel, "pageSeesWorkerVar"), "a variable of the worker is not a variable of the page");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>importScripts</code>: the scripts run in the global scope, in the order of the arguments and synchronously (the code after the
     * call sees what they defined), a relative URL resolves against the URL of the worker script, a script that imports a script works
     * (relative to its own URL), a failing fetch is a <code>NetworkError</code> and stops the import, an invalid URL is a
     * <code>SyntaxError</code> before anything is fetched, a script of another origin can be imported like with a script element but a data: URL cannot, and the user agent and the
     * additional headers of the component are sent.
     * <p>
     * <code>importScripts</code>: 스크립트는 전역 범위에서 인수 순서대로 동기적으로 실행되고(호출 뒤의 코드는 그것이 정의한 것을 본다), 상대 URL은 worker
     * 스크립트의 URL을 기준으로 해석되며, 스크립트를 가져오는 스크립트는 (자기 URL 기준으로) 동작하고, 가져오기 실패는 <code>NetworkError</code> 이고
     * 가져오기를 멈추며, 잘못된 URL은 아무것도 가져오기 전에 <code>SyntaxError</code> 이고, script 요소처럼 다른 오리진의 스크립트는 가져올 수 있으나 data: URL은
     * 가져올 수 없으며, 컴포넌트의 User-Agent 와 추가 헤더를 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImportScripts() throws Exception {
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            panel.setUserAgent("import-agent/1");
            java.util.Map<String, String> headers = new java.util.HashMap<String, String>();
            headers.put("X-Extra", "extra-value");
            panel.addHeaders(headers);
            other.routeFixed("/o.js", WorkerTestSupport.js("var fromOther = 1;"));
            other.start();
            server.routeFixed("/lib/a.js", WorkerTestSupport.js("var order = (typeof order === 'undefined' ? '' : order) + 'a'; var fromA = 'A';"));
            // 가져온 스크립트 안의 상대 URL도 worker 스크립트(/w.js)의 URL 기준으로 해석되므로 절대 경로로 쓴다.
            server.routeFixed("/lib/b.js", WorkerTestSupport.js("order += 'b'; importScripts('/lib/sub/c.js'); order += 'B'; this.viaThis = typeof self.fromA;"));
            server.routeFixed("/lib/sub/c.js", WorkerTestSupport.js("order += 'c'; var cUrl = location.href; importScripts('/lib/a.js');"));
            server.routeFixed("/lib/rel.js", WorkerTestSupport.js("importScripts('sub/c.js');"));
            server.routeFixed("/lib/bad.js", WorkerTestSupport.js("order += 'x'; throw new TypeError('bad import');"));
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var order = ''; function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "importScripts('lib/a.js', '/lib/b.js'); var afterCall = order;"
                    + "var results = [];"
                    + "results.push(err(function () { importScripts('lib/missing.js'); }));"
                    + "results.push(err(function () { importScripts('http://[bad/x.js'); }));"
                    + "var before = order; results.push(err(function () { importScripts('lib/a.js', 'http://[bad/x.js'); })); results.push(order === before ? 'nothing fetched' : 'fetched anyway');"
                    + "results.push(err(function () { importScripts('" + other.url("/o.js") + "'); }));"
                    + "results.push(err(function () { importScripts('lib/bad.js'); }));"
                    + "results.push(err(function () { importScripts(); }));"
                    + "results.push(err(function () { importScripts('data:text/javascript,var d=1'); }));"
                    + "results.push(err(function () { importScripts('lib/rel.js'); }));"
                    + "postMessage({ after: afterCall, order: order, fromA: fromA, viaThis: viaThis, cUrl: location.href.indexOf('/w.js') > 0, results: results.join('|'),"
                    + " type: typeof importScripts, retval: String(importScripts('lib/a.js')), other: typeof fromOther });"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js');"
                    + "w.onmessage = function (e) { var d = e.data; put('result', [d.after, d.order, d.fromA, d.viaThis, d.cUrl, d.results, d.type, d.retval, d.other].join(';')); };"
                    + "w.onerror = function (e) { put('workerError', e.message); };")));
            server.start();
            panel.setPage(server.url("/page"));
            String r = WorkerTestSupport.await(panel, "result");
            Check.equal("abcaB;abcaBx;A;string;true;NetworkError:19|SyntaxError:12|SyntaxError:12|nothing fetched|no error|TypeError:undefined|no error|NetworkError:19|NetworkError:19;"
                    + "function;undefined;number", r,
                    "scripts ran synchronously in the global scope, in order; relative URLs resolve against the worker script; failures are errors of the call");
            Check.equal(1, other.totalRequests(), "like a script element, importScripts may import a script of another origin");
            Check.equal("import-agent/1", server.last("/lib/a.js").header("User-Agent"), "the user agent of the component");
            Check.equal("extra-value", server.last("/lib/a.js").header("X-Extra"), "the additional headers of the component");
            Check.equal("import-agent/1", server.last("/w.js").header("User-Agent"), "the same for the script of the worker itself");
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
    }

    /**
     * The timers of a worker: <code>setTimeout</code> with extra arguments, ordering by delay and by creation, <code>clearTimeout</code>,
     * <code>setInterval</code> that clears itself, a cleared interval, a callback that sets another timer, <code>queueMicrotask</code>
     * before the timers, and a timer that was set in a message handler.
     * <p>
     * worker 의 타이머: 추가 인수가 있는 <code>setTimeout</code>, 지연과 생성 순서에 따른 실행 순서, <code>clearTimeout</code>, 스스로 해제하는
     * <code>setInterval</code>, 해제된 interval, 다른 타이머를 설정하는 콜백, 타이머보다 먼저인 <code>queueMicrotask</code>, 메시지 핸들러에서 설정한 타이머.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTimers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var log = [];"
                    + "setTimeout(function (a, b) { log.push('t600:' + a + b); }, 600, 'x', 'y');"
                    + "setTimeout(function () { log.push('t0a'); }, 0);"
                    + "var cleared = setTimeout(function () { log.push('CLEARED'); }, 5); clearTimeout(cleared);"
                    + "setTimeout(function () { log.push('t0b'); setTimeout(function () { log.push('nested'); }, 0); }, 0);"
                    + "queueMicrotask(function () { log.push('micro'); });"
                    + "Promise.resolve().then(function () { log.push('promise'); });"
                    + "var n = 0; var iv = setInterval(function () { n++; log.push('i' + n); if (n === 3) { clearInterval(iv); } }, 10);"
                    + "var never = setInterval(function () { log.push('NEVER'); }, 10); clearInterval(never);"
                    + "log.push('sync');"
                    + "var ids = [setTimeout(function () {}, 1), setTimeout(function () {}, 1)];"
                    + "setTimeout(function () { postMessage({ log: log.join(','), ids: (typeof ids[0]) + ':' + (ids[1] > ids[0]) }); }, 900);"
                    + "onmessage = function (e) { setTimeout(function () { postMessage({ late: e.data + ':' + log.length }); }, 20); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js');"
                    + "w.onmessage = function (e) { var d = e.data; if (d.log) { put('log', d.log + '|' + d.ids); w.postMessage('go'); } else { put('late', d.late); } };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("sync,micro,promise,t0a,t0b,nested,i1,i2,i3,t600:xy|number:true", WorkerTestSupport.await(panel, "log"),
                    "microtasks before timers, timers by delay then creation order, cleared timers never run");
            Check.isTrue(WorkerTestSupport.await(panel, "late").startsWith("go:"), "a timer set in a message handler fires");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Workers do not share anything: two workers that run the same script have their own global variables, and a message of one is not seen
     * by the other.
     * <p>
     * worker 는 아무것도 공유하지 않는다: 같은 스크립트를 실행하는 두 worker 는 각자의 전역 변수를 갖고, 한 worker 의 메시지는 다른 worker 에 보이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWorkersAreIndependentRealms() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var counter = 0; Object.prototype.polluted = 'yes'; Array.prototype.extra = 1;"
                    + "onmessage = function (e) { counter += e.data; postMessage(counter); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var a = new Worker('/w.js'); var b = new Worker('/w.js'); var got = [];"
                    + "a.onmessage = function (e) { got.push('a' + e.data); check(); }; b.onmessage = function (e) { got.push('b' + e.data); check(); };"
                    + "function check() { if (got.length === 3) { got.sort(); put('counters', got.join(',') + ';' + typeof ({}).polluted + ':' + typeof [].extra); } }"
                    + "a.postMessage(1); a.postMessage(10); b.postMessage(100);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("a1,a11,b100;undefined:undefined", WorkerTestSupport.await(panel, "counters"),
                    "each worker has its own global variables, and a prototype change of a worker does not reach the page");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
