package org.duckdns.hjow.swingwebpanel.dom;

import java.util.List;

/**
 * The selector matching service used by <code>querySelector</code>, <code>querySelectorAll</code>, <code>matches</code> and <code>closest</code>.
 * The DOM package only defines the interface; the CSS package implements it (plan item P08-09) and installs it with
 * {@link Document#setSelectorEngine(SelectorEngine)}. Until then these DOM methods throw a NotSupportedError.
 * <p>
 * <code>querySelector</code>, <code>querySelectorAll</code>, <code>matches</code>, <code>closest</code> 가 쓰는 선택자 일치 서비스.
 * DOM 패키지는 인터페이스만 정의하고 CSS 패키지가 구현하여(계획 P08-09) {@link Document#setSelectorEngine(SelectorEngine)} 로 설치한다.
 * 그때까지 이 DOM 메서드들은 NotSupportedError 를 던진다.
 */
public interface SelectorEngine {

    /**
     * Returns the first descendant of the root (in tree order) that matches the selector list, or null.
     * <p>
     * 루트의 후손 중 선택자 목록에 일치하는 첫 요소(트리 순서)를 반환한다. 없으면 null.
     *
     * @param root      node whose descendants are searched / 후손을 검색할 노드
     * @param selectors selector list text / 선택자 목록 문자열
     * @return the element or null / 요소 또는 null
     * @throws DomException SyntaxError for an invalid selector / 잘못된 선택자이면 SyntaxError
     */
    Element querySelector(Node root, String selectors);

    /**
     * Returns all matching descendants of the root in tree order as a static list.
     * <p>
     * 루트의 일치하는 모든 후손을 트리 순서의 정적 목록으로 반환한다.
     *
     * @param root      node whose descendants are searched / 후손을 검색할 노드
     * @param selectors selector list text / 선택자 목록 문자열
     * @return the elements, never null / 요소 목록(null 아님)
     * @throws DomException SyntaxError for an invalid selector / 잘못된 선택자이면 SyntaxError
     */
    List<Element> querySelectorAll(Node root, String selectors);

    /**
     * Tells whether the element matches the selector list.
     * <p>
     * 요소가 선택자 목록에 일치하는지 알려 준다.
     *
     * @param element   element to test / 검사할 요소
     * @param selectors selector list text / 선택자 목록 문자열
     * @return true if it matches / 일치하면 true
     * @throws DomException SyntaxError for an invalid selector / 잘못된 선택자이면 SyntaxError
     */
    boolean matches(Element element, String selectors);

    /**
     * Returns the element itself or its nearest ancestor that matches the selector list, or null.
     * <p>
     * 요소 자신 또는 선택자 목록에 일치하는 가장 가까운 조상을 반환한다. 없으면 null.
     *
     * @param element   start element / 시작 요소
     * @param selectors selector list text / 선택자 목록 문자열
     * @return the element or null / 요소 또는 null
     * @throws DomException SyntaxError for an invalid selector / 잘못된 선택자이면 SyntaxError
     */
    Element closest(Element element, String selectors);
}
