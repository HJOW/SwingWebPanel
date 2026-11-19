package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting and the hit testing of flex items (plan item P10-03, CSS Flexbox 4.2 "Flex Item Painting"): flex items paint
 * exactly like inline blocks, each as a whole, in the order-modified document order, and <code>z-index</code> creates a stacking
 * context even for items that are not positioned. The expected colors come from an off-screen image of the real parser, CSS engine,
 * layout engine and painter, and the hit test must find what is on top.
 * <p>
 * 플렉스 항목의 그리기와 히트 테스트 시험(계획 P10-03, CSS Flexbox 4.2 "Flex Item Painting"): 플렉스 항목은 inline-block 처럼 각각 통째로 order 를 반영한 문서
 * 순서로 그려지고 <code>z-index</code> 는 위치 지정이 없는 항목에도 쌓임 맥락을 만든다. 기대하는 색은 실제 파서, CSS 엔진, 레이아웃 엔진, 페인터로 만든 화면 밖
 * 이미지에서 얻고 히트 테스트는 맨 위에 있는 것을 찾아야 한다.
 */
public class FlexPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FlexPaintTest() {
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
     * The items paint in the order-modified order: the later item in the order is on top, whatever the document order is; the hit test
     * agrees.
     * <p>
     * 항목은 order 를 반영한 순서로 그려진다: 순서가 나중인 항목이 문서 순서와 상관없이 위에 있고 히트 테스트도 같다.
     */
    public void testOrderChangesPaintOrder() {
        RenderedPage p = RenderTestSupport.page("<div style='display:flex'><div id=a style='order:2;width:60px;height:40px;background:#f00'></div>"
                + "<div id=b style='order:1;width:60px;height:40px;margin-left:-30px;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        // 순서는 b, a: b 가 0..60, a 가 30..90 에 놓이고 a 가 위에 그려진다.
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 10, 20), "b alone");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 45, 20), "a (order 2) is above b in the overlap although it comes first in the document");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 80, 20), "a alone");
        Check.equal("a", idOf(p.hitTestDocument(45, 20)), "hit: the item that is painted last");
        Check.equal("b", idOf(p.hitTestDocument(10, 20)), "hit: b alone");
    }

    /**
     * Without <code>order</code> the later item in the document is above an overlapping earlier one.
     * <p>
     * <code>order</code> 가 없으면 문서에서 나중인 항목이 겹치는 앞선 항목 위에 있다.
     */
    public void testDocumentOrderPaintsLaterOnTop() {
        RenderedPage p = RenderTestSupport.page("<div style='display:flex'><div id=a style='width:60px;height:40px;background:#f00'></div>"
                + "<div id=b style='width:60px;height:40px;margin-left:-30px;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 45, 20), "b is above a in the overlap");
        Check.equal("b", idOf(p.hitTestDocument(45, 20)), "hit: b");
    }

    /**
     * <code>z-index</code> on an item that is not positioned creates a stacking context: the item with the higher value is on top.
     * <p>
     * 위치 지정이 없는 항목의 <code>z-index</code> 는 쌓임 맥락을 만든다: 값이 큰 항목이 위에 있다.
     */
    public void testZIndexOnStaticItem() {
        RenderedPage p = RenderTestSupport.page("<div style='display:flex'><div id=a style='z-index:1;width:60px;height:40px;background:#f00'></div>"
                + "<div id=b style='width:60px;height:40px;margin-left:-30px;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 45, 20), "a (z-index 1) is above the later sibling b");
        Check.equal("a", idOf(p.hitTestDocument(45, 20)), "hit: a");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 75, 20), "b alone");
    }

    /**
     * Every item is painted as a whole, like an inline block: the background of a later item covers the text of an earlier item that
     * it overlaps (in a block flow all backgrounds are painted first and the text would stay visible).
     * <p>
     * 모든 항목은 inline-block 처럼 통째로 그려진다: 나중 항목의 배경은 겹치는 앞 항목의 텍스트를 덮는다(블록 흐름이라면 배경이 먼저 모두 그려지고 텍스트가 보인다).
     */
    public void testItemsPaintAsAWhole() {
        RenderedPage p = RenderTestSupport.page("<div style='display:flex'><div id=a style='width:80px;height:40px;font:30px monospace;color:#000'>HH</div>"
                + "<div id=b style='width:80px;height:40px;margin-left:-80px;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        int black = 0;
        // 겹친 영역에 검은 글자 픽셀이 남아 있지 않아야 한다.
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 80; x++) {
                if (RenderTestSupport.rgb(img, x, y) == 0x000000) {
                    black++;
                }
            }
        }
        Check.equal(0, black, "the text of the earlier item is covered by the later item");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 40, 20), "the later item is on top");
    }

    /**
     * The container paints its own background and border below the items, and the items are positioned inside its content box.
     * <p>
     * 컨테이너는 자신의 배경과 border 를 항목 아래에 그리고 항목은 content box 안에 놓인다.
     */
    public void testContainerBackgroundAndBorder() {
        RenderedPage p = RenderTestSupport.page("<div id=c style='display:flex;width:100px;border:5px solid #0f0;padding:5px;background:#ff0'>"
                + "<div style='width:30px;height:20px;background:#f00'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 2, 2), "border");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 7, 7), "padding shows the background of the container");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 15, 15), "the item starts at the content box (border 5 + padding 5)");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 60, 15), "container background right of the item");
    }
}
