package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.List;

/**
 * The CSSOM <code>CSSRuleList</code>: a live, read-only list of the rules of a style sheet, of a group rule or of a keyframes rule. It wraps the
 * (unmodifiable) rule list of its owner, so it shows insertions and deletions at once, and the same object is handed out for the same owner.
 * <p>
 * CSSOM <code>CSSRuleList</code>: 스타일 시트, 그룹 규칙, 키프레임 규칙의 규칙을 읽기 전용으로 실시간으로 보여 주는 목록. 소유자의 (수정할 수 없는) 규칙 목록을 감싸므로
 * 삽입과 삭제를 바로 보여 주며, 같은 소유자에게는 같은 객체를 내준다.
 */
public final class CssRuleList {

    /** The rules of the owner (a read-only view that follows the owner). / 소유자의 규칙(소유자를 따라가는 읽기 전용 뷰). */
    private final List<? extends CssRule> rules;

    /**
     * Creates the list.
     * <p>
     * 목록을 만든다.
     *
     * @param rules the rules of the owner / 소유자의 규칙
     */
    CssRuleList(List<? extends CssRule> rules) {
        this.rules = rules;
    }

    /**
     * Returns the number of rules (CSSOM <code>length</code>).
     * <p>
     * 규칙 수를 반환한다(CSSOM <code>length</code>).
     *
     * @return the count / 개수
     */
    public int getLength() {
        return rules.size();
    }

    /**
     * Returns the rule at an index (CSSOM <code>item</code>).
     * <p>
     * 위치의 규칙을 반환한다(CSSOM <code>item</code>).
     *
     * @param index the position / 위치
     * @return the rule, or null if the index is out of range / 규칙. 위치가 범위를 벗어나면 null
     */
    public CssRule item(int index) {
        return index >= 0 && index < rules.size() ? rules.get(index) : null;
    }
}
