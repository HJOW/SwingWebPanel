package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The automation timeline of an {@link AudioParam} (Web Audio API 1.6): the ordered list of automation events and the function that gives
 * the value of the parameter at any time. The value at a time is computed from the exact times of the events (not quantized to the sample
 * rate) with the formulas of the specification: <code>setValueAtTime</code> holds a value, the linear and the exponential ramp interpolate
 * from the previous event, <code>setTargetAtTime</code> approaches a target exponentially, <code>setValueCurveAtTime</code> interpolates an
 * array of values. The cancel operations and the implicit events (the start of a ramp that has no previous event, the end of a curve) follow
 * the algorithms of the specification. All methods are synchronized: the control thread inserts events and the rendering thread evaluates
 * one whole render quantum with a single call, so a quantum never sees a half applied change.
 * <p>
 * {@link AudioParam} 의 자동화 타임라인(Web Audio API 1.6): 순서대로 놓인 자동화 이벤트 목록과 임의의 시간에서 파라미터 값을 주는 함수. 시간에서의 값은
 * 이벤트의 정확한 시간(샘플링 주파수에 맞추지 않음)과 규격의 식으로 계산한다: <code>setValueAtTime</code> 은 값을 유지하고, 선형/지수 램프는 이전
 * 이벤트에서 보간하며, <code>setTargetAtTime</code> 은 목표에 지수적으로 다가가고, <code>setValueCurveAtTime</code> 은 값 배열을 보간한다. 취소 연산과
 * 암묵 이벤트(이전 이벤트가 없는 램프의 시작, 곡선의 끝)는 규격의 알고리즘을 따른다. 모든 메서드는 synchronized 이다: 제어 스레드가 이벤트를 넣고
 * 렌더링 스레드가 render quantum 전체를 한 번의 호출로 계산하므로 quantum 이 반쯤 적용된 변경을 보는 일이 없다.
 */
final class AudioParamTimeline {

    /**
     * Event type: <code>setValueAtTime</code>.
     * <p>
     * 이벤트 종류: <code>setValueAtTime</code>.
     */
    static final int SET_VALUE = 0;

    /**
     * Event type: <code>linearRampToValueAtTime</code>.
     * <p>
     * 이벤트 종류: <code>linearRampToValueAtTime</code>.
     */
    static final int LINEAR_RAMP = 1;

    /**
     * Event type: <code>exponentialRampToValueAtTime</code>.
     * <p>
     * 이벤트 종류: <code>exponentialRampToValueAtTime</code>.
     */
    static final int EXPONENTIAL_RAMP = 2;

    /**
     * Event type: <code>setTargetAtTime</code>.
     * <p>
     * 이벤트 종류: <code>setTargetAtTime</code>.
     */
    static final int SET_TARGET = 3;

    /**
     * Event type: <code>setValueCurveAtTime</code>.
     * <p>
     * 이벤트 종류: <code>setValueCurveAtTime</code>.
     */
    static final int SET_VALUE_CURVE = 4;

    /**
     * One automation event.
     * <p>
     * 자동화 이벤트 하나.
     */
    private static final class Event {

        /**
         * The type, one of the type constants.
         * <p>
         * 종류(종류 상수 중 하나).
         */
        int type;

        /**
         * The event time in seconds (the end time of a ramp).
         * <p>
         * 이벤트 시간(초). 램프에서는 끝 시간.
         */
        double time;

        /**
         * The value, or the target of a <code>setTargetAtTime</code> event.
         * <p>
         * 값, 또는 <code>setTargetAtTime</code> 이벤트의 목표 값.
         */
        double value;

        /**
         * The time constant of a target event, or the duration over which a curve is sampled.
         * <p>
         * 목표 이벤트의 시간 상수, 또는 곡선을 표본화하는 지속 시간.
         */
        double aux;

        /**
         * The duration after which a curve event ends (shorter than {@link #aux} when <code>cancelAndHoldAtTime</code> cut the curve).
         * <p>
         * 곡선 이벤트가 끝나는 지속 시간(<code>cancelAndHoldAtTime</code> 이 곡선을 자르면 {@link #aux} 보다 짧다).
         */
        double duration;

        /**
         * The values of a curve event.
         * <p>
         * 곡선 이벤트의 값.
         */
        float[] curve;

        /**
         * The value of the timeline just before this event (the left limit); valid while the timeline is prepared.
         * <p>
         * 이 이벤트 바로 앞에서의 타임라인 값(왼쪽 극한). 타임라인이 준비된 동안 유효하다.
         */
        double left;

