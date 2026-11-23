package org.duckdns.hjow.swingwebpanel.core;

/**
 * Size information that the layout engine reports to the component so that it can behave as a <code>Scrollable</code> view:
 * the minimum width the laid-out content needs, the height of the content and the scroll step. The values are volatile, so
 * the layout thread may write them and the event dispatch thread may read them. Until a layout engine exists they stay at
 * their defaults (an empty page).
 * <p>
 * 컴포넌트가 <code>Scrollable</code> 뷰로 동작할 수 있도록 레이아웃 엔진이 알려 주는 크기 정보: 배치된 콘텐츠가 필요로 하는 최소 폭,
 * 콘텐츠 높이, 스크롤 단위. 값은 volatile 이라 레이아웃 스레드가 쓰고 이벤트 디스패치 스레드가 읽을 수 있다. 레이아웃 엔진이 생기기 전에는
 * 기본값(빈 페이지)을 유지한다.
 */
public final class ViewMetrics {

    /**
     * Default scroll step in pixels.
     * <p>
     * 기본 스크롤 단위(픽셀).
     */
    public static final int DEFAULT_UNIT_INCREMENT = 16;

    /**
     * Minimum content width; 0 means the content can shrink to any width (it wraps).
     * <p>
     * 콘텐츠의 최소 폭. 0 이면 어떤 폭으로도 줄일 수 있다(줄바꿈됨).
     */
    private volatile int minWidth;

    /**
     * Content height; 0 means unknown or empty.
     * <p>
     * 콘텐츠 높이. 0 이면 모르거나 비어 있다.
     */
    private volatile int height;

    /**
     * Scroll step in pixels.
     * <p>
     * 스크롤 단위(픽셀).
     */
    private volatile int unitIncrement = DEFAULT_UNIT_INCREMENT;

    /**
     * Creates metrics for an empty page.
     * <p>
     * 빈 페이지용 정보를 만든다.
     */
    public ViewMetrics() {
    }

    /**
     * Sets the content size reported by the layout engine.
     * <p>
     * 레이아웃 엔진이 알려 준 콘텐츠 크기를 지정한다.
     *
     * @param minimumWidth  minimum width (0 for none), not negative / 최소 폭(없으면 0), 음수 불가
     * @param contentHeight content height, not negative / 콘텐츠 높이, 음수 불가
     * @throws IllegalArgumentException for negative values / 음수일 때
     */
    public void setContentSize(int minimumWidth, int contentHeight) {
        if (minimumWidth < 0 || contentHeight < 0) {
            throw new IllegalArgumentException("sizes must not be negative");
        }
        this.minWidth = minimumWidth;
        this.height = contentHeight;
    }

    /**
     * Sets the scroll step (normally the line height of the default font).
     * <p>
     * 스크롤 단위를 지정한다(보통 기본 글꼴의 줄 높이).
     *
     * @param pixels step in pixels, at least 1 / 단위(픽셀), 1 이상
     * @throws IllegalArgumentException if smaller than 1 / 1 보다 작을 때
     */
    public void setUnitIncrement(int pixels) {
        if (pixels < 1) {
            throw new IllegalArgumentException("unit increment must be at least 1");
        }
        this.unitIncrement = pixels;
    }

    /**
     * Returns the minimum content width.
     * <p>
     * 콘텐츠의 최소 폭을 반환한다.
     *
     * @return minimum width / 최소 폭
     */
    public int getMinWidth() {
        return minWidth;
    }

    /**
     * Returns the content height.
     * <p>
     * 콘텐츠 높이를 반환한다.
     *
     * @return content height / 콘텐츠 높이
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the scroll step.
     * <p>
     * 스크롤 단위를 반환한다.
     *
     * @return step in pixels / 단위(픽셀)
     */
    public int getUnitIncrement() {
        return unitIncrement;
    }
}
