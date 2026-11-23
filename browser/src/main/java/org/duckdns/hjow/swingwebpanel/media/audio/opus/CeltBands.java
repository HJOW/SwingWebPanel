package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Decoding of the band shapes of a CELT frame (RFC 6716 sections 4.3.4 and 4.3.5, <code>celt/bands.c</code>): the recursive
 * splitting of bands with the stereo angle, the time/frequency resolution changes, the folding of lower bands into bands that
 * got no pulses, the spreading, the anti-collapse noise and the denormalization to the MDCT coefficients, in the floating-point
 * configuration of the reference decoder. An instance carries the range decoder and the noise seed of one frame.
 * <p>
 * CELT 프레임의 대역 모양 디코딩(RFC 6716 4.3.4, 4.3.5, <code>celt/bands.c</code>): 스테레오 각도에 따른 대역의 재귀 분할, 시간/주파수
 * 해상도 변경, 펄스가 없는 대역에 낮은 대역을 접어 넣기, 퍼짐, 붕괴 방지 잡음, MDCT 계수로의 비정규화. 참조 디코더의 부동소수점 설정을
 * 따른다. 인스턴스는 한 프레임의 범위 디코더와 잡음 시드를 가진다.
 */
final class CeltBands {

    /**
     * Fractional bits of the bit counts.
     * <p>
     * 비트 수의 소수 비트.
     */
    private static final int BITRES = CeltRate.BITRES;

    /**
     * Bit of the stereo angle offset of a two-dimensional stereo band.
     * <p>
     * 2차원 스테레오 대역의 각도 오프셋 상수.
     */
    private static final int QTHETA_OFFSET_TWOPHASE = 16;

    /**
     * Stereo angle offset constant.
     * <p>
     * 스테레오 각도 오프셋 상수.
     */
    private static final int QTHETA_OFFSET = 4;

    /**
     * Order of the Hadamard permutation of the interleaved short blocks.
     * <p>
     * 인터리브된 짧은 블록의 아다마르 순열 순서.
     */
    private static final int[] ORDERY_TABLE = { 1, 0, 3, 0, 2, 1, 7, 0, 4, 3, 6, 1, 5, 2, 15, 0, 8, 7, 12, 3, 11, 4, 14, 1, 9, 6, 13, 2, 10, 5 };

    /**
     * Fractional parts of 2^(i/8) in Q14 for the number of angle steps.
     * <p>
     * 각도 단계 수를 위한 2^(i/8) 의 Q14 값.
     */
    private static final int[] EXP2_TABLE8 = { 16384, 17866, 19483, 21247, 23170, 25267, 27554, 30048 };

    /**
     * Interleaving of the fill bits when short blocks are recombined.
     * <p>
     * 짧은 블록을 합칠 때 채움 비트의 인터리브 표.
     */
    private static final int[] BIT_INTERLEAVE_TABLE = { 0, 1, 1, 1, 2, 3, 3, 3, 2, 3, 3, 3, 2, 3, 3, 3 };

    /**
     * Deinterleaving of the collapse bits when short blocks are recombined.
     * <p>
     * 짧은 블록을 합칠 때 붕괴 비트의 디인터리브 표.
     */
    private static final int[] BIT_DEINTERLEAVE_TABLE = { 0x00, 0x03, 0x0C, 0x0F, 0x30, 0x33, 0x3C, 0x3F, 0xC0, 0xC3, 0xCC, 0xCF, 0xF0, 0xF3, 0xFC, 0xFF };

    /**
     * The range decoder.
     * <p>
     * 범위 디코더.
     */
    private final OpusRangeDecoder rc;

    /**
     * Noise seed.
     * <p>
     * 잡음 시드.
     */
    private int seed;

    /**
     * Bits (1/8 units) that the remaining bands may still use.
     * <p>
     * 남은 대역이 아직 쓸 수 있는 비트(1/8 단위).
     */
    private int remaining;

    /**
     * True to ignore the 180 degree phase inversion of the stereo bands (set when the output is mono, RFC 8251 section 10).
     * <p>
     * 스테레오 대역의 180도 위상 반전을 무시하려면 true(출력이 모노일 때 설정. RFC 8251 10장).
     */
    private final boolean disableInv;

    /**
     * True to fold hybrid frames as RFC 6716 does, with noise for the second band when the first one is too narrow; false for the folding
     * of RFC 8251 section 9.
     * <p>
     * 하이브리드 프레임을 RFC 6716 처럼 접으려면 true(첫 대역이 너무 좁으면 둘째 대역에 잡음을 쓴다). 거짓이면 RFC 8251 9장의 접기.
     */
    private final boolean legacyFolding;

