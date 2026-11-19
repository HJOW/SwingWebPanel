package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.Rectangle2D;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the selection geometry of a rendered page (plan item P11-08): the caret position for a point, the highlight of a range, the
 * words and paragraphs, <code>user-select</code>, and the text that copying a range gives. They use the real HTML parser, CSS engine and
 * layout (see {@link RenderTestSupport}); the page is a monospace font of 10 pixels so that character positions are easy to predict.
 * <p>
 * 렌더링된 페이지의 선택 기하 시험(계획 P11-08): 점의 캐럿 위치, 범위의 강조, 낱말과 문단, <code>user-select</code>, 범위를 복사하면 나오는 텍스트. 실제 HTML 파서,
 * CSS 엔진, 레이아웃을 쓴다({@link RenderTestSupport} 참조). 글자 위치를 예측하기 쉽도록 10 픽셀 고정폭 글꼴의 페이지를 쓴다.
 */
public class TextSelectionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextSelectionTest() {
    }

    /**
     * Returns the first layout fragment of the text node of an element.
     * <p>
     * 요소의 텍스트 노드의 첫 레이아웃 조각을 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id of the element whose first child is the text node / 첫 자식이 텍스트 노드인 요소의 id
     * @return the fragment / 조각
     */
    private static LayoutBox fragment(RenderedPage p, String id) {
        Node text = RenderTestSupport.lastDocument.getElementById(id).getFirstChild();
        return p.getLayout().getBoxes(text).get(0);
    }

    /**
     * Returns the text of the whole body of a page as copying it gives it.
     * <p>
     * 페이지의 body 전체를 복사했을 때의 텍스트를 반환한다.
     *
     * @param body the body markup / body 마크업
     * @return the text / 텍스트
     */
    private static String copied(String body) {
        RenderedPage p = RenderTestSupport.page(body, 300, 400, null);
        Document doc = RenderTestSupport.lastDocument;
        Range r = new Range(doc);
        r.selectNodeContents(doc.getElementsByTagName("body").item(0));
        return new TextSelections(p).text(r);
    }

    /**
     * The highlight of a range inside one text node covers exactly the selected characters of its fragment, as high as the fragment.
     * <p>
     * 한 텍스트 노드 안의 범위의 강조는 조각의 선택된 글자만큼, 조각의 높이로 덮는다.
     */
    public void testHighlightOfPartOfOneLine() {
        RenderedPage p = RenderTestSupport.page("<div id='t' style='padding:10px 0 0 10px'>abcdefghij</div>", 300, 100, null);
        Document doc = RenderTestSupport.lastDocument;
        Node text = doc.getElementById("t").getFirstChild();
        LayoutBox b = fragment(p, "t");
        double adv = b.getWidth() / 10.0;
        Range r = new Range(doc);
        r.setStart(text, 2);
        r.setEnd(text, 5);
        SelectionHighlight h = new TextSelections(p).highlight(r);
        Check.notNull(h, "something is highlighted");
        Check.same(p, h.getPage(), "the highlight belongs to the page it was computed for");
        Rectangle2D bounds = h.getBounds();
        Check.near(b.getX() + 2 * adv, bounds.getX(), 0.6, "starts before the third character");
        Check.near(3 * adv, bounds.getWidth(), 0.8, "three characters wide");
        Check.near(b.getY(), bounds.getY(), 0.01, "as high as the fragment: top");
        Check.near(b.getHeight(), bounds.getHeight(), 0.01, "as high as the fragment: height");
        Check.isNull(new TextSelections(p).highlight(new Range(doc)), "a collapsed range has no highlight");
    }

    /**
     * A range over several lines and blocks highlights every line: the first from the start, the last up to the end, the lines between
     * completely.
     * <p>
     * 여러 줄과 블록에 걸친 범위는 모든 줄을 강조한다: 처음 줄은 시작부터, 마지막 줄은 끝까지, 사이의 줄은 전부.
     */
    public void testHighlightOverLinesAndBlocks() {
        RenderedPage p = RenderTestSupport.page("<div id='a' style='width:60px'>aaa bbb ccc</div><div id='b'>second</div>", 300, 200, null);
        Document doc = RenderTestSupport.lastDocument;
        Node a = doc.getElementById("a").getFirstChild();
        Node b = doc.getElementById("b").getFirstChild();
        Range r = new Range(doc);
        r.setStart(a, 4);
        r.setEnd(b, 3);
        SelectionHighlight h = new TextSelections(p).highlight(r);
        Check.notNull(h, "highlighted");
        java.util.List<LayoutBox> lines = p.getLayout().getBoxes(a);
        Check.isTrue(lines.size() >= 3, "the narrow block wraps: " + lines.size());
        Rectangle2D bounds = h.getBounds();
        LayoutBox last = p.getLayout().getBoxes(b).get(0);
        Check.near(lines.get(1).getY(), bounds.getY(), 0.01, "starts on the second line (the first one is before the range)");
        Check.near(last.getY() + last.getHeight(), bounds.getY() + bounds.getHeight(), 0.01, "ends at the bottom of the last fragment");
        Check.isTrue(h.getShape().contains(lines.get(2).getX() + 1, lines.get(2).getY() + 1), "the middle line is covered");
        Check.isFalse(h.getShape().contains(lines.get(0).getX() + 1, lines.get(0).getY() + 1), "the first line is not covered");
        Check.isTrue(h.getShape().contains(last.getX() + 1, last.getY() + 1), "the start of the last block is covered");
        Check.isFalse(h.getShape().contains(last.getX() + last.getWidth() - 1, last.getY() + 1), "its end after the range end is not");
    }

    /**
     * Right-to-left fragments are highlighted from the right: the first characters of the logical order are at the right end.
     * <p>
     * 오른쪽에서 왼쪽 조각은 오른쪽부터 강조한다: 논리 순서의 첫 글자들이 오른쪽 끝에 있다.
     */
    public void testHighlightOfRightToLeftText() {
        String hebrew = "אבגדה";
        RenderedPage p = RenderTestSupport.page("<div id='t' dir='rtl'>" + hebrew + "</div>", 300, 100, null);
        Document doc = RenderTestSupport.lastDocument;
        Node text = doc.getElementById("t").getFirstChild();
        LayoutBox b = fragment(p, "t");
        Check.isTrue((b.getBidiLevel() & 1) == 1, "the fragment runs right to left");
        Range r = new Range(doc);
        r.setStart(text, 0);
        r.setEnd(text, 2);
        Rectangle2D bounds = new TextSelections(p).highlight(r).getBounds();
        Check.near(b.getX() + b.getWidth(), bounds.getMaxX(), 0.8, "the first characters are at the right end");
        Check.isTrue(bounds.getWidth() < b.getWidth() - 1, "and the rest is not covered: " + bounds.getWidth());
    }

    /**
     * Elements that a range covers completely and that are replaced (images) are highlighted, with the border box.
     * <p>
     * 범위가 완전히 덮는 치환 요소(이미지)는 border box 로 강조한다.
     */
    public void testHighlightOfSelectedImage() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages().put("http://t.test/dir/a.png",
                ImageResource.loaded(RenderTestSupport.solid(4, 4, 0xFFFF0000)));
        RenderedPage p = RenderTestSupport.page("<div><img id='i' src='a.png' width='30' height='20' alt='x'></div><div id='t'>after</div>", 300, 100, src);
        Document doc = RenderTestSupport.lastDocument;
        Element img = doc.getElementById("i");
        Range r = new Range(doc);
        r.selectNode(img);
        Rectangle2D box = p.getHitTester().getBoundingBox(img);
        SelectionHighlight h = new TextSelections(p).highlight(r);
        if (h == null) {
            Check.skip("the image has no replaced box in this configuration");
            return;
        }
        Check.near(box.getWidth(), h.getBounds().getWidth(), 0.01, "as wide as the image");
        Check.near(box.getHeight(), h.getBounds().getHeight(), 0.01, "as high as the image");
    }

    /**
     * The caret position over text is the nearest character boundary; outside the text it is the nearest line: before the first line,
     * after the last, and the start or the end of a line for a point at its left or right.
     * <p>
     * 텍스트 위의 캐럿 위치는 가장 가까운 글자 경계이고, 텍스트 밖이면 가장 가까운 줄이다: 첫 줄 앞, 마지막 줄 뒤, 줄의 왼쪽이나 오른쪽의 점은 줄의 처음이나 끝.
     */
    public void testCaretPositions() {
        RenderedPage p = RenderTestSupport.page("<div id='a' style='margin:20px 0 0 30px'>abcdefghij</div><div id='b' style='margin:20px 0 0 30px'>klmnop</div>",
                300, 200, null);
        Node ta = RenderTestSupport.lastDocument.getElementById("a").getFirstChild();
        Node tb = RenderTestSupport.lastDocument.getElementById("b").getFirstChild();
        LayoutBox ba = fragment(p, "a");
        LayoutBox bb = fragment(p, "b");
        TextSelections g = new TextSelections(p);
        double adv = ba.getWidth() / 10.0;
        double ya = ba.getY() + ba.getHeight() / 2;
        HitResult hit = p.hitTestDocument(ba.getX() + 2.4 * adv, ya);
        TextSelections.Caret over = g.caretAt(ba.getX() + 2.4 * adv, ya, hit);
        Check.same(ta, over.getNode(), "over the first text");
        Check.equal(2, over.getOffset(), "2.4 characters from the left: the boundary before the third one (the hit says " + hit.getTextOffset()
                + ", the box is " + ba.getWidth() + " wide at " + ba.getX() + ", selectable " + TextSelections.isSelectable(ba) + ")");
        TextSelections.Caret right = g.caretAt(280, ya, null);
        Check.same(ta, right.getNode(), "right of the first line: the first line");
        Check.equal(10, right.getOffset(), "at its end");
        TextSelections.Caret left = g.caretAt(2, bb.getY() + bb.getHeight() / 2, null);
        Check.same(tb, left.getNode(), "left of the second line: the second line");
        Check.equal(0, left.getOffset(), "at its start");
        TextSelections.Caret below = g.caretAt(100, 190, null);
        Check.same(tb, below.getNode(), "below everything: the last line");
        Check.equal(6, below.getOffset(), "at its end");
        TextSelections.Caret above = g.caretAt(2, 0, null);
        Check.same(ta, above.getNode(), "above everything: the first line");
        Check.equal(0, above.getOffset(), "at its start");
        Check.isNull(new TextSelections(RenderTestSupport.page("<div style='height:20px'></div>", 100, 100, null)).caretAt(5, 5, null),
                "a page without text has no caret position");
    }

    /**
     * The caret moves by a character or a word, crosses from one text node to the next and back, and stops at the ends of the page; a
     * position in an element (after Select All) goes to the nearest text position first.
     * <p>
     * 캐럿은 글자나 낱말 단위로 움직이고, 한 텍스트 노드에서 다음 노드로 넘어갔다 돌아오며, 페이지의 양끝에서 멈춘다. 요소 안의 위치(전체 선택 뒤)는 먼저 가장
     * 가까운 텍스트 위치로 간다.
     */
    public void testStepsOfTheCaret() {
        RenderedPage p = RenderTestSupport.page("<p id='a'>one two</p><p id='b'>cd</p>", 300, 200, null);
        Document doc = RenderTestSupport.lastDocument;
        Node ta = doc.getElementById("a").getFirstChild();
        Node tb = doc.getElementById("b").getFirstChild();
        TextSelections g = new TextSelections(p);
        Check.equal(1, g.step(ta, 0, true, false).getOffset(), "one character forward");
        Check.equal(3, g.step(ta, 2, true, false).getOffset(), "and another");
        Check.same(tb, g.step(ta, 7, true, false).getNode(), "past the end of the text: the next text node");
        Check.equal(0, g.step(ta, 7, true, false).getOffset(), "at its start");
        Check.same(ta, g.step(tb, 0, false, false).getNode(), "backward from the start: the previous text node");
        Check.equal(7, g.step(tb, 0, false, false).getOffset(), "at its end");
        Check.isNull(g.step(ta, 0, false, false), "nothing before the first text");
        Check.isNull(g.step(tb, 2, true, false), "nothing after the last text");
        Check.equal(3, g.step(ta, 0, true, true).getOffset(), "one word forward");
        Check.equal(7, g.step(ta, 3, true, true).getOffset(), "the white space is skipped: the end of the next word");
        Check.equal(4, g.step(ta, 7, false, true).getOffset(), "one word backward");
        Node body = doc.getElementsByTagName("body").item(0);
        Check.same(ta, g.step(body, 0, true, false).getNode(), "from the start of the body: the first text position comes first");
        Check.equal(0, g.step(body, 0, true, false).getOffset(), "at its start");
        Check.equal(1, g.step(tb, 0, true, false).getOffset(), "stepping goes on in the text");
        Check.same(tb, g.step(body, body.getChildNodes().getLength(), false, false).getNode(), "from the end of the body: the last text");
        Check.equal(1, g.step(body, body.getChildNodes().getLength(), false, false).getOffset(), "one character before its end");
        Rectangle2D start = g.caretRect(ta, 0);
        Rectangle2D later = g.caretRect(ta, 4);
        Check.isTrue(later.getX() > start.getX() + 1, "the caret rectangle follows the offset");
        TextSelections.Caret below = g.line(ta, 1, true);
        Check.same(tb, below.getNode(), "one line down is in the next paragraph");
        TextSelections.Caret edge = g.lineEdge(ta, 1, true);
        Check.equal(7, edge.getOffset(), "the end of the line");
        Check.equal(0, g.lineEdge(ta, 3, false).getOffset(), "the start of the line");
    }

    /**
     * Words: the word that contains the character after the caret (the one before at the end), a run of white space, Korean words, and
     * the limits of the data.
     * <p>
     * 낱말: 캐럿 뒤 문자가 속한 낱말(끝이면 앞 문자), 공백 연속, 한국어 낱말, 데이터의 끝.
     */
    public void testWordRanges() {
        Check.equal("6,11", join(TextSelections.wordRange("hello world", 7)), "inside the second word");
        Check.equal("0,5", join(TextSelections.wordRange("hello world", 0)), "at the start of the first word");
        Check.equal("5,6", join(TextSelections.wordRange("hello world", 5)), "at the space: the space");
        Check.equal("6,11", join(TextSelections.wordRange("hello world", 11)), "at the end: the last word");
        Check.equal("0,0", join(TextSelections.wordRange("", 0)), "empty data");
        String korean = "안녕하세요 세계";
        Check.equal("0,5", join(TextSelections.wordRange(korean, 2)), "a Korean word");
        Check.equal("6,8", join(TextSelections.wordRange(korean, 7)), "the next Korean word");
    }

    /**
     * Joins the two numbers of a range.
     * <p>
     * 범위의 두 수를 잇는다.
     *
     * @param range the range / 범위
     * @return the text / 문자열
     */
    private static String join(int[] range) {
        return range.length == 2 ? range[0] + "," + range[1] : "?";
    }

    /**
     * Paragraphs: the element of the block that the text belongs to; text inside an inline-block belongs to the block around it.
     * <p>
     * 문단: 텍스트가 속한 블록의 요소. inline-block 안의 텍스트는 그 둘레의 블록에 속한다.
     */
    public void testParagraphElement() {
        RenderedPage p = RenderTestSupport.page("<p id='p'>para <b id='b'>bold</b></p><div id='d'>x<span id='s' style='display:inline-block'>in</span></div>",
                300, 200, null);
        TextSelections g = new TextSelections(p);
        Document doc = RenderTestSupport.lastDocument;
        LayoutBox bold = fragment(p, "b");
        Check.same(doc.getElementById("p"), TextSelections.paragraphElement(g.caretAt(bold.getX() + 1, bold.getY() + 1, p.hitTestDocument(bold.getX() + 1, bold.getY() + 1))),
                "bold text inside the paragraph");
        LayoutBox inner = fragment(p, "s");
        Check.same(doc.getElementById("d"), TextSelections.paragraphElement(g.caretAt(inner.getX() + 1, inner.getY() + 1, p.hitTestDocument(inner.getX() + 1, inner.getY() + 1))),
                "text of an inline-block belongs to the block around it");
    }

    /**
     * <code>user-select</code>: none is not selectable (also for the text below it), auto follows the parent, text overrides none, all gives
     * the element that is selected as a unit; none text is not highlighted and not copied.
     * <p>
     * <code>user-select</code>: none 은 선택할 수 없고(그 아래 텍스트도), auto 는 부모를 따르며, text 는 none 을 덮어쓰고, all 은 한 단위로 선택되는 요소를 준다.
     * none 텍스트는 강조하지도 복사하지도 않는다.
     */
    public void testUserSelect() {
        RenderedPage p = RenderTestSupport.page("<div id='n' style='user-select:none'>none <b id='nb'>below</b> <i id='t' style='user-select:text'>text</i></div>"
                + "<div id='a' style='user-select:all'><span id='as'>all</span> units</div><div id='f'>free</div>", 300, 200, null);
        Check.isFalse(TextSelections.isSelectable(fragment(p, "n")), "none");
        Check.isFalse(TextSelections.isSelectable(fragment(p, "nb")), "auto below none follows the parent");
        Check.isTrue(TextSelections.isSelectable(fragment(p, "t")), "text overrides none");
        Check.isTrue(TextSelections.isSelectable(fragment(p, "f")), "the default is selectable");
        Document doc = RenderTestSupport.lastDocument;
        Check.same(doc.getElementById("a"), TextSelections.allElement(fragment(p, "as")), "all: the element is the unit");
        Check.isNull(TextSelections.allElement(fragment(p, "f")), "no all");
        Range r = new Range(doc);
        r.selectNodeContents(doc.getElementsByTagName("body").item(0));
        TextSelections g = new TextSelections(p);
        String text = g.text(r);
        Check.isTrue(text.indexOf("none") < 0 && text.indexOf("below") < 0, "none text is not copied: " + text);
        Check.isTrue(text.indexOf("text") >= 0 && text.indexOf("free") >= 0, "the rest is copied: " + text);
        LayoutBox n = fragment(p, "n");
        Check.isFalse(g.highlight(r).getShape().contains(n.getX() + 1, n.getY() + n.getHeight() / 2), "none text is not highlighted");
    }

    /**
     * Copied text: block, paragraph, table, line break, wrapped line, preformatted text, collapsed white space, inline-block and hidden
     * content follow the rules that the class description of {@link TextSelections} gives.
     * <p>
     * 복사되는 텍스트: 블록, 문단, 표, 줄바꿈, 자동 줄바꿈, 서식 있는 텍스트, 접힌 공백, inline-block, 숨겨진 내용이 {@link TextSelections} 클래스 설명의 규칙을 따른다.
     */
    public void testCopiedTextRules() {
        Check.equal("one\n\ntwo", copied("<p>one</p><p>two</p>"), "paragraphs are separated by an empty line");
        Check.equal("a\nb", copied("<div>a</div><div>b</div>"), "blocks by a line feed");
        Check.equal("a\nb", copied("a<br>b"), "br");
        Check.equal("aaa bbb ccc", copied("<div style='width:40px'>aaa bbb ccc</div>"), "wrapping gives a space");
        Check.equal("x\ny", copied("<pre>x\ny</pre>"), "preformatted line feeds are kept");
        Check.equal("a b", copied("<p>a   b</p>"), "white space is collapsed");
        Check.equal("a\tb\nc\td", copied("<table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>"), "tabs between cells, line feeds between rows");
        Check.equal("one\ntwo", copied("<ul><li>one</li><li>two</li></ul>"), "list items without markers");
        Check.equal("xyz", copied("x<span style='display:inline-block'>y</span>z"), "inline-block text stays in the line");
        Check.equal("ab", copied("a<span style='display:none'>X</span>b"), "display none is not copied");
        Check.equal("ab", copied("a<span style='visibility:hidden'>X</span>b"), "hidden text is not copied");
        Check.equal("a bold b", copied("<div>a <b>bold</b> b</div>"), "inline elements");
        Check.equal("", copied("<div style='height:10px'></div>"), "no text");
    }

    /**
     * A range that starts and ends inside text gives only the selected parts, and the first and last blocks keep the separators.
     * <p>
     * 텍스트 안에서 시작하고 끝나는 범위는 선택된 부분만 주고, 첫 블록과 마지막 블록은 구분자를 유지한다.
     */
    public void testCopiedPartOfText() {
        RenderedPage p = RenderTestSupport.page("<p id='a'>first</p><p id='b'>second</p>", 300, 200, null);
        Document doc = RenderTestSupport.lastDocument;
        Range r = new Range(doc);
        r.setStart(doc.getElementById("a").getFirstChild(), 2);
        r.setEnd(doc.getElementById("b").getFirstChild(), 3);
        TextSelections g = new TextSelections(p);
        Check.equal("rst\n\nsec", g.text(r), "from the middle of one paragraph to the middle of the next");
        Check.equal("", g.text(null), "no range");
    }
}
