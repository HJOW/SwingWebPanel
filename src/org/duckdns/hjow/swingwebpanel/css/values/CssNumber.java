package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A number (integer or real). Integers are remembered so that properties which only accept integers (<code>z-index</code>,
 * <code>order</code>) can be validated and serialized without a fraction.
 * <p>
 * 숫자(정수 또는 실수). 정수만 받는 속성(<code>z-index</code>, <code>order</code>)을 검증하고 소수부 없이 직렬화할 수 있도록 정수 여부를 기억한다.
 */
public final class CssNumber extends CssValue {

    /** The numeric value. / 숫자 값. */
    private final double value;

    /** True if the number was written as an integer. / 정수로 쓰였으면 true. */
    private final boolean integer;

    /**
     * Creates a number.
     * <p>
     * 숫자를 만든다.
     *
     * @param value   value / 값
     * @param integer true for integers / 정수이면 true
     */
    public CssNumber(double value, boolean integer) {
        this.value = value;
        this.integer = integer;
    }

    /**
     * Returns the value.
     * <p>
     * 값을 반환한다.
     *
     * @return number / 숫자
     */
    public double getValue() {
        return value;
    }

    /**
     * Tells whether the number is an integer.
     * <p>
     * 숫자가 정수인지 알려 준다.
     *
     * @return true for integers / 정수이면 true
     */
    public boolean isInteger() {
        return integer;
    }

    /**
     * Serializes the number.
     * <p>
     * 숫자를 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssEscape.number(value);
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
        return o instanceof CssNumber && ((CssNumber) o).value == value;
    }

    /**
     * Hash of the value.
     * <p>
     * 값의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return Double.valueOf(value).hashCode();
    }
}
