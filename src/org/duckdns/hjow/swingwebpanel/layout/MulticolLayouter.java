package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Layout of multi-column containers (plan item P10-03, CSS Multi-column Layout Module Level 1, W3C CR 2024-05-16). The used column
 * count N and the column width W follow the formulas of section 3.4 (<code>column-count</code>, <code>column-width</code> and the
 * <code>column-gap</code>, <code>normal</code> being 1em). The content of the container is laid out once as one continuous flow of width
 * W by the ordinary block and inline layout; the flow is then cut into columns: the columns take as much of the flow as fits the column
 * height, cuts are made only at break opportunities (between lines of a block, honoring <code>orphans</code> and
 * <code>widows</code>, and between sibling blocks; margins at a cut are dropped) and a box that crosses a cut is split into fragments
 * (<code>box-decoration-break:slice</code>: the top edge decoration belongs to the first fragment, the bottom edge decoration to the
 * last). Lines and boxes that cannot be split (replaced elements, tables, floats, flex containers, boxes without children) are kept
 * whole in the column where they start and overflow the column bottom if they are too tall. With <code>column-fill:balance</code> (the
 * default) the height of the columns is the smallest one with which the content fits N columns, limited by the height of the container
 * or <code>max-height</code>; <code>column-fill:auto</code> fills the columns one after the other up to that height; content that does
 * not fit creates overflow columns after the last column. The columns become anonymous block children of the container (one per
 * column, all as high as the content box of the container), column rules are recorded for the renderer and
 * <code>column-span:all</code> children split the container into sections that {@link BoxBuilder} wraps into anonymous multi-column
 * boxes.
 * <p>
 * Not supported (documented limits): <code>break-before</code>, <code>break-after</code>, <code>break-inside</code> only when the CSS
 * engine provides the properties (see the progress log), tables and flex containers are not split between columns, floats are not
 * fragmented (a float stays whole in the column where it starts), an absolutely positioned child keeps the position it had in the
 * continuous flow relative to the first fragment of its parent, <code>column-width</code> combined with <code>column-count</code> in
 * intrinsic sizing is approximated.
 * <p>
 * 다단 컨테이너의 레이아웃(계획 P10-03, CSS Multi-column Layout Module Level 1, W3C CR 2024-05-16). 사용 열 수 N 과 열 너비 W 는 3.4 절의 식을 따른다
 * (<code>column-count</code>, <code>column-width</code>, <code>column-gap</code> 이며 <code>normal</code> 은 1em). 컨테이너의 내용은 너비 W 의 연속된 흐름 하나로
 * 보통의 블록/인라인 레이아웃이 한 번 배치하고, 그 흐름을 열로 자른다: 열은 열 높이에 들어가는 만큼의 흐름을 가져가고, 자르는 곳은 끊을 수 있는 자리(블록 안의 줄 사이,
 * 이때 <code>orphans</code>, <code>widows</code> 반영, 그리고 형제 블록 사이. 자르는 곳의 margin 은 버림)뿐이며, 자르는 곳을 가로지르는 상자는 조각으로 나눈다
 * (<code>box-decoration-break:slice</code>: 위쪽 장식은 첫 조각, 아래쪽 장식은 마지막 조각). 나눌 수 없는 줄과 상자(치환 요소, 표, float, 플렉스 컨테이너, 자식이 없는
 * 상자)는 시작한 열에 통째로 두며 너무 크면 열 아래로 넘친다. <code>column-fill:balance</code>(기본)이면 열 높이는 내용이 N 개 열에 들어가는 가장 작은 높이이고
 * 컨테이너 높이나 <code>max-height</code> 가 한계이다. <code>column-fill:auto</code> 는 그 높이까지 열을 차례로 채운다. 들어가지 않는 내용은 마지막 열 뒤에 넘침 열을 만든다.
 * 열은 컨테이너의 익명 블록 자식이 되고(열마다 하나, 모두 컨테이너 content box 의 높이), 열 구분선은 렌더러를 위해 기록하며 <code>column-span:all</code> 자식은 컨테이너를
 * 구간으로 나눈다({@link BoxBuilder} 가 익명 다단 상자로 감쌈).
 * <p>
 * 지원하지 않음(문서화된 한계): <code>break-before</code>, <code>break-after</code>, <code>break-inside</code> 는 CSS 엔진이 그 속성을 제공할 때만(진행 기록 참조), 표와
 * 플렉스 컨테이너는 열 사이에서 나누지 않고, float 는 조각내지 않으며(시작한 열에 통째로), 절대 배치 자식은 연속 흐름에서의 위치를 부모의 첫 조각 기준으로 유지하고,
 * 고유 크기에서 <code>column-width</code> 와 <code>column-count</code> 의 조합은 근사한다.
 */
final class MulticolLayouter {

    /** Tolerance when comparing positions. 위치 비교 허용 오차. */
    private static final double EPS = 0.01;
    /** The largest number of columns that is used (the property value is limited). 쓰는 열 수의 상한(속성 값을 제한한다). */
    private static final int MAX_COLUMNS = 1000;
    /** Number of halvings of the search for the balanced column height. 균형 잡힌 열 높이를 찾는 이분 탐색의 횟수. */
    private static final int BALANCE_STEPS = 40;

    /** The block layouter that lays out the flow. 흐름을 배치하는 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the multi-column layouter.
     * <p>
     * 다단 레이아웃을 만든다.
     *
     * @param bl the block layouter of this layout run / 이 레이아웃 실행의 블록 레이아웃
     */
    MulticolLayouter(BlockLayouter bl) {
        this.bl = bl;
    }

