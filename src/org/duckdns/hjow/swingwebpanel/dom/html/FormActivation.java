package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The activation behavior of <code>click</code> events (DOM Standard 2.9 "dispatch", HTML Standard 4.10 and 5.?): before a click is dispatched, the nearest
 * element on the event path that has an activation behavior (the target itself, or - because a click bubbles - one of its ancestors such
 * as a <code>button</code> that contains a <code>span</code>, or a <code>label</code>) gets its <em>legacy-pre-activation behavior</em> (a checkbox
 * toggles, a radio button becomes checked); after the dispatch the <em>legacy-canceled-activation behavior</em> restores that state if a listener
 * cancelled the event, otherwise the <em>activation behavior</em> runs (checkboxes and radio buttons fire <code>input</code>/<code>change</code>,
 * submit and reset buttons submit and reset the form, a label clicks its labeled control, a file upload control asks the host for files).
 * Only <code>MouseEvent</code> clicks are activation events. A click that is targeted at a disabled form control is not dispatched at all
 * (HTML Standard 4.10.19.? "disabled").
 * <p>
 * <code>click</code> 이벤트의 활성화 동작(DOM 표준 2.9 "dispatch", HTML 표준 4.10): click 을 전달하기 전에 이벤트 경로에서 활성화 동작이 있는 가장 가까운 요소(대상 자신,
 * 또는 click 이 버블링하므로 <code>span</code> 을 품은 <code>button</code> 이나 <code>label</code> 같은 조상)에 <em>legacy-pre-activation behavior</em>(체크박스 토글,
 * 라디오 버튼 체크)를 실행한다. 전달 뒤 리스너가 이벤트를 취소했으면 <em>legacy-canceled-activation behavior</em> 가 그 상태를 되돌리고, 아니면 <em>활성화 동작</em>을
 * 실행한다(체크박스와 라디오 버튼은 <code>input</code>/<code>change</code> 발생, submit 과 reset 버튼은 폼 제출과 초기화, label 은 labeled control 클릭, 파일 업로드 컨트롤은
 * 호스트에 파일 요청). <code>MouseEvent</code> 인 click 만 활성화 이벤트이다. 비활성 폼 컨트롤을 대상으로 한 click 은 전달되지 않는다.
 */
final class FormActivation {

    /** Not instantiable. / 인스턴스를 만들 수 없다. */
    private FormActivation() {
    }

    /**
     * Tells whether an event is an activation event: a <code>click</code> that is a {@link MouseEvent}.
     * <p>
     * 이벤트가 활성화 이벤트인지 알려 준다: {@link MouseEvent} 인 <code>click</code>.
     *
     * @param event the event / 이벤트
     * @return true for an activation event / 활성화 이벤트이면 true
     */
    static boolean isActivationEvent(Event event) {
        return event instanceof MouseEvent && "click".equals(event.getType());
    }

    /**
     * Finds the activation target of a click: the target if it has an activation behavior, otherwise (the click must bubble) the nearest
     * ancestor that has one.
     * <p>
     * click 의 활성화 대상을 찾는다: 대상이 활성화 동작을 가지면 대상, 아니면(click 이 버블링해야 한다) 활성화 동작을 가진 가장 가까운 조상.
     *
     * @param target the target / 대상
     * @param event  the click / click
     * @return the activation target or null / 활성화 대상 또는 null
     */
    private static HtmlControl activationTarget(Element target, Event event) {
        if (target instanceof HtmlControl && ((HtmlControl) target).hasActivationBehavior()) {
            return (HtmlControl) target;
        }
        if (!event.isBubbles()) {
            return null;
        }
        // 대상의 조상을 올라가며 활성화 동작이 있는 첫 요소를 찾는다.
        for (Node p = target.getParentNode(); p != null; p = p.getParentNode()) {
            if (p instanceof HtmlControl && ((HtmlControl) p).hasActivationBehavior()) {
                return (HtmlControl) p;
            }
        }
        return null;
    }

