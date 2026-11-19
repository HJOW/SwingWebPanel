package org.duckdns.hjow.swingwebpanel.js.date;

/**
 * Signals that ECMAScript reports a <code>RangeError</code>, for example <code>toISOString</code> on an invalid date
 * ("Invalid time value"). The JavaScript glue code converts it into a RangeError object.
 * <p>
 * ECMAScript 가 <code>RangeError</code> 로 보고하는 경우를 알린다. 예를 들어 올바르지 않은 날짜에 대한 <code>toISOString</code>
 * ("Invalid time value"). JavaScript 연결 코드가 이를 RangeError 객체로 바꾼다.
 */
public class DateRangeException extends IllegalArgumentException {

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
     * @param message description / 설명
     */
    public DateRangeException(String message) {
        super(message);
    }
}
