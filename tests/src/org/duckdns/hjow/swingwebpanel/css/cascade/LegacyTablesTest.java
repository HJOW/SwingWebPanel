package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the legacy table presentation (plan item P07-07-b-7, HTML5 2014 section 10.3.9): the user-agent border colors, the
 * <code>border</code>, <code>frame</code> and <code>rules</code> attributes, <code>cellpadding</code>, <code>cellspacing</code>, the
 * size, margin, color and alignment hints of tables, row groups, rows, cells, columns and captions, the quirks-mode exceptions and the
 * forced hiding of forms in tables. The computed styles are checked first, then the boxes and the painted pixels of a few pages.
 * <p>
 * 과거 방식의 표 표시 시험(계획 P07-07-b-7, HTML5 2014 §10.3.9): 사용자 에이전트의 border 색, <code>border</code>, <code>frame</code>, <code>rules</code>
 * 속성, <code>cellpadding</code>, <code>cellspacing</code>, 표/행 그룹/행/셀/열/캡션의 크기·여백·색·정렬 힌트, 호환 모드의 예외, 표 안 폼의 강제 숨김.
 * 계산 스타일을 먼저 확인하고 몇 개 페이지의 상자와 화소를 확인한다.
 */
public class LegacyTablesTest {

    /** The shared fixture. / 공용 도구. */
    private final LegacyFixture fx = new LegacyFixture();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyTablesTest() {
    }

    /**
     * Releases the CSS engine after each case.
     * <p>
     * 각 사례 뒤에 CSS 엔진을 해제한다.
     */
    public void tearDown() {
        fx.dispose();
    }

    /**
     * Returns the four border styles of an element as <code>top right bottom left</code>.
     * <p>
     * 요소의 네 border 스타일을 <code>top right bottom left</code> 로 반환한다.
     *
     * @param id the id / id
     * @return the four style keywords / 네 스타일 키워드
     */
    private String styles(String id) {
        ComputedStyle s = fx.style(id);
        return s.keyword(Prop.BORDER_TOP_STYLE) + " " + s.keyword(Prop.BORDER_RIGHT_STYLE) + " " + s.keyword(Prop.BORDER_BOTTOM_STYLE) + " "
                + s.keyword(Prop.BORDER_LEFT_STYLE);
    }

    /**
     * Returns the border width of one side of an element in pixels.
     * <p>
     * 요소의 한 변 border 두께를 픽셀로 반환한다.
     *
     * @param id   the id / id
     * @param prop the width property / 두께 속성
     * @return pixels / 픽셀
     */
    private double width(String id, int prop) {
        return fx.style(id).px(prop);
    }

    /**
     * The <code>border</code> attribute maps to the border width of the table and, when it is not equivalent to zero, to the outset
     * style and to a 1px inset border of the cells; an unparsable value means 1px.
     * <p>
     * <code>border</code> 속성은 표의 border 두께가 되고, 0 과 같지 않으면 outset 스타일과 셀의 1px inset border 가 된다. 해석할 수 없는 값은 1px 이다.
     */
    public void testBorderAttribute() {
        fx.init("<table id=t1 border=3><tr><td id=c1>x</td></tr></table>" + "<table id=t2 border><tr><td id=c2>x</td></tr></table>"
                + "<table id=t3 border=0><tr><td id=c3>x</td></tr></table>" + "<table id=t4 border=-1><tr><td id=c4>x</td></tr></table>"
                + "<table id=t5 border=' +2x'><tr><td id=c5>x</td></tr></table>" + "<table id=t6 border=abc><tr><td id=c6>x</td></tr></table>"
                + "<table id=t7><tr><td id=c7>x</td></tr></table>" + "<table id=t8 border='-0'><tr><td id=c8>x</td></tr></table>", "");
        Check.equal("outset outset outset outset", styles("t1"), "border=3 style");
        Check.near(3, width("t1", Prop.BORDER_TOP_WIDTH), 0.001, "border=3 width");
        Check.near(3, width("t1", Prop.BORDER_LEFT_WIDTH), 0.001, "border=3 left width");
        Check.equal("inset inset inset inset", styles("c1"), "cell of border=3");
        Check.near(1, width("c1", Prop.BORDER_TOP_WIDTH), 0.001, "cell border width");
        Check.equal("outset outset outset outset", styles("t2"), "empty border attribute is an error: outset");
        Check.near(1, width("t2", Prop.BORDER_TOP_WIDTH), 0.001, "empty border attribute is 1px");
        Check.equal("inset inset inset inset", styles("c2"), "cell of an empty border attribute");
        Check.equal("none none none none", styles("t3"), "border=0 has no style");
        Check.near(0, width("t3", Prop.BORDER_TOP_WIDTH), 0.001, "border=0 width");
        Check.equal("none none none none", styles("c3"), "cell of border=0");
        Check.equal("outset outset outset outset", styles("t4"), "negative is an error: outset");
        Check.near(1, width("t4", Prop.BORDER_TOP_WIDTH), 0.001, "negative border is 1px");
        Check.equal("inset inset inset inset", styles("c4"), "cell of a negative border");
        Check.near(2, width("t5", Prop.BORDER_TOP_WIDTH), 0.001, "leading space, sign and trailing text");
        Check.equal("outset outset outset outset", styles("t5"), "t5 style");
        Check.near(1, width("t6", Prop.BORDER_TOP_WIDTH), 0.001, "border=abc is 1px");
        Check.equal("outset outset outset outset", styles("t6"), "t6 style");
        Check.equal("none none none none", styles("t7"), "no border attribute");
        Check.equal("none none none none", styles("c7"), "cell without a border attribute");
        Check.equal("none none none none", styles("t8"), "border=-0 is zero");
        Check.equal("none none none none", styles("c8"), "cell of border=-0");
    }

