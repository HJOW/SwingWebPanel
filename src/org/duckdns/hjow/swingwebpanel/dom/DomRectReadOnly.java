package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A rectangle of the geometry interfaces (<code>DOMRectReadOnly</code>, Geometry Interfaces Module): the origin and the size as numbers,
 * and the four edges that follow from them (a negative width or height puts the edge on the other side). The layout results that scripts
 * read (<code>getBoundingClientRect()</code>, <code>getClientRects()</code>) are such rectangles. Create one with the constructor and read
 * it with the getters; {@link DomRect} is the variant that scripts may change.
 * <p>
 * 기하 인터페이스의 사각형(<code>DOMRectReadOnly</code>, Geometry Interfaces Module): 원점과 크기 숫자, 그로부터 나오는 네 가장자리(너비나 높이가 음수이면
 * 가장자리가 반대쪽에 놓인다). 스크립트가 읽는 레이아웃 결과(<code>getBoundingClientRect()</code>, <code>getClientRects()</code>)가 이런 사각형이다.
 * 생성자로 만들고 접근자로 읽으며, 스크립트가 바꿀 수 있는 변종이 {@link DomRect} 이다.
 */
public class DomRectReadOnly {

    /**
     * The x coordinate of the origin.
     * <p>
     * 원점의 x 좌표.
     */
    double x;

    /**
     * The y coordinate of the origin.
     * <p>
     * 원점의 y 좌표.
     */
    double y;

    /**
     * The width (may be negative).
     * <p>
     * 너비(음수일 수 있다).
     */
    double width;

    /**
     * The height (may be negative).
     * <p>
     * 높이(음수일 수 있다).
     */
    double height;

    /**
     * Creates a rectangle.
     * <p>
     * 사각형을 만든다.
     *
     * @param x      the x of the origin / 원점의 x
     * @param y      the y of the origin / 원점의 y
     * @param width  the width / 너비
     * @param height the height / 높이
     */
    public DomRectReadOnly(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the x coordinate of the origin.
     * <p>
     * 원점의 x 좌표를 반환한다.
     *
     * @return x / x
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y coordinate of the origin.
     * <p>
     * 원점의 y 좌표를 반환한다.
     *
     * @return y / y
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the width.
     * <p>
     * 너비를 반환한다.
     *
     * @return the width / 너비
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the height.
     * <p>
     * 높이를 반환한다.
     *
     * @return the height / 높이
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns the top edge: the smaller of <code>y</code> and <code>y + height</code> (NaN if either is NaN).
     * <p>
     * 위쪽 가장자리를 반환한다: <code>y</code> 와 <code>y + height</code> 중 작은 값(하나가 NaN 이면 NaN).
     *
     * @return the top / 위쪽
     */
    public double getTop() {
        return Math.min(y, y + height);
    }

    /**
     * Returns the right edge: the larger of <code>x</code> and <code>x + width</code>.
     * <p>
     * 오른쪽 가장자리를 반환한다: <code>x</code> 와 <code>x + width</code> 중 큰 값.
     *
     * @return the right / 오른쪽
     */
    public double getRight() {
        return Math.max(x, x + width);
    }

    /**
     * Returns the bottom edge: the larger of <code>y</code> and <code>y + height</code>.
     * <p>
     * 아래쪽 가장자리를 반환한다: <code>y</code> 와 <code>y + height</code> 중 큰 값.
     *
     * @return the bottom / 아래쪽
     */
    public double getBottom() {
        return Math.max(y, y + height);
    }

    /**
     * Returns the left edge: the smaller of <code>x</code> and <code>x + width</code>.
     * <p>
     * 왼쪽 가장자리를 반환한다: <code>x</code> 와 <code>x + width</code> 중 작은 값.
     *
     * @return the left / 왼쪽
     */
    public double getLeft() {
        return Math.min(x, x + width);
    }
}
