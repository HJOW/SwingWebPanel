package org.duckdns.hjow.swingwebpanel.net;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes one resource request for {@link ResourceFetcher}: method, URL, optional body, the component's User-Agent and
 * extra headers, its {@link CookieStore}, cache mode, purpose, cancellation token, referrer and initiating origin.
 * All setters return the request for chaining. The fetch layer never changes a request.
 * <p>
 * Use: <code>new ResourceRequest("http://host/").setUserAgent(ua).setCookies(store).setPurpose(RequestPurpose.DOCUMENT)</code>.
 * <p>
 * {@link ResourceFetcher} 에 줄 리소스 요청 하나를 기술한다. 메서드, URL, 선택적 본문, 컴포넌트의 User-Agent 와 추가 헤더,
 * {@link CookieStore}, 캐시 모드, 목적, 취소 토큰, 참조 페이지, 요청을 일으킨 오리진을 가진다. 모든 setter 는 연쇄 호출을 위해
 * 요청 자신을 돌려준다. 가져오기 계층은 요청을 바꾸지 않는다.
 * <p>
 * 사용: <code>new ResourceRequest("http://host/").setUserAgent(ua).setCookies(store).setPurpose(RequestPurpose.DOCUMENT)</code>.
 */
public final class ResourceRequest {

    /**
     * The default User-Agent when none is set (same as the component default).
     * <p>
     * 지정하지 않았을 때의 기본 User-Agent(컴포넌트 기본값과 같음).
     */
    public static final String DEFAULT_USER_AGENT = "swingwebpanel/1.0";

    /**
     * Cache behaviour requested for this fetch.
     * <p>
     * 이 가져오기에 요청하는 캐시 동작.
     */
    public enum CacheMode {
        /**
         * Normal HTTP caching rules.
         * <p>
         * 일반 HTTP 캐시 규칙.
         */
        DEFAULT,
        /**
         * Bypass every cache (<code>Cache-Control: no-cache</code>, <code>Pragma: no-cache</code>).
         * <p>
         * 모든 캐시를 우회한다(<code>Cache-Control: no-cache</code>, <code>Pragma: no-cache</code>).
         */
        FORCE_RELOAD,
        /**
         * Prefer a stored copy even when it is stale (history navigation: back and forward); a response that the cache may not store
         * (<code>no-store</code>) or that is missing is fetched from the network.
         * <p>
         * 오래된 저장본이라도 우선 쓴다(이력 이동: 뒤로/앞으로 가기). 캐시가 저장할 수 없는 응답(<code>no-store</code>)이나 저장본이 없으면 네트워크에서 가져온다.
         */
        PREFER_CACHE
    }

