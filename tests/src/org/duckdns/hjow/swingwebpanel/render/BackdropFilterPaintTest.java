package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>backdrop-filter</code> (plan item P10-08, CSS Filter Effects Module Level 2): the filter functions change what has been painted behind the
 * box, only inside the border box (with its rounded corners), the content of the element is painted on top and is not filtered, the property makes a
 * stacking context and a containing block, a blur reads the mirrored edge of the box, and a page without the property is painted as before.
 * <p>
 * <code>backdrop-filter</code> 시험(계획 P10-08, CSS Filter Effects Module Level 2): 필터 함수는 상자 뒤에 이미 그려진 내용을 border box 안에서만(둥근 모서리 포함) 바꾸고,
 * 요소의 내용은 그 위에 필터 없이 그려지며, 속성은 쌓임 맥락과 포함 블록을 만들고, 흐림은 상자 가장자리를 거울로 이어 읽으며, 속성이 없는 페이지는 이전처럼 그려진다.
 */
public class BackdropFilterPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BackdropFilterPaintTest() {
    }

    /** The background of the tests: the left half is blue and the right half is red, 200 by 100 pixels. / 시험의 배경: 왼쪽 절반은 파랑, 오른쪽 절반은 빨강(200x100 화소). */
    private static final String HALVES = "<div style='position:absolute;left:0;top:0;width:100px;height:100px;background:#00f'></div>"
            + "<div style='position:absolute;left:100px;top:0;width:100px;height:100px;background:#f00'></div>";

    /**
     * Paints the two halves with an overlay on top and returns the page image.
     * <p>
     * 두 반쪽 위에 겹침 상자를 놓고 페이지 이미지를 반환한다.
     *
     * @param overlayStyle the style of the overlay (a 100 by 60 box at 50,20) / 겹침 상자의 스타일(50,20 에 있는 100x60 상자)
     * @param inner        the content of the overlay / 겹침 상자의 내용
     * @return the image / 이미지
     */
    private static BufferedImage over(String overlayStyle, String inner) {
        RenderedPage p = RenderTestSupport.page(HALVES + "<div id=o style='position:absolute;left:50px;top:20px;width:100px;height:60px;" + overlayStyle + "'>" + inner
                + "</div>", 300, 200, null);
        return RenderTestSupport.paint(p, 300, 200);
    }

    /**
     * Checks a pixel to a tolerance per channel.
     * <p>
     * 픽셀을 채널별 허용 오차로 확인한다.
     *
     * @param expected the expected RGB / 기대 RGB
     * @param img      the image / 이미지
     * @param x        x / x
     * @param y        y / y
     * @param tol      the tolerance / 허용 오차
     * @param what     the message / 메시지
     */
    private static void pixel(int expected, BufferedImage img, int x, int y, int tol, String what) {
        int got = RenderTestSupport.rgb(img, x, y);
        Check.isTrue(RenderTestSupport.near(expected, got, tol), what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(got));
    }

    /**
     * A page without the property is painted as before (the halves, the overlay without a backdrop filter changes nothing).
     * <p>
     * 속성이 없는 페이지는 이전처럼 그려진다(반쪽들, 배경 필터가 없는 겹침 상자는 아무것도 바꾸지 않는다).
     */
    public void testNoBackdropFilter() {
        BufferedImage img = over("", "");
        pixel(0x0000FF, img, 60, 30, 0, "the blue half under the overlay");
        pixel(0xFF0000, img, 140, 30, 0, "the red half under the overlay");
        pixel(0xFFFFFF, img, 250, 150, 0, "the page background");
    }

    /**
     * <code>invert(1)</code> turns the backdrop inside the box and nothing outside of it: blue becomes yellow and red becomes cyan.
     * <p>
     * <code>invert(1)</code> 은 상자 안의 배경만 뒤집는다: 파랑은 노랑이, 빨강은 청록이 된다.
     */
    public void testInvertInsideTheBox() {
        BufferedImage img = over("backdrop-filter:invert(1)", "");
        pixel(0xFFFF00, img, 60, 30, 0, "blue is inverted to yellow inside the box");
        pixel(0x00FFFF, img, 140, 30, 0, "red is inverted to cyan inside the box");
        pixel(0x0000FF, img, 40, 30, 0, "outside the box on the left nothing changes");
        pixel(0xFF0000, img, 160, 30, 0, "outside the box on the right nothing changes");
        pixel(0x0000FF, img, 60, 15, 0, "above the box nothing changes");
        pixel(0x0000FF, img, 60, 85, 0, "below the box nothing changes");
        pixel(0xFFFFFF, img, 250, 150, 0, "the page is not changed elsewhere");
    }

    /**
     * The element is painted on top of the filtered backdrop: its own background covers it, and its content is not filtered.
     * <p>
     * 요소는 필터링한 배경 위에 그려진다: 자신의 배경이 덮고, 내용은 필터링되지 않는다.
     */
    public void testElementContentIsPaintedOnTop() {
        BufferedImage img = over("backdrop-filter:grayscale(1)", "<div style='position:absolute;left:10px;top:10px;width:20px;height:20px;background:#0f0'></div>");
        pixel(0x0000FF, over("", ""), 60, 30, 0, "the blue half without the filter");
        pixel(0x121212, img, 55, 25, 2, "grayscale of blue is the luminance 0.0722 (0x12)");
        pixel(0x363636, img, 140, 25, 2, "grayscale of red is the luminance 0.2126 (0x36)");
        pixel(0x00FF00, img, 70, 40, 0, "the child is not filtered");
        BufferedImage translucent = over("backdrop-filter:invert(1);background:rgba(0,0,0,0.5)", "");
        pixel(0x7F7F00, translucent, 60, 30, 2, "the translucent background of the element is painted over the inverted backdrop");
    }

    /**
     * The rounded corners of the box limit the area: a pixel at the corner is outside of the shape and is not filtered.
     * <p>
     * 상자의 둥근 모서리가 영역을 제한한다: 모서리의 화소는 모양 밖이므로 필터링되지 않는다.
     */
    public void testRoundedCorners() {
        BufferedImage img = over("backdrop-filter:invert(1);border-radius:30px", "");
        pixel(0x0000FF, img, 51, 21, 0, "the corner pixel is outside of the rounded box");
        pixel(0xFFFF00, img, 70, 50, 0, "the middle of the box is inverted");
        pixel(0xFFFF00, img, 99, 21, 0, "the straight top edge is inverted");
    }

    /**
     * A blur mixes the two halves at their border and does not fade at the edges of the box (the edge is mirrored, so a uniform area stays uniform and
     * opaque).
     * <p>
     * 흐림은 두 반쪽의 경계에서 두 색을 섞고 상자 가장자리에서 옅어지지 않는다(가장자리를 거울로 이으므로 균일한 영역은 균일하고 불투명하게 남는다).
     */
    public void testBlurAndMirroredEdge() {
        BufferedImage img = over("backdrop-filter:blur(4px)", "");
        int mid = RenderTestSupport.rgb(img, 100, 50);
        int r = (mid >> 16) & 255;
        int b = mid & 255;
        Check.isTrue(r > 90 && r < 165 && b > 90 && b < 165, "the border between the halves is a mix: " + Integer.toHexString(mid));
        pixel(0x0000FF, img, 55, 25, 12, "the left edge of the box keeps the blue (the edge is mirrored, not faded)");
        pixel(0x0000FF, img, 70, 50, 3, "far from the border it is still blue");
        pixel(0xFF0000, img, 145, 75, 12, "the right edge of the box keeps the red");
        pixel(0x0000FF, img, 40, 50, 0, "outside the box nothing is blurred");
        pixel(0xFF0000, img, 160, 50, 0, "outside the box nothing is blurred (right)");
        pixel(0xFF0000, img, 100, 90, 0, "below the box the red half stays");
    }

    /**
     * A backdrop filter makes a stacking context and a containing block for fixed and absolute descendants, as <code>filter</code> does, and it does not
     * change the hit testing.
     * <p>
     * 배경 필터는 <code>filter</code> 처럼 쌓임 맥락과 fixed/absolute 자손의 포함 블록을 만들고 히트 테스트는 바꾸지 않는다.
     */
    public void testStackingContextAndContainingBlock() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='position:absolute;left:30px;top:40px;width:100px;height:100px;backdrop-filter:blur(2px)'>"
                + "<div id=f style='position:fixed;left:5px;top:6px;width:10px;height:10px'></div></div><div id=b style='width:10px;height:10px'></div>", 300, 200, null);
        LayoutBox a = RenderTestSupport.box(p, "a");
        Check.isTrue(a.isStackingContext(), "the box with backdrop-filter is a stacking context");
        LayoutBox f = RenderTestSupport.box(p, "f");
        Check.near(35, f.getX(), 0.01, "the fixed child is placed relative to the box (x)");
        Check.near(46, f.getY(), 0.01, "the fixed child is placed relative to the box (y)");
        Check.isNull(FilterSpec.of(a.getStyle()), "the filter property itself is none");
        Check.notNull(FilterSpec.ofBackdrop(a.getStyle()), "the backdrop filter is parsed");
        Check.isTrue(FilterSpec.hasBackdrop(a.getStyle()), "hasBackdrop");
        Check.isFalse(FilterSpec.hasBackdrop(RenderTestSupport.box(p, "b").getStyle()), "a box without it");
    }

    /**
     * Invalid values and <code>none</code> are dropped; <code>url()</code> is not supported and ignores the chain; the prefixed name is an alias.
     * <p>
     * 올바르지 않은 값과 <code>none</code> 은 버려진다. <code>url()</code> 은 지원하지 않아 사슬을 무시하며 접두사 이름은 별칭이다.
     */
    public void testParsingAndAlias() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='backdrop-filter:foo(1)'></div><div id=b style='backdrop-filter:none'></div>"
                + "<div id=c style='backdrop-filter:url(#f)'></div><div id=d style='-webkit-backdrop-filter:blur(3px) brightness(50%)'></div>", 300, 200, null);
        Check.isFalse(FilterSpec.hasBackdrop(RenderTestSupport.box(p, "a").getStyle()), "an unknown function is dropped");
        Check.isFalse(FilterSpec.hasBackdrop(RenderTestSupport.box(p, "b").getStyle()), "none");
        Check.isFalse(FilterSpec.hasBackdrop(RenderTestSupport.box(p, "c").getStyle()), "url() is ignored");
        Check.equal(2, FilterSpec.ofBackdrop(RenderTestSupport.box(p, "d").getStyle()).getOps().size(), "the prefixed alias");
    }

    /**
     * The page that is painted into a part of the component (a clip) and with a scroll offset gives the same pixels for the box as the whole page.
     * <p>
     * 컴포넌트의 일부(클립)와 스크롤 오프셋으로 그려도 상자의 화소가 전체 페이지와 같다.
     */
    public void testPaintWithClipAndScroll() {
        RenderedPage p = RenderTestSupport.page(HALVES + "<div style='position:absolute;left:50px;top:20px;width:100px;height:60px;backdrop-filter:invert(1)'></div>",
                300, 200, null);
        BufferedImage img = RenderTestSupport.blank(120, 70);
        Graphics2D g = img.createGraphics();
        try {
            p.paint(g, new Rectangle(0, 0, 120, 70), 30, 10);
        } finally {
            g.dispose();
        }
        pixel(0xFFFF00, img, 40, 20, 0, "the document pixel (70,30) scrolled by (30,10) is inverted blue");
        pixel(0x0000FF, img, 10, 20, 0, "the document pixel (40,30) is outside the box");
        pixel(0x00FFFF, img, 100, 20, 0, "the document pixel (130,30) is inverted red");
    }
}
