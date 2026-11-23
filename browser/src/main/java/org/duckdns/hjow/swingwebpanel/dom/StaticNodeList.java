package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed list of nodes (the result of <code>querySelectorAll</code>). It does not change when the document changes.
 * <p>
 * 고정된 노드 목록(<code>querySelectorAll</code> 의 결과). 문서가 바뀌어도 변하지 않는다.
 */
public final class StaticNodeList implements NodeList {

    /**
     * The nodes.
     * <p>
     * 노드들.
     */
    private final List<Node> nodes;

    /**
     * Creates a list holding a copy of the given nodes.
     * <p>
     * 주어진 노드의 복사본을 가진 목록을 만든다.
     *
     * @param source nodes to copy / 복사할 노드
     */
    public StaticNodeList(List<? extends Node> source) {
        this.nodes = new ArrayList<Node>(source);
    }

    /**
     * Returns the number of nodes.
     * <p>
     * 노드의 수를 반환한다.
     *
     * @return length / 길이
     */
    public int getLength() {
        return nodes.size();
    }

    /**
     * Returns the node at an index.
     * <p>
     * 위치의 노드를 반환한다.
     *
     * @param index zero based index / 0 기반 위치
     * @return node or null / 노드 또는 null
     */
    public Node item(int index) {
        return index >= 0 && index < nodes.size() ? nodes.get(index) : null;
    }

    /**
     * Returns a copy of the nodes.
     * <p>
     * 노드의 복사본을 반환한다.
     *
     * @return nodes / 노드 목록
     */
    public List<Node> toList() {
        return new ArrayList<Node>(nodes);
    }
}
