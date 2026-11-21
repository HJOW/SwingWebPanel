package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.ConvolutionKernel;
import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.PartitionedConvolver;

/**
 * A linear convolution effect given an impulse response (Web Audio API 1.17), as used for reverbs. The impulse response is an
 * {@link AudioBuffer} of 1, 2 or 4 channels at the sample rate of the context; the input has one or two channels. The convolution has no
 * latency (see {@link PartitionedConvolver}) and the node keeps producing output for the length of the response after the input became
 * silent.
 * <p>
 * Channels: the output is mono only for a mono input and a mono response; in every other case it is stereo. A mono response is applied to
 * every input channel; a stereo response with a mono input gives the left output from the first response channel and the right output
 * from the second; with a stereo input every channel is convolved with its own response channel; a four channel response is the "true
 * stereo" matrix: the left output is <code>L * h0 + R * h2</code> and the right output is <code>L * h1 + R * h3</code> (a mono input is
 * used as both <code>L</code> and <code>R</code>).
 * <p>
 * With <code>normalize</code> true (the default) the response is scaled when the buffer is set so that the loudness of convolutions with
 * different responses is alike: the scale of the specification is <code>0.00125 / rms * 44100 / sampleRate</code> (halved for four
 * channels), where <code>rms</code> is the root mean square over all samples of the response, at least 0.000125.
 * <p>
 * 임펄스 응답으로 하는 선형 합성곱 효과(Web Audio API 1.17). 리버브에 쓴다. 임펄스 응답은 컨텍스트와 같은 샘플링 주파수의 채널 1, 2, 4개인
 * {@link AudioBuffer} 이고 입력은 채널이 하나나 둘이다. 합성곱은 지연이 없고({@link PartitionedConvolver} 참고) 입력이 무음이 된 뒤에도 응답의 길이만큼
 * 출력을 계속 만든다.
 * <p>
 * 채널: 출력이 모노인 것은 모노 입력과 모노 응답일 때뿐이고 그 밖에는 스테레오이다. 모노 응답은 모든 입력 채널에 적용된다. 모노 입력에 스테레오 응답이면
 * 왼쪽 출력은 첫 응답 채널, 오른쪽 출력은 둘째 응답 채널에서 나온다. 스테레오 입력이면 채널마다 자기 응답 채널과 합성곱한다. 채널 넷인 응답은 "진짜
 * 스테레오" 행렬이다: 왼쪽 출력은 <code>L * h0 + R * h2</code>, 오른쪽 출력은 <code>L * h1 + R * h3</code> 이다(모노 입력은 <code>L</code> 과
 * <code>R</code> 둘 다로 쓴다).
 * <p>
 * <code>normalize</code> 가 true(기본)이면 서로 다른 응답으로 한 합성곱의 음량이 비슷해지도록 버퍼를 설정할 때 응답의 크기를 맞춘다. 규격의 배율은
 * <code>0.00125 / rms * 44100 / sampleRate</code>(채널이 넷이면 절반)이며, <code>rms</code> 는 응답 모든 샘플의 제곱 평균 제곱근(최소 0.000125)이다.
 */
public final class ConvolverNode extends AudioNode {

    /**
     * The gain calibration of the normalization (the specification: <code>GainCalibration</code>).
     * <p>
     * 정규화의 이득 보정값(규격의 <code>GainCalibration</code>).
     */
    private static final double GAIN_CALIBRATION = 0.00125;

    /**
     * The sample rate that the calibration refers to.
     * <p>
     * 보정값이 기준으로 삼는 샘플링 주파수.
     */
    private static final double GAIN_CALIBRATION_SAMPLE_RATE = 44100.0;

    /**
     * The smallest power that is used by the normalization.
     * <p>
     * 정규화에 쓰는 최소 파워.
     */
    private static final double MIN_POWER = 0.000125;

    /**
     * The processing state of one impulse response, created when the buffer is set.
     * <p>
     * 임펄스 응답 하나의 처리 상태. 버퍼를 설정할 때 만든다.
     */
    private static final class Engine {

        /**
         * The number of channels of the impulse response.
         * <p>
         * 임펄스 응답의 채널 수.
         */
        final int responseChannels;

        /**
         * The length of the impulse response in frames.
         * <p>
         * 임펄스 응답의 프레임 수.
         */
        final int length;

        /**
         * The convolvers: two for a response of one or two channels, four for a response of four channels.
         * <p>
         * 컨볼버: 응답이 1, 2 채널이면 둘, 4 채널이면 넷.
         */
        final PartitionedConvolver[] convolvers;

        /**
         * The temporary output for the second virtual source of the true stereo convolution.
         * <p>
         * 진짜 스테레오 합성곱의 두 번째 가상 원천을 위한 임시 출력.
         */
        final float[][] temp;

        /**
         * True while the second convolver of a mono response holds the state of a stereo input.
         * <p>
         * 모노 응답의 두 번째 컨볼버가 스테레오 입력의 상태를 가지고 있는 동안 true.
         */
        boolean secondUsed;

