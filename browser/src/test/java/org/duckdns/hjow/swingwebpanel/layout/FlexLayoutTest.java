package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of flex layout (plan item P10-03, CSS Flexible Box Layout Module Level 1): the main axis and the cross axis in the four
 * directions, <code>flex-grow</code>, <code>flex-shrink</code> and <code>flex-basis</code> with the freezing loop and min/max violations,
 * <code>flex-wrap</code> and <code>align-content</code>, <code>justify-content</code>, <code>align-items</code> and
 * <code>align-self</code> (stretch, start, end, center, baseline), auto margins, <code>gap</code>, <code>order</code>, anonymous flex
 * items, the automatic minimum size, nested containers, <code>inline-flex</code> and its intrinsic size, absolutely positioned children
 * and the baseline of the container. The expected coordinates are computed by hand from the specification and checked against real runs
 * of the HTML parser, the CSS engine and the layout engine.
 * <p>
 * 플렉스 레이아웃 시험(계획 P10-03, CSS Flexible Box Layout Module Level 1): 네 방향의 주축과 교차축, 고정 반복과 min/max 위반을 포함한
 * <code>flex-grow</code>, <code>flex-shrink</code>, <code>flex-basis</code>, <code>flex-wrap</code>과 <code>align-content</code>, <code>justify-content</code>,
 * <code>align-items</code>와 <code>align-self</code>(stretch, start, end, center, baseline), auto margin, <code>gap</code>, <code>order</code>, 익명 플렉스 항목, 자동
 * 최소 크기, 중첩 컨테이너, <code>inline-flex</code>와 고유 크기, 절대 배치 자식, 컨테이너의 기준선. 기대 좌표는 규격으로 손계산했으며 HTML 파서, CSS 엔진,
 * 레이아웃 엔진을 실제로 돌려 확인한다.
 */
