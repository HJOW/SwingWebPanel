package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A focus event (UI Events): <code>focus</code>, <code>blur</code>, <code>focusin</code> and <code>focusout</code>, with the element that
 * loses or gets the focus at the same time.
 * <p>
 * 포커스 이벤트(UI Events): <code>focus</code>, <code>blur</code>, <code>focusin</code>, <code>focusout</code> 이며 동시에 포커스를 잃거나 얻는
 * 요소를 가진다.
 */
public class FocusEvent extends UiEvent {

    /**
     * The other element of the focus change, or null.
     * <p>
     * 포커스 변경의 상대 요소. 없으면 null.
     */
    private EventTarget relatedTarget;

    /**
     * Creates a focus event.
     * <p>
     * 포커스 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public FocusEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets the related target.
     * <p>
     * 상대 대상을 지정한다.
     *
     * @param target the other element or null / 상대 요소. 없으면 null
     */
    public void setRelatedTarget(EventTarget target) {
        this.relatedTarget = target;
    }

    /**
     * Returns the related target.
     * <p>
     * 상대 대상을 반환한다.
     *
     * @return the other element or null / 상대 요소. 없으면 null
     */
    public EventTarget getRelatedTarget() {
        return relatedTarget;
    }
}