    /**
     * The border hints of a table apply to the cells of that table only, through row groups, and not to the cells of a nested table.
     * <p>
     * 표의 border 힌트는 행 그룹을 거치는 그 표의 셀에만 적용되고 중첩된 표의 셀에는 적용되지 않는다.
     */
    public void testBorderHintsStayWithTheirTable() {
        fx.init("<table id=o border=2 cellpadding=7><thead><tr><th id=h>x</th></tr></thead><tbody><tr><td id=oc>"
                + "<table id=i><tr><td id=ic>x</td></tr></table></td></tr></tbody><tfoot><tr><td id=f>x</td></tr></tfoot></table>", "");
        Check.equal("inset inset inset inset", styles("oc"), "outer body cell");
        Check.equal("inset inset inset inset", styles("h"), "header cell");
        Check.equal("inset inset inset inset", styles("f"), "footer cell");
        Check.near(7, fx.style("oc").px(Prop.PADDING_TOP), 0.001, "outer cell padding");
        Check.near(7, fx.style("f").px(Prop.PADDING_LEFT), 0.001, "footer cell padding");
        Check.equal("none none none none", styles("i"), "nested table has no border");
        Check.equal("none none none none", styles("ic"), "nested cell has no border");
        Check.near(1, fx.style("ic").px(Prop.PADDING_TOP), 0.001, "nested cell keeps the UA padding");
    }

    /**
     * Tables and cells have the gray user-agent border color, rows and row groups inherit it, and <code>bordercolor</code> colors the
     * table. A <code>rules</code> table draws its cell borders in black.
     * <p>
     * 표와 셀은 회색 UA border 색을 갖고 행과 행 그룹은 상속하며 <code>bordercolor</code> 는 표에 색을 준다. <code>rules</code> 표의 셀 border 는 검정이다.
     */
    public void testBorderColors() {
        fx.init("<table id=a border=2 bordercolor=red><tr><td id=ac>x</td></tr></table>" + "<table id=b><tbody id=bb><tr id=br><td id=bc>x</td></tr></tbody></table>"
                + "<table id=c style='border-color:red'><tbody id=cb><tr id=cr><td id=cc>x</td></tr></tbody></table>"
                + "<table id=d bordercolor=''><tr><td>x</td></tr></table>" + "<table id=e bordercolor=blue><tr><td id=ec>x</td></tr></table>"
                + "<table id=f rules=all><tr><td id=fc>x</td></tr></table>", "");
        Check.equal(0xFFFF0000, fx.style("a").color(Prop.BORDER_TOP_COLOR), "bordercolor top");
        Check.equal(0xFFFF0000, fx.style("a").color(Prop.BORDER_LEFT_COLOR), "bordercolor left");
        Check.equal(0xFF808080, fx.style("ac").color(Prop.BORDER_TOP_COLOR), "cell stays gray");
        Check.equal(0xFF808080, fx.style("b").color(Prop.BORDER_TOP_COLOR), "table gray");
        Check.equal(0xFF808080, fx.style("bc").color(Prop.BORDER_BOTTOM_COLOR), "cell gray");
        Check.equal(0xFF808080, fx.style("bb").color(Prop.BORDER_TOP_COLOR), "row group inherits");
        Check.equal(0xFF808080, fx.style("br").color(Prop.BORDER_TOP_COLOR), "row inherits");
        Check.equal(0xFFFF0000, fx.style("cb").color(Prop.BORDER_TOP_COLOR), "row group inherits the table's author color");
        Check.equal(0xFFFF0000, fx.style("cr").color(Prop.BORDER_RIGHT_COLOR), "row inherits the table's author color");
        Check.equal(0xFF808080, fx.style("cc").color(Prop.BORDER_TOP_COLOR), "cell ignores the table's author color");
        Check.equal(0xFF808080, fx.style("d").color(Prop.BORDER_TOP_COLOR), "empty bordercolor is an error");
        Check.equal(0xFF0000FF, fx.style("e").color(Prop.BORDER_BOTTOM_COLOR), "bordercolor works without the border attribute");
        Check.equal(0xFF000000, fx.style("f").color(Prop.BORDER_TOP_COLOR), "rules table is black");
        Check.equal(0xFF000000, fx.style("fc").color(Prop.BORDER_TOP_COLOR), "rules cell is black");
    }

    /**
     * Every value of <code>frame</code> shows its sides in outset and hides the others; the values are matched without regard to case.
     * <p>
     * <code>frame</code> 의 값마다 해당 변은 outset 으로 보이고 나머지는 숨겨진다. 값은 대소문자를 구분하지 않고 맞춘다.
     */
    public void testFrameAttribute() {
        String[] frames = { "void", "above", "below", "hsides", "lhs", "rhs", "vsides", "box", "border", "BOX", "bogus", "" };
        String[] expected = { "hidden hidden hidden hidden", "outset hidden hidden hidden", "hidden hidden outset hidden",
                "outset hidden outset hidden", "hidden hidden hidden outset", "hidden outset hidden hidden", "hidden outset hidden outset",
                "outset outset outset outset", "outset outset outset outset", "outset outset outset outset", "none none none none",
                "none none none none" };
        StringBuilder markup = new StringBuilder();
        // 값마다 표를 하나씩 만든다.
        for (int i = 0; i < frames.length; i++) {
            markup.append("<table id=t").append(i).append(" frame='").append(frames[i]).append("'><tr><td>x</td></tr></table>");
        }
        fx.init(markup.toString(), "");
        // 값마다 네 변의 스타일을 확인한다.
        for (int i = 0; i < frames.length; i++) {
            Check.equal(expected[i], styles("t" + i), "frame='" + frames[i] + "'");
        }
    }

    /**
     * <code>frame</code> and <code>border</code> work together: the border attribute gives the width and the frame chooses the sides.
     * <p>
     * <code>frame</code> 과 <code>border</code> 는 함께 동작한다: border 속성이 두께를 주고 frame 이 변을 고른다.
     */
    public void testFrameWithBorderWidth() {
        fx.init("<table id=a border=4 frame=above><tr><td id=ac>x</td></tr></table><table id=b border=0 frame=below><tr><td>x</td></tr></table>", "");
        Check.equal("outset hidden hidden hidden", styles("a"), "frame above with border=4");
        Check.near(4, width("a", Prop.BORDER_TOP_WIDTH), 0.001, "top width");
        Check.equal("inset inset inset inset", styles("ac"), "cells still have inset borders");
        Check.equal("hidden hidden outset hidden", styles("b"), "frame below with border=0");
    }

