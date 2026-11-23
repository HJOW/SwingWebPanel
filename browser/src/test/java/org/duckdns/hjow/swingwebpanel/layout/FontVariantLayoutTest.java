package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the font properties that change the glyphs the engine draws (plan item P10-08, CSS Fonts Level 3/4, https://www.w3.org/TR/css-fonts-3/#small-caps
 * and https://www.w3.org/TR/css-fonts-4/#font-size-adjust-prop): <code>font-variant: small-caps</code> and <code>all-small-caps</code> draw the lower case
 * letters (all letters) as smaller capitals, and <code>font-size-adjust</code> scales the font so that its x-height reaches the given fraction of the size.
 * <p>
 * 엔진이 그리는 글리프를 바꾸는 글꼴 속성 시험(계획 P10-08, CSS Fonts Level 3/4, https://www.w3.org/TR/css-fonts-3/#small-caps 와
 * https://www.w3.org/TR/css-fonts-4/#font-size-adjust-prop): <code>font-variant: small-caps</code> 와 <code>all-small-caps</code> 는 소문자(모든 글자)를 더 작은 대문자로
 * 그리고, <code>font-size-adjust</code> 는 글꼴의 x 높이가 크기의 주어진 비율이 되도록 글꼴을 조정한다.
 */
public class FontVariantLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontVariantLayoutTest() {
    }

    /**
     * Returns the text fragments of the first line of the element <code>p</code>.
     * <p>
     * 요소 <code>p</code> 의 첫 줄의 텍스트 조각을 반환한다.
     *
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the text fragments / 텍스트 조각
     */
    private static List<LayoutBox> fragments(String style, String text) {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;" + style + "'>" + text + "</p>", 600);
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
     * Returns the width of the first line of a paragraph.
     * <p>
     * 문단의 첫 줄의 너비를 반환한다.
     *
     * @param style the style / 스타일
     * @param text  the text / 텍스트
     * @return the width / 너비
     */
    private static double width(String style, String text) {
        double w = 0;
        // 조각의 너비를 더한다.
        for (LayoutBox f : fragments(style, text)) {
            w += f.getWidth();
        }
        return w;
    }

    /**
     * With <code>small-caps</code> the lower case letters become capitals of 80 percent of the size; the other characters keep theirs.
     * <p>
     * <code>small-caps</code> 이면 소문자는 크기의 80 % 인 대문자가 되고 다른 글자는 그대로이다.
     */
    public void testSmallCaps() {
        double cw = LayoutTestSupport.charWidth();
        List<LayoutBox> f = fragments("font-variant:small-caps", "Abc d");
        StringBuilder text = new StringBuilder();
        // 조각의 텍스트를 이어 붙인다.
        for (LayoutBox b : f) {
            text.append(b.getText());
        }
        Check.equal("ABC D", text.toString(), "the lower case letters are drawn as capitals");
        Check.near(cw + 3 * cw * 0.8 + cw, width("font-variant:small-caps", "Abc d"), 0.7, "the capital A is full size, the small ones are 80 percent, the space is normal");
        Check.near(5 * cw, width("", "Abc d"), 0.01, "ordinary text has full size characters");
        Check.near(4 * cw * 0.8 + cw, width("font-variant:all-small-caps", "ABC D"), 0.7, "all-small-caps shrinks the capitals as well, not the space");
        Check.equal("Abc d", LayoutTestSupport.lineText(LayoutTestSupport.lines(LayoutTestSupport.box(
                LayoutTestSupport.layout("<p id=p style='margin:0'>Abc d</p>", 600), "p")).get(0)), "plain text is unchanged");
    }

    /**
     * <code>font-size-adjust</code> grows a font whose x-height is smaller than the given fraction and shrinks one whose x-height is larger.
     * <p>
     * <code>font-size-adjust</code> 는 x 높이가 주어진 비율보다 작은 글꼴을 키우고 큰 글꼴을 줄인다.
     */
    public void testFontSizeAdjust() {
        double plain = width("", "abcdefgh");
        Check.isTrue(width("font-size-adjust:0.9", "abcdefgh") > plain * 1.2, "a large aspect value grows the text");
        Check.isTrue(width("font-size-adjust:0.2", "abcdefgh") < plain * 0.8, "a small aspect value shrinks it");
        Check.near(plain, width("font-size-adjust:none", "abcdefgh"), 0.01, "none changes nothing");
        double a = width("font-size-adjust:0.9", "abcdefgh");
        double b = width("font-size-adjust:0.45", "abcdefgh");
        Check.near(2.0, a / b, 0.15, "the size is proportional to the aspect value");
    }
}
