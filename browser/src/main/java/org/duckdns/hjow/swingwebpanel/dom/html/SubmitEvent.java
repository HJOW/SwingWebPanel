package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * The <code>submit</code> event (HTML Standard 4.10.22.10 <code>SubmitEvent</code>): fired at a form before it is submitted, cancelable and
 * bubbling. {@link #getSubmitter()} is the button that started the submission, or null if the submission did not come from a button.
 * <p>
 * <code>submit</code> 이벤트(HTML 표준 4.10.22.10 <code>SubmitEvent</code>): 폼을 제출하기 전에 폼에 발생하며 취소 가능하고 버블링한다.
 * {@link #getSubmitter()} 는 제출을 시작한 버튼이며, 버튼이 아닌 곳에서 시작되었으면 null 이다.
 */
public final class SubmitEvent extends Event {

    /** The submitter, or null. / submitter. 없으면 null. */
    private final Element submitter;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type       the event type, normally <code>submit</code> / 이벤트 종류. 보통 <code>submit</code>
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     * @param submitter  the submitter or null / submitter 또는 null
     */
    public SubmitEvent(String type, boolean bubbles, boolean cancelable, Element submitter) {
        super(type, bubbles, cancelable);
        this.submitter = submitter;
    }

    /**
     * Returns the submitter.
     * <p>
     * submitter 를 반환한다.
     *
     * @return the button or null / 버튼 또는 null
     */
    public Element getSubmitter() {
        return submitter;
    }
}
