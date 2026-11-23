package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the grid track sizing algorithm and the placement details of CSS Grid Layout Level 1 (plan item P10-03): the distribution
 * of the space that spanning items need (equal share beyond the limits, flexible tracks in proportion), the automatic minimum size
 * (scroll containers), percentages of tracks that cannot be resolved, implicit tracks in front of the explicit grid, the number of
 * implicit columns that auto-placement uses, line names (numbered, implicit fallback), the validity of <code>grid-template-areas</code>,
 * stretching to the minimum height, <code>direction:rtl</code>, the static position of absolutely positioned children and the
 * interaction with multi-column containers. The expected values are computed by hand from the specification (CSS Grid 7, 8.5, 11).
 * <p>
 * 그리드 트랙 크기 결정 알고리즘과 CSS Grid Layout Level 1 의 배치 세부 규칙 시험(계획 P10-03): 여러 트랙에 걸친 항목이 필요로 하는 공간의 분배(한계를 넘는 몫은 똑같이,
 * 유연한 트랙은 비율로), 자동 최소 크기(스크롤 컨테이너), 풀 수 없는 트랙 백분율, 명시적 그리드 앞의 암시적 트랙, 자동 배치가 쓰는 암시적 열 수, 선 이름(번호, 암시적
 * 대체), <code>grid-template-areas</code> 의 유효성, 최소 높이까지 늘리기, <code>direction:rtl</code>, 절대 배치 자식의 정적 위치, 다단 컨테이너와의 상호작용.
 * 기대값은 규격(CSS Grid 7, 8.5, 11)으로 손계산했다.
 */
