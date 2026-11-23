package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The base of all DOM tree objects (DOM Standard <code>Node</code>): the tree structure (parent, first/last child, siblings), the
 * mutation algorithms (pre-insert, replace, remove, adopt, clone with the validity rules and the exceptions the standard prescribes),
 * text content, normalization, equality and document position. Every change of the tree goes through the same few private methods, which
 * keep the links consistent, bump the document's mutation version (live collections use it to refresh themselves) and report a
 * {@link MutationRecord} to the document's listeners. A node belongs to exactly one {@link Document} and must only be used on that
 * document's thread.
 * <p>
 * 모든 DOM 트리 객체의 기반(DOM 표준 <code>Node</code>): 트리 구조(부모, 첫/마지막 자식, 형제), 변경 알고리즘(pre-insert, replace, remove, adopt,
 * 표준이 정한 유효성 규칙과 예외를 갖춘 복제), 텍스트 내용, 정규화, 동등성, 문서 내 위치. 트리의 모든 변경은 같은 몇 개의 비공개 메서드를 거치며
 * 이 메서드들이 연결을 일관되게 유지하고, 문서의 변경 버전을 올리고(라이브 컬렉션이 이를 보고 갱신한다), 문서의 리스너에게
 * {@link MutationRecord} 를 보고한다. 노드는 정확히 하나의 {@link Document} 에 속하며 그 문서의 스레드에서만 써야 한다.
 */
public abstract class Node extends EventTarget {

    /**
     * Node type: element.
     * <p>
     * 노드 종류: 요소.
     */
    public static final short ELEMENT_NODE = 1;

    /**
     * Node type: attribute (attributes are not tree nodes in this implementation; the value exists for completeness).
     * <p>
     * 노드 종류: 속성(이 구현에서 속성은 트리 노드가 아니며 값은 완전성을 위해 있다).
     */
    public static final short ATTRIBUTE_NODE = 2;

    /**
     * Node type: text.
     * <p>
     * 노드 종류: 텍스트.
     */
    public static final short TEXT_NODE = 3;

    /**
     * Node type: CDATA section.
     * <p>
     * 노드 종류: CDATA 섹션.
     */
    public static final short CDATA_SECTION_NODE = 4;

    /**
     * Node type: processing instruction.
     * <p>
     * 노드 종류: 처리 명령.
     */
    public static final short PROCESSING_INSTRUCTION_NODE = 7;

    /**
     * Node type: comment.
     * <p>
     * 노드 종류: 주석.
     */
    public static final short COMMENT_NODE = 8;

    /**
     * Node type: document.
     * <p>
     * 노드 종류: 문서.
     */
    public static final short DOCUMENT_NODE = 9;

    /**
     * Node type: document type.
     * <p>
     * 노드 종류: 문서 유형.
     */
    public static final short DOCUMENT_TYPE_NODE = 10;

    /**
     * Node type: document fragment.
     * <p>
     * 노드 종류: 문서 조각.
     */
    public static final short DOCUMENT_FRAGMENT_NODE = 11;

    /**
     * Document position bit: the nodes are in different trees.
     * <p>
     * 문서 내 위치 비트: 서로 다른 트리에 있다.
     */
    public static final short DOCUMENT_POSITION_DISCONNECTED = 1;

    /**
     * Document position bit: the other node precedes this node.
     * <p>
     * 문서 내 위치 비트: 다른 노드가 이 노드보다 앞선다.
     */
    public static final short DOCUMENT_POSITION_PRECEDING = 2;

    /**
     * Document position bit: the other node follows this node.
     * <p>
     * 문서 내 위치 비트: 다른 노드가 이 노드보다 뒤이다.
     */
    public static final short DOCUMENT_POSITION_FOLLOWING = 4;

    /**
     * Document position bit: the other node contains this node.
     * <p>
     * 문서 내 위치 비트: 다른 노드가 이 노드를 포함한다.
     */
    public static final short DOCUMENT_POSITION_CONTAINS = 8;

    /**
     * Document position bit: the other node is contained by this node.
     * <p>
     * 문서 내 위치 비트: 다른 노드가 이 노드에 포함된다.
     */
    public static final short DOCUMENT_POSITION_CONTAINED_BY = 16;

    /**
     * Document position bit: the order of the two nodes is implementation specific.
     * <p>
     * 문서 내 위치 비트: 두 노드의 순서가 구현에 따라 정해진다.
     */
    public static final short DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 32;

    /**
     * An empty node list shared by records without nodes.
     * <p>
     * 노드가 없는 기록이 공유하는 빈 노드 목록.
     */
    private static final List<Node> NO_NODES = Collections.emptyList();

    /**
     * Owner document (a document is its own owner internally).
     * <p>
     * 소유 문서(문서는 내부적으로 자기 자신이 소유자이다).
     */
    Document doc;

    /**
     * Parent node or null.
     * <p>
     * 부모 노드 또는 null.
     */
    Node parent;

    /**
     * First child or null.
     * <p>
     * 첫 자식 또는 null.
     */
    Node firstChild;

    /**
     * Last child or null.
     * <p>
     * 마지막 자식 또는 null.
     */
    Node lastChild;

    /**
     * Previous sibling or null.
     * <p>
     * 이전 형제 또는 null.
     */
    Node prevSibling;

    /**
     * Next sibling or null.
     * <p>
     * 다음 형제 또는 null.
     */
    Node nextSibling;

    /**
     * Cached live child list (the same object is returned on every access).
     * <p>
     * 캐시된 라이브 자식 목록(접근할 때마다 같은 객체를 반환한다).
     */
    private NodeList childNodesView;

    /**
     * Creates a node owned by a document.
     * <p>
     * 문서가 소유하는 노드를 만든다.
     *
     * @param owner owner document; a document passes null and sets itself afterwards / 소유 문서. 문서는 null 을 넘기고 나중에 자신을 지정한다
     */
    Node(Document owner) {
        this.doc = owner;
    }

    /**
     * Returns the node type constant.
     * <p>
     * 노드 종류 상수를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public abstract short getNodeType();

    /**
     * Returns the node name (tag name in upper case for HTML elements, <code>#text</code>, <code>#document</code>, ...).
     * <p>
     * 노드 이름을 반환한다(HTML 요소는 대문자 태그 이름, <code>#text</code>, <code>#document</code> 등).
     *
     * @return node name / 노드 이름
     */
    public abstract String getNodeName();

    /**
     * Returns the node value (data of character nodes), or null for other nodes.
     * <p>
     * 노드 값(문자 노드의 데이터)을 반환한다. 다른 노드는 null.
     *
     * @return value or null / 값 또는 null
     */
    public String getNodeValue() {
        return null;
    }

