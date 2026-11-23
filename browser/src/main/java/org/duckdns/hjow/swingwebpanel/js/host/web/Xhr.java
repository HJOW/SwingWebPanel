package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.net.ContentType;

/**
 * The state machine of one <code>XMLHttpRequest</code> (XMLHttpRequest Standard, section 3): the states unsent, opened, headers received,
 * loading and done, the author request headers, the response, and the events <code>readystatechange</code>, <code>loadstart</code>,
 * <code>progress</code>, <code>load</code>, <code>error</code>, <code>abort</code>, <code>timeout</code>, <code>loadend</code>. A request is always asynchronous
 * (synchronous requests are refused with <code>InvalidAccessError</code>, which the standard allows for pages). The network work is done by the
 * component's {@link WebRequestSource}; its results are queued to the page thread by {@link WebApis}, and every result carries the
 * <em>epoch</em> of the send that started it, so a result of a request that was aborted or reopened in the meantime is ignored. The
 * response body is delivered once, when it is complete: <code>responseText</code> is empty while the state is loading, and
 * <code>progress</code> events report the byte counts of the transfer. All methods run on the thread that owns the page's JavaScript.
 * <p>
 * <code>XMLHttpRequest</code> 하나의 상태 기계(XMLHttpRequest 표준 3절): unsent, opened, headers received, loading, done 상태, 작성자 요청 헤더, 응답, 그리고
 * <code>readystatechange</code>, <code>loadstart</code>, <code>progress</code>, <code>load</code>, <code>error</code>, <code>abort</code>, <code>timeout</code>,
 * <code>loadend</code> 이벤트. 요청은 항상 비동기이다(동기 요청은 표준이 페이지에 허용하는 대로 <code>InvalidAccessError</code> 로 거부). 네트워크 작업은 컴포넌트의
 * {@link WebRequestSource} 가 하고 결과는 {@link WebApis} 가 페이지 스레드에 줄 세우며, 모든 결과는 그것을 시작한 send 의 <em>epoch</em> 를 가지므로 그 사이
 * 중단되었거나 다시 열린 요청의 결과는 무시된다. 응답 본문은 완성되었을 때 한 번 전달된다. 상태가 loading 인 동안 <code>responseText</code> 는 비어 있고
 * <code>progress</code> 이벤트는 전송의 바이트 수를 알린다. 모든 메서드는 페이지 JavaScript 를 소유한 스레드에서 실행된다.
 */
final class Xhr extends XhrEventTarget {

    /** State: the object was constructed. / 상태: 객체가 만들어졌다. */
    static final int UNSENT = 0;

    /** State: <code>open()</code> was called. / 상태: <code>open()</code> 이 호출되었다. */
    static final int OPENED = 1;

    /** State: the headers of the response arrived. / 상태: 응답 헤더가 도착했다. */
    static final int HEADERS_RECEIVED = 2;

    /** State: the body is being received. / 상태: 본문을 받는 중이다. */
    static final int LOADING = 3;

    /** State: the transfer ended. / 상태: 전송이 끝났다. */
    static final int DONE = 4;

    /** The shortest time between two progress events in milliseconds. / 진행 이벤트 사이의 최소 간격(밀리초). */
    private static final long PROGRESS_INTERVAL_MS = 50;

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The API installer (for creating response objects). / API 설치기(응답 객체를 만들 때 쓴다). */
    private final XhrApi owner;

    /** The upload object. / 업로드 객체. */
    final XhrUpload upload = new XhrUpload();

    /** The state. / 상태. */
    private int state = UNSENT;

    /** True between send() and the end of the transfer. / send() 부터 전송이 끝날 때까지 true. */
    private boolean sendFlag;

    /** The request method. / 요청 메서드. */
    private String method = "GET";

    /** The absolute request URL. / 절대 요청 URL. */
    private String url = "";

    /** The author request headers: lower-case name to {name, combined value}. / 작성자 요청 헤더: 소문자 이름에서 {이름, 합친 값}. */
    private final Map<String, String[]> author = new LinkedHashMap<String, String[]>();

    /** The timeout in milliseconds, 0 for none. / 제한 시간(밀리초). 0 이면 없음. */
    private long timeout;

    /** The credentials flag for cross-origin requests. / 교차 오리진 요청의 자격 증명 표시. */
    private boolean withCredentials;

