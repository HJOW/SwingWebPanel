package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>border-image</code> (plan item P10-08, CSS Backgrounds and Borders Level 3, https://www.w3.org/TR/css-backgrounds-3/#border-images, section 5): the nine
 * parts of the source (an image or a gradient) cut by <code>border-image-slice</code>, drawn in place of the border styles into the border image area with
 * <code>border-image-width</code> and <code>border-image-outset</code>; the middle part only with <code>fill</code>; the repeat modes of the edges; the fallback to the
 * border styles when the source cannot be displayed; the independence of <code>border-radius</code>; the clip of an ancestor; the properties and the shorthand.
 * <p>
 * <code>border-image</code> 시험(계획 P10-08, CSS Backgrounds and Borders Level 3 5 절): 원본(이미지나 그라디언트)을 <code>border-image-slice</code> 로 자른 아홉 조각을
 * border 스타일 대신 <code>border-image-width</code> 와 <code>border-image-outset</code> 으로 정한 border 이미지 영역에 그리고, 가운데는 <code>fill</code> 일 때만 그리며,
 * 가장자리의 반복 방식, 원본을 표시할 수 없을 때 border 스타일로 돌아가는 것, <code>border-radius</code> 와 무관함, 조상의 클립, 속성과 단축 속성을 확인한다.
 */
public class BorderImagePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BorderImagePaintTest() {
    }

    /** The colors of the 3 by 3 test image: corners red, top green, right yellow, bottom magenta, left cyan, middle blue. 3x3 시험 이미지의 색: 모서리 빨강, 위 초록, 오른쪽 노랑, 아래 자홍, 왼쪽 청록, 가운데 파랑. */
    private static final int[] FRAME = {
        0xFFFF0000, 0xFF00FF00, 0xFFFF0000,
        0xFF00FFFF, 0xFF0000FF, 0xFFFFFF00,
        0xFFFF0000, 0xFFFF00FF, 0xFFFF0000};

    /** A box with a 10 px solid black border: a 100 by 60 border box at (20, 20). 10px 검은 실선 border 가 있는 상자: (20, 20) 의 100x60 border box. */
    private static final String BOX = "position:absolute;left:20px;top:20px;width:80px;height:40px;border:10px solid #000;box-sizing:content-box;";

    /**
     * Paints the box with the given extra style, the frame image being available as <code>frame.png</code>.
     * <p>
     * 주어진 스타일을 더한 상자를 그린다. 틀 이미지는 <code>frame.png</code> 로 쓸 수 있다.
     *
     * @param style the style that is added to the box / 상자에 더하는 스타일
     * @return the image / 이미지
     */
    private static BufferedImage paint(String style) {
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/frame.png",
                ImageResource.loaded(new DecodedImage(3, 3, FRAME.clone())));
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + style + "'></div>", 300, 200, images);
        return RenderTestSupport.paint(p, 300, 200);
    }

    /**
     * Tells whether a pixel has a color (within a small tolerance).
     * <p>
     * 화소가 어떤 색인지(작은 허용 오차 안에서) 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @param rgb the color / 색
     * @return true if it has the color / 그 색이면 true
     */
    private static boolean is(BufferedImage img, int x, int y, int rgb) {
        return RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), rgb, 30);
    }

    /**
     * The image is painted in place of the border styles: the corners, the four edges and (with <code>fill</code>) the middle show the pixels of the source, and the
     * border color is not seen. Slices that are one pixel of a three pixel image are scaled to the regions of the width 10 px.
     * <p>
     * 이미지를 border 스타일 대신 그린다: 모서리, 네 가장자리, (<code>fill</code> 이면) 가운데가 원본의 화소를 보여 주고 border 색은 보이지 않는다. 3화소 이미지의 한 화소 조각은
     * 너비 10px 인 영역으로 늘어난다.
     */
    public void testReplacesTheBorderStyles() {
        BufferedImage img = paint("border-image-source:url(frame.png);border-image-slice:1 fill;");
        Check.isTrue(is(img, 25, 25, 0xFF0000), "top left corner");
        Check.isTrue(is(img, 115, 25, 0xFF0000), "top right corner");
        Check.isTrue(is(img, 25, 75, 0xFF0000), "bottom left corner");
        Check.isTrue(is(img, 115, 75, 0xFF0000), "bottom right corner");
        Check.isTrue(is(img, 60, 24, 0x00FF00), "top edge");
        Check.isTrue(is(img, 115, 50, 0xFFFF00), "right edge");
        Check.isTrue(is(img, 60, 75, 0xFF00FF), "bottom edge");
        Check.isTrue(is(img, 24, 50, 0x00FFFF), "left edge");
        Check.isTrue(is(img, 60, 50, 0x0000FF), "the middle with fill");
        Check.isTrue(is(img, 10, 10, 0xFFFFFF), "outside the box nothing is painted");
        BufferedImage none = paint("border-image-slice:1 fill;");
        Check.isTrue(is(none, 25, 50, 0x000000), "without a source the black border style is painted");
        Check.isTrue(is(none, 60, 50, 0xFFFFFF), "and the middle is not");
    }

    /**
     * The middle part is dropped unless <code>fill</code> says so, and the slices of a part never blend with the pixels of their neighbors.
     * <p>
     * 가운데 조각은 <code>fill</code> 이 없으면 버려지고, 조각은 이웃 화소와 섞이지 않는다.
     */
    public void testMiddleAndSharpEdges() {
        BufferedImage img = paint("border-image-source:url(frame.png);border-image-slice:1;");
        Check.isTrue(is(img, 60, 50, 0xFFFFFF), "the middle is not drawn without fill");
        Check.isTrue(is(img, 60, 24, 0x00FF00), "the edges are");
        Check.isTrue(is(img, 29, 25, 0xFF0000), "the last column of the corner region is the pure corner color (no blending with the edge)");
        Check.isTrue(is(img, 30, 25, 0x00FF00), "and the first column of the edge region is the pure edge color");
        Check.isTrue(is(img, 25, 29, 0xFF0000), "the last row of the corner");
        Check.isTrue(is(img, 25, 30, 0x00FFFF), "the first row of the left edge");
    }

    /**
     * An image that cannot be displayed (missing, not arrived) leaves the border styles in force (section 5.1).
     * <p>
     * 표시할 수 없는 이미지(없거나 아직 도착하지 않음)는 border 스타일이 그대로 적용되게 둔다(5.1 절).
     */
    public void testFallbackToBorderStyles() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + "border-image-source:url(missing.png);border-image-slice:1 fill'></div>", 300, 200,
                new RenderTestSupport.FixedImages());
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.isTrue(is(img, 25, 50, 0x000000), "the border style is painted when the image is missing");
        Check.isTrue(is(img, 60, 50, 0xFFFFFF), "and the middle shows nothing");
    }

    /**
     * The sizes: <code>border-image-width</code> sets the regions (a number multiplies the border width) and <code>border-image-outset</code> grows the area beyond the
     * border box without changing the layout.
     * <p>
     * 크기: <code>border-image-width</code> 가 영역을 정하고(숫자는 border 너비에 곱한다) <code>border-image-outset</code> 은 레이아웃을 바꾸지 않고 영역을 border box 밖으로 넓힌다.
     */
    public void testWidthAndOutset() {
        BufferedImage narrow = paint("border-image-source:url(frame.png);border-image-slice:1;border-image-width:5px;");
        Check.isTrue(is(narrow, 22, 22, 0xFF0000), "the corner region is 5 by 5");
        Check.isTrue(is(narrow, 28, 28, 0xFFFFFF), "and nothing is painted behind it (the border style is replaced)");
        Check.isTrue(is(narrow, 60, 22, 0x00FF00), "the top edge is 5 px thick");
        Check.isTrue(is(narrow, 60, 28, 0xFFFFFF), "and no thicker");
        BufferedImage numbers = paint("border-image-source:url(frame.png);border-image-slice:1;border-image-width:2;");
        Check.isTrue(is(numbers, 35, 35, 0xFF0000), "a number multiplies the border width: the corner is 20 by 20");
        Check.isTrue(is(numbers, 45, 22, 0x00FF00), "the top edge starts at x 40");
        BufferedImage outset = paint("border-image-source:url(frame.png);border-image-slice:1;border-image-outset:10px;");
        Check.isTrue(is(outset, 15, 15, 0xFF0000), "the area starts 10 px outside the border box: the corner is at (10, 10)");
        Check.isTrue(is(outset, 25, 25, 0xFFFFFF), "and the border box itself is empty");
        Check.isTrue(is(outset, 60, 14, 0x00FF00), "the top edge lies outside");
        BufferedImage multiple = paint("border-image-source:url(frame.png);border-image-slice:1;border-image-outset:1;");
        Check.isTrue(is(multiple, 15, 15, 0xFF0000), "a number multiplies the border width (1 x 10 px)");
        BufferedImage big = paint("border-image-source:url(frame.png);border-image-slice:1;border-image-width:60px;");
        // 영역은 높이 60 이므로 위아래 60 + 60 은 30 + 30 으로 줄고 가운데 영역은 높이가 0 이 된다. 좌우도 같은 비율로 30 + 30 이 되어 가운데 너비는 40 이다.
        Check.isTrue(is(big, 60, 30, 0x00FF00), "regions that overlap are scaled down together: the top edge is 30 px thick (y 20 to 50)");
        Check.isTrue(is(big, 60, 70, 0xFF00FF), "the bottom edge is 30 px thick (y 50 to 80)");
        Check.isTrue(is(big, 45, 45, 0xFF0000), "the corner is 30 by 30");
        Check.isTrue(is(big, 25, 49, 0xFF0000), "and the left edge region has no height");
    }

    /**
     * The source cuts: a number is a pixel of the image, a percentage is relative to the image, and a corner larger than the image is the whole image.
     * <p>
     * 자르는 값: 숫자는 이미지의 화소이고 백분율은 이미지 기준이며 이미지보다 큰 조각은 이미지 전체이다.
     */
    public void testSliceValues() {
        BufferedImage pct = paint("border-image-source:url(frame.png);border-image-slice:34%;border-image-width:10px;");
        Check.isTrue(is(pct, 25, 25, 0xFF0000), "34 % of a 3 px image is one pixel");
        Check.isTrue(is(pct, 60, 24, 0x00FF00), "the edge");
        BufferedImage different = paint("border-image-source:url(frame.png);border-image-slice:1 1 1 0;border-image-width:10px;border-image-repeat:stretch;");
        Check.isTrue(is(different, 115, 50, 0xFFFF00), "the right edge is the pixel of column 2");
        BufferedImage overlap = paint("border-image-source:url(frame.png);border-image-slice:2 fill;border-image-width:10px;");
        Check.isTrue(is(overlap, 60, 50, 0xFFFFFF), "slices of the left and the right that cover the whole width leave the top, the bottom and the middle empty");
    }

    /**
     * The repeat modes of the edges (section 5.5 and 5.6): <code>stretch</code> stretches the edge slice, <code>repeat</code> tiles it with the first tile centered,
     * <code>round</code> makes the number of tiles whole, and <code>space</code> puts whole tiles with the rest of the room equal before, between and after them.
     * <p>
     * 가장자리의 반복 방식(5.5, 5.6 절): <code>stretch</code> 는 가장자리 조각을 늘이고, <code>repeat</code> 는 첫 타일을 가운데에 놓고 되풀이하며, <code>round</code> 는 타일 수를
     * 정수로 맞추고, <code>space</code> 는 정수 개의 타일을 놓고 남는 공간을 앞, 사이, 뒤에 똑같이 나눈다.
     */
    public void testRepeatModes() {
        // 4x4 원본에서 윗가장자리 조각은 (1,0) 과 (2,0) 두 화소이고 (1,0) 만 빨갛다: 높이 12px 로 그리면 타일은 24px 이고 앞 12px 만 빨갛다.
        BufferedImage stretch = edge("stretch");
        Check.isTrue(is(stretch, 50, 26, 0xFF0000), "stretch: one tile of 80 px, the first half shows (x 30 to 70)");
        Check.isTrue(is(stretch, 90, 26, 0xFFFFFF), "stretch: the second half is transparent");
        BufferedImage repeat = edge("repeat");
        Check.isTrue(is(repeat, 40, 26, 0xFF0000), "repeat: tiles of 24 px centered (five of them, from x 10): the first half of the tile at 34 shows");
        Check.isTrue(is(repeat, 52, 26, 0xFFFFFF), "repeat: and the second half of it is transparent");
        Check.isTrue(is(repeat, 64, 26, 0xFF0000), "repeat: the next tile");
        Check.isTrue(is(repeat, 76, 26, 0xFFFFFF), "repeat: and its end");
        Check.isTrue(is(repeat, 88, 26, 0xFF0000), "repeat: the tile at 82");
        Check.isTrue(is(repeat, 108, 26, 0xFF0000), "repeat: the last tile is clipped by the region (x 106 to 110)");
        BufferedImage round = edge("round");
        Check.isTrue(is(round, 36, 26, 0xFF0000) && is(round, 50, 26, 0xFFFFFF) && is(round, 63, 26, 0xFF0000) && is(round, 77, 26, 0xFFFFFF) && is(round, 90, 26, 0xFF0000)
                && is(round, 103, 26, 0xFFFFFF), "round: three tiles of 26.7 px fill the region exactly");
        BufferedImage space = edge("space");
        Check.isTrue(is(space, 36, 26, 0xFF0000) && is(space, 50, 26, 0xFFFFFF) && is(space, 62, 26, 0xFF0000) && is(space, 76, 26, 0xFFFFFF) && is(space, 88, 26, 0xFF0000)
                && is(space, 102, 26, 0xFFFFFF), "space: three tiles of 24 px at x 32, 58 and 84");
        Check.isTrue(is(space, 31, 26, 0xFFFFFF) && is(space, 57, 26, 0xFFFFFF), "space: with a gap of 2 px before and between them");
        BufferedImage tooBig = edge("space", "border-image-width:50px 10px 0 10px;");
        Check.isTrue(is(tooBig, 60, 30, 0xFFFFFF), "space: a tile that does not fit once leaves the edge region empty");
    }

    /**
     * Paints the box whose source is a 4 by 4 image with one red pixel (1, 0) in the top edge slice, so that the top edge region shows the tiles of the repeat mode.
     * <p>
     * 원본이 4x4 이미지이고 윗가장자리 조각에 빨간 화소 (1, 0) 하나가 있어서 윗가장자리 영역이 반복 방식의 타일을 보여 주는 상자를 그린다.
     *
     * @param repeat the repeat mode / 반복 방식
     * @return the image / 이미지
     */
    private static BufferedImage edge(String repeat) {
        return edge(repeat, "border-image-width:12px 10px 0 10px;");
    }

    /**
     * Paints the box of {@link #edge(String)} with other widths.
     * <p>
     * {@link #edge(String)} 의 상자를 다른 너비로 그린다.
     *
     * @param repeat the repeat mode / 반복 방식
     * @param width  the declaration of the width / 너비 선언
     * @return the image / 이미지
     */
    private static BufferedImage edge(String repeat, String width) {
        int[] px = new int[16];
        px[1] = 0xFFFF0000;
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/e.png", ImageResource.loaded(new DecodedImage(4, 4, px)));
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + "border-image-source:url(e.png);border-image-slice:1;" + width + "border-image-repeat:" + repeat
                + " stretch'></div>", 300, 200, images);
        return RenderTestSupport.paint(p, 300, 200);
    }

    /**
     * The side edges tile along the vertical axis with the second keyword, and the middle part is scaled by the factors of the top and the left edge before it tiles.
     * <p>
     * 왼쪽과 오른쪽 가장자리는 둘째 키워드로 세로축을 따라 놓이고, 가운데 조각은 위와 왼쪽 가장자리의 배율로 크기를 맞춘 뒤 놓인다.
     */
    public void testVerticalAndMiddleTiling() {
        // 4x4 원본의 왼쪽 가장자리 조각은 (0,1) 과 (0,2) 두 화소이고 (0,1) 만 파랗다.
        int[] px = new int[16];
        px[4] = 0xFF0000FF;
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/s.png", ImageResource.loaded(new DecodedImage(4, 4, px)));
        RenderedPage p = RenderTestSupport.page("<div id=a style='" + BOX + "border-image-source:url(s.png);border-image-slice:1;border-image-width:10px;"
                + "border-image-repeat:stretch repeat'></div>", 300, 200, images);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        // 왼쪽 영역은 x 20~30, y 30~70(40px). 타일은 너비 10px 이고 높이 2 x 10 = 20px 이다. 첫 타일을 영역 가운데에 놓으면 타일은 y 20, 40, 60 에서 시작하고
        // 타일의 위쪽 절반(10px)만 파랗다.
        Check.isTrue(is(img, 25, 45, 0x0000FF), "the left edge is tiled vertically: the upper half of the tile at y 40 is blue");
        Check.isTrue(is(img, 25, 55, 0xFFFFFF), "the lower half of that tile is transparent");
        Check.isTrue(is(img, 25, 65, 0x0000FF), "the next tile (y 60) is blue in its upper half");
        Check.isTrue(is(img, 25, 35, 0xFFFFFF), "the lower half of the tile at y 20 is transparent");
        Check.isTrue(is(img, 60, 24, 0xFFFFFF), "the top edge is the transparent pixel of the source");
    }

    /**
     * The middle part is scaled by the factors of the top and the left edge (here 10 and 10: a 2 by 2 slice becomes a 20 by 20 tile) before it is stretched or tiled
     * (section 5.6).
     * <p>
     * 가운데 조각은 늘이거나 놓기 전에 위와 왼쪽 가장자리의 배율(여기서는 10 과 10: 2x2 조각이 20x20 타일)로 크기를 맞춘다(5.6 절).
     */
    public void testMiddleTiling() {
        // 4x4 원본에서 가운데 조각은 (1,1)~(2,2) 2x2 이고 왼쪽 위 화소 (1,1) 만 초록이다.
        int[] px = new int[16];
        px[5] = 0xFF00FF00;
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/m.png", ImageResource.loaded(new DecodedImage(4, 4, px)));
        String base = BOX + "border-image-source:url(m.png);border-image-slice:1 fill;border-image-width:10px;";
        BufferedImage stretch = RenderTestSupport.paint(RenderTestSupport.page("<div style='" + base + "'></div>", 300, 200, images), 300, 200);
        Check.isTrue(is(stretch, 35, 35, 0x00FF00), "stretch: the whole middle region (x 30 to 110, y 30 to 70) is the 2 by 2 slice: the upper left quarter is green");
        Check.isTrue(is(stretch, 105, 65, 0xFFFFFF), "and the lower right quarter is transparent");
        BufferedImage repeat = RenderTestSupport.paint(RenderTestSupport.page("<div style='" + base + "border-image-repeat:repeat'></div>", 300, 200, images), 300, 200);
        // 타일은 20x20 이고 가운데 맞춤이므로 x 는 20, 40, 60, 80, 100 y 는 20, 40, 60 에서 시작하며 타일의 왼쪽 위 10x10 만 초록이다.
        Check.isTrue(is(repeat, 45, 45, 0x00FF00), "repeat: the tile at (40, 40) is green in its upper left quarter");
        Check.isTrue(is(repeat, 55, 55, 0xFFFFFF), "and transparent in the rest");
        Check.isTrue(is(repeat, 65, 65, 0x00FF00), "the tile at (60, 60)");
        Check.isTrue(is(repeat, 85, 45, 0x00FF00), "the tile at (80, 40)");
        Check.isTrue(is(repeat, 75, 45, 0xFFFFFF), "the lower right three quarters of a tile are transparent");
    }

    /**
     * A gradient is a source too; it is sized to the border image area, so its slices are in pixels of that area.
     * <p>
     * 그라디언트도 원본이다. border 이미지 영역 크기로 그려지므로 조각은 그 영역의 화소 단위이다.
     */
    public void testGradientSource() {
        BufferedImage img = paint("border-image-source:linear-gradient(to right,#f00,#00f);border-image-slice:10 fill;border-image-width:10px;");
        Check.isTrue(RenderTestSupport.rgb(img, 25, 25) >> 16 > 200, "the top left corner is the red start of the gradient");
        Check.isTrue((RenderTestSupport.rgb(img, 115, 25) & 0xFF) > 200, "the top right corner is the blue end of the gradient");
        Check.isTrue(is(img, 10, 10, 0xFFFFFF), "outside the area nothing is painted");
        BufferedImage ring = paint("border-image-source:linear-gradient(#0f0,#0f0);border-image-slice:10;");
        Check.isTrue(is(ring, 60, 24, 0x00FF00), "the top edge of a solid gradient (auto width of a gradient is the border width)");
        Check.isTrue(is(ring, 60, 50, 0xFFFFFF), "and no middle without fill");
    }

    /**
     * <code>border-radius</code> does not affect the border image (section 4.3), but it clips the background.
     * <p>
     * <code>border-radius</code> 는 border 이미지에 영향을 주지 않지만(4.3 절) 배경은 자른다.
     */
    public void testBorderRadiusDoesNotClipTheImage() {
        BufferedImage img = paint("border-radius:30px;background:#0f0;border-image-source:url(frame.png);border-image-slice:1;border-image-width:10px;");
        Check.isTrue(is(img, 22, 22, 0xFF0000), "the corner of the image is painted into the square corner of the border box");
        Check.isTrue(is(img, 60, 50, 0x00FF00), "the background is inside the rounded border edge");
        Check.isTrue(is(img, 31, 31, 0x00FF00), "and it is clipped by the curve: the background shows at the inner point, not at the square corner");
    }

    /**
     * The image is painted inside the clip of an ancestor: the part of the area outside an <code>overflow: hidden</code> parent is not painted.
     * <p>
     * 이미지는 조상의 클립 안에서 그려진다: <code>overflow: hidden</code> 부모 밖의 영역은 그려지지 않는다.
     */
    public void testAncestorClip() {
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/frame.png", ImageResource.loaded(new DecodedImage(3, 3, FRAME.clone())));
        RenderedPage p = RenderTestSupport.page("<div style='position:absolute;left:0;top:0;width:70px;height:200px;overflow:hidden'>"
                + "<div id=a style='" + BOX + "border-image-source:url(frame.png);border-image-slice:1;'></div></div>", 300, 200, images);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.isTrue(is(img, 60, 24, 0x00FF00), "inside the parent the top edge is painted");
        Check.isTrue(is(img, 100, 24, 0xFFFFFF), "outside the parent it is clipped");
        Check.isTrue(is(img, 115, 25, 0xFFFFFF), "also the corner");
    }

    /**
     * The properties: the grammar of the five longhands and the shorthand with the slash separated width and outset; the <code>border</code> shorthand resets them.
     * <p>
     * 속성: 개별 속성 다섯 개의 문법과 슬래시로 이어지는 너비와 outset 이 붙은 단축 속성. <code>border</code> 단축 속성은 이를 초기화한다.
     */
    public void testProperties() {
        PropertyRegistry reg = PropertyRegistry.getInstance();
        Check.equal("10 20% fill", parse(reg, "border-image-slice", "10 20% fill", "border-image-slice"), "slice with fill");
        Check.equal("INVALID", parse(reg, "border-image-slice", "-1", "border-image-slice"), "a negative slice");
        Check.equal("1 auto 5px", parse(reg, "border-image-width", "1 auto 5px", "border-image-width"), "widths");
        Check.equal("INVALID", parse(reg, "border-image-outset", "10%", "border-image-outset"), "an outset cannot be a percentage");
        Check.equal("repeat round", parse(reg, "border-image-repeat", "repeat round", "border-image-repeat"), "repeat modes");
        String all = "url(a.png) 27 / 10px / 5px round stretch";
        Check.equal("27", parse(reg, "border-image", all, "border-image-slice"), "the slice of the shorthand");
        Check.equal("10px", parse(reg, "border-image", all, "border-image-width"), "the width after the first slash");
        Check.equal("5px", parse(reg, "border-image", all, "border-image-outset"), "the outset after the second slash");
        Check.equal("round stretch", parse(reg, "border-image", all, "border-image-repeat"), "the repeat");
        Check.equal("url(\"a.png\")", parse(reg, "border-image", all, "border-image-source"), "the source");
        Check.equal("initial", parse(reg, "border-image", "url(a.png) 5", "border-image-repeat"), "an unmentioned longhand is reset");
        Check.equal("none", parse(reg, "border-image", "none", "border-image-source"), "none");
        Check.equal("INVALID", parse(reg, "border-image", "url(a.png) 5 / 3 /", "border-image-slice"), "a slash without an outset after it");
        Check.equal("UNKNOWN", parse(reg, "border-image-mode", "alpha", "border-image-slice"), "there is no mode");
        Check.equal("initial", parse(reg, "border", "1px solid red", "border-image-source"), "the border shorthand resets the source");
        Check.equal("initial", parse(reg, "border", "1px solid red", "border-image-slice"), "and the slice");
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
}