    /**
     * The five values of <code>rules</code> make the table a collapsed table with a hidden outer border and choose which inner lines the
     * cells, rows and groups draw.
     * <p>
     * <code>rules</code> 의 다섯 값은 표를 바깥 border 가 숨겨진 병합 표로 만들고 셀, 행, 그룹이 그리는 안쪽 선을 고른다.
     */
    public void testRulesAttribute() {
        String[] rules = { "none", "groups", "rows", "cols", "all", "ALL" };
        StringBuilder markup = new StringBuilder();
        // 값마다 표를 하나씩 만든다.
        for (int i = 0; i < rules.length; i++) {
            markup.append("<table id=t").append(i).append(" rules='").append(rules[i]).append("'><colgroup id=g").append(i).append("><col></colgroup>")
                    .append("<thead id=h").append(i).append("><tr id=hr").append(i).append("><th id=hc").append(i).append(">x</th></tr></thead>")
                    .append("<tbody id=b").append(i).append("><tr id=r").append(i).append("><td id=c").append(i).append(">x</td></tr></tbody></table>");
        }
        markup.append("<table id=bad rules=bogus><tr><td id=badc>x</td></tr></table>");
        fx.init(markup.toString(), "");
        String[] cell = { "none none none none", "none none none none", "none none none none", "none solid none solid", "solid solid solid solid",
                "solid solid solid solid" };
        // 값마다 표, 셀, 그룹, 행의 스타일을 확인한다.
        for (int i = 0; i < rules.length; i++) {
            String v = "rules='" + rules[i] + "'";
            Check.equal("hidden hidden hidden hidden", styles("t" + i), v + " table");
            Check.equal("collapse", fx.style("t" + i).keyword(Prop.BORDER_COLLAPSE), v + " collapses the borders");
            Check.equal(cell[i], styles("c" + i), v + " body cell");
            Check.equal(cell[i], styles("hc" + i), v + " header cell");
            if (cell[i].indexOf("solid") >= 0) {
                Check.near(1, width("c" + i, Prop.BORDER_LEFT_WIDTH), 0.001, v + " cell line width");
            }
            Check.equal(rules[i].equalsIgnoreCase("groups") ? "solid" : "none", fx.style("b" + i).keyword(Prop.BORDER_TOP_STYLE), v + " body group top");
            Check.equal(rules[i].equalsIgnoreCase("groups") ? "solid" : "none", fx.style("h" + i).keyword(Prop.BORDER_BOTTOM_STYLE), v + " head group bottom");
            Check.equal(rules[i].equalsIgnoreCase("groups") ? "solid" : "none", fx.style("g" + i).keyword(Prop.BORDER_LEFT_STYLE), v + " column group left");
            Check.equal(rules[i].equalsIgnoreCase("groups") ? "solid" : "none", fx.style("g" + i).keyword(Prop.BORDER_RIGHT_STYLE), v + " column group right");
            Check.equal(rules[i].equalsIgnoreCase("rows") ? "solid" : "none", fx.style("r" + i).keyword(Prop.BORDER_TOP_STYLE), v + " row top");
            Check.equal(rules[i].equalsIgnoreCase("rows") ? "solid" : "none", fx.style("hr" + i).keyword(Prop.BORDER_BOTTOM_STYLE), v + " head row bottom");
        }
        Check.equal("separate", fx.style("bad").keyword(Prop.BORDER_COLLAPSE), "an unknown rules value is ignored");
        Check.equal("none none none none", styles("bad"), "bad table style");
        Check.equal("none none none none", styles("badc"), "bad cell style");
    }

    /**
     * The <code>border</code> style hint follows the <code>rules</code> style hint, so a table with both keeps an outset outer border.
     * <p>
     * <code>border</code> 스타일 힌트는 <code>rules</code> 스타일 힌트 뒤에 오므로 둘 다 가진 표는 바깥 border 가 outset 으로 남는다.
     */
    public void testBorderAndRulesTogether() {
        fx.init("<table id=t border=2 rules=cols><tr><td id=c>x</td></tr></table>", "");
        Check.equal("outset outset outset outset", styles("t"), "outer border of border+rules");
        Check.equal("collapse", fx.style("t").keyword(Prop.BORDER_COLLAPSE), "collapsed");
        Check.equal("none solid none solid", styles("c"), "cell lines of rules=cols override the inset border");
    }

    /**
     * <code>cellpadding</code> pads every cell of the table, <code>cellspacing</code> sets the border spacing; both use the
     * non-negative integer rules and ignore errors.
     * <p>
     * <code>cellpadding</code> 은 표의 모든 셀에 안쪽 여백을 주고 <code>cellspacing</code> 은 border 간격을 정한다. 둘 다 음이 아닌 정수 규칙을 쓰고 오류는 무시한다.
     */
    public void testCellPaddingAndSpacing() {
        fx.init("<table id=a cellpadding=5 cellspacing=3><thead><tr><th id=ah>x</th></tr></thead><tr><td id=ad>x</td></tr></table>"
                + "<table id=b cellpadding=' +2' cellspacing='0'><tr><td id=bd>x</td></tr></table>"
                + "<table id=c cellpadding=-1 cellspacing=-2><tr><td id=cd>x</td></tr></table>"
                + "<table id=d cellpadding=abc cellspacing=''><tr><td id=dd>x</td></tr></table>"
                + "<table id=e cellpadding=0><tr><td id=ed>x</td></tr></table>", "");
        Check.near(5, fx.style("ah").px(Prop.PADDING_TOP), 0.001, "th padding top");
        Check.near(5, fx.style("ad").px(Prop.PADDING_RIGHT), 0.001, "td padding right");
        Check.near(5, fx.style("ad").px(Prop.PADDING_BOTTOM), 0.001, "td padding bottom");
        Check.near(5, fx.style("ad").px(Prop.PADDING_LEFT), 0.001, "td padding left");
        Check.equal("3px", fx.style("a").getPropertyValue("border-spacing"), "cellspacing=3");
        Check.near(2, fx.style("bd").px(Prop.PADDING_TOP), 0.001, "sign and space are accepted");
        Check.equal("0px", fx.style("b").getPropertyValue("border-spacing"), "cellspacing=0");
        Check.near(1, fx.style("cd").px(Prop.PADDING_TOP), 0.001, "negative padding keeps the UA padding");
        Check.equal("2px", fx.style("c").getPropertyValue("border-spacing"), "negative spacing keeps the UA spacing");
        Check.near(1, fx.style("dd").px(Prop.PADDING_TOP), 0.001, "garbage padding keeps the UA padding");
        Check.equal("2px", fx.style("d").getPropertyValue("border-spacing"), "empty spacing keeps the UA spacing");
        Check.near(0, fx.style("ed").px(Prop.PADDING_TOP), 0.001, "cellpadding=0");
    }

