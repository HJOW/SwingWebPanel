package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting and the hit testing of multi-column containers (plan item P10-03): column rules between the columns (solid,
 * and none), the fragments of a box that is split between columns (the border and the background of every fragment, the top edge only
 * on the first and the bottom edge only on the last fragment), the content in the columns and the hit test in the second column. The
 * expected colors come from an off-screen image of the real parser, CSS engine, layout engine and painter.
 * <p>
 * 다단 컨테이너의 그리기와 히트 테스트 시험(계획 P10-03): 열 사이의 열 구분선(실선, none), 열 사이에서 나뉜 상자의 조각(조각마다 border 와 배경, 위쪽 가장자리는
 * 첫 조각에만 아래쪽 가장자리는 마지막 조각에만), 열 안의 내용, 둘째 열에서의 히트 테스트. 기대하는 색은 실제 파서, CSS 엔진, 레이아웃 엔진, 페인터로 만든 화면 밖
 * 이미지에서 얻는다.
 */
public class MulticolPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MulticolPaintTest() {
    }

    /**
     * Returns the id of the element of a hit.
     * <p>
     * 히트의 요소 id 를 반환한다.
     *
     * @param h the hit / 히트
     * @return the id or "" / id 또는 ""
     */
    private static String idOf(HitResult h) {
        return h.getElement() == null || h.getElement().getAttribute("id") == null ? "" : h.getElement().getAttribute("id");
    }

    /**
     * A solid column rule is painted in the middle of the gap, as high as the columns, and not at all with <code>none</code>.
     * <p>
     * 실선 열 구분선은 gap 한가운데에 열과 같은 높이로 그려지고 <code>none</code> 이면 그려지지 않는다.
     */
    public void testColumnRulesArePainted() {
        RenderedPage p = RenderTestSupport.page("<div id=c style='width:220px;column-count:3;column-gap:20px;column-rule:4px solid #f00'>"
                + "<div style='height:30px;background:#ccc'></div><div style='height:30px;background:#ccc'></div><div style='height:30px;background:#ccc'></div></div>"
                + "<div style='margin-top:20px;width:220px;column-count:3;column-gap:20px;column-rule:4px none #f00'>"
                + "<div style='height:30px;background:#ccc'></div><div style='height:30px;background:#ccc'></div><div style='height:30px;background:#ccc'></div></div>", 300, 120, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 120);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 70, 15), "the first rule (x 68..72)");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 150, 15), "the second rule (x 148..152)");
        Check.equal(0xCCCCCC, RenderTestSupport.rgb(img, 30, 15), "the content of the first column");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 62, 15), "the gap beside the rule is empty");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 70, 28), "the rule is as high as the columns");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 70, 34), "and not higher");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 70, 65), "no rule with style none");
    }

    /**
     * The fragments of a box that is split between two columns are painted with the border and the background of the box, the top
     * border only in the first column and the bottom border only in the second one.
     * <p>
     * 두 열 사이에서 나뉜 상자의 조각은 상자의 border 와 배경으로 그려지고 위쪽 border 는 첫 열에만 아래쪽 border 는 둘째 열에만 있다.
     */
    public void testFragmentsKeepOnlyTheirEdges() {
        RenderedPage p = RenderTestSupport.page("<div style='width:200px;column-count:2;column-gap:0'><div id=box style='border:2px solid #00f;background:#ff0'>"
                + "<div style='height:20px'></div><div style='height:20px'></div><div style='height:20px'></div><div style='height:20px'></div></div></div>", 300, 120, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 120);
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 50, 1), "the top border of the first fragment");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 50, 40), "no bottom border at the cut (background)");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 150, 1), "no top border at the top of the second column (background)");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 150, 41), "the bottom border of the last fragment");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 1, 20), "left border of the first fragment");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 98, 20), "right border of the first fragment");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 101, 20), "left border of the second fragment");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 198, 20), "right border of the second fragment");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 150, 60), "below the columns nothing is painted");
    }

    /**
     * Text in the second column is painted there and found by the hit test.
     * <p>
     * 둘째 열의 텍스트는 거기에 그려지고 히트 테스트가 찾는다.
     */
    public void testContentInSecondColumn() {
        RenderedPage p = RenderTestSupport.page("<div style='width:200px;column-count:2;column-gap:0'><div id=a style='height:20px;background:#f00'></div>"
                + "<div id=b style='height:20px;background:#0f0'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 10), "a in the first column");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 150, 10), "b in the second column");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 50, 30), "the first column is only 20 pixel high");
        Check.equal("b", idOf(p.hitTestDocument(150, 10)), "hit: b");
        Check.equal("a", idOf(p.hitTestDocument(50, 10)), "hit: a");
    }
}
