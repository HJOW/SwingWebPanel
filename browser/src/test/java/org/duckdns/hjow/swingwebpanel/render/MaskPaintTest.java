package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the mask properties (plan item P10-08, CSS Masking Level 1 section 7): <code>mask-image</code> with gradients and with
 * images, <code>mask-mode</code> (alpha and luminance), <code>mask-size</code>, <code>mask-position</code>, <code>mask-repeat</code>,
 * <code>mask-origin</code>, <code>mask-clip</code>, the compositing of several layers with <code>mask-composite</code>, the
 * <code>mask</code> shorthand, a layer that is not available, and that a mask does not change what a click finds.
 * <p>
 * 마스크 속성 시험(계획 P10-08, CSS Masking Level 1 7 절): 그라디언트와 이미지를 쓴 <code>mask-image</code>, <code>mask-mode</code>(alpha, luminance),
 * <code>mask-size</code>, <code>mask-position</code>, <code>mask-repeat</code>, <code>mask-origin</code>, <code>mask-clip</code>, <code>mask-composite</code> 로 여러 레이어를 합성하는
 * 것, <code>mask</code> 단축 속성, 쓸 수 없는 레이어, 마스크가 클릭이 찾는 것을 바꾸지 않는다는 점.
 */
public class MaskPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MaskPaintTest() {
    }

    /**
     * Returns the image of a page with a 100 by 100 red box with a style.
     * <p>
     * 스타일을 가진 100x100 빨간 상자가 있는 페이지의 이미지를 반환한다.
     *
     * @param style the style of the box / 상자의 스타일
     * @return the image / 이미지
     */
    private static BufferedImage red(String style) {
        return RenderTestSupport.paint(RenderTestSupport.page("<div id=a style='width:100px;height:100px;background:#f00;" + style + "'></div>", 300, 200, null), 300, 200);
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
     * A gradient alpha mask makes the box fade out: the mask value is the alpha of the gradient.
     * <p>
     * 그라디언트 알파 마스크는 상자를 서서히 사라지게 한다: 마스크 값은 그라디언트의 알파이다.
     */
    public void testGradientAlphaMask() {
        BufferedImage img = red("mask-image:linear-gradient(to right,#000,transparent)");
        // 이 마스크는 x 에서 알파 1 - x/100 이다. 흰 배경 위의 빨강: G = 255 * (1 - 알파)
        pixel(0xFF0D0D, img, 5, 50, 6, "near the left edge the box is almost opaque");
        pixel(0xFF8080, img, 50, 50, 8, "in the middle it is half transparent");
        pixel(0xFFF2F2, img, 95, 50, 6, "near the right edge it is almost transparent");
        pixel(0xFFFFFF, img, 150, 50, 0, "outside nothing is painted");
        BufferedImage none = red("mask-image:none");
        pixel(0xFF0000, none, 50, 50, 0, "mask-image: none does not mask");
    }

    /**
     * <code>mask-mode</code>: alpha uses the alpha of the layer, luminance its brightness.
     * <p>
     * <code>mask-mode</code>: alpha 는 레이어의 알파를, luminance 는 밝기를 쓴다.
     */
    public void testMaskMode() {
        BufferedImage alpha = red("mask-image:linear-gradient(to right,#fff,#000)");
        pixel(0xFF0000, alpha, 50, 50, 0, "an opaque gradient is an opaque alpha mask");
        BufferedImage lum = red("mask-image:linear-gradient(to right,#fff,#000);mask-mode:luminance");
        pixel(0xFF0D0D, lum, 5, 50, 8, "white is fully visible");
        pixel(0xFF8080, lum, 50, 50, 12, "middle gray is half visible");
        pixel(0xFFF2F2, lum, 95, 50, 8, "black is hidden");
        BufferedImage mid = red("mask-image:linear-gradient(#808080,#808080);mask-mode:luminance");
        // 휘도 0.502 가 마스크 값: 흰 배경 위에서 G = 255 * (1 - 0.502) = 127
        pixel(0xFF7F7F, mid, 50, 50, 3, "luminance of #808080 is .5");
    }

    /**
     * The size, the repeat style and the position of a layer: the part of the box that is not covered by a tile is hidden.
     * <p>
     * 레이어의 크기, 반복 방식, 위치: 타일이 덮지 않는 상자 부분은 가려진다.
     */
    public void testSizeRepeatPosition() {
        BufferedImage img = red("mask-image:linear-gradient(#000,#000);mask-size:50px 50px;mask-repeat:no-repeat");
        pixel(0xFF0000, img, 25, 25, 0, "inside the tile");
        pixel(0xFFFFFF, img, 75, 25, 0, "right of the tile");
        pixel(0xFFFFFF, img, 75, 75, 0, "below and right of the tile");
        BufferedImage rep = red("mask-image:linear-gradient(#000,#000);mask-size:20px 20px;mask-repeat:repeat-x");
        pixel(0xFF0000, rep, 90, 10, 0, "the tiles repeat along x");
        pixel(0xFFFFFF, rep, 90, 30, 0, "but not along y");
        BufferedImage pos = red("mask-image:linear-gradient(#000,#000);mask-size:40px 40px;mask-repeat:no-repeat;mask-position:right 10px bottom 10px");
        pixel(0xFF0000, pos, 70, 70, 0, "the tile is placed at the bottom right with offsets");
        pixel(0xFFFFFF, pos, 20, 20, 0, "the rest is hidden");
        BufferedImage center = red("mask-image:linear-gradient(#000,#000);mask-size:40px 40px;mask-repeat:no-repeat;mask-position:center");
        pixel(0xFF0000, center, 50, 50, 0, "centered");
        pixel(0xFFFFFF, center, 20, 20, 0, "centered: corner hidden");
        // space: 타일을 정수 개 놓고 남는 공간을 타일 사이에 나눈다.
        BufferedImage space = red("mask-image:linear-gradient(#000,#000);mask-size:30px 100px;mask-repeat:space no-repeat");
        pixel(0xFF0000, space, 15, 50, 0, "first tile");
        pixel(0xFFFFFF, space, 32, 50, 0, "gap between the first and the second tile (30..35)");
        pixel(0xFF0000, space, 50, 50, 0, "the second tile (35..65)");
        pixel(0xFF0000, space, 85, 50, 0, "last tile touches the right edge");
    }

    /**
     * <code>mask-origin</code> and <code>mask-clip</code>: the origin positions the layer, the clip restricts where the mask applies;
     * both default to the border box.
     * <p>
     * <code>mask-origin</code> 과 <code>mask-clip</code>: origin 은 레이어의 기준 위치, clip 은 마스크가 적용되는 곳을 제한한다. 둘 다 기본값은 border box 이다.
     */
    public void testOriginAndClip() {
        BufferedImage clip = red("padding:20px;width:60px;height:60px;mask-image:linear-gradient(#000,#000);mask-clip:content-box");
        pixel(0xFFFFFF, clip, 10, 10, 0, "the padding area is outside of the mask painting area");
        pixel(0xFF0000, clip, 50, 50, 0, "the content box is masked in");
        BufferedImage pad = red("padding:20px;width:60px;height:60px;mask-image:linear-gradient(#000,#000);mask-clip:padding-box;border:10px solid #00f");
        pixel(0xFFFFFF, pad, 5, 5, 0, "the border is outside of the padding box");
        pixel(0xFF0000, pad, 50, 50, 0, "the padding box is masked in");
        // origin: padding-box 이면 배경 크기 100% 가 padding box 를 덮는다.
        BufferedImage org = red("padding:20px;width:60px;height:60px;mask-image:linear-gradient(to right,#000,transparent);mask-origin:content-box;mask-clip:border-box;mask-repeat:no-repeat");
        pixel(0xFFFFFF, org, 10, 50, 0, "the gradient starts at the content box: left of it nothing is masked in");
        pixel(0xFF8080, org, 50, 50, 14, "in the middle of the content box the gradient is half transparent");
        BufferedImage noclip = red("mask-image:linear-gradient(#000,#000);mask-size:50px 50px;mask-repeat:no-repeat;mask-clip:no-clip");
        pixel(0xFF0000, noclip, 25, 25, 0, "no-clip: the tile still masks in");
    }

    /**
     * Several layers are composited from the bottom up with the operator of each layer.
     * <p>
     * 여러 레이어는 아래부터 레이어마다의 연산자로 합성된다.
     */
    public void testComposite() {
        String two = "mask-image:linear-gradient(#000,#000),linear-gradient(#000,#000);mask-size:100px 50px,50px 100px;mask-repeat:no-repeat;mask-position:0 0,0 0;";
        BufferedImage add = red(two + "mask-composite:add");
        pixel(0xFF0000, add, 75, 25, 0, "add: the top layer only");
        pixel(0xFF0000, add, 25, 75, 0, "add: the bottom layer only");
        pixel(0xFF0000, add, 25, 25, 0, "add: both");
        pixel(0xFFFFFF, add, 75, 75, 0, "add: neither");
        BufferedImage inter = red(two + "mask-composite:intersect");
        pixel(0xFF0000, inter, 25, 25, 0, "intersect: both");
        pixel(0xFFFFFF, inter, 75, 25, 0, "intersect: the top layer only");
        pixel(0xFFFFFF, inter, 25, 75, 0, "intersect: the bottom layer only");
        BufferedImage sub = red(two + "mask-composite:subtract");
        pixel(0xFF0000, sub, 75, 25, 0, "subtract: where the top layer is outside of the bottom layer");
        pixel(0xFFFFFF, sub, 25, 25, 0, "subtract: where both are");
        pixel(0xFFFFFF, sub, 25, 75, 0, "subtract: the bottom layer only");
        BufferedImage ex = red(two + "mask-composite:exclude");
        pixel(0xFF0000, ex, 75, 25, 0, "exclude: the top layer only");
        pixel(0xFF0000, ex, 25, 75, 0, "exclude: the bottom layer only");
        pixel(0xFFFFFF, ex, 25, 25, 0, "exclude: both");
        // 맨 아래 레이어의 연산자는 무시한다: 한 레이어에 intersect 를 줘도 그대로이다.
        BufferedImage one = red("mask-image:linear-gradient(#000,#000);mask-size:50px 50px;mask-repeat:no-repeat;mask-composite:intersect");
        pixel(0xFF0000, one, 25, 25, 0, "the operator of the bottom layer is ignored");
        // none 레이어는 투명한 검정이다: intersect 로 합성하면 모두 가려진다.
        BufferedImage nn = red("mask-image:linear-gradient(#000,#000),none;mask-composite:intersect");
        pixel(0xFFFFFF, nn, 50, 50, 0, "a none layer is transparent black: intersect hides everything");
        BufferedImage na = red("mask-image:linear-gradient(#000,#000),none;mask-composite:add");
        pixel(0xFF0000, na, 50, 50, 0, "add with a none layer keeps the other layer");
    }

    /**
     * The <code>mask</code> shorthand sets all mask longhands: the layer parts in any order, one or two boxes, no-clip and the reset of
     * the parts that are not written.
     * <p>
     * <code>mask</code> 단축 속성은 모든 마스크 개별 속성을 정한다: 순서 없는 층의 부분, 상자 하나나 둘, no-clip, 쓰지 않은 부분의 초기화.
     */
    public void testShorthand() {
        BufferedImage img = red("mask:linear-gradient(#000,#000) 0 0 / 50px 50px no-repeat");
        pixel(0xFF0000, img, 25, 25, 0, "the tile");
        pixel(0xFFFFFF, img, 75, 75, 0, "outside the tile");
        RenderedPage p = RenderTestSupport.page("<div id=a style='mask:url(a.png) 10px 20px / 30px 40px repeat-x padding-box content-box exclude luminance, linear-gradient(#000,#000) no-clip'></div>",
                300, 200, new RenderTestSupport.FixedImages());
        ComputedStyle s = RenderTestSupport.box(p, "a").getStyle();
        Check.contains(s.get(Prop.MASK_POSITION).toCssText(), "10px", "position of the first layer");
        Check.contains(s.get(Prop.MASK_SIZE).toCssText(), "30px", "size of the first layer");
        Check.contains(s.get(Prop.MASK_REPEAT).toCssText(), "repeat-x", "repeat of the first layer");
        Check.contains(s.get(Prop.MASK_ORIGIN).toCssText(), "padding-box", "origin of the first layer");
        Check.contains(s.get(Prop.MASK_CLIP).toCssText(), "content-box", "clip of the first layer");
        Check.contains(s.get(Prop.MASK_COMPOSITE).toCssText(), "exclude", "composite of the first layer");
        Check.contains(s.get(Prop.MASK_MODE).toCssText(), "luminance", "mode of the first layer");
        Check.contains(s.get(Prop.MASK_CLIP).toCssText(), "no-clip", "no-clip of the second layer");
        // 단축 속성은 쓰지 않은 부분을 초기값으로 되돌린다.
        RenderedPage q = RenderTestSupport.page("<div id=a style='mask-size:10px;mask-repeat:no-repeat;mask:linear-gradient(#000,#000)'></div>", 300, 200, null);
        ComputedStyle t = RenderTestSupport.box(q, "a").getStyle();
        Check.equal("repeat", t.get(Prop.MASK_REPEAT).toCssText(), "mask resets the repeat style");
        // 올바르지 않은 값은 선언 전체를 버린다.
        RenderedPage bad = RenderTestSupport.page("<div id=a style='mask-size:10px;mask:linear-gradient(#000,#000) nonsense'></div>", 300, 200, null);
        Check.equal("10px", RenderTestSupport.box(bad, "a").getStyle().get(Prop.MASK_SIZE).toCssText(), "an invalid mask is dropped");
    }

    /**
     * An image mask: a loaded image is used as the mask, a layer that is not available is transparent black, so the box is not
     * painted.
     * <p>
     * 이미지 마스크: 읽은 이미지를 마스크로 쓰고, 쓸 수 없는 레이어는 투명한 검정이므로 상자는 그려지지 않는다.
     */
    public void testImageMask() {
        int[] px = new int[4 * 4];
        // 4x4 이미지: 왼쪽 절반은 불투명한 검정, 오른쪽 절반은 투명하다.
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                px[y * 4 + x] = x < 2 ? 0xFF000000 : 0x00000000;
            }
        }
        org.duckdns.hjow.swingwebpanel.media.image.DecodedImage di = new org.duckdns.hjow.swingwebpanel.media.image.DecodedImage(4, 4, px);
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/dir/m.png", ImageResource.loaded(di))
                .put("http://t.test/dir/bad.png", ImageResource.failed("404")).put("http://t.test/dir/wait.png", ImageResource.PENDING);
        RenderedPage p = RenderTestSupport.page("<div style='width:100px;height:100px;background:#f00;mask-image:url(m.png);mask-size:100px 100px;mask-repeat:no-repeat'></div>", 300, 200, src);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        pixel(0xFF0000, img, 25, 50, 0, "the opaque half of the mask image shows the box");
        pixel(0xFFFFFF, img, 75, 50, 0, "the transparent half hides it");
        RenderedPage f = RenderTestSupport.page("<div style='width:100px;height:100px;background:#f00;mask-image:url(bad.png)'></div>", 300, 200, src);
        pixel(0xFFFFFF, RenderTestSupport.paint(f, 300, 200), 50, 50, 0, "a mask image that failed is transparent black");
        RenderedPage w = RenderTestSupport.page("<div style='width:100px;height:100px;background:#f00;mask-image:url(wait.png)'></div>", 300, 200, src);
        pixel(0xFFFFFF, RenderTestSupport.paint(w, 300, 200), 50, 50, 0, "a mask image that is not there yet is transparent black");
    }

    /**
     * A mask creates a stacking context and does not change what a click finds, it applies to the whole subtree, and together with
     * a filter it is applied after the filter.
     * <p>
     * 마스크는 쌓임 맥락을 만들고 클릭이 찾는 것을 바꾸지 않으며 하위 트리 전체에 적용되고 필터와 함께 쓰면 필터 다음에 적용된다.
     */
    public void testStackingSubtreeAndFilter() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:100px;height:100px;background:#00f;mask-image:linear-gradient(#000,#000);mask-size:50px 100px;mask-repeat:no-repeat'>"
                + "<div style='width:100px;height:50px;background:#f00'></div></div>", 300, 200, null);
        Check.isTrue(RenderTestSupport.box(p, "a").isStackingContext(), "a mask makes a stacking context");
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        pixel(0xFF0000, img, 25, 25, 0, "the child inside the tile");
        pixel(0xFFFFFF, img, 75, 25, 0, "the child outside the tile is hidden too");
        pixel(0x0000FF, img, 25, 75, 0, "the box inside the tile");
        Check.isTrue(p.hitTestDocument(75, 75).getElement() != null && "a".equals(p.hitTestDocument(75, 75).getElement().getAttribute("id")),
                "a click on the masked-out part still finds the element");
        // 필터 다음에 마스크: invert 한 뒤 마스크로 오른쪽 절반을 가린다.
        BufferedImage fm = red("filter:invert(1);mask-image:linear-gradient(#000,#000);mask-size:50px 100px;mask-repeat:no-repeat");
        pixel(0x00FFFF, fm, 25, 50, 0, "filtered and masked in");
        pixel(0xFFFFFF, fm, 75, 50, 0, "masked out");
    }
}
