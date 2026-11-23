package org.duckdns.hjow.swingwebpanel.css.anim;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;

/**
 * An animation of Web Animations Level 1 (sections 4.4 and 6.4, <code>Animation</code>): it binds a {@link KeyframeEffectModel} to a
 * {@link WebTimeline} and controls its playback with a start time, a hold time and a playback rate. The procedures of the specification are
 * implemented as they are written: setting the timeline, the effect, the current time and the start time; playing (with auto-rewind), pausing,
 * finishing, canceling and reversing; the playback rate and its seamless update through a pending playback rate; the pending play and pause
 * tasks, which run at the next animation frame of the document ({@link WebAnimationSet#update(double)}), so that the <code>ready</code> promise
 * resolves asynchronously with the time of that frame as the ready time; "update an animation's finished state" with the finish notification
 * steps in a microtask (or at once for <code>finish()</code>); the replace state. Events (<code>finish</code>, <code>cancel</code>, <code>remove</code>)
 * go to the pending animation event queue of the document and are dispatched at the next frame. An unresolved time is <code>NaN</code>. The methods
 * that the specification lets throw throw {@link AnimationException}. All methods run on the document thread.
 * <p>
 * Web Animations Level 1 의 애니메이션(4.4 절과 6.4 절, <code>Animation</code>): {@link KeyframeEffectModel} 을 {@link WebTimeline} 에 묶고 시작 시간, 유지 시간,
 * 재생 속도로 재생을 제어한다. 규격의 절차를 쓰인 그대로 구현한다: 타임라인, 효과, 현재 시간, 시작 시간 설정, 재생(자동 되감기 포함), 일시정지, 끝내기, 취소,
 * 뒤집기, 재생 속도와 대기 재생 속도를 통한 매끄러운 갱신, 문서의 다음 애니메이션 프레임({@link WebAnimationSet#update(double)})에서 실행되는 대기 중인 play/pause
 * 작업(그래서 <code>ready</code> 프라미스는 그 프레임의 시간을 준비 시간으로 하여 비동기로 resolve 된다), 마이크로태스크에서(<code>finish()</code> 는 즉시) 실행되는
 * 끝 알림 단계를 가진 "update an animation's finished state", 교체 상태. 이벤트(<code>finish</code>, <code>cancel</code>, <code>remove</code>)는 문서의 대기 애니메이션
 * 이벤트 큐로 가서 다음 프레임에 전달된다. 결정되지 않은 시간은 <code>NaN</code> 이다. 규격이 던지게 한 메서드는 {@link AnimationException} 을 던진다. 모든 메서드는 문서
 * 스레드에서 실행된다.
 */
public class WebAnimation extends EventTarget {

    /** The registry of the document in which the animation was created. 애니메이션이 만들어진 문서의 등록부. */
    final WebAnimationSet set;
    /** The position in the global animation list (creation order). 전역 애니메이션 목록에서의 위치(만든 순서). */
    final long sequence;
    /** The identifier (<code>animation.id</code>). 식별자(<code>animation.id</code>). */
    private String id = "";
    /** The timeline, or null. 타임라인. 없으면 null. */
    private WebTimeline timeline;
    /** The associated effect, or null. 연결된 효과. 없으면 null. */
    private KeyframeEffectModel effect;
    /** The start time, NaN if unresolved. 시작 시간. 결정되지 않았으면 NaN. */
    private double startTime = Double.NaN;
    /** The hold time, NaN if unresolved. 유지 시간. 결정되지 않았으면 NaN. */
    private double holdTime = Double.NaN;
    /** The playback rate. 재생 속도. */
    private double playbackRate = 1;
    /** The pending playback rate, NaN if unset. 대기 재생 속도. 정해지지 않았으면 NaN. */
    private double pendingPlaybackRate = Double.NaN;
    /** True while a pending play task waits for the next frame. 대기 play 작업이 다음 프레임을 기다리는 동안 true. */
    private boolean pendingPlay;
    /** True while a pending pause task waits for the next frame. 대기 pause 작업이 다음 프레임을 기다리는 동안 true. */
    private boolean pendingPause;
    /** The previous current time, NaN if unresolved. 이전 현재 시간. 결정되지 않았으면 NaN. */
    private double previousCurrentTime = Double.NaN;
    /** The replace state: active, removed or persisted. 교체 상태: active, removed, persisted. */
    private String replaceState = "active";
    /** The current ready promise. 현재 ready 프라미스. */
    private WebAnimationHost.PromiseSlot ready;
    /** The current finished promise. 현재 finished 프라미스. */
    private WebAnimationHost.PromiseSlot finished;
    /** The queued finish notification microtask, or null. 큐에 넣은 끝 알림 마이크로태스크. 없으면 null. */
    private Runnable finishNotification;

