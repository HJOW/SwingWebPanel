package org.duckdns.hjow.swingwebpanel.dom;

import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the live behaviour of ranges that the DOM Standard describes for <code>normalize()</code> (the boundary points move into the
 * text node that receives the data), for <code>insertNode</code> that fails (the text at the start is not split before the check), and of
 * the removal of several nodes at once (<code>replaceChildren</code>) under a range and a node iterator (plan item P20-03).
 * <p>
 * DOM 표준이 <code>normalize()</code> 에 대해 설명하는 범위의 살아 있는 동작(경계점이 데이터를 받는 텍스트 노드 안으로 옮겨진다), 실패하는
 * <code>insertNode</code>(검사 전에 시작 지점의 텍스트를 쪼개지 않는다), 범위와 노드 반복자 아래에서 여러 노드를 한꺼번에 제거하는 일
 * (<code>replaceChildren</code>)의 시험(계획 P20-03).
 */
public class RangeNormalizeTest {

    /**
     * The document of the case.
     * <p>
     * 사례의 문서.
     */
    private Document doc;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RangeNormalizeTest() {
    }

    /**
     * Parses a page into a fresh document.
     * <p>
     * 페이지를 새 문서로 해석한다.
     *
     * @param html the page / 페이지
     */
    private void load(String html) {
        doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        new HtmlParser(doc, html).run();
    }

    /**
     * <code>normalize()</code> merges the text nodes and the boundary points of live ranges follow: a point in a merged node moves into the
     * first node after the characters that were there, a point of the parent right before a merged node moves to the end of the part that
     * was there, and a point after the merged nodes moves down by their number.
     * <p>
     * <code>normalize()</code> 가 텍스트 노드를 합치면 살아 있는 범위의 경계점이 따라간다: 합쳐진 노드 안의 점은 첫 노드 안의 이미 있던 글자 뒤로, 합쳐진 노드 바로 앞의 부모의 점은 그 부분의 끝으로, 합쳐진 노드들 뒤의 점은 그 수만큼 내려간다.
     */
    public void testNormalizeMovesBoundaryPoints() {
        load("<!doctype html><p id=p></p>");
        Element p = doc.getElementById("p");
        Text a = doc.createTextNode("ab");
        Text b = doc.createTextNode("cde");
        Text c = doc.createTextNode("");
        Text d = doc.createTextNode("fg");
        Element em = doc.createElement("em");
        Text e = doc.createTextNode("hi");
        p.appendChild(a);
        p.appendChild(b);
        p.appendChild(c);
        p.appendChild(d);
        p.appendChild(em);
        p.appendChild(e);
        Range inB = doc.createRange();
        inB.setStart(b, 1);
        inB.setEnd(d, 2);
        Range between = doc.createRange();
        between.setStart(p, 1);
        between.setEnd(p, 4);
        Range afterAll = doc.createRange();
        afterAll.setStart(p, 5);
        afterAll.setEnd(p, 6);
        p.normalize();
        Check.equal(3, p.getChildNodes().getLength(), "ab+cde+fg, the em element and hi");
        Check.equal("abcdefg", ((Text) p.getFirstChild()).getData(), "the data was merged into the first node");
        Check.same(a, inB.getStartContainer(), "the start moved into the first node");
        Check.equal(3, inB.getStartOffset(), "after the two characters that were there: 2 + 1");
        Check.same(a, inB.getEndContainer(), "the end moved into the first node");
        Check.equal(7, inB.getEndOffset(), "2 + 3 + 2");
        Check.same(a, between.getStartContainer(), "a point of the parent right before a merged node moved into the first node");
        Check.equal(2, between.getStartOffset(), "to the end of the part that was there");
        Check.same(p, between.getEndContainer(), "a point after the merged nodes stays in the parent");
        Check.equal(1, between.getEndOffset(), "and counts the removed nodes (4 - 3)");
        Check.same(p, afterAll.getStartContainer(), "the point before the last text");
        Check.equal(2, afterAll.getStartOffset(), "now at index 2 (5 - 3)");
        Check.equal(3, afterAll.getEndOffset(), "the end after the last text (6 - 3)");
    }

    /**
     * The empty text nodes are removed by <code>normalize()</code> and a point inside one of them moves to the place of the removed node.
     * <p>
     * <code>normalize()</code> 는 빈 텍스트 노드를 제거하고 그 안의 점은 제거된 노드의 자리로 옮겨진다.
     */
    public void testNormalizeRemovesEmptyText() {
        load("<!doctype html><p id=p></p>");
        Element p = doc.getElementById("p");
        Text empty = doc.createTextNode("");
        Text word = doc.createTextNode("word");
        p.appendChild(empty);
        p.appendChild(word);
        Range r = doc.createRange();
        r.setStart(empty, 0);
        r.setEnd(word, 2);
        p.normalize();
        Check.equal(1, p.getChildNodes().getLength(), "only the word is left");
        Check.same(p, r.getStartContainer(), "the start moved to the parent");
        Check.equal(0, r.getStartOffset(), "at the place of the removed node");
        Check.same(word, r.getEndContainer(), "the end is untouched");
        Check.equal(2, r.getEndOffset(), "at the same offset");
    }

