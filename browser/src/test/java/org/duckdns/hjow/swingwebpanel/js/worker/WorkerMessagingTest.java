package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The messages between a page and its dedicated workers through the real component and a real HTTP server (plan item P20-09-b): the round trip
 * with the structured clone algorithm (objects, shared references, cycles, Map, Set, Date, RegExp, Error, typed arrays) and the transfer of an
 * ArrayBuffer, the order of the messages, the microtasks that run before the next message, messages that are posted before the worker script
 * ran, the shape of the <code>message</code> event, <code>DataCloneError</code> on both sides, and <code>self.close()</code>. The worker script
 * and the page are served over HTTP; the page reports through the sessionStorage of the component.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 서버로 확인하는 페이지와 전용 worker 사이의 메시지(계획 P20-09-b): 구조화 복제(객체, 공유 참조, 순환, Map, Set, Date, RegExp,
 * Error, 형식화 배열)를 거친 왕복과 ArrayBuffer 전달, 메시지 순서, 다음 메시지보다 먼저 실행되는 마이크로태스크, worker 스크립트가 실행되기 전에 보낸 메시지,
 * <code>message</code> 이벤트의 모양, 양쪽의 <code>DataCloneError</code>, <code>self.close()</code>. worker 스크립트와 페이지는 HTTP 로 제공하고 페이지는
 * 컴포넌트의 sessionStorage 로 보고한다.
 */
public class WorkerMessagingTest {

