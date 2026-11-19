package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The pointer capture service of a page (W3C Pointer Events, "Pointer capture"): the component keeps the state of the pointer, a script asks
 * for capture through the {@link Element} methods <code>setPointerCapture</code>, <code>releasePointerCapture</code> and
 * <code>hasPointerCapture</code>, which the page layer checks against this service. All methods are called on the document queue (the thread that
 * runs the scripts of the page and delivers its input events), so the state needs no locking.
 * <p>
 * 페이지의 포인터 캡처 서비스(W3C Pointer Events, "Pointer capture"). 컴포넌트가 포인터의 상태를 보관하고, 스크립트는 요소의
 * <code>setPointerCapture</code>, <code>releasePointerCapture</code>, <code>hasPointerCapture</code> 메서드로 캡처를 요청하며, 페이지 계층이 이 서비스에 대어
 * 확인한다. 모든 메서드는 문서 큐(페이지의 스크립트를 실행하고 입력 이벤트를 전달하는 스레드)에서 호출하므로 상태에 잠금이 필요 없다.
 */
public interface PointerService {

    /**
     * Tells whether a pointer id names an active pointer (the mouse of the component has the id 1 while it is over the component or a button
     * is down).
     * <p>
     * 포인터 ID 가 활성 포인터를 가리키는지 알려 준다(컴포넌트의 마우스는 컴포넌트 위에 있거나 버튼이 눌려 있는 동안 ID 1 이다).
     *
     * @param pointerId the id / ID
     * @return true if the pointer is active / 활성 포인터이면 true
     */
    boolean isActive(int pointerId);

    /**
     * Sets the pending pointer capture of a pointer to an element. It has no effect (and no error) when the pointer has no button down or the
     * element is not in the document that the pointer is active in; the capture starts with the next pointer event (<code>gotpointercapture</code>).
     * <p>
     * 포인터의 대기 중인 포인터 캡처를 요소로 정한다. 포인터에 눌린 버튼이 없거나 요소가 포인터가 활성인 문서에 있지 않으면 효과도 오류도 없다. 캡처는 다음
     * 포인터 이벤트와 함께 시작한다(<code>gotpointercapture</code>).
     *
     * @param pointerId the id / ID
     * @param element   the element that wants the capture / 캡처를 원하는 요소
     */
    void setCapture(int pointerId, Element element);

    /**
     * Tells whether an element has the pending pointer capture of a pointer (true right after {@link #setCapture}, before the event that
     * announces it).
     * <p>
     * 요소가 포인터의 대기 중인 포인터 캡처를 가졌는지 알려 준다({@link #setCapture} 직후, 그것을 알리는 이벤트 전에도 true).
     *
     * @param pointerId the id / ID
     * @param element   the element / 요소
     * @return true if the element has it / 가졌으면 true
     */
    boolean hasCapture(int pointerId, Element element);

    /**
     * Clears the pending pointer capture of a pointer if the element has it; nothing happens otherwise. The capture ends with the next pointer
     * event (<code>lostpointercapture</code>).
     * <p>
     * 요소가 포인터의 대기 중인 포인터 캡처를 가졌으면 지운다. 아니면 아무 일도 없다. 캡처는 다음 포인터 이벤트와 함께 끝난다(<code>lostpointercapture</code>).
     *
     * @param pointerId the id / ID
     * @param element   the element / 요소
     */
    void releaseCapture(int pointerId, Element element);
}
