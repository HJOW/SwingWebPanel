package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.BiquadCoefficients;
import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.BiquadFilter;

/**
 * A second-order filter of one of the eight {@link BiquadFilterType types} (Web Audio API 1.13). The parameters <code>frequency</code>,
 * <code>detune</code>, <code>Q</code> and <code>gain</code> are a-rate; the frequency that the filter uses is
 * <code>frequency * 2^(detune / 1200)</code>. The coefficients are those of the specification (the Audio EQ Cookbook) and are computed
 * again for every frame in which a parameter changes. The number of channels of the output equals that of the input. The node has a tail
 * time (the filter is an IIR filter) that the engine estimates from the poles of the current coefficients, so the output of a resonant
 * filter does not stop abruptly when the input becomes silent; the tail is limited to
 * {@link WebAudioLimits#MAX_TAIL_SECONDS} seconds.
 * <p>
 * 여덟 가지 {@link BiquadFilterType 종류} 중 하나의 2차 필터(Web Audio API 1.13). 파라미터 <code>frequency</code>, <code>detune</code>, <code>Q</code>,
 * <code>gain</code> 은 a-rate 이며 필터가 쓰는 주파수는 <code>frequency * 2^(detune / 1200)</code> 이다. 계수는 규격(Audio EQ Cookbook)의 것이며 파라미터가
 * 바뀌는 프레임마다 다시 계산한다. 출력의 채널 수는 입력과 같다. 노드는 꼬리 시간(필터가 IIR 필터이므로)을 가지며 엔진이 현재 계수의 극에서 추정하므로
 * 공진이 큰 필터의 출력이 입력이 무음이 될 때 갑자기 멈추지 않는다. 꼬리는 {@link WebAudioLimits#MAX_TAIL_SECONDS} 초로 제한된다.
 */
public final class BiquadFilterNode extends AudioNode {

    /**
     * The approximate largest <code>gain</code> in dB, <code>40 log10(FLT_MAX)</code>.
     * <p>
     * 대략적인 <code>gain</code> 최댓값(dB) <code>40 log10(FLT_MAX)</code>.
     */
    private static final float MAX_GAIN_DB = 1541.0f;

    /**
     * The frequency in Hz (a-rate, default 350, from 0 to the Nyquist frequency).
     * <p>
     * 주파수(Hz, a-rate, 기본 350, 0 ~ 나이퀴스트 주파수).
     */
    private final AudioParam frequency;

    /**
     * The detune in cents (a-rate, default 0).
     * <p>
     * 디튠(센트, a-rate, 기본 0).
     */
    private final AudioParam detune;

    /**
     * The Q factor (a-rate, default 1); in dB for the low-pass and the high-pass filter.
     * <p>
     * Q 값(a-rate, 기본 1). 저역/고역 통과 필터에서는 dB.
     */
    private final AudioParam q;

    /**
     * The gain in dB (a-rate, default 0), used by the shelves and the peaking filter.
     * <p>
     * 이득(dB, a-rate, 기본 0). 선반 필터와 피킹 필터가 쓴다.
     */
    private final AudioParam gain;

    /**
     * The type of the filter.
     * <p>
     * 필터의 종류.
     */
    private volatile BiquadFilterType type = BiquadFilterType.LOWPASS;

    /**
     * The filter state (rendering thread).
     * <p>
     * 필터 상태(렌더링 스레드).
     */
    private final BiquadFilter filter = new BiquadFilter(2);

    /**
     * The coefficients of the last frame (rendering thread).
     * <p>
     * 마지막 프레임의 계수(렌더링 스레드).
     */
    private final BiquadCoefficients coefficients = new BiquadCoefficients();

    /**
     * The coefficients of every frame of the quantum, <code>b0</code> (rendering thread).
     * <p>
     * quantum 의 프레임별 계수 <code>b0</code>(렌더링 스레드).
     */
    private final double[] cb0;

    /**
     * The coefficients of every frame, <code>b1</code>.
     * <p>
     * 프레임별 계수 <code>b1</code>.
     */
    private final double[] cb1;

