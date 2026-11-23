package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the <code>EventSource</code> interface of the page against a fake streaming request service (the requests, the answers and the clock are
 * under the control of the test), following the processing model of the HTML Standard "Server-sent events" (fetched on 2026-11-17, see
 * <code>docs/progress/webapi2.md</code>). The real network, the request layer and the page lifetime are covered by
 * <code>org.duckdns.hjow.swingwebpanel.core.EventSourceIntegrationTest</code>.
 * <p>
 * 페이지의 <code>EventSource</code> 인터페이스를 가짜 스트리밍 요청 서비스(요청, 답, 시계를 시험이 정한다)로 시험한다. HTML Standard "Server-sent events"
 * (2026-11-17 조회, <code>docs/progress/webapi2.md</code>)의 처리 모델을 따른다. 실제 네트워크, 요청 계층, 페이지 수명은
 * <code>org.duckdns.hjow.swingwebpanel.core.EventSourceIntegrationTest</code> 가 맡는다.
 */
public class EventSourceTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public EventSourceTest() {
    }

    /**
     * Opens a stream that the fake service accepts as an event stream and returns its fixture.
     * <p>
     * 가짜 서비스가 이벤트 스트림으로 받아들이는 스트림을 열고 그 시험 틀을 반환한다.
     *
     * @param script the script that follows <code>window.log = []; var es = new EventSource('stream');</code> / 그 뒤에 이어질 스크립트
     * @return the fixture / 시험 틀
     */
    private static WebFixture open(String script) {
        WebFixture f = new WebFixture();
        f.js("window.log = []; window.es = new EventSource('stream');" + script);
        return f;
    }

    /**
     * The interface object, the constants, the tag and the constructor checks.
     * <p>
     * 인터페이스 객체, 상수, 태그, 생성자 검사.
     */
    public void testInterface() {
        WebFixture f = new WebFixture();
        Check.equal("function|0,1,2|[object EventSource]|true|true", f.js("[typeof EventSource, [EventSource.CONNECTING, EventSource.OPEN, "
                + "EventSource.CLOSED].join(), Object.prototype.toString.call(new EventSource('s')), new EventSource('s') instanceof EventTarget, "
                + "EventSource.prototype.CLOSED === 2].join('|')"), "interface, constants, tag, EventTarget");
        Check.equal("TypeError", f.js("try { new EventSource(); 'no' } catch (e) { e.name }"), "the URL is required");
        Check.equal("TypeError", f.js("try { EventSource('s'); 'no' } catch (e) { e.name }"), "new is required");
        Check.equal("SyntaxError", f.js("try { new EventSource('http://[bad'); 'no' } catch (e) { e.name }"), "a URL that cannot be parsed");
        Check.equal("SyntaxError", f.js("try { new EventSource('http://:80/x'); 'no' } catch (e) { e.name }"), "a URL without a host");
        Check.equal("TypeError", f.js("try { new EventSource('s', 5); 'no' } catch (e) { e.name }"), "the init dictionary must be an object");
        Check.equal("http://example.test/dir/s?a=1|false|0", f.js("var e1 = new EventSource('s?a=1'); [e1.url, e1.withCredentials, e1.readyState].join('|')"),
                "url is resolved against the document, no credentials, connecting");
        Check.equal("true", f.js("String(new EventSource('s', {withCredentials: true}).withCredentials)"), "withCredentials");
        Check.equal("function|true|true|true", f.js("[typeof e1.close, 'onopen' in e1, e1.onmessage === null, e1.onerror === null].join('|')"),
                "close and the handler attributes");
    }

    /**
     * The request that the page asks for: a GET that accepts event streams, never uses the cache and uses the credentials mode of the options.
     * <p>
     * 페이지가 요청하는 것: 이벤트 스트림을 받겠다는 GET, 캐시를 쓰지 않음, 옵션의 자격 증명 모드.
     */
    public void testRequestShape() {
        WebFixture f = new WebFixture();
        f.js("window.a = new EventSource('stream?x=1'); window.b = new EventSource('/abs', {withCredentials: true});");
        Check.equal(2, f.env.streams.size(), "two streams were started");
        WebRequest r = f.env.streams.get(0).request;
        Check.equal("http://example.test/dir/stream?x=1", r.getUrl(), "resolved URL");
        Check.equal("GET", r.getMethod(), "method");
        Check.equal(WebRequest.MODE_CORS, r.getMode(), "CORS mode");
        Check.equal(WebRequest.CREDENTIALS_SAME_ORIGIN, r.getCredentials(), "default credentials mode: same-origin");
        Check.equal("no-store", r.getCache(), "never from the cache");
        Check.equal("text/event-stream", header(r, "Accept"), "Accept");
        Check.isNull(header(r, "Last-Event-ID"), "no Last-Event-ID before an event ID was seen");
        Check.equal(WebRequest.CREDENTIALS_INCLUDE, f.env.streams.get(1).request.getCredentials(), "withCredentials: true is the include mode");
        Check.equal("http://example.test/abs", f.env.streams.get(1).request.getUrl(), "absolute path");
    }

    /**
     * Returns the value of a request header or null.
     * <p>
     * 요청 헤더의 값을 반환한다. 없으면 null.
     *
     * @param r    the request / 요청
     * @param name the header name / 헤더 이름
     * @return the value or null / 값 또는 null
     */
    private static String header(WebRequest r, String name) {
        // 헤더 목록에서 이름이 같은 첫 헤더를 찾는다.
        for (String[] h : r.getHeaders()) {
            if (h[0].equalsIgnoreCase(name)) {
                return h[1];
            }
        }
        return null;
    }

    /**
     * The open event, the message events and their fields, in order, as tasks of the page.
     * <p>
     * open 이벤트, message 이벤트와 그 필드. 페이지 작업으로 순서대로 온다.
     */
    public void testOpenAndMessages() {
        WebFixture f = open("es.onopen = function (e) { log.push('open:' + es.readyState + ':' + e.type + ':' + (e.target === es)); };"
                + "es.onmessage = function (e) { log.push('message:' + e.data + ':' + e.lastEventId + ':' + e.origin + ':' + e.type + ':' + e.isTrusted + ':'"
                + " + (e instanceof MessageEvent) + ':' + e.bubbles); };"
                + "es.addEventListener('add', function (e) { log.push('add:' + e.data + ':' + e.type); });");
        Check.equal("0", f.js("String(es.readyState)"), "connecting before the answer");
        Check.isTrue(f.streamHead(0, 200, "text/event-stream; charset=utf-8"), "the answer is accepted");
        Check.equal("open:1:open:true", f.js("log.join('|')"), "open after the head, as a page task");
        f.streamData(0, "data: one\nid: 1\n\nevent: add\ndata: two\n\n");
        f.streamData(0, "data: th");
        f.streamData(0, "ree\ndata: line two\n\n: comment\n\n");
        Check.equal("open:1:open:true|message:one:1:http://example.test:message:true:true:false|add:two:add|"
                + "message:three\nline two:1:http://example.test:message:true:true:false", f.js("log.join('|')"),
                "events in stream order, the ID stays, split pieces are joined");
    }

    /**
     * A stream that is not accepted fails the connection: <code>CLOSED</code>, an <code>error</code> event, no reconnection.
     * <p>
     * 받아들이지 않은 스트림은 연결을 실패시킨다: <code>CLOSED</code>, <code>error</code> 이벤트, 재연결 없음.
     */
    public void testRefusedAnswersFailTheConnection() {
        Object[][] cases = { { Integer.valueOf(200), "text/html" }, { Integer.valueOf(200), null }, { Integer.valueOf(204), "text/event-stream" },
            { Integer.valueOf(301), "text/event-stream" }, { Integer.valueOf(201), "text/event-stream" } };
        // 상태나 형식이 틀린 답마다 같은 방식으로 실패해야 한다.
        for (int i = 0; i < cases.length; i++) {
            WebFixture f = open("es.onerror = function (e) { log.push('error:' + es.readyState); }; es.onopen = function () { log.push('open'); };");
            int status = ((Integer) cases[i][0]).intValue();
            String type = (String) cases[i][1];
            Check.isFalse(f.streamHead(0, status, type), "refused: " + status + " " + type);
            f.streamEnd(0, headOnly(status, type));
            Check.equal("error:2", f.js("log.join('|')"), "failed: " + status + " " + type);
            f.advance(60000);
            Check.equal(1, f.env.streams.size(), "no reconnection after " + status + " " + type);
        }
        // 오류 상태(400 이상)는 스트림이 시작되지 않고 곧바로 끝으로 온다.
        WebFixture f = open("es.onerror = function () { log.push('error:' + es.readyState); };");
        f.streamEnd(0, WebFixture.response(500, "text/event-stream", "data: x\n\n"));
        Check.equal("error:2", f.js("log.join('|')"), "a 500 fails the connection");
        f.advance(60000);
        Check.equal(1, f.env.streams.size(), "no reconnection after a 500");
    }

    /**
     * Builds the head that the request layer reports at the end of a refused stream.
     * <p>
     * 요청 계층이 거부된 스트림의 끝에서 알리는 머리를 만든다.
     *
     * @param status the status / 상태
     * @param type   the Content-Type or null / Content-Type 또는 null
     * @return the response / 응답
     */
    private static WebResponse headOnly(int status, String type) {
        return WebFixture.response(status, type, "");
    }

    /**
     * The MIME type is compared as a media type: parameters and case do not matter.
     * <p>
     * MIME 형식은 미디어 타입으로 비교한다. 매개변수와 대소문자는 상관없다.
     */
    public void testMimeTypeIsAMediaType() {
        String[] ok = { "text/event-stream", "Text/Event-Stream", "text/event-stream;charset=UTF-8", " text/event-stream ; foo=bar" };
        // 형식 표기가 달라도 모두 받아들여야 한다.
        for (int i = 0; i < ok.length; i++) {
            WebFixture f = open("");
            Check.isTrue(f.streamHead(0, 200, ok[i]), "accepted: " + ok[i]);
        }
        String[] bad = { "text/event-streams", "application/event-stream", "text/event-stream-x" };
        // 비슷하지만 다른 형식은 거부해야 한다.
        for (int i = 0; i < bad.length; i++) {
            WebFixture f = open("");
            Check.isFalse(f.streamHead(0, 200, bad[i]), "refused: " + bad[i]);
        }
    }

    /**
     * The end of the body reestablishes the connection: <code>CONNECTING</code>, an <code>error</code> event, the reconnection time, a new request with the
     * <code>Last-Event-ID</code>; a <code>retry</code> field changes the time.
     * <p>
     * 본문의 끝은 연결을 다시 맺는다: <code>CONNECTING</code>, <code>error</code> 이벤트, 재연결 시간, <code>Last-Event-ID</code> 를 붙인 새 요청.
     * <code>retry</code> 필드가 시간을 바꾼다.
     */
    public void testReconnectionWithLastEventId() {
        WebFixture f = open("es.onerror = function () { log.push('error:' + es.readyState); }; es.onopen = function () { log.push('open'); };"
                + "es.onmessage = function (e) { log.push('m:' + e.data + ':' + e.lastEventId); };");
        f.streamHead(0, 200, "text/event-stream");
        f.streamData(0, "id: 7\ndata: a\n\nretry: 500\n\n");
        f.streamEnd(0, WebFixture.response(200, "text/event-stream", ""));
        Check.equal("open|m:a:7|error:0", f.js("log.join('|')"), "the end of the stream reestablishes the connection");
        Check.equal("0", f.js("String(es.readyState)"), "connecting again");
        f.advance(499);
        Check.equal(1, f.env.streams.size(), "not before the reconnection time that the stream set (500 ms)");
        f.advance(1);
        Check.equal(2, f.env.streams.size(), "then the new request goes out");
        Check.equal("7", header(f.env.streams.get(1).request, "Last-Event-ID"), "with the last event ID");
        f.streamHead(1, 200, "text/event-stream");
        f.streamData(1, "data: b\n\n");
        Check.equal("open|m:a:7|error:0|open|m:b:7", f.js("log.join('|')"), "a new open event; the ID of the source is kept for events without an id field");
        // 다시 끝나면 retry 로 정한 시간이 그대로 쓰인다.
        f.streamEnd(1, WebFixture.response(200, "text/event-stream", ""));
        f.advance(500);
        Check.equal(3, f.env.streams.size(), "the reconnection time stays 500 ms");
        // id 필드가 빈 값이면 ID 가 지워져 Last-Event-ID 를 보내지 않는다.
        f.streamHead(2, 200, "text/event-stream");
        f.streamData(2, "id\ndata: c\n\n");
        f.streamEnd(2, WebFixture.response(200, "text/event-stream", ""));
        f.advance(500);
        Check.isNull(header(f.env.streams.get(3).request, "Last-Event-ID"), "an empty id field resets the ID: no header");
    }

    /**
     * A network error reestablishes the connection after the default time (3 seconds); an error that a retry cannot fix fails it.
     * <p>
     * 네트워크 오류는 기본 시간(3초) 뒤에 연결을 다시 맺는다. 다시 시도해도 고쳐지지 않는 오류는 연결을 실패시킨다.
     */
    public void testNetworkErrors() {
        WebFixture f = open("es.onerror = function () { log.push('error:' + es.readyState); };");
        f.streamEnd(0, WebResponse.error("connection refused", false));
        Check.equal("error:0", f.js("log.join('|')"), "a network error before the answer: error event, still connecting");
        f.advance(2999);
        Check.equal(1, f.env.streams.size(), "the default reconnection time is 3 seconds");
        f.advance(1);
        Check.equal(2, f.env.streams.size(), "reconnected");
        f.streamEnd(1, WebResponse.error("again", false));
        Check.equal("error:0|error:0", f.js("log.join('|')"), "every failed attempt fires error");
        f.advance(3000);
        Check.equal(3, f.env.streams.size(), "and tries again");
        f.streamEnd(2, WebResponse.futileError("blocked by CORS policy"));
        Check.equal("error:0|error:0|error:2", f.js("log.join('|')"), "an error that a retry cannot fix fails the connection");
        f.advance(60000);
        Check.equal(3, f.env.streams.size(), "no more attempts");
    }

    /**
     * <code>close()</code> cancels the request and a waiting reconnection; later answers change nothing; it never fires an event.
     * <p>
     * <code>close()</code> 는 요청과 기다리는 재연결을 취소한다. 나중의 답은 아무것도 바꾸지 못한다. 이벤트는 발생하지 않는다.
     */
    public void testClose() {
        WebFixture f = open("es.onmessage = function (e) { log.push('m:' + e.data); }; es.onerror = function () { log.push('error'); };"
                + "es.onopen = function () { log.push('open'); };");
        f.streamHead(0, 200, "text/event-stream");
        f.js("es.close()");
        Check.equal("2", f.js("String(es.readyState)"), "closed");
        Check.isTrue(f.env.streams.get(0).cancelled, "the request was cancelled");
        f.streamData(0, "data: late\n\n");
        f.streamEnd(0, WebResponse.error("cancelled", true));
        Check.equal("open", f.js("log.join('|')"), "nothing happens after close(), not even an error event");
        f.js("es.close()");
        // 재연결을 기다리는 동안 닫으면 새 요청이 나가지 않는다.
        WebFixture g = open("es.onerror = function () { log.push('error'); };");
        g.streamEnd(0, WebResponse.error("down", false));
        g.js("es.close()");
        g.advance(60000);
        Check.equal(1, g.env.streams.size(), "closing while waiting cancels the reconnection");
        Check.equal("error", g.js("log.join('|')"), "only the error before the close");
    }

    /**
     * A handler that closes the source stops the events that follow in the same piece; a handler of <code>error</code> can stop the reconnection.
     * <p>
     * 소스를 닫는 핸들러는 같은 조각에서 뒤따르는 이벤트를 멈춘다. <code>error</code> 핸들러는 재연결을 멈출 수 있다.
     */
    public void testHandlersMayClose() {
        WebFixture f = open("es.onmessage = function (e) { log.push('m:' + e.data); if (e.data === 'a') { es.close(); } };");
        f.streamHead(0, 200, "text/event-stream");
        f.streamData(0, "data: a\n\ndata: b\n\ndata: c\n\n");
        Check.equal("m:a", f.js("log.join('|')"), "b and c are not delivered after the handler closed the source");
        WebFixture g = open("es.onerror = function () { log.push('error'); es.close(); };");
        g.streamEnd(0, WebResponse.error("down", false));
        g.advance(60000);
        Check.equal("error", g.js("log.join('|')"), "one error event");
        Check.equal(1, g.env.streams.size(), "closing in the error handler stops the reconnection");
    }

    /**
     * A result of an older attempt does not touch a newer one.
     * <p>
     * 더 오래된 시도의 결과는 더 새로운 시도에 영향을 주지 않는다.
     */
    public void testOldAttemptsAreIgnored() {
        WebFixture f = open("es.onmessage = function (e) { log.push('m:' + e.data); };");
        f.streamEnd(0, WebResponse.error("down", false));
        f.advance(3000);
        f.streamHead(1, 200, "text/event-stream");
        // 첫 시도의 늦은 데이터와 끝은 무시된다.
        f.streamData(0, "data: old\n\n");
        f.streamEnd(0, WebResponse.error("late", false));
        f.streamData(1, "data: new\n\n");
        Check.equal("m:new", f.js("log.join('|')"), "only the current attempt delivers");
        Check.equal("1", f.js("String(es.readyState)"), "and a late end of the old attempt does not close or restart the new one");
    }

    /**
     * Data that is longer than the limit fails the connection.
     * <p>
     * 한도보다 긴 데이터는 연결을 실패시킨다.
     */
    public void testOverlongDataFailsTheConnection() {
        WebFixture f = open("es.onerror = function () { log.push('error:' + es.readyState); }; es.onmessage = function () { log.push('m'); };");
        f.streamHead(0, 200, "text/event-stream");
        StringBuilder sb = new StringBuilder("data: ");
        // 한도(8 MiB)를 조금 넘도록 쌓는다.
        for (int i = 0; i < EventStreamParser.MAX_BUFFER + 1024; i++) {
            sb.append('x');
        }
        f.streamData(0, sb.toString());
        Check.equal("error:2", f.js("log.join('|')"), "an overlong line fails the connection");
        Check.isTrue(f.env.streams.get(0).cancelled, "and the request is cancelled");
    }

    /**
     * A page whose host cannot stream fails the connection at once, without reconnecting.
     * <p>
     * 스트리밍할 수 없는 호스트의 페이지는 재연결 없이 곧바로 연결이 실패한다.
     */
    public void testHostWithoutStreaming() {
        WebFixture f = new WebFixture();
        f.env.streaming = false;
        f.js("window.log = []; window.es = new EventSource('stream'); es.onerror = function () { log.push('error:' + es.readyState); };");
        Check.equal("0", f.js("String(es.readyState)"), "the constructor does not fail by itself");
        f.run();
        Check.equal("error:2", f.js("log.join('|')"), "an error event and CLOSED, as a page task");
        WebFixture g = new WebFixture();
        g.env.hasSource = false;
        g.js("window.log = []; window.es = new EventSource('stream'); es.onerror = function () { log.push('error:' + es.readyState); };");
        g.run();
        Check.equal("error:2", g.js("log.join('|')"), "the same without any request service");
    }

    /**
     * Disposing the page cancels the streams and nothing is delivered afterwards.
     * <p>
     * 페이지를 폐기하면 스트림이 취소되고 그 뒤에는 아무것도 전달되지 않는다.
     */
    public void testDisposalCancelsTheStream() {
        WebFixture f = open("es.onmessage = function (e) { log.push('m:' + e.data); };");
        f.streamHead(0, 200, "text/event-stream");
        f.scripts.requestDisposal();
        Check.isTrue(f.env.streams.get(0).cancelled, "requestDisposal cancels the stream without waiting");
        f.streamData(0, "data: late\n\n");
        f.scripts.dispose();
        f.advance(60000);
        Check.equal(1, f.env.streams.size(), "no reconnection");
        Check.equal("", f.log(), "no console output");
    }

    /**
     * The id that goes into the header is the UTF-8 form of the string.
     * <p>
     * 헤더에 들어가는 ID 는 문자열의 UTF-8 형태이다.
     */
    public void testHeaderBytes() {
        Check.equal("abc", EventSourceApi.headerBytes("abc"), "ASCII is unchanged");
        Check.equal("í\u0095\u009C", EventSourceApi.headerBytes("한"), "a Korean letter becomes its three UTF-8 bytes as Latin-1 characters");
    }
}