    /**
     * A failing <code>insertNode</code> leaves the text at the start of the range as it was: the validity of the insertion is checked before
     * the text is split (DOM Standard, "insertNode", steps 6 and 7).
     * <p>
     * 실패하는 <code>insertNode</code> 는 범위 시작의 텍스트를 그대로 둔다: 삽입이 가능한지 텍스트를 쪼개기 전에 확인한다(DOM 표준 "insertNode" 6, 7 단계).
     */
    public void testFailingInsertNodeDoesNotSplitText() {
        load("<!doctype html><p id=p>abcdef</p>");
        final Element p = doc.getElementById("p");
        final Range r = doc.createRange();
        r.setStart(p.getFirstChild(), 3);
        r.setEnd(p.getFirstChild(), 5);
        Check.throwsException(DomException.class, new Block() {
            /**
             * Tries to insert the parent of the start into itself.
             * <p>
             * 시작 지점의 부모를 자기 안에 삽입해 본다.
             */
            public void run() {
                r.insertNode(p);
            }
        }, "inserting the parent into itself is a hierarchy error");
        Check.equal(1, p.getChildNodes().getLength(), "the text was not split");
        Check.equal("abcdef", ((Text) p.getFirstChild()).getData(), "and has all its data");
        Check.equal(3, r.getStartOffset(), "the range is as before");
        Check.equal(5, r.getEndOffset(), "end as before");
    }

    /**
     * Removing several nodes at once with <code>replaceChildren</code> moves the boundary points inside them to the place of the removed
     * nodes, the points after them down by the number of the removed nodes, and the new nodes count after the point at the place.
     * <p>
     * <code>replaceChildren</code> 로 노드 여러 개를 한꺼번에 제거하면 그 안의 경계점은 제거된 자리로, 그 뒤의 점은 제거된 수만큼 내려가고, 새 노드는 그 자리의 점 뒤에 온다.
     */
    public void testReplaceChildrenUnderRanges() {
        load("<!doctype html><div id=d><b id=b1>one</b><i id=i1>two</i><u id=u1>three</u><s id=s1>four</s></div>");
        Element d = doc.getElementById("d");
        Range inside = doc.createRange();
        inside.setStart(doc.getElementById("i1").getFirstChild(), 1);
        inside.setEnd(doc.getElementById("u1").getFirstChild(), 2);
        Range after = doc.createRange();
        after.setStart(d, 4);
        after.setEnd(d, 4);
        Range before = doc.createRange();
        before.setStart(d, 0);
        before.setEnd(d, 1);
        d.replaceChildren(doc.createElement("hr"));
        Check.same(d, inside.getStartContainer(), "the start moved to the parent");
        Check.equal(0, inside.getStartOffset(), "at the place of the removed nodes");
        Check.same(d, inside.getEndContainer(), "the end moved to the parent");
        Check.equal(0, inside.getEndOffset(), "as well");
        Check.equal(0, after.getStartOffset(), "a point after them moved down to the place of the removal, and the insertion at the end does not move it");
        Check.equal(0, before.getStartOffset(), "a point at the start stays");
        Check.equal(0, before.getEndOffset(), "an end that was inside the removed nodes comes to the place of the removal");
    }

    /**
     * A node iterator whose reference node is in one of the nodes that <code>replaceChildren</code> removes moves to a node after them, or
     * to the end of the node before them when no node is left after.
     * <p>
     * 기준 노드가 <code>replaceChildren</code> 이 제거하는 노드 안에 있는 노드 반복자는 그 뒤의 노드로, 뒤에 남는 노드가 없으면 앞 노드의 끝으로 옮겨 간다.
     */
    public void testIteratorUnderReplaceChildren() {
        load("<!doctype html><div id=d><b id=b1>one</b><i id=i1>two</i></div><p id=after>tail</p>");
        Element d = doc.getElementById("d");
        NodeIterator it = doc.createNodeIterator(doc.getDocumentElement(), NodeFilter.SHOW_ALL, null);
        Node n;
        // 기준 노드가 i 안의 텍스트가 될 때까지 앞으로 간다.
        do {
            n = it.nextNode();
        } while (n != null && !(n instanceof Text && "two".equals(((Text) n).getData())));
        Check.notNull(n, "the iterator reached the text of the i element");
        d.replaceChildren();
        Check.same(d, it.getReferenceNode(), "the reference moved to the node before the removed nodes (the parent)");
        Check.isFalse(it.isPointerBeforeReferenceNode(), "and the pointer is after it");
        Node next = it.nextNode();
        Check.notNull(next, "iteration goes on");
        Check.same(doc.getElementById("after"), next, "with the node after the emptied div");
    }
}
