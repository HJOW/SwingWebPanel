package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport;
import org.duckdns.hjow.swingwebpanel.layout.TextEmphasis;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;

/**
 * Tests of <code>text-emphasis</code> in vertical text (plan item P10-08-d, CSS Text Decoration Level 4 sections 3.1 and 3.2, https://www.w3.org/TR/css-text-decor-4/#emphasis-marks):
 * in a vertical typographic mode (<code>writing-mode: vertical-rl</code> or <code>vertical-lr</code>) the marks stand upright at the right of the glyphs
 * (<code>over right</code>, the default) or at the left (<code>under</code> or <code>left</code>), at the center of each character along the line (the cell of an
 * upright character, the advance of a sideways one), the line grows by the size of the marks on that side, and the default shape of <code>filled</code> and
 * <code>open</code> alone is the sesame. The marks are red, so that their pixels are told from the black text.
 * <p>
 * 세로 텍스트의 <code>text-emphasis</code> 시험(계획 P10-08-d, CSS Text Decoration Level 4 3.1, 3.2 절): 세로 조판 모드(<code>writing-mode: vertical-rl</code> 또는
 * <code>vertical-lr</code>)에서 점은 글리프의 오른쪽(<code>over right</code>, 기본)이나 왼쪽(<code>under</code> 또는 <code>left</code>)에 똑바로 서고, 줄을 따라 글자마다
 * 가운데(똑바로 선 글자는 칸, 눕힌 글자는 advance)에 놓이며, 줄은 그쪽의 점 크기만큼 커지고, <code>filled</code> 나 <code>open</code> 만 있을 때의 기본 모양은 sesame 이다.
 * 점은 빨간색이어서 검은 글자와 구별된다.
 */