    /**
     * Width, height, margins, colors and background of a table, row, cell and column map as the section lists them.
     * <p>
     * 표, 행, 셀, 열의 너비, 높이, 여백, 색, 배경은 절의 목록대로 대응한다.
     */
    public void testSizeMarginAndColorHints() {
        fx.init("<table id=t width=150 height=90 hspace=7 vspace=4 bgcolor=#ff0000 background=bg.png><colgroup id=cg><col id=col width=30></colgroup>"
                + "<thead id=th height=11 bgcolor=#00ff00 background=h.png><tr id=hr height=12 bgcolor=#0000ff background=r.png><td id=hc width=60 height=40 bgcolor=#112233 background=c.png>x</td></tr></thead>"
                + "<tbody id=tb height=13><tr id=r height=30%><td id=c>x</td></tr></tbody></table>"
                + "<table id=p width=50% hspace=5% vspace=abc height=abc><tr><td>x</td></tr></table>", "");
        Check.near(150, fx.style("t").px(Prop.WIDTH), 0.001, "table width");
        Check.near(90, fx.style("t").px(Prop.HEIGHT), 0.001, "table height");
        Check.near(7, fx.style("t").px(Prop.MARGIN_LEFT), 0.001, "hspace left");
        Check.near(7, fx.style("t").px(Prop.MARGIN_RIGHT), 0.001, "hspace right");
        Check.near(4, fx.style("t").px(Prop.MARGIN_TOP), 0.001, "vspace top");
        Check.near(4, fx.style("t").px(Prop.MARGIN_BOTTOM), 0.001, "vspace bottom");
        Check.equal(0xFFFF0000, fx.style("t").color(Prop.BACKGROUND_COLOR), "table bgcolor");
        Check.isTrue(fx.style("t").getPropertyValue("background-image").indexOf("bg.png") >= 0, "table background");
        Check.near(30, fx.style("col").px(Prop.WIDTH), 0.001, "col width");
        Check.equal(0xFF00FF00, fx.style("th").color(Prop.BACKGROUND_COLOR), "thead bgcolor");
        Check.isTrue(fx.style("th").getPropertyValue("background-image").indexOf("h.png") >= 0, "thead background");
        Check.isTrue(Double.isNaN(fx.style("th").px(Prop.HEIGHT)), "thead height is not mapped");
        Check.isTrue(Double.isNaN(fx.style("tb").px(Prop.HEIGHT)), "tbody height is not mapped");
        Check.equal(0xFF0000FF, fx.style("hr").color(Prop.BACKGROUND_COLOR), "tr bgcolor");
        Check.isTrue(fx.style("hr").getPropertyValue("background-image").indexOf("r.png") >= 0, "tr background");
        Check.near(12, fx.style("hr").px(Prop.HEIGHT), 0.001, "tr height");
        Check.equal("30%", fx.style("r").getPropertyValue("height"), "tr height percentage");
        Check.near(60, fx.style("hc").px(Prop.WIDTH), 0.001, "td width");
        Check.near(40, fx.style("hc").px(Prop.HEIGHT), 0.001, "td height");
        Check.equal(0xFF112233, fx.style("hc").color(Prop.BACKGROUND_COLOR), "td bgcolor");
        Check.isTrue(fx.style("hc").getPropertyValue("background-image").indexOf("c.png") >= 0, "td background");
        Check.equal("50%", fx.style("p").getPropertyValue("width"), "percentage width");
        Check.equal("5%", fx.style("p").getPropertyValue("margin-left"), "percentage hspace");
        Check.near(0, fx.style("p").px(Prop.MARGIN_TOP), 0.001, "garbage vspace");
        Check.equal("auto", fx.style("p").getPropertyValue("height"), "garbage height");
    }

