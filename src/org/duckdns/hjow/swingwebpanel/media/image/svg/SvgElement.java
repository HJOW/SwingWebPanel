package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One element of the parsed SVG document tree (plan item P18-04). The parser builds the tree from XML, the style cascade stores the
 * declared properties of the element, and the renderer walks it. Elements of other XML namespaces are kept in the tree (so that the
 * structure stays faithful) but are never rendered.
 * <p>
 * 해석된 SVG 문서 트리의 요소 하나(계획 P18-04). 파서가 XML 에서 트리를 만들고, 스타일 연쇄가 요소의 선언된 속성을 저장하며, 렌더러가 트리를
 * 순회한다. 다른 XML 네임스페이스의 요소도 구조를 그대로 유지하기 위해 트리에 두지만 그리지는 않는다.
 */
final class SvgElement {

    /**
     * The local name of the element, for example <code>rect</code>.
     * <p>
     * 요소의 지역 이름. 예: <code>rect</code>.
     */
    final String name;

    /**
     * The namespace URI of the element (empty string for no namespace).
     * <p>
     * 요소의 네임스페이스 URI(없으면 빈 문자열).
     */
    final String namespace;

    /**
     * The parent element, or null for the root.
     * <p>
     * 부모 요소. 루트이면 null.
     */
    SvgElement parent;

    /**
     * The child elements in document order.
     * <p>
     * 문서 순서의 자식 요소.
     */
    final List<SvgElement> children = new ArrayList<SvgElement>();

    /**
     * The mixed content in document order: {@link SvgElement} children and {@link String} text nodes (used by the text elements).
     * <p>
     * 문서 순서의 혼합 내용: {@link SvgElement} 자식과 {@link String} 텍스트 노드(문자 요소가 사용한다).
     */
    final List<Object> content = new ArrayList<Object>();

    /**
     * The attributes: plain names for attributes without namespace, <code>xlink:href</code> and <code>xml:space</code> for the two well
     * known namespaces, <code>{uri}name</code> for others.
     * <p>
     * 속성: 네임스페이스 없는 속성은 평범한 이름, 잘 알려진 두 네임스페이스는 <code>xlink:href</code>, <code>xml:space</code>, 그 밖은
     * <code>{uri}name</code>.
     */
    final Map<String, String> attrs = new HashMap<String, String>();

    /**
     * The declared properties after the style cascade (presentation attributes, style sheets and the style attribute), or null before the
     * cascade ran.
     * <p>
     * 스타일 연쇄 뒤의 선언된 속성(표현 속성, 스타일시트, style 속성). 연쇄 전에는 null.
     */
    Map<String, String> declared;

    /**
     * Creates an element.
     * <p>
     * 요소를 만든다.
     *
     * @param name      local name / 지역 이름
     * @param namespace namespace URI / 네임스페이스 URI
     */
    SvgElement(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Tells whether the element belongs to the SVG namespace.
     * <p>
     * 요소가 SVG 네임스페이스에 속하는지 알려 준다.
     *
     * @return true for an SVG element / SVG 요소이면 true
     */
    boolean isSvg() {
        return SvgXmlParser.SVG_NS.equals(namespace);
    }

    /**
     * Tells whether the element is an SVG element with the given local name.
     * <p>
     * 요소가 지정한 지역 이름의 SVG 요소인지 알려 준다.
     *
     * @param local local name / 지역 이름
     * @return true if it matches / 맞으면 true
     */
    boolean is(String local) {
        return isSvg() && name.equals(local);
    }

    /**
     * Returns an attribute value or null.
     * <p>
     * 속성 값을 반환하며 없으면 null 이다.
     *
     * @param key attribute name / 속성 이름
     * @return value or null / 값 또는 null
     */
    String attr(String key) {
        return attrs.get(key);
    }

    /**
     * Returns the reference of the element: the <code>href</code> attribute or, if missing, the <code>xlink:href</code> attribute.
     * <p>
     * 요소의 참조를 반환한다: <code>href</code> 속성, 없으면 <code>xlink:href</code> 속성.
     *
     * @return reference or null / 참조 또는 null
     */
    String href() {
        String h = attrs.get("href");
        return h != null ? h : attrs.get("xlink:href");
    }

    /**
     * Returns the value of the <code>id</code> attribute or null.
     * <p>
     * <code>id</code> 속성 값을 반환하며 없으면 null 이다.
     *
     * @return id or null / id 또는 null
     */
    String id() {
        return attrs.get("id");
    }

    /**
     * Returns the position of this element among its parent's child elements (0 based), or 0 for the root.
     * <p>
     * 부모의 자식 요소 중 이 요소의 위치(0부터)를 반환한다. 루트이면 0.
     *
     * @return index / 위치
     */
    int indexInParent() {
        if (parent == null) {
            return 0;
        }
        return parent.children.indexOf(this);
    }

    /**
     * Returns the text of this element and its descendants concatenated.
     * <p>
     * 이 요소와 자손의 텍스트를 이어 붙여 반환한다.
     *
     * @return text content / 텍스트 내용
     */
    String textContent() {
        StringBuilder sb = new StringBuilder();
        appendText(sb);
        return sb.toString();
    }

    /**
     * Appends the text of this element and its descendants.
     * <p>
     * 이 요소와 자손의 텍스트를 덧붙인다.
     *
     * @param sb target / 대상
     */
    private void appendText(StringBuilder sb) {
        // 혼합 내용을 순서대로 훑어 텍스트와 자식의 텍스트를 모은다.
        for (int i = 0; i < content.size(); i++) {
            Object o = content.get(i);
            if (o instanceof String) {
                sb.append((String) o);
            } else {
                ((SvgElement) o).appendText(sb);
            }
        }
    }
}
