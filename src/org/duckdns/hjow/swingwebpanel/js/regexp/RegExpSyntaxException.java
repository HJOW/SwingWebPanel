package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * Signals that a regular expression pattern or flags string is invalid (an early error). The JavaScript glue code
 * converts it into a <code>SyntaxError</code> object whose message is {@link #getMessage()}.
 * <p>
 * 정규식 패턴 또는 플래그 문자열이 올바르지 않음(조기 오류)을 알린다. JavaScript 연결 코드가 {@link #getMessage()} 를 메시지로 하는
 * <code>SyntaxError</code> 객체로 바꾼다.
 */
public class RegExpSyntaxException extends Exception {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The pattern (or the flags string) that was rejected.
     * <p>
     * 거부된 패턴(또는 플래그 문자열).
     */
    private final String source;

    /**
     * Offset of the problem in the source, or -1 when it concerns the whole text.
     * <p>
     * 원본에서 문제 위치. 전체에 대한 문제이면 -1.
     */
    private final int position;

    /**
     * The bare reason without the "Invalid regular expression" prefix.
     * <p>
     * "Invalid regular expression" 접두사 없는 원인 문구.
     */
    private final String reason;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param source   rejected pattern or flags / 거부된 패턴 또는 플래그
     * @param reason   reason such as "Nothing to repeat" / "Nothing to repeat" 같은 원인
     * @param position offset of the problem or -1 / 문제 위치 또는 -1
     */
    public RegExpSyntaxException(String source, String reason, int position) {
        super("Invalid regular expression: /" + source + "/: " + reason);
        this.source = source;
        this.reason = reason;
        this.position = position;
    }

    /**
     * Returns the rejected pattern (or flags string).
     * <p>
     * 거부된 패턴(또는 플래그 문자열)을 반환한다.
     *
     * @return source text / 원본 문자열
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the reason without prefix.
     * <p>
     * 접두사 없는 원인 문구를 반환한다.
     *
     * @return reason / 원인
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the offset of the problem in the source, or -1.
     * <p>
     * 원본에서 문제 위치를 반환한다. 없으면 -1.
     *
     * @return offset / 위치
     */
    public int getPosition() {
        return position;
    }
}
