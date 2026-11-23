package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * An element that has no specialized class: every element that the {@link HtmlElementFactory} does not know (other HTML elements, SVG,
 * MathML, custom names). It behaves like a plain DOM {@link Element}, except that a <code>click</code> that is dispatched at it also runs the
 * activation behavior of the nearest ancestor that has one (a <code>button</code> that contains a <code>span</code>, a <code>label</code>; see
 * {@link FormActivation}). The class exists because the constructor of <code>Element</code> is protected.
 * <p>
 * 특화된 클래스가 없는 요소: {@link HtmlElementFactory} 가 모르는 모든 요소(그 밖의 HTML 요소, SVG, MathML, 사용자 정의 이름). 일반 DOM
 * {@link Element} 처럼 동작하되, 이 요소에 전달된 <code>click</code> 은 활성화 동작이 있는 가장 가까운 조상(<code>span</code> 을 품은 <code>button</code>,
 * <code>label</code>; {@link FormActivation} 참조)의 활성화 동작도 실행한다. <code>Element</code> 의 생성자가 protected 이기 때문에 존재하는 클래스이다.
 */
public final class HtmlGenericElement extends Element {

    /**
     * Creates a plain element.
     * <p>
     * 일반 요소를 만든다.
     *
     * @param owner        owner document / 소유 문서
     * @param namespaceURI namespace or null / 네임스페이스 또는 null
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name / 로컬 이름
     */
    HtmlGenericElement(Document owner, String namespaceURI, String prefix, String localName) {
        super(owner, namespaceURI, prefix, localName);
    }

    /**
     * Dispatches an event; a <code>click</code> mouse event also runs the activation behavior of the nearest activatable ancestor.
     * <p>
     * 이벤트를 전달한다. <code>click</code> 마우스 이벤트는 활성화 가능한 가장 가까운 조상의 활성화 동작도 실행한다.
     *
     * @param event the event / 이벤트
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    public boolean dispatchEvent(Event event) {
        if (FormActivation.isActivationEvent(event) && !FormSupport.isPlain(this, event) && getParentNode() != null) {
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
        if (FormActivation.isActivationEvent(event) && !FormSupport.isPlain(this, event) && getParentNode() != null) {
            return FormActivation.dispatchClick(this, event, true);
        }
        return super.dispatchTrustedEvent(event);
    }
}
