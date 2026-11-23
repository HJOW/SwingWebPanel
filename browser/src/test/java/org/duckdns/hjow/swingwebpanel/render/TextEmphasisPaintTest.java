package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;
import org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport;
import org.duckdns.hjow.swingwebpanel.layout.TextEmphasis;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the emphasis marks (plan item P10-08, CSS Text Decoration Module Level 4, https://www.w3.org/TR/css-text-decor-4/, W3C Working Draft 4 May 2022): the mark
 * that the style gives (shapes, filling, a string, the shorthand), the characters that get a mark, the room that the marks add to the line, and the pixels: one mark per
 * character, centered, in the color of the property, over or under the text.
 * <p>
 * 강조점 시험(계획 P10-08, CSS Text Decoration Module Level 4): 스타일이 주는 점(모양, 채움, 문자열, 단축 속성), 점이 붙는 글자, 점이 줄에 더하는 공간, 그리고 화소: 글자마다
 * 점 하나가 가운데에 속성의 색으로 텍스트 위나 아래에 그려진다.
 */
public class TextEmphasisPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextEmphasisPaintTest() {
    }

    /**
     * Returns the emphasis of the element <code>p</code> that has the style.
     * <p>
     * 스타일을 가진 요소 <code>p</code> 의 강조점을 반환한다.
     *
     * @param style the style / 스타일
     * @return the marks or null / 강조점. 없으면 null
     */
    private static TextEmphasis emphasis(String style) {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='" + style + "'>x</p>", 600);
        ComputedStyle s = LayoutTestSupport.box(r, "p").getStyle();
        return TextEmphasis.of(s);
    }

    /**
     * The mark of the style: the shapes with both fillings, <code>filled</code> and <code>open</code> alone give the circle, a string gives its first grapheme cluster, and
     * wrong values are dropped.
     * <p>
     * 스타일의 점: 두 채움의 모양, <code>filled</code> 나 <code>open</code> 만 있으면 원, 문자열은 첫 글자 단위, 틀린 값은 버려진다.
     */
    public void testMarkOfTheStyle() {
        Check.isNull(emphasis(""), "no emphasis by default");
        Check.isNull(emphasis("text-emphasis-style:none"), "none");
        Check.equal("●", emphasis("text-emphasis-style:filled").getMark(), "filled alone is the filled circle");
        Check.equal("○", emphasis("text-emphasis-style:open").getMark(), "open alone is the open circle");
        Check.equal("•", emphasis("text-emphasis-style:dot").getMark(), "dot is filled by default");
        Check.equal("◦", emphasis("text-emphasis-style:open dot").getMark(), "open dot");
        Check.equal("◦", emphasis("text-emphasis-style:dot open").getMark(), "the keywords are in any order");
        Check.equal("◉", emphasis("text-emphasis-style:filled double-circle").getMark(), "filled double circle");
        Check.equal("◎", emphasis("text-emphasis-style:open double-circle").getMark(), "open double circle");
        Check.equal("▲", emphasis("text-emphasis-style:triangle").getMark(), "filled triangle");
        Check.equal("△", emphasis("text-emphasis-style:open triangle").getMark(), "open triangle");
        Check.equal("﹅", emphasis("text-emphasis-style:sesame").getMark(), "filled sesame");
        Check.equal("﹆", emphasis("text-emphasis-style:open sesame").getMark(), "open sesame");
        Check.equal("★", emphasis("text-emphasis-style:\"★\"").getMark(), "a string is the mark itself");
        Check.equal("★", emphasis("text-emphasis-style:\"★☆\"").getMark(), "only the first character of a string is used");
        Check.equal("é", emphasis("text-emphasis-style:\"éx\"").getMark(), "a combining mark belongs to its character");
        Check.isNull(emphasis("text-emphasis-style:square"), "an unknown shape drops the declaration");
        Check.isNull(emphasis("text-emphasis-style:filled open"), "two fillings are invalid");
        Check.isNull(emphasis("text-emphasis-style:\"\""), "an empty string gives no mark");
    }

    /**
     * The shorthand sets the style and the color and does not touch the position; the color defaults to the text color; the position is over unless it says under.
     * <p>
     * 단축 속성은 스타일과 색을 정하고 위치는 건드리지 않는다. 색의 기본은 글자 색이고 위치는 under 라고 쓰지 않으면 over 이다.
     */
    public void testShorthandColorAndPosition() {
        TextEmphasis e = emphasis("color:#00f;text-emphasis:open triangle");
        Check.equal("△", e.getMark(), "the shorthand sets the style");
        Check.equal(0xFF0000FF, e.getColor(), "the color is the text color by default");
        Check.equal(0xFFFF0000, emphasis("color:#00f;text-emphasis:dot red").getColor(), "a color in the shorthand");
        Check.equal(0xFF00AA00, emphasis("text-emphasis-style:dot;text-emphasis-color:#0a0").getColor(), "the color property");
        Check.equal(0xFF00FF00, emphasis("color:#00f;text-emphasis-style:dot;text-emphasis-color:#0f0;color:#00f").getColor(), "the color does not follow the text color once set");
        Check.isTrue(e.isOver(), "over by default");
        Check.isTrue(emphasis("text-emphasis-style:dot;text-emphasis-position:over left").isOver(), "over left");
        Check.isFalse(emphasis("text-emphasis-style:dot;text-emphasis-position:under right").isOver(), "under right");
        Check.isFalse(emphasis("text-emphasis-style:dot;text-emphasis-position:left under").isOver(), "the order of the position keywords is free");
        Check.isFalse(emphasis("text-emphasis-position:under;text-emphasis:dot").isOver(), "the shorthand does not reset the position");
        Check.isTrue(emphasis("text-emphasis-style:dot").getFont().getSize2D() > 0, "the mark has a font");
    }

    /**
     * The marks are half as large as the text; they inherit with the other text properties, and a child can turn them off.
     * <p>
     * 점은 글자의 절반 크기이며 다른 텍스트 속성처럼 상속되고 자식이 끌 수 있다.
     */
    public void testSizeAndInheritance() {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='font-size:20px;text-emphasis:circle'><b id=b>x</b><i id=i style='text-emphasis:none'>y</i></p>", 600);
        TextEmphasis e = TextEmphasis.of(LayoutTestSupport.box(r, "p").getStyle());
        Check.near(10, e.getFont().getSize2D(), 0.01, "the mark is 50 percent of the font size");
        Check.notNull(TextEmphasis.of(LayoutTestSupport.box(r, "b").getStyle()), "the child inherits the marks");
        Check.isNull(TextEmphasis.of(LayoutTestSupport.box(r, "i").getStyle()), "the child that says none has none");
    }

    /**
     * Which characters get a mark: letters, digits, symbols and CJK characters do; spaces, other separators, control characters and most punctuation do not, but the signs
     * that the specification lists (<code>#</code>, <code>%</code>, <code>&amp;</code>, <code>@</code>, the section sign ...) do.
     * <p>
     * 어떤 글자에 점이 붙는지: 글자, 숫자, 기호, CJK 문자에는 붙고, 공백과 다른 구분자, 제어 문자, 대부분의 문장 부호에는 붙지 않지만 규격이 나열한 기호(<code>#</code>,
     * <code>%</code>, <code>&amp;</code>, <code>@</code>, 절 기호 등)에는 붙는다.
     */
    public void testWhichCharactersAreMarked() {
        String yes = "aZ09$+한漢あé#%&@§¶‰";
        // 점이 붙어야 하는 글자를 하나씩 확인한다.
        for (int i = 0; i < yes.length(); i++) {
            Check.isTrue(TextEmphasis.marked(yes.charAt(i)), "U+" + Integer.toHexString(yes.charAt(i)) + " is marked");
        }
        String no = " \t\n.,;:!?-()[]{}\"'" + new String(new char[] {(char) 0x00A0, (char) 0x3000, (char) 0x2003, (char) 0x3001, (char) 0x3002, (char) 0x300C, (char) 0x300D, (char) 0x2026, (char) 0x200B, (char) 0x0001, (char) 0x007F});
        // 점이 붙지 않아야 하는 글자를 하나씩 확인한다.
        for (int i = 0; i < no.length(); i++) {
            Check.isFalse(TextEmphasis.marked(no.charAt(i)), "U+" + Integer.toHexString(no.charAt(i)) + " is not marked");
        }
        Check.isTrue(TextEmphasis.marked(0x20000), "a character outside the BMP is marked");
    }

    /**
     * Returns the text fragments of the first line of the paragraph.
     * <p>
     * 문단의 첫 줄의 텍스트 조각을 반환한다.
     *
     * @param style the style / 스타일
     * @return the fragments / 조각
     */
    private static List<LayoutBox> fragments(String style) {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;font-size:20px;" + style + "'>abc</p>", 600);
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        // 첫 줄의 텍스트 조각을 모은다.
        for (LayoutBox c : LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0).getChildren()) {
            if (c.getType() == LayoutBox.Type.TEXT) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * The marks add room like ruby text: above the text for <code>over</code> and below it for <code>under</code>; the text fragment itself keeps its width.
     * <p>
     * 점은 ruby 텍스트처럼 공간을 더한다: <code>over</code> 이면 텍스트 위에, <code>under</code> 이면 아래에. 텍스트 조각의 너비는 그대로이다.
     */
    public void testRoomOfTheMarks() {
        LayoutBox plain = line("");
        LayoutBox over = line("text-emphasis:dot");
        LayoutBox under = line("text-emphasis:dot;text-emphasis-position:under");
        TextEmphasis e = emphasis("font-size:20px;text-emphasis:dot");
        Check.isTrue(e.reserve() > 3, "the marks need room");
        Check.near(plain.getHeight() + e.reserve(), over.getHeight(), 0.01, "over makes the line taller by the room of the marks");
        Check.near(plain.getBaseline() + e.reserve(), over.getBaseline(), 0.01, "and moves the baseline down by it");
        Check.near(plain.getHeight() + e.reserve(), under.getHeight(), 0.01, "under makes the line taller by the same room");
        Check.near(plain.getBaseline(), under.getBaseline(), 0.01, "but keeps the baseline");
        Check.near(fragments("").get(0).getWidth(), fragments("text-emphasis:dot").get(0).getWidth(), 0.01, "the width of the text does not change");
        // 줄 높이가 충분히 크면 점은 줄 높이 안의 여유에 들어가므로 줄이 커지지 않아도 되는지 본다: 점의 공간은 상자 위에 더해지므로 더 커진다.
        Check.isTrue(line("line-height:60px;text-emphasis:dot").getHeight() >= line("line-height:60px").getHeight(), "a tall line never gets shorter");
    }

    /**
     * Returns the first line box of the paragraph.
     * <p>
     * 문단의 첫 줄 상자를 반환한다.
     *
     * @param style the style / 스타일
     * @return the line box / 줄 상자
     */
    private static LayoutBox line(String style) {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;font-size:20px;" + style + "'>abc</p>", 600);
        return LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0);
    }

    /**
     * Paints a paragraph of the text <code>ab c</code> in 20px monospaced type and returns the image.
     * <p>
     * 20px 고정폭 글꼴의 <code>ab c</code> 문단을 그려 이미지를 반환한다.
     *
     * @param style the style / 스타일
     * @return the image / 이미지
     */
    private static BufferedImage paint(String style) {
        RenderedPage p = RenderTestSupport.page("<p id=p style='margin:0;font:20px monospace;" + style + "'>ab c</p>", 200, 80, null);
        return RenderTestSupport.paint(p, 200, 80);
    }

    /**
     * Returns the number of dark pixels of one column cell of the image, between two rows.
     * <p>
     * 이미지의 한 글자 칸의, 두 행 사이에 있는 어두운 화소 수를 반환한다.
     *
     * @param img the image / 이미지
     * @param col the column cell (0 is the first character) / 글자 칸(0 이 첫 글자)
     * @param cw  the width of a cell / 칸의 너비
     * @param y0  first row / 첫 행
     * @param y1  last row, exclusive / 마지막 행(미포함)
     * @return the count / 개수
     */
    private static int cell(BufferedImage img, int col, int cw, int y0, int y1) {
        return RenderTestSupport.ink(img, col * cw, y0, (col + 1) * cw, y1);
    }

    /**
     * The marks are drawn: one over each character that gets one, none over the space, in the color of the property, above the text; the page grows by the room of the
     * marks.
     * <p>
     * 점이 그려진다: 점이 붙는 글자 위에 하나씩, 공백 위에는 없고, 속성의 색으로, 텍스트 위쪽에. 페이지는 점의 공간만큼 커진다.
     */
    public void testMarksOverTheText() {
        LayoutBox frag = fragments("font-family:monospace").get(0);
        int cw = (int) Math.round(frag.getWidth() / 3);
        BufferedImage plain = paint("");
        BufferedImage img = paint("text-emphasis:filled circle;text-emphasis-color:#f00");
        int plainTop = firstInkRow(plain);
        int top = firstInkRow(img);
        Check.isTrue(top < plainTop + 1000, "both images have ink");
        int reserve = (int) Math.round(emphasis("font-size:20px;font-family:monospace;text-emphasis:filled circle").reserve());
        // 점은 텍스트 위쪽의 새로 생긴 공간에 있으므로 글자 자체의 맨 위 행 위에서 잉크가 시작된다.
        int textTop = firstInkRow(paint("text-emphasis:filled circle;text-emphasis-color:#fff"));
        Check.isTrue(textTop >= plainTop + reserve - 2, "the text moves down by the room of the marks: " + textTop + " vs " + plainTop + " + " + reserve);
        Check.isTrue(cw >= 8, "the cell width is known");
        Check.isTrue(redInCell(img, 0, cw, 0, textTop) > 3, "a mark over the first character");
        Check.isTrue(redInCell(img, 1, cw, 0, textTop) > 3, "a mark over the second character");
        Check.equal(0, redInCell(img, 2, cw, 0, textTop), "no mark over the space");
        Check.isTrue(redInCell(img, 3, cw, 0, textTop) > 3, "a mark over the fourth character");
        // 첫 글자 점의 가로 무게중심이 글자 칸의 가운데에 있는지 본다.
        double cx = redCenterX(img, 0, cw, textTop);
        Check.near(cw / 2.0, cx, 2.0, "the mark is centered on the character");
        Check.equal(0, redInCell(plain, 0, cw, 0, 80), "the plain page has no red");
    }

    /**
     * The marks of <code>text-emphasis-position: under</code> are below the text, and a string is drawn as a mark.
     * <p>
     * <code>text-emphasis-position: under</code> 의 점은 텍스트 아래에 있고 문자열은 점으로 그려진다.
     */
    public void testMarksUnderTheText() {
        LayoutBox frag = fragments("font-family:monospace").get(0);
        int cw = (int) Math.round(frag.getWidth() / 3);
        BufferedImage img = paint("text-emphasis:dot;text-emphasis-position:under;text-emphasis-color:#f00");
        int bottom = lastInkRowOutsideRed(img);
        Check.isTrue(redInCell(img, 0, cw, bottom + 1, 80) > 2, "a mark under the first character");
        Check.equal(0, redInCell(img, 2, cw, bottom + 1, 80), "no mark under the space");
        Check.equal(0, redInCell(img, 0, cw, 0, firstInkRow(img)), "nothing over the text for under");
        BufferedImage star = paint("text-emphasis:\"\u25A0\";text-emphasis-color:#f00");
        Check.isTrue(redInCell(star, 1, cw, 0, firstInkRow(paint("text-emphasis:\"\u25A0\";text-emphasis-color:#fff"))) > 2, "a string is drawn as the mark");
    }

    /**
     * Emphasis marks do not paint when the style says none, and a hidden element has none.
     * <p>
     * 스타일이 none 이면 강조점을 그리지 않고, 숨겨진 요소에도 없다.
     */
    public void testNoMarks() {
        BufferedImage img = paint("text-emphasis:none;text-emphasis-color:#f00");
        Check.equal(0, redInCell(img, 0, 12, 0, 80), "none paints no marks");
        BufferedImage hidden = paint("text-emphasis:dot;text-emphasis-color:#f00;visibility:hidden");
        Check.equal(0, redInCell(hidden, 0, 12, 0, 80), "a hidden element paints none");
    }

    /**
     * Returns the first row that has dark ink.
     * <p>
     * 어두운 잉크가 있는 첫 행을 반환한다.
     *
     * @param img the image / 이미지
     * @return the row or 1000 / 행. 없으면 1000
     */
    private static int firstInkRow(BufferedImage img) {
        // 위에서 아래로 훑어 처음으로 잉크가 있는 행을 찾는다.
        for (int y = 0; y < img.getHeight(); y++) {
            if (RenderTestSupport.ink(img, 0, y, img.getWidth(), y + 1) > 0) {
                return y;
            }
        }
        return 1000;
    }

    /**
     * Returns the last row that has dark non-red ink (the text).
     * <p>
     * 어둡고 빨갛지 않은 잉크(텍스트)가 있는 마지막 행을 반환한다.
     *
     * @param img the image / 이미지
     * @return the row / 행
     */
    private static int lastInkRowOutsideRed(BufferedImage img) {
        int last = 0;
        // 모든 행에서 검은 잉크가 있는 가장 아래 행을 찾는다.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                int r = (c >> 16) & 255;
                int g = (c >> 8) & 255;
                if (r < 100 && g < 100) {
                    last = y;
                }
            }
        }
        return last;
    }

    /**
     * Counts the red pixels of a cell between two rows.
     * <p>
     * 칸의 두 행 사이에 있는 빨간 화소 수를 센다.
     *
     * @param img the image / 이미지
     * @param col the cell / 칸
     * @param cw  the cell width / 칸 너비
     * @param y0  the first row / 첫 행
     * @param y1  the last row, exclusive / 마지막 행(미포함)
     * @return the count / 개수
     */
    private static int redInCell(BufferedImage img, int col, int cw, int y0, int y1) {
        int n = 0;
        // 칸의 화소를 훑으며 빨간 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = col * cw; x < (col + 1) * cw && x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                if (((c >> 16) & 255) > 180 && ((c >> 8) & 255) < 90 && (c & 255) < 90) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the horizontal center of the red pixels of a cell, relative to its left edge.
     * <p>
     * 칸의 빨간 화소의 가로 중심을 칸 왼쪽 기준으로 반환한다.
     *
     * @param img the image / 이미지
     * @param col the cell / 칸
     * @param cw  the cell width / 칸 너비
     * @param y1  the last row, exclusive / 마지막 행(미포함)
     * @return the center / 중심
     */
    private static double redCenterX(BufferedImage img, int col, int cw, int y1) {
        double sum = 0;
        int n = 0;
        // 칸 안의 빨간 화소의 x 좌표를 평균한다.
        for (int y = 0; y < Math.min(img.getHeight(), y1); y++) {
            for (int x = col * cw; x < (col + 1) * cw; x++) {
                int c = img.getRGB(x, y);
                if (((c >> 16) & 255) > 180 && ((c >> 8) & 255) < 90 && (c & 255) < 90) {
                    sum += x - col * cw + 0.5;
                    n++;
                }
            }
        }
        return n == 0 ? -100 : sum / n;
    }
}