    /**
     * Absolute request URL.
     * <p>
     * 절대 요청 URL.
     */
    private final String url;
    /**
     * HTTP method.
     * <p>
     * HTTP 메서드.
     */
    private String method = "GET";
    /**
     * Request body or null.
     * <p>
     * 요청 본문 또는 null.
     */
    private byte[] body;
    /**
     * Content-Type of the body.
     * <p>
     * 본문의 Content-Type.
     */
    private String bodyContentType;
    /**
     * User-Agent value.
     * <p>
     * User-Agent 헤더에 보낼 값.
     */
    private String userAgent = DEFAULT_USER_AGENT;
    /**
     * Extra headers of the component (may be null).
     * <p>
     * 컴포넌트의 추가 헤더(null 가능).
     */
    private RequestHeaders extraHeaders;
    /**
     * Cookie store of the component (may be null: no cookies).
     * <p>
     * 컴포넌트의 쿠키 저장소(null 이면 쿠키 없음).
     */
    private CookieStore cookies;
    /**
     * Cache mode.
     * <p>
     * 캐시 모드.
     */
    private CacheMode cacheMode = CacheMode.DEFAULT;
    /**
     * Request purpose.
     * <p>
     * 요청 목적.
     */
    private RequestPurpose purpose = RequestPurpose.OTHER;
    /** Embedder policy of the initiator. / 시작 문서의 삽입 정책. */
    private CrossOriginPolicy.Embedder embedderPolicy = CrossOriginPolicy.Embedder.UNSAFE_NONE;
    /** The container's origin when this navigates a child frame. / 하위 프레임을 이동할 때 컨테이너의 오리진. */
    private Origin embedderOrigin;
    /**
     * Cancellation token.
     * <p>
     * 취소 토큰.
     */
    private CancelToken cancelToken = new CancelToken();
    /**
     * URL of the referring page or null.
     * <p>
     * 참조 페이지 URL 또는 null.
     */
    private String referrer;
    /**
     * Origin of the initiating document or null.
     * <p>
     * 요청을 일으킨 문서의 오리진 또는 null.
     */
    private Origin initiator;
    /**
     * Progress listener or null.
     * <p>
     * 진행 리스너 또는 null.
     */
    private ProgressListener progressListener;
    /**
     * Whether cookies are attached and stored (credentials mode "include").
     * <p>
     * 쿠키를 붙이고 저장할지 여부(자격 증명 모드 "include").
     */
    private boolean sendCredentials = true;
    /**
     * Whether the cookies are limited to the hops (the first request and every redirect target) that have the initiator's origin
     * (credentials mode "same-origin": a redirect to another origin gets and stores no cookies).
     * <p>
     * 쿠키를 시작한 오리진과 같은 오리진의 구간(첫 요청과 모든 리디렉션 대상)으로 제한할지 여부(자격 증명 모드 "same-origin": 다른 오리진으로 리디렉션되면
     * 쿠키를 받지도 저장하지도 않는다).
     */
    private boolean sameOriginCredentialsOnly;
    /**
     * The headers that the page wrote itself (with the <code>Content-Type</code> of its body): only these decide whether a cross-origin
     * request needs a CORS preflight, because the additional headers of the component are not written by the page. Null means that the page
     * wrote none.
     * <p>
     * 페이지가 직접 쓴 헤더(본문의 <code>Content-Type</code> 포함). 교차 오리진 요청에 CORS 사전 요청이 필요한지는 이것만으로 정한다. 컴포넌트의 추가
     * 헤더는 페이지가 쓴 것이 아니기 때문이다. null 이면 페이지가 쓴 헤더가 없다.
     */
    private Map<String, String> corsAuthorHeaders;
    /**
     * The read timeout of the connection in milliseconds, or -1 for the limit of the request layer.
     * <p>
     * 연결의 읽기 제한 시간(밀리초). -1 이면 요청 계층의 제한을 쓴다.
     */
    private int readTimeoutMs = -1;
    /**
     * True for a response that stays open for long and is read while it arrives (an event stream).
     * <p>
     * 오래 열려 있고 도착하는 대로 읽는 응답(이벤트 스트림)이면 true.
     */
    private boolean streaming;

    /** Requesting environment of an object URL. / 객체 URL의 요청 환경. */
    private BlobUrlStore.Scope blobUrlScope;
    /** Entry resolved before a route that may unload its creator. / 생성자 언로드가 가능한 경로 이전에 조회한 항목. */
    private BlobUrlStore.Entry blobUrlEntry;

    /**
     * Creates a GET request.
     * <p>
     * GET 요청을 만든다.
     *
     * @param url absolute URL / 절대 URL
     */
    public ResourceRequest(String url) {
        this.url = url;
    }

    /**
     * Creates an independent copy of this request (the extra headers are copied, the cookie store, cancellation token and listeners are
     * shared). Used for conditional revalidation requests that must behave like the original.
     * <p>
     * 이 요청의 독립된 복사본을 만든다(추가 헤더는 복사하고 쿠키 저장소, 취소 토큰, 리스너는 공유한다). 원래 요청과 똑같이 동작해야 하는 조건부
     * 재검증 요청에 쓴다.
     *
     * @return the copy / 복사본
     */
    public ResourceRequest copy() {
        ResourceRequest c = new ResourceRequest(url);
        c.method = method;
        c.body = body;
        c.bodyContentType = bodyContentType;
        c.userAgent = userAgent;
        c.extraHeaders = extraHeaders == null ? null : extraHeaders.copy();
        c.cookies = cookies;
        c.cacheMode = cacheMode;
        c.purpose = purpose;
        c.embedderPolicy = embedderPolicy;
        c.embedderOrigin = embedderOrigin;
        c.cancelToken = cancelToken;
        c.referrer = referrer;
        c.initiator = initiator;
        c.progressListener = progressListener;
        c.sendCredentials = sendCredentials;
        c.sameOriginCredentialsOnly = sameOriginCredentialsOnly;
        c.corsAuthorHeaders = corsAuthorHeaders == null ? null : new LinkedHashMap<String, String>(corsAuthorHeaders);
        c.readTimeoutMs = readTimeoutMs;
        c.streaming = streaming;
        c.blobUrlScope = blobUrlScope;
        c.blobUrlEntry = blobUrlEntry;
        return c;
    }