    /** The response type. / 응답 종류. */
    private String responseType = "";

    /** The MIME type that overrides the response's, or null. / 응답의 것을 덮어쓰는 MIME 타입. 없으면 null. */
    private String overrideMime;

    /** The response (null while there is none or after a network error). / 응답(없거나 네트워크 오류가 난 뒤에는 null). */
    private WebResponse response;

    /** The complete body received. / 받은 완전한 본문. */
    private byte[] received = new byte[0];

    /** True once the headers of the response were accepted. / 응답 헤더를 받아들인 뒤에는 true. */
    private boolean headersDone;

    /** True once the upload was reported complete. / 업로드가 끝났다고 알린 뒤에는 true. */
    private boolean uploadComplete;

    /** True if upload listeners existed when send() was called. / send() 때 업로드 리스너가 있었으면 true. */
    private boolean uploadListener;

    /** The length of the request body, for upload events. / 업로드 이벤트용 요청 본문 길이. */
    private long uploadLength;

    /** True if the timeout elapsed. / 제한 시간이 지났으면 true. */
    private boolean timedOut;

    /** The network call. / 네트워크 호출. */
    private WebRequestSource.Call call;

    /** The timeout timer. / 제한 시간 타이머. */
    private Scheduler.Handle timer;

    /** The time when send() ran. / send() 가 실행된 시각. */
    private long startedAt;

    /** The number of the current send; stale results carry an older number. / 현재 send 의 번호. 오래된 결과는 더 작은 번호를 가진다. */
    private int epoch;

    /** The cached response object of the types arraybuffer, blob, json and document. / arraybuffer, blob, json, document 형식의 캐시된 응답 객체. */
    private JsValue responseObject;

    /** True if building the response object failed. / 응답 객체 만들기가 실패했으면 true. */
    private boolean responseFailed;

    /**
     * Creates a request object in the unsent state.
     * <p>
     * unsent 상태의 요청 객체를 만든다.
     *
     * @param api   the page APIs / 페이지 API
     * @param owner the installer / 설치기
     */
    Xhr(WebApis api, XhrApi owner) {
        this.api = api;
        this.owner = owner;
    }

    // ------------------------------------------------------------------ events

    /**
     * Fires the <code>readystatechange</code> event at this object.
     * <p>
     * 이 객체에 <code>readystatechange</code> 이벤트를 발생시킨다.
     */
    private void fireReadyState() {
        dispatchTrustedEvent(new Event("readystatechange", false, false));
    }

    /**
     * Fires a progress event at a target.
     * <p>
     * 대상에 진행 이벤트를 발생시킨다.
     *
     * @param target the target / 대상
     * @param type   the event type / 이벤트 종류
     * @param loaded the transferred bytes / 전송한 바이트
     * @param length the total, 0 if unknown / 전체. 모르면 0
     */
    private void fireProgress(XhrEventTarget target, String type, long loaded, long length) {
        target.dispatchTrustedEvent(new ProgressEvent(type, false, false, length != 0, loaded, length != 0 ? length : 0));
    }

    // ------------------------------------------------------------------ request

    /**
     * Implements <code>open()</code>.
     * <p>
     * <code>open()</code> 을 구현한다.
     *
     * @param m     the method / 메서드
     * @param u     the URL text / URL 문자열
     * @param async the async flag / async 표시
     */
    void open(String m, String u, boolean async) {
        if (!HeaderRules.isToken(m)) {
            throw api.domError("SyntaxError", "Failed to execute 'open' on 'XMLHttpRequest': '" + m + "' is not a valid HTTP method.");
        }
        if (HeaderRules.isForbiddenMethod(m)) {
            throw api.domError("SecurityError", "Failed to execute 'open' on 'XMLHttpRequest': '" + m + "' HTTP method is unsupported.");
        }
        String normalized = HeaderRules.normalizeMethod(m);
        String abs;
        try {
            abs = api.resolve(u);
        } catch (IllegalArgumentException e) {
            throw api.domError("SyntaxError", "Failed to execute 'open' on 'XMLHttpRequest': Invalid URL");
        }
        if (!async) {
            throw api.domError("InvalidAccessError", "Failed to execute 'open' on 'XMLHttpRequest': Synchronous requests are not supported.");
        }
        // 진행 중이던 가져오기를 끝낸다. 그 결과는 epoch 가 달라 무시된다.
        terminate();
        sendFlag = false;
        method = normalized;
        url = abs;
        author.clear();
        uploadListener = false;
        response = null;
        received = new byte[0];
        headersDone = false;
        responseObject = null;
        responseFailed = false;
        timedOut = false;
        if (state != OPENED) {
            state = OPENED;
            fireReadyState();
        }
    }

