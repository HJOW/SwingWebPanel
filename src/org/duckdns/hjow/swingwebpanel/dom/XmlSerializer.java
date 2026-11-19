package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The XML serialization of a DOM node, as defined for <code>XMLSerializer</code> (W3C DOM Parsing and Serialization, "XML Serialization",
 * with the <code>require well-formed</code> flag unset): elements and attributes are written so that their namespaces are kept, with
 * prefixes and namespace declarations added, replaced or dropped as needed (<code>xmlns</code> and <code>xmlns:prefix</code> are generated
 * where a namespace has no usable prefix; <code>ns1</code>, <code>ns2</code> ... are generated prefixes); an HTML element without children is
 * written with a start and end tag, except the void elements, which are written as <code>&lt;br /&gt;</code>; elements of other namespaces
 * without children are written as <code>&lt;a/&gt;</code>; text escapes <code>&amp; &lt; &gt;</code>, attribute values also escape the quotation
 * mark, tab, line feed and carriage return. Documents, document fragments, comments, CDATA sections, processing instructions and document
 * types are serialized too. This is not the HTML serialization of <code>innerHTML</code>.
 * <p>
 * DOM 노드의 XML 직렬화. <code>XMLSerializer</code> 가 쓰는 방식이다(W3C DOM Parsing and Serialization 의 "XML Serialization", <code>require
 * well-formed</code> 플래그는 끈 상태): 요소와 속성은 네임스페이스가 유지되도록 쓰며, 필요하면 접두사와 네임스페이스 선언을 더하거나 바꾸거나
 * 버린다(쓸 만한 접두사가 없는 네임스페이스에는 <code>xmlns</code> 와 <code>xmlns:prefix</code> 를 만들고, 만든 접두사는 <code>ns1</code>,
 * <code>ns2</code> ...). 자식이 없는 HTML 요소는 시작·끝 태그로 쓰되 void 요소만 <code>&lt;br /&gt;</code> 로 쓰고, 다른 네임스페이스의 자식 없는
 * 요소는 <code>&lt;a/&gt;</code> 로 쓴다. 텍스트는 <code>&amp; &lt; &gt;</code> 를 이스케이프하고 속성 값은 따옴표, 탭, 줄바꿈, 캐리지 리턴도
 * 이스케이프한다. 문서, 문서 조각, 주석, CDATA 섹션, 처리 명령, 문서 유형도 직렬화한다. <code>innerHTML</code> 의 HTML 직렬화와는 다르다.
 */
public final class XmlSerializer {

    /**
     * The names of the HTML elements that are written as <code>&lt;name /&gt;</code> when they have no children.
     * <p>
     * 자식이 없을 때 <code>&lt;name /&gt;</code> 로 쓰는 HTML 요소의 이름.
     */
    private static final String VOID_ELEMENTS = " area base basefont bgsound br col embed frame hr img input keygen link menuitem meta param source "
            + "track wbr ";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private XmlSerializer() {
    }

    /**
     * Returns the XML serialization of a node.
     * <p>
     * 노드의 XML 직렬화를 반환한다.
     *
     * @param node the node, not null / 노드(null 불가)
     * @return the markup / 마크업
     * @throws IllegalArgumentException for a null node / 노드가 null 이면
     */
    public static String serialize(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        add(map, "xml", Namespaces.XML);
        StringBuilder out = new StringBuilder();
        serializeNode(node, null, map, new int[] { 1 }, out);
        return out.toString();
    }

