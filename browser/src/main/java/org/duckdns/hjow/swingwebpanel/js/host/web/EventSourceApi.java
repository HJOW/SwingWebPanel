package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.StreamingWebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.net.ContentType;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The <code>EventSource</code> interface (HTML Standard "Server-sent events", W3C Recommendation 2015-02-03 and the living standard of
 * 2026-11-16, see <code>docs/progress/webapi2.md</code>). The constructor resolves the URL (a URL that cannot be parsed throws a
 * <code>SyntaxError</code>), stays in <code>CONNECTING</code> and opens a streaming request through the page's {@link StreamingWebRequestSource}: a GET
 * with <code>Accept: text/event-stream</code> that never uses the cache, with the credentials mode <code>same-origin</code> (<code>include</code> for
 * <code>withCredentials: true</code>) and the CORS mode, so the User-Agent and the extra headers of the component, its cookie store, the SSL hook, the
 * scheme limits and the cross-origin rules apply as for <code>fetch()</code>.
 * <ul>
 * <li>A response with the status 200 and the MIME type <code>text/event-stream</code> announces the connection: as a page task the state becomes
 * <code>OPEN</code> and <code>open</code> fires. The body is read while it arrives and parsed by {@link EventStreamParser}; each event becomes a task that
 * fires a <code>MessageEvent</code> (type <code>message</code> or the <code>event</code> field; <code>data</code>, <code>origin</code> of the final URL of
 * the stream and <code>lastEventId</code>). A <code>retry</code> field sets the reconnection time (3 seconds at first).</li>
 * <li>Any other status (including 204, which tells the client to stop), another MIME type, a scheme that is not allowed or a cross-origin response that the
 * page may not read fails the connection: the state becomes <code>CLOSED</code> and <code>error</code> fires; there is no reconnection.</li>
 * <li>When the stream ends or the network fails, the connection is reestablished: the state becomes <code>CONNECTING</code>, <code>error</code> fires and
 * after the reconnection time a new request goes out with <code>Last-Event-ID</code> (UTF-8) if an event ID was seen. A new connection starts with the
 * last event ID of the source (Chromium does so; the specification text starts the buffer empty).</li>
 * <li><code>close()</code>, the replacement of the page and the disposal of the component cancel the request and a pending reconnection; a response that
 * arrives later changes nothing.</li>
 * </ul>
 * A line or an event data longer than 8 MiB fails the connection. The page can have at most six open streams (a seventh fails at once).
 * <p>
 * <code>EventSource</code> 인터페이스(HTML Standard "Server-sent events", W3C 권고안 2015-02-03 과 2026-11-16 의 현행 표준, <code>docs/progress/webapi2.md</code>
 * 참조). 생성자는 URL 을 해석하고(해석할 수 없으면 <code>SyntaxError</code>) <code>CONNECTING</code> 상태로 머물며 페이지의
 * {@link StreamingWebRequestSource} 로 스트리밍 요청을 연다: <code>Accept: text/event-stream</code> 인 GET, 캐시를 쓰지 않음, 자격 증명 모드
 * <code>same-origin</code>(<code>withCredentials: true</code> 이면 <code>include</code>), CORS 모드. 그러므로 컴포넌트의 User-Agent·추가 헤더, 쿠키 저장소,
 * SSL 훅, 스킴 제한, 교차 오리진 규칙이 <code>fetch()</code> 와 똑같이 적용된다.
 * <ul>
 * <li>상태 200 이고 MIME 형식이 <code>text/event-stream</code> 인 응답은 연결을 알린다: 페이지 작업으로 상태가 <code>OPEN</code> 이 되고
 * <code>open</code> 이 발생한다. 본문은 도착하는 대로 읽어 {@link EventStreamParser} 로 해석하며, 이벤트마다 <code>MessageEvent</code>(종류는
 * <code>message</code> 또는 <code>event</code> 필드, <code>data</code>, 스트림 최종 URL 의 <code>origin</code>, <code>lastEventId</code>)를 발생시키는 작업이
 * 된다. <code>retry</code> 필드는 재연결 시간을 정한다(처음에는 3초).</li>
 * <li>그 밖의 상태(클라이언트에게 멈추라고 알리는 204 포함), 다른 MIME 형식, 허용되지 않은 스킴, 페이지가 읽을 수 없는 교차 오리진 응답은 연결을 실패시킨다:
 * 상태가 <code>CLOSED</code> 가 되고 <code>error</code> 가 발생하며 재연결하지 않는다.</li>
 * <li>스트림이 끝나거나 네트워크가 실패하면 연결을 다시 맺는다: 상태가 <code>CONNECTING</code> 이 되고 <code>error</code> 가 발생하며, 재연결 시간 뒤에
 * 이벤트 ID 를 본 적이 있으면 <code>Last-Event-ID</code>(UTF-8)를 붙인 새 요청을 보낸다. 새 연결은 이벤트 소스의 마지막 이벤트 ID 로 시작한다(Chromium 이
 * 그렇게 한다. 규격 본문은 버퍼를 빈 문자열로 시작한다).</li>
 * <li><code>close()</code>, 페이지 교체, 컴포넌트 폐기는 요청과 대기 중인 재연결을 취소한다. 나중에 도착하는 응답은 아무것도 바꾸지 못한다.</li>
 * </ul>
 * 8 MiB 보다 긴 줄이나 이벤트 데이터는 연결을 실패시킨다. 페이지는 스트림을 최대 6개까지 열 수 있다(일곱째는 곧바로 실패한다).
 */
