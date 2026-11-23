package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The super resolution upscaling of the AV1 decoder (plan item P18-03; AV1 specification section 7.16): a frame that was coded with a reduced
 * width (<code>use_superres</code>) is stretched horizontally to its real width with an 8 tap interpolation filter. The deblocked frame and
 * the frame after CDEF are both upscaled before the loop restoration works on them.
 * <p>
 * AV1 디코더의 초해상도 확대(계획 P18-03; AV1 규격 7.16절): 줄어든 너비로 부호화된 프레임(<code>use_superres</code>)을 8탭 보간 필터로 가로로 늘려 실제
 * 너비로 만든다. 블록 경계를 제거한 프레임과 CDEF 뒤의 프레임을 둘 다 확대한 다음 루프 복원이 그 위에서 동작한다.
 */
final class Av1Upscale {

    /**
     * The number of fractional bits of the position in the upscaling.
     * <p>
     * 확대에서 위치의 소수 비트 수.
     */
    private static final int SUPERRES_SCALE_BITS = 14;

    /**
     * The mask of the fractional bits of the position.
     * <p>
     * 위치의 소수 비트 마스크.
     */
    private static final int SUPERRES_SCALE_MASK = (1 << SUPERRES_SCALE_BITS) - 1;

    /**
     * The difference of the precision of the position and of the filter selection.
     * <p>
     * 위치 정밀도와 필터 선택 정밀도의 차이.
     */
    private static final int SUPERRES_EXTRA_BITS = 8;

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 가진다.
     */
    private Av1Upscale() {
    }

    /**
     * Returns the row strides of the upscaled planes.
     * <p>
     * 확대된 평면의 행 간격을 반환한다.
     *
     * @param f the frame / 프레임
     * @return the stride of each plane / 평면별 행 간격
     */
    static int[] strides(Av1Frame f) {
        int[] s = new int[3];
        int aligned = (f.hdr.upscaledWidth + 127) & ~127;
        // 평면마다 하위 표본화를 반영한 간격을 정한다.
        for (int p = 0; p < f.numPlanes; p++) {
            s[p] = aligned >> (p == 0 ? 0 : f.seq.subsamplingX);
        }
        return s;
    }

    /**
     * Upscales all planes (the upscaling process of the specification).
     * <p>
     * 모든 평면을 확대한다(규격의 확대 과정).
     *
     * @param f         the frame (its strides still describe the input planes) / 프레임(간격은 아직 입력 평면의 것)
     * @param in        the planes before the upscaling / 확대 전의 평면
     * @param outStride the strides of the output planes (see {@link #strides}) / 출력 평면의 간격({@link #strides} 참조)
     * @return the upscaled planes / 확대된 평면
     */
    static short[][] apply(Av1Frame f, short[][] in, int[] outStride) {
        Av1FrameHeader hdr = f.hdr;
        short[][] out = new short[3][];
        int maxVal = (1 << f.seq.bitDepth) - 1;
        int[] filter = AvifTables.UPSCALE_FILTER;
        // 평면마다 행을 따라 가로로 확대한다.
        for (int plane = 0; plane < f.numPlanes; plane++) {
            int subX = plane == 0 ? 0 : f.seq.subsamplingX;
            int subY = plane == 0 ? 0 : f.seq.subsamplingY;
            int downW = (hdr.frameWidth + subX) >> subX;
            int upW = (hdr.upscaledWidth + subX) >> subX;
            int planeH = (hdr.frameHeight + subY) >> subY;
            int stepX = ((downW << SUPERRES_SCALE_BITS) + (upW / 2)) / upW;
            int err = (upW * stepX) - (downW << SUPERRES_SCALE_BITS);
            int initialSubpelX = (-((upW - downW) << (SUPERRES_SCALE_BITS - 1)) + upW / 2) / upW + (1 << (SUPERRES_EXTRA_BITS - 1)) - err / 2;
            initialSubpelX &= SUPERRES_SCALE_MASK;
            int miW = f.miCols >> subX;
            int maxX = miW * 4 - 1;
            int inStride = f.stride[plane];
            short[] src = in[plane];
            short[] dst = new short[outStride[plane] * planeH];
            // 출력 행마다 모든 열의 위치를 계산해 8탭 필터를 적용한다.
            for (int y = 0; y < planeH; y++) {
                int srcRow = y * inStride;
                int dstRow = y * outStride[plane];
                // 출력 행의 열마다 확대 전의 위치(분수 포함)를 구한다.
                for (int x = 0; x < upW; x++) {
                    int srcX = -(1 << SUPERRES_SCALE_BITS) + initialSubpelX + x * stepX;
                    int srcXPx = srcX >> SUPERRES_SCALE_BITS;
                    int subpel = (srcX & SUPERRES_SCALE_MASK) >> SUPERRES_EXTRA_BITS;
                    int sum = 0;
                    // 필터 8개 탭을 가장자리로 자른 위치의 샘플에 적용한다.
                    for (int k = 0; k < 8; k++) {
                        int sampleX = srcXPx + (k - 3);
                        sampleX = sampleX < 0 ? 0 : (sampleX > maxX ? maxX : sampleX);
                        sum += src[srcRow + sampleX] * filter[subpel * 8 + k];
                    }
                    int v = (sum + 64) >> 7;
                    dst[dstRow + x] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
                }
            }
            out[plane] = dst;
        }
        return out;
    }
}
