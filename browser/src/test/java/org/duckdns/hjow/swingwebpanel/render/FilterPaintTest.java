package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>filter</code> property (plan item P10-08, CSS Filter Effects Module Level 1): the parsing of the filter functions, the
 * color functions with the values that the matrices of the specification give, <code>blur()</code> and <code>drop-shadow()</code> (the
 * painted area reaches beyond the box, the blurred edge is half way between the two colors), the order of a chain, the stacking context
 * and the containing block that a filter creates, and that a filter does not change what a click finds.
 * <p>
 * <code>filter</code> 속성 시험(계획 P10-08, CSS Filter Effects Module Level 1): 필터 함수 파싱, 규격의 행렬이 주는 값을 가진 색 함수, <code>blur()</code> 와
 * <code>drop-shadow()</code>(그려지는 영역이 상자 밖으로 닿고 흐린 가장자리는 두 색의 중간이다), 사슬의 순서, 필터가 만드는 쌓임 맥락과 포함 블록, 필터가 클릭이 찾는
 * 것을 바꾸지 않는다는 점.
 */
public class FilterPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FilterPaintTest() {
    }

    /**
     * Paints a 100 by 100 red box with the style and returns the image of the page.
     * <p>
     * 스타일을 가진 100x100 빨간 상자를 그려 페이지 이미지를 반환한다.
     *
     * @param style the style of the box / 상자의 스타일
     * @return the image / 이미지
     */
    private static BufferedImage red(String style) {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:100px;height:100px;background:#f00;" + style + "'></div>", 300, 200, null);
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
     * The parsing of the filter functions: the defaults, the percentages, the clamping to 1 and the units of the angle.
     * <p>
     * 필터 함수의 파싱: 기본값, 백분율, 1 로 한정, 각도의 단위.
     */
    public void testParsing() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='color:#0a0;filter:blur(4px) brightness(150%) contrast() grayscale(3) hue-rotate(0.5turn) invert(25%) opacity(.5)"
                + " saturate(2) sepia(40%) drop-shadow(2px 3px)'>x</div>", 300, 200, null);
        List<FilterSpec.Op> ops = FilterSpec.of(RenderTestSupport.box(p, "a").getStyle()).getOps();
        Check.equal(10, ops.size(), "ten functions");
        Check.equal(FilterSpec.BLUR, ops.get(0).kind, "blur");
        Check.near(4, ops.get(0).amount, 1e-9, "blur 4px");
        Check.near(1.5, ops.get(1).amount, 1e-9, "brightness 150%");
        Check.near(1, ops.get(2).amount, 1e-9, "contrast() defaults to 1");
        Check.near(1, ops.get(3).amount, 1e-9, "grayscale(3) is limited to 1");
        Check.near(180, ops.get(4).amount, 1e-9, "hue-rotate(0.5turn) is 180 degrees");
        Check.near(0.25, ops.get(5).amount, 1e-9, "invert 25%");
        Check.near(0.5, ops.get(6).amount, 1e-9, "opacity .5");
        Check.near(2, ops.get(7).amount, 1e-9, "saturate may exceed 1");
        Check.near(0.4, ops.get(8).amount, 1e-9, "sepia 40%");
        FilterSpec.Op ds = ops.get(9);
        Check.equal(FilterSpec.DROP_SHADOW, ds.kind, "drop-shadow");
        Check.near(2, ds.dx, 1e-9, "dx");
        Check.near(3, ds.dy, 1e-9, "dy");
        Check.near(0, ds.deviation, 1e-9, "the deviation defaults to 0");
        Check.equal(0xFF00AA00, ds.color, "the color defaults to the color property");
    }

    /**
     * A filter that is not valid is dropped like any invalid declaration; a url() reference is not supported and the chain is ignored.
     * <p>
     * 올바르지 않은 필터는 다른 잘못된 선언처럼 버려진다. url() 참조는 지원하지 않으므로 사슬을 무시한다.
     */
    public void testInvalidAndUrl() {
        BufferedImage a = red("filter:foo(1)");
        pixel(0xFF0000, a, 50, 50, 0, "an unknown function is dropped");
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:10px;height:10px;filter:url(#f)'></div><div id=b style='filter:brightness(-1)'></div>"
                + "<div id=c style='filter:blur(2px) url(#f)'></div>", 300, 200, null);
        // url(#id) 는 SVG filter 요소의 참조로 목록에 들어간다(요소가 없으면 그리는 쪽에서 사슬 전체를 버린다).
        FilterSpec ref = FilterSpec.of(RenderTestSupport.box(p, "a").getStyle());
        Check.notNull(ref, "url(#f) is a reference");
        Check.equal(FilterSpec.REFERENCE, ref.getOps().get(0).kind, "the kind of the reference");
        Check.equal("f", ref.getOps().get(0).ref, "the id of the referenced filter");
        Check.isNull(FilterSpec.of(RenderTestSupport.box(p, "b").getStyle()), "a negative amount is invalid");
        FilterSpec c = FilterSpec.of(RenderTestSupport.box(p, "c").getStyle());
        Check.equal(2, c.getOps().size(), "a reference is a function of the chain");
        Check.equal(FilterSpec.REFERENCE, c.getOps().get(1).kind, "after the blur");
        Check.isNull(c.resolve(null, 10, 10), "a reference to an element that does not exist ignores the whole chain");
        // 다른 문서를 가리키는 url() 은 쓰지 않는다.
        RenderedPage other = RenderTestSupport.page("<div id=d style='filter:url(other.svg#f)'></div>", 300, 200, null);
        Check.isNull(FilterSpec.of(RenderTestSupport.box(other, "d").getStyle()), "a reference to another document is not used");
    }

    /**
     * The color functions give the values of the matrices of the specification.
     * <p>
     * 색 함수는 규격의 행렬이 주는 값을 낸다.
     */
    public void testColorFunctions() {
        pixel(0x363636, red("filter:grayscale(1)"), 50, 50, 1, "grayscale(1) of red is the luminance 0.2126");
        pixel(0xFF0000, red("filter:grayscale(0)"), 50, 50, 0, "grayscale(0) changes nothing");
        pixel(0xFF0000, red("filter:none"), 50, 50, 0, "none");
        pixel(0x00FFFF, red("filter:invert(1)"), 50, 50, 0, "invert(1) of red is cyan");
        pixel(0x800000, red("filter:brightness(0.5)"), 50, 50, 1, "brightness(.5)");
        pixel(0xFF0000, red("filter:brightness(2)"), 50, 50, 0, "brightness(2) is limited to 255");
        pixel(0x808080, red("filter:contrast(0)"), 50, 50, 1, "contrast(0) is middle gray");
        pixel(0x363636, red("filter:saturate(0)"), 50, 50, 2, "saturate(0) is gray");
        pixel(0x007100, red("filter:hue-rotate(120deg)"), 50, 50, 3, "hue-rotate(120deg) of red");
        pixel(0xFF0000, red("filter:hue-rotate(360deg)"), 50, 50, 2, "a full turn changes nothing");
        // 흰 배경 위의 흰 상자에 sepia(1)(행렬의 행 합이 1 을 넘는 채널은 한정된다).
        RenderedPage w = RenderTestSupport.page("<div style='width:100px;height:100px;background:#fff;filter:sepia(1)'></div>", 300, 200, null);
        BufferedImage s = RenderTestSupport.paint(w, 300, 200);
        pixel(0xFFFFEF, s, 50, 50, 1, "sepia(1) of white");
        pixel(0xFF8080, red("filter:opacity(0.5)"), 50, 50, 1, "opacity(.5) of red over white");
        pixel(0xFFFFFF, red("filter:opacity(0)"), 50, 50, 0, "opacity(0) hides the box");
    }

    /**
     * A chain is applied in order, each function on the output of the previous one.
     * <p>
     * 사슬은 순서대로 적용되며 각 함수는 앞 함수의 출력을 입력으로 받는다.
     */
    public void testChainOrder() {
        // invert 다음 grayscale: 빨강 -> 청록 -> 회색(휘도 0.7874)
        pixel(0xC9C9C9, red("filter:invert(1) grayscale(1)"), 50, 50, 2, "invert, then grayscale");
        // grayscale 다음 invert: 빨강 -> 어두운 회색 -> 밝은 회색(255 - 54)
        pixel(0xC9C9C9, red("filter:grayscale(1) invert(1)"), 50, 50, 2, "grayscale, then invert gives the same gray here");
        // brightness 와 contrast 는 순서에 따라 다르다: brightness(.5) 다음 contrast(2) 는 (128/255*2 - 0.5) 가 아닌 0.5*... 로 계산한다.
        BufferedImage a = RenderTestSupport.paint(RenderTestSupport.page(
                "<div style='width:100px;height:100px;background:#808080;filter:brightness(2) contrast(0.5)'></div>", 300, 200, null), 300, 200);
        BufferedImage b = RenderTestSupport.paint(RenderTestSupport.page(
                "<div style='width:100px;height:100px;background:#808080;filter:contrast(0.5) brightness(2)'></div>", 300, 200, null), 300, 200);
        // brightness(2): 0.502*2 = 1.004 -> 1 -> contrast(.5): 0.5 + 0.25 = 0.75 -> 191 ; contrast(.5): 0.502*0.5+0.25 = 0.501 -> brightness(2): 1.0 -> 255
        pixel(0xBFBFBF, a, 50, 50, 2, "brightness then contrast");
        pixel(0xFFFFFF, b, 50, 50, 2, "contrast then brightness");
    }

    /**
     * A blur: the middle of a large area stays, the edge is half way between the colors, the blur reaches beyond the box, and the
     * total amount of ink stays the same.
     * <p>
     * 흐림: 큰 영역의 가운데는 그대로이고 가장자리는 두 색의 중간이며 흐림은 상자 밖으로 닿고 잉크의 총량은 유지된다.
     */
    public void testBlur() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:50px;width:40px;height:40px;background:#000;filter:blur(5px)'></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        pixel(0x000000, img, 70, 70, 12, "the middle of the box stays black");
        int edge = RenderTestSupport.rgb(img, 50, 70) & 0xFF;
        Check.isTrue(edge > 105 && edge < 150, "the edge is about half way: " + edge);
        int outside = RenderTestSupport.rgb(img, 47, 70) & 0xFF;
        Check.isTrue(outside > 150 && outside < 205, "the blur reaches beyond the box: " + outside);
        int inside = RenderTestSupport.rgb(img, 53, 70) & 0xFF;
        Check.isTrue(inside < 105 && inside > 50, "and into it: " + inside);
        pixel(0xFFFFFF, img, 20, 70, 0, "far away is white");
        Check.isFalse("a".equals(idOf(p.hitTestDocument(47, 70))), "a click outside the box finds nothing: the filter has no effect on hit testing");
        Check.equal("a", idOf(p.hitTestDocument(70, 70)), "a click in the box finds it");
        // 잉크의 총량: 흐려도 (거의) 같다.
        double ink = 0;
        for (int y = 0; y < 200; y++) {
            // 모든 픽셀의 잉크(255 에서 파랑 채널을 뺀 값)를 더한다.
            for (int x = 0; x < 300; x++) {
                ink += 255 - (RenderTestSupport.rgb(img, x, y) & 0xFF);
            }
        }
        Check.near(40.0 * 40.0 * 255.0, ink, 40.0 * 40.0 * 255.0 * 0.03, "the amount of ink is kept by a blur");
        // 작은 편차(2 미만)는 참 가우스 합성곱이다: 가장자리 한 픽셀 바깥의 값은 약 31 %(1 - Φ(0.5/1.0))이다.
        RenderedPage q = RenderTestSupport.page("<div style='margin:50px;width:40px;height:40px;background:#000;filter:blur(1px)'></div>", 300, 200, null);
        BufferedImage iq = RenderTestSupport.paint(q, 300, 200);
        int e1 = RenderTestSupport.rgb(iq, 49, 70) & 0xFF;
        Check.isTrue(e1 > 150 && e1 < 200, "a small blur is a true Gaussian: " + e1);
        pixel(0x000000, iq, 70, 70, 1, "the middle stays");
        pixel(0xFFFFFF, iq, 40, 70, 0, "no reach far beyond");
        BufferedImage none = RenderTestSupport.paint(RenderTestSupport.page("<div style='margin:50px;width:40px;height:40px;background:#000;filter:blur(0)'></div>", 300, 200, null), 300, 200);
        pixel(0x000000, none, 50, 70, 0, "blur(0) keeps the hard edge");
        pixel(0xFFFFFF, none, 49, 70, 0, "blur(0) keeps the hard edge outside");
    }

    /**
     * A drop shadow: an offset copy of the alpha mask in the shadow color below the box, optionally blurred.
     * <p>
     * drop shadow: 상자 아래에 그림자 색으로 칠한 알파 마스크의 옮긴 복사본(흐리게 할 수 있다).
     */
    public void testDropShadow() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:20px;width:50px;height:50px;background:#f00;filter:drop-shadow(#00f 10px 10px 0)'></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        pixel(0xFF0000, img, 45, 45, 0, "the box is on top of its shadow");
        pixel(0x0000FF, img, 75, 75, 0, "the shadow shows below and to the right");
        pixel(0xFFFFFF, img, 25, 75, 0, "left of the shadow");
        pixel(0xFFFFFF, img, 85, 45, 0, "right of the shadow");
        Check.isFalse("a".equals(idOf(p.hitTestDocument(75, 75))), "the shadow is not hit");
        // 흐린 그림자: 가장자리가 부드럽고 상자 밖으로 번진다.
        BufferedImage b = RenderTestSupport.paint(RenderTestSupport.page(
                "<div style='margin:50px;width:40px;height:40px;background:#000;filter:drop-shadow(#000 20px 0 4px)'></div>", 300, 200, null), 300, 200);
        pixel(0x000000, b, 70, 70, 0, "the box");
        int edge = RenderTestSupport.rgb(b, 110, 70) & 0xFF;
        Check.isTrue(edge > 100 && edge < 160, "the blurred edge of the shadow (x=110, the box edge 90 plus 20): " + edge);
        int far = RenderTestSupport.rgb(b, 100, 70) & 0xFF;
        Check.isTrue(far < 60, "inside the shadow: " + far);
        pixel(0xFFFFFF, b, 140, 70, 0, "beyond the reach of the blur");
        // 색을 생략하면 color 속성의 색을 쓴다.
        BufferedImage c = RenderTestSupport.paint(RenderTestSupport.page(
                "<div style='margin:20px;width:50px;height:50px;background:#f00;color:#0f0;filter:drop-shadow(10px 10px)'></div>", 300, 200, null), 300, 200);
        pixel(0x00FF00, c, 75, 75, 0, "currentcolor");
        // 반투명한 상자의 그림자는 알파에 비례한다.
        BufferedImage d = RenderTestSupport.paint(RenderTestSupport.page(
                "<div style='margin:20px;width:50px;height:50px;background:rgba(255,0,0,0.5);filter:drop-shadow(#000 60px 0)'></div>", 300, 200, null), 300, 200);
        pixel(0x808080, d, 100, 40, 2, "a half transparent box casts a half transparent black shadow");
    }

    /**
     * A filter applies to the whole subtree of the box together: children, text and nested boxes.
     * <p>
     * 필터는 상자의 하위 트리 전체(자식, 텍스트, 중첩된 상자)에 함께 적용된다.
     */
    public void testSubtreeAndEdge() {
        RenderedPage p = RenderTestSupport.page("<div style='width:100px;height:100px;background:#00f;filter:invert(1)'><div style='width:50px;height:50px;background:#f00'></div></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        pixel(0x00FFFF, img, 25, 25, 0, "the child is inverted too (red is cyan)");
        pixel(0xFFFF00, img, 75, 75, 0, "the box is inverted (blue is yellow)");
        pixel(0xFFFFFF, img, 150, 75, 0, "outside nothing is painted");
    }

    /**
     * A filter creates a stacking context and the containing block of the absolutely positioned and fixed descendants, and does not move
     * or resize anything.
     * <p>
     * 필터는 쌓임 맥락과 절대 배치 및 fixed 자손의 포함 블록을 만들고 아무것도 옮기거나 크기를 바꾸지 않는다.
     */
    public void testStackingContextAndContainingBlock() {
        RenderedPage p = RenderTestSupport.page("<div id=p style='margin:50px;width:100px;height:100px;filter:grayscale(1)'>"
                + "<div id=c style='position:absolute;left:10px;top:10px;width:5px;height:5px'></div><div id=f style='position:fixed;left:20px;top:20px;width:5px;height:5px'></div></div>", 300, 400, null);
        LayoutBox parent = RenderTestSupport.box(p, "p");
        Check.isTrue(parent.isStackingContext(), "a filter makes a stacking context");
        Check.near(50, parent.getX(), 1e-9, "the box is not moved");
        Check.near(100, parent.getWidth(), 1e-9, "nor resized");
        LayoutBox c = RenderTestSupport.box(p, "c");
        Check.near(60, c.getX(), 1e-9, "an absolutely positioned child is placed relative to the box with the filter");
        Check.near(60, c.getY(), 1e-9, "(y)");
        LayoutBox f = RenderTestSupport.box(p, "f");
        Check.near(70, f.getX(), 1e-9, "a fixed child is placed relative to it too");
        Check.isFalse(f.isFixedPosition(), "and scrolls with the page");
    }

    /**
     * A filter together with opacity and a transform: the filter is applied first, then the opacity; the transform moves the result.
     * <p>
     * 필터와 불투명도, 변환을 함께 쓸 때: 필터를 먼저, 그다음 불투명도를 적용하고 변환이 결과를 옮긴다.
     */
    public void testWithOpacityAndTransform() {
        BufferedImage a = red("filter:grayscale(1);opacity:.5");
        pixel(0x9A9A9A, a, 50, 50, 2, "gray (54) at half opacity over white");
        BufferedImage b = red("filter:invert(1);transform:translate(150px,0)");
        pixel(0x00FFFF, b, 200, 50, 0, "the filtered box is painted where the transform moves it");
        pixel(0xFFFFFF, b, 50, 50, 0, "and not at its old place");
        BufferedImage c = red("filter:blur(3px);transform:scale(2)");
        int e = RenderTestSupport.rgb(c, 150, 100) & 0xFFFFFF;
        Check.isTrue(e != 0xFFFFFF && e != 0xFF0000, "a scaled and blurred box has a soft edge at the scaled position: " + Integer.toHexString(e));
        pixel(0xFF0000, c, 100, 50, 2, "the middle of the scaled box stays red");
    }

    /**
     * A filter on a hidden box paints nothing, and a filter that reaches beyond the box is not cut by the box of an ancestor that does
     * not clip.
     * <p>
     * 숨겨진 상자의 필터는 아무것도 그리지 않고, 상자 밖으로 닿는 필터는 자르지 않는 조상의 상자에 잘리지 않는다.
     */
    public void testHiddenAndOverflow() {
        BufferedImage a = red("visibility:hidden;filter:drop-shadow(#00f 10px 10px 0)");
        pixel(0xFFFFFF, a, 105, 105, 0, "a hidden box casts no shadow");
        RenderedPage p = RenderTestSupport.page("<div style='width:60px;height:60px;margin:40px'><div style='width:60px;height:60px;background:#000;filter:drop-shadow(#00f 30px 0)'></div></div>", 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        pixel(0x0000FF, img, 110, 60, 0, "the shadow is painted outside the parent's box");
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
}
