package org.duckdns.hjow.swingwebpanel.tls13;

/**
 * Tells callers what a failed connection of this module means, without a compile time dependency on the module: every method is
 * static and takes and returns JRE types only, so a program that loads the module optionally can call it by reflection. Example:
 * <pre>
 * Class&lt;?&gt; c = Class.forName("org.duckdns.hjow.swingwebpanel.tls13.Tls13Failures");
 * Boolean retry = (Boolean) c.getMethod("isNegotiationFailure", Throwable.class).invoke(null, failure);
 * </pre>
 * <p>
 * 이 모듈의 연결 실패가 무엇을 뜻하는지 호출자에게 알려 준다. 모듈에 컴파일 의존성이 없어도 쓸 수 있도록 모든 메서드가 static 이고 JRE 형식만 받고
 * 반환하므로, 모듈을 선택적으로 불러오는 프로그램이 리플렉션으로 부를 수 있다.
 */
public final class Tls13Failures {

    /** The longest cause chain that is followed. / 따라가는 원인 사슬의 최대 길이. */
    private static final int MAX_DEPTH = 16;

    /**
     * Not instantiated.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private Tls13Failures() {
    }

    /**
     * Tells if a failure means that this module and the server have no protocol version or cipher suite in common: the server chose
     * TLS 1.1 or older, or refused the ClientHello with a handshake_failure, protocol_version or insufficient_security alert. Only
     * then may a caller try the same server once more with another TLS implementation (for example the one of the JRE). Every other
     * failure of the module (a rejected certificate or host name, a wrong signature or Finished value, a downgrade marker, a
     * protocol violation) returns false and must not be retried with a weaker implementation, and so do failures that are not from
     * this module (connection refused, timeouts and so on).
     * <p>
     * 실패가 이 모듈과 서버에 공통된 프로토콜 버전이나 암호 스위트가 없다는 뜻인지 알려 준다: 서버가 TLS 1.1 이하를 골랐거나, handshake_failure,
     * protocol_version, insufficient_security 경보로 ClientHello 를 거절한 경우이다. 이때만 호출자가 같은 서버를 다른 TLS 구현(예: JRE 의 구현)으로
     * 한 번 더 시도해도 된다. 모듈의 그 밖의 실패(거부된 인증서나 호스트 이름, 틀린 서명이나 Finished 값, 다운그레이드 표식, 프로토콜 위반)는 false 이며
     * 더 약한 구현으로 다시 시도하면 안 된다. 이 모듈의 것이 아닌 실패(연결 거부, 시간 초과 등)도 false 이다.
     *
     * @param failure the exception of the failed connection, or one that has it as cause; may be null / 실패한 연결의 예외 또는 그것을 원인으로 가진 예외. null 가능
     * @return true for a negotiation failure / 협상 실패이면 true
     */
    public static boolean isNegotiationFailure(Throwable failure) {
        Tls13Exception e = find(failure);
        return e != null && e.kind() == Tls13Exception.Kind.NEGOTIATION;
    }

    /**
     * Tells if a failure (or one of its causes) was raised by this module.
     * <p>
     * 실패(또는 그 원인 중 하나)를 이 모듈이 일으켰는지 알려 준다.
     *
     * @param failure the exception; may be null / 예외. null 가능
     * @return true for a failure of this module / 이 모듈의 실패이면 true
     */
    public static boolean isModuleFailure(Throwable failure) {
        return find(failure) != null;
    }

    /**
     * Finds the exception of this module in a cause chain.
     * <p>
     * 원인 사슬에서 이 모듈의 예외를 찾는다.
     *
     * @param failure the exception or null / 예외 또는 null
     * @return the module exception or null / 모듈의 예외 또는 null
     */
    private static Tls13Exception find(Throwable failure) {
        Throwable t = failure;
        // 원인 사슬을 따라가며 모듈의 예외를 찾는다. 순환에 대비해 길이를 제한한다.
        for (int depth = 0; t != null && depth < MAX_DEPTH; depth++) {
            if (t instanceof Tls13Exception) {
                return (Tls13Exception) t;
            }
            t = t.getCause();
        }
        return null;
    }
}
