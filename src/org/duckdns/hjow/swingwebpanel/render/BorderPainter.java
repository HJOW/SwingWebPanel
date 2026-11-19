package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Paints the borders of a box (plan item P09-05): every side separately with its width, style and color, and miter joins at the corners
 * (each side is the trapezoid between the outer and the inner corner points). Styles: solid, dashed, dotted, double, groove, ridge, inset
 * and outset; <code>none</code>, <code>hidden</code>, zero width and fully transparent colors paint nothing. Anti-aliasing is off in the
 * painter, so adjacent sides share the diagonal join without gaps or overlaps. Rounded corners are not painted (P10-04).
 * <p>
 * 상자의 경계선을 그린다(계획 P09-05): 변마다 너비, 스타일, 색을 따로 쓰고 모서리는 미터 조인이다(각 변은 바깥 모서리 점과 안쪽 모서리 점 사이의
 * 사다리꼴). 스타일: solid, dashed, dotted, double, groove, ridge, inset, outset. <code>none</code>, <code>hidden</code>, 너비 0, 완전 투명한 색은
 * 그리지 않는다. 렌더러는 안티앨리어싱을 끄므로 인접한 변이 대각선 조인을 틈이나 겹침 없이 공유한다. 둥근 모서리는 그리지 않는다(P10-04).
 */
final class BorderPainter {

    /**
     * Side index: top.
     * <p>
     * 변 번호: 위.
     */
    static final int TOP = 0;

    /**
     * Side index: right.
     * <p>
     * 변 번호: 오른쪽.
     */
    static final int RIGHT = 1;

    /**
     * Side index: bottom.
     * <p>
     * 변 번호: 아래.
     */
    static final int BOTTOM = 2;

    /**
     * Side index: left.
     * <p>
     * 변 번호: 왼쪽.
     */
    static final int LEFT = 3;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BorderPainter() {
    }

    /**
     * Tells whether a border style paints anything.
     * <p>
     * 경계선 스타일이 무언가를 그리는지 알려 준다.
     *
     * @param style the style keyword / 스타일 키워드
     * @return true if visible / 보이면 true
     */
    static boolean isVisibleStyle(String style) {
        return style != null && !"none".equals(style) && !"hidden".equals(style);
    }

    /**
     * Paints the four sides of a border box.
     * <p>
     * 테두리 상자의 네 변을 그린다.
     *
     * @param g      the graphics / 그래픽스
     * @param x      left edge of the border box / 테두리 상자의 왼쪽
     * @param y      top edge / 위쪽
     * @param w      width of the border box / 테두리 상자의 너비
     * @param h      height of the border box / 높이
     * @param widths side widths indexed by TOP, RIGHT, BOTTOM, LEFT / TOP, RIGHT, BOTTOM, LEFT 순서의 변 너비
     * @param styles style keywords in the same order / 같은 순서의 스타일 키워드
     * @param colors ARGB colors in the same order / 같은 순서의 ARGB 색
     */
    static void paint(Graphics2D g, double x, double y, double w, double h, double[] widths, String[] styles, int[] colors) {
        // 네 변을 차례로 그린다.
        for (int side = 0; side < 4; side++) {
            double bw = widths[side];
            if (bw <= 0 || !isVisibleStyle(styles[side]) || (colors[side] >>> 24) == 0) {
                continue;
            }
            Path2D poly = sidePolygon(side, x, y, w, h, widths);
            paintSide(g, side, poly, x, y, w, h, widths, styles[side], new Color(colors[side], true));
        }
    }

    /**
     * Builds the trapezoid of a side.
     * <p>
     * 변의 사다리꼴을 만든다.
     *
     * @param side   the side / 변
     * @param x      left / 왼쪽
     * @param y      top / 위쪽
     * @param w      width / 너비
     * @param h      height / 높이
     * @param widths side widths / 변 너비
     * @return the polygon / 다각형
     */
    private static Path2D sidePolygon(int side, double x, double y, double w, double h, double[] widths) {
        double wt = widths[TOP];
        double wr = widths[RIGHT];
        double wb = widths[BOTTOM];
        double wl = widths[LEFT];
        Path2D.Double p = new Path2D.Double();
        if (side == TOP) {
            p.moveTo(x, y);
            p.lineTo(x + w, y);
            p.lineTo(x + w - wr, y + wt);
            p.lineTo(x + wl, y + wt);
        } else if (side == RIGHT) {
            p.moveTo(x + w, y);
            p.lineTo(x + w, y + h);
            p.lineTo(x + w - wr, y + h - wb);
            p.lineTo(x + w - wr, y + wt);
        } else if (side == BOTTOM) {
            p.moveTo(x + w, y + h);
            p.lineTo(x, y + h);
            p.lineTo(x + wl, y + h - wb);
            p.lineTo(x + w - wr, y + h - wb);
        } else {
            p.moveTo(x, y + h);
            p.lineTo(x, y);
            p.lineTo(x + wl, y + wt);
            p.lineTo(x + wl, y + h - wb);
        }
        p.closePath();
        return p;
    }

