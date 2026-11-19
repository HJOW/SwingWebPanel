package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests <code>EventSource</code> in a real {@link SwingWebPanel} against real local HTTP servers: the events reach the page while the response is
 * still open (no buffering), the connection is reestablished with <code>Last-Event-ID</code>, the answers that must fail the connection do so, the
 * request layer (User-Agent, extra headers, cookies, CORS, schemes) applies, and the stream is cancelled when the page goes away. The page reports to
 * <code>sessionStorage</code>, which the test reads through {@link SwingWebPanel#getSessionStorage()}.
 * <p>
 * 실제 {@link SwingWebPanel} 에서 실제 로컬 HTTP 서버를 상대로 <code>EventSource</code> 를 시험한다: 응답이 아직 열려 있는 동안 이벤트가 페이지에 닿는다(버퍼링
 * 없음). 연결은 <code>Last-Event-ID</code> 로 다시 맺어지며, 연결을 실패시켜야 하는 답은 그렇게 한다. 요청 계층(User-Agent, 추가 헤더, 쿠키, CORS, 스킴)이
 * 적용되고, 페이지가 사라지면 스트림이 취소된다. 페이지는 <code>sessionStorage</code> 로 보고하고 시험은
 * {@link SwingWebPanel#getSessionStorage()} 로 읽는다.
 */
public class EventSourceIntegrationTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public EventSourceIntegrationTest() {
    }

    /**
     * Creates a panel on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 패널을 만든다.
     *
     * @return the panel / 패널
     * @throws Exception if creation fails / 만들지 못하면
     */
    private static SwingWebPanel newPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Reads a value that the page stored in its sessionStorage.
     * <p>
     * 페이지가 sessionStorage 에 저장한 값을 읽는다.
     *
     * @param panel the panel / 패널
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String value(SwingWebPanel panel, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until the page stored a value.
     * <p>
     * 페이지가 값을 저장할 때까지 기다린다.
     *
     * @param panel the panel / 패널
     * @param key   the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 중단되면
     */
    private static String await(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (value(panel, key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        StringBuilder known = new StringBuilder();
        // 실패했을 때 무엇이 저장되어 있었는지 알 수 있도록 항목을 모은다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            known.append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
        }
        Check.notNull(value(panel, key), "a value for " + key + " (the page stored: " + known + ")");
        return value(panel, key);
    }

    /**
     * Waits until the confirmed page of the panel is the URL.
     * <p>
     * 패널의 확정된 페이지가 그 URL 이 될 때까지 기다린다.
     *
     * @param panel the panel / 패널
     * @param url   the URL / URL
     * @throws Exception if interrupted / 중단되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 페이지가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Waits until the server has seen so many requests for a path.
     * <p>
     * 서버가 경로에 대해 그만큼의 요청을 볼 때까지 기다린다.
     *
     * @param server the server / 서버
     * @param path   the path / 경로
     * @param count  the count / 개수
     * @throws Exception if interrupted / 중단되면
     */
    private static void awaitRequests(TestHttpServer server, String path, int count) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 요청 수가 모자라면 짧게 쉬며 다시 확인한다.
        while (server.count(path) < count && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.isTrue(server.count(path) >= count, "the server saw " + count + " requests for " + path + " (saw " + server.count(path) + ")");
    }

    /**
     * Returns the first request that the server saw for a path.
     * <p>
     * 서버가 경로에 대해 처음 본 요청을 반환한다.
     *
     * @param server the server / 서버
     * @param path   the path / 경로
     * @return the request / 요청
     */
    private static TestHttpServer.Request firstRequest(TestHttpServer server, String path) {
        // 기록된 요청을 순서대로 보며 경로가 같은 첫 요청을 찾는다.
        for (TestHttpServer.Request r : server.requests()) {
            if (path.equals(r.path)) {
                return r;
            }
        }
        Check.fail("no request for " + path);
        return null;
    }

    /**
     * Returns the origin of a server (scheme, host and port).
     * <p>
     * 서버의 오리진(스킴, 호스트, 포트)을 반환한다.
     *
     * @param server the server / 서버
     * @return the origin / 오리진
     */
    private static String origin(TestHttpServer server) {
        String u = server.url("/x");
        return u.substring(0, u.length() - 2);
    }

    /**
     * Builds a page with the reporting function <code>put(key, value)</code> and a script.
     * <p>
     * 보고 함수 <code>put(key, value)</code> 와 스크립트가 있는 페이지를 만든다.
     *
     * @param script the script / 스크립트
     * @return the HTML / HTML
     */
    private static String page(String script) {
        return "<!doctype html><meta charset=utf-8><script>function put(k, v) { sessionStorage.setItem(k, String(v)); }" + script + "</" + "script>";
    }

    /**
     * Creates an event stream response whose body is written by a streamer.
     * <p>
     * 본문을 스트리머가 쓰는 이벤트 스트림 응답을 만든다.
     *
     * @param streamer the streamer / 스트리머
     * @return the response / 응답
     */
    private static TestHttpServer.Response eventStream(TestHttpServer.Streamer streamer) {
        return new TestHttpServer.Response(200).header("Content-Type", "text/event-stream; charset=utf-8").header("Cache-Control", "no-cache")
                .streamedBy(streamer);
    }

    /**
     * The events reach the page while the response is still open; the end of the response reestablishes the connection with
     * <code>Last-Event-ID</code>; a 204 then stops the source for good. The request goes through the request layer of the component.
     * <p>
     * 이벤트는 응답이 아직 열려 있는 동안 페이지에 닿는다. 응답이 끝나면 <code>Last-Event-ID</code> 로 연결을 다시 맺고, 그 뒤 204 는 소스를 영구히 멈춘다.
     * 요청은 컴포넌트의 요청 계층을 거친다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEventsAreDeliveredWhileTheResponseIsOpenAndTheConnectionIsReestablished() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        final CountDownLatch release = new CountDownLatch(1);
        try {
            server.route("/events", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    // 첫 요청은 이벤트 하나를 보낸 뒤 시험이 허락할 때까지 열어 둔다. 두 번째 요청은 204 로 멈추게 한다.
                    if (server.count("/events") >= 2) {
                        return new TestHttpServer.Response(204);
                    }
                    return eventStream(new TestHttpServer.Streamer() {
                        public void produce(TestHttpServer.Sink sink) throws Exception {
                            sink.write("retry: 100\n\ndata: one\n\n");
                            release.await(30, TimeUnit.SECONDS);
                            sink.write("id: 2\nevent: tick\ndata: two\ndata: lines\n\n");
                        }
                    });
                }
            });
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var es = new EventSource('/events'); var n = 0, errs = 0;"
                            + "es.onopen = function () { put('open', es.readyState); };"
                            + "es.onmessage = function (e) { put('m1', e.data + '|' + e.lastEventId + '|' + e.origin + '|' + e.type); };"
                            + "es.addEventListener('tick', function (e) { put('tick', e.data.replace(/\\n/g, '+') + '|' + e.lastEventId); });"
                            + "es.onerror = function () { errs++; put('err' + errs, es.readyState); };")).header("Set-Cookie", "sid=1; Path=/"));
            server.start();
            panel.setUserAgent("UA-sse/1");
            Map<String, String> extra = new LinkedHashMap<String, String>();
            extra.put("X-Extra", "e1");
            panel.addHeaders(extra);
            panel.setPage(server.url("/page"));
            Check.equal("1", await(panel, "open"), "the open event, with the state OPEN");
            Check.equal("one||" + origin(server) + "|message", await(panel, "m1"),
                    "the first event arrived although the response is still open (not buffered); the origin is the one of the stream");
            Check.isNull(value(panel, "tick"), "the second event has not been sent yet");
            release.countDown();
            Check.equal("two+lines|2", await(panel, "tick"), "a named event with two data lines, and its ID");
            Check.equal("0", await(panel, "err1"), "the end of the response: an error event and the state CONNECTING");
            awaitRequests(server, "/events", 2);
            TestHttpServer.Request first = firstRequest(server, "/events");
            Check.equal("text/event-stream", first.header("Accept"), "Accept");
            Check.equal("UA-sse/1", first.header("User-Agent"), "the User-Agent of the component");
            Check.equal("e1", first.header("X-Extra"), "the extra header of the component");
            Check.equal("sid=1", first.header("Cookie"), "the cookie jar of the component (credentials mode same-origin)");
            Check.equal(server.url("/page"), first.header("Referer"), "the Referer is the page");
            Check.contains(String.valueOf(first.header("Cache-Control")), "no-cache", "a stream never uses the cache");
            Check.isNull(first.header("Last-Event-ID"), "no ID before the first event with an id");
            Check.equal("2", server.last("/events").header("Last-Event-ID"), "the reconnection reports the last event ID");
            Check.equal("2", await(panel, "err2"), "the 204 fails the connection: CLOSED");
            Thread.sleep(600);
            Check.equal(2, server.count("/events"), "and the source does not reconnect again (retry was 100 ms)");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A connection that the server drops reestablishes with the last event ID, and the events of the new connection arrive in order.
     * <p>
     * 서버가 끊은 연결은 마지막 이벤트 ID 로 다시 맺어지고 새 연결의 이벤트가 순서대로 온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReconnectionAfterTheServerDropsTheConnection() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.route("/events", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(final TestHttpServer.Request request) {
                    // 첫 연결은 이벤트를 보낸 뒤 끝 조각 없이 끊긴다. 두 번째 연결은 요청 헤더를 이벤트로 돌려준다.
                    final boolean first = server.count("/events") <= 1;
                    return eventStream(new TestHttpServer.Streamer() {
                        public void produce(TestHttpServer.Sink sink) throws Exception {
                            if (first) {
                                sink.write("retry: 50\nid: 5\ndata: a\n\n");
                                throw new IOException("drop");
                            }
                            sink.write("data: id=" + request.header("Last-Event-ID") + "\n\n");
                            sink.pause(60000);
                        }
                    });
                }
            });
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var es = new EventSource('/events'); var log = [];"
                            + "es.onmessage = function (e) { log.push(e.data + '/' + e.lastEventId); if (log.length === 2) { put('log', log.join(',')); } };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("a/5,id=5/5", await(panel, "log"), "the second connection starts with the last ID and the source keeps the ID for its events");
            Check.equal("5", server.last("/events").header("Last-Event-ID"), "Last-Event-ID on the wire");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Answers that must fail the connection do so at once and are not retried: other MIME types, error statuses, other schemes.
     * <p>
     * 연결을 실패시켜야 하는 답은 곧바로 실패하고 다시 시도하지 않는다: 다른 MIME 형식, 오류 상태, 다른 스킴.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnswersThatFailTheConnection() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/html", TestHttpServer.Response.ok("text/html", "data: x\n\n"));
            server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/plain", "no"));
            server.routeFixed("/boom", new TestHttpServer.Response(500).header("Content-Type", "text/event-stream"));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var urls = ['/html', '/missing', '/boom', 'ftp://127.0.0.1:9/x', 'file:///etc/x', 'data:text/event-stream,data:x%0A%0A'];"
                            + "urls.forEach(function (u, i) { var es = new EventSource(u); es.onmessage = function () { put('msg' + i, 'bad'); };"
                            + "es.onerror = function () { put('r' + i, es.readyState); }; });")));
            server.start();
            panel.setPage(server.url("/page"));
            // 여섯 연결이 모두 한 번의 오류와 CLOSED 상태로 끝나야 한다.
            for (int i = 0; i < 6; i++) {
                Check.equal("2", await(panel, "r" + i), "source " + i + " failed (CLOSED)");
                Check.isNull(value(panel, "msg" + i), "source " + i + " delivered nothing");
            }
            Thread.sleep(500);
            Check.equal(1, server.count("/html"), "the wrong MIME type was not retried");
            Check.equal(1, server.count("/missing"), "the 404 was not retried");
            Check.equal(1, server.count("/boom"), "the 500 was not retried");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A stream that is redirected: the redirect is followed (same origin and to another origin that allows CORS), the events carry the origin of the
     * final URL of the stream (<code>MessageEvent.origin</code>), and a reconnection asks the address of the source again (not the target of the
     * redirect). A redirect to an address that is not allowed (another scheme) is a network error: the source reconnects.
     * <p>
     * 방향이 바뀐 스트림: 리디렉션을 따라가고(같은 오리진과 CORS 를 허락하는 다른 오리진), 이벤트는 스트림의 최종 URL 의 오리진을 싣고(<code>MessageEvent.origin</code>),
     * 재연결은 리디렉션 대상이 아니라 소스의 주소를 다시 묻는다. 허용되지 않는 주소(다른 스킴)로의 리디렉션은 네트워크 오류이므로 소스가 다시 연결한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectedStreams() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            other.routeFixed("/target", eventStream(new TestHttpServer.Streamer() {
                public void produce(TestHttpServer.Sink sink) throws Exception {
                    sink.write("data: far\n\n");
                    sink.pause(60000);
                }
            }).header("Access-Control-Allow-Origin", "*"));
            other.start();
            server.routeFixed("/local-target", eventStream(new TestHttpServer.Streamer() {
                public void produce(TestHttpServer.Sink sink) throws Exception {
                    sink.write("retry: 50\ndata: near\n\n");
                    throw new IOException("drop");
                }
            }));
            server.routeFixed("/near", TestHttpServer.Response.redirect(302, "/local-target"));
            server.routeFixed("/far", TestHttpServer.Response.redirect(302, other.url("/target")));
            server.routeFixed("/bad", TestHttpServer.Response.redirect(302, "ftp://127.0.0.1:9/x"));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var near = new EventSource('/near'); var far = new EventSource('/far'); var bad = new EventSource('/bad'); var nearCount = 0;"
                            + "near.onmessage = function (e) { nearCount++; if (nearCount === 2) { put('near', e.data + '@' + e.origin); } };"
                            + "far.onmessage = function (e) { put('far', e.data + '@' + e.origin); };"
                            + "bad.onerror = function () { put('bad', bad.readyState); };")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("near@" + origin(server), await(panel, "near"), "the redirect was followed and the origin is that of the final URL (twice: reconnected)");
            Check.equal("far@" + origin(other), await(panel, "far"), "a redirect to another origin that allows CORS works, with the origin of the target");
            Check.equal("0", await(panel, "bad"), "a redirect to an address that is not allowed is a network error: the source stays CONNECTING and tries again");
            Check.isTrue(server.count("/near") >= 2, "the reconnection asks the address of the source again (" + server.count("/near") + " requests)");
        } finally {
            panel.dispose();
            server.stop();
            other.stop();
        }
    }

    /**
     * Cross-origin streams follow CORS: an allowing server works and sees <code>Origin</code>; a server without the header fails the connection
     * without any retry; credentials need the exact origin and <code>Allow-Credentials</code>.
     * <p>
     * 교차 오리진 스트림은 CORS 를 따른다: 허락하는 서버는 동작하며 <code>Origin</code> 을 본다. 헤더가 없는 서버는 재시도 없이 연결이 실패한다. 자격 증명은
     * 정확한 오리진과 <code>Allow-Credentials</code> 가 필요하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginStreamsFollowCors() throws Exception {
        final TestHttpServer a = new TestHttpServer();
        final TestHttpServer b = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            a.start();
            b.start();
            String ao = origin(a);
            b.route("/open", cors(null, null));
            b.route("/deny", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return eventStream(new TestHttpServer.Streamer() {
                        public void produce(TestHttpServer.Sink sink) throws Exception {
                            sink.write("data: nope\n\n");
                        }
                    });
                }
            });
            b.route("/cred", cors(ao, "true"));
            b.route("/nocred", cors(ao, "true"));
            b.route("/credwild", cors("*", "true"));
            a.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var B = '" + origin(b) + "';"
                            + "function watch(k, es) { es.onmessage = function (e) { put(k, e.data + ':' + es.readyState); }; es.onerror = function () { put(k, 'error:' + es.readyState); }; }"
                            + "watch('open', new EventSource(B + '/open')); watch('deny', new EventSource(B + '/deny'));"
                            + "watch('cred', new EventSource(B + '/cred', {withCredentials: true}));"
                            + "watch('credwild', new EventSource(B + '/credwild', {withCredentials: true}));"
                            + "watch('nocred', new EventSource(B + '/nocred'));")).header("Set-Cookie", "sid=1; Path=/"));
            panel.setPage(a.url("/page"));
            Check.equal("hello:1", await(panel, "open"), "a server that allows every origin");
            Check.equal("error:2", await(panel, "deny"), "a server without Access-Control-Allow-Origin fails the connection");
            Check.equal("hello:1", await(panel, "cred"), "credentials with the exact origin and Allow-Credentials");
            Check.equal("error:2", await(panel, "credwild"), "a wildcard origin does not allow credentials");
            Check.equal("hello:1", await(panel, "nocred"), "an exact origin is fine without credentials too");
            Check.isNull(b.last("/nocred").header("Cookie"), "and then no cookie is sent");
            Check.equal(ao, b.last("/open").header("Origin"), "the request carries the Origin header of the page");
            Check.isNull(b.last("/open").header("Cookie"), "no cookie to another origin without withCredentials");
            Check.equal("sid=1", b.last("/cred").header("Cookie"), "the cookie goes with withCredentials");
            Thread.sleep(400);
            Check.equal(1, b.count("/deny"), "a CORS failure is not retried");
        } finally {
            panel.dispose();
            a.stop();
            b.stop();
        }
    }

    /**
     * Creates a handler for a cross-origin event stream with the given CORS headers.
     * <p>
     * 주어진 CORS 헤더를 가진 교차 오리진 이벤트 스트림의 처리기를 만든다.
     *
     * @param allowOrigin the Access-Control-Allow-Origin value, or null for "*" / Access-Control-Allow-Origin 값. null 이면 "*"
     * @param credentials the Access-Control-Allow-Credentials value or null / Access-Control-Allow-Credentials 값 또는 null
     * @return the handler / 처리기
     */
    private static TestHttpServer.Handler cors(final String allowOrigin, final String credentials) {
        return new TestHttpServer.Handler() {
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                TestHttpServer.Response r = eventStream(new TestHttpServer.Streamer() {
                    public void produce(TestHttpServer.Sink sink) throws Exception {
                        sink.write("data: hello\n\n");
                        sink.pause(60000);
                    }
                });
                r.header("Access-Control-Allow-Origin", allowOrigin == null ? "*" : allowOrigin);
                if (credentials != null) {
                    r.header("Access-Control-Allow-Credentials", credentials);
                }
                return r;
            }
        };
    }

    /**
     * A page that goes away cancels its stream: the server notices that the client left. So does the disposal of the component.
     * <p>
     * 사라지는 페이지는 스트림을 취소한다: 서버가 클라이언트가 떠난 것을 알아챈다. 컴포넌트 폐기도 마찬가지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationAndDisposalCancelTheStream() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        final CountDownLatch gone1 = new CountDownLatch(1);
        final CountDownLatch gone2 = new CountDownLatch(1);
        try {
            server.route("/events", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(final TestHttpServer.Request request) {
                    // 짧은 간격으로 주석 줄을 쓰다가 클라이언트가 떠나 쓰기가 실패하면 알린다.
                    final CountDownLatch gone = request.query != null && request.query.indexOf("n=2") >= 0 ? gone2 : gone1;
                    return eventStream(new TestHttpServer.Streamer() {
                        public void produce(TestHttpServer.Sink sink) throws Exception {
                            try {
                                sink.write("data: first\n\n");
                                // 쓰기가 실패하거나 서버가 멈출 때까지 계속 쓴다.
                                while (sink.isRunning()) {
                                    sink.write(":keep-alive\n\n");
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
            server.routeFixed("/page1", TestHttpServer.Response.html(page(
                    "var es = new EventSource('/events?n=1'); es.onmessage = function (e) { put('got1', e.data); };")));
            server.routeFixed("/page2", TestHttpServer.Response.html(page(
                    "var es = new EventSource('/events?n=2'); es.onmessage = function (e) { put('got2', e.data); };"
                            + "es.onerror = function () { put('err2', es.readyState); };")));
            server.routeFixed("/other", TestHttpServer.Response.html("<p>other</p>"));
            server.start();
            panel.setPage(server.url("/page1"));
            Check.equal("first", await(panel, "got1"), "the stream is open");
            Check.equal(1, gone1.getCount(), "the server still has the client");
            panel.setPage(server.url("/other"));
            awaitPage(panel, server.url("/other"));
            Check.isTrue(gone1.await(20, TimeUnit.SECONDS), "navigating away closed the connection of the stream");
            panel.setPage(server.url("/page2"));
            awaitPage(panel, server.url("/page2"));
            Check.equal("first", await(panel, "got2"), "the stream of the next page is open");
            panel.dispose();
            Check.isTrue(gone2.await(20, TimeUnit.SECONDS), "disposing the component closed the connection of the stream");
            Thread.sleep(300);
            Check.equal(2, server.count("/events"), "and nothing reconnected");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page has at most six open streams: the seventh fails at once, the six stay open.
     * <p>
     * 페이지는 열린 스트림을 최대 여섯 개까지 가질 수 있다: 일곱째는 곧바로 실패하고 여섯은 열려 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAPageHasAtMostSixOpenStreams() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.route("/events", cors(null, null));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var opened = 0, failed = [];"
                            + "for (var i = 0; i < 7; i++) { (function (i) { var es = new EventSource('/events?i=' + i);"
                            + "es.onmessage = function () { opened++; if (opened === 6) { put('opened', opened); } };"
                            + "es.onerror = function () { failed.push(i + ':' + es.readyState); put('failed', failed.join(',')); }; })(i); }")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("6:2", await(panel, "failed"), "only the seventh failed, and for good");
            Check.equal("6", await(panel, "opened"), "the other six receive their events");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
