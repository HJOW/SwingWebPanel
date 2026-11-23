package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Table layout (plan item P10-02, CSS 2.1 chapter 17): column widths with the automatic and the fixed table layout algorithm, row heights
 * with <code>rowspan</code>, cell alignment, the separated and the collapsing border model, captions and the geometry of all parts. The
 * grid comes from the {@link TableModel}. The steps are: (1) measure the minimum and maximum width of every cell and combine them into
 * columns (cells that span columns raise the columns they cover only as far as needed), (2) find the width of the table (shrink-to-fit
 * or the specified width, never below the minimum), (3) distribute the width over the columns (the minimum first, then percentage
 * columns, columns with a specified width and automatic columns in turn, proportionally between minimum and maximum; with
 * <code>table-layout:fixed</code> and a specified width the first row decides), (4) lay out every cell at the width of its columns,
 * (5) find the row heights (the tallest cell, baseline alignment, row spans, a specified table height), (6) place the rows, move the
 * contents of the cells for <code>vertical-align</code> and put the captions above or below. In the collapsing model the borders are
 * resolved by the {@link TableModel}, the cells and the table get half of them as border widths and the whole segments are recorded for
 * the renderer ({@link TableBorder}).
 * <p>
 * 표 레이아웃(계획 P10-02, CSS 2.1 17장): 자동/고정 표 레이아웃 알고리즘의 열 너비, <code>rowspan</code> 이 있는 행 높이, 셀 정렬, 분리/합치기 border 모델, caption,
 * 모든 부품의 기하. 격자는 {@link TableModel} 에서 온다. 단계: (1) 모든 셀의 최소/최대 너비를 재어 열로 합치고(열에 걸친 셀은 필요한 만큼만 걸친 열을 키운다),
 * (2) 표의 너비를 정하고(shrink-to-fit 또는 지정 너비, 최소보다 작아지지 않음), (3) 너비를 열에 나누고(먼저 최소, 그다음 백분율 열, 너비가 지정된 열, 자동 열 순서로
 * 최소와 최대 사이에서 비례하게. <code>table-layout:fixed</code> 이고 너비가 지정되면 첫 행이 결정), (4) 모든 셀을 열 너비로 배치하고, (5) 행 높이를 정하고(가장 큰
 * 셀, 기준선 정렬, 행 범위, 지정된 표 높이), (6) 행을 놓고 <code>vertical-align</code> 에 따라 셀 내용을 옮기며 caption 을 위나 아래에 놓는다. 합치기 모델에서는
 * {@link TableModel} 이 border 를 해결하고 셀과 표는 그 절반을 border 너비로 가지며 전체 선분은 렌더러를 위해 기록한다({@link TableBorder}).
 */
final class TableLayouter {

    /** Tolerance when comparing widths. 너비 비교 허용 오차. */
    private static final double EPS = 0.001;

    /** The block layouter that lays out the contents of the cells. 셀의 내용을 배치하는 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the table layouter.
     * <p>
     * 표 레이아웃을 만든다.
     *
     * @param bl the block layouter / 블록 레이아웃
     */
    TableLayouter(BlockLayouter bl) {
        this.bl = bl;
    }

    /**
     * Returns the model of a table box, building it the first time.
     * <p>
     * 표 상자의 모델을 반환한다. 처음에는 만든다.
     *
     * @param t the table box / 표 상자
     * @return the model / 모델
     */
    TableModel model(LayoutBox t) {
        if (t.tableModel == null) {
            t.tableModel = new TableModel(t);
            t.collapsedBorders = t.tableModel.collapse;
        }
        return t.tableModel;
    }

    /**
     * Sets the borders and padding of a table box that depend on the border model: in the collapsing model the table has no padding and
     * half of the collapsed outer borders as its border widths (CSS 2.1 17.6.2).
     * <p>
     * 상자의 border 모델에 따라 달라지는 표의 border 와 padding 을 정한다: 합치기 모델에서 표는 padding 이 없고 바깥 합쳐진 border 의 절반이 border 너비이다(CSS 2.1 17.6.2).
     *
     * @param t the table box whose style edges were resolved / 스타일 가장자리가 풀린 표 상자
     */
    void adjustEdges(LayoutBox t) {
        TableModel m = model(t);
        if (!m.collapse) {
            return;
        }
        double[] h = m.outerHalves();
        t.borderTop = h[TableModel.TOP];
        t.borderRight = h[TableModel.RIGHT];
        t.borderBottom = h[TableModel.BOTTOM];
        t.borderLeft = h[TableModel.LEFT];
        t.paddingTop = t.paddingRight = t.paddingBottom = t.paddingLeft = 0;
    }

    // ------------------------------------------------------------------------------------------ measuring

    /**
     * Sets the padding and border widths of a cell: its own (separated model) or half of the collapsed borders (collapsing model); an
     * anonymous cell has none and no cell has margins.
     * <p>
     * 셀의 padding 과 border 너비를 정한다: 분리 모델은 자기 것, 합치기 모델은 합쳐진 border 의 절반. 익명 셀은 없고 셀에는 margin 이 없다.
     *
     * @param m the model / 모델
     * @param c the cell / 셀
     */
    private void cellEdges(TableModel m, TableModel.Cell c) {
        LayoutBox b = c.box;
        b.marginTop = b.marginRight = b.marginBottom = b.marginLeft = 0;
        if (b.node == null) {
            b.paddingTop = b.paddingRight = b.paddingBottom = b.paddingLeft = 0;
            b.borderTop = b.borderRight = b.borderBottom = b.borderLeft = 0;
            return;
        }
        ComputedStyle s = b.style;
        b.paddingTop = Math.max(0, BlockLayouter.nz(s.resolve(Prop.PADDING_TOP, Double.NaN)));
        b.paddingRight = Math.max(0, BlockLayouter.nz(s.resolve(Prop.PADDING_RIGHT, Double.NaN)));
        b.paddingBottom = Math.max(0, BlockLayouter.nz(s.resolve(Prop.PADDING_BOTTOM, Double.NaN)));
        b.paddingLeft = Math.max(0, BlockLayouter.nz(s.resolve(Prop.PADDING_LEFT, Double.NaN)));
        if (m.collapse) {
            double[] h = m.cellHalves(c);
            b.borderTop = h[TableModel.TOP];
            b.borderRight = h[TableModel.RIGHT];
            b.borderBottom = h[TableModel.BOTTOM];
            b.borderLeft = h[TableModel.LEFT];
        } else {
            b.borderTop = Css.border(s, Prop.BORDER_TOP_WIDTH, Prop.BORDER_TOP_STYLE);
            b.borderRight = Css.border(s, Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_RIGHT_STYLE);
            b.borderBottom = Css.border(s, Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_BOTTOM_STYLE);
            b.borderLeft = Css.border(s, Prop.BORDER_LEFT_WIDTH, Prop.BORDER_LEFT_STYLE);
        }
    }

