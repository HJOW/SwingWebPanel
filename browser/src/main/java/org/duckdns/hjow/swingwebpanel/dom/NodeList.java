package org.duckdns.hjow.swingwebpanel.dom;

import java.util.List;

/**
 * An ordered list of nodes (<code>NodeList</code>). It is live when it comes from {@link Node#getChildNodes()} and static when it is the
 * result of a selector query.
 * <p>
 * 노드의 순서 있는 목록(<code>NodeList</code>). {@link Node#getChildNodes()} 에서 온 것은 라이브이고 선택자 질의의 결과는 정적이다.
 */
public interface NodeList {

    /**
     * Returns the number of nodes.
     * <p>
     * 노드의 수를 반환한다.
     *
     * @return length / 길이
     */
    int getLength();

    /**
     * Returns the node at an index.
     * <p>
     * 위치의 노드를 반환한다.
     *
     * @param index zero based index / 0 기반 위치
     * @return the node or null if the index is out of range / 노드. 범위 밖이면 null
     */
    Node item(int index);

    /**
     * Returns a snapshot copy of the nodes as a Java list.
     * <p>
     * 노드를 Java 목록의 스냅샷 복사본으로 반환한다.
     *
     * @return the nodes / 노드 목록
     */
    List<Node> toList();
}
