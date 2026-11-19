package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The floats of one block formatting context (plan item P10-01), kept as rectangles in the coordinates of that context (the origin is the
 * top-left corner of the content box of the root of the context). The layout asks it where a float goes (CSS 2.1 section 9.5.1), how wide
 * a line may be at a given height, where a cleared box must start (section 9.5.2) and how far the floats reach down (the height of a
 * block formatting context root that has an automatic height). Floats are only added in source order; the layout may also truncate the
 * list back to a previous size when it lays out a subtree a second time.
 * <p>
 * 하나의 블록 서식 문맥의 float 를 그 문맥의 좌표(원점은 문맥 루트의 content box 왼쪽 위)의 사각형으로 보관한다(계획 P10-01). 레이아웃은 이것에게
 * float 가 놓일 위치(CSS 2.1 9.5.1), 주어진 높이에서 줄의 너비, clear 된 상자가 시작할 위치(9.5.2), float 가 아래로 미치는 범위(높이가 auto 인 블록 서식
 * 문맥 루트의 높이)를 묻는다. float 는 소스 순서로만 더해지며, 하위 트리를 다시 배치할 때는 이전 크기로 목록을 줄일 수 있다.
 */
final class FloatContext {

    /** Tolerance when comparing coordinates. 좌표 비교 허용 오차. */
    private static final double EPS = 0.001;

    /**
     * One float: its margin box.
     * <p>
     * float 하나: margin box.
     */
    private static final class Item {
        /** True for a left float. 왼쪽 float 이면 true. */
        final boolean left;
        /** Left edge of the margin box. margin box 의 왼쪽. */
        final double x;
        /** Top edge of the margin box. margin box 의 위쪽. */
        final double y;
        /** Width of the margin box. margin box 의 너비. */
        final double w;
        /** Height of the margin box. margin box 의 높이. */
        final double h;
        /** The float area of <code>shape-outside</code> in the coordinates of the context, or null (the margin box). <code>shape-outside</code> 의 float 영역(문맥 좌표). 없으면 null(margin box). */
        final Area area;

        /**
         * Creates an item.
         * <p>
         * 항목을 만든다.
         *
         * @param left true for a left float / 왼쪽 float 이면 true
         * @param x    left edge / 왼쪽
         * @param y    top edge / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         */
        Item(boolean left, double x, double y, double w, double h, Area area) {
            this.left = left;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.area = area;
        }

        /**
         * Tells whether the float occupies some of the band that starts at a height and has a height.
         * <p>
         * float 가 주어진 높이에서 시작하는 띠의 일부를 차지하는지 알려 준다.
         *
         * @param top  top of the band / 띠의 위쪽
         * @param span height of the band (a point if 0) / 띠의 높이(0 이면 한 점)
         * @return true if they overlap vertically / 세로로 겹치면 true
         */
        boolean overlaps(double top, double span) {
            if (h <= EPS) {
                return false;
            }
            double bandBottom = top + Math.max(span, EPS);
            return y < bandBottom - EPS && y + h > top + EPS;
        }
    }

    /** The floats in source order. 소스 순서의 float. */
    private final List<Item> items = new ArrayList<Item>();

    /**
     * Returns the number of floats.
     * <p>
     * float 의 수를 반환한다.
     *
     * @return the count / 개수
     */
    int size() {
        return items.size();
    }

    /**
     * Removes the floats that were added after the list had a given size.
     * <p>
     * 목록이 주어진 크기였던 뒤에 더해진 float 를 없앤다.
     *
     * @param n the size to go back to / 되돌아갈 크기
     */
    void truncate(int n) {
        // 뒤에서부터 지운다.
        while (items.size() > n) {
            items.remove(items.size() - 1);
        }
    }

