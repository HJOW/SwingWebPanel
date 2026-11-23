package org.duckdns.hjow.swingwebpanel.media.audio.testkit;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;

/**
 * Test-only FLAC encoder written from the FLAC format specification, independent of the decoder: constant, verbatim, fixed and
 * LPC subframes (with coefficients found by autocorrelation and Levinson-Durbin), Rice coding with partitions and escape codes, wasted
 * bits, all stereo decorrelation modes, fixed and variable block sizes, all block size and sample size codes, CRC-8 and CRC-16, a
 * seek table and the Ogg FLAC packet layout. Used to produce files whose decoded samples must equal the input exactly.
 * <p>
 * FLAC 형식 규격에서 디코더와 별개로 직접 작성한 시험 전용 FLAC 인코더: 상수, 그대로, 고정, LPC 서브프레임(자기상관과 Levinson-Durbin 으로 구한
 * 계수), 파티션과 이스케이프 코드가 있는 Rice 부호화, 버려진 비트, 모든 스테레오 비상관 방식, 고정/가변 블록 크기, 모든 블록 크기/샘플 크기 코드,
 * CRC-8 과 CRC-16, 탐색 표, Ogg FLAC 패킷 배치. 디코딩한 샘플이 입력과 정확히 같아야 하는 파일을 만드는 데 쓴다.
 */
public final class FlacWriter {

    /**
     * Encoder options.
     * <p>
     * 인코더 옵션.
     */
    public static final class Options {
        /**
         * Block size in samples (the sizes list overrides it).
         * <p>
         * 블록 크기(샘플 수). sizes 목록이 있으면 그것이 우선한다.
         */
        public int blockSize = 1152;

        /**
         * Explicit block sizes of the frames for variable block size streams, or null.
         * <p>
         * 가변 블록 크기 스트림의 프레임별 블록 크기. 없으면 null.
         */
        public int[] sizes;

        /**
         * Bits per sample.
         * <p>
         * 샘플당 비트 수.
         */
        public int bps = 16;

        /**
         * Sample rate.
         * <p>
         * 샘플링 주파수.
         */
        public int rate = 44100;

        /**
         * Subframe strategy: "auto", "verbatim", "constant", "fixed0".."fixed4", "lpc2".."lpc32".
         * <p>
         * 서브프레임 전략: "auto", "verbatim", "constant", "fixed0".."fixed4", "lpc2".."lpc32".
         */
        public String strategy = "auto";

        /**
         * Stereo mode: 0 independent, 1 left/side, 2 side/right, 3 mid/side.
         * <p>
         * 스테레오 방식: 0 독립, 1 left/side, 2 side/right, 3 mid/side.
         */
        public int stereo = 0;

        /**
         * Rice partition order.
         * <p>
         * Rice 파티션 차수.
         */
        public int partitionOrder = 2;

        /**
         * Use the 5-bit Rice parameter coding.
         * <p>
         * 5비트 Rice 매개변수 부호화를 쓴다.
         */
        public boolean riceMethod1;

        /**
         * Allow escape codes when they are cheaper.
         * <p>
         * 더 싸면 이스케이프 코드를 쓴다.
         */
        public boolean escape;

        /**
         * Number of samples to claim in STREAMINFO (-1 for the real number, 0 for unknown).
         * <p>
         * STREAMINFO 에 적을 전체 샘플 수(-1 이면 실제 수, 0 이면 모름).
         */
        public long claimedTotal = -1;

        /**
         * Write a seek table with a point every this many frames (0 for none).
         * <p>
         * 이 프레임 수마다 탐색 지점을 둔 탐색 표를 쓴다(0 이면 없음).
         */
        public int seekEvery;

        /**
         * Write a stream header without the number of samples in frame headers (code 0 for sample rate and size).
         * <p>
         * 프레임 헤더의 샘플링 주파수와 샘플 크기를 STREAMINFO 에서 가져오게(코드 0) 쓴다.
         */
        public boolean fromStreamInfo;
    }