    /**
     * The widths of the columns of a table that do not depend on the available width: minimum, maximum (preferred), a specified width
     * and a percentage.
     * <p>
     * 사용 가능한 너비에 따르지 않는 표 열의 너비: 최소, 최대(선호), 지정 너비, 백분율.
     */
    private static final class Columns {
        /** Minimum outer widths. 최소 바깥 너비. */
        final double[] min;
        /** Maximum (preferred) outer widths. 최대(선호) 바깥 너비. */
        final double[] max;
        /** Specified outer widths or NaN. 지정된 바깥 너비 또는 NaN. */
        final double[] fixed;
        /** Specified percentages or NaN. 지정된 백분율 또는 NaN. */
        final double[] pct;

        /**
         * Creates empty columns.
         * <p>
         * 빈 열을 만든다.
         *
         * @param n number of columns / 열 수
         */
        Columns(int n) {
            min = new double[n];
            max = new double[n];
            fixed = new double[n];
            pct = new double[n];
            // 지정 너비와 백분율은 없는 상태(NaN)로 시작한다.
            for (int i = 0; i < n; i++) {
                fixed[i] = Double.NaN;
                pct[i] = Double.NaN;
            }
        }
    }

    /** The measured columns of each table box that was measured (kept with the box as an opaque model extension). 측정한 표 상자별 열 정보. */
    private final java.util.Map<LayoutBox, Columns> measured = new java.util.IdentityHashMap<LayoutBox, Columns>();

    /**
     * Returns the measured columns of a table, measuring the cells the first time.
     * <p>
     * 표의 측정된 열을 반환한다. 처음에는 셀을 측정한다.
     *
     * @param t the table box / 표 상자
     * @return the columns / 열
     */
    private Columns columns(LayoutBox t) {
        Columns cached = measured.get(t);
        if (cached != null) {
            return cached;
        }
        TableModel m = model(t);
        Columns col = new Columns(m.nCols);
        double[] cmin = new double[m.cells.size()];
        double[] cmax = new double[m.cells.size()];
        // 셀마다 가장자리를 정하고 최소/최대 너비와 지정 너비/백분율을 읽는다.
        for (int i = 0; i < m.cells.size(); i++) {
            TableModel.Cell c = m.cells.get(i);
            cellEdges(m, c);
            double extras = BlockLayouter.hExtras(c.box);
            cmin[i] = bl.contentMin(c.box) + extras;
            cmax[i] = Math.max(cmin[i], bl.contentMax(c.box) + extras);
            double fixed = Double.NaN;
            double pct = Double.NaN;
            if (c.box.node != null) {
                CssValue w = c.box.style.get(Prop.WIDTH);
                if (w instanceof CssPercentage) {
                    pct = ((CssPercentage) w).getValue();
                } else {
                    double px = c.box.style.px(Prop.WIDTH);
                    if (!Double.isNaN(px)) {
                        fixed = "border-box".equals(Css.kw(c.box.style, Prop.BOX_SIZING)) ? px : px + extras;
                        fixed = Math.max(fixed, extras);
                    }
                }
            }
            if (!Double.isNaN(fixed) && bl.quirksMode && isNowrapCell(c.box) && c.box.style.px(Prop.WIDTH) != 0) {
                // 호환 모드의 nowrap 셀은 0 이 아닌 길이 width 가 있으면 최소 너비가 그 width 보다 작지 않다(호환 모드 표준 3.9).
                cmin[i] = Math.max(cmin[i], fixed);
            }
            if (!Double.isNaN(fixed)) {
                // 너비가 지정된 셀의 선호 너비는 그 너비이다(최소보다 작아지지 않는다).
                cmax[i] = Math.max(cmin[i], fixed);
            }
            if (c.col >= col.min.length) {
                continue;
            }
            if (c.colSpan == 1) {
                col.min[c.col] = Math.max(col.min[c.col], cmin[i]);
                col.max[c.col] = Math.max(col.max[c.col], cmax[i]);
                if (!Double.isNaN(fixed)) {
                    col.fixed[c.col] = Double.isNaN(col.fixed[c.col]) ? fixed : Math.max(col.fixed[c.col], fixed);
                }
                if (!Double.isNaN(pct)) {
                    col.pct[c.col] = Double.isNaN(col.pct[c.col]) ? pct : Math.max(col.pct[c.col], pct);
                }
            }
        }
        // 열 요소(col)에 지정된 너비를 반영한다.
        for (int j = 0; j < m.nCols; j++) {
            LayoutBox cb = m.colBox[j];
            if (cb == null || cb.node == null) {
                continue;
            }
            CssValue w = cb.style.get(Prop.WIDTH);
            if (w instanceof CssPercentage) {
                col.pct[j] = Double.isNaN(col.pct[j]) ? ((CssPercentage) w).getValue() : Math.max(col.pct[j], ((CssPercentage) w).getValue());
            } else {
                double px = cb.style.px(Prop.WIDTH);
                if (!Double.isNaN(px)) {
                    col.fixed[j] = Double.isNaN(col.fixed[j]) ? px : Math.max(col.fixed[j], px);
                }
            }
        }
        // 지정 너비가 있는 열의 최대(선호) 너비는 그 너비이다(최소보다 작아지지 않는다).
        for (int j = 0; j < m.nCols; j++) {
            if (!Double.isNaN(col.fixed[j])) {
                col.max[j] = Math.max(col.min[j], col.fixed[j]);
            }
        }
        // 열에 걸친 셀은 걸친 열의 합이 모자랄 때만 그 열들을 키운다.
        for (int i = 0; i < m.cells.size(); i++) {
            TableModel.Cell c = m.cells.get(i);
            if (c.colSpan > 1 && c.col < col.min.length) {
                int end = Math.min(m.nCols, c.col + c.colSpan);
                spread(col.min, col.max, c.col, end, cmin[i] - (c.colSpan - 1) * m.hs);
                spread(col.max, col.max, c.col, end, cmax[i] - (c.colSpan - 1) * m.hs);
            }
        }
        // 최대 너비는 최소 너비보다 작을 수 없다.
        for (int j = 0; j < m.nCols; j++) {
            col.max[j] = Math.max(col.max[j], col.min[j]);
        }
        measured.put(t, col);
        return col;
    }

    /**
     * Raises the widths of the columns from <code>from</code> to <code>to</code> (exclusive) so that their sum reaches a needed width;
     * the missing width is shared in proportion to the weights (equally if all weights are 0).
     * <p>
     * <code>from</code> 부터 <code>to</code> 앞까지 열의 합이 필요한 너비에 이르도록 너비를 키운다. 모자란 너비는 가중치에 비례해 나누고 가중치가 모두 0 이면 같게 나눈다.
     *
     * @param w      the widths that are raised / 키울 너비
     * @param weight the weights / 가중치
     * @param from   first column / 첫 열
     * @param to     end column (exclusive) / 끝 열(제외)
     * @param need   the width that the columns must reach together / 열이 함께 이르러야 하는 너비
     */
    private static void spread(double[] w, double[] weight, int from, int to, double need) {
        double have = 0;
        double sumWeight = 0;
        // 걸친 열의 너비와 가중치의 합을 구한다.
        for (int j = from; j < to; j++) {
            have += w[j];
            sumWeight += Math.max(0, weight[j]);
        }
        double missing = need - have;
        if (missing <= EPS || to <= from) {
            return;
        }
        // 모자란 너비를 가중치에 비례해(없으면 같게) 나눈다.
        for (int j = from; j < to; j++) {
            w[j] += sumWeight > EPS ? missing * Math.max(0, weight[j]) / sumWeight : missing / (to - from);
        }
    }

