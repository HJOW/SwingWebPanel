package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Parsing and serializing of markup, provided by the HTML (or XML) layer. The DOM methods <code>innerHTML</code>, <code>outerHTML</code> and
 * <code>insertAdjacentHTML</code> delegate to it (plan items P07-06 and P20-04). Until the HTML layer installs an implementation with
 * {@link Document#setMarkupSupport(MarkupSupport)}, these DOM methods throw a NotSupportedError.
 * <p>
 * HTML(또는 XML) 계층이 제공하는 마크업 해석과 직렬화. DOM 메서드 <code>innerHTML</code>, <code>outerHTML</code>, <code>insertAdjacentHTML</code> 이
 * 여기에 위임한다(계획 P07-06, P20-04). HTML 계층이 {@link Document#setMarkupSupport(MarkupSupport)} 로 구현을 설치하기 전에는 이 DOM 메서드들이
 * NotSupportedError 를 던진다.
 */
public interface MarkupSupport {

    /**
     * Serializes the children of a node (innerHTML) or the node itself (outerHTML) according to the fragment serialization algorithm.
     * <p>
     * 노드의 자식(innerHTML) 또는 노드 자신(outerHTML)을 조각 직렬화 알고리즘에 따라 직렬화한다.
     *
     * @param node  node to serialize / 직렬화할 노드
     * @param outer true to include the node itself / 노드 자신을 포함하려면 true
     * @return markup / 마크업
     */
    String serialize(Node node, boolean outer);

    /**
     * Parses markup as a fragment in the context of an element and replaces the children of that element.
     * <p>
     * 마크업을 요소를 문맥으로 하는 조각으로 해석하여 그 요소의 자식을 교체한다.
     *
     * @param context element whose children are replaced / 자식을 교체할 요소
     * @param markup  markup text / 마크업 문자열
     */
    void setInnerHtml(Element context, String markup);

    /**
     * Parses markup as a fragment in the context of an element and returns the nodes without inserting them.
     * <p>
     * 마크업을 요소를 문맥으로 하는 조각으로 해석하고 삽입하지 않은 채 노드를 돌려준다.
     *
     * @param context element that gives the parsing context / 해석 문맥을 주는 요소
     * @param markup  markup text / 마크업 문자열
     * @return fragment holding the parsed nodes / 해석된 노드를 가진 조각
     */
    DocumentFragment parseFragment(Element context, String markup);
}
