package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * Thrown while matching when a configured resource limit is exceeded (step budget, backtracking stack) or the match
 * was cancelled. It protects the caller against catastrophic backtracking. The JavaScript glue code converts it into a
 * <code>RangeError</code> (or aborts the script when the cancellation comes from the engine).
 * <p>
 * 설정한 자원 제한(단계 예산, 역추적 스택)을 넘었거나 일치 검색이 취소되었을 때 일치 검색 중에 던져진다. 치명적인 역추적으로부터 호출자를
 * 보호한다. JavaScript 연결 코드가 이를 <code>RangeError</code> 로 바꾼다(취소가 엔진에서 온 경우에는 스크립트를 중단한다).
 */
public class RegExpLimitException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * True if the match was cancelled through {@link RegExpCancel}, false for a resource limit.
     * <p>
     * {@link RegExpCancel} 로 취소되었으면 true, 자원 제한이면 false.
     */
    private final boolean cancelled;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message   description / 설명
     * @param cancelled whether the match was cancelled / 취소 여부
     */
    public RegExpLimitException(String message, boolean cancelled) {
        super(message);
        this.cancelled = cancelled;
    }

    /**
     * Tells whether the match was cancelled (as opposed to exceeding a limit).
     * <p>
     * 취소된 것인지(한도 초과가 아니라) 알려 준다.
     *
     * @return true if cancelled / 취소되었으면 true
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
