package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.net.ContentType;

/**
 * The header rules of the Fetch Standard (section 2.2) in one place: what a header name and a header value may contain, which request
 * headers a page may never set (<em>forbidden request-headers</em>), which response headers a page may never see
 * (<em>forbidden response-header names</em>), which response headers a cross-origin response exposes without
 * <code>Access-Control-Expose-Headers</code> (<em>CORS-safelisted response-header names</em>), and the safelist of request headers for
 * <code>no-cors</code> requests. One project rule is added on top of the standard: <code>User-Agent</code> is forbidden for pages, because the
 * component owns that header (<code>setUserAgent</code>) and a page may not change it. Pure functions; no state.
 * <p>
 * Fetch 표준(2.2절)의 헤더 규칙을 한곳에 모았다: 헤더 이름과 값에 들어갈 수 있는 것, 페이지가 설정할 수 없는 요청 헤더(<em>forbidden request-header</em>),
 * 페이지가 볼 수 없는 응답 헤더(<em>forbidden response-header name</em>), <code>Access-Control-Expose-Headers</code> 없이 교차 오리진 응답이 노출하는 응답
 * 헤더(<em>CORS-safelisted response-header name</em>), <code>no-cors</code> 요청의 요청 헤더 안전 목록. 표준 위에 프로젝트 규칙 하나를 더했다:
 * <code>User-Agent</code> 는 컴포넌트가 소유하는 헤더(<code>setUserAgent</code>)이므로 페이지에 금지된다. 상태 없는 순수 함수이다.
 */
final class HeaderRules {

    /** The forbidden request-header names (lower case). / 금지된 요청 헤더 이름(소문자). */
    private static final String[] FORBIDDEN_REQUEST = { "accept-charset", "accept-encoding", "access-control-request-headers",
        "access-control-request-method", "connection", "content-length", "cookie", "cookie2", "date", "dnt", "expect", "host", "keep-alive",
        "origin", "referer", "set-cookie", "te", "trailer", "transfer-encoding", "upgrade", "via", "user-agent" };

