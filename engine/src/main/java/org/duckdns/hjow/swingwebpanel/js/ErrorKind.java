package org.duckdns.hjow.swingwebpanel.js;

/**
 * The native error types of ECMA-262 6th edition 19.5.5, used to throw errors from Java code without a realm at hand
 * (see {@link JsException}).
 * <p>
 * ECMA-262 6판 19.5.5 의 기본 오류 형식. realm 이 없는 Java 코드에서 오류를 던질 때 쓴다({@link JsException} 참조).
 */
public enum ErrorKind {
    /**
     * The generic Error type.
     * <p>
     * 일반 Error 형식(기본 오류).
     */
    ERROR("Error"),
    /**
     * The EvalError type for misuse of eval.
     * <p>
     * eval 오용을 나타내는 EvalError 형식(평가 오류).
     */
    EVAL_ERROR("EvalError"),
    /**
     * The RangeError type for values outside the allowed range.
     * <p>
     * 허용 범위를 벗어난 값을 나타내는 RangeError 형식(범위 오류).
     */
    RANGE_ERROR("RangeError"),
    /**
     * The ReferenceError type for unresolvable references.
     * <p>
     * 찾을 수 없는 참조를 나타내는 ReferenceError 형식(참조 오류).
     */
    REFERENCE_ERROR("ReferenceError"),
    /**
     * The SyntaxError type for invalid source text.
     * <p>
     * 잘못된 소스 텍스트를 나타내는 SyntaxError 형식(구문 오류).
     */
    SYNTAX_ERROR("SyntaxError"),
    /**
     * The TypeError type for operations on values of the wrong type.
     * <p>
     * 맞지 않는 형식의 값에 대한 연산을 나타내는 TypeError 형식(형식 오류).
     */
    TYPE_ERROR("TypeError"),
    /**
     * The URIError type for malformed URI handling.
     * <p>
     * 잘못된 URI 처리를 나타내는 URIError 형식(URI 오류).
     */
    URI_ERROR("URIError");

    /**
     * The constructor name, also the value of the prototype's name property.
     * <p>
     * 생성자 이름이자 프로토타입의 name 프로퍼티 값.
     */
    private final String errorName;

    /**
     * Creates a constant.
     * <p>
     * 상수를 만든다.
     *
     * @param errorName the JavaScript name / JavaScript 이름
     */
    private ErrorKind(String errorName) {
        this.errorName = errorName;
    }

    /**
     * Returns the JavaScript name such as "TypeError".
     * <p>
     * "TypeError" 같은 JavaScript 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getErrorName() {
        return errorName;
    }
}