final class EventSourceApi {

    /**
     * The reconnection time at the start of a source in milliseconds (the specification says "a few seconds"; Chromium uses 3000).
     * <p>
     * 소스를 시작할 때의 재연결 시간(밀리초). 규격은 "몇 초"라고 하며 Chromium 은 3000 을 쓴다.
     */
    static final long DEFAULT_RECONNECTION_MS = 3000;

    /**
     * The ready state values: connecting, open, closed.
     * <p>
     * 준비 상태 값: 연결 중, 열림, 닫힘.
     */
    private static final String[] STATES = { "CONNECTING", "OPEN", "CLOSED" };

    /**
     * What a request thread learned from the stream, to be applied by a page task.
     * <p>
     * 요청 스레드가 스트림에서 알게 된 것. 페이지 작업이 적용한다.
     */
    private static final class Record {
        /**
         * The kind: 0 retry, 1 dispatch, 2 overflow.
         * <p>
         * 종류: 0 retry, 1 dispatch, 2 overflow.
         */
        final int kind;

        /**
         * The new reconnection time of a retry.
         * <p>
         * retry 의 새 재연결 시간.
         */
        final long retry;

        /**
         * The last event ID buffer of a dispatch.
         * <p>
         * dispatch 의 마지막 이벤트 ID 버퍼.
         */
        final String id;

        /**
         * The event type of a dispatch, null if it has no data.
         * <p>
         * dispatch 의 이벤트 종류. 데이터가 없으면 null.
         */
        final String type;

        /**
         * The data of a dispatch, null if it has none.
         * <p>
         * dispatch 의 데이터. 없으면 null.
         */
        final String data;

        /**
         * Creates a record.
         * <p>
         * 기록을 만든다.
         *
         * @param kind  the kind / 종류
         * @param retry the reconnection time / 재연결 시간
         * @param id    the event ID / 이벤트 ID
         * @param type  the event type / 이벤트 종류
         * @param data  the data / 데이터
         */
        Record(int kind, long retry, String id, String type, String data) {
            this.kind = kind;
            this.retry = retry;
            this.id = id;
            this.type = type;
            this.data = data;
        }
    }

    /**
     * An event source: the state that the script sees and the connection that serves it. All fields except those of {@link Attempt} belong to the
     * page's task thread.
     * <p>
     * 이벤트 소스: 스크립트가 보는 상태와 그것을 대는 연결. {@link Attempt} 의 필드를 뺀 모든 필드는 페이지의 작업 스레드의 것이다.
     */
    static final class Source extends EventTarget {
        /**
         * The URL that the constructor resolved.
         * <p>
         * 생성자가 해석한 URL.
         */
        private final String url;

