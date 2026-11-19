package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of table layout (plan item P10-02) against the rules of CSS 2.1 chapter 17: the grid with <code>colspan</code> and
 * <code>rowspan</code>, the automatic and the fixed table layout, percentage and specified widths, border spacing, row heights and
 * vertical alignment, captions, anonymous table boxes, header and footer groups, <code>col</code> widths, the HTML presentational
 * attributes and the collapsing border model (conflict resolution and the geometry of collapsed borders). The expected positions are
 * computed by hand from the CSS; the sheet of the tests removes padding and spacing where they would only obscure the arithmetic.
 * <p>
 * CSS 2.1 17장의 규칙에 따른 표 레이아웃 시험(계획 P10-02): <code>colspan</code>, <code>rowspan</code> 이 있는 격자, 자동/고정 표 레이아웃, 백분율/지정 너비, border
 * 간격, 행 높이와 세로 정렬, caption, 익명 표 상자, 헤더/푸터 그룹, <code>col</code> 너비, HTML 표현 속성, 합치기 border 모델(충돌 해결과 합쳐진 border 의 기하).
 * 기대 위치는 CSS 로 손으로 계산했으며 시험 시트는 계산을 가리는 padding 과 간격을 없앤다.
 */
public class TableLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /** Sheet that removes spacing and padding. 간격과 padding 을 없애는 시트. */
    private static final String PLAIN = "<style>table{border-spacing:0;border-collapse:separate} td,th{padding:0}</style>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TableLayoutTest() {
    }

    /**
     * Returns the box of an element by id.
     * <p>
     * id 로 요소의 상자를 반환한다.
     *
     * @param r  the result / 결과
     * @param id the id / id
     * @return the box / 상자
     */
    private static LayoutBox b(LayoutResult r, String id) {
        return LayoutTestSupport.box(r, id);
    }

    /**
     * Cells with specified widths and heights form a grid: columns take the widest cell, rows the tallest, and an automatic table is as
     * wide as its columns.
     * <p>
     * 너비와 높이가 지정된 셀이 격자를 이룬다: 열은 가장 넓은 셀, 행은 가장 큰 셀을 따르고 자동 너비 표는 열의 합만큼 넓다.
     */
    public void testBasicGrid() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t><tr id=r1><td id=a style='width:50px;height:20px'></td><td id=b style='width:70px'></td></tr>"
                + "<tr id=r2><td id=c style='height:30px'></td><td id=d style='width:40px'></td></tr></table><div id=after style='height:1px'></div>", 400);
        LayoutBox t = b(r, "t");
        Check.near(120, t.getWidth(), T, "the table is as wide as its columns (50 + 70)");
        Check.near(50, t.getHeight(), T, "20 + 30");
        Check.equal(LayoutBox.Type.TABLE, t.getType(), "table box type");
        Check.near(0, b(r, "a").getX(), T, "a x");
        Check.near(0, b(r, "a").getY(), T, "a y");
        Check.near(50, b(r, "a").getWidth(), T, "a width");
        Check.near(20, b(r, "a").getHeight(), T, "a height");
        Check.near(50, b(r, "b").getX(), T, "b x");
        Check.near(70, b(r, "b").getWidth(), T, "b width: the widest of b and d");
        Check.near(0, b(r, "c").getX(), T, "c x");
        Check.near(20, b(r, "c").getY(), T, "c y");
        Check.near(30, b(r, "c").getHeight(), T, "c height");
        Check.near(50, b(r, "c").getWidth(), T, "c width is the column width");
        Check.near(70, b(r, "d").getWidth(), T, "d is as wide as its column");
        Check.near(20, b(r, "d").getHeight() - 10, T, "cells of a row are as tall as the row (d: 30)");
        Check.near(0, b(r, "r1").getX(), T, "row x");
        Check.near(120, b(r, "r1").getWidth(), T, "row width");
        Check.near(20, b(r, "r1").getHeight(), T, "row 1 height");
        Check.near(50, b(r, "after").getY(), T, "the next block follows the table");
        Check.equal(0, b(r, "a").getRowSpan() - 1, "row span 1");
    }

    /**
     * Border spacing is added around and between the cells, horizontally and vertically (separated borders model).
     * <p>
     * border 간격은 셀 둘레와 사이에 가로와 세로로 더해진다(분리 border 모델).
     */
    public void testBorderSpacing() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t style='border-spacing:5px 3px'><tr><td id=a style='width:50px;height:20px'></td>"
                + "<td id=b style='width:70px'></td></tr><tr><td id=c style='height:30px'></td></tr></table>", 400);
        Check.near(5 + 50 + 5 + 70 + 5, b(r, "t").getWidth(), T, "width with three horizontal spaces");
        Check.near(3 + 20 + 3 + 30 + 3, b(r, "t").getHeight(), T, "height with three vertical spaces");
        Check.near(5, b(r, "a").getX(), T, "a x");
        Check.near(3, b(r, "a").getY(), T, "a y");
        Check.near(60, b(r, "b").getX(), T, "b x = 5 + 50 + 5");
        Check.near(26, b(r, "c").getY(), T, "c y = 3 + 20 + 3");
    }

    /**
     * A cell that spans columns widens the columns it covers if they are too narrow together, and a cell that spans rows grows the
     * rows it covers.
     * <p>
     * 열에 걸친 셀은 걸친 열의 합이 모자라면 그 열들을 넓히고 행에 걸친 셀은 걸친 행을 키운다.
     */
    public void testColspanAndRowspan() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t><tr><td id=h colspan=2 style='width:150px;height:10px'></td></tr>"
                + "<tr><td id=a style='width:40px;height:10px'></td><td id=b rowspan=2 style='width:40px;height:50px'></td></tr>"
                + "<tr><td id=c style='width:40px;height:10px'></td></tr></table>", 400);
        Check.near(150, b(r, "t").getWidth(), T, "the spanning cell decides the table width");
        Check.near(150, b(r, "h").getWidth(), T, "header width");
        Check.near(75, b(r, "a").getWidth(), T, "the missing width is shared equally: 40 + 35");
        Check.near(75, b(r, "b").getWidth(), T, "b width");
        Check.near(75, b(r, "b").getX(), T, "b x");
        Check.near(2, b(r, "b").getRowSpan(), T, "row span");
        Check.near(50, b(r, "b").getHeight(), T, "the spanning cell is as tall as the rows it covers");
        Check.near(10, b(r, "b").getY(), T, "b y");
        Check.near(10, b(r, "a").getY(), T, "a y");
        Check.near(35, b(r, "c").getY(), T, "rows 2 and 3 share the missing height equally (10 + 15 each = 25): c is in row 3 at y = 10 + 25");
        Check.near(25, b(r, "a").getHeight(), T, "a fills row 2");
        Check.near(60, b(r, "t").getHeight(), T, "10 + 50");
    }

    /**
     * Extra width of a wide table goes to the automatic columns in proportion to their maximum widths.
     * <p>
     * 넓은 표의 여분 너비는 자동 열에 최대 너비에 비례해 간다.
     */
    public void testAutoLayoutDistributesExtraWidth() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t style='width:300px'><tr><td id=a>xx</td><td id=b>xxxx xxxx</td></tr></table>", 400);
        Check.near(300, b(r, "t").getWidth(), T, "specified width");
        double maxA = 2 * cwd;
        double maxB = 9 * cwd;
        Check.near(300 * maxA / (maxA + maxB), b(r, "a").getWidth(), 0.5, "the extra is shared in proportion to the maximum widths");
        Check.near(300 * maxB / (maxA + maxB), b(r, "b").getWidth(), 0.5, "b");
        Check.near(300, b(r, "a").getWidth() + b(r, "b").getWidth(), T, "the columns fill the table");
    }

    /**
     * A table that is narrower than the available width shrinks to the preferred width of its content, and below the minimum width of
     * its content it overflows its specified width.
     * <p>
     * 쓸 수 있는 너비보다 좁은 표는 내용의 선호 너비로 줄어들고, 내용의 최소 너비보다 좁게 지정하면 지정한 너비를 넘는다.
     */
    public void testShrinkToFitAndMinimumWidth() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t><tr><td>abcd efgh</td></tr></table>"
                + "<table id=n style='width:10px'><tr><td>abcdefgh</td></tr></table>"
                + "<table id=w><tr><td>abcd efgh abcd efgh</td></tr></table>", 4 * cwd + 2 * cwd);
        Check.near(Math.max(4 * cwd + 2 * cwd, 4 * cwd), b(r, "t").getWidth(), T, "shrink to fit between min (4 chars) and max (9 chars): the available width 6 chars");
        Check.near(8 * cwd, b(r, "n").getWidth(), T, "a table is at least as wide as the minimum width of its content");
        Check.near(6 * cwd, b(r, "w").getWidth(), T, "wide content: limited to the available width but wraps");
    }

    /**
     * Percentage widths are shares of the table width; with fixed table layout the first row decides the widths.
     * <p>
     * 백분율 너비는 표 너비의 몫이다. 고정 표 레이아웃에서는 첫 행이 너비를 정한다.
     */
    public void testPercentAndFixedLayout() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t style='width:200px'><tr><td id=a style='width:30%'></td><td id=b></td></tr></table>"
                + "<table id=f style='width:200px;table-layout:fixed'><tr><td id=c style='width:50px'></td><td id=d></td></tr>"
                + "<tr><td>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</td><td></td></tr></table>", 400);
        Check.near(60, b(r, "a").getWidth(), T, "30 percent of 200");
        Check.near(140, b(r, "b").getWidth(), T, "the rest");
        Check.near(50, b(r, "c").getWidth(), T, "fixed layout: the first row sets the first column");
        Check.near(150, b(r, "d").getWidth(), T, "the other column takes what is left, whatever the content of later rows is");
        Check.near(200, b(r, "f").getWidth(), T, "table width");
    }

    /**
     * Row heights follow the tallest cell, the row's own height and the specified table height (extra height is shared in proportion).
     * <p>
     * 행 높이는 가장 큰 셀, 행 자신의 height, 지정된 표 높이를 따른다(여분 높이는 비례해 나눈다).
     */
    public void testRowHeights() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t style='height:100px'><tr id=r1 style='height:30px'><td id=a style='height:10px'></td></tr>"
                + "<tr id=r2><td id=b style='height:20px'></td></tr></table>", 400);
        // 행 높이는 30 과 20, 표 높이 100 에서 50 이 남아 높이에 비례해 나눈다(30 → 60, 20 → 40).
        Check.near(100, b(r, "t").getHeight(), T, "table height");
        Check.near(60, b(r, "r1").getHeight(), T, "row 1: 30 + 50 * 30 / 50");
        Check.near(40, b(r, "r2").getHeight(), T, "row 2: 20 + 50 * 20 / 50");
        Check.near(60, b(r, "r2").getY(), T, "row 2 y");
        Check.near(60, b(r, "a").getHeight(), T, "a fills its row");
    }

    /**
     * Cells are aligned vertically: top, middle, bottom and baseline.
     * <p>
     * 셀은 세로로 정렬된다: top, middle, bottom, baseline.
     */
    public void testVerticalAlign() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-spacing:0} td{padding:0;line-height:10px}</style>"
                + "<table><tr><td style='height:50px;vertical-align:top'><span id=t>x</span></td><td style='vertical-align:middle'><span id=m>x</span></td>"
                + "<td style='vertical-align:bottom'><span id=bo>x</span></td><td style='vertical-align:baseline'><span id=ba>x</span></td></tr></table>", 400);
        double top = b(r, "t").getY();
        Check.near(top + 20, b(r, "m").getY(), T, "middle: (50 - 10) / 2");
        Check.near(top + 40, b(r, "bo").getY(), T, "bottom");
        Check.near(top, b(r, "ba").getY(), T, "baseline aligned with the other first lines: the same line top here");
    }

    /**
     * Captions go above or below the table and are as wide as the table; the table height includes them.
     * <p>
     * caption 은 표 위나 아래에 놓이고 표만큼 넓으며 표 높이에 포함된다.
     */
    public void testCaption() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t><caption id=c1 style='height:10px'>x</caption><tr><td style='width:80px;height:30px'></td></tr>"
                + "<caption id=c2 style='caption-side:bottom;height:5px;margin-top:2px'>y</caption></table>", 400);
        LayoutBox t = b(r, "t");
        Check.near(80, t.getWidth(), T, "table width");
        Check.near(10 + 30 + 2 + 5, t.getHeight(), T, "height includes both captions");
        Check.near(0, b(r, "c1").getY(), T, "top caption y");
        Check.near(80, b(r, "c1").getWidth(), T, "caption width is the table width");
        Check.near(10 + 30 + 2, b(r, "c2").getY(), T, "bottom caption below the grid plus its margin");
        double[] g = t.getGridRectangle();
        Check.near(10, g[1] - t.getY(), T, "the grid starts below the top caption");
        Check.near(30, g[3], T, "grid height");
    }

    /**
     * Anonymous table boxes: cells in a plain block, content directly in a table and rows directly in a table are wrapped.
     * <p>
     * 익명 표 상자: 보통 블록 안의 셀, 표 안에 바로 있는 콘텐츠와 행이 감싸진다.
     */
    public void testAnonymousBoxes() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:table'><div id=a style='display:table-cell;width:30px;height:10px'></div>"
                + "<div id=b style='display:table-cell;width:40px'></div></div>"
                + "<div><div id=c style='display:table-cell;width:20px;height:5px'></div><div id=d style='display:table-cell;width:25px'></div></div>"
                + "<div style='display:table'>loose<div id=e style='display:table-row'><div style='display:table-cell;width:12px;height:3px'>z</div></div></div>", 400);
        Check.near(0, b(r, "a").getX(), T, "a x");
        Check.near(30, b(r, "b").getX(), T, "b beside a");
        Check.near(10, b(r, "b").getHeight(), T, "b is as tall as the row");
        Check.near(0, b(r, "c").getX(), T, "cells in a plain block get an anonymous table: c x");
        Check.near(20, b(r, "d").getX(), T, "d beside c");
        LayoutBox e = b(r, "e");
        Check.isTrue(e.getY() > 0, "the loose text became an anonymous row above the real row");
    }

    /**
     * The first header group is displayed first and the first footer group last, whatever the source order is.
     * <p>
     * 소스 순서와 상관없이 첫 헤더 그룹이 맨 앞, 첫 푸터 그룹이 맨 뒤에 표시된다.
     */
    public void testHeaderAndFooterOrder() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table><tfoot><tr><td id=f style='height:10px'></td></tr></tfoot><tbody><tr><td id=b style='height:20px'></td></tr></tbody>"
                + "<thead><tr><td id=h style='height:30px'></td></tr></thead></table>", 400);
        Check.near(0, b(r, "h").getY(), T, "header first");
        Check.near(30, b(r, "b").getY(), T, "body second");
        Check.near(50, b(r, "f").getY(), T, "footer last");
    }

    /**
     * Widths of <code>col</code> elements apply to the columns.
     * <p>
     * <code>col</code> 요소의 너비가 열에 적용된다.
     */
    public void testColumnWidths() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t style='table-layout:fixed;width:300px'><colgroup><col style='width:100px'><col span=2 style='width:50px'></colgroup>"
                + "<tr><td id=a></td><td id=b></td><td id=c></td><td id=d></td></tr></table>", 400);
        Check.near(100, b(r, "a").getWidth(), T, "first column");
        Check.near(50, b(r, "b").getWidth(), T, "second column");
        Check.near(50, b(r, "c").getWidth(), T, "third column (span 2)");
        Check.near(100, b(r, "d").getWidth(), T, "the fourth column takes the rest");
    }

    /**
     * The border and the cellspacing, cellpadding and width attributes of HTML are honoured.
     * <p>
     * HTML 의 border, cellspacing, cellpadding, width 속성이 반영된다.
     */
    public void testHtmlAttributes() {
        LayoutResult r = LayoutTestSupport.layout("<table id=t border=1 cellspacing=4 cellpadding=3 width=200><tr><td id=a width=50>x</td><td id=b>y</td></tr></table>", 400);
        LayoutBox t = b(r, "t");
        Check.near(200, t.getWidth(), T, "width attribute");
        Check.near(1, t.getBorderLeft(), T, "table border");
        LayoutBox a = b(r, "a");
        Check.near(3, a.getPaddingLeft(), T, "cellpadding");
        Check.near(1, a.getBorderLeft(), T, "cells get a 1px border when the table has a border attribute");
        Check.near(1 + 4, a.getX() - t.getX(), T, "the first cell starts after the table border and the cell spacing");
        Check.near(4, b(r, "b").getX() - a.getX() - a.getWidth(), T, "cellspacing between the cells");
    }

    /**
     * Nested tables and a table in a shrink-to-fit parent (a float) get the width of their content.
     * <p>
     * 중첩 표와 shrink-to-fit 부모(float) 안의 표는 내용의 너비를 가진다.
     */
    public void testNestedAndFloatedTables() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<div id=f style='float:left'><table id=in><tr><td style='width:60px;height:10px'><table id=nest><tr><td style='width:25px;height:4px'></td></tr></table></td></tr></table></div>", 400);
        Check.near(60, b(r, "f").getWidth(), T, "the float shrinks to the table");
        Check.near(25, b(r, "nest").getWidth(), T, "nested table");
        Check.near(10, b(r, "in").getHeight(), T, "outer table height");
    }

    /**
     * A table can be centered with auto margins and sits beside floats.
     * <p>
     * 표는 auto margin 으로 가운데에 놓을 수 있고 float 옆에 놓인다.
     */
    public void testTableMarginsAndFloats() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=c style='margin:0 auto'><tr><td style='width:100px;height:5px'></td></tr></table>"
                + "<div style='float:left;width:50px;height:50px'></div><table id=n style='width:100px'><tr><td style='height:5px'></td></tr></table>", 400);
        Check.near(150, b(r, "c").getX(), T, "centered: (400 - 100) / 2");
        Check.near(50, b(r, "n").getX(), T, "a table does not overlap a float: it starts right of it");
        Check.near(5, b(r, "n").getY(), T, "and stays beside it (y is below the first table)");
    }

    /**
     * A big table is laid out in a reasonable time.
     * <p>
     * 큰 표도 적당한 시간에 배치된다.
     */
    public void testBigTable() {
        StringBuilder sb = new StringBuilder("<table id=t>");
        // 40 행 25 열의 표를 만든다.
        for (int i = 0; i < 40; i++) {
            sb.append("<tr>");
            // 한 행에 25 개의 셀을 만든다.
            for (int j = 0; j < 25; j++) {
                sb.append("<td>c").append(i).append('x').append(j).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        long start = System.currentTimeMillis();
        LayoutResult r = LayoutTestSupport.layout(PLAIN + sb, 800);
        long took = System.currentTimeMillis() - start;
        Check.isTrue(took < 20000, "1000 cells in reasonable time: " + took + " ms");
        List<LayoutBox> cells = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(b(r, "t"), LayoutBox.Type.TABLE_CELL, cells);
        Check.equal(1000, cells.size(), "all cells have boxes");
    }

    /**
     * Degenerate tables lay out without error: no rows, a caption only, text only, rows without cells, and a row span that is limited
     * to its row group or runs to the end of the group (<code>rowspan=0</code>).
     * <p>
     * 퇴화한 표도 오류 없이 배치된다: 행이 없는 표, caption 만 있는 표, 텍스트만 있는 표, 셀이 없는 행. 행 범위는 행 그룹까지로 제한되고 <code>rowspan=0</code> 은 그룹 끝까지이다.
     */
    public void testDegenerateTables() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=e></table><table id=c><caption style='height:7px'>c</caption></table>"
                + "<div id=x style='display:table'>loose text</div><table id=n><tr id=nr></tr></table>"
                + "<table id=g><tbody><tr><td id=a rowspan=5 style='width:10px;height:30px'></td><td id=b style='height:5px'></td></tr><tr><td id=c2 style='height:5px'></td></tr></tbody>"
                + "<tbody><tr><td id=d style='height:9px'></td></tr></tbody></table>"
                + "<table id=z><tr><td id=zz rowspan=0 style='height:4px'></td><td style='height:2px'></td></tr><tr><td style='height:2px'></td></tr></table>", 400);
        Check.near(0, b(r, "e").getHeight(), T, "empty table height");
        Check.near(0, b(r, "e").getWidth(), T, "empty table width");
        Check.near(7, b(r, "c").getHeight(), T, "caption only");
        Check.isTrue(b(r, "x").getHeight() > 0, "text only gets an anonymous cell");
        Check.near(0, b(r, "nr").getHeight(), T, "a row without cells");
        Check.equal(2, b(r, "a").getRowSpan(), "the row span is limited to the row group (2 rows)");
        Check.near(30, b(r, "a").getHeight(), T, "a spans both rows of its group");
        Check.near(30, b(r, "d").getY() - b(r, "a").getY(), T, "the second group starts below the first (rows grew to 15 + 15)");
        Check.equal(2, b(r, "zz").getRowSpan(), "rowspan=0 spans to the end of the group");
    }

    /**
     * An <code>inline-table</code> sits in a line of text and its baseline is the baseline of its first row.
     * <p>
     * <code>inline-table</code> 은 텍스트 줄 안에 놓이고 기준선은 첫 행의 기준선이다.
     */
    public void testInlineTable() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<p id=p style='margin:0;line-height:10px'>ab<span id=it style='display:inline-table'><span style='display:table-row'>"
                + "<span id=c1 style='display:table-cell;line-height:10px'>x</span></span></span>cd</p>", 400);
        LayoutBox it = b(r, "it");
        Check.equal(LayoutBox.Type.TABLE, it.getType(), "inline-table box");
        Check.isTrue(it.isInlineLevel(), "inline-level");
        Check.near(2 * cwd, it.getX(), T, "after the two letters");
        Check.near(cwd, it.getWidth(), T, "the width of its cell");
        int lines = 0;
        // p 의 직접 자식 줄만 센다(표 안의 줄은 제외).
        for (LayoutBox c : b(r, "p").getChildren()) {
            if (c.getType() == LayoutBox.Type.LINE) {
                lines++;
            }
        }
        Check.equal(1, lines, "one line");
        // 표 안 텍스트의 기준선이 줄의 텍스트 기준선과 같다.
        List<LayoutBox> texts = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(b(r, "p"), LayoutBox.Type.TEXT, texts);
        Check.near(texts.get(0).getBaseline(), texts.get(1).getBaseline(), 0.6, "the baseline of the inline-table is the baseline of its first row");
    }

    /**
     * A table can be positioned: relative, absolute and floated tables keep their size and are placed as other boxes are.
     * <p>
     * 표도 위치를 지정할 수 있다: 상대, 절대, float 표는 크기를 유지하고 다른 상자처럼 놓인다.
     */
    public void testPositionedTables() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=rel style='position:relative;left:7px;top:3px'><tr><td style='width:20px;height:10px'></td></tr></table>"
                + "<table id=abs style='position:absolute;right:10px;bottom:5px'><tr><td style='width:30px;height:12px'></td></tr></table>"
                + "<table id=fl style='float:right'><tr><td style='width:40px;height:8px'></td></tr></table>", 400);
        Check.near(7, b(r, "rel").getX(), T, "relative x");
        Check.near(3, b(r, "rel").getY(), T, "relative y");
        Check.near(400 - 10 - 30, b(r, "abs").getX(), T, "absolute x");
        Check.near(600 - 5 - 12, b(r, "abs").getY(), T, "absolute y");
        Check.near(360, b(r, "fl").getX(), T, "float right x");
        Check.near(10, b(r, "fl").getY(), T, "float below the relative table, which keeps its space");
    }

    /**
     * Content that is wider than a fixed column overflows the cell instead of widening the table.
     * <p>
     * 고정 열보다 넓은 내용은 표를 넓히지 않고 셀을 넘친다.
     */
    public void testFixedLayoutOverflow() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table id=t style='table-layout:fixed;width:100px'><tr><td id=a style='width:40px'>wwwwwwwwwwwwwwwwwwwwwwwwwwwwwww</td><td id=b></td></tr></table>", 400);
        Check.near(100, b(r, "t").getWidth(), T, "the table keeps its width");
        Check.near(40, b(r, "a").getWidth(), T, "the cell keeps the column width");
        Check.near(60, b(r, "b").getWidth(), T, "the other column");
    }

    /**
     * Cells keep the alignment of the text, and rows that are taller than their cells stretch the cells.
     * <p>
     * 셀은 텍스트 정렬을 유지하고 셀보다 큰 행은 셀을 늘린다.
     */
    public void testCellStretchAndTextAlign() {
        LayoutResult r = LayoutTestSupport.layout(PLAIN + "<table style='width:100px'><tr><td id=a style='text-align:right;vertical-align:top'>ab</td><td id=b style='height:40px'></td></tr></table>", 400);
        Check.near(40, b(r, "a").getHeight(), T, "the cell is as tall as the row");
        List<LayoutBox> texts = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(b(r, "a"), LayoutBox.Type.TEXT, texts);
        double cwd = LayoutTestSupport.charWidth();
        Check.near(b(r, "a").getX() + b(r, "a").getWidth() - 2 * cwd, texts.get(0).getX(), 0.6, "right aligned text inside the cell");
    }

    /**
     * Border spacing does not shrink the last column and cell rectangles of rows, row groups and columns cover only the columns.
     * <p>
     * border 간격은 마지막 열을 줄이지 않으며 행, 행 그룹, 열의 사각형은 열만 덮는다.
     */
    public void testSpacingGeometryOfParts() {
        LayoutResult r = LayoutTestSupport.layout("<style>table{border-spacing:10px} td{padding:0;width:20px;height:20px}</style>"
                + "<table id=t><colgroup id=cg><col id=c1><col id=c2></colgroup><tbody id=g><tr id=r><td id=a></td><td id=b></td></tr></tbody></table>", 400);
        Check.near(20, b(r, "b").getWidth(), T, "the last cell keeps its width");
        Check.near(70, b(r, "t").getWidth(), T, "10 + 20 + 10 + 20 + 10");
        Check.near(50, b(r, "r").getWidth(), T, "the row covers the cells and the space between them");
        Check.near(10, b(r, "r").getX(), T, "row x");
        Check.near(50, b(r, "g").getWidth(), T, "row group width");
        Check.near(20, b(r, "c2").getWidth(), T, "column width");
        Check.near(40, b(r, "c2").getX(), T, "second column x");
        Check.near(50, b(r, "cg").getWidth(), T, "column group width");
    }
}
