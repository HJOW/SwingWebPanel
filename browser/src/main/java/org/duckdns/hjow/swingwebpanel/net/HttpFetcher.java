package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.SequenceInputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

/**
 * HTTP and HTTPS client built on <code>HttpURLConnection</code> with everything the browser needs done by hand:
 * <ul>
 * <li><b>Request layer</b>: the same code path adds the User-Agent, the component's extra headers, cookies, referrer,
 * <code>Origin</code> and cache headers for main documents and for every sub-resource (images, audio, CSS, scripts).</li>
 * <li><b>Redirects</b> are followed manually (relative <code>Location</code>, loops, a maximum count, POST turns into GET on
 * 301/302/303, 307/308 keep method and body, http&lt;-&gt;https switches allowed, only http(s) targets are allowed from http).
 * The effective final URL is reported. <code>Authorization</code> and <code>Proxy-Authorization</code> extra headers are
 * <b>not</b> forwarded when the origin changes or after an https-to-http downgrade; other extra headers are.</li>
 * <li><b>Bodies</b> are decompressed (gzip, deflate; only these are advertised), limited in memory
 * ({@link FetchLimits#getMaxMemoryBytes()}) and checked against Content-Length (a short body is {@link FailureKind#TRUNCATED});
 * error statuses keep their body. A {@link ResponseHandler} can redirect the body to a stream without a size limit.</li>
 * <li><b>SSL</b>: {@link SslConfigurer#configureSsl(HttpsURLConnection)} runs for every https connection. If an https
 * attempt fails at connect or TLS level (never for an HTTP error status), one retry is made over http to the same host
 * (default port 443 becomes 80, an explicit other port is kept). The result reports the real http URL and
 * {@link ResourceResponse#isHttpFallbackUsed()}. Secure cookies are naturally not sent on the plain request and the sensitive
 * extra headers above are dropped. Certificate and host name errors also count as TLS failures, as the project requires.</li>
 * <li><b>CORS</b>: a request that carries <code>Origin</code> (a cross-origin request of a CORS purpose, every POST/PUT/PATCH/DELETE with an
 * initiating origin) is sent through the socket based {@link SocketHttpURLConnection}/{@link SocketHttpsURLConnection} because the JRE
 * drops that restricted header (not when a JVM proxy applies). A cross-origin request with a method or a page header that is not
 * safelisted first gets a preflight (<code>OPTIONS</code>, see {@link OriginPolicy#checkPreflight}); a denial is {@link FailureKind#CORS}
 * and the request is not sent.</li>
 * <li><b>Cancellation</b>: the request's {@link CancelToken} disconnects the connection; streams and connections are closed on
 * every path.</li>
 * </ul>
 * No JVM-wide state is changed (no default cookie handler, authenticator, SSL context or host name verifier). A fetch never
 * throws for network problems; it returns a {@link ResourceResponse} with a {@link FailureKind}. Instances are thread safe;
 * one fetch runs in the calling thread.
 * <p>
 * <code>HttpURLConnection</code> 위에 만든 HTTP/HTTPS 클라이언트로, 브라우저가 필요로 하는 것을 직접 처리한다.
 * <ul>
 * <li><b>요청 계층</b>: 주 문서와 모든 하위 리소스(이미지, 오디오, CSS, 스크립트)에 같은 코드 경로로 User-Agent, 컴포넌트의 추가
 * 헤더, 쿠키, 참조 페이지, <code>Origin</code>, 캐시 헤더를 붙인다.</li>
 * <li><b>리디렉션</b>은 직접 따라간다(상대 <code>Location</code>, 순환, 최대 횟수, 301/302/303 에서 POST 는 GET 으로, 307/308 은
 * 메서드와 본문 유지, http&lt;-&gt;https 전환 허용, http 에서는 http(s) 대상만 허용). 최종 유효 URL 을 보고한다. 오리진이 바뀌거나
 * https 에서 http 로 내려가면 <code>Authorization</code>, <code>Proxy-Authorization</code> 추가 헤더는 전달하지 <b>않으며</b>
 * 다른 추가 헤더는 전달한다.</li>
 * <li><b>본문</b>은 압축을 풀고(gzip, deflate 만 광고), 메모리 크기를 제한({@link FetchLimits#getMaxMemoryBytes()})하며
 * Content-Length 와 대조한다(짧으면 {@link FailureKind#TRUNCATED}). 오류 상태도 본문을 유지한다. {@link ResponseHandler} 로 본문을
 * 크기 제한 없는 스트림으로 보낼 수 있다.</li>
 * <li><b>SSL</b>: 모든 https 연결에 {@link SslConfigurer#configureSsl(HttpsURLConnection)} 를 호출한다. https 시도가 연결 또는 TLS
 * 수준에서 실패하면(HTTP 오류 상태는 해당 없음) 같은 호스트로 http 재시도를 한 번 한다(기본 443 포트는 80 으로, 명시한 다른 포트는
 * 유지). 결과에는 실제 http URL 과 {@link ResourceResponse#isHttpFallbackUsed()} 가 보고된다. 평문 요청에는 Secure 쿠키가 자연히
 * 가지 않고 위의 민감한 추가 헤더도 빠진다. 프로젝트 요구대로 인증서/호스트 이름 오류도 TLS 실패로 본다.</li>
 * <li><b>CORS</b>: <code>Origin</code> 을 싣는 요청(CORS 목적의 교차 오리진 요청, 시작한 오리진이 있는 모든 POST/PUT/PATCH/DELETE)과 JRE 가 거부하는 PATCH 는 JRE 가 그 제한 헤더를
 * 버리므로 소켓 기반 {@link SocketHttpURLConnection}/{@link SocketHttpsURLConnection} 으로 보낸다(JVM 프록시가 적용되면 제외). 안전 목록에 없는 메서드나
 * 페이지 헤더를 가진 교차 오리진 요청은 먼저 사전 요청(<code>OPTIONS</code>, {@link OriginPolicy#checkPreflight} 참조)을 받으며, 거부되면
 * {@link FailureKind#CORS} 이고 요청은 보내지 않는다.</li>
 * <li><b>취소</b>: 요청의 {@link CancelToken} 이 연결을 끊는다. 스트림과 연결은 모든 경로에서 닫는다.</li>
 * </ul>
 * JVM 전역 상태(기본 쿠키 처리기, 인증기, SSL 컨텍스트, 호스트 이름 검증기)는 바꾸지 않는다. 가져오기는 네트워크 문제로 예외를
 * 던지지 않고 {@link FailureKind} 가 있는 {@link ResourceResponse} 를 돌려준다. 인스턴스는 스레드 안전하며 가져오기 한 번은
 * 호출한 스레드에서 실행한다.
 */