        /**
         * True if the credentials mode is include.
         * <p>
         * 자격 증명 모드가 include 이면 true.
         */
        private final boolean withCredentials;

        /**
         * The ready state: 0 connecting, 1 open, 2 closed.
         * <p>
         * 준비 상태: 0 연결 중, 1 열림, 2 닫힘.
         */
        private int readyState;

        /**
         * The reconnection time in milliseconds.
         * <p>
         * 재연결 시간(밀리초).
         */
        private long reconnectionMs = DEFAULT_RECONNECTION_MS;

        /**
         * The last event ID string.
         * <p>
         * 마지막 이벤트 ID 문자열.
         */
        private String lastEventId = "";

        /**
         * The number of the current attempt; a result of an older attempt, or of a closed source, is ignored.
         * <p>
         * 현재 시도의 번호. 더 오래된 시도의 결과나 닫힌 소스의 결과는 무시한다.
         */
        private int epoch;

        /**
         * The request of the current attempt.
         * <p>
         * 현재 시도의 요청.
         */
        private WebRequestSource.Call call;

        /**
         * The timer of the reconnection that waits.
         * <p>
         * 기다리는 재연결의 타이머.
         */
        private Scheduler.Handle timer;

        /**
         * Creates a source in the connecting state.
         * <p>
         * 연결 중 상태의 소스를 만든다.
         *
         * @param url             the resolved URL / 해석한 URL
         * @param withCredentials true for the include mode / include 모드이면 true
         */
        Source(String url, boolean withCredentials) {
            this.url = url;
            this.withCredentials = withCredentials;
        }
    }

    /**
     * One connection attempt: the listener of a request thread. It parses the body on that thread and hands the results to page tasks.
     * <p>
     * 연결 시도 하나: 요청 스레드의 받는 쪽. 그 스레드에서 본문을 해석하고 결과를 페이지 작업으로 넘긴다.
     */
    private final class Attempt implements StreamingWebRequestSource.Listener, EventStreamParser.Sink {
        /**
         * The source.
         * <p>
         * 소스.
         */
        private final Source source;

        /**
         * The number of this attempt.
         * <p>
         * 이 시도의 번호.
         */
        private final int epoch;

        /**
         * The parser, created when the stream is accepted (request thread only).
         * <p>
         * 스트림을 받아들일 때 만드는 해석기(요청 스레드 전용).
         */
        private EventStreamParser parser;

        /**
         * The records that the parser produced for the piece that is being fed (request thread only).
         * <p>
         * 해석기가 지금 먹이는 조각에서 만든 기록(요청 스레드 전용).
         */
        private final List<Record> records = new ArrayList<Record>();

        /**
         * The origin of the final URL of the stream, the origin of its events.
         * <p>
         * 스트림 최종 URL 의 오리진. 이벤트의 오리진이다.
         */
        private volatile String origin = "";

        /**
         * True after the stream was accepted.
         * <p>
         * 스트림을 받아들인 뒤이면 true.
         */
        private volatile boolean accepted;

        /**
         * The last event ID that the source had when this attempt started.
         * <p>
         * 이 시도를 시작할 때 소스가 가진 마지막 이벤트 ID.
         */
        private final String startId;

        /**
         * Creates the attempt.
         * <p>
         * 시도를 만든다.
         *
         * @param source  the source / 소스
         * @param epoch   the number of the attempt / 시도의 번호
         * @param startId the last event ID at the start / 시작할 때의 마지막 이벤트 ID
         */
        Attempt(Source source, int epoch, String startId) {
            this.source = source;
            this.epoch = epoch;
            this.startId = startId;
        }