    /**
     * Dispatches a click event with its activation behavior (see the class description). The caller is {@link HtmlControl#dispatchEvent(Event)} or
     * the same method of {@link HtmlGenericElement}; the event is passed on to the plain dispatch of the target through
     * {@link FormSupport#dispatchPlain(Element, Event, boolean)}.
     * <p>
     * click 이벤트를 활성화 동작과 함께 전달한다(클래스 설명 참조). 호출자는 {@link HtmlControl#dispatchEvent(Event)} 이거나 {@link HtmlGenericElement} 의 같은 메서드이다.
     * 이벤트는 {@link FormSupport#dispatchPlain(Element, Event, boolean)} 를 통해 대상의 일반 전달로 넘어간다.
     *
     * @param target  the target element / 대상 요소
     * @param event   the click event / click 이벤트
     * @param trusted true for an event of the user agent / 사용자 에이전트의 이벤트이면 true
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    static boolean dispatchClick(Element target, Event event, boolean trusted) {
        // 비활성 폼 컨트롤을 대상으로 한 click 은 전달하지 않는다.
        if (target instanceof HtmlListedElement && ((HtmlListedElement) target).isDisabled()
                && !(target instanceof HtmlFieldSetElement)) {
            return true;
        }
        HtmlControl at = activationTarget(target, event);
        // 레이블 안의 상호작용 콘텐츠를 누른 click 은 레이블이 활성화하지 않는다(그 콘텐츠가 활성화 동작이 없어 조상으로 넘어온 경우).
        if (at instanceof HtmlLabelElement && insideInteractiveContent(target, at)) {
            at = null;
        }
        HtmlInputElement input = at instanceof HtmlInputElement ? (HtmlInputElement) at : null;
        if (input != null) {
            input.preActivate();
        }
        boolean ok = FormSupport.dispatchPlain(target, event, trusted);
        if (at == null) {
            // 활성화 동작이 있는 폼 컨트롤이 없으면 summary 의 활성화 동작(details 열고 닫기)을 확인한다.
            if (ok) {
                activateSummary(target);
            }
            return ok;
        }
        if (!ok) {
            if (input != null) {
                input.cancelActivation();
            }
        } else {
            at.activate(event);
        }
        return ok;
    }

    /**
     * The activation behavior of a <code>summary</code> element (HTML Standard 4.11.1 and 4.11.2): a click on the summary of a
     * <code>details</code> element, the first <code>summary</code> child of it, toggles the <code>open</code> attribute of the
     * <code>details</code> element (the <code>toggle</code> event follows from the change of the attribute). A click on interactive content
     * inside the summary, such as a link, does not toggle it.
     * <p>
     * <code>summary</code> 요소의 활성화 동작(HTML 표준 4.11.1, 4.11.2): <code>details</code> 요소의 첫 <code>summary</code> 자식인 summary 를 누르면
     * <code>details</code> 의 <code>open</code> 속성이 토글된다(<code>toggle</code> 이벤트는 속성 변경에서 이어진다). summary 안의 링크 같은 상호작용 콘텐츠를
     * 누른 경우는 토글하지 않는다.
     *
     * @param target the click target / click 대상
     */
    private static void activateSummary(Element target) {
        // 대상에서 올라가며 details 의 첫 summary 자식을 찾는다.
        for (Node n = target; n != null; n = n.getParentNode()) {
            if (!(n instanceof Element) || !org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML.equals(((Element) n).getNamespaceURI())
                    || !"summary".equals(((Element) n).getLocalName())) {
                continue;
            }
            Node parent = n.getParentNode();
            if (!(parent instanceof Element) || !"details".equals(((Element) parent).getLocalName())
                    || !org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML.equals(((Element) parent).getNamespaceURI())) {
                return;
            }
            // 첫 summary 자식이 아니거나 안에 상호작용 콘텐츠가 있으면 토글하지 않는다.
            if (firstSummary((Element) parent) != n || insideInteractiveContent(target, n)) {
                return;
            }
            Element details = (Element) parent;
            if (details.hasAttribute("open")) {
                details.removeAttribute("open");
            } else {
                details.setAttribute("open", "");
            }
            return;
        }
    }

