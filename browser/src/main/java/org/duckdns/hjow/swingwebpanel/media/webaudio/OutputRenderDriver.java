package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.AudioOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.NullOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.SystemOutputFactory;

/**
 * The driver of a real-time context that plays through a sound output (plan item P25-07): it replaces the {@link ClockRenderDriver} when the
 * audio should be heard. One daemon <em>rendering thread</em> (<code>swingwebpanel-audio-render-N</code>, a high priority) renders chunks of
 * the context and writes each chunk to an {@link AudioOutput}; the output makes the write wait while its buffer is full, which paces the thread
 * to real speed and keeps the buffer full. The class is pure Java; the {@link OutputFactory} decides which device is used (the default is the
 * Java Sound line of {@link SystemOutputFactory}).
 * <p>
 * <strong>Segments.</strong> The thread lives from the first {@link #start} to {@link #close()} and works in <em>segments</em>: each time the
 * context runs (after {@link #start} or a resume) it opens the output, renders chunks until a {@link #pause()} or {@link #close()}, and
 * releases the output. A suspended context therefore holds no sound device (and a pause is silent at once: the queued audio is discarded,
 * the context time continues from the rendered frame when it resumes). If the device cannot be opened, or cannot play, the segment plays into
 * a {@link NullOutput} instead: nothing is heard, the context time still advances at real speed and the graph works (the same role as the
 * {@link ClockRenderDriver}, but through the same code path and with the same lead of the time as a real device); the next segment tries the
 * device again, so a device that appears later is used. {@link #getOutputProblem()} tells why the device was not used.
 * <p>
 * <strong>Buffers and latency.</strong> The latency in seconds that the context was created with decides two sizes. The <em>chunk</em> is the
 * number of render quanta that are rendered before one write, half of the latency in quanta, from 1 to {@value #MAX_CHUNK_QUANTA} (10 ms:
 * 1 quantum, 20 ms: 3, 100 ms: 16 at 44100 Hz); it is the unit of the base latency of the thread. The <em>buffer</em> is the audio that is kept
 * queued for the device, the larger of {@value #MIN_BUFFER_SECONDS} seconds and {@value #BUFFER_PER_LATENCY} times the latency, rounded up to a whole
 * number of chunks (at least two). A Java Sound line is not reliable with a buffer of a few milliseconds, which is why the buffer is not the
 * latency itself. After every write the thread reports the queued audio in seconds to the context ({@link RealtimeAudioContext#reportOutputLatency}),
 * which is what <code>outputLatency</code> shows. <code>currentTime</code> counts the rendered frames, so it leads the sound that is heard by that
 * queue; {@link #getOutputTimestamp()} tells which context time is heard now.
 * <p>
 * <strong>Underruns.</strong> If rendering is slower than real time (a heavy graph, a busy computer, a late <code>ScriptProcessorNode</code> handler)
 * the device runs dry. The thread then renders and writes as fast as it can until the buffer is full again: no audio is dropped or repeated, the
 * latency does not grow, and the gap in the sound lasts as long as the shortage. {@link #getUnderrunCount()} counts the writes that found the
 * output empty.
 * <p>
 * <strong>Threads.</strong> {@link #start}, {@link #pause()} and {@link #close()} return at once and may be called from any thread (the context
 * serializes them); they only set flags, wake the thread and stop the output so that a write that waits returns. The thread is the only caller
 * of the renderer and of the output, and it never calls a script.
 * <p>
 * 소리 출력으로 재생하는 실시간 컨텍스트의 드라이버(계획 P25-07): 소리가 들려야 할 때 {@link ClockRenderDriver} 를 대신한다. 데몬 <em>렌더링 스레드</em>
 * 하나(<code>swingwebpanel-audio-render-N</code>, 높은 우선순위)가 컨텍스트를 청크 단위로 렌더링해 {@link AudioOutput} 에 쓴다. 출력은 버퍼가 가득 차 있으면
 * 쓰기를 기다리게 하며, 이것이 스레드를 실제 속도로 맞추고 버퍼를 가득 채워 둔다. 클래스는 순수 Java 이며 어떤 장치를 쓸지는 {@link OutputFactory} 가 정한다
 * (기본은 {@link SystemOutputFactory} 의 Java Sound 라인).
 * <p>
 * <strong>구간.</strong> 스레드는 처음 {@link #start} 부터 {@link #close()} 까지 살며 <em>구간</em>으로 일한다: 컨텍스트가 도는 구간마다({@link #start} 나 재개
 * 뒤) 출력을 열고, {@link #pause()} 나 {@link #close()} 까지 청크를 렌더링하고, 출력을 해제한다. 그래서 일시정지된 컨텍스트는 소리 장치를 붙잡지 않는다(일시정지는
 * 곧바로 조용해진다: 큐의 오디오는 버려지고, 컨텍스트 시간은 재개할 때 렌더링한 프레임에서 이어진다). 장치를 열 수 없거나 재생할 수 없으면 그 구간은 대신
 * {@link NullOutput} 으로 재생한다: 아무것도 들리지 않지만 컨텍스트 시간은 실제 속도로 진행하고 그래프는 동작한다({@link ClockRenderDriver} 와 같은 역할이지만 같은
 * 코드 경로를 지나고 실제 장치와 같은 시간의 앞섬을 가진다). 다음 구간은 장치를 다시 시도하므로 나중에 나타난 장치를 쓴다. {@link #getOutputProblem()} 이 장치를
 * 쓰지 못한 이유를 알려 준다.
 * <p>
 * <strong>버퍼와 지연.</strong> 컨텍스트를 만들 때의 지연(초)이 두 크기를 정한다. <em>청크</em>는 한 번 쓰기 전에 렌더링하는 render quantum 의 수로, 지연(quantum 수)의 절반이며
 * 1 ~ {@value #MAX_CHUNK_QUANTA} 이다(44100 Hz 에서 10 ms: 1, 20 ms: 3, 100 ms: 16). 스레드의 기본 지연의 단위이다. <em>버퍼</em>는 장치를 위해 쌓아 두는 오디오로,
 * {@value #MIN_BUFFER_SECONDS} 초와 지연의 {@value #BUFFER_PER_LATENCY} 배 중 큰 값을 청크의 정수 배(최소 둘)로 올림한 것이다. Java Sound 라인은 수 밀리초의 버퍼에서
 * 믿을 수 없으므로 버퍼는 지연 그 자체가 아니다. 스레드는 쓸 때마다 큐의 오디오(초)를 컨텍스트에 알리며({@link RealtimeAudioContext#reportOutputLatency}) 이것이
 * <code>outputLatency</code> 로 보인다. <code>currentTime</code> 은 렌더링한 프레임을 세므로 들리는 소리보다 그 큐만큼 앞선다. {@link #getOutputTimestamp()} 는 지금 들리는
 * 컨텍스트 시간을 알려 준다.
 * <p>
 * <strong>언더런.</strong> 렌더링이 실시간보다 느리면(무거운 그래프, 바쁜 컴퓨터, 늦은 <code>ScriptProcessorNode</code> 핸들러) 장치가 빈다. 그러면 스레드는 버퍼가 다시 가득 찰
 * 때까지 가능한 한 빨리 렌더링하고 쓰므로 오디오가 버려지거나 반복되지 않고 지연이 커지지 않으며, 소리의 끊김은 부족했던 시간만큼 이어진다. {@link #getUnderrunCount()} 는 출력이
 * 비어 있는 것을 발견한 쓰기의 횟수를 센다.
 * <p>
 * <strong>스레드.</strong> {@link #start}, {@link #pause()}, {@link #close()} 는 곧바로 돌아오며 어느 스레드에서나 부를 수 있다(컨텍스트가 직렬화한다). 플래그를 정하고 스레드를
 * 깨우고, 기다리는 쓰기가 돌아오도록 출력을 멈출 뿐이다. 렌더러와 출력을 부르는 것은 이 스레드뿐이며 스크립트는 부르지 않는다.
 */
