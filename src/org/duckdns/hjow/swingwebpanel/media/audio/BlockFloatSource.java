package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Base class of {@link FloatPcmSource} implementations that decode in blocks, the counterpart of {@link BlockPcmSource}: a subclass only has to
 * produce the next block of decoded samples ({@link #fill(float[][])}). This class keeps the block buffer, the end of the stream and the
 * error state. When a block fails to decode, the frames decoded before it are still delivered: the failure is reported by the read that follows
 * and every later read. The source does not seek (the wide path is read from the start to the end).
 * <p>
 * 블록 단위로 디코딩하는 {@link FloatPcmSource} 구현의 기반 클래스. {@link BlockPcmSource} 의 짝이다: 하위 클래스는 다음 샘플 블록을 만들어 내는 일
 * ({@link #fill(float[][])})만 하면 된다. 이 클래스는 블록 버퍼, 스트림 끝, 오류 상태를 관리한다. 블록 디코딩이 실패해도 그 앞까지 디코딩한 프레임은
 * 전달하며 실패는 바로 다음 읽기와 이후의 모든 읽기에서 보고한다. 이 소스는 탐색하지 않는다(넓은 경로는 처음부터 끝까지 읽는다).
 */
public abstract class BlockFloatSource implements FloatPcmSource {

    /**
     * Sample rate.
     * <p>
     * 샘플링 주파수.
     */
    private final int sampleRate;

    /**
     * Number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * Total frames or -1.
     * <p>
     * 전체 프레임 수 또는 -1.
     */
    private final long frameCount;

    /**
     * The block buffer handed to {@link #fill(float[][])}: one array per channel.
     * <p>
     * {@link #fill(float[][])} 에 넘기는 블록 버퍼. 채널마다 배열 하나.
     */
    private final float[][] buffer;

    /**
     * Next unread frame of the buffer.
     * <p>
     * 버퍼의 다음에 읽을 프레임.
     */
    private int bufPos;

    /**
     * End of the valid frames of the buffer.
     * <p>
     * 버퍼의 유효한 프레임의 끝.
     */
    private int bufEnd;

    /**
     * True after the subclass reported the end of the stream.
     * <p>
     * 하위 클래스가 스트림의 끝을 알린 뒤에는 true.
     */
    private boolean ended;

    /**
     * True after {@link #close()}.
     * <p>
     * {@link #close()} 뒤에는 true.
     */
    private boolean closed;

    /**
     * The decoding failure that every following read reports.
     * <p>
     * 이후의 모든 읽기가 보고하는 디코딩 실패.
     */
    private MediaException failure;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param sampleRate     frames per second / 초당 프레임 수
     * @param channels       channels, at least 1 / 채널 수(1 이상)
     * @param frameCount     total frames or -1 / 전체 프레임 수 또는 -1
     * @param maxBlockFrames size of the block buffer in frames; must hold the largest block that {@link #fill(float[][])} produces / 블록 버퍼의 프레임 수.
     *                       {@link #fill(float[][])} 가 만드는 가장 큰 블록을 담을 수 있어야 한다
     */
    protected BlockFloatSource(int sampleRate, int channels, long frameCount, int maxBlockFrames) {
        if (channels < 1 || sampleRate < 1 || maxBlockFrames < 1) {
            throw new IllegalArgumentException("invalid PCM format");
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.frameCount = frameCount < 0 ? -1 : frameCount;
        this.buffer = new float[channels][maxBlockFrames];
    }

    /**
     * Produces the next block of decoded samples.
     * <p>
     * 디코딩된 샘플의 다음 블록을 만든다.
     *
     * @param out one array per channel to fill from index 0; the length of the arrays is the maximum block size / 채널마다 0 번부터 채울 배열. 배열의 길이가 블록의
     *            최대 크기이다
     * @return frames written (at least 1), or -1 at the end of the stream / 쓴 프레임 수(1 이상). 스트림 끝이면 -1
     * @throws MediaException if the data is damaged / 데이터가 손상되었으면
     */
    protected abstract int fill(float[][] out) throws MediaException;

    /**
     * Releases decoder resources on {@link #close()}; the default does nothing.
     * <p>
     * {@link #close()} 때 디코더 자원을 해제한다. 기본은 아무것도 하지 않는다.
     */
    protected void release() {
    }

    /**
     * Returns the sample rate.
     * <p>
     * 샘플링 주파수를 반환한다.
     *
     * @return sample rate / 샘플링 주파수
     */
    public final int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of channels.
     * <p>
     * 채널 수를 반환한다.
     *
     * @return channels / 채널 수
     */
    public final int getChannels() {
        return channels;
    }

    /**
     * Returns the total number of frames or -1.
     * <p>
     * 전체 프레임 수 또는 -1 을 반환한다.
     *
     * @return frames or -1 / 프레임 수 또는 -1
     */
    public final long getFrameCount() {
        return frameCount;
    }

    /**
     * Reads frames (see {@link FloatPcmSource#read}).
     * <p>
     * 프레임을 읽는다({@link FloatPcmSource#read} 참조).
     *
     * @param planar one array per channel / 채널마다 배열 하나
     * @param offset first sample index in every array / 모든 배열에서 첫 표본 인덱스
     * @param frames maximum number of frames / 최대 프레임 수
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException if the data is damaged or the source is closed / 데이터가 손상되었거나 소스를 닫았으면
     */
    public final synchronized int read(float[][] planar, int offset, int frames) throws MediaException {
        if (planar == null || planar.length < channels || offset < 0 || frames < 1) {
            throw new IllegalArgumentException("invalid buffer range");
        }
        // 모든 채널 배열이 요청한 범위를 담을 수 있어야 한다.
        for (int c = 0; c < channels; c++) {
            if (planar[c] == null || (long) offset + frames > planar[c].length) {
                throw new IllegalArgumentException("invalid buffer range");
            }
        }
        if (closed) {
            throw new MediaException(MediaException.Kind.CANCELLED, "the audio source is closed");
        }
        int copied = 0;
        // 요청한 만큼 채울 때까지 버퍼를 비우고 다시 채운다.
        while (copied < frames) {
            if (bufPos == bufEnd) {
                if (ended) {
                    break;
                }
                if (failure != null) {
                    // 실패 앞까지 디코딩한 프레임을 먼저 전달하고 실패는 다음 읽기가 보고한다.
                    if (copied > 0) {
                        break;
                    }
                    throw failure;
                }
                refill();
                continue;
            }
            int n = Math.min(frames - copied, bufEnd - bufPos);
            // 채널마다 버퍼의 프레임을 복사한다.
            for (int c = 0; c < channels; c++) {
                System.arraycopy(buffer[c], bufPos, planar[c], offset + copied, n);
            }
            bufPos += n;
            copied += n;
        }
        return copied == 0 ? -1 : copied;
    }

    /**
     * Fills the block buffer with the next block and records the end or the failure.
     * <p>
     * 블록 버퍼를 다음 블록으로 채우고 끝이나 실패를 기록한다.
     */
    private void refill() {
        bufPos = 0;
        bufEnd = 0;
        try {
            int n = fill(buffer);
            if (n < 0) {
                ended = true;
            } else if (n == 0 || n > buffer[0].length) {
                failure = new MediaException(MediaException.Kind.CORRUPT, "the decoder returned an invalid block size");
            } else {
                bufEnd = n;
            }
        } catch (MediaException e) {
            failure = e;
        } catch (RuntimeException e) {
            // 손상된 데이터가 디코더 안에서 배열 범위를 벗어나는 값을 만들면 디코딩 실패로 보고한다.
            failure = new MediaException(MediaException.Kind.CORRUPT, "damaged audio data: " + e, e);
        }
    }

    /**
     * Releases the decoder (idempotent).
     * <p>
     * 디코더를 해제한다(여러 번 불러도 안전).
     */
    public final synchronized void close() {
        if (!closed) {
            closed = true;
            release();
        }
    }
}
