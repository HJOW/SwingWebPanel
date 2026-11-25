package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;

/**
 * One request that a page script asks the component to make through <code>XMLHttpRequest</code> or <code>fetch()</code>. The page layer
 * has already resolved the URL against the document's base URL, validated the method and the header names and applied the Fetch rules
 * for forbidden headers, so the component only has to enforce its network policy (schemes, User-Agent, extra headers, cookies, SSL hook,
 * cache, same-origin policy and CORS). The object is a plain value holder that is filled by the page layer before
 * {@link WebRequestSource#start(WebRequest, WebRequestSource.Callback)} and never changed afterwards.
 * <p>
 * 페이지 스크립트가 <code>XMLHttpRequest</code> 나 <code>fetch()</code> 로 컴포넌트에 요청하는 요청 하나. 페이지 계층이 URL을 문서 기준 URL로 이미
 * 해석했고 메서드와 헤더 이름을 검증했으며 금지 헤더에 대한 Fetch 규칙을 적용했으므로 컴포넌트는 자기 네트워크 정책(스킴, User-Agent, 추가 헤더, 쿠키,
 * SSL 훅, 캐시, 동일 출처 정책, CORS)만 적용하면 된다. 페이지 계층이 {@link WebRequestSource#start(WebRequest, WebRequestSource.Callback)} 전에
 * 채우는 단순 값 보관 객체이며 그 뒤에는 바꾸지 않는다.
 */
public final class WebRequest {

    /** Credentials mode "omit": never send or store cookies. / 자격 증명 모드 "omit": 쿠키를 보내거나 저장하지 않는다. */
    public static final String CREDENTIALS_OMIT = "omit";

    /** Credentials mode "same-origin": cookies only for the page's own origin. / 자격 증명 모드 "same-origin": 페이지 자신의 오리진에만 쿠키. */
    public static final String CREDENTIALS_SAME_ORIGIN = "same-origin";

    /** Credentials mode "include": cookies for every origin (the server must allow it across origins). / 자격 증명 모드 "include": 모든 오리진에 쿠키(교차 오리진은 서버가 허용해야 함). */
    public static final String CREDENTIALS_INCLUDE = "include";

    /** Request mode "cors": cross-origin allowed with the server's CORS permission. / 요청 모드 "cors": 서버의 CORS 허용이 있으면 교차 오리진 가능. */
    public static final String MODE_CORS = "cors";

    /** Request mode "same-origin": cross-origin requests fail. / 요청 모드 "same-origin": 교차 오리진 요청은 실패한다. */
    public static final String MODE_SAME_ORIGIN = "same-origin";

    /** Request mode "no-cors": cross-origin allowed but the response is opaque. / 요청 모드 "no-cors": 교차 오리진을 허용하되 응답은 불투명하다. */
    public static final String MODE_NO_CORS = "no-cors";

    /** Referrer value meaning "the document URL" (<code>about:client</code>). / "문서 URL" 을 뜻하는 참조 값(<code>about:client</code>). */
    public static final String REFERRER_CLIENT = "client";

    /** Referrer value meaning "send no Referer header". / "Referer 헤더를 보내지 않음" 을 뜻하는 참조 값. */
    public static final String REFERRER_NONE = "no-referrer";

    /** The absolute request URL (fragment allowed, it is not sent). / 절대 요청 URL(프래그먼트는 있어도 되며 전송하지 않는다). */
    private final String url;

    /** Optional requesting worker's policy. / 선택적인 요청 워커의 정책. */
    private CrossOriginPolicy.Embedder embedderPolicy;

    /** The normalized method. / 정규화한 메서드. */
    private String method = "GET";

    /** The author request headers as (name, value) pairs; values of equal names are already combined. / 작성자 요청 헤더(이름, 값) 쌍. 같은 이름의 값은 이미 합쳤다. */
    private final List<String[]> headers = new ArrayList<String[]>();

    /** The body bytes or null. / 본문 바이트 또는 null. */
    private byte[] body;

    /** The Content-Type that goes with the body, or null. / 본문에 딸린 Content-Type 또는 null. */
    private String bodyType;

    /** The credentials mode. / 자격 증명 모드. */
    private String credentials = CREDENTIALS_SAME_ORIGIN;

    /** The request mode. / 요청 모드. */
    private String mode = MODE_CORS;

    /** The cache mode of the Fetch Standard (default, no-store, reload, no-cache, force-cache). / Fetch 표준의 캐시 모드(default, no-store, reload, no-cache, force-cache). */
    private String cache = "default";

    /** The redirect mode (follow or error). / 리디렉션 모드(follow 또는 error). */
    private String redirect = "follow";

    /** The referrer: {@link #REFERRER_CLIENT}, {@link #REFERRER_NONE} or an absolute same-origin URL. / 참조: {@link #REFERRER_CLIENT}, {@link #REFERRER_NONE}, 같은 오리진의 절대 URL. */
    private String referrer = REFERRER_CLIENT;

    /** The referrer policy token or "". / 참조 정책 토큰 또는 "". */
    private String referrerPolicy = "";

    /**
     * Creates a request.
     * <p>
     * 요청을 만든다.
     *
     * @param url the absolute URL / 절대 URL
     */
    public WebRequest(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        this.url = url;
    }

    /**
     * Returns the URL.
     * <p>
     * URL을 반환한다.
     *
     * @return the absolute URL / 절대 URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns an explicit worker policy or null to inherit the document's settings.
     * <p>
     * 명시한 워커 정책을 반환한다. 문서 설정을 사용하려면 null이다.
     *
     * @return the policy or null / 정책 또는 null
     */
    public CrossOriginPolicy.Embedder getEmbedderPolicy() {
        return embedderPolicy;
    }

