package org.duckdns.hjow.swingwebpanel.media.audio.webm;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * A demultiplexer of WebM / Matroska files (EBML, RFC 8794 and the Matroska specification): it reads the segment info (time scale), the
 * audio tracks (codec id, channels, sampling frequency, codec private data, codec delay) and every block of every cluster (SimpleBlock and
 * BlockGroup, with Xiph, EBML and fixed-size lacing), also in files with unknown-size segments and clusters (live style). The frames are
 * kept as positions in the original data, so no frame is copied.
 * <p>
 * WebM / Matroska 파일(EBML, RFC 8794 와 Matroska 규격)의 역다중화기: 세그먼트 정보(시간 단위), 오디오 트랙(코덱 id, 채널, 샘플링 주파수, 코덱 비공개
 * 데이터, 코덱 지연), 모든 클러스터의 모든 블록(SimpleBlock 과 BlockGroup, Xiph/EBML/고정 크기 레이싱)을 읽으며 크기를 모르는 세그먼트와 클러스터(라이브
 * 방식)가 있는 파일도 다룬다. 프레임은 원본 데이터 안의 위치로 보관하므로 복사하지 않는다.
 */
public final class WebmFile {

    /** Largest retained format or codec identifier. / 보관할 형식·코덱 식별자의 최대 크기. */
    private static final int MAX_IDENTIFIER_BYTES = 128;

    /** Largest retained codec initialization record. / 보관할 코덱 초기화 레코드의 최대 크기. */
    private static final int MAX_CODEC_PRIVATE_BYTES = 1024 * 1024;

    /** Largest number of frames accepted. / 받아들이는 최대 프레임 수. */
    private static final int MAX_FRAMES = 4 * 1000 * 1000;

    /** Element ids of the children of a segment, used to find the end of an unknown-size cluster. / 크기를 모르는 클러스터의 끝을 찾는 데 쓰는 세그먼트 자식 id. */
    private static final int[] SEGMENT_CHILDREN = { 0x1F43B675, 0x1C53BB6B, 0x1254C367, 0x1043A770, 0x1941A469, 0x1549A966, 0x1654AE6B, 0x114D9B74 };

    /** One audio track. / 오디오 트랙 하나. */
    public static final class Track {
        /** Track number used by the blocks. / 블록이 쓰는 트랙 번호. */
        int number;
        /** Track type (2 = audio). / 트랙 종류(2 = 오디오). */
        int type;
        /** Codec id, for example A_OPUS. / 코덱 id. 예: A_OPUS. */
        String codecId = "";
        /** Codec private data. / 코덱 비공개 데이터. */
        byte[] codecPrivate = new byte[0];
        /** Number of channels. / 채널 수. */
        int channels = 1;
        /** Sampling frequency. / 샘플링 주파수. */
        double sampleRate = 8000.0;
        /** Codec delay in nanoseconds. / 코덱 지연(나노초). */
        long codecDelay;
        /** Seek pre-roll in nanoseconds. / 탐색 사전 구간(나노초). */
        long seekPreRoll;
        /** DiscardPadding of the last block that has one, in nanoseconds (samples to drop at the end of the track). / 있는 마지막 블록의 DiscardPadding(나노초. 트랙의 끝에서 버릴 샘플). */
        long discardPadding;
        /** Offsets of the frames. / 프레임의 위치. */
        int[] offsets = new int[64];
        /** Sizes of the frames. / 프레임의 크기. */
        int[] sizes = new int[64];
        /** Presentation time of the frames in nanoseconds. / 프레임의 재생 시각(나노초). */
        long[] times = new long[64];
        /** Duration of the frames in nanoseconds (0 if unknown). / 프레임의 길이(나노초, 모르면 0). */
        long[] durations = new long[64];
        /** Number of frames. / 프레임 수. */
        int count;

        /**
         * Returns the track number. / 트랙 번호를 반환한다.
         *
         * @return number / 번호
         */
        public int getNumber() {
            return number;
        }

