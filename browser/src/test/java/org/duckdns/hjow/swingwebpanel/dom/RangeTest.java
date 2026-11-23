package org.duckdns.hjow.swingwebpanel.dom;

import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link Range} (plan item P20-03, DOM Standard "Interface Range"): boundary points and their errors, the comparison operations,
 * the content operations with the examples of the Standard (clone, extract, delete, insert, surround, <code>toString</code>), and the live
 * behaviour (boundary points follow insertions, removals, replaced character data and split text nodes).
 * <p>
 * {@link Range} 시험(계획 P20-03, DOM 표준 "Interface Range"): 경계점과 그 오류, 비교 연산, 표준의 예를 쓴 내용 연산(clone, extract, delete, insert,
 * surround, <code>toString</code>), 살아 있는 동작(경계점이 삽입, 제거, 교체된 문자 데이터, 쪼개진 텍스트 노드를 따라간다).
 */
public class RangeTest {

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
    public RangeTest() {
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
     * Returns the serialization of the children of a node.
     * <p>
     * 노드의 자식의 직렬화를 반환한다.
     *
     * @param n the node / 노드
     * @return the HTML / HTML
     */
    private String inner(Node n) {
        StringBuilder sb = new StringBuilder();
        // 자식을 차례로 직렬화해 이어 붙인다.
        for (Node c = n.firstChild; c != null; c = c.nextSibling) {
            if (c instanceof Element) {
                sb.append(((Element) c).getOuterHtml());
            } else if (c instanceof CharacterData) {
                sb.append(((CharacterData) c).getData());
            }
        }
        return sb.toString();
    }

    /**
     * Describes the boundary points as <code>start:offset-end:offset</code> with the node names.
     * <p>
     * 경계점을 노드 이름과 함께 <code>start:offset-end:offset</code> 으로 설명한다.
     *
     * @param r the range / 범위
     * @return the description / 설명
     */
    private String show(Range r) {
        return name(r.getStartContainer()) + ":" + r.getStartOffset() + "-" + name(r.getEndContainer()) + ":" + r.getEndOffset();
    }

    /**
     * Names a node for {@link #show}: the id of an element, the quoted start of a text.
     * <p>
     * {@link #show} 를 위해 노드 이름을 짓는다: 요소는 id, 텍스트는 따옴표로 묶은 앞부분.
     *
     * @param n the node / 노드
     * @return the name / 이름
     */
    private String name(Node n) {
        if (n instanceof Element) {
            Element e = (Element) n;
            return e.getId().length() > 0 ? e.getId() : e.getLocalName();
        }
        if (n instanceof CharacterData) {
            String d = ((CharacterData) n).getData();
            return "'" + (d.length() > 6 ? d.substring(0, 6) : d) + "'";
        }
        return n.getNodeName();
    }

    /**
     * Boundary points: setters, "before/after" helpers, collapse, selectNode(Contents), the movement of the other end, and the errors.
     * <p>
     * 경계점: 지정, "before/after" 도우미, collapse, selectNode(Contents), 반대쪽 끝의 이동, 오류.
     */
    public void testBoundaryPoints() {
        load("<!doctype html><div id=d><p id=p>one<b id=b>two</b>three</p><i id=i>x</i></div>");
        Range r = doc.createRange();
        Check.equal("#document:0-#document:0", show(r), "a new range is collapsed at the start of the document");
        Check.isTrue(r.isCollapsed(), "collapsed");
        r.setStart(el("p"), 1);
        Check.equal("p:1-p:1", show(r), "setting the start after the end moves the end too");
        r.setEnd(el("p"), 3);
        Check.equal("p:1-p:3", show(r), "setEnd");
        r.setEnd(el("p"), 0);
        Check.equal("p:0-p:0", show(r), "an end before the start moves the start");
        r.selectNode(el("b"));
        Check.equal("p:1-p:2", show(r), "selectNode");
        r.selectNodeContents(el("b"));
        Check.equal("b:0-b:1", show(r), "selectNodeContents");
        r.setStartBefore(el("b"));
        r.setEndAfter(el("i"));
        Check.equal("p:1-d:2", show(r), "before and after helpers");
        r.setStartAfter(el("b"));
        r.setEndBefore(el("i"));
        Check.equal("p:2-d:1", show(r), "after and before helpers");
        r.collapse(true);
        Check.equal("p:2-p:2", show(r), "collapse to the start");
        r.setEnd(el("d"), 2);
        r.collapse(false);
        Check.equal("d:2-d:2", show(r), "collapse to the end");
        Check.equal("d", name(r.getCommonAncestorContainer()), "common ancestor of a collapsed range");
        r.setStart(el("b"), 0);
        r.setEnd(el("i"), 1);
        Check.equal("d", name(r.getCommonAncestorContainer()), "common ancestor");
        final Range bad = r;
        DomTestSupport.throwsDom("IndexSizeError", new Block() {
            /**
             * Sets an offset beyond the node.
             * <p>
             * 노드를 넘는 오프셋을 지정한다.
             */
            public void run() {
                bad.setStart(el("b"), 2);
            }
        }, "offset beyond the length of the node");
        DomTestSupport.throwsDom("InvalidNodeTypeError", new Block() {
            /**
             * Uses a document type as a boundary container.
             * <p>
             * 문서 유형을 경계 컨테이너로 쓴다.
             */
            public void run() {
                bad.setStart(doc.getFirstChild(), 0);
            }
        }, "a document type cannot be a boundary container");
        DomTestSupport.throwsDom("InvalidNodeTypeError", new Block() {
            /**
             * Sets a boundary before a node without a parent.
             * <p>
             * 부모가 없는 노드 앞으로 경계를 지정한다.
             */
            public void run() {
                bad.setStartBefore(doc.createElement("span"));
            }
        }, "a node without a parent has no 'before'");
        // 다른 트리의 점으로 지정하면 반대쪽 끝도 그 점으로 옮겨 가 범위가 한 트리 안에 있게 된다.
        Element other = doc.createElement("section");
        r.setStart(other, 0);
        Check.equal("section:0-section:0", show(r), "a start in another tree moves the end into that tree");
    }

    /**
     * Comparisons: the four modes of compareBoundaryPoints, comparePoint, isPointInRange and intersectsNode; a range of another tree is an error.
     * <p>
     * 비교: compareBoundaryPoints 의 네 방식, comparePoint, isPointInRange, intersectsNode. 다른 트리의 범위는 오류.
     */
    public void testComparisons() {
        load("<!doctype html><div id=d><p id=p>abcdef</p><i id=i>xyz</i><u id=u>uuu</u></div>");
        final Range a = doc.createRange();
        a.setStart(el("p").firstChild, 1);
        a.setEnd(el("i").firstChild, 2);
        final Range b = doc.createRange();
        b.setStart(el("p").firstChild, 3);
        b.setEnd(el("u").firstChild, 1);
        Check.equal(-1, a.compareBoundaryPoints(Range.START_TO_START, b), "start to start");
        Check.equal(-1, a.compareBoundaryPoints(Range.END_TO_END, b), "end to end");
        Check.equal(1, a.compareBoundaryPoints(Range.START_TO_END, b), "the end of a is after the start of b");
        Check.equal(-1, a.compareBoundaryPoints(Range.END_TO_START, b), "the start of a is before the end of b");
        Check.equal(0, a.compareBoundaryPoints(Range.START_TO_START, a), "a range equals itself");
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Compares with an unknown mode.
             * <p>
             * 알 수 없는 방식으로 비교한다.
             */
            public void run() {
                a.compareBoundaryPoints((short) 7, b);
            }
        }, "unknown mode");
        final Range detached = doc.createRange();
        detached.setStart(doc.createElement("x"), 0);
        DomTestSupport.throwsDom("WrongDocumentError", new Block() {
            /**
             * Compares ranges of different trees.
             * <p>
             * 다른 트리의 범위를 비교한다.
             */
            public void run() {
                a.compareBoundaryPoints(Range.START_TO_START, detached);
            }
        }, "ranges of different trees");
        Check.equal(-1, a.comparePoint(el("p").firstChild, 0), "a point before the range");
        Check.equal(0, a.comparePoint(el("p").firstChild, 1), "a point at the start is inside");
        Check.equal(0, a.comparePoint(el("i").firstChild, 2), "a point at the end is inside");
        Check.equal(1, a.comparePoint(el("i").firstChild, 3), "a point after the range");
        Check.isTrue(a.isPointInRange(el("d"), 1), "a point of the parent between the children");
        Check.isFalse(a.isPointInRange(el("u").firstChild, 0), "a point after");
        Check.isFalse(a.isPointInRange(doc.createTextNode("x"), 0), "a point of another tree is not in the range");
        Check.isTrue(a.intersectsNode(el("p")), "p overlaps");
        Check.isTrue(a.intersectsNode(el("i")), "i overlaps");
        Check.isFalse(a.intersectsNode(el("u")), "u does not");
        Check.isTrue(a.intersectsNode(el("d")), "an ancestor overlaps");
        Check.isFalse(a.intersectsNode(doc.createElement("z")), "a node of another tree does not");
    }

    /**
     * toString joins the covered text nodes, cut at the boundary points, and ignores comments.
     * <p>
     * toString 은 덮는 텍스트 노드를 경계점에서 잘라 이어 붙이며 주석은 무시한다.
     */
    public void testToString() {
        load("<!doctype html><div id=d><p id=p>Hello <b>wor</b>ld</p><!--hidden--><p id=q>Second</p></div>");
        Range r = doc.createRange();
        r.setStart(el("p").firstChild, 2);
        r.setEnd(el("q").firstChild, 3);
        Check.equal("llo world" + "Sec", r.toString(), "text across elements, comments left out");
        r.selectNodeContents(el("d"));
        Check.equal("Hello worldSecond", r.toString(), "the whole div");
        r.setStart(el("p").firstChild, 1);
        r.setEnd(el("p").firstChild, 4);
        Check.equal("ell", r.toString(), "inside one text node");
        Check.equal("", doc.createRange().toString(), "a collapsed range is empty");
    }

    /**
     * cloneContents: the content between two text nodes of different paragraphs, partially contained elements, and the unchanged tree.
     * <p>
     * cloneContents: 서로 다른 문단의 두 텍스트 노드 사이의 내용, 부분적으로 포함된 요소, 그대로인 트리.
     */
    public void testCloneContents() {
        load("<!doctype html><div id=d><p id=p1>Hello <b id=b>wor</b>ld</p><p id=p2>Second</p></div>");
        Range r = doc.createRange();
        r.setStart(el("p1").firstChild, 2);
        r.setEnd(el("p2").firstChild, 3);
        DocumentFragment f = r.cloneContents();
        Check.equal("<p id=\"p1\">llo <b id=\"b\">wor</b>ld</p><p id=\"p2\">Sec</p>", inner(f), "cloned fragment");
        Check.equal("<p id=\"p1\">Hello <b id=\"b\">wor</b>ld</p><p id=\"p2\">Second</p>", inner(el("d")), "the tree did not change");
        // 한 텍스트 노드 안이면 그 일부의 복사본 하나만 나온다.
        r.setStart(el("b").firstChild, 1);
        r.setEnd(el("b").firstChild, 3);
        Check.equal("or", inner(r.cloneContents()), "a part of one text node");
        // 시작이 요소 경계점이면 완전히 포함된 자식이 깊은 복사본으로 나온다.
        r.setStart(el("d"), 0);
        r.setEnd(el("d"), 1);
        Check.equal("<p id=\"p1\">Hello <b id=\"b\">wor</b>ld</p>", inner(r.cloneContents()), "a contained child is cloned deeply");
        Check.equal("", inner(doc.createRange().cloneContents()), "a collapsed range clones nothing");
    }

    /**
     * extractContents and deleteContents: the example of the DOM Standard (two paragraphs), the collapse position, a part of one text node,
     * and the removal of contained nodes.
     * <p>
     * extractContents 와 deleteContents: DOM 표준의 예(문단 둘), 접히는 위치, 한 텍스트 노드의 일부, 포함된 노드의 제거.
     */
    public void testExtractAndDeleteContents() {
        load("<!doctype html><div id=d><p id=p1>Hello <b id=b>wor</b>ld</p><p id=p2>Second</p></div>");
        Range r = doc.createRange();
        r.setStart(el("p1").firstChild, 2);
        r.setEnd(el("p2").firstChild, 3);
        DocumentFragment f = r.extractContents();
        Check.equal("<p id=\"p1\">llo <b id=\"b\">wor</b>ld</p><p id=\"p2\">Sec</p>", inner(f), "extracted fragment");
        Check.equal("<p id=\"p1\">He</p><p id=\"p2\">ond</p>", inner(el("d")), "the tree after the extraction");
        Check.equal("d:1-d:1", show(r), "the range collapsed between the paragraphs");
        load("<!doctype html><div id=d><p id=p1>Hello <b id=b>wor</b>ld</p><p id=p2>Second</p></div>");
        r = doc.createRange();
        r.setStart(el("p1").firstChild, 2);
        r.setEnd(el("p2").firstChild, 3);
        r.deleteContents();
        Check.equal("<p id=\"p1\">He</p><p id=\"p2\">ond</p>", inner(el("d")), "the tree after the deletion");
        Check.equal("d:1-d:1", show(r), "collapsed after the deletion");
        load("<!doctype html><div id=d>0123456789</div>");
        r = doc.createRange();
        r.setStart(el("d").firstChild, 2);
        r.setEnd(el("d").firstChild, 5);
        Check.equal("234", inner(r.extractContents()), "extract a part of a text node");
        Check.equal("0156789", inner(el("d")), "the rest of the text");
        Check.isTrue(r.isCollapsed() && r.getStartContainer() == el("d").firstChild && r.getStartOffset() == 2, "collapsed at the cut: " + show(r));
        load("<!doctype html><div id=d><a id=a>1</a><b id=b>2</b><i id=i>3</i><u id=u>4</u></div>");
        r = doc.createRange();
        r.setStart(el("d"), 1);
        r.setEnd(el("d"), 3);
        r.deleteContents();
        Check.equal("<a id=\"a\">1</a><u id=\"u\">4</u>", inner(el("d")), "contained elements are removed");
        Check.equal("d:1-d:1", show(r), "collapsed in place");
    }

    /**
     * insertNode: splits a text node at the start, inserts before a child, extends a collapsed range, accepts a fragment, and refuses a
     * comment start or the start node itself.
     * <p>
     * insertNode: 시작의 텍스트 노드를 쪼개고, 자식 앞에 삽입하고, 접힌 범위를 늘리고, 조각을 받고, 주석 시작이나 시작 노드 자신은 거부한다.
     */
    public void testInsertNode() {
        load("<!doctype html><div id=d>abcdef<b id=b>x</b></div>");
        Range r = doc.createRange();
        r.setStart(el("d").firstChild, 3);
        r.setEnd(el("d").firstChild, 3);
        Element em = doc.createElement("em");
        r.insertNode(em);
        Check.equal("abc<em></em>def<b id=\"b\">x</b>", inner(el("d")), "the text node was split and the node inserted");
        Check.equal("'abc':3-d:2", show(r), "a collapsed range keeps its start and ends after the new node");
        r.setStart(el("d"), 0);
        r.setEnd(el("d"), 1);
        r.insertNode(doc.createElement("hr"));
        Check.equal("<hr>abc<em></em>def<b id=\"b\">x</b>", inner(el("d")), "inserted before the child at the offset");
        Check.equal("d:0-d:2", show(r), "a range that is not collapsed keeps its end");
        DocumentFragment frag = doc.createDocumentFragment();
        frag.appendChild(doc.createElement("s"));
        frag.appendChild(doc.createElement("q"));
        r.setStart(el("b"), 0);
        r.setEnd(el("b"), 0);
        r.insertNode(frag);
        Check.equal("<s></s><q></q>x", inner(el("b")), "a fragment inserts its children");
        Check.equal("b:0-b:2", show(r), "the collapsed range covers the inserted children");
        load("<!doctype html><div id=d><!--c-->text</div>");
        final Range bad = doc.createRange();
        bad.setStart(el("d").firstChild, 1);
        bad.setEnd(el("d").firstChild, 1);
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Inserts at the start inside a comment.
             * <p>
             * 주석 안의 시작에 삽입한다.
             */
            public void run() {
                bad.insertNode(doc.createElement("x"));
            }
        }, "a comment start");
        final Range same = doc.createRange();
        same.selectNodeContents(el("d"));
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Inserts the start node into itself.
             * <p>
             * 시작 노드를 자기 자신에 삽입한다.
             */
            public void run() {
                same.insertNode(el("d"));
            }
        }, "the start node itself");
    }

    /**
     * surroundContents: wraps the selected text, refuses a partially contained element and an invalid new parent.
     * <p>
     * surroundContents: 선택한 텍스트를 감싸고, 부분적으로 포함된 요소와 잘못된 새 부모는 거부한다.
     */
    public void testSurroundContents() {
        load("<!doctype html><div id=d>abcdef<b id=b>xyz</b></div>");
        Range r = doc.createRange();
        r.setStart(el("d").firstChild, 2);
        r.setEnd(el("d").firstChild, 4);
        Element mark = doc.createElement("mark");
        r.surroundContents(mark);
        Check.equal("ab<mark>cd</mark>ef<b id=\"b\">xyz</b>", inner(el("d")), "the selected text is wrapped");
        Check.equal("d:1-d:2", show(r), "the range selects the new parent");
        final Range partial = doc.createRange();
        partial.setStart(el("d").firstChild, 1);
        partial.setEnd(el("b").firstChild, 1);
        DomTestSupport.throwsDom("InvalidStateError", new Block() {
            /**
             * Wraps a range that partially contains the b element.
             * <p>
             * b 요소를 부분적으로 포함하는 범위를 감싼다.
             */
            public void run() {
                partial.surroundContents(doc.createElement("span"));
            }
        }, "partially contained element");
        final Range ok = doc.createRange();
        ok.selectNodeContents(el("b"));
        DomTestSupport.throwsDom("InvalidNodeTypeError", new Block() {
            /**
             * Wraps in a document fragment.
             * <p>
             * 문서 조각으로 감싼다.
             */
            public void run() {
                ok.surroundContents(doc.createDocumentFragment());
            }
        }, "a fragment cannot be the new parent");
    }

    /**
     * createContextualFragment parses markup in the context of the range start.
     * <p>
     * createContextualFragment 는 범위 시작의 문맥에서 마크업을 해석한다.
     */
    public void testCreateContextualFragment() {
        load("<!doctype html><div id=d>text</div>");
        Range r = doc.createRange();
        r.selectNodeContents(el("d"));
        DocumentFragment f = r.createContextualFragment("<b>bold</b> and <i>it</i>");
        Check.equal("<b>bold</b> and <i>it</i>", inner(f), "the markup became nodes");
        r.setStart(doc.getDocumentElement(), 0);
        Check.equal("<b>x</b>", inner(r.createContextualFragment("<b>x</b>")), "the root element uses a body context");
    }

    /**
     * Live ranges follow removals: a boundary inside a removed node moves to the place of the removal, the boundaries after it shift.
     * <p>
     * 살아 있는 범위는 제거를 따라간다: 제거된 노드 안의 경계는 제거된 자리로 옮겨 가고 그 뒤의 경계는 당겨진다.
     */
    public void testLiveRangesFollowRemovals() {
        load("<!doctype html><div id=d><a id=a>1</a><b id=b>2<i id=i>3</i></b><u id=u>4</u><s id=s>5</s></div>");
        Range inside = doc.createRange();
        inside.setStart(el("i").firstChild, 1);
        inside.setEnd(el("d"), 3);
        Range after = doc.createRange();
        after.setStart(el("d"), 4);
        after.setEnd(el("d"), 4);
        el("d").removeChild(el("b"));
        Check.equal("d:1-d:2", show(inside), "inside the removed node moves to its place, the end shifts");
        Check.equal("d:3-d:3", show(after), "after the removed node");
        el("d").removeChild(el("a"));
        Check.equal("d:0-d:1", show(inside), "another removal before the points");
        // 앞쪽 형제가 사라져도 같은 부모의 경계 오프셋보다 뒤의 변경은 영향이 없다.
        el("d").removeChild(el("s"));
        Check.equal("d:0-d:1", show(inside), "a removal after the points changes nothing");
        // textContent 로 자식을 모두 바꾸면 경계는 부모의 시작으로 모인다.
        el("d").setTextContent("new");
        Check.equal("d:0-d:0", show(inside), "all children replaced");
    }

    /**
     * Live ranges follow insertions and replaced character data, and the split of a text node.
     * <p>
     * 살아 있는 범위는 삽입, 교체된 문자 데이터, 텍스트 노드의 분할을 따라간다.
     */
    public void testLiveRangesFollowInsertionsAndData() {
        load("<!doctype html><div id=d><a id=a>1</a><b id=b>2</b></div><p id=p>0123456789</p>");
        Range r = doc.createRange();
        r.setStart(el("d"), 1);
        r.setEnd(el("d"), 2);
        el("d").insertBefore(doc.createElement("hr"), el("a"));
        Check.equal("d:2-d:3", show(r), "an insertion before the points shifts them");
        el("d").appendChild(doc.createElement("br"));
        Check.equal("d:2-d:3", show(r), "an insertion after the points does not");
        el("d").insertBefore(doc.createElement("wbr"), el("b"));
        Check.equal("d:2-d:4", show(r), "an insertion between the points moves the end");
        Range t = doc.createRange();
        CharacterData text = (CharacterData) el("p").firstChild;
        t.setStart(text, 3);
        t.setEnd(text, 8);
        text.insertData(1, "XY");
        Check.equal(5, t.getStartOffset(), "data inserted before the points shifts the start");
        Check.equal(10, t.getEndOffset(), "data inserted before the points shifts the end");
        text.deleteData(4, 4);
        Check.equal(4, t.getStartOffset(), "deleted data over the start clamps it to the start of the deletion");
        Check.equal(6, t.getEndOffset(), "deleted data before the end pulls it");
        text.replaceData(0, 2, "");
        Check.equal(2, t.getStartOffset(), "the start after another deletion");
        Check.equal(4, t.getEndOffset(), "the end after another deletion");
        text.appendData("tail");
        Check.equal(4, t.getEndOffset(), "data appended after the end changes nothing");
        // 텍스트 노드를 쪼개면 쪼갠 위치 뒤의 경계는 새 노드로 옮겨 간다.
        Text first = (Text) text;
        Text second = first.splitText(3);
        Check.isTrue(t.getStartContainer() == first && t.getStartOffset() == 2, "the start before the split stays");
        Check.isTrue(t.getEndContainer() == second && t.getEndOffset() == 1, "the end after the split moves to the new node: " + show(t));
    }

    /**
     * The extraction of a range leaves other live ranges consistent, and a cloned range is independent.
     * <p>
     * 범위의 추출은 다른 살아 있는 범위를 일관되게 두고, 복제한 범위는 독립적이다.
     */
    public void testOtherRangesDuringExtractAndCloneRange() {
        load("<!doctype html><div id=d><a id=a>1</a><b id=b>2</b><i id=i>3</i></div>");
        Range extract = doc.createRange();
        extract.setStart(el("d"), 0);
        extract.setEnd(el("d"), 2);
        Range watcher = doc.createRange();
        watcher.setStart(el("i"), 0);
        watcher.setEnd(el("i"), 1);
        Range copy = extract.cloneRange();
        extract.extractContents();
        Check.equal("i:0-i:1", show(watcher), "a range in an element that stays is untouched");
        Check.equal("d:0-d:0", show(copy), "the copy followed the removal like any live range");
        Check.equal("d:0-d:0", show(extract), "the extracted range collapsed");
        copy.setStart(el("i"), 0);
        Check.equal("d:0-d:0", show(extract), "the copy is independent");
        extract.detach();
    }
}