        /**
         * Accepts a stream with the status 200 and the MIME type <code>text/event-stream</code>, and announces it.
         * <p>
         * 상태 200 이고 MIME 형식이 <code>text/event-stream</code> 인 스트림을 받아들이고 알린다.
         *
         * @param head the response head / 응답 머리
         * @return true if the stream is accepted / 스트림을 받아들이면 true
         */
        public boolean onHeaders(WebResponse head) {
            boolean ok = head.getStatus() == 200 && "text/event-stream".equals(ContentType.parse(head.getHeader("content-type")).getMime());
            if (!ok) {
                return false;
            }
            origin = head.getUrl() == null || head.getUrl().length() == 0 ? "" : Origin.of(head.getUrl()).serialize();
            parser = new EventStreamParser(startId, this);
            accepted = true;
            post(new Runnable() {
                public void run() {
                    announce(source, epoch);
                }
            });
            return true;
        }

        /**
         * Parses a piece of the body and hands the events over.
         * <p>
         * 본문의 조각을 해석하고 이벤트를 넘긴다.
         *
         * @param data   the bytes / 바이트
         * @param length the valid bytes / 유효한 바이트 수
         */
        public void onData(byte[] data, int length) {
            if (parser == null) {
                return;
            }
            parser.feed(data, 0, length);
            // 너무 긴 줄이나 이벤트는 연결을 실패시킨다.
            if (parser.isOverflowed()) {
                records.add(new Record(2, 0, null, null, null));
            }
            if (records.isEmpty()) {
                return;
            }
            final List<Record> batch = new ArrayList<Record>(records);
            records.clear();
            final String eventOrigin = origin;
            post(new Runnable() {
                public void run() {
                    deliver(source, epoch, batch, eventOrigin);
                }
            });
        }

        /**
         * Hands the end of the stream over.
         * <p>
         * 스트림의 끝을 넘긴다.
         *
         * @param end the end / 끝
         */
        public void onEnd(final WebResponse end) {
            if (parser != null) {
                parser.finish();
            }
            post(new Runnable() {
                public void run() {
                    ended(source, epoch, end, accepted);
                }
            });
        }

        /**
         * The stream sets the reconnection time.
         * <p>
         * 스트림이 재연결 시간을 정한다.
         *
         * @param ms the milliseconds / 밀리초
         */
        public void retry(long ms) {
            records.add(new Record(0, ms, null, null, null));
        }

        /**
         * A block ended.
         * <p>
         * 블록이 끝났다.
         *
         * @param lastEventId the last event ID buffer / 마지막 이벤트 ID 버퍼
         * @param type        the event type or null / 이벤트 종류 또는 null
         * @param data        the data or null / 데이터 또는 null
         */
        public void dispatch(String lastEventId, String type, String data) {
            records.add(new Record(1, 0, lastEventId, type, data));
        }
    }

    /**
     * The installer of the web APIs of the page.
     * <p>
     * 페이지의 웹 API 설치기.
     */
    private final WebApis api;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the web APIs of the page / 페이지의 웹 API
     */
    EventSourceApi(WebApis api) {
        this.api = api;
    }

    /**
     * Hands a task over to the page's task thread (see {@link WebApis#post(Runnable)}).
     * <p>
     * 작업을 페이지의 작업 스레드로 넘긴다({@link WebApis#post(Runnable)} 참조).
     *
     * @param task the task / 작업
     */
    private void post(Runnable task) {
        api.post(task);
    }

    /**
     * Returns the source of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 소스를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the source / 소스
     */
    private static Source source(JsValue self) {
        Object t = DomBridge.targetOf(self, Source.class);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return (Source) t;
    }

    /**
     * Resolves the URL of the constructor; a URL that cannot be parsed is a <code>SyntaxError</code>.
     * <p>
     * 생성자의 URL 을 해석한다. 해석할 수 없는 URL 은 <code>SyntaxError</code> 이다.
     *
     * @param text the script value / 스크립트 값
     * @return the resolved URL / 해석한 URL
     */
    private String resolveUrl(String text) {
        String abs;
        try {
            abs = api.resolve(text);
            URI uri = new URI(abs);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            // http 와 https 주소는 호스트가 있어야 한다.
            if (("http".equals(scheme) || "https".equals(scheme)) && (uri.getRawAuthority() == null || uri.getRawAuthority().length() == 0)) {
                throw new URISyntaxException(abs, "missing host");
            }
        } catch (URISyntaxException e) {
            throw api.domError("SyntaxError", "Failed to construct 'EventSource': Cannot resolve '" + text + "' to a valid URL.");
        } catch (IllegalArgumentException e) {
            // 요청 계층의 URL 해석기가 거부한 주소(잘못된 호스트나 포트)도 같은 오류이다.
            throw api.domError("SyntaxError", "Failed to construct 'EventSource': Cannot resolve '" + text + "' to a valid URL.");
        }
        return abs;
    }

