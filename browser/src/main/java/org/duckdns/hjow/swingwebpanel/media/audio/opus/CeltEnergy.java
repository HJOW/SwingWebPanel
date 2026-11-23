package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Decoding of the band energies of a CELT frame (RFC 6716 section 4.3.2): the coarse energy with time and frequency prediction
 * and its Laplace-coded residual, the fine energy bits and the leftover bits that refine the energy at the end of the frame
 * (<code>celt/quant_bands.c</code>), in the floating-point configuration of the reference decoder. The energies are kept as
 * logarithms (base 2) relative to a mean per band.
 * <p>
 * CELT 프레임의 대역 에너지 디코딩(RFC 6716 4.3.2): 시간과 주파수 예측을 쓰는 거친 에너지와 라플라스 부호화된 잔차, 미세 에너지 비트,
 * 프레임 끝에서 에너지를 다듬는 남은 비트(<code>celt/quant_bands.c</code>). 참조 디코더의 부동소수점 설정을 따른다. 에너지는 대역마다
 * 평균에 대한 밑 2 로그로 보관한다.
 */
final class CeltEnergy {

    /**
     * Number of bands.
     * <p>
     * 대역 수.
     */
    static final int NB_BANDS = 21;

    /**
     * Largest number of fine energy bits.
     * <p>
     * 미세 에너지 비트의 최대 수.
     */
    static final int MAX_FINE_BITS = 8;

    /**
     * Mean log energy of each band (added back when the energy is turned into an amplitude).
     * <p>
     * 대역마다의 평균 로그 에너지(에너지를 진폭으로 바꿀 때 다시 더한다).
     */
    private static final float[] E_MEANS = { 6.437500f, 6.250000f, 5.750000f, 5.312500f, 5.062500f, 4.812500f, 4.500000f, 4.375000f, 4.875000f,
            4.687500f, 4.562500f, 4.437500f, 4.875000f, 4.625000f, 4.312500f, 4.500000f, 4.375000f, 4.625000f, 4.750000f, 4.437500f,
            3.750000f, 3.750000f, 3.750000f, 3.750000f, 3.750000f };

    /**
     * Inter-frame prediction coefficients per frame size.
     * <p>
     * 프레임 크기별 프레임 간 예측 계수.
     */
    private static final float[] PRED_COEF = { (float) (29440 / 32768.), (float) (26112 / 32768.), (float) (21248 / 32768.), (float) (16384 / 32768.) };

    /**
     * Inter-band prediction coefficients per frame size.
     * <p>
     * 프레임 크기별 대역 간 예측 계수.
     */
    private static final float[] BETA_COEF = { (float) (30147 / 32768.), (float) (22282 / 32768.), (float) (12124 / 32768.), (float) (6554 / 32768.) };

    /**
     * Inter-band prediction coefficient of intra frames.
     * <p>
     * 인트라 프레임의 대역 간 예측 계수.
     */
    private static final float BETA_INTRA = (float) (4915 / 32768.);