    /**
     * The encoded stream in pieces.
     * <p>
     * 조각으로 나뉜 인코딩 결과.
     */
    public static final class Result {
        /**
         * STREAMINFO block body (34 bytes).
         * <p>
         * STREAMINFO 블록 본문(34바이트).
         */
        public byte[] streamInfo;

        /**
         * The frames.
         * <p>
         * 프레임들.
         */
        public byte[][] frames;

        /**
         * Sample number of the first sample of every frame.
         * <p>
         * 프레임마다의 첫 샘플 번호.
         */
        public long[] frameStart;

        /**
         * Block sizes of the frames.
         * <p>
         * 프레임의 블록 크기.
         */
        public int[] frameSize;

        /**
         * The seek table block body or null.
         * <p>
         * 탐색 표 블록 본문. 없으면 null.
         */
        public byte[] seekTable;

        /**
         * The native FLAC file.
         * <p>
         * 네이티브 FLAC 파일.
         */
        public byte[] file;

        /**
         * Offset of the first frame in the file.
         * <p>
         * 파일에서 첫 프레임의 위치.
         */
        public int firstFrame;
    }

    /**
     * MSB-first bit writer.
     * <p>
     * 최상위 비트부터 쓰는 비트 쓰기 객체.
     */
    private static final class Bits {
        /**
         * Bytes written.
         * <p>
         * 쓴 바이트.
         */
        private final Out out = new Out();

        /**
         * Bits waiting for a full byte.
         * <p>
         * 한 바이트가 찰 때까지 대기 중인 비트.
         */
        private long acc;

        /**
         * Number of waiting bits.
         * <p>
         * 대기 중인 비트 수.
         */
        private int n;

        /**
         * Writes up to 63 bits.
         * <p>
         * 최대 63비트를 쓴다.
         *
         * @param v    value (low bits are written) / 값(하위 비트를 쓴다)
         * @param bits number of bits / 비트 수
         */
        void put(long v, int bits) {
            // 비트를 하나씩 대기열에 넣고 8개가 차면 바이트로 내보낸다.
            for (int i = bits - 1; i >= 0; i--) {
                acc = (acc << 1) | ((v >> i) & 1);
                n++;
                if (n == 8) {
                    out.u8((int) acc);
                    acc = 0;
                    n = 0;
                }
            }
        }

        /**
         * Writes a two's complement number.
         * <p>
         * 2의 보수 수를 쓴다.
         *
         * @param v    value / 값
         * @param bits number of bits / 비트 수
         */
        void putSigned(long v, int bits) {
            put(v & ((1L << bits) - 1), bits);
        }

        /**
         * Writes zeros and a one bit.
         * <p>
         * 0 을 여러 개 쓰고 1 비트를 쓴다.
         *
         * @param zeros number of zeros / 0 의 개수
         */
        void unary(long zeros) {
            // 0 을 하나씩 쓴 다음 1 을 쓴다.
            for (long i = 0; i < zeros; i++) {
                put(0, 1);
            }
            put(1, 1);
        }

        /**
         * Pads with zero bits to a byte boundary.
         * <p>
         * 바이트 경계까지 0 비트로 채운다.
         */
        void align() {
            // 바이트 경계가 될 때까지 0 비트를 쓴다.
            while (n != 0) {
                put(0, 1);
            }
        }

        /**
         * Returns the bytes written so far (must be aligned).
         * <p>
         * 지금까지 쓴 바이트를 반환한다(정렬되어 있어야 한다).
         *
         * @return bytes / 바이트
         */
        byte[] bytes() {
            return out.toByteArray();
        }

