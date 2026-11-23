package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A source whose single output channel is the constant value of its <code>offset</code> parameter (Web Audio API 1.16). Automating
 * <code>offset</code> or connecting another node to it makes the node a constructible audio parameter that can drive several parameters
 * at once.
 * <p>
 * 하나뿐인 출력 채널이 <code>offset</code> 파라미터의 상수 값인 원천(Web Audio API 1.16). <code>offset</code> 을 자동화하거나 다른 노드를 연결하면 이
 * 노드는 여러 파라미터를 한꺼번에 구동하는, 만들 수 있는 오디오 파라미터가 된다.
 */
public final class ConstantSourceNode extends AudioScheduledSourceNode {

    /**
     * The constant value (a-rate, default 1, unlimited nominal range).
     * <p>
     * 상수 값(a-rate, 기본 1, 명목 범위 제한 없음).
     */
    private final AudioParam offset;

    /**
     * Creates a constant source with an offset of 1.
     * <p>
     * 오프셋이 1 인 상수 원천을 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public ConstantSourceNode(BaseAudioContext context) {
        super(context);
        offset = registerParam(new AudioParam(context, this, "offset", 1f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
    }

    /**
     * Returns the <code>offset</code> parameter.
     * <p>
     * <code>offset</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getOffset() {
        return offset;
    }

    /**
     * Writes the value of <code>offset</code> for the frames that are played.
     * <p>
     * 재생하는 프레임에 <code>offset</code> 의 값을 쓴다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus bus = outputBus(0);
        bus.setNumberOfChannels(1);
        float[] out = bus.channel(0);
        long frame0 = getQuantumStart();
        int first = firstPlayFrame(frame0, frames);
        int end = endPlayFrame(frame0, frames);
        // 재생하는 구간에는 offset 값을, 나머지에는 0 을 쓴다.
        for (int i = 0; i < frames; i++) {
            out[i] = (i >= first && i < end) ? offset.valueAt(i) : 0f;
        }
    }
}
