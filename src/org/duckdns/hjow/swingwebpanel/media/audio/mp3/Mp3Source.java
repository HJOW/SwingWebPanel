package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;

/**
 * The {@link PcmSource} of an MP3 file: finds the audio frames (skipping ID3 tags, resynchronizing after garbage), reads the Xing/Info/VBRI
 * and LAME headers for the exact length and the encoder delay, and decodes one frame at a time on demand. Seeking decodes a few frames
 * before the target to rebuild the bit reservoir and the filter state.
 * <p>
 * MP3 파일의 {@link PcmSource}: 오디오 프레임을 찾고(ID3 태그를 건너뛰고 쓰레기 뒤에 다시 동기화), Xing/Info/VBRI 와 LAME 헤더에서 정확한
 * 길이와 인코더 지연을 읽으며, 요청할 때마다 프레임 하나씩 해독한다. 탐색은 목표 앞의 프레임 몇 개를 해독해 비트 저장소와 필터 상태를 복원한다.
 */
final class Mp3Source implements PcmSource {

    /** Number of frames decoded before a seek target to restore the bit reservoir. / 비트 저장소를 복원하려고 탐색 목표 앞에서 해독하는 프레임 수. */
    private static final int SEEK_PREROLL = 8;

    /**
     * Bytes of main data that the bit reservoir can hold at most (MPEG-1: 511).
     * <p>
     * 비트 저장소가 가질 수 있는 주 데이터의 최대 바이트 수(MPEG-1: 511).
     */
    private static final int MAX_RESERVOIR_BYTES = 511;

    /**
     * Largest number of frames that a seek decodes in front of the target.
     * <p>
     * 탐색할 때 목표 앞에서 해독하는 프레임 수의 상한.
     */
    private static final int MAX_PREROLL = 64;

    /** Largest distance searched for a frame after garbage. / 쓰레기 뒤에서 프레임을 찾는 최대 거리. */
    private static final int MAX_RESYNC = 256 * 1024;

    /** Decoder delay of the synthesis filter bank in samples. / 합성 필터 뱅크의 디코더 지연(샘플). */
    private static final int DECODER_DELAY = 529;

    /** Encoded data. / 인코딩된 데이터. */
    private final byte[] data;
    /** Offsets of the audio frames. / 오디오 프레임의 위치. */
    private final int[] offsets;
    /** Number of channels of the output. / 출력의 채널 수. */
    private final int channels;
    /** Sampling frequency. / 샘플링 주파수. */
    private final int sampleRate;
    /** Samples per channel in one frame. / 프레임당 채널별 샘플 수. */
    private final int spf;
    /** Samples dropped at the start (encoder and decoder delay), 0 without LAME tag. / 시작에서 버리는 샘플(인코더, 디코더 지연). LAME 태그가 없으면 0. */
    private final int startSkip;
    /** Total number of output frames. / 출력 프레임 총수. */
    private final long totalFrames;
    /** Stream decoder. / 스트림 디코더. */
    private final Mp3FrameDecoder decoder = new Mp3FrameDecoder();
    /** Decoded floating-point frame [channel][sample]. / 해독된 부동소수점 프레임. */
    private final float[][] frameBuf = new float[2][1152];
    /** PCM bytes of the current frame. / 현재 프레임의 PCM 바이트. */
    private byte[] pcm;
    /** Read position in {@link #pcm}. / {@link #pcm} 의 읽기 위치. */
    private int pcmPos;
    /** Valid length of {@link #pcm}. / {@link #pcm} 의 유효 길이. */
    private int pcmLen;
    /** Next frame to decode. / 다음에 해독할 프레임. */
    private int nextFrame;
    /** Output frames still to drop before output starts (delay, seeking). / 출력이 시작되기 전에 더 버릴 출력 프레임 수(지연, 탐색). */
    private long dropFrames;
    /** Number of output frames delivered or dropped so far, counted from the logical start. / 논리적 시작부터 지금까지 내보냈거나 버린 출력 프레임 수. */
    private long outPos;
    /** First output frame of the block that {@link #decodeBlock()} decoded. / {@link #decodeBlock()} 이 해독한 블록의 첫 출력 프레임. */
    private int blockFrom;
    /** Number of output frames of that block. / 그 블록의 출력 프레임 수. */
    private int blockUsable;
    /** Channels of the frame header of that block (it can differ from the stream). / 그 블록의 프레임 헤더의 채널 수(스트림과 다를 수 있다). */
    private int blockSrcCh;
    /** True after {@link #close()}. / {@link #close()} 뒤에는 true. */
    private boolean closed;

