package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioLimits;

/**
 * The background decoder behind <code>decodeAudioData</code> (plan item P25-05): a request carries the bytes of an audio file, the sample rate of
 * the context and a {@link DecodeCallback}; the service decodes it on one of its own threads with {@link AudioBufferDecoder} (so neither the
 * control thread, the rendering thread nor the event dispatch thread ever waits for a decoding, the "decoding thread" of the specification) and
 * reports the result to the callback. The script layer builds on it: it detaches the <code>ArrayBuffer</code>, hands over its bytes, passes an executor that moves the
 * callback to the document queue and settles the promise in the callback.
 * <p>
 * <b>Threads.</b> The service owns a small pool ({@link #DEFAULT_THREADS} threads by default) of daemon threads named
 * <code>swingwebpanel-audio-decode-N</code> that are created when needed and end after {@link #IDLE_MILLIS} without work, so an unused or
 * forgotten service holds no thread. There is no static state: every service is independent (one per context is the intended use).
 * <p>
 * <b>Callbacks and order.</b> The callback runs on the executor given with the request (the decoding thread if there is none) after the result is
 * known. With one thread the requests are decoded and reported in the order in which they were made; with more threads they are decoded
 * in parallel and may be reported in any order (a short sound need not wait for a long one; the specification says that multiple decoding
 * threads can service calls in parallel and does not order the results). Each callback is called exactly once, or never if the request was
 * cancelled. After a request returned from {@link DecodeRequest#cancel()} with <code>true</code> or after {@link #close()} no callback of
 * it is started, even if its result was already waiting in the executor.
 * <p>
 * <b>Limits.</b> At most {@link #MAX_PENDING_REQUESTS} requests that have not finished decoding and at most {@link #MAX_PENDING_BYTES} bytes of
 * their data may be held at a time; {@link #decode} throws <code>NotSupportedError</code> for the one that does not fit (the script layer
 * rejects the promise with it). The service takes the array of a request over: the caller must not change it afterwards. The
 * array is released as soon as the decoding is over.
 * <p>
 * <b>Closing.</b> {@link #close()} (called when the context is closed, the page is left or the component is disposed) cancels every request
 * silently (the running decodings are interrupted and end at their next block), refuses new ones with <code>InvalidStateError</code> and shuts the
 * pool down. It is idempotent.
 * <p>
 * <code>decodeAudioData</code> 뒤의 백그라운드 디코더(계획 P25-05). 요청은 오디오 파일의 바이트, 컨텍스트의 표본율, {@link DecodeCallback} 을 가진다. 서비스는 자기
 * 스레드에서 {@link AudioBufferDecoder} 로 디코딩하고(그래서 제어 스레드, 렌더링 스레드, 이벤트 디스패치 스레드는 디코딩을 기다리지 않는다. 규격의 "디코딩
 * 스레드") 결과를 콜백에 알린다. 스크립트 계층은 이 위에 만든다. <code>ArrayBuffer</code> 를 분리하고 바이트를 넘기며, 콜백을 문서 큐로 옮기는 실행기를
 * 주고, 콜백에서 promise 를 정산한다.
 * <p>
 * <b>스레드.</b> 서비스는 작은 풀({@link #DEFAULT_THREADS} 개가 기본)을 가지며 스레드는 <code>swingwebpanel-audio-decode-N</code> 이라는 이름의 데몬 스레드이다.
 * 필요할 때 만들고 {@link #IDLE_MILLIS} 동안 일이 없으면 끝나므로 쓰지 않거나 잊어버린 서비스도 스레드를 쥐고 있지 않는다. 정적 상태는 없다. 서비스마다
 * 독립이다(컨텍스트마다 하나가 의도된 사용법).
 * <p>
 * <b>콜백과 순서.</b> 콜백은 결과가 나온 뒤 요청과 함께 준 실행기에서(없으면 디코딩 스레드에서) 실행된다. 스레드가 하나이면 요청한 순서대로 디코딩하고 알린다.
 * 스레드가 더 많으면 병렬로 디코딩하며 어떤 순서로든 알릴 수 있다(짧은 소리가 긴 소리를 기다릴 필요가 없다. 규격은 여러 디코딩 스레드가 호출을 병렬로 처리할 수
 * 있다고 하며 결과의 순서는 정하지 않는다). 콜백은 정확히 한 번 불리며 요청이 취소되었으면 불리지 않는다. 요청이 {@link DecodeRequest#cancel()} 에서
 * <code>true</code> 를 반환한 뒤나 {@link #close()} 뒤에는 결과가 이미 실행기에서 기다리고 있었더라도 그 요청의 콜백이 시작되지 않는다.
 * <p>
 * <b>한도.</b> 디코딩이 끝나지 않은 요청은 {@link #MAX_PENDING_REQUESTS} 개, 그 데이터는 {@link #MAX_PENDING_BYTES} 바이트까지만 한 번에 들고 있는다.
 * 들어가지 않는 요청은 {@link #decode} 가 <code>NotSupportedError</code> 를 던진다(스크립트 계층이 그것으로 promise 를 거부한다). 서비스는 요청의 배열을
 * 넘겨받는다. 호출한 쪽은 그 뒤에 바꾸면 안 된다. 배열은 디코딩이 끝나는 즉시 놓는다.
 * <p>
 * <b>닫기.</b> {@link #close()}(컨텍스트를 닫거나 페이지를 떠나거나 컴포넌트를 폐기할 때 부른다)는 모든 요청을 조용히 취소하고(실행 중인 디코딩은
 * 인터럽트되어 다음 블록에서 끝난다) 새 요청은 <code>InvalidStateError</code> 로 거부하며 풀을 종료한다. 여러 번 불러도 안전하다.
 */