    /**
     * Carries the worker's policy through the shared document request service.
     * <p>
     * 공용 문서 요청 서비스를 통해 워커의 정책을 전달한다.
     *
     * @param policy the worker's COEP value / 워커의 COEP 값
     */
    public void setEmbedderPolicy(CrossOriginPolicy.Embedder policy) {
        embedderPolicy = policy;
    }

    /**
     * Returns the method.
     * <p>
     * 메서드를 반환한다.
     *
     * @return the normalized method / 정규화한 메서드
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method.
     * <p>
     * 메서드를 지정한다.
     *
     * @param m the normalized method / 정규화한 메서드
     * @return this request / 이 요청
     */
    public WebRequest setMethod(String m) {
        this.method = m;
        return this;
    }

    /**
     * Returns the live list of author headers.
     * <p>
     * 작성자 헤더의 목록을 반환한다.
     *
     * @return (name, value) pairs in order / 순서대로 (이름, 값) 쌍
     */
    public List<String[]> getHeaders() {
        return headers;
    }

    /**
     * Adds an author header.
     * <p>
     * 작성자 헤더를 추가한다.
     *
     * @param name  the header name / 헤더 이름
     * @param value the header value / 헤더 값
     * @return this request / 이 요청
     */
    public WebRequest addHeader(String name, String value) {
        headers.add(new String[] { name, value });
        return this;
    }

    /**
     * Returns the body.
     * <p>
     * 본문을 반환한다.
     *
     * @return the bytes or null / 바이트 또는 null
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Returns the body Content-Type.
     * <p>
     * 본문의 Content-Type 을 반환한다.
     *
     * @return the type or null / 타입 또는 null
     */
    public String getBodyType() {
        return bodyType;
    }

    /**
     * Sets the body.
     * <p>
     * 본문을 지정한다.
     *
     * @param data the bytes or null / 바이트 또는 null
     * @param type the Content-Type or null / Content-Type 또는 null
     * @return this request / 이 요청
     */
    public WebRequest setBody(byte[] data, String type) {
        this.body = data;
        this.bodyType = type;
        return this;
    }

    /**
     * Returns the credentials mode.
     * <p>
     * 자격 증명 모드를 반환한다.
     *
     * @return omit, same-origin or include / omit, same-origin, include 중 하나
     */
    public String getCredentials() {
        return credentials;
    }

    /**
     * Sets the credentials mode.
     * <p>
     * 자격 증명 모드를 지정한다.
     *
     * @param c omit, same-origin or include / omit, same-origin, include 중 하나
     * @return this request / 이 요청
     */
    public WebRequest setCredentials(String c) {
        this.credentials = c;
        return this;
    }

    /**
     * Returns the request mode.
     * <p>
     * 요청 모드를 반환한다.
     *
     * @return cors, same-origin or no-cors / cors, same-origin, no-cors 중 하나
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the request mode.
     * <p>
     * 요청 모드를 지정한다.
     *
     * @param m cors, same-origin or no-cors / cors, same-origin, no-cors 중 하나
     * @return this request / 이 요청
     */
    public WebRequest setMode(String m) {
        this.mode = m;
        return this;
    }

    /**
     * Returns the cache mode.
     * <p>
     * 캐시 모드를 반환한다.
     *
     * @return the Fetch cache mode / Fetch 캐시 모드
     */
    public String getCache() {
        return cache;
    }

    /**
     * Sets the cache mode.
     * <p>
     * 캐시 모드를 지정한다.
     *
     * @param c the Fetch cache mode / Fetch 캐시 모드
     * @return this request / 이 요청
     */
    public WebRequest setCache(String c) {
        this.cache = c;
        return this;
    }

    /**
     * Returns the redirect mode.
     * <p>
     * 리디렉션 모드를 반환한다.
     *
     * @return follow or error / follow 또는 error
     */
    public String getRedirect() {
        return redirect;
    }

    /**
     * Sets the redirect mode.
     * <p>
     * 리디렉션 모드를 지정한다.
     *
     * @param r follow or error / follow 또는 error
     * @return this request / 이 요청
     */
    public WebRequest setRedirect(String r) {
        this.redirect = r;
        return this;
    }

    /**
     * Returns the referrer.
     * <p>
     * 참조를 반환한다.
     *
     * @return {@link #REFERRER_CLIENT}, {@link #REFERRER_NONE} or a URL / {@link #REFERRER_CLIENT}, {@link #REFERRER_NONE} 또는 URL
     */
    public String getReferrer() {
        return referrer;
    }

    /**
     * Sets the referrer.
     * <p>
     * 참조를 지정한다.
     *
     * @param r {@link #REFERRER_CLIENT}, {@link #REFERRER_NONE} or an absolute same-origin URL / {@link #REFERRER_CLIENT}, {@link #REFERRER_NONE} 또는 같은 오리진의 절대 URL
     * @return this request / 이 요청
     */
    public WebRequest setReferrer(String r) {
        this.referrer = r;
        return this;
    }

    /**
     * Returns the referrer policy.
     * <p>
     * 참조 정책을 반환한다.
     *
     * @return the policy token or "" / 정책 토큰 또는 ""
     */
    public String getReferrerPolicy() {
        return referrerPolicy;
    }

    /**
     * Sets the referrer policy.
     * <p>
     * 참조 정책을 지정한다.
     *
     * @param p the policy token or "" / 정책 토큰 또는 ""
     * @return this request / 이 요청
     */
    public WebRequest setReferrerPolicy(String p) {
        this.referrerPolicy = p;
        return this;
    }
}