    /**
     * Returns the minimum width of the content area of a table (the columns and the spacing), also the width of its widest caption.
     * <p>
     * 표 content 영역의 최소 너비(열과 간격)를 반환한다. 가장 넓은 caption 의 너비도 반영한다.
     *
     * @param t the table box / 표 상자
     * @param m the model / 모델
     * @param c the columns / 열
     * @return the width / 너비
     */
    private double minArea(LayoutBox t, TableModel m, Columns c) {
        double sum = spacing(m);
        // 열의 최소 너비를 더한다.
        for (int j = 0; j < m.nCols; j++) {
            sum += c.min[j];
        }
        return sum;
    }

    /**
     * Returns the maximum (preferred) width of the content area of a table: the columns at their maximum, where percentage columns are
     * stretched so that they get their percentage.
     * <p>
     * 표 content 영역의 최대(선호) 너비를 반환한다: 열은 최대 너비이며 백분율 열은 백분율만큼 되도록 늘어난다.
     *
     * @param m the model / 모델
     * @param c the columns / 열
     * @return the width / 너비
     */
    private double maxArea(TableModel m, Columns c) {
        double rest = 0;
        double pctSum = 0;
        double fromPct = 0;
        // 백분율 열과 나머지 열을 나누어 더한다.
        for (int j = 0; j < m.nCols; j++) {
            if (!Double.isNaN(c.pct[j]) && c.pct[j] > 0) {
                pctSum += c.pct[j];
                fromPct = Math.max(fromPct, c.max[j] * 100 / c.pct[j]);
            } else {
                rest += c.max[j];
            }
        }
        double cols;
        if (pctSum >= 100) {
            cols = fromPct;
        } else {
            cols = Math.max(rest / (1 - pctSum / 100), fromPct);
        }
        return Math.max(cols, rest) + spacing(m);
    }

    /**
     * Tells whether a cell box is the box of a <code>td</code> or <code>th</code> element with a <code>nowrap</code> attribute (the cells
     * that the table cell nowrap minimum width quirk of Quirks Mode Standard 3.9 applies to).
     * <p>
     * 셀 상자가 <code>nowrap</code> 속성을 가진 <code>td</code> 나 <code>th</code> 요소의 상자인지 알려 준다(호환 모드 표준 3.9 의 표 셀 nowrap 최소 너비 quirk 가
     * 적용되는 셀).
     *
     * @param box the cell box / 셀 상자
     * @return true for a td or th with the nowrap attribute / nowrap 속성이 있는 td 나 th 이면 true
     */
    private static boolean isNowrapCell(LayoutBox box) {
        if (!(box.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element)) {
            return false;
        }
        org.duckdns.hjow.swingwebpanel.dom.Element e = (org.duckdns.hjow.swingwebpanel.dom.Element) box.node;
        String name = BoxBuilder.lower(e.getLocalName());
        return ("td".equals(name) || "th".equals(name)) && e.hasAttribute("nowrap");
    }

    /**
     * Returns the horizontal border spacing of the table: one space around and between all columns.
     * <p>
     * 표의 가로 border 간격을 반환한다: 모든 열의 둘레와 사이에 하나씩.
     *
     * @param m the model / 모델
     * @return the total spacing / 간격의 합
     */
    private static double spacing(TableModel m) {
        return m.nCols == 0 || m.nRows == 0 ? 0 : (m.nCols + 1) * m.hs;
    }

    /**
     * Returns the width of the widest caption at its minimum or maximum.
     * <p>
     * 가장 넓은 caption 의 최소 또는 최대 너비를 반환한다.
     *
     * @param t   the table box / 표 상자
     * @param max true for the maximum / 최대이면 true
     * @return the outer width / 바깥 너비
     */
    private double captionWidth(LayoutBox t, boolean max) {
        double v = 0;
        // 모든 caption 중 가장 넓은 것을 고른다.
        for (int i = 0; i < t.children.size(); i++) {
            LayoutBox c = t.children.get(i);
            if (c.type == LayoutBox.Type.TABLE_CAPTION) {
                bl.edges(c, Double.NaN);
                double w = max ? bl.contentMax(c) : bl.contentMin(c);
                v = Math.max(v, w + BlockLayouter.hExtras(c) + c.marginLeft + c.marginRight);
            }
        }
        return v;
    }

    /**
     * Returns an intrinsic width of the content box of a table (without its own border and padding), for the shrink-to-fit width of the
     * boxes around it.
     * <p>
     * 표의 content box 의 고유 너비를 반환한다(자신의 border 와 padding 은 제외). 둘러싼 상자의 shrink-to-fit 너비에 쓴다.
     *
     * @param t   the table box whose edges are resolved / 가장자리가 풀린 표 상자
     * @param max true for max-content, false for min-content / max-content 이면 true, min-content 이면 false
     * @return the width / 너비
     */
    double intrinsic(LayoutBox t, boolean max) {
        TableModel m = model(t);
        Columns c = columns(t);
        double area = max ? maxArea(m, c) : minArea(t, m, c);
        double extras = BlockLayouter.hExtras(t);
        double cap = captionWidth(t, max) - extras;
        return Math.max(area, cap);
    }

    // ------------------------------------------------------------------------------------------ widths

    /**
     * Finds the width of the content area of a table (CSS 2.1 17.5.2): the specified width but not below the minimum, else
     * shrink-to-fit between the minimum and the maximum.
     * <p>
     * 표 content 영역의 너비를 정한다(CSS 2.1 17.5.2): 지정 너비(최소보다 작지 않게), 없으면 최소와 최대 사이의 shrink-to-fit.
     *
     * @param t     the table box whose edges are resolved / 가장자리가 풀린 표 상자
     * @param cbW   width of the containing block / 포함 블록의 너비
     * @param avail the space for the border box (the containing block width minus margins) / border box 가 쓸 수 있는 공간(포함 블록 너비에서 margin 을 뺀 값)
     * @param fixedLayout true for the fixed table layout, where the content does not set a minimum width / 고정 표 레이아웃이면 true. 내용은 최소 너비를 정하지 않는다
     * @return the width of the area inside the border and padding / border 와 padding 안쪽 영역의 너비
     */
    private double areaWidth(LayoutBox t, double cbW, double avail, boolean fixedLayout) {
        TableModel m = model(t);
        Columns c = columns(t);
        double extras = BlockLayouter.hExtras(t);
        double min = Math.max(minArea(t, m, c), captionWidth(t, false) - extras);
        double max = Math.max(maxArea(m, c), captionWidth(t, true) - extras);
        double spec = bl.specWidth(t, cbW);
        double w;
        if (Double.isNaN(spec)) {
            w = Math.min(Math.max(min, Math.max(0, avail - extras)), max);
        } else {
            w = fixedLayout ? spec : Math.max(spec, min);
        }
        double maxW = bl.maxWidth(t, cbW);
        double minW = bl.minWidth(t, cbW);
        w = Math.max(minW, Math.min(w, maxW));
        return fixedLayout ? w : Math.max(w, min);
    }

