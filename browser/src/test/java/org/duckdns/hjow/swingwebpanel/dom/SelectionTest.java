package org.duckdns.hjow.swingwebpanel.dom;

import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link Selection} (plan item P20-03, W3C Selection API): anchor and focus with the direction of the selection, collapse, extend,
 * setBaseAndExtent, selectAllChildren, addRange and removeRange, deleteFromDocument, containsNode, the type, and that the range of the
 * selection stays live when the tree changes.
 * <p>
 * {@link Selection} 시험(계획 P20-03, W3C Selection API): 선택의 방향과 함께 anchor, focus, collapse, extend, setBaseAndExtent, selectAllChildren,
 * addRange, removeRange, deleteFromDocument, containsNode, 종류, 트리가 바뀌어도 선택의 범위가 살아 있음.
 */
public class SelectionTest {

    /**
     * The document of the case.
     * <p>
     * 사례의 문서.
     */
    private Document doc;

    /**
     * The selection of the document.
     * <p>
     * 문서의 선택.
     */
    private Selection sel;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SelectionTest() {
    }

    /**
     * Creates a document with a small tree before each case.
     * <p>
     * 사례마다 작은 트리를 가진 문서를 만든다.
     */
    public void setUp() {
        doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        new HtmlParser(doc, "<!doctype html><div id=d><p id=p>Hello <b id=b>big</b> world</p><p id=q>Second</p></div>").run();
        sel = doc.getSelection();
    }

    /**
     * Returns an element by id.
     * <p>
     * id 로 요소를 반환한다.
     *
     * @param id the id / id
     * @return the element / 요소
     */
    private Element el(String id) {
        return doc.getElementById(id);
    }

    /**
     * A new selection is empty; the same object is returned every time.
     * <p>
     * 새 선택은 비어 있으며 같은 객체가 매번 반환된다.
     */
    public void testEmptySelection() {
        Check.same(sel, doc.getSelection(), "one selection per document");
        Check.equal(0, sel.getRangeCount(), "no range");
        Check.equal("None", sel.getType(), "type");
        Check.isTrue(sel.isCollapsed(), "collapsed");
        Check.isNull(sel.getAnchorNode(), "no anchor");
        Check.isNull(sel.getFocusNode(), "no focus");
        Check.equal(0, sel.getAnchorOffset() + sel.getFocusOffset(), "offsets are 0");
        Check.equal("", sel.toString(), "no text");
        Check.equal("none", sel.getDirection(), "direction");
        DomTestSupport.throwsDom("IndexSizeError", new Block() {
            /**
             * Asks for a range of an empty selection.
             * <p>
             * 빈 선택의 범위를 요청한다.
             */
            public void run() {
                sel.getRangeAt(0);
            }
        }, "getRangeAt of an empty selection");
        DomTestSupport.throwsDom("InvalidStateError", new Block() {
            /**
             * Collapses an empty selection to its start.
             * <p>
             * 빈 선택을 시작으로 접는다.
             */
            public void run() {
                sel.collapseToStart();
            }
        }, "collapseToStart of an empty selection");
        DomTestSupport.throwsDom("InvalidStateError", new Block() {
            /**
             * Extends an empty selection.
             * <p>
             * 빈 선택을 늘린다.
             */
            public void run() {
                sel.extend(el("p"), 0);
            }
        }, "extend of an empty selection");
    }

