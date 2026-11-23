package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * A decoded VP8 frame in planar YUV 4:2:0 form together with the conversion to ARGB pixels (plan item P18-03). The planes carry a border
 * (one row above and one column to the left of the picture, and padding to the right) because the intra predictors read the pixels around a
 * macroblock; {@link #yIndex(int, int)} and {@link #uvIndex(int, int)} give the array index of a pixel. The conversion uses the BT.601
 * "studio range" matrix with 6-bit fixed-point arithmetic and the "fancy" bilinear upsampling of the chroma planes (the weights 9/16,
 * 3/16, 3/16, 1/16), which is also what browsers do with WebP.
 * <p>
 * 평면 YUV 4:2:0 형태로 디코딩된 VP8 프레임과 ARGB 픽셀로의 변환(계획 P18-03). 인트라 예측기가 매크로블록 주변 픽셀을 읽으므로 평면에는 테두리
 * (그림 위의 한 행, 왼쪽의 한 열, 오른쪽 여백)가 있으며 {@link #yIndex(int, int)}, {@link #uvIndex(int, int)} 가 픽셀의 배열 색인을 준다.
 * 변환은 BT.601 "스튜디오 범위" 행렬을 6비트 고정소수점으로 쓰고 색차 평면은 "fancy" 이중선형 업샘플링(가중치 9/16, 3/16, 3/16, 1/16)을 쓴다.
 * 브라우저가 WebP 에 하는 것과 같다.
 */
final class Vp8Frame {

    /**
     * Width of the picture in pixels.
     * <p>
     * 그림의 가로 픽셀 수.
     */
    final int width;

    /**
     * Height of the picture in pixels.
     * <p>
     * 그림의 세로 픽셀 수.
     */
    final int height;

    /**
     * Number of macroblock columns.
     * <p>
     * 매크로블록 열 수.
     */
    final int mbCols;

    /**
     * Number of macroblock rows.
     * <p>
     * 매크로블록 행 수.
     */
    final int mbRows;

    /**
     * Luma plane with border.
     * <p>
     * 테두리가 있는 휘도 평면.
     */
    final byte[] y;

    /**
     * Distance between two rows of the luma plane.
     * <p>
     * 휘도 평면의 행 사이 거리.
     */
    final int yStride;

    /**
     * Blue-difference chroma plane with border.
     * <p>
     * 테두리가 있는 청색 색차 평면.
     */
    final byte[] u;

    /**
     * Red-difference chroma plane with border.
     * <p>
     * 테두리가 있는 적색 색차 평면.
     */
    final byte[] v;

    /**
     * Distance between two rows of a chroma plane.
     * <p>
     * 색차 평면의 행 사이 거리.
     */
    final int uvStride;

    /**
     * Allocates the planes for a picture size and fills the borders with the values the predictors expect: 127 for the row above the
     * picture (including the corner) and 129 for the column left of it.
     * <p>
     * 그림 크기에 맞게 평면을 할당하고 테두리를 예측기가 기대하는 값으로 채운다: 그림 위의 행(모서리 포함)은 127, 왼쪽 열은 129.
     *
     * @param width  picture width / 그림 가로
     * @param height picture height / 그림 세로
     */
    Vp8Frame(int width, int height) {
        this.width = width;
        this.height = height;
        this.mbCols = (width + 15) >> 4;
        this.mbRows = (height + 15) >> 4;
        this.yStride = mbCols * 16 + 32;
        this.uvStride = mbCols * 8 + 32;
        this.y = new byte[(mbRows * 16 + 1) * yStride];
        this.u = new byte[(mbRows * 8 + 1) * uvStride];
        this.v = new byte[(mbRows * 8 + 1) * uvStride];
        initBorder(y, yStride, mbRows * 16);
        initBorder(u, uvStride, mbRows * 8);
        initBorder(v, uvStride, mbRows * 8);
    }

    /**
     * Fills the border of one plane.
     * <p>
     * 평면 하나의 테두리를 채운다.
     *
     * @param plane  plane / 평면
     * @param stride row distance / 행 사이 거리
     * @param rows   number of picture rows / 그림 행 수
     */
    private static void initBorder(byte[] plane, int stride, int rows) {
        // 위 테두리 행 전체(모서리 포함)를 127 로 채운다.
        for (int x = 0; x < stride; x++) {
            plane[x] = 127;
        }
        // 왼쪽 테두리 열을 129 로 채운다.
        for (int r = 0; r < rows; r++) {
            plane[(r + 1) * stride] = (byte) 129;
        }
    }

    /**
     * Returns the array index of a luma pixel (x and y may be -1 for the border).
     * <p>
     * 휘도 픽셀의 배열 색인을 반환한다(테두리는 x, y 가 -1 일 수 있다).
     *
     * @param px column / 열
     * @param py row / 행
     * @return index / 색인
     */
    int yIndex(int px, int py) {
        return (py + 1) * yStride + 1 + px;
    }

    /**
     * Returns the array index of a chroma pixel (x and y may be -1 for the border).
     * <p>
     * 색차 픽셀의 배열 색인을 반환한다(테두리는 x, y 가 -1 일 수 있다).
     *
     * @param px column / 열
     * @param py row / 행
     * @return index / 색인
     */
    int uvIndex(int px, int py) {
        return (py + 1) * uvStride + 1 + px;
    }

    /**
     * Clips a fixed-point value (6 fractional bits) to 0..255.
     * <p>
     * 고정소수점 값(소수 6비트)을 0..255 로 자른다.
     *
     * @param value fixed-point value / 고정소수점 값
     * @return 0..255 / 0..255
     */
    private static int clip8(int value) {
        if ((value & ~16383) == 0) {
            return value >> 6;
        }
        return value < 0 ? 0 : 255;
    }

    /**
     * Converts one YUV sample to a packed opaque ARGB pixel.
     * <p>
     * YUV 샘플 하나를 불투명 ARGB 픽셀로 변환한다.
     *
     * @param yy luma / 휘도
     * @param uu blue difference / 청색 색차
     * @param vv red difference / 적색 색차
     * @return <code>0xFFRRGGBB</code> / <code>0xFFRRGGBB</code>
     */
    static int yuvToArgb(int yy, int uu, int vv) {
        int y1 = (yy * 19077) >> 8;
        int r = clip8(y1 + ((vv * 26149) >> 8) - 14234);
        int g = clip8(y1 - ((uu * 6419) >> 8) - ((vv * 13320) >> 8) + 8708);
        int b = clip8(y1 + ((uu * 33050) >> 8) - 17685);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Converts the picture to ARGB pixels (row by row, <code>width * height</code> values).
     * <p>
     * 그림을 ARGB 픽셀로 변환한다(행 순서, <code>가로 * 세로</code> 개).
     *
     * @param alpha alpha plane of <code>width * height</code> bytes, or null for an opaque picture / <code>가로 * 세로</code> 바이트의 알파 평면. 불투명이면 null
     * @return pixels, non-premultiplied / 픽셀(알파를 미리 곱하지 않음)
     */
    int[] toArgb(byte[] alpha) {
        int[] out = new int[width * height];
        int cw = (width + 1) >> 1;
        int[] topU = new int[cw];
        int[] topV = new int[cw];
        int[] curU = new int[cw];
        int[] curV = new int[cw];
        loadChroma(0, topU, topV);
        System.arraycopy(topU, 0, curU, 0, cw);
        System.arraycopy(topV, 0, curV, 0, cw);
        // 첫 행은 위쪽 색차와 현재 색차가 같은 행이다.
        upsampleRow(0, -1, topU, topV, curU, curV, out);
        // 둘째 행부터는 (2k-1, 2k) 행 쌍을 색차 행 k-1, k 로 보간한다.
        for (int k = 1; 2 * k - 1 < height; k++) {
            int topRow = 2 * k - 1;
            int botRow = 2 * k;
            loadChroma(k - 1, topU, topV);
            if (botRow < height) {
                loadChroma(k, curU, curV);
                upsampleRow(topRow, botRow, topU, topV, curU, curV, out);
            } else {
                // 높이가 짝수일 때 마지막 행은 마지막 색차 행만으로 보간한다.
                System.arraycopy(topU, 0, curU, 0, cw);
                System.arraycopy(topV, 0, curV, 0, cw);
                upsampleRow(topRow, -1, topU, topV, curU, curV, out);
            }
        }
        if (alpha != null) {
            // 알파 평면이 있으면 각 픽셀의 알파를 채운다.
            for (int i = 0; i < out.length; i++) {
                out[i] = (out[i] & 0x00ffffff) | ((alpha[i] & 0xff) << 24);
            }
        }
        return out;
    }

    /**
     * Copies one row of the chroma planes into int arrays.
     * <p>
     * 색차 평면의 한 행을 int 배열로 복사한다.
     *
     * @param row chroma row / 색차 행
     * @param du  destination for U / U 의 목적지
     * @param dv  destination for V / V 의 목적지
     */
    private void loadChroma(int row, int[] du, int[] dv) {
        int base = uvIndex(0, row);
        // 한 행의 색차 샘플을 부호 없는 값으로 옮긴다.
        for (int x = 0; x < du.length; x++) {
            du[x] = u[base + x] & 0xff;
            dv[x] = v[base + x] & 0xff;
        }
    }

    /**
     * Produces one or two output rows from two chroma rows with the 9-3-3-1 interpolation of the "fancy" upsampler.
     * <p>
     * 두 색차 행으로 "fancy" 업샘플러의 9-3-3-1 보간을 써서 출력 행 하나 또는 둘을 만든다.
     *
     * @param topRow output row that is closer to the upper chroma row / 위쪽 색차 행에 가까운 출력 행
     * @param botRow output row that is closer to the lower chroma row, or -1 if there is none / 아래쪽 색차 행에 가까운 출력 행. 없으면 -1
     * @param tu     upper chroma row, U / 위쪽 색차 행의 U
     * @param tv     upper chroma row, V / 위쪽 색차 행의 V
     * @param cu     lower chroma row, U / 아래쪽 색차 행의 U
     * @param cv     lower chroma row, V / 아래쪽 색차 행의 V
     * @param out    output pixels / 출력 픽셀
     */
    private void upsampleRow(int topRow, int botRow, int[] tu, int[] tv, int[] cu, int[] cv, int[] out) {
        int topY = yIndex(0, topRow);
        int botY = botRow >= 0 ? yIndex(0, botRow) : 0;
        int topOut = topRow * width;
        int botOut = botRow >= 0 ? botRow * width : 0;
        int len = width;
        int lastPair = (len - 1) >> 1;
        int tlU = tu[0];
        int tlV = tv[0];
        int lU = cu[0];
        int lV = cv[0];
        out[topOut] = yuvToArgb(y[topY] & 0xff, (3 * tlU + lU + 2) >> 2, (3 * tlV + lV + 2) >> 2);
        if (botRow >= 0) {
            out[botOut] = yuvToArgb(y[botY] & 0xff, (3 * lU + tlU + 2) >> 2, (3 * lV + tlV + 2) >> 2);
        }
        // 가로 방향으로 색차 샘플 두 개 사이의 출력 픽셀 둘씩 보간한다.
        for (int x = 1; x <= lastPair; x++) {
            int tU = tu[x];
            int tV = tv[x];
            int cU = cu[x];
            int cV = cv[x];
            int avgU = tlU + tU + lU + cU + 8;
            int avgV = tlV + tV + lV + cV + 8;
            int d12U = (avgU + 2 * (tU + lU)) >> 3;
            int d12V = (avgV + 2 * (tV + lV)) >> 3;
            int d03U = (avgU + 2 * (tlU + cU)) >> 3;
            int d03V = (avgV + 2 * (tlV + cV)) >> 3;
            out[topOut + 2 * x - 1] = yuvToArgb(y[topY + 2 * x - 1] & 0xff, (d12U + tlU) >> 1, (d12V + tlV) >> 1);
            out[topOut + 2 * x] = yuvToArgb(y[topY + 2 * x] & 0xff, (d03U + tU) >> 1, (d03V + tV) >> 1);
            if (botRow >= 0) {
                out[botOut + 2 * x - 1] = yuvToArgb(y[botY + 2 * x - 1] & 0xff, (d03U + lU) >> 1, (d03V + lV) >> 1);
                out[botOut + 2 * x] = yuvToArgb(y[botY + 2 * x] & 0xff, (d12U + cU) >> 1, (d12V + cV) >> 1);
            }
            tlU = tU;
            tlV = tV;
            lU = cU;
            lV = cV;
        }
        // 가로 길이가 짝수이면 마지막 픽셀 하나가 남는다.
        if ((len & 1) == 0) {
            out[topOut + len - 1] = yuvToArgb(y[topY + len - 1] & 0xff, (3 * tlU + lU + 2) >> 2, (3 * tlV + lV + 2) >> 2);
            if (botRow >= 0) {
                out[botOut + len - 1] = yuvToArgb(y[botY + len - 1] & 0xff, (3 * lU + tlU + 2) >> 2, (3 * lV + tlV + 2) >> 2);
            }
        }
    }
}