        /**
         * Creates the engine.
         * <p>
         * 엔진을 만든다.
         *
         * @param kernels the kernels of the response channels / 응답 채널의 커널
         * @param length  the length in frames / 길이(프레임)
         * @param quantum the render quantum size / render quantum 크기
         */
        Engine(ConvolutionKernel[] kernels, int length, int quantum) {
            this.responseChannels = kernels.length;
            this.length = length;
            int n = Math.max(kernels.length, 2);
            this.convolvers = new PartitionedConvolver[n];
            // 응답 채널마다 컨볼버를 만든다. 모노 응답은 같은 커널로 둘째 컨볼버를 만든다.
            for (int i = 0; i < n; i++) {
                convolvers[i] = new PartitionedConvolver(kernels[Math.min(i, kernels.length - 1)]);
            }
            this.temp = new float[2][quantum];
        }

        /**
         * Clears the state of every convolver.
         * <p>
         * 모든 컨볼버의 상태를 지운다.
         */
        void reset() {
            // 컨볼버마다 상태를 지운다.
            for (int i = 0; i < convolvers.length; i++) {
                convolvers[i].reset();
            }
            secondUsed = false;
        }
    }

    /**
     * The impulse response that was set, or null.
     * <p>
     * 설정한 임펄스 응답. 없으면 null.
     */
    private AudioBuffer buffer;

    /**
     * Whether the response is normalized when the buffer is set.
     * <p>
     * 버퍼를 설정할 때 응답을 정규화하는지.
     */
    private volatile boolean normalize = true;

    /**
     * The state of the response that is convolved (read by the rendering thread).
     * <p>
     * 합성곱하는 응답의 상태(렌더링 스레드가 읽는다).
     */
    private volatile Engine engine;

