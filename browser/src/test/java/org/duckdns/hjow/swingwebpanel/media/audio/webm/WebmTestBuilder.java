package org.duckdns.hjow.swingwebpanel.media.audio.webm;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * A test-only builder of WebM / Matroska files written straight from the EBML and Matroska specifications: EBML header, segment with info and
 * one audio track, clusters with SimpleBlocks or BlockGroups, all three lacing methods, and unknown-size segments and clusters.
 * <p>
 * EBML 과 Matroska 규격에서 직접 쓴 시험 전용 WebM / Matroska 생성기: EBML 헤더, 정보와 오디오 트랙 하나가 있는 세그먼트, SimpleBlock 이나 BlockGroup 이
 * 있는 클러스터, 세 가지 레이싱 방식, 크기를 모르는 세그먼트와 클러스터.
 */
public final class WebmTestBuilder {

    /** Codec id of the track. / 트랙의 코덱 id. */
    public String codecId = "A_VORBIS";
    /** Codec private data. / 코덱 비공개 데이터. */
    public byte[] codecPrivate = new byte[0];
    /** Number of channels. / 채널 수. */
    public int channels = 2;
    /** Sampling frequency. / 샘플링 주파수. */
    public double rate = 44100.0;
    /** Codec delay in nanoseconds (0 = not written). / 코덱 지연(나노초, 0 이면 쓰지 않음). */
    public long codecDelay;
    /** Use unknown sizes for the segment and the clusters. / 세그먼트와 클러스터에 크기 모름을 쓴다. */
    public boolean unknownSizes;
    /** Lacing: 0 none, 1 Xiph, 2 fixed, 3 EBML. / 레이싱: 0 없음, 1 Xiph, 2 고정, 3 EBML. */
    public int lacing;
    /** Number of frames in a laced block. / 레이싱된 블록의 프레임 수. */
    public int lacedCount = 3;
    /** Write BlockGroups with durations instead of SimpleBlocks. / SimpleBlock 대신 길이가 있는 BlockGroup 을 쓴다. */
    public boolean blockGroups;

    /** DiscardPadding (nanoseconds) written into the block group of the last block when it is not 0 (needs <code>blockGroups</code>). / 0 이 아니면 마지막 블록의 블록 그룹에 쓰는 DiscardPadding(나노초). <code>blockGroups</code> 가 필요하다. */
    public long discardPaddingLast;
    /** Number of blocks per cluster. / 클러스터당 블록 수. */
    public int blocksPerCluster = 4;

    /**
     * Makes an EBML variable length integer for a size. / 크기를 EBML 가변 길이 정수로 만든다.
     *
     * @param v value / 값
     * @param len number of bytes / 바이트 수
     * @return bytes / 바이트
     */
    static byte[] vint(long v, int len) {
        byte[] b = new byte[len];
        for (int i = len - 1; i >= 0; i--) { // 낮은 바이트부터 채운다
            b[i] = (byte) v;
            v >>= 8;
        }
        b[0] |= (byte) (0x80 >> (len - 1));
        return b;
    }

    /**
     * Makes the smallest variable length integer of a value. / 값의 가장 짧은 가변 길이 정수를 만든다.
     *
     * @param v value / 값
     * @return bytes / 바이트
     */
    static byte[] vint(long v) {
        int len = 1;
        while (v >= (1L << (7 * len)) - 1) { // 값이 들어갈 만큼 길이를 늘린다
            len++;
        }
        return vint(v, len);
    }

    /**
     * Makes an element. / 요소를 만든다.
     *
     * @param id   element id / 요소 id
     * @param body body / 본문
     * @return the element / 요소
     */
    static byte[] el(int id, byte[]... body) {
        return elWithSize(id, -2, body);
    }

    /**
     * Makes an element with a given size encoding.
     * <p>
     * 크기 표기를 정해 요소를 만든다.
     *
     * @param id   element id / 요소 id
     * @param mode -2 for the real size, -1 for an unknown size / -2 이면 실제 크기, -1 이면 크기 모름
     * @param body body / 본문
     * @return the element / 요소
     */
    static byte[] elWithSize(int id, int mode, byte[]... body) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (byte[] x : body) { // 본문 조각을 이어 붙인다
            b.write(x, 0, x.length);
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        int idLen = id > 0xFFFFFF ? 4 : (id > 0xFFFF ? 3 : (id > 0xFF ? 2 : 1));
        for (int i = idLen - 1; i >= 0; i--) { // id 바이트
            o.write(id >> (8 * i));
        }
        byte[] size = mode == -1 ? new byte[] { 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF } : vint(b.size());
        o.write(size, 0, size.length);
        byte[] bb = b.toByteArray();
        o.write(bb, 0, bb.length);
        return o.toByteArray();
    }

