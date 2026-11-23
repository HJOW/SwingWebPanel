package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;

/**
 * A parameter of an audio node (Web Audio API 1.6): a value that can be set at once, scheduled with automation events, and modulated by
 * the output of other nodes. The control thread calls the methods of this class; they validate the arguments, clamp the times to the
 * current time of the context and put the event on the {@link AudioParamTimeline}. The rendering thread calls
 * {@link #computeValues(long, int)} for every render quantum: it evaluates the timeline, adds the sum of the connected node outputs (mixed
 * down to mono), replaces a NaN with the default value and clamps the result to the nominal range.
 * <p>
 * 오디오 노드의 파라미터(Web Audio API 1.6): 한 번에 설정하거나, 자동화 이벤트로 예약하거나, 다른 노드의 출력으로 변조할 수 있는 값. 제어 스레드가 이
 * 클래스의 메서드를 부르면 인수를 검증하고, 시간을 컨텍스트의 현재 시간 이상으로 맞추고, 이벤트를 {@link AudioParamTimeline} 에 넣는다. 렌더링 스레드는
 * render quantum 마다 {@link #computeValues(long, int)} 를 부른다: 타임라인을 계산하고, 연결된 노드 출력의 합(모노로 다운믹스)을 더하고, NaN 을 기본 값으로
 * 바꾸고, 결과를 명목 범위로 제한한다.
 */
public final class AudioParam {

    /**
     * The largest positive float, the nominal bound of a parameter that has no practical limit (about 3.4028235e38).
     * <p>
     * 실질적인 제한이 없는 파라미터의 명목 한계인 가장 큰 양의 float(약 3.4028235e38).
     */
    public static final float MAX_FLOAT = Float.MAX_VALUE;

    /**
     * The context that owns the parameter.
     * <p>
     * 파라미터를 가진 컨텍스트.
     */
    private final BaseAudioContext context;

    /**
     * The node that owns the parameter, or null for a parameter of the listener.
     * <p>
     * 파라미터를 가진 노드. 리스너의 파라미터이면 null.
     */
    private final AudioNode owner;

    /**
     * The name of the parameter, for messages.
     * <p>
     * 메시지에 쓰는 파라미터 이름.
     */
    private final String name;

    /**
     * The initial value.
     * <p>
     * 초기 값.
     */
    private final float defaultValue;

    /**
     * The nominal minimum.
     * <p>
     * 명목 최솟값.
     */
    private final float minValue;

    /**
     * The nominal maximum.
     * <p>
     * 명목 최댓값.
     */
    private final float maxValue;

    /**
     * The automation rate; read by the rendering thread at every quantum.
     * <p>
     * 자동화 속도. 렌더링 스레드가 quantum 마다 읽는다.
     */
    private volatile AutomationRate rate;

    /**
     * True if the rate must stay <code>k-rate</code> (an <code>InvalidStateError</code> is thrown when it is set to a-rate).
     * <p>
     * 속도가 <code>k-rate</code> 로 유지되어야 하면 true(a-rate 로 정하면 <code>InvalidStateError</code>).
     */
    private final boolean rateLocked;

    /**
     * The automation timeline.
     * <p>
     * 자동화 타임라인.
     */
    private final AudioParamTimeline timeline;

    /**
     * The <code>[[current value]]</code> slot: the intrinsic value at the beginning of the last rendered quantum, or the value that was
     * assigned last.
     * <p>
     * <code>[[current value]]</code> 슬롯: 마지막으로 렌더링한 quantum 시작에서의 고유 값, 또는 마지막으로 대입한 값.
     */
    private volatile float currentValue;

    /**
     * The computed values of the last quantum (only the first element is meaningful when the block is constant).
     * <p>
     * 마지막 quantum 의 계산된 값(블록이 상수이면 첫 요소만 의미가 있다).
     */
    private final float[] values;

    /**
     * True if every value of the block equals <code>values[0]</code>.
     * <p>
     * 블록의 모든 값이 <code>values[0]</code> 와 같으면 true.
     */
    private boolean constantBlock = true;

    /**
     * The scratch values of the timeline (before the audio inputs are added).
     * <p>
     * 타임라인의 임시 값(오디오 입력을 더하기 전).
     */
    private final float[] intrinsic;

