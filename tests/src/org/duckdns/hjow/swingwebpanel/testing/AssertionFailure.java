package org.duckdns.hjow.swingwebpanel.testing;

/**
 * Thrown by {@link Check} when an expectation is not met.
 * It is a normal exception (not a Java <code>assert</code>), so it works even when assertions are disabled by JVM options.
 * <p>
 * {@link Check} 의 기대 조건이 맞지 않을 때 던지는 예외.
 * Java <code>assert</code> 가 아닌 일반 예외이므로 JVM 옵션으로 assert 가 꺼져 있어도 동작한다.
 */
public class AssertionFailure extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a failure with a message.
     * <p>
     * 메시지가 있는 실패 예외를 만든다.
     *
     * @param message failure description / 실패 설명
     */
    public AssertionFailure(String message) {
        super(message);
    }
}