public final class OutputRenderDriver implements RealtimeAudioContext.Driver, Runnable {

    /**
     * The prefix of the name of every rendering thread of a real-time context (the same as that of the {@link ClockRenderDriver}).
     * <p>
     * 실시간 컨텍스트의 모든 렌더링 스레드 이름의 접두사({@link ClockRenderDriver} 와 같다).
     */
    public static final String THREAD_NAME_PREFIX = "swingwebpanel-audio-render-";

    /**
     * The smallest buffer in seconds.
     * <p>
     * 가장 작은 버퍼(초).
     */
    public static final double MIN_BUFFER_SECONDS = 0.05;

    /**
     * The buffer in latencies: the buffer is this many times the latency of the context (if that is more than the smallest buffer).
     * <p>
     * 지연 배수의 버퍼: 컨텍스트 지연의 이 배수가 버퍼이다(가장 작은 버퍼보다 클 때).
     */
    public static final int BUFFER_PER_LATENCY = 4;

    /**
     * The largest chunk in render quanta.
     * <p>
     * 가장 큰 청크(render quantum 수).
     */
    public static final int MAX_CHUNK_QUANTA = 16;

    /**
     * The largest buffer in frames.
     * <p>
     * 가장 큰 버퍼(프레임).
     */
    private static final int MAX_BUFFER_FRAMES = 1 << 22;

    /**
     * The estimate of when a frame is heard (the result of {@link OutputRenderDriver#getOutputTimestamp()}).
     * <p>
     * 프레임이 들리는 시각의 추정({@link OutputRenderDriver#getOutputTimestamp()} 의 결과).
     */
    public static final class Timestamp {

        /**
         * The context time that is heard now.
         * <p>
         * 지금 들리는 컨텍스트 시간.
         */
        private final double contextTime;

