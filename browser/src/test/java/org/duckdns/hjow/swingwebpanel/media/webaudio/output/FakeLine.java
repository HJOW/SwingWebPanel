package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.io.ByteArrayOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

/**
 * A {@link SourceDataLine} for tests (plan item P25-07 tests) whose "device" plays bytes only when the test calls {@link #consume(int)}: it
 * keeps a buffer of a fixed size, never blocks (a write beyond the free room is a test failure, because a correct output only writes what
 * {@link #available()} reports), and records every byte that was written. The class is not a suite.
 * <p>
 * "장치"가 시험이 {@link #consume(int)} 를 부를 때만 바이트를 재생하는 시험용 {@link SourceDataLine}(계획 P25-07 시험): 고정 크기의 버퍼를 두고, 블록하지
 * 않으며(올바른 출력은 {@link #available()} 이 알린 것만 쓰므로 빈 자리를 넘는 쓰기는 시험 실패), 쓰인 모든 바이트를 기록한다. 이 클래스는 스위트가 아니다.
 */
public final class FakeLine implements SourceDataLine {

    /**
     * The format.
     * <p>
     * 형식.
     */
    private final AudioFormat format;

    /**
     * The size of the buffer in bytes.
     * <p>
     * 버퍼 크기(바이트).
     */
    private final int bufferSize;

    /**
     * Guards the fields.
     * <p>
     * 필드를 지킨다.
     */
    private final Object lock = new Object();

    /**
     * The bytes that were written.
     * <p>
     * 쓰인 바이트.
     */
    private final ByteArrayOutputStream recorded = new ByteArrayOutputStream();

    /**
     * The bytes in the buffer.
     * <p>
     * 버퍼에 있는 바이트.
     */
    private int queued;

    /**
     * True while open.
     * <p>
     * 열려 있는 동안 true.
     */
    private boolean open = true;

    /**
     * True while running.
     * <p>
     * 재생 중인 동안 true.
     */
    private boolean running;

    /**
     * The number of flushes.
     * <p>
     * 비움 횟수.
     */
    private int flushes;

    /**
     * Creates an open line.
     * <p>
     * 열린 라인을 만든다.
     *
     * @param format     the format / 형식
     * @param bufferSize the buffer size in bytes / 버퍼 크기(바이트)
     */
    public FakeLine(AudioFormat format, int bufferSize) {
        this.format = format;
        this.bufferSize = bufferSize;
    }

    /**
     * Decodes 16-bit little-endian interleaved bytes into floats (value / 32768), one array per channel.
     * <p>
     * 16비트 리틀 엔디언 인터리브 바이트를 float(값 / 32768)로 풀어 채널마다 하나의 배열로 만든다.
     *
     * @param bytes    the bytes / 바이트
     * @param channels the number of channels / 채널 수
     * @return the channels / 채널
     */
    public static float[][] decode16(byte[] bytes, int channels) {
        int frames = bytes.length / (2 * channels);
        float[][] out = new float[channels][frames];
        // 프레임마다 채널 순서대로 두 바이트를 읽는다.
        for (int i = 0; i < frames; i++) {
            for (int c = 0; c < channels; c++) {
                int p = (i * channels + c) * 2;
                short s = (short) ((bytes[p] & 0xFF) | (bytes[p + 1] << 8));
                out[c][i] = s / 32768f;
            }
        }
        return out;
    }

    /**
     * Plays bytes: the queue becomes smaller by that many bytes (or empty).
     * <p>
     * 바이트를 재생한다: 큐가 그만큼(또는 빌 때까지) 줄어든다.
     *
     * @param bytes the number of bytes / 바이트 수
     */
    public void consume(int bytes) {
        synchronized (lock) {
            queued = Math.max(0, queued - bytes);
        }
    }

    /**
     * Returns the bytes that were written.
     * <p>
     * 쓰인 바이트를 반환한다.
     *
     * @return the bytes / 바이트
     */
    public byte[] getRecorded() {
        synchronized (lock) {
            return recorded.toByteArray();
        }
    }

    /**
     * Returns the number of times the buffer was flushed.
     * <p>
     * 버퍼를 비운 횟수를 반환한다.
     *
     * @return the count / 횟수
     */
    public int getFlushCount() {
        synchronized (lock) {
            return flushes;
        }
    }

    /**
     * Tells whether the line is open.
     * <p>
     * 라인이 열려 있는지 알려 준다.
     *
     * @return true while open / 열려 있는 동안 true
     */
    public boolean isOpen() {
        synchronized (lock) {
            return open;
        }
    }

