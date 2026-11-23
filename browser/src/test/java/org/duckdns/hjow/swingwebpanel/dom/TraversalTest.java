package org.duckdns.hjow.swingwebpanel.dom;

import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link NodeIterator} and {@link TreeWalker} (plan item P20-03, DOM Standard "Traversal"): the order of the nodes, whatToShow
 * and filters (accept, skip, reject), the walker moves over the structure, the filter that must not run twice at the same time, and the
 * node iterator that keeps a valid reference when nodes are removed.
 * <p>
 * {@link NodeIterator} 와 {@link TreeWalker} 시험(계획 P20-03, DOM 표준 "Traversal"): 노드의 순서, whatToShow 와 필터(accept, skip, reject), 구조를 따라
 * 가는 걷는 도구의 이동, 동시에 두 번 실행될 수 없는 필터, 노드가 제거되어도 유효한 기준을 유지하는 노드 반복자.
 */
public class TraversalTest {

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
    public TraversalTest() {
    }

    /**
     * Creates a document with a tree before each case: r(a("A", i("I")), b("B"), comment, u("U", s("S"), "end")).
     * <p>
     * 사례마다 트리가 있는 문서를 만든다: r(a("A", i("I")), b("B"), 주석, u("U", s("S"), "end")).
     */
    public void setUp() {
        doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        new HtmlParser(doc, "<!doctype html><div id=r><a id=a>A<i id=i>I</i></a><b id=b>B</b><!--c--><u id=u>U<s id=s>S</s>end</u></div>").run();
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
     * Names a node: the id of an element, the text of a text node, <code>#c</code> for a comment.
     * <p>
     * 노드 이름을 짓는다: 요소는 id, 텍스트 노드는 내용, 주석은 <code>#c</code>.
     *
     * @param n the node, may be null / 노드. null 가능
     * @return the name / 이름
     */
    private static String name(Node n) {
        if (n == null) {
            return "null";
        }
        if (n instanceof Element) {
            return ((Element) n).getId();
        }
        if (n instanceof Comment) {
            return "#c";
        }
        return "'" + ((CharacterData) n).getData() + "'";
    }

    /**
     * Collects the nodes that an iterator returns, until null.
     * <p>
     * 반복자가 null 을 반환할 때까지 돌려주는 노드를 모은다.
     *
     * @param it the iterator / 반복자
     * @return the names joined by spaces / 공백으로 이은 이름
     */
    private static String forward(NodeIterator it) {
        StringBuilder sb = new StringBuilder();
        // 끝에 닿을 때까지 다음 노드를 모은다.
        for (Node n = it.nextNode(); n != null; n = it.nextNode()) {
            sb.append(sb.length() == 0 ? "" : " ").append(name(n));
        }
        return sb.toString();
    }

    /**
     * Collects the nodes that an iterator returns going backwards, until null.
     * <p>
     * 반복자가 뒤로 가며 null 을 반환할 때까지 돌려주는 노드를 모은다.
     *
     * @param it the iterator / 반복자
     * @return the names joined by spaces / 공백으로 이은 이름
     */
    private static String backward(NodeIterator it) {
        StringBuilder sb = new StringBuilder();
        // 처음에 닿을 때까지 이전 노드를 모은다.
        for (Node n = it.previousNode(); n != null; n = it.previousNode()) {
            sb.append(sb.length() == 0 ? "" : " ").append(name(n));
        }
        return sb.toString();
    }

    /**
     * A filter that answers with a fixed result for one element id and accepts everything else.
     * <p>
     * 요소 하나의 id 에는 정해진 결과로, 나머지는 모두 받아들이는 필터.
     *
     * @param id     the id of the element / 요소의 id
     * @param result the result for that element / 그 요소의 결과
     * @return the filter / 필터
     */
    private static NodeFilter answer(final String id, final short result) {
        return new NodeFilter() {
            /**
             * Answers for the element and accepts every other node.
             * <p>
             * 그 요소에는 정해진 결과로 답하고 다른 노드는 모두 받아들인다.
             *
             * @param node the node / 노드
             * @return the result / 결과
             */
            public short acceptNode(Node node) {
                return node instanceof Element && id.equals(((Element) node).getId()) ? result : NodeFilter.FILTER_ACCEPT;
            }
        };
    }

    /**
     * A node iterator visits every node in tree order, forward and backward, with the pointer semantics of the Standard.
     * <p>
     * 노드 반복자는 모든 노드를 트리 순서로 앞으로도 뒤로도 방문하며 표준의 포인터 의미를 따른다.
     */
    public void testIteratorOrder() {
        NodeIterator it = doc.createNodeIterator(el("r"), NodeFilter.SHOW_ALL, null);
        Check.same(el("r"), it.getReferenceNode(), "the iterator starts at the root");
        Check.isTrue(it.isPointerBeforeReferenceNode(), "before the root");
        Check.equal("r a 'A' i 'I' b 'B' #c u 'U' s 'S' 'end'", forward(it), "document order");
        Check.isNull(it.nextNode(), "stays at the end");
        Check.equal("'end' 'S' s 'U' u #c 'B' b 'I' i 'A' a r", backward(it), "reverse order");
        Check.isNull(it.previousNode(), "stays at the start");
        Check.equal("r", name(it.nextNode()), "the pointer came back before the root");
        it.detach();
        Check.equal("a", name(it.nextNode()), "detach changes nothing");
    }

    /**
     * whatToShow selects node kinds; filters decide the rest, and a rejected node still lets the iterator look at its children.
     * <p>
     * whatToShow 는 노드 종류를 고르고 나머지는 필터가 정하며, 거부된 노드도 반복자는 그 자식을 계속 살펴본다.
     */
    public void testIteratorWhatToShowAndFilters() {
        Check.equal("r a i b u s", forward(doc.createNodeIterator(el("r"), NodeFilter.SHOW_ELEMENT, null)), "elements");
        Check.equal("'A' 'I' 'B' 'U' 'S' 'end'", forward(doc.createNodeIterator(el("r"), NodeFilter.SHOW_TEXT, null)), "text");
        Check.equal("#c", forward(doc.createNodeIterator(el("r"), NodeFilter.SHOW_COMMENT, null)), "comments");
        Check.equal("'A' #c", forward(doc.createNodeIterator(el("r"), NodeFilter.SHOW_TEXT | NodeFilter.SHOW_COMMENT, new NodeFilter() {
            /**
             * Accepts the comment and the first text only.
             * <p>
             * 주석과 첫 텍스트만 받아들인다.
             *
             * @param node the node / 노드
             * @return the result / 결과
             */
            public short acceptNode(Node node) {
                return node instanceof Comment || "A".equals(((CharacterData) node).getData()) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP;
            }
        })), "kinds and filter together");
        Check.equal("r 'A' i 'I' b 'B' #c u 'U' s 'S' 'end'",
                forward(doc.createNodeIterator(el("r"), NodeFilter.SHOW_ALL, answer("a", NodeFilter.FILTER_REJECT))),
                "reject acts like skip for an iterator");
        Check.equal("r a 'A' 'I' b 'B' #c u 'U' s 'S' 'end'",
                forward(doc.createNodeIterator(el("r"), NodeFilter.SHOW_ALL, answer("i", NodeFilter.FILTER_SKIP))), "skip");
        Check.equal("i 'I'", forward(doc.createNodeIterator(el("i"), NodeFilter.SHOW_ALL, null)), "the root bounds the iteration to a subtree");
    }

    /**
     * The tree walker moves over the structure: parent, children, siblings, document order in both directions.
     * <p>
     * 트리 걷기 도구는 구조를 따라 움직인다: 부모, 자식, 형제, 양방향 문서 순서.
     */
    public void testWalkerStructure() {
        TreeWalker w = doc.createTreeWalker(el("r"), NodeFilter.SHOW_ELEMENT, null);
        Check.same(el("r"), w.getCurrentNode(), "starts at the root");
        Check.equal("a", name(w.firstChild()), "first child");
        Check.equal("i", name(w.firstChild()), "first child of a");
        Check.isNull(w.firstChild(), "i has no element child");
        Check.equal("i", name(w.getCurrentNode()), "the current node stays when there is no move");
        Check.isNull(w.nextSibling(), "i is the last element in a");
        Check.isNull(w.previousSibling(), "and the first");
        Check.equal("a", name(w.parentNode()), "parent");
        Check.equal("b", name(w.nextSibling()), "next sibling");
        Check.equal("u", name(w.nextSibling()), "the comment is not shown, next element sibling");
        Check.equal("s", name(w.lastChild()), "last child of u");
        Check.equal("u", name(w.parentNode()), "parent of s");
        w.setCurrentNode(el("r"));
        Check.isNull(w.parentNode(), "no parent above the root");
        Check.isNull(w.previousSibling(), "the root has no siblings for the walker");
        Check.equal("u", name(w.lastChild()), "last child of the root");
        Check.equal("b", name(w.previousSibling()), "previous sibling");
        Check.equal("a", name(w.previousSibling()), "previous sibling again");
        Check.isNull(w.previousSibling(), "no more");
        w.setCurrentNode(el("r"));
        StringBuilder order = new StringBuilder();
        // 문서 순서로 모든 요소를 방문한다.
        for (Node n = w.nextNode(); n != null; n = w.nextNode()) {
            order.append(order.length() == 0 ? "" : " ").append(name(n));
        }
        Check.equal("a i b u s", order.toString(), "nextNode");
        order.setLength(0);
        // 반대 순서로 되짚는다.
        for (Node n = w.previousNode(); n != null; n = w.previousNode()) {
            order.append(order.length() == 0 ? "" : " ").append(name(n));
        }
        Check.equal("u b i a r", order.toString(), "previousNode goes back to the root, which the walker accepts as a parent");
        Check.equal("r", name(w.getCurrentNode()), "the walker is back at the root");
    }

    /**
     * Reject hides a whole subtree from the walker, skip only the node; the filter sees each node once per question.
     * <p>
     * reject 는 걷는 도구에게서 하위 트리 전체를, skip 은 노드만 감춘다. 필터는 질문마다 각 노드를 한 번씩 본다.
     */
    public void testWalkerRejectAndSkip() {
        TreeWalker rejecting = doc.createTreeWalker(el("r"), NodeFilter.SHOW_ALL, answer("a", NodeFilter.FILTER_REJECT));
        StringBuilder order = new StringBuilder();
        // reject 된 a 의 하위 트리 전체가 빠진다.
        for (Node n = rejecting.nextNode(); n != null; n = rejecting.nextNode()) {
            order.append(order.length() == 0 ? "" : " ").append(name(n));
        }
        Check.equal("b 'B' #c u 'U' s 'S' 'end'", order.toString(), "a and its subtree are hidden");
        Check.equal("b", name(doc.createTreeWalker(el("r"), NodeFilter.SHOW_ELEMENT, answer("a", NodeFilter.FILTER_REJECT)).firstChild()),
                "firstChild looks past the rejected node to the sibling");
        TreeWalker skipping = doc.createTreeWalker(el("r"), NodeFilter.SHOW_ELEMENT, answer("a", NodeFilter.FILTER_SKIP));
        Check.equal("i", name(skipping.firstChild()), "a skipped node lets firstChild look at its children");
        skipping.setCurrentNode(el("r"));
        order.setLength(0);
        // skip 된 a 는 빠지고 그 자식은 방문한다.
        for (Node n = skipping.nextNode(); n != null; n = skipping.nextNode()) {
            order.append(order.length() == 0 ? "" : " ").append(name(n));
        }
        Check.equal("i b u s", order.toString(), "a is skipped but not its children");
        skipping.setCurrentNode(el("i"));
        Check.equal("r", name(skipping.parentNode()), "parentNode passes the skipped parent");
        Check.equal(NodeFilter.SHOW_ELEMENT, skipping.getWhatToShow(), "whatToShow");
        Check.notNull(skipping.getFilter(), "the filter");
    }

    /**
     * A filter must not call the traversal again while it runs.
     * <p>
     * 필터는 실행되는 동안 탐색을 다시 부르면 안 된다.
     */
    public void testFilterCannotBeReentered() {
        final NodeIterator[] holder = new NodeIterator[1];
        holder[0] = doc.createNodeIterator(el("r"), NodeFilter.SHOW_ALL, new NodeFilter() {
            /**
             * Calls the iterator that is asking.
             * <p>
             * 묻고 있는 반복자를 부른다.
             *
             * @param node the node / 노드
             * @return never / 반환하지 않음
             */
            public short acceptNode(Node node) {
                holder[0].nextNode();
                return NodeFilter.FILTER_ACCEPT;
            }
        });
        DomTestSupport.throwsDom("InvalidStateError", new Block() {
            /**
             * Starts a traversal whose filter calls it again.
             * <p>
             * 필터가 다시 부르는 탐색을 시작한다.
             */
            public void run() {
                holder[0].nextNode();
            }
        }, "re-entering the filter");
        Check.equal("r", name(doc.createNodeIterator(el("r"), NodeFilter.SHOW_ALL, null).nextNode()), "a fresh iterator works");
    }

    /**
     * A node iterator stays valid when the node holding its reference is removed: it moves to the node after the removed ones, or to the node
     * before them, as the DOM Standard says.
     * <p>
     * 노드 반복자는 기준 노드를 가진 노드가 제거되어도 유효하다: 표준대로 제거된 노드 다음 노드나 그 앞의 노드로 옮겨 간다.
     */
    public void testIteratorFollowsRemovals() {
        NodeIterator it = doc.createNodeIterator(el("r"), NodeFilter.SHOW_ALL, null);
        // 기준이 I 이고 포인터가 그 뒤일 때 a 를 제거하면 기준은 a 앞의 노드 r 로 가고 포인터는 뒤에 남는다.
        Check.equal("r", name(it.nextNode()), "r");
        Check.equal("a", name(it.nextNode()), "a");
        Check.equal("'A'", name(it.nextNode()), "A");
        Check.equal("i", name(it.nextNode()), "i");
        Check.equal("'I'", name(it.nextNode()), "I");
        el("r").removeChild(el("a"));
        Check.same(el("r"), it.getReferenceNode(), "the reference moved to the node before the removed subtree");
        Check.isFalse(it.isPointerBeforeReferenceNode(), "and stays after it");
        Check.equal("b", name(it.nextNode()), "the iteration goes on with the node after the removed one");
        Check.equal("'B'", name(it.nextNode()), "B");
        // previousNode 로 포인터를 기준 앞에 둔 채 기준을 가진 b 를 제거하면 기준은 b 다음 노드로 가고 포인터는 그 앞에 남는다.
        Check.equal("'B'", name(it.previousNode()), "the reference node again, now the pointer is before it");
        Check.isTrue(it.isPointerBeforeReferenceNode(), "the pointer is before B");
        el("r").removeChild(el("b"));
        Check.equal("#c", name(it.getReferenceNode()), "the reference moved to the node after the removed one");
        Check.isTrue(it.isPointerBeforeReferenceNode(), "and the pointer stays before it");
        Check.equal("#c", name(it.nextNode()), "the next node is that node");
        // 루트를 품은 노드를 제거할 때는 반복자를 건드리지 않는다.
        NodeIterator inner = doc.createNodeIterator(el("u"), NodeFilter.SHOW_ALL, null);
        inner.nextNode();
        inner.nextNode();
        el("r").removeChild(el("u"));
        Check.equal("'U'", name(inner.getReferenceNode()), "the reference stays in the removed subtree that holds the root");
    }

    /**
     * The root must not be null.
     * <p>
     * 루트는 null 일 수 없다.
     */
    public void testRootIsRequired() {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Creates an iterator without a root.
             * <p>
             * 루트 없이 반복자를 만든다.
             */
            public void run() {
                doc.createNodeIterator(null, NodeFilter.SHOW_ALL, null);
            }
        }, "null root");
    }
}
