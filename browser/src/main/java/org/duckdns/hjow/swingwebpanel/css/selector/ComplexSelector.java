package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complex selector: compound selectors joined by combinators (descendant, child, next-sibling, subsequent-sibling), for example
 * <code>nav &gt; ul li + li</code>. A relative complex selector (the argument of <code>:has()</code>) additionally has a leading combinator that
 * relates its first compound selector to the anchor element.
 * <p>
 * 복합 선택자: 결합자(자손, 자식, 다음 형제, 이후 형제)로 이어진 복합 선택자들. 예: <code>nav &gt; ul li + li</code>. 상대 복합 선택자(<code>:has()</code>
 * 의 인수)는 첫 복합 선택자와 기준 요소의 관계를 나타내는 맨 앞 결합자가 더 있다.
 */
public final class ComplexSelector {

    /** Combinator: descendant (whitespace). / 결합자: 자손(공백). */
    public static final char DESCENDANT = ' ';
    /** Combinator: child. / 결합자: 자식. */
    public static final char CHILD = '>';
    /** Combinator: next sibling. / 결합자: 다음 형제. */
    public static final char NEXT_SIBLING = '+';
    /** Combinator: subsequent sibling. / 결합자: 이후 형제. */
    public static final char LATER_SIBLING = '~';

    /** The compound selectors from left to right. / 왼쪽에서 오른쪽 순서의 복합 선택자. */
    private final List<CompoundSelector> compounds;

    /** Combinators between neighbours: combinators[i] joins compounds[i] and compounds[i+1]. / 이웃 사이의 결합자: combinators[i] 는 compounds[i] 와 compounds[i+1] 을 잇는다. */
    private final char[] combinators;

    /** Leading combinator of a relative selector, or 0. / 상대 선택자의 맨 앞 결합자. 아니면 0. */
    private final char leading;

    /** Packed specificity. / 묶은 구체성. */
    private final long specificity;

    /**
     * Creates a complex selector.
     * <p>
     * 복합 선택자를 만든다.
     *
     * @param compounds   compound selectors, at least one / 복합 선택자(하나 이상)
     * @param combinators combinators between them (one fewer than compounds) / 그 사이의 결합자(복합 선택자보다 하나 적음)
     * @param leading     leading combinator for relative selectors, else 0 / 상대 선택자의 맨 앞 결합자. 아니면 0
     */
    public ComplexSelector(List<CompoundSelector> compounds, char[] combinators, char leading) {
        this.compounds = Collections.unmodifiableList(new ArrayList<CompoundSelector>(compounds));
        this.combinators = combinators.clone();
        this.leading = leading;
        long s = 0;
        // 모든 복합 선택자의 구체성을 더한다.
        for (int i = 0; i < compounds.size(); i++) {
            s += compounds.get(i).specificity();
        }
        this.specificity = s;
    }

    /**
     * Returns the compound selectors.
     * <p>
     * 복합 선택자들을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<CompoundSelector> getCompounds() {
        return compounds;
    }

    /**
     * Returns the combinator between compound <code>i</code> and <code>i+1</code>.
     * <p>
     * 복합 선택자 <code>i</code> 와 <code>i+1</code> 사이의 결합자를 반환한다.
     *
     * @param i index of the left compound / 왼쪽 복합 선택자의 위치
     * @return combinator character / 결합자 문자
     */
    public char getCombinator(int i) {
        return combinators[i];
    }

    /**
     * Returns the leading combinator of a relative selector.
     * <p>
     * 상대 선택자의 맨 앞 결합자를 반환한다.
     *
     * @return combinator or 0 / 결합자 또는 0
     */
    public char getLeadingCombinator() {
        return leading;
    }

    /**
     * Tells whether this is a relative selector.
     * <p>
     * 상대 선택자인지 알려 준다.
     *
     * @return true for relative selectors / 상대 선택자이면 true
     */
    public boolean isRelative() {
        return leading != 0;
    }

    /**
     * Returns the rightmost compound selector (the subject).
     * <p>
     * 가장 오른쪽 복합 선택자(대상)를 반환한다.
     *
     * @return the subject compound / 대상 복합 선택자
     */
    public CompoundSelector getSubject() {
        return compounds.get(compounds.size() - 1);
    }

    /**
     * Returns the pseudo-element the selector targets.
     * <p>
     * 선택자가 가리키는 의사 요소를 반환한다.
     *
     * @return pseudo-element name or null / 의사 요소 이름 또는 null
     */
    public String getPseudoElement() {
        return getSubject().getPseudoElement();
    }

    /**
     * Returns the packed specificity.
     * <p>
     * 묶은 구체성을 반환한다.
     *
     * @return specificity (ids, classes, types in 20-bit fields) / 구체성(id, 클래스, 타입 각 20비트)
     */
    public long getSpecificity() {
        return specificity;
    }

    /**
     * Serializes the selector.
     * <p>
     * 선택자를 직렬화한다.
     *
     * @return text / 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder();
        if (leading != 0 && leading != DESCENDANT) {
            sb.append(leading).append(' ');
        }
        // 복합 선택자와 결합자를 번갈아 이어 쓴다.
        for (int i = 0; i < compounds.size(); i++) {
            if (i > 0) {
                char c = combinators[i - 1];
                sb.append(c == DESCENDANT ? " " : " " + c + " ");
            }
            sb.append(compounds.get(i).toCssText());
        }
        return sb.toString();
    }
}