    /**
     * Laplace parameters (probability of zero, decay) per frame size, inter/intra and band.
     * <p>
     * 프레임 크기, 인터/인트라, 대역마다의 라플라스 매개변수(0 의 확률, 감쇠).
     */
    private static final int[][][] E_PROB_MODEL = {
            { { 72, 127, 65, 129, 66, 128, 65, 128, 64, 128, 62, 128, 64, 128, 64, 128, 92, 78, 92, 79, 92, 78, 90, 79, 116, 41, 115, 40, 114, 40, 132, 26,
                    132, 26, 145, 17, 161, 12, 176, 10, 177, 11 },
                    { 24, 179, 48, 138, 54, 135, 54, 132, 53, 134, 56, 133, 55, 132, 55, 132, 61, 114, 70, 96, 74, 88, 75, 88, 87, 74, 89, 66, 91, 67, 100,
                            59, 108, 50, 120, 40, 122, 37, 97, 43, 78, 50 } },
            { { 83, 78, 84, 81, 88, 75, 86, 74, 87, 71, 90, 73, 93, 74, 93, 74, 109, 40, 114, 36, 117, 34, 117, 34, 143, 17, 145, 18, 146, 19, 162, 12,
                    165, 10, 178, 7, 189, 6, 190, 8, 177, 9 },
                    { 23, 178, 54, 115, 63, 102, 66, 98, 69, 99, 74, 89, 71, 91, 73, 91, 78, 89, 86, 80, 92, 66, 93, 64, 102, 59, 103, 60, 104, 60, 117, 52,
                            123, 44, 138, 35, 133, 31, 97, 38, 77, 45 } },
            { { 61, 90, 93, 60, 105, 42, 107, 41, 110, 45, 116, 38, 113, 38, 112, 38, 124, 26, 132, 27, 136, 19, 140, 20, 155, 14, 159, 16, 158, 18, 170,
                    13, 177, 10, 187, 8, 192, 6, 175, 9, 159, 10 },
                    { 21, 178, 59, 110, 71, 86, 75, 85, 84, 83, 91, 66, 88, 73, 87, 72, 92, 75, 98, 72, 105, 58, 107, 54, 115, 52, 114, 55, 112, 56, 129, 51,
                            132, 40, 150, 33, 140, 29, 98, 35, 77, 42 } },
            { { 42, 121, 96, 66, 108, 43, 111, 40, 117, 44, 123, 32, 120, 36, 119, 33, 127, 33, 134, 34, 139, 21, 147, 23, 152, 20, 158, 25, 154, 26, 166,
                    21, 173, 16, 184, 13, 184, 10, 150, 13, 139, 15 },
                    { 22, 178, 63, 114, 74, 82, 84, 83, 92, 82, 103, 62, 96, 72, 96, 67, 101, 73, 107, 72, 113, 55, 118, 52, 125, 52, 118, 52, 117, 55, 135,
                            49, 137, 39, 157, 32, 145, 29, 97, 33, 77, 40 } } };

    /**
     * Distribution of the small energy residual when few bits are left.
     * <p>
     * 남은 비트가 적을 때 작은 에너지 잔차의 분포.
     */
    private static final int[] SMALL_ENERGY_ICDF = { 2, 1, 0 };

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private CeltEnergy() {
    }

    /**
     * Decodes the coarse band energies.
     * <p>
     * 거친 대역 에너지를 디코딩한다.
     *
     * @param start     first band / 첫 대역
     * @param end       one past the last band / 마지막 대역의 다음
     * @param oldEBands energies of the previous frame, updated in place (C blocks of 21) / 이전 프레임의 에너지. 제자리에서 갱신(21개씩 C 블록)
     * @param intra     true for an intra frame (no inter-frame prediction) / 프레임 간 예측이 없는 인트라 프레임이면 true
     * @param rc        the range decoder / 범위 디코더
     * @param channels  number of coded channels / 부호화된 채널 수
     * @param lm        log2 of the frame size relative to 2.5 ms / 2.5 ms 에 대한 프레임 크기의 log2
     */
    static void unquantCoarse(int start, int end, float[] oldEBands, boolean intra, OpusRangeDecoder rc, int channels, int lm) {
        int[] probModel = E_PROB_MODEL[lm][intra ? 1 : 0];
        float[] prev = new float[2];
        float coef;
        float beta;
        if (intra) {
            coef = 0;
            beta = BETA_INTRA;
        } else {
            beta = BETA_COEF[lm];
            coef = PRED_COEF[lm];
        }
        int budget = rc.getStorage() * 8;
        // 대역마다, 채널마다 거친 에너지의 잔차를 읽어 예측에 더한다.
        for (int i = start; i < end; i++) {
            for (int c = 0; c < channels; c++) {
                int qi;
                int tell = rc.tell();
                // 남은 비트 양에 따라 잔차를 읽는 방법이 달라진다.
                if (budget - tell >= 15) {
                    int pi = 2 * Math.min(i, 20);
                    qi = rc.laplace(probModel[pi] << 7, probModel[pi + 1] << 6);
                } else if (budget - tell >= 2) {
                    qi = rc.icdf(SMALL_ENERGY_ICDF, 2);
                    qi = (qi >> 1) ^ -(qi & 1);
                } else if (budget - tell >= 1) {
                    qi = -rc.bitLogp(1);
                } else {
                    qi = -1;
                }
                float q = (float) qi;
                int idx = i + c * NB_BANDS;
                oldEBands[idx] = Math.max(-9.f, oldEBands[idx]);
                float tmp = coef * oldEBands[idx] + prev[c] + q;
                oldEBands[idx] = tmp;
                prev[c] = prev[c] + q - beta * q;
            }
        }
    }