    /**
     * Creates an idle animation of a document (the <code>Animation(effect, timeline)</code> constructor runs {@link #setTimeline} and
     * {@link #setEffect} afterwards).
     * <p>
     * 문서의 유휴 애니메이션을 만든다(<code>Animation(effect, timeline)</code> 생성자는 그 뒤 {@link #setTimeline} 과 {@link #setEffect} 를 실행한다).
     *
     * @param set the registry of the document / 문서의 등록부
     */
    public WebAnimation(WebAnimationSet set) {
        if (set == null) {
            throw new IllegalArgumentException("set must not be null");
        }
        this.set = set;
        this.sequence = set.nextSequence();
        ready = set.newPromise(this, true);
        finished = set.newPromise(this, false);
    }

    // ------------------------------------------------------------------------------------------------ attributes

    /**
     * Returns the registry of the document of the animation.
     * <p>
     * 애니메이션의 문서 등록부를 반환한다.
     *
     * @return the registry / 등록부
     */
    public WebAnimationSet getSet() {
        return set;
    }

    /**
     * Returns the position in the global animation list (the composite order among script animations).
     * <p>
     * 전역 애니메이션 목록에서의 위치(스크립트 애니메이션 사이의 합성 순서)를 반환한다.
     *
     * @return the position / 위치
     */
    public long getSequence() {
        return sequence;
    }

    /**
     * Returns the identifier.
     * <p>
     * 식별자를 반환한다.
     *
     * @return the id / id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier.
     * <p>
     * 식별자를 정한다.
     *
     * @param id the id / id
     */
    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    /**
     * Returns the timeline.
     * <p>
     * 타임라인을 반환한다.
     *
     * @return the timeline or null / 타임라인 또는 null
     */
    public WebTimeline getTimeline() {
        return timeline;
    }

    /**
     * Returns the associated effect.
     * <p>
     * 연결된 효과를 반환한다.
     *
     * @return the effect or null / 효과 또는 null
     */
    public KeyframeEffectModel getEffect() {
        return effect;
    }

    /**
     * Returns the playback rate.
     * <p>
     * 재생 속도를 반환한다.
     *
     * @return the rate / 속도
     */
    public double getPlaybackRate() {
        return playbackRate;
    }

    /**
     * Returns the effective playback rate: the pending playback rate if set, else the playback rate (4.4.15.2).
     * <p>
     * 유효 재생 속도를 반환한다: 대기 재생 속도가 있으면 그것, 아니면 재생 속도(4.4.15.2).
     *
     * @return the rate / 속도
     */
    public double getEffectivePlaybackRate() {
        return Double.isNaN(pendingPlaybackRate) ? playbackRate : pendingPlaybackRate;
    }

    /**
     * Returns the start time.
     * <p>
     * 시작 시간을 반환한다.
     *
     * @return milliseconds, NaN if unresolved / 밀리초. 결정되지 않았으면 NaN
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Returns the replace state.
     * <p>
     * 교체 상태를 반환한다.
     *
     * @return <code>active</code>, <code>removed</code> or <code>persisted</code> / 상태
     */
    public String getReplaceState() {
        return replaceState;
    }

    /**
     * Tells whether a pending play or pause task waits (<code>animation.pending</code>).
     * <p>
     * 대기 play 또는 pause 작업이 기다리는지 알려 준다(<code>animation.pending</code>).
     *
     * @return true if pending / 대기 중이면 true
     */
    public boolean isPending() {
        return pendingPlay || pendingPause;
    }

    /**
     * Tells whether a pending play task waits.
     * <p>
     * 대기 play 작업이 기다리는지 알려 준다.
     *
     * @return true if one waits / 기다리면 true
     */
    public boolean hasPendingPlay() {
        return pendingPlay;
    }

    /**
     * Tells whether a pending pause task waits.
     * <p>
     * 대기 pause 작업이 기다리는지 알려 준다.
     *
     * @return true if one waits / 기다리면 true
     */
    public boolean hasPendingPause() {
        return pendingPause;
    }

