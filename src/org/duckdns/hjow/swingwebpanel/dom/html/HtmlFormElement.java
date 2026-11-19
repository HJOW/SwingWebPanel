package org.duckdns.hjow.swingwebpanel.dom.html;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.html.EncodingLabels;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The <code>form</code> element (HTML Standard 4.10.3): the owner of a group of form controls. It provides the <code>elements</code> collection, the
 * enumerated attributes <code>method</code> and <code>enctype</code>, and the algorithms of the standard: <code>submit()</code> and
 * <code>requestSubmit()</code> (the form submission algorithm: interactive constraint validation, the cancelable <code>submit</code> event,
 * constructing the entry list, picking the encoding, encoding the entries for the method and enctype and the action URL), <code>reset()</code> (the
 * cancelable <code>reset</code> event and the reset algorithm of every resettable control), <code>checkValidity()</code>, <code>reportValidity()</code> and
 * implicit submission. The navigation itself is up to the {@link FormHost}.
 * <p>
 * <code>form</code> 요소(HTML 표준 4.10.3): 폼 컨트롤 묶음의 소유자. <code>elements</code> 컬렉션, 열거형 속성 <code>method</code>, <code>enctype</code>, 표준의 알고리즘을
 * 제공한다: <code>submit()</code> 과 <code>requestSubmit()</code>(폼 제출 알고리즘: 대화형 제약 검증, 취소 가능한 <code>submit</code> 이벤트, 항목 목록 만들기, 인코딩
 * 고르기, method 와 enctype 과 action URL 에 맞게 항목 인코딩), <code>reset()</code>(취소 가능한 <code>reset</code> 이벤트와 초기화 가능한 모든 컨트롤의 초기화 알고리즘),
 * <code>checkValidity()</code>, <code>reportValidity()</code>, 암묵적 제출. 이동 자체는 {@link FormHost} 의 몫이다.
 */
public final class HtmlFormElement extends HtmlControl {

    /** The enctype of simple forms. / 단순 폼의 enctype. */
    public static final String ENCTYPE_URLENCODED = "application/x-www-form-urlencoded";

    /** The enctype of forms with files. / 파일이 있는 폼의 enctype. */
    public static final String ENCTYPE_MULTIPART = "multipart/form-data";

    /** The enctype of plain text forms. / 일반 텍스트 폼의 enctype. */
    public static final String ENCTYPE_TEXT = "text/plain";

    /** True while the <code>submit</code> event and the validation of a submission run. / 제출의 <code>submit</code> 이벤트와 검증이 실행되는 동안 true. */
    private boolean firingSubmission;

    /** True while the entry list is constructed. / 항목 목록을 만드는 동안 true. */
    private boolean constructingEntries;

    /** True while the form is being reset. / 폼을 초기화하는 동안 true. */
    private boolean resetting;

    /** The cached elements collection. / 캐시한 elements 컬렉션. */
    private FormControlsCollection elements;

