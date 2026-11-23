package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.props.LogicalProps;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The Web Animations state of one document (Web Animations Level 1): the animations created by scripts in their global order (the composite order
 * among them), the time of the document that the {@link WebTimeline}s follow, the pending animation event queue and the procedure "update animations
 * and send events" ({@link #update(double)} for the timelines, the pending tasks, the finished states and the removal of replaced animations, then
 * {@link #takeEvents()} after the host's microtask checkpoint). The style side uses {@link #apply(Element, String, ComputedStyle)}: the
 * {@link AnimationEngine} calls it after the CSS transitions and animations, so script animations are at the top of the effect stack (5.4.2: CSS
 * transitions, then CSS animations, then the other animations). Only the animations that can still change something are kept ("tracked"); an
 * idle animation or one that finished without fill is let go and comes back when a script changes it, so finished animations do not accumulate. The
 * document time is the time of the host's clock while nothing is waiting for a frame and it stays at the time of the last frame while animations run,
 * as the specification wants (<code>document.timeline.currentTime</code> does not change between frames). Every method runs on the document thread.
 * <p>
 * 한 문서의 Web Animations 상태(Web Animations Level 1): 스크립트가 만든 애니메이션의 전역 순서(그 사이의 합성 순서), {@link WebTimeline} 이 따르는 문서 시간,
 * 대기 애니메이션 이벤트 큐, "update animations and send events" 절차(타임라인, 대기 작업, 끝남 상태, 교체된 애니메이션 제거는 {@link #update(double)}, 호스트의
 * 마이크로태스크 검사점 뒤의 {@link #takeEvents()}). 스타일 쪽은 {@link #apply(Element, String, ComputedStyle)} 를 쓴다: {@link AnimationEngine} 이 CSS 전환과
 * 애니메이션 뒤에 부르므로 스크립트 애니메이션이 효과 스택의 맨 위에 있다(5.4.2: CSS 전환, CSS 애니메이션, 그 밖의 애니메이션 순). 아직 무언가를 바꿀 수 있는
 * 애니메이션만 보관하며("추적") 유휴 상태이거나 채움 없이 끝난 애니메이션은 놓아 주고 스크립트가 바꾸면 다시 돌아오므로 끝난 애니메이션이 쌓이지 않는다. 문서 시간은
 * 프레임을 기다리는 것이 없을 때는 호스트 시계의 시간이고 애니메이션이 도는 동안은 마지막 프레임의 시간에 머문다(규격대로 <code>document.timeline.currentTime</code> 은
 * 프레임 사이에 바뀌지 않는다). 모든 메서드는 문서 스레드에서 실행된다.
 */
public final class WebAnimationSet {

    /**
     * The clock of the document: milliseconds since the time origin of the page.
     * <p>
     * 문서의 시계: 페이지의 시간 기준점 이후 밀리초.
     */
    public interface TimeSource {
        /**
         * Returns the time now.
         * <p>
         * 지금 시간을 반환한다.
         *
         * @return milliseconds since the time origin / 시간 기준점 이후 밀리초
         */
        double now();
    }

    /**
     * An event in the pending animation event queue with its target and its scheduled event time.
     * <p>
     * 대기 애니메이션 이벤트 큐에 든 이벤트와 그 대상, 예정 시각.
     */
    public static final class PendingEvent {
        /** The target animation. 대상 애니메이션. */
        private final WebAnimation target;
        /** The event. 이벤트. */
        private final AnimationPlaybackEvent event;
        /** The scheduled event time, NaN if unresolved. 예정 시각. 결정되지 않았으면 NaN. */
        private final double scheduled;
        /** The position in the queue (keeps the sort stable). 큐 안의 위치(정렬을 안정되게 한다). */
        private final long index;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param target    the animation / 애니메이션
         * @param event     the event / 이벤트
         * @param scheduled the scheduled time / 예정 시각
         * @param index     the position / 위치
         */
        PendingEvent(WebAnimation target, AnimationPlaybackEvent event, double scheduled, long index) {
            this.target = target;
            this.event = event;
            this.scheduled = scheduled;
            this.index = index;
        }

        /**
         * Returns the target.
         * <p>
         * 대상을 반환한다.
         *
         * @return the animation / 애니메이션
         */
        public WebAnimation getTarget() {
            return target;
        }

        /**
         * Returns the event.
         * <p>
         * 이벤트를 반환한다.
         *
         * @return the event / 이벤트
         */
        public AnimationPlaybackEvent getEvent() {
            return event;
        }
    }

    /**
     * A promise for a set without a host (only for code that uses the model without scripts): it records its state.
     * <p>
     * 호스트가 없는 등록부의 프라미스(스크립트 없이 모델을 쓰는 코드용): 상태만 기록한다.
     */
    private static final class LocalPromise implements WebAnimationHost.PromiseSlot {
        /** 0 pending, 1 resolved, 2 rejected. 0 대기, 1 resolve, 2 reject. */
        private int state;

        /**
         * Creates the promise.
         * <p>
         * 프라미스를 만든다.
         *
         * @param resolved true if already resolved / 이미 resolve 되었으면 true
         */
        LocalPromise(boolean resolved) {
            state = resolved ? 1 : 0;
        }

        /**
         * Resolves it if pending.
         * <p>
         * 대기 중이면 resolve 한다.
         */
        public void resolve() {
            if (state == 0) {
                state = 1;
            }
        }

        /**
         * Rejects it if pending.
         * <p>
         * 대기 중이면 reject 한다.
         */
        public void reject() {
            if (state == 0) {
                state = 2;
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
            return state == 0;
        }
    }

    /** The style resolver of the document, null for a set without styles. 문서의 스타일 리졸버. 스타일이 없는 등록부이면 null. */
    private final StyleResolver resolver;
    /** The engine of the CSS animations of the document, or null. 문서의 CSS 애니메이션 엔진. 없으면 null. */
    private final AnimationEngine engine;
    /** The script host, or null (written by the thread that stops the scripts). 스크립트 호스트. 없으면 null(스크립트를 멈추는 스레드가 쓴다). */
    private volatile WebAnimationHost host;
    /** The clock of the document, or null. 문서의 시계. 없으면 null. */
    private volatile TimeSource clock;
    /**
     * Set when the scripts of the page stopped (any thread): from then on the script animations have no effect, and the next {@link #flushStopped()} on the
     * document thread releases them and restyles their targets.
     * <p>
     * 페이지의 스크립트가 멈추면(어느 스레드든) 정해진다: 그때부터 스크립트 애니메이션은 효과가 없고, 문서 스레드의 다음 {@link #flushStopped()} 가 그것을 놓고 대상의 스타일을
     * 다시 계산하게 한다.
     */
    private volatile boolean stopped;
    /** The default document timeline. 기본 문서 타임라인. */
    private final WebTimeline timeline;
    /** The tracked animations in composite order. 합성 순서의 추적 중인 애니메이션. */
    private final List<WebAnimation> tracked = new ArrayList<WebAnimation>();
    /** The targets whose style the script animations changed since the last frame. 마지막 프레임 이후 스크립트 애니메이션이 스타일을 바꾼 대상. */
    private final Set<Element> animatedTargets = Collections.newSetFromMap(new IdentityHashMap<Element, Boolean>());
    /** The pending animation event queue. 대기 애니메이션 이벤트 큐. */
    private final List<PendingEvent> events = new ArrayList<PendingEvent>();
    /** The time of the document, NaN if unknown. 문서 시간. 모르면 NaN. */
    private double documentTime = Double.NaN;
    /** True while a frame was asked for and has not run. 프레임을 요청했고 아직 실행되지 않은 동안 true. */
    private boolean frameRequested;
    /** True while {@link #update(double)} runs. {@link #update(double)} 가 실행되는 동안 true. */
    private boolean updating;
    /** The next position in the global animation list. 전역 애니메이션 목록의 다음 위치. */
    private long nextSequence;
    /** The next position in the event queue. 이벤트 큐의 다음 위치. */
    private long nextEvent;
    /** Set by {@link #dispose()}. {@link #dispose()} 가 정한다. */
    private boolean disposed;

    /**
     * Creates the state of a document.
     * <p>
     * 문서의 상태를 만든다.
     *
     * @param resolver the style resolver whose styles the animations change, or null for a set that only keeps time / 애니메이션이 스타일을 바꿀 스타일 리졸버. 시간만 다루는 등록부이면 null
     * @param engine   the engine of the CSS animations, or null / CSS 애니메이션 엔진. 없으면 null
     */
    public WebAnimationSet(StyleResolver resolver, AnimationEngine engine) {
        this.resolver = resolver;
        this.engine = engine;
        this.timeline = new WebTimeline(this, 0);
    }

    /**
     * Connects the script host and the clock of the document (the page's script host does it when it installs <code>element.animate()</code>).
     * <p>
     * 스크립트 호스트와 문서의 시계를 연결한다(페이지의 스크립트 호스트가 <code>element.animate()</code> 를 설치할 때 한다).
     *
     * @param h the host / 호스트
     * @param c the clock / 시계
     */
    public void attach(WebAnimationHost h, TimeSource c) {
        if (disposed || stopped) {
            return;
        }
        host = h;
        clock = c;
        documentTime = Double.NaN;
    }

    /**
     * Tells whether a host is connected.
     * <p>
     * 호스트가 연결되어 있는지 알려 준다.
     *
     * @return true if connected / 연결되어 있으면 true
     */
    public boolean hasHost() {
        return host != null && !disposed && !stopped;
    }

    /**
     * Returns the default document timeline (<code>document.timeline</code>).
     * <p>
     * 기본 문서 타임라인을 반환한다(<code>document.timeline</code>).
     *
     * @return the timeline / 타임라인
     */
    public WebTimeline getTimeline() {
        return timeline;
    }

    /**
     * Returns the next position in the global animation list.
     * <p>
     * 전역 애니메이션 목록의 다음 위치를 반환한다.
     *
     * @return the position / 위치
     */
    long nextSequence() {
        return nextSequence++;
    }

    /**
     * Returns the time of the document: the time of the last frame while a frame is waited for or running, the clock otherwise.
     * <p>
     * 문서 시간을 반환한다: 프레임을 기다리거나 실행하는 동안은 마지막 프레임의 시간, 그 밖에는 시계의 시간.
     *
     * @return milliseconds since the time origin, NaN without a clock / 시간 기준점 이후 밀리초. 시계가 없으면 NaN
     */
    double documentTime() {
        TimeSource c = clock;
        if (disposed || stopped || c == null) {
            return Double.NaN;
        }
        // 프레임을 기다리는 것이 없으면 시계를 읽어 문서 시간을 지금으로 옮긴다(단조 증가는 유지한다).
        if (!updating && !frameRequested) {
            sample(c);
        }
        return documentTime;
    }

    /**
     * Moves the document time to the clock (never backwards).
     * <p>
     * 문서 시간을 시계로 옮긴다(뒤로 가지는 않는다).
     *
     * @param c the clock / 시계
     */
    private void sample(TimeSource c) {
        double now = c.now();
        if (Double.isNaN(documentTime) || now > documentTime) {
            documentTime = now;
        }
    }

    /**
     * Creates a promise of an animation through the host.
     * <p>
     * 호스트를 통해 애니메이션의 프라미스를 만든다.
     *
     * @param a        the animation / 애니메이션
     * @param resolved true for an already resolved promise / 이미 resolve 된 프라미스이면 true
     * @return the promise / 프라미스
     */
    WebAnimationHost.PromiseSlot newPromise(WebAnimation a, boolean resolved) {
        WebAnimationHost h = host;
        return h == null ? new LocalPromise(resolved) : h.newPromise(a, resolved);
    }

    /**
     * Queues a microtask through the host (dropped without a host).
     * <p>
     * 호스트를 통해 마이크로태스크를 큐에 넣는다(호스트가 없으면 버린다).
     *
     * @param task the task / 작업
     */
    void queueMicrotask(Runnable task) {
        WebAnimationHost h = host;
        if (h != null && !disposed && !stopped) {
            h.queueMicrotask(task);
        }
    }

    /**
     * Appends an event to the pending animation event queue and asks for a frame.
     * <p>
     * 대기 애니메이션 이벤트 큐에 이벤트를 더하고 프레임을 요청한다.
     *
     * @param target    the animation / 애니메이션
     * @param event     the event / 이벤트
     * @param scheduled the scheduled event time, NaN if unresolved / 예정 시각. 결정되지 않았으면 NaN
     */
    void queueEvent(WebAnimation target, AnimationPlaybackEvent event, double scheduled) {
        if (disposed || stopped) {
            return;
        }
        events.add(new PendingEvent(target, event, scheduled, nextEvent++));
        requestFrame();
    }

    /**
     * Asks the host for a frame unless one is waiting.
     * <p>
     * 기다리는 프레임이 없으면 호스트에 프레임을 요청한다.
     */
    private void requestFrame() {
        WebAnimationHost h = host;
        TimeSource c = clock;
        if (frameRequested || h == null || disposed || stopped) {
            return;
        }
        // 프레임을 기다리는 동안 문서 시간은 멈추므로 요청하기 직전의 시각으로 옮겨 둔다.
        if (!updating && c != null) {
            sample(c);
        }
        frameRequested = true;
        h.requestFrame();
    }

    /**
     * Records that an animation changed (its state, timing, keyframes or target): it is tracked, its target is restyled, a frame is asked for if it
     * needs one, and the host is told that the style changed.
     * <p>
     * 애니메이션이 바뀌었음(상태, 시간, 키프레임, 대상)을 기록한다: 추적 목록에 넣고 대상의 스타일을 다시 계산하게 하며, 프레임이 필요하면 요청하고 호스트에 스타일이
     * 바뀌었음을 알린다.
     *
     * @param a the animation / 애니메이션
     */
    void changed(WebAnimation a) {
        if (disposed || stopped) {
            return;
        }
        track(a);
        KeyframeEffectModel e = a.getEffect();
        if (e != null) {
            invalidate(e.getTarget());
        }
        if (needsFrame()) {
            requestFrame();
        }
        WebAnimationHost h = host;
        if (h != null && !updating) {
            h.styleChanged();
        }
    }

    /**
     * Drops the cached style of an element (and remembers it as animated for the next frame).
     * <p>
     * 요소의 캐시한 스타일을 버린다(다음 프레임을 위해 애니메이션된 대상으로 기억한다).
     *
     * @param e the element or null / 요소 또는 null
     */
    void invalidate(Element e) {
        if (e == null || resolver == null || disposed || stopped) {
            return;
        }
        animatedTargets.add(e);
        resolver.invalidateElement(e);
        if (engine != null) {
            engine.scriptStyleChanged();
        }
    }

    /**
     * Adds an animation to the tracked list in composite order unless it is there.
     * <p>
     * 애니메이션이 없으면 합성 순서에 맞게 추적 목록에 더한다.
     *
     * @param a the animation / 애니메이션
     */
    private void track(WebAnimation a) {
        int i = tracked.size();
        // 뒤에서부터 순서를 비교해 끼울 자리를 찾는다(이미 있으면 그대로 둔다).
        while (i > 0 && tracked.get(i - 1).sequence >= a.sequence) {
            if (tracked.get(i - 1) == a) {
                return;
            }
            i--;
        }
        tracked.add(i, a);
    }

    /**
     * Tells whether something waits for a frame: an event to dispatch, a pending task, or a running animation whose time moves.
     * <p>
     * 프레임을 기다리는 것이 있는지 알려 준다: 전달할 이벤트, 대기 작업, 시간이 움직이는 실행 중 애니메이션.
     *
     * @return true if a frame is needed / 프레임이 필요하면 true
     */
    private boolean needsFrame() {
        if (!events.isEmpty()) {
            return true;
        }
        // 추적 중인 애니메이션마다 다음 프레임에 바뀔 것이 있는지 본다.
        for (WebAnimation a : tracked) {
            WebTimeline t = a.getTimeline();
            if (t == null || !t.isActive()) {
                continue;
            }
            if (a.isPending() || ("running".equals(a.getPlayState()) && a.getEffectivePlaybackRate() != 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs the first part of "update animations and send events" (4.3) at a time of the document: the timelines take the time, the pending play and
     * pause tasks of the ready animations run, the finished states are updated and the replaced animations are removed; the targets are restyled.
     * The host then performs a microtask checkpoint and dispatches {@link #takeEvents()}.
     * <p>
     * 문서 시각에서 "update animations and send events"(4.3)의 앞부분을 실행한다: 타임라인이 시간을 받고, 준비된 애니메이션의 대기 play/pause 작업이 실행되며,
     * 끝남 상태를 갱신하고 교체된 애니메이션을 제거한다. 대상의 스타일을 다시 계산하게 한다. 그 뒤 호스트가 마이크로태스크 검사점을 수행하고 {@link #takeEvents()} 를
     * 전달한다.
     *
     * @param now the time of the document in milliseconds / 문서 시각(밀리초)
     */
    public void update(double now) {
        if (disposed || stopped) {
            return;
        }
        updating = true;
        frameRequested = false;
        try {
            if (Double.isNaN(documentTime) || now > documentTime) {
                documentTime = now;
            }
            List<WebAnimation> list = new ArrayList<WebAnimation>(tracked);
            // 애니메이션마다 대기 작업을 실행하고 끝남 상태를 갱신한다.
            for (WebAnimation a : list) {
                a.runPendingTasks();
                a.updateFinishedState(false, false);
            }
            removeReplaced();
            // 효과가 있던 대상과 지금 효과가 있는 대상의 스타일을 다시 계산하게 한다.
            Set<Element> targets = new LinkedHashSet<Element>(animatedTargets);
            animatedTargets.clear();
            // 추적 중인 애니메이션의 대상을 더한다.
            for (WebAnimation a : tracked) {
                KeyframeEffectModel e = a.getEffect();
                if (e != null && e.getTarget() != null) {
                    targets.add(e.getTarget());
                }
            }
            // 모은 대상마다 캐시한 스타일을 버린다.
            for (Element e : targets) {
                invalidate(e);
            }
            prune();
        } finally {
            updating = false;
        }
        if (needsFrame()) {
            requestFrame();
        }
    }

    /**
     * Lets go of the animations that cannot change anything any more: not pending, not running and not relevant.
     * <p>
     * 더 이상 아무것도 바꿀 수 없는 애니메이션을 놓아 준다: 대기 중이 아니고 실행 중이 아니며 관련 없는 것.
     */
    private void prune() {
        // 뒤에서부터 지워도 되는 애니메이션을 지운다.
        for (int i = tracked.size() - 1; i >= 0; i--) {
            WebAnimation a = tracked.get(i);
            if (!a.isPending() && !"running".equals(a.getPlayState()) && !a.isRelevant()) {
                tracked.remove(i);
            }
        }
    }

    /**
     * Removes the replaced animations (5.5.2): a finished, filling script animation all of whose target properties are also animated by a later
     * replaceable animation of the same target is removed, unless it was persisted.
     * <p>
     * 교체된 애니메이션을 제거한다(5.5.2): 끝나서 채우는 스크립트 애니메이션의 모든 대상 속성을 같은 대상의 더 나중의 교체 가능한 애니메이션도 애니메이션하면, 유지되지
     * 않은 한 제거한다.
     */
    private void removeReplaced() {
        List<WebAnimation> replaceable = new ArrayList<WebAnimation>();
        // 교체 가능한 애니메이션을 합성 순서로 모은다.
        for (WebAnimation a : tracked) {
            if (isReplaceable(a)) {
                replaceable.add(a);
            }
        }
        // 활성 교체 상태의 애니메이션마다 모든 대상 속성이 더 나중의 것에 덮이는지 본다.
        for (int i = 0; i < replaceable.size(); i++) {
            WebAnimation a = replaceable.get(i);
            if (!"active".equals(a.getReplaceState())) {
                continue;
            }
            KeyframeEffectModel ea = a.getEffect();
            boolean covered = true;
            Set<Object> props = new LinkedHashSet<Object>(ea.getTargetProperties());
            props.addAll(ea.getTargetCustomProperties());
            for (Object p : props) {
                boolean found = false;
                // 더 나중의 교체 가능한 애니메이션 중 같은 대상과 같은 속성을 가진 것을 찾는다.
                for (int j = i + 1; j < replaceable.size() && !found; j++) {
                    KeyframeEffectModel eb = replaceable.get(j).getEffect();
                    if (eb.getTarget() == ea.getTarget() && same(eb.getPseudoElement(), ea.getPseudoElement())
                            && (eb.getTargetProperties().contains(p) || eb.getTargetCustomProperties().contains(p))) {
                        found = true;
                    }
                }
                if (!found) {
                    covered = false;
                    break;
                }
            }
            if (covered) {
                a.markRemoved();
                invalidate(ea.getTarget());
            }
        }
    }

    /**
     * Tells whether an animation is replaceable (5.5.2).
     * <p>
     * 애니메이션이 교체 가능한지 알려 준다(5.5.2).
     *
     * @param a the animation / 애니메이션
     * @return true if replaceable / 교체 가능하면 true
     */
    private static boolean isReplaceable(WebAnimation a) {
        KeyframeEffectModel e = a.getEffect();
        if (e == null || e.getTarget() == null || "removed".equals(a.getReplaceState()) || !"finished".equals(a.getPlayState())) {
            return false;
        }
        EffectTiming.Computed c = a.computeTiming();
        return c != null && c.inEffect();
    }

    /**
     * Compares two pseudo-selectors that may be null.
     * <p>
     * null 일 수 있는 두 의사 선택자를 비교한다.
     *
     * @param a the first / 첫째
     * @param b the second / 둘째
     * @return true if equal / 같으면 true
     */
    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Takes the pending animation events sorted by scheduled event time (the unresolved times first) and then by composite order; the sort is stable.
     * <p>
     * 대기 애니메이션 이벤트를 예정 시각(결정되지 않은 시각이 먼저), 그 다음 합성 순서로 정렬해 가져간다. 정렬은 안정적이다.
     *
     * @return the events to dispatch / 전달할 이벤트
     */
    public List<PendingEvent> takeEvents() {
        List<PendingEvent> out = new ArrayList<PendingEvent>(events);
        events.clear();
        Collections.sort(out, new Comparator<PendingEvent>() {
            /**
             * Orders two events.
             * <p>
             * 두 이벤트의 순서를 정한다.
             *
             * @param x the first / 첫째
             * @param y the second / 둘째
             * @return the order / 순서
             */
            public int compare(PendingEvent x, PendingEvent y) {
                boolean xu = Double.isNaN(x.scheduled);
                boolean yu = Double.isNaN(y.scheduled);
                if (xu != yu) {
                    return xu ? -1 : 1;
                }
                if (!xu && x.scheduled != y.scheduled) {
                    return x.scheduled < y.scheduled ? -1 : 1;
                }
                if (x.target.sequence != y.target.sequence) {
                    return x.target.sequence < y.target.sequence ? -1 : 1;
                }
                return x.index < y.index ? -1 : (x.index == y.index ? 0 : 1);
            }
        });
        return out;
    }

    // ------------------------------------------------------------------------------------------------ queries

    /**
     * Returns the relevant animations whose target is in a subtree or is one element (<code>getAnimations()</code>, 6.8 and 6.10), in composite order.
     * <p>
     * 대상이 서브트리 안이거나 한 요소인 관련 애니메이션을 합성 순서로 반환한다(<code>getAnimations()</code>, 6.8 과 6.10).
     *
     * @param root    the element or the document / 요소 또는 문서
     * @param subtree true to include the descendants (always for a document) / 자손을 포함하려면 true(문서는 늘)
     * @return the animations / 애니메이션
     */
    public List<WebAnimation> getAnimations(Node root, boolean subtree) {
        List<WebAnimation> out = new ArrayList<WebAnimation>();
        if (disposed || stopped) {
            return out;
        }
        // 추적 중인 애니메이션 중 관련 있고 대상이 맞는 것을 고른다.
        for (WebAnimation a : tracked) {
            KeyframeEffectModel e = a.getEffect();
            if (e == null || e.getTarget() == null || !a.isRelevant()) {
                continue;
            }
            Element t = e.getTarget();
            boolean match;
            if (root instanceof Document) {
                match = t.getOwnerDocument() == root && t.isConnected();
            } else if (subtree) {
                match = isInclusiveAncestor(root, t);
            } else {
                match = t == root;
            }
            if (match) {
                out.add(a);
            }
        }
        return out;
    }

    /**
     * Tells whether a node is an inclusive ancestor of another.
     * <p>
     * 노드가 다른 노드의 포함 조상인지 알려 준다.
     *
     * @param ancestor the ancestor / 조상
     * @param n        the node / 노드
     * @return true if it is / 그러면 true
     */
    private static boolean isInclusiveAncestor(Node ancestor, Node n) {
        // 부모를 따라 올라가며 조상을 찾는다.
        for (Node p = n; p != null; p = p.getParentNode()) {
            if (p == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a tracked animation targets an element (or one of its pseudo-elements).
     * <p>
     * 추적 중인 애니메이션이 요소(또는 그 의사 요소)를 대상으로 하는지 알려 준다.
     *
     * @param e the element / 요소
     * @return true if one does / 그런 것이 있으면 true
     */
    public boolean targets(Element e) {
        if (disposed || stopped) {
            return false;
        }
        // 추적 중인 애니메이션의 대상을 비교한다.
        for (WebAnimation a : tracked) {
            KeyframeEffectModel fx = a.getEffect();
            if (fx != null && fx.getTarget() == e) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------ effects

    /**
     * Applies the effects of the script animations of an element or pseudo-element on its style, in composite order (5.4.3): each effect that is in
     * effect combines its values with the result so far.
     * <p>
     * 요소나 의사 요소의 스크립트 애니메이션 효과를 합성 순서대로 스타일에 적용한다(5.4.3): 적용 중인 효과마다 지금까지의 결과와 값을 결합한다.
     *
     * @param e      the element / 요소
     * @param pseudo the pseudo-element with colons (<code>::before</code>), null for the element / 콜론이 붙은 의사 요소. 요소이면 null
     * @param s      the style below the script animations / 스크립트 애니메이션 아래의 스타일
     * @return the animated style / 애니메이션이 적용된 스타일
     */
    public ComputedStyle apply(Element e, String pseudo, ComputedStyle s) {
        return applyUpTo(e, pseudo, s, null);
    }

    /**
     * Applies the effects up to and including one animation (also when it was removed); null applies all.
     * <p>
     * 한 애니메이션까지(제거되었더라도 포함) 효과를 적용한다. null 이면 모두 적용한다.
     *
     * @param e      the element / 요소
     * @param pseudo the pseudo-element or null / 의사 요소 또는 null
     * @param s      the style below the script animations / 스크립트 애니메이션 아래의 스타일
     * @param last   the last animation to apply, or null / 마지막으로 적용할 애니메이션. 없으면 null
     * @return the animated style / 애니메이션이 적용된 스타일
     */
    private ComputedStyle applyUpTo(Element e, String pseudo, ComputedStyle s, WebAnimation last) {
        if (resolver == null || disposed || stopped || tracked.isEmpty() || s == null) {
            return s;
        }
        ComputedStyle r = s;
        List<WebAnimation> list = new ArrayList<WebAnimation>(tracked);
        if (last != null && !list.contains(last)) {
            list.add(last);
        }
        // 합성 순서대로 대상이 맞고 적용 중인 효과를 적용한다.
        for (WebAnimation a : list) {
            if (last != null && a.sequence > last.sequence) {
                continue;
            }
            KeyframeEffectModel fx = a.getEffect();
            if (fx == null || fx.getTarget() != e || !same(fx.getPseudoElement(), pseudo) || ("removed".equals(a.getReplaceState()) && a != last)) {
                continue;
            }
            EffectTiming.Computed c = a.computeTiming();
            if (!c.inEffect()) {
                continue;
            }
            r = fx.apply(resolver, e, r, c.progress);
        }
        return r;
    }

    /**
     * Collects the values of the custom properties that the script animations of an element give (for a restyle with them).
     * <p>
     * 요소의 스크립트 애니메이션이 주는 사용자 정의 속성 값을 모은다(그것으로 스타일을 다시 계산하도록).
     *
     * @param e    the element / 요소
     * @param s    the style below the script animations / 스크립트 애니메이션 아래의 스타일
     * @param out  receives the values by name / 이름별 값을 받는다
     */
    public void collectCustom(Element e, ComputedStyle s, Map<String, String> out) {
        if (resolver == null || disposed || stopped || tracked.isEmpty()) {
            return;
        }
        // 합성 순서대로 대상이 요소 자신인 효과의 사용자 정의 속성 값을 모은다(뒤의 것이 이긴다).
        for (WebAnimation a : new ArrayList<WebAnimation>(tracked)) {
            KeyframeEffectModel fx = a.getEffect();
            if (fx == null || fx.getTarget() != e || fx.getPseudoElement() != null || "removed".equals(a.getReplaceState())) {
                continue;
            }
            EffectTiming.Computed c = a.computeTiming();
            if (c.inEffect()) {
                fx.collectCustom(resolver, e, s, c.progress, out);
            }
        }
    }

    /**
     * Writes the values that the effect stack up to an animation gives to its target into the target's <code>style</code> attribute
     * (<code>commitStyles()</code>, 6.4 "commit computed styles").
     * <p>
     * 한 애니메이션까지의 효과 스택이 대상에 주는 값을 대상의 <code>style</code> 속성에 쓴다(<code>commitStyles()</code>, 6.4 "commit computed styles").
     *
     * @param a the animation / 애니메이션
     * @throws AnimationException a NoModificationAllowedError for a pseudo-element, an InvalidStateError if the target is not rendered / 의사 요소이면 NoModificationAllowedError, 대상이 렌더링되지 않으면 InvalidStateError
     */
    public void commitStyles(WebAnimation a) {
        KeyframeEffectModel fx = a.getEffect();
        if (fx == null || fx.getTarget() == null) {
            return;
        }
        Element t = fx.getTarget();
        if (fx.getPseudoElement() != null) {
            throw new AnimationException(AnimationException.NO_MODIFICATION, "Animation target is a pseudo-element");
        }
        if (resolver == null || engine == null || disposed || stopped) {
            return;
        }
        ComputedStyle under;
        try {
            if (!isRendered(t)) {
                throw new AnimationException(AnimationException.INVALID_STATE, "Target element is not rendered.");
            }
            resolver.getComputedStyle(t);
            under = engine.belowScripts(t);
        } catch (IllegalArgumentException e) {
            // 다른 문서의 요소는 이 문서의 스타일로 계산할 수 없다.
            return;
        }
        if (under == null) {
            return;
        }
        ComputedStyle result = applyUpTo(t, null, under, a);
        String wm = result.keyword(Prop.WRITING_MODE) == null ? "horizontal-tb" : result.keyword(Prop.WRITING_MODE);
        String dr = result.keyword(Prop.DIRECTION) == null ? "ltr" : result.keyword(Prop.DIRECTION);
        String text = t.getAttribute("style");
        CssStyleDeclaration inline = CssStyleDeclaration.parse(text == null ? "" : text, t.getOwnerDocument().getBaseUri(), new CssDiagnostics());
        PropertyRegistry reg = PropertyRegistry.getInstance();
        // 효과의 대상 속성마다(물리 개별 속성으로) 결과 값을 인라인 스타일에 쓴다.
        for (Integer id : fx.getTargetProperties()) {
            int phys = LogicalProps.physical(id.intValue(), wm, dr);
            Property p = reg.byId(phys);
            CssValue v = result.get(phys);
            if (p != null && v != null) {
                inline.setProperty(p.getName(), v.toCssText(), "");
            }
        }
        t.setAttribute("style", inline.getCssText());
    }

    /**
     * Tells whether an element is being rendered: it is connected and neither it nor an ancestor has <code>display: none</code>.
     * <p>
     * 요소가 렌더링되고 있는지 알려 준다: 연결되어 있고 그것과 조상 모두 <code>display: none</code> 이 아니다.
     *
     * @param t the element / 요소
     * @return true if rendered / 렌더링되면 true
     */
    private boolean isRendered(Element t) {
        if (!t.isConnected()) {
            return false;
        }
        // 요소와 조상의 display 를 확인한다.
        for (Element p = t; p != null; p = p.getParentElement()) {
            if ("none".equals(resolver.getComputedStyle(p).display())) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------ end

    /**
     * Stops every script animation without events because the scripts of the page stopped (it may be called on any thread): from now on the animations
     * have no effect and the timelines are inactive; {@link #flushStopped()} releases them on the document thread.
     * <p>
     * 페이지의 스크립트가 멈췄으므로 이벤트 없이 모든 스크립트 애니메이션을 멈춘다(어느 스레드에서나 부를 수 있다): 이제부터 애니메이션은 효과가 없고 타임라인은 비활성이다.
     * {@link #flushStopped()} 가 문서 스레드에서 그것을 놓는다.
     */
    public void detach() {
        stopped = true;
        host = null;
        clock = null;
    }

    /**
     * Releases the script animations after the scripts stopped and restyles their targets; call it on the document thread before a rendering.
     * <p>
     * 스크립트가 멈춘 뒤 스크립트 애니메이션을 놓고 대상의 스타일을 다시 계산하게 한다. 렌더링 전에 문서 스레드에서 부른다.
     *
     * @return true if styles changed / 스타일이 바뀌었으면 true
     */
    public boolean flushStopped() {
        if (!stopped || disposed || (tracked.isEmpty() && animatedTargets.isEmpty())) {
            return false;
        }
        Set<Element> targets = new LinkedHashSet<Element>(animatedTargets);
        // 애니메이션마다 상태를 지우고 대상을 모은다.
        for (WebAnimation a : tracked) {
            a.abandon();
            KeyframeEffectModel e = a.getEffect();
            if (e != null && e.getTarget() != null) {
                targets.add(e.getTarget());
            }
        }
        tracked.clear();
        events.clear();
        animatedTargets.clear();
        // 효과가 있던 대상의 캐시한 스타일을 버린다.
        for (Element e : targets) {
            if (resolver != null) {
                resolver.invalidateElement(e);
            }
        }
        return true;
    }

    /**
     * Ends everything without events and releases the references (the page is replaced or disposed; any thread).
     * <p>
     * 이벤트 없이 모든 것을 끝내고 참조를 놓는다(페이지가 교체되거나 폐기된다. 어느 스레드든).
     */
    public void dispose() {
        detach();
        disposed = true;
        tracked.clear();
        events.clear();
        animatedTargets.clear();
    }

    /**
     * Tells whether the set was disposed.
     * <p>
     * 등록부가 폐기되었는지 알려 준다.
     *
     * @return true if disposed / 폐기되었으면 true
     */
    public boolean isDisposed() {
        return disposed;
    }
}
