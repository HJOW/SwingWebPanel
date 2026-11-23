package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The line spectral frequency (NLSF) handling of the SILK decoder: the two codebook configurations, the unpacking of the stage 2
 * selectors, the dequantization of the residual, the stabilization that keeps the frequencies ordered and apart, the conversion to
 * LPC predictor coefficients and the check of the prediction gain (<code>silk/NLSF_*.c</code>, <code>silk/NLSF2A.c</code>,
 * <code>silk/LPC_inv_pred_gain.c</code>). Integer arithmetic exactly as in the reference decoder.
 * <p>
 * SILK 디코더의 선 스펙트럼 주파수(NLSF) 처리: 두 가지 부호책 구성, 2단계 선택자 풀기, 잔차 역양자화, 주파수의 순서와 간격을 지키는 안정화,
 * LPC 예측 계수로의 변환, 예측 이득 확인(<code>silk/NLSF_*.c</code>, <code>silk/NLSF2A.c</code>, <code>silk/LPC_inv_pred_gain.c</code>).
 * 참조 디코더와 똑같은 정수 연산을 쓴다.
 */
final class SilkNlsf {

    /**
     * Largest LPC order.
     * <p>
     * 최대 LPC 차수.
     */
    static final int MAX_LPC_ORDER = 16;

    /**
     * Maximum amplitude of the NLSF residual indices before the extension.
     * <p>
     * 확장 전 NLSF 잔차 번호의 최대 진폭.
     */
    static final int NLSF_QUANT_MAX_AMPLITUDE = 4;

    /**
     * Q format of the NLSF weights.
     * <p>
     * NLSF 가중치의 Q 형식.
     */
    private static final int NLSF_W_Q = 2;

    /**
     * Ordering of the cosine values for order 16.
     * <p>
     * 차수 16 의 코사인 값 순서.
     */
    private static final int[] ORDERING16 = { 0, 15, 8, 7, 4, 11, 12, 3, 2, 13, 10, 5, 6, 9, 14, 1 };

    /**
     * Ordering of the cosine values for order 10.
     * <p>
     * 차수 10 의 코사인 값 순서.
     */
    private static final int[] ORDERING10 = { 0, 9, 6, 3, 4, 5, 8, 1, 2, 7 };

    /**
     * Codebook of the narrowband and mediumband configuration (order 10).
     * <p>
     * 협대역/중대역 구성(차수 10)의 부호책.
     */
    static final Codebook CB_NB_MB = new Codebook(10, (int) (0.18 * 65536 + 0.5), SilkNlsfTables.CB1_NB_MB_Q8, SilkNlsfTables.CB1_ICDF_NB_MB,
            SilkNlsfTables.PRED_NB_MB_Q8, SilkNlsfTables.CB2_SELECT_NB_MB, SilkNlsfTables.CB2_ICDF_NB_MB, SilkNlsfTables.DELTA_MIN_NB_MB_Q15);

    /**
     * Codebook of the wideband configuration (order 16).
     * <p>
     * 광대역 구성(차수 16)의 부호책.
     */
    static final Codebook CB_WB = new Codebook(16, (int) (0.15 * 65536 + 0.5), SilkNlsfTables.CB1_WB_Q8, SilkNlsfTables.CB1_ICDF_WB,
            SilkNlsfTables.PRED_WB_Q8, SilkNlsfTables.CB2_SELECT_WB, SilkNlsfTables.CB2_ICDF_WB, SilkNlsfTables.DELTA_MIN_WB_Q15);

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private SilkNlsf() {
    }

    /**
     * A two-stage NLSF codebook with its entropy coding tables.
     * <p>
     * 엔트로피 부호화 표를 가진 2단계 NLSF 부호책.
     */
    static final class Codebook {

        /**
         * Number of vectors of stage 1.
         * <p>
         * 1단계의 벡터 수.
         */
        final int nVectors = 32;

        /**
         * LPC order.
         * <p>
         * LPC 차수.
         */
        final int order;

        /**
         * Quantization step of the residual in Q16.
         * <p>
         * Q16 잔차 양자화 단계.
         */
        final int quantStepSizeQ16;

