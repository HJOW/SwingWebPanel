package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * Signals an argument that ECMAScript reports as a <code>RangeError</code> (invalid normalization form, invalid code point,
 * repeat count out of range ...). The JavaScript glue code converts it into a RangeError object.
 * <p>
 * ECMAScript 가 <code>RangeError</code> 로 보고하는 인수 오류(잘못된 정규화 형식, 잘못된 코드 포인트, 범위를 벗어난 반복 횟수 등)를 알린다.
 * JavaScript 연결 코드가 이 예외를 RangeError 객체로 바꾼다.
 */
public class TextRangeException extends IllegalArgumentException {

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
    public TextRangeException(String message) {
        super(message);
    }
}
