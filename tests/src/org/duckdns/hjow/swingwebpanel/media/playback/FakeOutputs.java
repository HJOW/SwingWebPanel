package org.duckdns.hjow.swingwebpanel.media.playback;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A fake audio output factory for tests (plan item P18-08, P18-10): it records the PCM that a player writes instead of playing it and
 * counts the calls of the output, so the playback can be verified exactly and without a sound device. The outputs render only on request
 * (gated mode: {@link Out#consume(long)} moves time forward) or at once (automatic mode), so a test controls the position of the sound.
 * <p>
 * 시험용 가짜 오디오 출력 팩터리(계획 P18-08, P18-10): 재생기가 쓰는 PCM 을 재생하는 대신 기록하고 출력의 호출 횟수를 세므로 소리 장치 없이
 * 재생을 정확히 검증할 수 있다. 출력은 요청할 때만 재생하거나({@link Out#consume(long)} 가 시간을 앞으로 보냄) 즉시 재생하므로(자동 모드) 시험이 소리의
 * 위치를 제어한다.
 */
public final class FakeOutputs implements AudioOutputFactory {

    /**
     * The outputs that were opened, in order.
     * <p>
     * 열린 출력(순서대로).
     */
    public final List<Out> outputs = new ArrayList<Out>();

    /**
     * The capacity in frames of every output; {@link Long#MAX_VALUE} renders at once (automatic mode), a finite value needs
     * {@link Out#consume(long)}.
     * <p>
     * 모든 출력의 용량(프레임 수). {@link Long#MAX_VALUE} 는 즉시 재생(자동 모드)이고, 유한한 값은 {@link Out#consume(long)} 이 필요하다.
     */
    public volatile long capacity;

    /**
     * When not null, {@link #open(int, int)} fails with this exception (a machine without a sound device).
     * <p>
     * null 이 아니면 {@link #open(int, int)} 가 이 예외로 실패한다(소리 장치가 없는 컴퓨터).
     */
    public volatile MediaException failure;

    /**
     * The number of calls of {@link #open(int, int)}.
     * <p>
     * {@link #open(int, int)} 호출 횟수.
     */
    public volatile int openCalls;

    /**
     * While true, every output takes nothing (like a device line that needs seconds before it accepts the first data, as Java Sound over a
     * remote desktop session does); a test sets it back to false to let the sound start.
     * <p>
     * true 인 동안 모든 출력이 아무것도 받지 않는다(원격 데스크톱 세션의 Java Sound 처럼 첫 데이터를 받기까지 몇 초 걸리는 장치 라인). 시험이 false 로
     * 되돌리면 소리가 시작된다.
     */
    public volatile boolean holdWrites;

    /**
     * Creates a factory in automatic mode.
     * <p>
     * 자동 모드의 팩터리를 만든다.
     */
    public FakeOutputs() {
        this(Long.MAX_VALUE);
    }

    /**
     * Creates a factory with a capacity.
     * <p>
     * 용량을 정해 팩터리를 만든다.
     *
     * @param capacity the capacity in frames, {@link Long#MAX_VALUE} for the automatic mode / 프레임 수 용량. 자동 모드는 {@link Long#MAX_VALUE}
     */
    public FakeOutputs(long capacity) {
        this.capacity = capacity;
    }

    /**
     * Opens a fake output or fails like a machine without sound.
     * <p>
     * 가짜 출력을 열거나, 소리가 없는 컴퓨터처럼 실패한다.
     *
     * @param sampleRate the frames per second / 초당 프레임 수
     * @param channels   1 or 2 / 1 또는 2
     * @return the output / 출력
     * @throws MediaException the configured failure / 설정된 실패
     */
    public AudioOutput open(int sampleRate, int channels) throws MediaException {
        synchronized (this) {
            openCalls++;
            if (failure != null) {
                throw failure;
            }
            Out o = new Out(sampleRate, channels, capacity);
            o.owner = this;
            outputs.add(o);
            return o;
        }
    }

    /**
     * Returns the output with an index (waits until it is opened).
     * <p>
     * 번호의 출력을 반환한다(열릴 때까지 기다린다).
     *
     * @param index the number of the output / 출력 번호
     * @return the output / 출력
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    public Out await(int index) throws InterruptedException {
        long end = System.currentTimeMillis() + 20000;
        // 출력이 열릴 때까지 짧게 쉬며 기다린다.
        while (System.currentTimeMillis() < end) {
            synchronized (this) {
                if (outputs.size() > index) {
                    return outputs.get(index);
                }
            }
            Thread.sleep(2);
        }
        throw new AssertionError("output " + index + " was not opened");
    }

    /**
     * A fake output.
     * <p>
     * 가짜 출력.
     */
    public static final class Out implements AudioOutput {

        /**
         * The frames per second.
         * <p>
         * 초당 프레임 수.
         */
        public final int sampleRate;

        /**
         * The channels.
         * <p>
         * 채널 수.
         */
        public final int channels;

        /**
         * The capacity in frames.
         * <p>
         * 용량(프레임 수).
         */
        private final long capacity;

        /**
         * Everything written since the output was opened.
         * <p>
         * 출력이 열린 뒤 쓰인 모든 것.
         */
        private final ByteArrayOutputStream all = new ByteArrayOutputStream();

        /**
         * Everything written since the last flush.
         * <p>
         * 마지막 flush 뒤 쓰인 모든 것.
         */
        private ByteArrayOutputStream sinceFlush = new ByteArrayOutputStream();

        /**
         * The frames that are written and not rendered.
         * <p>
         * 쓰였지만 재생되지 않은 프레임.
         */
        private long queued;

        /**
         * The frames that were rendered.
         * <p>
         * 재생된 프레임.
         */
        private long rendered;

        /**
         * True while started.
         * <p>
         * 시작된 동안 true.
         */
        private boolean started;

        /**
         * The number of start calls.
         * <p>
         * start 호출 횟수.
         */
        private int starts;

        /**
         * The number of stop calls.
         * <p>
         * stop 호출 횟수.
         */
        private int stops;

        /**
         * The number of flush calls.
         * <p>
         * flush 호출 횟수.
         */
        private int flushes;

        /**
         * The number of close calls.
         * <p>
         * close 호출 횟수.
         */
        private int closes;

        /**
         * The factory that opened the output (for {@link FakeOutputs#holdWrites}), or null.
         * <p>
         * 출력을 연 팩터리({@link FakeOutputs#holdWrites} 용). 없으면 null.
         */
        volatile FakeOutputs owner;

        /**
         * Creates an output.
         * <p>
         * 출력을 만든다.
         *
         * @param sampleRate the frames per second / 초당 프레임 수
         * @param channels   the channels / 채널 수
         * @param capacity   the capacity in frames / 용량(프레임 수)
         */
        Out(int sampleRate, int channels, long capacity) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.capacity = capacity;
        }

        /**
         * Takes what fits: everything in the automatic mode (rendered at once), otherwise up to the capacity.
         * <p>
         * 들어가는 만큼 받는다: 자동 모드는 전부(즉시 재생), 아니면 용량까지.
         *
         * @param data   the bytes / 바이트
         * @param offset the start / 시작
         * @param length the bytes offered / 제안한 바이트 수
         * @return the bytes taken / 받은 바이트 수
         */
        public synchronized int write(byte[] data, int offset, int length) {
            int frameBytes = channels * 2;
            if (closes > 0) {
                throw new IllegalStateException("written to a closed output");
            }
            FakeOutputs f = owner;
            // 장치가 아직 데이터를 받지 않는 상태를 흉내 낸다.
            if (f != null && f.holdWrites) {
                return 0;
            }
            long room =capacity == Long.MAX_VALUE ? Long.MAX_VALUE : capacity - queued;
            long frames = Math.min(length / frameBytes, room);
            int n = (int) (frames * frameBytes);
            if (n > 0) {
                all.write(data, offset, n);
                sinceFlush.write(data, offset, n);
                if (capacity == Long.MAX_VALUE) {
                    rendered += frames;
                } else {
                    queued += frames;
                }
            }
            return n;
        }

        /**
         * Returns the frames that are not rendered yet.
         * <p>
         * 아직 재생되지 않은 프레임 수를 반환한다.
         *
         * @return the frames / 프레임 수
         */
        public synchronized long getQueuedFrames() {
            return queued;
        }

        /**
         * Starts rendering.
         * <p>
         * 재생을 시작한다.
         */
        public synchronized void start() {
            started = true;
            starts++;
        }

        /**
         * Stops rendering.
         * <p>
         * 재생을 멈춘다.
         */
        public synchronized void stop() {
            started = false;
            stops++;
        }

        /**
         * Discards the queue.
         * <p>
         * 큐를 버린다.
         */
        public synchronized void flush() {
            queued = 0;
            sinceFlush = new ByteArrayOutputStream();
            flushes++;
        }

        /**
         * Closes the output.
         * <p>
         * 출력을 닫는다.
         */
        public synchronized void close() {
            closes++;
            started = false;
        }

        /**
         * Renders frames of the queue, but only while started (a stopped device renders nothing).
         * <p>
         * 큐의 프레임을 재생한다. 시작된 동안만 한다(멈춘 장치는 아무것도 재생하지 않는다).
         *
         * @param frames the frames to render / 재생할 프레임 수
         * @return the frames that were rendered / 실제로 재생된 프레임 수
         */
        public synchronized long consume(long frames) {
            long n = started ? Math.min(frames, queued) : 0;
            queued -= n;
            rendered += n;
            return n;
        }

        /**
         * Returns a copy of everything that was written.
         * <p>
         * 쓰인 모든 것의 복사본을 반환한다.
         *
         * @return the bytes / 바이트
         */
        public synchronized byte[] written() {
            return all.toByteArray();
        }

        /**
         * Returns a copy of what was written after the last flush.
         * <p>
         * 마지막 flush 뒤에 쓰인 것의 복사본을 반환한다.
         *
         * @return the bytes / 바이트
         */
        public synchronized byte[] writtenSinceFlush() {
            return sinceFlush.toByteArray();
        }

        /**
         * Returns the frames that were rendered.
         * <p>
         * 재생된 프레임 수를 반환한다.
         *
         * @return the frames / 프레임 수
         */
        public synchronized long renderedFrames() {
            return rendered;
        }

        /**
         * Tells whether the output is started.
         * <p>
         * 출력이 시작된 상태인지 알려 준다.
         *
         * @return true if started / 시작되었으면 true
         */
        public synchronized boolean isStarted() {
            return started;
        }

        /**
         * Returns the number of start calls.
         * <p>
         * start 호출 횟수를 반환한다.
         *
         * @return the count / 횟수
         */
        public synchronized int startCalls() {
            return starts;
        }

        /**
         * Returns the number of stop calls.
         * <p>
         * stop 호출 횟수를 반환한다.
         *
         * @return the count / 횟수
         */
        public synchronized int stopCalls() {
            return stops;
        }

        /**
         * Returns the number of flush calls.
         * <p>
         * flush 호출 횟수를 반환한다.
         *
         * @return the count / 횟수
         */
        public synchronized int flushCalls() {
            return flushes;
        }

        /**
         * Returns the number of close calls.
         * <p>
         * close 호출 횟수를 반환한다.
         *
         * @return the count / 횟수
         */
        public synchronized int closeCalls() {
            return closes;
        }

        /**
         * Waits until at least a number of frames is queued (the player filled the buffer).
         * <p>
         * 큐에 적어도 주어진 수의 프레임이 쌓일 때까지 기다린다(재생기가 버퍼를 채웠다).
         *
         * @param frames the frames / 프레임 수
         * @throws InterruptedException if interrupted / 인터럽트되면
         */
        public void awaitQueued(long frames) throws InterruptedException {
            long end = System.currentTimeMillis() + 20000;
            // 큐가 찰 때까지 짧게 쉬며 확인한다.
            while (System.currentTimeMillis() < end) {
                if (getQueuedFrames() >= frames) {
                    return;
                }
                Thread.sleep(2);
            }
            throw new AssertionError("the queue did not reach " + frames + " frames (" + getQueuedFrames() + ")");
        }

        /**
         * Waits until at least a number of bytes was written.
         * <p>
         * 적어도 주어진 수의 바이트가 쓰일 때까지 기다린다.
         *
         * @param bytes the bytes / 바이트 수
         * @throws InterruptedException if interrupted / 인터럽트되면
         */
        public void awaitWritten(int bytes) throws InterruptedException {
            long end = System.currentTimeMillis() + 20000;
            // 쓰인 바이트가 충분해질 때까지 짧게 쉬며 확인한다.
            while (System.currentTimeMillis() < end) {
                if (written().length >= bytes) {
                    return;
                }
                Thread.sleep(2);
            }
            throw new AssertionError("only " + written().length + " of " + bytes + " bytes were written");
        }
    }
}