    /**
     * Returns the URL.
     * <p>
     * URL 을 돌려준다.
     *
     * @return URL / URL
     */
    public String getUrl() {
        return url;
    }

    /** Returns the document or worker opening a blob URL. / blob URL을 여는 문서나 워커를 반환한다. */
    public BlobUrlStore.Scope getBlobUrlScope() {
        return blobUrlScope;
    }

    /**
     * Associates a blob URL request with its creating storage partition and requester origin.
     * <p>
     * blob URL 요청에 생성 환경의 저장소 파티션과 요청자 오리진을 연결한다.
     *
     * @param scope requesting environment / 요청 환경
     * @return this request / 이 요청
     */
    public ResourceRequest setBlobUrlScope(BlobUrlStore.Scope scope) {
        blobUrlScope = scope;
        return this;
    }

    /** Returns the object URL already accepted by this request. / 이 요청이 이미 받아들인 객체 URL을 반환한다. */
    BlobUrlStore.Entry getBlobUrlEntry() {
        return blobUrlEntry;
    }

    /**
     * Uses immutable bytes resolved at the start of a navigation or download.
     * <p>
     * 이동이나 다운로드가 시작할 때 조회했던 변경 불가 바이트를 사용한다.
     *
     * @param entry accepted URL entry / 받아들인 URL 항목
     * @return this request / 이 요청
     */
    public ResourceRequest setBlobUrlEntry(BlobUrlStore.Entry entry) {
        blobUrlEntry = entry;
        return this;
    }

    /**
     * Returns the method.
     * <p>
     * 메서드를 돌려준다.
     *
     * @return method / 메서드
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method (GET, HEAD, POST, PUT, PATCH, DELETE or OPTIONS; PATCH is sent over the socket connection because the JRE refuses it).
     * <p>
     * 메서드를 지정한다(GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS. PATCH 는 JRE 가 거부하므로 소켓 연결로 보낸다).
     *
     * @param m method name / 메서드 이름
     * @return this / 자기 자신
     * @throws IllegalArgumentException for other methods / 다른 메서드이면
     */
    public ResourceRequest setMethod(String m) {
        if (m == null) {
            throw new IllegalArgumentException("method is null");
        }
        String u = m.toUpperCase();
        if (!("GET".equals(u) || "HEAD".equals(u) || "POST".equals(u) || "PUT".equals(u) || "PATCH".equals(u) || "DELETE".equals(u) || "OPTIONS".equals(u))) {
            throw new IllegalArgumentException("unsupported method: " + m);
        }
        this.method = u;
        return this;
    }

    /**
     * Returns the body or null.
     * <p>
     * 본문을 돌려준다. 없으면 null.
     *
     * @return body / 본문
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Returns the body Content-Type.
     * <p>
     * 본문의 Content-Type 을 돌려준다.
     *
     * @return content type or null / Content-Type 또는 null
     */
    public String getBodyContentType() {
        return bodyContentType;
    }

    /**
     * Sets the request body and its Content-Type.
     * <p>
     * 요청 본문과 Content-Type 을 지정한다.
     *
     * @param data        body bytes or null / 본문 바이트 또는 null
     * @param contentType Content-Type value / Content-Type 값
     * @return this / 자기 자신
     */
    public ResourceRequest setBody(byte[] data, String contentType) {
        this.body = data;
        this.bodyContentType = contentType;
        return this;
    }

