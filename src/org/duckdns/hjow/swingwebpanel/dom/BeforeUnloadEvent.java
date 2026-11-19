package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>beforeunload</code> event (HTML Standard "BeforeUnloadEvent"): it is fired at the window of a page that is about to be left, it can be cancelled, and a
 * string in <code>returnValue</code> also asks to confirm the leaving. This component has no dialog for it; the page is left in any case and the status text tells
 * that the page asked (see <code>docs/progress/webapi2.md</code>).
 * <p>
 * <code>beforeunload</code> 이벤트(HTML Standard "BeforeUnloadEvent"): 떠나려는 페이지의 window 에서 발생하고 취소할 수 있으며, <code>returnValue</code> 의 문자열도
 * 떠나기 전 확인을 요청한다. 이 컴포넌트에는 그 대화상자가 없다. 어느 경우에도 페이지를 떠나며 상태 메시지로 페이지가 요청했음을 알린다
 * (<code>docs/progress/webapi2.md</code> 참조).
 */
public class BeforeUnloadEvent extends Event {

    /**
     * The text that the page gave to be shown in the confirmation (the empty string if it gave none).
     * <p>
     * 확인에 보일 문구로 페이지가 준 텍스트(주지 않았으면 빈 문자열).
     */
    private String returnValue = "";

    /**
     * Creates a <code>beforeunload</code> event: it does not bubble and can be cancelled.
     * <p>
     * <code>beforeunload</code> 이벤트를 만든다: 버블링하지 않고 취소할 수 있다.
     */
    public BeforeUnloadEvent() {
        super("beforeunload", false, true);
    }

    /**
     * Returns <code>returnValue</code>.
     * <p>
     * <code>returnValue</code> 를 반환한다.
     *
     * @return the text, never null / 텍스트. null 이 아님
     */
    public String getReturnValue() {
        return returnValue;
    }

    /**
     * Sets <code>returnValue</code>.
     * <p>
     * <code>returnValue</code> 를 정한다.
     *
     * @param value the text; null counts as the empty string / 텍스트. null 은 빈 문자열
     */
    public void setReturnValue(String value) {
        this.returnValue = value == null ? "" : value;
    }

    /**
     * Tells whether the page asked to confirm the leaving: the event was cancelled or <code>returnValue</code> is not empty.
     * <p>
     * 페이지가 떠나기 전 확인을 요청했는지 알려 준다: 이벤트가 취소되었거나 <code>returnValue</code> 가 비어 있지 않다.
     *
     * @return true if the page asked / 페이지가 요청했으면 true
     */
    public boolean isConfirmationRequested() {
        return isDefaultPrevented() || returnValue.length() > 0;
    }
}
