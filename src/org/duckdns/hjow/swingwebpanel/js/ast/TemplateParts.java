package org.duckdns.hjow.swingwebpanel.js.ast;

/**
 * The string parts of a template literal: for every part the cooked string (null if the part has an invalid escape,
 * which is allowed only in tagged templates) and the raw string.
 * <p>
 * 템플릿 리터럴의 문자열 조각: 각 조각의 해석된 문자열(잘못된 이스케이프가 있으면 null, 태그 템플릿에서만 허용)과 원문 문자열.
 */
public final class TemplateParts {

    /**
     * The cooked strings (a null entry is undefined).
     * <p>
     * 해석된 문자열들(null 항목은 undefined).
     */
    public final String[] cooked;

    /**
     * The raw strings.
     * <p>
     * 원문 문자열들.
     */
    public final String[] raw;

    /**
     * Creates the parts.
     * <p>
     * 조각들을 만든다.
     *
     * @param cooked the cooked strings / 해석된 문자열들
     * @param raw    the raw strings / 원문 문자열들
     */
    public TemplateParts(String[] cooked, String[] raw) {
        this.cooked = cooked;
        this.raw = raw;
    }
}