        /**
         * Number of bits written so far.
         * <p>
         * 지금까지 쓴 비트 수.
         *
         * @return bits / 비트 수
         */
        long length() {
            return (long) out.size() * 8 + n;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FlacWriter() {
    }

    /**
     * Bitwise CRC with a polynomial given without its top bit.
     * <p>
     * 최상위 비트를 뺀 다항식으로 계산하는 비트 단위 CRC.
     *
     * @param d    data / 데이터
     * @param poly polynomial / 다항식
     * @param bits CRC width (8 or 16) / CRC 폭(8 또는 16)
     * @return the CRC / CRC
     */
    static int crc(byte[] d, int poly, int bits) {
        int c = 0;
        int top = 1 << (bits - 1);
        int mask = (1 << bits) - 1;
        // 바이트의 비트를 앞에서부터 한 비트씩 밀어 넣는다.
        for (int i = 0; i < d.length; i++) {
            for (int b = 7; b >= 0; b--) {
                int in = (d[i] >> b) & 1;
                int t = ((c & top) != 0 ? 1 : 0) ^ in;
                c = (c << 1) & mask;
                if (t != 0) {
                    c ^= poly;
                }
            }
        }
        return c;
    }

    /**
     * Encodes samples to FLAC.
     * <p>
     * 샘플을 FLAC 으로 인코딩한다.
     *
     * @param x   samples [channel][index], each within the range of the bit depth / 샘플 [채널][위치]. 모두 비트 깊이의 범위 안
     * @param opt options / 옵션
     * @return the result / 결과
     */
    public static Result encode(long[][] x, Options opt) {
        int ch = x.length;
        int total = x[0].length;
        List<byte[]> frames = new ArrayList<byte[]>();
        List<Long> starts = new ArrayList<Long>();
        List<Integer> sizes = new ArrayList<Integer>();
        int pos = 0;
        int frameNo = 0;
        int minFrame = Integer.MAX_VALUE;
        int maxFrame = 0;
        // 블록 크기만큼씩 잘라 프레임을 만든다.
        while (pos < total) {
            int bs = opt.sizes != null ? opt.sizes[Math.min(frameNo, opt.sizes.length - 1)] : opt.blockSize;
            bs = Math.min(bs, total - pos);
            long[][] blk = new long[ch][bs];
            // 채널별 블록을 복사한다.
            for (int c = 0; c < ch; c++) {
                System.arraycopy(x[c], pos, blk[c], 0, bs);
            }
            byte[] f = frame(blk, bs, opt, opt.sizes != null ? pos : frameNo);
            frames.add(f);
            starts.add(Long.valueOf(pos));
            sizes.add(Integer.valueOf(bs));
            minFrame = Math.min(minFrame, f.length);
            maxFrame = Math.max(maxFrame, f.length);
            pos += bs;
            frameNo++;
        }
        Result r = new Result();
        r.frames = frames.toArray(new byte[0][]);
        r.frameStart = new long[frames.size()];
        r.frameSize = new int[frames.size()];
        int maxBs = 0;
        int minBs = Integer.MAX_VALUE;
        // 프레임 정보를 배열에 옮기고 최소/최대 블록 크기를 구한다.
        for (int i = 0; i < r.frameStart.length; i++) {
            r.frameStart[i] = starts.get(i).longValue();
            r.frameSize[i] = sizes.get(i).intValue();
            maxBs = Math.max(maxBs, r.frameSize[i]);
            minBs = Math.min(minBs, r.frameSize[i]);
        }
        long claimed = opt.claimedTotal < 0 ? total : opt.claimedTotal;
        Out si = new Out();
        si.be16(minBs == Integer.MAX_VALUE ? 4096 : minBs).be16(maxBs == 0 ? 4096 : maxBs);
        si.u8(minFrame == Integer.MAX_VALUE ? 0 : minFrame >> 16).u8(minFrame == Integer.MAX_VALUE ? 0 : minFrame >> 8).u8(minFrame == Integer.MAX_VALUE ? 0 : minFrame);
        si.u8(maxFrame >> 16).u8(maxFrame >> 8).u8(maxFrame);
        long packed = ((long) opt.rate << 44) | ((long) (ch - 1) << 41) | ((long) (opt.bps - 1) << 36) | (claimed & 0xFFFFFFFFFL);
        si.be32(packed >>> 32).be32(packed & 0xFFFFFFFFL);
        si.bytes(new byte[16]);
        r.streamInfo = si.toByteArray();
        if (opt.seekEvery > 0) {
            Out st = new Out();
            int offset = 0;
            // 지정한 프레임 수마다 탐색 지점(샘플 번호, 위치, 블록 크기)을 만든다.
            for (int i = 0; i < r.frames.length; i++) {
                if (i % opt.seekEvery == 0) {
                    st.be32(r.frameStart[i] >>> 32).be32(r.frameStart[i] & 0xFFFFFFFFL).be32((long) offset >>> 32).be32((long) offset & 0xFFFFFFFFL).be16(r.frameSize[i]);
                }
                offset += r.frames[i].length;
            }
            st.be32(0xFFFFFFFFL).be32(0xFFFFFFFFL).be32(0).be32(0).be16(0);
            r.seekTable = st.toByteArray();
        }
        Out file = new Out();
        file.ascii("fLaC");
        boolean lastIsInfo = r.seekTable == null;
        file.u8(lastIsInfo ? 0x80 : 0).be24(34).bytes(r.streamInfo);
        if (r.seekTable != null) {
            file.u8(0x80 | 3).be24(r.seekTable.length).bytes(r.seekTable);
        }
        r.firstFrame = file.size();
        // 프레임을 차례로 붙인다.
        for (byte[] f : r.frames) {
            file.bytes(f);
        }
        r.file = file.toByteArray();
        return r;
    }

    /**
     * Encodes one frame.
     * <p>
     * 프레임 하나를 인코딩한다.
     *
     * @param blk    samples of the frame / 프레임의 샘플
     * @param bs     block size / 블록 크기
     * @param opt    options / 옵션
     * @param number frame number (fixed block size) or first sample number (variable) / 프레임 번호(고정 블록 크기) 또는 첫 샘플 번호(가변)
     * @return the frame bytes / 프레임 바이트
     */
    private static byte[] frame(long[][] blk, int bs, Options opt, long number) {
        int ch = blk.length;
        int mode = ch == 2 ? opt.stereo : 0;
        long[][] sub = new long[ch][];
        int[] width = new int[ch];
        // 채널마다 서브프레임에 넣을 신호와 비트 폭을 정한다(스테레오 방식에 따라 side 채널이 생긴다).
        for (int c = 0; c < ch; c++) {
            sub[c] = blk[c];
            width[c] = opt.bps;
        }
        if (mode != 0) {
            long[] l = blk[0];
            long[] r = blk[1];
            long[] side = new long[bs];
            long[] mid = new long[bs];
            // 채널마다 side/mid 신호를 만든다.
            for (int i = 0; i < bs; i++) {
                side[i] = l[i] - r[i];
                mid[i] = (l[i] + r[i]) >> 1;
            }
            if (mode == 1) {
                sub[1] = side;
                width[1]++;
            } else if (mode == 2) {
                sub[0] = side;
                width[0]++;
            } else {
                sub[0] = mid;
                sub[1] = side;
                width[1]++;
            }
        }
        Bits h = new Bits();
        h.put(0x3FFE, 14);
        h.put(0, 1);
        boolean variable = opt.sizes != null;
        h.put(variable ? 1 : 0, 1);
        int bsCode;
        int[] std = { 0, 192, 0, 0, 0, 0, 0, 0, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768 };
        bsCode = bs == 192 ? 1 : bs == 576 ? 2 : bs == 1152 ? 3 : bs == 2304 ? 4 : bs == 4608 ? 5 : 0;
        // 256 부터 32768 까지의 표준 블록 크기 코드를 찾는다.
        for (int i = 8; i < 16 && bsCode == 0; i++) {
            if (std[i] == bs) {
                bsCode = i;
            }
        }
        if (bsCode == 0) {
            bsCode = bs <= 256 ? 6 : 7;
        }
        int rateCode = 0;
        int[] rates = { 0, 88200, 176400, 192000, 8000, 16000, 22050, 24000, 32000, 44100, 48000, 96000 };
        // 표준 샘플링 주파수 코드를 찾는다(STREAMINFO 에서 가져오는 옵션이면 0 을 쓴다).
        for (int i = 1; i < rates.length && !opt.fromStreamInfo; i++) {
            if (rates[i] == opt.rate) {
                rateCode = i;
            }
        }
        if (rateCode == 0 && !opt.fromStreamInfo) {
            rateCode = opt.rate % 1000 == 0 && opt.rate / 1000 < 256 ? 12 : opt.rate < 65536 ? 13 : 0;
        }
        int sizeCode = opt.fromStreamInfo ? 0 : opt.bps == 8 ? 1 : opt.bps == 12 ? 2 : opt.bps == 16 ? 4 : opt.bps == 20 ? 5 : opt.bps == 24 ? 6 : opt.bps == 32 ? 7 : 0;
        int chCode = ch != 2 || mode == 0 ? ch - 1 : mode == 1 ? 8 : mode == 2 ? 9 : 10;
        h.put(bsCode, 4);
        h.put(rateCode, 4);
        h.put(chCode, 4);
        h.put(sizeCode, 3);
        h.put(0, 1);
        utf8(h, number);
        if (bsCode == 6) {
            h.put(bs - 1, 8);
        } else if (bsCode == 7) {
            h.put(bs - 1, 16);
        }
        if (rateCode == 12) {
            h.put(opt.rate / 1000, 8);
        } else if (rateCode == 13) {
            h.put(opt.rate, 16);
        }
        h.align();
        byte[] hb = h.bytes();
        Bits f = new Bits();
        f.out.bytes(hb);
        f.out.u8(crc(hb, 0x07, 8));
        // 채널마다 서브프레임을 쓴다.
        for (int c = 0; c < ch; c++) {
            subframe(f, sub[c], bs, width[c], opt);
        }
        f.align();
        byte[] body = f.bytes();
        int c16 = crc(body, 0x8005, 16);
        return new Out().bytes(body).be16(c16).toByteArray();
    }

    /**
     * Writes a number in the UTF-8 style variable length coding of FLAC frame headers.
     * <p>
     * FLAC 프레임 헤더의 UTF-8 방식 가변 길이 부호로 수를 쓴다.
     *
     * @param b bit writer / 비트 쓰기 객체
     * @param v value / 값
     */
    private static void utf8(Bits b, long v) {
        if (v < 0x80) {
            b.put(v, 8);
            return;
        }
        int extra = v < (1L << 11) ? 1 : v < (1L << 16) ? 2 : v < (1L << 21) ? 3 : v < (1L << 26) ? 4 : v < (1L << 31) ? 5 : 6;
        int lead = (0xFF00 >> (extra + 1)) & 0xFF;
        b.put(lead | (v >> (6 * extra)), 8);
        // 나머지 6비트 묶음을 10xxxxxx 형태로 쓴다.
        for (int i = extra - 1; i >= 0; i--) {
            b.put(0x80 | ((v >> (6 * i)) & 0x3F), 8);
        }
    }

    /**
     * Writes one subframe using the strategy of the options.
     * <p>
     * 옵션의 전략으로 서브프레임 하나를 쓴다.
     *
     * @param f   frame bits / 프레임 비트
     * @param x   signal / 신호
     * @param n   block size / 블록 크기
     * @param w   sample width in bits / 샘플 폭(비트)
     * @param opt options / 옵션
     */
    private static void subframe(Bits f, long[] x, int n, int w, Options opt) {
        int wasted = 0;
        boolean allZero = true;
        long or = 0;
        // 모든 샘플의 하위 0 비트 수(버려진 비트)를 구한다.
        for (int i = 0; i < n; i++) {
            or |= x[i];
            if (x[i] != 0) {
                allZero = false;
            }
        }
        if (!allZero) {
            wasted = Long.numberOfTrailingZeros(or);
        }
        long[] y = x;
        if (wasted > 0) {
            y = new long[n];
            // 버려진 비트를 제거한 신호를 만든다.
            for (int i = 0; i < n; i++) {
                y[i] = x[i] >> wasted;
            }
        }
        int bits = w - wasted;
        boolean constant = true;
        // 처음 샘플과 다른 샘플이 있는지 확인한다.
        for (int i = 1; i < n; i++) {
            if (y[i] != y[0]) {
                constant = false;
                break;
            }
        }
        String s = opt.strategy;
        f.put(0, 1);
        if (s.equals("constant") && !constant) {
            s = "auto";
        }
        if (s.equals("auto")) {
            s = constant ? "constant" : n > 40 ? "lpc8" : "fixed2";
        }
        int type;
        if (s.equals("constant")) {
            type = 0;
        } else if (s.equals("verbatim")) {
            type = 1;
        } else if (s.startsWith("fixed")) {
            type = 8 | Integer.parseInt(s.substring(5));
        } else {
            type = 32 | (Integer.parseInt(s.substring(3)) - 1);
        }
        // 블록이 너무 짧으면 선택한 예측 차수를 쓸 수 없으므로 그대로 저장한다.
        int order = type >= 32 ? (type & 31) + 1 : type >= 8 && type < 32 ? type & 7 : 0;
        if (type >= 8 && n <= order + 1) {
            type = 1;
        }
        f.put(type, 6);
        f.put(wasted > 0 ? 1 : 0, 1);
        if (wasted > 0) {
            f.unary(wasted - 1);
        }
        if (type == 0) {
            f.putSigned(y[0], bits);
        } else if (type == 1) {
            // 샘플을 그대로 쓴다.
            for (int i = 0; i < n; i++) {
                f.putSigned(y[i], bits);
            }
        } else if (type < 32) {
            fixed(f, y, n, bits, order, opt);
        } else {
            lpc(f, y, n, bits, order, opt);
        }
    }

    /**
     * Writes a fixed predictor subframe.
     * <p>
     * 고정 예측 서브프레임을 쓴다.
     *
     * @param f     frame bits / 프레임 비트
     * @param y     signal / 신호
     * @param n     block size / 블록 크기
     * @param bits  sample width / 샘플 폭
     * @param order order / 차수
     * @param opt   options / 옵션
     */
    private static void fixed(Bits f, long[] y, int n, int bits, int order, Options opt) {
        // 예열 샘플을 쓴다.
        for (int i = 0; i < order; i++) {
            f.putSigned(y[i], bits);
        }
        long[] e = new long[n];
        // 차수에 따른 고정 다항식 예측의 잔차를 구한다.
        for (int i = order; i < n; i++) {
            long p = 0;
            switch (order) {
            case 1:
                p = y[i - 1];
                break;
            case 2:
                p = 2 * y[i - 1] - y[i - 2];
                break;
            case 3:
                p = 3 * y[i - 1] - 3 * y[i - 2] + y[i - 3];
                break;
            case 4:
                p = 4 * y[i - 1] - 6 * y[i - 2] + 4 * y[i - 3] - y[i - 4];
                break;
            default:
                break;
            }
            e[i] = y[i] - p;
        }
        residual(f, e, n, order, opt);
    }

    /**
     * Writes an LPC subframe with coefficients from autocorrelation analysis.
     * <p>
     * 자기상관 분석으로 구한 계수를 쓰는 LPC 서브프레임을 쓴다.
     *
     * @param f     frame bits / 프레임 비트
     * @param y     signal / 신호
     * @param n     block size / 블록 크기
     * @param bits  sample width / 샘플 폭
     * @param order order / 차수
     * @param opt   options / 옵션
     */
    private static void lpc(Bits f, long[] y, int n, int bits, int order, Options opt) {
        double[] a = levinson(y, n, order);
        int precision = 12;
        double max = 0;
        // 계수의 최대 크기로 양자화 시프트를 정한다.
        for (double v : a) {
            max = Math.max(max, Math.abs(v));
        }
        int shift = 14;
        // 양자화 범위에 들어갈 때까지 시프트를 줄인다.
        while (shift > 0 && max * (1 << shift) >= (1 << (precision - 1)) - 1) {
            shift--;
        }
        long[] q = new long[order];
        // 계수를 정수로 양자화한다(표현 범위 안으로 자른다).
        for (int j = 0; j < order; j++) {
            q[j] = Math.round(a[j] * (1 << shift));
            q[j] = Math.max(-(1L << (precision - 1)), Math.min((1L << (precision - 1)) - 1, q[j]));
        }
        // 예열 샘플을 쓴다.
        for (int i = 0; i < order; i++) {
            f.putSigned(y[i], bits);
        }
        f.put(precision - 1, 4);
        f.putSigned(shift, 5);
        // 양자화한 계수를 쓴다.
        for (int j = 0; j < order; j++) {
            f.putSigned(q[j], precision);
        }
        long[] e = new long[n];
        // 양자화한 계수로 예측한 값과의 차이를 잔차로 구한다.
        for (int i = order; i < n; i++) {
            long sum = 0;
            // 예측 계수와 이전 샘플을 곱해 더한다.
            for (int j = 0; j < order; j++) {
                sum += q[j] * y[i - 1 - j];
            }
            e[i] = y[i] - (sum >> shift);
        }
        residual(f, e, n, order, opt);
    }

    /**
     * Linear prediction coefficients by autocorrelation and Levinson-Durbin recursion.
     * <p>
     * 자기상관과 Levinson-Durbin 재귀로 구한 선형 예측 계수.
     *
     * @param y     signal / 신호
     * @param n     length / 길이
     * @param order order / 차수
     * @return coefficients so that y[i] is predicted by sum of a[j] * y[i-1-j] / y[i] 를 a[j] * y[i-1-j] 의 합으로 예측하는 계수
     */
    private static double[] levinson(long[] y, int n, int order) {
        double[] r = new double[order + 1];
        // 지연마다 자기상관을 계산한다.
        for (int k = 0; k <= order; k++) {
            double s = 0;
            // 신호의 지연 샘플과의 곱을 더한다.
            for (int i = k; i < n; i++) {
                s += (double) y[i] * y[i - k];
            }
            r[k] = s;
        }
        r[0] = r[0] * (1 + 1e-9) + 1e-9;
        double[] a = new double[order];
        double err = r[0];
        // 차수를 하나씩 늘리며 계수를 갱신한다.
        for (int m = 0; m < order; m++) {
            double acc = r[m + 1];
            // 이전 계수와 자기상관으로 반사 계수를 계산한다.
            for (int j = 0; j < m; j++) {
                acc -= a[j] * r[m - j];
            }
            double k = err == 0 ? 0 : acc / err;
            double[] next = a.clone();
            next[m] = k;
            // 차수 이전 계수를 갱신한다.
            for (int j = 0; j < m; j++) {
                next[j] = a[j] - k * a[m - 1 - j];
            }
            a = next;
            err *= 1 - k * k;
            if (err <= 0) {
                break;
            }
        }
        return a;
    }

    /**
     * Writes the Rice coded residual of a prediction subframe.
     * <p>
     * 예측 서브프레임의 Rice 부호화된 잔차를 쓴다.
     *
     * @param f     frame bits / 프레임 비트
     * @param e     residuals (valid from index order) / 잔차(order 위치부터 유효)
     * @param n     block size / 블록 크기
     * @param order predictor order / 예측 차수
     * @param opt   options / 옵션
     */
    private static void residual(Bits f, long[] e, int n, int order, Options opt) {
        int po = opt.partitionOrder;
        // 블록 크기가 파티션으로 나누어지지 않거나 첫 파티션이 예측 차수보다 작으면 차수를 줄인다.
        while (po > 0 && (n % (1 << po) != 0 || (n >> po) < order)) {
            po--;
        }
        int maxParam = opt.riceMethod1 ? 30 : 14;
        int paramBits = opt.riceMethod1 ? 5 : 4;
        f.put(opt.riceMethod1 ? 1 : 0, 2);
        f.put(po, 4);
        int pos = order;
        // 파티션마다 가장 비트가 적은 Rice 매개변수를 고른다(이스케이프가 더 싸면 이스케이프).
        for (int p = 0; p < (1 << po); p++) {
            int count = (n >> po) - (p == 0 ? order : 0);
            int best = 0;
            long bestCost = Long.MAX_VALUE;
            // Rice 매개변수 후보마다 비용을 계산한다.
            for (int k = 0; k <= maxParam; k++) {
                long cost = 0;
                // 파티션의 잔차 비용을 합한다.
                for (int i = 0; i < count; i++) {
                    long v = e[pos + i];
                    long u = v >= 0 ? v << 1 : ((-v) << 1) - 1;
                    cost += (u >> k) + 1 + k;
                }
                if (cost < bestCost) {
                    bestCost = cost;
                    best = k;
                }
            }
            int escBits = 0;
            // 이스케이프 코드로 쓸 때 필요한 비트 수를 구한다.
            for (int i = 0; i < count; i++) {
                long v = e[pos + i];
                int need = v == 0 ? 0 : 65 - Long.numberOfLeadingZeros(v >= 0 ? v : ~v);
                escBits = Math.max(escBits, need);
            }
            if (opt.escape && escBits < 32 && (long) escBits * count + 5 < bestCost) {
                f.put((1 << paramBits) - 1, paramBits);
                f.put(escBits, 5);
                // 이스케이프 파티션의 잔차를 고정 비트 수로 쓴다.
                for (int i = 0; i < count; i++) {
                    f.putSigned(e[pos + i], escBits);
                }
            } else {
                f.put(best, paramBits);
                // 이스케이프가 아닌 파티션의 잔차를 Rice 부호로 쓴다.
                for (int i = 0; i < count; i++) {
                    long v = e[pos + i];
                    long u = v >= 0 ? v << 1 : ((-v) << 1) - 1;
                    f.unary(u >> best);
                    f.put(u & ((1L << best) - 1), best);
                }
            }
            pos += count;
        }
    }

    /**
     * Builds the packets of an Ogg FLAC stream: the mapping header packet (with STREAMINFO) first, then the frames.
     * <p>
     * Ogg FLAC 스트림의 패킷을 만든다: 먼저 매핑 헤더 패킷(STREAMINFO 포함), 그 다음 프레임들.
     *
     * @param r        encoder result / 인코딩 결과
     * @param metadata additional metadata block packets (complete with their 4-byte headers) / 추가 메타데이터 블록 패킷(4바이트 헤더 포함)
     * @return packets: header, metadata..., frames... / 패킷: 헤더, 메타데이터..., 프레임...
     */
    public static byte[][] oggPackets(Result r, byte[][] metadata) {
        Out first = new Out();
        first.u8(0x7F).ascii("FLAC").u8(1).u8(0).be16(metadata.length).ascii("fLaC").u8(metadata.length == 0 ? 0x80 : 0).be24(34).bytes(r.streamInfo);
        byte[][] all = new byte[1 + metadata.length + r.frames.length][];
        all[0] = first.toByteArray();
        System.arraycopy(metadata, 0, all, 1, metadata.length);
        System.arraycopy(r.frames, 0, all, 1 + metadata.length, r.frames.length);
        return all;
    }
}
