package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The request APIs inside a worker (plan item P20-09-b): <code>fetch</code> and <code>XMLHttpRequest</code> of a worker go through the very
 * same request layer of the component as those of the page, so the user agent, the additional headers, the cookies of the component, the
 * scheme limits and the cross-origin rules apply to them too; a worker that is terminated while a request is pending never runs the
 * callbacks, and its request is cancelled.
 * <p>
 * worker 안의 요청 API(계획 P20-09-b): worker 의 <code>fetch</code> 와 <code>XMLHttpRequest</code> 는 페이지와 똑같은 컴포넌트의 요청 계층을 거치므로
 * User-Agent, 추가 헤더, 컴포넌트의 쿠키, 스킴 제한, 교차 오리진 규칙이 그대로 적용된다. 요청이 진행 중일 때 종료된 worker 는 콜백을 실행하지 않으며 요청은
 * 취소된다.
 */
public class WorkerRequestTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WorkerRequestTest() {
    }

    /**
     * A same-origin <code>fetch</code> and an <code>XMLHttpRequest</code> of a worker carry the user agent, the additional headers and the
     * cookie of the component, and the referrer is the URL of the worker script; the answers reach the worker, and the response document of
     * <code>XMLHttpRequest</code> does not exist in a worker.
     * <p>
     * worker 의 같은 오리진 <code>fetch</code> 와 <code>XMLHttpRequest</code> 는 컴포넌트의 User-Agent, 추가 헤더, 쿠키를 싣고 참조는 worker 스크립트의
     * URL이다. 응답은 worker 에 도착하고 <code>XMLHttpRequest</code> 의 응답 문서는 worker 에 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetchAndXhrUseTheRequestLayerOfTheComponent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            panel.setUserAgent("request-agent/3");
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Extra", "extra-value");
            panel.addHeaders(headers);
            server.routeFixed("/data", TestHttpServer.Response.ok("application/json", "{\"hello\":\"world\"}"));
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var out = {};"
                    + "fetch('/data').then(function (r) { out.status = r.status; return r.json(); }).then(function (j) { out.json = j.hello;"
                    + " var x = new XMLHttpRequest(); x.open('GET', 'data?xhr=1'); x.responseType = 'document';"
                    + " x.onload = function () { out.xhrType = x.responseType; out.xhrText = x.responseText.length; out.xhrDoc = String(x.responseXML);"
                    + "  postMessage(out); };"
                    + " x.onerror = function () { postMessage({ error: 'xhr' }); }; x.send(); })"
                    + ".catch(function (e) { postMessage({ error: String(e) }); });"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); w.onmessage = function (e) { var d = e.data; put('answer', d.error ? 'ERR ' + d.error"
                    + " : [d.status, d.json, d.xhrType, d.xhrText, d.xhrDoc].join('|')); };")).header("Set-Cookie", "sid=abc123; Path=/"));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("200|world||17|null", WorkerTestSupport.await(panel, "answer"), "both requests were answered; responseType document is ignored in a worker (it stays empty) and there is no response document");
            TestHttpServer.Request fetched = server.last("/data");
            Check.equal("request-agent/3", fetched.header("User-Agent"), "the user agent of the component");
            Check.equal("extra-value", fetched.header("X-Extra"), "the additional headers of the component");
            Check.equal("sid=abc123", fetched.header("Cookie"), "the cookie of the component (credentials mode same-origin)");
            Check.equal(server.url("/w.js"), fetched.header("Referer"), "the referrer of a request of a worker is the URL of its script");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The cross-origin rules apply inside a worker: a response without <code>Access-Control-Allow-Origin</code> rejects the promise, a
     * response that allows the origin resolves it, the <code>Origin</code> header of the request is the origin of the page, and a request
     * with a header that needs a preflight request is preflighted. A <code>data:</code> URL is refused by the scheme limits.
     * <p>
     * 교차 오리진 규칙은 worker 안에서도 적용된다: <code>Access-Control-Allow-Origin</code> 이 없는 응답은 약속을 거부하고, 오리진을 허용하는 응답은 이행하며,
     * 요청의 <code>Origin</code> 헤더는 페이지의 오리진이고, 사전 요청이 필요한 헤더가 있는 요청은 사전 요청을 거친다. <code>data:</code> URL은 스킴
     * 제한으로 거부된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginRulesApplyInsideAWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            other.routeFixed("/closed", TestHttpServer.Response.ok("text/plain", "closed"));
            other.routeFixed("/open", TestHttpServer.Response.ok("text/plain", "open").header("Access-Control-Allow-Origin", "*"));
            other.route("/pre", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    TestHttpServer.Response r = TestHttpServer.Response.ok("text/plain", "pre");
                    r.header("Access-Control-Allow-Origin", "*");
                    r.header("Access-Control-Allow-Headers", "x-custom");
                    return r;
                }
            });
            other.start();
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var results = [];"
                    + "function tryFetch(name, url, init) { return fetch(url, init).then(function (r) { return r.text().then(function (t) { results.push(name + '=' + t); }); },"
                    + " function (e) { results.push(name + '=' + e.name); }); }"
                    + "tryFetch('closed', '" + other.url("/closed") + "').then(function () { return tryFetch('open', '" + other.url("/open") + "'); })"
                    + ".then(function () { return tryFetch('pre', '" + other.url("/pre") + "', { headers: { 'X-Custom': 'yes' } }); })"
                    + ".then(function () { return tryFetch('data', 'data:text/plain,hi'); })"
                    + ".then(function () { postMessage(results.join('|')); });"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); w.onmessage = function (e) { put('cors', e.data); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("closed=TypeError|open=open|pre=pre|data=TypeError", WorkerTestSupport.await(panel, "cors"),
                    "a cross-origin response without permission is refused, with permission it is read, and data: is blocked");
            Check.equal(WorkerTestSupport.origin(server), other.last("/open").header("Origin"), "the Origin of a request of a worker is the origin of its page");
            Check.equal(2, other.count("/pre"), "a custom header made a preflight request first");
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
    }

    /**
     * A worker that is terminated while its request is pending never runs the callbacks and its request is cancelled (the connection of the
     * server is closed); the page keeps running.
     * <p>
     * 요청이 진행 중일 때 종료된 worker 는 콜백을 실행하지 않고 그 요청은 취소된다(서버의 연결이 닫힌다). 페이지는 계속 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTerminateCancelsThePendingRequestOfTheWorker() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(4000));
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "fetch('/slow').then(function (r) { postMessage('ANSWER'); }, function (e) { postMessage('REJECTED'); });"
                    + "var x = new XMLHttpRequest(); x.open('GET', '/slow?x=1'); x.onload = function () { postMessage('XHR-LOAD'); };"
                    + "x.onerror = function () { postMessage('XHR-ERROR'); }; x.onabort = function () { postMessage('XHR-ABORT'); }; x.send();"
                    + "postMessage('started');"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); var got = [];"
                    + "w.onmessage = function (e) { got.push(e.data); if (e.data === 'started') { setTimeout(function () { w.terminate(); put('terminated', got.join(',')); }, 300);"
                    + " setTimeout(function () { put('after', got.join(',')); }, 1500); } };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("started", WorkerTestSupport.await(panel, "terminated"), "the worker had started both requests");
            Check.equal("started", WorkerTestSupport.await(panel, "after"), "no callback of the terminated worker ran");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 10000) <= before, "the worker thread ended");
            long end = System.currentTimeMillis() + 10000;
            // 취소된 요청의 서버 연결이 닫힐 때까지 기다린다.
            while (server.getOpenConnections() > 0 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.equal(0, server.getOpenConnections(), "the requests of the terminated worker were cancelled");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