    /**
     * Table alignment: left and right float the table, only center centers it; the row groups, rows and cells align their text; the
     * caption is centered unless told otherwise and <code>bottom</code> moves it below the table.
     * <p>
     * 표 정렬: left 와 right 는 표를 띄우고 center 만 가운데에 두며, 행 그룹, 행, 셀은 글을 정렬하고 캡션은 달리 말하지 않으면 가운데이며
     * <code>bottom</code> 은 표 아래로 옮긴다.
     */
    public void testAlignmentHints() {
        fx.init("<table id=l align=left><tr><td>x</td></tr></table><table id=r align=RIGHT><tr><td>x</td></tr></table>"
                + "<table id=c align=center><tr><td>x</td></tr></table><table id=m align=middle><tr><td>x</td></tr></table>"
                + "<table id=g><caption id=cap1>x</caption><caption id=cap2 align=right>x</caption><caption id=cap3 align=BOTTOM>x</caption>"
                + "<caption id=cap4 align=left>x</caption><caption id=cap5 align=justify>x</caption><caption id=cap6 align=middle>x</caption>"
                + "<caption id=cap7 align=top>x</caption><caption id=cap8 align=bogus>x</caption>"
                + "<thead id=h1 align=left><tr id=r1 align=right><td id=d1 align=center>x</td><th id=d2 align=middle>x</th>"
                + "<td id=d3 align=absmiddle>x</td><td id=d4 align=justify>x</td><td id=d5 align=bogus>x</td><td id=d6>x</td><th id=d7>x</th></tr></thead>"
                + "<tbody id=h2 align=middle><tr><td id=e1>x</td></tr></tbody><tfoot id=h3 align=ABSMIDDLE><tr><td id=e2>x</td></tr></tfoot></table>", "");
        Check.equal("left", fx.style("l").keyword(Prop.FLOAT), "table align=left");
        Check.equal("right", fx.style("r").keyword(Prop.FLOAT), "table align=RIGHT");
        Check.equal("none", fx.style("c").keyword(Prop.FLOAT), "table align=center does not float");
        Check.equal("auto", fx.style("c").getPropertyValue("margin-left"), "table align=center margin left");
        Check.equal("auto", fx.style("c").getPropertyValue("margin-right"), "table align=center margin right");
        Check.equal("none", fx.style("m").keyword(Prop.FLOAT), "table align=middle does not float");
        Check.near(0, fx.style("m").px(Prop.MARGIN_LEFT), 0.001, "table align=middle is not centered");
        Check.equal("center", fx.style("cap1").keyword(Prop.TEXT_ALIGN), "caption is centered");
        Check.equal("right", fx.style("cap2").keyword(Prop.TEXT_ALIGN), "caption align=right");
        Check.equal("center", fx.style("cap3").keyword(Prop.TEXT_ALIGN), "caption align=bottom still centers the text");
        Check.equal("bottom", fx.style("cap3").keyword(Prop.CAPTION_SIDE), "caption align=bottom");
        Check.equal("left", fx.style("cap4").keyword(Prop.TEXT_ALIGN), "caption align=left");
        Check.equal("justify", fx.style("cap5").keyword(Prop.TEXT_ALIGN), "caption align=justify");
        Check.equal("center", fx.style("cap6").keyword(Prop.TEXT_ALIGN), "caption align=middle");
        Check.equal("top", fx.style("cap7").keyword(Prop.CAPTION_SIDE), "caption align=top");
        Check.equal("center", fx.style("cap8").keyword(Prop.TEXT_ALIGN), "caption align=bogus keeps the centered text");
        Check.equal("top", fx.style("cap1").keyword(Prop.CAPTION_SIDE), "caption side default");
        Check.equal("left", fx.style("h1").keyword(Prop.TEXT_ALIGN), "thead align=left");
        Check.equal("right", fx.style("r1").keyword(Prop.TEXT_ALIGN), "tr align=right");
        Check.equal("center", fx.style("d1").keyword(Prop.TEXT_ALIGN), "td align=center");
        Check.equal("center", fx.style("d2").keyword(Prop.TEXT_ALIGN), "th align=middle");
        Check.equal("center", fx.style("d3").keyword(Prop.TEXT_ALIGN), "td align=absmiddle");
        Check.equal("justify", fx.style("d4").keyword(Prop.TEXT_ALIGN), "td align=justify");
        Check.equal("right", fx.style("d5").keyword(Prop.TEXT_ALIGN), "td align=bogus inherits the row");
        Check.equal("right", fx.style("d6").keyword(Prop.TEXT_ALIGN), "td inherits");
        Check.equal("right", fx.style("d7").keyword(Prop.TEXT_ALIGN), "th with an aligned parent inherits it");
        Check.equal("center", fx.style("e1").keyword(Prop.TEXT_ALIGN), "tbody align=middle");
        Check.equal("center", fx.style("e2").keyword(Prop.TEXT_ALIGN), "tfoot align=ABSMIDDLE");
    }

    /**
     * A <code>th</code> centers its text unless the parent already has a non-initial <code>text-align</code>, in which case it
     * inherits the parent's value.
     * <p>
     * <code>th</code> 는 부모의 <code>text-align</code> 이 초기값일 때만 글을 가운데 정렬하며, 아니면 부모의 값을 상속한다.
     */
    public void testHeaderCellTextAlign() {
        fx.init("<table><tr><th id=a>x</th></tr></table><table style='text-align:right'><tr><th id=b>x</th></tr></table>"
                + "<table><tr style='text-align:left'><th id=c>x</th></tr></table><table><tr><th id=d style='text-align:left'>x</th></tr></table>"
                + "<table><tr style='text-align:start'><th id=e>x</th></tr></table><table><tr><td id=f>x</td></tr></table>", "");
        Check.equal("center", fx.style("a").keyword(Prop.TEXT_ALIGN), "th default");
        Check.equal("right", fx.style("b").keyword(Prop.TEXT_ALIGN), "table text-align reaches the th");
        Check.equal("left", fx.style("c").keyword(Prop.TEXT_ALIGN), "row text-align reaches the th");
        Check.equal("left", fx.style("d").keyword(Prop.TEXT_ALIGN), "own author value");
        Check.equal("center", fx.style("e").keyword(Prop.TEXT_ALIGN), "start is the initial value");
        Check.equal("start", fx.style("f").keyword(Prop.TEXT_ALIGN), "td stays start");
    }

    /**
     * <code>valign</code> maps the four keywords on row groups, rows and cells, ignoring case; cells inherit the row's value.
     * <p>
     * <code>valign</code> 은 행 그룹, 행, 셀에서 네 키워드를 대소문자 구분 없이 대응시키며 셀은 행의 값을 상속한다.
     */
    public void testVerticalAlignHints() {
        fx.init("<table><thead id=a valign=TOP><tr><td id=a1>x</td></tr></thead><tbody id=b valign=bottom><tr id=b1 valign=baseline>"
                + "<td id=b2>x</td><td id=b3 valign=middle>x</td><td id=b4 valign=bogus>x</td></tr></tbody>"
                + "<tfoot><tr id=c valign=middle><th id=c1 valign=top>x</th><td id=c2>x</td></tr></tfoot></table>", "");
        Check.equal("top", fx.style("a").keyword(Prop.VERTICAL_ALIGN), "thead valign");
        Check.equal("top", fx.style("a1").keyword(Prop.VERTICAL_ALIGN), "cell inherits the group's value through the row");
        Check.equal("bottom", fx.style("b").keyword(Prop.VERTICAL_ALIGN), "tbody valign");
        Check.equal("baseline", fx.style("b1").keyword(Prop.VERTICAL_ALIGN), "tr valign");
        Check.equal("baseline", fx.style("b2").keyword(Prop.VERTICAL_ALIGN), "cell inherits the row");
        Check.equal("middle", fx.style("b3").keyword(Prop.VERTICAL_ALIGN), "td valign");
        Check.equal("baseline", fx.style("b4").keyword(Prop.VERTICAL_ALIGN), "bogus valign");
        Check.equal("top", fx.style("c1").keyword(Prop.VERTICAL_ALIGN), "th valign");
        Check.equal("middle", fx.style("c2").keyword(Prop.VERTICAL_ALIGN), "td inherits tr valign");
    }

