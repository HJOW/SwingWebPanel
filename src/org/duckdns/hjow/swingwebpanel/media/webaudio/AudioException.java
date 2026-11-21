package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The error of a Web Audio operation. The {@link Kind} tells which exception the script layer must throw: a <code>DOMException</code> with
 * the name of the kind, or a <code>RangeError</code> / <code>TypeError</code> of the language. The engine never throws a Java checked
 * exception, so a control method can be called from any thread without a <code>throws</code> clause.
 * <p>
 * Web Audio 작업의 오류. {@link Kind} 는 스크립트 계층이 던져야 할 예외를 알려 준다: 종류 이름을 가진 <code>DOMException</code> 이거나 언어의
 * <code>RangeError</code> / <code>TypeError</code>. 엔진은 Java 검사 예외를 던지지 않으므로 제어 메서드를 <code>throws</code> 절 없이 어느
 * 스레드에서나 부를 수 있다.
 */
public final class AudioException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The kinds of error.
     * <p>
     * 오류의 종류.
     */
    public enum Kind {
        /**
         * <code>IndexSizeError</code>: an index (output, input, channel) is out of range.
         * <p>
         * <code>IndexSizeError</code>: 인덱스(출력, 입력, 채널)가 범위를 벗어났다.
         */
        INDEX_SIZE("IndexSizeError", true),
        /**
         * <code>NotSupportedError</code>: the value or operation is outside what is supported.
         * <p>
         * <code>NotSupportedError</code>: 값이나 작업이 지원 범위 밖이다.
         */
        NOT_SUPPORTED("NotSupportedError", true),
        /**
         * <code>InvalidStateError</code>: the object is in a state that does not allow the operation.
         * <p>
         * <code>InvalidStateError</code>: 객체의 상태가 작업을 허용하지 않는다.
         */
        INVALID_STATE("InvalidStateError", true),
        /**
         * <code>InvalidAccessError</code>: the connection or object does not match (other context, no such connection).
         * <p>
         * <code>InvalidAccessError</code>: 연결이나 객체가 맞지 않는다(다른 컨텍스트, 그런 연결 없음).
         */
        INVALID_ACCESS("InvalidAccessError", true),
        /**
         * <code>EncodingError</code>: audio data could not be decoded.
         * <p>
         * <code>EncodingError</code>: 오디오 데이터를 해독할 수 없다.
         */
        ENCODING("EncodingError", true),
        /**
         * The language <code>RangeError</code>: a number is outside its allowed range.
         * <p>
         * 언어의 <code>RangeError</code>: 숫자가 허용 범위를 벗어났다.
         */
        RANGE("RangeError", false),
        /**
         * The language <code>TypeError</code>: the argument has the wrong type, for example a number that is not finite.
         * <p>
         * 언어의 <code>TypeError</code>: 인수의 형식이 틀렸다. 예를 들어 유한하지 않은 숫자.
         */
        TYPE("TypeError", false);

        /**
         * The name of the exception.
         * <p>
         * 예외의 이름.
         */
        private final String exceptionName;

        /**
         * True when the exception is a <code>DOMException</code>.
         * <p>
         * 예외가 <code>DOMException</code> 이면 true.
         */
        private final boolean dom;

        /**
         * Creates a kind.
         * <p>
         * 종류를 만든다.
         *
         * @param exceptionName the name of the exception / 예외의 이름
         * @param dom           true for a DOMException / DOMException 이면 true
         */
        Kind(String exceptionName, boolean dom) {
            this.exceptionName = exceptionName;
            this.dom = dom;
        }

        /**
         * Returns the name of the exception (the <code>name</code> of the DOMException, or the language error name).
         * <p>
         * 예외의 이름(DOMException 의 <code>name</code> 또는 언어 오류 이름)을 반환한다.
         *
         * @return the name / 이름
         */
        public String getExceptionName() {
            return exceptionName;
        }

        /**
         * Tells whether the script layer must throw a DOMException.
         * <p>
         * 스크립트 계층이 DOMException 을 던져야 하는지 알려 준다.
         *
         * @return true for a DOMException, false for a RangeError or TypeError / DOMException 이면 true, RangeError 나 TypeError 이면 false
         */
        public boolean isDomException() {
            return dom;
        }
    }

    /**
     * The kind of this error.
     * <p>
     * 이 오류의 종류.
     */
    private final Kind kind;

    /**
     * Creates an error.
     * <p>
     * 오류를 만든다.
     *
     * @param kind    the kind / 종류
     * @param message the message / 메시지
     */
    public AudioException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    /**
     * Returns the kind of this error.
     * <p>
     * 이 오류의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the exception name followed by the message.
     * <p>
     * 예외 이름과 메시지를 반환한다.
     *
     * @return the text / 문자열
     */
    @Override
    public String toString() {
        return kind.getExceptionName() + ": " + getMessage();
    }
}