    /**
     * Forgets the network call of a send that ended (it must not stay in the list of requests that disposal cancels).
     * <p>
     * 끝난 send 의 네트워크 호출을 잊는다(폐기 때 취소하는 요청 목록에 남아 있으면 안 된다).
     */
    private void releaseCall() {
        if (call != null) {
            api.untrack(call);
            call = null;
        }
    }

    /**
     * Stops the network call and the timer of the current send and invalidates its late results.
     * <p>
     * 현재 send 의 네트워크 호출과 타이머를 멈추고 늦은 결과를 무효로 한다.
     */
    private void terminate() {
        epoch++;
        if (call != null) {
            call.cancel();
            api.untrack(call);
            call = null;
        }
        AbortApi.cancel(timer);
        timer = null;
    }

    /**
     * Implements <code>setRequestHeader()</code>.
     * <p>
     * <code>setRequestHeader()</code> 를 구현한다.
     *
     * @param name  the header name / 헤더 이름
     * @param value the header value / 헤더 값
     */
    void setRequestHeader(String name, String value) {
        if (state != OPENED || sendFlag) {
            throw api.domError("InvalidStateError", "Failed to execute 'setRequestHeader' on 'XMLHttpRequest': The object's state must be OPENED.");
        }
        String v = HeaderRules.normalizeValue(value);
        if (!HeaderRules.isToken(name)) {
            throw api.domError("SyntaxError", "Failed to execute 'setRequestHeader' on 'XMLHttpRequest': '" + name + "' is not a valid HTTP header "
                    + "field name.");
        }
        if (!HeaderRules.isValidValue(v)) {
            throw api.domError("SyntaxError", "Failed to execute 'setRequestHeader' on 'XMLHttpRequest': '" + value + "' is not a valid HTTP header "
                    + "field value.");
        }
        if (HeaderRules.isForbiddenRequestHeader(name, v)) {
            return;
        }
        String key = name.toLowerCase();
        String[] old = author.get(key);
        // 같은 이름이 이미 있으면 값을 ", " 로 이어 붙인다.
        if (old == null) {
            author.put(key, new String[] { name, v });
        } else {
            old[1] = old[1] + ", " + v;
        }
    }

    /**
     * Implements <code>send()</code>.
     * <p>
     * <code>send()</code> 를 구현한다.
     *
     * @param data the extracted body or null / 뽑아낸 본문 또는 null
     * @param text true if the body was a string or a URLSearchParams (its charset is forced to UTF-8) / 본문이 문자열이나 URLSearchParams 이면 true(문자셋을 UTF-8 로 강제)
     */
    void send(BodyApi.BodyData data, boolean text) {
        if (state != OPENED || sendFlag) {
            throw api.domError("InvalidStateError", "Failed to execute 'send' on 'XMLHttpRequest': The object's state must be OPENED.");
        }
        if ("GET".equals(method) || "HEAD".equals(method)) {
            data = null;
        }
        if (data != null) {
            String[] ct = author.get("content-type");
            if (ct != null) {
                if (text) {
                    ct[1] = forceUtf8(ct[1]);
                }
            } else if (data.type != null) {
                author.put("content-type", new String[] { "Content-Type", data.type });
            }
        }
        WebRequestSource source = api.source();
        if (api.scheduler() == null) {
            throw api.domError("NetworkError", "Failed to execute 'send' on 'XMLHttpRequest': network access is not available");
        }
        uploadListener = upload.getEventListenerCount() > 0;
        uploadLength = data == null ? 0 : data.bytes.length;
        uploadComplete = data == null;
        timedOut = false;
        sendFlag = true;
        final int mine = ++epoch;
        startedAt = System.currentTimeMillis();
        fireProgress(this, "loadstart", 0, 0);
        if (!uploadComplete && uploadListener) {
            fireProgress(upload, "loadstart", 0, uploadLength);
        }
        // loadstart 리스너가 요청을 중단하거나 다시 열었으면 더 진행하지 않는다.
        if (state != OPENED || !sendFlag || epoch != mine) {
            return;
        }
        WebRequest req = new WebRequest(url).setMethod(method).setMode(WebRequest.MODE_CORS)
                .setCredentials(withCredentials ? WebRequest.CREDENTIALS_INCLUDE : WebRequest.CREDENTIALS_SAME_ORIGIN);
        // 작성자 헤더를 요청에 옮긴다(Content-Type 은 본문과 함께 넘긴다).
        String contentType = null;
        for (String[] h : author.values()) {
            if ("content-type".equals(h[0].toLowerCase()) && data != null) {
                contentType = h[1];
            } else {
                req.addHeader(h[0], h[1]);
            }
        }
        if (data != null) {
            req.setBody(data.bytes, contentType);
        }
        if (source == null) {
            api.post(new Runnable() {
                public void run() {
                    complete(mine, WebResponse.error("network access is not available", false));
                }
            });
        } else {
            try {
                call = source.start(req, new Callback(mine));
                api.track(call);
            } catch (RuntimeException e) {
                api.post(new Runnable() {
                    public void run() {
                        complete(mine, WebResponse.error("the request could not be started", false));
                    }
                });
            }
        }
        scheduleTimeout();
    }

