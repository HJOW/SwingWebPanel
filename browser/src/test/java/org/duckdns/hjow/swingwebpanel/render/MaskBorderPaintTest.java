package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>mask-border</code> (plan item P10-08, CSS Masking Level 1, https://www.w3.org/TR/css-masking-1/#the-mask-border; the slicing is that of
 * <code>border-image</code>, CSS Backgrounds and Borders Level 3, 6.1 to 6.5): the nine parts of the source (an image or a gradient) cut by
 * <code>mask-border-slice</code>, drawn into the border image area with <code>mask-border-width</code> and <code>mask-border-outset</code>; the middle part only with
 * <code>fill</code>; the repeat modes of the edges; the alpha and the luminance mode; the combination with <code>mask-image</code>; and the shorthand.
 * <p>
 * <code>mask-border</code> 시험(계획 P10-08, CSS Masking Level 1): 원본(이미지나 그라디언트)을 <code>mask-border-slice</code> 로 자른 아홉 조각을 <code>mask-border-width</code> 와
 * <code>mask-border-outset</code> 으로 border 이미지 영역에 그리고, 가운데는 <code>fill</code> 일 때만 그리며, 가장자리의 반복 방식, 알파와 휘도 모드, <code>mask-image</code> 와의 결합,
 * 단축 속성을 확인한다.
 */