public final class AudioDecodeService {

    /**
     * The default number of decoding threads.
     * <p>
     * 디코딩 스레드의 기본 개수.
     */
    public static final int DEFAULT_THREADS = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors()));

    /**
     * The largest number of requests that may wait for or be in decoding at a time.
     * <p>
     * 디코딩을 기다리거나 디코딩 중인 요청의 한 번에 최대 개수.
     */
    public static final int MAX_PENDING_REQUESTS = 64;

    /**
     * The largest total size in bytes of the data of the requests that wait for or are in decoding.
     * <p>
     * 디코딩을 기다리거나 디코딩 중인 요청의 데이터 총 크기(바이트) 최댓값.
     */
    public static final long MAX_PENDING_BYTES = 256L * 1024 * 1024;

    /**
     * The time in milliseconds after which an idle decoding thread ends.
     * <p>
     * 일이 없는 디코딩 스레드가 끝나기까지의 시간(밀리초).
     */
    public static final long IDLE_MILLIS = 5000L;

    /**
     * The pool that decodes.
     * <p>
     * 디코딩하는 풀.
     */
    private final ThreadPoolExecutor executor;

    /**
     * The limit of requests that hold data.
     * <p>
     * 데이터를 들고 있는 요청의 한도.
     */
    private final int maxRequests;

    /**
     * The limit of the bytes of the requests that hold data.
     * <p>
     * 데이터를 들고 있는 요청의 바이트 한도.
     */
    private final long maxBytes;

    /**
     * The lock of the counters and of the set of active requests.
     * <p>
     * 카운터와 활성 요청 집합의 잠금.
     */
    private final Object lock = new Object();

    /**
     * The requests that have not ended (waiting, decoding, or with a result that waits for its callback). Guarded by {@link #lock}.
     * <p>
     * 끝나지 않은 요청(대기 중, 디코딩 중, 결과가 콜백을 기다리는 중). {@link #lock} 으로 보호한다.
     */
    private final Set<Job> active = new HashSet<Job>();

    /**
     * The number of requests that still hold their data. Guarded by {@link #lock}.
     * <p>
     * 아직 데이터를 들고 있는 요청의 수. {@link #lock} 으로 보호한다.
     */
    private int held;

    /**
     * The bytes of the data that the requests hold. Guarded by {@link #lock}.
     * <p>
     * 요청들이 들고 있는 데이터의 바이트 수. {@link #lock} 으로 보호한다.
     */
    private long heldBytes;

    /**
     * True after {@link #close()}. Guarded by {@link #lock}.
     * <p>
     * {@link #close()} 뒤에는 true. {@link #lock} 으로 보호한다.
     */
    private boolean closed;

    /**
     * The number of callbacks that threw an exception (which the service drops).
     * <p>
     * 예외를 던진 콜백의 수(서비스가 그 예외를 버린다).
     */
    private final AtomicInteger callbackFailures = new AtomicInteger();

    /**
     * Creates a service with the default number of threads.
     * <p>
     * 기본 개수의 스레드를 가진 서비스를 만든다.
     */
    public AudioDecodeService() {
        this(DEFAULT_THREADS);
    }

    /**
     * Creates a service with a number of decoding threads.
     * <p>
     * 디코딩 스레드의 개수를 정해 서비스를 만든다.
     *
     * @param threads the number of threads, at least 1 (1 decodes and reports in the order of the requests) / 스레드 수(1 이상. 1 이면 요청한 순서대로 디코딩하고 알린다)
     */
    public AudioDecodeService(int threads) {
        this(threads, IDLE_MILLIS, MAX_PENDING_REQUESTS, MAX_PENDING_BYTES);
    }

    /**
     * Creates a service with all parameters (the limits and the idle time are changed by tests).
     * <p>
     * 모든 매개변수를 정해 서비스를 만든다(한도와 유휴 시간은 시험이 바꾼다).
     *
     * @param threads     the number of threads, at least 1 / 스레드 수(1 이상)
     * @param idleMillis  the time after which an idle thread ends, at least 1 / 일이 없는 스레드가 끝나기까지의 시간(1 이상)
     * @param maxRequests the limit of requests that hold data / 데이터를 들고 있는 요청의 한도
     * @param maxBytes    the limit of the bytes that they hold / 그들이 들고 있는 바이트의 한도
     */
    AudioDecodeService(int threads, long idleMillis, int maxRequests, long maxBytes) {
        if (threads < 1 || idleMillis < 1 || maxRequests < 1 || maxBytes < 1) {
            throw new IllegalArgumentException("the parameters of the service must be positive");
        }
        this.maxRequests = maxRequests;
        this.maxBytes = maxBytes;
        this.executor = new ThreadPoolExecutor(threads, threads, idleMillis, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new DecodeThreads());
        // 코어 스레드도 일이 없으면 끝나도록 하여 쓰지 않는 서비스가 스레드를 쥐지 않게 한다.
        this.executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Starts decoding audio file data in the background (<code>decodeAudioData</code>, Web Audio API 1.1.2).
     * <p>
     * 오디오 파일 데이터의 디코딩을 백그라운드에서 시작한다(<code>decodeAudioData</code>, Web Audio API 1.1.2).
     *
     * @param data             the whole encoded file; the service takes the array over and the caller must not change it / 인코딩된 파일 전체. 서비스가 배열을 넘겨받으며
     *                         호출한 쪽은 바꾸면 안 된다
     * @param mimeHint         the MIME type of the data or null; only a hint / 데이터의 MIME 타입 또는 null. 힌트일 뿐이다
     * @param sampleRate       the sample rate of the context, from 3000 to 768000 Hz / 컨텍스트의 표본율(3000 ~ 768000 Hz)
     * @param callback         the receiver of the result / 결과를 받는 쪽
     * @param callbackExecutor where the callback runs (for example a document queue), or null for the decoding thread / 콜백을 실행할 곳(예: 문서 큐). null 이면 디코딩 스레드
     * @return the handle of the request / 요청의 핸들
     * @throws AudioException           <code>InvalidStateError</code> if the service is closed; <code>NotSupportedError</code> if the limits
     *                                  of pending requests are reached or the sample rate is outside the range / 서비스가 닫혔으면
     *                                  <code>InvalidStateError</code>. 대기 요청의 한도에 닿았거나 표본율이 범위 밖이면 <code>NotSupportedError</code>
     * @throws IllegalArgumentException if <code>data</code> or <code>callback</code> is null / <code>data</code> 나 <code>callback</code> 이 null 이면
     */
    public DecodeRequest decode(byte[] data, String mimeHint, float sampleRate, DecodeCallback callback, Executor callbackExecutor) {
        if (data == null || callback == null) {
            throw new IllegalArgumentException("data and callback must not be null");
        }
        if (!(sampleRate >= WebAudioLimits.MIN_SAMPLE_RATE && sampleRate <= WebAudioLimits.MAX_SAMPLE_RATE)) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "sampleRate " + sampleRate + " is not in " + WebAudioLimits.MIN_SAMPLE_RATE + ".." + WebAudioLimits.MAX_SAMPLE_RATE);
        }
        Job job = new Job(this, data, mimeHint, sampleRate, callback, callbackExecutor);
        // 닫힘과 한도를 확인하고 요청을 등록하는 일을 한 번에 한다.
        synchronized (lock) {
            if (closed) {
                throw new AudioException(AudioException.Kind.INVALID_STATE, "the decoding service is closed");
            }
            if (held >= maxRequests || heldBytes + data.length > maxBytes) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "too many audio data are waiting to be decoded (" + held + " requests, " + heldBytes + " bytes)");
            }
            held++;
            heldBytes += job.size;
            job.held = true;
            active.add(job);
        }
        try {
            executor.execute(job);
        } catch (RejectedExecutionException e) {
            // 닫는 도중에 풀이 거부했으면 등록을 되돌리고 닫힌 서비스로 보고한다.
            job.cancel();
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the decoding service is closed");
        }
        return job;
    }

    /**
     * Closes the service: cancels every request without a callback, refuses new requests and ends the threads. Safe to call more than once and from any
     * thread (also from a callback).
     * <p>
     * 서비스를 닫는다: 모든 요청을 콜백 없이 취소하고 새 요청을 거부하며 스레드를 끝낸다. 여러 번, 어느 스레드에서나(콜백 안에서도) 불러도 안전하다.
     */
    public void close() {
        List<Job> jobs;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            jobs = new ArrayList<Job>(active);
        }
        // 끝나지 않은 요청을 모두 취소한다(콜백은 시작되지 않는다).
        for (Job j : jobs) {
            j.cancel();
        }
        executor.shutdownNow();
    }

    /**
     * Tells whether the service was closed.
     * <p>
     * 서비스가 닫혔는지 알려 준다.
     *
     * @return true after {@link #close()} / {@link #close()} 뒤에는 true
     */
    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /**
     * Returns the number of requests that still hold their data (waiting or being decoded).
     * <p>
     * 아직 데이터를 들고 있는(대기 중이거나 디코딩 중인) 요청의 수를 반환한다.
     *
     * @return the number of requests / 요청 수
     */
    public int getPendingRequestCount() {
        synchronized (lock) {
            return held;
        }
    }

    /**
     * Returns the number of requests that have not ended: those of {@link #getPendingRequestCount()} and those whose result waits for its callback.
     * <p>
     * 끝나지 않은 요청의 수를 반환한다: {@link #getPendingRequestCount()} 의 요청과 결과가 콜백을 기다리는 요청.
     *
     * @return the number of requests / 요청 수
     */
    public int getActiveRequestCount() {
        synchronized (lock) {
            return active.size();
        }
    }

    /**
     * Returns the number of callbacks that threw an exception; the service dropped the exceptions so that other requests are not affected.
     * <p>
     * 예외를 던진 콜백의 수를 반환한다. 다른 요청에 영향을 주지 않도록 서비스가 그 예외를 버렸다.
     *
     * @return the number of failed callbacks / 실패한 콜백 수
     */
    public int getCallbackFailureCount() {
        return callbackFailures.get();
    }

    /**
     * Releases the data of a request from the counters (once per request).
     * <p>
     * 요청의 데이터를 카운터에서 놓는다(요청마다 한 번).
     *
     * @param job the request / 요청
     */
    private void releaseData(Job job) {
        synchronized (lock) {
            if (job.held) {
                job.held = false;
                held--;
                heldBytes -= job.size;
            }
        }
    }

    /**
     * Removes an ended request from the set of active requests.
     * <p>
     * 끝난 요청을 활성 요청 집합에서 뺀다.
     *
     * @param job the request / 요청
     */
    private void forget(Job job) {
        synchronized (lock) {
            active.remove(job);
        }
    }

    /**
     * Creates the decoding threads: daemon threads, a little below the normal priority so that decoding does not compete with the user
     * interface, with a name that tells their owner.
     * <p>
     * 디코딩 스레드를 만든다. 데몬 스레드이며 디코딩이 사용자 인터페이스와 경쟁하지 않도록 보통보다 약간 낮은 우선순위이고 소유자를 알 수 있는 이름을 가진다.
     */
    private static final class DecodeThreads implements ThreadFactory {

        /**
         * The number of the next thread.
         * <p>
         * 다음 스레드의 번호.
         */
        private final AtomicInteger counter = new AtomicInteger();

        /**
         * Creates a thread for a task of the pool.
         * <p>
         * 풀의 작업을 위한 스레드를 만든다.
         *
         * @param r the task / 작업
         * @return the thread (not started) / 시작하지 않은 스레드
         */
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "swingwebpanel-audio-decode-" + counter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    }

    /**
     * One request: the task that the pool runs, the handle of the caller and the flag that the decoder polls. Its state moves
     * <code>PENDING</code> to <code>RUNNING</code> to <code>DELIVERING</code> (the result is known, the callback is queued or running) to
     * <code>COMPLETE</code>, or to <code>CANCELLED</code> from any state before the callback starts.
     * <p>
     * 요청 하나: 풀이 실행하는 작업, 호출한 쪽의 핸들, 디코더가 확인하는 플래그. 상태는 <code>PENDING</code> 에서 <code>RUNNING</code>,
     * <code>DELIVERING</code>(결과가 나왔고 콜백이 대기 중이거나 실행 중), <code>COMPLETE</code> 로 가거나, 콜백이 시작되기 전의 어느 상태에서든
     * <code>CANCELLED</code> 로 간다.
     */
    private static final class Job implements Runnable, DecodeRequest, DecodeCancellation {

        /** Waiting in the queue. / 큐에서 대기 중. */
        private static final int PENDING = 0;

        /** Being decoded. / 디코딩 중. */
        private static final int RUNNING = 1;

        /** The result is known and the callback is queued or running. / 결과가 나왔고 콜백이 대기 중이거나 실행 중. */
        private static final int DELIVERING = 2;

        /** The callback has returned. / 콜백이 반환했다. */
        private static final int COMPLETE = 3;

        /** Cancelled before the callback started. / 콜백이 시작되기 전에 취소되었다. */
        private static final int CANCELLED = 4;

        /**
         * The service.
         * <p>
         * 서비스.
         */
        private final AudioDecodeService service;

        /**
         * The size of the data in bytes.
         * <p>
         * 데이터의 바이트 수.
         */
        private final long size;

        /**
         * The MIME type hint.
         * <p>
         * MIME 타입 힌트.
         */
        private final String mimeHint;

        /**
         * The wanted sample rate.
         * <p>
         * 원하는 표본율.
         */
        private final float sampleRate;

        /**
         * The data; null once the decoding is over. Guarded by this object.
         * <p>
         * 데이터. 디코딩이 끝나면 null. 이 객체로 보호한다.
         */
        private byte[] data;

        /**
         * The receiver; null once the request has ended. Guarded by this object.
         * <p>
         * 받는 쪽. 요청이 끝나면 null. 이 객체로 보호한다.
         */
        private DecodeCallback callback;

        /**
         * Where the callback runs, or null.
         * <p>
         * 콜백을 실행할 곳. 또는 null.
         */
        private final Executor callbackExecutor;

        /**
         * The state. Volatile so that the decoder can poll it; changed under the lock of this object.
         * <p>
         * 상태. 디코더가 확인할 수 있도록 volatile 이다. 이 객체의 잠금 아래에서 바꾼다.
         */
        private volatile int state = PENDING;

        /**
         * The thread that decodes while the state is RUNNING. Guarded by this object.
         * <p>
         * 상태가 RUNNING 인 동안 디코딩하는 스레드. 이 객체로 보호한다.
         */
        private Thread runner;

        /**
         * True once the callback has started. Guarded by this object.
         * <p>
         * 콜백이 시작되었으면 true. 이 객체로 보호한다.
         */
        private boolean callbackStarted;

        /**
         * True while the counters of the service include this request. Guarded by the lock of the service.
         * <p>
         * 서비스의 카운터가 이 요청을 포함하는 동안 true. 서비스의 잠금으로 보호한다.
         */
        boolean held;

        /**
         * Creates a request in the state PENDING.
         * <p>
         * PENDING 상태의 요청을 만든다.
         *
         * @param service          the service / 서비스
         * @param data             the data / 데이터
         * @param mimeHint         the MIME type hint / MIME 타입 힌트
         * @param sampleRate       the wanted sample rate / 원하는 표본율
         * @param callback         the receiver / 받는 쪽
         * @param callbackExecutor where the callback runs or null / 콜백을 실행할 곳 또는 null
         */
        Job(AudioDecodeService service, byte[] data, String mimeHint, float sampleRate, DecodeCallback callback, Executor callbackExecutor) {
            this.service = service;
            this.data = data;
            this.size = data.length;
            this.mimeHint = mimeHint;
            this.sampleRate = sampleRate;
            this.callback = callback;
            this.callbackExecutor = callbackExecutor;
        }

        /**
         * Decodes on the pool thread and delivers the result.
         * <p>
         * 풀 스레드에서 디코딩하고 결과를 전달한다.
         */
        public void run() {
            byte[] bytes;
            synchronized (this) {
                // 큐에서 기다리는 동안 취소되었으면 아무것도 하지 않는다.
                if (state != PENDING) {
                    return;
                }
                state = RUNNING;
                runner = Thread.currentThread();
                bytes = data;
            }
            AudioBuffer buffer = null;
            String warning = null;
            AudioException error = null;
            boolean stopped = false;
            try {
                DecodeResult result = AudioBufferDecoder.decodeResult(bytes, mimeHint, sampleRate, this);
                buffer = result.getBuffer();
                warning = result.getWarning();
            } catch (CancellationException e) {
                stopped = true;
            } catch (AudioException e) {
                error = e;
            } catch (RuntimeException e) {
                error = AudioBufferDecoder.encodingError("the decoding failed: " + e, e);
            } catch (OutOfMemoryError e) {
                error = AudioBufferDecoder.encodingError("not enough memory to decode the audio data", e);
            }
            // 콜백이 이 스레드에서 실행되더라도 파일 데이터를 붙잡고 있지 않도록 참조를 놓는다.
            bytes = null;
            boolean deliver;
            synchronized (this) {
                runner = null;
                // 취소가 남긴 인터럽트 상태를 풀 스레드에서 지운다.
                Thread.interrupted();
                data = null;
                if (state == CANCELLED || stopped) {
                    state = CANCELLED;
                    callback = null;
                    deliver = false;
                } else {
                    state = DELIVERING;
                    deliver = true;
                }
            }
            service.releaseData(this);
            if (!deliver) {
                service.forget(this);
                return;
            }
            schedule(buffer, error, warning);
        }

        /**
         * Runs the callback on the executor of the request (or here).
         * <p>
         * 요청의 실행기에서(없으면 여기서) 콜백을 실행한다.
         *
         * @param buffer the result or null / 결과 또는 null
         * @param error  the error or null / 오류 또는 null
         * @param warning the warning for a result that holds only part of the data, or null / 데이터의 일부만 담은 결과에 대한 경고. 또는 null
         */
        private void schedule(final AudioBuffer buffer, final AudioException error, final String warning) {
            Runnable call = new Runnable() {
                public void run() {
                    deliver(buffer, error, warning);
                }
            };
            if (callbackExecutor == null) {
                call.run();
                return;
            }
            try {
                callbackExecutor.execute(call);
            } catch (RuntimeException rejected) {
                // 실행기가 거부하면(예: 문서 큐가 닫힘) 콜백은 버린다.
                synchronized (this) {
                    state = COMPLETE;
                    callback = null;
                }
                service.forget(this);
            }
        }

        /**
         * Calls the callback unless the request was cancelled in the meantime.
         * <p>
         * 그 사이에 요청이 취소되지 않았으면 콜백을 부른다.
         *
         * @param buffer the result or null / 결과 또는 null
         * @param error  the error or null / 오류 또는 null
         * @param warning the warning for a result that holds only part of the data, or null / 데이터의 일부만 담은 결과에 대한 경고. 또는 null
         */
        private void deliver(AudioBuffer buffer, AudioException error, String warning) {
            DecodeCallback target;
            synchronized (this) {
                // 콜백이 시작되기 전에 취소되었으면 부르지 않는다.
                if (state != DELIVERING || callbackStarted) {
                    return;
                }
                callbackStarted = true;
                target = callback;
            }
            try {
                if (buffer != null) {
                    if (warning != null && target instanceof DecodeWarningCallback) {
                        ((DecodeWarningCallback) target).onWarning(warning);
                    }
                    target.onSuccess(buffer);
                } else {
                    target.onError(error);
                }
            } catch (RuntimeException e) {
                // 콜백이 던진 예외는 다른 요청에 영향을 주지 않도록 버리고 횟수만 센다.
                service.callbackFailures.incrementAndGet();
            } finally {
                synchronized (this) {
                    state = COMPLETE;
                    callback = null;
                }
                service.forget(this);
            }
        }

        /**
         * Cancels the request (see {@link DecodeRequest#cancel()}).
         * <p>
         * 요청을 취소한다({@link DecodeRequest#cancel()} 참조).
         *
         * @return true if it was cancelled before the callback started / 콜백이 시작되기 전에 취소되었으면 true
         */
        public boolean cancel() {
            boolean wasPending = false;
            synchronized (this) {
                switch (state) {
                case PENDING:
                    // 아직 시작하지 않았으면 큐에서 뺀다.
                    state = CANCELLED;
                    data = null;
                    callback = null;
                    wasPending = true;
                    break;
                case RUNNING:
                    // 실행 중이면 취소로 표시하고 디코딩 스레드를 깨운다. 정리는 그 스레드가 한다.
                    state = CANCELLED;
                    callback = null;
                    if (runner != null) {
                        runner.interrupt();
                    }
                    return true;
                case DELIVERING:
                    // 콜백이 아직 시작하지 않았으면 결과를 버린다.
                    if (callbackStarted) {
                        return false;
                    }
                    state = CANCELLED;
                    callback = null;
                    break;
                case CANCELLED:
                    return true;
                default:
                    return false;
                }
            }
            // 큐에서 기다리던 요청이면 풀의 큐에서도 빼고 카운터를 되돌린다.
            if (wasPending) {
                service.executor.remove(this);
                service.releaseData(this);
            }
            service.forget(this);
            return true;
        }

        /**
         * Tells whether the request was cancelled.
         * <p>
         * 요청이 취소되었는지 알려 준다.
         *
         * @return true if cancelled / 취소되었으면 true
         */
        public boolean isCancelled() {
            return state == CANCELLED;
        }

        /**
         * Tells whether the request is over.
         * <p>
         * 요청이 끝났는지 알려 준다.
         *
         * @return true if the callback has returned or the request was cancelled / 콜백이 반환했거나 요청이 취소되었으면 true
         */
        public boolean isDone() {
            int s = state;
            return s == COMPLETE || s == CANCELLED;
        }
    }
}
