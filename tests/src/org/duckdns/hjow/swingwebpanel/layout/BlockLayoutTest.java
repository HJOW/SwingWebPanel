package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of block formatting (plan item P09-02): the box model, used widths, margin collapsing, percentages, limits, anonymous boxes,
 * <code>display:none</code> and relative positioning. Coordinates are exact because the reset sheet removes all default margins.
 * <p>
 * 블록 서식 시험(계획 P09-02): 상자 모델, 사용 너비, margin 합치기, 백분율, 제한, 익명 상자, <code>display:none</code>, 상대 배치. 초기화 시트가 기본
 * margin 을 모두 없애므로 좌표는 정확하다.
 */
public class BlockLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.001;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BlockLayoutTest() {
    }

    /**
     * The border box of a block follows the box model: content plus padding plus border, positioned after its margin.
     * <p>
     * 블록의 border box 는 상자 모델을 따른다: content 에 padding 과 border 를 더하고 margin 뒤에 놓인다.
     */
    public void testBoxModel() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='width:200px;height:50px;padding:10px;border:5px solid;margin:7px'></div>", 800);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(7, a.getX(), T, "x");
        Check.near(7, a.getY(), T, "y: the margin of the first child collapses through body");
        Check.near(230, a.getWidth(), T, "border box width");
        Check.near(80, a.getHeight(), T, "border box height");
        Check.near(5, a.getBorderLeft(), T, "border");
        Check.near(10, a.getPaddingTop(), T, "padding");
        Check.near(22, a.getContentX(), T, "content x");
        Check.near(200, a.getContentWidth(), T, "content width");
        Check.near(50, a.getContentHeight(), T, "content height");
        Check.near(7, a.getMarginLeft(), T, "margin");
        Check.near(563, a.getMarginRight(), T, "over-constrained: the used right margin takes the rest (CSS 2.1 10.3.3)");
    }

    /**
     * A block without width fills its containing block; the root fills the viewport.
     * <p>
     * width 가 없는 블록은 포함 블록을 채우고 루트는 뷰포트를 채운다.
     */
    public void testAutoWidthFillsContainer() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='margin:0 10px'><div id=b style='padding:0 5px'>x</div></div>", 300);
        Check.near(300, r.getRoot().getWidth(), T, "root");
        Check.near(280, LayoutTestSupport.box(r, "a").getWidth(), T, "a");
        Check.near(10, LayoutTestSupport.box(r, "a").getX(), T, "a x");
        Check.near(280, LayoutTestSupport.box(r, "b").getWidth(), T, "b");
        Check.near(300, r.getViewportWidth(), T, "viewport width is reported");
    }

    /**
     * Adjoining vertical margins of siblings collapse to the larger one; negative margins add to the largest positive one.
     * <p>
     * 형제의 인접한 세로 margin 은 큰 쪽으로 합쳐지고 음수 margin 은 가장 큰 양수 margin 에 더해진다.
     */
    public void testSiblingMarginsCollapse() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='height:20px;margin:10px 0'></div><div id=b style='height:20px;margin:10px 0'></div>", 400);
        Check.near(10, LayoutTestSupport.box(r, "a").getY(), T, "a collapses with body");
        Check.near(40, LayoutTestSupport.box(r, "b").getY(), T, "10 between the boxes, not 20");
        Check.near(70, r.getContentHeight(), T, "the last bottom margin stays inside the root");
        LayoutResult n = LayoutTestSupport.layout("<div id=a style='height:10px;margin-bottom:20px'></div><div id=b style='height:10px;margin-top:-5px'></div>", 400);
        Check.near(25, LayoutTestSupport.box(n, "b").getY(), T, "20 plus -5");
    }

    /**
     * An empty block's own margins collapse through it and with its neighbours.
     * <p>
     * 빈 블록의 자기 margin 은 블록을 관통해 이웃과 합쳐진다.
     */
    public void testEmptyBlockCollapsesThrough() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a style='height:10px;margin-bottom:20px'></div><div style='margin:10px 0'></div><div id=b style='height:10px;margin-top:30px'></div>", 400);
        Check.near(40, LayoutTestSupport.box(r, "b").getY(), T, "the margins 20, 10, 10 and 30 collapse to 30");
    }

    /**
     * A parent's margin collapses with its first child's margin unless padding, border or a new formatting context separates them.
     * <p>
     * padding, border, 새 서식 문맥이 가르지 않으면 부모의 margin 이 첫 자식의 margin 과 합쳐진다.
     */
    public void testParentChildCollapse() {
        LayoutResult r = LayoutTestSupport.layout("<div id=p style='margin-top:10px'><div id=c style='margin-top:30px;height:5px'></div></div>", 400);
        Check.near(30, LayoutTestSupport.box(r, "p").getY(), T, "parent moves down by the collapsed margin");
        Check.near(30, LayoutTestSupport.box(r, "c").getY(), T, "child touches the parent's top");
        LayoutResult pad = LayoutTestSupport.layout("<div id=p style='margin-top:10px;padding-top:1px'><div id=c style='margin-top:30px;height:5px'></div></div>", 400);
        Check.near(10, LayoutTestSupport.box(pad, "p").getY(), T, "padding stops the collapse");
        Check.near(41, LayoutTestSupport.box(pad, "c").getY(), T, "padding plus the child's own margin");
        LayoutResult bfc = LayoutTestSupport.layout("<div id=p style='overflow:hidden'><div id=c style='margin-top:30px;height:5px'></div></div>", 400);
        Check.near(0, LayoutTestSupport.box(bfc, "p").getY(), T, "a new formatting context keeps the margin inside");
        Check.near(30, LayoutTestSupport.box(bfc, "c").getY(), T, "child inside");
        Check.near(35, LayoutTestSupport.box(bfc, "p").getHeight(), T, "parent contains the margin");
    }

    /**
     * The last child's bottom margin leaves a parent without padding, border or fixed height, but stays inside otherwise.
     * <p>
     * 마지막 자식의 아래 margin 은 padding, border, 고정 높이가 없는 부모를 벗어나고 그렇지 않으면 안에 머문다.
     */
    public void testLastChildBottomMargin() {
        LayoutResult r = LayoutTestSupport.layout("<div id=p><div style='height:10px;margin-bottom:20px'></div></div><div id=q style='height:5px'></div>", 400);
        Check.near(10, LayoutTestSupport.box(r, "p").getHeight(), T, "parent height excludes the margin");
        Check.near(30, LayoutTestSupport.box(r, "q").getY(), T, "the margin separates the next block");
        LayoutResult h = LayoutTestSupport.layout("<div id=p style='height:50px'><div style='height:10px;margin-bottom:20px'></div></div><div id=q style='height:5px'></div>", 400);
        Check.near(50, LayoutTestSupport.box(h, "q").getY(), T, "fixed height contains the child and its margin");
    }

    /**
     * Auto margins centre a block with a width; percentages refer to the containing block; min and max limits apply.
     * <p>
     * auto margin 은 너비가 있는 블록을 가운데 놓고, 백분율은 포함 블록을 기준으로 하며, 최소/최대 제한이 적용된다.
     */
    public void testAutoMarginsPercentagesAndLimits() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a style='width:100px;margin:0 auto'></div>"
                + "<div id=b style='width:50%'></div>"
                + "<div id=c style='width:500px;max-width:200px'></div>"
                + "<div id=d style='width:10px;min-width:40px'></div>"
                + "<div id=e style='margin-left:auto;width:100px'></div>"
                + "<div id=f style='width:300px;margin-right:50px'></div>"
                + "<div id=g style='padding:10%'><div id=h></div></div>", 400);
        Check.near(150, LayoutTestSupport.box(r, "a").getX(), T, "centred");
        Check.near(200, LayoutTestSupport.box(r, "b").getWidth(), T, "50% of 400");
        Check.near(200, LayoutTestSupport.box(r, "c").getWidth(), T, "max-width wins");
        Check.near(40, LayoutTestSupport.box(r, "d").getWidth(), T, "min-width wins");
        Check.near(300, LayoutTestSupport.box(r, "e").getX(), T, "one auto margin pushes the block to the right");
        Check.near(100, LayoutTestSupport.box(r, "f").getMarginRight(), T, "over-constrained: the used right margin takes the rest");
        Check.near(40, LayoutTestSupport.box(r, "g").getPaddingTop(), T, "padding percentages refer to the width of the containing block");
        Check.near(320, LayoutTestSupport.box(r, "h").getWidth(), T, "content width after the padding");
    }

    /**
     * <code>box-sizing:border-box</code> makes <code>width</code> include padding and border; percentage heights need a definite parent
     * height.
     * <p>
     * <code>box-sizing:border-box</code> 는 <code>width</code> 가 padding 과 border 를 포함하게 한다. 백분율 높이는 부모 높이가 정해져 있어야 한다.
     */
    public void testBoxSizingAndHeights() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a style='box-sizing:border-box;width:100px;padding:10px;border:5px solid;height:60px'></div>"
                + "<div id=p style='height:200px'><div id=c style='height:50%'></div></div>"
                + "<div style='height:auto'><div id=d style='height:50%;min-height:7px'></div></div>"
                + "<div id=m style='min-height:30px;max-height:20px'></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(100, a.getWidth(), T, "border box width");
        Check.near(70, a.getContentWidth(), T, "content width");
        Check.near(60, a.getHeight(), T, "border box height");
        Check.near(100, LayoutTestSupport.box(r, "c").getHeight(), T, "50% of a definite height");
        Check.near(7, LayoutTestSupport.box(r, "d").getHeight(), T, "percentage of an auto height is auto, then min-height");
        Check.near(30, LayoutTestSupport.box(r, "m").getHeight(), T, "min-height beats max-height");
    }

    /**
     * Elements with <code>display:none</code> have no box and do not take part in the layout; children of
     * <code>display:contents</code> do.
     * <p>
     * <code>display:none</code> 요소는 상자가 없고 배치에 참여하지 않는다. <code>display:contents</code> 의 자식은 참여한다.
     */
    public void testDisplayNoneAndContents() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a style='height:10px'></div><div id=n style='display:none;height:99px'><span id=s>x</span></div>"
                + "<div id=c style='display:contents'><div id=k style='height:5px'></div></div><div id=b style='height:10px'></div>", 400);
        Element n = (Element) LayoutTestSupport.box(r, "a").getNode().getNextSibling();
        Check.isTrue(r.getBoxes(n).isEmpty(), "no box for display:none");
        Check.isTrue(r.getBoxes(n.getFirstChild()).isEmpty(), "no box for its descendants");
        Check.near(10, LayoutTestSupport.box(r, "k").getY(), T, "the child of display:contents follows a");
        Check.near(15, LayoutTestSupport.box(r, "b").getY(), T, "and b follows it");
        Check.isTrue(r.getBoxes(n.getNextSibling()).isEmpty(), "display:contents makes no box of its own");
    }

    /**
     * Inline content between block children is wrapped in anonymous block boxes.
     * <p>
     * 블록 자식 사이의 인라인 콘텐츠는 익명 블록 상자로 감싼다.
     */
    public void testAnonymousBlocks() {
        LayoutResult r = LayoutTestSupport.layout("<div id=d>one<div id=c style='height:10px'></div>two</div>", 400);
        List<LayoutBox> kids = LayoutTestSupport.box(r, "d").getChildren();
        Check.equal(3, kids.size(), "children of d");
        Check.equal(LayoutBox.Type.ANONYMOUS_BLOCK, kids.get(0).getType(), "first");
        Check.equal(LayoutBox.Type.BLOCK, kids.get(1).getType(), "second");
        Check.equal(LayoutBox.Type.ANONYMOUS_BLOCK, kids.get(2).getType(), "third");
        Check.isNull(kids.get(0).getNode(), "an anonymous box has no DOM node");
        Check.near(kids.get(0).getHeight(), kids.get(1).getY() - kids.get(0).getY(), T, "stacked without gaps");
        LayoutResult w = LayoutTestSupport.layout("<div id=d>   <div id=c style='height:10px'></div>   </div>", 400);
        Check.equal(1, LayoutTestSupport.box(w, "d").getChildren().size(), "white space between blocks makes no box");
    }

    /**
     * <code>position:relative</code> moves the box visually without changing the flow.
     * <p>
     * <code>position:relative</code> 는 흐름을 바꾸지 않고 상자를 시각적으로만 옮긴다.
     */
    public void testRelativePositioning() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='height:10px;position:relative;left:15px;top:5px'><div id=in style='height:4px'></div></div><div id=b style='height:10px'></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(15, a.getX(), T, "shifted right");
        Check.near(5, a.getY(), T, "shifted down");
        Check.near(15, a.getRelativeOffsetX(), T, "offset is reported");
        Check.near(5, LayoutTestSupport.box(r, "in").getY(), T, "children move with the box");
        Check.near(10, LayoutTestSupport.box(r, "b").getY(), T, "the next sibling is not affected");
    }

    /**
     * Document sizes and hit testing: the content size covers overflowing boxes and the topmost box is found.
     * <p>
     * 문서 크기와 hit testing: 내용 크기는 넘치는 상자를 포함하고 가장 위 상자를 찾는다.
     */
    public void testContentSizeAndHitTesting() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='width:900px;height:30px'><div id=b style='height:10px;width:50px'></div></div>", 400);
        Check.near(900, r.getContentWidth(), T, "a wide box makes the document wider than the viewport");
        Check.near(30, r.getContentHeight(), T, "document height");
        Check.same(LayoutTestSupport.box(r, "b"), r.boxAt(10, 5), "deepest box at the point");
        Check.same(LayoutTestSupport.box(r, "a"), r.boxAt(100, 20), "outer box where the child is not");
        Check.isTrue(r.getBoxCount() > 3, "box count");
        Check.isNull(r.boxAt(5000, 5000), "nothing outside");
    }
}