    /**
     * Sets the node value; ignored for nodes without a value.
     * <p>
     * 노드 값을 지정한다. 값이 없는 노드는 무시한다.
     *
     * @param value new value / 새 값
     */
    public void setNodeValue(String value) {
        // 값이 없는 노드는 아무 일도 하지 않는다. 문자 노드가 재정의한다.
    }

    /**
     * Returns the owner document; a document returns null.
     * <p>
     * 소유 문서를 반환한다. 문서는 null 을 반환한다.
     *
     * @return owner document or null / 소유 문서 또는 null
     */
    public Document getOwnerDocument() {
        return doc == this ? null : doc;
    }

    /**
     * Returns the document this node belongs to (a document returns itself); never null for nodes created through a document.
     * <p>
     * 이 노드가 속한 문서를 반환한다(문서는 자기 자신). 문서를 통해 만든 노드에서는 null 이 아니다.
     *
     * @return document / 문서
     */
    public Document document() {
        return doc;
    }

    /**
     * Returns the parent node.
     * <p>
     * 부모 노드를 반환한다.
     *
     * @return parent or null / 부모 또는 null
     */
    public Node getParentNode() {
        return parent;
    }

    /**
     * Returns the parent if it is an element.
     * <p>
     * 부모가 요소이면 그것을 반환한다.
     *
     * @return parent element or null / 부모 요소 또는 null
     */
    public Element getParentElement() {
        return parent instanceof Element ? (Element) parent : null;
    }

    /**
     * Returns the first child.
     * <p>
     * 첫 자식을 반환한다.
     *
     * @return first child or null / 첫 자식 또는 null
     */
    public Node getFirstChild() {
        return firstChild;
    }

    /**
     * Returns the last child.
     * <p>
     * 마지막 자식을 반환한다.
     *
     * @return last child or null / 마지막 자식 또는 null
     */
    public Node getLastChild() {
        return lastChild;
    }

    /**
     * Returns the previous sibling.
     * <p>
     * 이전 형제를 반환한다.
     *
     * @return previous sibling or null / 이전 형제 또는 null
     */
    public Node getPreviousSibling() {
        return prevSibling;
    }

    /**
     * Returns the next sibling.
     * <p>
     * 다음 형제를 반환한다.
     *
     * @return next sibling or null / 다음 형제 또는 null
     */
    public Node getNextSibling() {
        return nextSibling;
    }

    /**
     * Tells whether the node has children.
     * <p>
     * 노드에 자식이 있는지 알려 준다.
     *
     * @return true if it has children / 자식이 있으면 true
     */
    public boolean hasChildNodes() {
        return firstChild != null;
    }

    /**
     * Returns the live list of the children; the same object is returned every time.
     * <p>
     * 자식의 라이브 목록을 반환한다. 매번 같은 객체이다.
     *
     * @return child list / 자식 목록
     */
    public NodeList getChildNodes() {
        if (childNodesView == null) {
            childNodesView = new ChildNodeList(this);
        }
        return childNodesView;
    }

    /**
     * Returns the root of the tree this node is in (the node itself if it has no parent).
     * <p>
     * 이 노드가 속한 트리의 루트를 반환한다(부모가 없으면 자기 자신).
     *
     * @return root node / 루트 노드
     */
    public Node getRootNode() {
        Node n = this;
        // 부모가 없는 노드에 닿을 때까지 위로 올라간다.
        while (n.parent != null) {
            n = n.parent;
        }
        return n;
    }

    /**
     * Tells whether the node is part of its document's tree.
     * <p>
     * 노드가 문서의 트리에 속하는지 알려 준다.
     *
     * @return true if connected / 연결되어 있으면 true
     */
    public boolean isConnected() {
        return getRootNode() == doc;
    }

    /**
     * Tells whether another node is this node or one of its descendants.
     * <p>
     * 다른 노드가 이 노드 자신이거나 후손인지 알려 준다.
     *
     * @param other node to test, may be null / 검사할 노드(null 가능)
     * @return true if contained / 포함되면 true
     */
    public boolean contains(Node other) {
        Node n = other;
        // other 에서 위로 올라가며 이 노드를 만나는지 본다.
        while (n != null) {
            if (n == this) {
                return true;
            }
            n = n.parent;
        }
        return false;
    }