    /**
     * Creates a convolver without an impulse response (it outputs silence).
     * <p>
     * 임펄스 응답이 없는 컨볼버를 만든다(무음을 출력한다).
     *
     * @param context the context / 컨텍스트
     */
    public ConvolverNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
    }

    /**
     * Returns the impulse response that was set, or null.
     * <p>
     * 설정한 임펄스 응답을 반환한다. 없으면 null.
     *
     * @return the buffer / 버퍼
     */
    public synchronized AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Sets the impulse response. The content of the buffer is acquired now: later changes of the buffer have no effect, and the
     * state of the convolution starts empty. The <code>normalize</code> flag at this moment decides whether the response is scaled.
     * Setting null makes the node output silence.
     * <p>
     * 임펄스 응답을 설정한다. 버퍼의 내용은 지금 가져간다. 이후 버퍼를 바꿔도 영향이 없고 합성곱의 상태는 빈 채로 시작한다. 이 순간의
     * <code>normalize</code> 가 응답의 크기를 맞출지 정한다. null 을 설정하면 노드가 무음을 출력한다.
     *
     * @param newBuffer the response, or null / 응답. 없으면 null
     * @throws AudioException <code>NotSupportedError</code> if the buffer does not have 1, 2 or 4 channels, its sample rate differs from
     *                        the one of the context, it is longer than {@link ConvolutionKernel#MAX_LENGTH}, or the render quantum
     *                        size of the context is not a power of two / 버퍼의 채널 수가 1, 2, 4 가 아니거나, 샘플링 주파수가 컨텍스트와 다르거나,
     *                        {@link ConvolutionKernel#MAX_LENGTH} 보다 길거나, 컨텍스트의 render quantum 크기가 2 의 거듭제곱이 아니면
     *                        <code>NotSupportedError</code>
     */
    public void setBuffer(AudioBuffer newBuffer) {
        Engine next = null;
        if (newBuffer != null) {
            int channels = newBuffer.getNumberOfChannels();
            if (channels != 1 && channels != 2 && channels != 4) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the impulse response has " + channels + " channels, not 1, 2 or 4");
            }
            if (newBuffer.getSampleRate() != getContext().getSampleRate()) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the sample rate of the impulse response is not the one of the context");
            }
            int quantum = getContext().getRenderQuantumSize();
            if ((quantum & (quantum - 1)) != 0) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "a ConvolverNode needs a render quantum size that is a power of two");
            }
            int length = newBuffer.getLength();
            if (length > ConvolutionKernel.MAX_LENGTH) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the impulse response is longer than " + ConvolutionKernel.MAX_LENGTH + " frames");
            }
            double scale = normalize ? normalizationScale(newBuffer) : 1.0;
            ConvolutionKernel[] kernels = new ConvolutionKernel[channels];
            // 채널마다 응답의 스펙트럼을 계산한다(이 순간의 내용을 가져간다).
            for (int c = 0; c < channels; c++) {
                kernels[c] = new ConvolutionKernel(newBuffer.getChannelData(c), length, scale, quantum);
            }
            next = new Engine(kernels, length, quantum);
        }
        synchronized (this) {
            buffer = newBuffer;
            engine = next;
        }
    }

    /**
     * Returns whether the response is normalized when the buffer is set.
     * <p>
     * 버퍼를 설정할 때 응답을 정규화하는지 반환한다.
     *
     * @return true if it is / 정규화하면 true
     */
    public boolean isNormalize() {
        return normalize;
    }

    /**
     * Sets whether the response is normalized. The change takes effect the next time the buffer is set.
     * <p>
     * 응답을 정규화할지 설정한다. 변경은 다음에 버퍼를 설정할 때 적용된다.
     *
     * @param value true to normalize / 정규화하려면 true
     */
    public void setNormalize(boolean value) {
        normalize = value;
    }

    /**
     * Computes the normalization scale of the specification (<code>calculateNormalizationScale</code>).
     * <p>
     * 규격의 정규화 배율(<code>calculateNormalizationScale</code>)을 계산한다.
     *
     * @param b the impulse response / 임펄스 응답
     * @return the scale / 배율
     */
    public static double normalizationScale(AudioBuffer b) {
        int channels = b.getNumberOfChannels();
        int length = b.getLength();
        double power = 0.0;
        // 모든 채널의 샘플 제곱을 더한다.
        for (int c = 0; c < channels; c++) {
            float[] data = b.getChannelData(c);
            double channelPower = 0.0;
            // 채널의 샘플 제곱을 더한다.
            for (int j = 0; j < length; j++) {
                double s = data[j];
                channelPower += s * s;
            }
            power += channelPower;
        }
        power = Math.sqrt(power / ((double) channels * length));
        // 무한대, NaN, 너무 작은 파워는 최솟값으로 막는다.
        if (Double.isInfinite(power) || Double.isNaN(power) || power < MIN_POWER) {
            power = MIN_POWER;
        }
        double scale = 1.0 / power;
        scale *= GAIN_CALIBRATION;
        if (b.getSampleRate() != 0f) {
            scale *= GAIN_CALIBRATION_SAMPLE_RATE / b.getSampleRate();
        }
        // 진짜 스테레오(채널 4개)는 두 가상 원천의 합이므로 절반으로 보정한다.
        if (channels == 4) {
            scale *= 0.5;
        }
        return scale;
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
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCount of a ConvolverNode is at most 2");
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
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCountMode of a ConvolverNode cannot be max");
        }
    }

    /**
     * Returns the tail time: the length of the impulse response, 0 without one.
     * <p>
     * 꼬리 시간을 반환한다: 임펄스 응답의 길이. 응답이 없으면 0.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    @Override
    long tailFrames() {
        Engine e = engine;
        return e == null ? 0 : e.length;
    }

    /**
     * Clears the state of the convolution.
     * <p>
     * 합성곱의 상태를 지운다.
     */
    @Override
    void resetState() {
        Engine e = engine;
        if (e != null) {
            e.reset();
        }
    }

    /**
     * Convolves the input.
     * <p>
     * 입력을 합성곱한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        Engine e = engine;
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        if (e == null) {
            // 응답이 없으면 무음 채널 하나를 출력한다.
            out.makeSilentMono();
            return;
        }
        int inputChannels = in.getNumberOfChannels();
        float[] left = in.channel(0);
        float[] right = inputChannels >= 2 ? in.channel(1) : left;
        PartitionedConvolver[] cv = e.convolvers;
        if (e.responseChannels == 1) {
            if (inputChannels == 1) {
                // 모노 입력과 모노 응답은 모노 출력이다. 둘째 컨볼버에 남은 상태는 버린다.
                out.setNumberOfChannels(1);
                if (e.secondUsed) {
                    cv[1].reset();
                    e.secondUsed = false;
                }
                cv[0].process(left, out.channel(0), frames);
            } else {
                // 스테레오 입력은 채널마다 같은 응답과 합성곱한다.
                out.setNumberOfChannels(2);
                cv[0].process(left, out.channel(0), frames);
                cv[1].process(right, out.channel(1), frames);
                e.secondUsed = true;
            }
            return;
        }
        out.setNumberOfChannels(2);
        if (e.responseChannels == 2) {
            // 스테레오 응답: 왼쪽 출력은 첫 채널, 오른쪽 출력은 둘째 채널과 합성곱한다(모노 입력은 양쪽에 같다).
            cv[0].process(left, out.channel(0), frames);
            cv[1].process(right, out.channel(1), frames);
            return;
        }
        // 진짜 스테레오: 왼쪽 가상 원천은 응답 0, 1 로, 오른쪽 가상 원천은 응답 2, 3 으로 합성곱해 더한다.
        cv[0].process(left, out.channel(0), frames);
        cv[1].process(left, out.channel(1), frames);
        cv[2].process(right, e.temp[0], frames);
        cv[3].process(right, e.temp[1], frames);
        float[] o0 = out.channel(0);
        float[] o1 = out.channel(1);
        // 두 가상 원천의 결과를 더한다.
        for (int i = 0; i < frames; i++) {
            o0[i] += e.temp[0][i];
            o1[i] += e.temp[1][i];
        }
    }
}
