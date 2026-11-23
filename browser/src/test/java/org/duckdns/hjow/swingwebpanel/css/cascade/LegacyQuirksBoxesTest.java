package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the box sizing quirks of quirks mode that the Quirks Mode Standard (WHATWG Living Standard of 2026-10-19,
 * https://quirks.spec.whatwg.org/) defines for tables and percentages (plan item P07-07-b-11): the percentage height calculation (3.5),
 * the table cell width calculation with images (3.8), the nowrap minimum width of cells (3.9) and the box sizing of the height of table
 * cells (3.13). Each quirk is checked in quirks mode and in limited-quirks and no-quirks modes, in which it must not apply.
 * <p>
 * 호환 모드의 상자 크기 quirk 시험(계획 P07-07-b-11): 호환 모드 표준(WHATWG Living Standard 2026-10-19)이 표와 백분율에 대해 정한 백분율 높이 계산(3.5), 이미지가
 * 있는 표 셀 너비 계산(3.8), 셀의 nowrap 최소 너비(3.9), 표 셀 높이의 box-sizing(3.13). 각 quirk 를 호환 모드에서 확인하고 제한적 호환 모드와 표준 모드에서는
 * 적용되지 않음을 확인한다.
 */
public class LegacyQuirksBoxesTest {

    /** The shared fixture. / 공용 도구. */
    private final LegacyFixture fx = new LegacyFixture();

    /** DOCTYPE of the limited-quirks mode (XHTML 1.0 Transitional). / 제한적 호환 모드의 DOCTYPE(XHTML 1.0 Transitional). */
    private static final String LIMITED = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyQuirksBoxesTest() {
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
     * Returns the first box of the element with an id of the current page.
     * <p>
     * 현재 페이지에서 id 가 있는 요소의 첫 상자를 반환한다.
     *
     * @param page the rendered page / 렌더링한 페이지
     * @param id   the id / id
     * @return the box / 상자
     */
    private LayoutBox box(RenderedPage page, String id) {
        Element e = fx.element(id);
        return page.getLayout().getBoxes(e).get(0);
    }

    /**
     * Parses the markup in one of the three modes.
     * <p>
     * 마크업을 세 모드 중 하나로 해석한다.
     *
     * @param mode   0 for quirks, 1 for limited quirks, 2 for no quirks / 0 은 호환, 1 은 제한적 호환, 2 는 표준
     * @param markup the body content / 본문 내용
     * @param rules  author CSS / 작성자 CSS
     */
    private void parse(int mode, String markup, String rules) {
        if (mode == 0) {
            fx.initQuirks(markup, rules);
        } else if (mode == 1) {
            fx.parse(LIMITED, markup, rules);
        } else {
            fx.init(markup, rules);
        }
    }

    /**
     * In quirks mode a table cell uses its border box for <code>height</code> whatever <code>box-sizing</code> says (Quirks Mode
     * Standard 3.13); other boxes, the width of cells and the other modes keep the content box.
     * <p>
     * 호환 모드의 표 셀은 <code>box-sizing</code> 과 무관하게 <code>height</code> 에 border box 를 쓴다(호환 모드 표준 3.13). 다른 상자, 셀의 너비, 다른 모드는 content box 를
     * 유지한다.
     */
    public void testTableCellHeightBoxSizing() {
        String markup = "<table><tr><td id=c style='height:50px;width:60px;padding:10px;border:2px solid #000'>x</td></tr></table>"
                + "<div id=d style='height:50px;padding:10px;border:2px solid #000'>x</div>"
                + "<table><tr><td id=e style='height:50px;padding:10px;border:2px solid #000;box-sizing:content-box'>x</td></tr></table>"
                + "<table><tr><td id=f style='min-height:70px;padding:10px;border:2px solid #000'>x</td></tr></table>";
        parse(0, markup, "");
        RenderedPage page = fx.render();
        Check.near(50, box(page, "c").getHeight(), 0.01, "quirks: the cell height is the border box height");
        Check.near(60 + 24, box(page, "c").getWidth(), 0.01, "quirks: the width of a cell keeps its content box");
        Check.near(74, box(page, "d").getHeight(), 0.01, "quirks: a block keeps the content box");
        Check.near(50, box(page, "e").getHeight(), 0.01, "quirks: an explicit content-box does not matter for the height of a cell");
        Check.near(70, box(page, "f").getHeight(), 0.01, "quirks: min-height of a cell is a border box height too");
        parse(1, markup, "");
        page = fx.render();
        Check.near(74, box(page, "c").getHeight(), 0.01, "limited quirks: content box");
        parse(2, markup, "");
        page = fx.render();
        Check.near(74, box(page, "c").getHeight(), 0.01, "standards: content box");
        Check.near(94, box(page, "f").getHeight(), 0.01, "standards: min-height is a content box height");
        parse(0, "<table><tr><td id=c style='height:50px;padding:10px;border:2px solid #000;box-sizing:content-box'>x</td></tr></table>"
                + "<div id=g style='display:table-cell;height:50px;padding:10px;border:2px solid #000;font-size:10px'>y</div>", "");
        page = fx.render();
        Check.near(50, box(page, "c").getHeight(), 0.01, "quirks: still border box");
        Check.near(50, box(page, "g").getHeight(), 0.01, "quirks: any display:table-cell element");
        parse(0, "<table><tr><td id=c style='height:10px;padding:10px;border:2px solid #000'>x</td></tr></table>", "");
        page = fx.render();
        Check.isTrue(box(page, "c").getHeight() >= 24 - 0.01, "quirks: a height below the padding and border grows to them (content height at least 0): " + box(page, "c").getHeight());
    }

    /**
     * A nowrap cell with a width that is a length other than zero has a minimum width of at least that width in quirks mode (Quirks
     * Mode Standard 3.9): a table that is narrower than the sum of the minimum widths still gives the cell its width.
     * <p>
     * 호환 모드에서 0 이 아닌 길이 width 를 가진 nowrap 셀의 최소 너비는 적어도 그 width 이다(호환 모드 표준 3.9): 최소 너비의 합보다 좁은 표도 셀에 그 너비를 준다.
     */
    public void testNowrapCellMinimumWidth() {
        String markup = "<table style='width:1px'><tr><td id=a nowrap width=100>a b c</td><td id=b>x</td></tr></table>"
                + "<table style='width:1px'><tr><td id=c nowrap>a b c</td><td id=d>x</td></tr></table>"
                + "<table style='width:1px'><tr><td id=e width=100>a b c</td><td id=f>x</td></tr></table>"
                + "<table style='width:1px'><tr><td id=g nowrap width=0>a b c</td><td id=h>x</td></tr></table>"
                + "<table style='width:1px'><tr><td id=i nowrap width='50%'>a b c</td><td id=j>x</td></tr></table>";
        parse(0, markup, "");
        RenderedPage page = fx.render();
        // 셀의 padding 은 1px 씩이므로 지정 너비 100 의 바깥 너비는 102 이다.
        Check.near(102, box(page, "a").getWidth(), 0.5, "quirks: nowrap cell with a length width keeps that width (outer 102)");
        Check.isTrue(box(page, "c").getWidth() < 102, "quirks: a nowrap cell without a width has its own minimum: " + box(page, "c").getWidth());
        Check.isTrue(box(page, "e").getWidth() < 102, "quirks: no nowrap, no quirk: " + box(page, "e").getWidth());
        Check.isTrue(box(page, "g").getWidth() < 102, "quirks: width 0 is not covered");
        Check.isTrue(box(page, "i").getWidth() < 102, "quirks: a percentage is not a length: " + box(page, "i").getWidth());
        parse(1, markup, "");
        page = fx.render();
        Check.isTrue(box(page, "a").getWidth() < 102, "limited quirks: no quirk: " + box(page, "a").getWidth());
        parse(2, markup, "");
        page = fx.render();
        Check.isTrue(box(page, "a").getWidth() < 102, "standards: no quirk: " + box(page, "a").getWidth());
        // 작성자가 white-space 를 정해도 quirk 는 적용된다(셀의 지정 너비가 최소 너비의 하한이다).
        parse(0, "<table style='width:1px'><tr><td id=k nowrap width=100 style='white-space:normal'>a b c</td><td>x</td></tr></table>", "");
        page = fx.render();
        Check.near(102, box(page, "k").getWidth(), 0.5, "quirks: author white-space does not matter");
        // th 도 같다.
        parse(0, "<table style='width:1px'><tr><th id=l nowrap width=100>a b c</th><td>x</td></tr></table>", "");
        page = fx.render();
        Check.near(102, box(page, "l").getWidth(), 0.5, "quirks: th cells too");
    }

    /**
     * In quirks mode the min-content width of a table cell with an automatic width has no soft wrap opportunity before and after an
     * <code>img</code> (Quirks Mode Standard 3.8): a table that has to shrink keeps a row of image slices on one line.
     * <p>
     * 호환 모드에서 너비가 auto 인 표 셀의 min-content 너비에는 <code>img</code> 앞뒤에 줄바꿈 기회가 없다(호환 모드 표준 3.8): 줄어들어야 하는 표도 이미지 조각의 줄을 한
     * 줄에 둔다.
     */
    public void testCellImagesHaveNoWrapOpportunity() {
        String slices = "<div style='width:10px'><table><tr><td id=c><img width=30 height=10><img width=30 height=10><img width=30 height=10></td></tr></table></div>";
        parse(0, slices, "");
        RenderedPage page = fx.render();
        Check.near(90 + 2, box(page, "c").getWidth(), 0.5, "quirks: three image slices stay together (3 x 30 + padding)");
        parse(1, slices, "");
        page = fx.render();
        Check.near(30 + 2, box(page, "c").getWidth(), 0.5, "limited quirks: the cell shrinks to one image");
        parse(2, slices, "");
        page = fx.render();
        Check.near(30 + 2, box(page, "c").getWidth(), 0.5, "standards: the cell shrinks to one image");
        // 텍스트와 이미지가 공백 없이 이어지면 한 덩어리이다(글자 너비는 20px 고정폭 글꼴의 실제 너비로 잰다; 호환 모드의 표는 글꼴을 초기화하므로 셀에 글꼴을 준다).
        String rules = "td{font:20px monospace}";
        parse(2, "<table><tr><td id=c>mmmmmmmmmm</td></tr></table>", rules);
        double adv = (box(fx.render(), "c").getWidth() - 2) / 10;
        String mixed = "<div style='width:10px'><table><tr><td id=c>ab<img width=30 height=10>cd</td></tr></table></div>";
        parse(0, mixed, rules);
        page = fx.render();
        double quirks = box(page, "c").getWidth();
        parse(2, mixed, rules);
        page = fx.render();
        double standards = box(page, "c").getWidth();
        Check.near(2 + adv * 2 + 30 + adv * 2, quirks, 0.5, "quirks: text, image and text without spaces stay together");
        Check.near(2 + Math.max(30, adv * 2), standards, 0.5, "standards: the widest unit is the image");
        // 이미지 바로 앞의 공백 뒤에는 기회가 없고(앞 글자, 공백, 이미지가 한 덩어리) 이미지 뒤 공백 다음의 기회는 남는다.
        String spaced = "<div style='width:10px'><table><tr><td id=c>ab <img width=30 height=10> cd</td></tr></table></div>";
        parse(0, spaced, rules);
        page = fx.render();
        Check.near(2 + adv * 2 + adv + 30, box(page, "c").getWidth(), 0.5,
                "quirks: ab, the space and the image form one unit; the break after the space that follows the image stays");
        parse(2, spaced, rules);
        page = fx.render();
        Check.near(2 + Math.max(30, adv * 2), box(page, "c").getWidth(), 0.5, "standards: units of the text and the image");
        // 너비가 지정된 셀과 셀이 아닌 블록에는 적용하지 않는다.
        parse(0, "<div style='width:10px'><table><tr><td id=c width=40><img width=30 height=10><img width=30 height=10></td></tr></table></div>", "");
        page = fx.render();
        Check.near(30 + 2, box(page, "c").getWidth(), 0.5, "quirks: a cell with a width is not affected (the quirk would need 62)");
        parse(0, "<div style='width:10px'><table><tr><td id=c><div><img width=30 height=10><img width=30 height=10></div></td></tr></table></div>", "");
        page = fx.render();
        Check.near(30 + 2, box(page, "c").getWidth(), 0.5, "quirks: the containing block of the images is the div, not the cell");
        // 텍스트만 있는 셀과 다른 요소(span 안)의 이미지.
        parse(0, "<div style='width:10px'><table><tr><td id=c><span><img width=30 height=10></span><img width=30 height=10></td></tr></table></div>", "");
        page = fx.render();
        Check.near(60 + 2, box(page, "c").getWidth(), 0.5, "quirks: inline element edges do not separate images");
    }

    /**
     * In quirks mode a percentage height refers to the nearest ancestor with a definite height, skipping the block containers with an
     * automatic height up to the initial containing block (Quirks Mode Standard 3.5); in the other modes such a height is indefinite.
     * <p>
     * 호환 모드에서 백분율 높이는 높이가 정해진 가장 가까운 조상을 참조하며 높이가 auto 인 블록 컨테이너를 초기 포함 블록까지 건너뛴다(호환 모드 표준 3.5). 다른 모드에서는 그런
     * 높이가 정해지지 않는다.
     */
    public void testPercentageHeight() {
        String markup = "<div id=o><div id=p style='height:50%'>x</div></div>";
        parse(0, markup, "");
        RenderedPage page = fx.render();
        Check.near(LegacyFixture.HEIGHT / 2, box(page, "p").getHeight(), 0.5, "quirks: 50% of the initial containing block through auto heights");
        parse(1, markup, "");
        page = fx.render();
        Check.isTrue(box(page, "p").getHeight() < 40, "limited quirks: indefinite, so automatic: " + box(page, "p").getHeight());
        parse(2, markup, "");
        page = fx.render();
        Check.isTrue(box(page, "p").getHeight() < 40, "standards: indefinite, so automatic: " + box(page, "p").getHeight());
        // 높이가 정해진 조상에서 멈춘다.
        String definite = "<div style='height:100px'><div id=m><div id=n style='height:50%'>x</div></div></div>";
        parse(0, definite, "");
        page = fx.render();
        Check.near(50, box(page, "n").getHeight(), 0.5, "quirks: 50% of the nearest ancestor with a definite height");
        parse(2, definite, "");
        page = fx.render();
        Check.isTrue(box(page, "n").getHeight() < 40, "standards: m is automatic, so is n: " + box(page, "n").getHeight());
        // body 의 height:100% 도 초기 포함 블록을 참조하고 그 안의 백분율은 body 의 높이를 참조한다.
        String body = "<div id=a style='height:100%'>x</div>";
        parse(0, body, "body{height:100%}");
        page = fx.render();
        Check.near(LegacyFixture.HEIGHT, box(page, "a").getHeight(), 0.5, "quirks: 100% through body and html");
        // 백분율 min-height 와 max-height 도 같다.
        String limits = "<div id=a style='min-height:50%'>x</div><div id=b style='max-height:25%;height:200px'>y</div>";
        parse(0, limits, "");
        page = fx.render();
        Check.near(80, box(page, "a").getHeight(), 0.5, "quirks: min-height percentage");
        Check.near(40, box(page, "b").getHeight(), 0.5, "quirks: max-height percentage");
        // 표 셀과 절대 배치 상자와 높이가 정해지지 않은 백분율은 탐색을 끝낸다.
        String stops = "<table><tr><td><div id=r style='height:50%'>x</div></td></tr></table>"
                + "<div style='position:absolute;top:0;left:100px'><div id=q style='height:50%'>x</div></div>"
                + "<div style='height:50%'><div id=s style='height:50%'>x</div></div>";
        parse(0, stops, "");
        page = fx.render();
        Check.isTrue(box(page, "r").getHeight() < 40, "quirks: a table cell ends the search: " + box(page, "r").getHeight());
        Check.isTrue(box(page, "q").getHeight() < 40, "quirks: an absolutely positioned box with an automatic height ends the search: " + box(page, "q").getHeight());
        Check.near(LegacyFixture.HEIGHT / 4, box(page, "s").getHeight(), 0.5, "quirks: the percentage of a percentage that is definite");
        // 백분율 높이를 가진 자식이 플렉스 컨테이너 안이면 쓰지 않는다(플렉스 컨테이너는 블록 컨테이너가 아니다).
        parse(0, "<div style='display:flex'><div id=f style='height:50%'>x</div></div>", "");
        page = fx.render();
        Check.isTrue(box(page, "f").getHeight() < 40, "quirks: a flex container is not skipped: " + box(page, "f").getHeight());
    }
}
