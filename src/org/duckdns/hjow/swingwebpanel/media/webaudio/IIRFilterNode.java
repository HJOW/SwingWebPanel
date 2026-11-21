package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.IirFilter;

/**
 * A general IIR filter with fixed coefficients (Web Audio API 1.21): <code>H(z) = sum b[m] z^-m / sum a[n] z^-n</code> with at most 20
 * coefficients in each array. The coefficients cannot change after the node is created. The number of channels of the output equals
 * that of the input. The tail time is estimated from the impulse response of the filter when the node is created (limited to
 * {@link WebAudioLimits#MAX_TAIL_SECONDS} seconds).
 * <p>
 * 계수가 고정된 일반 IIR 필터(Web Audio API 1.21): <code>H(z) = sum b[m] z^-m / sum a[n] z^-n</code> 이며 각 배열의 계수는 최대 20개이다. 노드를 만든
 * 뒤에는 계수를 바꿀 수 없다. 출력의 채널 수는 입력과 같다. 꼬리 시간은 노드를 만들 때 필터의 임펄스 응답으로 추정한다({@link WebAudioLimits#MAX_TAIL_SECONDS}
 * 초로 제한).
 */
public final class IIRFilterNode extends AudioNode {

    /**
     * The filter.
     * <p>
     * 필터.
     */
    private final IirFilter filter;

    /**
     * The tail time in frames.
     * <p>
     * 꼬리 시간(프레임).
     */
    private final long tail;

    /**
     * Creates an IIR filter.
     * <p>
     * IIR 필터를 만든다.
     *
     * @param context     the context / 컨텍스트
     * @param feedforward the numerator coefficients: 1 to 20, not all 0 / 분자 계수: 1~20개, 모두 0 은 아님
     * @param feedback    the denominator coefficients: 1 to 20, the first one not 0 / 분모 계수: 1~20개, 첫 계수는 0 이 아님
     * @throws AudioException <code>NotSupportedError</code> for an array that is empty or longer than 20, <code>InvalidStateError</code> if all feedforward
     *                        coefficients are 0 or the first feedback coefficient is 0 / 배열이 비었거나 20개를 넘으면 <code>NotSupportedError</code>, 피드포워드
     *                        계수가 모두 0 이거나 첫 피드백 계수가 0 이면 <code>InvalidStateError</code>
     */
    public IIRFilterNode(BaseAudioContext context, double[] feedforward, double[] feedback) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (feedforward == null || feedback == null) {
            throw new AudioException(AudioException.Kind.TYPE, "feedforward or feedback is null");
        }
        if (feedforward.length == 0 || feedforward.length > WebAudioLimits.MAX_IIR_COEFFICIENTS || feedback.length == 0
                || feedback.length > WebAudioLimits.MAX_IIR_COEFFICIENTS) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "an IIR filter needs 1 to " + WebAudioLimits.MAX_IIR_COEFFICIENTS + " coefficients in each array");
        }
        boolean allZero = true;
        // 계수가 모두 유한한지 확인하고, 피드포워드 계수가 모두 0 인지 본다.
        for (int i = 0; i < feedforward.length; i++) {
            if (Double.isNaN(feedforward[i]) || Double.isInfinite(feedforward[i])) {
                throw new AudioException(AudioException.Kind.TYPE, "feedforward[" + i + "] is not finite");
            }
            if (feedforward[i] != 0.0) {
                allZero = false;
            }
        }
        for (int i = 0; i < feedback.length; i++) {
            if (Double.isNaN(feedback[i]) || Double.isInfinite(feedback[i])) {
                throw new AudioException(AudioException.Kind.TYPE, "feedback[" + i + "] is not finite");
            }
        }
        if (allZero) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "all feedforward coefficients are 0");
        }
        if (feedback[0] == 0.0) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the first feedback coefficient is 0");
        }
        filter = new IirFilter(feedforward, feedback, 2);
        tail = filter.estimateTailFrames((long) (WebAudioLimits.MAX_TAIL_SECONDS * context.getSampleRate()));
    }

    /**
     * Calculates the frequency response for frequencies. A frequency outside the range from 0 to the Nyquist frequency gives NaN.
     * <p>
     * 주어진 주파수의 주파수 응답을 계산한다. 0 에서 나이퀴스트 주파수까지의 범위 밖의 주파수는 NaN 을 준다.
     *
     * @param frequencyHz   the frequencies in Hz / 주파수(Hz)
     * @param magResponse   receives the linear magnitude / 선형 크기를 받는다
     * @param phaseResponse receives the phase in radians / 위상(라디안)을 받는다
     * @throws AudioException <code>InvalidAccessError</code> if the arrays differ in length / 배열의 길이가 다르면 <code>InvalidAccessError</code>
     */
    public void getFrequencyResponse(float[] frequencyHz, float[] magResponse, float[] phaseResponse) {
        if (frequencyHz.length != magResponse.length || frequencyHz.length != phaseResponse.length) {
            throw new AudioException(AudioException.Kind.INVALID_ACCESS, "the three arrays must have the same length");
        }
        filter.frequencyResponse(frequencyHz, magResponse, phaseResponse, getContext().getSampleRate());
    }

    /**
     * The tail time estimated from the impulse response.
     * <p>
     * 임펄스 응답에서 추정한 꼬리 시간.
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
        // 채널마다 필터를 적용한다.
        for (int c = 0; c < channels; c++) {
            filter.process(c, in.channel(c), out.channel(c), frames);
        }
    }
}