    /**
     * collapse makes a caret, collapseToStart and collapseToEnd collapse a range, null empties.
     * <p>
     * collapse 는 캐럿을 만들고, collapseToStart 와 collapseToEnd 는 범위를 접으며, null 은 선택을 비운다.
     */
    public void testCollapse() {
        sel.collapse(el("p").firstChild, 2);
        Check.equal("Caret", sel.getType(), "caret");
        Check.equal(1, sel.getRangeCount(), "one range");
        Check.isTrue(sel.isCollapsed(), "collapsed");
        Check.same(el("p").firstChild, sel.getAnchorNode(), "anchor");
        Check.equal(2, sel.getAnchorOffset(), "anchor offset");
        sel.setBaseAndExtent(el("p").firstChild, 1, el("q").firstChild, 3);
        sel.collapseToEnd();
        Check.same(el("q").firstChild, sel.getFocusNode(), "collapsed to the end");
        Check.equal(3, sel.getFocusOffset(), "end offset");
        sel.setBaseAndExtent(el("p").firstChild, 1, el("q").firstChild, 3);
        sel.collapseToStart();
        Check.equal(1, sel.getFocusOffset(), "collapsed to the start");
        sel.collapse(null, 0);
        Check.equal(0, sel.getRangeCount(), "null empties the selection");
        DomTestSupport.throwsDom("IndexSizeError", new Block() {
            /**
             * Collapses beyond the node.
             * <p>
             * 노드 밖으로 접는다.
             */
            public void run() {
                sel.collapse(el("b"), 5);
            }
        }, "offset beyond the node");
        DomTestSupport.throwsDom("InvalidNodeTypeError", new Block() {
            /**
             * Collapses into a document type.
             * <p>
             * 문서 유형으로 접는다.
             */
            public void run() {
                sel.collapse(doc.getFirstChild(), 0);
            }
        }, "a document type");
        sel.collapse(doc.createElement("detached"), 0);
        Check.equal(0, sel.getRangeCount(), "a point outside the document is ignored");
    }

    /**
     * setBaseAndExtent and extend: anchor and focus, the forward and backward direction and the range between them.
     * <p>
     * setBaseAndExtent 와 extend: anchor 와 focus, 정방향과 역방향, 둘 사이의 범위.
     */
    public void testAnchorFocusAndDirection() {
        sel.setBaseAndExtent(el("p").firstChild, 2, el("q").firstChild, 3);
        Check.equal("Range", sel.getType(), "range");
        Check.equal("forward", sel.getDirection(), "forward");
        Check.equal("llo big worldSec", sel.toString(), "selected text");
        Range r = sel.getRangeAt(0);
        Check.same(el("p").firstChild, r.getStartContainer(), "range start");
        sel.setBaseAndExtent(el("q").firstChild, 3, el("p").firstChild, 2);
        Check.equal("backward", sel.getDirection(), "backward");
        Check.same(el("q").firstChild, sel.getAnchorNode(), "anchor is at the later point");
        Check.same(el("p").firstChild, sel.getFocusNode(), "focus is at the earlier point");
        Check.equal(3, sel.getAnchorOffset(), "anchor offset");
        Check.equal(2, sel.getFocusOffset(), "focus offset");
        Check.same(el("p").firstChild, sel.getRangeAt(0).getStartContainer(), "the range start is the focus");
        sel.collapse(el("q").firstChild, 2);
        sel.extend(el("p").firstChild, 1);
        Check.equal("backward", sel.getDirection(), "extend before the anchor");
        Check.same(el("q").firstChild, sel.getAnchorNode(), "the anchor did not move");
        sel.extend(el("q").firstChild, 5);
        Check.equal("forward", sel.getDirection(), "extend after the anchor");
        Check.equal("con", sel.toString(), "the text between anchor and focus");
        sel.selectAllChildren(el("q"));
        Check.equal("Second", sel.toString(), "selectAllChildren");
        Check.same(el("q"), sel.getAnchorNode(), "anchor of selectAllChildren");
        Check.equal(1, sel.getFocusOffset(), "focus offset of selectAllChildren");
        DomTestSupport.throwsDom("InvalidNodeTypeError", new Block() {
            /**
             * Selects the children of a document type.
             * <p>
             * 문서 유형의 자식을 선택한다.
             */
            public void run() {
                sel.selectAllChildren(doc.getFirstChild());
            }
        }, "document type");
    }

