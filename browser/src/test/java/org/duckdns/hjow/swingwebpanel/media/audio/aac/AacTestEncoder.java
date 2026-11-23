package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A test-only AAC-LC encoder written from ISO/IEC 14496-3 (forward MDCT with window sequences and window shapes, quantization with scale
 * factors, section data, Huffman coding with codebook 11 and escapes, M/S stereo, grouping of short windows, TNS analysis filtering, pulse
 * data, ADTS framing). It shares only the Huffman code tables with the decoder; everything else is independent, so decoding its output checks
 * the decoder. It is a plain encoder, not a quality encoder.
 * <p>
 * ISO/IEC 14496-3 으로 작성한 시험 전용 AAC-LC 인코더(윈도 순서와 모양을 쓰는 정방향 MDCT, 스케일팩터 양자화, 구간 데이터, 코드북 11 과 이스케이프를
 * 쓰는 허프만 부호화, M/S 스테레오, 짧은 윈도 그룹, TNS 분석 필터, 펄스 데이터, ADTS 프레임). 디코더와 허프만 부호 표만 공유하며 나머지는
 * 독립이므로 이 출력을 디코딩하면 디코더를 검증한다. 품질 인코더가 아닌 단순한 인코더이다.
 */
final class AacTestEncoder {

    /** Bit sink. / 비트 출력. */
    private static final class BitOut {
        /** Bytes written. / 쓴 바이트. */
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        /** Byte under construction. / 만드는 중인 바이트. */
        int cur;
        /** Bits in the current byte. / 현재 바이트의 비트 수. */
        int n;

        /**
         * Writes bits, most significant first. / 상위 비트부터 쓴다.
         *
         * @param v value / 값
         * @param count number of bits / 비트 수
         */
        void put(int v, int count) {
            for (int i = count - 1; i >= 0; i--) { // 비트를 하나씩 쓴다
                cur = (cur << 1) | ((v >> i) & 1);
                n++;
                if (n == 8) {
                    bytes.write(cur);
                    cur = 0;
                    n = 0;
                }
            }
        }

        /**
         * Pads with zero bits to a byte boundary. / 바이트 경계까지 0 으로 채운다.
         */
        void align() {
            while (n != 0) { // 바이트가 찰 때까지 0 비트를 쓴다
                put(0, 1);
            }
        }
    }

    /** Huffman code (length, code) of codebook 11 by pair [a * 17 + b]. / 코드북 11 의 허프만 부호. */
    private static final int[][] CB11 = new int[17 * 17][];
    /** Scale factor Huffman code (length, code) by index 0 to 120. / 스케일팩터 허프만 부호. */
    private static final int[][] SF = new int[121][];

    static {
        for (int[] row : AacCodebooks.spectral(10)) { // 코드북 11 의 행을 (값 쌍 → 부호) 표로 바꾼다
            CB11[row[2] * 17 + row[3]] = new int[] { row[0], row[1] };
        }
        for (int[] row : AacCodebooks.hcb_sf()) { // 스케일팩터 코드북을 (값 → 부호) 표로 바꾼다
            SF[row[2]] = new int[] { row[0], row[1] };
        }
    }

    /** Sampling frequency index. / 샘플링 주파수 인덱스. */
    final int rateIndex;
    /** Number of channels 1 or 2. / 채널 수 1 또는 2. */
    final int channels;
    /** Window sequence per frame (0 long, 1 start, 2 short, 3 stop), default long. / 프레임별 윈도 순서. */
    int[] sequences = new int[0];
    /** Window shape per frame (0 sine, 1 Kaiser-Bessel). / 프레임별 윈도 모양. */
    int[] shapes = new int[0];
    /** The 7 bit window grouping of short frames. / 짧은 프레임의 7비트 윈도 그룹. */
    int grouping;
    /** MS mask: 0 none, 1 per band (alternating), 2 all. / MS 마스크. */
    int msMode;
    /** Largest quantized magnitude when choosing the gain. / 이득을 고를 때 양자화 값의 최댓값. */
    int maxQuant = 200;
    /** Vary the scale factors of the bands. / 대역마다 스케일팩터를 다르게 한다. */
    boolean varySf;
    /** Number of TNS filters per window (0 = no TNS). / 윈도당 TNS 필터 수(0 이면 TNS 없음). */
    int tnsFilters;
    /** TNS filter order. / TNS 필터 차수. */
    int tnsOrder = 4;
    /** Use pulse data in long frames. / 긴 프레임에서 펄스 데이터를 쓴다. */
    boolean pulse;
    /** The raw data blocks written. / 쓴 원시 데이터 블록. */
    final List<byte[]> blocks = new ArrayList<byte[]>();

