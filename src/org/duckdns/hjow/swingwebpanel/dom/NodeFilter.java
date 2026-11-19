package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A filter that a <code>NodeIterator</code> or a <code>TreeWalker</code> asks about every node it looks at (the <code>NodeFilter</code>
 * callback interface of the DOM Standard, "Interface NodeFilter"). It also holds the constants of the interface: the three results and the
 * bits of <code>whatToShow</code>.
 * <p>
 * <code>NodeIterator</code> 나 <code>TreeWalker</code> 가 살펴보는 모든 노드에 대해 묻는 필터(DOM 표준의 <code>NodeFilter</code> 콜백 인터페이스,
 * "Interface NodeFilter"). 인터페이스의 상수인 세 가지 결과와 <code>whatToShow</code> 의 비트도 함께 가진다.
 */
public interface NodeFilter {

    /**
     * Result: the node is accepted.
     * <p>
     * 결과: 노드를 받아들인다.
     */
    short FILTER_ACCEPT = 1;

    /**
     * Result: the node and its descendants are rejected (a <code>NodeIterator</code> treats this like skip).
     * <p>
     * 결과: 노드와 그 후손을 거부한다(<code>NodeIterator</code> 는 건너뛰기와 같게 다룬다).
     */
    short FILTER_REJECT = 2;

    /**
     * Result: the node is skipped but its descendants are still looked at.
     * <p>
     * 결과: 노드는 건너뛰지만 후손은 계속 살펴본다.
     */
    short FILTER_SKIP = 3;

    /**
     * Bit mask that shows every kind of node.
     * <p>
     * 모든 종류의 노드를 보이는 비트 마스크.
     */
    long SHOW_ALL = 0xFFFFFFFFL;

    /**
     * Bit of element nodes.
     * <p>
     * 요소 노드의 비트.
     */
    long SHOW_ELEMENT = 0x1L;

    /**
     * Bit of attribute nodes.
     * <p>
     * 속성 노드의 비트.
     */
    long SHOW_ATTRIBUTE = 0x2L;

    /**
     * Bit of text nodes.
     * <p>
     * 텍스트 노드의 비트.
     */
    long SHOW_TEXT = 0x4L;

    /**
     * Bit of CDATA section nodes.
     * <p>
     * CDATA 섹션 노드의 비트.
     */
    long SHOW_CDATA_SECTION = 0x8L;

    /**
     * Bit of entity reference nodes (obsolete).
     * <p>
     * 엔티티 참조 노드의 비트(폐기됨).
     */
    long SHOW_ENTITY_REFERENCE = 0x10L;

    /**
     * Bit of entity nodes (obsolete).
     * <p>
     * 엔티티 노드의 비트(폐기됨).
     */
    long SHOW_ENTITY = 0x20L;

    /**
     * Bit of processing instruction nodes.
     * <p>
     * 처리 명령 노드의 비트.
     */
    long SHOW_PROCESSING_INSTRUCTION = 0x40L;

    /**
     * Bit of comment nodes.
     * <p>
     * 주석 노드의 비트.
     */
    long SHOW_COMMENT = 0x80L;

    /**
     * Bit of document nodes.
     * <p>
     * 문서 노드의 비트.
     */
    long SHOW_DOCUMENT = 0x100L;

    /**
     * Bit of document type nodes.
     * <p>
     * 문서 유형 노드의 비트.
     */
    long SHOW_DOCUMENT_TYPE = 0x200L;

    /**
     * Bit of document fragment nodes.
     * <p>
     * 문서 조각 노드의 비트.
     */
    long SHOW_DOCUMENT_FRAGMENT = 0x400L;

    /**
     * Bit of notation nodes (obsolete).
     * <p>
     * 표기법 노드의 비트(폐기됨).
     */
    long SHOW_NOTATION = 0x800L;

    /**
     * Decides whether the walk accepts a node. An exception that this method throws goes through the traversal call to its caller.
     * <p>
     * 탐색이 노드를 받아들일지 정한다. 이 메서드가 던진 예외는 탐색 호출을 거쳐 호출자에게 전달된다.
     *
     * @param node the node to look at / 살펴볼 노드
     * @return {@link #FILTER_ACCEPT}, {@link #FILTER_REJECT} or {@link #FILTER_SKIP} / 세 결과 중 하나
     */
    short acceptNode(Node node);
}