    /**
     * Opens the data.
     * <p>
     * 데이터를 연다.
     *
     * @param data   whole file / 파일 전체
     * @param limits limits / 제한
     * @throws MediaException if no Layer III frame is found / Layer III 프레임이 없으면
     */
    Mp3Source(byte[] data, DecodeLimits limits) throws MediaException {
        if (data.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "audio data is too large");
        }
        this.data = data;
        int start = skipId3v2(data);
        int end = data.length;
        // 끝의 ID3v1 태그(128바이트)는 오디오가 아니다.
        if (end - 128 >= start && data[end - 128] == 'T' && data[end - 127] == 'A' && data[end - 126] == 'G') {
            end -= 128;
        }
        int first = findFirst(data, start, end);
        if (first < 0) {
            // Layer III 프레임이 없을 때 Layer I/II 프레임이 있으면 미지원 형식으로 보고한다.
            if (findFirstAnyLayer(data, start, end)) {
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "MPEG audio Layer I and II are not supported");
            }
            throw new MediaException(MediaException.Kind.CORRUPT, "no MPEG audio frame found");
        }
        Mp3Header fh = Mp3Header.parse(data, first);
        if (fh.layer != 3) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "MPEG audio Layer " + fh.layer + " is not supported");
        }
        int[] list = new int[1024];
        int count = 0;
        int p = first;
        // 프레임 길이만큼 건너뛰며 프레임 위치를 모으고, 어긋나면 다시 동기화한다.
        while (p + 4 <= end) {
            Mp3Header h = Mp3Header.parse(data, p);
            if (h != null && h.layer == 3 && h.sameStream(fh) && p + h.frameBytes <= end) {
                if (count == list.length) {
                    int[] bigger = new int[count * 2];
                    System.arraycopy(list, 0, bigger, 0, count);
                    list = bigger;
                }
                list[count++] = p;
                p += h.frameBytes;
            } else {
                int q = resync(data, p + 1, end, fh);
                if (q < 0) {
                    break;
                }
                p = q;
            }
        }
        int delay = -1;
        int padding = 0;
        int skipFirst = 0;
        int xo = first + 4 + (fh.crc ? 2 : 0) + fh.sideInfoBytes();
        // 첫 프레임이 Xing/Info/VBRI 정보 프레임이면 오디오에서 빼고 LAME 지연을 읽는다.
        if (count > 0 && (matches(data, xo, "Xing") || matches(data, xo, "Info") || matches(data, first + 36, "VBRI"))) {
            skipFirst = 1;
            if (matches(data, xo, "Xing") || matches(data, xo, "Info")) {
                int flags = readInt(data, xo + 4);
                int q = xo + 8;
                if ((flags & 1) != 0) {
                    q += 4;
                }
                if ((flags & 2) != 0) {
                    q += 4;
                }
                if ((flags & 4) != 0) {
                    q += 100;
                }
                if ((flags & 8) != 0) {
                    q += 4;
                }
                if (q + 24 <= data.length && data[q] == 'L') {
                    int b0 = data[q + 21] & 0xFF;
                    int b1 = data[q + 22] & 0xFF;
                    int b2 = data[q + 23] & 0xFF;
                    delay = (b0 << 4) | (b1 >> 4);
                    padding = ((b1 & 15) << 8) | b2;
                }
            }
        }
        int n = count - skipFirst;
        if (n <= 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "no audio frames");
        }
        this.offsets = new int[n];
        System.arraycopy(list, skipFirst, offsets, 0, n);
        this.channels = fh.channels();
        this.sampleRate = fh.sampleRate;
        this.spf = fh.samplesPerFrame();
        long total = (long) n * spf;
        if (delay >= 0) {
            startSkip = delay + DECODER_DELAY;
            long endTrim = Math.max(0, padding - DECODER_DELAY);
            total = Math.max(0, total - startSkip - endTrim);
        } else {
            startSkip = 0;
        }
        this.totalFrames = total;
        this.pcm = new byte[spf * channels * 2];
        this.dropFrames = startSkip;
    }

    /**
     * Skips an ID3v2 tag at the start of the data.
     * <p>
     * 데이터 시작의 ID3v2 태그를 건너뛴다.
     *
     * @param d data / 데이터
     * @return position after the tag (0 if none) / 태그 뒤의 위치(없으면 0)
     */
    static int skipId3v2(byte[] d) {
        if (d.length >= 10 && d[0] == 'I' && d[1] == 'D' && d[2] == '3') {
            int size = ((d[6] & 0x7F) << 21) | ((d[7] & 0x7F) << 14) | ((d[8] & 0x7F) << 7) | (d[9] & 0x7F);
            int total = 10 + size + ((d[5] & 0x10) != 0 ? 10 : 0);
            if (total <= d.length) {
                return total;
            }
        }
        return 0;
    }

    /**
     * Finds the first position where a chain of valid Layer III headers starts.
     * <p>
     * 올바른 Layer III 헤더가 연이어 나오는 첫 위치를 찾는다.
     *
     * @param d     data / 데이터
     * @param start start of the search / 검색 시작
     * @param end   end of the audio / 오디오 끝
     * @return position or -1 / 위치 또는 -1
     */
    static int findFirst(byte[] d, int start, int end) {
        int limit = Math.min(end - 4, start + MAX_RESYNC);
        // 한 바이트씩 옮기며 헤더가 두 프레임 연속으로 맞는 곳을 찾는다.
        for (int p = start; p <= limit; p++) {
            Mp3Header h = Mp3Header.parse(d, p);
            if (h != null && h.layer == 3 && chainOk(d, p, h, end)) {
                return p;
            }
        }
        return -1;
    }

    /**
     * Tells whether a chain of frames of any layer starts within the search range.
     * <p>
     * 검색 범위 안에서 계층에 상관없이 프레임 연속이 시작하는지 알려 준다.
     *
     * @param d     data / 데이터
     * @param start start of the search / 검색 시작
     * @param end   end of the audio / 오디오 끝
     * @return true if found / 찾으면 true
     */
    private static boolean findFirstAnyLayer(byte[] d, int start, int end) {
        int limit = Math.min(end - 4, start + MAX_RESYNC);
        for (int p = start; p <= limit; p++) { // 한 바이트씩 옮기며 프레임 연속을 찾는다
            Mp3Header h = Mp3Header.parse(d, p);
            if (h != null && chainOk(d, p, h, end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for the next frame of the same stream after garbage.
     * <p>
     * 쓰레기 뒤에서 같은 스트림의 다음 프레임을 찾는다.
     *
     * @param d    data / 데이터
     * @param from start of the search / 검색 시작
     * @param end  end of the audio / 오디오 끝
     * @param ref  header of the stream / 스트림의 헤더
     * @return position or -1 / 위치 또는 -1
     */
    private static int resync(byte[] d, int from, int end, Mp3Header ref) {
        int limit = Math.min(end - 4, from + MAX_RESYNC);
        // 다음 프레임을 찾을 때까지 한 바이트씩 옮긴다.
        for (int p = from; p <= limit; p++) {
            Mp3Header h = Mp3Header.parse(d, p);
            if (h != null && h.layer == 3 && h.sameStream(ref) && chainOk(d, p, h, end)) {
                return p;
            }
        }
        return -1;
    }

    /**
     * Checks that a frame is followed by another valid frame of the same stream (or ends exactly at the end of the data).
     * <p>
     * 프레임 뒤에 같은 스트림의 올바른 프레임이 이어지는지(또는 데이터 끝에서 정확히 끝나는지) 확인한다.
     *
     * @param d   data / 데이터
     * @param p   position of the frame / 프레임 위치
     * @param h   its header / 그 헤더
     * @param end end of the audio / 오디오 끝
     * @return true if plausible / 그럴듯하면 true
     */
    private static boolean chainOk(byte[] d, int p, Mp3Header h, int end) {
        int next = p + h.frameBytes;
        if (next > end) {
            return false;
        }
        if (next + 4 > end) {
            return true;
        }
        Mp3Header n = Mp3Header.parse(d, next);
        return n != null && n.sameStream(h);
    }

    /**
     * Tells whether ASCII text is at a position.
     * <p>
     * 위치에 ASCII 문자열이 있는지 알려 준다.
     *
     * @param d   data / 데이터
     * @param p   position / 위치
     * @param tag text / 문자열
     * @return true if equal / 같으면 true
     */
    private static boolean matches(byte[] d, int p, String tag) {
        if (p < 0 || p + tag.length() > d.length) {
            return false;
        }
        for (int i = 0; i < tag.length(); i++) { // 문자열의 글자를 비교한다
            if (d[p + i] != tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads a big-endian 32-bit integer.
     * <p>
     * 빅 엔디언 32비트 정수를 읽는다.
     *
     * @param d data / 데이터
     * @param p position / 위치
     * @return value / 값
     */
    private static int readInt(byte[] d, int p) {
        if (p + 4 > d.length) {
            return 0;
        }
        return ((d[p] & 0xFF) << 24) | ((d[p + 1] & 0xFF) << 16) | ((d[p + 2] & 0xFF) << 8) | (d[p + 3] & 0xFF);
    }

    /**
     * Tells whether the data starts like an MP3 file (an ID3v2 tag or a chain of frames).
     * <p>
     * 데이터가 MP3 파일처럼 시작하는지 알려 준다(ID3v2 태그 또는 이어지는 프레임).
     *
     * @param d data / 데이터
     * @return true if an MP3 stream is found / MP3 스트림이 있으면 true
     */
    static boolean looksLikeMp3(byte[] d) {
        int start = skipId3v2(d);
        int found = findFirst(d, start, d.length);
        return found >= 0 && found - start < 65536;
    }

    /** Returns the sampling frequency in Hz. / 샘플링 주파수(Hz)를 반환한다. */
    public int getSampleRate() {
        return sampleRate;
    }

    /** Returns the number of channels of the output (1 or 2). / 출력의 채널 수(1 또는 2)를 반환한다. */
    public int getChannels() {
        return channels;
    }

    /** Returns the number of PCM frames after trimming the encoder delay and padding. / 인코더 지연과 패딩을 제외한 PCM 프레임 수를 반환한다. */
    public long getFrameCount() {
        return totalFrames;
    }

    /** MP3 streams are always seekable. / MP3 스트림은 항상 탐색할 수 있다. */
    public boolean isSeekable() {
        return true;
    }

    /** Reads decoded 16-bit little-endian PCM, decoding frames as needed. / 해독된 16비트 리틀 엔디언 PCM 을 읽으며 필요한 프레임을 해독한다. */
    public int read(byte[] buffer, int offset, int length) throws MediaException {
        if (closed) {
            throw new MediaException(MediaException.Kind.CANCELLED, "the audio source is closed");
        }
        int frameSize = channels * 2;
        int max = length - (length % frameSize);
        if (max < frameSize) {
            throw new IllegalArgumentException("length is smaller than one frame");
        }
        int written = 0;
        // 요청한 만큼 채우거나 데이터가 끝날 때까지 프레임을 하나씩 해독한다.
        while (written < max) {
            if (pcmPos >= pcmLen) {
                if (!decodeNext()) {
                    break;
                }
                continue;
            }
            int n = Math.min(pcmLen - pcmPos, max - written);
            System.arraycopy(pcm, pcmPos, buffer, offset + written, n);
            pcmPos += n;
            written += n;
        }
        return written == 0 ? -1 : written;
    }

    /**
     * Decodes the next frame into {@link #pcm}, applying the start delay and the end trim.
     * <p>
     * 다음 프레임을 {@link #pcm} 으로 해독하며 시작 지연과 끝 자르기를 적용한다.
     *
     * @return false at the end of the stream / 스트림 끝이면 false
     * @throws MediaException if the thread was interrupted / 스레드가 인터럽트되었으면
     */
    private boolean decodeNext() throws MediaException {
        if (!decodeBlock()) {
            return false;
        }
        int bi = 0;
        // 샘플을 16비트 PCM 바이트로 바꾼다.
        for (int i = 0; i < blockUsable; i++) {
            for (int c = 0; c < channels; c++) {
                float f = sampleOf(c, blockFrom + i);
                int s = (int) Math.floor(f * 32768.0f + 0.5f);
                if (s > 32767) {
                    s = 32767;
                } else if (s < -32768) {
                    s = -32768;
                }
                pcm[bi++] = (byte) s;
                pcm[bi++] = (byte) (s >> 8);
            }
        }
        pcmPos = 0;
        pcmLen = bi;
        return true;
    }

    /**
     * Returns the float sample of an output channel at an index of the block that {@link #decodeBlock()} decoded: the channel itself if the frame
     * has the channel layout of the stream, the mean of both channels if the stream is mono but the frame is not, the first channel if the stream is
     * stereo but the frame is not.
     * <p>
     * {@link #decodeBlock()} 이 해독한 블록의 인덱스에서 출력 채널의 float 샘플을 반환한다: 프레임이 스트림의 채널 배치이면 그 채널, 스트림은 모노인데 프레임이
     * 아니면 두 채널의 평균, 스트림은 스테레오인데 프레임이 아니면 첫 채널.
     *
     * @param c     the output channel / 출력 채널
     * @param index the index in the decoded frame / 해독한 프레임 안의 인덱스
     * @return the sample / 샘플
     */
    float sampleOf(int c, int index) {
        if (blockSrcCh == channels) {
            return frameBuf[c][index];
        } else if (channels == 1) {
            return (frameBuf[0][index] + frameBuf[1][index]) * 0.5f;
        }
        return frameBuf[0][index];
    }

    /**
     * Returns the frames of the block that {@link #decodeBlock()} decoded that are output: <code>blockUsable()</code> frames from
     * <code>blockFrom()</code> (the start delay and the end trim are already applied).
     * <p>
     * {@link #decodeBlock()} 이 해독한 블록에서 출력하는 프레임: <code>blockFrom()</code> 부터 <code>blockUsable()</code> 프레임(시작 지연과 끝 자르기는 이미 적용했다).
     *
     * @return the number of output frames, possibly 0 / 출력하는 프레임 수. 0 일 수 있다
     */
    int blockUsable() {
        return blockUsable;
    }

    /**
     * Returns the index of the first output frame of the block.
     * <p>
     * 블록의 첫 출력 프레임의 인덱스를 반환한다.
     *
     * @return the index / 인덱스
     */
    int blockFrom() {
        return blockFrom;
    }

    /**
     * Decodes the next frame into {@link #frameBuf} and works out which of its samples are output (the start delay and the end trim); both
     * the 16 bit path ({@link #decodeNext()}) and the wide path ({@link Mp3FloatSource}) read through this method.
     * <p>
     * 다음 프레임을 {@link #frameBuf} 로 해독하고 그 표본 중 어느 것을 출력하는지 정한다(시작 지연과 끝 자르기). 16비트 경로({@link #decodeNext()})와
     * 넓은 경로({@link Mp3FloatSource})가 모두 이 메서드로 읽는다.
     *
     * @return false at the end of the stream / 스트림 끝이면 false
     * @throws MediaException if the thread was interrupted / 스레드가 인터럽트되었으면
     */
    boolean decodeBlock() throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "decoding was interrupted");
        }
        if (nextFrame >= offsets.length || outPos >= totalFrames) {
            return false;
        }
        int off = offsets[nextFrame++];
        Mp3Header h = Mp3Header.parse(data, off);
        int srcCh = h == null ? channels : h.channels();
        boolean ok = false;
        if (h != null) {
            try {
                ok = decoder.decode(data, off, h, frameBuf);
            } catch (Mp3FrameDecoder.FormatException e) {
                ok = false;
            }
        }
        if (!ok) {
            // 해독하지 못한 프레임은 무음으로 처리한다.
            for (int c = 0; c < 2; c++) {
                java.util.Arrays.fill(frameBuf[c], 0f);
            }
        }
        int count = h == null ? spf : h.samplesPerFrame();
        int from = 0;
        // 시작 지연이나 탐색으로 버려야 할 프레임을 건너뛴다.
        if (dropFrames > 0) {
            int d = (int) Math.min(dropFrames, count);
            from = d;
            dropFrames -= d;
        }
        long remaining = totalFrames - outPos;
        int usable = (int) Math.min(count - from, remaining);
        blockFrom = from;
        blockUsable = usable;
        blockSrcCh = srcCh;
        outPos += usable;
        return true;
    }

    /**
     * Finds the frame at which a seek has to start decoding so that the target frame comes out exactly as in a sequential decoding. The two frames in
     * front of the target (the overlap of the transform and the history of the synthesis filter) must themselves be decodable, and for that the bit
     * reservoir has to be filled with the main data of the frames before them (up to 511 bytes): with small VBR frames of 104 bytes that takes more than
     * {@link #SEEK_PREROLL} frames, so the start goes back until enough main data has been seen (found with a 12 second VBR file of libmp3lame).
     * <p>
     * 탐색이 어느 프레임에서 해독을 시작해야 목표 프레임이 순차 디코딩과 똑같이 나오는지 찾는다. 목표 앞의 두 프레임(변환의 겹침과 합성 필터의 기록)도 해독되어야
     * 하고, 그러려면 그 앞 프레임들의 주 데이터로 비트 저장소(최대 511바이트)가 채워져 있어야 한다. 104바이트짜리 작은 VBR 프레임에서는
     * {@link #SEEK_PREROLL} 프레임보다 많이 필요하므로 충분한 주 데이터를 볼 때까지 시작점을 앞으로 옮긴다(libmp3lame 이 만든 12초 VBR 파일에서 발견).
     *
     * @param fi index of the frame that contains the target / 목표가 든 프레임의 번호
     * @return index of the first frame to decode / 해독을 시작할 첫 프레임의 번호
     */
    private int prerollStart(int fi) {
        int start = Math.max(0, fi - SEEK_PREROLL);
        int s = fi - 2;
        long bytes = 0;
        // 저장소를 채울 만큼의 주 데이터가 모일 때까지(최대 MAX_PREROLL 프레임) 앞으로 거슬러 올라간다.
        while (s > 0 && bytes < MAX_RESERVOIR_BYTES && fi - s < MAX_PREROLL) {
            s--;
            Mp3Header h = Mp3Header.parse(data, offsets[s]);
            if (h != null) {
                bytes += Math.max(0, h.frameBytes - 4 - (h.crc ? 2 : 0) - h.sideInfoBytes());
            }
        }
        return Math.max(0, Math.min(start, s));
    }

    /** Seeks to a PCM frame by decoding a few frames in front of it to restore the decoder state. / 디코더 상태를 복원하려고 앞쪽 프레임 몇 개를 해독해 PCM 프레임 위치로 이동한다. */
    public void seekToFrame(long frame) throws MediaException {
        if (closed) {
            throw new MediaException(MediaException.Kind.CANCELLED, "the audio source is closed");
        }
        long target = Math.max(0, Math.min(frame, totalFrames));
        long absolute = target + startSkip;
        int fi = (int) Math.min(offsets.length, absolute / spf);
        int start = prerollStart(fi);
        decoder.reset();
        nextFrame = start;
        pcmPos = 0;
        pcmLen = 0;
        // 앞쪽 프레임 해독분과 목표까지의 샘플을 모두 버린다.
        dropFrames = absolute - (long) start * spf;
        outPos = target;
    }

    /** Releases the source; later reads fail. / 소스를 해제하며 이후 읽기는 실패한다. */
    public void close() {
        closed = true;
        pcm = new byte[0];
    }
}