    /**
     * Loads JSON from classic and module workers under their own credentials and origin rules.
     * <p>
     * 일반 worker와 모듈 worker가 각자의 자격 증명과 오리진 규칙에 따라 JSON을 가져오는지 확인한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testJsonImportsInWorkers() throws Exception {
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            other.routeFixed("/closed", TestHttpServer.Response.ok("application/json", "0"));
            other.start();
            server.routeFixed("/classic.js", WorkerTestSupport.js("import('./data',{with:{type:'json'}})"
                    + ".then(m=>postMessage(m.default.value),e=>postMessage(e.name));"));
            server.routeFixed("/data", TestHttpServer.Response.ok("application/json", "{\"value\":\"classic\"}"));
            server.routeFixed("/module.js", TestHttpServer.Response.redirect(302, "/m/main.js"));
            server.routeFixed("/m/main.js", WorkerTestSupport.js("import data from './data' with {type:'json'};"
                    + "let reason;try {importScripts();}catch(e){reason=e.name;}"
                    + "try {await import('" + other.url("/closed") + "',{with:{type:'json'}});}catch(e)"
                    + "{postMessage([data.value,reason,e.name,location.pathname].join(':'));}"));
            server.routeFixed("/m/data", TestHttpServer.Response.ok("application/json", "{\"value\":\"module\"}"));
            server.routeFixed("/foreign.js", TestHttpServer.Response.redirect(302, other.url("/closed")));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var c=new Worker('/classic.js');c.onmessage=e=>put('classic',e.data);"
                    + "var m=new Worker('/module.js',{type:'module',credentials:'omit'});m.onmessage=e=>put('module',e.data);"
                    + "var f=new Worker('/foreign.js',{type:'module',credentials:'include'});f.onerror=e=>put('entry',e.type);"))
                    .header("Set-Cookie", "worker=42; Path=/"));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("classic", WorkerTestSupport.await(panel, "classic"), "classic dynamic JSON module");
            Check.equal("module:TypeError:TypeError:/m/main.js", WorkerTestSupport.await(panel, "module"), "static JSON, importScripts and CORS");
            Check.equal("error", WorkerTestSupport.await(panel, "entry"), "cross-origin redirect of a worker entry");
            Check.equal("worker=42", server.last("/data").header("Cookie"), "classic import uses same-origin credentials");
            Check.isNull(server.last("/module.js").header("Cookie"), "module worker entry omits credentials");
            Check.isNull(server.last("/m/data").header("Cookie"), "module worker dependency omits credentials");
            Check.equal(1, other.count("/closed"), "only the module dependency connects to the other origin");
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerMessagingTest() {
    }

    /**
     * Checks a resizable buffer with fixed and length-tracking views on both sides of a worker transfer.
     * <p>
     * Worker 양쪽에서 고정 길이 뷰와 길이 추적 뷰가 있는 가변 버퍼를 전달한다.
     *
     * @throws Exception on page or worker failure / 페이지 또는 Worker 실패
     */
    public void testResizableBufferTransferRoundTrip() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/rab.js", WorkerTestSupport.js("onmessage=e=>{const d=e.data;d.buf.resize(10);"
                    + "d.meta=[d.buf.maxByteLength,d.array.length,d.fixed.length,d.view.byteLength].join();"
                    + "d.array[1]=3.5;postMessage(d,[d.buf]);};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "const b=new ArrayBuffer(4,{maxByteLength:12}),a=new Float16Array(b,2);a[0]=1.5;"
                    + "const w=new Worker('/rab.js');w.onmessage=e=>{const d=e.data;d.buf.resize(12);"
                    + "put('resizable',d.meta+'|'+[d.buf.resizable,d.array.length,d.fixed.length,d.view.byteLength,"
                    + "d.array[0],d.array[1],d.array.buffer===d.view.buffer].join());};"
                    + "w.postMessage({buf:b,array:a,fixed:new Float16Array(b,2,1),view:new DataView(b,2)},[b]);"
                    + "put('resizableDetached',b.detached);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("12,4,1,8|true,5,1,10,1.5,3.5,true", WorkerTestSupport.await(panel, "resizable"),
                    "both realms preserve resizable metadata and buffer aliasing");
            Check.equal("true", WorkerTestSupport.await(panel, "resizableDetached"), "worker transfer detaches the source");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A message with every kind of value that the structured clone supports makes the round trip; the page cannot change what the worker
     * received by changing the original afterwards, and a transferred ArrayBuffer is detached at once, arrives with its bytes and comes back.
     * <p>
     * 구조화 복제가 지원하는 모든 종류의 값을 가진 메시지가 왕복한다. 페이지가 나중에 원본을 바꿔도 worker 가 받은 것은 바뀌지 않으며, 전달한 ArrayBuffer 는
     * 곧바로 분리되고 바이트와 함께 도착해 돌아온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMessageRoundTripWithStructuredClone() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "onmessage = function (e) {"
                    + " var d = e.data;"
                    + " if (d.kind === 'buffer') {"
                    + "  var u = new Uint8Array(d.buf), sum = 0;"
                    + "  for (var i = 0; i < u.length; i++) { sum += u[i]; }"
                    + "  postMessage({ kind: 'bufferBack', sum: sum, len: u.length, oldLen: d.buf.byteLength, buf: d.buf }, [d.buf]);"
                    + "  return;"
                    + " }"
                    + " postMessage({ kind: 'echo', str: d.str, num: d.num, neg0: (1 / d.zero < 0), nul: d.nul === null, und: ('und' in d) && d.und === undefined,"
                    + "  arr: d.arr.length + ':' + d.arr[1].x, shared: d.a === d.b, cyc: d.cyc.self === d.cyc,"
                    + "  map: (d.map instanceof Map) + ':' + d.map.get('k').v, set: (d.set instanceof Set) + ':' + d.set.size,"
                    + "  date: (d.date instanceof Date) + ':' + d.date.getTime(), re: (d.re instanceof RegExp) + ':' + d.re.source + ':' + d.re.flags,"
                    + "  err: (d.err instanceof RangeError) + ':' + d.err.message, typed: (d.typed instanceof Float64Array) + ':' + d.typed[1] });"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js');"
                    + "w.onmessage = function (e) {"
                    + " var d = e.data;"
                    + " if (d.kind === 'bufferBack') { put('bufBack', d.sum + ':' + d.len + ':' + d.oldLen + ':' + new Uint8Array(d.buf)[7] + ':' + d.buf.byteLength"
                    + "  + ':' + (d.buf instanceof ArrayBuffer)); }"
                    + " else { put('echo', JSON.stringify(d)); }"
                    + "};"
                    + "var shared = [1, 2]; var cyc = {}; cyc.self = cyc;"
                    + "var msg = { kind: 'echo', str: 'aü한', num: 1.5, zero: -0, nul: null, und: undefined, arr: [0, { x: 'X' }], a: shared, b: shared, cyc: cyc,"
                    + " map: new Map([['k', { v: 7 }]]), set: new Set([1, 2, 3]), date: new Date(86400000), re: /a+/gi, err: new RangeError('bad'),"
                    + " typed: new Float64Array([1, 2.5]) };"
                    + "w.postMessage(msg); msg.arr[1].x = 'CHANGED';"
                    + "var buf = new ArrayBuffer(8); var v = new Uint8Array(buf); for (var i = 0; i < 8; i++) { v[i] = i + 1; }"
                    + "w.postMessage({ kind: 'buffer', buf: buf }, [buf]); put('afterPost', buf.byteLength);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("{\"kind\":\"echo\",\"str\":\"aü한\",\"num\":1.5,\"neg0\":true,\"nul\":true,\"und\":true,\"arr\":\"2:X\",\"shared\":true,"
                    + "\"cyc\":true,\"map\":\"true:7\",\"set\":\"true:3\",\"date\":\"true:86400000\",\"re\":\"true:a+:gi\",\"err\":\"true:bad\",\"typed\":\"true:2.5\"}",
                    WorkerTestSupport.await(panel, "echo"), "every kind of value made the round trip, and the original was copied when it was posted");
            Check.equal("0", WorkerTestSupport.await(panel, "afterPost"), "the buffer of the transfer list is detached at once");
            Check.equal("36:8:8:8:8:true", WorkerTestSupport.await(panel, "bufBack"), "the buffer arrived with its bytes, was seen with its length before it was sent"
                    + " back, and returned with all of them");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Messages are delivered in the order in which they were posted, one task each; the microtasks of one message run before the next message
     * is delivered (and before the timers that were set with a delay of 0).
     * <p>
     * 메시지는 보낸 순서대로 하나씩 작업으로 전달된다. 한 메시지의 마이크로태스크는 다음 메시지가 전달되기 전에(그리고 지연 0 으로 설정한 타이머보다 먼저)
     * 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMessagesAreOrderedAndMicrotasksRunBeforeTheNextMessage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var log = [];"
                    + "onmessage = function (e) {"
                    + " if (e.data === 'report') { postMessage(log.join(',')); return; }"
                    + " log.push('m' + e.data);"
                    + " Promise.resolve().then(function () { log.push('p' + e.data); });"
                    + " setTimeout(function () { log.push('t' + e.data); }, 0);"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var got = [];"
                    + "w.onmessage = function (e) { put('log', e.data); };"
                    + "for (var i = 0; i < 4; i++) { w.postMessage(i); }"
                    + "w.postMessage('report'); got.push('posted'); put('sync', got.length + ':' + (typeof Worker));")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("m0,p0,m1,p1,m2,p2,m3,p3", WorkerTestSupport.await(panel, "log"), "messages in order, each followed by its microtask before the next one");
            Check.equal("1:function", WorkerTestSupport.await(panel, "sync"), "postMessage returned without delivering anything");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Messages that are posted before the worker script has run (it imports a slow script first) wait and are delivered, in order, after
     * the script ran and set its <code>onmessage</code>.
     * <p>
     * worker 스크립트가 실행되기 전에(먼저 느린 스크립트를 가져온다) 보낸 메시지는 기다렸다가, 스크립트가 실행되어 <code>onmessage</code> 를 지정한 뒤
     * 순서대로 전달된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMessagesPostedBeforeTheScriptRanAreQueued() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/lib.js", WorkerTestSupport.js("var libReady = 'yes';").delay(400));
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "importScripts('/lib.js');"
                    + "var seen = [];"
                    + "onmessage = function (e) { seen.push(e.data + ':' + libReady); if (seen.length === 3) { postMessage(seen.join(',')); } };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js');"
                    + "w.onmessage = function (e) { put('seen', e.data); };"
                    + "w.postMessage('a'); w.postMessage('b'); w.postMessage('c');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("a:yes,b:yes,c:yes", WorkerTestSupport.await(panel, "seen"), "the messages waited for the script and kept their order");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The <code>message</code> event of both sides: <code>MessageEvent</code>, trusted, the target and the current target, an empty origin, no
     * source, no ports; the handler property and <code>addEventListener</code> both receive it.
     * <p>
     * 양쪽의 <code>message</code> 이벤트: <code>MessageEvent</code>, 신뢰됨, target 과 currentTarget, 빈 origin, source 없음, ports 없음. 핸들러 프로퍼티와
     * <code>addEventListener</code> 가 모두 받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMessageEventShape() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var calls = [];"
                    + "addEventListener('message', function (e) { calls.push('listener'); });"
                    + "onmessage = function (e) {"
                    + " calls.push('handler');"
                    + " postMessage([(e.target === self), (e.currentTarget === self), JSON.stringify(e.origin), e.isTrusted, (e instanceof MessageEvent), e.type,"
                    + "  (e.source === null), e.ports.length, e.bubbles, calls.join('+')].join('|'));"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var order = [];"
                    + "w.addEventListener('message', function (e) { order.push('listener'); });"
                    + "w.onmessage = function (e) { order.push('handler'); put('workerShape', e.data);"
                    + " put('pageShape', [(e.target === w), (e.currentTarget === w), JSON.stringify(e.origin), e.isTrusted, (e instanceof MessageEvent), e.type,"
                    + "  (e.source === null), e.ports.length, e.bubbles, order.join('+'), (w instanceof EventTarget), (w instanceof Worker),"
                    + "  Object.prototype.toString.call(w)].join('|')); };"
                    + "w.postMessage('x');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("true|true|\"\"|true|true|message|true|0|false|listener+handler", WorkerTestSupport.await(panel, "workerShape"),
                    "the event inside the worker, and both the listener and the handler property were called in order");
            Check.equal("true|true|\"\"|true|true|message|true|0|false|listener+handler|true|true|[object Worker]", WorkerTestSupport.await(panel, "pageShape"),
                    "the event in the page");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A value that cannot be cloned (a function, a symbol, a DOM node), a duplicate in the transfer list, a missing argument: the
     * <code>postMessage</code> of the page and of the worker throw at the caller (<code>DataCloneError</code> or <code>TypeError</code>), and
     * nothing is sent.
     * <p>
     * 복제할 수 없는 값(함수, 심볼, DOM 노드), 전달 목록의 중복, 빠진 인수: 페이지와 worker 의 <code>postMessage</code> 는 호출자에게 던지며
     * (<code>DataCloneError</code> 또는 <code>TypeError</code>) 아무것도 보내지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDataCloneErrorsOnBothSides() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "var count = 0;"
                    + "onmessage = function (e) {"
                    + " count++;"
                    + " if (e.data === 'probe') {"
                    + "  var b = new ArrayBuffer(2);"
                    + "  postMessage([err(function () { postMessage(function () {}); }), err(function () { postMessage({ s: Symbol('x') }); }),"
                    + "   err(function () { postMessage(b, [b, b]); }), err(function () { postMessage(); }), err(function () { postMessage(1, [{}]); }),"
                    + "   'count=' + count].join('|'));"
                    + " }"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "function err(f) { try { f(); return 'no error'; } catch (e) { return e.name + ':' + e.code; } }"
                    + "var w = new Worker('/w.js'); var b = new ArrayBuffer(2);"
                    + "put('pageErrors', [err(function () { w.postMessage(function () {}); }), err(function () { w.postMessage({ n: document.documentElement }); }),"
                    + " err(function () { w.postMessage(Symbol('s')); }), err(function () { w.postMessage(b, [b, b]); }), err(function () { w.postMessage(); }),"
                    + " err(function () { w.postMessage(1, [{}]); }), 'detached=' + b.byteLength].join('|'));"
                    + "w.onmessage = function (e) { put('workerErrors', e.data); };"
                    + "w.postMessage('probe');")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("DataCloneError:25|DataCloneError:25|DataCloneError:25|DataCloneError:25|TypeError:undefined|DataCloneError:25|detached=2",
                    WorkerTestSupport.await(panel, "pageErrors"), "the page: nothing was cloned or transferred, the buffer is still attached");
            Check.equal("DataCloneError:25|DataCloneError:25|DataCloneError:25|TypeError:undefined|DataCloneError:25|count=1", WorkerTestSupport.await(panel, "workerErrors"),
                    "the worker; and only the one message that could be sent arrived");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>self.close()</code>: the current task finishes (a message that it posts afterwards still goes out), then the worker ends, its
     * thread is gone, and it receives nothing any more.
     * <p>
     * <code>self.close()</code>: 현재 작업은 끝까지 실행되고(그 뒤에 보낸 메시지도 나간다), 그 뒤 worker 가 끝나고 스레드가 사라지며 더 이상 아무것도 받지
     * 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSelfCloseEndsTheWorkerAfterTheCurrentTask() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var n = 0;"
                    + "onmessage = function (e) {"
                    + " n++;"
                    + " if (e.data === 'close') { postMessage('closing'); self.close(); setTimeout(function () { postMessage('timer after close'); }, 0);"
                    + "  postMessage('after close'); }"
                    + " else { postMessage('alive' + n); }"
                    + "};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var got = [];"
                    + "w.onmessage = function (e) { got.push(e.data); put('got', got.join(',')); };"
                    + "w.postMessage('hello'); w.postMessage('close'); w.postMessage('too late');")));
            server.start();
            panel.setPage(server.url("/page"));
            long end = System.currentTimeMillis() + 10000;
            // 닫히면서 보낸 메시지 셋이 모두 올 때까지 기다린다.
            while ((WorkerTestSupport.value(panel, "got") == null || WorkerTestSupport.value(panel, "got").indexOf("after close") < 0)
                    && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            Check.equal("alive1,closing,after close", WorkerTestSupport.value(panel, "got"), "the task that called close() finished; later tasks did not run");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the thread of a closed worker is gone");
            Thread.sleep(300);
            Check.equal("alive1,closing,after close", WorkerTestSupport.value(panel, "got"), "the timer and the message sent after close() never ran");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
