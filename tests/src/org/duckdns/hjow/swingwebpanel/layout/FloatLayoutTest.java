package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of floats and <code>clear</code> (plan item P10-01): the placement rules of CSS 2.1 9.5.1 (left and right floats, floats that do
 * not fit move down, a float never goes above an earlier float), lines that are shortened beside floats and move down when nothing fits,
 * the height of a block formatting context root that contains floats, <code>clear</code>, boxes that start a block formatting context
 * beside floats, floats in inline content and the estimate of the position of nested blocks (a second layout when the margins of the first
 * children collapse differently than expected). The expected coordinates are computed by hand from the CSS and checked against real runs
 * of the HTML parser, the CSS engine and the layout engine.
 * <p>
 * float 와 <code>clear</code> 시험(계획 P10-01): CSS 2.1 9.5.1 의 배치 규칙(왼쪽/오른쪽 float, 들어가지 않는 float 는 아래로, float 는 앞선 float 보다 위로
 * 가지 않음), float 옆에서 짧아지고 들어갈 곳이 없으면 내려가는 줄, float 를 포함한 블록 서식 문맥 루트의 높이, <code>clear</code>, float 옆에서 블록 서식 문맥을
 * 시작하는 상자, 인라인 콘텐츠 안의 float, 중첩 블록 위치의 추정(첫 자식들의 margin 이 예상과 다르게 합쳐지면 한 번 더 배치). 기대 좌표는 CSS 로
 * 손으로 계산했으며 HTML 파서, CSS 엔진, 레이아웃 엔진을 실제로 돌려 확인한다.
 */
