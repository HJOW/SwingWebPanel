package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * Signals invalid JSON text found by {@link JsonTokenizer}. The JavaScript glue code converts it into a
 * <code>SyntaxError</code> object. The message follows the familiar engine wording, for example
 * "Unexpected token } in JSON at position 5" or "Unexpected end of JSON input".
 * <p>
 * {@link JsonTokenizer} 가 찾은 올바르지 않은 JSON 텍스트를 알린다. JavaScript 연결 코드가 이를 <code>SyntaxError</code> 객체로 바꾼다.
 * 메시지는 흔히 쓰이는 엔진 문구를 따른다. 예: "Unexpected token } in JSON at position 5", "Unexpected end of JSON input".
 */
public class JsonSyntaxException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Offset in the text where the problem was found (text length at the end of input).
     * <p>
     * 문제를 찾은 텍스트 위치(입력 끝이면 텍스트 길이).
     */
    private final int position;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message  description / 설명
     * @param position offset of the problem / 문제 위치
     */
    public JsonSyntaxException(String message, int position) {
        super(message);
        this.position = position;
    }

    /**
     * Returns the offset where the problem was found.
     * <p>
     * 문제를 찾은 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getPosition() {
        return position;
    }
}
