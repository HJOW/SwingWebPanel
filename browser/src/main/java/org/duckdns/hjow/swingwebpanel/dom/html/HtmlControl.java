package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.InputEvent;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The base class of the HTML elements that take part in forms (<code>form</code>, <code>input</code>, <code>button</code>, <code>select</code>,
 * <code>option</code>, <code>textarea</code>, <code>label</code>, <code>fieldset</code> ...). It connects an element to the form support of its
 * document, offers the helpers that the elements share (attribute shortcuts, events of the user agent, <code>labels</code>, state notifications), and
 * runs the activation behavior of a click event that is dispatched on such an element (see {@link FormActivation}).
 * <p>
 * 폼에 참여하는 HTML 요소(<code>form</code>, <code>input</code>, <code>button</code>, <code>select</code>, <code>option</code>, <code>textarea</code>,
 * <code>label</code>, <code>fieldset</code> 등)의 기반 클래스. 요소를 문서의 폼 지원에 연결하고, 요소들이 공유하는 도우미(속성 단축 접근, 사용자 에이전트의
 * 이벤트, <code>labels</code>, 상태 통지)를 제공하며, 이런 요소에 전달된 click 이벤트의 활성화 동작({@link FormActivation} 참조)을 실행한다.
 */
public abstract class HtmlControl extends Element {

    /** The cached labels list. / 캐시한 labels 목록. */
    private FormElementList labels;

    /**
     * Creates an element of the HTML form layer and connects it to the form support of its document.
     * <p>
     * HTML 폼 계층의 요소를 만들고 문서의 폼 지원에 연결한다.
     *
     * @param owner        owner document / 소유 문서
     * @param namespaceURI namespace (the HTML namespace) / 네임스페이스(HTML 네임스페이스)
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name / 로컬 이름
     */
    protected HtmlControl(Document owner, String namespaceURI, String prefix, String localName) {
        super(owner, namespaceURI, prefix, localName);
        owner.getFormSupport().controlCreated();
    }

    /**
     * Connects the element to the form support of the document that adopted it.
     * <p>
     * 요소를 입양한 문서의 폼 지원에 연결한다.
     *
     * @param oldDocument the previous owner / 이전 소유 문서
     */
    protected void adoptedFrom(Document oldDocument) {
        super.adoptedFrom(oldDocument);
        document().getFormSupport().controlCreated();
        labels = null;
    }

    /**
     * Called after this element was inserted into a tree (also a tree that is not connected to a document). The default does nothing.
     * <p>
     * 이 요소가 트리(문서에 연결되지 않은 트리도 포함)에 삽입된 뒤 호출된다. 기본은 아무것도 하지 않는다.
     */
    void insertedIntoTree() {
        // 기본: 삽입에 반응하지 않는다.
    }

    /**
     * Called after this element was removed from a tree. The default does nothing.
     * <p>
     * 이 요소가 트리에서 제거된 뒤 호출된다. 기본은 아무것도 하지 않는다.
     */
    void removedFromTree() {
        // 기본: 제거에 반응하지 않는다.
    }

    /**
     * Returns the value of an attribute, or the empty string if it is absent.
     * <p>
     * 속성 값을 반환한다. 없으면 빈 문자열.
     *
     * @param name the attribute name / 속성 이름
     * @return the value / 값
     */
    final String attr(String name) {
        String v = getAttribute(name);
        return v == null ? "" : v;
    }

    /**
     * Tells the form support (and through it the host) that the state of this element changed.
     * <p>
     * 폼 지원(과 그것을 통해 호스트)에 이 요소의 상태가 바뀌었음을 알린다.
     */
    final void stateChanged() {
        document().getFormSupport().stateChanged(this);
    }

    /**
     * Returns the host of the document, or null.
     * <p>
     * 문서의 호스트를 반환한다. 없으면 null.
     *
     * @return the host or null / 호스트 또는 null
     */
    final FormHost host() {
        return document().getFormSupport().getHost();
    }

    /**
     * Fires an event of the user agent (trusted) at this element.
     * <p>
     * 이 요소에 사용자 에이전트의 이벤트(신뢰됨)를 발생시킨다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     * @return false if a listener cancelled it / 리스너가 취소했으면 false
     */
    public final boolean fireEvent(String type, boolean bubbles, boolean cancelable) {
        Event e = new Event(type, bubbles, cancelable);
        return dispatchTrustedEvent(e);
    }

