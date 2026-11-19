package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A number with a non-length unit: an angle (<code>45deg</code>), a time (<code>200ms</code>), a frequency, a resolution
 * (<code>2dppx</code>) or a flexible length (<code>1fr</code>). The kind is derived from the unit.
 * <p>
 * 길이가 아닌 단위가 붙은 숫자: 각도(<code>45deg</code>), 시간(<code>200ms</code>), 주파수, 해상도(<code>2dppx</code>), 유연한 길이(<code>1fr</code>).
 * 종류는 단위에서 정한다.
 */
public final class CssDimension extends CssValue {

    /** The number. / 숫자. */
    private final double value;

    /** Lower-case unit. / 소문자 단위. */
    private final String unit;

    /**
     * Creates a dimension.
     * <p>
     * 차원 값을 만든다.
     *
     * @param value number / 숫자
     * @param unit  unit name (lower-cased here) / 단위 이름(여기서 소문자로 바뀜)
     */
    public CssDimension(double value, String unit) {
        this.value = value;
        this.unit = CssKeyword.lower(unit);
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
     * Returns the kind of the unit (see {@link CssUnits}).
     * <p>
     * 단위의 종류를 반환한다({@link CssUnits} 참조).
     *
     * @return kind constant / 종류 상수
     */
    public int getKind() {
        return CssUnits.kindOf(unit);
    }

    /**
     * Returns the value in the canonical unit of its kind: degrees, seconds, dots per pixel; other kinds return the plain number.
     * <p>
     * 종류별 기준 단위의 값을 반환한다: 도, 초, 픽셀당 점. 다른 종류는 숫자 그대로이다.
     *
     * @return canonical value / 기준 단위 값
     */
    public double canonical() {
        int k = getKind();
        if (k == CssUnits.ANGLE) {
            return CssUnits.angleToDegrees(value, unit);
        }
        if (k == CssUnits.TIME) {
            return CssUnits.timeToSeconds(value, unit);
        }
        if (k == CssUnits.RESOLUTION) {
            return CssUnits.resolutionToDppx(value, unit);
        }
        if (k == CssUnits.FREQUENCY) {
            return unit.equals("khz") ? value * 1000.0 : value;
        }
        return value;
    }

    /**
     * Serializes the dimension.
     * <p>
     * 차원 값을 직렬화한다.
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
        return o instanceof CssDimension && ((CssDimension) o).value == value && ((CssDimension) o).unit.equals(unit);
    }

    /**
     * Hash of number and unit.
     * <p>
     * 숫자와 단위의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return Double.valueOf(value).hashCode() * 31 + unit.hashCode() + 7;
    }
}
