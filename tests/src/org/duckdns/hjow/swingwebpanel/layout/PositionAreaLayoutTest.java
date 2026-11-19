package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>position-area</code> (CSS Anchor Positioning Level 1 section 3.1 and 4.1, https://www.w3.org/TR/css-anchor-position-1/#position-area): the edges
 * of the anchor box and of the containing block make a 3 by 3 grid, the keywords select a region of it, the positioned box is laid out inside the region and
 * its default alignment puts it toward the anchor. The container is 300 by 200 and the anchor is 100 by 30 at (50, 40), so the columns are 0 to 50, 50 to 150 and
 * 150 to 300 and the rows 0 to 40, 40 to 70 and 70 to 200.
 * <p>
 * <code>position-area</code> 시험(CSS Anchor Positioning Level 1 3.1, 4.1 절): 앵커 상자와 포함 블록의 가장자리가 3x3 격자를 만들고, 키워드가 그 영역을 고르며, 위치 지정 상자는
 * 영역 안에 배치되고 기본 정렬은 앵커 쪽으로 붙인다. 컨테이너는 300x200 이고 앵커는 (50, 40) 의 100x30 이므로 열은 0~50, 50~150, 150~300 이고 행은 0~40, 40~70, 70~200 이다.
 */
public class PositionAreaLayoutTest {

    /** The container of 300 by 200 that is the containing block, with a 40 px spacer. 포함 블록인 300x200 컨테이너와 40px 간격 상자. */
    private static final String WRAP = "<div id=wrap style='position:relative;width:300px;height:200px;margin:0'><div style='height:40px'></div>";

    /** The anchor: 100 by 30 at (50, 40). 앵커: (50, 40) 의 100x30. */
    private static final String ANCHOR = "<div id=anc style='anchor-name:--a;margin-left:50px;width:100px;height:30px'></div>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PositionAreaLayoutTest() {
    }

    /**
     * Lays out the container with a positioned box of 40 by 20 that has a position area.
     * <p>
     * 위치 영역이 있는 40x20 배치 상자가 든 컨테이너를 배치한다.
     *
     * @param style the style that is added to the box / 상자에 더하는 스타일
     * @return the layout / 레이아웃
     */
    private static LayoutResult layout(String style) {
        return LayoutTestSupport.layout(WRAP + "<div id=pop style='position:absolute;position-anchor:--a;width:40px;height:20px;" + style + "'></div>" + ANCHOR + "</div>", 600);
    }

    /**
     * The x of the box relative to the container.
     * <p>
     * 컨테이너에 대한 상자의 x.
     *
     * @param r the layout / 레이아웃
     * @return x / x
     */
    private static double x(LayoutResult r) {
        return LayoutTestSupport.box(r, "pop").getX() - LayoutTestSupport.box(r, "wrap").getX();
    }

    /**
     * The y of the box relative to the container.
     * <p>
     * 컨테이너에 대한 상자의 y.
     *
     * @param r the layout / 레이아웃
     * @return y / y
     */
    private static double y(LayoutResult r) {
        return LayoutTestSupport.box(r, "pop").getY() - LayoutTestSupport.box(r, "wrap").getY();
    }

    /**
     * <code>top</code> is the top row across all columns: the box sits right above the anchor, centered on it.
     * <p>
     * <code>top</code> 은 모든 열에 걸친 위쪽 행이다: 상자는 앵커 바로 위에 앵커의 가운데에 놓인다.
     */
    public void testTopAndBottomAreCenteredOnTheAnchor() {
        LayoutResult top = layout("position-area:top");
        Check.near(80, x(top), 0.01, "centered on the anchor (center 100, width 40)");
        Check.near(20, y(top), 0.01, "its bottom edge on the top edge of the anchor (40 - 20)");
        LayoutResult bottom = layout("position-area:bottom");
        Check.near(80, x(bottom), 0.01, "bottom: centered on the anchor");
        Check.near(70, y(bottom), 0.01, "its top edge on the bottom edge of the anchor");
    }

    /**
     * <code>left</code> and <code>right</code> select a column across all rows: the box is centered on the anchor vertically and touches it.
     * <p>
     * <code>left</code> 와 <code>right</code> 는 모든 행에 걸친 열 하나를 고른다: 상자는 앵커의 세로 가운데에 놓이고 앵커에 붙는다.
     */
    public void testLeftAndRight() {
        LayoutResult left = layout("position-area:left");
        Check.near(10, x(left), 0.01, "its right edge on the left edge of the anchor (50 - 40)");
        Check.near(45, y(left), 0.01, "centered on the anchor vertically (center 55, height 20)");
        LayoutResult right = layout("position-area:right");
        Check.near(150, x(right), 0.01, "its left edge on the right edge of the anchor");
        Check.near(45, y(right), 0.01, "centered vertically");
    }

    /**
     * Two keywords select a corner region and the box touches the anchor at its corner.
     * <p>
     * 키워드 둘은 모서리 영역을 고르며 상자는 모서리에서 앵커에 닿는다.
     */
    public void testCorners() {
        LayoutResult tl = layout("position-area:top left");
        Check.near(10, x(tl), 0.01, "top left: the right edge on the left edge of the anchor");
        Check.near(20, y(tl), 0.01, "top left: the bottom edge on the top edge of the anchor");
        LayoutResult br = layout("position-area:bottom right");
        Check.near(150, x(br), 0.01, "bottom right: at the right edge of the anchor");
        Check.near(70, y(br), 0.01, "bottom right: at the bottom edge of the anchor");
        LayoutResult order = layout("position-area:right bottom");
        Check.near(150, x(order), 0.01, "the order of the keywords does not matter for the axes that they name");
        Check.near(70, y(order), 0.01, "the same box");
    }

    /**
     * <code>center</code> is the anchor box itself, and a span keyword spans two tracks.
     * <p>
     * <code>center</code> 는 앵커 상자 자체이며 span 키워드는 트랙 둘에 걸친다.
     */
    public void testCenterAndSpan() {
        LayoutResult c = layout("position-area:center");
        Check.near(80, x(c), 0.01, "centered in the anchor");
        Check.near(45, y(c), 0.01, "centered in the anchor vertically");
        LayoutResult s = layout("position-area:span-top");
        Check.near(50, y(s), 0.01, "span-top: the rows 0 to 70, the box sits at the bottom of them (70 - 20 = 50)");
        Check.near(80, x(s), 0.01, "centered on the anchor horizontally");
        LayoutResult sl = layout("position-area:bottom span-left");
        Check.near(110, x(sl), 0.01, "span-left covers the columns 0 to 150, the box ends at its end (150 - 40 = 110)");
        Check.near(70, y(sl), 0.01, "the bottom row starts at the bottom edge of the anchor");
    }

    /**
     * A box that is wider than its region is shifted back into the containing block, and without a default anchor the value does nothing.
     * <p>
     * 영역보다 넓은 상자는 포함 블록 안으로 되돌려 옮기고, 기본 앵커가 없으면 값은 아무 일도 하지 않는다.
     */
    public void testShiftAndNoAnchor() {
        LayoutResult wide = LayoutTestSupport.layout(WRAP + "<div id=pop style='position:absolute;position-anchor:--a;width:80px;height:20px;position-area:left'></div>" + ANCHOR + "</div>", 600);
        Check.near(0, x(wide), 0.01, "wider than the 50 px column: shifted to the edge of the containing block");
        LayoutResult none = LayoutTestSupport.layout(WRAP + "<div id=pop style='position:absolute;width:40px;height:20px;top:5px;left:7px;position-area:top'></div>" + ANCHOR + "</div>", 600);
        Check.near(7, x(none), 0.01, "no position-anchor: the value does not apply");
        Check.near(5, y(none), 0.01, "so the insets are used");
    }
}
