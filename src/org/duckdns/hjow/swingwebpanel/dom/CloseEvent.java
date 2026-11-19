package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>close</code> event of a <code>WebSocket</code> (<code>CloseEvent</code>, HTML Standard "Network" chapter): it tells whether the
 * connection was closed cleanly (<code>wasClean</code>), the status code that the server sent or that the user agent chose
 * (<code>code</code>, 1006 for a connection that was lost or never opened) and the reason text (<code>reason</code>). It does not bubble
 * and cannot be cancelled.
 * <p>
 * <code>WebSocket</code> 의 <code>close</code> 이벤트(<code>CloseEvent</code>, HTML Standard "Network" 장): 연결이 정상적으로 닫혔는지
 * (<code>wasClean</code>), 서버가 보냈거나 사용자 에이전트가 고른 상태 코드(<code>code</code>, 끊어졌거나 열리지 않은 연결은 1006), 이유 문자열
 * (<code>reason</code>)을 알려 준다. 버블링하지 않고 취소할 수 없다.
 */
public class CloseEvent extends Event {

    /**
     * Whether the connection was closed cleanly.
     * <p>
     * 연결이 정상적으로 닫혔는지 여부.
     */
    private final boolean wasClean;

    /**
     * The status code.
     * <p>
     * 상태 코드.
     */
    private final int code;

    /**
     * The reason text.
     * <p>
     * 이유 문자열.
     */
    private final String reason;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type     the event type, usually <code>close</code> / 이벤트 종류. 보통 <code>close</code>
     * @param bubbles  true if it bubbles / 버블링하면 true
     * @param wasClean true if the connection was closed cleanly / 연결이 정상적으로 닫혔으면 true
     * @param code     the status code / 상태 코드
     * @param reason   the reason text / 이유 문자열
     */
    public CloseEvent(String type, boolean bubbles, boolean wasClean, int code, String reason) {
        super(type, bubbles, false);
        this.wasClean = wasClean;
        this.code = code;
        this.reason = reason == null ? "" : reason;
    }

    /**
     * Returns whether the connection was closed cleanly.
     * <p>
     * 연결이 정상적으로 닫혔는지 반환한다.
     *
     * @return true if clean / 정상이면 true
     */
    public boolean getWasClean() {
        return wasClean;
    }

    /**
     * Returns the status code.
     * <p>
     * 상태 코드를 반환한다.
     *
     * @return the code / 코드
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the reason text.
     * <p>
     * 이유 문자열을 반환한다.
     *
     * @return the reason / 이유
     */
    public String getReason() {
        return reason;
    }
}
