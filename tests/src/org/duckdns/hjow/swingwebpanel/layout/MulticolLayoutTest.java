package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of multi-column layout (plan item P10-03, CSS Multi-column Layout Module Level 1): the used column count and width
 * (<code>column-count</code>, <code>column-width</code>, <code>column-gap</code>), column balancing, <code>column-fill:auto</code>
 * with a height, overflow columns, breaking between lines with <code>orphans</code> and <code>widows</code>, truncated margins at a
 * cut, boxes that are split between columns (fragments with the edges of the first and the last fragment only), units that cannot be
 * split, <code>column-span:all</code> sections, column rules, <code>direction:rtl</code> and intrinsic widths. The expected
 * coordinates are computed by hand from the specification and checked against real runs of the HTML parser, the CSS engine and the
 * layout engine.
 * <p>
 * 다단 레이아웃 시험(계획 P10-03, CSS Multi-column Layout Module Level 1): 사용 열 수와 너비(<code>column-count</code>, <code>column-width</code>,
 * <code>column-gap</code>), 열 균형, 높이가 있는 <code>column-fill:auto</code>, 넘침 열, <code>orphans</code> 와 <code>widows</code> 를 반영한 줄 사이 끊기, 자르는 곳에서
 * 버려지는 margin, 열 사이에서 나뉘는 상자(첫 조각과 마지막 조각만 가장자리를 가지는 조각), 나눌 수 없는 단위, <code>column-span:all</code> 구간, 열 구분선,
 * <code>direction:rtl</code>, 고유 너비. 기대 좌표는 규격으로 손계산했으며 HTML 파서, CSS 엔진, 레이아웃 엔진을 실제로 돌려 확인한다.
 */
