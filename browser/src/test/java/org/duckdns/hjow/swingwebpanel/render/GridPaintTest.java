package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting and the hit testing of grid items (plan item P10-03, CSS Grid Layout Module Level 1, section 9 "Grid Item
 * Painting"): grid items paint exactly like inline blocks, each as a whole, in the order-modified document order, and
 * <code>z-index</code> creates a stacking context even for items that are not positioned. The expected colors come from an
 * off-screen image of the real parser, CSS engine, layout engine and painter.
 * <p>
 * 그리드 항목의 그리기와 히트 테스트 시험(계획 P10-03, CSS Grid Layout Module Level 1, 9 절 "Grid Item Painting"): 그리드 항목은 inline-block 처럼 각각 통째로
 * order 를 반영한 문서 순서로 그려지고 <code>z-index</code> 는 위치 지정이 없는 항목에도 쌓임 맥락을 만든다. 기대하는 색은 실제 파서, CSS 엔진, 레이아웃 엔진, 페인터로 만든
 * 화면 밖 이미지에서 얻는다.
 */
public class GridPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public GridPaintTest() {
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
     * The cells of a grid are painted with the background of the items, the later item on top where items overlap (an item that
     * spans the cells of another one).
     * <p>
     * 그리드의 칸은 항목의 배경으로 그려지고 항목이 겹치는 곳(다른 항목의 칸에 걸친 항목)에서는 나중 항목이 위에 있다.
     */
    public void testOverlappingItemsAndOrder() {
        RenderedPage p = RenderTestSupport.page("<div style='display:grid;width:200px;grid-template-columns:100px 100px;grid-template-rows:50px'>"
                + "<div id=a style='grid-column:1 / 3;grid-row:1;background:#f00'></div><div id=b style='grid-column:2;grid-row:1;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 25), "a alone");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 150, 25), "b is above a where they overlap (later in the document)");
        Check.equal("b", idOf(p.hitTestDocument(150, 25)), "hit: b");
        Check.equal("a", idOf(p.hitTestDocument(50, 25)), "hit: a");
        RenderedPage q = RenderTestSupport.page("<div style='display:grid;width:200px;grid-template-columns:100px 100px;grid-template-rows:50px'>"
                + "<div id=a style='order:2;grid-column:1 / 3;grid-row:1;background:#f00'></div><div id=b style='grid-column:2;grid-row:1;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img2 = RenderTestSupport.paint(q, 300, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img2, 150, 25), "with order:2 a is painted last and covers b");
    }

    /**
     * <code>z-index</code> on a grid item that is not positioned puts it above later items.
     * <p>
     * 위치 지정이 없는 그리드 항목의 <code>z-index</code> 는 그 항목을 나중 항목 위에 올린다.
     */
    public void testZIndexOnGridItem() {
        RenderedPage p = RenderTestSupport.page("<div style='display:grid;width:200px;grid-template-columns:100px 100px;grid-template-rows:50px'>"
                + "<div id=a style='z-index:1;grid-column:1 / 3;grid-row:1;background:#f00'></div><div id=b style='grid-column:2;grid-row:1;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 150, 25), "a (z-index 1) is above the later item b");
        Check.equal("a", idOf(p.hitTestDocument(150, 25)), "hit: a");
    }

    /**
     * The grid container paints its own background and border below the items.
     * <p>
     * 그리드 컨테이너는 자신의 배경과 border 를 항목 아래에 그린다.
     */
    public void testContainerBackgroundAndGaps() {
        RenderedPage p = RenderTestSupport.page("<div style='display:grid;width:110px;grid-template-columns:50px 50px;gap:10px;background:#ff0'>"
                + "<div style='height:20px;background:#f00'></div><div style='height:20px;background:#00f'></div></div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 25, 10), "first item");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 55, 10), "the gap shows the background of the container");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 80, 10), "second item");
    }
}
