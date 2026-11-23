package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Edge cases of flex layout (plan item P10-03): replaced items (natural size, aspect ratio, stretch), table items, floats and
 * absolutely positioned boxes inside items, wrapping columns, <code>min-height</code> that creates free space in a column,
 * percentage margins and paddings, <code>display:contents</code> children, generated content as flex items, a floated or absolutely
 * positioned flex container (shrink-to-fit), list items, mixed text and boxes, and the robustness of deeply nested containers.
 * <p>
 * 플렉스 레이아웃의 경계 사례(계획 P10-03): 치환 항목(자연 크기, 가로세로 비율, stretch), 표 항목, 항목 안의 float 와 절대 배치 상자, 줄바꿈하는 열,
 * 열에서 남는 공간을 만드는 <code>min-height</code>, 백분율 margin 과 padding, <code>display:contents</code> 자식, 항목이 되는 생성 콘텐츠, float 나 절대 배치된
 * 플렉스 컨테이너(shrink-to-fit), 목록 항목, 텍스트와 상자가 섞인 경우, 깊이 중첩된 컨테이너의 견고성.
 */
public class FlexEdgeLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FlexEdgeLayoutTest() {
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
     * Returns a resolver that gives every image the natural size 40 by 20.
     * <p>
     * 모든 이미지에 자연 크기 40×20 을 주는 리졸버를 반환한다.
     *
     * @return the resolver / 리졸버
     */
    private static ReplacedResolver images() {
        return new ReplacedResolver() {
            /**
             * Gives the image the natural size 40 by 20.
             * <p>
             * 이미지에 자연 크기 40×20 을 준다.
             *
             * @param element the element / 요소
             * @return the content / 콘텐츠
             */
            public ReplacedContent getReplacedContent(Element element) {
                return new ReplacedContent() {
                    /**
                     * Returns 40.
                     * <p>
                     * 40 을 반환한다.
                     *
                     * @return width / 너비
                     */
                    public double getNaturalWidth() {
                        return 40;
                    }

                    /**
                     * Returns 20.
                     * <p>
                     * 20 을 반환한다.
                     *
                     * @return height / 높이
                     */
                    public double getNaturalHeight() {
                        return 20;
                    }
                };
            }
        };
    }

    /**
     * Images are flex items with their natural size; with <code>align-items:flex-start</code> the height follows the aspect ratio; a
     * stretched image takes the line height; an image that grows keeps its natural height.
     * <p>
     * 이미지는 자연 크기의 플렉스 항목이다. <code>align-items:flex-start</code> 이면 높이가 가로세로 비율을 따르고 stretch 한 이미지는 줄 높이를 얻으며 늘어나는
     * 이미지는 자연 높이를 유지한다.
     */
    public void testReplacedItems() {
        LayoutResult r = LayoutTestSupport.layout(images(), "<div style='display:flex;align-items:flex-start'><img id=a><img id=b style='width:80px'>"
                + "<img id=c style='flex-grow:1'></div><div style='display:flex;height:60px'><img id=d></div>", 300);
        Check.near(40, b(r, "a").getWidth(), T, "natural width");
        Check.near(20, b(r, "a").getHeight(), T, "natural height");
        Check.near(80, b(r, "b").getWidth(), T, "specified width");
        Check.near(40, b(r, "b").getHeight(), T, "height follows the ratio");
        Check.near(120, b(r, "c").getX(), T, "c x after a and b");
        Check.near(180, b(r, "c").getWidth(), T, "c grows to the end");
        Check.near(60, b(r, "d").getHeight(), T, "stretched to the container height");
        Check.near(40, b(r, "d").getWidth(), T, "natural width is kept");
    }

