package org.duckdns.hjow.swingwebpanel.layout;

/**
 * One segment of a collapsed table border (plan item P10-02): the winner of the conflict between the borders of the cells, rows, row
 * groups, columns, column groups and the table that meet at a grid line (CSS 2.1 17.6.2.1), as a rectangle to paint. The segment is
 * centered on the grid line, so half of its thickness lies in each of the two cells beside it. The geometry of a segment of
 * {@link LayoutBox#collapsedEdges} is relative to the border box of the table; the segments that {@link LayoutBox#getCollapsedBorders()}
 * returns are in document coordinates. A segment is immutable.
 * <p>
 * 합쳐진 표 border 의 선분 하나(계획 P10-02): 격자선에서 만나는 셀, 행, 행 그룹, 열, 열 그룹, 표의 border 충돌의 승자(CSS 2.1 17.6.2.1)를 그릴 사각형으로
 * 나타낸 것이다. 선분은 격자선을 중심으로 하므로 두께의 절반씩이 옆의 두 셀에 놓인다. {@link LayoutBox#collapsedEdges} 의 선분 기하는 표 border box 기준이고
 * {@link LayoutBox#getCollapsedBorders()} 가 반환하는 선분은 문서 좌표이다. 선분은 바뀌지 않는다.
 */
public final class TableBorder {

    /** Left edge of the rectangle. 사각형의 왼쪽. */
    private final double x;
    /** Top edge of the rectangle. 사각형의 위쪽. */
    private final double y;
    /** Width of the rectangle. 사각형의 너비. */
    private final double w;
    /** Height of the rectangle. 사각형의 높이. */
    private final double h;
    /** True if the segment runs horizontally (its thickness is the height). 가로로 달리는 선분이면 true(두께가 높이). */
    private final boolean horizontal;
    /** The <code>border-style</code> keyword. <code>border-style</code> 키워드. */
    private final String style;
    /** The color as ARGB. ARGB 형식의 색 값. */
    private final int color;

    /**
     * Creates a segment.
     * <p>
     * 선분을 만든다.
     *
     * @param x          left edge / 왼쪽
     * @param y          top edge / 위쪽
     * @param w          width / 너비
     * @param h          height / 높이
     * @param horizontal true if it runs horizontally / 가로로 달리면 true
     * @param style      the border style keyword / border 스타일 키워드
     * @param color      the color as ARGB / ARGB 색
     */
    TableBorder(double x, double y, double w, double h, boolean horizontal, String style, int color) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.horizontal = horizontal;
        this.style = style;
        this.color = color;
    }

    /**
     * Returns a copy moved by an offset.
     * <p>
     * 오프셋만큼 옮긴 복사본을 반환한다.
     *
     * @param dx offset in x / x 오프셋
     * @param dy offset in y / y 오프셋
     * @return the new segment / 새 선분
     */
    TableBorder moved(double dx, double dy) {
        return new TableBorder(x + dx, y + dy, w, h, horizontal, style, color);
    }

    /**
     * Returns the left edge of the rectangle to paint.
     * <p>
     * 그릴 사각형의 왼쪽을 반환한다.
     *
     * @return x / x
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the top edge of the rectangle to paint.
     * <p>
     * 그릴 사각형의 위쪽을 반환한다.
     *
     * @return y / y
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the width of the rectangle to paint.
     * <p>
     * 그릴 사각형의 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getWidth() {
        return w;
    }

    /**
     * Returns the height of the rectangle to paint.
     * <p>
     * 그릴 사각형의 높이를 반환한다.
     *
     * @return height / 높이
     */
    public double getHeight() {
        return h;
    }

    /**
     * Tells whether the segment runs horizontally; its thickness is then {@link #getHeight()}, else {@link #getWidth()}.
     * <p>
     * 선분이 가로로 달리는지 알려 준다. 그러면 두께는 {@link #getHeight()} 이고 아니면 {@link #getWidth()} 이다.
     *
     * @return true for a horizontal segment / 가로 선분이면 true
     */
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * Returns the thickness of the border.
     * <p>
     * border 의 두께를 반환한다.
     *
     * @return the thickness / 두께
     */
    public double getThickness() {
        return horizontal ? h : w;
    }

    /**
     * Returns the <code>border-style</code> keyword of the winning border.
     * <p>
     * 이긴 border 의 <code>border-style</code> 키워드를 반환한다.
     *
     * @return the style / 스타일
     */
    public String getStyle() {
        return style;
    }

    /**
     * Returns the color of the winning border as ARGB.
     * <p>
     * 이긴 border 의 색을 ARGB 로 반환한다.
     *
     * @return the color / 색
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns a short description for diagnostics.
     * <p>
     * 진단용 짧은 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        return (horizontal ? "H" : "V") + "[" + x + "," + y + " " + w + "x" + h + " " + style + "]";
    }
}