        /**
         * Stage 1 vectors in Q8.
         * <p>
         * Q8 1단계 벡터.
         */
        final int[] cb1Q8;

        /**
         * Distributions of the stage 1 index.
         * <p>
         * 1단계 번호의 분포.
         */
        final int[] cb1Icdf;

        /**
         * Prediction weights between neighbouring coefficients in Q8.
         * <p>
         * Q8 이웃 계수 사이의 예측 가중치.
         */
        final int[] predQ8;

        /**
         * Packed selectors of the stage 2 distributions.
         * <p>
         * 2단계 분포의 압축된 선택자.
         */
        final int[] ecSel;

        /**
         * Distributions of the stage 2 indices.
         * <p>
         * 2단계 번호의 분포.
         */
        final int[] ecIcdf;

        /**
         * Minimum spacing of the NLSFs in Q15.
         * <p>
         * Q15 NLSF 최소 간격.
         */
        final int[] deltaMinQ15;

        /**
         * Creates a codebook.
         * <p>
         * 부호책을 만든다.
         *
         * @param order       LPC order / LPC 차수
         * @param stepQ16     quantization step in Q16 / Q16 양자화 단계
         * @param cb1Q8       stage 1 vectors / 1단계 벡터
         * @param cb1Icdf     stage 1 distributions / 1단계 분포
         * @param predQ8      prediction weights / 예측 가중치
         * @param ecSel       stage 2 selectors / 2단계 선택자
         * @param ecIcdf      stage 2 distributions / 2단계 분포
         * @param deltaMinQ15 minimum spacing / 최소 간격
         */
        Codebook(int order, int stepQ16, int[] cb1Q8, int[] cb1Icdf, int[] predQ8, int[] ecSel, int[] ecIcdf, int[] deltaMinQ15) {
            this.order = order;
            this.quantStepSizeQ16 = stepQ16;
            this.cb1Q8 = cb1Q8;
            this.cb1Icdf = cb1Icdf;
            this.predQ8 = predQ8;
            this.ecSel = ecSel;
            this.ecIcdf = ecIcdf;
            this.deltaMinQ15 = deltaMinQ15;
        }
    }

    /**
     * Unpacks the stage 2 selectors of a stage 1 vector.
     * <p>
     * 1단계 벡터에 대한 2단계 선택자를 푼다.
     *
     * @param ecIx   receives the offsets into the stage 2 distributions / 2단계 분포의 위치를 받는 배열
     * @param predQ8 receives the prediction weights / 예측 가중치를 받는 배열
     * @param cb     the codebook / 부호책
     * @param cb1    the stage 1 index / 1단계 번호
     */
    static void unpack(int[] ecIx, int[] predQ8, Codebook cb, int cb1) {
        int sel = cb1 * cb.order / 2;
        // 선택자 바이트 하나가 계수 두 개의 정보를 담는다.
        for (int i = 0; i < cb.order; i += 2) {
            int entry = cb.ecSel[sel++];
            ecIx[i] = ((entry >> 1) & 7) * (2 * NLSF_QUANT_MAX_AMPLITUDE + 1);
            predQ8[i] = cb.predQ8[i + (entry & 1) * (cb.order - 1)];
            ecIx[i + 1] = ((entry >> 5) & 7) * (2 * NLSF_QUANT_MAX_AMPLITUDE + 1);
            predQ8[i + 1] = cb.predQ8[i + ((entry >> 4) & 1) * (cb.order - 1) + 1];
        }
    }

