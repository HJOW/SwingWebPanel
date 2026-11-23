package org.duckdns.hjow.swingwebpanel.dom;

import java.util.List;

/**
 * A flat, live iterator over the nodes of a subtree in tree order (DOM Standard, "Interface NodeIterator"). It sits at a reference node,
 * before or after it; {@link #nextNode()} and {@link #previousNode()} move over the next node that the {@link NodeFilter} and
 * <code>whatToShow</code> accept. It is live: when a node that holds the reference node is removed from the tree, the iterator moves its
 * reference so that it stays valid (the "NodeIterator pre-removing steps"). One restriction against the Standard: when the removed node
 * contains the root of the iterator itself, nothing moves.
 * <p>
 * 하위 트리의 노드를 트리 순서로 훑는 평평하고 살아 있는 반복자(DOM 표준 "Interface NodeIterator"). 기준 노드의 앞이나 뒤에 서 있고
 * {@link #nextNode()}, {@link #previousNode()} 는 {@link NodeFilter} 와 <code>whatToShow</code> 가 받아들이는 다음 노드를 넘어간다. 살아 있다:
 * 기준 노드를 가진 노드가 트리에서 제거되면 반복자가 유효하게 남도록 기준을 옮긴다("NodeIterator pre-removing steps"). 표준과 다른 제한 하나: 제거되는
 * 노드가 반복자의 루트 자신을 포함하면 아무것도 옮기지 않는다.
 */
public final class NodeIterator extends Traversal {

    /**
     * The node that the iterator is at.
     * <p>
     * 반복자가 서 있는 노드.
     */
    private Node reference;

    /**
     * True if the iterator is before the reference node, false if it is after it.
     * <p>
     * 반복자가 기준 노드 앞에 있으면 true, 뒤에 있으면 false.
     */
    private boolean pointerBeforeReference = true;

    /**
     * Creates an iterator at the start of the root (before the root), and registers it with the document so that it follows removals.
     * <p>
     * 루트의 시작(루트 앞)에 반복자를 만들고, 제거를 따라가도록 문서에 등록한다.
     *
     * @param root       the root node / 루트 노드
     * @param whatToShow the bit mask of the node kinds to show / 보여 줄 노드 종류의 비트 마스크
     * @param filter     the filter or null / 필터 또는 null
     */
    NodeIterator(Node root, long whatToShow, NodeFilter filter) {
        super(root, whatToShow, filter);
        this.reference = root;
        root.document().liveRegistry().add(this);
    }

    /**
     * Returns the node that the iterator is at.
     * <p>
     * 반복자가 서 있는 노드를 반환한다.
     *
     * @return the reference node / 기준 노드
     */
    public Node getReferenceNode() {
        return reference;
    }

    /**
     * Tells whether the iterator is before its reference node.
     * <p>
     * 반복자가 기준 노드 앞에 있는지 알려 준다.
     *
     * @return true if before the reference node / 기준 노드 앞이면 true
     */
    public boolean isPointerBeforeReferenceNode() {
        return pointerBeforeReference;
    }

    /**
     * Moves to the next accepted node and returns it.
     * <p>
     * 받아들여지는 다음 노드로 옮겨 그 노드를 반환한다.
     *
     * @return the node, or null at the end / 노드. 끝이면 null
     * @throws DomException InvalidStateError if the filter is running / 필터가 실행 중이면 InvalidStateError
     */
    public Node nextNode() {
        return traverse(true);
    }

    /**
     * Moves to the previous accepted node and returns it.
     * <p>
     * 받아들여지는 이전 노드로 옮겨 그 노드를 반환한다.
     *
     * @return the node, or null at the start / 노드. 처음이면 null
     * @throws DomException InvalidStateError if the filter is running / 필터가 실행 중이면 InvalidStateError
     */
    public Node previousNode() {
        return traverse(false);
    }

    /**
     * Does nothing (the DOM Standard keeps the method for compatibility only).
     * <p>
     * 아무것도 하지 않는다(DOM 표준은 호환성을 위해서만 이 메서드를 남겨 두었다).
     */
    public void detach() {
        return;
    }

