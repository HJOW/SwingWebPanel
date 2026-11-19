package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;

/**
 * The structure dependent style rules of quirks mode (plan item P07-07-b-10): the "margin collapsing quirks" of HTML5 2014 section 10.3.10.
 * In quirks mode an element with default margins (<code>blockquote</code>, <code>dir</code>, <code>dl</code>, <code>h1</code> to
 * <code>h6</code>, <code>listing</code>, <code>multicol</code>, <code>ol</code>, <code>p</code>, <code>plaintext</code>, <code>pre</code>,
 * <code>ul</code>, <code>xmp</code>) that is a child of <code>body</code>, <code>td</code> or <code>th</code> loses the top margin when no
 * substantial sibling precedes it (and the bottom margin too when it is blank), loses the top margin when it is blank in a cell and nothing
 * substantial follows, and a <code>p</code> at the end of a cell loses its bottom margin. A node is substantial if it is an element or a
 * text node that is not inter-element white space; an element is blank if it contains no substantial node. The rules are "user-agent level
 * style sheet" rules in the standard; here they are emitted with the presentational hints, which sit above the user-agent sheet and below
 * every author rule, so author CSS still wins. {@link StyleResolver} drops the cached styles of the affected elements when a child or a text
 * of such a parent changes. Source: https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#margin-collapsing-quirks (retrieved 2026-11-17).
 * <p>
 * 호환 모드의 구조에 따른 스타일 규칙(계획 P07-07-b-10): HTML5 2014 §10.3.10 의 "여백 병합 quirk". 호환 모드에서 기본 여백이 있는 요소(<code>blockquote</code>,
 * <code>dir</code>, <code>dl</code>, <code>h1</code>~<code>h6</code>, <code>listing</code>, <code>multicol</code>, <code>ol</code>, <code>p</code>,
 * <code>plaintext</code>, <code>pre</code>, <code>ul</code>, <code>xmp</code>)가 <code>body</code>, <code>td</code>, <code>th</code> 의 자식이면, 앞에 실질적인 형제가 없을 때
 * 위쪽 margin 이 없어지고(비어 있으면 아래쪽도), 셀 안에서 비어 있고 뒤에 실질적인 것이 없으면 위쪽 margin 이 없어지며, 셀의 끝에 있는 <code>p</code> 는 아래쪽
 * margin 이 없어진다. 노드가 실질적이라는 것은 요소이거나 요소 사이 공백이 아닌 텍스트 노드라는 뜻이고, 요소가 비었다는 것은 실질적인 노드가 없다는 뜻이다. 표준에서는
 * "사용자 에이전트 수준 스타일 시트" 규칙이지만 여기서는 사용자 에이전트 시트 위, 모든 작성자 규칙 아래에 있는 표현 힌트와 함께 낸다(작성자 CSS 가 이긴다).
 * 이런 부모의 자식이나 텍스트가 바뀌면 {@link StyleResolver} 가 영향받는 요소의 캐시된 스타일을 버린다.
 */
final class QuirkHints {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private QuirkHints() {
    }

    /**
     * Tells whether an element is one of the elements with default margins of the rendering section.
     * <p>
     * 요소가 렌더링 절이 말하는 기본 여백이 있는 요소인지 알려 준다.
     *
     * @param n the lower-case local name / 소문자 지역 이름
     * @return true for blockquote, dir, dl, h1 to h6, listing, multicol, ol, p, plaintext, pre, ul and xmp / 해당하면 true
     */
    static boolean hasDefaultMargins(String n) {
        return n.equals("p") || n.equals("blockquote") || n.equals("dir") || n.equals("dl") || n.equals("h1") || n.equals("h2")
                || n.equals("h3") || n.equals("h4") || n.equals("h5") || n.equals("h6") || n.equals("listing") || n.equals("multicol")
                || n.equals("ol") || n.equals("plaintext") || n.equals("pre") || n.equals("ul") || n.equals("xmp");
    }