    /**
     * Weights of the NLSF coefficients (Laroia's method).
     * <p>
     * NLSF 계수의 가중치(Laroia 방식).
     *
     * @param w   receives the weights in Q2 / Q2 가중치를 받는 배열
     * @param nlsfQ15 the NLSFs / NLSF
     * @param d   order / 차수
     */
    private static void vqWeightsLaroia(int[] w, int[] nlsfQ15, int d) {
        int tmp1 = Math.max(nlsfQ15[0], 1);
        tmp1 = (1 << (15 + NLSF_W_Q)) / tmp1;
        int tmp2 = Math.max(nlsfQ15[1] - nlsfQ15[0], 1);
        tmp2 = (1 << (15 + NLSF_W_Q)) / tmp2;
        w[0] = Math.min(tmp1 + tmp2, 32767);
        // 이웃한 두 간격의 역수를 더해 계수 쌍마다 가중치를 구한다.
        for (int k = 1; k < d - 1; k += 2) {
            tmp1 = Math.max(nlsfQ15[k + 1] - nlsfQ15[k], 1);
            tmp1 = (1 << (15 + NLSF_W_Q)) / tmp1;
            w[k] = Math.min(tmp1 + tmp2, 32767);
            tmp2 = Math.max(nlsfQ15[k + 2] - nlsfQ15[k + 1], 1);
            tmp2 = (1 << (15 + NLSF_W_Q)) / tmp2;
            w[k + 1] = Math.min(tmp1 + tmp2, 32767);
        }
        tmp1 = Math.max((1 << 15) - nlsfQ15[d - 1], 1);
        tmp1 = (1 << (15 + NLSF_W_Q)) / tmp1;
        w[d - 1] = Math.min(tmp1 + tmp2, 32767);
    }

    /**
     * Keeps the NLSFs ordered and at least a minimum distance apart.
     * <p>
     * NLSF 가 순서를 지키고 최소 간격 이상 떨어지게 한다.
     *
     * @param nlsfQ15 the NLSFs, modified in place / NLSF. 제자리에서 바뀐다
     * @param deltaMin minimum spacings (L + 1 values) / 최소 간격(L + 1 개)
     * @param l       order / 차수
     */
    static void stabilize(int[] nlsfQ15, int[] deltaMin, int l) {
        int loops;
        // 가장 크게 어긋난 곳을 찾아 고치는 일을 최대 20 번 되풀이한다.
        for (loops = 0; loops < 20; loops++) {
            int minDiff = nlsfQ15[0] - deltaMin[0];
            int idx = 0;
            // i 를 1 부터 l - 1 까지 늘려 가며 항목마다 처리한다.
            for (int i = 1; i <= l - 1; i++) {
                int diff = nlsfQ15[i] - (nlsfQ15[i - 1] + deltaMin[i]);
                if (diff < minDiff) {
                    minDiff = diff;
                    idx = i;
                }
            }
            int diff = (1 << 15) - (nlsfQ15[l - 1] + deltaMin[l]);
            if (diff < minDiff) {
                minDiff = diff;
                idx = l;
            }
            if (minDiff >= 0) {
                return;
            }
            if (idx == 0) {
                nlsfQ15[0] = deltaMin[0];
            } else if (idx == l) {
                nlsfQ15[l - 1] = (1 << 15) - deltaMin[l];
            } else {
                int minCenter = 0;
                // k 를 0 부터 idx 까지 늘려 가며 항목마다 처리한다.
                for (int k = 0; k < idx; k++) {
                    minCenter += deltaMin[k];
                }
                minCenter += deltaMin[idx] >> 1;
                int maxCenter = 1 << 15;
                // k 를 l 부터 idx 까지 줄여 가며 항목마다 처리한다.
                for (int k = l; k > idx; k--) {
                    maxCenter -= deltaMin[k];
                }
                maxCenter -= deltaMin[idx] >> 1;
                int center = (short) SilkMath.limit(SilkMath.rshiftRound(nlsfQ15[idx - 1] + nlsfQ15[idx], 1), minCenter, maxCenter);
                nlsfQ15[idx - 1] = center - (deltaMin[idx] >> 1);
                nlsfQ15[idx] = nlsfQ15[idx - 1] + deltaMin[idx];
            }
        }
        // 반복으로 해결하지 못하면 정렬하고 한계를 강제한다.
        for (int i = 1; i < l; i++) {
            int value = nlsfQ15[i];
            int j;
            // j 를 i - 1 부터 0 && value < nlsfQ15[j] 까지 줄여 가며 항목마다 처리한다.
            for (j = i - 1; j >= 0 && value < nlsfQ15[j]; j--) {
                nlsfQ15[j + 1] = nlsfQ15[j];
            }
            nlsfQ15[j + 1] = value;
        }
        nlsfQ15[0] = Math.max(nlsfQ15[0], deltaMin[0]);
        // i 를 1 부터 l 까지 늘려 가며 항목마다 처리한다.
        for (int i = 1; i < l; i++) {
            nlsfQ15[i] = Math.max(nlsfQ15[i], SilkMath.sat16(nlsfQ15[i - 1] + deltaMin[i]));
        }
        nlsfQ15[l - 1] = Math.min(nlsfQ15[l - 1], (1 << 15) - deltaMin[l]);
        // i 를 l - 2 부터 0 까지 줄여 가며 항목마다 처리한다.
        for (int i = l - 2; i >= 0; i--) {
            nlsfQ15[i] = Math.min(nlsfQ15[i], nlsfQ15[i + 1] - deltaMin[i + 1]);
        }
    }

