package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.util.Arrays;

/**
 * A stand-in for a sound device that plays only when the test says so (plan item P25-07 tests): it records every frame that is written
 * (as floats, so a test can compare them exactly with an offline rendering) and its virtual device consumes frames only when the test calls
 * {@link #consume(long)} (or all the time, with {@link #setAutoConsume(boolean)}). It follows the contract of {@link AudioOutput}: while it is
 * started and full a {@link #write} waits, and {@link #stop()} or {@link #close()} wake the writer. The class is not a suite (its name does
 * not end with <code>Test</code>).
 * <p>
 * 시험이 시킬 때만 재생하는 소리 장치의 대역(계획 P25-07 시험): 쓰인 모든 프레임을 (float 으로) 기록하므로 시험이 오프라인 렌더링과 정확히 비교할 수 있고, 가상
 * 장치는 시험이 {@link #consume(long)} 을 부를 때만(또는 {@link #setAutoConsume(boolean)} 으로 늘) 프레임을 소비한다. {@link AudioOutput} 의 약속을 따른다:
 * 시작되어 가득 차 있는 동안 {@link #write} 는 기다리고 {@link #stop()} 이나 {@link #close()} 가 쓰는 쪽을 깨운다. 이 클래스는 스위트가 아니다(이름이
 * <code>Test</code> 로 끝나지 않는다).
 */
public final class ManualOutput implements AudioOutput {

    /**
     * The sample rate.
     * <p>
     * 샘플링 주파수.
     */
    private final int sampleRate;

    /**
     * The number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * The capacity in frames.
     * <p>
     * 용량(프레임).
     */
    private final int bufferFrames;

    /**
     * Guards the fields below.
     * <p>
     * 아래 필드를 지킨다.
     */
    private final Object lock = new Object();

    /**
     * The recorded channels.
     * <p>
     * 기록한 채널.
     */
    private float[][] recorded;

    /**
     * The number of recorded frames.
     * <p>
     * 기록한 프레임 수.
     */
    private long written;

    /**
     * The number of frames that the virtual device has played.
     * <p>
     * 가상 장치가 재생한 프레임 수.
     */
    private long played;

    /**
     * True after {@link #start()}.
     * <p>
     * {@link #start()} 를 부른 뒤에는 true 이다.
     */
    private boolean started;

    /**
     * True after {@link #close()}.
     * <p>
     * {@link #close()} 를 부른 뒤에는 true 이다.
     */
    private boolean closed;

    /**
     * True if the virtual device consumes everything at once.
     * <p>
     * 가상 장치가 모든 것을 곧바로 소비하면 true.
     */
    private boolean autoConsume;

    /**
     * The number of calls of {@link #stop()}.
     * <p>
     * {@link #stop()} 호출 횟수.
     */
    private int stops;

    /**
     * The number of calls of {@link #flush()}.
     * <p>
     * {@link #flush()} 호출 횟수.
     */
    private int flushes;

    /**
     * The number of calls of {@link #start()}.
     * <p>
     * {@link #start()} 호출 횟수.
     */
    private int starts;