    /**
     * Distributes the width of the area over the columns and returns the outer widths of the columns.
     * <p>
     * 영역의 너비를 열에 나누고 열의 바깥 너비를 반환한다.
     *
     * @param t       the table box / 표 상자
     * @param area    the width of the area inside the border and padding / border 와 padding 안쪽 영역의 너비
     * @param fixedOk true if the fixed table layout applies / 고정 표 레이아웃이 적용되면 true
     * @return the column widths / 열 너비
     */
    private double[] distribute(LayoutBox t, double area, boolean fixedOk) {
        TableModel m = model(t);
        Columns c = columns(t);
        int n = m.nCols;
        double[] w = new double[n];
        if (n == 0) {
            return w;
        }
        double room = area - spacing(m);
        if (fixedOk) {
            return fixedColumns(t, m, c, room);
        }
        double sumMin = 0;
        // 모든 열은 최소 너비에서 시작한다.
        for (int j = 0; j < n; j++) {
            w[j] = c.min[j];
            sumMin += c.min[j];
        }
        double extra = room - sumMin;
        if (extra <= EPS) {
            return w;
        }
        double pctTotal = 0;
        // 백분율의 합이 100 을 넘으면 비례해 줄인다.
        for (int j = 0; j < n; j++) {
            if (!Double.isNaN(c.pct[j])) {
                pctTotal += c.pct[j];
            }
        }
        double pctScale = pctTotal > 100 ? 100 / pctTotal : 1;
        double[] need = new double[n];
        // 1 단계: 백분율 열은 백분율에 해당하는 너비까지.
        for (int j = 0; j < n; j++) {
            need[j] = !Double.isNaN(c.pct[j]) ? Math.max(0, c.pct[j] * pctScale / 100 * room - w[j]) : 0;
        }
        extra -= grow(w, need, extra);
        // 2 단계: 너비가 지정된 열은 그 너비(최대)까지.
        for (int j = 0; j < n; j++) {
            need[j] = Double.isNaN(c.pct[j]) && !Double.isNaN(c.fixed[j]) ? Math.max(0, c.max[j] - w[j]) : 0;
        }
        extra -= grow(w, need, extra);
        // 3 단계: 자동 열은 최대 너비까지(최소와 최대의 차에 비례).
        for (int j = 0; j < n; j++) {
            need[j] = Double.isNaN(c.pct[j]) && Double.isNaN(c.fixed[j]) ? Math.max(0, c.max[j] - w[j]) : 0;
        }
        extra -= grow(w, need, extra);
        if (extra > EPS) {
            // 4 단계: 남은 너비는 자동 열에 최대 너비에 비례해 준다(자동 열이 없으면 백분율이 아닌 열, 그것도 없으면 모든 열).
            double[] weight = new double[n];
            int kind = 2;
            // 가장 우선하는 종류의 열을 고른다.
            while (kind >= 0) {
                double sum = 0;
                // 이 종류에 해당하는 열에 가중치(최대 너비)를 주고 가중치의 합을 구한다.
                for (int j = 0; j < n; j++) {
                    boolean pick = kind == 2 ? Double.isNaN(c.pct[j]) && Double.isNaN(c.fixed[j]) : (kind == 1 ? Double.isNaN(c.pct[j]) : true);
                    weight[j] = pick ? Math.max(c.max[j], 1) : 0;
                    sum += weight[j];
                }
                if (sum > EPS) {
                    // 고른 열에 가중치에 비례해 나눈다.
                    for (int j = 0; j < n; j++) {
                        w[j] += extra * weight[j] / sum;
                    }
                    break;
                }
                kind--;
            }
        }
        return w;
    }

    /**
     * Adds to the widths in proportion to the needs, at most the available extra width, and returns what was added.
     * <p>
     * 필요한 만큼에 비례해 너비를 키우되 쓸 수 있는 여분을 넘지 않으며 더한 양을 반환한다.
     *
     * @param w     the widths / 너비
     * @param need  the wanted growth of every column / 열마다 원하는 증가량
     * @param extra the available extra width / 쓸 수 있는 여분
     * @return the added width / 더한 너비
     */
    private static double grow(double[] w, double[] need, double extra) {
        double total = 0;
        // 필요한 증가량의 합을 구한다.
        for (int j = 0; j < need.length; j++) {
            total += need[j];
        }
        if (total <= EPS || extra <= EPS) {
            return 0;
        }
        double give = Math.min(extra, total);
        // 필요한 만큼에 비례해 나눠 준다.
        for (int j = 0; j < need.length; j++) {
            w[j] += need[j] * give / total;
        }
        return give;
    }

    /**
     * Finds the column widths of the fixed table layout (CSS 2.1 17.5.2.1): a column gets the width of its <code>col</code>, else of
     * the cell of the first row that starts in it (a cell that spans columns gives each the same share), columns without a width share
     * what is left equally, and if the columns together are narrower than the table the extra is shared equally. The table becomes
     * wider if the columns need more room.
     * <p>
     * 고정 표 레이아웃의 열 너비를 정한다(CSS 2.1 17.5.2.1): 열은 <code>col</code> 의 너비, 없으면 그 열에서 시작하는 첫 행 셀의 너비를 얻는다(열에 걸친 셀은 각 열에
     * 같은 몫을 준다). 너비가 없는 열은 남은 것을 같게 나누고 열의 합이 표보다 좁으면 여분을 같게 나눈다. 열이 더 많은 공간을 필요로 하면 표가 넓어진다.
     *
     * @param t    the table box / 표 상자
     * @param m    the model / 모델
     * @param c    the columns / 열
     * @param room the width that is available for the columns / 열이 쓸 수 있는 너비
     * @return the column widths / 열 너비
     */
    private double[] fixedColumns(LayoutBox t, TableModel m, Columns c, double room) {
        int n = m.nCols;
        double[] w = new double[n];
        // 열 요소의 너비를 먼저 쓴다.
        for (int j = 0; j < n; j++) {
            w[j] = Double.NaN;
            if (!Double.isNaN(c.pct[j]) && m.colBox[j] != null && m.colBox[j].node != null && m.colBox[j].style.get(Prop.WIDTH) instanceof CssPercentage) {
                w[j] = c.pct[j] / 100 * room;
            } else if (m.colBox[j] != null && m.colBox[j].node != null && !Double.isNaN(m.colBox[j].style.px(Prop.WIDTH))) {
                w[j] = m.colBox[j].style.px(Prop.WIDTH);
            }
        }
        // 첫 행에서 시작하는 셀의 너비를 열 요소에 너비가 없는 열에 쓴다.
        for (int i = 0; i < m.cells.size(); i++) {
            TableModel.Cell cell = m.cells.get(i);
            if (cell.row != 0 || cell.box.node == null) {
                continue;
            }
            double extras = BlockLayouter.hExtras(cell.box);
            double spec = Double.NaN;
            CssValue wv = cell.box.style.get(Prop.WIDTH);
            if (wv instanceof CssPercentage) {
                spec = ((CssPercentage) wv).getValue() / 100 * room;
            } else if (!Double.isNaN(cell.box.style.px(Prop.WIDTH))) {
                double px = cell.box.style.px(Prop.WIDTH);
                spec = "border-box".equals(Css.kw(cell.box.style, Prop.BOX_SIZING)) ? px : px + extras;
            }
            if (Double.isNaN(spec)) {
                continue;
            }
            int end = Math.min(n, cell.col + cell.colSpan);
            // 걸친 열에 같은 몫을 준다(간격은 뺀다).
            for (int j = cell.col; j < end; j++) {
                if (Double.isNaN(w[j])) {
                    w[j] = Math.max(0, (spec - (cell.colSpan - 1) * m.hs) / cell.colSpan);
                }
            }
        }
        double known = 0;
        int unknown = 0;
        // 너비가 정해진 열의 합과 정해지지 않은 열의 수를 센다.
        for (int j = 0; j < n; j++) {
            if (Double.isNaN(w[j])) {
                unknown++;
            } else {
                known += w[j];
            }
        }
        if (unknown > 0) {
            double share = Math.max(0, room - known) / unknown;
            // 정해지지 않은 열은 남은 너비를 같게 나눈다.
            for (int j = 0; j < n; j++) {
                if (Double.isNaN(w[j])) {
                    w[j] = share;
                }
            }
        } else if (known < room - EPS) {
            // 열이 모두 정해졌는데 표보다 좁으면 여분을 같게 나눈다.
            double add = (room - known) / n;
            for (int j = 0; j < n; j++) {
                w[j] += add;
            }
        }
        return w;
    }