    /**
     * The fragments of one box (or of the root) in the columns while the flow is distributed.
     * <p>
     * 흐름을 열에 나누는 동안 한 상자(또는 루트)의 열별 조각.
     */
    private static final class Frames {
        /** The original box, null for the root. 원래 상자. 루트는 null. */
        final LayoutBox owner;
        /** The frames of the parent box, null for the root. 부모 상자의 조각 모음. 루트는 null. */
        final Frames up;
        /** The fragment per column (null until a unit is placed in it). 열별 조각(상자가 놓이기 전에는 null). */
        final LayoutBox[] f;
        /** Flow y of the top edge of each fragment. 조각마다 위쪽 가장자리의 흐름 y. */
        final double[] top;
        /** Largest bottom of the content placed in each fragment, relative to its top. 조각 안에 놓인 내용의 가장 큰 아래쪽(조각 위쪽 기준). */
        final double[] extent;
        /** The top edge decoration (border and padding) that a continuation fragment repeats with <code>box-decoration-break: clone</code>, by column: the content of the fragment starts that far below its top. <code>box-decoration-break: clone</code> 에서 이어지는 조각이 되풀이하는 위쪽 가장자리 장식(border 와 padding). 열별이며 조각의 내용은 위쪽에서 그만큼 아래에서 시작한다. */
        final double[] extra;

        /**
         * Creates the frames.
         * <p>
         * 조각 모음을 만든다.
         *
         * @param owner   the original box or null / 원래 상자 또는 null
         * @param up      the frames of the parent or null / 부모의 조각 모음 또는 null
         * @param columns the number of columns / 열 수
         */
        Frames(LayoutBox owner, Frames up, int columns) {
            this.owner = owner;
            this.up = up;
            this.f = new LayoutBox[columns];
            this.top = new double[columns];
            this.extent = new double[columns];
            this.extra = new double[columns];
        }
    }

    /**
     * Tells whether a box repeats its edge decoration at every fragment (<code>box-decoration-break: clone</code>, CSS Fragmentation Level 3 section 5.3).
     * <p>
     * 상자가 조각마다 가장자리 장식을 되풀이하는지 알려 준다(<code>box-decoration-break: clone</code>, CSS Fragmentation Level 3 5.3 절).
     *
     * @param k the box / 상자
     * @return true for clone / clone 이면 true
     */
    private static boolean isClone(LayoutBox k) {
        return k.style != null && "clone".equals(Css.kw(k.style, Prop.BOX_DECORATION_BREAK));
    }

    /** The state of the distribution of one container. 컨테이너 하나를 나누는 상태. */
    private static final class Cut {
        /** Start of each segment in flow y (the first is minus infinity). 구간마다 시작의 흐름 y(첫 구간은 음의 무한대). */
        double[] lo;
        /** End of each segment in flow y (the last is plus infinity). 구간마다 끝의 흐름 y(마지막 구간은 양의 무한대). */
        double[] end;
        /** First fragment of every box that was split, to repair the containing blocks of absolutely positioned boxes. 나뉜 상자의 첫 조각. 절대 배치 상자의 포함 블록을 고치는 데 쓴다. */
        final Map<LayoutBox, LayoutBox> first = new IdentityHashMap<LayoutBox, LayoutBox>();
        /** The absolutely positioned boxes that were moved. 옮긴 절대 배치 상자. */
        final List<LayoutBox> abs = new ArrayList<LayoutBox>();

        /**
         * Returns the index of the segment that holds a flow position.
         * <p>
         * 흐름 위치가 속한 구간의 번호를 반환한다.
         *
         * @param y flow y / 흐름 y
         * @return the index / 번호
         */
        int columnOf(double y) {
            int k = 0;
            // 시작이 y 이하인 마지막 구간을 찾는다.
            for (int i = 1; i < lo.length; i++) {
                if (lo[i] <= y + EPS) {
                    k = i;
                } else {
                    break;
                }
            }
            return k;
        }
    }

    /**
     * Returns the used <code>column-gap</code>: <code>normal</code> is 1em, a percentage refers to the width of the container.
     * <p>
     * 사용 <code>column-gap</code> 을 반환한다: <code>normal</code> 은 1em 이고 백분율은 컨테이너 너비를 참조한다.
     *
     * @param s    the style / 스타일
     * @param base the content width of the container / 컨테이너의 content 너비
     * @return the gap / gap
     */
    static double columnGap(ComputedStyle s, double base) {
        CssValue v = s.get(Prop.COLUMN_GAP);
        if (v == null || v.isKeyword("normal")) {
            double fs = s.fontSizePx();
            return Double.isNaN(fs) ? 0 : fs;
        }
        double r = s.resolve(Prop.COLUMN_GAP, base);
        return Double.isNaN(r) || r < 0 ? 0 : r;
    }

