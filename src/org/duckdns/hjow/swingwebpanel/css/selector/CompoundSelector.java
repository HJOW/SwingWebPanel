package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A compound selector: a sequence of simple selectors that must all match the same element (for example <code>a.link#top:hover</code>),
 * optionally ending in a pseudo-element.
 * <p>
 * 복합 선택자: 같은 요소에 모두 일치해야 하는 단순 선택자의 나열(예: <code>a.link#top:hover</code>). 끝에 의사 요소가 올 수 있다.
 */
public final class CompoundSelector {

    /** The simple selectors in source order. / 소스 순서의 단순 선택자. */
    private final List<SimpleSelector> simples;

    /** Pseudo-element name (lower case) without colons, or null. / 콜론 없는 의사 요소 이름(소문자). 없으면 null. */
    private final String pseudoElement;

    /**
     * Creates a compound selector.
     * <p>
     * 복합 선택자를 만든다.
     *
     * @param simples       simple selectors / 단순 선택자
     * @param pseudoElement pseudo-element name or null / 의사 요소 이름 또는 null
     */
    public CompoundSelector(List<SimpleSelector> simples, String pseudoElement) {
        this.simples = Collections.unmodifiableList(new ArrayList<SimpleSelector>(simples));
        this.pseudoElement = pseudoElement;
    }

    /**
     * Returns the simple selectors.
     * <p>
     * 단순 선택자를 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<SimpleSelector> getSimples() {
        return simples;
    }

    /**
     * Returns the pseudo-element.
     * <p>
     * 의사 요소를 반환한다.
     *
     * @return name or null / 이름 또는 null
     */
    public String getPseudoElement() {
        return pseudoElement;
    }

    /**
     * Returns the specificity of this compound selector (a pseudo-element counts as a type selector).
     * <p>
     * 이 복합 선택자의 구체성을 반환한다(의사 요소는 타입 선택자로 센다).
     *
     * @return packed specificity / 묶은 구체성
     */
    public long specificity() {
        long s = pseudoElement != null ? SimpleSelector.SPEC_TYPE : 0;
        // 단순 선택자의 구체성을 모두 더한다.
        for (int i = 0; i < simples.size(); i++) {
            s += simples.get(i).specificity();
        }
        return s;
    }

    /**
     * Serializes the compound selector.
     * <p>
     * 복합 선택자를 직렬화한다.
     *
     * @return text / 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder();
        // 단순 선택자를 순서대로 이어 쓴다.
        for (int i = 0; i < simples.size(); i++) {
            sb.append(simples.get(i).toCssText());
        }
        if (pseudoElement != null) {
            sb.append("::").append(pseudoElement);
        }
        return sb.toString();
    }
}
