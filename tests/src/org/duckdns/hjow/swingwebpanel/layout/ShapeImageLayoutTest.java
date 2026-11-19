package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the image shapes of <code>shape-outside</code> (plan item P10-08-d, CSS Shapes Module Level 1 section 4 "Shapes from Image" and 6.2
 * <code>shape-image-threshold</code>, https://www.w3.org/TR/css-shapes-1/#shapes-from-image): the float area is where the alpha of the image is greater than
 * the threshold, the image is stretched over the content box of the float, <code>shape-margin</code> grows the area, an image without a pixel above the
 * threshold gives an empty area, and an image that is not available leaves the margin box in force. The images are alpha masks that a test source gives, so
 * the arithmetic of the shape is checked without a decoder.
 * <p>
 * <code>shape-outside</code> 의 이미지 모양 시험(계획 P10-08-d, CSS Shapes Module Level 1 4 절 "Shapes from Image"와 6.2 <code>shape-image-threshold</code>):
 * float 영역은 이미지의 알파가 기준보다 큰 곳이고, 이미지는 float 의 콘텐츠 상자에 맞게 늘어나며, <code>shape-margin</code> 은 영역을 넓히고, 기준보다 큰 픽셀이 없는
 * 이미지는 빈 영역이 되고, 쓸 수 없는 이미지는 margin box 가 그대로 쓰인다. 이미지는 시험 공급자가 주는 알파 마스크이므로 디코더 없이 모양의 계산을 확인한다.
 */
public class ShapeImageLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 1.0;

    /** A line height of 10px. 줄 높이 10px. */
    private static final String LH = "<style>body{line-height:10px}</style>";

    /**
     * A source of masks for the tests: it gives one mask for every address and records the addresses that were asked for.
     * <p>
     * 시험용 마스크 공급자: 모든 주소에 마스크 하나를 주고 요청받은 주소를 기록한다.
     */
    private static final class Masks implements ReplacedResolver, ShapeImageSource {
        /** The mask that is given, or null for an image that is not available. 주는 마스크. 쓸 수 없는 이미지이면 null. */
        final ShapeImage image;
        /** The addresses that were asked for. 요청받은 주소. */
        final List<String> urls = new ArrayList<String>();
        /** The size of the last gradient that was asked for, {width, height}. 마지막으로 요청받은 그라디언트의 크기. */
        final int[] gradientSize = new int[2];

        /**
         * Creates the source.
         * <p>
         * 공급자를 만든다.
         *
         * @param image the mask or null / 마스크 또는 null
         */
        Masks(ShapeImage image) {
            this.image = image;
        }

        /**
         * Gives no content: the elements of the tests are not replaced.
         * <p>
         * 콘텐츠를 주지 않는다: 시험의 요소는 치환 요소가 아니다.
         *
         * @param element the element / 요소
         * @return null / null
         */
        public ReplacedContent getReplacedContent(Element element) {
            return null;
        }

        /**
         * Gives the mask and records the address.
         * <p>
         * 마스크를 주고 주소를 기록한다.
         *
         * @param absoluteUrl the address / 주소
         * @return the mask / 마스크
         */
        public ShapeImage getShapeImage(String absoluteUrl) {
            urls.add(absoluteUrl);
            return image;
        }

        /**
         * Gives a mask whose alpha falls from 255 at the left edge to 0 at the right edge (a horizontal gradient) and records the size.
         * <p>
         * 왼쪽 가장자리의 255 에서 오른쪽 가장자리의 0 까지 알파가 줄어드는 마스크(가로 그라디언트)를 주고 크기를 기록한다.
         *
         * @param gradient     the gradient / 그라디언트
         * @param currentColor the color of the text / 글자색
         * @param width        the width / 너비
         * @param height       the height / 높이
         * @return the mask / 마스크
         */
        public ShapeImage getShapeGradient(CssFunctionValue gradient, int currentColor, int width, int height) {
            gradientSize[0] = width;
            gradientSize[1] = height;
            return ramp(width, height);
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ShapeImageLayoutTest() {
    }

    /**
     * Builds a mask whose columns left of a column are opaque and the others transparent.
     * <p>
     * 어떤 열보다 왼쪽의 열은 불투명하고 나머지는 투명한 마스크를 만든다.
     *
     * @param w       the width / 너비
     * @param h       the height / 높이
     * @param opaqueW the number of opaque columns from the left / 왼쪽부터 불투명한 열 수
     * @return the mask / 마스크
     */
    private static ShapeImage columns(int w, int h, int opaqueW) {
        byte[] a = new byte[w * h];
        // 왼쪽 열들을 불투명(255)으로 채운다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < opaqueW; x++) {
                a[y * w + x] = (byte) 255;
            }
        }
        return new ShapeImage(w, h, a);
    }

    /**
     * Builds a mask whose alpha falls linearly from 255 at the left column to 0 at the right column.
     * <p>
     * 알파가 왼쪽 열의 255 에서 오른쪽 열의 0 까지 선형으로 줄어드는 마스크를 만든다.
     *
     * @param w the width / 너비
     * @param h the height / 높이
     * @return the mask / 마스크
     */
    private static ShapeImage ramp(int w, int h) {
        byte[] a = new byte[w * h];
        // 열마다 알파를 정한다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                a[y * w + x] = (byte) Math.round(255.0 - x * 255.0 / Math.max(1, w - 1));
            }
        }
        return new ShapeImage(w, h, a);
    }

    /**
     * Returns the lines of a paragraph of many short words beside a 100 by 100 float with a style.
     * <p>
     * 스타일이 있는 100x100 float 옆에 있는, 짧은 단어가 많은 문단의 줄을 반환한다.
     *
     * @param source     the source of the masks / 마스크 공급자
     * @param floatStyle the style of the float / float 의 스타일
     * @param side       left or right / left 나 right
     * @return the lines / 줄
     */
    private static List<LayoutBox> lines(Masks source, String floatStyle, String side) {
        StringBuilder words = new StringBuilder();
        // 줄마다 여러 단어가 들어가도록 짧은 단어를 많이 만든다.
        for (int i = 0; i < 400; i++) {
            words.append("ab ");
        }
        LayoutResult r = LayoutTestSupport.layout(source, LH + "<div style='float:" + side + ";width:100px;height:100px;" + floatStyle + "'></div><p id=p style='margin:0'>"
                + words + "</p>", 400);
        return LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
    }

    /**
     * Where the image is opaque, the text is kept away: a float whose image is opaque in its left half wraps the lines at the edge of that half.
     * <p>
     * 이미지가 불투명한 곳에서는 텍스트가 비켜 간다: 이미지가 왼쪽 절반에서만 불투명한 float 는 그 절반의 가장자리에서 줄을 감싼다.
     */
    public void testOpaquePartKeepsTheText() {
        Masks m = new Masks(columns(100, 100, 50));
        List<LayoutBox> l = lines(m, "shape-outside:url(a.png)", "left");
        Check.near(50, l.get(0).getX(), T, "the first line starts at the right edge of the opaque half (50)");
        Check.near(50, lineBelow(l, 40).getX(), T, "a line in the middle of the float starts at 50");
        Check.near(0, lineBelow(l, 100).getX(), T, "below the float the lines start at the left edge");
        Check.isTrue(m.urls.size() > 0, "the source was asked for the image");
        Check.equal("a.png", m.urls.get(0), "the address of the image");
    }

    /**
     * Returns the first line whose top is at or below a height.
     * <p>
     * 위쪽이 주어진 높이 이상인 첫 줄을 반환한다.
     *
     * @param lines the lines / 줄
     * @param y     the height / 높이
     * @return the line / 줄
     */
    private static LayoutBox lineBelow(List<LayoutBox> lines, double y) {
        // 위쪽이 y 이상인 첫 줄을 찾는다.
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getY() >= y - 0.001) {
                return lines.get(i);
            }
        }
        Check.fail("no line at " + y);
        return null;
    }

    /**
     * The image is stretched over the content box: a mask of 10 by 10 with five opaque columns gives the same area as one of 100 by 100 with fifty.
     * <p>
     * 이미지는 콘텐츠 상자에 맞게 늘어난다: 열 다섯이 불투명한 10x10 마스크는 열 쉰이 불투명한 100x100 마스크와 같은 영역을 준다.
     */
    public void testImageIsStretchedOverTheContentBox() {
        List<LayoutBox> l = lines(new Masks(columns(10, 10, 5)), "shape-outside:url(a.png)", "left");
        Check.near(50, l.get(0).getX(), T, "five of ten columns are the left half of the 100 by 100 content box");
        // padding 이 있으면 이미지는 콘텐츠 상자(padding 안쪽)에 놓인다.
        List<LayoutBox> p = lines(new Masks(columns(10, 10, 5)), "box-sizing:content-box;width:60px;height:60px;padding:20px;shape-outside:url(a.png)", "left");
        // 콘텐츠 상자는 x 20..80 이고 왼쪽 절반은 20..50 이다.
        Check.near(50, p.get(2).getX(), T, "the image is placed in the content box of the float (20 + 30)");
        Check.near(0, p.get(0).getX(), T, "above the content box (in the padding) nothing is in the way");
    }

    /**
     * <code>shape-image-threshold</code>: only pixels whose alpha is greater than the threshold belong to the shape; a percentage is allowed.
     * <p>
     * <code>shape-image-threshold</code>: 알파가 기준보다 큰 픽셀만 모양에 속한다. 백분율도 쓸 수 있다.
     */
    public void testThreshold() {
        Masks m = new Masks(ramp(100, 100));
        Check.near(99, lines(m, "shape-outside:url(a.png)", "left").get(0).getX(), T, "threshold 0: every pixel with an alpha above 0 (all but the last column)");
        Check.near(50, lines(m, "shape-outside:url(a.png);shape-image-threshold:0.5", "left").get(0).getX(), T, "threshold 0.5: the alpha is above half up to column 49");
        Check.near(20, lines(m, "shape-outside:url(a.png);shape-image-threshold:80%", "left").get(0).getX(), T, "threshold 80%: up to column 19");
        Check.near(0, lines(m, "shape-outside:url(a.png);shape-image-threshold:1", "left").get(0).getX(), T, "threshold 1: no pixel is above it, the area is empty");
    }

    /**
     * An image without a pixel above the threshold is an empty area that does not affect wrapping.
     * <p>
     * 기준보다 큰 픽셀이 없는 이미지는 줄 감싸기에 영향이 없는 빈 영역이다.
     */
    public void testEmptyShapeDoesNotAffectWrapping() {
        List<LayoutBox> l = lines(new Masks(columns(100, 100, 0)), "shape-outside:url(a.png)", "left");
        Check.near(0, l.get(0).getX(), T, "a fully transparent image leaves the lines at the left edge");
    }

    /**
     * An image that is not available wraps around the margin box.
     * <p>
     * 쓸 수 없는 이미지는 margin box 를 감싼다.
     */
    public void testUnavailableImageUsesTheMarginBox() {
        List<LayoutBox> l = lines(new Masks(null), "shape-outside:url(a.png)", "left");
        Check.near(100, l.get(0).getX(), T, "the margin box until the image arrived");
    }

    /**
     * <code>shape-margin</code> grows the shape of an image outwards.
     * <p>
     * <code>shape-margin</code> 은 이미지의 모양을 바깥으로 넓힌다.
     */
    public void testShapeMargin() {
        List<LayoutBox> l = lines(new Masks(columns(100, 100, 50)), "shape-outside:url(a.png);shape-margin:10px", "left");
        Check.near(60, lineBelow(l, 40).getX(), T, "the middle of the float: 50 + 10");
    }

    /**
     * A right float wraps the lines at the left edge of its shape.
     * <p>
     * 오른쪽 float 는 모양의 왼쪽 가장자리에서 줄을 감싼다.
     */
    public void testRightFloat() {
        // 100 너비 float 는 x 300..400 에 있고 이미지의 오른쪽 절반(열 50..99)이 불투명하다.
        byte[] a = new byte[100 * 100];
        // 오른쪽 절반을 불투명하게 채운다.
        for (int y = 0; y < 100; y++) {
            for (int x = 50; x < 100; x++) {
                a[y * 100 + x] = (byte) 255;
            }
        }
        List<LayoutBox> l = lines(new Masks(new ShapeImage(100, 100, a)), "shape-outside:url(a.png)", "right");
        Check.near(350, l.get(0).getWidth(), T, "the first line ends where the opaque half begins (350)");
        Check.near(400, lineBelow(l, 100).getWidth(), T, "below the float the line is as wide as the container");
    }

    /**
     * A gradient is an image too: it is painted into the content box of the float and its alpha gives the shape.
     * <p>
     * 그라디언트도 이미지이다: float 의 콘텐츠 상자에 그리며 그 알파가 모양을 준다.
     */
    public void testGradientShape() {
        Masks m = new Masks(null);
        List<LayoutBox> l = lines(m, "shape-outside:linear-gradient(to right, black, transparent);shape-image-threshold:0.5", "left");
        Check.near(50, l.get(0).getX(), T, "the half of the gradient that is more than 50% opaque");
        Check.equal(100, m.gradientSize[0], "the gradient was painted at the width of the content box");
        Check.equal(100, m.gradientSize[1], "and at its height");
    }

    /**
     * A float without a source of masks (an ordinary resolver) treats an image like <code>none</code>.
     * <p>
     * 마스크 공급자가 없는 리졸버(일반 리졸버)에서는 float 가 이미지를 <code>none</code> 처럼 다룬다.
     */
    public void testWithoutASource() {
        StringBuilder words = new StringBuilder();
        // 줄마다 여러 단어가 들어가도록 짧은 단어를 많이 만든다.
        for (int i = 0; i < 400; i++) {
            words.append("ab ");
        }
        LayoutResult r = LayoutTestSupport.layout(LH + "<div style='float:left;width:100px;height:100px;shape-outside:url(a.png)'></div><p id=p style='margin:0'>" + words + "</p>", 400);
        Check.near(100, LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0).getX(), T, "the margin box");
    }

    /**
     * <code>image-set()</code> picks the candidate like the images of backgrounds do.
     * <p>
     * <code>image-set()</code> 은 배경 이미지처럼 후보를 고른다.
     */
    public void testImageSet() {
        Masks m = new Masks(columns(100, 100, 50));
        lines(m, "shape-outside:image-set(url(lo.png) 1x, url(hi.png) 2x)", "left");
        Check.equal("lo.png", m.urls.get(0), "the smallest resolution that is at least 1");
        Masks n = new Masks(columns(100, 100, 50));
        lines(n, "shape-outside:image-set(url(a.png) 0.5x, url(b.png) 0.25x)", "left");
        Check.equal("a.png", n.urls.get(0), "no candidate of 1x or more: the largest one");
    }
}
