package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A context whose time advances with the wall clock and whose graph is rendered by a {@link Driver} (Web Audio API 1.2, <code>AudioContext</code>):
 * the real-time counterpart of {@link OfflineAudioContext}. The context itself does not own a thread. It owns the state
 * (<code>suspended</code>, <code>running</code>, <code>closed</code>) and a <em>driver</em> that calls the rendering of one render quantum again and
 * again while the context runs. The driver of this stage is the {@link ClockRenderDriver}, which renders at the pace of the wall clock and discards the
 * output (the time advances and the graph works, but nothing is heard); a driver that writes the output to a sound device replaces it without any
 * change of the script layer. The control methods may be called from any thread; the rendering thread is the thread of the driver.
 * <p>
 * Latency: {@link #resolveLatency} turns the <code>latencyHint</code> of the script layer into a number of seconds; the context rounds it up to a
 * whole number of render quanta and reports it as <code>baseLatency</code>. The driver reports the latency of its output device through
 * {@link #reportOutputLatency}.
 * <p>
 * 시간이 벽시계와 함께 흐르고 그래프를 {@link Driver} 가 렌더링하는 컨텍스트(Web Audio API 1.2, <code>AudioContext</code>): {@link OfflineAudioContext} 의
 * 실시간 짝이다. 컨텍스트 자신은 스레드를 갖지 않는다. 상태(<code>suspended</code>, <code>running</code>, <code>closed</code>)와, 컨텍스트가 도는 동안
 * render quantum 하나의 렌더링을 되풀이해서 부르는 <em>드라이버</em>를 가진다. 이 단계의 드라이버는 {@link ClockRenderDriver} 이며 벽시계의 속도로
 * 렌더링하고 출력은 버린다(시간이 흐르고 그래프가 동작하지만 소리는 나지 않는다). 소리 장치로 출력을 쓰는 드라이버는 스크립트 계층을 바꾸지 않고 이것을
 * 대신한다. 제어 메서드는 어느 스레드에서나 부를 수 있고 렌더링 스레드는 드라이버의 스레드이다.
 * <p>
 * 지연: {@link #resolveLatency} 가 스크립트 계층의 <code>latencyHint</code> 를 초 단위 수로 바꾸고, 컨텍스트는 이를 render quantum 의 정수 배로 올림해
 * <code>baseLatency</code> 로 알린다. 드라이버는 출력 장치의 지연을 {@link #reportOutputLatency} 로 알린다.
 */
public final class RealtimeAudioContext extends BaseAudioContext {

    /**
     * What a driver needs to render: it calls {@link #renderQuantum()} once per quantum and reads the position of the context.
     * <p>
     * 드라이버가 렌더링하는 데 필요한 것: quantum 마다 {@link #renderQuantum()} 을 한 번 부르고 컨텍스트의 위치를 읽는다.
     */
    public interface Renderer {

        /**
         * Renders one render quantum (the rendering thread of the driver calls it, one thread at a time).
         * <p>
         * render quantum 하나를 렌더링한다(드라이버의 렌더링 스레드가 한 번에 한 스레드로 부른다).
         */
        void renderQuantum();

        /**
         * Returns the number of frames that have been rendered.
         * <p>
         * 지금까지 렌더링한 프레임 수를 반환한다.
         *
         * @return the current frame / 현재 프레임
         */
        long getCurrentFrame();

        /**
         * Returns the number of frames of a render quantum.
         * <p>
         * render quantum 의 프레임 수를 반환한다.
         *
         * @return the quantum size / quantum 크기
         */
        int getRenderQuantumSize();

        /**
         * Returns the sample rate in Hz.
         * <p>
         * 샘플링 주파수(Hz)를 반환한다.
         *
         * @return the sample rate / 샘플링 주파수
         */
        float getSampleRate();

        /**
         * Returns the number of channels of the destination (the device output has this many channels).
         * <p>
         * 목적지의 채널 수를 반환한다(장치 출력의 채널 수가 이만큼이다).
         *
         * @return the number of channels / 채널 수
         */
        int getDestinationChannels();

        /**
         * Returns the audio that the destination received in the last quantum, mixed to its channels; valid until the next quantum is rendered.
         * <p>
         * 마지막 quantum 에서 목적지가 받은 오디오를 채널 수에 맞게 섞은 것으로 반환한다. 다음 quantum 을 렌더링할 때까지 유효하다.
         *
         * @return the audio / 오디오
         */
        AudioBus getOutputBus();
    }

    /**
     * Drives the rendering of a real-time context: it calls {@link Renderer#renderQuantum()} at the pace of its output while it is started. The
     * methods may be called from any thread, one at a time (the context serializes them).
     * <p>
     * 실시간 컨텍스트의 렌더링을 구동한다: 시작된 동안 출력의 속도에 맞춰 {@link Renderer#renderQuantum()} 을 부른다. 메서드는 어느 스레드에서나 한 번에 하나씩
     * 부를 수 있다(컨텍스트가 직렬화한다).
     */
    public interface Driver {

        /**
         * Starts or restarts the rendering (after {@link #pause()}); the time of the context continues from where it stopped.
         * <p>
         * 렌더링을 시작하거나(<code>pause</code> 뒤에는) 다시 시작한다. 컨텍스트의 시간은 멈춘 곳에서 이어진다.
         *
         * @param renderer the renderer of the context / 컨텍스트의 렌더러
         */
        void start(Renderer renderer);

        /**
         * Stops the rendering; {@link #start(Renderer)} may start it again.
         * <p>
         * 렌더링을 멈춘다. {@link #start(Renderer)} 로 다시 시작할 수 있다.
         */
        void pause();

        /**
         * Stops the rendering for good and releases the thread and the device.
         * <p>
         * 렌더링을 완전히 멈추고 스레드와 장치를 놓아 준다.
         */
        void close();
    }

    /**
     * The latency of the category <code>interactive</code> in seconds.
     * <p>
     * <code>interactive</code> 종류의 지연(초).
     */
    public static final double LATENCY_INTERACTIVE = 0.010;

    /**
     * The latency of the category <code>balanced</code> in seconds.
     * <p>
     * <code>balanced</code> 종류의 지연(초).
     */
    public static final double LATENCY_BALANCED = 0.020;

    /**
     * The latency of the category <code>playback</code> in seconds.
     * <p>
     * <code>playback</code> 종류의 지연(초).
     */
    public static final double LATENCY_PLAYBACK = 0.100;

    /**
     * The sample rate used when the script does not ask for one (there is no device to ask).
     * <p>
     * 스크립트가 정하지 않았을 때 쓰는 샘플링 주파수(물어볼 장치가 없다).
     */
    public static final float DEFAULT_SAMPLE_RATE = 48000f;

    /**
     * The number of channels of the destination of a real-time context.
     * <p>
     * 실시간 컨텍스트 목적지의 채널 수.
     */
    public static final int DESTINATION_CHANNELS = 2;

    /**
     * The driver that renders the context.
     * <p>
     * 컨텍스트를 렌더링하는 드라이버.
     */
    private final Driver driver;

    /**
     * The renderer that the driver calls.
     * <p>
     * 드라이버가 부르는 렌더러.
     */
    private final Renderer renderer = new Renderer() {
        /**
         * Renders one quantum of the context.
         * <p>
         * 컨텍스트의 quantum 하나를 렌더링한다.
         */
        public void renderQuantum() {
            RealtimeAudioContext.this.renderQuantum();
        }

        /**
         * Returns the current frame.
         * <p>
         * 현재 프레임을 반환한다.
         *
         * @return the current frame / 현재 프레임
         */
        public long getCurrentFrame() {
            return RealtimeAudioContext.this.getCurrentFrame();
        }

        /**
         * Returns the quantum size.
         * <p>
         * quantum 크기를 반환한다.
         *
         * @return the quantum size / quantum 크기
         */
        public int getRenderQuantumSize() {
            return RealtimeAudioContext.this.getRenderQuantumSize();
        }

        /**
         * Returns the sample rate.
         * <p>
         * 샘플링 주파수를 반환한다.
         *
         * @return the sample rate / 샘플링 주파수
         */
        public float getSampleRate() {
            return RealtimeAudioContext.this.getSampleRate();
        }

        /**
         * Returns the number of channels of the destination.
         * <p>
         * 목적지의 채널 수를 반환한다.
         *
         * @return the number of channels / 채널 수
         */
        public int getDestinationChannels() {
            return getDestination().getChannelCount();
        }

        /**
         * Returns the audio of the last quantum.
         * <p>
         * 마지막 quantum 의 오디오를 반환한다.
         *
         * @return the audio / 오디오
         */
        public AudioBus getOutputBus() {
            return getDestination().getRenderedBus();
        }
    };

    /**
     * True after {@link #close()}.
     * <p>
     * 컨텍스트를 닫은 뒤에는 true 이다.
     */
    private boolean closed;

    /**
     * Creates a context in the <code>suspended</code> state; {@link #resume()} starts it.
     * <p>
     * <code>suspended</code> 상태의 컨텍스트를 만든다. {@link #resume()} 이 시작한다.
     *
     * @param sampleRate     the sample rate in Hz, from 3000 to 768000 / 샘플링 주파수(Hz, 3000 ~ 768000)
     * @param latencySeconds the requested latency in seconds (see {@link #resolveLatency}) / 요청한 지연(초, {@link #resolveLatency} 참고)
     * @param driver         the driver that renders the context / 컨텍스트를 렌더링하는 드라이버
     * @throws AudioException <code>NotSupportedError</code> if the sample rate is out of range / 샘플링 주파수가 범위 밖이면 <code>NotSupportedError</code>
     */
    public RealtimeAudioContext(float sampleRate, double latencySeconds, Driver driver) {
        super(sampleRate, WebAudioLimits.RENDER_QUANTUM_SIZE, DESTINATION_CHANNELS, DESTINATION_CHANNELS, false);
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        this.driver = driver;
        int quantum = getRenderQuantumSize();
        // 요청한 지연을 render quantum 의 정수 배로 올림해 기본 지연으로 삼는다(최소 2 quantum, 이중 버퍼링).
        long frames = Math.max(2L * quantum, (long) Math.ceil(latencySeconds * sampleRate / quantum) * quantum);
        setLatencies(frames / (double) sampleRate, 0.0);
    }

    /**
     * Turns the <code>latencyHint</code> of the script layer into seconds: a category name, or a number of seconds that is limited to the range
     * from the <code>interactive</code> to the <code>playback</code> latency.
     * <p>
     * 스크립트 계층의 <code>latencyHint</code> 를 초로 바꾼다: 종류 이름이거나, <code>interactive</code> 에서 <code>playback</code> 지연까지의 범위로 제한한
     * 초 단위 수이다.
     *
     * @param category the category name (<code>interactive</code>, <code>balanced</code>, <code>playback</code>), or null if a number is given / 종류 이름. 수를 줬으면 null
     * @param seconds  the number of seconds, used when the category is null / 초 단위 수. 종류가 null 일 때 쓴다
     * @return the latency in seconds / 지연(초)
     */
    public static double resolveLatency(String category, double seconds) {
        if (category == null) {
            return Math.min(LATENCY_PLAYBACK, Math.max(LATENCY_INTERACTIVE, seconds));
        } else if ("balanced".equals(category)) {
            return LATENCY_BALANCED;
        } else if ("playback".equals(category)) {
            return LATENCY_PLAYBACK;
        }
        return LATENCY_INTERACTIVE;
    }

    /**
     * Returns the driver (a test or the host reads its state).
     * <p>
     * 드라이버를 반환한다(시험이나 호스트가 그 상태를 읽는다).
     *
     * @return the driver / 드라이버
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * Starts or continues the rendering: the state becomes <code>running</code>.
     * <p>
     * 렌더링을 시작하거나 이어 간다. 상태가 <code>running</code> 이 된다.
     *
     * @throws AudioException <code>InvalidStateError</code> if the context was closed / 컨텍스트가 닫혔으면 <code>InvalidStateError</code>
     */
    public synchronized void resume() {
        if (closed) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the context is closed");
        }
        setState(ContextState.RUNNING);
        driver.start(renderer);
    }

    /**
     * Stops the progression of the time: the state becomes <code>suspended</code>; {@link #resume()} continues from the same time.
     * <p>
     * 시간의 진행을 멈춘다. 상태가 <code>suspended</code> 가 되며 {@link #resume()} 이 같은 시간에서 이어 간다.
     *
     * @throws AudioException <code>InvalidStateError</code> if the context was closed / 컨텍스트가 닫혔으면 <code>InvalidStateError</code>
     */
    public synchronized void suspend() {
        if (closed) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the context is closed");
        }
        driver.pause();
        setState(ContextState.SUSPENDED);
    }

    /**
     * Closes the context for good: the rendering stops, the thread and the device of the driver are released and the state becomes
     * <code>closed</code>. Calling it again has no effect.
     * <p>
     * 컨텍스트를 완전히 닫는다: 렌더링이 멈추고 드라이버의 스레드와 장치가 놓이며 상태가 <code>closed</code> 가 된다. 다시 불러도 효과가 없다.
     */
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        driver.close();
        setState(ContextState.CLOSED);
    }

    /**
     * Tells whether the context was closed.
     * <p>
     * 컨텍스트가 닫혔는지 알려 준다.
     *
     * @return true after close / close 후이면 true
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Reports the latency of the output device (called by a driver that writes to a device).
     * <p>
     * 출력 장치의 지연을 알린다(장치에 쓰는 드라이버가 부른다).
     *
     * @param seconds the latency in seconds / 지연(초)
     */
    public void reportOutputLatency(double seconds) {
        setLatencies(getBaseLatency(), seconds);
    }
}
