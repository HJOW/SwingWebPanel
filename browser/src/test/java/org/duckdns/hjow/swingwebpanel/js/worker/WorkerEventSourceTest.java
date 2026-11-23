package org.duckdns.hjow.swingwebpanel.js.worker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * <code>EventSource</code> inside a dedicated worker: the worker has the interface and its stream goes through the same request service of the
 * component as the streams of the page (User-Agent, cookies, the referrer of the worker script); terminating the worker closes the connection.
 * <p>
 * 전용 worker 안의 <code>EventSource</code>: worker 에도 인터페이스가 있고 그 스트림은 페이지의 스트림과 똑같은 컴포넌트의 요청 서비스를 거친다(User-Agent,
 * 쿠키, worker 스크립트의 참조). worker 를 종료하면 연결이 닫힌다.
 */
public class WorkerEventSourceTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public WorkerEventSourceTest() {
    }

    /**
     * A worker receives the events of a stream through the request layer of the component and posts them to the page.
     * <p>
     * worker 는 컴포넌트의 요청 계층을 거쳐 스트림의 이벤트를 받고 페이지로 전한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAWorkerReadsAnEventStream() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            panel.setUserAgent("worker-sse/2");
            server.route("/events", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return new TestHttpServer.Response(200).header("Content-Type", "text/event-stream").streamedBy(new TestHttpServer.Streamer() {
                        public void produce(TestHttpServer.Sink sink) throws Exception {
                            sink.write("data: from the server\nid: 9\n\n");
                            sink.pause(60000);
                        }
                    });
                }
            });
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var es = new EventSource('/events');"
                            + "es.onmessage = function (e) { postMessage(['message', e.data, e.lastEventId, e.origin, es.readyState, typeof EventSource].join('|')); };"
                            + "es.onerror = function () { postMessage('error'); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); w.onmessage = function (e) { put('answer', e.data); };")).header("Set-Cookie", "sid=sse1; Path=/"));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("message|from the server|9|" + WorkerTestSupport.origin(server) + "|1|function", WorkerTestSupport.await(panel, "answer"),
                    "the worker got the event");
            TestHttpServer.Request r = server.last("/events");
            Check.equal("worker-sse/2", r.header("User-Agent"), "the user agent of the component");
            Check.equal("sid=sse1", r.header("Cookie"), "the cookie of the component");
            Check.equal(server.url("/w.js"), r.header("Referer"), "the referrer is the URL of the worker script");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Terminating the worker cancels its stream: the server sees the client leave and nothing reconnects.
     * <p>
     * worker 를 종료하면 스트림이 취소된다: 서버가 클라이언트가 떠나는 것을 보고 다시 연결하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTerminatingTheWorkerClosesTheStream() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        final CountDownLatch gone = new CountDownLatch(1);
        try {
            server.route("/events", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return new TestHttpServer.Response(200).header("Content-Type", "text/event-stream").streamedBy(new TestHttpServer.Streamer() {
                        public void produce(TestHttpServer.Sink sink) throws Exception {
                            try {
                                sink.write("data: hi\n\n");
                                // 쓰기가 실패하거나 서버가 멈출 때까지 짧은 간격으로 주석 줄을 쓴다.
                                while (sink.isRunning()) {
                                    sink.write(":ka\n\n");
                                    sink.pause(20);
                                }
                            } catch (IOException e) {
                                gone.countDown();
                                throw e;
                            }
                        }
                    });
                }
            });
            server.routeFixed("/w.js", WorkerTestSupport.js(
                    "var es = new EventSource('/events'); es.onmessage = function (e) { postMessage(e.data); };"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "var w = new Worker('/w.js'); w.onmessage = function (e) { put('answer', e.data); w.terminate(); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("hi", WorkerTestSupport.await(panel, "answer"), "the worker got the event and was terminated");
            Check.isTrue(gone.await(20, TimeUnit.SECONDS), "terminating the worker closed the connection of its stream");
            Thread.sleep(300);
            Check.equal(1, server.count("/events"), "and nothing reconnected");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
