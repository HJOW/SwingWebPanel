package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.HashMap;
import java.util.Map;

/**
 * The namespace prefixes declared by <code>@namespace</code> rules of a style sheet: a default namespace and prefix to URI bindings. The selector
 * parser consults it to resolve <code>ns|name</code>; without a map (as in <code>querySelector</code>) prefixes are not allowed and no default
 * namespace applies.
 * <p>
 * 스타일 시트의 <code>@namespace</code> 규칙이 선언한 네임스페이스 접두사: 기본 네임스페이스와 접두사-URI 연결. 선택자 파서가 <code>ns|name</code> 을
 * 해석할 때 참조한다. 맵이 없으면(<code>querySelector</code> 처럼) 접두사는 허용되지 않고 기본 네임스페이스도 적용되지 않는다.
 */
public final class NamespaceMap {

    /** Default namespace URI, or null if none was declared. / 기본 네임스페이스 URI. 선언이 없으면 null. */
    private String defaultNamespace;

    /** Prefix to URI. / 접두사에서 URI 로. */
    private final Map<String, String> prefixes = new HashMap<String, String>();

    /**
     * Declares a namespace (a null or empty prefix declares the default namespace).
     * <p>
     * 네임스페이스를 선언한다(접두사가 null 이거나 비었으면 기본 네임스페이스).
     *
     * @param prefix prefix or null / 접두사 또는 null
     * @param uri    namespace URI / 네임스페이스 URI
     */
    public void declare(String prefix, String uri) {
        if (prefix == null || prefix.length() == 0) {
            defaultNamespace = uri;
        } else {
            prefixes.put(prefix, uri);
        }
    }

    /**
     * Returns the default namespace.
     * <p>
     * 기본 네임스페이스를 반환한다.
     *
     * @return URI or null / URI 또는 null
     */
    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    /**
     * Resolves a prefix.
     * <p>
     * 접두사를 해석한다.
     *
     * @param prefix prefix / 접두사
     * @return URI, or null if the prefix is not declared / URI. 선언되지 않았으면 null
     */
    public String resolve(String prefix) {
        return prefixes.get(prefix);
    }

    /**
     * Tells whether a prefix is declared.
     * <p>
     * 접두사가 선언되었는지 알려 준다.
     *
     * @param prefix prefix / 접두사
     * @return true if declared / 선언되었으면 true
     */
    public boolean has(String prefix) {
        return prefixes.containsKey(prefix);
    }
}