    /**
     * Floats inside an item stay inside it (the item starts a block formatting context) and give it its height; an absolutely
     * positioned descendant of an item with <code>position:relative</code> is placed against the item.
     * <p>
     * 항목 안의 float 는 항목 안에 머물고(항목은 블록 서식 문맥을 시작) 항목에 높이를 준다. <code>position:relative</code> 인 항목의 절대 배치 자손은 항목을 기준으로 놓인다.
     */
    public void testFloatsAndAbsoluteInsideItems() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;align-items:flex-start'><div id=a style='width:100px'><div style='float:left;width:30px;height:40px'></div></div>"
                + "<div id=b style='width:100px;position:relative'><div id=c style='position:absolute;left:10px;top:5px;width:5px;height:5px'></div>x</div></div>", 300);
        Check.near(40, b(r, "a").getHeight(), T, "the item contains its float");
        Check.near(110, b(r, "c").getX(), T, "the absolute box is placed against item b (x=100 plus left 10)");
        Check.near(5, b(r, "c").getY(), T, "top 5");
    }

    /**
     * A table is a flex item and keeps its own sizing rules.
     * <p>
     * 표는 플렉스 항목이며 자신의 크기 규칙을 유지한다.
     */
    public void testTableItem() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;align-items:flex-start'><table id=t><tr><td id=td>abcd</td></tr></table><div id=d style='width:10px;height:5px'></div></div>", 300);
        Check.near(4 * cw + 2, b(r, "td").getWidth(), T, "the cell fits its text (plus the 1 pixel padding of the user-agent sheet)");
        Check.near(b(r, "t").getWidth(), b(r, "d").getX(), T, "the next item follows the table");
    }

    /**
     * A wrapping column container breaks at its definite height: items continue in the next column; the lines are the columns.
     * <p>
     * 줄바꿈하는 열 컨테이너는 정해진 높이에서 끊는다. 항목은 다음 열에서 이어지고 줄이 열이다.
     */
    public void testWrappingColumn() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-direction:column;flex-wrap:wrap;height:100px;align-content:flex-start'>"
                + "<div id=a style='width:30px;height:40px'></div><div id=b style='width:50px;height:40px'></div><div id=d style='width:20px;height:40px'></div>"
                + "<div id=e style='width:20px;height:40px'></div></div>", 300);
        Check.near(0, b(r, "a").getX(), T, "a in the first column");
        Check.near(40, b(r, "b").getY(), T, "b below a");
        Check.near(50, b(r, "d").getX(), T, "d starts the second column after the widest item of the first (50)");
        Check.near(0, b(r, "d").getY(), T, "d at the top");
        Check.near(40, b(r, "e").getY(), T, "e below d");
    }

    /**
     * <code>min-height</code> of a column container with auto height gives the items free space that <code>flex-grow</code> takes.
     * <p>
     * 높이가 auto 인 열 컨테이너의 <code>min-height</code> 는 <code>flex-grow</code> 가 가져갈 남는 공간을 만든다.
     */
    public void testColumnMinHeightGrows() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='display:flex;flex-direction:column;min-height:100px'><div id=a style='height:20px'></div>"
                + "<div id=b style='height:20px;flex-grow:1'></div></div>"
                + "<div id=d style='display:flex;flex-direction:column;max-height:30px'><div id=e style='height:20px;flex-shrink:1'></div>"
                + "<div id=f style='height:20px;flex-shrink:1'></div></div>", 300);
        Check.near(100, b(r, "c").getHeight(), T, "min-height");
        Check.near(80, b(r, "b").getHeight(), T, "b takes the free space");
        Check.near(30, b(r, "d").getHeight(), T, "max-height caps the container");
        Check.near(15, b(r, "e").getHeight(), T, "items shrink equally to fit the maximum");
    }

    /**
     * Percentage margins and paddings of items refer to the width of the container.
     * <p>
     * 항목의 백분율 margin 과 padding 은 컨테이너의 너비를 참조한다.
     */
    public void testPercentageMarginsAndPaddings() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;width:200px'><div id=a style='width:50px;height:10px;margin-left:10%;padding:5%'></div></div>", 400);
        Check.near(20, b(r, "a").getX(), T, "margin-left 10 percent of 200");
        Check.near(70, b(r, "a").getWidth(), T, "border box: 50 + 2*10 (padding 5 percent of 200)");
        Check.near(30, b(r, "a").getHeight(), T, "height: 10 + 2*10");
    }

    /**
     * Children of a <code>display:contents</code> child take its place as flex items; generated content is an item.
     * <p>
     * <code>display:contents</code> 자식의 자식이 그 자리의 플렉스 항목이 되고 생성 콘텐츠도 항목이다.
     */
    public void testContentsAndPseudoItems() {
        LayoutResult r = LayoutTestSupport.layout("<style>#p::before{content:'';display:block;width:15px;height:5px;background:red} "
                + "#p::after{content:'';width:25px;height:5px}</style>"
                + "<div id=p style='display:flex'><div style='display:contents'><div id=a style='width:10px;height:5px'></div>"
                + "<div id=b style='width:20px;height:5px'></div></div></div>", 300);
        Check.near(15, b(r, "a").getX(), T, "the ::before item comes first");
        Check.near(25, b(r, "b").getX(), T, "b follows a");
        Check.near(5, b(r, "p").getHeight(), T, "container height");
    }

    /**
     * A floated flex container and an absolutely positioned one use their intrinsic width (shrink-to-fit); in a table cell it is
     * sized by the content.
     * <p>
     * float 된 플렉스 컨테이너와 절대 배치된 컨테이너는 고유 너비(shrink-to-fit)를 쓴다.
     */
    public void testShrinkToFitContainers() {
        LayoutResult r = LayoutTestSupport.layout("<div id=f style='float:left;display:flex'><div style='width:30px;height:5px'></div><div style='width:40px;height:5px'></div></div>"
                + "<div id=a style='position:absolute;left:0;top:100px;display:flex'><div style='width:20px;height:5px'></div><div style='width:25px;height:5px'></div></div>", 300);
        Check.near(70, b(r, "f").getWidth(), T, "floated container is as wide as its items");
        Check.near(45, b(r, "a").getWidth(), T, "absolute container is as wide as its items");
    }

    /**
     * List items as flex items keep their marker; a text run beside boxes is an item in order; a text-only container has one item.
     * <p>
     * 항목이 된 목록 항목은 표식을 유지하고 상자 옆의 텍스트 덩어리는 순서대로 항목이 되며 텍스트만 있는 컨테이너는 항목이 하나이다.
     */
    public void testListItemsAndText() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<ul id=u style='display:flex;list-style:none;margin:0;padding:0'><li id=l1>ab</li><li id=l2>cd</li></ul>"
                + "<div id=t style='display:flex'>just text</div>", 300);
        Check.near(2 * cw, b(r, "l1").getWidth(), T, "li width is its text");
        Check.near(2 * cw, b(r, "l2").getX(), T, "second li");
        Check.near(300, b(r, "t").getWidth(), T, "container fills");
        int items = 0;
        // 컨테이너의 플렉스 항목 수를 센다.
        for (LayoutBox k : b(r, "t").getChildren()) {
            if (k.isFlexItem()) {
                items++;
            }
        }
        Check.equal(1, items, "one anonymous item");
    }

    /**
     * Deeply nested flex containers (each a flex item of the next) are laid out in reasonable time and without errors.
     * <p>
     * 깊이 중첩된 플렉스 컨테이너(각각 다음의 플렉스 항목)는 적절한 시간 안에 오류 없이 배치된다.
     */
    public void testDeepNesting() {
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        // 컨테이너를 열두 단계 중첩한다(행과 열을 번갈아).
        for (int i = 0; i < 12; i++) {
            open.append("<div style='display:flex;").append(i % 2 == 0 ? "" : "flex-direction:column;").append("'><div style='flex:1'>");
            close.append("</div></div>");
        }
        long t0 = System.currentTimeMillis();
        LayoutResult r = LayoutTestSupport.layout(open + "<span id=x>leaf</span>" + close, 300);
        long ms = System.currentTimeMillis() - t0;
        Check.isTrue(b(r, "x").getWidth() > 0, "leaf has a width");
        Check.isTrue(ms < 20000, "nesting depth 12 took " + ms + " ms");
    }

    /**
     * Auto margins on the cross axis of a wrapped line and on a column container item center the item in its line.
     * <p>
     * 줄바꿈된 줄의 교차축 auto margin 과 열 컨테이너 항목의 auto margin 은 항목을 줄 안에서 가운데로 놓는다.
     */
    public void testCrossAutoMarginsInColumn() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;flex-direction:column;width:200px'><div id=a style='width:50px;height:10px;margin:0 auto'></div>"
                + "<div id=b style='width:50px;height:10px;margin-left:auto'></div></div>", 400);
        Check.near(75, b(r, "a").getX(), T, "centered by auto margins");
        Check.near(150, b(r, "b").getX(), T, "pushed to the right by margin-left:auto");
    }

    /**
     * <code>justify-content</code> and <code>align-items</code> with <code>start</code>, <code>end</code>, <code>left</code> and
     * <code>right</code> are relative to the writing direction, <code>flex-start</code> to the flex direction.
     * <p>
     * <code>start</code>, <code>end</code>, <code>left</code>, <code>right</code> 를 쓴 <code>justify-content</code> 와 <code>align-items</code> 는 쓰기 방향을,
     * <code>flex-start</code> 는 플렉스 방향을 기준으로 한다.
     */
    public void testLogicalAndPhysicalAlignmentKeywords() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;flex-direction:row-reverse;justify-content:flex-start'><div id=a style='width:50px;height:5px'></div></div>"
                + "<div style='display:flex;flex-direction:row-reverse;justify-content:start'><div id=b style='width:50px;height:5px'></div></div>"
                + "<div style='display:flex;flex-direction:row-reverse;justify-content:left'><div id=c style='width:50px;height:5px'></div></div>"
                + "<div style='display:flex;justify-content:right'><div id=d style='width:50px;height:5px'></div></div>"
                + "<div style='display:flex;justify-content:end'><div id=e style='width:50px;height:5px'></div></div>", 300);
        Check.near(250, b(r, "a").getX(), T, "flex-start of row-reverse is the right");
        Check.near(0, b(r, "b").getX(), T, "start is the left");
        Check.near(0, b(r, "c").getX(), T, "left is the left");
        Check.near(250, b(r, "d").getX(), T, "right");
        Check.near(250, b(r, "e").getX(), T, "end");
    }
}
