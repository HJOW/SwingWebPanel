package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.Rectangle2D;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the rectangles of a range (plan item P20-03, CSSOM View <code>Range.getClientRects()</code>) and of the caret place of any DOM
 * position: the rectangles are the real glyph geometry of the selected characters, one per line, not the boxes of the elements. The page
 * uses a 10 pixel monospace font so that the positions are easy to predict.
 * <p>
 * 범위의 사각형(계획 P20-03, CSSOM View <code>Range.getClientRects()</code>)과 아무 DOM 위치의 캐럿 자리 시험: 사각형은 요소의 상자가 아니라 선택된 글자의 실제
 * 글리프 기하이며 줄마다 하나이다. 위치를 예측하기 쉽도록 10 픽셀 고정폭 글꼴의 페이지를 쓴다.
 */
public class RangeRectsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RangeRectsTest() {
    }

    /**
     * A range inside one text node gives one rectangle of exactly the selected characters, as high as the line.
     * <p>
     * 한 텍스트 노드 안의 범위는 선택된 글자만큼의 사각형 하나를 주며 줄의 높이이다.
     */
    public void testPartOfOneLine() {
        RenderedPage p = RenderTestSupport.page("<div id='t' style='padding:10px 0 0 10px'>abcdefghij</div>", 300, 100, null);
        Document doc = RenderTestSupport.lastDocument;
        Node text = doc.getElementById("t").getFirstChild();
        LayoutBox b = p.getLayout().getBoxes(text).get(0);
        double adv = b.getWidth() / 10.0;
        Range r = new Range(doc);
        r.setStart(text, 2);
        r.setEnd(text, 5);
        List<Rectangle2D> rects = new TextSelections(p).rects(r);
        Check.equal(1, rects.size(), "one rectangle");
        Rectangle2D one = rects.get(0);
        Check.near(b.getX() + 2 * adv, one.getX(), 0.6, "starts before the third character");
        Check.near(3 * adv, one.getWidth(), 0.8, "three characters wide");
        Check.near(b.getY(), one.getY(), 0.01, "top of the line");
        Check.near(b.getHeight(), one.getHeight(), 0.01, "height of the line");
    }

    /**
     * A range over a wrapped paragraph gives one rectangle per line: the first from the start, the last up to the end, the lines between
     * across the whole text of the line.
     * <p>
     * 줄바꿈된 문단에 걸친 범위는 줄마다 사각형 하나를 준다: 처음 줄은 시작부터, 마지막 줄은 끝까지, 사이의 줄은 줄의 모든 글자.
     */
    public void testOneRectanglePerLine() {
        RenderedPage p = RenderTestSupport.page("<div id='a' style='width:30px'>aaa bbb ccc ddd eee</div>", 300, 200, null);
        Document doc = RenderTestSupport.lastDocument;
        Node a = doc.getElementById("a").getFirstChild();
        List<LayoutBox> lines = p.getLayout().getBoxes(a);
        Check.isTrue(lines.size() >= 4, "the narrow block wraps into words: " + lines.size());
        Range r = new Range(doc);
        r.setStart(a, 1);
        r.setEnd(a, 17);
        List<Rectangle2D> rects = new TextSelections(p).rects(r);
        Check.equal(5, rects.size(), "one rectangle per line: " + rects);
        // 줄마다 하나씩이므로 y 가 모두 달라야 한다.
        for (int i = 1; i < rects.size(); i++) {
            Check.isTrue(rects.get(i).getY() > rects.get(i - 1).getY() + 1, "rectangle " + i + " is on a lower line");
        }
        Check.near(lines.get(0).getX() + 5, rects.get(0).getX(), 0.6, "the first line starts after one character");
        Check.isTrue(rects.get(1).getWidth() > 10, "a middle line holds a whole word: " + rects.get(1).getWidth());
    }

    /**
     * An element that the range covers completely gives its border box, and its text is not listed again.
     * <p>
     * 범위가 완전히 덮는 요소는 border box 를 주며 그 안의 텍스트는 다시 나열하지 않는다.
     */
    public void testCoveredElementGivesItsBox() {
        RenderedPage p = RenderTestSupport.page("<div id='x'>before</div><div id='y' style='border:2px solid #000;padding:3px'>inner text</div><div id='z'>after</div>", 300,
                200, null);
        Document doc = RenderTestSupport.lastDocument;
        Element x = doc.getElementById("x");
        Element y = doc.getElementById("y");
        Element z = doc.getElementById("z");
        Range r = new Range(doc);
        r.setStart(x.getFirstChild(), 3);
        r.setEnd(z.getFirstChild(), 2);
        List<Rectangle2D> rects = new TextSelections(p).rects(r);
        Check.equal(3, rects.size(), "the end of x, the box of y, the start of z");
        LayoutBox yb = p.getLayout().getBoxes(y).get(0);
        Rectangle2D mid = rects.get(1);
        Check.near(yb.getX(), mid.getX(), 0.01, "box of y: x");
        Check.near(yb.getY(), mid.getY(), 0.01, "box of y: y");
        Check.near(yb.getWidth(), mid.getWidth(), 0.01, "box of y: width");
        Check.near(yb.getHeight(), mid.getHeight(), 0.01, "box of y: height");
    }

    /**
     * A collapsed range in text gives one rectangle of zero width at the caret.
     * <p>
     * 텍스트 안의 접힌 범위는 캐럿 위치의 너비 0 사각형 하나를 준다.
     */
    public void testCollapsedRange() {
        RenderedPage p = RenderTestSupport.page("<div id='t' style='padding-left:10px'>abcdef</div>", 300, 100, null);
        Document doc = RenderTestSupport.lastDocument;
        Node text = doc.getElementById("t").getFirstChild();
        LayoutBox b = p.getLayout().getBoxes(text).get(0);
        Range r = new Range(doc);
        r.setStart(text, 3);
        r.collapse(true);
        List<Rectangle2D> rects = new TextSelections(p).rects(r);
        Check.equal(1, rects.size(), "one rectangle");
        Check.near(0, rects.get(0).getWidth(), 0.01, "zero width");
        Check.near(b.getX() + 3 * b.getWidth() / 6.0, rects.get(0).getX(), 0.6, "at the caret");
    }

    /**
     * A range that covers half of a typographic character unit (a surrogate pair, a base letter with a combining mark) gives the rectangle
     * of the whole unit (CSSOM View).
     * <p>
     * 글자 단위(서로게이트 쌍, 결합 문자가 붙은 기본 문자)의 절반만 덮는 범위는 그 단위 전체의 사각형을 준다(CSSOM View).
     */
    public void testPartialClusterIsWidened() {
        String pair = new String(Character.toChars(0x1F600));
        int[] w = TextSelections.wholeClusters("a" + pair + "b", 2, 3);
        Check.equal(1, w[0], "the start moved back to the start of the surrogate pair");
        Check.equal(3, w[1], "the end is after the pair");
        int[] c = TextSelections.wholeClusters("aéz", 1, 2);
        Check.equal(1, c[0], "a combining mark: the start is at the base letter");
        Check.equal(3, c[1], "and the end after the mark");
        int[] plain = TextSelections.wholeClusters("abcdef", 2, 4);
        Check.equal(2, plain[0], "plain text is not widened: start");
        Check.equal(4, plain[1], "plain text is not widened: end");
    }

    /**
     * The caret place of a position in an element: the end of the text before it, the start of the text after it, and the content box of
     * an empty block.
     * <p>
     * 요소 안의 위치의 캐럿 자리: 앞의 텍스트의 끝, 뒤의 텍스트의 시작, 빈 블록의 내용 상자.
     */
    public void testCaretPlaceInElements() {
        RenderedPage p = RenderTestSupport.page("<div id='a'>abc<b id='b'>def</b></div><div id='e' style='padding:5px 0 0 7px'></div>", 300, 200, null);
        Document doc = RenderTestSupport.lastDocument;
        Element a = doc.getElementById("a");
        Element e = doc.getElementById("e");
        TextSelections sel = new TextSelections(p);
        LayoutBox def = p.getLayout().getBoxes(doc.getElementById("b").getFirstChild()).get(0);
        Rectangle2D end = sel.caretRectAt(a, 2);
        Check.notNull(end, "after the bold element");
        Check.near(def.getX() + def.getWidth(), end.getX(), 0.6, "the end of the last text before the position");
        Rectangle2D start = sel.caretRectAt(a, 0);
        Check.notNull(start, "before the first text");
        Check.near(p.getLayout().getBoxes(a.getFirstChild()).get(0).getX(), start.getX(), 0.6, "the start of the first text");
        Rectangle2D empty = sel.caretRectAt(e, 0);
        Check.notNull(empty, "an empty block has a caret place");
        LayoutBox eb = p.getLayout().getBoxes(e).get(0);
        Check.near(eb.getContentX(), empty.getX(), 0.01, "at the left of the content box");
        Check.near(eb.getContentY(), empty.getY(), 0.01, "at the top of the content box");
        Check.isTrue(empty.getHeight() > 5, "as high as a line: " + empty.getHeight());
    }
}
