package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A length: a number with a length unit. The unit is kept in lower case. Computed values hold absolute pixels; specified values may use any
 * unit. The unitless zero is accepted by the parser and stored as <code>0px</code>.
 * <p>
 * 길이: 길이 단위가 붙은 숫자. 단위는 소문자로 보관한다. 계산 값은 절대 픽셀이며 지정 값은 어떤 단위든 가질 수 있다. 단위 없는 0 은 파서가 받아들여
 * <code>0px</code> 로 저장한다.
 */
public final class CssLength extends CssValue {

    /** The zero length. / 0 길이. */
    public static final CssLength ZERO = new CssLength(0, "px");

    /** The number. / 숫자. */
    private final double value;

    /** Lower-case unit. / 소문자 단위. */
    private final String unit;

    /**
     * Creates a length.
     * <p>
     * 길이를 만든다.
     *
     * @param value number / 숫자
     * @param unit  unit name (lower-cased here) / 단위 이름(여기서 소문자로 바뀜)
     */
    public CssLength(double value, String unit) {
        this.value = value;
        this.unit = CssKeyword.lower(unit);
    }

    /**
     * Creates a length in pixels.
     * <p>
     * 픽셀 길이를 만든다.
     *
     * @param px pixels / 픽셀
     * @return the length / 길이
     */
    public static CssLength px(double px) {
        return new CssLength(px, "px");
    }

    /**
     * Returns the number.
     * <p>
     * 숫자를 반환한다.
     *
     * @return number / 숫자
     */
    public double getValue() {
        return value;
    }

    /**
     * Returns the unit.
     * <p>
     * 단위를 반환한다.
     *
     * @return lower-case unit / 소문자 단위
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Tells whether the unit is already pixels.
     * <p>
     * 단위가 이미 픽셀인지 알려 준다.
     *
     * @return true for px / px 이면 true
     */
    public boolean isPx() {
        return unit.equals("px");
    }

    /**
     * Converts to pixels with a context.
     * <p>
     * 컨텍스트로 픽셀로 바꾼다.
     *
     * @param ctx reference sizes / 기준 크기
     * @return pixels / 픽셀
     */
    public double toPx(LengthContext ctx) {
        return ctx.toPx(value, unit);
    }

    /**
     * Returns the length in pixels if it is absolute, else NaN.
     * <p>
     * 절대 길이이면 픽셀 값을, 아니면 NaN 을 반환한다.
     *
     * @return pixels or NaN / 픽셀 또는 NaN
     */
    public double absolutePx() {
        return CssUnits.absoluteToPx(value, unit);
    }

    /**
     * Serializes the length (a zero keeps its unit).
     * <p>
     * 길이를 직렬화한다(0 도 단위를 유지).
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssEscape.number(value) + unit;
    }

    /**
     * Compares number and unit.
     * <p>
     * 숫자와 단위를 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssLength && ((CssLength) o).value == value && ((CssLength) o).unit.equals(unit);
    }

    /**
     * Hash of number and unit.
     * <p>
     * 숫자와 단위의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return Double.valueOf(value).hashCode() * 31 + unit.hashCode();
    }
}
