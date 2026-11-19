package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of positioning (plan item P10-01): <code>position:relative</code>, <code>absolute</code>, <code>fixed</code> and
 * <code>sticky</code>, the containing block of absolutely positioned boxes (the padding box of the nearest positioned ancestor, else the
 * initial containing block), the width and height equations of CSS 2.1 10.3.7 and 10.6.4 (offsets, auto sizes, shrink-to-fit, auto
 * margins, minimum and maximum sizes), static positions, replaced elements, the flags that tell the renderer about positioned boxes and
 * the scrollable size. The expected coordinates are computed by hand from the CSS.
 * <p>
 * 위치 지정 시험(계획 P10-01): <code>position:relative</code>, <code>absolute</code>, <code>fixed</code>, <code>sticky</code>, 절대 배치 상자의 포함 블록(가장 가까운
 * 위치 지정 조상의 padding box, 없으면 초기 포함 블록), CSS 2.1 10.3.7 과 10.6.4 의 너비/높이 방정식(오프셋, auto 크기, shrink-to-fit, auto margin, 최소/최대
 * 크기), 정적 위치, 치환 요소, 렌더러에 위치 지정 상자를 알려 주는 표시, 스크롤 크기. 기대 좌표는 CSS 로 손으로 계산했다.
 */
public class PositionLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PositionLayoutTest() {
    }

    /**
     * Without a positioned ancestor the containing block is the initial containing block: offsets count from the page origin, and the box
     * takes no room in the flow.
     * <p>
     * 위치 지정 조상이 없으면 포함 블록은 초기 포함 블록이다: 오프셋은 페이지 원점에서 세고 상자는 흐름에서 공간을 차지하지 않는다.
     */
    public void testAbsoluteInInitialContainingBlock() {
        LayoutResult r = LayoutTestSupport.layout("<div id=pad style='height:40px;margin-left:15px'><div id=a style='position:absolute;left:30px;top:20px;"
                + "width:50px;height:10px;margin:3px'></div></div><div id=after style='height:5px'></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(33, a.getX(), T, "x = left + margin");
        Check.near(23, a.getY(), T, "y = top + margin");
        Check.near(50, a.getWidth(), T, "width");
        Check.near(40, LayoutTestSupport.box(r, "after").getY(), T, "the box takes no room in the flow");
        Check.isTrue(a.isAbsolutelyPositioned() && a.getPositionKind() == LayoutBox.POS_ABSOLUTE, "position flag");
        Check.isFalse(a.isFloat(), "an absolutely positioned box does not float");
    }

    /**
     * The containing block of an absolutely positioned box is the padding box of the nearest positioned ancestor.
     * <p>
     * 절대 배치 상자의 포함 블록은 가장 가까운 위치 지정 조상의 padding box 이다.
     */
    public void testContainingBlockIsPaddingBox() {
        LayoutResult r = LayoutTestSupport.layout("<div style='height:20px'></div><div id=p style='position:relative;margin:10px 0 0 40px;border:5px solid;"
                + "padding:7px;width:200px;height:100px'><div style='height:3px'></div><div id=a style='position:absolute;left:0;top:0;width:10px;height:10px'></div>"
                + "<div id=b style='position:absolute;right:0;bottom:0;width:10px;height:10px'></div></div>", 600);
        LayoutBox p = LayoutTestSupport.box(r, "p");
        LayoutBox a = LayoutTestSupport.box(r, "a");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(40, p.getX(), T, "p x");
        Check.near(30, p.getY(), T, "p y");
        Check.near(p.getX() + 5, a.getX(), T, "left:0 is the left padding edge (inside the border)");
        Check.near(p.getY() + 5, a.getY(), T, "top:0 is the top padding edge");
        double padRight = p.getX() + p.getWidth() - 5;
        double padBottom = p.getY() + p.getHeight() - 5;
        Check.near(padRight - 10, b.getX(), T, "right:0 is the right padding edge");
        Check.near(padBottom - 10, b.getY(), T, "bottom:0 is the bottom padding edge");
        Check.same(p, a.getContainingBlock(), "containing block box");
    }

    /**
     * Left and right (top and bottom) with an automatic size fill the containing block; percentages refer to the containing block;
     * <code>box-sizing</code> and margins are respected.
     * <p>
     * left 와 right(top 과 bottom)가 있고 크기가 auto 이면 포함 블록을 채운다. 백분율은 포함 블록을 참조하며 <code>box-sizing</code> 과 margin 을 따른다.
     */
    public void testStretchBetweenOffsets() {
        LayoutResult r = LayoutTestSupport.layout("<div id=p style='position:relative;width:300px;height:200px'><div id=a style='position:absolute;left:10px;right:20px;"
                + "top:5px;bottom:15px;padding:2px;margin:1px'></div><div id=b style='position:absolute;left:10%;top:25%;width:20%;height:10%'></div></div>", 600);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(268, a.getWidth(), T, "border box width = 300 - left 10 - right 20 - margins 2 = 268");
        Check.near(264, a.getContentWidth(), T, "content width");
        Check.near(178, a.getHeight(), T, "border box height = 200 - top 5 - bottom 15 - margins 2 = 178");
        Check.near(11, a.getX(), T, "x");
        Check.near(6, a.getY(), T, "y");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(30, b.getX(), T, "10 percent of 300");
        Check.near(50, b.getY(), T, "25 percent of 200");
        Check.near(60, b.getWidth(), T, "20 percent width");
        Check.near(20, b.getHeight(), T, "10 percent height");
    }

    /**
     * Auto margins center a box whose offsets and size are all given; an over-constrained box ignores the right offset.
     * <p>
     * 오프셋과 크기가 모두 주어진 상자는 auto margin 으로 가운데 정렬되고, 제약이 넘치면 오른쪽 오프셋을 무시한다.
     */
    public void testAutoMarginsCenter() {
        LayoutResult r = LayoutTestSupport.layout("<div style='position:relative;width:300px;height:100px'><div id=a style='position:absolute;left:0;right:0;"
                + "top:0;bottom:0;width:100px;height:40px;margin:auto'></div><div id=b style='position:absolute;left:10px;right:10px;width:100px;height:5px;"
                + "margin:0'></div></div>", 600);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(100, a.getX(), T, "centered horizontally");
        Check.near(30, a.getY(), T, "centered vertically");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(10, b.getX(), T, "over-constrained: left wins, right is ignored");
        Check.near(100, b.getWidth(), T, "width wins");
    }

    /**
     * A box with an automatic width and only one horizontal offset shrinks to fit its content.
     * <p>
     * 너비가 auto 이고 가로 오프셋이 하나뿐인 상자는 내용에 맞게 줄어든다.
     */
    public void testShrinkToFit() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='position:relative;width:200px;height:100px'><div id=a style='position:absolute;left:10px;top:0'>abc de</div>"
                + "<div id=b style='position:absolute;right:30px;top:20px'>abc de</div><div id=c style='position:absolute;left:150px;top:40px'>abc defghijk</div></div>", 600);
        Check.near(6 * cwd, LayoutTestSupport.box(r, "a").getWidth(), T, "max-content when there is room");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(200 - 30 - b.getWidth(), b.getX(), T, "right aligned");
        LayoutBox c = LayoutTestSupport.box(r, "c");
        Check.near(Math.min(Math.max(8 * cwd, 50), 12 * cwd), c.getWidth(), T, "limited by the available 50 px but at least the longest word");
    }

    /**
     * Without offsets the box stays where it would have been in the flow (its static position), for block-level and inline content.
     * <p>
     * 오프셋이 없으면 상자는 정상 흐름에서 있었을 자리(정적 위치)에 머문다. 블록 수준과 인라인 콘텐츠 모두 그렇다.
     */
    public void testStaticPosition() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='height:30px'></div><div id=a style='position:absolute;width:10px;height:4px'></div>"
                + "<div style='height:10px'></div><p id=p style='margin:0'>ab<span id=s style='position:absolute;width:5px;height:5px'></span>cd</p>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(0, a.getX(), T, "block static x");
        Check.near(30, a.getY(), T, "block static y is after the first block");
        LayoutBox s = LayoutTestSupport.box(r, "s");
        Check.near(2 * cwd, s.getX(), T, "inline static x is after the text before it");
        Check.near(40, s.getY(), T, "inline static y is the top of its line");
    }

    /**
     * A positioned box that is absolutely positioned inside an absolutely positioned box uses the outer box as its containing block, and
     * a box inside an inline-block that is positioned uses the inline-block.
     * <p>
     * 절대 배치 상자 안의 절대 배치 상자는 바깥 상자를 포함 블록으로 쓰고, 위치 지정된 inline-block 안의 상자는 그 inline-block 을 쓴다.
     */
    public void testNestedAbsolute() {
        LayoutResult r = LayoutTestSupport.layout("<div id=o style='position:absolute;left:100px;top:50px;width:80px;height:60px'><div id=i style='position:absolute;"
                + "right:5px;bottom:5px;width:10px;height:10px'></div></div><span id=ib style='display:inline-block;position:relative;width:40px;height:20px'>"
                + "<span id=x style='position:absolute;left:3px;top:4px;width:5px;height:5px'></span></span>", 400);
        LayoutBox i = LayoutTestSupport.box(r, "i");
        Check.near(100 + 80 - 5 - 10, i.getX(), T, "inner x");
        Check.near(50 + 60 - 5 - 10, i.getY(), T, "inner y");
        LayoutBox ib = LayoutTestSupport.box(r, "ib");
        LayoutBox x = LayoutTestSupport.box(r, "x");
        Check.near(ib.getX() + 3, x.getX(), T, "x in the inline-block");
        Check.near(ib.getY() + 4, x.getY(), T, "y in the inline-block");
    }

    /**
     * <code>position:fixed</code> boxes are positioned relative to the viewport (at scroll offset 0), whatever their ancestors are, and
     * are marked for the renderer.
     * <p>
     * <code>position:fixed</code> 상자는 조상과 상관없이 뷰포트를 기준으로(스크롤 오프셋 0 일 때) 배치되며 렌더러가 알 수 있게 표시된다.
     */
    public void testFixed() {
        LayoutResult r = LayoutTestSupport.layout("<div style='position:relative;margin:50px;height:2000px'><div id=f style='position:fixed;right:10px;bottom:20px;"
                + "width:100px;height:30px'></div></div>", 500);
        LayoutBox f = LayoutTestSupport.box(r, "f");
        Check.near(500 - 10 - 100, f.getX(), T, "fixed x");
        Check.near(600 - 20 - 30, f.getY(), T, "fixed y is relative to the viewport height");
        Check.isTrue(f.isFixedPosition() && f.isStackingContext(), "flags");
        Check.near(2100, r.getContentHeight(), T, "a fixed box does not extend the scrollable area");
    }

    /**
     * Relative positioning moves the box and its content but keeps its space; sticky is laid out like relative without an offset.
     * <p>
     * 상대 배치는 상자와 내용을 옮기지만 자리는 유지한다. sticky 는 오프셋이 없는 relative 처럼 배치된다.
     */
    public void testRelativeAndSticky() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='position:relative;left:7px;top:3px;height:10px'><div id=in style='height:4px'></div></div>"
                + "<div id=b style='height:10px'></div><div id=c style='position:sticky;top:5px;height:10px'></div>"
                + "<div id=d style='position:relative;right:5px;bottom:2px;height:10px'></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(7, a.getX(), T, "a x");
        Check.near(3, a.getY(), T, "a y");
        Check.near(3, LayoutTestSupport.box(r, "in").getY(), T, "the content moves with it");
        Check.near(10, LayoutTestSupport.box(r, "b").getY(), T, "the space of a is kept");
        Check.near(20, LayoutTestSupport.box(r, "c").getY(), T, "sticky is not shifted");
        Check.equal(LayoutBox.POS_STICKY, LayoutTestSupport.box(r, "c").getPositionKind(), "sticky flag");
        LayoutBox d = LayoutTestSupport.box(r, "d");
        Check.near(-5, d.getX(), T, "right moves to the left");
        Check.near(28, d.getY(), T, "bottom moves up");
    }

    /**
     * Minimum and maximum widths and heights limit absolutely positioned boxes.
     * <p>
     * 최소/최대 너비와 높이가 절대 배치 상자를 제한한다.
     */
    public void testMinMaxOfAbsoluteBox() {
        LayoutResult r = LayoutTestSupport.layout("<div style='position:relative;width:300px;height:200px'><div id=a style='position:absolute;left:0;right:0;max-width:120px;"
                + "height:5px'></div><div id=b style='position:absolute;left:0;top:0;width:10px;min-width:40px;height:5px'></div></div>", 600);
        Check.near(120, LayoutTestSupport.box(r, "a").getWidth(), T, "max-width");
        Check.near(40, LayoutTestSupport.box(r, "b").getWidth(), T, "min-width");
    }

    /**
     * Replaced elements keep their natural or attribute size and can be positioned; stacking flags follow <code>z-index</code>.
     * <p>
     * 치환 요소는 자연 크기나 속성 크기를 유지하며 위치를 지정할 수 있다. 쌓임 표시는 <code>z-index</code> 를 따른다.
     */
    public void testReplacedAndStackingFlags() {
        LayoutResult r = LayoutTestSupport.layout("<div style='position:relative;width:300px;height:200px'><img id=i style='position:absolute;right:0;bottom:0' width=30 height=20>"
                + "<div id=z style='position:relative;z-index:3'></div><div id=au style='position:relative'></div><div id=nz style='z-index:5'></div>"
                + "<div id=op style='opacity:0.5'></div></div>", 600);
        LayoutBox i = LayoutTestSupport.box(r, "i");
        Check.near(270, i.getX(), T, "replaced x");
        Check.near(180, i.getY(), T, "replaced y");
        Check.isTrue(LayoutTestSupport.box(r, "z").isStackingContext() && LayoutTestSupport.box(r, "z").getZIndex() == 3, "z-index 3 forms a context");
        Check.isFalse(LayoutTestSupport.box(r, "au").isStackingContext(), "auto z-index does not");
        Check.isFalse(LayoutTestSupport.box(r, "nz").isStackingContext(), "z-index without position is ignored");
        Check.isTrue(LayoutTestSupport.box(r, "op").isStackingContext(), "opacity below 1 forms a context");
        Check.isTrue(r.getRoot().isStackingContext(), "the root forms a context");
    }

    /**
     * Boxes that are far outside the viewport still have coordinates, and the scrollable area grows to the right and bottom but not to
     * the left or top.
     * <p>
     * 뷰포트에서 멀리 벗어난 상자도 좌표를 가지며 스크롤 영역은 오른쪽과 아래로만 커지고 왼쪽이나 위로는 커지지 않는다.
     */
    public void testOffscreenBoxes() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='position:absolute;left:-9999px;top:-50px;width:10px;height:10px'></div>"
                + "<div id=b style='position:absolute;left:5000px;top:3000px;width:10px;height:10px'></div>", 400);
        Check.near(-9999, LayoutTestSupport.box(r, "a").getX(), T, "negative x");
        Check.near(5010, r.getContentWidth(), T, "scrollable width");
        Check.near(3010, r.getContentHeight(), T, "scrollable height");
    }

    /**
     * An absolutely positioned box can be the first thing in an inline formatting context without giving it a line.
     * <p>
     * 절대 배치 상자는 인라인 서식 문맥의 유일한 내용이어도 줄을 만들지 않는다.
     */
    public void testAbsoluteBoxDoesNotMakeLines() {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0'><span style='position:absolute;width:5px;height:5px'></span></p><div id=n style='height:3px'></div>", 400);
        Check.near(0, LayoutTestSupport.box(r, "p").getHeight(), T, "no line box");
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        Check.equal(0, lines.size(), "no lines");
        Check.near(0, LayoutTestSupport.box(r, "n").getY(), T, "the next block follows directly");
    }

    /**
     * An inline element with <code>position:relative</code> moves its text and its fragments, not the text around it.
     * <p>
     * <code>position:relative</code> 인 인라인 요소는 자신의 텍스트와 조각을 옮기며 둘레의 텍스트는 옮기지 않는다.
     */
    public void testRelativeInlineElement() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0'>ab<span id=s style='position:relative;left:10px;top:3px'>cd</span>ef</p>", 400);
        LayoutBox p = LayoutTestSupport.box(r, "p");
        LayoutBox s = LayoutTestSupport.box(r, "s");
        Check.near(2 * cwd + 10, s.getX(), T, "the fragment of the inline element is moved");
        Check.near(3, s.getY() - p.getY(), 6, "and down by 3 (relative to its line)");
        List<LayoutBox> texts = new java.util.ArrayList<LayoutBox>();
        LayoutTestSupport.collect(p, LayoutBox.Type.TEXT, texts);
        Check.equal(3, texts.size(), "three text fragments");
        Check.near(0, texts.get(0).getX(), T, "text before is not moved");
        Check.near(2 * cwd + 10, texts.get(1).getX(), T, "text inside is moved");
        Check.near(4 * cwd, texts.get(2).getX(), T, "text after is not moved");
        Check.near(texts.get(0).getY() + 3, texts.get(1).getY(), T, "text inside is moved down");
    }

    /**
     * Content that overflows a box that clips does not enlarge the scrollable area, but a box whose containing block is outside the clip does.
     * <p>
     * 자르는 상자에서 넘치는 콘텐츠는 스크롤 영역을 키우지 않지만 포함 블록이 자르기 밖인 상자는 키운다.
     */
    public void testClippedOverflowDoesNotEnlargeScrollArea() {
        LayoutResult r = LayoutTestSupport.layout("<div style='overflow:hidden;width:50px;height:50px'><div style='width:500px;height:500px'></div></div>", 400);
        Check.near(50, r.getContentHeight(), T, "clipped in-flow overflow");
        Check.near(400, r.getContentWidth(), T, "clipped in-flow overflow (width)");
        LayoutResult a = LayoutTestSupport.layout("<div style='overflow:hidden;width:50px;height:50px'><div style='position:absolute;left:0;top:0;width:500px;height:500px'></div></div>", 400);
        Check.near(500, a.getContentHeight(), T, "an absolutely positioned box whose containing block is the initial one is not clipped");
        LayoutResult c = LayoutTestSupport.layout("<div style='position:relative;overflow:hidden;width:50px;height:50px'><div style='position:absolute;left:0;top:0;width:500px;height:500px'></div></div>", 400);
        Check.near(50, c.getContentHeight(), T, "clipped by its containing block");
    }
}
