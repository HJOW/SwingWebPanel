package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting and the input of a box that moves along an offset path (plan item P10-08, CSS Motion Path Level 1): the box is painted where the path puts it,
 * is rotated with the path, is found by a click there and not at the place that it has in the layout, and the layout of the other boxes does not change.
 * <p>
 * 오프셋 경로를 따라 움직이는 상자의 그리기와 입력 시험(계획 P10-08, CSS Motion Path Level 1): 상자는 경로가 놓은 자리에 그려지고 경로에 따라 회전하며, 레이아웃에서 가진 자리가 아니라
 * 거기에서 클릭으로 찾아지고, 다른 상자의 배치는 바뀌지 않는다.
 */
public class MotionPathPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MotionPathPaintTest() {
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
     * A red 20 by 10 box that the path moves 100 px to the right and 50 px down is painted there and found there; its place in the layout stays empty, and the box that
     * follows it in the flow is not moved.
     * <p>
     * 경로가 오른쪽으로 100 px, 아래로 50 px 옮긴 빨간 20x10 상자는 거기에 그려지고 거기에서 찾아진다. 레이아웃에서의 자리는 비고, 흐름에서 뒤따르는 상자는 옮겨지지 않는다.
     */
    public void testPaintAndHit() {
        RenderedPage p = RenderTestSupport.page("<div id=m style='position:absolute;left:0;top:0;width:20px;height:10px;background:#f00;offset-anchor:left top;"
                + "offset-rotate:0deg;offset-path:path(\"M 100 50 L 200 50\");offset-distance:0'></div><div id=s style='margin-top:100px;width:20px;height:10px;background:#00f'></div>",
                300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 110, 55), "the box is painted at the start of the path");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 10, 5), "and not at the place that it has in the layout");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 10, 105), "the box that follows keeps its place");
        Check.equal("m", idOf(p.hitTestDocument(110, 55)), "a click finds the moved box where it is painted");
        Check.isFalse("m".equals(idOf(p.hitTestDocument(10, 5))), "and not at its place in the layout");
    }

    /**
     * With <code>offset-rotate: auto</code> the painted box is turned along a vertical path: the wide box becomes a tall one, which the pixels and the click show.
     * <p>
     * <code>offset-rotate: auto</code> 이면 그려진 상자가 세로 경로를 따라 돈다: 넓은 상자가 키 큰 상자가 되며 화소와 클릭이 그것을 보여 준다.
     */
    public void testRotatedAlongThePath() {
        RenderedPage p = RenderTestSupport.page("<div id=m style='position:absolute;left:0;top:0;width:40px;height:10px;background:#f00;offset-anchor:left top;"
                + "offset-path:path(\"M 100 20 L 100 120\");offset-distance:0'></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 95, 50), "a quarter turn clockwise: the 40 px long side runs down from the start (95, 50)");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 120, 25), "and the box is not wide any more");
        Check.equal("m", idOf(p.hitTestDocument(95, 50)), "the click follows the rotation");
        Check.isFalse("m".equals(idOf(p.hitTestDocument(120, 25))), "and misses where the wide box would have been");
    }
}