    /**
     * Decodes the NLSFs from the indices.
     * <p>
     * 번호로부터 NLSF 를 디코딩한다.
     *
     * @param nlsfQ15 receives the NLSFs in Q15 / Q15 NLSF 를 받는 배열
     * @param indices the indices: stage 1 index followed by the stage 2 residual indices / 번호: 1단계 번호 다음에 2단계 잔차 번호
     * @param cb      the codebook / 부호책
     */
    static void decode(int[] nlsfQ15, int[] indices, Codebook cb) {
        int order = cb.order;
        int[] predQ8 = new int[MAX_LPC_ORDER];
        int[] ecIx = new int[MAX_LPC_ORDER];
        int[] resQ10 = new int[MAX_LPC_ORDER];
        int[] wTmpQW = new int[MAX_LPC_ORDER];
        int base = indices[0] * order;
        // 1단계 벡터를 Q15 로 옮긴다.
        for (int i = 0; i < order; i++) {
            nlsfQ15[i] = (short) cb.cb1Q8[base + i] << 7;
        }
        unpack(ecIx, predQ8, cb, indices[0]);
        int outQ10 = 0;
        // 뒤에서부터 잔차를 역양자화하며 예측을 반영한다.
        for (int i = order - 1; i >= 0; i--) {
            int predQ10 = SilkMath.smulbb(outQ10, (short) predQ8[i]) >> 8;
            outQ10 = indices[i + 1] << 10;
            if (outQ10 > 0) {
                outQ10 = outQ10 - 102;
            } else if (outQ10 < 0) {
                outQ10 = outQ10 + 102;
            }
            outQ10 = SilkMath.smlawb(predQ10, outQ10, cb.quantStepSizeQ16);
            resQ10[i] = (short) outQ10;
        }
        vqWeightsLaroia(wTmpQW, nlsfQ15, order);
        // 가중치로 잔차를 보정해 NLSF 를 만든다.
        for (int i = 0; i < order; i++) {
            int wTmpQ9 = SilkMath.sqrtApprox(wTmpQW[i] << (18 - NLSF_W_Q));
            int tmp = nlsfQ15[i] + ((resQ10[i] << 14) / wTmpQ9);
            nlsfQ15[i] = (short) SilkMath.limit(tmp, 0, 32767);
        }
        stabilize(nlsfQ15, cb.deltaMinQ15, order);
    }

    /**
     * Polynomial of the line spectral pairs used by {@link #nlsf2a}.
     * <p>
     * {@link #nlsf2a} 가 쓰는 선 스펙트럼 쌍의 다항식.
     *
     * @param out  receives the dd + 1 coefficients in Q16 / Q16 계수 dd + 1 개를 받는 배열
     * @param cLsf the cosines of the frequencies / 주파수의 코사인
     * @param cOff offset of the first cosine / 첫 코사인의 위치
     * @param dd   half of the order / 차수의 절반
     */
    private static void findPoly(int[] out, int[] cLsf, int cOff, int dd) {
        out[0] = 1 << 16;
        out[1] = -cLsf[cOff];
        // 주파수 쌍마다 다항식을 한 차수씩 키운다.
        for (int k = 1; k < dd; k++) {
            int ftmp = cLsf[cOff + 2 * k];
            out[k + 1] = (out[k - 1] << 1) - (int) SilkMath.rshiftRound64((long) ftmp * out[k], 16);
            // n 를 k 부터 1 까지 줄여 가며 항목마다 처리한다.
            for (int n = k; n > 1; n--) {
                out[n] += out[n - 2] - (int) SilkMath.rshiftRound64((long) ftmp * out[n - 1], 16);
            }
            out[1] -= ftmp;
        }
    }

