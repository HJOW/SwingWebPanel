package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.PointerEvent;
import org.duckdns.hjow.swingwebpanel.js.host.PointerService;

/**
 * The state of the mouse pointer that the pages of a component share, and the pointer capture service that scripts use (W3C Pointer Events
 * Level 3): whether the pointer is active, which buttons are down, the document that the pointer is active in, and the two capture target
 * slots of the specification: the <em>pending</em> capture (what <code>setPointerCapture</code> and <code>releasePointerCapture</code> change at once; what
 * <code>hasPointerCapture</code> reports) and the capture target <em>override</em> (what the events really go to). The component promotes the pending
 * capture to the override when it fires the next pointer event and announces the change with <code>lostpointercapture</code> and
 * <code>gotpointercapture</code> ("process pending pointer capture"). Everything except {@link #setPresent(boolean)} belongs to the document queue,
 * the thread that delivers the input events and runs the scripts. The component has one mouse, so there is exactly one pointer, with
 * the id {@link PointerEvent#MOUSE_POINTER_ID}.
 * <p>
 * 컴포넌트의 페이지들이 공유하는 마우스 포인터의 상태와 스크립트가 쓰는 포인터 캡처 서비스(W3C Pointer Events Level 3): 포인터가 활성인지, 어느 버튼이
 * 눌려 있는지, 포인터가 활성인 문서, 그리고 규격의 두 캡처 대상 칸. <em>대기 중</em> 캡처(<code>setPointerCapture</code> 와
 * <code>releasePointerCapture</code> 가 곧바로 바꾸고 <code>hasPointerCapture</code> 가 보고하는 것)와 캡처 대상 <em>재정의</em>(이벤트가 실제로 가는 곳).
 * 컴포넌트는 다음 포인터 이벤트를 발생시킬 때 대기 중 캡처를 재정의로 올리고 <code>lostpointercapture</code> 와 <code>gotpointercapture</code> 로 변화를 알린다("process
 * pending pointer capture"). {@link #setPresent(boolean)} 를 뺀 모든 것은 입력 이벤트를 전달하고 스크립트를 실행하는 스레드인 문서 큐의 것이다. 컴포넌트에는 마우스가
 * 하나이므로 포인터는 정확히 하나이며 ID 는 {@link PointerEvent#MOUSE_POINTER_ID} 이다.
 */
final class PagePointers implements PointerService {

    /**
     * True while the mouse is over the component (it is read on the document queue and written on the event dispatch thread).
     * <p>
     * 마우스가 컴포넌트 위에 있는 동안 true(문서 큐에서 읽고 이벤트 디스패치 스레드에서 쓴다).
     */
    private volatile boolean present;

    /**
     * The buttons that are down as the DOM bit mask (1 main, 2 secondary, 4 auxiliary).
     * <p>
     * 눌려 있는 버튼의 DOM 비트 마스크(1 주, 2 보조, 4 가운데).
     */
    private int buttons;

    /**
     * The pending capture target override, or null.
     * <p>
     * 대기 중인 캡처 대상 재정의. 없으면 null.
     */
    private Element pending;

    /**
     * The capture target override: the element that the pointer events go to, or null.
     * <p>
     * 캡처 대상 재정의: 포인터 이벤트가 가는 요소. 없으면 null.
     */
    private Element override;

    /**
     * The document that the pointer is active in: the document of the target of the last pointer event.
     * <p>
     * 포인터가 활성인 문서: 마지막 포인터 이벤트의 대상이 속한 문서.
     */
    private Document active;

    /**
     * Tells that the mouse is over the component or left it (call it from the event dispatch thread).
     * <p>
     * 마우스가 컴포넌트 위에 있는지 떠났는지 알린다(이벤트 디스패치 스레드에서 호출한다).
     *
     * @param value true while the mouse is over the component / 마우스가 컴포넌트 위에 있는 동안 true
     */
    void setPresent(boolean value) {
        present = value;
    }

    /**
     * Records the buttons that are down.
     * <p>
     * 눌려 있는 버튼을 기록한다.
     *
     * @param mask the DOM bit mask / DOM 비트 마스크
     */
    void setButtons(int mask) {
        buttons = mask;
    }

