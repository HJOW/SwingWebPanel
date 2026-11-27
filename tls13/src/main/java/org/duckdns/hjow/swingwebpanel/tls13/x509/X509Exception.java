package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.security.cert.CertificateException;

/**
 * A certificate was rejected. The {@link Reason} tells why, so that callers and tests can tell an expired certificate from a wrong host
 * name or an unknown issuer. Example: <code>catch (X509Exception e) { if (e.reason() == X509Exception.Reason.EXPIRED) { ... } }</code>.
 * <p>
 * 인증서를 거부했다. {@link Reason} 이 이유를 알려 주므로 호출자와 시험이 만료된 인증서, 틀린 호스트 이름, 알 수 없는 발급자를 구분할 수 있다.
 */
public final class X509Exception extends CertificateException {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /**
     * The reasons for rejecting a certificate or a chain.
     * <p>
     * 인증서나 사슬을 거부하는 이유.
     */
    public enum Reason {
        /** The DER encoding or the structure is wrong. / DER 부호화나 구조가 틀렸다. */
        MALFORMED,
        /** The certificate is past its notAfter date. / 인증서의 notAfter 날짜가 지났다. */
        EXPIRED,
        /** The certificate is before its notBefore date. / 인증서의 notBefore 날짜 전이다. */
        NOT_YET_VALID,
        /** The certificate does not name the host. / 인증서가 그 호스트의 이름을 갖지 않는다. */
        NAME_MISMATCH,
        /** No path to a trusted root was found. / 신뢰하는 루트까지의 경로를 찾지 못했다. */
        UNTRUSTED,
        /** A signature in the chain is wrong. / 사슬의 서명이 틀렸다. */
        BAD_SIGNATURE,
        /** An issuing certificate is not a certification authority. / 발급 인증서가 인증 기관이 아니다. */
        NOT_CA,
        /** The path is longer than a path length constraint allows. / 경로가 경로 길이 제약보다 길다. */
        PATH_LENGTH,
        /** The key usage does not allow the use. / 키 용도가 그 사용을 허락하지 않는다. */
        KEY_USAGE,
        /** The extended key usage does not allow TLS server authentication. / 확장 키 용도가 TLS 서버 인증을 허락하지 않는다. */
        EXTENDED_KEY_USAGE,
        /** A name constraint of an issuer is violated. / 발급자의 이름 제약을 어겼다. */
        NAME_CONSTRAINTS,
        /** A key is too short or of an unsupported kind. / 키가 너무 짧거나 지원하지 않는 종류이다. */
        WEAK_KEY,
        /** A signature uses a broken hash such as SHA-1 or MD5. / 서명이 SHA-1, MD5 같은 깨진 해시를 쓴다. */
        WEAK_SIGNATURE,
        /** A signature or key algorithm is not supported. / 서명 또는 키 알고리즘을 지원하지 않는다. */
        UNSUPPORTED_ALGORITHM,
        /** A critical extension is not understood. / critical 확장을 이해하지 못한다. */
        UNKNOWN_CRITICAL_EXTENSION,
        /** The root is distrusted for certificates issued after a date. / 그 날짜 뒤에 발급한 인증서에 대해 루트를 신뢰하지 않는다. */
        DISTRUSTED,
        /** The chain has too many certificates. / 사슬에 인증서가 너무 많다. */
        TOO_LONG
    }

    /** The reason. / 이유. */
    private final Reason reason;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param reason  the reason / 이유
     * @param message the detail / 상세 설명
     */
    public X509Exception(Reason reason, String message) {
        super(reason + ": " + message);
        this.reason = reason;
    }

    /**
     * Returns the reason.
     * <p>
     * 이유를 반환한다.
     *
     * @return the reason / 이유
     */
    public Reason reason() {
        return reason;
    }
}