    /**
     * Fires the <code>input</code> event that follows a change that the user made (an <code>InputEvent</code> that bubbles and is composed). The host
     * calls it after it changed the value through {@link HtmlInputElement#setValueFromUser(String)} or the same method of a textarea.
     * <p>
     * 사용자가 바꾼 뒤의 <code>input</code> 이벤트(버블링하고 composed 인 <code>InputEvent</code>)를 발생시킨다. 호스트가
     * {@link HtmlInputElement#setValueFromUser(String)} 이나 textarea 의 같은 메서드로 값을 바꾼 뒤 부른다.
     *
     * @param inputType the input type such as <code>insertText</code> or <code>deleteContentBackward</code>, may be null / <code>insertText</code>,
     *                  <code>deleteContentBackward</code> 같은 입력 종류. null 가능
     * @param data      the inserted text or null / 삽입한 글 또는 null
     */
    public final void fireInputEvent(String inputType, String data) {
        InputEvent ev = new InputEvent("input", true, false);
        ev.setComposed(true);
        // 입력기가 조합 중인 글을 바꾼 입력(insertCompositionText)은 isComposing 이다(UI Events, Input Events).
        ev.setInput(data, inputType == null ? "" : inputType, "insertCompositionText".equals(inputType));
        dispatchTrustedEvent(ev);
    }

    /**
     * Fires the <code>beforeinput</code> event before a change that the user is making (an <code>InputEvent</code> that bubbles and is composed). It
     * can be cancelled except for the text of an input method that is being composed (<code>insertCompositionText</code>, Input Events).
     * <p>
     * 사용자가 하려는 변경 앞의 <code>beforeinput</code> 이벤트(버블링하고 composed 인 <code>InputEvent</code>)를 발생시킨다. 입력기가 조합 중인 글
     * (<code>insertCompositionText</code>)을 빼면 취소할 수 있다(Input Events).
     *
     * @param inputType the input type such as <code>insertText</code> / <code>insertText</code> 같은 입력 종류
     * @param data      the text to insert or null / 삽입할 글 또는 null
     * @return false if a listener cancelled the change / 리스너가 변경을 취소했으면 false
     */
    public final boolean fireBeforeInputEvent(String inputType, String data) {
        boolean composition = "insertCompositionText".equals(inputType);
        InputEvent ev = new InputEvent("beforeinput", true, !composition);
        ev.setComposed(true);
        ev.setInput(data, inputType == null ? "" : inputType, composition);
        return dispatchTrustedEvent(ev);
    }

    /**
     * Fires the <code>change</code> event (it bubbles).
     * <p>
     * <code>change</code> 이벤트를 발생시킨다(버블링한다).
     */
    public final void fireChangeEvent() {
        fireEvent("change", true, false);
    }

    /**
     * Tells whether this element can have labels (<code>button</code>, <code>input</code> except hidden, <code>meter</code>, <code>output</code>,
     * <code>progress</code>, <code>select</code>, <code>textarea</code>). The default is false.
     * <p>
     * 이 요소가 label 을 가질 수 있는지(<code>button</code>, hidden 이 아닌 <code>input</code>, <code>meter</code>, <code>output</code>, <code>progress</code>,
     * <code>select</code>, <code>textarea</code>) 알려 준다. 기본은 false.
     *
     * @return true for labelable elements / 라벨을 붙일 수 있는 요소이면 true
     */
    public boolean isLabelable() {
        return false;
    }

    /**
     * Returns the live list of the <code>label</code> elements whose labeled control is this element.
     * <p>
     * 이 요소를 labeled control 로 갖는 <code>label</code> 요소의 라이브 목록을 반환한다.
     *
     * @return the same list every time, or null if the element is not labelable / 매번 같은 목록. 라벨을 붙일 수 없는 요소이면 null
     */
    public final FormElementList getLabels() {
        if (!isLabelable()) {
            return null;
        }
        if (labels == null) {
            labels = new LabelList(this);
        }
        return labels;
    }

    /**
     * Returns the default tab index of the element when it has no <code>tabindex</code> attribute: 0 for elements that the user can focus by
     * default, otherwise -1.
     * <p>
     * <code>tabindex</code> 속성이 없을 때의 요소의 기본 tab 순서를 반환한다: 사용자가 기본으로 포커스할 수 있는 요소는 0, 아니면 -1.
     *
     * @return the default / 기본값
     */
    int defaultTabIndex() {
        return -1;
    }