    /**
     * Creates the band decoder of one frame.
     * <p>
     * 한 프레임의 대역 디코더를 만든다.
     *
     * @param rc            the range decoder / 범위 디코더
     * @param seed          the initial noise seed / 초기 잡음 시드
     * @param disableInv    true to ignore the phase inversion of intensity stereo / 강도 스테레오의 위상 반전을 무시하려면 true
     * @param legacyFolding true for the RFC 6716 folding of hybrid frames / 하이브리드 프레임을 RFC 6716 방식으로 접으려면 true
     */
    CeltBands(OpusRangeDecoder rc, int seed, boolean disableInv, boolean legacyFolding) {
        this.rc = rc;
        this.seed = seed;
        this.disableInv = disableInv;
        this.legacyFolding = legacyFolding;
    }

    /**
     * Returns the noise seed after the frame.
     * <p>
     * 프레임이 끝난 뒤의 잡음 시드를 반환한다.
     *
     * @return the seed / 시드
     */
    int getSeed() {
        return seed;
    }

    /**
     * Turns the normalized bands into MDCT coefficients by multiplying with the band amplitudes.
     * <p>
     * 정규화된 대역에 대역 진폭을 곱해 MDCT 계수로 바꾼다.
     *
     * @param x     normalized coefficients (C blocks of N) / 정규화된 계수(N 개씩 C 블록)
     * @param freq  receives the coefficients / 계수를 받는 배열
     * @param bandE amplitudes (C blocks of 21) / 진폭(21개씩 C 블록)
     * @param end   one past the last band / 마지막 대역의 다음
     * @param c     number of channels / 채널 수
     * @param m     frame size multiplier / 프레임 크기 배수
     */
    static void denormaliseBands(float[] x, float[] freq, float[] bandE, int end, int c, int m) {
        int n = m * 120;
        int[] eb = CeltTables.EBANDS;
        // 채널마다 처리한다.
        for (int ch = 0; ch < c; ch++) {
            int f = ch * n;
            int xi = ch * n;
            // 대역마다 진폭을 곱한다.
            for (int i = 0; i < end; i++) {
                float g = bandE[i + ch * CeltEnergy.NB_BANDS];
                int j = m * eb[i];
                int bandEnd = m * eb[i + 1];
                // 조건이 만족되는 동안 반복해서 처리한다.
                do {
                    freq[f++] = x[xi++] * g;
                } while (++j < bandEnd);
            }
            // 처리하지 않은 높은 주파수는 0 으로 둔다.
            for (int i = m * eb[end]; i < n; i++) {
                freq[f++] = 0;
            }
        }
    }

    /**
     * Fills the bands of a transient frame that were quantized to zero with noise so that the energy does not collapse.
     * <p>
     * 과도 프레임에서 0 으로 양자화된 대역에 잡음을 채워 에너지가 붕괴하지 않게 한다.
     *
     * @param x             the coefficients (C blocks of size) / 계수(size 개씩 C 블록)
     * @param collapseMasks collapse masks per band and channel / 대역과 채널마다의 붕괴 마스크
     * @param lm            log2 of the frame size / 프레임 크기의 log2
     * @param c             number of channels / 채널 수
     * @param size          coefficients per channel / 채널당 계수 수
     * @param start         first band / 첫 대역
     * @param end           one past the last band / 마지막 대역의 다음
     * @param logE          current energies / 현재 에너지
     * @param prev1logE     energies of the previous frame / 이전 프레임의 에너지
     * @param prev2logE     energies of the frame before that / 그 이전 프레임의 에너지
     * @param pulses        pulse bits per band / 대역마다 펄스 비트
     * @param seedIn        noise seed / 잡음 시드
     */
    static void antiCollapse(float[] x, int[] collapseMasks, int lm, int c, int size, int start, int end, float[] logE, float[] prev1logE,
            float[] prev2logE, int[] pulses, int seedIn) {
        int sd = seedIn;
        int[] eb = CeltTables.EBANDS;
        final int nb = CeltEnergy.NB_BANDS;
        // 대역마다 처리한다.
        for (int i = start; i < end; i++) {
            int n0 = eb[i + 1] - eb[i];
            int depth = (1 + pulses[i]) / ((eb[i + 1] - eb[i]) << lm);
            float thresh = .5f * CeltMath.exp2(-.125f * depth);
            float sqrt1 = CeltMath.rsqrt(n0 << lm);
            // 채널마다 처리한다.
            for (int ch = 0; ch < c; ch++) {
                float prev1 = prev1logE[ch * nb + i];
                float prev2 = prev2logE[ch * nb + i];
                boolean renormalize = false;
                if (c == 1) {
                    prev1 = Math.max(prev1, prev1logE[nb + i]);
                    prev2 = Math.max(prev2, prev2logE[nb + i]);
                }
                float ediff = logE[ch * nb + i] - Math.min(prev1, prev2);
                ediff = Math.max(0, ediff);
                float r = 2.f * CeltMath.exp2(-ediff);
                if (lm == 3) {
                    r *= 1.41421356f;
                }
                r = Math.min(thresh, r);
                r = r * sqrt1;
                int xo = ch * size + (eb[i] << lm);
                // 짧은 블록마다, 펄스가 없었던 블록에 잡음을 채운다.
                for (int k = 0; k < 1 << lm; k++) {
                    if ((collapseMasks[i * c + ch] & (1 << k)) == 0) {
                        // j 를 0 부터 n0 까지 늘려 가며 항목마다 처리한다.
                        for (int j = 0; j < n0; j++) {
                            sd = CeltMath.lcgRand(sd);
                            x[xo + (j << lm) + k] = (sd & 0x8000) != 0 ? r : -r;
                        }
                        renormalize = true;
                    }
                }
                if (renormalize) {
                    CeltPvq.renormaliseVector(x, xo, n0 << lm, 1.0f);
                }
            }
        }
    }

