package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The events of drag and drop (<code>DragEvent</code>, HTML Standard 6.11): <code>dragstart</code>, <code>drag</code>, <code>dragenter</code>,
 * <code>dragover</code>, <code>dragleave</code>, <code>drop</code> and <code>dragend</code>. It is a mouse event (the position of the pointer, the
 * buttons and keys) with the {@link ClipboardData} of the drag operation as its <code>dataTransfer</code>. The component creates one data
 * transfer per event on top of the drag data store of the operation, and ends its association with the operation when the event is
 * over (see {@link ClipboardData#disable()}).
 * <p>
 * 끌어다 놓기의 이벤트(<code>DragEvent</code>, HTML 표준 6.11): <code>dragstart</code>, <code>drag</code>, <code>dragenter</code>, <code>dragover</code>, <code>dragleave</code>,
 * <code>drop</code>, <code>dragend</code>. 마우스 이벤트(포인터 위치, 버튼, 키)이며 끌기 작업의 {@link ClipboardData} 를 <code>dataTransfer</code> 로 가진다. 컴포넌트는
 * 작업의 드래그 데이터 저장소 위에 이벤트마다 데이터 전달 객체를 하나 만들고, 이벤트가 끝나면 작업과의 연결을 끊는다({@link ClipboardData#disable()} 참조).
 */
public class DragEvent extends MouseEvent {

    /**
     * The data of the drag operation (<code>dataTransfer</code>), or null.
     * <p>
     * 끌기 작업의 데이터(<code>dataTransfer</code>). 없으면 null.
     */
    private ClipboardData dataTransfer;

    /**
     * Creates a drag event.
     * <p>
     * 끌기 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public DragEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets the data of the drag operation.
     * <p>
     * 끌기 작업의 데이터를 정한다.
     *
     * @param transfer the data or null / 데이터. 없으면 null
     */
    public void setDataTransfer(ClipboardData transfer) {
        this.dataTransfer = transfer;
    }

    /**
     * Returns the data of the drag operation (<code>dataTransfer</code>).
     * <p>
     * 끌기 작업의 데이터를 반환한다(<code>dataTransfer</code>).
     *
     * @return the data or null / 데이터. 없으면 null
     */
    public ClipboardData getDataTransfer() {
        return dataTransfer;
    }
}
