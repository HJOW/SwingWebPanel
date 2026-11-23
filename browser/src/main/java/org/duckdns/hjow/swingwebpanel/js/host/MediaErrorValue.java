package org.duckdns.hjow.swingwebpanel.js.host;

/**
 * The value behind a script's <code>MediaError</code> object (the <code>error</code> attribute of a media element, plan item P18-08): the
 * error code of HTML5 section 4.7.10.1 and a message. The class is public because the DOM bridge reads its getters.
 * <p>
 * 스크립트의 <code>MediaError</code> 객체(미디어 요소의 <code>error</code> 속성, 계획 P18-08) 뒤의 값: HTML5 4.7.10.1 의 오류 코드와 메시지.
 * DOM 브리지가 getter 를 읽으므로 클래스는 public 이다.
 */
public final class MediaErrorValue {

    /**
     * The error code, 1 to 4.
     * <p>
     * 오류 코드(1에서 4).
     */
    private final int code;

    /**
     * The message.
     * <p>
     * 메시지.
     */
    private final String message;

    /**
     * Creates the value.
     * <p>
     * 값을 만든다.
     *
     * @param code    the error code (<code>MEDIA_ERR_ABORTED</code> 1, <code>MEDIA_ERR_NETWORK</code> 2, <code>MEDIA_ERR_DECODE</code> 3,
     *                <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> 4) / 오류 코드
     * @param message the message (may be empty) / 메시지(비어 있어도 됨)
     */
    MediaErrorValue(int code, String message) {
        this.code = code;
        this.message = message == null ? "" : message;
    }

    /**
     * Returns the error code.
     * <p>
     * 오류 코드를 반환한다.
     *
     * @return the code / 코드
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the message.
     * <p>
     * 메시지를 반환한다.
     *
     * @return the message / 메시지
     */
    public String getMessage() {
        return message;
    }
}
