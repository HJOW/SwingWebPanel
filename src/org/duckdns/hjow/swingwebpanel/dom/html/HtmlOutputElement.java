package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * The <code>output</code> element (HTML Standard 4.10.12): the result of a calculation. It is a listed, labelable and resettable element, but
 * it is not submitted and it is barred from constraint validation. Its value is the text content; the "default value override" and the
 * "value mode" of the standard make reset restore the markup text after a script or the user changed the value.
 * <p>
 * <code>output</code> 요소(HTML 표준 4.10.12): 계산의 결과. 목록에 오르고 라벨을 붙일 수 있으며 초기화할 수 있지만 제출되지 않고 제약 검증에서 제외된다. 값은 텍스트
 * 내용이며, 표준의 "default value override" 와 "value mode" 덕분에 스크립트나 사용자가 값을 바꾼 뒤에도 reset 이 마크업의 글로 되돌린다.
 */
public final class HtmlOutputElement extends HtmlListedElement {

    /** The default value override, or null. / default value override. 없으면 null. */
    private String defaultOverride;

    /** True if the value mode flag is "value" (the value was set), false for "default". / value mode flag 가 "value"(값이 지정됨)이면 true, "default" 이면 false. */
    private boolean valueMode;

    /**
     * Creates an output element.
     * <p>
     * output 요소를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlOutputElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Copies the value mode and the default override to a clone.
     * <p>
     * value mode 와 default override 를 복제본에 복사한다.
     *
     * @param copy the clone / 복제본
     */
    protected void copyStateTo(Element copy) {
        super.copyStateTo(copy);
        HtmlOutputElement o = (HtmlOutputElement) copy;
        o.defaultOverride = defaultOverride;
        o.valueMode = valueMode;
    }

    /**
     * An output cannot be disabled.
     * <p>
     * output 은 비활성이 될 수 없다.
     *
     * @return false / false
     */
    boolean canBeDisabled() {
        return false;
    }

    /**
     * An output is barred from constraint validation.
     * <p>
     * output 은 제약 검증에서 제외된다.
     *
     * @return true / true
     */
    boolean isBarredBySelf() {
        return true;
    }

    /**
     * An output is labelable.
     * <p>
     * output 은 라벨을 붙일 수 있다.
     *
     * @return true / true
     */
    public boolean isLabelable() {
        return true;
    }

    /**
     * An output is resettable.
     * <p>
     * output 은 초기화할 수 있다.
     *
     * @return true / true
     */
    boolean isResettable() {
        return true;
    }

    /**
     * Resets the output: the text becomes the default value and the value mode flag "default".
     * <p>
     * output 을 초기화한다: 글이 기본값이 되고 value mode flag 는 "default" 가 된다.
     */
    void resetControl() {
        String d = getDefaultValue();
        valueMode = false;
        setTextContent(d);
    }

    /**
     * Returns the <code>type</code> IDL attribute: <code>output</code>.
     * <p>
     * <code>type</code> IDL 속성을 반환한다: <code>output</code>.
     *
     * @return "output" / "output"
     */
    public String getType() {
        return "output";
    }

    /**
     * Returns the <code>defaultValue</code> IDL attribute: the default value override, or the text content.
     * <p>
     * <code>defaultValue</code> IDL 속성을 반환한다: default value override, 없으면 텍스트 내용.
     *
     * @return the default value / 기본값
     */
    public String getDefaultValue() {
        return defaultOverride != null ? defaultOverride : getTextContent();
    }

    /**
     * Sets <code>defaultValue</code>: stores the override and, in the "default" value mode, replaces the text.
     * <p>
     * <code>defaultValue</code> 를 지정한다: override 를 저장하고 "default" value mode 이면 글을 바꾼다.
     *
     * @param value the new default value / 새 기본값
     */
    public void setDefaultValue(String value) {
        String v = value == null ? "" : value;
        defaultOverride = v;
        if (!valueMode) {
            setTextContent(v);
        }
    }

    /**
     * Returns the <code>value</code> IDL attribute: the text content.
     * <p>
     * <code>value</code> IDL 속성을 반환한다: 텍스트 내용.
     *
     * @return the value / 값
     */
    public String getValue() {
        return getTextContent();
    }

    /**
     * Sets <code>value</code>: the value mode becomes "value", the current text is remembered as the default and the text is replaced.
     * <p>
     * <code>value</code> 를 지정한다: value mode 가 "value" 가 되고, 현재 글은 기본값으로 기억하며 글을 바꾼다.
     *
     * @param value the new value / 새 값
     */
    public void setValue(String value) {
        if (defaultOverride == null) {
            defaultOverride = getTextContent();
        }
        valueMode = true;
        setTextContent(value == null ? "" : value);
        stateChanged();
    }

    /**
     * Returns the <code>for</code> attribute.
     * <p>
     * <code>for</code> 속성을 반환한다.
     *
     * @return the tokens text / 토큰 문자열
     */
    public String getHtmlFor() {
        return attr("for");
    }

    /**
     * Sets the <code>for</code> attribute.
     * <p>
     * <code>for</code> 속성을 지정한다.
     *
     * @param value the tokens text / 토큰 문자열
     */
    public void setHtmlFor(String value) {
        setAttribute("for", value);
    }
}
