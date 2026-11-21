package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * Combines the channels of several inputs into one stream (Web Audio API 1.14): it has a number of inputs and one output whose number of
 * channels equals the number of inputs while any input is active. Every input is mixed down to one channel (<code>channelCount</code> 1,
 * <code>explicit</code>) and an input without a connection counts as a silent channel, so the order of the output channels never changes.
 * The node does not interpret the channel identities.
 * <p>
 * 여러 입력의 채널을 하나의 스트림으로 합친다(Web Audio API 1.14): 입력이 여러 개이고 출력이 하나이며, 입력 중 하나라도 활성인 동안 출력의 채널 수는
 * 입력 수와 같다. 모든 입력은 채널 하나로 다운믹스되고(<code>channelCount</code> 1, <code>explicit</code>) 연결이 없는 입력은 무음 채널 하나로 세므로
 * 출력 채널의 순서는 바뀌지 않는다. 노드는 채널의 의미를 해석하지 않는다.
 */
public final class ChannelMergerNode extends AudioNode {

    /**
     * Creates a merger.
     * <p>
     * 머저를 만든다.
     *
     * @param context        the context / 컨텍스트
     * @param numberOfInputs the number of inputs, from 1 to the maximum number of channels / 입력 수(1 ~ 최대 채널 수)
     * @throws AudioException <code>IndexSizeError</code> if the number of inputs is out of range / 입력 수가 범위를 벗어나면 <code>IndexSizeError</code>
     */
    public ChannelMergerNode(BaseAudioContext context, int numberOfInputs) {
        super(context, checkInputs(numberOfInputs), 1, 1, ChannelCountMode.EXPLICIT, ChannelInterpretation.SPEAKERS);
    }

    /**
     * Checks the number of inputs.
     * <p>
     * 입력 수를 검사한다.
     *
     * @param n the number of inputs / 입력 수
     * @return the same number / 같은 수
     */
    private static int checkInputs(int n) {
        if (n < 1 || n > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "numberOfInputs " + n + " is not in 1.." + WebAudioLimits.MAX_CHANNELS);
        }
        return n;
    }

    /**
     * Rejects a <code>channelCount</code> other than 1.
     * <p>
     * 1 이 아닌 <code>channelCount</code> 를 거부한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (count != 1) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCount of a ChannelMergerNode cannot be changed");
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
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCountMode of a ChannelMergerNode cannot be changed");
        }
    }

    /**
     * Puts the one channel of every input into its own channel of the output.
     * <p>
     * 입력마다의 채널 하나를 출력의 각자의 채널에 넣는다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus out = outputBus(0);
        out.setNumberOfChannels(inputs.length);
        // 입력 k 의 채널 하나를 출력의 채널 k 로 복사한다.
        for (int k = 0; k < inputs.length; k++) {
            System.arraycopy(inputBus(k).channel(0), 0, out.channel(k), 0, frames);
        }
    }
}
