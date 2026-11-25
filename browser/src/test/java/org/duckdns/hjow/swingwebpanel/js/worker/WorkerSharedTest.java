package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * <code>SharedWorker</code> through the real component and a real HTTP server (plan item P20-09-b; HTML Standard "Shared workers and the
 * SharedWorker interface"): the global scope of a shared worker (<code>SharedWorkerGlobalScope</code>, <code>onconnect</code>, a
 * <code>connect</code> event with the inside port, no <code>postMessage</code>), the sharing of one worker by the same origin, script URL and
 * name (and not by another name or another component), state that survives between the connections, ports that are transferred to the
 * worker, the request layer of the component, the life cycle (the worker ends with its last owner and with its own <code>close()</code>), and
 * classic and module entry scripts, and constructor errors (a missing script, another origin, wrong options, too many shared workers).
 * <p>
 * 실제 컴포넌트와 실제 HTTP 서버로 확인하는 <code>SharedWorker</code>(계획 P20-09-b. HTML 표준 "Shared workers and the SharedWorker interface"): 공유
 * worker 의 전역 범위(<code>SharedWorkerGlobalScope</code>, <code>onconnect</code>, 안쪽 포트를 가진 <code>connect</code> 이벤트, <code>postMessage</code> 없음),
 * 같은 오리진, 스크립트 URL, 이름이 하나의 worker 를 공유함(다른 이름이나 다른 컴포넌트는 아님), 연결 사이에 유지되는 상태, worker 로 전달한 포트, 컴포넌트의
 * 요청 계층, 수명(마지막 소유자가 끝나거나 스스로 <code>close()</code> 하면 worker 가 끝난다), 생성자의 오류(없는 스크립트, 다른 오리진, 잘못된 옵션, 모듈
 * worker, 너무 많은 공유 worker).
 */
public class WorkerSharedTest {

    /**
     * Starts a shared module worker with a JSON dependency once for both connections.
     * <p>
     * JSON 의존성이 있는 공유 모듈 worker를 두 연결에 한 번만 실행한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSharedModuleWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/shared.js", WorkerTestSupport.js("import data from './settings' with {type:'json'};"
                    + "let count=0;onconnect=e=>e.ports[0].postMessage(data.name+ ++count);"));
            server.routeFixed("/settings", TestHttpServer.Response.ok("application/json", "{\"name\":\"shared\"}"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var opts={name:'one',type:'module'};"
                    + "var a=new SharedWorker('/shared.js',opts),b=new SharedWorker('/shared.js',opts);"
                    + "a.port.onmessage=e=>put('a',e.data);b.port.onmessage=e=>put('b',e.data);"
                    + "var c=new SharedWorker('/shared.js','one');c.onerror=e=>put('type',e.type);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("shared1", WorkerTestSupport.await(panel, "a"), "first connection");
            Check.equal("shared2", WorkerTestSupport.await(panel, "b"), "second connection");
            Check.equal("error", WorkerTestSupport.await(panel, "type"), "same key with a different worker type");
            Check.equal(1, server.count("/settings"), "one module instance fetched its JSON dependency");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The script of the shared worker of most cases: it counts the connections, answers <code>who</code> with the counters, broadcasts to all
     * its ports, echoes, fetches, accepts a transferred port and closes itself.
     * <p>
     * 대부분의 사례가 쓰는 공유 worker 의 스크립트: 연결을 세고, <code>who</code> 에 카운터로 답하고, 모든 포트로 방송하고, 되돌려 주고, 가져오고, 전달된 포트를
     * 받고, 스스로 닫는다.
     */
    private static final String SHARED_SCRIPT =
            "var count = 0; var ports = [];"
            + "var info = typeof postMessage + ':' + Object.prototype.toString.call(self) + ':' + self.name + ':' + typeof close;"
            + "onconnect = function (e) {"
            + " count++; var p = e.ports[0]; ports.push(p);"
            + " p.onmessage = function (m) {"
            + "  var d = m.data;"
            + "  if (d === 'who') { p.postMessage('count=' + count + ',ports=' + ports.length); return; }"
            + "  if (d === 'close') { self.close(); return; }"
            + "  if (d === 'fetch') { fetch('/data').then(function (r) { return r.text(); }).then(function (t) { p.postMessage('fetched ' + t); }); return; }"
            + "  if (d === 'chan') { m.ports[0].postMessage('worker got ports=' + m.ports.length); return; }"
            + "  if (typeof d === 'string' && d.indexOf('bc:') === 0) { for (var i = 0; i < ports.length; i++) { ports[i].postMessage('broadcast ' + d.substring(3)); } return; }"
            + "  p.postMessage('echo ' + d);"
            + " };"
            + " p.postMessage('hello#' + count + ':' + e.ports.length + ':' + (e.source === p) + ':' + e.type + ':' + JSON.stringify(e.data) + ':' + info);"
            + "};";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerSharedTest() {
    }

