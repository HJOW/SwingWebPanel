package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A text node. Text is stored exactly as given: it is never parsed as markup and never escaped; escaping happens only when a tree
 * is serialized.
 * <p>
 * 텍스트 노드. 텍스트는 주어진 그대로 저장되며 마크업으로 해석되거나 이스케이프되지 않는다. 이스케이프는 트리를 직렬화할 때만 일어난다.
 */
public class Text extends CharacterData {

    /**
     * Creates a text node.
     * <p>
     * 텍스트 노드를 만든다.
     *
     * @param owner owner document / 소유 문서
     * @param data  text / 텍스트
     */
    Text(Document owner, String data) {
        super(owner, data);
    }

    /**
     * Returns {@link Node#TEXT_NODE}.
     * <p>
     * {@link Node#TEXT_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return TEXT_NODE;
    }

    /**
     * Returns <code>#text</code>.
     * <p>
     * <code>#text</code> 를 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return "#text";
    }

    /**
     * Splits the node at an offset: this node keeps the text before it, a new sibling gets the rest and is returned.
     * <p>
     * 노드를 오프셋에서 나눈다: 이 노드는 앞부분을 유지하고, 새 형제가 나머지를 가져 반환된다.
     *
     * @param offset split offset / 나눌 위치
     * @return the new node / 새 노드
     * @throws DomException IndexSizeError if the offset is beyond the data / 오프셋이 데이터를 넘으면 IndexSizeError
     */
    public Text splitText(int offset) {
        if (offset < 0 || offset > getLength()) {
            throw DomException.indexSize("offset " + offset + " is outside the data");
        }
        String rest = substringData(offset, getLength() - offset);
        Text created = (Text) shallowCopy(doc);
        created.setData(rest);
        if (parent != null) {
            parent.insertBefore(created, nextSibling);
            // 살아 있는 범위: 쪼갠 위치 뒤의 경계점은 새 노드로 옮긴다(아래에서 뒷부분을 지우기 전에).
            doc.liveTextSplit(this, created, offset);
        }
        replaceData(offset, getLength() - offset, "");
        return created;
    }

    /**
     * Returns the text of this node and all directly adjacent text nodes (<code>wholeText</code>).
     * <p>
     * 이 노드와 바로 이웃한 모든 텍스트 노드의 텍스트를 반환한다(<code>wholeText</code>).
     *
     * @return the combined text / 합친 텍스트
     */
    public String getWholeText() {
        Node start = this;
        // 앞쪽으로 이어진 텍스트 노드의 시작을 찾는다.
        while (start.prevSibling != null && isTextType(start.prevSibling)) {
            start = start.prevSibling;
        }
        StringBuilder sb = new StringBuilder();
        // 시작부터 뒤로 이어진 텍스트 노드의 데이터를 모은다.
        for (Node n = start; n != null && isTextType(n); n = n.nextSibling) {
            sb.append(((CharacterData) n).getData());
        }
        return sb.toString();
    }

    /**
     * Tells whether a node is a text or CDATA node.
     * <p>
     * 노드가 텍스트 또는 CDATA 노드인지 알려 준다.
     *
     * @param n node / 노드
     * @return true for text nodes / 텍스트 노드이면 true
     */
    private static boolean isTextType(Node n) {
        return n.getNodeType() == TEXT_NODE || n.getNodeType() == CDATA_SECTION_NODE;
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
        return new Text(target, getData());
    }
}
