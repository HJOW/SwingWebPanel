package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * Supplies the pictures of <code>canvas</code> elements to the renderer. The script engine owns the bitmaps and draws on them; the renderer
 * asks for an immutable snapshot of one canvas while it builds a display list and paints that. A snapshot is a copy, so the script can keep
 * drawing while the renderer paints. The renderer calls it on the document thread and the method must not block. An
 * {@link ImageSource} that also implements this interface is used for the canvases of the page.
 * <p>
 * 렌더러에 <code>canvas</code> 요소의 그림을 제공한다. 스크립트 엔진이 비트맵을 소유하고 그 위에 그리며, 렌더러는 디스플레이 리스트를 만드는 동안 캔버스 하나의
 * 불변 스냅샷을 요청하여 그것을 그린다. 스냅샷은 복사본이므로 렌더러가 그리는 동안에도 스크립트는 계속 그릴 수 있다. 렌더러는 문서 스레드에서 이 메서드를
 * 부르며 메서드는 막으면 안 된다. 이 인터페이스도 구현하는 {@link ImageSource} 가 페이지의 캔버스에 쓰인다.
 */
public interface CanvasSource {

    /**
     * Returns the current picture of a canvas.
     * <p>
     * 캔버스의 현재 그림을 반환한다.
     *
     * @param canvas the <code>canvas</code> element / <code>canvas</code> 요소
     * @return an immutable snapshot (the same object while the canvas does not change), or null if nothing was drawn or the canvas has no bitmap / 불변 스냅샷(캔버스가 바뀌지 않는 동안 같은 객체). 그린 것이 없거나 비트맵이 없으면 null
     */
    DecodedImage getCanvasImage(Element canvas);
}
