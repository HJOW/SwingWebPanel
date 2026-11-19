package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A document type (<code>&lt;!DOCTYPE name publicId systemId&gt;</code>). The HTML parser uses its name and identifiers to choose the
 * document mode (standards, limited quirks, quirks).
 * <p>
 * 문서 유형(<code>&lt;!DOCTYPE name publicId systemId&gt;</code>). HTML 파서는 이름과 식별자로 문서 모드(표준, 제한 호환, 호환)를 고른다.
 */
public class DocumentType extends Node {

    /**
     * The name.
     * <p>
     * 이름.
     */
    private final String name;

    /**
     * The public identifier, empty if none.
     * <p>
     * 공개 식별자. 없으면 빈 문자열.
     */
    private final String publicId;

    /**
     * The system identifier, empty if none.
     * <p>
     * 시스템 식별자. 없으면 빈 문자열.
     */
    private final String systemId;

    /**
     * Creates a document type.
     * <p>
     * 문서 유형을 만든다.
     *
     * @param owner    owner document / 소유 문서
     * @param name     name / 이름
     * @param publicId public identifier, null means empty / 공개 식별자(null 은 빈 문자열)
     * @param systemId system identifier, null means empty / 시스템 식별자(null 은 빈 문자열)
     */
    DocumentType(Document owner, String name, String publicId, String systemId) {
        super(owner);
        this.name = name;
        this.publicId = publicId == null ? "" : publicId;
        this.systemId = systemId == null ? "" : systemId;
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 반환한다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the public identifier.
     * <p>
     * 공개 식별자를 반환한다.
     *
     * @return public identifier / 공개 식별자
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Returns the system identifier.
     * <p>
     * 시스템 식별자를 반환한다.
     *
     * @return system identifier / 시스템 식별자
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns {@link Node#DOCUMENT_TYPE_NODE}.
     * <p>
     * {@link Node#DOCUMENT_TYPE_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return DOCUMENT_TYPE_NODE;
    }

    /**
     * Returns the name as node name.
     * <p>
     * 이름을 노드 이름으로 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return name;
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
        return new DocumentType(target, name, publicId, systemId);
    }

    /**
     * Compares name and identifiers.
     * <p>
     * 이름과 식별자를 비교한다.
     *
     * @param other node of the same type / 같은 종류의 노드
     * @return true if equal / 같으면 true
     */
    boolean equalOwnData(Node other) {
        DocumentType o = (DocumentType) other;
        return name.equals(o.name) && publicId.equals(o.publicId) && systemId.equals(o.systemId);
    }
}
