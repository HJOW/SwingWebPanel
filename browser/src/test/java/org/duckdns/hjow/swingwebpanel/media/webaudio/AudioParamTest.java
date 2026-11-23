package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the automation of an audio parameter (Web Audio API 1.6, plan item P25-03): the value of every automation method at exact
 * times and at sample frames against the formulas of the specification (written out in the test), the order of events at the same time,
 * the error rules, the cancel operations, the continuity of ramps after a target, the clamping to the nominal range, the a-rate and the
 * k-rate evaluation, the clamping of times to the current time and the folding of old events.
 * <p>
 * 오디오 파라미터 자동화 시험(Web Audio API 1.6, 계획 P25-03): 모든 자동화 메서드의 값을 정확한 시간과 샘플 프레임에서 규격의 식(시험에 직접 적었다)과
 * 비교하고, 같은 시간 이벤트의 순서, 오류 규칙, 취소 연산, 목표 뒤 램프의 연속성, 명목 범위 제한, a-rate/k-rate 계산, 시간을 현재 시간으로 맞추기, 오래된
 * 이벤트의 접기를 확인한다.
 */
public class AudioParamTest {

    /**
     * The sample rate of the tests.
     * <p>
     * 시험의 샘플링 주파수.
     */
    private static final double SR = 8000.0;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioParamTest() {
    }

    /**
     * Creates a context and returns the gain parameter of a new gain node (default 1, unlimited range).
     * <p>
     * 컨텍스트를 만들고 새 게인 노드의 gain 파라미터(기본 1, 범위 제한 없음)를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    private static AudioParam gain() {
        return WebAudioTestKit.mono(8000 * 20).createGain().getGain();
    }

    /**
     * Computes the first quantum that starts at a frame and returns the value at a frame of it.
     * <p>
     * 프레임에서 시작하는 quantum 을 계산하고 그 안의 프레임에서의 값을 반환한다.
     *
     * @param p     the parameter / 파라미터
     * @param start the first frame of the quantum / quantum 첫 프레임
     * @param i     the index in the quantum / quantum 안의 인덱스
     * @return the computed value / 계산된 값
     */
    private static float at(AudioParam p, long start, int i) {
        p.computeValues(start, 128);
        return p.valueAt(i);
    }