    /**
     * Returns the User-Agent.
     * <p>
     * User-Agent 를 돌려준다.
     *
     * @return User-Agent / User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the User-Agent (null or empty restores the default).
     * <p>
     * User-Agent 를 지정한다(null 이나 빈 문자열이면 기본값).
     *
     * @param ua User-Agent / User-Agent
     * @return this / 자기 자신
     */
    public ResourceRequest setUserAgent(String ua) {
        this.userAgent = (ua == null || ua.length() == 0) ? DEFAULT_USER_AGENT : ua;
        return this;
    }

    /**
     * Returns the extra headers or null.
     * <p>
     * 추가 헤더를 돌려준다. 없으면 null.
     *
     * @return headers / 헤더
     */
    public RequestHeaders getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * Sets the extra headers (they are copied when the request is executed).
     * <p>
     * 추가 헤더를 지정한다(요청 실행 시 복사해서 쓴다).
     *
     * @param h headers or null / 헤더 또는 null
     * @return this / 자기 자신
     */
    public ResourceRequest setExtraHeaders(RequestHeaders h) {
        this.extraHeaders = h;
        return this;
    }

    /**
     * Returns the cookie store or null.
     * <p>
     * 쿠키 저장소를 돌려준다. 없으면 null.
     *
     * @return store / 저장소
     */
    public CookieStore getCookies() {
        return cookies;
    }

    /**
     * Sets the cookie store.
     * <p>
     * 쿠키 저장소를 지정한다.
     *
     * @param c store or null / 저장소 또는 null
     * @return this / 자기 자신
     */
    public ResourceRequest setCookies(CookieStore c) {
        this.cookies = c;
        return this;
    }

    /**
     * Returns the cache mode.
     * <p>
     * 캐시 모드를 돌려준다.
     *
     * @return mode / 모드
     */
    public CacheMode getCacheMode() {
        return cacheMode;
    }

    /**
     * Sets the cache mode.
     * <p>
     * 캐시 모드를 지정한다.
     *
     * @param mode mode / 모드
     * @return this / 자기 자신
     */
    public ResourceRequest setCacheMode(CacheMode mode) {
        this.cacheMode = mode == null ? CacheMode.DEFAULT : mode;
        return this;
    }

    /**
     * Returns the purpose.
     * <p>
     * 목적을 돌려준다.
     *
     * @return purpose / 목적
     */
    public RequestPurpose getPurpose() {
        return purpose;
    }

    /**
     * Returns the effective embedder policy of the requester.
     * <p>
     * 요청자가 적용하는 삽입 정책을 반환한다.
     *
     * @return the policy / 정책
     */
    public CrossOriginPolicy.Embedder getEmbedderPolicy() {
        return embedderPolicy;
    }

    /**
     * Sets the initiator's policy for resource access and credential processing.
     * <p>
     * 리소스 접근과 자격 증명 처리에 적용할 시작 문서의 정책을 설정한다.
     *
     * @param policy the initiator's embedder policy / 시작 문서의 삽입 정책
     * @return this request / 이 요청
     */
    public ResourceRequest setEmbedderPolicy(CrossOriginPolicy.Embedder policy) {
        embedderPolicy = policy == null ? CrossOriginPolicy.Embedder.UNSAFE_NONE : policy;
        return this;
    }

    /**
     * Returns the origin used in a CORP decision, including the container origin of a frame navigation.
     * <p>
     * 프레임 이동이면 컨테이너 오리진을 포함하여 CORP 판정에 사용할 오리진을 반환한다.
     *
     * @return the policy origin or null / 정책 오리진 또는 null
     */
    Origin embedderOrigin() {
        return embedderOrigin == null ? initiator : embedderOrigin;
    }

    /**
     * Sets the embedding document's origin for a child navigation.
     * <p>
     * 하위 이동에 적용할 컨테이너 문서의 오리진을 설정한다.
     *
     * @param origin the container's origin / 컨테이너의 오리진
     * @return this request / 이 요청
     */
    public ResourceRequest setEmbedderOrigin(Origin origin) {
        embedderOrigin = origin;
        return this;
    }