public class MaskBorderPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MaskBorderPaintTest() {
    }

    /** The style of the box that the tests mask: a red 100 by 60 box at (20, 20) with a 10 px border. 시험이 마스크하는 상자의 스타일: (20, 20) 에 있는 border 10px 의 빨간 100x60 상자. */
    private static final String BOX = "position:absolute;left:20px;top:20px;width:80px;height:40px;border:10px solid #f00;background:#f00;box-sizing:content-box;";

    /**
     * Paints the red box with the mask style and returns the image.
     * <p>
     * 마스크 스타일을 가진 빨간 상자를 그려 이미지를 반환한다.
     *
     * @param mask the style of the mask / 마스크 스타일
     * @return the image / 이미지
     */
    private static BufferedImage paint(String mask) {
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + mask + "'></div>", 300, 200, null);
        return RenderTestSupport.paint(p, 300, 200);
    }

    /**
     * Tells whether a pixel is painted red (the content shows through the mask).
     * <p>
     * 화소가 빨갛게 칠해졌는지(내용이 마스크를 통해 보이는지) 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return true if it is the red of the box / 상자의 빨강이면 true
     */
    private static boolean red(BufferedImage img, int x, int y) {
        return RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), 0xFF0000, 24);
    }

    /**
     * Tells whether a pixel is untouched (the white page).
     * <p>
     * 화소가 건드려지지 않았는지(흰 페이지) 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return true if it is white / 흰색이면 true
     */
    private static boolean white(BufferedImage img, int x, int y) {
        return RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), 0xFFFFFF, 24);
    }

    /**
     * With an opaque source and the slices equal to the widths, only the ring of the border shows (the border box is 100 by 60 at (20, 20)); the middle shows with
     * <code>fill</code>.
     * <p>
     * 불투명한 원본과 너비와 같은 조각이면 border 의 고리만 보인다(border box 는 (20, 20) 의 100x60). 가운데는 <code>fill</code> 이면 보인다.
     */
    public void testRingAndFill() {
        String src = "mask-border-source:linear-gradient(#000,#000);mask-border-slice:10;mask-border-width:10px;";
        BufferedImage ring = paint(src);
        Check.isTrue(red(ring, 25, 25), "the top left corner shows");
        Check.isTrue(red(ring, 60, 24), "the top edge shows");
        Check.isTrue(red(ring, 115, 70), "the bottom right corner region shows (the right edge at x 110 to 120)");
        Check.isTrue(white(ring, 60, 45), "the middle does not show without fill");
        Check.isTrue(white(ring, 10, 10), "outside the box nothing is painted");
        BufferedImage filled = paint("mask-border-source:linear-gradient(#000,#000);mask-border-slice:10 fill;mask-border-width:10px;");
        Check.isTrue(red(filled, 60, 45), "the middle shows with fill");
        Check.isTrue(red(filled, 25, 25), "and so do the edges");
        BufferedImage none = paint("mask-border-source:none");
        Check.isTrue(red(none, 60, 45), "none: no mask at all");
    }

    /**
     * The mask mode: the default is alpha; in the luminance mode a black part of the source hides and a white part shows.
     * <p>
     * 마스크 모드: 기본은 알파이고, 휘도 모드에서는 원본의 검은 부분은 숨기고 흰 부분은 보인다.
     */
    public void testLuminanceMode() {
        String half = "mask-border-source:linear-gradient(to right,#000 50%,#fff 50%);mask-border-slice:10 fill;mask-border-width:10px;";
        BufferedImage alpha = paint(half);
        Check.isTrue(red(alpha, 30, 45), "alpha: both halves of an opaque source show (left)");
        Check.isTrue(red(alpha, 100, 45), "alpha: right");
        BufferedImage lum = paint(half + "mask-border-mode:luminance");
        Check.isTrue(white(lum, 25, 25), "luminance: the black corner hides");
        Check.isTrue(red(lum, 115, 25), "luminance: the white corner shows");
        Check.isTrue(white(lum, 40, 45), "luminance: the black part of the middle hides (the middle is stretched over the source: x 10 to 90 of 100)");
        Check.isTrue(red(lum, 100, 45), "luminance: the white part of the middle shows");
    }

    /**
     * The sizes: <code>mask-border-width</code> scales the slices (a 5 px slice drawn 20 px wide), a number multiplies the border width, and
     * <code>mask-border-outset</code> grows the area beyond the border box.
     * <p>
     * 크기: <code>mask-border-width</code> 는 조각을 늘린다(5px 조각을 20px 너비로), 숫자는 border 너비에 곱하고, <code>mask-border-outset</code> 은 영역을 border box 밖으로 넓힌다.
     */
    public void testWidthAndOutset() {
        String half = "mask-border-source:linear-gradient(to right,#000 5%,#fff 5%);mask-border-mode:luminance;mask-border-slice:5;";
        BufferedImage wide = paint(half + "mask-border-width:20px");
        Check.isTrue(white(wide, 30, 30), "the corner slice is black and drawn 20 px wide: it hides the whole corner 20 by 20 (x 20 to 40)");
        Check.isTrue(white(wide, 38, 38), "also near its far edge");
        Check.isTrue(red(wide, 60, 25), "the top edge (slice x 5 to 95: mostly white) shows");
        BufferedImage numbers = paint(half + "mask-border-width:2");
        Check.isTrue(white(numbers, 30, 30), "a number multiplies the border width (2 x 10 = 20 px)");
        Check.isTrue(red(numbers, 50, 25), "the top edge shows");
        BufferedImage outside = paint("mask-border-source:linear-gradient(#000,#000);mask-border-slice:10;mask-border-width:10px;mask-border-outset:10px");
        Check.isTrue(white(outside, 25, 25), "the ring moved outside the border box (x 10 to 20): the box is hidden");
        BufferedImage outset = paint("mask-border-source:linear-gradient(#000,#000);mask-border-slice:10;mask-border-width:20px;mask-border-outset:10px");
        Check.isTrue(red(outset, 25, 25), "a ring that is 20 px wide and starts 10 px outside reaches 10 px into the box");
        Check.isTrue(white(outset, 35, 35), "and not further");
    }

    /**
     * The repeat modes of the edges: <code>stretch</code> stretches the edge slice, <code>repeat</code> tiles it (centered), <code>round</code> makes the number of tiles
     * whole, and <code>space</code> leaves gaps between whole tiles.
     * <p>
     * 가장자리의 반복 방식: <code>stretch</code> 는 가장자리 조각을 늘이고, <code>repeat</code> 는 타일로 되풀이하며(가운데 맞춤), <code>round</code> 는 타일 수를 정수로 맞추고,
     * <code>space</code> 는 정수 개의 타일 사이에 틈을 둔다.
     */
    public void testRepeatModes() {
        // 4x4 원본에서 윗가장자리 조각은 (1,0) 과 (2,0) 두 화소이고 (1,0) 만 불투명하다: 너비 12px 로 그리면 타일은 24px 이고 앞 12px 만 보인다.
        BufferedImage stretch = edge("stretch");
        Check.isTrue(red(stretch, 50, 26), "stretch: one tile of 80 px, the first half shows (x 30 to 70)");
        Check.isTrue(white(stretch, 90, 26), "stretch: the second half is hidden");
        BufferedImage repeat = edge("repeat");
        Check.isTrue(red(repeat, 40, 26), "repeat: tiles of 24 px centered (five of them, from x 10): the first half of the tile at 34 shows");
        Check.isTrue(white(repeat, 52, 26), "repeat: and the second half of it is hidden");
        Check.isTrue(red(repeat, 64, 26), "repeat: the next tile shows again");
        Check.isTrue(white(repeat, 76, 26), "repeat: and is hidden again");
        Check.isTrue(red(repeat, 88, 26), "repeat: the tile at 82");
        Check.isTrue(white(repeat, 100, 26), "repeat: the end of that tile");
        Check.isTrue(red(repeat, 108, 26), "repeat: the last tile is clipped by the region (x 106 to 110)");
        BufferedImage round = edge("round");
        Check.isTrue(red(round, 36, 26) && white(round, 50, 26) && red(round, 63, 26) && white(round, 77, 26) && red(round, 90, 26) && white(round, 103, 26),
                "round: three tiles of 26.7 px fill the region exactly");
        BufferedImage space = edge("space");
        // 영역 80px 에 24px 타일 셋이 들어가고 남는 8px 은 앞, 사이, 뒤 네 틈에 2px 씩 나뉜다(CSS Backgrounds 3 5.6): 타일은 x 32, 58, 84 에서 시작한다.
        Check.isTrue(red(space, 36, 26) && white(space, 50, 26) && red(space, 62, 26) && white(space, 76, 26) && red(space, 88, 26) && white(space, 102, 26),
                "space: three tiles of 24 px with 2 px before, between and after them");
        Check.isTrue(white(space, 31, 26) && white(space, 57, 26), "space: the gaps are empty");
    }

    /**
     * Paints the box whose source is a 4 by 4 image with one opaque pixel (1, 0) in the top edge slice, so that the top edge region shows the tiles of the repeat mode.
     * <p>
     * 원본이 4x4 이미지이고 윗가장자리 조각에 불투명 화소 (1, 0) 하나가 있어서 윗가장자리 영역이 반복 방식의 타일을 보여 주는 상자를 그린다.
     *
     * @param repeat the repeat mode / 반복 방식
     * @return the image / 이미지
     */
    private static BufferedImage edge(String repeat) {
        int[] px = new int[16];
        px[1] = 0xFF000000;
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/e.png", org.duckdns.hjow.swingwebpanel.render.ImageResource.loaded(new DecodedImage(4, 4, px)));
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + "mask-border-source:url(e.png);mask-border-slice:1;mask-border-width:12px 10px 0 10px;"
                + "mask-border-repeat:" + repeat + " stretch'></div>", 300, 200, images);
        return RenderTestSupport.paint(p, 300, 200);
    }

    /**
     * <code>mask-border</code> is combined with the layers of <code>mask-image</code>: the border is added on top, so what either of them shows is shown.
     * <p>
     * <code>mask-border</code> 는 <code>mask-image</code> 의 레이어와 결합한다: 테두리는 맨 위에 더해지므로 둘 중 하나가 보여 주는 것이 보인다.
     */
    public void testWithMaskImage() {
        BufferedImage img = paint("mask-image:linear-gradient(#000,#000);mask-size:30px 30px;mask-repeat:no-repeat;mask-position:0 0;"
                + "mask-border-source:linear-gradient(#000,#000);mask-border-slice:10;mask-border-width:10px;mask-border-outset:0");
        Check.isTrue(red(img, 25, 25), "the mask image shows the top left corner");
        Check.isTrue(red(img, 115, 70), "the border shows the bottom right corner");
        Check.isTrue(white(img, 80, 50), "and nothing shows the rest");
    }

    /**
     * The properties: the grammar and the shorthand with the slash separated width and outset.
     * <p>
     * 속성: 문법과 슬래시로 이어지는 너비와 outset 이 붙은 단축 속성.
     */
    public void testProperties() {
        PropertyRegistry reg = PropertyRegistry.getInstance();
        Check.equal("10 20% fill", parse(reg, "mask-border-slice", "10 20% fill", "mask-border-slice"), "slice with fill");
        Check.equal("INVALID", parse(reg, "mask-border-slice", "fill", "mask-border-slice"), "fill alone is not a slice");
        Check.equal("1 auto 5px", parse(reg, "mask-border-width", "1 auto 5px", "mask-border-width"), "widths");
        Check.equal("INVALID", parse(reg, "mask-border-outset", "10%", "mask-border-outset"), "an outset cannot be a percentage");
        Check.equal("repeat round", parse(reg, "mask-border-repeat", "repeat round", "mask-border-repeat"), "repeat modes");
        Check.equal("luminance", parse(reg, "mask-border-mode", "luminance", "mask-border-mode"), "mode");
        Check.equal("linear-gradient(red, blue)", parse(reg, "mask-border", "linear-gradient(red, blue) 5 fill / 10px / 2 round luminance", "mask-border-source"), "the source of the shorthand");
        Check.equal("5 fill", parse(reg, "mask-border", "linear-gradient(red, blue) 5 fill / 10px / 2 round luminance", "mask-border-slice"), "the slice");
        Check.equal("10px", parse(reg, "mask-border", "linear-gradient(red, blue) 5 fill / 10px / 2 round luminance", "mask-border-width"), "the width after the first slash");
        Check.equal("2", parse(reg, "mask-border", "linear-gradient(red, blue) 5 fill / 10px / 2 round luminance", "mask-border-outset"), "the outset after the second slash");
        Check.equal("round", parse(reg, "mask-border", "linear-gradient(red, blue) 5 fill / 10px / 2 round luminance", "mask-border-repeat"), "the repeat");
        Check.equal("luminance", parse(reg, "mask-border", "linear-gradient(red, blue) 5 fill / 10px / 2 round luminance", "mask-border-mode"), "the mode");
        Check.equal("initial", parse(reg, "mask-border", "linear-gradient(red, blue) 5", "mask-border-mode"), "an unmentioned longhand is reset");
        Check.equal("none", parse(reg, "mask-border", "none", "mask-border-source"), "none");
        Check.equal("3", parse(reg, "mask-border", "5 / / 3", "mask-border-outset"), "an omitted width between two slashes is allowed");
        Check.equal("initial", parse(reg, "mask-border", "5 / / 3", "mask-border-width"), "and the width is reset");
        Check.equal("INVALID", parse(reg, "mask-border", "5 / 3 /", "mask-border-slice"), "a slash without an outset after it");
        Check.equal("INVALID", parse(reg, "mask-border", "5 / 3 / 2 / 1", "mask-border-slice"), "a third slash");
    }

    /**
     * Parses a declaration and returns the text of one longhand or the name of the result code.
     * <p>
     * 선언을 파싱해 개별 속성 하나의 문자열 또는 결과 코드 이름을 반환한다.
     *
     * @param reg      the registry / 등록부
     * @param name     the property / 속성
     * @param value    the value / 값
     * @param longhand the longhand to read / 읽을 개별 속성
     * @return the text, or UNKNOWN, UNSUPPORTED, INVALID, MISSING / 문자열 또는 결과 코드 이름
     */
    private static String parse(PropertyRegistry reg, String name, String value, String longhand) {
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int r = reg.parse(name, value, out);
        if (r == PropertyRegistry.UNKNOWN) {
            return "UNKNOWN";
        }
        if (r == PropertyRegistry.UNSUPPORTED) {
            return "UNSUPPORTED";
        }
        if (r == PropertyRegistry.INVALID) {
            return "INVALID";
        }
        CssValue x = out.get(Integer.valueOf(reg.byName(longhand).getId()));
        return x == null ? "MISSING" : x.toCssText();
    }

    /**
     * A source that cannot be used (an image that has not arrived or does not exist) is treated like <code>none</code>: the box is painted without a mask, unless
     * <code>mask-image</code> masks it.
     * <p>
     * 쓸 수 없는 원본(아직 도착하지 않았거나 없는 이미지)은 <code>none</code> 처럼 다룬다: <code>mask-image</code> 가 마스크하지 않으면 상자는 마스크 없이 그려진다.
     */
    public void testUnavailableSource() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + "mask-border-source:url(missing.png);mask-border-slice:1 fill;mask-border-width:10px'></div>", 300,
                200, new RenderTestSupport.FixedImages());
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.isTrue(red(img, 60, 45), "the whole box shows when the source is missing");
        RenderedPage q = RenderTestSupport.page("<div id=a style='" + BOX + "mask-image:linear-gradient(#000,#000);mask-size:30px 30px;mask-repeat:no-repeat;"
                + "mask-border-source:url(missing.png);mask-border-slice:1;mask-border-width:10px'></div>", 300, 200, new RenderTestSupport.FixedImages());
        BufferedImage masked = RenderTestSupport.paint(q, 300, 200);
        Check.isTrue(red(masked, 25, 25), "mask-image still masks");
        Check.isTrue(white(masked, 80, 50), "and hides the rest");
    }

    /**
     * An image source (a decoded image) is cut like a gradient: a 3 by 3 image with a transparent middle and opaque edges gives a frame, the pixels of the source
     * are the slices.
     * <p>
     * 이미지 원본(디코딩된 이미지)도 그라디언트처럼 잘린다: 가운데가 투명하고 가장자리가 불투명한 3x3 이미지는 틀이 되며 원본의 화소가 조각이다.
     */
    public void testImageSource() {
        int[] px = new int[9];
        // 가장자리 여덟 칸은 불투명한 검정, 가운데는 투명한 3x3 이미지를 만든다.
        for (int i = 0; i < 9; i++) {
            px[i] = i == 4 ? 0 : 0xFF000000;
        }
        DecodedImage di = new DecodedImage(3, 3, px);
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/frame.png", org.duckdns.hjow.swingwebpanel.render.ImageResource.loaded(di));
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + "mask-border-source:url(frame.png);mask-border-slice:1 fill;mask-border-width:10px'></div>", 300, 200,
                images);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.isTrue(red(img, 25, 25), "the corner pixel of the image fills the corner region");
        Check.isTrue(red(img, 60, 24), "the edge pixel fills the edge region");
        Check.isTrue(white(img, 60, 45), "the transparent middle pixel hides the middle (fill)");
    }
}
