package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the soft hyphen and of balanced lines (plan item P10-08, CSS Text Level 3, https://www.w3.org/TR/css-text-3/#hyphenation, and
 * <code>text-wrap-style: balance</code> of CSS Text Level 4, https://www.w3.org/TR/css-text-4/#text-wrap-style): a soft hyphen draws nothing unless
 * the line breaks there, where a hyphen ends the line, <code>hyphens: none</code> ignores it, and a balanced paragraph has lines of about the same
 * length, kept aligned in the container.
 * <p>
 * 소프트 하이픈과 균형 맞춘 줄 시험(계획 P10-08, CSS Text Level 3, https://www.w3.org/TR/css-text-3/#hyphenation, 그리고 CSS Text Level 4 의
 * <code>text-wrap-style: balance</code>, https://www.w3.org/TR/css-text-4/#text-wrap-style): 소프트 하이픈은 줄이 거기서 나뉠 때만 하이픈으로 줄을 끝내고 그 밖에는 아무것도
 * 그리지 않으며, <code>hyphens: none</code> 은 그것을 무시하고, 균형 맞춘 문단은 길이가 비슷한 줄을 가지며 컨테이너 안에서 정렬을 유지한다.
 */
public class HyphensAndBalanceLayoutTest {

    /** The soft hyphen. 소프트 하이픈. */
    private static final String SHY = String.valueOf((char) 0x00AD);

    /** The zero width space that stands for a soft hyphen in the laid out text. 배치한 텍스트에서 소프트 하이픈을 대신하는 폭 없는 공백. */
    private static final String ZWSP = String.valueOf((char) 0x200B);

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HyphensAndBalanceLayoutTest() {
    }

    /**
     * Lays out a paragraph in a container of a width in characters.
     * <p>
     * 글자 수로 정한 너비의 컨테이너 안에 문단을 배치한다.
     *
     * @param chars the width in characters / 글자 수로 센 너비
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the layout; the paragraph has the id <code>p</code> / 레이아웃. 문단의 id 는 <code>p</code>
     */
    private static LayoutResult page(double chars, String style, String text) {
        double w = chars * LayoutTestSupport.charWidth();
        return LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0;" + style + "'>" + text + "</p></div>", 600);
    }

    /**
     * Returns the lines of a paragraph in a container of a width in characters.
     * <p>
     * 글자 수로 정한 너비의 컨테이너 안에 있는 문단의 줄을 반환한다.
     *
     * @param chars the width in characters / 글자 수로 센 너비
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the lines / 줄
     */
    private static List<LayoutBox> lines(double chars, String style, String text) {
        return LayoutTestSupport.lines(LayoutTestSupport.box(page(chars, style, text), "p"));
    }

    /**
     * A soft hyphen is a break opportunity that draws a hyphen at the end of the line it ends.
     * <p>
     * 소프트 하이픈은 끝이 된 줄의 끝에 하이픈을 그리는 줄바꿈 기회이다.
     */
    public void testBreakAtSoftHyphen() {
        List<LayoutBox> l = lines(6, "", "aaaa" + SHY + "bbbb");
        Check.equal(2, l.size(), "the word breaks at the soft hyphen");
        Check.equal("aaaa-", LayoutTestSupport.lineText(l.get(0)), "the first line ends with a hyphen");
        Check.equal("bbbb", LayoutTestSupport.lineText(l.get(1)), "the second line has the rest");
        Check.near(5 * LayoutTestSupport.charWidth(), l.get(0).getChildren().get(0).getWidth(), 0.01, "the hyphen takes the width of a character");
        List<LayoutBox> m = lines(10, "", "aaaa" + SHY + "bbbb" + SHY + "cccc");
        Check.equal(2, m.size(), "two breaks are possible, one is taken");
        Check.equal("aaaabbbb-", LayoutTestSupport.lineText(m.get(0)).replace(ZWSP, ""), "as many letters as fit, then the hyphen");
        Check.equal("cccc", LayoutTestSupport.lineText(m.get(1)), "and the rest");
        List<LayoutBox> k = lines(8, "", "aaaa" + SHY + "bbbb" + SHY + "cccc");
        Check.equal("aaaa-", LayoutTestSupport.lineText(k.get(0)).replace(ZWSP, ""), "if aaaabbbb- (nine) does not fit in eight, the earlier opportunity is taken");
    }

    /**
     * Without a break the soft hyphen is invisible and has no width; <code>hyphens: none</code> removes it and its break opportunity.
     * <p>
     * 줄이 나뉘지 않으면 소프트 하이픈은 보이지 않고 너비가 없다. <code>hyphens: none</code> 은 그것과 줄바꿈 기회를 없앤다.
     */
    public void testInvisibleWithoutBreak() {
        List<LayoutBox> l = lines(20, "", "aaaa" + SHY + "bbbb");
        Check.equal(1, l.size(), "it fits in one line");
        Check.equal("aaaabbbb", LayoutTestSupport.lineText(l.get(0)).replace(ZWSP, ""), "no hyphen is drawn");
        double total = 0;
        // 줄의 텍스트 조각 너비를 모두 더한다(소프트 하이픈이 조각을 나눈다).
        for (LayoutBox c : l.get(0).getChildren()) {
            total += c.getWidth();
        }
        Check.near(8 * LayoutTestSupport.charWidth(), total, 0.01, "and it has no width");
        List<LayoutBox> n = lines(6, "hyphens:none", "aaaa" + SHY + "bbbb");
        Check.equal(1, n.size(), "hyphens: none does not break there");
        Check.equal("aaaabbbb", LayoutTestSupport.lineText(n.get(0)).replace(ZWSP, ""), "and ignores the character");
        List<LayoutBox> p = lines(6, "white-space:nowrap", "aaaa" + SHY + "bbbb");
        Check.equal(1, p.size(), "nowrap does not break there");
        Check.equal("aaaabbbb", LayoutTestSupport.lineText(p.get(0)).replace(ZWSP, ""), "and draws no hyphen");
    }

    /**
     * A balanced paragraph has lines of about the same length instead of a long line and a short rest; the number of lines does not change.
     * <p>
     * 균형 맞춘 문단은 긴 줄과 짧은 나머지 대신 길이가 비슷한 줄을 가진다. 줄 수는 바뀌지 않는다.
     */
    public void testBalancedLines() {
        String text = "aaaa bbbb cccc dddd";
        List<LayoutBox> plain = lines(15, "", text);
        Check.equal("aaaa bbbb cccc", LayoutTestSupport.lineText(plain.get(0)), "without balancing the first line is filled");
        Check.equal("dddd", LayoutTestSupport.lineText(plain.get(1)), "and the rest is short");
        List<LayoutBox> b = lines(15, "text-wrap:balance", text);
        Check.equal(2, b.size(), "the number of lines is kept");
        Check.equal("aaaa bbbb", LayoutTestSupport.lineText(b.get(0)), "balanced first line");
        Check.equal("cccc dddd", LayoutTestSupport.lineText(b.get(1)), "balanced second line");
        Check.equal(1, lines(15, "text-wrap:balance", "aaaa bbbb").size(), "a one line paragraph is left alone");
        Check.equal(1, lines(15, "text-wrap:balance;text-wrap-mode:nowrap", text).size(), "nowrap does not wrap at all");
        Check.equal("aaaa bbbb cccc", LayoutTestSupport.lineText(lines(15, "text-wrap:stable", text).get(0)), "stable does not balance");
        Check.equal("aaaa bbbb cccc", LayoutTestSupport.lineText(lines(15, "text-wrap:pretty", text).get(0)), "nor does pretty");
    }

    /**
     * The balanced lines are aligned in the whole container: centered text is centered in it, text aligned to the right ends at its edge.
     * <p>
     * 균형 맞춘 줄은 컨테이너 전체 안에서 정렬된다: 가운데 정렬한 텍스트는 그 가운데에 있고, 오른쪽 정렬한 텍스트는 가장자리에서 끝난다.
     */
    public void testBalancedLinesStayAligned() {
        String text = "aaaa bbbb cccc dddd";
        double cwid = LayoutTestSupport.charWidth();
        LayoutResult c = page(15, "text-wrap:balance;text-align:center", text);
        double left = LayoutTestSupport.box(c, "p").getX();
        List<LayoutBox> cl = LayoutTestSupport.lines(LayoutTestSupport.box(c, "p"));
        Check.near(left + 3 * cwid, cl.get(0).getChildren().get(0).getX(), 0.6, "a line of nine characters is centered in fifteen");
        Check.near(left + 3 * cwid, cl.get(1).getChildren().get(0).getX(), 0.6, "also the second line");
        LayoutResult r = page(15, "text-wrap:balance;text-align:right", text);
        List<LayoutBox> kids = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0).getChildren();
        LayoutBox first = kids.get(kids.size() - 1);
        Check.near(LayoutTestSupport.box(r, "p").getX() + 15 * cwid, first.getX() + first.getWidth(), 0.6, "right aligned text ends at the right edge");
        LayoutResult s = page(15, "text-wrap:balance", text);
        Check.near(LayoutTestSupport.box(s, "p").getX(), LayoutTestSupport.lines(LayoutTestSupport.box(s, "p")).get(0).getChildren().get(0).getX(), 0.6,
                "left aligned text starts at the left edge");
    }
}
