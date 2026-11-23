package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Layout of flex containers (plan item P10-03, CSS Flexible Box Layout Module Level 1, W3C CRD 2025-10-14 section 9 "Flex Layout
 * Algorithm"). The flex items are the children of the container box that {@link BoxBuilder} marked with {@link LayoutBox#flexItem}, in
 * order-modified document order. The algorithm follows the specification: the flex base size and the hypothetical main size of every
 * item (flex-basis, the specified size, the content size, the automatic minimum size, min/max sizes), the collection of items into flex
 * lines (<code>flex-wrap</code>), the resolution of flexible lengths with freezing and min/max violations, the hypothetical cross size,
 * the cross size of the lines, <code>align-content</code>, <code>align-self</code> (stretch, start, end, center, first baseline), auto
 * margins, <code>justify-content</code>, <code>gap</code>, the reversed directions (<code>row-reverse</code>,
 * <code>column-reverse</code>, <code>wrap-reverse</code>, <code>direction:rtl</code>), the baseline of the container and its intrinsic
 * sizes. The layout works in "flex coordinates" (main-start and cross-start are 0) and mirrors the positions at the end if an axis runs
 * from the high coordinate to the low one. A flex item is laid out by {@link BlockLayouter#finish} at the used content width and, if
 * its height is forced, at the used height (<code>flex-direction:column</code>, stretch); an item is laid out again only if the width or
 * the forced height changed. Absolutely positioned children take their static position from the alignment of the container.
 * <p>
 * Not supported (documented limits): <code>visibility:collapse</code> items, <code>last baseline</code> (treated as <code>end</code>),
 * baseline alignment in column containers and with <code>wrap-reverse</code> (treated as <code>start</code>), table items are not
 * stretched, the <code>aspect-ratio</code> property.
 * <p>
 * 플렉스 컨테이너의 레이아웃(계획 P10-03, CSS Flexible Box Layout Module Level 1, W3C CRD 2025-10-14 9장 "Flex Layout Algorithm"). 플렉스 항목은
 * {@link BoxBuilder} 가 {@link LayoutBox#flexItem} 으로 표시한 컨테이너 상자의 자식이며 order 를 반영한 문서 순서이다. 알고리즘은 규격을 따른다: 항목마다
 * flex base size 와 가상 주축 크기(flex-basis, 지정 크기, 내용 크기, 자동 최소 크기, min/max 크기), 플렉스 줄 모으기(<code>flex-wrap</code>), 고정과 min/max
 * 위반을 포함한 유연한 길이 풀이, 가상 교차축 크기, 줄의 교차축 크기, <code>align-content</code>, <code>align-self</code>(stretch, start, end, center, 첫 기준선),
 * auto margin, <code>justify-content</code>, <code>gap</code>, 뒤집힌 방향(<code>row-reverse</code>, <code>column-reverse</code>, <code>wrap-reverse</code>,
 * <code>direction:rtl</code>), 컨테이너의 기준선, 고유 크기. 배치는 "플렉스 좌표"(주축 시작과 교차축 시작이 0)로 하고 축이 큰 좌표에서 작은 좌표로 가면 마지막에
 * 위치를 거울처럼 뒤집는다. 플렉스 항목은 {@link BlockLayouter#finish} 로 사용 content 너비에서, 높이가 강제되면(<code>flex-direction:column</code>, stretch) 사용
 * 높이로 배치한다. 너비나 강제 높이가 바뀐 경우에만 항목을 다시 배치한다. 절대 배치 자식은 컨테이너의 정렬에서 정적 위치를 얻는다.
 * <p>
 * 지원하지 않음(문서화된 한계): <code>visibility:collapse</code> 항목, <code>last baseline</code>(<code>end</code> 로 취급), 열 방향 컨테이너와
 * <code>wrap-reverse</code> 에서의 기준선 정렬(<code>start</code> 로 취급), 표 항목의 stretch, <code>aspect-ratio</code> 속성.
 */
final class FlexLayouter {

    /** Tolerance when comparing sizes. 크기 비교 허용 오차. */
    private static final double EPS = 0.001;

    /** The block layouter that lays out the items. 항목을 배치하는 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the flex layouter.
     * <p>
     * 플렉스 레이아웃을 만든다.
     *
     * @param bl the block layouter of this layout run / 이 레이아웃 실행의 블록 레이아웃
     */
    FlexLayouter(BlockLayouter bl) {
        this.bl = bl;
    }

    /** The state of one flex item. 플렉스 항목 하나의 상태. */
    private static final class Item {
        /** The box of the item. 항목의 상자. */
        final LayoutBox box;
        /** True for an anonymous item (a run of text), which has default flex properties. 익명 항목(텍스트 덩어리)이면 true. 기본 플렉스 속성을 가진다. */
        boolean anon;
        /** <code>flex-grow</code> of the item. 항목의 <code>flex-grow</code> 값. */
        double grow;
        /** <code>flex-shrink</code> of the item. 항목의 <code>flex-shrink</code> 값. */
        double shrink = 1;
        /** Margin at the main-start side. 주축 시작 쪽 margin. */
        double ms;
        /** Margin at the main-end side. 주축 끝 쪽 margin. */
        double me;
        /** Margin at the cross-start side. 교차축 시작 쪽 margin. */
        double cs;
        /** Margin at the cross-end side. 교차축 끝 쪽 margin. */
        double ce;
        /** True if the main-start margin is auto. 주축 시작 margin 이 auto 이면 true. */
        boolean autoMs;
        /** True if the main-end margin is auto. 주축 끝 margin 이 auto 이면 true. */
        boolean autoMe;
        /** True if the cross-start margin is auto. 교차축 시작 margin 이 auto 이면 true. */
        boolean autoCs;
        /** True if the cross-end margin is auto. 교차축 끝 margin 이 auto 이면 true. */
        boolean autoCe;
        /** Padding plus border along the main axis. 주축 방향 padding 과 border 의 합. */
        double extraMain;
        /** Padding plus border along the cross axis. 교차축 방향 padding 과 border 의 합. */
        double extraCross;
        /** Flex base size (content box). 항목의 flex base size(content box 기준). */
        double base;
        /** Minimum main size (content box). 최소 주축 크기(content box). */
        double min;
        /** Maximum main size (content box), infinity if none. 최대 주축 크기(content box). 없으면 무한대. */
        double max = Double.POSITIVE_INFINITY;
        /** Hypothetical main size: the base size clamped by min and max. 가상 주축 크기: base size 를 min/max 로 제한한 값. */
        double hypo;
        /** Target (used) main size while flexible lengths are resolved. 유연한 길이를 푸는 동안의 목표(사용) 주축 크기. */
        double target;
        /** True once the target size is frozen. 목표 크기가 고정되었으면 true. */
        boolean frozen;
        /** True if the item was clamped up by its minimum. 최솟값으로 올려졌으면 true. */
        boolean violMin;
        /** True if the item was clamped down by its maximum. 최댓값으로 내려졌으면 true. */
        boolean violMax;
        /** Used content size across the axis. 교차축 사용 content 크기. */
        double crossContent;
        /** Content width used to measure an item of a column container. 열 컨테이너 항목을 잴 때 쓴 content 너비. */
        double crossW;
        /** True if the specified cross size is auto. 지정된 교차축 크기가 auto 이면 true. */
        boolean crossAuto;
        /** Normalized align-self: stretch, start, end, center or baseline. 정규화한 align-self: stretch, start, end, center, baseline. */
        String align = "stretch";
        /** True for a <code>safe</code> alignment. <code>safe</code> 정렬이면 true. */
        boolean safe;
        /** Distance of the baseline from the cross-start margin edge. 교차축 시작 margin 가장자리에서 기준선까지 거리. */
        double above;
        /** True once the item was laid out at least once. 한 번이라도 배치했으면 true. */
        boolean laid;
        /** Content width of the last layout. 마지막 배치의 content 너비. */
        double laidW = Double.NaN;
        /** Forced content height of the last layout (NaN: none). 마지막 배치의 강제 content 높이(NaN: 없음). */
        double laidH = Double.NaN;
        /** Start of the outer box along the main axis (flex coordinates). 주축 방향 바깥 상자의 시작(플렉스 좌표). */
        double mainPos;
        /** Start of the outer box along the cross axis (flex coordinates). 교차축 방향 바깥 상자의 시작(플렉스 좌표). */
        double crossPos;

        /**
         * Creates the state of an item.
         * <p>
         * 항목의 상태를 만든다.
         *
         * @param box the item box / 항목 상자
         */
        Item(LayoutBox box) {
            this.box = box;
        }

        /**
         * Returns the outer main size with the margins as they are now.
         * <p>
         * 현재 margin 으로 바깥 주축 크기를 반환한다.
         *
         * @param size the content main size / content 주축 크기
         * @return outer size / 바깥 크기
         */
        double outerMain(double size) {
            return ms + extraMain + size + me;
        }

        /**
         * Returns the outer cross size with the margins as they are now.
         * <p>
         * 현재 margin 으로 바깥 교차축 크기를 반환한다.
         *
         * @return outer size / 바깥 크기
         */
        double outerCross() {
            return cs + extraCross + crossContent + ce;
        }
    }

    /** A flex line. 플렉스 줄. */
    private static final class Line {
        /** The items of the line in flex order. 줄의 항목(플렉스 순서). */
        final List<Item> items = new ArrayList<Item>();
        /** Cross size of the line. 줄의 교차축 크기. */
        double cross;
        /** Start of the line along the cross axis. 교차축 방향 줄의 시작. */
        double crossPos;
        /** The largest distance from the cross-start edge to the baseline among the baseline-aligned items. 기준선 정렬 항목의 시작 가장자리에서 기준선까지 최대 거리. */
        double maxAbove;
    }

    /**
     * Replaces NaN by a default.
     * <p>
     * NaN 을 기본값으로 바꾼다.
     *
     * @param v   value / 값
     * @param def default / 기본값
     * @return value or default / 값 또는 기본값
     */
    private static double or(double v, double def) {
        return Double.isNaN(v) ? def : v;
    }

    /**
     * Compares two sizes where NaN equals NaN.
     * <p>
     * NaN 끼리 같다고 보고 두 크기를 비교한다.
     *
     * @param a first / 첫째
     * @param b second / 둘째
     * @return true if equal / 같으면 true
     */
    private static boolean same(double a, double b) {
        return (Double.isNaN(a) && Double.isNaN(b)) || Math.abs(a - b) < EPS;
    }

    /**
     * Returns the lower-case words of a keyword-valued property (for example <code>safe center</code>).
     * <p>
     * 키워드 값 속성의 소문자 단어를 반환한다(예: <code>safe center</code>).
     *
     * @param s  the style / 스타일
     * @param id the property / 속성
     * @return the words / 단어
     */
    static String[] words(ComputedStyle s, int id) {
        CssValue v = s.get(id);
        String t = v == null ? "" : v.toCssText().trim().toLowerCase(Locale.ENGLISH);
        if (t.length() == 0) {
            return new String[] { "" };
        }
        // anchor-center 는 앵커가 있는 절대 배치 상자만 앵커에 맞추고 그 밖에서는 center 로 동작한다(CSS Anchor Positioning 1 4.2 절).
        if (t.equals("anchor-center")) {
            return new String[] { "center" };
        }
        return t.split("\\s+");
    }

    /**
     * Returns a gap value (<code>row-gap</code> or <code>column-gap</code>): 0 for <code>normal</code>, a percentage refers to the given
     * base (0 if the base is unknown).
     * <p>
     * gap 값(<code>row-gap</code> 또는 <code>column-gap</code>)을 반환한다: <code>normal</code> 은 0, 백분율은 주어진 기준에 대한 값(기준을 모르면 0).
     *
     * @param s    the style / 스타일
     * @param id   the property / 속성
     * @param base the length that 100% stands for or NaN / 100% 가 뜻하는 길이 또는 NaN
     * @return the gap / gap
     */
    static double gap(ComputedStyle s, int id, double base) {
        CssValue v = s.get(id);
        if (v == null || v.isKeyword("normal")) {
            return 0;
        }
        double r = s.resolve(id, base);
        return Double.isNaN(r) || r < 0 ? 0 : r;
    }

    /**
     * Lays out the items of a flex container and returns the height of its content box.
     * <p>
     * 플렉스 컨테이너의 항목을 배치하고 content box 의 높이를 반환한다.
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
        Run r = new Run(c, cw, specH, minH, maxH);
        return r.run(contentLeft, contentTop);
    }

    /**
     * Returns the min-content or max-content width of a flex container (the content box). A row container sums the contributions of
     * its items and gaps (a wrapping container uses the widest item for the min-content width), a column container uses the widest
     * item.
     * <p>
     * 플렉스 컨테이너의 min-content 또는 max-content 너비(content box)를 반환한다. 행 컨테이너는 항목의 기여와 gap 을 더하고(줄바꿈하는 컨테이너의
     * min-content 는 가장 넓은 항목) 열 컨테이너는 가장 넓은 항목을 쓴다.
     *
     * @param c   the container / 컨테이너
     * @param max true for max-content / max-content 이면 true
     * @return the width / 너비
     */
    double intrinsic(LayoutBox c, boolean max) {
        ComputedStyle cs = c.style;
        String dir = Css.kw(cs, Prop.FLEX_DIRECTION);
        boolean row = !dir.startsWith("column");
        String w = Css.kw(cs, Prop.FLEX_WRAP);
        boolean wrap = "wrap".equals(w) || "wrap-reverse".equals(w);
        double gap = gap(cs, Prop.COLUMN_GAP, Double.NaN);
        double sum = 0;
        double widest = 0;
        int n = 0;
        // 항목마다 바깥 기여 너비를 구해 더하거나 최댓값을 고른다.
        for (int i = 0; i < c.children.size(); i++) {
            LayoutBox b = c.children.get(i);
            if (!b.flexItem) {
                continue;
            }
            bl.edges(b, Double.NaN);
            double spec = bl.specWidth(b, Double.NaN);
            double content;
            if (!Double.isNaN(spec)) {
                content = spec;
            } else {
                content = max ? bl.contentMax(b) : bl.contentMin(b);
            }
            content = Math.max(bl.minWidth(b, Double.NaN), Math.min(content, bl.maxWidth(b, Double.NaN)));
            double outer = content + BlockLayouter.hExtras(b) + b.marginLeft + b.marginRight;
            sum += outer;
            widest = Math.max(widest, outer);
            n++;
        }
        if (row) {
            return (!max && wrap) ? widest : sum + gap * Math.max(0, n - 1);
        }
        return widest;
    }

    /** The state of one layout of a flex container. 플렉스 컨테이너 한 번의 배치 상태. */
    private final class Run {
        /** The container. 컨테이너. */
        final LayoutBox c;
        /** Content width of the container. 컨테이너의 content 너비. */
        final double cw;
        /** Definite content height of the container (clamped by min and max) or NaN. 컨테이너의 정해진 content 높이(min/max 로 제한) 또는 NaN. */
        final double contH;
        /** Minimum content height. 최소 content 높이. */
        final double minH;
        /** Maximum content height. 최대 content 높이. */
        final double maxH;
        /** True if the main axis is horizontal. 주축이 가로이면 true. */
        final boolean row;
        /** True for <code>row-reverse</code> and <code>column-reverse</code>. <code>row-reverse</code>, <code>column-reverse</code> 이면 true. */
        final boolean reverse;
        /** True for <code>direction:rtl</code>. <code>direction:rtl</code> 이면 true. */
        final boolean rtl;
        /** True if the container wraps. 줄바꿈하는 컨테이너이면 true. */
        final boolean wrap;
        /** True for <code>wrap-reverse</code>. <code>wrap-reverse</code> 이면 true. */
        final boolean wrapRev;
        /** True if the main axis runs from the high coordinate to the low one. 주축이 큰 좌표에서 작은 좌표로 가면 true. */
        final boolean mainMirror;
        /** True if the cross axis runs from the high coordinate to the low one. 교차축이 큰 좌표에서 작은 좌표로 가면 true. */
        final boolean crossMirror;
        /** Gap between items along the main axis. 주축 방향 항목 사이 gap. */
        double mainGap;
        /** Gap between lines. 줄 사이 gap. */
        double crossGap;
        /** The flex items. 플렉스 항목. */
        final List<Item> items = new ArrayList<Item>();
        /** The absolutely positioned children. 절대 배치 자식. */
        final List<LayoutBox> absBoxes = new ArrayList<LayoutBox>();
        /** The flex lines. 플렉스 줄. */
        final List<Line> lines = new ArrayList<Line>();
        /** Inner main size of the container. 컨테이너의 안쪽 주축 크기. */
        double mainSize;
        /** Inner cross size of the container. 컨테이너의 안쪽 교차축 크기. */
        double crossSize;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param c     the container / 컨테이너
         * @param cw    content width / content 너비
         * @param specH specified content height or NaN / 지정된 content 높이 또는 NaN
         * @param minH  minimum content height / 최소 content 높이
         * @param maxH  maximum content height / 최대 content 높이
         */
        Run(LayoutBox c, double cw, double specH, double minH, double maxH) {
            this.c = c;
            this.cw = cw;
            this.minH = minH;
            this.maxH = maxH;
            this.contH = Double.isNaN(specH) ? Double.NaN : Math.max(minH, Math.min(specH, maxH));
            ComputedStyle cs = c.style;
            String dir = Css.kw(cs, Prop.FLEX_DIRECTION);
            row = !dir.startsWith("column");
            reverse = dir.endsWith("-reverse");
            rtl = "rtl".equals(Css.kw(cs, Prop.DIRECTION));
            String w = Css.kw(cs, Prop.FLEX_WRAP);
            wrap = "wrap".equals(w) || "wrap-reverse".equals(w);
            wrapRev = "wrap-reverse".equals(w);
            mainMirror = row ? (rtl != reverse) : reverse;
            crossMirror = (!row && rtl) != wrapRev;
            double colGap = gap(cs, Prop.COLUMN_GAP, cw);
            double rowGap = gap(cs, Prop.ROW_GAP, contH);
            mainGap = row ? colGap : rowGap;
            crossGap = row ? rowGap : colGap;
        }

        /**
         * Runs the whole algorithm.
         * <p>
         * 알고리즘 전체를 실행한다.
         *
         * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
         * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
         * @return height of the content / content 높이
         */
        double run(double contentLeft, double contentTop) {
            // 자식 중 플렉스 항목은 준비하고 절대 배치 상자는 따로 모은다.
            for (int i = 0; i < c.children.size(); i++) {
                LayoutBox b = c.children.get(i);
                if (b.flexItem) {
                    items.add(prepare(b));
                } else if (b.isAbsolutelyPositioned()) {
                    absBoxes.add(b);
                }
            }
            double mainAvail = row ? cw : contH;
            // 항목마다 flex base size 와 가상 주축 크기를 구한다.
            for (int i = 0; i < items.size(); i++) {
                sizeMain(items.get(i), mainAvail);
            }
            buildLines(mainAvail);
            determineMainSize(mainAvail);
            // 줄마다 유연한 길이를 푼다.
            for (int i = 0; i < lines.size(); i++) {
                resolveFlexible(lines.get(i));
            }
            crossSizing();
            // 줄마다 항목의 교차축 정렬과 주축 정렬을 하고 위치를 정한다.
            for (int i = 0; i < lines.size(); i++) {
                alignCross(lines.get(i));
                justify(lines.get(i));
            }
            apply(contentLeft, contentTop);
            staticPositions(contentLeft, contentTop);
            c.baseline = containerBaseline();
            return row ? crossSize : mainSize;
        }

        /**
         * Prepares an item: its margins, paddings, borders, flex factors and alignment.
         * <p>
         * 항목을 준비한다: margin, padding, border, flex 계수, 정렬.
         *
         * @param b the item box / 항목 상자
         * @return the item state / 항목 상태
         */
        Item prepare(LayoutBox b) {
            Item it = new Item(b);
            it.anon = b.type == LayoutBox.Type.ANONYMOUS_BLOCK;
            int auto = bl.edges(b, cw);
            int fr = BlockLayouter.frameOf(b);
            boolean aT = !it.anon && b.style.isAuto(Css.side(Css.MARGIN, fr, 0));
            boolean aB = !it.anon && b.style.isAuto(Css.side(Css.MARGIN, fr, 2));
            boolean aL = (auto & 1) != 0;
            boolean aR = (auto & 2) != 0;
            double mL = b.marginLeft;
            double mR = b.marginRight;
            double mT = b.marginTop;
            double mB = b.marginBottom;
            // 물리적 위/아래/왼쪽/오른쪽 margin 을 주축과 교차축의 시작/끝으로 옮긴다.
            if (row) {
                it.ms = mainMirror ? mR : mL;
                it.me = mainMirror ? mL : mR;
                it.autoMs = mainMirror ? aR : aL;
                it.autoMe = mainMirror ? aL : aR;
                it.cs = crossMirror ? mB : mT;
                it.ce = crossMirror ? mT : mB;
                it.autoCs = crossMirror ? aB : aT;
                it.autoCe = crossMirror ? aT : aB;
                it.extraMain = BlockLayouter.hExtras(b);
                it.extraCross = BlockLayouter.vExtras(b);
            } else {
                it.ms = mainMirror ? mB : mT;
                it.me = mainMirror ? mT : mB;
                it.autoMs = mainMirror ? aB : aT;
                it.autoMe = mainMirror ? aT : aB;
                it.cs = crossMirror ? mR : mL;
                it.ce = crossMirror ? mL : mR;
                it.autoCs = crossMirror ? aR : aL;
                it.autoCe = crossMirror ? aL : aR;
                it.extraMain = BlockLayouter.vExtras(b);
                it.extraCross = BlockLayouter.hExtras(b);
            }
            if (!it.anon) {
                it.grow = Math.max(0, or(b.style.number(Prop.FLEX_GROW), 0));
                it.shrink = Math.max(0, or(b.style.number(Prop.FLEX_SHRINK), 1));
            }
            String[] a = it.anon ? new String[] { "auto" } : words(b.style, Prop.ALIGN_SELF);
            String last = a[a.length - 1];
            if ("auto".equals(last) || last.length() == 0) {
                a = words(c.style, Prop.ALIGN_ITEMS);
            }
            normalizeAlign(it, a);
            return it;
        }

        /**
         * Normalizes the words of an <code>align-self</code> value into the item's alignment: <code>stretch</code>, <code>start</code>,
         * <code>end</code> (relative to the cross-start of the flex container), <code>center</code> or <code>baseline</code>.
         * <p>
         * <code>align-self</code> 값의 단어를 항목의 정렬로 정규화한다: <code>stretch</code>, <code>start</code>, <code>end</code>(플렉스 컨테이너의 교차축 시작
         * 기준), <code>center</code>, <code>baseline</code>.
         *
         * @param it the item / 항목
         * @param a  the words / 단어
         */
        private void normalizeAlign(Item it, String[] a) {
            String k = a[a.length - 1];
            boolean lastBase = false;
            // 단어 중 safe 와 last 를 찾는다.
            for (int i = 0; i < a.length; i++) {
                if ("safe".equals(a[i])) {
                    it.safe = true;
                }
                if ("last".equals(a[i])) {
                    lastBase = true;
                }
            }
            it.align = alignKind(k, lastBase);
        }

        /**
         * Maps an alignment keyword to <code>stretch</code>, <code>start</code>, <code>end</code>, <code>center</code> or
         * <code>baseline</code> (flex start and end of the cross axis).
         * <p>
         * 정렬 키워드를 <code>stretch</code>, <code>start</code>, <code>end</code>, <code>center</code>, <code>baseline</code>(교차축의 플렉스 시작과 끝)으로 바꾼다.
         *
         * @param k        the keyword / 키워드
         * @param lastBase true for <code>last baseline</code> / <code>last baseline</code> 이면 true
         * @return the kind / 종류
         */
        private String alignKind(String k, boolean lastBase) {
            if ("flex-start".equals(k)) {
                return "start";
            }
            if ("flex-end".equals(k)) {
                return "end";
            }
            if ("center".equals(k)) {
                return "center";
            }
            if ("start".equals(k) || "self-start".equals(k) || "left".equals(k)) {
                return wrapRev ? "end" : "start";
            }
            if ("end".equals(k) || "self-end".equals(k) || "right".equals(k)) {
                return wrapRev ? "start" : "end";
            }
            if ("baseline".equals(k)) {
                if (lastBase) {
                    return "end";
                }
                return (row && !crossMirror) ? "baseline" : "start";
            }
            return "stretch";
        }

        /**
         * Lays out an item at a content width and an optional forced content height unless it already is laid out like that.
         * <p>
         * 항목을 content 너비와 선택적 강제 content 높이로 배치한다. 이미 그렇게 배치되어 있으면 하지 않는다.
         *
         * @param it      the item / 항목
         * @param w       content width / content 너비
         * @param forcedH forced content height or NaN / 강제 content 높이 또는 NaN
         */
        void place(Item it, double w, double forcedH) {
            if (it.laid && same(it.laidW, w) && same(it.laidH, forcedH)) {
                return;
            }
            LayoutBox b = it.box;
            if (b.type == LayoutBox.Type.REPLACED) {
                b.forcedH = Double.NaN;
                bl.finish(b, w, contH, replacedAt(b, w, forcedH));
            } else if (b.type == LayoutBox.Type.TABLE) {
                // 표는 자신의 너비 규칙을 따른다: 쓸 수 있는 너비만 알려 준다.
                bl.tables.layout(b, w + BlockLayouter.hExtras(b) + b.marginLeft + b.marginRight, true);
            } else {
                b.forcedH = forcedH;
                bl.finish(b, w, contH, null);
                b.forcedH = Double.NaN;
            }
            it.laid = true;
            it.laidW = w;
            it.laidH = forcedH;
        }

        /**
         * Returns the content size of a replaced item at a content width: the height is forced, specified, or follows the aspect ratio
         * of the natural size.
         * <p>
         * 치환 항목의 content 너비에서의 content 크기를 반환한다: 높이는 강제, 지정, 또는 자연 크기의 가로세로 비율을 따른다.
         *
         * @param b       the replaced box (its edges are resolved) / 치환 상자(가장자리가 풀려 있음)
         * @param w       content width / content 너비
         * @param forcedH forced content height or NaN / 강제 content 높이 또는 NaN
         * @return {width, height} / {너비, 높이}
         */
        private double[] replacedAt(LayoutBox b, double w, double forcedH) {
            double[] r = bl.replacedSize(b, cw, contH);
            double[] nat = Replaced.natural(b, BlockLayouter.frameOf(b));
            double ratio = nat[0] > 0 && nat[1] > 0 ? nat[0] / nat[1] : Double.NaN;
            double sh = bl.specHeight(b, contH);
            double h;
            if (!Double.isNaN(forcedH)) {
                h = forcedH;
            } else if (!Double.isNaN(sh)) {
                h = sh;
            } else if (!Double.isNaN(ratio)) {
                h = w / ratio;
            } else {
                h = r[1];
            }
            h = Math.max(bl.minHeight(b, contH), Math.min(h, bl.maxHeight(b, contH)));
            return new double[] { w, h };
        }

        /**
         * Returns the content width at which an item of a column container is measured and laid out: the specified width, the stretched
         * width (single-line container), or the fit-content width.
         * <p>
         * 열 컨테이너 항목을 재고 배치할 content 너비를 반환한다: 지정 너비, stretch 한 너비(한 줄 컨테이너), 또는 fit-content 너비.
         *
         * @param it the item / 항목
         * @return content width / content 너비
         */
        private double crossWidth(Item it) {
            LayoutBox b = it.box;
            double spec = bl.specWidth(b, cw);
            double lo = bl.minWidth(b, cw);
            double hi = bl.maxWidth(b, cw);
            double w;
            if (!Double.isNaN(spec)) {
                w = spec;
            } else {
                double avail = Math.max(0, cw - b.marginLeft - b.marginRight - BlockLayouter.hExtras(b));
                if ("stretch".equals(it.align) && !it.autoCs && !it.autoCe && !wrap) {
                    w = avail;
                } else {
                    w = Math.min(Math.max(bl.contentMin(b), avail), bl.contentMax(b));
                }
            }
            return Math.max(lo, Math.min(w, hi));
        }

        /**
         * Determines the flex base size, the minimum and maximum main size and the hypothetical main size of an item (CSS Flexbox 9.2
         * step 3 and 9.7).
         * <p>
         * 항목의 flex base size, 최소/최대 주축 크기, 가상 주축 크기를 정한다(CSS Flexbox 9.2 의 3단계와 9.7).
         *
         * @param it        the item / 항목
         * @param mainAvail the definite inner main size of the container or NaN / 컨테이너의 정해진 안쪽 주축 크기 또는 NaN
         */
        private void sizeMain(Item it, double mainAvail) {
            LayoutBox b = it.box;
            boolean clips = !it.anon && Css.clips(b.style);
            double content;
            double specMain;
            boolean minAuto;
            if (row) {
                specMain = bl.specWidth(b, cw);
                it.max = bl.maxWidth(b, cw);
                minAuto = it.anon || b.style.isAuto(Css.inlineSizeProp(BlockLayouter.frameOf(b), 1));
                content = b.type == LayoutBox.Type.REPLACED ? bl.replacedSize(b, cw, contH)[0] : bl.contentMax(b);
                it.crossAuto = Double.isNaN(bl.specHeight(b, contH));
            } else {
                it.crossW = crossWidth(it);
                // 내용 크기는 지정된 높이를 무시하고 잰다. 이어서 실제 높이로 다시 배치하도록 기록을 지운다.
                b.measureContent = true;
                it.laid = false;
                place(it, it.crossW, Double.NaN);
                b.measureContent = false;
                it.laid = false;
                content = Math.max(0, b.height - BlockLayouter.vExtras(b));
                specMain = bl.specHeight(b, contH);
                it.max = bl.maxHeight(b, contH);
                minAuto = it.anon || b.style.isAuto(Css.blockSizeProp(BlockLayouter.frameOf(b), 1));
                it.crossAuto = Double.isNaN(bl.specWidth(b, cw));
            }
            if (minAuto && !clips) {
                // 자동 최소 크기: 내용 크기 제안과 지정 크기 제안 중 작은 값(최댓값을 넘지 않음).
                double suggestion = row ? bl.contentMin(b) : content;
                if (!Double.isNaN(specMain)) {
                    suggestion = Math.min(suggestion, specMain);
                }
                it.min = Math.min(suggestion, it.max);
            } else {
                it.min = row ? bl.minWidth(b, cw) : bl.minHeight(b, contH);
            }
            it.base = basis(it, mainAvail, specMain, content);
            it.hypo = Math.max(it.min, Math.min(it.base, it.max));
        }

        /**
         * Resolves <code>flex-basis</code> to a content-box size (CSS Flexbox 9.2 step 3).
         * <p>
         * <code>flex-basis</code> 를 content box 크기로 푼다(CSS Flexbox 9.2 의 3단계).
         *
         * @param it        the item / 항목
         * @param mainAvail the base of percentages (the container main size) or NaN / 백분율의 기준(컨테이너 주축 크기) 또는 NaN
         * @param specMain  the specified width or height (content box) or NaN / 지정된 너비나 높이(content box) 또는 NaN
         * @param content   the content size / 내용 크기
         * @return the flex base size / flex base size
         */
        private double basis(Item it, double mainAvail, double specMain, double content) {
            LayoutBox b = it.box;
            CssValue fb = it.anon ? null : b.style.get(Prop.FLEX_BASIS);
            if (fb == null || fb.isKeyword("auto")) {
                return Double.isNaN(specMain) ? content : specMain;
            }
            if (fb.isKeyword("content") || fb.isKeyword("max-content") || fb.isKeyword("fit-content") || fb.isKeyword("stretch")) {
                return content;
            }
            if (fb.isKeyword("min-content")) {
                return row ? bl.contentMin(b) : content;
            }
            double v = b.style.resolve(Prop.FLEX_BASIS, mainAvail);
            if (Double.isNaN(v)) {
                return content;
            }
            return Math.max(0, BlockLayouter.toContent(b, v, it.extraMain));
        }

        /**
         * Collects the items into flex lines: one line for <code>nowrap</code>, else a new line starts when the next item does not fit
         * (CSS Flexbox 9.3).
         * <p>
         * 항목을 플렉스 줄로 모은다: <code>nowrap</code> 은 한 줄, 아니면 다음 항목이 들어가지 않을 때 새 줄을 시작한다(CSS Flexbox 9.3).
         *
         * @param mainAvail the definite inner main size or NaN / 정해진 안쪽 주축 크기 또는 NaN
         */
        private void buildLines(double mainAvail) {
            double limit = mainAvail;
            if (Double.isNaN(limit)) {
                limit = maxH;
            }
            Line cur = new Line();
            double sum = 0;
            // 항목을 차례로 현재 줄에 넣고 넘치면 새 줄을 시작한다.
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                double w = it.outerMain(it.hypo);
                if (wrap && !cur.items.isEmpty() && sum + mainGap + w > limit + EPS) {
                    lines.add(cur);
                    cur = new Line();
                    sum = 0;
                }
                sum = cur.items.isEmpty() ? w : sum + mainGap + w;
                cur.items.add(it);
            }
            if (!cur.items.isEmpty() || lines.isEmpty()) {
                lines.add(cur);
            }
        }

        /**
         * Determines the inner main size of the container: the definite size, or the largest sum of hypothetical sizes clamped by the
         * minimum and maximum (CSS Flexbox 9.2 step 2).
         * <p>
         * 컨테이너의 안쪽 주축 크기를 정한다: 정해진 크기, 아니면 가상 크기 합의 최댓값을 min/max 로 제한한 값(CSS Flexbox 9.2 의 2단계).
         *
         * @param mainAvail the definite inner main size or NaN / 정해진 안쪽 주축 크기 또는 NaN
         */
        private void determineMainSize(double mainAvail) {
            if (!Double.isNaN(mainAvail)) {
                mainSize = mainAvail;
                return;
            }
            double best = 0;
            // 줄마다 가상 바깥 크기와 gap 의 합을 구해 가장 큰 값을 고른다.
            for (int i = 0; i < lines.size(); i++) {
                Line l = lines.get(i);
                double sum = mainGap * Math.max(0, l.items.size() - 1);
                // 줄의 항목마다 가상 바깥 주축 크기를 더한다.
                for (int k = 0; k < l.items.size(); k++) {
                    sum += l.items.get(k).outerMain(l.items.get(k).hypo);
                }
                best = Math.max(best, sum);
            }
            mainSize = Math.max(minH, Math.min(best, maxH));
        }

        /**
         * Resolves the flexible lengths of a line (CSS Flexbox 9.7): freezes the inflexible items, distributes the free space in a loop,
         * clamps by minimum and maximum and freezes the violating items until all items are frozen.
         * <p>
         * 줄의 유연한 길이를 푼다(CSS Flexbox 9.7): 유연하지 않은 항목을 고정하고, 남는 공간을 나누고, min/max 로 제한하고, 위반한 항목을 고정하는 일을 모두
         * 고정될 때까지 반복한다.
         *
         * @param line the line / 줄
         */
        private void resolveFlexible(Line line) {
            List<Item> its = line.items;
            double gaps = mainGap * Math.max(0, its.size() - 1);
            double sumHypo = gaps;
            // 가상 바깥 크기의 합으로 늘릴지 줄일지 정한다.
            for (int i = 0; i < its.size(); i++) {
                sumHypo += its.get(i).outerMain(its.get(i).hypo);
            }
            boolean grow = sumHypo < mainSize;
            // 유연하지 않은 항목은 가상 크기로 고정한다.
            for (int i = 0; i < its.size(); i++) {
                Item it = its.get(i);
                double factor = grow ? it.grow : it.shrink;
                it.target = it.base;
                it.frozen = false;
                it.violMin = false;
                it.violMax = false;
                if (factor == 0 || (grow && it.base > it.hypo) || (!grow && it.base < it.hypo)) {
                    it.frozen = true;
                    it.target = it.hypo;
                }
            }
            double initialFree = freeSpace(its, gaps);
            // 모든 항목이 고정될 때까지 반복한다(한 번에 적어도 한 항목은 고정되므로 항목 수 + 1 번이면 끝난다).
            for (int guard = 0; guard <= its.size() + 1; guard++) {
                boolean allFrozen = true;
                double sumFactor = 0;
                double sumScaled = 0;
                // 고정되지 않은 항목의 계수 합을 구한다.
                for (int i = 0; i < its.size(); i++) {
                    Item it = its.get(i);
                    if (!it.frozen) {
                        allFrozen = false;
                        sumFactor += grow ? it.grow : it.shrink;
                        sumScaled += it.shrink * it.base;
                    }
                }
                if (allFrozen) {
                    break;
                }
                double free = freeSpace(its, gaps);
                // 계수 합이 1 보다 작으면 처음 남는 공간에 그 합을 곱한 값이 더 작을 때 그것을 쓴다.
                if (sumFactor < 1) {
                    double scaled = initialFree * sumFactor;
                    if (Math.abs(scaled) < Math.abs(free)) {
                        free = scaled;
                    }
                }
                // 고정되지 않은 항목에 남는 공간을 계수 비율로 나눈다.
                for (int i = 0; i < its.size(); i++) {
                    Item it = its.get(i);
                    if (it.frozen) {
                        continue;
                    }
                    it.target = it.base;
                    if (free == 0) {
                        continue;
                    }
                    if (grow) {
                        if (sumFactor > 0) {
                            it.target = it.base + free * it.grow / sumFactor;
                        }
                    } else if (sumScaled > 0) {
                        it.target = it.base - Math.abs(free) * (it.shrink * it.base) / sumScaled;
                    }
                }
                // min/max 위반을 고치고 위반의 합에 따라 고정한다.
                double total = 0;
                for (int i = 0; i < its.size(); i++) {
                    Item it = its.get(i);
                    if (it.frozen) {
                        continue;
                    }
                    double clamped = Math.max(0, Math.max(it.min, Math.min(it.max, it.target)));
                    double v = clamped - it.target;
                    total += v;
                    it.violMin = v > 0;
                    it.violMax = v < 0;
                    it.target = clamped;
                }
                // 위반 합에 해당하는 항목을 고정한다.
                for (int i = 0; i < its.size(); i++) {
                    Item it = its.get(i);
                    if (!it.frozen && (total == 0 || (total > 0 && it.violMin) || (total < 0 && it.violMax))) {
                        it.frozen = true;
                    }
                }
            }
        }

        /**
         * Returns the free space of a line: the container main size minus the gaps and the outer sizes (the target size of frozen items,
         * the base size of the others).
         * <p>
         * 줄의 남는 공간을 반환한다: 컨테이너 주축 크기에서 gap 과 바깥 크기(고정된 항목은 목표 크기, 나머지는 base size)를 뺀 값.
         *
         * @param its  the items / 항목
         * @param gaps the gaps of the line / 줄의 gap 합
         * @return the free space / 남는 공간
         */
        private double freeSpace(List<Item> its, double gaps) {
            double used = gaps;
            // 항목의 바깥 크기를 더한다.
            for (int i = 0; i < its.size(); i++) {
                Item it = its.get(i);
                used += it.outerMain(it.frozen ? it.target : it.base);
            }
            return mainSize - used;
        }

        /**
         * Determines the hypothetical cross sizes, the cross size of the lines and of the container, and distributes the lines with
         * <code>align-content</code> (CSS Flexbox 9.4).
         * <p>
         * 가상 교차축 크기, 줄과 컨테이너의 교차축 크기를 정하고 <code>align-content</code> 로 줄을 나눈다(CSS Flexbox 9.4).
         */
        private void crossSizing() {
            boolean crossDefinite = row ? !Double.isNaN(contH) : true;
            // 항목을 사용 주축 크기로 배치해 가상 교차축 크기와 기준선을 구한다.
            for (int i = 0; i < lines.size(); i++) {
                Line l = lines.get(i);
                l.maxAbove = Double.NaN;
                double maxBelow = 0;
                double plain = 0;
                // 줄의 항목마다 가상 교차축 크기를 구하고 기준선 정렬 항목은 따로 모은다.
                for (int k = 0; k < l.items.size(); k++) {
                    Item it = l.items.get(k);
                    hypotheticalCross(it);
                    if ("baseline".equals(it.align) && !it.autoCs && !it.autoCe) {
                        double base = it.box.baseline;
                        if (Double.isNaN(base)) {
                            base = BlockLayouter.firstBaseline(it.box);
                        }
                        if (Double.isNaN(base)) {
                            base = it.box.height;
                        }
                        it.above = it.cs + base;
                        l.maxAbove = Double.isNaN(l.maxAbove) ? it.above : Math.max(l.maxAbove, it.above);
                        maxBelow = Math.max(maxBelow, it.outerCross() - it.above);
                    } else {
                        plain = Math.max(plain, it.outerCross());
                    }
                }
                double span = Double.isNaN(l.maxAbove) ? 0 : l.maxAbove + maxBelow;
                l.cross = Math.max(plain, span);
            }
            double lineSum = crossGap * Math.max(0, lines.size() - 1);
            // 한 줄 컨테이너는 정해진 교차축 크기를 줄 크기로 쓰고 아니면 min/max 로 제한한다.
            if (!wrap && lines.size() == 1) {
                Line l = lines.get(0);
                if (row) {
                    l.cross = crossDefinite ? contH : Math.max(minH, Math.min(l.cross, maxH));
                } else {
                    l.cross = cw;
                }
            }
            // 줄의 교차축 크기를 모두 더한다.
            for (int i = 0; i < lines.size(); i++) {
                lineSum += lines.get(i).cross;
            }
            if (row) {
                crossSize = crossDefinite ? contH : Math.max(minH, Math.min(lineSum, maxH));
            } else {
                crossSize = cw;
            }
            double free = crossSize - lineSum;
            double lead = 0;
            double between = 0;
            if (wrap) {
                String[] ac = words(c.style, Prop.ALIGN_CONTENT);
                String k = distribution(ac, free, lines.size(), true);
                double n = lines.size();
                if ("stretch".equals(k)) {
                    // 남는 공간을 모든 줄에 똑같이 나눠 준다.
                    if (free > 0) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.get(i).cross += free / n;
                        }
                    }
                } else if ("end".equals(k)) {
                    lead = free;
                } else if ("center".equals(k)) {
                    lead = free / 2;
                } else if ("between".equals(k)) {
                    between = n > 1 ? free / (n - 1) : 0;
                } else if ("around".equals(k)) {
                    between = free / n;
                    lead = between / 2;
                } else if ("evenly".equals(k)) {
                    between = free / (n + 1);
                    lead = between;
                }
            }
            double pos = lead;
            // 줄의 교차축 시작 위치를 차례로 정한다.
            for (int i = 0; i < lines.size(); i++) {
                Line l = lines.get(i);
                l.crossPos = pos;
                pos += l.cross + crossGap + between;
            }
        }

        /**
         * Lays out an item at its used main size to find its hypothetical cross size (CSS Flexbox 9.4 step 7).
         * <p>
         * 항목을 사용 주축 크기로 배치해 가상 교차축 크기를 구한다(CSS Flexbox 9.4 의 7단계).
         *
         * @param it the item / 항목
         */
        private void hypotheticalCross(Item it) {
            LayoutBox b = it.box;
            if (row) {
                place(it, it.target, Double.NaN);
                it.crossContent = Math.max(0, b.height - BlockLayouter.vExtras(b));
            } else {
                place(it, it.crossW, it.target);
                it.crossContent = it.crossW;
            }
        }

        /**
         * Maps <code>justify-content</code> or <code>align-content</code> words to a distribution keyword: <code>start</code>,
         * <code>end</code>, <code>center</code>, <code>between</code>, <code>around</code>, <code>evenly</code> or <code>stretch</code>,
         * with the fallbacks for negative free space.
         * <p>
         * <code>justify-content</code> 나 <code>align-content</code> 단어를 분배 키워드로 바꾼다: <code>start</code>, <code>end</code>, <code>center</code>,
         * <code>between</code>, <code>around</code>, <code>evenly</code>, <code>stretch</code>. 남는 공간이 음수일 때의 대체도 적용한다.
         *
         * @param w       the words / 단어
         * @param free    the free space / 남는 공간
         * @param count   the number of items or lines / 항목이나 줄의 수
         * @param content true for <code>align-content</code> (cross axis) / <code>align-content</code>(교차축)이면 true
         * @return the keyword / 키워드
         */
        private String distribution(String[] w, double free, int count, boolean content) {
            String k = w[w.length - 1];
            boolean safe = false;
            // safe 키워드가 있는지 찾는다.
            for (int i = 0; i < w.length; i++) {
                if ("safe".equals(w[i])) {
                    safe = true;
                }
            }
            String r;
            if ("flex-start".equals(k)) {
                r = "start";
            } else if ("flex-end".equals(k)) {
                r = "end";
            } else if ("center".equals(k)) {
                r = "center";
            } else if ("space-between".equals(k)) {
                r = "between";
            } else if ("space-around".equals(k)) {
                r = "around";
            } else if ("space-evenly".equals(k)) {
                r = "evenly";
            } else if ("start".equals(k) || "self-start".equals(k)) {
                r = (content ? wrapRev : reverse) ? "end" : "start";
            } else if ("end".equals(k) || "self-end".equals(k)) {
                r = (content ? wrapRev : reverse) ? "start" : "end";
            } else if ("left".equals(k) || "right".equals(k)) {
                boolean leftStart = !mainMirror;
                r = ("left".equals(k) == leftStart) ? "start" : "end";
                if (!row || content) {
                    r = "start";
                }
            } else if (content && ("normal".equals(k) || "stretch".equals(k) || k.length() == 0)) {
                r = "stretch";
            } else {
                r = "start";
            }
            if (free < 0 || count <= 0) {
                // 남는 공간이 음수이면 between 은 start, around/evenly 는 center 로 바꾸고 safe 는 start 로 한다.
                if ("between".equals(r)) {
                    r = "start";
                } else if ("around".equals(r) || "evenly".equals(r)) {
                    r = "center";
                }
                if (safe && ("center".equals(r) || "end".equals(r))) {
                    r = "start";
                }
                if ("stretch".equals(r)) {
                    r = "start";
                }
            }
            return r;
        }

        /**
         * Aligns the items of a line across the axis (CSS Flexbox 9.4 step 11 and 9.6): auto margins, <code>stretch</code> (the item is
         * laid out again at the stretched size), start, end, center and baseline.
         * <p>
         * 줄의 항목을 교차축으로 정렬한다(CSS Flexbox 9.4 의 11단계와 9.6): auto margin, <code>stretch</code>(늘린 크기로 다시 배치), start, end, center, baseline.
         *
         * @param line the line / 줄
         */
        private void alignCross(Line line) {
            // 항목마다 줄 안에서 교차축 위치와 크기를 정한다.
            for (int i = 0; i < line.items.size(); i++) {
                Item it = line.items.get(i);
                LayoutBox b = it.box;
                double free = line.cross - it.outerCross();
                if (it.autoCs || it.autoCe) {
                    // auto 교차축 margin 이 있으면 정렬보다 먼저 남는 공간을 차지한다.
                    if (free > 0) {
                        if (it.autoCs && it.autoCe) {
                            it.cs = free / 2;
                            it.ce = free / 2;
                        } else if (it.autoCs) {
                            it.cs = free;
                        } else {
                            it.ce = free;
                        }
                    } else {
                        // 남는 공간이 없으면 시작 margin 은 0 이고 끝 margin 이 바깥 교차축 크기를 줄 크기에 맞춘다.
                        if (it.autoCs) {
                            it.cs = 0;
                        }
                        if (it.autoCe) {
                            it.ce = line.cross - it.cs - it.extraCross - it.crossContent;
                        }
                    }
                    it.crossPos = line.crossPos;
                    continue;
                }
                String a = it.align;
                if (it.safe && free < 0 && ("center".equals(a) || "end".equals(a))) {
                    a = "start";
                }
                if ("stretch".equals(a)) {
                    if (it.crossAuto && b.type != LayoutBox.Type.TABLE) {
                        double target = line.cross - it.cs - it.ce - it.extraCross;
                        double lo = row ? bl.minHeight(b, contH) : bl.minWidth(b, cw);
                        double hi = row ? bl.maxHeight(b, contH) : bl.maxWidth(b, cw);
                        target = Math.max(0, Math.max(lo, Math.min(target, hi)));
                        if (!same(target, it.crossContent)) {
                            it.crossContent = target;
                            if (row) {
                                place(it, it.target, target);
                            } else {
                                place(it, target, it.target);
                            }
                        }
                    }
                    it.crossPos = line.crossPos;
                } else if ("end".equals(a)) {
                    it.crossPos = line.crossPos + free;
                } else if ("center".equals(a)) {
                    it.crossPos = line.crossPos + free / 2;
                } else if ("baseline".equals(a)) {
                    it.crossPos = line.crossPos + (Double.isNaN(line.maxAbove) ? 0 : line.maxAbove - it.above);
                } else {
                    it.crossPos = line.crossPos;
                }
            }
        }

        /**
         * Distributes the free space of a line along the main axis (CSS Flexbox 9.5): auto margins take it first, then
         * <code>justify-content</code> places the items.
         * <p>
         * 줄의 남는 공간을 주축으로 나눈다(CSS Flexbox 9.5): auto margin 이 먼저 차지하고 그다음 <code>justify-content</code> 가 항목을 놓는다.
         *
         * @param line the line / 줄
         */
        private void justify(Line line) {
            List<Item> its = line.items;
            double used = mainGap * Math.max(0, its.size() - 1);
            int autos = 0;
            // 사용 크기의 바깥 크기 합과 주축 auto margin 의 수를 센다.
            for (int i = 0; i < its.size(); i++) {
                Item it = its.get(i);
                used += it.outerMain(it.target);
                autos += (it.autoMs ? 1 : 0) + (it.autoMe ? 1 : 0);
            }
            double free = mainSize - used;
            double lead = 0;
            double between = 0;
            if (autos > 0) {
                double each = free > 0 ? free / autos : 0;
                // 주축 auto margin 이 남는 공간을 똑같이 나눠 가진다(남는 공간이 없으면 0).
                for (int i = 0; i < its.size(); i++) {
                    Item it = its.get(i);
                    if (it.autoMs) {
                        it.ms = each;
                    }
                    if (it.autoMe) {
                        it.me = each;
                    }
                }
            } else {
                String k = distribution(words(c.style, Prop.JUSTIFY_CONTENT), free, its.size(), false);
                double n = its.size();
                if ("end".equals(k)) {
                    lead = free;
                } else if ("center".equals(k)) {
                    lead = free / 2;
                } else if ("between".equals(k)) {
                    between = n > 1 ? free / (n - 1) : 0;
                } else if ("around".equals(k)) {
                    between = free / n;
                    lead = between / 2;
                } else if ("evenly".equals(k)) {
                    between = free / (n + 1);
                    lead = between;
                }
            }
            double pos = lead;
            // 항목을 주축 시작부터 차례로 놓는다.
            for (int i = 0; i < its.size(); i++) {
                Item it = its.get(i);
                it.mainPos = pos;
                pos += it.outerMain(it.target) + mainGap + between;
            }
        }

        /**
         * Sets the final geometry of the items: sizes, margins and positions relative to the border box of the container, mirroring the
         * axes that run from the high coordinate to the low one.
         * <p>
         * 항목의 최종 기하를 정한다: 크기, margin, 컨테이너 border box 기준 위치. 큰 좌표에서 작은 좌표로 가는 축은 거울처럼 뒤집는다.
         *
         * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
         * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
         */
        private void apply(double contentLeft, double contentTop) {
            // 줄마다, 항목마다 물리적 위치를 계산해 상자에 쓴다.
            for (int i = 0; i < lines.size(); i++) {
                Line l = lines.get(i);
                // 줄의 항목마다 최종 위치와 margin 을 상자에 기록한다.
                for (int k = 0; k < l.items.size(); k++) {
                    Item it = l.items.get(k);
                    LayoutBox b = it.box;
                    double outerM = it.outerMain(it.target);
                    double outerC = it.outerCross();
                    double mainPhys = mainMirror ? mainSize - (it.mainPos + outerM) : it.mainPos;
                    double crossPhys = crossMirror ? crossSize - (it.crossPos + outerC) : it.crossPos;
                    double lowMain = mainMirror ? it.me : it.ms;
                    double highMain = mainMirror ? it.ms : it.me;
                    double lowCross = crossMirror ? it.ce : it.cs;
                    double highCross = crossMirror ? it.cs : it.ce;
                    if (row) {
                        b.marginLeft = lowMain;
                        b.marginRight = highMain;
                        b.marginTop = lowCross;
                        b.marginBottom = highCross;
                        b.lx = contentLeft + mainPhys + lowMain;
                        b.ly = contentTop + crossPhys + lowCross;
                    } else {
                        b.marginTop = lowMain;
                        b.marginBottom = highMain;
                        b.marginLeft = lowCross;
                        b.marginRight = highCross;
                        b.lx = contentLeft + crossPhys + lowCross;
                        b.ly = contentTop + mainPhys + lowMain;
                    }
                }
            }
        }

        /**
         * Gives the absolutely positioned children their static position: the content box of the container, aligned by
         * <code>justify-content</code> and by the <code>align-self</code> of the child, as if the child were the only flex item.
         * <p>
         * 절대 배치 자식에게 정적 위치를 준다: 컨테이너의 content box 를 <code>justify-content</code> 와 자식의 <code>align-self</code> 로 정렬한 곳이며 자식이 유일한
         * 플렉스 항목인 것처럼 본다.
         *
         * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
         * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
         */
        private void staticPositions(double contentLeft, double contentTop) {
            double ch = row ? crossSize : mainSize;
            String jk = distribution(words(c.style, Prop.JUSTIFY_CONTENT), 1, 1, false);
            // 절대 배치 자식마다 정적 위치 사각형과 정렬을 기록한다.
            for (int i = 0; i < absBoxes.size(); i++) {
                LayoutBox a = absBoxes.get(i);
                String[] aw = words(a.style, Prop.ALIGN_SELF);
                if ("auto".equals(aw[aw.length - 1]) || aw[aw.length - 1].length() == 0) {
                    aw = words(c.style, Prop.ALIGN_ITEMS);
                }
                String ak = alignKind(aw[aw.length - 1], false);
                a.staticX = contentLeft;
                a.staticY = contentTop;
                a.staticW = cw;
                a.staticH = ch;
                // 뒤집힌 축에서는 시작과 끝이 바뀐다.
                int mA = flipAlign(alignCode(jk), mainMirror);
                int cA = flipAlign(alignCode(ak), crossMirror);
                a.staticAlignX = row ? mA : cA;
                a.staticAlignY = row ? cA : mA;
            }
        }

        /**
         * Returns 0 for start, 1 for center and 2 for end alignment.
         * <p>
         * 시작 정렬은 0, 가운데는 1, 끝은 2 를 반환한다.
         *
         * @param k the keyword (start, end, center, around, evenly, ...) / 키워드
         * @return the code / 코드
         */
        private int alignCode(String k) {
            if ("center".equals(k) || "around".equals(k) || "evenly".equals(k)) {
                return 1;
            }
            return "end".equals(k) ? 2 : 0;
        }

        /**
         * Swaps start and end alignment if the axis is mirrored.
         * <p>
         * 축이 뒤집혀 있으면 시작과 끝 정렬을 바꾼다.
         *
         * @param code   the code of {@link #alignCode} / {@link #alignCode} 의 코드
         * @param mirror true if the axis is mirrored / 축이 뒤집혔으면 true
         * @return the physical code / 물리적 코드
         */
        private int flipAlign(int code, boolean mirror) {
            if (!mirror || code == 1) {
                return code;
            }
            return code == 0 ? 2 : 0;
        }

        /**
         * Returns the baseline of the container (relative to its border box): the baseline of the first item of the first line (an item
         * that is aligned by baseline is preferred), NaN if there is no item.
         * <p>
         * 컨테이너의 기준선(border box 기준)을 반환한다: 첫 줄 첫 항목의 기준선(기준선 정렬 항목이 우선). 항목이 없으면 NaN.
         *
         * @return the baseline or NaN / 기준선 또는 NaN
         */
        private double containerBaseline() {
            if (lines.isEmpty() || lines.get(0).items.isEmpty()) {
                return Double.NaN;
            }
            Line l = lines.get(0);
            Item pick = l.items.get(0);
            // 첫 줄에서 기준선 정렬 항목을 찾아 우선한다.
            for (int i = 0; i < l.items.size(); i++) {
                if ("baseline".equals(l.items.get(i).align) && !l.items.get(i).autoCs && !l.items.get(i).autoCe) {
                    pick = l.items.get(i);
                    break;
                }
            }
            LayoutBox b = pick.box;
            double base = b.baseline;
            if (Double.isNaN(base)) {
                base = BlockLayouter.firstBaseline(b);
            }
            if (Double.isNaN(base)) {
                base = b.height;
            }
            return b.ly + base;
        }
    }
}