    /**
     * The outputs of other nodes that are connected to this parameter (rendering thread only).
     * <p>
     * 이 파라미터에 연결된 다른 노드의 출력(렌더링 스레드 전용).
     */
    final ArrayList<AudioNodeOutput> sources = new ArrayList<AudioNodeOutput>();

    /**
     * The mono mix of the connected outputs.
     * <p>
     * 연결된 출력의 모노 믹스.
     */
    private final AudioBus mono;

    /**
     * Creates a parameter.
     * <p>
     * 파라미터를 만든다.
     *
     * @param context      the context / 컨텍스트
     * @param owner        the owning node, or null / 가진 노드. 없으면 null
     * @param name         the name / 이름
     * @param defaultValue the default value / 기본 값
     * @param minValue     the nominal minimum / 명목 최솟값
     * @param maxValue     the nominal maximum / 명목 최댓값
     * @param rate         the initial automation rate / 초기 자동화 속도
     * @param rateLocked   true if the rate must stay k-rate / 속도가 k-rate 로 유지되어야 하면 true
     */
    AudioParam(BaseAudioContext context, AudioNode owner, String name, float defaultValue, float minValue, float maxValue, AutomationRate rate, boolean rateLocked) {
        this.context = context;
        this.owner = owner;
        this.name = name;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.rate = rate;
        this.rateLocked = rateLocked;
        this.timeline = new AudioParamTimeline(defaultValue);
        this.currentValue = defaultValue;
        int q = context.getRenderQuantumSize();
        this.values = new float[q];
        this.intrinsic = new float[q];
        this.mono = new AudioBus(1, q);
        java.util.Arrays.fill(values, defaultValue);
    }

    /**
     * Returns the node that owns this parameter.
     * <p>
     * 이 파라미터를 가진 노드를 반환한다.
     *
     * @return the node, or null for a listener parameter / 노드. 리스너 파라미터이면 null
     */
    public AudioNode getOwner() {
        return owner;
    }

    /**
     * Returns the name of the parameter.
     * <p>
     * 파라미터 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the context of the parameter.
     * <p>
     * 파라미터의 컨텍스트를 반환한다.
     *
     * @return the context / 컨텍스트
     */
    public BaseAudioContext getContext() {
        return context;
    }

    /**
     * Returns the <code>[[current value]]</code>: the value that was assigned last, or the intrinsic value at the beginning of the last
     * rendered quantum.
     * <p>
     * <code>[[current value]]</code> 를 반환한다: 마지막으로 대입한 값, 또는 마지막으로 렌더링한 quantum 시작에서의 고유 값.
     *
     * @return the value / 값
     */
    public float getValue() {
        return currentValue;
    }

    /**
     * Assigns the value: it becomes the <code>[[current value]]</code> and <code>setValueAtTime</code> is called with the current time of
     * the context, so the exceptions of that method are thrown here too.
     * <p>
     * 값을 대입한다: <code>[[current value]]</code> 가 되고 컨텍스트의 현재 시간으로 <code>setValueAtTime</code> 을 부르므로 그 메서드의 예외도 여기서
     * 던져진다.
     *
     * @param value the value / 값
     */
    public void setValue(float value) {
        setValueAtTime(value, context.getCurrentTime());
        currentValue = value;
    }

    /**
     * Returns the initial value.
     * <p>
     * 초기 값을 반환한다.
     *
     * @return the default value / 기본 값
     */
    public float getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the nominal minimum.
     * <p>
     * 명목 최솟값을 반환한다.
     *
     * @return the minimum / 최솟값
     */
    public float getMinValue() {
        return minValue;
    }

    /**
     * Returns the nominal maximum.
     * <p>
     * 명목 최댓값을 반환한다.
     *
     * @return the maximum / 최댓값
     */
    public float getMaxValue() {
        return maxValue;
    }

    /**
     * Returns the automation rate.
     * <p>
     * 자동화 속도를 반환한다.
     *
     * @return the rate / 속도
     */
    public AutomationRate getAutomationRate() {
        return rate;
    }

