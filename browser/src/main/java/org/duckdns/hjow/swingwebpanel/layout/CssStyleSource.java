package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * A {@link StyleSource} on top of the CSS engine: computed styles come from the cascade of {@link CssEngine} (user-agent, author and
 * inline styles, media queries, <code>::before</code>/<code>::after</code> rules).
 * <p>
 * CSS 엔진 위의 {@link StyleSource}: 계산 스타일은 {@link CssEngine} 의 캐스케이드(사용자 에이전트, 작성자, 인라인 스타일, 미디어 질의,
 * <code>::before</code>/<code>::after</code> 규칙)에서 온다.
 */
public final class CssStyleSource implements StyleSource, ContainerSource, CounterStyleSource, PositionTrySource {

    /** The CSS engine of the document. 문서의 CSS 엔진. */
    private final CssEngine engine;

    /**
     * Creates the adapter.
     * <p>
     * 어댑터를 만든다.
     *
     * @param engine the CSS engine, not null / CSS 엔진(null 불가)
     */
    public CssStyleSource(CssEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }
        this.engine = engine;
    }

    /**
     * Returns the computed style of an element.
     * <p>
     * 요소의 계산 스타일을 반환한다.
     *
     * @param element the element / 요소
     * @return the style / 스타일
     */
    public ComputedStyle getStyle(Element element) {
        return engine.getComputedStyle(element);
    }

    /**
     * Returns the style of a generated pseudo-element, or null if no rule generates it.
     * <p>
     * 생성되는 의사 요소의 스타일을 반환한다. 규칙이 만들지 않으면 null.
     *
     * @param element the originating element / 기원 요소
     * @param pseudo  <code>before</code> or <code>after</code> / <code>before</code> 또는 <code>after</code>
     * @return the style or null / 스타일 또는 null
     */
    public ComputedStyle getPseudoStyle(Element element, String pseudo) {
        return engine.getResolver().getPseudoStyle(element, pseudo);
    }

    /**
     * Returns the style of the custom highlight pseudo-element <code>::highlight(name)</code> of an element.
     * <p>
     * 요소의 사용자 정의 하이라이트 의사 요소 <code>::highlight(이름)</code> 의 스타일을 반환한다.
     *
     * @param element the originating element / 원본 요소
     * @param name    the custom highlight name / 사용자 정의 하이라이트 이름
     * @return the style / 스타일
     */
    public ComputedStyle getHighlightStyle(Element element, String name) {
        return engine.getResolver().getHighlightStyle(element, name);
    }

    /**
     * Returns the properties that the rules of a pseudo-element declare.
     * <p>
     * 의사 요소의 규칙이 선언하는 속성을 반환한다.
     *
     * @param element the originating element / 원본 요소
     * @param pseudo  the pseudo-element name / 의사 요소 이름
     * @return the property ids / 속성 id
     */
    public java.util.BitSet getPseudoDeclared(Element element, String pseudo) {
        return engine.getResolver().getPseudoDeclared(element, pseudo);
    }

    /**
     * Returns the <code>@font-feature-values</code> rules of the style sheets in document order.
     * <p>
     * 스타일 시트의 <code>@font-feature-values</code> 규칙을 문서 순서로 반환한다.
     *
     * @return the rules / 규칙
     */
    public java.util.List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> getFontFeatureValues() {
        return engine.getResolver().getFontFeatureValues();
    }

    /**
     * Returns the counter styles that the style sheets define.
     * <p>
     * 스타일 시트가 정의한 카운터 스타일을 반환한다.
     *
     * @return the definitions by name / 이름별 정의
     */
    public java.util.Map<String, org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule> getCounterStyleRules() {
        return engine.getResolver().getCounterStyleRules();
    }

    /**
     * Returns the declarations of the <code>@position-try</code> rule of a name as computed values for an element.
     * <p>
     * 이름의 <code>@position-try</code> 규칙의 선언을 요소의 계산 값으로 반환한다.
     *
     * @param name    the dashed identifier / 대시 식별자
     * @param element the element / 요소
     * @param own     the computed style of the element / 요소의 계산 스타일
     * @return the computed values by property id, or null if there is no such rule / 속성 id 별 계산 값. 그런 규칙이 없으면 null
     */
    public java.util.Map<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue> positionTryOption(String name, Element element, ComputedStyle own) {
        org.duckdns.hjow.swingwebpanel.css.cssom.PositionTryRule rule = engine.getResolver().getPositionTryRule(name);
        if (rule == null) {
            return null;
        }
        java.util.Map<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue> declared = rule.getStyle().normal();
        java.util.Map<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue> result = new java.util.LinkedHashMap<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue>();
        if (declared.isEmpty() || element == null) {
            return result;
        }
        ComputedStyle c = engine.getResolver().computeDeclared(element, new java.util.LinkedHashMap<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue>(declared), own);
        String wm = own == null || own.keyword(Prop.WRITING_MODE) == null ? "horizontal-tb" : own.keyword(Prop.WRITING_MODE);
        String dr = own == null || own.keyword(Prop.DIRECTION) == null ? "ltr" : own.keyword(Prop.DIRECTION);
        // 선언한 속성마다 계산한 값을 모은다(논리 속성은 쓰기 모드가 가리키는 물리 속성에 있다).
        for (java.util.Map.Entry<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue> e : declared.entrySet()) {
            int id = org.duckdns.hjow.swingwebpanel.css.props.LogicalProps.physical(e.getKey().intValue(), wm, dr);
            result.put(Integer.valueOf(id), c.get(id));
        }
        return result;
    }

    /**
     * Tells whether the style sheets contain <code>@container</code> rules.
     * <p>
     * 스타일 시트에 <code>@container</code> 규칙이 있는지 알려 준다.
     *
     * @return true if container queries are in use / 컨테이너 질의를 쓰면 true
     */
    public boolean usesContainerQueries() {
        return engine.getResolver().usesContainerQueries();
    }

    /**
     * Reports the measured sizes of the query containers to the style resolver.
     * <p>
     * 잰 질의 컨테이너 크기를 스타일 리졸버에 알린다.
     *
     * @param measured the sizes by container / 컨테이너별 크기
     * @return true if the styles changed and the layout must run again / 스타일이 바뀌어 레이아웃을 다시 해야 하면 true
     */
    public boolean updateContainerSizes(java.util.Map<Element, double[]> measured) {
        return engine.getResolver().updateContainerSizes(measured);
    }
}
