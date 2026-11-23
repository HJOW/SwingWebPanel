package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>::first-line</code> (plan item P10-04-f, CSS Pseudo-Elements Level 4, CSS 2.1 5.12.1): the style applies to the text of
 * the first line only, the end of the first line follows from the first-line font (a larger font breaks earlier, a narrower box breaks
 * earlier), the text after the first line keeps the normal style, the background is painted as a fragment of the first line, text
 * decoration and <code>text-transform</code> of the pseudo-element, the style of a block whose content starts with a block-level
 * child, and the rule that a property set by an inline element inside the first line is kept.
 * <p>
 * <code>::first-line</code> 시험(계획 P10-04-f, CSS Pseudo-Elements Level 4, CSS 2.1 5.12.1): 스타일은 첫 줄의 텍스트에만 적용되고, 첫 줄의 끝은 첫 줄 글꼴에서
 * 정해지며(큰 글꼴이나 좁은 상자는 더 일찍 나눈다), 첫 줄 뒤의 텍스트는 보통 스타일을 유지하고, 배경은 첫 줄의 조각으로 그려지며, 의사 요소의 텍스트 장식과
 * <code>text-transform</code>, 내용이 블록 수준 자식으로 시작하는 블록의 스타일, 첫 줄 안의 인라인 요소가 정한 속성은 유지한다는 규칙을 확인한다.
 */
