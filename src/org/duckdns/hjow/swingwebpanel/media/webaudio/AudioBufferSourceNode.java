package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A source that plays an {@link AudioBuffer} (Web Audio API 1.9). The playback follows the algorithm of section 1.9.6: the playhead
 * starts at <code>offset</code> and advances by the computed playback rate <code>playbackRate * 2^(detune / 1200)</code> (both k-rate;
 * the sample rate of the buffer is taken into account) for every output frame; between two sample frames the signal is interpolated
 * linearly. With <code>loop</code> the region from <code>loopStart</code> to <code>loopEnd</code> is repeated (the endpoints are in
 * seconds of the buffer; invalid endpoints mean the whole buffer), in the direction of the sign of the rate. A <code>duration</code> given
 * to <code>start</code> limits the content that is played, in seconds of the buffer and independent of the rate. The node ends when the
 * buffer has been played to its end (without loop), the duration has been reached, or the stop time has been reached. The output has the
 * channels of the buffer, and is a single channel of silence while the node does not play. The loop endpoints are fractional frame
 * positions; a fraction between two frame positions at the splice of the loop is interpolated between the neighbouring frames of the
 * region (exact for endpoints on whole frames).
 * <p>
 * {@link AudioBuffer} 를 재생하는 원천(Web Audio API 1.9). 재생은 1.9.6절의 알고리즘을 따른다: 재생 헤드는 <code>offset</code> 에서 시작해 출력 프레임마다
 * 계산된 재생 속도 <code>playbackRate * 2^(detune / 1200)</code> (둘 다 k-rate, 버퍼의 샘플링 주파수를 반영)만큼 나아가며, 두 샘플 프레임 사이의 신호는 선형
 * 보간한다. <code>loop</code> 이면 <code>loopStart</code> 에서 <code>loopEnd</code> 까지의 구간을 속도 부호의 방향으로 반복한다(끝점은 버퍼의 초 단위이며
 * 올바르지 않은 끝점은 버퍼 전체를 뜻한다). <code>start</code> 에 준 <code>duration</code> 은 재생할 내용을 제한하며 버퍼의 초 단위이고 속도와 무관하다.
 * 노드는 버퍼를 끝까지 재생했거나(loop 가 아닐 때) duration 에 이르렀거나 정지 시간에 이르면 끝난다. 출력은 버퍼의 채널을 가지며 노드가 재생하지 않는
 * 동안은 무음 채널 하나이다. 루프 끝점은 소수 프레임 위치이며, 루프 이음매에서 두 프레임 위치 사이의 소수는 구간의 이웃 프레임 사이로 보간한다(끝점이
 * 정수 프레임이면 정확하다).
 */
public final class AudioBufferSourceNode extends AudioScheduledSourceNode {

    /**
     * The arguments of <code>start</code> that the rendering thread needs, published as one object.
     * <p>
     * 렌더링 스레드가 필요로 하는 <code>start</code> 인수를 하나의 객체로 공개한 것.
     */
    private static final class Playback {

        /**
         * The playhead position where playback begins, in seconds of the buffer.
         * <p>
         * 재생이 시작되는 재생 헤드 위치(버퍼의 초 단위).
         */
        final double offset;

        /**
         * The duration of the content to play, in seconds of the buffer.
         * <p>
         * 재생할 내용의 길이(버퍼의 초 단위).
         */
        final double duration;

        /**
         * True if a duration was given.
         * <p>
         * duration 을 주었으면 true.
         */
        final boolean hasDuration;

        /**
         * Creates the arguments.
         * <p>
         * 인수를 만든다.
         *
         * @param offset      the offset / offset
         * @param duration    the duration / duration
         * @param hasDuration true if a duration was given / duration 을 주었으면 true
         */
        Playback(double offset, double duration, boolean hasDuration) {
            this.offset = offset;
            this.duration = duration;
            this.hasDuration = hasDuration;
        }
    }

    /**
     * The speed of playback (k-rate, default 1, unlimited nominal range).
     * <p>
     * 재생 속도(k-rate, 기본 1, 명목 범위 제한 없음).
     */
    private final AudioParam playbackRate;

    /**
     * The detune in cents (k-rate, default 0, unlimited nominal range).
     * <p>
     * 디튠(센트, k-rate, 기본 0, 명목 범위 제한 없음).
     */
    private final AudioParam detune;

    /**
     * The buffer that is played.
     * <p>
     * 재생하는 버퍼.
     */
    private volatile AudioBuffer buffer;

    /**
     * True after a buffer was set (a buffer can be set once; <code>[[buffer set]]</code>).
     * <p>
     * 버퍼를 정한 뒤 true(버퍼는 한 번만 정할 수 있다. <code>[[buffer set]]</code>).
     */
    private boolean bufferSet;

