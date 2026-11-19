package org.duckdns.hjow.swingwebpanel.css.anim;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the timing calculations of Web Animations Level 1 (sections 4.5 to 4.11, W3C Working Draft 5 June 2023): phases, active time, the
 * endpoint-exclusive iteration boundaries, the fill at the end of an iteration, the iteration start, the directions, zero durations with endless
 * iterations, negative end delays, the before flag of step easings and the validation of timing dictionaries.
 * <p>
 * Web Animations Level 1 의 시간 계산 시험(4.5~4.11 절, W3C Working Draft 2023-06-05): 단계, 활성 시간, 끝점 제외 반복 경계, 반복 끝의 채움, 반복 시작, 방향,
 * 끝없는 반복의 0 지속 시간, 음의 끝 지연, 계단 완화의 before 플래그, 시간 사전의 검사.
 */
public class EffectTimingTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public EffectTimingTest() {
    }

    /**
     * Creates a timing with a duration, an iteration count and a fill mode.
     * <p>
     * 지속 시간, 반복 횟수, 채움 모드로 시간을 만든다.
     *
     * @param duration   the duration / 지속 시간
     * @param iterations the iteration count / 반복 횟수
     * @param fill       the fill / 채움
     * @return the timing / 시간
     */
    private static EffectTiming timing(double duration, double iterations, String fill) {
        EffectTiming t = new EffectTiming();
        t.setDuration(duration);
        t.setIterations(iterations);
        t.setFill(fill);
        return t;
    }

    /**
     * The progress and the current iteration of 2.5 iterations of 1 s: the end of an iteration belongs to the next one (endpoint-exclusive), the
     * active duration and the end time are derived, and after the end the fill holds the progress of the last moment.
     * <p>
     * 1 초를 2.5 번 반복할 때의 진행도와 현재 반복: 반복의 끝은 다음 반복에 속하고(끝점 제외), 활성 지속 시간과 끝 시간이 계산되며, 끝 뒤에는 채움이 마지막 순간의
     * 진행도를 유지한다.
     */
    public void testIterationsAndEndpointExclusiveBoundaries() {
        EffectTiming t = timing(1000, 2.5, "both");
        Check.near(2500, t.getActiveDuration(), 1e-9, "active duration");
        Check.near(2500, t.getEndTime(), 1e-9, "end time");
        EffectTiming.Computed c = t.compute(0, false, true);
        Check.equal(EffectTiming.PHASE_ACTIVE, c.phase, "active at 0");
        Check.near(0, c.progress, 1e-9, "progress 0");
        Check.near(0, c.currentIteration, 1e-9, "iteration 0");
        c = t.compute(500, false, true);
        Check.near(0.5, c.progress, 1e-9, "half of the first iteration");
        c = t.compute(1000, false, true);
        Check.near(0, c.progress, 1e-9, "the boundary starts the next iteration");
        Check.near(1, c.currentIteration, 1e-9, "second iteration");
        c = t.compute(2500, false, true);
        Check.equal(EffectTiming.PHASE_AFTER, c.phase, "after at the end time going forwards");
        Check.near(0.5, c.progress, 1e-9, "the fill keeps the progress of the end (half an iteration)");
        Check.near(2, c.currentIteration, 1e-9, "third iteration");
        Check.equal(EffectTiming.PHASE_ACTIVE, t.compute(2500, true, true).phase, "going backwards the end time is still active");
    }

    /**
     * An active interval that ends exactly at the end of an iteration fills with 1, not with 0 of a next iteration; without a fill the after
     * phase has no active time; a start delay gives a before phase that only a backwards fill fills.
     * <p>
     * 반복의 끝에서 정확히 끝나는 활성 구간은 다음 반복의 0 이 아니라 1 로 채우고, 채움이 없으면 after 단계에 활성 시간이 없으며, 시작 지연은 backwards 채움만 채우는 before
     * 단계를 만든다.
     */
    public void testFillModesAndDelays() {
        EffectTiming t = timing(1000, 2, "forwards");
        EffectTiming.Computed c = t.compute(3000, false, true);
        Check.equal(EffectTiming.PHASE_AFTER, c.phase, "after");
        Check.near(1, c.progress, 1e-9, "the end of the last iteration");
        Check.near(1, c.currentIteration, 1e-9, "the last iteration index");
        Check.isFalse(timing(1000, 2, "none").compute(3000, false, true).inEffect(), "no fill: not in effect after the end");
        Check.isFalse(timing(1000, 2, "auto").compute(3000, false, true).inEffect(), "auto is none for a keyframe effect");
        EffectTiming d = timing(1000, 1, "none");
        d.setDelay(500);
        c = d.compute(200, false, true);
        Check.equal(EffectTiming.PHASE_BEFORE, c.phase, "before the delay ends");
        Check.isFalse(c.inEffect(), "no backwards fill");
        d.setFill("backwards");
        c = d.compute(200, false, true);
        Check.near(0, c.progress, 1e-9, "the backwards fill holds the start");
        Check.near(0.3, d.compute(800, false, true).progress, 1e-9, "the delay shifts the active interval");
        EffectTiming e = timing(1000, 1, "forwards");
        e.setEndDelay(-400);
        Check.near(600, e.getEndTime(), 1e-9, "a negative end delay ends early");
        c = e.compute(600, false, true);
        Check.equal(EffectTiming.PHASE_AFTER, c.phase, "after from the end time on");
        Check.near(0.6, c.progress, 1e-9, "the fill gives the progress at the end time");
        Check.equal(EffectTiming.PHASE_ACTIVE, e.compute(599, false, true).phase, "active just before");
        e.setEndDelay(-2000);
        Check.near(0, e.getEndTime(), 1e-9, "the end time is never negative");
    }

    /**
     * The iteration start shifts the progress, and the directions reverse whole iterations (alternate from the second, alternate-reverse from the first).
     * <p>
     * 반복 시작은 진행도를 옮기고, 방향은 반복 전체를 뒤집는다(alternate 는 둘째부터, alternate-reverse 는 첫째부터).
     */
    public void testIterationStartAndDirections() {
        EffectTiming t = timing(1000, 1, "both");
        t.setIterationStart(0.5);
        Check.near(0.5, t.compute(0, false, true).progress, 1e-9, "starts half way");
        EffectTiming.Computed c = t.compute(500, false, true);
        Check.near(0, c.progress, 1e-9, "wraps to the next iteration");
        Check.near(1, c.currentIteration, 1e-9, "in the second iteration");
        EffectTiming a = timing(1000, 2, "both");
        a.setDirection("alternate");
        Check.near(0.25, a.compute(250, false, true).progress, 1e-9, "alternate: the first iteration forwards");
        Check.near(0.75, a.compute(1250, false, true).progress, 1e-9, "alternate: the second iteration backwards");
        a.setDirection("alternate-reverse");
        Check.near(0.75, a.compute(250, false, true).progress, 1e-9, "alternate-reverse: the first iteration backwards");
        a.setDirection("reverse");
        Check.near(0.9, a.compute(1100, false, true).progress, 1e-9, "reverse: every iteration backwards");
    }

    /**
     * A zero duration that repeats forever is in the after phase at once and reports the iteration Infinity; an endless effect has an infinite end.
     * <p>
     * 끝없이 반복하는 0 지속 시간은 즉시 after 단계이며 반복 Infinity 를 알리고, 끝없는 효과의 끝은 무한대이다.
     */
    public void testZeroDurationAndEndlessIterations() {
        EffectTiming t = timing(0, Double.POSITIVE_INFINITY, "forwards");
        EffectTiming.Computed c = t.compute(0, false, true);
        Check.equal(EffectTiming.PHASE_AFTER, c.phase, "after at once");
        Check.isTrue(Double.isInfinite(c.currentIteration), "the iteration is Infinity");
        Check.near(1, c.progress, 1e-9, "the end of the iteration");
        Check.near(0, t.getActiveDuration(), 1e-9, "zero times infinity is zero");
        EffectTiming e = timing(1000, Double.POSITIVE_INFINITY, "none");
        Check.isTrue(Double.isInfinite(e.getEndTime()), "endless");
        Check.near(0.25, e.compute(1000000250, false, true).progress, 1e-9, "far into the iterations");
        EffectTiming auto = new EffectTiming();
        Check.near(0, auto.getIterationDuration(), 1e-9, "duration auto is zero");
        Check.isTrue(Double.isNaN(auto.getDuration()), "the specified duration stays auto");
        Check.isFalse(new EffectTiming().compute(Double.NaN, false, true).inEffect(), "no local time, no effect");
    }

    /**
     * The timing function: an easing applies to the directed progress, and a step easing that jumps at the start keeps the step before the jump in
     * the before phase (the before flag of CSS Easing Level 1).
     * <p>
     * 완화 함수: 완화는 방향을 적용한 진행도에 적용되고, 시작에서 점프하는 계단 완화는 before 단계에서 점프 전의 단계를 유지한다(CSS Easing Level 1 의 before 플래그).
     */
    public void testEasingAndBeforeFlag() {
        EffectTiming t = timing(1000, 1, "both");
        Check.isTrue(t.setEasing("steps(2, jump-start)"), "the easing parses");
        t.setDelay(1000);
        Check.near(0, t.compute(0, false, true).progress, 1e-9, "before phase: the step before the jump");
        Check.near(0.5, t.compute(1000, false, true).progress, 1e-9, "active start: the first jump happened");
        Check.near(1, t.compute(1600, false, true).progress, 1e-9, "second step");
        EffectTiming e = timing(1000, 1, "both");
        Check.isTrue(e.setEasing("ease-in"), "ease-in parses");
        Check.equal("ease-in", e.getEasing(), "serialized keyword");
        Check.isTrue(e.compute(500, false, true).progress < 0.5, "ease-in is slow at first");
        Check.isFalse(e.setEasing("bogus"), "an unknown easing is refused");
        Check.equal("ease-in", e.getEasing(), "and leaves the easing unchanged");
    }

    /**
     * The checks of the timing dictionaries (6.5.4): negative iteration start, negative or NaN iterations and durations and an easing that does not
     * parse (also a list or a CSS-wide keyword) give the TypeError messages.
     * <p>
     * 시간 사전의 검사(6.5.4): 음의 반복 시작, 음수나 NaN 인 반복 횟수와 지속 시간, 해석되지 않는 완화(목록이나 CSS 전역 키워드 포함)는 TypeError 메시지를 준다.
     */
    public void testValidation() {
        Check.isNull(EffectTiming.validate(0, Double.valueOf(1), Double.valueOf(1000), "linear"), "valid");
        Check.isNull(EffectTiming.validate(Double.NaN, Double.valueOf(Double.POSITIVE_INFINITY), null, null), "missing members and Infinity");
        Check.notNull(EffectTiming.validate(-1, null, null, null), "negative iteration start");
        Check.notNull(EffectTiming.validate(Double.NaN, Double.valueOf(-1), null, null), "negative iterations");
        Check.notNull(EffectTiming.validate(Double.NaN, Double.valueOf(Double.NaN), null, null), "NaN iterations");
        Check.notNull(EffectTiming.validate(Double.NaN, null, Double.valueOf(-5), null), "negative duration");
        Check.notNull(EffectTiming.validate(Double.NaN, null, null, "nope"), "unknown easing");
        Check.isNull(EffectTiming.parseEasing("ease, linear"), "a list is not one easing");
        Check.isNull(EffectTiming.parseEasing("initial"), "a CSS-wide keyword is not an easing");
        Check.equal("cubic-bezier(0.1, 0.2, 0.3, 0.4)", EffectTiming.parseEasing("cubic-bezier(0.1,0.2,0.3,0.4)").toCssText(), "serialization");
    }
}
