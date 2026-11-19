package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the underline options (plan item P10-08, CSS Text Decoration Level 4, https://www.w3.org/TR/css-text-decoration-4/): the underline skips the ink of
 * the glyphs where a descender crosses it (<code>text-decoration-skip-ink: auto</code>, the default) and is a full line with <code>none</code>;
 * <code>text-underline-offset</code> moves the underline away from the baseline and <code>text-underline-position: under</code> puts it below the descent
 * of the font. The decoration is red and the text is black, so the pixels of the line can be counted in the image.
 * <p>
 * 밑줄 옵션 시험(계획 P10-08, CSS Text Decoration Level 4): 밑줄은 내려긋는 획이 가로지르는 곳에서 글리프의 잉크를 건너뛰고(<code>text-decoration-skip-ink: auto</code>, 기본값)
 * <code>none</code> 이면 온전한 선이며, <code>text-underline-offset</code> 은 밑줄을 기준선에서 떼고 <code>text-underline-position: under</code> 는 글꼴의 descent 아래에 둔다.
 * 장식은 빨강, 텍스트는 검정이므로 선의 픽셀을 이미지에서 셀 수 있다.
 */
public class TextDecorationInkPaintTest {

    /** The color of the decoration lines. 장식선의 색. */
    private static final int RED = 0xFF0000;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextDecorationInkPaintTest() {
    }

    /**
     * Paints a line of text with a red underline and returns the image.
     * <p>
     * 빨간 밑줄이 있는 텍스트 한 줄을 그려 이미지를 반환한다.
     *
     * @param style the extra style / 추가 스타일
     * @param text  the text / 텍스트
     * @return the image of 300 x 100 / 300 x 100 이미지
     */
    private static BufferedImage paint(String style, String text) {
        RenderedPage p = RenderTestSupport.page("<p style='margin:0;font:40px monospace;color:#000;text-decoration:underline;text-decoration-color:#f00;" + style + "'>" + text + "</p>", 300,
                100, null);
        return RenderTestSupport.paint(p, 300, 100);
    }

    /**
     * Counts the red pixels of the image.
     * <p>
     * 이미지의 빨간 화소를 센다.
     *
     * @param img the image / 이미지
     * @return the count / 개수
     */
    private static int red(BufferedImage img) {
        int n = 0;
        // 이미지의 모든 화소에서 장식선의 색을 센다.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (RenderTestSupport.rgb(img, x, y) == RED) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the first row that has red pixels in the line (the top of the underline), or -1.
     * <p>
     * 선에 빨간 화소가 있는 첫 행(밑줄의 위쪽)을 반환한다. 없으면 -1.
     *
     * @param img the image / 이미지
     * @return the row / 행
     */
    private static int firstRedRow(BufferedImage img) {
        // 위에서부터 빨간 화소가 있는 행을 찾는다.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (RenderTestSupport.rgb(img, x, y) == RED) {
                    return y;
                }
            }
        }
        return -1;
    }

    /**
     * Descenders interrupt the underline unless <code>skip-ink</code> is <code>none</code>; text without descenders keeps the whole line.
     * <p>
     * <code>skip-ink</code> 가 <code>none</code> 이 아니면 내려긋는 획이 밑줄을 끊는다. 내려긋는 획이 없는 텍스트는 선이 온전하다.
     */
    public void testDescendersInterruptTheUnderline() {
        int full = red(paint("text-decoration-skip-ink:none", "gjpqy"));
        int skipped = red(paint("", "gjpqy"));
        int autoSkip = red(paint("text-decoration-skip-ink:auto", "gjpqy"));
        Check.isTrue(full > 0, "the underline is drawn: " + full);
        Check.isTrue(skipped < full * 85 / 100, "the descenders cut the underline: " + skipped + " of " + full);
        Check.equal(skipped, autoSkip, "auto is the default");
        int plainFull = red(paint("text-decoration-skip-ink:none", "AAAAA"));
        int plainSkip = red(paint("", "AAAAA"));
        Check.equal(plainFull, plainSkip, "a text without descenders keeps the whole line");
        Check.equal(full, red(paint("text-decoration-skip-ink:none", "gjpqy")), "none is stable");
    }

    /**
     * <code>text-underline-offset</code> moves the underline down from its default place.
     * <p>
     * <code>text-underline-offset</code> 은 밑줄을 기본 자리에서 아래로 옮긴다.
     */
    public void testUnderlineOffset() {
        int plain = firstRedRow(paint("", "AAAAA"));
        int offset = firstRedRow(paint("text-underline-offset:10px", "AAAAA"));
        Check.isTrue(plain >= 0 && offset > plain + 3, "the offset of 10px puts the underline lower: " + plain + " -> " + offset);
        int deeper = firstRedRow(paint("text-underline-offset:20px", "AAAAA"));
        Check.equal(offset + 10, deeper, "the position follows the offset");
    }

    /**
     * <code>text-underline-position: under</code> puts the underline below the descent of the font.
     * <p>
     * <code>text-underline-position: under</code> 는 밑줄을 글꼴의 descent 아래에 둔다.
     */
    public void testUnderlinePositionUnder() {
        int plain = firstRedRow(paint("", "AAAAA"));
        int under = firstRedRow(paint("text-underline-position:under", "AAAAA"));
        Check.isTrue(under > plain, "under is lower than the default: " + plain + " -> " + under);
    }
}