    /**
     * Merges a mid/side pair back into left/right.
     * <p>
     * 미드/사이드 쌍을 좌/우로 되돌린다.
     *
     * @param x   mid channel then left output / 미드 채널이며 왼쪽 출력
     * @param xo  start of the mid channel / 미드 채널의 시작
     * @param y   side channel then right output / 사이드 채널이며 오른쪽 출력
     * @param yo  start of the side channel / 사이드 채널의 시작
     * @param mid gain of the mid channel / 미드 채널의 이득
     * @param n   number of coefficients / 계수 수
     */
    private static void stereoMerge(float[] x, int xo, float[] y, int yo, float mid, int n) {
        float xp = 0;
        float side = 0;
        // 미드와 사이드의 내적과 사이드의 에너지를 구한다.
        for (int j = 0; j < n; j++) {
            xp = xp + x[xo + j] * y[yo + j];
            side = side + y[yo + j] * y[yo + j];
        }
        xp = mid * xp;
        float mid2 = mid;
        float el = mid2 * mid2 + side - 2 * xp;
        float er = mid2 * mid2 + side + 2 * xp;
        // 에너지가 너무 작으면 미드를 그대로 복사한다.
        if (er < 6e-4f || el < 6e-4f) {
            System.arraycopy(x, xo, y, yo, n);
            return;
        }
        float lgain = CeltMath.rsqrt(el);
        float rgain = CeltMath.rsqrt(er);
        // 좌우 채널의 이득을 맞춰 합성한다.
        for (int j = 0; j < n; j++) {
            float l = mid * x[xo + j];
            float r = y[yo + j];
            x[xo + j] = lgain * (l - r);
            y[yo + j] = rgain * (l + r);
        }
    }

    /**
     * Reorders the coefficients of interleaved short blocks into block order.
     * <p>
     * 인터리브된 짧은 블록의 계수를 블록 순서로 재배열한다.
     *
     * @param x        the coefficients / 계수
     * @param xo       start / 시작
     * @param n0       coefficients per block / 블록당 계수 수
     * @param stride   number of blocks / 블록 수
     * @param hadamard true to use the Hadamard order / 아다마르 순서를 쓰려면 true
     */
    private static void deinterleaveHadamard(float[] x, int xo, int n0, int stride, boolean hadamard) {
        int n = n0 * stride;
        float[] tmp = new float[n];
        // 순서 표에 따라 블록별로 모은다.
        for (int i = 0; i < stride; i++) {
            for (int j = 0; j < n0; j++) {
                int dst = hadamard ? ORDERY_TABLE[stride - 2 + i] * n0 + j : i * n0 + j;
                tmp[dst] = x[xo + j * stride + i];
            }
        }
        System.arraycopy(tmp, 0, x, xo, n);
    }

    /**
     * Inverse of {@link #deinterleaveHadamard}.
     * <p>
     * {@link #deinterleaveHadamard} 의 역.
     *
     * @param x        the coefficients / 계수
     * @param xo       start / 시작
     * @param n0       coefficients per block / 블록당 계수 수
     * @param stride   number of blocks / 블록 수
     * @param hadamard true to use the Hadamard order / 아다마르 순서를 쓰려면 true
     */
    private static void interleaveHadamard(float[] x, int xo, int n0, int stride, boolean hadamard) {
        int n = n0 * stride;
        float[] tmp = new float[n];
        // 블록별로 모인 계수를 다시 인터리브한다.
        for (int i = 0; i < stride; i++) {
            for (int j = 0; j < n0; j++) {
                int src = hadamard ? ORDERY_TABLE[stride - 2 + i] * n0 + j : i * n0 + j;
                tmp[j * stride + i] = x[xo + src];
            }
        }
        System.arraycopy(tmp, 0, x, xo, n);
    }

    /**
     * One level of the Haar transform across blocks.
     * <p>
     * 블록 사이의 하르 변환 한 단계.
     *
     * @param x      the coefficients / 계수
     * @param xo     start / 시작
     * @param n0In   number of coefficients per block / 블록당 계수 수
     * @param stride distance of the pairs / 쌍의 간격
     */
    private static void haar1(float[] x, int xo, int n0In, int stride) {
        int n0 = n0In >> 1;
        // 인접한 계수 쌍의 합과 차를 구한다.
        for (int i = 0; i < stride; i++) {
            for (int j = 0; j < n0; j++) {
                float tmp1 = .70710678f * x[xo + stride * 2 * j + i];
                float tmp2 = .70710678f * x[xo + stride * (2 * j + 1) + i];
                x[xo + stride * 2 * j + i] = tmp1 + tmp2;
                x[xo + stride * (2 * j + 1) + i] = tmp1 - tmp2;
            }
        }
    }

