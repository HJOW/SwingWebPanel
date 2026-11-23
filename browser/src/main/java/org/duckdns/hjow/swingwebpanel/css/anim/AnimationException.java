package org.duckdns.hjow.swingwebpanel.css.anim;

/**
 * An error that a procedure of Web Animations throws: a <code>TypeError</code> or a <code>DOMException</code> with a name
 * (<code>InvalidStateError</code>, <code>NoModificationAllowedError</code>, <code>SyntaxError</code>). The script binding turns it into the
 * JavaScript error of that kind; Java callers catch it like any runtime exception.
 * <p>
 * Web Animations 의 절차가 던지는 오류: <code>TypeError</code> 또는 이름을 가진 <code>DOMException</code>(<code>InvalidStateError</code>,
 * <code>NoModificationAllowedError</code>, <code>SyntaxError</code>). 스크립트 연결은 그 종류의 JavaScript 오류로 바꾸고 Java 호출자는 다른 실행 시간 예외처럼 잡는다.
 */
public final class AnimationException extends RuntimeException {

    /** Serialization version. 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The kind of a <code>TypeError</code>. <code>TypeError</code> 의 종류. */
    public static final String TYPE_ERROR = "TypeError";
    /** The name of the <code>DOMException</code> for a wrong state. 잘못된 상태의 <code>DOMException</code> 이름. */
    public static final String INVALID_STATE = "InvalidStateError";
    /** The name of the <code>DOMException</code> for a target without a style attribute. style 속성이 없는 대상의 <code>DOMException</code> 이름. */
    public static final String NO_MODIFICATION = "NoModificationAllowedError";

    /** The kind: {@link #TYPE_ERROR} or the name of a <code>DOMException</code>. 종류: {@link #TYPE_ERROR} 또는 <code>DOMException</code> 의 이름. */
    private final String kind;

    /**
     * Creates the error.
     * <p>
     * 오류를 만든다.
     *
     * @param kind    {@link #TYPE_ERROR} or a <code>DOMException</code> name / {@link #TYPE_ERROR} 또는 <code>DOMException</code> 이름
     * @param message the message / 메시지
     */
    public AnimationException(String kind, String message) {
        super(message);
        this.kind = kind;
    }

    /**
     * Returns the kind of the error.
     * <p>
     * 오류의 종류를 반환한다.
     *
     * @return {@link #TYPE_ERROR} or a <code>DOMException</code> name / {@link #TYPE_ERROR} 또는 <code>DOMException</code> 이름
     */
    public String getKind() {
        return kind;
    }
}
