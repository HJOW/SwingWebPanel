package org.duckdns.hjow.swingwebpanel.net;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Same-origin policy and the CORS decision for cross-origin requests, separated by {@link RequestPurpose}.
 * <ul>
 * <li>Same origin: everything is allowed, including script access to the response body.</li>
 * <li>Cross origin, "no-cors" purposes (image, audio, style sheet, script, frame, document, download, other): the response may
 * be displayed or executed but its body is <b>never</b> exposed to scripts.</li>
 * <li>Cross origin, CORS purposes (XHR/fetch and web fonts): the response must carry <code>Access-Control-Allow-Origin</code>
 * equal to the requesting origin (or <code>*</code> without credentials); with credentials it must also carry
 * <code>Access-Control-Allow-Credentials: true</code> and may not use <code>*</code>. A request that is not
 * <em>simple</em> needs a CORS preflight first (see {@link #checkPreflight}); see {@link #isSimpleRequest(String, Map)}.</li>
 * </ul>
 * <p>
 * 동일 출처 정책과 교차 오리진 요청의 CORS 판정을 {@link RequestPurpose} 별로 구분한다.
 * <ul>
 * <li>같은 오리진: 응답 본문의 스크립트 접근을 포함해 모두 허용한다.</li>
 * <li>교차 오리진 "no-cors" 목적(이미지, 오디오, 스타일시트, 스크립트, 프레임, 문서, 다운로드, 기타): 응답을 표시하거나
 * 실행할 수는 있지만 본문을 스크립트에 <b>절대</b> 노출하지 않는다.</li>
 * <li>교차 오리진 CORS 목적(XHR/fetch, 웹 글꼴): 응답에 요청 오리진과 같은 <code>Access-Control-Allow-Origin</code>
 * (자격 증명이 없으면 <code>*</code> 도 가능)이 있어야 한다. 자격 증명을 쓰면 <code>Access-Control-Allow-Credentials: true</code>
 * 도 있어야 하고 <code>*</code> 는 쓸 수 없다. <em>단순</em>하지 않은 요청은 CORS 사전 요청이 필요하다({@link #checkPreflight} 참조). {@link #isSimpleRequest(String, Map)} 참조.</li>
 * </ul>
 */
public final class OriginPolicy {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private OriginPolicy() {
    }

    /**
     * Tells whether cross-origin use of a purpose needs CORS permission.
     * <p>
     * 목적의 교차 오리진 사용에 CORS 허용이 필요한지 알려 준다.
     *
     * @param purpose request purpose / 요청 목적
     * @return true for XHR, fonts, module scripts and images with a <code>crossorigin</code> attribute / XHR, 글꼴, 모듈 스크립트, <code>crossorigin</code> 속성이 있는 이미지이면 true
     */
    public static boolean requiresCors(RequestPurpose purpose) {
        return purpose == RequestPurpose.XHR || purpose == RequestPurpose.FONT || purpose == RequestPurpose.MODULE
                || purpose == RequestPurpose.IMAGE_CORS;
    }

    /**
     * Checks the CORS response headers for a cross-origin request.
     * <p>
     * 교차 오리진 요청에 대한 CORS 응답 헤더를 검사한다.
     *
     * @param initiator       requesting origin / 요청한 오리진
     * @param allowOrigin     value of Access-Control-Allow-Origin or null / Access-Control-Allow-Origin 값 또는 null
     * @param allowCredentials value of Access-Control-Allow-Credentials or null / Access-Control-Allow-Credentials 값 또는 null
     * @param withCredentials whether the request sent credentials (cookies) / 요청이 자격 증명(쿠키)을 보냈는지
     * @return true if the response grants access / 응답이 접근을 허용하면 true
     */
    public static boolean corsAllows(Origin initiator, String allowOrigin, String allowCredentials, boolean withCredentials) {
        if (initiator == null || initiator.isOpaque() || allowOrigin == null) {
            return false;
        }
        String ao = allowOrigin.trim();
        boolean credentialsOk = allowCredentials != null && "true".equals(allowCredentials.trim());
        if ("*".equals(ao)) {
            return !withCredentials;
        }
        if (!ao.equals(initiator.serialize())) {
            return false;
        }
        return !withCredentials || credentialsOk;
    }

    /**
     * Like {@link #corsAllows}, but a document with an opaque origin (a page made with <code>setContent</code>, whose origin serializes as
     * <code>null</code>) can be granted too: by <code>*</code> without credentials and by the answer <code>null</code> (with
     * <code>Access-Control-Allow-Credentials: true</code> if credentials were sent). Used for images of an element with a
     * <code>crossorigin</code> attribute.
     * <p>
     * {@link #corsAllows} 와 같지만 불투명 오리진 문서(<code>setContent</code> 로 만든 페이지. 오리진이 <code>null</code> 로 직렬화된다)도 허용받을 수 있다:
     * 자격 증명 없는 <code>*</code> 와 <code>null</code> 이라는 답으로(자격 증명을 보냈으면 <code>Access-Control-Allow-Credentials: true</code> 도). <code>crossorigin</code>
     * 속성이 있는 요소의 이미지에 쓴다.
     *
     * @param initiator        requesting origin / 요청한 오리진
     * @param allowOrigin      value of Access-Control-Allow-Origin or null / Access-Control-Allow-Origin 값 또는 null
     * @param allowCredentials value of Access-Control-Allow-Credentials or null / Access-Control-Allow-Credentials 값 또는 null
     * @param withCredentials  whether the request sent credentials (cookies) / 요청이 자격 증명(쿠키)을 보냈는지
     * @return true if the response grants access / 응답이 접근을 허용하면 true
     */
    public static boolean corsAllowsDocument(Origin initiator, String allowOrigin, String allowCredentials, boolean withCredentials) {
        if (initiator != null && initiator.isOpaque()) {
            String a = allowOrigin == null ? "" : allowOrigin.trim();
            boolean credentialsOk = !withCredentials || (allowCredentials != null && "true".equals(allowCredentials.trim()));
            return ("*".equals(a) && !withCredentials) || ("null".equals(a) && credentialsOk);
        }
        return corsAllows(initiator, allowOrigin, allowCredentials, withCredentials);
    }

    /**
     * Tells whether a script of the initiator may read the response body.
     * <p>
     * 요청한 오리진의 스크립트가 응답 본문을 읽을 수 있는지 알려 준다.
     *
     * @param initiator       requesting origin / 요청한 오리진
     * @param target          origin of the final response URL / 최종 응답 URL 의 오리진
     * @param purpose         request purpose / 요청 목적
     * @param response        the response (for CORS headers) / 응답(CORS 헤더용)
     * @param withCredentials whether credentials were sent / 자격 증명을 보냈는지
     * @return true if allowed / 허용되면 true
     */
    public static boolean mayScriptReadBody(Origin initiator, Origin target, RequestPurpose purpose,
            ResourceResponse response, boolean withCredentials) {
        if (initiator != null && initiator.isSameOrigin(target)) {
            return true;
        }
        if (!requiresCors(purpose) || response == null) {
            return false;
        }
        return corsAllows(initiator, response.getHeader("Access-Control-Allow-Origin"),
                response.getHeader("Access-Control-Allow-Credentials"), withCredentials);
    }

    /**
     * Tells whether the response may be used for its purpose (displayed, executed, applied). Cross-origin fonts and
     * XHR need CORS permission; the other purposes are allowed across origins.
     * <p>
     * 응답을 목적대로(표시, 실행, 적용) 쓸 수 있는지 알려 준다. 교차 오리진 글꼴과 XHR 은 CORS 허용이 필요하고 다른
     * 목적은 교차 오리진이어도 허용한다.
     *
     * @param initiator       requesting origin / 요청한 오리진
     * @param target          origin of the final response URL / 최종 응답 URL 의 오리진
     * @param purpose         request purpose / 요청 목적
     * @param response        the response / 응답
     * @param withCredentials whether credentials were sent / 자격 증명을 보냈는지
     * @return true if usable / 쓸 수 있으면 true
     */
    public static boolean mayUse(Origin initiator, Origin target, RequestPurpose purpose,
            ResourceResponse response, boolean withCredentials) {
        if (initiator != null && initiator.isSameOrigin(target)) {
            return true;
        }
        if (requiresCors(purpose)) {
            return response != null && corsAllows(initiator, response.getHeader("Access-Control-Allow-Origin"),
                    response.getHeader("Access-Control-Allow-Credentials"), withCredentials);
        }
        return true;
    }

    /**
     * Tells whether a request is a CORS "simple request" (no preflight needed): method GET, HEAD or POST and only
     * safelisted headers (<code>Accept</code>, <code>Accept-Language</code>, <code>Content-Language</code>,
     * <code>Content-Type</code> limited to form/plain types). Any other cross-origin request needs a preflight
     * ({@link #checkPreflight}).
     * <p>
     * 요청이 CORS "단순 요청"(사전 요청 불필요)인지 알려 준다. 메서드가 GET, HEAD, POST 이고 안전 목록 헤더
     * (<code>Accept</code>, <code>Accept-Language</code>, <code>Content-Language</code>, 폼/일반 텍스트로 제한된
     * <code>Content-Type</code>)만 있어야 한다. 그 밖의 교차 오리진 요청은 사전 요청이 필요하다({@link #checkPreflight}).
     *
     * @param method  request method / 요청 메서드
     * @param headers request headers (may be null) / 요청 헤더(null 가능)
     * @return true if simple / 단순 요청이면 true
     */
    public static boolean isSimpleRequest(String method, Map<String, String> headers) {
        return isSimpleMethod(method) && unsafeHeaderNames(headers).isEmpty();
    }

    /**
     * Tells whether a method is CORS-safelisted (GET, HEAD or POST, case-insensitively).
     * <p>
     * 메서드가 CORS 안전 목록에 있는지(GET, HEAD, POST. 대소문자 무시) 알려 준다.
     *
     * @param method request method / 요청 메서드
     * @return true if safelisted / 안전 목록에 있으면 true
     */
    public static boolean isSimpleMethod(String method) {
        String m = method == null ? "GET" : method.toUpperCase(Locale.ENGLISH);
        return "GET".equals(m) || "HEAD".equals(m) || "POST".equals(m);
    }

    /**
     * Returns the names of the request headers that are not CORS-safelisted, lower case, sorted and without duplicates: every header except
     * <code>Accept</code>, <code>Accept-Language</code>, <code>Content-Language</code>, <code>User-Agent</code> and a
     * <code>Content-Type</code> of a form, multipart or plain text type. These are the names that a preflight announces in
     * <code>Access-Control-Request-Headers</code>.
     * <p>
     * CORS 안전 목록에 없는 요청 헤더의 이름을 소문자로, 정렬하고 중복 없이 반환한다: <code>Accept</code>, <code>Accept-Language</code>,
     * <code>Content-Language</code>, <code>User-Agent</code>, 폼/멀티파트/일반 텍스트 형식의 <code>Content-Type</code> 을 뺀 모든 헤더. 사전 요청이
     * <code>Access-Control-Request-Headers</code> 로 알리는 이름이다.
     *
     * @param headers request headers (may be null) / 요청 헤더(null 가능)
     * @return the names / 이름
     */
    public static List<String> unsafeHeaderNames(Map<String, String> headers) {
        if (headers == null) {
            return new ArrayList<String>();
        }
        Set<String> names = new TreeSet<String>();
        // 헤더를 하나씩 보며 안전 목록에 없는 이름이나 허용되지 않은 Content-Type 의 이름을 모은다.
        for (Map.Entry<String, String> e : headers.entrySet()) {
            String n = e.getKey().toLowerCase(Locale.ENGLISH);
            if ("accept".equals(n) || "accept-language".equals(n) || "content-language".equals(n) || "user-agent".equals(n)) {
                continue;
            }
            if ("content-type".equals(n)) {
                String ct = ContentType.parse(e.getValue()).getMime();
                if ("application/x-www-form-urlencoded".equals(ct) || "multipart/form-data".equals(ct) || "text/plain".equals(ct)) {
                    continue;
                }
            }
            names.add(n);
        }
        return new ArrayList<String>(names);
    }

    /**
     * What a CORS-preflight response grants: the methods and header names that the server allows and how long the answer may be reused. A
     * <code>*</code> grants everything (except <code>Authorization</code> for headers) when the request has no credentials.
     * <p>
     * CORS 사전 요청 응답이 허용하는 것: 서버가 허용하는 메서드와 헤더 이름, 그리고 답을 얼마나 오래 다시 쓸 수 있는지. 요청에 자격 증명이 없으면
     * <code>*</code> 은 모든 것(헤더의 <code>Authorization</code> 은 제외)을 허용한다.
     */
    public static final class PreflightGrant {
        /**
         * The allowed methods, upper case, or the single entry <code>*</code>.
         * <p>
         * 허용된 메서드(대문자) 또는 <code>*</code> 하나.
         */
        private final Set<String> methods;
        /**
         * The allowed header names, lower case, or the single entry <code>*</code>.
         * <p>
         * 허용된 헤더 이름(소문자) 또는 <code>*</code> 하나.
         */
        private final Set<String> headers;
        /**
         * The seconds the grant may be reused.
         * <p>
         * 허용을 다시 쓸 수 있는 초.
         */
        private final int maxAgeSeconds;

        /**
         * Creates a grant.
         * <p>
         * 허용을 만든다.
         *
         * @param methods       the methods / 메서드
         * @param headers       the header names / 헤더 이름
         * @param maxAgeSeconds the seconds it may be reused / 다시 쓸 수 있는 초
         */
        PreflightGrant(Set<String> methods, Set<String> headers, int maxAgeSeconds) {
            this.methods = methods;
            this.headers = headers;
            this.maxAgeSeconds = maxAgeSeconds;
        }

        /**
         * Returns the seconds that the grant may be reused.
         * <p>
         * 허용을 다시 쓸 수 있는 초를 반환한다.
         *
         * @return the seconds / 초
         */
        public int getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        /**
         * Tells whether the grant covers a request.
         * <p>
         * 허용이 요청을 포함하는지 알려 준다.
         *
         * @param method          the method / 메서드
         * @param unsafeHeaders   the lower-case names of the non-safelisted headers / 안전 목록에 없는 헤더의 소문자 이름
         * @param withCredentials true if the request has credentials (then <code>*</code> grants nothing) / 요청에 자격 증명이 있으면 true(그러면 <code>*</code> 은 아무것도 허용하지 않는다)
         * @return true if covered / 포함하면 true
         */
        public boolean covers(String method, List<String> unsafeHeaders, boolean withCredentials) {
            if (!isSimpleMethod(method)) {
                String m = method.toUpperCase(Locale.ENGLISH);
                if (!methods.contains(m) && !(methods.contains("*") && !withCredentials)) {
                    return false;
                }
            }
            // 안전하지 않은 헤더 이름이 모두 허용 목록에 있어야 한다. 와일드카드는 Authorization 을 허용하지 않는다.
            for (String h : unsafeHeaders) {
                if (headers.contains(h)) {
                    continue;
                }
                if (headers.contains("*") && !withCredentials && !"authorization".equals(h)) {
                    continue;
                }
                return false;
            }
            return true;
        }
    }

    /**
     * Longest time, in seconds, that a preflight answer is reused whatever the server says.
     * <p>
     * 서버가 뭐라고 하든 사전 요청의 답을 다시 쓰는 최대 시간(초).
     */
    public static final int MAX_PREFLIGHT_AGE_SECONDS = 600;

    /**
     * Time, in seconds, that a preflight answer is reused when the server sends no <code>Access-Control-Max-Age</code>.
     * <p>
     * 서버가 <code>Access-Control-Max-Age</code> 를 보내지 않을 때 사전 요청의 답을 다시 쓰는 시간(초).
     */
    public static final int DEFAULT_PREFLIGHT_AGE_SECONDS = 5;

    /**
     * Checks the response of a CORS preflight and returns what it grants: the status must be 2xx, the
     * <code>Access-Control-Allow-Origin</code> and <code>Access-Control-Allow-Credentials</code> headers must permit the requesting origin as
     * for the actual request ({@link #corsAllows}), and the method and header lists are read from <code>Access-Control-Allow-Methods</code>
     * and <code>Access-Control-Allow-Headers</code>.
     * <p>
     * CORS 사전 요청의 응답을 검사하고 그것이 허용하는 것을 반환한다: 상태가 2xx 이어야 하고, <code>Access-Control-Allow-Origin</code> 과
     * <code>Access-Control-Allow-Credentials</code> 가 본 요청과 똑같이({@link #corsAllows}) 요청 오리진을 허용해야 하며, 메서드와 헤더 목록은
     * <code>Access-Control-Allow-Methods</code> 와 <code>Access-Control-Allow-Headers</code> 에서 읽는다.
     *
     * @param initiator       requesting origin / 요청한 오리진
     * @param status          status code of the preflight response / 사전 요청 응답의 상태 코드
     * @param response        the preflight response (for its headers) / 사전 요청 응답(헤더용)
     * @param withCredentials whether the actual request sends credentials / 본 요청이 자격 증명을 보내는지
     * @return what the response grants, or null if it denies the request / 응답이 허용하는 것. 요청을 거부하면 null
     */
    public static PreflightGrant checkPreflight(Origin initiator, int status, ResourceResponse response, boolean withCredentials) {
        if (status < 200 || status > 299 || response == null) {
            return null;
        }
        if (!corsAllows(initiator, response.getHeader("Access-Control-Allow-Origin"), response.getHeader("Access-Control-Allow-Credentials"),
                withCredentials)) {
            return null;
        }
        Set<String> methods = new HashSet<String>();
        // 허용 메서드 목록을 쉼표로 나누어 대문자로 모은다.
        for (String line : response.getHeaders("access-control-allow-methods")) {
            for (String m : line.split(",")) {
                if (m.trim().length() > 0) {
                    methods.add(m.trim().toUpperCase(Locale.ENGLISH));
                }
            }
        }
        Set<String> headers = new HashSet<String>();
        // 허용 헤더 목록을 쉼표로 나누어 소문자로 모은다.
        for (String line : response.getHeaders("access-control-allow-headers")) {
            for (String h : line.split(",")) {
                if (h.trim().length() > 0) {
                    headers.add(h.trim().toLowerCase(Locale.ENGLISH));
                }
            }
        }
        int age = DEFAULT_PREFLIGHT_AGE_SECONDS;
        String maxAge = response.getHeader("Access-Control-Max-Age");
        if (maxAge != null) {
            try {
                age = Math.max(0, Math.min(MAX_PREFLIGHT_AGE_SECONDS, Integer.parseInt(maxAge.trim())));
            } catch (NumberFormatException e) {
                age = DEFAULT_PREFLIGHT_AGE_SECONDS;
            }
        }
        return new PreflightGrant(methods, headers, age);
    }
}
