package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the blend modes (plan item P10-08, CSS Compositing and Blending Level 1): the formulas of the separable and the
 * non-separable modes with values computed by hand from section 10, the alpha handling of section 9, and the painting of
 * <code>mix-blend-mode</code>, <code>isolation</code> and <code>background-blend-mode</code> on a page, including the isolation that
 * every stacking context gives to what is inside it.
 * <p>
 * 혼합 모드 시험(계획 P10-08, CSS Compositing and Blending Level 1): 10 절에서 손으로 계산한 값으로 분리 가능/불가능 모드의 식, 9 절의 알파 처리, 페이지에서 <code>mix-blend-mode</code>,
 * <code>isolation</code>, <code>background-blend-mode</code> 를 그리는 것(모든 쌓임 맥락이 안쪽에 주는 격리 포함).
 */
public class BlendModeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BlendModeTest() {
    }

    /**
     * Blends two opaque colors with a mode and returns the result without alpha.
     * <p>
     * 불투명한 두 색을 모드로 혼합하고 알파를 뺀 결과를 반환한다.
     *
     * @param mode the mode name / 모드 이름
     * @param src  the source RGB / 원본 RGB
     * @param dst  the backdrop RGB / 배경 RGB
     * @return the RGB of the result / 결과 RGB
     */
    private static int mix(String mode, int src, int dst) {
        return BlendModes.blend(BlendModes.modeOf(mode), 0xFF000000 | src, 0xFF000000 | dst, 1f) & 0xFFFFFF;
    }

    /**
     * Checks the RGB value to a tolerance of one level per channel.
     * <p>
     * RGB 값을 채널당 한 단계의 허용 오차로 확인한다.
     *
     * @param expected the expected RGB / 기대 RGB
     * @param actual   the RGB / RGB
     * @param what     the message / 메시지
     */
    private static void rgb(int expected, int actual, String what) {
        Check.isTrue(RenderTestSupport.near(expected, actual, 1), what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual));
    }

    /**
     * The separable modes against the formulas of section 10.1.
     * <p>
     * 분리 가능한 모드를 10.1 절의 식에 대해 확인한다.
     */
    public void testSeparableFormulas() {
        rgb(0x804000, mix("multiply", 0xFF8000, 0x808080), "multiply");
        rgb(0xFF00FF, mix("screen", 0xFF0000, 0x0000FF), "screen of red and blue");
        rgb(0x7F7F7F, mix("screen", 0x000000, 0x7F7F7F), "screen with black keeps the backdrop");
        rgb(0x960064, mix("difference", 0xC86432, 0x326496), "difference");
        rgb(0x326432, mix("darken", 0xC86432, 0x326496), "darken");
        rgb(0xC86496, mix("lighten", 0xC86432, 0x326496), "lighten");
        // overlay(Cb=0.2, Cs=0.6) = hard-light(Cb=0.6, Cs=0.2) = multiply(0.6, 0.4) = 0.24
        rgb(0x3D3D3D, mix("overlay", 0x999999, 0x333333), "overlay on a dark backdrop multiplies");
        // hard-light(Cb=0.6, Cs=0.8) = screen(0.6, 0.6) = 0.84
        rgb(0xD6D6D6, mix("hard-light", 0xCCCCCC, 0x999999), "hard-light screens for a light source");
        rgb(0xFFFFFF, mix("color-dodge", 0x808080, 0x808080), "color-dodge reaches white when Cb = 1 - Cs");
        rgb(0x000000, mix("color-dodge", 0xFFFFFF, 0x000000), "color-dodge of a black backdrop stays black");
        rgb(0xFFFFFF, mix("color-dodge", 0xFFFFFF, 0x404040), "color-dodge with a white source is white");
        rgb(0x000000, mix("color-burn", 0x404040, 0x404040), "color-burn burns a dark backdrop with an equal source to black");
        rgb(0xFFFFFF, mix("color-burn", 0x000000, 0xFFFFFF), "color-burn of a white backdrop stays white");
        rgb(0x000000, mix("color-burn", 0x000000, 0x404040), "color-burn with a black source is black");
        // soft-light(Cb=0.5, Cs=0.25) = 0.5 - (1 - 0.5) * 0.5 * 0.5 = 0.375
        rgb(0x606060, mix("soft-light", 0x404040, 0x808080), "soft-light darkens for a dark source");
        // soft-light(Cb=0.25, Cs=1) = 0.25 + 1 * (D(0.25) - 0.25) = 0.5
        rgb(0x808080, mix("soft-light", 0xFFFFFF, 0x404040), "soft-light lightens with D(Cb)");
        rgb(0x808080, mix("exclusion", 0x808080, 0x808080), "exclusion of equal grays is middle gray");
        rgb(0xFF0000, mix("normal", 0xFF0000, 0x00FF00), "normal selects the source");
    }

    /**
     * The non-separable modes against the formulas of section 10.2.
     * <p>
     * 분리 불가능한 모드를 10.2 절의 식에 대해 확인한다.
     */
    public void testNonSeparableFormulas() {
        // luminosity: SetLum(Cb, Lum(Cs)) with Cb red (Lum 0.3), Cs gray 0.5: (1.2, 0.2, 0.2) clipped to (1, 0.2857, 0.2857)
        rgb(0xFF4949, mix("luminosity", 0x808080, 0xFF0000), "luminosity of a gray over red is clipped red");
        // color: SetLum(Cs, Lum(Cb)) with Cs red, Cb gray 0.5: the same numbers
        rgb(0xFF4949, mix("color", 0xFF0000, 0x808080), "color of red over gray");
        // 채도가 0 인 배경 위의 hue 와 saturation 은 배경 그대로이다.
        rgb(0x808080, mix("hue", 0xFF0000, 0x808080), "hue over a gray backdrop is the gray");
        rgb(0x808080, mix("saturation", 0xFF0000, 0x808080), "saturation over a gray backdrop is the gray");
        // 같은 색을 hue 로 혼합하면 그 색의 밝기와 채도를 유지하므로 그대로이다.
        rgb(0xC83264, mix("hue", 0xC83264, 0xC83264), "hue with the same color keeps it");
        // 밝기는 유지된다: 임의의 색 위의 color 혼합 결과의 밝기는 배경의 밝기와 같다.
        int r = mix("color", 0x2090E0, 0xC07030);
        double lumB = 0.3 * 0xC0 + 0.59 * 0x70 + 0.11 * 0x30;
        double lumR = 0.3 * ((r >> 16) & 0xFF) + 0.59 * ((r >> 8) & 0xFF) + 0.11 * (r & 0xFF);
        Check.near(lumB, lumR, 3, "color keeps the luminosity of the backdrop");
    }

    /**
     * Alpha: with a transparent backdrop the source stays as it is, with a transparent source the backdrop stays, and a half
     * transparent source mixes with the blended color by section 9.
     * <p>
     * 알파: 배경이 투명하면 원본 그대로, 원본이 투명하면 배경 그대로, 반투명한 원본은 9 절에 따라 혼합한 색과 섞인다.
     */
    public void testAlpha() {
        int none = BlendModes.blend(BlendModes.MULTIPLY, 0xFFFF0000, 0x00000000, 1f);
        Check.equal(0xFFFF0000, none, "over a transparent backdrop the source stays");
        int gone = BlendModes.blend(BlendModes.MULTIPLY, 0x00FF0000, 0xFF808080, 1f);
        Check.equal(0xFF808080, gone, "a transparent source leaves the backdrop");
        // multiply 의 B = (128, 0, 0); 절반 불투명: Cr = 0.5 * Cb + 0.5 * B
        int half = BlendModes.blend(BlendModes.MULTIPLY, 0x80FF0000, 0xFF808080, 1f);
        Check.equal(0xFF, half >>> 24, "an opaque backdrop gives an opaque result");
        rgb(0x804040, half & 0xFFFFFF, "half transparent multiply");
        // 그룹 불투명도 0.5 는 원본 알파에 곱해진다.
        int group = BlendModes.blend(BlendModes.NORMAL, 0xFFFFFFFF, 0xFF000000, 0.5f);
        rgb(0x808080, group & 0xFFFFFF, "normal at group opacity .5");
        // 배경이 반투명하면 결과 알파는 as + ab * (1 - as) 이고 혼합은 ab 로 가중된다.
        int ab = BlendModes.blend(BlendModes.MULTIPLY, 0xFFFF0000, 0x80808080, 1f);
        Check.equal(0xFF, ab >>> 24, "an opaque source gives an opaque result");
        // Cr = (1 - ab) * Cs + ab * B = 0.498 * (1,0,0) + 0.502 * (0.502, 0, 0) = 0.75
        Check.isTrue(Math.abs(((ab >> 16) & 0xFF) - 191) <= 2, "partly transparent backdrop weights the blend: " + Integer.toHexString(ab));
        int lighter = BlendModes.blend(BlendModes.PLUS_LIGHTER, 0xFF643200, 0xFF643200, 1f);
        rgb(0xC86400, lighter & 0xFFFFFF, "plus-lighter adds");
        int cap = BlendModes.blend(BlendModes.PLUS_LIGHTER, 0xFFC86400, 0xFFC86400, 1f);
        rgb(0xFFC800, cap & 0xFFFFFF, "plus-lighter is limited to 1");
    }

    /**
     * Returns a page with a gray square behind a red square with a blend mode.
     * <p>
     * 파란 네모 뒤의 회색 네모와 혼합 모드를 가진 빨간 네모가 있는 페이지의 이미지를 반환한다.
     *
     * @param redStyle the extra style of the red square / 빨간 네모의 추가 스타일
     * @param wrapper  the opening tag of an element around the red square, or "" / 빨간 네모를 감싸는 요소의 여는 태그. 없으면 ""
     * @return the image / 이미지
     */
    private static BufferedImage page(String redStyle, String wrapper) {
        String close = wrapper.length() == 0 ? "" : "</div>";
        String body = "<div style='position:absolute;left:0;top:0;width:100px;height:100px;background:#ccc'></div>" + wrapper
                + "<div style='position:absolute;left:50px;top:50px;width:100px;height:100px;background:#f00;" + redStyle + "'></div>" + close;
        return RenderTestSupport.paint(RenderTestSupport.page(body, 300, 200, null), 300, 200);
    }

    /**
     * A page with <code>mix-blend-mode: multiply</code>: the overlap is the product, the rest is the box over the page.
     * <p>
     * <code>mix-blend-mode: multiply</code> 페이지: 겹친 곳은 곱이고 나머지는 페이지 위의 상자이다.
     */
    public void testMixBlendMode() {
        BufferedImage img = page("mix-blend-mode:multiply", "");
        rgb(0xCC0000, RenderTestSupport.rgb(img, 75, 75), "multiply over #ccc");
        rgb(0xFF0000, RenderTestSupport.rgb(img, 125, 125), "multiply over the white page is the box color");
        rgb(0xCCCCCC, RenderTestSupport.rgb(img, 25, 25), "the backdrop box itself");
        BufferedImage normal = page("", "");
        rgb(0xFF0000, RenderTestSupport.rgb(normal, 75, 75), "without a blend mode the box covers");
        BufferedImage diff = page("mix-blend-mode:difference", "");
        rgb(0x33CCCC, RenderTestSupport.rgb(diff, 75, 75), "difference of #f00 and #ccc");
        BufferedImage screen = page("mix-blend-mode:screen", "");
        rgb(0xFFCCCC, RenderTestSupport.rgb(screen, 75, 75), "screen of #f00 and #ccc");
    }

    /**
     * The opacity of the group is applied together with the blend.
     * <p>
     * 그룹의 불투명도는 혼합과 함께 적용된다.
     */
    public void testBlendWithOpacity() {
        BufferedImage img = page("mix-blend-mode:multiply;opacity:.5", "");
        // B = (204, 0, 0), 절반 불투명: 0.5 * #ccc + 0.5 * B = (204, 102, 102)
        rgb(0xCC6666, RenderTestSupport.rgb(img, 75, 75), "multiply at opacity .5");
        rgb(0xFF8080, RenderTestSupport.rgb(img, 125, 125), "over the page it is the box at half opacity");
    }

    /**
     * Isolation: an <code>isolation:isolate</code> parent, or any other stacking context, hides what is behind it from the blend of
     * its children; a parent that is not a stacking context does not.
     * <p>
     * 격리: <code>isolation:isolate</code> 인 부모나 다른 쌓임 맥락은 자식의 혼합에서 그 뒤의 것을 가린다. 쌓임 맥락이 아닌 부모는 가리지 않는다.
     */
    public void testIsolation() {
        String wrapper = "<div style='%s;position:absolute;left:0;top:0;width:200px;height:200px'>";
        // 쌓임 맥락이 아닌 부모(position:static): 자식은 부모 뒤의 #ccc 와 혼합한다.
        BufferedImage plain = page("mix-blend-mode:multiply", "<div style='width:200px;height:200px'>");
        rgb(0xCC0000, RenderTestSupport.rgb(plain, 75, 75), "a parent that is not a stacking context does not isolate");
        // isolation:isolate
        BufferedImage iso = page("mix-blend-mode:multiply", String.format(wrapper, "isolation:isolate"));
        rgb(0xFF0000, RenderTestSupport.rgb(iso, 75, 75), "isolation:isolate hides the sibling behind the group");
        rgb(0xCCCCCC, RenderTestSupport.rgb(iso, 25, 25), "the sibling is still painted");
        // z-index 가 있는 위치 지정 부모도 쌓임 맥락이다.
        BufferedImage z = page("mix-blend-mode:multiply", String.format(wrapper, "z-index:0"));
        rgb(0xFF0000, RenderTestSupport.rgb(z, 75, 75), "z-index:0 makes a stacking context that isolates");
        // 불투명도가 1 보다 작은 부모도 맥락이다(그룹으로 그리므로 안쪽이 격리된다).
        BufferedImage op = page("mix-blend-mode:multiply", String.format(wrapper, "opacity:.99"));
        Check.isTrue(RenderTestSupport.rgb(op, 75, 75) >= 0xFC0000, "opacity makes a group: " + Integer.toHexString(RenderTestSupport.rgb(op, 75, 75)));
    }

    /**
     * <code>mix-blend-mode</code> and <code>isolation</code> make a stacking context, and a hit test is not affected by them.
     * <p>
     * <code>mix-blend-mode</code> 와 <code>isolation</code> 은 쌓임 맥락을 만들고 히트 테스트는 영향을 받지 않는다.
     */
    public void testStackingContextAndHit() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:50px;height:50px;mix-blend-mode:multiply;background:#f00'></div>"
                + "<div id=b style='width:50px;height:50px;isolation:isolate'></div><div id=c style='width:50px;height:50px'></div>", 300, 200, null);
        Check.isTrue(RenderTestSupport.box(p, "a").isStackingContext(), "mix-blend-mode makes a stacking context");
        Check.isTrue(RenderTestSupport.box(p, "b").isStackingContext(), "isolation:isolate makes a stacking context");
        Check.isFalse(RenderTestSupport.box(p, "c").isStackingContext(), "a plain box does not");
        LayoutBox a = RenderTestSupport.box(p, "a");
        Check.equal("a", p.hitTestDocument(a.getX() + 10, a.getY() + 10).getElement().getAttribute("id"), "a click finds the blended box");
    }

    /**
     * <code>background-blend-mode</code>: the layers are blended with each other and with the background color, not with what is behind
     * the box.
     * <p>
     * <code>background-blend-mode</code>: 레이어는 서로, 그리고 배경색과 혼합하며 상자 뒤의 것과는 혼합하지 않는다.
     */
    public void testBackgroundBlendMode() {
        BufferedImage a = RenderTestSupport.paint(RenderTestSupport.page("<div style='width:100px;height:100px;background-color:#888;"
                + "background-image:linear-gradient(#f00,#f00);background-blend-mode:multiply'></div>", 300, 200, null), 300, 200);
        rgb(0x880000, RenderTestSupport.rgb(a, 50, 50), "red multiplied with the gray background color");
        BufferedImage n = RenderTestSupport.paint(RenderTestSupport.page("<div style='width:100px;height:100px;background-color:#888;"
                + "background-image:linear-gradient(#f00,#f00)'></div>", 300, 200, null), 300, 200);
        rgb(0xFF0000, RenderTestSupport.rgb(n, 50, 50), "without the blend mode the layer covers the color");
        // 레이어 두 개: 아래 초록은 difference 로 파랑 배경색과, 위 빨강은 multiply 로 그 결과와 혼합한다.
        BufferedImage two = RenderTestSupport.paint(RenderTestSupport.page("<div style='width:100px;height:100px;background-color:#00f;"
                + "background-image:linear-gradient(#f00,#f00),linear-gradient(#0f0,#0f0);background-blend-mode:multiply,difference'></div>", 300, 200, null),
                300, 200);
        // difference(#00f, #0f0) = (0,255,255); multiply(#f00, (0,255,255)) = (0,0,0)
        rgb(0x000000, RenderTestSupport.rgb(two, 50, 50), "two layers with their own modes");
        // 상자 뒤의 것과는 혼합하지 않는다: 같은 배경이 흰 페이지 위에서 변하지 않는다.
        rgb(0xFFFFFF, RenderTestSupport.rgb(two, 150, 50), "outside of the box is the page");
        // 목록은 레이어 수에 맞게 되풀이된다.
        BufferedImage rep = RenderTestSupport.paint(RenderTestSupport.page("<div style='width:100px;height:100px;background-color:#ccc;"
                + "background-image:linear-gradient(#f00,#f00),linear-gradient(#f00,#f00);background-blend-mode:multiply'></div>", 300, 200, null), 300, 200);
        // 아래 레이어: multiply(#f00, #ccc) = (204,0,0); 위 레이어: multiply(#f00, (204,0,0)) = (204,0,0)
        rgb(0xCC0000, RenderTestSupport.rgb(rep, 50, 50), "the list of modes repeats for the layers");
    }
}
