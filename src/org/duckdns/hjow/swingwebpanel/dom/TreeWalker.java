package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A walker over the nodes of a subtree that sees the tree structure (DOM Standard, "Interface TreeWalker"): besides the document order
 * traversal of {@link #nextNode()} and {@link #previousNode()} it moves to the parent, the first and last child and the siblings of its
 * current node, always over the nodes that the {@link NodeFilter} and <code>whatToShow</code> accept. A node that the filter rejects hides
 * its whole subtree from the walker, a skipped node only itself. The current node can be set to any node; it is not adjusted when nodes are
 * removed.
 * <p>
 * 하위 트리의 노드를 트리 구조를 보며 걷는 도구(DOM 표준 "Interface TreeWalker"): {@link #nextNode()}, {@link #previousNode()} 의 문서 순서 탐색 외에
 * 현재 노드의 부모, 첫째/마지막 자식, 형제로 옮겨 가며 언제나 {@link NodeFilter} 와 <code>whatToShow</code> 가 받아들이는 노드만 넘어간다. 필터가
 * 거부한 노드는 하위 트리 전체를, 건너뛴 노드는 자신만 걷는 도구에게서 감춘다. 현재 노드는 아무 노드로나 지정할 수 있으며 노드가 제거되어도 조정하지
 * 않는다.
 */
public final class TreeWalker extends Traversal {

    /**
     * The current node.
     * <p>
     * 현재 노드.
     */
    private Node current;

    /**
     * Creates a walker whose current node is the root.
     * <p>
     * 현재 노드가 루트인 걷는 도구를 만든다.
     *
     * @param root       the root node / 루트 노드
     * @param whatToShow the bit mask of the node kinds to show / 보여 줄 노드 종류의 비트 마스크
     * @param filter     the filter or null / 필터 또는 null
     */
    TreeWalker(Node root, long whatToShow, NodeFilter filter) {
        super(root, whatToShow, filter);
        this.current = root;
    }

    /**
     * Returns the current node.
     * <p>
     * 현재 노드를 반환한다.
     *
     * @return the current node / 현재 노드
     */
    public Node getCurrentNode() {
        return current;
    }

    /**
     * Sets the current node (any node is allowed, also outside the root).
     * <p>
     * 현재 노드를 지정한다(루트 밖의 노드를 포함해 아무 노드나 허용한다).
     *
     * @param node the new current node, not null / 새 현재 노드(null 불가)
     */
    public void setCurrentNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("the current node must not be null");
        }
        this.current = node;
    }

    /**
     * Moves to the closest ancestor (not above the root) that the walker accepts.
     * <p>
     * 걷는 도구가 받아들이는 가장 가까운 조상(루트 위로는 가지 않는다)으로 옮긴다.
     *
     * @return the node, or null if there is none (the current node stays) / 노드. 없으면 null(현재 노드는 그대로)
     */
    public Node parentNode() {
        Node node = current;
        // 루트에 닿을 때까지 부모를 따라 올라가며 받아들여지는 첫 노드를 찾는다.
        while (node != null && node != getRoot()) {
            node = node.parent;
            if (node != null && filterNode(node) == NodeFilter.FILTER_ACCEPT) {
                current = node;
                return node;
            }
        }
        return null;
    }

    /**
     * Moves to the first child that the walker accepts (looking into skipped children).
     * <p>
     * 걷는 도구가 받아들이는 첫 자식으로 옮긴다(건너뛴 자식의 안도 살펴본다).
     *
     * @return the node or null / 노드 또는 null
     */
    public Node firstChild() {
        return traverseChildren(true);
    }

    /**
     * Moves to the last child that the walker accepts (looking into skipped children).
     * <p>
     * 걷는 도구가 받아들이는 마지막 자식으로 옮긴다(건너뛴 자식의 안도 살펴본다).
     *
     * @return the node or null / 노드 또는 null
     */
    public Node lastChild() {
        return traverseChildren(false);
    }

    /**
     * Moves to the previous sibling that the walker accepts.
     * <p>
     * 걷는 도구가 받아들이는 이전 형제로 옮긴다.
     *
     * @return the node or null / 노드 또는 null
     */
    public Node previousSibling() {
        return traverseSiblings(false);
    }

    /**
     * Moves to the next sibling that the walker accepts.
     * <p>
     * 걷는 도구가 받아들이는 다음 형제로 옮긴다.
     *
     * @return the node or null / 노드 또는 null
     */
    public Node nextSibling() {
        return traverseSiblings(true);
    }

    /**
     * Moves to the previous accepted node in document order (not above the root).
     * <p>
     * 문서 순서로 받아들여지는 이전 노드로 옮긴다(루트 위로는 가지 않는다).
     *
     * @return the node or null / 노드 또는 null
     */
    public Node previousNode() {
        Node node = current;
        // 루트에 닿을 때까지 이전 형제와 그 안쪽 끝을 먼저, 없으면 부모를 본다.
        while (node != getRoot()) {
            Node sibling = node.prevSibling;
            // 이전 형제들을 하나씩 살피며 받아들여지는 노드를 찾는다.
            while (sibling != null) {
                node = sibling;
                short result = filterNode(node);
                // 거부되지 않은 노드는 가장 안쪽 마지막 자식까지 내려가며 확인한다.
                while (result != NodeFilter.FILTER_REJECT && node.lastChild != null) {
                    node = node.lastChild;
                    result = filterNode(node);
                }
                if (result == NodeFilter.FILTER_ACCEPT) {
                    current = node;
                    return node;
                }
                sibling = node.prevSibling;
            }
            if (node == getRoot() || node.parent == null) {
                return null;
            }
            node = node.parent;
            if (filterNode(node) == NodeFilter.FILTER_ACCEPT) {
                current = node;
                return node;
            }
        }
        return null;
    }

    /**
     * Moves to the next accepted node in document order (not above the root).
     * <p>
     * 문서 순서로 받아들여지는 다음 노드로 옮긴다(루트 위로는 가지 않는다).
     *
     * @return the node or null / 노드 또는 null
     */
    public Node nextNode() {
        Node node = current;
        short result = NodeFilter.FILTER_ACCEPT;
        // 받아들여지는 노드를 찾을 때까지 자식 → 다음 형제 → 조상의 다음 형제 순으로 나아간다.
        for (;;) {
            // 거부되지 않은 노드의 자식으로 내려가며 확인한다.
            while (result != NodeFilter.FILTER_REJECT && node.firstChild != null) {
                node = node.firstChild;
                result = filterNode(node);
                if (result == NodeFilter.FILTER_ACCEPT) {
                    current = node;
                    return node;
                }
            }
            Node sibling = null;
            Node temporary = node;
            // 다음 형제가 있는 노드를 찾을 때까지 올라간다. 루트에 닿으면 끝이다.
            while (temporary != null) {
                if (temporary == getRoot()) {
                    return null;
                }
                sibling = temporary.nextSibling;
                if (sibling != null) {
                    node = sibling;
                    break;
                }
                temporary = temporary.parent;
            }
            if (sibling == null) {
                return null;
            }
            result = filterNode(node);
            if (result == NodeFilter.FILTER_ACCEPT) {
                current = node;
                return node;
            }
        }
    }

    /**
     * The "traverse children" algorithm of the DOM Standard.
     * <p>
     * DOM 표준의 "traverse children" 알고리즘.
     *
     * @param first true to look for the first child, false for the last / 첫 자식을 찾으려면 true, 마지막이면 false
     * @return the node or null / 노드 또는 null
     */
    private Node traverseChildren(boolean first) {
        Node node = first ? current.firstChild : current.lastChild;
        // 현재 노드의 자식에서 시작해, 건너뛴 노드의 안쪽과 형제를 차례로 살펴본다.
        while (node != null) {
            short result = filterNode(node);
            if (result == NodeFilter.FILTER_ACCEPT) {
                current = node;
                return node;
            }
            if (result == NodeFilter.FILTER_SKIP) {
                Node child = first ? node.firstChild : node.lastChild;
                if (child != null) {
                    node = child;
                    continue;
                }
            }
            // 다음 후보: 형제, 없으면 현재 노드를 넘지 않는 한 조상의 형제.
            while (node != null) {
                Node sibling = first ? node.nextSibling : node.prevSibling;
                if (sibling != null) {
                    node = sibling;
                    break;
                }
                Node parent = node.parent;
                if (parent == null || parent == getRoot() || parent == current) {
                    return null;
                }
                node = parent;
            }
        }
        return null;
    }

    /**
     * The "traverse siblings" algorithm of the DOM Standard.
     * <p>
     * DOM 표준의 "traverse siblings" 알고리즘.
     *
     * @param next true to look for the next sibling, false for the previous one / 다음 형제를 찾으려면 true, 이전 형제이면 false
     * @return the node or null / 노드 또는 null
     */
    private Node traverseSiblings(boolean next) {
        Node node = current;
        if (node == getRoot()) {
            return null;
        }
        // 형제에서 받아들여지는 노드를 찾고, 없으면 건너뛴 부모를 거슬러 올라가며 그 형제를 본다.
        for (;;) {
            Node sibling = next ? node.nextSibling : node.prevSibling;
            // 형제를 하나씩 살피며 받아들여지는 노드를 찾는다.
            while (sibling != null) {
                node = sibling;
                short result = filterNode(node);
                if (result == NodeFilter.FILTER_ACCEPT) {
                    current = node;
                    return node;
                }
                sibling = next ? node.firstChild : node.lastChild;
                if (result == NodeFilter.FILTER_REJECT || sibling == null) {
                    sibling = next ? node.nextSibling : node.prevSibling;
                }
            }
            node = node.parent;
            if (node == null || node == getRoot()) {
                return null;
            }
            if (filterNode(node) == NodeFilter.FILTER_ACCEPT) {
                return null;
            }
        }
    }
}