    /** Previous time samples of the input (for the overlapping blocks) [channel][2048]. / 입력의 앞 시간 샘플. */
    private final double[][] history;

    /**
     * Creates an encoder.
     * <p>
     * 인코더를 만든다.
     *
     * @param rateIndex sampling frequency index / 샘플링 주파수 인덱스
     * @param channels  1 or 2 / 1 또는 2
     */
    AacTestEncoder(int rateIndex, int channels) {
        this.rateIndex = rateIndex;
        this.channels = channels;
        this.history = new double[channels][0];
    }

    /**
     * Returns the rising half of a window.
     * <p>
     * 윈도의 상승 절반을 반환한다.
     *
     * @param shape 0 sine, 1 Kaiser-Bessel / 0 사인, 1 카이저-베셀
     * @param half  number of samples of the half (1024 or 128) / 절반의 샘플 수
     * @return values / 값
     */
    static double[] rise(int shape, int half) {
        double[] w = new double[half];
        if (shape == 0) {
            for (int n = 0; n < half; n++) { // 사인 윈도
                w[n] = Math.sin(Math.PI * (n + 0.5) / (2.0 * half));
            }
            return w;
        }
        double alpha = half == 1024 ? 4.0 : 6.0;
        double[] k = new double[half + 1];
        double total = 0;
        for (int p = 0; p <= half; p++) { // 카이저 창
            double r = 2.0 * p / half - 1.0;
            k[p] = bessel(Math.PI * alpha * Math.sqrt(Math.max(0, 1 - r * r)));
            total += k[p];
        }
        double acc = 0;
        for (int n = 0; n < half; n++) { // 누적 합의 제곱근
            acc += k[n];
            w[n] = Math.sqrt(acc / total);
        }
        return w;
    }

    /**
     * Modified Bessel function of order zero.
     * <p>
     * 0차 변형 베셀 함수.
     *
     * @param x argument / 인수
     * @return value / 값
     */
    private static double bessel(double x) {
        double s = 1;
        double t = 1;
        for (int i = 1; i < 80; i++) { // 급수를 더한다
            t *= (x / (2.0 * i)) * (x / (2.0 * i));
            s += t;
        }
        return s;
    }

    /**
     * The forward MDCT of a windowed block: <code>X[k] = 2 * sum of x[n] cos(2 pi / N (n + n0)(k + 1/2))</code>, n0 = (N/2 + 1)/2 (the factor 2 of ISO/IEC 14496-3 4.6.11.2 that pairs with the decoder's 2/N).
     * <p>
     * 윈도를 곱한 블록의 정방향 MDCT.
     *
     * @param x windowed block of N samples / 윈도를 곱한 N 샘플
     * @return N/2 coefficients / 계수 N/2개
     */
    static double[] mdct(double[] x) {
        int n = x.length;
        double[] out = new double[n / 2];
        double n0 = (n / 2.0 + 1.0) / 2.0;
        double[] table = new double[4 * n];
        for (int i = 0; i < table.length; i++) { // 코사인 표
            table[i] = Math.cos(2 * Math.PI * i / (4.0 * n));
        }
        for (int k = 0; k < n / 2; k++) { // 계수마다 합한다
            double s = 0;
            for (int i = 0; i < n; i++) {
                long m = ((long) (2 * i + 1 + n / 2)) * (2 * k + 1);
                s += x[i] * table[(int) (m % (4L * n))];
            }
            out[k] = 2.0 * s;
        }
        return out;
    }

