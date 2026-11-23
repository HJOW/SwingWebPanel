package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Base class of {@link PcmSource} implementations that decode in blocks: a subclass only has to produce the next block of decoded PCM
 * ({@link #fill(byte[])}) and, if it can, jump to a position ({@link #seekTo(long)}). This class keeps the output buffer, the frame
 * position, the end of stream and the error state, and gives seeking an exact frame position by decoding forward and discarding. When a
 * block fails to decode, the PCM decoded before it is still delivered: the failure is reported by the read that follows and every later read.
 * <p>
 * 블록 단위로 디코딩하는 {@link PcmSource} 구현의 기반 클래스: 하위 클래스는 다음 PCM 블록을 만들어 내는 일({@link #fill(byte[])})과, 가능하면 위치로
 * 이동하는 일({@link #seekTo(long)})만 하면 된다. 이 클래스는 출력 버퍼, 프레임 위치, 스트림 끝, 오류 상태를 관리하며 앞으로 디코딩하며 버리는 방식으로
 * 탐색이 정확한 프레임 위치에 닿게 한다. 블록 디코딩이 실패해도 그 앞까지 디코딩한 PCM 은 전달하며 실패는 바로 다음 읽기와 이후의 모든 읽기에서 보고한다.
 */
public abstract class BlockPcmSource implements PcmSource {

    /**
     * Default size of the output buffer in frames.
     * <p>
     * 출력 버퍼의 기본 크기(프레임 수).
     */
    public static final int DEFAULT_BUFFER_FRAMES = 4096;

    /**
     * Sample rate.
     * <p>
     * 샘플링 주파수.
     */
    private final int sampleRate;

    /**
     * Number of output channels (1 or 2).
     * <p>
     * 출력 채널 수(1 또는 2).
     */
    private final int channels;

    /**
     * Total frames or -1.
     * <p>
     * 전체 프레임 수 또는 -1.
     */
    private final long frameCount;

    /**
     * Bytes of one output frame.
     * <p>
     * 출력 프레임 하나의 바이트 수.
     */
    private final int frameBytes;

    /**
     * Block buffer handed to {@link #fill(byte[])}.
     * <p>
     * {@link #fill(byte[])} 에 넘기는 블록 버퍼.
     */
    private final byte[] buffer;

    /**
     * Next unread byte of the buffer.
     * <p>
     * 버퍼의 다음에 읽을 바이트.
     */
    private int bufPos;

    /**
     * End of the valid bytes of the buffer.
     * <p>
     * 버퍼의 유효한 바이트의 끝.
     */
    private int bufEnd;

    /**
     * Frame index of the next frame that will be delivered.
     * <p>
     * 다음에 전달할 프레임의 프레임 번호.
     */
    private long position;

    /**
     * True after the subclass reported the end of stream.
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
     * @param sampleRate   frames per second / 초당 프레임 수
     * @param channels     output channels, 1 or 2 / 출력 채널 수(1 또는 2)
     * @param frameCount   total frames or -1 / 전체 프레임 수 또는 -1
     * @param bufferFrames size of the block buffer in frames; must hold the largest block that {@link #fill(byte[])} produces / 블록 버퍼의 프레임 수. {@link #fill(byte[])} 가 만드는 가장 큰 블록을 담을 수 있어야 한다
     */
    protected BlockPcmSource(int sampleRate, int channels, long frameCount, int bufferFrames) {
        if (channels < 1 || channels > 2 || sampleRate < 1 || bufferFrames < 1) {
            throw new IllegalArgumentException("invalid PCM format");
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.frameCount = frameCount < 0 ? -1 : frameCount;
        this.frameBytes = channels * 2;
        this.buffer = new byte[bufferFrames * frameBytes];
    }

    /**
     * Produces the next block of decoded PCM (16-bit signed little-endian interleaved, whole frames).
     * <p>
     * 디코딩된 PCM 의 다음 블록(16비트 부호 있는 리틀 엔디언 인터리브, 완전한 프레임)을 만든다.
     *
     * @param out buffer to fill from index 0; its length is the maximum block size / 0 번부터 채울 버퍼. 길이가 블록의 최대 크기이다
     * @return bytes written (a positive multiple of the frame size), or -1 at the end of the stream / 쓴 바이트 수(프레임 크기의 양의 배수). 스트림 끝이면 -1
     * @throws MediaException if the data is damaged / 데이터가 손상되었으면
     */
    protected abstract int fill(byte[] out) throws MediaException;

    /**
     * Tells whether this source can seek (default no).
     * <p>
     * 이 소스가 탐색할 수 있는지 알려 준다(기본은 아니오).
     *
     * @return true if {@link #seekTo(long)} is implemented / {@link #seekTo(long)} 를 구현했으면 true
     */
    protected boolean canSeek() {
        return false;
    }

    /**
     * Repositions the decoder so that the next {@link #fill(byte[])} starts at the returned frame, which must not be after
     * <code>frame</code>; the base class then discards the frames in between.
     * <p>
     * 다음 {@link #fill(byte[])} 가 반환하는 프레임에서 시작하도록 디코더의 위치를 옮긴다. 반환값은 <code>frame</code> 보다 뒤여서는 안 되며 그 사이의
     * 프레임은 기반 클래스가 버린다.
     *
     * @param frame requested frame, within 0 and the frame count / 요청한 프레임. 0 이상이며 전체 프레임 수 이내
     * @return the frame at which decoding resumes (at most <code>frame</code>) / 디코딩을 다시 시작하는 프레임(<code>frame</code> 이하)
     * @throws MediaException if the data is damaged / 데이터가 손상되었으면
     */
    protected long seekTo(long frame) throws MediaException {
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "seeking is not supported");
    }

    /**
     * Releases the decoder's resources; called once by {@link #close()}.
     * <p>
     * 디코더의 자원을 해제한다. {@link #close()} 가 한 번 호출한다.
     */
    protected void release() {
    }

    /**
     * Returns the sample rate.
     * <p>
     * 샘플링 주파수를 반환한다.
     *
     * @return frames per second / 초당 프레임 수
     */
    public final int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of output channels.
     * <p>
     * 출력 채널 수를 반환한다.
     *
     * @return 1 or 2 / 1 또는 2
     */
    public final int getChannels() {
        return channels;
    }

    /**
     * Returns the total frame count or -1.
     * <p>
     * 전체 프레임 수 또는 -1 을 반환한다.
     *
     * @return frames or -1 / 프레임 수 또는 -1
     */
    public final long getFrameCount() {
        return frameCount;
    }

    /**
     * Returns the frame index of the next frame to be delivered.
     * <p>
     * 다음에 전달할 프레임의 번호를 반환한다.
     *
     * @return position in frames / 프레임 단위 위치
     */
    public final long getPosition() {
        return position;
    }

    /**
     * Tells whether the source can seek.
     * <p>
     * 소스가 탐색할 수 있는지 알려 준다.
     *
     * @return true if seekable / 탐색할 수 있으면 true
     */
    public final boolean isSeekable() {
        return canSeek();
    }

    /**
     * Reads decoded PCM; see {@link PcmSource#read}. Reading after {@link #close()} fails with ACCESS_FAILED.
     * <p>
     * 디코딩된 PCM 을 읽는다. {@link PcmSource#read} 참조. {@link #close()} 뒤에 읽으면 ACCESS_FAILED 로 실패한다.
     *
     * @param dst    buffer to fill / 채울 버퍼
     * @param offset start offset / 시작 위치
     * @param length maximum number of bytes (at least one frame) / 최대 바이트 수(최소 한 프레임)
     * @return bytes read or -1 at the end / 읽은 바이트 수. 끝이면 -1
     * @throws MediaException when the data is damaged or the source is closed / 데이터가 손상되었거나 소스가 닫혔으면
     */
    public final synchronized int read(byte[] dst, int offset, int length) throws MediaException {
        if (offset < 0 || length < frameBytes || (long) offset + length > dst.length) {
            throw new IllegalArgumentException("invalid buffer range");
        }
        int copied = readInternal(dst, offset, length - length % frameBytes);
        return copied == 0 ? -1 : copied;
    }

    /**
     * Copies decoded bytes out, filling the block buffer as needed.
     * <p>
     * 필요하면 블록 버퍼를 채우면서 디코딩된 바이트를 복사해 내보낸다.
     *
     * @param dst    destination (null to discard) / 대상(버리려면 null)
     * @param offset start offset / 시작 위치
     * @param length number of bytes wanted, a multiple of the frame size / 원하는 바이트 수(프레임 크기의 배수)
     * @return bytes delivered; 0 only at the end of the stream / 전달한 바이트 수. 0 이면 스트림의 끝
     * @throws MediaException on failure / 실패 시
     */
    private int readInternal(byte[] dst, int offset, int length) throws MediaException {
        if (closed) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "the audio source is closed");
        }
        int copied = 0;
        // 요청한 만큼 채울 때까지 버퍼를 비우고 다시 채운다.
        while (copied < length) {
            if (bufPos == bufEnd) {
                if (ended) {
                    break;
                }
                if (failure != null) {
                    if (copied > 0) {
                        break;
                    }
                    throw failure;
                }
                refill();
                continue;
            }
            int n = Math.min(length - copied, bufEnd - bufPos);
            if (dst != null) {
                System.arraycopy(buffer, bufPos, dst, offset + copied, n);
            }
            bufPos += n;
            copied += n;
        }
        position += copied / frameBytes;
        return copied;
    }

    /**
     * Asks the subclass for the next block and records the end of stream or the failure.
     * <p>
     * 하위 클래스에 다음 블록을 요청하고 스트림의 끝이나 실패를 기록한다.
     */
    private void refill() {
        bufPos = 0;
        bufEnd = 0;
        try {
            int n = fill(buffer);
            if (n < 0) {
                ended = true;
            } else if (n == 0 || n % frameBytes != 0 || n > buffer.length) {
                failure = new MediaException(MediaException.Kind.CORRUPT, "the decoder returned an invalid block size");
            } else {
                bufEnd = n;
            }
        } catch (MediaException e) {
            failure = e;
        }
    }

    /**
     * Moves to a frame; see {@link PcmSource#seekToFrame(long)}. The position is exact: the decoder resumes at or before the frame and the
     * frames in between are decoded and discarded.
     * <p>
     * 프레임으로 이동한다. {@link PcmSource#seekToFrame(long)} 참조. 위치는 정확하다: 디코더가 그 프레임 이전 또는 그 위치에서 다시 시작하고
     * 그 사이의 프레임은 디코딩해 버린다.
     *
     * @param frame frame index / 프레임 번호
     * @throws MediaException if not seekable, closed or the data is damaged / 탐색할 수 없거나 닫혔거나 데이터가 손상되었으면
     */
    public final synchronized void seekToFrame(long frame) throws MediaException {
        if (closed) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "the audio source is closed");
        }
        if (!canSeek()) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "this audio source cannot seek");
        }
        long target = Math.max(0, frame);
        if (frameCount >= 0 && target > frameCount) {
            target = frameCount;
        }
        failure = null;
        ended = false;
        bufPos = 0;
        bufEnd = 0;
        long actual = Math.min(seekTo(target), target);
        position = Math.max(0, actual);
        long skip = target - position;
        // 요청한 위치까지 디코딩하면서 버린다.
        while (skip > 0) {
            long chunk = Math.min(skip, 4096) * frameBytes;
            int got = readInternal(null, 0, (int) chunk);
            if (got == 0) {
                break;
            }
            skip -= got / frameBytes;
        }
    }

    /**
     * Releases the decoder; later reads fail.
     * <p>
     * 디코더를 해제한다. 이후의 읽기는 실패한다.
     */
    public final synchronized void close() {
        if (!closed) {
            closed = true;
            release();
        }
    }
}
