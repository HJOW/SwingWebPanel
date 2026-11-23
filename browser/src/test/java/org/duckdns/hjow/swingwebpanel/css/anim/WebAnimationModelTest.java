package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the animation state machine of Web Animations Level 1 without scripts or styles (sections 4.4 and 5.5): pending play and pause tasks that
 * resolve the ready promise at the next frame with the frame time as the ready time, the current time, the finished state with the finish notification
 * in a microtask (and at once for <code>finish()</code>), canceling, reversing, playback rates, the errors of the procedures, the replace state with
 * the <code>remove</code> event, the order of the queued events and the time of the document between frames.
 * <p>
 * 스크립트나 스타일 없이 Web Animations Level 1 의 애니메이션 상태 기계를 시험한다(4.4 절과 5.5 절): 다음 프레임에서 그 프레임 시각을 준비 시간으로 ready 프라미스를
 * resolve 하는 대기 play/pause 작업, 현재 시간, 마이크로태스크에서(<code>finish()</code> 는 즉시) 끝을 알리는 끝남 상태, 취소, 뒤집기, 재생 속도, 절차의 오류,
 * <code>remove</code> 이벤트가 있는 교체 상태, 큐에 든 이벤트의 순서, 프레임 사이의 문서 시간.
 */
public class WebAnimationModelTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public WebAnimationModelTest() {
    }

    /** The host of the current test. 현재 시험의 호스트. */
    private FakeAnimationHost host;
    /** The registry of the current test. 현재 시험의 등록부. */
    private WebAnimationSet set;

    /**
     * Creates a registry without styles connected to a fake host at time 1000.
     * <p>
     * 시각 1000 의 가짜 호스트에 연결한 스타일 없는 등록부를 만든다.
     */
    private void setUp() {
        host = new FakeAnimationHost();
        set = new WebAnimationSet(null, null);
        set.attach(host, host);
    }

    /**
     * Creates an animation of an effect with a duration on the default timeline.
     * <p>
     * 기본 타임라인에서 지속 시간을 가진 효과의 애니메이션을 만든다.
     *
     * @param id       the id / id
     * @param duration the duration / 지속 시간
     * @param fill     the fill / 채움
     * @return the animation / 애니메이션
     */
    private WebAnimation animation(String id, double duration, String fill) {
        EffectTiming t = new EffectTiming();
        t.setDuration(duration);
        t.setFill(fill);
        WebAnimation a = new WebAnimation(set);
        a.setId(id);
        a.setTimeline(set.getTimeline());
        a.setEffect(new KeyframeEffectModel(t));
        return a;
    }

    /**
     * Returns the state of a promise.
     * <p>
     * 프라미스의 상태를 반환한다.
     *
     * @param p the promise / 프라미스
     * @return the state / 상태
     */
    private static String state(WebAnimationHost.PromiseSlot p) {
        return FakeAnimationHost.state(p);
    }

    /**
     * <code>play()</code> waits for the next frame: the animation is pending and running with the current time 0, the ready promise is pending; the
     * frame takes its time as the start time and resolves the promise; the current time follows the frames and at the end the animation finishes,
     * the finished promise resolves in the microtask checkpoint and a <code>finish</code> event follows.
     * <p>
     * <code>play()</code> 는 다음 프레임을 기다린다: 애니메이션은 현재 시간 0 으로 대기 중이며 실행 중이고 ready 프라미스는 대기 중이다. 프레임이 그 시각을 시작 시간으로
     * 하고 프라미스를 resolve 한다. 현재 시간은 프레임을 따르고 끝에서 애니메이션이 끝나며 finished 프라미스는 마이크로태스크 검사점에서 resolve 되고
     * <code>finish</code> 이벤트가 이어진다.
     */
    public void testPlayIsPendingUntilTheNextFrame() {
        setUp();
        WebAnimation a = animation("a", 1000, "auto");
        Check.equal("idle", a.getPlayState(), "a new animation is idle");
        Check.equal("resolved", state(a.getReady()), "the first ready promise is resolved");
        Check.equal("pending", state(a.getFinished()), "the first finished promise is pending");
        a.play();
        Check.isTrue(a.isPending(), "pending play task");
        Check.equal("running", a.getPlayState(), "running while pending");
        Check.near(0, a.getCurrentTime(), 1e-9, "the hold time 0");
        Check.isTrue(Double.isNaN(a.getStartTime()), "no start time yet");
        Check.equal("pending", state(a.getReady()), "a new ready promise");
        Check.isTrue(host.frameRequests > 0, "a frame was asked for");
        host.frame(set, 1016);
        Check.isFalse(a.isPending(), "the task ran at the frame");
        Check.equal("resolved", state(a.getReady()), "ready resolved");
        Check.near(1016, a.getStartTime(), 1e-9, "the frame time is the ready time and the start time");
        Check.near(0, a.getCurrentTime(), 1e-9, "starts from 0");
        host.time = 1300;
        Check.near(0, a.getCurrentTime(), 1e-9, "the timeline time does not move between frames while animating");
        host.frame(set, 1516);
        Check.near(500, a.getCurrentTime(), 1e-9, "half way");
        Check.near(0.5, a.computeTiming().progress, 1e-9, "progress");
        host.frame(set, 2100);
        Check.equal("finished", a.getPlayState(), "finished after the end");
        Check.near(1000, a.getCurrentTime(), 1e-9, "the current time is held at the end");
        Check.equal("resolved", state(a.getFinished()), "finished resolved");
        Check.equal("[finish:a:1000:2100]", host.events.toString(), "one finish event with the times");
        host.frame(set, 2200);
        Check.equal(1, host.events.size(), "no second finish event");
        Check.near(1000, a.getCurrentTime(), 1e-9, "stays at the end");
    }

    /**
     * <code>pause()</code> is pending until the next frame, which holds the current time; <code>play()</code> continues from the held time; a
     * <code>play()</code> during a pending pause reuses the same ready promise.
     * <p>
     * <code>pause()</code> 는 다음 프레임까지 대기하며 프레임이 현재 시간을 유지한다. <code>play()</code> 는 유지한 시간에서 이어 가고, 대기 pause 중의 <code>play()</code> 는
     * 같은 ready 프라미스를 다시 쓴다.
     */
    public void testPauseAndResume() {
        setUp();
        WebAnimation a = animation("a", 1000, "auto");
        a.play();
        host.frame(set, 1000);
        host.frame(set, 1300);
        a.pause();
        Check.equal("paused", a.getPlayState(), "paused while the pause is pending");
        Check.isTrue(a.isPending(), "pending pause task");
        WebAnimationHost.PromiseSlot ready = a.getReady();
        Check.equal("pending", state(ready), "a new ready promise");
        host.frame(set, 1400);
        Check.equal("resolved", state(ready), "the pause resolved the ready promise");
        Check.near(400, a.getCurrentTime(), 1e-9, "the time of the frame is held");
        Check.isTrue(Double.isNaN(a.getStartTime()), "no start time while paused");
        host.frame(set, 1900);
        Check.near(400, a.getCurrentTime(), 1e-9, "paused: the time does not move");
        a.play();
        host.frame(set, 2000);
        Check.near(2000 - 400, a.getStartTime(), 1e-9, "resumes from the held time");
        host.frame(set, 2100);
        Check.near(500, a.getCurrentTime(), 1e-9, "running again");
        a.pause();
        WebAnimationHost.PromiseSlot p = a.getReady();
        a.play();
        Check.same(p, a.getReady(), "play during a pending pause keeps the ready promise");
        host.frame(set, 2200);
        Check.equal("running", a.getPlayState(), "running");
        Check.equal("resolved", state(p), "resolved");
    }

    /**
     * <code>finish()</code> notifies at once: the finished promise resolves and the event is queued even if the current time is changed right after;
     * an endless effect cannot be finished forwards, nor an animation with a zero rate.
     * <p>
     * <code>finish()</code> 는 즉시 알린다: 바로 뒤에 현재 시간을 바꿔도 finished 프라미스가 resolve 되고 이벤트가 큐에 들어간다. 끝없는 효과는 정방향으로 끝낼 수 없고
     * 속도가 0 인 애니메이션도 그렇다.
     */
    public void testFinishIsSynchronousAndChecked() {
        setUp();
        WebAnimation a = animation("a", 1000, "auto");
        a.play();
        a.finish();
        Check.equal("resolved", state(a.getFinished()), "finished resolved at once");
        Check.equal("resolved", state(a.getReady()), "the pending play task was completed");
        Check.equal("finished", a.getPlayState(), "finished");
        a.setCurrentTime(0);
        Check.equal("pending", state(a.getFinished()), "leaving the finished state gives a new finished promise");
        host.frame(set, 1016);
        Check.equal("[finish:a:1000:1000]", host.events.toString(), "the finish event of finish() was still dispatched");
        WebAnimation e = animation("e", 1000, "auto");
        e.getEffect().getTiming().setIterations(Double.POSITIVE_INFINITY);
        e.play();
        AnimationException x = null;
        try {
            e.finish();
        } catch (AnimationException ex) {
            x = ex;
        }
        Check.notNull(x, "endless effect");
        Check.equal(AnimationException.INVALID_STATE, x.getKind(), "InvalidStateError");
        WebAnimation z = animation("z", 1000, "auto");
        z.setPlaybackRate(0);
        x = null;
        try {
            z.finish();
        } catch (AnimationException ex) {
            x = ex;
        }
        Check.notNull(x, "zero rate");
    }

    /**
     * <code>cancel()</code> rejects the pending promises, replaces them, queues a <code>cancel</code> event with a null current time and makes the
     * animation idle; canceling an idle animation does nothing more.
     * <p>
     * <code>cancel()</code> 은 대기 중인 프라미스를 reject 하고 바꾸며, 현재 시간이 null 인 <code>cancel</code> 이벤트를 큐에 넣고 애니메이션을 유휴 상태로 만든다. 유휴
     * 애니메이션의 취소는 더 하는 일이 없다.
     */
    public void testCancel() {
        setUp();
        WebAnimation a = animation("a", 1000, "auto");
        a.play();
        WebAnimationHost.PromiseSlot ready = a.getReady();
        WebAnimationHost.PromiseSlot finished = a.getFinished();
        a.cancel();
        Check.equal("rejected", state(ready), "the pending ready promise is rejected");
        Check.equal("rejected", state(finished), "the finished promise is rejected");
        Check.equal("resolved", state(a.getReady()), "a new resolved ready promise");
        Check.equal("pending", state(a.getFinished()), "a new pending finished promise");
        Check.equal("idle", a.getPlayState(), "idle");
        Check.isTrue(Double.isNaN(a.getCurrentTime()), "no current time");
        a.cancel();
        host.frame(set, 1016);
        Check.equal("[cancel:a:null:1000]", host.events.toString(), "one cancel event with a null current time");
    }

    /**
     * <code>reverse()</code> plays backwards from the current time and finishes at 0; <code>playbackRate</code> keeps the current time;
     * <code>updatePlaybackRate()</code> applies at the next frame without a jump; a reversed endless animation cannot be played.
     * <p>
     * <code>reverse()</code> 는 현재 시간에서 거꾸로 재생해 0 에서 끝나고, <code>playbackRate</code> 는 현재 시간을 유지하며, <code>updatePlaybackRate()</code> 는 다음
     * 프레임에 튀지 않고 적용된다. 역방향의 끝없는 애니메이션은 재생할 수 없다.
     */
    public void testReverseAndRates() {
        setUp();
        WebAnimation a = animation("a", 1000, "auto");
        a.play();
        host.frame(set, 1000);
        host.frame(set, 1300);
        a.reverse();
        Check.near(-1, a.getEffectivePlaybackRate(), 1e-9, "pending rate -1");
        host.frame(set, 1400);
        Check.near(-1, a.getPlaybackRate(), 1e-9, "applied at the frame");
        Check.near(400, a.getCurrentTime(), 1e-9, "continues from the time of the frame");
        host.frame(set, 1600);
        Check.near(200, a.getCurrentTime(), 1e-9, "goes backwards");
        host.frame(set, 1900);
        Check.equal("finished", a.getPlayState(), "finished at 0");
        Check.near(0, a.getCurrentTime(), 1e-9, "held at 0");
        WebAnimation b = animation("b", 1000, "auto");
        b.play();
        host.frame(set, 2000);
        host.frame(set, 2100);
        b.setPlaybackRate(2);
        Check.near(100, b.getCurrentTime(), 1e-9, "a rate change keeps the current time");
        host.frame(set, 2200);
        Check.near(300, b.getCurrentTime(), 1e-9, "twice as fast");
        b.updatePlaybackRate(0.5);
        Check.near(300, b.getCurrentTime(), 1e-9, "no jump before the frame");
        host.frame(set, 2300);
        Check.near(500, b.getCurrentTime(), 1e-9, "the old rate until the frame applies the new one");
        host.frame(set, 2500);
        Check.near(600, b.getCurrentTime(), 1e-9, "half as fast after the frame");
        WebAnimation e = animation("e", 1000, "auto");
        e.getEffect().getTiming().setIterations(Double.POSITIVE_INFINITY);
        e.setPlaybackRate(-1);
        AnimationException x = null;
        try {
            e.play();
        } catch (AnimationException ex) {
            x = ex;
        }
        Check.notNull(x, "a reversed endless animation cannot be played");
        Check.equal(AnimationException.INVALID_STATE, x.getKind(), "InvalidStateError");
    }

    /**
     * Seeking: the current time of an idle animation can be set (it becomes paused), but a resolved current time cannot become null (TypeError); the
     * start time can be set directly and resolves a pending play task; setting a time past the end finishes the animation.
     * <p>
     * 이동: 유휴 애니메이션의 현재 시간을 정할 수 있고(일시정지 상태가 된다) 결정된 현재 시간을 null 로 바꿀 수는 없다(TypeError). 시작 시간을 직접 정할 수 있으며 대기
     * play 작업을 끝낸다. 끝을 넘는 시간을 정하면 애니메이션이 끝난다.
     */
    public void testSeeking() {
        setUp();
        WebAnimation a = animation("a", 1000, "auto");
        a.setCurrentTime(250);
        Check.equal("paused", a.getPlayState(), "a seeked idle animation is paused");
        Check.near(250, a.getCurrentTime(), 1e-9, "held");
        AnimationException x = null;
        try {
            a.setCurrentTime(Double.NaN);
        } catch (AnimationException ex) {
            x = ex;
        }
        Check.notNull(x, "null current time");
        Check.equal(AnimationException.TYPE_ERROR, x.getKind(), "TypeError");
        WebAnimation b = animation("b", 1000, "auto");
        b.play();
        WebAnimationHost.PromiseSlot ready = b.getReady();
        b.setStartTime(900);
        Check.isFalse(b.isPending(), "setting the start time ends the pending task");
        Check.equal("resolved", state(ready), "and resolves ready");
        Check.near(100, b.getCurrentTime(), 1e-9, "1000 - 900");
        b.setCurrentTime(5000);
        Check.equal("finished", b.getPlayState(), "past the end");
        Check.near(5000, b.getCurrentTime(), 1e-9, "a seek may go past the end");
    }

    /**
     * Two finished filling animations of the same target and property: the older one is removed at the next frame with a <code>remove</code> event;
     * a persisted one is kept; an animation of another property is kept.
     * <p>
     * 같은 대상과 속성의 끝나서 채우는 두 애니메이션: 오래된 것은 다음 프레임에 <code>remove</code> 이벤트와 함께 제거되고, 유지된 것은 남으며, 다른 속성의 애니메이션도
     * 남는다.
     */
    public void testReplacedAnimationsAreRemoved() {
        setUp();
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><div id=d></div>");
        Element d = doc.getElementById("d");
        WebAnimation a = keyframed("a", d, "opacity", "0.5");
        WebAnimation b = keyframed("b", d, "opacity", "0.2");
        WebAnimation c = keyframed("c", d, "marginLeft", "10px");
        WebAnimation p = keyframed("p", d, "color", "red");
        WebAnimation q = keyframed("q", d, "color", "blue");
        p.persist();
        a.finish();
        b.finish();
        c.finish();
        p.finish();
        q.finish();
        host.frame(set, 1016);
        Check.equal("removed", a.getReplaceState(), "the older opacity animation is removed");
        Check.equal("active", b.getReplaceState(), "the newer one stays");
        Check.equal("active", c.getReplaceState(), "another property stays");
        Check.equal("persisted", p.getReplaceState(), "a persisted one stays");
        Check.contains(host.events.toString(), "remove:a:100:1016", "the remove event");
        List<WebAnimation> list = set.getAnimations(doc, true);
        Check.isFalse(list.contains(a), "a removed animation is not relevant");
        Check.isTrue(list.contains(b) && list.contains(c) && list.contains(p) && list.contains(q), "the others are");
        Check.isTrue(list.indexOf(b) < list.indexOf(c), "composite order");
    }

    /**
     * Creates a finished-fill animation of one property on a target.
     * <p>
     * 대상의 속성 하나를 가진 채움 애니메이션을 만든다.
     *
     * @param id       the id / id
     * @param target   the target / 대상
     * @param idl      the IDL name / IDL 이름
     * @param value    the value / 값
     * @return the animation / 애니메이션
     */
    private WebAnimation keyframed(String id, Element target, String idl, String value) {
        WebAnimation a = animation(id, 100, "forwards");
        Map<String, String> pairs = new LinkedHashMap<String, String>();
        pairs.put(KeyframeEffectModel.animatableProperty(idl), value);
        List<KeyframeEffectModel.Keyframe> list = new ArrayList<KeyframeEffectModel.Keyframe>();
        list.add(KeyframeEffectModel.newKeyframe(null, "linear", null, pairs));
        a.getEffect().setKeyframes(list);
        a.getEffect().setTarget(target, null);
        a.play();
        return a;
    }

    /**
     * The events of one frame are sorted by their scheduled time (the end of the effect on the timeline) and then by composite order, whatever the order
     * in which they were queued; an animation that finished without fill is let go and comes back when a script plays it again.
     * <p>
     * 한 프레임의 이벤트는 큐에 들어간 순서와 관계없이 예정 시각(타임라인 위의 효과 끝), 그 다음 합성 순서로 정렬된다. 채움 없이 끝난 애니메이션은 놓아 주고 스크립트가 다시
     * 재생하면 돌아온다.
     */
    public void testEventOrderAndLettingGo() {
        setUp();
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><div id=d></div>");
        Element d = doc.getElementById("d");
        WebAnimation late = animation("late", 300, "auto");
        WebAnimation early = animation("early", 100, "auto");
        late.getEffect().setTarget(d, null);
        early.getEffect().setTarget(d, null);
        late.play();
        early.play();
        host.frame(set, 1000);
        host.frame(set, 1500);
        Check.equal("[finish:early:100:1500, finish:late:300:1500]", host.events.toString(), "sorted by the scheduled end, not by creation");
        Check.isFalse(set.targets(d), "finished without fill: let go");
        Check.equal(0, set.getAnimations(d, false).size(), "not relevant");
        late.play();
        Check.isTrue(set.targets(d), "back when played again");
        Check.equal(1, set.getAnimations(d, false).size(), "relevant again");
    }

    /**
     * The time of the document follows the clock while nothing animates and stays at the frame time while a frame is awaited; it never goes backwards;
     * after {@link WebAnimationSet#detach()} the timeline is inactive and the animations have no effect.
     * <p>
     * 문서 시간은 애니메이션되는 것이 없으면 시계를 따르고 프레임을 기다리는 동안은 프레임 시각에 머물며, 뒤로 가지 않는다. {@link WebAnimationSet#detach()} 뒤에는
     * 타임라인이 비활성이고 애니메이션의 효과가 없다.
     */
    public void testDocumentTime() {
        setUp();
        WebTimeline t = set.getTimeline();
        host.time = 2000;
        Check.near(2000, t.getCurrentTime(), 1e-9, "follows the clock when idle");
        WebTimeline other = new WebTimeline(set, 500);
        Check.near(1500, other.getCurrentTime(), 1e-9, "a timeline with an origin time");
        Check.near(2000, other.toOriginRelative(1500), 1e-9, "origin-relative");
        WebAnimation a = animation("a", 1000, "auto");
        a.play();
        host.time = 2050;
        Check.near(2000, t.getCurrentTime(), 1e-9, "frozen while a frame is awaited");
        host.frame(set, 2066);
        Check.near(2066, t.getCurrentTime(), 1e-9, "the frame time");
        set.update(1000);
        Check.near(2066, t.getCurrentTime(), 1e-9, "never backwards");
        set.detach();
        Check.isFalse(t.isActive(), "inactive without a host");
        Check.isTrue(Double.isNaN(a.getCurrentTime()), "the animation lost its time");
        Check.isTrue(set.getAnimations(null, true).isEmpty(), "nothing is relevant any more");
        Check.isTrue(set.flushStopped(), "the document thread releases the animations");
        Check.isFalse(set.flushStopped(), "only once");
    }
}
