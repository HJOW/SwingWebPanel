package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>popstate</code> event (<code>PopStateEvent</code>, HTML5 2014 REC 5.6.10.1): fired at the window when the document moves to
 * another entry of the session history. Its <code>state</code> is the state object of that entry, or null. The state is an arbitrary
 * object; the JavaScript layer keeps a script value in it.
 * <p>
 * <code>popstate</code> 이벤트(<code>PopStateEvent</code>, HTML5 2014 REC 5.6.10.1): 문서가 세션 이력의 다른 항목으로 옮겨 갈 때 window 에 발생한다.
 * <code>state</code> 는 그 항목의 상태 객체이거나 null 이다. 상태는 임의의 객체이며 JavaScript 계층은 스크립트 값을 담는다.
 */
public class PopStateEvent extends Event {

    /**
     * The state object.
     * <p>
     * 상태 객체.
     */
    private Object state;

    /**
     * Creates the event with a null state.
     * <p>
     * 상태가 null 인 이벤트를 만든다.
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public PopStateEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Returns the state object.
     * <p>
     * 상태 객체를 반환한다.
     *
     * @return the state, or null / 상태. 없으면 null
     */
    public Object getState() {
        return state;
    }

    /**
     * Sets the state object (before the event is dispatched).
     * <p>
     * 상태 객체를 지정한다(이벤트를 전달하기 전에).
     *
     * @param value the state / 상태
     */
    public void setState(Object value) {
        this.state = value;
    }
}
