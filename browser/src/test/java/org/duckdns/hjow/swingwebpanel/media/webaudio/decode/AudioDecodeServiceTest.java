package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioTestKit;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the background decoding service (plan item P25-05): the result on the decoding thread or on an executor of the caller, errors,
 * the order with one thread and the parallel decoding with several, cancellation in every state of a request (waiting, decoding, result
 * waiting for its callback, callback running), the closing of the service, the limits of pending requests, the isolation of a failing
 * callback, and the lifetime of the threads (daemon threads that are named after their owner, none without requests, idle ones end, none left
 * after the closing). The time limits are generous (seconds) because the machine may be loaded; no test depends on a decoding being slow.
 * <p>
 * 백그라운드 디코딩 서비스의 시험(계획 P25-05): 디코딩 스레드나 호출한 쪽의 실행기에서의 결과, 오류, 스레드 하나일 때의 순서와 여러 개일 때의 병렬 디코딩,
 * 요청의 모든 상태(대기, 디코딩, 결과가 콜백을 기다림, 콜백 실행 중)에서의 취소, 서비스 닫기, 대기 요청의 한도, 실패하는 콜백의 격리, 스레드의 수명(소유자
 * 이름이 붙은 데몬 스레드, 요청이 없으면 없음, 유휴 스레드는 끝남, 닫은 뒤 남지 않음). 시간 제한은 PC 에 부하가 있을 수 있으므로 넉넉하다(초 단위).
 * 어떤 시험도 디코딩이 느리다는 데 기대지 않는다.
 */
public class AudioDecodeServiceTest {

    /**
     * The name prefix of the decoding threads.
     * <p>
     * 디코딩 스레드의 이름 접두사.
     */
    private static final String PREFIX = "swingwebpanel-audio-decode-";

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public AudioDecodeServiceTest() {
    }

    /**
     * A condition that a test waits for.
     * <p>
     * 시험이 기다리는 조건.
     */
    private abstract static class Condition {

        /**
         * Tells whether the condition holds.
         * <p>
         * 조건이 성립하는지 알려 준다.
         *
         * @return true if it holds / 성립하면 true
         */
        abstract boolean holds();
    }

    /**
     * Waits until a condition holds or the time is over.
     * <p>
     * 조건이 성립하거나 시간이 다할 때까지 기다린다.
     *
     * @param c      the condition / 조건
     * @param millis the longest time to wait / 기다리는 최대 시간
     * @return true if the condition holds / 조건이 성립하면 true
     */
    private static boolean await(Condition c, long millis) {
        long end = System.currentTimeMillis() + millis;
        // 조건이 성립하거나 시간이 다할 때까지 짧게 쉬며 확인한다.
        while (System.currentTimeMillis() < end) {
            if (c.holds()) {
                return true;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return c.holds();
            }
        }
        return c.holds();
    }

