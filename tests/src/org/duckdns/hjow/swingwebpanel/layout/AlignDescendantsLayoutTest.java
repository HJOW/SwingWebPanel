package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the descendant alignment of the legacy <code>center</code> element and <code>align</code> attributes (plan item
 * P07-07-b-5, HTML5 2014 section 10.2 "align descendants", 10.3.3 and 10.3.9): which boxes are aligned, the used margins, the nearest
 * aligning ancestor, the applicable <code>align</code> attribute of the descendant, <code>auto</code> margins, negative free space,
 * independence of <code>text-align</code>, tables, table cells, captions and floats inside an aligned box. The viewport is 400px wide
 * and the reset sheet removes the default margins.
 * <p>
 * 이전 방식의 <code>center</code> 요소와 <code>align</code> 속성의 자손 정렬 시험(계획 P07-07-b-5, HTML5 2014 10.2 "align descendants", 10.3.3,
 * 10.3.9): 정렬되는 상자, margin 의 사용 값, 가장 가까운 정렬 조상, 자손의 적용 가능한 <code>align</code> 속성, <code>auto</code> margin, 음수 남는
 * 공간, <code>text-align</code> 과의 독립, 표, 표 셀, caption, 정렬된 상자 안의 float. 뷰포트는 400px 이며 초기화 시트가 기본 margin 을 없앤다.
 */
