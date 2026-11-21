package org.duckdns.hjow.swingwebpanel.js.num;

/**
 * Signals an argument that ECMAScript reports as a <code>RangeError</code> (for example a digit count outside 0..20
 * in toFixed or a radix outside 2..36 in toString). The JavaScript glue code converts it into a RangeError object.
 * <p>
 * ECMAScript 가 <code>RangeError</code> 로 보고하는 인수 오류를 알린다(예: toFixed 의 자릿수가 0..20 밖이거나 toString 의 진법이 2..36 밖).
 * JavaScript 연결 코드가 이 예외를 RangeError 객체로 바꾼다.
 */
public class NumberRangeException extends IllegalArgumentException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message.
     * <p>
     * 메시지가 있는 예외를 만든다.
     *
     * @param message description such as "toFixed() digits argument must be between 0 and 20" / 설명
     */
    public NumberRangeException(String message) {
        super(message);
    }
}
