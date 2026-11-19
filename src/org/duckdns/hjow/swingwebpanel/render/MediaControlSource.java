package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Supplies the state of the control bars of <code>audio</code> elements to the renderer (plan item P18-08). The script host owns the
 * media elements; the renderer asks for an immutable snapshot of one element while it builds a display list and paints that. The renderer
 * calls it on the document thread and the method must not block. An {@link ImageSource} that also implements this interface is used for
 * the media elements of the page; without one the renderer paints a bar that shows 0:00 and is paused.
 * <p>
 * 렌더러에 <code>audio</code> 요소의 컨트롤 막대 상태를 제공한다(계획 P18-08). 스크립트 호스트가 미디어 요소를 소유하고, 렌더러는 디스플레이 리스트를
 * 만드는 동안 요소 하나의 불변 스냅샷을 요청하여 그것을 그린다. 렌더러는 문서 스레드에서 이 메서드를 부르며 메서드는 막으면 안 된다. 이 인터페이스도
 * 구현하는 {@link ImageSource} 가 페이지의 미디어 요소에 쓰인다. 없으면 렌더러는 0:00 을 보여 주는 일시정지 상태의 막대를 그린다.
 */
public interface MediaControlSource {

    /**
     * Returns the state that the control bar of an element shows now.
     * <p>
     * 요소의 컨트롤 막대가 지금 보여 주는 상태를 반환한다.
     *
     * @param media the <code>audio</code> element / <code>audio</code> 요소
     * @return the snapshot, or null if the element is unknown to the host / 스냅샷. 호스트가 모르는 요소이면 null
     */
    MediaControlState getMediaControls(Element media);
}
