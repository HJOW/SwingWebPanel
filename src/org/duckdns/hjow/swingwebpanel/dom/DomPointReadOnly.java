package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A point of the geometry interfaces (<code>DOMPointReadOnly</code>, Geometry Interfaces Module): the three coordinates and the
 * perspective <code>w</code> (1 by default) as numbers. {@link DomPoint} is the variant that scripts may change.
 * <p>
 * 기하 인터페이스의 점(<code>DOMPointReadOnly</code>, Geometry Interfaces Module): 세 좌표와 원근 값 <code>w</code>(기본 1)의 숫자. 스크립트가 바꿀 수 있는
 * 변종이 {@link DomPoint} 이다.
 */
public class DomPointReadOnly {

    /**
     * The x coordinate.
     * <p>
     * x 좌표.
     */
    double x;

    /**
     * The y coordinate.
     * <p>
     * y 좌표.
     */
    double y;

    /**
     * The z coordinate.
     * <p>
     * z 좌표.
     */
    double z;

    /**
     * The perspective value.
     * <p>
     * 원근 값.
     */
    double w;

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
    public DomPointReadOnly(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Returns the x coordinate.
     * <p>
     * x 좌표를 반환한다.
     *
     * @return x / x
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y coordinate.
     * <p>
     * y 좌표를 반환한다.
     *
     * @return y / y
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the z coordinate.
     * <p>
     * z 좌표를 반환한다.
     *
     * @return z / z
     */
    public double getZ() {
        return z;
    }

    /**
     * Returns the perspective value.
     * <p>
     * 원근 값을 반환한다.
     *
     * @return w / w
     */
    public double getW() {
        return w;
    }
}