    /**
     * Default values, ranges and automation rates of the parameters of the first-wave nodes.
     * <p>
     * 1차 노드 파라미터의 기본 값, 범위, 자동화 속도.
     */
    public void testDefaultsRangesAndRates() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        AudioParam g = ctx.createGain().getGain();
        Check.near(1.0, g.getDefaultValue(), 0, "gain default");
        Check.near(1.0, g.getValue(), 0, "gain value");
        Check.near(-Float.MAX_VALUE, g.getMinValue(), 0, "gain min");
        Check.near(Float.MAX_VALUE, g.getMaxValue(), 0, "gain max");
        Check.equal(AutomationRate.A_RATE, g.getAutomationRate(), "gain rate");
        OscillatorNode osc = ctx.createOscillator();
        Check.near(440.0, osc.getFrequency().getValue(), 0, "frequency default");
        Check.near(-4000.0, osc.getFrequency().getMinValue(), 0, "frequency min is minus the Nyquist frequency");
        Check.near(4000.0, osc.getFrequency().getMaxValue(), 0, "frequency max is the Nyquist frequency");
        Check.near(153600.0, osc.getDetune().getMaxValue(), 0, "detune max");
        AudioBufferSourceNode src = ctx.createBufferSource();
        Check.equal(AutomationRate.K_RATE, src.getPlaybackRate().getAutomationRate(), "playbackRate is k-rate");
        Check.equal(AutomationRate.K_RATE, src.getDetune().getAutomationRate(), "detune is k-rate");
        BiquadFilterNode bq = ctx.createBiquadFilter();
        Check.near(350.0, bq.getFrequency().getValue(), 0, "biquad frequency default");
        Check.near(1.0, bq.getQ().getValue(), 0, "biquad Q default");
        Check.near(0.0, bq.getGain().getValue(), 0, "biquad gain default");
        Check.near(1541.0, bq.getGain().getMaxValue(), 1.0, "biquad gain max is about 1541 dB");
    }

    /**
     * The k-rate parameters of a buffer source refuse a-rate; other parameters can change their rate.
     * <p>
     * 버퍼 원천의 k-rate 파라미터는 a-rate 를 거부하고, 다른 파라미터는 속도를 바꿀 수 있다.
     */
    public void testAutomationRateConstraint() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        final AudioParam rate = ctx.createBufferSource().getPlaybackRate();
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                rate.setAutomationRate(AutomationRate.A_RATE);
            }
        }, "a-rate for playbackRate");
        rate.setAutomationRate(AutomationRate.K_RATE);
        AudioParam g = ctx.createGain().getGain();
        g.setAutomationRate(AutomationRate.K_RATE);
        Check.equal(AutomationRate.K_RATE, g.getAutomationRate(), "gain can be k-rate");
        g.setAutomationRate(AutomationRate.A_RATE);
        Check.equal(AutomationRate.A_RATE, g.getAutomationRate(), "gain can be a-rate again");
    }

    /**
     * <code>setValueAtTime</code> holds the value from the first frame at or after the time; times between frames are not rounded.
     * <p>
     * <code>setValueAtTime</code> 은 시간 이후의 첫 프레임부터 값을 유지한다. 프레임 사이의 시간은 반올림하지 않는다.
     */
    public void testSetValueAtTime() {
        AudioParam p = gain();
        p.setValueAtTime(0.5f, 80 / SR);
        p.computeValues(0, 128);
        Check.near(1.0, p.valueAt(79), 0, "before the time");
        Check.near(0.5, p.valueAt(80), 0, "at the time");
        Check.near(0.5, p.valueAt(127), 0, "after the time");
        AudioParam q = gain();
        q.setValueAtTime(0.25f, 80.5 / SR);
        q.computeValues(0, 128);
        Check.near(1.0, q.valueAt(80), 0, "frame 80 is before a time of 80.5 frames");
        Check.near(0.25, q.valueAt(81), 0, "frame 81 is the first frame at or after it");
    }

    /**
     * A linear ramp follows <code>v(t) = V0 + (V1 - V0) (t - T0) / (T1 - T0)</code> and holds the end value afterwards.
     * <p>
     * 선형 램프는 <code>v(t) = V0 + (V1 - V0) (t - T0) / (T1 - T0)</code> 를 따르고 이후에는 끝 값을 유지한다.
     */
    public void testLinearRamp() {
        AudioParam p = gain();
        p.setValueAtTime(0f, 0);
        p.linearRampToValueAtTime(1f, 0.5);
        p.computeValues(0, 128);
        // 프레임마다 v(t) = t / 0.5 와 비교한다.
        for (int i = 0; i < 128; i++) {
            Check.near(i / SR / 0.5, p.valueAt(i), 1e-6, "ramp at frame " + i);
        }
        Check.near(1.0, at(p, 4000, 0), 0, "end value at the end time");
        Check.near(1.0, at(p, 5000, 100), 0, "end value is held");
    }

    /**
     * An exponential ramp follows <code>v(t) = V0 (V1 / V0)^((t - T0) / (T1 - T0))</code>; with opposite signs, or a start of 0, the start
     * value is held; a value of 0 is a <code>RangeError</code>.
     * <p>
     * 지수 램프는 <code>v(t) = V0 (V1 / V0)^((t - T0) / (T1 - T0))</code> 를 따른다. 부호가 반대이거나 시작이 0 이면 시작 값을 유지하고, 값 0 은
     * <code>RangeError</code> 이다.
     */
    public void testExponentialRamp() {
        final AudioParam p = gain();
        p.setValueAtTime(1f, 0);
        p.exponentialRampToValueAtTime(8f, 3.0);
        Check.near(Math.pow(8.0, 1.5 / 3.0), p.getIntrinsicValueAt(1.5), 1e-9, "midpoint is the geometric mean");
        Check.near(2.0, p.getIntrinsicValueAt(1.0), 1e-9, "one third of the way");
        Check.near(8.0, p.getIntrinsicValueAt(3.5), 0, "end value is held");
        float v = at(p, 0, 64);
        Check.near(Math.pow(8.0, (64 / SR) / 3.0), v, 1e-6, "sample of the ramp");
        AudioParam neg = gain();
        neg.setValueAtTime(-1f, 0);
        neg.exponentialRampToValueAtTime(8f, 3.0);
        Check.near(-1.0, neg.getIntrinsicValueAt(1.0), 0, "opposite signs hold the start value");
        Check.near(8.0, neg.getIntrinsicValueAt(3.0), 0, "and reach the end value at the end time");
        AudioParam zero = gain();
        zero.setValueAtTime(0f, 0);
        zero.exponentialRampToValueAtTime(8f, 3.0);
        Check.near(0.0, zero.getIntrinsicValueAt(1.0), 0, "a start of 0 holds the start value");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.exponentialRampToValueAtTime(0f, 5.0);
            }
        }, "exponential ramp to 0");
    }

    /**
     * A ramp without a previous event starts from the current value at the current time as if <code>setValueAtTime</code> was called.
     * <p>
     * 이전 이벤트가 없는 램프는 <code>setValueAtTime</code> 을 부른 것처럼 현재 시간의 현재 값에서 시작한다.
     */
    public void testRampWithoutPreviousEvent() {
        AudioParam p = gain();
        p.linearRampToValueAtTime(3f, 1.0);
        Check.near(1.0, p.getIntrinsicValueAt(0.0), 0, "starts at the current value");
        Check.near(2.0, p.getIntrinsicValueAt(0.5), 1e-9, "linear from 1 to 3");
        AudioParam e = gain();
        e.exponentialRampToValueAtTime(4f, 2.0);
        Check.near(2.0, e.getIntrinsicValueAt(1.0), 1e-9, "exponential from 1 to 4");
    }

    /**
     * <code>setTargetAtTime</code> follows <code>v(t) = V1 + (V0 - V1) e^(-(t - T0) / tau)</code>; a time constant of 0 jumps to the
     * target; a negative time constant is a <code>RangeError</code>.
     * <p>
     * <code>setTargetAtTime</code> 은 <code>v(t) = V1 + (V0 - V1) e^(-(t - T0) / tau)</code> 를 따른다. 시간 상수 0 은 목표로 바로 가고, 음수 시간 상수는
     * <code>RangeError</code> 이다.
     */
    public void testSetTarget() {
        final AudioParam p = gain();
        p.setValueAtTime(2f, 0);
        p.setTargetAtTime(0.5f, 0.25, 0.5f);
        Check.near(2.0, p.getIntrinsicValueAt(0.2), 0, "before the start");
        Check.near(2.0, p.getIntrinsicValueAt(0.25), 1e-9, "at the start");
        Check.near(0.5 + 1.5 * Math.exp(-(1.0 - 0.25) / 0.5), p.getIntrinsicValueAt(1.0), 1e-9, "approaching the target");
        p.computeValues(2000, 128);
        // 샘플마다 식과 비교한다.
        for (int i = 0; i < 128; i += 17) {
            double t = (2000 + i) / SR;
            Check.near(0.5 + 1.5 * Math.exp(-(t - 0.25) / 0.5), p.valueAt(i), 1e-6, "sample " + i);
        }
        AudioParam j = gain();
        j.setTargetAtTime(7f, 1.0, 0f);
        Check.near(7.0, j.getIntrinsicValueAt(1.0), 0, "a time constant of 0 jumps");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.setTargetAtTime(1f, 1.0, -1f);
            }
        }, "negative time constant");
    }

    /**
     * A target event ends at the next event.
     * <p>
     * 목표 이벤트는 다음 이벤트에서 끝난다.
     */
    public void testTargetEndsAtNextEvent() {
        AudioParam p = gain();
        p.setValueAtTime(0f, 0);
        p.setTargetAtTime(1f, 0, 1f);
        p.setValueAtTime(5f, 2.0);
        Check.near(1 - Math.exp(-1.0), p.getIntrinsicValueAt(1.0), 1e-9, "target at 1");
        Check.near(5.0, p.getIntrinsicValueAt(2.0), 0, "the set value replaces the target");
        Check.near(5.0, p.getIntrinsicValueAt(3.0), 0, "and holds");
    }

    /**
     * A value curve linearly interpolates its values over the duration and holds the last value; a curve of fewer than 2 values is an
     * <code>InvalidStateError</code>, a duration that is not positive a <code>RangeError</code>.
     * <p>
     * 값 곡선은 지속 시간에 걸쳐 값을 선형 보간하고 마지막 값을 유지한다. 값이 2개 미만이면 <code>InvalidStateError</code>, 지속 시간이 양수가 아니면
     * <code>RangeError</code> 이다.
     */
    public void testSetValueCurve() {
        final AudioParam p = gain();
        p.setValueCurveAtTime(new float[] {0f, 1f, 0f, 2f}, 1.0, 3.0);
        Check.near(1.0, p.getIntrinsicValueAt(0.5), 0, "before the curve");
        Check.near(0.0, p.getIntrinsicValueAt(1.0), 1e-9, "first value");
        Check.near(0.5, p.getIntrinsicValueAt(1.5), 1e-9, "between the first two");
        Check.near(1.0, p.getIntrinsicValueAt(2.0), 1e-9, "second value");
        Check.near(0.0, p.getIntrinsicValueAt(3.0), 1e-9, "third value");
        Check.near(1.0, p.getIntrinsicValueAt(3.5), 1e-9, "between the last two");
        Check.near(2.0, p.getIntrinsicValueAt(4.0), 0, "the last value at the end");
        Check.near(2.0, p.getIntrinsicValueAt(10.0), 0, "and held");
        p.computeValues(8000, 128);
        // 곡선 안의 샘플은 k = floor((N-1)/D (t-T0)) 의 두 값 사이 선형 보간이다.
        for (int i = 0; i < 128; i += 9) {
            double t = (8000 + i) / SR;
            double pos = 3.0 / 3.0 * (t - 1.0);
            int k = (int) Math.floor(pos);
            double[] v = {0, 1, 0, 2};
            Check.near(v[k] + (v[k + 1] - v[k]) * (pos - k), p.valueAt(i), 1e-6, "curve sample " + i);
        }
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                p.setValueCurveAtTime(new float[] {1f}, 6.0, 1.0);
            }
        }, "curve of one value");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.setValueCurveAtTime(new float[] {1f, 2f}, 6.0, 0.0);
            }
        }, "curve with duration 0");
    }

    /**
     * The curve is copied, the automation after a curve starts from its end value, and the implicit event at the end of the curve makes
     * a following ramp start there.
     * <p>
     * 곡선은 복사되고, 곡선 뒤의 자동화는 그 끝 값에서 시작하며, 곡선 끝의 암묵 이벤트 때문에 뒤따르는 램프가 거기서 시작한다.
     */
    public void testCurveCopyAndRampAfterCurve() {
        AudioParam p = gain();
        float[] values = {0f, 4f};
        p.setValueCurveAtTime(values, 0.0, 1.0);
        values[1] = 100f;
        Check.near(4.0, p.getIntrinsicValueAt(1.0), 0, "the curve was copied");
        p.linearRampToValueAtTime(0f, 3.0);
        Check.near(4.0, p.getIntrinsicValueAt(1.0), 1e-9, "the ramp starts at the end of the curve");
        Check.near(2.0, p.getIntrinsicValueAt(2.0), 1e-9, "linear from 4 at 1 to 0 at 3");
    }

    /**
     * An event inside a value curve is a <code>NotSupportedError</code>, but events exactly at its start or its end are allowed, and so
     * is a curve that ends where another starts.
     * <p>
     * 값 곡선 안의 이벤트는 <code>NotSupportedError</code> 이지만 시작이나 끝 시간의 이벤트는 허용되고, 다른 곡선이 시작하는 곳에서 끝나는 곡선도 허용된다.
     */
    public void testCurveOverlapRules() {
        final AudioParam p = gain();
        p.setValueAtTime(1f, 1.0);
        p.setValueCurveAtTime(new float[] {0f, 1f}, 1.0, 2.0);
        p.setValueAtTime(7f, 3.0);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setValueAtTime(5f, 2.0);
            }
        }, "event inside the curve");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.linearRampToValueAtTime(5f, 2.5);
            }
        }, "ramp inside the curve");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setTargetAtTime(5f, 1.0, 1f);
            }
        }, "event at the start of the curve is inside it");
        p.setValueCurveAtTime(new float[] {2f, 3f}, 3.0, 1.0);
        final AudioParam q = gain();
        q.setValueAtTime(1f, 2.0);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                q.setValueCurveAtTime(new float[] {0f, 1f}, 1.0, 2.0);
            }
        }, "a curve that contains an event");
        q.setValueCurveAtTime(new float[] {0f, 1f}, 1.0, 1.0);
        Check.near(1.0, q.getIntrinsicValueAt(1.9), 0.11, "a curve that ends before an event is allowed");
    }

    /**
     * Events at the same time keep the order of insertion, so the last one decides.
     * <p>
     * 같은 시간의 이벤트는 넣은 순서를 유지하므로 마지막 것이 정한다.
     */
    public void testEventsAtTheSameTime() {
        AudioParam p = gain();
        p.setValueAtTime(2f, 1.0);
        p.setValueAtTime(3f, 1.0);
        p.setValueAtTime(4f, 0.5);
        Check.near(4.0, p.getIntrinsicValueAt(0.75), 0, "the earlier event");
        Check.near(3.0, p.getIntrinsicValueAt(1.0), 0, "the later of two events at the same time");
    }

    /**
     * <code>cancelScheduledValues</code> removes the events at or after the time; a ramp that is cancelled restores the value before it.
     * <p>
     * <code>cancelScheduledValues</code> 는 시간 이후(같은 시간 포함)의 이벤트를 지운다. 취소된 램프는 그 앞의 값을 복원한다.
     */
    public void testCancelScheduledValues() {
        AudioParam p = gain();
        p.setValueAtTime(1f, 1.0);
        p.setValueAtTime(2f, 2.0);
        p.setValueAtTime(3f, 3.0);
        p.cancelScheduledValues(2.0);
        Check.near(1.0, p.getIntrinsicValueAt(2.5), 0, "the events at and after 2 are gone");
        Check.near(1.0, p.getIntrinsicValueAt(5.0), 0, "the value stays");
        AudioParam r = gain();
        r.setValueAtTime(0f, 0);
        r.linearRampToValueAtTime(10f, 10.0);
        r.cancelScheduledValues(5.0);
        Check.near(0.0, r.getIntrinsicValueAt(6.0), 0, "the cancelled ramp restores the value before it");
        AudioParam c = gain();
        c.setValueCurveAtTime(new float[] {0f, 1f}, 1.0, 2.0);
        c.cancelScheduledValues(2.0);
        Check.near(1.0, c.getIntrinsicValueAt(2.5), 0, "a curve that contains the time is removed with its end event");
        AudioParam all = gain();
        all.setValueAtTime(9f, 5.0);
        all.cancelScheduledValues(0.0);
        Check.near(1.0, all.getIntrinsicValueAt(6.0), 0, "with no event left the value is the one before");
        Check.equal(0, all.getEventCount(), "no events left");
    }

    /**
     * <code>cancelAndHoldAtTime</code> keeps the value that a ramp, a target and a value curve would have had at the time.
     * <p>
     * <code>cancelAndHoldAtTime</code> 은 램프, 목표, 값 곡선이 그 시간에 가졌을 값을 유지한다.
     */
    public void testCancelAndHold() {
        AudioParam ramp = gain();
        ramp.setValueAtTime(0f, 0);
        ramp.linearRampToValueAtTime(10f, 10.0);
        ramp.cancelAndHoldAtTime(4.0);
        Check.near(2.0, ramp.getIntrinsicValueAt(2.0), 1e-9, "the ramp continues before the time");
        Check.near(4.0, ramp.getIntrinsicValueAt(4.0), 1e-9, "and reaches the held value");
        Check.near(4.0, ramp.getIntrinsicValueAt(8.0), 1e-9, "the value is held afterwards");
        AudioParam target = gain();
        target.setValueAtTime(0f, 0);
        target.setTargetAtTime(1f, 0, 1f);
        target.setValueAtTime(100f, 6.0);
        target.cancelAndHoldAtTime(2.0);
        Check.near(1 - Math.exp(-1.0), target.getIntrinsicValueAt(1.0), 1e-9, "the target approach before the time");
        Check.near(1 - Math.exp(-2.0), target.getIntrinsicValueAt(5.0), 1e-9, "the value of the target at the time is held");
        AudioParam curve = gain();
        curve.setValueCurveAtTime(new float[] {0f, 10f}, 1.0, 4.0);
        curve.cancelAndHoldAtTime(3.0);
        Check.near(2.5, curve.getIntrinsicValueAt(2.0), 1e-9, "the curve is sampled as before: 10 over 4 seconds");
        Check.near(5.0, curve.getIntrinsicValueAt(3.0), 1e-9, "the curve value at the time");
        Check.near(5.0, curve.getIntrinsicValueAt(7.0), 1e-9, "held after the cut");
        AudioParam exp = gain();
        exp.setValueAtTime(1f, 0);
        exp.exponentialRampToValueAtTime(16f, 4.0);
        exp.cancelAndHoldAtTime(2.0);
        Check.near(4.0, exp.getIntrinsicValueAt(3.0), 1e-9, "an exponential ramp is cut at its value");
    }

    /**
     * A ramp after a target that has not started replaces it (it ramps from the value before the target at the start time); after a
     * target that started it ramps from the current value of the target at the current time, so the curve stays continuous.
     * <p>
     * 시작하지 않은 목표 뒤의 램프는 그것을 대체한다(목표 이전의 값에서 시작 시간부터 램프). 시작한 목표 뒤의 램프는 목표의 현재 값에서 현재 시간부터
     * 램프하므로 곡선이 이어진다.
     */
    public void testRampAfterTarget() {
        AudioParam notStarted = gain();
        notStarted.setValueAtTime(0f, 0);
        notStarted.setTargetAtTime(1f, 2.0, 1f);
        notStarted.linearRampToValueAtTime(5f, 4.0);
        Check.near(0.0, notStarted.getIntrinsicValueAt(1.0), 0, "before the replaced target");
        Check.near(2.5, notStarted.getIntrinsicValueAt(3.0), 1e-9, "ramp from (2, 0) to (4, 5)");
        OfflineAudioContext ctx = WebAudioTestKit.mono(8000 * 5);
        AudioParam started = ctx.createGain().getGain();
        started.setValueAtTime(0f, 0);
        started.setTargetAtTime(1f, 0.5, 1f);
        // 컨텍스트 시간을 3 초 가까이 보낸다.
        while (ctx.getCurrentTime() < 3.0) {
            ctx.renderNextQuantum();
        }
        double now = ctx.getCurrentTime();
        double v0 = 1 - Math.exp(-(now - 0.5));
        started.linearRampToValueAtTime(5f, 5.0);
        Check.near(v0, started.getIntrinsicValueAt(now), 1e-6, "continuous at the current time");
        Check.near((v0 + 5.0) / 2, started.getIntrinsicValueAt((now + 5.0) / 2), 1e-6, "linear from the current value to the end value");
        Check.near(5.0, started.getIntrinsicValueAt(5.0), 0, "end value");
    }

    /**
     * The computed value is clamped to the nominal range, but the automation runs as if there was no clamping (the example of
     * section 1.6.3: a ramp to 4 and back to 0 on a parameter of the range [-1, 1]).
     * <p>
     * 계산된 값은 명목 범위로 제한되지만 자동화는 제한이 없는 것처럼 실행된다(1.6.3절의 예: 범위 [-1, 1] 파라미터에서 4 까지 올랐다가 0 으로 내려오는
     * 램프).
     */
    public void testClampingToNominalRange() {
        AudioParam p = WebAudioTestKit.mono(8000 * 3).createStereoPanner().getPan();
        p.setValueAtTime(0f, 0);
        p.linearRampToValueAtTime(4f, 1.0);
        p.linearRampToValueAtTime(0f, 2.0);
        p.computeValues(0, 128);
        Check.near(4.0 * 64 / SR, p.valueAt(64), 1e-6, "inside the range");
        Check.near(1.0, at(p, 2000, 0), 1e-6, "exactly at the limit (t=0.25)");
        Check.near(1.0, at(p, 4000, 0), 0, "clamped at the maximum (the ramp is at 2 at t=0.5)");
        Check.near(1.0, at(p, 8000, 0), 0, "clamped at the top of the ramp (4 at t=1)");
        Check.near(1.0, at(p, 12000, 0), 0, "clamped on the way down (2 at t=1.5)");
        Check.near(1.0, at(p, 14000, 0), 1e-6, "back at the limit (t=1.75)");
        Check.near(0.5, at(p, 15000, 0), 1e-6, "the automation was not clamped, so the way down passes 0.5 at t=1.875");
        Check.near(0.0, at(p, 16000, 0), 1e-6, "the end value");
    }

    /**
     * A k-rate parameter uses the value at the first frame for the whole quantum.
     * <p>
     * k-rate 파라미터는 quantum 전체에 첫 프레임의 값을 쓴다.
     */
    public void testKRate() {
        AudioParam p = gain();
        p.setAutomationRate(AutomationRate.K_RATE);
        p.setValueAtTime(0f, 0);
        p.linearRampToValueAtTime(1f, 1.0);
        p.computeValues(8000, 128);
        Check.near(1.0, p.valueAt(0), 1e-6, "the value of the first frame");
        Check.near(1.0, p.valueAt(127), 1e-6, "the same for the last frame");
        p.computeValues(4000, 128);
        Check.near(0.5, p.valueAt(0), 1e-6, "k-rate: the first frame of the quantum at 4000");
        Check.near(0.5, p.valueAt(100), 1e-6, "k-rate: no change inside the quantum");
        Check.isTrue(p.isConstantBlock(), "a k-rate block is constant");
    }

    /**
     * Setting <code>value</code> assigns the <code>[[current value]]</code> and schedules <code>setValueAtTime</code> at the current
     * time; the exceptions of that method are thrown too.
     * <p>
     * <code>value</code> 를 정하면 <code>[[current value]]</code> 를 대입하고 현재 시간에 <code>setValueAtTime</code> 을 예약한다. 그 메서드의 예외도 던져진다.
     */
    public void testValueSetter() {
        final AudioParam p = gain();
        p.setValue(0.25f);
        Check.near(0.25, p.getValue(), 0, "the getter returns the assigned value at once");
        Check.near(0.25, p.getIntrinsicValueAt(0.0), 0, "an event was scheduled at the current time");
        p.setValueCurveAtTime(new float[] {0f, 1f}, 1.0, 1.0);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setValueAtTime(1f, 1.5);
            }
        }, "event inside a curve");
        Check.near(0.25, p.getValue(), 0, "a failed set keeps the value");
    }

    /**
     * Arguments are checked: a negative time is a <code>RangeError</code>, a value or time that is not finite a <code>TypeError</code>.
     * <p>
     * 인수를 검사한다: 음수 시간은 <code>RangeError</code>, 유한하지 않은 값이나 시간은 <code>TypeError</code>.
     */
    public void testArgumentErrors() {
        final AudioParam p = gain();
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.setValueAtTime(1f, -1.0);
            }
        }, "negative start time");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.linearRampToValueAtTime(1f, -0.5);
            }
        }, "negative end time");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                p.setValueAtTime(1f, Double.POSITIVE_INFINITY);
            }
        }, "infinite time");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                p.setValueAtTime(Float.NaN, 1.0);
            }
        }, "NaN value");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.cancelScheduledValues(-1.0);
            }
        }, "negative cancel time");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.cancelAndHoldAtTime(-1.0);
            }
        }, "negative hold time");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                p.setValueCurveAtTime(new float[] {0f, Float.NaN}, 1.0, 1.0);
            }
        }, "curve with NaN");
    }

    /**
     * A time in the past is clamped to the current time of the context.
     * <p>
     * 과거의 시간은 컨텍스트의 현재 시간으로 맞춰진다.
     */
    public void testTimesAreClampedToTheCurrentTime() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(8000 * 3);
        AudioParam p = ctx.createGain().getGain();
        // 컨텍스트 시간을 1 초 가까이 보낸다.
        while (ctx.getCurrentTime() < 1.0) {
            ctx.renderNextQuantum();
        }
        double now = ctx.getCurrentTime();
        p.setValueAtTime(5f, 0.5);
        Check.near(1.0, p.getIntrinsicValueAt(now - 0.01), 0, "the event is not placed in the past");
        Check.near(5.0, p.getIntrinsicValueAt(now), 0, "it was placed at the current time");
    }

    /**
     * Old events are folded into the base value, so that a parameter that is set again and again keeps a short list.
     * <p>
     * 오래된 이벤트는 기준 값으로 접히므로 계속 다시 설정되는 파라미터는 짧은 목록을 유지한다.
     */
    public void testOldEventsAreFolded() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(8000 * 60);
        AudioParam p = ctx.createGain().getGain();
        // quantum 이 지날 때마다 값을 다시 정한다.
        for (int i = 0; i < 2000; i++) {
            ctx.renderNextQuantum();
            p.setValue(i * 0.001f);
        }
        Check.isTrue(p.getEventCount() <= 3, "the list is short but has " + p.getEventCount() + " events");
        Check.near(1.999, p.getIntrinsicValueAt(ctx.getCurrentTime()), 1e-6, "the last value");
    }

    /**
     * The number of automation events is limited by a named constant.
     * <p>
     * 자동화 이벤트 수는 이름 있는 상수로 제한된다.
     */
    public void testEventLimit() {
        final AudioParam p = gain();
        // 한계까지 미래의 이벤트를 쌓는다.
        for (int i = 0; i < WebAudioLimits.MAX_AUTOMATION_EVENTS - 1; i++) {
            p.setValueAtTime(i, 1.0 + i);
        }
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setValueAtTime(1f, 9999999.0);
            }
        }, "too many events");
    }
}
