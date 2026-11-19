package org.duckdns.hjow.swingwebpanel.js.host.structured;

/**
 * Thrown when a value cannot be copied by the structured clone algorithm (a function, a symbol, a DOM node, a Proxy ..., a structure that
 * is too deep or too large, or a bad transfer list). The host turns it into a <code>DataCloneError</code> <code>DOMException</code> of the
 * page; the engine code and the tests catch it as a plain Java exception.
 * <p>
 * 구조화 복제 알고리즘이 값을 복사할 수 없을 때 던진다(함수, 심볼, DOM 노드, Proxy 등, 너무 깊거나 큰 구조, 잘못된 전달 목록). 호스트는 이것을 페이지의
 * <code>DataCloneError</code> <code>DOMException</code> 으로 바꾸고, 엔진 코드와 시험은 일반 Java 예외로 잡는다.
 */
public final class StructuredCloneException extends RuntimeException {

    /**
     * Serialization version.
     * <p>
     * 직렬화 버전.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message what could not be cloned / 복제할 수 없었던 것
     */
    public StructuredCloneException(String message) {
        super(message);
    }

    /**
     * Returns the DOMException name of this error.
     * <p>
     * 이 오류의 DOMException 이름을 반환한다.
     *
     * @return always <code>DataCloneError</code> / 항상 <code>DataCloneError</code>
     */
    public String getName() {
        return "DataCloneError";
    }
}