    // ------------------------------------------------------------------------------------------ layout

    /**
     * Returns the margin box width of a table that is laid out as an atomic box (a float or <code>inline-table</code>), without laying
     * it out (used when intrinsic widths are measured).
     * <p>
     * 원자적 상자(float 나 <code>inline-table</code>)로 배치되는 표의 margin box 너비를 배치하지 않고 반환한다(고유 너비를 잴 때 쓴다).
     *
     * @param t     the table box / 표 상자
     * @param availW the width of the containing block / 포함 블록의 너비
     * @return the margin box width / margin box 너비
     */
    double outerWidth(LayoutBox t, double availW) {
        bl.edges(t, availW);
        double extras = BlockLayouter.hExtras(t);
        boolean fixedLayout = "fixed".equals(Css.kw(t.style, Prop.TABLE_LAYOUT)) && !Double.isNaN(bl.specWidth(t, availW));
        return areaWidth(t, availW, availW - t.marginLeft - t.marginRight, fixedLayout) + extras + t.marginLeft + t.marginRight;
    }

    /**
     * Lays out a table box: width, columns, cells, rows, captions and the collapsed border segments. The caller sets the position.
     * <p>
     * 표 상자를 배치한다: 너비, 열, 셀, 행, caption, 합쳐진 border 선분. 위치는 호출자가 정한다.
     *
     * @param t      the table box / 표 상자
     * @param cbW    width of the containing block / 포함 블록의 너비
     * @param atomic true if it is laid out as an atomic box (auto margins are 0) / 원자적 상자로 배치하면 true(auto margin 은 0)
     */
    void layout(LayoutBox t, double cbW, boolean atomic) {
        int auto = bl.edges(t, cbW);
        TableModel m = model(t);
        double extras = BlockLayouter.hExtras(t);
        boolean fixedLayout = "fixed".equals(Css.kw(t.style, Prop.TABLE_LAYOUT)) && !Double.isNaN(bl.specWidth(t, cbW));
        double area = areaWidth(t, cbW, cbW - t.marginLeft - t.marginRight, fixedLayout);
        // 이전 배치가 만든 하위 상자 자료를 새로 시작한다.
        t.outTop = Margin.of(t.marginTop);
        t.outBottom = Margin.of(t.marginBottom);
        t.through = false;
        t.baseline = Double.NaN;
        double[] colW = distribute(t, area, fixedLayout);
        double sumW = spacing(m);
        // 열 너비의 합으로 실제 영역 너비를 정한다(열이 더 필요하면 표가 넓어진다).
        for (int j = 0; j < colW.length; j++) {
            sumW += colW[j];
        }
        double areaW = Math.max(area, sumW);
        t.width = areaW + extras;
        if (!atomic) {
            // 블록 수준 표는 auto margin 과 남는 너비를 실제 너비로 푼다.
            bl.resolveMargins(t, cbW, auto, areaW, extras);
        }
        place(t, m, colW, areaW);
        if (t.absList != null) {
            bl.absolute.layoutContaining(t);
        }
    }

