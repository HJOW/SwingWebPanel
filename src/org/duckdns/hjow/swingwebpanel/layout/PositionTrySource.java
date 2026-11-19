package org.duckdns.hjow.swingwebpanel.layout;

import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * A {@link StyleSource} that knows the position options that the style sheets define with <code>@position-try</code> (CSS Anchor Positioning Level 1 section 6.4), so that
 * <code>position-try-fallbacks</code> can use them when an anchor-positioned box overflows.
 * <p>
 * 스타일 시트가 <code>@position-try</code>(CSS Anchor Positioning Level 1 6.4 절)로 정의한 위치 대안을 아는 {@link StyleSource}. 앵커 위치 상자가 넘칠 때
 * <code>position-try-fallbacks</code> 가 그것을 쓸 수 있게 한다.
 */
public interface PositionTrySource {

    /**
     * Returns the declarations of the <code>@position-try</code> rule of a name as computed values for an element: the longhands that the rule sets, by property id
     * (a logical property appears as the physical property of the writing mode of the element). The last rule of the name wins.
     * <p>
     * 이름의 <code>@position-try</code> 규칙의 선언을 요소의 계산 값으로 반환한다: 규칙이 정하는 개별 속성을 속성 id 별로 담는다(논리 속성은 요소의 쓰기 모드에 따른 물리 속성으로
     * 나타난다). 이름이 같으면 마지막 규칙이 이긴다.
     *
     * @param name    the dashed identifier of the option / 대안의 대시 식별자
     * @param element the element of the box that uses the option / 대안을 쓰는 상자의 요소
     * @param own     the computed style of that element / 그 요소의 계산 스타일
     * @return the computed values by property id, or null if there is no rule of that name / 속성 id 별 계산 값. 그 이름의 규칙이 없으면 null
     */
    Map<Integer, CssValue> positionTryOption(String name, Element element, ComputedStyle own);
}
