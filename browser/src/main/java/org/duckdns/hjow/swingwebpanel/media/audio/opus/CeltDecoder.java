package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The CELT decoder of Opus (RFC 6716 section 4.3), a translation of <code>celt/celt.c</code> of the reference decoder in its
 * floating-point configuration for the only mode that Opus uses (48 kHz output, 120-sample short blocks, up to 20 ms frames).
 * It decodes the header flags, the band energies, the allocation and the band shapes, synthesizes the frame with the inverse MDCT
 * and the overlap-add, applies the pitch post-filter and the de-emphasis, and conceals lost frames by noise substitution. One
 * instance keeps the inter-frame state of one stream of one or two channels. Not thread safe.
 * <p>
 * Opus 의 CELT 디코더(RFC 6716 4.3). 참조 디코더 <code>celt/celt.c</code> 를 부동소수점 설정으로 옮긴 것이며 Opus 가 쓰는 유일한 모드
 * (48 kHz 출력, 120 샘플 짧은 블록, 최대 20 ms 프레임)를 대상으로 한다. 헤더 플래그, 대역 에너지, 할당, 대역 모양을 디코딩하고, 역 MDCT 와
 * 겹쳐 더하기로 프레임을 합성하며, 피치 후처리 필터와 디엠퍼시스를 적용하고, 손실된 프레임은 잡음 대체로 숨긴다. 인스턴스는 채널 하나 또는
 * 둘의 스트림 하나가 프레임 사이에 유지하는 상태를 가진다. 스레드에 안전하지 않다.
 */
final class CeltDecoder {

    /**
     * Size of the history buffer per channel.
     * <p>
     * 채널당 이력 버퍼의 크기.
     */
    private static final int DECODE_BUFFER_SIZE = 2048;

    /**
     * Length of the history that the post-filter and the concealment need.
     * <p>
     * 후처리 필터와 손실 은닉이 필요로 하는 이력의 길이.
     */
    private static final int MAX_PERIOD = 1024;

    /**
     * Overlap of the MDCT windows.
     * <p>
     * MDCT 창의 겹침.
     */
    private static final int OVERLAP = 120;

    /**
     * Number of coefficients of the shortest block.
     * <p>
     * 가장 짧은 블록의 계수 수.
     */
    private static final int SHORT_MDCT = 120;

    /**
     * Number of bands.
     * <p>
     * 대역 수.
     */
    private static final int NB = CeltEnergy.NB_BANDS;

    /**
     * Smallest period of the post-filter.
     * <p>
     * 후처리 필터의 최소 주기.
     */
    private static final int COMBFILTER_MINPERIOD = 15;

    /**
     * Distribution of the allocation trim.
     * <p>
     * 할당 조정의 분포.
     */
    private static final int[] TRIM_ICDF = { 126, 124, 119, 109, 87, 41, 19, 9, 4, 2, 0 };

    /**
     * Distribution of the spreading mode.
     * <p>
     * 퍼짐 모드의 분포.
     */
    private static final int[] SPREAD_ICDF = { 25, 23, 2, 0 };

    /**
     * Distribution of the post-filter tap set.
     * <p>
     * 후처리 필터 탭 집합의 분포.
     */
    private static final int[] TAPSET_ICDF = { 2, 1, 0 };

    /**
     * Time/frequency selection table.
     * <p>
     * 시간/주파수 선택 표.
     */
    private static final int[][] TF_SELECT_TABLE = { { 0, -1, 0, -1, 0, -1, 0, -1 }, { 0, -1, 0, -2, 1, 0, 1, -1 }, { 0, -2, 0, -3, 2, 0, 1, -1 },
            { 0, -2, 0, -3, 3, 0, 1, -1 } };

    /**
     * Post-filter taps of the three tap sets.
     * <p>
     * 세 탭 집합의 후처리 필터 탭.
     */
    private static final float[][] COMB_GAINS = { { 0.3066406250f, 0.2170410156f, 0.1296386719f }, { 0.4638671875f, 0.2680664062f, 0.f },
            { 0.7998046875f, 0.1000976562f, 0.f } };

    /**
     * De-emphasis coefficient.
     * <p>
     * 디엠퍼시스 계수.
     */
    private static final float PREEMPH0 = 0.85000610f;

    /**
     * Number of output channels (1 or 2).
     * <p>
     * 출력 채널 수(1 또는 2).
     */
    private final int channels;

