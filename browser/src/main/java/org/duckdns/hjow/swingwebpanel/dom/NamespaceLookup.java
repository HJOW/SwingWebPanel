package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The namespace lookups of <code>Node</code> (<code>lookupNamespaceURI</code>, <code>lookupPrefix</code>, <code>isDefaultNamespace</code>), the
 * algorithms "locate a namespace" and "locate a namespace prefix" of the WHATWG DOM Standard (section 4.4 "Interface Node"). The functions only
 * read the tree and the <code>xmlns</code> attributes; they never change anything. Use: <code>NamespaceLookup.locateNamespace(node, "svg")</code>.
 * <p>
 * <code>Node</code> 의 네임스페이스 조회(<code>lookupNamespaceURI</code>, <code>lookupPrefix</code>, <code>isDefaultNamespace</code>): WHATWG DOM Standard
 * (4.4 "Interface Node")의 "locate a namespace" 와 "locate a namespace prefix" 알고리즘. 함수는 트리와 <code>xmlns</code> 속성을 읽기만 하고
 * 아무것도 바꾸지 않는다. 사용: <code>NamespaceLookup.locateNamespace(node, "svg")</code>.
 */
public final class NamespaceLookup {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private NamespaceLookup() {
    }

    /**
     * Locates the namespace that a prefix is bound to as seen from a node ("locate a namespace"): the element's own namespace and prefix, the
     * <code>xmlns:prefix</code> or <code>xmlns</code> attributes, then the ancestors.
     * <p>
     * 노드에서 본 접두사가 묶인 네임스페이스를 찾는다("locate a namespace"): 요소 자신의 네임스페이스와 접두사, <code>xmlns:prefix</code> 나
     * <code>xmlns</code> 속성, 그 다음 조상.
     *
     * @param node   the node / 노드
     * @param prefix the prefix, or null for the default namespace / 접두사. 기본 네임스페이스이면 null
     * @return the namespace or null / 네임스페이스 또는 null
     */
    public static String locateNamespace(Node node, String prefix) {
        // 노드 종류마다 규격이 정한 위치에서 찾는다.
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element e = (Element) node;
            // 요소 자신의 접두사가 같으면 요소의 네임스페이스가 답이다.
            if (e.getNamespaceURI() != null && equal(e.getPrefix(), prefix)) {
                return e.getNamespaceURI();
            }
            NamedAttributeMap attrs = e.getAttributes();
            // xmlns:접두사 속성이나 접두사 없는 xmlns 속성이 있으면 그 값이 답이다(빈 값은 바인딩 해제).
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr a = attrs.item(i);
                if (!Namespaces.XMLNS.equals(a.getNamespaceURI())) {
                    continue;
                }
                boolean prefixed = "xmlns".equals(a.getPrefix()) && a.getLocalName().equals(prefix);
                boolean unprefixed = a.getPrefix() == null && "xmlns".equals(a.getLocalName()) && prefix == null;
                if (prefixed || unprefixed) {
                    return a.getValue().length() == 0 ? null : a.getValue();
                }
            }
            Element parent = e.getParentElement();
            return parent == null ? null : locateNamespace(parent, prefix);
        case Node.DOCUMENT_NODE:
            Element root = ((Document) node).getDocumentElement();
            return root == null ? null : locateNamespace(root, prefix);
        case Node.DOCUMENT_TYPE_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
            return null;
        default:
            Element up = node.getParentElement();
            return up == null ? null : locateNamespace(up, prefix);
        }
    }

    /**
     * Locates a prefix bound to a namespace as seen from a node ("locate a namespace prefix").
     * <p>
     * 노드에서 본 네임스페이스에 묶인 접두사를 찾는다("locate a namespace prefix").
     *
     * @param node      the node / 노드
     * @param namespace the namespace (not empty) / 네임스페이스(비어 있지 않음)
     * @return the prefix or null / 접두사 또는 null
     */
    public static String locatePrefix(Node node, String namespace) {
        // 노드 종류마다 규격이 정한 위치에서 찾는다.
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element e = (Element) node;
            // 요소 자신이 그 네임스페이스에 접두사를 갖고 있으면 그것이 답이다.
            if (namespace.equals(e.getNamespaceURI()) && e.getPrefix() != null) {
                return e.getPrefix();
            }
            NamedAttributeMap attrs = e.getAttributes();
            // xmlns:접두사 속성의 값이 그 네임스페이스이면 속성의 로컬 이름이 접두사이다.
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr a = attrs.item(i);
                if ("xmlns".equals(a.getPrefix()) && namespace.equals(a.getValue())) {
                    return a.getLocalName();
                }
            }
            Element parent = e.getParentElement();
            return parent == null ? null : locatePrefix(parent, namespace);
        case Node.DOCUMENT_NODE:
            Element root = ((Document) node).getDocumentElement();
            return root == null ? null : locatePrefix(root, namespace);
        case Node.DOCUMENT_TYPE_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
            return null;
        default:
            Element up = node.getParentElement();
            return up == null ? null : locatePrefix(up, namespace);
        }
    }

    /**
     * Tells whether two strings are equal when both may be null.
     * <p>
     * 두 문자열이 같은지 알려 준다(둘 다 null 일 수 있음).
     *
     * @param a the first string / 첫 문자열
     * @param b the second string / 두 번째 문자열
     * @return true if equal / 같으면 true
     */
    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