        /**
         * Returns the codec id. / 코덱 id 를 반환한다.
         *
         * @return codec id / 코덱 id
         */
        public String getCodecId() {
            return codecId;
        }

        /**
         * Returns the codec private data. / 코덱 비공개 데이터를 반환한다.
         *
         * @return bytes / 바이트
         */
        public byte[] getCodecPrivate() {
            return codecPrivate;
        }

        /**
         * Returns the number of channels. / 채널 수를 반환한다.
         *
         * @return channels / 채널 수
         */
        public int getChannels() {
            return channels;
        }

        /**
         * Returns the sampling frequency. / 샘플링 주파수를 반환한다.
         *
         * @return Hz / Hz
         */
        public double getSampleRate() {
            return sampleRate;
        }

        /**
         * Returns the codec delay in nanoseconds. / 코덱 지연(나노초)을 반환한다.
         *
         * @return nanoseconds / 나노초
         */
        public long getCodecDelay() {
            return codecDelay;
        }

        /**
         * Returns the seek pre-roll in nanoseconds. / 탐색 사전 구간(나노초)을 반환한다.
         *
         * @return nanoseconds / 나노초
         */
        public long getSeekPreRoll() {
            return seekPreRoll;
        }

        /**
         * Returns the DiscardPadding of the track: the time at the end of the track that is not played (Opus).
         * <p>
         * 트랙의 DiscardPadding 을 반환한다: 트랙의 끝에서 재생하지 않는 시간(Opus).
         *
         * @return nanoseconds, 0 if none / 나노초. 없으면 0
         */
        public long getDiscardPadding() {
            return discardPadding;
        }

        /**
         * Returns the number of frames. / 프레임 수를 반환한다.
         *
         * @return count / 개수
         */
        public int getFrameCount() {
            return count;
        }

        /**
         * Returns the offset of a frame. / 프레임의 위치를 반환한다.
         *
         * @param i frame index / 프레임 번호
         * @return offset in the file / 파일 안의 위치
         */
        public int getOffset(int i) {
            return offsets[i];
        }

        /**
         * Returns the size of a frame. / 프레임의 크기를 반환한다.
         *
         * @param i frame index / 프레임 번호
         * @return bytes / 바이트
         */
        public int getSize(int i) {
            return sizes[i];
        }

        /**
         * Returns the time of a frame in nanoseconds. / 프레임의 시각(나노초)을 반환한다.
         *
         * @param i frame index / 프레임 번호
         * @return nanoseconds / 나노초
         */
        public long getTime(int i) {
            return times[i];
        }

        /**
         * Returns the duration of a frame in nanoseconds, 0 if the file does not tell it. / 프레임의 길이(나노초)를 반환하며 파일이 알려 주지 않으면 0 이다.
         *
         * @param i frame index / 프레임 번호
         * @return nanoseconds / 나노초
         */
        public long getFrameDuration(int i) {
            return durations[i];
        }