    /**
     * Creates a form.
     * <p>
     * 폼을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlFormElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 컨트롤 목록
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the listed elements whose form owner is this form, in tree order (including image buttons).
     * <p>
     * 폼 소유자가 이 폼인 목록 요소를 트리 순서로 반환한다(이미지 버튼 포함).
     *
     * @return the elements (a new list) / 요소(새 목록)
     */
    List<HtmlListedElement> listedElements() {
        List<HtmlListedElement> out = new ArrayList<HtmlListedElement>();
        Node root = getRootNode();
        // 같은 트리의 모든 노드를 보며 폼 소유자가 이 폼인 목록 요소를 모은다.
        for (Node n = Node.nextInTree(root, root); n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof HtmlListedElement && ((HtmlListedElement) n).getForm() == this) {
                out.add((HtmlListedElement) n);
            }
        }
        return out;
    }

    /**
     * Returns the live <code>elements</code> collection (the same object every time).
     * <p>
     * 라이브 <code>elements</code> 컬렉션을 반환한다(매번 같은 객체).
     *
     * @return the collection / 컬렉션
     */
    public FormControlsCollection getElements() {
        if (elements == null) {
            elements = new FormControlsCollection(this);
        }
        return elements;
    }

    /**
     * Returns the <code>length</code> IDL attribute: the number of controls in <code>elements</code>.
     * <p>
     * <code>length</code> IDL 속성을 반환한다: <code>elements</code> 의 컨트롤 수.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return getElements().getLength();
    }

    /**
     * Returns the value of a named property of the form (<code>form[name]</code>): the control or <code>img</code> element with that id or
     * name, or a {@link RadioNodeList} if several controls share the name.
     * <p>
     * 폼의 이름 있는 프로퍼티(<code>form[name]</code>)의 값을 반환한다: id 나 name 이 같은 컨트롤 또는 <code>img</code> 요소. 여러 컨트롤이 이름을 공유하면
     * {@link RadioNodeList}.
     *
     * @param name the name / 이름
     * @return an element, a list of elements or null / 요소, 요소 목록 또는 null
     */
    public Object getNamedProperty(String name) {
        Object o = getElements().namedItem(name);
        if (o != null) {
            return o;
        }
        if (name == null || name.length() == 0) {
            return null;
        }
        List<HtmlListedElement> listed = listedElements();
        // elements 에 오르지 않는 이미지 버튼도 id 나 name 으로 찾는다.
        for (int i = 0; i < listed.size(); i++) {
            HtmlListedElement c = listed.get(i);
            if (!c.isInFormElements() && (name.equals(c.getId()) || name.equals(c.getAttribute("name")))) {
                return c;
            }
        }
        // 컨트롤이 없으면 이 폼 안의 img 요소를 id 나 name 으로 찾는다.
        for (Node n = Node.nextInTree(this, this); n != null; n = Node.nextInTree(n, this)) {
            if (HtmlControl.isHtml(n, "img") && (name.equals(((Element) n).getId()) || name.equals(((Element) n).getAttribute("name")))) {
                return n;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 속성
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>method</code> IDL attribute: <code>get</code> (the default), <code>post</code> or <code>dialog</code>.
     * <p>
     * <code>method</code> IDL 속성을 반환한다: <code>get</code>(기본), <code>post</code>, <code>dialog</code>.
     *
     * @return the method state / method 상태
     */
    public String getMethod() {
        return methodOf(getAttribute("method"));
    }

    /**
     * Maps an attribute value to a method state.
     * <p>
     * 속성 값을 method 상태로 바꾼다.
     *
     * @param v the attribute value or null / 속성 값 또는 null
     * @return get, post or dialog / get, post, dialog
     */
    static String methodOf(String v) {
        String m = v == null ? "" : HtmlText.lower(v);
        return "post".equals(m) || "dialog".equals(m) ? m : "get";
    }

    /**
     * Returns the <code>enctype</code> IDL attribute: <code>application/x-www-form-urlencoded</code> (the default),
     * <code>multipart/form-data</code> or <code>text/plain</code>.
     * <p>
     * <code>enctype</code> IDL 속성을 반환한다: <code>application/x-www-form-urlencoded</code>(기본), <code>multipart/form-data</code>, <code>text/plain</code>.
     *
     * @return the enctype state / enctype 상태
     */
    public String getEnctype() {
        return enctypeOf(getAttribute("enctype"));
    }

    /**
     * Maps an attribute value to an enctype state.
     * <p>
     * 속성 값을 enctype 상태로 바꾼다.
     *
     * @param v the attribute value or null / 속성 값 또는 null
     * @return the enctype / enctype
     */
    static String enctypeOf(String v) {
        String m = v == null ? "" : HtmlText.lower(v);
        return ENCTYPE_MULTIPART.equals(m) || ENCTYPE_TEXT.equals(m) ? m : ENCTYPE_URLENCODED;
    }

    /**
     * Returns the <code>action</code> IDL attribute: the <code>action</code> attribute resolved against the document, or the URL of the document
     * if the attribute is missing or empty.
     * <p>
     * <code>action</code> IDL 속성을 반환한다: 문서 기준으로 해석한 <code>action</code> 속성. 속성이 없거나 비어 있으면 문서의 URL.
     *
     * @return the URL / URL
     */
    public String getAction() {
        return resolveAction(getAttribute("action"));
    }

    /**
     * Resolves an action value against the document: an empty value stands for the document URL.
     * <p>
     * action 값을 문서 기준으로 해석한다: 빈 값은 문서의 URL 이다.
     *
     * @param action the attribute value or null / 속성 값 또는 null
     * @return the absolute URL, or the raw text if it cannot be resolved / 절대 URL. 해석할 수 없으면 원래 문자열
     */
    String resolveAction(String action) {
        Document d = document();
        if (action == null || action.length() == 0) {
            return d.getUrl();
        }
        try {
            return UrlUtil.resolve(d.getBaseUri(), action);
        } catch (RuntimeException e) {
            return action;
        }
    }

    /**
     * Sets the <code>action</code> content attribute (the <code>action</code> IDL attribute reflects it).
     * <p>
     * <code>action</code> 콘텐츠 속성을 지정한다(<code>action</code> IDL 속성이 이를 반영한다).
     *
     * @param value the new value / 새 값
     */
    public void setAction(String value) {
        setAttribute("action", value);
    }

    /**
     * Sets the <code>method</code> content attribute (the <code>method</code> IDL attribute reflects it as an enumerated attribute).
     * <p>
     * <code>method</code> 콘텐츠 속성을 지정한다(<code>method</code> IDL 속성은 열거형 속성으로 이를 반영한다).
     *
     * @param value the new value / 새 값
     */
    public void setMethod(String value) {
        setAttribute("method", value);
    }

    /**
     * Sets the <code>enctype</code> content attribute (the <code>enctype</code> IDL attribute reflects it as an enumerated attribute).
     * <p>
     * <code>enctype</code> 콘텐츠 속성을 지정한다(<code>enctype</code> IDL 속성은 열거형 속성으로 이를 반영한다).
     *
     * @param value the new value / 새 값
     */
    public void setEnctype(String value) {
        setAttribute("enctype", value);
    }

    /**
     * Tells whether the <code>novalidate</code> attribute is present.
     * <p>
     * <code>novalidate</code> 속성이 있는지 알려 준다.
     *
     * @return true if validation is turned off / 검증이 꺼져 있으면 true
     */
    public boolean isNoValidate() {
        return hasAttribute("novalidate");
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 제약 검증
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the submittable elements whose form owner is this form and which are candidates for constraint validation but violate their
     * constraints, in tree order.
     * <p>
     * 폼 소유자가 이 폼인 제출 가능 요소 중 제약 검증 대상이면서 제약을 어기는 것을 트리 순서로 반환한다.
     *
     * @return the invalid controls / 올바르지 않은 컨트롤
     */
    private List<HtmlListedElement> invalidControls() {
        List<HtmlListedElement> out = new ArrayList<HtmlListedElement>();
        List<HtmlListedElement> all = listedElements();
        // 제출 가능한 요소 중 검증 대상이며 제약을 어기는 것을 모은다.
        for (int i = 0; i < all.size(); i++) {
            HtmlListedElement c = all.get(i);
            if (c.isSubmittable() && !c.isBarred() && c.validityFlags() != 0) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Implements <code>checkValidity()</code> of the form (static validation): true if every control satisfies its constraints, otherwise a
     * cancelable <code>invalid</code> event is fired at each invalid control and false is returned.
     * <p>
     * 폼의 <code>checkValidity()</code> 를 구현한다(정적 검증): 모든 컨트롤이 제약을 만족하면 true, 아니면 올바르지 않은 각 컨트롤에 취소 가능한
     * <code>invalid</code> 이벤트를 발생시키고 false 를 반환한다.
     *
     * @return true if valid / 올바르면 true
     */
    public boolean checkValidity() {
        List<HtmlListedElement> invalid = invalidControls();
        // 올바르지 않은 컨트롤마다 invalid 이벤트를 보낸다.
        for (int i = 0; i < invalid.size(); i++) {
            invalid.get(i).fireEvent("invalid", false, true);
        }
        return invalid.isEmpty();
    }

    /**
     * Implements <code>reportValidity()</code> of the form (interactive validation): like {@link #checkValidity()}, and the first invalid control
     * whose <code>invalid</code> event was not cancelled gets the focus and the host shows its message.
     * <p>
     * 폼의 <code>reportValidity()</code> 를 구현한다(대화형 검증): {@link #checkValidity()} 와 같고, <code>invalid</code> 이벤트가 취소되지 않은 첫 컨트롤이 포커스를
     * 받고 호스트가 그 메시지를 보여 준다.
     *
     * @return true if valid / 올바르면 true
     */
    public boolean reportValidity() {
        List<HtmlListedElement> invalid = invalidControls();
        HtmlListedElement firstUnhandled = null;
        // 올바르지 않은 컨트롤마다 invalid 이벤트를 보내고 취소되지 않은 첫 컨트롤을 기억한다.
        for (int i = 0; i < invalid.size(); i++) {
            if (invalid.get(i).fireEvent("invalid", false, true) && firstUnhandled == null) {
                firstUnhandled = invalid.get(i);
            }
        }
        if (firstUnhandled != null) {
            firstUnhandled.report();
        }
        return invalid.isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 초기화
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Implements <code>reset()</code>: fires a cancelable <code>reset</code> event and, unless it was cancelled, runs the reset algorithm of every
     * resettable control of the form.
     * <p>
     * <code>reset()</code> 를 구현한다: 취소 가능한 <code>reset</code> 이벤트를 발생시키고, 취소되지 않았으면 폼의 초기화 가능한 모든 컨트롤의 초기화 알고리즘을
     * 실행한다.
     */
    public void reset() {
        if (resetting) {
            return;
        }
        resetting = true;
        try {
            if (fireEvent("reset", true, true)) {
                List<HtmlListedElement> all = listedElements();
                // 초기화 가능한 컨트롤을 차례로 초기화한다.
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).isResettable()) {
                        all.get(i).resetControl();
                    }
                }
            }
        } finally {
            resetting = false;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 제출
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Implements <code>submit()</code>: submits the form without validation and without a <code>submit</code> event.
     * <p>
     * <code>submit()</code> 를 구현한다: 검증도 <code>submit</code> 이벤트도 없이 폼을 제출한다.
     */
    public void submit() {
        submitFrom(this, true);
    }

    /**
     * Implements <code>requestSubmit(submitter)</code>: submits the form as if the submitter had been clicked: constraints are validated
     * (unless turned off) and the <code>submit</code> event is fired.
     * <p>
     * <code>requestSubmit(submitter)</code> 를 구현한다: submitter 를 클릭한 것처럼 폼을 제출한다. 제약을 검증하고(꺼져 있지 않으면) <code>submit</code> 이벤트를 발생시킨다.
     *
     * @param submitter a submit button of this form, or null / 이 폼의 제출 버튼 또는 null
     * @throws DomException NotFoundError if the submitter's form owner is not this form, TypeError-like InvalidStateError if it is not a submit button / submitter 의 폼 소유자가 이 폼이 아니면 NotFoundError, 제출 버튼이 아니면 InvalidStateError
     */
    public void requestSubmit(Element submitter) {
        if (submitter != null) {
            boolean button = (submitter instanceof HtmlButtonElement && ((HtmlButtonElement) submitter).isSubmitButton())
                    || (submitter instanceof HtmlInputElement && ((HtmlInputElement) submitter).isSubmitButton());
            if (!button) {
                throw DomException.invalidState("the submitter is not a submit button");
            }
            if (((HtmlListedElement) submitter).getForm() != this) {
                throw DomException.notFound("the submitter is not owned by this form");
            }
        }
        submitFrom(submitter == null ? this : submitter, false);
    }

    /**
     * Picks the encoding of the submission ("picking an encoding for the form"): the first usable label of the <code>accept-charset</code>
     * attribute, otherwise the encoding of the document; UTF-16 and the replacement encoding become UTF-8.
     * <p>
     * 제출의 인코딩을 고른다("picking an encoding for the form"): <code>accept-charset</code> 속성에서 처음 쓸 수 있는 라벨, 없으면 문서의 인코딩. UTF-16 과
     * replacement 인코딩은 UTF-8 이 된다.
     *
     * @return the canonical name of the encoding / 인코딩의 표준 이름
     */
    String pickEncoding() {
        String name = null;
        String accept = getAttribute("accept-charset");
        if (accept != null) {
            List<String> labels = HtmlText.splitSpaces(accept);
            // 라벨을 차례로 보며 알려진 첫 인코딩을 쓴다.
            for (int i = 0; i < labels.size() && name == null; i++) {
                String c = EncodingLabels.canonicalName(labels.get(i));
                if (c != null) {
                    name = c;
                }
            }
            if (name == null) {
                return EncodingLabels.UTF_8;
            }
        } else {
            name = EncodingLabels.canonicalName(document().getCharacterSet());
        }
        if (name == null || EncodingLabels.UTF_16BE.equals(name) || EncodingLabels.UTF_16LE.equals(name) || EncodingLabels.REPLACEMENT.equals(name)) {
            return EncodingLabels.UTF_8;
        }
        return name;
    }

    /**
     * Returns the default button of the form: its first submit button in tree order.
     * <p>
     * 폼의 기본 버튼을 반환한다: 트리 순서로 첫 제출 버튼.
     *
     * @return the button or null / 버튼 또는 null
     */
    public Element getDefaultButton() {
        List<HtmlListedElement> all = listedElements();
        // 폼의 컨트롤을 순서대로 보며 처음 만나는 제출 버튼을 찾는다.
        for (int i = 0; i < all.size(); i++) {
            HtmlListedElement c = all.get(i);
            if ((c instanceof HtmlButtonElement && ((HtmlButtonElement) c).isSubmitButton())
                    || (c instanceof HtmlInputElement && ((HtmlInputElement) c).isSubmitButton())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Implements implicit submission (the user pressed Enter in a text field): if the form has a default button it receives a click (nothing
     * happens if the button is disabled); without one the form is submitted only if it has at most one field that blocks implicit submission.
     * The host calls it when the Enter key is pressed in a text control.
     * <p>
     * 암묵적 제출을 구현한다(사용자가 텍스트 필드에서 Enter 를 눌렀다): 폼에 기본 버튼이 있으면 그 버튼에 클릭을 보낸다(버튼이 비활성이면 아무 일도 없다). 기본 버튼이
     * 없으면 암묵적 제출을 막는 필드가 하나 이하일 때만 폼을 제출한다. 호스트가 텍스트 컨트롤에서 Enter 키를 눌렀을 때 부른다.
     */
    public void implicitSubmit() {
        Element def = getDefaultButton();
        if (def != null) {
            if (!((HtmlListedElement) def).isDisabled()) {
                FormActivation.click(def);
            }
            return;
        }
        int blockers = 0;
        List<HtmlListedElement> all = listedElements();
        // 암묵적 제출을 막는 필드를 센다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof HtmlInputElement && ((HtmlInputElement) all.get(i)).blocksImplicitSubmission()) {
                blockers++;
            }
        }
        if (blockers <= 1) {
            submitFrom(this, false);
        }
    }

    /**
     * The form submission algorithm (HTML Standard 4.10.22.3). With <code>fromSubmitMethod</code> the validation and the <code>submit</code> event are skipped.
     * The result is handed to {@link FormHost#submit(FormSubmission, Element)}; nothing happens for a form that is not connected to its document
     * (it "cannot navigate"), for <code>method=dialog</code> (there are no dialogs), for an action that cannot be resolved, or without a host.
     * <p>
     * 폼 제출 알고리즘(HTML 표준 4.10.22.3). <code>fromSubmitMethod</code> 이면 검증과 <code>submit</code> 이벤트를 건너뛴다. 결과는
     * {@link FormHost#submit(FormSubmission, Element)} 에 넘긴다. 문서에 연결되지 않은 폼("cannot navigate"), <code>method=dialog</code>(dialog 가 없다), 해석할 수
     * 없는 action, 호스트가 없는 경우에는 아무 일도 없다.
     *
     * @param submitter        the submit button, or this form if no button started the submission / 제출 버튼. 버튼이 아니면 이 폼
     * @param fromSubmitMethod true for <code>submit()</code> / <code>submit()</code> 이면 true
     */
    void submitFrom(Element submitter, boolean fromSubmitMethod) {
        if (!isConnected() || constructingEntries) {
            return;
        }
        Element button = submitter == this ? null : submitter;
        if (!fromSubmitMethod) {
            if (firingSubmission) {
                return;
            }
            firingSubmission = true;
            try {
                boolean noValidate = isNoValidate() || (button != null && button.hasAttribute("formnovalidate"));
                if (!noValidate && !reportValidity()) {
                    return;
                }
                SubmitEvent ev = new SubmitEvent("submit", true, true, button);
                if (!dispatchTrustedEvent(ev)) {
                    return;
                }
            } finally {
                firingSubmission = false;
            }
        }
        if (!isConnected()) {
            return;
        }
        String encoding = pickEncoding();
        List<FormEntry> entries;
        constructingEntries = true;
        try {
            entries = FormEntryList.construct(this, button, encoding);
        } finally {
            constructingEntries = false;
        }
        FormSubmission s = buildSubmission(button, entries, encoding);
        FormHost h = host();
        if (s != null && h != null) {
            try {
                h.submit(s, this);
            } catch (RuntimeException e) {
                // 호스트의 오류가 스크립트로 번지지 않게 한다.
                System.err.println("FormHost.submit failed: " + e);
            }
        }
    }

    /**
     * Builds the submission for the entries: picks method, action, enctype and target (the submitter's <code>formaction</code>,
     * <code>formmethod</code>, <code>formenctype</code>, <code>formtarget</code> override those of the form) and encodes the entries.
     * <p>
     * 항목에 대한 제출 정보를 만든다: method, action, enctype, target 을 고르고(submitter 의 <code>formaction</code>, <code>formmethod</code>,
     * <code>formenctype</code>, <code>formtarget</code> 이 폼의 것보다 우선한다) 항목을 인코딩한다.
     *
     * @param button   the submitter button or null / submitter 버튼 또는 null
     * @param entries  the entry list / 항목 목록
     * @param encoding the canonical encoding name / 표준 인코딩 이름
     * @return the submission, or null if nothing is to be requested / 제출 정보. 요청할 것이 없으면 null
     */
    private FormSubmission buildSubmission(Element button, List<FormEntry> entries, String encoding) {
        String method = button != null && button.hasAttribute("formmethod") ? methodOf(button.getAttribute("formmethod"))
                : getMethod();
        if ("dialog".equals(method)) {
            return null;
        }
        String actionAttr = button != null && button.hasAttribute("formaction") ? button.getAttribute("formaction") : getAttribute("action");
        String action;
        try {
            action = actionAttr == null || actionAttr.length() == 0 ? document().getUrl() : UrlUtil.resolve(document().getBaseUri(), actionAttr);
        } catch (RuntimeException e) {
            return null;
        }
        String enctype = button != null && button.hasAttribute("formenctype") ? enctypeOf(button.getAttribute("formenctype")) : getEnctype();
        String target = button != null && button.hasAttribute("formtarget") ? button.getAttribute("formtarget") : attr("target");
        Charset cs = FormEncoders.charsetOf(encoding);
        String scheme = HtmlText.lower(UrlUtil.getScheme(action) == null ? "" : UrlUtil.getScheme(action));
        boolean getUrl = "ftp".equals(scheme) || "javascript".equals(scheme) || ("data".equals(scheme) && "post".equals(method));
        if (getUrl) {
            return new FormSubmission(FormSubmission.GET, method, action, null, null, target, encoding, entries);
        }
        if ("get".equals(method)) {
            String query = asciiString(FormEncoders.urlEncoded(entries, cs));
            UrlUtil.Parts p = UrlUtil.parse(action);
            String url = UrlUtil.toString(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, p.port, p.path, query, p.fragment));
            return new FormSubmission(FormSubmission.GET, method, url, null, null, target, encoding, entries);
        }
        byte[] body;
        String type;
        if (ENCTYPE_MULTIPART.equals(enctype)) {
            String boundary = FormEncoders.newBoundary();
            body = FormEncoders.multipart(entries, cs, boundary);
            type = ENCTYPE_MULTIPART + "; boundary=" + boundary;
        } else if (ENCTYPE_TEXT.equals(enctype)) {
            body = FormEncoders.textPlain(entries, cs);
            type = ENCTYPE_TEXT;
        } else {
            body = FormEncoders.urlEncoded(entries, cs);
            type = ENCTYPE_URLENCODED;
        }
        return new FormSubmission(FormSubmission.POST, method, action, type, body, target, encoding, entries);
    }

    /**
     * Converts ASCII bytes to a string.
     * <p>
     * ASCII 바이트를 문자열로 바꾼다.
     *
     * @param b the bytes / 바이트
     * @return the string / 문자열
     */
    private static String asciiString(byte[] b) {
        StringBuilder s = new StringBuilder(b.length);
        // 바이트를 문자로 옮긴다.
        for (int i = 0; i < b.length; i++) {
            s.append((char) (b[i] & 0x7F));
        }
        return s.toString();
    }
}