    /**
     * The "traverse" algorithm of the DOM Standard: the filter is asked about every node on the way, and the iterator stays where the last
     * accepted node is.
     * <p>
     * DOM 표준의 "traverse" 알고리즘: 지나는 모든 노드를 필터에 묻고, 반복자는 마지막으로 받아들인 노드에 머문다.
     *
     * @param forward true for {@link #nextNode()} / {@link #nextNode()} 이면 true
     * @return the accepted node or null / 받아들인 노드. 없으면 null
     */
    private Node traverse(boolean forward) {
        Node node = reference;
        boolean before = pointerBeforeReference;
        short result;
        // 받아들여지는 노드를 만날 때까지 한 노드씩 옮기며 필터에 묻는다.
        do {
            if (forward) {
                if (!before) {
                    node = nextInTree(node);
                    if (node == null) {
                        return null;
                    }
                } else {
                    before = false;
                }
            } else {
                if (before) {
                    node = previousInTree(node);
                    if (node == null) {
                        return null;
                    }
                } else {
                    before = true;
                }
            }
            result = filterNode(node);
        } while (result != NodeFilter.FILTER_ACCEPT);
        reference = node;
        pointerBeforeReference = before;
        return node;
    }

    /**
     * Returns the node that follows a node in tree order inside the root.
     * <p>
     * 루트 안에서 트리 순서로 노드의 다음 노드를 반환한다.
     *
     * @param node the node / 노드
     * @return the following node or null / 다음 노드. 없으면 null
     */
    private Node nextInTree(Node node) {
        return Node.nextInTree(node, getRoot());
    }

    /**
     * Returns the node that precedes a node in tree order inside the root: the last descendant of the previous sibling, else the parent.
     * <p>
     * 루트 안에서 트리 순서로 노드의 이전 노드를 반환한다: 이전 형제의 마지막 후손, 없으면 부모.
     *
     * @param node the node / 노드
     * @return the preceding node or null / 이전 노드. 없으면 null
     */
    private Node previousInTree(Node node) {
        if (node == getRoot()) {
            return null;
        }
        if (node.prevSibling != null) {
            return lastInclusiveDescendant(node.prevSibling);
        }
        return node.parent;
    }

    /**
     * Returns the last node of a subtree in tree order.
     * <p>
     * 하위 트리의 트리 순서로 마지막 노드를 반환한다.
     *
     * @param node the root of the subtree / 하위 트리의 루트
     * @return the last inclusive descendant / 마지막 포괄 후손
     */
    static Node lastInclusiveDescendant(Node node) {
        Node n = node;
        // 마지막 자식을 따라 내려가 더 갈 곳이 없는 노드를 찾는다.
        while (n.lastChild != null) {
            n = n.lastChild;
        }
        return n;
    }

    /**
     * The "NodeIterator pre-removing steps" of the DOM Standard, applied after the removal: the nodes just left their parent, and the
     * tree around the place where they were is still intact. If the reference node is in one of them, the reference moves to the node
     * that follows the removed nodes inside the root (staying before it); when none follows, the iterator goes to the end of the node
     * that precedes them.
     * <p>
     * DOM 표준의 "NodeIterator pre-removing steps" 를 제거 뒤에 적용한다: 노드들은 방금 부모를 떠났고 그들이 있던 자리 둘레의 트리는 그대로이다.
     * 기준 노드가 그 안에 있으면 기준은 루트 안에서 제거된 노드 다음에 오는 노드로 옮겨지고(그 앞에 머문다), 뒤따르는 노드가 없으면 반복자는 그
     * 앞의 노드의 끝으로 간다.
     *
     * @param parent  the parent that the nodes were removed from / 노드를 제거당한 부모
     * @param removed the removed nodes / 제거된 노드
     * @param prev    the sibling before the removed nodes, or null / 제거된 노드 앞의 형제. 없으면 null
     * @param next    the sibling after the removed nodes, or null / 제거된 노드 뒤의 형제. 없으면 null
     */
    void nodesRemoved(Node parent, List<Node> removed, Node prev, Node next) {
        Node removedHolder = null;
        // 기준 노드를 품은 제거된 노드가 있는지 찾는다. 루트를 품은 것이 있으면 아무것도 하지 않는다.
        for (int i = 0; i < removed.size(); i++) {
            Node r = removed.get(i);
            if (r.contains(getRoot())) {
                return;
            }
            if (r.contains(reference)) {
                removedHolder = r;
            }
        }
        if (removedHolder == null) {
            return;
        }
        if (pointerBeforeReference) {
            Node following = next;
            Node up = parent;
            // 다음 형제가 없으면 조상으로 올라가며 루트 안에서 뒤따르는 노드를 찾는다.
            while (following == null && up != null && up != getRoot()) {
                following = up.nextSibling;
                up = up.parent;
            }
            if (following != null) {
                reference = following;
                return;
            }
            pointerBeforeReference = false;
        }
        reference = prev == null ? parent : lastInclusiveDescendant(prev);
    }
}
