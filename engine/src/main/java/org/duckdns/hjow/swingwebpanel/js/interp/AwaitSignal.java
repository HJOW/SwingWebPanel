package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * Internal control signal thrown when an async function reaches an await expression.
 * <p>
 * async 함수가 await 식에 도달했을 때 던지는 내부 제어 신호.
 */
final class AwaitSignal extends RuntimeException {

    /**
     * Shared signal.
     * <p>
     * 공유 신호.
     */
    static final AwaitSignal INSTANCE = new AwaitSignal();

    /**
     * Prevents external construction.
     * <p>
     * 외부 생성을 막는다.
     */
    private AwaitSignal() {
        super();
    }
}

