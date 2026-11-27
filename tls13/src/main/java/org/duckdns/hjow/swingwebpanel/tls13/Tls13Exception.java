package org.duckdns.hjow.swingwebpanel.tls13;

import javax.net.ssl.SSLException;

/**
 * A TLS connection of this module failed. Besides the message, the exception tells the {@link Kind} of the failure and the TLS alert
 * that was sent or received (RFC 8446 section 6), so that a caller can tell "the server does not speak a version or cipher suite of
 * this module" (which another TLS implementation may still solve) from failures that must not be retried elsewhere, such as a
 * certificate that was rejected. Example:
 * <pre>
 * try {
 *     socket.startHandshake();
 * } catch (Tls13Exception e) {
 *     if (e.kind() == Tls13Exception.Kind.NEGOTIATION) { ... }
 * }
 * </pre>
 * <p>
 * 이 모듈의 TLS 연결이 실패했다. 메시지 외에 실패의 {@link Kind} 와 보내거나 받은 TLS 경보(RFC 8446 6절)를 알려 주므로, 호출자가 "서버가 이 모듈의
 * 버전이나 암호 스위트를 쓰지 않는다"(다른 TLS 구현이 해결할 수도 있다)는 것과, 거부된 인증서처럼 다른 곳에서 다시 시도하면 안 되는 실패를 구분할 수 있다.
 */
public final class Tls13Exception extends SSLException {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** Alert close_notify. / 경보 close_notify. */
    public static final int CLOSE_NOTIFY = 0;

    /** Alert unexpected_message. / 경보 unexpected_message. */
    public static final int UNEXPECTED_MESSAGE = 10;

    /** Alert bad_record_mac. / 경보 bad_record_mac. */
    public static final int BAD_RECORD_MAC = 20;

    /** Alert record_overflow. / 경보 record_overflow. */
    public static final int RECORD_OVERFLOW = 22;

    /** Alert handshake_failure. / 경보 handshake_failure. */
    public static final int HANDSHAKE_FAILURE = 40;

    /** Alert bad_certificate. / 경보 bad_certificate. */
    public static final int BAD_CERTIFICATE = 42;

    /** Alert unsupported_certificate. / 경보 unsupported_certificate. */
    public static final int UNSUPPORTED_CERTIFICATE = 43;

    /** Alert certificate_expired. / 경보 certificate_expired. */
    public static final int CERTIFICATE_EXPIRED = 45;

    /** Alert certificate_unknown. / 경보 certificate_unknown. */
    public static final int CERTIFICATE_UNKNOWN = 46;

    /** Alert illegal_parameter. / 경보 illegal_parameter. */
    public static final int ILLEGAL_PARAMETER = 47;

    /** Alert unknown_ca. / 경보 unknown_ca. */
    public static final int UNKNOWN_CA = 48;

    /** Alert decode_error. / 경보 decode_error. */
    public static final int DECODE_ERROR = 50;

    /** Alert decrypt_error. / 경보 decrypt_error. */
    public static final int DECRYPT_ERROR = 51;

    /** Alert protocol_version. / 경보 protocol_version. */
    public static final int PROTOCOL_VERSION = 70;

    /** Alert insufficient_security. / 경보 insufficient_security. */
    public static final int INSUFFICIENT_SECURITY = 71;

    /** Alert internal_error. / 경보 internal_error. */
    public static final int INTERNAL_ERROR = 80;

    /** Alert user_canceled. / 경보 user_canceled. */
    public static final int USER_CANCELED = 90;

    /** Alert no_renegotiation. / 경보 no_renegotiation. */
    public static final int NO_RENEGOTIATION = 100;

    /** Alert missing_extension. / 경보 missing_extension. */
    public static final int MISSING_EXTENSION = 109;

    /** Alert unsupported_extension. / 경보 unsupported_extension. */
    public static final int UNSUPPORTED_EXTENSION = 110;

    /** Alert no_application_protocol. / 경보 no_application_protocol. */
    public static final int NO_APPLICATION_PROTOCOL = 120;

