package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The part that {@link NodeIterator} and {@link TreeWalker} share (DOM Standard, "Interface NodeIterator" and "Interface TreeWalker"): the
 * root node, the <code>whatToShow</code> bit mask, the optional {@link NodeFilter} and the algorithm that filters one node. The filter
 * must not be re-entered: while it runs, calling the traversal again is an <code>InvalidStateError</code>.
 * <p>
 * {@link NodeIterator} 와 {@link TreeWalker} 가 공유하는 부분(DOM 표준 "Interface NodeIterator", "Interface TreeWalker"): 루트 노드,
 * <code>whatToShow</code> 비트 마스크, 선택적 {@link NodeFilter}, 노드 하나를 걸러 내는 알고리즘. 필터는 재진입할 수 없다. 필터가 실행되는 동안
 * 탐색을 다시 부르면 <code>InvalidStateError</code> 이다.
 */
public abstract class Traversal {

    /**
     * The root of the traversal.
     * <p>
     * 탐색의 루트.
     */
    private final Node root;

    /**
     * The bit mask of the node kinds to show (unsigned 32 bits).
     * <p>
     * 보여 줄 노드 종류의 비트 마스크(부호 없는 32비트).
     */
    private final long whatToShow;

    /**
     * The filter, or null.
     * <p>
     * 필터. 없으면 null.
     */
    private final NodeFilter filter;

    /**
     * True while the filter runs.
     * <p>
     * 필터가 실행되는 동안 true.
     */
    private boolean active;

    /**
     * Creates the shared part.
     * <p>
     * 공유하는 부분을 만든다.
     *
     * @param root       the root node, not null / 루트 노드(null 불가)
     * @param whatToShow the bit mask / 비트 마스크
     * @param filter     the filter or null / 필터 또는 null
     */
    Traversal(Node root, long whatToShow, NodeFilter filter) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
        this.whatToShow = whatToShow & 0xFFFFFFFFL;
        this.filter = filter;
    }

    /**
     * Returns the root node.
     * <p>
     * 루트 노드를 반환한다.
     *
     * @return the root / 루트
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Returns the <code>whatToShow</code> bit mask.
     * <p>
     * <code>whatToShow</code> 비트 마스크를 반환한다.
     *
     * @return the mask (an unsigned 32 bit value) / 마스크(부호 없는 32비트 값)
     */
    public long getWhatToShow() {
        return whatToShow;
    }

    /**
     * Returns the filter.
     * <p>
     * 필터를 반환한다.
     *
     * @return the filter, or null / 필터. 없으면 null
     */
    public NodeFilter getFilter() {
        return filter;
    }

    /**
     * The "filter" algorithm of the DOM Standard: a node whose kind is not shown is skipped, otherwise the filter decides (a missing filter
     * accepts). The filter must not be running already.
     * <p>
     * DOM 표준의 "filter" 알고리즘: 종류가 보이지 않는 노드는 건너뛰고, 아니면 필터가 정한다(필터가 없으면 받아들인다). 필터가 이미 실행 중이면 안
     * 된다.
     *
     * @param node the node / 노드
     * @return {@link NodeFilter#FILTER_ACCEPT}, {@link NodeFilter#FILTER_REJECT} or {@link NodeFilter#FILTER_SKIP} / 세 결과 중 하나
     * @throws DomException InvalidStateError if the filter is running / 필터가 실행 중이면 InvalidStateError
     */
    final short filterNode(Node node) {
        if (active) {
            throw DomException.invalidState("the node filter is already running");
        }
        int bit = node.getNodeType() - 1;
        // 보이지 않는 종류의 노드는 필터에 묻지 않고 건너뛴다.
        if ((whatToShow >> bit & 1L) == 0) {
            return NodeFilter.FILTER_SKIP;
        }
        if (filter == null) {
            return NodeFilter.FILTER_ACCEPT;
        }
        active = true;
        try {
            return filter.acceptNode(node);
        } finally {
            active = false;
        }
    }
}