        /**
         * The <code>System.nanoTime()</code> reading at which that is true.
         * <p>
         * 그것이 참인 <code>System.nanoTime()</code> 값.
         */
        private final long nanoTime;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param contextTime the context time in seconds / 컨텍스트 시간(초)
         * @param nanoTime    the clock reading / 시계 값
         */
        Timestamp(double contextTime, long nanoTime) {
            this.contextTime = contextTime;
            this.nanoTime = nanoTime;
        }

        /**
         * Returns the time of the sample frame that the output plays now, in the time of the context (<code>contextTime</code> of
         * <code>getOutputTimestamp()</code>).
         * <p>
         * 출력이 지금 재생하는 샘플 프레임의 시간을 컨텍스트의 시간으로 반환한다(<code>getOutputTimestamp()</code> 의 <code>contextTime</code>).
         *
         * @return the context time in seconds / 컨텍스트 시간(초)
         */
        public double getContextTime() {
            return contextTime;
        }

        /**
         * Returns the <code>System.nanoTime()</code> reading at which the context time is heard; the script layer converts it to the time base
         * of <code>performance.now()</code> (<code>performanceTime</code>).
         * <p>
         * 그 컨텍스트 시간이 들리는 <code>System.nanoTime()</code> 값을 반환한다. 스크립트 계층이 이를 <code>performance.now()</code> 의 시간 기준으로 바꾼다
         * (<code>performanceTime</code>).
         *
         * @return the nanosecond reading / 나노초 값
         */
        public long getNanoTime() {
            return nanoTime;
        }
    }

    /**
     * The name of the rendering thread.
     * <p>
     * 렌더링 스레드의 이름.
     */
    private final String threadName;

    /**
     * The factory of the outputs.
     * <p>
     * 출력의 팩터리.
     */
    private volatile OutputFactory outputs;

    /**
     * The latency that the context was created with, in seconds.
     * <p>
     * 컨텍스트를 만들 때의 지연(초).
     */
    private final double latencySeconds;

    /**
     * Guards {@link #renderer}, {@link #thread}, {@link #running}, {@link #closed} and {@link #epoch}.
     * <p>
     * {@link #renderer}, {@link #thread}, {@link #running}, {@link #closed}, {@link #epoch} 를 지킨다.
     */
    private final Object lock = new Object();

    /**
     * The renderer of the context.
     * <p>
     * 컨텍스트의 렌더러.
     */
    private RealtimeAudioContext.Renderer renderer;

    /**
     * The rendering thread, or null before the first start.
     * <p>
     * 렌더링 스레드. 첫 시작 전에는 null.
     */
    private Thread thread;

    /**
     * True while the context should run.
     * <p>
     * 컨텍스트가 돌아야 하는 동안 true.
     */
    private boolean running;

    /**
     * True after {@link #close()}.
     * <p>
     * {@link #close()} 뒤에 true.
     */
    private boolean closed;

    /**
     * The number of the current run: every pause ends the segment of the run that it interrupts, even if a start follows at once.
     * <p>
     * 현재 실행의 번호: 일시정지마다 그것이 끊는 실행의 구간이 끝난다. 곧바로 시작이 이어져도 마찬가지이다.
     */
    private long epoch;

    /**
     * The output of the segment that is running, or null; the control side stops it to wake a write that waits.
     * <p>
     * 실행 중인 구간의 출력. 없으면 null. 제어 쪽이 기다리는 쓰기를 깨우려고 멈춘다.
     */
    private volatile AudioOutput activeOutput;

    /**
     * The context that receives the latency of the output.
     * <p>
     * 출력의 지연을 받는 컨텍스트.
     */
    private volatile RealtimeAudioContext context;

    /**
     * Guards the timing snapshot.
     * <p>
     * 시간 스냅샷을 지킨다.
     */
    private final Object timingLock = new Object();

    /**
     * The frames rendered at the last write (timing snapshot).
     * <p>
     * 마지막 쓰기 때 렌더링한 프레임 수(시간 스냅샷).
     */
    private long snapFrames;

    /**
     * The frames queued in the output at the last write (timing snapshot).
     * <p>
     * 마지막 쓰기 때 출력에 쌓여 있던 프레임 수(시간 스냅샷).
     */
    private long snapQueued;

    /**
     * The <code>System.nanoTime()</code> at the last write (timing snapshot).
     * <p>
     * 마지막 쓰기 때의 <code>System.nanoTime()</code>(시간 스냅샷).
     */
    private long snapNanos;

    /**
     * True while the output plays (timing snapshot).
     * <p>
     * 출력이 재생하는 동안 true(시간 스냅샷).
     */
    private boolean snapPlaying;

    /**
     * The sample rate of the context for the snapshot.
     * <p>
     * 스냅샷을 위한 컨텍스트의 샘플링 주파수.
     */
    private double snapRate = 1.0;

    /**
     * The number of underruns.
     * <p>
     * 언더런 횟수.
     */
    private volatile int underruns;

    /**
     * The number of segments that started (a test counts them).
     * <p>
     * 시작한 구간의 수(시험이 센다).
     */
    private volatile int segments;