        /**
         * Creates an event.
         * <p>
         * 이벤트를 만든다.
         *
         * @param type  the type / 종류
         * @param time  the time / 시간
         * @param value the value / 값
         */
        Event(int type, double time, double value) {
            this.type = type;
            this.time = time;
            this.value = value;
        }
    }

    /**
     * The events in ascending time order; events at the same time are in the order of insertion.
     * <p>
     * 시간 오름차순의 이벤트. 시간이 같은 이벤트는 넣은 순서대로 놓인다.
     */
    private final ArrayList<Event> events = new ArrayList<Event>();

    /**
     * The value before the first event.
     * <p>
     * 첫 이벤트 앞에서의 값.
     */
    private double base;

    /**
     * True while the {@link Event#left} values are valid.
     * <p>
     * {@link Event#left} 값이 유효한 동안 true.
     */
    private boolean prepared;

    /**
     * Creates an empty timeline.
     * <p>
     * 빈 타임라인을 만든다.
     *
     * @param defaultValue the value of the parameter before any event / 이벤트가 없을 때의 파라미터 값
     */
    AudioParamTimeline(double defaultValue) {
        this.base = defaultValue;
    }

    /**
     * Returns the number of events (including the implicit ones).
     * <p>
     * 이벤트 수(암묵 이벤트 포함)를 반환한다.
     *
     * @return the number of events / 이벤트 수
     */
    synchronized int size() {
        return events.size();
    }

