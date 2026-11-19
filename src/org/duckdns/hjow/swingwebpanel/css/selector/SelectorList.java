package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A comma separated list of complex selectors (the prelude of a style rule, or the argument of <code>:is()</code>, <code>:not()</code>,
 * <code>:has()</code>). The list matches an element when any of its selectors does.
 * <p>
 * 쉼표로 구분한 복합 선택자 목록(스타일 규칙의 전문이나 <code>:is()</code>, <code>:not()</code>, <code>:has()</code> 의 인수). 목록의 선택자 중 하나라도
 * 일치하면 요소와 일치한다.
 */
public final class SelectorList {

    /** The selectors. / 선택자들. */
    private final List<ComplexSelector> selectors;

    /**
     * Creates a list.
     * <p>
     * 목록을 만든다.
     *
     * @param selectors the selectors / 선택자들
     */
    public SelectorList(List<ComplexSelector> selectors) {
        this.selectors = Collections.unmodifiableList(new ArrayList<ComplexSelector>(selectors));
    }

    /**
     * Returns the selectors.
     * <p>
     * 선택자들을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ComplexSelector> getSelectors() {
        return selectors;
    }

    /**
     * Returns the number of selectors.
     * <p>
     * 선택자 개수를 반환한다.
     *
     * @return size / 개수
     */
    public int size() {
        return selectors.size();
    }

    /**
     * Returns the largest specificity of the selectors (the specificity of a logical pseudo-class argument).
     * <p>
     * 선택자들의 가장 큰 구체성을 반환한다(논리 의사 클래스 인수의 구체성).
     *
     * @return packed specificity / 묶은 구체성
     */
    public long maxSpecificity() {
        long max = 0;
        // 가장 큰 구체성을 찾는다.
        for (int i = 0; i < selectors.size(); i++) {
            max = Math.max(max, selectors.get(i).getSpecificity());
        }
        return max;
    }

    /**
     * Serializes the list as CSSOM does (comma and space between selectors).
     * <p>
     * CSSOM 처럼 목록을 직렬화한다(선택자 사이에 쉼표와 공백).
     *
     * @return text / 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder();
        // 선택자를 쉼표와 공백으로 이어 쓴다.
        for (int i = 0; i < selectors.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(selectors.get(i).toCssText());
        }
        return sb.toString();
    }
}
