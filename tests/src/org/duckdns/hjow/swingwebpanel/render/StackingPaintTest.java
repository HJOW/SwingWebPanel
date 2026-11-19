package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting order and the input mapping for floats, positioned boxes, stacking contexts and clipping (plan item P10-01): the
 * order of CSS 2.1 Appendix E is checked with real pixels of an off-screen image and with the order of the display list, and the hit
 * test is checked to find exactly what is on top at a point (a higher <code>z-index</code>, a float over a block, a positioned box
 * that is not clipped by an ancestor that is not its containing block, content that is clipped by <code>overflow</code> or
 * <code>clip</code>, boxes outside the viewport, fixed boxes while the page is scrolled).
 * <p>
 * float, 위치 지정 상자, 쌓임 맥락, 자르기의 그리기 순서와 입력 대응 시험(계획 P10-01): CSS 2.1 부록 E 의 순서를 화면 밖 이미지의 실제 픽셀과 디스플레이 리스트의
 * 순서로 확인하고, 히트 테스트가 점에서 맨 위에 있는 것을 정확히 찾는지(더 높은 <code>z-index</code>, 블록 위의 float, 포함 블록이 아닌 조상에게 잘리지 않는 위치
 * 지정 상자, <code>overflow</code> 나 <code>clip</code> 에 잘린 내용, 뷰포트 밖의 상자, 스크롤된 페이지의 fixed 상자) 확인한다.
 */