    /**
     * Computes the used number of columns, the column width and the gap (section 3.4 of the module).
     * <p>
     * 사용 열 수, 열 너비, gap 을 계산한다(모듈 3.4 절).
     *
     * @param s the style of the container / 컨테이너의 스타일
     * @param u the content width of the container / 컨테이너의 content 너비
     * @return {N, W, gap} / {N, W, gap}
     */
    static double[] used(ComputedStyle s, double u) {
        double gap = columnGap(s, u);
        double count = s.number(Prop.COLUMN_COUNT);
        double width = s.px(Prop.COLUMN_WIDTH);
        int n;
        if (Double.isNaN(width)) {
            n = Double.isNaN(count) ? 1 : (int) Math.min(MAX_COLUMNS, Math.max(1, count));
        } else {
            double wd = Math.max(width, 1);
            int fit = (int) Math.max(1, Math.min(MAX_COLUMNS, Math.floor((u + gap) / (wd + gap))));
            n = Double.isNaN(count) ? fit : (int) Math.min(Math.max(1, count), fit);
        }
        double w = Math.max(0, (u + gap) / n - gap);
        return new double[] { n, w, gap };
    }

    /**
     * Returns the min-content or max-content width of a multi-column container: with a <code>column-count</code> it is that number of
     * columns of the flow plus the gaps, with only a <code>column-width</code> the width is limited as for a single column (the
     * specification leaves the details to the sizing module).
     * <p>
     * 다단 컨테이너의 min-content 또는 max-content 너비를 반환한다: <code>column-count</code> 가 있으면 그 수만큼의 흐름 열과 gap 이고 <code>column-width</code> 만
     * 있으면 한 열처럼 제한한다(세부는 규격이 크기 모듈에 맡긴다).
     *
     * @param b   the container / 컨테이너
     * @param max true for max-content / max-content 이면 true
     * @return the width / 너비
     */
    double intrinsic(LayoutBox b, boolean max) {
        ComputedStyle s = b.style;
        double flow = bl.flowIntrinsic(b, max);
        double count = s.number(Prop.COLUMN_COUNT);
        double width = s.px(Prop.COLUMN_WIDTH);
        double gap = columnGap(s, Double.NaN);
        if (!Double.isNaN(count)) {
            double n = Math.min(MAX_COLUMNS, Math.max(1, count));
            double w = flow;
            if (!Double.isNaN(width)) {
                w = max ? Math.max(flow, width) : Math.max(flow, Math.min(width, flow));
            }
            return n * w + (n - 1) * gap;
        }
        if (!Double.isNaN(width)) {
            return max ? Math.max(flow, width) : Math.min(width, flow);
        }
        return flow;
    }

    /**
     * Lays out a multi-column container and returns the height of its content box.
     * <p>
     * 다단 컨테이너를 배치하고 content box 의 높이를 반환한다.
     *
     * @param c           the container / 컨테이너
     * @param cw          content width / content 너비
     * @param specH       specified content height or NaN / 지정된 content 높이 또는 NaN
     * @param minH        minimum content height / 최소 content 높이
     * @param maxH        maximum content height (infinity if none) / 최대 content 높이(없으면 무한대)
     * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
     * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
     * @return height of the content / content 높이
     */
    double layout(LayoutBox c, double cw, double specH, double minH, double maxH, double contentLeft, double contentTop) {
        ComputedStyle s = c.style;
        double[] u = used(s, cw);
        int n = (int) u[0];
        double w = u[1];
        double gap = u[2];
        double flowH = bl.layoutFlowContent(c, w);
        List<LayoutBox> flow = new ArrayList<LayoutBox>(c.children);
        c.columnSource = flow;
        double hMax = Double.isNaN(specH) ? maxH : Math.max(minH, Math.min(specH, maxH));
        List<double[]> cand = new ArrayList<double[]>();
        collect(c, 0, cand, false, 0, 0);
        sortCandidates(cand);
        String fill = Css.kw(s, Prop.COLUMN_FILL);
        double h;
        if ("auto".equals(fill)) {
            h = Double.isInfinite(hMax) ? Math.max(flowH, 0) : hMax;
        } else {
            h = balance(cand, flowH, n, hMax);
        }
        List<double[]> segs = fill(h, cand, flowH, 0);
        Cut cut = new Cut();
        int k = segs.size();
        cut.lo = new double[k];
        cut.end = new double[k];
        // 구간의 시작과 끝을 배열로 옮긴다(첫 구간의 시작과 마지막 구간의 끝은 무한대).
        for (int i = 0; i < k; i++) {
            cut.lo[i] = i == 0 ? Double.NEGATIVE_INFINITY : segs.get(i)[0];
            cut.end[i] = i == k - 1 ? Double.POSITIVE_INFINITY : segs.get(i)[1];
        }
        LayoutBox[] wrappers = new LayoutBox[k];
        Frames root = new Frames(null, null, k);
        double rtlSign = "rtl".equals(Css.kw(s, Prop.DIRECTION)) ? -1 : 1;
        // 열마다 익명 블록 열 상자를 만든다.
        for (int i = 0; i < k; i++) {
            LayoutBox wr = new LayoutBox(LayoutBox.Type.ANONYMOUS_BLOCK, null, s);
            wr.frame = c.frame;
            wr.parentFrame = c.frame;
            wr.width = w;
            double slot = rtlSign > 0 ? i * (w + gap) : (n - 1 - i) * (w + gap);
            wr.lx = contentLeft + slot;
            wr.ly = contentTop;
            wrappers[i] = wr;
            root.f[i] = wr;
            root.top[i] = i == 0 ? 0 : segs.get(i)[0];
        }
        distribute(flow, 0, root, cut);
        // 절대 배치 상자의 포함 블록이 나뉘었으면 첫 조각을 가리키게 한다.
        for (int i = 0; i < cut.abs.size(); i++) {
            LayoutBox a = cut.abs.get(i);
            LayoutBox f = a.absCb == null ? null : cut.first.get(a.absCb);
            if (f != null) {
                a.absCb = f;
            }
        }
        double used = 0;
        // 열의 길이(구간 길이와 내용의 아래쪽 중 큰 값)의 최댓값이 내용 높이이다.
        for (int i = 0; i < k; i++) {
            double len = (i == k - 1 ? flowH : segs.get(i)[1]) - root.top[i];
            used = Math.max(used, Math.max(len, root.extent[i]));
        }
        // 내용이 열 수에 들어가지 않아 높이 한계(max-height)까지 열을 채웠으면 컨테이너 높이는 그 한계이다.
        if (Double.isNaN(specH) && !Double.isInfinite(hMax) && k > n) {
            used = Math.max(used, hMax);
        }
        double finalH = Double.isNaN(specH) ? Math.max(minH, Math.min(used, maxH)) : Math.max(minH, Math.min(specH, maxH));
        c.children.clear();
        // 열 상자를 컨테이너의 자식으로 붙인다.
        for (int i = 0; i < k; i++) {
            wrappers[i].height = finalH;
            c.add(wrappers[i]);
        }
        c.columnCount = k;
        c.columnRules = rules(c, k, n, w, gap, contentLeft, contentTop, finalH, rtlSign);
        return Double.isNaN(specH) ? used : finalH;
    }