    /**
     * <code>nowrap</code> keeps a cell on one line, except that in quirks mode a cell that also has a length width wraps normally.
     * <p>
     * <code>nowrap</code> 은 셀을 한 줄에 두지만, 호환 모드에서 길이 너비도 가진 셀은 보통대로 줄바꿈한다.
     */
    public void testNowrap() {
        String markup = "<table><tr><td id=a nowrap>x</td><td id=b nowrap width=50>x</td><td id=c nowrap width=50%>x</td>"
                + "<td id=d nowrap width=abc>x</td><th id=e nowrap width=50>x</th><td id=f width=50>x</td><td id=g nowrap width=' 3.5 px'>x</td></tr></table>";
        fx.init(markup, "");
        Check.equal("nowrap", fx.style("a").keyword(Prop.WHITE_SPACE), "standards: nowrap");
        Check.equal("nowrap", fx.style("b").keyword(Prop.WHITE_SPACE), "standards: nowrap with a width");
        Check.equal("nowrap", fx.style("e").keyword(Prop.WHITE_SPACE), "standards: th nowrap with a width");
        fx.initQuirks(markup, "");
        Check.equal("nowrap", fx.style("a").keyword(Prop.WHITE_SPACE), "quirks: nowrap alone");
        Check.equal("normal", fx.style("b").keyword(Prop.WHITE_SPACE), "quirks: nowrap with a length width");
        Check.equal("nowrap", fx.style("c").keyword(Prop.WHITE_SPACE), "quirks: nowrap with a percentage width");
        Check.equal("nowrap", fx.style("d").keyword(Prop.WHITE_SPACE), "quirks: nowrap with an invalid width");
        Check.equal("normal", fx.style("e").keyword(Prop.WHITE_SPACE), "quirks: th nowrap with a length width");
        Check.equal("normal", fx.style("f").keyword(Prop.WHITE_SPACE), "quirks: width alone");
        Check.equal("normal", fx.style("g").keyword(Prop.WHITE_SPACE), "quirks: width parsed by the dimension rules");
    }

    /**
     * In quirks mode a table does not inherit the font properties, the line height, the white space and the text alignment of the
     * surrounding text; in standards mode it does.
     * <p>
     * 호환 모드의 표는 주변 글의 글꼴 속성, 줄 높이, 공백 처리, 글 정렬을 상속하지 않고 표준 모드에서는 상속한다.
     */
    public void testQuirksTableFontReset() {
        String markup = "<div id=d style='font-size:20px;font-weight:bold;font-style:italic;line-height:2;white-space:pre;text-align:right;"
                + "font-variant:small-caps;color:red'><table id=t><tr><td id=c>x</td></tr></table></div>";
        fx.init(markup, "");
        Check.near(20, fx.style("t").fontSizePx(), 0.001, "standards: font size");
        Check.equal(700, fx.style("t").fontWeight(), "standards: bold");
        Check.equal("italic", fx.style("t").keyword(Prop.FONT_STYLE), "standards: italic");
        Check.equal("pre", fx.style("t").keyword(Prop.WHITE_SPACE), "standards: white-space");
        Check.equal("right", fx.style("t").keyword(Prop.TEXT_ALIGN), "standards: text-align");
        fx.initQuirks(markup, "");
        Check.near(16, fx.style("t").fontSizePx(), 0.001, "quirks: font size is medium");
        Check.equal(400, fx.style("t").fontWeight(), "quirks: weight reset");
        Check.equal("normal", fx.style("t").keyword(Prop.FONT_STYLE), "quirks: style reset");
        Check.equal("normal", fx.style("t").keyword(Prop.FONT_VARIANT_CAPS), "quirks: variant reset");
        Check.equal("normal", fx.style("t").keyword(Prop.WHITE_SPACE), "quirks: white-space reset");
        Check.equal("start", fx.style("t").keyword(Prop.TEXT_ALIGN), "quirks: text-align reset");
        Check.isTrue("normal".equals(fx.style("t").keyword(Prop.LINE_HEIGHT)), "quirks: line-height reset");
        Check.near(16, fx.style("c").fontSizePx(), 0.001, "quirks: the cell inherits from the table");
        Check.equal(0xFF000000, fx.style("t").color(Prop.COLOR), "quirks: a table takes the color of the body, not of the surrounding div (Quirks Mode Standard 3.12)");
    }

    /**
     * In HTML documents a form that is a child of a table, a row group or a row is not displayed whatever CSS says; other forms are.
     * <p>
     * HTML 문서에서 표, 행 그룹, 행의 자식인 폼은 CSS 와 상관없이 표시되지 않고 다른 폼은 표시된다.
     */
    public void testFormsInTablesAreHidden() {
        fx.init("<table id=t><form id=f0></form><tbody id=b><form id=f1></form><tr id=r><form id=f2></form><td id=c><form id=f3>x</form></td></tr></tbody></table>"
                + "<div><form id=f4>x</form></div>", "form{display:block !important}");
        Check.equal("none", fx.style("f0").display(), "form child of table");
        Check.equal("none", fx.style("f1").display(), "form child of tbody");
        Check.equal("none", fx.style("f2").display(), "form child of tr");
        Check.equal("block", fx.style("f3").display(), "form inside a cell");
        Check.equal("block", fx.style("f4").display(), "form outside tables");
        Check.equal("table-row", fx.style("r").display(), "the row is unaffected");
    }