    /**
     * Forces the charset parameter of a Content-Type value to UTF-8 when it names another charset.
     * <p>
     * Content-Type 값의 문자셋 매개변수가 다른 문자셋이면 UTF-8 로 바꾼다.
     *
     * @param value the header value / 헤더 값
     * @return the new value / 새 값
     */
    private static String forceUtf8(String value) {
        ContentType ct = ContentType.parse(value);
        if (ct.getMime() == null || ct.getMime().length() == 0 || ct.getCharset() == null || "utf-8".equalsIgnoreCase(ct.getCharset())) {
            return value;
        }
        return ct.getMime() + ";charset=UTF-8";
    }

    /**
     * Starts the timeout timer of the current send.
     * <p>
     * 현재 send 의 제한 시간 타이머를 시작한다.
     */
    private void scheduleTimeout() {
        AbortApi.cancel(timer);
        timer = null;
        if (timeout <= 0 || !sendFlag) {
            return;
        }
        final int mine = epoch;
        long left = Math.max(0, timeout - (System.currentTimeMillis() - startedAt));
        timer = api.postLater(new Runnable() {
            public void run() {
                if (epoch == mine && sendFlag && state != DONE) {
                    timedOut = true;
                    if (call != null) {
                        call.cancel();
                        api.untrack(call);
                        call = null;
                    }
                    epoch++;
                    requestError("timeout");
                }
            }
        }, left);
    }

    /**
     * Implements the <code>abort()</code> operation: cancels the transfer and fires the abort events.
     * <p>
     * <code>abort()</code> 연산을 구현한다: 전송을 취소하고 abort 이벤트를 발생시킨다.
     */
    void abort() {
        boolean active = (state == OPENED && sendFlag) || state == HEADERS_RECEIVED || state == LOADING;
        terminate();
        if (active) {
            requestError("abort");
        }
        if (state == DONE) {
            state = UNSENT;
            response = null;
        }
    }

    /**
     * The request error steps: the transfer ended without a response.
     * <p>
     * 요청 오류 단계: 응답 없이 전송이 끝났다.
     *
     * @param type the event type: error, abort or timeout / 이벤트 종류: error, abort, timeout
     */
    private void requestError(String type) {
        state = DONE;
        sendFlag = false;
        response = null;
        AbortApi.cancel(timer);
        timer = null;
        fireReadyState();
        if (!uploadComplete) {
            uploadComplete = true;
            if (uploadListener) {
                fireProgress(upload, type, 0, 0);
                fireProgress(upload, "loadend", 0, 0);
            }
        }
        fireProgress(this, type, 0, 0);
        fireProgress(this, "loadend", 0, 0);
    }

    // ------------------------------------------------------------------ results of the network layer

    /**
     * Receives the results of one send on a request thread and queues them to the page thread.
     * <p>
     * 한 send 의 결과를 요청 스레드에서 받아 페이지 스레드에 줄 세운다.
     */
    private final class Callback implements WebRequestSource.Callback {
        /** The epoch of the send. / 이 send 의 epoch. */
        private final int mine;

