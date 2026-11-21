package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.DynamicsCompressorKernel;

/**
 * A dynamics compression effect (Web Audio API 1.19): it lowers the volume of the loudest parts of the signal, so that loud and soft
 * sounds that play together stay under control and clipping is avoided. The processing has a look-ahead of 6 milliseconds (a fixed
 * latency), the parameters <code>threshold</code>, <code>knee</code>, <code>ratio</code>, <code>attack</code> and <code>release</code>
 * are read once per render quantum (they are k-rate and cannot be changed to a-rate), and <code>reduction</code> reports the gain
 * reduction in decibels. The input has one or two channels and the output has as many; the detector is linked across the channels, so
 * every channel gets the same gain. See {@link DynamicsCompressorKernel} for the algorithm.
 * <p>
 * 다이내믹 압축 효과(Web Audio API 1.19): 신호에서 가장 큰 부분의 음량을 낮춰 함께 재생되는 큰 소리와 작은 소리를 다스리고 클리핑을 피한다. 처리는 6 ms 의
 * 선행 지연(고정 지연)이 있고, <code>threshold</code>, <code>knee</code>, <code>ratio</code>, <code>attack</code>, <code>release</code> 파라미터는 render
 * quantum 마다 한 번 읽으며(k-rate 이고 a-rate 로 바꿀 수 없다), <code>reduction</code> 이 이득 감소량(dB)을 알린다. 입력은 채널이 하나나 둘이고 출력은 같은
 * 수이다. 검출기가 채널을 묶으므로 모든 채널이 같은 게인을 받는다. 알고리즘은 {@link DynamicsCompressorKernel} 을 본다.
 */
public final class DynamicsCompressorNode extends AudioNode {

    /**
     * The level in decibels above which the compression starts (default -24, from -100 to 0).
     * <p>
     * 압축이 시작되는 레벨(dB, 기본 -24, -100 ~ 0).
     */
    private final AudioParam threshold;

    /**
     * The range in decibels above the threshold in which the curve changes smoothly to the ratio (default 30, from 0 to 40).
     * <p>
     * 임계값 위에서 곡선이 비율로 부드럽게 바뀌는 구간(dB, 기본 30, 0 ~ 40).
     */
    private final AudioParam knee;

    /**
     * The change in decibels of the input for a change of 1 dB of the output (default 12, from 1 to 20).
     * <p>
     * 출력 1 dB 변화에 대한 입력의 변화(dB, 기본 12, 1 ~ 20).
     */
    private final AudioParam ratio;

    /**
     * The time in seconds to reduce the gain by 10 dB (default 0.003, from 0 to 1).
     * <p>
     * 게인을 10 dB 줄이는 데 걸리는 시간(초, 기본 0.003, 0 ~ 1).
     */
    private final AudioParam attack;

    /**
     * The time in seconds to increase the gain by 10 dB (default 0.25, from 0 to 1).
     * <p>
     * 게인을 10 dB 늘리는 데 걸리는 시간(초, 기본 0.25, 0 ~ 1).
     */
    private final AudioParam release;

    /**
     * The signal processing (rendering thread).
     * <p>
     * 신호 처리(렌더링 스레드).
     */
    private final DynamicsCompressorKernel kernel;

    /**
     * The gain reduction in decibels that was metered at the end of the last quantum.
     * <p>
     * 마지막 quantum 끝에서 측정한 이득 감소량(dB).
     */
    private volatile float reduction;

    /**
     * The input channels of the quantum (rendering thread).
     * <p>
     * quantum 의 입력 채널(렌더링 스레드).
     */
    private final float[][] inChannels = new float[2][];

    /**
     * The output channels of the quantum (rendering thread).
     * <p>
     * quantum 의 출력 채널(렌더링 스레드).
     */
    private final float[][] outChannels = new float[2][];

    /**
     * Creates a compressor with the default parameters.
     * <p>
     * 기본 파라미터의 컴프레서를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public DynamicsCompressorNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
        threshold = registerParam(new AudioParam(context, this, "threshold", -24f, -100f, 0f, AutomationRate.K_RATE, true));
        knee = registerParam(new AudioParam(context, this, "knee", 30f, 0f, 40f, AutomationRate.K_RATE, true));
        ratio = registerParam(new AudioParam(context, this, "ratio", 12f, 1f, 20f, AutomationRate.K_RATE, true));
        attack = registerParam(new AudioParam(context, this, "attack", 0.003f, 0f, 1f, AutomationRate.K_RATE, true));
        release = registerParam(new AudioParam(context, this, "release", 0.25f, 0f, 1f, AutomationRate.K_RATE, true));
        kernel = new DynamicsCompressorKernel(context.getSampleRate(), 2);
    }

    /**
     * Returns the <code>threshold</code> parameter.
     * <p>
     * <code>threshold</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getThreshold() {
        return threshold;
    }

    /**
     * Returns the <code>knee</code> parameter.
     * <p>
     * <code>knee</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getKnee() {
        return knee;
    }

    /**
     * Returns the <code>ratio</code> parameter.
     * <p>
     * <code>ratio</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getRatio() {
        return ratio;
    }

    /**
     * Returns the <code>attack</code> parameter.
     * <p>
     * <code>attack</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getAttack() {
        return attack;
    }

    /**
     * Returns the <code>release</code> parameter.
     * <p>
     * <code>release</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getRelease() {
        return release;
    }

    /**
     * Returns the gain reduction in decibels that the compressor applied, as metered at the end of the last render quantum: 0 or less,
     * and 0 when the node has no signal.
     * <p>
     * 컴프레서가 적용한 이득 감소량(dB)을 마지막 render quantum 끝에서 측정한 값으로 반환한다. 0 이하이며 노드에 신호가 없으면 0 이다.
     *
     * @return the reduction / 감소량
     */
    public float getReduction() {
        return reduction;
    }

    /**
     * Rejects a <code>channelCount</code> above 2.
     * <p>
     * 2 를 넘는 <code>channelCount</code> 를 거부한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (count > 2) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCount of a DynamicsCompressorNode is at most 2");
        }
    }

    /**
     * Rejects the mode <code>max</code>.
     * <p>
     * <code>max</code> 모드를 거부한다.
     *
     * @param mode the new value / 새 값
     */
    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCountMode of a DynamicsCompressorNode cannot be max");
        }
    }

    /**
     * Returns the tail time: the look-ahead and the time that the metered reduction needs to fall back (five metering time constants).
     * <p>
     * 꼬리 시간을 반환한다: 선행 지연과 측정한 감소량이 되돌아오는 데 걸리는 시간(측정 시간 상수의 다섯 배).
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    @Override
    long tailFrames() {
        return kernel.getTailFrames();
    }

    /**
     * Clears the state of the compressor and the metered reduction.
     * <p>
     * 컴프레서의 상태와 측정한 감소량을 지운다.
     */
    @Override
    void resetState() {
        kernel.reset();
        reduction = 0f;
    }

    /**
     * Compresses the input.
     * <p>
     * 입력을 압축한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        int channels = in.getNumberOfChannels();
        out.setNumberOfChannels(channels);
        // 채널 배열을 준비한다.
        for (int c = 0; c < channels; c++) {
            inChannels[c] = in.channel(c);
            outChannels[c] = out.channel(c);
        }
        // 파라미터는 k-rate 이므로 quantum 의 첫 값을 쓴다.
        kernel.process(inChannels, outChannels, channels, frames, threshold.valueAt(0), knee.valueAt(0), ratio.valueAt(0), attack.valueAt(0),
                release.valueAt(0));
        reduction = (float) kernel.getReduction();
    }
}
