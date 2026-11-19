package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * The control-flow signal that unwinds the Java stack when a generator yields. It carries no data (the yielded value is in the
 * {@link GenState}) and is never caught by script <code>try</code> statements, so a single shared instance is used.
 * <p>
 * 제너레이터가 yield 할 때 Java 스택을 풀어 내는 제어 흐름 신호. 데이터는 없으며(내보낸 값은 {@link GenState} 에 있다) 스크립트의
 * <code>try</code> 문이 잡지 않으므로 공유 인스턴스 하나를 쓴다.
 */
final class YieldSignal extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The shared instance.
     * <p>
     * 공유 인스턴스.
     */
    static final YieldSignal INSTANCE = new YieldSignal();

    /**
     * Private constructor: use {@link #INSTANCE}.
     * <p>
     * 비공개 생성자: {@link #INSTANCE} 를 사용한다.
     */
    private YieldSignal() {
        super(null, null);
    }

    /**
     * Disables stack trace capture.
     * <p>
     * 스택 추적 수집을 끈다.
     *
     * @return this exception / 이 예외
     */
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
