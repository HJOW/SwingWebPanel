package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The warped motion of the AV1 decoder (plan item P18-03; AV1 specification sections 7.11.3.5 to 7.11.3.8): the shear decomposition of an
 * affine model (<code>setup shear</code>), the approximate division, the least squares fit of a local warp model from the motion vectors of
 * neighbouring blocks (<code>warp estimation</code>) and the prediction of a block with such a model (<code>block warp</code>).
 * <p>
 * AV1 디코더의 워프 움직임(계획 P18-03; AV1 규격 7.11.3.5~7.11.3.8절): 아핀 모델의 전단 분해(<code>setup shear</code>), 근사 나눗셈, 이웃 블록 움직임
 * 벡터로부터 국소 워프 모델을 최소 제곱으로 맞추기(<code>warp estimation</code>), 그 모델로 블록 예측하기(<code>block warp</code>).
 */
final class Av1Warp {

    /**
     * The number of fractional bits of the warp model parameters.
     * <p>
     * 워프 모델 매개변수의 소수 비트 수.
     */
    private static final int WARPEDMODEL_PREC_BITS = 16;

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 가진다.
     */
    private Av1Warp() {
    }

    /**
     * The resolve divisor process (specification 7.11.3.7): an approximate division by multiplying and shifting.
     * <p>
     * 제수 해석 과정(규격 7.11.3.7): 곱셈과 시프트로 하는 근사 나눗셈.
     *
     * @param d   the divisor / 제수
     * @param out the result: the shift and the factor / 결과: 시프트와 계수
     */
    static void resolveDivisor(long d, long[] out) {
        long ad = Math.abs(d);
        int n = 63 - Long.numberOfLeadingZeros(ad);
        long e = ad - (1L << n);
        long f;
        if (n > 8) {
            f = (e + (1L << (n - 8 - 1))) >> (n - 8);
        } else {
            f = e << (8 - n);
        }
        out[0] = n + 14;
        long lut = AvifInterTables.DIV_LUT[(int) f];
        out[1] = d < 0 ? -lut : lut;
    }

    /**
     * The setup shear process (specification 7.11.3.6).
     * <p>
     * 전단 설정 과정(규격 7.11.3.6).
     *
     * @param wp  the six warp parameters / 워프 매개변수 여섯 개
     * @param out the output alpha, beta, gamma and delta / 출력 alpha, beta, gamma, delta
     * @return true if the model can be used (<code>warpValid</code>) / 모델을 쓸 수 있으면 true(<code>warpValid</code>)
     */
    static boolean setupShear(int[] wp, int[] out) {
        long alpha0 = clip(wp[2] - (1 << WARPEDMODEL_PREC_BITS));
        long beta0 = clip(wp[3]);
        long[] div = new long[2];
        resolveDivisor(wp[2], div);
        long v = (long) wp[4] << WARPEDMODEL_PREC_BITS;
        long gamma0 = clip(Av1MvStack.round2Signed(v * div[1], (int) div[0]));
        long w = (long) wp[3] * wp[4];
        long delta0 = clip(wp[5] - Av1MvStack.round2Signed(w * div[1], (int) div[0]) - (1 << WARPEDMODEL_PREC_BITS));
        int alpha = (int) (Av1MvStack.round2Signed(alpha0, 6) << 6);
        int beta = (int) (Av1MvStack.round2Signed(beta0, 6) << 6);
        int gamma = (int) (Av1MvStack.round2Signed(gamma0, 6) << 6);
        int delta = (int) (Av1MvStack.round2Signed(delta0, 6) << 6);
        out[0] = alpha;
        out[1] = beta;
        out[2] = gamma;
        out[3] = delta;
        if (4 * Math.abs(alpha) + 7 * Math.abs(beta) >= (1 << WARPEDMODEL_PREC_BITS)) {
            return false;
        }
        return 4 * Math.abs(gamma) + 4 * Math.abs(delta) < (1 << WARPEDMODEL_PREC_BITS);
    }

    /**
     * Clips a value to 16 signed bits.
     * <p>
     * 값을 부호 있는 16비트 범위로 자른다.
     *
     * @param v the value / 값
     * @return the clipped value / 자른 값
     */
    private static long clip(long v) {
        return Math.max(-32768, Math.min(32767, v));
    }