    /** The CORS-safelisted response-header names (lower case). / CORS 안전 목록 응답 헤더 이름(소문자). */
    private static final String[] SAFELISTED_RESPONSE = { "cache-control", "content-language", "content-length", "content-type", "expires",
        "last-modified", "pragma" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HeaderRules() {
    }

    /**
     * Tells whether a string is an HTTP token, which is what a header name or a method must be.
     * <p>
     * 문자열이 HTTP 토큰인지 알려 준다. 헤더 이름과 메서드가 토큰이어야 한다.
     *
     * @param s the string / 문자열
     * @return true for a non-empty token / 비어 있지 않은 토큰이면 true
     */
    static boolean isToken(String s) {
        if (s.length() == 0) {
            return false;
        }
        // 모든 문자가 토큰 문자(영숫자와 !#$%&'*+-.^_`|~)여야 한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes a header value: leading and trailing HTTP whitespace (tab, LF, CR, space) is removed.
     * <p>
     * 헤더 값을 정규화한다: 앞뒤의 HTTP 공백(탭, LF, CR, 공백)을 제거한다.
     *
     * @param v the value / 값
     * @return the normalized value / 정규화한 값
     */
    static String normalizeValue(String v) {
        int a = 0;
        int b = v.length();
        // 앞뒤에서 HTTP 공백을 걷어 낸다.
        while (a < b && isWhitespace(v.charAt(a))) {
            a++;
        }
        // 뒤쪽의 공백도 걷어 낸다.
        while (b > a && isWhitespace(v.charAt(b - 1))) {
            b--;
        }
        return v.substring(a, b);
    }

    /**
     * Tells whether a character is HTTP whitespace.
     * <p>
     * 문자가 HTTP 공백인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for tab, LF, CR and space / 탭, LF, CR, 공백이면 true
     */
    private static boolean isWhitespace(char c) {
        return c == '\t' || c == '\n' || c == '\r' || c == ' ';
    }

    /**
     * Tells whether a normalized value is a valid header value: no NUL, CR or LF.
     * <p>
     * 정규화한 값이 올바른 헤더 값인지 알려 준다: NUL, CR, LF 가 없어야 한다.
     *
     * @param v the value / 값
     * @return true if valid / 올바르면 true
     */
    static boolean isValidValue(String v) {
        // 헤더 분할을 막기 위해 NUL, CR, LF 가 있으면 거부한다.
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == 0 || c == '\r' || c == '\n') {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a request header is forbidden for pages.
     * <p>
     * 요청 헤더가 페이지에 금지되었는지 알려 준다.
     *
     * @param name  the header name / 헤더 이름
     * @param value the header value / 헤더 값
     * @return true if a page may not set it / 페이지가 설정할 수 없으면 true
     */
    static boolean isForbiddenRequestHeader(String name, String value) {
        String n = name.toLowerCase();
        // 이름이 금지 목록에 있으면 금지이다.
        for (int i = 0; i < FORBIDDEN_REQUEST.length; i++) {
            if (FORBIDDEN_REQUEST[i].equals(n)) {
                return true;
            }
        }
        if (n.startsWith("proxy-") || n.startsWith("sec-")) {
            return true;
        }
        // 메서드 재정의 헤더는 값에 금지된 메서드가 들어 있을 때만 금지이다.
        if ("x-http-method".equals(n) || "x-http-method-override".equals(n) || "x-method-override".equals(n)) {
            String[] parts = value.split(",");
            // 쉼표로 나눈 값마다 금지된 메서드인지 확인한다.
            for (int i = 0; i < parts.length; i++) {
                if (isForbiddenMethod(parts[i].trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tells whether a method is forbidden (CONNECT, TRACE, TRACK).
     * <p>
     * 메서드가 금지되었는지 알려 준다(CONNECT, TRACE, TRACK).
     *
     * @param method the method / 메서드
     * @return true if forbidden / 금지이면 true
     */
    static boolean isForbiddenMethod(String method) {
        return "CONNECT".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method) || "TRACK".equalsIgnoreCase(method);
    }

    /**
     * Normalizes a method: DELETE, GET, HEAD, OPTIONS, POST and PUT are upper-cased in any letter case; other methods stay as written.
     * <p>
     * 메서드를 정규화한다: DELETE, GET, HEAD, OPTIONS, POST, PUT 은 어떤 대소문자든 대문자로 바꾸고 다른 메서드는 그대로 둔다.
     *
     * @param method the method / 메서드
     * @return the normalized method / 정규화한 메서드
     */
    static String normalizeMethod(String method) {
        String u = method.toUpperCase();
        if ("DELETE".equals(u) || "GET".equals(u) || "HEAD".equals(u) || "OPTIONS".equals(u) || "POST".equals(u) || "PUT".equals(u)) {
            return u;
        }
        return method;
    }

    /**
     * Tells whether a response header name is forbidden for pages (<code>Set-Cookie</code>, <code>Set-Cookie2</code>).
     * <p>
     * 응답 헤더 이름이 페이지에 금지되었는지 알려 준다(<code>Set-Cookie</code>, <code>Set-Cookie2</code>).
     *
     * @param name the header name / 헤더 이름
     * @return true if forbidden / 금지이면 true
     */
    static boolean isForbiddenResponseHeaderName(String name) {
        String n = name.toLowerCase();
        return "set-cookie".equals(n) || "set-cookie2".equals(n);
    }

    /**
     * Tells whether a response header is exposed to every cross-origin page.
     * <p>
     * 응답 헤더가 모든 교차 오리진 페이지에 노출되는지 알려 준다.
     *
     * @param name the header name / 헤더 이름
     * @return true if CORS-safelisted / CORS 안전 목록이면 true
     */
    static boolean isSafelistedResponseHeader(String name) {
        String n = name.toLowerCase();
        // 안전 목록의 이름과 같은지 비교한다.
        for (int i = 0; i < SAFELISTED_RESPONSE.length; i++) {
            if (SAFELISTED_RESPONSE[i].equals(n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a (name, value) pair is a CORS-safelisted request header (the pair may be sent in a simple cross-origin request).
     * <p>
     * (이름, 값) 쌍이 CORS 안전 목록 요청 헤더인지 알려 준다(단순 교차 오리진 요청에 보낼 수 있다).
     *
     * @param name  the header name / 헤더 이름
     * @param value the header value / 헤더 값
     * @return true if safelisted / 안전 목록이면 true
     */
    static boolean isSafelistedRequestHeader(String name, String value) {
        if (value.length() > 128) {
            return false;
        }
        String n = name.toLowerCase();
        if ("accept".equals(n)) {
            return !hasUnsafeByte(value);
        }
        if ("accept-language".equals(n) || "content-language".equals(n)) {
            // 영숫자와 공백 * , - . ; = 만 허용한다.
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                boolean ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || " *,-.;=".indexOf(c) >= 0;
                if (!ok) {
                    return false;
                }
            }
            return true;
        }
        if ("content-type".equals(n)) {
            if (hasUnsafeByte(value)) {
                return false;
            }
            String mime = ContentType.parse(value).getMime();
            return "application/x-www-form-urlencoded".equals(mime) || "multipart/form-data".equals(mime) || "text/plain".equals(mime);
        }
        return false;
    }

    /**
     * Tells whether a value contains a CORS-unsafe request-header byte.
     * <p>
     * 값에 CORS 안전하지 않은 요청 헤더 바이트가 있는지 알려 준다.
     *
     * @param v the value / 값
     * @return true if unsafe / 안전하지 않으면 true
     */
    private static boolean hasUnsafeByte(String v) {
        // 제어 문자(탭 제외)와 구분 문자가 있으면 안전하지 않다.
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if ((c < 0x20 && c != 0x09) || "\"():<>?@[\\]{}".indexOf(c) >= 0 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a header name is one of the no-CORS-safelisted request-header names.
     * <p>
     * 헤더 이름이 no-CORS 안전 목록 요청 헤더 이름인지 알려 준다.
     *
     * @param name the header name / 헤더 이름
     * @return true for accept, accept-language, content-language and content-type / accept, accept-language, content-language, content-type 이면 true
     */
    static boolean isNoCorsSafelistedName(String name) {
        String n = name.toLowerCase();
        return "accept".equals(n) || "accept-language".equals(n) || "content-language".equals(n) || "content-type".equals(n);
    }
}
