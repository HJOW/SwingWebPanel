package org.duckdns.hjow.swingwebpanel.css.values;

/**
 * Base class of all typed CSS values: keywords, numbers, lengths, percentages, colors, strings, urls, lists, functions and calc expressions.
 * Values are immutable and serialize back to CSS text with {@link #toCssText()} in the canonical form of the CSSOM.
 * <p>
 * 형식이 있는 모든 CSS 값의 기반 클래스: 키워드, 숫자, 길이, 백분율, 색, 문자열, url, 목록, 함수, calc 식. 값은 불변이며
 * {@link #toCssText()} 로 CSSOM 의 정규 형태의 CSS 문자열로 다시 직렬화된다.
 */
public abstract class CssValue {

    /**
     * Serializes the value.
     * <p>
     * 값을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public abstract String toCssText();

    /**
     * Tells whether this value is the given keyword (ASCII case-insensitive); false for every other kind of value.
     * <p>
     * 이 값이 주어진 키워드인지 알려 준다(ASCII 대소문자 무시). 다른 종류의 값이면 항상 false.
     *
     * @param name keyword / 키워드
     * @return true if it is that keyword / 그 키워드이면 true
     */
    public boolean isKeyword(String name) {
        return false;
    }

    /**
     * Returns the serialized form.
     * <p>
     * 직렬화한 문자열을 반환한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toString() {
        return toCssText();
    }
}
