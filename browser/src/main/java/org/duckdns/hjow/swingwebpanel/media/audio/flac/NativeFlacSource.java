package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * Source of a native FLAC file (<code>fLaC</code> followed by metadata and frames). Frames are decoded one at a time. Bytes after the
 * last frame that do not start a valid frame header (tags, padding) end the stream; garbage between frames, a bad frame CRC and invalid
 * subframes are CORRUPT, reported after all PCM before them. Seeking uses the SEEKTABLE when it points to a real frame and otherwise an
 * index of the frame positions that is built while decoding (the file is decoded forward from the last known frame at or before the
 * wanted sample); the position is always exact.
 * <p>
 * 네이티브 FLAC 파일(<code>fLaC</code> 뒤에 메타데이터와 프레임)의 소스. 프레임을 하나씩 디코딩한다. 마지막 프레임 뒤에서 올바른 프레임 헤더로
 * 시작하지 않는 바이트(태그, 패딩)는 스트림을 끝낸다. 프레임 사이의 쓰레기, 틀린 프레임 CRC, 올바르지 않은 서브프레임은 CORRUPT 이며 그 앞의 PCM 을
 * 모두 전달한 뒤에 보고한다. 탐색은 SEEKTABLE 이 실제 프레임을 가리키면 그것을 쓰고 아니면 디코딩하며 만든 프레임 위치 색인을 쓴다(원하는 샘플 이전의
 * 마지막으로 알려진 프레임부터 앞으로 디코딩한다). 위치는 항상 정확하다.
 */
final class NativeFlacSource extends FlacPcmSource {

    /** Initial frame window size. / 처음 프레임 읽기 창 크기. */
    private static final int INITIAL_WINDOW = 64 * 1024;

    /** Largest representable FLAC frame size in STREAMINFO. / STREAMINFO가 표현할 수 있는 가장 큰 프레임 크기. */
    private static final int MAX_FRAME_BYTES = 0xFFFFFF;

    /** Largest possible frame header. / 가장 큰 프레임 헤더의 길이. */
    private final byte[] headerBytes = new byte[16];

    /** Reusable encoded window, retained when the following frame is already present. / 다음 프레임이 들어 있으면 재사용하는 인코딩 창. */
    private byte[] window = new byte[0];

    /** Position in the file of the first byte of the window, or -1. / 읽기 창의 파일 시작 위치. 아직 없으면 -1. */
    private int windowStart = -1;

    /** Valid bytes in the window. / 읽기 창의 유효 바이트 수. */
    private int windowLength;

    /**
     * End of the data.
     * <p>
     * 데이터의 끝.
     */
    private final int end;

    /**
     * Offset of the first frame.
     * <p>
     * 첫 프레임의 위치.
     */
    private final int audioStart;

    /**
     * Seek table sample numbers (without placeholders).
     * <p>
     * 탐색 표의 샘플 번호(자리 표시 항목 제외).
     */
    private final long[] pointSamples;

    /**
     * Seek table offsets relative to the first frame.
     * <p>
     * 첫 프레임에 대한 탐색 표의 상대 위치.
     */
    private final long[] pointOffsets;

    /**
     * Offset of the next frame to decode.
     * <p>
     * 다음에 디코딩할 프레임의 위치.
     */
    private int pos;

    /**
     * Sample number of the next frame.
     * <p>
     * 다음 프레임의 샘플 번호.
     */
    private long cursor;

    /**
     * Sample numbers of the frames that were decoded so far (increasing).
     * <p>
     * 지금까지 디코딩한 프레임의 샘플 번호(증가한다).
     */
    private long[] indexSamples = new long[64];

    /**
     * Offsets of those frames.
     * <p>
     * 그 프레임들의 위치.
     */
    private int[] indexOffsets = new int[64];

    /**
     * Number of index entries.
     * <p>
     * 색인 항목 수.
     */
    private int indexCount;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param data         file data / 파일 데이터
     * @param info         stream parameters / 스트림 파라미터
     * @param audioStart   offset of the first frame / 첫 프레임의 위치
     * @param pointSamples seek table sample numbers / 탐색 표의 샘플 번호
     * @param pointOffsets seek table offsets / 탐색 표의 위치
     */
    NativeFlacSource(ByteSource data, FlacStreamInfo info, int audioStart, long[] pointSamples, long[] pointOffsets) {
        super(data, info, info.totalSamples > 0 ? info.totalSamples : -1);
        this.end = (int) data.length();
        this.audioStart = audioStart;
        this.pointSamples = pointSamples;
        this.pointOffsets = pointOffsets;
        this.pos = audioStart;
    }

