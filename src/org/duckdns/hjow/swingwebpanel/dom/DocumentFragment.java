package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A lightweight container of nodes without a place in a document tree. Inserting a fragment inserts its children and leaves the fragment
 * empty; the parser uses it for <code>innerHTML</code> results and templates.
 * <p>
 * 문서 트리에 자리가 없는 가벼운 노드 컨테이너. 조각을 삽입하면 그 자식들이 삽입되고 조각은 비게 된다. 파서는 <code>innerHTML</code> 결과와 템플릿에
 * 이것을 쓴다.
 */
public class DocumentFragment extends Node {

    /**
     * Creates a fragment.
     * <p>
     * 조각을 만든다.
     *
     * @param owner owner document / 소유 문서
     */
    DocumentFragment(Document owner) {
        super(owner);
    }

    /**
     * Returns {@link Node#DOCUMENT_FRAGMENT_NODE}.
     * <p>
     * {@link Node#DOCUMENT_FRAGMENT_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return DOCUMENT_FRAGMENT_NODE;
    }

    /**
     * Returns <code>#document-fragment</code>.
     * <p>
     * <code>#document-fragment</code> 를 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return "#document-fragment";
    }

    /**
     * Returns the first descendant element with the given id.
     * <p>
     * 주어진 id 를 가진 첫 후손 요소를 반환한다.
     *
     * @param id id value / id 값
     * @return element or null / 요소 또는 null
     */
    public Element getElementById(String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        Node n = nextInTree(this, this);
        // 후손을 트리 순서로 훑으며 id 가 일치하는 첫 요소를 찾는다.
        while (n != null) {
            if (n instanceof Element && id.equals(((Element) n).getId())) {
                return (Element) n;
            }
            n = nextInTree(n, this);
        }
        return null;
    }

    /**
     * Creates a copy.
     * <p>
     * 복사본을 만든다.
     *
     * @param target owner of the copy / 복사본의 소유 문서
     * @return the copy / 복사본
     */
    Node shallowCopy(Document target) {
        return new DocumentFragment(target);
    }

    /**
     * Returns the first descendant element that matches a selector list, or null.
     * <p>
     * 선택자 목록에 일치하는 첫 후손 요소를 반환한다. 없으면 null.
     *
     * @param selectors selector list / 선택자 목록
     * @return element or null / 요소 또는 null
     * @throws DomException SyntaxError for invalid selectors, NotSupportedError if no selector engine is installed / 잘못된 선택자는 SyntaxError, 선택자 엔진이 없으면 NotSupportedError
     */
    public Element querySelector(String selectors) {
        return document().requireSelectorEngine().querySelector(this, selectors);
    }

    /**
     * Returns all descendants that match a selector list as a static list.
     * <p>
     * 선택자 목록에 일치하는 모든 후손을 정적 목록으로 반환한다.
     *
     * @param selectors selector list / 선택자 목록
     * @return static node list / 정적 노드 목록
     * @throws DomException SyntaxError for invalid selectors, NotSupportedError if no selector engine is installed / 잘못된 선택자는 SyntaxError, 선택자 엔진이 없으면 NotSupportedError
     */
    public NodeList querySelectorAll(String selectors) {
        return new StaticNodeList(document().requireSelectorEngine().querySelectorAll(this, selectors));
    }
}
