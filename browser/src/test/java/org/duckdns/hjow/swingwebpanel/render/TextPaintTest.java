package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting of text (plan items P09-04, P09-09): ink inside the text box only, decoration lines at the expected rows, list
 * markers, letter spacing, Korean, combining marks, surrogate pairs and the scripts that crash <code>TextLayout</code> on JRE 6u45.
 * <p>
 * 텍스트 그리기 시험(계획 P09-04, P09-09): 텍스트 상자 안에만 있는 잉크, 기대한 행의 장식선, 목록 표식, 글자 간격, 한글, 결합 문자, 서로게이트 쌍,
 * JRE 6u45 에서 <code>TextLayout</code> 을 죽이는 문자 체계.
 */
public class TextPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextPaintTest() {
    }

    /**
     * Returns the first TEXT box of the first child of an element.
     * <p>
     * 요소의 첫 자식의 첫 TEXT 상자를 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the element id / 요소 id
     * @return the TEXT box / TEXT 상자
     */
    private static LayoutBox text(RenderedPage p, String id) {
        Element e = RenderTestSupport.lastDocument.getElementById(id);
        Check.notNull(e, "element #" + id);
        Node t = e.getFirstChild();
        Check.notNull(t, "element #" + id + " has a child");
        List<LayoutBox> l = p.getLayout().getBoxes(t);
        Check.isFalse(l.isEmpty(), "the text of #" + id + " has a box");
        return l.get(0);
    }

    /**
     * Counts the ink pixels inside a layout box.
     * <p>
     * 레이아웃 상자 안의 잉크 픽셀 수를 센다.
     *
     * @param img the image / 이미지
     * @param b   the box / 상자
     * @return the count / 개수
     */
    private static int inkIn(BufferedImage img, LayoutBox b) {
        return RenderTestSupport.touched(img, (int) Math.floor(b.getX()), (int) Math.floor(b.getY()), (int) Math.ceil(b.getX() + b.getWidth()),
                (int) Math.ceil(b.getY() + b.getHeight()));
    }

    /**
     * Text paints ink inside its box and nowhere else.
     * <p>
     * 텍스트는 자기 상자 안에만 잉크를 남기고 다른 곳에는 남기지 않는다.
     */
    public void testInkOnlyInsideTheTextBox() {
        RenderedPage p = RenderTestSupport.page("<div style='height:30px'></div><div id='t' style='font-size:14px'>Hello World</div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        Node node = RenderTestSupport.lastDocument.getElementById("t").getFirstChild();
        double left = 1e9;
        double top = 1e9;
        double right = -1e9;
        double bottom = -1e9;
        // 텍스트 노드의 모든 조각(낱말마다 나뉜 상자)을 하나의 영역으로 합친다.
        for (LayoutBox f : p.getLayout().getBoxes(node)) {
            left = Math.min(left, f.getX());
            top = Math.min(top, f.getY());
            right = Math.max(right, f.getX() + f.getWidth());
            bottom = Math.max(bottom, f.getY() + f.getHeight());
        }
        Check.isTrue(RenderTestSupport.touched(img, (int) left, (int) top, (int) Math.ceil(right), (int) Math.ceil(bottom)) > 20, "there is ink in the text box");
        Check.equal(0, RenderTestSupport.touched(img, (int) Math.ceil(right) + 2, 0, 300, 100), "no ink to the right of the text");
        Check.equal(0, RenderTestSupport.touched(img, 0, 0, 300, (int) Math.floor(top) - 1), "no ink above the text");
        Check.equal(0, RenderTestSupport.touched(img, 0, (int) Math.ceil(bottom) + 2, 300, 100), "no ink below the text");
    }

    /**
     * Counts the rows of a box in which at least 90 percent of the pixels across its width are dark.
     * <p>
     * 상자의 너비에 걸쳐 픽셀의 90 % 이상이 어두운 행을 센다.
     *
     * @param img the image / 이미지
     * @param b   the box / 상자
     * @param y0  first row / 처음 행
     * @param y1  last row (exclusive) / 마지막 행(미포함)
     * @return the number of rows / 행의 수
     */
    private static int fullRows(BufferedImage img, LayoutBox b, int y0, int y1) {
        int rows = 0;
        int x0 = (int) Math.ceil(b.getX()) + 1;
        int x1 = (int) Math.floor(b.getX() + b.getWidth()) - 1;
        // 행마다 어두운 픽셀의 비율을 확인한다.
        for (int y = y0; y < y1; y++) {
            int dark = RenderTestSupport.ink(img, x0, y, x1, y + 1);
            if (dark >= 0.9 * (x1 - x0)) {
                rows++;
            }
        }
        return rows;
    }

    /**
     * Underline, overline and line-through are painted at the expected rows and only when asked for.
     * <p>
     * 밑줄, 윗줄, 취소선은 기대한 행에 그려지며 요청했을 때만 그려진다.
     */
    public void testDecorationLines() {
        String body = "<div id='n' style='font-size:30px;height:40px'>WWW</div>"
                + "<div id='u' style='font-size:30px;height:40px;text-decoration:underline'>WWW</div>"
                + "<div id='o' style='font-size:30px;height:40px;text-decoration:overline'>WWW</div>"
                + "<div id='l' style='font-size:30px;height:40px;text-decoration:line-through'>WWW</div>";
        RenderedPage p = RenderTestSupport.page(body, 300, 200, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        LayoutBox n = text(p, "n");
        LayoutBox u = text(p, "u");
        LayoutBox o = text(p, "o");
        LayoutBox l = text(p, "l");
        int ub = (int) Math.round(u.getBaseline());
        int ob = (int) Math.round(o.getBaseline());
        int lb = (int) Math.round(l.getBaseline());
        Check.equal(0, fullRows(img, n, (int) n.getBaseline() - 30, (int) n.getBaseline() + 12), "plain text has no full-width row");
        Check.isTrue(fullRows(img, u, ub + 1, ub + 12) >= 1, "underline below the baseline");
        Check.equal(0, fullRows(img, u, ub - 30, ub - 5), "an underline paints nothing above the baseline");
        Check.isTrue(fullRows(img, o, ob - (int) Math.ceil(o.getAscent()) - 1, ob - (int) Math.floor(o.getAscent()) + 3) >= 1, "overline at the top of the text");
        Check.isTrue(fullRows(img, l, lb - (int) o.getAscent() + 4, lb - 2) >= 1, "line-through in the middle of the text");
    }

    /**
     * List markers: a disc is a filled circle, a circle is hollow, a square is filled and numbers are text.
     * <p>
     * 목록 표식: disc 는 채운 원, circle 은 속이 빈 원, square 는 채운 사각형, 숫자는 텍스트이다.
     */
    public void testListMarkers() {
        String body = "<ul style='margin:0;padding-left:30px;list-style-type:disc'><li style='height:20px'>a</li></ul>"
                + "<ul style='margin:0;padding-left:30px;list-style-type:circle'><li style='height:20px'>b</li></ul>"
                + "<ul style='margin:0;padding-left:30px;list-style-type:square'><li style='height:20px'>c</li></ul>"
                + "<ol style='margin:0;padding-left:30px'><li id='n' style='height:20px'>d</li></ol>";
        RenderedPage p = RenderTestSupport.page(body, 200, 120, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 120);
        DisplayList l = p.getDisplayList();
        int disc = 0;
        int circle = 0;
        int square = 0;
        // 표식 모양 명령을 종류별로 센다.
        for (int i = 0; i < l.size(); i++) {
            DisplayCommand c = l.get(i);
            if (c.getType() == DisplayCommand.Type.MARKER_SHAPE) {
                int s = ((DisplayCommands.MarkerShape) c).getShape();
                java.awt.geom.Rectangle2D b = c.getBounds();
                int cx = (int) (b.getX() + b.getWidth() / 2);
                int cy = (int) (b.getY() + b.getHeight() / 2);
                if (s == DisplayCommands.SHAPE_DISC) {
                    disc++;
                    Check.isTrue(RenderTestSupport.ink(img, cx, cy, cx + 1, cy + 1) == 1, "the disc is filled in the middle");
                } else if (s == DisplayCommands.SHAPE_CIRCLE) {
                    circle++;
                    Check.isTrue(RenderTestSupport.ink(img, (int) b.getX(), (int) b.getY(), (int) (b.getX() + b.getWidth() + 1),
                            (int) (b.getY() + b.getHeight() + 1)) > 4, "the circle has an outline");
                } else {
                    square++;
                    Check.isTrue(RenderTestSupport.ink(img, cx, cy, cx + 1, cy + 1) == 1, "the square is filled in the middle");
                }
            }
        }
        Check.equal(1, disc, "one disc marker");
        Check.equal(1, circle, "one circle marker");
        Check.equal(1, square, "one square marker");
        boolean numberFound = false;
        // 번호 목록의 표식 상자를 찾아 그 안에 잉크가 있는지 본다.
        for (LayoutBox b : p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("n"))) {
            if (b.getType() == LayoutBox.Type.MARKER) {
                numberFound = true;
                Check.isTrue(inkIn(img, b) > 3, "the number of the ordered list is painted");
            }
        }
        Check.isTrue(numberFound, "the ordered list item has a marker box");
    }

    /**
     * Letter spacing widens the painted text.
     * <p>
     * 글자 간격은 그려진 텍스트를 넓힌다.
     */
    public void testLetterSpacing() {
        RenderedPage a = RenderTestSupport.page("<div id='t' style='font-size:16px'>iiii</div>", 200, 40, null);
        RenderedPage b = RenderTestSupport.page("<div id='t' style='font-size:16px;letter-spacing:6px'>iiii</div>", 200, 40, null);
        BufferedImage ia = RenderTestSupport.paint(a, 200, 40);
        BufferedImage ib = RenderTestSupport.paint(b, 200, 40);
        int ra = rightmostInk(ia);
        int rb = rightmostInk(ib);
        Check.isTrue(rb >= ra + 12, "letter-spacing:6px moves the last glyph to the right: " + ra + " -> " + rb);
    }

    /**
     * Finds the x of the rightmost ink pixel.
     * <p>
     * 가장 오른쪽 잉크 픽셀의 x 를 찾는다.
     *
     * @param img the image / 이미지
     * @return x, or -1 / x. 없으면 -1
     */
    private static int rightmostInk(BufferedImage img) {
        // 오른쪽 끝에서 왼쪽으로 가며 잉크가 있는 첫 열을 찾는다.
        for (int x = img.getWidth() - 1; x >= 0; x--) {
            if (RenderTestSupport.ink(img, x, 0, x + 1, img.getHeight()) > 0) {
                return x;
            }
        }
        return -1;
    }

    /**
     * Korean text, combining marks and surrogate pairs are painted.
     * <p>
     * 한글, 결합 문자, 서로게이트 쌍이 그려진다.
     */
    public void testKoreanCombiningAndSurrogates() {
        String korean = RenderTestSupport.str(0xD55C, 0xAE00, 0x20, 0xD14D, 0xC2A4, 0xD2B8);
        String combining = RenderTestSupport.str(0x65, 0x301, 0x61, 0x308);
        String emoji = RenderTestSupport.str(0x61, 0xD83D, 0xDE00, 0x62);
        RenderedPage p = RenderTestSupport.page("<div id='k' style='font-size:18px;height:30px'>" + korean + "</div><div id='c' style='font-size:18px;height:30px'>"
                + combining + "</div><div id='e' style='font-size:18px;height:30px'>" + emoji + "</div>", 200, 120, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 120);
        Check.isTrue(inkIn(img, text(p, "k")) > 40, "Korean text paints ink: " + inkIn(img, text(p, "k")));
        Check.isTrue(inkIn(img, text(p, "c")) > 10, "text with combining marks paints ink");
        Check.isTrue(inkIn(img, text(p, "e")) > 5, "text with a surrogate pair paints ink");
    }

    /**
     * Scripts that crash TextLayout on JRE 6u45 (Hebrew and others) are painted without crashing, left-to-right and right-to-left.
     * <p>
     * JRE 6u45 에서 TextLayout 을 죽이는 문자 체계(히브리어 등)가 죽지 않고 그려진다(왼쪽에서 오른쪽, 오른쪽에서 왼쪽).
     */
    public void testComplexScriptsDoNotCrash() {
        String hebrew = RenderTestSupport.str(0x05E9, 0x05DC, 0x05D5, 0x05DD);
        String arabic = RenderTestSupport.str(0x0645, 0x0631, 0x062D, 0x0628, 0x0627);
        String thai = RenderTestSupport.str(0x0E2A, 0x0E27, 0x0E31, 0x0E2A, 0x0E14, 0x0E35);
        String hindi = RenderTestSupport.str(0x0928, 0x092E, 0x0938, 0x094D, 0x0924, 0x0947);
        RenderedPage p = RenderTestSupport.page("<div id='h' style='font-size:18px;height:30px'>" + hebrew + "</div>"
                + "<div id='a' style='font-size:18px;height:30px'>" + arabic + "</div>"
                + "<div id='t' style='font-size:18px;height:30px'>" + thai + "</div>"
                + "<div id='d' style='font-size:18px;height:30px'>" + hindi + "</div>"
                + "<div id='r' dir='rtl' style='font-size:18px;height:30px'>" + hebrew + " abc " + arabic + "</div>", 240, 170, null);
        BufferedImage img = RenderTestSupport.paint(p, 240, 170);
        Check.isTrue(inkIn(img, text(p, "h")) > 20, "Hebrew paints ink: " + inkIn(img, text(p, "h")));
        Check.isTrue(inkIn(img, text(p, "a")) > 20, "Arabic paints ink");
        Check.isTrue(inkIn(img, text(p, "t")) > 20, "Thai paints ink");
        Check.isTrue(inkIn(img, text(p, "d")) > 20, "Devanagari paints ink");
        Check.isTrue(RenderTestSupport.ink(img, 0, 120, 240, 150) > 40, "a right-to-left paragraph paints ink");
        BufferedImage again = RenderTestSupport.paint(p, 240, 170);
        Check.equal(img.getRGB(20, 10), again.getRGB(20, 10), "painting is repeatable");
    }
}
