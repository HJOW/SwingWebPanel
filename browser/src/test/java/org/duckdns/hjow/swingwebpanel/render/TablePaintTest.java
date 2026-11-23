package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of how tables are painted and found by the pointer (plan item P10-02): the layers of a table (table, columns, row groups, rows,
 * cells; CSS 2.1 17.5.1) are painted in order with real pixels, the background and border of the table cover the grid but not the
 * captions, <code>empty-cells:hide</code> hides empty cells, collapsed borders are painted as strips of the resolved style and color
 * above the cell backgrounds, and a hit test finds the cell, the row and the table.
 * <p>
 * 표를 그리고 포인터로 찾는 방식 시험(계획 P10-02): 표의 계층(표, 열, 행 그룹, 행, 셀. CSS 2.1 17.5.1)이 실제 픽셀로 순서대로 그려지는지, 표의 배경과 border 가
 * caption 이 아닌 격자를 덮는지, <code>empty-cells:hide</code> 가 빈 셀을 숨기는지, 합쳐진 border 가 이긴 스타일과 색의 띠로 셀 배경 위에 그려지는지, 히트 테스트가
 * 셀, 행, 표를 찾는지 확인한다.
 */
public class TablePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TablePaintTest() {
    }

    /**
     * Returns the id of the element of a hit.
     * <p>
     * 히트의 요소 id 를 반환한다.
     *
     * @param h the hit / 히트
     * @return the id or "" / id 또는 ""
     */
    private static String idOf(HitResult h) {
        return h.getElement() == null || h.getElement().getAttribute("id") == null ? "" : h.getElement().getAttribute("id");
    }

    /**
     * The layers are painted from the bottom: table, row, cell; the spacing shows the table, the area between the cells of a row shows
     * the row, and a cell covers its row.
     * <p>
     * 계층은 아래부터 그려진다: 표, 행, 셀. 간격에는 표가, 한 행의 셀 사이에는 행이 보이고 셀은 행을 덮는다.
     */
    public void testLayersOfSeparatedTable() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-spacing:10px;background:#00f} td{padding:0;width:20px;height:20px}</style>"
                + "<table id=t><tr id=r style='background:#0f0'><td id=a style='background:#f00'></td><td id=b></td></tr>"
                + "<tr><td id=c></td><td id=d></td></tr></table>", 200, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 200);
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 3, 3), "the table background shows in the spacing around the cells");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 20, 20), "the cell is above the row");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 35, 20), "the row shows between its cells (spacing inside the row)");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 55, 20), "the cell without a background shows the row");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 35, 35), "between the rows only the table shows");
        Check.equal("a", idOf(p.hitTestDocument(20, 20)), "hit: cell");
        Check.equal("r", idOf(p.hitTestDocument(35, 20)), "hit: the row in the spacing");
        Check.equal("tbody", idOfElement(p.hitTestDocument(35, 35)), "hit: the row group, whose box covers the spacing between its rows");
        Check.equal("b", idOf(p.hitTestDocument(55, 20)), "hit: the cell without a background");
    }

    /**
     * The table background and border belong to the grid: captions are outside of them.
     * <p>
     * 표의 배경과 border 는 격자에 속한다: caption 은 그 밖이다.
     */
    public void testCaptionIsOutsideOfTableBackground() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-spacing:0;background:#00f;border:4px solid #000} td{padding:0;width:40px;height:20px} caption{height:10px}</style>"
                + "<table id=t><caption>c</caption><tr><td></td></tr></table>", 200, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 200);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 20, 5), "above the grid (in the caption) there is no table background");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 20, 12), "the table border starts below the caption");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 20, 24), "the background is behind the cell area (no cell background here)");
    }

    /**
     * Cells without content are not painted when <code>empty-cells:hide</code> applies; cells with content are.
     * <p>
     * <code>empty-cells:hide</code> 일 때 내용이 없는 셀은 그리지 않고 내용이 있는 셀은 그린다.
     */
    public void testEmptyCellsHide() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-spacing:4px;empty-cells:hide} td{padding:0;width:20px;height:20px;background:#f00;border:2px solid #000}</style>"
                + "<table><tr><td id=a>x</td><td id=b></td></tr></table>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        Check.equal(0x000000, RenderTestSupport.rgb(img, 5, 14), "the cell with content has a border (x 4..6)");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 33, 14), "the empty cell (x 32..56) shows no border");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 44, 20), "and no background");
        LayoutBox box = p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("b")).get(0);
        Check.isTrue(box.isHiddenEmptyCell(), "layout flag");
        Check.isFalse(p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("a")).get(0).isHiddenEmptyCell(), "no flag with content");
    }

    /**
     * Collapsed borders are painted above the backgrounds of the cells, centered on the grid lines, in the style and color that won.
     * <p>
     * 합쳐진 border 는 셀 배경 위에 격자선을 중심으로 이긴 스타일과 색으로 그려진다.
     */
    public void testCollapsedBordersArePainted() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-collapse:collapse} td{padding:0;width:20px;height:20px;background:#ff0;border:4px solid #000}</style>"
                + "<table><tr><td></td><td style='border-color:#f00'></td></tr></table>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        // 셀 폭 20 + 반 border 2 + 2 = 24, 격자선 0 은 x=2, 1 은 x=26, 2 는 x=50; 두께 4.
        Check.equal(0x000000, RenderTestSupport.rgb(img, 1, 10), "the outer left border (x 0..4)");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 10, 10), "the cell background inside");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 25, 10), "the shared border between the cells: left wins the tie, so black");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 10, 1), "the top border");
        Check.equal(0x000000, RenderTestSupport.rgb(img, 10, 26), "the bottom border (y 24..28 centered on 26)");
        // 오른쪽 셀의 바깥 오른쪽 border 는 그 셀 자신의 색(빨강)이다.
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 51, 10), "the outer right border has the color of the right cell");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 60, 10), "outside the table");
    }

    /**
     * A wider cell border wins the shared edge and is painted with its own thickness.
     * <p>
     * 더 넓은 셀 border 가 공유 가장자리를 이기고 자기 두께로 그려진다.
     */
    public void testWiderBorderWins() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-collapse:collapse} td{padding:0;width:20px;height:20px}</style>"
                + "<table><tr><td style='border:2px solid #000'></td><td style='border:6px solid #f00'></td></tr></table>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        // 왼쪽 셀: 바깥 왼쪽 반 1 + 20 + 가운데 반 3 = 24 → 가운데 격자선은 x = 1 + 24 = 25, 두께 6 → 22..28.
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 23, 10), "the 6px red border covers x 22..28");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 27, 10), "also on the right half");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 21, 10), "and not wider");
    }

    /**
     * The cell content (text and blocks) is above the table backgrounds and borders.
     * <p>
     * 셀 내용(텍스트와 블록)은 표 배경과 border 위에 있다.
     */
    public void testCellContentIsOnTop() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-collapse:collapse;background:#00f} td{padding:0;border:2px solid #000}</style>"
                + "<table><tr><td><div id=in style='width:30px;height:12px;background:#0f0'></div></td></tr></table>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 10, 8), "the block in the cell is painted");
        Check.equal("in", idOf(p.hitTestDocument(10, 8)), "and found");
        Element td = (Element) RenderTestSupport.lastDocument.getElementById("in").getParentNode();
        Check.equal("td", td.getLocalName(), "inside a cell");
        Check.equal("td", idOfElement(p.hitTestDocument(1, 1)), "the corner belongs to the cell");
    }

    /**
     * Returns the local name of the element of a hit.
     * <p>
     * 히트 요소의 로컬 이름을 반환한다.
     *
     * @param h the hit / 히트
     * @return the name or "" / 이름 또는 ""
     */
    private static String idOfElement(HitResult h) {
        return h.getElement() == null ? "" : h.getElement().getLocalName();
    }

    /**
     * Row and column backgrounds are painted under the cells; a column background shows through cells without a background.
     * <p>
     * 행과 열 배경은 셀 아래에 그려지며 열 배경은 배경이 없는 셀을 통해 보인다.
     */
    public void testColumnAndRowGroupBackgrounds() {
        RenderedPage p = RenderTestSupport.page("<style>table{border-spacing:0} td{padding:0;width:20px;height:20px}</style>"
                + "<table><colgroup><col style='background:#f00'><col></colgroup><tbody style='background:#00f'><tr><td></td><td></td></tr></tbody></table>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 10, 10), "the row group is above the column");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 30, 10), "the row group covers the second column");
    }
}
