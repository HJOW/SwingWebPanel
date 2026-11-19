package org.duckdns.hjow.swingwebpanel.media.playback;

/**
 * Changes the speed of 16-bit PCM (plan item P18-08, <code>playbackRate</code>). Two methods are used. With <code>preservePitch</code>
 * (the default of the media elements) the sound gets shorter or longer but keeps its pitch: a waveform-similarity overlap-add (WSOLA) cuts
 * the input into overlapping windows of about 30 ms, moves every window a little so that it continues the previous one without a jump (the
 * place with the best correlation), and overlaps them with a raised cosine window. Without it the sound is resampled by linear
 * interpolation, so a higher speed also raises the pitch like a record that turns faster.
 * <p>
 * The converter is a stream: {@link #push(byte[], int, int)} gives it source blocks, {@link #read(byte[], int, int)} takes what is ready
 * (nothing at the start, because a window needs its neighbours), {@link #finish()} tells that the source ended so that the rest is
 * produced, and {@link #reset()} forgets everything after a seek. The speed may change at any time; a block that was produced already keeps
 * its speed. All work is done in {@link #push(byte[], int, int)} and {@link #finish()}, on the thread that calls them.
 * <p>
 * 16비트 PCM 의 속도를 바꾼다(계획 P18-08, <code>playbackRate</code>). 두 방법을 쓴다. <code>preservePitch</code>(미디어 요소의 기본값)이면
 * 소리가 짧아지거나 길어지지만 음높이는 유지된다: 파형 유사 중첩 가산(WSOLA)이 입력을 약 30 ms 의 겹치는 창으로 자르고, 각 창을 조금 움직여 앞의 창을
 * 끊김 없이 이어지게 하며(상관이 가장 좋은 곳), 코사인 창으로 겹친다. 아니면 선형 보간으로 리샘플링하므로 속도가 빨라지면 음높이도 올라간다(레코드를
 * 빨리 돌릴 때처럼).
 * <p>
 * 변환기는 스트림이다: {@link #push(byte[], int, int)} 가 소스 블록을 주고, {@link #read(byte[], int, int)} 가 준비된 것을 가져가며(시작할 때는 창이
 * 이웃을 필요로 하므로 아무것도 없다), {@link #finish()} 가 소스가 끝났음을 알려 나머지를 만들고, {@link #reset()} 이 탐색 뒤에 모든 것을 잊는다.
 * 속도는 언제든 바꿀 수 있고 이미 만든 블록은 그 속도를 유지한다. 모든 일은 {@link #push(byte[], int, int)} 와 {@link #finish()} 를 부르는 스레드에서 한다.
 */
final class RateConverter {

    /**
     * The slowest supported rate, a quarter of the normal speed.
     * <p>
     * 지원하는 가장 느린 속도(보통 속도의 4분의 1).
     */
    static final double MIN_RATE = 0.25;

    /**
     * The fastest supported rate, four times the normal speed.
     * <p>
     * 지원하는 가장 빠른 속도(보통 속도의 4배).
     */
    static final double MAX_RATE = 4.0;

    /**
     * Tells whether a rate can be played.
     * <p>
     * 속도를 재생할 수 있는지 알려 준다.
     *
     * @param rate the rate / 속도
     * @return true for a rate from {@link #MIN_RATE} to {@link #MAX_RATE} / {@link #MIN_RATE} 에서 {@link #MAX_RATE} 까지의 속도이면 true
     */
    static boolean isSupported(double rate) {
        return rate >= MIN_RATE && rate <= MAX_RATE;
    }

    /**
     * The number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * The length of an overlap-add window in frames (even).
     * <p>
     * 중첩 가산 창의 길이(프레임 수, 짝수).
     */
    private final int windowFrames;

    /**
     * The half of the window: the distance between two output windows.
     * <p>
     * 창의 절반: 출력 창 사이의 거리.
     */
    private final int hop;

    /**
     * How far from its nominal place a window may be moved to find the best continuation, in frames.
     * <p>
     * 가장 잘 이어지는 곳을 찾으려고 창을 원래 자리에서 옮길 수 있는 거리(프레임 수).
     */
    private final int search;

    /**
     * The raised cosine window (periodic, so that windows with half overlap add up to one).
     * <p>
     * 코사인 창(주기형이므로 절반이 겹치는 창을 더하면 1 이 된다).
     */
    private final float[] window;

    /**
     * The speed that new output uses.
     * <p>
     * 새 출력이 쓰는 속도.
     */
    private double rate = 1.0;

