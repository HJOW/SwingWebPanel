package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * The boxes of a layout in painting order (plan item P10-01), recorded by a {@link PaintWalker} walk, with the clips that apply to every
 * entry. A hit test goes through the entries from the last to the first: the last entry that contains the point and lies inside all its
 * clips is what is on top at that point. Boxes with <code>visibility:hidden</code> or <code>pointer-events:none</code> are recorded
 * but never hit. Entries inside a fixed group are compared with the point in viewport coordinates.
 * <p>
 * 레이아웃의 상자를 그리기 순서로 기록한 것(계획 P10-01). {@link PaintWalker} 순회가 기록하며 항목마다 적용되는 자르기를 가진다. 히트 테스트는 항목을 마지막부터
 * 처음으로 훑는다: 점을 포함하고 모든 자르기 안에 있는 마지막 항목이 그 점에서 맨 위에 있는 것이다. <code>visibility:hidden</code> 이나
 * <code>pointer-events:none</code> 인 상자는 기록되지만 히트되지 않는다. fixed 그룹 안의 항목은 점을 뷰포트 좌표로 비교한다.
 */
final class PaintSequence implements PaintWalker.Visitor {

    /**
     * One clip rectangle; the clips that apply to an entry form a chain.
     * <p>
     * 자르기 사각형 하나. 항목에 적용되는 자르기는 사슬을 이룬다.
     */
    private static final class Clip {
        /** Left edge. 왼쪽. */
        final double x;
        /** Top edge. 위쪽. */
        final double y;
        /** Width of the clip. 자르기의 너비. */
        final double w;
        /** Height of the clip. 자르기의 높이. */
        final double h;
        /** The clip around this one or null. 이것을 둘러싼 자르기 또는 null. */
        final Clip outer;
        /** The transform chain that applied when the clip began: its rectangle is in those local coordinates (P10-06). 자르기가 시작될 때 적용되던 변환 사슬: 사각형은 그 지역 좌표이다(P10-06). */
        final Xf xf;
        /** The shape of a <code>clip-path</code> clip (the rectangle is its bounds), or null for a plain rectangle (P10-08). <code>clip-path</code> 자르기의 모양(사각형은 그 경계). 보통 사각형이면 null(P10-08). */
        final java.awt.Shape shape;

        /**
         * Creates a clip.
         * <p>
         * 자르기를 만든다.
         *
         * @param x     left / 왼쪽
         * @param y     top / 위쪽
         * @param w     width / 너비
         * @param h     height / 높이
         * @param outer the surrounding clip or null / 둘러싼 자르기 또는 null
         * @param xf    the transform chain at the start of the clip / 자르기 시작 때의 변환 사슬
         * @param shape the shape or null / 모양 또는 null
         */
        Clip(double x, double y, double w, double h, Clip outer, Xf xf, java.awt.Shape shape) {
            this.shape = shape;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.outer = outer;
            this.xf = xf;
        }

