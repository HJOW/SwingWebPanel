package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.css.anim.CssKeyframesEvent;
import org.duckdns.hjow.swingwebpanel.css.anim.CssTransitionEvent;

/**
 * The script interfaces of the events of CSS transitions and animations (CSS Transitions Level 1 section 6 and CSS Animations Level 1
 * section 4): <code>TransitionEvent</code> (<code>propertyName</code>, <code>elapsedTime</code>, <code>pseudoElement</code>) and
 * <code>AnimationEvent</code> (<code>animationName</code>, <code>elapsedTime</code>, <code>pseudoElement</code>). The events are made by the
 * animation engine and bubble; the scripts get them through <code>addEventListener</code> and the handler properties
 * (<code>ontransitionend</code>, <code>onanimationstart</code> and so on). The constructors of the interfaces are not callable (the page cannot
 * make such events).
 * <p>
 * CSS 전환과 애니메이션 이벤트의 스크립트 인터페이스(CSS Transitions Level 1 6절, CSS Animations Level 1 4절): <code>TransitionEvent</code>
 * (<code>propertyName</code>, <code>elapsedTime</code>, <code>pseudoElement</code>)와 <code>AnimationEvent</code>(<code>animationName</code>,
 * <code>elapsedTime</code>, <code>pseudoElement</code>). 이벤트는 애니메이션 엔진이 만들고 버블링하며, 스크립트는 <code>addEventListener</code> 와 핸들러
 * 프로퍼티(<code>ontransitionend</code>, <code>onanimationstart</code> 등)로 받는다. 인터페이스의 생성자는 호출할 수 없다(페이지가 그런 이벤트를 만들 수 없다).
 */
final class AnimationEventBindings {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private AnimationEventBindings() {
    }

    /**
     * Defines the two interfaces (call after the Event interface exists).
     * <p>
     * 두 인터페이스를 정의한다(Event 인터페이스가 생긴 뒤에 호출한다).
     *
     * @param bridge the bridge / 브리지
     */
    static void install(DomBridge bridge) {
        bridge.defineInterface("TransitionEvent", "Event", CssTransitionEvent.class, new String[] { "a:propertyName", "a:elapsedTime", "a:pseudoElement" });
        bridge.defineInterface("AnimationEvent", "Event", CssKeyframesEvent.class, new String[] { "a:animationName", "a:elapsedTime", "a:pseudoElement" });
    }
}
