package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A rule as produced by the parser: either a qualified rule (a prelude and a block, such as a style rule) or an at-rule (an at-keyword name,
 * a prelude and an optional block). The contents are not interpreted here; the style sheet builder (<code>css.cssom</code>) does that.
 * <p>
 * 파서가 만든 규칙: 한정 규칙(전문과 블록, 예: 스타일 규칙) 또는 at-규칙(at-키워드 이름, 전문, 선택적 블록). 내용은 여기서 해석하지 않으며
 * 스타일 시트 구성기(<code>css.cssom</code>)가 해석한다.
 */
public final class SyntaxRule {

    /** True for at-rules. / at-규칙이면 true. */
    private final boolean atRule;

    /** At-keyword name in lower case (at-rules only), else null. / at-키워드 이름(소문자, at-규칙만). 아니면 null. */
    private final String name;

    /** Prelude component values with leading and trailing whitespace removed. / 앞뒤 공백을 제거한 전문의 컴포넌트 값. */
    private final List<ComponentValue> prelude;

    /** Block contents, or null when the at-rule ended with a semicolon. / 블록의 내용. at-규칙이 세미콜론으로 끝났으면 null. */
    private final List<ComponentValue> block;

    /** Source offset of the first token. / 첫 토큰의 소스 위치. */
    private final int start;

    /**
     * Creates a rule.
     * <p>
     * 규칙을 만든다.
     *
     * @param atRule  true for at-rules / at-규칙이면 true
     * @param name    at-keyword name or null / at-키워드 이름 또는 null
     * @param prelude prelude component values / 전문의 컴포넌트 값
     * @param block   block contents or null / 블록의 내용 또는 null
     * @param start   source offset / 소스 위치
     */
    public SyntaxRule(boolean atRule, String name, List<ComponentValue> prelude, List<ComponentValue> block, int start) {
        this.atRule = atRule;
        this.name = name == null ? null : SyntaxDeclaration.asciiLower(name);
        this.prelude = Collections.unmodifiableList(new ArrayList<ComponentValue>(prelude));
        this.block = block == null ? null : Collections.unmodifiableList(new ArrayList<ComponentValue>(block));
        this.start = start;
    }

    /**
     * Tells whether this is an at-rule.
     * <p>
     * at-규칙인지 알려 준다.
     *
     * @return true for at-rules / at-규칙이면 true
     */
    public boolean isAtRule() {
        return atRule;
    }

    /**
     * Returns the at-keyword name in lower case.
     * <p>
     * at-키워드 이름(소문자)을 반환한다.
     *
     * @return name or null for qualified rules / 이름. 한정 규칙이면 null
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the prelude.
     * <p>
     * 전문을 반환한다.
     *
     * @return component values / 컴포넌트 값
     */
    public List<ComponentValue> getPrelude() {
        return prelude;
    }

    /**
     * Returns the block contents.
     * <p>
     * 블록의 내용을 반환한다.
     *
     * @return component values, or null if there is no block / 컴포넌트 값. 블록이 없으면 null
     */
    public List<ComponentValue> getBlock() {
        return block;
    }

    /**
     * Returns the source offset of the rule.
     * <p>
     * 규칙의 소스 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the serialized prelude.
     * <p>
     * 직렬화한 전문을 반환한다.
     *
     * @return text / 문자열
     */
    public String getPreludeText() {
        return CssBlock.join(prelude).trim();
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (atRule) {
            sb.append('@').append(name).append(' ');
        }
        sb.append(getPreludeText());
        if (block != null) {
            sb.append(" {").append(CssBlock.join(block)).append('}');
        } else {
            sb.append(';');
        }
        return sb.toString();
    }
}
