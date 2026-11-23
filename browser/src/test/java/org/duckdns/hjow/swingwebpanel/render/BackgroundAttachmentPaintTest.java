package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>background-attachment</code> <code>fixed</code> and <code>local</code> and of <code>background-clip: text</code> (CSS
 * Backgrounds and Borders Level 3 section 3.9 and Level 4 section 3.6, https://www.w3.org/TR/css-backgrounds-3/#the-background-attachment): a fixed
 * background is positioned against the viewport, so it does not move when the page scrolls behind its box; a local background is positioned
 * in the scrollable overflow area of a box that clips; a background clipped to text shows only where the glyphs of the element are, for a
 * block, for an inline element and with the prefixed name. The pixels are read from an off-screen image at different scroll positions.
 * <p>
 * <code>background-attachment</code> 의 <code>fixed</code> 와 <code>local</code>, 그리고 <code>background-clip: text</code> 의 시험(CSS Backgrounds and Borders Level 3
 * 3.9 절과 Level 4 3.6 절): fixed 배경은 뷰포트를 기준으로 위치하므로 페이지가 상자 뒤에서 스크롤되어도 움직이지 않고, local 배경은 내용을 자르는 상자의 스크롤 가능한 넘침 영역에
 * 위치하며, 글자로 자른 배경은 요소의 글리프가 있는 곳에만 보인다(블록, 인라인 요소, 접두사 붙은 이름). 픽셀은 여러 스크롤 위치에서 화면 밖 이미지로 읽는다.
 */
public class BackgroundAttachmentPaintTest {

    /** The red of the pixels that are expected inside the box. 상자 안에서 기대하는 화소의 빨강. */
    private static final int RED = 0xFF0000;
    /** The blue of the pixels that are expected in the lower half. 아래쪽 절반에서 기대하는 화소의 파랑. */
    private static final int BLUE = 0x0000FF;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BackgroundAttachmentPaintTest() {
    }

    /**
     * Paints a page whose scroll the painter applies.
     * <p>
     * 페이지를 그리기 단계가 스크롤을 적용하는 방식으로 그린다.
     *
     * @param p       the page / 페이지
     * @param w       image width / 이미지 너비
     * @param h       image height / 이미지 높이
     * @param scrollY vertical scroll / 세로 스크롤
     * @return the image / 이미지
     */
    private static BufferedImage paintAt(RenderedPage p, int w, int h, int scrollY) {
        BufferedImage img = RenderTestSupport.blank(w, h);
        Graphics2D g = img.createGraphics();
        try {
            p.paint(g, new Rectangle(0, 0, w, h), 0, scrollY);
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Counts the pixels of a color in a rectangle of an image.
     * <p>
     * 이미지의 사각형 안에서 어떤 색의 화소 수를 센다.
     *
     * @param img   the image / 이미지
     * @param color the RGB color / RGB 색
     * @param x0    left / 왼쪽
     * @param y0    top / 위쪽
     * @param x1    right (exclusive) / 오른쪽(미포함)
     * @param y1    bottom (exclusive) / 아래쪽(미포함)
     * @return the count / 개수
     */
    private static int count(BufferedImage img, int color, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 사각형의 화소를 하나씩 보며 색이 같은 것을 센다.
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (RenderTestSupport.rgb(img, x, y) == color) {
                    n++;
                }
            }
        }
        return n;
    }

    /** The page of the fixed tests: 200px of spacer and a box that is as wide as the viewport and 100px high. 고정 배경 시험의 페이지: 200px 간격과 뷰포트만큼 넓은 높이 100px 상자. */
    private static final String FIXED = "<div style='height:200px'></div><div id=box style='height:100px;"
            + "background:linear-gradient(to bottom,#f00 0%,#f00 50%,#00f 50%,#00f 100%) fixed'></div><div style='height:400px'></div>";

    /**
     * A fixed background is sized and positioned against the viewport: the box shows the part of the viewport gradient that lies behind it, so the
     * colors do not follow the box when the page scrolls.
     * <p>
     * fixed 배경은 뷰포트를 기준으로 크기와 위치가 정해진다: 상자는 그 뒤에 있는 뷰포트 그라디언트의 부분을 보여 주므로 페이지가 스크롤되어도 색이 상자를 따라가지 않는다.
     */
    public void testFixedBackgroundStaysWithTheViewport() {
        RenderedPage p = RenderTestSupport.page(FIXED, 200, 100, null);
        // 스크롤 150: 뷰포트는 문서 y 150..250 이고 상자는 화면 y 50..100 에 보인다. 뷰포트 그라디언트의 아래 절반(화면 y 50..100)은 파랑이다.
        BufferedImage mid = paintAt(p, 200, 100, 150);
        Check.equal(BLUE, RenderTestSupport.rgb(mid, 10, 55), "scroll 150: the lower half of the viewport gradient is behind the box (blue)");
        Check.equal(BLUE, RenderTestSupport.rgb(mid, 10, 95), "scroll 150: blue to the bottom");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(mid, 10, 20), "scroll 150: nothing outside the box");
        // 스크롤 200: 상자가 뷰포트를 정확히 채우므로 그라디언트 전체(위 절반 빨강, 아래 절반 파랑)가 보인다.
        BufferedImage full = paintAt(p, 200, 100, 200);
        Check.equal(RED, RenderTestSupport.rgb(full, 10, 10), "scroll 200: the upper half of the viewport gradient");
        Check.equal(BLUE, RenderTestSupport.rgb(full, 10, 90), "scroll 200: the lower half");
        // 스크롤 170: 상자는 화면 y 30..100 에 보이고 뷰포트의 위 절반(y 30..50)은 빨강이다. 상자 기준이면 상자 위쪽(빨강)이 보이므로 파랑이 나온 위치로 구별한다.
        BufferedImage some = paintAt(p, 200, 100, 170);
        Check.equal(RED, RenderTestSupport.rgb(some, 10, 40), "scroll 170: the viewport gradient is still red at the screen y 40");
        Check.equal(BLUE, RenderTestSupport.rgb(some, 10, 60), "scroll 170: and blue at the screen y 60");
    }

    /**
     * A host that scrolled the graphics already gives the viewport position, and the fixed background follows the viewport.
     * <p>
     * 이미 그래픽스를 스크롤한 호스트는 뷰포트 위치를 주며 fixed 배경은 뷰포트를 따라간다.
     */
    public void testFixedBackgroundWithHostScroll() {
        RenderedPage p = RenderTestSupport.page(FIXED, 200, 100, null);
        BufferedImage img = RenderTestSupport.blank(200, 100);
        Graphics2D g = img.createGraphics();
        try {
            g.translate(0, -150);
            p.paint(g, new Rectangle(0, 150, 200, 100), 0, 0, 1.0, 0L, 0, 150);
        } finally {
            g.dispose();
        }
        Check.equal(BLUE, RenderTestSupport.rgb(img, 10, 55), "scroll 150 given by the host: blue behind the box");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 10, 20), "nothing above the box");
    }

    /**
     * Without <code>fixed</code> the same gradient belongs to the box, as a control of the test above.
     * <p>
     * <code>fixed</code> 가 없으면 같은 그라디언트는 상자에 속한다(위 시험의 대조군).
     */
    public void testScrollBackgroundFollowsTheBox() {
        RenderedPage p = RenderTestSupport.page("<div style='height:200px'></div><div style='height:100px;"
                + "background:linear-gradient(to bottom,#f00 0%,#f00 50%,#00f 50%,#00f 100%)'></div><div style='height:400px'></div>", 200, 100, null);
        BufferedImage mid = paintAt(p, 200, 100, 150);
        Check.equal(RED, RenderTestSupport.rgb(mid, 10, 55), "the upper half of the box gradient is at the screen y 50..100");
    }

    /**
     * A local background is positioned in the whole scrollable area of a box that clips, so the box shows only the beginning of it.
     * <p>
     * local 배경은 내용을 자르는 상자의 스크롤 가능한 전체 영역에 위치하므로 상자는 그 처음 부분만 보여 준다.
     */
    public void testLocalBackgroundCoversTheScrollableArea() {
        String gradient = "linear-gradient(to bottom,#f00 0%,#f00 50%,#00f 50%,#00f 100%)";
        RenderedPage local = RenderTestSupport.page("<div id=box style='height:100px;overflow:auto;background:" + gradient + " local'><div style='height:400px'></div></div>", 200, 300,
                null);
        BufferedImage a = paintAt(local, 200, 300, 0);
        Check.equal(RED, RenderTestSupport.rgb(a, 10, 90), "local: the area is 400px high, so the box shows only the red upper half");
        RenderedPage scroll = RenderTestSupport.page("<div id=box style='height:100px;overflow:auto;background:" + gradient + " scroll'><div style='height:400px'></div></div>", 200, 300,
                null);
        BufferedImage b = paintAt(scroll, 200, 300, 0);
        Check.equal(BLUE, RenderTestSupport.rgb(b, 10, 90), "scroll: the gradient fits the padding box of 100px");
        // overflow 가 visible 인 상자의 local 은 scroll 과 같다.
        RenderedPage plain = RenderTestSupport.page("<div style='height:100px;background:" + gradient + " local'></div>", 200, 300, null);
        BufferedImage c = paintAt(plain, 200, 300, 0);
        Check.equal(BLUE, RenderTestSupport.rgb(c, 10, 90), "a box that does not scroll: local is like scroll");
    }

    /**
     * A block whose background is clipped to the text shows the background only inside the glyphs; the text itself is transparent.
     * <p>
     * 배경을 글자로 자른 블록은 글리프 안에서만 배경을 보여 준다. 텍스트 자체는 투명하다.
     */
    public void testBackgroundClippedToTextOfABlock() {
        String style = "font:bold 60px monospace;color:transparent;background:#f00;width:120px;height:72px;";
        RenderedPage clipped = RenderTestSupport.page("<div id=t style='" + style + "background-clip:text'>MM</div>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(clipped, 200, 100);
        int red = count(img, RED, 0, 0, 120, 72);
        Check.isTrue(red > 300, "red shows inside the glyphs: " + red);
        Check.isTrue(red < 120 * 72 * 7 / 10, "but not over the whole box: " + red);
        RenderedPage plain = RenderTestSupport.page("<div id=t style='" + style + "'>MM</div>", 200, 100, null);
        BufferedImage all = RenderTestSupport.paint(plain, 200, 100);
        Check.equal(120 * 72, count(all, RED, 0, 0, 120, 72), "without the clip the whole box is red");
        RenderedPage prefixed = RenderTestSupport.page("<div id=t style='" + style + "-webkit-background-clip:text'>MM</div>", 200, 100, null);
        BufferedImage pre = RenderTestSupport.paint(prefixed, 200, 100);
        Check.equal(red, count(pre, RED, 0, 0, 120, 72), "the prefixed name is the same");
    }

    /**
     * The usual gradient text: an inline element with a gradient that is clipped to its text.
     * <p>
     * 흔히 쓰는 그라디언트 글자: 글자로 자른 그라디언트를 가진 인라인 요소.
     */
    public void testGradientTextOfAnInlineElement() {
        RenderedPage p = RenderTestSupport.page("<p style='margin:0;font:bold 60px monospace;color:transparent'>"
                + "<span id=g style='background:linear-gradient(to right,#f00,#f00 50%,#00f 50%,#00f);background-clip:text'>MMMM</span></p>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        // 인라인 요소의 상자에서 가운데를 구해 그라디언트가 바뀌는 곳을 알아낸다.
        int x0 = (int) Math.floor(RenderTestSupport.box(p, "g").getX());
        int x1 = (int) Math.ceil(RenderTestSupport.box(p, "g").getX() + RenderTestSupport.box(p, "g").getWidth());
        int mid = (x0 + x1) / 2;
        int red = count(img, RED, x0, 0, mid - 3, 80);
        int blue = count(img, BLUE, mid + 3, 0, x1, 80);
        Check.isTrue(red > 200, "the left half of the text is red: " + red);
        Check.isTrue(blue > 200, "the right half of the text is blue: " + blue);
        Check.equal(0, count(img, BLUE, x0, 0, mid - 3, 80), "no blue in the left half");
        Check.equal(0, count(img, RED, mid + 3, 0, x1, 80), "no red in the right half");
        Check.equal(0, count(img, 0x000000, 0, 0, 300, 80), "the text itself is transparent");
    }
}