    /**
     * Converts NLSFs to LPC predictor coefficients in Q12.
     * <p>
     * NLSF 를 Q12 LPC 예측 계수로 변환한다.
     *
     * @param aQ12    receives the coefficients / 계수를 받는 배열
     * @param aOff    offset in <code>aQ12</code> / <code>aQ12</code> 의 위치
     * @param nlsf    the NLSFs in Q15 / Q15 NLSF
     * @param d       order, 10 or 16 / 차수(10 또는 16)
     */
    static void nlsf2a(short[] aQ12, int aOff, int[] nlsf, int d) {
        int[] ordering = d == 16 ? ORDERING16 : ORDERING10;
        int[] cosLsfQA = new int[MAX_LPC_ORDER];
        int[] p = new int[MAX_LPC_ORDER / 2 + 1];
        int[] q = new int[MAX_LPC_ORDER / 2 + 1];
        int[] a32QA1 = new int[MAX_LPC_ORDER];
        // 주파수마다 코사인 표를 보간한다.
        for (int k = 0; k < d; k++) {
            int fInt = nlsf[k] >> (15 - 7);
            int fFrac = nlsf[k] - (fInt << (15 - 7));
            int cosVal = SilkTables.LSF_COS_TAB_Q12[fInt];
            int delta = SilkTables.LSF_COS_TAB_Q12[fInt + 1] - cosVal;
            cosLsfQA[ordering[k]] = SilkMath.rshiftRound((cosVal << 8) + delta * fFrac, 20 - 16);
        }
        int dd = d >> 1;
        findPoly(p, cosLsfQA, 0, dd);
        findPoly(q, cosLsfQA, 1, dd);
        // 두 다항식의 합과 차로 예측 계수를 만든다.
        for (int k = 0; k < dd; k++) {
            int ptmp = p[k + 1] + p[k];
            int qtmp = q[k + 1] - q[k];
            a32QA1[k] = -qtmp - ptmp;
            a32QA1[d - k - 1] = qtmp - ptmp;
        }
        int i;
        // 계수가 16비트를 넘으면 대역폭 확장으로 줄이는 일을 최대 10 번 한다.
        for (i = 0; i < 10; i++) {
            int maxabs = 0;
            int idx = 0;
            // k 를 0 부터 d 까지 늘려 가며 항목마다 처리한다.
            for (int k = 0; k < d; k++) {
                int absval = Math.abs(a32QA1[k]);
                if (absval > maxabs) {
                    maxabs = absval;
                    idx = k;
                }
            }
            maxabs = SilkMath.rshiftRound(maxabs, 16 + 1 - 12);
            if (maxabs > 32767) {
                maxabs = Math.min(maxabs, 163838);
                int scQ16 = 65470 - (((maxabs - 32767) << 14) / ((maxabs * (idx + 1)) >> 2));
                SilkMath.bwexpander32(a32QA1, d, scQ16);
            } else {
                break;
            }
        }
        if (i == 10) {
            // 확장으로도 줄이지 못하면 포화시킨다.
            for (int k = 0; k < d; k++) {
                aQ12[aOff + k] = (short) SilkMath.sat16(SilkMath.rshiftRound(a32QA1[k], 16 + 1 - 12));
                a32QA1[k] = aQ12[aOff + k] << (16 + 1 - 12);
            }
        } else {
            // k 를 0 부터 d 까지 늘려 가며 항목마다 처리한다.
            for (int k = 0; k < d; k++) {
                aQ12[aOff + k] = (short) SilkMath.rshiftRound(a32QA1[k], 16 + 1 - 12);
            }
        }
        final int minInvGain = (int) (1.0 / 1e4f * (1L << 30) + 0.5);
        // 필터가 불안정하면 대역폭 확장을 더해 안정화한다.
        for (i = 0; i < 16; i++) {
            if (lpcInversePredGain(aQ12, aOff, d) < minInvGain) {
                SilkMath.bwexpander32(a32QA1, d, 65536 - (2 << i));
                // k 를 0 부터 d 까지 늘려 가며 항목마다 처리한다.
                for (int k = 0; k < d; k++) {
                    aQ12[aOff + k] = (short) SilkMath.rshiftRound(a32QA1[k], 16 + 1 - 12);
                }
            } else {
                break;
            }
        }
    }