public class StackingPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StackingPaintTest() {
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
     * A higher z-index paints on top whatever the source order; without z-index the later box is on top; the hit test agrees.
     * <p>
     * 더 높은 z-index 가 소스 순서와 상관없이 위에 그려지고 z-index 가 없으면 나중 상자가 위에 있다. 히트 테스트도 같다.
     */
    public void testZIndexOrder() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='position:absolute;left:0;top:0;width:60px;height:60px;background:#f00;z-index:2'></div>"
                + "<div id=b style='position:absolute;left:30px;top:30px;width:60px;height:60px;background:#00f;z-index:1'></div>"
                + "<div id=c style='position:absolute;left:100px;top:0;width:60px;height:60px;background:#0f0'></div>"
                + "<div id=d style='position:absolute;left:130px;top:30px;width:60px;height:60px;background:#ff0'></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 45, 45), "z-index 2 is above z-index 1 in the overlap");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 80, 80), "b alone");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 145, 45), "the later box is above the earlier one without z-index");
        Check.equal("a", idOf(p.hitTestDocument(45, 45)), "hit: z-index 2");
        Check.equal("d", idOf(p.hitTestDocument(145, 45)), "hit: later box");
        Check.equal("c", idOf(p.hitTestDocument(105, 5)), "hit: c where d is not");
    }

    /**
     * A context with a negative z-index is painted behind the in-flow blocks of its parent context (above the background of the root
     * only); a positive one is above everything else.
     * <p>
     * 음수 z-index 맥락은 부모 맥락의 정상 흐름 블록 뒤에 그려지고(루트의 배경 위에만) 양수는 다른 모든 것 위에 그려진다.
     */
    public void testNegativeAndPositiveZIndex() {
        RenderedPage p = RenderTestSupport.page("<div id=flow style='width:100px;height:60px;background:#00f'></div>"
                + "<div id=neg style='position:absolute;left:20px;top:20px;width:100px;height:100px;background:#f00;z-index:-1'></div>"
                + "<div id=pos style='position:absolute;left:60px;top:40px;width:100px;height:100px;background:#0f0;z-index:5'></div>"
                + "<div id=zero style='position:absolute;left:80px;top:60px;width:100px;height:100px;background:#ff0;z-index:0'></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 30, 30), "the in-flow block covers the negative context");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 30, 80), "the negative context is visible where nothing covers it");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 100, 70), "the positive context is above zero");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 170, 150), "z-index 0 alone");
        Check.equal("flow", idOf(p.hitTestDocument(30, 30)), "hit: the block is above the negative context");
        Check.equal("neg", idOf(p.hitTestDocument(30, 80)), "hit: the negative context where it shows");
        Check.equal("pos", idOf(p.hitTestDocument(100, 70)), "hit: positive");
    }

    /**
     * A float is above the backgrounds of in-flow blocks, below the inline content (text) and below positioned boxes.
     * <p>
     * float 는 정상 흐름 블록의 배경 위, 인라인 콘텐츠(텍스트) 아래, 위치 지정 상자 아래에 있다.
     */
    public void testFloatOrder() {
        RenderedPage p = RenderTestSupport.page("<div id=blk style='height:50px;background:#00f'></div>"
                + "<div id=fl style='float:left;width:40px;height:40px;margin-top:-30px;background:#f00'></div>"
                + "<div id=ab style='position:absolute;left:20px;top:20px;width:10px;height:10px;background:#0f0'></div>"
                + "<p id=tx style='margin:0;font:20px sans-serif;color:#000'>HHHH</p>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 5, 25), "the float is above the block background");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 25, 25), "the positioned box is above the float");
        // 디스플레이 리스트에서도 float 의 배경이 텍스트보다 먼저, 블록 배경보다 나중에 온다.
        DisplayList l = p.getDisplayList();
        int blk = -1;
        int fl = -1;
        int tx = -1;
        // 명령을 훑으며 각 요소의 첫 명령 위치를 찾는다.
        for (int i = 0; i < l.size(); i++) {
            DisplayCommand c = l.get(i);
            String id = c.getNode() instanceof org.duckdns.hjow.swingwebpanel.dom.Element ? ((org.duckdns.hjow.swingwebpanel.dom.Element) c.getNode()).getAttribute("id") : null;
            if (c.getType() == DisplayCommand.Type.FILL_RECT && "blk".equals(id) && blk < 0) {
                blk = i;
            } else if (c.getType() == DisplayCommand.Type.FILL_RECT && "fl".equals(id) && fl < 0) {
                fl = i;
            } else if (c.getType() == DisplayCommand.Type.TEXT && tx < 0) {
                tx = i;
            }
        }
        Check.isTrue(blk >= 0 && fl > blk && tx > fl, "order: block background " + blk + " < float " + fl + " < text " + tx);
    }

    /**
     * A box inside an overflow:hidden box is clipped only if the clipping box is on its chain of containing blocks: an absolutely
     * positioned box whose containing block is outside the clip is not clipped, one whose containing block is the clipping box is.
     * <p>
     * overflow:hidden 상자 안의 상자는 자르는 상자가 포함 블록 사슬에 있을 때만 잘린다: 포함 블록이 자르기 밖에 있는 절대 배치 상자는 잘리지 않고 포함 블록이
     * 자르는 상자인 것은 잘린다.
     */
    public void testClippingFollowsContainingBlockChain() {
        RenderedPage free = RenderTestSupport.page("<div style='overflow:hidden;width:50px;height:50px'>"
                + "<div id=x style='position:absolute;left:0;top:0;width:100px;height:100px;background:#f00'></div></div>", 200, 200, null);
        BufferedImage a = RenderTestSupport.paint(free, 200, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(a, 80, 80), "not clipped: its containing block is the initial one");
        Check.equal("x", idOf(free.hitTestDocument(80, 80)), "hit: not clipped either");
        RenderedPage clipped = RenderTestSupport.page("<div style='position:relative;overflow:hidden;width:50px;height:50px'>"
                + "<div id=x style='position:absolute;left:0;top:0;width:100px;height:100px;background:#f00'></div></div>", 200, 200, null);
        BufferedImage b = RenderTestSupport.paint(clipped, 200, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(b, 30, 30), "inside the clip");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(b, 80, 80), "outside the clip");
        Check.isFalse("x".equals(idOf(clipped.hitTestDocument(80, 80))), "hit: the clipped part is not found");
        Check.equal("x", idOf(clipped.hitTestDocument(30, 30)), "hit: the visible part is found");
    }

    /**
     * In-flow content is clipped by overflow:hidden for painting and for hit testing, also content that overflows to the side.
     * <p>
     * 정상 흐름 콘텐츠는 그리기와 히트 테스트 모두에서 overflow:hidden 에 잘린다. 옆으로 넘치는 콘텐츠도 그렇다.
     */
    public void testOverflowHiddenClipsInFlowContent() {
        RenderedPage p = RenderTestSupport.page("<div id=o style='overflow:hidden;width:50px;height:50px;border:5px solid #000'>"
                + "<div id=in style='width:200px;height:200px;background:#f00'></div></div><div id=below style='height:10px;background:#0f0'></div>", 300, 300, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 300);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 30, 30), "inside the padding box");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 100, 30), "right of the box: clipped");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 100, 65), "the next block is not hidden by the overflow");
        Check.equal("in", idOf(p.hitTestDocument(30, 30)), "hit inside");
        Check.isFalse("in".equals(idOf(p.hitTestDocument(100, 30))), "no hit on the clipped part");
        Check.equal("o", idOf(p.hitTestDocument(2, 2)), "the border still belongs to the box");
    }

    /**
     * <code>clip: rect()</code> clips an absolutely positioned box for painting and hit testing.
     * <p>
     * <code>clip: rect()</code> 는 절대 배치 상자를 그리기와 히트 테스트에서 자른다.
     */
    public void testClipRect() {
        RenderedPage p = RenderTestSupport.page("<div id=c style='position:absolute;left:10px;top:10px;width:100px;height:100px;background:#f00;"
                + "clip:rect(20px,80px,80px,20px)'></div>", 200, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 200);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 15, 15), "above-left of the clip rectangle");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 50), "inside the clip rectangle");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 100, 100), "below-right of the clip rectangle");
        Check.equal("c", idOf(p.hitTestDocument(50, 50)), "hit inside");
        Check.isFalse("c".equals(idOf(p.hitTestDocument(15, 15))), "no hit outside the clip rectangle");
    }

    /**
     * A fixed box stays at the same place of the viewport when the page is scrolled, and the hit test follows it.
     * <p>
     * fixed 상자는 페이지가 스크롤되어도 뷰포트의 같은 자리에 있고 히트 테스트도 그것을 따라간다.
     */
    public void testFixedBoxDoesNotScroll() {
        RenderedPage p = RenderTestSupport.page("<div id=tall style='height:1000px;background:#eee'></div>"
                + "<div id=fx style='position:fixed;left:20px;top:30px;width:40px;height:40px;background:#f00'></div>", 200, 100, null);
        Check.equal(1000, p.getPreferredHeight(), "the fixed box does not change the page height");
        BufferedImage top = RenderTestSupport.blank(200, 100);
        Graphics2D g = top.createGraphics();
        p.paint(g, new Rectangle(0, 0, 200, 100), 0, 0);
        g.dispose();
        BufferedImage scrolled = RenderTestSupport.blank(200, 100);
        g = scrolled.createGraphics();
        p.paint(g, new Rectangle(0, 0, 200, 100), 0, 500);
        g.dispose();
        Check.equal(0xFF0000, RenderTestSupport.rgb(top, 40, 50), "fixed box at scroll 0");
        Check.equal(0xFF0000, RenderTestSupport.rgb(scrolled, 40, 50), "fixed box at scroll 500: same place of the viewport");
        Check.equal(0xEEEEEE, RenderTestSupport.rgb(scrolled, 100, 50), "the page scrolled");
        Check.equal("fx", idOf(p.hitTest(40, 50, 0, 500, 1.0)), "hit at the viewport position while scrolled");
        Check.equal("tall", idOf(p.hitTest(150, 50, 0, 500, 1.0)), "the page behind the fixed box");
        Check.isFalse("fx".equals(idOf(p.hitTestDocument(40, 540))), "the document position of the scrolled viewport is not the fixed box without scroll info");
    }

    /**
     * Boxes far outside the viewport have document coordinates that can be hit; boxes that are only moved there by the layout do not
     * influence what is found inside the viewport.
     * <p>
     * 뷰포트에서 멀리 벗어난 상자도 히트할 수 있는 문서 좌표를 가지며 배치가 그곳으로 옮긴 상자는 뷰포트 안에서 찾는 것에 영향을 주지 않는다.
     */
    public void testOffscreenBoxesCanBeHit() {
        RenderedPage p = RenderTestSupport.page("<div id=far style='position:absolute;left:3000px;top:2000px;width:50px;height:50px;background:#f00'></div>"
                + "<div id=neg style='position:absolute;left:-500px;top:-500px;width:50px;height:50px;background:#00f'></div>"
                + "<div id=near style='width:30px;height:30px;background:#0f0'></div>", 200, 100, null);
        Check.equal("far", idOf(p.hitTestDocument(3020, 2020)), "far right and down");
        Check.equal("neg", idOf(p.hitTestDocument(-480, -480)), "negative coordinates");
        Check.equal("near", idOf(p.hitTestDocument(10, 10)), "inside the viewport");
        Check.equal(3050, p.getPreferredWidth(), "the scrollable width includes the far box");
        Check.equal(2050, p.getPreferredHeight(), "the scrollable height includes the far box");
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 10, 10), "painted");
    }

    /**
     * A box with visibility:hidden or pointer-events:none is not found by the hit test but what is below it is.
     * <p>
     * visibility:hidden 이나 pointer-events:none 상자는 히트 테스트에서 찾지 않고 그 아래의 것을 찾는다.
     */
    public void testHiddenBoxesAreNotHit() {
        RenderedPage p = RenderTestSupport.page("<div id=base style='width:100px;height:100px;background:#0f0'></div>"
                + "<div id=h style='position:absolute;left:0;top:0;width:50px;height:50px;visibility:hidden;background:#f00'></div>"
                + "<div id=n style='position:absolute;left:50px;top:0;width:50px;height:50px;pointer-events:none;background:#00f'></div>", 200, 200, null);
        Check.equal("base", idOf(p.hitTestDocument(10, 10)), "hidden box");
        Check.equal("base", idOf(p.hitTestDocument(60, 10)), "pointer-events:none box");
    }

    /**
     * An opacity context keeps its positioned descendants in its own group and paints them in z-order above its in-flow content.
     * <p>
     * 불투명도 맥락은 위치 지정 자손을 자기 그룹 안에 두고 정상 흐름 콘텐츠 위에 z 순서로 그린다.
     */
    public void testOpacityContextPaintsPositionedChildren() {
        RenderedPage p = RenderTestSupport.page("<div style='opacity:0.5;width:100px;height:100px;position:relative'>"
                + "<div style='width:60px;height:60px;background:#000'></div>"
                + "<div style='position:absolute;left:30px;top:30px;width:60px;height:60px;background:#000'></div></div>", 200, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 200);
        // 겹침에서도 한 번만 섞이므로 두 상자가 겹친 곳과 한 상자만 있는 곳의 색이 같다.
        Check.equal(RenderTestSupport.rgb(img, 10, 10), RenderTestSupport.rgb(img, 40, 40), "the group is blended once");
        Check.isTrue(RenderTestSupport.near(0x808080, RenderTestSupport.rgb(img, 10, 10), 3), "half transparent black is gray");
    }
}
