package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * The <code>button</code> element (HTML Standard 4.10.6). Its <code>type</code> is the enumerated attribute <code>submit</code> (the default,
 * also for a missing or unknown value), <code>reset</code> or <code>button</code>. Activating a submit button submits the form owner (the
 * button is the submitter), a reset button resets it. Reset and plain buttons are barred from constraint validation.
 * <p>
 * <code>button</code> 요소(HTML 표준 4.10.6). <code>type</code> 은 열거형 속성이며 <code>submit</code>(기본값. 없거나 모르는 값도 포함), <code>reset</code>,
 * <code>button</code> 이다. submit 버튼을 활성화하면 폼 소유자를 제출하고(버튼이 submitter) reset 버튼은 폼을 초기화한다. reset 버튼과 일반 버튼은 제약 검증에서
 * 제외된다.
 */
public final class HtmlButtonElement extends HtmlListedElement {

    /**
     * Creates a button.
     * <p>
     * button 을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlButtonElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * A button is labelable.
     * <p>
     * button 은 라벨을 붙일 수 있다.
     *
     * @return true / true
     */
    public boolean isLabelable() {
        return true;
    }

    /**
     * A button can be a submitter, so it takes part in form submission.
     * <p>
     * button 은 submitter 가 될 수 있으므로 폼 제출에 참여한다.
     *
     * @return true / true
     */
    boolean isSubmittable() {
        return true;
    }

    /**
     * A button takes part in the default focus order.
     * <p>
     * button 은 기본 포커스 순서에 참여한다.
     *
     * @return 0 / 0
     */
    int defaultTabIndex() {
        return 0;
    }

    /**
     * A button has an activation behavior.
     * <p>
     * button 에는 활성화 동작이 있다.
     *
     * @return true unless it is disabled / 비활성이 아니면 true
     */
    boolean hasActivationBehavior() {
        return !isDisabled();
    }

    /**
     * Returns the <code>type</code> IDL attribute: <code>submit</code>, <code>reset</code> or <code>button</code>.
     * <p>
     * <code>type</code> IDL 속성을 반환한다: <code>submit</code>, <code>reset</code>, <code>button</code>.
     *
     * @return the type state / type 상태
     */
    public String getType() {
        String t = HtmlText.lower(attr("type"));
        return "reset".equals(t) || "button".equals(t) ? t : "submit";
    }

    /**
     * Sets the <code>type</code> attribute.
     * <p>
     * <code>type</code> 속성을 지정한다.
     *
     * @param value the new type / 새 종류
     */
    public void setType(String value) {
        setAttribute("type", value);
    }

    /**
     * Tells whether this is a submit button.
     * <p>
     * 제출 버튼인지 알려 준다.
     *
     * @return true for type submit / type 이 submit 이면 true
     */
    public boolean isSubmitButton() {
        return "submit".equals(getType());
    }

    /**
     * Returns the <code>value</code> attribute.
     * <p>
     * <code>value</code> 속성을 반환한다.
     *
     * @return the value, empty if absent / 값. 없으면 빈 문자열
     */
    public String getValue() {
        return attr("value");
    }

    /**
     * Sets the <code>value</code> attribute.
     * <p>
     * <code>value</code> 속성을 지정한다.
     *
     * @param value the new value / 새 값
     */
    public void setValue(String value) {
        setAttribute("value", value);
    }

    /**
     * Reset and plain buttons are barred from constraint validation.
     * <p>
     * reset 버튼과 일반 버튼은 제약 검증에서 제외된다.
     *
     * @return true unless this is a submit button / submit 버튼이 아니면 true
     */
    boolean isBarredBySelf() {
        return !isSubmitButton();
    }

    /**
     * Runs the activation behavior: a submit button submits its form owner, a reset button resets it.
     * <p>
     * 활성화 동작을 실행한다: submit 버튼은 폼 소유자를 제출하고 reset 버튼은 초기화한다.
     *
     * @param click the click event / click 이벤트
     */
    void activate(Event click) {
        HtmlFormElement form = getForm();
        if (form == null) {
            return;
        }
        String t = getType();
        if ("submit".equals(t)) {
            form.submitFrom(this, false);
        } else if ("reset".equals(t)) {
            form.reset();
        }
    }
}