    /**
     * Returns a darker version of a color for the shaded sides of inset, outset, groove and ridge borders.
     * <p>
     * inset, outset, groove, ridge 경계선의 그늘진 변에 쓸 더 어두운 색을 반환한다.
     *
     * @param c the color / 색
     * @return the darker color with the same alpha / 알파가 같은 더 어두운 색
     */
    private static Color shade(Color c) {
        return new Color(c.getRed() * 2 / 3, c.getGreen() * 2 / 3, c.getBlue() * 2 / 3, c.getAlpha());
    }

    /**
     * Paints one side in its style.
     * <p>
     * 변 하나를 해당 스타일로 그린다.
     *
     * @param g      graphics / 그래픽스
     * @param side   the side / 변
     * @param poly   the trapezoid of the side / 변의 사다리꼴
     * @param x      left / 왼쪽
     * @param y      top / 위쪽
     * @param w      width / 너비
     * @param h      height / 높이
     * @param widths side widths / 변 너비
     * @param style  style keyword / 스타일 키워드
     * @param color  color / 색
     */
    private static void paintSide(Graphics2D g, int side, Path2D poly, double x, double y, double w, double h, double[] widths, String style,
            Color color) {
        boolean topLeft = side == TOP || side == LEFT;
        double bw = widths[side];
        if ("dashed".equals(style) || "dotted".equals(style)) {
            Shape old = g.getClip();
            g.clip(poly);
            g.setColor(color);
            double len = "dotted".equals(style) ? bw : 3 * bw;
            double gap = "dotted".equals(style) ? bw : 2 * bw;
            Rectangle2D b = poly.getBounds2D();
            boolean horizontal = side == TOP || side == BOTTOM;
            double from = horizontal ? b.getMinX() : b.getMinY();
            double to = horizontal ? b.getMaxX() : b.getMaxY();
            // 변을 따라 일정한 길이의 조각을 건너뛰며 채운다.
            for (double p = from; p < to; p += len + gap) {
                if (horizontal) {
                    g.fill(new Rectangle2D.Double(p, b.getMinY(), len, b.getHeight()));
                } else {
                    g.fill(new Rectangle2D.Double(b.getMinX(), p, b.getWidth(), len));
                }
            }
            g.setClip(old);
        } else if (("double".equals(style) && bw >= 3) || "groove".equals(style) || "ridge".equals(style)) {
            Shape old = g.getClip();
            g.clip(poly);
            Rectangle2D b = poly.getBounds2D();
            Color first = color;
            Color second = color;
            double split = "double".equals(style) ? bw / 3.0 : bw / 2.0;
            if ("groove".equals(style)) {
                first = topLeft ? shade(color) : color;
                second = topLeft ? color : shade(color);
            } else if ("ridge".equals(style)) {
                first = topLeft ? color : shade(color);
                second = topLeft ? shade(color) : color;
            }
            strip(g, side, b, x, y, w, h, widths, 0, split, first);
            strip(g, side, b, x, y, w, h, widths, "double".equals(style) ? 2 * split : split, bw, second);
            g.setClip(old);
        } else {
            Color c = color;
            if ("inset".equals(style) && topLeft) {
                c = shade(color);
            } else if ("outset".equals(style) && !topLeft) {
                c = shade(color);
            }
            g.setColor(c);
            g.fill(poly);
        }
    }

    /**
     * Fills the part of a side between two distances from its outer edge (used by double, groove and ridge).
     * <p>
     * 변의 바깥 가장자리에서 두 거리 사이의 부분을 채운다(double, groove, ridge 가 쓴다).
     *
     * @param g      graphics (the clip is the side polygon) / 그래픽스(클립이 변의 다각형)
     * @param side   the side / 변
     * @param b      bounds of the side polygon / 변 다각형의 경계
     * @param x      left of the border box / 테두리 상자의 왼쪽
     * @param y      top / 위쪽
     * @param w      width / 너비
     * @param h      height / 높이
     * @param widths side widths / 변 너비
     * @param from   inner distance of the strip start / 띠 시작의 거리
     * @param to     distance of the strip end / 띠 끝의 거리
     * @param color  fill color / 채울 색
     */
    private static void strip(Graphics2D g, int side, Rectangle2D b, double x, double y, double w, double h, double[] widths, double from,
            double to, Color color) {
        if (to <= from) {
            return;
        }
        g.setColor(color);
        if (side == TOP) {
            g.fill(new Rectangle2D.Double(b.getMinX(), y + from, b.getWidth(), to - from));
        } else if (side == BOTTOM) {
            g.fill(new Rectangle2D.Double(b.getMinX(), y + h - to, b.getWidth(), to - from));
        } else if (side == LEFT) {
            g.fill(new Rectangle2D.Double(x + from, b.getMinY(), to - from, b.getHeight()));
        } else {
            g.fill(new Rectangle2D.Double(x + w - to, b.getMinY(), to - from, b.getHeight()));
        }
    }
}