public class HttpFetcher {

    /**
     * Loop result: the response is complete (success or failure recorded).
     * <p>
     * 반복 결과: 응답이 완성되었다(성공 또는 실패가 기록됨).
     */
    private static final int DONE = 0;

    /**
     * Loop result: another request must be sent (redirect or http retry).
     * <p>
     * 반복 결과: 요청을 한 번 더 보내야 한다(리디렉션 또는 http 재시도).
     */
    private static final int CONTINUE = 1;

    /**
     * Extra header names the request layer decides itself (lower case).
     * <p>
     * 요청 계층이 직접 정하므로 추가 헤더로 받지 않는 이름(소문자).
     */
    private static final Set<String> MANAGED = new HashSet<String>();

    /**
     * Extra header names that are not forwarded across origins or to plain http after a downgrade (lower case).
     * <p>
     * 오리진이 바뀌거나 http 로 내려간 뒤에는 전달하지 않는 추가 헤더 이름(소문자).
     */
    private static final Set<String> SENSITIVE = new HashSet<String>();

    static {
        String[] managed = { "host", "content-length", "connection", "transfer-encoding", "upgrade", "expect", "te", "trailer",
            "cookie", "user-agent", "accept-encoding" };
        // 요청 계층이 직접 관리하는 헤더 이름을 집합에 넣는다.
        for (int i = 0; i < managed.length; i++) {
            MANAGED.add(managed[i]);
        }
        SENSITIVE.add("authorization");
        SENSITIVE.add("proxy-authorization");
    }

    /**
     * SSL hook invoked for every https connection.
     * <p>
     * 모든 https 연결에 호출하는 SSL 훅.
     */
    private final SslConfigurer ssl;
    /**
     * Timeouts and size limits.
     * <p>
     * 제한 시간과 크기 제한.
     */
    private final FetchLimits limits;
    /**
     * Whether the one-time http retry after a failed https attempt is enabled.
     * <p>
     * https 실패 뒤의 일회성 http 재시도를 쓰는지 여부.
     */
    private volatile boolean httpFallbackEnabled = true;

    /**
     * The largest number of remembered preflight grants.
     * <p>
     * 기억하는 사전 요청 허락의 최대 개수.
     */
    private static final int MAX_PREFLIGHT_ENTRIES = 256;

    /**
     * A remembered preflight grant and the time (milliseconds) until which it may be reused.
     * <p>
     * 기억한 사전 요청 허락과 그것을 다시 쓸 수 있는 시각(밀리초).
     */
    private static final class PreflightEntry {
        /** The grant. / 허락. */
        final OriginPolicy.PreflightGrant grant;
        /** The time until which it may be reused. / 다시 쓸 수 있는 시각. */
        final long expires;

        /**
         * Creates the entry.
         * <p>
         * 항목을 만든다.
         *
         * @param grant   the grant / 허락
         * @param expires the time until which it may be reused / 다시 쓸 수 있는 시각
         */
        PreflightEntry(OriginPolicy.PreflightGrant grant, long expires) {
            this.grant = grant;
            this.expires = expires;
        }
    }

    /**
     * The remembered preflight grants of this fetcher by origin, URL and credentials (oldest entries leave first). Guarded by itself.
     * <p>
     * 이 가져오기 객체가 기억하는 사전 요청 허락(오리진, URL, 자격 증명별. 오래된 것부터 나간다). 자신을 잠금으로 쓴다.
     */
    private final Map<String, PreflightEntry> preflightGrants = new LinkedHashMap<String, PreflightEntry>() {
        private static final long serialVersionUID = 1L;

        /**
         * Removes the oldest entry when there are too many.
         * <p>
         * 너무 많으면 가장 오래된 항목을 지운다.
         *
         * @param eldest the oldest entry / 가장 오래된 항목
         * @return true to remove it / 지우려면 true
         */
        protected boolean removeEldestEntry(Map.Entry<String, PreflightEntry> eldest) {
            return size() > MAX_PREFLIGHT_ENTRIES;
        }
    };

    /**
     * Creates a fetcher.
     * <p>
     * 가져오기 객체를 만든다.
     *
     * @param ssl    SSL hook, null for {@link DefaultSslConfigurer} / SSL 훅. null 이면 {@link DefaultSslConfigurer}
     * @param limits limits, null for {@link FetchLimits#DEFAULT} / 제한. null 이면 {@link FetchLimits#DEFAULT}
     */
    public HttpFetcher(SslConfigurer ssl, FetchLimits limits) {
        this.ssl = ssl == null ? DefaultSslConfigurer.INSTANCE : ssl;
        this.limits = limits == null ? FetchLimits.DEFAULT : limits;
    }

    /**
     * Returns the limits in use.
     * <p>
     * 사용 중인 제한을 돌려준다.
     *
     * @return limits / 제한
     */
    public FetchLimits getLimits() {
        return limits;
    }

    /**
     * Enables or disables the http retry after a failed https attempt (enabled by default as the project requires).
     * <p>
     * https 실패 뒤의 http 재시도를 켜거나 끈다(프로젝트 요구에 따라 기본은 켜짐).
     *
     * @param enabled true to retry over http / http 로 재시도하려면 true
     */
    public void setHttpFallbackEnabled(boolean enabled) {
        this.httpFallbackEnabled = enabled;
    }

    /**
     * Tells whether the http retry is enabled.
     * <p>
     * http 재시도가 켜져 있는지 알려 준다.
     *
     * @return true if enabled / 켜져 있으면 true
     */
    public boolean isHttpFallbackEnabled() {
        return httpFallbackEnabled;
    }

