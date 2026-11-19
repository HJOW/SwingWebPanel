package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A test-only MPEG audio Layer III encoder written from the standard's analysis algorithms (polyphase analysis with the window
 * C[i] = D[i] / 32, forward MDCT with window switching, alias reduction in the encoder direction, quantization with global gain and
 * scale factors, Huffman coding, side information, bit reservoir). It does not share code with the decoder, so decoding its output checks
 * the decoder. It is a plain high-rate encoder, not a quality encoder.
 * <p>
 * 표준의 분석 알고리즘(윈도 C[i] = D[i] / 32 의 다상 분석, 윈도 전환 정방향 MDCT, 인코더 방향 얼라이어스 감소, 전체 이득과 스케일팩터 양자화,
 * 허프만 부호화, 부가 정보, 비트 저장소)으로 작성한 시험 전용 Layer III 인코더. 디코더와 코드를 공유하지 않으므로 이 출력을 디코딩하면 디코더를
 * 검증한다. 품질 인코더가 아니라 단순한 고비트레이트 인코더이다.
 */
final class Mp3TestEncoder {

    /** Bit sink. / 비트 출력. */
    private static final class BitOut {
        /** Bytes written so far. / 지금까지 쓴 바이트. */
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        /** Current byte under construction. / 만들고 있는 바이트. */
        int cur;
        /** Number of bits in {@link #cur}. / {@link #cur} 의 비트 수. */
        int n;
        /** Total number of bits written. / 쓴 전체 비트 수. */
        int total;

        /**
         * Writes bits, most significant first. / 상위 비트부터 비트를 쓴다.
         *
         * @param v value / 값
         * @param count number of bits / 비트 수
         */
        void put(int v, int count) {
            for (int i = count - 1; i >= 0; i--) { // 비트를 하나씩 쓴다
                cur = (cur << 1) | ((v >> i) & 1);
                n++;
                total++;
                if (n == 8) {
                    bytes.write(cur);
                    cur = 0;
                    n = 0;
                }
            }
        }

        /**
         * Pads to a byte boundary and returns the bytes. / 바이트 경계까지 채우고 바이트를 반환한다.
         *
         * @return bytes / 바이트
         */
        byte[] done() {
            while (n != 0) { // 마지막 바이트를 0 으로 채운다
                put(0, 1);
                total--;
            }
            return bytes.toByteArray();
        }
    }

    /** Code of one Huffman symbol. / 허프만 기호 하나의 부호. */
    private static final class Code {
        /** Code bits. / 부호 비트. */
        int bits;
        /** Code length (0 = no code). / 부호 길이(0 = 없음). */
        int len;
    }

    /** Huffman codes by table [table][x * 16 + y], built from the decoder's trees. / 표별 허프만 부호(디코더의 트리에서 만듦). */
    private static final Code[][] CODES = new Code[32][];

    /** Maximum value (without escape) that each simple table can code. / 단순 표가 부호화할 수 있는 최대 값(이스케이프 없이). */
    private static final int[] SIMPLE_TABLES = { 1, 2, 5, 7, 10, 13 };
    /** Maximum values of the simple tables. / 단순 표의 최대 값. */
    private static final int[] SIMPLE_MAX = { 1, 2, 3, 5, 7, 15 };

    /** MPEG version (1, 2, 25). / MPEG 버전. */
    private final int version;
    /** Sampling frequency. / 샘플링 주파수. */
    private final int rate;
    /** Number of channels. / 채널 수. */
    private final int channels;
    /** Bit rate in kbit/s. / 비트레이트(kbit/s). */
    private final int kbps;
    /** Use mid/side stereo. / MS 스테레오 사용. */
    boolean ms;
    /** Block type per granule (index = granule number), default long. / 그래뉼별 블록 종류. */
    int[] blockTypes = new int[0];
    /** Use scale factors (sf = band % 2). / 스케일팩터 사용(sf = 대역 % 2). */
    boolean scalefactors;
    /** Use scalefac_scale = 1. / scalefac_scale = 1 사용. */
    boolean sfScale;
    /** Largest quantized magnitude allowed when choosing the global gain. / 전체 이득을 고를 때 허용하는 양자화 값의 최댓값. */
    int maxQuant = 100;
    /** True to use the bit reservoir (main_data_begin may be non-zero). / 비트 저장소 사용 여부. */
    boolean reservoir;
    /** Main data begin of every frame written (for assertions). / 쓴 프레임마다 main_data_begin(검증용). */
    final List<Integer> mainDataBegins = new ArrayList<Integer>();

