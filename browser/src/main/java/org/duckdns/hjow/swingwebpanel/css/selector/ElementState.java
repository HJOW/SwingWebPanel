package org.duckdns.hjow.swingwebpanel.css.selector;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The plug-in through which the selector matcher learns the dynamic state of elements: pointer and focus state, the fragment target, form
 * control state that lives outside the attributes (the user typed into a field, a checkbox was clicked, a script changed a property) and
 * validity. The matcher asks for one pseudo-class at a time; <code>UNKNOWN</code> means "not tracked here", and the matcher then falls back to
 * what the attributes of the element say. The input layer (plan item P11) supplies the real implementation.
 * <p>
 * 선택자 매처가 요소의 동적 상태를 알아내는 플러그인: 포인터와 포커스 상태, 프래그먼트 대상, 속성 밖에 있는 폼 컨트롤 상태(사용자가 입력, 체크박스 클릭,
 * 스크립트가 프로퍼티 변경), 유효성. 매처는 의사 클래스를 하나씩 묻는다. <code>UNKNOWN</code> 은 "여기서는 추적하지 않음"이며 이때 매처는 요소의
 * 속성이 말해 주는 값으로 되돌아간다. 입력 계층(계획 P11)이 실제 구현을 제공한다.
 */
public interface ElementState {

    /** Answer: the pseudo-class matches. / 응답: 의사 클래스가 일치한다. */
    int TRUE = 1;

    /** Answer: the pseudo-class does not match. / 응답: 의사 클래스가 일치하지 않는다. */
    int FALSE = 0;

    /** Answer: this state is not tracked here. / 응답: 이 상태는 여기서 추적하지 않는다. */
    int UNKNOWN = -1;

    /**
     * Returns the state of a pseudo-class for an element.
     * <p>
     * 요소에 대한 의사 클래스의 상태를 반환한다.
     *
     * @param element       the element / 요소
     * @param pseudoClassId one of the ids of {@link PseudoClasses} (HOVER, ACTIVE, FOCUS, FOCUS_VISIBLE, FOCUS_WITHIN, TARGET, VISITED, CHECKED,
     *                      INDETERMINATE, VALID, INVALID, IN_RANGE, OUT_OF_RANGE, PLACEHOLDER_SHOWN) / {@link PseudoClasses} 의 id 중 하나
     * @return {@link #TRUE}, {@link #FALSE} or {@link #UNKNOWN} / TRUE, FALSE 또는 UNKNOWN
     */
    int get(Element element, int pseudoClassId);
}
