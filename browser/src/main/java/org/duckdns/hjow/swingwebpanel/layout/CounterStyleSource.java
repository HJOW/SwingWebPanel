package org.duckdns.hjow.swingwebpanel.layout;

import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule;

/**
 * A {@link StyleSource} that knows the counter styles the style sheets define with <code>@counter-style</code> (CSS Counter Styles Level 3), so that
 * <code>list-style-type</code> and <code>counter()</code> can use them.
 * <p>
 * 스타일 시트가 <code>@counter-style</code>(CSS Counter Styles Level 3)로 정의한 카운터 스타일을 아는 {@link StyleSource}. <code>list-style-type</code> 과
 * <code>counter()</code> 가 그것을 쓸 수 있게 한다.
 */
public interface CounterStyleSource {

    /**
     * Returns the counter styles defined by the style sheets.
     * <p>
     * 스타일 시트가 정의한 카운터 스타일을 반환한다.
     *
     * @return the definitions by name / 이름별 정의
     */
    Map<String, CounterStyleRule> getCounterStyleRules();
}