    /**
     * Number of quantization steps of the stereo angle.
     * <p>
     * 스테레오 각도의 양자화 단계 수.
     *
     * @param n        dimension / 차원
     * @param b        bits / 비트
     * @param offset   offset / 오프셋
     * @param pulseCap cap of the pulse bits / 펄스 비트 상한
     * @param stereo   true for a stereo band / 스테레오 대역이면 true
     * @return the number of steps / 단계 수
     */
    private static int computeQn(int n, int b, int offset, int pulseCap, boolean stereo) {
        int n2 = 2 * n - 1;
        if (stereo && n == 2) {
            n2--;
        }
        int qb = Math.min(b - pulseCap - (4 << BITRES), (b + n2 * offset) / n2);
        qb = Math.min(8 << BITRES, qb);
        if (qb < (1 << BITRES >> 1)) {
            return 1;
        }
        int qn = EXP2_TABLE8[qb & 0x7] >> (14 - (qb >> BITRES));
        qn = (qn + 1) >> 1 << 1;
        return qn;
    }

    /**
     * Decodes one band (or a half of a split band).
     * <p>
     * 대역 하나(또는 분할된 대역의 반쪽)를 디코딩한다.
     *
     * @param i          band number / 대역 번호
     * @param x          array of the first vector / 첫 벡터의 배열
     * @param xo         start of the first vector / 첫 벡터의 시작
     * @param yIn        array of the second vector (stereo) or null / 두 번째 벡터(스테레오)의 배열. 없으면 null
     * @param yoIn       start of the second vector / 두 번째 벡터의 시작
     * @param nIn        dimension / 차원
     * @param bIn        bit budget / 비트 예산
     * @param spread     spreading mode / 퍼짐 모드
     * @param bIn2       number of short blocks / 짧은 블록 수
     * @param intensity  intensity start band / 강도 시작 대역
     * @param tfChangeIn time/frequency change / 시간/주파수 변경
     * @param lowbandIn  folding source or null / 접기 원본. 없으면 null
     * @param lbo        start of the folding source / 접기 원본의 시작
     * @param lmIn       log2 of the frame size (reduced by splits) / 프레임 크기의 log2(분할로 줄어듦)
     * @param lowbandOut receives the folding source of the next band or null / 다음 대역의 접기 원본을 받는 배열. 없으면 null
     * @param lboo       start in <code>lowbandOut</code> / <code>lowbandOut</code> 의 시작
     * @param level      recursion level / 재귀 단계
     * @param gain       gain of the vector / 벡터의 이득
     * @param lowbandScratch scratch for the folding source / 접기 원본의 작업 공간
     * @param fillIn     bit mask of blocks that may be filled / 채울 수 있는 블록의 비트 마스크
     * @return the collapse mask / 붕괴 마스크
     */
    private int quantBand(int i, float[] x, int xo, float[] yIn, int yoIn, int nIn, int bIn, int spread, int bIn2, int intensity, int tfChangeIn,
            float[] lowbandIn, int lbo, int lmIn, float[] lowbandOut, int lboo, int level, float gain, float[] lowbandScratch, int fillIn) {
        int n = nIn;
        int b = bIn;
        int bb = bIn2;
        int lm = lmIn;
        int tfChange = tfChangeIn;
        int fill = fillIn;
        float[] y = yIn;
        int yo = yoIn;
        float[] lowband = lowbandIn;
        int lbOff = lbo;
        int n0 = n;
        int nB = n;
        int nB0;
        int b0 = bb;
        int timeDivide = 0;
        int recombine = 0;
        boolean inv = false;
        float mid = 0;
        float side = 0;
        int cm = 0;
        boolean longBlocks = b0 == 1;
        nB /= bb;
        nB0 = nB;
        boolean stereo = y != null;
        boolean split = stereo;
        // 차원이 1 이면 부호 비트 하나만 읽는다.
        if (n == 1) {
            float[] xa = x;
            int xp = xo;
            // c 를 0 부터 1 + (stereo ? 1 : 0) 까지 늘려 가며 항목마다 처리한다.
            for (int c = 0; c < 1 + (stereo ? 1 : 0); c++) {
                int sign = 0;
                if (remaining >= 1 << BITRES) {
                    sign = (int) rc.readBits(1);
                    remaining -= 1 << BITRES;
                    b -= 1 << BITRES;
                }
                xa[xp] = sign != 0 ? -1.f : 1.f;
                xa = y;
                xp = yo;
            }
            if (lowbandOut != null) {
                lowbandOut[lboo] = x[xo];
            }
            return 1;
        }
        // 첫 단계의 모노 대역에서는 시간/주파수 해상도 변경을 준비한다.
        if (!stereo && level == 0) {
            if (tfChange > 0) {
                recombine = tfChange;
            }
            if (lowband != null && (recombine != 0 || ((nB & 1) == 0 && tfChange < 0) || b0 > 1)) {
                System.arraycopy(lowband, lbOff, lowbandScratch, 0, n);
                lowband = lowbandScratch;
                lbOff = 0;
            }
            // 짧은 블록을 합쳐 주파수 해상도를 높인다.
            for (int k = 0; k < recombine; k++) {
                if (lowband != null) {
                    haar1(lowband, lbOff, n >> k, 1 << k);
                }
                fill = BIT_INTERLEAVE_TABLE[fill & 0xF] | (BIT_INTERLEAVE_TABLE[fill >> 4] << 2);
            }
            bb >>= recombine;
            nB <<= recombine;
            // 긴 블록을 나누어 시간 해상도를 높인다.
            while ((nB & 1) == 0 && tfChange < 0) {
                if (lowband != null) {
                    haar1(lowband, lbOff, nB, bb);
                }
                fill |= fill << bb;
                bb <<= 1;
                nB >>= 1;
                timeDivide++;
                tfChange++;
            }
            b0 = bb;
            nB0 = nB;
            if (b0 > 1) {
                if (lowband != null) {
                    deinterleaveHadamard(lowband, lbOff, nB >> recombine, b0 << recombine, longBlocks);
                }
            }
        }
        int cacheBase = CeltTables.CACHE_INDEX[(lm + 1) * CeltEnergy.NB_BANDS + i];
        // 비트가 충분하면 모노 대역을 둘로 나눈다.
        if (!stereo && lm != -1 && b > CeltTables.CACHE_BITS[cacheBase + CeltTables.CACHE_BITS[cacheBase]] + 12 && n > 2) {
            n >>= 1;
            y = x;
            yo = xo + n;
            split = true;
            lm -= 1;
            if (bb == 1) {
                fill = (fill & 1) | (fill << 1);
            }
            bb = (bb + 1) >> 1;
        }
        if (split) {
            int itheta = 0;
            int imid;
            int iside;
            int delta;
            int pulseCap = CeltTables.LOG_N[i] + lm * (1 << BITRES);
            int offset = (pulseCap >> 1) - (stereo && n == 2 ? QTHETA_OFFSET_TWOPHASE : QTHETA_OFFSET);
            int qn = computeQn(n, b, offset, pulseCap, stereo);
            if (stereo && i >= intensity) {
                qn = 1;
            }
            int tell = rc.tellFrac();
            // 각도 단계가 둘 이상이면 각도를 디코딩한다.
            if (qn != 1) {
                if (stereo && n > 2) {
                    int p0 = 3;
                    int x0 = qn / 2;
                    int ft = p0 * (x0 + 1) + x0;
                    int fs = (int) rc.decode(ft);
                    int xv;
                    if (fs < (x0 + 1) * p0) {
                        xv = fs / p0;
                    } else {
                        xv = x0 + 1 + (fs - (x0 + 1) * p0);
                    }
                    rc.update(xv <= x0 ? p0 * xv : (xv - 1 - x0) + (x0 + 1) * p0, xv <= x0 ? p0 * (xv + 1) : (xv - x0) + (x0 + 1) * p0, ft);
                    itheta = xv;
                } else if (b0 > 1 || stereo) {
                    itheta = (int) rc.readUint(qn + 1);
                } else {
                    int fs = 1;
                    int ft = ((qn >> 1) + 1) * ((qn >> 1) + 1);
                    int fl = 0;
                    int fm = (int) rc.decode(ft);
                    if (fm < ((qn >> 1) * ((qn >> 1) + 1) >> 1)) {
                        itheta = (CeltMath.isqrt32(8L * (fm & 0xFFFFFFFFL) + 1) - 1) >> 1;
                        fs = itheta + 1;
                        fl = itheta * (itheta + 1) >> 1;
                    } else {
                        itheta = (2 * (qn + 1) - CeltMath.isqrt32(8L * (long) (ft - fm - 1) + 1)) >> 1;
                        fs = qn + 1 - itheta;
                        fl = ft - ((qn + 1 - itheta) * (qn + 2 - itheta) >> 1);
                    }
                    rc.update(fl, fl + fs, ft);
                }
                itheta = itheta * 16384 / qn;
            } else if (stereo) {
                if (b > 2 << BITRES && remaining > 2 << BITRES) {
                    inv = rc.bitLogp(2) != 0;
                } else {
                    inv = false;
                }
                // 모노로 다운믹스할 때 위상이 반대인 대역이 상쇄되지 않도록 반전 표시는 읽되 무시한다.
                if (disableInv) {
                    inv = false;
                }
                itheta = 0;
            }
            int qalloc = rc.tellFrac() - tell;
            b -= qalloc;
            int origFill = fill;
            // 각도에 따라 미드와 사이드의 이득과 비트 배분 편향을 정한다.
            if (itheta == 0) {
                imid = 32767;
                iside = 0;
                fill &= (1 << bb) - 1;
                delta = -16384;
            } else if (itheta == 16384) {
                imid = 0;
                iside = 32767;
                fill &= ((1 << bb) - 1) << bb;
                delta = 16384;
            } else {
                imid = CeltMath.bitexactCos(itheta);
                iside = CeltMath.bitexactCos(16384 - itheta);
                delta = CeltMath.fracMul16((n - 1) << 7, CeltMath.bitexactLog2tan(iside, imid));
            }
            mid = (1.f / 32768) * imid;
            side = (1.f / 32768) * iside;
            int mbits;
            int sbits;
            if (n == 2 && stereo) {
                int sign = 0;
                mbits = b;
                sbits = 0;
                if (itheta != 0 && itheta != 16384) {
                    sbits = 1 << BITRES;
                }
                mbits -= sbits;
                boolean c = itheta > 8192;
                remaining -= qalloc + sbits;
                float[] x2 = c ? y : x;
                int x2o = c ? yo : xo;
                float[] y2 = c ? x : y;
                int y2o = c ? xo : yo;
                if (sbits != 0) {
                    sign = (int) rc.readBits(1);
                }
                sign = 1 - 2 * sign;
                cm = quantBand(i, x2, x2o, null, 0, n, mbits, spread, bb, intensity, tfChange, lowband, lbOff, lm, lowbandOut, lboo, level, gain,
                        lowbandScratch, origFill);
                y2[y2o] = -sign * x2[x2o + 1];
                y2[y2o + 1] = sign * x2[x2o];
                x[xo] = mid * x[xo];
                x[xo + 1] = mid * x[xo + 1];
                y[yo] = side * y[yo];
                y[yo + 1] = side * y[yo + 1];
                float tmp = x[xo];
                x[xo] = tmp - y[yo];
                y[yo] = tmp + y[yo];
                tmp = x[xo + 1];
                x[xo + 1] = tmp - y[yo + 1];
                y[yo + 1] = tmp + y[yo + 1];
            } else {
                float[] nextLowband2 = null;
                int nl2o = 0;
                float[] nextLowbandOut1 = null;
                int nlo1 = 0;
                int nextLevel = 0;
                if (b0 > 1 && !stereo && (itheta & 0x3fff) != 0) {
                    if (itheta > 8192) {
                        delta -= delta >> (4 - lm);
                    } else {
                        delta = Math.min(0, delta + (n << BITRES >> (5 - lm)));
                    }
                }
                mbits = Math.max(0, Math.min(b, (b - delta) / 2));
                sbits = b - mbits;
                remaining -= qalloc;
                if (lowband != null && !stereo) {
                    nextLowband2 = lowband;
                    nl2o = lbOff + n;
                }
                if (stereo) {
                    nextLowbandOut1 = lowbandOut;
                    nlo1 = lboo;
                } else {
                    nextLevel = level + 1;
                }
                int rebalance = remaining;
                float gainMid = stereo ? 1.0f : gain * mid;
                float gainSide = gain * side;
                // 비트가 많은 쪽(미드 또는 사이드)을 먼저 디코딩한다.
                if (mbits >= sbits) {
                    cm = quantBand(i, x, xo, null, 0, n, mbits, spread, bb, intensity, tfChange, lowband, lbOff, lm, nextLowbandOut1, nlo1, nextLevel,
                            gainMid, lowbandScratch, fill);
                    rebalance = mbits - (rebalance - remaining);
                    if (rebalance > 3 << BITRES && itheta != 0) {
                        sbits += rebalance - (3 << BITRES);
                    }
                    cm |= quantBand(i, y, yo, null, 0, n, sbits, spread, bb, intensity, tfChange, nextLowband2, nl2o, lm, null, 0, nextLevel, gainSide,
                            null, fill >> bb) << ((b0 >> 1) & ((stereo ? 1 : 0) - 1));
                } else {
                    cm = quantBand(i, y, yo, null, 0, n, sbits, spread, bb, intensity, tfChange, nextLowband2, nl2o, lm, null, 0, nextLevel, gainSide,
                            null, fill >> bb) << ((b0 >> 1) & ((stereo ? 1 : 0) - 1));
                    rebalance = sbits - (rebalance - remaining);
                    if (rebalance > 3 << BITRES && itheta != 16384) {
                        mbits += rebalance - (3 << BITRES);
                    }
                    cm |= quantBand(i, x, xo, null, 0, n, mbits, spread, bb, intensity, tfChange, lowband, lbOff, lm, nextLowbandOut1, nlo1, nextLevel,
                            gainMid, lowbandScratch, fill);
                }
            }
        } else {
            int q = CeltRate.bits2pulses(i, lm, b);
            int currBits = CeltRate.pulses2bits(i, lm, q);
            remaining -= currBits;
            // 남은 비트가 모자라면 펄스 수를 줄인다.
            while (remaining < 0 && q > 0) {
                remaining += currBits;
                q--;
                currBits = CeltRate.pulses2bits(i, lm, q);
                remaining -= currBits;
            }
            if (q != 0) {
                int k = CeltRate.getPulses(q);
                cm = CeltPvq.algUnquant(x, xo, n, k, spread, bb, rc, gain);
            } else {
                int cmMask = (1 << bb) - 1;
                fill &= cmMask;
                if (fill == 0) {
                    // 채울 블록이 없으면 0 으로 둔다.
                    for (int j = 0; j < n; j++) {
                        x[xo + j] = 0;
                    }
                } else {
                    if (lowband == null) {
                        // 접을 원본이 없으면 잡음으로 채운다.
                        for (int j = 0; j < n; j++) {
                            seed = CeltMath.lcgRand(seed);
                            x[xo + j] = (float) (seed >> 20);
                        }
                        cm = cmMask;
                    } else {
                        // 낮은 대역을 접어 넣고 약한 잡음을 더한다.
                        for (int j = 0; j < n; j++) {
                            seed = CeltMath.lcgRand(seed);
                            float tmp = 1.0f / 256;
                            tmp = (seed & 0x8000) != 0 ? tmp : -tmp;
                            x[xo + j] = lowband[lbOff + j] + tmp;
                        }
                        cm = fill;
                    }
                    CeltPvq.renormaliseVector(x, xo, n, gain);
                }
            }
        }
        if (stereo) {
            if (n != 2) {
                stereoMerge(x, xo, y, yo, mid, n);
            }
            if (inv) {
                // 위상이 반전된 오른쪽 채널의 부호를 되돌린다.
                for (int j = 0; j < n; j++) {
                    y[yo + j] = -y[yo + j];
                }
            }
        } else if (level == 0) {
            if (b0 > 1) {
                interleaveHadamard(x, xo, nB >> recombine, b0 << recombine, longBlocks);
            }
            nB = nB0;
            bb = b0;
            // 나눈 시간 블록을 다시 합친다.
            for (int k = 0; k < timeDivide; k++) {
                bb >>= 1;
                nB <<= 1;
                cm |= cm >> bb;
                haar1(x, xo, nB, bb);
            }
            // 합쳤던 주파수 해상도를 되돌린다.
            for (int k = 0; k < recombine; k++) {
                cm = BIT_DEINTERLEAVE_TABLE[cm & 0xF];
                haar1(x, xo, n0 >> k, 1 << k);
            }
            bb <<= recombine;
            if (lowbandOut != null) {
                float nn = CeltMath.sqrt(n0);
                // 다음 대역이 접어 쓸 원본을 기록한다.
                for (int j = 0; j < n0; j++) {
                    lowbandOut[lboo + j] = nn * x[xo + j];
                }
            }
            cm &= (1 << bb) - 1;
        }
        return cm;
    }