        /** The time of the last queued progress event. / 마지막으로 줄 세운 진행 이벤트의 시각. */
        private volatile long last;

        /**
         * Creates the receiver.
         * <p>
         * 받는 쪽을 만든다.
         *
         * @param mine the epoch / epoch
         */
        Callback(int mine) {
            this.mine = mine;
        }

        /**
         * Queues the arrival of the response headers.
         * <p>
         * 응답 헤더의 도착을 줄 세운다.
         *
         * @param head the response without its body / 본문 없는 응답
         */
        public void onHeaders(final WebResponse head) {
            api.post(new Runnable() {
                public void run() {
                    headers(mine, head);
                }
            });
        }

        /**
         * Queues a progress report (at most one per interval).
         * <p>
         * 진행 보고를 줄 세운다(간격마다 최대 하나).
         *
         * @param loaded the bytes received / 받은 바이트
         * @param total  the declared length or -1 / 선언된 길이 또는 -1
         */
        public void onProgress(final long loaded, final long total) {
            long now = System.currentTimeMillis();
            if (now - last < PROGRESS_INTERVAL_MS) {
                return;
            }
            last = now;
            api.post(new Runnable() {
                public void run() {
                    progress(mine, loaded, total);
                }
            });
        }

        /**
         * Queues the end of the request.
         * <p>
         * 요청의 끝을 줄 세운다.
         *
         * @param done the final response / 최종 응답
         */
        public void onComplete(final WebResponse done) {
            api.post(new Runnable() {
                public void run() {
                    complete(mine, done);
                }
            });
        }
    }

    /**
     * Reports to the upload object that the request body was sent.
     * <p>
     * 업로드 객체에 요청 본문 전송이 끝났음을 알린다.
     */
    private void finishUpload() {
        if (uploadComplete) {
            return;
        }
        uploadComplete = true;
        if (uploadListener) {
            fireProgress(upload, "progress", uploadLength, uploadLength);
            fireProgress(upload, "load", uploadLength, uploadLength);
            fireProgress(upload, "loadend", uploadLength, uploadLength);
        }
    }

    /**
     * Handles the arrival of the headers.
     * <p>
     * 헤더의 도착을 처리한다.
     *
     * @param mine the epoch of the send / send 의 epoch
     * @param head the response head / 응답 머리
     */
    private void headers(int mine, WebResponse head) {
        if (mine != epoch || !sendFlag || state != OPENED || head.isError()) {
            return;
        }
        response = head;
        headersDone = true;
        finishUpload();
        if (mine != epoch || !sendFlag) {
            return;
        }
        state = HEADERS_RECEIVED;
        fireReadyState();
    }

    /**
     * Handles a progress report.
     * <p>
     * 진행 보고를 처리한다.
     *
     * @param mine   the epoch of the send / send 의 epoch
     * @param loaded the bytes received / 받은 바이트
     * @param total  the declared length or -1 / 선언된 길이 또는 -1
     */
    private void progress(int mine, long loaded, long total) {
        if (mine != epoch || !sendFlag || (state != HEADERS_RECEIVED && state != LOADING)) {
            return;
        }
        state = LOADING;
        fireReadyState();
        if (mine == epoch && sendFlag) {
            fireProgress(this, "progress", loaded, total > 0 ? total : 0);
        }
    }