    /**
     * Sets the automation rate.
     * <p>
     * 자동화 속도를 정한다.
     *
     * @param newRate the rate / 속도
     * @throws AudioException <code>InvalidStateError</code> if the rate is fixed to k-rate and a-rate is requested / 속도가 k-rate 로 고정인데 a-rate 를
     *                        요청하면 <code>InvalidStateError</code>
     */
    public void setAutomationRate(AutomationRate newRate) {
        if (newRate == null) {
            throw new AudioException(AudioException.Kind.TYPE, "automationRate is null");
        }
        if (rateLocked && newRate == AutomationRate.A_RATE) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the automation rate of " + name + " must be k-rate");
        }
        rate = newRate;
    }

    /**
     * Throws a <code>TypeError</code> if a value is not finite.
     * <p>
     * 값이 유한하지 않으면 <code>TypeError</code> 를 던진다.
     *
     * @param value the value / 값
     * @param what  the name of the argument / 인수 이름
     */
    private static void checkFinite(double value, String what) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new AudioException(AudioException.Kind.TYPE, what + " is not a finite number");
        }
    }

    /**
     * Validates a time and clamps it to the current time of the context.
     * <p>
     * 시간을 검증하고 컨텍스트의 현재 시간 이상으로 맞춘다.
     *
     * @param time the time argument / 시간 인수
     * @param what the name of the argument / 인수 이름
     * @return the time, not before the current time / 현재 시간보다 앞서지 않은 시간
     */
    private double clampTime(double time, String what) {
        checkFinite(time, what);
        if (time < 0) {
            throw new AudioException(AudioException.Kind.RANGE, what + " " + time + " is negative");
        }
        return Math.max(time, context.getCurrentTime());
    }

    /**
     * Schedules a change of the value at a time.
     * <p>
     * 시간에 값을 바꾸도록 예약한다.
     *
     * @param value     the value / 값
     * @param startTime the time in seconds / 시간(초)
     * @return this parameter / 이 파라미터
     * @throws AudioException <code>RangeError</code> for a negative time, <code>NotSupportedError</code> inside a value curve / 시간이 음수이면
     *                        <code>RangeError</code>, 값 곡선 안이면 <code>NotSupportedError</code>
     */
    public AudioParam setValueAtTime(float value, double startTime) {
        checkFinite(value, "value");
        double t = clampTime(startTime, "startTime");
        timeline.insertSetValue(value, t, context.getCurrentTime());
        return this;
    }

    /**
     * Schedules a linear ramp from the previous event to the value at the end time.
     * <p>
     * 이전 이벤트에서 끝 시간의 값까지 선형 램프를 예약한다.
     *
     * @param value   the end value / 끝 값
     * @param endTime the end time in seconds / 끝 시간(초)
     * @return this parameter / 이 파라미터
     */
    public AudioParam linearRampToValueAtTime(float value, double endTime) {
        checkFinite(value, "value");
        double t = clampTime(endTime, "endTime");
        timeline.insertRamp(AudioParamTimeline.LINEAR_RAMP, value, t, context.getCurrentTime(), currentValue);
        return this;
    }

    /**
     * Schedules an exponential ramp from the previous event to the value at the end time.
     * <p>
     * 이전 이벤트에서 끝 시간의 값까지 지수 램프를 예약한다.
     *
     * @param value   the end value, not 0 / 끝 값(0 이 아님)
     * @param endTime the end time in seconds / 끝 시간(초)
     * @return this parameter / 이 파라미터
     * @throws AudioException <code>RangeError</code> if the value is 0 / 값이 0 이면 <code>RangeError</code>
     */
    public AudioParam exponentialRampToValueAtTime(float value, double endTime) {
        checkFinite(value, "value");
        if (value == 0f) {
            throw new AudioException(AudioException.Kind.RANGE, "the value of an exponential ramp must not be 0");
        }
        double t = clampTime(endTime, "endTime");
        timeline.insertRamp(AudioParamTimeline.EXPONENTIAL_RAMP, value, t, context.getCurrentTime(), currentValue);
        return this;
    }

    /**
     * Schedules an exponential approach to a target value that starts at a time.
     * <p>
     * 시간에 시작하는 목표 값으로의 지수적 접근을 예약한다.
     *
     * @param target       the target value / 목표 값
     * @param startTime    the start time in seconds / 시작 시간(초)
     * @param timeConstant the time constant in seconds, not negative; 0 jumps to the target / 시간 상수(초, 음수가 아님). 0 이면 목표로 바로 간다
     * @return this parameter / 이 파라미터
     * @throws AudioException <code>RangeError</code> if the time constant is negative / 시간 상수가 음수이면 <code>RangeError</code>
     */
    public AudioParam setTargetAtTime(float target, double startTime, float timeConstant) {
        checkFinite(target, "target");
        checkFinite(timeConstant, "timeConstant");
        if (timeConstant < 0) {
            throw new AudioException(AudioException.Kind.RANGE, "timeConstant " + timeConstant + " is negative");
        }
        double t = clampTime(startTime, "startTime");
        timeline.insertSetTarget(target, t, timeConstant, context.getCurrentTime());
        return this;
    }

    /**
     * Schedules an array of values that are interpolated over a duration.
     * <p>
     * 지속 시간에 걸쳐 보간되는 값 배열을 예약한다.
     *
     * @param values    the values (at least 2), copied / 값(2개 이상, 복사한다)
     * @param startTime the start time in seconds / 시작 시간(초)
     * @param duration  the duration in seconds, positive / 지속 시간(초, 양수)
     * @return this parameter / 이 파라미터
     * @throws AudioException <code>InvalidStateError</code> for fewer than 2 values, <code>RangeError</code> for a duration that is not positive,
     *                        <code>NotSupportedError</code> if other events lie inside the curve / 값이 2개 미만이면 <code>InvalidStateError</code>, 지속 시간이
     *                        양수가 아니면 <code>RangeError</code>, 곡선 안에 다른 이벤트가 있으면 <code>NotSupportedError</code>
     */
    public AudioParam setValueCurveAtTime(float[] values, double startTime, double duration) {
        if (values == null) {
            throw new AudioException(AudioException.Kind.TYPE, "values is null");
        }
        if (values.length < 2) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "a value curve needs at least 2 values");
        }
        // 곡선의 값이 모두 유한한지 확인한다.
        for (int i = 0; i < values.length; i++) {
            checkFinite(values[i], "values[" + i + "]");
        }
        double t = clampTime(startTime, "startTime");
        checkFinite(duration, "duration");
        if (!(duration > 0)) {
            throw new AudioException(AudioException.Kind.RANGE, "duration " + duration + " is not positive");
        }
        timeline.insertCurve(values, t, duration, context.getCurrentTime());
        return this;
    }

    /**
     * Cancels the scheduled changes at or after a time.
     * <p>
     * 시간 이후(같은 시간 포함)에 예약된 변경을 취소한다.
     *
     * @param cancelTime the time in seconds / 시간(초)
     * @return this parameter / 이 파라미터
     */
    public AudioParam cancelScheduledValues(double cancelTime) {
        double t = clampTime(cancelTime, "cancelTime");
        timeline.cancelScheduledValues(t);
        return this;
    }

    /**
     * Cancels the scheduled changes after a time and holds the value that the automation would have had at that time.
     * <p>
     * 시간 이후에 예약된 변경을 취소하고 그 시간에 자동화가 가졌을 값을 유지한다.
     *
     * @param cancelTime the time in seconds / 시간(초)
     * @return this parameter / 이 파라미터
     */
    public AudioParam cancelAndHoldAtTime(double cancelTime) {
        double t = clampTime(cancelTime, "cancelTime");
        timeline.cancelAndHold(t);
        return this;
    }

    /**
     * Returns the value of the automation timeline at a time (without the audio inputs and without clamping); for tests and for the
     * script layer that shows the automation.
     * <p>
     * 시간에서의 자동화 타임라인 값을 반환한다(오디오 입력과 범위 제한 없이). 시험과 자동화를 보여 주는 스크립트 계층용.
     *
     * @param time the time in seconds / 시간(초)
     * @return the intrinsic value / 고유 값
     */
    public double getIntrinsicValueAt(double time) {
        return timeline.valueAtTime(time);
    }

    /**
     * Returns the number of automation events that the timeline keeps (for tests).
     * <p>
     * 타임라인이 가진 자동화 이벤트 수를 반환한다(시험용).
     *
     * @return the number of events / 이벤트 수
     */
    public int getEventCount() {
        return timeline.size();
    }

    /**
     * Tells whether a value curve (<code>setValueCurveAtTime</code>) was set on this parameter and is still in its timeline: the
     * deprecated <code>setPosition</code> and <code>setOrientation</code> methods of the spatial nodes refuse to run then.
     * <p>
     * 이 파라미터에 값 곡선(<code>setValueCurveAtTime</code>)을 설정했고 타임라인에 아직 남아 있는지 알려 준다. 공간 노드의 폐기 예정 <code>setPosition</code>,
     * <code>setOrientation</code> 메서드는 그러면 실행을 거부한다.
     *
     * @return true if there is a value curve / 값 곡선이 있으면 true
     */
    boolean hasValueCurve() {
        return timeline.hasValueCurve();
    }

    /**
     * Computes the values of one render quantum (rendering thread). The <code>[[current value]]</code> becomes the intrinsic value at the
     * first frame; then the mono sum of the active connected outputs is added, a NaN is replaced by the default value and the result is
     * clamped to the nominal range.
     * <p>
     * render quantum 하나의 값을 계산한다(렌더링 스레드). <code>[[current value]]</code> 는 첫 프레임의 고유 값이 되고, 활성인 연결 출력의 모노 합을 더하고,
     * NaN 은 기본 값으로 바꾸고, 결과를 명목 범위로 제한한다.
     *
     * @param startFrame the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames     the number of frames / 프레임 수
     */
    void computeValues(long startFrame, int frames) {
        boolean aRate = rate == AutomationRate.A_RATE;
        boolean varies = timeline.evaluate(startFrame, frames, context.getSampleRate(), aRate, intrinsic);
        currentValue = intrinsic[0];
        boolean hasInput = false;
        if (!sources.isEmpty()) {
            mono.zero();
            long serial = context.getRenderSerial();
            // 연결된 출력 중 이번 quantum 에 활성인 것만 모노로 다운믹스해 더한다.
            for (int i = 0; i < sources.size(); i++) {
                AudioNodeOutput src = sources.get(i);
                if (src.stamp == serial && src.active) {
                    ChannelMixing.mix(src.bus, mono, ChannelInterpretation.SPEAKERS, true);
                    hasInput = true;
                }
            }
        }
        if (!aRate) {
            float v = intrinsic[0];
            if (hasInput) {
                v += mono.channel(0)[0];
            }
            values[0] = finish(v);
            constantBlock = true;
            return;
        }
        float[] in = hasInput ? mono.channel(0) : null;
        if (!hasInput && !varies) {
            // 입력도 변화도 없으면 블록 전체가 같은 값이다.
            values[0] = finish(intrinsic[0]);
            constantBlock = true;
            return;
        }
        // 프레임마다 고유 값에 입력을 더하고 범위를 제한한다.
        for (int i = 0; i < frames; i++) {
            float v = intrinsic[i];
            if (in != null) {
                v += in[i];
            }
            values[i] = finish(v);
        }
        constantBlock = false;
    }

    /**
     * Replaces a NaN with the default value and clamps to the nominal range.
     * <p>
     * NaN 을 기본 값으로 바꾸고 명목 범위로 제한한다.
     *
     * @param v the value / 값
     * @return the final value / 최종 값
     */
    private float finish(float v) {
        if (v != v) {
            v = defaultValue;
        }
        if (v < minValue) {
            return minValue;
        }
        if (v > maxValue) {
            return maxValue;
        }
        return v;
    }

    /**
     * Tells whether every computed value of the last quantum is the same (rendering thread).
     * <p>
     * 마지막 quantum 의 계산된 값이 모두 같은지 알려 준다(렌더링 스레드).
     *
     * @return true if the block is constant / 블록이 상수이면 true
     */
    boolean isConstantBlock() {
        return constantBlock;
    }

    /**
     * Returns the computed value at a frame of the last quantum (rendering thread).
     * <p>
     * 마지막 quantum 의 프레임에서 계산된 값을 반환한다(렌더링 스레드).
     *
     * @param frame the frame index in the quantum / quantum 안의 프레임 인덱스
     * @return the value / 값
     */
    float valueAt(int frame) {
        return constantBlock ? values[0] : values[frame];
    }

    /**
     * Returns the computed values of the last quantum; only <code>[0]</code> is valid when {@link #isConstantBlock()} is true (rendering
     * thread).
     * <p>
     * 마지막 quantum 의 계산된 값 배열을 반환한다. {@link #isConstantBlock()} 이 true 이면 <code>[0]</code> 만 유효하다(렌더링 스레드).
     *
     * @return the live array / 살아 있는 배열
     */
    float[] getBlockValues() {
        return values;
    }
}
