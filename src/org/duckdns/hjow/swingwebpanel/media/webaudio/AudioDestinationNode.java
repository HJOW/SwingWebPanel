package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The final destination of a context (Web Audio API 1.10): all audio that is heard is routed to it. It has one input and no output
 * (browsers report 0 outputs, although the tables of the specification say 1). Its input has the mode <code>explicit</code>: the
 * <code>channelCount</code> is the number of channels that the context renders. For a real-time context it can be set from 1 to
 * {@link #getMaxChannelCount()}; for an offline context it is the number of channels given to the constructor and cannot change. The
 * audio of the last quantum is the mixed input bus, which the context reads after each render quantum.
 * <p>
 * 컨텍스트의 최종 목적지(Web Audio API 1.10): 들리는 모든 오디오가 여기로 간다. 입력이 하나이고 출력은 없다(규격 표는 1 이라고 적지만 브라우저는 출력 0 으로
 * 보고한다). 입력은 <code>explicit</code> 모드이며 <code>channelCount</code> 는 컨텍스트가 렌더링하는 채널 수이다. 실시간 컨텍스트에서는 1 부터
 * {@link #getMaxChannelCount()} 까지 정할 수 있고, 오프라인 컨텍스트에서는 생성자에 준 채널 수이며 바꿀 수 없다. 마지막 quantum 의 오디오는 섞인 입력
 * 버스이며 컨텍스트가 render quantum 마다 읽는다.
 */
public final class AudioDestinationNode extends AudioNode {

    /**
     * The largest <code>channelCount</code>.
     * <p>
     * <code>channelCount</code> 의 최댓값.
     */
    private final int maxChannelCount;

    /**
     * True for the destination of an offline context.
     * <p>
     * 오프라인 컨텍스트의 목적지이면 true.
     */
    private final boolean offline;

    /**
     * Creates the destination of a context.
     * <p>
     * 컨텍스트의 목적지를 만든다.
     *
     * @param context         the context / 컨텍스트
     * @param channels        the initial number of channels / 처음 채널 수
     * @param maxChannelCount the largest number of channels / 최대 채널 수
     * @param offline         true for an offline context, whose channel count cannot change / 채널 수를 바꿀 수 없는 오프라인 컨텍스트이면 true
     */
    AudioDestinationNode(BaseAudioContext context, int channels, int maxChannelCount, boolean offline) {
        super(context, 1, 0, channels, ChannelCountMode.EXPLICIT, ChannelInterpretation.SPEAKERS);
        this.maxChannelCount = maxChannelCount;
        this.offline = offline;
    }

    /**
     * Returns the largest <code>channelCount</code>.
     * <p>
     * <code>channelCount</code> 의 최댓값을 반환한다.
     *
     * @return the maximum number of channels / 최대 채널 수
     */
    public int getMaxChannelCount() {
        return maxChannelCount;
    }

    /**
     * Returns the audio that the destination received in the last quantum, mixed to its <code>channelCount</code> channels.
     * <p>
     * 목적지가 마지막 quantum 에 받은 오디오를 <code>channelCount</code> 채널로 섞어 반환한다.
     *
     * @return the live bus / 살아 있는 버스
     */
    public AudioBus getRenderedBus() {
        return inputBus(0);
    }

    /**
     * Checks the new <code>channelCount</code>: it cannot change for an offline context and must be from 1 to the maximum otherwise.
     * <p>
     * 새 <code>channelCount</code> 를 검사한다: 오프라인 컨텍스트에서는 바꿀 수 없고, 그 밖에는 1 부터 최댓값까지여야 한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (offline) {
            if (count != getChannelCount()) {
                throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCount of the destination of an OfflineAudioContext cannot be changed");
            }
            return;
        }
        if (count < 1 || count > maxChannelCount) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "channelCount " + count + " is not in 1.." + maxChannelCount);
        }
    }

    /**
     * Rejects a change of the mode for an offline context.
     * <p>
     * 오프라인 컨텍스트에서는 모드 변경을 거부한다.
     *
     * @param mode the new value / 새 값
     */
    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (offline && mode != ChannelCountMode.EXPLICIT) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCountMode of the destination of an OfflineAudioContext cannot be changed");
        }
    }

    /**
     * Does nothing: the mixed input bus is the output of the destination.
     * <p>
     * 아무것도 하지 않는다: 섞인 입력 버스가 목적지의 출력이다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
    }
}
