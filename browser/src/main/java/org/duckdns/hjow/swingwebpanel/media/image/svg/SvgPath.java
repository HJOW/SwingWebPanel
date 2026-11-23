package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsed SVG path data (plan item P18-04): the path data grammar with all commands (<code>MmLlHhVvCcSsQqTtAaZz</code>), numbers without
 * separators, arc flags without separators, and the error rule of the specification: the path is rendered up to the last correct segment.
 * Besides the Java2D shape the object keeps the original segments with their start and end tangents, which the marker code needs
 * (an arc is one vertex although it is drawn with several Bezier curves).
 * <p>
 * 해석된 SVG 경로 데이터(계획 P18-04): 모든 명령(<code>MmLlHhVvCcSsQqTtAaZz</code>)을 가진 경로 데이터 문법, 구분자 없는 수, 구분자 없는 호
 * 플래그, 규격의 오류 규칙(마지막으로 올바른 구간까지 그린다). Java2D 도형과 함께, 마커 코드가 필요로 하는 원래 구간과 그 시작/끝 접선을
 * 보관한다(호는 여러 베지어 곡선으로 그려도 꼭짓점은 하나이다).
 */
final class SvgPath {

    /**
     * Segment kind: move to (starts a subpath).
     * <p>
     * 구간 종류: 이동(부분 경로 시작).
     */
    static final int MOVE = 0;

    /**
     * Segment kind: a line, a curve or an arc (anything drawn).
     * <p>
     * 구간 종류: 직선, 곡선, 호(그려지는 모든 것).
     */
    static final int DRAW = 1;

    /**
     * Segment kind: close path.
     * <p>
     * 구간 종류: 경로 닫기.
     */
    static final int CLOSE = 2;

    /**
     * One original path segment with its tangents.
     * <p>
     * 접선을 가진 원래 경로 구간 하나.
     */
    static final class Seg {

        /**
         * The kind: {@link SvgPath#MOVE}, {@link SvgPath#DRAW} or {@link SvgPath#CLOSE}.
         * <p>
         * 종류: {@link SvgPath#MOVE}, {@link SvgPath#DRAW}, {@link SvgPath#CLOSE}.
         */
        final int kind;

        /**
         * The end point (for CLOSE the start point of the subpath).
         * <p>
         * 끝점(CLOSE 이면 부분 경로의 시작점).
         */
        final double x;

        /**
         * The y coordinate of the end point.
         * <p>
         * 끝점의 y 좌표.
         */
        final double y;

        /**
         * The direction (not normalized) in which the segment leaves its start point.
         * <p>
         * 구간이 시작점에서 나가는 방향(정규화하지 않음).
         */
        double startDx;

        /**
         * The y part of the start direction.
         * <p>
         * 시작 방향의 y 성분.
         */
        double startDy;

        /**
         * The direction (not normalized) in which the segment arrives at its end point.
         * <p>
         * 구간이 끝점에 도착하는 방향(정규화하지 않음).
         */
        double endDx;

        /**
         * The y part of the end direction.
         * <p>
         * 도착 방향의 y 성분.
         */
        double endDy;

