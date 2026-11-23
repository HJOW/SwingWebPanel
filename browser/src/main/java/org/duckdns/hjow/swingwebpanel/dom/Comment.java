package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A comment node.
 * <p>
 * 주석 노드.
 */
public class Comment extends CharacterData {

    /**
     * Creates a comment.
     * <p>
     * 주석을 만든다.
     *
     * @param owner owner document / 소유 문서
     * @param data  comment text / 주석 텍스트
     */
    Comment(Document owner, String data) {
        super(owner, data);
    }

    /**
     * Returns {@link Node#COMMENT_NODE}.
     * <p>
     * {@link Node#COMMENT_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return COMMENT_NODE;
    }

    /**
     * Returns <code>#comment</code>.
     * <p>
     * <code>#comment</code> 를 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return "#comment";
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
        return new Comment(target, getData());
    }
}