    /**
     * Returns the current ready promise.
     * <p>
     * 현재 ready 프라미스를 반환한다.
     *
     * @return the promise / 프라미스
     */
    public WebAnimationHost.PromiseSlot getReady() {
        return ready;
    }

    /**
     * Returns the current finished promise.
     * <p>
     * 현재 finished 프라미스를 반환한다.
     *
     * @return the promise / 프라미스
     */
    public WebAnimationHost.PromiseSlot getFinished() {
        return finished;
    }

    /**
     * Returns the current time (4.4.3): the hold time if resolved; unresolved without an active timeline or without a start time; otherwise
     * <code>(timeline time - start time) * playback rate</code>.
     * <p>
     * 현재 시간을 반환한다(4.4.3): 유지 시간이 있으면 그것, 활성 타임라인이나 시작 시간이 없으면 결정되지 않음, 그 밖에는
     * <code>(타임라인 시간 - 시작 시간) * 재생 속도</code>.
     *
     * @return milliseconds, NaN if unresolved / 밀리초. 결정되지 않았으면 NaN
     */
    public double getCurrentTime() {
        if (!Double.isNaN(holdTime)) {
            return holdTime;
        }
        return unheldCurrentTime();
    }

    /**
     * Returns the current time calculated without the hold time.
     * <p>
     * 유지 시간 없이 계산한 현재 시간을 반환한다.
     *
     * @return milliseconds or NaN / 밀리초 또는 NaN
     */
    private double unheldCurrentTime() {
        double tt = timelineTime();
        if (Double.isNaN(tt) || Double.isNaN(startTime)) {
            return Double.NaN;
        }
        return (tt - startTime) * playbackRate;
    }

    /**
     * Returns the current time of the timeline.
     * <p>
     * 타임라인의 현재 시간을 반환한다.
     *
     * @return milliseconds, NaN without an active timeline / 밀리초. 활성 타임라인이 없으면 NaN
     */
    private double timelineTime() {
        return timeline == null ? Double.NaN : timeline.getCurrentTime();
    }

    /**
     * Returns the associated effect end: the end time of the effect, zero without an effect (4.4.12).
     * <p>
     * 연결된 효과의 끝을 반환한다: 효과의 끝 시간. 효과가 없으면 0(4.4.12).
     *
     * @return milliseconds / 밀리초
     */
    public double getEffectEnd() {
        return effect == null ? 0 : effect.getTiming().getEndTime();
    }

    /**
     * Returns the play state (4.4.17).
     * <p>
     * 재생 상태를 반환한다(4.4.17).
     *
     * @return <code>idle</code>, <code>running</code>, <code>paused</code> or <code>finished</code> / 상태
     */
    public String getPlayState() {
        double ct = getCurrentTime();
        if (Double.isNaN(ct) && Double.isNaN(startTime) && !isPending()) {
            return "idle";
        }
        if (pendingPause || (Double.isNaN(startTime) && !pendingPlay)) {
            return "paused";
        }
        double rate = getEffectivePlaybackRate();
        if (!Double.isNaN(ct) && ((rate > 0 && ct >= getEffectEnd()) || (rate < 0 && ct <= 0))) {
            return "finished";
        }
        return "running";
    }

    /**
     * Returns the computed timing of the effect at the current time.
     * <p>
     * 현재 시간에서 효과의 계산된 시간을 반환한다.
     *
     * @return the result, or null without an effect / 결과. 효과가 없으면 null
     */
    public EffectTiming.Computed computeTiming() {
        if (effect == null) {
            return null;
        }
        return effect.getTiming().compute(getCurrentTime(), playbackRate < 0, true);
    }

    /**
     * Tells whether the animation is relevant (4.5.6): its effect is current or in effect and it was not removed.
     * <p>
     * 애니메이션이 관련 있는지 알려 준다(4.5.6): 효과가 current 이거나 in effect 이고 제거되지 않았다.
     *
     * @return true if relevant / 관련 있으면 true
     */
    public boolean isRelevant() {
        if (effect == null || "removed".equals(replaceState)) {
            return false;
        }
        EffectTiming.Computed c = computeTiming();
        if (c.inEffect()) {
            return true;
        }
        // current: 실행 중인 active 단계, 또는 재생 방향 앞쪽의 단계.
        boolean inPlay = c.phase == EffectTiming.PHASE_ACTIVE && !"finished".equals(getPlayState());
        return inPlay || (playbackRate > 0 && c.phase == EffectTiming.PHASE_BEFORE) || (playbackRate < 0 && c.phase == EffectTiming.PHASE_AFTER);
    }