    /**
     * True if the region from {@link #loopStart} to {@link #loopEnd} is repeated.
     * <p>
     * {@link #loopStart} 에서 {@link #loopEnd} 까지의 구간을 반복하면 true.
     */
    private volatile boolean loop;

    /**
     * The start of the loop in seconds of the buffer.
     * <p>
     * 루프의 시작(버퍼의 초 단위).
     */
    private volatile double loopStart;

    /**
     * The end of the loop in seconds of the buffer; 0 or less, or more than the duration of the buffer, means the end of the buffer.
     * <p>
     * 루프의 끝(버퍼의 초 단위). 0 이하이거나 버퍼 길이보다 크면 버퍼의 끝을 뜻한다.
     */
    private volatile double loopEnd;

    /**
     * The arguments of <code>start</code>.
     * <p>
     * <code>start</code> 인수.
     */
    private volatile Playback playback = new Playback(0, 0, false);

    /**
     * The playhead in frames of the buffer (rendering thread).
     * <p>
     * 버퍼 프레임 단위의 재생 헤드(렌더링 스레드).
     */
    private double position;

    /**
     * True after the playhead was set to the start (rendering thread).
     * <p>
     * 재생 헤드를 시작 위치로 정한 뒤 true(렌더링 스레드).
     */
    private boolean playStarted;

    /**
     * True after the playhead entered the loop (rendering thread).
     * <p>
     * 재생 헤드가 루프에 들어간 뒤 true(렌더링 스레드).
     */
    private boolean enteredLoop;

    /**
     * The offset after the adjustment for a loop, in frames (rendering thread).
     * <p>
     * 루프에 맞게 조정한 뒤의 offset(프레임, 렌더링 스레드).
     */
    private double adjustedOffset;

    /**
     * The content that was played, in frames of the buffer (rendering thread).
     * <p>
     * 재생한 내용의 길이(버퍼 프레임 단위, 렌더링 스레드).
     */
    private double elapsed;

    /**
     * Creates a source without a buffer.
     * <p>
     * 버퍼가 없는 원천을 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public AudioBufferSourceNode(BaseAudioContext context) {
        super(context);
        playbackRate = registerParam(new AudioParam(context, this, "playbackRate", 1f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.K_RATE, true));
        detune = registerParam(new AudioParam(context, this, "detune", 0f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.K_RATE, true));
    }

    /**
     * Returns the <code>playbackRate</code> parameter.
     * <p>
     * <code>playbackRate</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getPlaybackRate() {
        return playbackRate;
    }

    /**
     * Returns the <code>detune</code> parameter.
     * <p>
     * <code>detune</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getDetune() {
        return detune;
    }

    /**
     * Returns the buffer.
     * <p>
     * 버퍼를 반환한다.
     *
     * @return the buffer, or null / 버퍼. 없으면 null
     */
    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Sets the buffer. A buffer can be set once; setting null is always allowed.
     * <p>
     * 버퍼를 정한다. 버퍼는 한 번만 정할 수 있으며 null 은 언제나 정할 수 있다.
     *
     * @param newBuffer the buffer or null / 버퍼 또는 null
     * @throws AudioException <code>InvalidStateError</code> if a buffer was set before / 이미 버퍼를 정했으면 <code>InvalidStateError</code>
     */
    public synchronized void setBuffer(AudioBuffer newBuffer) {
        if (newBuffer != null) {
            if (bufferSet) {
                throw new AudioException(AudioException.Kind.INVALID_STATE, "the buffer of an AudioBufferSourceNode can be set once");
            }
            bufferSet = true;
        }
        buffer = newBuffer;
    }

    /**
     * Replaces the content that the node plays by an acquired copy (Web Audio API 1.4.3, "acquire the content"). The script layer owns the rule that a
     * script can set its <code>buffer</code> once and keeps its own array that the script may change; whenever the content is acquired (when
     * <code>start</code> is called, or when the buffer is set after that) it hands a snapshot of the data to this method, so the node plays the data of
     * that moment even if the script changes the array later. Unlike {@link #setBuffer} it can be called again and has no once-only rule. The snapshot
     * must not be changed by the caller afterwards (the rendering thread reads it); share it only with other readers.
     * <p>
     * 노드가 재생하는 내용을 가져온(acquire) 복사본으로 바꾼다(Web Audio API 1.4.3, "acquire the content"). 스크립트가 <code>buffer</code> 를 한 번만 정할 수 있다는
     * 규칙과 스크립트가 바꿀 수 있는 자기 배열은 스크립트 계층이 맡는다. 내용을 가져오는 순간(<code>start</code> 를 부를 때, 또는 그 뒤에 버퍼를 정할 때) 그 순간의
     * 데이터 스냅샷을 이 메서드에 넘기므로, 스크립트가 나중에 배열을 바꿔도 노드는 그 순간의 데이터를 재생한다. {@link #setBuffer} 와 달리 다시 부를 수 있고 한 번만
     * 정한다는 규칙이 없다. 호출자는 스냅샷을 나중에 바꾸면 안 된다(렌더링 스레드가 읽는다). 읽기만 하는 다른 쪽과만 공유한다.
     *
     * @param snapshot the content to play, or null for none / 재생할 내용. 없으면 null
     */
    public synchronized void acquireBuffer(AudioBuffer snapshot) {
        if (snapshot != null) {
            bufferSet = true;
        }
        buffer = snapshot;
    }

