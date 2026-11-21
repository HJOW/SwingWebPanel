package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * Delays its input by the value of <code>delayTime</code> seconds (Web Audio API 1.18): <code>output(t) = input(t - delayTime(t))</code>.
 * <code>delayTime</code> is an a-rate parameter from 0 to <code>maxDelayTime</code>; a fractional delay interpolates linearly between
 * the two neighbouring samples. The node has a tail time of <code>maxDelayTime</code>, and the number of channels of its output equals
 * the number of channels of its input (a change of the channel count shows when the delayed samples reach the output; the samples that
 * are already in the delay line are mixed to the prevailing layout).
 * <p>
 * A delay node can be part of a cycle of the graph (a feedback loop). The rendering order then splits it into a reader step that reads
 * the delay line first, like a source, and a writer step that writes the delay line after its inputs; the <code>delayTime</code> is
 * limited to at least one render quantum, because the reader cannot see what the writer writes in the same quantum. A delay node in a
 * cycle is active only while its output is not silent (the magnitude of a sample is at least 2^-126) or it is within its tail time.
 * <p>
 * 입력을 <code>delayTime</code> 초만큼 지연시킨다(Web Audio API 1.18): <code>output(t) = input(t - delayTime(t))</code>. <code>delayTime</code> 은 0 에서
 * <code>maxDelayTime</code> 까지의 a-rate 파라미터이며 소수 지연은 이웃한 두 샘플 사이를 선형 보간한다. 노드의 꼬리 시간은 <code>maxDelayTime</code> 이고
 * 출력의 채널 수는 입력의 채널 수와 같다(채널 수 변화는 지연된 샘플이 출력에 닿을 때 나타나며, 이미 지연선에 있는 샘플은 그때의 배치로 섞는다).
 * <p>
 * 지연 노드는 그래프의 순환(피드백 루프)의 일부일 수 있다. 렌더링 순서는 그때 노드를 원천처럼 지연선을 먼저 읽는 읽기 단계와 입력 뒤에 지연선에 쓰는 쓰기
 * 단계로 나눈다. 읽기 단계는 같은 quantum 에서 쓰기 단계가 쓰는 것을 볼 수 없으므로 <code>delayTime</code> 은 최소 render quantum 하나로 제한된다.
 * 순환 안의 지연 노드는 출력이 무음이 아닌 동안(샘플 크기가 2^-126 이상) 또는 꼬리 시간 안에서만 활성이다.
 */
public final class DelayNode extends AudioNode {

    /**
     * The delay in seconds (a-rate, default 0, from 0 to <code>maxDelayTime</code>).
     * <p>
     * 지연(초, a-rate, 기본 0, 0 ~ <code>maxDelayTime</code>).
     */
    private final AudioParam delayTime;

    /**
     * The largest delay in seconds.
     * <p>
     * 가장 긴 지연(초).
     */
    private final double maxDelayTime;

    /**
     * The largest delay in frames.
     * <p>
     * 가장 긴 지연(프레임).
     */
    private final int maxDelayFrames;

    /**
     * The length of the circular delay line in frames: the longest delay plus a quantum of samples that are written but not read yet.
     * <p>
     * 순환 지연선의 길이(프레임): 가장 긴 지연에 아직 읽지 않은 한 quantum 분량의 쓴 샘플을 더한 값.
     */
    private final int lineLength;

    /**
     * The delay line, one array per channel (rendering thread).
     * <p>
     * 지연선. 채널마다 배열 하나(렌더링 스레드).
     */
    private float[][] line;

    /**
     * The index in the delay line where the sample of the next frame is written (rendering thread).
     * <p>
     * 다음 프레임의 샘플을 쓸 지연선의 인덱스(렌더링 스레드).
     */
    private int writeIndex;

    /**
     * The first frame at which the channels of the delay line may be reduced again (rendering thread).
     * <p>
     * 지연선의 채널을 다시 줄여도 되는 첫 프레임(렌더링 스레드).
     */
    private long narrowFrom;

    /**
     * True if the node breaks a cycle of the graph (set by the rendering order).
     * <p>
     * 노드가 그래프의 순환을 끊으면 true(렌더링 순서가 정한다).
     */
    private boolean cycleBreaker;

    /**
     * The mix of the input to the channel layout of the delay line (rendering thread).
     * <p>
     * 지연선의 채널 배치에 맞춘 입력의 믹스(렌더링 스레드).
     */
    private AudioBus mixed;

