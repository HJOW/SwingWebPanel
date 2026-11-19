package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A range that does not follow changes of the tree (<code>StaticRange</code>, DOM Standard): two boundary points that are kept as they
 * were set. The target ranges of an <code>InputEvent</code> are such ranges, because the event is dispatched before the DOM changes.
 * <p>
 * 트리의 변경을 따라가지 않는 범위(<code>StaticRange</code>, DOM Standard): 정해진 그대로 보관되는 경계점 두 개. <code>InputEvent</code> 의 대상 범위가
 * 이런 범위이다. 이벤트가 DOM 이 바뀌기 전에 전달되기 때문이다.
 */
public final class StaticRange {

    /**
     * The start node.
     * <p>
     * 시작 노드.
     */
    private final Node startContainer;

    /**
     * The start offset.
     * <p>
     * 시작 오프셋.
     */
    private final int startOffset;

    /**
     * The end node.
     * <p>
     * 끝 노드.
     */
    private final Node endContainer;

    /**
     * The end offset.
     * <p>
     * 끝 오프셋.
     */
    private final int endOffset;

    /**
     * Creates a static range.
     * <p>
     * 정적 범위를 만든다.
     *
     * @param startContainer the start node / 시작 노드
     * @param startOffset    the start offset / 시작 오프셋
     * @param endContainer   the end node / 끝 노드
     * @param endOffset      the end offset / 끝 오프셋
     */
    public StaticRange(Node startContainer, int startOffset, Node endContainer, int endOffset) {
        this.startContainer = startContainer;
        this.startOffset = startOffset;
        this.endContainer = endContainer;
        this.endOffset = endOffset;
    }

    /**
     * Returns the start node.
     * <p>
     * 시작 노드를 반환한다.
     *
     * @return the node / 노드
     */
    public Node getStartContainer() {
        return startContainer;
    }

    /**
     * Returns the start offset.
     * <p>
     * 시작 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * Returns the end node.
     * <p>
     * 끝 노드를 반환한다.
     *
     * @return the node / 노드
     */
    public Node getEndContainer() {
        return endContainer;
    }

    /**
     * Returns the end offset.
     * <p>
     * 끝 오프셋을 반환한다.
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
}