    /**
     * The description of the last opened output, or null.
     * <p>
     * 마지막으로 연 출력의 설명. 없으면 null.
     */
    private volatile String outputDescription;

    /**
     * True if the last opened output plays to a device.
     * <p>
     * 마지막으로 연 출력이 장치로 재생하면 true.
     */
    private volatile boolean outputAudible;

    /**
     * The reason why the device was not used the last time, or null.
     * <p>
     * 마지막으로 장치를 쓰지 못한 이유. 없으면 null.
     */
    private volatile String outputProblem;

    /**
     * The failure that stopped the last segment, or null.
     * <p>
     * 마지막 구간을 멈춘 실패. 없으면 null.
     */
    private volatile Throwable renderFailure;

    /**
     * The frames of the chunk of the last segment.
     * <p>
     * 마지막 구간의 청크 프레임 수.
     */
    private volatile int chunkFrames;

    /**
     * The frames of the buffer of the last segment.
     * <p>
     * 마지막 구간의 버퍼 프레임 수.
     */
    private volatile int bufferFrames;

    /**
     * Creates a driver; {@link #attach} or {@link #createContext} connects it to its context.
     * <p>
     * 드라이버를 만든다. {@link #attach} 나 {@link #createContext} 가 드라이버를 컨텍스트와 잇는다.
     *
     * @param threadName     the name of the rendering thread, for example <code>swingwebpanel-audio-render-1</code> (the prefix is added if it is missing) / 렌더링
     *                       스레드의 이름. 예: <code>swingwebpanel-audio-render-1</code>(접두사가 없으면 붙인다)
     * @param outputs        the factory of the outputs, or null for the system output (Java Sound) / 출력의 팩터리. 시스템 출력(Java Sound)이면 null
     * @param latencySeconds the latency of the context in seconds, which decides the sizes of the chunk and the buffer / 컨텍스트의 지연(초). 청크와 버퍼의 크기를 정한다
     */
    public OutputRenderDriver(String threadName, OutputFactory outputs, double latencySeconds) {
        if (threadName == null) {
            throw new IllegalArgumentException("threadName must not be null");
        }
        if (!(latencySeconds >= 0) || Double.isInfinite(latencySeconds)) {
            throw new IllegalArgumentException("latencySeconds " + latencySeconds);
        }
        // 제품의 모든 스레드는 소유자를 알 수 있는 접두사로 시작해야 하므로 접두사가 없으면 붙인다.
        this.threadName = threadName.startsWith(THREAD_NAME_PREFIX) ? threadName : THREAD_NAME_PREFIX + threadName;
        this.outputs = outputs == null ? new SystemOutputFactory() : outputs;
        this.latencySeconds = latencySeconds;
    }

    /**
     * Creates a real-time context that is played by a new driver: the context is built with the driver and the driver is attached to it, so the
     * output latency reaches the context.
     * <p>
     * 새 드라이버가 재생하는 실시간 컨텍스트를 만든다: 드라이버로 컨텍스트를 만들고 드라이버를 컨텍스트에 붙이므로 출력 지연이 컨텍스트에 닿는다.
     *
     * @param sampleRate     the sample rate in Hz / 샘플링 주파수(Hz)
     * @param latencySeconds the latency in seconds (see {@link RealtimeAudioContext#resolveLatency}) / 지연(초, {@link RealtimeAudioContext#resolveLatency} 참고)
     * @param outputs        the factory of the outputs, or null for the system output / 출력의 팩터리. 시스템 출력이면 null
     * @param threadName     the name of the rendering thread / 렌더링 스레드의 이름
     * @return the context, suspended / 컨텍스트(suspended)
     * @throws AudioException <code>NotSupportedError</code> if the sample rate is out of range / 샘플링 주파수가 범위 밖이면 <code>NotSupportedError</code>
     */
    public static RealtimeAudioContext createContext(float sampleRate, double latencySeconds, OutputFactory outputs, String threadName) {
        OutputRenderDriver driver = new OutputRenderDriver(threadName, outputs, latencySeconds);
        RealtimeAudioContext c = new RealtimeAudioContext(sampleRate, latencySeconds, driver);
        driver.attach(c);
        return c;
    }

    /**
     * Returns a factory of silent outputs: the segments that it serves play into a {@link NullOutput} on purpose (the time of the context runs at real
     * speed and nothing is heard, and no problem is reported). The script layer sets it for <code>setSinkId({type: "none"})</code>.
     * <p>
     * 무음 출력의 팩터리를 반환한다: 이것이 맡는 구간은 일부러 {@link NullOutput} 으로 재생한다(컨텍스트 시간은 실제 속도로 흐르고 아무것도 들리지 않으며 문제도 보고하지
     * 않는다). 스크립트 계층이 <code>setSinkId({type: "none"})</code> 에 쓴다.
     *
     * @return the factory / 팩터리
     */
    public static OutputFactory silentOutputs() {
        return new OutputFactory() {
            /**
             * Opens a silent output.
             * <p>
             * 무음 출력을 연다.
             *
             * @param sampleRate   the sample rate / 샘플링 주파수
             * @param channels     the channels / 채널 수
             * @param bufferFrames the buffer / 버퍼
             * @return the silent output / 무음 출력
             */
            public AudioOutput open(int sampleRate, int channels, int bufferFrames) {
                return new NullOutput(sampleRate, channels, bufferFrames);
            }
        };
    }

