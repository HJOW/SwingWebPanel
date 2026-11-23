package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;

/**
 * A rectangle with rounded corners (plan item P10-04, CSS Backgrounds and Borders Level 3 section 5): the border edge, the padding edge
 * or the content edge of a box with the corner radii of <code>border-radius</code>. Every corner has a horizontal and a vertical radius
 * (an elliptical corner). The radii are resolved against the border box, scaled down together when two radii of a side would overlap
 * (section 5.5, factor <i>f = min(side length / sum of the radii on that side)</i>), and shrink by the border width and padding when the
 * edge moves inward (the radius of the inner edge is the outer radius minus the width, but not less than zero, 5.2).
 * <p>
 * Immutable. The path is built from quarter ellipses with Bezier curves.
 * <p>
 * 둥근 모서리가 있는 사각형(계획 P10-04, CSS Backgrounds and Borders Level 3 5 절): <code>border-radius</code> 의 모서리 반지름을 가진 상자의 border 가장자리,
 * padding 가장자리 또는 content 가장자리. 모서리마다 가로 반지름과 세로 반지름이 있다(타원 모서리). 반지름은 border box 에 대해 해석하고, 한 변에서 두 반지름이
 * 겹치면 함께 줄이며(5.5 절, 계수 <i>f = min(변의 길이 / 그 변 반지름의 합)</i>), 가장자리가 안쪽으로 옮겨 가면 border 너비와 padding 만큼 줄어든다(안쪽 가장자리의 반지름은
 * 바깥 반지름에서 너비를 뺀 값이며 0 미만은 아님, 5.2).
 * <p>
 * 불변이다. 경로는 베지어 곡선으로 만든 사분 타원으로 이룬다.
 */
final class RoundedBox {

    /** Left edge. 왼쪽. */
    final double x;
    /** Top edge. 위쪽. */
    final double y;
    /** Width of the rectangle. 사각형의 너비(border 가장자리 기준). */
    final double w;
    /** Height of the rectangle. 사각형의 높이(border 가장자리 기준). */
    final double h;
    /** The radii {top-left h, top-left v, top-right h, top-right v, bottom-right h, bottom-right v, bottom-left h, bottom-left v}. 반지름 {왼쪽 위 가로, 세로, 오른쪽 위 가로, 세로, 오른쪽 아래 가로, 세로, 왼쪽 아래 가로, 세로}. */
    final double[] r;

    /** The Bezier constant of a quarter ellipse. 사분 타원의 베지어 상수. */
    private static final double KAPPA = 0.5522847498307936;

