package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting and the hit testing of vertical text (plan item P10-03, CSS Writing Modes Level 3): the glyphs of
 * <code>vertical-rl</code>/<code>vertical-lr</code> text are the glyphs of horizontal text rotated by a quarter turn clockwise,
 * those of <code>sideways-lr</code> counter-clockwise (compared pixel by pixel with a horizontal rendering of the same text), the
 * background and the border of a vertical box lie where its physical geometry is, and a click on vertical text finds the character
 * under the point along the vertical line.
 * <p>
 * 세로쓰기 텍스트의 그리기와 히트 테스트 시험(계획 P10-03, CSS Writing Modes Level 3): <code>vertical-rl</code>/<code>vertical-lr</code> 텍스트의 글리프는 가로쓰기 텍스트의
 * 글리프를 시계 방향으로 사분원 돌린 것이고 <code>sideways-lr</code> 은 시계 반대 방향이다(같은 텍스트의 가로 렌더링과 화소 단위로 비교). 세로 상자의 배경과 border 는
 * 물리적 기하의 자리에 있고 세로 텍스트를 클릭하면 세로 줄을 따라 점 아래의 글자를 찾는다.
 */
public class VerticalTextPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VerticalTextPaintTest() {
    }

    /**
     * Finds the first text fragment below a box.
     * <p>
     * 상자 아래의 첫 텍스트 조각을 찾는다.
     *
     * @param b the box / 상자
     * @return the fragment or null / 조각. 없으면 null
     */
    private static LayoutBox firstText(LayoutBox b) {
        if (b.getType() == LayoutBox.Type.TEXT) {
            return b;
        }
        List<LayoutBox> kids = b.getChildren();
        // 자식 아래에서 첫 텍스트 조각을 찾는다.
        for (int i = 0; i < kids.size(); i++) {
            LayoutBox t = firstText(kids.get(i));
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /**
     * Tells whether a pixel is dark (ink of black text).
     * <p>
     * 화소가 어두운지(검은 텍스트의 잉크인지) 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return true for ink / 잉크이면 true
     */
    private static boolean ink(BufferedImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
            return false;
        }
        int c = RenderTestSupport.rgb(img, x, y);
        return ((c >> 16) & 255) < 128 && ((c >> 8) & 255) < 128 && (c & 255) < 128;
    }

    /**
     * Compares the ink of vertical text with the rotated ink of the same horizontal text: the share of the horizontal ink that is found at
     * the rotated position.
     * <p>
     * 세로 텍스트의 잉크를 같은 가로 텍스트를 돌린 잉크와 비교한다: 가로 잉크 중 돌린 위치에서 발견되는 비율.
     *
     * @param horizontalCss css of the horizontal box / 가로 상자의 css
     * @param verticalCss   css of the vertical box / 세로 상자의 css
     * @param clockwise     true if the vertical text is rotated clockwise / 세로 텍스트가 시계 방향으로 돌았으면 true
     * @return the share between 0 and 1 / 0 과 1 사이의 비율
     */
    private static double overlap(String horizontalCss, String verticalCss, boolean clockwise) {
        String font = "font:36px monospace;line-height:44px;color:#000;";
        RenderedPage ph = RenderTestSupport.page("<div id=h style='" + font + horizontalCss + "'>LAT</div>", 300, 300, null);
        LayoutBox th = firstText(RenderTestSupport.box(ph, "h"));
        RenderedPage pv = RenderTestSupport.page("<div id=v style='" + font + verticalCss + "'>LAT</div>", 300, 300, null);
        LayoutBox tv = firstText(RenderTestSupport.box(pv, "v"));
        BufferedImage ih = RenderTestSupport.paint(ph, 300, 300);
        BufferedImage iv = RenderTestSupport.paint(pv, 300, 300);
        Check.notNull(th, "horizontal text fragment");
        Check.notNull(tv, "vertical text fragment");
        Check.near(th.getWidth(), tv.getHeight(), 0.01, "the advance is the height of the vertical fragment");
        Check.near(th.getHeight(), tv.getWidth(), 0.01, "the line height is the width of the vertical fragment");
        int total = 0;
        int found = 0;
        int w = (int) Math.ceil(th.getWidth());
        int h = (int) Math.ceil(th.getHeight());
        // 가로 텍스트 상자의 화소마다 돌린 위치의 세로 화소와 비교한다.
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (!ink(ih, (int) Math.round(th.getX()) + dx, (int) Math.round(th.getY()) + dy)) {
                    continue;
                }
                total++;
                int vx = clockwise ? (int) Math.round(tv.getX()) + (h - 1 - dy) : (int) Math.round(tv.getX()) + dy;
                int vy = clockwise ? (int) Math.round(tv.getY()) + dx : (int) Math.round(tv.getY()) + (w - 1 - dx);
                // 안티앨리어싱과 반올림 때문에 한 화소 안쪽의 이웃까지 본다.
                boolean hit = false;
                for (int ox = -1; ox <= 1 && !hit; ox++) {
                    // 세로 이웃을 훑는다.
                    for (int oy = -1; oy <= 1 && !hit; oy++) {
                        hit = ink(iv, vx + ox, vy + oy);
                    }
                }
                if (hit) {
                    found++;
                }
            }
        }
        Check.isTrue(total > 100, "the horizontal text has ink: " + total);
        return (double) found / total;
    }

    /**
     * The ink of <code>vertical-rl</code> text is the ink of the horizontal text turned a quarter turn clockwise.
     * <p>
     * <code>vertical-rl</code> 텍스트의 잉크는 가로 텍스트의 잉크를 시계 방향으로 사분원 돌린 것이다.
     */
    public void testVerticalRlGlyphsAreRotatedClockwise() {
        double share = overlap("", "writing-mode:vertical-rl;height:200px;", true);
        Check.isTrue(share > 0.9, "rotated ink overlap: " + share);
    }

    /**
     * Also in <code>vertical-lr</code> the glyphs are rotated clockwise (the line-over side is on the right).
     * <p>
     * <code>vertical-lr</code> 에서도 글리프는 시계 방향으로 돈다(line-over 쪽이 오른쪽이다).
     */
    public void testVerticalLrGlyphsAreRotatedClockwise() {
        double share = overlap("", "writing-mode:vertical-lr;height:200px;", true);
        Check.isTrue(share > 0.9, "rotated ink overlap: " + share);
    }

    /**
     * The glyphs of <code>sideways-lr</code> are rotated counter-clockwise and start at the bottom.
     * <p>
     * <code>sideways-lr</code> 의 글리프는 시계 반대 방향으로 돌고 아래에서 시작한다.
     */
    public void testSidewaysLrGlyphsAreRotatedCounterClockwise() {
        double share = overlap("", "writing-mode:sideways-lr;height:200px;", false);
        Check.isTrue(share > 0.9, "rotated ink overlap: " + share);
    }

    /**
     * Text without rotation does not change: horizontal text found at the horizontal position (control of the comparison).
     * <p>
     * 회전하지 않은 텍스트는 바뀌지 않는다: 가로 텍스트는 가로 위치에서 발견된다(비교의 대조군).
     */
    public void testControlHorizontalVersusHorizontal() {
        RenderedPage p = RenderTestSupport.page("<div id=h style='font:36px monospace;line-height:44px;color:#000'>LAT</div>", 300, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 300, 100);
        LayoutBox t = firstText(RenderTestSupport.box(p, "h"));
        int ink = 0;
        // 텍스트 상자 안의 잉크를 센다.
        for (int y = (int) t.getY(); y < (int) (t.getY() + t.getHeight()); y++) {
            for (int x = (int) t.getX(); x < (int) (t.getX() + t.getWidth()); x++) {
                if (ink(img, x, y)) {
                    ink++;
                }
            }
        }
        Check.isTrue(ink > 100, "ink inside the text box: " + ink);
        Check.equal(0, t.getTextRotation(), "no rotation");
    }

    /**
     * The background and the border of a vertical box are painted at its physical rectangle, below the vertical text.
     * <p>
     * 세로 상자의 배경과 border 는 물리적 사각형에 세로 텍스트 아래에 그려진다.
     */
    public void testVerticalBoxBackgroundAndBorder() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='font:16px monospace;writing-mode:vertical-rl;height:80px;line-height:20px;background:#ff0;border:3px solid #00f;color:#000'>ab<br>cd</div>", 200, 150, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 150);
        LayoutBox v = RenderTestSupport.box(p, "v");
        Check.near(46, v.getWidth(), 0.5, "two lines of about 20 and two borders");
        Check.near(86, v.getHeight(), 0.5, "80 and two borders");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 1, 40), "left border");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, (int) v.getWidth() - 2, 40), "right border");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 20, 1), "top border");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 20, (int) v.getHeight() - 2), "bottom border");
        Check.equal(0xFFFF00, RenderTestSupport.rgb(img, 5, 75), "the background below the text");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 100, 40), "outside the box nothing is painted");
    }

    /**
     * A click on vertical text finds the character under the point along the vertical line.
     * <p>
     * 세로 텍스트를 클릭하면 세로 줄을 따라 점 아래의 글자를 찾는다.
     */
    public void testHitTestOnVerticalText() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='font:20px monospace;writing-mode:vertical-rl;height:200px;line-height:30px'>abcdef</div>", 200, 300, null);
        LayoutBox t = firstText(RenderTestSupport.box(p, "v"));
        double cw = t.getHeight() / 6;
        double mx = t.getX() + t.getWidth() / 2;
        Check.equal(0, p.hitTestDocument(mx, t.getY() + 0.2 * cw).getTextOffset(), "the first character is at the top");
        Check.equal(1, p.hitTestDocument(mx, t.getY() + 1.2 * cw).getTextOffset(), "second character");
        Check.equal(3, p.hitTestDocument(mx, t.getY() + 3.2 * cw).getTextOffset(), "fourth character");
        Check.equal(6, p.hitTestDocument(mx, t.getY() + 5.9 * cw).getTextOffset(), "the end of the text (nearest caret position after f)");
        Check.equal("v", p.hitTestDocument(mx, t.getY() + 10).getElement().getAttribute("id"), "the element under the point");
    }

    /**
     * A horizontal box inside vertical text is painted upright.
     * <p>
     * 세로쓰기 안의 가로 상자는 똑바로 그려진다.
     */
    public void testHorizontalIslandIsPaintedUpright() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='font:20px monospace;writing-mode:vertical-rl;height:100px;line-height:24px;color:#000'>"
                + "<div id=h style='writing-mode:horizontal-tb;width:80px;line-height:24px'>HHH</div></div>", 200, 150, null);
        LayoutBox t = firstText(RenderTestSupport.box(p, "h"));
        Check.equal(0, t.getTextRotation(), "not rotated");
        Check.isTrue(t.getWidth() > t.getHeight(), "wider than high: " + t.getWidth() + " x " + t.getHeight());
    }
}