    /**
     * Serializes a node of any kind.
     * <p>
     * 어떤 종류의 노드든 직렬화한다.
     *
     * @param node      the node / 노드
     * @param namespace the current default namespace / 현재 기본 네임스페이스
     * @param map       the namespace prefix map / 네임스페이스 접두사 맵
     * @param index     the prefix index (a one element array that is shared) / 접두사 인덱스(공유하는 원소 하나짜리 배열)
     * @param out       the output / 출력
     */
    private static void serializeNode(Node node, String namespace, Map<String, List<String>> map, int[] index, StringBuilder out) {
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            serializeElement((Element) node, namespace, map, index, out);
            break;
        case Node.DOCUMENT_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
            // 자식을 차례로 직렬화한다.
            for (Node c = node.firstChild; c != null; c = c.nextSibling) {
                serializeNode(c, namespace, map, index, out);
            }
            break;
        case Node.COMMENT_NODE:
            out.append("<!--").append(((CharacterData) node).getData()).append("-->");
            break;
        case Node.CDATA_SECTION_NODE:
            out.append("<![CDATA[").append(((CharacterData) node).getData()).append("]]>");
            break;
        case Node.TEXT_NODE:
            out.append(escapeText(((CharacterData) node).getData()));
            break;
        case Node.DOCUMENT_TYPE_NODE:
            serializeDocumentType((DocumentType) node, out);
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            ProcessingInstruction pi = (ProcessingInstruction) node;
            out.append("<?").append(pi.getTarget()).append(' ').append(pi.getData()).append("?>");
            break;
        case Node.ATTRIBUTE_NODE:
            break;
        default:
            throw new IllegalArgumentException("only nodes can be serialized");
        }
    }

    /**
     * Serializes a document type: <code>&lt;!DOCTYPE name PUBLIC "id" "system"&gt;</code> with the parts that exist.
     * <p>
     * 문서 유형을 직렬화한다: 있는 부분만 쓴 <code>&lt;!DOCTYPE name PUBLIC "id" "system"&gt;</code>.
     *
     * @param type the document type / 문서 유형
     * @param out  the output / 출력
     */
    private static void serializeDocumentType(DocumentType type, StringBuilder out) {
        out.append("<!DOCTYPE ").append(type.getName());
        String pub = type.getPublicId();
        String sys = type.getSystemId();
        if (pub != null && pub.length() > 0) {
            out.append(" PUBLIC ").append(quoteId(pub));
        }
        if (sys != null && sys.length() > 0) {
            if (pub == null || pub.length() == 0) {
                out.append(" SYSTEM");
            }
            out.append(' ').append(quoteId(sys));
        }
        out.append('>');
    }

    /**
     * Quotes an identifier with the quotation mark, or with the apostrophe if it contains the quotation mark.
     * <p>
     * 식별자를 따옴표로 묶는다. 따옴표가 들어 있으면 작은따옴표로 묶는다.
     *
     * @param id the identifier / 식별자
     * @return the quoted identifier / 묶은 식별자
     */
    private static String quoteId(String id) {
        char q = id.indexOf('"') >= 0 ? '\'' : '"';
        return q + id + q;
    }

    /**
     * Serializes an element: the "XML serializing an Element node" algorithm.
     * <p>
     * 요소를 직렬화한다: "XML serializing an Element node" 알고리즘.
     *
     * @param element   the element / 요소
     * @param namespace the current default namespace / 현재 기본 네임스페이스
     * @param prefixMap the namespace prefix map of the parent / 부모의 네임스페이스 접두사 맵
     * @param index     the prefix index / 접두사 인덱스
     * @param out       the output / 출력
     */
    private static void serializeElement(Element element, String namespace, Map<String, List<String>> prefixMap, int[] index, StringBuilder out) {
        boolean ignoreNamespaceDefinition = false;
        Map<String, List<String>> map = copy(prefixMap);
        Map<String, String> localPrefixes = new LinkedHashMap<String, String>();
        String localDefault = recordNamespaces(element, map, localPrefixes);
        String inherited = namespace;
        String ns = element.getNamespaceURI();
        String local = element.getLocalName();
        String qualified;
        out.append('<');
        if (same(inherited, ns)) {
            // 요소가 현재 기본 네임스페이스에 있으면 접두사 없이 쓰고(XML 네임스페이스만 xml: 접두사) 기본 네임스페이스 선언은 버린다.
            if (localDefault != null) {
                ignoreNamespaceDefinition = true;
            }
            qualified = Namespaces.XML.equals(ns) ? "xml:" + local : local;
            out.append(qualified);
        } else {
            String prefix = element.getPrefix();
            String candidate = preferred(map, prefix, ns);
            if ("xmlns".equals(prefix)) {
                candidate = prefix;
            }
            if (candidate != null) {
                // 이 요소나 조상이 이미 이 네임스페이스에 접두사를 정의했다.
                qualified = candidate + ":" + local;
                if (localDefault != null && !Namespaces.XML.equals(localDefault)) {
                    inherited = localDefault.length() == 0 ? null : localDefault;
                }
                out.append(qualified);
            } else if (prefix != null) {
                // 접두사를 쓰되 요소가 같은 접두사를 다른 네임스페이스로 이미 선언했으면 새 접두사를 만들고, 선언을 덧붙인다.
                if (localPrefixes.containsKey(prefix)) {
                    prefix = generatePrefix(map, ns, index);
                }
                add(map, prefix, ns);
                qualified = prefix + ":" + local;
                out.append(qualified).append(" xmlns:").append(prefix).append("=\"").append(escapeAttribute(ns)).append('"');
                if (localDefault != null) {
                    inherited = localDefault.length() == 0 ? null : localDefault;
                }
            } else if (localDefault == null || !localDefault.equals(ns)) {
                // 접두사가 없으면 기본 네임스페이스 선언으로 이 요소의 네임스페이스를 정의한다.
                ignoreNamespaceDefinition = true;
                qualified = local;
                inherited = ns;
                out.append(qualified).append(" xmlns=\"").append(escapeAttribute(ns)).append('"');
            } else {
                qualified = local;
                inherited = ns;
                out.append(qualified);
            }
        }
        serializeAttributes(element, map, index, localPrefixes, ignoreNamespaceDefinition, out);
        boolean html = Namespaces.HTML.equals(ns);
        boolean empty = element.firstChild == null;
        if (html && empty && VOID_ELEMENTS.indexOf(" " + local + " ") >= 0) {
            out.append(" />");
            return;
        }
        if (!html && empty) {
            out.append("/>");
            return;
        }
        out.append('>');
        if (html && "template".equals(local) && element.getTemplateContents() != null) {
            serializeNode(element.getTemplateContents(), inherited, map, index, out);
        } else {
            // 자식을 차례로 직렬화한다.
            for (Node c = element.firstChild; c != null; c = c.nextSibling) {
                serializeNode(c, inherited, map, index, out);
            }
        }
        out.append("</").append(qualified).append('>');
    }

    /**
     * Records the namespace declarations of an element in the prefix map (the "recording the namespace information" algorithm) and returns its
     * default namespace declaration (the empty string for <code>xmlns=""</code>), or null if it has none.
     * <p>
     * 요소의 네임스페이스 선언을 접두사 맵에 기록하고("recording the namespace information" 알고리즘) 기본 네임스페이스 선언을 반환한다
     * (<code>xmlns=""</code> 는 빈 문자열). 없으면 null.
     *
     * @param element       the element / 요소
     * @param map           the prefix map of this element / 이 요소의 접두사 맵
     * @param localPrefixes the prefixes that this element declares / 이 요소가 선언하는 접두사
     * @return the default namespace declaration or null / 기본 네임스페이스 선언 또는 null
     */
    private static String recordNamespaces(Element element, Map<String, List<String>> map, Map<String, String> localPrefixes) {
        String defaultNamespace = null;
        NamedAttributeMap attrs = element.getAttributes();
        // 속성 목록에서 XMLNS 네임스페이스의 선언만 본다.
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = attrs.item(i);
            if (!Namespaces.XMLNS.equals(attr.getNamespaceURI())) {
                continue;
            }
            if (attr.getPrefix() == null) {
                defaultNamespace = attr.getValue();
                continue;
            }
            String prefixDefinition = attr.getLocalName();
            String definition = attr.getValue();
            if (Namespaces.XML.equals(definition)) {
                continue;
            }
            if (definition.length() == 0) {
                definition = null;
            }
            if (isFound(map, prefixDefinition, definition)) {
                continue;
            }
            add(map, prefixDefinition, definition);
            localPrefixes.put(prefixDefinition, definition == null ? "" : definition);
        }
        return defaultNamespace;
    }

    /**
     * Serializes the attributes of an element: the "XML serialization of the attributes" algorithm.
     * <p>
     * 요소의 속성을 직렬화한다: "XML serialization of the attributes" 알고리즘.
     *
     * @param element       the element / 요소
     * @param map           the prefix map / 접두사 맵
     * @param index         the prefix index / 접두사 인덱스
     * @param localPrefixes the prefixes that the element declares / 요소가 선언하는 접두사
     * @param ignoreDefault true to skip the default namespace declaration / 기본 네임스페이스 선언을 건너뛰려면 true
     * @param out           the output / 출력
     */
    private static void serializeAttributes(Element element, Map<String, List<String>> map, int[] index, Map<String, String> localPrefixes,
            boolean ignoreDefault, StringBuilder out) {
        NamedAttributeMap attrs = element.getAttributes();
        // 속성을 순서대로 쓴다. 필요하면 접두사 선언을 앞에 덧붙인다.
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = attrs.item(i);
            String attrNamespace = attr.getNamespaceURI();
            String candidate = null;
            if (attrNamespace != null) {
                String prefix = attr.getPrefix();
                candidate = preferred(map, prefix, attrNamespace);
                if (Namespaces.XMLNS.equals(attrNamespace)) {
                    String value = attr.getValue();
                    if (Namespaces.XML.equals(value)) {
                        continue;
                    }
                    if (prefix == null && ignoreDefault) {
                        continue;
                    }
                    if (prefix != null && (!localPrefixes.containsKey(attr.getLocalName()) || !localPrefixes.get(attr.getLocalName()).equals(value))
                            && isFound(map, attr.getLocalName(), value)) {
                        continue;
                    }
                    if ("xmlns".equals(prefix)) {
                        candidate = "xmlns";
                    }
                } else if (candidate == null) {
                    if (prefix != null && !localPrefixes.containsKey(prefix)) {
                        candidate = prefix;
                    } else {
                        candidate = generatePrefix(map, attrNamespace, index);
                    }
                    add(map, candidate, attrNamespace);
                    localPrefixes.put(candidate, attrNamespace);
                    out.append(" xmlns:").append(candidate).append("=\"").append(escapeAttribute(attrNamespace)).append('"');
                }
            }
            out.append(' ');
            if (candidate != null) {
                out.append(candidate).append(':');
            }
            out.append(attr.getLocalName()).append("=\"").append(escapeAttribute(attr.getValue())).append('"');
        }
    }

    /**
     * Copies a prefix map (the lists are copied too).
     * <p>
     * 접두사 맵을 복사한다(목록도 복사한다).
     *
     * @param map the map / 맵
     * @return the copy / 복사본
     */
    private static Map<String, List<String>> copy(Map<String, List<String>> map) {
        Map<String, List<String>> copy = new HashMap<String, List<String>>();
        // 키마다 접두사 목록을 복사한다.
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            copy.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
        return copy;
    }

    /**
     * The "retrieving a preferred prefix string" algorithm: the preferred prefix if the map has it for the namespace, else the last prefix
     * that was seen for the namespace, null if there is none.
     * <p>
     * "retrieving a preferred prefix string" 알고리즘: 맵에 있으면 선호하는 접두사, 아니면 그 네임스페이스에 마지막으로 본 접두사, 없으면 null.
     *
     * @param map       the map / 맵
     * @param preferred the preferred prefix or null / 선호하는 접두사 또는 null
     * @param ns        the namespace / 네임스페이스
     * @return the prefix or null / 접두사 또는 null
     */
    private static String preferred(Map<String, List<String>> map, String preferred, String ns) {
        List<String> candidates = map.get(ns);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (preferred != null) {
            // 선호하는 접두사가 후보에 있으면 그것을 쓴다.
            for (int i = 0; i < candidates.size(); i++) {
                if (preferred.equals(candidates.get(i))) {
                    return preferred;
                }
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /**
     * Tells whether a prefix is listed for a namespace.
     * <p>
     * 접두사가 네임스페이스에 등록되어 있는지 알려 준다.
     *
     * @param map    the map / 맵
     * @param prefix the prefix / 접두사
     * @param ns     the namespace / 네임스페이스
     * @return true if found / 있으면 true
     */
    private static boolean isFound(Map<String, List<String>> map, String prefix, String ns) {
        List<String> candidates = map.get(ns);
        return candidates != null && candidates.contains(prefix);
    }

    /**
     * Adds a prefix for a namespace to the map.
     * <p>
     * 맵에 네임스페이스의 접두사를 더한다.
     *
     * @param map    the map / 맵
     * @param prefix the prefix / 접두사
     * @param ns     the namespace / 네임스페이스
     */
    private static void add(Map<String, List<String>> map, String prefix, String ns) {
        List<String> candidates = map.get(ns);
        if (candidates == null) {
            candidates = new ArrayList<String>();
            map.put(ns, candidates);
        }
        candidates.add(prefix);
    }

    /**
     * The "generating namespace prefixes" algorithm: <code>ns</code> and the next number, which is also added to the map.
     * <p>
     * "generating namespace prefixes" 알고리즘: <code>ns</code> 와 다음 번호이며 맵에도 더한다.
     *
     * @param map   the map / 맵
     * @param ns    the namespace / 네임스페이스
     * @param index the prefix index / 접두사 인덱스
     * @return the generated prefix / 만든 접두사
     */
    private static String generatePrefix(Map<String, List<String>> map, String ns, int[] index) {
        String generated = "ns" + index[0];
        index[0]++;
        add(map, generated, ns);
        return generated;
    }

    /**
     * Compares two strings that may be null.
     * <p>
     * null 일 수 있는 두 문자열을 비교한다.
     *
     * @param a the first / 첫째
     * @param b the second / 둘째
     * @return true if equal / 같으면 true
     */
    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Escapes the text of a text node: ampersand, less-than and greater-than.
     * <p>
     * 텍스트 노드의 텍스트를 이스케이프한다: &amp;, &lt;, &gt;.
     *
     * @param text the text / 텍스트
     * @return the escaped text / 이스케이프한 텍스트
     */
    private static String escapeText(String text) {
        StringBuilder sb = new StringBuilder(text.length() + 8);
        // 문자마다 마크업에 쓰이는 문자만 바꾼다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes an attribute value (null is the empty string): ampersand, quotation mark, less-than, greater-than, tab, line feed and
     * carriage return.
     * <p>
     * 속성 값을 이스케이프한다(null 은 빈 문자열): &amp;, 따옴표, &lt;, &gt;, 탭, 줄바꿈, 캐리지 리턴.
     *
     * @param value the value / 값
     * @return the escaped value / 이스케이프한 값
     */
    private static String escapeAttribute(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        // 문자마다 속성 값에서 바꿔야 하는 문자만 바꾼다.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '\t') {
                sb.append("&#9;");
            } else if (c == '\n') {
                sb.append("&#xA;");
            } else if (c == '\r') {
                sb.append("&#xD;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
