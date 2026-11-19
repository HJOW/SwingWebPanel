package org.duckdns.hjow.swingwebpanel.dom;

/**
 * An event that comes from the user interface (UI Events): a detail number and the state of the modifier keys that were down when it
 * happened. It is the base of the mouse, keyboard, focus, input and composition events.
 * <p>
 * 사용자 인터페이스에서 오는 이벤트(UI Events): 상세 숫자와 이벤트가 일어날 때 눌려 있던 보조 키의 상태. 마우스, 키보드, 포커스, 입력, 조합 이벤트의
 * 기반이다.
 */
public class UiEvent extends Event {

    /**
     * The detail number (for example the click count of a mouse event).
     * <p>
     * 상세 숫자(예: 마우스 이벤트의 클릭 횟수).
     */
    private int detail;

    /**
     * True while the Control key was down.
     * <p>
     * Control 키가 눌려 있었으면 true.
     */
    private boolean ctrlKey;

    /**
     * True while the Shift key was down.
     * <p>
     * Shift 키가 눌려 있었으면 true.
     */
    private boolean shiftKey;

    /**
     * True while the Alt key was down.
     * <p>
     * Alt 키가 눌려 있었으면 true.
     */
    private boolean altKey;

    /**
     * True while the Meta key (the Windows or Command key) was down.
     * <p>
     * Meta 키(Windows 또는 Command 키)가 눌려 있었으면 true.
     */
    private boolean metaKey;

    /**
     * Creates a UI event.
     * <p>
     * UI 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public UiEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Returns the detail number.
     * <p>
     * 상세 숫자를 반환한다.
     *
     * @return the detail / 상세 숫자
     */
    public int getDetail() {
        return detail;
    }

    /**
     * Sets the detail number.
     * <p>
     * 상세 숫자를 지정한다.
     *
     * @param value the detail / 상세 숫자
     */
    public void setDetail(int value) {
        this.detail = value;
    }

    /**
     * Sets the state of the modifier keys.
     * <p>
     * 보조 키의 상태를 지정한다.
     *
     * @param ctrl  Control / Control
     * @param shift Shift / Shift
     * @param alt   Alt / Alt
     * @param meta  Meta / Meta
     */
    public void setModifiers(boolean ctrl, boolean shift, boolean alt, boolean meta) {
        this.ctrlKey = ctrl;
        this.shiftKey = shift;
        this.altKey = alt;
        this.metaKey = meta;
    }

    /**
     * Tells whether Control was down.
     * <p>
     * Control 이 눌려 있었는지 알려 준다.
     *
     * @return the state / 상태
     */
    public boolean isCtrlKey() {
        return ctrlKey;
    }

    /**
     * Tells whether Shift was down.
     * <p>
     * Shift 가 눌려 있었는지 알려 준다.
     *
     * @return the state / 상태
     */
    public boolean isShiftKey() {
        return shiftKey;
    }

    /**
     * Tells whether Alt was down.
     * <p>
     * Alt 가 눌려 있었는지 알려 준다.
     *
     * @return the state / 상태
     */
    public boolean isAltKey() {
        return altKey;
    }

    /**
     * Tells whether Meta was down.
     * <p>
     * Meta 가 눌려 있었는지 알려 준다.
     *
     * @return the state / 상태
     */
    public boolean isMetaKey() {
        return metaKey;
    }

    /**
     * Returns the state of a modifier by its key name (<code>getModifierState</code>): <code>Control</code>, <code>Shift</code>,
     * <code>Alt</code>, <code>Meta</code>; other names are false.
     * <p>
     * 보조 키의 상태를 키 이름으로 반환한다(<code>getModifierState</code>): <code>Control</code>, <code>Shift</code>, <code>Alt</code>,
     * <code>Meta</code>. 다른 이름은 false 이다.
     *
     * @param key the key name / 키 이름
     * @return the state / 상태
     */
    public boolean getModifierState(String key) {
        if ("Control".equals(key)) {
            return ctrlKey;
        }
        if ("Shift".equals(key)) {
            return shiftKey;
        }
        if ("Alt".equals(key)) {
            return altKey;
        }
        return "Meta".equals(key) && metaKey;
    }
}
