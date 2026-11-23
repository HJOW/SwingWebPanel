package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the truncation of lines (plan item P10-08): the ellipsis of <code>text-overflow</code> (CSS Basic User Interface Level 3,
 * https://www.w3.org/TR/css-ui-3/#text-overflow) and <code>line-clamp</code>/<code>-webkit-line-clamp</code> (CSS Overflow Level 4,
 * https://www.w3.org/TR/css-overflow-4/#line-clamp): the text is cut so that the ellipsis ends inside the content box, a line that fits is kept, the
 * property only works for a box that clips its overflow, a string replaces the ellipsis, atomic boxes are removed whole, and clamped lines are removed
 * and the height shrinks.
 * <p>
 * 줄 자르기 시험(계획 P10-08): <code>text-overflow</code> 의 줄임표(CSS Basic User Interface Level 3, https://www.w3.org/TR/css-ui-3/#text-overflow)와
 * <code>line-clamp</code>/<code>-webkit-line-clamp</code>(CSS Overflow Level 4, https://www.w3.org/TR/css-overflow-4/#line-clamp): 줄임표가 content box 안에서
 * 끝나도록 텍스트를 자르고, 들어가는 줄은 그대로 두며, 넘침을 자르는 상자에서만 동작하고, 문자열이 줄임표를 대체하며, 원자적 상자는 통째로 지우고, clamp 된 줄은 지워져 높이가 준다.
 */
public class TextOverflowLayoutTest {

    /** A text of many letters. 글자가 많은 텍스트. */
    private static final String LONG = "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextOverflowLayoutTest() {
    }

    /**
     * Returns the text of the first line of an element joined from its text fragments.
     * <p>
     * 요소의 첫 줄의 텍스트를 텍스트 조각에서 이어 반환한다.
     *
     * @param r  the layout / 레이아웃
     * @param id the element id / 요소 id
     * @return the text of the first line / 첫 줄의 텍스트
     */
    private static String firstLine(LayoutResult r, String id) {
        return LayoutTestSupport.lineText(LayoutTestSupport.lines(LayoutTestSupport.box(r, id)).get(0));
    }

    /**
     * An overflowing line of a clipping box ends in an ellipsis that lies inside the content box.
     * <p>
     * 넘침을 자르는 상자의 넘치는 줄은 content box 안에 있는 줄임표로 끝난다.
     */
    public void testEllipsisOfOverflowingLine() {
        LayoutResult r = LayoutTestSupport.layout("<div id=d style='width:100px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis'>" + LONG + "</div>", 600);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "d"));
        Check.equal(1, lines.size(), "nowrap: one line");
        String t = firstLine(r, "d");
        Check.isTrue(t.endsWith("…"), "the line ends in an ellipsis: " + t);
        Check.isTrue(LONG.startsWith(t.substring(0, t.length() - 1)), "the rest is a prefix of the text: " + t);
        LayoutBox last = lines.get(0).getChildren().get(lines.get(0).getChildren().size() - 1);
        Check.isTrue(lines.get(0).getX() + last.getX() - lines.get(0).getX() + last.getWidth() <= LayoutTestSupport.box(r, "d").getX() + 100.01,
                "the ellipsis ends inside the content box");
        Check.isTrue(t.length() > 3 && t.length() * LayoutTestSupport.charWidth() <= 100.5, "as many letters as fit are kept, " + t.length());
    }

    /**
     * A line that fits is kept, and the property does nothing for a box that does not clip its overflow or that has no <code>text-overflow</code>.
     * <p>
     * 들어가는 줄은 그대로 두고, 넘침을 자르지 않는 상자나 <code>text-overflow</code> 가 없는 상자에서는 아무 일도 하지 않는다.
     */
    public void testNoEllipsisWhenItIsNotNeeded() {
        String fits = "<div id=d style='width:300px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis'>short text</div>";
        Check.equal("short text", firstLine(LayoutTestSupport.layout(fits, 600), "d"), "a line that fits");
        String visible = "<div id=d style='width:100px;white-space:nowrap;text-overflow:ellipsis'>" + LONG + "</div>";
        Check.equal(LONG, firstLine(LayoutTestSupport.layout(visible, 600), "d"), "overflow: visible does not truncate");
        String clip = "<div id=d style='width:100px;white-space:nowrap;overflow:hidden'>" + LONG + "</div>";
        Check.equal(LONG, firstLine(LayoutTestSupport.layout(clip, 600), "d"), "without text-overflow nothing is cut");
        String explicit = "<div id=d style='width:100px;white-space:nowrap;overflow:hidden;text-overflow:clip'>" + LONG + "</div>";
        Check.equal(LONG, firstLine(LayoutTestSupport.layout(explicit, 600), "d"), "clip");
    }

    /**
     * A string replaces the ellipsis, the second of two values is the end value, and an inline-block that does not fit is removed whole.
     * <p>
     * 문자열이 줄임표를 대체하고, 값이 둘이면 둘째가 끝 값이며, 들어가지 않는 inline-block 은 통째로 지운다.
     */
    public void testStringAndAtomicBoxes() {
        LayoutResult s = LayoutTestSupport.layout("<div id=d style='width:100px;white-space:nowrap;overflow:hidden;text-overflow:\">>\"'>" + LONG + "</div>", 600);
        Check.isTrue(firstLine(s, "d").endsWith(">>"), "a string: " + firstLine(s, "d"));
        LayoutResult t = LayoutTestSupport.layout("<div id=d style='width:100px;white-space:nowrap;overflow:hidden;text-overflow:clip \"~\"'>" + LONG + "</div>", 600);
        Check.isTrue(firstLine(t, "d").endsWith("~"), "the end value of two: " + firstLine(t, "d"));
        LayoutResult a = LayoutTestSupport.layout("<div id=d style='width:100px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis'>ab"
                + "<span id=i style='display:inline-block;width:150px;height:10px'></span>cd</div>", 600);
        String l = firstLine(a, "d");
        Check.isTrue(l.startsWith("ab") && l.endsWith("…") && l.indexOf("cd") < 0, "the box and what follows it are gone: " + l);
    }

    /**
     * <code>line-clamp</code> keeps the first N lines, adds an ellipsis to the last one and shrinks the block; it works with the prefixed name and
     * leaves shorter text alone.
     * <p>
     * <code>line-clamp</code> 는 처음 N 줄을 남기고 마지막 줄에 줄임표를 더하며 블록을 줄인다. 접두사 이름도 동작하고 짧은 텍스트는 그대로 둔다.
     */
    public void testLineClamp() {
        LayoutResult full = LayoutTestSupport.layout("<p id=p style='margin:0;width:100px'>" + LONG + "</p>", 600);
        int all = LayoutTestSupport.lines(LayoutTestSupport.box(full, "p")).size();
        Check.isTrue(all > 3, "the text needs more than three lines: " + all);
        double lineH = LayoutTestSupport.box(full, "p").getHeight() / all;
        LayoutResult c = LayoutTestSupport.layout("<p id=p style='margin:0;width:100px;overflow:hidden;line-clamp:2'>" + LONG + "</p>", 600);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(c, "p"));
        Check.equal(2, lines.size(), "two lines are kept");
        Check.near(2 * lineH, LayoutTestSupport.box(c, "p").getHeight(), 0.5, "and the block shrinks to them");
        Check.isTrue(LayoutTestSupport.lineText(lines.get(1)).endsWith("…"), "the last kept line ends in an ellipsis: " + LayoutTestSupport.lineText(lines.get(1)));
        Check.isFalse(LayoutTestSupport.lineText(lines.get(0)).endsWith("…"), "the first line is whole");
        LayoutResult w = LayoutTestSupport.layout("<p id=p style='margin:0;width:100px;-webkit-line-clamp:3;display:-webkit-box;-webkit-box-orient:vertical;overflow:hidden'>"
                + LONG + "</p>", 600);
        Check.equal(3, LayoutTestSupport.lines(LayoutTestSupport.box(w, "p")).size(), "-webkit-line-clamp with display: -webkit-box");
        LayoutResult n = LayoutTestSupport.layout("<p id=p style='margin:0;width:100px;line-clamp:none'>" + LONG + "</p>", 600);
        Check.equal(all, LayoutTestSupport.lines(LayoutTestSupport.box(n, "p")).size(), "none");
        LayoutResult s = LayoutTestSupport.layout("<p id=p style='margin:0;width:100px;line-clamp:9'>" + LONG + "</p>", 600);
        Check.equal(all, LayoutTestSupport.lines(LayoutTestSupport.box(s, "p")).size(), "a limit above the number of lines changes nothing");
        Check.isFalse(firstLine(s, "p").endsWith("…"), "and adds no ellipsis");
    }
}
