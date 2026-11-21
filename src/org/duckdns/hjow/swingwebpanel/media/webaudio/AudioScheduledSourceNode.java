package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The base of the source nodes that are started and stopped at a time: {@link OscillatorNode}, {@link ConstantSourceNode} and
 * {@link AudioBufferSourceNode} (Web Audio API 1.7). Before <code>start</code> and after <code>stop</code> the node outputs silence. The
 * start and the stop time are used exactly (not rounded to a sample frame): a sample frame is played when its time is at or after the
 * start time and before the stop time. A time of 0, or one in the past, means "at once". The engine keeps a started node alive until it
 * has ended; when it ends the <code>ended</code> listener is called through the task sink of the context.
 * <p>
 * 정해진 시간에 시작하고 멈추는 원천 노드의 기반: {@link OscillatorNode}, {@link ConstantSourceNode}, {@link AudioBufferSourceNode}(Web Audio API 1.7).
 * <code>start</code> 전과 <code>stop</code> 후에 노드는 무음을 출력한다. 시작 시간과 정지 시간은 정확히(샘플 프레임으로 반올림하지 않고) 쓴다: 샘플
 * 프레임은 그 시간이 시작 시간 이후이고 정지 시간 이전일 때 재생된다. 시간이 0 이거나 과거이면 "즉시"이다. 엔진은 시작된 노드를 끝날 때까지 붙잡고, 끝나면
 * 컨텍스트의 작업 싱크를 통해 <code>ended</code> 리스너를 부른다.
 */
public abstract class AudioScheduledSourceNode extends AudioNode {

    /**
     * True after <code>start</code> was called (<code>[[source started]]</code>).
     * <p>
     * <code>start</code> 를 부른 뒤 true(<code>[[source started]]</code>).
     */
    private volatile boolean started;

    /**
     * The start time in seconds.
     * <p>
     * 시작 시간(초).
     */
    private volatile double startTime;

    /**
     * The first frame that is played.
     * <p>
     * 재생하는 첫 프레임.
     */
    private volatile long startFrame = Long.MAX_VALUE;

    /**
     * The stop time in seconds, infinite if <code>stop</code> was not called.
     * <p>
     * 정지 시간(초). <code>stop</code> 을 부르지 않았으면 무한대.
     */
    private volatile double stopTime = Double.POSITIVE_INFINITY;

    /**
     * The first frame that is not played any more.
     * <p>
     * 더 이상 재생하지 않는 첫 프레임.
     */
    private volatile long stopFrame = Long.MAX_VALUE;

    /**
     * The listener of the <code>ended</code> event.
     * <p>
     * <code>ended</code> 이벤트의 리스너.
     */
    private volatile Runnable endedListener;

    /**
     * True after the node ended (rendering thread).
     * <p>
     * 노드가 끝난 뒤 true(렌더링 스레드).
     */
    private boolean ended;

    /**
     * True if a subclass found that the playback is over (the end of a buffer); the node ends after the quantum.
     * <p>
     * 하위 클래스가 재생이 끝났음을 알았으면 true(버퍼의 끝). 노드는 quantum 이 끝난 뒤 끝난다.
     */
    private boolean finishRequested;

    /**
     * The index of the first frame of the quantum that is rendered (set by {@link #sourceActive(long, int)}).
     * <p>
     * 렌더링 중인 quantum 의 첫 프레임 인덱스({@link #sourceActive(long, int)} 가 정한다).
     */
    private long quantumStart;

    /**
     * Creates a source node with no input and one output.
     * <p>
     * 입력이 없고 출력이 하나인 원천 노드를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    AudioScheduledSourceNode(BaseAudioContext context) {
        super(context, 0, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
    }

    /**
     * Returns the first frame at or after a time.
     * <p>
     * 시간 이후(같은 시간 포함)의 첫 프레임을 반환한다.
     *
     * @param time       the time in seconds / 시간(초)
     * @param sampleRate the sample rate / 샘플링 주파수
     * @return the frame index (the maximum long for a time that is too large) / 프레임 인덱스(너무 큰 시간이면 long 최댓값)
     */
    static long firstFrameAtOrAfter(double time, double sampleRate) {
        if (time <= 0) {
            return 0;
        }
        double estimate = time * sampleRate;
        if (!(estimate < 9.0e18)) {
            return Long.MAX_VALUE;
        }
        long f = (long) Math.ceil(estimate);
        // 부동소수점 오차를 바로잡는다: 한 프레임 앞이 이미 시간 이후이면 앞으로, 이 프레임이 시간 이전이면 뒤로 옮긴다.
        while (f > 0 && (f - 1) / sampleRate >= time) {
            f--;
        }
        while (f / sampleRate < time) {
            f++;
        }
        return f;
    }

