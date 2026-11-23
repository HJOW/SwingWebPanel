package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>initial-letter</code> (plan item P10-08, CSS Inline Layout Module Level 3, https://www.w3.org/TR/css-inline-3/#initial-letter-styling): the first
 * letter of a paragraph is as large as <code>size</code> lines (its capitals are <code>(size - 1)</code> line heights plus the height of the capitals of the paragraph
 * high), the box of the letter is <code>sink</code> lines high, the first <code>sink</code> lines of the text flow beside it, the tops of the capitals of the letter and of
 * the first line are on one line, and the baseline of the letter is that of the <code>size</code>-th line.
 * <p>
 * <code>initial-letter</code> 시험(계획 P10-08, CSS Inline Layout Module Level 3): 문단의 첫 글자가 <code>size</code> 줄 크기이고(대문자의 높이가 <code>(size - 1)</code> 줄 높이에
 * 문단 대문자 높이를 더한 값), 글자 상자의 높이는 <code>sink</code> 줄이며, 텍스트의 첫 <code>sink</code> 줄이 그 옆으로 흐르고, 글자와 첫 줄의 대문자 위쪽이 한 줄에 놓이며, 글자의
 * 기준선이 <code>size</code> 번째 줄의 기준선이다.
 */
public class InitialLetterLayoutTest {

    /** The render context of the measurements. 측정에 쓰는 렌더 컨텍스트. */
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /** A text that makes many lines in the paragraph of the tests. 시험 문단에서 여러 줄이 되는 텍스트. */
    private static final String TEXT = "Drop cap text that goes on and on and on and on and on and on and on and on and on and on and on and on and on and on and on and on and on";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public InitialLetterLayoutTest() {
    }

    /**
     * Lays out a paragraph of 200 px with the type 20 px on a line of 30 px.
     * <p>
     * 줄 높이 30px, 글자 20px 의 200px 문단을 배치한다.
     *
     * @param letterStyle the style of <code>::first-letter</code> / <code>::first-letter</code> 의 스타일
     * @return the layout / 레이아웃
     */
    private static LayoutResult layout(String letterStyle) {
        return LayoutTestSupport.layout("<style>p{margin:0;width:200px;font:20px/30px monospace} p::first-letter{" + letterStyle + "}</style><p id=p>" + TEXT + "</p>", 600);
    }

    /**
     * Returns the first-letter float of the paragraph, or null.
     * <p>
     * 문단의 첫 글자 float 를 반환한다. 없으면 null.
     *
     * @param p the paragraph box / 문단 상자
     * @return the float or null / float. 없으면 null
     */
    private static LayoutBox cap(LayoutBox p) {
        // 문단의 자식 중 첫 글자 float 를 찾는다.
        for (int i = 0; i < p.getChildren().size(); i++) {
            if ("first-letter".equals(p.getChildren().get(i).getPseudo())) {
                return p.getChildren().get(i);
            }
        }
        return null;
    }

    /**
     * Returns the lines of the text of the paragraph itself (not the line inside the float of the letter).
     * <p>
     * 문단 자신의 텍스트의 줄을 반환한다(글자 float 안의 줄은 제외).
     *
     * @param p the paragraph box / 문단 상자
     * @return the lines / 줄
     */
    private static List<LayoutBox> bodyLines(LayoutBox p) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        // 문단의 직접 자식 중 줄 상자만 모은다.
        for (int i = 0; i < p.getChildren().size(); i++) {
            if (p.getChildren().get(i).getType() == LayoutBox.Type.LINE) {
                out.add(p.getChildren().get(i));
            }
        }
        return out;
    }

    /**
     * Measures the height of the capital letters of a font.
     * <p>
     * 글꼴의 대문자 높이를 잰다.
     *
     * @param f the font / 글꼴
     * @return the height / 높이
     */
    private static double capHeight(Font f) {
        return f.createGlyphVector(FRC, "H").getVisualBounds().getHeight();
    }

    /**
     * Returns the text fragments of a box.
     * <p>
     * 상자의 텍스트 조각을 반환한다.
     *
     * @param b the box / 상자
     * @return the fragments / 조각
     */
    private static List<LayoutBox> texts(LayoutBox b) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(b, LayoutBox.Type.TEXT, out);
        return out;
    }

    /**
     * <code>initial-letter: 3</code>: the letter is a float that is three lines high; the first three lines are indented by it and the fourth is not.
     * <p>
     * <code>initial-letter: 3</code>: 글자는 세 줄 높이의 float 이다. 첫 세 줄은 그만큼 들여 쓰이고 넷째 줄은 아니다.
     */
    public void testThreeLines() {
        LayoutResult r = layout("initial-letter:3");
        LayoutBox p = LayoutTestSupport.box(r, "p");
        LayoutBox cap = cap(p);
        Check.notNull(cap, "the first letter is a box of its own");
        Check.isTrue(cap.isFloat(), "it floats");
        Check.near(90, cap.getHeight(), 0.5, "and is three lines (3 x 30 px) high");
        List<LayoutBox> lines = bodyLines(p);
        Check.isTrue(lines.size() >= 5, "the text takes more than four lines");
        double right = cap.getX() + cap.getWidth();
        Check.isTrue(lines.get(0).getX() >= right - 0.5, "the first line starts right of the letter");
        Check.isTrue(lines.get(1).getX() >= right - 0.5, "the second line too");
        Check.isTrue(lines.get(2).getX() >= right - 0.5, "the third line too");
        Check.near(p.getX(), lines.get(3).getX(), 0.5, "the fourth line starts at the left edge of the paragraph");
        Check.equal("D", texts(cap).get(0).getText(), "the letter is the first character");
    }

    /**
     * The letter is as large as the lines say: its capitals are two line heights plus the capitals of the paragraph high; the tops of the capitals of the letter and of the
     * first line are on one line; the baseline of the letter is that of the third line.
     * <p>
     * 글자의 크기는 줄이 정한다: 대문자 높이가 두 줄 높이에 문단 대문자 높이를 더한 값이고, 글자와 첫 줄의 대문자 위쪽이 한 줄에 놓이며, 글자의 기준선이 셋째 줄의 기준선이다.
     */
    public void testSizeAndAlignment() {
        LayoutResult r = layout("initial-letter:3");
        LayoutBox p = LayoutTestSupport.box(r, "p");
        LayoutBox cap = cap(p);
        LayoutBox letter = texts(cap).get(0);
        List<LayoutBox> lines = bodyLines(p);
        LayoutBox first = texts(lines.get(0)).get(0);
        LayoutBox third = texts(lines.get(2)).get(0);
        double parentCap = capHeight(first.getFont());
        Check.near(2 * 30 + parentCap, capHeight(letter.getFont()), 1.5, "the capitals of the letter are two lines plus the capitals of the paragraph high");
        double letterTop = letter.getBaseline() - capHeight(letter.getFont());
        double firstTop = first.getBaseline() - parentCap;
        Check.near(firstTop, letterTop, 1.5, "the tops of the capitals are on one line");
        Check.near(third.getBaseline(), letter.getBaseline(), 1.5, "and the baseline of the letter is that of the third line");
        Check.isTrue(letter.getFont().getSize2D() > 40, "the letter is much larger than the text: " + letter.getFont().getSize2D());
    }

    /**
     * The sink: <code>initial-letter: 3 2</code> is three lines large but only two lines of text flow beside it; <code>raise</code> is one line; a size that is not whole
     * sinks to the whole number below it.
     * <p>
     * 가라앉는 줄: <code>initial-letter: 3 2</code> 는 세 줄 크기지만 옆으로 흐르는 텍스트는 두 줄뿐이다. <code>raise</code> 는 한 줄이고 정수가 아닌 크기는 그 아래 정수로 가라앉는다.
     */
    public void testSink() {
        LayoutResult two = layout("initial-letter:3 2");
        LayoutBox p = LayoutTestSupport.box(two, "p");
        LayoutBox cap = cap(p);
        Check.near(60, cap.getHeight(), 0.5, "a sink of two lines: the box is two lines high");
        List<LayoutBox> lines = bodyLines(p);
        Check.isTrue(lines.get(1).getX() >= cap.getX() + cap.getWidth() - 0.5, "two lines are beside the letter");
        Check.near(p.getX(), lines.get(2).getX(), 0.5, "the third line is not");
        LayoutBox raised = cap(LayoutTestSupport.box(layout("initial-letter:3 raise"), "p"));
        Check.near(30, raised.getHeight(), 0.5, "raise: the box is one line high");
        LayoutBox fractional = cap(LayoutTestSupport.box(layout("initial-letter:2.5"), "p"));
        Check.near(60, fractional.getHeight(), 0.5, "2.5 sinks two lines");
        Check.near(2 * 30, cap(LayoutTestSupport.box(layout("initial-letter:2 drop"), "p")).getHeight(), 0.5, "drop is the whole size");
    }

    /**
     * The cases that are not changed: <code>normal</code>, a size below one, a value that is not valid, and a letter that the author floated or positioned.
     * <p>
     * 바뀌지 않는 경우: <code>normal</code>, 1 보다 작은 크기, 올바르지 않은 값, 작성자가 float 나 위치 지정을 준 글자.
     */
    public void testNotChanged() {
        Check.isNull(cap(LayoutTestSupport.box(layout("initial-letter:normal"), "p")), "normal: no box of its own");
        Check.isNull(cap(LayoutTestSupport.box(layout("initial-letter:0.5"), "p")), "a size below one is ignored");
        Check.isNull(cap(LayoutTestSupport.box(layout("initial-letter:big"), "p")), "an invalid value is dropped");
        LayoutBox own = cap(LayoutTestSupport.box(layout("initial-letter:3;float:left;font-size:300%"), "p"));
        Check.notNull(own, "an author float is a box of its own");
        Check.near(60, texts(own).get(0).getFont().getSize2D(), 0.01, "and keeps the font size that the author gave (300% of 20 px)");
        Check.isTrue(own.getHeight() < 90 - 0.5 || own.getHeight() > 90 + 0.5, "and its height is not three lines");
    }
}