    /**
     * Author CSS beats every table presentational hint, including <code>border</code>, <code>cellpadding</code>, alignment and size.
     * <p>
     * 작성자 CSS 는 <code>border</code>, <code>cellpadding</code>, 정렬, 크기를 포함한 모든 표 표현 힌트보다 우선한다.
     */
    public void testAuthorStylesOverrideTableHints() {
        fx.init("<table id=t border=5 cellpadding=9 width=100 align=left><tr id=r align=right height=20><td id=c width=30>x</td></tr></table>",
                "table{border:2px solid blue;width:120px;float:none} td{padding:0;width:44px} tr{text-align:center;height:50px}");
        Check.equal("solid solid solid solid", styles("t"), "author border style");
        Check.near(2, width("t", Prop.BORDER_TOP_WIDTH), 0.001, "author border width");
        Check.equal(0xFF0000FF, fx.style("t").color(Prop.BORDER_TOP_COLOR), "author border color");
        Check.near(120, fx.style("t").px(Prop.WIDTH), 0.001, "author width");
        Check.equal("none", fx.style("t").keyword(Prop.FLOAT), "author float");
        Check.near(0, fx.style("c").px(Prop.PADDING_TOP), 0.001, "author padding");
        Check.near(44, fx.style("c").px(Prop.WIDTH), 0.001, "author cell width");
        Check.equal("center", fx.style("r").keyword(Prop.TEXT_ALIGN), "author text-align");
        Check.near(50, fx.style("r").px(Prop.HEIGHT), 0.001, "author row height");
        Check.equal("inset inset inset inset", styles("c"), "the cell hint that the author did not override remains");
    }

    /**
     * Changing, adding or removing the attributes of a table recomputes the styles of the table and of its cells.
     * <p>
     * 표의 속성을 바꾸거나 더하거나 지우면 표와 셀의 스타일이 다시 계산된다.
     */
    public void testAttributeMutation() {
        fx.init("<table id=t><tr><td id=c>x</td></tr></table>", "");
        Check.equal("none none none none", styles("c"), "before");
        Element t = fx.element("t");
        t.setAttribute("border", "2");
        Check.equal("inset inset inset inset", styles("c"), "after border");
        t.setAttribute("cellpadding", "6");
        Check.near(6, fx.style("c").px(Prop.PADDING_LEFT), 0.001, "after cellpadding");
        t.setAttribute("rules", "all");
        Check.equal("solid solid solid solid", styles("c"), "after rules");
        Check.equal("collapse", fx.style("t").keyword(Prop.BORDER_COLLAPSE), "collapse after rules");
        t.removeAttribute("rules");
        t.removeAttribute("border");
        t.removeAttribute("cellpadding");
        Check.equal("none none none none", styles("c"), "after removal");
        Check.equal("separate", fx.style("t").keyword(Prop.BORDER_COLLAPSE), "separate after removal");
        Check.near(1, fx.style("c").px(Prop.PADDING_LEFT), 0.001, "padding after removal");
    }

    /**
     * The boxes follow the hints: border, spacing, padding, row height, hspace and column widths change the geometry of the table.
     * <p>
     * 상자는 힌트를 따른다: border, 간격, 안쪽 여백, 행 높이, hspace, 열 너비가 표의 기하를 바꾼다.
     */
    public void testGeometry() {
        fx.init("<table id=t border=2 cellspacing=4 cellpadding=3><tr><td id=c>X</td></tr></table>"
                + "<table id=u cellspacing=0 cellpadding=0 hspace=10><tr height=50><td id=d>X</td></tr></table>"
                + "<table id=w cellspacing=0 cellpadding=0><colgroup><col width=30><col span=2 width=40></colgroup><tr><td id=w1>a</td><td id=w2>b</td><td id=w3>c</td></tr></table>",
                "");
        RenderedPage page = fx.render();
        LayoutBox t = fx.box(page, "t");
        LayoutBox c = fx.box(page, "c");
        double glyph = c.getContentWidth();
        Check.isTrue(glyph > 0, "the cell has a content width");
        // 표 폭 = 표 border 2*2 + 간격 4*2 + 셀 (border 1 + padding 3) * 2 + 글자.
        Check.near(glyph + 2 * (1 + 3) + 2 * 4 + 2 * 2, t.getWidth(), 0.01, "table width of border, spacing and padding");
        Check.near(3, c.getPaddingLeft(), 0.001, "cell padding");
        Check.near(1, c.getBorderTop(), 0.001, "cell border");
        Check.near(2, t.getBorderLeft(), 0.001, "table border");
        // 셀 왼쪽 가장자리는 표 안쪽(border 2) 에서 간격 4 만큼 들어간다.
        Check.near(t.getX() + 2 + 4, c.getX(), 0.01, "cell offset");
        LayoutBox u = fx.box(page, "u");
        LayoutBox d = fx.box(page, "d");
        Check.near(10, u.getMarginLeft(), 0.001, "hspace");
        Check.near(10, u.getX() - 0, 0.01, "table left with hspace");
        Check.near(50, d.getHeight(), 0.01, "tr height");
        LayoutBox w1 = fx.box(page, "w1");
        LayoutBox w2 = fx.box(page, "w2");
        LayoutBox w3 = fx.box(page, "w3");
        Check.near(30, w1.getWidth(), 0.01, "first column");
        Check.near(40, w2.getWidth(), 0.01, "second column (col span)");
        Check.near(40, w3.getWidth(), 0.01, "third column (col span)");
    }

    /**
     * A <code>colgroup</code> without <code>col</code> children acts as <code>span</code> columns, and its HTML 4.01 <code>width</code>
     * is the width of each of them.
     * <p>
     * <code>col</code> 자식이 없는 <code>colgroup</code> 은 <code>span</code> 개의 열이고 HTML 4.01 의 <code>width</code> 가 각 열의 너비이다.
     */
    public void testColumnGroupSpan() {
        fx.init("<table id=t cellspacing=0 cellpadding=0><colgroup span=2 width=35></colgroup><tr><td id=a>a</td><td id=b>b</td><td id=c>c</td></tr></table>", "");
        RenderedPage page = fx.render();
        Check.near(35, fx.box(page, "a").getWidth(), 0.01, "first column of the group");
        Check.near(35, fx.box(page, "b").getWidth(), 0.01, "second column of the group");
        Check.isTrue(fx.box(page, "c").getWidth() > 0 && fx.box(page, "c").getWidth() < 35, "the third column is outside the group");
    }