    /**
     * Lays out the cells at their column widths, finds the row heights, places all parts and the captions and sets the size of the table.
     * <p>
     * 셀을 열 너비로 배치하고 행 높이를 정하며 모든 부품과 caption 을 놓고 표의 크기를 정한다.
     *
     * @param t     the table box / 표 상자
     * @param m     the model / 모델
     * @param colW  the column widths / 열 너비
     * @param areaW the width of the area inside border and padding / border 와 padding 안쪽 영역의 너비
     */
    private void place(LayoutBox t, TableModel m, double[] colW, double areaW) {
        int nRows = m.nRows;
        int nCols = m.nCols;
        double hs = m.hs;
        double vs = m.vs;
        // 캡션의 위/아래 높이를 구하려고 먼저 caption 을 배치한다(표 상자의 너비는 정해졌다).
        double topH = 0;
        double bottomH = 0;
        List<LayoutBox> topCaps = new ArrayList<LayoutBox>();
        List<LayoutBox> bottomCaps = new ArrayList<LayoutBox>();
        // 각 caption 을 border box 너비에 맞춰 배치하고 위/아래로 나눈다.
        for (int i = 0; i < t.children.size(); i++) {
            LayoutBox cap = t.children.get(i);
            if (cap.type != LayoutBox.Type.TABLE_CAPTION) {
                continue;
            }
            bl.edges(cap, t.width);
            double spec = bl.specWidth(cap, t.width);
            double cw = Double.isNaN(spec) ? Math.max(0, t.width - cap.marginLeft - cap.marginRight - BlockLayouter.hExtras(cap)) : spec;
            bl.finish(cap, cw, Double.NaN, null);
            double outer = cap.marginTop + cap.height + cap.marginBottom;
            if ("bottom".equals(Css.kw(cap.style, Prop.CAPTION_SIDE))) {
                bottomCaps.add(cap);
                bottomH += outer;
            } else {
                topCaps.add(cap);
                topH += outer;
            }
        }
        double padL = t.borderLeft + t.paddingLeft;
        double padT = t.borderTop + t.paddingTop;
        // 셀을 열 너비로 배치한다.
        for (int i = 0; i < m.cells.size(); i++) {
            TableModel.Cell c = m.cells.get(i);
            cellEdges(m, c);
            if (c.row >= nRows || c.col >= nCols) {
                continue;
            }
            int endCol = Math.min(nCols, c.col + c.colSpan);
            double slotW = (endCol - c.col - 1) * hs;
            // 걸친 열의 너비를 더한다.
            for (int j = c.col; j < endCol; j++) {
                slotW += colW[j];
            }
            double cw = Math.max(0, slotW - BlockLayouter.hExtras(c.box));
            bl.finish(c.box, cw, Double.NaN, null);
            c.box.width = Math.max(slotW, c.box.width);
        }
        // 행 높이를 정한다: 한 행짜리 셀(기준선 정렬 포함), 그다음 여러 행에 걸친 셀, 마지막으로 지정된 표 높이.
        double[] rowH = new double[nRows];
        double[] above = new double[nRows];
        double[] below = new double[nRows];
        // 행 요소에 지정된 높이를 최솟값으로 쓴다.
        for (int r = 0; r < nRows; r++) {
            LayoutBox row = m.rows.get(r);
            double rh = row.node == null ? Double.NaN : row.style.px(Prop.HEIGHT);
            rowH[r] = Double.isNaN(rh) ? 0 : rh;
        }
        // 한 행짜리 셀로 행 높이와 기준선 정렬 범위를 정한다.
        for (int i = 0; i < m.cells.size(); i++) {
            TableModel.Cell c = m.cells.get(i);
            if (c.row >= nRows || c.rowSpan != 1) {
                continue;
            }
            if (alignOf(c.box) == 3) {
                double a = BlockLayouter.firstBaseline(c.box);
                if (Double.isNaN(a)) {
                    a = c.box.height;
                }
                above[c.row] = Math.max(above[c.row], a);
                below[c.row] = Math.max(below[c.row], c.box.height - a);
            } else {
                rowH[c.row] = Math.max(rowH[c.row], c.box.height);
            }
        }
        // 기준선 정렬 셀이 있는 행은 기준선 위아래 범위의 합 이상이어야 한다.
        for (int r = 0; r < nRows; r++) {
            rowH[r] = Math.max(rowH[r], above[r] + below[r]);
        }
        // 여러 행에 걸친 셀이 걸친 행의 합보다 크면 모자란 높이를 걸친 행에 같게 나눈다.
        for (int i = 0; i < m.cells.size(); i++) {
            TableModel.Cell c = m.cells.get(i);
            if (c.row >= nRows || c.rowSpan <= 1) {
                continue;
            }
            int endRow = Math.min(nRows, c.row + c.rowSpan);
            double have = (endRow - c.row - 1) * vs;
            // 걸친 행의 높이를 더한다.
            for (int r = c.row; r < endRow; r++) {
                have += rowH[r];
            }
            double missing = c.box.height - have;
            if (missing > EPS) {
                // 모자란 높이를 걸친 행에 같게 나눈다.
                for (int r = c.row; r < endRow; r++) {
                    rowH[r] += missing / (endRow - c.row);
                }
            }
        }
        double gridInner = nRows == 0 || nCols == 0 ? 0 : (nRows + 1) * vs;
        // 행 높이의 합을 더해 격자 안쪽 높이를 구한다.
        for (int r = 0; r < nRows; r++) {
            gridInner += rowH[r];
        }
        double extrasV = t.borderTop + t.paddingTop + t.paddingBottom + t.borderBottom;
        double specH = bl.specHeight(t, Double.NaN);
        if (!Double.isNaN(specH) && nRows > 0 && nCols > 0) {
            double target = specH;
            double missing = target - gridInner;
            if (missing > EPS) {
                double total = 0;
                // 행 높이의 합을 구해 모자란 높이를 행 높이에 비례해(0 이면 같게) 나눈다.
                for (int r = 0; r < nRows; r++) {
                    total += rowH[r];
                }
                // 행마다 높이에 비례해(합이 0 이면 같게) 모자란 높이를 더한다.
                for (int r = 0; r < nRows; r++) {
                    rowH[r] += total > EPS ? missing * rowH[r] / total : missing / nRows;
                }
                gridInner += missing;
            }
        }
        double gridH = gridInner + extrasV;
        // 격자선(행 경계)의 위치를 구한다(표 격자 위쪽 기준).
        double[] rowY = new double[nRows + 1];
        double y = padT + vs;
        // 행을 위에서 아래로 쌓는다.
        for (int r = 0; r < nRows; r++) {
            rowY[r] = y;
            y += rowH[r] + vs;
        }
        rowY[nRows] = y - vs;
        double[] colX = new double[nCols + 1];
        double x = padL + hs;
        // 열을 왼쪽에서 오른쪽으로 쌓는다.
        for (int j = 0; j < nCols; j++) {
            colX[j] = x;
            x += colW[j] + hs;
        }
        colX[nCols] = x;
        // 표 상자의 크기와 격자 사각형(위쪽 caption 아래)을 정한다.
        double captionTop = 0;
        // 위쪽 caption 을 위에서부터 쌓는다.
        for (int i = 0; i < topCaps.size(); i++) {
            LayoutBox cap = topCaps.get(i);
            cap.lx = cap.marginLeft;
            cap.ly = captionTop + cap.marginTop;
            captionTop += cap.marginTop + cap.height + cap.marginBottom;
        }
        t.grid = new double[] { 0, topH, t.width, gridH };
        t.height = topH + gridH + bottomH;
        double captionBottom = topH + gridH;
        // 아래쪽 caption 을 격자 아래에서부터 쌓는다.
        for (int i = 0; i < bottomCaps.size(); i++) {
            LayoutBox cap = bottomCaps.get(i);
            cap.lx = cap.marginLeft;
            cap.ly = captionBottom + cap.marginTop;
            captionBottom += cap.marginTop + cap.height + cap.marginBottom;
        }
        positionParts(t, m, colX, rowY, rowH, above, topH, hs, vs);
        t.baseline = Double.NaN;
        // inline-table 의 기준선은 첫 행의 기준선이다(없으면 첫 셀의 첫 줄 기준선).
        if (nRows > 0 && nCols > 0) {
            double rb = above[0];
            // 기준선 정렬 셀이 없으면 첫 행의 첫 셀에서 기준선을 찾는다.
            for (int i = 0; i < m.cells.size() && rb <= 0; i++) {
                TableModel.Cell c = m.cells.get(i);
                if (c.row == 0) {
                    double a = BlockLayouter.firstBaseline(c.box);
                    if (!Double.isNaN(a)) {
                        rb = a + (alignOf(c.box) == 1 ? (rowH[0] - c.box.height) / 2 : (alignOf(c.box) == 2 ? rowH[0] - c.box.height : 0));
                    }
                }
            }
            if (rb > 0) {
                t.baseline = topH + rowY[0] + rb;
            }
        }
        if (m.collapse) {
            t.collapsedEdges = borderSegments(m, colX, rowY, topH);
        } else {
            t.collapsedEdges = null;
        }
        t.collapsedBorders = m.collapse;
    }

