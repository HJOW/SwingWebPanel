package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The grid of a table (plan item P10-02): which cell covers which rows and columns (CSS 2.1 17.5 and the HTML table model: the cells of
 * a row take the next free column, <code>colspan</code> and <code>rowspan</code> occupy several slots, a row span ends at the end of its
 * row group), the columns that <code>col</code> and <code>colgroup</code> define, the border spacing and, for
 * <code>border-collapse:collapse</code>, the resolution of the conflicts between all borders that meet at a grid line (CSS 2.1
 * 17.6.2.1: <code>hidden</code> wins, then the widest border, then the style in the order double, solid, dashed, dotted, ridge, outset,
 * groove, inset, then the element in the order cell, row, row group, column, column group, table, then the leftmost and topmost). The
 * model depends only on the box tree and the styles, never on widths, so it is built once per table and cached in the box. At most
 * {@link #MAX_COLUMNS} columns and {@link #MAX_SLOTS} grid slots are kept; cells beyond are not laid out.
 * <p>
 * 표의 격자(계획 P10-02): 어느 셀이 어느 행과 열을 차지하는지(CSS 2.1 17.5 와 HTML 표 모델: 행의 셀은 다음 빈 열을 차지하고, <code>colspan</code> 과
 * <code>rowspan</code> 은 여러 칸을 차지하며, 행 범위는 행 그룹의 끝에서 끝난다), <code>col</code> 과 <code>colgroup</code> 이 정의하는 열, border 간격,
 * <code>border-collapse:collapse</code> 에서는 격자선에서 만나는 모든 border 의 충돌 해결(CSS 2.1 17.6.2.1: <code>hidden</code> 이 이기고, 그다음 가장 넓은
 * border, 그다음 double, solid, dashed, dotted, ridge, outset, groove, inset 순서의 스타일, 그다음 셀, 행, 행 그룹, 열, 열 그룹, 표 순서의 요소, 그다음 가장 왼쪽과
 * 위쪽). 모델은 상자 트리와 스타일에만 의존하고 너비에는 의존하지 않으므로 표마다 한 번 만들어 상자에 보관한다. 열은 최대 {@link #MAX_COLUMNS} 개, 격자 칸은 최대
 * {@link #MAX_SLOTS} 개만 다루며 그 밖의 셀은 배치하지 않는다.
 */
final class TableModel {

    /** Largest number of columns. 열의 최대 개수. */
    static final int MAX_COLUMNS = 1000;
    /** Largest number of grid slots (rows times columns). 격자 칸(행 × 열)의 최대 개수. */
    static final int MAX_SLOTS = 2000000;

    /** Index of the top side in the arrays of four sides. 네 변 배열에서 위쪽 변의 번호. */
    static final int TOP = 0;
    /** Index of the right side. 오른쪽 변의 번호. */
    static final int RIGHT = 1;
    /** Index of the bottom side. 아래쪽 변의 번호. */
    static final int BOTTOM = 2;
    /** Index of the left side. 왼쪽 변의 번호. */
    static final int LEFT = 3;

    /** Border width properties by side. 변별 border 너비 속성. */
    private static final int[] WIDTH_PROP = { Prop.BORDER_TOP_WIDTH, Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_LEFT_WIDTH };
    /** Border style properties by side. 변별 border 스타일 속성. */
    private static final int[] STYLE_PROP = { Prop.BORDER_TOP_STYLE, Prop.BORDER_RIGHT_STYLE, Prop.BORDER_BOTTOM_STYLE, Prop.BORDER_LEFT_STYLE };
    /** Border color properties by side. 변별 border 색 속성. */
    private static final int[] COLOR_PROP = { Prop.BORDER_TOP_COLOR, Prop.BORDER_RIGHT_COLOR, Prop.BORDER_BOTTOM_COLOR, Prop.BORDER_LEFT_COLOR };
    /** Border styles from the strongest to the weakest. 가장 강한 것부터 약한 것까지의 border 스타일. */
    private static final String[] STYLE_ORDER = { "double", "solid", "dashed", "dotted", "ridge", "outset", "groove", "inset" };

    /**
     * A cell and the grid slots it occupies.
     * <p>
     * 셀과 그것이 차지하는 격자 칸.
     */
    static final class Cell {
        /** The cell box. 셀 상자. */
        final LayoutBox box;
        /** First row. 첫 행. */
        int row;
        /** First column. 첫 열. */
        int col;
        /** Number of rows. 행 수. */
        int rowSpan;
        /** Number of columns. 열 수. */
        int colSpan;

        /**
         * Creates a cell record.
         * <p>
         * 셀 기록을 만든다.
         *
         * @param box the cell box / 셀 상자
         */
        Cell(LayoutBox box) {
            this.box = box;
        }
    }

    /**
     * One collapsed border (the border of one element on one side, or the winner of a conflict).
     * <p>
     * 합쳐지는 border 하나(한 요소의 한 변 border, 또는 충돌의 승자).
     */
    static final class Edge {
        /** Width in pixels. 픽셀 너비. */
        final double width;
        /** The border style keyword. border 스타일 키워드. */
        final String style;
        /** The color as ARGB. ARGB 형식의 색 값. */
        final int color;
        /** The element rank: 0 cell, 1 row, 2 row group, 3 column, 4 column group, 5 table. 요소 순위: 0 셀, 1 행, 2 행 그룹, 3 열, 4 열 그룹, 5 표. */
        final int rank;
        /** True for <code>border-style:hidden</code>. <code>border-style:hidden</code> 이면 true. */
        final boolean hidden;

        /**
         * Creates an edge.
         * <p>
         * 가장자리를 만든다.
         *
         * @param width  width / 너비
         * @param style  style / 스타일
         * @param color  color / 색
         * @param rank   element rank / 요소 순위
         * @param hidden true for hidden / hidden 이면 true
         */
        Edge(double width, String style, int color, int rank, boolean hidden) {
            this.width = width;
            this.style = style;
            this.color = color;
            this.rank = rank;
            this.hidden = hidden;
        }
    }

    /** The table box. 표 상자. */
    final LayoutBox table;
    /** True for <code>border-collapse:collapse</code>. <code>border-collapse:collapse</code> 이면 true. */
    final boolean collapse;
    /** Horizontal border spacing (0 when collapsed). 가로 border 간격(합칠 때는 0). */
    final double hs;
    /** Vertical border spacing (0 when collapsed). 세로 border 간격(합칠 때는 0). */
    final double vs;
    /** The row boxes in display order. 표시 순서의 행 상자. */
    final List<LayoutBox> rows = new ArrayList<LayoutBox>();
    /** The row group box of each row. 행마다 그 행 그룹 상자. */
    final List<LayoutBox> groupOfRow = new ArrayList<LayoutBox>();
    /** All cells in grid order. 격자 순서의 모든 셀. */
    final List<Cell> cells = new ArrayList<Cell>();
    /** Number of rows. 행 수. */
    int nRows;
    /** Number of columns. 열 수. */
    int nCols;
    /** The cell that covers a grid slot (null: none), indexed [row][column]. 격자 칸을 덮는 셀(null: 없음). [행][열] 순. */
    Cell[][] slot;
    /** The column box (or column group box without columns) of every grid column, null if none was defined. 격자 열마다 열 상자(열이 없는 열 그룹 상자). 정의가 없으면 null. */
    LayoutBox[] colBox;
    /** The column group box of every grid column, null if none. 격자 열마다 열 그룹 상자. 없으면 null. */
    LayoutBox[] colGroupBox;
    /** Resolved horizontal borders [line 0..nRows][column] (null: none), only for the collapsed model. 합쳐진 가로 border [선 0..nRows][열]. 합치기 모델에서만. */
    Edge[][] hEdge;
    /** Resolved vertical borders [row][line 0..nCols] (null: none), only for the collapsed model. 합쳐진 세로 border [행][선 0..nCols]. 합치기 모델에서만. */
    Edge[][] vEdge;
    /** True if the grid was limited because it was too big. 격자가 너무 커서 제한되었으면 true. */
    boolean truncated;

    /**
     * Builds the model of a table box.
     * <p>
     * 표 상자의 모델을 만든다.
     *
     * @param table the table box / 표 상자
     */
    TableModel(LayoutBox table) {
        this.table = table;
        ComputedStyle s = table.style;
        this.collapse = "collapse".equals(Css.kw(s, Prop.BORDER_COLLAPSE));
        double h = 0;
        double v = 0;
        if (!collapse) {
            CssValue sp = s.get(Prop.BORDER_SPACING);
            if (sp instanceof CssList && ((CssList) sp).getItems().size() >= 2) {
                h = lengthOf(((CssList) sp).getItems().get(0));
                v = lengthOf(((CssList) sp).getItems().get(1));
            } else {
                h = lengthOf(sp);
                v = h;
            }
        }
        this.hs = h;
        this.vs = v;
        buildGrid();
        if (collapse) {
            resolveBorders();
        }
    }

    /**
     * Reads a length value as pixels (0 for anything else).
     * <p>
     * 길이 값을 픽셀로 읽는다(그 밖은 0).
     *
     * @param v the value / 값
     * @return pixels, not negative / 픽셀. 음수 아님
     */
    private static double lengthOf(CssValue v) {
        return v instanceof CssLength ? Math.max(0, ((CssLength) v).getValue()) : 0;
    }

    /**
     * Places the cells in the grid.
     * <p>
     * 셀을 격자에 놓는다.
     */
    private void buildGrid() {
        List<BitSet> occupied = new ArrayList<BitSet>();
        List<Integer> remaining = new ArrayList<Integer>();
        // 행 그룹마다 행을 모으고 그룹 끝까지 남은 행 수를 기록한다.
        for (int i = 0; i < table.children.size(); i++) {
            LayoutBox g = table.children.get(i);
            if (g.type != LayoutBox.Type.TABLE_ROW_GROUP) {
                continue;
            }
            int first = rows.size();
            // 그룹의 행을 차례로 모은다.
            for (int k = 0; k < g.children.size(); k++) {
                LayoutBox r = g.children.get(k);
                if (r.type == LayoutBox.Type.TABLE_ROW) {
                    rows.add(r);
                    groupOfRow.add(g);
                }
            }
            // 그룹 안에서 각 행 뒤에 남은 행 수(자기 포함)를 기록한다.
            for (int k = first; k < rows.size(); k++) {
                remaining.add(Integer.valueOf(rows.size() - k));
            }
        }
        nRows = rows.size();
        int maxCol = 0;
        // 행을 차례로 보며 셀을 다음 빈 열에 놓는다(앞 행의 rowspan 이 차지한 칸은 건너뛴다).
        for (int r = 0; r < nRows; r++) {
            while (occupied.size() <= r) {
                occupied.add(new BitSet());
            }
            int col = 0;
            LayoutBox row = rows.get(r);
            row.tableRow = r;
            // 행의 셀을 차례로 놓는다.
            for (int k = 0; k < row.children.size(); k++) {
                LayoutBox cb = row.children.get(k);
                if (cb.type != LayoutBox.Type.TABLE_CELL) {
                    continue;
                }
                // 이미 차지된 칸은 건너뛴다.
                while (occupied.get(r).get(col) && col < MAX_COLUMNS) {
                    col++;
                }
                if (col >= MAX_COLUMNS) {
                    truncated = true;
                    col = MAX_COLUMNS - 1;
                }
                int cs = Math.max(1, Math.min(cb.colSpan, MAX_COLUMNS - col));
                int left = remaining.get(r).intValue();
                int rs = cb.rowSpan == 0 ? left : Math.min(cb.rowSpan, left);
                Cell c = new Cell(cb);
                c.row = r;
                c.col = col;
                c.colSpan = cs;
                c.rowSpan = rs;
                // 차지하는 모든 행의 비트를 켠다.
                for (int rr = r; rr < r + rs; rr++) {
                    while (occupied.size() <= rr) {
                        occupied.add(new BitSet());
                    }
                    occupied.get(rr).set(col, col + cs);
                }
                cb.tableRow = r;
                cb.tableCol = col;
                cb.rowSpan = rs;
                cb.colSpan = cs;
                cells.add(c);
                maxCol = Math.max(maxCol, col + cs);
                col += cs;
            }
        }
        nCols = maxCol;
        colBox = new LayoutBox[Math.max(nCols, 0)];
        colGroupBox = new LayoutBox[Math.max(nCols, 0)];
        List<LayoutBox> defs = new ArrayList<LayoutBox>();
        List<LayoutBox> defGroups = new ArrayList<LayoutBox>();
        // 열과 열 그룹의 정의를 격자 열 순서로 펼친다.
        for (int i = 0; i < table.children.size(); i++) {
            LayoutBox c = table.children.get(i);
            if (c.type == LayoutBox.Type.TABLE_COLUMN) {
                // span 이 나타내는 수만큼 격자 열을 이 열 상자에 연결한다.
                for (int k = 0; k < c.colSpan && defs.size() < MAX_COLUMNS; k++) {
                    defs.add(c);
                    defGroups.add(null);
                }
            } else if (c.type == LayoutBox.Type.TABLE_COLUMN_GROUP) {
                if (c.colSpan == 0) {
                    // 열이 있는 그룹은 열들이 격자 열을 정의한다.
                    for (int k = 0; k < c.children.size(); k++) {
                        LayoutBox col = c.children.get(k);
                        // 열 그룹 안 열의 span 만큼 격자 열을 연결한다.
                        for (int m = 0; m < col.colSpan && defs.size() < MAX_COLUMNS; m++) {
                            defs.add(col);
                            defGroups.add(c);
                        }
                    }
                } else {
                    // 열이 없는 그룹은 자신의 span 만큼 격자 열을 차지한다.
                    for (int k = 0; k < c.colSpan && defs.size() < MAX_COLUMNS; k++) {
                        defs.add(c);
                        defGroups.add(c);
                    }
                }
            }
        }
        nCols = Math.max(nCols, defs.size());
        if ((long) nRows * nCols > MAX_SLOTS) {
            truncated = true;
            nRows = Math.max(0, MAX_SLOTS / Math.max(1, nCols));
        }
        colBox = new LayoutBox[nCols];
        colGroupBox = new LayoutBox[nCols];
        // 정의된 열 상자를 격자 열에 연결하고 첫 열 번호를 적는다.
        for (int j = 0; j < defs.size(); j++) {
            colBox[j] = defs.get(j);
            colGroupBox[j] = defGroups.get(j);
            if (j == 0 || defs.get(j) != defs.get(j - 1)) {
                defs.get(j).tableCol = j;
            }
        }
        slot = new Cell[nRows][nCols];
        // 셀이 차지하는 칸에 셀을 적는다(나머지 행은 제한으로 잘린 것).
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            // 셀이 차지하는 행을 훑는다.
            for (int rr = c.row; rr < Math.min(nRows, c.row + c.rowSpan); rr++) {
                // 셀이 차지하는 열을 훑으며 칸에 셀을 적는다.
                for (int cc = c.col; cc < Math.min(nCols, c.col + c.colSpan); cc++) {
                    slot[rr][cc] = c;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------ collapsed borders

    /**
     * Returns the border of an element on a side as a candidate of the conflict resolution, or null if it has none.
     * <p>
     * 요소의 한 변 border 를 충돌 해결의 후보로 반환한다. 없으면 null.
     *
     * @param b    the box (anonymous boxes have no borders) / 상자(익명 상자는 border 가 없다)
     * @param side the side / 변
     * @param rank the element rank / 요소 순위
     * @return the candidate or null / 후보 또는 null
     */
    private static Edge candidate(LayoutBox b, int side, int rank) {
        if (b == null || b.node == null || b.style == null) {
            return null;
        }
        String st = Css.kw(b.style, STYLE_PROP[side]);
        if ("hidden".equals(st)) {
            return new Edge(0, st, 0, rank, true);
        }
        if (st.length() == 0 || "none".equals(st)) {
            return null;
        }
        double w = b.style.px(WIDTH_PROP[side]);
        if (Double.isNaN(w) || w <= 0) {
            return null;
        }
        return new Edge(w, st, b.style.color(COLOR_PROP[side]), rank, false);
    }

    /**
     * Returns the rank of a border style (smaller wins).
     * <p>
     * border 스타일의 순위를 반환한다(작을수록 이긴다).
     *
     * @param style the style keyword / 스타일 키워드
     * @return the rank / 순위
     */
    private static int styleRank(String style) {
        // 스타일 순서 표에서 위치를 찾는다.
        for (int i = 0; i < STYLE_ORDER.length; i++) {
            if (STYLE_ORDER[i].equals(style)) {
                return i;
            }
        }
        return STYLE_ORDER.length;
    }

    /**
     * Resolves the conflict between candidates (CSS 2.1 17.6.2.1): a hidden border removes the border; else the widest wins, then the
     * strongest style, then the element rank, then the first candidate (the left or top one).
     * <p>
     * 후보 사이의 충돌을 해결한다(CSS 2.1 17.6.2.1): hidden 이 있으면 border 가 없고, 아니면 가장 넓은 것, 그다음 가장 강한 스타일, 그다음 요소 순위, 그다음 첫 후보(왼쪽이나
     * 위쪽)가 이긴다.
     *
     * @param list the candidates (nulls are ignored) / 후보(null 은 무시)
     * @return the winner or null if there is no border / 승자. border 가 없으면 null
     */
    private static Edge resolve(Edge[] list) {
        Edge best = null;
        // 후보를 차례로 보며 hidden 이면 바로 없애고 아니면 더 나은 것으로 바꾼다.
        for (int i = 0; i < list.length; i++) {
            Edge e = list[i];
            if (e == null) {
                continue;
            }
            if (e.hidden) {
                return null;
            }
            if (best == null || e.width > best.width || (e.width == best.width && (styleRank(e.style) < styleRank(best.style)
                    || (styleRank(e.style) == styleRank(best.style) && e.rank < best.rank)))) {
                best = e;
            }
        }
        return best;
    }

    /**
     * Resolves all horizontal and vertical grid line borders of the collapsed model.
     * <p>
     * 합치기 모델의 모든 가로/세로 격자선 border 를 해결한다.
     */
    private void resolveBorders() {
        hEdge = new Edge[nRows + 1][nCols];
        vEdge = new Edge[nRows][nCols + 1];
        // 가로선: 위 셀의 아래 border, 아래 셀의 위 border 와 행, 행 그룹, 열, 열 그룹, 표의 border 를 합친다.
        for (int i = 0; i <= nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                Cell above = i > 0 ? slot[i - 1][j] : null;
                Cell below = i < nRows ? slot[i][j] : null;
                if (above != null && above == below) {
                    continue;
                }
                LayoutBox rowAbove = i > 0 ? rows.get(i - 1) : null;
                LayoutBox rowBelow = i < nRows ? rows.get(i) : null;
                LayoutBox groupAbove = i > 0 && (i == nRows || groupOfRow.get(i - 1) != groupOfRow.get(i)) ? groupOfRow.get(i - 1) : null;
                LayoutBox groupBelow = i < nRows && (i == 0 || groupOfRow.get(i) != groupOfRow.get(i - 1)) ? groupOfRow.get(i) : null;
                hEdge[i][j] = resolve(new Edge[] { candidate(above == null ? null : above.box, BOTTOM, 0),
                    candidate(below == null ? null : below.box, TOP, 0), candidate(rowAbove, BOTTOM, 1), candidate(rowBelow, TOP, 1),
                    candidate(groupAbove, BOTTOM, 2), candidate(groupBelow, TOP, 2),
                    i == 0 ? candidate(colBox[j], TOP, 3) : (i == nRows ? candidate(colBox[j], BOTTOM, 3) : null),
                    i == 0 ? candidate(colGroupBox[j], TOP, 4) : (i == nRows ? candidate(colGroupBox[j], BOTTOM, 4) : null),
                    i == 0 ? candidate(table, TOP, 5) : (i == nRows ? candidate(table, BOTTOM, 5) : null) });
            }
        }
        // 세로선: 왼쪽 셀의 오른쪽 border, 오른쪽 셀의 왼쪽 border 와 행, 행 그룹, 열, 열 그룹, 표의 border 를 합친다.
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j <= nCols; j++) {
                Cell left = j > 0 ? slot[i][j - 1] : null;
                Cell right = j < nCols ? slot[i][j] : null;
                if (left != null && left == right) {
                    continue;
                }
                LayoutBox colLeft = j > 0 ? colBox[j - 1] : null;
                LayoutBox colRight = j < nCols ? colBox[j] : null;
                LayoutBox grpLeft = j > 0 && (j == nCols || colGroupBox[j - 1] != colGroupBox[j]) ? colGroupBox[j - 1] : null;
                LayoutBox grpRight = j < nCols && (j == 0 || colGroupBox[j] != colGroupBox[j - 1]) ? colGroupBox[j] : null;
                vEdge[i][j] = resolve(new Edge[] { candidate(left == null ? null : left.box, RIGHT, 0),
                    candidate(right == null ? null : right.box, LEFT, 0),
                    j == 0 ? candidate(rows.get(i), LEFT, 1) : (j == nCols ? candidate(rows.get(i), RIGHT, 1) : null),
                    j == 0 ? candidate(groupOfRow.get(i), LEFT, 2) : (j == nCols ? candidate(groupOfRow.get(i), RIGHT, 2) : null),
                    candidate(colLeft, RIGHT, 3), candidate(colRight, LEFT, 3), candidate(grpLeft, RIGHT, 4), candidate(grpRight, LEFT, 4),
                    j == 0 ? candidate(table, LEFT, 5) : (j == nCols ? candidate(table, RIGHT, 5) : null) });
            }
        }
    }

    /**
     * Returns half the width of a resolved border (0 for none).
     * <p>
     * 합쳐진 border 너비의 절반을 반환한다(없으면 0).
     *
     * @param e the border or null / border 또는 null
     * @return half the width / 너비의 절반
     */
    private static double half(Edge e) {
        return e == null ? 0 : e.width / 2;
    }

    /**
     * Returns the used border widths of a cell in the collapsed model: half of the widest resolved border along each of its sides.
     * <p>
     * 합치기 모델에서 셀의 사용 border 너비를 반환한다: 각 변을 따르는 합쳐진 border 중 가장 넓은 것의 절반.
     *
     * @param c the cell / 셀
     * @return {top, right, bottom, left} / {위, 오른쪽, 아래, 왼쪽}
     */
    double[] cellHalves(Cell c) {
        double t = 0;
        double b = 0;
        double l = 0;
        double r = 0;
        int rEnd = Math.min(nRows, c.row + c.rowSpan);
        int cEnd = Math.min(nCols, c.col + c.colSpan);
        // 위/아래 변을 따라 가로선의 너비를 훑는다.
        for (int j = c.col; j < cEnd; j++) {
            t = Math.max(t, half(hEdge[c.row][j]));
            b = Math.max(b, half(hEdge[rEnd][j]));
        }
        // 왼쪽/오른쪽 변을 따라 세로선의 너비를 훑는다.
        for (int i = c.row; i < rEnd; i++) {
            l = Math.max(l, half(vEdge[i][c.col]));
            r = Math.max(r, half(vEdge[i][cEnd]));
        }
        return new double[] { t, r, b, l };
    }

    /**
     * Returns the used border widths of the table box in the collapsed model: half of the widest resolved border along each outer side.
     * <p>
     * 합치기 모델에서 표 상자의 사용 border 너비를 반환한다: 바깥 각 변을 따르는 합쳐진 border 중 가장 넓은 것의 절반.
     *
     * @return {top, right, bottom, left} / {위, 오른쪽, 아래, 왼쪽}
     */
    double[] outerHalves() {
        double t = 0;
        double b = 0;
        double l = 0;
        double r = 0;
        // 위/아래 바깥 변을 훑는다.
        for (int j = 0; j < nCols; j++) {
            t = Math.max(t, half(hEdge[0][j]));
            b = Math.max(b, half(hEdge[nRows][j]));
        }
        // 왼쪽/오른쪽 바깥 변을 훑는다.
        for (int i = 0; i < nRows; i++) {
            l = Math.max(l, half(vEdge[i][0]));
            r = Math.max(r, half(vEdge[i][nCols]));
        }
        return new double[] { t, r, b, l };
    }
}
