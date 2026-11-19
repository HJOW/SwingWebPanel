package org.duckdns.hjow.swingwebpanel.dom.html;

import java.math.BigDecimal;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * The <code>progress</code> element (HTML Standard 4.10.13): a completion bar. It is labelable but not a listed form control. Without a valid
 * <code>value</code> attribute the progress bar is indeterminate (its <code>position</code> is -1).
 * <p>
 * <code>progress</code> 요소(HTML 표준 4.10.13): 진행 막대. 라벨을 붙일 수 있지만 목록에 오르는 폼 컨트롤은 아니다. 올바른 <code>value</code> 속성이 없으면
 * 진행 막대는 불확정 상태이다(<code>position</code> 이 -1).
 */
public final class HtmlProgressElement extends HtmlControl {

    /**
     * Creates a progress element.
     * <p>
     * progress 요소를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlProgressElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * A progress element is labelable.
     * <p>
     * progress 요소는 라벨을 붙일 수 있다.
     *
     * @return true / true
     */
    public boolean isLabelable() {
        return true;
    }

    /**
     * Returns the maximum: the <code>max</code> attribute if it is a valid number greater than zero, otherwise 1.
     * <p>
     * 최댓값을 반환한다: <code>max</code> 속성이 0 보다 큰 올바른 수이면 그 값, 아니면 1.
     *
     * @return the maximum / 최댓값
     */
    public double getMax() {
        BigDecimal m = NumberSyntax.parse(getAttribute("max"));
        return m != null && m.signum() > 0 ? m.doubleValue() : 1.0;
    }

    /**
     * Sets the <code>max</code> attribute; a value that is not greater than zero is ignored.
     * <p>
     * <code>max</code> 속성을 지정한다. 0 보다 크지 않은 값은 무시한다.
     *
     * @param value the new maximum / 새 최댓값
     */
    public void setMax(double value) {
        if (value > 0) {
            setAttribute("max", NumberSyntax.format(value));
        }
    }

    /**
     * Tells whether the progress bar is determinate (a valid <code>value</code> attribute).
     * <p>
     * 진행 막대가 확정 상태인지(올바른 <code>value</code> 속성이 있는지) 알려 준다.
     *
     * @return true if determinate / 확정 상태이면 true
     */
    public boolean isDeterminate() {
        BigDecimal v = NumberSyntax.parse(getAttribute("value"));
        return v != null && v.signum() >= 0;
    }

    /**
     * Returns the value: the <code>value</code> attribute if valid (not below zero), limited to the maximum, otherwise 0.
     * <p>
     * 값을 반환한다: <code>value</code> 속성이 올바르면(0 이상) 최댓값으로 제한한 그 값, 아니면 0.
     *
     * @return the value / 값
     */
    public double getValue() {
        BigDecimal v = NumberSyntax.parse(getAttribute("value"));
        if (v == null || v.signum() < 0) {
            return 0;
        }
        return Math.min(v.doubleValue(), getMax());
    }

    /**
     * Sets the <code>value</code> attribute; a negative value is ignored.
     * <p>
     * <code>value</code> 속성을 지정한다. 음수는 무시한다.
     *
     * @param value the new value / 새 값
     */
    public void setValue(double value) {
        if (value >= 0) {
            setAttribute("value", NumberSyntax.format(value));
        }
    }

    /**
     * Returns the position: value divided by maximum, or -1 if the progress bar is indeterminate.
     * <p>
     * 위치를 반환한다: 값을 최댓값으로 나눈 것. 불확정 상태이면 -1.
     *
     * @return the position / 위치
     */
    public double getPosition() {
        return isDeterminate() ? getValue() / getMax() : -1;
    }
}