    /**
     * True to keep the pitch (WSOLA), false to resample.
     * <p>
     * 음높이를 유지하려면(WSOLA) true, 리샘플링하려면 false.
     */
    private boolean preservePitch = true;

    /**
     * The input frames that are still needed, interleaved; the first one has the absolute index {@link #base}.
     * <p>
     * 아직 필요한 입력 프레임(인터리브). 첫 프레임의 절대 번호가 {@link #base} 이다.
     */
    private short[] in = new short[0];

    /**
     * The number of valid input frames in {@link #in}.
     * <p>
     * {@link #in} 의 유효한 입력 프레임 수.
     */
    private int inFrames;

    /**
     * The absolute index of the first frame of {@link #in} (the number of frames that were dropped from its start).
     * <p>
     * {@link #in} 의 첫 프레임의 절대 번호(앞에서 버린 프레임 수).
     */
    private long base;

    /**
     * True after {@link #finish()}: no more input comes.
     * <p>
     * {@link #finish()} 뒤에는 true: 더 이상 입력이 오지 않는다.
     */
    private boolean finished;

    /**
     * The output that is ready, as little-endian 16-bit samples.
     * <p>
     * 준비된 출력(리틀 엔디언 16비트 샘플).
     */
    private byte[] out = new byte[0];

    /**
     * The number of ready bytes in {@link #out}.
     * <p>
     * {@link #out} 에서 준비된 바이트 수.
     */
    private int outBytes;

    /**
     * The absolute index of the start of the previous window (WSOLA), -1 before the first window.
     * <p>
     * 이전 창의 시작의 절대 번호(WSOLA). 첫 창 전에는 -1.
     */
    private long previousStart = -1;

    /**
     * The nominal absolute start of the next window (WSOLA).
     * <p>
     * 다음 창의 이론상 시작의 절대 번호(WSOLA).
     */
    private double nominal;

    /**
     * The second half of the previous window, already multiplied by the window and waiting to be added to the next one (WSOLA).
     * <p>
     * 이전 창의 두 번째 절반. 이미 창을 곱했고 다음 창에 더해지기를 기다린다(WSOLA).
     */
    private final float[] tail;

    /**
     * The absolute position of the next output sample in the input (resampling).
     * <p>
     * 입력에서의 다음 출력 샘플의 절대 위치(리샘플링).
     */
    private double position;

    /**
     * The number of output frames that were produced since the last reset.
     * <p>
     * 마지막 초기화 뒤에 만든 출력 프레임 수.
     */
    private long producedFrames;

    /**
     * The number of input frames that were pushed since the last reset.
     * <p>
     * 마지막 초기화 뒤에 넣은 입력 프레임 수.
     */
    private long pushedFrames;

