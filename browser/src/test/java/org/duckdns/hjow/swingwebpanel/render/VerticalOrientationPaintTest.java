package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting and the hit testing of upright and combined characters in vertical text (plan item P10-03-e, CSS Writing
 * Modes Level 3): an upright Hangul syllable has the ink of the same syllable in horizontal text (not that of a rotated one), it is
 * centered across the line and in its cell of one em along the line, the next character is one em further down, composed text
 * (<code>text-combine-upright</code>) is horizontal, compressed into one em and centered, and a click finds the character under the
 * point in the cells of upright text. The checks compare the ink of the glyphs with the ink of the horizontal rendering of the same
 * text and the centers of the ink, not whole screens, so they do not depend on a particular font.
 * <p>
 * 세로쓰기에서 똑바로 선 글자와 조합한 글자의 그리기와 히트 테스트 시험(계획 P10-03-e, CSS Writing Modes Level 3): 똑바로 선 한글 음절은 가로쓰기의 같은 음절과 같은
 * 잉크를 가지며(돌린 글자가 아님) 줄을 가로질러 가운데, 줄을 따라 1em 칸 안에 놓이고 다음 글자는 1em 아래이며, 조합한 텍스트(<code>text-combine-upright</code>)는 가로이고
 * 1em 으로 압축되어 가운데에 놓이고, 클릭하면 똑바로 선 텍스트의 칸에서 점 아래의 글자를 찾는다. 화면 전체가 아니라 글리프의 잉크를 같은 텍스트의 가로 렌더링의 잉크와
 * 비교하고 잉크의 중심을 보므로 특정 글꼴에 의존하지 않는다.
 */
public class VerticalOrientationPaintTest {

