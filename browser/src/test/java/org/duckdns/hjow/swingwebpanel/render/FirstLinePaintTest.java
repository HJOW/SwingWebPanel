package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Pixel tests of <code>::first-line</code> (plan item P10-04-f): the background of the pseudo-element is painted behind the text of the
 * first line only, the text of the first line has the first-line color, the rest of the paragraph has neither.
 * <p>
 * <code>::first-line</code> 화소 시험(계획 P10-04-f): 의사 요소의 배경은 첫 줄의 텍스트 뒤에만 그려지고, 첫 줄의 텍스트는 첫 줄의 색을 가지며, 문단의 나머지는 둘 다 없다.
 */
public class FirstLinePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FirstLinePaintTest() {
    }

    /**
     * Counts the pixels of a color in a rectangle.
     * <p>
     * 사각형 안에서 한 색의 픽셀 수를 센다.
     *
     * @param img the image / 이미지
     * @param rgb the color 0xRRGGBB / 색 0xRRGGBB
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @return the count / 개수
     */
    private static int count(BufferedImage img, int rgb, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 색이 같은 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Counts the reddish pixels (text anti-aliased over a yellow background is not pure red) in a rectangle.
     * <p>
     * 사각형 안의 붉은 픽셀을 센다(노란 배경 위에서 안티앨리어싱된 글자는 순수한 빨강이 아니다).
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @return the count / 개수
     */
    private static int reddish(BufferedImage img, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 빨강이 크고 초록이 작은 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                int c = img.getRGB(x, y);
                if (((c >> 16) & 0xFF) > 0xC0 && ((c >> 8) & 0xFF) < 0xA0 && (c & 0xFF) < 0x80) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * The yellow background and the red text are on the first line; the second line is white with black text.
     * <p>
     * 노란 배경과 빨간 글자는 첫 줄에 있고 둘째 줄은 흰 바탕에 검은 글자이다.
     */
    public void testBackgroundAndColorOnTheFirstLineOnly() {
        double cw = LayoutTestSupport.charWidth();
        RenderedPage p = RenderTestSupport.page("<style>p::first-line{background:#ff0;color:#f00}</style><p id=a style='width:" + (14 * cw)
                + "px'>aaaa bbbb cccc dddd eeee</p>", 200, 100, null);
        List<LayoutBox> lines = LayoutTestSupport.lines(RenderTestSupport.box(p, "a"));
        Check.isTrue(lines.size() >= 2, "the paragraph has two lines");
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        LayoutBox l1 = lines.get(0);
        LayoutBox l2 = lines.get(1);
        int x0 = (int) Math.floor(l1.getX());
        int x1 = (int) Math.ceil(l1.getX() + l1.getWidth());
        int yellow1 = count(img, 0xFFFF00, x0, (int) l1.getY(), x1, (int) (l1.getY() + l1.getHeight()));
        int red1 = reddish(img, x0, (int) l1.getY(), x1, (int) (l1.getY() + l1.getHeight()));
        int yellow2 = count(img, 0xFFFF00, x0, (int) l2.getY(), x1, (int) (l2.getY() + l2.getHeight()));
        int red2 = reddish(img, x0, (int) l2.getY(), x1, (int) (l2.getY() + l2.getHeight()));
        Check.isTrue(yellow1 > 100, "the first line has the yellow background: " + yellow1);
        Check.isTrue(red1 > 10, "the first line has red ink: " + red1);
        Check.equal(0, yellow2, "the second line has no yellow");
        Check.equal(0, red2, "the second line has no red ink");
        Check.isTrue(RenderTestSupport.ink(img, x0, (int) l2.getY(), x1, (int) (l2.getY() + l2.getHeight())) > 10, "the second line has dark ink");
    }
}
