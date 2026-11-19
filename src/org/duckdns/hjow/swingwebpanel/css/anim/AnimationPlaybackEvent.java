package org.duckdns.hjow.swingwebpanel.css.anim;

import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * An animation playback event of Web Animations Level 1 (section 6.12, <code>AnimationPlaybackEvent</code>): <code>finish</code>,
 * <code>cancel</code> and <code>remove</code>, fired at a {@link WebAnimation}. The event does not bubble and cannot be canceled. Its
 * {@link #getCurrentTime()} is the current time of the animation when the event was queued and {@link #getTimelineTime()} the time of its
 * timeline; both are null when unresolved. Scripts create one with <code>new AnimationPlaybackEvent(type, init)</code>.
 * <p>
 * Web Animations Level 1 의 애니메이션 재생 이벤트(6.12 절, <code>AnimationPlaybackEvent</code>): {@link WebAnimation} 에 발생하는 <code>finish</code>,
 * <code>cancel</code>, <code>remove</code>. 버블링되지 않고 취소할 수 없다. {@link #getCurrentTime()} 은 이벤트를 큐에 넣을 때의 애니메이션 현재 시간이고
 * {@link #getTimelineTime()} 은 타임라인의 시간이며 결정되지 않았으면 둘 다 null 이다. 스크립트는 <code>new AnimationPlaybackEvent(type, init)</code> 으로 만든다.
 */
public final class AnimationPlaybackEvent extends Event {

    /** The current time of the animation, null if unresolved. 애니메이션의 현재 시간. 결정되지 않았으면 null. */
    private final Double currentTime;
    /** The time of the timeline, null if unresolved. 타임라인의 시간. 결정되지 않았으면 null. */
    private final Double timelineTime;

    /**
     * Creates an event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type         the type / 종류
     * @param currentTime  the current time of the animation in milliseconds, NaN for null / 애니메이션 현재 시간(밀리초). null 이면 NaN
     * @param timelineTime the timeline time in milliseconds, NaN for null / 타임라인 시간(밀리초). null 이면 NaN
     */
    public AnimationPlaybackEvent(String type, double currentTime, double timelineTime) {
        this(type, false, false, currentTime, timelineTime);
    }

    /**
     * Creates an event with the flags of an <code>EventInit</code> dictionary (the script constructor).
     * <p>
     * <code>EventInit</code> 사전의 플래그로 이벤트를 만든다(스크립트 생성자).
     *
     * @param type         the type / 종류
     * @param bubbles      whether it bubbles / 버블링 여부
     * @param cancelable   whether it can be canceled / 취소 가능 여부
     * @param currentTime  the current time, NaN for null / 현재 시간. null 이면 NaN
     * @param timelineTime the timeline time, NaN for null / 타임라인 시간. null 이면 NaN
     */
    public AnimationPlaybackEvent(String type, boolean bubbles, boolean cancelable, double currentTime, double timelineTime) {
        super(type, bubbles, cancelable);
        this.currentTime = Double.isNaN(currentTime) ? null : Double.valueOf(currentTime);
        this.timelineTime = Double.isNaN(timelineTime) ? null : Double.valueOf(timelineTime);
    }

    /**
     * Returns the current time of the animation when the event was queued (<code>event.currentTime</code>).
     * <p>
     * 이벤트를 큐에 넣을 때의 애니메이션 현재 시간을 반환한다(<code>event.currentTime</code>).
     *
     * @return milliseconds or null / 밀리초 또는 null
     */
    public Double getCurrentTime() {
        return currentTime;
    }

    /**
     * Returns the time of the timeline when the event was queued (<code>event.timelineTime</code>).
     * <p>
     * 이벤트를 큐에 넣을 때의 타임라인 시간을 반환한다(<code>event.timelineTime</code>).
     *
     * @return milliseconds or null / 밀리초 또는 null
     */
    public Double getTimelineTime() {
        return timelineTime;
    }
}