        /**
         * Creates a segment.
         * <p>
         * 구간을 만든다.
         *
         * @param kind kind / 종류
         * @param x    end x / 끝 x
         * @param y    end y / 끝 y
         */
        Seg(int kind, double x, double y) {
            this.kind = kind;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * The Java2D shape of the path.
     * <p>
     * 경로의 Java2D 도형.
     */
    final Path2D.Double shape = new Path2D.Double(Path2D.WIND_NON_ZERO);

    /**
     * The original segments.
     * <p>
     * 원래 구간.
     */
    final List<Seg> segs = new ArrayList<Seg>();

    /**
     * Creates an empty path.
     * <p>
     * 빈 경로를 만든다.
     */
    SvgPath() {
    }

    /**
     * Parses path data; on an error the already parsed part is kept.
     * <p>
     * 경로 데이터를 해석한다. 오류가 나면 이미 해석한 부분을 유지한다.
     *
     * @param d the value of the <code>d</code> attribute / <code>d</code> 속성 값
     * @return the path (possibly empty) / 경로(비어 있을 수 있음)
     */
    static SvgPath parse(String d) {
        SvgPath path = new SvgPath();
        if (d == null) {
            return path;
        }
        SvgScanner sc = new SvgScanner(d);
        double cx = 0;
        double cy = 0;
        double sx = 0;
        double sy = 0;
        double lastCtrlX = 0;
        double lastCtrlY = 0;
        char prev = 0;
        char cmd = 0;
        boolean first = true;
        sc.skipWs();
        // 명령 문자와 그 인수를 끝까지 읽는다.
        while (!sc.atEnd()) {
            char c = sc.peek();
            if (isCommand(c)) {
                cmd = c;
                sc.p++;
                if (first && cmd != 'M' && cmd != 'm') {
                    return path;
                }
                first = false;
            } else if (cmd == 0 || cmd == 'Z' || cmd == 'z') {
                break;
            } else if (cmd == 'M') {
                cmd = 'L';
            } else if (cmd == 'm') {
                cmd = 'l';
            }
            boolean rel = Character.isLowerCase(cmd);
            char up = Character.toUpperCase(cmd);
            sc.skipWs();
            if (up == 'Z') {
                if (path.segs.size() > 0) {
                    path.close(sx, sy);
                    cx = sx;
                    cy = sy;
                }
                prev = 'Z';
                sc.skipWsAndCommas();
                continue;
            }
            double[] a = new double[7];
            int need = argCount(up);
            // 명령이 요구하는 수만큼 인수를 읽는다(호 플래그는 한 글자).
            boolean ok = true;
            for (int i = 0; i < need; i++) {
                if (i > 0) {
                    sc.skipSep();
                }
                if (up == 'A' && (i == 3 || i == 4)) {
                    char f = sc.peek();
                    if (f != '0' && f != '1') {
                        ok = false;
                        break;
                    }
                    a[i] = f - '0';
                    sc.p++;
                } else if (sc.number()) {
                    a[i] = sc.value;
                } else {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                break;
            }
            double ox = rel ? cx : 0;
            double oy = rel ? cy : 0;
            switch (up) {
            case 'M': {
                cx = a[0] + ox;
                cy = a[1] + oy;
                sx = cx;
                sy = cy;
                path.moveTo(cx, cy);
                prev = 'M';
                break;
            }
            case 'L': {
                double x = a[0] + ox;
                double y = a[1] + oy;
                path.lineTo(cx, cy, x, y);
                cx = x;
                cy = y;
                prev = 'L';
                break;
            }
            case 'H': {
                double x = a[0] + ox;
                path.lineTo(cx, cy, x, cy);
                cx = x;
                prev = 'L';
                break;
            }
            case 'V': {
                double y = a[0] + oy;
                path.lineTo(cx, cy, cx, y);
                cy = y;
                prev = 'L';
                break;
            }
            case 'C': {
                double x1 = a[0] + ox;
                double y1 = a[1] + oy;
                double x2 = a[2] + ox;
                double y2 = a[3] + oy;
                double x = a[4] + ox;
                double y = a[5] + oy;
                path.curveTo(cx, cy, x1, y1, x2, y2, x, y);
                lastCtrlX = x2;
                lastCtrlY = y2;
                cx = x;
                cy = y;
                prev = 'C';
                break;
            }
            case 'S': {
                double x1 = cx;
                double y1 = cy;
                // 앞 명령이 곡선이면 이전 제어점을 현재점에 대해 반사한다.
                if (prev == 'C') {
                    x1 = 2 * cx - lastCtrlX;
                    y1 = 2 * cy - lastCtrlY;
                }
                double x2 = a[0] + ox;
                double y2 = a[1] + oy;
                double x = a[2] + ox;
                double y = a[3] + oy;
                path.curveTo(cx, cy, x1, y1, x2, y2, x, y);
                lastCtrlX = x2;
                lastCtrlY = y2;
                cx = x;
                cy = y;
                prev = 'C';
                break;
            }
            case 'Q': {
                double x1 = a[0] + ox;
                double y1 = a[1] + oy;
                double x = a[2] + ox;
                double y = a[3] + oy;
                path.quadTo(cx, cy, x1, y1, x, y);
                lastCtrlX = x1;
                lastCtrlY = y1;
                cx = x;
                cy = y;
                prev = 'Q';
                break;
            }
            case 'T': {
                double x1 = cx;
                double y1 = cy;
                // 앞 명령이 이차 곡선이면 이전 제어점을 현재점에 대해 반사한다.
                if (prev == 'Q') {
                    x1 = 2 * cx - lastCtrlX;
                    y1 = 2 * cy - lastCtrlY;
                }
                double x = a[0] + ox;
                double y = a[1] + oy;
                path.quadTo(cx, cy, x1, y1, x, y);
                lastCtrlX = x1;
                lastCtrlY = y1;
                cx = x;
                cy = y;
                prev = 'Q';
                break;
            }
            default: {
                double x = a[5] + ox;
                double y = a[6] + oy;
                path.arcTo(cx, cy, a[0], a[1], a[2], a[3] != 0, a[4] != 0, x, y);
                cx = x;
                cy = y;
                prev = 'A';
                break;
            }
            }
            sc.skipWsAndCommas();
        }
        return path;
    }

    /**
     * Tells whether a character is a path command letter.
     * <p>
     * 문자가 경로 명령 문자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for a command / 명령이면 true
     */
    private static boolean isCommand(char c) {
        return "MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0;
    }

    /**
     * Returns the number of arguments of a command.
     * <p>
     * 명령의 인수 개수를 반환한다.
     *
     * @param up the upper-case command letter / 대문자 명령 문자
     * @return the count / 개수
     */
    private static int argCount(char up) {
        switch (up) {
        case 'M':
        case 'L':
        case 'T':
            return 2;
        case 'H':
        case 'V':
            return 1;
        case 'C':
            return 6;
        case 'S':
        case 'Q':
            return 4;
        case 'A':
            return 7;
        default:
            return 0;
        }
    }

    /**
     * Starts a subpath.
     * <p>
     * 부분 경로를 시작한다.
     *
     * @param x x / x
     * @param y y / y
     */
    private void moveTo(double x, double y) {
        shape.moveTo(x, y);
        segs.add(new Seg(MOVE, x, y));
    }

    /**
     * Adds a line from the current point.
     * <p>
     * 현재점에서 직선을 추가한다.
     *
     * @param x0 start x / 시작 x
     * @param y0 start y / 시작 y
     * @param x  end x / 끝 x
     * @param y  end y / 끝 y
     */
    private void lineTo(double x0, double y0, double x, double y) {
        ensureStart(x0, y0);
        shape.lineTo(x, y);
        Seg s = new Seg(DRAW, x, y);
        s.startDx = x - x0;
        s.startDy = y - y0;
        s.endDx = s.startDx;
        s.endDy = s.startDy;
        segs.add(s);
    }

    /**
     * Adds a cubic Bezier curve.
     * <p>
     * 삼차 베지어 곡선을 추가한다.
     *
     * @param x0 start x / 시작 x
     * @param y0 start y / 시작 y
     * @param x1 first control x / 첫 제어점 x
     * @param y1 first control y / 첫 제어점 y
     * @param x2 second control x / 둘째 제어점 x
     * @param y2 second control y / 둘째 제어점 y
     * @param x  end x / 끝 x
     * @param y  end y / 끝 y
     */
    private void curveTo(double x0, double y0, double x1, double y1, double x2, double y2, double x, double y) {
        ensureStart(x0, y0);
        shape.curveTo(x1, y1, x2, y2, x, y);
        segs.add(curveSeg(x0, y0, x1, y1, x2, y2, x, y));
    }

    /**
     * Builds the segment record of a cubic curve with its tangents (a coincident control point falls back to the next point).
     * <p>
     * 삼차 곡선의 구간 기록을 접선과 함께 만든다(제어점이 겹치면 다음 점으로 대신한다).
     *
     * @param x0 start x / 시작 x
     * @param y0 start y / 시작 y
     * @param x1 first control x / 첫 제어점 x
     * @param y1 first control y / 첫 제어점 y
     * @param x2 second control x / 둘째 제어점 x
     * @param y2 second control y / 둘째 제어점 y
     * @param x  end x / 끝 x
     * @param y  end y / 끝 y
     * @return the segment / 구간
     */
    private static Seg curveSeg(double x0, double y0, double x1, double y1, double x2, double y2, double x, double y) {
        Seg s = new Seg(DRAW, x, y);
        double dx = x1 - x0;
        double dy = y1 - y0;
        // 시작 접선: 첫 제어점이 시작점과 같으면 둘째 제어점, 그것도 같으면 끝점을 쓴다.
        if (dx == 0 && dy == 0) {
            dx = x2 - x0;
            dy = y2 - y0;
        }
        if (dx == 0 && dy == 0) {
            dx = x - x0;
            dy = y - y0;
        }
        s.startDx = dx;
        s.startDy = dy;
        double ex = x - x2;
        double ey = y - y2;
        // 끝 접선: 둘째 제어점이 끝점과 같으면 첫 제어점, 그것도 같으면 시작점을 쓴다.
        if (ex == 0 && ey == 0) {
            ex = x - x1;
            ey = y - y1;
        }
        if (ex == 0 && ey == 0) {
            ex = x - x0;
            ey = y - y0;
        }
        s.endDx = ex;
        s.endDy = ey;
        return s;
    }

    /**
     * Adds a quadratic Bezier curve.
     * <p>
     * 이차 베지어 곡선을 추가한다.
     *
     * @param x0 start x / 시작 x
     * @param y0 start y / 시작 y
     * @param x1 control x / 제어점 x
     * @param y1 control y / 제어점 y
     * @param x  end x / 끝 x
     * @param y  end y / 끝 y
     */
    private void quadTo(double x0, double y0, double x1, double y1, double x, double y) {
        ensureStart(x0, y0);
        shape.quadTo(x1, y1, x, y);
        // 이차 곡선을 같은 모양의 삼차 곡선으로 보아 접선을 계산한다.
        segs.add(curveSeg(x0, y0, x0 + 2.0 / 3 * (x1 - x0), y0 + 2.0 / 3 * (y1 - y0), x + 2.0 / 3 * (x1 - x), y + 2.0 / 3 * (y1 - y), x, y));
    }

    /**
     * Makes sure that a subpath is open before a drawing command (after a close the next subpath starts at the closing point).
     * <p>
     * 그리기 명령 전에 부분 경로가 열려 있게 한다(닫은 직후에는 닫은 점에서 다음 부분 경로가 시작한다).
     *
     * @param x0 the current x / 현재 x
     * @param y0 the current y / 현재 y
     */
    private void ensureStart(double x0, double y0) {
        if (segs.isEmpty() || segs.get(segs.size() - 1).kind == CLOSE) {
            shape.moveTo(x0, y0);
            segs.add(new Seg(MOVE, x0, y0));
        }
    }

    /**
     * Closes the current subpath.
     * <p>
     * 현재 부분 경로를 닫는다.
     *
     * @param sx start x of the subpath / 부분 경로 시작 x
     * @param sy start y of the subpath / 부분 경로 시작 y
     */
    private void close(double sx, double sy) {
        if (segs.get(segs.size() - 1).kind == CLOSE) {
            return;
        }
        shape.closePath();
        Seg last = segs.get(segs.size() - 1);
        Seg s = new Seg(CLOSE, sx, sy);
        s.startDx = sx - last.x;
        s.startDy = sy - last.y;
        s.endDx = s.startDx;
        s.endDy = s.startDy;
        segs.add(s);
    }

    /**
     * Adds an elliptical arc (SVG 1.1 appendix F.6): out of range radii are scaled up, a zero radius gives a line.
     * <p>
     * 타원 호를 추가한다(SVG 1.1 부록 F.6). 범위를 벗어난 반지름은 키우고 반지름이 0 이면 직선이 된다.
     *
     * @param x0    start x / 시작 x
     * @param y0    start y / 시작 y
     * @param rx    x radius / x 반지름
     * @param ry    y radius / y 반지름
     * @param rot   x axis rotation in degrees / x 축 회전(도)
     * @param large large-arc flag / 큰 호 플래그
     * @param sweep sweep flag / 방향 플래그
     * @param x     end x / 끝 x
     * @param y     end y / 끝 y
     */
    private void arcTo(double x0, double y0, double rx, double ry, double rot, boolean large, boolean sweep, double x, double y) {
        if (x0 == x && y0 == y) {
            return;
        }
        double ax = Math.abs(rx);
        double ay = Math.abs(ry);
        if (ax == 0 || ay == 0) {
            lineTo(x0, y0, x, y);
            return;
        }
        ensureStart(x0, y0);
        double phi = Math.toRadians(rot % 360);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double dx2 = (x0 - x) / 2;
        double dy2 = (y0 - y) / 2;
        double x1p = cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;
        double lambda = (x1p * x1p) / (ax * ax) + (y1p * y1p) / (ay * ay);
        // 반지름이 너무 작아 두 점을 잇지 못하면 비율을 유지하며 키운다.
        if (lambda > 1) {
            double sq = Math.sqrt(lambda);
            ax *= sq;
            ay *= sq;
        }
        double num = ax * ax * ay * ay - ax * ax * y1p * y1p - ay * ay * x1p * x1p;
        double den = ax * ax * y1p * y1p + ay * ay * x1p * x1p;
        double coef = den == 0 ? 0 : Math.sqrt(Math.max(0, num / den));
        if (large == sweep) {
            coef = -coef;
        }
        double cxp = coef * (ax * y1p / ay);
        double cyp = coef * -(ay * x1p / ax);
        double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2;
        double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2;
        double th1 = Math.atan2((y1p - cyp) / ay, (x1p - cxp) / ax);
        double th2 = Math.atan2((-y1p - cyp) / ay, (-x1p - cxp) / ax);
        double dth = th2 - th1;
        // 방향 플래그에 맞게 각도 변화를 한 바퀴 안으로 맞춘다.
        if (sweep && dth < 0) {
            dth += 2 * Math.PI;
        } else if (!sweep && dth > 0) {
            dth -= 2 * Math.PI;
        }
        int n = (int) Math.ceil(Math.abs(dth) / (Math.PI / 2) - 1e-9);
        if (n < 1) {
            n = 1;
        }
        double step = dth / n;
        double t = 4.0 / 3 * Math.tan(step / 4);
        double px = x0;
        double py = y0;
        Seg first = null;
        Seg last = null;
        // 호를 90도 이하의 삼차 베지어 조각으로 나눈다.
        for (int i = 0; i < n; i++) {
            double a1 = th1 + i * step;
            double a2 = a1 + step;
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            double c2 = Math.cos(a2);
            double s2 = Math.sin(a2);
            double e1x = ax * (c1 - t * s1);
            double e1y = ay * (s1 + t * c1);
            double e2x = ax * (c2 + t * s2);
            double e2y = ay * (s2 - t * c2);
            double ex = ax * c2;
            double ey = ay * s2;
            double q1x = cosPhi * e1x - sinPhi * e1y + cx;
            double q1y = sinPhi * e1x + cosPhi * e1y + cy;
            double q2x = cosPhi * e2x - sinPhi * e2y + cx;
            double q2y = sinPhi * e2x + cosPhi * e2y + cy;
            double qx = i == n - 1 ? x : cosPhi * ex - sinPhi * ey + cx;
            double qy = i == n - 1 ? y : sinPhi * ex + cosPhi * ey + cy;
            shape.curveTo(q1x, q1y, q2x, q2y, qx, qy);
            Seg s = curveSeg(px, py, q1x, q1y, q2x, q2y, qx, qy);
            if (first == null) {
                first = s;
            }
            last = s;
            px = qx;
            py = qy;
        }
        // 호 전체를 꼭짓점 하나로 보고 시작/끝 접선을 합친다.
        Seg whole = new Seg(DRAW, x, y);
        whole.startDx = first.startDx;
        whole.startDy = first.startDy;
        whole.endDx = last.endDx;
        whole.endDy = last.endDy;
        segs.add(whole);
    }
}