    /**
     * Returns the next node in tree order inside a root, or null at the end.
     * <p>
     * 루트 안에서 트리 순서의 다음 노드를 반환한다. 끝이면 null.
     *
     * @param current node to start from / 시작 노드
     * @param root    root that bounds the traversal / 순회 범위를 정하는 루트
     * @return following node or null / 다음 노드 또는 null
     */
    public static Node nextInTree(Node current, Node root) {
        Node n = current.firstChild;
        if (n != null) {
            return n;
        }
        Node c = current;
        // 다음 형제가 있는 조상을 찾을 때까지 위로 올라간다.
        while (c != root && c != null) {
            if (c.nextSibling != null) {
                return c.nextSibling;
            }
            c = c.parent;
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 요소 형제/자식 탐색 (ParentNode, NonDocumentTypeChildNode)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the first child that is an element.
     * <p>
     * 요소인 첫 자식을 반환한다.
     *
     * @return element or null / 요소 또는 null
     */
    public Element getFirstElementChild() {
        // 자식을 앞에서부터 보며 첫 요소를 찾는다.
        for (Node c = firstChild; c != null; c = c.nextSibling) {
            if (c instanceof Element) {
                return (Element) c;
            }
        }
        return null;
    }

    /**
     * Returns the last child that is an element.
     * <p>
     * 요소인 마지막 자식을 반환한다.
     *
     * @return element or null / 요소 또는 null
     */
    public Element getLastElementChild() {
        // 자식을 뒤에서부터 보며 마지막 요소를 찾는다.
        for (Node c = lastChild; c != null; c = c.prevSibling) {
            if (c instanceof Element) {
                return (Element) c;
            }
        }
        return null;
    }

    /**
     * Returns the number of children that are elements.
     * <p>
     * 요소인 자식의 수를 반환한다.
     *
     * @return count / 개수
     */
    public int getChildElementCount() {
        int n = 0;
        // 자식 중 요소만 센다.
        for (Node c = firstChild; c != null; c = c.nextSibling) {
            if (c instanceof Element) {
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the closest preceding sibling that is an element.
     * <p>
     * 앞쪽 형제 중 가장 가까운 요소를 반환한다.
     *
     * @return element or null / 요소 또는 null
     */
    public Element getPreviousElementSibling() {
        // 이전 형제를 거슬러 올라가며 요소를 찾는다.
        for (Node c = prevSibling; c != null; c = c.prevSibling) {
            if (c instanceof Element) {
                return (Element) c;
            }
        }
        return null;
    }

    /**
     * Returns the closest following sibling that is an element.
     * <p>
     * 뒤쪽 형제 중 가장 가까운 요소를 반환한다.
     *
     * @return element or null / 요소 또는 null
     */
    public Element getNextElementSibling() {
        // 다음 형제를 따라가며 요소를 찾는다.
        for (Node c = nextSibling; c != null; c = c.nextSibling) {
            if (c instanceof Element) {
                return (Element) c;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 변경 알고리즘
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Appends a node (or all children of a document fragment) as the last child(ren).
     * <p>
     * 노드(또는 문서 조각의 모든 자식)를 마지막 자식으로 추가한다.
     *
     * @param node node to add, not null / 추가할 노드(null 불가)
     * @return the added node (the fragment for a fragment) / 추가한 노드(조각이면 조각)
     * @throws DomException HierarchyRequestError, NotFoundError / HierarchyRequestError, NotFoundError
     */
    public Node appendChild(Node node) {
        return preInsert(node, null);
    }

    /**
     * Inserts a node (or the children of a fragment) before a child; with a null reference the node is appended.
     * <p>
     * 노드(또는 조각의 자식)를 자식 앞에 삽입한다. 기준이 null 이면 끝에 추가한다.
     *
     * @param node  node to insert, not null / 삽입할 노드(null 불가)
     * @param child reference child or null / 기준 자식 또는 null
     * @return the inserted node / 삽입한 노드
     * @throws DomException HierarchyRequestError, NotFoundError / HierarchyRequestError, NotFoundError
     */
    public Node insertBefore(Node node, Node child) {
        return preInsert(node, child);
    }

    /**
     * Replaces a child with a node (or with the children of a fragment).
     * <p>
     * 자식을 노드(또는 조각의 자식)로 교체한다.
     *
     * @param node  new node, not null / 새 노드(null 불가)
     * @param child child to replace, not null / 교체할 자식(null 불가)
     * @return the replaced child / 교체된 자식
     * @throws DomException HierarchyRequestError, NotFoundError / HierarchyRequestError, NotFoundError
     */
    public Node replaceChild(Node node, Node child) {
        if (node == null || child == null) {
            throw new IllegalArgumentException("node and child must not be null");
        }
        ensureValidParent();
        ensureNoCycle(node);
        if (child.parent != this) {
            throw DomException.notFound("the node to be replaced is not a child of this node");
        }
        ensureValidChildType(node);
        ensureDocumentRulesForReplace(node, child);
        Node reference = child.nextSibling;
        if (reference == node) {
            reference = node.nextSibling;
        }
        Node previous = child.prevSibling;
        doc.adoptInternal(node);
        // adopt 가 같은 부모에서 node 를 뺐을 수 있으므로 child 가 아직 자식인지 확인한 뒤 제거한다.
        List<Node> removed = new ArrayList<Node>(1);
        if (child.parent == this) {
            removed.add(child);
            removeChildInternal(child, true);
        }
        List<Node> added = nodesToInsert(node);
        insertInternal(node, reference, true);
        doc.recordChildList(this, added, removed, previous, reference);
        return child;
    }

    /**
     * Removes a child.
     * <p>
     * 자식을 제거한다.
     *
     * @param child child to remove, not null / 제거할 자식(null 불가)
     * @return the removed child / 제거한 자식
     * @throws DomException NotFoundError if it is not a child / 자식이 아니면 NotFoundError
     */
    public Node removeChild(Node child) {
        if (child == null) {
            throw new IllegalArgumentException("child must not be null");
        }
        if (child.parent != this) {
            throw DomException.notFound("the node to be removed is not a child of this node");
        }
        removeChildInternal(child, false);
        return child;
    }

    /**
     * The "pre-insert" algorithm of the DOM Standard.
     * <p>
     * DOM 표준의 "pre-insert" 알고리즘.
     *
     * @param node  node to insert / 삽입할 노드
     * @param child reference child or null / 기준 자식 또는 null
     * @return the inserted node / 삽입한 노드
     */
    private Node preInsert(Node node, Node child) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        ensureValidParent();
        ensureNoCycle(node);
        if (child != null && child.parent != this) {
            throw DomException.notFound("the reference node is not a child of this node");
        }
        ensureValidChildType(node);
        ensureDocumentRulesForInsert(node, child);
        Node reference = child;
        if (reference == node) {
            reference = node.nextSibling;
        }
        doc.adoptInternal(node);
        insertInternal(node, reference, false);
        return node;
    }

    /**
     * Throws unless a node could be pre-inserted into this node before a child (the checks of "ensure pre-insertion validity"), without
     * changing anything. {@link Range#insertNode} calls it before it splits the text at its start, as the DOM Standard orders the steps.
     * <p>
     * 노드를 이 노드의 자식 앞에 사전 삽입할 수 있는지 아무것도 바꾸지 않고 확인하며, 안 되면 예외를 던진다("ensure pre-insertion validity" 의 검사).
     * DOM 표준의 단계 순서대로 {@link Range#insertNode} 가 시작 지점의 텍스트를 쪼개기 전에 부른다.
     *
     * @param node  node to insert / 삽입할 노드
     * @param child reference child or null / 기준 자식 또는 null
     */
    final void ensurePreInsertionValidity(Node node, Node child) {
        ensureValidParent();
        ensureNoCycle(node);
        if (child != null && child.parent != this) {
            throw DomException.notFound("the reference node is not a child of this node");
        }
        ensureValidChildType(node);
        ensureDocumentRulesForInsert(node, child);
    }

    /**
     * Throws unless this node may have children (document, fragment, element).
     * <p>
     * 이 노드가 자식을 가질 수 있는 종류(문서, 조각, 요소)가 아니면 예외를 던진다.
     */
    private void ensureValidParent() {
        short t = getNodeType();
        if (t != DOCUMENT_NODE && t != DOCUMENT_FRAGMENT_NODE && t != ELEMENT_NODE) {
            throw DomException.hierarchyRequest("this node type cannot have children");
        }
    }

    /**
     * Throws if the node is this node or one of its ancestors (the insertion would create a cycle).
     * <p>
     * 노드가 이 노드 자신이거나 조상이면 예외를 던진다(삽입하면 순환이 생긴다).
     *
     * @param node node to insert / 삽입할 노드
     */
    private void ensureNoCycle(Node node) {
        if (node.contains(this)) {
            throw DomException.hierarchyRequest("the new child contains the parent");
        }
    }

    /**
     * Throws unless the node may be a child at all (fragment, doctype, element, text, comment, processing instruction) and unless the
     * text/doctype placement rules hold.
     * <p>
     * 노드가 자식이 될 수 있는 종류(조각, 문서 유형, 요소, 텍스트, 주석, 처리 명령)이고 텍스트/문서 유형 배치 규칙에 맞지 않으면 예외를 던진다.
     *
     * @param node node to insert / 삽입할 노드
     */
    private void ensureValidChildType(Node node) {
        short t = node.getNodeType();
        if (t != DOCUMENT_FRAGMENT_NODE && t != DOCUMENT_TYPE_NODE && t != ELEMENT_NODE && t != TEXT_NODE && t != CDATA_SECTION_NODE
                && t != PROCESSING_INSTRUCTION_NODE && t != COMMENT_NODE) {
            throw DomException.hierarchyRequest("this node type cannot be a child");
        }
        short p = getNodeType();
        if ((t == TEXT_NODE || t == CDATA_SECTION_NODE) && p == DOCUMENT_NODE) {
            throw DomException.hierarchyRequest("a document cannot have text children");
        }
        if (t == DOCUMENT_TYPE_NODE && p != DOCUMENT_NODE) {
            throw DomException.hierarchyRequest("a document type can only be a child of a document");
        }
    }

    /**
     * Applies the extra rules of the DOM Standard for insertion into a document (one element, one doctype, doctype before element).
     * <p>
     * 문서에 삽입할 때의 DOM 표준 추가 규칙을 적용한다(요소 하나, 문서 유형 하나, 문서 유형이 요소보다 앞).
     *
     * @param node  node to insert / 삽입할 노드
     * @param child reference child or null / 기준 자식 또는 null
     */
    private void ensureDocumentRulesForInsert(Node node, Node child) {
        if (getNodeType() != DOCUMENT_NODE) {
            return;
        }
        short t = node.getNodeType();
        if (t == DOCUMENT_FRAGMENT_NODE) {
            int elements = node.getChildElementCount();
            if (elements > 1 || hasTextChild(node)) {
                throw DomException.hierarchyRequest("a document fragment with several elements or text cannot be inserted into a document");
            }
            if (elements == 1 && (hasElementChild() || (child != null && child.getNodeType() == DOCUMENT_TYPE_NODE)
                    || (child != null && hasDoctypeAfter(child)))) {
                throw DomException.hierarchyRequest("a document can have only one element");
            }
        } else if (t == ELEMENT_NODE) {
            if (hasElementChild() || (child != null && child.getNodeType() == DOCUMENT_TYPE_NODE)
                    || (child != null && hasDoctypeAfter(child))) {
                throw DomException.hierarchyRequest("a document can have only one element");
            }
        } else if (t == DOCUMENT_TYPE_NODE) {
            if (hasDoctypeChild() || (child != null && hasElementBefore(child)) || (child == null && hasElementChild())) {
                throw DomException.hierarchyRequest("a document can have only one document type that comes before the element");
            }
        }
    }

    /**
     * Applies the extra rules of the DOM Standard for replacing a child of a document.
     * <p>
     * 문서의 자식을 교체할 때의 DOM 표준 추가 규칙을 적용한다.
     *
     * @param node  new node / 새 노드
     * @param child child to replace / 교체할 자식
     */
    private void ensureDocumentRulesForReplace(Node node, Node child) {
        if (getNodeType() != DOCUMENT_NODE) {
            return;
        }
        short t = node.getNodeType();
        if (t == DOCUMENT_FRAGMENT_NODE) {
            int elements = node.getChildElementCount();
            if (elements > 1 || hasTextChild(node)) {
                throw DomException.hierarchyRequest("a document fragment with several elements or text cannot replace a child of a document");
            }
            if (elements == 1 && (hasElementChildExcept(child) || hasDoctypeAfter(child))) {
                throw DomException.hierarchyRequest("a document can have only one element");
            }
        } else if (t == ELEMENT_NODE) {
            if (hasElementChildExcept(child) || hasDoctypeAfter(child)) {
                throw DomException.hierarchyRequest("a document can have only one element");
            }
        } else if (t == DOCUMENT_TYPE_NODE) {
            if (hasDoctypeChildExcept(child) || hasElementBefore(child)) {
                throw DomException.hierarchyRequest("a document can have only one document type that comes before the element");
            }
        }
    }

    /**
     * Tells whether a node has a text child.
     * <p>
     * 노드에 텍스트 자식이 있는지 알려 준다.
     *
     * @param n node / 노드
     * @return true if it has one / 있으면 true
     */
    private static boolean hasTextChild(Node n) {
        // 자식 중 텍스트(또는 CDATA)가 있는지 찾는다.
        for (Node c = n.firstChild; c != null; c = c.nextSibling) {
            if (c.getNodeType() == TEXT_NODE || c.getNodeType() == CDATA_SECTION_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether this node has an element child.
     * <p>
     * 이 노드에 요소 자식이 있는지 알려 준다.
     *
     * @return true if it has one / 있으면 true
     */
    private boolean hasElementChild() {
        return hasElementChildExcept(null);
    }

    /**
     * Tells whether this node has an element child other than the given one.
     * <p>
     * 주어진 노드가 아닌 요소 자식이 이 노드에 있는지 알려 준다.
     *
     * @param except child to ignore, may be null / 무시할 자식(null 가능)
     * @return true if there is such a child / 있으면 true
     */
    private boolean hasElementChildExcept(Node except) {
        // 제외할 자식이 아닌 요소가 있는지 찾는다.
        for (Node c = firstChild; c != null; c = c.nextSibling) {
            if (c != except && c.getNodeType() == ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether this node has a document type child.
     * <p>
     * 이 노드에 문서 유형 자식이 있는지 알려 준다.
     *
     * @return true if it has one / 있으면 true
     */
    private boolean hasDoctypeChild() {
        return hasDoctypeChildExcept(null);
    }

    /**
     * Tells whether this node has a document type child other than the given one.
     * <p>
     * 주어진 노드가 아닌 문서 유형 자식이 이 노드에 있는지 알려 준다.
     *
     * @param except child to ignore, may be null / 무시할 자식(null 가능)
     * @return true if there is such a child / 있으면 true
     */
    private boolean hasDoctypeChildExcept(Node except) {
        // 제외할 자식이 아닌 문서 유형이 있는지 찾는다.
        for (Node c = firstChild; c != null; c = c.nextSibling) {
            if (c != except && c.getNodeType() == DOCUMENT_TYPE_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a document type follows the given child.
     * <p>
     * 주어진 자식 뒤에 문서 유형이 있는지 알려 준다.
     *
     * @param child child to start behind / 기준 자식
     * @return true if a document type follows / 문서 유형이 뒤따르면 true
     */
    private static boolean hasDoctypeAfter(Node child) {
        // 기준 자식 뒤의 형제 중 문서 유형을 찾는다.
        for (Node c = child.nextSibling; c != null; c = c.nextSibling) {
            if (c.getNodeType() == DOCUMENT_TYPE_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether an element precedes the given child.
     * <p>
     * 주어진 자식 앞에 요소가 있는지 알려 준다.
     *
     * @param child child to start before / 기준 자식
     * @return true if an element precedes / 요소가 앞서면 true
     */
    private static boolean hasElementBefore(Node child) {
        // 기준 자식 앞의 형제 중 요소를 찾는다.
        for (Node c = child.prevSibling; c != null; c = c.prevSibling) {
            if (c.getNodeType() == ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the nodes that an insertion of a node adds: the node, or the children of a fragment.
     * <p>
     * 노드를 삽입할 때 실제로 추가되는 노드를 돌려준다: 노드 자신 또는 조각의 자식.
     *
     * @param node node to insert / 삽입할 노드
     * @return list of nodes / 노드 목록
     */
    private static List<Node> nodesToInsert(Node node) {
        List<Node> nodes = new ArrayList<Node>();
        if (node.getNodeType() == DOCUMENT_FRAGMENT_NODE) {
            // 조각이면 그 자식들이 삽입된다.
            for (Node c = node.firstChild; c != null; c = c.nextSibling) {
                nodes.add(c);
            }
        } else {
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * The "insert" algorithm: links the node (or the fragment's children) before a reference child. The caller has already adopted
     * the node and validated the insertion.
     * <p>
     * "insert" 알고리즘: 노드(또는 조각의 자식)를 기준 자식 앞에 연결한다. 호출자가 이미 노드를 adopt 하고 삽입의 유효성을 검사했다.
     *
     * @param node     node or fragment / 노드 또는 조각
     * @param child    reference child or null / 기준 자식 또는 null
     * @param suppress true to skip the mutation record (the caller reports it) / 변경 기록을 생략하려면 true(호출자가 보고)
     */
    private void insertInternal(Node node, Node child, boolean suppress) {
        List<Node> nodes = nodesToInsert(node);
        if (nodes.isEmpty()) {
            return;
        }
        if (node.getNodeType() == DOCUMENT_FRAGMENT_NODE) {
            // 조각의 자식을 먼저 조각에서 떼어 낸다(조각 쪽에도 기록을 남긴다).
            Node fragLast = node.lastChild;
            for (int i = 0; i < nodes.size(); i++) {
                node.unlink(nodes.get(i));
            }
            node.doc.recordChildList(node, NO_NODES, nodes, null, null);
            if (fragLast == null) {
                return;
            }
        }
        Node previous = child != null ? child.prevSibling : lastChild;
        // 노드를 순서대로 기준 자식 앞에 연결한다.
        for (int i = 0; i < nodes.size(); i++) {
            linkBefore(nodes.get(i), child);
        }
        doc.bumpVersion();
        if (!suppress) {
            doc.recordChildList(this, nodes, NO_NODES, previous, child);
        }
        if (isConnected()) {
            // 문서에 연결되었으므로 삽입된 노드와 그 후손에 연결 알림을 준다.
            for (int i = 0; i < nodes.size(); i++) {
                notifyConnected(nodes.get(i), true);
            }
        }
    }

    /**
     * Links a node before a reference child (no checks, no records).
     * <p>
     * 노드를 기준 자식 앞에 연결한다(검사도 기록도 없음).
     *
     * @param n   node to link / 연결할 노드
     * @param ref reference child or null for the end / 기준 자식. 끝이면 null
     */
    private void linkBefore(Node n, Node ref) {
        n.parent = this;
        n.nextSibling = ref;
        n.prevSibling = ref != null ? ref.prevSibling : lastChild;
        if (n.prevSibling != null) {
            n.prevSibling.nextSibling = n;
        } else {
            firstChild = n;
        }
        if (ref != null) {
            ref.prevSibling = n;
        } else {
            lastChild = n;
        }
    }

    /**
     * Unlinks a child (no checks, no records).
     * <p>
     * 자식의 연결을 끊는다(검사도 기록도 없음).
     *
     * @param c child to unlink / 연결을 끊을 자식
     */
    private void unlink(Node c) {
        if (c.prevSibling != null) {
            c.prevSibling.nextSibling = c.nextSibling;
        } else {
            firstChild = c.nextSibling;
        }
        if (c.nextSibling != null) {
            c.nextSibling.prevSibling = c.prevSibling;
        } else {
            lastChild = c.prevSibling;
        }
        c.parent = null;
        c.prevSibling = null;
        c.nextSibling = null;
    }

    /**
     * Appends a child without any check or record. Used while building trees that nobody else can see yet (clones, parser output).
     * <p>
     * 검사나 기록 없이 자식을 추가한다. 아직 아무도 볼 수 없는 트리(복제, 파서 출력)를 만들 때 쓴다.
     *
     * @param n node to append, parentless and of the same document / 추가할 노드(부모 없음, 같은 문서)
     */
    final void appendUnchecked(Node n) {
        linkBefore(n, null);
        doc.bumpVersion();
    }

    /**
     * Removes a child from this node.
     * <p>
     * 이 노드에서 자식을 제거한다.
     *
     * @param child    child to remove / 제거할 자식
     * @param suppress true to skip the mutation record / 변경 기록을 생략하려면 true
     */
    final void removeChildInternal(Node child, boolean suppress) {
        Node previous = child.prevSibling;
        Node next = child.nextSibling;
        boolean connected = isConnected();
        unlink(child);
        doc.bumpVersion();
        if (!suppress) {
            List<Node> removed = new ArrayList<Node>(1);
            removed.add(child);
            doc.recordChildList(this, NO_NODES, removed, previous, next);
        }
        if (connected) {
            notifyConnected(child, false);
        }
    }

    /**
     * Calls {@link #connectedChanged(boolean)} for a node and all its descendants in tree order.
     * <p>
     * 노드와 모든 후손에 대해 트리 순서로 {@link #connectedChanged(boolean)} 를 호출한다.
     *
     * @param root      subtree root / 하위 트리 루트
     * @param connected the new connection state / 새 연결 상태
     */
    private static void notifyConnected(Node root, boolean connected) {
        Node n = root;
        // 하위 트리를 앞에서부터 순회하며 각 노드에 알린다.
        while (n != null) {
            n.connectedChanged(connected);
            n = nextInTree(n, root);
        }
    }


    /**
     * Hook called after the node (and every node of its subtree) was moved from another document into the document that now owns it. Subclasses
     * use it to move document specific state. The default does nothing.
     * <p>
     * 노드(와 하위 트리의 모든 노드)가 다른 문서에서 지금 소유한 문서로 옮겨진 뒤 호출되는 확장 지점. 하위 클래스가 문서에 속한 상태를 옮기는 데 쓴다.
     * 기본은 아무것도 하지 않는다.
     *
     * @param oldDocument the document that owned the node before / 이전에 노드를 소유한 문서
     */
    protected void adoptedFrom(Document oldDocument) {
        // 기본: 문서가 바뀌어도 옮길 상태가 없다.
    }
    /**
     * Hook called when the node became connected to or disconnected from its document's tree. Subclasses use it for work that must start
     * or stop with the connection (scripts, media, resource loading). The default does nothing.
     * <p>
     * 노드가 문서의 트리에 연결되거나 분리되었을 때 호출되는 확장 지점. 하위 클래스는 연결과 함께 시작하거나 멈춰야 하는 작업(스크립트, 미디어,
     * 리소스 로딩)에 쓴다. 기본은 아무것도 하지 않는다.
     *
     * @param connected true if connected, false if disconnected / 연결되었으면 true, 분리되었으면 false
     */
    protected void connectedChanged(boolean connected) {
        // 기본: 연결 상태 변화에 반응하지 않는다.
    }

    /**
     * Removes this node from its parent, if any (<code>ChildNode.remove()</code>).
     * <p>
     * 부모가 있으면 이 노드를 부모에서 제거한다(<code>ChildNode.remove()</code>).
     */
    public void remove() {
        if (parent != null) {
            parent.removeChild(this);
        }
    }

    /**
     * Appends nodes and strings (<code>ParentNode.append</code>); strings become text nodes.
     * <p>
     * 노드와 문자열을 추가한다(<code>ParentNode.append</code>). 문자열은 텍스트 노드가 된다.
     *
     * @param nodesOrStrings nodes or strings / 노드 또는 문자열
     */
    public void append(Object... nodesOrStrings) {
        appendChild(convertToNode(nodesOrStrings));
    }

    /**
     * Replaces all children with the given nodes and strings (<code>ParentNode.replaceChildren</code>); without arguments the children are
     * removed. The insertion is validated before any child is removed.
     * <p>
     * 모든 자식을 주어진 노드와 문자열로 교체한다(<code>ParentNode.replaceChildren</code>). 인수가 없으면 자식을 모두 제거한다. 자식을 제거하기 전에 삽입이
     * 유효한지 검사한다.
     *
     * @param nodesOrStrings nodes or strings / 노드 또는 문자열
     * @throws DomException HierarchyRequestError when the nodes cannot be children of this node
     */
    public void replaceChildren(Object... nodesOrStrings) {
        // 인수가 없으면 자식을 모두 지우기만 한다. 하나뿐이면 그 노드이고 여럿이면 조각이다.
        boolean none = nodesOrStrings.length == 0;
        Node n = convertToNode(nodesOrStrings);
        ensureValidParent();
        ensureNoCycle(n);
        ensureValidChildType(n);
        // 문서는 요소 자식이 하나뿐이고 텍스트가 없어야 한다.
        if (getNodeType() == DOCUMENT_NODE && n.getNodeType() == DOCUMENT_FRAGMENT_NODE
                && (n.getChildElementCount() > 1 || hasTextChild(n))) {
            throw DomException.hierarchyRequest("a document fragment with several elements or text cannot be inserted into a document");
        }
        replaceAllChildren(none ? null : n);
    }

    /**
     * Inserts nodes and strings before the first child (<code>ParentNode.prepend</code>).
     * <p>
     * 노드와 문자열을 첫 자식 앞에 삽입한다(<code>ParentNode.prepend</code>).
     *
     * @param nodesOrStrings nodes or strings / 노드 또는 문자열
     */
    public void prepend(Object... nodesOrStrings) {
        Node n = convertToNode(nodesOrStrings);
        insertBefore(n, firstChild);
    }

    /**
     * Inserts nodes and strings before this node (<code>ChildNode.before</code>).
     * <p>
     * 노드와 문자열을 이 노드 앞에 삽입한다(<code>ChildNode.before</code>).
     *
     * @param nodesOrStrings nodes or strings / 노드 또는 문자열
     */
    public void before(Object... nodesOrStrings) {
        Node p = parent;
        if (p == null) {
            return;
        }
        Node viablePrevious = prevSibling;
        // 삽입할 노드들 중에 있는 이전 형제는 건너뛰어 실제로 남을 이전 형제를 찾는다.
        while (viablePrevious != null && containsRef(nodesOrStrings, viablePrevious)) {
            viablePrevious = viablePrevious.prevSibling;
        }
        Node n = convertToNode(nodesOrStrings);
        p.insertBefore(n, viablePrevious == null ? p.firstChild : viablePrevious.nextSibling);
    }

    /**
     * Inserts nodes and strings after this node (<code>ChildNode.after</code>).
     * <p>
     * 노드와 문자열을 이 노드 뒤에 삽입한다(<code>ChildNode.after</code>).
     *
     * @param nodesOrStrings nodes or strings / 노드 또는 문자열
     */
    public void after(Object... nodesOrStrings) {
        Node p = parent;
        if (p == null) {
            return;
        }
        Node viableNext = nextSibling;
        // 삽입할 노드들 중에 있는 다음 형제는 건너뛴다.
        while (viableNext != null && containsRef(nodesOrStrings, viableNext)) {
            viableNext = viableNext.nextSibling;
        }
        Node n = convertToNode(nodesOrStrings);
        p.insertBefore(n, viableNext);
    }

    /**
     * Replaces this node with nodes and strings (<code>ChildNode.replaceWith</code>).
     * <p>
     * 이 노드를 노드와 문자열로 교체한다(<code>ChildNode.replaceWith</code>).
     *
     * @param nodesOrStrings nodes or strings / 노드 또는 문자열
     */
    public void replaceWith(Object... nodesOrStrings) {
        Node p = parent;
        if (p == null) {
            return;
        }
        Node viableNext = nextSibling;
        // 삽입할 노드들 중에 있는 다음 형제는 건너뛴다.
        while (viableNext != null && containsRef(nodesOrStrings, viableNext)) {
            viableNext = viableNext.nextSibling;
        }
        Node n = convertToNode(nodesOrStrings);
        if (parent == p) {
            p.replaceChild(n, this);
        } else {
            p.insertBefore(n, viableNext);
        }
    }

    /**
     * Tells whether an argument list contains the given node object.
     * <p>
     * 인수 목록에 주어진 노드 객체가 있는지 알려 준다.
     *
     * @param items arguments / 인수
     * @param node  node to look for / 찾을 노드
     * @return true if contained / 있으면 true
     */
    private static boolean containsRef(Object[] items, Node node) {
        // 인수를 하나씩 비교한다.
        for (int i = 0; i < items.length; i++) {
            if (items[i] == node) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts nodes and strings into one node: the node itself for a single node, otherwise a fragment (strings become text nodes).
     * <p>
     * 노드와 문자열을 하나의 노드로 바꾼다: 노드 하나이면 그 노드, 아니면 조각(문자열은 텍스트 노드가 됨).
     *
     * @param items nodes or strings / 노드 또는 문자열
     * @return the node / 노드
     */
    private Node convertToNode(Object[] items) {
        List<Node> nodes = new ArrayList<Node>(items.length);
        // 문자열은 텍스트 노드로, 노드는 그대로 목록에 모은다.
        for (int i = 0; i < items.length; i++) {
            Object o = items[i];
            if (o instanceof Node) {
                nodes.add((Node) o);
            } else {
                nodes.add(doc.createTextNode(String.valueOf(o)));
            }
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        DocumentFragment frag = doc.createDocumentFragment();
        // 모은 노드를 하나의 조각에 담는다.
        for (int i = 0; i < nodes.size(); i++) {
            frag.appendChild(nodes.get(i));
        }
        return frag;
    }

    /**
     * Replaces all children with a node (or with nothing if the node is null) and reports one record.
     * <p>
     * 모든 자식을 노드로 교체하고(노드가 null 이면 비움) 기록 하나를 보고한다.
     *
     * @param node node to insert or null / 삽입할 노드 또는 null
     */
    final void replaceAllChildren(Node node) {
        if (node != null) {
            doc.adoptInternal(node);
        }
        List<Node> removed = new ArrayList<Node>();
        // 현재 자식을 모두 제거 목록에 모은다.
        for (Node c = firstChild; c != null; c = c.nextSibling) {
            removed.add(c);
        }
        List<Node> added = node == null ? NO_NODES : nodesToInsert(node);
        boolean connected = isConnected();
        // 자식을 하나씩 떼어 낸다(기록은 마지막에 한 번만 남긴다).
        for (int i = 0; i < removed.size(); i++) {
            unlink(removed.get(i));
        }
        doc.bumpVersion();
        if (connected) {
            // 분리된 노드마다 연결 해제를 알린다.
            for (int i = 0; i < removed.size(); i++) {
                notifyConnected(removed.get(i), false);
            }
        }
        if (node != null) {
            insertInternal(node, null, true);
        }
        if (!removed.isEmpty() || !added.isEmpty()) {
            doc.recordChildList(this, added, removed, null, null);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 텍스트 내용, 복제, 정규화, 비교
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the text content: the concatenated data of all descendant text nodes for elements and fragments, the data for character
     * nodes, null for documents and document types.
     * <p>
     * 텍스트 내용을 반환한다: 요소와 조각은 모든 후손 텍스트 노드의 데이터를 이은 것, 문자 노드는 그 데이터, 문서와 문서 유형은 null.
     *
     * @return text content or null / 텍스트 내용 또는 null
     */
    public String getTextContent() {
        short t = getNodeType();
        if (t == ELEMENT_NODE || t == DOCUMENT_FRAGMENT_NODE) {
            StringBuilder sb = new StringBuilder();
            Node n = nextInTree(this, this);
            // 후손을 트리 순서로 훑으며 텍스트 노드의 데이터를 모은다.
            while (n != null) {
                if (n.getNodeType() == TEXT_NODE || n.getNodeType() == CDATA_SECTION_NODE) {
                    sb.append(((CharacterData) n).getData());
                }
                n = nextInTree(n, this);
            }
            return sb.toString();
        }
        if (this instanceof CharacterData) {
            return ((CharacterData) this).getData();
        }
        return null;
    }

    /**
     * Sets the text content: elements and fragments get one text node with the given text (or no child for an empty string); character
     * nodes get new data; other nodes ignore it. The text is never parsed as markup.
     * <p>
     * 텍스트 내용을 지정한다: 요소와 조각은 주어진 텍스트의 텍스트 노드 하나를 갖고(빈 문자열이면 자식 없음), 문자 노드는 새 데이터를 갖고,
     * 다른 노드는 무시한다. 텍스트는 마크업으로 해석되지 않는다.
     *
     * @param text new text, null is treated as empty / 새 텍스트(null 은 빈 문자열로 취급)
     */
    public void setTextContent(String text) {
        String value = text == null ? "" : text;
        short t = getNodeType();
        if (t == ELEMENT_NODE || t == DOCUMENT_FRAGMENT_NODE) {
            replaceAllChildren(value.length() == 0 ? null : doc.createTextNode(value));
        } else if (this instanceof CharacterData) {
            ((CharacterData) this).setData(value);
        }
    }

    /**
     * Clones the node (<code>cloneNode</code>): a copy without parent that belongs to the same document, with copies of the descendants
     * if deep.
     * <p>
     * 노드를 복제한다(<code>cloneNode</code>): 같은 문서에 속하고 부모가 없는 복사본. deep 이면 후손의 복사본도 가진다.
     *
     * @param deep true to copy the descendants / 후손도 복사하려면 true
     * @return the copy / 복사본
     */
    public Node cloneNode(boolean deep) {
        return cloneInternal(doc, deep);
    }

    /**
     * Clones the node into a target document.
     * <p>
     * 노드를 대상 문서로 복제한다.
     *
     * @param target document that owns the copy / 복사본을 소유할 문서
     * @param deep   true to copy the descendants / 후손도 복사하려면 true
     * @return the copy / 복사본
     */
    final Node cloneInternal(Document target, boolean deep) {
        Node copy = shallowCopy(target);
        if (deep) {
            Document owner = copy instanceof Document ? (Document) copy : target;
            // 자식을 차례로 복제해 복사본에 붙인다.
            for (Node c = firstChild; c != null; c = c.nextSibling) {
                copy.appendUnchecked(c.cloneInternal(owner, true));
            }
            // template 내용은 자식이 아니지만 깊은 복제와 import 시 독립적으로 복사해야 한다.
            if (this instanceof Element) {
                DocumentFragment contents = ((Element) this).getTemplateContents();
                if (contents != null) {
                    ((Element) copy).setTemplateContents((DocumentFragment) contents.cloneInternal(owner, true));
                }
            }
        }
        return copy;
    }

    /**
     * Creates a copy of this node without children.
     * <p>
     * 자식 없이 이 노드의 복사본을 만든다.
     *
     * @param target document that owns the copy / 복사본을 소유할 문서
     * @return the copy / 복사본
     */
    abstract Node shallowCopy(Document target);

    /**
     * Merges adjacent text nodes and removes empty ones in the whole subtree (<code>normalize</code>).
     * <p>
     * 하위 트리 전체에서 이웃한 텍스트 노드를 합치고 빈 것을 제거한다(<code>normalize</code>).
     */
    public void normalize() {
        Node c = firstChild;
        // 자식을 순회하며 텍스트 노드는 합치고 요소는 재귀적으로 정규화한다.
        while (c != null) {
            Node next = c.nextSibling;
            if (c.getNodeType() == TEXT_NODE) {
                Text text = (Text) c;
                if (text.getLength() == 0) {
                    removeChild(c);
                } else {
                    // 바로 뒤에 이어지는 텍스트 노드의 데이터를 한꺼번에 이 노드에 합친 뒤(DOM Standard "normalize" 4단계) 범위 경계를 옮기고 그 노드들을 제거한다.
                    StringBuilder data = new StringBuilder();
                    java.util.List<Node> merged = new java.util.ArrayList<Node>();
                    // 뒤따르는 텍스트 노드의 데이터를 모은다.
                    for (Node n = next; n != null && n.getNodeType() == TEXT_NODE; n = n.nextSibling) {
                        data.append(((Text) n).getData());
                        merged.add(n);
                    }
                    if (!merged.isEmpty()) {
                        int length = text.getLength();
                        text.appendData(data.toString());
                        Document doc = document();
                        // 합쳐지는 노드마다 살아 있는 범위의 경계점을 옮긴다(위치는 아직 제거 전이므로 text 바로 뒤에서 센다).
                        for (int k = 0; k < merged.size(); k++) {
                            Node m = merged.get(k);
                            doc.liveTextMerged(text, m, length, Range.indexOf(m));
                            length += ((Text) m).getLength();
                        }
                        // 합쳐진 노드를 제거한다.
                        for (int k = 0; k < merged.size(); k++) {
                            removeChild(merged.get(k));
                        }
                        next = text.nextSibling;
                    }
                }
            } else {
                c.normalize();
            }
            c = next;
        }
    }

    /**
     * Tells whether another node is this very node.
     * <p>
     * 다른 노드가 바로 이 노드인지 알려 준다.
     *
     * @param other other node / 다른 노드
     * @return true if identical / 같은 객체이면 true
     */
    public boolean isSameNode(Node other) {
        return this == other;
    }

    /**
     * Tells whether another node has the same type, names, attributes, data and (recursively) children.
     * <p>
     * 다른 노드가 종류, 이름, 속성, 데이터, (재귀적으로) 자식이 같은지 알려 준다.
     *
     * @param other other node, may be null / 다른 노드(null 가능)
     * @return true if equal / 같으면 true
     */
    public boolean isEqualNode(Node other) {
        if (other == null || other.getNodeType() != getNodeType()) {
            return false;
        }
        if (!equalOwnData(other)) {
            return false;
        }
        Node a = firstChild;
        Node b = other.firstChild;
        // 두 노드의 자식을 나란히 비교한다.
        while (a != null && b != null) {
            if (!a.isEqualNode(b)) {
                return false;
            }
            a = a.nextSibling;
            b = b.nextSibling;
        }
        return a == null && b == null;
    }

    /**
     * Compares the data that belong to the node itself (names, attributes, character data) with another node of the same type.
     * <p>
     * 노드 자신에 속한 데이터(이름, 속성, 문자 데이터)를 같은 종류의 다른 노드와 비교한다.
     *
     * @param other node of the same type / 같은 종류의 노드
     * @return true if equal / 같으면 true
     */
    boolean equalOwnData(Node other) {
        return true;
    }

    /**
     * Returns the bit mask that describes where another node is relative to this node (<code>compareDocumentPosition</code>).
     * <p>
     * 다른 노드가 이 노드를 기준으로 어디에 있는지를 나타내는 비트 마스크를 반환한다(<code>compareDocumentPosition</code>).
     *
     * @param other other node, not null / 다른 노드(null 불가)
     * @return position bits / 위치 비트
     */
    public short compareDocumentPosition(Node other) {
        if (other == this) {
            return 0;
        }
        List<Node> chain1 = ancestorsAndSelf(this);
        List<Node> chain2 = ancestorsAndSelf(other);
        if (chain1.get(0) != chain2.get(0)) {
            // 서로 다른 트리에 있으면 분리됨 + 구현 정의 순서(주소 대신 문서/식별 해시 순)로 앞뒤를 정한다.
            short order = System.identityHashCode(chain1.get(0)) < System.identityHashCode(chain2.get(0)) ? DOCUMENT_POSITION_PRECEDING
                    : DOCUMENT_POSITION_FOLLOWING;
            return (short) (DOCUMENT_POSITION_DISCONNECTED | DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC | order);
        }
        if (other.contains(this)) {
            return (short) (DOCUMENT_POSITION_CONTAINS | DOCUMENT_POSITION_PRECEDING);
        }
        if (this.contains(other)) {
            return (short) (DOCUMENT_POSITION_CONTAINED_BY | DOCUMENT_POSITION_FOLLOWING);
        }
        int i = 0;
        // 공통 조상 아래에서 갈라지는 지점까지 두 경로를 함께 내려간다.
        while (i < chain1.size() && i < chain2.size() && chain1.get(i) == chain2.get(i)) {
            i++;
        }
        Node a = chain1.get(i);
        Node b = chain2.get(i);
        // 갈라진 두 형제 중 어느 쪽이 앞인지 형제 사슬을 따라가 본다.
        for (Node s = a.nextSibling; s != null; s = s.nextSibling) {
            if (s == b) {
                return DOCUMENT_POSITION_FOLLOWING;
            }
        }
        return DOCUMENT_POSITION_PRECEDING;
    }

    /**
     * Returns the chain from the root down to a node.
     * <p>
     * 루트에서 노드까지의 사슬을 반환한다.
     *
     * @param n node / 노드
     * @return chain, root first / 사슬(루트가 첫 번째)
     */
    private static List<Node> ancestorsAndSelf(Node n) {
        List<Node> chain = new ArrayList<Node>();
        // 노드에서 루트로 올라가며 모은 뒤 뒤집는다.
        for (Node c = n; c != null; c = c.parent) {
            chain.add(c);
        }
        Collections.reverse(chain);
        return chain;
    }

    /**
     * Returns the document whose handler receives listener failures of this node.
     * <p>
     * 이 노드의 리스너 실패를 받는 핸들러를 반환한다.
     *
     * @return the handler / 핸들러
     */
    protected ListenerErrorHandler getListenerErrorHandler() {
        return doc != null ? doc.getListenerErrorHandlerInternal() : PRINTING_HANDLER;
    }

    /**
     * Returns the parent as the next target of the event path.
     * <p>
     * 이벤트 경로의 다음 대상으로 부모를 반환한다.
     *
     * @param event the event / 이벤트
     * @return parent / 부모
     */
    protected EventTarget getEventParent(Event event) {
        return parent;
    }
}
