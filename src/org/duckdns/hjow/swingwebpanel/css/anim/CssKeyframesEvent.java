package org.duckdns.hjow.swingwebpanel.css.anim;

/**
 * The event of a CSS animation (<code>animationstart</code>, <code>animationiteration</code>, <code>animationend</code>,
 * <code>animationcancel</code>): the script interface <code>AnimationEvent</code> with <code>animationName</code>, <code>elapsedTime</code>
 * and <code>pseudoElement</code>.
 * <p>
 * CSS 애니메이션의 이벤트(<code>animationstart</code>, <code>animationiteration</code>, <code>animationend</code>, <code>animationcancel</code>): 스크립트
 * 인터페이스 <code>AnimationEvent</code> 가 <code>animationName</code>, <code>elapsedTime</code>, <code>pseudoElement</code> 를 가진다.
 */
public final class CssKeyframesEvent extends CssAnimationEvent {

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type          the event type / 이벤트 타입
     * @param animationName the name of the <code>\@keyframes</code> / <code>\@keyframes</code> 이름
     * @param elapsedTime   the seconds that the animation ran / 애니메이션이 진행된 초
     */
    public CssKeyframesEvent(String type, String animationName, double elapsedTime) {
        super(type, "", animationName, elapsedTime);
    }
}