    /**
     * Returns the window of a long block for a window sequence.
     * <p>
     * 윈도 순서에 대한 긴 블록의 윈도를 반환한다.
     *
     * @param seq       sequence 0, 1 or 3 / 순서 0, 1, 3
     * @param prevShape shape of the previous frame / 앞 프레임의 모양
     * @param shape     shape of this frame / 이 프레임의 모양
     * @return 2048 values / 값 2048개
     */
    static double[] longWindow(int seq, int prevShape, int shape) {
        double[] lp = rise(prevShape, 1024);
        double[] lc = rise(shape, 1024);
        double[] sp = rise(prevShape, 128);
        double[] sc = rise(shape, 128);
        double[] w = new double[2048];
        for (int n = 0; n < 2048; n++) { // 순서에 따라 왼쪽, 오른쪽 절반을 채운다
            if (n < 1024) {
                if (seq == 3) {
                    w[n] = n < 448 ? 0 : (n < 576 ? sp[n - 448] : 1);
                } else {
                    w[n] = lp[n];
                }
            } else if (seq == 1) {
                w[n] = n < 1472 ? 1 : (n < 1600 ? sc[1599 - n] : 0);
            } else {
                w[n] = lc[2047 - n];
            }
        }
        return w;
    }

    /**
     * Encodes PCM into an ADTS stream.
     * <p>
     * PCM 을 ADTS 스트림으로 부호화한다.
     *
     * @param pcm samples [channel][n] in -1..1 / 샘플
     * @return the ADTS stream / ADTS 스트림
     */
    byte[] encodeAdts(double[][] pcm) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encode(pcm);
        for (byte[] b : blocks) { // 블록마다 ADTS 헤더를 붙인다
            int len = b.length + 7;
            out.write(0xFF);
            out.write(0xF1);
            out.write((1 << 6) | (rateIndex << 2) | (channels >> 2));
            out.write(((channels & 3) << 6) | (len >> 11));
            out.write((len >> 3) & 0xFF);
            out.write(((len & 7) << 5) | 0x1F);
            out.write(0xFC);
            out.write(b, 0, b.length);
        }
        return out.toByteArray();
    }

    /**
     * Encodes PCM into raw data blocks (see {@link #blocks}).
     * <p>
     * PCM 을 원시 데이터 블록으로 부호화한다({@link #blocks} 참조).
     *
     * @param pcm samples [channel][n] in -1..1 / 샘플
     */
    void encode(double[][] pcm) {
        int n = pcm[0].length;
        int frames = (n + 1023) / 1024 + 2;
        int prevShape = 0;
        for (int f = 0; f < frames; f++) { // 프레임마다 블록을 만들어 변환하고 부호화한다
            int seq = f < sequences.length ? sequences[f] : 0;
            int shape = f < shapes.length ? shapes[f] : 0;
            double[][][] coef = new double[channels][][];
            for (int ch = 0; ch < channels; ch++) {
                double[] block = new double[2048];
                for (int i = 0; i < 2048; i++) { // 입력의 [1024 f, 1024 f + 2048) 구간을 가져온다(32768 배)
                    int idx = 1024 * f + i;
                    block[i] = idx < n ? pcm[ch][idx] * 32768.0 : 0.0;
                }
                coef[ch] = transform(block, seq, prevShape, shape);
            }
            blocks.add(writeBlock(coef, seq, shape));
            prevShape = shape;
        }
    }

    /**
     * Transforms a block: one long transform, or eight short transforms for the short sequence.
     * <p>
     * 블록을 변환한다: 긴 변환 하나 또는 짧은 순서의 짧은 변환 8개.
     *
     * @param block     2048 input samples / 입력 샘플 2048개
     * @param seq       window sequence / 윈도 순서
     * @param prevShape previous shape / 앞 모양
     * @param shape     shape / 모양
     * @return [window][coefficients] / [윈도][계수]
     */
    private double[][] transform(double[] block, int seq, int prevShape, int shape) {
        if (seq != 2) {
            double[] w = longWindow(seq, prevShape, shape);
            double[] x = new double[2048];
            for (int i = 0; i < 2048; i++) { // 윈도를 곱한다
                x[i] = block[i] * w[i];
            }
            return new double[][] { mdct(x) };
        }
        double[] first = rise(prevShape, 128);
        double[] cur = rise(shape, 128);
        double[][] res = new double[8][];
        for (int w = 0; w < 8; w++) { // 짧은 윈도 8개
            double[] x = new double[256];
            double[] left = w == 0 ? first : cur;
            for (int i = 0; i < 256; i++) { // 창 안의 샘플마다 윈도를 곱한다
                double win = i < 128 ? left[i] : cur[255 - i];
                x[i] = block[448 + 128 * w + i] * win;
            }
            res[w] = mdct(x);
        }
        return res;
    }

    /**
     * Writes one raw data block.
     * <p>
     * 원시 데이터 블록 하나를 쓴다.
     *
     * @param coef  coefficients [channel][window][k] / 계수
     * @param seq   window sequence / 윈도 순서
     * @param shape window shape / 윈도 모양
     * @return the block bytes / 블록 바이트
     */
    private byte[] writeBlock(double[][][] coef, int seq, int shape) {
        BitOut o = new BitOut();
        int[] swb = seq == 2 ? AacBands.SHORT[rateIndex] : AacBands.LONG[rateIndex];
        int[][] groups = groupsOf(seq);
        double[][][] c = coef;
        boolean ms = msMode != 0 && channels == 2;
        boolean[] msUsed = new boolean[8 * 64];
        if (ms) {
            // 오른쪽, 왼쪽을 합/차 로 바꾼다(MS 대역만). 한 번에 L=(M+S), R=(M-S) 가 되도록 M=(L+R)/2, S=(L-R)/2.
            int maxSfbGuess = swb.length - 1;
            for (int idx = 0; idx < groups.length * maxSfbGuess; idx++) { // 마스크 모양을 정한다
                msUsed[idx] = msMode == 2 || (idx % 2 == 0);
            }
        }
        // 최대 스케일팩터 대역을 구한다(모든 채널, 윈도의 0 이 아닌 마지막 선).
        int last = 0;
        for (int ch = 0; ch < channels; ch++) { // 0 이 아닌 마지막 선을 찾는다
            for (int w = 0; w < c[ch].length; w++) {
                for (int k = 0; k < c[ch][w].length; k++) {
                    if (Math.abs(c[ch][w][k]) > 1e-3) {
                        last = Math.max(last, k + 1);
                    }
                }
            }
        }
        int maxSfb = 0;
        while (maxSfb < swb.length - 1 && swb[maxSfb] < last) { // 마지막 선을 덮는 대역 수
            maxSfb++;
        }
        maxSfb = Math.min(maxSfb, seq == 2 ? 15 : 51);
        if (ms) { // MS 변환을 적용한다(선택된 대역만)
            applyMs(c, swb, groups, maxSfb, msUsed);
        }
        o.put(channels == 1 ? 0 : 1, 3);
        o.put(0, 4);
        if (channels == 2) {
            o.put(1, 1);
            writeInfo(o, seq, shape, maxSfb);
            o.put(ms ? msMode : 0, 2);
            if (msMode == 1 && ms) {
                for (int i = 0; i < groups.length * maxSfb; i++) { // 그룹과 대역마다 MS 사용 표시
                    o.put(msUsed[i] ? 1 : 0, 1);
                }
            }
        }
        for (int ch = 0; ch < channels; ch++) { // 채널마다 개별 채널 스트림을 쓴다
            writeIcs(o, c[ch], seq, shape, maxSfb, swb, groups, channels == 1);
        }
        o.put(7, 3);
        o.align();
        return o.bytes.toByteArray();
    }

    /**
     * Applies the M/S transform M = (L + R) / 2, S = (L - R) / 2 to the bands whose flag is set.
     * <p>
     * 표시가 켜진 대역에 M/S 변환 M = (L + R) / 2, S = (L - R) / 2 를 적용한다.
     *
     * @param c      coefficients / 계수
     * @param swb    band offsets / 대역 오프셋
     * @param groups window groups / 윈도 그룹
     * @param maxSfb number of bands / 대역 수
     * @param used   flags / 표시
     */
    private static void applyMs(double[][][] c, int[] swb, int[][] groups, int maxSfb, boolean[] used) {
        for (int g = 0; g < groups.length; g++) { // 그룹, 대역, 윈도마다
            for (int b = 0; b < maxSfb; b++) {
                if (!used[g * maxSfb + b]) {
                    continue;
                }
                for (int w : groups[g]) { // 그룹의 윈도마다
                    for (int k = swb[b]; k < swb[b + 1]; k++) { // 대역의 선마다 0 이 아닌 값을 찾는다
                        double l = c[0][w][k];
                        double r = c[1][w][k];
                        c[0][w][k] = (l + r) / 2.0;
                        c[1][w][k] = (l - r) / 2.0;
                    }
                }
            }
        }
    }

    /**
     * Returns the windows of every group.
     * <p>
     * 그룹마다 윈도 목록을 반환한다.
     *
     * @param seq window sequence / 윈도 순서
     * @return windows by group / 그룹별 윈도
     */
    private int[][] groupsOf(int seq) {
        if (seq != 2) {
            return new int[][] { { 0 } };
        }
        List<List<Integer>> g = new ArrayList<List<Integer>>();
        List<Integer> cur = new ArrayList<Integer>();
        cur.add(Integer.valueOf(0));
        for (int i = 0; i < 7; i++) { // 그룹 비트가 1 이면 같은 그룹, 0 이면 새 그룹
            if (((grouping >> (6 - i)) & 1) != 0) {
                cur.add(Integer.valueOf(i + 1));
            } else {
                g.add(cur);
                cur = new ArrayList<Integer>();
                cur.add(Integer.valueOf(i + 1));
            }
        }
        g.add(cur);
        int[][] r = new int[g.size()][];
        for (int i = 0; i < r.length; i++) { // 목록을 배열로 바꾼다
            r[i] = new int[g.get(i).size()];
            for (int j = 0; j < r[i].length; j++) {
                r[i][j] = g.get(i).get(j).intValue();
            }
        }
        return r;
    }

    /**
     * Writes ics_info.
     * <p>
     * ics_info 를 쓴다.
     *
     * @param o      output / 출력
     * @param seq    window sequence / 윈도 순서
     * @param shape  window shape / 윈도 모양
     * @param maxSfb max_sfb / 최대 대역
     */
    private void writeInfo(BitOut o, int seq, int shape, int maxSfb) {
        o.put(0, 1);
        o.put(seq, 2);
        o.put(shape, 1);
        if (seq == 2) {
            o.put(maxSfb, 4);
            o.put(grouping, 7);
        } else {
            o.put(maxSfb, 6);
            o.put(0, 1);
        }
    }

    /**
     * Writes an individual channel stream.
     * <p>
     * 개별 채널 스트림을 쓴다.
     *
     * @param o      output / 출력
     * @param c      coefficients [window][k] / 계수
     * @param seq    sequence / 순서
     * @param shape  shape / 모양
     * @param maxSfb bands / 대역 수
     * @param swb    band offsets / 대역 오프셋
     * @param groups groups / 그룹
     * @param single true if this is a single channel element (writes its own info) / 단일 채널 요소이면 true
     */
    private void writeIcs(BitOut o, double[][] c, int seq, int shape, int maxSfb, int[] swb, int[][] groups, boolean single) {
        double[][] spec = new double[c.length][];
        for (int w = 0; w < c.length; w++) { // 계수를 복사한다
            spec[w] = c[w].clone();
        }
        int[][] tns = null;
        if (tnsFilters > 0) { // TNS 분석 필터를 적용하고 계수를 기록한다
            tns = applyTnsAnalysis(spec, seq, swb, maxSfb);
        }
        // 이득을 고른다: 모든 대역에서 양자화 값이 maxQuant 이하가 되는 가장 작은 값.
        int gain = 0;
        int[] sfOff = new int[maxSfb + 1];
        for (int b = 0; b <= maxSfb; b++) { // 대역마다 스케일팩터 변화량
            sfOff[b] = varySf ? ((b % 3) - 1) * 2 : 0;
        }
        for (gain = 20; gain < 250; gain++) { // 이득을 올리며 최댓값을 확인한다
            if (maxQ(spec, groups, swb, maxSfb, gain, sfOff) <= maxQuant) {
                break;
            }
        }
        int[][] q = new int[c.length][1024 / (c.length == 1 ? 1 : 8)];
        quantizeAll(spec, q, groups, swb, maxSfb, gain, sfOff);
        int[][] pulseInfo = null;
        if (pulse && seq != 2) { // 가장 큰 값 몇 개에서 펄스 진폭만큼 빼고 펄스 데이터로 되돌린다
            pulseInfo = makePulses(q[0], swb, maxSfb);
        }
        o.put(gain, 8);
        if (single) {
            writeInfo(o, seq, shape, maxSfb);
        }
        // 구간 데이터: 대역마다 코드북(0 또는 11)을 정하고 같은 코드북이 이어지는 구간으로 묶는다.
        int bits = seq == 2 ? 3 : 5;
        int esc = (1 << bits) - 1;
        boolean[][] nonZero = new boolean[groups.length][maxSfb];
        for (int g = 0; g < groups.length; g++) { // 대역에 0 이 아닌 값이 있는지 표시한다
            for (int b = 0; b < maxSfb; b++) {
                for (int w : groups[g]) {
                    for (int k = swb[b]; k < swb[b + 1]; k++) { // 대역의 선마다 양자화 크기를 구한다
                        if (q[w][k] != 0) {
                            nonZero[g][b] = true;
                        }
                    }
                }
            }
        }
        for (int g = 0; g < groups.length; g++) { // 그룹마다 구간을 쓴다
            int b = 0;
            while (b < maxSfb) {
                int cb = nonZero[g][b] ? 11 : 0;
                int e = b;
                while (e < maxSfb && (nonZero[g][e] ? 11 : 0) == cb) { // 같은 코드북이 이어지는 동안 구간을 늘린다
                    e++;
                }
                o.put(cb, 4);
                int len = e - b;
                while (len >= esc) { // 이스케이프 값을 반복한다
                    o.put(esc, bits);
                    len -= esc;
                }
                o.put(len, bits);
                b = e;
            }
        }
        // 스케일팩터: 코드북이 0 이 아닌 대역의 차분을 쓴다.
        int prev = gain;
        for (int g = 0; g < groups.length; g++) { // 그룹과 대역마다
            for (int b = 0; b < maxSfb; b++) {
                if (!nonZero[g][b]) {
                    continue;
                }
                int sf = gain + sfOff[b];
                int[] code = SF[sf - prev + 60];
                o.put(code[1], code[0]);
                prev = sf;
            }
        }
        if (pulseInfo != null) {
            o.put(1, 1);
            o.put(pulseInfo[0].length - 1, 2);
            o.put(pulseInfo[1][0], 6);
            for (int i = 0; i < pulseInfo[0].length; i++) { // 펄스마다 위치 증분과 진폭
                o.put(pulseInfo[0][i], 5);
                o.put(pulseInfo[2][i], 4);
            }
        } else {
            o.put(0, 1);
        }
        if (tns != null) {
            o.put(1, 1);
            writeTns(o, tns, seq);
        } else {
            o.put(0, 1);
        }
        o.put(0, 1);
        // 스펙트럼 데이터: 그룹, 대역, 윈도, 선 순서로 코드북 11 쌍을 쓴다.
        for (int g = 0; g < groups.length; g++) {
            for (int b = 0; b < maxSfb; b++) {
                if (!nonZero[g][b]) {
                    continue;
                }
                for (int w : groups[g]) { // 그룹의 윈도마다
                    for (int k = swb[b]; k < swb[b + 1]; k += 2) { // 대역의 선 쌍마다 코드북 11 로 쓴다
                        putPair(o, q[w][k], q[w][k + 1]);
                    }
                }
            }
        }
    }

    /**
     * Returns the largest quantized magnitude for a gain.
     * <p>
     * 이득에 대한 가장 큰 양자화 크기를 반환한다.
     *
     * @param spec   spectrum / 스펙트럼
     * @param groups groups / 그룹
     * @param swb    band offsets / 대역 오프셋
     * @param maxSfb bands / 대역 수
     * @param gain   global gain / 전체 이득
     * @param sfOff  scale factor offsets / 스케일팩터 변화량
     * @return maximum / 최댓값
     */
    private static int maxQ(double[][] spec, int[][] groups, int[] swb, int maxSfb, int gain, int[] sfOff) {
        int m = 0;
        for (int g = 0; g < groups.length; g++) { // 대역마다 양자화해 최댓값을 구한다
            for (int b = 0; b < maxSfb; b++) {
                double step = Math.pow(2.0, (gain + sfOff[b] - 100) / 4.0);
                for (int w : groups[g]) { // 그룹의 윈도마다
                    for (int k = swb[b]; k < swb[b + 1]; k++) { // 대역의 선마다 최댓값을 구한다
                        m = Math.max(m, quant(spec[w][k], step));
                    }
                }
            }
        }
        return m;
    }

    /**
     * Quantizes a value to its magnitude.
     * <p>
     * 값을 양자화해 크기를 반환한다.
     *
     * @param x    value / 값
     * @param step step 2^((sf - 100) / 4) / 단계
     * @return magnitude / 크기
     */
    private static int quant(double x, double step) {
        return (int) Math.floor(Math.pow(Math.abs(x) / step, 0.75) + 0.4054);
    }

    /**
     * Quantizes all bands with signs.
     * <p>
     * 모든 대역을 부호와 함께 양자화한다.
     *
     * @param spec   spectrum / 스펙트럼
     * @param q      output [window][k] / 출력
     * @param groups groups / 그룹
     * @param swb    band offsets / 대역 오프셋
     * @param maxSfb bands / 대역 수
     * @param gain   gain / 이득
     * @param sfOff  scale factor offsets / 스케일팩터 변화량
     */
    private static void quantizeAll(double[][] spec, int[][] q, int[][] groups, int[] swb, int maxSfb, int gain, int[] sfOff) {
        for (int w = 0; w < q.length; w++) { // 모든 선을 먼저 0 으로 둔다
            java.util.Arrays.fill(q[w], 0);
        }
        for (int g = 0; g < groups.length; g++) { // 대역마다 양자화한다
            for (int b = 0; b < maxSfb; b++) {
                double step = Math.pow(2.0, (gain + sfOff[b] - 100) / 4.0);
                for (int w : groups[g]) { // 그룹의 윈도마다
                    for (int k = swb[b]; k < swb[b + 1]; k++) { // 대역의 선마다 양자화한다
                        int m = quant(spec[w][k], step);
                        q[w][k] = spec[w][k] < 0 ? -m : m;
                    }
                }
            }
        }
    }

    /**
     * Turns up to three of the largest values into pulses: each pulse amplitude is removed from the quantized value.
     * <p>
     * 가장 큰 값 최대 3개를 펄스로 바꾼다: 펄스 진폭을 양자화 값에서 뺀다.
     *
     * @param q      quantized spectrum of the long window / 긴 윈도의 양자화 스펙트럼
     * @param swb    band offsets / 대역 오프셋
     * @param maxSfb bands / 대역 수
     * @return {position increments, start band, amplitudes} / {위치 증분, 시작 대역, 진폭}
     */
    private static int[][] makePulses(int[] q, int[] swb, int maxSfb) {
        List<Integer> pos = new ArrayList<Integer>();
        for (int k = 0; k < swb[maxSfb] && pos.size() < 3; k++) { // 크기가 5 이상인 값을 위치 순으로 고른다(증분 31 이하)
            boolean near = pos.isEmpty() ? k - swb[bandOf(swb, k)] <= 31 : k - pos.get(pos.size() - 1).intValue() <= 31;
            if (Math.abs(q[k]) >= 5 && near) {
                pos.add(Integer.valueOf(k));
            }
        }
        if (pos.isEmpty()) {
            return null;
        }
        int startBand = 0;
        while (startBand + 1 < swb.length - 1 && swb[startBand + 1] <= pos.get(0).intValue()) { // 첫 펄스가 속한 대역
            startBand++;
        }
        int[] inc = new int[pos.size()];
        int[] amp = new int[pos.size()];
        int prev = swb[startBand];
        for (int i = 0; i < inc.length; i++) { // 증분과 진폭을 정하고 양자화 값에서 진폭을 뺀다
            int p = pos.get(i).intValue();
            inc[i] = p - prev;
            amp[i] = 3;
            q[p] += q[p] > 0 ? -amp[i] : amp[i];
            prev = p;
        }
        return new int[][] { inc, { startBand }, amp };
    }

    /**
     * Returns the band that contains a line.
     * <p>
     * 선이 속한 대역을 반환한다.
     *
     * @param swb band offsets / 대역 오프셋
     * @param k   line / 선
     * @return band index / 대역 번호
     */
    private static int bandOf(int[] swb, int k) {
        int b = 0;
        while (b + 1 < swb.length - 1 && swb[b + 1] <= k) { // 선이 속한 대역까지 올라간다
            b++;
        }
        return b;
    }

    /**
     * Writes a pair of values with codebook 11.
     * <p>
     * 값 한 쌍을 코드북 11 로 쓴다.
     *
     * @param o output / 출력
     * @param a first value / 첫 값
     * @param b second value / 둘째 값
     */
    private static void putPair(BitOut o, int a, int b) {
        int ma = Math.min(Math.abs(a), 16);
        int mb = Math.min(Math.abs(b), 16);
        int[] code = CB11[ma * 17 + mb];
        o.put(code[1], code[0]);
        if (a != 0) {
            o.put(a < 0 ? 1 : 0, 1);
        }
        if (b != 0) {
            o.put(b < 0 ? 1 : 0, 1);
        }
        putEscape(o, Math.abs(a));
        putEscape(o, Math.abs(b));
    }

    /**
     * Writes the escape sequence of a magnitude of 16 or more.
     * <p>
     * 크기 16 이상의 이스케이프 열을 쓴다.
     *
     * @param o output / 출력
     * @param m magnitude / 크기
     */
    private static void putEscape(BitOut o, int m) {
        if (m < 16) {
            return;
        }
        int bits = 31 - Integer.numberOfLeadingZeros(m);
        for (int i = 4; i < bits; i++) { // 비트 수만큼 1 을 쓴다
            o.put(1, 1);
        }
        o.put(0, 1);
        o.put(m & ((1 << bits) - 1), bits);
    }

    /**
     * Applies the TNS analysis (FIR) filter to the spectrum of every window and returns the quantized reflection coefficients as raw indexes.
     * <p>
     * 모든 윈도의 스펙트럼에 TNS 분석(FIR) 필터를 적용하고 양자화된 반사 계수를 원시 인덱스로 반환한다.
     *
     * @param spec   spectrum, filtered in place / 스펙트럼(제자리에서 필터됨)
     * @param seq    window sequence / 윈도 순서
     * @param swb    band offsets / 대역 오프셋
     * @param maxSfb bands / 대역 수
     * @return per window and filter: {order, direction, start band, raw...} / 윈도와 필터별 {차수, 방향, 시작 대역, 원시 값...}
     */
    private int[][] applyTnsAnalysis(double[][] spec, int seq, int[] swb, int maxSfb) {
        int windows = spec.length;
        int tnsMax = seq == 2 ? AacBands.TNS_MAX_SHORT[rateIndex] : AacBands.TNS_MAX_LONG[rateIndex];
        int top = Math.min(Math.min(swb.length - 1, tnsMax), maxSfb);
        int lengthBands = Math.max(1, top / 2);
        int[][] info = new int[windows][];
        for (int w = 0; w < windows; w++) { // 윈도마다 필터 하나(길이 = 대역의 절반)를 만든다
            int order = tnsOrder;
            int[] raw = new int[order];
            double[] k = new double[order];
            for (int i = 0; i < order; i++) { // 부호 있는 3비트 인덱스로 반사 계수를 정한다
                int idx = ((i * 5 + w) % 7) - 3;
                raw[i] = idx & 7;
                k[i] = Math.sin(idx / (idx >= 0 ? (3.5 / (Math.PI / 2)) : (4.5 / (Math.PI / 2))));
            }
            double[] a = new double[order + 1];
            a[0] = 1;
            for (int m = 1; m <= order; m++) { // 반사 계수에서 선형 예측 계수를 만든다
                double[] t = a.clone();
                for (int i = 1; i < m; i++) {
                    t[i] = a[i] + k[m - 1] * a[m - i];
                }
                t[m] = k[m - 1];
                a = t;
            }
            int bottom = Math.max(top - lengthBands, 0);
            int start = swb[bottom];
            int end = swb[top];
            double[] x = spec[w].clone();
            for (int i = start; i < end; i++) { // 위쪽으로 FIR 분석 필터: y[n] = x[n] + sum a[j] x[n-j]
                double s = x[i];
                for (int j = 1; j <= order && i - j >= start; j++) {
                    s += a[j] * x[i - j];
                }
                spec[w][i] = s;
            }
            info[w] = new int[3 + order];
            info[w][0] = order;
            info[w][1] = (swb.length - 1) - bottom;
            info[w][2] = 0;
            System.arraycopy(raw, 0, info[w], 3, order);
        }
        return info;
    }

    /**
     * Writes tns_data.
     * <p>
     * tns_data 를 쓴다.
     *
     * @param o   output / 출력
     * @param tns TNS parameters / TNS 매개변수
     * @param seq window sequence / 윈도 순서
     */
    private void writeTns(BitOut o, int[][] tns, int seq) {
        boolean shortW = seq == 2;
        for (int w = 0; w < tns.length; w++) { // 윈도마다 필터 수와 필터 매개변수를 쓴다
            o.put(1, shortW ? 1 : 2);
            o.put(0, 1);
            o.put(tns[w][1], shortW ? 4 : 6);
            o.put(tns[w][0], shortW ? 3 : 5);
            o.put(tns[w][2], 1);
            o.put(0, 1);
            for (int i = 0; i < tns[w][0]; i++) { // 3비트 계수 인덱스
                o.put(tns[w][3 + i], 3);
            }
        }
    }
}