    /**
     * Decodes the next frame.
     * <p>
     * 다음 프레임을 디코딩한다.
     *
     * @return the header of the decoded frame or null at the end / 디코딩한 프레임의 헤더. 끝이면 null
     * @throws MediaException CORRUPT for a lost sync, a bad CRC or an invalid frame / 동기를 잃었거나 CRC 가 틀리거나 프레임이 올바르지 않으면 CORRUPT
     */
    FlacFrameDecoder.Header nextFrame() throws MediaException {
        if (pos >= end) {
            return null;
        }
        try {
            FlacFrameDecoder.Header h = readHeader(pos);
            if (h == null) {
                // 올바른 헤더가 이어지지 않는다: 뒤에 올바른 프레임이 있으면 쓰레기가 끼어든 것이고, 없으면 꼬리표 같은 뒤쪽 데이터이다.
                if (findHeader(pos + 1) >= 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "FLAC stream lost sync at offset " + pos);
                }
                pos = end;
                return null;
            }
            int len = decodeFrame(h);
            record(cursor, pos);
            pos += len;
            cursor += h.blockSize;
            return h;
        } catch (FlacFrameDecoder.Truncated t) {
            pos = end;
            return null;
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        }
    }

    /**
     * Reads a complete frame header out of the current window or a small input read. / 현재 창 또는 작은 입력 읽기에서 프레임 헤더를 확인한다.
     * @param at absolute file offset / 파일의 절대 위치
     * @return a valid header, or null / 올바른 헤더 또는 null
     * @throws MediaException if the input cannot be read / 입력을 읽을 수 없으면
     */
    private FlacFrameDecoder.Header readHeader(int at) throws MediaException {
        int count = Math.min(headerBytes.length, end - at);
        int relative = windowStart >= 0 ? at - windowStart : -1;
        if (relative >= 0 && relative <= windowLength - count) {
            return decoder.parseHeader(window, relative, windowLength);
        }
        try {
            Bytes.readFully(data, at, headerBytes, 0, count);
            return decoder.parseHeader(headerBytes, 0, count);
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        }
    }

    /**
     * Tries the current window and expands it if decoding needs more input. / 현재 창으로 디코딩을 시도하고 더 필요한 경우 늘린다.
     * @param header the validated frame header / 검증한 프레임 헤더
     * @return bytes consumed / 사용한 바이트 수
     * @throws MediaException on a damaged frame or size limit / 손상 프레임이거나 크기 제한을 초과하면
     */
    private int decodeFrame(FlacFrameDecoder.Header header) throws MediaException {
        int available = end - pos;
        int limit = Math.min(MAX_FRAME_BYTES, available);
        if (windowStart < 0 || pos < windowStart || pos - windowStart >= windowLength) {
            fillWindow(pos, Math.min(INITIAL_WINDOW, limit));
        }
        // 잘린 위치가 파일 끝이 아니면 창을 확장해서 같은 프레임을 다시 해독한다.
        for (;;) {
            int offset = pos - windowStart;
            try {
                return decoder.decodeFrame(window, offset, windowLength, header);
            } catch (FlacFrameDecoder.Truncated e) {
                int count = windowLength - offset;
                if (count >= available) {
                    throw e;
                }
                if (count >= limit) {
                    throw new MediaException(MediaException.Kind.TOO_LARGE, "FLAC frame exceeds the decoding window limit");
                }
                fillWindow(pos, Math.min(limit, Math.max(INITIAL_WINDOW, count * 2)));
            }
        }
    }

    /**
     * Keeps any overlapping bytes and reads only the missing suffix. / 겹치는 바이트를 유지하고 없는 뒷부분만 읽는다.
     * @param at start of the wanted bytes / 필요한 바이트의 시작
     * @param count number of bytes wanted / 필요한 바이트 수
     */
    private void fillWindow(int at, int count) {
        int relative = windowStart >= 0 ? at - windowStart : -1;
        if (relative >= 0 && relative <= windowLength) {
            int remaining = windowLength - relative;
            if (remaining >= count) {
                return;
            }
            System.arraycopy(window, relative, window, 0, remaining);
            windowLength = remaining;
        } else {
            windowLength = 0;
        }
        if (window.length < count) {
            window = Arrays.copyOf(window, count);
        }
        windowStart = at;
        Bytes.readFully(data, at + windowLength, window, windowLength, count - windowLength);
        windowLength = count;
    }

    /**
     * Finds the next position that holds a valid frame header.
     * <p>
     * 올바른 프레임 헤더가 있는 다음 위치를 찾는다.
     *
     * @param from first offset to examine / 처음 검사할 위치
     * @return offset or -1 / 위치. 없으면 -1
     * @throws MediaException if the scan cannot finish / 검색을 끝낼 수 없으면
     */
    private int findHeader(int from) throws MediaException {
        // 동기 코드가 나오는 위치마다 헤더 CRC 까지 확인한다.
        for (int i = from; i < end - 1; i++) {
            if ((i & 0xFFF) == 0 && Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "FLAC resynchronization was interrupted");
            }
            if (data.get(i) == 0xFF && FlacFrameDecoder.isSync(0xFF, data.get(i + 1))) {
                try {
                    if (readHeader(i) != null) {
                        return i;
                    }
                } catch (FlacFrameDecoder.Truncated t) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Adds a frame to the position index if it is beyond the last indexed frame.
     * <p>
     * 마지막으로 색인한 프레임보다 뒤에 있는 프레임이면 위치 색인에 추가한다.
     *
     * @param sample sample number of the frame / 프레임의 샘플 번호
     * @param offset offset of the frame / 프레임의 위치
     */
    private void record(long sample, int offset) {
        if (indexCount > 0 && indexSamples[indexCount - 1] >= sample) {
            return;
        }
        if (indexCount == indexSamples.length) {
            indexSamples = Arrays.copyOf(indexSamples, indexCount * 2);
            indexOffsets = Arrays.copyOf(indexOffsets, indexCount * 2);
        }
        indexSamples[indexCount] = sample;
        indexOffsets[indexCount] = offset;
        indexCount++;
    }

    /**
     * Returns whether this source can seek.
     * <p>
     * 이 소스가 탐색할 수 있는지 반환한다.
     *
     * @return always true / 항상 true
     */
    protected boolean canSeek() {
        return true;
    }

    /**
     * Moves to the best known frame at or before a sample.
     * <p>
     * 샘플 이전의 알려진 가장 좋은 프레임으로 이동한다.
     *
     * @param frame the wanted sample / 원하는 샘플
     * @return the sample number at which decoding resumes / 디코딩을 다시 시작하는 샘플 번호
     * @throws MediaException if a seek header cannot be read / 탐색 헤더를 읽을 수 없으면
     */
    protected long seekTo(long frame) throws MediaException {
        long bestSample = 0;
        int bestPos = audioStart;
        // 색인에서 원하는 샘플 이하의 마지막 항목을 이분 탐색한다.
        int lo = 0;
        int hi = indexCount - 1;
        // 이분 탐색으로 색인 구간을 좁힌다.
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (indexSamples[mid] <= frame) {
                bestSample = indexSamples[mid];
                bestPos = indexOffsets[mid];
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        // 탐색 표의 항목 중 더 가까운 것이 실제 프레임을 가리키는지 확인해 쓴다.
        for (int i = 0; i < pointSamples.length; i++) {
            if (pointSamples[i] <= frame && pointSamples[i] > bestSample && validPoint(pointSamples[i], pointOffsets[i])) {
                bestSample = pointSamples[i];
                bestPos = (int) (audioStart + pointOffsets[i]);
            }
        }
        pos = bestPos;
        cursor = bestSample;
        return bestSample;
    }

    /**
     * Checks that a seek point refers to a frame that starts at the stated sample.
     * <p>
     * 탐색 지점이 말한 샘플에서 시작하는 프레임을 가리키는지 확인한다.
     *
     * @param sample sample number / 샘플 번호
     * @param offset offset relative to the first frame / 첫 프레임에 대한 상대 위치
     * @return true if the point is valid / 올바른 지점이면 true
     * @throws MediaException if the input cannot be read / 입력을 읽을 수 없으면
     */
    private boolean validPoint(long sample, long offset) throws MediaException {
        if (offset < 0 || offset >= (long) end - 6 - audioStart) {
            return false;
        }
        try {
            FlacFrameDecoder.Header h = readHeader(audioStart + (int) offset);
            if (h == null) {
                return false;
            }
            long start = h.variable ? h.number : h.number * info.maxBlock;
            return start == sample;
        } catch (FlacFrameDecoder.Truncated t) {
            return false;
        }
    }

    /** Releases the encoded window and source. / 인코딩 창과 원천을 해제한다. */
    protected void release() {
        window = new byte[0];
        super.release();
    }
}
