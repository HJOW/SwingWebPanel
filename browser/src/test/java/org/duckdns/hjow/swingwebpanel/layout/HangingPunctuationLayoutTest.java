package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>hanging-punctuation</code> (plan item P10-08, CSS Text Module Level 3, https://www.w3.org/TR/css-text-3/#hanging-punctuation-property, W3C Candidate
 * Recommendation Draft 14 August 2026): <code>first</code> hangs an opening bracket or quote outside the start of the first line, <code>force-end</code> and
 * <code>allow-end</code> let a stop or a comma hang outside the end of a line, and <code>last</code> a closing bracket or quote at the end of the last line; a hanging
 * character does not count when a word is fitted into the line, so the line may be one character longer than the box.
 * <p>
 * <code>hanging-punctuation</code> 시험(계획 P10-08, CSS Text Module Level 3): <code>first</code> 는 여는 괄호나 따옴표를 첫 줄의 시작 밖에 매달고, <code>force-end</code> 와
 * <code>allow-end</code> 는 마침표와 쉼표를 줄 끝 밖에 매달며, <code>last</code> 는 마지막 줄 끝의 닫는 괄호나 따옴표를 매단다. 매달린 글자는 낱말을 줄에 맞출 때 세지 않으므로 줄이 상자보다
 * 한 글자 길 수 있다.
 */