    /**
     * Sets the purpose.
     * <p>
     * 목적을 지정한다.
     *
     * @param p purpose / 목적
     * @return this / 자기 자신
     */
    public ResourceRequest setPurpose(RequestPurpose p) {
        this.purpose = p == null ? RequestPurpose.OTHER : p;
        return this;
    }

    /**
     * Returns the cancellation token (never null).
     * <p>
     * 취소 토큰을 돌려준다(null 이 아님).
     *
     * @return token / 토큰
     */
    public CancelToken getCancelToken() {
        return cancelToken;
    }

    /**
     * Sets the cancellation token.
     * <p>
     * 취소 토큰을 지정한다.
     *
     * @param t token / 토큰
     * @return this / 자기 자신
     */
    public ResourceRequest setCancelToken(CancelToken t) {
        this.cancelToken = t == null ? new CancelToken() : t;
        return this;
    }

    /**
     * Returns the referrer URL or null.
     * <p>
     * 참조 URL 을 돌려준다. 없으면 null.
     *
     * @return referrer / 참조 URL
     */
    public String getReferrer() {
        return referrer;
    }

    /**
     * Sets the referrer (the fragment is removed and nothing is sent when going from https to http).
     * <p>
     * 참조 URL 을 지정한다(프래그먼트는 제거하며 https 에서 http 로 갈 때는 보내지 않는다).
     *
     * @param r referrer URL or null / 참조 URL 또는 null
     * @return this / 자기 자신
     */
    public ResourceRequest setReferrer(String r) {
        this.referrer = r;
        return this;
    }

    /**
     * Returns the initiator origin or null.
     * <p>
     * 요청한 오리진을 돌려준다. 없으면 null.
     *
     * @return origin / 오리진
     */
    public Origin getInitiator() {
        return initiator;
    }

    /**
     * Sets the origin of the initiating document (used for the <code>Origin</code> header of XHR/font requests).
     * <p>
     * 요청을 일으킨 문서의 오리진을 지정한다(XHR/글꼴 요청의 <code>Origin</code> 헤더에 쓴다).
     *
     * @param o origin or null / 오리진 또는 null
     * @return this / 자기 자신
     */
    public ResourceRequest setInitiator(Origin o) {
        this.initiator = o;
        return this;
    }

    /**
     * Returns the progress listener or null.
     * <p>
     * 진행 리스너를 돌려준다. 없으면 null.
     *
     * @return listener / 리스너
     */
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    /**
     * Sets the progress listener.
     * <p>
     * 진행 리스너를 지정한다.
     *
     * @param l listener or null / 리스너 또는 null
     * @return this / 자기 자신
     */
    public ResourceRequest setProgressListener(ProgressListener l) {
        this.progressListener = l;
        return this;
    }

    /**
     * Tells whether cookies are attached to requests and stored from responses.
     * <p>
     * 요청에 쿠키를 붙이고 응답의 쿠키를 저장하는지 알려 준다.
     *
     * @return true if credentials are sent / 자격 증명을 보내면 true
     */
    public boolean isSendCredentials() {
        return sendCredentials;
    }

    /**
     * Sets whether cookies are attached and stored (false for cross-origin XHR without withCredentials).
     * <p>
     * 쿠키를 붙이고 저장할지 지정한다(withCredentials 없는 교차 오리진 XHR 은 false).
     *
     * @param send true to use cookies / 쿠키를 쓰려면 true
     * @return this / 자기 자신
     */
    public ResourceRequest setSendCredentials(boolean send) {
        this.sendCredentials = send;
        return this;
    }

    /**
     * Tells whether cookies are limited to hops with the initiator's origin.
     * <p>
     * 쿠키가 시작한 오리진과 같은 오리진의 구간으로 제한되는지 알려 준다.
     *
     * @return true for credentials mode "same-origin" / 자격 증명 모드 "same-origin" 이면 true
     */
    public boolean isSameOriginCredentialsOnly() {
        return sameOriginCredentialsOnly;
    }