    /**
     * Returns the left limit of the free space in a band: the right edge of the left floats that overlap it, but at least the left edge of
     * the container.
     * <p>
     * 띠에서 비어 있는 공간의 왼쪽 한계를 반환한다: 띠와 겹치는 왼쪽 float 의 오른쪽 가장자리. 최소한 컨테이너의 왼쪽이다.
     *
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @param min  left edge of the container / 컨테이너의 왼쪽
     * @return the left limit / 왼쪽 한계
     */
    double leftEdge(double top, double span, double min) {
        double v = min;
        // 띠와 겹치는 왼쪽 float 중 가장 오른쪽 가장자리를 고른다.
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            if (f.left && f.overlaps(top, span)) {
                v = Math.max(v, f.x + f.w);
            }
        }
        return v;
    }

    /**
     * Returns the right limit of the free space in a band: the left edge of the right floats that overlap it, but at most the right edge of
     * the container.
     * <p>
     * 띠에서 비어 있는 공간의 오른쪽 한계를 반환한다: 띠와 겹치는 오른쪽 float 의 왼쪽 가장자리. 최대한 컨테이너의 오른쪽이다.
     *
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @param max  right edge of the container / 컨테이너의 오른쪽
     * @return the right limit / 오른쪽 한계
     */
    double rightEdge(double top, double span, double max) {
        double v = max;
        // 띠와 겹치는 오른쪽 float 중 가장 왼쪽 가장자리를 고른다.
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            if (!f.left && f.overlaps(top, span)) {
                v = Math.min(v, f.x);
            }
        }
        return v;
    }

    /**
     * Returns the left limit of the free space for a line in a band: like {@link #leftEdge}, but a left float that has a
     * <code>shape-outside</code> only takes the part of the band that its float area covers, up to the rightmost point of the area in
     * the band (CSS Shapes Level 1 section 2: the shape only affects the inline content that wraps around the float).
     * <p>
     * 줄을 위한 띠의 비어 있는 공간의 왼쪽 한계를 반환한다: {@link #leftEdge} 와 같지만 <code>shape-outside</code> 가 있는 왼쪽 float 는 띠에서 float 영역이 덮는
     * 부분만, 띠 안에서 영역의 가장 오른쪽 점까지만 차지한다(CSS Shapes Level 1 2 절: 모양은 float 를 감싸는 인라인 내용에만 영향을 준다).
     *
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @param min  left edge of the container / 컨테이너의 왼쪽
     * @return the left limit / 왼쪽 한계
     */
    double leftEdgeShaped(double top, double span, double min) {
        double v = min;
        // 띠와 겹치는 왼쪽 float 중 가장 오른쪽 가장자리를 고른다(모양이 있으면 영역의 가장자리).
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            if (f.left && f.overlaps(top, span)) {
                if (f.area == null) {
                    v = Math.max(v, f.x + f.w);
                } else {
                    Rectangle2D r = within(f, top, span);
                    if (r != null) {
                        v = Math.max(v, r.getMaxX());
                    }
                }
            }
        }
        return v;
    }

    /**
     * Returns the right limit of the free space for a line in a band, taking the shape of right floats into account (see
     * {@link #leftEdgeShaped}).
     * <p>
     * 줄을 위한 띠의 비어 있는 공간의 오른쪽 한계를 반환한다. 오른쪽 float 의 모양을 반영한다({@link #leftEdgeShaped} 참조).
     *
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @param max  right edge of the container / 컨테이너의 오른쪽
     * @return the right limit / 오른쪽 한계
     */
    double rightEdgeShaped(double top, double span, double max) {
        double v = max;
        // 띠와 겹치는 오른쪽 float 중 가장 왼쪽 가장자리를 고른다(모양이 있으면 영역의 가장자리).
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            if (!f.left && f.overlaps(top, span)) {
                if (f.area == null) {
                    v = Math.min(v, f.x);
                } else {
                    Rectangle2D r = within(f, top, span);
                    if (r != null) {
                        v = Math.min(v, r.getMinX());
                    }
                }
            }
        }
        return v;
    }

    /**
     * Returns the bounds of the part of the float area of a float that is inside a band.
     * <p>
     * float 의 float 영역 중 띠 안에 있는 부분의 경계를 반환한다.
     *
     * @param f    the float / float
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @return the bounds, or null if the area does not reach into the band / 경계. 영역이 띠 안에 들어오지 않으면 null
     */
    private static Rectangle2D within(Item f, double top, double span) {
        Area a = new Area(f.area);
        a.intersect(new Area(new Rectangle2D.Double(f.x - 1, top, f.w + 2, Math.max(span, EPS))));
        return a.isEmpty() ? null : a.getBounds2D();
    }

    /**
     * Returns the smallest bottom edge below a height among the floats that overlap a band, or infinity if none overlaps. A line that has no
     * room moves down to this height.
     * <p>
     * 띠와 겹치는 float 중 주어진 높이보다 아래에 있는 가장 작은 아래쪽 가장자리를 반환한다. 겹치는 것이 없으면 무한대. 들어갈 곳이 없는 줄은 이 높이로 내려간다.
     *
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @return the bottom edge or infinity / 아래쪽 가장자리 또는 무한대
     */
    double nextBottom(double top, double span) {
        double best = Double.POSITIVE_INFINITY;
        // 띠와 겹치는 float 의 아래쪽 가장자리 중 가장 작은 것을 고른다.
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            if (f.overlaps(top, span) && f.y + f.h > top + EPS) {
                best = Math.min(best, f.y + f.h);
            }
        }
        return best;
    }

    /**
     * Returns the highest top edge a new float may have: the largest top edge of the floats so far (CSS 2.1 9.5.1: the top of a float may
     * not be higher than the top of an earlier float).
     * <p>
     * 새 float 가 가질 수 있는 가장 높은 위쪽을 반환한다: 지금까지 float 의 위쪽 중 가장 큰 값(CSS 2.1 9.5.1: float 의 위쪽은 앞선 float 의 위쪽보다 높을 수 없다).
     *
     * @return the minimum top edge, or negative infinity if there are no floats / 위쪽의 최솟값. float 가 없으면 음의 무한대
     */
    double lastTop() {
        double v = Double.NEGATIVE_INFINITY;
        // float 의 위쪽 중 가장 아래에 있는 것을 고른다.
        for (int i = 0; i < items.size(); i++) {
            v = Math.max(v, items.get(i).y);
        }
        return v;
    }

    /**
     * Returns the height below which a box that clears the given sides may start: the bottom edge of the lowest float of those sides.
     * <p>
     * 주어진 방향을 clear 하는 상자가 시작할 수 있는 높이를 반환한다: 그 방향 float 중 가장 아래의 아래쪽 가장자리.
     *
     * @param sides 1 for left, 2 for right, 3 for both / 1 이면 왼쪽, 2 이면 오른쪽, 3 이면 양쪽
     * @return the height, or negative infinity if there is nothing to clear / 높이. 비울 것이 없으면 음의 무한대
     */
    double clearY(int sides) {
        double v = Double.NEGATIVE_INFINITY;
        // 해당 방향 float 의 아래쪽 가장자리 중 가장 큰 것을 고른다.
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            if (((f.left ? 1 : 2) & sides) != 0 && f.h > EPS) {
                v = Math.max(v, f.y + f.h);
            }
        }
        return v;
    }

    /**
     * Returns the bottom edge of the lowest float, or negative infinity if there is none.
     * <p>
     * 가장 아래 float 의 아래쪽 가장자리를 반환한다. 없으면 음의 무한대.
     *
     * @return the bottom edge / 아래쪽 가장자리
     */
    double maxBottom() {
        double v = Double.NEGATIVE_INFINITY;
        // 모든 float 의 아래쪽 가장자리 중 가장 큰 것을 고른다.
        for (int i = 0; i < items.size(); i++) {
            Item f = items.get(i);
            v = Math.max(v, f.y + f.h);
        }
        return v;
    }

    /**
     * Tells whether any float overlaps a band.
     * <p>
     * 띠와 겹치는 float 가 있는지 알려 준다.
     *
     * @param top  top of the band / 띠의 위쪽
     * @param span height of the band / 띠의 높이
     * @return true if some float is next to the band / 띠 옆에 float 가 있으면 true
     */
    boolean hasFloatIn(double top, double span) {
        // float 를 하나씩 보며 띠와 겹치는 것이 있는지 확인한다.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).overlaps(top, span)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the place of a new float without adding it (CSS 2.1 9.5.1): as high as possible at or below the minimum height, then as far to
     * the left (right) as possible. If the float does not fit beside the floats that are already there, it moves down to the bottom edge
     * of the lowest of them that is in its way, until it fits or no float is in the way (then it sticks out of a container that is too
     * narrow, on the side it floats to).
     * <p>
     * 새 float 를 더하지 않고 놓일 자리를 찾는다(CSS 2.1 9.5.1): 최소 높이 이상에서 가능한 한 높이, 그다음 가능한 한 왼쪽(오른쪽). 이미 있는 float 옆에 들어가지
     * 않으면 방해가 되는 float 중 가장 낮은 것의 아래쪽으로 내려가며, 들어가거나 방해하는 float 가 없어질 때까지 반복한다(그때는 너무 좁은 컨테이너라면 float 가
     * 향하는 쪽으로 튀어나온다).
     *
     * @param left true for a left float / 왼쪽 float 이면 true
     * @param w    width of the margin box / margin box 너비
     * @param h    height of the margin box / margin box 높이
     * @param minY the highest allowed top edge / 허용되는 가장 높은 위쪽
     * @param cl   left edge of the containing block / 포함 블록의 왼쪽
     * @param cr   right edge of the containing block / 포함 블록의 오른쪽
     * @return {x, y} of the top-left corner of the margin box / margin box 왼쪽 위의 {x, y}
     */
    double[] place(boolean left, double w, double h, double minY, double cl, double cr) {
        double y = Math.max(minY, lastTop());
        // 들어갈 자리가 나올 때까지 방해하는 float 의 아래쪽으로 내려간다(float 수만큼만 반복해 끝을 보장한다).
        for (int guard = 0; guard <= items.size() + 1; guard++) {
            double l = leftEdge(y, h, cl);
            double r = rightEdge(y, h, cr);
            if (r - l >= w - EPS) {
                return new double[] { left ? l : r - w, y };
            }
            double nb = nextBottom(y, h);
            if (Double.isInfinite(nb)) {
                break;
            }
            y = nb;
        }
        return new double[] { left ? cl : cr - w, y };
    }

    /**
     * Adds a float.
     * <p>
     * float 를 더한다.
     *
     * @param left true for a left float / 왼쪽 float 이면 true
     * @param x    left edge of the margin box / margin box 왼쪽
     * @param y    top edge / 위쪽
     * @param w    width / 너비
     * @param h    height / 높이
     */
    void add(boolean left, double x, double y, double w, double h) {
        items.add(new Item(left, x, y, w, h, null));
    }

    /**
     * Adds a float with a shape: lines (not boxes) wrap around the float area instead of the margin box (CSS Shapes Level 1).
     * <p>
     * 모양이 있는 float 를 더한다: 줄(상자가 아님)은 margin box 대신 float 영역을 따라 감싼다(CSS Shapes Level 1).
     *
     * @param left true for a left float / 왼쪽 float 이면 true
     * @param x    left edge of the margin box / margin box 왼쪽
     * @param y    top edge / 위쪽
     * @param w    width / 너비
     * @param h    height / 높이
     * @param area the float area in the coordinates of the context, or null / 문맥 좌표의 float 영역. 없으면 null
     */
    void add(boolean left, double x, double y, double w, double h, Area area) {
        items.add(new Item(left, x, y, w, h, area));
    }
}