    /** Previous granule's subband samples [ch][sb][18]. / 앞 그래뉼의 서브밴드 샘플. */
    private final double[][][] prevSb;
    /** Analysis input buffer [ch][512]. / 분석 입력 버퍼. */
    private final double[][] xbuf;

    /**
     * Creates an encoder.
     * <p>
     * 인코더를 만든다.
     *
     * @param version  1, 2 or 25 / 1, 2, 25
     * @param rate     sampling frequency / 샘플링 주파수
     * @param channels 1 or 2 / 1 또는 2
     * @param kbps     bit rate / 비트레이트
     */
    Mp3TestEncoder(int version, int rate, int channels, int kbps) {
        this.version = version;
        this.rate = rate;
        this.channels = channels;
        this.kbps = kbps;
        this.prevSb = new double[channels][32][18];
        this.xbuf = new double[channels][512];
        buildCodes();
    }

    /**
     * Builds the Huffman code maps by walking the decoder's trees.
     * <p>
     * 디코더의 트리를 따라가며 허프만 부호 표를 만든다.
     */
    private static synchronized void buildCodes() {
        if (CODES[1] != null) {
            return;
        }
        for (int t = 1; t < 32; t++) { // 표마다 트리를 깊이 우선으로 훑는다
            if (Mp3Tables.TREE_LENGTH[t] == 0) {
                continue;
            }
            CODES[t] = new Code[256];
            for (int i = 0; i < 256; i++) { // 부호 표 항목을 초기화한다
                CODES[t][i] = new Code();
            }
            walk(t, 0, 0, 0);
        }
    }

    /**
     * Recursive tree walk.
     * <p>
     * 재귀 트리 순회.
     *
     * @param t     table / 표
     * @param point node / 노드
     * @param bits  path so far / 지금까지의 경로
     * @param len   path length / 경로 길이
     */
    private static void walk(int t, int point, int bits, int len) {
        char[] tree = Mp3Tables.HUFFMAN_TREE;
        int base = Mp3Tables.TREE_OFFSET[t];
        int word = tree[base + point];
        if ((word & 0xFF00) == 0) {
            Code c = CODES[t][word & 0xFF];
            c.bits = bits;
            c.len = len;
            return;
        }
        if (len >= 30) {
            return;
        }
        int p0 = point;
        while ((tree[base + p0] >> 8) >= 250) { // 왼쪽 연쇄 점프
            p0 += tree[base + p0] >> 8;
        }
        p0 += tree[base + p0] >> 8;
        walk(t, p0, bits << 1, len + 1);
        int p1 = point;
        while ((tree[base + p1] & 0xFF) >= 250) { // 오른쪽 연쇄 점프
            p1 += tree[base + p1] & 0xFF;
        }
        p1 += tree[base + p1] & 0xFF;
        walk(t, p1, (bits << 1) | 1, len + 1);
    }