    /**
     * True for the hybrid folding of RFC 6716 instead of RFC 8251.
     * <p>
     * RFC 8251 대신 RFC 6716 의 하이브리드 접기를 쓰면 true.
     */
    private boolean legacyFolding;

    /**
     * Number of channels in the stream being decoded (1 or 2).
     * <p>
     * 디코딩하는 스트림의 채널 수(1 또는 2).
     */
    private int streamChannels;

    /**
     * First coded band.
     * <p>
     * 부호화된 첫 대역.
     */
    private int start;

    /**
     * One past the last coded band.
     * <p>
     * 부호화된 마지막 대역의 다음.
     */
    private int end;

    /**
     * Noise seed, and the final range of the last frame.
     * <p>
     * 잡음 시드이자 마지막 프레임의 최종 범위.
     */
    private int rng;

    /**
     * Number of consecutive lost frames.
     * <p>
     * 연속으로 손실된 프레임 수.
     */
    private int lossCount;

    /**
     * Post-filter period of the current frame.
     * <p>
     * 현재 프레임의 후처리 필터 주기.
     */
    private int postfilterPeriod;

    /**
     * Post-filter period of the previous frame.
     * <p>
     * 이전 프레임의 후처리 필터 주기.
     */
    private int postfilterPeriodOld;

    /**
     * Post-filter gain of the current frame.
     * <p>
     * 현재 프레임의 후처리 필터 이득.
     */
    private float postfilterGain;

    /**
     * Post-filter gain of the previous frame.
     * <p>
     * 이전 프레임의 후처리 필터 이득.
     */
    private float postfilterGainOld;

    /**
     * Post-filter tap set of the current frame.
     * <p>
     * 현재 프레임의 후처리 필터 탭 집합.
     */
    private int postfilterTapset;

    /**
     * Post-filter tap set of the previous frame.
     * <p>
     * 이전 프레임의 후처리 필터 탭 집합.
     */
    private int postfilterTapsetOld;

    /**
     * De-emphasis filter memory per channel.
     * <p>
     * 채널별 디엠퍼시스 필터 메모리.
     */
    private final float[] preemphMem = new float[2];

    /**
     * Synthesis history and overlap memory per channel.
     * <p>
     * 채널별 합성 이력과 겹침 메모리.
     */
    private final float[][] decodeMem;

    /**
     * Band energies of the previous frame (log scale, 2 x 21).
     * <p>
     * 이전 프레임의 대역 에너지(로그 단위, 2 x 21).
     */
    private final float[] oldBandE = new float[2 * NB];

    /**
     * Band energies of the last non-transient frame.
     * <p>
     * 마지막 비과도 프레임의 대역 에너지.
     */
    private final float[] oldLogE = new float[2 * NB];

    /**
     * Band energies of the non-transient frame before that.
     * <p>
     * 그 이전 비과도 프레임의 대역 에너지.
     */
    private final float[] oldLogE2 = new float[2 * NB];

    /**
     * Slowly rising background noise estimate.
     * <p>
     * 천천히 상승하는 배경 잡음 추정.
     */
    private final float[] backgroundLogE = new float[2 * NB];

    /**
     * The inverse MDCT.
     * <p>
     * 역 MDCT 변환기.
     */
    private final CeltMdct mdct = new CeltMdct(2 * SHORT_MDCT * 8, 3);

