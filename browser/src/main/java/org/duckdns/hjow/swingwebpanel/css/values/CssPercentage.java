package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A percentage. It stays a percentage in computed values; the layout resolves it against the proper reference length (the used value).
 * <p>
 * 백분율. 계산 값에서도 백분율로 남으며, 실제 값은 레이아웃이 알맞은 기준 길이에 대해 계산한다.
 */
public final class CssPercentage extends CssValue {

    /** The percentage number (50 for 50%). / 백분율 숫자(50% 이면 50). */
    private final double value;

    /**
     * Creates a percentage.
     * <p>
     * 백분율을 만든다.
     *
     * @param value number before the percent sign / 퍼센트 기호 앞의 숫자
     */
    public CssPercentage(double value) {
        this.value = value;
    }

    /**
     * Returns the number before the percent sign.
     * <p>
     * 퍼센트 기호 앞의 숫자를 반환한다.
     *
     * @return number / 숫자
     */
    public double getValue() {
        return value;
    }

    /**
     * Serializes the percentage.
     * <p>
     * 백분율을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssEscape.number(value) + "%";
    }

    /**
     * Compares by value.
     * <p>
     * 값으로 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssPercentage && ((CssPercentage) o).value == value;
    }

    /**
     * Hash of the value.
     * <p>
     * 값의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return Double.valueOf(value).hashCode() + 1;
    }
}