    /**
     * Tells whether the region is repeated.
     * <p>
     * 구간을 반복하는지 알려 준다.
     *
     * @return true if looping / 반복하면 true
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * Sets whether the region is repeated.
     * <p>
     * 구간을 반복할지 정한다.
     *
     * @param value true to loop / 반복하려면 true
     */
    public void setLoop(boolean value) {
        loop = value;
    }

    /**
     * Returns the start of the loop in seconds.
     * <p>
     * 루프의 시작(초)을 반환한다.
     *
     * @return the loop start / 루프 시작
     */
    public double getLoopStart() {
        return loopStart;
    }

    /**
     * Sets the start of the loop in seconds.
     * <p>
     * 루프의 시작(초)을 정한다.
     *
     * @param value the loop start / 루프 시작
     */
    public void setLoopStart(double value) {
        checkFinite(value, "loopStart");
        loopStart = value;
    }

    /**
     * Returns the end of the loop in seconds.
     * <p>
     * 루프의 끝(초)을 반환한다.
     *
     * @return the loop end / 루프 끝
     */
    public double getLoopEnd() {
        return loopEnd;
    }

    /**
     * Sets the end of the loop in seconds.
     * <p>
     * 루프의 끝(초)을 정한다.
     *
     * @param value the loop end / 루프 끝
     */
    public void setLoopEnd(double value) {
        checkFinite(value, "loopEnd");
        loopEnd = value;
    }