        /**
         * Appends a frame. / 프레임을 덧붙인다.
         *
         * @param off      offset / 위치
         * @param size     size / 크기
         * @param time     time / 시각
         * @param duration duration / 길이
         * @throws MediaException TOO_LARGE if there are too many frames / 프레임이 너무 많으면 TOO_LARGE
         */
        void add(int off, int size, long time, long duration) throws MediaException {
            if (count >= MAX_FRAMES) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "too many frames");
            }
            if (count == offsets.length) {
                offsets = java.util.Arrays.copyOf(offsets, count * 2);
                sizes = java.util.Arrays.copyOf(sizes, count * 2);
                times = java.util.Arrays.copyOf(times, count * 2);
                durations = java.util.Arrays.copyOf(durations, count * 2);
            }
            offsets[count] = off;
            sizes[count] = size;
            times[count] = time;
            durations[count] = duration;
            count++;
        }
    }

    /** The data. / 데이터. */
    private final ByteSource d;
    /** Audio and other tracks by file order. / 파일 순서대로의 트랙. */
    private final List<Track> tracks = new ArrayList<Track>();
    /** Time scale in nanoseconds per tick. / 틱당 나노초(시간 단위). */
    private long timecodeScale = 1000000L;
    /** Document type text. / 문서 종류 문자열. */
    private String docType = "";
    /** Cluster time code in ticks. / 클러스터 시간 코드(틱). */
    private long clusterTime;

    /** DiscardPadding of the block group that is being read, or 0. / 읽는 중인 블록 그룹의 DiscardPadding. 없으면 0. */
    private long pendingDiscard;

    /**
     * Private constructor. / 비공개 생성자.
     *
     * @param d data / 데이터
     */
    private WebmFile(ByteSource d) {
        this.d = d;
    }

    /**
     * Tells whether the data starts with the EBML magic number.
     * <p>
     * 데이터가 EBML 표식으로 시작하는지 알려 준다.
     *
     * @param d data / 데이터
     * @return true for EBML / EBML 이면 true
     */
    public static boolean looksLikeEbml(byte[] d) {
        return d != null && looksLikeEbmlSource(new ArrayByteSource(d));
    }

    /**
     * Checks the EBML signature from a seekable source. / 탐색 가능한 원천의 EBML 시그니처를 확인한다.
     * @param d encoded input / 인코딩 입력
     * @return true for EBML / EBML이면 true
     */
    public static boolean looksLikeEbmlSource(ByteSource d) {
        return Bytes.has(d, 0, 4) && Bytes.be32(d, 0) == 0x1A45DFA3L;
    }

    /**
     * Parses a file.
     * <p>
     * 파일을 해석한다.
     *
     * @param d data / 데이터
     * @return the parsed file / 해석된 파일
     * @throws MediaException CORRUPT if the structure is damaged / 구조가 손상되었으면 CORRUPT
     */
    public static WebmFile parse(byte[] d) throws MediaException {
        return parseSource(new ArrayByteSource(d));
    }

    /**
     * Reads track metadata and frame indexes while the caller retains input ownership.
     * <p>
     * 호출자가 입력을 소유하는 동안 트랙 메타데이터와 프레임 색인을 읽는다.
     * @param d encoded input / 인코딩 입력
     * @return the parsed file / 해석된 파일
     * @throws MediaException if the data cannot be parsed / 데이터를 해석할 수 없으면
     */
    public static WebmFile parseSource(ByteSource d) throws MediaException {
        if (d.length() > Integer.MAX_VALUE) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "WebM data exceeds the container-offset limit");
        }
        WebmFile f = new WebmFile(d);
        try {
            f.level(0, (int) d.length(), 0);
            return f;
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        }
    }

    /**
     * Returns all tracks. / 모든 트랙을 반환한다.
     *
     * @return tracks / 트랙
     */
    public List<Track> getTracks() {
        return tracks;
    }

    /**
     * Returns the document type (webm or matroska). / 문서 종류(webm 또는 matroska)를 반환한다.
     *
     * @return type / 종류
     */
    public String getDocType() {
        return docType;
    }

    /**
     * Returns the first audio track, or null. / 첫 오디오 트랙을 반환하며 없으면 null 이다.
     *
     * @return track or null / 트랙 또는 null
     */
    public Track firstAudioTrack() {
        for (Track t : tracks) { // 종류가 오디오이고 프레임이 있는 첫 트랙을 찾는다
            if (t.type == 2 && t.count > 0) {
                return t;
            }
        }
        return null;
    }

    /**
     * Reads an element id with its marker bits.
     * <p>
     * 표지 비트를 포함한 요소 id 를 읽는다.
     *
     * @param pos position holder / 위치 보관
     * @param end limit / 한계
     * @return the id or -1 / id 또는 -1
     */
    private int readId(int[] pos, int end) {
        if (pos[0] >= end) {
            return -1;
        }
        int b = d.get(pos[0]);
        int len = Integer.numberOfLeadingZeros(b) - 24 + 1;
        if (len < 1 || len > 4 || len > end - pos[0]) {
            return -1;
        }
        int id = 0;
        for (int i = 0; i < len; i++) { // 길이만큼 바이트를 모은다
            id = (id << 8) | d.get(pos[0] + i);
        }
        pos[0] += len;
        return id;
    }

    /**
     * Reads an element size; -1 means unknown size, -2 an invalid value.
     * <p>
     * 요소 크기를 읽는다. -1 은 크기 모름, -2 는 올바르지 않은 값이다.
     *
     * @param pos position holder / 위치 보관
     * @param end limit / 한계
     * @return the size / 크기
     */
    private long readSize(int[] pos, int end) {
        if (pos[0] >= end) {
            return -2;
        }
        int b = d.get(pos[0]);
        int len = Integer.numberOfLeadingZeros(b) - 24 + 1;
        if (len < 1 || len > 8 || len > end - pos[0]) {
            return -2;
        }
        long v = b & (0xFF >> len);
        boolean allOnes = v == (0xFF >> len);
        for (int i = 1; i < len; i++) { // 나머지 바이트를 이어 붙인다
            int x = d.get(pos[0] + i);
            v = (v << 8) | x;
            if (x != 0xFF) {
                allOnes = false;
            }
        }
        pos[0] += len;
        return allOnes ? -1 : v;
    }

    /**
     * Reads an unsigned integer element body.
     * <p>
     * 부호 없는 정수 요소 본문을 읽는다.
     *
     * @param p    start / 시작
     * @param size size / 크기
     * @return value / 값
     */
    private long uint(int p, int size) {
        long v = 0;
        for (int i = 0; i < size && i < 8; i++) { // 빅 엔디언으로 모은다
            v = (v << 8) | d.get(p + i);
        }
        return v;
    }

    /**
     * Reads a float element body (4 or 8 bytes).
     * <p>
     * 실수 요소 본문(4 또는 8바이트)을 읽는다.
     *
     * @param p    start / 시작
     * @param size size / 크기
     * @return value / 값
     */
    private double real(int p, int size) {
        if (size == 4) {
            return Float.intBitsToFloat((int) uint(p, 4));
        }
        if (size == 8) {
            return Double.longBitsToDouble(uint(p, 8));
        }
        return 0.0;
    }

    /**
     * Walks the elements of a range.
     * <p>
     * 범위의 요소를 훑는다.
     *
     * @param start start of the range / 범위 시작
     * @param end   end of the range / 범위 끝
     * @param depth nesting depth / 중첩 깊이
     * @throws MediaException on damaged structure / 구조가 손상되었을 때
     */
    private void level(int start, int end, int depth) throws MediaException {
        if (depth > 8) {
            throw new MediaException(MediaException.Kind.CORRUPT, "EBML elements are nested too deeply");
        }
        int[] pos = { start };
        while (pos[0] < end) { // 요소를 하나씩 읽는다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "WebM indexing was interrupted");
            }
            int id = readId(pos, end);
            if (id < 0) {
                return;
            }
            long size = readSize(pos, end);
            if (size == -2) {
                return;
            }
            int body = pos[0];
            long limit = size < 0 ? end : body + size;
            if (limit > end) {
                limit = end;
            }
            int bodyEnd = (int) limit;
            if (id == 0x1A45DFA3) {
                readHeader(body, bodyEnd);
            } else if (id == 0x18538067) {
                level(body, bodyEnd, depth + 1);
            } else if (id == 0x1549A966) {
                readInfo(body, bodyEnd);
            } else if (id == 0x1654AE6B) {
                readTracks(body, bodyEnd);
            } else if (id == 0x1F43B675) {
                int stop = size < 0 ? clusterEnd(body, bodyEnd) : bodyEnd;
                readCluster(body, stop);
                pos[0] = stop;
                continue;
            }
            pos[0] = bodyEnd;
        }
    }

    /**
     * Finds the end of an unknown-size cluster: the position of the next element that cannot belong to it.
     * <p>
     * 크기를 모르는 클러스터의 끝을 찾는다: 클러스터에 속할 수 없는 다음 요소의 위치.
     *
     * @param start start of the body / 본문 시작
     * @param end   end of the data / 데이터 끝
     * @return position / 위치
     * @throws MediaException if interrupted / 인터럽트되었으면
     */
    private int clusterEnd(int start, int end) throws MediaException {
        int[] pos = { start };
        while (pos[0] < end) { // 요소를 훑다가 세그먼트 자식 id 가 나오면 거기서 끝난다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "WebM indexing was interrupted");
            }
            int before = pos[0];
            int id = readId(pos, end);
            if (id < 0) {
                return end;
            }
            for (int s : SEGMENT_CHILDREN) { // 세그먼트 자식 id 와 비교한다
                if (id == s) {
                    return before;
                }
            }
            long size = readSize(pos, end);
            if (size < 0) {
                return end;
            }
            if (pos[0] + size > end) {
                return end;
            }
            pos[0] += (int) size;
        }
        return end;
    }

    /**
     * Reads the EBML header for the document type.
     * <p>
     * EBML 헤더에서 문서 종류를 읽는다.
     *
     * @param start start / 시작
     * @param end   end / 끝
     * @throws MediaException if the document type is too long / 문서 종류가 너무 길면
     */
    private void readHeader(int start, int end) throws MediaException {
        int[] pos = { start };
        while (pos[0] < end) { // 하위 요소를 읽어 DocType 을 찾는다
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                return;
            }
            if (id == 0x4282) {
                docType = identifier(pos[0], (int) size);
            }
            pos[0] += (int) size;
        }
    }

    /**
     * Reads the segment info (time scale).
     * <p>
     * 세그먼트 정보(시간 단위)를 읽는다.
     *
     * @param start start / 시작
     * @param end   end / 끝
     */
    private void readInfo(int start, int end) {
        int[] pos = { start };
        while (pos[0] < end) { // 하위 요소에서 TimecodeScale 을 찾는다
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                return;
            }
            if (id == 0x2AD7B1) {
                long v = uint(pos[0], (int) size);
                if (v > 0) {
                    timecodeScale = v;
                }
            }
            pos[0] += (int) size;
        }
    }

    /**
     * Reads the track list.
     * <p>
     * 트랙 목록을 읽는다.
     *
     * @param start start / 시작
     * @param end   end / 끝
     * @throws MediaException if the track metadata is too large / 트랙 메타데이터가 너무 크면
     */
    private void readTracks(int start, int end) throws MediaException {
        int[] pos = { start };
        while (pos[0] < end) { // 트랙 항목을 하나씩 읽는다
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                return;
            }
            if (id == 0xAE) {
                tracks.add(readTrack(pos[0], pos[0] + (int) size));
            }
            pos[0] += (int) size;
        }
    }

    /**
     * Reads one track entry.
     * <p>
     * 트랙 항목 하나를 읽는다.
     *
     * @param start start / 시작
     * @param end   end / 끝
     * @return the track / 트랙
     * @throws MediaException if the track metadata is too large / 트랙 메타데이터가 너무 크면
     */
    private Track readTrack(int start, int end) throws MediaException {
        Track t = new Track();
        int[] pos = { start };
        while (pos[0] < end) { // 하위 요소를 읽어 트랙 속성을 채운다
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                return t;
            }
            int p = pos[0];
            int s = (int) size;
            if (id == 0xD7) {
                t.number = (int) uint(p, s);
            } else if (id == 0x83) {
                t.type = (int) uint(p, s);
            } else if (id == 0x86) {
                t.codecId = identifier(p, s);
            } else if (id == 0x63A2) {
                if (s > MAX_CODEC_PRIVATE_BYTES) {
                    throw new MediaException(MediaException.Kind.TOO_LARGE, "WebM codec initialization data is too large");
                }
                t.codecPrivate = Bytes.copy(d, p, s);
            } else if (id == 0x56AA) {
                t.codecDelay = uint(p, s);
            } else if (id == 0x56BB) {
                t.seekPreRoll = uint(p, s);
            } else if (id == 0xE1) {
                readAudio(p, p + s, t);
            }
            pos[0] += s;
        }
        return t;
    }

    /**
     * Reads a bounded ASCII identifier. / 제한된 길이의 ASCII 식별자를 읽는다.
     * @param at identifier offset / 식별자 위치
     * @param count its length / 식별자 길이
     * @return the identifier / 식별자
     * @throws MediaException if the identifier is too large / 식별자가 너무 크면
     */
    private String identifier(int at, int count) throws MediaException {
        if (count > MAX_IDENTIFIER_BYTES) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "WebM identifier is too large");
        }
        return Bytes.ascii(d, at, count);
    }

    /**
     * Reads the audio settings of a track.
     * <p>
     * 트랙의 오디오 설정을 읽는다.
     *
     * @param start start / 시작
     * @param end   end / 끝
     * @param t     track / 트랙
     */
    private void readAudio(int start, int end, Track t) {
        int[] pos = { start };
        while (pos[0] < end) { // 채널 수와 샘플링 주파수를 찾는다
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                return;
            }
            if (id == 0x9F) {
                t.channels = (int) uint(pos[0], (int) size);
            } else if (id == 0xB5) {
                t.sampleRate = real(pos[0], (int) size);
            }
            pos[0] += (int) size;
        }
    }

    /**
     * Reads a cluster: its time code and its blocks.
     * <p>
     * 클러스터를 읽는다: 시간 코드와 블록.
     *
     * @param start start / 시작
     * @param end   end / 끝
     * @throws MediaException on damaged data / 손상된 데이터일 때
     */
    private void readCluster(int start, int end) throws MediaException {
        clusterTime = 0;
        int[] pos = { start };
        while (pos[0] < end) { // 클러스터의 하위 요소를 읽는다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "WebM indexing was interrupted");
            }
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                return;
            }
            int p = pos[0];
            int s = (int) size;
            if (id == 0xE7) {
                clusterTime = uint(p, s);
            } else if (id == 0xA3) {
                readBlock(p, p + s, 0);
            } else if (id == 0xA0) {
                readGroup(p, p + s);
            }
            pos[0] += s;
        }
    }

    /**
     * Reads a block group: a block and its duration.
     * <p>
     * 블록 그룹을 읽는다: 블록과 그 길이.
     *
     * @param start start / 시작
     * @param end   end / 끝
     * @throws MediaException on damaged data / 손상된 데이터일 때
     */
    private void readGroup(int start, int end) throws MediaException {
        int[] pos = { start };
        long duration = 0;
        int blockStart = -1;
        int blockEnd = -1;
        while (pos[0] < end) { // 블록과 BlockDuration 을 찾는다
            int id = readId(pos, end);
            long size = readSize(pos, end);
            if (id < 0 || size < 0 || pos[0] + size > end) {
                break;
            }
            if (id == 0xA1) {
                blockStart = pos[0];
                blockEnd = pos[0] + (int) size;
            } else if (id == 0x9B) {
                duration = uint(pos[0], (int) size);
            } else if (id == 0x75A2 && size >= 1 && size <= 8) {
                // DiscardPadding 은 부호 있는 정수이므로 상위 비트를 확장한다.
                long raw = uint(pos[0], (int) size);
                int shift = 64 - 8 * (int) size;
                pendingDiscard = (raw << shift) >> shift;
            }
            pos[0] += (int) size;
        }
        if (blockStart >= 0) {
            readBlock(blockStart, blockEnd, duration);
        }
        pendingDiscard = 0;
    }

    /**
     * Reads a block header and its laced frames.
     * <p>
     * 블록 헤더와 레이싱된 프레임을 읽는다.
     *
     * @param start    start of the block body / 블록 본문 시작
     * @param end      end / 끝
     * @param duration block duration in ticks, or 0 / 블록 길이(틱). 없으면 0
     * @throws MediaException on damaged data / 손상된 데이터일 때
     */
    private void readBlock(int start, int end, long duration) throws MediaException {
        int[] pos = { start };
        long track = readSize(pos, end);
        if (track < 0 || pos[0] > end - 3) {
            return;
        }
        int rel = (short) Bytes.be16(d, pos[0]);
        int flags = d.get(pos[0] + 2);
        pos[0] += 3;
        Track t = null;
        for (Track x : tracks) { // 트랙 번호로 대상 트랙을 찾는다
            if (x.number == track) {
                t = x;
            }
        }
        if (t == null || t.type != 2) {
            return;
        }
        if (pendingDiscard != 0) {
            t.discardPadding = pendingDiscard;
        }
        long time = (clusterTime + rel) * timecodeScale;
        int lacing = (flags >> 1) & 3;
        if (lacing == 0) {
            t.add(pos[0], end - pos[0], time, duration * timecodeScale);
            return;
        }
        if (pos[0] >= end) {
            return;
        }
        int n = d.get(pos[0]++) + 1;
        int[] sizes = new int[n];
        int total = end - pos[0];
        if (lacing == 2) {
            if (total % n != 0) {
                throw new MediaException(MediaException.Kind.CORRUPT, "uneven fixed lacing");
            }
            int each = total / n;
            for (int i = 0; i < n; i++) { // 고정 크기 레이싱
                sizes[i] = each;
            }
        } else if (lacing == 1) {
            long used = 0;
            for (int i = 0; i < n - 1; i++) { // Xiph 레이싱: 255 가 이어지는 동안 더한다
                long s = 0;
                int b;
                do { // 255 가 이어지는 동안 크기를 더한다
                    if (pos[0] >= end) {
                        return;
                    }
                    b = d.get(pos[0]++);
                    s += b;
                } while (b == 255);
                if (s > end - pos[0]) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid Xiph laced frame size");
                }
                sizes[i] = (int) s;
                used += s;
            }
            if (used > end - pos[0]) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Xiph lacing total");
            }
            sizes[n - 1] = (int) (end - pos[0] - used);
        } else {
            long first = readSize(pos, end);
            if (first < 0) {
                return;
            }
            if (first > end - pos[0]) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid first EBML laced frame size");
            }
            sizes[0] = (int) first;
            long used = first;
            for (int i = 1; i < n - 1; i++) { // EBML 레이싱: 앞 크기와의 부호 있는 차이
                int p0 = pos[0];
                long raw = readSize(pos, end);
                if (raw < 0) {
                    return;
                }
                int len = pos[0] - p0;
                long bias = (1L << (7 * len - 1)) - 1;
                long size = sizes[i - 1] + (raw - bias);
                if (size < 0 || size > end - pos[0]) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "invalid EBML laced frame size");
                }
                sizes[i] = (int) size;
                used += sizes[i];
            }
            if (used > end - pos[0]) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid EBML lacing total");
            }
            sizes[n - 1] = (int) (end - pos[0] - used);
        }
        long frameDuration = duration > 0 ? duration * timecodeScale / n : 0;
        int p = pos[0];
        for (int i = 0; i < n; i++) { // 프레임을 하나씩 추가한다
            if (sizes[i] < 0 || sizes[i] > end - p) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid laced frame size");
            }
            t.add(p, sizes[i], time + frameDuration * i, frameDuration);
            p += sizes[i];
        }
    }
}