    /**
     * Sets the listener that is called when the node has ended (the <code>ended</code> event); the listener runs through the task sink
     * of the context.
     * <p>
     * 노드가 끝났을 때(<code>ended</code> 이벤트) 불리는 리스너를 정한다. 리스너는 컨텍스트의 작업 싱크를 통해 실행된다.
     *
     * @param listener the listener, or null / 리스너. 없으면 null
     */
    public final void setEndedListener(Runnable listener) {
        this.endedListener = listener;
    }

    /**
     * Tells whether <code>start</code> was called.
     * <p>
     * <code>start</code> 를 불렀는지 알려 준다.
     *
     * @return true after start / start 후이면 true
     */
    public final boolean isStarted() {
        return started;
    }

    /**
     * Schedules the node to play from a time.
     * <p>
     * 노드가 시간부터 재생하도록 예약한다.
     *
     * @param when the start time in seconds; 0 or a time in the past starts at once / 시작 시간(초). 0 이나 과거이면 즉시 시작한다
     * @throws AudioException <code>InvalidStateError</code> if the node was started before, <code>RangeError</code> for a negative time / 이미 시작했으면
     *                        <code>InvalidStateError</code>, 시간이 음수이면 <code>RangeError</code>
     */
    public void start(double when) {
        beginStart(when);
    }

    /**
     * Checks the state and the time, then marks the node as started and queues the control message that keeps the node alive.
     * <p>
     * 상태와 시간을 검사한 뒤 노드를 시작됨으로 표시하고 노드를 살려 두는 제어 메시지를 쌓는다.
     *
     * @param when the start time in seconds / 시작 시간(초)
     */
    final void beginStart(double when) {
        checkNotStarted();
        checkTime(when, "when");
        commitStart(when);
    }

