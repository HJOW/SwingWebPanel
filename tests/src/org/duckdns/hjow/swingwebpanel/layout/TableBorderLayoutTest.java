package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the collapsing border model of tables (plan item P10-02, CSS 2.1 17.6.2): the geometry that follows from half of every
 * collapsed border belonging to each side of a grid line, the resolution of conflicts between the borders of cells, rows, row groups,
 * columns and the table (<code>hidden</code> wins, then width, then style, then element, then position) and the segments that the
 * table hands to the renderer. The separated model is checked for the cell and table border widths that it keeps.
 * <p>
 * 표의 border 합치기 모델 시험(계획 P10-02, CSS 2.1 17.6.2): 합쳐진 모든 border 의 절반씩이 격자선 양쪽에 속해서 나오는 기하, 셀, 행, 행 그룹, 열, 표의 border 사이
 * 충돌 해결(<code>hidden</code> 이 이기고, 그다음 너비, 스타일, 요소, 위치), 표가 렌더러에 넘기는 선분. 분리 모델은 셀과 표의 border 너비가 유지되는지 확인한다.
 */
public class TableBorderLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TableBorderLayoutTest() {
    }

    /**
     * Returns the segments of a table that have a color.
     * <p>
     * 색이 있는 표의 선분을 반환한다.
     *
     * @param t     the table box / 표 상자
     * @param color the ARGB color / ARGB 색
     * @return the segments / 선분
     */
    private static List<TableBorder> withColor(LayoutBox t, int color) {
        List<TableBorder> out = new java.util.ArrayList<TableBorder>();
        // 선분 중 색이 같은 것을 모은다.
        for (TableBorder s : t.getCollapsedBorders()) {
            if (s.getColor() == color) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * Half of every collapsed border lies inside each of the two cells; the outer half of the outer borders belongs to the table.
     * <p>
     * 합쳐진 모든 border 의 절반은 양쪽 셀 안에 놓이고 바깥 border 의 바깥쪽 절반은 표에 속한다.
     */
    public void testGeometryOfCollapsedBorders() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-collapse:collapse} td{padding:0;border:2px solid #000;width:10px;height:10px}</style>"
                + "<table id=t><tr><td id=a></td><td id=b></td></tr><tr><td id=c></td><td id=d></td></tr></table>", 400);
        LayoutBox t = LayoutTestSupport.box(r, "t");
        Check.isTrue(t.isCollapsedBorderModel(), "model flag");
        Check.near(1, t.getBorderLeft(), T, "the table border is half of the outer collapsed border");
        Check.near(26, t.getWidth(), T, "1 + 12 + 12 + 1");
        Check.near(26, t.getHeight(), T, "1 + 12 + 12 + 1");
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(1, a.getBorderLeft(), T, "a left: half of 2");
        Check.near(1, a.getBorderRight(), T, "a right: half of the shared 2");
        Check.near(1, a.getX() - t.getX(), T, "a starts at the grid line (inside the table border)");
        Check.near(12, a.getWidth(), T, "a width = 10 + 1 + 1");
        Check.near(13, LayoutTestSupport.box(r, "b").getX() - t.getX(), T, "b x");
        Check.near(13, LayoutTestSupport.box(r, "c").getY() - t.getY(), T, "c y");
        List<TableBorder> all = t.getCollapsedBorders();
        Check.equal(6, all.size(), "three horizontal and three vertical lines, each merged into one segment");
        TableBorder middle = null;
        // 가운데 가로선(y 중심 13 - 1)을 찾는다.
        for (TableBorder s : all) {
            if (s.isHorizontal() && Math.abs(s.getY() - t.getY() - 12) < 0.1) {
                middle = s;
            }
        }
        Check.notNull(middle, "the middle horizontal segment");
        Check.near(2, middle.getHeight(), T, "thickness");
        Check.near(0, middle.getX() - t.getX(), T, "starts one border half left of the first grid line (the corner)");
        Check.near(26, middle.getWidth(), T, "spans the whole table including the corners");
        Check.equal("solid", middle.getStyle(), "style");
    }

    /**
     * The wider border wins; with equal widths the stronger style wins; <code>hidden</code> removes the border; with equal borders
     * the cell wins over the row, row group, column, column group and table, and the left or top border over the right or bottom one.
     * <p>
     * 더 넓은 border 가 이긴다. 너비가 같으면 더 강한 스타일이 이기고 <code>hidden</code> 은 border 를 없앤다. border 가 같으면 셀이 행, 행 그룹, 열, 열 그룹, 표를 이기고
     * 왼쪽이나 위쪽 border 가 오른쪽이나 아래쪽 border 를 이긴다.
     */
    public void testConflictResolution() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-collapse:collapse} td{padding:0;width:10px;height:10px}</style>"
                + "<table id=w><tr><td style='border:4px solid #f00'></td><td style='border:2px solid #00f'></td></tr></table>"
                + "<table id=s><tr><td style='border:3px solid #f00'></td><td style='border:3px double #00f'></td></tr></table>"
                + "<table id=h><tr><td style='border:3px solid #f00;border-right-style:hidden'></td><td style='border:3px solid #00f'></td></tr></table>"
                + "<table id=e style='border:2px solid #f00'><tr><td style='border:2px solid #00f'></td></tr></table>"
                + "<table id=p><tr><td style='border:2px solid #f00'></td><td style='border:2px solid #00f'></td></tr></table>"
                + "<table id=row><tr style='border:2px solid #f00'><td style='border:2px solid #00f'></td></tr></table>", 400);
        // 너비: 가운데 세로선에는 4px 빨강 하나뿐이다(오른쪽 셀의 2px 파랑은 졌다).
        LayoutBox w = LayoutTestSupport.box(r, "w");
        Check.isTrue(withColor(w, 0xFFFF0000).size() >= 1, "red segments exist");
        boolean wideRedInTheMiddle = false;
        // 빨강 선분 중 두께 4 의 세로 선분이 있는지 본다.
        for (TableBorder s : withColor(w, 0xFFFF0000)) {
            if (!s.isHorizontal() && Math.abs(s.getThickness() - 4) < T) {
                wideRedInTheMiddle = true;
            }
        }
        Check.isTrue(wideRedInTheMiddle, "the 4px red border wins the shared edge");
        Check.isTrue(withColor(w, 0xFF0000FF).size() >= 1, "the blue cell keeps its outer borders (top, right and bottom)");
        // 스타일: 같은 너비에서 double 이 solid 를 이긴다.
        LayoutBox s = LayoutTestSupport.box(r, "s");
        boolean doubleWins = false;
        // 가운데 세로선(두 셀 사이)의 스타일을 본다.
        for (TableBorder b : s.getCollapsedBorders()) {
            if (!b.isHorizontal() && b.getStyle().equals("double") && Math.abs(b.getX() + b.getWidth() / 2 - (s.getX() + 1.5 + 13)) < 1.0) {
                doubleWins = true;
            }
        }
        Check.isTrue(doubleWins, "double beats solid at equal width");
        // hidden: 오른쪽 hidden 이 가운데 세로선을 없앤다.
        LayoutBox h = LayoutTestSupport.box(r, "h");
        int middleLines = 0;
        // 세로 선분 중 안쪽(가장자리가 아닌) 것을 센다.
        for (TableBorder b : h.getCollapsedBorders()) {
            if (!b.isHorizontal() && b.getX() > h.getX() + 4 && b.getX() < h.getX() + h.getWidth() - 4) {
                middleLines++;
            }
        }
        Check.equal(0, middleLines, "hidden removes the shared border");
        // 요소: 표 border 와 셀 border 가 같으면 셀(파랑)이 이긴다.
        LayoutBox e = LayoutTestSupport.box(r, "e");
        Check.equal(0, withColor(e, 0xFFFF0000).size(), "the cell beats the table");
        Check.isTrue(withColor(e, 0xFF0000FF).size() >= 4, "blue on all four sides");
        // 위치: 같은 border 는 왼쪽(빨강)이 이긴다.
        LayoutBox p = LayoutTestSupport.box(r, "p");
        boolean leftWins = false;
        // 가운데 세로선의 색을 본다.
        for (TableBorder b : p.getCollapsedBorders()) {
            if (!b.isHorizontal() && b.getX() > p.getX() + 3 && b.getX() < p.getX() + p.getWidth() - 3) {
                leftWins = b.getColor() == 0xFFFF0000;
            }
        }
        Check.isTrue(leftWins, "the left cell wins a tie");
        // 행: 행 border(빨강)와 셀 border(파랑)가 같으면 셀이 이긴다.
        LayoutBox row = LayoutTestSupport.box(r, "row");
        Check.equal(0, withColor(row, 0xFFFF0000).size(), "the cell beats the row");
    }

    /**
     * The separated model keeps the own border widths of table and cells and adds nothing to the table.
     * <p>
     * 분리 모델은 표와 셀의 자기 border 너비를 유지하며 표 border 모델 표시를 하지 않는다.
     */
    public void testSeparatedModelKeepsBorders() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-spacing:2px;border:3px solid #000} td{padding:1px;border:2px solid #f00;width:10px;height:10px}</style>"
                + "<table id=t><tr><td id=a></td></tr></table>", 400);
        LayoutBox t = LayoutTestSupport.box(r, "t");
        Check.isFalse(t.isCollapsedBorderModel(), "separated");
        Check.near(3, t.getBorderLeft(), T, "table border");
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(2, a.getBorderLeft(), T, "cell border");
        Check.near(1, a.getPaddingLeft(), T, "cell padding");
        Check.near(26, t.getWidth(), T, "3 + spacing 2 + cell (10 + padding 2 + border 4 = 16) + spacing 2 + 3");
        Check.equal(0, t.getCollapsedBorders().size(), "no collapsed segments");
    }

    /**
     * Borders of column groups and columns take part in the conflict and a row group border wins over a column border.
     * <p>
     * 열 그룹과 열의 border 도 충돌에 참여하고 행 그룹 border 가 열 border 를 이긴다.
     */
    public void testColumnAndGroupBorders() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-collapse:collapse} td{padding:0;width:10px;height:10px}</style>"
                + "<table id=t><colgroup><col style='border-left:3px solid #f00'><col></colgroup><tbody style='border-top:3px solid #00f'><tr><td></td><td></td></tr></tbody></table>", 400);
        LayoutBox t = LayoutTestSupport.box(r, "t");
        Check.near(1.5, t.getBorderLeft(), T, "the column border applies to the left edge of the table");
        Check.near(1.5, t.getBorderTop(), T, "the row group border applies to the top edge");
        Check.isTrue(withColor(t, 0xFFFF0000).size() >= 1, "red column border");
        Check.isTrue(withColor(t, 0xFF0000FF).size() >= 1, "blue row group border");
    }

    /**
     * A cell that spans columns and rows has no border inside itself: the grid lines it covers are not painted.
     * <p>
     * 열과 행에 걸친 셀은 자기 안쪽에 border 가 없다: 그것이 덮는 격자선은 그리지 않는다.
     */
    public void testSpannedCellHasNoInnerBorders() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-collapse:collapse} td{padding:0;border:2px solid #000;width:10px;height:10px}</style>"
                + "<table id=t><tr><td id=a colspan=2 rowspan=2></td><td></td></tr><tr><td></td></tr><tr><td></td><td></td><td></td></tr></table>", 400);
        LayoutBox t = LayoutTestSupport.box(r, "t");
        int vertical = 0;
        // 위쪽 두 행 높이 안에서 가운데 세로선(첫 열과 둘째 열 사이)이 있는지 센다.
        for (TableBorder s : t.getCollapsedBorders()) {
            double cx = s.getX() + s.getWidth() / 2 - t.getX();
            if (!s.isHorizontal() && Math.abs(cx - 13) < 1 && s.getY() - t.getY() < 20) {
                vertical++;
            }
        }
        Check.equal(0, vertical, "no vertical line inside the spanning cell");
        Check.equal(2, LayoutTestSupport.box(r, "a").getColumnSpan(), "column span");
        Check.equal(2, LayoutTestSupport.box(r, "a").getRowSpan(), "row span");
    }

    /**
     * Collapsed borders of a table with captions are placed below the top caption.
     * <p>
     * 합쳐진 border 는 위쪽 caption 아래에 놓인다.
     */
    public void testCollapsedBordersBelowCaption() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-collapse:collapse} td{padding:0;border:2px solid #000;width:10px;height:10px} caption{height:6px}</style>"
                + "<table id=t><caption>c</caption><tr><td></td></tr></table>", 400);
        LayoutBox t = LayoutTestSupport.box(r, "t");
        double topMost = Double.MAX_VALUE;
        // 가장 위 선분의 위쪽을 찾는다.
        for (TableBorder s : t.getCollapsedBorders()) {
            topMost = Math.min(topMost, s.getY() - t.getY());
        }
        Check.near(6, topMost, T, "the top border starts below the caption");
    }
}
