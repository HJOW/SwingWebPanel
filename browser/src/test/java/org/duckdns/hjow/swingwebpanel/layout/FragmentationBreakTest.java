package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the break properties in a multi-column container (plan item P10-08, CSS Fragmentation Module Level 3,
 * https://www.w3.org/TR/css-break-3/): a forced break from <code>break-before</code> and <code>break-after</code> (<code>column</code>,
 * <code>always</code>, <code>all</code> and legacy names), page and region values in screen columns, no break at the start of the
 * container, the propagation of the value of a first or last child,
 * <code>avoid</code> before and after a box and <code>break-inside: avoid</code> that moves a box to the next column when it would
 * be split.
 * <p>
 * 다단 컨테이너에서 나눔 속성 시험(계획 P10-08, CSS Fragmentation Module Level 3, https://www.w3.org/TR/css-break-3/): <code>break-before</code> 와
 * <code>break-after</code> 의 강제 나눔(<code>column</code>, <code>always</code>, <code>all</code>과 예전 이름), 화면의 쪽과 영역 값,
 * 컨테이너 시작에서는 나누지 않음, 첫 자식과 마지막 자식 값의 전파, 상자 앞뒤의 <code>avoid</code>,
 * 나뉘게 될 상자를 다음 열로 옮기는 <code>break-inside: avoid</code>.
 */
public class FragmentationBreakTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FragmentationBreakTest() {
    }

    /**
     * Builds a block of a height with an id and extra style.
     * <p>
     * id 와 추가 스타일을 가진 일정한 높이의 블록 마크업을 만든다.
     *
     * @param id    the id / id
     * @param h     the height / 높이
     * @param extra the extra style / 추가 스타일
     * @return the markup / 마크업
     */
    private static String blk(String id, int h, String extra) {
        return "<div id=" + id + " style='height:" + h + "px;" + extra + "'></div>";
    }

    /**
     * Lays out a multi-column container of three columns (100px wide, 10px gap) with the given content.
     * <p>
     * 주어진 내용을 가진 3 열(너비 100px, 간격 10px) 다단 컨테이너를 배치한다.
     *
     * @param extra the extra style of the container / 컨테이너의 추가 스타일
     * @param inner the content / 내용
     * @return the layout / 레이아웃
     */
    private static LayoutResult run(String extra, String inner) {
        return LayoutTestSupport.layout("<div id=c style='width:320px;column-count:3;column-gap:10px;" + extra + "'>" + inner + "</div>", 400);
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
     * Returns the box of an id.
     * <p>
     * id 의 상자를 반환한다.
     *
     * @param r  the result / 결과
     * @param id the id / id
     * @return the box / 상자
     */
    private static LayoutBox b(LayoutResult r, String id) {
        return LayoutTestSupport.box(r, id);
    }

    /**
     * A forced break before a box starts a new column at that point.
     * <p>
     * 상자 앞의 강제 나눔은 그 자리에서 새 열을 시작한다.
     */
    public void testBreakBeforeColumn() {
        LayoutResult r = run("", blk("b1", 20, "") + blk("b2", 20, "") + blk("b3", 20, "break-before:column") + blk("b4", 20, ""));
        Check.near(0, b(r, "b1").getX(), T, "b1 in the first column");
        Check.near(0, b(r, "b2").getX(), T, "b2 in the first column");
        Check.near(20, b(r, "b2").getY(), T, "b2 below b1");
        Check.near(110, b(r, "b3").getX(), T, "b3 starts the second column");
        Check.near(0, b(r, "b3").getY(), T, "b3 at the top of the column");
        Check.near(110, b(r, "b4").getX(), T, "b4 follows b3");
        Check.near(20, b(r, "b4").getY(), T, "b4 below b3");
        Check.near(40, b(r, "c").getHeight(), T, "the columns are as high as the taller segment");
    }

    /**
     * <code>break-after</code> on the previous box forces the same break; several breaks fill several columns.
     * <p>
     * 앞 상자의 <code>break-after</code> 도 같은 나눔을 강제하며 나눔이 여럿이면 여러 열을 채운다.
     */
    public void testBreakAfterAndSeveral() {
        LayoutResult r = run("", blk("b1", 20, "break-after:column") + blk("b2", 20, "break-after:column") + blk("b3", 20, ""));
        Check.near(0, b(r, "b1").getX(), T, "b1 in the first column");
        Check.near(110, b(r, "b2").getX(), T, "b2 in the second column");
        Check.near(220, b(r, "b3").getX(), T, "b3 in the third column");
        Check.near(0, b(r, "b3").getY(), T, "b3 at the top");
        Check.near(20, b(r, "c").getHeight(), T, "one box per column");
    }

    /**
     * There is no break at the start of the container, and generic and legacy column values force a break elsewhere.
     * <p>
     * 컨테이너 시작에서는 나누지 않으며 일반 값과 예전 열 나눔 값은 다른 위치에서 강제로 나눈다.
     */
    public void testStartValuesAndAliases() {
        // 열 높이가 고정이고 column-fill:auto 이면 내용이 첫 열을 채운다(균형을 맞추면 상자가 열마다 하나씩 놓인다).
        String fixed = "height:100px;column-fill:auto;";
        LayoutResult first = run(fixed, blk("b1", 20, "break-before:column") + blk("b2", 20, ""));
        Check.near(0, b(first, "b1").getX(), T, "no break before the first box");
        Check.near(0, b(first, "b2").getX(), T, "so b2 follows in the first column");
        String[] values = {"always", "all", "column"};
        // 열을 강제로 나누는 값마다 b2 가 둘째 열에서 시작하는지 확인한다.
        for (int i = 0; i < values.length; i++) {
            LayoutResult r = run(fixed, blk("b1", 20, "") + blk("b2", 20, "break-before:" + values[i]));
            Check.near(110, b(r, "b2").getX(), T, "break-before:" + values[i] + " forces a break");
        }
        LayoutResult webkit = run(fixed, blk("b1", 20, "-webkit-column-break-after:always") + blk("b2", 20, ""));
        Check.near(110, b(webkit, "b2").getX(), T, "-webkit-column-break-after:always");
        LayoutResult webkitBefore = run(fixed, blk("b1", 20, "") + blk("b2", 20, "--force:always;-webkit-column-break-before:var(--force)"));
        Check.equal("column", b(webkitBefore, "b2").getStyle().keyword(Prop.BREAK_BEFORE), "the column alias survives variable substitution");
        Check.near(110, b(webkitBefore, "b2").getX(), T, "the substituted column alias starts a column");
        LayoutResult none = run(fixed, blk("b1", 20, "break-after:auto") + blk("b2", 20, "break-before:avoid"));
        Check.near(0, b(none, "b2").getX(), T, "auto and avoid do not force a break");
        LayoutResult pageAvoid = run(fixed, blk("b1", 20, "break-after:avoid-page") + blk("b2", 20, "break-before:avoid-page"));
        Check.near(0, b(pageAvoid, "b2").getX(), T, "avoid-page does not force a break");
    }

    /**
     * Paged and regional breaks require a matching ancestor context. / 쪽과 영역 나눔에는 해당 종류의 조상 문맥이 필요하다.
     */
    public void testPageAndRegionBreaksLeaveScreenColumnsIntact() {
        String fixed = "height:100px;column-fill:auto;";
        String[] values = {"page", "left", "right", "recto", "verso", "region"};
        // 쪽·영역 분할 값은 앞과 뒤 어느 쪽에 지정해도 화면의 열을 나누지 않는다.
        for (String value : values) {
            LayoutResult before = run(fixed, blk("b1", 20, "") + blk("b2", 20, "break-before:" + value));
            Check.near(0, b(before, "b2").getX(), T, "break-before:" + value + " leaves the following box in the first column");
            LayoutResult after = run(fixed, blk("b1", 20, "break-after:" + value) + blk("b2", 20, ""));
            Check.near(0, b(after, "b2").getX(), T, "break-after:" + value + " leaves the following box in the first column");
        }
        CssStyleDeclaration declaration = CssStyleDeclaration.parse("--force:always;page-break-before:var(--force)", null, null);
        LayoutResult legacy = run(fixed, blk("b1", 20, "page-break-after:always") + blk("b2", 20, declaration.getCssText()));
        Check.equal("page", b(legacy, "b1").getStyle().keyword(Prop.BREAK_AFTER), "legacy always computes to page");
        Check.equal("page", b(legacy, "b2").getStyle().keyword(Prop.BREAK_BEFORE), "variable substitution preserves the page alias");
        Check.near(0, b(legacy, "b2").getX(), T, "legacy page values stay in the first column on screen");
    }

    /**
     * The break value of a first or last child applies to the box that contains it.
     * <p>
     * 첫 자식이나 마지막 자식의 나눔 값은 그것을 담은 상자에 적용된다.
     */
    public void testPropagation() {
        String fixed = "height:100px;column-fill:auto;";
        LayoutResult r = run(fixed, blk("b1", 20, "") + "<div id=wrap><div id=h style='height:20px;break-before:column'></div><div style='height:20px'></div></div>");
        Check.near(110, b(r, "wrap").getX(), T, "a break value of the first child moves the parent to the next column");
        Check.near(110, b(r, "h").getX(), T, "and the child with it");
        Check.near(0, b(r, "h").getY(), T, "at the top");
        LayoutResult a = run(fixed, "<div id=wrap><div style='height:20px'></div><div style='height:20px;break-after:column'></div></div>" + blk("b2", 20, ""));
        Check.near(110, b(a, "b2").getX(), T, "a break value of the last child applies after the parent");
    }

    /**
     * A forced column break reaches the outer block edge through other break declarations. / 강제 열 분할은 다른 분할 선언이 있어도 바깥 블록까지 전파된다.
     */
    public void testForcedChildBreakCombinesWithAncestorValues() {
        String fixed = "height:100px;column-fill:auto;";
        String nested = blk("edge", 20, "break-before:column");
        // 보통 블록을 여러 겹 쌓아도 첫 자식의 강제 분할이 맨 바깥 가장자리에 닿는다.
        for (int i = 0; i < 12; i++) {
            nested = "<div>" + nested + "</div>";
        }
        LayoutResult before = run(fixed, blk("prefix", 20, "break-after:avoid-column")
                + "<div id=wrap style='break-before:avoid-column'>" + nested + "</div>");
        Check.near(110, b(before, "wrap").getX(), T, "a deeply nested forced break overrides the avoidance at the same edge");
        Check.near(0, b(before, "edge").getY(), T, "the first child starts the column");
        LayoutResult after = run(fixed, "<div id=wrap style='break-after:page'>" + blk("edge", 20, "break-after:column") + "</div>"
                + blk("last", 20, ""));
        Check.near(110, b(after, "last").getX(), T, "the propagated column break also applies after the parent");
    }

    /**
     * A nested multicol context keeps its own column breaks. / 중첩 다단 문맥의 열 분할은 해당 문맥 안에서 처리한다.
     */
    public void testNestedColumnBreaksDoNotPropagateAcrossContext() {
        String fixed = "height:100px;column-fill:auto;";
        LayoutResult result = run(fixed, blk("prefix", 20, "")
                + "<div id=nested style='column-count:2;column-gap:0;height:40px;column-fill:auto'>"
                + blk("first", 10, "break-before:column") + blk("second", 10, "break-before:column;break-after:column")
                + "</div>" + blk("last", 20, ""));
        Check.near(0, b(result, "nested").getX(), T, "the inner context remains in the first outer column");
        Check.near(50, b(result, "second").getX(), T, "the inner break starts the second inner column");
        Check.near(0, b(result, "last").getX(), T, "the last inner child does not force an outer column break");
    }

    /**
     * <code>break-inside: avoid</code> moves a box that would be split to the next column when it fits there; without it the box is
     * split. A box that is taller than a column is split anyway.
     * <p>
     * <code>break-inside: avoid</code> 는 나뉘게 될 상자가 다음 열에 들어가면 그 열로 옮긴다. 없으면 상자는 나뉜다. 열보다 큰 상자는 어쨌든 나뉜다.
     */
    public void testBreakInsideAvoid() {
        String fixed = "height:100px;column-fill:auto;";
        String inner = "<div style='height:30px'></div><div style='height:30px'></div>";
        LayoutResult split = run(fixed, blk("b1", 60, "") + "<div id=b2>" + inner + "</div>");
        Check.equal(2, all(split, "b2").size(), "without break-inside the box is split between the columns");
        LayoutResult avoid = run(fixed, blk("b1", 60, "") + "<div id=b2 style='break-inside:avoid'>" + inner + "</div>");
        Check.equal(1, all(avoid, "b2").size(), "with break-inside:avoid it is not split");
        Check.near(110, b(avoid, "b2").getX(), T, "it moves to the second column");
        Check.near(0, b(avoid, "b2").getY(), T, "at the top");
        LayoutResult colAvoid = run(fixed, blk("b1", 60, "") + "<div id=b2 style='break-inside:avoid-column'>" + inner + "</div>");
        Check.equal(1, all(colAvoid, "b2").size(), "avoid-column is the same in columns");
        LayoutResult pageAvoid = run(fixed, blk("b1", 60, "") + "<div id=b2 style='break-inside:avoid-page'>" + inner + "</div>");
        Check.equal(2, all(pageAvoid, "b2").size(), "avoid-page does not avoid a break of the columns");
        // 열보다 큰 상자는 피할 수 없으므로 나뉜다.
        LayoutResult tall = run(fixed, "<div id=b2 style='break-inside:avoid'><div style='height:70px'></div><div style='height:70px'></div></div>");
        Check.equal(2, all(tall, "b2").size(), "a box taller than a column is split anyway");
        // 안쪽 줄 사이도 피한다: 상자의 줄이 한 열에 모두 들어가면 통째로 옮긴다.
        LayoutResult legacy = run(fixed, blk("b1", 60, "") + "<div id=b2 style='page-break-inside:avoid'>" + inner + "</div>");
        Check.equal(1, all(legacy, "b2").size(), "page-break-inside:avoid is the same");
    }

    /**
     * <code>avoid</code> before or after a box makes the engine break somewhere else when it can.
     * <p>
     * 상자 앞이나 뒤의 <code>avoid</code> 는 다른 곳에서 나눌 수 있으면 거기서 나누게 한다.
     */
    public void testAvoidBetweenBoxes() {
        String fixed = "height:100px;column-fill:auto;";
        LayoutResult plain = run(fixed, blk("b1", 40, "") + blk("b2", 40, "") + blk("b3", 40, ""));
        Check.near(0, b(plain, "b2").getX(), T, "without avoid b2 stays in the first column");
        Check.near(110, b(plain, "b3").getX(), T, "and b3 moves to the second column");
        LayoutResult after = run(fixed, blk("b1", 40, "") + blk("b2", 40, "break-after:avoid") + blk("b3", 40, ""));
        Check.near(110, b(after, "b2").getX(), T, "break-after:avoid on b2 moves b2 to the next column with b3");
        Check.near(110, b(after, "b3").getX(), T, "b3 stays with b2");
        LayoutResult before = run(fixed, blk("b1", 40, "") + blk("b2", 40, "") + blk("b3", 40, "break-before:avoid"));
        Check.near(110, b(before, "b2").getX(), T, "break-before:avoid on b3 moves b2 to the next column");
        // 피할 곳이 하나뿐이면 어쩔 수 없이 거기서 나눈다.
        LayoutResult only = run(fixed, blk("b1", 60, "break-after:avoid") + blk("b2", 60, ""));
        Check.near(110, b(only, "b2").getX(), T, "if the only break is a bad one it is used");
    }

    /**
     * The properties parse: the initial value is auto, the keywords are accepted and a wrong keyword is dropped.
     * <p>
     * 속성 파싱: 초기값은 auto 이고 키워드는 받아들여지며 틀린 키워드는 버려진다.
     */
    public void testParsing() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a></div><div id=b style='break-before:column;break-after:avoid-page;break-inside:avoid-region;box-decoration-break:clone'></div>"
                + "<div id=c style='break-before:bogus;break-inside:always'></div>", 400);
        Check.equal("auto", b(r, "a").getStyle().get(Prop.BREAK_BEFORE).toCssText(), "initial break-before");
        Check.equal("column", b(r, "b").getStyle().get(Prop.BREAK_BEFORE).toCssText(), "break-before:column");
        Check.equal("avoid-page", b(r, "b").getStyle().get(Prop.BREAK_AFTER).toCssText(), "break-after:avoid-page");
        Check.equal("avoid-region", b(r, "b").getStyle().get(Prop.BREAK_INSIDE).toCssText(), "break-inside:avoid-region");
        Check.equal("clone", b(r, "b").getStyle().get(Prop.BOX_DECORATION_BREAK).toCssText(), "box-decoration-break:clone is parsed");
        Check.equal("auto", b(r, "c").getStyle().get(Prop.BREAK_BEFORE).toCssText(), "a bogus keyword is dropped");
        Check.equal("auto", b(r, "c").getStyle().get(Prop.BREAK_INSIDE).toCssText(), "break-inside does not take always");
    }
}