    /**
     * Mutable state of one fetch.
     * <p>
     * 가져오기 한 번의 가변 상태.
     */
    private static final class State {
        /**
         * The request.
         * <p>
         * 요청.
         */
        final ResourceRequest req;
        /**
         * Optional body handler.
         * <p>
         * 선택적 본문 처리기.
         */
        final ResponseHandler handler;
        /**
         * The response being built.
         * <p>
         * 만들고 있는 응답.
         */
        final ResourceResponse result;
        /**
         * URL of the next/current request without fragment.
         * <p>
         * 다음/현재 요청의 프래그먼트 없는 URL.
         */
        String current;
        /**
         * Fragment to report in the final URL or null.
         * <p>
         * 최종 URL 에 보고할 프래그먼트 또는 null.
         */
        String fragment;
        /**
         * Current method.
         * <p>
         * 현재 메서드.
         */
        String method;
        /**
         * Current body or null.
         * <p>
         * 현재 본문 또는 null.
         */
        byte[] body;
        /**
         * Current body content type.
         * <p>
         * 현재 본문 Content-Type.
         */
        String bodyType;
        /**
         * Number of redirects followed.
         * <p>
         * 따라간 리디렉션 수.
         */
        int redirects;
        /**
         * Whether the chain went through an https-to-http fallback.
         * <p>
         * 연쇄가 https 에서 http 로의 대체 접속을 거쳤는지 여부.
         */
        boolean downgraded;
        /**
         * Whether the body is being read (failures then mean truncation).
         * <p>
         * 본문을 읽는 중인지 여부(이때의 실패는 잘림을 뜻함).
         */
        boolean inBody;
        /**
         * Origin of the first URL.
         * <p>
         * 첫 URL 의 오리진.
         */
        Origin firstOrigin;
        /**
         * Visited method+URL keys for loop detection.
         * <p>
         * 순환 감지를 위한 방문한 메서드+URL 키.
         */
        final Set<String> visited = new HashSet<String>();

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param req     request / 요청
         * @param handler body handler / 본문 처리기
         * @param result  response / 응답
         */
        State(ResourceRequest req, ResponseHandler handler, ResourceResponse result) {
            this.req = req;
            this.handler = handler;
            this.result = result;
        }
    }

    /**
     * Fetches an http or https URL. Never throws for network problems.
     * <p>
     * http 또는 https URL 을 가져온다. 네트워크 문제로 예외를 던지지 않는다.
     *
     * @param req     the request / 요청
     * @param handler decides where bodies go, may be null (everything in memory) / 본문의 목적지를 정한다. null 이면 모두 메모리
     * @return the response / 응답
     */
    public ResourceResponse fetch(ResourceRequest req, ResponseHandler handler) {
        String requestUrl = req.getUrl();
        ResourceResponse result = new ResourceResponse(requestUrl);
        SchemePolicy.Kind kind = SchemePolicy.classify(requestUrl);
        if (kind == SchemePolicy.Kind.INVALID) {
            result.setFailure(FailureKind.INVALID_URL, "not an absolute url: " + requestUrl);
            return result;
        }
        if (kind != SchemePolicy.Kind.HTTP && kind != SchemePolicy.Kind.HTTPS) {
            result.setFailure(FailureKind.BLOCKED_SCHEME, "scheme is not allowed here: " + requestUrl);
            return result;
        }
        String normalized;
        try {
            normalized = UrlUtil.normalize(requestUrl);
        } catch (IllegalArgumentException e) {
            result.setFailure(FailureKind.INVALID_URL, e.getMessage());
            return result;
        }
        State st = new State(req, handler, result);
        st.fragment = UrlUtil.getFragment(normalized);
        st.current = UrlUtil.stripFragment(normalized);
        st.method = req.getMethod();
        boolean hasBody = "POST".equals(st.method) || "PUT".equals(st.method) || "PATCH".equals(st.method);
        st.body = hasBody ? req.getBody() : null;
        st.bodyType = req.getBodyContentType();
        st.firstOrigin = Origin.of(st.current);
        result.setFinalUrl(finalUrl(st));
        // 응답이 완성될 때까지(성공 또는 실패) 요청을 보내고 리디렉션/재시도를 따라간다.
        while (step(st) == CONTINUE) {
            // step 이 상태를 갱신하므로 반복만 한다.
        }
        return result;
    }

    /**
     * Convenience: fetches into memory.
     * <p>
     * 편의 메서드: 메모리로 가져온다.
     *
     * @param req the request / 요청
     * @return the response / 응답
     */
    public ResourceResponse fetch(ResourceRequest req) {
        return fetch(req, null);
    }

    /**
     * Builds the URL to report (with the fragment).
     * <p>
     * 보고할 URL 을 만든다(프래그먼트 포함).
     *
     * @param st state / 상태
     * @return final URL text / 최종 URL 문자열
     */
    private static String finalUrl(State st) {
        return st.fragment == null ? st.current : st.current + "#" + st.fragment;
    }

    /**
     * Records a failure in the result.
     * <p>
     * 결과에 실패를 기록한다.
     *
     * @param st   state / 상태
     * @param kind failure kind / 실패 종류
     * @param msg  message / 메시지
     * @return {@link #DONE}
     */
    private static int fail(State st, FailureKind kind, String msg) {
        st.result.setFinalUrl(finalUrl(st));
        st.result.setFailure(kind, msg);
        st.result.setBody(null);
        return DONE;
    }