        /**
         * Tells whether the point is inside this clip and all clips around it.
         * <p>
         * 점이 이 자르기와 그것을 둘러싼 모든 자르기 안에 있는지 알려 준다.
         *
         * @param px   point x / 점 x
         * @param py   point y / 점 y
         * @param view the viewport in the coordinates of the point (for sticky groups) / 점의 좌표로 나타낸 뷰포트(sticky 그룹용)
         * @return true if inside all / 모두의 안이면 true
         */
        boolean contains(double px, double py, java.awt.geom.Rectangle2D view) {
            // 사슬을 따라 모든 자르기 사각형을 확인한다(사각형이 속한 변환의 지역 좌표로 점을 옮겨서 비교한다).
            for (Clip c = this; c != null; c = c.outer) {
                double[] p = c.xf == null ? new double[] { px, py } : Xf.map(px, py, c.xf, view);
                if (p == null || p[0] < c.x || p[0] >= c.x + c.w || p[1] < c.y || p[1] >= c.y + c.h || (c.shape != null && !c.shape.contains(p[0], p[1]))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * One transform of the transform chain that applies to an entry or a clip (P10-06): the inverse maps a point of the surrounding
     * coordinates to the local coordinates; the chain goes from the innermost transform to the outermost one.
     * <p>
     * 항목이나 자르기에 적용되는 변환 사슬의 변환 하나(P10-06): 역변환은 둘러싼 좌표의 점을 지역 좌표로 옮기며, 사슬은 가장 안쪽 변환에서 가장 바깥쪽으로 이어진다.
     */
    private static final class Xf {
        /** The inverse of the transform, or null if the transform cannot be inverted (the content is never hit) or the link is a sticky group. 변환의 역. 역변환할 수 없으면 null(내용은 히트되지 않는다). sticky 그룹이면 null. */
        final java.awt.geom.AffineTransform inverse;
        /** The constraint of a sticky group, or null for a transform. sticky 그룹의 제약. 변환이면 null. */
        final StickyConstraint sticky;
        /** True if this link or one around it is a sticky group (then the viewport in local coordinates is needed). 이 고리나 바깥 고리가 sticky 그룹이면 true(그러면 지역 좌표의 뷰포트가 필요하다). */
        final boolean hasSticky;
        /** The transform around this one or null. 이것을 둘러싼 변환 또는 null. */
        final Xf outer;

        /**
         * Creates a link of the chain.
         * <p>
         * 사슬의 고리를 만든다.
         *
         * @param t     the transform / 변환
         * @param outer the surrounding link or null / 둘러싼 고리 또는 null
         */
        Xf(java.awt.geom.AffineTransform t, Xf outer) {
            java.awt.geom.AffineTransform inv = null;
            try {
                inv = t.createInverse();
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                inv = null;
            }
            this.inverse = inv;
            this.sticky = null;
            this.hasSticky = outer != null && outer.hasSticky;
            this.outer = outer;
        }

        /**
         * Creates a link that is a sticky group.
         * <p>
         * sticky 그룹인 고리를 만든다.
         *
         * @param sc    the constraint / 제약
         * @param outer the surrounding link or null / 둘러싼 고리 또는 null
         */
        Xf(StickyConstraint sc, Xf outer) {
            this.inverse = null;
            this.sticky = sc;
            this.hasSticky = true;
            this.outer = outer;
        }

        /**
         * Maps a point of the outermost coordinates to the local coordinates of the innermost link: the outermost link first. A transform link
         * applies its inverse; a sticky link subtracts its shift, which depends on the viewport in the coordinates of that link (the viewport
         * is mapped along with the point).
         * <p>
         * 가장 바깥 좌표의 점을 가장 안쪽 고리의 지역 좌표로 옮긴다: 가장 바깥쪽 고리가 먼저이다. 변환 고리는 역변환을 적용하고, sticky 고리는 이동량을 뺀다. 이동량은 그 고리의
         * 좌표로 나타낸 뷰포트에 달려 있으므로 뷰포트도 점과 함께 옮긴다.
         *
         * @param x     x in the outermost coordinates / 가장 바깥 좌표의 x
         * @param y     y / y
         * @param chain the innermost link or null / 가장 안쪽 고리 또는 null
         * @param view  the viewport in the outermost coordinates / 가장 바깥 좌표의 뷰포트
         * @return {x, y} in the local coordinates, or null if a transform cannot be inverted / 지역 좌표의 {x, y}. 역변환할 수 없는 변환이 있으면 null
         */
        static double[] map(double x, double y, Xf chain, java.awt.geom.Rectangle2D view) {
            if (chain == null) {
                return new double[] { x, y };
            }
            int n = 0;
            // 안쪽에서 바깥쪽으로 고리 수를 센 뒤 바깥쪽부터 차례로 적용하도록 배열에 뒤집어 담는다.
            for (Xf c = chain; c != null; c = c.outer) {
                n++;
            }
            Xf[] links = new Xf[n];
            int k = n;
            // 안쪽 고리부터 거슬러 올라가며 배열의 뒤에서부터 채운다.
            for (Xf c = chain; c != null; c = c.outer) {
                links[--k] = c;
            }
            double[] p = { x, y };
            java.awt.geom.Rectangle2D v = view;
            // 바깥쪽 고리부터 점과 뷰포트를 그 고리의 지역 좌표로 옮긴다.
            for (int i = 0; i < n; i++) {
                Xf l = links[i];
                if (l.sticky != null) {
                    double[] d = l.sticky.shift(v);
                    p[0] -= d[0];
                    p[1] -= d[1];
                    v = new java.awt.geom.Rectangle2D.Double(v.getX() - d[0], v.getY() - d[1], v.getWidth(), v.getHeight());
                } else {
                    if (l.inverse == null) {
                        return null;
                    }
                    l.inverse.transform(p, 0, p, 0, 1);
                    // 뒤에 sticky 고리가 있을 때만 뷰포트가 쓰이므로 있을 때만 계산한다.
                    if (chain.hasSticky) {
                        v = l.inverse.createTransformedShape(v).getBounds2D();
                    }
                }
            }
            return p;
        }
    }

    /** The transform chain of every entry (null: none). 항목마다 변환 사슬(null: 없음). */
    private final List<Xf> xfOf = new ArrayList<Xf>();
    /** The transform chain that applies now. 지금 적용되는 변환 사슬. */
    private Xf currentXf;
    /** The recorded boxes. 기록한 상자. */
    private final List<LayoutBox> boxes = new ArrayList<LayoutBox>();
    /** The clip of every entry (null: none). 항목마다 자르기(null: 없음). */
    private final List<Clip> clipOf = new ArrayList<Clip>();
    /** Whether the entry is inside a fixed group. 항목이 fixed 그룹 안인지. */
    private final List<Boolean> fixedOf = new ArrayList<Boolean>();
    /** The clip that applies now. 지금 적용되는 자르기. */
    private Clip current;
    /** Nesting depth of fixed groups. fixed 그룹의 중첩 깊이. */
    private int fixedDepth;

    /**
     * Records the sequence of a box tree.
     * <p>
     * 상자 트리의 순서를 기록한다.
     *
     * @param root the root box / 루트 상자
     * @return the sequence / 순서
     */
    static PaintSequence of(LayoutBox root) {
        PaintSequence s = new PaintSequence();
        PaintWalker.walk(root, s);
        return s;
    }

    /**
     * Returns the number of entries.
     * <p>
     * 항목 수를 반환한다.
     *
     * @return the count / 개수
     */
    int size() {
        return boxes.size();
    }

    /**
     * Adds an entry for a box.
     * <p>
     * 상자의 항목을 더한다.
     *
     * @param b the box / 상자
     */
    private void add(LayoutBox b) {
        boxes.add(b);
        clipOf.add(current);
        xfOf.add(currentXf);
        fixedOf.add(fixedDepth > 0 ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Starts a sticky group: the entries and clips recorded until the matching end are shifted by the amount that the constraint gives for
     * the viewport at the time of the hit test.
     * <p>
     * sticky 그룹을 시작한다: 짝이 되는 끝까지 기록하는 항목과 자르기는 히트 테스트 때의 뷰포트에 대해 제약이 정하는 만큼 옮겨진다.
     *
     * @param box        the sticky box / sticky 상자
     * @param constraint its constraint / 제약
     */
    public void beginSticky(LayoutBox box, StickyConstraint constraint) {
        currentXf = new Xf(constraint, currentXf);
    }

    /**
     * Ends a sticky group.
     * <p>
     * sticky 그룹을 끝낸다.
     *
     * @param box the sticky box / sticky 상자
     */
    public void endSticky(LayoutBox box) {
        currentXf = currentXf == null ? null : currentXf.outer;
    }

    /**
     * Starts a transform group: the entries and clips recorded until the matching end are in the local coordinates of the transform.
     * <p>
     * 변환 그룹을 시작한다: 짝이 되는 끝까지 기록하는 항목과 자르기는 변환의 지역 좌표로 나타낸다.
     *
     * @param box       the transformed box / 변환된 상자
     * @param transform its transform / 변환
     */
    public void beginTransform(LayoutBox box, java.awt.geom.AffineTransform transform) {
        currentXf = new Xf(transform, currentXf);
    }

    /**
     * Ends a transform group.
     * <p>
     * 변환 그룹을 끝낸다.
     *
     * @param box the transformed box / 변환된 상자
     */
    public void endTransform(LayoutBox box) {
        currentXf = currentXf == null ? null : currentXf.outer;
    }

    /**
     * Starts the clip of a <code>clip-path</code>: what is outside of the shape is not hit (CSS Masking 1 section 5.1).
     * <p>
     * <code>clip-path</code> 의 자르기를 시작한다: 모양 밖은 히트되지 않는다(CSS Masking 1 5.1 절).
     *
     * @param box   the box / 상자
     * @param shape the shape / 모양
     */
    public void beginClipPath(LayoutBox box, java.awt.Shape shape) {
        java.awt.geom.Rectangle2D r = shape.getBounds2D();
        current = new Clip(r.getX(), r.getY(), r.getWidth(), r.getHeight(), current, currentXf, shape);
    }

    /**
     * Ends the clip of a <code>clip-path</code>.
     * <p>
     * <code>clip-path</code> 의 자르기를 끝낸다.
     *
     * @param box the box / 상자
     */
    public void endClipPath(LayoutBox box) {
        current = current == null ? null : current.outer;
    }

    /**
     * Records a decoration entry.
     * <p>
     * 장식 항목을 기록한다.
     *
     * @param box the box / 상자
     */
    public void decorate(LayoutBox box) {
        add(box);
    }

    /**
     * Records a content entry (replaced boxes were recorded by their decoration already).
     * <p>
     * 내용 항목을 기록한다(치환 상자는 장식으로 이미 기록되었다).
     *
     * @param box the box / 상자
     */
    public void content(LayoutBox box) {
        if (box.type != LayoutBox.Type.REPLACED) {
            add(box);
        }
    }

    /**
     * Starts a clip.
     * <p>
     * 자르기를 시작한다.
     *
     * @param owner the clipping box / 자르는 상자
     * @param x     left / 왼쪽
     * @param y     top / 위쪽
     * @param w     width / 너비
     * @param h     height / 높이
     */
    public void beginClip(LayoutBox owner, double x, double y, double w, double h) {
        current = new Clip(x, y, w, h, current, currentXf, null);
    }

    /**
     * Ends a clip.
     * <p>
     * 자르기를 끝낸다.
     *
     * @param owner the clipping box / 자르는 상자
     */
    public void endClip(LayoutBox owner) {
        current = current == null ? null : current.outer;
    }

    /**
     * Ignored: a filter has no effect on hit testing (CSS Filter Effects 1 section 5.1), and a mask does not change what a click finds either.
     * <p>
     * 무시한다: 필터는 히트 테스트에 영향이 없고(CSS Filter Effects 1 5.1 절) 마스크도 클릭이 찾는 것을 바꾸지 않는다.
     *
     * @param box the box / 상자
     */
    public void beginEffects(LayoutBox box) {
        // 효과는 그려지는 모양만 바꾸므로 기록할 것이 없다.
    }

    /**
     * Ignored, see {@link #beginEffects}.
     * <p>
     * 무시한다. {@link #beginEffects} 참조.
     *
     * @param box the box / 상자
     */
    public void endEffects(LayoutBox box) {
        // 시작과 마찬가지로 할 일이 없다.
    }

    /**
     * Ignored: opacity does not change what a click finds.
     * <p>
     * 무시한다: 불투명도는 클릭이 찾는 것을 바꾸지 않는다.
     *
     * @param box     the box / 상자
     * @param opacity the opacity / 불투명도
     */
    public void beginOpacity(LayoutBox box, double opacity) {
        // 불투명도가 0 이어도 요소는 이벤트를 받으므로 기록할 것이 없다.
    }

    /**
     * Ignored, see {@link #beginOpacity}.
     * <p>
     * 무시한다. {@link #beginOpacity} 참조.
     *
     * @param box the box / 상자
     */
    public void endOpacity(LayoutBox box) {
        // 시작과 마찬가지로 할 일이 없다.
    }

    /**
     * Starts a fixed group.
     * <p>
     * fixed 그룹을 시작한다.
     *
     * @param box the box / 상자
     */
    public void beginFixed(LayoutBox box) {
        fixedDepth++;
    }

    /**
     * Ends a fixed group.
     * <p>
     * fixed 그룹을 끝낸다.
     *
     * @param box the box / 상자
     */
    public void endFixed(LayoutBox box) {
        fixedDepth--;
    }

    /**
     * Ignored: the segments of collapsed borders are not boxes of their own; the boxes below them are what a click finds.
     * <p>
     * 무시한다: 합쳐진 border 선분은 상자가 아니며 클릭이 찾는 것은 그 아래의 상자이다.
     *
     * @param table the table box / 표 상자
     */
    public void tableBorders(LayoutBox table) {
        // 선분은 히트 대상이 아니므로 기록할 것이 없다.
    }

    /**
     * Ignored: column rules are not boxes of their own; the boxes below them are what a click finds.
     * <p>
     * 무시한다: 열 구분선은 상자가 아니며 클릭이 찾는 것은 그 아래의 상자이다.
     *
     * @param container the multi-column container / 다단 컨테이너
     */
    public void columnRules(LayoutBox container) {
        // 구분선은 히트 대상이 아니므로 기록할 것이 없다.
    }

    /**
     * Ignored: the resize handle is a user interface part of the box; the box itself is already recorded and the handle is found by
     * {@link Resizing#find}.
     * <p>
     * 무시한다: 크기 조절 핸들은 상자의 사용자 인터페이스 부분이다. 상자는 이미 기록되었고 핸들은 {@link Resizing#find} 가 찾는다.
     *
     * @param box the resizable box / 크기를 바꿀 수 있는 상자
     */
    public void resizer(LayoutBox box) {
        // 핸들은 히트 대상이 아니므로 기록할 것이 없다.
    }

    /**
     * Tells whether a box can be hit: it is not hidden and does not ignore pointer events.
     * <p>
     * 상자가 히트될 수 있는지 알려 준다: 숨겨지지 않았고 포인터 이벤트를 무시하지 않는다.
     *
     * @param b the box / 상자
     * @return true if it can be hit / 히트될 수 있으면 true
     */
    private static boolean hittable(LayoutBox b) {
        if (b.style == null) {
            return true;
        }
        if (b.style.isKeyword(Prop.VISIBILITY, "hidden") || b.style.isKeyword(Prop.VISIBILITY, "collapse")) {
            return false;
        }
        return !b.style.isKeyword(Prop.POINTER_EVENTS, "none");
    }

    /**
     * Finds the topmost box at a point.
     * <p>
     * 점의 가장 위 상자를 찾는다.
     *
     * @param x       x in document coordinates / 문서 좌표 x
     * @param y       y in document coordinates / 문서 좌표 y
     * @param scrollX horizontal scroll offset, applied to fixed boxes / 가로 스크롤 오프셋. fixed 상자에 적용된다
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @param vw      viewport width, for the sticky boxes / sticky 상자를 위한 뷰포트 너비
     * @param vh      viewport height / 뷰포트 높이
     * @return the box or null / 상자 또는 null
     */
    LayoutBox hit(double x, double y, double scrollX, double scrollY, double vw, double vh) {
        // 나중에 그려진 항목부터 점을 포함하고 모든 자르기 안에 있는 것을 찾는다.
        for (int i = boxes.size() - 1; i >= 0; i--) {
            LayoutBox b = boxes.get(i);
            boolean fixed = fixedOf.get(i).booleanValue();
            double px = fixed ? x - scrollX : x;
            double py = fixed ? y - scrollY : y;
            Clip c = clipOf.get(i);
            Xf xf = xfOf.get(i);
            // sticky 그룹 안이면 이동량을 구할 뷰포트를 점과 같은 좌표로 준비한다(fixed 그룹 안에서는 뷰포트가 원점에 있다).
            java.awt.geom.Rectangle2D view = xf != null && xf.hasSticky ? new java.awt.geom.Rectangle2D.Double(fixed ? 0 : scrollX, fixed ? 0 : scrollY, vw, vh) : null;
            // 항목이 변환 안에 있으면 점을 역변환으로 항목의 지역 좌표에 옮겨 비교한다(변환된 요소는 그려진 자리에서 클릭된다).
            double[] lp = xf == null ? null : Xf.map(px, py, xf, view);
            if (xf != null && lp == null) {
                continue;
            }
            double lx = lp == null ? px : lp[0];
            double ly = lp == null ? py : lp[1];
            if (!b.contains(lx, ly)) {
                continue;
            }
            if (c != null && !c.contains(px, py, view)) {
                continue;
            }
            if (hittable(b)) {
                return b;
            }
        }
        return null;
    }
}