    /**
     * Returns the buttons that are down as the DOM bit mask.
     * <p>
     * 눌려 있는 버튼을 DOM 비트 마스크로 반환한다.
     *
     * @return the mask / 마스크
     */
    int getButtons() {
        return buttons;
    }

    /**
     * Records the document that the pointer is active in.
     * <p>
     * 포인터가 활성인 문서를 기록한다.
     *
     * @param document the document or null / 문서. 없으면 null
     */
    void setActiveDocument(Document document) {
        active = document;
    }

    /**
     * Returns the capture target override, or null.
     * <p>
     * 캡처 대상 재정의를 반환한다. 없으면 null.
     *
     * @return the element or null / 요소 또는 null
     */
    Element getOverride() {
        return override;
    }

    /**
     * Returns the pending capture target override, or null.
     * <p>
     * 대기 중인 캡처 대상 재정의를 반환한다. 없으면 null.
     *
     * @return the element or null / 요소 또는 null
     */
    Element getPending() {
        return pending;
    }

    /**
     * Sets the capture target override ("process pending pointer capture" does it as it fires <code>lostpointercapture</code> and
     * <code>gotpointercapture</code>).
     * <p>
     * 캡처 대상 재정의를 정한다("process pending pointer capture" 가 <code>lostpointercapture</code> 와 <code>gotpointercapture</code> 를 발생시키며 정한다).
     *
     * @param element the new override or null / 새 재정의. 없으면 null
     */
    void promote(Element element) {
        override = element;
    }

    /**
     * Clears the pending capture (the implicit release of the capture after <code>pointerup</code> and <code>pointercancel</code>, or the loss of
     * a capture target that left the document).
     * <p>
     * 대기 중인 캡처를 지운다(<code>pointerup</code> 과 <code>pointercancel</code> 뒤의 캡처 암묵 해제, 또는 문서를 떠난 캡처 대상의 상실).
     */
    void clearPending() {
        pending = null;
    }

    /**
     * Forgets everything about the pointer (the page was replaced or the component was disposed): no capture, no buttons, no active document.
     * <p>
     * 포인터에 대한 모든 것을 잊는다(페이지가 교체되었거나 컴포넌트가 폐기됨): 캡처, 버튼, 활성 문서가 없다.
     */
    void reset() {
        pending = null;
        override = null;
        buttons = 0;
        active = null;
    }

    /**
     * Tells whether a pointer id is the active mouse pointer.
     * <p>
     * 포인터 ID 가 활성인 마우스 포인터인지 알려 준다.
     *
     * @param pointerId the id / ID
     * @return true for the mouse while it is over the component or a button is down / 마우스가 컴포넌트 위에 있거나 버튼이 눌려 있으면 true
     */
    public boolean isActive(int pointerId) {
        return pointerId == PointerEvent.MOUSE_POINTER_ID && (present || buttons != 0);
    }

    /**
     * Sets the pending capture: silently nothing unless a button is down and the element belongs to the document that the pointer is active in.
     * <p>
     * 대기 중인 캡처를 정한다. 버튼이 눌려 있고 요소가 포인터가 활성인 문서에 속하지 않으면 조용히 아무 일도 없다.
     *
     * @param pointerId the id / ID
     * @param element   the element / 요소
     */
    public void setCapture(int pointerId, Element element) {
        if (pointerId != PointerEvent.MOUSE_POINTER_ID || buttons == 0 || active == null || element.getOwnerDocument() != active) {
            return;
        }
        pending = element;
    }

    /**
     * Tells whether the element has the pending capture.
     * <p>
     * 요소가 대기 중인 캡처를 가졌는지 알려 준다.
     *
     * @param pointerId the id / ID
     * @param element   the element / 요소
     * @return true if it has it / 가졌으면 true
     */
    public boolean hasCapture(int pointerId, Element element) {
        return pointerId == PointerEvent.MOUSE_POINTER_ID && pending != null && pending == element;
    }

    /**
     * Clears the pending capture if the element has it.
     * <p>
     * 요소가 가졌으면 대기 중인 캡처를 지운다.
     *
     * @param pointerId the id / ID
     * @param element   the element / 요소
     */
    public void releaseCapture(int pointerId, Element element) {
        if (hasCapture(pointerId, element)) {
            pending = null;
        }
    }
}
