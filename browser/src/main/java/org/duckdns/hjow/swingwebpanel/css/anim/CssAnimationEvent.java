package org.duckdns.hjow.swingwebpanel.css.anim;

import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * The event of a CSS transition or animation (plan item P10-07): <code>transitionrun</code>, <code>transitionstart</code>,
 * <code>transitionend</code>, <code>transitioncancel</code> (with {@link #getPropertyName()}) and <code>animationstart</code>,
 * <code>animationiteration</code>, <code>animationend</code>, <code>animationcancel</code> (with {@link #getAnimationName()}). The events
 * bubble and cannot be canceled; {@link #getElapsedTime()} is in seconds, {@link #getPseudoElement()} names the pseudo-element of a transition (empty for an element; the keyframe animations do not run on pseudo-elements, only the transitions
 * do).
 * <p>
 * CSS 전환이나 애니메이션의 이벤트(계획 P10-07): <code>transitionrun</code>, <code>transitionstart</code>, <code>transitionend</code>, <code>transitioncancel</code>
 * ({@link #getPropertyName()} 포함)과 <code>animationstart</code>, <code>animationiteration</code>, <code>animationend</code>, <code>animationcancel</code>
 * ({@link #getAnimationName()} 포함). 이벤트는 버블링되고 취소할 수 없다. {@link #getElapsedTime()} 은 초 단위이며 {@link #getPseudoElement()} 는 의사 요소의 전환이면 그 이름이고 요소이면 비어 있다(의사 요소에서는 키프레임 애니메이션은 실행하지 않고 전환만
 * 한다).
 */
public class CssAnimationEvent extends Event {

    /** The animated property of a transition event, else empty. 전환 이벤트의 애니메이션한 속성. 아니면 빈 문자열. */
    private final String propertyName;
    /** The name of the animation of an animation event, else empty. 애니메이션 이벤트의 애니메이션 이름. 아니면 빈 문자열. */
    private final String animationName;
    /** The time that the transition or animation has been running, in seconds. 전환이나 애니메이션이 진행한 시간(초). */
    private final double elapsedTime;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type          the event type / 이벤트 종류
     * @param propertyName  the property of a transition event or null / 전환 이벤트의 속성 또는 null
     * @param animationName the name of an animation event or null / 애니메이션 이벤트의 이름 또는 null
     * @param elapsedTime   the elapsed time in seconds / 경과 시간(초)
     */
    public CssAnimationEvent(String type, String propertyName, String animationName, double elapsedTime) {
        this(type, propertyName, animationName, elapsedTime, null);
    }

    /**
     * Creates the event of a pseudo-element.
     * <p>
     * 의사 요소의 이벤트를 만든다.
     *
     * @param type          the event type / 이벤트 종류
     * @param propertyName  the property of a transition event or null / 전환 이벤트의 속성 또는 null
     * @param animationName the name of an animation event or null / 애니메이션 이벤트의 이름 또는 null
     * @param elapsedTime   the elapsed time in seconds / 경과 시간(초)
     * @param pseudoElement the pseudo-element with colons (<code>::before</code>) or null / 콜론이 붙은 의사 요소(<code>::before</code>). 없으면 null
     */
    public CssAnimationEvent(String type, String propertyName, String animationName, double elapsedTime, String pseudoElement) {
        super(type, true, false);
        this.propertyName = propertyName == null ? "" : propertyName;
        this.animationName = animationName == null ? "" : animationName;
        this.elapsedTime = elapsedTime;
        this.pseudoElement = pseudoElement == null ? "" : (pseudoElement.startsWith("::") ? pseudoElement : "::" + pseudoElement);
    }

    /** The pseudo-element that the event is about (<code>::before</code>), empty for an element. 이벤트가 가리키는 의사 요소(<code>::before</code>). 요소이면 빈 문자열. */
    private final String pseudoElement;

    /**
     * Returns the property of a transition event.
     * <p>
     * 전환 이벤트의 속성을 반환한다.
     *
     * @return the property name, empty for animation events / 속성 이름. 애니메이션 이벤트는 빈 문자열
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the name of the animation of an animation event.
     * <p>
     * 애니메이션 이벤트의 애니메이션 이름을 반환한다.
     *
     * @return the name, empty for transition events / 이름. 전환 이벤트는 빈 문자열
     */
    public String getAnimationName() {
        return animationName;
    }

    /**
     * Returns the elapsed time.
     * <p>
     * 경과 시간을 반환한다.
     *
     * @return seconds / 초
     */
    public double getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Returns the pseudo-element of the event (<code>::before</code> for the transition of a pseudo-element), empty for an element.
     * <p>
     * 이벤트의 의사 요소를 반환한다(의사 요소의 전환이면 <code>::before</code> 같은 값). 요소이면 빈 문자열.
     *
     * @return the pseudo-element with colons or the empty string / 콜론이 붙은 의사 요소 또는 빈 문자열
     */
    public String getPseudoElement() {
        return pseudoElement;
    }
}