    /**
     * Creates a rounded rectangle; the radii are used as given (not scaled).
     * <p>
     * 둥근 사각형을 만든다. 반지름은 주어진 대로 쓴다(줄이지 않는다).
     *
     * @param x left / 왼쪽
     * @param y top / 위쪽
     * @param w width / 너비
     * @param h height / 높이
     * @param r the eight radii / 여덟 반지름
     */
    RoundedBox(double x, double y, double w, double h, double[] r) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.r = r;
    }

    /**
     * Resolves one radius value (a length, a percentage of the given base, a calc expression) to pixels.
     * <p>
     * 반지름 값 하나(길이, 주어진 기준에 대한 백분율, calc 식)를 픽셀로 해석한다.
     *
     * @param v    the value / 값
     * @param base the length that 100% stands for / 100% 가 뜻하는 길이
     * @return pixels, at least 0 / 픽셀(0 이상)
     */
    static double resolve(CssValue v, double base) {
        double d = 0;
        if (v instanceof CssLength) {
            d = ((CssLength) v).getValue();
        } else if (v instanceof CssPercentage) {
            d = ((CssPercentage) v).getValue() * base / 100.0;
        } else if (v instanceof CssCalc) {
            d = ((CssCalc) v).resolve(LengthContext.defaults(), base);
        }
        return Double.isNaN(d) || d < 0 ? 0 : d;
    }

    /**
     * Reads the radii of <code>border-radius</code> of a style for a border box and scales them so that no two radii overlap.
     * <p>
     * 스타일의 <code>border-radius</code> 반지름을 border box 에 대해 읽고 두 반지름이 겹치지 않게 줄인다.
     *
     * @param s the style / 스타일
     * @param x left of the border box / border box 의 왼쪽
     * @param y top / 위쪽
     * @param w width / 너비
     * @param h height / 높이
     * @return the rounded border edge; its radii are all zero if the box has square corners / 둥근 border 가장자리. 모서리가 각지면 반지름이 모두 0
     */
    static RoundedBox ofBorderBox(ComputedStyle s, double x, double y, double w, double h) {
        double[] r = new double[8];
        // 모서리 네 개의 (가로, 세로) 반지름을 읽는다.
        for (int i = 0; i < 4; i++) {
            CssValue v = s.get(Prop.BORDER_TOP_LEFT_RADIUS + i);
            CssValue hv = v;
            CssValue vv = v;
            if (v instanceof CssList && ((CssList) v).size() >= 2) {
                hv = ((CssList) v).get(0);
                vv = ((CssList) v).get(1);
            }
            r[i * 2] = resolve(hv, w);
            r[i * 2 + 1] = resolve(vv, h);
        }
        double f = 1;
        // 변마다 두 반지름의 합이 변의 길이를 넘으면 모든 반지름을 같은 비율로 줄인다(5.5 절).
        f = Math.min(f, sideFactor(w, r[0] + r[2]));
        f = Math.min(f, sideFactor(h, r[3] + r[5]));
        f = Math.min(f, sideFactor(w, r[4] + r[6]));
        f = Math.min(f, sideFactor(h, r[7] + r[1]));
        if (f < 1) {
            // 모든 반지름에 같은 계수를 곱한다.
            for (int i = 0; i < 8; i++) {
                r[i] *= f;
            }
        }
        return new RoundedBox(x, y, w, h, r);
    }

    /**
     * Returns the scale factor that makes the radii on one side fit.
     * <p>
     * 한 변의 반지름이 들어맞게 하는 축소 계수를 반환한다.
     *
     * @param side length of the side / 변의 길이
     * @param sum  sum of the two radii on the side / 변의 두 반지름의 합
     * @return the factor, at most 1 / 계수(1 이하)
     */
    private static double sideFactor(double side, double sum) {
        return sum > side && sum > 0 ? Math.max(0, side) / sum : 1;
    }

    /**
     * Tells whether any corner is rounded.
     * <p>
     * 둥근 모서리가 있는지 알려 준다.
     *
     * @return true if a radius is positive / 반지름이 양수이면 true
     */
    boolean isRounded() {
        // 반지름이 하나라도 양수인지 본다.
        for (int i = 0; i < 8; i++) {
            if (r[i] > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the rectangle moved inward by the widths of four sides; the radii shrink by the width of the sides at each corner (not
     * below zero). A negative inset grows the rectangle (used for the spread of a shadow).
     * <p>
     * 네 변의 너비만큼 안쪽으로 옮긴 사각형을 반환한다. 반지름은 모서리의 두 변 너비만큼 줄어든다(0 미만은 아님). 음수 안쪽 거리는 사각형을 키운다(그림자의 퍼짐에 쓴다).
     *
     * @param top    inset of the top / 위쪽 안쪽 거리
     * @param right  inset of the right / 오른쪽 안쪽 거리
     * @param bottom inset of the bottom / 아래쪽 안쪽 거리
     * @param left   inset of the left / 왼쪽 안쪽 거리
     * @return the inner rectangle / 안쪽 사각형
     */
    RoundedBox inset(double top, double right, double bottom, double left) {
        double[] n = new double[8];
        n[0] = Math.max(0, r[0] - left);
        n[1] = Math.max(0, r[1] - top);
        n[2] = Math.max(0, r[2] - right);
        n[3] = Math.max(0, r[3] - top);
        n[4] = Math.max(0, r[4] - right);
        n[5] = Math.max(0, r[5] - bottom);
        n[6] = Math.max(0, r[6] - left);
        n[7] = Math.max(0, r[7] - bottom);
        return new RoundedBox(x + left, y + top, Math.max(0, w - left - right), Math.max(0, h - top - bottom), n);
    }

    /**
     * Returns the rectangle grown by a spread distance on every side: a radius grows by the distance if it is positive and stays zero
     * if it is zero (a square corner stays square, CSS Backgrounds and Borders 6.1).
     * <p>
     * 모든 변으로 퍼짐 거리만큼 키운 사각형을 반환한다. 양수 반지름은 그 거리만큼 커지고 0 이면 0 으로 남는다(각진 모서리는 각진 채로 남음, 6.1 절).
     *
     * @param d the spread distance (negative shrinks) / 퍼짐 거리(음수면 줄어듦)
     * @return the spread rectangle / 퍼진 사각형
     */
    RoundedBox spread(double d) {
        double[] n = new double[8];
        // 모서리마다 양수 반지름만 거리만큼 키우거나 줄인다.
        for (int i = 0; i < 8; i++) {
            n[i] = r[i] > 0 ? Math.max(0, r[i] + d) : 0;
        }
        return new RoundedBox(x - d, y - d, Math.max(0, w + 2 * d), Math.max(0, h + 2 * d), n);
    }

    /**
     * Returns the same rectangle moved by an offset.
     * <p>
     * 같은 사각형을 오프셋만큼 옮겨 반환한다.
     *
     * @param dx horizontal offset / 가로 오프셋
     * @param dy vertical offset / 세로 오프셋
     * @return the moved rectangle / 옮긴 사각형
     */
    RoundedBox translate(double dx, double dy) {
        return new RoundedBox(x + dx, y + dy, w, h, r);
    }

    /**
     * Returns the outline as a Java2D shape: a plain rectangle if no corner is rounded, otherwise a path of quarter ellipses.
     * <p>
     * 윤곽을 Java2D 도형으로 반환한다: 둥근 모서리가 없으면 평범한 사각형, 있으면 사분 타원의 경로.
     *
     * @return the shape / 도형
     */
    Shape toShape() {
        if (!isRounded()) {
            return new Rectangle2D.Double(x, y, w, h);
        }
        Path2D.Double p = new Path2D.Double();
        double tlh = r[0];
        double tlv = r[1];
        double trh = r[2];
        double trv = r[3];
        double brh = r[4];
        double brv = r[5];
        double blh = r[6];
        double blv = r[7];
        p.moveTo(x + tlh, y);
        p.lineTo(x + w - trh, y);
        corner(p, x + w - trh, y, x + w, y + trv, trh, trv, 0);
        p.lineTo(x + w, y + h - brv);
        corner(p, x + w, y + h - brv, x + w - brh, y + h, brh, brv, 1);
        p.lineTo(x + blh, y + h);
        corner(p, x + blh, y + h, x, y + h - blv, blh, blv, 2);
        p.lineTo(x, y + tlv);
        corner(p, x, y + tlv, x + tlh, y, tlh, tlv, 3);
        p.closePath();
        return p;
    }

    /**
     * Appends the quarter ellipse of a corner to a path (nothing for a zero radius, where the caller already moved to the corner).
     * <p>
     * 모서리의 사분 타원을 경로에 덧붙인다(반지름이 0 이면 이미 모서리에 와 있으므로 아무것도 하지 않는다).
     *
     * @param p      the path / 경로
     * @param x0     start x / 시작 x
     * @param y0     start y / 시작 y
     * @param x1     end x / 끝 x
     * @param y1     end y / 끝 y
     * @param rh     horizontal radius / 가로 반지름
     * @param rv     vertical radius / 세로 반지름
     * @param corner 0 top-right, 1 bottom-right, 2 bottom-left, 3 top-left / 0 오른쪽 위, 1 오른쪽 아래, 2 왼쪽 아래, 3 왼쪽 위
     */
    private static void corner(Path2D.Double p, double x0, double y0, double x1, double y1, double rh, double rv, int corner) {
        if (rh <= 0 || rv <= 0) {
            p.lineTo(x1, y1);
            return;
        }
        double kx = rh * KAPPA;
        double ky = rv * KAPPA;
        if (corner == 0) {
            p.curveTo(x0 + kx, y0, x1, y1 - ky, x1, y1);
        } else if (corner == 1) {
            p.curveTo(x0, y0 + ky, x1 + kx, y1, x1, y1);
        } else if (corner == 2) {
            p.curveTo(x0 - kx, y0, x1, y1 + ky, x1, y1);
        } else {
            p.curveTo(x0, y0 - ky, x1 - kx, y1, x1, y1);
        }
    }
}
