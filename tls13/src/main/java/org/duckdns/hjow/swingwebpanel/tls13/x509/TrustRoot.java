package org.duckdns.hjow.swingwebpanel.tls13.x509;

/**
 * A trust anchor: the subject name and the public key of a root certificate, with the restrictions that the root program applies to it.
 * The signature and the validity dates of the root certificate itself are not checked (as the Java PKIX validator does not check them
 * either); what makes a root trusted is its presence in the trust store. Example: <code>TrustRoot r = store.roots().get(0);</code>.
 * <p>
 * 신뢰 앵커: 루트 인증서의 주체 이름과 공개 키, 그리고 루트 프로그램이 그 루트에 적용하는 제한. 루트 인증서 자체의 서명과 유효 기간은 검사하지 않는다
 * (Java 의 PKIX 검증기도 검사하지 않는다). 루트를 신뢰하게 하는 것은 신뢰 저장소에 들어 있다는 사실이다.
 */
public final class TrustRoot {

    /** The root certificate. / 루트 인증서. */
    private final X509Cert cert;

    /** Certificates issued after this time (milliseconds since 1970) are not trusted; <code>Long.MAX_VALUE</code> if unrestricted. / 이 시각(1970년부터의 밀리초) 뒤에 발급한 인증서는 신뢰하지 않는다. 제한이 없으면 <code>Long.MAX_VALUE</code>. */
    private final long distrustAfter;

    /** Constraints applied by the root program, or null. / 루트 프로그램이 적용하는 제약. 없으면 null. */
    private final NameConstraints applied;

    /**
     * Creates a trust anchor.
     * <p>
     * 신뢰 앵커를 만든다.
     *
     * @param cert          the root certificate / 루트 인증서
     * @param distrustAfter the distrust date or <code>Long.MAX_VALUE</code> / 불신 날짜 또는 <code>Long.MAX_VALUE</code>
     * @param applied       the applied name constraints or null / 적용 이름 제약 또는 null
     */
    TrustRoot(X509Cert cert, long distrustAfter, NameConstraints applied) {
        this.cert = cert;
        this.distrustAfter = distrustAfter;
        this.applied = applied;
    }

    /**
     * Returns the root certificate.
     * <p>
     * 루트 인증서를 반환한다.
     *
     * @return the certificate / 인증서
     */
    public X509Cert certificate() {
        return cert;
    }

    /**
     * Returns the time after which issued certificates are not trusted.
     * <p>
     * 그 뒤에 발급한 인증서를 신뢰하지 않는 시각을 반환한다.
     *
     * @return milliseconds since 1970, or <code>Long.MAX_VALUE</code> / 1970년부터의 밀리초 또는 <code>Long.MAX_VALUE</code>
     */
    public long distrustAfter() {
        return distrustAfter;
    }

    /**
     * Returns the name constraints that the root program applies.
     * <p>
     * 루트 프로그램이 적용하는 이름 제약을 반환한다.
     *
     * @return the constraints or null / 제약 또는 null
     */
    public NameConstraints appliedConstraints() {
        return applied;
    }
}