    /** Hangul syllable HAN used as the glyph under test. 시험 대상 글리프로 쓰는 한글 음절 "한". */
    private static final String HAN = "한";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VerticalOrientationPaintTest() {
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
     * Returns the bounding box {minX, minY, maxX, maxY} and the center of mass {cx, cy} of the ink in a rectangle, or null if there is
     * no ink.
     * <p>
     * 사각형 안 잉크의 경계 상자 {minX, minY, maxX, maxY}와 무게 중심 {cx, cy}를 반환한다. 잉크가 없으면 null.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(제외)
     * @param y1  bottom (exclusive) / 아래쪽(제외)
     * @return {minX, minY, maxX, maxY, cx, cy, count} / {minX, minY, maxX, maxY, cx, cy, 개수}
     */
    private static double[] inkBox(BufferedImage img, int x0, int y0, int x1, int y1) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -1;
        double maxY = -1;
        double sx = 0;
        double sy = 0;
        int n = 0;
        // 영역의 모든 화소를 훑으며 잉크의 범위와 합을 모은다.
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (!ink(img, x, y)) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                sx += x;
                sy += y;
                n++;
            }
        }
        if (n == 0) {
            return null;
        }
        return new double[] { minX, minY, maxX, maxY, sx / n, sy / n, n };
    }

    /**
     * Returns the share of the ink pixels of a reference rendering that are found (within one pixel) in a second rendering, after the
     * two ink boxes are aligned at their top left corners; {@code rotate} turns the reference a quarter turn clockwise first.
     * <p>
     * 기준 렌더링의 잉크 화소 중 두 잉크 상자의 왼쪽 위를 맞춘 뒤 두 번째 렌더링에서 (한 화소 안에서) 발견되는 비율을 반환한다. {@code rotate} 이면 기준을 먼저 시계
     * 방향으로 사분원 돌린다.
     *
     * @param ref    the reference image / 기준 이미지
     * @param rb     ink box of the reference / 기준의 잉크 상자
     * @param other  the second image / 두 번째 이미지
     * @param ob     ink box of the second image / 두 번째 이미지의 잉크 상자
     * @param rotate true to turn the reference / 기준을 돌리면 true
     * @return the share between 0 and 1 / 0 과 1 사이의 비율
     */
    private static double overlap(BufferedImage ref, double[] rb, BufferedImage other, double[] ob, boolean rotate) {
        int rw = (int) (rb[2] - rb[0]) + 1;
        int rh = (int) (rb[3] - rb[1]) + 1;
        int total = 0;
        int found = 0;
        // 기준의 잉크 화소마다 (돌렸다면 돌린 위치에서) 두 번째 이미지의 이웃 화소를 본다.
        for (int dy = 0; dy < rh; dy++) {
            for (int dx = 0; dx < rw; dx++) {
                if (!ink(ref, (int) rb[0] + dx, (int) rb[1] + dy)) {
                    continue;
                }
                total++;
                int tx = rotate ? rh - 1 - dy : dx;
                int ty = rotate ? dx : dy;
                boolean hit = false;
                // 안티앨리어싱과 반올림 때문에 한 화소 안쪽의 이웃까지 본다.
                for (int ox = -1; ox <= 1 && !hit; ox++) {
                    for (int oy = -1; oy <= 1 && !hit; oy++) {
                        hit = ink(other, (int) ob[0] + tx + ox, (int) ob[1] + ty + oy);
                    }
                }
                if (hit) {
                    found++;
                }
            }
        }
        Check.isTrue(total > 50, "the reference has ink: " + total);
        return (double) found / total;
    }

    /**
     * An upright Hangul syllable in a vertical line has the ink of the same syllable in horizontal text, and not that of the syllable
     * rotated by a quarter turn.
     * <p>
     * 세로 줄에서 똑바로 선 한글 음절은 가로쓰기의 같은 음절의 잉크를 갖고 사분원 돌린 음절의 잉크가 아니다.
     */
    public void testUprightGlyphIsNotRotated() {
        String font = "font:40px sans-serif;line-height:50px;color:#000;";
        RenderedPage ph = RenderTestSupport.page("<div id=h style='" + font + "'>" + HAN + "</div>", 300, 300, null);
        LayoutBox th = firstText(RenderTestSupport.box(ph, "h"));
        RenderedPage pv = RenderTestSupport.page("<div id=v style='" + font + "writing-mode:vertical-rl;height:200px'>" + HAN + "</div>", 300, 300, null);
        LayoutBox tv = firstText(RenderTestSupport.box(pv, "v"));
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, tv.getGlyphMode(), "the syllable is upright in mixed orientation");
        BufferedImage ih = RenderTestSupport.paint(ph, 300, 300);
        BufferedImage iv = RenderTestSupport.paint(pv, 300, 300);
        double[] bh = inkBox(ih, (int) th.getX(), (int) th.getY(), (int) (th.getX() + th.getWidth()) + 2, (int) (th.getY() + th.getHeight()) + 2);
        double[] bv = inkBox(iv, (int) tv.getX() - 2, (int) tv.getY() - 2, (int) (tv.getX() + tv.getWidth()) + 2, (int) (tv.getY() + tv.getHeight()) + 2);
        Check.notNull(bh, "horizontal ink");
        Check.notNull(bv, "vertical ink");
        Check.near(bh[2] - bh[0], bv[2] - bv[0], 1.5, "the glyph is as wide as in horizontal text");
        Check.near(bh[3] - bh[1], bv[3] - bv[1], 1.5, "the glyph is as high as in horizontal text");
        double upright = overlap(ih, bh, iv, bv, false);
        double rotated = overlap(ih, bh, iv, bv, true);
        Check.isTrue(upright > 0.9, "the ink is that of the horizontal rendering: " + upright);
        Check.isTrue(rotated < upright - 0.15, "and not that of a rotated glyph: rotated " + rotated + " upright " + upright);
        // text-orientation: sideways 는 같은 음절을 눕혀서 그린다.
        RenderedPage ps = RenderTestSupport.page("<div id=v style='" + font + "writing-mode:vertical-rl;text-orientation:sideways;height:200px'>" + HAN + "</div>", 300, 300, null);
        LayoutBox ts = firstText(RenderTestSupport.box(ps, "v"));
        BufferedImage is = RenderTestSupport.paint(ps, 300, 300);
        double[] bs = inkBox(is, (int) ts.getX() - 2, (int) ts.getY() - 2, (int) (ts.getX() + ts.getWidth()) + 2, (int) (ts.getY() + ts.getHeight()) + 2);
        Check.isTrue(overlap(ih, bh, is, bs, true) > 0.9, "sideways text is the rotated glyph");
    }

    /**
     * The glyph is centered across the line and the characters follow each other in cells of one em along the line: the centers of the
     * ink of two syllables are one em apart, each in its own cell.
     * <p>
     * 글리프는 줄을 가로질러 가운데에 놓이고 글자는 줄을 따라 1em 칸에서 차례로 놓인다: 두 음절의 잉크 중심은 1em 떨어져 있고 각각 자기 칸 안에 있다.
     */
    public void testUprightCells() {
        String font = "font:40px sans-serif;line-height:60px;color:#000;";
        RenderedPage p = RenderTestSupport.page("<div id=v style='" + font + "writing-mode:vertical-rl;height:300px'>" + HAN + HAN + HAN + "</div>", 300, 300, null);
        LayoutBox v = RenderTestSupport.box(p, "v");
        BufferedImage img = RenderTestSupport.paint(p, 300, 300);
        int x0 = (int) v.getX();
        int x1 = (int) (v.getX() + v.getWidth());
        int top = (int) v.getY();
        double[] c0 = inkBox(img, x0, top, x1, top + 40);
        double[] c1 = inkBox(img, x0, top + 40, x1, top + 80);
        double[] c2 = inkBox(img, x0, top + 80, x1, top + 120);
        Check.notNull(c0, "first cell has ink");
        Check.notNull(c1, "second cell has ink");
        Check.notNull(c2, "third cell has ink");
        Check.near(c0[5] + 40, c1[5], 1.5, "the second glyph is one em below the first");
        Check.near(c1[5] + 40, c2[5], 1.5, "and the third one em below the second");
        Check.near(c0[4], c1[4], 1.0, "the glyphs are in the same column");
        Check.near(v.getX() + v.getWidth() / 2, c0[4], 6.0, "centered across the line");
        Check.isTrue(c0[5] > top + 12 && c0[5] < top + 28, "the ink of the first glyph is in the middle of its cell: " + (c0[5] - top));
        Check.equal(0, RenderTestSupport.ink(img, x0, top + 124, x1, top + 300), "nothing below the three cells");
    }

    /**
     * Text composed with <code>text-combine-upright: all</code> is painted horizontally, centered in one cell and compressed to one em
     * if it is wider.
     * <p>
     * <code>text-combine-upright: all</code> 로 조합한 텍스트는 가로로 그려지고 한 칸의 가운데에 놓이며 더 넓으면 1em 으로 압축된다.
     */
    public void testCombinedTextIsHorizontalAndCompressed() {
        String font = "font:40px sans-serif;line-height:60px;color:#000;";
        RenderedPage p = RenderTestSupport.page("<div id=v style='" + font + "writing-mode:vertical-rl;height:300px'><span id=c style='text-combine-upright:all'>2025</span></div>", 300, 300, null);
        LayoutBox c = firstText(RenderTestSupport.box(p, "v"));
        Check.equal(LayoutBox.GLYPHS_COMBINED, c.getGlyphMode(), "combined");
        Check.near(40, c.getHeight(), 0.01, "one em along the line");
        BufferedImage img = RenderTestSupport.paint(p, 300, 300);
        double[] b = inkBox(img, (int) c.getX() - 4, (int) c.getY() - 4, (int) (c.getX() + c.getWidth()) + 4, (int) (c.getY() + c.getHeight()) + 4);
        Check.notNull(b, "ink");
        double plain = LayoutBoxAdvance.advance(c, "2025");
        Check.isTrue(plain > 45, "the digits are wider than one em when set normally: " + plain);
        Check.isTrue(b[2] - b[0] + 1 <= 40 + 2, "the composition is compressed to one em: " + (b[2] - b[0] + 1));
        Check.isTrue(b[2] - b[0] + 1 >= 25, "and is still wide: " + (b[2] - b[0] + 1));
        Check.near(c.getX() + c.getWidth() / 2, b[4], 3.0, "centered across the line");
        Check.near(c.getY() + c.getHeight() / 2, b[5], 6.0, "centered in the cell along the line");
        // 같은 텍스트를 가로로 그린 것보다 높이가 크게 다르지 않다: 글리프가 회전하지 않았다.
        RenderedPage ph = RenderTestSupport.page("<div id=h style='" + font + "'>2025</div>", 300, 300, null);
        LayoutBox th = firstText(RenderTestSupport.box(ph, "h"));
        BufferedImage ihh = RenderTestSupport.paint(ph, 300, 300);
        double[] bh = inkBox(ihh, (int) th.getX(), (int) th.getY(), (int) (th.getX() + th.getWidth()) + 2, (int) (th.getY() + th.getHeight()) + 2);
        Check.near(bh[3] - bh[1], b[3] - b[1], 1.5, "the digits keep their height: they are not rotated");
        Check.isTrue(b[2] - b[0] < bh[2] - bh[0] - 3, "but are narrower than in plain horizontal text");
    }

    /**
     * Measures with the font of a box the advance of a string (helper of the checks).
     * <p>
     * 상자의 글꼴로 문자열의 advance 를 잰다(시험의 도우미).
     */
    private static final class LayoutBoxAdvance {
        /**
         * Private constructor: static helper class.
         * <p>
         * 비공개 생성자: 정적 도우미 클래스이다.
         */
        private LayoutBoxAdvance() {
        }

        /**
         * Returns the advance of a string in the font of a box.
         * <p>
         * 상자의 글꼴에서 문자열의 advance 를 반환한다.
         *
         * @param b the box / 상자
         * @param s the string / 문자열
         * @return the advance / advance
         */
        static double advance(LayoutBox b, String s) {
            return b.getFont().getStringBounds(s, new java.awt.font.FontRenderContext(null, true, true)).getWidth();
        }
    }

    /**
     * A click on upright text finds the caret position nearest to the point in the cell of the character: the first half of a cell is
     * before the character, the second half after it.
     * <p>
     * 똑바로 선 텍스트를 클릭하면 글자의 칸에서 점에 가장 가까운 캐럿 위치를 찾는다: 칸의 앞쪽 절반은 글자 앞, 뒤쪽 절반은 글자 뒤이다.
     */
    public void testHitTestOnUprightText() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='font:20px sans-serif;writing-mode:vertical-rl;height:200px;line-height:30px'>"
                + HAN + HAN + HAN + HAN + "</div>", 200, 300, null);
        LayoutBox v = RenderTestSupport.box(p, "v");
        double mx = v.getX() + v.getWidth() / 2;
        double top = v.getY();
        Check.equal(0, p.hitTestDocument(mx, top + 5).getTextOffset(), "first half of the first cell");
        Check.equal(1, p.hitTestDocument(mx, top + 15).getTextOffset(), "second half of the first cell");
        Check.equal(1, p.hitTestDocument(mx, top + 25).getTextOffset(), "first half of the second cell");
        Check.equal(3, p.hitTestDocument(mx, top + 55).getTextOffset(), "second half of the third cell");
        Check.equal(4, p.hitTestDocument(mx, top + 79).getTextOffset(), "the end of the text");
        Check.equal("v", p.hitTestDocument(mx, top + 25).getElement().getAttribute("id"), "the element under the point");
    }

    /**
     * A click on a combined group is before the group in its first half and after it in the second half.
     * <p>
     * 조합 묶음을 클릭하면 앞쪽 절반은 묶음 앞, 뒤쪽 절반은 묶음 뒤이다.
     */
    public void testHitTestOnCombinedText() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='font:20px sans-serif;writing-mode:vertical-rl;height:200px;line-height:30px'>"
                + "<span style='text-combine-upright:all'>2025</span></div>", 200, 300, null);
        LayoutBox v = RenderTestSupport.box(p, "v");
        double mx = v.getX() + v.getWidth() / 2;
        Check.equal(0, p.hitTestDocument(mx, v.getY() + 4).getTextOffset(), "first half");
        Check.equal(4, p.hitTestDocument(mx, v.getY() + 16).getTextOffset(), "second half");
    }

    /**
     * Fragments of upright text sit in the display list as text commands whose area covers their box, so that partial repainting finds
     * them.
     * <p>
     * 똑바로 선 텍스트의 조각은 디스플레이 리스트에서 자기 상자를 덮는 영역의 텍스트 명령이므로 부분 다시 그리기가 찾을 수 있다.
     */
    public void testUprightCommandsCoverTheirBoxes() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='font:20px sans-serif;writing-mode:vertical-rl;height:200px;line-height:30px'>"
                + HAN + HAN + "A</div>", 200, 300, null);
        List<DisplayCommand> texts = new ArrayList<DisplayCommand>();
        List<DisplayCommand> all = p.getDisplayList().getCommands();
        // 텍스트 명령만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getType() == DisplayCommand.Type.TEXT) {
                texts.add(all.get(i));
            }
        }
        Check.isTrue(texts.size() >= 3, "a text command for each fragment: " + texts.size());
        LayoutBox v = RenderTestSupport.box(p, "v");
        // 명령마다 영역이 조각 상자들 중 하나를 덮는지 본다.
        for (int i = 0; i < texts.size(); i++) {
            Check.isTrue(texts.get(i).getBounds().getY() < v.getY() + v.getHeight() + 3, "command " + i + " is inside the vertical box");
        }
    }
}
