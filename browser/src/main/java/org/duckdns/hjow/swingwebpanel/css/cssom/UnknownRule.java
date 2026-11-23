package org.duckdns.hjow.swingwebpanel.css.cssom;

/**
 * A rule that the engine keeps without interpreting it: <code>@page</code>, <code>@property</code>, <code>@counter-style</code>, <code>@container</code>
 * and similar at-rules. It preserves the written text so that the style sheet can be serialized again.
 * <p>
 * 엔진이 해석하지 않고 보관하는 규칙: <code>@page</code>, <code>@property</code>, <code>@counter-style</code>, <code>@container</code> 같은 at-규칙. 스타일 시트를 다시 직렬화할 수
 * 있도록 쓰인 문자열을 보존한다.
 */
public final class UnknownRule extends CssRule {

    /** The at-rule name in lower case. / 소문자 at-규칙 이름. */
    private final String name;

    /** The written text. / 쓰인 문자열. */
    private final String text;

    /**
     * Creates an unknown rule.
     * <p>
     * 알 수 없는 규칙을 만든다.
     *
     * @param name the at-rule name / at-규칙 이름
     * @param text the written text / 쓰인 문자열
     */
    public UnknownRule(String name, String text) {
        this.name = name;
        this.text = text;
    }

    /**
     * Returns the at-rule name.
     * <p>
     * at-규칙 이름을 반환한다.
     *
     * @return the name without the at sign / 골뱅이 없는 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #UNKNOWN_RULE} / UNKNOWN_RULE
     */
    public int getType() {
        return UNKNOWN_RULE;
    }

    /**
     * Returns the number that the CSSOM <code>CSSRule.type</code> reports for the kept at-rule: the legacy numbers of <code>@page</code> (6),
     * <code>@counter-style</code> (11) and <code>@font-feature-values</code> (14), 0 for the others.
     * <p>
     * 보관한 at-규칙에 대해 CSSOM <code>CSSRule.type</code> 이 알려 주는 숫자를 반환한다: <code>@page</code>(6), <code>@counter-style</code>(11),
     * <code>@font-feature-values</code>(14)의 옛 번호이며 나머지는 0.
     *
     * @return the CSSOM type number / CSSOM 종류 번호
     */
    public int getCssomType() {
        return name.equals("page") ? 6 : name.equals("counter-style") ? 11 : name.equals("font-feature-values") ? 14 : 0;
    }

    /**
     * Returns the written text.
     * <p>
     * 쓰인 문자열을 반환한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        return text;
    }
}
