package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A source that takes the audio of a media element into the graph (Web Audio API 1.22). The node has no input and one output with as
 * many channels as the media (one silent channel if the media has no audio). The audio comes from a {@link MediaElementPcmSource} that
 * the host implements on top of the player of the element; the node pulls one render quantum of the playing media at a time. While the
 * media is paused or has ended, or while nothing was delivered, the output is silent; if the media is from another origin without the
 * permission of the server ({@link MediaElementPcmSource#isOutputAllowed()} is false) the output is silent too and nothing is pulled.
 * <p>
 * If the sample rate of the media differs from the one of the context the samples are resampled with a linear interpolation (exact
 * for equal rates; for other rates the interpolation is a simple, cheap method that leaves some imaging and aliasing at high
 * frequencies).
 * <p>
 * The context keeps the node processed from its creation until {@link #close()} is called (the host calls it when the element goes
 * away), because the context cannot tell whether the page still refers to the node.
 * <p>
 * 미디어 요소의 오디오를 그래프로 가져오는 원천(Web Audio API 1.22). 노드는 입력이 없고 미디어와 같은 채널 수의 출력 하나를 가진다(미디어에 오디오가 없으면
 * 무음 채널 하나). 오디오는 호스트가 요소의 재생기 위에서 구현한 {@link MediaElementPcmSource} 에서 오며, 노드는 재생 중인 미디어를 render quantum 하나씩
 * 가져간다. 미디어가 일시 정지되었거나 끝났거나 아무것도 오지 않는 동안 출력은 무음이다. 미디어가 서버의 허가 없이 가져온 다른 오리진의 것이면
 * ({@link MediaElementPcmSource#isOutputAllowed()} 가 false) 출력은 역시 무음이고 아무것도 가져오지 않는다.
 * <p>
 * 미디어의 샘플링 주파수가 컨텍스트와 다르면 선형 보간으로 리샘플링한다(같은 주파수이면 정확하다. 다른 주파수에서는 단순하고 값싼 방법이라 높은 주파수에
 * 이미징과 앨리어싱이 조금 남는다).
 * <p>
 * 컨텍스트는 페이지가 노드를 아직 참조하는지 알 수 없으므로 노드를 만든 때부터 {@link #close()} 가 불릴 때까지 처리 대상으로 붙잡는다(호스트는 요소가 사라질 때
 * 부른다).
 */
public final class MediaElementAudioSourceNode extends AudioNode {

    /**
     * The supplier of the audio.
     * <p>
     * 오디오 공급자.
     */
    private final MediaElementPcmSource source;

    /**
     * True once {@link #close()} was called.
     * <p>
     * {@link #close()} 를 부른 뒤에는 true.
     */
    private volatile boolean closed;

    /**
     * The samples that were pulled for the quantum, one array per channel (rendering thread).
     * <p>
     * quantum 을 위해 가져온 샘플. 채널마다 배열 하나(렌더링 스레드).
     */
    private float[][] pulled = new float[0][];

    /**
     * The number of channels of the output of the quantum (rendering thread).
     * <p>
     * quantum 출력의 채널 수(렌더링 스레드).
     */
    private int outChannels = 1;

    /**
     * Whether the quantum has audio (rendering thread).
     * <p>
     * quantum 에 오디오가 있는지(렌더링 스레드).
     */
    private boolean hasAudio;

    /**
     * The source samples that wait for the resampler, one array per channel (rendering thread).
     * <p>
     * 리샘플러를 기다리는 원본 샘플. 채널마다 배열 하나(렌더링 스레드).
     */
    private float[][] fifo = new float[0][];

    /**
     * The work arrays into which the supplier delivers the samples that the resampler wants (rendering thread).
     * <p>
     * 공급자가 리샘플러가 원하는 샘플을 넣어 주는 작업 배열(렌더링 스레드).
     */
    private float[][] scratch = new float[0][];

    /**
     * Whether the previous quantum had audio: the quantum after the last delivered frames is still output, because the resampler holds
     * a few frames (rendering thread).
     * <p>
     * 이전 quantum 에 오디오가 있었는지. 리샘플러가 몇 프레임을 쥐고 있으므로 마지막으로 받은 프레임 다음 quantum 도 출력한다(렌더링 스레드).
     */
    private boolean wasActive;

    /**
     * The number of source frames in the queue (rendering thread).
     * <p>
     * 큐에 있는 원본 프레임 수(렌더링 스레드).
     */
    private int fifoCount;

    /**
     * The position of the next output frame in the source frames of the queue (rendering thread).
     * <p>
     * 다음 출력 프레임의 큐 안 원본 프레임 기준 위치(렌더링 스레드).
     */
    private double phase;

    /**
     * Creates the node and keeps it processed until {@link #close()}.
     * <p>
     * 노드를 만들고 {@link #close()} 까지 처리 대상으로 붙잡는다.
     *
     * @param context the context / 컨텍스트
     * @param source  the supplier of the audio of the media element / 미디어 요소의 오디오 공급자
     * @throws AudioException <code>TypeError</code> if the source is null / 공급자가 null 이면 <code>TypeError</code>
     */
    public MediaElementAudioSourceNode(BaseAudioContext context, MediaElementPcmSource source) {
        super(context, 0, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (source == null) {
            throw new AudioException(AudioException.Kind.TYPE, "the media element source is null");
        }
        this.source = source;
        // 컨텍스트가 처리 대상으로 붙잡도록 렌더링 스레드에 요청한다.
        context.post(new Runnable() {
            public void run() {
                if (!closed) {
                    getContext().pin(MediaElementAudioSourceNode.this);
                }
            }
        });
    }

    /**
     * Returns the supplier of the audio.
     * <p>
     * 오디오 공급자를 반환한다.
     *
     * @return the supplier / 공급자
     */
    public MediaElementPcmSource getMediaElementSource() {
        return source;
    }

    /**
     * Releases the node: the context stops processing it and the output is silent. The host calls it when the element goes away.
     * <p>
     * 노드를 놓아 준다: 컨텍스트가 더 처리하지 않고 출력은 무음이다. 호스트는 요소가 사라질 때 부른다.
     */
    public void close() {
        closed = true;
        getContext().post(new Runnable() {
            public void run() {
                getContext().unpin(MediaElementAudioSourceNode.this);
            }
        });
    }

    /**
     * Tells that the node produces audio by itself.
     * <p>
     * 노드가 스스로 오디오를 만든다고 알린다.
     *
     * @return true / true
     */
    @Override
    boolean isSource() {
        return true;
    }

    /**
     * Pulls the audio of the quantum from the media (resampled if needed) and tells whether there is any.
     * <p>
     * quantum 의 오디오를 미디어에서 (필요하면 리샘플링해) 가져오고 오디오가 있는지 알린다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @return true if the output has audio / 출력에 오디오가 있으면 true
     */
    @Override
    boolean sourceActive(long frame0, int frames) {
        hasAudio = false;
        int channels = source.getChannelCount();
        if (closed || channels <= 0 || !source.isOutputAllowed()) {
            outChannels = 1;
            // 막힌 출력이면 큐를 비워 다음에 낡은 소리가 섞이지 않게 한다.
            fifoCount = 0;
            phase = 0.0;
            wasActive = false;
            return false;
        }
        channels = Math.min(channels, WebAudioLimits.MAX_CHANNELS);
        outChannels = channels;
        ensureArrays(channels, frames);
        double ratio = source.getSampleRate() / (double) getContext().getSampleRate();
        int delivered;
        if (ratio == 1.0) {
            // 샘플링 주파수가 같으면 그대로 가져온다.
            delivered = clamp(source.pull(pulled, frames), frames);
            // 가져오지 못한 나머지 프레임은 무음으로 채운다.
            for (int c = 0; c < channels; c++) {
                java.util.Arrays.fill(pulled[c], delivered, frames, 0f);
            }
        } else {
            delivered = resample(channels, frames, ratio);
        }
        // 리샘플링 중이면 마지막으로 받은 프레임을 큐에서 비워 내도록 한 quantum 을 더 출력한다.
        hasAudio = delivered > 0 || (ratio != 1.0 && wasActive);
        wasActive = delivered > 0;
        return hasAudio;
    }

    /**
     * Limits a number of delivered frames to the valid range.
     * <p>
     * 받은 프레임 수를 유효한 범위로 제한한다.
     *
     * @param n   the number reported by the supplier / 공급자가 알린 수
     * @param max the number that was asked for / 요청한 수
     * @return the number, from 0 to max / 0 ~ max 의 수
     */
    private static int clamp(int n, int max) {
        return n < 0 ? 0 : (n > max ? max : n);
    }

    /**
     * Makes sure that the work arrays have room for a quantum.
     * <p>
     * 작업 배열에 quantum 이 들어갈 공간이 있게 한다.
     *
     * @param channels the number of channels / 채널 수
     * @param frames   the number of frames of the quantum / quantum 의 프레임 수
     */
    private void ensureArrays(int channels, int frames) {
        if (pulled.length != channels || pulled[0].length < frames) {
            pulled = new float[channels][frames];
        }
    }

    /**
     * Resamples the media to the rate of the context with a linear interpolation: pulls as many source frames as the output frames of
     * the quantum need, keeps the unused ones for the next quantum, and silences the frames that the source could not deliver.
     * <p>
     * 선형 보간으로 미디어를 컨텍스트의 속도로 리샘플링한다: quantum 의 출력 프레임이 필요로 하는 만큼 원본 프레임을 가져오고, 쓰지 않은 것은 다음 quantum 을
     * 위해 보관하며, 원본이 주지 못한 프레임은 무음으로 만든다.
     *
     * @param channels the number of channels / 채널 수
     * @param frames   the number of output frames / 출력 프레임 수
     * @param ratio    the number of source frames per output frame / 출력 프레임당 원본 프레임 수
     * @return the number of source frames that the source delivered in this call (0 means nothing is playing) / 이번 호출에서 원본이 준 프레임 수(0 이면 재생 중이
     *         아님)
     */
    private int resample(int channels, int frames, double ratio) {
        // 채널 수가 바뀌었으면 큐를 새로 시작한다.
        if (fifo.length != channels) {
            fifo = new float[channels][0];
            fifoCount = 0;
            phase = 0.0;
        }
        int needed = (int) Math.floor(phase + (frames - 1) * ratio) + 2;
        int want = Math.max(0, needed - fifoCount);
        if (fifo[0].length < needed + 1) {
            // 큐 배열이 모자라면 키우면서 남은 샘플을 옮긴다.
            float[][] bigger = new float[channels][needed + 65];
            for (int c = 0; c < channels; c++) {
                System.arraycopy(fifo[c], 0, bigger[c], 0, fifoCount);
            }
            fifo = bigger;
        }
        int delivered = 0;
        if (want > 0) {
            // 가져올 샘플을 담을 작업 배열을 준비하고(새로 할당하지 않고 재사용) 공급자에게서 가져온다.
            if (scratch.length != channels || scratch[0].length < want) {
                scratch = new float[channels][want + 64];
            }
            delivered = clamp(source.pull(scratch, want), want);
            // 받은 프레임을 큐 뒤에 덧붙이고 못 받은 나머지는 무음으로 채운다.
            for (int c = 0; c < channels; c++) {
                java.util.Arrays.fill(scratch[c], delivered, want, 0f);
                System.arraycopy(scratch[c], 0, fifo[c], fifoCount, want);
            }
            fifoCount += want;
        }
        // 출력 프레임마다 인접한 두 원본 프레임을 선형 보간한다.
        for (int c = 0; c < channels; c++) {
            float[] f = fifo[c];
            float[] o = pulled[c];
            // 채널의 출력 프레임마다 위치를 계산해 보간한다.
            for (int i = 0; i < frames; i++) {
                double p = phase + i * ratio;
                int k = (int) p;
                double frac = p - k;
                o[i] = (float) (f[k] * (1.0 - frac) + f[k + 1] * frac);
            }
        }
        phase += frames * ratio;
        int used = (int) phase;
        phase -= used;
        // 쓴 원본 프레임은 큐에서 버리고 나머지를 앞으로 당긴다.
        for (int c = 0; c < channels; c++) {
            System.arraycopy(fifo[c], used, fifo[c], 0, fifoCount - used);
        }
        fifoCount -= used;
        return delivered;
    }

    /**
     * Writes the pulled audio to the output.
     * <p>
     * 가져온 오디오를 출력에 쓴다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus out = outputBus(0);
        if (!hasAudio) {
            out.makeSilentMono();
            return;
        }
        out.setNumberOfChannels(outChannels);
        // 채널마다 가져온 샘플을 출력에 복사한다.
        for (int c = 0; c < outChannels; c++) {
            System.arraycopy(pulled[c], 0, out.channel(c), 0, frames);
        }
    }
}
