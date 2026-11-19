package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ExifOrientation;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the style <code>image-orientation</code> (plan item P18-03; CSS Images 3, section 5.1): an image whose pixels were turned by the
 * EXIF orientation of its file is shown turned by default (<code>from-image</code>) and with the natural size after the turn, and
 * <code>image-orientation: none</code> shows it as stored, with the natural size of the stored picture, for the <code>img</code> element and
 * for a CSS background image; the property is inherited and rejects values other than the two keywords.
 * <p>
 * 스타일 <code>image-orientation</code> 시험(계획 P18-03; CSS Images 3, 5.1절): 파일의 EXIF 방향으로 픽셀을 돌린 이미지는 기본적으로
 * (<code>from-image</code>) 돌려서, 돌린 뒤의 자연 크기로 보이고, <code>image-orientation: none</code> 은 <code>img</code> 요소와 CSS 배경 이미지에서
 * 저장된 그대로, 저장된 그림의 자연 크기로 보여 준다. 이 속성은 상속되며 두 키워드 외의 값은 받지 않는다.
 */
public class ImageOrientationPaintTest {

    /**
     * Red, green, blue in the first row of the stored picture.
     * <p>
     * 저장된 그림의 첫 줄의 빨강, 초록, 파랑.
     */
    private static final int R = 0xFF0000;