    /**
     * The painted outset border of <code>border=N</code> has a lighter top-left and a darker bottom-right, and the inset cell border
     * the other way round, both with the UA gray; <code>border=0</code> paints no border.
     * <p>
     * <code>border=N</code> 의 outset border 는 왼쪽 위가 밝고 오른쪽 아래가 어둡게, inset 셀 border 는 반대로 UA 회색으로 그려지며 <code>border=0</code> 은 그리지 않는다.
     */
    public void testBorderPixels() {
        fx.init("<table id=a border=6 cellspacing=0 cellpadding=0 style='position:absolute;left:10px;top:10px'><tr><td id=ac width=40 height=30>&nbsp;</td></tr></table>"
                + "<table id=b border=0 cellspacing=0 cellpadding=0 style='position:absolute;left:100px;top:10px'><tr><td id=bc width=40 height=30>&nbsp;</td></tr></table>", "");
        RenderedPage page = fx.render();
        BufferedImage image = LegacyFixture.paint(page);
        LayoutBox a = fx.box(page, "a");
        int ax = (int) Math.round(a.getX());
        int ay = (int) Math.round(a.getY());
        int ar = (int) Math.round(a.getX() + a.getWidth()) - 1;
        int ab = (int) Math.round(a.getY() + a.getHeight()) - 1;
        int topLeft = image.getRGB(ax + 2, ay + 2) & 0xFF;
        int bottomRight = image.getRGB(ar - 2, ab - 2) & 0xFF;
        Check.isTrue(topLeft != 0xFF && bottomRight != 0xFF, "the table border is drawn");
        Check.isTrue(topLeft > bottomRight, "outset: the top-left is lighter than the bottom-right: " + topLeft + " > " + bottomRight);
        LayoutBox c = fx.box(page, "ac");
        int cx = (int) Math.round(c.getX());
        int cy = (int) Math.round(c.getY());
        int cellTopLeft = image.getRGB(cx, cy) & 0xFF;
        int cellBottomRight = image.getRGB((int) Math.round(c.getX() + c.getWidth()) - 1, (int) Math.round(c.getY() + c.getHeight()) - 1) & 0xFF;
        Check.isTrue(cellTopLeft < cellBottomRight, "inset: the top-left is darker than the bottom-right: " + cellTopLeft + " < " + cellBottomRight);
        LayoutBox b = fx.box(page, "b");
        Check.equal(0xFFFFFF, image.getRGB((int) Math.round(b.getX()), (int) Math.round(b.getY())) & 0xFFFFFF, "border=0 paints nothing at the corner");
    }

    /**
     * The painted pixels show the frame sides and the rules lines: the frame <code>above</code> draws only the top edge, and
     * <code>rules=all</code> draws the inner lines but no outer border.
     * <p>
     * 그려진 화소는 frame 의 변과 rules 의 선을 보여 준다: frame <code>above</code> 는 위쪽 가장자리만 그리고 <code>rules=all</code> 은 바깥 border 없이 안쪽 선만 그린다.
     */
    public void testFrameAndRulesPixels() {
        fx.init("<table id=a frame=above border=6 cellspacing=0 cellpadding=0 bordercolor=#ff0000 style='position:absolute;left:10px;top:10px'><tr><td id=ac width=40 height=30>&nbsp;</td></tr></table>"
                + "<table id=b rules=all cellspacing=0 cellpadding=0 style='position:absolute;left:100px;top:10px'><tr><td id=bc1 width=30 height=30></td><td id=bc2 width=30 height=30></td></tr></table>",
                "");
        RenderedPage page = fx.render();
        BufferedImage image = LegacyFixture.paint(page);
        LayoutBox a = fx.box(page, "a");
        int ax = (int) Math.round(a.getX());
        int ay = (int) Math.round(a.getY());
        // frame=above: 위쪽 가장자리 6px 는 빨간 계열(outset), 왼쪽/오른쪽/아래는 흰색.
        int top = image.getRGB(ax + 20, ay + 2) & 0xFFFFFF;
        Check.isTrue(((top >> 16) & 0xFF) > 0x80 && ((top >> 8) & 0xFF) < 0x80, "the top edge of frame=above is drawn in the border color: " + Integer.toHexString(top));
        int bottomY = (int) Math.round(a.getY() + a.getHeight()) - 2;
        Check.equal(0xFFFFFF, image.getRGB(ax + 20, bottomY) & 0xFFFFFF, "no bottom edge");
        Check.equal(0xFFFFFF, image.getRGB(ax + 1, ay + 20) & 0xFFFFFF, "no left edge");
        LayoutBox b = fx.box(page, "b");
        int bx = (int) Math.round(b.getX());
        int by = (int) Math.round(b.getY());
        // rules=all: 바깥 가장자리는 숨겨지고 두 셀 사이에 검은 선이 있다.
        Check.equal(0xFFFFFF, image.getRGB(bx + 10, by) & 0xFFFFFF, "no outer top line");
        Check.equal(0xFFFFFF, image.getRGB(bx, by + 10) & 0xFFFFFF, "no outer left line");
        LayoutBox c1 = fx.box(page, "bc1");
        int lineX = (int) Math.round(c1.getX() + c1.getWidth());
        boolean dark = false;
        // 두 셀 사이 몇 화소 안에 검은 선이 있어야 한다.
        for (int dx = -2; dx <= 2; dx++) {
            int rgb = image.getRGB(lineX + dx, by + 15) & 0xFFFFFF;
            if (rgb == 0x000000) {
                dark = true;
            }
        }
        Check.isTrue(dark, "an inner vertical line is drawn between the cells");
    }
}
