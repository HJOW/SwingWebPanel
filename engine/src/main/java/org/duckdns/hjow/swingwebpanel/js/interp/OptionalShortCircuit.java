package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * Internal control signal used to skip the remainder of one optional chain.
 * It is not a JavaScript exception and is always caught by the enclosing OPTIONAL_CHAIN node.
 * <p>
 * 하나의 optional chain 나머지를 건너뛰기 위한 내부 제어 신호이다.
 * JavaScript 예외가 아니며 바깥 OPTIONAL_CHAIN 노드가 반드시 잡는다.
 */
final class OptionalShortCircuit extends RuntimeException {

    /**
     * Shared signal instance.
     * <p>
     * 공유 신호 인스턴스.
     */
    static final OptionalShortCircuit INSTANCE = new OptionalShortCircuit();

    /**
     * Prevents external construction.
     * <p>
     * 외부 생성을 막는다.
     */
    private OptionalShortCircuit() {
        super();
    }
}

