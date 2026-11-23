package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.html.ParserNodeData;

/**
 * The base class of the listed form-associated elements (<code>button</code>, <code>fieldset</code>, <code>input</code>, <code>output</code>,
 * <code>select</code>, <code>textarea</code>): the form owner (the <code>form</code> attribute, the owner that the parser assigned, or the nearest
 * <code>form</code> ancestor), the <code>disabled</code> state including the effect of a disabled <code>fieldset</code>, and the constraint
 * validation API (<code>willValidate</code>, <code>validity</code>, <code>validationMessage</code>, <code>setCustomValidity</code>,
 * <code>checkValidity</code>, <code>reportValidity</code>). Subclasses compute which constraints they violate.
 * <p>
 * 목록에 오르는 폼 연관 요소(<code>button</code>, <code>fieldset</code>, <code>input</code>, <code>output</code>, <code>select</code>, <code>textarea</code>)의
 * 기반 클래스: 폼 소유자(<code>form</code> 속성, 파서가 지정한 소유자, 가장 가까운 <code>form</code> 조상), 비활성 <code>fieldset</code> 의 영향을 포함한
 * <code>disabled</code> 상태, 제약 검증 API(<code>willValidate</code>, <code>validity</code>, <code>validationMessage</code>, <code>setCustomValidity</code>,
 * <code>checkValidity</code>, <code>reportValidity</code>). 하위 클래스가 어떤 제약을 어기는지 계산한다.
 */
public abstract class HtmlListedElement extends HtmlControl {

    /** The message set by <code>setCustomValidity</code>, empty if none. / <code>setCustomValidity</code> 로 지정한 메시지. 없으면 빈 문자열. */
    private String customError = "";

    /** The cached validity view. / 캐시한 validity 뷰. */
    private ValidityState validity;

    /**
     * Creates a listed element.
     * <p>
     * 목록에 오르는 요소를 만든다.
     *
     * @param owner        owner document / 소유 문서
     * @param namespaceURI namespace / 네임스페이스
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name / 로컬 이름
     */
    protected HtmlListedElement(Document owner, String namespaceURI, String prefix, String localName) {
        super(owner, namespaceURI, prefix, localName);
    }

