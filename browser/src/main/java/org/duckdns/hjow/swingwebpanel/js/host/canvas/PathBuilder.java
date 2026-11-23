package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * The current default path of a 2D context. Points are transformed by the current transformation matrix when they are added, so the path is
 * kept in device (bitmap) space and later changes of the matrix do not move it. The builder tracks subpaths the way the canvas specification
 * describes them: a drawing method without a current subpath starts one, <code>closePath()</code> closes the subpath and the next method starts
 * a new subpath at the same start point, <code>rect()</code> leaves a new subpath at its corner. Arcs are made of cubic Bezier curves (at most
 * a quarter circle each) in user space and then transformed, which keeps ellipses exact under any affine matrix.
 * <p>
 * 2D 컨텍스트의 현재 기본 경로. 점은 더할 때 현재 변환 행렬로 변환되므로 경로는 장치(비트맵) 좌표로 보관되며 행렬이 나중에 바뀌어도 움직이지 않는다.
 * 빌더는 캔버스 규격이 설명하는 방식으로 하위 경로를 추적한다: 현재 하위 경로가 없을 때 그리기 메서드는 하위 경로를 시작하고,
 * <code>closePath()</code> 는 하위 경로를 닫으며 다음 메서드는 같은 시작점에서 새 하위 경로를 시작하고, <code>rect()</code> 는 모서리에 새 하위 경로를
 * 남긴다. 호는 사용자 좌표계에서 3차 베지어 곡선(각각 사분원 이하)으로 만든 뒤 변환하므로 어떤 아핀 행렬에서도 타원이 정확하다.
 */
final class PathBuilder {

    /**
     * Two pi, the angle of a full turn.
     * <p>
     * 한 바퀴의 각도인 2π.
     */
    private static final double TWO_PI = Math.PI * 2;

    /**
     * The path in device space.
     * <p>
     * 장치 좌표의 경로.
     */
    private Path2D.Double path = new Path2D.Double();

    /**
     * Whether the path has a current subpath.
     * <p>
     * 현재 하위 경로가 있는지 여부.
     */
    private boolean hasSubpath;

    /**
     * Whether the current subpath was closed (the next operation then starts a new subpath at the start point).
     * <p>
     * 현재 하위 경로가 닫혔는지 여부(그러면 다음 연산은 시작점에서 새 하위 경로를 시작한다).
     */
    private boolean closed;

    /**
     * The start point of the current subpath in device space.
     * <p>
     * 현재 하위 경로의 시작점(장치 좌표).
     */
    private double startX;

    /**
     * The start point of the current subpath in device space.
     * <p>
     * 현재 하위 경로의 시작점(장치 좌표).
     */
    private double startY;

    /**
     * The last point in device space.
     * <p>
     * 마지막 점(장치 좌표).
     */
    private double lastX;

    /**
     * The last point in device space.
     * <p>
     * 마지막 점(장치 좌표).
     */
    private double lastY;

    /**
     * Removes every subpath (<code>beginPath()</code>).
     * <p>
     * 모든 하위 경로를 지운다(<code>beginPath()</code>).
     */
    void reset() {
        path = new Path2D.Double();
        hasSubpath = false;
        closed = false;
    }

    /**
     * Tells whether the path has no subpath.
     * <p>
     * 경로에 하위 경로가 없는지 알려 준다.
     *
     * @return true if empty / 비어 있으면 true
     */
    boolean isEmpty() {
        return !hasSubpath;
    }

