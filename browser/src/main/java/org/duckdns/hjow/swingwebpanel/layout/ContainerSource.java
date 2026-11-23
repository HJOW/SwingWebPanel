package org.duckdns.hjow.swingwebpanel.layout;

import java.util.Map;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * A {@link StyleSource} whose styles depend on the size of the query containers of the layout (the style sheets contain <code>@container</code>
 * rules). The layout engine lays the document out, reports the measured sizes of the containers, and lays it out again if the styles changed because
 * of them (container query evaluation, CSS Conditional Level 5, needs the layout of the container first).
 * <p>
 * 스타일이 레이아웃의 질의 컨테이너 크기에 달려 있는(스타일 시트에 <code>@container</code> 규칙이 있는) {@link StyleSource}. 레이아웃 엔진은 문서를 배치하고 잰 컨테이너
 * 크기를 알린 뒤, 그것 때문에 스타일이 바뀌었으면 다시 배치한다(컨테이너 질의 평가는 컨테이너의 레이아웃이 먼저 필요하다. CSS Conditional Level 5).
 */
public interface ContainerSource {

    /**
     * Tells whether container queries are in use at all; if not, the layout does not measure the containers.
     * <p>
     * 컨테이너 질의를 쓰는지 알려 준다. 쓰지 않으면 레이아웃은 컨테이너를 재지 않는다.
     *
     * @return true if a rule depends on a container size / 규칙이 컨테이너 크기에 달려 있으면 true
     */
    boolean usesContainerQueries();

    /**
     * Reports the content box sizes of the query containers of a layout.
     * <p>
     * 레이아웃의 질의 컨테이너의 content box 크기를 알린다.
     *
     * @param measured the sizes {width, height} by container element / 컨테이너 요소별 크기 {너비, 높이}
     * @return true if a size differs from the one the styles were computed with, so that the layout has to be run again / 스타일을 계산할 때의 크기와 다르면, 즉 레이아웃을 다시 해야 하면 true
     */
    boolean updateContainerSizes(Map<Element, double[]> measured);
}
