package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The clipboard events <code>copy</code>, <code>cut</code> and <code>paste</code> (<code>ClipboardEvent</code>, Clipboard API and events): a
 * cancelable, bubbling event with the {@link ClipboardData} of the operation. The component fires <code>copy</code> at the element that has
 * the focus (the body if none has it) when the user copies the selection; a listener that calls <code>preventDefault</code> replaces what
 * the user selected with the data it put into {@link #getClipboardData()}.
 * <p>
 * 클립보드 이벤트 <code>copy</code>, <code>cut</code>, <code>paste</code>(<code>ClipboardEvent</code>, Clipboard API and events): 취소할 수 있고 버블하는
 * 이벤트이며 작업의 {@link ClipboardData} 를 가진다. 컴포넌트는 사용자가 선택을 복사할 때 포커스를 가진 요소(없으면 body)에 <code>copy</code> 를 보내며,
 * <code>preventDefault</code> 를 부른 리스너는 사용자가 선택한 것 대신 {@link #getClipboardData()} 에 넣은 데이터를 쓴다.
 */
public class ClipboardEvent extends Event {

    /**
     * The data of the operation.
     * <p>
     * 작업의 데이터.
     */
    private final ClipboardData clipboardData;

    /**
     * Creates a clipboard event.
     * <p>
     * 클립보드 이벤트를 만든다.
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     * @param data       the data of the operation, may be null for an event without data / 작업의 데이터. 데이터 없는 이벤트이면 null 가능
     */
    public ClipboardEvent(String type, boolean bubbles, boolean cancelable, ClipboardData data) {
        super(type, bubbles, cancelable);
        this.clipboardData = data;
    }

    /**
     * Returns the data of the operation (<code>clipboardData</code>).
     * <p>
     * 작업의 데이터를 반환한다(<code>clipboardData</code>).
     *
     * @return the data, or null / 데이터. 없으면 null
     */
    public ClipboardData getClipboardData() {
        return clipboardData;
    }
}
