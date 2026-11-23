package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of list markers and generated content (plan items P09-01, P09-03): bullets and numbers of list items (<code>start</code>,
 * <code>reversed</code>, <code>value</code>, counter styles, inside markers), and <code>::before</code>/<code>::after</code> content.
 * <p>
 * 목록 표식과 생성 콘텐츠 시험(계획 P09-01, P09-03): 목록 항목의 글머리 기호와 번호(<code>start</code>, <code>reversed</code>, <code>value</code>, 카운터 스타일,
 * 안쪽 표식)와 <code>::before</code>/<code>::after</code> 콘텐츠.
 */
public class ListAndGeneratedContentTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ListAndGeneratedContentTest() {
    }

    /**
     * Returns the marker box of a list item.
     * <p>
     * 목록 항목의 표식 상자를 반환한다.
     *
     * @param r  the result / 결과
     * @param id id of the list item / 목록 항목의 id
     * @return the marker / 표식
     */
    private static LayoutBox marker(LayoutResult r, String id) {
        LayoutBox li = LayoutTestSupport.box(r, id);
        // 목록 항목의 자식 중 표식 상자를 찾는다.
        for (LayoutBox c : li.getChildren()) {
            if (c.getType() == LayoutBox.Type.MARKER) {
                return c;
            }
        }
        Check.fail("list item #" + id + " has no marker");
        return null;
    }

    /**
     * Bullets sit outside the item: the right edge of the marker touches the left border edge of the item and their baselines agree.
     * <p>
     * 글머리 기호는 항목 바깥에 놓인다: 표식의 오른쪽 가장자리가 항목의 왼쪽 border 가장자리에 닿고 기준선이 같다.
     */
    public void testBulletsAreOutside() {
        LayoutResult r = LayoutTestSupport.layout("<ul><li id=a>first</li><li id=b>second</li></ul>", 400);
        LayoutBox m = marker(r, "a");
        LayoutBox li = LayoutTestSupport.box(r, "a");
        Check.equal("• ", m.getText(), "bullet text");
        Check.near(li.getX(), m.getX() + m.getWidth(), T, "right edge of the marker is at the left edge of the item");
        LayoutBox line = LayoutTestSupport.lines(li).get(0);
        Check.near(line.getBaseline(), m.getBaseline(), T, "baselines are aligned");
        Check.isTrue(li.getX() > 0, "the list is indented by the user-agent style");
        Check.isTrue(marker(r, "b").getY() > m.getY(), "the second marker is below the first");
    }

    /**
     * Ordered lists number their items; <code>start</code>, <code>reversed</code>, <code>value</code> and counter styles are honoured.
     * <p>
     * 순서 있는 목록은 항목에 번호를 매긴다. <code>start</code>, <code>reversed</code>, <code>value</code>, 카운터 스타일을 따른다.
     */
    public void testNumbering() {
        LayoutResult r = LayoutTestSupport.layout(
                "<ol><li id=a>x</li><li id=b>x</li></ol>"
                + "<ol start=5><li id=c>x</li><li id=d value=9>x</li><li id=e>x</li></ol>"
                + "<ol reversed><li id=f>x</li><li id=g>x</li><li id=h>x</li></ol>"
                + "<ol style='list-style-type:lower-alpha'><li id=i>x</li><li id=j>x</li></ol>"
                + "<ol style='list-style-type:upper-roman'><li id=k>x</li><li id=l>x</li><li id=m>x</li><li id=n>x</li></ol>"
                + "<ul style='list-style-type:none'><li id=o>x</li></ul>", 400);
        Check.equal("1. ", marker(r, "a").getText(), "first");
        Check.equal("2. ", marker(r, "b").getText(), "second");
        Check.equal("5. ", marker(r, "c").getText(), "start=5");
        Check.equal("9. ", marker(r, "d").getText(), "value=9");
        Check.equal("10. ", marker(r, "e").getText(), "continues after value");
        Check.equal("3. ", marker(r, "f").getText(), "reversed starts at the number of items");
        Check.equal("1. ", marker(r, "h").getText(), "and counts down");
        Check.equal("a. ", marker(r, "i").getText(), "lower-alpha");
        Check.equal("b. ", marker(r, "j").getText(), "lower-alpha second");
        Check.equal("IV. ", marker(r, "n").getText(), "upper-roman");
        List<LayoutBox> kids = LayoutTestSupport.box(r, "o").getChildren();
        Check.isFalse(kids.isEmpty() && false, "list-style-type:none keeps the item");
        // 표식이 없는 항목에는 MARKER 상자가 없어야 한다.
        for (LayoutBox c : kids) {
            Check.isFalse(c.getType() == LayoutBox.Type.MARKER, "no marker for list-style-type:none");
        }
    }

    /**
     * An inside marker is part of the first line.
     * <p>
     * 안쪽 표식은 첫 줄의 일부이다.
     */
    public void testInsideMarker() {
        LayoutResult r = LayoutTestSupport.layout("<ul style='list-style-position:inside'><li id=a>item</li></ul>", 400);
        LayoutBox li = LayoutTestSupport.box(r, "a");
        Check.equal("• item", LayoutTestSupport.lineText(LayoutTestSupport.lines(li).get(0)), "the marker text starts the line");
        // 표식이 없어야 하는 항목의 자식을 확인한다.
        for (LayoutBox c : li.getChildren()) {
            Check.isFalse(c.getType() == LayoutBox.Type.MARKER, "no outside marker box");
        }
    }

    /**
     * <code>::before</code> and <code>::after</code> add text to the lines of their element; <code>attr()</code> and the empty string work.
     * <p>
     * <code>::before</code> 와 <code>::after</code> 는 요소의 줄에 텍스트를 더한다. <code>attr()</code> 와 빈 문자열도 동작한다.
     */
    public void testInlinePseudoElements() {
        LayoutResult r = LayoutTestSupport.layout(
                "<style>#p::before{content:'>> '} #p::after{content:' <<'} #a::after{content:' (' attr(title) ')'}</style>"
                + "<div id=p>x</div><div id=q><span id=a title='tip'>link</span></div>", 400);
        Check.equal(">> x <<", LayoutTestSupport.lineText(LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0)), "before and after");
        Check.equal("link (tip)", LayoutTestSupport.lineText(LayoutTestSupport.lines(LayoutTestSupport.box(r, "q")).get(0)), "attr()");
        Element p = (Element) LayoutTestSupport.box(r, "p").getNode();
        boolean generated = false;
        // 요소에 대응하는 조각 중 의사 요소에서 온 것이 있는지 확인한다.
        for (LayoutBox b : r.getBoxes(p)) {
            if ("before".equals(b.getPseudo()) || "after".equals(b.getPseudo())) {
                generated = true;
            }
        }
        Check.isTrue(generated, "fragments of generated content know their pseudo-element");
    }

    /**
     * A block-level pseudo-element makes a box; an empty string content still makes it (clearfix).
     * <p>
     * 블록 수준 의사 요소는 상자를 만든다. 빈 문자열 콘텐츠도 상자를 만든다(clearfix).
     */
    public void testBlockPseudoElement() {
        LayoutResult r = LayoutTestSupport.layout(
                "<style>#d::after{content:'';display:block;height:7px} #e::before{content:'top';display:block}</style>"
                + "<div id=d><div style='height:10px'></div></div><div id=e>body</div>", 400);
        Check.near(17, LayoutTestSupport.box(r, "d").getHeight(), T, "the generated block adds its height");
        List<String> lines = new java.util.ArrayList<String>();
        // e 안의 줄 텍스트를 모은다.
        for (LayoutBox l : LayoutTestSupport.lines(LayoutTestSupport.box(r, "e"))) {
            lines.add(LayoutTestSupport.lineText(l));
        }
        Check.equal("[top, body]", lines.toString(), "block pseudo-element is on its own line");
    }
}