    /**
     * Sets the factory of the outputs for the next segment (a running segment keeps its output until a pause and a resume); null means the system
     * output. The device of a context changes with it when the context is suspended and resumed.
     * <p>
     * 다음 구간을 위한 출력의 팩터리를 정한다(실행 중인 구간은 일시정지와 재개까지 자기 출력을 유지한다). null 이면 시스템 출력이다. 컨텍스트를 일시정지하고
     * 재개하면 그 컨텍스트의 장치가 바뀐다.
     *
     * @param factory the factory or null / 팩터리 또는 null
     */
    public void setOutputFactory(OutputFactory factory) {
        this.outputs = factory == null ? new SystemOutputFactory() : factory;
    }

    /**
     * Connects the driver to its context so that the latency of the output is reported to it.
     * <p>
     * 출력의 지연을 알릴 수 있도록 드라이버를 컨텍스트와 잇는다.
     *
     * @param c the context that this driver renders / 이 드라이버가 렌더링하는 컨텍스트
     */
    public void attach(RealtimeAudioContext c) {
        this.context = c;
    }

    /**
     * Starts the rendering, or lets it continue if it runs: the rendering thread is created at the first start and opens the output.
     * <p>
     * 렌더링을 시작하거나, 돌고 있으면 계속하게 한다: 렌더링 스레드는 처음 시작할 때 만들어지고 출력을 연다.
     *
     * @param r the renderer of the context / 컨텍스트의 렌더러
     */
    public void start(RealtimeAudioContext.Renderer r) {
        synchronized (lock) {
            if (closed) {
                return;
            }
            renderer = r;
            running = true;
            // 스레드는 처음 시작할 때 만든다. 이미 있으면 깨우기만 한다.
            if (thread == null) {
                Thread t = new Thread(this, threadName);
                t.setDaemon(true);
                t.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 3));
                thread = t;
                t.start();
            } else {
                lock.notifyAll();
            }
        }
    }

    /**
     * Stops the rendering: the segment ends after the chunk that is being rendered, the queued audio is discarded and the output is released.
     * <p>
     * 렌더링을 멈춘다: 렌더링 중인 청크 뒤에 구간이 끝나고, 큐의 오디오는 버려지고, 출력이 해제된다.
     */
    public void pause() {
        synchronized (lock) {
            running = false;
            epoch++;
            lock.notifyAll();
        }
        wakeWriter();
    }

    /**
     * Ends the segment that runs now and starts the next one at once, so that a new factory (see {@link #setOutputFactory}) is used from now on: the output of the old segment
     * is released (the audio that was queued in it is discarded, at most one buffer of it, 50 to 80 ms) and the next segment opens a new output. The time of the context goes on
     * from the rendered frames, as at a pause and a resume, but the state of the context is not touched. It has no effect while the driver is paused, not started or closed
     * (the next start uses the new factory anyway). The script layer calls it for <code>setSinkId</code> of a running context.
     * <p>
     * 지금 돌고 있는 구간을 끝내고 곧바로 다음 구간을 시작해 새 팩터리({@link #setOutputFactory} 참고)를 이제부터 쓰게 한다: 이전 구간의 출력은 해제되고(그 안에 큐에 있던
     * 오디오는, 많아야 버퍼 하나 분량인 50 ~ 80 ms, 버려진다) 다음 구간이 새 출력을 연다. 컨텍스트의 시간은 일시정지와 재개 때처럼 렌더링한 프레임에서 이어지지만 컨텍스트의
     * 상태는 건드리지 않는다. 드라이버가 일시정지, 시작 전, 닫힘이면 효과가 없다(다음 시작이 어차피 새 팩터리를 쓴다). 스크립트 계층이 실행 중인 컨텍스트의
     * <code>setSinkId</code> 에 쓴다.
     */
    public void restart() {
        synchronized (lock) {
            // 돌고 있는 구간이 없으면 할 일이 없다. 다음 시작이 새 팩터리를 쓴다.
            if (!running || closed) {
                return;
            }
            epoch++;
            lock.notifyAll();
        }
        wakeWriter();
    }

    /**
     * Stops the rendering for good: the thread ends and the output is released.
     * <p>
     * 렌더링을 완전히 멈춘다: 스레드가 끝나고 출력이 해제된다.
     */
    public void close() {
        synchronized (lock) {
            closed = true;
            running = false;
            epoch++;
            lock.notifyAll();
        }
        wakeWriter();
    }

    /**
     * Stops the output of the running segment so that a write that waits for room returns at once (nothing can make room in a stopped
     * output). It is harmless when no segment runs, or when the segment is the next one: the thread checks the number of the run before it
     * writes again.
     * <p>
     * 실행 중인 구간의 출력을 멈춰 자리를 기다리는 쓰기가 곧바로 돌아오게 한다(멈춘 출력에서는 아무것도 자리를 만들 수 없다). 실행 중인 구간이 없어도, 그 구간이
     * 다음 구간이어도 무해하다. 스레드는 다시 쓰기 전에 실행의 번호를 확인한다.
     */
    private void wakeWriter() {
        AudioOutput out = activeOutput;
        if (out != null) {
            out.stop();
        }
    }

    /**
     * Returns the rendering thread, or null before the first start (a test checks its name and that it ends).
     * <p>
     * 렌더링 스레드를 반환한다. 첫 시작 전에는 null(시험이 그 이름과 종료를 확인한다).
     *
     * @return the thread or null / 스레드 또는 null
     */
    public Thread getThread() {
        synchronized (lock) {
            return thread;
        }
    }

    /**
     * Returns the number of times that the output was empty when a chunk was ready to be written while the output played.
     * <p>
     * 출력이 재생하는 동안 청크를 쓸 준비가 되었을 때 출력이 비어 있던 횟수를 반환한다.
     *
     * @return the number of underruns / 언더런 횟수
     */
    public int getUnderrunCount() {
        return underruns;
    }

    /**
     * Returns the number of segments that were started (one per run of the context).
     * <p>
     * 시작한 구간의 수(컨텍스트가 도는 구간마다 하나)를 반환한다.
     *
     * @return the number of segments / 구간 수
     */
    public int getSegmentCount() {
        return segments;
    }

    /**
     * Returns the description of the output that was opened last (for example <code>Java Sound 48000 Hz 2 ch</code> or <code>silent</code>),
     * or null if none was opened.
     * <p>
     * 마지막으로 연 출력의 설명(예: <code>Java Sound 48000 Hz 2 ch</code> 또는 <code>silent</code>)을 반환한다. 연 적이 없으면 null.
     *
     * @return the description / 설명
     */
    public String getOutputDescription() {
        return outputDescription;
    }

    /**
     * Tells whether the output that was opened last plays to a sound device (false for the silent output).
     * <p>
     * 마지막으로 연 출력이 소리 장치로 재생하는지 알려 준다(무음 출력이면 false).
     *
     * @return true if the sound reaches a device / 소리가 장치에 닿으면 true
     */
    public boolean isOutputAudible() {
        return outputAudible;
    }

    /**
     * Returns the reason why the sound device was not used the last time (the segment played silently instead), or null.
     * <p>
     * 마지막으로 소리 장치를 쓰지 못한 이유를 반환한다(그 구간은 대신 무음으로 재생했다). 없으면 null.
     *
     * @return the reason or null / 이유. 없으면 null
     */
    public String getOutputProblem() {
        return outputProblem;
    }

    /**
     * Returns the failure that stopped the last segment unexpectedly (an exception of the renderer or the output), or null. The driver
     * stops after a failure and tries again at the next {@link #start}.
     * <p>
     * 마지막 구간을 예기치 않게 멈춘 실패(렌더러나 출력의 예외)를 반환한다. 없으면 null. 드라이버는 실패 뒤 멈추고 다음 {@link #start} 에서 다시 시도한다.
     *
     * @return the failure or null / 실패. 없으면 null
     */
    public Throwable getRenderFailure() {
        return renderFailure;
    }

    /**
     * Returns the frames of one chunk of the last segment.
     * <p>
     * 마지막 구간의 청크 하나의 프레임 수를 반환한다.
     *
     * @return the frames, 0 before the first segment / 프레임 수. 첫 구간 전에는 0
     */
    public int getChunkFrames() {
        return chunkFrames;
    }

    /**
     * Returns the frames of the output buffer of the last segment.
     * <p>
     * 마지막 구간의 출력 버퍼의 프레임 수를 반환한다.
     *
     * @return the frames, 0 before the first segment / 프레임 수. 첫 구간 전에는 0
     */
    public int getBufferFrames() {
        return bufferFrames;
    }

    /**
     * Returns the context time that the output plays now and the clock reading at which that is true. While nothing plays it is the rendered
     * time. The value is an estimate from the output queue at the last write plus the time that has passed since, never beyond the rendered
     * time.
     * <p>
     * 출력이 지금 재생하는 컨텍스트 시간과 그것이 참인 시계 값을 반환한다. 아무것도 재생하지 않으면 렌더링한 시간이다. 값은 마지막 쓰기 때의 출력 큐에 그 뒤로 지난
     * 시간을 더한 추정이며 렌더링한 시간을 넘지 않는다.
     *
     * @return the timestamp / 타임스탬프
     */
    public Timestamp getOutputTimestamp() {
        long now = System.nanoTime();
        synchronized (timingLock) {
            // 재생 중이 아니면 렌더링한 시간이 곧 들린 시간이다.
            if (!snapPlaying) {
                return new Timestamp(snapFrames / snapRate, now);
            }
            double heard = snapFrames - snapQueued + (now - snapNanos) * 1e-9 * snapRate;
            // 렌더링하지 않은 시간은 들을 수 없고 음수일 수도 없다.
            if (heard > snapFrames) {
                heard = snapFrames;
            }
            if (heard < 0) {
                heard = 0;
            }
            return new Timestamp(heard / snapRate, now);
        }
    }

    /**
     * Tells whether the segment of a run must end: the driver was closed or a pause happened since the run started.
     * <p>
     * 실행의 구간이 끝나야 하는지 알려 준다: 드라이버가 닫혔거나 실행이 시작된 뒤 일시정지가 있었다.
     *
     * @param runEpoch the number of the run that the segment belongs to / 구간이 속한 실행의 번호
     * @return true if the segment must end / 구간이 끝나야 하면 true
     */
    private boolean cancelled(long runEpoch) {
        synchronized (lock) {
            return closed || epoch != runEpoch;
        }
    }

    /**
     * The loop of the rendering thread: waits while the context does not run and plays one segment each time it runs.
     * <p>
     * 렌더링 스레드의 루프: 컨텍스트가 돌지 않는 동안 기다리고, 돌 때마다 구간 하나를 재생한다.
     */
    public void run() {
        // 닫힐 때까지 구간을 되풀이한다.
        for (;;) {
            RealtimeAudioContext.Renderer r;
            long runEpoch;
            synchronized (lock) {
                // 컨텍스트가 일시정지된 동안과 아직 시작하지 않은 동안 기다린다.
                while (!closed && !running) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (closed) {
                    return;
                }
                r = renderer;
                runEpoch = epoch;
            }
            playSegment(r, runEpoch);
        }
    }

    /**
     * Plays one segment: opens the output, fills it, renders until the run is cancelled and releases the output. A failure is recorded and
     * stops the driver until the next start.
     * <p>
     * 구간 하나를 재생한다: 출력을 열고, 채우고, 실행이 취소될 때까지 렌더링하고, 출력을 해제한다. 실패는 기록하며 다음 시작까지 드라이버를 멈춘다.
     *
     * @param r        the renderer / 렌더러
     * @param runEpoch the number of the run / 실행의 번호
     */
    private void playSegment(RealtimeAudioContext.Renderer r, long runEpoch) {
        AudioOutput out = null;
        RealtimeAudioContext ctx = context;
        double rate = r.getSampleRate();
        try {
            segments++;
            int q = r.getRenderQuantumSize();
            // 지연(quantum 수)의 절반을 청크로, 지연의 몇 배(또는 가장 작은 버퍼)를 청크의 정수 배로 올림해 버퍼로 삼는다.
            int chunkQuanta = (int) Math.max(1, Math.min(MAX_CHUNK_QUANTA, Math.floor(latencySeconds * rate / q / 2)));
            int chunk = chunkQuanta * q;
            double bufferSeconds = Math.max(MIN_BUFFER_SECONDS, latencySeconds * BUFFER_PER_LATENCY);
            long chunks = Math.max(2L, (long) Math.ceil(bufferSeconds * rate / chunk));
            int buffer = (int) Math.min(chunks * chunk, (long) Math.max(MAX_BUFFER_FRAMES, 2 * chunk));
            chunkFrames = chunk;
            bufferFrames = buffer;
            out = openOutput((int) Math.round(rate), buffer);
            activeOutput = out;
            // 출력을 여는 사이 일시정지나 닫기가 왔으면 아무것도 재생하지 않고 끝낸다.
            if (cancelled(runEpoch)) {
                return;
            }
            int outChannels = out.getChannels();
            float[][] data = new float[outChannels][chunk];
            AudioBus staging = new AudioBus(outChannels, q);
            synchronized (timingLock) {
                snapRate = rate;
                snapFrames = r.getCurrentFrame();
                snapQueued = 0;
                snapPlaying = false;
            }
            // 소리가 시작되기 전에 버퍼를 채워 시작할 때의 끊김을 없앤다(적어도 한 청크).
            int most = out.getBufferFrames() / chunk + 2;
            for (int i = 0; i < most && !cancelled(runEpoch); i++) {
                if (i > 0 && out.getQueuedFrames() + chunk > out.getBufferFrames()) {
                    break;
                }
                renderChunk(r, out, data, staging, chunkQuanta, ctx, false);
            }
            if (cancelled(runEpoch)) {
                return;
            }
            out.start();
            synchronized (timingLock) {
                snapPlaying = true;
            }
            // 일시정지나 닫기가 올 때까지 청크를 렌더링해 출력에 쓴다.
            while (!cancelled(runEpoch)) {
                renderChunk(r, out, data, staging, chunkQuanta, ctx, true);
            }
        } catch (Throwable t) {
            // 예기치 않은 실패는 기록하고, 다음 시작까지 멈춘다(끝없이 실패를 되풀이하지 않도록).
            renderFailure = t;
            synchronized (lock) {
                running = false;
            }
        } finally {
            activeOutput = null;
            synchronized (timingLock) {
                snapPlaying = false;
                snapFrames = r.getCurrentFrame();
            }
            releaseOutput(out);
            if (ctx != null) {
                ctx.reportOutputLatency(0.0);
            }
        }
    }

    /**
     * Opens the output, or a silent one if there is no device or it cannot be opened.
     * <p>
     * 출력을 연다. 장치가 없거나 열 수 없으면 무음 출력을 연다.
     *
     * @param rate         the sample rate in Hz / 샘플링 주파수(Hz)
     * @param bufferFrames the buffer in frames / 버퍼(프레임)
     * @return the output, stopped / 멈춘 상태의 출력
     */
    private AudioOutput openOutput(int rate, int bufferFrames) {
        AudioOutput out = null;
        outputProblem = null;
        OutputFactory factory = outputs;
        try {
            out = factory.open(rate, RealtimeAudioContext.DESTINATION_CHANNELS, bufferFrames);
            // 팩터리가 다른 주파수의 출력을 돌려주면 약속을 어긴 것이므로 쓰지 않는다.
            if (out != null && out.getSampleRate() != rate) {
                out.close();
                out = null;
                outputProblem = "the output does not play " + rate + " Hz";
            }
        } catch (IOException e) {
            outputProblem = e.getMessage();
        } catch (RuntimeException e) {
            outputProblem = e.toString();
        }
        // 장치를 열지 못했으면 소리 없이 시계만 진행하는 출력을 쓴다.
        if (out == null) {
            out = new NullOutput(rate, RealtimeAudioContext.DESTINATION_CHANNELS, bufferFrames);
        }
        outputDescription = out.getDescription();
        outputAudible = out.isAudible();
        return out;
    }

    /**
     * Stops, flushes and closes an output.
     * <p>
     * 출력을 멈추고 비우고 닫는다.
     *
     * @param out the output or null / 출력 또는 null
     */
    private static void releaseOutput(AudioOutput out) {
        if (out == null) {
            return;
        }
        try {
            out.stop();
            out.flush();
        } catch (RuntimeException ignored) {
            // 출력이 이미 망가졌어도 닫기는 계속한다.
        } finally {
            try {
                out.close();
            } catch (RuntimeException ignored) {
                // 닫는 중의 예외는 더 처리할 수 없다.
            }
        }
    }

    /**
     * Renders one chunk and writes it to the output.
     * <p>
     * 청크 하나를 렌더링해 출력에 쓴다.
     *
     * @param r           the renderer / 렌더러
     * @param out         the output / 출력
     * @param data        the chunk buffer, one array per channel of the output / 청크 버퍼(출력의 채널마다 하나의 배열)
     * @param staging     the bus that mixes to the channels of the output / 출력의 채널로 섞는 버스
     * @param chunkQuanta the number of render quanta of the chunk / 청크의 render quantum 수
     * @param ctx         the context that receives the output latency, or null / 출력 지연을 받는 컨텍스트. 없으면 null
     * @param countGaps   true to count an empty queue as an underrun / 빈 큐를 언더런으로 세려면 true
     */
    private void renderChunk(RealtimeAudioContext.Renderer r, AudioOutput out, float[][] data, AudioBus staging, int chunkQuanta, RealtimeAudioContext ctx,
            boolean countGaps) {
        int q = r.getRenderQuantumSize();
        int outChannels = data.length;
        // 청크를 이루는 quantum 을 하나씩 렌더링해 청크 버퍼에 모은다.
        for (int k = 0; k < chunkQuanta; k++) {
            r.renderQuantum();
            AudioBus bus = r.getOutputBus();
            int at = k * q;
            // 목적지의 채널 수가 출력과 같으면 그대로 복사하고, 다르면 스피커 규칙으로 섞어서 복사한다.
            if (bus.getNumberOfChannels() == outChannels) {
                for (int c = 0; c < outChannels; c++) {
                    System.arraycopy(bus.channel(c), 0, data[c], at, q);
                }
            } else {
                ChannelMixing.mix(bus, staging, ChannelInterpretation.SPEAKERS, false);
                // 섞은 채널을 청크 버퍼에 복사한다.
                for (int c = 0; c < outChannels; c++) {
                    System.arraycopy(staging.channel(c), 0, data[c], at, q);
                }
            }
        }
        // 재생 중인데 장치가 이미 비었으면(렌더링이 늦었다) 언더런으로 센다.
        if (countGaps && out.getQueuedFrames() == 0) {
            underruns++;
        }
        out.write(data, chunkQuanta * q);
        long queued = out.getQueuedFrames();
        double rate = r.getSampleRate();
        synchronized (timingLock) {
            snapFrames = r.getCurrentFrame();
            snapQueued = queued;
            snapNanos = System.nanoTime();
        }
        // 컨텍스트에 장치 쪽 지연(큐에 쌓인 오디오의 시간)을 알린다.
        if (ctx != null) {
            ctx.reportOutputLatency(queued / rate);
        }
    }
}