    /**
     * Handles the end of the request.
     * <p>
     * 요청의 끝을 처리한다.
     *
     * @param mine the epoch of the send / send 의 epoch
     * @param done the final response / 최종 응답
     */
    private void complete(int mine, WebResponse done) {
        if (mine != epoch || !sendFlag) {
            return;
        }
        if (done.isError()) {
            releaseCall();
            requestError(timedOut ? "timeout" : "error");
            return;
        }
        if (!headersDone) {
            response = done;
            headersDone = true;
            finishUpload();
            if (mine != epoch || !sendFlag) {
                return;
            }
            state = HEADERS_RECEIVED;
            fireReadyState();
            if (mine != epoch || !sendFlag) {
                return;
            }
        }
        response = done;
        received = done.getBody();
        // 선언된 길이가 압축 전송 크기여서 받은 바이트보다 작을 수 있으므로 그런 경우에는 받은 바이트 수를 전체로 쓴다.
        long length = done.getDeclaredLength() >= received.length && done.getDeclaredLength() > 0 ? done.getDeclaredLength() : received.length;
        // 아직 loading 에 이르지 않았으면 본문이 도착했음을 알리는 loading 단계와 progress 를 거친다.
        if (state == HEADERS_RECEIVED) {
            state = LOADING;
            fireReadyState();
            if (mine != epoch || !sendFlag) {
                return;
            }
            if (received.length > 0) {
                fireProgress(this, "progress", received.length, length);
                if (mine != epoch || !sendFlag) {
                    return;
                }
            }
        }
        releaseCall();
        AbortApi.cancel(timer);
        timer = null;
        fireProgress(this, "progress", received.length, length);
        state = DONE;
        sendFlag = false;
        fireReadyState();
        fireProgress(this, "load", received.length, length);
        fireProgress(this, "loadend", received.length, length);
    }

    // ------------------------------------------------------------------ attributes

    /**
     * Returns <code>readyState</code>.
     * <p>
     * <code>readyState</code> 를 반환한다.
     *
     * @return the state / 상태
     */
    int getReadyState() {
        return state;
    }

    /**
     * Returns <code>timeout</code>.
     * <p>
     * <code>timeout</code> 을 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    long getTimeout() {
        return timeout;
    }

    /**
     * Sets <code>timeout</code>; a running request gets its timer rescheduled relative to its start.
     * <p>
     * <code>timeout</code> 을 지정한다. 실행 중인 요청은 시작 시점 기준으로 타이머를 다시 예약한다.
     *
     * @param ms milliseconds / 밀리초
     */
    void setTimeout(long ms) {
        timeout = ms;
        if (sendFlag) {
            scheduleTimeout();
        }
    }

    /**
     * Returns <code>withCredentials</code>.
     * <p>
     * <code>withCredentials</code> 를 반환한다.
     *
     * @return the flag / 표시
     */
    boolean getWithCredentials() {
        return withCredentials;
    }

    /**
     * Sets <code>withCredentials</code>.
     * <p>
     * <code>withCredentials</code> 를 지정한다.
     *
     * @param v the flag / 표시
     */
    void setWithCredentials(boolean v) {
        if ((state != UNSENT && state != OPENED) || sendFlag) {
            throw api.domError("InvalidStateError", "Failed to set the 'withCredentials' property on 'XMLHttpRequest': The value may only be set if "
                    + "the object's state is UNSENT or OPENED.");
        }
        withCredentials = v;
    }

    /**
     * Returns <code>responseType</code>.
     * <p>
     * <code>responseType</code> 을 반환한다.
     *
     * @return the type / 종류
     */
    String getResponseType() {
        return responseType;
    }

    /**
     * Sets <code>responseType</code>.
     * <p>
     * <code>responseType</code> 을 지정한다.
     *
     * @param type the type / 종류
     */
    void setResponseType(String type) {
        if (state == LOADING || state == DONE) {
            throw api.domError("InvalidStateError", "Failed to set the 'responseType' property on 'XMLHttpRequest': The response type cannot be "
                    + "set if the object's state is LOADING or DONE.");
        }
        responseType = type;
    }

    /**
     * Returns <code>status</code>.
     * <p>
     * <code>status</code> 를 반환한다.
     *
     * @return the HTTP status or 0 / HTTP 상태 또는 0
     */
    int getStatus() {
        return response == null || state < HEADERS_RECEIVED ? 0 : response.getStatus();
    }

    /**
     * Returns <code>statusText</code>.
     * <p>
     * <code>statusText</code> 를 반환한다.
     *
     * @return the reason phrase / 이유 문구
     */
    String getStatusText() {
        return response == null || state < HEADERS_RECEIVED ? "" : response.getStatusText();
    }

    /**
     * Returns <code>responseURL</code>.
     * <p>
     * <code>responseURL</code> 을 반환한다.
     *
     * @return the final URL without the fragment / 프래그먼트 없는 최종 URL
     */
    String getResponseUrl() {
        if (response == null || state < HEADERS_RECEIVED) {
            return "";
        }
        String u = response.getUrl();
        int hash = u.indexOf('#');
        return hash < 0 ? u : u.substring(0, hash);
    }