    /**
     * Returns the value of the <code>tabIndex</code> IDL attribute.
     * <p>
     * <code>tabIndex</code> IDL 속성의 값을 반환한다.
     *
     * @return the tab index / tab 순서
     */
    public final int getTabIndex() {
        Long v = HtmlText.parseInteger(getAttribute("tabindex"));
        return v == null || v.longValue() > Integer.MAX_VALUE || v.longValue() < Integer.MIN_VALUE ? defaultTabIndex() : v.intValue();
    }

    /**
     * Sets the <code>tabindex</code> attribute.
     * <p>
     * <code>tabindex</code> 속성을 지정한다.
     *
     * @param value the new tab index / 새 tab 순서
     */
    public final void setTabIndex(int value) {
        setAttribute("tabindex", Integer.toString(value));
    }

    /**
     * Tells whether a click on this element runs an activation behavior of this layer (submitting, resetting, toggling, picking files,
     * activating the labeled control). The default is false.
     * <p>
     * 이 요소의 클릭이 이 계층의 활성화 동작(제출, 초기화, 토글, 파일 선택, labeled control 활성화)을 실행하는지 알려 준다. 기본은 false.
     *
     * @return true if the element has an activation behavior / 활성화 동작이 있으면 true
     */
    boolean hasActivationBehavior() {
        return false;
    }

    /**
     * Runs the activation behavior after a click that nobody cancelled. The default does nothing.
     * <p>
     * 아무도 취소하지 않은 클릭 뒤에 활성화 동작을 실행한다. 기본은 아무것도 하지 않는다.
     *
     * @param click the click event / click 이벤트
     */
    void activate(Event click) {
        // 기본: 활성화 동작이 없다.
    }

    /**
     * Dispatches an event; a <code>click</code> mouse event runs the activation behavior of the target or its nearest ancestor that has
     * one (see {@link FormActivation#dispatchClick(Element, Event, boolean)}), other events are dispatched as usual.
     * <p>
     * 이벤트를 전달한다. <code>click</code> 마우스 이벤트는 대상 또는 활성화 동작이 있는 가장 가까운 조상의 활성화 동작을 실행하고
     * ({@link FormActivation#dispatchClick(Element, Event, boolean)} 참조), 다른 이벤트는 평소처럼 전달한다.
     *
     * @param event the event / 이벤트
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    public boolean dispatchEvent(Event event) {
        if (FormActivation.isActivationEvent(event) && !FormSupport.isPlain(this, event)) {
            return FormActivation.dispatchClick(this, event, false);
        }
        return super.dispatchEvent(event);
    }

    /**
     * Dispatches an event created by the user agent (trusted); see {@link #dispatchEvent(Event)}.
     * <p>
     * 사용자 에이전트가 만든 이벤트(신뢰됨)를 전달한다. {@link #dispatchEvent(Event)} 를 참조한다.
     *
     * @param event the event / 이벤트
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    public boolean dispatchTrustedEvent(Event event) {
        if (FormActivation.isActivationEvent(event) && !FormSupport.isPlain(this, event)) {
            return FormActivation.dispatchClick(this, event, true);
        }
        return super.dispatchTrustedEvent(event);
    }

    /**
     * Finds the first element with an id in the tree that contains a node, in tree order (the lookup that <code>for</code> and
     * <code>form</code> attributes use).
     * <p>
     * 노드가 속한 트리에서 트리 순서로 처음 만나는 id 가 같은 요소를 찾는다(<code>for</code>, <code>form</code> 속성이 쓰는 조회).
     *
     * @param from a node of the tree / 트리의 노드
     * @param id   the id / id
     * @return the element or null / 요소 또는 null
     */
    static Element findById(Node from, String id) {
        if (id.length() == 0) {
            return null;
        }
        Node root = from.getRootNode();
        // 트리의 모든 요소를 순서대로 보며 id 가 같은 첫 요소를 찾는다.
        for (Node n = Node.nextInTree(root, root); n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof Element && id.equals(((Element) n).getId())) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Tells whether an element is an HTML element with a local name.
     * <p>
     * 요소가 주어진 로컬 이름의 HTML 요소인지 알려 준다.
     *
     * @param n    the node / 노드
     * @param name the lower-case local name / 소문자 로컬 이름
     * @return true if it is / 그렇다면 true
     */
    static boolean isHtml(Node n, String name) {
        return n instanceof Element && org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML.equals(((Element) n).getNamespaceURI())
                && name.equals(((Element) n).getLocalName());
    }
}