    /**
     * Throws a <code>TypeError</code> for a number that is not finite.
     * <p>
     * 유한하지 않은 수에는 <code>TypeError</code> 를 던진다.
     *
     * @param v    the number / 수
     * @param what the name of the argument / 인수 이름
     */
    private static void checkFinite(double v, String what) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new AudioException(AudioException.Kind.TYPE, what + " is not a finite number");
        }
    }

    /**
     * Schedules the whole buffer to play from a time.
     * <p>
     * 버퍼 전체가 시간부터 재생되도록 예약한다.
     *
     * @param when the start time in seconds / 시작 시간(초)
     */
    @Override
    public void start(double when) {
        begin(when, 0.0, 0.0, false);
    }

    /**
     * Schedules the buffer to play from a time, beginning at an offset.
     * <p>
     * 버퍼가 시간부터 offset 에서 시작해 재생되도록 예약한다.
     *
     * @param when   the start time in seconds / 시작 시간(초)
     * @param offset the playhead position where playback begins, in seconds of the buffer / 재생이 시작되는 재생 헤드 위치(버퍼의 초 단위)
     */
    public void start(double when, double offset) {
        begin(when, offset, 0.0, false);
    }

    /**
     * Schedules the buffer to play from a time, beginning at an offset, for a duration of content.
     * <p>
     * 버퍼가 시간부터 offset 에서 시작해 duration 만큼의 내용을 재생되도록 예약한다.
     *
     * @param when     the start time in seconds / 시작 시간(초)
     * @param offset   the playhead position where playback begins, in seconds of the buffer / 재생이 시작되는 재생 헤드 위치(버퍼의 초 단위)
     * @param duration the duration of the content to play in seconds of the buffer, independent of the rate / 재생할 내용의 길이(버퍼의 초 단위, 속도와 무관)
     * @throws AudioException <code>InvalidStateError</code> if the node was started, <code>RangeError</code> for a negative argument / 이미 시작했으면
     *                        <code>InvalidStateError</code>, 인수가 음수이면 <code>RangeError</code>
     */
    public void start(double when, double offset, double duration) {
        begin(when, offset, duration, true);
    }

    /**
     * Checks the arguments of <code>start</code> and starts the node.
     * <p>
     * <code>start</code> 의 인수를 검사하고 노드를 시작한다.
     *
     * @param when        the start time / 시작 시간
     * @param offset      the offset / offset
     * @param duration    the duration / duration
     * @param hasDuration true if the duration was given / duration 을 주었으면 true
     */
    private void begin(double when, double offset, double duration, boolean hasDuration) {
        checkNotStarted();
        checkTime(when, "when");
        checkFinite(offset, "offset");
        if (offset < 0) {
            throw new AudioException(AudioException.Kind.RANGE, "offset " + offset + " is negative");
        }
        if (hasDuration) {
            checkFinite(duration, "duration");
            if (duration < 0) {
                throw new AudioException(AudioException.Kind.RANGE, "duration " + duration + " is negative");
            }
        }
        playback = new Playback(offset, duration, hasDuration);
        commitStart(when);
    }

    /**
     * A buffer source plays only if it has a buffer.
     * <p>
     * 버퍼 원천은 버퍼가 있을 때만 재생한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @return true if the source plays / 원천이 재생하면 true
     */
    @Override
    boolean sourceActive(long frame0, int frames) {
        return super.sourceActive(frame0, frames) && buffer != null;
    }

    /**
     * Plays the buffer in the frames of the quantum in which the node plays.
     * <p>
     * 노드가 재생하는 quantum 의 프레임에서 버퍼를 재생한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBuffer buf = buffer;
        AudioBus out = outputBus(0);
        int channels = buf.getNumberOfChannels();
        out.setNumberOfChannels(channels);
        out.zero();
        long frame0 = getQuantumStart();
        int first = firstPlayFrame(frame0, frames);
        int end = endPlayFrame(frame0, frames);
        double bufRate = buf.getSampleRate();
        double rate = playbackRate.valueAt(0) * Math.pow(2.0, detune.valueAt(0) / 1200.0);
        double step = rate * bufRate / getContext().getSampleRate();
        int length = buf.getLength();
        double lengthF = length;
        boolean lp = loop;
        double loopFrom = 0.0;
        double loopTo = lengthF;
        if (lp) {
            double ls = loopStart;
            double le = loopEnd;
            // 루프 끝점이 올바르면 그 구간을, 아니면 버퍼 전체를 반복한다.
            if (ls >= 0 && le > 0 && ls < le) {
                loopFrom = ls * bufRate;
                loopTo = Math.min(le * bufRate, lengthF);
            }
            if (!(loopTo > loopFrom)) {
                loopFrom = 0.0;
                loopTo = lengthF;
            }
        }
        Playback pb = playback;
        double offsetF = Math.max(0.0, Math.min(pb.offset * bufRate, lengthF));
        double durationF = pb.duration * bufRate;
        // 재생하는 프레임마다 재생 헤드의 값을 보간해 쓰고 재생 헤드를 나아가게 한다.
        for (int i = first; i < end; i++) {
            if (!playStarted) {
                double p = offsetF;
                if (lp && step >= 0 && p >= loopTo) {
                    p = loopTo;
                }
                if (step < 0 && lp && p < loopFrom) {
                    p = loopFrom;
                }
                adjustedOffset = p;
                position = p;
                playStarted = true;
            }
            if (pb.hasDuration && elapsed >= durationF) {
                requestFinish();
                break;
            }
            if (lp) {
                if (!enteredLoop) {
                    if (adjustedOffset < loopTo && position >= loopFrom) {
                        enteredLoop = true;
                    }
                    if (adjustedOffset >= loopTo && position < loopTo) {
                        enteredLoop = true;
                    }
                }
                if (enteredLoop && (position >= loopTo || position < loopFrom)) {
                    double len = loopTo - loopFrom;
                    position = loopFrom + ((position - loopFrom) % len + len) % len;
                }
            } else {
                enteredLoop = false;
            }
            if (position >= 0 && position < lengthF) {
                int i0 = (int) position;
                double frac = position - i0;
                int i1 = i0 + 1;
                if (i1 >= length || (lp && enteredLoop && i1 >= loopTo)) {
                    // 이음매의 다음 프레임: 루프이면 루프 시작 쪽 프레임, 아니면 마지막 프레임을 그대로 쓴다.
                    i1 = (lp && enteredLoop) ? (int) (loopFrom + (i1 - loopTo)) : i0;
                    if (i1 < 0 || i1 >= length) {
                        i1 = i0;
                    }
                }
                // 채널마다 두 프레임 사이를 선형 보간한다.
                for (int c = 0; c < channels; c++) {
                    float[] d = buf.getChannelData(c);
                    float s0 = d[i0];
                    out.channel(c)[i] = frac == 0.0 ? s0 : (float) (s0 + (d[i1] - s0) * frac);
                }
            }
            position += step;
            elapsed += step;
            if (!lp && ((step > 0 && position >= lengthF) || (step < 0 && position < 0))) {
                requestFinish();
                break;
            }
        }
    }
}
