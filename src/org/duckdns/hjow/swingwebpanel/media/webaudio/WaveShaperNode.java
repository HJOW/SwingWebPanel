package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.WaveShaperKernel;

/**
 * A non-linear distortion effect (Web Audio API 1.31). Every input sample in <code>[-1, 1]</code> indexes the <code>curve</code> with
 * linear interpolation (values below -1 give the first curve value, values above 1 the last one); without a curve the input passes
 * unchanged. The optional oversampling (<code>2x</code>, <code>4x</code>) applies the curve to an up-sampled signal and filters it back
 * (see {@link WaveShaperKernel}), which avoids some aliasing at the price of a delay of the filters and a tail time. The number of output
 * channels equals the number of input channels (the node has <code>channelCount</code> 2, mode <code>max</code> and interpretation
 * <code>speakers</code> by default).
 * <p>
 * A curve that maps zero to a non-zero value makes the node produce a constant signal even without any input; the context keeps such a
 * node processed for as long as the curve is set.
 * <p>
 * 비선형 왜곡 효과(Web Audio API 1.31). <code>[-1, 1]</code> 의 입력 샘플은 선형 보간으로 <code>curve</code> 를 색인한다(-1 보다 작으면 곡선의 첫 값, 1 보다
 * 크면 마지막 값). 곡선이 없으면 입력이 그대로 지나간다. 선택 사항인 오버샘플링(<code>2x</code>, <code>4x</code>)은 업샘플링한 신호에 곡선을 적용하고 다시
 * 필터링해 내려보내므로({@link WaveShaperKernel} 참고) 앨리어싱을 일부 막는 대신 필터의 지연과 꼬리 시간이 생긴다. 출력 채널 수는 입력 채널 수와 같다(노드의
 * 기본값은 <code>channelCount</code> 2, 모드 <code>max</code>, 해석 <code>speakers</code>).
 * <p>
 * 0 을 0 이 아닌 값으로 옮기는 곡선은 입력이 없어도 노드가 일정한 신호를 만들게 한다. 컨텍스트는 곡선이 설정된 동안 그런 노드를 계속 처리한다.
 */
public final class WaveShaperNode extends AudioNode {

    /**
     * The shaping curve, an immutable copy of the array that was set, or null.
     * <p>
     * 변환 곡선. 설정한 배열의 불변 복사본이거나 null.
     */
    private volatile float[] curve;

    /**
     * True once a curve was set: a second non-null curve is an error (<code>[[curve set]]</code> of the specification).
     * <p>
     * 곡선을 한 번 설정했으면 true. 두 번째 null 이 아닌 곡선은 오류이다(규격의 <code>[[curve set]]</code>).
     */
    private boolean curveSet;

    /**
     * The oversampling type.
     * <p>
     * 오버샘플링 종류.
     */
    private volatile OverSampleType oversample = OverSampleType.NONE;

    /**
     * True while the curve gives a non-zero value for the input zero.
     * <p>
     * 곡선이 입력 0 에 0 이 아닌 값을 주는 동안 true.
     */
    private volatile boolean constantOutput;

    /**
     * The filter state of every channel (rendering thread).
     * <p>
     * 채널마다의 필터 상태(렌더링 스레드).
     */
    private WaveShaperKernel[] kernels = new WaveShaperKernel[0];

    /**
     * The number of channels that were processed in the last quantum (rendering thread).
     * <p>
     * 마지막 quantum 에서 처리한 채널 수(렌더링 스레드).
     */
    private int usedChannels;

    /**
     * Creates a wave shaper without a curve.
     * <p>
     * 곡선이 없는 웨이브셰이퍼를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public WaveShaperNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
    }

    /**
     * Returns a copy of the curve, or null.
     * <p>
     * 곡선의 복사본을 반환한다. 없으면 null.
     *
     * @return the curve / 곡선
     */
    public float[] getCurve() {
        float[] c = curve;
        return c == null ? null : c.clone();
    }