    /**
     * Counts the live decoding threads of all services.
     * <p>
     * 모든 서비스의 살아 있는 디코딩 스레드 수를 센다.
     *
     * @return the number of threads / 스레드 수
     */
    private static int liveDecodeThreads() {
        int n = 0;
        // 살아 있는 모든 스레드 중 이름 접두사가 맞는 것을 센다.
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith(PREFIX) && t.isAlive()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Creates a service that is cheap to leave behind: threads end after 300 ms of idleness.
     * <p>
     * 놔두어도 부담이 작은 서비스를 만든다: 스레드는 300 ms 동안 일이 없으면 끝난다.
     *
     * @param threads the number of threads / 스레드 수
     * @return the service / 서비스
     */
    private static AudioDecodeService service(int threads) {
        return new AudioDecodeService(threads, 300, AudioDecodeService.MAX_PENDING_REQUESTS, AudioDecodeService.MAX_PENDING_BYTES);
    }

    /**
     * Builds a small WAV file.
     * <p>
     * 작은 WAV 파일을 만든다.
     *
     * @param frames the number of frames at 8000 Hz, mono / 8000 Hz 모노의 프레임 수
     * @return the file / 파일
     */
    private static byte[] smallWav(int frames) {
        return AudioFiles.sineWav(8000, frames, new double[] {300}, 0.5);
    }

    /**
     * A callback that records what it is called with.
     * <p>
     * 불린 내용을 기록하는 콜백.
     */
    private static class Recorder implements DecodeCallback {

        /** Released by the first call. / 첫 호출이 해제한다. */
        final CountDownLatch done = new CountDownLatch(1);

        /** The number of calls. / 호출 횟수. */
        final AtomicInteger calls = new AtomicInteger();

        /** The buffer or null. / 버퍼 또는 null. */
        volatile AudioBuffer buffer;

        /** The error or null. / 오류 또는 null. */
        volatile AudioException error;

        /** The thread of the call. / 호출한 스레드. */
        volatile Thread thread;

        /**
         * Records a success.
         * <p>
         * 성공을 기록한다.
         *
         * @param b the buffer / 버퍼
         */
        public void onSuccess(AudioBuffer b) {
            thread = Thread.currentThread();
            buffer = b;
            calls.incrementAndGet();
            done.countDown();
        }

        /**
         * Records an error.
         * <p>
         * 오류를 기록한다.
         *
         * @param e the error / 오류
         */
        public void onError(AudioException e) {
            thread = Thread.currentThread();
            error = e;
            calls.incrementAndGet();
            done.countDown();
        }

        /**
         * Waits for the call.
         * <p>
         * 호출을 기다린다.
         *
         * @return true if it came / 호출이 왔으면 true
         * @throws InterruptedException if interrupted / 인터럽트되면
         */
        boolean await() throws InterruptedException {
            return done.await(30, TimeUnit.SECONDS);
        }
    }

    /**
     * A callback that blocks the thread that calls it until it is released (it holds a decoding thread busy, so that later requests
     * wait in the queue).
     * <p>
     * 호출한 스레드를 해제될 때까지 막는 콜백(디코딩 스레드를 붙잡아 두어 이후의 요청이 큐에서 기다리게 한다).
     */
    private static final class Gate extends Recorder {

        /** Released when the call has started. / 호출이 시작되면 해제된다. */
        final CountDownLatch entered = new CountDownLatch(1);

        /** The call returns when this is released. / 이것이 해제되면 호출이 반환한다. */
        final CountDownLatch release = new CountDownLatch(1);

        /**
         * Blocks.
         * <p>
         * 막는다.
         *
         * @param b the buffer / 버퍼
         */
        @Override
        public void onSuccess(AudioBuffer b) {
            entered.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // 서비스를 닫으면 스레드가 인터럽트되며, 그때는 그냥 반환한다.
                Thread.currentThread().interrupt();
            }
            super.onSuccess(b);
        }
    }

    /**
     * An executor that keeps what it is given, so that a test decides when it runs.
     * <p>
     * 받은 것을 보관하는 실행기. 언제 실행할지 시험이 정한다.
     */
    private static final class Holding implements Executor {

        /** The tasks that wait. / 대기 중인 작업. */
        final List<Runnable> tasks = Collections.synchronizedList(new ArrayList<Runnable>());

        /**
         * Keeps the task.
         * <p>
         * 작업을 보관한다.
         *
         * @param r the task / 작업
         */
        public void execute(Runnable r) {
            tasks.add(r);
        }
    }

    /**
     * A successful request: the result arrives on a thread of the service (a daemon thread with the name of its owner, a little below the
     * normal priority), with the channels, the length and the rate that were asked for.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testTheResultIsReportedOnTheDecodingThread() throws Exception {
        AudioDecodeService s = service(2);
        try {
            Recorder r = new Recorder();
            DecodeRequest req = s.decode(smallWav(8000), "audio/wav", 48000f, r, null);
            Check.isTrue(r.await(), "the callback came");
            Check.equal(1, r.calls.get(), "called once");
            Check.notNull(r.buffer, "a buffer");
            Check.isNull(r.error, "no error");
            Check.equal(48000, r.buffer.getLength(), "one second at 48000 Hz");
            Check.near(48000, r.buffer.getSampleRate(), 0, "rate");
            Check.isTrue(r.thread.getName().startsWith(PREFIX), "decoded on " + r.thread.getName());
            Check.isTrue(r.thread.isDaemon(), "a daemon thread");
            Check.isTrue(r.thread.getPriority() < Thread.NORM_PRIORITY, "a priority below the normal one");
            Check.isTrue(r.thread != Thread.currentThread(), "not the calling thread");
            Check.isTrue(await(isDone(req), 5000), "the request is over after the callback");
            Check.isFalse(req.isCancelled(), "not cancelled");
            Check.isFalse(req.cancel(), "a completed request cannot be cancelled");
        } finally {
            s.close();
        }
    }

    /**
     * Makes the condition "the request is done".
     * <p>
     * "요청이 끝났다" 조건을 만든다.
     *
     * @param req the request / 요청
     * @return the condition / 조건
     */
    private static Condition isDone(final DecodeRequest req) {
        return new Condition() {
            boolean holds() {
                return req.isDone();
            }
        };
    }

