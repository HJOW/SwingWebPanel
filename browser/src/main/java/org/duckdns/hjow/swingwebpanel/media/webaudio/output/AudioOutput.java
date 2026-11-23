package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

/**
 * One open sound output of a real-time Web Audio context (plan item P25-07): a queue of float audio of a fixed sample rate and channel
 * count that a device (or a stand-in for it) plays at real speed. The rendering thread of the driver is the only caller of {@link #write},
 * {@link #start()}, {@link #flush()} and {@link #close()}; {@link #getQueuedFrames()} and {@link #stop()} may be called by any thread (the
 * control side stops the output to wake a rendering thread that waits in {@link #write}, which then returns at once). An output is
 * created in the stopped state: the frames that are written wait in its buffer until {@link #start()}, which lets the context fill the
 * buffer before the first sound is played. The sample rate of the data is the sample rate of the context; an implementation whose device
 * cannot play that rate converts it itself, so a context never depends on the device rate.
 * <p>
 * 실시간 Web Audio 컨텍스트의 열린 소리 출력 하나(계획 P25-07): 장치(또는 그 대역)가 실제 속도로 재생하는, 샘플링 주파수와 채널 수가 정해진 float 오디오의
 * 큐. {@link #write}, {@link #start()}, {@link #flush()}, {@link #close()} 는 드라이버의 렌더링 스레드만 부르고 {@link #getQueuedFrames()} 와
 * {@link #stop()} 은 어느 스레드에서나 부를 수 있다(제어 쪽이 {@link #write} 에서 기다리는 렌더링 스레드를 깨우려고 출력을 멈추며, 그러면 쓰기는 곧바로
 * 돌아온다). 출력은 멈춘 상태로 만들어진다: 쓴 프레임은 {@link #start()} 까지 버퍼에서 기다리므로 컨텍스트는 첫 소리가 재생되기 전에 버퍼를 채울 수 있다.
 * 데이터의 샘플링 주파수는 컨텍스트의 샘플링 주파수이다. 장치가 그 주파수를 재생할 수 없으면 구현이 직접 변환하므로 컨텍스트는 장치의 주파수에 의존하지 않는다.
 */
public interface AudioOutput {

    /**
     * Returns the sample rate of the data that {@link #write} accepts, which is the sample rate of the context that opened the output.
     * <p>
     * {@link #write} 가 받는 데이터의 샘플링 주파수를 반환한다. 이는 출력을 연 컨텍스트의 샘플링 주파수이다.
     *
     * @return the sample rate in Hz / 샘플링 주파수(Hz)
     */
    int getSampleRate();

    /**
     * Returns the number of channels that {@link #write} accepts. It can differ from the number that was asked for when the device cannot
     * play that many; the caller mixes its audio to this number.
     * <p>
     * {@link #write} 가 받는 채널 수를 반환한다. 장치가 요청한 만큼 재생할 수 없으면 요청한 수와 다를 수 있으며, 호출자는 오디오를 이 수로 섞는다.
     *
     * @return the number of channels / 채널 수
     */
    int getChannels();

    /**
     * Returns the capacity of the queue in frames of {@link #getSampleRate()}: how much audio can wait before the device plays it. It
     * decides the latency of a context that keeps the queue full.
     * <p>
     * 큐의 용량을 {@link #getSampleRate()} 의 프레임 수로 반환한다: 장치가 재생하기 전에 기다릴 수 있는 오디오의 양. 큐를 가득 채워 두는 컨텍스트의
     * 지연을 정한다.
     *
     * @return the capacity in frames / 용량(프레임)
     */
    int getBufferFrames();

    /**
     * Queues planar float audio. The call returns when all frames are in the queue: while the output is started and the queue is full it waits
     * for the device to play (that wait is what paces the rendering thread). It returns at once, dropping what does not fit, when the output is
     * stopped and full (nothing can make room then) or when it is closed; a caller therefore never writes more than the free room into a
     * stopped output. Samples outside -1 to 1 are clipped and a NaN is played as silence.
     * <p>
     * 평면 float 오디오를 큐에 넣는다. 모든 프레임이 큐에 들어가면 돌아온다: 출력이 시작된 상태에서 큐가 가득 차 있으면 장치가 재생할 때까지 기다리며(이
     * 기다림이 렌더링 스레드의 속도를 맞춘다), 출력이 멈춘 채 가득 찼거나(그때는 아무것도 자리를 만들 수 없다) 닫혔으면 들어가지 않는 것을 버리고 곧바로
     * 돌아온다. 그러므로 호출자는 멈춘 출력에 빈 자리보다 많이 쓰지 않는다. -1 ~ 1 밖의 샘플은 잘리고 NaN 은 무음으로 재생된다.
     *
     * @param data   one array per channel, at least {@link #getChannels()} of them, each with at least <code>frames</code> samples / 채널마다 하나의 배열(최소
     *               {@link #getChannels()} 개, 각각 <code>frames</code> 개 이상의 샘플)
     * @param frames the number of frames to write / 쓸 프레임 수
     */
    void write(float[][] data, int frames);

    /**
     * Returns the number of frames that were written and are not played yet (the frames of a stopped output included). It is 0 when the device
     * has played everything, which is how a context notices an underrun. Callable from any thread.
     * <p>
     * 쓰였지만 아직 재생되지 않은 프레임 수를 반환한다(멈춘 출력의 프레임도 포함). 장치가 모두 재생했으면 0 이며 컨텍스트는 이것으로 언더런을 알아챈다.
     * 어느 스레드에서나 부를 수 있다.
     *
     * @return the queued frames of {@link #getSampleRate()} / {@link #getSampleRate()} 의 큐에 있는 프레임 수
     */
    long getQueuedFrames();

    /**
     * Starts or resumes playing the queue.
     * <p>
     * 큐의 재생을 시작하거나 재개한다.
     */
    void start();

    /**
     * Pauses playing; the queue stays and continues at {@link #start()}. Callable from any thread; a {@link #write} that waits for room
     * returns at once because nothing can make room in a stopped output.
     * <p>
     * 재생을 멈춘다. 큐는 남고 {@link #start()} 에서 이어진다. 어느 스레드에서나 부를 수 있다. 멈춘 출력에서는 아무것도 자리를 만들 수 없으므로 자리를 기다리는
     * {@link #write} 는 곧바로 돌아온다.
     */
    void stop();

    /**
     * Discards the queued frames; the output keeps its started or stopped state.
     * <p>
     * 큐에 있는 프레임을 버린다. 출력은 시작/정지 상태를 유지한다.
     */
    void flush();

    /**
     * Releases the output (the device line); it is not used again. Repeated calls are harmless, and a {@link #write} that waits returns.
     * <p>
     * 출력(장치 라인)을 해제한다. 다시 쓰지 않는다. 여러 번 불러도 무해하며 기다리고 있는 {@link #write} 는 돌아온다.
     */
    void close();

    /**
     * Returns a short description for diagnostics, for example <code>Java Sound 48000 Hz 2 ch</code> or <code>silent</code>.
     * <p>
     * 진단용 짧은 설명을 반환한다. 예: <code>Java Sound 48000 Hz 2 ch</code> 또는 <code>silent</code>.
     *
     * @return the description / 설명
     */
    String getDescription();

    /**
     * Tells whether a device plays the audio (false for the output that discards it).
     * <p>
     * 장치가 오디오를 재생하는지 알려 준다(오디오를 버리는 출력이면 false).
     *
     * @return true if the sound reaches a device / 소리가 장치에 닿으면 true
     */
    boolean isAudible();
}