    /**
     * The kinds of failure.
     * <p>
     * 실패의 종류.
     */
    public enum Kind {
        /**
         * Client and server have no protocol version or cipher suite in common (the server chose TLS 1.1 or older, or refused the
         * ClientHello with a handshake_failure, protocol_version or insufficient_security alert). Another TLS implementation with older
         * versions or other suites may still connect.
         * <p>
         * 클라이언트와 서버에 공통된 프로토콜 버전이나 암호 스위트가 없다(서버가 TLS 1.1 이하를 골랐거나, handshake_failure, protocol_version,
         * insufficient_security 경보로 ClientHello 를 거절했다). 더 오래된 버전이나 다른 스위트를 가진 TLS 구현은 연결할 수도 있다.
         */
        NEGOTIATION,
        /** The certificate chain of the server was rejected. / 서버의 인증서 사슬을 거부했다. */
        CERTIFICATE,
        /** A signature, a Finished value or a record tag was wrong, or the server violated the protocol. / 서명, Finished 값, 레코드 태그가 틀렸거나 서버가 프로토콜을 어겼다. */
        PROTOCOL,
        /** The server sent a fatal alert after the negotiation. / 협상 뒤에 서버가 치명적 경보를 보냈다. */
        PEER_ALERT
    }

    /** The kind. / 종류. */
    private final Kind kind;

    /** The alert description code. / 경보 설명 코드. */
    private final int alert;

    /** True if the alert was received from the server, false if this side raised it. / 경보를 서버에서 받았으면 true, 이쪽에서 일으켰으면 false. */
    private final boolean received;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param kind     the kind / 종류
     * @param alert    the alert description code / 경보 설명 코드
     * @param received true if the alert came from the server / 경보가 서버에서 왔으면 true
     * @param message  the detail / 상세 설명
     */
    public Tls13Exception(Kind kind, int alert, boolean received, String message) {
        super(message);
        this.kind = kind;
        this.alert = alert;
        this.received = received;
    }

    /**
     * Creates a protocol failure raised by this side.
     * <p>
     * 이쪽에서 일으킨 프로토콜 실패를 만든다.
     *
     * @param alert   the alert to send / 보낼 경보
     * @param message the detail / 상세 설명
     * @return the exception / 예외
     */
    public static Tls13Exception protocol(int alert, String message) {
        return new Tls13Exception(Kind.PROTOCOL, alert, false, message + " (" + alertName(alert) + ")");
    }

    /**
     * Returns the kind of the failure.
     * <p>
     * 실패의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the alert description code (RFC 8446 section 6).
     * <p>
     * 경보 설명 코드를 반환한다(RFC 8446 6절).
     *
     * @return the code / 코드
     */
    public int alert() {
        return alert;
    }

    /**
     * Tells if the alert was received from the server.
     * <p>
     * 경보를 서버에서 받았는지 알려 준다.
     *
     * @return true if received / 받았으면 true
     */
    public boolean received() {
        return received;
    }

    /**
     * Returns the name of an alert description.
     * <p>
     * 경보 설명의 이름을 반환한다.
     *
     * @param alert the code / 코드
     * @return the name, or the number for unknown codes / 이름. 모르는 코드는 숫자
     */
    public static String alertName(int alert) {
        switch (alert) {
        case CLOSE_NOTIFY:
            return "close_notify";
        case UNEXPECTED_MESSAGE:
            return "unexpected_message";
        case BAD_RECORD_MAC:
            return "bad_record_mac";
        case RECORD_OVERFLOW:
            return "record_overflow";
        case HANDSHAKE_FAILURE:
            return "handshake_failure";
        case BAD_CERTIFICATE:
            return "bad_certificate";
        case UNSUPPORTED_CERTIFICATE:
            return "unsupported_certificate";
        case CERTIFICATE_EXPIRED:
            return "certificate_expired";
        case CERTIFICATE_UNKNOWN:
            return "certificate_unknown";
        case ILLEGAL_PARAMETER:
            return "illegal_parameter";
        case UNKNOWN_CA:
            return "unknown_ca";
        case DECODE_ERROR:
            return "decode_error";
        case DECRYPT_ERROR:
            return "decrypt_error";
        case PROTOCOL_VERSION:
            return "protocol_version";
        case INSUFFICIENT_SECURITY:
            return "insufficient_security";
        case INTERNAL_ERROR:
            return "internal_error";
        case USER_CANCELED:
            return "user_canceled";
        case NO_RENEGOTIATION:
            return "no_renegotiation";
        case MISSING_EXTENSION:
            return "missing_extension";
        case UNSUPPORTED_EXTENSION:
            return "unsupported_extension";
        case NO_APPLICATION_PROTOCOL:
            return "no_application_protocol";
        default:
            return "alert " + alert;
        }
    }
}