public class AlignDescendantsLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.001;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AlignDescendantsLayoutTest() {
    }

    /**
     * A fixed-width block inside <code>center</code> is centered; its used margins split the free space.
     * <p>
     * <code>center</code> 안의 고정 너비 블록은 가운데로 가고 사용 margin 이 남는 공간을 나눈다.
     */
    public void testCenterElementCentersFixedWidthBlock() {
        LayoutResult r = LayoutTestSupport.layout("<center><div id=a style='width:100px;height:10px'></div></center>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(150, a.getX(), T, "x");
        Check.near(150, a.getMarginLeft(), T, "left margin");
        Check.near(150, a.getMarginRight(), T, "right margin");
        Check.near(100, a.getWidth(), T, "width");
    }

    /**
     * The margin box is centered, not the border box: the computed margins stay and the free space is split around them.
     * <p>
     * border box 가 아니라 margin 상자가 가운데로 가며 계산된 margin 은 유지되고 남는 공간이 그 둘레로 나뉜다.
     */
    public void testMarginBoxIsCentered() {
        LayoutResult r = LayoutTestSupport.layout("<div align=center><div id=a style='width:100px;margin:0 10px 0 30px;height:10px'></div></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        // 남는 공간은 400 - (30 + 100 + 10) = 260 이고 반씩 나뉜다.
        Check.near(160, a.getMarginLeft(), T, "left margin 30 + 130");
        Check.near(140, a.getMarginRight(), T, "right margin 10 + 130");
        Check.near(160, a.getX(), T, "x");
    }

    /**
     * <code>div align</code> values: middle and center center, left and justify keep the start, right moves to the end.
     * <p>
     * <code>div align</code> 값: middle 과 center 는 가운데, left 와 justify 는 시작 쪽, right 는 끝 쪽이다.
     */
    public void testDivAlignValues() {
        String[] values = { "center", "middle", "left", "justify", "right", "CENTER", "foo" };
        double[] expected = { 150, 150, 0, 0, 300, 150, 0 };
        // 값마다 새 문서를 배치해 첫 번째 상자의 위치를 확인한다.
        for (int i = 0; i < values.length; i++) {
            LayoutResult r = LayoutTestSupport.layout("<div align='" + values[i] + "'><div id=a style='width:100px;height:10px'></div></div>", 400);
            Check.near(expected[i], LayoutTestSupport.box(r, "a").getX(), T, "align=" + values[i]);
        }
    }

    /**
     * Elements that are not aligning elements do not align their children, even with an <code>align</code> attribute or
     * <code>text-align</code>.
     * <p>
     * 정렬 요소가 아닌 요소는 <code>align</code> 속성이나 <code>text-align</code> 이 있어도 자식을 정렬하지 않는다.
     */
    public void testOtherElementsDoNotAlignChildren() {
        LayoutResult r = LayoutTestSupport.layout("<section align=center><div id=a style='width:100px;height:10px'></div></section>"
                + "<div style='text-align:center'><div id=b style='width:100px;height:10px'></div></div>"
                + "<blockquote align=right style='margin:0'><div id=c style='width:100px;height:10px'></div></blockquote>", 400);
        Check.near(0, LayoutTestSupport.box(r, "a").getX(), T, "section");
        Check.near(0, LayoutTestSupport.box(r, "b").getX(), T, "text-align alone does not move blocks");
        Check.near(0, LayoutTestSupport.box(r, "c").getX(), T, "blockquote");
    }

    /**
     * The most deeply nested aligning element wins, whichever alignment the outer one requests.
     * <p>
     * 바깥 요소가 어떤 정렬을 요구해도 가장 깊게 중첩된 정렬 요소가 이긴다.
     */
    public void testNearestAligningAncestorWins() {
        LayoutResult r = LayoutTestSupport.layout("<center><div align=left><div id=a style='width:100px;height:10px'></div></div></center>"
                + "<div align=right><center><div id=b style='width:100px;height:10px'></div></center></div>"
                + "<center><div><section><div id=c style='width:100px;height:10px'></div></section></div></center>", 400);
        Check.near(0, LayoutTestSupport.box(r, "a").getX(), T, "inner div align=left");
        Check.near(150, LayoutTestSupport.box(r, "b").getX(), T, "inner center");
        Check.near(150, LayoutTestSupport.box(r, "c").getX(), T, "descendants at any depth");
    }

    /**
     * A descendant with an applicable <code>align</code> attribute is not aligned by its ancestors; an attribute without a rule is.
     * <p>
     * 적용 가능한 <code>align</code> 속성이 있는 자손은 조상이 정렬하지 않으며 규칙이 없는 속성은 정렬된다.
     */
    public void testDescendantWithApplicableAlignIsNotAligned() {
        LayoutResult r = LayoutTestSupport.layout("<center>"
                + "<div id=a align=left style='width:100px;height:10px'></div>"
                + "<p id=b align=left style='width:100px;margin:0'></p>"
                + "<div id=c align=foo style='width:100px;height:10px'></div>"
                + "<div id=d align=bottom style='width:100px;height:10px'></div>"
                + "</center>", 400);
        Check.near(0, LayoutTestSupport.box(r, "a").getX(), T, "div align=left");
        Check.near(0, LayoutTestSupport.box(r, "b").getX(), T, "p align=left");
        Check.near(150, LayoutTestSupport.box(r, "c").getX(), T, "unknown value has no rule");
        Check.near(150, LayoutTestSupport.box(r, "d").getX(), T, "bottom has no rule on a div");
    }

    /**
     * Only boxes whose margins are both not <code>auto</code> and that are over-constrained are aligned: an <code>auto</code> margin keeps
     * its CSS meaning and a box that fills its container has no free space.
     * <p>
     * 두 margin 이 모두 <code>auto</code> 가 아니고 제약이 넘치는 상자만 정렬한다: <code>auto</code> margin 은 CSS 의미를 유지하고 포함 블록을 채우는
     * 상자에는 남는 공간이 없다.
     */
    public void testAutoMarginsAndFillingBoxesAreUntouched() {
        LayoutResult r = LayoutTestSupport.layout("<div align=left>"
                + "<div id=a style='width:100px;height:10px;margin-left:auto'></div>"
                + "<div id=b style='width:100px;height:10px;margin-right:auto'></div>"
                + "<div id=c style='width:100px;height:10px;margin:0 auto'></div>"
                + "<div id=d style='height:10px;margin:0 20px'></div>"
                + "</div><center>"
                + "<div id=e style='width:100px;height:10px;margin-left:auto'></div>"
                + "<div id=f style='height:10px;margin:0 20px'></div>"
                + "</center>", 400);
        Check.near(300, LayoutTestSupport.box(r, "a").getX(), T, "auto left margin pushes the box to the right");
        Check.near(0, LayoutTestSupport.box(r, "b").getX(), T, "auto right margin");
        Check.near(150, LayoutTestSupport.box(r, "c").getX(), T, "both auto center");
        Check.near(20, LayoutTestSupport.box(r, "d").getX(), T, "auto width fills");
        Check.near(300, LayoutTestSupport.box(r, "e").getX(), T, "auto margin inside center stays right-aligned");
        Check.near(360, LayoutTestSupport.box(r, "f").getWidth(), T, "auto width inside center fills");
        Check.near(20, LayoutTestSupport.box(r, "f").getX(), T, "auto width inside center keeps the margin");
    }

    /**
     * A box that is wider than its container (negative free space) is not moved: the right margin becomes negative as in CSS 2.1.
     * <p>
     * 포함 블록보다 넓은 상자(음수 남는 공간)는 움직이지 않는다: CSS 2.1 처럼 오른쪽 margin 이 음수가 된다.
     */
    public void testNegativeFreeSpaceIsNotAligned() {
        LayoutResult r = LayoutTestSupport.layout("<center><div id=a style='width:500px;height:10px'></div></center>"
                + "<div align=right><div id=b style='width:500px;height:10px'></div></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(0, a.getX(), T, "center, wide box");
        Check.near(-100, a.getMarginRight(), T, "forced right margin is smaller");
        Check.near(0, LayoutTestSupport.box(r, "b").getX(), T, "right, wide box");
    }

    /**
     * The alignment is independent of <code>text-align</code> and of author CSS that changes the text alignment of the element.
     * <p>
     * 정렬은 <code>text-align</code> 과 요소의 글 정렬을 바꾸는 작성자 CSS 와 독립이다.
     */
    public void testIndependentOfTextAlign() {
        LayoutResult r = LayoutTestSupport.layout("<center style='text-align:left'><div id=a style='width:100px;height:10px'></div></center>"
                + "<div align=center style='text-align:right'><div id=b style='width:100px;height:10px'></div></div>"
                + "<center><div id=c style='width:100px;height:10px;text-align:right'></div></center>", 400);
        Check.near(150, LayoutTestSupport.box(r, "a").getX(), T, "center with text-align:left");
        Check.near(150, LayoutTestSupport.box(r, "b").getX(), T, "div align=center with text-align:right");
        Check.near(150, LayoutTestSupport.box(r, "c").getX(), T, "own text-align does not matter");
    }

    /**
     * A width clamped by <code>max-width</code> is over-constrained too, so the block is aligned; <code>min-width</code> that makes the
     * box wider than the container is not.
     * <p>
     * <code>max-width</code> 로 제한된 너비도 제약이 넘치므로 블록이 정렬되고 포함 블록보다 넓게 만드는 <code>min-width</code> 는 정렬되지 않는다.
     */
    public void testMaxWidthClampIsOverConstrained() {
        LayoutResult r = LayoutTestSupport.layout("<center><div id=a style='max-width:200px;height:10px'></div></center>"
                + "<div align=right><div id=b style='max-width:100px;margin:0 10px;height:10px'></div></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(100, a.getX(), T, "x");
        Check.near(200, a.getWidth(), T, "clamped width");
        Check.near(290, LayoutTestSupport.box(r, "b").getX(), T, "right: 400 - 10 - 100");
    }

    /**
     * Block-level replaced elements are aligned like other blocks, inline content is not moved by this rule.
     * <p>
     * 블록 수준 치환 요소는 다른 블록처럼 정렬되며 인라인 내용은 이 규칙으로 움직이지 않는다.
     */
    public void testBlockReplacedElementIsAligned() {
        LayoutResult r = LayoutTestSupport.layout("<center><img id=a style='display:block' width=100 height=20></center>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(150, a.getX(), T, "block image");
        Check.near(100, a.getWidth(), T, "width");
    }

    /**
     * A table in <code>center</code> is centered whether its width is specified or comes from its content; a table that has its own
     * <code>align</code> attribute follows that attribute.
     * <p>
     * <code>center</code> 안의 표는 너비가 지정되었거나 내용에서 나왔거나 가운데로 가며 자신의 <code>align</code> 속성이 있는 표는 그 속성을 따른다.
     */
    public void testTablesAreAligned() {
        LayoutResult r = LayoutTestSupport.layout("<center>"
                + "<table id=a width=200 style='border-spacing:0'><tr><td style='padding:0'>x</td></tr></table>"
                + "<table id=b style='border-spacing:0'><tr><td style='padding:0;width:60px'>x</td></tr></table>"
                + "<table id=c align=left width=100 style='border-spacing:0'><tr><td style='padding:0'>x</td></tr></table>"
                + "</center>"
                + "<div align=right><table id=d width=100 style='border-spacing:0'><tr><td style='padding:0'>x</td></tr></table></div>", 400);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(100, a.getX(), T, "specified width");
        Check.near(200, a.getWidth(), T, "width");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near((400 - b.getWidth()) / 2, b.getX(), T, "content width");
        Check.near(0, LayoutTestSupport.box(r, "c").getX(), T, "table align=left floats to the left");
        Check.near(300, LayoutTestSupport.box(r, "d").getX(), T, "div align=right");
    }

    /**
     * Cell and row alignment attributes align the blocks inside the cells; a caption centers by default.
     * <p>
     * 셀과 행의 정렬 속성은 셀 안의 블록을 정렬하고 caption 은 기본으로 가운데이다.
     */
    public void testTableCellsRowsAndCaption() {
        LayoutResult r = LayoutTestSupport.layout("<table style='border-spacing:0;width:400px'><caption><div id=cap style='width:100px;height:10px'></div></caption>"
                + "<tr><td align=center style='padding:0'><div id=a style='width:100px;height:10px'></div></td></tr>"
                + "<tr align=right><td style='padding:0'><div id=b style='width:100px;height:10px'></div></td></tr>"
                + "<tr><td style='padding:0'><div id=c style='width:100px;height:10px'></div></td></tr>"
                + "<tr><td align=right style='padding:0'><div id=d align=left style='width:100px;height:10px'></div></td></tr></table>"
                + "<table style='border-spacing:0;width:400px'><caption align=left><div id=cap2 style='width:100px;height:10px'></div></caption>"
                + "<tr><td style='padding:0'>x</td></tr></table>", 400);
        Check.near(150, LayoutTestSupport.box(r, "cap").getX(), T, "caption centers by default");
        Check.near(150, LayoutTestSupport.box(r, "a").getX(), T, "td align=center");
        Check.near(300, LayoutTestSupport.box(r, "b").getX(), T, "tr align=right");
        Check.near(0, LayoutTestSupport.box(r, "c").getX(), T, "no alignment");
        Check.near(0, LayoutTestSupport.box(r, "d").getX(), T, "div with its own align inside td align=right");
        Check.near(0, LayoutTestSupport.box(r, "cap2").getX(), T, "caption align=left");
    }

    /**
     * A float inside an aligned block uses the final x of that block: the used margins are known before the content is laid out.
     * <p>
     * 정렬된 블록 안의 float 는 그 블록의 최종 x 를 쓴다: 사용 margin 은 내용을 배치하기 전에 알려져 있다.
     */
    public void testFloatInsideAlignedBlock() {
        LayoutResult r = LayoutTestSupport.layout("<center><div id=a style='width:100px'><div id=f style='float:left;width:20px;height:10px'></div>x</div></center>"
                + "<div style='width:100px;margin:0 auto'><div id=g style='float:left;width:20px;height:10px'></div>x</div>", 400);
        Check.near(150, LayoutTestSupport.box(r, "f").getX(), T, "float in the centered block");
        Check.near(150, LayoutTestSupport.box(r, "g").getX(), T, "float in an auto-margin block");
        // 바깥 float 가 있으면 안쪽 float 의 위치가 float 문맥 안의 블록 위치에 의존한다: 옛 위치로 계산하면 바깥 float 옆으로 밀린다.
        r = LayoutTestSupport.layout("<div style='float:left;width:60px;height:100px'></div>"
                + "<center><div id=a style='width:100px'><div id=f style='float:left;width:20px;height:10px'></div>x</div></center>"
                + "<div style='width:100px;margin:0 auto'><div id=g style='float:left;width:20px;height:10px'></div>x</div>", 400);
        Check.near(150, LayoutTestSupport.box(r, "f").getX(), T, "inner float is not pushed by an outer float to its left");
        Check.near(150, LayoutTestSupport.box(r, "g").getX(), T, "same for auto margins");
    }

    /**
     * Absolutely positioned boxes and floats are outside the normal flow and are not aligned by the ancestors.
     * <p>
     * 절대 배치 상자와 float 는 정상 흐름 밖이므로 조상이 정렬하지 않는다.
     */
    public void testOutOfFlowBoxesAreNotAligned() {
        LayoutResult r = LayoutTestSupport.layout("<center><div id=a style='float:left;width:100px;height:10px'></div>"
                + "<div style='position:relative'><div id=b style='position:absolute;left:0;width:100px;height:10px'></div></div></center>", 400);
        Check.near(0, LayoutTestSupport.box(r, "a").getX(), T, "float");
        Check.near(0, LayoutTestSupport.box(r, "b").getX(), T, "absolute");
    }

    /**
     * Generated content boxes are children of their element: a block <code>::before</code> in <code>center</code> is aligned by it.
     * <p>
     * 생성 콘텐츠 상자는 요소의 자식이다: <code>center</code> 안의 블록 <code>::before</code> 는 그 요소가 정렬한다.
     */
    public void testGeneratedBlockInsideCenter() {
        LayoutResult r = LayoutTestSupport.layout("<style>#c::before{content:'';display:block;width:100px;height:10px}</style><center id=c></center>", 400);
        LayoutBox before = null;
        LayoutBox c = LayoutTestSupport.box(r, "c");
        // 생성된 자식 중 ::before 상자를 찾는다.
        for (int i = 0; i < c.getChildren().size(); i++) {
            if ("before".equals(c.getChildren().get(i).getPseudo())) {
                before = c.getChildren().get(i);
            }
        }
        Check.notNull(before, "::before box");
        Check.near(150, before.getX(), T, "x");
    }
}
