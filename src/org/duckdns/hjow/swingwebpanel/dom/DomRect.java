package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A rectangle that scripts may change (<code>DOMRect</code>): the same as {@link DomRectReadOnly} with setters for the origin and the
 * size. The edges follow the changes. <code>getBoundingClientRect()</code> returns such rectangles (a fresh one on every call, so that a
 * change by a script never touches the layout).
 * <p>
 * 스크립트가 바꿀 수 있는 사각형(<code>DOMRect</code>): {@link DomRectReadOnly} 와 같되 원점과 크기의 설정자가 있다. 가장자리는 변경을 따른다.
 * <code>getBoundingClientRect()</code> 는 이런 사각형을 돌려준다(호출마다 새것이므로 스크립트의 변경이 레이아웃에 닿지 않는다).
 */
public class DomRect extends DomRectReadOnly {

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
    public DomRect(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    /**
     * Sets the x coordinate of the origin.
     * <p>
     * 원점의 x 좌표를 정한다.
     *
     * @param value x / x
     */
    public void setX(double value) {
        this.x = value;
    }

    /**
     * Sets the y coordinate of the origin.
     * <p>
     * 원점의 y 좌표를 정한다.
     *
     * @param value y / y
     */
    public void setY(double value) {
        this.y = value;
    }

    /**
     * Sets the width.
     * <p>
     * 너비를 정한다.
     *
     * @param value the width / 너비
     */
    public void setWidth(double value) {
        this.width = value;
    }

    /**
     * Sets the height.
     * <p>
     * 높이를 정한다.
     *
     * @param value the height / 높이
     */
    public void setHeight(double value) {
        this.height = value;
    }
}