public class FirstLineLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FirstLineLayoutTest() {
    }

    /**
     * Returns the text boxes of a line box.
     * <p>
     * 줄 상자의 텍스트 상자를 반환한다.
     *
     * @param line the line / 줄
     * @return the text boxes / 텍스트 상자
     */
    private static List<LayoutBox> texts(LayoutBox line) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(line, LayoutBox.Type.TEXT, out);
        return out;
    }

    /**
     * Returns the largest font size of the text boxes of a line.
     * <p>
     * 줄의 텍스트 상자 중 가장 큰 글꼴 크기를 반환한다.
     *
     * @param line the line / 줄
     * @return the font size / 글꼴 크기
     */
    private static double fontSize(LayoutBox line) {
        double m = 0;
        // 줄의 텍스트 상자마다 글꼴 크기의 최댓값을 구한다.
        for (LayoutBox t : texts(line)) {
            m = Math.max(m, t.getFont().getSize2D());
        }
        return m;
    }

    /**
     * The first-line font applies to the first line only; the font size decides what fits on the line; the rest keeps the normal style.
     * <p>
     * 첫 줄 글꼴은 첫 줄에만 적용되고, 글꼴 크기가 줄에 무엇이 들어가는지 정하며, 나머지는 보통 스타일을 유지한다.
     */
    public void testFontAndColorOnlyOnTheFirstLine() {
        double cw = LayoutTestSupport.charWidth();
        // 10px 글자 22칸 너비: 20px 글꼴의 첫 줄에는 "aaaa bbbb"(9글자 = 18칸)만 들어가고 나머지는 10px 글꼴로 한 줄에 들어간다.
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-line{font-size:20px;color:#f00}</style>"
                + "<p id=a style='width:" + (22 * cw) + "px'>aaaa bbbb cccc dddd eeee ffff</p>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "a"));
        Check.equal(2, lines.size(), "two lines");
        Check.equal("aaaa bbbb", LayoutTestSupport.lineText(lines.get(0)), "the first line is what fits in the first-line font");
        Check.equal("cccc dddd eeee ffff", LayoutTestSupport.lineText(lines.get(1)), "the rest is laid out in the normal font");
        Check.near(20, fontSize(lines.get(0)), 0.01, "first-line font size");
        Check.near(10, fontSize(lines.get(1)), 0.01, "normal font size after the first line");
        Check.equal(0xFFFF0000, texts(lines.get(0)).get(0).getStyle().color(Prop.COLOR), "first-line color");
        Check.equal(0xFF000000, texts(lines.get(1)).get(0).getStyle().color(Prop.COLOR), "normal color after the first line");
        Check.isTrue(lines.get(0).getHeight() > lines.get(1).getHeight() + 4, "the first line is taller because of the larger font");
    }

    /**
     * The end of the first line follows the width of the box: a narrower box breaks the first line earlier, and the style follows.
     * <p>
     * 첫 줄의 끝은 상자의 너비를 따른다: 더 좁은 상자는 첫 줄을 더 일찍 나누고 스타일이 그것을 따른다.
     */
    public void testFirstLineFollowsTheWidth() {
        double cw = LayoutTestSupport.charWidth();
        String body = "<style>p::first-line{font-weight:bold;color:#00f}</style><p id=a style='width:%spx'>one two three four five six seven</p>";
        LayoutResult wide = LayoutTestSupport.layout(String.format(body, Double.toString(30 * cw)), 400);
        LayoutResult narrow = LayoutTestSupport.layout(String.format(body, Double.toString(12 * cw)), 400);
        List<LayoutBox> lw = LayoutTestSupport.lines(LayoutTestSupport.box(wide, "a"));
        List<LayoutBox> ln = LayoutTestSupport.lines(LayoutTestSupport.box(narrow, "a"));
        Check.isTrue(LayoutTestSupport.lineText(lw.get(0)).length() > LayoutTestSupport.lineText(ln.get(0)).length(), "wider box, longer first line");
        // 파란색 글자는 두 경우 모두 첫 줄의 글자뿐이다.
        for (int k = 0; k < 2; k++) {
            List<LayoutBox> lines = k == 0 ? lw : ln;
            // 모든 줄의 텍스트 상자를 확인한다.
            for (int i = 0; i < lines.size(); i++) {
                for (LayoutBox t : texts(lines.get(i))) {
                    Check.equal(i == 0 ? 0xFF0000FF : 0xFF000000, t.getStyle().color(Prop.COLOR), "color of line " + i + " of case " + k);
                    Check.equal(i == 0, t.getStyle().fontWeight() >= 700, "weight of line " + i + " of case " + k);
                }
            }
        }
    }

    /**
     * The background of the pseudo-element is a fragment that covers the text of the first line only.
     * <p>
     * 의사 요소의 배경은 첫 줄의 텍스트만 덮는 조각이다.
     */
    public void testBackgroundIsAFragmentOfTheFirstLine() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-line{background:#ff0}</style><p id=a style='width:" + (14 * cw)
                + "px'>aaaa bbbb cccc dddd</p>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "a"));
        Check.equal(2, lines.size(), "two lines");
        List<LayoutBox> frags0 = new ArrayList<LayoutBox>();
        List<LayoutBox> frags1 = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(lines.get(0), LayoutBox.Type.INLINE, frags0);
        LayoutTestSupport.collect(lines.get(1), LayoutBox.Type.INLINE, frags1);
        Check.equal(1, frags0.size(), "one first-line fragment on the first line");
        Check.equal("first-line", frags0.get(0).getPseudo(), "it is the pseudo-element");
        Check.equal(0, frags1.size(), "no fragment on the second line");
        // 조각의 너비는 줄의 텍스트(뒤 공백 제외)의 너비이다.
        double textW = 0;
        for (LayoutBox t : texts(lines.get(0))) {
            textW += t.getWidth();
        }
        Check.near(textW, frags0.get(0).getWidth(), 0.5, "the fragment is as wide as the text of the line");
        Check.near(lines.get(0).getX(), frags0.get(0).getX(), 0.5, "it starts at the start of the line");
    }

    /**
     * Text decoration and <code>text-transform</code> of the pseudo-element apply to the first line only, and the break position is that
     * of the transformed text.
     * <p>
     * 의사 요소의 텍스트 장식과 <code>text-transform</code> 은 첫 줄에만 적용되고 줄바꿈 위치는 변환된 텍스트의 것이다.
     */
    public void testDecorationAndTransform() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-line{text-decoration:underline;text-transform:uppercase}</style><p id=a style='width:"
                + (13 * cw) + "px'>aaaa bbbb cccc dddd</p>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "a"));
        Check.equal("AAAA BBBB", LayoutTestSupport.lineText(lines.get(0)), "uppercase first line");
        Check.equal("cccc dddd", LayoutTestSupport.lineText(lines.get(1)), "the rest is unchanged");
        Check.isTrue((texts(lines.get(0)).get(0).getTextDecoration() & LayoutBox.DECORATION_UNDERLINE) != 0, "underlined first line");
        Check.equal(0, texts(lines.get(1)).get(0).getTextDecoration() & LayoutBox.DECORATION_UNDERLINE, "no underline on the second line");
    }

    /**
     * An inline element inside the first line that sets a property itself keeps it; one that inherits takes the first-line value; the
     * source offsets of the text are those of the text node.
     * <p>
     * 첫 줄 안에서 스스로 속성을 정한 인라인 요소는 그것을 유지하고, 상속하는 요소는 첫 줄의 값을 받으며, 텍스트의 원본 위치는 텍스트 노드의 것이다.
     */
    public void testInlineElementsInsideTheFirstLine() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-line{color:#f00}</style><p id=a style='width:" + (30 * cw)
                + "px'>aaa <em id=e>bbb</em> <b style='color:#0f0'>ccc</b> ddd eee fff ggg hhh iii</p>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "a"));
        List<LayoutBox> t = texts(lines.get(0));
        // 첫 줄의 텍스트 상자는 em 의 "bbb"(상속한 색 → 빨강)와 b 의 "ccc"(자기 색 → 초록)를 포함한다.
        boolean red = false;
        boolean green = false;
        // 첫 줄의 텍스트 상자에서 "bbb" 와 "ccc" 의 색을 확인한다.
        for (LayoutBox x : t) {
            if ("bbb".equals(x.getText())) {
                red = x.getStyle().color(Prop.COLOR) == 0xFFFF0000;
            }
            if ("ccc".equals(x.getText())) {
                green = x.getStyle().color(Prop.COLOR) == 0xFF00FF00;
            }
        }
        Check.isTrue(red, "the em inherits the first-line color");
        Check.isTrue(green, "the b keeps the color it sets");
        Check.isTrue(lines.size() >= 2, "the paragraph has a second line");
        Check.isFalse(LayoutTestSupport.lineText(lines.get(0)).indexOf('i') >= 0 && lines.size() == 1, "the text wraps");
    }

    /**
     * A block whose content starts with a block-level child gets its first line in that child.
     * <p>
     * 내용이 블록 수준 자식으로 시작하는 블록은 그 자식에서 첫 줄을 얻는다.
     */
    public void testFirstLineOfTheFirstBlockChild() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<style>div::first-line{color:#f00}</style><div id=d style='width:" + (10 * cw)
                + "px'><p id=p1>aaaa bbbb cccc</p><p id=p2>dddd eeee ffff</p></div>", 400);
        List<LayoutBox> l1 = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p1"));
        List<LayoutBox> l2 = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p2"));
        Check.equal(0xFFFF0000, texts(l1.get(0)).get(0).getStyle().color(Prop.COLOR), "first line of the first child");
        Check.equal(0xFF000000, texts(l1.get(1)).get(0).getStyle().color(Prop.COLOR), "second line of the first child");
        Check.equal(0xFF000000, texts(l2.get(0)).get(0).getStyle().color(Prop.COLOR), "the second child is not the first line");
    }

    /**
     * A paragraph that fits on one line is styled completely; a forced break ends the first line; justified text keeps justification on
     * the first line.
     * <p>
     * 한 줄에 들어가는 문단은 모두 스타일이 적용되고, 강제 줄바꿈은 첫 줄을 끝내며, 양쪽 정렬한 텍스트는 첫 줄에서도 양쪽 정렬을 유지한다.
     */
    public void testSingleLineForcedBreakAndJustify() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult one = LayoutTestSupport.layout("<style>p::first-line{color:#f00}</style><p id=a>short</p>", 400);
        List<LayoutBox> l1 = LayoutTestSupport.lines(LayoutTestSupport.box(one, "a"));
        Check.equal(1, l1.size(), "one line");
        Check.equal(0xFFFF0000, texts(l1.get(0)).get(0).getStyle().color(Prop.COLOR), "the only line is the first line");
        LayoutResult br = LayoutTestSupport.layout("<style>p::first-line{color:#f00}</style><p id=a>one<br>two</p>", 400);
        List<LayoutBox> l2 = LayoutTestSupport.lines(LayoutTestSupport.box(br, "a"));
        Check.equal(0xFFFF0000, texts(l2.get(0)).get(0).getStyle().color(Prop.COLOR), "text before the break");
        Check.equal(0xFF000000, texts(l2.get(1)).get(0).getStyle().color(Prop.COLOR), "text after the break");
        double w = 20 * cw;
        LayoutResult j = LayoutTestSupport.layout("<style>p::first-line{color:#f00}</style><p id=a style='text-align:justify;width:" + w
                + "px'>aaa bbb ccc ddd eee fff ggg</p>", 400);
        List<LayoutBox> lj = LayoutTestSupport.lines(LayoutTestSupport.box(j, "a"));
        double right = 0;
        // 첫 줄의 마지막 텍스트 조각의 오른쪽 끝은 상자의 오른쪽 끝에 닿는다(양쪽 정렬).
        for (LayoutBox t : texts(lj.get(0))) {
            right = Math.max(right, t.getX() + t.getWidth());
        }
        Check.near(LayoutTestSupport.box(j, "a").getX() + w, right, 1.0, "the first line is justified");
    }
}