    // ------------------------------------------------------------------------------------------------ timeline and effect

    /**
     * Sets the timeline (4.4.1).
     * <p>
     * 타임라인을 정한다(4.4.1).
     *
     * @param newTimeline the timeline or null / 타임라인 또는 null
     */
    public void setTimeline(WebTimeline newTimeline) {
        if (newTimeline == timeline) {
            return;
        }
        timeline = newTimeline;
        if (!Double.isNaN(startTime)) {
            holdTime = Double.NaN;
        }
        updateFinishedState(false, false);
        set.changed(this);
    }

    /**
     * Sets the associated effect (4.4.2): an effect that belongs to another animation is taken from it first.
     * <p>
     * 연결된 효과를 정한다(4.4.2): 다른 애니메이션에 속한 효과는 먼저 그것에서 떼어 온다.
     *
     * @param newEffect the effect or null / 효과 또는 null
     */
    public void setEffect(KeyframeEffectModel newEffect) {
        if (newEffect == effect) {
            return;
        }
        if (newEffect != null && newEffect.animation != null) {
            newEffect.animation.setEffect(null);
        }
        KeyframeEffectModel old = effect;
        if (old != null) {
            old.animation = null;
            set.invalidate(old.getTarget());
        }
        effect = newEffect;
        if (newEffect != null) {
            newEffect.animation = this;
        }
        updateFinishedState(false, false);
        set.changed(this);
    }

    // ------------------------------------------------------------------------------------------------ seeking

    /**
     * Silently sets the current time (4.4.4).
     * <p>
     * 현재 시간을 조용히 정한다(4.4.4).
     *
     * @param seek the time, NaN for null / 시간. null 이면 NaN
     * @throws AnimationException a TypeError if the time is null while the current time is resolved / 현재 시간이 결정된 상태에서 null 이면 TypeError
     */
    private void silentlySetCurrentTime(double seek) {
        if (Double.isNaN(seek)) {
            if (!Double.isNaN(getCurrentTime())) {
                throw new AnimationException(AnimationException.TYPE_ERROR, "currentTime may not be changed from resolved to unresolved");
            }
            return;
        }
        boolean timelineActive = timeline != null && timeline.isActive();
        if (!Double.isNaN(holdTime) || Double.isNaN(startTime) || !timelineActive || playbackRate == 0) {
            holdTime = seek;
        } else {
            startTime = timelineTime() - seek / playbackRate;
        }
        if (!timelineActive) {
            startTime = Double.NaN;
        }
        previousCurrentTime = Double.NaN;
    }

    /**
     * Sets the current time (<code>animation.currentTime = t</code>, 4.4.4).
     * <p>
     * 현재 시간을 정한다(<code>animation.currentTime = t</code>, 4.4.4).
     *
     * @param seek the time, NaN for null / 시간. null 이면 NaN
     * @throws AnimationException a TypeError for null while the current time is resolved / 현재 시간이 결정된 상태에서 null 이면 TypeError
     */
    public void setCurrentTime(double seek) {
        silentlySetCurrentTime(seek);
        // 대기 pause 작업이 있으면 일시정지를 지금 끝낸다.
        if (pendingPause) {
            holdTime = seek;
            applyPendingPlaybackRate();
            startTime = Double.NaN;
            pendingPause = false;
            ready.resolve();
        }
        updateFinishedState(true, false);
        set.changed(this);
    }

    /**
     * Sets the start time (<code>animation.startTime = t</code>, 4.4.5).
     * <p>
     * 시작 시간을 정한다(<code>animation.startTime = t</code>, 4.4.5).
     *
     * @param newStart the time, NaN for null / 시간. null 이면 NaN
     */
    public void setStartTime(double newStart) {
        double tt = timeline != null && timeline.isActive() ? timelineTime() : Double.NaN;
        if (Double.isNaN(tt) && !Double.isNaN(newStart)) {
            holdTime = Double.NaN;
        }
        double previous = getCurrentTime();
        applyPendingPlaybackRate();
        startTime = newStart;
        if (!Double.isNaN(newStart)) {
            if (playbackRate != 0) {
                holdTime = Double.NaN;
            }
        } else {
            holdTime = previous;
        }
        // 대기 작업이 있으면 취소하고 ready 를 resolve 한다.
        if (pendingPlay || pendingPause) {
            pendingPlay = false;
            pendingPause = false;
            ready.resolve();
        }
        updateFinishedState(true, false);
        set.changed(this);
    }

