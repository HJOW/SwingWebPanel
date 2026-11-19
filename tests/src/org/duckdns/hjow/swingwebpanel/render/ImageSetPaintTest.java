package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>image-set()</code> (plan item P10-08, CSS Images Level 4, https://www.w3.org/TR/css-images-4/#image-set-notation) in <code>background-image</code> and
 * <code>border-image-source</code>: the candidate of the smallest resolution that is not below 1x is selected (the largest one if all are below), the resolution
 * units <code>x</code>, <code>dppx</code>, <code>dpi</code> and the strings as image names are understood, and the natural size of the image is divided by its resolution.
 * <p>
 * <code>background-image</code> 와 <code>border-image-source</code> 의 <code>image-set()</code> 시험(계획 P10-08, CSS Images Level 4 image-set 표기): 1x 보다 작지 않은 해상도 중 가장 작은
 * 후보를 고르고(모두 작으면 가장 큰 것), 해상도 단위 <code>x</code>, <code>dppx</code>, <code>dpi</code> 와 이미지 이름으로 쓴 문자열을 알아보며, 이미지의 고유 크기를 그 해상도로 나눈다.
 */
public class ImageSetPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageSetPaintTest() {
    }

    /**
     * The candidate images: <code>a.png</code> is solid red, <code>b.png</code> solid green and <code>c.png</code> solid blue, each 8 by 8 pixels.
     * <p>
     * 후보 이미지: <code>a.png</code> 는 빨강, <code>b.png</code> 는 초록, <code>c.png</code> 는 파랑 단색이며 모두 8x8 화소이다.
     *
     * @return the image source / 이미지 소스
     */
    private static RenderTestSupport.FixedImages images() {
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages();
        images.put("http://t.test/dir/a.png", ImageResource.loaded(RenderTestSupport.solid(8, 8, 0xFFFF0000)));
        images.put("http://t.test/dir/b.png", ImageResource.loaded(RenderTestSupport.solid(8, 8, 0xFF00FF00)));
        images.put("http://t.test/dir/c.png", ImageResource.loaded(RenderTestSupport.solid(8, 8, 0xFF0000FF)));
        return images;
    }

    /**
     * Paints a 40 by 40 box with a background image value and returns the pixel at (1, 1), inside the first tile.
     * <p>
     * 배경 이미지 값을 가진 40x40 상자를 그려 첫 타일 안의 (1, 1) 화소를 반환한다.
     *
     * @param value the value of <code>background-image</code> / <code>background-image</code> 의 값
     * @param extra more declarations / 더 붙이는 선언
     * @return the RGB of the pixel / 화소의 RGB
     */
    private static int paintBackground(String value, String extra) {
        RenderedPage p = RenderTestSupport.page("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:" + value + ";" + extra + "'></div>", 100, 100, images());
        return RenderTestSupport.rgb(RenderTestSupport.paint(p, 100, 100), 1, 1);
    }

    /**
     * The selection: 1x is taken when it exists; otherwise the smallest resolution above 1; if all are below 1, the largest; the order of the candidates does not matter.
     * <p>
     * 선택: 1x 가 있으면 그것을, 없으면 1 보다 큰 가장 작은 해상도를, 모두 1 보다 작으면 가장 큰 것을 고른다. 후보의 순서는 상관없다.
     */
    public void testSelection() {
        Check.equal(0xFF0000, paintBackground("image-set(url(a.png) 1x, url(b.png) 2x)", ""), "1x is taken");
        Check.equal(0xFF0000, paintBackground("image-set(url(b.png) 2x, url(a.png) 1x)", ""), "whatever the order of the candidates");
        Check.equal(0x00FF00, paintBackground("image-set(url(a.png) 3x, url(b.png) 2x)", ""), "no 1x: the smallest resolution above 1 is 2x");
        Check.equal(0x0000FF, paintBackground("image-set(url(a.png) 0.5x, url(b.png) 0.75x, url(c.png) 0.9x)", ""), "all below 1x: the largest, 0.9x");
        Check.equal(0xFF0000, paintBackground("image-set(url(a.png))", ""), "a candidate without a resolution is 1x");
        Check.equal(0xFF0000, paintBackground("image-set(url(a.png) 96dpi, url(b.png) 192dpi)", ""), "96dpi is 1x");
        Check.equal(0x00FF00, paintBackground("image-set(url(a.png) 2dppx, url(b.png) 1dppx)", ""), "dppx is a resolution unit too: the 1dppx one is taken");
        Check.equal(0xFF0000, paintBackground("image-set(\"a.png\" 1x, \"b.png\" 2x)", ""), "strings are image names (relative to the style sheet)");
        Check.equal(0xFF0000, paintBackground("image-set(url(a.png) 1x type(\"image/png\"), url(b.png) 2x type(\"image/webp\"))", ""), "a type() hint does not disturb the selection");
    }

    /**
     * An image of 2x is half as large in CSS pixels: an 8 pixel image taken as 2x is 4 CSS pixels wide, so the tiles of a repeating background are 4 pixels apart; as 1x it is 8.
     * <p>
     * 2x 이미지는 CSS 픽셀로 절반 크기이다: 2x 로 고른 8 화소 이미지는 CSS 픽셀 4 이므로 반복 배경의 타일은 4 픽셀 간격이고, 1x 이면 8 이다.
     */
    public void testNaturalSizeUsesTheResolution() {
        RenderedPage p = RenderTestSupport.page("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:image-set(url(a.png) 2x)'></div>", 100, 100, images());
        BufferedImage img = RenderTestSupport.paint(p, 100, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 3, 3), "the red image covers 4 by 4 CSS pixels");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 6, 6), "and not 8 by 8");
        RenderedPage q = RenderTestSupport.page("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:image-set(url(a.png) 1x)'></div>", 100, 100, images());
        Check.equal(0xFF0000, RenderTestSupport.rgb(RenderTestSupport.paint(q, 100, 100), 6, 6), "as 1x the image covers 8 by 8");
    }

    /**
     * <code>border-image-source</code> takes an <code>image-set()</code> too.
     * <p>
     * <code>border-image-source</code> 도 <code>image-set()</code> 을 받는다.
     */
    public void testBorderImage() {
        RenderedPage p = RenderTestSupport.page("<div style='position:absolute;left:10px;top:10px;width:40px;height:40px;border:10px solid #000;border-image:image-set(url(b.png) 1x, url(a.png) 2x) 1 fill / 10px'></div>",
                100, 100, images());
        BufferedImage img = RenderTestSupport.paint(p, 100, 100);
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 15, 15), "the 1x candidate (green) fills the border image area");
        Check.equal(0x00FF00, RenderTestSupport.rgb(img, 40, 40), "and the middle (fill)");
    }

    /**
     * A candidate that is not available paints nothing (and does not fall back on the other candidate): the selection is made by the resolution only.
     * <p>
     * 쓸 수 없는 후보는 아무것도 그리지 않는다(다른 후보로 대신하지도 않는다): 선택은 해상도로만 이루어진다.
     */
    public void testUnavailableCandidate() {
        RenderedPage p = RenderTestSupport.page("<div style='width:40px;height:40px;background-repeat:no-repeat;background-image:image-set(url(missing.png) 1x, url(b.png) 2x)'></div>", 100,
                100, images());
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(RenderTestSupport.paint(p, 100, 100), 1, 1), "the selected 1x image is missing: nothing is painted");
    }
}