public class MulticolLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MulticolLayoutTest() {
    }

    /**
     * Builds n blocks of a height: <code>&lt;div id=PREFIXi style='height:Hpx'&gt;</code>.
     * <p>
     * 높이가 정해진 블록 n 개의 마크업을 만든다.
     *
     * @param prefix id prefix / id 접두어
     * @param n      number of blocks / 블록 수
     * @param h      height / 높이
     * @return the markup / 마크업
     */
    private static String blocks(String prefix, int n, int h) {
        StringBuilder sb = new StringBuilder();
        // 같은 높이의 블록을 n 개 만든다.
        for (int i = 1; i <= n; i++) {
            sb.append("<div id=").append(prefix).append(i).append(" style='height:").append(h).append("px'></div>");
        }
        return sb.toString();
    }

    /**
     * Builds a paragraph of n lines separated by line breaks.
     * <p>
     * 줄바꿈으로 나뉜 n 줄 문단의 마크업을 만든다.
     *
     * @param n number of lines / 줄 수
     * @return the markup / 마크업
     */
    private static String lines(int n) {
        StringBuilder sb = new StringBuilder();
        // 줄 사이에 br 을 넣는다.
        for (int i = 1; i <= n; i++) {
            sb.append("l").append(i);
            if (i < n) {
                sb.append("<br>");
            }
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
     * Returns all the boxes (fragments) of an element.
     * <p>
     * 요소의 모든 상자(조각)를 반환한다.
     *
     * @param r  the result / 결과
     * @param id the id / id
     * @return the boxes / 상자
     */
    private static List<LayoutBox> all(LayoutResult r, String id) {
        Element e = ((Element) r.getRoot().getNode()).getOwnerDocument().getElementById(id);
        return r.getBoxes(e);
    }

    /**
     * The used column width is (width + gap) / count - gap; the blocks are balanced over the columns; the container is as high as the
     * columns.
     * <p>
     * 사용 열 너비는 (너비 + gap) / 열 수 - gap 이고 블록은 열에 균형 있게 나뉘며 컨테이너는 열의 높이이다.
     */
    public void testColumnCountAndBalance() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:320px;column-count:3;column-gap:10px'>" + blocks("b", 6, 20) + "</div><div id=after style='height:5px'></div>", 400);
        LayoutBox c = b(r, "c");
        Check.isTrue(c.isMulticolContainer(), "multicol flag");
        Check.equal(3, c.getColumnCount(), "three columns");
        Check.near(100, b(r, "b1").getWidth(), T, "column width (320+10)/3-10");
        Check.near(0, b(r, "b1").getX(), T, "b1 x");
        Check.near(0, b(r, "b1").getY(), T, "b1 y");
        Check.near(20, b(r, "b2").getY(), T, "b2 y (second block of the first column)");
        Check.near(110, b(r, "b3").getX(), T, "b3 starts the second column");
        Check.near(0, b(r, "b3").getY(), T, "b3 y");
        Check.near(220, b(r, "b5").getX(), T, "b5 starts the third column");
        Check.near(220, b(r, "b6").getX(), T, "b6 x");
        Check.near(20, b(r, "b6").getY(), T, "b6 y");
        Check.near(40, c.getHeight(), T, "balanced column height");
        Check.near(40, b(r, "after").getY(), T, "the next block follows the container");
        Check.equal(3, c.getChildren().size(), "one column box per column");
    }

    /**
     * <code>column-width</code> gives as many columns as fit; with a <code>column-count</code> it is the maximum.
     * <p>
     * <code>column-width</code> 는 들어가는 만큼의 열을 주고 <code>column-count</code> 와 함께 쓰면 그것이 최대이다.
     */
    public void testColumnWidth() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:330px;columns:100px;column-gap:10px'>" + blocks("a", 3, 10) + "</div>"
                + "<div id=d style='width:400px;column-count:2;column-width:100px;column-gap:10px'>" + blocks("d", 2, 10) + "</div>"
                + "<div id=e style='width:100px;columns:300px'>" + blocks("e", 2, 10) + "</div>", 500);
        Check.equal(3, b(r, "c").getColumnCount(), "floor((330+10)/(100+10)) = 3 columns");
        Check.near(340.0 / 3 - 10, b(r, "a1").getWidth(), T, "column width");
        Check.equal(2, b(r, "d").getColumnCount(), "count 2 limits the three that would fit");
        Check.near(195, b(r, "d1").getWidth(), T, "column width (400+10)/2-10");
        Check.equal(1, b(r, "e").getColumnCount(), "at least one column");
        Check.near(100, b(r, "e1").getWidth(), T, "a single column fills the container");
    }

    /**
     * With <code>column-fill:auto</code> and a height the columns are filled one after another; content that does not fit goes into
     * overflow columns after the last column.
     * <p>
     * <code>column-fill:auto</code> 와 높이가 있으면 열을 차례로 채우고 들어가지 않는 내용은 마지막 열 뒤의 넘침 열로 간다.
     */
    public void testFillAutoAndOverflowColumns() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:210px;height:40px;column-count:2;column-gap:10px;column-fill:auto'>" + blocks("b", 5, 20) + "</div>", 500);
        Check.near(0, b(r, "b1").getX(), T, "first column");
        Check.near(20, b(r, "b2").getY(), T, "two blocks fit the 40 pixel height");
        Check.near(110, b(r, "b3").getX(), T, "second column");
        Check.near(0, b(r, "b3").getY(), T, "b3 y");
        Check.near(110, b(r, "b4").getX(), T, "second column again");
        Check.near(220, b(r, "b5").getX(), T, "the fifth block goes into an overflow column");
        Check.equal(3, b(r, "c").getColumnCount(), "three columns were needed");
        Check.near(40, b(r, "c").getHeight(), T, "the specified height");
        Check.near(320, b(r, "b5").getX() + b(r, "b5").getWidth(), T, "the overflow column leaves the container (210) to the right: 220 + 100");
    }

    /**
     * Without a height <code>column-fill:auto</code> keeps all the content in the first column.
     * <p>
     * 높이가 없으면 <code>column-fill:auto</code> 는 모든 내용을 첫 열에 둔다.
     */
    public void testFillAutoWithoutHeight() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;column-count:2;column-fill:auto'>" + blocks("b", 4, 10) + "</div>", 500);
        Check.near(40, b(r, "c").getHeight(), T, "all the content is in one column");
        Check.near(0, b(r, "b4").getX(), T, "b4 in the first column");
    }

    /**
     * Lines are broken between lines; <code>widows</code> and <code>orphans</code> keep at least two lines together at both sides of a
     * cut.
     * <p>
     * 줄 사이에서 끊는다. <code>widows</code> 와 <code>orphans</code> 가 자르는 곳 양쪽에 적어도 두 줄을 남긴다.
     */
    public void testLineBreaksAndWidowsOrphans() {
        LayoutResult r = LayoutTestSupport.layout("<style>.p{line-height:10px;margin:0}</style>"
                + "<div id=c style='width:200px;column-count:2;column-gap:0'><p class=p id=p>" + lines(6) + "</p></div>"
                + "<div id=d style='width:200px;height:40px;column-count:2;column-gap:0;column-fill:auto'><p class=p id=q>" + lines(5) + "</p></div>", 500);
        List<LayoutBox> l1 = LayoutTestSupport.lines(b(r, "c"));
        Check.equal(6, l1.size(), "six lines");
        Check.near(0, l1.get(0).getX(), T, "line 1 in the first column");
        Check.near(0, l1.get(2).getX(), T, "line 3 in the first column (3 lines each)");
        Check.near(100, l1.get(3).getX(), T, "line 4 starts the second column");
        Check.near(0, l1.get(3).getY(), T, "line 4 at the top of the column");
        Check.near(30, b(r, "c").getHeight(), T, "balanced to three lines");
        List<LayoutBox> l2 = LayoutTestSupport.lines(b(r, "d"));
        // 40 픽셀에 4 줄이 들어가지만 뒤에 1 줄만 남으면 widows(2) 에 어긋나므로 3 줄에서 끊는다.
        Check.near(0, l2.get(2).getX(), T, "line 3 stays in the first column");
        Check.near(100, l2.get(3).getX(), T, "line 4 moved to the second column so that two lines follow the cut (widows)");
        Check.near(100, l2.get(4).getX(), T, "line 5 in the second column");
        Check.near(d(b(r, "d")), l2.get(3).getY(), T, "line 4 at the top of the second column");
    }

    /**
     * Returns the y of a box.
     * <p>
     * 상자의 y 를 반환한다.
     *
     * @param c the box / 상자
     * @return y / y
     */
    private static double d(LayoutBox c) {
        return c.getY();
    }

    /**
     * Margins at a cut are dropped: the first box of a column after a cut starts at the top of the column; the first column keeps the
     * top margin.
     * <p>
     * 자르는 곳의 margin 은 버려진다: 자른 뒤 열의 첫 상자는 열 위쪽에서 시작하고 첫 열은 위쪽 margin 을 유지한다.
     */
    public void testMarginsAtCutsAreDropped() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;column-count:2;column-gap:0'>"
                + "<div id=b1 style='height:20px;margin:10px 0'></div><div id=b2 style='height:20px;margin:10px 0'></div>"
                + "<div id=b3 style='height:20px;margin:10px 0'></div><div id=b4 style='height:20px;margin:10px 0'></div></div>", 500);
        Check.near(10, b(r, "b1").getY(), T, "first column keeps the margin");
        Check.near(40, b(r, "b2").getY(), T, "b2 y");
        Check.near(100, b(r, "b3").getX(), T, "b3 starts the second column");
        Check.near(0, b(r, "b3").getY(), T, "the margin above b3 is dropped at the cut");
        Check.near(30, b(r, "b4").getY(), T, "b4 y");
        Check.near(60, b(r, "c").getHeight(), T, "height of the columns");
    }

    /**
     * A box that crosses a cut is split: the first fragment has the top edge and the second the bottom edge; the children are in the
     * columns where they are.
     * <p>
     * 자르는 곳을 가로지르는 상자는 나뉜다: 첫 조각은 위쪽 가장자리를, 두 번째 조각은 아래쪽 가장자리를 가지고 자식은 자신이 있는 열에 있다.
     */
    public void testBoxSplitIntoFragments() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;column-count:2;column-gap:0'><div id=box style='border:1px solid #000;padding:5px'>"
                + blocks("p", 4, 20) + "</div></div>", 500);
        List<LayoutBox> f = all(r, "box");
        Check.equal(2, f.size(), "two fragments");
        LayoutBox f1 = f.get(0);
        LayoutBox f2 = f.get(1);
        Check.near(46, f1.getHeight(), T, "first fragment: border 1 + padding 5 + two children (40)");
        Check.near(1, f1.getBorderTop(), T, "first fragment has the top border");
        Check.near(0, f1.getBorderBottom(), T, "but no bottom border");
        Check.near(0, f1.getPaddingBottom(), T, "and no bottom padding");
        Check.near(0, f2.getBorderTop(), T, "the second fragment has no top border");
        Check.near(1, f2.getBorderBottom(), T, "but the bottom border");
        Check.near(5, f2.getPaddingBottom(), T, "and the bottom padding");
        Check.near(46, f2.getHeight(), T, "second fragment: two children (40) + padding 5 + border 1");
        Check.near(100, f2.getX(), T, "second fragment in the second column");
        Check.near(0, f2.getY(), T, "at the top");
        Check.near(100 + 1 + 5, b(r, "p3").getX(), T, "p3 x: inside the content box of the box (border and padding)");
        Check.near(0, b(r, "p3").getY(), T, "p3 y: the continuation has no top edge");
        Check.near(20, b(r, "p4").getY(), T, "p4 y");
        Check.near(6, b(r, "p1").getY(), T, "p1 y: border 1 + padding 5");
        Check.near(46, b(r, "c").getHeight(), T, "container height");
    }

    /**
     * Boxes that cannot be split are kept whole in the column where they start; a column that is too short for them overflows.
     * <p>
     * 나눌 수 없는 상자는 시작한 열에 통째로 둔다. 그것에 비해 짧은 열은 넘친다.
     */
    public void testUnbreakableUnits() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;column-count:2;column-gap:0'><div id=a style='height:20px'></div>"
                + "<div id=f style='display:flex;height:50px'></div><div id=z style='height:20px'></div></div>"
                + "<div id=d style='width:200px;height:30px;column-count:2;column-gap:0'><div id=a2 style='height:20px'></div>"
                + "<div id=f2 style='display:flex;height:50px'></div><div id=z2 style='height:20px'></div></div>", 500);
        Check.near(0, b(r, "a").getX(), T, "a in the first column");
        Check.near(20, b(r, "f").getY(), T, "the unit is placed after a");
        Check.near(0, b(r, "f").getX(), T, "the unit is whole in the first column");
        Check.near(100, b(r, "z").getX(), T, "z in the second column");
        Check.near(70, b(r, "c").getHeight(), T, "column height");
        // d: 높이 30 에는 들어가지 않으므로 넘침 열이 생긴다.
        Check.equal(3, b(r, "d").getColumnCount(), "overflow columns");
        Check.near(50, b(r, "f2").getHeight(), T, "the tall unit keeps its height");
        Check.near(30, b(r, "d").getHeight(), T, "the container keeps the specified height");
    }

    /**
     * <code>max-height</code> limits the balanced column height; the rest goes into overflow columns.
     * <p>
     * <code>max-height</code> 는 균형 잡힌 열 높이를 제한하고 나머지는 넘침 열로 간다.
     */
    public void testMaxHeight() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;column-count:2;column-gap:0;max-height:30px'>" + blocks("b", 4, 20) + "</div>", 500);
        Check.near(30, b(r, "c").getHeight(), T, "the content does not fit two columns: the columns are as high as max-height");
        Check.equal(4, b(r, "c").getColumnCount(), "one block per column: two overflow columns");
        Check.near(300, b(r, "b4").getX(), T, "b4 in the fourth column");
    }

    /**
     * Column rules lie in the middle of the gaps between the columns, as high as the columns.
     * <p>
     * 열 구분선은 열 사이 gap 의 한가운데에 있고 열과 높이가 같다.
     */
    public void testColumnRules() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:220px;column-count:3;column-gap:20px;column-rule:4px solid red'>" + blocks("b", 3, 30) + "</div>"
                + "<div id=d style='width:220px;column-count:3;column-gap:20px;column-rule:4px none red'>" + blocks("d", 3, 30) + "</div>", 500);
        double[][] rules = b(r, "c").getColumnRules();
        Check.equal(2, rules.length, "two rules between three columns");
        // 열 너비 60: 첫 gap 은 60..80, 가운데 78..82.
        Check.near(68, rules[0][0], T, "first rule x: centered in the gap 60..80 (70 minus half the rule width)");
        Check.near(4, rules[0][2], T, "rule width");
        Check.near(30, rules[0][3], T, "rule height");
        Check.near(148, rules[1][0], T, "second rule x: the gap 140..160");
        Check.equal(0, b(r, "d").getColumnRules().length, "no rules for style none");
    }

    /**
     * A <code>column-span:all</code> child spans the whole container and splits the columns into sections that are balanced
     * separately.
     * <p>
     * <code>column-span:all</code> 자식은 컨테이너 전체에 걸치고 열을 따로 균형을 맞추는 구간으로 나눈다.
     */
    public void testColumnSpanAll() {
        LayoutResult r = LayoutTestSupport.layout("<style>.p{line-height:10px;margin:0}</style><div id=c style='width:200px;column-count:2;column-gap:0'>"
                + "<p class=p id=p1>" + lines(4) + "</p><h2 id=h style='column-span:all;height:10px;margin:0'></h2><p class=p id=p2>" + lines(6) + "</p></div>", 500);
        Check.near(200, b(r, "h").getWidth(), T, "the spanning box is as wide as the container");
        Check.near(20, b(r, "h").getY(), T, "below the first section (4 lines in 2 columns: 20)");
        Check.near(30, b(r, "p2").getY(), T, "the second section starts below the spanning box");
        List<LayoutBox> l = new java.util.ArrayList<LayoutBox>();
        // p2 는 두 조각으로 나뉘므로 모든 조각의 줄을 모은다.
        for (LayoutBox f : all(r, "p2")) {
            l.addAll(LayoutTestSupport.lines(f));
        }
        Check.equal(6, l.size(), "six lines in the two fragments");
        Check.near(100, l.get(3).getX(), T, "the second section is balanced on its own: 3 + 3 lines");
        Check.near(60, b(r, "c").getHeight(), T, "20 + 10 + 30");
    }

    /**
     * In a right-to-left container the first column is at the right.
     * <p>
     * 오른쪽에서 왼쪽 컨테이너에서는 첫 열이 오른쪽에 있다.
     */
    public void testRightToLeft() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;direction:rtl;column-count:2;column-gap:0'>" + blocks("b", 4, 10) + "</div>", 200);
        Check.near(100, b(r, "b1").getX(), T, "first column at the right");
        Check.near(0, b(r, "b3").getX(), T, "second column at the left");
    }

    /**
     * An inline-level or floated multi-column container is as wide as its columns of content (the number of columns of the content plus
     * the gaps).
     * <p>
     * 인라인 수준이나 float 된 다단 컨테이너의 너비는 내용 열들의 너비(내용의 열 수와 gap)이다.
     */
    public void testIntrinsicWidth() {
        LayoutResult r = LayoutTestSupport.layout("<div id=f style='float:left;column-count:2;column-gap:10px'><div style='width:30px;height:20px'></div></div>", 500);
        Check.near(70, b(r, "f").getWidth(), T, "2 columns of 30 and a gap of 10");
    }

    /**
     * A multi-column container beside a float and with margins that collapse through its parent is laid out correctly when it is laid
     * out a second time (the children of the first layout are restored).
     * <p>
     * float 옆에 있고 부모를 통해 margin 이 합쳐지는 다단 컨테이너는 두 번째로 배치할 때도 올바르다(첫 배치의 자식이 복원된다).
     */
    public void testSecondLayoutRun() {
        LayoutResult r = LayoutTestSupport.layout("<div style='float:left;width:50px;height:100px'></div><div id=o><div id=m style='margin-top:30px;width:200px;column-count:2;column-gap:0'>"
                + blocks("b", 4, 10) + "</div></div>", 500);
        Check.near(30, b(r, "m").getY(), T, "the margin of the container");
        Check.near(50, b(r, "m").getX(), T, "the container starts a formatting context: it sits beside the float");
        Check.near(30, b(r, "b1").getY(), T, "b1 y");
        Check.near(150, b(r, "b3").getX(), T, "b3 in the second column (50 + 100)");
        Check.near(30, b(r, "b3").getY(), T, "b3 y");
        Check.equal(2, b(r, "m").getChildren().size(), "two column boxes (no duplicates)");
    }

    /**
     * A multi-column container that is a flex item is laid out more than once (to measure it and with the used size); the second layout
     * starts from the original children and leaves exactly one set of columns.
     * <p>
     * 플렉스 항목인 다단 컨테이너는 여러 번 배치된다(측정과 사용 크기). 두 번째 배치는 원래 자식에서 시작하며 열은 한 벌만 남는다.
     */
    public void testLaidOutTwiceInsideFlexItem() {
        LayoutResult r = LayoutTestSupport.layout("<div style='display:flex;flex-direction:column;height:100px'><div id=m style='width:200px;flex-grow:1;column-count:2;column-gap:0'>"
                + blocks("b", 4, 10) + "</div></div>", 500);
        Check.equal(2, b(r, "m").getChildren().size(), "two column boxes");
        Check.near(100, b(r, "m").getHeight(), T, "grown to the container height");
        Check.near(100, b(r, "b3").getX(), T, "b3 in the second column");
        Check.near(0, b(r, "b3").getY(), T, "b3 y");
        Check.near(10, b(r, "b2").getY(), T, "b2 y");
    }

    /**
     * Hit testing finds the boxes in the columns where they are painted.
     * <p>
     * 히트 테스트는 상자를 그려진 열에서 찾는다.
     */
    public void testHitTesting() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='width:200px;column-count:2;column-gap:0'>" + blocks("b", 4, 10) + "</div>", 500);
        Check.same(b(r, "b1"), r.boxAt(10, 5), "b1");
        Check.same(b(r, "b2"), r.boxAt(10, 15), "b2");
        Check.same(b(r, "b3"), r.boxAt(110, 5), "b3 in the second column");
        Check.same(b(r, "b4"), r.boxAt(150, 15), "b4 in the second column");
    }

    /**
     * An empty container and a container without block content do not fail; text in a multi-column container is laid out in lines of
     * the column width.
     * <p>
     * 빈 컨테이너와 블록 내용이 없는 컨테이너는 실패하지 않는다. 다단 컨테이너 안의 텍스트는 열 너비의 줄로 배치된다.
     */
    public void testEmptyAndTextContainers() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='column-count:3'></div><div id=t style='width:" + (cw * 20) + "px;column-count:2;column-gap:0;line-height:10px'>"
                + "aaaaaaaaa bbbbbbbbb ccccccccc ddddddddd</div>", 500);
        Check.near(0, b(r, "a").getHeight(), T, "empty container");
        List<LayoutBox> l = LayoutTestSupport.lines(b(r, "t"));
        Check.equal(4, l.size(), "one word per line at the column width of 10 characters");
        Check.near(cw * 10, l.get(2).getX(), T, "the third line starts the second column");
        Check.near(20, b(r, "t").getHeight(), T, "balanced: two lines per column");
    }
}
