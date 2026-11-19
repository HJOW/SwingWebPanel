package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the image functions of CSS Images Level 4 (plan item P10-08-h, https://www.w3.org/TR/css-images-4/ W3C Working Draft 24 September 2025):
 * <code>cross-fade()</code> (section 2.6: the syntax, the normalization of the percentages, the natural size as the weighted average of the images, the
 * premultiplied average of the pixels, the legacy <code>-webkit-cross-fade()</code>), <code>image()</code> (section 2.5: the fallback color, the solid-color
 * image, the <code>#xywh=</code> fragment, the direction tags) and <code>element()</code> (section 2.7: the rendering of a rendered element without its
 * transform, the decorated bounding box, the paint source of an <code>img</code> that is not rendered, the cycles), in <code>background-image</code>,
 * <code>border-image-source</code> and <code>mask-image</code>.
 * <p>
 * CSS Images Level 4 이미지 함수의 시험(계획 P10-08-h): <code>cross-fade()</code>(2.6 절: 문법, 백분율의 정규화, 이미지들의 가중 평균인 고유 크기, 화소의 프리멀티플라이드 평균,
 * 옛 <code>-webkit-cross-fade()</code>), <code>image()</code>(2.5 절: 대체 색, 단색 이미지, <code>#xywh=</code> 조각, 방향 꼬리표), <code>element()</code>(2.7 절: 변환 없이
 * 그리는 요소, 장식된 경계 상자, 그려지지 않는 <code>img</code> 의 그림 원본, 순환)를 <code>background-image</code>, <code>border-image-source</code>, <code>mask-image</code> 에서
 * 확인한다.
 */
public class ImageFunctionsPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageFunctionsPaintTest() {
    }

    /**
     * The images: <code>a.png</code> red 8 by 8, <code>b.png</code> green 16 by 16, <code>halves.png</code> 16 by 8 with a red left half and a green
     * right half.
     * <p>
     * 이미지: <code>a.png</code> 빨강 8x8, <code>b.png</code> 초록 16x16, <code>halves.png</code> 16x8 이며 왼쪽 절반은 빨강, 오른쪽 절반은 초록.
     *
     * @return the image source / 이미지 소스
     */
    private static RenderTestSupport.FixedImages images() {
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages();
        images.put("http://t.test/dir/a.png", ImageResource.loaded(RenderTestSupport.solid(8, 8, 0xFFFF0000)));
        images.put("http://t.test/dir/b.png", ImageResource.loaded(RenderTestSupport.solid(16, 16, 0xFF00FF00)));
        int[] px = new int[16 * 8];
        // 왼쪽 절반은 빨강, 오른쪽 절반은 초록으로 채운다.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                px[y * 16 + x] = x < 8 ? 0xFFFF0000 : 0xFF00FF00;
            }
        }
        images.put("http://t.test/dir/halves.png", ImageResource.loaded(new DecodedImage(16, 8, px)));
        return images;
    }

    /**
     * Paints a body and returns the image.
     * <p>
     * body 를 그려 이미지를 반환한다.
     *
     * @param body the markup / 마크업
     * @return the painted image / 그린 이미지
     */
    private static BufferedImage paint(String body) {
        RenderedPage p = RenderTestSupport.page(body, 100, 100, images());
        return RenderTestSupport.paint(p, 100, 100);
    }

    /**
     * Paints a 40 by 40 box at the top left with a background image that does not repeat and returns the RGB of a pixel.
     * <p>
     * 반복하지 않는 배경 이미지를 가진 40x40 상자를 왼쪽 위에 그려 화소의 RGB 를 반환한다.
     *
     * @param value the value of <code>background-image</code> / <code>background-image</code> 값
     * @param x     x / x
     * @param y     y / y
     * @return the RGB / RGB
     */
    private static int bg(String value, int x, int y) {
        return RenderTestSupport.rgb(paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:" + value + "'></div>"), x, y);
    }

    /**
     * Tells whether two RGB values are within a tolerance on every channel.
     * <p>
     * 두 RGB 값이 채널마다 허용 오차 안인지 알려 준다.
     *
     * @param expected the expected RGB / 기대 RGB
     * @param actual   the actual RGB / 실제 RGB
     * @param tol      the tolerance / 허용 오차
     * @param what     the description / 설명
     */
    private static void near(int expected, int actual, int tol, String what) {
        Check.isTrue(RenderTestSupport.near(expected, actual, tol), what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual));
    }

    /**
     * Returns the computed <code>background-image</code> of an element of a page.
     * <p>
     * 페이지 요소의 계산된 <code>background-image</code> 를 반환한다.
     *
     * @param value the declared value / 선언한 값
     * @return the computed value text / 계산 값 텍스트
     */
    private static String computed(String value) {
        RenderedPage p = RenderTestSupport.page("<div id='d' style='width:10px;height:10px;background-image:" + value + "'></div>", 100, 100, images());
        CssValue v = RenderTestSupport.box(p, "d").getStyle().get(Prop.BACKGROUND_IMAGE);
        return v == null ? "" : v.toCssText();
    }

    /**
     * The syntax of the three functions: what is valid is kept, what is not makes the declaration invalid (the initial value <code>none</code>).
     * <p>
     * 세 함수의 문법: 올바른 것은 남고, 틀린 것은 선언을 무효로 만든다(초기값 <code>none</code>).
     */
    public void testSyntax() {
        Check.isTrue(computed("cross-fade(url(a.png) 50%, url(b.png))").startsWith("cross-fade("), "two images, one percentage");
        Check.isTrue(computed("cross-fade(red, 20% blue)").startsWith("cross-fade("), "colors and the percentage before the image");
        Check.equal("none", computed("cross-fade(url(a.png) 120%, url(b.png))"), "a percentage above 100% is invalid");
        Check.equal("none", computed("cross-fade(url(a.png) -1%, url(b.png))"), "a negative percentage is invalid");
        Check.equal("none", computed("cross-fade(url(a.png) 10% 20%)"), "two percentages in an argument are invalid");
        Check.equal("none", computed("cross-fade(10%)"), "an argument without an image is invalid");
        Check.equal("none", computed("cross-fade()"), "no argument is invalid");
        Check.isTrue(computed("image(red)").startsWith("image("), "a color only");
        Check.isTrue(computed("image(\"a.png\", blue)").startsWith("image("), "a string and a color");
        Check.isTrue(computed("image(rtl url(a.png))").startsWith("image("), "a direction tag and a URL");
        Check.equal("none", computed("image(url(a.png) url(b.png))"), "two URLs are invalid");
        Check.equal("none", computed("image(red, blue)"), "a color before the comma is invalid");
        Check.equal("none", computed("image(up url(a.png))"), "an unknown tag is invalid");
        Check.isTrue(computed("element(#src)").startsWith("element(#src"), "an ID selector");
        Check.equal("none", computed("element(src)"), "an identifier is not an ID selector");
        Check.equal("none", computed("element(#1a)"), "a hash that is not an identifier is not an ID selector");
        Check.equal("none", computed("element(#a, #b)"), "only one selector");
        Check.isTrue(computed("-webkit-cross-fade(url(a.png), url(b.png), 30%)").startsWith("-webkit-cross-fade("), "the legacy form");
        Check.equal("none", computed("-webkit-cross-fade(url(a.png), url(b.png), 1.5)"), "the legacy amount is between 0 and 1");
    }

    /**
     * Colors in <code>cross-fade()</code> have no natural size, so the image fills the positioning area; the pixels are the weighted average.
     * <p>
     * <code>cross-fade()</code> 의 색은 고유 크기가 없으므로 이미지는 위치 기준 영역을 채운다. 화소는 가중 평균이다.
     */
    public void testColorsAreAveraged() {
        near(0x4000BF, bg("cross-fade(red 25%, blue 75%)", 35, 35), 2, "25% red and 75% blue everywhere in the box");
        near(0x800080, bg("cross-fade(red, blue)", 20, 20), 2, "omitted percentages share 100%");
        near(0x800080, bg("cross-fade(red 100%, blue 100%)", 20, 20), 2, "a sum above 100% is scaled down to 100%");
        near(0xFFB3B3, bg("cross-fade(red 30%)", 20, 20), 2, "a sum below 100% leaves transparent black: 30% red over the white page");
        near(0xFFFFFF, bg("cross-fade(white 50%, transparent 50%)", 20, 20), 1, "premultiplied: half transparent white, not gray");
        near(0x800080, bg("cross-fade(red 50%, cross-fade(blue 100%) 50%)", 20, 20), 2, "a nested cross-fade() is an image argument");
    }

    /**
     * The natural size is the weighted average of the images that have one; their pixels are rescaled to that size before they are averaged.
     * <p>
     * 고유 크기는 고유 크기가 있는 이미지들의 가중 평균이며, 화소는 평균하기 전에 그 크기로 늘린다.
     */
    public void testNaturalSizeOfImages() {
        BufferedImage img = paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:cross-fade(url(a.png) 50%, url(b.png) 50%)'></div>");
        near(0x808000, RenderTestSupport.rgb(img, 5, 5), 3, "red and green averaged");
        near(0x808000, RenderTestSupport.rgb(img, 10, 10), 3, "the image is 12 by 12 (the average of 8 and 16)");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 13, 13), "and not larger");
        BufferedImage w = paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:cross-fade(url(a.png) 75%, url(b.png) 25%)'></div>");
        Check.isTrue(RenderTestSupport.rgb(w, 9, 9) != 0xFFFFFF, "75% of 8 and 25% of 16 is 10 by 10");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(w, 11, 11), "and not larger");
        BufferedImage c = paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:cross-fade(url(a.png) 50%, blue 50%)'></div>");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(c, 10, 10), "a color does not take part in the size: the image is 8 by 8");
        near(0x800080, RenderTestSupport.rgb(c, 4, 4), 2, "but in the pixels");
        BufferedImage m = paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:cross-fade(url(missing.png) 50%, red 50%)'></div>");
        near(0xFF8080, RenderTestSupport.rgb(m, 30, 30), 2, "an image that is missing is transparent and has no size: half red over the whole box");
        BufferedImage legacy = paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:-webkit-cross-fade(url(a.png), url(b.png), 25%)'></div>");
        near(0xBF4000, RenderTestSupport.rgb(legacy, 3, 3), 3, "the legacy form keeps 25% of the second image");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(legacy, 11, 11), "and has the weighted size 10 by 10");
    }

    /**
     * <code>background-size</code> and repetition use the natural size of the cross-fade: a repeating 12 by 12 image starts again at 12.
     * <p>
     * <code>background-size</code> 와 반복은 cross-fade 의 고유 크기를 쓴다: 반복하는 12x12 이미지는 12 에서 다시 시작한다.
     */
    public void testRepeatedTiles() {
        BufferedImage img = paint("<div style='width:40px;height:40px;background-image:cross-fade(url(a.png) 50%, url(b.png) 50%)'></div>");
        near(0x808000, RenderTestSupport.rgb(img, 14, 14), 3, "the second tile");
        BufferedImage cover = paint("<div style='width:40px;height:20px;background-repeat:no-repeat;background-size:contain;background-image:cross-fade(url(a.png) 50%, url(b.png) 50%)'></div>");
        near(0x808000, RenderTestSupport.rgb(cover, 18, 18), 3, "contain scales the 12 by 12 image to 20 by 20");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(cover, 22, 10), "and not wider");
    }

    /**
     * <code>image()</code>: a color only is a solid-color image without natural size, a URL that fails gives the fallback color, a fragment cuts a part, and a
     * direction tag that differs from the direction of the box flips the image.
     * <p>
     * <code>image()</code>: 색만 있으면 고유 크기 없는 단색 이미지, 실패한 URL 은 대체 색, 조각은 일부를 잘라 내고, 상자의 방향과 다른 방향 꼬리표는 이미지를 뒤집는다.
     */
    public void testImageFunction() {
        Check.equal(0x0000FF, bg("image(blue)", 35, 35), "a solid-color image fills the box even with no-repeat");
        Check.equal(0x0000FF, bg("image(url(missing.png), blue)", 35, 35), "the URL fails: the fallback color");
        Check.equal(0xFF0000, bg("image(url(a.png), blue)", 3, 3), "the URL works: the image");
        Check.equal(0xFFFFFF, bg("image(url(a.png), blue)", 20, 20), "with its natural size 8 by 8");
        Check.equal(0xFFFFFF, bg("image(url(missing.png))", 3, 3), "without a color a failing URL is an invalid image");
        Check.equal(0x00FF00, bg("image(\"halves.png#xywh=8,0,8,8\")", 3, 3), "the fragment is the green right half");
        Check.equal(0xFFFFFF, bg("image(\"halves.png#xywh=8,0,8,8\")", 10, 3), "an 8 by 8 image");
        Check.equal(0xFF0000, bg("image(\"halves.png#xywh=percent:0,0,50,100\")", 3, 3), "a percent fragment: the red left half");
        Check.equal(0xFFFFFF, bg("image(\"halves.png#xywh=percent:0,0,50,100\")", 10, 3), "8 pixels wide");
        Check.equal(0xFFFFFF, bg("image(\"halves.png#foo\")", 3, 3), "an unknown fragment is an invalid image");
        Check.equal(0x00FF00, bg("image(\"halves.png#foo\", lime)", 3, 3), "an unknown fragment falls back on the color");
        Check.equal(0xFF0000, bg("image(ltr url(halves.png))", 3, 3), "an ltr image in an ltr box is not flipped");
        Check.equal(0x00FF00, bg("image(rtl url(halves.png))", 3, 3), "an rtl image in an ltr box is flipped: green on the left");
        BufferedImage rtl = paint("<div dir='rtl' style='width:40px;height:40px;background-repeat:no-repeat;background-image:image(rtl url(halves.png))'></div>");
        Check.equal(0xFF0000, RenderTestSupport.rgb(rtl, 3, 3), "an rtl image in an rtl box is not flipped");
        Check.equal(0x0000FF, bg("image(url(a.png)), image(blue)", 20, 20), "image() as one layer of several");
    }

    /**
     * <code>element()</code> paints a rendered element at its size, without its transform, and clips what is outside its decorated bounding box.
     * <p>
     * <code>element()</code> 는 그려지는 요소를 그 크기로, 변환 없이 그리며 장식된 경계 상자 밖은 자른다.
     */
    public void testElementImage() {
        String src = "<div id='src' style='position:absolute;left:0;top:60px;width:20px;height:10px;background:lime;border-left:4px solid blue'></div>";
        BufferedImage img = paint(src + "<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:element(#src)'></div>");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 1, 1), "the border of the element is at the left edge of the image");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 10, 5), "its background");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 30, 5), "the image is 24 by 10");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 10, 15), "and not higher");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 10, 65), "the element itself is still painted");
        String turned = "<div id='src' style='position:absolute;left:0;top:60px;width:20px;height:20px;background:lime;transform:rotate(45deg)'></div>";
        BufferedImage t = paint(turned + "<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:element(#src)'></div>");
        Check.equal(0x00FF00, RenderTestSupport.rgb(t, 1, 1), "the transform of the element is ignored: the corner is lime");
        String text = "<div id='src' style='position:absolute;left:0;top:60px;width:30px;height:12px;color:#000;font:10px monospace'>XXX</div>";
        BufferedImage w = paint(text + "<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:element(#src)'></div>");
        Check.isTrue(RenderTestSupport.ink(w, 0, 0, 30, 12) > 0, "the text of the element is painted in the image");
        BufferedImage repeated = paint("<div id='src' style='position:absolute;left:0;top:60px;width:10px;height:10px;background:red'></div>"
                + "<div style='width:40px;height:40px;background-image:element(#src);background-size:5px 5px;background-repeat:repeat'></div>");
        Check.equal(0xFF0000, RenderTestSupport.rgb(repeated, 37, 37), "the image is scaled by background-size and repeated");
    }

    /**
     * Invalid images: a missing element, an element that is not rendered and has no paint source, a reference to the element itself or an ancestor; an
     * <code>img</code> that is not rendered is a paint source.
     * <p>
     * 잘못된 이미지: 없는 요소, 그려지지 않으며 그림 원본이 없는 요소, 자기 자신이나 조상의 참조. 그려지지 않는 <code>img</code> 는 그림 원본이다.
     */
    public void testElementInvalidAndPaintSources() {
        Check.equal(0xFFFFFF, bg("element(#nothing)", 5, 5), "no such element");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(paint("<p id='none' style='display:none;background:red;width:10px;height:10px'></p>"
                + "<div style='width:40px;height:40px;background-image:element(#none)'></div>"), 5, 5), "an element without a box is not a paint source");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(paint("<div id='self' style='width:40px;height:40px;background:element(#self)'></div>"), 5, 5),
                "an element that uses itself: a cycle, transparent");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(paint("<div id='outer' style='width:40px;height:40px;border-top:1px solid #fff'><div style='width:20px;height:20px;background:element(#outer)'></div></div>"),
                5, 5), "an element that uses its ancestor: a cycle");
        BufferedImage img = paint("<img id='pic' src='a.png' style='display:none'><div style='width:40px;height:40px;background-repeat:no-repeat;background-image:element(#pic)'></div>");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 3, 3), "an img that is not rendered gives its image");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 10, 10), "at its natural size");
        BufferedImage inner = paint("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:element(#child)'><div id='child' style='width:10px;height:10px;background:blue'></div></div>");
        Check.equal(0x0000FF, RenderTestSupport.rgb(inner, 3, 3), "an element may use its descendant");
    }

    /**
     * <code>list-style-image</code> (CSS Lists 3 section 3.3) takes every <code>&lt;image&gt;</code>: the outside marker is the image at its natural size (an image
     * without one is a 1em square), its bottom on the baseline, even with <code>list-style-type: none</code>; an image that is missing falls back on the
     * <code>list-style-type</code>.
     * <p>
     * <code>list-style-image</code>(CSS Lists 3 3.3 절)는 모든 <code>&lt;image&gt;</code> 를 받는다: 바깥쪽 표식은 고유 크기의 이미지이고(없으면 1em 정사각형) 아래쪽이 기준선에 놓이며
     * <code>list-style-type: none</code> 이어도 보인다. 없는 이미지는 <code>list-style-type</code> 으로 대신한다.
     */
    public void testListStyleImage() {
        String list = "<ul style='margin:0;padding:0 0 0 40px;font:10px monospace;line-height:20px;list-style-image:";
        BufferedImage url = paint(list + "url(a.png)'><li>x</li></ul>");
        Check.isTrue(RenderTestSupport.touched(url, 0, 0, 40, 20) > 0, "something is painted left of the item");
        Check.isTrue(countRed(url, 0, 0, 40, 20) >= 60, "the red 8 by 8 image is the marker: " + countRed(url, 0, 0, 40, 20));
        BufferedImage none = paint(list + "url(a.png);list-style-type:none'><li>x</li></ul>");
        Check.isTrue(countRed(none, 0, 0, 40, 20) >= 60, "list-style-type: none does not hide the image");
        BufferedImage grad = paint(list + "linear-gradient(red, red)'><li>x</li></ul>");
        Check.isTrue(countRed(grad, 0, 0, 40, 20) >= 90, "a gradient has no natural size: a 1em square (10 by 10): " + countRed(grad, 0, 0, 40, 20));
        BufferedImage fade = paint(list + "cross-fade(url(a.png) 50%, url(b.png) 50%)'><li>x</li></ul>");
        Check.isTrue(RenderTestSupport.touched(fade, 0, 0, 40, 20) > 100, "a cross-fade of 12 by 12 as the marker");
        BufferedImage missing = paint(list + "url(missing.png)'><li>x</li></ul>");
        Check.equal(0, countRed(missing, 0, 0, 40, 20), "a missing image is not painted");
        Check.isTrue(RenderTestSupport.ink(missing, 0, 0, 40, 20) > 0, "the disc of list-style-type is shown instead");
    }

    /**
     * Counts the red pixels of a rectangle.
     * <p>
     * 사각형의 빨간 화소를 센다.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right / 오른쪽
     * @param y1  bottom / 아래쪽
     * @return the count / 수
     */
    private static int countRed(BufferedImage img, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 사각형의 화소를 훑는다.
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (RenderTestSupport.rgb(img, x, y) == 0xFF0000) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * The other places that take an <code>&lt;image&gt;</code>: <code>border-image-source</code> and <code>mask-image</code>.
     * <p>
     * <code>&lt;image&gt;</code> 를 받는 다른 곳: <code>border-image-source</code> 와 <code>mask-image</code>.
     */
    public void testBorderImageAndMask() {
        BufferedImage b = paint("<div style='position:absolute;left:10px;top:10px;width:40px;height:40px;border:10px solid #000;border-image:cross-fade(red 50%, blue 50%) 1 fill / 10px'></div>");
        near(0x800080, RenderTestSupport.rgb(b, 15, 15), 2, "the border image area shows the cross-fade");
        near(0x800080, RenderTestSupport.rgb(b, 40, 40), 2, "and the middle (fill)");
        BufferedImage e = paint("<div style='position:absolute;left:10px;top:10px;width:40px;height:40px;border:10px solid #000;border-image:image(url(missing.png), lime) 1 fill / 10px'></div>");
        Check.equal(0x00FF00, RenderTestSupport.rgb(e, 15, 15), "image() with a fallback color as border image");
        BufferedImage m = paint("<div style='width:40px;height:40px;background:red;mask-image:cross-fade(black 50%)'></div>");
        near(0xFF8080, RenderTestSupport.rgb(m, 20, 20), 3, "a mask of half alpha shows half of the red");
    }
}