    // ------------------------------------------------------------------------------------------------ playback control

    /**
     * Plays the animation (<code>play()</code>: the procedure "play an animation" with auto-rewind, 4.4.8).
     * <p>
     * 애니메이션을 재생한다(<code>play()</code>: 자동 되감기가 있는 "play an animation" 절차, 4.4.8).
     *
     * @throws AnimationException an InvalidStateError when a reversed endless animation would have to seek to its end / 역방향의 끝없는 애니메이션이 끝으로 이동해야 하면 InvalidStateError
     */
    public void play() {
        play(true);
    }

    /**
     * Runs "play an animation" (4.4.8).
     * <p>
     * "play an animation" 을 실행한다(4.4.8).
     *
     * @param autoRewind the auto-rewind flag / 자동 되감기 플래그
     * @throws AnimationException an InvalidStateError for a reversed endless animation / 역방향의 끝없는 애니메이션이면 InvalidStateError
     */
    void play(boolean autoRewind) {
        boolean abortedPause = pendingPause;
        boolean hasPendingReady = false;
        double seek = Double.NaN;
        double ct = getCurrentTime();
        double end = getEffectEnd();
        double rate = getEffectivePlaybackRate();
        // 자동 되감기: 재생 방향의 끝에 있거나 범위 밖이면 처음(역방향이면 끝)으로 이동한다.
        if (autoRewind) {
            if (rate >= 0 && (Double.isNaN(ct) || ct < 0 || ct >= end)) {
                seek = 0;
            } else if (rate < 0 && (Double.isNaN(ct) || ct <= 0 || ct > end)) {
                if (Double.isInfinite(end)) {
                    throw new AnimationException(AnimationException.INVALID_STATE, "Cannot play reversed Animation with infinite target effect end.");
                }
                seek = end;
            }
        }
        if (Double.isNaN(seek) && Double.isNaN(startTime) && Double.isNaN(ct)) {
            seek = 0;
        }
        if (!Double.isNaN(seek)) {
            holdTime = seek;
        }
        if (!Double.isNaN(holdTime)) {
            startTime = Double.NaN;
        }
        // 대기 중인 작업이 있으면 취소하고 같은 ready 프라미스를 다시 쓴다.
        if (pendingPlay || pendingPause) {
            pendingPlay = false;
            pendingPause = false;
            hasPendingReady = true;
        }
        if (Double.isNaN(holdTime) && Double.isNaN(seek) && !abortedPause && Double.isNaN(pendingPlaybackRate)) {
            return;
        }
        if (!hasPendingReady) {
            ready = set.newPromise(this, false);
        }
        pendingPlay = true;
        updateFinishedState(false, false);
        set.changed(this);
    }

    /**
     * Pauses the animation (<code>pause()</code>, 4.4.9).
     * <p>
     * 애니메이션을 일시정지한다(<code>pause()</code>, 4.4.9).
     *
     * @throws AnimationException an InvalidStateError for an idle reversed endless animation / 유휴 상태의 역방향 끝없는 애니메이션이면 InvalidStateError
     */
    public void pause() {
        if (pendingPause || "paused".equals(getPlayState())) {
            return;
        }
        double seek = Double.NaN;
        // 현재 시간이 없으면 재생 방향의 시작으로 이동한다.
        if (Double.isNaN(getCurrentTime())) {
            if (playbackRate >= 0) {
                seek = 0;
            } else {
                if (Double.isInfinite(getEffectEnd())) {
                    throw new AnimationException(AnimationException.INVALID_STATE, "Cannot pause reversed Animation with infinite target effect end.");
                }
                seek = getEffectEnd();
            }
        }
        if (!Double.isNaN(seek)) {
            holdTime = seek;
        }
        boolean hasPendingReady = false;
        if (pendingPlay) {
            pendingPlay = false;
            hasPendingReady = true;
        }
        if (!hasPendingReady) {
            ready = set.newPromise(this, false);
        }
        pendingPause = true;
        updateFinishedState(false, false);
        set.changed(this);
    }