    /**
     * Makes an unsigned integer element. / 부호 없는 정수 요소를 만든다.
     *
     * @param id element id / 요소 id
     * @param v  value / 값
     * @return the element / 요소
     */
    static byte[] uint(int id, long v) {
        int n = 1;
        while (n < 8 && (v >>> (8 * n)) != 0) { // 필요한 바이트 수
            n++;
        }
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) { // 빅 엔디언
            b[i] = (byte) (v >> (8 * (n - 1 - i)));
        }
        return el(id, b);
    }

    /**
     * Concatenates arrays. / 배열을 이어 붙인다.
     *
     * @param parts arrays / 배열
     * @return result / 결과
     */
    static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) { // 배열을 차례로 쓴다
            o.write(p, 0, p.length);
        }
        return o.toByteArray();
    }

    /**
     * Builds a block body (track number, time code, flags, lacing, frames).
     * <p>
     * 블록 본문(트랙 번호, 시간 코드, 플래그, 레이싱, 프레임)을 만든다.
     *
     * @param frames frames of the block / 블록의 프레임
     * @param rel    time code relative to the cluster / 클러스터에 대한 시간 코드
     * @return the block body / 블록 본문
     */
    private byte[] blockBody(List<byte[]> frames, int rel) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(0x81);
        o.write(rel >> 8);
        o.write(rel);
        int mode = frames.size() == 1 ? 0 : lacing;
        o.write(0x80 | (mode == 0 ? 0 : (mode == 1 ? 2 : (mode == 2 ? 4 : 6))));
        if (mode != 0) {
            o.write(frames.size() - 1);
            if (mode == 1) {
                for (int i = 0; i < frames.size() - 1; i++) { // Xiph 레이싱: 255 단위로 크기를 쓴다
                    int s = frames.get(i).length;
                    while (s >= 255) {
                        o.write(255);
                        s -= 255;
                    }
                    o.write(s);
                }
            } else if (mode == 3) {
                byte[] first = vint(frames.get(0).length);
                o.write(first, 0, first.length);
                for (int i = 1; i < frames.size() - 1; i++) { // EBML 레이싱: 앞 크기와의 차이를 부호 있는 가변 길이로 쓴다
                    long d = frames.get(i).length - frames.get(i - 1).length;
                    for (int len = 1; len <= 8; len++) {
                        long bias = (1L << (7 * len - 1)) - 1;
                        long raw = d + bias;
                        if (raw >= 0 && raw < (1L << (7 * len)) - 1) {
                            byte[] v = vint(raw, len);
                            o.write(v, 0, v.length);
                            break;
                        }
                    }
                }
            }
        }
        for (byte[] f : frames) { // 프레임을 이어 쓴다
            o.write(f, 0, f.length);
        }
        return o.toByteArray();
    }

    /**
     * Builds the file.
     * <p>
     * 파일을 만든다.
     *
     * @param frames the frames / 프레임
     * @param timesMs presentation time of every frame in milliseconds / 프레임마다 재생 시각(밀리초)
     * @return the file bytes / 파일 바이트
     */
    public byte[] build(List<byte[]> frames, long[] timesMs) {
        byte[] ebml = el(0x1A45DFA3, uint(0x4286, 1), uint(0x42F7, 1), uint(0x42F2, 4), uint(0x42F3, 8), el(0x4282, "webm".getBytes(java.nio.charset.Charset.forName("US-ASCII"))),
                uint(0x4287, 4), uint(0x4285, 2));
        byte[] info = el(0x1549A966, uint(0x2AD7B1, 1000000), el(0x4D80, "test".getBytes(java.nio.charset.Charset.forName("US-ASCII"))));
        byte[] audio = el(0xE1, uint(0x9F, channels), el(0xB5, doubleBytes(rate)));
        byte[] entry = el(0xAE, uint(0xD7, 1), uint(0x83, 2), el(0x86, codecId.getBytes(java.nio.charset.Charset.forName("US-ASCII"))), el(0x63A2, codecPrivate),
                codecDelay > 0 ? uint(0x56AA, codecDelay) : new byte[0], audio);
        byte[] tracks = el(0x1654AE6B, entry);
        ByteArrayOutputStream clusters = new ByteArrayOutputStream();
        int i = 0;
        while (i < frames.size()) { // 클러스터마다 블록을 담는다
            long base = timesMs[i];
            ByteArrayOutputStream cb = new ByteArrayOutputStream();
            byte[] tc = uint(0xE7, base);
            cb.write(tc, 0, tc.length);
            for (int b = 0; b < blocksPerCluster && i < frames.size(); b++) { // 클러스터 안의 블록을 하나씩 쓴다
                int count = lacing == 0 ? 1 : Math.min(lacedCount, frames.size() - i);
                List<byte[]> group = frames.subList(i, i + count);
                byte[] body = blockBody(group, (int) (timesMs[i] - base));
                byte[] blk;
                if (blockGroups) {
                    long dur = (i + count < frames.size() ? timesMs[i + count] : timesMs[i] + 20) - timesMs[i];
                    byte[] discard = new byte[0];
                    if (discardPaddingLast != 0 && i + count >= frames.size()) {
                        byte[] eight = new byte[8];
                        for (int k = 0; k < 8; k++) { // DiscardPadding 은 8바이트 빅 엔디언 부호 있는 정수로 쓴다
                            eight[k] = (byte) (discardPaddingLast >> (56 - 8 * k));
                        }
                        discard = el(0x75A2, eight);
                    }
                    blk = el(0xA0, el(0xA1, body), uint(0x9B, dur), discard);
                } else {
                    blk = el(0xA3, body);
                }
                cb.write(blk, 0, blk.length);
                i += count;
            }
            byte[] cl = elWithSize(0x1F43B675, unknownSizes ? -1 : -2, cb.toByteArray());
            clusters.write(cl, 0, cl.length);
        }
        byte[] seg = elWithSize(0x18538067, unknownSizes ? -1 : -2, info, tracks, clusters.toByteArray());
        return cat(ebml, seg);
    }

    /**
     * Returns the big-endian bytes of a double. / double 의 빅 엔디언 바이트를 반환한다.
     *
     * @param v value / 값
     * @return eight bytes / 8바이트
     */
    private static byte[] doubleBytes(double v) {
        long bits = Double.doubleToLongBits(v);
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) { // 빅 엔디언으로 쓴다
            b[i] = (byte) (bits >> (56 - 8 * i));
        }
        return b;
    }
}
