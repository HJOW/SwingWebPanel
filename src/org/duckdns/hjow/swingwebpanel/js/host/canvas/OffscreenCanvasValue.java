package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.lang.ref.WeakReference;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;

/**
 * An <code>OffscreenCanvas</code> value (HTML Standard 4.12.5.3): a bitmap and its 2D context without a place in the document. It is an
 * <code>EventTarget</code> (<code>contextlost</code> and <code>contextrestored</code> listeners can be added; the canvas never loses its context
 * here). The pixels live in a {@link CanvasSurface}, the same engine that a <code>canvas</code> element uses, so an offscreen canvas and the context
 * of a canvas element draw alike. An offscreen canvas that came from <code>HTMLCanvasElement.transferControlToOffscreen()</code> shares the
 * surface of its placeholder element, so whatever is drawn is shown by the element; moving the object to a worker (a transfer) detaches it: it
 * gives its surface to the receiver, its size is 0 and it cannot be used any more. All members are meant for the thread that owns the script
 * realm of the object.
 * <p>
 * <code>OffscreenCanvas</code> 값(HTML 표준 4.12.5.3): 문서에 자리가 없는 비트맵과 그 2D 컨텍스트. <code>EventTarget</code> 이다(<code>contextlost</code>,
 * <code>contextrestored</code> 리스너를 더할 수 있으나 여기서 캔버스는 컨텍스트를 잃지 않는다). 화소는 <code>canvas</code> 요소가 쓰는 것과 같은 엔진인
 * {@link CanvasSurface} 에 있으므로 offscreen 캔버스와 canvas 요소의 컨텍스트는 똑같이 그린다. <code>HTMLCanvasElement.transferControlToOffscreen()</code> 이
 * 만든 offscreen 캔버스는 자리표시 요소의 표면을 공유하므로 그려진 것은 요소가 보여 준다. 객체를 worker 로 옮기면(전달) 떼어 내진다: 표면을 받는 쪽에 내주고
 * 크기가 0 이 되며 더 쓸 수 없다. 모든 멤버는 객체의 스크립트 realm 을 소유한 스레드에서 쓰도록 만들었다.
 */
public final class OffscreenCanvasValue extends EventTarget {

    /**
     * The pixels, or null once the object was detached.
     * <p>
     * 화소. 떼어 낸 뒤에는 null.
     */
    private CanvasSurface surface;

    /**
     * The placeholder canvas element (weakly), or null for an offscreen canvas that has none.
     * <p>
     * 자리표시 canvas 요소(약한 참조). 없으면 null.
     */
    private final WeakReference<Element> placeholder;

    /**
     * Creates an offscreen canvas over a surface; the surface learns that this object is its canvas.
     * <p>
     * 표면 위의 offscreen 캔버스를 만든다. 표면은 이 객체가 자기 캔버스임을 안다.
     *
     * @param surface     the surface / 표면
     * @param placeholder the placeholder element or null / 자리표시 요소 또는 null
     */
    OffscreenCanvasValue(CanvasSurface surface, Element placeholder) {
        this.surface = surface;
        this.placeholder = placeholder == null ? null : new WeakReference<Element>(placeholder);
        surface.setStrongOwner(this);
    }

    /**
     * Returns the surface, or null after the object was detached.
     * <p>
     * 표면을 반환한다. 떼어 낸 뒤에는 null.
     *
     * @return the surface or null / 표면 또는 null
     */
    synchronized CanvasSurface surface() {
        return surface;
    }

    /**
     * Tells whether the object was detached by a transfer.
     * <p>
     * 전달로 떼어 낸 객체인지 알려 준다.
     *
     * @return true when detached / 떼어 냈으면 true
     */
    public synchronized boolean isDetached() {
        return surface == null;
    }

    /**
     * Returns the placeholder canvas element, or null if there is none or it was collected.
     * <p>
     * 자리표시 canvas 요소를 반환한다. 없거나 수거되었으면 null.
     *
     * @return the element or null / 요소 또는 null
     */
    Element placeholder() {
        return placeholder == null ? null : placeholder.get();
    }

    /**
     * Tells whether the object has a placeholder element.
     * <p>
     * 자리표시 요소가 있는지 알려 준다.
     *
     * @return true if it has one / 있으면 true
     */
    boolean hasPlaceholder() {
        return placeholder != null;
    }

    /**
     * Returns the width of the bitmap in pixels, 0 after the object was detached.
     * <p>
     * 비트맵의 너비(화소)를 반환한다. 떼어 낸 뒤에는 0.
     *
     * @return the width / 너비
     */
    public synchronized int getWidth() {
        return surface == null ? 0 : surface.getWidth();
    }

    /**
     * Returns the height of the bitmap in pixels, 0 after the object was detached.
     * <p>
     * 비트맵의 높이(화소)를 반환한다. 떼어 낸 뒤에는 0.
     *
     * @return the height / 높이
     */
    public synchronized int getHeight() {
        return surface == null ? 0 : surface.getHeight();
    }

    /**
     * Detaches the object (a transfer): the surface goes to the caller and the object is empty from now on.
     * <p>
     * 객체를 떼어 낸다(전달): 표면은 호출자에게 가고 객체는 이제부터 비어 있다.
     *
     * @return the surface that the object had, or null if it was detached already / 객체가 가졌던 표면. 이미 떼어 냈으면 null
     */
    synchronized CanvasSurface detach() {
        CanvasSurface s = surface;
        surface = null;
        return s;
    }
}