public class VerticalEmphasisPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VerticalEmphasisPaintTest() {
    }

    /**
     * Returns the page of a vertical text of three characters at (30, 10) with a style.
     * <p>
     * 스타일이 있는, (30, 10) 의 세 글자 세로 텍스트의 페이지를 반환한다.
     *
     * @param style the style / 스타일
     * @return the page / 페이지
     */
    private static RenderedPage page(String style) {
        return RenderTestSupport.page("<div id=v style='position:absolute;left:30px;top:10px;writing-mode:vertical-rl;font:20px monospace;line-height:30px;" + style
                + "'>漢字漢</div>", 200, 150, null);
    }

    /**
     * Counts the red pixels of a rectangle.
     * <p>
     * 사각형의 빨간 픽셀을 센다.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @return the number of red pixels / 빨간 픽셀 수
     */
    private static int red(BufferedImage img, double x0, double y0, double x1, double y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 빨강이 뚜렷한 것을 센다.
        for (int y = (int) Math.max(0, Math.floor(y0)); y < Math.min(img.getHeight(), Math.ceil(y1)); y++) {
            for (int x = (int) Math.max(0, Math.floor(x0)); x < Math.min(img.getWidth(), Math.ceil(x1)); x++) {
                int c = img.getRGB(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r > 180 && g < 120 && b < 120) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the first text fragment of a page.
     * <p>
     * 페이지의 첫 텍스트 조각을 반환한다.
     *
     * @param p the page / 페이지
     * @return the box / 상자
     */
    private static LayoutBox text(RenderedPage p) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(RenderTestSupport.box(p, "v"), LayoutBox.Type.TEXT, out);
        Check.isFalse(out.isEmpty(), "the text fragment");
        return out.get(0);
    }

    /**
     * The marks are at the right of an upright run, one at the center of each character cell, and not at the left.
     * <p>
     * 점은 똑바로 선 조각의 오른쪽에 있으며 글자 칸의 가운데마다 하나씩이고 왼쪽에는 없다.
     */
    public void testMarksAtTheRightOfUprightText() {
        RenderedPage p = page("text-emphasis-style:filled circle;text-emphasis-color:#f00");
        BufferedImage img = RenderTestSupport.paint(p, 200, 150);
        LayoutBox t = text(p);
        double reserve = TextEmphasis.of(RenderTestSupport.box(p, "v").getStyle()).reserve();
        double right0 = t.getX() + t.getWidth();
        Check.isTrue(red(img, right0, t.getY(), right0 + reserve, t.getY() + t.getHeight()) >= 12, "marks in the space at the right of the text");
        Check.equal(0, red(img, t.getX() - reserve - 2, t.getY(), t.getX() + t.getWidth(), t.getY() + t.getHeight()), "no mark at the left or on the glyphs");
        // 글자 칸(1em 높이)의 가운데마다 점이 있고 칸의 경계 근처에는 없다.
        for (int k = 0; k < 3; k++) {
            double cy = t.getY() + k * 20 + 10;
            Check.isTrue(red(img, right0, cy - 3, right0 + reserve, cy + 3) >= 4, "a mark at the center of cell " + k);
            Check.equal(0, red(img, right0, t.getY() + k * 20 + 18, right0 + reserve, t.getY() + k * 20 + 22), "none at the boundary after cell " + k);
        }
    }

    /**
     * <code>text-emphasis-position: left</code> (or <code>under</code>) puts the marks at the left; the line grows on that side.
     * <p>
     * <code>text-emphasis-position: left</code>(또는 <code>under</code>)는 점을 왼쪽에 두며 줄은 그쪽으로 커진다.
     */
    public void testMarksAtTheLeft() {
        RenderedPage p = page("text-emphasis-style:filled circle;text-emphasis-color:#f00;text-emphasis-position:under left");
        BufferedImage img = RenderTestSupport.paint(p, 200, 150);
        LayoutBox t = text(p);
        double reserve = TextEmphasis.of(RenderTestSupport.box(p, "v").getStyle()).reserve();
        Check.isTrue(red(img, t.getX() - reserve, t.getY(), t.getX(), t.getY() + t.getHeight()) >= 12, "marks at the left of the text");
        Check.equal(0, red(img, t.getX() + t.getWidth(), t.getY(), t.getX() + t.getWidth() + reserve + 2, t.getY() + t.getHeight()), "none at the right");
        // over 와 under 는 세로 조판 모드에서 상관없고 right 가 기본이므로 under 만 쓰면 오른쪽이다.
        RenderedPage under = page("text-emphasis-style:filled circle;text-emphasis-color:#f00;text-emphasis-position:under");
        LayoutBox u = text(under);
        Check.isTrue(red(RenderTestSupport.paint(under, 200, 150), u.getX() + u.getWidth(), u.getY(), u.getX() + u.getWidth() + reserve, u.getY() + u.getHeight()) >= 12,
                "under alone is the right side in vertical text");
    }

    /**
     * The line grows by the size of the marks on the side where they are.
     * <p>
     * 줄은 점이 있는 쪽으로 점의 크기만큼 커진다.
     */
    public void testLineGrows() {
        // 시험 도우미는 마지막으로 만든 문서에서 요소를 찾으므로 페이지마다 값을 읽은 뒤 다음 페이지를 만든다.
        double plainWidth = RenderTestSupport.box(page(""), "v").getWidth();
        RenderedPage marked = page("text-emphasis-style:filled circle");
        double reserve = TextEmphasis.of(RenderTestSupport.box(marked, "v").getStyle()).reserve();
        double grown = RenderTestSupport.box(marked, "v").getWidth() - plainWidth;
        Check.near(reserve, grown, 1.0, "the box is wider by the height of the marks");
        Check.isTrue(reserve > 5, "the marks have a size: " + reserve);
    }

    /**
     * The default shape in vertical text is the sesame; the horizontal text keeps the circle; <code>vertical-lr</code> works as well.
     * <p>
     * 세로 텍스트의 기본 모양은 sesame 이다. 가로 텍스트는 원을 유지한다. <code>vertical-lr</code> 도 같다.
     */
    public void testDefaultShapeAndVerticalLr() {
        LayoutResult r = LayoutTestSupport.layout("<p id=v style='writing-mode:vertical-rl;text-emphasis-style:filled'>x</p><p id=l style='writing-mode:vertical-lr;text-emphasis-style:open'>x</p>"
                + "<p id=h style='text-emphasis-style:filled'>x</p>", 600);
        TextEmphasis v = TextEmphasis.of(LayoutTestSupport.box(r, "v").getStyle());
        Check.equal("﹅", v.getMark(), "filled alone is the filled sesame in vertical-rl");
        Check.isTrue(v.isVertical(), "vertical typographic mode");
        Check.equal("﹆", TextEmphasis.of(LayoutTestSupport.box(r, "l").getStyle()).getMark(), "open alone is the open sesame in vertical-lr");
        Check.equal("●", TextEmphasis.of(LayoutTestSupport.box(r, "h").getStyle()).getMark(), "the circle in horizontal text");
        Check.isFalse(TextEmphasis.of(LayoutTestSupport.box(r, "h").getStyle()).isVertical(), "horizontal");
        RenderedPage p = RenderTestSupport.page("<div id=v style='position:absolute;left:30px;top:10px;writing-mode:vertical-lr;font:20px monospace;line-height:30px;"
                + "text-emphasis-style:filled circle;text-emphasis-color:#f00'>漢字</div>", 200, 150, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 150);
        LayoutBox t = text(p);
        Check.isTrue(red(img, t.getX() + t.getWidth(), t.getY(), t.getX() + t.getWidth() + 12, t.getY() + t.getHeight()) >= 8, "vertical-lr: marks at the right too");
    }

    /**
     * Sideways text (Latin letters in vertical text) gets a mark per letter along the advances, and spaces get none.
     * <p>
     * 눕힌 텍스트(세로 텍스트 안의 라틴 글자)는 advance 를 따라 글자마다 점 하나를 받고 공백에는 없다.
     */
    public void testSidewaysRunAndSpaces() {
        RenderedPage p = RenderTestSupport.page("<div id=v style='position:absolute;left:30px;top:10px;writing-mode:vertical-rl;font:20px monospace;line-height:30px;"
                + "text-emphasis-style:filled circle;text-emphasis-color:#f00'>a b</div>", 200, 150, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 150);
        LayoutBox t = text(p);
        double reserve = TextEmphasis.of(RenderTestSupport.box(p, "v").getStyle()).reserve();
        double right0 = t.getX() + t.getWidth();
        double adv = 12;
        // 눕힌 monospace 20px 글자의 advance 는 약 12px 이다: a(0..12), 공백(12..24), b(24..36).
        Check.isTrue(red(img, right0, t.getY(), right0 + reserve, t.getY() + 12) >= 4, "a mark for the first letter");
        Check.equal(0, red(img, right0, t.getY() + adv + 3, right0 + reserve, t.getY() + 2 * adv - 3), "none for the space");
        Check.isTrue(red(img, right0, t.getY() + 2 * adv, right0 + reserve, t.getY() + 3 * adv) >= 4, "a mark for the last letter");
    }
}
