package org.duckdns.hjow.swingwebpanel.js;

/**
 * Thrown when running script code is aborted by the host or by an execution limit (step limit, time limit, cancellation).
 * It is a plain Java exception that script code can <b>not</b> catch and that does not run script <code>finally</code> blocks,
 * so a runaway script cannot defeat the limit by catching the error. The host catches it around
 * {@link JsRealm#evaluateScript(String, String)} and similar calls.
 * <p>
 * 호스트가 실행을 중단했거나 실행 제한(단계 수, 시간, 취소)에 걸려 스크립트 실행이 중단될 때 던진다. 스크립트 코드가 잡을 수
 * <b>없고</b> 스크립트의 <code>finally</code> 블록도 실행하지 않는 일반 Java 예외이므로, 폭주하는 스크립트가 오류를 잡아 제한을
 * 무력화할 수 없다. 호스트는 {@link JsRealm#evaluateScript(String, String)} 같은 호출 주위에서 이를 잡는다.
 */
public final class ScriptAbortedException extends RuntimeException {

    /**
     * The reason for the abort.
     * <p>
     * 중단 이유.
     */
    public enum Reason {
        /**
         * The step limit was exceeded.
         * <p>
         * 단계 수 제한을 넘었다.
         */
        STEP_LIMIT,
        /**
         * The wall-clock time limit was exceeded.
         * <p>
         * 실제 시간 제한을 넘었다.
         */
        TIME_LIMIT,
        /**
         * The host cancelled the execution.
         * <p>
         * 호스트가 실행을 취소했다.
         */
        CANCELLED
    }

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The reason.
     * <p>
     * 이유.
     */
    private final Reason reason;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param reason  the reason / 이유
     * @param message a description / 설명
     */
    public ScriptAbortedException(Reason reason, String message) {
        super(message, null);
        this.reason = reason;
    }

    /**
     * Returns the reason.
     * <p>
     * 이유를 반환한다.
     *
     * @return the reason / 이유
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Disables stack trace capture for speed.
     * <p>
     * 속도를 위해 스택 추적 수집을 끈다.
     *
     * @return this exception / 이 예외
     */
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
