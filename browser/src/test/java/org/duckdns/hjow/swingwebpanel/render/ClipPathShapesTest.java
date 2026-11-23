package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the later basic shapes of <code>clip-path</code> (plan item P10-08, CSS Masking Level 1 section 5): <code>path()</code> with the
 * two fill rules, <code>rect()</code> with <code>auto</code>, <code>xywh()</code>, the radii of <code>round</code> for each corner and each
 * axis, a reference box alone, and the values that are not usable.
 * <p>
 * <code>clip-path</code> 의 나중에 더한 기본 도형 시험(계획 P10-08, CSS Masking Level 1 5 절): 두 채움 규칙의 <code>path()</code>, <code>auto</code> 가 있는
 * <code>rect()</code>, <code>xywh()</code>, <code>round</code> 의 모서리별 축별 반지름, 기준 상자만 있는 값, 쓸 수 없는 값.
 */
public class ClipPathShapesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ClipPathShapesTest() {
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
     * @param value the declarations / 선언
     * @return the page / 페이지
     */
    private static RenderedPage page(String value) {
        return RenderTestSupport.page("<div id=a style='width:100px;height:100px;background:#f00;" + value + "'></div>", 300, 200, null);
    }

    /**
     * Checks a pixel.
     * <p>
     * 픽셀을 확인한다.
     *
     * @param img  the image / 이미지
     * @param x    x / x
     * @param y    y / y
     * @param rgb  the expected RGB / 기대 RGB
     * @param what the message / 메시지
     */
    private static void px(BufferedImage img, int x, int y, int rgb, String what) {
        Check.equal(rgb, RenderTestSupport.rgb(img, x, y), what);
    }

    /**
     * <code>path()</code>: the path is placed at the top-left corner of the reference box; non-zero fills nested squares, even-odd leaves a
     * hole; a path without a drawing segment is not usable and clips nothing.
     * <p>
     * <code>path()</code>: 경로는 기준 상자의 왼쪽 위에 놓인다. nonzero 는 중첩된 사각형을 채우고 evenodd 는 구멍을 낸다. 그리는 구간이 없는 경로는 쓸 수 없어 아무것도 자르지 않는다.
     */
    public void testPath() {
        RenderedPage tri = page("clip-path:path(\"M0 0 L100 0 L50 100 Z\")");
        BufferedImage it = RenderTestSupport.paint(tri, 300, 200);
        px(it, 50, 10, 0xFF0000, "inside the triangle near the top");
        px(it, 5, 90, 0xFFFFFF, "outside at the bottom left");
        px(it, 95, 90, 0xFFFFFF, "outside at the bottom right");
        Check.equal("a", idOf(tri.hitTestDocument(50, 10)), "hit in the path");
        Check.isFalse("a".equals(idOf(tri.hitTestDocument(5, 90))), "no hit outside of the path");
        String nested = "M0 0 H100 V100 H0 Z M25 25 H75 V75 H25 Z";
        px(RenderTestSupport.paint(page("clip-path:path(nonzero,\"" + nested + "\")"), 300, 200), 50, 50, 0xFF0000, "non-zero fills the inner square (same direction)");
        BufferedImage eo = RenderTestSupport.paint(page("clip-path:path(evenodd,\"" + nested + "\")"), 300, 200);
        px(eo, 50, 50, 0xFFFFFF, "even-odd leaves a hole");
        px(eo, 10, 10, 0xFF0000, "even-odd keeps the ring");
        px(RenderTestSupport.paint(page("clip-path:path(\"foo\")"), 300, 200), 50, 50, 0xFF0000, "path data without a drawing segment clips nothing");
        px(RenderTestSupport.paint(page("clip-path:path(1)"), 300, 200), 50, 50, 0xFF0000, "a path() without a string clips nothing");
        // 박스가 옮겨진 곳(margin)에서도 경로는 상자의 왼쪽 위 기준이다.
        RenderedPage m = RenderTestSupport.page("<div id=a style='margin:50px;width:100px;height:100px;background:#f00;clip-path:path(\"M0 0 H40 V40 H0 Z\")'></div>", 300, 200, null);
        BufferedImage im = RenderTestSupport.paint(m, 300, 200);
        px(im, 60, 60, 0xFF0000, "relative to the box");
        px(im, 100, 100, 0xFFFFFF, "outside of the path");
    }

    /**
     * The two rectangle shapes <code>rect()</code> and <code>xywh()</code>: edges, auto, percentages, round and invalid values.
     * 사각형 도형 <code>rect()</code> 와 <code>xywh()</code>: 가장자리, auto, 백분율, round, 올바르지 않은 값.
     * <code>rect()</code> 와 <code>xywh()</code>.
     */
    public void testRectAndXywh() {
        BufferedImage r = RenderTestSupport.paint(page("clip-path:rect(10px 90px 80px 20px)"), 300, 200);
        px(r, 50, 50, 0xFF0000, "inside rect()");
        px(r, 10, 50, 0xFFFFFF, "left of rect()");
        px(r, 95, 50, 0xFFFFFF, "right of rect()");
        px(r, 50, 5, 0xFFFFFF, "above rect()");
        px(r, 50, 85, 0xFFFFFF, "below rect()");
        BufferedImage a = RenderTestSupport.paint(page("clip-path:rect(auto auto 50px auto)"), 300, 200);
        px(a, 5, 5, 0xFF0000, "auto edges are the edges of the reference box");
        px(a, 95, 45, 0xFF0000, "auto right edge");
        px(a, 50, 60, 0xFFFFFF, "below the 50px bottom edge");
        BufferedImage x = RenderTestSupport.paint(page("clip-path:xywh(10px 20px 50px 30px)"), 300, 200);
        px(x, 30, 30, 0xFF0000, "inside xywh()");
        px(x, 5, 30, 0xFFFFFF, "left of xywh()");
        px(x, 65, 30, 0xFFFFFF, "right of xywh()");
        px(x, 30, 55, 0xFFFFFF, "below xywh()");
        BufferedImage p = RenderTestSupport.paint(page("clip-path:xywh(10% 10% 50% 50%)"), 300, 200);
        px(p, 30, 30, 0xFF0000, "percentages of the reference box");
        px(p, 80, 80, 0xFFFFFF, "outside the percentage box");
        BufferedImage rr = RenderTestSupport.paint(page("clip-path:rect(0 100px 100px 0 round 40px)"), 300, 200);
        px(rr, 1, 1, 0xFFFFFF, "the rounded corner of rect() is cut");
        px(rr, 50, 3, 0xFF0000, "the straight edge stays");
        px(RenderTestSupport.paint(page("clip-path:xywh(10px 20px 50px)"), 300, 200), 5, 5, 0xFF0000, "xywh() with three values is not valid and clips nothing");
    }