public class FloatLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /** Sheet that gives every line a height of 10 pixels. 모든 줄의 높이를 10 픽셀로 하는 시트. */
    private static final String LH = "<style>body{line-height:10px}</style>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FloatLayoutTest() {
    }

    /**
     * A left float is placed at the left edge of its containing block; a right float at the right edge; neither takes room in the
     * height of the parent.
     * <p>
     * 왼쪽 float 는 포함 블록의 왼쪽 가장자리에, 오른쪽 float 는 오른쪽 가장자리에 놓이며 어느 쪽도 부모의 높이를 차지하지 않는다.
     */
    public void testLeftAndRightFloats() {
        LayoutResult r = LayoutTestSupport.layout("<div id=o><div id=l style='float:left;width:50px;height:30px'></div>"
                + "<div id=r style='float:right;width:40px;height:20px;margin:0 5px'></div></div><div id=after style='height:7px'></div>", 400);
        LayoutBox l = LayoutTestSupport.box(r, "l");
        LayoutBox rr = LayoutTestSupport.box(r, "r");
        Check.near(0, l.getX(), T, "left float x");
        Check.near(0, l.getY(), T, "left float y");
        Check.near(50, l.getWidth(), T, "left float width");
        Check.near(400 - 40 - 5, rr.getX(), T, "right float x (the margin is inside the container)");
        Check.near(0, rr.getY(), T, "right float y");
        Check.isTrue(l.isFloat() && l.getFloatSide() == LayoutBox.FLOAT_LEFT, "left float flag");
        Check.isTrue(rr.getFloatSide() == LayoutBox.FLOAT_RIGHT, "right float flag");
        Check.near(0, LayoutTestSupport.box(r, "o").getHeight(), T, "a float does not give its parent a height");
        Check.near(0, LayoutTestSupport.box(r, "after").getY(), T, "following blocks ignore the float");
    }

    /**
     * An auto width float shrinks to fit its content; a percentage width refers to the containing block.
     * <p>
     * 너비가 auto 인 float 는 내용에 맞게 줄어들고 백분율 너비는 포함 블록을 참조한다.
     */
    public void testFloatWidths() {
        double cwd = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='float:left'>abcd</div><div id=b style='float:left;width:25%;height:5px'></div>", 400);
        Check.near(4 * cwd, LayoutTestSupport.box(r, "a").getWidth(), T, "shrink-to-fit");
        Check.near(100, LayoutTestSupport.box(r, "b").getWidth(), T, "25 percent of 400");
        Check.near(4 * cwd, LayoutTestSupport.box(r, "b").getX(), T, "the second float sits right of the first");
    }

    /**
     * The third 150 pixel float of a 400 pixel container does not fit beside the first two and goes below the lowest float that is in
     * its way (the right-hand one, which an earlier float may not be passed by); the next float is never above it.
     * <p>
     * 400 픽셀 컨테이너의 세 번째 150 픽셀 float 는 앞의 둘 옆에 들어가지 않으며 방해가 되는 가장 낮은 float(앞선 float 를 앞지를 수 없다) 아래로 간다. 그다음
     * float 는 그보다 위에 놓이지 않는다.
     */
    public void testThirdFloatMovesBelowShortest() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='float:left;width:150px;height:30px'></div>"
                + "<div id=b style='float:left;width:150px;height:50px'></div><div id=c style='float:left;width:150px;height:10px'></div>"
                + "<div id=d style='float:left;width:10px;height:10px'></div>", 400);
        LayoutBox c = LayoutTestSupport.box(r, "c");
        Check.near(0, LayoutTestSupport.box(r, "a").getX(), T, "a is at the left edge");
        Check.near(150, LayoutTestSupport.box(r, "b").getX(), T, "b is beside a");
        Check.near(50, c.getY(), T, "c does not fit beside a and b (100 left of 150) and may not go left of the earlier float b: below b (50)");
        Check.near(0, c.getX(), T, "c is at the left edge below b");
        LayoutBox d = LayoutTestSupport.box(r, "d");
        Check.near(50, d.getY(), T, "d is not above the earlier float c");
        Check.near(150, d.getX(), T, "d is beside c");
    }

    /**
     * Lines beside a float are shortened and start right of a left float; below the float they use the full width again.
     * <p>
     * float 옆의 줄은 짧아지고 왼쪽 float 의 오른쪽에서 시작하며 float 아래에서는 다시 전체 너비를 쓴다.
     */
    public void testLinesBesideFloat() {
        double cwd = LayoutTestSupport.charWidth();
        // 한 줄에 10 글자가 들어가는 너비의 문단 옆에 높이 20 의 float 를 둔다: 처음 두 줄은 float 옆, 다음 줄은 처음부터 시작한다.
        double width = 100 + 10 * cwd;
        LayoutResult r = LayoutTestSupport.layout(LH + "<div style='float:left;width:100px;height:20px'></div>"
                + "<p id=p style='margin:0'>aaaaaaaaa bbbbbbbbb ccccccccc</p>", width);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        Check.equal(3, lines.size(), "three lines of nine letters");
        Check.near(100, lines.get(0).getX(), T, "line 1 starts right of the float");
        Check.near(10 * cwd, lines.get(0).getWidth(), T, "line 1 is shortened");
        Check.near(100, lines.get(1).getX(), T, "line 2 starts right of the float");
        Check.near(0, lines.get(2).getX(), T, "line 3 is below the float (20 px) and uses the full width");
        Check.near(width, lines.get(2).getWidth(), T, "line 3 width");
        Check.near(20, lines.get(2).getY(), T, "line 3 y");
        Check.contains(LayoutTestSupport.lineText(lines.get(0)), "aaaaaaaaa", "text of line 1");
    }

    /**
     * A line that cannot hold its first word beside the float moves down below the float.
     * <p>
     * float 옆에 첫 낱말이 들어가지 않는 줄은 float 아래로 내려간다.
     */
    public void testLineMovesBelowFloatWhenNothingFits() {
        double cwd = LayoutTestSupport.charWidth();
        double width = 100 + 5 * cwd;
        LayoutResult r = LayoutTestSupport.layout(LH + "<div style='float:left;width:100px;height:25px'></div>"
                + "<p id=p style='margin:0'>aaaaaaaa bb</p>", width);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        Check.equal(1, lines.size(), "one line: the words fit together below the float");
        Check.near(25, lines.get(0).getY(), T, "the first line moved below the float");
        Check.near(0, lines.get(0).getX(), T, "x below the float");
    }

    /**
     * A right float shortens the lines on its side only.
     * <p>
     * 오른쪽 float 는 자기 쪽의 줄만 짧게 한다.
     */
    public void testRightFloatShortensLine() {
        double cwd = LayoutTestSupport.charWidth();
        double width = 60 + 10 * cwd;
        LayoutResult r = LayoutTestSupport.layout(LH + "<div style='float:right;width:60px;height:10px'></div>"
                + "<p id=p style='margin:0'>aaaaaaaaa bbbbbbbbb</p>", width);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        Check.equal(2, lines.size(), "two lines");
        Check.near(0, lines.get(0).getX(), T, "line 1 starts at the left edge");
        Check.near(10 * cwd, lines.get(0).getWidth(), T, "line 1 is shortened on the right");
        Check.near(width, lines.get(1).getWidth(), T, "line 2 is below the float");
    }

    /**
     * A block formatting context root (<code>overflow:hidden</code>, a float, an inline block) contains its floats; an ordinary block
     * does not.
     * <p>
     * 블록 서식 문맥 루트(<code>overflow:hidden</code>, float, inline-block)는 안쪽 float 를 포함하고 보통 블록은 포함하지 않는다.
     */
    public void testBfcRootContainsFloats() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a><div style='float:left;width:10px;height:40px;margin-bottom:5px'></div></div>"
                + "<div id=b style='overflow:hidden'><div style='float:left;width:10px;height:40px;margin-bottom:5px'></div></div>"
                + "<div id=c style='float:left'><div style='float:left;width:10px;height:40px'></div></div>"
                + "<div id=d style='display:inline-block'><div style='float:left;width:10px;height:30px'></div></div>", 400);
        Check.near(0, LayoutTestSupport.box(r, "a").getHeight(), T, "ordinary block");
        Check.near(45, LayoutTestSupport.box(r, "b").getHeight(), T, "overflow:hidden includes the bottom margin edge of the float");
        Check.near(40, LayoutTestSupport.box(r, "c").getHeight(), T, "float");
        Check.near(30, LayoutTestSupport.box(r, "d").getHeight(), T, "inline-block");
    }

    /**
     * <code>clear</code> moves a block below the floats of the chosen side; a margin of the cleared block does not collapse with the
     * ones before it.
     * <p>
     * <code>clear</code> 는 블록을 선택한 쪽 float 아래로 옮기며 clear 된 블록의 margin 은 앞의 margin 과 합쳐지지 않는다.
     */
    public void testClear() {
        LayoutResult r = LayoutTestSupport.layout("<div style='float:left;width:30px;height:40px'></div>"
                + "<div style='float:right;width:30px;height:70px'></div>"
                + "<div id=none style='height:5px'></div><div id=l style='clear:left;height:5px'></div>"
                + "<div id=r style='clear:right;height:5px'></div><div id=both style='clear:both;height:5px;margin-top:3px'></div>", 400);
        Check.near(0, LayoutTestSupport.box(r, "none").getY(), T, "no clear");
        Check.near(40, LayoutTestSupport.box(r, "l").getY(), T, "clear:left goes below the left float");
        Check.near(70, LayoutTestSupport.box(r, "r").getY(), T, "clear:right goes below the right float");
        Check.near(75 + 3, LayoutTestSupport.box(r, "both").getY(), T, "clear both: the box before ends at 75, which is below both floats, so only the 3px margin applies");
    }

    /**
     * The first child of a block with a floating sibling keeps its position relative to the margin collapse: a cleared box that is the
     * first child does not collapse its margin with its parent.
     * <p>
     * float 형제가 있는 블록의 첫 자식은 margin 합치기에서 위치를 유지한다: 첫 자식이 clear 되면 부모와 margin 이 합쳐지지 않는다.
     */
    public void testClearedFirstChild() {
        LayoutResult r = LayoutTestSupport.layout("<div style='float:left;width:30px;height:40px'></div>"
                + "<div id=p><div id=c style='clear:left;height:5px;margin-top:10px'></div></div>", 400);
        Check.near(40, LayoutTestSupport.box(r, "c").getY(), T, "the cleared child starts below the float");
        Check.near(0, LayoutTestSupport.box(r, "p").getY(), T, "the parent stays: the margin of a cleared child does not collapse with it");
        Check.near(45, LayoutTestSupport.box(r, "p").getHeight(), T, "the parent contains the clearance and the child");
    }

    /**
     * A box that starts a block formatting context is narrowed to the space beside a float when it fits there, and moved below the
     * float when it does not.
     * <p>
     * 블록 서식 문맥을 시작하는 상자는 float 옆 공간에 들어가면 그만큼 좁아지고 들어가지 않으면 float 아래로 옮겨진다.
     */
    public void testBfcBesideFloat() {
        LayoutResult r = LayoutTestSupport.layout("<div style='float:left;width:100px;height:30px'></div>"
                + "<div id=o style='overflow:hidden;height:10px'></div>", 400);
        LayoutBox o = LayoutTestSupport.box(r, "o");
        Check.near(100, o.getX(), T, "starts at the right edge of the float");
        Check.near(300, o.getWidth(), T, "fills the rest");
        Check.near(0, o.getY(), T, "beside the float, not below it");
        LayoutResult w = LayoutTestSupport.layout("<div style='float:left;width:100px;height:30px'></div>"
                + "<div id=o style='overflow:hidden;width:350px;height:10px'></div>", 400);
        LayoutBox ow = LayoutTestSupport.box(w, "o");
        Check.near(0, ow.getX(), T, "does not fit: moved below");
        Check.near(30, ow.getY(), T, "below the float");
    }

    /**
     * A float in inline content goes to the top of the line it is found on and the line gives way to it; the text on both sides of the
     * float's source position flows around it.
     * <p>
     * 인라인 콘텐츠 안의 float 는 만난 줄의 위쪽에 놓이고 줄은 거기에 자리를 내준다. float 의 소스 위치 앞뒤의 텍스트가 모두 그 둘레로 흐른다.
     */
    public void testFloatInsideInlineContent() {
        double cwd = LayoutTestSupport.charWidth();
        double width = 40 + 8 * cwd;
        LayoutResult r = LayoutTestSupport.layout(LH + "<p id=p style='margin:0'>ab<span id=f style='float:right;width:40px;height:10px'></span>cd efg</p>", width);
        LayoutBox f = LayoutTestSupport.box(r, "f");
        Check.near(8 * cwd, f.getX(), T, "the float is at the right edge");
        Check.near(0, f.getY(), T, "the float is at the top of the first line");
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        Check.equal(1, lines.size(), "the text fits on one line beside the float");
        Check.near(8 * cwd, lines.get(0).getWidth(), T, "the line is shortened");
        Check.equal("abcd efg", LayoutTestSupport.lineText(lines.get(0)), "the text around the float stays together");
    }

    /**
     * A float found after the line is full waits for the next line.
     * <p>
     * 줄이 가득 찬 뒤에 만난 float 는 다음 줄을 기다린다.
     */
    public void testFloatThatDoesNotFitBesideContentWaits() {
        double cwd = LayoutTestSupport.charWidth();
        double width = 10 * cwd + 20;
        LayoutResult r = LayoutTestSupport.layout(LH + "<p id=p style='margin:0'>aaaaaaaaa<span id=f style='float:left;width:30px;height:10px'></span> bb</p>", width);
        LayoutBox f = LayoutTestSupport.box(r, "f");
        Check.near(10, f.getY(), T, "the float goes to the top of the next line (10 px)");
        Check.near(0, f.getX(), T, "left edge");
    }

    /**
     * The positions of floats inside nested blocks follow the margins that collapse through the first children: the child's margin moves
     * its parent, and the float and the lines beside it must be in the right place after the estimate was corrected.
     * <p>
     * 중첩 블록 안 float 의 위치는 첫 자식을 통과해 합쳐지는 margin 을 따른다: 자식의 margin 이 부모를 옮기며, 추정을 고친 뒤 float 와 그 옆의 줄이 올바른
     * 자리에 있어야 한다.
     */
    public void testNestedMarginsAndFloats() {
        double cwd = LayoutTestSupport.charWidth();
        double width = 50 + 10 * cwd;
        LayoutResult r = LayoutTestSupport.layout(LH + "<div id=fl style='float:left;width:50px;height:100px'></div>"
                + "<div id=o><div id=i><h1 id=h style='margin:30px 0 0;font:10px monospace;line-height:10px'>aaaaaaaaa bbbbbbbbb</h1></div></div>", width);
        LayoutBox h = LayoutTestSupport.box(r, "h");
        Check.near(30, h.getY(), T, "the margin of h collapses through i, o and body");
        Check.near(30, LayoutTestSupport.box(r, "fl").getY(), T, "the float is at the top of body, which the collapsed margin moved down");
        List<LayoutBox> lines = LayoutTestSupport.lines(h);
        Check.equal(2, lines.size(), "two lines");
        Check.near(50, lines.get(0).getX(), T, "line 1 beside the float (the float covers y 30..130)");
        Check.near(30, lines.get(0).getY(), T, "line 1 y");
        Check.near(10 * cwd, lines.get(0).getWidth(), T, "line 1 width");
    }

    /**
     * The same document laid out twice gives the same boxes (the second layout of subtrees leaves no duplicated lines or floats).
     * <p>
     * 같은 문서를 두 번 배치해도 같은 상자가 나온다(하위 트리의 두 번째 배치가 줄이나 float 를 중복으로 남기지 않는다).
     */
    public void testRelayoutDoesNotDuplicate() {
        String html = LH + "<div style='float:left;width:50px;height:100px'></div><div id=o><div id=i><p id=h style='margin:30px 0 0'>aaaaaaaa bbbbbbbb</p></div></div>";
        LayoutResult r1 = LayoutTestSupport.layout(html, 150);
        LayoutResult r2 = LayoutTestSupport.layout(html, 150);
        Check.equal(r1.getBoxCount(), r2.getBoxCount(), "the box count is stable");
        StringBuilder all = new StringBuilder();
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r1, "h"));
        // 모든 줄의 텍스트를 이어 붙여 낱말이 한 번씩만 나오는지 본다.
        for (int i = 0; i < lines.size(); i++) {
            all.append(LayoutTestSupport.lineText(lines.get(i)));
        }
        Check.equal("aaaaaaaabbbbbbbb", all.toString().replace(" ", ""), "no duplicated lines");
        Check.equal(lines.size(), LayoutTestSupport.lines(LayoutTestSupport.box(r2, "h")).size(), "same line count");
    }

    /**
     * A float that is wider than its container sticks out at the side it floats to instead of looping.
     * <p>
     * 컨테이너보다 넓은 float 는 무한히 시도하지 않고 float 방향 쪽으로 튀어나온다.
     */
    public void testFloatWiderThanContainer() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='float:left;width:500px;height:10px'></div><div id=b style='float:left;width:10px;height:10px'></div>", 300);
        Check.near(0, LayoutTestSupport.box(r, "a").getX(), T, "a is at the left edge");
        Check.near(10, LayoutTestSupport.box(r, "b").getY(), T, "b goes below a");
        Check.near(500, r.getContentWidth(), T, "the scrollable width includes the float");
    }

    /**
     * Floats do not affect the minimum and maximum content widths of the shrink-to-fit parent incorrectly: the float's width counts.
     * <p>
     * float 는 shrink-to-fit 부모의 min/max content 너비에 자신의 너비만큼 기여한다.
     */
    public void testFloatInShrinkToFitParent() {
        LayoutResult r = LayoutTestSupport.layout("<div id=o style='float:left'><div style='float:left;width:70px;height:5px'></div>abc</div>", 400);
        double cwd = LayoutTestSupport.charWidth();
        Check.near(70 + 3 * cwd, LayoutTestSupport.box(r, "o").getWidth(), T, "max-content: the float plus the text");
    }

    /**
     * Elements that float even though they were inline become block boxes; replaced elements can float too.
     * <p>
     * 인라인이었던 요소도 float 이면 블록 상자가 된다. 치환 요소도 float 가 될 수 있다.
     */
    public void testInlineBecomesBlockWhenFloated() {
        LayoutResult r = LayoutTestSupport.layout("<span id=s style='float:left;width:20px;height:8px'>x</span><img id=i style='float:left' width=15 height=6>", 400);
        LayoutBox s = LayoutTestSupport.box(r, "s");
        Check.equal(LayoutBox.Type.BLOCK, s.getType(), "blockified");
        Check.near(20, s.getWidth(), T, "width applies");
        LayoutBox i = LayoutTestSupport.box(r, "i");
        Check.equal(LayoutBox.Type.REPLACED, i.getType(), "replaced float");
        Check.near(20, i.getX(), T, "next to the first float");
    }

    /**
     * Thousands of floats are laid out in a time that grows moderately and give the expected grid.
     * <p>
     * 수천 개의 float 가 완만하게 늘어나는 시간에 배치되고 기대한 격자를 이룬다.
     */
    public void testManyFloatsStayFast() {
        StringBuilder sb = new StringBuilder("<div id=g>");
        // 한 줄에 40 개씩 들어가는 10x10 float 3000 개를 만든다.
        for (int i = 0; i < 3000; i++) {
            sb.append("<div id=f").append(i).append(" style='float:left;width:10px;height:10px'></div>");
        }
        sb.append("</div><div id=after style='clear:both;height:1px'></div>");
        long start = System.currentTimeMillis();
        LayoutResult r = LayoutTestSupport.layout(sb.toString(), 400);
        long took = System.currentTimeMillis() - start;
        Check.isTrue(took < 20000, "3000 floats must be laid out in a reasonable time: " + took + " ms");
        Check.near(390, LayoutTestSupport.box(r, "f39").getX(), T, "the last float of the first row");
        Check.near(0, LayoutTestSupport.box(r, "f40").getX(), T, "the 41st float starts the second row");
        Check.near(10, LayoutTestSupport.box(r, "f40").getY(), T, "second row y");
        Check.near(74 * 10 + 10, LayoutTestSupport.box(r, "after").getY(), T, "75 rows of floats are cleared");
    }

    /**
     * Nested blocks whose margins collapse in a way that the position estimate cannot predict do not make the layout explode: the second
     * layouts are limited.
     * <p>
     * 위치 추정이 예측할 수 없는 방식으로 margin 이 합쳐지는 중첩 블록이 배치를 폭발시키지 않는다. 두 번째 배치는 제한된다.
     */
    public void testPathologicalNestingIsBounded() {
        StringBuilder sb = new StringBuilder("<div style='float:left;width:30px;height:5px'></div>");
        // 첫 자식이 빈 블록이고 그 뒤 블록의 margin 이 합쳐지는, 추정이 틀리는 중첩을 40 단계 만든다.
        for (int i = 0; i < 40; i++) {
            sb.append("<div><div style='margin-bottom:").append(10 + i).append("px'></div>");
        }
        sb.append("<p id=deep style='margin:5px 0 0'>text text text text text</p>");
        // 열었던 블록을 모두 닫는다.
        for (int i = 0; i < 40; i++) {
            sb.append("</div>");
        }
        long start = System.currentTimeMillis();
        LayoutResult r = LayoutTestSupport.layout(sb.toString(), 400);
        long took = System.currentTimeMillis() - start;
        Check.isTrue(took < 30000, "the layout is bounded: " + took + " ms");
        Check.isTrue(LayoutTestSupport.box(r, "deep").getY() >= 10, "the paragraph is below the collapsed margin");
        Check.isTrue(LayoutTestSupport.lines(LayoutTestSupport.box(r, "deep")).size() >= 1, "the paragraph has lines");
    }
}
