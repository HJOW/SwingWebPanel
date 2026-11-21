package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

/**
 * The output for a machine without a sound device (plan item P25-07): it discards the audio, but it behaves like a device of a fixed
 * buffer that plays at real speed. A started output "plays" the queue as wall-clock time passes, a {@link #write} waits while the queue is
 * full, and the queue drains to 0 when the writer is too slow (an underrun, after which the virtual device waits for new data). A context
 * that renders into it therefore advances its <code>currentTime</code> at the real speed of the clock, as it would with a real device, and
 * a page that measures time with the context still works on a computer without sound.
 * <p>
 * 소리 장치가 없는 컴퓨터를 위한 출력(계획 P25-07): 오디오를 버리지만 고정 버퍼를 가진, 실제 속도로 재생하는 장치처럼 동작한다. 시작된 출력은 벽시계 시간이
 * 지나는 만큼 큐를 "재생"하고, 큐가 가득 차 있으면 {@link #write} 가 기다리며, 쓰는 쪽이 너무 느리면 큐가 0 으로 비워진다(언더런. 그 뒤 가상 장치는
 * 새 데이터를 기다린다). 그래서 이것으로 렌더링하는 컨텍스트는 실제 장치처럼 <code>currentTime</code> 이 시계의 실제 속도로 진행하고, 컨텍스트로 시간을
 * 재는 페이지는 소리가 없는 컴퓨터에서도 동작한다.
 */
public final class NullOutput implements AudioOutput {

    /**
     * The source of time; a test replaces it to control the clock.
     * <p>
     * 시간의 원천. 시험이 시계를 제어하려고 바꾼다.
     */
    interface Clock {

        /**
         * Returns a nanosecond reading that only the differences of which are meaningful.
         * <p>
         * 차이만 의미가 있는 나노초 값을 반환한다.
         *
         * @return the reading / 값
         */
        long nanoTime();
    }

    /**
     * The clock of the machine.
     * <p>
     * 컴퓨터의 시계.
     */
    private static final Clock SYSTEM_CLOCK = new Clock() {
        /**
         * Returns the system nanosecond clock.
         * <p>
         * 시스템 나노초 시계를 반환한다.
         *
         * @return the reading / 값
         */
        public long nanoTime() {
            return System.nanoTime();
        }
    };

    /**
     * The sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz).
     */
    private final int sampleRate;

    /**
     * The number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * The capacity of the virtual buffer in frames.
     * <p>
     * 가상 버퍼의 용량(프레임).
     */
    private final int bufferFrames;

    /**
     * The clock.
     * <p>
     * 시계.
     */
    private final Clock clock;

    /**
     * Guards the counters below and wakes a writer that waits.
     * <p>
     * 아래 카운터를 지키고 기다리는 쓰는 쪽을 깨운다.
     */
    private final Object lock = new Object();

    /**
     * The number of frames that were accepted so far.
     * <p>
     * 지금까지 받아들인 프레임 수.
     */
    private long written;

    /**
     * The number of frames that the virtual device had played at {@link #baseNanos}.
     * <p>
     * {@link #baseNanos} 시점에 가상 장치가 재생한 프레임 수.
     */
    private long consumedBase;

    /**
     * The time at which {@link #consumedBase} was valid.
     * <p>
     * {@link #consumedBase} 가 유효했던 시각.
     */
    private long baseNanos;

    /**
     * True while the virtual device plays.
     * <p>
     * 가상 장치가 재생하는 동안 true.
     */
    private boolean started;

    /**
     * True after {@link #close()}.
     * <p>
     * {@link #close()} 뒤에 true.
     */
    private boolean closed;

    /**
     * Creates a silent output on the clock of the machine.
     * <p>
     * 컴퓨터의 시계를 쓰는 무음 출력을 만든다.
     *
     * @param sampleRate   the sample rate of the data in Hz / 데이터의 샘플링 주파수(Hz)
     * @param channels     the number of channels / 채널 수
     * @param bufferFrames the capacity of the virtual buffer in frames, at least 1 / 가상 버퍼의 용량(프레임, 1 이상)
     */
    public NullOutput(int sampleRate, int channels, int bufferFrames) {
        this(sampleRate, channels, bufferFrames, SYSTEM_CLOCK);
    }

