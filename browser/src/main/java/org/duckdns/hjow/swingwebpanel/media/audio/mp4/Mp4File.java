package org.duckdns.hjow.swingwebpanel.media.audio.mp4;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * A demultiplexer of the ISO base media file format (MP4, M4A, ISO/IEC 14496-12): it reads the box structure, the tracks with their sample
 * tables (stts, stsc, stsz, stco/co64), the sample entries with the elementary stream descriptor of AAC (esds) and the Opus configuration (dOps),
 * the edit list and fragmented files (moof, traf, tfhd, trun). All sizes are checked against the data before arrays are allocated.
 * <p>
 * ISO 기본 미디어 파일 형식(MP4, M4A, ISO/IEC 14496-12)의 역다중화기: 박스 구조, 샘플 표(stts, stsc, stsz, stco/co64)가 있는 트랙, AAC 의 기본 스트림
 * 서술자(esds)와 Opus 구성(dOps)이 있는 샘플 엔트리, 편집 목록, 조각화된 파일(moof, traf, tfhd, trun)을 읽는다. 모든 크기는 배열을 할당하기 전에
 * 데이터와 대조한다.
 */
public final class Mp4File {

    /** Largest number of samples accepted in one track. / 트랙 하나에서 받아들이는 최대 샘플 수. */
    private static final int MAX_SAMPLES = 8 * 1000 * 1000;

    /** Largest retained codec initialization record. / 보관할 코덱 초기화 레코드의 최대 크기. */
    private static final int MAX_CODEC_CONFIG = 64 * 1024;

    /** The data. / 데이터. */
    private final ByteSource d;
    /** Tracks found. / 찾은 트랙. */
    private final List<Mp4Track> tracks = new ArrayList<Mp4Track>();
    /** Movie time scale. / 영상 시간 단위. */
    private int movieTimescale = 1;
    /** Defaults of fragmented tracks from trex: [track id] = {description index, duration, size, flags}. / trex 의 조각 트랙 기본값. */
    private final java.util.Map<Integer, int[]> trex = new java.util.HashMap<Integer, int[]>();
    /** Chunk offsets read for the track under construction. / 구성 중인 트랙의 청크 위치. */
    private long[] chunkOffsets = new long[0];
    /** Samples per chunk runs [first chunk, samples per chunk]. / 청크당 샘플 수 구간. */
    private int[] stscFirst = new int[0];
    /** Samples per chunk of each run. / 구간별 청크당 샘플 수. */
    private int[] stscCount = new int[0];

    /**
     * Private constructor. / 비공개 생성자.
     *
     * @param d data / 데이터
     */
    private Mp4File(ByteSource d) {
        this.d = d;
    }

    /**
     * Tells whether the data looks like an ISO base media file (an ftyp box, or a moov/mdat box at the start).
     * <p>
     * 데이터가 ISO 기본 미디어 파일처럼 보이는지(ftyp 박스, 또는 처음의 moov/mdat 박스) 알려 준다.
     *
     * @param d data / 데이터
     * @return true if it looks like MP4 / MP4 처럼 보이면 true
     */
    public static boolean looksLikeMp4(byte[] d) {
        return d != null && looksLikeMp4Source(new ArrayByteSource(d));
    }

    /**
     * Checks the first box of a seekable ISO base media file. / 탐색 가능한 ISO 기본 미디어 파일의 첫 박스를 확인한다.
     * @param d the encoded input / 인코딩 입력
     * @return true for a plausible MP4 header / MP4 헤더로 보이면 true
     */
    public static boolean looksLikeMp4Source(ByteSource d) {
        if (d.length() < 12) {
            return false;
        }
        String t = type(d, 4);
        if ("ftyp".equals(t)) {
            return true;
        }
        long size = u32(d, 0);
        return ("moov".equals(t) || "mdat".equals(t) || "free".equals(t) || "skip".equals(t)) && size >= 8 && size <= d.length();
    }

    /**
     * Parses a file.
     * <p>
     * 파일을 해석한다.
     *
     * @param d data / 데이터
     * @return the parsed file / 해석된 파일
     * @throws MediaException CORRUPT if the box structure is damaged / 박스 구조가 손상되었으면 CORRUPT
     */
    public static Mp4File parse(byte[] d) throws MediaException {
        return parseSource(new ArrayByteSource(d));
    }