    /**
     * Creates a converter.
     * <p>
     * 변환기를 만든다.
     *
     * @param sampleRate the frames per second of the sound / 소리의 초당 프레임 수
     * @param channels   the number of channels (1 or 2) / 채널 수(1 또는 2)
     */
    RateConverter(int sampleRate, int channels) {
        this.channels = channels;
        int w = (int) Math.round(sampleRate * 0.030);
        w = Math.max(64, w - (w % 2));
        this.windowFrames = w;
        this.hop = w / 2;
        this.search = Math.max(8, w / 4);
        this.window = new float[w];
        // 주기형 코사인 창: w[j] + w[j + w/2] = 1 이다.
        for (int j = 0; j < w; j++) {
            window[j] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * j / w));
        }
        this.tail = new float[hop * channels];
    }

    /**
     * Sets the speed and the method; it applies to the sound that is produced from now on.
     * <p>
     * 속도와 방법을 정한다. 지금부터 만드는 소리에 적용된다.
     *
     * @param newRate       the speed, from {@link #MIN_RATE} to {@link #MAX_RATE} / 속도. {@link #MIN_RATE} 에서 {@link #MAX_RATE}
     * @param keepPitch     true to keep the pitch / 음높이를 유지하려면 true
     */
    void setRate(double newRate, boolean keepPitch) {
        double r = Math.max(MIN_RATE, Math.min(MAX_RATE, newRate));
        // 방법이 바뀌면 지금 입력 위치에서 이어 가도록 새 방법의 상태를 맞춘다.
        if (keepPitch != preservePitch) {
            double next = preservePitch ? Math.max(nominal, base) : Math.max(position, base);
            preservePitch = keepPitch;
            previousStart = -1;
            position = next;
            nominal = next;
            clearTail();
        }
        rate = r;
    }

    /**
     * Returns the speed.
     * <p>
     * 속도를 반환한다.
     *
     * @return the speed / 속도
     */
    double getRate() {
        return rate;
    }

    /**
     * Forgets the input, the output and the state of the method (after a seek or a jump back to the start).
     * <p>
     * 입력, 출력, 방법의 상태를 잊는다(탐색이나 처음으로 되돌아간 뒤).
     */
    void reset() {
        inFrames = 0;
        base = 0;
        finished = false;
        outBytes = 0;
        previousStart = -1;
        nominal = 0;
        position = 0;
        producedFrames = 0;
        pushedFrames = 0;
        clearTail();
    }

    /**
     * Clears the part of the previous window that waits for overlapping.
     * <p>
     * 겹치기를 기다리는 이전 창의 부분을 지운다.
     */
    private void clearTail() {
        // 겹치기를 기다리는 값을 모두 0 으로 만든다.
        for (int i = 0; i < tail.length; i++) {
            tail[i] = 0;
        }
    }

    /**
     * Gives the converter a block of source sound and produces the output that this makes possible.
     * <p>
     * 소스 소리 블록을 변환기에 주고, 그로 인해 가능한 출력을 만든다.
     *
     * @param src    little-endian 16-bit interleaved samples / 리틀 엔디언 16비트 인터리브 샘플
     * @param offset the first byte / 첫 바이트
     * @param length the number of bytes (whole frames) / 바이트 수(프레임 단위)
     */
    void push(byte[] src, int offset, int length) {
        int frames = length / (channels * 2);
        ensureInput(inFrames + frames);
        int p = offset;
        int at = inFrames * channels;
        // 바이트를 16비트 샘플로 바꾸어 입력 버퍼 끝에 붙인다.
        for (int i = 0; i < frames * channels; i++) {
            in[at + i] = (short) ((src[p] & 0xff) | (src[p + 1] << 8));
            p += 2;
        }
        inFrames += frames;
        pushedFrames += frames;
        process();
    }

    /**
     * Tells that the source ended; the rest of the sound is produced.
     * <p>
     * 소스가 끝났음을 알린다. 나머지 소리를 만든다.
     */
    void finish() {
        if (finished) {
            return;
        }
        finished = true;
        process();
        // 마지막 창의 두 번째 절반(페이드 아웃)도 내보낸 뒤 기대 길이로 자른다.
        if (preservePitch && previousStart >= 0) {
            ensureOutput(hop);
            // 기다리던 절반의 샘플을 하나씩 쓴다.
            for (int i = 0; i < tail.length; i++) {
                putSample(Math.round(tail[i]));
            }
            producedFrames += hop;
            clearTail();
        }
        trimOutput();
    }

    /**
     * Cuts the output after the end so that its length is the length of the source divided by the speed.
     * <p>
     * 출력의 길이가 소스 길이를 속도로 나눈 값이 되도록 끝 뒤의 출력을 자른다.
     */
    private void trimOutput() {
        long wanted = (long) Math.ceil(pushedFrames / rate);
        long extra = producedFrames - wanted;
        // 만든 출력이 기대 길이보다 길면 남는 프레임을 잘라 낸다.
        if (extra > 0) {
            long cut = Math.min(extra, outBytes / (channels * 2));
            outBytes -= (int) cut * channels * 2;
            producedFrames -= cut;
        }
    }

    /**
     * Returns the number of bytes that {@link #read(byte[], int, int)} can deliver now.
     * <p>
     * {@link #read(byte[], int, int)} 가 지금 줄 수 있는 바이트 수를 반환한다.
     *
     * @return the ready bytes / 준비된 바이트 수
     */
    int available() {
        return outBytes;
    }

    /**
     * Tells whether the source ended and all the output was read.
     * <p>
     * 소스가 끝났고 모든 출력을 읽었는지 알려 준다.
     *
     * @return true if nothing more will come / 더 나올 것이 없으면 true
     */
    boolean isDrained() {
        return finished && outBytes == 0;
    }

    /**
     * Takes the output that is ready.
     * <p>
     * 준비된 출력을 가져간다.
     *
     * @param dst    the destination / 목적지
     * @param offset the first byte of the destination / 목적지의 첫 바이트
     * @param length the most bytes to take (rounded down to whole frames) / 가져갈 최대 바이트 수(프레임 단위로 내림)
     * @return the bytes taken, 0 if nothing is ready / 가져간 바이트 수. 준비된 것이 없으면 0
     */
    int read(byte[] dst, int offset, int length) {
        int frameBytes = channels * 2;
        int n = Math.min(outBytes, length - (length % frameBytes));
        if (n <= 0) {
            return 0;
        }
        System.arraycopy(out, 0, dst, offset, n);
        outBytes -= n;
        System.arraycopy(out, n, out, 0, outBytes);
        return n;
    }

    /**
     * Makes room for input frames.
     * <p>
     * 입력 프레임을 위한 자리를 만든다.
     *
     * @param frames the number of frames that the buffer must hold / 버퍼가 담아야 하는 프레임 수
     */
    private void ensureInput(int frames) {
        if (frames * channels > in.length) {
            short[] bigger = new short[Math.max(frames * channels, in.length * 2)];
            System.arraycopy(in, 0, bigger, 0, inFrames * channels);
            in = bigger;
        }
    }

    /**
     * Appends frames to the output.
     * <p>
     * 출력에 프레임을 덧붙인다.
     *
     * @param frames the number of frames that will be added / 더할 프레임 수
     */
    private void ensureOutput(int frames) {
        int need = outBytes + frames * channels * 2;
        if (need > out.length) {
            byte[] bigger = new byte[Math.max(need, out.length * 2)];
            System.arraycopy(out, 0, bigger, 0, outBytes);
            out = bigger;
        }
    }

    /**
     * Produces as much output as the input allows.
     * <p>
     * 입력이 허락하는 만큼 출력을 만든다.
     */
    private void process() {
        if (preservePitch) {
            overlapAdd();
        } else {
            resample();
        }
        dropOldInput();
    }

    /**
     * Drops the input frames that no later output needs.
     * <p>
     * 이후의 출력이 필요로 하지 않는 입력 프레임을 버린다.
     */
    private void dropOldInput() {
        long keep = preservePitch ? Math.min(previousStart < 0 ? 0 : previousStart + hop, (long) nominal - search) : (long) position - 1;
        long drop = keep - base;
        // 버릴 것이 충분히 쌓였을 때만 앞으로 당긴다.
        if (drop > 4096 && drop <= inFrames) {
            int d = (int) drop;
            System.arraycopy(in, d * channels, in, 0, (inFrames - d) * channels);
            inFrames -= d;
            base += d;
        }
    }

    /**
     * Returns a sample of the input.
     * <p>
     * 입력의 샘플 하나를 반환한다.
     *
     * @param frame   the absolute frame index; outside of the input it reads as silence / 절대 프레임 번호. 입력 밖은 무음으로 읽힌다
     * @param channel the channel / 채널
     * @return the sample / 샘플
     */
    private int sample(long frame, int channel) {
        long i = frame - base;
        if (i < 0 || i >= inFrames) {
            return 0;
        }
        return in[(int) i * channels + channel];
    }

    /**
     * Resampling by linear interpolation: the output takes the input at steps of the speed.
     * <p>
     * 선형 보간 리샘플링: 출력은 속도만큼의 간격으로 입력을 취한다.
     */
    private void resample() {
        long total = base + inFrames;
        // 보간에 필요한 다음 프레임까지 입력이 있는 동안 출력 프레임을 하나씩 만든다. 끝나면 마지막 프레임은 그대로 둔다.
        while (finished ? position < total - 1 + 1e-9 : position < total - 1) {
            long i0 = (long) Math.floor(position);
            double f = position - i0;
            ensureOutput(1);
            // 채널마다 두 프레임 사이를 보간한다.
            for (int c = 0; c < channels; c++) {
                double a = sample(i0, c);
                double b = i0 + 1 < total ? sample(i0 + 1, c) : a;
                int v = (int) Math.round(a + (b - a) * f);
                putSample(v);
            }
            producedFrames++;
            position += rate;
        }
    }

    /**
     * Writes one sample to the output.
     * <p>
     * 출력에 샘플 하나를 쓴다.
     *
     * @param value the sample (limited to 16 bits) / 샘플(16비트로 제한)
     */
    private void putSample(int value) {
        int v = Math.max(-32768, Math.min(32767, value));
        out[outBytes++] = (byte) v;
        out[outBytes++] = (byte) (v >> 8);
    }

    /**
     * Time scaling by overlap-add of windows that continue each other (WSOLA).
     * <p>
     * 서로 이어지는 창의 중첩 가산으로 하는 시간 조절(WSOLA).
     */
    private void overlapAdd() {
        long total = base + inFrames;
        // 창 하나를 놓을 만큼 입력이 있는 동안 창을 하나씩 더한다. 끝난 뒤에는 모자란 부분을 무음으로 채워 마지막 창까지 만든다.
        while (true) {
            long start;
            if (previousStart < 0) {
                start = Math.max(base, (long) Math.floor(nominal));
                if (!finished && total < start + windowFrames) {
                    return;
                }
                if (finished && total <= start) {
                    return;
                }
            } else {
                long lo = Math.max(previousStart + 1, (long) Math.floor(nominal) - search);
                long hi = (long) Math.floor(nominal) + search;
                if (!finished && total < hi + windowFrames) {
                    return;
                }
                if (finished && (long) Math.floor(nominal) >= pushedEnd()) {
                    return;
                }
                start = bestStart(lo, hi);
            }
            boolean first = previousStart < 0;
            addWindow(start);
            previousStart = start;
            nominal = (first ? start : nominal) + hop * rate;
        }
    }

    /**
     * Returns the absolute index after the last input frame that was pushed.
     * <p>
     * 지금까지 넣은 마지막 입력 프레임 다음의 절대 번호를 반환한다.
     *
     * @return the end of the input / 입력의 끝
     */
    private long pushedEnd() {
        return base + inFrames;
    }

    /**
     * Finds the window start whose beginning continues the previous window best: the one with the highest normalized correlation with the
     * natural continuation of the previous window.
     * <p>
     * 시작 부분이 이전 창을 가장 잘 이어 주는 창 시작을 찾는다: 이전 창의 자연스러운 연장과 정규화된 상관이 가장 높은 것.
     *
     * @param lo the lowest candidate / 가장 낮은 후보
     * @param hi the highest candidate / 가장 높은 후보
     * @return the best start / 가장 좋은 시작
     */
    private long bestStart(long lo, long hi) {
        long first = Math.max(lo, base);
        long best = Math.max(first, Math.min(hi, (long) Math.floor(nominal)));
        int span = (int) Math.max(0, hi - first) + hop + 1;
        float[] region = new float[span];
        // 후보가 훑는 구간의 채널 합을 한 번만 계산해 둔다.
        for (int i = 0; i < region.length; i++) {
            float sum = 0;
            // 프레임의 채널 값을 모두 더한다.
            for (int ch = 0; ch < channels; ch++) {
                sum += sample(first + i, ch);
            }
            region[i] = sum;
        }
        long target = previousStart + hop;
        float[] goal = new float[hop / 4 + 1];
        // 이전 창의 자연스러운 연장을 네 프레임마다 취해 둔다.
        for (int j = 0; j * 4 < hop; j++) {
            float sum = 0;
            // 프레임의 채널 값을 모두 더한다.
            for (int ch = 0; ch < channels; ch++) {
                sum += sample(target + j * 4L, ch);
            }
            goal[j] = sum;
        }
        double bestScore = -Double.MAX_VALUE;
        // 후보마다(두 프레임 간격) 연장과의 상관을 후보 에너지로 정규화해 가장 큰 것을 고른다.
        for (long c = first; c <= hi; c += 2) {
            int o = (int) (c - first);
            double dot = 0;
            double energy = 1e-6;
            // 겹치는 구간을 네 프레임마다 훑는다.
            for (int j = 0; j * 4 < hop; j++) {
                float b = region[o + j * 4];
                dot += goal[j] * b;
                energy += b * b;
            }
            double score = dot / Math.sqrt(energy);
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    /**
     * Adds the window that starts at an input position to the output: the first half is added to the waiting half of the previous window
     * and written, the second half waits for the next window.
     * <p>
     * 입력 위치에서 시작하는 창을 출력에 더한다: 첫 절반은 이전 창의 기다리던 절반에 더해 쓰고, 두 번째 절반은 다음 창을 기다린다.
     *
     * @param start the absolute index of the first frame of the window / 창의 첫 프레임의 절대 번호
     */
    private void addWindow(long start) {
        boolean first = previousStart < 0;
        ensureOutput(hop);
        // 첫 절반: 첫 창은 페이드 인 없이 그대로 쓰고, 그 뒤의 창은 기다리던 절반에 더한다.
        for (int j = 0; j < hop; j++) {
            for (int c = 0; c < channels; c++) {
                double s = sample(start + j, c);
                double v = first ? s : tail[j * channels + c] + s * window[j];
                putSample((int) Math.round(v));
            }
        }
        producedFrames += hop;
        // 두 번째 절반: 창을 곱해 다음 창을 기다리게 한다.
        for (int j = 0; j < hop; j++) {
            for (int c = 0; c < channels; c++) {
                tail[j * channels + c] = (float) (sample(start + hop + j, c) * window[hop + j]);
            }
        }
    }
}