    /**
     * The coefficients of every frame, <code>b2</code>.
     * <p>
     * 프레임별 계수 <code>b2</code>.
     */
    private final double[] cb2;

    /**
     * The coefficients of every frame, <code>a1</code>.
     * <p>
     * 프레임별 계수 <code>a1</code>.
     */
    private final double[] ca1;

    /**
     * The coefficients of every frame, <code>a2</code>.
     * <p>
     * 프레임별 계수 <code>a2</code>.
     */
    private final double[] ca2;

    /**
     * The estimated tail time in frames from the coefficients of the last processed quantum.
     * <p>
     * 마지막으로 처리한 quantum 의 계수에서 추정한 꼬리 시간(프레임).
     */
    private long tail;

    /**
     * Creates a low-pass filter of 350 Hz.
     * <p>
     * 350 Hz 의 저역 통과 필터를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public BiquadFilterNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        float nyquist = context.getSampleRate() / 2f;
        frequency = registerParam(new AudioParam(context, this, "frequency", 350f, 0f, nyquist, AutomationRate.A_RATE, false));
        detune = registerParam(new AudioParam(context, this, "detune", 0f, -153600f, 153600f, AutomationRate.A_RATE, false));
        q = registerParam(new AudioParam(context, this, "Q", 1f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        gain = registerParam(new AudioParam(context, this, "gain", 0f, -AudioParam.MAX_FLOAT, MAX_GAIN_DB, AutomationRate.A_RATE, false));
        int n = context.getRenderQuantumSize();
        cb0 = new double[n];
        cb1 = new double[n];
        cb2 = new double[n];
        ca1 = new double[n];
        ca2 = new double[n];
    }

    /**
     * Returns the <code>frequency</code> parameter.
     * <p>
     * <code>frequency</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getFrequency() {
        return frequency;
    }

    /**
     * Returns the <code>detune</code> parameter.
     * <p>
     * <code>detune</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getDetune() {
        return detune;
    }

    /**
     * Returns the <code>Q</code> parameter.
     * <p>
     * <code>Q</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getQ() {
        return q;
    }

    /**
     * Returns the <code>gain</code> parameter.
     * <p>
     * <code>gain</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getGain() {
        return gain;
    }

    /**
     * Returns the type of the filter.
     * <p>
     * 필터의 종류를 반환한다.
     *
     * @return the type / 종류
     */
    public BiquadFilterType getType() {
        return type;
    }

    /**
     * Sets the type of the filter.
     * <p>
     * 필터의 종류를 정한다.
     *
     * @param newType the type / 종류
     */
    public void setType(BiquadFilterType newType) {
        if (newType == null) {
            throw new AudioException(AudioException.Kind.TYPE, "type is null");
        }
        type = newType;
    }

    /**
     * Calculates the frequency response for frequencies, with the <code>[[current value]]</code> of the parameters. A frequency outside
     * the range from 0 to the Nyquist frequency gives NaN.
     * <p>
     * 파라미터의 <code>[[current value]]</code> 로 주어진 주파수의 주파수 응답을 계산한다. 0 에서 나이퀴스트 주파수까지의 범위 밖의 주파수는 NaN 을 준다.
     *
     * @param frequencyHz the frequencies in Hz / 주파수(Hz)
     * @param magResponse receives the linear magnitude / 선형 크기를 받는다
     * @param phaseResponse receives the phase in radians / 위상(라디안)을 받는다
     * @throws AudioException <code>InvalidAccessError</code> if the arrays differ in length / 배열의 길이가 다르면 <code>InvalidAccessError</code>
     */
    public void getFrequencyResponse(float[] frequencyHz, float[] magResponse, float[] phaseResponse) {
        if (frequencyHz.length != magResponse.length || frequencyHz.length != phaseResponse.length) {
            throw new AudioException(AudioException.Kind.INVALID_ACCESS, "the three arrays must have the same length");
        }
        double nyquist = getContext().getSampleRate() / 2.0;
        double f = frequency.getValue() * Math.pow(2.0, detune.getValue() / 1200.0);
        BiquadCoefficients c = new BiquadCoefficients();
        c.compute(type.getKernelType(), f / nyquist, q.getValue(), gain.getValue());
        double[] r = new double[2];
        // 주파수마다 전달 함수를 계산한다. 범위 밖의 주파수는 NaN 이다.
        for (int k = 0; k < frequencyHz.length; k++) {
            double x = frequencyHz[k];
            if (!(x >= 0 && x <= nyquist)) {
                magResponse[k] = Float.NaN;
                phaseResponse[k] = Float.NaN;
            } else {
                c.response(x / nyquist, r);
                magResponse[k] = (float) Math.sqrt(r[0] * r[0] + r[1] * r[1]);
                phaseResponse[k] = (float) Math.atan2(r[1], r[0]);
            }
        }
    }