public class GridSizingTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public GridSizingTest() {
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
     * An item that spans two <code>auto</code> tracks needs more than they have: the extra space goes beyond the growth limits and is
     * divided equally between the tracks (CSS Grid 11.5.1), so the tracks keep their difference.
     * <p>
     * <code>auto</code> 트랙 둘에 걸친 항목이 트랙이 가진 것보다 많이 필요로 하면 모자란 공간은 성장 한계를 넘어 트랙에 똑같이 나뉜다(CSS Grid 11.5.1). 그래서 트랙의
     * 차이는 그대로 유지된다.
     */
    public void testSpanningItemSharesExtraSpaceEqually() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:auto auto'>"
                + "<div id=s style='grid-column:1 / 3;width:200px;height:5px'></div>"
                + "<div id=a style='width:100px;height:5px'></div><div id=b style='width:10px;height:5px'></div>"
                + "<div id=c style='height:5px'></div><div id=d style='height:5px'></div></div>", 500);
        // 한 칸 항목: 첫 트랙 100, 둘째 트랙 10. 두 칸 항목이 200 을 요구하므로 모자란 90 을 똑같이 45 씩 나눈다.
        Check.near(145, b(r, "c").getWidth(), T, "first track: 100 + 45");
        Check.near(55, b(r, "d").getWidth(), T, "second track: 10 + 45");
        Check.near(145, b(r, "d").getX(), T, "the second track starts behind the first");
        Check.near(200, b(r, "s").getWidth(), T, "the spanning item fills both tracks");
    }

    /**
     * An item that spans flexible tracks raises them in proportion to their flex factors (CSS Grid 11.5): <code>1fr 3fr</code> with a
     * 400 pixel wide spanning item give 100 and 300 although the container is narrower.
     * <p>
     * 유연한 트랙에 걸친 항목은 그 트랙을 계수 비율로 키운다(CSS Grid 11.5): 400 픽셀 너비의 항목이 걸친 <code>1fr 3fr</code> 은 컨테이너가 더 좁아도 100 과 300 이 된다.
     */
    public void testSpanningItemRaisesFlexibleTracksProportionally() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:1fr 3fr'>"
                + "<div id=s style='grid-column:1 / 3;width:400px;height:5px'></div><div id=a style='height:5px'></div><div id=b style='height:5px'></div></div>", 600);
        Check.near(100, b(r, "a").getWidth(), T, "1fr");
        Check.near(300, b(r, "b").getWidth(), T, "3fr");
        Check.near(100, b(r, "b").getX(), T, "second column");
    }

    /**
     * The automatic minimum size of a grid item is its min-content size unless the item is a scroll container (CSS Grid 6.6): a
     * <code>1fr</code> track grows with a long word, but not for an item with <code>overflow:hidden</code>.
     * <p>
     * 그리드 항목의 자동 최소 크기는 min-content 크기이지만 스크롤 컨테이너이면 0 이다(CSS Grid 6.6): <code>1fr</code> 트랙은 긴 낱말에 맞춰 커지지만
     * <code>overflow:hidden</code> 인 항목에는 그렇지 않다.
     */
    public void testAutomaticMinimumSize() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:1fr 1fr'><div id=a style='overflow:hidden'>aaaaaaaaaaaaaaaaaaaa</div><div id=b></div></div>"
                + "<div id=h style='display:grid;width:200px;grid-template-columns:1fr 1fr'><div id=c>aaaaaaaaaaaaaaaaaaaa</div><div id=d></div></div>", 500);
        Check.near(100, b(r, "a").getWidth(), T, "a scroll container does not stretch its track");
        Check.near(100, b(r, "b").getWidth(), T, "the other track keeps its share");
        Check.near(20 * cw, b(r, "c").getWidth(), T, "a visible item makes its track as wide as its word");
        Check.near(200 - 20 * cw, b(r, "d").getWidth(), T, "the rest");
        LayoutResult r2 = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;grid-template-columns:1fr 1fr'><div id=a style='min-width:150px'>x</div><div id=b></div></div>", 500);
        Check.near(150, b(r2, "a").getWidth(), T, "an explicit min-width is used as the minimum");
        Check.near(50, b(r2, "b").getWidth(), T, "and the other track gets the rest");
    }

    /**
     * Percentages of track sizes resolve against the container if its size is definite; else they behave like <code>auto</code>.
     * <p>
     * 트랙 크기의 백분율은 컨테이너 크기가 정해졌으면 그것에 대해 풀고, 아니면 <code>auto</code> 처럼 동작한다.
     */
    public void testPercentageTracks() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:400px;grid-template-columns:25% 75%'><div id=a style='height:5px'></div><div id=b style='height:5px'></div></div>"
                + "<div id=h style='display:inline-grid;grid-template-columns:25% 75%'><div id=c style='width:40px;height:5px'></div><div id=d style='width:60px;height:5px'></div></div>", 500);
        Check.near(100, b(r, "a").getWidth(), T, "25% of 400");
        Check.near(300, b(r, "b").getWidth(), T, "75% of 400");
        Check.near(100, b(r, "h").getWidth(), T, "indefinite container: the percentages behave like auto while the container is sized (40 + 60)");
        Check.near(25, b(r, "d").getX() - b(r, "c").getX(), T, "then they resolve against that width: 25% of 100");
    }

    /**
     * A line number beyond the explicit grid in front of it creates implicit tracks there (CSS Grid 8.5): the explicit tracks move
     * behind them and the implicit track is sized by <code>grid-auto-columns</code>.
     * <p>
     * 명시적 그리드 앞쪽 밖의 선 번호는 그곳에 암시적 트랙을 만든다(CSS Grid 8.5): 명시적 트랙은 그 뒤로 밀리고 암시적 트랙은 <code>grid-auto-columns</code> 로 크기가 정해진다.
     */
    public void testImplicitTracksBeforeTheExplicitGrid() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:50px 50px;grid-auto-columns:30px'>"
                + "<div id=a style='grid-column:-4 / span 1;height:5px'></div><div id=b style='height:5px'></div><div id=c style='height:5px'></div></div>", 500);
        Check.near(0, b(r, "a").getX(), T, "a in the implicit track in front");
        Check.near(30, b(r, "a").getWidth(), T, "sized by grid-auto-columns");
        Check.near(30, b(r, "b").getX(), T, "b in the first explicit track");
        Check.near(50, b(r, "b").getWidth(), T, "explicit track width");
        Check.near(80, b(r, "c").getX(), T, "c in the second explicit track");
        Check.near(0, b(r, "c").getY(), T, "all in the first row");
    }

    /**
     * Items with a definite column behind the explicit grid widen the implicit grid; the auto-placement cursor wraps at that width
     * (CSS Grid 8.5).
     * <p>
     * 명시적 그리드 뒤에 열이 정해진 항목이 있으면 암시적 그리드가 넓어지고 자동 배치 커서는 그 너비에서 줄을 바꾼다(CSS Grid 8.5).
     */
    public void testImplicitColumnCountFromDefiniteItems() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:400px;grid-template-columns:50px 50px;grid-auto-columns:50px'>"
                + "<div id=x style='grid-column:4;height:10px'></div><div id=a style='height:10px'></div><div id=b style='height:10px'></div>"
                + "<div id=c style='height:10px'></div><div id=d style='height:10px'></div><div id=e style='height:10px'></div></div>", 500);
        Check.near(150, b(r, "x").getX(), T, "x in the fourth column of the first row");
        Check.near(0, b(r, "x").getY(), T, "first row");
        // 희소 배치: 커서는 x 뒤(4열 다음)에 있으므로 a 는 둘째 행의 첫 칸에서 시작하고, 암시적 그리드의 열 수 4 에서 줄을 바꾼다.
        Check.near(0, b(r, "a").getX(), T, "a starts the second row");
        Check.near(10, b(r, "a").getY(), T, "second row");
        Check.near(150, b(r, "d").getX(), T, "d is in the fourth column of the second row");
        Check.near(10, b(r, "d").getY(), T, "second row");
        Check.near(0, b(r, "e").getX(), T, "e wraps to the third row");
        Check.near(20, b(r, "e").getY(), T, "third row");
    }

    /**
     * Line names: the n-th line with a name, names merged by <code>repeat()</code>, and the implicit lines behind the explicit grid that
     * count as having every name (CSS Grid 8.3).
     * <p>
     * 선 이름: 이름이 있는 n 번째 선, <code>repeat()</code> 로 합쳐진 이름, 이름이 모자랄 때 모든 이름을 가진 것으로 치는 명시적 그리드 뒤의 암시적 선(CSS Grid 8.3).
     */
    public void testLineNames() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:400px;grid-template-columns:repeat(3,[a] 50px [b])'>"
                + "<div id=x style='grid-column:a 2 / b 3;height:5px'></div></div>"
                + "<div id=h style='display:grid;width:400px;grid-template-columns:50px 50px;grid-auto-columns:40px'>"
                + "<div id=y style='grid-column-start:foo;width:10px;height:5px'></div></div>", 500);
        // [a] 50px [b a] 50px [b a] 50px [b]: a 는 선 1, 2, 3 이고 b 는 선 2, 3, 4 이다. 'a 2' 는 선 2(x=50), 'b 3' 는 선 4(x=150).
        Check.near(50, b(r, "x").getX(), T, "the second line named a");
        Check.near(100, b(r, "x").getWidth(), T, "up to the third line named b");
        // foo 라는 선이 없으므로 명시적 그리드 뒤의 첫 암시적 선(선 4)에서 시작한다. 셋째 트랙(선 3~4)은 비어 있어 크기가 0 이다.
        Check.near(140, b(r, "y").getX() - b(r, "h").getX(), T, "the first implicit line behind the two explicit tracks: 50 + 50 + the empty implicit track of 40");
    }

    /**
     * <code>grid-template-areas</code>: the areas create the explicit rows and columns; rows with different numbers of cells or areas
     * that are not rectangles make the whole declaration invalid (CSS Grid 7.3).
     * <p>
     * <code>grid-template-areas</code>: 영역이 명시적 행과 열을 만든다. 칸 수가 다른 행이나 직사각형이 아닌 영역이 있으면 선언 전체가 잘못이다(CSS Grid 7.3).
     */
    public void testTemplateAreasValidity() {
        LayoutResult ok = LayoutTestSupport.layout("<div id=g style='display:grid;width:100px;grid-template-areas:\"a b\" \"c d\";grid-auto-rows:20px'><div id=x></div><div id=y></div></div>", 500);
        Check.near(40, b(ok, "g").getHeight(), T, "two explicit rows of the areas, sized by grid-auto-rows");
        Check.near(50, b(ok, "y").getWidth(), T, "and two columns of auto size that share the width");
        LayoutResult l = LayoutTestSupport.layout("<div id=g style='display:grid;width:100px;grid-template-columns:50px 50px;grid-template-areas:\"a a\" \"a b\";grid-auto-rows:20px'><div id=x></div><div id=y></div></div>", 500);
        Check.near(20, b(l, "g").getHeight(), T, "an L-shaped area is invalid: no areas, one implicit row");
        LayoutResult u = LayoutTestSupport.layout("<div id=g style='display:grid;width:100px;grid-template-columns:50px 50px;grid-template-areas:\"a b\" \"c\";grid-auto-rows:20px'><div id=x></div><div id=y></div></div>", 500);
        Check.near(20, b(u, "g").getHeight(), T, "rows with different numbers of cells are invalid");
        LayoutResult dots = LayoutTestSupport.layout("<div id=g style='display:grid;width:100px;grid-template-areas:\"a ...\" \". b\";grid-auto-rows:20px'><div id=x style='grid-area:b'></div></div>", 500);
        Check.near(50, b(dots, "x").getX(), T, "a sequence of dots is a null cell; b is in the second column");
        Check.near(20, b(dots, "x").getY() - b(dots, "g").getY(), T, "and in the second row");
    }

    /**
     * Rows are stretched to the minimum height of the container if the height is not definite (CSS Grid 11.8).
     * <p>
     * 높이가 정해지지 않았으면 행을 컨테이너의 최소 높이까지 늘린다(CSS Grid 11.8).
     */
    public void testAutoRowsStretchToMinHeight() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;min-height:100px;grid-template-rows:auto auto'><div id=a style='height:10px'></div><div id=b style='height:10px'></div></div>", 500);
        Check.near(100, b(r, "g").getHeight(), T, "the container is as high as its minimum height");
        Check.near(50, b(r, "b").getY() - b(r, "g").getY(), T, "two auto rows share the 80 free pixels: 10 + 40 each");
    }

    /**
     * <code>direction:rtl</code> runs the columns from the right to the left; <code>justify-content</code>, <code>justify-self</code>
     * and their physical keywords follow.
     * <p>
     * <code>direction:rtl</code> 이면 열이 오른쪽에서 왼쪽으로 놓인다. <code>justify-content</code>, <code>justify-self</code> 와 물리적 키워드가 그에 따른다.
     */
    public void testRtlColumns() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;direction:rtl;width:300px;grid-template-columns:100px 100px'><div id=a style='height:5px'></div><div id=b style='height:5px'></div></div>"
                + "<div id=h style='display:grid;direction:rtl;width:300px;grid-template-columns:100px 100px;justify-content:left'><div id=c style='height:5px'></div><div id=d style='height:5px'></div></div>"
                + "<div id=i style='display:grid;direction:rtl;width:300px;grid-template-columns:100px;justify-items:start'><div id=e style='height:5px;width:20px'></div></div>"
                + "<div id=j style='display:grid;direction:rtl;width:300px;grid-template-columns:100px;justify-items:left'><div id=f style='height:5px;width:20px'></div></div>", 500);
        Check.near(200, b(r, "a").getX(), T, "the first column is at the right edge");
        Check.near(100, b(r, "b").getX(), T, "the second column to its left");
        Check.near(100, b(r, "c").getX(), T, "justify-content:left puts the tracks at the left edge: the first column is the right one of them");
        Check.near(0, b(r, "d").getX(), T, "second column at the left edge");
        Check.near(280, b(r, "e").getX() - b(r, "i").getX(), T, "start alignment is the right edge of the area in rtl: 300 - 20");
        Check.near(200, b(r, "f").getX() - b(r, "j").getX(), T, "justify-items:left is the physical left edge of the area: the area is 200..300");
    }

    /**
     * The static position of an absolutely positioned child is the content box of the container; the child is aligned in it by its own
     * <code>justify-self</code> and <code>align-self</code> (CSS Grid 9.2).
     * <p>
     * 절대 배치 자식의 정적 위치는 컨테이너의 content box 이다. 자식은 자신의 <code>justify-self</code> 와 <code>align-self</code> 로 그 안에 정렬된다(CSS Grid 9.2).
     */
    public void testStaticPositionOfAbsoluteChildren() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;position:relative;width:200px;height:100px;padding:10px'>"
                + "<div id=s style='position:absolute;width:20px;height:10px'></div>"
                + "<div id=c style='position:absolute;width:20px;height:10px;justify-self:center;align-self:center'></div>"
                + "<div id=e style='position:absolute;width:20px;height:10px;justify-self:end;align-self:end'></div></div>", 500);
        double gx = b(r, "g").getX();
        double gy = b(r, "g").getY();
        Check.near(10, b(r, "s").getX() - gx, T, "start: the content box origin x");
        Check.near(10, b(r, "s").getY() - gy, T, "start: the content box origin y");
        Check.near(10 + 90, b(r, "c").getX() - gx, T, "center: (200 - 20) / 2");
        Check.near(10 + 45, b(r, "c").getY() - gy, T, "center: (100 - 10) / 2 = 45");
        Check.near(10 + 180, b(r, "e").getX() - gx, T, "end");
        Check.near(10 + 90, b(r, "e").getY() - gy, T, "end");
    }

    /**
     * A grid container is not split between the columns of a multi-column container.
     * <p>
     * 그리드 컨테이너는 다단 컨테이너의 열 사이에서 나뉘지 않는다.
     */
    public void testGridIsNotSplitByColumns() {
        LayoutResult r = LayoutTestSupport.layout("<div style='column-count:2;column-gap:0;width:200px'>"
                + "<div id=g style='display:grid;grid-template-columns:100px'><div id=a style='height:20px'></div><div id=b style='height:20px'></div>"
                + "<div id=c style='height:20px'></div><div id=d style='height:20px'></div></div><div id=s style='height:100px'></div></div>", 500);
        Check.near(80, b(r, "g").getHeight(), T, "the grid stays in one piece");
        Check.near(b(r, "a").getX(), b(r, "d").getX(), T, "all items in the same column");
        Check.near(60, b(r, "d").getY() - b(r, "a").getY(), T, "the rows are below each other");
    }

    /**
     * An item that spans more tracks than the explicit grid has makes implicit tracks; <code>fit-content()</code> takes a percentage.
     * <p>
     * 명시적 그리드보다 많은 트랙에 걸친 항목은 암시적 트랙을 만든다. <code>fit-content()</code> 는 백분율을 받는다.
     */
    public void testSpanBeyondExplicitGridAndFitContentPercentage() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template-columns:50px 50px;grid-auto-columns:60px'><div id=a style='grid-column:span 3;height:5px'></div></div>"
                + "<div id=h style='display:grid;width:200px;grid-template-columns:fit-content(25%) 1fr'><div id=c>cc cc cc cc cc</div><div id=d style='height:5px'></div></div>"
                + "<div id=k style='display:grid;width:200px;grid-template-columns:fit-content(25%) 1fr'><div id=m style='width:120px;height:5px'></div><div id=n style='height:5px'></div></div>", 500);
        Check.near(160, b(r, "a").getWidth(), T, "50 + 50 + one implicit column of 60");
        Check.near(50, b(r, "c").getWidth(), T, "fit-content(25%) of 200 limits the max-content size of the text to 50");
        Check.near(120, b(r, "m").getWidth(), T, "fit-content() does not limit the minimum contribution of an item with a definite width");
        Check.near(80, b(r, "n").getWidth(), T, "the flexible track gets the rest");
    }

    /**
     * A grid in a vertical writing mode works in the logical axes: the columns run along the inline direction (down) and the rows
     * stack in the block direction (from the right to the left for <code>vertical-rl</code>).
     * <p>
     * 세로쓰기 모드의 그리드는 논리적 축으로 동작한다: 열은 인라인 방향(아래로)으로 놓이고 행은 블록 방향(<code>vertical-rl</code> 은 오른쪽에서 왼쪽)으로 쌓인다.
     */
    public void testVerticalWritingMode() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;writing-mode:vertical-rl;height:100px;grid-template-columns:50px 50px'>"
                + "<div id=a style='width:20px'></div><div id=b style='width:20px'></div><div id=c style='width:20px'></div><div id=d style='width:20px'></div></div>", 500);
        double gx = b(r, "g").getX();
        double gy = b(r, "g").getY();
        Check.near(40, b(r, "g").getWidth(), T, "two rows of 20 pixels stack horizontally");
        Check.near(0, b(r, "a").getY() - gy, T, "a in the first column (top)");
        Check.near(50, b(r, "b").getY() - gy, T, "b in the second column");
        Check.near(50, b(r, "a").getHeight(), T, "stretched along the inline axis");
        Check.near(20, b(r, "a").getX() - gx, T, "the first row is at the right edge");
        Check.near(0, b(r, "c").getX() - gx, T, "the second row to its left");
        Check.near(0, b(r, "c").getY() - gy, T, "c in the first column of the second row");
    }

    /**
     * Many items with spans in <code>dense</code> packing are placed in reasonable time and fill the rows without holes.
     * <p>
     * span 이 있는 많은 항목도 <code>dense</code> 배치에서 적절한 시간 안에 놓이고 구멍 없이 행을 채운다.
     */
    public void testManyItemsWithSpansDense() {
        StringBuilder sb = new StringBuilder("<div id=g style='display:grid;width:800px;grid-template-columns:repeat(8,1fr);grid-auto-flow:row dense'>");
        // 폭 2 칸과 1 칸 항목을 번갈아 1500 개 만든다.
        for (int i = 0; i < 1500; i++) {
            sb.append("<div id=m").append(i).append(" style='height:4px;").append(i % 3 == 0 ? "grid-column:span 2" : "").append("'></div>");
        }
        sb.append("</div>");
        long t0 = System.currentTimeMillis();
        LayoutResult r = LayoutTestSupport.layout(sb.toString(), 900);
        long ms = System.currentTimeMillis() - t0;
        // 3 항목마다 칸 4 개(2 + 1 + 1)를 쓰므로 1500 항목은 2000 칸이고 행은 250 개이다.
        Check.near(250 * 4, b(r, "g").getHeight(), T, "250 rows of 8 cells");
        Check.isTrue(ms < 20000, "placed in reasonable time (" + ms + " ms)");
    }

    /**
     * The <code>grid-template</code> shorthand with areas and the <code>grid</code> shorthand with <code>auto-flow</code> drive the
     * layout.
     * <p>
     * 영역을 쓴 <code>grid-template</code> 단축 속성과 <code>auto-flow</code> 를 쓴 <code>grid</code> 단축 속성이 레이아웃을 이끈다.
     */
    public void testTemplateAndGridShorthandsLayout() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:300px;grid-template:\"h h\" 20px \"s m\" 30px / 100px 200px'>"
                + "<div id=h style='grid-area:h'></div><div id=s style='grid-area:s'></div><div id=m style='grid-area:m'></div></div>"
                + "<div id=f style='display:grid;width:300px;grid:auto-flow 15px / 100px 100px'><div id=a></div><div id=b></div><div id=c></div></div>", 500);
        Check.near(300, b(r, "h").getWidth(), T, "h spans both columns");
        Check.near(20, b(r, "h").getHeight(), T, "first row 20");
        Check.near(100, b(r, "s").getWidth(), T, "s in the first column");
        Check.near(20, b(r, "s").getY() - b(r, "g").getY(), T, "second row");
        Check.near(30, b(r, "m").getHeight(), T, "second row height 30");
        Check.near(100, b(r, "m").getX(), T, "m in the second column");
        Check.near(15, b(r, "c").getY() - b(r, "f").getY(), T, "grid: auto-flow rows of 15 pixels: c is in the second row");
        Check.near(100, b(r, "b").getX(), T, "b in the second column");
        Check.near(30, b(r, "f").getHeight(), T, "two implicit rows of 15");
    }


    /**
     * Gaps, margins, padding and borders are part of the track sizes: a spanning item also needs the gaps it spans, the margin of an
     * item adds to the width of its <code>auto</code> track, and <code>auto-fill</code> counts the gaps.
     * <p>
     * gap, margin, padding, border 는 트랙 크기에 포함된다: 여러 트랙에 걸친 항목은 걸친 gap 도 필요로 하고, 항목의 margin 은 <code>auto</code> 트랙의 너비를 키우며,
     * <code>auto-fill</code> 은 gap 을 센다.
     */
    public void testGapsAndMarginsInTrackSizing() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;width:200px;column-gap:20px;grid-template-columns:auto auto'>"
                + "<div id=s style='grid-column:1 / 3;width:200px;height:5px'></div><div id=a style='width:100px;height:5px'></div><div id=b style='width:10px;height:5px'></div>"
                + "<div id=c style='height:5px'></div><div id=d style='height:5px'></div></div>"
                + "<div id=h style='display:grid;width:300px;grid-template-columns:auto auto'><div id=e style='margin:0 10px;padding:0 5px;border:2px solid;border-width:0 2px;width:20px;height:5px'></div>"
                + "<div id=f style='height:5px'></div></div>"
                + "<div id=i style='display:grid;width:350px;column-gap:10px;grid-template-columns:repeat(auto-fill,100px)'>"
                + "<div style='height:5px'></div><div style='height:5px'></div><div style='height:5px'></div><div id=j style='height:5px'></div></div>", 500);
        // 한 칸 항목 100 과 10, 두 칸 항목 200 에서 gap 20 을 빼면 70 이 모자라고 똑같이 35 씩 나눈다.
        Check.near(135, b(r, "c").getWidth(), T, "first track: 100 + 35");
        Check.near(45, b(r, "d").getWidth(), T, "second track: 10 + 35");
        Check.near(155, b(r, "d").getX(), T, "the gap of 20 lies between the tracks");
        // 항목 e 의 바깥 너비는 margin 20 + padding 10 + border 4 + width 20 = 54 이고 auto 트랙은 그만큼이 되며 남는 공간 246 을 둘이 나눠 가진다.
        Check.near(54 + 123, b(r, "f").getX(), T, "the first track holds the margin box of e plus half of the free space");
        // 350 안에 100 짜리 트랙이 gap 10 을 두고 3 개(100 + 10 + 100 + 10 + 100 = 320) 들어가고 넷째 항목은 둘째 행이다.
        Check.near(0, b(r, "j").getX(), T, "the fourth item starts the second row: three tracks fit");
        Check.near(5, b(r, "j").getY() - b(r, "i").getY(), T, "the second row is 5 below: only the column gap is set");
    }


    /**
     * The containing block of an absolutely positioned child of a positioned grid container is the grid area that its grid-placement
     * properties select; an edge without a line is the padding edge (CSS Grid 9.1).
     * <p>
     * 위치 지정된 그리드 컨테이너의 절대 배치 자식의 포함 블록은 그리드 배치 속성이 고른 그리드 영역이다. 선이 없는 가장자리는 padding 가장자리이다(CSS Grid 9.1).
     */
    public void testAbsoluteChildInGridArea() {
        LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;position:relative;width:300px;padding:10px;grid-template-columns:100px 100px 100px;grid-template-rows:40px 40px'>"
                + "<div id=a style='position:absolute;grid-column:2 / 3;grid-row:1 / 3;left:5px;top:5px;width:10px;height:10px'></div>"
                + "<div id=b style='position:absolute;grid-column-start:3;right:0;top:0;width:10px;height:10px'></div>"
                + "<div id=c style='position:absolute;grid-column-start:9;left:0;top:0;width:10px;height:10px'></div>"
                + "<div id=d style='position:absolute;grid-row:2 / 3;grid-column:span 2;left:0;bottom:0;width:10px;height:10px'></div></div>"
                + "<div id=h style='display:grid;direction:rtl;position:relative;width:300px;grid-template-columns:100px 100px 100px'>"
                + "<div id=e style='position:absolute;grid-column:1 / 2;left:0;top:0;width:10px;height:10px'></div><div style='height:5px'></div></div>", 500);
        double gx = b(r, "g").getX();
        double gy = b(r, "g").getY();
        Check.near(10 + 100 + 5, b(r, "a").getX() - gx, T, "a: left 5 inside the second column");
        Check.near(10 + 5, b(r, "a").getY() - gy, T, "a: top 5 below the top of the first row");
        // b: 3번째 열 선에서 시작하고 끝은 padding 가장자리이다(너비 10 + 10 + 300 + 10 = 320 의 오른쪽).
        Check.near(320 - 10, b(r, "b").getX() - gx, T, "b: right 0 at the padding edge");
        Check.near(0, b(r, "b").getY() - gy, T, "b: no row is set, so the top edge is the padding edge of the container");
        Check.near(0, b(r, "c").getX() - gx, T, "c: the line 9 does not exist, so the edge is the padding edge");
        // d: 행 2 는 선 2 에서 선 3(10 + 80)까지이고 열은 span 만 있으면 두 선 모두 auto 이다(padding 가장자리).
        Check.near(10 + 80 - 10, b(r, "d").getY() - gy, T, "d: bottom 0 at the end of the second row");
        Check.near(200, b(r, "e").getX() - b(r, "h").getX(), T, "rtl: the first column is the right one");
    }


    /**
     * Strange and extreme values neither throw nor make the sizes infinite or NaN: zero sized auto-fill groups, huge repetitions,
     * lines far outside the grid, empty or ragged areas, negative gaps, minmax with flexible minimum and so on.
     * <p>
     * 이상하거나 극단적인 값도 예외를 던지거나 크기를 무한대나 NaN 으로 만들지 않는다: 크기가 0 인 auto-fill 묶음, 아주 큰 반복, 그리드에서 멀리 떨어진 선, 비었거나 들쭉날쭉한
     * 영역, 음수 gap, 최소가 유연한 minmax 등.
     */
    public void testExtremeValuesAreHarmless() {
        String[] styles = {
            "grid-template-columns:repeat(auto-fill,minmax(0,0))",
            "grid-template-columns:repeat(100000,1fr)",
            "grid-template-columns:repeat(auto-fit,minmax(10px,1fr));gap:-5px",
            "grid-template-columns:minmax(1fr,2fr) minmax(10px,5px) fit-content(-3px)",
            "grid-template-areas:\"\";grid-template-columns:1fr",
            "grid-template-areas:\"a b\" \"c\" \"\"",
            "grid-template-rows:repeat(3,minmax(auto,1fr));height:0",
            "grid-auto-flow:column dense;grid-template-rows:repeat(2,10px);grid-auto-columns:0",
            "grid-template-columns:[a] 1fr [a] 1fr;justify-content:space-evenly;align-content:center",
            "grid-template-columns:calc(10px + 5%) 1fr;column-gap:50%",
            "grid-template-columns:0fr 0fr;grid-auto-rows:minmax(0,0)"
        };
        // 이상한 스타일마다 항목의 위치를 극단적인 값으로 정해 배치해 본다.
        for (int i = 0; i < styles.length; i++) {
            String item = "<div id=a style='grid-column:-99999 / 99999;grid-row:span 100000'>text</div><div id=b style='grid-column:5000;grid-row:-5000 / span 3;width:50%;height:200%'>word</div>"
                    + "<div id=c style='grid-area:x / y / z / w'>w</div><div id=d>d</div>";
            LayoutResult r = LayoutTestSupport.layout("<div id=g style='display:grid;position:relative;width:300px;" + styles[i] + "'>" + item + "</div>", 500);
            LayoutBox g = b(r, "g");
            Check.isTrue(!Double.isNaN(g.getWidth()) && !Double.isInfinite(g.getWidth()) && g.getWidth() >= 0, "finite width for " + styles[i]);
            Check.isTrue(!Double.isNaN(g.getHeight()) && !Double.isInfinite(g.getHeight()) && g.getHeight() >= 0, "finite height for " + styles[i]);
            Check.isTrue(!Double.isNaN(b(r, "d").getX()) && !Double.isInfinite(b(r, "d").getX()), "finite position of an item for " + styles[i]);
        }
    }


    /**
     * Grids work together with the other layout modes: a grid inside a flex item, a flex container and a table inside grid items, a
     * grid inside a table cell and a grid inside a grid item.
     * <p>
     * 그리드는 다른 레이아웃 모드와 함께 동작한다: 플렉스 항목 안의 그리드, 그리드 항목 안의 플렉스 컨테이너와 표, 표 셀 안의 그리드, 그리드 항목 안의 그리드.
     */
    public void testCombinedWithFlexTableAndNestedGrids() {
        LayoutResult r = LayoutTestSupport.layout("<div id=f style='display:flex;width:400px'>"
                + "<div id=g1 style='display:grid;grid-template-columns:1fr 1fr;flex:1'><div id=a style='height:10px'></div><div id=b style='height:10px'></div></div>"
                + "<div id=g2 style='flex:none;width:100px;display:grid;grid-template-columns:repeat(2,50px)'>"
                + "<div id=n style='display:flex;justify-content:flex-end'><div id=nn style='width:10px;height:5px'></div></div>"
                + "<div id=t><table id=tb style='width:100%'><tr><td id=td>x</td></tr></table></div></div></div>"
                + "<table id=outer style='width:300px'><tr><td id=cell><div id=gc style='display:grid;grid-template-columns:30px 1fr;gap:10px'><div id=c1 style='height:5px'></div><div id=c2 style='height:5px'></div></div></td></tr></table>"
                + "<div id=o style='display:grid;width:200px;grid-template-columns:1fr 3fr'><div id=i style='display:grid;grid-template-columns:1fr 1fr;gap:4px'>"
                + "<div id=i1 style='height:5px'></div><div id=i2 style='height:5px'></div></div><div id=j style='height:5px'></div></div>", 600);
        Check.near(300, b(r, "g1").getWidth(), T, "the grid grows in the flex container");
        Check.near(150, b(r, "a").getWidth(), T, "two fr columns of the grid in the flex item");
        Check.near(150, b(r, "b").getX(), T, "second column");
        Check.near(300, b(r, "g2").getX(), T, "the second flex item follows");
        Check.near(340, b(r, "nn").getX(), T, "a flex container in a grid item: flex-end of the 50 pixel column at 300");
        Check.near(50, b(r, "tb").getWidth(), T, "a table with width 100% in a grid item fills the column");
        Check.near(350, b(r, "tb").getX(), T, "in the second column");
        double cx = b(r, "cell").getX();
        Check.near(30, b(r, "c1").getWidth(), T, "a grid in a table cell: the fixed column");
        Check.near(1 + 30 + 10, b(r, "c2").getX() - cx, T, "after the 1 pixel padding of the cell, the fixed column and the gap");
        Check.isTrue(b(r, "c2").getWidth() > 200, "the fr column takes the rest of the cell");
        Check.near(50, b(r, "i").getWidth(), T, "a grid in a grid item takes the width of the 1fr column of 200");
        Check.near(23, b(r, "i1").getWidth(), T, "and shares it between two columns with a gap of 4: (50 - 4) / 2");
        Check.near(23 + 4, b(r, "i2").getX() - b(r, "o").getX(), T, "the second inner column: 23 and the gap of 4");
        Check.near(150, b(r, "j").getWidth(), T, "3fr");
    }

}