    /**
     * Decodes all bands of a frame.
     * <p>
     * 프레임의 모든 대역을 디코딩한다.
     *
     * @param start         first band / 첫 대역
     * @param end           one past the last band / 마지막 대역의 다음
     * @param x             receives the coefficients; the second channel starts at offset N / 계수를 받는 배열. 둘째 채널은 N 번에서 시작
     * @param channels      1 or 2 / 1 또는 2
     * @param collapseMasks receives the collapse masks per band and channel / 대역과 채널마다 붕괴 마스크를 받는 배열
     * @param pulses        pulse bits per band / 대역마다 펄스 비트
     * @param shortBlocks   number of short blocks (0 for a long block) / 짧은 블록 수(긴 블록이면 0)
     * @param spread        spreading mode / 퍼짐 모드
     * @param dualStereoIn  dual stereo flag / 이중 스테레오 플래그
     * @param intensity     intensity start band / 강도 시작 대역
     * @param tfRes         time/frequency change per band / 대역마다 시간/주파수 변경
     * @param totalBits     total bits in 1/8 bits / 전체 비트(1/8 비트 단위)
     * @param balanceIn     balance left by the allocation / 할당이 남긴 균형
     * @param lm            log2 of the frame size / 프레임 크기의 log2
     * @param codedBands    number of coded bands / 부호화된 대역 수
     */
    void quantAllBands(int start, int end, float[] x, int channels, int[] collapseMasks, int[] pulses, int shortBlocks, int spread,
            boolean dualStereoIn, int intensity, int[] tfRes, int totalBits, int balanceIn, int lm, int codedBands) {
        int[] eb = CeltTables.EBANDS;
        int m = 1 << lm;
        int bq = shortBlocks != 0 ? m : 1;
        int balance = balanceIn;
        boolean dualStereo = dualStereoIn;
        int normSize = m * eb[CeltEnergy.NB_BANDS];
        float[] norm = new float[channels * normSize];
        float[] lowbandScratch = new float[m * (eb[CeltEnergy.NB_BANDS] - eb[CeltEnergy.NB_BANDS - 1])];
        int norm2 = normSize;
        int n = m * 120;
        int lowbandOffset = 0;
        boolean updateLowband = true;
        // 대역마다 접기 원본을 정하고 디코딩한다.
        for (int i = start; i < end; i++) {
            int xo = m * eb[i];
            float[] y = channels == 2 ? x : null;
            int yo = n + m * eb[i];
            int nn = m * eb[i + 1] - m * eb[i];
            int tell = rc.tellFrac();
            if (i != start) {
                balance -= tell;
            }
            remaining = totalBits - tell - 1;
            int b;
            // 부호화된 대역이면 남은 균형을 반영해 예산을 정한다.
            if (i <= codedBands - 1) {
                int currBalance = balance / Math.min(3, codedBands - i);
                b = Math.max(0, Math.min(16383, Math.min(remaining + 1, pulses[i] + currBalance)));
            } else {
                b = 0;
            }
            if ((m * eb[i] - nn >= m * eb[start] || (!legacyFolding && i == start + 1)) && (updateLowband || lowbandOffset == 0)) {
                lowbandOffset = i;
            }
            // 첫 대역이 둘째 대역보다 좁으면 첫 대역의 접기 데이터를 일부 되풀이해 둘째 대역을 접을 수 있게 한다(RFC 8251 9장).
            if (!legacyFolding && i == start + 1) {
                int n1 = m * (eb[start + 1] - eb[start]);
                int n2 = m * (eb[start + 2] - eb[start + 1]);
                int offset = m * eb[start];
                if (n2 > n1) {
                    System.arraycopy(norm, offset + 2 * n1 - n2, norm, offset + n1, n2 - n1);
                    if (channels == 2) {
                        System.arraycopy(norm, norm2 + offset + 2 * n1 - n2, norm, norm2 + offset + n1, n2 - n1);
                    }
                }
            }
            int tfChange = tfRes[i];
            float[] xa = x;
            int xao = xo;
            float[] ya = y;
            int yao = yo;
            // 부호화하지 않는 대역은 버리는 작업 공간에 디코딩한다.
            if (i >= CeltEnergy.NB_BANDS) {
                xa = norm;
                xao = 0;
                if (channels == 2) {
                    ya = norm;
                    yao = 0;
                }
            }
            int effectiveLowband = -1;
            int xcm;
            int ycm;
            if (lowbandOffset != 0 && (spread != CeltPvq.SPREAD_AGGRESSIVE || bq > 1 || tfChange < 0)) {
                effectiveLowband = Math.max(m * eb[start], m * eb[lowbandOffset] - nn);
                int foldStart = lowbandOffset;
                // 접기 원본이 시작하는 대역을 찾는다.
                while (m * eb[--foldStart] > effectiveLowband) {
                    // 대역 번호만 줄인다.
                }
                int foldEnd = lowbandOffset - 1;
                // 접기 원본이 끝나는 대역을 찾는다(RFC 8251 은 현재 대역을 넘지 않게 한다).
                if (legacyFolding) {
                    while (m * eb[++foldEnd] < effectiveLowband + nn) {
                        // 대역 번호만 늘린다.
                    }
                } else {
                    while (++foldEnd < i && m * eb[foldEnd] < effectiveLowband + nn) {
                        // 대역 번호만 늘린다.
                    }
                }
                xcm = 0;
                ycm = 0;
                int foldI = foldStart;
                // 접기 원본 대역들의 붕괴 마스크를 합친다.
                do {
                    xcm |= collapseMasks[foldI * channels + 0];
                    ycm |= collapseMasks[foldI * channels + channels - 1];
                } while (++foldI < foldEnd);
            } else {
                xcm = (1 << bq) - 1;
                ycm = xcm;
            }
            if (dualStereo && i == intensity) {
                dualStereo = false;
                // 이중 스테레오가 끝나는 대역에서 두 채널의 접기 원본을 평균한다.
                for (int j = m * eb[start]; j < m * eb[i]; j++) {
                    norm[j] = .5f * (norm[j] + norm[norm2 + j]);
                }
            }
            if (dualStereo) {
                xcm = quantBand(i, xa, xao, null, 0, nn, b / 2, spread, bq, intensity, tfChange, effectiveLowband != -1 ? norm : null,
                        effectiveLowband, lm, norm, m * eb[i], 0, 1.0f, lowbandScratch, xcm);
                ycm = quantBand(i, ya, yao, null, 0, nn, b / 2, spread, bq, intensity, tfChange, effectiveLowband != -1 ? norm : null,
                        norm2 + effectiveLowband, lm, norm, norm2 + m * eb[i], 0, 1.0f, lowbandScratch, ycm);
            } else {
                xcm = quantBand(i, xa, xao, channels == 2 ? ya : null, yao, nn, b, spread, bq, intensity, tfChange,
                        effectiveLowband != -1 ? norm : null, effectiveLowband, lm, norm, m * eb[i], 0, 1.0f, lowbandScratch, xcm | ycm);
                ycm = xcm;
            }
            collapseMasks[i * channels] = xcm & 0xFF;
            collapseMasks[i * channels + channels - 1] = ycm & 0xFF;
            balance += pulses[i] + tell;
            updateLowband = b > (nn << BITRES);
        }
    }
}