    /**
     * Starts a connection attempt: the source stays or becomes <code>CONNECTING</code> and a streaming request goes out.
     * <p>
     * 연결 시도를 시작한다: 소스는 <code>CONNECTING</code> 상태로 있거나 그 상태가 되고 스트리밍 요청이 나간다.
     *
     * @param s the source / 소스
     */
    private void connect(final Source s) {
        final int mine = ++s.epoch;
        WebRequestSource any = api.source();
        // 스트리밍을 할 수 없는 페이지는 만들 수 없는 연결처럼 실패한다.
        if (!(any instanceof StreamingWebRequestSource) || api.scheduler() == null) {
            post(new Runnable() {
                public void run() {
                    fail(s, mine);
                }
            });
            return;
        }
        WebRequest req = new WebRequest(s.url).setMode(WebRequest.MODE_CORS)
                .setCredentials(s.withCredentials ? WebRequest.CREDENTIALS_INCLUDE : WebRequest.CREDENTIALS_SAME_ORIGIN).setCache("no-store");
        req.addHeader("Accept", "text/event-stream");
        // 이벤트 ID 를 본 적이 있으면 UTF-8 바이트를 담은 Last-Event-ID 헤더로 알린다.
        if (s.lastEventId.length() > 0) {
            req.addHeader("Last-Event-ID", headerBytes(s.lastEventId));
        }
        Attempt attempt = new Attempt(s, mine, s.lastEventId);
        try {
            s.call = ((StreamingWebRequestSource) any).startStream(req, attempt);
            api.track(s.call);
        } catch (RuntimeException e) {
            post(new Runnable() {
                public void run() {
                    fail(s, mine);
                }
            });
        }
    }