    /**
     * Returns the first <code>summary</code> child element of a <code>details</code> element.
     * <p>
     * <code>details</code> 요소의 첫 <code>summary</code> 자식 요소를 반환한다.
     *
     * @param details the details element / details 요소
     * @return the summary or null / summary 또는 null
     */
    private static Node firstSummary(Element details) {
        // 자식 중 첫 HTML summary 요소를 찾는다.
        for (Node c = details.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element && org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML.equals(((Element) c).getNamespaceURI())
                    && "summary".equals(((Element) c).getLocalName())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Tells whether a click target lies inside interactive content that sits between it and the label (so that the label does not
     * activate its control when, for example, the user clicks a link inside the label).
     * <p>
     * click 대상이 자신과 label 사이에 있는 상호작용 콘텐츠 안에 있는지 알려 준다(예: label 안의 링크를 눌렀을 때 label 이 컨트롤을 활성화하지 않도록).
     *
     * @param target the click target / click 대상
     * @param label  the label / label
     * @return true if inside interactive content / 상호작용 콘텐츠 안이면 true
     */
    private static boolean insideInteractiveContent(Element target, Node label) {
        // 대상에서 label 까지 올라가며 상호작용 콘텐츠를 찾는다.
        for (Node n = target; n != null && n != label; n = n.getParentNode()) {
            if (n instanceof Element) {
                Element e = (Element) n;
                String name = e.getLocalName();
                if (org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML.equals(e.getNamespaceURI())) {
                    if (("a".equals(name) && e.hasAttribute("href")) || "button".equals(name) || "select".equals(name) || "textarea".equals(name)
                            || "iframe".equals(name) || "embed".equals(name) || "details".equals(name)
                            || ("input".equals(name) && !"hidden".equalsIgnoreCase(e.getAttribute("type")))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Implements <code>element.click()</code>: does nothing for a disabled form control, otherwise a synthetic <code>click</code>
     * (a bubbling, cancelable mouse event) is dispatched at the element with the activation behavior, as the user agent's own event.
     * <p>
     * <code>element.click()</code> 를 구현한다: 비활성 폼 컨트롤에는 아무 일도 하지 않고, 아니면 합성 <code>click</code>(버블링하고 취소 가능한 마우스 이벤트)을 요소에
     * 활성화 동작과 함께 사용자 에이전트의 이벤트로 전달한다.
     *
     * @param element the element / 요소
     * @return false if a listener cancelled the click / 리스너가 click 을 취소했으면 false
     */
    static boolean click(Element element) {
        return click(element, true);
    }

    /**
     * Implements <code>element.click()</code> with a choice of the trust of the event: a script's click is not trusted, so it cannot open a file chooser.
     * <p>
     * 이벤트의 신뢰 여부를 고를 수 있는 <code>element.click()</code>: 스크립트의 click 은 신뢰되지 않아 파일 선택 창을 열 수 없다.
     *
     * @param element the element / 요소
     * @param trusted true if the click originates from the user agent or the user / click 이 사용자 에이전트나 사용자에게서 왔으면 true
     * @return false if a listener cancelled the click / 리스너가 click 을 취소했으면 false
     */
    static boolean click(Element element, boolean trusted) {
        if (element instanceof HtmlListedElement && ((HtmlListedElement) element).isDisabled()) {
            return true;
        }
        MouseEvent ev = new MouseEvent("click", true, true);
        ev.setComposed(true);
        ev.setButtons(0, 0);
        return trusted ? element.dispatchTrustedEvent(ev) : element.dispatchEvent(ev);
    }
}
