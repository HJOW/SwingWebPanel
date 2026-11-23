package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * Signals malformed input to encodeURI, encodeURIComponent, decodeURI or decodeURIComponent: a lone surrogate while
 * encoding, or a bad percent sequence or bad UTF-8 while decoding. The JavaScript glue code converts it into a
 * <code>URIError</code> object.
 * <p>
 * encodeURI, encodeURIComponent, decodeURI, decodeURIComponent 에 올바르지 않은 입력이 들어왔음을 알린다:
 * 부호화 중의 짝 없는 서로게이트, 복호화 중의 잘못된 퍼센트 시퀀스나 잘못된 UTF-8. JavaScript 연결 코드가 이 예외를
 * <code>URIError</code> 객체로 바꾼다.
 */
public class UriFormatException extends RuntimeException {

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
     * @param message description, normally "URI malformed" / 설명(보통 "URI malformed")
     */
    public UriFormatException(String message) {
        super(message);
    }
}