    /**
     * Encodes PCM into a complete sequence of frames.
     * <p>
     * PCM 을 프레임 연속으로 부호화한다.
     *
     * @param pcm samples [channel][n] in -1..1 / 샘플
     * @return the frames / 프레임들
     */
    byte[] encode(double[][] pcm) {
        int ngr = version == 1 ? 2 : 1;
        int spf = ngr * 576;
        int n = pcm[0].length;
        int frames = (n + spf - 1) / spf + 2;
        int sideLen = version == 1 ? (channels == 1 ? 17 : 32) : (channels == 1 ? 9 : 17);
        int mainBeginMax = version == 1 ? 511 : 255;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<byte[]> heads = new ArrayList<byte[]>();
        List<int[]> regions = new ArrayList<int[]>();
        int capStart = 0;
        int granuleNo = 0;
        for (int f = 0; f < frames; f++) { // 프레임마다 그래뉼을 부호화해 주 데이터 스트림에 놓는다
            int frameBytes = frameBytes(f);
            int capacity = frameBytes - 4 - sideLen;
            BitOut[][] data = new BitOut[2][2];
            List<int[]> sideFields = new ArrayList<int[]>();
            for (int gr = 0; gr < ngr; gr++) { // 그래뉼마다 분석하고 양자화한다
                double[][] coef = new double[channels][];
                int bt = granuleNo < blockTypes.length ? blockTypes[granuleNo] : 0;
                for (int ch = 0; ch < channels; ch++) { // 채널마다 분석과 MDCT 를 한다
                    double[][] sb = analyze(pcm[ch], ch, (f * ngr + gr) * 576);
                    coef[ch] = mdct(sb, ch, bt);
                }
                if (ms && channels == 2) {
                    for (int i = 0; i < 576; i++) { // 중간/측면으로 바꾼다
                        double l = coef[0][i];
                        double r = coef[1][i];
                        coef[0][i] = (l + r) / Math.sqrt(2.0);
                        coef[1][i] = (l - r) / Math.sqrt(2.0);
                    }
                }
                for (int ch = 0; ch < channels; ch++) { // 채널마다 양자화하고 부호화한다
                    int[] side = new int[16];
                    data[gr][ch] = quantizeAndCode(coef[ch], bt, side);
                    sideFields.add(side);
                }
                granuleNo++;
            }
            BitOut all = new BitOut();
            for (int gr = 0; gr < ngr; gr++) { // 그래뉼과 채널의 데이터를 이어 붙인다
                for (int ch = 0; ch < channels; ch++) {
                    appendBits(all, data[gr][ch]);
                }
            }
            byte[] dataBytes = all.done();
            int dataStart = reservoir ? Math.max(stream.size(), capStart - mainBeginMax) : capStart;
            if (dataStart > stream.size()) { // 허용 범위 안으로 맞추려고 스터핑을 넣는다
                byte[] pad = new byte[dataStart - stream.size()];
                stream.write(pad, 0, pad.length);
            }
            int mainBegin = capStart - dataStart;
            if (mainBegin < 0 || dataStart + dataBytes.length > capStart + capacity) {
                throw new IllegalStateException("frame " + f + " needs " + dataBytes.length + " bytes, capacity " + capacity);
            }
            stream.write(dataBytes, 0, dataBytes.length);
            mainDataBegins.add(Integer.valueOf(mainBegin));
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            head.write(0xFF);
            head.write(version == 1 ? 0xFB : (version == 2 ? 0xF3 : 0xE3));
            head.write((bitrateIndex() << 4) | (rateIndex() << 2) | ((frameBytes - baseBytes(f)) != 0 ? 2 : 0));
            head.write(channels == 1 ? 0xC0 : (ms ? 0x60 : 0x00));
            BitOut si = new BitOut();
            si.put(mainBegin, version == 1 ? 9 : 8);
            si.put(0, version == 1 ? (channels == 1 ? 5 : 3) : (channels == 1 ? 1 : 2));
            if (version == 1) {
                si.put(0, 4 * channels);
            }
            int k = 0;
            for (int gr = 0; gr < ngr; gr++) { // 그래뉼·채널의 부가 정보를 쓴다
                for (int ch = 0; ch < channels; ch++) {
                    writeSide(si, sideFields.get(k), data[gr][ch].total);
                    k++;
                }
            }
            byte[] sideBytes = si.done();
            head.write(sideBytes, 0, sideBytes.length);
            heads.add(head.toByteArray());
            regions.add(new int[] { capStart, capacity });
            capStart += capacity;
        }
        // 완성된 스트림에서 프레임마다 용량 구간을 잘라 헤더 뒤에 붙인다.
        byte[] sAll = stream.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int f = 0; f < frames; f++) { // 프레임마다 헤더와 본문을 쓴다
            byte[] h = heads.get(f);
            out.write(h, 0, h.length);
            int cs = regions.get(f)[0];
            int cap = regions.get(f)[1];
            for (int i = 0; i < cap; i++) { // 스트림 끝을 넘는 부분은 0 으로 채운다
                out.write(cs + i < sAll.length ? sAll[cs + i] : 0);
            }
        }
        return out.toByteArray();
    }

    /**
     * Appends the bits of one sink to another.
     * <p>
     * 비트 출력 하나의 비트를 다른 출력에 이어 붙인다.
     *
     * @param dst destination / 대상
     * @param src source / 원본
     */
    private static void appendBits(BitOut dst, BitOut src) {
        byte[] b = src.bytes.toByteArray();
        for (int i = 0; i < b.length; i++) { // 완전한 바이트를 옮긴다
            dst.put(b[i] & 0xFF, 8);
        }
        if (src.n > 0) {
            dst.put(src.cur, src.n);
        }
    }

    /**
     * Writes the side information of one granule and channel.
     * <p>
     * 그래뉼과 채널 하나의 부가 정보를 쓴다.
     *
     * @param si   sink / 출력
     * @param f    fields: part23, big, gain, compress, blockType, t0, t1, t2, r0, r1, preflag, scale, c1table, sbgain... / 필드
     * @param bits part2_3 bits / part2_3 비트 수
     */
    private void writeSide(BitOut si, int[] f, int bits) {
        si.put(bits, 12);
        si.put(f[1], 9);
        si.put(f[2], 8);
        si.put(f[3], version == 1 ? 4 : 9);
        boolean win = f[4] != 0;
        si.put(win ? 1 : 0, 1);
        if (win) {
            si.put(f[4], 2);
            si.put(0, 1);
            si.put(f[5], 5);
            si.put(f[6], 5);
            si.put(0, 9);
        } else {
            si.put(f[5], 5);
            si.put(f[6], 5);
            si.put(f[7], 5);
            si.put(f[8], 4);
            si.put(f[9], 3);
        }
        if (version == 1) {
            si.put(0, 1);
        }
        si.put(f[11], 1);
        si.put(0, 1);
    }

    /**
     * Returns the frame length for frame number f (cycles the padding to reach the exact bit rate).
     * <p>
     * 프레임 f 의 길이를 반환한다(정확한 비트레이트를 맞추려고 패딩을 번갈아 쓴다).
     *
     * @param f frame number / 프레임 번호
     * @return bytes / 바이트
     */
    private int frameBytes(int f) {
        return baseBytes(f) + ((slotsFraction(f)) ? 1 : 0);
    }

    /**
     * Returns the base length without padding.
     * <p>
     * 패딩을 뺀 기본 길이를 반환한다.
     *
     * @param f frame number / 프레임 번호
     * @return bytes / 바이트
     */
    private int baseBytes(int f) {
        int coef = version == 1 ? 144 : 72;
        return coef * kbps * 1000 / rate;
    }

    /**
     * Tells whether frame f carries a padding byte.
     * <p>
     * 프레임 f 에 패딩 바이트가 있는지 알려 준다.
     *
     * @param f frame number / 프레임 번호
     * @return true if padded / 패딩이 있으면 true
     */
    private boolean slotsFraction(int f) {
        int coef = version == 1 ? 144 : 72;
        long a = (long) coef * kbps * 1000 * f;
        long b = (long) coef * kbps * 1000 * (f + 1);
        return (b / rate) - (a / rate) > baseBytes(f);
    }

    /**
     * Returns the bit rate index.
     * <p>
     * 비트레이트 인덱스를 반환한다.
     *
     * @return index / 인덱스
     */
    private int bitrateIndex() {
        int[] v1 = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 };
        int[] v2 = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 };
        int[] t = version == 1 ? v1 : v2;
        for (int i = 1; i < t.length; i++) { // 비트레이트와 같은 항목을 찾는다
            if (t[i] == kbps) {
                return i;
            }
        }
        throw new IllegalArgumentException("bit rate");
    }

    /**
     * Returns the sampling frequency index.
     * <p>
     * 샘플링 주파수 인덱스를 반환한다.
     *
     * @return index / 인덱스
     */
    private int rateIndex() {
        int base = version == 1 ? rate : (version == 2 ? rate * 2 : rate * 4);
        return base == 44100 ? 0 : (base == 48000 ? 1 : 2);
    }

    /**
     * Runs the polyphase analysis filter bank over 576 input samples and returns 18 slots of 32 subband samples.
     * <p>
     * 입력 576개에 다상 분석 필터 뱅크를 적용해 서브밴드 샘플 32개씩 18슬롯을 반환한다.
     *
     * @param x     channel input / 채널 입력
     * @param ch    channel / 채널
     * @param start first input sample index / 첫 입력 위치
     * @return [subband][slot] / [서브밴드][슬롯]
     */
    private double[][] analyze(double[] x, int ch, int start) {
        double[][] sb = new double[32][18];
        double[] buf = xbuf[ch];
        for (int slot = 0; slot < 18; slot++) { // 슬롯마다 입력 32개를 받아 서브밴드 32개를 만든다
            System.arraycopy(buf, 0, buf, 32, 512 - 32);
            for (int i = 0; i < 32; i++) {
                int idx = start + slot * 32 + i;
                buf[31 - i] = idx < x.length ? x[idx] : 0.0;
            }
            double[] y = new double[64];
            for (int i = 0; i < 64; i++) { // 윈도를 곱해 8개씩 접는다
                double s = 0;
                for (int j = 0; j < 8; j++) {
                    s += buf[i + 64 * j] * (Mp3Tables.SYNTH_WINDOW[i + 64 * j] / 32.0);
                }
                y[i] = s;
            }
            for (int k = 0; k < 32; k++) { // 행렬 곱으로 서브밴드 샘플을 만든다
                double s = 0;
                for (int i = 0; i < 64; i++) {
                    s += Math.cos((2 * k + 1) * (i - 16) * Math.PI / 64.0) * y[i];
                }
                sb[k][slot] = s;
            }
        }
        return sb;
    }

    /**
     * Applies the frequency inversion, the forward MDCT with the window of the block type and the alias reduction.
     * <p>
     * 주파수 반전, 블록 종류의 윈도를 쓴 정방향 MDCT, 얼라이어스 감소를 적용한다.
     *
     * @param sb  subband samples [subband][slot] / 서브밴드 샘플
     * @param ch  channel / 채널
     * @param bt  block type / 블록 종류
     * @return 576 coefficients in bit stream order / 비트스트림 순서의 계수 576개
     */
    private double[] mdct(double[][] sb, int ch, int bt) {
        double[] spec = new double[576];
        for (int s = 0; s < 32; s++) { // 서브밴드마다 변환한다
            double[] z = new double[36];
            for (int i = 0; i < 18; i++) {
                double cur = sb[s][i];
                if ((s & 1) == 1 && (i & 1) == 1) {
                    cur = -cur;
                }
                z[i] = prevSb[ch][s][i];
                z[18 + i] = cur;
            }
            for (int i = 0; i < 18; i++) { // 다음 그래뉼을 위해 현재 값을 보관한다
                double cur = sb[s][i];
                if ((s & 1) == 1 && (i & 1) == 1) {
                    cur = -cur;
                }
                prevSb[ch][s][i] = cur;
            }
            if (bt == 2) {
                for (int w = 0; w < 3; w++) { // 짧은 창 3개
                    for (int k = 0; k < 6; k++) {
                        double sum = 0;
                        for (int nn = 0; nn < 12; nn++) { // 창 안의 샘플마다 합한다
                            double win = Math.sin(Math.PI / 12 * (nn + 0.5));
                            sum += z[6 + 6 * w + nn] * win * Math.cos(Math.PI / 24 * (2 * nn + 1 + 6) * (2 * k + 1));
                        }
                        spec[s * 18 + w + 3 * k] = sum / 3.0;
                    }
                }
            } else {
                for (int k = 0; k < 18; k++) { // 긴 창
                    double sum = 0;
                    for (int nn = 0; nn < 36; nn++) {
                        sum += z[nn] * longWindow(bt, nn) * Math.cos(Math.PI / 72 * (2 * nn + 1 + 18) * (2 * k + 1));
                    }
                    spec[s * 18 + k] = sum / 9.0;
                }
            }
        }
        if (bt != 2) { // 얼라이어스 감소(디코더 연산의 역)
            double[] c = { -0.6, -0.535, -0.33, -0.185, -0.095, -0.041, -0.0142, -0.0037 };
            for (int s = 1; s < 32; s++) {
                for (int i = 0; i < 8; i++) { // 버터플라이 8쌍을 계산한다
                    double cs = 1 / Math.sqrt(1 + c[i] * c[i]);
                    double ca = c[i] / Math.sqrt(1 + c[i] * c[i]);
                    double lo = spec[18 * s - 1 - i];
                    double up = spec[18 * s + i];
                    spec[18 * s - 1 - i] = lo * cs + up * ca;
                    spec[18 * s + i] = up * cs - lo * ca;
                }
            }
        } else { // 짧은 블록은 대역-창 순서(비트스트림 순서)로 되돌린다.
            int[] widths = shortWidths();
            double[] out = new double[576];
            int pos = 0;
            int startCoef = 0;
            for (int b = 0; b < widths.length; b++) { // 대역마다 창 3개를 이어 붙인다
                for (int w = 0; w < 3; w++) {
                    for (int j = 0; j < widths[b]; j++) {
                        out[pos++] = spec[3 * (startCoef + j) + w];
                    }
                }
                startCoef += widths[b];
            }
            spec = out;
        }
        return spec;
    }

    /**
     * Returns the short block band widths of the stream (one entry per band).
     * <p>
     * 스트림의 짧은 블록 대역 폭(대역마다 하나)을 반환한다.
     *
     * @return widths / 폭
     */
    private int[] shortWidths() {
        int idx;
        if (version == 1) {
            idx = rate == 44100 ? 5 : (rate == 48000 ? 6 : 7);
        } else if (version == 2) {
            idx = rate == 22050 ? 2 : (rate == 24000 ? 3 : 4);
        } else {
            idx = rate == 8000 ? 1 : 0;
        }
        int[] src = Mp3Tables.SFB_SHORT[idx];
        int[] w = new int[13];
        for (int b = 0; b < 13; b++) { // 창 3개마다 반복되는 폭에서 대역 폭만 뽑는다
            w[b] = src[3 * b];
        }
        return w;
    }

    /**
     * Returns the long window value of a block type.
     * <p>
     * 블록 종류의 긴 윈도 값을 반환한다.
     *
     * @param bt block type 0, 1 or 3 / 블록 종류
     * @param i  index 0..35 / 위치
     * @return window value / 윈도 값
     */
    private static double longWindow(int bt, int i) {
        if (bt == 0 || (bt == 1 && i < 18) || (bt == 3 && i >= 18)) {
            return Math.sin(Math.PI / 36 * (i + 0.5));
        }
        if (bt == 1) {
            if (i < 24) {
                return 1.0;
            }
            return i < 30 ? Math.sin(Math.PI / 12 * (i - 18 + 0.5)) : 0.0;
        }
        if (i < 6) {
            return 0.0;
        }
        return i < 12 ? Math.sin(Math.PI / 12 * (i - 6 + 0.5)) : 1.0;
    }

    /**
     * Quantizes a granule, chooses the Huffman tables and returns the bits (scale factors and Huffman data).
     * <p>
     * 그래뉼을 양자화하고 허프만 표를 골라 비트(스케일팩터와 허프만 데이터)를 반환한다.
     *
     * @param spec spectrum in bit stream order / 비트스트림 순서의 스펙트럼
     * @param bt   block type / 블록 종류
     * @param side output side info fields / 출력 부가 정보 필드
     * @return bits / 비트
     */
    private BitOut quantizeAndCode(double[] spec, int bt, int[] side) {
        int[] sfb = bandWidths(bt);
        int[] sf = new int[sfb.length];
        int sfc = 0;
        int scaleBit = 0;
        if (scalefactors && bt != 2) {
            for (int b = 0; b < 21; b++) { // 대역마다 스케일팩터 0 또는 1
                sf[b] = b % 2;
            }
            sfc = version == 1 ? 5 : 101;
            scaleBit = sfScale ? 1 : 0;
        }
        double mult = scaleBit != 0 ? 1.0 : 0.5;
        int[] ix = new int[576];
        int gain = 210 - 40;
        double maxAbs = 0;
        for (int i = 0; i < 576; i++) { // 가장 큰 값을 찾는다
            maxAbs = Math.max(maxAbs, Math.abs(spec[i]));
        }
        if (maxAbs > 0) { // 최댓값이 maxQuant 이하가 되는 가장 작은 전체 이득을 찾는다
            for (gain = 0; gain < 255; gain++) {
                if (quantize(spec, sfb, sf, mult, gain, ix) <= maxQuant) {
                    break;
                }
            }
        } else {
            gain = 210;
            quantize(spec, sfb, sf, mult, gain, ix);
        }
        int big = 0;
        for (int i = 575; i >= 0; i--) { // 마지막 0 이 아닌 값 위치를 찾는다
            if (ix[i] != 0) {
                big = (i + 2) / 2;
                break;
            }
        }
        int region1Start;
        int region2Start;
        int r0 = 0;
        int r1 = 0;
        int[] cum = cumulative(Mp3Tables.SFB_LONG[bandLayout()]);
        if (bt == 2) {
            region1Start = 36;
            region2Start = 576;
        } else if (bt == 0) {
            r0 = 7;
            r1 = 7;
            region1Start = cum[r0 + 1];
            region2Start = cum[r0 + r1 + 2];
        } else {
            region1Start = cum[8];
            region2Start = cum[22];
        }
        int[] tableSel = new int[3];
        int[] bounds = { 0, Math.min(region1Start, big * 2), Math.min(region2Start, big * 2), big * 2 };
        for (int r = 0; r < 3; r++) { // 영역마다 값의 최댓값으로 허프만 표를 고른다
            int m = 0;
            for (int i = bounds[r]; i < bounds[r + 1]; i++) {
                m = Math.max(m, Math.abs(ix[i]));
            }
            tableSel[r] = chooseTable(m);
        }
        BitOut bo = new BitOut();
        if (scalefactors && bt != 2) { // 스케일팩터 비트(MPEG-1 은 slen 1, LSF 는 모두 1비트)
            for (int b = 0; b < 21; b++) {
                bo.put(sf[b], 1);
            }
        }
        for (int p = 0; p < big; p++) { // 쌍마다 해당 영역의 표로 부호화한다
            int i = p * 2;
            int t = i < bounds[1] ? tableSel[0] : (i < bounds[2] ? tableSel[1] : tableSel[2]);
            putPair(bo, t, ix[i], ix[i + 1]);
        }
        side[1] = big;
        side[2] = gain;
        side[3] = sfc;
        side[4] = bt == 0 ? 0 : bt;
        side[5] = tableSel[0];
        side[6] = tableSel[1];
        side[7] = tableSel[2];
        side[8] = r0;
        side[9] = r1;
        side[11] = scaleBit;
        return bo;
    }

    /**
     * Returns the index of the long band layout of the stream.
     * <p>
     * 스트림의 긴 대역 배치 번호를 반환한다.
     *
     * @return index / 번호
     */
    private int bandLayout() {
        if (version == 1) {
            return rate == 44100 ? 5 : (rate == 48000 ? 6 : 7);
        }
        if (version == 2) {
            return rate == 22050 ? 2 : (rate == 24000 ? 3 : 4);
        }
        return rate == 8000 ? 1 : 0;
    }

    /**
     * Returns the band widths (bit stream order segments) of a block type.
     * <p>
     * 블록 종류의 대역 폭(비트스트림 순서의 구간)을 반환한다.
     *
     * @param bt block type / 블록 종류
     * @return widths without terminator / 끝 표시를 뺀 폭
     */
    private int[] bandWidths(int bt) {
        int idx = bandLayout();
        int[] src = bt == 2 ? Mp3Tables.SFB_SHORT[idx] : Mp3Tables.SFB_LONG[idx];
        int n = 0;
        while (src[n] != 0) { // 끝 표시까지 센다
            n++;
        }
        int[] w = new int[n];
        System.arraycopy(src, 0, w, 0, n);
        return w;
    }

    /**
     * Computes cumulative starts.
     * <p>
     * 누적 시작 위치를 계산한다.
     *
     * @param widths widths with terminator / 끝 표시가 있는 폭
     * @return starts / 시작 위치
     */
    private static int[] cumulative(int[] widths) {
        int[] c = new int[widths.length];
        int s = 0;
        for (int i = 0; widths[i] != 0; i++) { // 폭을 누적한다
            c[i] = s;
            s += widths[i];
            c[i + 1] = s;
        }
        return c;
    }

    /**
     * Quantizes the spectrum with a global gain and returns the largest magnitude.
     * <p>
     * 전체 이득으로 스펙트럼을 양자화하고 가장 큰 크기를 반환한다.
     *
     * @param spec spectrum / 스펙트럼
     * @param sfb  segment widths / 구간 폭
     * @param sf   scale factors by segment / 구간별 스케일팩터
     * @param mult scale factor multiplier / 스케일팩터 배율
     * @param gain global gain / 전체 이득
     * @param ix   output / 출력
     * @return largest magnitude / 가장 큰 크기
     */
    private static int quantize(double[] spec, int[] sfb, int[] sf, double mult, int gain, int[] ix) {
        int pos = 0;
        int max = 0;
        for (int b = 0; b < sfb.length; b++) { // 구간마다 단계 크기를 계산해 양자화한다
            double step = Math.pow(2.0, (gain - 210) / 4.0 - mult * sf[b]);
            for (int j = 0; j < sfb[b] && pos < 576; j++, pos++) {
                double v = Math.abs(spec[pos]) / step;
                int q = (int) Math.floor(Math.pow(v, 0.75) + 0.4054);
                ix[pos] = spec[pos] < 0 ? -q : q;
                max = Math.max(max, q);
            }
        }
        for (; pos < 576; pos++) { // 구간 밖은 0
            ix[pos] = 0;
        }
        return max;
    }

    /**
     * Chooses the smallest Huffman table that can code values up to a maximum.
     * <p>
     * 값의 최댓값까지 부호화할 수 있는 가장 작은 허프만 표를 고른다.
     *
     * @param max largest magnitude / 가장 큰 크기
     * @return table number / 표 번호
     */
    private static int chooseTable(int max) {
        if (max == 0) {
            return 0;
        }
        for (int i = 0; i < SIMPLE_TABLES.length; i++) { // 단순 표 중에서 찾는다
            if (max <= SIMPLE_MAX[i]) {
                return SIMPLE_TABLES[i];
            }
        }
        for (int t = 16; t < 24; t++) { // linbits 가 있는 표 중에서 찾는다
            if (max <= 15 + (1 << Mp3Tables.LINBITS[t]) - 1) {
                return t;
            }
        }
        throw new IllegalStateException("value too large: " + max);
    }

    /**
     * Writes a pair of values with a Huffman table.
     * <p>
     * 값 한 쌍을 허프만 표로 쓴다.
     *
     * @param bo output / 출력
     * @param t  table / 표
     * @param x  first value / 첫 값
     * @param y  second value / 둘째 값
     */
    private static void putPair(BitOut bo, int t, int x, int y) {
        if (t == 0) {
            return;
        }
        int ax = Math.abs(x);
        int ay = Math.abs(y);
        int lin = Mp3Tables.LINBITS[t];
        int cx = lin != 0 ? Math.min(ax, 15) : ax;
        int cy = lin != 0 ? Math.min(ay, 15) : ay;
        Code c = CODES[t][cx * 16 + cy];
        if (c.len == 0) {
            throw new IllegalStateException("no code for " + cx + "," + cy + " in table " + t);
        }
        bo.put(c.bits, c.len);
        if (lin != 0 && ax >= 15) {
            bo.put(ax - 15, lin);
        }
        if (ax != 0) {
            bo.put(x < 0 ? 1 : 0, 1);
        }
        if (lin != 0 && ay >= 15) {
            bo.put(ay - 15, lin);
        }
        if (ay != 0) {
            bo.put(y < 0 ? 1 : 0, 1);
        }
    }
}
