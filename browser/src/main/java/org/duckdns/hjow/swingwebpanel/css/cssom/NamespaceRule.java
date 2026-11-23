package org.duckdns.hjow.swingwebpanel.css.cssom;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * An <code>@namespace</code> rule: the default namespace or a prefix bound to a namespace URI.
 * <p>
 * <code>@namespace</code> 규칙: 기본 네임스페이스 또는 네임스페이스 URI 에 연결된 접두사.
 */
public final class NamespaceRule extends CssRule {

    /** The prefix, or the empty string for the default namespace. / 접두사. 기본 네임스페이스이면 빈 문자열. */
    private final String prefix;

    /** The namespace URI. / 네임스페이스 URI. */
    private final String uri;

    /**
     * Creates a namespace rule.
     * <p>
     * 네임스페이스 규칙을 만든다.
     *
     * @param prefix prefix or empty / 접두사 또는 빈 문자열
     * @param uri    namespace URI / 네임스페이스 URI
     */
    public NamespaceRule(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
    }

    /**
     * Returns the prefix.
     * <p>
     * 접두사를 반환한다.
     *
     * @return prefix, empty for the default namespace / 접두사. 기본 네임스페이스이면 빈 문자열
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the namespace URI.
     * <p>
     * 네임스페이스 URI 를 반환한다.
     *
     * @return the URI / URI
     */
    public String getNamespaceUri() {
        return uri;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #NAMESPACE_RULE} / NAMESPACE_RULE
     */
    public int getType() {
        return NAMESPACE_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        return "@namespace " + (prefix.length() > 0 ? CssEscape.ident(prefix) + " " : "") + "url(" + CssEscape.string(uri) + ");";
    }
}
