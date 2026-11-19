package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>clip-path</code> (plan item P10-08, CSS Masking Level 1 section 5): the basic shapes <code>inset()</code>,
 * <code>circle()</code>, <code>ellipse()</code> and <code>polygon()</code> clip what is painted and what is hit, the reference boxes,
 * a value that cannot be used clips nothing, and a transform and a clip path work together.
 * <p>
 * <code>clip-path</code> 시험(계획 P10-08, CSS Masking Level 1 5 절): 기본 도형 <code>inset()</code>, <code>circle()</code>, <code>ellipse()</code>,
 * <code>polygon()</code> 이 그려지는 것과 히트되는 것을 자르고, 기준 상자, 쓸 수 없는 값은 아무것도 자르지 않으며, 변환과 clip-path 가 함께 동작한다.
 */
public class ClipPathPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ClipPathPaintTest() {
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
     * Paints a 100 by 100 red box with a <code>clip-path</code> value and returns the page.
     * <p>
     * <code>clip-path</code> 값을 가진 100x100 빨간 상자를 그리고 페이지를 반환한다.
     *
     * @param value the value of <code>clip-path</code> and other declarations / <code>clip-path</code> 값과 다른 선언
     * @return the page / 페이지
     */
    private static RenderedPage page(String value) {
        return RenderTestSupport.page("<div id=a style='width:100px;height:100px;background:#f00;" + value + "'></div>", 300, 200, null);
    }

    /**
     * A circle: the center and the inside are painted and hit, the corners and the outside are neither.
     * <p>
     * 원: 중심과 안쪽은 그려지고 히트되며 모서리와 바깥은 그렇지 않다.
     */
    public void testCircleAndEllipse() {
        RenderedPage p = page("clip-path:circle(30px at 50% 50%)");
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 50), "center");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 78, 50), "inside near the edge");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 83, 50), "outside the circle");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 5, 5), "the corner of the box");
        Check.equal("a", idOf(p.hitTestDocument(50, 50)), "hit in the circle");
        Check.isFalse("a".equals(idOf(p.hitTestDocument(5, 5))), "no hit in the clipped corner");
        RenderedPage e = page("clip-path:ellipse(40px 20px at 50% 50%)");
        BufferedImage ie = RenderTestSupport.paint(e, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(ie, 85, 50), "wide axis");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(ie, 50, 25), "outside on the short axis");
        Check.equal(0xFF0000, RenderTestSupport.rgb(ie, 50, 35), "inside on the short axis");
        RenderedPage d = page("clip-path:circle(closest-side)");
        Check.equal(0xFF0000, RenderTestSupport.rgb(RenderTestSupport.paint(d, 300, 200), 50, 5), "closest-side radius is 50px");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(RenderTestSupport.paint(d, 300, 200), 5, 5), "corner");
    }

    /**
     * An inset (with a rounded corner), a polygon and the reference boxes.
     * <p>
     * inset(둥근 모서리 포함), 다각형, 기준 상자.
     */
    public void testInsetPolygonAndBoxes() {
        RenderedPage p = page("clip-path:inset(10px 20px 30px 40px)");
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 20, 50), "left of the inset");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 50), "inside the inset");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 90, 50), "right of the inset (20px)");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 50, 80), "below the inset (30px)");
        RenderedPage r = page("clip-path:inset(0 round 40px)");
        BufferedImage ir = RenderTestSupport.paint(r, 300, 200);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(ir, 1, 1), "the rounded corner is cut");
        Check.equal(0xFF0000, RenderTestSupport.rgb(ir, 50, 3), "the straight edge stays");
        RenderedPage t = page("clip-path:polygon(50% 0, 100% 100%, 0 100%)");
        BufferedImage it = RenderTestSupport.paint(t, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(it, 50, 20), "triangle: inside near the apex");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(it, 10, 10), "triangle: outside");
        Check.equal(0xFF0000, RenderTestSupport.rgb(it, 50, 90), "triangle: base");
        Check.equal("a", idOf(t.hitTestDocument(50, 90)), "hit in the triangle");
        Check.isFalse("a".equals(idOf(t.hitTestDocument(5, 5))), "no hit outside of the triangle");
        RenderedPage c = RenderTestSupport.page("<div id=a style='width:60px;height:60px;padding:20px;background:#f00;clip-path:inset(0) content-box'></div>", 300, 200, null);
        BufferedImage ic = RenderTestSupport.paint(c, 300, 200);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(ic, 10, 10), "padding area is outside of the content box");
        Check.equal(0xFF0000, RenderTestSupport.rgb(ic, 50, 50), "content box");
    }

    /**
     * A value that cannot be used clips nothing; a clip path together with a transform follows the box to where it is painted.
     * <p>
     * 쓸 수 없는 값은 아무것도 자르지 않는다. clip-path 는 변환과 함께 쓰면 상자가 그려지는 곳을 따라간다.
     */
    public void testUnusableValueAndTransform() {
        RenderedPage p = page("clip-path:shape(from 0 0, line to 10px 10px)");
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 5, 50), "a function that is not supported clips nothing");
        RenderedPage t = page("transform:translate(150px,0);clip-path:circle(30px at 50% 50%)");
        BufferedImage it = RenderTestSupport.paint(t, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(it, 200, 50), "painted at the translated place");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(it, 155, 5), "its corner is clipped");
        Check.equal("a", idOf(t.hitTestDocument(200, 50)), "hit at the translated center");
        Check.isFalse("a".equals(idOf(t.hitTestDocument(155, 5))), "no hit at the clipped corner");
    }
}
