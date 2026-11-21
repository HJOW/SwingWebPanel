package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * Gives access to the individual channels of its input (Web Audio API 1.15): it has one input and a fixed number of outputs of one
 * channel each. Output <code>k</code> carries the channel <code>k</code> of the input; an output without a matching channel is silent.
 * The node does not interpret the channel identities. Its input has the fixed settings <code>channelCount</code> equal to the number of
 * outputs, <code>channelCountMode</code> <code>explicit</code> and <code>channelInterpretation</code> <code>discrete</code>.
 * <p>
 * 입력의 개별 채널에 접근하게 한다(Web Audio API 1.15): 입력이 하나이고 채널이 하나씩인 출력이 정해진 수만큼 있다. 출력 <code>k</code> 는 입력의 채널
 * <code>k</code> 를 싣고, 대응하는 채널이 없는 출력은 무음이다. 노드는 채널의 의미를 해석하지 않는다. 입력은 <code>channelCount</code> 가 출력 수와 같고
 * <code>channelCountMode</code> 가 <code>explicit</code>, <code>channelInterpretation</code> 이 <code>discrete</code> 로 고정이다.
 */
public final class ChannelSplitterNode extends AudioNode {

    /**
     * Creates a splitter.
     * <p>
     * 스플리터를 만든다.
     *
     * @param context         the context / 컨텍스트
     * @param numberOfOutputs the number of outputs, from 1 to the maximum number of channels / 출력 수(1 ~ 최대 채널 수)
     * @throws AudioException <code>IndexSizeError</code> if the number of outputs is out of range / 출력 수가 범위를 벗어나면 <code>IndexSizeError</code>
     */
    public ChannelSplitterNode(BaseAudioContext context, int numberOfOutputs) {
        super(context, 1, checkOutputs(numberOfOutputs), numberOfOutputs, ChannelCountMode.EXPLICIT, ChannelInterpretation.DISCRETE);
    }

    /**
     * Checks the number of outputs.
     * <p>
     * 출력 수를 검사한다.
     *
     * @param n the number of outputs / 출력 수
     * @return the same number / 같은 수
     */
    private static int checkOutputs(int n) {
        if (n < 1 || n > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "numberOfOutputs " + n + " is not in 1.." + WebAudioLimits.MAX_CHANNELS);
        }
        return n;
    }

    /**
     * Rejects a <code>channelCount</code> other than the number of outputs.
     * <p>
     * 출력 수가 아닌 <code>channelCount</code> 를 거부한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (count != getNumberOfOutputs()) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCount of a ChannelSplitterNode cannot be changed");
        }
    }

    /**
     * Rejects a mode other than <code>explicit</code>.
     * <p>
     * <code>explicit</code> 가 아닌 모드를 거부한다.
     *
     * @param mode the new value / 새 값
     */
    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode != ChannelCountMode.EXPLICIT) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCountMode of a ChannelSplitterNode cannot be changed");
        }
    }

    /**
     * Rejects an interpretation other than <code>discrete</code>.
     * <p>
     * <code>discrete</code> 가 아닌 해석을 거부한다.
     *
     * @param interpretation the new value / 새 값
     */
    @Override
    void checkChannelInterpretation(ChannelInterpretation interpretation) {
        if (interpretation != ChannelInterpretation.DISCRETE) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelInterpretation of a ChannelSplitterNode cannot be changed");
        }
    }

    /**
     * Copies each channel of the input to its own output.
     * <p>
     * 입력의 채널을 각각의 출력으로 복사한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        int channels = in.getNumberOfChannels();
        // 출력마다 대응하는 입력 채널을 복사한다. 대응하는 채널이 없는 출력은 무음이며 활성이 아니다.
        for (int k = 0; k < outputs.length; k++) {
            AudioBus out = outputBus(k);
            out.setNumberOfChannels(1);
            if (k < channels) {
                System.arraycopy(in.channel(k), 0, out.channel(0), 0, frames);
            } else {
                java.util.Arrays.fill(out.channel(0), 0, frames, 0f);
                outputs[k].active = false;
            }
        }
    }
}
