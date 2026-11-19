package org.duckdns.hjow.swingwebpanel.net;

/**
 * One stored cookie (RFC 6265). Instances are immutable. A <em>host-only</em> cookie matches exactly the host that set it,
 * a domain cookie also matches sub domains. The expiry is an absolute time in milliseconds; session cookies have no expiry.
 * <p>
 * 저장된 쿠키 하나(RFC 6265). 인스턴스는 불변이다. <em>호스트 전용</em> 쿠키는 설정한 호스트에만 일치하고 도메인 쿠키는
 * 하위 도메인에도 일치한다. 만료는 밀리초 단위의 절대 시각이며 세션 쿠키는 만료가 없다.
 */
public final class Cookie {

    /**
     * Cookie name.
     * <p>
     * 쿠키 이름.
     */
    private final String name;
    /**
     * Cookie value.
     * <p>
     * 쿠키 값.
     */
    private final String value;
    /**
     * Domain in lower case without a leading dot.
     * <p>
     * 앞의 점이 없는 소문자 도메인.
     */
    private final String domain;
    /**
     * Whether only the exact host matches.
     * <p>
     * 정확히 같은 호스트에만 일치하는지 여부.
     */
    private final boolean hostOnly;
    /**
     * Cookie path starting with '/'.
     * <p>
     * '/' 로 시작하는 쿠키 경로.
     */
    private final String path;
    /**
     * Expiry time in milliseconds since the epoch, or Long.MAX_VALUE for a session cookie.
     * <p>
     * 에포크 이후 밀리초 단위의 만료 시각. 세션 쿠키는 Long.MAX_VALUE.
     */
    private final long expiresAt;
    /**
     * Whether the cookie is sent only over https.
     * <p>
     * https 에서만 보내는지 여부.
     */
    private final boolean secure;
    /**
     * Whether the cookie is hidden from scripts.
     * <p>
     * 스크립트에서 숨기는지 여부.
     */
    private final boolean httpOnly;
    /**
     * Creation order number (older cookies have smaller numbers).
     * <p>
     * 생성 순서 번호(오래된 쿠키일수록 작음).
     */
    private final long creation;

    /**
     * Creates a cookie.
     * <p>
     * 쿠키를 만든다.
     *
     * @param name      name / 이름
     * @param value     value / 값
     * @param domain    domain / 도메인
     * @param hostOnly  host-only flag / 호스트 전용 여부
     * @param path      path / 경로
     * @param expiresAt expiry or Long.MAX_VALUE / 만료 시각 또는 Long.MAX_VALUE
     * @param secure    secure flag / Secure 여부
     * @param httpOnly  HttpOnly flag / HttpOnly 여부
     * @param creation  creation order / 생성 순서
     */
    Cookie(String name, String value, String domain, boolean hostOnly, String path, long expiresAt,
            boolean secure, boolean httpOnly, long creation) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.hostOnly = hostOnly;
        this.path = path;
        this.expiresAt = expiresAt;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.creation = creation;
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 돌려준다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     * <p>
     * 값을 돌려준다.
     *
     * @return value / 값
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the domain.
     * <p>
     * 도메인을 돌려준다.
     *
     * @return domain / 도메인
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Tells whether the cookie matches only its exact host.
     * <p>
     * 쿠키가 정확한 호스트에만 일치하는지 알려 준다.
     *
     * @return true for host-only / 호스트 전용이면 true
     */
    public boolean isHostOnly() {
        return hostOnly;
    }

    /**
     * Returns the path.
     * <p>
     * 경로를 돌려준다.
     *
     * @return path / 경로
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the expiry time in milliseconds, or Long.MAX_VALUE for session cookies.
     * <p>
     * 만료 시각(밀리초)을 돌려준다. 세션 쿠키는 Long.MAX_VALUE.
     *
     * @return expiry / 만료 시각
     */
    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * Tells whether this is a session cookie.
     * <p>
     * 세션 쿠키인지 알려 준다.
     *
     * @return true if no expiry / 만료가 없으면 true
     */
    public boolean isSession() {
        return expiresAt == Long.MAX_VALUE;
    }

    /**
     * Tells whether the cookie is Secure.
     * <p>
     * Secure 쿠키인지 알려 준다.
     *
     * @return true if secure / Secure 이면 true
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Tells whether the cookie is HttpOnly.
     * <p>
     * HttpOnly 쿠키인지 알려 준다.
     *
     * @return true if HttpOnly / HttpOnly 이면 true
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Returns the creation order number.
     * <p>
     * 생성 순서 번호를 돌려준다.
     *
     * @return order number / 순서 번호
     */
    public long getCreation() {
        return creation;
    }

    /**
     * Returns <code>name=value</code>.
     * <p>
     * <code>이름=값</code> 을 돌려준다.
     *
     * @return pair text / 쌍 문자열
     */
    public String toPair() {
        return name + "=" + value;
    }

    /**
     * Returns a readable description for diagnostics.
     * <p>
     * 진단용으로 읽기 쉬운 설명을 돌려준다.
     *
     * @return description / 설명
     */
    public String toString() {
        return toPair() + "; domain=" + domain + (hostOnly ? "(host-only)" : "") + "; path=" + path
                + (secure ? "; Secure" : "") + (httpOnly ? "; HttpOnly" : "");
    }
}