    /**
     * Throws an <code>InvalidStateError</code> if the node was started already.
     * <p>
     * 노드가 이미 시작했으면 <code>InvalidStateError</code> 를 던진다.
     */
    final void checkNotStarted() {
        if (started) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "start() was called more than once");
        }
    }

    /**
     * Throws a <code>TypeError</code> for a time that is not finite and a <code>RangeError</code> for a negative time.
     * <p>
     * 유한하지 않은 시간에는 <code>TypeError</code>, 음수 시간에는 <code>RangeError</code> 를 던진다.
     *
     * @param when the time / 시간
     * @param what the name of the argument / 인수 이름
     */
    static void checkTime(double when, String what) {
        if (Double.isNaN(when) || Double.isInfinite(when)) {
            throw new AudioException(AudioException.Kind.TYPE, what + " is not a finite number");
        }
        if (when < 0) {
            throw new AudioException(AudioException.Kind.RANGE, what + " " + when + " is negative");
        }
    }

    /**
     * Marks the node as started at a time (the arguments were checked already).
     * <p>
     * 노드를 시간에 시작됨으로 표시한다(인수는 이미 검사했다).
     *
     * @param when the start time / 시작 시간
     */
    final void commitStart(double when) {
        startTime = when;
        startFrame = firstFrameAtOrAfter(when, getContext().getSampleRate());
        started = true;
        getContext().post(new Runnable() {
            public void run() {
                getContext().pin(AudioScheduledSourceNode.this);
            }
        });
    }

    /**
     * Schedules the node to stop at a time. A later call replaces the stop time unless the node has ended.
     * <p>
     * 노드가 시간에 멈추도록 예약한다. 노드가 끝나지 않았다면 나중의 호출이 정지 시간을 바꾼다.
     *
     * @param when the stop time in seconds; 0 or a time in the past stops at once / 정지 시간(초). 0 이나 과거이면 즉시 멈춘다
     * @throws AudioException <code>InvalidStateError</code> if the node was not started, <code>RangeError</code> for a negative time / 시작하지 않았으면
     *                        <code>InvalidStateError</code>, 시간이 음수이면 <code>RangeError</code>
     */
    public final void stop(double when) {
        if (!started) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "stop() was called before start()");
        }
        checkTime(when, "when");
        stopTime = when;
        stopFrame = firstFrameAtOrAfter(when, getContext().getSampleRate());
    }

    /**
     * Returns the start time in seconds.
     * <p>
     * 시작 시간(초)을 반환한다.
     *
     * @return the start time / 시작 시간
     */
    final double getStartTime() {
        return startTime;
    }

    /**
     * Returns the stop time in seconds (infinite if not set).
     * <p>
     * 정지 시간(초)을 반환한다(정하지 않았으면 무한대).
     *
     * @return the stop time / 정지 시간
     */
    final double getStopTime() {
        return stopTime;
    }

    /**
     * Returns the first frame of the quantum in which the node plays, as an index in the quantum (the quantum size if it does not play
     * yet).
     * <p>
     * 노드가 재생하는 첫 프레임을 quantum 안의 인덱스로 반환한다(아직 재생하지 않으면 quantum 크기).
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the quantum size / quantum 크기
     * @return the index in the quantum / quantum 안의 인덱스
     */
    final int firstPlayFrame(long frame0, int frames) {
        long s = startFrame - frame0;
        return s <= 0 ? 0 : (s >= frames ? frames : (int) s);
    }

    /**
     * Returns the first frame of the quantum in which the node does not play any more, as an index in the quantum (the quantum size if
     * it has not stopped).
     * <p>
     * 노드가 더 이상 재생하지 않는 첫 프레임을 quantum 안의 인덱스로 반환한다(멈추지 않았으면 quantum 크기).
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the quantum size / quantum 크기
     * @return the index in the quantum / quantum 안의 인덱스
     */
    final int endPlayFrame(long frame0, int frames) {
        long e = stopFrame - frame0;
        return e <= 0 ? 0 : (e >= frames ? frames : (int) e);
    }

    /**
     * Returns the index of the first frame of the quantum that is rendered.
     * <p>
     * 렌더링 중인 quantum 의 첫 프레임 인덱스를 반환한다.
     *
     * @return the frame index / 프레임 인덱스
     */
    final long getQuantumStart() {
        return quantumStart;
    }

    /**
     * Asks the node to end after the quantum (the playback is over).
     * <p>
     * quantum 이 끝난 뒤 노드가 끝나도록 요청한다(재생이 끝났다).
     */
    final void requestFinish() {
        finishRequested = true;
    }

    /**
     * Tells whether the node produces audio of its own; a scheduled source does.
     * <p>
     * 노드가 스스로 오디오를 만드는지 알려 준다. 예약 원천은 그렇다.
     *
     * @return true / true
     */
    @Override
    final boolean isSource() {
        return true;
    }

    /**
     * Tells whether the source plays in at least a part of the quantum.
     * <p>
     * 원천이 quantum 의 일부라도 재생하는지 알려 준다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @return true if the source plays / 원천이 재생하면 true
     */
    @Override
    boolean sourceActive(long frame0, int frames) {
        quantumStart = frame0;
        if (ended || !started) {
            return false;
        }
        return firstPlayFrame(frame0, frames) < endPlayFrame(frame0, frames);
    }

    /**
     * Ends the node when its stop time was reached in the quantum or the playback is over: the node is released by the engine and the
     * <code>ended</code> listener is queued.
     * <p>
     * 정지 시간이 이 quantum 에 이르렀거나 재생이 끝났으면 노드를 끝낸다: 엔진이 노드를 놓아 주고 <code>ended</code> 리스너를 쌓는다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param active true if the node was active / 노드가 활성이었으면 true
     */
    @Override
    final void afterRender(long frame0, int frames, boolean active) {
        if (ended || !started) {
            return;
        }
        if (finishRequested || stopFrame <= frame0 + frames) {
            ended = true;
            getContext().unpin(this);
            Runnable listener = endedListener;
            if (listener != null) {
                getContext().postTask(listener);
            }
        }
    }
}
