package org.duckdns.hjow.swingwebpanel.dom.edit;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * Which parts of a document the user can edit (HTML Standard, "Editing", 6.8): the state of the <code>contenteditable</code> attribute,
 * the editing hosts, and the editable nodes. An <em>editing host</em> is an HTML element whose <code>contenteditable</code> is in the
 * true or the plaintext-only state, or, when the document is in design mode, the root element of the document; a node is
 * <em>editable</em> when it is inside an editing host and no element on the way says <code>contenteditable="false"</code>. The class
 * has only static methods and reads the attributes of the DOM, so it works without layout.
 * <p>
 * 사용자가 문서의 어느 부분을 편집할 수 있는지(HTML Standard "Editing" 6.8): <code>contenteditable</code> 속성의 상태, 편집 호스트(editing host), 편집
 * 가능한 노드. <em>편집 호스트</em>는 <code>contenteditable</code> 가 true 나 plaintext-only 상태인 HTML 요소이거나, 문서가 디자인 모드이면 문서의 루트
 * 요소이다. 노드는 편집 호스트 안에 있고 그 사이의 어느 요소도 <code>contenteditable="false"</code> 가 아닐 때 <em>편집 가능</em>하다. 정적 메서드만 있고
 * DOM 의 속성을 읽으므로 레이아웃 없이 동작한다.
 */
public final class EditingHosts {

    /**
     * The state "true" of the <code>contenteditable</code> attribute.
     * <p>
     * <code>contenteditable</code> 속성의 true 상태.
     */
    public static final String TRUE = "true";

    /**
     * The state "false".
     * <p>
     * false 상태.
     */
    public static final String FALSE = "false";

    /**
     * The state "plaintext-only".
     * <p>
     * plaintext-only 상태.
     */
    public static final String PLAINTEXT_ONLY = "plaintext-only";

    /**
     * The state "inherit" (the attribute is missing or has an invalid value).
     * <p>
     * inherit 상태(속성이 없거나 값이 올바르지 않다).
     */
    public static final String INHERIT = "inherit";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EditingHosts() {
    }

    /**
     * Returns the state of the <code>contenteditable</code> attribute of an element: the keywords <code>true</code>, <code>false</code> and
     * <code>plaintext-only</code> (any letter case; the empty string is <code>true</code>), and <code>inherit</code> for a missing or invalid
     * value.
     * <p>
     * 요소의 <code>contenteditable</code> 속성 상태를 반환한다: 키워드 <code>true</code>, <code>false</code>, <code>plaintext-only</code>(대소문자 무관.
     * 빈 문자열은 <code>true</code>), 속성이 없거나 올바르지 않은 값이면 <code>inherit</code>.
     *
     * @param e the element / 요소
     * @return one of {@link #TRUE}, {@link #FALSE}, {@link #PLAINTEXT_ONLY}, {@link #INHERIT} / 상태
     */
    public static String state(Element e) {
        String v = e.getAttribute("contenteditable");
        if (v == null) {
            return INHERIT;
        }
        String s = v.trim().toLowerCase(java.util.Locale.ENGLISH);
        if (s.length() == 0 || TRUE.equals(s)) {
            return TRUE;
        }
        if (FALSE.equals(s)) {
            return FALSE;
        }
        return PLAINTEXT_ONLY.equals(s) ? PLAINTEXT_ONLY : INHERIT;
    }

    /**
     * Tells whether an element is an editing host: an HTML element whose <code>contenteditable</code> is true or plaintext-only, or the
     * root element of a document in design mode.
     * <p>
     * 요소가 편집 호스트인지 알려 준다: <code>contenteditable</code> 가 true 나 plaintext-only 인 HTML 요소, 또는 디자인 모드 문서의 루트 요소.
     *
     * @param e the element, may be null / 요소. null 가능
     * @return true for an editing host / 편집 호스트이면 true
     */
    public static boolean isEditingHost(Element e) {
        if (e == null || !Namespaces.HTML.equals(e.getNamespaceURI())) {
            return false;
        }
        String s = state(e);
        if (TRUE.equals(s) || PLAINTEXT_ONLY.equals(s)) {
            return true;
        }
        Document doc = e.getOwnerDocument();
        return doc != null && doc.isDesignMode() && e == doc.getDocumentElement() && !FALSE.equals(s);
    }

    /**
     * Tells whether a node is editable (inside an editing host, or an editing host itself, and not switched off by a
     * <code>contenteditable="false"</code> on the way up). A text node is editable when its parent is.
     * <p>
     * 노드가 편집 가능한지 알려 준다(편집 호스트 안이거나 편집 호스트 자신이며 위로 가는 길에 <code>contenteditable="false"</code> 로 꺼져 있지 않다).
     * 텍스트 노드는 부모가 편집 가능하면 편집 가능하다.
     *
     * @param n the node, may be null / 노드. null 가능
     * @return true if editable / 편집 가능하면 true
     */
    public static boolean isEditable(Node n) {
        return hostOf(n) != null;
    }

    /**
     * Returns the editing host of a node: the outermost editing host that contains the node (or is the node) without a
     * <code>contenteditable="false"</code> element between them, or null if the node is not editable. A host inside another editable
     * region belongs to that outer region, as in the browsers.
     * <p>
     * 노드의 편집 호스트를 반환한다: 노드를 포함하는(또는 노드 자신인) 가장 바깥의 편집 호스트이며 둘 사이에 <code>contenteditable="false"</code> 요소가 없다.
     * 노드가 편집 가능하지 않으면 null. 편집 가능한 영역 안의 호스트는 브라우저처럼 그 바깥 영역에 속한다.
     *
     * @param n the node, may be null / 노드. null 가능
     * @return the editing host or null / 편집 호스트. 없으면 null
     */
    public static Element hostOf(Node n) {
        Element found = null;
        // 위로 올라가며 contenteditable=false 를 만나기 전까지 지나는 편집 호스트 중 가장 바깥의 것을 기억한다.
        for (Node c = n; c != null; c = c.getParentNode()) {
            if (!(c instanceof Element)) {
                continue;
            }
            Element e = (Element) c;
            if (FALSE.equals(state(e))) {
                // false 요소 자신이 시작점이거나 그 안이면 편집할 수 없고, 호스트를 찾은 뒤 만났으면 거기서 멈춘다.
                return found;
            }
            if (isEditingHost(e)) {
                found = e;
            }
        }
        return found;
    }

    /**
     * Tells whether an editing host accepts only plain text (the plaintext-only state): the editor then inserts no markup.
     * <p>
     * 편집 호스트가 일반 텍스트만 받는지 알려 준다(plaintext-only 상태). 이때 편집기는 마크업을 넣지 않는다.
     *
     * @param host the editing host / 편집 호스트
     * @return true for plaintext-only / plaintext-only 이면 true
     */
    public static boolean isPlainTextOnly(Element host) {
        return host != null && PLAINTEXT_ONLY.equals(state(host));
    }
}
