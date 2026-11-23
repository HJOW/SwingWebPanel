package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The services that the browser component provides to the form controls of a document. Everything that is not part of the DOM model itself
 * is done through this interface: starting the navigation that a form submission asks for, showing why a control is invalid, letting the
 * user pick files and telling the rendering side that the state of a control changed. A document has no host until
 * {@link org.duckdns.hjow.swingwebpanel.dom.Document#setFormHost(FormHost)} installs one; without a host a submission does nothing, an invalid control is
 * only reported by the boolean result of <code>reportValidity()</code>, no file can be chosen, and nobody is notified. All methods are called
 * on the document thread, and an implementation must not throw.
 * <p>
 * 브라우저 컴포넌트가 문서의 폼 컨트롤에 제공하는 서비스. DOM 모델 자체가 아닌 일은 모두 이 인터페이스로 한다: 폼 제출이 요구하는 이동을 시작하기, 컨트롤이
 * 올바르지 않은 이유를 보여 주기, 사용자가 파일을 고르게 하기, 컨트롤의 상태가 바뀌었음을 렌더링 쪽에 알리기. 문서는
 * {@link org.duckdns.hjow.swingwebpanel.dom.Document#setFormHost(FormHost)} 로 설치하기 전에는 호스트가 없다. 호스트가 없으면 제출은 아무 일도 하지 않고, 올바르지 않은
 * 컨트롤은 <code>reportValidity()</code> 의 불리언 결과로만 알려지며, 파일을 고를 수 없고, 아무에게도 통지하지 않는다. 모든 메서드는 문서 스레드에서
 * 호출되며 구현은 예외를 던지면 안 된다.
 */
public interface FormHost {

    /**
     * Starts the navigation that a form submission asks for. The submission may use a scheme that this project does not support (for
     * example <code>mailto:</code>); the host decides what to do with it and normally applies the same navigation policy as for links.
     * <p>
     * 폼 제출이 요구하는 이동을 시작한다. 제출 주소는 이 프로젝트가 지원하지 않는 스킴(예: <code>mailto:</code>)일 수 있으며, 처리 방법은 호스트가 정하고 보통
     * 링크와 같은 이동 정책을 적용한다.
     *
     * @param submission the submission (immutable) / 제출 정보(불변)
     * @param form       the form element that was submitted / 제출된 폼 요소
     */
    void submit(FormSubmission submission, Element form);

    /**
     * Shows the user why a control is invalid ("report the problems with the constraints"): normally the control has just received the
     * focus and the host shows a bubble with the message. It is called after the cancelable <code>invalid</code> event was not cancelled.
     * <p>
     * 컨트롤이 올바르지 않은 이유를 사용자에게 보여 준다("report the problems with the constraints"). 보통 컨트롤은 방금 포커스를 받았고 호스트가 메시지를 말풍선으로
     * 보여 준다. 취소 가능한 <code>invalid</code> 이벤트가 취소되지 않았을 때 호출된다.
     *
     * @param control the invalid control / 올바르지 않은 컨트롤
     * @param message the validation message / 검증 메시지
     */
    void reportInvalid(Element control, String message);

    /**
     * Lets the user choose files for a file upload control that a script activated (<code>click()</code> or a dispatched click); the
     * host normally shows a file chooser and reads the chosen files. User clicks on the control are handled by the host itself, which then
     * calls {@link HtmlInputElement#setSelectedFiles(java.util.List)}.
     * <p>
     * 스크립트가 활성화한 파일 업로드 컨트롤(<code>click()</code> 이나 전달된 click)에 대해 사용자가 파일을 고르게 한다. 호스트는 보통 파일 선택기를 보여 주고 고른
     * 파일을 읽는다. 컨트롤을 직접 클릭한 경우는 호스트가 직접 처리하고 {@link HtmlInputElement#setSelectedFiles(java.util.List)} 를 부른다.
     *
     * @param input    the file input / 파일 input
     * @param multiple true if several files may be chosen / 여러 파일을 고를 수 있으면 true
     * @param accept   the <code>accept</code> attribute (empty if absent) / <code>accept</code> 속성(없으면 빈 문자열)
     * @return the chosen files, or null if the user cancelled or no chooser is available / 고른 파일. 사용자가 취소했거나 선택기가 없으면 null
     */
    FileEntry[] chooseFiles(Element input, boolean multiple, String accept);

    /**
     * Tells that the state of an element (checkedness, value, selectedness, validity ...) changed without a change of the DOM tree or of its
     * attributes, so that the styles that depend on it (<code>:checked</code>, <code>:valid</code>, <code>:placeholder-shown</code> ...)
     * must be computed again and the control must be painted again.
     * <p>
     * DOM 트리나 속성의 변경 없이 요소의 상태(checkedness, 값, selectedness, 유효성 등)가 바뀌었음을 알린다. 그에 의존하는 스타일(<code>:checked</code>,
     * <code>:valid</code>, <code>:placeholder-shown</code> 등)을 다시 계산하고 컨트롤을 다시 그려야 한다.
     *
     * @param element the element whose state changed / 상태가 바뀐 요소
     */
    void stateChanged(Element element);
}