    /**
     * Places the columns, row groups, rows and cells (all positions relative to the parent box) and moves the content of the cells for
     * their vertical alignment.
     * <p>
     * 열, 행 그룹, 행, 셀을 놓고(모든 위치는 부모 상자 기준) 셀 내용을 세로 정렬에 따라 옮긴다.
     *
     * @param t     the table / 표
     * @param m     the model / 모델
     * @param colX  the x of the grid lines (relative to the table) / 격자선의 x(표 기준)
     * @param rowY  the y of the grid lines (relative to the grid) / 격자선의 y(격자 기준)
     * @param rowH  the row heights / 행 높이
     * @param above the part of baseline-aligned cells above the baseline of every row / 행마다 기준선 위쪽 부분
     * @param topH  the height of the captions above the grid / 격자 위 caption 의 높이
     * @param hs    horizontal spacing / 가로 간격
     * @param vs    vertical spacing / 세로 간격
     */
    private void positionParts(LayoutBox t, TableModel m, double[] colX, double[] rowY, double[] rowH, double[] above, double topH, double hs,
            double vs) {
        int nRows = m.nRows;
        int nCols = m.nCols;
        double left = nCols == 0 ? 0 : colX[0];
        double right = nCols == 0 ? 0 : colX[nCols] - hs;
        double top = nRows == 0 ? 0 : rowY[0];
        double bottom = nRows == 0 ? 0 : rowY[nRows];
        // 열과 열 그룹: 격자 높이 전체에 걸친 사각형(표 기준)이다.
        for (int i = 0; i < t.children.size(); i++) {
            LayoutBox c = t.children.get(i);
            if (c.type == LayoutBox.Type.TABLE_COLUMN) {
                placeColumn(c, m, colX, top, bottom, topH, 0, 0, left);
            } else if (c.type == LayoutBox.Type.TABLE_COLUMN_GROUP) {
                int first = c.colSpan == 0 && !c.children.isEmpty() ? c.children.get(0).tableCol : c.tableCol;
                int last = first;
                if (c.colSpan == 0) {
                    // 자식 열의 끝 열을 구한다.
                    for (int k = 0; k < c.children.size(); k++) {
                        last = Math.max(last, c.children.get(k).tableCol + Math.max(1, c.children.get(k).colSpan) - 1);
                    }
                } else {
                    last = first + c.colSpan - 1;
                }
                first = Math.min(first, nCols);
                last = Math.min(Math.max(last, first), Math.max(0, nCols - 1));
                setRect(c, nCols == 0 ? 0 : colX[first], topH + top, nCols == 0 ? 0 : colX[last + 1] - hs - colX[first], bottom - top, 0, 0);
                // 그룹 안의 열은 그룹 기준 위치를 쓴다.
                for (int k = 0; k < c.children.size(); k++) {
                    placeColumn(c.children.get(k), m, colX, top, bottom, topH, c.tx, c.ty, left);
                }
            }
        }
        // 행 그룹과 행과 셀을 놓는다.
        int r = 0;
        for (int i = 0; i < t.children.size(); i++) {
            LayoutBox g = t.children.get(i);
            if (g.type != LayoutBox.Type.TABLE_ROW_GROUP) {
                continue;
            }
            int firstRow = r;
            int count = 0;
            // 그룹의 행 수를 센다.
            for (int k = 0; k < g.children.size(); k++) {
                if (g.children.get(k).type == LayoutBox.Type.TABLE_ROW) {
                    count++;
                }
            }
            int lastRow = Math.min(nRows, firstRow + count) - 1;
            if (lastRow < firstRow) {
                setRect(g, left, topH + top, right - left, 0, 0, 0);
                r += count;
                continue;
            }
            double gy = rowY[firstRow];
            double gh = rowY[lastRow] + rowH[lastRow] - gy;
            setRect(g, left, topH + gy, right - left, gh, 0, 0);
            // 그룹의 행을 놓는다.
            for (int k = 0; k < g.children.size(); k++) {
                LayoutBox row = g.children.get(k);
                if (row.type != LayoutBox.Type.TABLE_ROW || r >= nRows) {
                    continue;
                }
                setRect(row, left, topH + rowY[r], right - left, rowH[r], g.tx, g.ty);
                // 행의 셀을 놓는다.
                for (int q = 0; q < row.children.size(); q++) {
                    LayoutBox cell = row.children.get(q);
                    if (cell.type != LayoutBox.Type.TABLE_CELL) {
                        continue;
                    }
                    placeCell(cell, m, colX, rowY, rowH, above, topH, row, hs, vs);
                }
                r++;
            }
        }
    }

    /**
     * Gives a box a rectangle in the coordinates of the table and the offset in its parent.
     * <p>
     * 상자에 표 좌표의 사각형과 부모 안의 오프셋을 준다.
     *
     * @param b  the box / 상자
     * @param x  x in the table / 표 안 x
     * @param y  y in the table / 표 안 y
     * @param w  width / 너비
     * @param h  height / 높이
     * @param px x of the parent in the table / 표 안 부모의 x
     * @param py y of the parent in the table / 표 안 부모의 y
     */
    private static void setRect(LayoutBox b, double x, double y, double w, double h, double px, double py) {
        b.tx = x;
        b.ty = y;
        b.width = Math.max(0, w);
        b.height = Math.max(0, h);
        b.lx = x - px;
        b.ly = y - py;
    }

    /**
     * Places a column box.
     * <p>
     * 열 상자를 놓는다.
     *
     * @param c      the column / 열
     * @param m      the model / 모델
     * @param colX   x of the grid lines / 격자선 x
     * @param top    y of the first row line in the grid / 격자 안 첫 행선의 y
     * @param bottom y of the last row line / 마지막 행선의 y
     * @param topH   the height above the grid / 격자 위 높이
     * @param px     x of the parent in the table / 표 안 부모의 x
     * @param py     y of the parent in the table / 표 안 부모의 y
     * @param left   x of the first column / 첫 열의 x
     */
    private static void placeColumn(LayoutBox c, TableModel m, double[] colX, double top, double bottom, double topH, double px, double py,
            double left) {
        int first = Math.min(c.tableCol, Math.max(0, m.nCols - 1));
        int last = Math.min(m.nCols - 1, c.tableCol + Math.max(1, c.colSpan) - 1);
        if (m.nCols == 0) {
            setRect(c, left, topH + top, 0, bottom - top, px, py);
            return;
        }
        setRect(c, colX[first], topH + top, colX[Math.max(first, last) + 1] - m.hs - colX[first], bottom - top, px, py);
    }

    /**
     * Returns the vertical alignment of a cell: 0 top, 1 middle, 2 bottom, 3 baseline.
     * <p>
     * 셀의 세로 정렬을 반환한다: 0 top, 1 middle, 2 bottom, 3 baseline.
     *
     * @param cell the cell / 셀
     * @return the alignment / 정렬
     */
    private static int alignOf(LayoutBox cell) {
        String v = Css.kw(cell.style, Prop.VERTICAL_ALIGN);
        if ("top".equals(v)) {
            return 0;
        }
        if ("middle".equals(v)) {
            return 1;
        }
        if ("bottom".equals(v)) {
            return 2;
        }
        return 3;
    }