    /**
     * Sends one request and handles its response.
     * <p>
     * 요청 하나를 보내고 응답을 처리한다.
     *
     * @param st state / 상태
     * @return {@link #DONE} or {@link #CONTINUE} / {@link #DONE} 또는 {@link #CONTINUE}
     */
    private int step(State st) {
        CancelToken token = st.req.getCancelToken();
        if (token.isCancelled()) {
            return fail(st, FailureKind.CANCELLED, "request cancelled");
        }
        // worker 진입 모듈은 다른 오리진으로 이동한 구간에 연결하지 않는다.
        Origin initiator = st.req.getInitiator();
        if (st.req.getPurpose() == RequestPurpose.WORKER_MODULE
                && (initiator == null || !initiator.isSameOrigin(Origin.of(st.current)))) {
            return fail(st, FailureKind.CORS, "worker module entry must be same-origin");
        }
        if (!st.visited.add(st.method + " " + st.current)) {
            return fail(st, FailureKind.REDIRECT_LOOP, "redirect loop at " + st.current);
        }
        HttpURLConnection conn = null;
        CancelToken.Hook hook = null;
        boolean keep = false;
        st.inBody = false;
        try {
            // 교차 오리진의 단순하지 않은 요청이면 본 요청에 앞서 CORS 사전 요청으로 서버의 허락을 받는다.
            String denied = preflightDenial(st);
            if (denied != null) {
                return fail(st, FailureKind.CORS, denied);
            }
            boolean streamed = st.req.isStreaming() || st.handler instanceof ResponseHandler.BodyObserver;
            conn = open(st.current, sendsOrigin(st) || "PATCH".equals(st.method) || streamed);
            final HttpURLConnection c = conn;
            // 오래 열려 있는 스트림을 JRE 연결로 읽는 중이면(프록시 때문에) disconnect 가 읽기 잠금을 기다릴 수 있으므로 별도 스레드에서 끊는다.
            final boolean detached = streamed && !(conn instanceof SocketHttpURLConnection) && !(conn instanceof SocketHttpsURLConnection);
            hook = new CancelToken.Hook() {
                /**
                 * Disconnects so that blocked reads end. The JRE connection cannot be closed while another thread waits in a read of it
                 * (it takes the lock of the body stream), so a stream that stays open is disconnected from a thread of its own and the
                 * canceller never waits for the next piece of data.
                 * <p>
                 * 막힌 읽기가 끝나도록 연결을 끊는다. JRE 연결은 다른 스레드가 읽기를 기다리는 동안에는 닫을 수 없으므로(본문 스트림의 잠금을 잡는다)
                 * 열려 있는 스트림은 별도 스레드에서 끊고, 취소하는 쪽은 다음 데이터를 기다리지 않는다.
                 */
                public void onCancel() {
                    if (!detached) {
                        c.disconnect();
                        return;
                    }
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            c.disconnect();
                        }
                    }, "swingwebpanel-disconnect");
                    t.setDaemon(true);
                    t.start();
                }
            };
            token.addHook(hook);
            prepare(st, conn);
            if (st.body != null) {
                OutputStream out = conn.getOutputStream();
                try {
                    out.write(st.body);
                } finally {
                    out.close();
                }
            }
            int status = conn.getResponseCode();
            if (status < 0) {
                throw new IOException("invalid HTTP response");
            }
            Map<String, List<String>> headers = collectHeaders(conn);
            storeCookies(st, headers);
            List<String> location = headers.get("location");
            if (isRedirect(status) && location != null && !location.isEmpty()) {
                return follow(st, status, location.get(0));
            }
            keep = finish(st, conn, status, headers);
            return DONE;
        } catch (IOException e) {
            FailureKind kind = kindFor(e, st.inBody, token);
            if (st.current.startsWith("https:") && httpFallbackEnabled && !st.inBody
                    && (kind == FailureKind.TLS || kind == FailureKind.CONNECT)) {
                st.current = toHttp(st.current);
                st.downgraded = true;
                st.result.setHttpFallbackUsed();
                return CONTINUE;
            }
            return fail(st, kind, e.toString());
        } catch (RuntimeException e) {
            return fail(st, FailureKind.IO, "internal error: " + e);
        } finally {
            if (hook != null) {
                token.removeHook(hook);
            }
            if (conn != null && !keep) {
                conn.disconnect();
            }
        }
    }

    /**
     * Tells whether the request to the current URL carries an <code>Origin</code> header: a cross-origin request of a CORS purpose and every
     * POST, PUT or DELETE that has an initiating origin.
     * <p>
     * 현재 URL 로의 요청이 <code>Origin</code> 헤더를 싣는지 알려 준다: CORS 목적의 교차 오리진 요청, 그리고 시작한 오리진이 있는 모든 POST, PUT, DELETE.
     *
     * @param st state / 상태
     * @return true if the header is sent / 헤더를 보내면 true
     */
    private static boolean sendsOrigin(State st) {
        Origin initiator = st.req.getInitiator();
        if (initiator == null) {
            return false;
        }
        boolean cors = OriginPolicy.requiresCors(st.req.getPurpose()) && !initiator.isSameOrigin(Origin.of(st.current));
        return cors || "POST".equals(st.method) || "PUT".equals(st.method) || "PATCH".equals(st.method) || "DELETE".equals(st.method);
    }

    /**
     * Returns the value of the <code>Origin</code> header of an initiating origin (<code>null</code> for opaque origins and for pages that
     * come from the class path).
     * <p>
     * 시작한 오리진의 <code>Origin</code> 헤더 값을 반환한다(불투명 오리진과 클래스패스 페이지는 <code>null</code>).
     *
     * @param initiator the initiating origin / 시작한 오리진
     * @return the header value / 헤더 값
     */
    private static String originHeader(Origin initiator) {
        boolean hide = initiator.isOpaque() || UrlUtil.SCHEME_CLASSPATH.equals(initiator.getScheme());
        return hide ? "null" : initiator.serialize();
    }

    /**
     * Opens the connection of a request. The <code>HttpURLConnection</code> of the JRE silently drops restricted request headers such as
     * <code>Origin</code> and <code>Access-Control-Request-Method</code>, so a request that must carry one is made with
     * {@link SocketHttpURLConnection}/{@link SocketHttpsURLConnection} instead, unless a proxy of the JVM applies to it (this client cannot
     * use proxies; the request then goes through the JRE and the restricted headers are lost as before).
     * <p>
     * 요청의 연결을 연다. JRE 의 <code>HttpURLConnection</code> 은 <code>Origin</code>, <code>Access-Control-Request-Method</code> 같은 제한 요청
     * 헤더를 조용히 버리므로, 그런 헤더를 실어야 하는 요청은 JVM 의 프록시가 적용되지 않는 한 대신
     * {@link SocketHttpURLConnection}/{@link SocketHttpsURLConnection} 으로 보낸다(이 클라이언트는 프록시를 쓸 수 없다. 그 요청은 JRE 를 지나며 제한
     * 헤더가 예전처럼 사라진다).
     *
     * @param url        the absolute http or https URL / 절대 http 또는 https URL
     * @param restricted true if the request carries a restricted header / 제한 헤더를 싣는 요청이면 true
     * @return the connection / 연결
     * @throws IOException if the URL is malformed or has no http connection / URL 이 잘못되었거나 http 연결이 없으면
     */
    private HttpURLConnection open(String url, boolean restricted) throws IOException {
        URL u = new URL(url);
        if (restricted && !SocketHttpClient.viaProxy(u)) {
            return "https".equalsIgnoreCase(u.getProtocol()) ? new SocketHttpsURLConnection(u) : new SocketHttpURLConnection(u);
        }
        return (HttpURLConnection) u.openConnection();
    }

    /**
     * Returns the headers of the page that decide about a preflight: the headers that the page wrote and the <code>Content-Type</code> of the
     * body of this hop.
     * <p>
     * 사전 요청 여부를 정하는 페이지의 헤더를 반환한다: 페이지가 쓴 헤더와 이번 구간 본문의 <code>Content-Type</code>.
     *
     * @param st state / 상태
     * @return the headers / 헤더
     */
    private static Map<String, String> preflightHeaders(State st) {
        Map<String, String> hdr = st.req.getCorsAuthorHeaders();
        boolean hasType = false;
        // 페이지가 이미 Content-Type 을 썼는지 대소문자 없이 확인한다.
        for (String name : hdr.keySet()) {
            if ("content-type".equalsIgnoreCase(name)) {
                hasType = true;
            }
        }
        if (st.body != null && !hasType) {
            hdr.put("Content-Type", st.bodyType != null ? st.bodyType : "application/x-www-form-urlencoded");
        }
        return hdr;
    }

    /**
     * Makes the CORS preflight of the current hop if one is needed and not yet allowed: a request that goes to another origin for a CORS
     * purpose with a method or a page header that is not safelisted ({@link OriginPolicy#isSimpleRequest}) is announced with an
     * <code>OPTIONS</code> request (without cookies and without the headers of the page) that carries <code>Origin</code>,
     * <code>Access-Control-Request-Method</code> and <code>Access-Control-Request-Headers</code>. The answer must grant the request
     * ({@link OriginPolicy#checkPreflight}); it is reused for the seconds of <code>Access-Control-Max-Age</code> (5 by default, at most
     * {@link OriginPolicy#MAX_PREFLIGHT_AGE_SECONDS}) for the same origin, URL and credentials. Module scripts and requests without a
     * page header or with a simple method are never preflighted, and the additional headers of the component never cause a preflight.
     * <p>
     * 필요하고 아직 허락받지 못했다면 현재 구간의 CORS 사전 요청을 보낸다: CORS 목적으로 다른 오리진에 가는 요청이 안전 목록에 없는 메서드나 페이지
     * 헤더({@link OriginPolicy#isSimpleRequest})를 가지면, <code>Origin</code>, <code>Access-Control-Request-Method</code>,
     * <code>Access-Control-Request-Headers</code> 를 실은 <code>OPTIONS</code> 요청(쿠키와 페이지의 헤더 없이)으로 미리 알린다. 응답이 요청을
     * 허락해야 하며({@link OriginPolicy#checkPreflight}), 같은 오리진, URL, 자격 증명에 대해 <code>Access-Control-Max-Age</code> 초 동안(기본 5초,
     * 최대 {@link OriginPolicy#MAX_PREFLIGHT_AGE_SECONDS}초) 다시 쓴다. 모듈 스크립트와, 페이지 헤더가 없거나 단순 메서드인 요청은 사전 요청을 하지
     * 않으며, 컴포넌트의 추가 헤더는 사전 요청을 일으키지 않는다.
     *
     * @param st state / 상태
     * @return the reason if the server denies the request, or null if no preflight is needed or it was allowed / 서버가 요청을 거부했다면 그 이유. 사전
     *         요청이 필요 없거나 허락받았으면 null
     * @throws IOException if the preflight request fails at the network level / 사전 요청이 네트워크 수준에서 실패하면
     */
    private String preflightDenial(State st) throws IOException {
        ResourceRequest req = st.req;
        Origin initiator = req.getInitiator();
        if (initiator == null || !OriginPolicy.requiresCors(req.getPurpose()) || req.getPurpose() == RequestPurpose.MODULE
                || initiator.isSameOrigin(Origin.of(st.current))) {
            return null;
        }
        List<String> unsafe = OriginPolicy.unsafeHeaderNames(preflightHeaders(st));
        if (OriginPolicy.isSimpleMethod(st.method) && unsafe.isEmpty()) {
            return null;
        }
        boolean withCredentials = credentialsFor(st);
        String key = initiator.serialize() + " " + st.current + " " + withCredentials;
        OriginPolicy.PreflightGrant cached = cachedGrant(key);
        if (cached != null && cached.covers(st.method, unsafe, withCredentials)) {
            return null;
        }
        CancelToken token = req.getCancelToken();
        HttpURLConnection conn = open(st.current, true);
        final HttpURLConnection c = conn;
        CancelToken.Hook hook = new CancelToken.Hook() {
            /**
             * Disconnects so that a blocked read of the preflight ends.
             * <p>
             * 사전 요청의 막힌 읽기가 끝나도록 연결을 끊는다.
             */
            public void onCancel() {
                c.disconnect();
            }
        };
        token.addHook(hook);
        try {
            conn.setRequestMethod("OPTIONS");
            conn.setConnectTimeout(limits.getConnectTimeoutMs());
            conn.setReadTimeout(limits.getReadTimeoutMs());
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setDoInput(true);
            if (conn instanceof HttpsURLConnection) {
                try {
                    ssl.configureSsl((HttpsURLConnection) conn);
                } catch (IOException e) {
                    throw new SSLException("ssl configuration failed: " + e.getMessage());
                }
            }
            conn.setRequestProperty("User-Agent", req.getUserAgent());
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Origin", originHeader(initiator));
            conn.setRequestProperty("Access-Control-Request-Method", st.method);
            if (!unsafe.isEmpty()) {
                StringBuilder names = new StringBuilder();
                // 안전하지 않은 헤더 이름을 쉼표로 이어 붙인다.
                for (String n : unsafe) {
                    if (names.length() > 0) {
                        names.append(',');
                    }
                    names.append(n);
                }
                conn.setRequestProperty("Access-Control-Request-Headers", names.toString());
            }
            String referer = referrerFor(req.getReferrer(), st.current);
            if (referer != null) {
                conn.setRequestProperty("Referer", referer);
            }
            int status = conn.getResponseCode();
            ResourceResponse answer = new ResourceResponse(st.current);
            answer.setStatus(status, conn.getResponseMessage());
            // 응답 헤더를 판정에 쓸 응답 객체로 옮긴다.
            for (Map.Entry<String, List<String>> e : collectHeaders(conn).entrySet()) {
                for (String v : e.getValue()) {
                    answer.addHeader(e.getKey(), v);
                }
            }
            OriginPolicy.PreflightGrant grant = OriginPolicy.checkPreflight(initiator, status, answer, withCredentials);
            if (grant == null) {
                return "CORS preflight to " + st.current + " was denied (status " + status + ", Access-Control-Allow-Origin: "
                        + answer.getHeader("Access-Control-Allow-Origin") + ")";
            }
            if (!grant.covers(st.method, unsafe, withCredentials)) {
                return "CORS preflight to " + st.current + " does not allow the method " + st.method + " with the headers " + unsafe;
            }
            storeGrant(key, grant);
            return null;
        } finally {
            token.removeHook(hook);
            conn.disconnect();
        }
    }

    /**
     * Returns the reusable grant of an earlier preflight, or null.
     * <p>
     * 앞선 사전 요청의 다시 쓸 수 있는 허락을 반환한다. 없으면 null.
     *
     * @param key origin, URL and credentials of the preflight / 사전 요청의 오리진, URL, 자격 증명
     * @return the grant or null / 허락 또는 null
     */
    private OriginPolicy.PreflightGrant cachedGrant(String key) {
        synchronized (preflightGrants) {
            PreflightEntry e = preflightGrants.get(key);
            if (e == null) {
                return null;
            }
            if (e.expires < System.currentTimeMillis()) {
                preflightGrants.remove(key);
                return null;
            }
            return e.grant;
        }
    }

    /**
     * Remembers a grant for the seconds that the server allowed (nothing for 0).
     * <p>
     * 서버가 허락한 초 동안 허락을 기억한다(0 이면 기억하지 않는다).
     *
     * @param key   origin, URL and credentials of the preflight / 사전 요청의 오리진, URL, 자격 증명
     * @param grant the grant / 허락
     */
    private void storeGrant(String key, OriginPolicy.PreflightGrant grant) {
        if (grant.getMaxAgeSeconds() <= 0) {
            return;
        }
        synchronized (preflightGrants) {
            preflightGrants.put(key, new PreflightEntry(grant, System.currentTimeMillis() + grant.getMaxAgeSeconds() * 1000L));
        }
    }

    /**
     * Configures a connection: method, timeouts, SSL hook and all request headers.
     * <p>
     * 연결을 설정한다. 메서드, 제한 시간, SSL 훅, 모든 요청 헤더.
     *
     * @param st   state / 상태
     * @param conn the connection / 연결
     * @throws IOException on failure / 실패 시
     */
    private void prepare(State st, HttpURLConnection conn) throws IOException {
        ResourceRequest req = st.req;
        conn.setRequestMethod(st.method);
        conn.setConnectTimeout(limits.getConnectTimeoutMs());
        conn.setReadTimeout(req.getReadTimeoutMs() >= 0 ? req.getReadTimeoutMs() : limits.getReadTimeoutMs());
        conn.setInstanceFollowRedirects(false);
        // JRE 는 useCaches=false 일 때 스스로 Cache-Control/Pragma: no-cache 를 붙이므로 강제 새로고침에만 false 를 쓴다.
        conn.setUseCaches(st.req.getCacheMode() != ResourceRequest.CacheMode.FORCE_RELOAD);
        conn.setAllowUserInteraction(false);
        conn.setDoInput(true);
        if (conn instanceof HttpsURLConnection) {
            try {
                ssl.configureSsl((HttpsURLConnection) conn);
            } catch (IOException e) {
                throw new SSLException("ssl configuration failed: " + e.getMessage());
            }
        }
        conn.setRequestProperty("User-Agent", req.getUserAgent());
        conn.setRequestProperty("Accept", acceptFor(req.getPurpose()));
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        boolean sameOrigin = Origin.of(st.current).isSameOrigin(st.firstOrigin);
        boolean forwardSensitive = sameOrigin && !st.downgraded && !CrossOriginPolicy.omitsCredentials(req, st.current);
        RequestHeaders extra = req.getExtraHeaders();
        if (extra != null) {
            // 추가 헤더를 하나씩 적용하되 관리 대상 헤더와 (조건에 따라) 민감한 헤더는 건너뛴다.
            for (Map.Entry<String, String> e : extra.toMap().entrySet()) {
                String lower = e.getKey().toLowerCase();
                if (MANAGED.contains(lower) || (SENSITIVE.contains(lower) && !forwardSensitive)) {
                    continue;
                }
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }
        if (credentialsFor(st) && req.getCookies() != null) {
            String cookie = req.getCookies().getCookieHeader(st.current);
            if (cookie != null) {
                conn.setRequestProperty("Cookie", cookie);
            }
        }
        String referer = referrerFor(req.getReferrer(), st.current);
        if (referer != null) {
            conn.setRequestProperty("Referer", referer);
        }
        if (sendsOrigin(st)) {
            conn.setRequestProperty("Origin", originHeader(req.getInitiator()));
        }
        if (req.getCacheMode() == ResourceRequest.CacheMode.FORCE_RELOAD) {
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Pragma", "no-cache");
        }
        if (st.body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                    st.bodyType != null ? st.bodyType : "application/x-www-form-urlencoded");
        }
    }

    /**
     * Returns the default Accept header of a purpose.
     * <p>
     * 목적별 기본 Accept 헤더를 돌려준다.
     *
     * @param purpose request purpose / 요청 목적
     * @return Accept value / Accept 값
     */
    private static String acceptFor(RequestPurpose purpose) {
        switch (purpose) {
        case DOCUMENT:
        case FRAME:
            return "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
        case IMAGE:
        case IMAGE_CORS:
            return "image/*,*/*;q=0.8";
        case STYLESHEET:
            return "text/css,*/*;q=0.1";
        case AUDIO:
            return "audio/*,*/*;q=0.5";
        case MODULE:
        case WORKER_MODULE:
        case SCRIPT:
            return "*/*";
        default:
            return "*/*";
        }
    }

    /**
     * Computes the Referer to send: only http(s) referrers, no fragment, nothing when going from https to http.
     * <p>
     * 보낼 Referer 를 계산한다. http(s) 참조만, 프래그먼트 제외, https 에서 http 로 갈 때는 보내지 않는다.
     *
     * @param referrer referrer URL or null / 참조 URL 또는 null
     * @param target   target URL / 대상 URL
     * @return Referer value or null / Referer 값 또는 null
     */
    private static String referrerFor(String referrer, String target) {
        if (referrer == null) {
            return null;
        }
        SchemePolicy.Kind k = SchemePolicy.classify(referrer);
        if (k != SchemePolicy.Kind.HTTP && k != SchemePolicy.Kind.HTTPS) {
            return null;
        }
        if (k == SchemePolicy.Kind.HTTPS && target.startsWith("http:")) {
            return null;
        }
        try {
            return UrlUtil.stripFragment(UrlUtil.normalize(referrer));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Collects the response headers by lower-case name (status line excluded).
     * <p>
     * 응답 헤더를 소문자 이름으로 모은다(상태 줄 제외).
     *
     * @param conn the connection / 연결
     * @return headers in arrival order / 도착 순서의 헤더
     */
    private static Map<String, List<String>> collectHeaders(HttpURLConnection conn) {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        // 헤더 이름이 null 인 항목(상태 줄)은 건너뛴다.
        for (Map.Entry<String, List<String>> e : conn.getHeaderFields().entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String key = e.getKey().toLowerCase();
            List<String> list = out.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                out.put(key, list);
            }
            list.addAll(e.getValue());
        }
        return out;
    }

    /**
     * Tells whether cookies may be sent to and stored from the current hop: the request must allow credentials and, in the
     * "same-origin" credentials mode, the hop must have the origin of the initiator (a redirect to another origin gets no cookies).
     * <p>
     * 현재 구간에 쿠키를 보내고 그 응답의 쿠키를 저장해도 되는지 알려 준다. 요청이 자격 증명을 허용해야 하며, "same-origin" 자격 증명 모드에서는 구간이
     * 시작한 오리진과 같은 오리진이어야 한다(다른 오리진으로 리디렉션되면 쿠키를 쓰지 않는다).
     *
     * @param st state / 상태
     * @return true if cookies are used for this hop / 이 구간에 쿠키를 쓰면 true
     */
    private static boolean credentialsFor(State st) {
        ResourceRequest r = st.req;
        if (!r.isSendCredentials() || CrossOriginPolicy.omitsCredentials(r, st.current)) {
            return false;
        }
        if (!r.isSameOriginCredentialsOnly()) {
            return true;
        }
        Origin initiator = r.getInitiator();
        return initiator != null && initiator.isSameOrigin(Origin.of(st.current));
    }

    /**
     * Stores Set-Cookie headers of this hop in the request's cookie store.
     * <p>
     * 이번 응답의 Set-Cookie 헤더를 요청의 쿠키 저장소에 저장한다.
     *
     * @param st      state / 상태
     * @param headers response headers / 응답 헤더
     */
    private static void storeCookies(State st, Map<String, List<String>> headers) {
        CookieStore store = st.req.getCookies();
        List<String> cookies = headers.get("set-cookie");
        if (store != null && credentialsFor(st) && cookies != null) {
            store.storeFromResponse(st.current, cookies);
        }
    }

    /**
     * Tells whether a status code is a followed redirect.
     * <p>
     * 상태 코드가 따라가는 리디렉션인지 알려 준다.
     *
     * @param status status code / 상태 코드
     * @return true for 301, 302, 303, 307 and 308 / 301, 302, 303, 307, 308 이면 true
     */
    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    /**
     * Prepares the next request of a redirect.
     * <p>
     * 리디렉션의 다음 요청을 준비한다.
     *
     * @param st       state / 상태
     * @param status   redirect status / 리디렉션 상태
     * @param location Location header value / Location 헤더 값
     * @return {@link #CONTINUE} or {@link #DONE} after a failure / {@link #CONTINUE}, 실패 시 {@link #DONE}
     */
    private int follow(State st, int status, String location) {
        if (st.redirects >= limits.getMaxRedirects()) {
            return fail(st, FailureKind.TOO_MANY_REDIRECTS, "more than " + limits.getMaxRedirects() + " redirects");
        }
        String next;
        try {
            next = UrlUtil.resolve(st.current, location.trim());
        } catch (IllegalArgumentException e) {
            return fail(st, FailureKind.INVALID_URL, "bad Location: " + location);
        }
        SchemePolicy.Kind kind = SchemePolicy.classify(next);
        if (kind == SchemePolicy.Kind.BLOCKED || kind == SchemePolicy.Kind.INVALID) {
            return fail(st, FailureKind.BLOCKED_SCHEME, "redirect to a scheme that is not allowed: " + next);
        }
        if (kind != SchemePolicy.Kind.HTTP && kind != SchemePolicy.Kind.HTTPS) {
            return fail(st, FailureKind.BLOCKED_SCHEME, "an http response may not redirect to " + next);
        }
        st.redirects++;
        st.result.addRedirect(st.current);
        String nextFragment = UrlUtil.getFragment(next);
        if (nextFragment != null) {
            st.fragment = nextFragment;
        }
        st.current = UrlUtil.stripFragment(next);
        // 301/302 의 POST 와 303 의 GET/HEAD 이외 메서드는 GET 으로 바꾸고 본문을 버린다. 307/308 은 그대로 둔다.
        boolean toGet = (status == 303 && !"GET".equals(st.method) && !"HEAD".equals(st.method))
                || ((status == 301 || status == 302) && "POST".equals(st.method));
        if (toGet) {
            st.method = "GET";
            st.body = null;
            st.bodyType = null;
        }
        return CONTINUE;
    }

    /**
     * Converts an https URL into the http URL used for the one-time retry: port 443 or none becomes the http default,
     * any other explicit port stays.
     * <p>
     * https URL 을 일회성 재시도용 http URL 로 바꾼다. 443 이거나 없는 포트는 http 기본값이 되고 다른 명시 포트는 유지한다.
     *
     * @param url https URL / https URL
     * @return http URL / http URL
     */
    static String toHttp(String url) {
        UrlUtil.Parts p = UrlUtil.parse(url);
        int port = (p.port < 0 || p.port == 443) ? -1 : p.port;
        return UrlUtil.toString(new UrlUtil.Parts("http", p.hasAuthority, p.userInfo, p.host, port, p.path, p.query, p.fragment));
    }

    /**
     * Reads the final response: fills the result, runs the handler and reads the body.
     * <p>
     * 최종 응답을 읽는다. 결과를 채우고 처리기를 실행하고 본문을 읽는다.
     *
     * @param st      state / 상태
     * @param conn    the connection / 연결
     * @param status  status code / 상태 코드
     * @param headers response headers / 응답 헤더
     * @return true if the connection may be reused (body fully read) / 연결을 재사용해도 되면(본문을 모두 읽었으면) true
     * @throws IOException on read failure / 읽기 실패 시
     */
    private boolean finish(State st, HttpURLConnection conn, int status, Map<String, List<String>> headers) throws IOException {
        ResourceResponse res = st.result;
        res.setFinalUrl(finalUrl(st));
        res.setStatus(status, conn.getResponseMessage());
        // 헤더를 응답 객체에 옮긴다.
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            for (int i = 0; i < e.getValue().size(); i++) {
                res.addHeader(e.getKey(), e.getValue().get(i));
            }
        }
        // 304는 캐시 헤더와 병합한 뒤 검사한다. 다른 응답은 본문이나 처리기 호출 전에 판정한다.
        if (status != 304 && !CrossOriginPolicy.permits(st.req, res)) {
            fail(st, FailureKind.BLOCKED_EMBEDDER_POLICY, "Blocked by cross-origin embedder or resource policy");
            return false;
        }
        ContentType ct = ContentType.parse(res.getHeader("Content-Type"));
        res.setContentType(ct.getMime(), ct.getCharset());
        long declared = -1;
        String cl = res.getHeader("Content-Length");
        if (cl != null) {
            try {
                declared = Long.parseLong(cl.trim());
            } catch (NumberFormatException e) {
                declared = -1;
            }
        }
        res.setDeclaredLength(declared);
        String enc = res.getHeader("Content-Encoding");
        enc = enc == null ? null : enc.trim().toLowerCase();
        boolean encoded = enc != null && enc.length() > 0 && !"identity".equals(enc);
        if (encoded && !"gzip".equals(enc) && !"x-gzip".equals(enc) && !"deflate".equals(enc)) {
            fail(st, FailureKind.IO, "unsupported content-encoding: " + enc);
            return false;
        }
        boolean noBody = "HEAD".equals(st.method) || status == 204 || status == 304 || (status >= 100 && status < 200);

        boolean observe = st.handler instanceof ResponseHandler.BodyObserver;
        OutputStream sink = null;
        if (st.handler != null && (status < 400 || observe)) {
            sink = st.handler.onResponse(res);
        }
        boolean keepBody = sink == null || observe;
        ProgressListener progress = st.req.getProgressListener();
        long total = encoded ? -1 : declared;
        if (keepBody && !encoded && declared > limits.getMaxMemoryBytes()) {
            closeQuietly(sink);
            fail(st, FailureKind.TOO_LARGE, "Content-Length " + declared + " exceeds " + limits.getMaxMemoryBytes());
            return false;
        }
        InputStream in = null;
        ByteArrayOutputStream mem = keepBody ? new ByteArrayOutputStream() : null;
        long received = 0;
        CancelToken token = st.req.getCancelToken();
        st.inBody = true;
        InputStream dec = null;
        try {
            // 스트림을 열지 못한 경우에도 본문 목적지를 닫고 이미 받은 헤더 뒤에 본문 오류로 보고한다.
            if (!noBody) {
                in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            }
            if (progress != null) {
                progress.onProgress(0, total);
            }
            if (in != null) {
                dec = decode(in, enc);
                byte[] buf = new byte[8192];
                int n;
                // 본문을 끝까지 읽으며 취소, 크기 제한, 진행 상황을 매번 확인한다.
                while ((n = dec.read(buf)) >= 0) {
                    token.throwIfCancelled();
                    if (mem != null) {
                        if (received + n > limits.getMaxMemoryBytes()) {
                            fail(st, FailureKind.TOO_LARGE, "body exceeds " + limits.getMaxMemoryBytes() + " bytes");
                            return false;
                        }
                        mem.write(buf, 0, n);
                    }
                    // Fetch 관찰자는 캐시용 바이트와 함께 도착한 조각도 받는다.
                    if (sink != null) {
                        sink.write(buf, 0, n);
                    }
                    received += n;
                    if (progress != null) {
                        progress.onProgress(received, total);
                    }
                }
            }
            if (!encoded && !noBody && declared >= 0 && received < declared) {
                fail(st, FailureKind.TRUNCATED, "received " + received + " of " + declared + " bytes");
                return false;
            }
            if (sink != null) {
                sink.flush();
            }
            if (keepBody) {
                res.setBody(mem.toByteArray());
            } else {
                res.setStreamed(received);
            }
            if (progress != null) {
                progress.onProgress(received, received);
            }
            return true;
        } finally {
            closeQuietly(dec);
            if (dec != in) {
                closeQuietly(in);
            }
            closeQuietly(sink);
        }
    }

    /**
     * Closes a stream ignoring errors.
     * <p>
     * 오류를 무시하고 스트림을 닫는다.
     *
     * @param c stream or null / 스트림 또는 null
     */
    private static void closeQuietly(java.io.Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            // 닫기 실패는 무시한다.
        }
    }

    /**
     * Wraps a body stream with the decompressor of the Content-Encoding (gzip, x-gzip, deflate in zlib or raw form).
     * <p>
     * 본문 스트림을 Content-Encoding 에 맞는 압축 해제기로 감싼다(gzip, x-gzip, zlib 또는 raw 형태의 deflate).
     *
     * @param in  raw body / 원본 본문
     * @param enc lower-case Content-Encoding or null / 소문자 Content-Encoding 또는 null
     * @return decoded stream / 해독된 스트림
     * @throws IOException for unsupported encodings or damaged data / 지원하지 않는 인코딩이거나 손상된 데이터
     */
    static InputStream decode(InputStream in, String enc) throws IOException {
        if (enc == null || enc.length() == 0 || "identity".equals(enc)) {
            return in;
        }
        PushbackInputStream p = new PushbackInputStream(in, 2);
        int b0 = p.read();
        if (b0 < 0) {
            return new ByteArrayInputStream(new byte[0]);
        }
        if ("gzip".equals(enc) || "x-gzip".equals(enc)) {
            p.unread(b0);
            return new GZIPInputStream(p);
        }
        if ("deflate".equals(enc)) {
            int b1 = p.read();
            if (b1 < 0) {
                p.unread(b0);
                return rawInflater(p);
            }
            p.unread(new byte[] { (byte) b0, (byte) b1 });
            // zlib 머리(CMF 의 방식 8, 두 바이트가 31 의 배수)가 있으면 zlib, 없으면 raw deflate 로 푼다.
            boolean zlib = (b0 & 0x0F) == 8 && (((b0 & 0xFF) << 8) | (b1 & 0xFF)) % 31 == 0;
            return zlib ? new InflaterInputStream(p) : rawInflater(p);
        }
        throw new IOException("unsupported content-encoding: " + enc);
    }

    /**
     * Creates a decoder for raw deflate data (no zlib header). The JRE inflater in "nowrap" mode needs one extra dummy
     * byte after the data, so a zero byte is appended.
     * <p>
     * zlib 머리가 없는 raw deflate 데이터용 해독기를 만든다. "nowrap" 모드의 JRE inflater 는 데이터 뒤에 더미 바이트가 하나 더
     * 필요하므로 0 바이트를 덧붙인다.
     *
     * @param in raw deflate bytes / raw deflate 바이트
     * @return decoded stream / 해독된 스트림
     */
    private static InputStream rawInflater(InputStream in) {
        InputStream withDummy = new SequenceInputStream(in, new ByteArrayInputStream(new byte[] { 0 }));
        return new InflaterInputStream(withDummy, new Inflater(true));
    }

    /**
     * Maps an I/O exception to a failure kind.
     * <p>
     * 입출력 예외를 실패 종류로 바꾼다.
     *
     * @param e      the exception / 예외
     * @param inBody true if it happened while reading the body / 본문을 읽는 중에 일어났으면 true
     * @param token  cancellation token / 취소 토큰
     * @return failure kind / 실패 종류
     */
    static FailureKind kindFor(IOException e, boolean inBody, CancelToken token) {
        if (token.isCancelled() || e instanceof CancelledException) {
            return FailureKind.CANCELLED;
        }
        if (e instanceof UnknownHostException) {
            return FailureKind.DNS;
        }
        if (e instanceof SocketTimeoutException) {
            return FailureKind.TIMEOUT;
        }
        if (e instanceof SSLException) {
            return FailureKind.TLS;
        }
        if (e instanceof ConnectException || e instanceof NoRouteToHostException || e instanceof PortUnreachableException) {
            return FailureKind.CONNECT;
        }
        if (inBody) {
            return FailureKind.TRUNCATED;
        }
        return FailureKind.IO;
    }
}
