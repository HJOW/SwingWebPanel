package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The plug-in that turns the presentational attributes of the markup (<code>bgcolor</code>, <code>align</code>, <code>width</code>, <code>border</code> ...)
 * into style declarations. The cascade treats them as author declarations with the lowest specificity that come before every author rule. The default
 * implementation is {@link HtmlPresentationalHints}; a different markup language can supply its own.
 * <p>
 * 마크업의 표현 속성(<code>bgcolor</code>, <code>align</code>, <code>width</code>, <code>border</code> 등)을 스타일 선언으로 바꾸는 플러그인. 캐스케이드는 이들을 모든 작성자
 * 규칙보다 앞서는, 구체성이 가장 낮은 작성자 선언으로 다룬다. 기본 구현은 {@link HtmlPresentationalHints} 이며 다른 마크업 언어는 자체 구현을 제공할 수 있다.
 */
public interface PresentationalHints {

    /**
     * Returns the declarations an element's presentational attributes stand for.
     * <p>
     * 요소의 표현 속성이 뜻하는 선언을 반환한다.
     *
     * @param element the element / 요소
     * @param base    the document base URL for relative urls, or null / 상대 url 의 문서 기준 URL 또는 null
     * @return declaration text such as <code>background-color: red; width: 100px</code>, or null if there is none / <code>background-color: red; width: 100px</code> 같은 선언 문자열. 없으면 null
     */
    String hintsFor(Element element, String base);
}