    /**
     * The warp estimation process (specification 7.11.3.8): fits an affine model to the candidate samples by least squares.
     * <p>
     * 워프 추정 과정(규격 7.11.3.8): 후보 표본에 아핀 모델을 최소 제곱으로 맞춘다.
     *
     * @param candList   the candidates (source and destination position per sample) / 후보(표본마다 원본 위치와 목적 위치)
     * @param numSamples the number of candidates / 후보 수
     * @param miRow      the row of the block (4x4 units) / 블록의 행(4x4 단위)
     * @param miCol      the column of the block / 블록의 열
     * @param w4         the width of the block (4x4 units) / 블록 너비(4x4 단위)
     * @param h4         the height of the block / 블록 높이
     * @param mvRow      the vertical vector component of the block / 블록 움직임 벡터의 세로 성분
     * @param mvCol      the horizontal vector component of the block / 블록 움직임 벡터의 가로 성분
     * @param out        the six parameters of the model / 모델의 매개변수 여섯 개
     * @return true if a model was found (<code>LocalValid</code>) / 모델을 찾았으면 true(<code>LocalValid</code>)
     */
    static boolean estimate(int[] candList, int numSamples, int miRow, int miCol, int w4, int h4, int mvRow, int mvCol, int[] out) {
        long a00 = 0;
        long a01 = 0;
        long a11 = 0;
        long bx0 = 0;
        long bx1 = 0;
        long by0 = 0;
        long by1 = 0;
        int midY = miRow * 4 + h4 * 2 - 1;
        int midX = miCol * 4 + w4 * 2 - 1;
        int suy = midY * 8;
        int sux = midX * 8;
        int duy = suy + mvRow;
        int dux = sux + mvCol;
        // 후보 표본마다 최소 제곱 방정식의 항을 누적한다.
        for (int i = 0; i < numSamples; i++) {
            int sy = candList[i * 4] - suy;
            int sx = candList[i * 4 + 1] - sux;
            int dy = candList[i * 4 + 2] - duy;
            int dx = candList[i * 4 + 3] - dux;
            if (Math.abs(sx - dx) < 256 && Math.abs(sy - dy) < 256) {
                a00 += ls(sx, sx) + 8;
                a01 += ls(sx, sy) + 4;
                a11 += ls(sy, sy) + 8;
                bx0 += ls(sx, dx) + 8;
                bx1 += ls(sy, dx) + 4;
                by0 += ls(sx, dy) + 4;
                by1 += ls(sy, dy) + 8;
            }
        }
        long det = a00 * a11 - a01 * a01;
        if (det == 0) {
            return false;
        }
        long[] div = new long[2];
        resolveDivisor(det, div);
        long divShift = div[0] - WARPEDMODEL_PREC_BITS;
        long divFactor = div[1];
        if (divShift < 0) {
            divFactor = divFactor << (-divShift);
            divShift = 0;
        }
        out[2] = diag(a11 * bx0 - a01 * bx1, divFactor, (int) divShift);
        out[3] = nondiag(-a01 * bx0 + a00 * bx1, divFactor, (int) divShift);
        out[4] = nondiag(a11 * by0 - a01 * by1, divFactor, (int) divShift);
        out[5] = diag(-a01 * by0 + a00 * by1, divFactor, (int) divShift);
        long vx = (long) mvCol * (1 << (WARPEDMODEL_PREC_BITS - 3)) - ((long) midX * (out[2] - (1 << WARPEDMODEL_PREC_BITS)) + (long) midY * out[3]);
        long vy = (long) mvRow * (1 << (WARPEDMODEL_PREC_BITS - 3)) - ((long) midX * out[4] + (long) midY * (out[5] - (1 << WARPEDMODEL_PREC_BITS)));
        out[0] = (int) Math.max(-(1 << 23), Math.min((1 << 23) - 1, vx));
        out[1] = (int) Math.max(-(1 << 23), Math.min((1 << 23) - 1, vy));
        return true;
    }

    /**
     * The product of two sample distances of the least squares fit.
     * <p>
     * 최소 제곱 맞춤에서 두 표본 거리의 곱.
     *
     * @param a the first distance / 첫 거리
     * @param b the second distance / 둘째 거리
     * @return the product / 곱
     */
    private static long ls(int a, int b) {
        return (((long) a * b) >> 2) + (a + b);
    }

    /**
     * Divides and clamps an off-diagonal parameter.
     * <p>
     * 대각선이 아닌 매개변수를 나누고 범위를 제한한다.
     *
     * @param v      the numerator / 분자
     * @param factor the divisor factor / 제수 계수
     * @param shift  the divisor shift / 제수 시프트
     * @return the parameter / 매개변수
     */
    private static int nondiag(long v, long factor, int shift) {
        long r = Av1MvStack.round2Signed(v * factor, shift);
        return (int) Math.max(-(1 << 13) + 1, Math.min((1 << 13) - 1, r));
    }

    /**
     * Divides and clamps a diagonal parameter.
     * <p>
     * 대각선 매개변수를 나누고 범위를 제한한다.
     *
     * @param v      the numerator / 분자
     * @param factor the divisor factor / 제수 계수
     * @param shift  the divisor shift / 제수 시프트
     * @return the parameter / 매개변수
     */
    private static int diag(long v, long factor, int shift) {
        long r = Av1MvStack.round2Signed(v * factor, shift);
        return (int) Math.max((1 << WARPEDMODEL_PREC_BITS) - (1 << 13) + 1, Math.min((1 << WARPEDMODEL_PREC_BITS) + (1 << 13) - 1, r));
    }

