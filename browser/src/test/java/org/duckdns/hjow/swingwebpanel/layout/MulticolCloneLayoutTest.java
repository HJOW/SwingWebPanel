package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>box-decoration-break: clone</code> for a block that is split over columns (plan item P10-08-d, CSS Fragmentation Module Level 3 section 5.3,
 * https://www.w3.org/TR/css-break-3/#break-decoration): every fragment has the border and the padding of the box on both its edges, the content of a continuation
 * fragment starts below the repeated top decoration, the columns are cut where the content and the bottom decoration of the fragment still fit, and the margins at a
 * cut are dropped. The default <code>slice</code> keeps the decoration at the first top and the last bottom only. The box is 8 blocks of 20 px with a border
 * of 5 px and a padding of 10 px (the decoration of one edge is 15 px) in three columns of 100 px height that are filled one after the other.
 * <p>
 * 열에 나뉘는 블록의 <code>box-decoration-break: clone</code> 시험(계획 P10-08-d, CSS Fragmentation Module Level 3 5.3 절): 조각마다 양쪽 가장자리에 상자의 border 와 padding 이
 * 있고, 이어지는 조각의 내용은 되풀이한 위쪽 장식 아래에서 시작하며, 조각의 내용과 아래쪽 장식이 들어가는 곳에서 열을 자르고, 끊는 곳의 margin 은 버린다. 기본인 <code>slice</code> 는
 * 첫 조각의 위쪽과 마지막 조각의 아래쪽에만 장식을 둔다. 상자는 20px 블록 8 개이고 border 5px, padding 10px(한 가장자리의 장식은 15px)이며 높이 100px 의 열 셋에 차례로 채운다.
 */
public class MulticolCloneLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MulticolCloneLayoutTest() {
    }

    /**
     * Lays out the box in the columns and returns the fragments of the box.
     * <p>
     * 열 안에 상자를 배치하고 상자의 조각을 반환한다.
     *
     * @param decoration the value of <code>box-decoration-break</code> / <code>box-decoration-break</code> 의 값
     * @param extra      more style of the box / 상자의 추가 스타일
     * @return the fragments in the order of the columns / 열 순서의 조각
     */
    private static List<LayoutBox> fragments(String decoration, String extra) {
        StringBuilder kids = new StringBuilder();
        // 높이 20px 블록 여덟 개를 만든다.
        for (int i = 0; i < 8; i++) {
            kids.append("<div class=k style='height:20px'></div>");
        }
        LayoutResult r = LayoutTestSupport.layout("<div id=mc style='column-count:3;column-gap:0;column-fill:auto;width:300px;height:100px'>"
                + "<div id=b style='border:5px solid #000;padding:10px;box-decoration-break:" + decoration + ";" + extra + "'>" + kids + "</div></div>", 600);
        Element e = ((Element) r.getRoot().getNode()).getOwnerDocument().getElementById("b");
        return r.getBoxes(e);
    }

    /**
     * Without clone (the default slice) the first fragment has the top decoration and the last the bottom one; the middle one has none.
     * <p>
     * clone 이 아니면(기본 slice) 첫 조각이 위쪽 장식을, 마지막 조각이 아래쪽 장식을 가지고 가운데 조각은 없다.
     */
    public void testSliceKeepsTheDecorationOnTheOuterEdgesOnly() {
        List<LayoutBox> f = fragments("slice", "");
        Check.equal(2, f.size(), "slice: 95 px fit the first column, the rest the second: two fragments");
        Check.near(95, f.get(0).getHeight(), 0.5, "the first fragment: 15 + 4 blocks");
        Check.near(5, f.get(0).getBorderTop(), 0.01, "its top border");
        Check.near(0, f.get(0).getBorderBottom(), 0.01, "no bottom border at the cut");
        Check.near(0, f.get(1).getBorderTop(), 0.01, "no top border of the continuation");
        Check.near(0, f.get(1).getPaddingTop(), 0.01, "no top padding of the continuation");
        Check.near(5, f.get(1).getBorderBottom(), 0.01, "the bottom border of the last fragment");
    }

    /**
     * With clone every fragment has both decorations, the content of a continuation starts below the repeated top decoration, and the cuts leave
     * room for the bottom decoration.
     * <p>
     * clone 이면 모든 조각이 양쪽 장식을 가지고, 이어지는 조각의 내용은 되풀이한 위쪽 장식 아래에서 시작하며, 자르는 곳은 아래쪽 장식의 공간을 남긴다.
     */
    public void testCloneRepeatsTheDecorationAtEveryFragment() {
        List<LayoutBox> f = fragments("clone", "");
        Check.equal(3, f.size(), "clone: the decoration takes room, three fragments");
        // 첫 조각: 위쪽 장식 15 + 블록 셋 60 + 아래쪽 장식 15 = 90 (네 번째 블록까지는 110 이라 100 에 들어가지 않는다).
        Check.near(90, f.get(0).getHeight(), 0.5, "the first fragment: 15 + 60 + 15");
        Check.near(5, f.get(0).getBorderTop(), 0.01, "first: top border");
        Check.near(5, f.get(0).getBorderBottom(), 0.01, "first: the bottom border is repeated at the cut");
        Check.near(10, f.get(0).getPaddingBottom(), 0.01, "first: the bottom padding is repeated");
        // 둘째 조각: 위쪽 장식 15 + 블록 셋 60 + 아래쪽 장식 15 = 90.
        Check.near(90, f.get(1).getHeight(), 0.5, "the second fragment");
        Check.near(5, f.get(1).getBorderTop(), 0.01, "second: the top border is repeated");
        Check.near(10, f.get(1).getPaddingTop(), 0.01, "second: the top padding is repeated");
        Check.near(5, f.get(1).getBorderBottom(), 0.01, "second: the bottom border is repeated");
        // 셋째 조각: 위쪽 장식 15 + 블록 둘 40 + 아래쪽 장식 15 = 70.
        Check.near(70, f.get(2).getHeight(), 0.5, "the last fragment: 15 + 40 + 15");
        Check.near(5, f.get(2).getBorderBottom(), 0.01, "last: the bottom border");
        // 이어지는 조각의 첫 블록은 되풀이한 위쪽 장식(15) 아래에서 시작한다.
        LayoutBox firstOfSecond = f.get(1).getChildren().get(0);
        Check.near(15, firstOfSecond.getY() - f.get(1).getY(), 0.5, "the content of the second fragment starts below its top decoration");
        LayoutBox firstOfThird = f.get(2).getChildren().get(0);
        Check.near(15, firstOfThird.getY() - f.get(2).getY(), 0.5, "and so does the third");
        Check.near(0, f.get(1).getY() - f.get(0).getY(), 0.5, "the columns start at the same height");
        Check.isTrue(f.get(1).getX() > f.get(0).getX() && f.get(2).getX() > f.get(1).getX(), "the fragments are in successive columns");
    }

    /**
     * The decoration is part of the column: no fragment is taller than the column.
     * <p>
     * 장식은 열의 일부이다: 열보다 높은 조각은 없다.
     */
    public void testNoFragmentIsTallerThanTheColumn() {
        List<LayoutBox> f = fragments("clone", "");
        // 조각마다 높이가 열 높이(100) 이하인지 확인한다.
        for (int i = 0; i < f.size(); i++) {
            Check.isTrue(f.get(i).getHeight() <= 100.01, "fragment " + i + " fits the column: " + f.get(i).getHeight());
        }
    }

    /**
     * The border box of a clone box that is not split is unchanged.
     * <p>
     * 나뉘지 않는 clone 상자의 border box 는 바뀌지 않는다.
     */
    public void testUnsplitCloneBoxIsUnchanged() {
        LayoutResult r = LayoutTestSupport.layout("<div style='column-count:3;column-gap:0;width:300px'><div id=b style='border:5px solid #000;padding:10px;box-decoration-break:clone'>"
                + "<div style='height:20px'></div></div></div>", 600);
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(50, b.getHeight(), 0.5, "20 + 2 * 15");
        Check.near(5, b.getBorderTop(), 0.01, "top border");
        Check.near(5, b.getBorderBottom(), 0.01, "bottom border");
    }

    /**
     * A box nested in a clone box that is split too: both repeat their decoration, the inner one below the outer one.
     * <p>
     * clone 상자 안에서 같이 나뉘는 중첩된 상자: 둘 다 장식을 되풀이하며 안쪽 것은 바깥 것 아래에 있다.
     */
    public void testNestedCloneBoxes() {
        StringBuilder kids = new StringBuilder();
        // 높이 20px 블록 여덟 개를 만든다.
        for (int i = 0; i < 8; i++) {
            kids.append("<div style='height:20px'></div>");
        }
        LayoutResult r = LayoutTestSupport.layout("<div style='column-count:3;column-gap:0;column-fill:auto;width:300px;height:100px'>"
                + "<div id=o style='border:5px solid #000;box-decoration-break:clone'><div id=i style='border:5px solid #00f;box-decoration-break:clone'>" + kids + "</div></div></div>", 600);
        Element o = ((Element) r.getRoot().getNode()).getOwnerDocument().getElementById("o");
        Element i = ((Element) r.getRoot().getNode()).getOwnerDocument().getElementById("i");
        List<LayoutBox> outer = r.getBoxes(o);
        List<LayoutBox> inner = r.getBoxes(i);
        Check.equal(outer.size(), inner.size(), "both are split into the same columns");
        Check.isTrue(outer.size() >= 2, "the boxes are split");
        // 바깥 조각마다 안쪽 조각이 바깥 조각의 위쪽 장식(5) 아래에서 시작하고 바깥 조각 안에 들어 있다.
        for (int k = 0; k < outer.size(); k++) {
            Check.near(5, inner.get(k).getY() - outer.get(k).getY(), 0.5, "the inner fragment " + k + " starts below the top border of the outer one");
            Check.isTrue(inner.get(k).getY() + inner.get(k).getHeight() <= outer.get(k).getY() + outer.get(k).getHeight() + 0.01, "and ends inside it");
            Check.near(5, inner.get(k).getBorderTop(), 0.01, "the inner top border is repeated");
            Check.near(5, outer.get(k).getBorderBottom(), 0.01, "the outer bottom border is repeated");
        }
    }
}