    /**
     * Records bytes; writing more than the free room is a failure of the caller.
     * <p>
     * 바이트를 기록한다. 빈 자리보다 많이 쓰는 것은 호출한 쪽의 실패이다.
     *
     * @param b   the bytes / 바이트
     * @param off the start / 시작
     * @param len the number of bytes / 바이트 수
     * @return the number of bytes written / 쓴 바이트 수
     */
    public int write(byte[] b, int off, int len) {
        synchronized (lock) {
            if (len > bufferSize - queued) {
                throw new IllegalStateException("write of " + len + " bytes with only " + (bufferSize - queued) + " free");
            }
            queued += len;
            recorded.write(b, off, len);
            return len;
        }
    }

    /**
     * Returns the free room in bytes.
     * <p>
     * 빈 자리(바이트)를 반환한다.
     *
     * @return the free bytes / 빈 바이트 수
     */
    public int available() {
        synchronized (lock) {
            return bufferSize - queued;
        }
    }

    /**
     * Returns the buffer size.
     * <p>
     * 버퍼 크기를 반환한다.
     *
     * @return the size in bytes / 크기(바이트)
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Starts the line.
     * <p>
     * 라인을 시작한다.
     */
    public void start() {
        synchronized (lock) {
            running = true;
        }
    }

    /**
     * Stops the line.
     * <p>
     * 라인을 멈춘다.
     */
    public void stop() {
        synchronized (lock) {
            running = false;
        }
    }

    /**
     * Discards the buffer.
     * <p>
     * 버퍼를 버린다.
     */
    public void flush() {
        synchronized (lock) {
            queued = 0;
            flushes++;
        }
    }

    /**
     * Closes the line.
     * <p>
     * 라인을 닫는다.
     */
    public void close() {
        synchronized (lock) {
            open = false;
            running = false;
        }
    }

    /**
     * Does nothing (not used).
     * <p>
     * 아무것도 하지 않는다(쓰이지 않는다).
     */
    public void drain() {
    }

    /**
     * Tells whether the line runs.
     * <p>
     * 라인이 재생 중인지 알려 준다.
     *
     * @return true while running / 재생 중이면 true
     */
    public boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    /**
     * Tells whether the line is active.
     * <p>
     * 라인이 활성인지 알려 준다.
     *
     * @return true while running / 재생 중이면 true
     */
    public boolean isActive() {
        return isRunning();
    }

    /**
     * Returns the format.
     * <p>
     * 형식을 반환한다.
     *
     * @return the format / 형식
     */
    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Not used.
     * <p>
     * 쓰이지 않는다.
     *
     * @return 0
     */
    public int getFramePosition() {
        return 0;
    }

    /**
     * Not used.
     * <p>
     * 쓰이지 않는다.
     *
     * @return 0
     */
    public long getLongFramePosition() {
        return 0;
    }

    /**
     * Not used.
     * <p>
     * 쓰이지 않는다.
     *
     * @return 0
     */
    public long getMicrosecondPosition() {
        return 0;
    }

    /**
     * Not used.
     * <p>
     * 쓰이지 않는다.
     *
     * @return 0
     */
    public float getLevel() {
        return 0f;
    }

    /**
     * Returns the info of the line.
     * <p>
     * 라인의 정보를 반환한다.
     *
     * @return the info / 정보
     */
    public Info getLineInfo() {
        return new DataLine.Info(SourceDataLine.class, format);
    }

    /**
     * Not used: the line is open from the start.
     * <p>
     * 쓰이지 않는다: 라인은 처음부터 열려 있다.
     */
    public void open() {
    }

    /**
     * Not used: the line is open from the start.
     * <p>
     * 쓰이지 않는다: 라인은 처음부터 열려 있다.
     *
     * @param f the format / 형식
     * @param s the buffer size / 버퍼 크기
     */
    public void open(AudioFormat f, int s) {
    }

    /**
     * Not used: the line is open from the start.
     * <p>
     * 쓰이지 않는다: 라인은 처음부터 열려 있다.
     *
     * @param f the format / 형식
     */
    public void open(AudioFormat f) {
    }

    /**
     * Returns no controls.
     * <p>
     * 컨트롤이 없다.
     *
     * @return an empty array / 빈 배열
     */
    public Control[] getControls() {
        return new Control[0];
    }

    /**
     * Returns false.
     * <p>
     * false 를 반환한다.
     *
     * @param type the type / 종류
     * @return false
     */
    public boolean isControlSupported(Control.Type type) {
        return false;
    }

    /**
     * Not supported.
     * <p>
     * 지원하지 않는다.
     *
     * @param type the type / 종류
     * @return never / 반환하지 않는다
     */
    public Control getControl(Control.Type type) {
        throw new IllegalArgumentException("no controls");
    }

    /**
     * Not used.
     * <p>
     * 쓰이지 않는다.
     *
     * @param listener the listener / 리스너
     */
    public void addLineListener(LineListener listener) {
    }

    /**
     * Not used.
     * <p>
     * 쓰이지 않는다.
     *
     * @param listener the listener / 리스너
     */
    public void removeLineListener(LineListener listener) {
    }
}