    /**
     * Creates the rectangles of the column rules between adjacent columns.
     * <p>
     * 이웃한 열 사이의 열 구분선 사각형을 만든다.
     *
     * @param c           the container / 컨테이너
     * @param k           the number of column boxes / 열 상자 수
     * @param n           the used column count / 사용 열 수
     * @param w           the column width / 열 너비
     * @param gap         the gap / gap
     * @param contentLeft x of the content box / content box 의 x
     * @param contentTop  y of the content box / content box 의 y
     * @param h           the column height / 열 높이
     * @param rtlSign     1 for left-to-right, -1 for right-to-left / 왼쪽에서 오른쪽이면 1, 오른쪽에서 왼쪽이면 -1
     * @return the rectangles or null / 사각형 또는 null
     */
    private static double[][] rules(LayoutBox c, int k, int n, double w, double gap, double contentLeft, double contentTop, double h, double rtlSign) {
        double rw = Css.border(c.style, Prop.COLUMN_RULE_WIDTH, Prop.COLUMN_RULE_STYLE);
        if (k < 2 || rw <= 0) {
            return null;
        }
        double[][] r = new double[k - 1][];
        // 이웃한 두 열의 gap 한가운데에 구분선을 놓는다.
        for (int i = 0; i < k - 1; i++) {
            double x;
            if (rtlSign > 0) {
                x = contentLeft + i * (w + gap) + w + gap / 2 - rw / 2;
            } else {
                x = contentLeft + (n - 1 - i) * (w + gap) - gap / 2 - rw / 2;
            }
            r[i] = new double[] { x, contentTop, rw, h };
        }
        return r;
    }

    /**
     * Tells whether a child takes part in the flow of its parent (it is no float, no absolutely positioned box and no list marker).
     * <p>
     * 자식이 부모의 흐름에 참여하는지(float, 절대 배치 상자, 목록 표식이 아닌지) 알려 준다.
     *
     * @param k the child / 자식
     * @return true for in-flow children / 정상 흐름 자식이면 true
     */
    private static boolean inFlow(LayoutBox k) {
        return k.floatSide == LayoutBox.FLOAT_NONE && !k.isAbsolutelyPositioned() && k.type != LayoutBox.Type.MARKER;
    }

    /**
     * Tells whether a box can be split between columns: a block or an anonymous block with children that is not a float, not a
     * positioned-out box, not a flex container, not a grid container and not a multi-column container.
     * <p>
     * 상자를 열 사이에서 나눌 수 있는지 알려 준다: 자식이 있는 블록 또는 익명 블록이며 float, 흐름 밖 상자, 플렉스 컨테이너, 그리드 컨테이너, 다단 컨테이너가 아닌 것.
     *
     * @param k the box / 상자
     * @return true if it can be split / 나눌 수 있으면 true
     */
    private static boolean breakable(LayoutBox k) {
        return (k.type == LayoutBox.Type.BLOCK || k.type == LayoutBox.Type.ANONYMOUS_BLOCK) && !k.inlineLevel && !k.flexContainer && !k.gridContainer && !k.multicol
                && k.floatSide == LayoutBox.FLOAT_NONE && !k.isAbsolutelyPositioned() && !k.children.isEmpty() && inFlowChildren(k) > 0;
    }