    /**
     * Places a cell in the slot of its rows and columns and moves its content for the vertical alignment.
     * <p>
     * 셀을 행과 열이 만드는 칸에 놓고 세로 정렬에 따라 내용을 옮긴다.
     *
     * @param cell  the cell / 셀
     * @param m     the model / 모델
     * @param colX  x of the column lines / 열선 x
     * @param rowY  y of the row lines / 행선 y
     * @param rowH  the row heights / 행 높이
     * @param above the baseline offsets of the rows / 행의 기준선 오프셋
     * @param topH  the height above the grid / 격자 위 높이
     * @param row   the row box / 행 상자
     * @param hs    horizontal spacing / 가로 간격
     * @param vs    vertical spacing / 세로 간격
     */
    private void placeCell(LayoutBox cell, TableModel m, double[] colX, double[] rowY, double[] rowH, double[] above, double topH,
            LayoutBox row, double hs, double vs) {
        int r = cell.tableRow;
        int c = cell.tableCol;
        if (r >= m.nRows || c >= m.nCols) {
            return;
        }
        int endCol = Math.min(m.nCols, c + cell.colSpan);
        int endRow = Math.min(m.nRows, r + cell.rowSpan);
        double slotW = colX[endCol] - hs - colX[c];
        double slotH = rowY[endRow - 1] - rowY[r] + rowH[endRow - 1];
        double natural = cell.height;
        double dy = 0;
        int align = alignOf(cell);
        if (align == 1) {
            dy = (slotH - natural) / 2;
        } else if (align == 2) {
            dy = slotH - natural;
        } else if (align == 3 && cell.rowSpan == 1) {
            double a = BlockLayouter.firstBaseline(cell);
            dy = Double.isNaN(a) ? 0 : above[r] - a;
        }
        if (dy > EPS) {
            // 셀의 내용(줄, 하위 블록, float)을 세로 정렬만큼 아래로 옮긴다.
            for (int i = 0; i < cell.children.size(); i++) {
                cell.children.get(i).ly += dy;
            }
        }
        setRect(cell, colX[c], topH + rowY[r], slotW, slotH, row.tx, row.ty);
        cell.hiddenEmptyCell = !m.collapse && "hide".equals(Css.kw(cell.style, Prop.EMPTY_CELLS)) && isEmpty(cell);
        cell.collapsedBorders = m.collapse;
        if (cell.absList != null) {
            bl.absolute.layoutContaining(cell);
        }
    }

    /**
     * Tells whether a cell has no content that is shown: no line boxes, no blocks with content, no replaced boxes (CSS 2.1 17.6.1.1).
     * <p>
     * 셀에 보이는 내용이 없는지 알려 준다: 줄 상자도, 내용이 있는 블록도, 치환 상자도 없음(CSS 2.1 17.6.1.1).
     *
     * @param cell the cell / 셀
     * @return true if empty / 비어 있으면 true
     */
    private static boolean isEmpty(LayoutBox cell) {
        // 자식 중 줄, 치환 상자, 내용이 있는 블록이 있는지 확인한다.
        for (int i = 0; i < cell.children.size(); i++) {
            LayoutBox k = cell.children.get(i);
            if (k.type == LayoutBox.Type.LINE || k.type == LayoutBox.Type.REPLACED || k.type == LayoutBox.Type.MARKER || k.type == LayoutBox.Type.TABLE) {
                return false;
            }
            if ((k.type == LayoutBox.Type.BLOCK || k.type == LayoutBox.Type.ANONYMOUS_BLOCK) && !isEmpty(k)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------ collapsed border segments

    /**
     * Builds the segments of the collapsed borders (relative to the table box): the borders along every horizontal and vertical grid
     * line, with equal neighbours merged. A horizontal segment is made longer by half the width of the vertical borders at its ends,
     * so that the corners are filled; the vertical segments are painted after the horizontal ones.
     * <p>
     * 합쳐진 border 의 선분을 만든다(표 상자 기준): 모든 가로/세로 격자선을 따르는 border 이며 같은 이웃은 합친다. 가로 선분은 양 끝의 세로 border 너비의 절반만큼 길어져
     * 모서리를 채우고, 세로 선분은 가로 선분 뒤에 그린다.
     *
     * @param m    the model / 모델
     * @param colX x of the vertical grid lines / 세로 격자선의 x
     * @param rowY y of the horizontal grid lines (relative to the grid) / 가로 격자선의 y(격자 기준)
     * @param topH height above the grid / 격자 위 높이
     * @return the segments in painting order / 그리기 순서의 선분
     */
    private List<TableBorder> borderSegments(TableModel m, double[] colX, double[] rowY, double topH) {
        List<TableBorder> out = new ArrayList<TableBorder>();
        int nRows = m.nRows;
        int nCols = m.nCols;
        // 가로선: 같은 border 가 이어지는 열을 한 선분으로 합친다.
        for (int i = 0; i <= nRows; i++) {
            int j = 0;
            // 열을 왼쪽에서 오른쪽으로 훑으며 이어지는 같은 border 를 합친다.
            while (j < nCols) {
                TableModel.Edge e = m.hEdge[i][j];
                if (e == null || e.width <= 0) {
                    j++;
                    continue;
                }
                int k = j + 1;
                // 오른쪽으로 같은 border 가 이어지는 동안 선분을 늘린다.
                while (k < nCols && same(m.hEdge[i][k], e)) {
                    k++;
                }
                double x0 = colX[j] - cornerHalf(m, i, j);
                double x1 = colX[k] + cornerHalf(m, i, k);
                out.add(new TableBorder(x0, topH + rowY[i] - e.width / 2, x1 - x0, e.width, true, e.style, e.color));
                j = k;
            }
        }
        // 세로선: 같은 border 가 이어지는 행을 한 선분으로 합친다.
        for (int j = 0; j <= nCols; j++) {
            int i = 0;
            // 행을 위에서 아래로 훑으며 이어지는 같은 border 를 합친다.
            while (i < nRows) {
                TableModel.Edge e = m.vEdge[i][j];
                if (e == null || e.width <= 0) {
                    i++;
                    continue;
                }
                int k = i + 1;
                // 아래로 같은 border 가 이어지는 동안 선분을 늘린다.
                while (k < nRows && same(m.vEdge[k][j], e)) {
                    k++;
                }
                double y0 = rowY[i];
                double y1 = rowY[k];
                out.add(new TableBorder(colX[j] - e.width / 2, topH + y0, e.width, y1 - y0, false, e.style, e.color));
                i = k;
            }
        }
        return out;
    }

    /**
     * Tells whether two borders are the same (so that segments can be merged).
     * <p>
     * 두 border 가 같은지 알려 준다(선분을 합칠 수 있는지).
     *
     * @param a first / 첫째
     * @param b second / 둘째
     * @return true if equal / 같으면 true
     */
    private static boolean same(TableModel.Edge a, TableModel.Edge b) {
        return a != null && b != null && a.width == b.width && a.style.equals(b.style) && a.color == b.color;
    }

    /**
     * Returns half the width of the widest vertical border that ends at the corner of a horizontal grid line and a vertical one.
     * <p>
     * 가로 격자선과 세로 격자선의 모서리에서 끝나는 가장 넓은 세로 border 너비의 절반을 반환한다.
     *
     * @param m the model / 모델
     * @param i the horizontal line / 가로선
     * @param j the vertical line / 세로선
     * @return half the width / 너비의 절반
     */
    private static double cornerHalf(TableModel m, int i, int j) {
        double w = 0;
        if (i > 0 && m.vEdge[i - 1][j] != null) {
            w = Math.max(w, m.vEdge[i - 1][j].width);
        }
        if (i < m.nRows && m.vEdge[i][j] != null) {
            w = Math.max(w, m.vEdge[i][j].width);
        }
        return w / 2;
    }
}
