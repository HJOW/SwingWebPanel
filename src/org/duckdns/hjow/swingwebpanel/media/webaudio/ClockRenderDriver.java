package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The driver of a real-time context that has no sound device (or does not use one): a daemon thread renders the graph at the pace of the wall clock
 * and throws the output away, so the <code>currentTime</code> of the context advances like the time of a context that plays, the sources start and
 * end on time, the analysers and the script processors see the audio, and nothing is heard. It is the default driver of the script layer and the
 * fallback of a driver that cannot open a device.
 * <p>
 * Pacing: the thread renders a quantum when the number of frames rendered is behind the number of frames that the wall clock has produced since the
 * driver started (plus a small lead of {@link #LEAD_QUANTA} quanta, so that the thread does not wake up exactly at the end of a quantum), and sleeps
 * otherwise. After a pause (a suspended context or a long stop of the process) it renders the frames that are due at once and then keeps the pace,
 * but never more than {@link #MAX_CATCH_UP_QUANTA} quanta in a row, so that a stall does not starve the other threads. Suspending resets the base of the
 * clock, so the time does not jump when the context is resumed.
 * <p>
 * The thread is created when the driver starts for the first time and ends when the driver is closed. A driver renders one context.
 * <p>
 * 소리 장치가 없는(또는 쓰지 않는) 실시간 컨텍스트의 드라이버: 데몬 스레드가 벽시계의 속도로 그래프를 렌더링하고 출력을 버린다. 그래서 컨텍스트의
 * <code>currentTime</code> 이 소리를 내는 컨텍스트의 시간처럼 흐르고, 원천이 제때 시작하고 끝나며, 분석기와 스크립트 프로세서가 오디오를 보지만 아무 소리도
 * 나지 않는다. 스크립트 계층의 기본 드라이버이며 장치를 열지 못하는 드라이버의 대체이다.
 * <p>
 * 속도 맞춤: 렌더링한 프레임 수가 드라이버가 시작한 뒤 벽시계가 만든 프레임 수(와 작은 여유 {@link #LEAD_QUANTA} quantum, 스레드가 quantum 끝에 정확히 깨지
 * 않도록)보다 뒤처지면 quantum 을 렌더링하고, 아니면 잠든다. 멈춤(일시 중단된 컨텍스트나 프로세스의 긴 정지) 뒤에는 밀린 프레임을 한꺼번에 렌더링한 뒤 속도를
 * 유지하되, 정지가 다른 스레드를 굶기지 않도록 연달아 {@link #MAX_CATCH_UP_QUANTA} quantum 을 넘지 않는다. 일시 중단하면 시계의 기준을 다시 잡으므로 재개할 때
 * 시간이 튀지 않는다.
 * <p>
 * 스레드는 드라이버가 처음 시작할 때 만들고 드라이버를 닫을 때 끝난다. 드라이버 하나가 컨텍스트 하나를 렌더링한다.
 */
public final class ClockRenderDriver implements RealtimeAudioContext.Driver, Runnable {

    /**
     * The number of quanta that the rendering runs ahead of the wall clock.
     * <p>
     * 렌더링이 벽시계보다 앞서 가는 quantum 수.
     */
    public static final int LEAD_QUANTA = 1;

    /**
     * The largest number of quanta that are rendered in a row to catch up with the wall clock.
     * <p>
     * 벽시계를 따라잡으려고 연달아 렌더링하는 quantum 의 최대 수.
     */
    public static final int MAX_CATCH_UP_QUANTA = 64;

    /**
     * The name of the rendering thread.
     * <p>
     * 렌더링 스레드의 이름.
     */
    private final String threadName;

    /**
     * Guards the state of the driver.
     * <p>
     * 드라이버의 상태를 지킨다.
     */
    private final Object lock = new Object();

    /**
     * The renderer of the context (guarded by {@link #lock}).
     * <p>
     * 컨텍스트의 렌더러({@link #lock} 으로 보호).
     */
    private RealtimeAudioContext.Renderer renderer;

    /**
     * The rendering thread, or null before the first start (guarded by {@link #lock}).
     * <p>
     * 렌더링 스레드. 첫 시작 전에는 null({@link #lock} 으로 보호).
     */
    private Thread thread;

    /**
     * True while the context runs (guarded by {@link #lock}).
     * <p>
     * 컨텍스트가 도는 동안 true({@link #lock} 으로 보호).
     */
    private boolean running;

    /**
     * True after {@link #close()} (guarded by {@link #lock}).
     * <p>
     * {@link #close()} 뒤 true({@link #lock} 으로 보호).
     */
    private boolean closed;

    /**
     * The wall clock reading when the current run started, in nanoseconds (guarded by {@link #lock}).
     * <p>
     * 현재 실행이 시작됐을 때의 벽시계 값(나노초, {@link #lock} 으로 보호).
     */
    private long baseNanos;

    /**
     * The frame of the context at {@link #baseNanos} (guarded by {@link #lock}).
     * <p>
     * {@link #baseNanos} 에서의 컨텍스트 프레임({@link #lock} 으로 보호).
     */
    private long baseFrame;

    /**
     * Creates a driver.
     * <p>
     * 드라이버를 만든다.
     *
     * @param threadName the name of the rendering thread, for example <code>swingwebpanel-audio-render-1</code> / 렌더링 스레드의 이름. 예: <code>swingwebpanel-audio-render-1</code>
     */
    public ClockRenderDriver(String threadName) {
        if (threadName == null) {
            throw new IllegalArgumentException("threadName must not be null");
        }
        this.threadName = threadName;
    }

    /**
     * Starts or restarts the rendering; the clock continues from the current frame of the context.
     * <p>
     * 렌더링을 시작하거나 다시 시작한다. 시계는 컨텍스트의 현재 프레임에서 이어진다.
     *
     * @param r the renderer of the context / 컨텍스트의 렌더러
     */
    public void start(RealtimeAudioContext.Renderer r) {
        synchronized (lock) {
            if (closed) {
                return;
            }
            renderer = r;
            baseNanos = System.nanoTime();
            baseFrame = r.getCurrentFrame();
            running = true;
            // 스레드는 처음 시작할 때 만든다. 이미 있으면 깨우기만 한다.
            if (thread == null) {
                Thread t = new Thread(this, threadName);
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY + 1);
                thread = t;
                t.start();
            } else {
                lock.notifyAll();
            }
        }
    }

    /**
     * Stops the rendering.
     * <p>
     * 렌더링을 멈춘다.
     */
    public void pause() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
    }

    /**
     * Stops the rendering for good; the thread ends.
     * <p>
     * 렌더링을 완전히 멈춘다. 스레드가 끝난다.
     */
    public void close() {
        synchronized (lock) {
            closed = true;
            running = false;
            lock.notifyAll();
        }
    }

    /**
     * Returns the thread of the driver, or null before the first start (a test checks its name and that it ends).
     * <p>
     * 드라이버의 스레드를 반환한다. 첫 시작 전에는 null(시험이 그 이름과 종료를 확인한다).
     *
     * @return the thread or null / 스레드 또는 null
     */
    public Thread getThread() {
        synchronized (lock) {
            return thread;
        }
    }

    /**
     * The loop of the rendering thread: waits while the context is suspended, renders the quanta that are due and sleeps until the next one is.
     * <p>
     * 렌더링 스레드의 루프: 컨텍스트가 일시 중단된 동안 기다리고, 밀린 quantum 을 렌더링하고, 다음 quantum 때까지 잔다.
     */
    public void run() {
        // 닫힐 때까지 되풀이한다.
        for (;;) {
            RealtimeAudioContext.Renderer r;
            long nanos;
            long frame;
            synchronized (lock) {
                // 일시 중단된 동안과 아직 시작하지 않은 동안 기다린다.
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
                nanos = baseNanos;
                frame = baseFrame;
            }
            double rate = r.getSampleRate();
            int quantum = r.getRenderQuantumSize();
            long elapsed = System.nanoTime() - nanos;
            long due = frame + (long) (elapsed * rate / 1000000000.0) + (long) LEAD_QUANTA * quantum;
            int rendered = 0;
            // 벽시계가 만든 프레임까지 밀린 quantum 을 렌더링한다(연달아 한도를 넘지 않게).
            while (r.getCurrentFrame() < due && rendered < MAX_CATCH_UP_QUANTA) {
                r.renderQuantum();
                rendered++;
            }
            if (rendered == 0) {
                // 다음 quantum 이 필요한 시각까지 잔다(1 밀리초 단위 이상). 일시 중단과 닫기는 알림으로 깨운다.
                long waitNanos = (long) ((r.getCurrentFrame() - frame - (long) LEAD_QUANTA * quantum) * 1000000000.0 / rate) - elapsed;
                long millis = Math.max(1L, waitNanos / 1000000L);
                synchronized (lock) {
                    if (!closed && running && baseNanos == nanos) {
                        try {
                            lock.wait(millis);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            }
        }
    }
}
