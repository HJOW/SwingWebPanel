package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A composition event (UI Events): <code>compositionstart</code>, <code>compositionupdate</code> and <code>compositionend</code> of an input
 * method (for example while Hangul letters are being assembled), with the text of the composition.
 * <p>
 * 조합 이벤트(UI Events): 입력기의 <code>compositionstart</code>, <code>compositionupdate</code>, <code>compositionend</code>(예: 한글을 조립하는
 * 동안)이며 조합 중인 텍스트를 가진다.
 */
public class CompositionEvent extends UiEvent {

    /**
     * The text of the composition.
     * <p>
     * 조합 중인 텍스트.
     */
    private String data = "";

    /**
     * Creates a composition event.
     * <p>
     * 조합 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public CompositionEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets the text of the composition.
     * <p>
     * 조합 중인 텍스트를 지정한다.
     *
     * @param text the text / 텍스트
     */
    public void setData(String text) {
        this.data = text == null ? "" : text;
    }

    /**
     * Returns the text of the composition.
     * <p>
     * 조합 중인 텍스트를 반환한다.
     *
     * @return the text / 텍스트
     */
    public String getData() {
        return data;
    }
}
