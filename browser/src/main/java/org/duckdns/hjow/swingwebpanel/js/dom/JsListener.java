package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * A DOM event listener that calls a JavaScript function or the <code>handleEvent</code> method of a JavaScript object. Two listeners are
 * equal when they wrap the same callback object, which is how <code>removeEventListener</code> finds the listener that
 * <code>addEventListener</code> registered. An error thrown by the callback is reported to the page ({@link DomBridge#reportError}) and does
 * not stop the other listeners.
 * <p>
 * JavaScript 함수 또는 JavaScript 객체의 <code>handleEvent</code> 메서드를 부르는 DOM 이벤트 리스너. 같은 콜백 객체를 감싼 두 리스너는 같다.
 * <code>removeEventListener</code> 가 <code>addEventListener</code> 가 등록한 리스너를 찾는 방법이다. 콜백이 던진 오류는 페이지에 보고하며
 * ({@link DomBridge#reportError}) 다른 리스너를 멈추지 않는다.
 */
public final class JsListener implements EventListener {

    /**
     * The bridge of the page.
     * <p>
     * 페이지의 브리지.
     */
    private final DomBridge bridge;

    /**
     * The function or the object with <code>handleEvent</code>.
     * <p>
     * 함수 또는 <code>handleEvent</code> 를 가진 객체.
     */
    private final JsObject callback;

    /**
     * Creates a listener.
     * <p>
     * 리스너를 만든다.
     *
     * @param bridge   the bridge / 브리지
     * @param callback the callback object / 콜백 객체
     */
    public JsListener(DomBridge bridge, JsObject callback) {
        this.bridge = bridge;
        this.callback = callback;
    }

    /**
     * Calls the callback with the event; <code>this</code> is the current target.
     * <p>
     * 이벤트와 함께 콜백을 부른다. <code>this</code> 는 현재 대상이다.
     *
     * @param event the event / 이벤트
     */
    public void handleEvent(Event event) {
        if (!bridge.isEventExecutionEnabled()) {
            return;
        }
        JsValue previousEvent = null;
        try {
            JsValue self = event.getCurrentTarget() == null ? Js.UNDEFINED : bridge.toJs(event.getCurrentTarget());
            JsValue arg = bridge.toJs(event);
            // 리스너가 도는 동안 window.event 는 이 이벤트이다(중첩 전달을 위해 이전 값을 보관한다).
            previousEvent = bridge.enterEvent(arg);
            if (callback.isCallable()) {
                Ops.call(callback, self, new JsValue[] { arg });
            } else {
                JsValue m = callback.get("handleEvent");
                if (m.isCallable()) {
                    Ops.call(m, callback, new JsValue[] { arg });
                }
            }
        } catch (JsException e) {
            bridge.reportError(e);
        } finally {
            if (previousEvent != null) {
                bridge.leaveEvent(previousEvent);
            }
        }
    }

    /**
     * Compares by callback identity.
     * <p>
     * 콜백 동일성으로 비교한다.
     *
     * @param o the other object / 다른 객체
     * @return true for the same callback / 같은 콜백이면 true
     */
    public boolean equals(Object o) {
        return o instanceof JsListener && ((JsListener) o).callback == callback;
    }

    /**
     * Returns the hash code of the callback identity.
     * <p>
     * 콜백 동일성의 해시 코드를 반환한다.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return System.identityHashCode(callback);
    }
}
