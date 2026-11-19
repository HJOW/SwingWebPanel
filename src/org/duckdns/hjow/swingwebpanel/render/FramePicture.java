package org.duckdns.hjow.swingwebpanel.render;

/**
 * The picture of a nested browsing context (<code>iframe</code>, <code>frame</code>) that the renderer paints inside the content box of the
 * frame element: the immutable, fully rendered page of the frame (laid out for the size of the box), the scroll position of the frame and
 * which overlay scrollbars to draw. Instances are immutable; the frame owner makes a new one whenever anything changes.
 * <p>
 * 렌더러가 프레임 요소의 콘텐츠 상자 안에 그리는 중첩된 브라우징 문맥(<code>iframe</code>, <code>frame</code>)의 그림: 프레임의 불변이며 완전히 렌더링된
 * 페이지(상자 크기에 맞춰 배치됨), 프레임의 스크롤 위치, 그릴 오버레이 스크롤바. 인스턴스는 불변이며 무엇이든 바뀌면 프레임 소유자가 새로 만든다.
 */
public final class FramePicture {

    /**
     * The rendered page of the frame, or null if nothing is shown (the frame has no document yet).
     * <p>
     * 프레임의 렌더링된 페이지. 보일 것이 없으면(프레임에 아직 문서가 없음) null.
     */
    private final RenderedPage page;

    /**
     * The horizontal scroll offset of the frame in CSS pixels.
     * <p>
     * 프레임의 가로 스크롤 오프셋(CSS 픽셀).
     */
    private final int scrollX;

    /**
     * The vertical scroll offset of the frame in CSS pixels.
     * <p>
     * 프레임의 세로 스크롤 오프셋(CSS 픽셀).
     */
    private final int scrollY;

    /**
     * True if the horizontal overlay scrollbar is drawn.
     * <p>
     * 가로 오버레이 스크롤바를 그리면 true.
     */
    private final boolean horizontalBar;

    /**
     * True if the vertical overlay scrollbar is drawn.
     * <p>
     * 세로 오버레이 스크롤바를 그리면 true.
     */
    private final boolean verticalBar;

    /**
     * Creates a picture.
     * <p>
     * 그림을 만든다.
     *
     * @param page          the rendered page of the frame or null / 프레임의 렌더링된 페이지 또는 null
     * @param scrollX       the horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY       the vertical scroll offset / 세로 스크롤 오프셋
     * @param horizontalBar true to draw the horizontal scrollbar / 가로 스크롤바를 그리려면 true
     * @param verticalBar   true to draw the vertical scrollbar / 세로 스크롤바를 그리려면 true
     */
    public FramePicture(RenderedPage page, int scrollX, int scrollY, boolean horizontalBar, boolean verticalBar) {
        this.page = page;
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.horizontalBar = horizontalBar;
        this.verticalBar = verticalBar;
    }

    /**
     * Returns the rendered page of the frame.
     * <p>
     * 프레임의 렌더링된 페이지를 반환한다.
     *
     * @return the page or null / 페이지 또는 null
     */
    public RenderedPage getPage() {
        return page;
    }

    /**
     * Returns the horizontal scroll offset.
     * <p>
     * 가로 스크롤 오프셋을 반환한다.
     *
     * @return the offset in CSS pixels / 오프셋(CSS 픽셀)
     */
    public int getScrollX() {
        return scrollX;
    }

    /**
     * Returns the vertical scroll offset.
     * <p>
     * 세로 스크롤 오프셋을 반환한다.
     *
     * @return the offset in CSS pixels / 오프셋(CSS 픽셀)
     */
    public int getScrollY() {
        return scrollY;
    }

    /**
     * Tells whether the horizontal scrollbar is drawn.
     * <p>
     * 가로 스크롤바를 그리는지 알려 준다.
     *
     * @return true if drawn / 그리면 true
     */
    public boolean hasHorizontalBar() {
        return horizontalBar;
    }

    /**
     * Tells whether the vertical scrollbar is drawn.
     * <p>
     * 세로 스크롤바를 그리는지 알려 준다.
     *
     * @return true if drawn / 그리면 true
     */
    public boolean hasVerticalBar() {
        return verticalBar;
    }
}
