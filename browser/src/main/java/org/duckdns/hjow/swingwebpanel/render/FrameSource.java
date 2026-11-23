package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Supplies the pictures of nested browsing contexts (<code>iframe</code> and <code>frame</code> elements) to the renderer. The owner of the
 * frames lays the document of a frame out for the size of the box that the parent page gave the frame element and returns the immutable
 * result, which the renderer paints with a clip inside that box. The renderer calls it on the document thread while it builds a display list,
 * so it may do the layout of the frame at once, but it must not wait for the network. An {@link ImageSource} that also implements this
 * interface is used for the frames of the page (like {@link CanvasSource} for canvases).
 * <p>
 * 렌더러에 중첩된 브라우징 문맥(<code>iframe</code>, <code>frame</code> 요소)의 그림을 제공한다. 프레임의 소유자가 프레임 문서를 부모 페이지가 프레임
 * 요소에 준 상자 크기에 맞춰 배치하고 불변 결과를 돌려주면, 렌더러가 그 상자 안에서 클립하여 그린다. 렌더러가 디스플레이 리스트를 만드는 동안 문서
 * 스레드에서 호출하므로 프레임의 배치를 곧바로 할 수 있지만 네트워크를 기다리면 안 된다. 이 인터페이스도 구현하는 {@link ImageSource} 가 페이지의
 * 프레임에 쓰인다({@link CanvasSource} 가 캔버스에 쓰이듯이).
 */
public interface FrameSource {

    /**
     * Returns the current picture of the frame of an element.
     * <p>
     * 요소의 프레임의 현재 그림을 반환한다.
     *
     * @param frameElement the <code>iframe</code> or <code>frame</code> element / <code>iframe</code> 또는 <code>frame</code> 요소
     * @param width        the width of the content box in CSS pixels / 콘텐츠 상자의 너비(CSS 픽셀)
     * @param height       the height of the content box in CSS pixels / 콘텐츠 상자의 높이(CSS 픽셀)
     * @return the picture, or null if the element has no browsing context / 그림. 요소에 브라우징 문맥이 없으면 null
     */
    FramePicture getFrame(Element frameElement, double width, double height);
}