    /**
     * Tells whether a parent element can make the margin quirks of its children apply.
     * <p>
     * 부모 요소가 자식에게 여백 quirk 를 적용하게 할 수 있는지 알려 준다.
     *
     * @param e the element or null / 요소 또는 null
     * @return true for <code>body</code>, <code>td</code> and <code>th</code> / <code>body</code>, <code>td</code>, <code>th</code> 이면 true
     */
    static boolean isQuirkParent(Element e) {
        return LegacyAttributes.is(e, "body") || LegacyAttributes.is(e, "td") || LegacyAttributes.is(e, "th");
    }

    /**
     * Builds the margin declarations of an element in a quirks mode document, or null.
     * <p>
     * 호환 모드 문서의 요소에 대한 여백 선언을 만든다. 없으면 null.
     *
     * @param e the element / 요소
     * @param n its lower-case local name / 소문자 지역 이름
     * @return declaration text or null / 선언 문자열 또는 null
     */
    static String margins(Element e, String n) {
        Document d = e.getOwnerDocument();
        if (d == null || d.getCompatibilityMode() != Document.QUIRKS_MODE || !hasDefaultMargins(n)) {
            return null;
        }
        Element parent = e.getParentElement();
        if (!isQuirkParent(parent)) {
            return null;
        }
        boolean cell = !LegacyAttributes.is(parent, "body");
        boolean noPrevious = !hasSubstantial(e, true);
        boolean blank = isBlank(e);
        boolean noFollowing = !hasSubstantial(e, false);
        StringBuilder sb = new StringBuilder();
        // 앞에 실질적인 형제가 없으면 위쪽 margin 이 없고, 비어 있으면 아래쪽도 없다.
        if (noPrevious) {
            LegacyAttributes.add(sb, "margin-top", "0");
            if (blank) {
                LegacyAttributes.add(sb, "margin-bottom", "0");
            }
        }
        // 셀 안에서 비어 있고 뒤에 실질적인 것이 없으면 위쪽 margin 이 없다.
        if (cell && noFollowing && blank) {
            LegacyAttributes.add(sb, "margin-top", "0");
        }
        // 셀의 끝에 있는 p 는 아래쪽 margin 이 없다.
        if (cell && noFollowing && n.equals("p")) {
            LegacyAttributes.add(sb, "margin-bottom", "0");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Tells whether an element has a substantial sibling before or after it.
     * <p>
     * 요소의 앞이나 뒤에 실질적인 형제가 있는지 알려 준다.
     *
     * @param e        the element / 요소
     * @param previous true to look at the preceding siblings, false for the following ones / true 이면 앞의 형제, false 이면 뒤의 형제
     * @return true if one of them is substantial / 하나라도 실질적이면 true
     */
    private static boolean hasSubstantial(Element e, boolean previous) {
        // 형제를 가까운 쪽부터 훑는다.
        for (Node s = previous ? e.getPreviousSibling() : e.getNextSibling(); s != null; s = previous ? s.getPreviousSibling() : s.getNextSibling()) {
            if (isSubstantial(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether an element contains no substantial node (it is "blank").
     * <p>
     * 요소가 실질적인 노드를 담지 않는지("blank") 알려 준다.
     *
     * @param e the element / 요소
     * @return true if blank / blank 이면 true
     */
    private static boolean isBlank(Element e) {
        // 자식을 훑어 실질적인 것이 있는지 본다.
        for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (isSubstantial(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a node is substantial: an element, or a text node that is not inter-element white space.
     * <p>
     * 노드가 실질적인지 알려 준다: 요소이거나 요소 사이 공백이 아닌 텍스트 노드.
     *
     * @param n the node / 노드
     * @return true if substantial / 실질적이면 true
     */
    private static boolean isSubstantial(Node n) {
        if (n instanceof Element) {
            return true;
        }
        if (!(n instanceof Text)) {
            return false;
        }
        String s = n.getTextContent();
        // 텍스트에 HTML 공백이 아닌 글자가 하나라도 있으면 실질적이다.
        for (int i = 0; s != null && i < s.length(); i++) {
            if (!Namespaces.isAsciiWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