    /**
     * Forgets the owner that the parser assigned: after the element was inserted somewhere, the form owner follows the tree.
     * <p>
     * 파서가 지정한 소유자를 잊는다: 요소가 어딘가에 삽입된 뒤에는 폼 소유자가 트리를 따른다.
     */
    void insertedIntoTree() {
        if (ParserNodeData.getFormOwner(this) != null) {
            ParserNodeData.setFormOwner(this, null);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 폼 소유자와 비활성
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the form owner: the <code>form</code> element that the <code>form</code> attribute names (when connected), otherwise the owner
     * that the HTML parser assigned (when it is still in the same tree), otherwise the nearest <code>form</code> ancestor.
     * <p>
     * 폼 소유자를 반환한다: 연결되어 있으면 <code>form</code> 속성이 가리키는 <code>form</code> 요소, 아니면 HTML 파서가 지정한 소유자(같은 트리에 있을 때), 아니면
     * 가장 가까운 <code>form</code> 조상.
     *
     * @return the form or null / 폼 또는 null
     */
    public final HtmlFormElement getForm() {
        String fa = getAttribute("form");
        if (fa != null && isConnected()) {
            Element e = HtmlControl.findById(this, fa);
            return e instanceof HtmlFormElement ? (HtmlFormElement) e : null;
        }
        Element po = ParserNodeData.getFormOwner(this);
        if (po instanceof HtmlFormElement && po.getRootNode() == getRootNode()) {
            return (HtmlFormElement) po;
        }
        // 가장 가까운 form 조상을 찾는다.
        for (Node p = getParentNode(); p != null; p = p.getParentNode()) {
            if (p instanceof HtmlFormElement) {
                return (HtmlFormElement) p;
            }
        }
        return null;
    }

    /**
     * Returns the value of the <code>name</code> attribute.
     * <p>
     * <code>name</code> 속성 값을 반환한다.
     *
     * @return the name, empty if absent / 이름. 없으면 빈 문자열
     */
    public final String getName() {
        return attr("name");
    }

    /**
     * Sets the <code>name</code> attribute.
     * <p>
     * <code>name</code> 속성을 지정한다.
     *
     * @param value the new name / 새 이름
     */
    public final void setName(String value) {
        setAttribute("name", value);
    }

    /**
     * Tells whether the element can be disabled (all listed elements except <code>output</code>).
     * <p>
     * 요소가 비활성이 될 수 있는지(<code>output</code> 을 제외한 모든 목록 요소) 알려 준다.
     *
     * @return true if the element has the disabled state / disabled 상태가 있으면 true
     */
    boolean canBeDisabled() {
        return true;
    }

    /**
     * Tells whether the element is a disabled form control: its <code>disabled</code> attribute is set, or it is a descendant of a
     * <code>fieldset</code> whose <code>disabled</code> attribute is set and it is not inside that fieldset's first <code>legend</code> child.
     * <p>
     * 요소가 비활성 폼 컨트롤인지 알려 준다: <code>disabled</code> 속성이 있거나, <code>disabled</code> 속성이 있는 <code>fieldset</code> 의 후손이면서 그 fieldset 의
     * 첫 <code>legend</code> 자식 안에 있지 않다.
     *
     * @return true if disabled / 비활성이면 true
     */
    public final boolean isDisabled() {
        if (!canBeDisabled()) {
            return false;
        }
        return hasAttribute("disabled") || isDisabledByFieldset();
    }

    /**
     * Tells whether a disabled <code>fieldset</code> ancestor disables this element.
     * <p>
     * 비활성 <code>fieldset</code> 조상이 이 요소를 비활성으로 만드는지 알려 준다.
     *
     * @return true if disabled by a fieldset / fieldset 때문에 비활성이면 true
     */
    final boolean isDisabledByFieldset() {
        Node child = this;
        // 위로 올라가며 disabled fieldset 을 찾고, 지나온 자식이 그 fieldset 의 첫 legend 이면 예외로 한다.
        for (Node p = getParentNode(); p instanceof Element; child = p, p = p.getParentNode()) {
            if (p instanceof HtmlFieldSetElement && ((Element) p).hasAttribute("disabled")) {
                Element legend = ((HtmlFieldSetElement) p).getFirstLegend();
                if (legend == null || legend != child) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tells whether the control is mutable (the user and scripts may change its value): not disabled and, where the attribute applies, not
     * read-only.
     * <p>
     * 컨트롤이 mutable 인지(사용자와 스크립트가 값을 바꿀 수 있는지) 알려 준다: 비활성이 아니며, 속성이 적용되는 곳에서는 읽기 전용이 아니다.
     *
     * @return true if mutable / mutable 이면 true
     */
    boolean isMutable() {
        return !isDisabled();
    }

    /**
     * Tells whether the control is submitted with the form (<code>button</code>, <code>input</code>, <code>select</code>, <code>textarea</code>).
     * <p>
     * 컨트롤이 폼과 함께 제출되는지(<code>button</code>, <code>input</code>, <code>select</code>, <code>textarea</code>) 알려 준다.
     *
     * @return true if submittable / 제출 가능하면 true
     */
    boolean isSubmittable() {
        return false;
    }

    /**
     * Tells whether the control has a reset algorithm (<code>input</code>, <code>output</code>, <code>select</code>, <code>textarea</code>).
     * <p>
     * 컨트롤에 초기화 알고리즘이 있는지(<code>input</code>, <code>output</code>, <code>select</code>, <code>textarea</code>) 알려 준다.
     *
     * @return true if resettable / 초기화 가능하면 true
     */
    boolean isResettable() {
        return false;
    }

    /**
     * Runs the reset algorithm of the control (only called if {@link #isResettable()}).
     * <p>
     * 컨트롤의 초기화 알고리즘을 실행한다({@link #isResettable()} 일 때만 호출된다).
     */
    void resetControl() {
        // 기본: 초기화할 상태가 없다.
    }

    /**
     * Tells whether the element is listed in <code>form.elements</code> (all listed elements except image buttons).
     * <p>
     * 요소가 <code>form.elements</code> 에 오르는지(이미지 버튼을 제외한 모든 목록 요소) 알려 준다.
     *
     * @return true if it is in the elements collection / elements 컬렉션에 오르면 true
     */
    boolean isInFormElements() {
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 제약 검증
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether the element, by its kind and state alone, is barred from constraint validation (for example a <code>fieldset</code>,
     * an <code>output</code>, a read-only text control, a reset button). The default is false.
     * <p>
     * 요소가 종류와 상태만으로 제약 검증에서 제외되는지(예: <code>fieldset</code>, <code>output</code>, 읽기 전용 텍스트 컨트롤, reset 버튼) 알려 준다.
     * 기본은 false.
     *
     * @return true if barred / 제외되면 true
     */
    boolean isBarredBySelf() {
        return false;
    }

    /**
     * Tells whether the element is barred from constraint validation: barred by its kind, disabled, or inside a <code>datalist</code>.
     * <p>
     * 요소가 제약 검증에서 제외되는지 알려 준다: 종류로 제외, 비활성, <code>datalist</code> 안.
     *
     * @return true if barred / 제외되면 true
     */
    final boolean isBarred() {
        if (isBarredBySelf() || isDisabled()) {
            return true;
        }
        // 조상 중에 datalist 가 있으면 검증에서 제외된다.
        for (Node p = getParentNode(); p != null; p = p.getParentNode()) {
            if (HtmlControl.isHtml(p, "datalist")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes the flags of the constraints that the element violates (bits of {@link ValidityState}, without the custom error).
     * <p>
     * 요소가 어기는 제약의 플래그를 계산한다({@link ValidityState} 의 비트. 사용자 정의 오류는 제외).
     *
     * @return the flags / 플래그
     */
    int computeFlags() {
        return 0;
    }

    /**
     * Returns the flags of the violated constraints including the custom error.
     * <p>
     * 사용자 정의 오류를 포함해 어긴 제약의 플래그를 반환한다.
     *
     * @return the flags / 플래그
     */
    final int validityFlags() {
        return computeFlags() | (customError.length() > 0 ? ValidityState.CUSTOM_ERROR : 0);
    }

    /**
     * Returns the <code>willValidate</code> IDL attribute: whether the element is a candidate for constraint validation.
     * <p>
     * <code>willValidate</code> IDL 속성을 반환한다: 요소가 제약 검증의 대상인지.
     *
     * @return true if it is validated / 검증 대상이면 true
     */
    public final boolean willValidate() {
        return !isBarred();
    }

    /**
     * Returns the same value as {@link #willValidate()} in the form of a bean property (for the binding of the <code>willValidate</code> IDL attribute).
     * <p>
     * {@link #willValidate()} 와 같은 값을 빈 프로퍼티 형태로 반환한다(<code>willValidate</code> IDL 속성의 연결용).
     *
     * @return true if the element is a candidate for constraint validation / 제약 검증 대상이면 true
     */
    public final boolean isWillValidate() {
        return !isBarred();
    }

    /**
     * Returns the <code>validity</code> IDL attribute (the same live object every time).
     * <p>
     * <code>validity</code> IDL 속성을 반환한다(매번 같은 라이브 객체).
     *
     * @return the validity state / validity 상태
     */
    public final ValidityState getValidity() {
        if (validity == null) {
            validity = new ValidityState(this);
        }
        return validity;
    }

    /**
     * Returns the <code>validationMessage</code> IDL attribute: the custom message or an English default message of the first violated
     * constraint; empty if the element is not validated or valid.
     * <p>
     * <code>validationMessage</code> IDL 속성을 반환한다: 사용자 정의 메시지 또는 처음 어긴 제약의 영어 기본 메시지. 검증 대상이 아니거나 올바르면 빈 문자열.
     *
     * @return the message / 메시지
     */
    public final String getValidationMessage() {
        if (isBarred()) {
            return "";
        }
        int f = validityFlags();
        if (f == 0) {
            return "";
        }
        if ((f & ValidityState.CUSTOM_ERROR) != 0) {
            return customError;
        }
        String specific = messageFor(f);
        return specific != null ? specific : "Please correct this value.";
    }

    /**
     * Returns the default message of the first violated constraint among the given flags.
     * <p>
     * 주어진 플래그 중 처음 어긴 제약의 기본 메시지를 반환한다.
     *
     * @param f the flags (without the custom error) / 플래그(사용자 정의 오류 제외)
     * @return the message / 메시지
     */
    String messageFor(int f) {
        if ((f & ValidityState.VALUE_MISSING) != 0) {
            return "Please fill out this field.";
        }
        if ((f & ValidityState.TYPE_MISMATCH) != 0) {
            return "Please enter a valid value.";
        }
        if ((f & ValidityState.PATTERN_MISMATCH) != 0) {
            String t = getAttribute("title");
            return "Please match the requested format." + (t != null && t.length() > 0 ? " " + t : "");
        }
        return "Please enter a valid value.";
    }

    /**
     * Sets the custom validity message; an empty string clears it.
     * <p>
     * 사용자 정의 검증 메시지를 지정한다. 빈 문자열이면 지운다.
     *
     * @param message the message / 메시지
     */
    public final void setCustomValidity(String message) {
        customError = message == null ? "" : message;
        stateChanged();
    }

    /**
     * Implements <code>checkValidity()</code>: true if the element is not validated or satisfies its constraints; otherwise a cancelable
     * <code>invalid</code> event (not bubbling) is fired at it and false is returned.
     * <p>
     * <code>checkValidity()</code> 를 구현한다: 요소가 검증 대상이 아니거나 제약을 만족하면 true. 아니면 취소 가능한 <code>invalid</code> 이벤트(버블링 안 함)를
     * 발생시키고 false 를 반환한다.
     *
     * @return true if valid / 올바르면 true
     */
    public final boolean checkValidity() {
        if (isBarred() || validityFlags() == 0) {
            return true;
        }
        fireEvent("invalid", false, true);
        return false;
    }

    /**
     * Implements <code>reportValidity()</code>: like {@link #checkValidity()}, and when no listener cancelled the <code>invalid</code> event the
     * control gets the focus and the host shows the validation message.
     * <p>
     * <code>reportValidity()</code> 를 구현한다: {@link #checkValidity()} 와 같고, 리스너가 <code>invalid</code> 이벤트를 취소하지 않았으면 컨트롤이 포커스를 받고
     * 호스트가 검증 메시지를 보여 준다.
     *
     * @return true if valid / 올바르면 true
     */
    public final boolean reportValidity() {
        if (isBarred() || validityFlags() == 0) {
            return true;
        }
        if (fireEvent("invalid", false, true)) {
            report();
        }
        return false;
    }

    /**
     * Focuses the control and asks the host to show the validation message.
     * <p>
     * 컨트롤에 포커스를 주고 호스트에 검증 메시지를 보여 달라고 요청한다.
     */
    final void report() {
        FocusManager.focus(this, document().isWindowFocused());
        FormHost h = host();
        if (h != null) {
            try {
                h.reportInvalid(this, getValidationMessage());
            } catch (RuntimeException e) {
                // 호스트의 오류가 검증을 깨뜨리지 않게 한다.
                System.err.println("FormHost.reportInvalid failed: " + e);
            }
        }
    }

    /**
     * Tells whether the control satisfies its constraints for the pseudo-classes <code>:valid</code> and <code>:invalid</code> (null if it is not validated).
     * <p>
     * 컨트롤이 <code>:valid</code>, <code>:invalid</code> 의사 클래스에서 제약을 만족하는지 알려 준다(검증 대상이 아니면 null).
     *
     * @return TRUE or FALSE, or null if barred / TRUE 또는 FALSE. 제외되면 null
     */
    final Boolean validityOrNull() {
        if (isBarred()) {
            return null;
        }
        return validityFlags() == 0 ? Boolean.TRUE : Boolean.FALSE;
    }
}
