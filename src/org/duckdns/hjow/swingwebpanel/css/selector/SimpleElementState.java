package org.duckdns.hjow.swingwebpanel.css.selector;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * A small mutable {@link ElementState}: the host sets which element is hovered, active, focused and which one is the fragment target. It
 * answers <code>:focus-within</code> from the focused element's ancestry and leaves everything else <code>UNKNOWN</code>. The element
 * references are held strongly, so clear them (or drop this object) when the page is replaced.
 * <p>
 * 작은 가변 {@link ElementState}: 호스트가 마우스가 올라간 요소, 활성 요소, 포커스 요소, 프래그먼트 대상 요소를 설정한다. <code>:focus-within</code> 은
 * 포커스 요소의 조상으로 답하고 나머지는 <code>UNKNOWN</code> 으로 둔다. 요소 참조를 강하게 쥐므로 페이지를 바꿀 때 지우거나 이 객체를 버려야 한다.
 */
public final class SimpleElementState implements ElementState {

    /** Hovered element. / 마우스가 올라간 요소. */
    private volatile Element hover;

    /** Active element. / 활성 요소. */
    private volatile Element active;

    /** Focused element. / 포커스를 가진 요소. */
    private volatile Element focus;

    /** Whether the focus should show a focus ring (<code>:focus-visible</code>). / 포커스 링을 보여야 하는지(<code>:focus-visible</code>). */
    private volatile boolean focusVisible = true;

    /** Fragment target element. / 프래그먼트 대상 요소. */
    private volatile Element target;

    /**
     * Sets the hovered element; its ancestors are hovered too.
     * <p>
     * 마우스가 올라간 요소를 설정한다. 그 조상도 hover 상태이다.
     *
     * @param e element or null / 요소 또는 null
     */
    public void setHover(Element e) {
        hover = e;
    }

    /**
     * Sets the active element; its ancestors are active too.
     * <p>
     * 활성 요소를 설정한다. 그 조상도 active 상태이다.
     *
     * @param e element or null / 요소 또는 null
     */
    public void setActive(Element e) {
        active = e;
    }

    /**
     * Sets the focused element.
     * <p>
     * 포커스를 가진 요소를 설정한다.
     *
     * @param e element or null / 요소 또는 null
     */
    public void setFocus(Element e) {
        focus = e;
    }

    /**
     * Sets whether the focus is keyboard-like (<code>:focus-visible</code>).
     * <p>
     * 포커스가 키보드 방식인지(<code>:focus-visible</code>) 설정한다.
     *
     * @param visible true to match :focus-visible / :focus-visible 과 일치하게 하려면 true
     */
    public void setFocusVisible(boolean visible) {
        focusVisible = visible;
    }

    /**
     * Sets the fragment target element.
     * <p>
     * 프래그먼트 대상 요소를 설정한다.
     *
     * @param e element or null / 요소 또는 null
     */
    public void setTarget(Element e) {
        target = e;
    }

    /**
     * Tells whether an element is the given element or one of its ancestors.
     * <p>
     * 요소가 주어진 요소이거나 그 조상인지 알려 준다.
     *
     * @param e     candidate / 후보
     * @param leaf  the deepest element or null / 가장 깊은 요소 또는 null
     * @return true if e is leaf or contains it / e 가 leaf 이거나 leaf 를 포함하면 true
     */
    private static boolean isInclusiveAncestor(Element e, Element leaf) {
        Node n = leaf;
        // 가장 깊은 요소에서 위로 올라가며 e 를 찾는다.
        while (n != null) {
            if (n == e) {
                return true;
            }
            n = n.getParentNode();
        }
        return false;
    }

    /**
     * Answers a pseudo-class.
     * <p>
     * 의사 클래스에 답한다.
     *
     * @param element       the element / 요소
     * @param pseudoClassId pseudo-class id / 의사 클래스 id
     * @return TRUE, FALSE or UNKNOWN / TRUE, FALSE 또는 UNKNOWN
     */
    public int get(Element element, int pseudoClassId) {
        switch (pseudoClassId) {
        case PseudoClasses.HOVER:
            return isInclusiveAncestor(element, hover) ? TRUE : FALSE;
        case PseudoClasses.ACTIVE:
            return isInclusiveAncestor(element, active) ? TRUE : FALSE;
        case PseudoClasses.FOCUS:
            return element == focus ? TRUE : FALSE;
        case PseudoClasses.FOCUS_VISIBLE:
            return element == focus && focusVisible ? TRUE : FALSE;
        case PseudoClasses.FOCUS_WITHIN:
            return isInclusiveAncestor(element, focus) ? TRUE : FALSE;
        case PseudoClasses.TARGET:
            return element == target ? TRUE : FALSE;
        default:
            return UNKNOWN;
        }
    }
}