    /**
     * Implements <code>getResponseHeader()</code>.
     * <p>
     * <code>getResponseHeader()</code> 를 구현한다.
     *
     * @param name the header name / 헤더 이름
     * @return the combined value or null / 합친 값 또는 null
     */
    String getResponseHeader(String name) {
        if (response == null || state < HEADERS_RECEIVED || !HeaderRules.isToken(name)) {
            return null;
        }
        String n = name.toLowerCase();
        StringBuilder out = null;
        List<String[]> list = response.getHeaders();
        // 이름이 같은 헤더의 값을 순서대로 ", " 로 잇는다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[0].equals(n)) {
                if (out == null) {
                    out = new StringBuilder(list.get(i)[1]);
                } else {
                    out.append(", ").append(list.get(i)[1]);
                }
            }
        }
        return out == null ? null : out.toString();
    }

    /**
     * Implements <code>getAllResponseHeaders()</code>.
     * <p>
     * <code>getAllResponseHeaders()</code> 를 구현한다.
     *
     * @return the headers as text / 텍스트로 된 헤더
     */
    String getAllResponseHeaders() {
        if (response == null || state < HEADERS_RECEIVED) {
            return "";
        }
        List<String> names = new ArrayList<String>();
        List<String[]> list = response.getHeaders();
        // 이름의 중복을 없앤다.
        for (int i = 0; i < list.size(); i++) {
            if (!names.contains(list.get(i)[0])) {
                names.add(list.get(i)[0]);
            }
        }
        Collections.sort(names, new Comparator<String>() {
            public int compare(String a, String b) {
                return a.toUpperCase().compareTo(b.toUpperCase());
            }
        });
        StringBuilder out = new StringBuilder();
        // 이름마다 합친 값을 "이름: 값" 줄로 쓴다.
        for (int i = 0; i < names.size(); i++) {
            out.append(names.get(i)).append(": ").append(getResponseHeader(names.get(i))).append("\r\n");
        }
        return out.toString();
    }

    /**
     * Implements <code>overrideMimeType()</code>.
     * <p>
     * <code>overrideMimeType()</code> 을 구현한다.
     *
     * @param mime the MIME type text / MIME 타입 문자열
     */
    void overrideMimeType(String mime) {
        if (state == LOADING || state == DONE) {
            throw api.domError("InvalidStateError", "Failed to execute 'overrideMimeType' on 'XMLHttpRequest': MimeType cannot be overridden when "
                    + "the state is LOADING or DONE.");
        }
        ContentType ct = ContentType.parse(mime);
        overrideMime = ct.getMime() == null || ct.getMime().length() == 0 ? "application/octet-stream"
                : (ct.getCharset() == null ? ct.getMime() : ct.getMime() + ";charset=" + ct.getCharset());
    }

    // ------------------------------------------------------------------ response body

    /**
     * Returns the final MIME type: the override or the type of the response (text/xml if the response has none).
     * <p>
     * 최종 MIME 타입을 반환한다: 덮어쓴 타입 또는 응답의 타입(응답에 없으면 text/xml).
     *
     * @return the type with an optional charset parameter / 문자셋 매개변수가 있을 수 있는 타입
     */
    private String finalMime() {
        if (overrideMime != null) {
            return overrideMime;
        }
        String raw = response == null ? null : response.getHeader("content-type");
        if (raw == null) {
            return "text/xml";
        }
        ContentType ct = ContentType.parse(raw);
        return ct.getMime() == null || ct.getMime().length() == 0 ? "text/xml" : ct.getMime();
    }

    /**
     * Returns the charset label of the response (an override wins), or null.
     * <p>
     * 응답의 문자셋 레이블을 반환한다(덮어쓴 것이 우선). 없으면 null.
     *
     * @return the label or null / 레이블 또는 null
     */
    private String finalCharset() {
        String label = null;
        String raw = response == null ? null : response.getHeader("content-type");
        if (raw != null) {
            label = ContentType.parse(raw).getCharset();
        }
        if (overrideMime != null) {
            String o = ContentType.parse(overrideMime).getCharset();
            if (o != null) {
                label = o;
            }
        }
        return label;
    }

    /**
     * Returns the text response: the received bytes decoded with the charset of the response, UTF-8 by default.
     * <p>
     * 텍스트 응답을 반환한다: 받은 바이트를 응답의 문자셋(기본 UTF-8)으로 디코딩한 것.
     *
     * @return the text / 텍스트
     */
    private String textResponse() {
        if (response == null || received.length == 0) {
            return "";
        }
        return Encodings.decode(received, finalCharset());
    }

    /**
     * Implements <code>responseText</code>.
     * <p>
     * <code>responseText</code> 를 구현한다.
     *
     * @return the text / 텍스트
     */
    String getResponseText() {
        if (!"".equals(responseType) && !"text".equals(responseType)) {
            throw api.domError("InvalidStateError", "Failed to read the 'responseText' property from 'XMLHttpRequest': The value is only accessible "
                    + "if the object's 'responseType' is '' or 'text' (was '" + responseType + "').");
        }
        if (state != LOADING && state != DONE) {
            return "";
        }
        // loading 동안에는 본문이 아직 한꺼번에 오지 않았으므로 비어 있다(완성된 본문만 전달한다).
        return state == DONE ? textResponse() : "";
    }

    /**
     * Implements <code>response</code>.
     * <p>
     * <code>response</code> 를 구현한다.
     *
     * @return the response as the response type says / 응답 종류에 따른 응답
     */
    JsValue getResponse() {
        if ("".equals(responseType) || "text".equals(responseType)) {
            return Js.str(state == DONE ? textResponse() : "");
        }
        if (state != DONE) {
            return Js.NULL;
        }
        if (responseFailed) {
            return Js.NULL;
        }
        if (responseObject != null) {
            return responseObject;
        }
        try {
            responseObject = buildResponseObject();
        } catch (JsException e) {
            responseFailed = true;
            return Js.NULL;
        }
        return responseObject;
    }

    /**
     * Builds the response object for the types arraybuffer, blob, json and document.
     * <p>
     * arraybuffer, blob, json, document 형식의 응답 객체를 만든다.
     *
     * @return the object, or null if there is none / 객체. 없으면 null
     */
    private JsValue buildResponseObject() {
        if (response == null) {
            return Js.NULL;
        }
        if ("arraybuffer".equals(responseType)) {
            return TypedArrays.newArrayBufferCopy(api.realm(), received, 0, received.length);
        }
        if ("blob".equals(responseType)) {
            String mime = finalMime();
            String charset = finalCharset();
            // blob 의 타입은 최종 MIME 타입에 문자셋 매개변수를 붙인 직렬화 형태이다. 응답에 Content-Type 이 없으면 빈 타입이다.
            String type = charset == null ? mime : mime + ";charset=" + charset.toLowerCase();
            return api.blobs.newBlob(received, response.getHeader("content-type") == null && overrideMime == null ? "" : type);
        }
        if ("json".equals(responseType)) {
            if (received.length == 0) {
                return Js.NULL;
            }
            return Ops.invoke(api.realm(), api.realm().getIntrinsic("%JSON%"), "parse", new JsValue[] { Js.str(Encodings.utf8Decode(received)) });
        }
        return owner.documentOf(this, received, finalMime(), finalCharset(), response.getUrl());
    }

    /**
     * Implements <code>responseXML</code>.
     * <p>
     * <code>responseXML</code> 를 구현한다.
     *
     * @return the document or null / 문서 또는 null
     */
    JsValue getResponseXml() {
        if (!"".equals(responseType) && !"document".equals(responseType)) {
            throw api.domError("InvalidStateError", "Failed to read the 'responseXML' property from 'XMLHttpRequest': The value is only accessible "
                    + "if the object's 'responseType' is '' or 'document' (was '" + responseType + "').");
        }
        if (state != DONE || response == null) {
            return Js.NULL;
        }
        if (responseObject != null && "document".equals(responseType)) {
            return responseObject;
        }
        String mime = finalMime();
        boolean xml = mime.endsWith("xml");
        boolean html = "text/html".equals(mime);
        // 응답 종류가 빈 문자열이면 HTML 은 문서로 만들지 않는다(웹 호환).
        if (!(xml || (html && "document".equals(responseType)))) {
            return Js.NULL;
        }
        JsValue doc = owner.documentOf(this, received, mime, finalCharset(), response.getUrl());
        if ("document".equals(responseType)) {
            responseObject = doc;
        }
        return doc;
    }
}