    /**
     * Runs the pending play or pause task at a frame of the document whose timeline is active (4.4.8, 4.4.9): the ready time is the current
     * time of the timeline.
     * <p>
     * 타임라인이 활성인 문서의 프레임에서 대기 play 또는 pause 작업을 실행한다(4.4.8, 4.4.9): 준비 시간은 타임라인의 현재 시간이다.
     */
    void runPendingTasks() {
        if (timeline == null || !timeline.isActive()) {
            return;
        }
        double readyTime = timelineTime();
        if (pendingPlay) {
            pendingPlay = false;
            // 유지 시간이 있으면 그 시간에서 이어지도록 시작 시간을 정한다.
            if (!Double.isNaN(holdTime)) {
                applyPendingPlaybackRate();
                double newStart = playbackRate == 0 ? readyTime : readyTime - holdTime / playbackRate;
                startTime = newStart;
                if (playbackRate != 0) {
                    holdTime = Double.NaN;
                }
            } else if (!Double.isNaN(startTime) && !Double.isNaN(pendingPlaybackRate)) {
                double match = (readyTime - startTime) * playbackRate;
                applyPendingPlaybackRate();
                if (playbackRate == 0) {
                    holdTime = match;
                }
                startTime = playbackRate == 0 ? readyTime : readyTime - match / playbackRate;
            }
            ready.resolve();
            updateFinishedState(false, false);
            set.changed(this);
        } else if (pendingPause) {
            pendingPause = false;
            // 유지 시간이 아직 없으면 준비 시각의 현재 시간을 유지 시간으로 둔다.
            if (!Double.isNaN(startTime) && Double.isNaN(holdTime)) {
                holdTime = (readyTime - startTime) * playbackRate;
            }
            applyPendingPlaybackRate();
            startTime = Double.NaN;
            ready.resolve();
            updateFinishedState(false, false);
            set.changed(this);
        }
    }

    /**
     * Finishes the animation (<code>finish()</code>, 4.4.13): it seeks to the end in the current direction and notifies at once.
     * <p>
     * 애니메이션을 끝낸다(<code>finish()</code>, 4.4.13): 현재 방향의 끝으로 이동하고 즉시 알린다.
     *
     * @throws AnimationException an InvalidStateError with a zero rate or an endless effect played forwards / 속도가 0 이거나 정방향의 끝없는 효과이면 InvalidStateError
     */
    public void finish() {
        double rate = getEffectivePlaybackRate();
        if (rate == 0 || (rate > 0 && Double.isInfinite(getEffectEnd()))) {
            throw new AnimationException(AnimationException.INVALID_STATE, "Cannot finish Animation with a playbackRate of 0 or an infinite target effect end.");
        }
        applyPendingPlaybackRate();
        double limit = playbackRate > 0 ? getEffectEnd() : 0;
        silentlySetCurrentTime(limit);
        if (Double.isNaN(startTime) && timeline != null && timeline.isActive()) {
            startTime = timelineTime() - limit / playbackRate;
        }
        // 대기 pause 작업이 있고 시작 시간이 결정되었으면 일시정지를 취소한다.
        if (pendingPause && !Double.isNaN(startTime)) {
            holdTime = Double.NaN;
            pendingPause = false;
            ready.resolve();
        }
        if (pendingPlay && !Double.isNaN(startTime)) {
            pendingPlay = false;
            ready.resolve();
        }
        updateFinishedState(true, true);
        set.changed(this);
    }

    /**
     * Cancels the animation (<code>cancel()</code>, 4.4.14): its effect disappears, the promises are rejected and a <code>cancel</code> event is
     * queued unless it was idle.
     * <p>
     * 애니메이션을 취소한다(<code>cancel()</code>, 4.4.14): 효과가 사라지고 프라미스가 reject 되며 유휴 상태가 아니었으면 <code>cancel</code> 이벤트가 큐에 들어간다.
     */
    public void cancel() {
        if (!"idle".equals(getPlayState())) {
            resetPendingTasks();
            finished.reject();
            finished = set.newPromise(this, false);
            cancelFinishNotification();
            double tt = timeline != null && timeline.isActive() ? timelineTime() : Double.NaN;
            set.queueEvent(this, new AnimationPlaybackEvent("cancel", Double.NaN, tt), timeline == null ? Double.NaN : timeline.toOriginRelative(tt));
        }
        holdTime = Double.NaN;
        startTime = Double.NaN;
        set.changed(this);
    }