    /**
     * Counts the in-flow children of a box.
     * <p>
     * 상자의 정상 흐름 자식 수를 센다.
     *
     * @param k the box / 상자
     * @return the number / 수
     */
    private static int inFlowChildren(LayoutBox k) {
        int count = 0;
        // 자식 중 정상 흐름에 참여하는 것을 센다.
        for (int i = 0; i < k.children.size(); i++) {
            if (inFlow(k.children.get(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Collects the break opportunities below a box as {end of the content before the break, start of the content after it, kind} in
     * flow coordinates (kind 0: allowed). Between two lines of a block a break is allowed only if at least <code>orphans</code> lines
     * stay before it and <code>widows</code> lines follow it; between two sibling boxes it is always allowed.
     * <p>
     * 상자 아래의 끊을 수 있는 자리를 흐름 좌표의 {끊기 전 내용의 끝, 끊은 뒤 내용의 시작, 종류}로 모은다(종류 0: 허용). 블록의 두 줄 사이는 앞에 적어도
     * <code>orphans</code> 줄이 남고 뒤에 <code>widows</code> 줄이 이어질 때만 허용하며 두 형제 상자 사이는 항상 허용한다.
     *
     * @param box     the box / 상자
     * @param originY flow y of the origin of the coordinates of its children / 자식 좌표의 원점의 흐름 y
     * @param out     receives the opportunities / 끊을 수 있는 자리를 받는다
     * @param avoidInside true if the box or an ancestor has <code>break-inside: avoid</code> (the opportunities below it are to be avoided) / 상자나 조상이 <code>break-inside: avoid</code> 이면 true(그 아래의 끊을 수 있는 자리는 피해야 한다)
     * @param bottomClone the bottom edge decoration (border and padding) that the boxes from this one up that have <code>box-decoration-break: clone</code> repeat at the end of the fragment before a break below this box / 이 상자 아래의 끊는 자리 앞에서 이 상자부터 위로 <code>box-decoration-break: clone</code> 인 상자들이 조각 끝에 되풀이하는 아래쪽 가장자리 장식(border 와 padding)
     * @param topClone    the top edge decoration that those boxes repeat at the start of the fragment after the break / 그 상자들이 끊은 뒤 조각의 시작에 되풀이하는 위쪽 가장자리 장식
     */
    private void collect(LayoutBox box, double originY, List<double[]> out, boolean avoidInside, double bottomClone, double topClone) {
        int lines = 0;
        // 줄 상자가 몇 개인지 센다.
        for (int i = 0; i < box.children.size(); i++) {
            if (box.children.get(i).type == LayoutBox.Type.LINE) {
                lines++;
            }
        }
        double orphans = box.style == null ? 2 : box.style.number(Prop.ORPHANS);
        double widows = box.style == null ? 2 : box.style.number(Prop.WIDOWS);
        if (Double.isNaN(orphans)) {
            orphans = 2;
        }
        if (Double.isNaN(widows)) {
            widows = 2;
        }
        LayoutBox prev = null;
        double prevBottom = 0;
        int seen = 0;
        // 정상 흐름 자식을 차례로 보며 앞 자식과의 사이에 끊는 자리를 더하고 나눌 수 있는 자식 안으로 내려간다.
        for (int i = 0; i < box.children.size(); i++) {
            LayoutBox k = box.children.get(i);
            if (!inFlow(k)) {
                continue;
            }
            double t = originY + k.ly;
            if (prev != null) {
                boolean ok = true;
                if (prev.type == LayoutBox.Type.LINE && k.type == LayoutBox.Type.LINE) {
                    ok = seen >= orphans && lines - seen >= widows;
                }
                // 두 형제 사이의 break-after 와 break-before 가 정하는 종류: 강제(2), 피함(1), 허용(0)이다(CSS Fragmentation 3 3.1 절).
                int kind = avoidInside ? 1 : 0;
                if (prev.type != LayoutBox.Type.LINE || k.type != LayoutBox.Type.LINE) {
                    String after = breakAfter(prev, 0);
                    String before = breakBefore(k, 0);
                    if (isForced(after) || isForced(before)) {
                        kind = 2;
                    } else if (isAvoid(after) || isAvoid(before)) {
                        kind = 1;
                    }
                }
                if (ok || kind == 2) {
                    out.add(new double[] { prevBottom, t, kind, bottomClone, topClone });
                }
            }
            if (k.type == LayoutBox.Type.LINE) {
                seen++;
            }
            if (breakable(k)) {
                // clone 인 상자는 나뉜 조각마다 위·아래 가장자리 장식을 되풀이하므로 그 안의 끊는 자리는 그만큼의 공간을 더 든다.
                boolean clone = isClone(k);
                collect(k, t, out, avoidInside || isAvoidInside(k), bottomClone + (clone ? k.borderBottom + k.paddingBottom : 0),
                        topClone + (clone ? k.borderTop + k.paddingTop : 0));
            }
            prev = k;
            prevBottom = t + k.height;
        }
    }

    /**
     * Returns the value of <code>break-before</code> that applies at the start of a box: its own value, or, if that is <code>auto</code>,
     * the value of its first in-flow child (a break value of a first child propagates to the parent, CSS Fragmentation 3 section 3.1).
     * <p>
     * 상자의 시작에 적용되는 <code>break-before</code> 값을 반환한다: 자신의 값, 그것이 <code>auto</code> 이면 첫 정상 흐름 자식의 값(첫 자식의 나눔 값은 부모에게
     * 전파된다. CSS Fragmentation 3 3.1 절).
     *
     * @param k     the box / 상자
     * @param depth the depth of the propagation / 전파 깊이
     * @return the keyword / 키워드
     */
    private static String breakBefore(LayoutBox k, int depth) {
        String v = keyword(k, Prop.BREAK_BEFORE);
        if (!"auto".equals(v) || depth > 8) {
            return v;
        }
        // 첫 정상 흐름 자식에게서 전파되는 값을 찾는다.
        for (int i = 0; i < k.children.size(); i++) {
            LayoutBox c = k.children.get(i);
            if (inFlow(c) && c.type != LayoutBox.Type.LINE) {
                return breakBefore(c, depth + 1);
            }
            if (inFlow(c)) {
                return "auto";
            }
        }
        return "auto";
    }

    /**
     * Returns the value of <code>break-after</code> that applies at the end of a box: its own value, or, if that is <code>auto</code>, the
     * value of its last in-flow child.
     * <p>
     * 상자의 끝에 적용되는 <code>break-after</code> 값을 반환한다: 자신의 값, 그것이 <code>auto</code> 이면 마지막 정상 흐름 자식의 값.
     *
     * @param k     the box / 상자
     * @param depth the depth of the propagation / 전파 깊이
     * @return the keyword / 키워드
     */
    private static String breakAfter(LayoutBox k, int depth) {
        String v = keyword(k, Prop.BREAK_AFTER);
        if (!"auto".equals(v) || depth > 8) {
            return v;
        }
        // 마지막 정상 흐름 자식에게서 전파되는 값을 찾는다.
        for (int i = k.children.size() - 1; i >= 0; i--) {
            LayoutBox c = k.children.get(i);
            if (inFlow(c) && c.type != LayoutBox.Type.LINE) {
                return breakAfter(c, depth + 1);
            }
            if (inFlow(c)) {
                return "auto";
            }
        }
        return "auto";
    }

    /**
     * Returns the keyword of a break property of a box.
     * <p>
     * 상자의 나눔 속성 키워드를 반환한다.
     *
     * @param k  the box / 상자
     * @param id the property / 속성
     * @return the keyword, <code>auto</code> if there is none / 키워드. 없으면 <code>auto</code>
     */
    private static String keyword(LayoutBox k, int id) {
        String v = k.style == null ? null : k.style.keyword(id);
        return v == null ? "auto" : v;
    }

    /**
     * Tells whether a break value forces a break of the columns: <code>always</code>, <code>all</code>, <code>column</code> and the
     * page values (<code>page</code>, <code>left</code>, <code>right</code>, <code>recto</code>, <code>verso</code>), which break through all
     * fragmentation contexts, the columns among them.
     * <p>
     * 나눔 값이 열을 강제로 나누는지 알려 준다: <code>always</code>, <code>all</code>, <code>column</code> 과 모든 분할 문맥(열 포함)을 가로질러 나누는 쪽 값
     * (<code>page</code>, <code>left</code>, <code>right</code>, <code>recto</code>, <code>verso</code>).
     *
     * @param v the keyword / 키워드
     * @return true if the break is forced / 강제이면 true
     */
    private static boolean isForced(String v) {
        return "always".equals(v) || "all".equals(v) || "column".equals(v) || "page".equals(v) || "left".equals(v) || "right".equals(v)
                || "recto".equals(v) || "verso".equals(v);
    }

    /**
     * Tells whether a break value asks to avoid a break of the columns (<code>avoid</code>, <code>avoid-column</code>).
     * <p>
     * 나눔 값이 열을 나누지 말라고 요청하는지(<code>avoid</code>, <code>avoid-column</code>) 알려 준다.
     *
     * @param v the keyword / 키워드
     * @return true to avoid a break / 나누지 말라면 true
     */
    private static boolean isAvoid(String v) {
        return "avoid".equals(v) || "avoid-column".equals(v);
    }

    /**
     * Tells whether a box has <code>break-inside: avoid</code> (or <code>avoid-column</code>).
     * <p>
     * 상자가 <code>break-inside: avoid</code>(또는 <code>avoid-column</code>)인지 알려 준다.
     *
     * @param k the box / 상자
     * @return true if breaks inside the box are to be avoided / 상자 안에서 나누지 말아야 하면 true
     */
    private static boolean isAvoidInside(LayoutBox k) {
        return isAvoid(keyword(k, Prop.BREAK_INSIDE));
    }

    /**
     * Sorts the break opportunities by position.
     * <p>
     * 끊을 수 있는 자리를 위치 순서로 정렬한다.
     *
     * @param cand the opportunities / 끊을 수 있는 자리
     */
    private static void sortCandidates(List<double[]> cand) {
        Collections.sort(cand, new Comparator<double[]>() {
            /**
             * Compares two opportunities by the end of the content before the break, then by the start of the content after it.
             * <p>
             * 두 자리를 끊기 전 내용의 끝, 그다음 끊은 뒤 내용의 시작으로 비교한다.
             *
             * @param a first / 첫째
             * @param b second / 둘째
             * @return the comparison / 비교 결과
             */
            public int compare(double[] a, double[] b) {
                if (a[0] != b[0]) {
                    return a[0] < b[0] ? -1 : 1;
                }
                return a[1] < b[1] ? -1 : (a[1] == b[1] ? 0 : 1);
            }
        });
    }

    /**
     * Cuts the flow into segments for a column height: every segment takes the content up to the last break opportunity that fits the
     * height (an opportunity that is allowed is preferred to one that should be avoided; if nothing fits, the first opportunity after
     * the limit is used, so that tall units overflow).
     * <p>
     * 열 높이에 맞게 흐름을 구간으로 자른다: 구간마다 높이에 들어가는 마지막 끊을 수 있는 자리까지의 내용을 가져간다(허용된 자리가 피해야 할 자리보다 우선이며
     * 들어가는 것이 없으면 한계 뒤의 첫 자리를 써서 큰 단위가 넘치게 한다).
     *
     * @param h       the column height / 열 높이
     * @param cand    the sorted break opportunities / 정렬된 끊을 수 있는 자리
     * @param flowH   the height of the flow / 흐름의 높이
     * @param maxCols the number of segments after which null is returned (0: no limit) / 구간 수가 이를 넘으면 null 을 반환(0: 제한 없음)
     * @return the segments {start, end} or null / 구간 {시작, 끝}. 또는 null
     */
    private List<double[]> fill(double h, List<double[]> cand, double flowH, int maxCols) {
        List<double[]> segs = new ArrayList<double[]>();
        double a = 0;
        int ci = 0;
        // 앞 구간을 끊은 자리에서 clone 상자가 구간 시작에 되풀이하는 위쪽 장식(구간의 내용이 그만큼 낮은 곳에서 시작한다).
        double topExtra = 0;
        // 구간을 하나씩 정하며 흐름의 끝에 닿을 때까지 반복한다(구간마다 a 가 커지므로 끝난다).
        while (true) {
            double limit = a + h - topExtra;
            int forced = -1;
            // 한계 안의 첫 강제 나눔을 찾는다(강제 나눔은 흐름 전체가 들어가는 높이여도 구간을 끝낸다).
            for (int i = ci; i < cand.size(); i++) {
                double[] cd = cand.get(i);
                if (cd[1] <= a + EPS) {
                    ci = i + 1;
                    continue;
                }
                if (cd[0] > limit + EPS) {
                    break;
                }
                if (cd[2] == 2) {
                    forced = i;
                    break;
                }
            }
            if (forced >= 0) {
                segs.add(new double[] { a, cand.get(forced)[0] });
                a = cand.get(forced)[1];
                topExtra = cand.get(forced).length > 4 ? cand.get(forced)[4] : 0;
                ci = forced + 1;
                if (maxCols > 0 && segs.size() > maxCols) {
                    return null;
                }
                continue;
            }
            if (flowH <= limit + EPS) {
                segs.add(new double[] { a, flowH });
                return maxCols > 0 && segs.size() > maxCols ? null : segs;
            }
            int best = -1;
            int bestAvoid = -1;
            int after = -1;
            // 시작 뒤의 끊을 수 있는 자리 중 한계에 들어가는 마지막 것을 고른다.
            for (int i = ci; i < cand.size(); i++) {
                double[] cd = cand.get(i);
                if (cd[1] <= a + EPS) {
                    ci = i + 1;
                    continue;
                }
                if (cd[0] > limit + EPS) {
                    after = i;
                    break;
                }
                // 이 자리에서 끊으면 조각 끝에 되풀이하는 아래쪽 장식(clone)까지 구간에 들어가야 한다.
                if (cd.length > 3 && cd[0] + cd[3] > limit + EPS) {
                    continue;
                }
                if (cd[2] == 0) {
                    best = i;
                } else {
                    bestAvoid = i;
                }
            }
            int pick = best >= 0 ? best : (bestAvoid >= 0 ? bestAvoid : after);
            if (pick < 0) {
                segs.add(new double[] { a, flowH });
                return maxCols > 0 && segs.size() > maxCols ? null : segs;
            }
            segs.add(new double[] { a, cand.get(pick)[0] });
            a = cand.get(pick)[1];
            topExtra = cand.get(pick).length > 4 ? cand.get(pick)[4] : 0;
            ci = pick + 1;
            if (maxCols > 0 && segs.size() > maxCols) {
                return null;
            }
        }
    }

    /**
     * Finds the smallest column height with which the flow fits the given number of columns (column balancing), at most the given
     * maximum; if the content does not fit even then, the maximum is returned (overflow columns follow).
     * <p>
     * 흐름이 주어진 열 수에 들어가는 가장 작은 열 높이를 찾는다(열 균형). 최대값을 넘지 않으며 그때도 들어가지 않으면 최대값을 반환한다(넘침 열이 이어진다).
     *
     * @param cand  the sorted break opportunities / 정렬된 끊을 수 있는 자리
     * @param flowH the height of the flow / 흐름의 높이
     * @param n     the number of columns / 열 수
     * @param hMax  the largest column height or infinity / 가장 큰 열 높이 또는 무한대
     * @return the column height / 열 높이
     */
    private double balance(List<double[]> cand, double flowH, int n, double hMax) {
        double hi = Math.max(0, Math.min(flowH, hMax));
        if (fill(hi, cand, flowH, n) == null) {
            return Double.isInfinite(hMax) ? flowH : hMax;
        }
        double lo = Math.max(0, flowH / n - 1);
        // 들어가는 가장 작은 높이를 이분 탐색한다.
        for (int i = 0; i < BALANCE_STEPS && hi - lo > EPS / 10; i++) {
            double mid = (lo + hi) / 2;
            if (fill(mid, cand, flowH, n) == null) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return hi;
    }

    /**
     * Distributes the children of a box over the columns: a unit (a line, an atomic box, a float) goes to the column that holds its
     * top; a box that can be split gets fragments in the columns where its content is; absolutely positioned children go to the first
     * fragment of their parent. Positions are converted from the coordinates of the original parent to those of the fragment.
     * <p>
     * 상자의 자식을 열에 나눈다: 단위(줄, 나눌 수 없는 상자, float)는 위쪽이 속한 열로 가고, 나눌 수 있는 상자는 내용이 있는 열마다 조각을 얻으며, 절대 배치 자식은
     * 부모의 첫 조각으로 간다. 위치는 원래 부모의 좌표에서 조각의 좌표로 바뀐다.
     *
     * @param kids    the children of the original box / 원래 상자의 자식
     * @param originY flow y of the origin of the coordinates of the children / 자식 좌표의 원점의 흐름 y
     * @param fr      the fragments of the original box / 원래 상자의 조각 모음
     * @param cut     the segments / 구간
     */
    private void distribute(List<LayoutBox> kids, double originY, Frames fr, Cut cut) {
        List<LayoutBox> list = new ArrayList<LayoutBox>(kids);
        int cols = fr.f.length;
        // 자식을 문서 순서로 하나씩 열에 나눈다.
        for (int i = 0; i < list.size(); i++) {
            LayoutBox k = list.get(i);
            double t = originY + k.ly;
            if (k.isAbsolutelyPositioned()) {
                int col = cut.columnOf(originY);
                LayoutBox parent = ensure(fr, col, cut);
                double delta = originY - fr.top[col];
                k.parent = parent;
                parent.children.add(k);
                k.ly += delta;
                k.staticY += delta;
                cut.abs.add(k);
                continue;
            }
            if (inFlow(k) && breakable(k)) {
                Frames sub = new Frames(k, fr, cols);
                // 조각마다 위쪽 가장자리의 흐름 y: 상자가 시작한 열은 상자 위쪽, 이어지는 열은 구간 시작이다.
                for (int c = 0; c < cols; c++) {
                    sub.top[c] = Math.max(t, cut.lo[c]);
                    // clone 상자의 이어지는 조각은 위쪽 가장자리 장식을 되풀이하므로 내용이 그만큼 아래에서 시작한다.
                    sub.extra[c] = isClone(k) && t < cut.lo[c] - EPS ? k.borderTop + k.paddingTop : 0;
                }
                distribute(k.children, t, sub, cut);
                finishFragments(k, sub, t, cut);
                continue;
            }
            int col = cut.columnOf(t);
            LayoutBox parent = ensure(fr, col, cut);
            k.parent = parent;
            parent.children.add(k);
            k.ly = t - fr.top[col] + fr.extra[col];
            fr.extent[col] = Math.max(fr.extent[col], k.ly + k.height);
        }
    }

    /**
     * Returns the fragment of a box in a column, creating it (and the fragments of its ancestors) if it does not exist yet.
     * <p>
     * 열에서의 상자 조각을 반환한다. 아직 없으면 만든다(조상의 조각도 만든다).
     *
     * @param fr  the fragments of the box / 상자의 조각 모음
     * @param col the column / 열
     * @param cut the segments / 구간
     * @return the fragment / 조각
     */
    private LayoutBox ensure(Frames fr, int col, Cut cut) {
        if (fr.f[col] != null) {
            return fr.f[col];
        }
        LayoutBox frag = fr.owner.fragmentCopy();
        frag.lx = fr.owner.lx;
        LayoutBox parent = ensure(fr.up, col, cut);
        frag.ly = fr.top[col] - fr.up.top[col] + fr.up.extra[col];
        frag.parent = parent;
        parent.children.add(frag);
        fr.f[col] = frag;
        // 상자의 첫 조각(가장 앞 열의 것)을 기억해 둔다.
        if (!cut.first.containsKey(fr.owner)) {
            cut.first.put(fr.owner, frag);
        }
        return frag;
    }

    /**
     * Sets the size and the edges of the fragments of a split box: the first fragment keeps the top edge and the last the bottom edge
     * of the box; the fragments in between have none.
     * <p>
     * 나뉜 상자의 조각의 크기와 가장자리를 정한다: 첫 조각은 상자의 위쪽 가장자리를, 마지막 조각은 아래쪽 가장자리를 가지고 중간 조각은 가지지 않는다.
     *
     * @param k   the original box / 원래 상자
     * @param sub its fragments / 조각 모음
     * @param t   flow y of the top of the box / 상자 위쪽의 흐름 y
     * @param cut the segments / 구간
     */
    private void finishFragments(LayoutBox k, Frames sub, double t, Cut cut) {
        double bt = t + k.height;
        int cols = sub.f.length;
        // 조각이 생긴 열마다 높이와 가장자리를 정한다.
        for (int c = 0; c < cols; c++) {
            LayoutBox frag = sub.f[c];
            if (frag == null) {
                continue;
            }
            double fTop = sub.top[c];
            boolean firstFragment = t >= cut.lo[c] - EPS;
            boolean lastFragment = bt <= cut.end[c] + EPS;
            boolean clone = isClone(k);
            // 조각의 높이: 구간 안의 흐름 높이에 되풀이한 위쪽 장식을 더한 것과 내용이 놓인 아래쪽 중 큰 값이고, 끊긴 clone 상자는 아래쪽 장식도 더한다.
            double span = Math.max(Math.min(bt, cut.end[c]) - fTop + sub.extra[c], sub.extent[c]);
            if (clone && !lastFragment) {
                span += k.borderBottom + k.paddingBottom;
            }
            frag.height = Math.max(0, span);
            // slice(기본)는 가장자리 장식이 첫 조각의 위쪽과 마지막 조각의 아래쪽에만 있다. clone 은 조각마다 모두 가진다. margin 은 어느 쪽이든 끊는 곳에서 버린다.
            if (!firstFragment) {
                if (!clone) {
                    frag.borderTop = 0;
                    frag.paddingTop = 0;
                }
                frag.marginTop = 0;
            }
            if (!lastFragment) {
                if (!clone) {
                    frag.borderBottom = 0;
                    frag.paddingBottom = 0;
                }
                frag.marginBottom = 0;
            }
            sub.up.extent[c] = Math.max(sub.up.extent[c], frag.ly + frag.height);
        }
    }
}
