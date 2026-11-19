package org.duckdns.hjow.swingwebpanel.dom;

/**
 * An input event (UI Events and Input Events): a change of the value of an editable element, with the text that was inserted and the
 * kind of change (<code>insertText</code>, <code>deleteContentBackward</code> ...), also used for <code>beforeinput</code>.
 * <p>
 * 입력 이벤트(UI Events, Input Events): 편집 가능한 요소의 값이 바뀌었다는 이벤트이며 삽입한 텍스트와 변경 종류(<code>insertText</code>,
 * <code>deleteContentBackward</code> 등)를 가진다. <code>beforeinput</code> 에도 쓴다.
 */
public class InputEvent extends UiEvent {

    /**
     * The inserted text, or null.
     * <p>
     * 삽입한 텍스트. 없으면 null.
     */
    private String data;

    /**
     * The kind of change.
     * <p>
     * 변경 종류.
     */
    private String inputType = "";

    /**
     * True while an input method composes text.
     * <p>
     * 입력기가 텍스트를 조합하는 중이면 true.
     */
    private boolean composing;

    /**
     * The data transfer of a paste, a drop or a similar input (<code>dataTransfer</code>), or null.
     * <p>
     * 붙여넣기, 놓기 같은 입력의 데이터 전달 객체(<code>dataTransfer</code>). 없으면 null.
     */
    private ClipboardData dataTransfer;

    /**
     * The ranges that the event will change (<code>getTargetRanges()</code>), empty for an event without ranges.
     * <p>
     * 이벤트가 바꿀 범위(<code>getTargetRanges()</code>). 범위가 없는 이벤트는 빈 목록.
     */
    private java.util.List<StaticRange> targetRanges = new java.util.ArrayList<StaticRange>();

    /**
     * Creates an input event.
     * <p>
     * 입력 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public InputEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets what the event reports.
     * <p>
     * 이벤트가 알리는 내용을 지정한다.
     *
     * @param text      the inserted text or null / 삽입한 텍스트. 없으면 null
     * @param kind      the kind of change / 변경 종류
     * @param isComposing whether an input method composes / 입력기가 조합 중인지
     */
    public void setInput(String text, String kind, boolean isComposing) {
        this.data = text;
        this.inputType = kind == null ? "" : kind;
        this.composing = isComposing;
    }

    /**
     * Returns the inserted text.
     * <p>
     * 삽입한 텍스트를 반환한다.
     *
     * @return the text or null / 텍스트. 없으면 null
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the kind of change.
     * <p>
     * 변경 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public String getInputType() {
        return inputType;
    }

    /**
     * Tells whether an input method composes text.
     * <p>
     * 입력기가 텍스트를 조합 중인지 알려 준다.
     *
     * @return true if composing / 조합 중이면 true
     */
    public boolean isComposing() {
        return composing;
    }

    /**
     * Sets the data transfer of the event.
     * <p>
     * 이벤트의 데이터 전달 객체를 정한다.
     *
     * @param transfer the data or null / 데이터. 없으면 null
     */
    public void setDataTransfer(ClipboardData transfer) {
        this.dataTransfer = transfer;
    }

    /**
     * Returns the data transfer of the event (<code>dataTransfer</code>).
     * <p>
     * 이벤트의 데이터 전달 객체를 반환한다(<code>dataTransfer</code>).
     *
     * @return the data or null / 데이터. 없으면 null
     */
    public ClipboardData getDataTransfer() {
        return dataTransfer;
    }

    /**
     * Sets the target ranges of the event (copied).
     * <p>
     * 이벤트의 대상 범위를 정한다(복사한다).
     *
     * @param ranges the ranges, null for none / 범위. 없으면 null
     */
    public void setTargetRanges(java.util.List<StaticRange> ranges) {
        this.targetRanges = ranges == null ? new java.util.ArrayList<StaticRange>() : new java.util.ArrayList<StaticRange>(ranges);
    }

    /**
     * Returns a copy of the target ranges (<code>getTargetRanges()</code>).
     * <p>
     * 대상 범위의 복사본을 반환한다(<code>getTargetRanges()</code>).
     *
     * @return the ranges / 범위
     */
    public java.util.List<StaticRange> getTargetRanges() {
        return new java.util.ArrayList<StaticRange>(targetRanges);
    }
}
