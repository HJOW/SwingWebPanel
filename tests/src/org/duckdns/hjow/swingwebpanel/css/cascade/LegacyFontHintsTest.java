package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.PageRenderer;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks HTML5 2014 legacy font hints through parsing, cascade, mutation and rendering (P07-07-b-3).
 * <p>HTML5 2014의 과거 font 힌트를 파싱·캐스케이드·변경·렌더링으로 검증한다.
 */
public class LegacyFontHintsTest {
    /** Parsed document for the current case. / 현재 사례의 파싱한 문서. */
    private Document document;
    /** CSS engine owned by the current case. / 현재 사례가 소유한 CSS 엔진. */
    private CssEngine css;

    /** Creates the suite. / 시험 스위트를 만든다. */
    public LegacyFontHintsTest() {
    }

    /** Releases the document's CSS listeners after each case. / 각 사례 후 문서의 CSS 리스너를 해제한다. */
    public void tearDown() {
        if (css != null) {
            css.dispose();
            css = null;
        }
    }

    /**
     * Parses real HTML and loads UA and author styles; callers may mutate attributes afterward.
     * <p>실제 HTML을 파싱하고 UA·작성자 스타일을 읽으며 이후 속성 변경도 가능하다.
     * @param markup body markup / 본문 마크업
     * @param rules author CSS / 작성자 CSS
     */
    private void init(String markup, String rules) {
        tearDown();
        document = HtmlParser.parseDocument("<!doctype html><style>" + rules + "</style><body>" + markup,
                "http://legacy.test/", null, false);
        css = new CssEngine(document, null, new MediaEnvironment(200, 120));
        css.loadStyleSheets();
    }

    /** Returns a computed style by ID. / ID로 계산 스타일을 반환한다.
     * @param id element ID / 요소 ID
     * @return computed style / 계산 스타일 */
    private ComputedStyle style(String id) {
        return css.getComputedStyle(document.getElementById(id));
    }

    /**
     * One-digit components represent values 0..15, including colors that become empty after trimming or removing '#'.
     * <p>공백·# 제거 후 빈 값까지 포함하여 한 자리 성분은 0~15의 값이 된다.
     */
    public void testShortColorsAndEmptyInput() {
        String[][] cases = {{"f", "#0f0000"}, {"12", "#010200"}, {"123", "#010203"},
                {"#", "#000000"}, {"#ab", "#0a0b00"}, {" \t\r\n\f", "#000000"},
                {"#aBc", "#aaBBcc"}, {"chucknorris", "#c00000"}};
        // 규격의 짧은 성분과 일반 문자열을 고정 기대값으로 확인한다.
        for (int i = 0; i < cases.length; i++) {
            Check.equal(cases[i][1].toLowerCase(), HtmlPresentationalHints.legacyColor(cases[i][0]).toLowerCase(),
                    "legacy color " + cases[i][0]);
        }
        Check.isNull(HtmlPresentationalHints.legacyColor(null), "absent color");
        Check.isNull(HtmlPresentationalHints.legacyColor(""), "empty original attribute");
        Check.isNull(HtmlPresentationalHints.legacyColor(" TrAnSpArEnT "), "transparent is an error");
    }

    /**
     * Only HTML ASCII whitespace and CSS3 SVG color names get special treatment; later CSS and system colors do not.
     * <p>HTML ASCII 공백과 CSS3 SVG 색 이름만 특별 취급하며 후대 CSS 색과 시스템 색은 포함하지 않는다.
     */
    public void testColorWhitespaceAndKeywordEdition() {
        Check.equal("red", HtmlPresentationalHints.legacyColor("\tRED\r"), "ASCII trim and case");
        Check.equal("#00ed00", HtmlPresentationalHints.legacyColor("" + (char) 11 + "red"), "vertical tab stays input");
        Check.equal("#00ed00", HtmlPresentationalHints.legacyColor("" + (char) 160 + "red"), "NBSP stays input");
        Check.equal("#0eca00", HtmlPresentationalHints.legacyColor("rebeccapurple"), "CSS Color 4 name follows legacy digits");
        Check.equal("#000d00", HtmlPresentationalHints.legacyColor("Window"), "system colors are not keywords");
    }

    /**
     * Supplementary characters contribute two zeroes, and truncation precedes leading '#' removal and component splitting.
     * <p>보충 문자는 0 두 개로 바뀌며 # 제거·성분 분할 전에 128자에서 자른다.
     */
    public void testColorUnicodeAndLengthBoundary() {
        String supplementary = new String(new char[] {(char) 0xd83d, (char) 0xde00});
        Check.equal("#00000f", HtmlPresentationalHints.legacyColor(supplementary + "f"), "supplementary code point");
        StringBuilder prefix = new StringBuilder("#");
        // 마지막 유효 숫자가 128번째 또는 129번째에 오도록 접두사를 만든다.
        for (int i = 0; i < 126; i++) {
            prefix.append('0');
        }
        Check.equal("#0000f0", HtmlPresentationalHints.legacyColor(prefix + "f"), "128th character retained");
        Check.equal("#000000", HtmlPresentationalHints.legacyColor(prefix + "0f"), "129th character discarded");
    }

