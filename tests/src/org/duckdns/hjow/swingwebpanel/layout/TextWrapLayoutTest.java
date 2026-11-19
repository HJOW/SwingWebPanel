package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the line breaking properties of CSS Text Level 4 (plan item P10-08, https://www.w3.org/TR/css-text-4/): <code>text-wrap-mode</code> and
 * the <code>text-wrap</code> shorthand with <code>nowrap</code>, in combination with <code>white-space</code>.
 * <p>
 * CSS Text Level 4 의 줄바꿈 속성 시험(계획 P10-08, https://www.w3.org/TR/css-text-4/): <code>text-wrap-mode</code> 와 <code>nowrap</code> 을 쓴 <code>text-wrap</code>
 * 단축 속성, 그리고 <code>white-space</code> 와의 조합.
 */
public class TextWrapLayoutTest {

    /** Words that do not fit in one line of a 100px box in the 10px monospace font of the tests. 시험의 10px monospace 글꼴에서 100px 상자 한 줄에 들어가지 않는 단어들. */
    private static final String WORDS = "alpha beta gamma delta epsilon zeta eta theta iota kappa";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextWrapLayoutTest() {
    }

    /**
     * Returns the number of lines of a paragraph in a 100px box.
     * <p>
     * 100px 상자 안의 문단의 줄 수를 반환한다.
     *
     * @param style the style of the paragraph / 문단의 스타일
     * @return the lines / 줄 수
     */
    private static int lines(String style) {
        LayoutResult r = LayoutTestSupport.layout("<div style='width:100px'><p id=p style='margin:0;" + style + "'>" + WORDS + "</p></div>", 600);
        List<LayoutBox> l = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        return l.size();
    }

    /**
     * <code>text-wrap-mode: nowrap</code> keeps the text in one line; <code>wrap</code> (the initial value) breaks it; the mode is inherited.
     * <p>
     * <code>text-wrap-mode: nowrap</code> 은 텍스트를 한 줄에 둔다. 초기값 <code>wrap</code> 은 줄을 나눈다. 이 속성은 상속된다.
     */
    public void testNowrap() {
        Check.isTrue(lines("") > 1, "the text wraps by default");
        Check.equal(1, lines("text-wrap-mode:nowrap"), "text-wrap-mode: nowrap");
        Check.equal(1, lines("text-wrap:nowrap"), "the text-wrap shorthand");
        Check.isTrue(lines("text-wrap:wrap") > 1, "wrap");
        LayoutResult r = LayoutTestSupport.layout("<div style='width:100px;text-wrap-mode:nowrap'><p id=p style='margin:0'>" + WORDS + "</p></div>", 600);
        Check.equal(1, LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).size(), "inherited from the container");
    }

    /**
     * <code>line-break: anywhere</code> allows a break between any two characters, so that a long word fills the lines; <code>text-justify: none</code>
     * keeps justified text ragged on the right.
     * <p>
     * <code>line-break: anywhere</code> 는 모든 글자 사이에서 줄을 나눌 수 있게 하여 긴 단어가 줄을 채우게 한다. <code>text-justify: none</code> 은 양쪽 정렬한 텍스트를 오른쪽이
     * 가지런하지 않게 둔다.
     */
    public void testLineBreakAnywhereAndTextJustify() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult plain = LayoutTestSupport.layout("<div style='width:" + 10 * cw + "px'><p id=p style='margin:0'>abcdefghijklmnopqrstuvwxyz</p></div>", 600);
        Check.equal(1, LayoutTestSupport.lines(LayoutTestSupport.box(plain, "p")).size(), "a long word overflows by default");
        LayoutResult any = LayoutTestSupport.layout("<div style='width:" + 10 * cw + "px'><p id=p style='margin:0;line-break:anywhere'>abcdefghijklmnopqrstuvwxyz</p></div>", 600);
        Check.equal(3, LayoutTestSupport.lines(LayoutTestSupport.box(any, "p")).size(), "26 letters in lines of ten: three lines");
        String text = "aaaa bbbb cccc dddd eeee";
        String box = "<div style='width:" + 12 * cw + "px'><p id=p style='margin:0;text-align:justify%s'>" + text + "</p></div>";
        LayoutResult j = LayoutTestSupport.layout(String.format(box, ""), 600);
        List<LayoutBox> jl = LayoutTestSupport.lines(LayoutTestSupport.box(j, "p"));
        LayoutBox lastOfFirst = jl.get(0).getChildren().get(jl.get(0).getChildren().size() - 1);
        Check.near(LayoutTestSupport.box(j, "p").getX() + 12 * cw, lastOfFirst.getX() + lastOfFirst.getWidth(), 0.6, "justified: the first line reaches the right edge");
        LayoutResult n = LayoutTestSupport.layout(String.format(box, ";text-justify:none"), 600);
        List<LayoutBox> nl = LayoutTestSupport.lines(LayoutTestSupport.box(n, "p"));
        LayoutBox lastN = nl.get(0).getChildren().get(nl.get(0).getChildren().size() - 1);
        Check.isTrue(lastN.getX() + lastN.getWidth() < LayoutTestSupport.box(n, "p").getX() + 12 * cw - cw / 2, "text-justify: none leaves the line ragged");
    }

    /**
     * With <code>pre-wrap</code> the text-wrap mode nowrap makes <code>pre</code> (spaces are preserved, lines do not wrap), and a forced
     * line break still breaks the line.
     * <p>
     * <code>pre-wrap</code> 이면 text-wrap 모드 nowrap 은 <code>pre</code> 가 된다(공백은 보존하고 줄은 나뉘지 않는다). 강제 줄바꿈은 여전히 줄을 나눈다.
     */
    public void testWithWhiteSpace() {
        Check.isTrue(lines("white-space:pre-wrap") > 1, "pre-wrap wraps");
        Check.equal(1, lines("white-space:pre-wrap;text-wrap-mode:nowrap"), "pre-wrap with nowrap does not wrap");
        LayoutResult r = LayoutTestSupport.layout("<div style='width:100px'><p id=p style='margin:0;white-space:pre-wrap;text-wrap-mode:nowrap'>one\ntwo three four five six seven</p></div>", 600);
        Check.equal(2, LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).size(), "the newline is kept and nothing else breaks");
    }
}