    /**
     * Limits the cookies to hops that have the initiator's origin (see {@link #isSameOriginCredentialsOnly()}); it needs an initiator, a
     * request without one never sends cookies in this mode.
     * <p>
     * 쿠키를 시작한 오리진과 같은 오리진의 구간으로 제한한다({@link #isSameOriginCredentialsOnly()} 참조). 시작한 오리진이 필요하며 이 모드에서 그것이 없는
     * 요청은 쿠키를 보내지 않는다.
     *
     * @param only true to limit the cookies / 쿠키를 제한하려면 true
     * @return this / 자기 자신
     */
    public ResourceRequest setSameOriginCredentialsOnly(boolean only) {
        this.sameOriginCredentialsOnly = only;
        return this;
    }

    /**
     * Returns the headers that the page wrote itself, which decide whether a cross-origin request needs a CORS preflight.
     * <p>
     * 페이지가 직접 쓴 헤더를 반환한다. 교차 오리진 요청에 CORS 사전 요청이 필요한지를 이것이 정한다.
     *
     * @return a copy of the headers (the <code>Content-Type</code> of the body is included), empty if the page wrote none / 헤더의 복사본(본문의 <code>Content-Type</code> 포함). 없으면 빈 맵
     */
    public Map<String, String> getCorsAuthorHeaders() {
        return corsAuthorHeaders == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(corsAuthorHeaders);
    }

    /**
     * Sets the headers that the page wrote itself (see {@link #getCorsAuthorHeaders()}). The additional headers of the component are not
     * counted: they are sent, but they never make a request need a preflight.
     * <p>
     * 페이지가 직접 쓴 헤더를 정한다({@link #getCorsAuthorHeaders()} 참조). 컴포넌트의 추가 헤더는 세지 않는다. 보내기는 하지만 그것 때문에 사전 요청이
     * 필요해지지는 않는다.
     *
     * @param headers the headers or null for none / 헤더. 없으면 null
     * @return this / 자기 자신
     */
    public ResourceRequest setCorsAuthorHeaders(Map<String, String> headers) {
        this.corsAuthorHeaders = headers == null ? null : new LinkedHashMap<String, String>(headers);
        return this;
    }

    /**
     * Returns the read timeout that this request asks for.
     * <p>
     * 이 요청이 요구하는 읽기 제한 시간을 반환한다.
     *
     * @return the milliseconds, or -1 for the limit of the request layer / 밀리초. -1 이면 요청 계층의 제한
     */
    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /**
     * Sets the read timeout of this request, for a response that stays open for long and is silent between its pieces (an event stream).
     * The connect timeout and the other limits stay those of the request layer.
     * <p>
     * 이 요청의 읽기 제한 시간을 정한다. 오래 열려 있고 조각 사이에 조용한 응답(이벤트 스트림)을 위한 것이다. 연결 제한 시간과 다른 제한은 요청 계층의
     * 것을 그대로 쓴다.
     *
     * @param ms the milliseconds (0 means no timeout), or -1 for the limit of the request layer / 밀리초(0 은 제한 없음). -1 이면 요청 계층의 제한
     * @return this / 자기 자신
     */
    public ResourceRequest setReadTimeoutMs(int ms) {
        this.readTimeoutMs = ms;
        return this;
    }

    /**
     * Tells whether this request is for a response that stays open for long (an event stream).
     * <p>
     * 이 요청이 오래 열려 있는 응답(이벤트 스트림)을 위한 것인지 알려 준다.
     *
     * @return true for a stream / 스트림이면 true
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Marks this request as one for a response that stays open for long. The request layer then uses its own socket connection (which can be
     * closed at once, even while a thread waits for the next piece of data) instead of the connection of the JRE (which cannot be closed while
     * another thread waits in a read of it), unless a proxy applies to the URL.
     * <p>
     * 이 요청을 오래 열려 있는 응답을 위한 것으로 표시한다. 그러면 요청 계층은 JRE 의 연결(다른 스레드가 읽기를 기다리는 동안에는 닫을 수 없다) 대신 자체
     * 소켓 연결(다음 데이터를 기다리는 스레드가 있어도 곧바로 닫을 수 있다)을 쓴다. URL 에 프록시가 적용되면 그렇지 않다.
     *
     * @param value true for a stream / 스트림이면 true
     * @return this / 자기 자신
     */
    public ResourceRequest setStreaming(boolean value) {
        this.streaming = value;
        return this;
    }
}