    /**
     * Decodes the fine energy bits.
     * <p>
     * 미세 에너지 비트를 디코딩한다.
     *
     * @param start     first band / 첫 대역
     * @param end       one past the last band / 마지막 대역의 다음
     * @param oldEBands energies, updated in place / 에너지. 제자리에서 갱신
     * @param fineQuant number of fine bits per band / 대역마다 미세 비트 수
     * @param rc        the range decoder / 범위 디코더
     * @param channels  number of coded channels / 부호화된 채널 수
     */
    static void unquantFine(int start, int end, float[] oldEBands, int[] fineQuant, OpusRangeDecoder rc, int channels) {
        // 미세 비트가 있는 대역마다 에너지를 보정한다.
        for (int i = start; i < end; i++) {
            if (fineQuant[i] <= 0) {
                continue;
            }
            // c 를 0 부터 channels 까지 늘려 가며 항목마다 처리한다.
            for (int c = 0; c < channels; c++) {
                int q2 = (int) rc.readBits(fineQuant[i]);
                float offset = (q2 + .5f) * (1 << (14 - fineQuant[i])) * (1.f / 16384) - .5f;
                oldEBands[i + c * NB_BANDS] += offset;
            }
        }
    }

    /**
     * Uses the bits left at the end of the frame to refine the energies one more bit.
     * <p>
     * 프레임 끝에 남은 비트로 에너지를 한 비트 더 다듬는다.
     *
     * @param start        first band / 첫 대역
     * @param end          one past the last band / 마지막 대역의 다음
     * @param oldEBands    energies, updated in place / 에너지. 제자리에서 갱신
     * @param fineQuant    fine bits per band / 대역마다 미세 비트 수
     * @param finePriority priority of each band / 대역마다의 우선순위
     * @param bitsLeftIn   number of bits left / 남은 비트 수
     * @param rc           the range decoder / 범위 디코더
     * @param channels     number of coded channels / 부호화된 채널 수
     */
    static void unquantFinalise(int start, int end, float[] oldEBands, int[] fineQuant, int[] finePriority, int bitsLeftIn, OpusRangeDecoder rc,
            int channels) {
        int bitsLeft = bitsLeftIn;
        // 우선순위 0 인 대역부터 남은 비트를 나눠 준다.
        for (int prio = 0; prio < 2; prio++) {
            for (int i = start; i < end && bitsLeft >= channels; i++) {
                if (fineQuant[i] >= MAX_FINE_BITS || finePriority[i] != prio) {
                    continue;
                }
                // c 를 0 부터 channels 까지 늘려 가며 항목마다 처리한다.
                for (int c = 0; c < channels; c++) {
                    int q2 = (int) rc.readBits(1);
                    float offset = (q2 - .5f) * (1 << (14 - fineQuant[i] - 1)) * (1.f / 16384);
                    oldEBands[i + c * NB_BANDS] += offset;
                    bitsLeft--;
                }
            }
        }
    }

    /**
     * Converts log energies to linear amplitudes.
     * <p>
     * 로그 에너지를 선형 진폭으로 바꾼다.
     *
     * @param start     first band / 첫 대역
     * @param end       one past the last band / 마지막 대역의 다음
     * @param eBands    receives the amplitudes / 진폭을 받는 배열
     * @param oldEBands the log energies / 로그 에너지
     * @param channels  number of channels / 채널 수
     */
    static void log2Amp(int start, int end, float[] eBands, float[] oldEBands, int channels) {
        // 채널마다 처리 범위 밖은 0, 안은 지수 변환한다.
        for (int c = 0; c < channels; c++) {
            int i;
            // i 를 0 부터 start 까지 늘려 가며 항목마다 처리한다.
            for (i = 0; i < start; i++) {
                eBands[i + c * NB_BANDS] = 0;
            }
            // 항목마다 반복해서 처리한다.
            for (; i < end; i++) {
                float lg = Math.min(32.f, oldEBands[i + c * NB_BANDS] + E_MEANS[i]);
                eBands[i + c * NB_BANDS] = CeltMath.exp2(lg);
            }
            // 항목마다 반복해서 처리한다.
            for (; i < NB_BANDS; i++) {
                eBands[i + c * NB_BANDS] = 0;
            }
        }
    }
}
