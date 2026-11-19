package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.List;

/**
 * A script host for the Web Animations model in tests (no JavaScript): promises are records with a state, microtasks wait in a list until
 * {@link #runMicrotasks()}, frame requests are counted and the clock is a number that the test sets. {@link #frame(double)} runs one animation frame
 * as a page does: {@link WebAnimationSet#update(double)}, the microtask checkpoint, then the events, which it records as
 * <code>type:id:currentTime:timelineTime</code>.
 * <p>
 * 시험용 Web Animations 모델의 스크립트 호스트(JavaScript 없음): 프라미스는 상태를 가진 기록이고, 마이크로태스크는 {@link #runMicrotasks()} 까지 목록에서 기다리며,
 * 프레임 요청은 세고, 시계는 시험이 정하는 숫자이다. {@link #frame(double)} 은 페이지처럼 애니메이션 프레임 하나를 실행한다: {@link WebAnimationSet#update(double)},
 * 마이크로태스크 검사점, 그 다음 이벤트(<code>type:id:currentTime:timelineTime</code> 로 기록한다).
 */
public final class FakeAnimationHost implements WebAnimationHost, WebAnimationSet.TimeSource {

    /**
     * A recorded promise.
     * <p>
     * 기록된 프라미스.
     */
    public static final class Promise implements WebAnimationHost.PromiseSlot {
        /** The state: pending, resolved or rejected. 상태: pending, resolved, rejected. */
        public String state;

        /**
         * Creates a promise.
         * <p>
         * 프라미스를 만든다.
         *
         * @param resolved true if already resolved / 이미 resolve 되었으면 true
         */
        Promise(boolean resolved) {
            state = resolved ? "resolved" : "pending";
        }

        /**
         * Resolves it if pending.
         * <p>
         * 대기 중이면 resolve 한다.
         */
        public void resolve() {
            if ("pending".equals(state)) {
                state = "resolved";
            }
        }

        /**
         * Rejects it if pending.
         * <p>
         * 대기 중이면 reject 한다.
         */
        public void reject() {
            if ("pending".equals(state)) {
                state = "rejected";
            }
        }

        /**
         * Tells whether it is pending.
         * <p>
         * 대기 중인지 알려 준다.
         *
         * @return true if pending / 대기 중이면 true
         */
        public boolean isPending() {
            return "pending".equals(state);
        }
    }

    /** The clock in milliseconds since the time origin. 시간 기준점 이후 밀리초의 시계. */
    public double time = 1000;
    /** The number of frame requests. 프레임 요청 수. */
    public int frameRequests;
    /** The number of style change notifications. 스타일 변경 알림 수. */
    public int styleChanges;
    /** The queued microtasks. 큐에 든 마이크로태스크. */
    public final List<Runnable> microtasks = new ArrayList<Runnable>();
    /** The dispatched events. 전달한 이벤트. */
    public final List<String> events = new ArrayList<String>();

    /**
     * Creates the host.
     * <p>
     * 호스트를 만든다.
     */
    public FakeAnimationHost() {
    }

    /**
     * Creates a recorded promise.
     * <p>
     * 기록되는 프라미스를 만든다.
     *
     * @param animation the animation / 애니메이션
     * @param resolved  true if resolved / resolve 되었으면 true
     * @return the promise / 프라미스
     */
    public PromiseSlot newPromise(WebAnimation animation, boolean resolved) {
        return new Promise(resolved);
    }

    /**
     * Queues a microtask.
     * <p>
     * 마이크로태스크를 큐에 넣는다.
     *
     * @param task the task / 작업
     */
    public void queueMicrotask(Runnable task) {
        microtasks.add(task);
    }

    /**
     * Counts a frame request.
     * <p>
     * 프레임 요청을 센다.
     */
    public void requestFrame() {
        frameRequests++;
    }

    /**
     * Counts a style change.
     * <p>
     * 스타일 변경을 센다.
     */
    public void styleChanged() {
        styleChanges++;
    }

    /**
     * Returns the clock.
     * <p>
     * 시계를 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    public double now() {
        return time;
    }

    /**
     * Runs the queued microtasks, also those that they queue.
     * <p>
     * 큐에 든 마이크로태스크를 실행한다(그것이 넣는 것도).
     */
    public void runMicrotasks() {
        // 큐가 빌 때까지 앞에서부터 실행한다.
        while (!microtasks.isEmpty()) {
            microtasks.remove(0).run();
        }
    }

    /**
     * Runs one animation frame at a time: update, microtask checkpoint, events.
     * <p>
     * 한 시각에 애니메이션 프레임 하나를 실행한다: 갱신, 마이크로태스크 검사점, 이벤트.
     *
     * @param set the registry / 등록부
     * @param at  the time / 시각
     */
    public void frame(WebAnimationSet set, double at) {
        time = at;
        set.update(at);
        runMicrotasks();
        // 정렬된 이벤트를 기록한다.
        for (WebAnimationSet.PendingEvent e : set.takeEvents()) {
            AnimationPlaybackEvent ev = e.getEvent();
            events.add(ev.getType() + ":" + e.getTarget().getId() + ":" + text(ev.getCurrentTime()) + ":" + text(ev.getTimelineTime()));
            e.getTarget().dispatchEvent(ev);
        }
    }

    /**
     * Formats a nullable time.
     * <p>
     * null 일 수 있는 시간을 문자열로 만든다.
     *
     * @param d the time / 시간
     * @return the text / 문자열
     */
    private static String text(Double d) {
        return d == null ? "null" : String.valueOf(Math.round(d.doubleValue()));
    }

    /**
     * Returns the state of a promise slot.
     * <p>
     * 프라미스 자리의 상태를 반환한다.
     *
     * @param p the slot / 자리
     * @return the state / 상태
     */
    public static String state(PromiseSlot p) {
        return ((Promise) p).state;
    }
}
