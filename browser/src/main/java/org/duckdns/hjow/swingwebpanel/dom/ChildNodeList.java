package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * The live list of the children of a node. It reads the tree on every call; sequential access is fast because the position of the last
 * access is cached (and dropped when the document changes).
 * <p>
 * 노드의 자식의 라이브 목록. 호출할 때마다 트리를 읽는다. 마지막 접근 위치를 캐시하므로(문서가 바뀌면 버림) 순차 접근이 빠르다.
 */
final class ChildNodeList implements NodeList {

    /**
     * Node whose children are listed.
     * <p>
     * 자식을 나열하는 노드.
     */
    private final Node owner;

    /**
     * Document version for which the cached position is valid.
     * <p>
     * 캐시된 위치가 유효한 문서 버전.
     */
    private long cachedVersion = -1;

    /**
     * Cached index.
     * <p>
     * 캐시된 위치.
     */
    private int cachedIndex;

    /**
     * Cached node at the cached index.
     * <p>
     * 캐시된 위치의 노드.
     */
    private Node cachedNode;

    /**
     * Creates the list.
     * <p>
     * 목록을 만든다.
     *
     * @param owner parent node / 부모 노드
     */
    ChildNodeList(Node owner) {
        this.owner = owner;
    }

    /**
     * Returns the number of children.
     * <p>
     * 자식의 수를 반환한다.
     *
     * @return length / 길이
     */
    public int getLength() {
        int n = 0;
        // 자식을 세어 길이를 구한다.
        for (Node c = owner.firstChild; c != null; c = c.nextSibling) {
            n++;
        }
        return n;
    }

    /**
     * Returns the child at an index.
     * <p>
     * 위치의 자식을 반환한다.
     *
     * @param index zero based index / 0 기반 위치
     * @return child or null / 자식 또는 null
     */
    public Node item(int index) {
        if (index < 0) {
            return null;
        }
        long version = owner.doc.getMutationVersion();
        Node n;
        int i;
        if (cachedVersion == version && cachedNode != null && index >= cachedIndex) {
            n = cachedNode;
            i = cachedIndex;
        } else {
            n = owner.firstChild;
            i = 0;
        }
        // 시작 위치에서 원하는 위치까지 다음 형제를 따라간다.
        while (n != null && i < index) {
            n = n.nextSibling;
            i++;
        }
        if (n != null) {
            cachedVersion = version;
            cachedIndex = i;
            cachedNode = n;
        }
        return n;
    }

    /**
     * Returns a snapshot copy of the children.
     * <p>
     * 자식의 스냅샷 복사본을 반환한다.
     *
     * @return children / 자식 목록
     */
    public List<Node> toList() {
        List<Node> list = new ArrayList<Node>();
        // 자식을 순서대로 목록에 담는다.
        for (Node c = owner.firstChild; c != null; c = c.nextSibling) {
            list.add(c);
        }
        return list;
    }
}