    /**
     * Creates an output.
     * <p>
     * 출력을 만든다.
     *
     * @param sampleRate   the sample rate in Hz / 샘플링 주파수(Hz)
     * @param channels     the number of channels / 채널 수
     * @param bufferFrames the capacity in frames / 용량(프레임)
     */
    public ManualOutput(int sampleRate, int channels, int bufferFrames) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bufferFrames = bufferFrames;
        this.recorded = new float[channels][1 << 12];
    }

    /**
     * Returns the sample rate.
     * <p>
     * 샘플링 주파수를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
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
     * Returns the capacity.
     * <p>
     * 용량을 반환한다.
     *
     * @return the capacity in frames / 용량(프레임)
     */
    public int getBufferFrames() {
        return bufferFrames;
    }

    /**
     * Sets whether the virtual device consumes every frame at once (an output that never holds anything).
     * <p>
     * 가상 장치가 모든 프레임을 곧바로 소비할지 정한다(아무것도 쌓아 두지 않는 출력).
     *
     * @param value true to consume at once / 곧바로 소비하려면 true
     */
    public void setAutoConsume(boolean value) {
        synchronized (lock) {
            autoConsume = value;
            if (value) {
                played = written;
            }
            lock.notifyAll();
        }
    }

    /**
     * Records the frames, waiting while the output is started and full.
     * <p>
     * 프레임을 기록하며, 출력이 시작되어 가득 차 있으면 기다린다.
     *
     * @param data   the audio / 오디오
     * @param frames the number of frames / 프레임 수
     */
    public void write(float[][] data, int frames) {
        int offset = 0;
        synchronized (lock) {
            // 모든 프레임을 기록하거나 닫힐 때까지 반복한다.
            while (offset < frames && !closed) {
                long free = autoConsume ? frames - offset : bufferFrames - (written - played);
                // 빈 자리가 있으면 그만큼 기록한다.
                if (free > 0) {
                    int n = (int) Math.min(free, frames - offset);
                    ensure(written + n);
                    // 채널마다 기록한다.
                    for (int c = 0; c < channels; c++) {
                        System.arraycopy(data[c], offset, recorded[c], (int) written, n);
                    }
                    written += n;
                    offset += n;
                    if (autoConsume) {
                        played = written;
                    }
                    lock.notifyAll();
                    continue;
                }
                // 멈춘 채 가득 차면 나머지를 버리고 돌아온다.
                if (!started) {
                    break;
                }
                try {
                    lock.wait(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Makes the recording large enough.
     * <p>
     * 기록 공간을 충분히 키운다.
     *
     * @param frames the frames needed / 필요한 프레임 수
     */
    private void ensure(long frames) {
        // 모자라면 두 배로 키운다.
        while (frames > recorded[0].length) {
            for (int c = 0; c < channels; c++) {
                recorded[c] = Arrays.copyOf(recorded[c], recorded[c].length * 2);
            }
        }
    }

    /**
     * Plays frames on the virtual device: the queue becomes smaller by that many frames (or empty).
     * <p>
     * 가상 장치에서 프레임을 재생한다: 큐가 그만큼(또는 빌 때까지) 줄어든다.
     *
     * @param frames the number of frames / 프레임 수
     */
    public void consume(long frames) {
        synchronized (lock) {
            played = Math.min(written, played + frames);
            lock.notifyAll();
        }
    }

    /**
     * Plays everything that is queued.
     * <p>
     * 큐에 있는 모든 것을 재생한다.
     */
    public void consumeAll() {
        synchronized (lock) {
            played = written;
            lock.notifyAll();
        }
    }

    /**
     * Returns the number of frames that were written.
     * <p>
     * 쓰인 프레임 수를 반환한다.
     *
     * @return the frames / 프레임 수
     */
    public long getWrittenFrames() {
        synchronized (lock) {
            return written;
        }
    }

    /**
     * Returns a copy of the recorded samples of a channel.
     * <p>
     * 채널의 기록한 샘플의 복사본을 반환한다.
     *
     * @param channel the channel / 채널
     * @return the samples / 샘플
     */
    public float[] copyChannel(int channel) {
        synchronized (lock) {
            return Arrays.copyOf(recorded[channel], (int) written);
        }
    }

    /**
     * Returns the frames that the virtual device has not played.
     * <p>
     * 가상 장치가 재생하지 않은 프레임 수를 반환한다.
     *
     * @return the queued frames / 큐에 있는 프레임 수
     */
    public long getQueuedFrames() {
        synchronized (lock) {
            return written - played;
        }
    }

    /**
     * Starts the virtual device.
     * <p>
     * 가상 장치를 시작한다.
     */
    public void start() {
        synchronized (lock) {
            started = true;
            starts++;
            lock.notifyAll();
        }
    }

    /**
     * Stops the virtual device and wakes the writer.
     * <p>
     * 가상 장치를 멈추고 쓰는 쪽을 깨운다.
     */
    public void stop() {
        synchronized (lock) {
            started = false;
            stops++;
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
            played = written;
            flushes++;
            lock.notifyAll();
        }
    }

    /**
     * Closes the output.
     * <p>
     * 출력을 닫는다.
     */
    public void close() {
        synchronized (lock) {
            closed = true;
            started = false;
            lock.notifyAll();
        }
    }

    /**
     * Tells whether the output is closed.
     * <p>
     * 출력이 닫혔는지 알려 준다.
     *
     * @return true after close / close 뒤 true
     */
    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /**
     * Tells whether the output is started.
     * <p>
     * 출력이 시작되었는지 알려 준다.
     *
     * @return true while started / 시작된 동안 true
     */
    public boolean isStarted() {
        synchronized (lock) {
            return started;
        }
    }

    /**
     * Returns the number of calls of {@link #stop()}.
     * <p>
     * {@link #stop()} 호출 횟수를 반환한다.
     *
     * @return the count / 횟수
     */
    public int getStopCount() {
        synchronized (lock) {
            return stops;
        }
    }

    /**
     * Returns the number of calls of {@link #flush()}.
     * <p>
     * {@link #flush()} 호출 횟수를 반환한다.
     *
     * @return the count / 횟수
     */
    public int getFlushCount() {
        synchronized (lock) {
            return flushes;
        }
    }

    /**
     * Returns the number of calls of {@link #start()}.
     * <p>
     * {@link #start()} 호출 횟수를 반환한다.
     *
     * @return the count / 횟수
     */
    public int getStartCount() {
        synchronized (lock) {
            return starts;
        }
    }

    /**
     * Returns a description.
     * <p>
     * 설명을 반환한다.
     *
     * @return the description / 설명
     */
    public String getDescription() {
        return "manual " + sampleRate + " Hz " + channels + " ch";
    }

    /**
     * Returns true: the test treats it as a device.
     * <p>
     * true 를 반환한다: 시험은 이것을 장치로 본다.
     *
     * @return true
     */
    public boolean isAudible() {
        return true;
    }
}
