package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * Multiplies every sample of every channel of its input by the computed value of the <code>gain</code> parameter (Web Audio API 1.20).
 * The number of channels of the output equals the number of channels of the input.
 * <p>
 * 입력의 모든 채널의 모든 샘플에 <code>gain</code> 파라미터의 계산된 값을 곱한다(Web Audio API 1.20). 출력의 채널 수는 입력의 채널 수와 같다.
 */
public final class GainNode extends AudioNode {

    /**
     * The gain (a-rate, default 1, unlimited nominal range).
     * <p>
     * 이득(a-rate, 기본 1, 명목 범위 제한 없음).
     */
    private final AudioParam gain;

    /**
     * Creates a gain node with a gain of 1.
     * <p>
     * 이득이 1 인 게인 노드를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public GainNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        gain = registerParam(new AudioParam(context, this, "gain", 1f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
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
     * Multiplies the input by the gain.
     * <p>
     * 입력에 이득을 곱한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        int channels = in.getNumberOfChannels();
        out.setNumberOfChannels(channels);
        boolean constant = gain.isConstantBlock();
        float g = gain.valueAt(0);
        float[] gains = gain.getBlockValues();
        // 채널마다 샘플에 이득을 곱한다. 이득이 일정하면 값 하나를 쓴다.
        for (int c = 0; c < channels; c++) {
            float[] src = in.channel(c);
            float[] dst = out.channel(c);
            if (constant) {
                // 이득이 일정하면 같은 값을 곱한다.
                for (int i = 0; i < frames; i++) {
                    dst[i] = src[i] * g;
                }
            } else {
                // 이득이 프레임마다 다르면 프레임별 값을 곱한다.
                for (int i = 0; i < frames; i++) {
                    dst[i] = src[i] * gains[i];
                }
            }
        }
    }
}