    /**
     * Creates a silent output on a given clock (for tests).
     * <p>
     * 주어진 시계를 쓰는 무음 출력을 만든다(시험용).
     *
     * @param sampleRate   the sample rate of the data in Hz / 데이터의 샘플링 주파수(Hz)
     * @param channels     the number of channels / 채널 수
     * @param bufferFrames the capacity of the virtual buffer in frames / 가상 버퍼의 용량(프레임)
     * @param clock        the clock / 시계
     */
    NullOutput(int sampleRate, int channels, int bufferFrames, Clock clock) {
        if (sampleRate < 1 || channels < 1 || bufferFrames < 1) {
            throw new IllegalArgumentException("sampleRate=" + sampleRate + " channels=" + channels + " bufferFrames=" + bufferFrames);
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bufferFrames = bufferFrames;
        this.clock = clock;
    }

    /**
     * Returns the sample rate of the data.
     * <p>
     * 데이터의 샘플링 주파수를 반환한다.
     *
     * @return the sample rate in Hz / 샘플링 주파수(Hz)
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of channels.
     * <p>
     * 채널 수를 반환한다.
     *
     * @return the number of channels / 채널 수
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Returns the capacity of the virtual buffer.
     * <p>
     * 가상 버퍼의 용량을 반환한다.
     *
     * @return the capacity in frames / 용량(프레임)
     */
    public int getBufferFrames() {
        return bufferFrames;
    }

    /**
     * Returns the number of frames that the virtual device has played, and moves the base of the clock forward when the device ran dry:
     * an empty device does not play, so the time since it ran dry is not counted. Call it with the lock held.
     * <p>
     * 가상 장치가 재생한 프레임 수를 반환하며, 장치가 비었으면 시계의 기준을 앞으로 옮긴다: 빈 장치는 재생하지 않으므로 비워진 뒤의 시간은 세지 않는다.
     * 잠금을 잡은 채로 부른다.
     *
     * @param now the current reading of the clock / 시계의 현재 값
     * @return the frames played / 재생한 프레임 수
     */
    private long consumedLocked(long now) {
        if (!started) {
            return consumedBase;
        }
        long played = consumedBase + (long) ((now - baseNanos) * (double) sampleRate / 1e9);
        // 쓴 것보다 많이 재생했다는 계산은 장치가 비었다는 뜻이므로(언더런) 기준을 지금으로 옮긴다.
        if (played >= written) {
            consumedBase = written;
            baseNanos = now;
            return written;
        }
        return played;
    }

    /**
     * Accepts the frames, waiting while the virtual buffer is full and the output is started.
     * <p>
     * 프레임을 받아들이며, 가상 버퍼가 가득 차 있고 출력이 시작된 상태이면 기다린다.
     *
     * @param data   the audio, ignored / 오디오(무시한다)
     * @param frames the number of frames / 프레임 수
     */
    public void write(float[][] data, int frames) {
        int remaining = frames;
        synchronized (lock) {
            // 모든 프레임을 받아들이거나 닫힐 때까지 반복한다.
            while (remaining > 0 && !closed) {
                long free = bufferFrames - (written - consumedLocked(clock.nanoTime()));
                // 빈 자리가 있으면 그만큼 받아들인다.
                if (free > 0) {
                    int n = (int) Math.min(free, remaining);
                    written += n;
                    remaining -= n;
                    continue;
                }
                // 멈춘 채 가득 차면 자리를 만들 것이 없으므로 나머지를 버리고 돌아온다.
                if (!started) {
                    break;
                }
                // 장치가 프레임 몇 개를 재생할 때까지(버퍼의 1/4 또는 남은 만큼) 기다린다.
                long need = Math.max(1, Math.min(remaining, Math.max(1, bufferFrames / 4)));
                long millis = Math.max(1L, (long) Math.ceil(need * 1000.0 / sampleRate));
                try {
                    lock.wait(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Returns the frames that the virtual device has not played yet.
     * <p>
     * 가상 장치가 아직 재생하지 않은 프레임 수를 반환한다.
     *
     * @return the queued frames / 큐에 있는 프레임 수
     */
    public long getQueuedFrames() {
        synchronized (lock) {
            long queued = written - consumedLocked(clock.nanoTime());
            return queued < 0 ? 0 : queued;
        }
    }

    /**
     * Starts the virtual device.
     * <p>
     * 가상 장치를 시작한다.
     */
    public void start() {
        synchronized (lock) {
            if (!started && !closed) {
                started = true;
                baseNanos = clock.nanoTime();
            }
        }
    }

    /**
     * Stops the virtual device; the queue stays.
     * <p>
     * 가상 장치를 멈춘다. 큐는 남는다.
     */
    public void stop() {
        synchronized (lock) {
            if (started) {
                consumedBase = consumedLocked(clock.nanoTime());
                started = false;
            }
            lock.notifyAll();
        }
    }

    /**
     * Discards the queue.
     * <p>
     * 큐를 버린다.
     */
    public void flush() {
        synchronized (lock) {
            consumedBase = consumedLocked(clock.nanoTime());
            written = consumedBase;
            baseNanos = clock.nanoTime();
            lock.notifyAll();
        }
    }

    /**
     * Closes the output and wakes a writer that waits.
     * <p>
     * 출력을 닫고 기다리는 쓰는 쪽을 깨운다.
     */
    public void close() {
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
        }
    }

    /**
     * Returns <code>silent</code> with the format.
     * <p>
     * 형식을 곁들여 <code>silent</code> 를 반환한다.
     *
     * @return the description / 설명
     */
    public String getDescription() {
        return "silent " + sampleRate + " Hz " + channels + " ch";
    }

    /**
     * Returns false: nothing is played.
     * <p>
     * false 를 반환한다: 아무것도 재생하지 않는다.
     *
     * @return false
     */
    public boolean isAudible() {
        return false;
    }
}