    /**
     * Returns a copy of the path with a winding rule.
     * <p>
     * 감김 규칙을 준 경로의 복사본을 반환한다.
     *
     * @param evenOdd true for the even-odd rule, false for non-zero / 짝홀 규칙이면 true, 0이 아닌 규칙이면 false
     * @return the shape in device space / 장치 좌표의 도형
     */
    Shape copy(boolean evenOdd) {
        Path2D.Double p = new Path2D.Double(path);
        p.setWindingRule(evenOdd ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        return p;
    }

    /**
     * Transforms a user-space point to device space.
     * <p>
     * 사용자 좌표의 점을 장치 좌표로 변환한다.
     *
     * @param ctm the matrix / 행렬
     * @param x   user x / 사용자 x
     * @param y   user y / 사용자 y
     * @return the device point / 장치 좌표의 점
     */
    private static Point2D.Double device(AffineTransform ctm, double x, double y) {
        Point2D.Double p = new Point2D.Double(x, y);
        ctm.transform(p, p);
        return p;
    }

    /**
     * Starts a new subpath at the start point after <code>closePath()</code>, if that is pending.
     * <p>
     * <code>closePath()</code> 뒤에 시작점에서 새 하위 경로를 시작해야 하면 시작한다.
     */
    private void resumeAfterClose() {
        if (closed) {
            path.moveTo(startX, startY);
            closed = false;
        }
    }

    /**
     * Starts a new subpath.
     * <p>
     * 새 하위 경로를 시작한다.
     *
     * @param ctm the matrix / 행렬
     * @param x   user x / 사용자 x
     * @param y   user y / 사용자 y
     */
    void moveTo(AffineTransform ctm, double x, double y) {
        Point2D.Double p = device(ctm, x, y);
        path.moveTo(p.x, p.y);
        startX = p.x;
        startY = p.y;
        lastX = p.x;
        lastY = p.y;
        hasSubpath = true;
        closed = false;
    }

    /**
     * Adds a straight line; without a subpath it starts one.
     * <p>
     * 직선을 더한다. 하위 경로가 없으면 시작한다.
     *
     * @param ctm the matrix / 행렬
     * @param x   user x / 사용자 x
     * @param y   user y / 사용자 y
     */
    void lineTo(AffineTransform ctm, double x, double y) {
        if (!hasSubpath) {
            moveTo(ctm, x, y);
            return;
        }
        resumeAfterClose();
        Point2D.Double p = device(ctm, x, y);
        path.lineTo(p.x, p.y);
        lastX = p.x;
        lastY = p.y;
    }

    /**
     * Adds a quadratic curve.
     * <p>
     * 2차 곡선을 더한다.
     *
     * @param ctm the matrix / 행렬
     * @param cx  control point user x / 제어점 사용자 x
     * @param cy  control point user y / 제어점 사용자 y
     * @param x   end point user x / 끝점 사용자 x
     * @param y   end point user y / 끝점 사용자 y
     */
    void quadTo(AffineTransform ctm, double cx, double cy, double x, double y) {
        if (!hasSubpath) {
            moveTo(ctm, cx, cy);
        }
        resumeAfterClose();
        Point2D.Double c = device(ctm, cx, cy);
        Point2D.Double p = device(ctm, x, y);
        path.quadTo(c.x, c.y, p.x, p.y);
        lastX = p.x;
        lastY = p.y;
    }

    /**
     * Adds a cubic Bezier curve.
     * <p>
     * 3차 베지어 곡선을 더한다.
     *
     * @param ctm the matrix / 행렬
     * @param x1  first control point user x / 첫 제어점 사용자 x
     * @param y1  first control point user y / 첫 제어점 사용자 y
     * @param x2  second control point user x / 둘째 제어점 사용자 x
     * @param y2  second control point user y / 둘째 제어점 사용자 y
     * @param x   end point user x / 끝점 사용자 x
     * @param y   end point user y / 끝점 사용자 y
     */
    void cubicTo(AffineTransform ctm, double x1, double y1, double x2, double y2, double x, double y) {
        if (!hasSubpath) {
            moveTo(ctm, x1, y1);
        }
        resumeAfterClose();
        Point2D.Double a = device(ctm, x1, y1);
        Point2D.Double b = device(ctm, x2, y2);
        Point2D.Double p = device(ctm, x, y);
        path.curveTo(a.x, a.y, b.x, b.y, p.x, p.y);
        lastX = p.x;
        lastY = p.y;
    }

    /**
     * Closes the current subpath.
     * <p>
     * 현재 하위 경로를 닫는다.
     */
    void closePath() {
        if (!hasSubpath || closed) {
            return;
        }
        path.closePath();
        closed = true;
        lastX = startX;
        lastY = startY;
    }

    /**
     * Adds a rectangle as a closed subpath and leaves a new subpath at its corner.
     * <p>
     * 사각형을 닫힌 하위 경로로 더하고 그 모서리에 새 하위 경로를 남긴다.
     *
     * @param ctm the matrix / 행렬
     * @param x   user x / 사용자 x
     * @param y   user y / 사용자 y
     * @param w   width (may be negative) / 너비(음수 가능)
     * @param h   height (may be negative) / 높이(음수 가능)
     */
    void rect(AffineTransform ctm, double x, double y, double w, double h) {
        moveTo(ctm, x, y);
        lineTo(ctm, x + w, y);
        lineTo(ctm, x + w, y + h);
        lineTo(ctm, x, y + h);
        closePath();
    }

    /**
     * Adds an arc of an ellipse; a line connects the current point to the start of the arc.
     * <p>
     * 타원의 호를 더한다. 현재 점에서 호의 시작까지 직선으로 잇는다.
     *
     * @param ctm   the matrix / 행렬
     * @param cx    center user x / 중심 사용자 x
     * @param cy    center user y / 중심 사용자 y
     * @param rx    x radius, not negative / x 반지름(음수 불가)
     * @param ry    y radius, not negative / y 반지름(음수 불가)
     * @param rot   rotation of the ellipse in radians / 타원의 회전(라디안)
     * @param start start angle in radians / 시작 각도(라디안)
     * @param end   end angle in radians / 끝 각도(라디안)
     * @param ccw   true to go counter-clockwise / 반시계 방향이면 true
     */
    void ellipse(AffineTransform ctm, double cx, double cy, double rx, double ry, double rot, double start, double end, boolean ccw) {
        double sweep;
        // 한 바퀴 이상이면 정확히 한 바퀴, 아니면 방향에 맞춰 각도 차를 한 바퀴 안으로 줄인다.
        if (!ccw && end - start >= TWO_PI) {
            sweep = TWO_PI;
        } else if (ccw && start - end >= TWO_PI) {
            sweep = -TWO_PI;
        } else if (!ccw) {
            sweep = mod(end - start);
        } else {
            sweep = -mod(start - end);
        }
        double cosR = Math.cos(rot);
        double sinR = Math.sin(rot);
        double sx = cx + rx * Math.cos(start) * cosR - ry * Math.sin(start) * sinR;
        double sy = cy + rx * Math.cos(start) * sinR + ry * Math.sin(start) * cosR;
        if (hasSubpath) {
            lineTo(ctm, sx, sy);
        } else {
            moveTo(ctm, sx, sy);
        }
        emitArc(ctm, cx, cy, rx, ry, rot, start, sweep);
    }

    /**
     * Reduces an angle to the range 0 (inclusive) to 2 pi (exclusive).
     * <p>
     * 각도를 0 이상 2π 미만으로 줄인다.
     *
     * @param a the angle / 각도
     * @return the reduced angle / 줄인 각도
     */
    private static double mod(double a) {
        double r = a % TWO_PI;
        return r < 0 ? r + TWO_PI : r;
    }

    /**
     * Appends the Bezier curves of an arc after the current point (which is already at the start of the arc).
     * <p>
     * 호의 베지어 곡선을 현재 점(이미 호의 시작에 있음) 뒤에 덧붙인다.
     *
     * @param ctm   the matrix / 행렬
     * @param cx    center user x / 중심 사용자 x
     * @param cy    center user y / 중심 사용자 y
     * @param rx    x radius / x 반지름
     * @param ry    y radius / y 반지름
     * @param rot   rotation / 회전
     * @param start start angle / 시작 각도
     * @param sweep signed sweep angle / 부호 있는 휩쓴 각도
     */
    private void emitArc(AffineTransform ctm, double cx, double cy, double rx, double ry, double rot, double start, double sweep) {
        if (sweep == 0) {
            return;
        }
        int n = (int) Math.ceil(Math.abs(sweep) / (Math.PI / 2) - 1e-9);
        n = Math.max(1, n);
        double d = sweep / n;
        double k = 4.0 / 3.0 * Math.tan(d / 4);
        double cosR = Math.cos(rot);
        double sinR = Math.sin(rot);
        // 사분원 이하의 조각마다 3차 베지어 곡선 하나를 더한다.
        for (int i = 0; i < n; i++) {
            double t0 = start + d * i;
            double t1 = t0 + d;
            double c0 = Math.cos(t0);
            double s0 = Math.sin(t0);
            double c1 = Math.cos(t1);
            double s1 = Math.sin(t1);
            double[] p1 = unit(cx, cy, rx, ry, cosR, sinR, c0 - k * s0, s0 + k * c0);
            double[] p2 = unit(cx, cy, rx, ry, cosR, sinR, c1 + k * s1, s1 - k * c1);
            double[] p3 = unit(cx, cy, rx, ry, cosR, sinR, c1, s1);
            cubicTo(ctm, p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
        }
    }

    /**
     * Maps a point of the unit circle to the (rotated) ellipse in user space.
     * <p>
     * 단위원의 점을 (회전한) 타원 위의 사용자 좌표로 옮긴다.
     *
     * @param cx   center x / 중심 x
     * @param cy   center y / 중심 y
     * @param rx   x radius / x 반지름
     * @param ry   y radius / y 반지름
     * @param cosR cosine of the rotation / 회전의 코사인
     * @param sinR sine of the rotation / 회전의 사인
     * @param ux   unit circle x / 단위원 x
     * @param uy   unit circle y / 단위원 y
     * @return x and y / x 와 y
     */
    private static double[] unit(double cx, double cy, double rx, double ry, double cosR, double sinR, double ux, double uy) {
        double ex = rx * ux;
        double ey = ry * uy;
        return new double[] { cx + ex * cosR - ey * sinR, cy + ex * sinR + ey * cosR };
    }

    /**
     * Adds a rounded rectangle as a closed subpath and leaves a new subpath at its corner. Negative sizes mirror the rectangle (and the
     * corners with it); radii whose neighbouring sums exceed a side are scaled down together.
     * <p>
     * 둥근 사각형을 닫힌 하위 경로로 더하고 그 모서리에 새 하위 경로를 남긴다. 음수 크기는 사각형(과 모서리)을 뒤집고, 이웃한 합이 변보다 긴 반지름은 함께
     * 줄인다.
     *
     * @param m     the matrix / 행렬
     * @param x     user x / 사용자 x
     * @param y     user y / 사용자 y
     * @param w     width (may be negative) / 너비(음수 가능)
     * @param h     height (may be negative) / 높이(음수 가능)
     * @param radii eight non-negative numbers: the x and y radius of the top left, top right, bottom right and bottom left corner / 음이 아닌 숫자 여덟 개: 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래 모서리의 x, y 반지름
     */
    void roundRect(AffineTransform m, double x, double y, double w, double h, double[] radii) {
        double[] r = radii.clone();
        double left = x;
        double top = y;
        double width = w;
        double height = h;
        // 너비가 음수이면 왼쪽과 오른쪽 모서리를 맞바꿔 양수로 만든다.
        if (width < 0) {
            left += width;
            width = -width;
            swap(r, 0, 2);
            swap(r, 1, 3);
            swap(r, 4, 6);
            swap(r, 5, 7);
        }
        // 높이가 음수이면 위쪽과 아래쪽 모서리를 맞바꿔 양수로 만든다.
        if (height < 0) {
            top += height;
            height = -height;
            swap(r, 0, 6);
            swap(r, 1, 7);
            swap(r, 2, 4);
            swap(r, 3, 5);
        }
        double scale = 1;
        // 이웃한 반지름의 합이 변보다 길면 모든 반지름을 같은 비율로 줄인다.
        scale = Math.min(scale, ratio(width, r[0] + r[2]));
        scale = Math.min(scale, ratio(width, r[6] + r[4]));
        scale = Math.min(scale, ratio(height, r[1] + r[7]));
        scale = Math.min(scale, ratio(height, r[3] + r[5]));
        // 줄이는 비율이 1 보다 작으면 모든 반지름에 곱한다.
        if (scale < 1) {
            for (int i = 0; i < r.length; i++) {
                r[i] *= scale;
            }
        }
        moveTo(m, left + r[0], top);
        lineTo(m, left + width - r[2], top);
        ellipse(m, left + width - r[2], top + r[3], r[2], r[3], 0, -Math.PI / 2, 0, false);
        lineTo(m, left + width, top + height - r[5]);
        ellipse(m, left + width - r[4], top + height - r[5], r[4], r[5], 0, 0, Math.PI / 2, false);
        lineTo(m, left + r[6], top + height);
        ellipse(m, left + r[6], top + height - r[7], r[6], r[7], 0, Math.PI / 2, Math.PI, false);
        lineTo(m, left, top + r[1]);
        ellipse(m, left + r[0], top + r[1], r[0], r[1], 0, Math.PI, Math.PI * 3 / 2, false);
        closePath();
        moveTo(m, left, top);
    }

    /**
     * Returns how much the sum of two radii has to shrink to fit a side.
     * <p>
     * 반지름 두 개의 합이 변에 들어가려면 얼마나 줄여야 하는지 반환한다.
     *
     * @param side the length of the side / 변의 길이
     * @param sum  the sum of the radii / 반지름의 합
     * @return the factor, at most 1 / 비율(1 이하)
     */
    private static double ratio(double side, double sum) {
        return sum > side && sum > 0 ? side / sum : 1;
    }

    /**
     * Swaps two entries of an array.
     * <p>
     * 배열의 두 항목을 맞바꾼다.
     *
     * @param a the array / 배열
     * @param i the first index / 첫 위치
     * @param j the second index / 둘째 위치
     */
    private static void swap(double[] a, int i, int j) {
        double t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Appends the subpaths of another path, transformed by a matrix (<code>Path2D.addPath</code>). The other path may be this path's twin
     * but not this very object.
     * <p>
     * 다른 경로의 하위 경로를 행렬로 변환하여 덧붙인다(<code>Path2D.addPath</code>). 다른 경로는 이 경로와 똑같은 모양일 수는 있지만 이 객체 자신이면 안 된다.
     *
     * @param other the path to append / 덧붙일 경로
     * @param m     the matrix applied to its points / 그 점에 적용할 행렬
     */
    void append(PathBuilder other, AffineTransform m) {
        if (!other.hasSubpath) {
            return;
        }
        path.append(m.createTransformedShape(other.path), false);
        Point2D.Double s = new Point2D.Double(other.startX, other.startY);
        Point2D.Double l = new Point2D.Double(other.lastX, other.lastY);
        m.transform(s, s);
        m.transform(l, l);
        startX = s.x;
        startY = s.y;
        lastX = l.x;
        lastY = l.y;
        hasSubpath = true;
        closed = other.closed;
    }

    /**
     * Adds the arc of <code>arcTo()</code>: the arc of the given radius that touches both lines (current point to p1, and p1 to p2).
     * <p>
     * <code>arcTo()</code> 의 호를 더한다: 두 직선(현재 점에서 p1, p1 에서 p2)에 모두 접하는 주어진 반지름의 호.
     *
     * @param ctm the matrix / 행렬
     * @param x1  p1 user x / p1 사용자 x
     * @param y1  p1 user y / p1 사용자 y
     * @param x2  p2 user x / p2 사용자 x
     * @param y2  p2 user y / p2 사용자 y
     * @param r   radius, not negative / 반지름(음수 불가)
     */
    void arcTo(AffineTransform ctm, double x1, double y1, double x2, double y2, double r) {
        if (!hasSubpath) {
            moveTo(ctm, x1, y1);
        }
        Point2D.Double p0;
        try {
            p0 = new Point2D.Double(lastX, lastY);
            ctm.createInverse().transform(p0, p0);
        } catch (NoninvertibleTransformException e) {
            // 행렬을 뒤집을 수 없으면 직선으로 대신한다.
            lineTo(ctm, x1, y1);
            return;
        }
        double ax = p0.x - x1;
        double ay = p0.y - y1;
        double bx = x2 - x1;
        double by = y2 - y1;
        double la = Math.sqrt(ax * ax + ay * ay);
        double lb = Math.sqrt(bx * bx + by * by);
        double cross = ax * by - ay * bx;
        // 점이 겹치거나 반지름이 0 이거나 세 점이 한 직선 위에 있으면 직선으로 대신한다.
        if (la == 0 || lb == 0 || r == 0 || Math.abs(cross) < 1e-12 * la * lb) {
            lineTo(ctm, x1, y1);
            return;
        }
        double ux = ax / la;
        double uy = ay / la;
        double vx = bx / lb;
        double vy = by / lb;
        double cosT = Math.max(-1, Math.min(1, ux * vx + uy * vy));
        double theta = Math.acos(cosT);
        double tanHalf = Math.tan(theta / 2);
        double dist = r / tanHalf;
        double t0x = x1 + ux * dist;
        double t0y = y1 + uy * dist;
        double t2x = x1 + vx * dist;
        double t2y = y1 + vy * dist;
        double bisx = ux + vx;
        double bisy = uy + vy;
        double bl = Math.sqrt(bisx * bisx + bisy * bisy);
        double centerDist = r / Math.sin(theta / 2);
        double ccx = x1 + bisx / bl * centerDist;
        double ccy = y1 + bisy / bl * centerDist;
        double a0 = Math.atan2(t0y - ccy, t0x - ccx);
        double a2 = Math.atan2(t2y - ccy, t2x - ccx);
        double sweep = a2 - a0;
        // 짧은 쪽 호를 고르도록 휩쓴 각도를 -π ~ π 로 줄인다.
        while (sweep > Math.PI) {
            sweep -= TWO_PI;
        }
        // 반대쪽 한계도 같은 방식으로 맞춘다.
        while (sweep < -Math.PI) {
            sweep += TWO_PI;
        }
        lineTo(ctm, t0x, t0y);
        emitArc(ctm, ccx, ccy, r, r, 0, a0, sweep);
    }
}
