package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Supplies the computed styles that the layout engine needs. The CSS style resolver implements it (or is adapted to it); tests may supply
 * fixed styles. Both methods are called on the thread that runs the layout and must return immutable {@link ComputedStyle} objects.
 * <p>
 * 레이아웃 엔진이 필요로 하는 계산 스타일을 제공한다. CSS 스타일 리졸버가 구현하거나 어댑터로 연결하며 시험은 고정 스타일을 줄 수 있다. 두 메서드는
 * 레이아웃을 실행하는 스레드에서 호출되며 불변 {@link ComputedStyle} 을 반환해야 한다.
 */
public interface StyleSource {

    /**
     * Returns the computed style of an element.
     * <p>
     * 요소의 계산 스타일을 반환한다.
     *
     * @param element the element / 요소
     * @return its style, never null / 스타일(null 이 아님)
     */
    ComputedStyle getStyle(Element element);

    /**
     * Returns the computed style of a generated pseudo-element (<code>before</code> or <code>after</code>).
     * <p>
     * 생성되는 의사 요소(<code>before</code> 또는 <code>after</code>)의 계산 스타일을 반환한다.
     *
     * @param element the originating element / 기원 요소
     * @param pseudo  <code>before</code> or <code>after</code> / <code>before</code> 또는 <code>after</code>
     * @return the style, or null if no rule generates the pseudo-element / 스타일. 규칙이 의사 요소를 만들지 않으면 null
     */
    ComputedStyle getPseudoStyle(Element element, String pseudo);

    /**
     * Returns the style of the custom highlight pseudo-element <code>::highlight(name)</code> of an element (CSS Custom Highlight API 1): the highlight
     * properties (<code>color</code>, <code>background-color</code>, <code>text-decoration</code>...) inherit from the highlight of the parent element when no
     * rule sets them, the other properties are those of the element; <code>color</code> may be the keyword <code>currentcolor</code> (the color of the text).
     * <p>
     * 요소의 사용자 정의 하이라이트 의사 요소 <code>::highlight(이름)</code> 의 스타일을 반환한다(CSS Custom Highlight API 1): 규칙이 정하지 않은 하이라이트 속성(<code>color</code>,
     * <code>background-color</code>, <code>text-decoration</code> 등)은 부모 요소의 하이라이트에서 물려받고 나머지 속성은 요소의 것이다. <code>color</code> 는 키워드
     * <code>currentcolor</code>(텍스트의 색)일 수 있다.
     *
     * @param element the originating element / 원본 요소
     * @param name    the custom highlight name / 사용자 정의 하이라이트 이름
     * @return the style, never null / 스타일(null 이 아님)
     */
    ComputedStyle getHighlightStyle(Element element, String name);

    /**
     * Returns the properties that the rules of a pseudo-element declare (the renderer of a part that it draws itself, such as the button of a file control,
     * keeps its own look for the others).
     * <p>
     * 의사 요소의 규칙이 선언하는 속성을 반환한다(파일 컨트롤의 단추처럼 렌더러가 직접 그리는 부분은 나머지 속성에 대해 자신의 모양을 지킨다).
     *
     * @param element the originating element / 원본 요소
     * @param pseudo  the pseudo-element name / 의사 요소 이름
     * @return the property ids, never null / 속성 id(null 이 아님)
     */
    java.util.BitSet getPseudoDeclared(Element element, String pseudo);
}