    /**
     * addRange and removeRange: only one range, only in the document, and the range object is the live one.
     * <p>
     * addRange 와 removeRange: 범위는 하나뿐이고 문서 안에서만 가능하며 범위 객체는 살아 있는 것 그 자체이다.
     */
    public void testAddAndRemoveRange() {
        final Range r = doc.createRange();
        r.selectNodeContents(el("b"));
        sel.addRange(r);
        Check.same(r, sel.getRangeAt(0), "the range object itself");
        Range second = doc.createRange();
        second.selectNodeContents(el("q"));
        sel.addRange(second);
        Check.same(r, sel.getRangeAt(0), "a second range is ignored");
        Range outside = doc.createRange();
        outside.setStart(doc.createElement("x"), 0);
        sel.removeAllRanges();
        sel.addRange(outside);
        Check.equal(0, sel.getRangeCount(), "a range outside the document is ignored");
        sel.addRange(r);
        DomTestSupport.throwsDom("NotFoundError", new Block() {
            /**
             * Removes a range that is not the one of the selection.
             * <p>
             * 선택의 범위가 아닌 범위를 제거한다.
             */
            public void run() {
                sel.removeRange(doc.createRange());
            }
        }, "removeRange of another range");
        sel.removeRange(r);
        Check.equal(0, sel.getRangeCount(), "removeRange");
    }

    /**
     * The range of the selection is live: a mutation changes the selected text the way it changes the range, and deleteFromDocument removes
     * the selected content.
     * <p>
     * 선택의 범위는 살아 있다: 변경은 범위가 바뀌는 방식대로 선택한 텍스트를 바꾸며, deleteFromDocument 는 선택한 내용을 지운다.
     */
    public void testSelectionStaysLiveAndDeletes() {
        sel.setBaseAndExtent(el("b").firstChild, 1, el("q").firstChild, 3);
        Check.equal("ig worldSec", sel.toString(), "the selected text");
        el("p").insertBefore(doc.createTextNode(">>"), el("b"));
        Check.equal("ig worldSec", sel.toString(), "an insertion before the selection leaves the selected text as it is");
        ((CharacterData) el("q").firstChild).insertData(0, "##");
        Check.equal("ig world##Sec", sel.toString(), "data inserted before the end boundary is selected");
        sel.deleteFromDocument();
        Check.equal("Hello &gt;&gt;<b id=\"b\">b</b>", el("p").getInnerHtml(), "the selected content is gone from the first paragraph");
        Check.equal("ond", el("q").getInnerHtml(), "and from the second");
        Check.equal("Caret", sel.getType(), "the selection collapsed");
        Check.same(el("d"), sel.getFocusNode(), "between the paragraphs");
        Check.equal(1, sel.getFocusOffset(), "after the first paragraph");
    }

    /**
     * containsNode with and without partial containment.
     * <p>
     * 부분 포함을 허용하거나 허용하지 않는 containsNode.
     */
    public void testContainsNode() {
        sel.setBaseAndExtent(el("p").firstChild, 2, el("b").firstChild, 1);
        Check.isTrue(sel.containsNode(el("b"), true), "partially selected element");
        Check.isFalse(sel.containsNode(el("b"), false), "a partially selected element is not contained");
        Check.isFalse(sel.containsNode(el("q"), true), "an element after the selection");
        Check.isTrue(sel.containsNode(el("p"), true), "the element of the start");
        Check.isFalse(sel.containsNode(el("p"), false), "the paragraph is only partially selected");
        sel.selectAllChildren(el("p"));
        Check.isTrue(sel.containsNode(el("b"), false), "a child of a fully selected element");
        Check.isTrue(sel.containsNode(el("p").firstChild, false), "a text node that the selection covers exactly");
        sel.removeAllRanges();
        Check.isFalse(sel.containsNode(el("b"), true), "an empty selection contains nothing");
    }
}
