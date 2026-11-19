package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.css.selector.MatchContext;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Optional presentational-hint provider that consults dynamic selector state. Reusing the resolver's match context records
 * dependencies so stateChanged invalidates the computed style. Ordinary providers can keep implementing PresentationalHints.
 * <p>동적 선택자 상태를 조회하는 선택적 표현 힌트 제공자. 리졸버의 매칭 문맥을 공유하여 상태 변경 시 계산 스타일을 무효화하도록 의존성을 기록한다.
 * 일반 제공자는 기존 PresentationalHints 구현을 유지할 수 있다.
 */
public interface StatefulPresentationalHints extends PresentationalHints {
    /**
     * Returns hint declarations while recording dynamic state dependencies in the supplied context.
     * <p>전달된 문맥에 동적 상태 의존성을 기록하면서 힌트 선언을 반환한다.
     * @param element styled element / 스타일을 계산할 요소
     * @param base document base URL / 문서 기준 URL
     * @param context resolver's match context / 리졸버의 매칭 문맥
     * @return declarations or null / 선언 또는 null
     */
    String hintsFor(Element element, String base, MatchContext context);
}
