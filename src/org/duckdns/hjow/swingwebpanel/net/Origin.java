package org.duckdns.hjow.swingwebpanel.net;

/**
 * A web origin: scheme, host and effective port (RFC 6454 / HTML origin). Origins of <code>http</code>, <code>https</code> and
 * <code>ftp</code> URLs are tuples; all <code>classpath://</code> documents share the tuple origin <code>classpath://</code>
 * (same JVM, same class path); <code>about:blank</code>, <code>about:text</code> and unparsable or blocked addresses have an
 * <em>opaque</em> origin that equals no other origin (not even another opaque one) and serializes as <code>null</code>.
 * <p>
 * Use {@link #of(String)} and {@link #isSameOrigin(Origin)}. Cross origin response bodies must never be exposed to scripts
 * only because they can be displayed; see {@link OriginPolicy}.
 * <p>
 * 웹 오리진: 스킴, 호스트, 실효 포트(RFC 6454 / HTML 오리진). <code>http</code>, <code>https</code>, <code>ftp</code> URL 의 오리진은
 * 튜플이고, 모든 <code>classpath://</code> 문서는 같은 튜플 오리진 <code>classpath://</code> 를 공유한다(같은 JVM, 같은 클래스패스).
 * <code>about:blank</code>, <code>about:text</code>, 해석 불가 또는 차단된 주소는 어떤 오리진과도(다른 불투명 오리진과도) 같지 않은
 * <em>불투명</em> 오리진이며 <code>null</code> 로 직렬화된다.
 * <p>
 * {@link #of(String)} 와 {@link #isSameOrigin(Origin)} 를 사용한다. 교차 오리진 응답 본문은 표시할 수 있다는 이유만으로 스크립트에
 * 노출해서는 안 된다. {@link OriginPolicy} 를 참조한다.
 */
public final class Origin {

    /**
     * Scheme in lower case, null for opaque origins.
     * <p>
     * 소문자 스킴. 불투명 오리진이면 null.
     */
    private final String scheme;
    /**
     * Host in lower case (empty for classpath), null for opaque origins.
     * <p>
     * 소문자 호스트(classpath 는 빈 문자열). 불투명 오리진이면 null.
     */
    private final String host;
    /**
     * Effective port or -1.
     * <p>
     * 실효 포트. 없으면 -1.
     */
    private final int port;

    /**
     * Creates an origin.
     * <p>
     * 오리진을 만든다.
     *
     * @param scheme scheme or null for opaque / 스킴, 불투명이면 null
     * @param host   host or null / 호스트 또는 null
     * @param port   effective port or -1 / 실효 포트 또는 -1
     */
    private Origin(String scheme, String host, int port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    /**
     * Creates a fresh opaque origin (unequal to every other origin).
     * <p>
     * 새 불투명 오리진을 만든다(다른 모든 오리진과 다름).
     *
     * @return opaque origin / 불투명 오리진
     */
    public static Origin opaque() {
        return new Origin(null, null, -1);
    }

    /**
     * Computes the origin of an address.
     * <p>
     * 주소의 오리진을 계산한다.
     *
     * @param url absolute URL text / 절대 URL 문자열
     * @return the origin; opaque for special, blocked or invalid addresses / 오리진. 특수/차단/잘못된 주소는 불투명
     */
    public static Origin of(String url) {
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        if (kind == SchemePolicy.Kind.CLASSPATH) {
            return new Origin(UrlUtil.SCHEME_CLASSPATH, "", -1);
        }
        if (kind == SchemePolicy.Kind.HTTP || kind == SchemePolicy.Kind.HTTPS || kind == SchemePolicy.Kind.FTP) {
            UrlUtil.Parts p = UrlUtil.parse(url);
            return new Origin(p.scheme, p.host, UrlUtil.effectivePort(p));
        }
        return opaque();
    }

    /**
     * Tells whether this origin is opaque.
     * <p>
     * 이 오리진이 불투명한지 알려 준다.
     *
     * @return true for opaque / 불투명이면 true
     */
    public boolean isOpaque() {
        return scheme == null;
    }

    /**
     * Returns the scheme, or null for opaque origins.
     * <p>
     * 스킴을 돌려준다. 불투명 오리진이면 null.
     *
     * @return scheme / 스킴
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Returns the host, or null for opaque origins.
     * <p>
     * 호스트를 돌려준다. 불투명 오리진이면 null.
     *
     * @return host / 호스트
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the effective port or -1.
     * <p>
     * 실효 포트를 돌려준다. 없으면 -1.
     *
     * @return port / 포트
     */
    public int getPort() {
        return port;
    }

    /**
     * Same-origin check: both are tuple origins with equal scheme, host and effective port.
     * <p>
     * 동일 오리진 검사: 둘 다 튜플 오리진이며 스킴, 호스트, 실효 포트가 같다.
     *
     * @param other the other origin / 다른 오리진
     * @return true if same origin / 같은 오리진이면 true
     */
    public boolean isSameOrigin(Origin other) {
        return other != null && !isOpaque() && !other.isOpaque() && equals(other);
    }

    /**
     * Serializes as <code>scheme://host[:port]</code> (default port omitted), <code>classpath://</code>, or <code>null</code>.
     * <p>
     * <code>스킴://호스트[:포트]</code>(기본 포트 생략), <code>classpath://</code>, 또는 <code>null</code> 로 직렬화한다.
     *
     * @return serialized origin / 직렬화된 오리진
     */
    public String serialize() {
        if (isOpaque()) {
            return "null";
        }
        if (UrlUtil.SCHEME_CLASSPATH.equals(scheme)) {
            return "classpath://";
        }
        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        if (port >= 0 && port != UrlUtil.defaultPort(scheme)) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }

    /**
     * Two tuple origins are equal when scheme, host and port are equal; an opaque origin equals only itself.
     * <p>
     * 튜플 오리진은 스킴, 호스트, 포트가 같으면 같다. 불투명 오리진은 자기 자신하고만 같다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Origin)) {
            return false;
        }
        Origin other = (Origin) o;
        if (isOpaque() || other.isOpaque()) {
            return false;
        }
        return scheme.equals(other.scheme) && host.equals(other.host) && port == other.port;
    }

    /**
     * Hash code consistent with {@link #equals(Object)}.
     * <p>
     * {@link #equals(Object)} 와 일치하는 해시 코드.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        if (isOpaque()) {
            return System.identityHashCode(this);
        }
        return scheme.hashCode() * 31 * 31 + host.hashCode() * 31 + port;
    }

    /**
     * Returns the serialized form.
     * <p>
     * 직렬화된 형태를 돌려준다.
     *
     * @return text / 문자열
     */
    public String toString() {
        return serialize();
    }
}
