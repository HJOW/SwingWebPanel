package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>box-decoration-break</code> for an inline element that breaks over lines (CSS Fragmentation Module Level 3 section 5.3,
 * https://www.w3.org/TR/css-break-3/#break-decoration): with <code>slice</code> (the default) the border and the padding of the start belong to the first
 * fragment and those of the end to the last one, as if the box were cut from one long box; with <code>clone</code> every fragment gets the edges of both
 * ends, as if each were a box of its own, and the text starts after the cloned start edge on the continuation lines.
 * <p>
 * 여러 줄에 걸쳐 나뉘는 인라인 요소의 <code>box-decoration-break</code> 시험(CSS Fragmentation Module Level 3 5.3 절): <code>slice</code>(기본값)에서는 시작의 border 와
 * padding 은 첫 조각에, 끝의 것은 마지막 조각에 속해 긴 상자 하나를 자른 것과 같고, <code>clone</code> 에서는 조각마다 양쪽 끝의 가장자리를 가져 각각 독립된 상자와 같으며
 * 이어지는 줄에서 텍스트는 복제한 시작 가장자리 뒤에서 시작한다.
 */
public class BoxDecorationBreakLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BoxDecorationBreakLayoutTest() {
    }

    /**
     * Returns the fragments of the element <code>s</code> in the order of the lines.
     * <p>
     * 요소 <code>s</code> 의 조각을 줄 순서로 반환한다.
     *
     * @param style the style of the span / span 의 스타일
     * @return the fragments / 조각
     */
    private static List<LayoutBox> fragments(String style) {
        double w = 12 * LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0'><span id=s style='" + style + "'>aaaa bbbb cccc dddd eeee ffff</span></p></div>", 600);
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        // 줄마다 span 의 조각(INLINE 상자)을 모은다.
        for (LayoutBox line : LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"))) {
            for (LayoutBox c : line.getChildren()) {
                if (c.getType() == LayoutBox.Type.INLINE && c.getNode() == LayoutTestSupport.box(r, "s").getNode()) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /**
     * Slice: the first fragment has the left edge, the last one the right edge, the ones between have none.
     * <p>
     * slice: 첫 조각은 왼쪽 가장자리를, 마지막 조각은 오른쪽 가장자리를 가지고 사이의 조각은 가지지 않는다.
     */
    public void testSlice() {
        List<LayoutBox> f = fragments("border:2px solid #000;padding:0 3px");
        Check.isTrue(f.size() >= 3, "the span breaks over three lines: " + f.size());
        Check.near(2, f.get(0).getBorderLeft(), 0.01, "first: left border");
        Check.near(0, f.get(0).getBorderRight(), 0.01, "first: no right border");
        Check.near(0, f.get(1).getBorderLeft(), 0.01, "middle: no left border");
        Check.near(0, f.get(1).getBorderRight(), 0.01, "middle: no right border");
        Check.near(0, f.get(f.size() - 1).getBorderLeft(), 0.01, "last: no left border");
        Check.near(2, f.get(f.size() - 1).getBorderRight(), 0.01, "last: right border");
    }

    /**
     * Clone: every fragment has the border and the padding of both ends, and the text of a continuation line starts after the start edge.
     * <p>
     * clone: 모든 조각이 양쪽 끝의 border 와 padding 을 가지며 이어지는 줄의 텍스트는 시작 가장자리 뒤에서 시작한다.
     */
    public void testClone() {
        List<LayoutBox> f = fragments("border:2px solid #000;padding:0 3px;box-decoration-break:clone");
        Check.isTrue(f.size() >= 3, "the span breaks over three lines: " + f.size());
        // 조각마다 양쪽 가장자리를 확인한다.
        for (int i = 0; i < f.size(); i++) {
            Check.near(2, f.get(i).getBorderLeft(), 0.01, "fragment " + i + ": left border");
            Check.near(2, f.get(i).getBorderRight(), 0.01, "fragment " + i + ": right border");
            Check.near(3, f.get(i).getPaddingLeft(), 0.01, "fragment " + i + ": left padding");
            Check.near(3, f.get(i).getPaddingRight(), 0.01, "fragment " + i + ": right padding");
        }
        LayoutBox second = f.get(1);
        LayoutBox text = null;
        // 둘째 줄에서 이 조각 뒤의 첫 텍스트 상자를 찾는다.
        for (LayoutBox c : second.getParent().getChildren()) {
            if (c.getType() == LayoutBox.Type.TEXT && text == null) {
                text = c;
            }
        }
        Check.isTrue(text != null && text.getX() >= second.getX() + 5 - 0.01, "the text starts after the cloned start edge (border 2 + padding 3)");
    }

    /**
     * Without borders and padding there is nothing to clone, so the lines are the same as for slice.
     * <p>
     * border 와 padding 이 없으면 복제할 것이 없으므로 줄은 slice 와 같다.
     */
    public void testNothingToCloneWithoutEdges() {
        List<LayoutBox> a = fragments("background:#ff0");
        List<LayoutBox> b = fragments("background:#ff0;box-decoration-break:clone");
        Check.equal(a.size(), b.size(), "the same number of fragments");
        Check.near(a.get(1).getX(), b.get(1).getX(), 0.01, "at the same place");
    }
}