public class FlexLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FlexLayoutTest() {
    }

    /**
     * Builds the markup of a row of items with fixed sizes: <code>&lt;div id=iN style='width:W;height:H'&gt;</code>.
     * <p>
     * 크기가 정해진 항목 행의 마크업을 만든다.
     *
     * @param prefix id prefix / id 접두어
     * @param n      number of items / 항목 수
     * @param w      width in pixels / 너비
     * @param h      height in pixels / 높이
     * @return the markup / 마크업
     */
    private static String items(String prefix, int n, int w, int h) {
        StringBuilder sb = new StringBuilder();
        // 같은 크기의 항목을 n 개 만든다.
        for (int i = 1; i <= n; i++) {
            sb.append("<div id=").append(prefix).append(i).append(" style='width:").append(w).append("px;height:").append(h).append("px'></div>");
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
     * Items of a row container are placed one after the other from the left; the container height is the tallest item; the items
     * stretch to the container height unless they have a height.
     * <p>
     * 행 컨테이너의 항목은 왼쪽부터 차례로 놓이고 컨테이너 높이는 가장 큰 항목이며 높이가 없는 항목은 컨테이너 높이로 늘어난다.
     */
    public void testRowBasics() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;width:400px'><div id=a style='width:50px;height:20px'></div>"
                + "<div id=b style='width:60px;height:30px'></div><div id=d style='width:70px'></div></div><div id=after style='height:5px'></div>", 500);
        Check.near(0, b(r, "a").getX(), T, "a x");
        Check.near(50, b(r, "b").getX(), T, "b x");
        Check.near(110, b(r, "d").getX(), T, "d x");
        Check.near(0, b(r, "a").getY(), T, "a y");
        Check.near(20, b(r, "a").getHeight(), T, "a keeps its height (it is not auto)");
        Check.near(30, b(r, "d").getHeight(), T, "an auto height item stretches to the line");
        Check.near(30, b(r, "c").getHeight(), T, "container height is the line height");
        Check.near(400, b(r, "c").getWidth(), T, "container width");
        Check.near(30, b(r, "after").getY(), T, "the next block follows the container");
        Check.isTrue(b(r, "c").isFlexContainer(), "flex container flag");
        Check.isTrue(b(r, "a").isFlexItem(), "flex item flag");
    }

    /**
     * A block-level flex container fills the width; a text run is an anonymous item; white space between items is dropped; inline
     * children are blockified (the width applies).
     * <p>
     * 블록 수준 플렉스 컨테이너는 너비를 채우고 텍스트 덩어리는 익명 항목이며 항목 사이의 공백은 버려지고 인라인 자식은 블록화되어 너비가 적용된다.
     */
    public void testAnonymousItemsAndBlockification() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex'> <span id=s style='width:40px;background:red'>x</span>\n"
                + " text <b id=bb>yy</b> <i style='display:none'>hidden</i></div>", 300);
        Check.near(300, b(r, "c").getWidth(), T, "fills the width");
        Check.near(40, b(r, "s").getWidth(), T, "the span is blockified: the width applies");
        Check.near(0, b(r, "s").getX(), T, "no white space item before the span");
        LayoutBox c = b(r, "c");
        int items = 0;
        // 컨테이너의 플렉스 항목 수를 센다.
        for (LayoutBox k : c.getChildren()) {
            if (k.isFlexItem()) {
                items++;
            }
        }
        Check.equal(3, items, "span, anonymous text item and b (white space and display:none are no items)");
        LayoutBox anon = c.getChildren().get(1);
        Check.isTrue(anon.getType() == LayoutBox.Type.ANONYMOUS_BLOCK, "the text run is an anonymous block");
        Check.near(40, anon.getX(), T, "anonymous item follows the span");
        Check.near(cw * 4, anon.getWidth(), T, "anonymous width is the width of 'text' (the white space at the edges collapses away)");
        Check.near(40 + cw * 4, b(r, "bb").getX(), T, "b follows");
    }

    /**
     * <code>flex-grow</code> shares the free space in proportion; <code>flex:1</code> items share the container equally.
     * <p>
     * <code>flex-grow</code> 는 남는 공간을 비율로 나누고 <code>flex:1</code> 항목은 컨테이너를 똑같이 나눈다.
     */
    public void testGrow() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;width:300px'><div id=a style='flex:1;height:10px'></div>"
                + "<div id=b style='flex:2;height:10px'></div></div>"
                + "<div id=c2 style='display:flex;width:300px'><div id=a2 style='width:50px;height:10px;flex-grow:1'></div>"
                + "<div id=b2 style='width:100px;height:10px;flex-grow:3'></div><div id=d2 style='width:40px;height:10px'></div></div>", 400);
        Check.near(100, b(r, "a").getWidth(), T, "flex:1 of 3 parts");
        Check.near(200, b(r, "b").getWidth(), T, "flex:2 of 3 parts");
        Check.near(100, b(r, "b").getX(), T, "b follows a");
        // 남는 공간 = 300 - 190 = 110: a 에 27.5, b 에 82.5
        Check.near(77.5, b(r, "a2").getWidth(), T, "50 + 110/4");
        Check.near(182.5, b(r, "b2").getWidth(), T, "100 + 110*3/4");
        Check.near(40, b(r, "d2").getWidth(), T, "no grow");
        Check.near(260, b(r, "d2").getX(), T, "d2 x");
    }

    /**
     * Shrinking is weighted by the flex base size; the automatic minimum size and <code>min-width</code> stop shrinking and the freezing
     * loop gives the rest to the other items.
     * <p>
     * 줄어드는 정도는 flex base size 로 가중되고 자동 최소 크기와 <code>min-width</code> 가 줄어듦을 멈추며 고정 반복이 나머지를 다른 항목에 준다.
     */
    public void testShrink() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:200px'><div id=a style='width:300px;height:10px'></div>"
                + "<div id=b style='width:100px;height:10px'></div></div>"
                + "<div style='display:flex;width:200px'><div id=a2 style='width:300px;height:10px;min-width:250px'></div>"
                + "<div id=b2 style='width:100px;height:10px'></div></div>"
                + "<div style='display:flex;width:200px'><div id=a3 style='width:300px;height:10px;flex-shrink:0'></div>"
                + "<div id=b3 style='width:100px;height:10px'></div></div>", 400);
        // 넘침 200: 가중치 300 과 100 이므로 a 는 150, b 는 50 줄어든다.
        Check.near(150, b(r, "a").getWidth(), T, "300 - 200*3/4");
        Check.near(50, b(r, "b").getWidth(), T, "100 - 200*1/4");
        // a2 는 250 아래로 못 줄어든다: 최솟값 위반으로 고정되고 나머지는 b2 가 줄인다(100 -> 0, 합 250 + 0 이 200 을 넘지만 b2 최소 0).
        Check.near(250, b(r, "a2").getWidth(), T, "min-width violation freezes a2");
        Check.near(0, b(r, "b2").getWidth(), T, "b2 shrinks to zero");
        Check.near(300, b(r, "a3").getWidth(), T, "flex-shrink:0 keeps its size");
        Check.near(0, b(r, "b3").getWidth(), T, "b3 takes all the shrinking");
    }

    /**
     * <code>max-width</code> violations freeze an item at its maximum and the other items take over the free space.
     * <p>
     * <code>max-width</code> 위반은 항목을 최댓값에 고정하고 다른 항목이 남는 공간을 이어받는다.
     */
    public void testMaxViolation() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:300px'><div id=a style='flex:1;max-width:50px;height:10px'></div>"
                + "<div id=b style='flex:1;height:10px'></div></div>", 400);
        Check.near(50, b(r, "a").getWidth(), T, "capped at max-width");
        Check.near(250, b(r, "b").getWidth(), T, "b takes the rest");
        Check.near(50, b(r, "b").getX(), T, "b x");
    }

    /**
     * <code>flex-basis</code> overrides <code>width</code>; <code>content</code> uses the max-content size; percentages refer to the
     * container.
     * <p>
     * <code>flex-basis</code> 는 <code>width</code> 를 덮어쓰고 <code>content</code> 는 max-content 크기를 쓰며 백분율은 컨테이너를 참조한다.
     */
    public void testFlexBasis() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:400px'><div id=a style='width:10px;flex-basis:100px;height:5px'></div>"
                + "<div id=b style='flex:0 0 25%;height:5px'></div><div id=c style='flex:0 0 content;height:5px'>abcd</div></div>", 500);
        Check.near(100, b(r, "a").getWidth(), T, "flex-basis wins over width");
        Check.near(100, b(r, "b").getWidth(), T, "25 percent of 400");
        Check.near(4 * cw, b(r, "c").getWidth(), T, "content basis is the max-content width");
        Check.near(200, b(r, "c").getX(), T, "c x");
    }

    /**
     * <code>justify-content</code> distributes the free space of a line.
     * <p>
     * <code>justify-content</code> 는 줄의 남는 공간을 나눈다.
     */
    public void testJustifyContent() {
        String[] modes = { "flex-start", "flex-end", "center", "space-between", "space-around", "space-evenly" };
        double[][] expected = { { 0, 50, 100 }, { 150, 200, 250 }, { 75, 125, 175 }, { 0, 125, 250 }, { 25, 125, 225 }, { 37.5, 125, 212.5 } };
        // 모드마다 새 문서를 만들어 세 항목의 x 를 확인한다.
        for (int m = 0; m < modes.length; m++) {
            LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:300px;justify-content:" + modes[m] + "'>" + items("i", 3, 50, 10)
                    + "</div>", 400);
            // 세 항목의 x 를 기대값과 비교한다.
            for (int i = 0; i < 3; i++) {
                Check.near(expected[m][i], b(r, "i" + (i + 1)).getX(), T, modes[m] + " item " + (i + 1));
            }
        }
    }

    /**
     * Negative free space: <code>space-between</code> falls back to start, <code>space-around</code> to center, and
     * <code>safe center</code> to start; plain <code>center</code> overflows on both sides.
     * <p>
     * 남는 공간이 음수일 때: <code>space-between</code> 은 start 로, <code>space-around</code> 는 center 로, <code>safe center</code> 는 start 로 대체되고 보통의
     * <code>center</code> 는 양쪽으로 넘친다.
     */
    public void testJustifyOverflow() {
        String body = "<div style='display:flex;width:100px;justify-content:%s'><div id=a style='width:80px;height:5px;flex-shrink:0'></div>"
                + "<div id=b style='width:80px;height:5px;flex-shrink:0'></div></div>";
        Check.near(0, b(LayoutTestSupport.layout(String.format(body, "space-between"), 400), "a").getX(), T, "space-between falls back to start");
        Check.near(-30, b(LayoutTestSupport.layout(String.format(body, "space-around"), 400), "a").getX(), T, "space-around falls back to center");
        Check.near(-30, b(LayoutTestSupport.layout(String.format(body, "center"), 400), "a").getX(), T, "center overflows both sides");
        Check.near(0, b(LayoutTestSupport.layout(String.format(body, "safe center"), 400), "a").getX(), T, "safe center starts at the start");
    }

    /**
     * <code>align-items</code> and <code>align-self</code> place the items across the line.
     * <p>
     * <code>align-items</code> 와 <code>align-self</code> 는 항목을 줄의 교차축에 놓는다.
     */
    public void testAlignItemsAndSelf() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;height:100px;align-items:flex-start'><div id=a style='width:10px;height:20px'></div>"
                + "<div id=b style='width:10px;height:20px;align-self:flex-end'></div><div id=c style='width:10px;height:20px;align-self:center'></div>"
                + "<div id=d style='width:10px;align-self:stretch'></div></div>"
                + "<div style='display:flex;height:100px;align-items:center'><div id=e style='width:10px;height:30px'></div></div>"
                + "<div style='display:flex;height:100px;align-items:flex-end'><div id=f style='width:10px;height:30px'></div></div>", 400);
        Check.near(0, b(r, "a").getY(), T, "flex-start");
        Check.near(80, b(r, "b").getY(), T, "flex-end");
        Check.near(40, b(r, "c").getY(), T, "center");
        Check.near(0, b(r, "d").getY(), T, "stretch y");
        Check.near(100, b(r, "d").getHeight(), T, "stretch height");
        Check.near(100 + 35, b(r, "e").getY(), T, "center in the second container");
        Check.near(200 + 70, b(r, "f").getY(), T, "flex-end in the third container");
    }

    /**
     * Auto margins absorb the free space before <code>justify-content</code> and <code>align-self</code> apply, on both axes.
     * <p>
     * auto margin 은 <code>justify-content</code> 와 <code>align-self</code> 보다 먼저 남는 공간을 차지한다(양쪽 축 모두).
     */
    public void testAutoMargins() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:300px;height:100px;justify-content:center'>"
                + "<div id=a style='width:50px;height:10px'></div><div id=b style='width:50px;height:10px;margin-left:auto'></div></div>"
                + "<div style='display:flex;width:300px;height:100px'><div id=c style='width:50px;height:10px;margin:auto'></div></div>"
                + "<div style='display:flex;width:300px;height:100px'><div id=d style='width:50px;height:10px;margin-top:auto'></div></div>", 400);
        Check.near(0, b(r, "a").getX(), T, "a at the start: the auto margin takes all the free space");
        Check.near(250, b(r, "b").getX(), T, "b pushed to the end");
        Check.near(125, b(r, "c").getX(), T, "auto margins center horizontally");
        Check.near(100 + 45, b(r, "c").getY(), T, "auto margins center vertically");
        Check.near(200 + 90, b(r, "d").getY(), T, "margin-top:auto pushes down");
    }

    /**
     * <code>gap</code> separates the items and the lines.
     * <p>
     * <code>gap</code> 은 항목 사이와 줄 사이를 띄운다.
     */
    public void testGap() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-wrap:wrap;width:230px;gap:10px 20px'>" + items("i", 4, 100, 30) + "</div>", 400);
        Check.near(0, b(r, "i1").getX(), T, "i1 x");
        Check.near(120, b(r, "i2").getX(), T, "i2 x: width 100 + column gap 20");
        Check.near(0, b(r, "i3").getX(), T, "i3 starts a new line");
        Check.near(40, b(r, "i3").getY(), T, "i3 y: height 30 + row gap 10");
        Check.near(120, b(r, "i4").getX(), T, "i4 x");
        Check.near(70, b(r, "c").getHeight(), T, "container height: two lines and a gap");
    }

    /**
     * <code>flex-wrap</code> breaks the items into lines; <code>align-content</code> distributes the lines; <code>wrap-reverse</code>
     * starts at the bottom.
     * <p>
     * <code>flex-wrap</code> 은 항목을 줄로 나누고 <code>align-content</code> 는 줄을 분배하며 <code>wrap-reverse</code> 는 아래에서 시작한다.
     */
    public void testWrapAndAlignContent() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-wrap:wrap;width:200px'>" + items("a", 5, 80, 20) + "</div>"
                + "<div id=d style='display:flex;flex-wrap:wrap;width:200px;height:120px;align-items:flex-start'>" + items("b", 5, 80, 20) + "</div>"
                + "<div id=e style='display:flex;flex-wrap:wrap;width:200px;height:120px;align-content:flex-end;align-items:flex-start'>" + items("e", 5, 80, 20) + "</div>"
                + "<div id=f style='display:flex;flex-wrap:wrap;width:200px;height:120px;align-content:space-between;align-items:flex-start'>" + items("f", 5, 80, 20) + "</div>"
                + "<div id=g style='display:flex;flex-wrap:wrap-reverse;width:200px;height:120px;align-content:flex-start;align-items:flex-start'>" + items("g", 5, 80, 20) + "</div>", 400);
        Check.near(60, b(r, "c").getHeight(), T, "three lines of 20");
        Check.near(0, b(r, "a1").getY(), T, "first line y");
        Check.near(20, b(r, "a3").getY(), T, "second line y");
        Check.near(40, b(r, "a5").getY(), T, "third line y");
        Check.near(80, b(r, "a2").getX(), T, "second item x");
        Check.near(0, b(r, "a5").getX(), T, "the fifth item starts the third line");
        // d: 높이 120 에 줄 3 개: 기본 align-content 는 stretch 이므로 줄이 40 씩.
        double dy = b(r, "d").getY();
        Check.near(dy, b(r, "b1").getY(), T, "stretched line 1");
        Check.near(dy + 40, b(r, "b3").getY(), T, "stretched line 2 starts at 40");
        Check.near(dy + 80, b(r, "b5").getY(), T, "stretched line 3 starts at 80");
        // e: flex-end 이므로 줄 묶음이 아래: 60 만큼 내려감.
        double ey = b(r, "e").getY();
        Check.near(ey + 60, b(r, "e1").getY(), T, "flex-end lines");
        // f: space-between: 남는 60 이 줄 사이 2 곳에 30 씩.
        double fy = b(r, "f").getY();
        Check.near(fy, b(r, "f1").getY(), T, "space-between first");
        Check.near(fy + 20 + 30, b(r, "f3").getY(), T, "space-between second");
        Check.near(fy + 100, b(r, "f5").getY(), T, "space-between third");
        // g: wrap-reverse: 첫 줄이 아래, flex-start 는 교차축 시작(= 아래)에 모인다.
        double gy = b(r, "g").getY();
        Check.near(gy + 100, b(r, "g1").getY(), T, "wrap-reverse first line at the bottom");
        Check.near(gy + 80, b(r, "g3").getY(), T, "wrap-reverse second line above");
        Check.near(gy + 60, b(r, "g5").getY(), T, "wrap-reverse third line above");
    }

    /**
     * A column container stacks the items vertically; its height is the sum; <code>flex-grow</code> distributes a definite height;
     * <code>align-items:stretch</code> gives the items the container width.
     * <p>
     * 열 컨테이너는 항목을 세로로 쌓고 높이는 합이며 <code>flex-grow</code> 는 정해진 높이를 나누고 <code>align-items:stretch</code> 는 항목에 컨테이너 너비를 준다.
     */
    public void testColumn() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-direction:column;width:100px'><div id=a style='height:30px'></div>"
                + "<div id=b style='height:40px;width:60px'></div></div>"
                + "<div id=d style='display:flex;flex-direction:column;width:100px;height:200px'><div id=e style='height:30px;flex-grow:1'></div>"
                + "<div id=f style='height:40px;flex-grow:3'></div></div>", 400);
        Check.near(0, b(r, "a").getY(), T, "a y");
        Check.near(30, b(r, "b").getY(), T, "b y");
        Check.near(70, b(r, "c").getHeight(), T, "container height is the sum");
        Check.near(100, b(r, "a").getWidth(), T, "stretched to the container width");
        Check.near(60, b(r, "b").getWidth(), T, "specified width is kept");
        // 남는 공간 130: e 에 32.5, f 에 97.5
        Check.near(62.5, b(r, "e").getHeight(), T, "30 + 130/4");
        Check.near(137.5, b(r, "f").getHeight(), T, "40 + 130*3/4");
        Check.near(62.5, b(r, "f").getY() - b(r, "d").getY(), T, "f follows e");
    }

    /**
     * A column container with auto height sizes the text items from their content at the container width.
     * <p>
     * 높이가 auto 인 열 컨테이너는 텍스트 항목의 높이를 컨테이너 너비에서의 내용으로 정한다.
     */
    public void testColumnTextHeight() {
        double cw = LayoutTestSupport.charWidth();
        // 항목 너비가 6 글자: 12 글자 텍스트는 두 줄이다.
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-direction:column;width:" + (cw * 6) + "px;line-height:10px'>"
                + "<div id=a>aaaaaa bbbbbb</div><div id=b>c</div></div>", 400);
        Check.near(20, b(r, "a").getHeight(), T, "two lines");
        Check.near(10, b(r, "b").getHeight(), T, "one line");
        Check.near(20, b(r, "b").getY(), T, "b below a");
        Check.near(30, b(r, "c").getHeight(), T, "container height");
    }

    /**
     * <code>row-reverse</code>, <code>column-reverse</code> and <code>direction:rtl</code> flip the main axis.
     * <p>
     * <code>row-reverse</code>, <code>column-reverse</code>, <code>direction:rtl</code> 은 주축을 뒤집는다.
     */
    public void testReverseDirections() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;flex-direction:row-reverse'>" + items("r", 3, 50, 10) + "</div>"
                + "<div style='display:flex;direction:rtl'>" + items("t", 3, 50, 10) + "</div>"
                + "<div style='display:flex;flex-direction:column-reverse;height:200px'>" + items("c", 3, 50, 10) + "</div>"
                + "<div style='display:flex;flex-direction:row-reverse;justify-content:flex-start'><div id=m style='width:50px;height:10px;margin-left:7px'></div>"
                + "<div id=n style='width:50px;height:10px'></div></div>", 300);
        Check.near(250, b(r, "r1").getX(), T, "row-reverse: the first item is at the right");
        Check.near(200, b(r, "r2").getX(), T, "row-reverse second");
        Check.near(150, b(r, "r3").getX(), T, "row-reverse third");
        Check.near(250, b(r, "t1").getX(), T, "rtl row starts at the right");
        Check.near(150, b(r, "t3").getX(), T, "rtl third");
        Check.near(190, b(r, "c1").getY() - b(r, "c1").getParent().getY(), T, "column-reverse: the first item is at the bottom");
        Check.near(170, b(r, "c3").getY() - b(r, "c3").getParent().getY(), T, "column-reverse: the third item is above");
        // margin 은 물리적 쪽을 유지한다: m 의 왼쪽 margin 7 은 m 의 왼쪽에 있다.
        Check.near(300 - 50, b(r, "m").getX(), T, "m at the right edge (its margin-left is on the left)");
        Check.near(300 - 50 - 7 - 50, b(r, "n").getX(), T, "n left of m, separated by m's left margin");
    }

    /**
     * <code>order</code> changes the layout order and the painting order but not the DOM; items with the same order keep the document
     * order.
     * <p>
     * <code>order</code> 는 레이아웃 순서와 그리기 순서를 바꾸지만 DOM 은 바꾸지 않으며 같은 order 의 항목은 문서 순서를 유지한다.
     */
    public void testOrder() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;width:300px'><div id=a style='order:2;width:50px;height:10px'></div>"
                + "<div id=b style='order:-1;width:60px;height:10px'></div><div id=d style='width:70px;height:10px'></div>"
                + "<div id=e style='order:2;width:80px;height:10px'></div></div>", 400);
        Check.near(0, b(r, "b").getX(), T, "b first (order -1)");
        Check.near(60, b(r, "d").getX(), T, "d (order 0)");
        Check.near(130, b(r, "a").getX(), T, "a (order 2, document order before e)");
        Check.near(180, b(r, "e").getX(), T, "e (order 2)");
        List<LayoutBox> kids = b(r, "c").getChildren();
        Check.same(b(r, "b"), kids.get(0), "children are in order-modified document order");
        Check.same(b(r, "e"), kids.get(3), "the last child");
    }

    /**
     * The automatic minimum size of an item is its min-content size: it does not shrink below the longest word unless
     * <code>min-width:0</code> or <code>overflow</code> is set.
     * <p>
     * 항목의 자동 최소 크기는 min-content 크기이다. <code>min-width:0</code> 이나 <code>overflow</code> 를 정하지 않으면 가장 긴 낱말보다 줄어들지 않는다.
     */
    public void testAutomaticMinimumSize() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:100px'><div id=a style='flex:1 1 200px;height:10px'>abcdefghijklmnopqrstuvwxyz0123456789</div></div>"
                + "<div style='display:flex;width:100px'><div id=b style='flex:1 1 200px;min-width:0;height:10px'>abcdefghijklmnopqrstuvwxyz0123456789</div></div>"
                + "<div style='display:flex;width:100px'><div id=c style='flex:1 1 200px;overflow:hidden;height:10px'>abcdefghijklmnopqrstuvwxyz0123456789</div></div>"
                + "<div style='display:flex;width:20px'><div id=d style='flex:0 1 200px;width:30px;height:10px'>abcdefghijklmnopqrstuvwxyz0123456789</div></div>", 400);
        Check.near(36 * cw, b(r, "a").getWidth(), T, "min-content (the long word) is the minimum");
        Check.near(100, b(r, "b").getWidth(), T, "min-width:0 shrinks to the container");
        Check.near(100, b(r, "c").getWidth(), T, "a scroll container has no automatic minimum");
        Check.near(30, b(r, "d").getWidth(), T, "the specified size caps the automatic minimum");
    }

    /**
     * <code>align-items:baseline</code> aligns the first baselines; the container baseline is that of the first item.
     * <p>
     * <code>align-items:baseline</code> 은 첫 기준선을 맞추고 컨테이너의 기준선은 첫 항목의 것이다.
     */
    public void testBaselineAlignment() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;align-items:baseline'><div id=a style='font-size:10px;line-height:10px'>aa</div>"
                + "<div id=b style='font-size:30px;line-height:30px;padding-top:5px'>bb</div></div>", 400);
        List<LayoutBox> ta = new java.util.ArrayList<LayoutBox>();
        List<LayoutBox> tb = new java.util.ArrayList<LayoutBox>();
        LayoutTestSupport.collect(b(r, "a"), LayoutBox.Type.LINE, ta);
        LayoutTestSupport.collect(b(r, "b"), LayoutBox.Type.LINE, tb);
        Check.near(ta.get(0).getBaseline(), tb.get(0).getBaseline(), T, "the baselines of both lines are on the same y");
        Check.isTrue(b(r, "b").getY() <= b(r, "a").getY() + T, "the item with the larger ascent is at the top of the line");
        Check.near(ta.get(0).getBaseline(), b(r, "c").getBaseline(), T, "the container baseline is the baseline of the aligned items");
    }

    /**
     * A nested flex container is a flex item: it takes part in the outer layout, and its own children stretch to it.
     * <p>
     * 중첩된 플렉스 컨테이너는 플렉스 항목이다. 바깥 레이아웃에 참여하고 자신의 자식은 그것에 맞춰 늘어난다.
     */
    public void testNestedContainers() {
        LayoutResult r = LayoutTestSupport.layout("<div id=o style='display:flex;width:300px;height:100px'><div id=l style='flex:1;display:flex;flex-direction:column'>"
                + "<div id=l1 style='flex:1'></div><div id=l2 style='height:30px'></div></div><div id=rr style='width:100px;display:flex'>"
                + "<div id=r1 style='flex:1'></div></div></div>", 400);
        Check.near(200, b(r, "l").getWidth(), T, "l takes the remaining width");
        Check.near(100, b(r, "l").getHeight(), T, "l stretches to the container height");
        Check.near(70, b(r, "l1").getHeight(), T, "l1 fills the column above l2");
        Check.near(30, b(r, "l2").getHeight(), T, "l2");
        Check.near(70, b(r, "l2").getY() - b(r, "l").getY(), T, "l2 y");
        Check.near(100, b(r, "r1").getWidth(), T, "r1 fills the inner row");
        Check.near(100, b(r, "r1").getHeight(), T, "r1 stretches to the inner height");
        Check.near(200, b(r, "r1").getX(), T, "r1 x");
    }

    /**
     * <code>inline-flex</code> is an atomic inline whose width is the sum of its items; it sits in a line with text and its baseline is
     * the baseline of its first item.
     * <p>
     * <code>inline-flex</code> 는 원자적 인라인이며 너비는 항목의 합이고 텍스트와 같은 줄에 놓이며 기준선은 첫 항목의 기준선이다.
     */
    public void testInlineFlex() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=p style='line-height:10px'>ab<span id=f style='display:inline-flex'><i id=x style='width:30px;height:10px'></i>"
                + "<i id=y style='width:40px;height:10px'></i></span>cd</div>", 400);
        Check.near(70, b(r, "f").getWidth(), T, "shrink-to-fit width is the sum");
        Check.near(2 * cw, b(r, "f").getX(), T, "after the text 'ab'");
        Check.near(2 * cw, b(r, "x").getX(), T, "first item");
        Check.near(2 * cw + 30, b(r, "y").getX(), T, "second item");
        List<LayoutBox> texts = new java.util.ArrayList<LayoutBox>();
        LayoutTestSupport.collect(b(r, "p"), LayoutBox.Type.TEXT, texts);
        Check.near(2 * cw + 70, texts.get(texts.size() - 1).getX(), T, "the text 'cd' follows the inline-flex box");
    }

    /**
     * The intrinsic widths of a flex container: the max-content width of a row is the sum of the items, the min-content width of a
     * wrapping row is the widest item; a column uses the widest item.
     * <p>
     * 플렉스 컨테이너의 고유 너비: 행의 max-content 는 항목의 합이고 줄바꿈하는 행의 min-content 는 가장 넓은 항목이며 열은 가장 넓은 항목을 쓴다.
     */
    public void testIntrinsicWidths() {
        LayoutResult r = LayoutTestSupport.layout("<div style='float:left;clear:left'><div id=a style='display:flex'>" + items("a", 3, 40, 5) + "</div></div>"
                + "<div style='float:left;clear:left'><div id=b style='display:flex;flex-direction:column'><div style='width:30px;height:5px'></div>"
                + "<div style='width:70px;height:5px'></div></div></div>"
                + "<div style='float:left;clear:left'><div id=c style='display:flex;column-gap:10px'>" + items("c", 2, 40, 5) + "</div></div>", 400);
        Check.near(120, b(r, "a").getWidth(), T, "row: sum of three 40 pixel items");
        Check.near(70, b(r, "b").getWidth(), T, "column: widest item");
        Check.near(90, b(r, "c").getWidth(), T, "row with a gap: 40 + 10 + 40");
    }

    /**
     * An absolutely positioned child of a flex container is not a flex item; its static position follows <code>justify-content</code>
     * and <code>align-items</code> of the container (as if it were the only item).
     * <p>
     * 플렉스 컨테이너의 절대 배치 자식은 플렉스 항목이 아니다. 정적 위치는 컨테이너의 <code>justify-content</code> 와 <code>align-items</code> 를 따른다(자식이 유일한
     * 항목인 것처럼).
     */
    public void testAbsoluteChildren() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;position:relative;width:300px;height:100px;justify-content:center;align-items:center'>"
                + "<div id=a style='position:absolute;width:50px;height:20px'></div><div id=i style='width:40px;height:10px'></div></div>"
                + "<div id=d style='display:flex;position:relative;width:300px;height:100px'><div id=e style='position:absolute;width:50px;height:20px'></div>"
                + "<div id=f style='width:40px;height:10px'></div></div>", 400);
        Check.near(125, b(r, "a").getX(), T, "centered horizontally by justify-content");
        Check.near(40, b(r, "a").getY(), T, "centered vertically by align-items");
        Check.near(130, b(r, "i").getX(), T, "the item is centered, the absolute box takes no space");
        Check.near(0, b(r, "e").getX() - b(r, "d").getX(), T, "start aligned by default");
        Check.near(0, b(r, "f").getX() - b(r, "d").getX(), T, "the item starts at the start although the absolute box comes first");
        Check.isFalse(b(r, "a").isFlexItem(), "an absolute child is no flex item");
    }

    /**
     * A row that does not wrap overflows: the scroll size covers the items outside the container and they can be hit.
     * <p>
     * 줄바꿈하지 않는 행은 넘친다: 스크롤 크기가 컨테이너 밖 항목을 포함하고 그 항목을 가리킬 수 있다.
     */
    public void testOverflowScrollSizeAndHit() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:100px'>" + "<div id=a style='width:80px;height:10px;flex-shrink:0'></div>"
                + "<div id=b style='width:80px;height:10px;flex-shrink:0'></div></div>", 120);
        Check.near(160, r.getContentWidth(), T, "scroll width covers the overflowing item");
        Check.same(b(r, "b"), r.boxAt(140, 5), "the overflowing item is hit");
    }

    /**
     * Flex items with percentage <code>height</code> resolve against a definite container height; an auto-height container leaves
     * them auto.
     * <p>
     * 백분율 <code>height</code> 의 플렉스 항목은 정해진 컨테이너 높이에 대해 풀리고 높이가 auto 인 컨테이너에서는 auto 로 남는다.
     */
    public void testPercentageHeights() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;height:200px;align-items:flex-start'><div id=a style='width:10px;height:50%'></div></div>"
                + "<div style='display:flex;align-items:flex-start;line-height:10px'><div id=b style='width:10px;height:50%'>x</div></div>", 400);
        Check.near(100, b(r, "a").getHeight(), T, "50 percent of 200");
        Check.near(10, b(r, "b").getHeight(), T, "auto height container: auto (one line of text)");
    }

    /**
     * An empty container and a container with only hidden or absolute children have no height and do not fail.
     * <p>
     * 빈 컨테이너와 숨겨진 자식이나 절대 배치 자식만 있는 컨테이너는 높이가 없고 실패하지 않는다.
     */
    public void testEmptyContainers() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='display:flex'></div><div id=b style='display:flex;position:relative'>"
                + "<div style='display:none;width:10px;height:10px'></div><div id=x style='position:absolute;width:5px;height:5px'></div></div>"
                + "<div id=c style='display:flex;flex-direction:column'></div><div id=z style='height:3px'></div>", 400);
        Check.near(0, b(r, "a").getHeight(), T, "empty row");
        Check.near(0, b(r, "b").getHeight(), T, "no in-flow children");
        Check.near(0, b(r, "c").getHeight(), T, "empty column");
        Check.near(0, b(r, "z").getY(), T, "following block");
    }

    /**
     * Many items are laid out in reasonable time.
     * <p>
     * 많은 항목도 적절한 시간 안에 배치된다.
     */
    public void testManyItems() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-wrap:wrap;width:1000px'>" + items("m", 800, 90, 10) + "</div>", 1200);
        Check.near(0, b(r, "m1").getY(), T, "first line");
        Check.near(10 * 72, b(r, "m800").getY(), T, "800 items, 11 per line: line 72 (0-based) holds the item 800");
    }
}