    /**
     * Turns a string into the string whose ISO-8859-1 bytes are its UTF-8 bytes, which is how a header value carries UTF-8.
     * <p>
     * 문자열을, ISO-8859-1 바이트가 원래 문자열의 UTF-8 바이트인 문자열로 바꾼다. 헤더 값이 UTF-8 을 싣는 방법이다.
     *
     * @param value the text / 텍스트
     * @return the header text / 헤더 텍스트
     */
    static String headerBytes(String value) {
        try {
            return new String(value.getBytes("UTF-8"), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Tells whether a result still belongs to the source: the source has not been closed and no newer attempt began.
     * <p>
     * 결과가 아직 소스의 것인지 알려 준다: 소스가 닫히지 않았고 더 새로운 시도가 시작되지 않았다.
     *
     * @param s     the source / 소스
     * @param epoch the attempt of the result / 결과의 시도
     * @return true if the result counts / 결과가 유효하면 true
     */
    private static boolean current(Source s, int epoch) {
        return s.epoch == epoch && s.readyState != 2;
    }

    /**
     * Announces the connection: <code>OPEN</code> and the <code>open</code> event.
     * <p>
     * 연결을 알린다: <code>OPEN</code> 과 <code>open</code> 이벤트.
     *
     * @param s     the source / 소스
     * @param epoch the attempt / 시도
     */
    private void announce(Source s, int epoch) {
        if (!current(s, epoch)) {
            return;
        }
        s.readyState = 1;
        s.dispatchTrustedEvent(new Event("open", false, false));
    }

    /**
     * Applies the records of a piece of the stream in order; a handler that closes the source stops the rest.
     * <p>
     * 스트림 조각의 기록을 순서대로 적용한다. 소스를 닫는 핸들러가 있으면 나머지는 멈춘다.
     *
     * @param s       the source / 소스
     * @param epoch   the attempt / 시도
     * @param records the records / 기록
     * @param origin  the origin of the stream / 스트림의 오리진
     */
    private void deliver(Source s, int epoch, List<Record> records, String origin) {
        // 기록을 하나씩 적용한다. 핸들러가 소스를 닫거나 새 시도가 시작되면 나머지는 버린다.
        for (int i = 0; i < records.size(); i++) {
            if (!current(s, epoch)) {
                return;
            }
            Record r = records.get(i);
            if (r.kind == 0) {
                s.reconnectionMs = r.retry;
            } else if (r.kind == 2) {
                fail(s, epoch);
                return;
            } else {
                s.lastEventId = r.id;
                // 데이터가 없는 블록은 마지막 이벤트 ID 만 바꾼다.
                if (r.data != null) {
                    MessageEvent event = new MessageEvent(r.type, false, false);
                    event.setContent(Js.str(r.data), origin, r.id, null, null);
                    s.dispatchTrustedEvent(event);
                }
            }
        }
    }

    /**
     * Handles the end of an attempt: a network error or the end of the body reestablishes the connection, a refused answer or an error that a retry
     * cannot fix fails it.
     * <p>
     * 시도의 끝을 처리한다: 네트워크 오류나 본문의 끝은 연결을 다시 맺고, 거부된 응답이나 다시 시도해도 고쳐지지 않는 오류는 연결을 실패시킨다.
     *
     * @param s        the source / 소스
     * @param epoch    the attempt / 시도
     * @param end      the end / 끝
     * @param accepted true if the stream had been accepted / 스트림을 받아들였으면 true
     */
    private void ended(Source s, int epoch, WebResponse end, boolean accepted) {
        if (s.epoch == epoch) {
            api.untrack(s.call);
            s.call = null;
        }
        if (!current(s, epoch)) {
            return;
        }
        // 취소되었거나 다시 시도해도 소용없는 오류, 그리고 받아들이지 않은 응답은 연결을 실패시킨다. 그 밖에는 다시 맺는다.
        if (end.isError()) {
            if (end.isCancelled() || end.isFutile()) {
                fail(s, epoch);
            } else {
                reestablish(s, epoch);
            }
        } else if (!accepted) {
            fail(s, epoch);
        } else {
            reestablish(s, epoch);
        }
    }

    /**
     * Fails the connection: <code>CLOSED</code> and the <code>error</code> event, without a reconnection.
     * <p>
     * 연결을 실패시킨다: <code>CLOSED</code> 와 <code>error</code> 이벤트. 재연결하지 않는다.
     *
     * @param s     the source / 소스
     * @param epoch the attempt / 시도
     */
    private void fail(Source s, int epoch) {
        if (!current(s, epoch)) {
            return;
        }
        s.readyState = 2;
        s.epoch++;
        stop(s);
        s.dispatchTrustedEvent(new Event("error", false, false));
    }

    /**
     * Reestablishes the connection: <code>CONNECTING</code>, the <code>error</code> event and, after the reconnection time, a new attempt.
     * <p>
     * 연결을 다시 맺는다: <code>CONNECTING</code>, <code>error</code> 이벤트, 그리고 재연결 시간 뒤의 새 시도.
     *
     * @param s     the source / 소스
     * @param epoch the attempt / 시도
     */
    private void reestablish(final Source s, final int epoch) {
        s.readyState = 0;
        s.dispatchTrustedEvent(new Event("error", false, false));
        // 핸들러가 소스를 닫았으면 재연결하지 않는다.
        if (!current(s, epoch) || s.readyState != 0) {
            return;
        }
        s.timer = api.postLater(new Runnable() {
            public void run() {
                s.timer = null;
                if (current(s, epoch) && s.readyState == 0) {
                    connect(s);
                }
            }
        }, s.reconnectionMs);
    }

    /**
     * Cancels the request and the waiting reconnection of a source.
     * <p>
     * 소스의 요청과 기다리는 재연결을 취소한다.
     *
     * @param s the source / 소스
     */
    private void stop(Source s) {
        AbortApi.cancel(s.timer);
        s.timer = null;
        WebRequestSource.Call c = s.call;
        s.call = null;
        if (c != null) {
            c.cancel();
            api.untrack(c);
        }
    }

    /**
     * Installs the interface and the constructor.
     * <p>
     * 인터페이스와 생성자를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = api.bridge();
        final JsRealm realm = api.realm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of an event source.
             * <p>
             * 이벤트 소스의 인터페이스를 고른다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object target) {
                return target instanceof Source ? "EventSource" : null;
            }
        });
        DomBridge.InterfaceInfo info = bridge.defineInterface("EventSource", "EventTarget", Source.class, new String[0]);
        final JsObject proto = info.getPrototype();
        bridge.getEventHandlers().define(proto, Source.class, new String[] { "open", "message", "error" });
        NativeFunction ctor = new NativeFunction(realm, realm.getFunctionPrototype(), "EventSource", 1, new NativeFunction.Call() {
            /**
             * Refuses a call without <code>new</code>.
             * <p>
             * <code>new</code> 없는 호출을 거부한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never returns / 반환하지 않는다
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'EventSource': Please use the 'new' operator, this DOM object constructor cannot "
                        + "be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an event source and starts its connection.
             * <p>
             * 이벤트 소스를 만들고 연결을 시작한다.
             *
             * @param r         the realm / realm
             * @param args      arguments: URL and the init dictionary / 인수: URL 과 초기화 사전
             * @param newTarget the new target / new 대상
             * @return the source / 소스
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Natives.requireArgs(args, 1, "constructor", "EventSource");
                String url = resolveUrl(Natives.usv(args[0]));
                JsValue init = Js.arg(args, 1);
                boolean credentials = false;
                // 초기화 사전은 객체이거나 생략되어야 한다.
                if (!init.isNullOrUndefined()) {
                    if (!init.isObject()) {
                        throw JsException.typeError("Failed to construct 'EventSource': The provided value is not of type 'EventSourceInit'.");
                    }
                    credentials = Ops.toBoolean(((JsObject) init).get("withCredentials"));
                }
                Source s = new Source(url, credentials);
                HostObject w = bridge.wrap(s);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                connect(s);
                return w;
            }
        });
        bridge.setConstructor("EventSource", ctor);
        // 상태 상수를 인터페이스 객체와 프로토타입에 정의한다.
        for (int i = 0; i < STATES.length; i++) {
            ctor.defineData(STATES[i], Js.num(i), false, true, false);
            proto.defineData(STATES[i], Js.num(i), false, true, false);
        }
        Natives.getter(realm, proto, "url", this, "url");
        Natives.getter(realm, proto, "withCredentials", this, "withCredentials");
        Natives.getter(realm, proto, "readyState", this, "readyState");
        Natives.method(realm, proto, "close", 0, this, "close");
    }

    /**
     * Returns <code>url</code>.
     * <p>
     * <code>url</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the URL / URL
     */
    JsValue url(JsValue self, JsValue[] args) {
        return Js.str(source(self).url);
    }

    /**
     * Returns <code>withCredentials</code>.
     * <p>
     * <code>withCredentials</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the flag / 표시
     */
    JsValue withCredentials(JsValue self, JsValue[] args) {
        return Js.bool(source(self).withCredentials);
    }

    /**
     * Returns <code>readyState</code>.
     * <p>
     * <code>readyState</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the state / 상태
     */
    JsValue readyState(JsValue self, JsValue[] args) {
        return Js.num(source(self).readyState);
    }

    /**
     * Implements <code>close()</code>: cancels the request and any waiting reconnection and sets <code>CLOSED</code> (no event fires).
     * <p>
     * <code>close()</code> 를 구현한다: 요청과 기다리는 재연결을 취소하고 <code>CLOSED</code> 로 정한다(이벤트는 발생하지 않는다).
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue close(JsValue self, JsValue[] args) {
        Source s = source(self);
        s.readyState = 2;
        s.epoch++;
        stop(s);
        return Js.UNDEFINED;
    }
}
