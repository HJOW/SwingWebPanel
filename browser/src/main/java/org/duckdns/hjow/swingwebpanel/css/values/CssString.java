package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A string value (<code>"Arial"</code>, the argument of <code>content</code>, a quoted font name).
 * <p>
 * 문자열 값(<code>"Arial"</code>, <code>content</code> 의 인수, 따옴표 친 글꼴 이름).
 */
public final class CssString extends CssValue {

    /** The string content without quotes. / 따옴표 없는 문자열 내용. */
    private final String value;

    /**
     * Creates a string value.
     * <p>
     * 문자열 값을 만든다.
     *
     * @param value content / 내용
     */
    public CssString(String value) {
        this.value = value;
    }

    /**
     * Returns the content.
     * <p>
     * 내용을 반환한다.
     *
     * @return content / 내용
     */
    public String getValue() {
        return value;
    }

    /**
     * Serializes with double quotes.
     * <p>
     * 큰따옴표로 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssEscape.string(value);
    }

    /**
     * Compares the content.
     * <p>
     * 내용을 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssString && ((CssString) o).value.equals(value);
    }

    /**
     * Hash of the content.
     * <p>
     * 내용의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return value.hashCode() + 3;
    }
}