public class HangingPunctuationLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HangingPunctuationLayoutTest() {
    }

    /**
     * Lays out a paragraph that is <code>columns</code> characters wide in a monospaced font and returns its lines.
     * <p>
     * 고정폭 글꼴로 <code>columns</code> 글자 너비인 문단을 배치해 줄을 반환한다.
     *
     * @param columns the width in characters / 글자 수로 센 너비
     * @param style   more style / 추가 스타일
     * @param text    the text / 텍스트
     * @return the lines / 줄
     */
    private static List<LayoutBox> lines(int columns, String style, String text) {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;width:" + (columns * cw) + "px;" + style + "'>" + text + "</p>", 600);
        return LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
    }

    /**
     * Returns the text of a line.
     * <p>
     * 줄의 텍스트를 반환한다.
     *
     * @param line the line box / 줄 상자
     * @return the text without the trailing space / 뒤 공백을 뺀 텍스트
     */
    private static String text(LayoutBox line) {
        return LayoutTestSupport.lineText(line).trim();
    }

    /**
     * Returns the left edge of the first text fragment of a line, relative to the content box of the paragraph.
     * <p>
     * 줄의 첫 텍스트 조각의 왼쪽을 문단의 content box 기준으로 반환한다.
     *
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the offset / 위치
     */
    private static double firstX(String style, String text) {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;width:" + (10 * cw) + "px;" + style + "'>" + text + "</p>", 600);
        LayoutBox p = LayoutTestSupport.box(r, "p");
        List<LayoutBox> fragments = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(p, LayoutBox.Type.TEXT, fragments);
        return fragments.get(0).getX() - p.getX();
    }

    /**
     * <code>first</code>: the opening quote at the start of the first line hangs to the left of the box, so a word that is one character too long fits, and the text
     * of the line starts one character to the left of the content box; the later lines are not moved.
     * <p>
     * <code>first</code>: 첫 줄 맨 앞의 여는 따옴표가 상자 왼쪽 밖에 매달리므로 한 글자 모자란 낱말이 들어가고 줄의 텍스트가 content box 보다 한 글자 왼쪽에서 시작한다. 뒷줄은 옮겨지지 않는다.
     */
    public void testFirst() {
        double cw = LayoutTestSupport.charWidth();
        String text = "&quot;abcdefgh i";
        Check.equal(2, lines(10, "", text).size(), "without the property the text needs two lines");
        List<LayoutBox> hung = lines(10, "hanging-punctuation:first", text);
        Check.equal(1, hung.size(), "with first the opening quote hangs, so everything fits on one line");
        Check.near(-cw, firstX("hanging-punctuation:first", text), 0.01, "and the line starts one character left of the content box");
        Check.near(0, firstX("", text), 0.01, "without it the line starts at the content box");
        String two = "&quot;abcdefgh ijklmnopqrstu";
        List<LayoutBox> l = lines(10, "hanging-punctuation:first", two);
        Check.isTrue(l.size() >= 2, "the text goes on to the next line");
        Check.near(cw, l.get(1).getX() - l.get(0).getX(), 0.01, "the second line is not moved: it starts where the hung quote ended");
        Check.near(-cw, firstX("hanging-punctuation:first", "(abcdefgh i"), 0.01, "an opening bracket hangs too");
        Check.isTrue(firstX("hanging-punctuation:first", "「abcdefg i") <= -cw + 0.01, "so does a corner bracket (it is wide, so it hangs by its own advance)");
        Check.near(0, firstX("hanging-punctuation:first", "abcdefgh ij"), 0.01, "a letter does not hang");
        Check.near(0, firstX("hanging-punctuation:first", ")abcdefgh i"), 0.01, "a closing bracket does not hang at the start");
    }

    /**
     * <code>allow-end</code> and <code>force-end</code>: a stop or a comma at the end of the line hangs outside it, so the word that ends with it fits on a line that
     * is one character too short; without the property the word goes to the next line.
     * <p>
     * <code>allow-end</code> 와 <code>force-end</code>: 줄 끝의 마침표나 쉼표가 줄 밖에 매달리므로 그것으로 끝나는 낱말이 한 글자 모자란 줄에 들어간다. 속성이 없으면 낱말이 다음 줄로 간다.
     */
    public void testEnd() {
        String text = "ab cdefgh, ij";
        List<LayoutBox> plain = lines(9, "", text);
        Check.equal("ab", text(plain.get(0)), "without the property the word with the comma moves to the next line");
        List<LayoutBox> allow = lines(9, "hanging-punctuation:allow-end", text);
        Check.equal("ab cdefgh,", text(allow.get(0)), "allow-end: the comma hangs");
        Check.equal("ij", text(allow.get(1)), "and the rest follows");
        Check.equal("ab cdefgh,", text(lines(9, "hanging-punctuation:force-end", text).get(0)), "force-end does the same");
        Check.equal("ab cdefgh.", text(lines(9, "hanging-punctuation:force-end", "ab cdefgh. ij").get(0)), "a full stop hangs");
        Check.equal("ab cdefgh、", text(lines(9, "hanging-punctuation:force-end", "ab cdefgh、 ij").get(0)), "an ideographic comma hangs");
        Check.equal("ab", text(lines(9, "hanging-punctuation:force-end", "ab cdefgh; ij").get(0)), "a semicolon is not a stop or a comma");
        Check.equal("ab", text(lines(9, "hanging-punctuation:first", text).get(0)), "first alone does not hang the comma");
        Check.equal("ab", text(lines(9, "hanging-punctuation:force-end", "ab cdefgh,, ij").get(0)), "at most one character hangs: two commas leave a word that is too long");
    }

    /**
     * <code>last</code>: a closing bracket or quote at the end of the last line hangs; one in the middle of the text does not.
     * <p>
     * <code>last</code>: 마지막 줄 끝의 닫는 괄호나 따옴표는 매달리고, 글 중간의 것은 매달리지 않는다.
     */
    public void testLast() {
        Check.equal(2, lines(9, "", "ab cdefgh)").size(), "without the property the text needs two lines");
        Check.equal(1, lines(9, "hanging-punctuation:last", "ab cdefgh)").size(), "last: the closing bracket at the end of the text hangs");
        Check.equal(2, lines(9, "hanging-punctuation:last", "ab cdefgh) ij").size(), "a bracket that is not at the end of the text does not hang");
        Check.equal(1, lines(9, "hanging-punctuation:last", "ab cdefgh&quot;").size(), "a closing quote hangs");
        Check.equal(2, lines(9, "hanging-punctuation:first", "ab cdefgh)").size(), "first does not hang a closing bracket at the end");
        Check.equal(2, lines(9, "hanging-punctuation:last", "ab cdefgh,").size(), "last does not hang a comma");
    }

    /**
     * The property is inherited: the text of a child element hangs the same way, and a child can turn it off with <code>none</code>.
     * <p>
     * 속성은 상속된다: 자식 요소의 텍스트도 같은 방식으로 매달리며 자식이 <code>none</code> 으로 끌 수 있다.
     */
    public void testInheritance() {
        Check.equal(1, lines(9, "hanging-punctuation:last", "ab <span>cdefgh)</span>").size(), "a child inherits it");
        Check.equal(2, lines(9, "hanging-punctuation:last", "ab <span style='hanging-punctuation:none'>cdefgh)</span>").size(), "and can turn it off");
        Check.equal(2, lines(9, "hanging-punctuation:last", "ab <b style='hanging-punctuation:none'>cdefgh</b>)").size() - 1 + 1, "a bracket after the child");
    }
}
