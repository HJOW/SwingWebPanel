package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssRaw;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Layout of grid containers (plan item P10-03, CSS Grid Layout Module Level 1, W3C CRD 2025-03-26, section numbers below are those of
 * that edition). The grid items are the children of the container box that {@link BoxBuilder} marked with {@link LayoutBox#gridItem}
 * (blockified, in order-modified document order, text runs become anonymous items).
 * <p>
 * Implemented: the explicit grid of <code>grid-template-columns</code> and <code>grid-template-rows</code> (lengths, percentages,
 * <code>fr</code>, <code>auto</code>, <code>min-content</code>, <code>max-content</code>, <code>minmax()</code>,
 * <code>fit-content()</code> with a length or a percentage, <code>repeat()</code> with a number, <code>auto-fill</code> and
 * <code>auto-fit</code> (7.2.3.2, empty <code>auto-fit</code> tracks collapse), line names) and <code>grid-template-areas</code> (7.3:
 * all rows need the same number of cells and every area must be a rectangle, else the declaration is invalid; areas give the lines
 * <code>name-start</code> and <code>name-end</code>); the placement by <code>grid-row-start</code>, <code>-end</code>,
 * <code>grid-column-start</code> and <code>-end</code> (8.3: line numbers, negative numbers, <code>span</code>, names with an
 * occurrence number, the implicit lines that count as having every name, conflict handling) with implicit tracks behind and in front
 * of the explicit grid sized by <code>grid-auto-rows</code> and <code>grid-auto-columns</code>; the auto-placement algorithm of 8.5
 * (<code>grid-auto-flow</code> <code>row</code>, <code>column</code>, <code>dense</code>, items locked to a row or column); the grid
 * track sizing algorithm of section 11 (initialize, the intrinsic sizes of the items with the distribution of the extra space
 * across the spanned tracks of 11.5.1 including the limits, the infinitely growable flag and the flexible tracks of spanning
 * items, the automatic minimum size of 6.6, maximize, expand the flexible tracks with and without a definite size including the
 * minimum and maximum height, stretch the <code>auto</code> tracks also up to <code>min-height</code>, min-content and max-content
 * sizing of the container); <code>gap</code> (also in the sizes of spanning items); the alignment of the items in their grid area
 * (<code>justify-self</code>, <code>align-self</code>, <code>justify-items</code>, <code>align-items</code>: stretch, normal, start,
 * end, center, <code>left</code>/<code>right</code>, <code>safe</code>, auto margins, replaced elements) and of the tracks in the
 * container (<code>justify-content</code>, <code>align-content</code>: start, end, center, space-between, space-around,
 * space-evenly, stretch); <code>direction:rtl</code> (the columns run from the right to the left); vertical writing modes (the
 * layout works in the logical axes of the frame of the box); absolutely positioned children (9: the static position aligned by
 * their own <code>justify-self</code>/<code>align-self</code>, and the grid area of their grid-placement properties as containing
 * block if the container is positioned); the baseline of the container and its intrinsic widths.
 * <p>
 * Not implemented (documented limits): subgrid and masonry (Levels 2 and 3), baseline alignment of items (<code>baseline</code> is
 * treated as <code>start</code>), the second pass of 11.1 that resolves the columns again after the rows if the min-content
 * contribution of an item changed with its height, track sizes that are <code>calc()</code> (treated as <code>auto</code>), named
 * spans (<code>span NAME</code> counts tracks), line names at the end of an auto-repeat group are not repeated, percentage
 * padding and margins of items resolve against the width of the container instead of the grid area, a grid area that is a
 * containing block for absolutely positioned descendants that are not direct children, <code>visibility:collapse</code>,
 * fragmentation of a grid container (it is not split between columns or pages).
 * <p>
 * 그리드 컨테이너의 레이아웃(계획 P10-03, CSS Grid Layout Module Level 1, W3C CRD 2025-03-26. 아래 절 번호는 그 판의 번호이다). 그리드 항목은 {@link BoxBuilder} 가
 * {@link LayoutBox#gridItem} 으로 표시한 컨테이너 상자의 자식이다(블록화, order 를 반영한 문서 순서, 텍스트 덩어리는 익명 항목).
 * <p>
 * 구현: <code>grid-template-columns</code>, <code>grid-template-rows</code> 의 명시적 그리드(길이, 백분율, <code>fr</code>, <code>auto</code>, <code>min-content</code>,
 * <code>max-content</code>, <code>minmax()</code>, 길이나 백분율을 쓴 <code>fit-content()</code>, 숫자/<code>auto-fill</code>/<code>auto-fit</code> 을 쓴
 * <code>repeat()</code>(7.2.3.2, 빈 <code>auto-fit</code> 트랙은 접힘), 선 이름)와 <code>grid-template-areas</code>(7.3: 모든 행의 칸 수가 같아야 하고 영역은 직사각형이어야 하며
 * 아니면 선언이 잘못이다. 영역이 <code>이름-start</code>, <code>이름-end</code> 선을 줌), <code>grid-row-start</code>, <code>-end</code>, <code>grid-column-start</code>,
 * <code>-end</code> 로 하는 배치(8.3: 선 번호, 음수, <code>span</code>, 몇 번째인지가 붙은 이름, 모든 이름을 가진 것으로 치는 암시적 선, 충돌 처리)와
 * <code>grid-auto-rows</code>, <code>grid-auto-columns</code> 로 크기가 정해지는 명시적 그리드 뒤와 앞의 암시적 트랙, 8.5 의 자동 배치 알고리즘(<code>grid-auto-flow</code>
 * <code>row</code>, <code>column</code>, <code>dense</code>, 행이나 열이 정해진 항목), 11 절의 그리드 트랙 크기 결정 알고리즘(초기화, 11.5.1 의 걸친 트랙에 추가 공간을 나누는 항목의
 * 고유 크기 풀이: 한계, infinitely growable 표시, 유연한 트랙에 걸친 항목, 6.6 의 자동 최소 크기, 최대화, 크기가 정해졌을 때와 아닐 때(최소/최대 높이 포함)의 유연한 트랙 확장,
 * <code>min-height</code> 까지 포함한 <code>auto</code> 트랙 늘리기, 컨테이너의 min-content/max-content 크기), <code>gap</code>(걸친 항목의 크기에도 반영), 그리드 영역 안 항목의
 * 정렬(<code>justify-self</code>, <code>align-self</code>, <code>justify-items</code>, <code>align-items</code>: stretch, normal, start, end, center, <code>left</code>/<code>right</code>,
 * <code>safe</code>, auto margin, 치환 요소)과 컨테이너 안 트랙의 정렬(<code>justify-content</code>, <code>align-content</code>: start, end, center, space-between,
 * space-around, space-evenly, stretch), <code>direction:rtl</code>(열이 오른쪽에서 왼쪽으로 놓임), 세로쓰기 모드(상자의 프레임의 논리적 축으로 배치), 절대 배치 자식(9: 자신의
 * <code>justify-self</code>/<code>align-self</code> 로 정렬한 정적 위치, 컨테이너가 위치 지정이면 그리드 배치 속성이 고른 그리드 영역이 포함 블록), 컨테이너의 기준선과 고유 너비.
 * <p>
 * 구현하지 않음(문서화된 한계): subgrid 와 masonry(Level 2, 3), 항목의 기준선 정렬(<code>baseline</code> 은 <code>start</code> 로 취급), 항목의 min-content 기여가 높이에 따라 변할 때
 * 행을 정한 뒤 열을 다시 푸는 11.1 의 둘째 패스, <code>calc()</code> 트랙 크기(<code>auto</code> 로 취급), 이름 있는 span(<code>span 이름</code> 은 트랙 수로만 센다), 자동 반복
 * 묶음 끝의 선 이름(반복하지 않음), 항목의 백분율 padding/margin 을 그리드 영역이 아닌 컨테이너 너비에 대해 풂, 직접 자식이 아닌 절대 배치 후손의 그리드 영역 포함 블록,
 * <code>visibility:collapse</code>, 그리드 컨테이너의 분할(열이나 쪽 사이에서 나누지 않음).
 */
final class GridLayouter {

    /** Track size kind: a length in pixels. 트랙 크기 종류: 픽셀 길이. */
    private static final int T_LEN = 0;
    /** Track size kind: a percentage of the container. 트랙 크기 종류: 컨테이너에 대한 백분율. */
    private static final int T_PCT = 1;
    /** Track size kind: <code>auto</code>. 트랙 크기 종류: <code>auto</code>. */
    private static final int T_AUTO = 2;
    /** Track size kind: <code>min-content</code>. 트랙 크기 종류: <code>min-content</code>. */
    private static final int T_MINC = 3;
    /** Track size kind: <code>max-content</code>. 트랙 크기 종류: <code>max-content</code>. */
    private static final int T_MAXC = 4;
    /** Track size kind: a flexible length (<code>fr</code>). 트랙 크기 종류: 유연한 길이(<code>fr</code>). */
    private static final int T_FR = 5;
    /** Track size kind: <code>fit-content()</code> (only as a maximum). 트랙 크기 종류: <code>fit-content()</code>(최대로만). */
    private static final int T_FIT = 6;
    /** The largest number of tracks of an axis (the specification allows to limit it). 한 축의 최대 트랙 수(규격이 제한을 허용한다). */
    private static final int MAX_TRACKS = 1000;
    /** Tolerance when comparing sizes. 크기 비교 허용 오차. */
    private static final double EPS = 0.001;
    /** Marks an unbounded growth limit. 한계가 없는 성장 한계를 나타낸다. */
    private static final double INF = Double.POSITIVE_INFINITY;
    /** Result of a line name search that found no line. 이름으로 선을 찾지 못했을 때의 결과. */
    private static final int NOT_FOUND = Integer.MIN_VALUE;
    /** Contribution to accommodate: the minimum contribution (the limited min-content contribution under an intrinsic size constraint). 수용할 기여: 최소 기여(고유 크기 제약 아래에서는 제한된 min-content 기여). */
    private static final int K_MINIMUM = 0;
    /** Contribution to accommodate: the min-content contribution. 수용할 기여: min-content 기여. */
    private static final int K_MIN = 1;
    /** Contribution to accommodate: the max-content contribution. 수용할 기여: max-content 기여. */
    private static final int K_MAX = 2;
    /** Contribution to accommodate: the limited max-content contribution. 수용할 기여: 제한된 max-content 기여. */
    private static final int K_LMAX = 3;
    /** Affected tracks: tracks whose minimum sizing function is intrinsic (<code>auto</code>, <code>min-content</code>, <code>max-content</code>). 영향받는 트랙: 최소 크기 함수가 고유 크기(<code>auto</code>, <code>min-content</code>, <code>max-content</code>)인 트랙. */
    private static final int A_INTRINSIC_MIN = 0;
    /** Affected tracks: tracks whose minimum sizing function is <code>min-content</code> or <code>max-content</code>. 영향받는 트랙: 최소 크기 함수가 <code>min-content</code> 나 <code>max-content</code> 인 트랙. */
    private static final int A_CONTENT_MIN = 1;
    /** Affected tracks: tracks whose minimum sizing function is <code>auto</code> or <code>max-content</code>. 영향받는 트랙: 최소 크기 함수가 <code>auto</code> 나 <code>max-content</code> 인 트랙. */
    private static final int A_AUTO_MAXC_MIN = 2;
    /** Affected tracks: tracks whose minimum sizing function is <code>max-content</code>. 영향받는 트랙: 최소 크기 함수가 <code>max-content</code> 인 트랙. */
    private static final int A_MAXC_MIN = 3;
    /** Affected tracks: tracks whose maximum sizing function is intrinsic (<code>auto</code>, <code>min-content</code>, <code>max-content</code>, <code>fit-content()</code>). 영향받는 트랙: 최대 크기 함수가 고유 크기인 트랙. */
    private static final int A_INTRINSIC_MAX = 4;
    /** Affected tracks: tracks whose maximum sizing function behaves as <code>max-content</code> (<code>max-content</code>, <code>auto</code>, <code>fit-content()</code>). 영향받는 트랙: 최대 크기 함수가 <code>max-content</code> 처럼 동작하는 트랙. */
    private static final int A_MAXC_MAX = 5;

    /** The block layouter that lays out the items. 항목을 배치하는 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the grid layouter.
     * <p>
     * 그리드 레이아웃을 만든다.
     *
     * @param bl the block layouter of this layout run / 이 레이아웃 실행의 블록 레이아웃
     */
    GridLayouter(BlockLayouter bl) {
        this.bl = bl;
    }

    /**
     * The occupied cells of the grid during auto-placement: small areas are stored cell by cell, large areas (more than
     * {@link #BIG_AREA} cells, which only strange stylesheets produce) as rectangles, so that an item that spans a thousand rows and
     * columns costs nothing.
     * <p>
     * 자동 배치 중 그리드의 점유된 칸: 작은 영역은 칸 단위로, 큰 영역({@link #BIG_AREA} 칸 초과. 이상한 스타일시트만 만든다)은 사각형으로 저장하므로 천 개의 행과 열에 걸친
     * 항목도 비용이 들지 않는다.
     */
    private static final class Occupancy {
        /** The largest area (in cells) that is stored cell by cell. 칸 단위로 저장하는 가장 큰 영역(칸 수). */
        static final long BIG_AREA = 256;
        /** The occupied cells of small areas: row in the upper and column in the lower 32 bits. 작은 영역의 점유된 칸: 위 32 비트에 행, 아래 32 비트에 열. */
        final Set<Long> cells = new HashSet<Long>();
        /** The rectangles {row start, row end, column start, column end} of the large areas (end exclusive). 큰 영역의 사각형 {행 시작, 행 끝, 열 시작, 열 끝}(끝은 포함하지 않음). */
        final List<int[]> bigs = new ArrayList<int[]>();

        /**
         * Marks a rectangle of cells as occupied.
         * <p>
         * 칸의 사각형을 점유로 표시한다.
         *
         * @param r0 first row / 첫 행
         * @param r1 end row (exclusive) / 끝 행(포함하지 않음)
         * @param c0 first column / 첫 열
         * @param c1 end column (exclusive) / 끝 열(포함하지 않음)
         */
        void add(int r0, int r1, int c0, int c1) {
            if (r1 <= r0 || c1 <= c0) {
                return;
            }
            if ((long) (r1 - r0) * (c1 - c0) > BIG_AREA) {
                bigs.add(new int[] { r0, r1, c0, c1 });
                return;
            }
            // 영역의 칸을 하나씩 표시한다.
            for (int r = r0; r < r1; r++) {
                for (int c = c0; c < c1; c++) {
                    cells.add(Long.valueOf(((long) r << 32) | c));
                }
            }
        }

        /**
         * Tells whether a rectangle of cells overlaps an occupied cell.
         * <p>
         * 칸의 사각형이 점유된 칸과 겹치는지 알려 준다.
         *
         * @param r0 first row / 첫 행
         * @param r1 end row (exclusive) / 끝 행(포함하지 않음)
         * @param c0 first column / 첫 열
         * @param c1 end column (exclusive) / 끝 열(포함하지 않음)
         * @return true if one of the cells is occupied / 점유된 칸이 하나라도 있으면 true
         */
        boolean overlaps(int r0, int r1, int c0, int c1) {
            if (r1 <= r0 || c1 <= c0) {
                return false;
            }
            // 큰 영역의 사각형과 겹치는지 본다.
            for (int i = 0; i < bigs.size(); i++) {
                int[] b = bigs.get(i);
                if (r0 < b[1] && b[0] < r1 && c0 < b[3] && b[2] < c1) {
                    return true;
                }
            }
            if ((long) (r1 - r0) * (c1 - c0) <= cells.size()) {
                // 사각형의 칸이 점유된 칸보다 적으면 칸을 하나씩 찾아본다.
                for (int r = r0; r < r1; r++) {
                    for (int c = c0; c < c1; c++) {
                        if (cells.contains(Long.valueOf(((long) r << 32) | c))) {
                            return true;
                        }
                    }
                }
                return false;
            }
            // 점유된 칸이 더 적으면 점유된 칸이 사각형 안에 있는지 본다.
            for (Long key : cells) {
                int r = (int) (key.longValue() >>> 32);
                int c = (int) key.longValue();
                if (r >= r0 && r < r1 && c >= c0 && c < c1) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The sizing functions of a track. 트랙의 크기 결정 함수. */
    private static final class Def {
        /** Kind of the minimum sizing function. 최소 크기 함수의 종류. */
        int minKind = T_AUTO;
        /** Value of the minimum (pixels or percent). 최소 값(픽셀 또는 백분율). */
        double minVal;
        /** Kind of the maximum sizing function. 최대 크기 함수의 종류. */
        int maxKind = T_AUTO;
        /** Value of the maximum (pixels, percent, fr factor or the limit of fit-content). 최대 값(픽셀, 백분율, fr 계수, fit-content 의 한계). */
        double maxVal;
        /** True if the argument of <code>fit-content()</code> is a percentage. <code>fit-content()</code> 의 인수가 백분율이면 true. */
        boolean fitPct;
    }

    /** A list of track definitions with the names of the lines between them. 트랙 정의의 목록과 그 사이 선의 이름. */
    private static final class TrackList {
        /** The tracks. 트랙. */
        final List<Def> defs = new ArrayList<Def>();
        /** The names of the lines (one more entry than tracks). 선의 이름(트랙보다 하나 많다). */
        final List<List<String>> names = new ArrayList<List<String>>();
        /** Index of the first track of the auto-repeat group or -1. 자동 반복 묶음의 첫 트랙 번호. 없으면 -1. */
        int arStart = -1;
        /** Number of tracks of the auto-repeat group. 자동 반복 묶음의 트랙 수. */
        int arLen;
        /** True for <code>auto-fit</code>. <code>auto-fit</code> 이면 true. */
        boolean arFit;
        /** The names waiting for the next track. 다음 트랙을 기다리는 이름. */
        List<String> pending = new ArrayList<String>();
    }

    /** A track of the used grid. 사용 그리드의 트랙. */
    private static final class Track {
        /** The sizing functions. 크기 결정 함수. */
        final Def def;
        /** Kind of the minimum sizing function in use (percentages resolved or turned into <code>auto</code>, collapsed tracks fixed). 쓰는 최소 크기 함수의 종류(백분율은 풀었거나 <code>auto</code> 로 바꿈, 접힌 트랙은 고정). */
        int minKind;
        /** Value of the minimum sizing function in use (pixels). 쓰는 최소 크기 함수의 값(픽셀). */
        double minVal;
        /** Kind of the maximum sizing function in use. 쓰는 최대 크기 함수의 종류. */
        int maxKind;
        /** Value of the maximum sizing function in use (pixels, the fr factor or the argument of fit-content). 쓰는 최대 크기 함수의 값(픽셀, fr 계수, fit-content 의 인수). */
        double maxVal;
        /** Base size. 기본 크기. */
        double base;
        /** Growth limit. 성장 한계. */
        double limit;
        /** True while the growth limit may still grow although it is finite (CSS Grid 11.5, "infinitely growable"). 성장 한계가 유한해도 더 커질 수 있는 동안 true(CSS Grid 11.5 의 "infinitely growable"). */
        boolean infGrowable;
        /** Used size. 사용 크기. */
        double size;
        /** Start position in the content box of the container. 컨테이너 content box 안 시작 위치. */
        double pos;
        /** True for an empty track of <code>auto-fit</code> that is collapsed. 접힌 <code>auto-fit</code> 빈 트랙이면 true. */
        boolean collapsed;

        /**
         * Creates a track.
         * <p>
         * 트랙을 만든다.
         *
         * @param def the sizing functions / 크기 결정 함수
         */
        Track(Def def) {
            this.def = def;
        }
    }

    /** The state of one grid item. 그리드 항목 하나의 상태. */
    private static final class Item {
        /** The box. 상자. */
        final LayoutBox box;
        /** First line (0-based) per axis, 0 = columns, 1 = rows. 축별 시작 선(0부터). 0 은 열, 1 은 행. */
        final int[] start = new int[2];
        /** Number of tracks per axis. 축별 트랙 수. */
        final int[] span = { 1, 1 };
        /** True if the start line of the axis is definite. 축의 시작 선이 정해졌으면 true. */
        final boolean[] definite = new boolean[2];
        /** Minimum content contribution (outer) per axis. 축별 최소 content 기여(바깥). */
        final double[] minC = new double[2];
        /** Maximum content contribution (outer) per axis. 축별 최대 content 기여(바깥). */
        final double[] maxC = new double[2];
        /** Minimum contribution (outer) per axis: the smallest outer size the item can have (CSS Grid 11.5). 축별 최소 기여(바깥): 항목이 가질 수 있는 가장 작은 바깥 크기(CSS Grid 11.5). */
        final double[] minimumC = new double[2];
        /** True once the item was laid out. 한 번이라도 배치했으면 true. */
        boolean laid;
        /** Content width of the last layout. 마지막 배치의 content 너비. */
        double laidW = Double.NaN;
        /** Forced content height of the last layout. 마지막 배치의 강제 content 높이. */
        double laidH = Double.NaN;
        /** Containing block height of the last layout (NaN if indefinite). 마지막 배치의 포함 블록 높이(정해지지 않았으면 NaN). */
        double laidCb = Double.NaN;
        /** True for an anonymous item. 익명 항목이면 true. */
        boolean anon;
        /** Used horizontal margins (left, right) and vertical margins (top, bottom). 사용 margin(왼쪽, 오른쪽, 위, 아래). */
        final double[] margin = new double[4];
        /** True for the auto margins (left, right, top, bottom). auto margin 이면 true(왼쪽, 오른쪽, 위, 아래). */
        final boolean[] autoMargin = new boolean[4];

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param box the box / 상자
         */
        Item(LayoutBox box) {
            this.box = box;
        }
    }

    /**
     * Lays out the items of a grid container and returns the height of its content box.
     * <p>
     * 그리드 컨테이너의 항목을 배치하고 content box 의 높이를 반환한다.
     *
     * @param c           the container / 컨테이너
     * @param cw          content width / content 너비
     * @param specH       specified content height or NaN / 지정된 content 높이 또는 NaN
     * @param minH        minimum content height / 최소 content 높이
     * @param maxH        maximum content height / 최대 content 높이
     * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
     * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
     * @return height of the content / content 높이
     */
    double layout(LayoutBox c, double cw, double specH, double minH, double maxH, double contentLeft, double contentTop) {
        Run r = new Run(c, cw, Double.isNaN(specH) ? Double.NaN : Math.max(minH, Math.min(specH, maxH)), minH, maxH);
        return r.run(contentLeft, contentTop);
    }

    /**
     * Returns the min-content or max-content width of a grid container: the width of the columns when the tracks are sized to their
     * min-content or max-content contributions.
     * <p>
     * 그리드 컨테이너의 min-content 또는 max-content 너비를 반환한다: 트랙을 min-content 나 max-content 기여로 정했을 때 열의 너비.
     *
     * @param c   the container / 컨테이너
     * @param max true for max-content / max-content 이면 true
     * @return the width / 너비
     */
    double intrinsic(LayoutBox c, boolean max) {
        Run r = new Run(c, Double.NaN, Double.NaN, 0, INF);
        return r.intrinsicWidth(max);
    }

    /**
     * Returns the lower-case words of a keyword-valued property.
     * <p>
     * 키워드 값 속성의 소문자 단어를 반환한다.
     *
     * @param s  the style / 스타일
     * @param id the property / 속성
     * @return the words / 단어
     */
    private static String[] words(ComputedStyle s, int id) {
        return FlexLayouter.words(s, id);
    }

    /** The state of one layout of a grid container. 그리드 컨테이너 한 번의 배치 상태. */
    private final class Run {
        /** The container. 컨테이너. */
        final LayoutBox c;
        /** Content width (NaN while measuring the intrinsic width). content 너비(고유 너비를 재는 동안은 NaN). */
        final double cw;
        /** Definite content height or NaN. 정해진 content 높이 또는 NaN. */
        final double contH;
        /** Minimum content height. 최소 content 높이. */
        final double minH;
        /** Maximum content height. 최대 content 높이. */
        final double maxH;
        /** The grid items. 그리드 항목. */
        final List<Item> items = new ArrayList<Item>();
        /** The absolutely positioned children. 절대 배치 자식. */
        final List<LayoutBox> absBoxes = new ArrayList<LayoutBox>();
        /** Gaps: [0] between columns, [1] between rows. gap: [0] 열 사이, [1] 행 사이. */
        final double[] gap = new double[2];
        /** The tracks: [0] columns, [1] rows. 트랙: [0] 열, [1] 행. */
        Track[][] tracks = new Track[2][];
        /** The explicit track definitions: [0] columns, [1] rows. 명시적 트랙 정의: [0] 열, [1] 행. */
        final TrackList[] lists = new TrackList[2];
        /** The number of explicit tracks per axis after the repeats were expanded. 반복을 펼친 뒤 축별 명시적 트랙 수. */
        final int[] explicit = new int[2];
        /** The number of implicit tracks in front of the explicit grid per axis (negative line numbers beyond the explicit grid create them). 축별 명시적 그리드 앞의 암시적 트랙 수(명시적 그리드 밖의 음수 선 번호가 만든다). */
        final int[] shift = new int[2];
        /** The names of the lines per axis including the implicit names of the named areas, indexed by explicit line (0-based, before the shift). 이름 있는 영역의 암시적 이름을 포함한 축별 선 이름. 명시적 선 번호(0부터, 이동 전)로 찾는다. */
        final List<List<List<String>>> lineNames = new ArrayList<List<List<String>>>();
        /** True for <code>direction:rtl</code>: the columns run from the right to the left. <code>direction:rtl</code> 이면 true: 열이 오른쪽에서 왼쪽으로 놓인다. */
        boolean rtl;
        /** The named areas: name, then {row start, row end, column start, column end} (0-based lines). 이름 있는 영역: 이름과 {행 시작, 행 끝, 열 시작, 열 끝}(0부터 선). */
        final List<Object[]> areas = new ArrayList<Object[]>();

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param c     the container / 컨테이너
         * @param cw    content width or NaN / content 너비 또는 NaN
         * @param contH definite content height or NaN / 정해진 content 높이 또는 NaN
         * @param minH  minimum content height / 최소 content 높이
         * @param maxH  maximum content height / 최대 content 높이
         */
        Run(LayoutBox c, double cw, double contH, double minH, double maxH) {
            this.c = c;
            this.cw = cw;
            this.contH = contH;
            this.minH = minH;
            this.maxH = maxH;
        }

        /**
         * Returns the size of the content box along an axis (NaN if indefinite).
         * <p>
         * 축 방향 content box 의 크기를 반환한다(정해지지 않았으면 NaN).
         *
         * @param axis 0 columns (width), 1 rows (height) / 0 열(너비), 1 행(높이)
         * @return the size or NaN / 크기 또는 NaN
         */
        double avail(int axis) {
            return axis == 0 ? cw : contH;
        }

        /**
         * Collects the items and the absolutely positioned children and resolves the gaps.
         * <p>
         * 항목과 절대 배치 자식을 모으고 gap 을 푼다.
         */
        void collect() {
            ComputedStyle cs = c.style;
            gap[0] = FlexLayouter.gap(cs, Prop.COLUMN_GAP, cw);
            gap[1] = FlexLayouter.gap(cs, Prop.ROW_GAP, contH);
            rtl = "rtl".equals(Css.kw(cs, Prop.DIRECTION));
            // 자식 중 그리드 항목은 준비하고 절대 배치 상자는 따로 모은다.
            for (int i = 0; i < c.children.size(); i++) {
                LayoutBox b = c.children.get(i);
                if (b.gridItem) {
                    items.add(prepare(b));
                } else if (b.isAbsolutelyPositioned()) {
                    absBoxes.add(b);
                }
            }
        }

        /**
         * Prepares an item: its edges and its auto margins.
         * <p>
         * 항목을 준비한다: 가장자리와 auto margin.
         *
         * @param b the item box / 항목 상자
         * @return the item state / 항목 상태
         */
        Item prepare(LayoutBox b) {
            Item it = new Item(b);
            it.anon = b.type == LayoutBox.Type.ANONYMOUS_BLOCK;
            double base = Double.isNaN(cw) ? 0 : cw;
            int auto = bl.edges(b, base);
            int fr = BlockLayouter.frameOf(b);
            it.autoMargin[0] = (auto & 1) != 0;
            it.autoMargin[1] = (auto & 2) != 0;
            it.autoMargin[2] = !it.anon && b.style.isAuto(Css.side(Css.MARGIN, fr, 0));
            it.autoMargin[3] = !it.anon && b.style.isAuto(Css.side(Css.MARGIN, fr, 2));
            it.margin[0] = b.marginLeft;
            it.margin[1] = b.marginRight;
            it.margin[2] = b.marginTop;
            it.margin[3] = b.marginBottom;
            return it;
        }

        /**
         * Runs the layout algorithm.
         * <p>
         * 레이아웃 알고리즘을 실행한다.
         *
         * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
         * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
         * @return height of the content / content 높이
         */
        double run(double contentLeft, double contentTop) {
            collect();
            buildGrid();
            sizeColumnsAndRows();
            double usedW = trackExtent(0);
            double usedH = trackExtent(1);
            double outerH = Double.isNaN(contH) ? Math.max(minH, Math.min(usedH, maxH)) : contH;
            double[] lead = distributeFree(0, cw, usedW);
            double[] leadY = distributeFree(1, outerH, usedH);
            position(0, lead);
            position(1, leadY);
            // 항목을 그리드 영역에 정렬해 놓는다.
            for (int i = 0; i < items.size(); i++) {
                placeItem(items.get(i), contentLeft, contentTop);
            }
            staticPositions(contentLeft, contentTop, outerH);
            absoluteContainingBlocks(contentLeft, contentTop);
            c.baseline = containerBaseline();
            return outerH;
        }

        /**
         * Computes the intrinsic width of the container.
         * <p>
         * 컨테이너의 고유 너비를 계산한다.
         *
         * @param max true for max-content / max-content 이면 true
         * @return the width / 너비
         */
        double intrinsicWidth(boolean max) {
            collect();
            buildGrid();
            sizeAxis(0, Double.NaN, max ? 2 : 1);
            double sum = 0;
            int n = 0;
            // 접히지 않은 트랙의 크기와 gap 을 더한다.
            for (int i = 0; i < tracks[0].length; i++) {
                if (!tracks[0][i].collapsed) {
                    sum += tracks[0][i].size;
                    n++;
                }
            }
            return sum + gap[0] * Math.max(0, n - 1);
        }

        /**
         * Builds the explicit grid, places the items and creates the tracks (explicit and implicit).
         * <p>
         * 명시적 그리드를 만들고 항목을 배치하고 트랙(명시적과 암시적)을 만든다.
         */
        void buildGrid() {
            ComputedStyle cs = c.style;
            lists[0] = parseTemplate(cs.get(Prop.GRID_TEMPLATE_COLUMNS));
            lists[1] = parseTemplate(cs.get(Prop.GRID_TEMPLATE_ROWS));
            expandAutoRepeat(0);
            expandAutoRepeat(1);
            explicit[0] = lists[0].defs.size();
            explicit[1] = lists[1].defs.size();
            parseAreas(cs.get(Prop.GRID_TEMPLATE_AREAS));
            buildLineNames();
            placeItems();
            // 항목이 놓인 범위까지 암시적 트랙을 더해 축마다 트랙 배열을 만든다.
            for (int axis = 0; axis < 2; axis++) {
                int count = shift[axis] + explicit[axis];
                // 항목이 놓인 끝 선까지 트랙 수를 늘린다.
                for (int i = 0; i < items.size(); i++) {
                    Item it = items.get(i);
                    count = Math.max(count, it.start[axis] + it.span[axis]);
                }
                count = Math.min(count, MAX_TRACKS);
                tracks[axis] = new Track[count];
                List<Def> autoDefs = parseAutoTracks(cs.get(axis == 0 ? Prop.GRID_AUTO_COLUMNS : Prop.GRID_AUTO_ROWS));
                int sized = lists[axis].defs.size();
                // 템플릿이 크기를 정한 트랙은 그 정의를, 그 밖의 트랙은 자동 트랙 정의를 쓴다.
                for (int i = 0; i < count; i++) {
                    int ex = i - shift[axis];
                    Def d;
                    if (ex >= 0 && ex < sized) {
                        d = lists[axis].defs.get(ex);
                    } else if (ex >= sized) {
                        // 명시적으로 크기가 정해진 마지막 트랙 뒤의 첫 트랙이 첫 크기를 받고 앞으로 반복한다.
                        d = autoDefs.get((ex - sized) % autoDefs.size());
                    } else {
                        // 명시적 그리드 앞의 트랙은 마지막 암시적 트랙이 마지막 크기를 받고 뒤로 반복한다.
                        d = autoDefs.get(autoDefs.size() - 1 - ((-ex - 1) % autoDefs.size()));
                    }
                    tracks[axis][i] = new Track(d);
                }
            }
            collapseEmptyFitTracks();
        }

        /**
         * Builds the names of the lines of both axes: the names of the template plus the implicit names <code>NAME-start</code> and
         * <code>NAME-end</code> of the named areas (CSS Grid 7.3).
         * <p>
         * 두 축의 선 이름을 만든다: 템플릿의 이름과 이름 있는 영역의 암시적 이름 <code>이름-start</code>, <code>이름-end</code>(CSS Grid 7.3).
         */
        void buildLineNames() {
            lineNames.clear();
            // 축마다 템플릿의 선 이름을 복사하고 영역의 암시적 이름을 더한다.
            for (int axis = 0; axis < 2; axis++) {
                List<List<String>> src = lists[axis].names;
                List<List<String>> nm = new ArrayList<List<String>>();
                // 템플릿의 선 이름을 복사한다.
                for (int i = 0; i < src.size(); i++) {
                    nm.add(new ArrayList<String>(src.get(i)));
                }
                // 영역이 만든 선까지 목록을 채운다.
                while (nm.size() < explicit[axis] + 1) {
                    nm.add(new ArrayList<String>());
                }
                int idx = axis == 1 ? 0 : 2;
                // 영역마다 시작 선과 끝 선에 이름을 붙인다.
                for (int i = 0; i < areas.size(); i++) {
                    String an = (String) areas.get(i)[0];
                    int[] r = (int[]) areas.get(i)[1];
                    nm.get(r[idx]).add(an + "-start");
                    nm.get(r[idx + 1]).add(an + "-end");
                }
                lineNames.add(nm);
            }
        }

        /**
         * Collapses the empty tracks that came from <code>auto-fit</code>.
         * <p>
         * <code>auto-fit</code> 에서 나온 빈 트랙을 접는다.
         */
        void collapseEmptyFitTracks() {
            // 축마다 자동 반복(auto-fit)으로 만든 트랙 중 항목이 없는 것을 접는다.
            for (int axis = 0; axis < 2; axis++) {
                TrackList tl = lists[axis];
                if (!tl.arFit || tl.arStart < 0) {
                    continue;
                }
                int from = shift[axis] + tl.arStart;
                int to = Math.min(tracks[axis].length, from + tl.arLen * arCount(axis));
                // 자동 반복으로 만든 트랙을 차례로 보며 항목이 없는 트랙을 접는다.
                for (int t = from; t < to; t++) {
                    boolean used = false;
                    // 항목 중 이 트랙에 걸친 것이 있는지 찾는다.
                    for (int i = 0; i < items.size() && !used; i++) {
                        Item it = items.get(i);
                        used = it.start[axis] <= t && t < it.start[axis] + it.span[axis];
                    }
                    tracks[axis][t].collapsed = !used;
                }
            }
        }

        /**
         * Number of repetitions of the auto-repeat group of an axis after expansion.
         * <p>
         * 확장한 뒤 축의 자동 반복 묶음의 반복 횟수.
         *
         * @param axis the axis / 축
         * @return the count / 횟수
         */
        int arCount(int axis) {
            return repeatCounts[axis];
        }

        /** The used number of repetitions of the auto-repeat groups. 자동 반복 묶음의 사용 반복 횟수. */
        final int[] repeatCounts = new int[2];

        /**
         * Parses a <code>grid-template-columns</code> or <code>-rows</code> value into track definitions and line names.
         * <p>
         * <code>grid-template-columns</code> 나 <code>-rows</code> 값을 트랙 정의와 선 이름으로 해석한다.
         *
         * @param v the computed value / 계산 값
         * @return the tracks / 트랙
         */
        TrackList parseTemplate(CssValue v) {
            TrackList tl = new TrackList();
            if (v != null && !v.isKeyword("none")) {
                List<CssValue> flat = new ArrayList<CssValue>();
                if (v instanceof CssList) {
                    flat.addAll(((CssList) v).getItems());
                } else {
                    flat.add(v);
                }
                appendTracks(flat, tl, true);
            }
            tl.names.add(tl.pending);
            tl.pending = new ArrayList<String>();
            return tl;
        }

        /**
         * Appends the tracks and the line names of a list of values (the argument of <code>repeat()</code> or the whole value).
         * <p>
         * 값 목록(<code>repeat()</code> 의 인수 또는 값 전체)의 트랙과 선 이름을 덧붙인다.
         *
         * @param vals the values / 값
         * @param tl   receives the tracks / 트랙을 받는다
         * @param top  true for the top level list, where the auto-repeat is allowed / 자동 반복이 허용되는 최상위 목록이면 true
         */
        void appendTracks(List<CssValue> vals, TrackList tl, boolean top) {
            // 값을 차례로 보며 선 이름은 대기시키고 트랙 크기는 트랙으로 더한다.
            for (int i = 0; i < vals.size() && tl.defs.size() < MAX_TRACKS; i++) {
                CssValue v = vals.get(i);
                if (v instanceof CssRaw) {
                    tl.pending.addAll(names(v));
                } else if (v instanceof CssFunctionValue && ((CssFunctionValue) v).isNamed("repeat")) {
                    appendRepeat((CssFunctionValue) v, tl, top);
                } else {
                    Def d = trackSize(v);
                    tl.names.add(tl.pending);
                    tl.pending = new ArrayList<String>();
                    tl.defs.add(d);
                }
            }
        }

        /**
         * Appends the tracks of a <code>repeat()</code>.
         * <p>
         * <code>repeat()</code> 의 트랙을 덧붙인다.
         *
         * @param f   the function / 함수
         * @param tl  receives the tracks / 트랙을 받는다
         * @param top true if auto-repeat is allowed / 자동 반복이 허용되면 true
         */
        void appendRepeat(CssFunctionValue f, TrackList tl, boolean top) {
            List<CssValue> a = f.getArguments();
            if (a.size() < 2) {
                return;
            }
            List<CssValue> inner = new ArrayList<CssValue>();
            // 첫 인수 뒤의 인수는 모두 반복할 트랙 목록이다(목록 하나이거나 값 여러 개).
            for (int i = 1; i < a.size(); i++) {
                if (a.get(i) instanceof CssList) {
                    inner.addAll(((CssList) a.get(i)).getItems());
                } else {
                    inner.add(a.get(i));
                }
            }
            CssValue count = a.get(0);
            if (count instanceof CssNumber) {
                int n = (int) Math.max(1, Math.min(MAX_TRACKS, ((CssNumber) count).getValue()));
                // 숫자만큼 같은 목록을 반복해 더한다.
                for (int k = 0; k < n && tl.defs.size() < MAX_TRACKS; k++) {
                    appendTracks(inner, tl, false);
                }
            } else if (count instanceof CssKeyword && top && tl.arStart < 0) {
                int before = tl.defs.size();
                tl.arFit = "auto-fit".equals(((CssKeyword) count).getName());
                tl.arStart = before;
                appendTracks(inner, tl, false);
                tl.arLen = tl.defs.size() - before;
            }
        }

        /**
         * Returns the names of a <code>[name ...]</code> value.
         * <p>
         * <code>[이름 ...]</code> 값의 이름을 반환한다.
         *
         * @param v the value / 값
         * @return the names / 이름
         */
        List<String> names(CssValue v) {
            List<String> out = new ArrayList<String>();
            String t = v.toCssText().trim();
            if (t.startsWith("[")) {
                t = t.substring(1);
            }
            if (t.endsWith("]")) {
                t = t.substring(0, t.length() - 1);
            }
            String[] w = t.trim().split("\\s+");
            // 공백으로 나뉜 이름을 모은다.
            for (int i = 0; i < w.length; i++) {
                if (w[i].length() > 0) {
                    out.add(w[i]);
                }
            }
            return out;
        }

        /**
         * Parses a track size: a breadth, <code>minmax()</code> or <code>fit-content()</code>.
         * <p>
         * 트랙 크기를 해석한다: 크기 하나, <code>minmax()</code>, <code>fit-content()</code>.
         *
         * @param v the value / 값
         * @return the sizing functions / 크기 결정 함수
         */
        Def trackSize(CssValue v) {
            Def d = new Def();
            if (v instanceof CssFunctionValue && ((CssFunctionValue) v).isNamed("minmax") && ((CssFunctionValue) v).getArguments().size() == 2) {
                List<CssValue> a = ((CssFunctionValue) v).getArguments();
                breadth(a.get(0), d, true);
                breadth(a.get(1), d, false);
                if (d.minKind == T_FR) {
                    d.minKind = T_AUTO;
                }
            } else if (v instanceof CssFunctionValue && ((CssFunctionValue) v).isNamed("fit-content") && ((CssFunctionValue) v).getArguments().size() == 1) {
                CssValue l = ((CssFunctionValue) v).getArguments().get(0);
                d.minKind = T_AUTO;
                d.maxKind = T_FIT;
                if (l instanceof CssPercentage) {
                    d.maxVal = ((CssPercentage) l).getValue();
                    d.fitPct = true;
                } else {
                    d.maxVal = l instanceof CssLength ? ((CssLength) l).getValue() : INF;
                }
            } else {
                breadth(v, d, true);
                breadth(v, d, false);
                if (d.minKind == T_FR) {
                    d.minKind = T_AUTO;
                }
            }
            return d;
        }

        /**
         * Reads a track breadth into the minimum or the maximum sizing function.
         * <p>
         * 트랙 크기를 최소나 최대 크기 함수로 읽는다.
         *
         * @param v   the breadth / 크기
         * @param d   receives the function / 함수를 받는다
         * @param min true for the minimum / 최소이면 true
         */
        void breadth(CssValue v, Def d, boolean min) {
            int kind = T_AUTO;
            double val = 0;
            if (v instanceof CssLength) {
                kind = T_LEN;
                val = ((CssLength) v).getValue();
            } else if (v instanceof CssPercentage) {
                kind = T_PCT;
                val = ((CssPercentage) v).getValue();
            } else if (v instanceof CssDimension && "fr".equals(((CssDimension) v).getUnit())) {
                kind = T_FR;
                val = ((CssDimension) v).getValue();
            } else if (v.isKeyword("min-content")) {
                kind = T_MINC;
            } else if (v.isKeyword("max-content")) {
                kind = T_MAXC;
            }
            if (min) {
                d.minKind = kind;
                d.minVal = val;
            } else {
                d.maxKind = kind;
                d.maxVal = val;
            }
        }

        /**
         * Parses <code>grid-auto-columns</code> or <code>grid-auto-rows</code>: the sizes of the implicit tracks, repeated.
         * <p>
         * <code>grid-auto-columns</code> 나 <code>grid-auto-rows</code> 를 해석한다: 암시적 트랙의 크기(반복해서 쓴다).
         *
         * @param v the value / 값
         * @return the size definitions, never empty / 크기 정의(비어 있지 않음)
         */
        List<Def> parseAutoTracks(CssValue v) {
            List<Def> out = new ArrayList<Def>();
            if (v instanceof CssList) {
                List<CssValue> l = ((CssList) v).getItems();
                // 목록의 값마다 트랙 크기 하나이다.
                for (int i = 0; i < l.size(); i++) {
                    out.add(trackSize(l.get(i)));
                }
            } else if (v != null) {
                out.add(trackSize(v));
            }
            if (out.isEmpty()) {
                out.add(new Def());
            }
            return out;
        }

        /**
         * Expands the auto-repeat group of an axis to the number of repetitions that fits the container (CSS Grid 7.2.3.2).
         * <p>
         * 축의 자동 반복 묶음을 컨테이너에 들어가는 반복 횟수로 펼친다(CSS Grid 7.2.3.2).
         *
         * @param axis the axis / 축
         */
        void expandAutoRepeat(int axis) {
            TrackList tl = lists[axis];
            repeatCounts[axis] = 0;
            if (tl.arStart < 0 || tl.arLen == 0) {
                return;
            }
            double avail = avail(axis);
            double fixed = 0;
            double group = 0;
            // 반복 묶음 밖의 트랙과 묶음 안의 트랙의 (최대가 길이이면 그 값, 아니면 최소) 크기를 더한다.
            for (int i = 0; i < tl.defs.size(); i++) {
                Def d = tl.defs.get(i);
                double s = d.maxKind == T_LEN ? d.maxVal : (d.minKind == T_LEN ? d.minVal : 0);
                if (i >= tl.arStart && i < tl.arStart + tl.arLen) {
                    group += s;
                } else {
                    fixed += s;
                }
            }
            int outside = tl.defs.size() - tl.arLen;
            int n = 1;
            double g = gap[axis];
            if (!Double.isNaN(avail) && group + g * tl.arLen > 0) {
                // 묶음을 n 번 반복했을 때 gap 을 포함한 크기가 컨테이너를 넘지 않는 가장 큰 n.
                double rest = avail - fixed - g * outside;
                n = (int) Math.max(1, Math.floor((rest + g) / (group + g * tl.arLen)));
            }
            n = Math.min(n, Math.max(1, (MAX_TRACKS - outside) / Math.max(1, tl.arLen)));
            repeatCounts[axis] = n;
            // 묶음의 트랙을 n - 1 번 더 복사해 묶음 뒤에 끼워 넣는다.
            List<Def> extraDefs = new ArrayList<Def>();
            List<List<String>> extraNames = new ArrayList<List<String>>();
            // 반복 횟수 - 1 번 더 복사한다.
            for (int k = 1; k < n; k++) {
                // 묶음의 트랙을 하나씩 복사한다.
                for (int i = 0; i < tl.arLen; i++) {
                    extraDefs.add(tl.defs.get(tl.arStart + i));
                    extraNames.add(tl.names.get(tl.arStart + i));
                }
            }
            tl.defs.addAll(tl.arStart + tl.arLen, extraDefs);
            tl.names.addAll(tl.arStart + tl.arLen, extraNames);
        }

        /**
         * Parses <code>grid-template-areas</code> into named areas.
         * <p>
         * <code>grid-template-areas</code> 를 이름 있는 영역으로 해석한다.
         *
         * @param v the computed value / 계산 값
         */
        void parseAreas(CssValue v) {
            if (v == null || v.isKeyword("none")) {
                return;
            }
            List<String> rows = new ArrayList<String>();
            if (v instanceof CssList) {
                List<CssValue> l = ((CssList) v).getItems();
                // 문자열 하나가 행 하나이다.
                for (int i = 0; i < l.size(); i++) {
                    if (l.get(i) instanceof CssString) {
                        rows.add(((CssString) l.get(i)).getValue());
                    }
                }
            } else if (v instanceof CssString) {
                rows.add(((CssString) v).getValue());
            }
            List<String[]> cells = new ArrayList<String[]>();
            int cols = -1;
            // 행마다 칸 이름을 공백으로 나눈다. 칸이 없는 행이나 칸 수가 다른 행은 선언 전체가 잘못되어 영역이 없다(CSS Grid 7.3).
            for (int i = 0; i < rows.size(); i++) {
                String t = rows.get(i).trim();
                String[] w = t.length() == 0 ? new String[0] : t.split("\\s+");
                if (w.length == 0 || (cols >= 0 && w.length != cols)) {
                    return;
                }
                cols = w.length;
                cells.add(w);
            }
            List<Object[]> found = new ArrayList<Object[]>();
            Set<String> seen = new HashSet<String>();
            // 이름마다 처음 나온 칸에서 시작하는 직사각형 영역을 찾는다. 점만으로 된 칸은 비어 있는 칸이다.
            for (int r = 0; r < cells.size(); r++) {
                String[] w = cells.get(r);
                // 행의 칸을 차례로 보며 이름 있는 영역의 직사각형을 찾는다.
                for (int k = 0; k < w.length; k++) {
                    String name = w[k];
                    if (isNullCell(name) || seen.contains(name)) {
                        continue;
                    }
                    seen.add(name);
                    int r2 = r;
                    int k2 = k;
                    // 같은 이름이 이어지는 오른쪽 끝을 찾는다.
                    while (k2 + 1 < w.length && name.equals(w[k2 + 1])) {
                        k2++;
                    }
                    // 아래 행에서 같은 이름이 이어지는 끝을 찾는다.
                    while (r2 + 1 < cells.size() && name.equals(cells.get(r2 + 1)[k])) {
                        r2++;
                    }
                    // 같은 이름의 칸이 직사각형 밖에 있거나 직사각형 안에 다른 이름이 있으면 직사각형이 아니므로 선언 전체가 잘못이다.
                    for (int rr = 0; rr < cells.size(); rr++) {
                        for (int kk = 0; kk < w.length; kk++) {
                            boolean inside = rr >= r && rr <= r2 && kk >= k && kk <= k2;
                            if (inside != name.equals(cells.get(rr)[kk])) {
                                return;
                            }
                        }
                    }
                    found.add(new Object[] { name, new int[] { r, r2 + 1, k, k2 + 1 } });
                }
            }
            areas.addAll(found);
            explicit[0] = Math.max(explicit[0], cols);
            explicit[1] = Math.max(explicit[1], cells.size());
        }

        /**
         * Tells whether a token of <code>grid-template-areas</code> is a null cell token (a sequence of full stops).
         * <p>
         * <code>grid-template-areas</code> 의 토큰이 빈 칸(점의 나열)인지 알려 준다.
         *
         * @param s the token / 토큰
         * @return true for a null cell / 빈 칸이면 true
         */
        private boolean isNullCell(String s) {
            // 모든 글자가 점이면 빈 칸이다.
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) != '.') {
                    return false;
                }
            }
            return s.length() > 0;
        }

        /**
         * Resolves a line specification of a placement property into a line number.
         * <p>
         * 배치 속성의 선 지정을 선 번호로 푼다.
         *
         * @param v     the computed value of <code>grid-*-start</code> or <code>grid-*-end</code> / 계산 값
         * @param axis  the axis / 축
         * @param end   true for an end line / 끝 선이면 true
         * @return {kind, value}: kind 0 auto, 1 line (0-based line index in value), 2 span (value spans), -1 invalid / {종류, 값}
         */
        int[] lineSpec(CssValue v, int axis, boolean end) {
            if (v == null || v.isKeyword("auto")) {
                return new int[] { 0, 0 };
            }
            List<CssValue> parts = new ArrayList<CssValue>();
            if (v instanceof CssList) {
                parts.addAll(((CssList) v).getItems());
            } else {
                parts.add(v);
            }
            boolean span = false;
            int num = Integer.MIN_VALUE;
            String name = null;
            // 값의 구성요소(span, 정수, 이름)를 차례로 읽는다.
            for (int i = 0; i < parts.size(); i++) {
                CssValue p = parts.get(i);
                if (p.isKeyword("span")) {
                    span = true;
                } else if (p instanceof CssNumber) {
                    num = (int) ((CssNumber) p).getValue();
                } else if (p instanceof CssKeyword) {
                    name = ((CssKeyword) p).getName();
                }
            }
            if (span) {
                return new int[] { 2, num == Integer.MIN_VALUE || num < 1 ? 1 : Math.min(num, MAX_TRACKS) };
            }
            if (name != null) {
                if (num == Integer.MIN_VALUE) {
                    // 식별자만 있으면 먼저 영역 이름(NAME-start, NAME-end)에 맞추고 없으면 그 이름의 첫 선이다(CSS Grid 8.3).
                    int area = findNamedLine(name + (end ? "-end" : "-start"), 1, axis, false);
                    if (area != NOT_FOUND) {
                        return new int[] { 1, area };
                    }
                    return new int[] { 1, findNamedLine(name, 1, axis, true) };
                }
                return new int[] { 1, findNamedLine(name, num == 0 ? 1 : num, axis, true) };
            }
            if (num == Integer.MIN_VALUE || num == 0) {
                return new int[] { 0, 0 };
            }
            return new int[] { 1, num > 0 ? num - 1 : explicit[axis] + 1 + num };
        }

        /**
         * Finds the line index of the n-th line with a name: a named line of the template, or the implicit line
         * <code>name-start</code>/<code>name-end</code> of a named area (CSS Grid 8.3). If the explicit grid has too few lines with the
         * name, the implicit lines are assumed to have the name: after the explicit grid for a positive n, in front of it for a negative n.
         * <p>
         * 이름이 있는 n 번째 선의 번호를 찾는다: 템플릿의 이름 있는 선, 또는 이름 있는 영역의 암시적 선 <code>name-start</code>/<code>name-end</code>(CSS Grid 8.3).
         * 명시적 그리드에 그 이름의 선이 모자라면 암시적 선이 모두 그 이름을 가진 것으로 본다: n 이 양수이면 명시적 그리드 뒤, 음수이면 앞이다.
         *
         * @param name             the name / 이름
         * @param n                the occurrence (negative counts from the end) / 몇 번째(음수는 끝에서부터)
         * @param axis             the axis / 축
         * @param implicitFallback true to use the implicit lines if there are too few named lines, false to report {@link #NOT_FOUND} / 이름 있는 선이 모자랄 때 암시적 선을 쓰면 true, {@link #NOT_FOUND} 를 반환하면 false
         * @return the line index in explicit coordinates (0-based, may be negative or beyond the explicit grid) or {@link #NOT_FOUND} / 명시적 좌표의 선 번호(0부터, 음수이거나 명시적 그리드 밖일 수 있음) 또는 {@link #NOT_FOUND}
         */
        int findNamedLine(String name, int n, int axis, boolean implicitFallback) {
            List<Integer> found = new ArrayList<Integer>();
            List<List<String>> nm = lineNames.get(axis);
            // 템플릿과 영역의 선 이름을 차례로 비교한다.
            for (int i = 0; i < nm.size(); i++) {
                if (nm.get(i).contains(name)) {
                    found.add(Integer.valueOf(i));
                }
            }
            int k = found.size();
            if (n > 0) {
                if (n <= k) {
                    return found.get(n - 1).intValue();
                }
                // 이름이 붙은 선이 모자라면 명시적 그리드 뒤의 암시적 선이 모두 그 이름을 가진 것으로 본다.
                return implicitFallback ? explicit[axis] + (n - k) : NOT_FOUND;
            }
            int m = -n;
            if (m <= k) {
                return found.get(k - m).intValue();
            }
            // 음수는 끝에서 세므로 모자란 만큼 명시적 그리드 앞의 암시적 선이다.
            return implicitFallback ? -(m - k) : NOT_FOUND;
        }

        /**
         * Resolves the placement properties of an item along an axis: the start line and the span, or auto.
         * <p>
         * 항목의 배치 속성을 축 방향으로 푼다: 시작 선과 span, 또는 auto.
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         */
        void resolveAxis(Item it, int axis) {
            ComputedStyle s = it.box.style;
            if (it.anon) {
                it.definite[axis] = false;
                return;
            }
            CssValue sv = s.get(axis == 0 ? Prop.GRID_COLUMN_START : Prop.GRID_ROW_START);
            CssValue ev = s.get(axis == 0 ? Prop.GRID_COLUMN_END : Prop.GRID_ROW_END);
            int[] a = lineSpec(sv, axis, false);
            int[] b = lineSpec(ev, axis, true);
            int start = 0;
            int span = 1;
            boolean definite = false;
            if (a[0] == 1 && b[0] == 1) {
                start = Math.min(a[1], b[1]);
                int endLine = Math.max(a[1], b[1]);
                span = Math.max(1, endLine - start);
                definite = true;
            } else if (a[0] == 1) {
                start = a[1];
                span = b[0] == 2 ? b[1] : 1;
                definite = true;
            } else if (b[0] == 1) {
                span = a[0] == 2 ? a[1] : 1;
                start = b[1] - span;
                definite = true;
            } else {
                span = a[0] == 2 ? a[1] : (b[0] == 2 ? b[1] : 1);
            }
            it.start[axis] = start;
            it.span[axis] = Math.max(1, Math.min(span, MAX_TRACKS));
            it.definite[axis] = definite;
        }

        /**
         * Places the items: the items with a definite position first, then the auto-placement algorithm (CSS Grid 8.5).
         * <p>
         * 항목을 배치한다: 위치가 정해진 항목이 먼저이고 그다음 자동 배치 알고리즘(CSS Grid 8.5)이다.
         */
        void placeItems() {
            String[] flowWords = words(c.style, Prop.GRID_AUTO_FLOW);
            boolean columnFlow = false;
            boolean dense = false;
            // grid-auto-flow 의 단어(row, column, dense)를 읽는다.
            for (int i = 0; i < flowWords.length; i++) {
                if ("column".equals(flowWords[i])) {
                    columnFlow = true;
                } else if ("dense".equals(flowWords[i])) {
                    dense = true;
                }
            }
            // 항목의 두 축 배치 속성을 푼다.
            for (int i = 0; i < items.size(); i++) {
                resolveAxis(items.get(i), 0);
                resolveAxis(items.get(i), 1);
            }
            // 명시적 그리드보다 앞(음수 선)에 놓이는 항목이 있으면 그만큼 암시적 트랙이 앞에 생기므로 모든 선 번호를 그만큼 옮긴다(CSS Grid 8.5).
            for (int axis = 0; axis < 2; axis++) {
                int minStart = 0;
                // 정해진 위치 중 가장 앞의 선을 찾는다.
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).definite[axis]) {
                        minStart = Math.min(minStart, items.get(i).start[axis]);
                    }
                }
                shift[axis] = Math.min(-minStart, MAX_TRACKS);
                // 정해진 위치의 항목을 옮기고 너무 먼 선은 제한한다.
                for (int i = 0; i < items.size(); i++) {
                    Item it = items.get(i);
                    if (it.definite[axis]) {
                        it.start[axis] = Math.max(0, Math.min(it.start[axis] + shift[axis], MAX_TRACKS));
                    }
                }
            }
            int major = columnFlow ? 0 : 1;
            int minor = 1 - major;
            Occupancy occupied = new Occupancy();
            // 1 단계: 두 축 모두 정해진 항목을 놓는다.
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it.definite[0] && it.definite[1]) {
                    occupy(occupied, it);
                }
            }
            java.util.Map<Integer, Integer> lockedCursor = new java.util.HashMap<Integer, Integer>();
            // 2 단계: 주축 위치가 정해진 항목의 부축 위치를 찾는다.
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it.definite[0] && it.definite[1]) {
                    continue;
                }
                if (it.definite[major] && !it.definite[minor]) {
                    int from = dense ? 0 : (lockedCursor.containsKey(Integer.valueOf(it.start[major])) ? lockedCursor.get(Integer.valueOf(it.start[major])).intValue() : 0);
                    int pos = from;
                    // 겹치지 않는 가장 앞의 부축 위치를 찾는다.
                    while (!fits(occupied, it, major, it.start[major], minor, pos)) {
                        pos++;
                    }
                    it.start[minor] = pos;
                    it.definite[minor] = true;
                    lockedCursor.put(Integer.valueOf(it.start[major]), Integer.valueOf(pos + it.span[minor]));
                    occupy(occupied, it);
                }
            }
            // 암시적 그리드의 부축 크기: 명시적 그리드(와 앞쪽 암시적 트랙)와 부축 위치가 정해진 항목의 끝 중 큰 값이다.
            int minorCount = Math.max(1, shift[minor] + explicit[minor]);
            // 부축 위치가 정해진 항목이 놓인 끝 선까지 암시적 그리드가 넓어진다.
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).definite[minor]) {
                    minorCount = Math.max(minorCount, items.get(i).start[minor] + items.get(i).span[minor]);
                }
            }
            int curMajor = 0;
            int curMinor = 0;
            // 3~4 단계: 나머지 항목을 커서를 옮겨 가며 자동으로 놓는다.
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it.definite[0] && it.definite[1]) {
                    continue;
                }
                if (dense) {
                    curMajor = 0;
                    curMinor = 0;
                }
                if (it.definite[minor]) {
                    // 부축 위치가 정해진 항목: 커서보다 앞이면 다음 주축 줄로 가고 겹치지 않는 주축 위치를 찾는다.
                    if (!dense && it.start[minor] < curMinor) {
                        curMajor++;
                    }
                    curMinor = it.start[minor];
                    // 겹치지 않는 주축 위치가 나올 때까지 다음 주축 줄로 간다.
                    while (!fits(occupied, it, major, curMajor, minor, curMinor)) {
                        curMajor++;
                    }
                } else {
                    // 두 축 모두 auto: 부축으로 나아가며 들어가는 자리를 찾고 줄이 차면 다음 주축 줄로 간다.
                    int guard = 0;
                    while (guard++ < 1000000) {
                        boolean inside = curMinor + it.span[minor] <= minorCount || curMinor == 0;
                        if (inside && fits(occupied, it, major, curMajor, minor, curMinor)) {
                            break;
                        }
                        if (curMinor + it.span[minor] > minorCount && curMinor > 0 || !inside) {
                            curMajor++;
                            curMinor = 0;
                        } else {
                            curMinor++;
                        }
                    }
                }
                it.start[major] = curMajor;
                it.start[minor] = curMinor;
                it.definite[0] = true;
                it.definite[1] = true;
                occupy(occupied, it);
                curMinor = it.start[minor] + it.span[minor];
            }
        }

        /**
         * Marks the cells of a placed item as occupied.
         * <p>
         * 놓인 항목의 칸을 점유로 표시한다.
         *
         * @param occ the occupied cells / 점유된 칸
         * @param it  the item / 항목
         */
        void occupy(Occupancy occ, Item it) {
            occ.add(it.start[1], Math.min(MAX_TRACKS, it.start[1] + it.span[1]), it.start[0], Math.min(MAX_TRACKS, it.start[0] + it.span[0]));
        }

        /**
         * Tells whether an item fits at a position without overlapping occupied cells.
         * <p>
         * 항목이 점유된 칸과 겹치지 않고 그 위치에 들어가는지 알려 준다.
         *
         * @param occ   the occupied cells / 점유된 칸
         * @param it    the item / 항목
         * @param major the major axis / 주축
         * @param mp    the position along the major axis / 주축 위치
         * @param minor the minor axis / 부축
         * @param np    the position along the minor axis / 부축 위치
         * @return true if free / 비어 있으면 true
         */
        boolean fits(Occupancy occ, Item it, int major, int mp, int minor, int np) {
            int[] pos = new int[2];
            pos[major] = mp;
            pos[minor] = np;
            return !occ.overlaps(pos[1], Math.min(MAX_TRACKS, pos[1] + it.span[1]), pos[0], Math.min(MAX_TRACKS, pos[0] + it.span[0]));
        }

        /**
         * Sizes the columns and then the rows (CSS Grid 11.1): the heights of the items depend on the widths of their columns, so the
         * columns come first.
         * <p>
         * 열을 먼저, 그다음 행의 크기를 정한다(CSS Grid 11.1): 항목의 높이는 열의 너비에 달려 있으므로 열이 먼저이다.
         */
        void sizeColumnsAndRows() {
            sizeAxis(0, cw, 0);
            sizeAxis(1, contH, 0);
        }

        /**
         * Returns the sum of the used sizes of the tracks of an axis plus the gaps.
         * <p>
         * 축의 트랙 사용 크기의 합과 gap 을 더한 값을 반환한다.
         *
         * @param axis the axis / 축
         * @return the extent / 범위
         */
        double trackExtent(int axis) {
            double sum = 0;
            int n = 0;
            // 접히지 않은 트랙의 크기와 gap 을 더한다.
            for (int i = 0; i < tracks[axis].length; i++) {
                if (!tracks[axis][i].collapsed) {
                    sum += tracks[axis][i].size;
                    n++;
                }
            }
            return sum + gap[axis] * Math.max(0, n - 1);
        }

        /**
         * Resolves the sizing functions of the tracks of an axis and sets the initial base sizes and growth limits (CSS Grid 11.4).
         * Percentages are resolved against the size of the container if it is definite, else they behave like <code>auto</code>;
         * collapsed tracks are fixed at zero.
         * <p>
         * 축의 트랙의 크기 결정 함수를 풀고 기본 크기와 성장 한계의 초깃값을 정한다(CSS Grid 11.4). 백분율은 컨테이너 크기가 정해졌으면 그것에 대해 풀고 아니면
         * <code>auto</code> 처럼 동작하며 접힌 트랙은 0 으로 고정한다.
         *
         * @param axis  the axis / 축
         * @param avail the size of the content box along the axis or NaN / 축 방향 content box 의 크기 또는 NaN
         * @param mode  0 normal, 1 min-content, 2 max-content / 0 보통, 1 min-content, 2 max-content
         */
        void initTracks(int axis, double avail, int mode) {
            Track[] t = tracks[axis];
            boolean definite = !Double.isNaN(avail) && mode == 0;
            // 트랙마다 크기 결정 함수를 복사해 백분율을 풀고 초기 크기를 정한다.
            for (int i = 0; i < t.length; i++) {
                Track tr = t[i];
                Def d = tr.def;
                tr.minKind = d.minKind;
                tr.minVal = d.minVal;
                tr.maxKind = d.maxKind;
                tr.maxVal = d.maxVal;
                if (tr.minKind == T_PCT) {
                    tr.minKind = definite ? T_LEN : T_AUTO;
                    tr.minVal = definite ? avail * d.minVal / 100 : 0;
                }
                if (tr.maxKind == T_PCT) {
                    tr.maxKind = definite ? T_LEN : T_AUTO;
                    tr.maxVal = definite ? avail * d.maxVal / 100 : 0;
                }
                if (tr.maxKind == T_FIT && d.fitPct) {
                    // fit-content(백분율)은 컨테이너 크기가 정해지지 않았으면 max-content 처럼 동작한다.
                    tr.maxKind = definite ? T_FIT : T_AUTO;
                    tr.maxVal = definite ? avail * d.maxVal / 100 : 0;
                }
                if (tr.collapsed) {
                    tr.minKind = T_LEN;
                    tr.minVal = 0;
                    tr.maxKind = T_LEN;
                    tr.maxVal = 0;
                }
                tr.base = tr.minKind == T_LEN ? tr.minVal : 0;
                tr.limit = tr.maxKind == T_LEN ? tr.maxVal : INF;
                if (tr.limit < tr.base) {
                    tr.limit = tr.base;
                }
                tr.infGrowable = false;
                tr.size = 0;
            }
        }

        /**
         * Computes the contributions of the items to the tracks of an axis: the min-content, max-content and minimum contributions
         * (CSS Grid 11.5, 6.6).
         * <p>
         * 항목이 축의 트랙에 하는 기여를 계산한다: min-content, max-content, 최소 기여(CSS Grid 11.5, 6.6).
         *
         * @param axis the axis / 축
         */
        void contributions(int axis) {
            // 항목마다 축 방향 최소/최대 content 기여(바깥 크기)를 구한다.
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                LayoutBox b = it.box;
                double extra = outerExtra(it, axis);
                if (axis == 0) {
                    // 백분율 너비는 영역을 알 수 없는 동안 auto 로 본다.
                    double spec = bl.specWidth(b, Double.NaN);
                    double lo = bl.minWidth(b, Double.NaN);
                    double hi = bl.maxWidth(b, Double.NaN);
                    double mn = Double.isNaN(spec) ? bl.contentMin(b) : spec;
                    double mx = Double.isNaN(spec) ? bl.contentMax(b) : spec;
                    it.minC[0] = Math.max(lo, Math.min(mn, hi)) + extra;
                    it.maxC[0] = Math.max(lo, Math.min(mx, hi)) + extra;
                    if (!Double.isNaN(spec)) {
                        it.minimumC[0] = it.minC[0];
                    } else {
                        double used = usedMinimum(it, 0, lo, hi, bl.contentMin(b));
                        it.minimumC[0] = Math.max(lo, Math.min(used, hi)) + extra;
                    }
                } else {
                    double h = measureHeight(it, itemWidth(it));
                    it.minC[1] = h;
                    it.maxC[1] = h;
                    double spec = bl.specHeight(b, Double.NaN);
                    if (!Double.isNaN(spec)) {
                        it.minimumC[1] = h;
                    } else {
                        double lo = bl.minHeight(b, Double.NaN);
                        double hi = bl.maxHeight(b, Double.NaN);
                        double used = usedMinimum(it, 1, lo, hi, Math.max(0, h - extra));
                        it.minimumC[1] = Math.max(lo, Math.min(used, hi)) + extra;
                    }
                }
            }
        }

        /**
         * Returns the outer extra size of an item along an axis: padding, border and margins (auto margins count as zero).
         * <p>
         * 항목의 축 방향 바깥 추가 크기를 반환한다: padding, border, margin(auto margin 은 0).
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return the extra size / 추가 크기
         */
        double outerExtra(Item it, int axis) {
            if (axis == 0) {
                return BlockLayouter.hExtras(it.box) + it.margin[0] + it.margin[1];
            }
            return BlockLayouter.vExtras(it.box) + it.margin[2] + it.margin[3];
        }

        /**
         * Returns the used minimum size (content box) of an item that has no definite preferred size along an axis: the specified
         * <code>min-width</code>/<code>min-height</code>, or the automatic minimum size of CSS Grid 6.6 (the smaller of the content size
         * suggestion and the maximum size, limited by fixed track maximums) if the minimum size is <code>auto</code>, or zero for items
         * that are scroll containers, items that span no <code>auto</code> minimum track, or items that span flexible tracks together
         * with others.
         * <p>
         * 축 방향 선호 크기가 정해지지 않은 항목의 사용 최소 크기(content box)를 반환한다: 지정한 <code>min-width</code>/<code>min-height</code>,
         * 최소 크기가 <code>auto</code> 이면 CSS Grid 6.6 의 자동 최소 크기(내용 크기 제안과 최대 크기 중 작은 값. 고정 트랙 최대로 제한), 스크롤 컨테이너이거나
         * <code>auto</code> 최소 트랙에 걸치지 않거나 유연한 트랙과 다른 트랙에 함께 걸치는 항목은 0.
         *
         * @param it         the item / 항목
         * @param axis       the axis / 축
         * @param lo         the specified minimum size / 지정한 최소 크기
         * @param hi         the maximum size / 최대 크기
         * @param suggestion the content size suggestion (min-content size) / 내용 크기 제안(min-content 크기)
         * @return the used minimum size / 사용 최소 크기
         */
        double usedMinimum(Item it, int axis, double lo, double hi, double suggestion) {
            LayoutBox b = it.box;
            int frame = BlockLayouter.frameOf(b);
            boolean minAuto = it.anon || b.style.isAuto(axis == 0 ? Css.inlineSizeProp(frame, 1) : Css.blockSizeProp(frame, 1));
            if (!minAuto) {
                return lo;
            }
            if (!autoMinimumApplies(it, axis)) {
                return 0;
            }
            double v = Math.min(suggestion, hi);
            double cap = fixedMaxSpan(it, axis);
            // 걸친 트랙의 최대 크기가 모두 고정이면 영역의 최대 크기(stretch fit)로 제한한다.
            if (!Double.isNaN(cap)) {
                v = Math.min(v, Math.max(0, cap - outerExtra(it, axis)));
            }
            return v;
        }

        /**
         * Tells whether the automatic minimum size of an item is its content-based minimum size (CSS Grid 6.6): the item is not a scroll
         * container, it spans a track whose minimum sizing function is <code>auto</code>, and none of the tracks it spans are flexible
         * if it spans more than one.
         * <p>
         * 항목의 자동 최소 크기가 내용 기반 최소 크기인지 알려 준다(CSS Grid 6.6): 스크롤 컨테이너가 아니고, 최소 크기 함수가 <code>auto</code> 인 트랙에 걸치고,
         * 여러 트랙에 걸치면 그중 유연한 트랙이 없어야 한다.
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return true if the content-based minimum size applies / 내용 기반 최소 크기를 쓰면 true
         */
        boolean autoMinimumApplies(Item it, int axis) {
            if (!it.anon && Css.clips(it.box.style)) {
                return false;
            }
            Track[] t = tracks[axis];
            int to = Math.min(t.length, it.start[axis] + it.span[axis]);
            boolean anyAuto = false;
            boolean anyFlex = false;
            // 걸친 트랙 중 최소 크기 함수가 auto 인 것과 유연한 것이 있는지 본다.
            for (int i = it.start[axis]; i < to; i++) {
                if (t[i].minKind == T_AUTO) {
                    anyAuto = true;
                }
                if (t[i].maxKind == T_FR) {
                    anyFlex = true;
                }
            }
            return anyAuto && (it.span[axis] == 1 || !anyFlex);
        }

        /**
         * Returns the sum of the fixed maximum sizes of the tracks an item spans plus the gaps between them, or NaN if one of the tracks
         * has a maximum sizing function that is not fixed (a length or the argument of <code>fit-content()</code>).
         * <p>
         * 항목이 걸친 트랙의 고정 최대 크기의 합과 그 사이 gap 을 반환한다. 최대 크기 함수가 고정(길이 또는 <code>fit-content()</code> 의 인수)이 아닌 트랙이 있으면
         * NaN.
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return the sum or NaN / 합 또는 NaN
         */
        double fixedMaxSpan(Item it, int axis) {
            Track[] t = tracks[axis];
            int from = it.start[axis];
            int to = Math.min(t.length, from + it.span[axis]);
            if (from >= to) {
                return Double.NaN;
            }
            double sum = 0;
            int active = 0;
            // 걸친 트랙의 최대 크기가 모두 고정인지 확인하며 더한다.
            for (int i = from; i < to; i++) {
                Track tr = t[i];
                if (tr.maxKind == T_LEN || (tr.maxKind == T_FIT && !Double.isInfinite(tr.maxVal))) {
                    sum += tr.maxVal;
                } else {
                    return Double.NaN;
                }
                if (!tr.collapsed) {
                    active++;
                }
            }
            return sum + gap[axis] * Math.max(0, active - 1);
        }

        /**
         * Returns the used size of the grid area of an item along an axis (the sizes of the tracks it spans and the gaps in between).
         * <p>
         * 항목 그리드 영역의 축 방향 사용 크기를 반환한다(걸친 트랙의 크기와 그 사이 gap).
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return the size / 크기
         */
        double areaSize(Item it, int axis) {
            double s = 0;
            int n = 0;
            int to = Math.min(tracks[axis].length, it.start[axis] + it.span[axis]);
            // 걸친 트랙의 크기를 더하고 접히지 않은 트랙 사이의 gap 을 더한다.
            for (int t = it.start[axis]; t < to; t++) {
                if (!tracks[axis][t].collapsed) {
                    s += tracks[axis][t].size;
                    n++;
                }
            }
            return s + gap[axis] * Math.max(0, n - 1);
        }

        /**
         * Returns the content width at which an item is laid out in its grid area: the specified width, the natural width of a replaced
         * element, the area width for <code>stretch</code>/<code>normal</code> alignment, else the fit-content width.
         * <p>
         * 항목을 그리드 영역 안에서 배치할 content 너비를 반환한다: 지정 너비, 치환 요소의 자연 너비, <code>stretch</code>/<code>normal</code> 정렬이면 영역 너비, 아니면
         * fit-content 너비.
         *
         * @param it the item / 항목
         * @return the content width / content 너비
         */
        double itemWidth(Item it) {
            return itemWidth(it, areaSize(it, 0));
        }

        /**
         * Lays out an item at a content width and returns its outer height (margins included).
         * <p>
         * 항목을 content 너비로 배치하고 바깥 높이(margin 포함)를 반환한다.
         *
         * @param it the item / 항목
         * @param w  the content width / content 너비
         * @return the outer height / 바깥 높이
         */
        double measureHeight(Item it, double w) {
            place(it, w, Double.NaN, Double.NaN);
            return it.box.height + it.margin[2] + it.margin[3];
        }

        /**
         * Lays out an item at a content width and an optional forced content height unless it is laid out like that already.
         * <p>
         * 항목을 content 너비와 선택적 강제 content 높이로 배치한다. 이미 그렇게 배치되어 있으면 하지 않는다.
         *
         * @param it      the item / 항목
         * @param w       the content width / content 너비
         * @param forcedH the forced content height or NaN / 강제 content 높이 또는 NaN
         * @param cbH     the height of the containing block (the grid area) for percentages or NaN / 백분율의 기준인 포함 블록(그리드 영역) 높이 또는 NaN
         */
        void place(Item it, double w, double forcedH, double cbH) {
            if (it.laid && same(it.laidW, w) && same(it.laidH, forcedH) && same(it.laidCb, cbH)) {
                return;
            }
            LayoutBox b = it.box;
            if (b.type == LayoutBox.Type.REPLACED) {
                b.forcedH = Double.NaN;
                double[] rsz = bl.replacedSize(b, Double.isNaN(cw) ? 0 : cw, cbH);
                double h = !Double.isNaN(forcedH) ? forcedH : rsz[1];
                bl.finish(b, w, cbH, new double[] { w, h });
            } else if (b.type == LayoutBox.Type.TABLE) {
                bl.tables.layout(b, w + BlockLayouter.hExtras(b) + b.marginLeft + b.marginRight, true);
            } else {
                b.forcedH = forcedH;
                bl.finish(b, w, cbH, null);
                b.forcedH = Double.NaN;
            }
            it.laid = true;
            it.laidW = w;
            it.laidH = forcedH;
            it.laidCb = cbH;
        }

        /**
         * Tells whether two sizes are the same (both NaN or closer than the tolerance).
         * <p>
         * 두 크기가 같은지 알려 준다(둘 다 NaN 이거나 허용 오차보다 가까움).
         *
         * @param a the first size / 첫 크기
         * @param b the second size / 둘째 크기
         * @return true if the same / 같으면 true
         */
        private boolean same(double a, double b) {
            return (Double.isNaN(a) && Double.isNaN(b)) || Math.abs(a - b) < EPS;
        }

        /**
         * Runs the grid track sizing algorithm for an axis (CSS Grid 11.3): initialize the tracks, resolve the intrinsic sizes, maximize
         * the tracks, expand the flexible tracks and stretch the <code>auto</code> tracks.
         * <p>
         * 축의 그리드 트랙 크기 결정 알고리즘을 실행한다(CSS Grid 11.3): 트랙 초기화, 고유 크기 풀이, 트랙 최대화, 유연한 트랙 확장, <code>auto</code> 트랙 늘리기.
         *
         * @param axis  the axis / 축
         * @param avail the size of the content box along the axis or NaN / 축 방향 content box 의 크기 또는 NaN
         * @param mode  0 normal, 1 min-content (intrinsic width), 2 max-content (intrinsic width) / 0 보통, 1 min-content(고유 너비), 2 max-content(고유 너비)
         */
        void sizeAxis(int axis, double avail, int mode) {
            Track[] t = tracks[axis];
            double g = gap[axis];
            // 1. 초기화
            initTracks(axis, avail, mode);
            // 2. 고유 크기 풀이
            contributions(axis);
            resolveIntrinsic(axis, mode);
            // 3. 최대화: 크기가 정해졌으면 남는 공간을 한계까지 나누고, 정해지지 않았거나 max-content 제약이면 한계까지 키우며 min-content 제약이면 키우지 않는다.
            if (mode == 0 && !Double.isNaN(avail)) {
                double used = g * Math.max(0, activeTracks(t) - 1);
                // 트랙의 기본 크기를 더해 쓰인 공간을 구한다.
                for (int i = 0; i < t.length; i++) {
                    used += t[i].base;
                }
                growToLimits(t, avail - used);
            } else if (mode != 1) {
                // 트랙마다 기본 크기를 성장 한계까지 키운다.
                for (int i = 0; i < t.length; i++) {
                    if (!Double.isInfinite(t[i].limit)) {
                        t[i].base = Math.max(t[i].base, t[i].limit);
                    }
                }
            }
            // 4. 유연한 트랙 확장
            expandFlexible(axis, t, avail, g, mode);
            // 5. auto 트랙 늘리기
            for (int i = 0; i < t.length; i++) {
                t[i].size = t[i].collapsed ? 0 : t[i].base;
            }
            double stretchAvail = avail;
            if (mode == 0 && Double.isNaN(stretchAvail) && axis == 1 && minH > 0) {
                // 컨테이너 높이가 정해지지 않았어도 정해진 최소 높이가 있으면 그 크기로 남는 공간을 구한다(CSS Grid 11.8).
                stretchAvail = minH;
            }
            if (mode == 0 && !Double.isNaN(stretchAvail)) {
                String[] cd = words(c.style, axis == 0 ? Prop.JUSTIFY_CONTENT : Prop.ALIGN_CONTENT);
                String k = cd[cd.length - 1];
                if ("normal".equals(k) || "stretch".equals(k) || k.length() == 0) {
                    double used = g * Math.max(0, activeTracks(t) - 1);
                    int autos = 0;
                    // 쓰인 공간을 더하고 최대 크기 함수가 auto 인 트랙의 수를 센다.
                    for (int i = 0; i < t.length; i++) {
                        used += t[i].size;
                        if (t[i].maxKind == T_AUTO && !t[i].collapsed) {
                            autos++;
                        }
                    }
                    double free = stretchAvail - used;
                    // 남는 공간을 최대 크기 함수가 auto 인 트랙에 똑같이 나눈다.
                    if (free > EPS && autos > 0) {
                        for (int i = 0; i < t.length; i++) {
                            if (t[i].maxKind == T_AUTO && !t[i].collapsed) {
                                t[i].size += free / autos;
                            }
                        }
                    }
                }
            }
        }

        /**
         * Counts the tracks that are not collapsed.
         * <p>
         * 접히지 않은 트랙의 수를 센다.
         *
         * @param t the tracks / 트랙
         * @return the count / 수
         */
        int activeTracks(Track[] t) {
            int n = 0;
            // 접히지 않은 트랙을 센다.
            for (int i = 0; i < t.length; i++) {
                if (!t[i].collapsed) {
                    n++;
                }
            }
            return n;
        }

        /**
         * Tells whether an item spans a flexible track along an axis.
         * <p>
         * 항목이 축 방향으로 유연한 트랙에 걸치는지 알려 준다.
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return true if it does / 걸치면 true
         */
        boolean spansFlexible(Item it, int axis) {
            Track[] t = tracks[axis];
            int to = Math.min(t.length, it.start[axis] + it.span[axis]);
            // 걸친 트랙 중 유연한 것이 있는지 본다.
            for (int i = it.start[axis]; i < to; i++) {
                if (t[i].maxKind == T_FR && !t[i].collapsed) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Resolves the intrinsic track sizes (CSS Grid 11.5): the items that do not span a flexible track are considered in the order of
         * increasing span, one group per span, then all the items that span a flexible track together.
         * <p>
         * 고유 트랙 크기를 푼다(CSS Grid 11.5): 유연한 트랙에 걸치지 않는 항목은 걸친 트랙 수가 늘어나는 순서로 span 마다 한 묶음씩 처리하고, 유연한 트랙에 걸치는 항목은
         * 모두 함께 처리한다.
         *
         * @param axis the axis / 축
         * @param mode 0 normal, 1 min-content constraint, 2 max-content constraint / 0 보통, 1 min-content 제약, 2 max-content 제약
         */
        void resolveIntrinsic(int axis, int mode) {
            Track[] t = tracks[axis];
            int maxSpan = 1;
            // 가장 많은 트랙에 걸친 항목의 span 을 찾는다.
            for (int i = 0; i < items.size(); i++) {
                maxSpan = Math.max(maxSpan, items.get(i).span[axis]);
            }
            // 걸친 트랙 수가 적은 항목 묶음부터 차례로 고유 크기에 반영한다.
            for (int span = 1; span <= maxSpan; span++) {
                List<Item> group = new ArrayList<Item>();
                // 이 span 이고 유연한 트랙에 걸치지 않는 항목을 모은다.
                for (int i = 0; i < items.size(); i++) {
                    Item it = items.get(i);
                    if (it.span[axis] == span && !spansFlexible(it, axis)) {
                        group.add(it);
                    }
                }
                if (group.isEmpty()) {
                    continue;
                }
                round(group, axis, false, A_INTRINSIC_MIN, K_MINIMUM, mode);
                round(group, axis, false, A_CONTENT_MIN, K_MIN, mode);
                if (mode == 2) {
                    round(group, axis, false, A_AUTO_MAXC_MIN, K_LMAX, mode);
                }
                round(group, axis, false, A_MAXC_MIN, K_MAX, mode);
                raiseLimits(t);
                round(group, axis, true, A_INTRINSIC_MAX, K_MIN, mode);
                round(group, axis, true, A_MAXC_MAX, K_MAX, mode);
                // "infinitely growable" 표시는 다음 단계까지만 유효하다.
                for (int i = 0; i < t.length; i++) {
                    t[i].infGrowable = false;
                }
            }
            List<Item> flex = new ArrayList<Item>();
            // 유연한 트랙에 걸치는 항목을 모은다.
            for (int i = 0; i < items.size(); i++) {
                if (spansFlexible(items.get(i), axis)) {
                    flex.add(items.get(i));
                }
            }
            if (!flex.isEmpty()) {
                flexRound(flex, axis, K_MINIMUM, A_INTRINSIC_MIN, mode);
                if (mode == 2) {
                    flexRound(flex, axis, K_LMAX, A_AUTO_MAXC_MIN, mode);
                }
                raiseLimits(t);
            }
            // 성장 한계가 무한대로 남은 트랙(항목이 없거나 유연한 트랙)은 기본 크기를 한계로 한다.
            for (int i = 0; i < t.length; i++) {
                if (Double.isInfinite(t[i].limit)) {
                    t[i].limit = t[i].base;
                }
            }
        }

        /**
         * Raises the growth limits that are smaller than the base size to the base size.
         * <p>
         * 기본 크기보다 작은 성장 한계를 기본 크기로 올린다.
         *
         * @param t the tracks / 트랙
         */
        void raiseLimits(Track[] t) {
            // 기본 크기가 성장 한계보다 크면 한계를 맞춘다.
            for (int i = 0; i < t.length; i++) {
                if (t[i].limit < t[i].base) {
                    t[i].limit = t[i].base;
                }
            }
        }

        /**
         * Tells whether a track takes part in a distribution round: its minimum or maximum sizing function is one of the kinds of the set.
         * <p>
         * 트랙이 분배 단계에 참여하는지 알려 준다: 최소 또는 최대 크기 함수가 그 집합의 종류에 속한다.
         *
         * @param tr       the track / 트랙
         * @param affected one of the <code>A_*</code> constants / <code>A_*</code> 상수 중 하나
         * @return true if the track is affected / 영향을 받으면 true
         */
        private boolean matches(Track tr, int affected) {
            int mk = tr.minKind;
            int xk = tr.maxKind;
            switch (affected) {
                case A_INTRINSIC_MIN:
                    return mk == T_AUTO || mk == T_MINC || mk == T_MAXC;
                case A_CONTENT_MIN:
                    return mk == T_MINC || mk == T_MAXC;
                case A_AUTO_MAXC_MIN:
                    return mk == T_AUTO || mk == T_MAXC;
                case A_MAXC_MIN:
                    return mk == T_MAXC;
                case A_INTRINSIC_MAX:
                    return xk == T_AUTO || xk == T_MINC || xk == T_MAXC || xk == T_FIT;
                case A_MAXC_MAX:
                    return xk == T_AUTO || xk == T_MAXC || xk == T_FIT;
                default:
                    return false;
            }
        }

        /**
         * Returns the contribution of an item that a distribution round accommodates.
         * <p>
         * 분배 단계가 수용하는 항목의 기여를 반환한다.
         *
         * @param it      the item / 항목
         * @param axis    the axis / 축
         * @param contrib one of the <code>K_*</code> constants / <code>K_*</code> 상수 중 하나
         * @param mode    0 normal, 1 min-content constraint, 2 max-content constraint / 0 보통, 1 min-content 제약, 2 max-content 제약
         * @return the outer size contribution / 바깥 크기 기여
         */
        double contribution(Item it, int axis, int contrib, int mode) {
            switch (contrib) {
                case K_MINIMUM:
                    // min-content/max-content 제약 아래에서는 최소 기여 대신 제한된 min-content 기여를 쓴다.
                    return mode == 0 ? it.minimumC[axis] : limited(it, axis, it.minC[axis]);
                case K_MIN:
                    return it.minC[axis];
                case K_MAX:
                    return it.maxC[axis];
                default:
                    return limited(it, axis, it.maxC[axis]);
            }
        }

        /**
         * Returns the limited contribution of an item: the contribution limited by the fixed maximum sizing functions of the tracks the
         * item spans (if they are all fixed) and floored by the minimum contribution (CSS Grid 11.5).
         * <p>
         * 항목의 제한된 기여를 반환한다: 항목이 걸친 트랙의 고정 최대 크기 함수(모두 고정일 때)로 제한하고 최소 기여를 바닥으로 한 기여(CSS Grid 11.5).
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @param c    the contribution / 기여
         * @return the limited contribution / 제한된 기여
         */
        double limited(Item it, int axis, double c) {
            double cap = fixedMaxSpan(it, axis);
            if (Double.isNaN(cap)) {
                return c;
            }
            return Math.max(it.minimumC[axis], Math.min(c, cap));
        }

        /**
         * Returns how much a track can still grow in a distribution round before it reaches its limit (CSS Grid 11.5.1): the growth limit
         * (capped by the argument of <code>fit-content()</code>) for the base sizes; for the growth limits zero unless the limit is
         * infinite or infinitely growable.
         * <p>
         * 분배 단계에서 트랙이 한계에 닿기 전까지 얼마나 더 커질 수 있는지 반환한다(CSS Grid 11.5.1): 기본 크기는 성장 한계(<code>fit-content()</code> 의 인수로 제한),
         * 성장 한계는 한계가 무한대이거나 infinitely growable 이 아니면 0.
         *
         * @param tr     the track / 트랙
         * @param limits true if the growth limits are affected, false for the base sizes / 성장 한계에 영향을 주면 true, 기본 크기이면 false
         * @return the room / 남은 여유
         */
        private double roomOf(Track tr, boolean limits) {
            if (!limits) {
                double lim = tr.limit;
                if (tr.maxKind == T_FIT) {
                    lim = Math.min(lim, tr.maxVal);
                }
                return Math.max(0, lim - tr.base);
            }
            if (!Double.isInfinite(tr.limit) && !tr.infGrowable) {
                return 0;
            }
            double cur = Double.isInfinite(tr.limit) ? tr.base : tr.limit;
            if (tr.maxKind == T_FIT) {
                return Math.max(0, tr.maxVal - cur);
            }
            return INF;
        }

        /**
         * Distributes space equally among the tracks of a set, freezing a track when its increase reaches its room.
         * <p>
         * 집합에 속한 트랙에 공간을 똑같이 나누되 증가량이 여유에 닿은 트랙은 고정한다.
         *
         * @param space the space / 공간
         * @param set   marks the tracks of the set / 집합의 트랙 표시
         * @param room  the room of every track / 트랙마다의 여유
         * @param inc   the increases (updated) / 증가량(갱신됨)
         * @return the space that is left / 남은 공간
         */
        private double fill(double space, boolean[] set, double[] room, double[] inc) {
            double left = space;
            // 여유가 남은 트랙에 똑같이 나누고 여유가 다한 트랙을 빼는 일을 반복한다.
            for (int guard = 0; guard <= set.length && left > EPS; guard++) {
                int open = 0;
                // 여유가 남은 트랙의 수를 센다.
                for (int i = 0; i < set.length; i++) {
                    if (set[i] && room[i] - inc[i] > EPS) {
                        open++;
                    }
                }
                if (open == 0) {
                    break;
                }
                double share = left / open;
                // 열린 트랙에 몫을 더한다.
                for (int i = 0; i < set.length; i++) {
                    if (set[i] && room[i] - inc[i] > EPS) {
                        double add = Math.min(share, room[i] - inc[i]);
                        inc[i] += add;
                        left -= add;
                    }
                }
            }
            return Math.max(0, left);
        }

        /**
         * Tells whether a track receives the space that is distributed beyond the limits (CSS Grid 11.5.1): the tracks with an intrinsic
         * maximum sizing function (with a max-content maximum when accommodating max-content contributions into the base sizes); a
         * <code>fit-content()</code> track counts until it has reached its argument.
         * <p>
         * 트랙이 한계를 넘어 분배되는 공간을 받는지 알려 준다(CSS Grid 11.5.1): 최대 크기 함수가 고유 크기인 트랙(기본 크기에 max-content 기여를 수용할 때는 max-content 최대).
         * <code>fit-content()</code> 트랙은 인수에 닿기 전까지만 해당한다.
         *
         * @param tr      the track / 트랙
         * @param limits  true if the growth limits are affected / 성장 한계에 영향을 주면 true
         * @param contrib one of the <code>K_*</code> constants / <code>K_*</code> 상수 중 하나
         * @param current the size that is compared with the argument of fit-content() / fit-content() 의 인수와 비교할 현재 크기
         * @return true if the track receives space beyond its limit / 한계를 넘는 공간을 받으면 true
         */
        private boolean beyondLimits(Track tr, boolean limits, int contrib, double current) {
            int xk = tr.maxKind;
            if (xk == T_FIT) {
                return current < tr.maxVal - EPS;
            }
            if (!limits && (contrib == K_MAX || contrib == K_LMAX)) {
                return xk == T_MAXC || xk == T_AUTO;
            }
            return xk == T_AUTO || xk == T_MINC || xk == T_MAXC;
        }

        /**
         * Distributes the extra space that the items of a group need over the tracks they span (CSS Grid 11.5.1): the space is divided
         * equally among the affected tracks up to their limits, then among the other spanned tracks up to their limits, then beyond the
         * limits; a track gets the largest increase that any item of the group asks for.
         * <p>
         * 묶음의 항목이 필요로 하는 추가 공간을 걸친 트랙에 나눈다(CSS Grid 11.5.1): 영향받는 트랙에 한계까지 똑같이 나누고, 남으면 걸친 다른 트랙에 한계까지, 그래도
         * 남으면 한계를 넘어서 나눈다. 트랙은 묶음의 항목이 요구하는 증가량 중 가장 큰 값을 받는다.
         *
         * @param group    the items / 항목
         * @param axis     the axis / 축
         * @param limits   true to grow the growth limits, false for the base sizes / 성장 한계를 키우면 true, 기본 크기를 키우면 false
         * @param affected one of the <code>A_*</code> constants / <code>A_*</code> 상수 중 하나
         * @param contrib  one of the <code>K_*</code> constants / <code>K_*</code> 상수 중 하나
         * @param mode     0 normal, 1 min-content constraint, 2 max-content constraint / 0 보통, 1 min-content 제약, 2 max-content 제약
         */
        void round(List<Item> group, int axis, boolean limits, int affected, int contrib, int mode) {
            Track[] t = tracks[axis];
            double g = gap[axis];
            double[] planned = new double[t.length];
            // 항목마다 필요한 공간을 구해 걸친 트랙에 나눈다.
            for (int gi = 0; gi < group.size(); gi++) {
                Item it = group.get(gi);
                int from = it.start[axis];
                int to = Math.min(t.length, from + it.span[axis]);
                if (from >= to) {
                    continue;
                }
                int n = to - from;
                boolean[] aff = new boolean[n];
                boolean[] oth = new boolean[n];
                double[] room = new double[n];
                int affCount = 0;
                int othCount = 0;
                int active = 0;
                double sizes = 0;
                // 걸친 트랙의 현재 크기를 더하고 영향받는 트랙과 아닌 트랙을 나눈다.
                for (int i = from; i < to; i++) {
                    Track tr = t[i];
                    sizes += limits ? (Double.isInfinite(tr.limit) ? tr.base : tr.limit) : tr.base;
                    room[i - from] = roomOf(tr, limits);
                    if (tr.collapsed) {
                        continue;
                    }
                    active++;
                    if (matches(tr, affected)) {
                        aff[i - from] = true;
                        affCount++;
                    } else {
                        oth[i - from] = true;
                        othCount++;
                    }
                }
                if (affCount == 0) {
                    continue;
                }
                sizes += g * Math.max(0, active - 1);
                double space = contribution(it, axis, contrib, mode) - sizes;
                if (space <= EPS) {
                    continue;
                }
                double[] inc = new double[n];
                // 1. 한계까지 영향받는 트랙에 똑같이 나눈다.
                double left = fill(space, aff, room, inc);
                // 2. 남으면 영향받지 않는 트랙에 한계까지 나눈다.
                if (left > EPS && othCount > 0) {
                    left = fill(left, oth, room, inc);
                }
                // 3. 그래도 남으면 한계를 넘어서 나눈다: 우선 대상이 있으면 그 트랙, 없으면 영향받는 트랙 전부.
                if (left > EPS) {
                    boolean[] beyond = new boolean[n];
                    int bc = 0;
                    // 한계를 넘어 공간을 받을 트랙을 고른다.
                    for (int i = 0; i < n; i++) {
                        Track tr = t[from + i];
                        double cur = (limits ? (Double.isInfinite(tr.limit) ? tr.base : tr.limit) : tr.base) + inc[i];
                        if (aff[i] && beyondLimits(tr, limits, contrib, cur)) {
                            beyond[i] = true;
                            bc++;
                        }
                    }
                    if (bc == 0 && !limits) {
                        // 기본 크기를 키울 때는 대상이 없으면 영향받는 트랙 전부가 받는다. 성장 한계는 그런 대체 없이 받을 트랙이 없으면 공간을 버린다.
                        beyond = aff;
                        bc = affCount;
                    }
                    // 고른 트랙에 남은 공간을 똑같이 나눈다.
                    for (int i = 0; i < n && bc > 0; i++) {
                        if (beyond[i]) {
                            inc[i] += left / bc;
                        }
                    }
                }
                // 트랙마다 항목들이 요구한 증가량 중 가장 큰 값을 계획 증가량으로 한다.
                for (int i = 0; i < n; i++) {
                    planned[from + i] = Math.max(planned[from + i], inc[i]);
                }
            }
            // 계획한 증가량을 기본 크기나 성장 한계에 더한다.
            for (int i = 0; i < t.length; i++) {
                if (planned[i] <= 0) {
                    continue;
                }
                Track tr = t[i];
                if (limits) {
                    boolean wasInf = Double.isInfinite(tr.limit);
                    tr.limit = (wasInf ? tr.base : tr.limit) + planned[i];
                    if (wasInf && affected == A_INTRINSIC_MAX) {
                        // 무한대에서 유한해진 한계는 다음 단계에서 다시 커질 수 있다.
                        tr.infGrowable = true;
                    }
                } else {
                    tr.base += planned[i];
                }
            }
        }

        /**
         * Distributes the extra space that the items spanning flexible tracks need, only to the flexible tracks and in proportion to their
         * flex factors (CSS Grid 11.5): if the factors add up to less than one, only that proportion is divided by the ratios and the
         * rest equally.
         * <p>
         * 유연한 트랙에 걸친 항목이 필요로 하는 추가 공간을 유연한 트랙에만 계수 비율로 나눈다(CSS Grid 11.5): 계수의 합이 1 보다 작으면 그 비율만큼만 계수 비율로 나누고 나머지는
         * 똑같이 나눈다.
         *
         * @param flex     the items / 항목
         * @param axis     the axis / 축
         * @param contrib  one of the <code>K_*</code> constants / <code>K_*</code> 상수 중 하나
         * @param affected one of the <code>A_*</code> constants (the minimum sizing function that the flexible tracks need) / <code>A_*</code> 상수 중 하나(유연한 트랙이 가져야 할 최소 크기 함수)
         * @param mode     0 normal, 1 min-content constraint, 2 max-content constraint / 0 보통, 1 min-content 제약, 2 max-content 제약
         */
        void flexRound(List<Item> flex, int axis, int contrib, int affected, int mode) {
            Track[] t = tracks[axis];
            double g = gap[axis];
            double[] planned = new double[t.length];
            // 항목마다 필요한 공간을 구해 걸친 유연한 트랙에 나눈다.
            for (int gi = 0; gi < flex.size(); gi++) {
                Item it = flex.get(gi);
                int from = it.start[axis];
                int to = Math.min(t.length, from + it.span[axis]);
                double sizes = 0;
                double sumF = 0;
                int count = 0;
                int active = 0;
                // 걸친 트랙의 기본 크기를 더하고 영향받는 유연한 트랙의 계수를 모은다.
                for (int i = from; i < to; i++) {
                    Track tr = t[i];
                    sizes += tr.base;
                    if (tr.collapsed) {
                        continue;
                    }
                    active++;
                    if (tr.maxKind == T_FR && matches(tr, affected)) {
                        sumF += tr.maxVal;
                        count++;
                    }
                }
                if (count == 0) {
                    continue;
                }
                sizes += g * Math.max(0, active - 1);
                double space = contribution(it, axis, contrib, mode) - sizes;
                if (space <= EPS) {
                    continue;
                }
                // 계수 비율로 나누고 계수 합이 1 보다 작으면 모자란 비율은 똑같이 나눈다.
                for (int i = from; i < to; i++) {
                    Track tr = t[i];
                    if (tr.collapsed || tr.maxKind != T_FR || !matches(tr, affected)) {
                        continue;
                    }
                    double share = sumF >= 1 ? space * tr.maxVal / sumF : space * tr.maxVal + space * (1 - sumF) / count;
                    planned[i] = Math.max(planned[i], share);
                }
            }
            // 계획한 증가량을 기본 크기에 더한다.
            for (int i = 0; i < t.length; i++) {
                t[i].base += planned[i];
            }
        }

        /**
         * Distributes free space equally to the tracks that are below their growth limit (CSS Grid 11.6, maximize tracks).
         * <p>
         * 성장 한계에 못 미친 트랙에 남는 공간을 똑같이 나눈다(CSS Grid 11.6, 트랙 최대화).
         *
         * @param t    the tracks / 트랙
         * @param free the free space / 남는 공간
         */
        void growToLimits(Track[] t, double free) {
            // 한계에 닿지 않은 트랙에 똑같이 나누고 닿은 트랙은 빼는 일을 반복한다.
            for (int guard = 0; guard < t.length + 2 && free > EPS; guard++) {
                List<Track> open = new ArrayList<Track>();
                // 성장 한계에 못 미친 열린 트랙을 모은다.
                for (int i = 0; i < t.length; i++) {
                    if (!t[i].collapsed && t[i].base < t[i].limit - EPS) {
                        open.add(t[i]);
                    }
                }
                if (open.isEmpty()) {
                    return;
                }
                double share = free / open.size();
                double used = 0;
                // 열린 트랙에 몫을 더하고 쓴 양을 합한다.
                for (int i = 0; i < open.size(); i++) {
                    Track tr = open.get(i);
                    double add = Math.min(share, tr.limit - tr.base);
                    tr.base += add;
                    used += add;
                }
                free -= used;
            }
        }

        /**
         * Expands the flexible tracks (CSS Grid 11.7): finds the used flex fraction and gives every flexible track at least that fraction
         * times its flex factor. If the container has no definite size the fraction comes from the base sizes of the flexible tracks and
         * the max-content contributions of the items that span them (and is zero under a min-content constraint); for the rows it is
         * redone with the minimum or maximum height if the result would be smaller or larger.
         * <p>
         * 유연한 트랙을 확장한다(CSS Grid 11.7): 사용 fr 비율을 찾아 모든 유연한 트랙이 적어도 그 비율 × 계수만큼 되게 한다. 컨테이너 크기가 정해지지 않았으면 비율은
         * 유연한 트랙의 기본 크기와 그 트랙에 걸친 항목의 max-content 기여에서 나오며(min-content 제약이면 0), 행은 결과가 최소 높이보다 작거나 최대 높이보다 크면 그 높이로
         * 다시 계산한다.
         *
         * @param axis  the axis / 축
         * @param t     the tracks / 트랙
         * @param avail the size along the axis or NaN / 축 방향 크기 또는 NaN
         * @param g     the gap / gap
         * @param mode  0 normal, 1 min-content, 2 max-content / 0 보통, 1 min-content, 2 max-content
         */
        void expandFlexible(int axis, Track[] t, double avail, double g, int mode) {
            double flexSum = 0;
            // 유연한 트랙의 계수 합을 구한다.
            for (int i = 0; i < t.length; i++) {
                if (!t[i].collapsed && t[i].maxKind == T_FR) {
                    flexSum += t[i].maxVal;
                }
            }
            if (flexSum <= 0 || mode == 1) {
                return;
            }
            double fr;
            if (!Double.isNaN(avail) && mode == 0) {
                fr = frSize(t, 0, t.length, avail, g);
            } else {
                fr = 0;
                // 유연한 트랙마다 계수가 1 보다 크면 기본 크기 / 계수, 아니면 기본 크기를 비율 후보로 한다.
                for (int i = 0; i < t.length; i++) {
                    if (!t[i].collapsed && t[i].maxKind == T_FR) {
                        fr = Math.max(fr, t[i].maxVal > 1 ? t[i].base / t[i].maxVal : t[i].base);
                    }
                }
                // 유연한 트랙에 걸친 항목의 max-content 기여를 채우는 fr 크기도 후보로 한다.
                for (int i = 0; i < items.size(); i++) {
                    Item it = items.get(i);
                    if (spansFlexible(it, axis)) {
                        int to = Math.min(t.length, it.start[axis] + it.span[axis]);
                        fr = Math.max(fr, frSize(t, it.start[axis], to, it.maxC[axis], g));
                    }
                }
                // 행은 이 비율로 그리드가 최소 높이보다 작거나 최대 높이보다 커지면 그 높이를 정해진 크기로 보고 다시 계산한다.
                if (axis == 1) {
                    double extent = g * Math.max(0, activeTracks(t) - 1);
                    // 이 비율로 정해지는 트랙 크기의 합을 구한다.
                    for (int i = 0; i < t.length; i++) {
                        if (t[i].collapsed) {
                            continue;
                        }
                        extent += t[i].maxKind == T_FR ? Math.max(t[i].base, fr * t[i].maxVal) : t[i].base;
                    }
                    if (extent < minH - EPS) {
                        fr = frSize(t, 0, t.length, minH, g);
                    } else if (!Double.isInfinite(maxH) && extent > maxH + EPS) {
                        fr = frSize(t, 0, t.length, maxH, g);
                    }
                }
            }
            // 유연한 트랙의 크기는 fr × 계수와 기본 크기 중 큰 값이다.
            for (int i = 0; i < t.length; i++) {
                if (t[i].maxKind == T_FR && !t[i].collapsed) {
                    t[i].base = Math.max(t[i].base, fr * t[i].maxVal);
                }
            }
        }

        /**
         * Finds the size of an <code>fr</code> for a space to fill (CSS Grid 11.7.1): the leftover space is the space minus the gaps and
         * the base sizes of the inflexible tracks; flexible tracks whose base size is larger than their share are treated as inflexible
         * and the calculation restarts, until all shares are valid.
         * <p>
         * 채울 공간에 대한 <code>fr</code> 의 크기를 찾는다(CSS Grid 11.7.1): 남는 공간은 공간에서 gap 과 유연하지 않은 트랙의 기본 크기를 뺀 값이다. 기본 크기가 자기 몫보다
         * 큰 유연한 트랙은 유연하지 않은 것으로 보고 모든 몫이 유효할 때까지 다시 계산한다.
         *
         * @param t     the tracks / 트랙
         * @param from  the first track / 첫 트랙
         * @param to    the end of the tracks (exclusive) / 트랙의 끝(포함하지 않음)
         * @param space the space to fill / 채울 공간
         * @param g     the gap / gap
         * @return the size of one fr / fr 하나의 크기
         */
        double frSize(Track[] t, int from, int to, double space, double g) {
            boolean[] out = new boolean[Math.max(0, to - from)];
            int active = 0;
            double left = space;
            double sum = 0;
            // 접히지 않은 트랙의 gap 을 빼고 유연하지 않은 트랙의 기본 크기를 빼며 계수의 합을 구한다.
            for (int i = from; i < to; i++) {
                if (t[i].collapsed) {
                    continue;
                }
                active++;
                if (t[i].maxKind == T_FR) {
                    sum += t[i].maxVal;
                } else {
                    left -= t[i].base;
                }
            }
            left -= g * Math.max(0, active - 1);
            double hyp = 0;
            // 몫보다 기본 크기가 큰 트랙을 빼고 다시 계산하는 일을 안정될 때까지 반복한다.
            for (int guard = 0; guard <= out.length; guard++) {
                hyp = Math.max(0, left) / Math.max(1, sum);
                boolean changed = false;
                // 몫보다 기본 크기가 큰 유연한 트랙을 유연하지 않은 것으로 돌린다.
                for (int i = from; i < to; i++) {
                    if (!out[i - from] && !t[i].collapsed && t[i].maxKind == T_FR && t[i].base > hyp * t[i].maxVal + EPS) {
                        out[i - from] = true;
                        left -= t[i].base;
                        sum -= t[i].maxVal;
                        changed = true;
                    }
                }
                if (!changed) {
                    break;
                }
            }
            return hyp;
        }

        /**
         * Computes the offset of the first track and the extra gap between the tracks for the content distribution of an axis
         * (<code>justify-content</code> for columns, <code>align-content</code> for rows). The offsets are measured from the start edge of
         * the axis (the right edge of the columns for <code>direction:rtl</code>); the physical <code>left</code> and <code>right</code>
         * of <code>justify-content</code> are turned into start and end.
         * <p>
         * 축의 콘텐츠 분배(열은 <code>justify-content</code>, 행은 <code>align-content</code>)에 따른 첫 트랙의 오프셋과 트랙 사이 추가 간격을 계산한다. 오프셋은 축의
         * 시작 가장자리(<code>direction:rtl</code> 인 열은 오른쪽)에서 잰다. <code>justify-content</code> 의 물리적 <code>left</code>, <code>right</code> 는 시작과 끝으로
         * 바꾼다.
         *
         * @param axis  the axis / 축
         * @param avail the size of the content box or NaN / content box 의 크기 또는 NaN
         * @param used  the extent of the tracks / 트랙의 범위
         * @return {offset of the first track, extra gap} / {첫 트랙의 오프셋, 추가 간격}
         */
        double[] distributeFree(int axis, double avail, double used) {
            double[] r = new double[2];
            if (Double.isNaN(avail)) {
                return r;
            }
            double free = avail - used;
            String[] w = words(c.style, axis == 0 ? Prop.JUSTIFY_CONTENT : Prop.ALIGN_CONTENT);
            String k = w[w.length - 1];
            boolean safe = false;
            // safe 키워드가 있는지 찾는다.
            for (int i = 0; i < w.length; i++) {
                if ("safe".equals(w[i])) {
                    safe = true;
                }
            }
            if (axis == 0 && ("left".equals(k) || "right".equals(k))) {
                // 물리적 left/right 는 쓰기 방향에 따라 인라인 시작/끝이다.
                k = "left".equals(k) != rtl ? "start" : "end";
            }
            int n = activeTracks(tracks[axis]);
            if (free < 0 && ("center".equals(k) || "end".equals(k) || "flex-end".equals(k)) && safe) {
                return r;
            }
            if ("center".equals(k)) {
                r[0] = free / 2;
            } else if ("end".equals(k) || "flex-end".equals(k)) {
                r[0] = free;
            } else if (free > 0 && "space-between".equals(k) && n > 1) {
                r[1] = free / (n - 1);
            } else if (free > 0 && "space-around".equals(k) && n > 0) {
                r[1] = free / n;
                r[0] = r[1] / 2;
            } else if (free > 0 && "space-evenly".equals(k) && n > 0) {
                r[1] = free / (n + 1);
                r[0] = r[1];
            }
            return r;
        }

        /**
         * Sets the positions of the tracks of an axis. The positions run from the start edge of the axis; the columns of a
         * <code>direction:rtl</code> grid are mirrored in the content box afterwards.
         * <p>
         * 축의 트랙 위치를 정한다. 위치는 축의 시작 가장자리에서 시작하며 <code>direction:rtl</code> 그리드의 열은 나중에 content box 안에서 거울 반사한다.
         *
         * @param axis the axis / 축
         * @param lead {offset of the first track, extra gap} / {첫 트랙의 오프셋, 추가 간격}
         */
        void position(int axis, double[] lead) {
            double pos = lead[0];
            boolean first = true;
            // 트랙을 차례로 놓으며 접힌 트랙은 건너뛴다.
            for (int i = 0; i < tracks[axis].length; i++) {
                Track tr = tracks[axis][i];
                if (tr.collapsed) {
                    tr.pos = pos;
                    continue;
                }
                if (!first) {
                    pos += gap[axis] + lead[1];
                }
                tr.pos = pos;
                pos += tr.size;
                first = false;
            }
            if (axis == 0 && rtl && !Double.isNaN(cw)) {
                // rtl 이면 열이 오른쪽에서 왼쪽으로 놓이므로 content box 너비 안에서 거울 반사한다.
                for (int i = 0; i < tracks[0].length; i++) {
                    tracks[0][i].pos = cw - (tracks[0][i].pos + tracks[0][i].size);
                }
            }
        }

        /**
         * Returns the start of the grid area of an item along an axis (its smaller coordinate).
         * <p>
         * 항목 그리드 영역의 축 방향 시작(작은 쪽 좌표)을 반환한다.
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return the start offset in the content box / content box 안 시작 오프셋
         */
        double areaStart(Item it, int axis) {
            int n = tracks[axis].length;
            int from = Math.min(it.start[axis], n - 1);
            int to = Math.min(n, it.start[axis] + it.span[axis]) - 1;
            if (from < 0 || to < from) {
                return 0;
            }
            return Math.min(tracks[axis][from].pos, tracks[axis][to].pos);
        }

        /**
         * Returns the size of the grid area including the extra gaps of the content distribution.
         * <p>
         * 콘텐츠 분배의 추가 간격을 포함한 그리드 영역의 크기를 반환한다.
         *
         * @param it   the item / 항목
         * @param axis the axis / 축
         * @return the size / 크기
         */
        double areaExtent(Item it, int axis) {
            int n = tracks[axis].length;
            int from = Math.min(it.start[axis], n - 1);
            int to = Math.min(n, it.start[axis] + it.span[axis]) - 1;
            if (from < 0 || to < from) {
                return 0;
            }
            Track a = tracks[axis][from];
            Track b = tracks[axis][to];
            return Math.max(a.pos + a.size, b.pos + b.size) - Math.min(a.pos, b.pos);
        }

        /**
         * Lays out an item in its grid area with the alignment of <code>justify-self</code> and <code>align-self</code> and sets its
         * position.
         * <p>
         * 항목을 <code>justify-self</code> 와 <code>align-self</code> 의 정렬로 그리드 영역 안에 배치하고 위치를 정한다.
         *
         * @param it          the item / 항목
         * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
         * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
         */
        void placeItem(Item it, double contentLeft, double contentTop) {
            LayoutBox b = it.box;
            double ax = areaStart(it, 0);
            double ay = areaStart(it, 1);
            double aw = areaExtent(it, 0);
            double ah = areaExtent(it, 1);
            boolean replaced = b.type == LayoutBox.Type.REPLACED;
            String aa = alignKind(it, false);
            double vx = BlockLayouter.vExtras(b);
            double mL = it.autoMargin[0] ? 0 : it.margin[0];
            double mR = it.autoMargin[1] ? 0 : it.margin[1];
            double mT = it.autoMargin[2] ? 0 : it.margin[2];
            double mB = it.autoMargin[3] ? 0 : it.margin[3];
            double w = itemWidth(it, aw);
            place(it, w, Double.NaN, ah);
            double natH = Math.max(0, b.height - vx);
            double specHh = bl.specHeight(b, ah);
            double forced = Double.NaN;
            if (Double.isNaN(specHh) && b.type != LayoutBox.Type.TABLE && (replaced ? "stretch".equals(aa) : ("stretch".equals(aa) || "normal".equals(aa)))
                    && !(it.autoMargin[2] || it.autoMargin[3])) {
                forced = Math.max(bl.minHeight(b, ah), Math.min(Math.max(0, ah - mT - mB - vx), bl.maxHeight(b, ah)));
                if (Math.abs(forced - natH) > EPS) {
                    place(it, w, forced, ah);
                } else {
                    forced = Double.NaN;
                }
            }
            double outerW = b.width;
            double outerH = b.height;
            double freeW = aw - outerW - mL - mR;
            double freeH = ah - outerH - mT - mB;
            double ml = mL;
            double mr = mR;
            double mt = mT;
            double mb = mB;
            // auto margin 은 남는 공간을 나눠 가진다.
            if (it.autoMargin[0] || it.autoMargin[1]) {
                double each = freeW > 0 ? freeW / ((it.autoMargin[0] ? 1 : 0) + (it.autoMargin[1] ? 1 : 0)) : 0;
                ml = it.autoMargin[0] ? each : mL;
                mr = it.autoMargin[1] ? each : mR;
                freeW = 0;
            }
            if (it.autoMargin[2] || it.autoMargin[3]) {
                double each = freeH > 0 ? freeH / ((it.autoMargin[2] ? 1 : 0) + (it.autoMargin[3] ? 1 : 0)) : 0;
                mt = it.autoMargin[2] ? each : mT;
                mb = it.autoMargin[3] ? each : mB;
                freeH = 0;
            }
            String ja = alignKind(it, true);
            boolean safeX = freeW < 0 && "safe".equals(safeOf(it, true));
            boolean safeY = freeH < 0 && "safe".equals(safeOf(it, false));
            // 인라인 축 정렬: 시작 가장자리는 ltr 이면 왼쪽, rtl 이면 오른쪽이다. 끝/가운데도 그에 맞춘다.
            double offX;
            if (!safeX && "end".equals(ja)) {
                offX = rtl ? 0 : freeW;
            } else if (!safeX && "center".equals(ja)) {
                offX = freeW / 2;
            } else {
                offX = rtl ? freeW : 0;
            }
            double offY = !safeY && "end".equals(aa) ? freeH : (!safeY && "center".equals(aa) ? freeH / 2 : 0);
            b.marginLeft = ml;
            b.marginRight = mr;
            b.marginTop = mt;
            b.marginBottom = mb;
            b.lx = contentLeft + ax + offX + ml;
            b.ly = contentTop + ay + offY + mt;
        }

        /**
         * Returns the content width at which an item is laid out in a grid area of a given width (see {@link #itemWidth(Item)}).
         * <p>
         * 주어진 너비의 그리드 영역 안에서 항목을 배치할 content 너비를 반환한다({@link #itemWidth(Item)} 참조).
         *
         * @param it the item / 항목
         * @param aw the width of the grid area including the extra gaps / 추가 간격을 포함한 그리드 영역의 너비
         * @return the content width / content 너비
         */
        double itemWidth(Item it, double aw) {
            LayoutBox b = it.box;
            boolean replaced = b.type == LayoutBox.Type.REPLACED;
            String ja = alignKind(it, true);
            double mL = it.autoMargin[0] ? 0 : it.margin[0];
            double mR = it.autoMargin[1] ? 0 : it.margin[1];
            double specW = bl.specWidth(b, aw);
            double availW = Math.max(0, aw - mL - mR - BlockLayouter.hExtras(b));
            boolean autoMarginsH = it.autoMargin[0] || it.autoMargin[1];
            double w;
            if (!Double.isNaN(specW)) {
                w = specW;
            } else if (replaced && !"stretch".equals(ja)) {
                w = bl.replacedSize(b, aw, Double.NaN)[0];
            } else if (("stretch".equals(ja) || "normal".equals(ja)) && !autoMarginsH) {
                w = availW;
            } else {
                w = Math.min(Math.max(bl.contentMin(b), availW), bl.contentMax(b));
            }
            return Math.max(bl.minWidth(b, aw), Math.min(w, bl.maxWidth(b, aw)));
        }

        /**
         * Returns <code>safe</code> if the alignment of an item has the <code>safe</code> keyword, else "".
         * <p>
         * 항목의 정렬에 <code>safe</code> 키워드가 있으면 <code>safe</code> 를, 아니면 ""를 반환한다.
         *
         * @param it     the item / 항목
         * @param inline true for the inline axis / 인라인 축이면 true
         * @return "safe" or "" / "safe" 또는 ""
         */
        String safeOf(Item it, boolean inline) {
            String[] w = selfWords(it, inline);
            // safe 키워드를 찾는다.
            for (int i = 0; i < w.length; i++) {
                if ("safe".equals(w[i])) {
                    return "safe";
                }
            }
            return "";
        }

        /**
         * Returns the words of the used self alignment of an item (<code>justify-self</code>/<code>align-self</code>, else the
         * <code>justify-items</code>/<code>align-items</code> of the container).
         * <p>
         * 항목이 쓰는 자기 정렬의 단어를 반환한다(<code>justify-self</code>/<code>align-self</code>, 없으면 컨테이너의 <code>justify-items</code>/<code>align-items</code>).
         *
         * @param it     the item / 항목
         * @param inline true for the inline axis (<code>justify-*</code>) / 인라인 축(<code>justify-*</code>)이면 true
         * @return the words / 단어
         */
        String[] selfWords(Item it, boolean inline) {
            return selfWordsOf(it.anon ? null : it.box.style, inline);
        }

        /**
         * Returns the words of the used self alignment of a box with a style (the style of an absolutely positioned child, for example).
         * <p>
         * 스타일이 있는 상자가 쓰는 자기 정렬의 단어를 반환한다(예: 절대 배치 자식의 스타일).
         *
         * @param s      the style of the box or null for an anonymous box / 상자의 스타일. 익명 상자이면 null
         * @param inline true for the inline axis / 인라인 축이면 true
         * @return the words / 단어
         */
        String[] selfWordsOf(ComputedStyle s, boolean inline) {
            String[] w = s == null ? new String[] { "auto" } : words(s, inline ? Prop.JUSTIFY_SELF : Prop.ALIGN_SELF);
            String last = w[w.length - 1];
            if ("auto".equals(last) || last.length() == 0) {
                w = words(c.style, inline ? Prop.JUSTIFY_ITEMS : Prop.ALIGN_ITEMS);
            }
            return w;
        }

        /**
         * Normalizes the self alignment of an item: <code>stretch</code>, <code>normal</code>, <code>start</code>, <code>end</code> or
         * <code>center</code>. <code>left</code> and <code>right</code> become start or end by the direction; baseline and the
         * unsupported values are <code>start</code>.
         * <p>
         * 항목의 자기 정렬을 정규화한다: <code>stretch</code>, <code>normal</code>, <code>start</code>, <code>end</code>, <code>center</code>.
         * <code>left</code>, <code>right</code> 는 쓰기 방향에 따라 시작이나 끝이 되고 기준선과 지원하지 않는 값은 <code>start</code> 이다.
         *
         * @param it     the item / 항목
         * @param inline true for the inline axis / 인라인 축이면 true
         * @return the kind / 종류
         */
        String alignKind(Item it, boolean inline) {
            return alignKindOf(selfWords(it, inline), inline);
        }

        /**
         * Normalizes the words of a self alignment (see {@link #alignKind(Item, boolean)}).
         * <p>
         * 자기 정렬의 단어를 정규화한다({@link #alignKind(Item, boolean)} 참조).
         *
         * @param w      the words / 단어
         * @param inline true for the inline axis / 인라인 축이면 true
         * @return the kind / 종류
         */
        String alignKindOf(String[] w, boolean inline) {
            String k = w[w.length - 1];
            if ("stretch".equals(k)) {
                return "stretch";
            }
            if ("normal".equals(k) || "legacy".equals(k) || k.length() == 0) {
                return "normal";
            }
            if ("center".equals(k)) {
                return "center";
            }
            if ("end".equals(k) || "flex-end".equals(k) || "self-end".equals(k)) {
                return "end";
            }
            if (inline && "left".equals(k)) {
                return rtl ? "end" : "start";
            }
            if (inline && "right".equals(k)) {
                return rtl ? "start" : "end";
            }
            return "start";
        }

        /**
         * Gives the absolutely positioned children the content box of the container as the area of their static position (CSS Grid 9.2):
         * the child is aligned in it as the only item of a grid area, by its own <code>justify-self</code> and <code>align-self</code>.
         * <p>
         * 절대 배치 자식에게 컨테이너의 content box 를 정적 위치의 영역으로 준다(CSS Grid 9.2): 자식은 그리드 영역의 유일한 항목처럼 자신의 <code>justify-self</code> 와
         * <code>align-self</code> 로 그 안에 정렬된다.
         *
         * @param contentLeft x of the content box / content box 의 x
         * @param contentTop  y of the content box / content box 의 y
         * @param outerH      the height of the content box / content box 의 높이
         */
        void staticPositions(double contentLeft, double contentTop, double outerH) {
            // 절대 배치 자식의 정적 위치 사각형은 컨테이너 content box 이다.
            for (int i = 0; i < absBoxes.size(); i++) {
                LayoutBox a = absBoxes.get(i);
                a.staticX = contentLeft;
                a.staticY = contentTop;
                a.staticW = cw;
                a.staticH = outerH;
                String ja = alignKindOf(selfWordsOf(a.style, true), true);
                String aa = alignKindOf(selfWordsOf(a.style, false), false);
                // 가로: 시작 가장자리는 ltr 이면 왼쪽(0), rtl 이면 오른쪽(끝, 2)이다. 1 은 가운데이다.
                a.staticAlignX = "center".equals(ja) ? 1 : ("end".equals(ja) == rtl ? 0 : 2);
                a.staticAlignY = "center".equals(aa) ? 1 : ("end".equals(aa) ? 2 : 0);
            }
        }

        /**
         * Gives the absolutely positioned children whose containing block is this container the grid area that their grid-placement
         * properties select (CSS Grid 9.1). An edge that has no line, or a line that does not exist in the grid, is the padding edge of the
         * container.
         * <p>
         * 이 컨테이너가 포함 블록인 절대 배치 자식에게 그리드 배치 속성이 고른 그리드 영역을 준다(CSS Grid 9.1). 선이 없거나 그리드에 없는 선인 가장자리는 컨테이너의 padding
         * 가장자리이다.
         *
         * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
         * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
         */
        void absoluteContainingBlocks(double contentLeft, double contentTop) {
            // 절대 배치 자식마다 가로와 세로의 그리드 영역을 구한다.
            for (int i = 0; i < absBoxes.size(); i++) {
                LayoutBox a = absBoxes.get(i);
                a.gridCb = null;
                if (a.absCb != c) {
                    continue;
                }
                double[] x = absoluteRange(a, 0);
                double[] y = absoluteRange(a, 1);
                if (x == null && y == null) {
                    continue;
                }
                a.gridCb = new double[] { x == null ? Double.NaN : contentLeft + x[0], x == null ? Double.NaN : contentLeft + x[1],
                    y == null ? Double.NaN : contentTop + y[0], y == null ? Double.NaN : contentTop + y[1] };
            }
        }

        /**
         * Resolves the grid-placement properties of an absolutely positioned box along an axis into the physical edges of its containing
         * block in the content box.
         * <p>
         * 절대 배치 상자의 그리드 배치 속성을 축 방향으로 풀어 content box 안 포함 블록의 물리적 가장자리로 바꾼다.
         *
         * @param a    the box / 상자
         * @param axis the axis / 축
         * @return {low edge, high edge} (NaN for the padding edge) or null if both edges are the padding edges / {작은 쪽 가장자리, 큰 쪽 가장자리}(padding 가장자리이면 NaN). 둘 다 padding 가장자리이면 null
         */
        double[] absoluteRange(LayoutBox a, int axis) {
            int[] s = lineSpec(a.style.get(axis == 0 ? Prop.GRID_COLUMN_START : Prop.GRID_ROW_START), axis, false);
            int[] e = lineSpec(a.style.get(axis == 0 ? Prop.GRID_COLUMN_END : Prop.GRID_ROW_END), axis, true);
            int n = tracks[axis].length;
            int sl = s[0] == 1 ? s[1] + shift[axis] : -1;
            int el = e[0] == 1 ? e[1] + shift[axis] : -1;
            // 그리드에 없는 선은 auto 로 본다.
            if (sl < 0 || sl > n) {
                sl = -1;
            }
            if (el < 0 || el > n) {
                el = -1;
            }
            // span 은 반대쪽 선이 있을 때만 쓰고 그리드 밖으로 나가면 auto 이다.
            if (sl >= 0 && el < 0 && e[0] == 2 && sl + e[1] <= n) {
                el = sl + e[1];
            } else if (el >= 0 && sl < 0 && s[0] == 2 && el - s[1] >= 0) {
                sl = el - s[1];
            }
            if (sl >= 0 && el >= 0) {
                if (sl > el) {
                    int t = sl;
                    sl = el;
                    el = t;
                } else if (sl == el && el < n) {
                    el = sl + 1;
                }
            }
            if (sl < 0 && el < 0) {
                return null;
            }
            double ps = sl >= 0 ? linePosition(axis, sl) : Double.NaN;
            double pe = el >= 0 ? linePosition(axis, el) : Double.NaN;
            // rtl 열은 시작 선이 오른쪽에 있다.
            if (axis == 0 && rtl) {
                return new double[] { pe, ps };
            }
            return new double[] { ps, pe };
        }

        /**
         * Returns the position of a grid line in the content box: the start edge of the track behind it, or the end edge of the last
         * track for the last line (the columns of a <code>direction:rtl</code> grid are mirrored).
         * <p>
         * content box 안 그리드 선의 위치를 반환한다: 선 뒤 트랙의 시작 가장자리이며 마지막 선은 마지막 트랙의 끝 가장자리이다(<code>direction:rtl</code> 그리드의 열은 거울
         * 반사).
         *
         * @param axis the axis / 축
         * @param line the line index (0 to the number of tracks) / 선 번호(0부터 트랙 수까지)
         * @return the position / 위치
         */
        double linePosition(int axis, int line) {
            Track[] t = tracks[axis];
            if (t.length == 0) {
                return 0;
            }
            boolean mirrored = axis == 0 && rtl;
            if (line < t.length) {
                return mirrored ? t[line].pos + t[line].size : t[line].pos;
            }
            Track last = t[t.length - 1];
            return mirrored ? last.pos : last.pos + last.size;
        }

        /**
         * Returns the baseline of the container: the baseline of the first item of the first row.
         * <p>
         * 컨테이너의 기준선을 반환한다: 첫 행 첫 항목의 기준선.
         *
         * @return the baseline relative to the border box or NaN / border box 기준 기준선. 없으면 NaN
         */
        double containerBaseline() {
            Item best = null;
            // 가장 위(그다음 가장 앞쪽 열)에 있는 항목을 찾는다.
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (best == null || it.start[1] < best.start[1] || (it.start[1] == best.start[1] && it.start[0] < best.start[0])) {
                    best = it;
                }
            }
            if (best == null) {
                return Double.NaN;
            }
            double base = best.box.baseline;
            if (Double.isNaN(base)) {
                base = BlockLayouter.firstBaseline(best.box);
            }
            if (Double.isNaN(base)) {
                base = best.box.height;
            }
            return best.box.ly + base;
        }
    }
}
