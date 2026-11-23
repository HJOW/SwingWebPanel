package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A CDATA section, which exists only in XML documents (for example XHTML served as <code>application/xhtml+xml</code>). It behaves like
 * a text node whose node type is {@link Node#CDATA_SECTION_NODE}.
 * <p>
 * CDATA 섹션. XML 문서(예: <code>application/xhtml+xml</code> 로 제공된 XHTML)에만 있다. 노드 종류가 {@link Node#CDATA_SECTION_NODE} 인
 * 텍스트 노드처럼 동작한다.
 */
public class CDATASection extends Text {

    /**
     * Creates a CDATA section.
     * <p>
     * CDATA 섹션을 만든다.
     *
     * @param owner owner document / 소유 문서
     * @param data  text / 텍스트
     */
    CDATASection(Document owner, String data) {
        super(owner, data);
    }

    /**
     * Returns {@link Node#CDATA_SECTION_NODE}.
     * <p>
     * {@link Node#CDATA_SECTION_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return CDATA_SECTION_NODE;
    }

    /**
     * Returns <code>#cdata-section</code>.
     * <p>
     * <code>#cdata-section</code> 을 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return "#cdata-section";
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
        return new CDATASection(target, getData());
    }
}