    /**
     * Throws a <code>NotSupportedError</code> if the time lies inside a value curve (the interval from its start, inclusive, to its end,
     * exclusive).
     * <p>
     * 시간이 값 곡선 안(시작은 포함, 끝은 미포함)이면 <code>NotSupportedError</code> 를 던진다.
     *
     * @param time the time of the new event / 새 이벤트의 시간
     */
    private void checkNotInsideCurve(double time) {
        // 값 곡선이 지속되는 동안에는 다른 이벤트를 넣을 수 없다.
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            if (e.type == SET_VALUE_CURVE && time >= e.time && time < e.time + e.duration) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the time " + time + " lies inside a value curve");
            }
        }
    }

    /**
     * Throws a <code>NotSupportedError</code> if there is no room for another event.
     * <p>
     * 이벤트를 더 넣을 자리가 없으면 <code>NotSupportedError</code> 를 던진다.
     */
    private void checkRoom() {
        if (events.size() + 2 > WebAudioLimits.MAX_AUTOMATION_EVENTS) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "more than " + WebAudioLimits.MAX_AUTOMATION_EVENTS + " automation events");
        }
    }

    /**
     * Returns the index at which an event of the time is inserted: after all events with a time less than or equal to it.
     * <p>
     * 시간이 주어진 이벤트를 넣을 인덱스를 반환한다: 시간이 같거나 작은 모든 이벤트의 뒤.
     *
     * @param time the time / 시간
     * @return the insertion index / 삽입 인덱스
     */
    private int upperBound(double time) {
        int lo = 0;
        int hi = events.size();
        // 이분 탐색으로 time 보다 큰 첫 이벤트의 인덱스를 찾는다.
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (events.get(mid).time <= time) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /**
     * Adds a <code>setValueAtTime</code> event.
     * <p>
     * <code>setValueAtTime</code> 이벤트를 넣는다.
     *
     * @param value the value / 값
     * @param time  the time, already clamped to the current time / 현재 시간 이상으로 맞춘 시간
     * @param now   the current time of the context / 컨텍스트의 현재 시간
     */
    synchronized void insertSetValue(double value, double time, double now) {
        checkNotInsideCurve(time);
        checkRoom();
        events.add(upperBound(time), new Event(SET_VALUE, time, value));
        prepared = false;
        prune(now);
    }

    /**
     * Adds a linear or exponential ramp event. If no event precedes it the ramp starts from the current value at the current time, as
     * if <code>setValueAtTime</code> had been called; if a <code>setTargetAtTime</code> event precedes it the target event is replaced
     * (not started) or cut at the current time (started) so that the curve stays continuous.
     * <p>
     * 선형 또는 지수 램프 이벤트를 넣는다. 앞선 이벤트가 없으면 <code>setValueAtTime</code> 을 부른 것처럼 현재 시간의 현재 값에서 시작하고, 앞선 이벤트가
     * <code>setTargetAtTime</code> 이면 곡선이 이어지도록 목표 이벤트를 바꾸거나(시작 전) 현재 시간에서 자른다(시작 후).
     *
     * @param type     {@link #LINEAR_RAMP} or {@link #EXPONENTIAL_RAMP} / {@link #LINEAR_RAMP} 또는 {@link #EXPONENTIAL_RAMP}
     * @param value    the end value / 끝 값
     * @param time     the end time, already clamped to the current time / 현재 시간 이상으로 맞춘 끝 시간
     * @param now      the current time of the context / 컨텍스트의 현재 시간
     * @param nowValue the current value of the parameter / 파라미터의 현재 값
     */
    synchronized void insertRamp(int type, double value, double time, double now, double nowValue) {
        checkNotInsideCurve(time);
        checkRoom();
        prepare();
        int pos = upperBound(time);
        if (pos == 0) {
            // 앞선 이벤트가 없으면 현재 시간과 현재 값으로 setValueAtTime 을 부른 것처럼 동작한다.
            events.add(0, new Event(SET_VALUE, now, nowValue));
            pos = 1;
            prepared = false;
        } else {
            Event prev = events.get(pos - 1);
            if (prev.type == SET_TARGET) {
                if (prev.time > now) {
                    // 목표 이벤트가 아직 시작하지 않았으면 시작 직전의 값을 유지하는 setValue 로 바꾼다.
                    prev.type = SET_VALUE;
                    prev.value = prev.left;
                } else {
                    // 이미 시작했으면 현재 시간의 목표 곡선 값에서 램프가 이어지도록 setValue 를 끼워 넣는다.
                    events.add(pos, new Event(SET_VALUE, now, valueAt(pos - 1, now, pos)));
                    pos++;
                }
                prepared = false;
            }
        }
        events.add(pos, new Event(type, time, value));
        prepared = false;
        prune(now);
    }

    /**
     * Adds a <code>setTargetAtTime</code> event.
     * <p>
     * <code>setTargetAtTime</code> 이벤트를 넣는다.
     *
     * @param target       the target value / 목표 값
     * @param time         the start time, already clamped to the current time / 현재 시간 이상으로 맞춘 시작 시간
     * @param timeConstant the time constant, not negative / 시간 상수(음수가 아님)
     * @param now          the current time of the context / 컨텍스트의 현재 시간
     */
    synchronized void insertSetTarget(double target, double time, double timeConstant, double now) {
        checkNotInsideCurve(time);
        checkRoom();
        Event e = new Event(SET_TARGET, time, target);
        e.aux = timeConstant;
        events.add(upperBound(time), e);
        prepared = false;
        prune(now);
    }

    /**
     * Adds a <code>setValueCurveAtTime</code> event and the implicit <code>setValueAtTime</code> event at its end with the last value,
     * so that the automation that follows starts from the end of the curve.
     * <p>
     * <code>setValueCurveAtTime</code> 이벤트와, 뒤따르는 자동화가 곡선의 끝에서 시작하도록 그 끝에 마지막 값으로 암묵 <code>setValueAtTime</code>
     * 이벤트를 넣는다.
     *
     * @param values   the values of the curve (at least 2), copied / 곡선의 값(2개 이상, 복사한다)
     * @param time     the start time, already clamped to the current time / 현재 시간 이상으로 맞춘 시작 시간
     * @param duration the duration, positive / 지속 시간(양수)
     * @param now      the current time of the context / 컨텍스트의 현재 시간
     */
    synchronized void insertCurve(float[] values, double time, double duration, double now) {
        checkNotInsideCurve(time);
        checkRoom();
        // 곡선이 지속되는 동안 시작하는 다른 이벤트가 있으면 안 된다(곡선과 같은 시간이나 끝 시간은 허용).
        for (int i = 0; i < events.size(); i++) {
            double t = events.get(i).time;
            if (t > time && t < time + duration) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "an event lies inside the value curve");
            }
        }
        Event e = new Event(SET_VALUE_CURVE, time, values[0]);
        e.curve = values.clone();
        e.aux = duration;
        e.duration = duration;
        int pos = upperBound(time);
        events.add(pos, e);
        Event end = new Event(SET_VALUE, time + duration, values[values.length - 1]);
        events.add(upperBound(time + duration), end);
        prepared = false;
        prune(now);
    }

    /**
     * Cancels all events with a time at or after the cancel time, and a value curve that contains the cancel time.
     * <p>
     * 취소 시간 이후(같은 시간 포함)의 모든 이벤트와 취소 시간을 포함하는 값 곡선을 취소한다.
     *
     * @param cancelTime the cancel time, already clamped to the current time / 현재 시간 이상으로 맞춘 취소 시간
     */
    synchronized void cancelScheduledValues(double cancelTime) {
        // 뒤에서부터 지워 인덱스가 밀리지 않게 한다.
        for (int i = events.size() - 1; i >= 0; i--) {
            Event e = events.get(i);
            boolean inside = e.type == SET_VALUE_CURVE && cancelTime >= e.time && cancelTime <= e.time + e.duration;
            if (e.time >= cancelTime || inside) {
                events.remove(i);
            }
        }
        prepared = false;
    }

    /**
     * Implements <code>cancelAndHoldAtTime</code>: cancels the events after the cancel time but keeps the value that the automation would
     * have had at that time: a ramp that crosses the time is cut there, a target event is replaced by the value it had, a value curve is
     * cut but sampled as before.
     * <p>
     * <code>cancelAndHoldAtTime</code> 을 구현한다: 취소 시간 이후의 이벤트를 취소하되 그 시간에 자동화가 가졌을 값을 유지한다: 그 시간을 지나는 램프는
     * 거기서 자르고, 목표 이벤트는 그때의 값으로 바꾸며, 값 곡선은 자르되 이전과 같이 표본화한다.
     *
     * @param cancelTime the cancel time, already clamped to the current time / 현재 시간 이상으로 맞춘 취소 시간
     */
    synchronized void cancelAndHold(double cancelTime) {
        prepare();
        int n = events.size();
        int i1 = upperBound(cancelTime) - 1;
        int i2 = i1 + 1;
        boolean rampCut = false;
        if (i2 < n) {
            Event e2 = events.get(i2);
            if (e2.type == LINEAR_RAMP || e2.type == EXPONENTIAL_RAMP) {
                // 취소 시간을 지나는 램프는 그 시간에 끝나도록 다시 쓴다(끝 값은 원래 램프의 그 시간 값).
                double v = valueAt(i1, cancelTime, n);
                e2.time = cancelTime;
                e2.value = v;
                rampCut = true;
            }
        }
        if (!rampCut && i1 >= 0) {
            Event e1 = events.get(i1);
            if (e1.type == SET_TARGET) {
                // 목표 이벤트는 취소 시간의 목표 곡선 값을 유지하는 setValue 로 이어 준다.
                events.add(i1 + 1, new Event(SET_VALUE, cancelTime, valueAt(i1, cancelTime, n)));
            } else if (e1.type == SET_VALUE_CURVE && cancelTime <= e1.time + e1.duration) {
                // 곡선은 취소 시간에서 자르되 표본화 길이는 그대로 두어 같은 값이 나오게 한다.
                double held = valueAt(i1, cancelTime, n);
                e1.duration = cancelTime - e1.time;
                events.add(i1 + 1, new Event(SET_VALUE, cancelTime, held));
            }
        }
        // 취소 시간보다 뒤의 이벤트를 모두 지운다.
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).time > cancelTime) {
                events.remove(i);
            }
        }
        prepared = false;
    }

    /**
     * Computes the values of the whole render quantum. A k-rate evaluation computes only the value at the first frame.
     * <p>
     * render quantum 전체의 값을 계산한다. k-rate 계산은 첫 프레임의 값만 계산한다.
     *
     * @param startFrame the index of the first frame / 첫 프레임의 인덱스
     * @param frames     the number of frames / 프레임 수
     * @param sampleRate the sample rate / 샘플링 주파수
     * @param aRate      true to compute every frame, false to compute only the first / 모든 프레임을 계산하면 true, 첫 프레임만이면 false
     * @param out        receives the values (<code>out[0]</code> only for k-rate) / 값을 받는다(k-rate 는 <code>out[0]</code> 만)
     * @return true if the computed values are not all equal / 계산한 값이 모두 같지 않으면 true
     */
    synchronized boolean evaluate(long startFrame, int frames, double sampleRate, boolean aRate, float[] out) {
        int n = events.size();
        if (n == 0) {
            float b = (float) base;
            if (aRate) {
                Arrays.fill(out, 0, frames, b);
            } else {
                out[0] = b;
            }
            return false;
        }
        prepare();
        double t = startFrame / sampleRate;
        int i = upperBound(t) - 1;
        if (!aRate) {
            out[0] = (float) valueAt(i, t, n);
            return false;
        }
        boolean varies = false;
        // 프레임마다 그 시간의 값을 계산한다. 이벤트 커서는 앞으로만 나아간다.
        for (int j = 0; j < frames; j++) {
            t = (startFrame + j) / sampleRate;
            while (i + 1 < n && events.get(i + 1).time <= t) {
                i++;
            }
            out[j] = (float) valueAt(i, t, n);
            if (out[j] != out[0]) {
                varies = true;
            }
        }
        return varies;
    }

    /**
     * Returns the value of the timeline at a time.
     * <p>
     * 시간에서의 타임라인 값을 반환한다.
     *
     * @param t the time in seconds / 시간(초)
     * @return the value / 값
     */
    synchronized double valueAtTime(double t) {
        prepare();
        return valueAt(upperBound(t) - 1, t, events.size());
    }

    /**
     * Computes the {@link Event#left} values of all events if they are not valid.
     * <p>
     * 유효하지 않으면 모든 이벤트의 {@link Event#left} 값을 계산한다.
     */
    private void prepare() {
        if (prepared) {
            return;
        }
        // 이벤트 순서대로 바로 앞의 값을 구한다. 이전 이벤트의 값만 있으면 되므로 재귀가 없다.
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            e.left = i == 0 ? base : valueAt(i - 1, e.time, i);
        }
        prepared = true;
    }

    /**
     * Folds events that lie completely in the past into the base value, so that a parameter that is set again and again keeps a short
     * list. The first event is dropped when the second one starts before the time, is not a ramp (a ramp needs the previous event as
     * its start) and so makes the first one irrelevant.
     * <p>
     * 완전히 과거에 있는 이벤트를 기준 값으로 접어, 계속 다시 설정되는 파라미터가 짧은 목록을 유지하게 한다. 둘째 이벤트가 그 시간 전에 시작하고 램프가
     * 아니면(램프는 이전 이벤트를 시작점으로 필요로 한다) 첫 이벤트는 쓸모가 없어 버린다.
     *
     * @param now the current time of the context / 컨텍스트의 현재 시간
     */
    private void prune(double now) {
        // 첫 이벤트가 완전히 덮인 동안 앞에서부터 접는다.
        while (events.size() >= 2) {
            Event second = events.get(1);
            if (second.type == LINEAR_RAMP || second.type == EXPONENTIAL_RAMP || !(second.time < now)) {
                break;
            }
            prepare();
            base = second.left;
            events.remove(0);
            prepared = false;
        }
    }

    /**
     * Computes the value at a time from the events before the limit.
     * <p>
     * 한계 앞의 이벤트로 시간에서의 값을 계산한다.
     *
     * @param i     the index of the last event at or before the time, or -1 / 시간 이전(같은 시간 포함)의 마지막 이벤트 인덱스. 없으면 -1
     * @param t     the time / 시간
     * @param limit only the events with an index below the limit exist / 이 인덱스 미만의 이벤트만 있는 것으로 본다
     * @return the value / 값
     */
    private double valueAt(int i, double t, int limit) {
        if (i < 0) {
            return base;
        }
        Event e = events.get(i);
        Event next = i + 1 < limit ? events.get(i + 1) : null;
        if (next != null && (next.type == LINEAR_RAMP || next.type == EXPONENTIAL_RAMP)) {
            // 다음 이벤트가 램프이면 이 이벤트의 끝 값에서 램프의 끝 값까지 보간한다.
            double t0 = e.time;
            double v0;
            if (e.type == SET_TARGET) {
                v0 = e.left;
            } else if (e.type == SET_VALUE_CURVE) {
                t0 = e.time + e.duration;
                v0 = e.curve[e.curve.length - 1];
            } else {
                v0 = e.value;
            }
            double t1 = next.time;
            double v1 = next.value;
            double f = (t - t0) / (t1 - t0);
            if (next.type == LINEAR_RAMP) {
                return v0 + (v1 - v0) * f;
            }
            if (v0 == 0 || (v0 < 0) != (v1 < 0)) {
                return v0;
            }
            return v0 * Math.pow(v1 / v0, f);
        }
        if (e.type == SET_TARGET) {
            if (e.aux == 0) {
                return e.value;
            }
            return e.value + (e.left - e.value) * Math.exp(-(t - e.time) / e.aux);
        }
        if (e.type == SET_VALUE_CURVE) {
            int n = e.curve.length;
            double pos = (n - 1) * (t - e.time) / e.aux;
            if (!(pos > 0)) {
                return e.curve[0];
            }
            int k = (int) Math.floor(pos);
            if (k >= n - 1) {
                return e.curve[n - 1];
            }
            return e.curve[k] + (e.curve[k + 1] - e.curve[k]) * (pos - k);
        }
        return e.value;
    }
}
