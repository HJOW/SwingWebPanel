package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * Layout of absolutely positioned boxes (plan item P10-01): <code>position:absolute</code> and <code>position:fixed</code> boxes are laid
 * out when their containing block is finished (CSS 2.1 10.3.7 and 10.6.4 for width and height and the offsets
 * <code>top</code>/<code>right</code>/<code>bottom</code>/<code>left</code>, 10.3.8 and 10.6.5 for replaced elements). The containing
 * block is the padding box of the nearest positioned ancestor box, else the initial containing block (the viewport rectangle at the
 * origin of the page, also for fixed boxes). An offset that is <code>auto</code> takes the static position that the inline layout recorded
 * ({@link LayoutBox#staticX}, {@link LayoutBox#staticY}): the place where the box would have been in the normal flow. Auto margins center a
 * box whose offsets and size are all given. The result is stored like every other box position, relative to the parent box in the tree
 * ({@link LayoutBox#lx}, {@link LayoutBox#ly}), although the parent is not the containing block, so the conversion to document
 * coordinates stays the same.
 * <p>
 * 절대 배치 상자의 레이아웃(계획 P10-01): <code>position:absolute</code> 와 <code>position:fixed</code> 상자는 포함 블록이 끝날 때 배치한다(너비와 높이,
 * <code>top</code>/<code>right</code>/<code>bottom</code>/<code>left</code> 오프셋은 CSS 2.1 10.3.7 과 10.6.4, 치환 요소는 10.3.8 과 10.6.5). 포함 블록은
 * 가장 가까운 위치 지정 조상 상자의 padding box 이고 없으면 초기 포함 블록(페이지 원점의 뷰포트 사각형이며 fixed 상자도 같다)이다. <code>auto</code> 인 오프셋은
 * 인라인 레이아웃이 기록한 정적 위치({@link LayoutBox#staticX}, {@link LayoutBox#staticY}), 즉 정상 흐름에서 상자가 있었을 자리를 쓴다. 오프셋과 크기가 모두
 * 주어진 상자는 auto margin 으로 가운데 정렬된다. 결과는 다른 상자의 위치와 같이 트리의 부모 상자 기준({@link LayoutBox#lx}, {@link LayoutBox#ly})으로
 * 저장하므로 부모가 포함 블록이 아니어도 문서 좌표로 바꾸는 과정은 같다.
 */
final class AbsoluteLayouter {

    /** The block layouter that provides sizes and the layout of the content. 크기와 내용 배치를 제공하는 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the layouter.
     * <p>
     * 레이아웃을 만든다.
     *
     * @param bl the block layouter / 블록 레이아웃
     */
    AbsoluteLayouter(BlockLayouter bl) {
        this.bl = bl;
    }

    /**
     * Lays out the absolutely positioned boxes of a positioned box whose own layout is finished (its width and height are known).
     * <p>
     * 자신의 배치가 끝난(너비와 높이를 아는) 위치 지정 상자의 절대 배치 상자를 배치한다.
     *
     * @param cb the containing block box / 포함 블록 상자
     */
    void layoutContaining(LayoutBox cb) {
        double padW = Math.max(0, cb.width - cb.borderLeft - cb.borderRight);
        double padH = Math.max(0, cb.height - cb.borderTop - cb.borderBottom);
        layoutList(cb, cb.absList, cb.borderLeft, cb.borderTop, padW, padH);
    }

    /**
     * Lays out the absolutely positioned boxes whose containing block is the initial containing block (or the viewport). The positions
     * of all the boxes between the root and the parents of these boxes must be final.
     * <p>
     * 포함 블록이 초기 포함 블록(또는 뷰포트)인 절대 배치 상자를 배치한다. 루트와 이 상자들의 부모 사이 모든 상자의 위치가 확정되어 있어야 한다.
     *
     * @param list the boxes / 상자
     * @param vw   width of the viewport / 뷰포트 너비
     * @param vh   height of the viewport / 뷰포트 높이
     */
    void layoutInitial(List<LayoutBox> list, double vw, double vh) {
        layoutList(null, list, 0, 0, vw, vh);
    }

    /**
     * Lays out a list of boxes that share a containing block.
     * <p>
     * 포함 블록을 공유하는 상자의 목록을 배치한다.
     *
     * @param cb   the containing block box or null for the initial containing block / 포함 블록 상자 또는 초기 포함 블록이면 null
     * @param list the boxes / 상자
     * @param padX x of the padding box in the border box of cb / cb 의 border box 안 padding box 의 x
     * @param padY y of the padding box / padding box 의 y
     * @param padW width of the padding box / padding box 의 너비
     * @param padH height of the padding box / padding box 의 높이
     */
    private void layoutList(LayoutBox cb, List<LayoutBox> list, double padX, double padY, double padW, double padH) {
        // 포함 블록이 같은 절대 배치 상자를 소스 순서로 하나씩 배치한다.
        for (int i = 0; i < list.size(); i++) {
            LayoutBox a = list.get(i);
            if (a.parent == null) {
                continue;
            }
            // 정적 위치는 부모 기준이므로 포함 블록 기준으로 바꾼다.
            double ox = 0;
            double oy = 0;
            // 부모에서 포함 블록(제외)까지 올라가며 부모 border box 의 위치를 더한다.
            for (LayoutBox p = a.parent; p != null && p != cb; p = p.parent) {
                ox += p.lx;
                oy += p.ly;
            }
            double bx = padX;
            double by = padY;
            double bw = padW;
            double bh = padH;
            if (a.gridCb != null) {
                // 그리드 컨테이너가 포함 블록이면 그리드 배치 속성이 정한 그리드 영역이 포함 블록이다(CSS Grid 9.1). padding 가장자리인 쪽은 그대로 둔다.
                double x0 = Double.isNaN(a.gridCb[0]) ? padX : a.gridCb[0];
                double x1 = Double.isNaN(a.gridCb[1]) ? padX + padW : a.gridCb[1];
                double y0 = Double.isNaN(a.gridCb[2]) ? padY : a.gridCb[2];
                double y1 = Double.isNaN(a.gridCb[3]) ? padY + padH : a.gridCb[3];
                bx = x0;
                by = y0;
                bw = Math.max(0, x1 - x0);
                bh = Math.max(0, y1 - y0);
            }
            ComputedStyle base = a.style;
            Trial best = trial(a, base, cb, list, i, bx, by, bw, bh, ox, oy, true);
            // 상자가 inset-modified containing block 을 넘치면 position-try-fallbacks 의 대안을 차례로 시도해 넘치지 않는 첫 대안을 쓴다(CSS Anchor Positioning 1 6.5).
            List<PositionTry.Option> options = PositionTry.options(base);
            if (best.overflow && !options.isEmpty()) {
                Trial won = fallback(a, base, options, cb, list, i, bx, by, bw, bh, ox, oy);
                // 맞는 대안이 없으면 처음 스타일을 다시 배치해 상자의 상태를 되돌린다.
                best = won != null ? won : trial(a, base, cb, list, i, bx, by, bw, bh, ox, oy, true);
            }
            applyVisibility(a, cb, list, i, best);
            a.lx = bx + best.pos[0] - ox;
            a.ly = by + best.pos[1] - oy;
        }
    }

    /** The tolerance of the overflow test in pixels (the layout works with floating point numbers). 넘침 판정의 허용 오차(픽셀). 레이아웃은 부동소수점으로 계산한다. */
    private static final double OVERFLOW_EPSILON = 0.05;

    /**
     * The result of laying out a box with one style: the style that the layout used, the position, the inset-modified containing block and whether the margin box
     * overflows it.
     * <p>
     * 상자를 한 스타일로 배치한 결과: 레이아웃이 쓴 스타일, 위치, inset-modified containing block 과 margin box 가 그것을 넘치는지 여부.
     */
    private static final class Trial {
        /** The style before the position area and the anchor functions were resolved (the option applied to the base style). 위치 영역과 anchor 함수를 풀기 전의 스타일(기본 스타일에 대안을 적용한 것). */
        ComputedStyle input;
        /** The position {x, y} of the border box relative to the padding box, or null if the box was not laid out. padding box 기준 border box 의 위치 {x, y}. 배치하지 않았으면 null. */
        double[] pos;
        /** The inset-modified containing block {left, right, top, bottom} relative to the padding box. padding box 기준 inset-modified containing block {왼쪽, 오른쪽, 위쪽, 아래쪽}. */
        double[] imcb;
        /** True if the margin box is not inside the inset-modified containing block. margin box 가 inset-modified containing block 안에 없으면 true. */
        boolean overflow;
        /** True if the inset-modified containing block has a negative size (it is corrected to zero size). inset-modified containing block 의 크기가 음수이면 true(0 크기로 고친다). */
        boolean negative;
    }

    /**
     * Lays out a box with a style (section 6.5.2 "apply a position option"): the position area and <code>anchor-center</code> become the region of the box, the anchor
     * functions become lengths, the box is laid out and its position corrected for the alignment, and the inset-modified containing block is determined; with
     * <code>layout</code> false only the style and the containing block are determined (for <code>position-try-order</code>).
     * <p>
     * 상자를 스타일로 배치한다(6.5.2 절 "위치 대안 적용"): 위치 영역과 <code>anchor-center</code> 는 상자의 영역이 되고, anchor 함수는 길이가 되며, 상자를 배치하고 정렬에 맞게
     * 위치를 바로잡고, inset-modified containing block 을 정한다. <code>layout</code> 이 false 이면 스타일과 포함 블록만 정한다(<code>position-try-order</code> 용).
     *
     * @param a      the box / 상자
     * @param input  the style to lay out with / 배치에 쓸 스타일
     * @param cb     the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list   the boxes of the containing block / 포함 블록의 상자
     * @param i      the index of the box / 상자의 번호
     * @param bx     left of the padding box / padding box 의 왼쪽
     * @param by     top of the padding box / 위쪽
     * @param bw     width of the padding box / 너비
     * @param bh     height of the padding box / 높이
     * @param ox     x offset from the parent to the containing block / 부모에서 포함 블록까지 x 오프셋
     * @param oy     y offset / y 오프셋
     * @param layout true to lay the box out / 상자를 배치하려면 true
     * @return the result / 결과
     */
    private Trial trial(LayoutBox a, ComputedStyle input, LayoutBox cb, List<LayoutBox> list, int i, double bx, double by, double bw, double bh, double ox, double oy,
            boolean layout) {
        Trial t = new Trial();
        t.input = input;
        a.style = input;
        // position-area 가 있으면 앵커 상자와 포함 블록이 만드는 격자의 영역에 상자를 놓는다(CSS Anchor Positioning 1 3.1).
        double[] padBox = { bx, by, bw, bh };
        AnchorPositioning.Area area = AnchorPositioning.hasArea(a.style) ? AnchorPositioning.area(a, cb, list, i, bx, by, bw, bh) : null;
        // anchor-center 는 그 축의 정렬을 앵커 가운데로 바꾼다(4.2 절).
        area = AnchorPositioning.withAnchorCenter(area, a, cb, list, i, bx, by, bw, bh);
        if (area != null) {
            a.style = area.style(a.style, padBox);
        }
        // anchor() 와 anchor-size() 는 앵커 상자를 알아야 풀리므로 배치 직전에 길이로 바꾼다(CSS Anchor Positioning 1).
        if (AnchorPositioning.uses(a.style)) {
            a.style = AnchorPositioning.resolve(a, cb, list, i, bx, by, bw, bh);
        }
        t.imcb = imcbOf(a, area, padBox, bw, bh);
        t.negative = t.imcb[1] < t.imcb[0] || t.imcb[3] < t.imcb[2];
        if (!layout) {
            return t;
        }
        double[] pos = layoutOne(a, bw, bh, ox + a.staticX - bx, oy + a.staticY - by);
        // 영역 안에서 정렬(끝, 가운데, 앵커 가운데)에 맞게 위치를 바로잡는다.
        if (area != null) {
            area.place(a, pos, padBox);
        }
        t.pos = pos;
        double x0 = pos[0] - a.marginLeft;
        double x1 = pos[0] + a.width + a.marginRight;
        double y0 = pos[1] - a.marginTop;
        double y1 = pos[1] + a.height + a.marginBottom;
        // margin box 가 inset-modified containing block 안에 완전히 들어 있는지 본다. 크기가 음수인 포함 블록은 넘침으로 센다.
        t.overflow = t.negative || x0 < t.imcb[0] - OVERFLOW_EPSILON || x1 > t.imcb[1] + OVERFLOW_EPSILON || y0 < t.imcb[2] - OVERFLOW_EPSILON
                || y1 > t.imcb[3] + OVERFLOW_EPSILON;
        return t;
    }

    /**
     * Returns the inset-modified containing block of a box laid out with its style in use (CSS Anchor Positioning 1 section 6.5, CSS Positioned Layout 3): the
     * padding box of the containing block, or the region of the position area, reduced by the insets that are not <code>auto</code>.
     * <p>
     * 쓰고 있는 스타일로 배치한 상자의 inset-modified containing block 을 반환한다(CSS Anchor Positioning 1 6.5 절, CSS Positioned Layout 3): 포함 블록의 padding box 또는 위치 영역의
     * 영역에서 <code>auto</code> 가 아닌 오프셋만큼 줄인 사각형.
     *
     * @param a    the box (its style is the style in use) / 상자(스타일은 쓰고 있는 스타일)
     * @param area the region of the box or null / 상자의 영역. 없으면 null
     * @param pad  the padding box {x, y, w, h} / padding box
     * @param bw   width of the padding box / 너비
     * @param bh   height of the padding box / 높이
     * @return {left, right, top, bottom} relative to the padding box / padding box 기준 {왼쪽, 오른쪽, 위쪽, 아래쪽}
     */
    private double[] imcbOf(LayoutBox a, AnchorPositioning.Area area, double[] pad, double bw, double bh) {
        ComputedStyle s = a.style;
        int fr = BlockLayouter.frameOf(a);
        double left = s.resolve(Css.side(Css.OFFSET, fr, 3), bw);
        double right = s.resolve(Css.side(Css.OFFSET, fr, 1), bw);
        double top = s.resolve(Css.side(Css.OFFSET, fr, 0), bh);
        double bottom = s.resolve(Css.side(Css.OFFSET, fr, 2), bh);
        double[] r = { Double.isNaN(left) ? 0 : left, bw - (Double.isNaN(right) ? 0 : right), Double.isNaN(top) ? 0 : top, bh - (Double.isNaN(bottom) ? 0 : bottom) };
        // 영역이 적용되는 축은 영역에서 오프셋만큼 줄인 사각형이다.
        if (area != null && area.active[0]) {
            r[0] = area.region[0] + area.insets[3] - pad[0];
            r[1] = area.region[1] - area.insets[1] - pad[0];
        }
        if (area != null && area.active[1]) {
            r[2] = area.region[2] + area.insets[0] - pad[1];
            r[3] = area.region[3] - area.insets[2] - pad[1];
        }
        return r;
    }

    /**
     * Determines the position fallback styles (section 6.5): the position options are the base style and the entries of <code>position-try-fallbacks</code>
     * (an entry that names a missing <code>@position-try</code> rule has no effect), sorted by <code>position-try-order</code>; each, except the base style that is known
     * to overflow, is laid out and the first whose margin box fits in its inset-modified containing block (which must not be negative) is returned.
     * <p>
     * 위치 대안 스타일을 정한다(6.5 절): 위치 대안은 기본 스타일과 <code>position-try-fallbacks</code> 의 항목이며(없는 <code>@position-try</code> 규칙을 가리키는 항목은 효과가 없다)
     * <code>position-try-order</code> 로 정렬한다. 넘치는 것을 이미 아는 기본 스타일을 뺀 각각을 배치해 margin box 가 inset-modified containing block(음수이면 안 된다)에
     * 들어가는 첫 것을 반환한다.
     *
     * @param a       the box / 상자
     * @param base    the base style / 기본 스타일
     * @param options the entries of <code>position-try-fallbacks</code> / <code>position-try-fallbacks</code> 의 항목
     * @param cb      the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list    the boxes of the containing block / 포함 블록의 상자
     * @param i       the index of the box / 상자의 번호
     * @param bx      left of the padding box / padding box 의 왼쪽
     * @param by      top of the padding box / 위쪽
     * @param bw      width of the padding box / 너비
     * @param bh      height of the padding box / 높이
     * @param ox      x offset from the parent to the containing block / 부모에서 포함 블록까지 x 오프셋
     * @param oy      y offset / y 오프셋
     * @return the result of the first option that fits (the box is laid out with it), or null / 맞는 첫 대안의 결과(상자는 그것으로 배치되어 있다). 없으면 null
     */
    private Trial fallback(LayoutBox a, ComputedStyle base, List<PositionTry.Option> options, LayoutBox cb, List<LayoutBox> list, int i, double bx, double by, double bw,
            double bh, double ox, double oy) {
        ComputedStyle ref = cb != null && cb.style != null ? cb.style : base;
        String wm = Css.kw(ref, Prop.WRITING_MODE);
        String dir = Css.kw(ref, Prop.DIRECTION);
        List<ComputedStyle> styles = new java.util.ArrayList<ComputedStyle>();
        styles.add(base);
        // 항목마다 대안의 스타일을 만든다(@position-try 규칙이 없는 이름은 효과가 없으므로 건너뛴다).
        for (int k = 0; k < options.size(); k++) {
            PositionTry.Option o = options.get(k);
            java.util.Map<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue> rule = null;
            if (o.name != null) {
                rule = bl.positionTry != null && a.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element ? bl.positionTry.positionTryOption(o.name, (org.duckdns.hjow.swingwebpanel.dom.Element) a.node, base) : null;
                if (rule == null) {
                    continue;
                }
            }
            styles.add(PositionTry.apply(o, base, rule, wm, dir));
        }
        int n = styles.size();
        int[] order = new int[n];
        // 정렬하지 않은 순서가 기본이다.
        for (int k = 0; k < n; k++) {
            order[k] = k;
        }
        String orderKey = PositionTry.order(base);
        // position-try-order 가 있으면 대안마다 inset-modified containing block 의 크기를 재어 큰 것이 앞에 오도록 안정 정렬한다(6.2 절).
        if (!"normal".equals(orderKey) && n > 2) {
            boolean vertical = wm.startsWith("vertical") || wm.startsWith("sideways");
            double[] key = new double[n];
            // 대안마다 배치하지 않고 포함 블록만 정해 키를 구한다.
            for (int k = 0; k < n; k++) {
                Trial t = trial(a, styles.get(k), cb, list, i, bx, by, bw, bh, ox, oy, false);
                double w = Math.max(0, t.imcb[1] - t.imcb[0]);
                double h = Math.max(0, t.imcb[3] - t.imcb[2]);
                if ("most-width".equals(orderKey)) {
                    key[k] = w;
                } else if ("most-height".equals(orderKey)) {
                    key[k] = h;
                } else if ("most-block-size".equals(orderKey)) {
                    key[k] = vertical ? w : h;
                } else {
                    key[k] = vertical ? h : w;
                }
            }
            // 삽입 정렬: 앞의 항목이 같거나 크면 그대로 두어 안정적으로 정렬한다.
            for (int k = 1; k < n; k++) {
                int x = order[k];
                int j = k - 1;
                // 더 작은 키를 가진 앞 항목을 한 칸씩 뒤로 민다.
                while (j >= 0 && key[order[j]] < key[x]) {
                    order[j + 1] = order[j];
                    j--;
                }
                order[j + 1] = x;
            }
        }
        // 정렬한 차례로 대안을 배치해 넘치지 않는 첫 것을 쓴다(기본 스타일은 이미 넘친다).
        for (int k = 0; k < n; k++) {
            if (order[k] == 0) {
                continue;
            }
            Trial t = trial(a, styles.get(order[k]), cb, list, i, bx, by, bw, bh, ox, oy, true);
            if (!t.negative && !t.overflow) {
                return t;
            }
        }
        return null;
    }

    /**
     * Applies <code>position-visibility</code> (CSS Anchor Positioning 1 section 6.6): the box and its descendants are hidden if the box overflows (<code>no-overflow</code>),
     * if a required anchor is missing (<code>anchors-valid</code>), or if its default anchor box is invisible or clipped (<code>anchors-visible</code>, the initial value).
     * <p>
     * <code>position-visibility</code> 를 적용한다(CSS Anchor Positioning 1 6.6 절): 상자가 넘치거나(<code>no-overflow</code>), 필수 앵커가 없거나(<code>anchors-valid</code>), 기본 앵커
     * 상자가 보이지 않거나 잘렸으면(<code>anchors-visible</code>, 초기값) 상자와 그 자손을 숨긴다.
     *
     * @param a    the box (laid out with the result) / 상자(결과로 배치된 것)
     * @param cb   the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list the boxes of the containing block / 포함 블록의 상자
     * @param i    the index of the box / 상자의 번호
     * @param t    the result that is used / 쓰는 결과
     */
    private void applyVisibility(LayoutBox a, LayoutBox cb, List<LayoutBox> list, int i, Trial t) {
        int flags = PositionTry.visibility(a.style);
        if (flags == 0) {
            return;
        }
        boolean hide = (flags & PositionTry.VISIBLE_NO_OVERFLOW) != 0 && t.overflow;
        // 앵커를 쓰는 상자만 앵커에 관한 조건을 검사한다.
        boolean anchored = AnchorPositioning.uses(t.input) || AnchorPositioning.hasArea(t.input) || AnchorPositioning.hasDefaultAnchor(t.input);
        if (!hide && anchored && (flags & PositionTry.VISIBLE_ANCHORS_VALID) != 0) {
            hide = AnchorPositioning.anchorsMissing(t.input, a, cb, list, i);
        }
        if (!hide && anchored && (flags & PositionTry.VISIBLE_ANCHORS_VISIBLE) != 0) {
            hide = AnchorPositioning.anchorHidden(a, cb, list, i);
        }
        if (hide) {
            forceHide(a);
        }
    }

    /**
     * Hides a box and its descendants: the computed <code>visibility</code> of all of them is <code>hidden</code> and cannot be overridden by a descendant
     * (<code>force-hidden</code> of CSS Anchor Positioning 1 section 6.6).
     * <p>
     * 상자와 자손을 숨긴다: 모두의 <code>visibility</code> 계산 값이 <code>hidden</code> 이며 자손이 되돌릴 수 없다(CSS Anchor Positioning 1 6.6 절의 <code>force-hidden</code>).
     *
     * @param b the box / 상자
     */
    private static void forceHide(LayoutBox b) {
        b.positionHidden = true;
        if (b.style != null) {
            b.style = b.style.with(Prop.VISIBILITY, org.duckdns.hjow.swingwebpanel.css.values.CssKeyword.of("hidden"));
        }
        // 자손도 모두 숨긴다.
        for (int k = 0; k < b.children.size(); k++) {
            forceHide(b.children.get(k));
        }
    }

    /**
     * Lays out one absolutely positioned box: width and horizontal offsets, content, height and vertical offsets.
     * <p>
     * 절대 배치 상자 하나를 배치한다: 너비와 가로 오프셋, 내용, 높이와 세로 오프셋.
     *
     * @param a   the box / 상자
     * @param cbW width of the containing block's padding box / 포함 블록 padding box 의 너비
     * @param cbH height of the containing block's padding box / 포함 블록 padding box 의 높이
     * @param sx  static left position relative to the padding box / padding box 기준 정적 위치 왼쪽
     * @param sy  static top position relative to the padding box / padding box 기준 정적 위치 위쪽
     * @return {x, y} of the border box relative to the padding box / padding box 기준 border box 의 {x, y}
     */
    private double[] layoutOne(LayoutBox a, double cbW, double cbH, double sx, double sy) {
        if (a.type == LayoutBox.Type.TABLE) {
            return layoutTable(a, cbW, cbH, sx, sy);
        }
        int auto = bl.edges(a, cbW);
        ComputedStyle s = a.style;
        boolean replaced = a.type == LayoutBox.Type.REPLACED;
        int fr = BlockLayouter.frameOf(a);
        // 위치 오프셋도 논리적 변에 해당하는 물리적 속성을 프레임에 따라 고른다.
        double left = s.resolve(Css.side(Css.OFFSET, fr, 3), cbW);
        double right = s.resolve(Css.side(Css.OFFSET, fr, 1), cbW);
        double top = s.resolve(Css.side(Css.OFFSET, fr, 0), cbH);
        double bottom = s.resolve(Css.side(Css.OFFSET, fr, 2), cbH);
        double[] rsz = replaced ? bl.replacedSize(a, cbW, cbH) : null;
        double wSpec = replaced ? rsz[0] : bl.specWidth(a, cbW);
        String wKeyword = replaced || fr != Css.FRAME_H ? null : intrinsicKeyword(s.get(Prop.WIDTH));
        if (wKeyword != null) {
            // fit-content, min-content, max-content 너비는 오프셋과 margin 을 뺀 공간에서 내용으로 정한다(CSS Box Sizing 3 3 절). 그러면 auto margin 이 남은 공간을 나눈다.
            double avail = cbW - (Double.isNaN(left) ? 0 : left) - (Double.isNaN(right) ? 0 : right) - ((auto & 1) != 0 ? 0 : a.marginLeft)
                    - ((auto & 2) != 0 ? 0 : a.marginRight) - hx0(a);
            wSpec = "min-content".equals(wKeyword) ? bl.contentMin(a) : "max-content".equals(wKeyword) ? bl.contentMax(a) : shrinkToFit(a, avail);
        }
        double[] hs = solveHorizontal(a, cbW, left, right, wSpec, (auto & 1) != 0, (auto & 2) != 0, sx, replaced);
        if (Double.isNaN(left) && Double.isNaN(right) && a.staticAlignX != 0 && !Double.isNaN(a.staticW)) {
            // 정적 위치를 플렉스 컨테이너의 정렬로 옮긴다: 정적 위치 사각형 안에서 가운데나 끝에 놓는다.
            double outer = hs[0] + hx0(a) + hs[2] + hs[3];
            double free = a.staticW - outer;
            hs[1] = sx + (a.staticAlignX == 1 ? free / 2 : free);
        }
        a.marginLeft = hs[2];
        a.marginRight = hs[3];
        double vx = BlockLayouter.vExtras(a);
        double hSpec = replaced ? rsz[1] : bl.specHeight(a, cbH);
        a.forcedH = Double.NaN;
        // fit-content 같은 내용 높이는 top 과 bottom 이 있어도 늘어나지 않고 auto margin 이 남은 공간을 나눈다.
        boolean hIntrinsic = !replaced && fr == Css.FRAME_H && intrinsicKeyword(s.get(Prop.HEIGHT)) != null;
        if (!replaced && !hIntrinsic && Double.isNaN(hSpec) && !Double.isNaN(top) && !Double.isNaN(bottom)) {
            // 높이가 auto 이고 top 과 bottom 이 모두 있으면 높이는 남은 공간을 채운다(CSS 2.1 10.6.4 의 6 번).
            a.forcedH = Math.max(0, cbH - top - bottom - a.marginTop - a.marginBottom - vx);
        }
        bl.finish(a, hs[0], cbH, rsz);
        double h = a.height;
        double mt = a.marginTop;
        double mb = a.marginBottom;
        boolean mtAuto = s.isAuto(Css.side(Css.MARGIN, fr, 0));
        boolean mbAuto = s.isAuto(Css.side(Css.MARGIN, fr, 2));
        double y;
        if (!Double.isNaN(top) && !Double.isNaN(bottom) && (replaced || hIntrinsic || !Double.isNaN(hSpec))) {
            // top, height, bottom 이 모두 주어지면 auto margin 이 남은 공간을 나눈다(위쪽을 무시하지 않고 bottom 을 무시한다).
            double free = cbH - top - bottom - h;
            if (mtAuto && mbAuto) {
                mt = free / 2;
                mb = free / 2;
            } else if (mtAuto) {
                mt = free - mb;
            } else if (mbAuto) {
                mb = free - mt;
            }
            y = top + mt;
        } else if (!Double.isNaN(top)) {
            y = top + mt;
        } else if (!Double.isNaN(bottom)) {
            y = cbH - bottom - mb - h;
        } else {
            // top 과 bottom 이 모두 auto 이면 정적 위치를 쓴다(플렉스 컨테이너의 정렬이 있으면 정적 위치 사각형 안에서 정렬).
            y = sy + mt;
            if (a.staticAlignY != 0 && !Double.isNaN(a.staticH)) {
                double free = a.staticH - (mt + h + mb);
                y += a.staticAlignY == 1 ? free / 2 : free;
            }
        }
        a.marginTop = mt;
        a.marginBottom = mb;
        a.forcedH = Double.NaN;
        return new double[] { hs[1] + a.marginLeft, y };
    }

    /**
     * Returns the horizontal padding plus border of a box.
     * <p>
     * 상자의 가로 padding 과 border 의 합을 반환한다.
     *
     * @param a the box / 상자
     * @return the sum / 합
     */
    private static double hx0(LayoutBox a) {
        return BlockLayouter.hExtras(a);
    }

    /**
     * Lays out an absolutely positioned table: the table decides its own width (specified or shrink-to-fit) and height, and the offsets
     * only place it (<code>left</code> before <code>right</code>, <code>top</code> before <code>bottom</code>, else the static position).
     * <p>
     * 절대 배치 표를 배치한다: 표가 자신의 너비(지정 또는 shrink-to-fit)와 높이를 정하고 오프셋은 위치만 정한다(<code>left</code> 가 <code>right</code> 보다,
     * <code>top</code> 이 <code>bottom</code> 보다 우선하고 없으면 정적 위치).
     *
     * @param a   the table box / 표 상자
     * @param cbW width of the containing block's padding box / 포함 블록 padding box 의 너비
     * @param cbH height of the containing block's padding box / 포함 블록 padding box 의 높이
     * @param sx  static left position / 정적 위치 왼쪽
     * @param sy  static top position / 정적 위치 위쪽
     * @return {x, y} of the border box relative to the padding box / padding box 기준 border box 의 {x, y}
     */
    private double[] layoutTable(LayoutBox a, double cbW, double cbH, double sx, double sy) {
        bl.tables.layout(a, cbW, true);
        ComputedStyle s = a.style;
        double left = s.resolve(Prop.LEFT, cbW);
        double right = s.resolve(Prop.RIGHT, cbW);
        double top = s.resolve(Prop.TOP, cbH);
        double bottom = s.resolve(Prop.BOTTOM, cbH);
        double x = !Double.isNaN(left) ? left + a.marginLeft : (!Double.isNaN(right) ? cbW - right - a.marginRight - a.width : sx + a.marginLeft);
        double y = !Double.isNaN(top) ? top + a.marginTop : (!Double.isNaN(bottom) ? cbH - bottom - a.marginBottom - a.height : sy + a.marginTop);
        return new double[] { x, y };
    }

    /**
     * Resolves the horizontal constraint equation of an absolutely positioned box (CSS 2.1 10.3.7, the left-to-right direction): the
     * seven combinations of <code>auto</code> values of <code>left</code>, <code>width</code> and <code>right</code>, the shrink-to-fit
     * width, the auto margins and the over-constrained case; the result is limited by <code>min-width</code> and <code>max-width</code> by
     * resolving the equation a second time with the limit as the width.
     * <p>
     * 절대 배치 상자의 가로 제약 방정식을 푼다(CSS 2.1 10.3.7, 왼쪽에서 오른쪽): <code>left</code>, <code>width</code>, <code>right</code> 의 <code>auto</code>
     * 일곱 가지 조합, shrink-to-fit 너비, auto margin, 제약이 넘치는 경우. <code>min-width</code> 와 <code>max-width</code> 는 한계를 너비로 삼아 방정식을 한 번 더
     * 풀어 적용한다.
     *
     * @param a        the box / 상자
     * @param cbW      containing block width / 포함 블록 너비
     * @param left     <code>left</code> or NaN / <code>left</code> 또는 NaN
     * @param right    <code>right</code> or NaN / <code>right</code> 또는 NaN
     * @param w        content width or NaN for auto / content 너비. auto 이면 NaN
     * @param mlAuto   true if the left margin is auto / 왼쪽 margin 이 auto 이면 true
     * @param mrAuto   true if the right margin is auto / 오른쪽 margin 이 auto 이면 true
     * @param sx       static left position / 정적 위치 왼쪽
     * @param replaced true for replaced elements (their width is never auto) / 치환 요소이면 true(너비가 auto 인 경우 없음)
     * @return {content width, left of the margin box, left margin, right margin} / {content 너비, margin box 왼쪽, 왼쪽 margin, 오른쪽 margin}
     */
    private double[] solveHorizontal(LayoutBox a, double cbW, double left, double right, double w, boolean mlAuto, boolean mrAuto,
            double sx, boolean replaced) {
        double hx = BlockLayouter.hExtras(a);
        double ml = mlAuto ? 0 : a.marginLeft;
        double mr = mrAuto ? 0 : a.marginRight;
        double minW = replaced ? 0 : bl.minWidth(a, cbW);
        double maxW = replaced ? Double.POSITIVE_INFINITY : bl.maxWidth(a, cbW);
        double width = w;
        double[] result = null;
        // 너비가 한계를 넘으면 한계를 너비로 삼아 한 번 더 푼다.
        for (int pass = 0; pass < 2; pass++) {
            double l = left;
            double r = right;
            double cw = width;
            double lm = ml;
            double rm = mr;
            boolean wAuto = Double.isNaN(cw);
            if (Double.isNaN(l) && Double.isNaN(r) && wAuto) {
                // 셋 다 auto 이면 left 는 정적 위치이다.
                l = sx;
            }
            if (Double.isNaN(l) && wAuto) {
                cw = shrinkToFit(a, cbW - r - lm - rm - hx);
                l = cbW - r - cw - lm - rm - hx;
            } else if (Double.isNaN(l) && Double.isNaN(r)) {
                l = sx;
            } else if (wAuto && Double.isNaN(r)) {
                cw = shrinkToFit(a, cbW - l - lm - rm - hx);
            } else if (Double.isNaN(l)) {
                l = cbW - r - cw - lm - rm - hx;
            } else if (wAuto) {
                cw = Math.max(0, cbW - l - r - lm - rm - hx);
            } else if (!Double.isNaN(r)) {
                // 셋 다 주어졌다: auto margin 이 남은 공간을 나누고, 그렇지 않으면 제약이 넘친 것이므로 right 를 무시한다.
                double free = cbW - l - r - cw - hx;
                if (mlAuto && mrAuto) {
                    if (free >= 0) {
                        lm = free / 2;
                        rm = free / 2;
                    } else {
                        lm = 0;
                        rm = free;
                    }
                } else if (mlAuto) {
                    lm = free - rm;
                } else if (mrAuto) {
                    rm = free - lm;
                }
            }
            result = new double[] { cw, l, lm, rm };
            if (pass == 0 && cw > maxW) {
                width = maxW;
            } else if (pass == 0 && cw < minW) {
                width = minW;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Returns the intrinsic size keyword of a <code>width</code> or <code>height</code> value (<code>fit-content</code>, <code>min-content</code>,
     * <code>max-content</code>), or null for any other value.
     * <p>
     * <code>width</code> 나 <code>height</code> 값의 내재 크기 키워드(<code>fit-content</code>, <code>min-content</code>, <code>max-content</code>)를 반환한다. 다른 값이면 null.
     *
     * @param v the value / 값
     * @return the keyword or null / 키워드 또는 null
     */
    private static String intrinsicKeyword(org.duckdns.hjow.swingwebpanel.css.values.CssValue v) {
        if (v == null) {
            return null;
        }
        if (v.isKeyword("fit-content") || v.isKeyword("-moz-fit-content") || v.isKeyword("-webkit-fit-content")) {
            return "fit-content";
        }
        if (v.isKeyword("min-content")) {
            return "min-content";
        }
        return v.isKeyword("max-content") ? "max-content" : null;
    }

    /**
     * Returns the shrink-to-fit content width: the available width limited by the min-content and max-content widths.
     * <p>
     * shrink-to-fit content 너비를 반환한다: 쓸 수 있는 너비를 min-content 와 max-content 너비로 제한한 값.
     *
     * @param a     the box (its edges are resolved) / 상자(가장자리가 풀려 있음)
     * @param avail the available width / 쓸 수 있는 너비
     * @return the content width / content 너비
     */
    private double shrinkToFit(LayoutBox a, double avail) {
        return Math.min(Math.max(bl.contentMin(a), Math.max(0, avail)), bl.contentMax(a));
    }
}