    /**
     * Sets the curve. The node keeps a copy. A curve can be set only once; setting null is always allowed.
     * <p>
     * 곡선을 설정한다. 노드는 복사본을 보관한다. 곡선은 한 번만 설정할 수 있고 null 은 언제나 설정할 수 있다.
     *
     * @param newCurve the curve with at least 2 values, or null / 값이 둘 이상인 곡선. 또는 null
     * @throws AudioException <code>InvalidStateError</code> if a curve was set before or the curve has fewer than 2 values /
     *                        이전에 곡선을 설정했거나 곡선의 값이 둘보다 적으면 <code>InvalidStateError</code>
     */
    public void setCurve(float[] newCurve) {
        float[] copy = null;
        boolean dc = false;
        synchronized (this) {
            if (newCurve != null) {
                if (curveSet) {
                    throw new AudioException(AudioException.Kind.INVALID_STATE, "the curve of a WaveShaperNode can be set only once");
                }
                if (newCurve.length < 2) {
                    throw new AudioException(AudioException.Kind.INVALID_STATE, "the curve needs at least 2 values");
                }
                copy = newCurve.clone();
                curveSet = true;
                dc = WaveShaperKernel.shape(0.0, copy) != 0.0;
            }
            curve = copy;
            constantOutput = dc;
        }
        final boolean pin = dc;
        // 0 을 0 이 아닌 값으로 옮기는 곡선이면 입력이 없어도 처리하도록 컨텍스트가 붙잡게 하고, 아니면 놓아 준다.
        getContext().post(new Runnable() {
            public void run() {
                if (pin) {
                    getContext().pin(WaveShaperNode.this);
                } else {
                    getContext().unpin(WaveShaperNode.this);
                }
            }
        });
    }

    /**
     * Returns the oversampling type.
     * <p>
     * 오버샘플링 종류를 반환한다.
     *
     * @return the type / 종류
     */
    public OverSampleType getOversample() {
        return oversample;
    }

    /**
     * Sets the oversampling type.
     * <p>
     * 오버샘플링 종류를 설정한다.
     *
     * @param type the type / 종류
     * @throws AudioException <code>TypeError</code> if the type is null / 종류가 null 이면 <code>TypeError</code>
     */
    public void setOversample(OverSampleType type) {
        if (type == null) {
            throw new AudioException(AudioException.Kind.TYPE, "oversample is null");
        }
        oversample = type;
    }

    /**
     * Returns the number of frames that the output stays non-silent after the input became silent: the tail of the filters of the
     * oversampling, 0 without oversampling.
     * <p>
     * 입력이 무음이 된 뒤 출력이 무음이 아닌 프레임 수를 반환한다: 오버샘플링 필터의 꼬리. 오버샘플링이 없으면 0.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    @Override
    long tailFrames() {
        OverSampleType t = oversample;
        if (t == OverSampleType.NONE || curve == null) {
            return 0;
        }
        return WaveShaperKernel.tailFramesOf(t.getFactor());
    }

    /**
     * Tells whether the curve makes the node produce a constant signal without input.
     * <p>
     * 곡선이 입력 없이 노드가 일정한 신호를 만들게 하는지 알려 준다.
     *
     * @return true if the node is always active / 항상 활성이면 true
     */
    @Override
    boolean alwaysActive() {
        return constantOutput;
    }

    /**
     * Clears the memory of the filters.
     * <p>
     * 필터의 기억을 지운다.
     */
    @Override
    void resetState() {
        // 모든 채널의 필터 기억을 지운다.
        for (int c = 0; c < kernels.length; c++) {
            kernels[c].reset();
        }
    }

    /**
     * Shapes the input.
     * <p>
     * 입력을 변환한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        int channels = in.getNumberOfChannels();
        out.setNumberOfChannels(channels);
        float[] c = curve;
        if (c == null) {
            // 곡선이 없으면 입력이 그대로 지나가고 필터의 기억은 쓰지 않으므로 지운다.
            for (int ch = 0; ch < channels; ch++) {
                System.arraycopy(in.channel(ch), 0, out.channel(ch), 0, frames);
            }
            if (usedChannels > 0) {
                resetState();
                usedChannels = 0;
            }
            return;
        }
        int factor = oversample.getFactor();
        // 오버샘플링 배수가 바뀌었거나 채널이 늘었으면 해당 채널의 필터를 새로 만든다.
        if (kernels.length < channels || (kernels.length > 0 && kernels[0].getFactor() != factor)) {
            WaveShaperKernel[] next = new WaveShaperKernel[Math.max(channels, kernels.length)];
            // 배수가 그대로인 기존 필터는 상태를 이어 쓴다.
            for (int ch = 0; ch < next.length; ch++) {
                boolean keep = ch < kernels.length && kernels[ch].getFactor() == factor;
                next[ch] = keep ? kernels[ch] : new WaveShaperKernel(factor);
            }
            kernels = next;
        }
        // 이번 quantum 에 쓰지 않는 채널의 필터는 기억을 지워 다음에 낡은 소리가 섞이지 않게 한다.
        for (int ch = channels; ch < usedChannels; ch++) {
            kernels[ch].reset();
        }
        usedChannels = channels;
        // 채널마다 곡선을 적용한다.
        for (int ch = 0; ch < channels; ch++) {
            kernels[ch].process(in.channel(ch), out.channel(ch), frames, c);
        }
    }
}
