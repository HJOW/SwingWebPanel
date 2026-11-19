package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The environment of one value parse: the registry of named definitions and the base URL against which <code>url()</code> values are resolved
 * (the URL of the style sheet that contains the declaration, or null if it is not known).
 * <p>
 * 값 파싱 하나의 환경: 이름 있는 정의의 등록부와 <code>url()</code> 값을 해석할 기준 URL(선언을 담은 스타일 시트의 URL. 모르면 null).
 */
public final class ParseEnv {

    /** Named definitions or null. / 이름 있는 정의 또는 null. */
    private final GrammarRegistry registry;

    /** Base URL or null. / 기준 URL 또는 null. */
    private final String baseUrl;

    /**
     * True if the CSS is parsed in quirks mode: unitless lengths and hashless hex colors are accepted by the properties that the
     * Quirks Mode Standard names. / CSS 를 호환 모드로 파싱하면 true: 호환 모드 표준이 정한 속성에서 단위 없는 길이와 # 없는 16진수 색을 받는다.
     */
    private final boolean quirks;

    /**
     * Creates an environment for standards-mode CSS.
     * <p>
     * 표준 모드 CSS 용 환경을 만든다.
     *
     * @param registry named definitions or null / 이름 있는 정의 또는 null
     * @param baseUrl  base URL or null / 기준 URL 또는 null
     */
    public ParseEnv(GrammarRegistry registry, String baseUrl) {
        this(registry, baseUrl, false);
    }

    /**
     * Creates an environment.
     * <p>
     * 환경을 만든다.
     *
     * @param registry named definitions or null / 이름 있는 정의 또는 null
     * @param baseUrl  base URL or null / 기준 URL 또는 null
     * @param quirks   true to parse in quirks mode / 호환 모드로 파싱하면 true
     */
    public ParseEnv(GrammarRegistry registry, String baseUrl, boolean quirks) {
        this.registry = registry;
        this.baseUrl = baseUrl;
        this.quirks = quirks;
    }

    /**
     * Tells whether the CSS is parsed in quirks mode.
     * <p>
     * CSS 를 호환 모드로 파싱하는지 알려 준다.
     *
     * @return true for quirks mode / 호환 모드이면 true
     */
    public boolean isQuirks() {
        return quirks;
    }

    /**
     * Returns the registry.
     * <p>
     * 등록부를 반환한다.
     *
     * @return registry or null / 등록부 또는 null
     */
    public GrammarRegistry getRegistry() {
        return registry;
    }

    /**
     * Returns the base URL.
     * <p>
     * 기준 URL 을 반환한다.
     *
     * @return URL or null / URL 또는 null
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns an environment with another base URL.
     * <p>
     * 기준 URL 만 다른 환경을 반환한다.
     *
     * @param newBase the new base URL / 새 기준 URL
     * @return the environment / 환경
     */
    public ParseEnv withBase(String newBase) {
        return new ParseEnv(registry, newBase, quirks);
    }

    /**
     * Resolves a URL reference against the base URL.
     * <p>
     * URL 참조를 기준 URL 에 대해 해석한다.
     *
     * @param ref the reference as written / 쓰인 그대로의 참조
     * @return the absolute URL, or null if there is no base or resolution failed / 절대 URL. 기준이 없거나 해석에 실패하면 null
     */
    public String resolve(String ref) {
        if (baseUrl == null) {
            return null;
        }
        try {
            return UrlUtil.resolve(baseUrl, ref);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