    /**
     * Parses box and sample tables; the caller keeps ownership of the input. / 박스와 샘플 표를 해석한다. 입력은 호출자가 소유한다.
     * @param d the encoded input / 인코딩 입력
     * @return the parsed metadata / 해석된 메타데이터
     * @throws MediaException if the structure or input is invalid / 구조나 입력이 올바르지 않으면
     */
    public static Mp4File parseSource(ByteSource d) throws MediaException {
        if (d.length() > Integer.MAX_VALUE) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "MP4 data exceeds the container-offset limit");
        }
        Mp4File parsed = new Mp4File(d);
        try {
            parsed.boxes(0, (int) d.length(), 0, null);
            return parsed;
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        } catch (IndexOutOfBoundsException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "MP4 data is truncated", e);
        }
    }

    /**
     * Returns the tracks (sample tables completed). / 트랙을 반환한다(샘플 표 완성).
     *
     * @return tracks / 트랙
     */
    public List<Mp4Track> getTracks() {
        return tracks;
    }

    /**
     * Returns the first audio track, or null.
     * <p>
     * 첫 오디오 트랙을 반환하며 없으면 null 이다.
     *
     * @return track or null / 트랙 또는 null
     */
    public Mp4Track firstAudioTrack() {
        for (Mp4Track t : tracks) { // 핸들러가 soun 인 첫 트랙을 찾는다
            if ("soun".equals(t.handler) && t.sizes.length > 0) {
                return t;
            }
        }
        return null;
    }

    /**
     * Reads a 32-bit big-endian unsigned value.
     * <p>
     * 빅 엔디언 32비트 부호 없는 값을 읽는다.
     *
     * @param d data / 데이터
     * @param p position / 위치
     * @return value / 값
     */
    private static long u32(ByteSource d, int p) {
        return Bytes.be32(d, p);
    }

    /**
     * Reads a four character code.
     * <p>
     * 4글자 코드를 읽는다.
     *
     * @param d data / 데이터
     * @param p position / 위치
     * @return the code / 코드
     */
    private static String type(ByteSource d, int p) {
        char[] c = new char[4];
        for (int i = 0; i < 4; i++) { // 바이트를 문자로 바꾼다
            c[i] = (char) d.get(p + i);
        }
        return new String(c);
    }

    /**
     * Reads a 64-bit value that must fit in a long.
     * <p>
     * long 에 들어가야 하는 64비트 값을 읽는다.
     *
     * @param p position / 위치
     * @return value / 값
     */
    private long u64(int p) {
        return (u32(d, p) << 32) | u32(d, p + 4);
    }

    /**
     * Checks that a range lies inside a box.
     * <p>
     * 범위가 박스 안에 있는지 확인한다.
     *
     * @param p   start / 시작
     * @param len length / 길이
     * @param end box end / 박스 끝
     * @throws MediaException CORRUPT if outside / 밖이면 CORRUPT
     */
    private static void need(int p, int len, int end) throws MediaException {
        if (p < 0 || p > end - len) {
            throw new MediaException(MediaException.Kind.CORRUPT, "MP4 box is truncated");
        }
    }

    /**
     * Walks the boxes of a range, descending into container boxes.
     * <p>
     * 범위의 박스를 훑으며 컨테이너 박스 안으로 들어간다.
     *
     * @param start start of the range / 범위 시작
     * @param end   end of the range / 범위 끝
     * @param depth nesting depth / 중첩 깊이
     * @param ctx   the track under construction, or null / 구성 중인 트랙. 없으면 null
     * @throws MediaException on a damaged structure / 구조가 손상되었을 때
     */
    private void boxes(int start, int end, int depth, Mp4Track ctx) throws MediaException {
        if (depth > 12) {
            throw new MediaException(MediaException.Kind.CORRUPT, "MP4 boxes are nested too deeply");
        }
        int p = start;
        while (p <= end - 8) { // 박스를 하나씩 읽는다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "MP4 parsing was interrupted");
            }
            long size = u32(d, p);
            String t = type(d, p + 4);
            int header = 8;
            if (size == 1) {
                if (p > end - 16) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "MP4 extended box size is truncated");
                }
                size = u64(p + 8);
                header = 16;
            } else if (size == 0) {
                size = end - p;
            }
            if (size < header || size > end - p) {
                throw new MediaException(MediaException.Kind.CORRUPT, "MP4 box '" + t + "' has an invalid size");
            }
            int body = p + header;
            int boxEnd = (int) (p + size);
            if ("moov".equals(t) || "mdia".equals(t) || "minf".equals(t) || "stbl".equals(t) || "edts".equals(t) || "mvex".equals(t)) {
                boxes(body, boxEnd, depth + 1, ctx);
            } else if ("trak".equals(t)) {
                Mp4Track trk = new Mp4Track();
                trk.movieTimescale = movieTimescale;
                chunkOffsets = new long[0];
                stscFirst = new int[0];
                stscCount = new int[0];
                boxes(body, boxEnd, depth + 1, trk);
                finishTrack(trk);
                tracks.add(trk);
            } else if ("mvhd".equals(t)) {
                need(body, 24, boxEnd);
                int v = d.get(body);
                movieTimescale = (int) u32(d, body + (v == 1 ? 20 : 12));
            } else if ("tkhd".equals(t) && ctx != null) {
                need(body, 24, boxEnd);
                int v = d.get(body);
                ctx.id = (int) u32(d, body + (v == 1 ? 20 : 12));
            } else if ("mdhd".equals(t) && ctx != null) {
                need(body, 24, boxEnd);
                int v = d.get(body);
                if (v == 1) {
                    need(body, 36, boxEnd);
                    ctx.timescale = (int) u32(d, body + 20);
                    ctx.duration = u64(body + 24);
                } else {
                    ctx.timescale = (int) u32(d, body + 12);
                    ctx.duration = u32(d, body + 16);
                }
                if (ctx.timescale <= 0) {
                    ctx.timescale = 1;
                }
            } else if ("hdlr".equals(t) && ctx != null) {
                need(body, 12, boxEnd);
                ctx.handler = type(d, body + 8);
            } else if ("elst".equals(t) && ctx != null) {
                readElst(body, boxEnd, ctx);
            } else if ("stsd".equals(t) && ctx != null) {
                readStsd(body, boxEnd, ctx);
            } else if ("stts".equals(t) && ctx != null) {
                readStts(body, boxEnd, ctx);
            } else if ("stsc".equals(t) && ctx != null) {
                readStsc(body, boxEnd);
            } else if ("stsz".equals(t) && ctx != null) {
                readStsz(body, boxEnd, ctx);
            } else if (("stco".equals(t) || "co64".equals(t)) && ctx != null) {
                readStco(body, boxEnd, "co64".equals(t));
            } else if ("trex".equals(t)) {
                need(body, 24, boxEnd);
                trex.put(Integer.valueOf((int) u32(d, body + 4)), new int[] { (int) u32(d, body + 8), (int) u32(d, body + 12),
                        (int) u32(d, body + 16), (int) u32(d, body + 20) });
            } else if ("moof".equals(t)) {
                readMoof(p, body, boxEnd);
            }
            p = boxEnd;
        }
    }

    /**
     * Reads the edit list.
     * <p>
     * 편집 목록을 읽는다.
     *
     * @param p   body start / 본문 시작
     * @param end box end / 박스 끝
     * @param trk track / 트랙
     * @throws MediaException on truncation / 잘렸을 때
     */
    private void readElst(int p, int end, Mp4Track trk) throws MediaException {
        need(p, 8, end);
        int v = d.get(p);
        long count = u32(d, p + 4);
        int q = p + 8;
        for (long i = 0; i < count && q <= end - (v == 1 ? 20 : 12); i++) { // 편집 항목 중 비어 있지 않은 첫 항목을 찾는다
            long segment;
            long mediaTime;
            if (v == 1) {
                segment = u64(q);
                mediaTime = u64(q + 8);
                q += 20;
            } else {
                segment = u32(d, q);
                mediaTime = (int) u32(d, q + 4);
                q += 12;
            }
            if (mediaTime >= 0) {
                trk.editStart = mediaTime;
                trk.editDuration = segment;
                return;
            }
        }
    }

    /**
     * Reads the sample description box: format, channels, sampling frequency and codec configuration.
     * <p>
     * 샘플 설명 박스를 읽는다: 형식, 채널, 샘플링 주파수, 코덱 구성.
     *
     * @param p   body start / 본문 시작
     * @param end box end / 박스 끝
     * @param trk track / 트랙
     * @throws MediaException on a damaged structure / 구조가 손상되었을 때
     */
    private void readStsd(int p, int end, Mp4Track trk) throws MediaException {
        need(p, 16, end);
        long count = u32(d, p + 4);
        if (count < 1) {
            return;
        }
        int e = p + 8;
        long size = u32(d, e);
        if (size < 36 || e + size > end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid sample entry");
        }
        trk.format = type(d, e + 4);
        int entryEnd = (int) (e + size);
        if (!"soun".equals(trk.handler)) {
            return;
        }
        int q = e + 8 + 6 + 2;
        int version = Bytes.be16(d, q);
        trk.channels = Bytes.be16(d, q + 8);
        trk.sampleRate = (int) (u32(d, q + 16) >> 16);
        int entryHeader = 20 + (version == 1 ? 16 : version == 2 ? 36 : 0);
        if (q > entryEnd - entryHeader) {
            return;
        }
        int cp = q + entryHeader;
        while (cp <= entryEnd - 8) { // 샘플 엔트리 안의 하위 박스(esds, dOps ...)를 읽는다
            long bs = u32(d, cp);
            if (bs < 8 || cp + bs > entryEnd) {
                break;
            }
            String bt = type(d, cp + 4);
            if ("esds".equals(bt) && bs >= 12) {
                readEsds(cp + 12, (int) (cp + bs), trk);
            } else if ("dOps".equals(bt)) {
                trk.config = copy(cp + 8, (int) (cp + bs));
            } else if ("wave".equals(bt)) {
                int w = cp + 8;
                int we = (int) (cp + bs);
                while (w <= we - 8) { // QuickTime 의 wave 박스 안의 esds 를 찾는다
                    long ws = u32(d, w);
                    if (ws < 8 || w + ws > we) {
                        break;
                    }
                    if (ws >= 12 && "esds".equals(type(d, w + 4))) {
                        readEsds(w + 12, (int) (w + ws), trk);
                    }
                    w += (int) ws;
                }
            }
            cp += (int) bs;
        }
    }

    /**
     * Copies an initialization record after checking its size.
     * <p>
     * 초기화 레코드의 크기를 검사한 뒤 복사한다.
     *
     * @param from start / 시작
     * @param to   end / 끝
     * @return the bytes / 바이트
     * @throws MediaException if the record is too large / 레코드가 너무 크면
     */
    private byte[] copy(int from, int to) throws MediaException {
        if (to - from > MAX_CODEC_CONFIG) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "MP4 codec configuration is too large");
        }
        return Bytes.copy(d, from, to - from);
    }

    /**
     * Reads a descriptor length (up to four bytes of seven bits).
     * <p>
     * 서술자 길이(7비트씩 최대 4바이트)를 읽는다.
     *
     * @param pos position holder / 위치 보관
     * @param end limit / 한계
     * @return the length / 길이
     */
    private int descLen(int[] pos, int end) {
        int len = 0;
        for (int i = 0; i < 4 && pos[0] < end; i++) { // 연속 비트가 있는 동안 7비트씩 모은다
            int b = d.get(pos[0]++);
            len = (len << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return len;
    }

    /**
     * Reads the elementary stream descriptor.
     * <p>
     * 기본 스트림 서술자를 읽는다.
     *
     * @param p   start of the descriptors / 서술자 시작
     * @param end end / 끝
     * @param trk track / 트랙
     * @throws MediaException if the record is too large / 레코드가 너무 크면
     */
    private void readEsds(int p, int end, Mp4Track trk) throws MediaException {
        int[] pos = { p };
        while (pos[0] <= end - 2) { // 서술자를 차례로 읽는다
            int tag = d.get(pos[0]++);
            int len = descLen(pos, end);
            int bodyStart = pos[0];
            // 잘린 서술자나 그 안의 고정 헤더가 다음 박스까지 넘어가면 무시한다.
            if (len > end - bodyStart || (tag == 0x03 && len < 3) || (tag == 0x04 && len < 13)) {
                return;
            }
            if (tag == 0x03) {
                int q = bodyStart + 2;
                int flags = d.get(q++);
                int descriptorEnd = bodyStart + len;
                int extra = (flags & 0x80) != 0 ? 2 : 0;
                if ((flags & 0x40) != 0) {
                    if (extra >= descriptorEnd - q) {
                        return;
                    }
                    extra += 1 + d.get(q + extra);
                }
                extra += (flags & 0x20) != 0 ? 2 : 0;
                if (extra > descriptorEnd - q) {
                    return;
                }
                pos[0] = q + extra;
            } else if (tag == 0x04) {
                trk.objectType = d.get(bodyStart);
                pos[0] = bodyStart + 13;
            } else if (tag == 0x05) {
                trk.config = copy(bodyStart, bodyStart + len);
                return;
            } else {
                pos[0] = bodyStart + len;
            }
        }
    }

    /**
     * Reads the time-to-sample table into the sample durations.
     * <p>
     * 시간-샘플 표를 샘플 길이로 읽는다.
     *
     * @param p   body / 본문
     * @param end end / 끝
     * @param trk track / 트랙
     * @throws MediaException on a damaged table / 표가 손상되었을 때
     */
    private void readStts(int p, int end, Mp4Track trk) throws MediaException {
        need(p, 8, end);
        long count = u32(d, p + 4);
        if (count * 8 > end - (p + 8)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "stts is truncated");
        }
        List<int[]> runs = new ArrayList<int[]>();
        long total = 0;
        int q = p + 8;
        for (long i = 0; i < count; i++) { // 같은 길이의 샘플 묶음을 모은다
            long n = u32(d, q);
            long delta = u32(d, q + 4);
            total += n;
            if (total > MAX_SAMPLES) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "too many samples");
            }
            runs.add(new int[] { (int) n, (int) delta });
            q += 8;
        }
        int[] dur = new int[(int) total];
        int k = 0;
        for (int[] r : runs) { // 묶음을 샘플별 길이로 펼친다
            for (int i = 0; i < r[0]; i++) {
                dur[k++] = r[1];
            }
        }
        trk.durations = dur;
    }

    /**
     * Reads the sample-to-chunk table.
     * <p>
     * 샘플-청크 표를 읽는다.
     *
     * @param p   body / 본문
     * @param end end / 끝
     * @throws MediaException on a damaged table / 표가 손상되었을 때
     */
    private void readStsc(int p, int end) throws MediaException {
        need(p, 8, end);
        long count = u32(d, p + 4);
        if (count * 12 > end - (p + 8)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "stsc is truncated");
        }
        stscFirst = new int[(int) count];
        stscCount = new int[(int) count];
        for (int i = 0; i < count; i++) { // 항목마다 첫 청크와 청크당 샘플 수를 읽는다
            stscFirst[i] = (int) u32(d, p + 8 + 12 * i);
            stscCount[i] = (int) u32(d, p + 12 + 12 * i);
        }
    }

    /**
     * Reads the sample sizes.
     * <p>
     * 샘플 크기를 읽는다.
     *
     * @param p   body / 본문
     * @param end end / 끝
     * @param trk track / 트랙
     * @throws MediaException on a damaged table / 표가 손상되었을 때
     */
    private void readStsz(int p, int end, Mp4Track trk) throws MediaException {
        need(p, 12, end);
        long fixed = u32(d, p + 4);
        long count = u32(d, p + 8);
        if (count > MAX_SAMPLES) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "too many samples");
        }
        if (fixed == 0 && count * 4 > end - (p + 12)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "stsz is truncated");
        }
        int[] sizes = new int[(int) count];
        if (fixed != 0) {
            java.util.Arrays.fill(sizes, (int) fixed);
        } else {
            for (int i = 0; i < count; i++) { // 샘플마다 크기를 읽는다
                sizes[i] = (int) u32(d, p + 12 + 4 * i);
            }
        }
        trk.sizes = sizes;
    }

    /**
     * Reads the chunk offsets.
     * <p>
     * 청크 위치를 읽는다.
     *
     * @param p    body / 본문
     * @param end  end / 끝
     * @param wide true for 64-bit offsets / 64비트 위치이면 true
     * @throws MediaException on a damaged table / 표가 손상되었을 때
     */
    private void readStco(int p, int end, boolean wide) throws MediaException {
        need(p, 8, end);
        long count = u32(d, p + 4);
        int w = wide ? 8 : 4;
        if (count * w > end - (p + 8)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "chunk offset table is truncated");
        }
        chunkOffsets = new long[(int) count];
        for (int i = 0; i < count; i++) { // 청크마다 위치를 읽는다
            chunkOffsets[i] = wide ? u64(p + 8 + 8 * i) : u32(d, p + 8 + 4 * i);
        }
    }

    /**
     * Combines the tables of a finished track into the sample offsets.
     * <p>
     * 끝난 트랙의 표를 합쳐 샘플 위치를 만든다.
     *
     * @param trk track / 트랙
     * @throws MediaException on inconsistent tables / 표가 서로 맞지 않을 때
     */
    private void finishTrack(Mp4Track trk) throws MediaException {
        int n = trk.sizes.length;
        trk.offsets = new long[n];
        if (n == 0) {
            return;
        }
        int sample = 0;
        for (int c = 0; c < chunkOffsets.length && sample < n; c++) { // 청크마다 그 청크의 샘플 수를 구해 위치를 계산한다
            int perChunk = 1;
            for (int r = 0; r < stscFirst.length; r++) {
                if (stscFirst[r] <= c + 1) {
                    perChunk = stscCount[r];
                }
            }
            long off = chunkOffsets[c];
            for (int i = 0; i < perChunk && sample < n; i++) { // 청크 안의 샘플마다 위치를 계산한다
                trk.offsets[sample] = off;
                off += trk.sizes[sample];
                sample++;
            }
        }
        if (sample < n) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the sample table is inconsistent");
        }
        if (trk.durations.length < n) {
            int[] dur = new int[n];
            System.arraycopy(trk.durations, 0, dur, 0, trk.durations.length);
            int last = trk.durations.length > 0 ? trk.durations[trk.durations.length - 1] : 0;
            for (int i = trk.durations.length; i < n; i++) { // 길이가 모자라면 마지막 값으로 채운다
                dur[i] = last;
            }
            trk.durations = dur;
        }
    }

    /**
     * Reads a movie fragment and appends its samples to the tracks.
     * <p>
     * 영상 조각을 읽어 샘플을 트랙에 덧붙인다.
     *
     * @param moofStart start of the moof box / moof 박스 시작
     * @param p         body start / 본문 시작
     * @param end       box end / 박스 끝
     * @throws MediaException on a damaged fragment / 조각이 손상되었을 때
     */
    private void readMoof(int moofStart, int p, int end) throws MediaException {
        int q = p;
        while (q <= end - 8) { // moof 안의 traf 를 찾는다
            long size = u32(d, q);
            if (size < 8 || q + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid fragment box");
            }
            if ("traf".equals(type(d, q + 4))) {
                readTraf(moofStart, q + 8, (int) (q + size));
            }
            q += (int) size;
        }
    }

    /**
     * Reads a track fragment.
     * <p>
     * 트랙 조각을 읽는다.
     *
     * @param moofStart start of the moof box / moof 박스 시작
     * @param p         body start / 본문 시작
     * @param end       box end / 박스 끝
     * @throws MediaException on a damaged fragment / 조각이 손상되었을 때
     */
    private void readTraf(int moofStart, int p, int end) throws MediaException {
        int trackId = 0;
        long baseOffset = moofStart;
        int defDur = 0;
        int defSize = 0;
        int q = p;
        Mp4Track target = null;
        long dataCursor = -1;
        while (q <= end - 8) { // tfhd 와 trun 박스를 차례로 읽는다
            long size = u32(d, q);
            if (size < 8 || q + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid track fragment box");
            }
            String t = type(d, q + 4);
            int body = q + 8;
            int boxEnd = (int) (q + size);
            if ("tfhd".equals(t)) {
                need(body, 8, boxEnd);
                long flags = u32(d, body) & 0xFFFFFF;
                int optional = ((flags & 1) != 0 ? 8 : 0) + 4 * Integer.bitCount((int) (flags & 0x3A));
                need(body + 8, optional, boxEnd);
                trackId = (int) u32(d, body + 4);
                int r = body + 8;
                if ((flags & 1) != 0) {
                    baseOffset = u64(r);
                    r += 8;
                }
                if ((flags & 2) != 0) {
                    r += 4;
                }
                int[] tx = trex.get(Integer.valueOf(trackId));
                defDur = tx != null ? tx[1] : 0;
                defSize = tx != null ? tx[2] : 0;
                if ((flags & 8) != 0) {
                    defDur = (int) u32(d, r);
                    r += 4;
                }
                if ((flags & 0x10) != 0) {
                    defSize = (int) u32(d, r);
                }
                for (Mp4Track tr : tracks) { // 트랙 id 로 대상 트랙을 찾는다
                    if (tr.id == trackId) {
                        target = tr;
                    }
                }
            } else if ("trun".equals(t) && target != null) {
                dataCursor = readTrun(body, boxEnd, target, baseOffset, dataCursor, defDur, defSize);
            }
            q += (int) size;
        }
    }

    /**
     * Reads a track run.
     * <p>
     * 트랙 런을 읽는다.
     *
     * @param p          body / 본문
     * @param end        box end / 박스 끝
     * @param trk        track / 트랙
     * @param baseOffset base of the data offsets / 데이터 위치의 기준
     * @param cursor     file offset after the previous run, or -1 / 앞 런 뒤의 파일 위치. 없으면 -1
     * @param defDur     default sample duration / 기본 샘플 길이
     * @param defSize    default sample size / 기본 샘플 크기
     * @return the file offset after this run / 이 런 뒤의 파일 위치
     * @throws MediaException on a damaged run / 런이 손상되었을 때
     */
    private long readTrun(int p, int end, Mp4Track trk, long baseOffset, long cursor, int defDur, int defSize) throws MediaException {
        need(p, 8, end);
        long flags = u32(d, p) & 0xFFFFFF;
        need(p + 8, 4 * Integer.bitCount((int) (flags & 5)), end);
        long count = u32(d, p + 4);
        int q = p + 8;
        long off = cursor;
        if ((flags & 1) != 0) {
            off = baseOffset + (int) u32(d, q);
            q += 4;
        } else if (off < 0) {
            off = baseOffset;
        }
        if ((flags & 4) != 0) {
            q += 4;
        }
        int per = ((flags & 0x100) != 0 ? 4 : 0) + ((flags & 0x200) != 0 ? 4 : 0) + ((flags & 0x400) != 0 ? 4 : 0) + ((flags & 0x800) != 0 ? 4 : 0);
        if (count > MAX_SAMPLES || count * per > end - q) {
            throw new MediaException(MediaException.Kind.CORRUPT, "trun is truncated");
        }
        int old = trk.sizes.length;
        if (old + count > MAX_SAMPLES) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "too many samples");
        }
        int n = old + (int) count;
        trk.sizes = java.util.Arrays.copyOf(trk.sizes, n);
        trk.durations = java.util.Arrays.copyOf(trk.durations, n);
        trk.offsets = java.util.Arrays.copyOf(trk.offsets, n);
        for (int i = 0; i < count; i++) { // 샘플마다 길이와 크기를 읽는다
            int dur = defDur;
            int size = defSize;
            if ((flags & 0x100) != 0) {
                dur = (int) u32(d, q);
                q += 4;
            }
            if ((flags & 0x200) != 0) {
                size = (int) u32(d, q);
                q += 4;
            }
            if ((flags & 0x400) != 0) {
                q += 4;
            }
            if ((flags & 0x800) != 0) {
                q += 4;
            }
            trk.durations[old + i] = dur;
            trk.sizes[old + i] = size;
            trk.offsets[old + i] = off;
            off += size;
        }
        return off;
    }
}
