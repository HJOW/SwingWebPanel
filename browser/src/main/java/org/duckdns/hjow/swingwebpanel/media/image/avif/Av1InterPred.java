package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The motion compensation of the AV1 decoder (plan item P18-03; AV1 specification section 7.11.3): the motion vector scaling process (7.11.3.3)
 * and the block inter prediction process (7.11.3.4), the two dimensional interpolation of a block of a reference plane. Intra block copy uses
 * it with the plane of the current frame as the reference; the inter frames use it with the reference frames.
 * <p>
 * AV1 디코더의 움직임 보상(계획 P18-03; AV1 규격 7.11.3절): 움직임 벡터 스케일 과정(7.11.3.3)과 블록 인터 예측 과정(7.11.3.4), 즉 참조 평면의 블록을
 * 2차원으로 보간한다. 인트라 블록 복사는 현재 프레임의 평면을 참조로 하여, 인터 프레임은 참조 프레임으로 쓴다.
 */
final class Av1InterPred {

    /**
     * The number of fractional bits of the sub-sample positions of the filter selection.
     * <p>
     * 필터 선택에 쓰는 부표본 위치의 소수 비트 수.
     */
    private static final int SUBPEL_BITS = 4;

    /**
     * The number of fractional bits of the positions in the reference frame.
     * <p>
     * 참조 프레임 안 위치의 소수 비트 수.
     */
    private static final int SCALE_SUBPEL_BITS = 10;

    /**
     * The number of fractional bits of the scale factor of the reference frame.
     * <p>
     * 참조 프레임 스케일 계수의 소수 비트 수.
     */
    private static final int REF_SCALE_SHIFT = 14;

    /**
     * The result of the motion vector scaling process: the position in the reference frame (1/1024 samples) and the steps.
     * <p>
     * 움직임 벡터 스케일 과정의 결과: 참조 프레임 안의 위치(1/1024 샘플)와 간격.
     */
    static final class Position {

        /**
         * The horizontal position of the top left sample, in 1/1024 samples.
         * <p>
         * 왼쪽 위 샘플의 가로 위치, 1/1024 샘플 단위.
         */
        long startX;

        /**
         * The vertical position of the top left sample, in 1/1024 samples.
         * <p>
         * 왼쪽 위 샘플의 세로 위치, 1/1024 샘플 단위.
         */
        long startY;

        /**
         * The horizontal step per predicted sample, in 1/1024 samples.
         * <p>
         * 예측 샘플마다 가로 간격, 1/1024 샘플 단위.
         */
        int stepX;

        /**
         * The vertical step per predicted sample, in 1/1024 samples.
         * <p>
         * 예측 샘플마다 세로 간격, 1/1024 샘플 단위.
         */
        int stepY;
    }

    /**
     * The intermediate rows of the horizontal filter pass.
     * <p>
     * 가로 필터 단계의 중간 행.
     */
    private final int[] intermediate = new int[(2 * 128 + 16 + 8) * 128];

    /**
     * Creates the interpolator (it owns the temporary array, one per tile decoder).
     * <p>
     * 보간기를 만든다(임시 배열을 가지므로 타일 디코더마다 하나).
     */
    Av1InterPred() {
    }

    /**
     * The motion vector scaling process (specification 7.11.3.3).
     * <p>
     * 움직임 벡터 스케일 과정(규격 7.11.3.3).
     *
     * @param out          the result / 결과
     * @param subX         the horizontal subsampling of the plane / 평면의 가로 하위 표본화
     * @param subY         the vertical subsampling of the plane / 평면의 세로 하위 표본화
     * @param x            the x position of the block in the current plane / 현재 평면에서 블록의 x 위치
     * @param y            the y position of the block in the current plane / 현재 평면에서 블록의 y 위치
     * @param mvRow        the vertical motion vector component (1/8 luma samples) / 세로 움직임 벡터 성분(1/8 휘도 샘플)
     * @param mvCol        the horizontal motion vector component / 가로 움직임 벡터 성분
     * @param refUpWidth   the upscaled width of the reference frame / 참조 프레임의 확대된 너비
     * @param refHeight    the height of the reference frame / 참조 프레임의 높이
     * @param frameWidth   the coded width of the current frame / 현재 프레임의 부호화된 너비
     * @param frameHeight  the height of the current frame / 현재 프레임의 높이
     */
    static void scale(Position out, int subX, int subY, int x, int y, int mvRow, int mvCol, int refUpWidth, int refHeight, int frameWidth,
            int frameHeight) {
        long xScale = (((long) refUpWidth << REF_SCALE_SHIFT) + (frameWidth / 2)) / frameWidth;
        long yScale = (((long) refHeight << REF_SCALE_SHIFT) + (frameHeight / 2)) / frameHeight;
        int halfSample = 1 << (SUBPEL_BITS - 1);
        long origX = ((long) x << SUBPEL_BITS) + ((2 * mvCol) >> subX) + halfSample;
        long origY = ((long) y << SUBPEL_BITS) + ((2 * mvRow) >> subY) + halfSample;
        long baseX = origX * xScale - ((long) halfSample << REF_SCALE_SHIFT);
        long baseY = origY * yScale - ((long) halfSample << REF_SCALE_SHIFT);
        int off = (1 << (SCALE_SUBPEL_BITS - SUBPEL_BITS)) / 2;
        int shift = REF_SCALE_SHIFT + SUBPEL_BITS - SCALE_SUBPEL_BITS;
        out.startX = Av1MvStack.round2Signed(baseX, shift) + off;
        out.startY = Av1MvStack.round2Signed(baseY, shift) + off;
        out.stepX = (int) Av1MvStack.round2Signed(xScale, REF_SCALE_SHIFT - SCALE_SUBPEL_BITS);
        out.stepY = (int) Av1MvStack.round2Signed(yScale, REF_SCALE_SHIFT - SCALE_SUBPEL_BITS);
    }