    /** Absolute sizes use the seven fixed keywords, with clamping and arbitrarily long leading zeroes.
     * <p>절대 크기는 고정 키워드 일곱 개와 범위 제한을 쓰며 긴 선행 0도 처리한다. */
    public void testAbsoluteFontSizes() {
        init("<font id=f size=1>x</font>", "body{font-size:40px}");
        String[] inputs = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "999999999999999999999999",
                "000000000000000000000002", "\t4junk", "6.9"};
        double[] expected = {10, 10, 13, 16, 18, 24, 32, 48, 48, 48, 13, 18, 32};
        Element font = document.getElementById("f");
        // 속성 변경마다 캐시가 무효화되며 상속 크기와 독립적인 절대 크기가 적용되어야 한다.
        for (int i = 0; i < inputs.length; i++) {
            font.setAttribute("size", inputs[i]);
            Check.near(expected[i], style("f").fontSizePx(), 0.001, "size " + inputs[i]);
        }
    }

    /** Relative sizes use base three, and the sign must be immediately followed by ASCII digits.
     * <p>상대 크기는 기준 3을 사용하며 부호 바로 뒤에 ASCII 숫자가 있어야 한다. */
    public void testRelativeSizesAndInvalidPrefixes() {
        init("<font id=f>x</font>", "body{font-size:40px}");
        String[] inputs = {"+0", "-0", "+1", "-1", "+00000000000000000002", "-999999999999999999999999",
                "+999999999999999999999999", " +2tail", "+ 2", "-\t1", "", " ", "+", "--1", "x2",
                "" + (char) 160 + "2", "" + (char) 11 + "2"};
        double[] expected = {16, 16, 18, 13, 24, 10, 48, 24, 40, 40, 40, 40, 40, 40, 40, 40, 40};
        Element font = document.getElementById("f");
        // 잘못된 접두사는 힌트를 만들지 않으므로 부모의 40px이 상속된다.
        for (int i = 0; i < inputs.length; i++) {
            font.setAttribute("size", inputs[i]);
            Check.near(expected[i], style("f").fontSizePx(), 0.001, "size " + inputs[i]);
        }
    }

    /**
     * CSS family lists, quoted delimiters and comments are parsed independently; a face value cannot introduce declarations.
     * <p>글꼴 목록·인용 구분자·주석은 독립적으로 파싱하며 face 값이 다른 선언을 추가할 수 없다.
     */
    public void testFaceValueIsolation() {
        init("<font id=f size=5 color=red>x</font>", "body{font-family:serif}");
        Element font = document.getElementById("f");
        font.setAttribute("face", "'A;{B}', monospace");
        Check.equal("A;{B}", style("f").fontFamilies().get(0), "quoted punctuation is a family name");
        Check.equal("monospace", style("f").fontFamilies().get(1), "fallback family");
        font.setAttribute("face", "Arial/*");
        Check.equal("Arial", style("f").fontFamilies().get(0), "comment ends with the attribute value");
        Check.near(24, style("f").fontSizePx(), 0.001, "face comment cannot swallow size");
        font.setAttribute("face", "Arial;display:none;color:blue");
        Check.equal("serif", style("f").fontFamilies().get(0), "invalid family is ignored");
        Check.equal("inline", style("f").display(), "face cannot add display");
        Check.equal("rgb(255, 0, 0)", style("f").getPropertyValue("color"), "independent color hint");
    }

    /** Hints cascade below author CSS, propagate to descendants, and disappear when attributes are removed.
     * <p>힌트는 작성자 CSS보다 낮고 자손에 상속되며 속성을 제거하면 사라진다. */
    public void testInheritanceMutationAndAuthorCascade() {
        init("<font id=f size=5 color=f face=monospace><span id=child>x</span></font>", "");
        Check.equal("rgb(15, 0, 0)", style("child").getPropertyValue("color"), "child inherits parsed legacy color");
        Check.near(24, style("child").fontSizePx(), 0.001, "child inherits font size");
        Element font = document.getElementById("f");
        font.setAttribute("color", "blue");
        Check.equal("rgb(0, 0, 255)", style("child").getPropertyValue("color"), "child cache invalidated");
        font.removeAttribute("size");
        font.removeAttribute("face");
        Check.near(16, style("child").fontSizePx(), 0.001, "size removed");
        Check.equal("serif", style("child").fontFamilies().get(0), "face removed");
        init("<font id=f size=7 color=red face=monospace>x</font>",
                "font{font-size:20px;color:blue;font-family:sans-serif}");
        Check.near(20, style("f").fontSizePx(), 0.001, "author size wins");
        Check.equal("rgb(0, 0, 255)", style("f").getPropertyValue("color"), "author color wins");
        Check.equal("sans-serif", style("f").fontFamilies().get(0), "author family wins");
    }

    /**
     * Legacy size reaches em-based geometry and a parsed legacy color reaches the display-list pixels without font rasterization assumptions.
     * <p>과거 크기를 em 기반 상자로, 해석한 과거 색을 글꼴 래스터화에 기대지 않는 실제 그림 화소로 검증한다.
     */
    public void testLayoutAndPixels() {
        init("<font id=f size=5 color=f></font>",
                "html,body{margin:0;padding:0}font{display:block;width:2em;height:1em;background:currentColor}");
        RenderedPage page = PageRenderer.render(document, css, null, 200, 120);
        LayoutBox box = page.getLayout().getBoxes(document.getElementById("f")).get(0);
        Check.near(48, box.getContentWidth(), 0.001, "two em width");
        Check.near(24, box.getContentHeight(), 0.001, "one em height");
        BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            page.paint(graphics, null, 0, 0);
        } finally {
            graphics.dispose();
        }
        Check.equal(0x0f0000, image.getRGB(20, 10) & 0xffffff, "legacy color inside the sized box");
        Check.isTrue((image.getRGB(50, 10) & 0xffffff) != 0x0f0000, "outside two em width");
    }
}
