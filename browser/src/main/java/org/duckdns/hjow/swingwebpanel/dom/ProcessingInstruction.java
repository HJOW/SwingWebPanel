package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A processing instruction (<code>&lt;?target data?&gt;</code>), which appears in XML documents.
 * <p>
 * 처리 명령(<code>&lt;?target data?&gt;</code>). XML 문서에 나타난다.
 */
public class ProcessingInstruction extends CharacterData {

    /**
     * The target name.
     * <p>
     * 대상 이름.
     */
    private final String target;

    /**
     * Creates a processing instruction.
     * <p>
     * 처리 명령을 만든다.
     *
     * @param owner  owner document / 소유 문서
     * @param target target name / 대상 이름
     * @param data   data / 데이터
     */
    ProcessingInstruction(Document owner, String target, String data) {
        super(owner, data);
        this.target = target;
    }

    /**
     * Returns the target name.
     * <p>
     * 대상 이름을 반환한다.
     *
     * @return target / 대상
     */
    public String getTarget() {
        return target;
    }

    /**
     * Returns {@link Node#PROCESSING_INSTRUCTION_NODE}.
     * <p>
     * {@link Node#PROCESSING_INSTRUCTION_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return PROCESSING_INSTRUCTION_NODE;
    }

    /**
     * Returns the target as node name.
     * <p>
     * 대상 이름을 노드 이름으로 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return target;
    }

    /**
     * Creates a copy.
     * <p>
     * 복사본을 만든다.
     *
     * @param doc owner of the copy / 복사본의 소유 문서
     * @return the copy / 복사본
     */
    Node shallowCopy(Document doc) {
        return new ProcessingInstruction(doc, target, getData());
    }

    /**
     * Compares target and data.
     * <p>
     * 대상과 데이터를 비교한다.
     *
     * @param other node of the same type / 같은 종류의 노드
     * @return true if equal / 같으면 true
     */
    boolean equalOwnData(Node other) {
        return target.equals(((ProcessingInstruction) other).target) && super.equalOwnData(other);
    }
}