    /**
     * The block inter prediction process (specification 7.11.3.4): predicts a block of a reference plane with the two pass interpolation.
     * <p>
     * 블록 인터 예측 과정(규격 7.11.3.4): 2단계 보간으로 참조 평면의 블록을 예측한다.
     *
     * @param ref       the samples of the reference plane / 참조 평면의 샘플
     * @param stride    the row stride of the reference plane / 참조 평면의 행 간격
     * @param lastX     the x coordinate of the last sample of the reference plane / 참조 평면 마지막 샘플의 x 좌표
     * @param lastY     the y coordinate of the last sample of the reference plane / 참조 평면 마지막 샘플의 y 좌표
     * @param x         the horizontal start position in 1/1024 samples / 가로 시작 위치(1/1024 샘플)
     * @param y         the vertical start position in 1/1024 samples / 세로 시작 위치(1/1024 샘플)
     * @param xStep     the horizontal step in 1/1024 samples / 가로 간격(1/1024 샘플)
     * @param yStep     the vertical step in 1/1024 samples / 세로 간격(1/1024 샘플)
     * @param w         the width of the block / 블록 너비
     * @param h         the height of the block / 블록 높이
     * @param filterH   the interpolation filter of the horizontal direction (interp_filter[1]) / 가로 방향 보간 필터(interp_filter[1])
     * @param filterV   the interpolation filter of the vertical direction (interp_filter[0]) / 세로 방향 보간 필터(interp_filter[0])
     * @param round0    the rounding after the horizontal filter / 가로 필터 뒤의 반올림
     * @param round1    the rounding after the vertical filter / 세로 필터 뒤의 반올림
     * @param pred      the output array (row stride w) / 출력 배열(행 간격 w)
     */
    void predict(short[] ref, int stride, int lastX, int lastY, long x, long y, int xStep, int yStep, int w, int h, int filterH, int filterV,
            int round0, int round1, int[] pred) {
        int intermediateHeight = (((h - 1) * yStep + (1 << SCALE_SUBPEL_BITS) - 1) >> SCALE_SUBPEL_BITS) + 8;
        int fh = filterH;
        if (w <= 4) {
            if (fh == 0 || fh == 2) {
                fh = 4;
            } else if (fh == 1) {
                fh = 5;
            }
        }
        int fv = filterV;
        if (h <= 4) {
            if (fv == 0 || fv == 2) {
                fv = 4;
            } else if (fv == 1) {
                fv = 5;
            }
        }
        int[] filters = AvifInterTables.SUBPEL_FILTERS;
        int rnd0 = (1 << round0) >> 1;
        int y0 = (int) (y >> SCALE_SUBPEL_BITS);
        // 수평 방향으로 먼저 보간하여 중간 배열을 만든다.
        for (int r = 0; r < intermediateHeight; r++) {
            int ry = Math.max(0, Math.min(lastY, y0 + r - 3));
            int rowBase = ry * stride;
            // 예측 블록의 열마다 8탭 필터를 적용한다.
            for (int c = 0; c < w; c++) {
                long p = x + (long) xStep * c;
                int fo = (fh * 16 + (int) ((p >> 6) & 15)) * 8;
                int px = (int) (p >> SCALE_SUBPEL_BITS) - 3;
                int s = 0;
                // 8탭 필터를 적용한다. 참조 평면 밖의 좌표는 가장자리 샘플로 바꾼다.
                for (int t = 0; t < 8; t++) {
                    int rx = px + t;
                    rx = rx < 0 ? 0 : (rx > lastX ? lastX : rx);
                    s += filters[fo + t] * ref[rowBase + rx];
                }
                intermediate[r * w + c] = (s + rnd0) >> round0;
            }
        }
        int rnd1 = (1 << round1) >> 1;
        // 수직 방향으로 보간하여 예측 배열을 만든다.
        for (int r = 0; r < h; r++) {
            long p = (y & 1023) + (long) yStep * r;
            int fo = (fv * 16 + (int) ((p >> 6) & 15)) * 8;
            int base = (int) (p >> SCALE_SUBPEL_BITS);
            // 예측 블록의 열마다 중간 배열의 세로 방향 8탭 필터를 적용한다.
            for (int c = 0; c < w; c++) {
                int s = 0;
                // 세로 방향 필터의 8개 탭을 누적한다.
                for (int t = 0; t < 8; t++) {
                    s += filters[fo + t] * intermediate[(base + t) * w + c];
                }
                pred[r * w + c] = (s + rnd1) >> round1;
            }
        }
    }
}