    /**
     * The tail time estimated from the poles of the filter.
     * <p>
     * 필터의 극에서 추정한 꼬리 시간.
     *
     * @return the tail time in frames / 꼬리 시간(프레임)
     */
    @Override
    long tailFrames() {
        return tail;
    }

    /**
     * Clears the filter state when the node becomes inactive.
     * <p>
     * 노드가 비활성이 되면 필터 상태를 지운다.
     */
    @Override
    void resetState() {
        filter.reset();
    }

    /**
     * Filters the input.
     * <p>
     * 입력을 필터링한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        int channels = in.getNumberOfChannels();
        out.setNumberOfChannels(channels);
        filter.setChannels(channels);
        int kt = type.getKernelType();
        double nyquist = getContext().getSampleRate() / 2.0;
        boolean constant = frequency.isConstantBlock() && detune.isConstantBlock() && q.isConstantBlock() && gain.isConstantBlock();
        if (constant) {
            // 파라미터가 블록 동안 일정하면 계수를 한 번만 계산한다.
            double f = frequency.valueAt(0) * Math.pow(2.0, detune.valueAt(0) / 1200.0);
            coefficients.compute(kt, f / nyquist, q.valueAt(0), gain.valueAt(0));
            store(0);
        } else {
            double lastF = Double.NaN;
            double lastQ = Double.NaN;
            double lastG = Double.NaN;
            boolean detuneConstant = detune.isConstantBlock();
            double detuneFactor = detuneConstant ? Math.pow(2.0, detune.valueAt(0) / 1200.0) : 1.0;
            // 프레임마다 파라미터가 바뀐 경우에만 계수를 다시 계산한다. 디튠이 일정하면 거듭제곱은 한 번만 계산한다.
            for (int i = 0; i < frames; i++) {
                double f = frequency.valueAt(i) * (detuneConstant ? detuneFactor : Math.pow(2.0, detune.valueAt(i) / 1200.0));
                double qq = q.valueAt(i);
                double g = gain.valueAt(i);
                if (f != lastF || qq != lastQ || g != lastG) {
                    coefficients.compute(kt, f / nyquist, qq, g);
                    lastF = f;
                    lastQ = qq;
                    lastG = g;
                }
                store(i);
            }
        }
        // 채널마다 필터를 적용한다.
        for (int c = 0; c < channels; c++) {
            filter.process(c, in.channel(c), out.channel(c), frames, cb0, cb1, cb2, ca1, ca2, constant);
        }
        tail = coefficients.tailFrames((long) (WebAudioLimits.MAX_TAIL_SECONDS * getContext().getSampleRate()));
    }

    /**
     * Stores the current coefficients for a frame.
     * <p>
     * 현재 계수를 프레임에 저장한다.
     *
     * @param i the frame index / 프레임 인덱스
     */
    private void store(int i) {
        cb0[i] = coefficients.b0;
        cb1[i] = coefficients.b1;
        cb2[i] = coefficients.b2;
        ca1[i] = coefficients.a1;
        ca2[i] = coefficients.a2;
    }
}