    /**
     * The bit allocator.
     * <p>
     * 비트 할당기.
     */
    private final CeltRate rate = new CeltRate();

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param channels number of output channels, 1 or 2 / 출력 채널 수(1 또는 2)
     */
    CeltDecoder(int channels) {
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("channels");
        }
        this.channels = channels;
        this.streamChannels = channels;
        this.decodeMem = new float[channels][DECODE_BUFFER_SIZE + OVERLAP];
        this.start = 0;
        this.end = NB;
        reset();
    }

    /**
     * Clears the decoder state (the equivalent of a new decoder).
     * <p>
     * 디코더 상태를 지운다(새 디코더와 같다).
     */
    void reset() {
        rng = 0;
        lossCount = 0;
        postfilterPeriod = 0;
        postfilterPeriodOld = 0;
        postfilterGain = 0;
        postfilterGainOld = 0;
        postfilterTapset = 0;
        postfilterTapsetOld = 0;
        java.util.Arrays.fill(preemphMem, 0);
        // 채널마다 이력 버퍼를 지운다.
        for (int c = 0; c < channels; c++) {
            java.util.Arrays.fill(decodeMem[c], 0);
        }
        java.util.Arrays.fill(oldBandE, 0);
        java.util.Arrays.fill(backgroundLogE, 0);
        // 이전 프레임 에너지는 매우 작은 값으로 초기화한다.
        for (int i = 0; i < 2 * NB; i++) {
            oldLogE[i] = -28.f;
            oldLogE2[i] = -28.f;
        }
    }

    /**
     * Selects the folding of hybrid frames: true for the behavior of the RFC 6716 reference decoder, false (the default) for the
     * corrected behavior of RFC 8251 section 9, which modern decoders implement.
     * <p>
     * 하이브리드 프레임의 접기 방식을 고른다: true 는 RFC 6716 참조 디코더의 동작, false(기본)는 현대 디코더가 구현한 RFC 8251 9장의 수정된 동작.
     *
     * @param legacy true for the RFC 6716 behavior / RFC 6716 동작이면 true
     */
    void setLegacyFolding(boolean legacy) {
        legacyFolding = legacy;
    }

    /**
     * Sets the first coded band (0 for CELT-only frames, 17 for hybrid frames).
     * <p>
     * 부호화된 첫 대역을 정한다(CELT 전용 프레임은 0, 하이브리드 프레임은 17).
     *
     * @param value the band / 대역
     */
    void setStartBand(int value) {
        start = value;
    }

    /**
     * Sets the end band, which follows from the audio bandwidth.
     * <p>
     * 오디오 대역폭에 따라 정해지는 끝 대역을 정한다.
     *
     * @param value one past the last band / 마지막 대역의 다음
     */
    void setEndBand(int value) {
        end = value;
    }

    /**
     * Sets the number of channels coded in the stream.
     * <p>
     * 스트림에 부호화된 채널 수를 정한다.
     *
     * @param value 1 or 2 / 1 또는 2
     */
    void setStreamChannels(int value) {
        streamChannels = value;
    }

    /**
     * Returns the final range of the last decoded frame.
     * <p>
     * 마지막으로 디코딩한 프레임의 최종 범위를 반환한다.
     *
     * @return the range coder state / 범위 디코더 상태
     */
    int getFinalRange() {
        return rng;
    }

    /**
     * Returns the window of the overlap (used by the Opus layer for cross-fades).
     * <p>
     * 겹침 창을 반환한다(Opus 계층이 교차 페이드에 쓴다).
     *
     * @return the 120-sample window / 120 샘플 창
     */
    static float[] getWindow() {
        return CeltTables.WINDOW;
    }

    /**
     * Decodes the time/frequency resolution changes of the bands.
     * <p>
     * 대역의 시간/주파수 해상도 변경을 디코딩한다.
     *
     * @param isTransient true for a transient frame / 과도 프레임이면 true
     * @param tfRes       receives the changes per band / 대역마다 변경을 받는 배열
     * @param lm          log2 of the frame size / 프레임 크기의 log2
     * @param dec         the range decoder / 범위 디코더
     */
    private void tfDecode(boolean isTransient, int[] tfRes, int lm, OpusRangeDecoder dec) {
        int budget = dec.getStorage() * 8;
        int tell = dec.tell();
        int logp = isTransient ? 2 : 4;
        int tfSelectRsv = lm > 0 && tell + logp + 1 <= budget ? 1 : 0;
        budget -= tfSelectRsv;
        int tfChanged = 0;
        int curr = 0;
        // 대역마다 변경 플래그를 읽는다.
        for (int i = start; i < end; i++) {
            if (tell + logp <= budget) {
                curr ^= dec.bitLogp(logp);
                tell = dec.tell();
                tfChanged |= curr;
            }
            tfRes[i] = curr;
            logp = isTransient ? 4 : 5;
        }
        int tfSelect = 0;
        int t = isTransient ? 4 : 0;
        if (tfSelectRsv != 0 && TF_SELECT_TABLE[lm][t + 0 + tfChanged] != TF_SELECT_TABLE[lm][t + 2 + tfChanged]) {
            tfSelect = dec.bitLogp(1);
        }
        // 선택된 표 항목으로 변경량을 확정한다.
        for (int i = start; i < end; i++) {
            tfRes[i] = TF_SELECT_TABLE[lm][t + 2 * tfSelect + tfRes[i]];
        }
    }

    /**
     * Computes the allocation caps of the bands.
     * <p>
     * 대역의 할당 상한을 계산한다.
     *
     * @param cap receives the caps / 상한을 받는 배열
     * @param lm  log2 of the frame size / 프레임 크기의 log2
     * @param c   number of channels / 채널 수
     */
    private static void initCaps(int[] cap, int lm, int c) {
        // 대역마다 표의 값과 대역 너비로 상한을 계산한다.
        for (int i = 0; i < NB; i++) {
            int n = (CeltTables.EBANDS[i + 1] - CeltTables.EBANDS[i]) << lm;
            cap[i] = (CeltTables.CACHE_CAPS[NB * (2 * lm + c - 1) + i] + 64) * c * n >> 2;
        }
    }

    /**
     * Pitch post-filter (an IIR comb filter that cross-fades between two parameter sets over the overlap).
     * <p>
     * 피치 후처리 필터(겹침 구간에서 두 매개변수 집합 사이를 교차 페이드하는 IIR 빗살 필터).
     *
     * @param y       output / 출력
     * @param yo      start of the output / 출력의 시작
     * @param x       input / 입력
     * @param xo      start of the input / 입력의 시작
     * @param t0      previous period / 이전 주기
     * @param t1      new period / 새 주기
     * @param n       number of samples / 샘플 수
     * @param g0      previous gain / 이전 이득
     * @param g1      new gain / 새 이득
     * @param tapset0 previous tap set / 이전 탭 집합
     * @param tapset1 new tap set / 새 탭 집합
     * @param window  the overlap window / 겹침 창
     * @param overlap number of cross-faded samples / 교차 페이드하는 샘플 수
     */
    private static void combFilter(float[] y, int yo, float[] x, int xo, int t0, int t1, int n, float g0, float g1, int tapset0, int tapset1,
            float[] window, int overlap) {
        float g00 = g0 * COMB_GAINS[tapset0][0];
        float g01 = g0 * COMB_GAINS[tapset0][1];
        float g02 = g0 * COMB_GAINS[tapset0][2];
        float g10 = g1 * COMB_GAINS[tapset1][0];
        float g11 = g1 * COMB_GAINS[tapset1][1];
        float g12 = g1 * COMB_GAINS[tapset1][2];
        // 겹침 구간에서는 두 매개변수 집합을 창으로 섞는다.
        for (int i = 0; i < overlap; i++) {
            float f = window[i] * window[i];
            int p = xo + i;
            y[yo + i] = x[p] + ((1.0f - f) * g00) * x[p - t0] + ((1.0f - f) * g01) * x[p - t0 - 1] + ((1.0f - f) * g01) * x[p - t0 + 1]
                    + ((1.0f - f) * g02) * x[p - t0 - 2] + ((1.0f - f) * g02) * x[p - t0 + 2] + (f * g10) * x[p - t1] + (f * g11) * x[p - t1 - 1]
                    + (f * g11) * x[p - t1 + 1] + (f * g12) * x[p - t1 - 2] + (f * g12) * x[p - t1 + 2];
        }
        // 그 뒤에는 새 매개변수만 쓴다.
        for (int i = overlap; i < n; i++) {
            int p = xo + i;
            y[yo + i] = x[p] + g10 * x[p - t1] + g11 * x[p - t1 - 1] + g11 * x[p - t1 + 1] + g12 * x[p - t1 - 2] + g12 * x[p - t1 + 2];
        }
    }

    /**
     * Inverse MDCT of all blocks of a frame with overlap-add into the synthesis buffers.
     * <p>
     * 프레임의 모든 블록을 역 MDCT 하여 합성 버퍼에 겹쳐 더한다.
     *
     * @param shortBlocks number of short blocks, 0 for a long block / 짧은 블록 수. 긴 블록이면 0
     * @param x           coefficients (C blocks of N) / 계수(N 개씩 C 블록)
     * @param ch          number of channels to synthesize / 합성할 채널 수
     * @param lm          log2 of the frame size / 프레임 크기의 log2
     */
    private void computeInvMdcts(int shortBlocks, float[] x, int ch, int lm) {
        int n = SHORT_MDCT << lm;
        float[] buf = new float[n + OVERLAP];
        // 채널마다 합성한다.
        for (int c = 0; c < ch; c++) {
            int n2 = n;
            int b = 1;
            if (shortBlocks != 0) {
                n2 = SHORT_MDCT;
                b = shortBlocks;
            }
            java.util.Arrays.fill(buf, 0, OVERLAP, 0.f);
            // 짧은 블록이면 블록마다 역 MDCT 한다.
            for (int blk = 0; blk < b; blk++) {
                mdct.backward(x, blk + c * n2 * b, buf, n2 * blk, CeltTables.WINDOW, OVERLAP, shortBlocks != 0 ? 3 : 3 - lm, b);
            }
            float[] mem = decodeMem[c];
            int out = DECODE_BUFFER_SIZE - n;
            int ov = DECODE_BUFFER_SIZE;
            // 겹침 구간은 이전 프레임의 꼬리와 더하고 나머지는 그대로 쓴다.
            for (int j = 0; j < OVERLAP; j++) {
                mem[out + j] = buf[j] + mem[ov + j];
            }
            // j 를 OVERLAP 부터 n 까지 늘려 가며 항목마다 처리한다.
            for (int j = OVERLAP; j < n; j++) {
                mem[out + j] = buf[j];
            }
            // 다음 프레임에 더할 꼬리를 보관한다.
            for (int j = 0; j < OVERLAP; j++) {
                mem[ov + j] = buf[n + j];
            }
        }
    }

    /**
     * De-emphasis filter and conversion to the output scale.
     * <p>
     * 디엠퍼시스 필터와 출력 크기로의 변환.
     *
     * @param pcm    output (interleaved) / 출력(인터리브)
     * @param pcmOff start of the output / 출력의 시작
     * @param n      number of samples per channel / 채널당 샘플 수
     */
    private void deemphasis(float[] pcm, int pcmOff, int n) {
        // 채널마다 처리한다.
        for (int c = 0; c < channels; c++) {
            float m = preemphMem[c];
            float[] mem = decodeMem[c];
            int xi = DECODE_BUFFER_SIZE - n;
            int yi = pcmOff + c;
            // 샘플마다 1차 IIR 필터를 적용한다.
            for (int j = 0; j < n; j++) {
                float tmp = mem[xi] + m;
                m = PREEMPH0 * tmp - 0.f * mem[xi];
                xi++;
                pcm[yi] = tmp * (1 / 32768.f);
                yi += channels;
            }
            preemphMem[c] = m;
        }
    }

    /**
     * Fills a lost frame with noise shaped like the energies of the previous frames (the noise substitution of the reference).
     * <p>
     * 손실된 프레임을 이전 프레임의 에너지를 따르는 잡음으로 채운다(참조 디코더의 잡음 대체).
     *
     * @param pcm    output / 출력
     * @param pcmOff start of the output / 출력의 시작
     * @param n      number of samples / 샘플 수
     * @param lm     log2 of the frame size / 프레임 크기의 log2
     */
    private void decodeLost(float[] pcm, int pcmOff, int n, int lm) {
        int cc = channels;
        float[] freq = new float[cc * n];
        float[] xx = new float[cc * n];
        float[] bandE = new float[NB * cc];
        int m = 1 << lm;
        int effEnd = Math.min(end, NB);
        // 이력 버퍼를 한 프레임만큼 앞으로 민다.
        for (int c = 0; c < cc; c++) {
            System.arraycopy(decodeMem[c], n, decodeMem[c], 0, DECODE_BUFFER_SIZE - n);
        }
        // 오래 손실되었으면 배경 잡음 수준으로, 아니면 에너지를 줄여 가며 잡음을 만든다.
        if (lossCount >= 5) {
            CeltEnergy.log2Amp(start, end, bandE, backgroundLogE, cc);
        } else {
            float decay = lossCount == 0 ? 1.5f : .5f;
            // c 를 0 부터 cc 까지 늘려 가며 항목마다 처리한다.
            for (int c = 0; c < cc; c++) {
                // i 를 start 부터 end 까지 늘려 가며 항목마다 처리한다.
                for (int i = start; i < end; i++) {
                    oldBandE[c * NB + i] -= decay;
                }
            }
            CeltEnergy.log2Amp(start, end, bandE, oldBandE, cc);
        }
        int seed = rng;
        // 채널과 대역마다 임의의 벡터를 만들어 정규화한다.
        for (int c = 0; c < cc; c++) {
            for (int i = start; i < NB; i++) {
                int boffs = n * c + (CeltTables.EBANDS[i] << lm);
                int blen = (CeltTables.EBANDS[i + 1] - CeltTables.EBANDS[i]) << lm;
                // j 를 0 부터 blen 까지 늘려 가며 항목마다 처리한다.
                for (int j = 0; j < blen; j++) {
                    seed = CeltMath.lcgRand(seed);
                    xx[boffs + j] = (float) (seed >> 20);
                }
                CeltPvq.renormaliseVector(xx, boffs, blen, 1.0f);
            }
        }
        rng = seed;
        CeltBands.denormaliseBands(xx, freq, bandE, NB, cc, m);
        // 부호화되지 않는 낮은 주파수와 높은 주파수를 0 으로 둔다.
        for (int c = 0; c < cc; c++) {
            for (int i = 0; i < CeltTables.EBANDS[start] << lm; i++) {
                freq[c * n + i] = 0;
            }
            // i 를 CeltTables.EBANDS[effEnd] << lm 부터 n 까지 늘려 가며 항목마다 처리한다.
            for (int i = CeltTables.EBANDS[effEnd] << lm; i < n; i++) {
                freq[c * n + i] = 0;
            }
        }
        computeInvMdcts(0, freq, cc, lm);
        deemphasis(pcm, pcmOff, n);
        lossCount++;
    }

    /**
     * Decodes one frame, or conceals it when <code>data</code> is null or at most one byte long.
     * <p>
     * 프레임 하나를 디코딩한다. <code>data</code> 가 null 이거나 1 바이트 이하이면 손실로 보고 은닉한다.
     *
     * @param data      the frame (null for a lost frame) / 프레임(손실이면 null)
     * @param off       start of the frame / 프레임의 시작
     * @param len       length of the frame in bytes / 프레임의 바이트 수
     * @param pcm       output, interleaved floats in the range -1..1 / 출력(-1..1 범위의 인터리브 float)
     * @param pcmOff    start of the output / 출력의 시작
     * @param frameSize number of samples per channel to produce (120, 240, 480 or 960) / 만들 채널당 샘플 수(120, 240, 480, 960)
     * @param decIn     the range decoder shared with SILK (hybrid), or null to create one / SILK 와 공유하는 범위 디코더(하이브리드). null 이면 새로 만든다
     * @return the number of samples per channel, or a negative value if the frame is invalid / 채널당 샘플 수. 프레임이 올바르지 않으면 음수
     */
    int decode(byte[] data, int off, int len, float[] pcm, int pcmOff, int frameSize, OpusRangeDecoder decIn) {
        int cc = channels;
        int c = streamChannels;
        int lm;
        // 프레임 크기에서 블록 크기 단계를 찾는다.
        for (lm = 0; lm <= 3; lm++) {
            if ((SHORT_MDCT << lm) == frameSize) {
                break;
            }
        }
        if (lm > 3) {
            return -1;
        }
        int m = 1 << lm;
        if (len < 0 || len > 1275) {
            return -1;
        }
        int n = m * SHORT_MDCT;
        int effEnd = Math.min(end, NB);
        if (data == null || len <= 1) {
            decodeLost(pcm, pcmOff, n, lm);
            return frameSize;
        }
        OpusRangeDecoder dec = decIn != null ? decIn : new OpusRangeDecoder(data, off, len);
        float[] freq = new float[Math.max(cc, c) * n];
        float[] x = new float[c * n];
        float[] bandE = new float[NB * c];
        int[] eb = CeltTables.EBANDS;
        // 모노 스트림이면 두 채널 에너지 중 큰 쪽을 쓴다.
        if (c == 1) {
            for (int i = 0; i < NB; i++) {
                oldBandE[i] = Math.max(oldBandE[i], oldBandE[NB + i]);
            }
        }
        int totalBits = len * 8;
        int tell = dec.tell();
        boolean silence;
        // 데이터가 이미 소진되었거나 첫 비트가 무음을 알리면 무음 프레임이다.
        if (tell >= totalBits) {
            silence = true;
        } else if (tell == 1) {
            silence = dec.bitLogp(15) != 0;
        } else {
            silence = false;
        }
        if (silence) {
            tell = len * 8;
            dec.skipTo(tell);
        }
        float pfGain = 0;
        int pfPitch = 0;
        int pfTapset = 0;
        // 후처리 필터 매개변수를 읽는다.
        if (start == 0 && tell + 16 <= totalBits) {
            if (dec.bitLogp(1) != 0) {
                int octave = (int) dec.readUint(6);
                pfPitch = (16 << octave) + (int) dec.readBits(4 + octave) - 1;
                int qg = (int) dec.readBits(3);
                if (dec.tell() + 2 <= totalBits) {
                    pfTapset = dec.icdf(TAPSET_ICDF, 2);
                }
                pfGain = 0.09375f * (qg + 1);
            }
            tell = dec.tell();
        }
        boolean isTransient;
        if (lm > 0 && tell + 3 <= totalBits) {
            isTransient = dec.bitLogp(3) != 0;
            tell = dec.tell();
        } else {
            isTransient = false;
        }
        int shortBlocks = isTransient ? m : 0;
        boolean intraEner = tell + 3 <= totalBits && dec.bitLogp(3) != 0;
        CeltEnergy.unquantCoarse(start, end, oldBandE, intraEner, dec, c, lm);
        int[] tfRes = new int[NB];
        tfDecode(isTransient, tfRes, lm, dec);
        tell = dec.tell();
        int spreadDecision = 2;
        if (tell + 4 <= totalBits) {
            spreadDecision = dec.icdf(SPREAD_ICDF, 5);
        }
        int[] pulses = new int[NB];
        int[] cap = new int[NB];
        int[] offsets = new int[NB];
        int[] finePriority = new int[NB];
        int[] fineQuant = new int[NB];
        initCaps(cap, lm, c);
        int dynallocLogp = 6;
        totalBits <<= CeltRate.BITRES;
        tell = dec.tellFrac();
        // 대역마다 증폭(boost) 플래그를 읽는다.
        for (int i = start; i < end; i++) {
            int width = c * (eb[i + 1] - eb[i]) << lm;
            int quanta = Math.min(width << CeltRate.BITRES, Math.max(6 << CeltRate.BITRES, width));
            int dynallocLoopLogp = dynallocLogp;
            int boost = 0;
            // 증폭 플래그가 계속 1 인 동안 증폭량을 늘린다.
            while (tell + (dynallocLoopLogp << CeltRate.BITRES) < totalBits && boost < cap[i]) {
                int flag = dec.bitLogp(dynallocLoopLogp);
                tell = dec.tellFrac();
                if (flag == 0) {
                    break;
                }
                boost += quanta;
                totalBits -= quanta;
                dynallocLoopLogp = 1;
            }
            offsets[i] = boost;
            if (boost > 0) {
                dynallocLogp = Math.max(2, dynallocLogp - 1);
            }
        }
        int allocTrim = tell + (6 << CeltRate.BITRES) <= totalBits ? dec.icdf(TRIM_ICDF, 7) : 5;
        int bits = ((len * 8) << CeltRate.BITRES) - dec.tellFrac() - 1;
        int antiCollapseRsv = isTransient && lm >= 2 && bits >= ((lm + 2) << CeltRate.BITRES) ? (1 << CeltRate.BITRES) : 0;
        bits -= antiCollapseRsv;
        int codedBands = rate.computeAllocation(start, end, offsets, cap, allocTrim, bits, pulses, fineQuant, finePriority, c, lm, dec);
        CeltEnergy.unquantFine(start, end, oldBandE, fineQuant, dec, c);
        int[] collapseMasks = new int[c * NB];
        CeltBands bands = new CeltBands(dec, rng, cc == 1, legacyFolding);
        bands.quantAllBands(start, end, x, c, collapseMasks, pulses, shortBlocks, spreadDecision, rate.dualStereo, rate.intensity, tfRes,
                len * (8 << CeltRate.BITRES) - antiCollapseRsv, rate.balance, lm, codedBands);
        rng = bands.getSeed();
        boolean antiCollapseOn = false;
        if (antiCollapseRsv > 0) {
            antiCollapseOn = dec.readBits(1) != 0;
        }
        CeltEnergy.unquantFinalise(start, end, oldBandE, fineQuant, finePriority, len * 8 - dec.tell(), dec, c);
        if (antiCollapseOn) {
            CeltBands.antiCollapse(x, collapseMasks, lm, c, n, start, end, oldBandE, oldLogE, oldLogE2, pulses, rng);
        }
        CeltEnergy.log2Amp(start, end, bandE, oldBandE, c);
        // 무음 프레임이면 모든 대역 진폭을 0 으로 두고 에너지 이력을 최소로 낮춘다.
        if (silence) {
            for (int i = 0; i < c * NB; i++) {
                bandE[i] = 0;
                oldBandE[i] = -28.f;
            }
        }
        CeltBands.denormaliseBands(x, freq, bandE, effEnd, c, m);
        // 이력 버퍼를 한 프레임만큼 앞으로 민다.
        for (int ch = 0; ch < cc; ch++) {
            System.arraycopy(decodeMem[ch], n, decodeMem[ch], 0, DECODE_BUFFER_SIZE - n);
        }
        // 부호화되지 않은 낮은 대역과 높은 대역의 계수를 0 으로 둔다.
        for (int ch = 0; ch < c; ch++) {
            for (int i = 0; i < m * eb[start]; i++) {
                freq[ch * n + i] = 0;
            }
            // i 를 m * eb[effEnd] 부터 n 까지 늘려 가며 항목마다 처리한다.
            for (int i = m * eb[effEnd]; i < n; i++) {
                freq[ch * n + i] = 0;
            }
        }
        // 스트림과 출력의 채널 수가 다르면 복제하거나 평균한다.
        if (cc == 2 && c == 1) {
            System.arraycopy(freq, 0, freq, n, n);
        }
        if (cc == 1 && c == 2) {
            // i 를 0 부터 n 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < n; i++) {
                freq[i] = .5f * (freq[i] + freq[n + i]);
            }
        }
        computeInvMdcts(shortBlocks, freq, cc, lm);
        postfilterPeriod = Math.max(postfilterPeriod, COMBFILTER_MINPERIOD);
        postfilterPeriodOld = Math.max(postfilterPeriodOld, COMBFILTER_MINPERIOD);
        // 채널마다 피치 후처리 필터를 적용한다.
        for (int ch = 0; ch < cc; ch++) {
            float[] mem = decodeMem[ch];
            int syn = DECODE_BUFFER_SIZE - n;
            combFilter(mem, syn, mem, syn, postfilterPeriodOld, postfilterPeriod, SHORT_MDCT, postfilterGainOld, postfilterGain, postfilterTapsetOld,
                    postfilterTapset, CeltTables.WINDOW, OVERLAP);
            if (lm != 0) {
                combFilter(mem, syn + SHORT_MDCT, mem, syn + SHORT_MDCT, postfilterPeriod, pfPitch, n - SHORT_MDCT, postfilterGain, pfGain,
                        postfilterTapset, pfTapset, CeltTables.WINDOW, OVERLAP);
            }
        }
        postfilterPeriodOld = postfilterPeriod;
        postfilterGainOld = postfilterGain;
        postfilterTapsetOld = postfilterTapset;
        postfilterPeriod = pfPitch;
        postfilterGain = pfGain;
        postfilterTapset = pfTapset;
        if (lm != 0) {
            postfilterPeriodOld = postfilterPeriod;
            postfilterGainOld = postfilterGain;
            postfilterTapsetOld = postfilterTapset;
        }
        if (c == 1) {
            System.arraycopy(oldBandE, 0, oldBandE, NB, NB);
        }
        // 에너지 이력을 갱신한다(과도 프레임은 이력의 최솟값만 낮춘다).
        if (!isTransient) {
            System.arraycopy(oldLogE, 0, oldLogE2, 0, 2 * NB);
            System.arraycopy(oldBandE, 0, oldLogE, 0, 2 * NB);
            // i 를 0 부터 2 * NB 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < 2 * NB; i++) {
                backgroundLogE[i] = Math.min(backgroundLogE[i] + m * 0.001f, oldBandE[i]);
            }
        } else {
            // i 를 0 부터 2 * NB 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < 2 * NB; i++) {
                oldLogE[i] = Math.min(oldLogE[i], oldBandE[i]);
            }
        }
        // 부호화되지 않은 대역의 이력을 지운다.
        for (int ch = 0; ch < 2; ch++) {
            for (int i = 0; i < start; i++) {
                oldBandE[ch * NB + i] = 0;
                oldLogE[ch * NB + i] = -28.f;
                oldLogE2[ch * NB + i] = -28.f;
            }
            // i 를 end 부터 NB 까지 늘려 가며 항목마다 처리한다.
            for (int i = end; i < NB; i++) {
                oldBandE[ch * NB + i] = 0;
                oldLogE[ch * NB + i] = -28.f;
                oldLogE2[ch * NB + i] = -28.f;
            }
        }
        rng = dec.getFinalRange();
        deemphasis(pcm, pcmOff, n);
        lossCount = 0;
        if (dec.tell() > 8 * len) {
            return -2;
        }
        return frameSize;
    }
}