    /**
     * Resets the pending tasks (4.4.14 "reset an animation's pending tasks").
     * <p>
     * 대기 작업을 초기화한다(4.4.14 "reset an animation's pending tasks").
     */
    private void resetPendingTasks() {
        if (!pendingPlay && !pendingPause) {
            return;
        }
        pendingPlay = false;
        pendingPause = false;
        applyPendingPlaybackRate();
        ready.reject();
        ready = set.newPromise(this, true);
    }

    /**
     * Reverses the animation (<code>reverse()</code>, 4.4.16).
     * <p>
     * 애니메이션을 뒤집는다(<code>reverse()</code>, 4.4.16).
     *
     * @throws AnimationException an InvalidStateError without an active timeline or for an endless effect / 활성 타임라인이 없거나 끝없는 효과이면 InvalidStateError
     */
    public void reverse() {
        if (timeline == null || !timeline.isActive()) {
            throw new AnimationException(AnimationException.INVALID_STATE, "Cannot reverse an animation with no active timeline");
        }
        double original = pendingPlaybackRate;
        pendingPlaybackRate = -getEffectivePlaybackRate();
        try {
            play(true);
        } catch (AnimationException e) {
            pendingPlaybackRate = original;
            throw e;
        }
    }

    /**
     * Sets the playback rate at once (<code>animation.playbackRate = r</code>, 4.4.15.1); the current time is kept.
     * <p>
     * 재생 속도를 즉시 정한다(<code>animation.playbackRate = r</code>, 4.4.15.1). 현재 시간은 유지된다.
     *
     * @param rate the rate / 속도
     */
    public void setPlaybackRate(double rate) {
        pendingPlaybackRate = Double.NaN;
        double previous = getCurrentTime();
        playbackRate = rate;
        // 단조 증가하는 타임라인이면 이전 현재 시간을 다시 정해 시간이 튀지 않게 한다.
        if (timeline != null && !Double.isNaN(previous)) {
            setCurrentTime(previous);
        } else {
            set.changed(this);
        }
    }

    /**
     * Updates the playback rate seamlessly (<code>updatePlaybackRate(r)</code>, 4.4.15.2).
     * <p>
     * 재생 속도를 매끄럽게 갱신한다(<code>updatePlaybackRate(r)</code>, 4.4.15.2).
     *
     * @param rate the new rate / 새 속도
     */
    public void updatePlaybackRate(double rate) {
        String previous = getPlayState();
        pendingPlaybackRate = rate;
        if (pendingPlay || pendingPause) {
            set.changed(this);
            return;
        }
        if ("idle".equals(previous) || "paused".equals(previous) || Double.isNaN(getCurrentTime())) {
            applyPendingPlaybackRate();
        } else if ("finished".equals(previous)) {
            double unconstrained = unheldCurrentTime();
            double tt = timelineTime();
            startTime = rate == 0 ? tt : tt - unconstrained / rate;
            applyPendingPlaybackRate();
            updateFinishedState(false, false);
        } else {
            play(false);
            return;
        }
        set.changed(this);
    }

    /**
     * Applies the pending playback rate (4.4.15.2).
     * <p>
     * 대기 재생 속도를 적용한다(4.4.15.2).
     */
    private void applyPendingPlaybackRate() {
        if (Double.isNaN(pendingPlaybackRate)) {
            return;
        }
        playbackRate = pendingPlaybackRate;
        pendingPlaybackRate = Double.NaN;
    }

    /**
     * Sets the replace state to <code>persisted</code> (<code>persist()</code>).
     * <p>
     * 교체 상태를 <code>persisted</code> 로 정한다(<code>persist()</code>).
     */
    public void persist() {
        boolean wasRemoved = "removed".equals(replaceState);
        replaceState = "persisted";
        if (wasRemoved) {
            set.changed(this);
        }
    }

    /**
     * Marks the animation as removed (5.5.2) and queues the <code>remove</code> event.
     * <p>
     * 애니메이션을 제거됨으로 표시하고(5.5.2) <code>remove</code> 이벤트를 큐에 넣는다.
     */
    void markRemoved() {
        replaceState = "removed";
        double tt = timelineTime();
        set.queueEvent(this, new AnimationPlaybackEvent("remove", getCurrentTime(), tt), timeline == null ? Double.NaN : timeline.toOriginRelative(tt));
    }

