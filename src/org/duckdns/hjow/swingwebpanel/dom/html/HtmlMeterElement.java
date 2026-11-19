package org.duckdns.hjow.swingwebpanel.dom.html;

import java.math.BigDecimal;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * The <code>meter</code> element (HTML Standard 4.10.14): a scalar measurement within a known range. It is labelable but not a listed form
 * control. The values follow the "ranges" rules of the standard: the minimum defaults to 0, the maximum to 1 (and is not below the minimum),
 * the value, low, high and optimum are limited to the range, and low, high and optimum default to the minimum, the maximum and the middle.
 * <p>
 * <code>meter</code> 요소(HTML 표준 4.10.14): 알려진 범위 안의 스칼라 측정값. 라벨을 붙일 수 있지만 목록에 오르는 폼 컨트롤은 아니다. 값은 표준의 "ranges"
 * 규칙을 따른다: 최솟값 기본 0, 최댓값 기본 1(최솟값 아래로 내려가지 않음), value, low, high, optimum 은 범위로 제한하고 low, high, optimum 의 기본은 최솟값,
 * 최댓값, 중간값이다.
 */
public final class HtmlMeterElement extends HtmlControl {

    /**
     * Creates a meter element.
     * <p>
     * meter 요소를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlMeterElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * A meter element is labelable.
     * <p>
     * meter 요소는 라벨을 붙일 수 있다.
     *
     * @return true / true
     */
    public boolean isLabelable() {
        return true;
    }

    /**
     * Reads a numeric attribute.
     * <p>
     * 숫자 속성을 읽는다.
     *
     * @param name     the attribute / 속성
     * @param fallback the value if it is absent or invalid / 없거나 올바르지 않을 때의 값
     * @return the number / 수
     */
    private double number(String name, double fallback) {
        BigDecimal v = NumberSyntax.parse(getAttribute(name));
        return v == null ? fallback : v.doubleValue();
    }

    /**
     * Returns the minimum.
     * <p>
     * 최솟값을 반환한다.
     *
     * @return the minimum, 0 by default / 최솟값. 기본 0
     */
    public double getMin() {
        return number("min", 0);
    }

    /**
     * Returns the maximum (never below the minimum).
     * <p>
     * 최댓값을 반환한다(최솟값 아래로 내려가지 않는다).
     *
     * @return the maximum, 1 by default / 최댓값. 기본 1
     */
    public double getMax() {
        return Math.max(number("max", 1), getMin());
    }

    /**
     * Returns the value, limited to the range.
     * <p>
     * 범위로 제한한 값을 반환한다.
     *
     * @return the value, 0 by default / 값. 기본 0
     */
    public double getValue() {
        return Math.min(Math.max(number("value", 0), getMin()), getMax());
    }

    /**
     * Returns the low boundary, limited to the range.
     * <p>
     * 범위로 제한한 low 경계를 반환한다.
     *
     * @return low, the minimum by default / low. 기본은 최솟값
     */
    public double getLow() {
        return Math.min(Math.max(number("low", getMin()), getMin()), getMax());
    }

    /**
     * Returns the high boundary, limited to the range and not below low.
     * <p>
     * 범위로 제한하고 low 아래로 내려가지 않는 high 경계를 반환한다.
     *
     * @return high, the maximum by default / high. 기본은 최댓값
     */
    public double getHigh() {
        return Math.max(Math.min(Math.max(number("high", getMax()), getMin()), getMax()), getLow());
    }

    /**
     * Returns the optimum, limited to the range.
     * <p>
     * 범위로 제한한 optimum 을 반환한다.
     *
     * @return optimum, the middle by default / optimum. 기본은 중간값
     */
    public double getOptimum() {
        return Math.min(Math.max(number("optimum", (getMin() + getMax()) / 2), getMin()), getMax());
    }

    /**
     * Sets a numeric attribute.
     * <p>
     * 숫자 속성을 지정한다.
     *
     * @param name  the attribute / 속성
     * @param value the number / 수
     */
    private void put(String name, double value) {
        setAttribute(name, NumberSyntax.format(value));
    }

    /**
     * Sets the <code>min</code> attribute.
     * <p>
     * <code>min</code> 속성을 지정한다.
     *
     * @param value the minimum / 최솟값
     */
    public void setMin(double value) {
        put("min", value);
    }

    /**
     * Sets the <code>max</code> attribute.
     * <p>
     * <code>max</code> 속성을 지정한다.
     *
     * @param value the maximum / 최댓값
     */
    public void setMax(double value) {
        put("max", value);
    }

    /**
     * Sets the <code>value</code> attribute.
     * <p>
     * <code>value</code> 속성을 지정한다.
     *
     * @param value the value / 값
     */
    public void setValue(double value) {
        put("value", value);
    }

    /**
     * Sets the <code>low</code> attribute.
     * <p>
     * <code>low</code> 속성을 지정한다.
     *
     * @param value the low boundary / low 경계
     */
    public void setLow(double value) {
        put("low", value);
    }

    /**
     * Sets the <code>high</code> attribute.
     * <p>
     * <code>high</code> 속성을 지정한다.
     *
     * @param value the high boundary / high 경계
     */
    public void setHigh(double value) {
        put("high", value);
    }

    /**
     * Sets the <code>optimum</code> attribute.
     * <p>
     * <code>optimum</code> 속성을 지정한다.
     *
     * @param value the optimum / optimum
     */
    public void setOptimum(double value) {
        put("optimum", value);
    }
}