    /**
     * The colours of the stored 3 x 2 picture are R G B / Y C M (yellow, cyan, magenta in the second row).
     * <p>
     * 저장된 3 x 2 그림의 색은 R G B / Y C M(둘째 줄은 노랑, 청록, 자홍)이다.
     */
    private static final int[] STORED = { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageOrientationPaintTest() {
    }

    /**
     * Creates the image sources: the picture of the stored pixels turned by orientation 6 (a quarter turn to the right, so that it is
     * 2 x 3 on the page), and the upright picture with the same pixels.
     * <p>
     * 이미지 소스를 만든다: 저장된 픽셀을 방향 6(오른쪽으로 4분의 1 회전이라 화면에서는 2 x 3)으로 돌린 그림과, 같은 픽셀의 정상 그림.
     *
     * @return the sources / 소스
     * @throws Exception on failure / 실패 시
     */
    private static RenderTestSupport.FixedImages images() throws Exception {
        DecodedImage stored = new DecodedImage(3, 2, STORED);
        return new RenderTestSupport.FixedImages().put("http://t.test/dir/turned.png", ImageResource.loaded(ExifOrientation.apply(stored, 6)))
                .put("http://t.test/dir/upright.png", ImageResource.loaded(stored));
    }

    /**
     * Returns the colour of a pixel of the box of the first <code>img</code>.
     * <p>
     * 첫 <code>img</code> 상자의 픽셀 색을 반환한다.
     *
     * @param p   the page / 페이지
     * @param img the painted page / 그려진 페이지
     * @param x   column in the box / 상자 안의 열
     * @param y   row in the box / 상자 안의 행
     * @return 0xRRGGBB / 0xRRGGBB
     */
    private static int at(RenderedPage p, BufferedImage img, int x, int y) {
        LayoutBox b = RenderTestSupport.firstBox(p, "img");
        return RenderTestSupport.rgb(img, (int) b.getX() + x, (int) Math.round(b.getY()) + y);
    }

    /**
     * By default the turned picture is shown turned, and the layout uses its size after the turn: 2 x 3 with the pixels Y R / C G / M B.
     * <p>
     * 기본적으로 돌린 그림은 돌려서 보이고 레이아웃은 돌린 뒤의 크기를 쓴다: 2 x 3, 픽셀은 Y R / C G / M B.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultShowsTheTurnedPicture() throws Exception {
        RenderedPage p = RenderTestSupport.page("<img src='turned.png'>", 40, 30, images());
        BufferedImage img = RenderTestSupport.paint(p, 40, 30);
        LayoutBox b = RenderTestSupport.firstBox(p, "img");
        Check.equal(2.0, b.getWidth(), "natural width after the turn");
        Check.equal(3.0, b.getHeight(), "natural height after the turn");
        Check.equal(0xFFFF00, at(p, img, 0, 0), "top left is yellow");
        Check.equal(R, at(p, img, 1, 0), "top right is red");
        Check.equal(0x00FFFF, at(p, img, 0, 1), "middle left is cyan");
        Check.equal(0x00FF00, at(p, img, 1, 1), "middle right is green");
        Check.equal(0xFF00FF, at(p, img, 0, 2), "bottom left is magenta");
        Check.equal(0x0000FF, at(p, img, 1, 2), "bottom right is blue");
        Check.equal("from-image", b.getStyle().keyword(Prop.IMAGE_ORIENTATION), "the initial value");
    }

    /**
     * <code>image-orientation: none</code> shows the stored picture: 3 x 2 with the pixels R G B / Y C M, and an explicit
     * <code>from-image</code> is the same as the default.
     * <p>
     * <code>image-orientation: none</code> 은 저장된 그림을 보여 준다: 3 x 2, 픽셀은 R G B / Y C M. 명시한 <code>from-image</code> 는 기본값과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoneShowsTheStoredPicture() throws Exception {
        RenderedPage p = RenderTestSupport.page("<img src='turned.png' style='image-orientation:none'>", 40, 30, images());
        BufferedImage img = RenderTestSupport.paint(p, 40, 30);
        LayoutBox b = RenderTestSupport.firstBox(p, "img");
        Check.equal(3.0, b.getWidth(), "stored width");
        Check.equal(2.0, b.getHeight(), "stored height");
        Check.equal(R, at(p, img, 0, 0), "top left is red");
        Check.equal(0x00FF00, at(p, img, 1, 0), "top middle is green");
        Check.equal(0x0000FF, at(p, img, 2, 0), "top right is blue");
        Check.equal(0xFFFF00, at(p, img, 0, 1), "bottom left is yellow");
        Check.equal(0x00FFFF, at(p, img, 1, 1), "bottom middle is cyan");
        Check.equal(0xFF00FF, at(p, img, 2, 1), "bottom right is magenta");
        Check.equal("none", b.getStyle().keyword(Prop.IMAGE_ORIENTATION), "the computed value");
        RenderedPage q = RenderTestSupport.page("<img src='turned.png' style='image-orientation:from-image'>", 40, 30, images());
        Check.equal(2.0, RenderTestSupport.firstBox(q, "img").getWidth(), "from-image is the default");
    }

    /**
     * The property is inherited, so an element can set it for its descendants, and an <code>img</code> of an upright picture is not changed
     * by it; width and height attributes still scale the chosen picture.
     * <p>
     * 속성은 상속되므로 요소가 자손에게 지정할 수 있고, 정상 그림의 <code>img</code> 는 영향을 받지 않는다. width/height 속성은 고른 그림을 그대로 늘린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInheritanceAndUprightPictures() throws Exception {
        RenderedPage p = RenderTestSupport.page("<div style='image-orientation:none'><img src='turned.png'></div>", 40, 30, images());
        Check.equal(3.0, RenderTestSupport.firstBox(p, "img").getWidth(), "inherited none: stored width");
        RenderedPage upright = RenderTestSupport.page("<img src='upright.png' style='image-orientation:none'>", 40, 30, images());
        Check.equal(3.0, RenderTestSupport.firstBox(upright, "img").getWidth(), "an upright picture is unchanged by none: width");
        Check.equal(2.0, RenderTestSupport.firstBox(upright, "img").getHeight(), "an upright picture is unchanged by none: height");
        // 10 배로 늘려도 각 칸의 가운데는 그 칸의 색이다(보간은 가장자리에서만 일어난다).
        RenderedPage scaled = RenderTestSupport.page("<img src='turned.png' style='image-orientation:none' width='30' height='20'>", 40, 30, images());
        BufferedImage img = RenderTestSupport.paint(scaled, 40, 30);
        Check.isTrue(RenderTestSupport.near(R, at(scaled, img, 5, 5), 40), "scaled stored picture: red in the top left cell");
        Check.isTrue(RenderTestSupport.near(0x0000FF, at(scaled, img, 25, 5), 40), "scaled stored picture: blue in the top right cell");
        Check.isTrue(RenderTestSupport.near(0xFFFF00, at(scaled, img, 5, 15), 40), "scaled stored picture: yellow in the bottom left cell");
    }

    /**
     * Only <code>from-image</code> and <code>none</code> are accepted; the deprecated optional angle and flip values are ignored like any
     * invalid value.
     * <p>
     * <code>from-image</code> 와 <code>none</code> 만 받는다. 폐기 예정인 선택적 각도/flip 값은 잘못된 값처럼 무시된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOnlyTheTwoKeywordsAreAccepted() throws Exception {
        String[] invalid = { "90deg", "flip", "none flip", "from-image 90deg", "auto", "1" };
        // 잘못된 값은 모두 선언이 무시되어 초기값 from-image 로 남는다.
        for (int i = 0; i < invalid.length; i++) {
            RenderedPage p = RenderTestSupport.page("<img src='turned.png' style='image-orientation:" + invalid[i] + "'>", 40, 30, images());
            Check.equal("from-image", RenderTestSupport.firstBox(p, "img").getStyle().keyword(Prop.IMAGE_ORIENTATION), "invalid value " + invalid[i]);
            Check.equal(2.0, RenderTestSupport.firstBox(p, "img").getWidth(), "invalid value " + invalid[i] + ": width");
        }
    }

    /**
     * A CSS background image follows the style too: by default the turned picture, with <code>none</code> the stored one.
     * <p>
     * CSS 배경 이미지도 스타일을 따른다: 기본은 돌린 그림, <code>none</code> 이면 저장된 그림.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackgroundImages() throws Exception {
        String box = "<div id='b' style='width:10px;height:10px;background:url(turned.png) no-repeat 0 0;";
        RenderedPage turned = RenderTestSupport.page(box + "'></div>", 40, 30, images());
        BufferedImage a = RenderTestSupport.paint(turned, 40, 30);
        LayoutBox ba = RenderTestSupport.box(turned, "b");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(a, (int) ba.getX(), (int) Math.round(ba.getY())), "default: yellow at the top left");
        Check.equal(R, RenderTestSupport.rgb(a, (int) ba.getX() + 1, (int) Math.round(ba.getY())), "default: red next to it");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(a, (int) ba.getX() + 2, (int) Math.round(ba.getY())), "default: the picture is only 2 pixels wide");
        RenderedPage stored = RenderTestSupport.page(box + "image-orientation:none'></div>", 40, 30, images());
        BufferedImage b = RenderTestSupport.paint(stored, 40, 30);
        LayoutBox bb = RenderTestSupport.box(stored, "b");
        Check.equal(R, RenderTestSupport.rgb(b, (int) bb.getX(), (int) Math.round(bb.getY())), "none: red at the top left");
        Check.equal(0x0000FF, RenderTestSupport.rgb(b, (int) bb.getX() + 2, (int) Math.round(bb.getY())), "none: blue three pixels wide");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(b, (int) bb.getX(), (int) Math.round(bb.getY()) + 1), "none: yellow below the red");
    }
}
