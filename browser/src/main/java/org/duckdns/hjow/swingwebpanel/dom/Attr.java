package org.duckdns.hjow.swingwebpanel.dom;

/**
 * An attribute of an element: namespace, prefix, local name and value. Attributes are not tree nodes in this implementation (the DOM
 * Standard no longer makes them nodes either); they live in the attribute list of their owner element. Changing the value of an attribute
 * that belongs to an element is reported like <code>setAttribute</code>.
 * <p>
 * 요소의 속성: 네임스페이스, 접두사, 로컬 이름, 값. 이 구현에서 속성은 트리 노드가 아니며(DOM 표준도 더 이상 노드로 보지 않는다) 소유 요소의
 * 속성 목록에 산다. 요소에 속한 속성의 값을 바꾸면 <code>setAttribute</code> 처럼 보고된다.
 */
public final class Attr {

    /**
     * Namespace URI or null.
     * <p>
     * 네임스페이스 URI 또는 null.
     */
    private final String namespaceURI;

    /**
     * Prefix or null.
     * <p>
     * 접두사 또는 null.
     */
    private final String prefix;

    /**
     * Local name.
     * <p>
     * 로컬 이름.
     */
    private final String localName;

    /**
     * Current value.
     * <p>
     * 현재 값.
     */
    private String value;

    /**
     * Element that owns the attribute, or null.
     * <p>
     * 속성을 가진 요소. 없으면 null.
     */
    Element ownerElement;

    /**
     * Document that created the attribute.
     * <p>
     * 속성을 만든 문서.
     */
    private Document doc;

    /**
     * Creates an attribute.
     * <p>
     * 속성을 만든다.
     *
     * @param doc          document / 문서
     * @param namespaceURI namespace or null / 네임스페이스 또는 null
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name / 로컬 이름
     * @param value        value / 값
     */
    Attr(Document doc, String namespaceURI, String prefix, String localName, String value) {
        this.doc = doc;
        this.namespaceURI = namespaceURI;
        this.prefix = prefix;
        this.localName = localName;
        this.value = value == null ? "" : value;
    }

    /**
     * Returns the namespace URI.
     * <p>
     * 네임스페이스 URI 를 반환한다.
     *
     * @return namespace or null / 네임스페이스 또는 null
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * Returns the prefix.
     * <p>
     * 접두사를 반환한다.
     *
     * @return prefix or null / 접두사 또는 null
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the local name.
     * <p>
     * 로컬 이름을 반환한다.
     *
     * @return local name / 로컬 이름
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Returns the qualified name (prefix and local name joined by a colon).
     * <p>
     * 정규화된 이름(접두사와 로컬 이름을 콜론으로 이은 것)을 반환한다.
     *
     * @return qualified name / 정규화된 이름
     */
    public String getName() {
        return prefix == null ? localName : prefix + ":" + localName;
    }

    /**
     * Returns the value.
     * <p>
     * 값을 반환한다.
     *
     * @return value / 값
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value; if the attribute belongs to an element the change is reported.
     * <p>
     * 값을 지정한다. 속성이 요소에 속해 있으면 변경이 보고된다.
     *
     * @param newValue new value, null is treated as empty / 새 값(null 은 빈 문자열)
     */
    public void setValue(String newValue) {
        String v = newValue == null ? "" : newValue;
        if (ownerElement != null) {
            ownerElement.changeAttributeValue(this, v);
        } else {
            value = v;
        }
    }


    /**
     * Changes the document that owns this attribute (when its element is adopted by another document).
     * <p>
     * 이 속성을 소유하는 문서를 바꾼다(요소가 다른 문서로 옮겨질 때).
     *
     * @param newDocument new owner / 새 소유 문서
     */
    void setOwnerDocument(Document newDocument) {
        this.doc = newDocument;
    }
    /**
     * Stores a value without reporting (used by the element after it reported the change).
     * <p>
     * 보고 없이 값을 저장한다(요소가 변경을 보고한 뒤 쓴다).
     *
     * @param newValue new value / 새 값
     */
    void storeValue(String newValue) {
        this.value = newValue;
    }

    /**
     * Returns the element that has this attribute.
     * <p>
     * 이 속성을 가진 요소를 반환한다.
     *
     * @return element or null / 요소 또는 null
     */
    public Element getOwnerElement() {
        return ownerElement;
    }

    /**
     * Returns the document that created this attribute.
     * <p>
     * 이 속성을 만든 문서를 반환한다.
     *
     * @return document / 문서
     */
    public Document getOwnerDocument() {
        return doc;
    }

    /**
     * Returns a readable text for logs.
     * <p>
     * 로그용 읽기 쉬운 문자열을 반환한다.
     *
     * @return text / 문자열
     */
    public String toString() {
        return getName() + "=\"" + value + "\"";
    }
}