    /**
     * Inverse of the prediction gain of an LPC filter, which is 0 when the filter is unstable.
     * <p>
     * LPC 필터의 예측 이득의 역수. 필터가 불안정하면 0 이다.
     *
     * @param aQ12  the coefficients in Q12 / Q12 계수
     * @param off   offset of the coefficients / 계수의 위치
     * @param order the order / 차수
     * @return the inverse gain in Q30 / Q30 이득의 역수
     */
    static int lpcInversePredGain(short[] aQ12, int off, int order) {
        int[][] atmp = new int[2][MAX_LPC_ORDER];
        int[] anew = atmp[order & 1];
        int dcResp = 0;
        // 계수를 Q24 로 옮기며 직류 응답을 구한다.
        for (int k = 0; k < order; k++) {
            dcResp += aQ12[off + k];
            anew[k] = aQ12[off + k] << (24 - 12);
        }
        if (dcResp >= 4096) {
            return 0;
        }
        return lpcInversePredGainQA(atmp, order);
    }

    /**
     * Levinson-style step-down recursion that computes the inverse prediction gain.
     * <p>
     * 예측 이득의 역수를 구하는 Levinson 방식의 차수 감소 재귀.
     *
     * @param aQA   two work arrays of coefficients in Q24 / Q24 계수의 작업 배열 두 개
     * @param order the order / 차수
     * @return the inverse gain in Q30, 0 if unstable / Q30 이득의 역수. 불안정하면 0
     */
    private static int lpcInversePredGainQA(int[][] aQA, int order) {
        final int aLimit = (int) (0.99975 * (1L << 24) + 0.5);
        int[] anew = aQA[order & 1];
        int invGainQ30 = 1 << 30;
        // 차수를 하나씩 줄이며 반사 계수와 이득을 구한다.
        for (int k = order - 1; k > 0; k--) {
            if (anew[k] > aLimit || anew[k] < -aLimit) {
                return 0;
            }
            int rcQ31 = -(anew[k] << (31 - 24));
            int rcMult1Q30 = (1 << 30) - SilkMath.smmul(rcQ31, rcQ31);
            int mult2Q = 32 - SilkMath.clz32(Math.abs(rcMult1Q30));
            int rcMult2 = SilkMath.inverse32VarQ(rcMult1Q30, mult2Q + 30);
            invGainQ30 = SilkMath.smmul(invGainQ30, rcMult1Q30) << 2;
            int[] aold = anew;
            anew = aQA[k & 1];
            // n 를 0 부터 k 까지 늘려 가며 항목마다 처리한다.
            for (int n = 0; n < k; n++) {
                int tmpQA = SilkMath.subSat32(aold[n], (int) SilkMath.rshiftRound64((long) aold[k - n - 1] * rcQ31, 31));
                long tmp64 = SilkMath.rshiftRound64((long) tmpQA * rcMult2, mult2Q);
                // 32비트를 넘으면 필터가 불안정한 것으로 본다(RFC 8251 6장).
                if (tmp64 > Integer.MAX_VALUE || tmp64 < Integer.MIN_VALUE) {
                    return 0;
                }
                anew[n] = (int) tmp64;
            }
        }
        if (anew[0] > aLimit || anew[0] < -aLimit) {
            return 0;
        }
        int rcQ31 = -(anew[0] << (31 - 24));
        int rcMult1Q30 = (1 << 30) - SilkMath.smmul(rcQ31, rcQ31);
        invGainQ30 = SilkMath.smmul(invGainQ30, rcMult1Q30) << 2;
        return invGainQ30;
    }
}