    // ------------------------------------------------------------------------------------------------ finished state

    /**
     * Runs "update an animation's finished state" (4.4.12).
     * <p>
     * "update an animation's finished state" 를 실행한다(4.4.12).
     *
     * @param didSeek            true after the current time was set / 현재 시간을 정한 뒤이면 true
     * @param synchronouslyNotify true to run the finish notification steps at once / 끝 알림 단계를 즉시 실행하려면 true
     */
    void updateFinishedState(boolean didSeek, boolean synchronouslyNotify) {
        double unconstrained = didSeek ? getCurrentTime() : unheldCurrentTime();
        double end = getEffectEnd();
        // 대기 작업이 없고 시작 시간이 결정된 실행 중인 애니메이션은 경계를 넘으면 유지 시간에 묶인다.
        if (!Double.isNaN(unconstrained) && !Double.isNaN(startTime) && !pendingPlay && !pendingPause) {
            if (playbackRate > 0 && unconstrained >= end) {
                if (didSeek) {
                    holdTime = unconstrained;
                } else {
                    holdTime = Double.isNaN(previousCurrentTime) ? end : Math.max(previousCurrentTime, end);
                }
            } else if (playbackRate < 0 && unconstrained <= 0) {
                if (didSeek) {
                    holdTime = unconstrained;
                } else {
                    holdTime = Double.isNaN(previousCurrentTime) ? 0 : Math.min(previousCurrentTime, 0);
                }
            } else if (playbackRate != 0 && timeline != null && timeline.isActive()) {
                if (didSeek && !Double.isNaN(holdTime)) {
                    startTime = timelineTime() - holdTime / playbackRate;
                }
                holdTime = Double.NaN;
            }
        }
        previousCurrentTime = getCurrentTime();
        boolean finishedNow = "finished".equals(getPlayState());
        if (finishedNow && finished.isPending()) {
            if (synchronouslyNotify) {
                cancelFinishNotification();
                finishNotificationSteps();
            } else if (finishNotification == null) {
                finishNotification = new Runnable() {
                    /**
                     * Runs the finish notification steps.
                     * <p>
                     * 끝 알림 단계를 실행한다.
                     */
                    public void run() {
                        if (finishNotification != this) {
                            return;
                        }
                        finishNotification = null;
                        finishNotificationSteps();
                    }
                };
                set.queueMicrotask(finishNotification);
            }
        }
        if (!finishedNow && !finished.isPending()) {
            finished = set.newPromise(this, false);
        }
    }

    /**
     * Cancels a queued finish notification.
     * <p>
     * 큐에 넣은 끝 알림을 취소한다.
     */
    private void cancelFinishNotification() {
        finishNotification = null;
    }

    /**
     * The finish notification steps (4.4.12): if the animation is still finished, the finished promise is resolved and a <code>finish</code>
     * event is queued.
     * <p>
     * 끝 알림 단계(4.4.12): 애니메이션이 아직 끝난 상태이면 finished 프라미스를 resolve 하고 <code>finish</code> 이벤트를 큐에 넣는다.
     */
    private void finishNotificationSteps() {
        if (!"finished".equals(getPlayState())) {
            return;
        }
        finished.resolve();
        double tt = timelineTime();
        double scheduled = Double.NaN;
        // 예정 시각: 효과의 끝을 기준점 상대 시간으로 바꾼다.
        if (timeline != null && !Double.isNaN(startTime) && playbackRate != 0 && !Double.isInfinite(getEffectEnd())) {
            double endTimeline = getEffectEnd() / playbackRate + startTime;
            scheduled = timeline.toOriginRelative(endTimeline);
        }
        set.queueEvent(this, new AnimationPlaybackEvent("finish", getCurrentTime(), tt), scheduled);
    }

    /**
     * Ends the animation without events or promise changes (the page is disposed or scripts stopped).
     * <p>
     * 이벤트나 프라미스 변경 없이 애니메이션을 끝낸다(페이지가 폐기되거나 스크립트가 멈췄다).
     */
    void abandon() {
        pendingPlay = false;
        pendingPause = false;
        finishNotification = null;
        holdTime = Double.NaN;
        startTime = Double.NaN;
    }
}
