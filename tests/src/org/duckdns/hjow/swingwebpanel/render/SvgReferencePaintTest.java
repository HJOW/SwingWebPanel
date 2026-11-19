package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the references from CSS to SVG elements of the same document (plan item P10-08-d): <code>filter: url(#id)</code> (CSS Filter Effects Level 1
 * section 5), <code>clip-path: url(#id)</code> (CSS Masking Level 1 section 5.1) and <code>mask-image: url(#id)</code> (CSS Masking Level 1 section 7.1). The
 * elements are inline <code>svg</code> definitions in the page; the pixels show that the filter primitives, the clip path (in both units) and the mask are
 * applied to an HTML box with its border box as the bounding box and the origin of the user space, that a reference can be mixed with the filter functions,
 * and that a reference to nothing leaves the box as it is.
 * <p>
 * 같은 문서의 SVG 요소를 가리키는 CSS 참조의 시험(계획 P10-08-d): <code>filter: url(#id)</code>(CSS Filter Effects Level 1 5 절), <code>clip-path: url(#id)</code>(CSS Masking Level 1
 * 5.1 절), <code>mask-image: url(#id)</code>(CSS Masking Level 1 7.1 절). 요소는 페이지의 인라인 <code>svg</code> 정의이다. 픽셀로 필터 기본 요소, 클립 경로(두 단위 모두), 마스크가
 * border box 를 경계 상자와 사용자 공간의 원점으로 하여 HTML 상자에 적용되는지, 참조를 필터 함수와 섞을 수 있는지, 아무것도 가리키지 않는 참조는 상자를 그대로 두는지 확인한다.
 */
public class SvgReferencePaintTest {

    /** The definitions: a filter that moves the red to green, an offset, a blur, and the clip paths and masks. 정의: 빨강을 초록으로 옮기는 필터, 오프셋, 흐림, 클립 경로와 마스크. */
    private static final String DEFS = "<svg width='0' height='0' style='position:absolute'><defs>"
            + "<filter id='swap'><feColorMatrix type='matrix' values='0 0 0 0 0  1 0 0 0 0  0 0 0 0 0  0 0 0 1 0'/></filter>"
            + "<filter id='shift' x='0' y='0' width='1.2' height='1'><feOffset dx='20' dy='0'/></filter>"
            + "<filter id='blur'><feGaussianBlur stdDeviation='4'/></filter>"
            + "<filter id='user' filterUnits='userSpaceOnUse' x='0' y='0' width='30' height='30'><feOffset dx='0' dy='0'/></filter>"
            + "<clipPath id='left'><rect x='0' y='0' width='50' height='100'/></clipPath>"
            + "<clipPath id='circle' clipPathUnits='objectBoundingBox'><circle cx='0.5' cy='0.5' r='0.5'/></clipPath>"
            + "<mask id='half'><rect x='0' y='0' width='50' height='100' fill='white'/></mask>"
            + "<mask id='bbox' maskContentUnits='objectBoundingBox'><rect x='0' y='0' width='1' height='0.5' fill='white'/></mask>"
            + "</defs></svg>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SvgReferencePaintTest() {
    }

    /**
     * Paints a red 100 by 100 box with a style, below the definitions, at the position (20, 20).
     * <p>
     * 스타일이 있는 100x100 빨간 상자를 (20, 20) 에 정의 아래에 그린다.
     *
     * @param style the style of the box / 상자의 스타일
     * @return the image / 이미지
     */
    private static BufferedImage red(String style) {
        RenderedPage p = RenderTestSupport.page(DEFS + "<div id=a style='margin:20px;width:100px;height:100px;background:#f00;" + style + "'></div>", 300, 200, null);
        return RenderTestSupport.paint(p, 300, 200);
    }

    /**
     * Checks a pixel.
     * <p>
     * 픽셀을 확인한다.
     *
     * @param expected the RGB / 기대 RGB
     * @param img      the image / 이미지
     * @param x        x / x
     * @param y        y / y
     * @param tol      the tolerance per channel / 채널별 허용 오차
     * @param what     the description / 설명
     */
    private static void pixel(int expected, BufferedImage img, int x, int y, int tol, String what) {
        int got = RenderTestSupport.rgb(img, x, y);
        Check.isTrue(RenderTestSupport.near(expected, got, tol), what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(got));
    }

    /**
     * A filter element: <code>feColorMatrix</code> turns the red box green.
     * <p>
     * 필터 요소: <code>feColorMatrix</code> 가 빨간 상자를 초록으로 바꾼다.
     */
    public void testFilterElementIsApplied() {
        pixel(0x00FF00, red("filter:url(#swap)"), 70, 70, 2, "the red box became green");
        pixel(0xFFFFFF, red("filter:url(#swap)"), 10, 70, 0, "outside the box stays white");
    }

    /**
     * The border box is the bounding box: a filter region of 120% of the width lets the offset content show 10 px beyond the right edge, and the
     * offset moves the content by 20 px to the right.
     * <p>
     * border box 가 경계 상자이다: 너비의 120% 인 필터 영역 덕분에 오프셋한 내용이 오른쪽 가장자리 밖 20px 까지 보이고, 오프셋은 내용을 오른쪽으로 20px 옮긴다.
     */
    public void testOffsetAndTheFilterRegion() {
        BufferedImage img = red("filter:url(#shift)");
        // 상자는 x 20..120 이고 필터 영역은 x 20..140 이다. 오프셋 20 으로 내용은 40..140 에 있다.
        pixel(0xFFFFFF, img, 30, 70, 0, "the left 20 px of the box are empty after the offset");
        pixel(0xFF0000, img, 50, 70, 0, "the content moved to x 40");
        pixel(0xFF0000, img, 130, 70, 0, "and shows in the part of the region beyond the box");
        pixel(0xFFFFFF, img, 145, 70, 0, "but not beyond the region");
    }

    /**
     * <code>filterUnits="userSpaceOnUse"</code>: the region is in px with the top left corner of the border box as the origin.
     * <p>
     * <code>filterUnits="userSpaceOnUse"</code>: 영역은 border box 의 왼쪽 위 모서리를 원점으로 하는 px 이다.
     */
    public void testUserSpaceRegion() {
        BufferedImage img = red("filter:url(#user)");
        pixel(0xFF0000, img, 30, 30, 0, "inside the region of 30 by 30 at the corner of the box");
        pixel(0xFFFFFF, img, 70, 70, 0, "everything outside the region is removed");
    }

    /**
     * A blur reaches beyond the box up to the default filter region (10 percent of the box on every side).
     * <p>
     * 흐림은 기본 필터 영역(상자의 사방 10%)까지 상자 밖으로 닿는다.
     */
    public void testBlurReachesBeyondTheBox() {
        BufferedImage img = red("filter:url(#blur)");
        int g = RenderTestSupport.rgb(img, 17, 70) & 0xFF;
        Check.isTrue(g < 250 && g > 100, "the blur is seen outside of the box, in the region: " + g);
        pixel(0xFFFFFF, img, 5, 70, 0, "but not beyond the region (10 px)");
        pixel(0xFF0000, img, 70, 70, 6, "the middle stays red");
    }

    /**
     * A reference can be mixed with the filter functions, in order: the function runs on the output of the reference.
     * <p>
     * 참조는 필터 함수와 섞을 수 있고 순서대로 적용된다: 함수는 참조의 출력을 입력으로 받는다.
     */
    public void testReferenceInAChain() {
        pixel(0x000000, red("filter:url(#swap) brightness(0)"), 70, 70, 1, "the green box became black");
        pixel(0x00FF00, red("filter:brightness(1) url(#swap)"), 70, 70, 2, "the function first, then the reference");
        pixel(0xFF00FF, red("filter:url(#swap) invert(1)"), 70, 70, 1, "invert(1) of the green is magenta");
    }

    /**
     * A reference to nothing, to an element of another kind or to another document leaves the box as it is; the box still is a stacking context.
     * <p>
     * 아무것도 가리키지 않거나 다른 종류의 요소를 가리키거나 다른 문서를 가리키는 참조는 상자를 그대로 둔다. 상자는 여전히 쌓임 맥락이다.
     */
    public void testBrokenReferencesChangeNothing() {
        pixel(0xFF0000, red("filter:url(#nothing)"), 70, 70, 0, "no such element");
        pixel(0xFF0000, red("filter:url(#left)"), 70, 70, 0, "a clipPath is no filter");
        pixel(0xFF0000, red("filter:url(#swap) url(#nothing)"), 70, 70, 0, "one broken reference drops the whole chain");
        pixel(0xFF0000, red("clip-path:url(#nothing)"), 100, 70, 0, "a clip path that does not exist clips nothing");
        pixel(0xFF0000, red("clip-path:url(#swap)"), 100, 70, 0, "a filter is no clip path");
        RenderedPage p = RenderTestSupport.page(DEFS + "<div id=a style='filter:url(#nothing)'></div>", 300, 200, null);
        Check.isTrue(RenderTestSupport.box(p, "a").isStackingContext(), "a filter that is not none makes a stacking context");
        Check.notNull(FilterSpec.of(RenderTestSupport.box(p, "a").getStyle()), "the reference is kept in the value");
    }

    /**
     * <code>clip-path: url(#id)</code> with a <code>clipPath</code> in user space units: the left half of the box is kept.
     * <p>
     * 사용자 공간 단위의 <code>clipPath</code> 를 쓰는 <code>clip-path: url(#id)</code>: 상자의 왼쪽 절반이 남는다.
     */
    public void testClipPathInUserSpace() {
        BufferedImage img = red("clip-path:url(#left)");
        pixel(0xFF0000, img, 40, 70, 0, "the left half is kept");
        pixel(0xFFFFFF, img, 100, 70, 0, "the right half is clipped");
        pixel(0xFFFFFF, img, 60, 150, 0, "below the box nothing");
    }

    /**
     * <code>clipPathUnits="objectBoundingBox"</code>: a circle of the radius 0.5 in the unit square of the box.
     * <p>
     * <code>clipPathUnits="objectBoundingBox"</code>: 상자의 단위 정사각형 안의 반지름 0.5 인 원.
     */
    public void testClipPathInBoundingBoxUnits() {
        BufferedImage img = red("clip-path:url(#circle)");
        pixel(0xFF0000, img, 70, 70, 0, "the center of the circle");
        pixel(0xFFFFFF, img, 22, 22, 0, "the corner of the box is outside of the circle");
        pixel(0xFF0000, img, 70, 24, 6, "the middle of the top edge is inside");
    }

    /**
     * <code>mask-image: url(#id)</code> with a <code>mask</code> element: where the mask is white the box is seen, elsewhere not.
     * <p>
     * <code>mask</code> 요소를 쓰는 <code>mask-image: url(#id)</code>: 마스크가 흰 곳에서는 상자가 보이고 다른 곳에서는 보이지 않는다.
     */
    public void testMaskElement() {
        BufferedImage img = red("mask-image:url(#half)");
        pixel(0xFF0000, img, 40, 70, 0, "the left half is under the white rect of the mask");
        pixel(0xFFFFFF, img, 100, 70, 0, "the right half is not under anything");
        BufferedImage bbox = red("mask-image:url(#bbox)");
        pixel(0xFF0000, bbox, 70, 40, 0, "maskContentUnits objectBoundingBox: the upper half is kept");
        pixel(0xFFFFFF, bbox, 70, 100, 0, "and the lower half is masked");
    }

    /**
     * A mask that names nothing is a transparent black layer: the box is not painted.
     * <p>
     * 아무것도 가리키지 않는 마스크는 투명한 검정 레이어이다: 상자가 그려지지 않는다.
     */
    public void testMissingMaskHidesTheBox() {
        pixel(0xFFFFFF, red("mask-image:url(#nothing)"), 70, 70, 0, "a reference to nothing masks everything");
    }

    /**
     * The three references can be used together, in the order of the compositing model: the filter, the clip path, the mask.
     * <p>
     * 세 참조를 함께 쓸 수 있으며 합성 모델의 순서(필터, 클립 경로, 마스크)를 따른다.
     */
    public void testAllThree() {
        BufferedImage img = red("filter:url(#swap);clip-path:url(#left);mask-image:url(#bbox)");
        pixel(0x00FF00, img, 40, 40, 2, "green, in the left half, in the upper half");
        pixel(0xFFFFFF, img, 100, 40, 0, "clipped on the right");
        pixel(0xFFFFFF, img, 40, 100, 0, "masked below");
    }

    /**
     * The elements of the references are read from the DOM: definitions in an <code>svg</code> that is not displayed work too, and the box that refers
     * keeps its layout.
     * <p>
     * 참조의 요소는 DOM 에서 읽는다: 표시되지 않는 <code>svg</code> 안의 정의도 쓸 수 있고, 참조하는 상자의 배치는 바뀌지 않는다.
     */
    public void testHiddenDefinitions() {
        RenderedPage p = RenderTestSupport.page("<svg style='display:none'><filter id='swap'><feColorMatrix type='matrix' values='0 0 0 0 0  1 0 0 0 0  0 0 0 0 0  0 0 0 1 0'/></filter></svg>"
                + "<div id=a style='margin:20px;width:100px;height:100px;background:#f00;filter:url(#swap)'></div>", 300, 200, null);
        pixel(0x00FF00, RenderTestSupport.paint(p, 300, 200), 70, 70, 2, "a filter in a display:none svg");
        Check.near(100, RenderTestSupport.box(p, "a").getWidth(), 0.01, "the box keeps its size");
    }
}
