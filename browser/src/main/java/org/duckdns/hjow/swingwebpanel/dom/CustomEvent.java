package org.duckdns.hjow.swingwebpanel.dom;

/**
 * An event with an application defined payload (<code>CustomEvent</code>). The payload is an arbitrary object; the JavaScript layer keeps
 * a script value in it.
 * <p>
 * 응용이 정한 값을 실어 나르는 이벤트(<code>CustomEvent</code>). 값은 임의의 객체이며 JavaScript 계층은 스크립트 값을 담는다.
 */
public class CustomEvent extends Event {

    /**
     * The payload.
     * <p>
     * 실려 가는 값.
     */
    private Object detail;

    /**
     * Creates a custom event without payload.
     * <p>
     * 값이 없는 사용자 정의 이벤트를 만든다.
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public CustomEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Creates a custom event with a payload.
     * <p>
     * 값이 있는 사용자 정의 이벤트를 만든다.
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     * @param detail     payload / 실려 가는 값
     */
    public CustomEvent(String type, boolean bubbles, boolean cancelable, Object detail) {
        super(type, bubbles, cancelable);
        this.detail = detail;
    }

    /**
     * Returns the payload.
     * <p>
     * 실려 가는 값을 반환한다.
     *
     * @return payload or null / 값 또는 null
     */
    public Object getDetail() {
        return detail;
    }

    /**
     * Re-initializes the event with a payload (the legacy <code>initCustomEvent</code>).
     * <p>
     * 값과 함께 이벤트를 다시 초기화한다(옛 <code>initCustomEvent</code>).
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     * @param value      payload / 값
     */
    public void initCustomEvent(String type, boolean bubbles, boolean cancelable, Object value) {
        initEvent(type, bubbles, cancelable);
        this.detail = value;
    }
}
