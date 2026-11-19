package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A point that scripts may change (<code>DOMPoint</code>): {@link DomPointReadOnly} with setters.
 * <p>
 * 스크립트가 바꿀 수 있는 점(<code>DOMPoint</code>): 설정자가 있는 {@link DomPointReadOnly}.
 */
public class DomPoint extends DomPointReadOnly {

    /**
     * Creates a point.
     * <p>
     * 점을 만든다.
     *
     * @param x the x / x
     * @param y the y / y
     * @param z the z / z
     * @param w the perspective / 원근 값
     */
    public DomPoint(double x, double y, double z, double w) {
        super(x, y, z, w);
    }

    /**
     * Sets the x coordinate.
     * <p>
     * x 좌표를 정한다.
     *
     * @param value x / x
     */
    public void setX(double value) {
        this.x = value;
    }

    /**
     * Sets the y coordinate.
     * <p>
     * y 좌표를 정한다.
     *
     * @param value y / y
     */
    public void setY(double value) {
        this.y = value;
    }

    /**
     * Sets the z coordinate.
     * <p>
     * z 좌표를 정한다.
     *
     * @param value z / z
     */
    public void setZ(double value) {
        this.z = value;
    }

    /**
     * Sets the perspective value.
     * <p>
     * 원근 값을 정한다.
     *
     * @param value w / w
     */
    public void setW(double value) {
        this.w = value;
    }
}
