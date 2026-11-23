package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of grid layout (plan item P10-03, CSS Grid Layout Module Level 1): explicit tracks with lengths, <code>fr</code>,
 * <code>auto</code>, <code>min-content</code>, <code>max-content</code>, <code>minmax()</code> and <code>repeat()</code> (also
 * <code>auto-fill</code>), gaps, the placement of items by line numbers, negative lines, spans, names and areas, the auto-placement
 * algorithm of rows, columns and <code>dense</code>, implicit tracks (<code>grid-auto-rows</code>), the track sizing algorithm
 * (intrinsic contributions, spanning items, flexible tracks that grow with their content, stretching of <code>auto</code> tracks), the
 * alignment of the items in their area and of the grid in the container, <code>inline-grid</code> and its intrinsic width, absolutely
 * positioned children and many items. The expected coordinates are computed by hand from the specification and checked against real
 * runs of the HTML parser, the CSS engine and the layout engine.
 * <p>
 * 그리드 레이아웃 시험(계획 P10-03, CSS Grid Layout Module Level 1): 길이, <code>fr</code>, <code>auto</code>, <code>min-content</code>, <code>max-content</code>,
 * <code>minmax()</code>, <code>repeat()</code>(<code>auto-fill</code> 포함)를 쓴 명시적 트랙, gap, 선 번호, 음수 선, span, 이름, 영역으로 하는 항목 배치, 행/열/<code>dense</code> 자동 배치
 * 알고리즘, 암시적 트랙(<code>grid-auto-rows</code>), 트랙 크기 결정 알고리즘(고유 기여, 여러 트랙에 걸친 항목, 내용과 함께 커지는 유연한 트랙, <code>auto</code> 트랙 늘리기),
 * 영역 안 항목의 정렬과 컨테이너 안 그리드의 정렬, <code>inline-grid</code> 와 고유 너비, 절대 배치 자식, 많은 항목. 기대 좌표는 규격으로 손계산했으며 HTML 파서, CSS 엔진,
 * 레이아웃 엔진을 실제로 돌려 확인한다.
 */