    /**
     * Creates a delay node.
     * <p>
     * 지연 노드를 만든다.
     *
     * @param context      the context / 컨텍스트
     * @param maxDelayTime the largest delay in seconds, greater than 0 and less than 3 minutes / 가장 긴 지연(초). 0 보다 크고 3분 미만
     * @throws AudioException <code>NotSupportedError</code> if the maximum delay is outside the range / 가장 긴 지연이 범위 밖이면
     *                        <code>NotSupportedError</code>
     */
    public DelayNode(BaseAudioContext context, double maxDelayTime) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (!(maxDelayTime > 0 && maxDelayTime < WebAudioLimits.MAX_DELAY_SECONDS)) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "maxDelayTime " + maxDelayTime + " is not between 0 and " + WebAudioLimits.MAX_DELAY_SECONDS);
        }
        this.maxDelayTime = maxDelayTime;
        this.maxDelayFrames = (int) Math.ceil(maxDelayTime * context.getSampleRate());
        this.lineLength = maxDelayFrames + context.getRenderQuantumSize() + 2;
        if ((long) lineLength > WebAudioLimits.MAX_BUFFER_SAMPLES) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the delay line is too long");
        }
        this.delayTime = registerParam(new AudioParam(context, this, "delayTime", 0f, 0f, (float) maxDelayTime, AutomationRate.A_RATE, false));
    }

    /**
     * Returns the <code>delayTime</code> parameter.
     * <p>
     * <code>delayTime</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getDelayTime() {
        return delayTime;
    }

    /**
     * Returns the largest delay in seconds.
     * <p>
     * 가장 긴 지연(초)을 반환한다.
     *
     * @return the largest delay / 가장 긴 지연
     */
    public double getMaxDelayTime() {
        return maxDelayTime;
    }

    /**
     * Marks the node as breaking a cycle (called by the rendering order).
     * <p>
     * 노드가 순환을 끊는다고 표시한다(렌더링 순서가 부른다).
     *
     * @param value true if the node breaks a cycle / 순환을 끊으면 true
     */
    void setCycleBreaker(boolean value) {
        cycleBreaker = value;
    }

    /**
     * Tells whether the node breaks a cycle (for tests).
     * <p>
     * 노드가 순환을 끊는지 알려 준다(시험용).
     *
     * @return true if it does / 끊으면 true
     */
    boolean isCycleBreaker() {
        return cycleBreaker;
    }

    /**
     * The tail time: the longest delay plus a quantum.
     * <p>
     * 꼬리 시간: 가장 긴 지연에 한 quantum 을 더한 값.
     *
     * @return the tail time in frames / 꼬리 시간(프레임)
     */
    @Override
    long tailFrames() {
        return lineLength;
    }

    /**
     * Clears the delay line when the node becomes inactive: its content is older than the tail time and silent anyway.
     * <p>
     * 노드가 비활성이 되면 지연선을 지운다. 내용은 꼬리 시간보다 오래되어 어차피 무음이다.
     */
    @Override
    void resetState() {
        line = null;
        mixed = null;
        writeIndex = 0;
    }

    /**
     * Makes sure that the delay line has at least the channels of the input: new channels are zero, the content of the line is mixed
     * to a larger layout when the input has more channels, and the line is reduced again when the wider input is not in it any more.
     * <p>
     * 지연선이 입력의 채널 수 이상을 갖게 한다: 새 채널은 0 이고, 입력의 채널이 더 많아지면 지연선의 내용을 더 큰 배치로 섞고, 더 넓은 입력이 지연선에 더
     * 없으면 다시 줄인다.
     *
     * @param inChannels the number of channels of the input / 입력의 채널 수
     * @param frame0     the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames     the quantum size / quantum 크기
     */
    private void fitChannels(int inChannels, long frame0, int frames) {
        if (line == null) {
            line = new float[inChannels][lineLength];
            writeIndex = 0;
            narrowFrom = frame0 + frames + lineLength;
            mixed = new AudioBus(inChannels, frames);
            return;
        }
        int have = line.length;
        if (inChannels > have || (inChannels < have && frame0 >= narrowFrom)) {
            // 채널 수가 바뀌면 지연선의 내용을 새 배치로 섞어 옮긴다.
            AudioBus old = new AudioBus(have, lineLength);
            for (int c = 0; c < have; c++) {
                System.arraycopy(line[c], 0, old.channel(c), 0, lineLength);
            }
            AudioBus next = new AudioBus(inChannels, lineLength);
            ChannelMixing.mix(old, next, getChannelInterpretation(), false);
            line = new float[inChannels][];
            // 새 배치의 채널 배열로 바꾼다.
            for (int c = 0; c < inChannels; c++) {
                line[c] = next.channel(c);
            }
            mixed = new AudioBus(inChannels, frames);
        }
        if (inChannels >= line.length) {
            narrowFrom = frame0 + frames + lineLength;
        }
    }

    /**
     * Writes the input of the quantum into the delay line.
     * <p>
     * quantum 의 입력을 지연선에 쓴다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the quantum size / quantum 크기
     */
    private void write(long frame0, int frames) {
        AudioBus in = inputBus(0);
        fitChannels(in.getNumberOfChannels(), frame0, frames);
        AudioBus src = in;
        if (in.getNumberOfChannels() != line.length) {
            // 지연선이 더 넓으면 입력을 지연선의 배치로 섞는다.
            mixed.setNumberOfChannels(line.length);
            ChannelMixing.mix(in, mixed, getChannelInterpretation(), false);
            src = mixed;
        }
        // 채널마다 프레임을 원형 버퍼에 쓴다.
        for (int c = 0; c < line.length; c++) {
            float[] from = src.channel(c);
            float[] to = line[c];
            int w = writeIndex;
            // 프레임을 원형 버퍼에 쓴다. 끝에 닿으면 처음으로 돌아간다.
            for (int i = 0; i < frames; i++) {
                to[w] = from[i];
                if (++w == lineLength) {
                    w = 0;
                }
            }
        }
        writeIndex = (writeIndex + frames) % lineLength;
    }

    /**
     * Reads the delayed samples of the quantum from the delay line. The samples of this quantum have been written already when the node is
     * rendered in one step; in the reader step of a cycle they are not, and the delay is at least one quantum.
     * <p>
     * quantum 의 지연된 샘플을 지연선에서 읽는다. 노드가 한 단계로 렌더링되면 이 quantum 의 샘플은 이미 써져 있고, 순환의 읽기 단계에서는 아직 써지지 않았으며
     * 지연은 최소 한 quantum 이다.
     *
     * @param frames       the quantum size / quantum 크기
     * @param writtenAlready true if the samples of this quantum were written before / 이 quantum 의 샘플을 먼저 썼으면 true
     */
    private void read(int frames, boolean writtenAlready) {
        AudioBus out = outputBus(0);
        int channels = line.length;
        out.setNumberOfChannels(channels);
        // 이 quantum 의 첫 프레임에 해당하는 지연선 위치.
        int base = writtenAlready ? (writeIndex - frames + lineLength) % lineLength : writeIndex;
        double minDelay = cycleBreaker ? getContext().getRenderQuantumSize() : 0.0;
        double sr = getContext().getSampleRate();
        // 프레임마다 지연 프레임 수를 구해 두 샘플 사이를 선형 보간한다.
        for (int i = 0; i < frames; i++) {
            double d = delayTime.valueAt(i) * sr;
            if (d != d) {
                d = 0.0;
            }
            d = Math.max(minDelay, Math.min(maxDelayFrames, d));
            double pos = (double) (base + i) - d;
            double floor = Math.floor(pos);
            double frac = pos - floor;
            int i0 = (int) (((long) floor % lineLength + lineLength) % lineLength);
            int i1 = i0 + 1 == lineLength ? 0 : i0 + 1;
            // 채널마다 보간한 샘플을 출력한다.
            for (int c = 0; c < channels; c++) {
                float[] l = line[c];
                out.channel(c)[i] = frac == 0.0 ? l[i0] : (float) (l[i0] + (l[i1] - l[i0]) * frac);
            }
        }
    }

    /**
     * Writes the input and reads the delayed output in one step.
     * <p>
     * 입력을 쓰고 지연된 출력을 한 단계로 읽는다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        write(renderFrame0, frames);
        read(frames, true);
    }

    /**
     * Renders one step of the node when it breaks a cycle: the reader step or the writer step.
     * <p>
     * 노드가 순환을 끊을 때 한 단계를 렌더링한다: 읽기 단계 또는 쓰기 단계.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param serial the render serial of the quantum / quantum 의 렌더링 일련번호
     * @param phase  the phase / 단계
     */
    @Override
    void renderSplit(long frame0, int frames, long serial, int phase) {
        if (phase == PHASE_READ) {
            // 읽기 단계: 파라미터를 계산하고, 꼬리 시간 안이면 지연선을 읽는다. 출력은 소리가 있을 때만 활성이다.
            computeParams(frame0, frames);
            boolean active = frame0 < lastInputActiveEnd + tailFrames() && line != null;
            stampOutputs(serial, false);
            if (active) {
                read(frames, false);
                float threshold = WebAudioLimits.SILENCE_THRESHOLD;
                boolean loud = false;
                AudioBus out = outputBus(0);
                // 출력에 무음이 아닌 샘플이 하나라도 있는지 본다.
                for (int c = 0; c < out.getNumberOfChannels() && !loud; c++) {
                    float[] s = out.channel(c);
                    // 프레임마다 문턱 이상인 샘플이 있는지 본다.
                    for (int i = 0; i < frames; i++) {
                        if (Math.abs(s[i]) >= threshold) {
                            loud = true;
                            break;
                        }
                    }
                }
                outputs[0].active = loud;
            } else {
                outputBus(0).makeSilentMono();
            }
        } else {
            // 쓰기 단계: 입력을 끌어와 활성이면 지연선에 쓴다.
            boolean inputActive = false;
            inputs[0].pull(serial);
            inputActive = inputs[0].anyActive;
            if (inputActive) {
                lastInputActiveEnd = frame0 + frames;
            }
            if (inputActive || (line != null && frame0 < lastInputActiveEnd + tailFrames())) {
                write(frame0, frames);
                getContext().updatePin(this, true);
            } else {
                if (line != null) {
                    resetState();
                }
                getContext().updatePin(this, false);
            }
        }
    }
}
