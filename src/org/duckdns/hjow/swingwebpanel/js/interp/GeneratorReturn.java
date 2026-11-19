package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * The signal that carries a <code>return(value)</code> request of a generator through its code: it behaves like a return
 * completion raised at the suspended yield, so enclosing <code>finally</code> blocks run, for-of loops close their iterators,
 * and the generator ends with the value. Script <code>catch</code> clauses do not see it.
 * <p>
 * 제너레이터의 <code>return(value)</code> 요청을 코드 안으로 전달하는 신호. 중단된 yield 에서 발생한 return 완료처럼 동작하므로
 * 바깥의 <code>finally</code> 블록이 실행되고 for-of 가 이터레이터를 닫으며 제너레이터는 그 값으로 끝난다. 스크립트의
 * <code>catch</code> 절은 이를 보지 못한다.
 */
final class GeneratorReturn extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The return value.
     * <p>
     * 반환 값.
     */
    final transient JsValue value;

    /**
     * Creates the signal.
     * <p>
     * 신호를 만든다.
     *
     * @param value the value passed to return() / return() 에 전달된 값
     */
    GeneratorReturn(JsValue value) {
        super(null, null);
        this.value = value;
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