public class GridLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public GridLayoutTest() {
    }

    /**
     * Builds n items with a height: <code>&lt;div id=PREFIXi style='height:Hpx'&gt;</code>.
     * <p>
     * 높이가 있는 항목 n 개의 마크업을 만든다.
     *
     * @param prefix id prefix / id 접두어
     * @param n      number of items / 항목 수
     * @param h      height / 높이
     * @return the markup / 마크업
     */
    private static String items(String prefix, int n, int h) {
        StringBuilder sb = new StringBuilder();
        // 같은 높이의 항목을 n 개 만든다.
        for (int i = 1; i <= n; i++) {
            sb.append("<div id=").append(prefix).append(i).append(" style='height:").append(h).append("px'></div>");
        }
        return sb.toString();
    }

    /**
     * Gets the box of an id.
     * <p>
     * id 의 상자를 얻는다.
     *
     * @param r  the result / 결과
     * @param id the id / id
     * @return the box / 상자
     */
    private static LayoutBox b(LayoutResult r, String id) {
        return LayoutTestSupport.box(r, id);
    }

    /**
     * Tracks with lengths and <code>fr</code>: the flexible tracks share what is left in proportion; the items are placed in row order and
     * stretch to their area.
     * <p>
     * 길이와 <code>fr</code> 트랙: 유연한 트랙은 남는 공간을 비율로 나누고 항목은 행 순서로 놓여 영역에 맞춰 늘어난다.
     */
    public void testExplicitTracksAndFr() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:100px 1fr 2fr;grid-template-rows:20px 30px'>"
                + "<div id=a></div><div id=b></div><div id=c></div><div id=d></div><div id=e></div><div id=f></div></div><div id=after style='height:5px'></div>", 400);
        Check.isTrue(b(r, "g").isGridContainer() && b(r, "a").isGridItem(), "grid flags");
        Check.near(0, b(r, "a").getX(), T, "a x");
        Check.near(100, b(r, "a").getWidth(), T, "a width: the fixed track");
        Check.near(100, b(r, "b").getX(), T, "b x");
        Check.near(200.0 / 3, b(r, "b").getWidth(), T, "b width: 1fr of 200");
        Check.near(100 + 200.0 / 3, b(r, "c").getX(), T, "c x");
        Check.near(400.0 / 3, b(r, "c").getWidth(), T, "c width: 2fr of 200");
        Check.near(20, b(r, "a").getHeight(), T, "first row height");
        Check.near(20, b(r, "d").getY(), T, "d is in the second row");
        Check.near(30, b(r, "d").getHeight(), T, "second row height");
        Check.near(50, b(r, "g").getHeight(), T, "container height is the sum of the rows");
        Check.near(50, b(r, "after").getY(), T, "the next block follows the container");
    }

    /**
     * <code>repeat()</code> with a number and gaps: the columns are separated by the column gap and the rows by the row gap.
     * <p>
     * 숫자를 쓴 <code>repeat()</code> 와 gap: 열은 열 gap 으로, 행은 행 gap 으로 떨어진다.
     */
    public void testRepeatAndGaps() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:340px;grid-template-columns:repeat(3,1fr);gap:10px 20px'>" + items("i", 6, 15) + "</div>", 500);
        Check.near(100, b(r, "i1").getWidth(), T, "column width (340 - 2*20)/3");
        Check.near(120, b(r, "i2").getX(), T, "second column");
        Check.near(240, b(r, "i3").getX(), T, "third column");
        Check.near(0, b(r, "i4").getX(), T, "fourth item starts the second row");
        Check.near(25, b(r, "i4").getY(), T, "row height 15 + row gap 10");
        Check.near(40, b(r, "g").getHeight(), T, "two rows and a gap: 15 + 10 + 15");
    }

    /**
     * Items are placed by line numbers, spans and negative lines; the others fill the free cells in order.
     * <p>
     * 항목은 선 번호, span, 음수 선으로 놓이고 나머지는 빈 칸을 순서대로 채운다.
     */
    public void testPlacementByLines() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:400px;grid-template-columns:repeat(4,100px);grid-template-rows:repeat(3,10px)'>"
                + "<div id=a style='grid-column-start:2;grid-column-end:4'></div>"
                + "<div id=b style='grid-column-start:span 2'></div>"
                + "<div id=c style='grid-column-start:-2;grid-row-start:3'></div>"
                + "<div id=d style='grid-row-start:2;grid-row-end:span 2;grid-column-start:1'></div>"
                + "<div id=e></div></div>", 500);
        Check.near(100, b(r, "a").getX(), T, "a: lines 2 to 4");
        Check.near(200, b(r, "a").getWidth(), T, "a spans two columns");
        Check.near(0, b(r, "a").getY(), T, "a in the first row");
        // c(열 -2 = 4번째 열, 3행)와 d(1열, 2~3행)가 먼저 자리를 잡고 a 는 열 2~3 의 첫 행이다.
        Check.near(0, b(r, "a").getY(), T, "a in the first row");
        Check.near(100, b(r, "b").getX(), T, "b: span 2 does not fit behind a (column 4 left), so it starts the second row at the first fit: columns 2-3");
        Check.near(10, b(r, "b").getY(), T, "b in the second row (column 1 of that row belongs to d)");
        Check.near(200, b(r, "b").getWidth(), T, "b spans two columns");
        Check.near(300, b(r, "c").getX(), T, "c: line -2 is the fourth column");
        Check.near(20, b(r, "c").getY(), T, "c in the third row");
        Check.near(0, b(r, "d").getX(), T, "d in the first column");
        Check.near(10, b(r, "d").getY(), T, "d starts at row 2");
        Check.near(20, b(r, "d").getHeight(), T, "d spans two rows");
        Check.near(300, b(r, "e").getX(), T, "e continues behind b: the fourth column of the second row");
        Check.near(10, b(r, "e").getY(), T, "e y");
    }

    /**
     * The auto-placement cursor moves on: items that do not fit in the rest of a row start the next row; an item with a definite column
     * moves the cursor to the next row if it is behind the cursor.
     * <p>
     * 자동 배치 커서는 앞으로 나아간다: 행의 남은 칸에 들어가지 않는 항목은 다음 행에서 시작하고 열이 정해진 항목이 커서보다 앞이면 커서는 다음 행으로 간다.
     */
    public void testAutoPlacementCursor() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:repeat(3,100px)'>"
                + "<div id=a style='height:10px;grid-column-start:span 2'></div><div id=b style='height:10px;grid-column-start:span 2'></div>"
                + "<div id=c style='height:10px'></div></div>", 500);
        Check.near(0, b(r, "a").getX(), T, "a at the start");
        Check.near(200, b(r, "a").getWidth(), T, "a spans two columns");
        Check.near(0, b(r, "b").getX(), T, "b does not fit beside a (one column left): next row");
        Check.near(10, b(r, "b").getY(), T, "b in the second row");
        Check.near(200, b(r, "c").getX(), T, "c continues after b (sparse packing: it does not go back to the hole beside a)");
        Check.near(10, b(r, "c").getY(), T, "c in the second row");
    }

    /**
     * <code>dense</code> packing goes back to fill the holes.
     * <p>
     * <code>dense</code> 배치는 구멍을 메우러 되돌아간다.
     */
    public void testDensePacking() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:repeat(3,100px);grid-auto-flow:row dense'>"
                + "<div id=a style='height:10px;grid-column-start:span 2'></div><div id=b style='height:10px;grid-column-start:span 2'></div>"
                + "<div id=c style='height:10px'></div></div>", 500);
        Check.near(200, b(r, "c").getX(), T, "c fills the hole beside a");
        Check.near(0, b(r, "c").getY(), T, "c in the first row");
        Check.near(10, b(r, "b").getY(), T, "b in the second row");
    }

    /**
     * <code>grid-auto-flow:column</code> fills the columns first.
     * <p>
     * <code>grid-auto-flow:column</code> 은 열을 먼저 채운다.
     */
    public void testColumnFlow() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-rows:repeat(2,10px);grid-auto-flow:column;grid-auto-columns:50px'>"
                + "<div id=a></div><div id=b></div><div id=c></div></div>", 500);
        Check.near(0, b(r, "a").getX(), T, "a at the start");
        Check.near(0, b(r, "b").getX(), T, "b below a");
        Check.near(10, b(r, "b").getY(), T, "b in the second row");
        Check.near(50, b(r, "c").getX(), T, "c in the next (implicit) column");
        Check.near(50, b(r, "c").getWidth(), T, "implicit column width from grid-auto-columns");
    }

    /**
     * Named areas give named lines: items refer to an area with its name.
     * <p>
     * 이름 있는 영역은 이름 있는 선을 준다: 항목은 영역의 이름으로 영역을 가리킨다.
     */
    public void testNamedAreas() {
        String area = "grid-row-start:%s;grid-row-end:%s;grid-column-start:%s;grid-column-end:%s";
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:100px 200px;grid-template-rows:20px 30px;"
                + "grid-template-areas:\"head head\" \"side main\"'>"
                + "<div id=h style='" + String.format(area, "head", "head", "head", "head") + "'></div>"
                + "<div id=s style='" + String.format(area, "side", "side", "side", "side") + "'></div>"
                + "<div id=m style='" + String.format(area, "main", "main", "main", "main") + "'></div></div>", 500);
        Check.near(0, b(r, "h").getX(), T, "head x");
        Check.near(300, b(r, "h").getWidth(), T, "head spans both columns");
        Check.near(20, b(r, "h").getHeight(), T, "head height");
        Check.near(0, b(r, "s").getX(), T, "side x");
        Check.near(100, b(r, "s").getWidth(), T, "side width");
        Check.near(20, b(r, "s").getY(), T, "side y");
        Check.near(100, b(r, "m").getX(), T, "main x");
        Check.near(200, b(r, "m").getWidth(), T, "main width");
        Check.near(30, b(r, "m").getHeight(), T, "main height");
    }

    /**
     * Named lines of the template are found by name and occurrence.
     * <p>
     * 템플릿의 이름 있는 선은 이름과 몇 번째인지로 찾는다.
     */
    public void testNamedLines() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:[a] 50px [b] 100px [c] 150px [d];grid-template-rows:10px'>"
                + "<div id=x style='grid-column-start:b;grid-column-end:d'></div></div>", 500);
        Check.near(50, b(r, "x").getX(), T, "starts at line b");
        Check.near(250, b(r, "x").getWidth(), T, "ends at line d");
    }

    /**
     * Intrinsic track sizes: <code>auto</code> tracks fit their items and share the free space, <code>min-content</code> and
     * <code>max-content</code> do not grow.
     * <p>
     * 고유 트랙 크기: <code>auto</code> 트랙은 항목에 맞고 남는 공간을 나눠 가지며 <code>min-content</code> 와 <code>max-content</code> 는 늘어나지 않는다.
     */
    public void testIntrinsicTracks() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:auto auto'><div id=a>aaaa</div><div id=b>bb</div></div>"
                + "<div id=h style='display:grid;width:200px;grid-template-columns:max-content min-content 50px'><div id=c>cc cc</div><div id=d>dd dd</div><div id=e></div></div>", 500);
        double free = 200 - 6 * cw;
        Check.near(4 * cw + free / 2, b(r, "a").getWidth(), T, "auto track: content plus half of the free space");
        Check.near(2 * cw + free / 2, b(r, "b").getWidth(), T, "second auto track");
        Check.near(5 * cw, b(r, "c").getWidth(), T, "max-content track: 'cc cc' in one line");
        Check.near(2 * cw, b(r, "d").getWidth(), T, "min-content track: the longest word");
        Check.near(50, b(r, "e").getWidth(), T, "fixed track");
        Check.near(5 * cw + 2 * cw, b(r, "e").getX(), T, "the third track follows");
    }

    /**
     * A flexible track is as large as its content needs: a <code>1fr</code> track with a long word grows beyond its share and the other
     * flexible tracks share the rest.
     * <p>
     * 유연한 트랙은 내용이 필요로 하는 만큼 크다: 긴 낱말이 있는 <code>1fr</code> 트랙은 자기 몫보다 커지고 다른 유연한 트랙이 나머지를 나눈다.
     */
    public void testFlexibleTrackGrowsWithContent() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:1fr 1fr'><div id=a>aaaaaaaaaaaaaaaaaaaa</div><div id=b></div></div>", 500);
        Check.near(20 * cw, b(r, "a").getWidth(), T, "the first track keeps the width of its word (120)");
        Check.near(200 - 20 * cw, b(r, "b").getWidth(), T, "the second track gets the rest");
        LayoutResult r2 = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:minmax(0,1fr) minmax(0,1fr)'><div id=a>aaaaaaaaaaaaaaaaaaaa</div><div id=b></div></div>", 500);
        Check.near(100, b(r2, "a").getWidth(), T, "minmax(0, 1fr) does not grow with the content");
    }

    /**
     * <code>minmax()</code> and <code>fit-content()</code> tracks.
     * <p>
     * <code>minmax()</code> 와 <code>fit-content()</code> 트랙.
     */
    public void testMinmaxAndFitContent() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:minmax(100px,150px) minmax(50px,1fr)'><div id=a></div><div id=b></div></div>"
                + "<div id=h style='display:grid;width:300px;grid-template-columns:fit-content(30px) 1fr'><div id=c>cc cc cc cc cc</div><div id=d></div></div>", 500);
        Check.near(150, b(r, "a").getWidth(), T, "minmax(100px,150px) grows to its maximum");
        Check.near(150, b(r, "b").getWidth(), T, "the flexible track gets the rest");
        Check.near(30, b(r, "c").getWidth(), T, "fit-content(30px) limits the max-content size");
        Check.near(270, b(r, "d").getWidth(), T, "the rest");
        Check.isTrue(14 * cw > 30 && 2 * cw < 30, "sanity: the max-content size is wider than the limit, the longest word is narrower");
    }

    /**
     * An item that spans several auto tracks makes them as wide as it needs.
     * <p>
     * 여러 auto 트랙에 걸친 항목은 그 트랙들을 필요한 만큼 넓힌다.
     */
    public void testSpanningItemGrowsTracks() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:inline-grid;grid-template-columns:auto auto'><div id=a style='grid-column-start:1;grid-column-end:3;width:100px;height:5px'></div>"
                + "<div id=b style='width:10px;height:5px'></div><div id=c style='width:10px;height:5px'></div></div>", 500);
        Check.near(100, b(r, "g").getWidth(), T, "the grid is as wide as the spanning item");
        Check.near(100, b(r, "a").getWidth(), T, "the spanning item covers both tracks");
        Check.near(0, b(r, "b").getX(), T, "b in the first track");
        Check.near(50, b(r, "c").getX(), T, "the tracks share the 80 missing pixels equally (10 + 40 each), so the second track starts at 50");
    }

    /**
     * <code>repeat(auto-fill, ...)</code> makes as many tracks as fit; <code>auto-fit</code> collapses the empty ones.
     * <p>
     * <code>repeat(auto-fill, ...)</code> 는 들어가는 만큼 트랙을 만들고 <code>auto-fit</code> 은 빈 트랙을 접는다.
     */
    public void testAutoFillAndAutoFit() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:350px;grid-template-columns:repeat(auto-fill,100px)'>" + items("a", 4, 5) + "</div>"
                + "<div id=h style='display:grid;width:350px;grid-template-columns:repeat(auto-fit,minmax(100px,1fr))'>" + items("b", 2, 5) + "</div>", 500);
        Check.near(300, b(r, "a3").getX() + b(r, "a3").getWidth(), T, "three 100 pixel tracks fit into 350");
        Check.near(0, b(r, "a4").getX(), T, "the fourth item starts the second row");
        Check.near(5, b(r, "a4").getY() - b(r, "g").getY(), T, "second row");
        Check.near(175, b(r, "b1").getWidth(), T, "auto-fit: three tracks of 116.67 collapse to two of 175 (the empty one is dropped)");
        Check.near(175, b(r, "b2").getX(), T, "second track");
    }

    /**
     * <code>fr</code> rows share a definite height; implicit rows take <code>grid-auto-rows</code>.
     * <p>
     * <code>fr</code> 행은 정해진 높이를 나누고 암시적 행은 <code>grid-auto-rows</code> 를 쓴다.
     */
    public void testRowsWithDefiniteHeightAndImplicitRows() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:100px;height:100px;grid-template-rows:1fr 3fr'><div id=a></div><div id=b></div></div>"
                + "<div id=h style='display:grid;width:100px;grid-auto-rows:30px'><div id=c></div><div id=d></div><div id=e></div></div>", 500);
        Check.near(25, b(r, "a").getHeight(), T, "1fr of 100");
        Check.near(75, b(r, "b").getHeight(), T, "3fr of 100");
        Check.near(25, b(r, "b").getY() - b(r, "g").getY(), T, "b below a");
        Check.near(30, b(r, "c").getHeight(), T, "implicit row height");
        Check.near(60, b(r, "e").getY() - b(r, "h").getY(), T, "third implicit row");
        Check.near(90, b(r, "h").getHeight(), T, "container height");
    }

    /**
     * The alignment of an item in its area: stretch (default), start, end, center, and auto margins.
     * <p>
     * 영역 안 항목의 정렬: stretch(기본), start, end, center, auto margin.
     */
    public void testItemAlignment() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;height:100px;grid-template-columns:100px 100px;grid-template-rows:50px 50px;justify-items:center;align-items:end'>"
                + "<div id=a style='width:20px;height:10px'></div><div id=b style='width:20px;height:10px;justify-self:start;align-self:start'></div>"
                + "<div id=c style='width:20px;height:10px;margin:auto'></div><div id=d style='justify-self:stretch;align-self:stretch'></div></div>", 500);
        Check.near(40, b(r, "a").getX(), T, "a centered horizontally in 100");
        Check.near(40, b(r, "a").getY(), T, "a at the end of its 50 pixel row");
        Check.near(100, b(r, "b").getX(), T, "b at the start of the second column");
        Check.near(0, b(r, "b").getY(), T, "b at the top");
        Check.near(40, b(r, "c").getX(), T, "auto margins center horizontally");
        Check.near(50 + 20, b(r, "c").getY() - b(r, "g").getY(), T, "and vertically");
        Check.near(100, b(r, "d").getWidth(), T, "stretched width");
        Check.near(50, b(r, "d").getHeight(), T, "stretched height");
    }

    /**
     * The grid in the container: <code>justify-content</code> and <code>align-content</code> with tracks that are smaller than the
     * container.
     * <p>
     * 컨테이너 안의 그리드: 컨테이너보다 작은 트랙의 <code>justify-content</code> 와 <code>align-content</code>.
     */
    public void testContentAlignment() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:50px 50px;justify-content:center'><div id=a style='height:5px'></div><div id=b style='height:5px'></div></div>"
                + "<div id=h style='display:grid;width:300px;grid-template-columns:50px 50px;justify-content:space-between'><div id=c style='height:5px'></div><div id=d style='height:5px'></div></div>"
                + "<div id=i style='display:grid;width:300px;grid-template-columns:50px 50px;justify-content:end'><div id=e style='height:5px'></div></div>", 500);
        Check.near(100, b(r, "a").getX(), T, "centered: (300 - 100) / 2");
        Check.near(150, b(r, "b").getX(), T, "second track");
        Check.near(0, b(r, "c").getX(), T, "space-between: first at the start");
        Check.near(250, b(r, "d").getX(), T, "space-between: last at the end");
        Check.near(200, b(r, "e").getX(), T, "end");
    }

    /**
     * <code>inline-grid</code> is an atomic inline box whose width is the sum of its tracks; its baseline is that of the first item.
     * <p>
     * <code>inline-grid</code> 는 원자적 인라인 상자이며 너비는 트랙의 합이고 기준선은 첫 항목의 기준선이다.
     */
    public void testInlineGrid() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=p style='line-height:10px'>ab<span id=s style='display:inline-grid;grid-template-columns:30px auto'><i id=x>xx</i><i id=y>yyy</i></span>cd</div>", 500);
        Check.near(30 + 3 * cw, b(r, "s").getWidth(), T, "30 plus the content of the auto track");
        Check.near(2 * cw, b(r, "s").getX(), T, "after the text ab");
        Check.near(30, b(r, "x").getWidth(), T, "first track");
        Check.near(2 * cw + 30, b(r, "y").getX(), T, "second track");
    }

    /**
     * The intrinsic widths: a grid in a float is as wide as its max-content tracks.
     * <p>
     * 고유 너비: float 안의 그리드는 max-content 트랙만큼 넓다.
     */
    public void testIntrinsicWidth() {
        LayoutResult r = LayoutTestSupport.layout("<div style='float:left'><div id=g style='display:grid;grid-template-columns:40px 60px;column-gap:10px'><div></div><div></div></div></div>", 500);
        Check.near(110, b(r, "g").getWidth(), T, "40 + 10 + 60");
    }

    /**
     * Absolutely positioned children are no grid items and take no cell; text runs are anonymous items.
     * <p>
     * 절대 배치 자식은 그리드 항목이 아니며 칸을 차지하지 않는다. 텍스트 덩어리는 익명 항목이다.
     */
    public void testAbsoluteChildrenAndText() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;position:relative;width:200px;grid-template-columns:100px 100px'><div id=abs style='position:absolute;width:5px;height:5px;left:3px;top:4px'></div>"
                + "text<div id=b style='height:10px'></div></div>", 500);
        Check.isFalse(b(r, "abs").isGridItem(), "an absolute child is no grid item");
        Check.near(3, b(r, "abs").getX() - b(r, "g").getX(), T, "left: 3");
        Check.near(100, b(r, "b").getX(), T, "b is in the second column (the first one holds the text)");
        Check.isTrue(b(r, "g").getHeight() >= 10, "the text item and b share the first row");
        Check.near(4 * cw, LayoutTestSupport.lineText(LayoutTestSupport.lines(b(r, "g")).get(0)).length() * cw, T, "the text item holds the line of the text");
    }

    /**
     * Grid items are painted in order-modified document order (the order of the children): <code>order</code> moves an item.
     * <p>
     * 그리드 항목은 order 를 반영한 문서 순서(자식의 순서)로 그려진다: <code>order</code> 는 항목을 옮긴다.
     */
    public void testOrderMovesItems() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:100px 100px'><div id=a style='order:2;height:5px'></div><div id=b style='height:5px'></div></div>", 500);
        Check.near(0, b(r, "b").getX(), T, "b first");
        Check.near(100, b(r, "a").getX(), T, "a second (order 2)");
        Check.same(b(r, "b"), b(r, "g").getChildren().get(0), "children in order-modified order");
    }

    /**
     * The grid shorthands place items: <code>grid-column</code>, <code>grid-row</code>, <code>grid-area</code> with names and
     * numbers, and <code>grid-template</code> with rows and columns.
     * <p>
     * 그리드 단축 속성이 항목을 놓는다: <code>grid-column</code>, <code>grid-row</code>, 이름과 숫자를 쓴 <code>grid-area</code>, 행과 열을 쓴 <code>grid-template</code>.
     */
    public void testPlacementShorthands() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template:20px 30px / 100px 100px 100px;grid-template-areas:\"nav nav nav\" \"side body body\"'>"
                + "<div id=a style='grid-column:1 / 3;grid-row:1'></div><div id=b style='grid-area:side'></div><div id=c style='grid-area:2 / 2 / 3 / 4'></div>"
                + "<div id=d style='grid-column:3;grid-row:1 / span 2'></div></div>", 500);
        Check.near(0, b(r, "a").getX(), T, "a from line 1");
        Check.near(200, b(r, "a").getWidth(), T, "to line 3");
        Check.near(20, b(r, "a").getHeight(), T, "first row");
        Check.near(0, b(r, "b").getX(), T, "side is in the first column");
        Check.near(20, b(r, "b").getY() - b(r, "g").getY(), T, "and the second row");
        Check.near(100, b(r, "c").getX(), T, "grid-area with numbers: column 2");
        Check.near(200, b(r, "c").getWidth(), T, "to column line 4");
        Check.near(200, b(r, "d").getX(), T, "d in the third column");
        Check.near(50, b(r, "d").getHeight(), T, "spans two rows");
    }

    /**
     * Many items are placed in reasonable time.
     * <p>
     * 많은 항목도 적절한 시간 안에 배치된다.
     */
    public void testManyItems() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:1000px;grid-template-columns:repeat(10,1fr)'>" + items("m", 1000, 5) + "</div>", 1200);
        Check.near(99 * 5, b(r, "m1000").getY(), T, "100 rows of 5: the last item is in row 100 (index 99)");
        Check.near(900, b(r, "m1000").getX(), T, "last column");
    }

    /**
     * An empty grid and a grid without template are one column of auto rows.
     * <p>
     * 비어 있는 그리드와 템플릿이 없는 그리드는 auto 행의 열 하나이다.
     */
    public void testEmptyAndDefaultGrid() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='display:grid'></div><div id=b style='display:grid'><div id=c style='height:10px'></div><div id=d style='height:20px'></div></div>", 300);
        Check.near(0, b(r, "a").getHeight(), T, "empty grid");
        Check.near(300, b(r, "c").getWidth(), T, "one column of the container width");
        Check.near(10, b(r, "d").getY() - b(r, "b").getY(), T, "the second item is in the second row");
        Check.near(30, b(r, "b").getHeight(), T, "rows are as high as the items");
    }
}