    /**
     * A callback that implements {@link DecodeWarningCallback} hears of a result that holds only part of a cut-off file: <code>onWarning</code> comes first,
     * then <code>onSuccess</code> with the frames that could be decoded, on the same thread; a plain {@link DecodeCallback} gets the same buffer from
     * <code>onSuccess</code> and nothing else; a complete file gives no warning.
     * <p>
     * {@link DecodeWarningCallback} 을 구현한 콜백은 잘린 파일의 일부만 담은 결과를 안다. <code>onWarning</code> 이 먼저 오고, 같은 스레드에서 디코딩할 수 있었던 프레임과 함께
     * <code>onSuccess</code> 가 온다. 평범한 {@link DecodeCallback} 은 같은 버퍼를 <code>onSuccess</code> 로 받을 뿐 다른 것은 없다. 완전한 파일은 경고를 주지 않는다.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testTheWarningOfACutOffFileComesBeforeSuccess() throws Exception {
        AudioDecodeService s = service(1);
        try {
            byte[] whole = TruncatedDataTest.resource("long_flac_8000_st.flac");
            final List<String> events = Collections.synchronizedList(new ArrayList<String>());
            final AtomicReference<Thread> warnThread = new AtomicReference<Thread>();
            final AtomicReference<Thread> successThread = new AtomicReference<Thread>();
            final CountDownLatch done = new CountDownLatch(1);
            s.decode(java.util.Arrays.copyOf(whole, whole.length * 85 / 100), null, 8000f, new DecodeWarningCallback() {
                public void onWarning(String message) {
                    warnThread.set(Thread.currentThread());
                    events.add("warning: " + message);
                }

                public void onSuccess(AudioBuffer b) {
                    successThread.set(Thread.currentThread());
                    events.add("success " + b.getLength());
                    done.countDown();
                }

                public void onError(AudioException e) {
                    events.add("error " + e);
                    done.countDown();
                }
            }, null);
            Check.isTrue(done.await(30, TimeUnit.SECONDS), "the callback came");
            Check.equal(2, events.size(), "two calls: " + events);
            Check.isTrue(events.get(0).startsWith("warning: the audio data is cut off"), "the warning comes first: " + events.get(0));
            Check.isTrue(events.get(1).startsWith("success "), "then the success: " + events.get(1));
            Check.same(warnThread.get(), successThread.get(), "on the same thread");
            Recorder plain = new Recorder();
            s.decode(java.util.Arrays.copyOf(whole, whole.length * 85 / 100), null, 8000f, plain, null);
            Check.isTrue(plain.await(), "a plain callback is called");
            Check.notNull(plain.buffer, "with the same buffer");
            Check.isNull(plain.error, "and no error");
            final List<String> complete = Collections.synchronizedList(new ArrayList<String>());
            final CountDownLatch done2 = new CountDownLatch(1);
            s.decode(whole, null, 8000f, new DecodeWarningCallback() {
                public void onWarning(String message) {
                    complete.add("warning");
                }

                public void onSuccess(AudioBuffer b) {
                    complete.add("success");
                    done2.countDown();
                }

                public void onError(AudioException e) {
                    done2.countDown();
                }
            }, null);
            Check.isTrue(done2.await(30, TimeUnit.SECONDS), "the callback came for the whole file");
            Check.equal("[success]", complete.toString(), "no warning for a complete file");
        } finally {
            s.close();
        }
    }

    /**
     * A file that cannot be decoded and empty data are reported through the error callback (and not by an exception of the call), as an
     * <code>EncodingError</code> with the cause.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testErrorsAreReportedThroughTheCallback() throws Exception {
        AudioDecodeService s = service(1);
        try {
            byte[][] bad = {new byte[0], "not audio at all".getBytes("UTF-8"), new byte[] {1, 2, 3}};
            // 디코딩할 수 없는 데이터마다 오류 콜백이 한 번 와야 한다.
            for (int i = 0; i < bad.length; i++) {
                Recorder r = new Recorder();
                s.decode(bad[i], null, 44100f, r, null);
                Check.isTrue(r.await(), "the callback came for " + i);
                Check.isNull(r.buffer, "no buffer for " + i);
                Check.notNull(r.error, "an error for " + i);
                Check.equal(AudioException.Kind.ENCODING, r.error.getKind(), "kind for " + i);
                Check.notNull(r.error.getCause(), "cause for " + i);
            }
        } finally {
            s.close();
        }
    }

    /**
     * With an executor of the caller the callback runs there and not on the decoding thread; the order of the callbacks on that executor is
     * the order in which the results were reported.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testTheCallbackRunsOnTheExecutorOfTheCaller() throws Exception {
        AudioDecodeService s = service(1);
        final Holding exec = new Holding();
        try {
            Recorder r = new Recorder();
            s.decode(smallWav(800), null, 8000f, r, exec);
            Check.isTrue(await(new Condition() {
                boolean holds() {
                    return exec.tasks.size() == 1;
                }
            }, 10000), "the result waits on the executor");
            Check.equal(0, r.calls.get(), "the callback has not run yet");
            exec.tasks.get(0).run();
            Check.equal(1, r.calls.get(), "run by the executor");
            Check.same(Thread.currentThread(), r.thread, "on the thread that runs the task");
        } finally {
            s.close();
        }
    }

    /**
     * With one thread the requests are decoded and reported in the order in which they were made, also when a short request follows a long one.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testOneThreadKeepsTheOrder() throws Exception {
        AudioDecodeService s = service(1);
        try {
            final List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
            final CountDownLatch all = new CountDownLatch(12);
            // 긴 요청과 짧은 요청을 섞어 차례로 요청한다.
            for (int i = 0; i < 12; i++) {
                final int id = i;
                s.decode(smallWav(i % 3 == 0 ? 60000 : 100 + i), null, 8000f, new DecodeCallback() {
                    public void onSuccess(AudioBuffer b) {
                        order.add(id);
                        all.countDown();
                    }

                    public void onError(AudioException e) {
                        order.add(-id - 1);
                        all.countDown();
                    }
                }, null);
            }
            Check.isTrue(all.await(60, TimeUnit.SECONDS), "all callbacks came");
            List<Integer> want = new ArrayList<Integer>();
            // 기대하는 완료 순서 0, 1, 2, ... 를 만든다.
            for (int i = 0; i < 12; i++) {
                want.add(i);
            }
            Check.equal(want.toString(), order.toString(), "the order of the callbacks");
        } finally {
            s.close();
        }
    }

    /**
     * With several threads many requests are decoded in parallel and every one is reported exactly once with its own result (the results are
     * told apart by their lengths); no particular order is promised.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testParallelRequestsAreAllReportedOnce() throws Exception {
        AudioDecodeService s = service(3);
        try {
            int n = 60;
            final int[] lengths = new int[n];
            final AtomicInteger[] calls = new AtomicInteger[n];
            final CountDownLatch all = new CountDownLatch(n);
            final AtomicReference<String> problem = new AtomicReference<String>();
            // 요청마다 길이가 다른 파일을 만들어 결과를 구별할 수 있게 한다.
            for (int i = 0; i < n; i++) {
                final int id = i;
                lengths[i] = 500 + i * 37;
                calls[i] = new AtomicInteger();
                s.decode(smallWav(lengths[i]), null, 8000f, new DecodeCallback() {
                    public void onSuccess(AudioBuffer b) {
                        calls[id].incrementAndGet();
                        if (b.getLength() != lengths[id]) {
                            problem.compareAndSet(null, "request " + id + " got " + b.getLength() + " frames instead of " + lengths[id]);
                        }
                        all.countDown();
                    }

                    public void onError(AudioException e) {
                        calls[id].incrementAndGet();
                        problem.compareAndSet(null, "request " + id + ": " + e);
                        all.countDown();
                    }
                }, null);
            }
            Check.isTrue(all.await(60, TimeUnit.SECONDS), "all callbacks came");
            Check.isNull(problem.get(), "a wrong result");
            // 모든 요청의 콜백이 정확히 한 번씩 불렸는지 확인한다.
            for (int i = 0; i < n; i++) {
                Check.equal(1, calls[i].get(), "calls of request " + i);
            }
        } finally {
            s.close();
        }
    }

    /**
     * A request that is still in the queue is cancelled: it is removed at once (the counters go down), its callback never comes, and the
     * request that had the decoding thread is not disturbed.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCancelBeforeItStarts() throws Exception {
        final AudioDecodeService s = service(1);
        try {
            Gate gate = new Gate();
            s.decode(smallWav(100), null, 8000f, gate, null);
            Check.isTrue(gate.entered.await(30, TimeUnit.SECONDS), "the first callback is running, so the thread is busy");
            Recorder second = new Recorder();
            Recorder third = new Recorder();
            DecodeRequest r2 = s.decode(smallWav(200), null, 8000f, second, null);
            s.decode(smallWav(300), null, 8000f, third, null);
            Check.equal(2, s.getPendingRequestCount(), "two requests wait");
            Check.equal(3, s.getActiveRequestCount(), "and one has its callback running");
            Check.isTrue(r2.cancel(), "the waiting request is cancelled");
            Check.isTrue(r2.isCancelled() && r2.isDone(), "cancelled and over");
            Check.isTrue(r2.cancel(), "cancelling again still reports it as cancelled");
            Check.equal(1, s.getPendingRequestCount(), "one request waits");
            Check.equal(2, s.getActiveRequestCount(), "two have not ended");
            gate.release.countDown();
            Check.isTrue(third.await(), "the third request is served");
            Check.equal(0, second.calls.get(), "the cancelled request never calls back");
            Check.isTrue(await(new Condition() {
                boolean holds() {
                    return s.getActiveRequestCount() == 0;
                }
            }, 10000), "everything ended");
        } finally {
            s.close();
        }
    }

    /**
     * A decoding that is running is cancelled: it ends at its next block (long before it would have ended by itself), no callback comes,
     * and the thread of the pool, interrupted by the cancellation, serves the next request normally.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCancelWhileDecoding() throws Exception {
        final AudioDecodeService s = service(1);
        try {
            // 96 kHz 스테레오 40초(약 7.7MB)를 44.1 kHz 로 변환하는 일은 수 초가 걸린다.
            byte[] big = AudioFiles.sineWav(96000, 96000 * 40, new double[] {200, 300}, 0.5);
            Recorder r = new Recorder();
            DecodeRequest req = s.decode(big, null, 44100f, r, null);
            Thread.sleep(150);
            long t0 = System.currentTimeMillis();
            boolean cancelled = req.cancel();
            // 아주 느린 PC 가 아니면 150 ms 뒤에도 디코딩 중이다. 이미 끝났으면 취소는 실패한다.
            Check.isTrue(cancelled, "the decoding was still running after 150 ms");
            Check.isTrue(await(new Condition() {
                boolean holds() {
                    return s.getActiveRequestCount() == 0;
                }
            }, 10000), "the request left the service");
            long stopped = System.currentTimeMillis() - t0;
            Check.isTrue(stopped < 3000, "the decoding stopped " + stopped + " ms after the cancellation");
            Check.equal(0, r.calls.get(), "no callback");
            Check.isTrue(req.isCancelled(), "cancelled");
            Recorder next = new Recorder();
            s.decode(smallWav(800), null, 8000f, next, null);
            Check.isTrue(next.await(), "the next request is served");
            Check.notNull(next.buffer, "by the same pool");
            Check.isFalse(next.thread.isInterrupted(), "the thread was not left interrupted");
        } finally {
            s.close();
        }
    }

    /**
     * A result that waits for its callback (on an executor that holds it) is dropped by the cancellation: the callback does not start even
     * if the executor runs it later.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCancelAfterTheResultWaitsForItsCallback() throws Exception {
        final AudioDecodeService s = service(1);
        final Holding exec = new Holding();
        try {
            Recorder r = new Recorder();
            DecodeRequest req = s.decode(smallWav(800), null, 8000f, r, exec);
            Check.isTrue(await(new Condition() {
                boolean holds() {
                    return exec.tasks.size() == 1;
                }
            }, 10000), "the result waits on the executor");
            Check.equal(1, s.getActiveRequestCount(), "the request has not ended");
            Check.equal(0, s.getPendingRequestCount(), "but it holds no data any more");
            Check.isTrue(req.cancel(), "cancelled before the callback started");
            exec.tasks.get(0).run();
            Check.equal(0, r.calls.get(), "the callback did not start");
            Check.equal(0, s.getCallbackFailureCount(), "the callback was not even attempted (no failure of a missing receiver)");
            Check.equal(0, s.getActiveRequestCount(), "the request left the service");
        } finally {
            s.close();
        }
    }

    /**
     * A callback that cancels its own request cannot undo it: the call returns <code>false</code> (the callback is running) and the request
     * is done afterwards.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCancelInsideTheCallbackReturnsFalse() throws Exception {
        AudioDecodeService s = service(1);
        try {
            final AtomicReference<DecodeRequest> holder = new AtomicReference<DecodeRequest>();
            final AtomicReference<Boolean> result = new AtomicReference<Boolean>();
            final CountDownLatch done = new CountDownLatch(1);
            final CountDownLatch set = new CountDownLatch(1);
            DecodeRequest req = s.decode(smallWav(800), null, 8000f, new DecodeCallback() {
                public void onSuccess(AudioBuffer b) {
                    try {
                        set.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    result.set(holder.get().cancel());
                    done.countDown();
                }

                public void onError(AudioException e) {
                    done.countDown();
                }
            }, null);
            holder.set(req);
            set.countDown();
            Check.isTrue(done.await(30, TimeUnit.SECONDS), "the callback ran");
            Check.equal(Boolean.FALSE, result.get(), "cancel inside the callback");
            Check.isTrue(await(isDone(req), 5000), "done after the callback");
            Check.isFalse(req.isCancelled(), "it was not cancelled");
        } finally {
            s.close();
        }
    }

    /**
     * Closing: the requests that wait or decode are cancelled without a callback, new requests are refused with
     * <code>InvalidStateError</code>, the callback that was running goes on, closing twice is harmless, and no decoding thread is left.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCloseCancelsEverythingAndEndsTheThreads() throws Exception {
        final int before = liveDecodeThreads();
        final AudioDecodeService s = service(2);
        Gate g1 = new Gate();
        Gate g2 = new Gate();
        s.decode(smallWav(100), null, 8000f, g1, null);
        s.decode(smallWav(100), null, 8000f, g2, null);
        Check.isTrue(g1.entered.await(30, TimeUnit.SECONDS) && g2.entered.await(30, TimeUnit.SECONDS), "both threads are busy in a callback");
        Recorder waiting = new Recorder();
        Recorder waiting2 = new Recorder();
        DecodeRequest w1 = s.decode(smallWav(100), null, 8000f, waiting, null);
        DecodeRequest w2 = s.decode(smallWav(100), null, 8000f, waiting2, null);
        Check.equal(2, s.getPendingRequestCount(), "two requests wait");
        Check.isTrue(liveDecodeThreads() > before, "decoding threads exist");
        s.close();
        s.close();
        Check.isTrue(s.isClosed(), "closed");
        Check.isTrue(w1.isCancelled() && w2.isCancelled(), "the waiting requests are cancelled");
        Check.equal(0, s.getPendingRequestCount(), "nothing is pending");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                s.decode(smallWav(100), null, 8000f, new Recorder(), null);
            }
        }, "a request after the closing");
        g1.release.countDown();
        g2.release.countDown();
        Thread.sleep(200);
        Check.equal(0, waiting.calls.get() + waiting2.calls.get(), "no callback of a cancelled request");
        Check.isTrue(await(new Condition() {
            boolean holds() {
                return liveDecodeThreads() <= before;
            }
        }, 10000), "the decoding threads ended: " + liveDecodeThreads() + " left, " + before + " before");
    }

    /**
     * Closing while a decoding is running interrupts it: it ends soon and no callback comes.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCloseDuringADecoding() throws Exception {
        final AudioDecodeService s = service(1);
        byte[] big = AudioFiles.sineWav(96000, 96000 * 40, new double[] {200, 300}, 0.5);
        Recorder r = new Recorder();
        DecodeRequest req = s.decode(big, null, 44100f, r, null);
        Thread.sleep(150);
        long t0 = System.currentTimeMillis();
        s.close();
        Check.isTrue(req.isCancelled(), "cancelled by the closing");
        Check.isTrue(await(new Condition() {
            boolean holds() {
                return s.getActiveRequestCount() == 0;
            }
        }, 10000), "the request left the service");
        Check.isTrue(System.currentTimeMillis() - t0 < 3000, "the decoding stopped soon after the closing");
        Thread.sleep(100);
        Check.equal(0, r.calls.get(), "no callback");
    }

    /**
     * A service that is never used has no thread, a used one gets threads, and they end by themselves after the idle time: a service that is
     * forgotten without being closed holds nothing.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testIdleThreadsEndWithoutClosing() throws Exception {
        final int before = liveDecodeThreads();
        AudioDecodeService s = new AudioDecodeService(2, 150, 8, 1 << 20);
        Check.equal(before, liveDecodeThreads(), "a new service has no thread");
        Recorder r = new Recorder();
        s.decode(smallWav(800), null, 8000f, r, null);
        Check.isTrue(r.await(), "served");
        Check.isTrue(await(new Condition() {
            boolean holds() {
                return liveDecodeThreads() <= before;
            }
        }, 10000), "the idle thread ended by itself");
        Recorder r2 = new Recorder();
        s.decode(smallWav(800), null, 8000f, r2, null);
        Check.isTrue(r2.await(), "the service works again after its threads ended");
        s.close();
    }

    /**
     * The limits of pending requests: a number of requests and a number of bytes at most, <code>NotSupportedError</code> for the one that does not fit,
     * and a cancelled request makes room again.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testLimitsOfPendingRequests() throws Exception {
        final AudioDecodeService s = new AudioDecodeService(1, 300, 3, 10000);
        try {
            Gate gate = new Gate();
            s.decode(smallWav(100), null, 8000f, gate, null);
            Check.isTrue(gate.entered.await(30, TimeUnit.SECONDS), "the thread is busy");
            DecodeRequest a = s.decode(smallWav(2000), null, 8000f, new Recorder(), null);
            DecodeRequest b = s.decode(smallWav(2000), null, 8000f, new Recorder(), null);
            WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
                public void run() {
                    s.decode(smallWav(2000), null, 8000f, new Recorder(), null);
                }
            }, "the byte limit (3 x 4044 bytes > 10000)");
            DecodeRequest c = s.decode(smallWav(100), null, 8000f, new Recorder(), null);
            Check.equal(3, s.getPendingRequestCount(), "three requests wait");
            WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
                public void run() {
                    s.decode(smallWav(10), null, 8000f, new Recorder(), null);
                }
            }, "the limit of three requests");
            Check.isTrue(a.cancel(), "cancel one");
            DecodeRequest d = s.decode(smallWav(2000), null, 8000f, new Recorder(), null);
            Check.equal(3, s.getPendingRequestCount(), "the freed room is used");
            Check.notNull(b, "b is a handle");
            Check.notNull(c, "c is a handle");
            Check.notNull(d, "d is a handle");
            gate.release.countDown();
        } finally {
            s.close();
        }
    }

    /**
     * An exception thrown by a callback is dropped and counted: the pool thread, the executor and the other requests are not affected.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testAFailingCallbackDoesNotAffectOthers() throws Exception {
        AudioDecodeService s = service(1);
        final Holding exec = new Holding();
        try {
            final CountDownLatch thrown = new CountDownLatch(1);
            s.decode(smallWav(800), null, 8000f, new DecodeCallback() {
                public void onSuccess(AudioBuffer b) {
                    thrown.countDown();
                    throw new IllegalStateException("a callback that fails");
                }

                public void onError(AudioException e) {
                    throw new IllegalStateException("a callback that fails");
                }
            }, null);
            Check.isTrue(thrown.await(30, TimeUnit.SECONDS), "the failing callback ran");
            Recorder ok = new Recorder();
            s.decode(smallWav(800), null, 8000f, ok, null);
            Check.isTrue(ok.await(), "the next request is served by the same service");
            Check.equal(1, s.getCallbackFailureCount(), "the failure was counted");
            s.decode(smallWav(800), null, 8000f, new DecodeCallback() {
                public void onSuccess(AudioBuffer b) {
                    throw new IllegalStateException("a callback that fails on an executor");
                }

                public void onError(AudioException e) {
                    throw new IllegalStateException("a callback that fails on an executor");
                }
            }, exec);
            Check.isTrue(await(new Condition() {
                boolean holds() {
                    return exec.tasks.size() == 1;
                }
            }, 10000), "the task waits");
            exec.tasks.get(0).run();
            Check.equal(2, s.getCallbackFailureCount(), "the failure on the executor does not reach it");
        } finally {
            s.close();
        }
    }

    /**
     * The arguments: null data or callback, a sample rate outside the range, the parameters of the constructor.
     * <p>
     * 인수: null 데이터나 콜백, 범위 밖의 표본율, 생성자의 매개변수.
     */
    public void testArguments() {
        final AudioDecodeService s = service(1);
        try {
            Check.throwsException(IllegalArgumentException.class, new Block() {
                public void run() throws Exception {
                    s.decode(null, null, 44100f, new Recorder(), null);
                }
            }, "null data");
            Check.throwsException(IllegalArgumentException.class, new Block() {
                public void run() throws Exception {
                    s.decode(new byte[1], null, 44100f, null, null);
                }
            }, "null callback");
            float[] bad = {2999f, 768001f, Float.NaN, 0f};
            // 범위 밖의 표본율은 모두 NotSupportedError 이다.
            for (int i = 0; i < bad.length; i++) {
                final float rate = bad[i];
                WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
                    public void run() {
                        s.decode(new byte[1], null, rate, new Recorder(), null);
                    }
                }, "rate " + rate);
            }
            Check.throwsException(IllegalArgumentException.class, new Block() {
                public void run() throws Exception {
                    new AudioDecodeService(0);
                }
            }, "no threads");
            Check.throwsException(IllegalArgumentException.class, new Block() {
                public void run() throws Exception {
                    new AudioDecodeService(1, 0, 1, 1);
                }
            }, "no idle time");
        } finally {
            s.close();
        }
    }

    /**
     * An executor that refuses the callback (a document queue that is closed) makes the request end without a callback, and nothing is left.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testAnExecutorThatRefusesDropsTheCallback() throws Exception {
        final AudioDecodeService s = service(1);
        try {
            Recorder r = new Recorder();
            DecodeRequest req = s.decode(smallWav(800), null, 8000f, r, new Executor() {
                public void execute(Runnable command) {
                    throw new RejectedExecutionException("the queue is closed");
                }
            });
            Check.isTrue(await(isDone(req), 10000), "the request ended");
            Check.equal(0, r.calls.get(), "no callback");
            Check.equal(0, s.getActiveRequestCount(), "nothing is left");
        } finally {
            s.close();
        }
    }

    /**
     * A callback may close the service (the page is left from a handler): no deadlock, the callback returns, the service is closed after it.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testCloseInsideACallback() throws Exception {
        final AudioDecodeService s = service(1);
        final CountDownLatch done = new CountDownLatch(1);
        s.decode(smallWav(800), null, 8000f, new DecodeCallback() {
            public void onSuccess(AudioBuffer b) {
                s.close();
                done.countDown();
            }

            public void onError(AudioException e) {
                done.countDown();
            }
        }, null);
        Check.isTrue(done.await(30, TimeUnit.SECONDS), "the callback returned");
        Check.isTrue(s.isClosed(), "closed");
        Check.isTrue(await(new Condition() {
            boolean holds() {
                return s.getActiveRequestCount() == 0;
            }
        }, 10000), "nothing is left");
    }

    /**
     * Many requests with random sizes are cancelled at random moments (before the start, during the decoding, after the result):
     * a request whose cancellation returned <code>true</code> never calls back, every other one calls back exactly once, and in the end the
     * service has nothing pending and no thread is left after the closing.
     *
     * 임의 크기와 시점으로 요청을 취소하여 콜백 횟수와 작업자 정리를 확인한다.
     *
     * @throws Exception if the wait fails / 기다림이 실패하면
     */
    public void testRandomCancellationKeepsTheBooks() throws Exception {
        final int before = liveDecodeThreads();
        Random rnd = new Random(20261122L);
        int n = 150;
        int maxFrames = 30100;
        // 디코딩이 늦어도 모든 요청을 받을 수 있게 하여 취소 계약만 검증한다.
        final AudioDecodeService s = new AudioDecodeService(3, 300, n, n * (44L + 2L * maxFrames));
        Recorder[] recorders = new Recorder[n];
        boolean[] cancelledOk = new boolean[n];
        try {
            // 요청을 만들면서 일부는 곧바로, 일부는 잠시 뒤에 취소한다.
            for (int i = 0; i < n; i++) {
                recorders[i] = new Recorder();
                DecodeRequest request = s.decode(smallWav(100 + rnd.nextInt(maxFrames - 100)), null, 8000f, recorders[i], null);
                int what = rnd.nextInt(4);
                // 일부 취소에 지연을 두어 디코딩과 콜백이 시작될 기회를 준다.
                if (what == 1) {
                    Thread.sleep(rnd.nextInt(3));
                }
                if (what == 0 || what == 1) {
                    cancelledOk[i] = request.cancel();
                }
            }
            // 취소하지 않았거나 취소에 실패한 요청은 모두 콜백이 와야 한다.
            for (int i = 0; i < n; i++) {
                if (!cancelledOk[i]) {
                    Check.isTrue(recorders[i].await(), "the callback of request " + i + " came");
                }
            }
            Check.isTrue(await(new Condition() {
                boolean holds() {
                    return s.getActiveRequestCount() == 0 && s.getPendingRequestCount() == 0;
                }
            }, 30000), "nothing pending: " + s.getActiveRequestCount() + " active, " + s.getPendingRequestCount() + " pending");
            Thread.sleep(50);
            // 요청마다 콜백 횟수가 취소 결과와 맞는지 확인한다.
            for (int i = 0; i < n; i++) {
                Check.equal(cancelledOk[i] ? 0 : 1, recorders[i].calls.get(), "calls of request " + i + " (cancelled: " + cancelledOk[i] + ")");
            }
        } finally {
            s.close();
        }
        Check.isTrue(await(new Condition() {
            boolean holds() {
                return liveDecodeThreads() <= before;
            }
        }, 10000), "no thread is left after the closing");
    }
}
