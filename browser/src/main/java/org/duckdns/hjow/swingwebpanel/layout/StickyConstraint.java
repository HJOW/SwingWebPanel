package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.geom.Rectangle2D;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * The geometry that decides how far a <code>position:sticky</code> box is shifted while the page scrolls (CSS Positioned Layout Module
 * Level 3, section 3.4 "Sticky positioning", https://www.w3.org/TR/css-position-3/#sticky-pos). The layout puts a sticky box where it would be in the normal flow;
 * what is shifted is the painting and the hit test: for each side with an <code>top</code>/<code>right</code>/<code>bottom</code>/<code>left</code>
 * inset that is not <code>auto</code>, a box whose border edge would be outside the sticky view rectangle (the viewport inset by that value) is moved
 * inward of that edge as far as it can while its margin box (the "position box") stays inside the content box of its containing block. When the box is
 * too large for both insets of one axis, the start edge wins (top; left, or right in a right-to-left box). The shift is computed from the
 * viewport rectangle, which is given in the coordinates of the box (so an ancestor transform is taken into account by the caller).
 * <p>
 * Documented limits: the nearest scroll container is always the viewport. The engine has no scrolling <code>overflow</code> boxes (those clip
 * only; see EXC-13), so a box inside an <code>overflow</code> box other than <code>visible</code> is never shifted (its scroll container does not
 * scroll); inline (non-atomic) boxes are not shifted; the geometry queries of the DOM (<code>getBoundingClientRect</code>) and the highlight of a
 * text selection report the unshifted position.
 * <p>
 * 페이지가 스크롤될 때 <code>position:sticky</code> 상자를 얼마나 옮길지 정하는 기하(CSS Positioned Layout Module Level 3 3.4 절 "Sticky positioning").
 * 레이아웃은 sticky 상자를 정상 흐름에서의 자리에 두고, 옮기는 것은 그리기와 히트 테스트이다: <code>top</code>/<code>right</code>/<code>bottom</code>/<code>left</code>
 * 가 <code>auto</code> 가 아닌 변마다, border 가장자리가 sticky 뷰 사각형(그 값만큼 줄인 뷰포트) 밖에 나가게 될 상자를 그 가장자리 안쪽으로, 마진 상자("위치 상자")가
 * 포함 블록의 콘텐츠 상자 안에 머무는 한도까지 옮긴다. 한 축의 두 값을 모두 만족시킬 수 없을 만큼 상자가 크면 시작 가장자리(위쪽, 왼쪽. 오른쪽에서 왼쪽 상자는 오른쪽)가
 * 이긴다. 옮김은 상자의 좌표로 준 뷰포트 사각형에서 계산한다(조상의 변환은 호출자가 반영한다).
 * <p>
 * 문서화된 한계: 가장 가까운 스크롤 컨테이너는 항상 뷰포트이다. 이 엔진에는 스크롤하는 <code>overflow</code> 상자가 없으므로(자르기만 함. EXC-13 참조)
 * <code>visible</code> 이 아닌 <code>overflow</code> 상자 안의 상자는 옮기지 않는다(그 스크롤 컨테이너는 스크롤되지 않는다). 인라인(원자적이 아닌) 상자는 옮기지 않으며,
 * DOM 의 기하 조회(<code>getBoundingClientRect</code>)와 텍스트 선택 강조는 옮기기 전 위치를 알려 준다.
 */
public final class StickyConstraint {

    /** The insets top, right, bottom, left in px (NaN for auto). top, right, bottom, left 오프셋(px). auto 이면 NaN. */
    private final double[] insets;
    /** The margin box {x, y, w, h} of the box in its coordinates. 상자의 좌표로 나타낸 마진 상자 {x, y, w, h}. */
    private final double[] position;
    /** The content box {x, y, w, h} of the containing block. 포함 블록의 콘텐츠 상자 {x, y, w, h}. */
    private final double[] container;
    /** The border box {x, y, w, h} of the box. 상자의 border 상자 {x, y, w, h}. */
    private final double[] border;
    /** True if the box is right-to-left. 상자가 오른쪽에서 왼쪽이면 true. */
    private final boolean rtl;

    /**
     * Creates a constraint.
     * <p>
     * 제약을 만든다.
     *
     * @param insets    top, right, bottom, left (NaN for auto) / 위, 오른쪽, 아래, 왼쪽(auto 는 NaN)
     * @param position  the margin box {x, y, w, h} / 마진 상자
     * @param border    the border box {x, y, w, h} / border 상자
     * @param container the content box of the containing block {x, y, w, h} / 포함 블록의 콘텐츠 상자
     * @param rtl       true for a right-to-left box / 오른쪽에서 왼쪽이면 true
     */
    private StickyConstraint(double[] insets, double[] position, double[] border, double[] container, boolean rtl) {
        this.insets = insets;
        this.position = position;
        this.border = border;
        this.container = container;
        this.rtl = rtl;
    }

    /**
     * Builds the constraint of a box whose final geometry is known, or returns null if the box is not shifted: it is not sticky, has no inset
     * that is not <code>auto</code>, is an inline box, or lies inside a box that clips (its <code>overflow</code> is not <code>visible</code>; the
     * root element and the <code>body</code> element are skipped, as their overflow belongs to the viewport).
     * <p>
     * 최종 기하를 아는 상자의 제약을 만들며, 상자를 옮기지 않으면 null 을 반환한다: sticky 가 아니거나, <code>auto</code> 가 아닌 오프셋이 없거나, 인라인 상자이거나,
     * 자르는(<code>overflow</code> 가 <code>visible</code> 이 아닌) 상자 안에 있는 경우(루트 요소와 <code>body</code> 요소는 overflow 가 뷰포트의 것이므로 건너뛴다).
     *
     * @param b  the box / 상자
     * @param vw the width of the viewport used for percentage insets / 백분율 오프셋의 기준인 뷰포트 너비
     * @param vh the height of the viewport / 뷰포트 높이
     * @return the constraint or null / 제약 또는 null
     */
    static StickyConstraint of(LayoutBox b, double vw, double vh) {
        if (b.posKind != LayoutBox.POS_STICKY || b.style == null || b.type == LayoutBox.Type.INLINE || b.type == LayoutBox.Type.TEXT
                || b.type == LayoutBox.Type.LINE) {
            return null;
        }
        ComputedStyle s = b.style;
        double top = s.resolve(Prop.TOP, vh);
        double right = s.resolve(Prop.RIGHT, vw);
        double bottom = s.resolve(Prop.BOTTOM, vh);
        double left = s.resolve(Prop.LEFT, vw);
        if (Double.isNaN(top) && Double.isNaN(right) && Double.isNaN(bottom) && Double.isNaN(left)) {
            return null;
        }
        LayoutBox cb = containerOf(b);
        if (cb == null) {
            return null;
        }
        // 조상 중 overflow 로 자르는 상자가 있으면 그 상자가 스크롤 컨테이너인데 스크롤되지 않으므로 옮기지 않는다.
        for (LayoutBox p = b.parent; p != null; p = p.parent) {
            if (p.style != null && p.node != null && !isViewportOverflow(p) && hasOverflow(p.style)) {
                return null;
            }
        }
        double[] border = { b.x, b.y, b.width, b.height };
        // 위치 상자는 마진 상자이되 지정한 마진만 쓰고 auto 는 0 으로 본다(블록의 사용된 오른쪽 마진은 남는 공간을 채우므로 그대로 쓰면 좁은 블록이 움직일 수 없다. 브라우저도 지정한 값을 쓴다).
        double base = cb.getContentWidth();
        double ml = nz(s.resolve(Prop.MARGIN_LEFT, base));
        double mr = nz(s.resolve(Prop.MARGIN_RIGHT, base));
        double mt = nz(s.resolve(Prop.MARGIN_TOP, base));
        double mb = nz(s.resolve(Prop.MARGIN_BOTTOM, base));
        double[] position = { b.x - ml, b.y - mt, b.width + ml + mr, b.height + mt + mb };
        double[] container = { cb.getContentX(), cb.getContentY(), cb.getContentWidth(), cb.getContentHeight() };
        boolean rtl = "rtl".equals(Css.kw(s, Prop.DIRECTION));
        return new StickyConstraint(new double[] { top, right, bottom, left }, position, border, container, rtl);
    }

    /**
     * Returns 0 for NaN (an <code>auto</code> margin) and the value itself otherwise.
     * <p>
     * NaN(<code>auto</code> 마진)이면 0, 아니면 값 그대로 반환한다.
     *
     * @param v the value / 값
     * @return the value without NaN / NaN 이 없는 값
     */
    private static double nz(double v) {
        return Double.isNaN(v) ? 0 : v;
    }

    /**
     * Tells whether a box is the root element or the body element, whose <code>overflow</code> is that of the viewport.
     * <p>
     * 상자가 루트 요소나 body 요소여서 <code>overflow</code> 가 뷰포트의 것인지 알려 준다.
     *
     * @param p the box / 상자
     * @return true for the root or the body / 루트나 body 이면 true
     */
    private static boolean isViewportOverflow(LayoutBox p) {
        if (p.parent == null) {
            return true;
        }
        return p.parent.parent == null && p.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element && "body".equals(((org.duckdns.hjow.swingwebpanel.dom.Element) p.node).getLocalName());
    }

    /**
     * Tells whether a style has an <code>overflow</code> other than <code>visible</code> (the paint containment that also clips does not
     * make a scroll container).
     * <p>
     * 스타일에 <code>visible</code> 이 아닌 <code>overflow</code> 가 있는지 알려 준다(자르기만 하는 그리기 격리는 스크롤 컨테이너를 만들지 않는다).
     *
     * @param s the style / 스타일
     * @return true if the overflow is not visible / overflow 가 visible 이 아니면 true
     */
    private static boolean hasOverflow(ComputedStyle s) {
        String x = Css.kw(s, Prop.OVERFLOW_X);
        String y = Css.kw(s, Prop.OVERFLOW_Y);
        return !("visible".equals(x) || x.length() == 0) || !("visible".equals(y) || y.length() == 0);
    }

    /**
     * Finds the box whose content box limits the sticky box: the nearest ancestor that is neither a line, an inline fragment nor an anonymous
     * box (an anonymous box is not the containing block that CSS names).
     * <p>
     * 이 sticky 상자를 제한하는 콘텐츠 상자를 가진 상자를 찾는다: 줄, 인라인 조각, 익명 상자가 아닌 가장 가까운 조상(익명 상자는 CSS 가 말하는 포함 블록이 아니다).
     *
     * @param b the sticky box / sticky 상자
     * @return the box or null / 상자 또는 null
     */
    private static LayoutBox containerOf(LayoutBox b) {
        LayoutBox p = b.parent;
        // 줄, 인라인 조각, 익명 블록을 건너뛰어 요소의 블록 컨테이너를 찾는다.
        while (p != null && (p.type == LayoutBox.Type.LINE || p.type == LayoutBox.Type.INLINE || p.type == LayoutBox.Type.ANONYMOUS_BLOCK || p.node == null)) {
            p = p.parent;
        }
        return p;
    }

    /**
     * Computes the shift of the box for a viewport rectangle given in the coordinates of the box.
     * <p>
     * 상자의 좌표로 준 뷰포트 사각형에 대한 상자의 이동량을 계산한다.
     *
     * @param viewport the viewport in the coordinates of the box / 상자의 좌표로 나타낸 뷰포트
     * @return {dx, dy} / {dx, dy}
     */
    public double[] shift(Rectangle2D viewport) {
        double dx = axisShift(position[0], position[0] + position[2], border[0], border[0] + border[2], container[0], container[0] + container[2], viewport.getMinX(),
                viewport.getMaxX(), insets[3], insets[1], !rtl);
        double dy = axisShift(position[1], position[1] + position[3], border[1], border[1] + border[3], container[1], container[1] + container[3], viewport.getMinY(),
                viewport.getMaxY(), insets[0], insets[2], true);
        return new double[] { dx, dy };
    }

    /**
     * Computes the shift along one axis (CSS Positioned Layout 3 section 3.4): the constraint of the edge that wins is applied last.
     * <p>
     * 한 축의 이동량을 계산한다(CSS Positioned Layout 3 3.4 절): 이기는 가장자리의 제약을 마지막에 적용한다.
     *
     * @param ps        start of the margin box / 마진 상자의 시작
     * @param pe        end of the margin box / 마진 상자의 끝
     * @param bs        start of the border box / border 상자의 시작
     * @param be        end of the border box / border 상자의 끝
     * @param cs        start of the content box of the containing block / 포함 블록 콘텐츠 상자의 시작
     * @param ce        end of the content box of the containing block / 포함 블록 콘텐츠 상자의 끝
     * @param vs        start of the viewport / 뷰포트의 시작
     * @param ve        end of the viewport / 뷰포트의 끝
     * @param insetS    the inset of the start side (NaN for auto) / 시작 쪽 오프셋(auto 는 NaN)
     * @param insetE    the inset of the end side (NaN for auto) / 끝 쪽 오프셋(auto 는 NaN)
     * @param startWins true if the start edge wins when both cannot be met / 둘을 모두 만족시킬 수 없을 때 시작 가장자리가 이기면 true
     * @return the shift / 이동량
     */
    public static double axisShift(double ps, double pe, double bs, double be, double cs, double ce, double vs, double ve, double insetS, double insetE, boolean startWins) {
        double shift = 0;
        // 이기지 못하는 쪽 제약을 먼저, 이기는 쪽 제약을 나중에 적용한다.
        for (int pass = 0; pass < 2; pass++) {
            boolean doStart = startWins ? pass == 1 : pass == 0;
            if (doStart) {
                if (!Double.isNaN(insetS)) {
                    double deficit = (vs + insetS) - (bs + shift);
                    // 시작 가장자리가 뷰 사각형 밖이면 안쪽으로 옮기되 마진 상자가 포함 블록 끝을 넘지 않게 한다.
                    if (deficit > 0) {
                        shift = Math.min(shift + deficit, Math.max(shift, ce - pe));
                    }
                }
            } else if (!Double.isNaN(insetE)) {
                double excess = (be + shift) - (ve - insetE);
                // 끝 가장자리가 뷰 사각형 밖이면 안쪽으로 옮기되 마진 상자가 포함 블록 시작을 넘지 않게 한다.
                if (excess > 0) {
                    shift = Math.max(shift - excess, Math.min(shift, cs - ps));
                }
            }
        }
        return shift;
    }
}