    /**
     * The block warp process (specification 7.11.3.5): predicts an 8x8 part of a block with an affine model.
     * <p>
     * 블록 워프 과정(규격 7.11.3.5): 아핀 모델로 블록의 8x8 부분을 예측한다.
     *
     * @param ref    the samples of the reference plane / 참조 평면의 샘플
     * @param stride the row stride of the reference plane / 참조 평면의 행 간격
     * @param lastX  the last column of the reference plane / 참조 평면의 마지막 열
     * @param lastY  the last row of the reference plane / 참조 평면의 마지막 행
     * @param wp     the six parameters of the model / 모델의 매개변수 여섯 개
     * @param shear  alpha, beta, gamma and delta of the model / 모델의 alpha, beta, gamma, delta
     * @param subX   the horizontal subsampling of the plane / 평면의 가로 하위 표본화
     * @param subY   the vertical subsampling of the plane / 평면의 세로 하위 표본화
     * @param x      the x position of the block in the plane / 평면에서 블록의 x 위치
     * @param y      the y position of the block in the plane / 평면에서 블록의 y 위치
     * @param i8     the row of the 8x8 part inside the block / 블록 안 8x8 부분의 행
     * @param j8     the column of the 8x8 part / 8x8 부분의 열
     * @param w      the width of the block / 블록 너비
     * @param h      the height of the block / 블록 높이
     * @param round0 the rounding after the horizontal filter / 가로 필터 뒤의 반올림
     * @param round1 the rounding after the vertical filter / 세로 필터 뒤의 반올림
     * @param pred   the prediction array of the block (row stride w) / 블록의 예측 배열(행 간격 w)
     */
    static void blockWarp(short[] ref, int stride, int lastX, int lastY, int[] wp, int[] shear, int subX, int subY, int x, int y, int i8, int j8,
            int w, int h, int round0, int round1, int[] pred) {
        int alpha = shear[0];
        int beta = shear[1];
        int gamma = shear[2];
        int delta = shear[3];
        int srcX = (x + j8 * 8 + 4) << subX;
        int srcY = (y + i8 * 8 + 4) << subY;
        long dstX = (long) wp[2] * srcX + (long) wp[3] * srcY + wp[0];
        long dstY = (long) wp[4] * srcX + (long) wp[5] * srcY + wp[1];
        long x4 = dstX >> subX;
        long y4 = dstY >> subY;
        int ix4 = (int) (x4 >> WARPEDMODEL_PREC_BITS);
        int sx4 = (int) (x4 & ((1 << WARPEDMODEL_PREC_BITS) - 1));
        int iy4 = (int) (y4 >> WARPEDMODEL_PREC_BITS);
        int sy4 = (int) (y4 & ((1 << WARPEDMODEL_PREC_BITS) - 1));
        int[] filters = AvifInterTables.WARPED_FILTERS;
        int[] intermediate = new int[15 * 8];
        int rnd0 = (1 << round0) >> 1;
        // 가로 방향 필터: 위아래 15행, 8열의 중간 값을 만든다.
        for (int i1 = -7; i1 < 8; i1++) {
            int ry = Math.max(0, Math.min(lastY, iy4 + i1));
            // 한 행의 8열마다 가로 필터 값을 구한다.
            for (int i2 = -4; i2 < 4; i2++) {
                int sx = sx4 + alpha * i2 + beta * i1;
                int offs = ((sx + (1 << 9)) >> 10) + 64;
                int s = 0;
                // 8탭 워프 필터를 적용한다.
                for (int i3 = 0; i3 < 8; i3++) {
                    int rx = Math.max(0, Math.min(lastX, ix4 + i2 - 3 + i3));
                    s += filters[offs * 8 + i3] * ref[ry * stride + rx];
                }
                intermediate[(i1 + 7) * 8 + (i2 + 4)] = (s + rnd0) >> round0;
            }
        }
        int rnd1 = (1 << round1) >> 1;
        int limY = Math.min(4, h - i8 * 8 - 4);
        int limX = Math.min(4, w - j8 * 8 - 4);
        // 세로 방향 필터: 8x8 부분의 샘플을 만든다.
        for (int i1 = -4; i1 < limY; i1++) {
            for (int i2 = -4; i2 < limX; i2++) {
                int sy = sy4 + gamma * i2 + delta * i1;
                int offs = ((sy + (1 << 9)) >> 10) + 64;
                int s = 0;
                // 세로 방향 8탭 필터를 중간 값에 적용한다.
                for (int i3 = 0; i3 < 8; i3++) {
                    s += filters[offs * 8 + i3] * intermediate[(i1 + i3 + 4) * 8 + (i2 + 4)];
                }
                pred[(i8 * 8 + i1 + 4) * w + (j8 * 8 + i2 + 4)] = (s + rnd1) >> round1;
            }
        }
    }
}
