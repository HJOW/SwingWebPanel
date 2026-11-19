package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Turns the style that the cascade computed for an element into the style that the element has at the moment (plan item P10-07,
 * CSS Transitions Level 1 and CSS Animations Level 1): the {@link StyleResolver} calls it every time it computes the style of an
 * element, before the style is cached and given to the children for inheritance. An implementation compares the style with the
 * previous one to start transitions and returns the style with the values of the running transitions and animations.
 * <p>
 * 캐스케이드가 요소에 대해 계산한 스타일을 요소가 지금 가진 스타일로 바꾼다(계획 P10-07, CSS Transitions Level 1, CSS Animations Level 1):
 * {@link StyleResolver} 가 요소의 스타일을 계산할 때마다, 스타일을 캐시하고 자식이 상속하기 전에 부른다. 구현은 스타일을 이전 것과 비교해 전환을 시작하고
 * 실행 중인 전환과 애니메이션의 값을 가진 스타일을 돌려준다.
 */
public interface StyleAnimator {

    /**
     * Returns the style of an element at the moment.
     * <p>
     * 요소의 지금 스타일을 반환한다.
     *
     * @param element  the element / 요소
     * @param computed the style that the cascade computed (without animations) / 캐스케이드가 계산한 스타일(애니메이션 없이)
     * @return the style with the animated values; <code>computed</code> itself if nothing is animated / 애니메이션 값을 가진 스타일. 애니메이션이 없으면 <code>computed</code> 자신
     */
    ComputedStyle animate(Element element, ComputedStyle computed);

    /**
     * Returns the style of a pseudo-element of an element at the moment (transitions of <code>::before</code>, <code>::after</code> ...).
     * <p>
     * 요소의 의사 요소의 지금 스타일을 반환한다(<code>::before</code>, <code>::after</code> 등의 전환).
     *
     * @param element  the originating element / 원본 요소
     * @param pseudo   the pseudo-element name without colons / 콜론 없는 의사 요소 이름
     * @param computed the style that the cascade computed / 캐스케이드가 계산한 스타일
     * @return the style with the animated values; <code>computed</code> itself if nothing is animated / 애니메이션 값을 가진 스타일. 없으면 <code>computed</code> 자신
     */
    ComputedStyle animatePseudo(Element element, String pseudo, ComputedStyle computed);
}
