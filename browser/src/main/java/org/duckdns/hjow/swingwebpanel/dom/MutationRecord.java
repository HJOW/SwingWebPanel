package org.duckdns.hjow.swingwebpanel.dom;

import java.util.Collections;
import java.util.List;

/**
 * A description of one change of the DOM (the data of a <code>MutationRecord</code> of the DOM Standard). The document passes one record
 * to its {@link DomMutationListener}s for every change of a child list, an attribute or the data of a character node. The lists are
 * immutable copies.
 * <p>
 * DOM 의 변경 하나에 대한 설명(DOM 표준 <code>MutationRecord</code> 의 데이터). 문서는 자식 목록, 속성, 문자 노드 데이터가 바뀔 때마다 기록
 * 하나를 {@link DomMutationListener} 에게 넘긴다. 목록은 변경할 수 없는 복사본이다.
 */
public final class MutationRecord {

    /**
     * Type value: the child list of the target changed.
     * <p>
     * 종류 값: 대상의 자식 목록이 바뀜.
     */
    public static final int CHILD_LIST = 0;

    /**
     * Type value: an attribute of the target changed.
     * <p>
     * 종류 값: 대상의 속성이 바뀜.
     */
    public static final int ATTRIBUTES = 1;

    /**
     * Type value: the data of the target (a character node) changed.
     * <p>
     * 종류 값: 대상(문자 노드)의 데이터가 바뀜.
     */
    public static final int CHARACTER_DATA = 2;

    /**
     * Change type.
     * <p>
     * 변경 종류.
     */
    private final int type;

    /**
     * Changed node.
     * <p>
     * 변경된 노드.
     */
    private final Node target;

    /**
     * Added nodes.
     * <p>
     * 추가된 노드.
     */
    private final List<Node> addedNodes;

    /**
     * Removed nodes.
     * <p>
     * 제거된 노드.
     */
    private final List<Node> removedNodes;

    /**
     * Previous sibling of the added or removed nodes, or null.
     * <p>
     * 추가/제거된 노드의 이전 형제. 없으면 null.
     */
    private final Node previousSibling;

    /**
     * Next sibling of the added or removed nodes, or null.
     * <p>
     * 추가/제거된 노드의 다음 형제. 없으면 null.
     */
    private final Node nextSibling;

    /**
     * Local name of the changed attribute, or null.
     * <p>
     * 바뀐 속성의 로컬 이름. 아니면 null.
     */
    private final String attributeName;

    /**
     * Namespace of the changed attribute, or null.
     * <p>
     * 바뀐 속성의 네임스페이스. 없으면 null.
     */
    private final String attributeNamespace;

    /**
     * Value before the change (attributes and character data), or null.
     * <p>
     * 변경 전의 값(속성과 문자 데이터). 아니면 null.
     */
    private final String oldValue;

    /**
     * Creates a record.
     * <p>
     * 기록을 만든다.
     *
     * @param type               change type / 변경 종류
     * @param target             changed node / 변경된 노드
     * @param addedNodes         added nodes / 추가된 노드
     * @param removedNodes       removed nodes / 제거된 노드
     * @param previousSibling    previous sibling or null / 이전 형제 또는 null
     * @param nextSibling        next sibling or null / 다음 형제 또는 null
     * @param attributeName      attribute local name or null / 속성 로컬 이름 또는 null
     * @param attributeNamespace attribute namespace or null / 속성 네임스페이스 또는 null
     * @param oldValue           old value or null / 이전 값 또는 null
     */
    MutationRecord(int type, Node target, List<Node> addedNodes, List<Node> removedNodes, Node previousSibling, Node nextSibling,
            String attributeName, String attributeNamespace, String oldValue) {
        this.type = type;
        this.target = target;
        this.addedNodes = Collections.unmodifiableList(addedNodes);
        this.removedNodes = Collections.unmodifiableList(removedNodes);
        this.previousSibling = previousSibling;
        this.nextSibling = nextSibling;
        this.attributeName = attributeName;
        this.attributeNamespace = attributeNamespace;
        this.oldValue = oldValue;
    }

    /**
     * Returns the change type ({@link #CHILD_LIST}, {@link #ATTRIBUTES} or {@link #CHARACTER_DATA}).
     * <p>
     * 변경 종류({@link #CHILD_LIST}, {@link #ATTRIBUTES}, {@link #CHARACTER_DATA})를 반환한다.
     *
     * @return type / 종류
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the type as the DOM string: <code>childList</code>, <code>attributes</code> or <code>characterData</code>.
     * <p>
     * 종류를 DOM 문자열(<code>childList</code>, <code>attributes</code>, <code>characterData</code>)로 반환한다.
     *
     * @return type name / 종류 이름
     */
    public String getTypeName() {
        return type == CHILD_LIST ? "childList" : type == ATTRIBUTES ? "attributes" : "characterData";
    }

    /**
     * Returns the changed node.
     * <p>
     * 변경된 노드를 반환한다.
     *
     * @return target / 대상
     */
    public Node getTarget() {
        return target;
    }

    /**
     * Returns the added nodes.
     * <p>
     * 추가된 노드를 반환한다.
     *
     * @return added nodes / 추가된 노드
     */
    public List<Node> getAddedNodes() {
        return addedNodes;
    }

    /**
     * Returns the removed nodes.
     * <p>
     * 제거된 노드를 반환한다.
     *
     * @return removed nodes / 제거된 노드
     */
    public List<Node> getRemovedNodes() {
        return removedNodes;
    }

    /**
     * Returns the previous sibling of the changed children.
     * <p>
     * 바뀐 자식의 이전 형제를 반환한다.
     *
     * @return sibling or null / 형제 또는 null
     */
    public Node getPreviousSibling() {
        return previousSibling;
    }

    /**
     * Returns the next sibling of the changed children.
     * <p>
     * 바뀐 자식의 다음 형제를 반환한다.
     *
     * @return sibling or null / 형제 또는 null
     */
    public Node getNextSibling() {
        return nextSibling;
    }

    /**
     * Returns the local name of the changed attribute.
     * <p>
     * 바뀐 속성의 로컬 이름을 반환한다.
     *
     * @return name or null / 이름 또는 null
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Returns the namespace of the changed attribute.
     * <p>
     * 바뀐 속성의 네임스페이스를 반환한다.
     *
     * @return namespace or null / 네임스페이스 또는 null
     */
    public String getAttributeNamespace() {
        return attributeNamespace;
    }

    /**
     * Returns the value before the change.
     * <p>
     * 변경 전의 값을 반환한다.
     *
     * @return old value or null / 이전 값 또는 null
     */
    public String getOldValue() {
        return oldValue;
    }
}