    /**
     * The radii of <code>round</code>: one to four values, a slash for the vertical radii.
     * <p>
     * <code>round</code> 의 반지름: 값 1~4 개, 슬래시 뒤는 세로 반지름.
     */
    public void testRadii() {
        BufferedImage one = RenderTestSupport.paint(page("clip-path:inset(0 round 40px 0 0 0)"), 300, 200);
        px(one, 1, 1, 0xFFFFFF, "the top-left corner is rounded");
        px(one, 98, 1, 0xFF0000, "the top-right corner is square");
        px(one, 1, 98, 0xFF0000, "the bottom-left corner is square");
        px(one, 98, 98, 0xFF0000, "the bottom-right corner is square");
        BufferedImage two = RenderTestSupport.paint(page("clip-path:inset(0 round 40px 0)"), 300, 200);
        px(two, 1, 1, 0xFFFFFF, "two values: the top-left corner is rounded");
        px(two, 98, 1, 0xFF0000, "two values: the top-right corner is square");
        px(two, 98, 98, 0xFFFFFF, "two values: the bottom-right corner is rounded");
        px(two, 1, 98, 0xFF0000, "two values: the bottom-left corner is square");
        BufferedImage four = RenderTestSupport.paint(page("clip-path:inset(0 round 0 0 40px 40px)"), 300, 200);
        px(four, 1, 1, 0xFF0000, "four values: the top-left is square");
        px(four, 98, 98, 0xFFFFFF, "four values: the bottom-right is rounded");
        px(four, 1, 98, 0xFFFFFF, "four values: the bottom-left is rounded");
        BufferedImage ell = RenderTestSupport.paint(page("clip-path:inset(0 round 50px / 20px)"), 300, 200);
        px(ell, 2, 2, 0xFFFFFF, "elliptical corner: cut");
        px(ell, 50, 2, 0xFF0000, "elliptical corner: the top edge between the corners");
        px(ell, 2, 25, 0xFF0000, "elliptical corner: inside the ellipse near the side");
        px(ell, 2, 5, 0xFFFFFF, "elliptical corner: outside the ellipse");
        px(RenderTestSupport.paint(page("clip-path:inset(0 round 1px 2px 3px 4px 5px)"), 300, 200), 1, 1, 0xFF0000, "five radii are not valid and clip nothing");
        // 반지름이 변보다 크면 함께 줄어든다(원이 된다): 100x100 상자에 200px 반지름은 원이다.
        BufferedImage big = RenderTestSupport.paint(page("clip-path:inset(0 round 200px)"), 300, 200);
        px(big, 50, 50, 0xFF0000, "the radii are scaled down to a circle: the center");
        px(big, 3, 3, 0xFFFFFF, "the radii are scaled down to a circle: the corner");
    }

    /**
     * A reference box alone clips to the edge of that box; fill-box, stroke-box and view-box are the content box and the border box.
     * <p>
     * 기준 상자만 있으면 그 상자의 가장자리로 자른다. fill-box, stroke-box, view-box 는 content 상자와 border 상자이다.
     */
    public void testBoxOnly() {
        String pad = "<div id=a style='width:60px;height:60px;padding:20px;background:#f00;clip-path:%s'></div>";
        BufferedImage c = RenderTestSupport.paint(RenderTestSupport.page(String.format(pad, "content-box"), 300, 200, null), 300, 200);
        px(c, 10, 10, 0xFFFFFF, "content-box alone: the padding is cut");
        px(c, 50, 50, 0xFF0000, "content-box alone: the content stays");
        BufferedImage f = RenderTestSupport.paint(RenderTestSupport.page(String.format(pad, "fill-box"), 300, 200, null), 300, 200);
        px(f, 10, 10, 0xFFFFFF, "fill-box is the content box");
        BufferedImage b = RenderTestSupport.paint(RenderTestSupport.page(String.format(pad, "border-box"), 300, 200, null), 300, 200);
        px(b, 10, 10, 0xFF0000, "border-box alone keeps everything");
        BufferedImage v = RenderTestSupport.paint(RenderTestSupport.page(String.format(pad, "inset(10px) view-box"), 300, 200, null), 300, 200);
        px(v, 5, 5, 0xFFFFFF, "view-box is the border box: the inset is measured from it");
        px(v, 15, 15, 0xFF0000, "inside the inset");
    }
}
