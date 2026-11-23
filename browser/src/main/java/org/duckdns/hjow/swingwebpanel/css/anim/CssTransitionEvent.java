package org.duckdns.hjow.swingwebpanel.css.anim;

/**
 * The event of a CSS transition (<code>transitionrun</code>, <code>transitionstart</code>, <code>transitionend</code>,
 * <code>transitioncancel</code>): the script interface <code>TransitionEvent</code> with <code>propertyName</code>,
 * <code>elapsedTime</code> and <code>pseudoElement</code>.
 * <p>
 * CSS 전환의 이벤트(<code>transitionrun</code>, <code>transitionstart</code>, <code>transitionend</code>, <code>transitioncancel</code>): 스크립트 인터페이스
 * <code>TransitionEvent</code> 가 <code>propertyName</code>, <code>elapsedTime</code>, <code>pseudoElement</code> 를 가진다.
 */
public final class CssTransitionEvent extends CssAnimationEvent {

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type          the event type / 이벤트 타입
     * @param propertyName  the transitioned property / 전환한 속성
     * @param elapsedTime   the seconds that the transition ran / 전환이 진행된 초
     */
    public CssTransitionEvent(String type, String propertyName, double elapsedTime) {
        super(type, propertyName, "", elapsedTime);
    }

    /**
     * Creates the event of a transition of a pseudo-element.
     * <p>
     * 의사 요소의 전환 이벤트를 만든다.
     *
     * @param type          the event type / 이벤트 타입
     * @param propertyName  the transitioned property / 전환한 속성
     * @param elapsedTime   the seconds that the transition ran / 전환이 진행된 초
     * @param pseudoElement the pseudo-element with its colons such as <code>::before</code>, or null for the element itself / <code>::before</code> 처럼 콜론이 붙은 의사 요소. 요소 자신이면 null
     */
    public CssTransitionEvent(String type, String propertyName, double elapsedTime, String pseudoElement) {
        super(type, propertyName, "", elapsedTime, pseudoElement);
    }
}