    /**
     * The first connection: the <code>connect</code> event (<code>ports</code>, <code>source</code>, empty <code>data</code>), the global scope
     * of a shared worker (no <code>postMessage</code>, the name of the constructor), and an echo through the port.
     * <p>
     * 첫 연결: <code>connect</code> 이벤트(<code>ports</code>, <code>source</code>, 빈 <code>data</code>), 공유 worker 의 전역 범위(<code>postMessage</code> 없음,
     * 생성자의 이름), 포트를 통한 되돌림.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConnectEventAndGlobalScope() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/s.js", WorkerTestSupport.js(SHARED_SCRIPT));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var sw = new SharedWorker('/s.js', 'shared-one'); var log = [];"
                    + "put('kinds', [typeof SharedWorker, (sw instanceof EventTarget), (sw.port instanceof MessagePort), (sw.port === sw.port), Object.prototype.toString.call(sw)].join(','));"
                    + "sw.port.onmessage = function (e) { log.push(e.data); if (log.length === 1) { sw.port.postMessage({ k: [1, 2] }); } else { put('log', log.join('|')); } };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("function,true,true,true,[object SharedWorker]", WorkerTestSupport.await(panel, "kinds"), "the interface");
            Check.equal("hello#1:1:true:connect:\"\":undefined:[object SharedWorkerGlobalScope]:shared-one:function|echo [object Object]", WorkerTestSupport.await(panel, "log"),
                    "the connect event, the global scope of a shared worker and the echo through the port");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The same origin, script URL and name give the one shared worker (two objects of one page, state shared through a broadcast to every
     * port), another name gives another worker with its own state, and each worker has one thread.
     * <p>
     * 같은 오리진, 스크립트 URL, 이름은 하나의 공유 worker 를 준다(한 페이지의 두 객체, 모든 포트로의 방송으로 공유되는 상태). 다른 이름은 자기 상태를 가진 다른
     * worker 를 주며 worker 마다 스레드 하나이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSameKeyIsOneWorkerAndOtherNameIsAnother() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/s.js", WorkerTestSupport.js(SHARED_SCRIPT));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var a = new SharedWorker('/s.js', 'n'); var b = new SharedWorker('/s.js', { name: 'n' }); var c = new SharedWorker('/s.js', 'other');"
                    + "var all = { a: a, b: b, c: c }; var hellos = 0; var got = {};"
                    + "function listen(k) { all[k].port.onmessage = function (e) {"
                    + "  if (e.data.indexOf('hello') === 0) { hellos++; if (hellos === 3) { a.port.postMessage('who'); b.port.postMessage('who'); c.port.postMessage('who'); } return; }"
                    + "  got[k] = got[k] ? got[k] + ';' + e.data : e.data;"
                    + "  if (e.data.indexOf('count') === 0 && !all.asked) { all.asked = true; a.port.postMessage('bc:hi'); }"
                    + "  if (got.a && got.b && got.c && got.a.indexOf('broadcast') > 0 && got.b.indexOf('broadcast') > 0) { put('result', [got.a, got.b, got.c].join('|')); } }; }"
                    + "listen('a'); listen('b'); listen('c');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("count=2,ports=2;broadcast hi|count=2,ports=2;broadcast hi|count=1,ports=1", WorkerTestSupport.await(panel, "result"),
                    "a and b are connected to the same worker (two ports, state shared), c has its own worker; the broadcast reached both ports of the shared one");
            Check.equal(before + 2, WorkerTestSupport.workerThreads(), "one thread per shared worker, not per connection");
        } finally {
            panel.dispose();
            server.stop();
        }
        Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the shared workers ended with the component");
    }

    /**
     * A shared worker lives as long as a page owns it: a new page gets a fresh worker (the old one ended with the old page and its thread is
     * gone), and a worker that closed itself is replaced by a new one for the next constructor.
     * <p>
     * 공유 worker 는 페이지가 소유하는 동안 산다: 새 페이지는 새 worker 를 얻고(이전 worker 는 이전 페이지와 함께 끝나 스레드가 없어졌다), 스스로 닫은 worker 는 다음
     * 생성자에서 새 worker 로 바뀐다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSharedWorkerLifeCycle() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/s.js", WorkerTestSupport.js(SHARED_SCRIPT));
            String ask = "var sw = new SharedWorker('/s.js', 'life'); sw.port.onmessage = function (e) { if (e.data.indexOf('hello') === 0) { sw.port.postMessage('who'); }"
                    + " else if (e.data.indexOf('count') === 0) { put(KEY, e.data); } };";
            server.routeFixed("/one", TestHttpServer.Response.html(WorkerTestSupport.page(ask.replace("KEY", "'one'"))));
            server.routeFixed("/two", TestHttpServer.Response.html(WorkerTestSupport.page(ask.replace("KEY", "'two'"))));
            server.routeFixed("/closing", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var first = new SharedWorker('/s.js', 'closing'); var phase = 0;"
                    + "first.port.onmessage = function (e) { if (e.data.indexOf('hello') === 0 && phase === 0) { phase = 1; first.port.postMessage('close');"
                    + " setTimeout(function () { var second = new SharedWorker('/s.js', 'closing'); second.port.onmessage = function (m) {"
                    + "  if (m.data.indexOf('hello') === 0) { second.port.postMessage('who'); } else { put('afterClose', m.data); } }; }, 300); } };")));
            server.start();
            panel.setPage(server.url("/one"));
            Check.equal("count=1,ports=1", WorkerTestSupport.await(panel, "one"), "the first page");
            Check.equal(before + 1, WorkerTestSupport.workerThreads(), "the shared worker has a thread");
            panel.setPage(server.url("/two"));
            Check.equal("count=1,ports=1", WorkerTestSupport.await(panel, "two"), "the next page has a fresh shared worker");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before + 1, 10000) <= before + 1, "the shared worker of the page before ended");
            panel.setPage(server.url("/closing"));
            Check.equal("count=1,ports=1", WorkerTestSupport.await(panel, "afterClose"), "a worker that closed itself is replaced by a new one");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before + 1, 10000) <= before + 1, "only the new worker is left");
            panel.clean();
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "clean() ended the shared worker");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Two components never share a shared worker, also not for the same origin, script URL and name (the registry belongs to the component).
     * <p>
     * 두 컴포넌트는 같은 오리진, 스크립트 URL, 이름이어도 공유 worker 를 공유하지 않는다(등록부는 컴포넌트에 속한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTwoComponentsDoNotShareAWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel one = WorkerTestSupport.newPanel();
        SwingWebPanel two = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/s.js", WorkerTestSupport.js(SHARED_SCRIPT));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var sw = new SharedWorker('/s.js', 'same'); sw.port.onmessage = function (e) { if (e.data.indexOf('hello') === 0) { sw.port.postMessage('who'); }"
                    + " else { put('who', e.data); } };")));
            server.start();
            one.setPage(server.url("/page"));
            two.setPage(server.url("/page"));
            Check.equal("count=1,ports=1", WorkerTestSupport.await(one, "who"), "the first component");
            Check.equal("count=1,ports=1", WorkerTestSupport.await(two, "who"), "the second component has a shared worker of its own");
            Check.equal(before + 2, WorkerTestSupport.workerThreads(), "two shared workers");
            one.dispose();
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before + 1, 10000) <= before + 1, "disposing one component ended only its shared worker");
        } finally {
            one.dispose();
            two.dispose();
            server.stop();
        }
        Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "no shared worker is left");
    }

    /**
     * A port that the page transfers to the shared worker arrives in <code>ports</code> of the message and works; the requests of the shared
     * worker go through the request layer of the component (user agent, additional headers); a broadcast channel connects the shared worker
     * with the page.
     * <p>
     * 페이지가 공유 worker 에 전달한 포트는 메시지의 <code>ports</code> 로 도착해 동작한다. 공유 worker 의 요청은 컴포넌트의 요청 계층(User-Agent, 추가 헤더)을
     * 거친다. 브로드캐스트 채널은 공유 worker 와 페이지를 이어 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTransferredPortRequestsAndBroadcast() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            panel.setUserAgent("shared-agent/7");
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Shared", "yes");
            panel.addHeaders(headers);
            server.routeFixed("/data", TestHttpServer.Response.ok("text/plain", "payload"));
            server.routeFixed("/s.js", WorkerTestSupport.js(SHARED_SCRIPT
                    + "var bc = new BroadcastChannel('shared-bc'); bc.onmessage = function (e) { bc.postMessage('shared heard ' + e.data); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var sw = new SharedWorker('/s.js', 'features'); var ch = new MessageChannel(); var asked = false;"
                    + "ch.port1.onmessage = function (e) { put('chan', e.data); };"
                    + "sw.port.onmessage = function (e) { if (e.data.indexOf('hello') === 0) { sw.port.postMessage('chan', [ch.port2]); sw.port.postMessage('fetch'); }"
                    + " else if (e.data.indexOf('fetched') === 0) { put('fetched', e.data); } };"
                    + "var bc = new BroadcastChannel('shared-bc'); bc.onmessage = function (e) { put('bc', e.data); };"
                    + "setTimeout(function () { bc.postMessage('from page'); }, 400);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("worker got ports=1", WorkerTestSupport.await(panel, "chan"), "the transferred port arrived in ports of the message and works");
            Check.equal("fetched payload", WorkerTestSupport.await(panel, "fetched"), "fetch in the shared worker");
            Check.equal("shared-agent/7", server.last("/data").header("User-Agent"), "the user agent of the component");
            Check.equal("yes", server.last("/data").header("X-Shared"), "the additional headers of the component");
            Check.equal("shared heard from page", WorkerTestSupport.await(panel, "bc"), "the broadcast channel connects the shared worker and the page");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The errors of the constructor: no argument, no <code>new</code>, a wrong <code>type</code>, a script of another origin
     * (<code>SecurityError</code>), a script that cannot be fetched (an <code>error</code> event at the object), options
     * that do not fit the shared worker that exists (an <code>error</code> event), and too many shared workers (<code>QuotaExceededError</code>).
     * <p>
     * 생성자의 오류: 인수 없음, <code>new</code> 없음, 잘못된 <code>type</code>, 다른 오리진의 스크립트(<code>SecurityError</code>),
     * 가져올 수 없는 스크립트(객체에 <code>error</code> 이벤트), 이미 있는 공유 worker와 맞지 않는 옵션(<code>error</code> 이벤트), 너무 많은 공유 worker
     * (<code>QuotaExceededError</code>).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConstructorErrors() throws Exception {
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            other.routeFixed("/s.js", WorkerTestSupport.js("1;"));
            other.start();
            server.routeFixed("/s.js", WorkerTestSupport.js(SHARED_SCRIPT));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "put('errors', [err(function () { new SharedWorker(); }), err(function () { SharedWorker('/s.js'); }),"
                    + " err(function () { new SharedWorker('/s.js', { type: 'bogus' }); }), err(function () { new SharedWorker('" + other.url("/s.js") + "'); }),"
                    + " err(function () { new SharedWorker('data:text/javascript,1'); }), err(function () { new SharedWorker('http://[bad/x.js'); })].join('|'));"
                    + "var missing = new SharedWorker('/missing.js'); missing.onerror = function (e) { put('missing', e.type + ':' + (e instanceof ErrorEvent)); };"
                    + "var base = new SharedWorker('/s.js', 'mm'); var misfit = new SharedWorker('/s.js', { name: 'mm', credentials: 'include' });"
                    + "misfit.onerror = function (e) { put('misfit', e.type); };"
                    + "var made = 0, quota = ''; var keep = [];"
                    + "try { for (var i = 0; i < 40; i++) { keep.push(new SharedWorker('/s.js', 'many' + i)); made++; } } catch (e) { quota = e.name + ':' + e.code; }"
                    + "put('quota', made + ':' + quota);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("TypeError:undefined|TypeError:undefined|TypeError:undefined|SecurityError:18|SecurityError:18|SyntaxError:12", WorkerTestSupport.await(panel, "errors"),
                    "constructor errors");
            Check.equal("error:false", WorkerTestSupport.await(panel, "missing"), "a script that cannot be fetched is a plain error event at the object");
            Check.equal("error", WorkerTestSupport.await(panel, "misfit"), "options that do not fit the existing shared worker are an error event");
            Check.equal(0, other.totalRequests(), "the script of another origin was never requested");
            String quota = WorkerTestSupport.await(panel, "quota");
            // 한도 16 개에서 'mm' 하나와, 아직 실패 정리가 끝나지 않았을 수 있는 없는 스크립트 하나를 뺀 15 또는 14 개를 만들고 거부된다.
            Check.isTrue(quota.equals("15:QuotaExceededError:22") || quota.equals("14:QuotaExceededError:22"), "the component refuses too many shared workers: " + quota);
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
        Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "no worker thread is left");
    }
}
