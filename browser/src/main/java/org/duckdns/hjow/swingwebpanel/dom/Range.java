package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * A range of content in a DOM tree (DOM Standard, "Interface Range"): two boundary points, each a node and an offset in it (a character
 * offset in a text-like node, a child index in any other node). A range is live: when nodes are inserted or removed, or character data is
 * replaced, the boundary points move with the content (see {@link LiveRanges}), so that a range keeps standing for the same content. It
 * offers the boundary operations ({@link #setStart}, {@link #selectNode}, {@link #collapse} ...), the comparison of boundary points, the
 * operations on the content ({@link #cloneContents()}, {@link #extractContents()}, {@link #deleteContents()}, {@link #insertNode},
 * {@link #surroundContents}), {@link #toString()}, and {@link #createContextualFragment} of the DOM Parsing specification. Geometry
 * (<code>getBoundingClientRect</code>) is not part of this class; the host adds it because it needs the layout.
 * <p>
 * Restriction against the Standard: the changes of a tree of another document do not move a boundary point that was set into that tree
 * (the registry of live ranges belongs to the document that created the range).
 * <p>
 * DOM 트리 안의 내용 범위(DOM 표준 "Interface Range"): 두 경계점이며 각각 노드와 그 안의 오프셋(텍스트 계열 노드는 문자 오프셋, 그 밖의 노드는 자식
 * 인덱스)이다. 범위는 살아 있다: 노드가 삽입되거나 제거되거나 문자 데이터가 바뀌면 경계점이 내용과 함께 움직여({@link LiveRanges} 참조) 범위가 계속
 * 같은 내용을 가리킨다. 경계 연산({@link #setStart}, {@link #selectNode}, {@link #collapse} 등), 경계점 비교, 내용 연산({@link #cloneContents()},
 * {@link #extractContents()}, {@link #deleteContents()}, {@link #insertNode}, {@link #surroundContents}), {@link #toString()}, DOM Parsing 규격의
 * {@link #createContextualFragment} 를 제공한다. 기하(<code>getBoundingClientRect</code>)는 레이아웃이 필요하므로 이 클래스가 아니라 호스트가 더한다.
 * <p>
 * 표준과 다른 제한: 다른 문서의 트리에 정한 경계점은 그 문서의 변경을 따라가지 않는다(살아 있는 범위의 등록부는 범위를 만든 문서의 것이다).
 */
public final class Range {

    /**
     * <code>compareBoundaryPoints</code> mode: the start of this range with the start of the other.
     * <p>
     * <code>compareBoundaryPoints</code> 방식: 이 범위의 시작과 다른 범위의 시작.
     */
    public static final short START_TO_START = 0;

    /**
     * Mode: the end of this range with the start of the other.
     * <p>
     * 방식: 이 범위의 끝과 다른 범위의 시작.
     */
    public static final short START_TO_END = 1;

    /**
     * Mode: the end of this range with the end of the other.
     * <p>
     * 방식: 이 범위의 끝과 다른 범위의 끝.
     */
    public static final short END_TO_END = 2;

    /**
     * Mode: the start of this range with the end of the other.
     * <p>
     * 방식: 이 범위의 시작과 다른 범위의 끝.
     */
    public static final short END_TO_START = 3;

    /**
     * The document that created the range (the one that a new range is collapsed at the start of).
     * <p>
     * 범위를 만든 문서(새 범위는 그 문서의 시작에 접혀 있다).
     */
    private final Document document;

    /**
     * The node of the start boundary point.
     * <p>
     * 시작 경계점의 노드.
     */
    private Node startContainer;

    /**
     * The offset of the start boundary point.
     * <p>
     * 시작 경계점의 오프셋.
     */
    private int startOffset;

    /**
     * The node of the end boundary point.
     * <p>
     * 끝 경계점의 노드.
     */
    private Node endContainer;

    /**
     * The offset of the end boundary point.
     * <p>
     * 끝 경계점의 오프셋.
     */
    private int endOffset;

    /**
     * The selection that uses this range as its range, or null (then moving a boundary point tells nobody).
     * <p>
     * 이 범위를 자기 범위로 쓰는 선택. 없으면 null(그러면 경계점이 움직여도 아무에게도 알리지 않는다).
     */
    private Selection selection;

    /**
     * Creates a range in a document, collapsed at the start of the document, and registers it so that it follows the changes of the tree.
     * <p>
     * 문서에 범위를 만든다. 문서의 시작에 접혀 있으며, 트리의 변경을 따라가도록 등록한다.
     *
     * @param document the document, not null / 문서(null 불가)
     */
    public Range(Document document) {
        this(document, true);
    }

    /**
     * Creates a range, optionally without registering it (the short-lived ranges of the content algorithms are not registered).
     * <p>
     * 범위를 만든다. 등록하지 않을 수도 있다(내용 알고리즘이 쓰는 잠깐 쓰는 범위는 등록하지 않는다).
     *
     * @param document the document, not null / 문서(null 불가)
     * @param live     true to register the range with the document / 문서에 등록하려면 true
     */
    private Range(Document document, boolean live) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        this.document = document;
        this.startContainer = document;
        this.endContainer = document;
        if (live) {
            document.liveRegistry().add(this);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 경계점 조회
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the document that created the range.
     * <p>
     * 범위를 만든 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the node of the start boundary point.
     * <p>
     * 시작 경계점의 노드를 반환한다.
     *
     * @return the node / 노드
     */
    public Node getStartContainer() {
        return startContainer;
    }

    /**
     * Returns the offset of the start boundary point.
     * <p>
     * 시작 경계점의 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * Returns the node of the end boundary point.
     * <p>
     * 끝 경계점의 노드를 반환한다.
     *
     * @return the node / 노드
     */
    public Node getEndContainer() {
        return endContainer;
    }

    /**
     * Returns the offset of the end boundary point.
     * <p>
     * 끝 경계점의 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public int getEndOffset() {
        return endOffset;
    }

    /**
     * Tells whether the two boundary points are the same.
     * <p>
     * 두 경계점이 같은지 알려 준다.
     *
     * @return true if collapsed / 접혀 있으면 true
     */
    public boolean isCollapsed() {
        return startContainer == endContainer && startOffset == endOffset;
    }

    /**
     * Returns the deepest node that contains both boundary points.
     * <p>
     * 두 경계점을 모두 포함하는 가장 깊은 노드를 반환한다.
     *
     * @return the common ancestor container / 공통 조상 컨테이너
     */
    public Node getCommonAncestorContainer() {
        Node n = startContainer;
        // 끝 경계의 노드를 포함하는 노드를 찾을 때까지 시작 노드에서 위로 올라간다.
        while (!n.contains(endContainer)) {
            n = n.parent;
        }
        return n;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 노드 도우미
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the length of a node as a boundary container: the number of characters of character data, 0 for a document type, the number
     * of children for every other node.
     * <p>
     * 경계 컨테이너로서의 노드 길이를 반환한다: 문자 데이터는 문자 수, 문서 유형은 0, 나머지는 자식 수.
     *
     * @param node the node / 노드
     * @return the length / 길이
     */
    static int length(Node node) {
        if (node instanceof CharacterData) {
            return ((CharacterData) node).getLength();
        }
        if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            return 0;
        }
        int n = 0;
        // 자식 수를 센다.
        for (Node c = node.firstChild; c != null; c = c.nextSibling) {
            n++;
        }
        return n;
    }

    /**
     * Returns the index of a node among the children of its parent.
     * <p>
     * 부모의 자식들 중 노드의 인덱스를 반환한다.
     *
     * @param node the node (it has a parent) / 노드(부모가 있다)
     * @return the index / 인덱스
     */
    static int indexOf(Node node) {
        int i = 0;
        // 앞의 형제를 센다.
        for (Node s = node.prevSibling; s != null; s = s.prevSibling) {
            i++;
        }
        return i;
    }

    /**
     * Returns the child of a node at an index, or null.
     * <p>
     * 노드의 해당 인덱스 자식을 반환한다. 없으면 null.
     *
     * @param node  the node / 노드
     * @param index the index / 인덱스
     * @return the child or null / 자식 또는 null
     */
    private static Node childAt(Node node, int index) {
        Node c = node.firstChild;
        // 인덱스만큼 형제를 따라간다.
        for (int i = 0; c != null && i < index; i++) {
            c = c.nextSibling;
        }
        return c;
    }

    /**
     * Compares two nodes of one tree in tree order.
     * <p>
     * 한 트리의 두 노드를 트리 순서로 비교한다.
     *
     * @param a the first node / 첫째 노드
     * @param b the second node / 둘째 노드
     * @return -1 if a is before b (an ancestor is before its descendants), 0 if the same, 1 if after / a 가 b 보다 앞이면(조상은 후손보다 앞) -1, 같으면 0, 뒤이면 1
     */
    static int compareNodes(Node a, Node b) {
        if (a == b) {
            return 0;
        }
        List<Node> pa = pathFromRoot(a);
        List<Node> pb = pathFromRoot(b);
        int n = Math.min(pa.size(), pb.size());
        // 루트에서 내려오는 경로가 처음 갈라지는 곳을 찾는다.
        for (int i = 0; i < n; i++) {
            if (pa.get(i) != pb.get(i)) {
                return indexOf(pa.get(i)) < indexOf(pb.get(i)) ? -1 : 1;
            }
        }
        return pa.size() < pb.size() ? -1 : 1;
    }

    /**
     * Returns the nodes from the root of the tree down to a node.
     * <p>
     * 트리의 루트에서 노드까지의 노드 목록을 반환한다.
     *
     * @param node the node / 노드
     * @return the path, root first / 루트가 처음인 경로
     */
    private static List<Node> pathFromRoot(Node node) {
        ArrayList<Node> path = new ArrayList<Node>();
        // 부모를 따라 올라가며 모은 뒤 뒤집는다.
        for (Node n = node; n != null; n = n.parent) {
            path.add(0, n);
        }
        return path;
    }

    /**
     * Compares a boundary point with another one of the same tree: the "position of a boundary point relative to another" algorithm.
     * <p>
     * 경계점을 같은 트리의 다른 경계점과 비교한다: "position of a boundary point relative to another" 알고리즘.
     *
     * @param nodeA   the node of the first point / 첫 점의 노드
     * @param offsetA the offset of the first point / 첫 점의 오프셋
     * @param nodeB   the node of the second point / 둘째 점의 노드
     * @param offsetB the offset of the second point / 둘째 점의 오프셋
     * @return -1 if the first point is before the second, 0 if equal, 1 if after / 첫 점이 앞이면 -1, 같으면 0, 뒤이면 1
     */
    static int comparePoints(Node nodeA, int offsetA, Node nodeB, int offsetB) {
        if (nodeA == nodeB) {
            return offsetA < offsetB ? -1 : (offsetA == offsetB ? 0 : 1);
        }
        int order = compareNodes(nodeA, nodeB);
        if (order > 0) {
            return -comparePoints(nodeB, offsetB, nodeA, offsetA);
        }
        // A 가 B 의 조상이면 B 쪽 자식의 위치와 A 의 오프셋을 비교한다.
        if (nodeA.contains(nodeB)) {
            Node child = nodeB;
            // A 의 직계 자식을 만날 때까지 B 에서 위로 올라간다.
            while (child.parent != nodeA) {
                child = child.parent;
            }
            if (indexOf(child) < offsetA) {
                return 1;
            }
        }
        return -1;
    }

    /**
     * Checks that a node can be a boundary container (a document type cannot) and that the offset lies inside it.
     * <p>
     * 노드가 경계 컨테이너가 될 수 있고(문서 유형은 안 된다) 오프셋이 그 안에 있는지 검사한다.
     *
     * @param node   the node / 노드
     * @param offset the offset / 오프셋
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    private static void checkBoundary(Node node, int offset) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            throw new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "a document type cannot be a boundary point");
        }
        if (offset < 0 || offset > length(node)) {
            throw DomException.indexSize("the offset " + offset + " is larger than the length of the node");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 경계 지정
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Sets the start boundary point. If the new start is after the end, or in another tree, the end moves to the new start as well.
     * <p>
     * 시작 경계점을 지정한다. 새 시작이 끝보다 뒤이거나 다른 트리이면 끝도 새 시작으로 옮겨 간다.
     *
     * @param node   the node / 노드
     * @param offset the offset / 오프셋
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    public void setStart(Node node, int offset) {
        checkBoundary(node, offset);
        if (startContainer.getRootNode() != node.getRootNode() || comparePoints(node, offset, endContainer, endOffset) > 0) {
            endContainer = node;
            endOffset = offset;
        }
        startContainer = node;
        startOffset = offset;
        touched();
    }

    /**
     * Sets the end boundary point. If the new end is before the start, or in another tree, the start moves to the new end as well.
     * <p>
     * 끝 경계점을 지정한다. 새 끝이 시작보다 앞이거나 다른 트리이면 시작도 새 끝으로 옮겨 간다.
     *
     * @param node   the node / 노드
     * @param offset the offset / 오프셋
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    public void setEnd(Node node, int offset) {
        checkBoundary(node, offset);
        if (startContainer.getRootNode() != node.getRootNode() || comparePoints(node, offset, startContainer, startOffset) < 0) {
            startContainer = node;
            startOffset = offset;
        }
        endContainer = node;
        endOffset = offset;
        touched();
    }

    /**
     * Sets the start before a node.
     * <p>
     * 노드 앞으로 시작을 지정한다.
     *
     * @param node the node (it needs a parent) / 노드(부모가 있어야 한다)
     * @throws DomException InvalidNodeTypeError if the node has no parent / 부모가 없으면 InvalidNodeTypeError
     */
    public void setStartBefore(Node node) {
        setStart(parentOf(node), indexOf(node));
    }

    /**
     * Sets the start after a node.
     * <p>
     * 노드 뒤로 시작을 지정한다.
     *
     * @param node the node (it needs a parent) / 노드(부모가 있어야 한다)
     * @throws DomException InvalidNodeTypeError if the node has no parent / 부모가 없으면 InvalidNodeTypeError
     */
    public void setStartAfter(Node node) {
        setStart(parentOf(node), indexOf(node) + 1);
    }

    /**
     * Sets the end before a node.
     * <p>
     * 노드 앞으로 끝을 지정한다.
     *
     * @param node the node (it needs a parent) / 노드(부모가 있어야 한다)
     * @throws DomException InvalidNodeTypeError if the node has no parent / 부모가 없으면 InvalidNodeTypeError
     */
    public void setEndBefore(Node node) {
        setEnd(parentOf(node), indexOf(node));
    }

    /**
     * Sets the end after a node.
     * <p>
     * 노드 뒤로 끝을 지정한다.
     *
     * @param node the node (it needs a parent) / 노드(부모가 있어야 한다)
     * @throws DomException InvalidNodeTypeError if the node has no parent / 부모가 없으면 InvalidNodeTypeError
     */
    public void setEndAfter(Node node) {
        setEnd(parentOf(node), indexOf(node) + 1);
    }

    /**
     * Returns the parent of a node for the "before/after" operations, which fail for a node without a parent.
     * <p>
     * "before/after" 연산을 위해 노드의 부모를 반환한다. 부모가 없는 노드이면 실패한다.
     *
     * @param node the node / 노드
     * @return the parent / 부모
     */
    private static Node parentOf(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.parent == null) {
            throw new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "the node has no parent");
        }
        return node.parent;
    }

    /**
     * Collapses the range to one of its boundary points.
     * <p>
     * 범위를 경계점 하나로 접는다.
     *
     * @param toStart true to collapse to the start, false to the end / 시작으로 접으려면 true, 끝으로 접으려면 false
     */
    public void collapse(boolean toStart) {
        if (toStart) {
            endContainer = startContainer;
            endOffset = startOffset;
        } else {
            startContainer = endContainer;
            startOffset = endOffset;
        }
        touched();
    }

    /**
     * Makes the range surround a node (the start before it, the end after it).
     * <p>
     * 범위가 노드를 감싸게 한다(노드 앞에서 시작해 노드 뒤에서 끝난다).
     *
     * @param node the node (it needs a parent) / 노드(부모가 있어야 한다)
     * @throws DomException InvalidNodeTypeError if the node has no parent / 부모가 없으면 InvalidNodeTypeError
     */
    public void selectNode(Node node) {
        Node parent = parentOf(node);
        int index = indexOf(node);
        startContainer = parent;
        startOffset = index;
        endContainer = parent;
        endOffset = index + 1;
        touched();
    }

    /**
     * Makes the range cover everything inside a node.
     * <p>
     * 범위가 노드 안의 모든 것을 덮게 한다.
     *
     * @param node the node / 노드
     * @throws DomException InvalidNodeTypeError for a document type / 문서 유형이면 InvalidNodeTypeError
     */
    public void selectNodeContents(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            throw new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "a document type has no contents");
        }
        startContainer = node;
        startOffset = 0;
        endContainer = node;
        endOffset = length(node);
        touched();
    }

    /**
     * Compares a boundary point of this range with a boundary point of another range.
     * <p>
     * 이 범위의 경계점을 다른 범위의 경계점과 비교한다.
     *
     * @param how   {@link #START_TO_START}, {@link #START_TO_END}, {@link #END_TO_END} or {@link #END_TO_START} / 비교 방식
     * @param other the other range / 다른 범위
     * @return -1, 0 or 1 when the point of this range is before, equal to or after the point of the other range / 이 범위의 점이 앞, 같음, 뒤일 때 -1, 0, 1
     * @throws DomException NotSupportedError for another mode, WrongDocumentError if the ranges are in different trees / 다른 방식이면
     *                      NotSupportedError, 범위가 다른 트리에 있으면 WrongDocumentError
     */
    public short compareBoundaryPoints(short how, Range other) {
        if (how < START_TO_START || how > END_TO_START) {
            throw DomException.notSupported("the comparison mode " + how + " is not supported");
        }
        if (startContainer.getRootNode() != other.startContainer.getRootNode()) {
            throw new DomException("WrongDocumentError", DomException.WRONG_DOCUMENT_ERR, "the ranges are not in the same tree");
        }
        Node thisNode;
        int thisOffset;
        Node otherNode;
        int otherOffset;
        // 방식에 따라 이 범위와 다른 범위에서 비교할 경계점을 고른다.
        if (how == START_TO_START) {
            thisNode = startContainer;
            thisOffset = startOffset;
            otherNode = other.startContainer;
            otherOffset = other.startOffset;
        } else if (how == START_TO_END) {
            thisNode = endContainer;
            thisOffset = endOffset;
            otherNode = other.startContainer;
            otherOffset = other.startOffset;
        } else if (how == END_TO_END) {
            thisNode = endContainer;
            thisOffset = endOffset;
            otherNode = other.endContainer;
            otherOffset = other.endOffset;
        } else {
            thisNode = startContainer;
            thisOffset = startOffset;
            otherNode = other.endContainer;
            otherOffset = other.endOffset;
        }
        return (short) comparePoints(thisNode, thisOffset, otherNode, otherOffset);
    }

    /**
     * Compares a point with the range: before the start, inside, or after the end.
     * <p>
     * 점을 범위와 비교한다: 시작 앞, 안, 끝 뒤.
     *
     * @param node   the node of the point / 점의 노드
     * @param offset the offset of the point / 점의 오프셋
     * @return -1 if before the range, 0 if in it, 1 if after it / 범위 앞이면 -1, 안이면 0, 뒤이면 1
     * @throws DomException WrongDocumentError, InvalidNodeTypeError, IndexSizeError / WrongDocumentError, InvalidNodeTypeError, IndexSizeError
     */
    public short comparePoint(Node node, int offset) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.getRootNode() != startContainer.getRootNode()) {
            throw new DomException("WrongDocumentError", DomException.WRONG_DOCUMENT_ERR, "the node is not in the tree of the range");
        }
        checkBoundary(node, offset);
        if (comparePoints(node, offset, startContainer, startOffset) < 0) {
            return -1;
        }
        if (comparePoints(node, offset, endContainer, endOffset) > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Tells whether a point is inside the range (a point in another tree is outside).
     * <p>
     * 점이 범위 안에 있는지 알려 준다(다른 트리의 점은 밖이다).
     *
     * @param node   the node of the point / 점의 노드
     * @param offset the offset of the point / 점의 오프셋
     * @return true if inside / 안이면 true
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    public boolean isPointInRange(Node node, int offset) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.getRootNode() != startContainer.getRootNode()) {
            return false;
        }
        checkBoundary(node, offset);
        return comparePoints(node, offset, startContainer, startOffset) >= 0 && comparePoints(node, offset, endContainer, endOffset) <= 0;
    }

    /**
     * Tells whether a node touches the range (any part of it is inside the range).
     * <p>
     * 노드가 범위와 닿는지(노드의 어느 부분이라도 범위 안에 있는지) 알려 준다.
     *
     * @param node the node / 노드
     * @return true if the node and the range overlap / 노드와 범위가 겹치면 true
     */
    public boolean intersectsNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.getRootNode() != startContainer.getRootNode()) {
            return false;
        }
        Node parent = node.parent;
        if (parent == null) {
            return true;
        }
        int offset = indexOf(node);
        return comparePoints(parent, offset, endContainer, endOffset) < 0 && comparePoints(parent, offset + 1, startContainer, startOffset) > 0;
    }

    /**
     * Returns a new range with the same boundary points.
     * <p>
     * 경계점이 같은 새 범위를 반환한다.
     *
     * @return the copy / 복사본
     */
    public Range cloneRange() {
        Range r = new Range(document, true);
        r.startContainer = startContainer;
        r.startOffset = startOffset;
        r.endContainer = endContainer;
        r.endOffset = endOffset;
        return r;
    }

    /**
     * Does nothing (the DOM Standard keeps the method for compatibility only).
     * <p>
     * 아무것도 하지 않는다(DOM 표준은 호환성을 위해서만 이 메서드를 남겨 두었다).
     */
    public void detach() {
        return;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 내용 연산
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether a node is contained in the range (it lies completely between the two boundary points).
     * <p>
     * 노드가 범위에 포함되는지(두 경계점 사이에 완전히 놓이는지) 알려 준다.
     *
     * @param node the node / 노드
     * @return true if contained / 포함되면 true
     */
    private boolean isContained(Node node) {
        return node.getRootNode() == startContainer.getRootNode() && comparePoints(node, 0, startContainer, startOffset) > 0
                && comparePoints(node, length(node), endContainer, endOffset) < 0;
    }

    /**
     * Tells whether a node is partially contained: it is an inclusive ancestor of exactly one of the two boundary containers.
     * <p>
     * 노드가 부분적으로 포함되는지 알려 준다: 두 경계 컨테이너 중 정확히 하나의 포괄 조상이다.
     *
     * @param node the node / 노드
     * @return true if partially contained / 부분적으로 포함되면 true
     */
    private boolean isPartiallyContained(Node node) {
        return node.contains(startContainer) != node.contains(endContainer);
    }

    /**
     * Tells whether a node is text-like for the purposes of boundary offsets (text, CDATA section, comment, processing instruction).
     * <p>
     * 경계 오프셋에서 노드가 텍스트 계열인지 알려 준다(텍스트, CDATA 섹션, 주석, 처리 명령).
     *
     * @param node the node / 노드
     * @return true for character data / 문자 데이터이면 true
     */
    private static boolean isData(Node node) {
        return node instanceof CharacterData;
    }

    /**
     * Returns the ranges' contents as a document fragment without changing the tree (copies of the content).
     * <p>
     * 트리를 바꾸지 않고 범위의 내용을 문서 조각으로 반환한다(내용의 복사본).
     *
     * @return the fragment / 조각
     * @throws DomException HierarchyRequestError if a document type is inside the range / 문서 유형이 범위 안에 있으면 HierarchyRequestError
     */
    public DocumentFragment cloneContents() {
        return copyContents(false);
    }

    /**
     * Moves the content of the range out of the tree into a document fragment and collapses the range to where the content was.
     * <p>
     * 범위의 내용을 트리에서 꺼내 문서 조각으로 옮기고 범위를 내용이 있던 자리로 접는다.
     *
     * @return the fragment / 조각
     * @throws DomException HierarchyRequestError if a document type is inside the range / 문서 유형이 범위 안에 있으면 HierarchyRequestError
     */
    public DocumentFragment extractContents() {
        return copyContents(true);
    }

    /**
     * The "clone the contents" and "extract" algorithms of the DOM Standard (they differ in few places).
     * <p>
     * DOM 표준의 "clone the contents" 와 "extract" 알고리즘(다른 곳은 몇 군데뿐이다).
     *
     * @param extract true to remove the content from the tree / 트리에서 내용을 제거하려면 true
     * @return the fragment / 조각
     */
    private DocumentFragment copyContents(boolean extract) {
        DocumentFragment fragment = startContainer.document().createDocumentFragment();
        if (isCollapsed()) {
            return fragment;
        }
        Node originalStartNode = startContainer;
        int originalStartOffset = startOffset;
        Node originalEndNode = endContainer;
        int originalEndOffset = endOffset;
        // 시작과 끝이 같은 문자 데이터 노드 안이면 그 일부만 복사(이동)한다.
        if (originalStartNode == originalEndNode && isData(originalStartNode)) {
            CharacterData data = (CharacterData) originalStartNode;
            CharacterData clone = (CharacterData) data.cloneNode(false);
            clone.setData(data.substringData(originalStartOffset, originalEndOffset - originalStartOffset));
            fragment.appendChild(clone);
            if (extract) {
                data.replaceData(originalStartOffset, originalEndOffset - originalStartOffset, "");
            }
            return fragment;
        }
        Node commonAncestor = originalStartNode;
        // 끝 노드를 포함하는 첫 조상을 찾는다.
        while (!commonAncestor.contains(originalEndNode)) {
            commonAncestor = commonAncestor.parent;
        }
        Node firstPartial = null;
        if (!originalStartNode.contains(originalEndNode)) {
            // 공통 조상의 자식 중 범위에 부분적으로 포함되는 첫 자식.
            for (Node c = commonAncestor.firstChild; c != null; c = c.nextSibling) {
                if (isPartiallyContained(c)) {
                    firstPartial = c;
                    break;
                }
            }
        }
        Node lastPartial = null;
        if (!originalEndNode.contains(originalStartNode)) {
            // 공통 조상의 자식 중 범위에 부분적으로 포함되는 마지막 자식.
            for (Node c = commonAncestor.lastChild; c != null; c = c.prevSibling) {
                if (isPartiallyContained(c)) {
                    lastPartial = c;
                    break;
                }
            }
        }
        List<Node> contained = new ArrayList<Node>();
        // 공통 조상의 자식 중 범위에 완전히 포함되는 것을 순서대로 모은다.
        for (Node c = commonAncestor.firstChild; c != null; c = c.nextSibling) {
            if (isContained(c)) {
                if (c.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
                    throw DomException.hierarchyRequest("the range contains a document type");
                }
                contained.add(c);
            }
        }
        Node newNode;
        int newOffset;
        if (originalStartNode.contains(originalEndNode)) {
            newNode = originalStartNode;
            newOffset = originalStartOffset;
        } else {
            Node reference = originalStartNode;
            // 부모가 끝 노드를 포함하게 되는 곳까지 시작 노드에서 올라간다.
            while (!reference.parent.contains(originalEndNode)) {
                reference = reference.parent;
            }
            newNode = reference.parent;
            newOffset = indexOf(reference) + 1;
        }
        if (firstPartial != null) {
            if (isData(firstPartial)) {
                CharacterData data = (CharacterData) firstPartial;
                CharacterData clone = (CharacterData) data.cloneNode(false);
                clone.setData(data.substringData(originalStartOffset, data.getLength() - originalStartOffset));
                fragment.appendChild(clone);
                if (extract) {
                    data.replaceData(originalStartOffset, data.getLength() - originalStartOffset, "");
                }
            } else {
                Node clone = firstPartial.cloneNode(false);
                fragment.appendChild(clone);
                Range sub = new Range(document, false);
                sub.startContainer = originalStartNode;
                sub.startOffset = originalStartOffset;
                sub.endContainer = firstPartial;
                sub.endOffset = length(firstPartial);
                clone.appendChild(sub.copyContents(extract));
            }
        }
        // 완전히 포함된 자식은 복사하거나(extract 이면 그대로 옮겨) 조각에 붙인다.
        for (int i = 0; i < contained.size(); i++) {
            Node child = contained.get(i);
            fragment.appendChild(extract ? child : child.cloneNode(true));
        }
        if (lastPartial != null) {
            if (isData(lastPartial)) {
                CharacterData data = (CharacterData) lastPartial;
                CharacterData clone = (CharacterData) data.cloneNode(false);
                clone.setData(data.substringData(0, originalEndOffset));
                fragment.appendChild(clone);
                if (extract) {
                    data.replaceData(0, originalEndOffset, "");
                }
            } else {
                Node clone = lastPartial.cloneNode(false);
                fragment.appendChild(clone);
                Range sub = new Range(document, false);
                sub.startContainer = lastPartial;
                sub.startOffset = 0;
                sub.endContainer = originalEndNode;
                sub.endOffset = originalEndOffset;
                clone.appendChild(sub.copyContents(extract));
            }
        }
        if (extract) {
            startContainer = newNode;
            startOffset = newOffset;
            endContainer = newNode;
            endOffset = newOffset;
            touched();
        }
        return fragment;
    }

    /**
     * Removes the content of the range from the tree and collapses the range to where the content was.
     * <p>
     * 범위의 내용을 트리에서 지우고 범위를 내용이 있던 자리로 접는다.
     */
    public void deleteContents() {
        if (isCollapsed()) {
            return;
        }
        Node originalStartNode = startContainer;
        int originalStartOffset = startOffset;
        Node originalEndNode = endContainer;
        int originalEndOffset = endOffset;
        // 시작과 끝이 같은 문자 데이터 노드 안이면 그 일부만 지운다.
        if (originalStartNode == originalEndNode && isData(originalStartNode)) {
            ((CharacterData) originalStartNode).replaceData(originalStartOffset, originalEndOffset - originalStartOffset, "");
            return;
        }
        List<Node> toRemove = new ArrayList<Node>();
        Node commonAncestor = getCommonAncestorContainer();
        Node n = commonAncestor;
        // 트리 순서로 범위에 완전히 포함된 노드를 모은다. 포함된 노드의 후손은 그 노드와 함께 지워지므로 하위 트리는 건너뛴다.
        while (n != null) {
            if (isContained(n)) {
                toRemove.add(n);
                n = nextSkippingChildren(n, commonAncestor);
            } else {
                n = Node.nextInTree(n, commonAncestor);
            }
        }
        Node newNode;
        int newOffset;
        if (originalStartNode.contains(originalEndNode)) {
            newNode = originalStartNode;
            newOffset = originalStartOffset;
        } else {
            Node reference = originalStartNode;
            // 부모가 끝 노드를 포함하게 되는 곳까지 시작 노드에서 올라간다.
            while (!reference.parent.contains(originalEndNode)) {
                reference = reference.parent;
            }
            newNode = reference.parent;
            newOffset = indexOf(reference) + 1;
        }
        if (isData(originalStartNode)) {
            CharacterData data = (CharacterData) originalStartNode;
            data.replaceData(originalStartOffset, data.getLength() - originalStartOffset, "");
        }
        // 포함된 노드를 차례로 트리에서 뗀다.
        for (int i = 0; i < toRemove.size(); i++) {
            Node r = toRemove.get(i);
            if (r.parent != null) {
                r.parent.removeChild(r);
            }
        }
        if (isData(originalEndNode)) {
            ((CharacterData) originalEndNode).replaceData(0, originalEndOffset, "");
        }
        startContainer = newNode;
        startOffset = newOffset;
        endContainer = newNode;
        endOffset = newOffset;
        touched();
    }

    /**
     * Returns the node that follows a node and all its descendants in tree order inside a root.
     * <p>
     * 루트 안에서 트리 순서로 노드와 그 모든 후손 다음에 오는 노드를 반환한다.
     *
     * @param node the node / 노드
     * @param root the root / 루트
     * @return the next node or null / 다음 노드 또는 null
     */
    private static Node nextSkippingChildren(Node node, Node root) {
        Node c = node;
        // 다음 형제가 있는 조상(자신 포함)을 찾을 때까지 올라간다.
        while (c != null && c != root) {
            if (c.nextSibling != null) {
                return c.nextSibling;
            }
            c = c.parent;
        }
        return null;
    }

    /**
     * Inserts a node at the start boundary point of the range (a text node there is split) and, for a collapsed range, extends the range
     * to cover the new node.
     * <p>
     * 노드를 범위의 시작 경계점에 삽입한다(그곳의 텍스트 노드는 쪼갠다). 접힌 범위이면 범위가 새 노드를 덮도록 늘린다.
     *
     * @param node the node (a document fragment inserts its children) / 노드(문서 조각이면 그 자식을 삽입한다)
     * @throws DomException HierarchyRequestError and the other errors of node insertion / HierarchyRequestError 와 노드 삽입의 다른 오류
     */
    public void insertNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        short type = startContainer.getNodeType();
        boolean startIsText = startContainer instanceof Text;
        if (type == Node.PROCESSING_INSTRUCTION_NODE || type == Node.COMMENT_NODE || (startIsText && startContainer.parent == null)
                || node == startContainer) {
            throw DomException.hierarchyRequest("the node cannot be inserted at the start of the range");
        }
        Node referenceNode;
        if (startIsText) {
            referenceNode = startContainer;
        } else {
            referenceNode = childAt(startContainer, startOffset);
        }
        Node parent = referenceNode == null ? startContainer : referenceNode.parent;
        if (parent == null) {
            throw DomException.hierarchyRequest("the start of the range has no parent for the new node");
        }
        // 표준의 순서대로 삽입이 가능한지 먼저 확인한다(실패하면 시작 지점의 텍스트를 쪼개지 않는다).
        parent.ensurePreInsertionValidity(node, referenceNode);
        if (startIsText) {
            referenceNode = ((Text) startContainer).splitText(startOffset);
        }
        if (node == referenceNode) {
            referenceNode = referenceNode.nextSibling;
        }
        if (node.parent != null) {
            node.parent.removeChild(node);
        }
        int newOffset = referenceNode == null ? length(parent) : indexOf(referenceNode);
        newOffset += node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE ? length(node) : 1;
        parent.insertBefore(node, referenceNode);
        if (isCollapsed()) {
            endContainer = parent;
            endOffset = newOffset;
            touched();
        }
    }

    /**
     * Wraps the content of the range in a new parent: the content is extracted, the (emptied) new parent is inserted at the range, the
     * content becomes its children, and the range selects the new parent.
     * <p>
     * 범위의 내용을 새 부모로 감싼다: 내용을 꺼내고, (비운) 새 부모를 범위에 삽입하고, 내용을 그 자식으로 만들고, 범위가 새 부모를 선택한다.
     *
     * @param newParent the new parent / 새 부모
     * @throws DomException InvalidStateError if a non-text node is partially contained, InvalidNodeTypeError for a document, document type
     *                      or fragment / 텍스트가 아닌 노드가 부분적으로 포함되면 InvalidStateError, 문서/문서 유형/조각이면 InvalidNodeTypeError
     */
    public void surroundContents(Node newParent) {
        if (newParent == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        // 텍스트가 아닌 노드가 부분적으로 포함되면(경계 컨테이너의 조상 중 텍스트 아닌 것) 감쌀 수 없다.
        if (hasPartialNonText(startContainer, endContainer) || hasPartialNonText(endContainer, startContainer)) {
            throw DomException.invalidState("a node that is not text is only partially contained in the range");
        }
        short t = newParent.getNodeType();
        if (t == Node.DOCUMENT_NODE || t == Node.DOCUMENT_TYPE_NODE || t == Node.DOCUMENT_FRAGMENT_NODE) {
            throw new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "the new parent cannot be a document, document type or fragment");
        }
        DocumentFragment fragment = extractContents();
        // 새 부모의 기존 자식은 지운다.
        while (newParent.firstChild != null) {
            newParent.removeChild(newParent.firstChild);
        }
        insertNode(newParent);
        newParent.appendChild(fragment);
        selectNode(newParent);
    }

    /**
     * Tells whether an inclusive ancestor of one container that does not contain the other container is a node that is not text.
     * <p>
     * 한 컨테이너의 포괄 조상 중 다른 컨테이너를 포함하지 않는 것에 텍스트가 아닌 노드가 있는지 알려 준다.
     *
     * @param container the container to start from / 시작할 컨테이너
     * @param other     the other container / 다른 컨테이너
     * @return true if such a node exists / 그런 노드가 있으면 true
     */
    private static boolean hasPartialNonText(Node container, Node other) {
        // 컨테이너에서 위로 올라가며 다른 컨테이너를 포함하지 않는 조상을 본다.
        for (Node n = container; n != null && !n.contains(other); n = n.parent) {
            if (n.getNodeType() != Node.TEXT_NODE && n.getNodeType() != Node.CDATA_SECTION_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the text of the range: the character data of the text nodes (not comments) that the range covers, cut at the boundary points.
     * <p>
     * 범위의 텍스트를 반환한다: 범위가 덮는 텍스트 노드(주석은 아니다)의 문자 데이터를 경계점에서 잘라 이은 것.
     *
     * @return the text / 텍스트
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean startText = startContainer.getNodeType() == Node.TEXT_NODE || startContainer.getNodeType() == Node.CDATA_SECTION_NODE;
        boolean endText = endContainer.getNodeType() == Node.TEXT_NODE || endContainer.getNodeType() == Node.CDATA_SECTION_NODE;
        if (startContainer == endContainer && startText) {
            return ((CharacterData) startContainer).substringData(startOffset, endOffset - startOffset);
        }
        if (startText) {
            CharacterData d = (CharacterData) startContainer;
            sb.append(d.substringData(startOffset, d.getLength() - startOffset));
        }
        Node root = getCommonAncestorContainer();
        Node n = root;
        // 공통 조상 아래를 트리 순서로 훑으며 범위에 완전히 포함된 텍스트 노드의 데이터를 이어 붙인다.
        while (n != null) {
            if ((n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) && isContained(n)) {
                sb.append(((CharacterData) n).getData());
            }
            n = Node.nextInTree(n, root);
        }
        if (endText) {
            sb.append(((CharacterData) endContainer).substringData(0, endOffset));
        }
        return sb.toString();
    }

    /**
     * Parses a piece of markup in the context of the start of the range and returns it as a document fragment (DOM Parsing and
     * Serialization, <code>createContextualFragment</code>). The context is the start container if it is an element, the parent element
     * of a text-like start container, and a new <code>body</code> element if there is no usable element or the context is the root
     * <code>html</code> element of an HTML document.
     * <p>
     * 범위 시작의 문맥에서 마크업 조각을 해석해 문서 조각으로 반환한다(DOM Parsing and Serialization 의 <code>createContextualFragment</code>).
     * 문맥은 시작 컨테이너가 요소이면 그 요소, 텍스트 계열이면 부모 요소이며, 쓸 수 있는 요소가 없거나 HTML 문서의 루트 <code>html</code>
     * 요소이면 새 <code>body</code> 요소이다.
     *
     * @param markup the markup / 마크업
     * @return the fragment / 조각
     */
    public DocumentFragment createContextualFragment(String markup) {
        Node node = startContainer;
        Element element;
        if (node instanceof Element) {
            element = (Element) node;
        } else if (node instanceof CharacterData) {
            element = node.parent instanceof Element ? (Element) node.parent : null;
        } else {
            element = null;
        }
        Document doc = startContainer.document();
        // 요소가 없거나 HTML 문서의 루트 html 요소이면 body 를 문맥으로 쓴다.
        if (element == null || (doc.isHtmlDocument() && "html".equals(element.getLocalName()) && element.parent == doc)) {
            element = doc.createElement("body");
        }
        return doc.requireMarkupSupport().parseFragment(element, markup == null ? "" : markup);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 살아 있는 범위: 트리 변경에 따른 경계점 조정
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Adjusts the boundary points after children were removed from a parent and nodes were inserted in their place: the points inside a
     * removed node move to the place of the removal, the points after it move up by the number of removed nodes (but not before the place of
     * the removal), and the points after the place of an insertion move down by the number of inserted nodes. The removal happens first.
     * <p>
     * 부모에서 자식이 제거되고 그 자리에 노드가 삽입된 뒤 경계점을 조정한다: 제거된 노드 안의 점은 제거된 자리로 옮기고, 그 뒤의 점은 제거된 노드
     * 수만큼 앞으로 당기며(제거된 자리보다 앞으로는 가지 않는다), 삽입된 자리 뒤의 점은 삽입된 노드 수만큼 뒤로 민다. 제거가 먼저 일어난다.
     *
     * @param parent       the parent that changed / 바뀐 부모
     * @param removed      the removed nodes (in order, they stood at the index) / 제거된 노드(순서대로, 인덱스 자리에 있었다)
     * @param index        the index of the first removed or inserted node / 처음 제거되거나 삽입된 노드의 인덱스
     * @param insertedSize the number of inserted nodes / 삽입된 노드 수
     */
    void childListChanged(Node parent, List<Node> removed, int index, int insertedSize) {
        Node oldStart = startContainer;
        int oldStartOffset = startOffset;
        Node oldEnd = endContainer;
        int oldEndOffset = endOffset;
        // 시작과 끝 경계점을 같은 규칙으로 조정한다.
        for (int side = 0; side < 2; side++) {
            Node node = side == 0 ? startContainer : endContainer;
            int offset = side == 0 ? startOffset : endOffset;
            boolean moved = false;
            // 제거된 노드의 안에 있으면 제거된 자리로 옮긴다.
            for (int i = 0; i < removed.size() && !moved; i++) {
                if (removed.get(i).contains(node)) {
                    node = parent;
                    offset = index;
                    moved = true;
                }
            }
            if (!moved && node == parent && offset > index) {
                offset = Math.max(index, offset - removed.size());
            }
            // 삽입된 자리 뒤의 점은 삽입된 노드 수만큼 민다.
            if (node == parent && offset > index && insertedSize > 0) {
                offset += insertedSize;
            }
            if (side == 0) {
                startContainer = node;
                startOffset = offset;
            } else {
                endContainer = node;
                endOffset = offset;
            }
        }
        // 경계점이 실제로 움직였을 때만 선택에 알린다.
        if (oldStart != startContainer || oldStartOffset != startOffset || oldEnd != endContainer || oldEndOffset != endOffset) {
            touched();
        }
    }

    /**
     * Adjusts the boundary points after character data was replaced: a point inside the replaced part moves to the start of it, a point
     * after it moves by the difference of the lengths.
     * <p>
     * 문자 데이터가 교체된 뒤 경계점을 조정한다: 교체된 부분 안의 점은 그 시작으로 옮기고 그 뒤의 점은 길이 차이만큼 옮긴다.
     *
     * @param node       the node whose data was replaced / 데이터가 교체된 노드
     * @param offset     where the replacement started / 교체가 시작된 위치
     * @param count      the number of replaced characters / 교체된 문자 수
     * @param dataLength the number of characters that took their place / 대신 들어간 문자 수
     */
    void dataReplaced(Node node, int offset, int count, int dataLength) {
        int oldStartOffset = startOffset;
        int oldEndOffset = endOffset;
        if (startContainer == node) {
            startOffset = adjusted(startOffset, offset, count, dataLength);
        }
        if (endContainer == node) {
            endOffset = adjusted(endOffset, offset, count, dataLength);
        }
        // 경계점의 오프셋이 실제로 바뀌었을 때만 선택에 알린다.
        if (oldStartOffset != startOffset || oldEndOffset != endOffset) {
            touched();
        }
    }

    /**
     * Applies the "replace data" rule to one offset.
     * <p>
     * 오프셋 하나에 "replace data" 규칙을 적용한다.
     *
     * @param value      the offset of the point / 점의 오프셋
     * @param offset     where the replacement started / 교체가 시작된 위치
     * @param count      the number of replaced characters / 교체된 문자 수
     * @param dataLength the number of characters that took their place / 대신 들어간 문자 수
     * @return the new offset / 새 오프셋
     */
    private static int adjusted(int value, int offset, int count, int dataLength) {
        if (value > offset && value <= offset + count) {
            return offset;
        }
        if (value > offset + count) {
            return value + dataLength - count;
        }
        return value;
    }

    /**
     * Adjusts the boundary points after a text node was split: the points of the original node after the split offset move to the new
     * node, and the points just after the original node in its parent move after the new node as well.
     * <p>
     * 텍스트 노드가 쪼개진 뒤 경계점을 조정한다: 원래 노드에서 쪼갠 위치 뒤의 점은 새 노드로 옮기고, 부모 안에서 원래 노드 바로 뒤의 점은 새 노드
     * 뒤로 옮긴다.
     *
     * @param node    the original text node / 원래 텍스트 노드
     * @param created the new text node (already inserted after the original) / 새 텍스트 노드(원래 노드 뒤에 이미 삽입되었다)
     * @param offset  the split offset / 쪼갠 위치
     */
    void textSplit(Node node, Node created, int offset) {
        Node parent = node.parent;
        Node oldStart = startContainer;
        int oldStartOffset = startOffset;
        Node oldEnd = endContainer;
        int oldEndOffset = endOffset;
        if (startContainer == node && startOffset > offset) {
            startContainer = created;
            startOffset -= offset;
        } else if (parent != null && startContainer == parent && startOffset == indexOf(node) + 1) {
            startOffset++;
        }
        if (endContainer == node && endOffset > offset) {
            endContainer = created;
            endOffset -= offset;
        } else if (parent != null && endContainer == parent && endOffset == indexOf(node) + 1) {
            endOffset++;
        }
        // 경계점이 실제로 움직였을 때만 선택에 알린다.
        if (oldStart != startContainer || oldStartOffset != startOffset || oldEnd != endContainer || oldEndOffset != endOffset) {
            touched();
        }
    }

    /**
     * Adjusts the boundary points when {@link Node#normalize()} merges a text node into the text node before it: a point in the merged node
     * moves into the target after the characters that were there (offset plus the length so far), and a point of the parent that stood right
     * before the merged node moves to the end of the part of the target that was there (DOM Standard, "normalize", steps 6.1 to 6.4).
     * <p>
     * {@link Node#normalize()} 가 텍스트 노드를 앞의 텍스트 노드에 합칠 때 경계점을 조정한다: 합쳐지는 노드 안의 점은 대상의 이미 있던 글자 뒤로 옮기고(오프셋에
     * 지금까지의 길이를 더한다), 합쳐지는 노드 바로 앞에 있던 부모의 점은 대상의 그 부분의 끝으로 옮긴다(DOM Standard "normalize" 6.1~6.4 단계).
     *
     * @param target  the text node that receives the data / 데이터를 받는 텍스트 노드
     * @param current the text node that is merged and removed / 합쳐지고 제거될 텍스트 노드
     * @param length  the length of the target before the data of the current node (the sum of the earlier parts) / 현재 노드의 데이터 앞의 대상 길이
     * @param index   the index of the current node in its parent / 현재 노드의 부모 안의 위치
     */
    void textMerged(Node target, Node current, int length, int index) {
        Node parent = current.parent;
        Node oldStart = startContainer;
        int oldStartOffset = startOffset;
        Node oldEnd = endContainer;
        int oldEndOffset = endOffset;
        if (startContainer == current) {
            startContainer = target;
            startOffset += length;
        } else if (parent != null && startContainer == parent && startOffset == index) {
            startContainer = target;
            startOffset = length;
        }
        if (endContainer == current) {
            endContainer = target;
            endOffset += length;
        } else if (parent != null && endContainer == parent && endOffset == index) {
            endContainer = target;
            endOffset = length;
        }
        // 경계점이 실제로 움직였을 때만 선택에 알린다.
        if (oldStart != startContainer || oldStartOffset != startOffset || oldEnd != endContainer || oldEndOffset != endOffset) {
            touched();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 선택과의 연결
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Ties the range to the selection that uses it, so that the selection hears about every move of a boundary point (only the range of
     * the selection is tied; ranges that scripts create themselves are not).
     * <p>
     * 범위를 그것을 쓰는 선택에 연결해, 선택이 경계점의 모든 이동을 듣게 한다(선택의 범위만 연결되며 스크립트가 직접 만든 범위는 연결되지 않는다).
     *
     * @param owner the selection / 선택
     */
    void tie(Selection owner) {
        this.selection = owner;
    }

    /**
     * Cuts the connection to a selection (the selection chose another range).
     * <p>
     * 선택과의 연결을 끊는다(선택이 다른 범위를 골랐다).
     *
     * @param owner the selection that lets go of the range / 범위를 놓는 선택
     */
    void untie(Selection owner) {
        if (selection == owner) {
            selection = null;
        }
    }

    /**
     * Tells the selection that a boundary point of this range moved (nothing happens for a range that no selection uses).
     * <p>
     * 이 범위의 경계점이 움직였음을 선택에 알린다(어떤 선택도 쓰지 않는 범위이면 아무 일도 없다).
     */
    private void touched() {
        Selection s = selection;
        // 선택에 연결된 범위일 때만 알린다.
        if (s != null) {
            s.rangeChanged(this);
        }
    }

    /**
     * Compares two boundary points of a document: returns a negative number if the first is before the second, 0 if they are the same
     * point and a positive number if it is after.
     * <p>
     * 문서의 두 경계점을 비교한다: 첫째가 둘째보다 앞이면 음수, 같은 점이면 0, 뒤이면 양수를 반환한다.
     *
     * @param nodeA   the node of the first point / 첫째 점의 노드
     * @param offsetA the offset of the first point / 첫째 점의 오프셋
     * @param nodeB   the node of the second point / 둘째 점의 노드
     * @param offsetB the offset of the second point / 둘째 점의 오프셋
     * @return the comparison / 비교 결과
     */
    public static int comparePointOrder(Node nodeA, int offsetA, Node nodeB, int offsetB) {
        return comparePoints(nodeA, offsetA, nodeB, offsetB);
    }
}
