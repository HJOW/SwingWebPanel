package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * The intra predictors and the inverse transforms of VP8 (RFC 6386 sections 12 and 14). All methods work on a plane with a border (see
 * {@link Vp8Frame}): the pixels above, to the left and above-left of a block are read from the plane itself, where the border holds the
 * fixed values 127 (above) and 129 (left) that the specification prescribes for pixels outside of the picture. A block of 16x16 luma or 8x8
 * chroma pixels is predicted as a whole; a luma subblock of 4x4 pixels is predicted from an explicit array of neighbour pixels because the
 * pixels above and to the right of the subblocks of the right column are not the ones that lie there in the plane.
 * <p>
 * VP8 의 인트라 예측기와 역변환(RFC 6386 12, 14절). 모든 메서드는 테두리가 있는 평면({@link Vp8Frame} 참조)에서 동작한다: 블록의 위, 왼쪽,
 * 왼쪽 위 픽셀은 평면에서 직접 읽으며 테두리에는 그림 바깥 픽셀에 대해 규격이 정한 값 127(위)과 129(왼쪽)이 들어 있다. 16x16 휘도나 8x8 색차 블록은
 * 통째로 예측하고 4x4 휘도 서브블록은 이웃 픽셀 배열에서 예측한다. 오른쪽 열 서브블록의 위-오른쪽 픽셀은 평면에서 그 자리에 있는 픽셀이 아니기 때문이다.
 */
final class Vp8Predict {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Vp8Predict() {
    }

    /**
     * Clamps a value to 0..255.
     * <p>
     * 값을 0..255 로 자른다.
     *
     * @param v value / 값
     * @return clamped value / 자른 값
     */
    static int clamp255(int v) {
        return (v & ~255) == 0 ? v : (v < 0 ? 0 : 255);
    }

    /**
     * Predicts a square block of luma (16) or chroma (8) pixels with one of the four macroblock modes.
     * <p>
     * 정사각형 휘도(16) 또는 색차(8) 블록을 매크로블록 모드 넷 중 하나로 예측한다.
     *
     * @param plane    plane with border / 테두리가 있는 평면
     * @param base     index of the top-left pixel of the block / 블록의 왼쪽 위 픽셀 색인
     * @param stride   row distance / 행 사이 거리
     * @param size     block size, 16 or 8 / 블록 크기(16 또는 8)
     * @param mode     DC_PRED, V_PRED, H_PRED or TM_PRED / DC_PRED, V_PRED, H_PRED, TM_PRED 중 하나
     * @param haveTop  whether the block has a neighbour above (not in the first row) / 위쪽에 이웃이 있는지(첫 행이 아닌지)
     * @param haveLeft whether the block has a neighbour to the left (not in the first column) / 왼쪽에 이웃이 있는지(첫 열이 아닌지)
     */
    static void predictBlock(byte[] plane, int base, int stride, int size, int mode, boolean haveTop, boolean haveLeft) {
        int above = base - stride;
        switch (mode) {
        case Vp8ModeTables.DC_PRED: {
            int dc;
            int shift = size == 16 ? 3 : 2;
            if (haveTop && haveLeft) {
                int sum = 0;
                // 위쪽 한 행과 왼쪽 한 열의 픽셀을 모두 더한다.
                for (int i = 0; i < size; i++) {
                    sum += (plane[above + i] & 0xff) + (plane[base + i * stride - 1] & 0xff);
                }
                dc = (sum + size) >> (shift + 2);
            } else if (haveTop || haveLeft) {
                int sum = 0;
                // 가능한 한쪽 이웃의 픽셀만 더한다.
                for (int i = 0; i < size; i++) {
                    sum += haveTop ? (plane[above + i] & 0xff) : (plane[base + i * stride - 1] & 0xff);
                }
                dc = (sum + (size >> 1)) >> (shift + 1);
            } else {
                dc = 128;
            }
            // 블록 전체를 DC 값으로 채운다.
            for (int r = 0; r < size; r++) {
                int row = base + r * stride;
                // 한 행의 모든 열을 DC 값으로 채운다.
                for (int c = 0; c < size; c++) {
                    plane[row + c] = (byte) dc;
                }
            }
            break;
        }
        case Vp8ModeTables.V_PRED:
            // 위쪽 행을 아래로 복사한다.
            for (int r = 0; r < size; r++) {
                System.arraycopy(plane, above, plane, base + r * stride, size);
            }
            break;
        case Vp8ModeTables.H_PRED:
            // 왼쪽 열의 각 픽셀을 그 행 전체에 복사한다.
            for (int r = 0; r < size; r++) {
                int row = base + r * stride;
                byte left = plane[row - 1];
                // 한 행의 모든 열에 왼쪽 픽셀 값을 쓴다.
                for (int c = 0; c < size; c++) {
                    plane[row + c] = left;
                }
            }
            break;
        default: {
            int topLeft = plane[above - 1] & 0xff;
            // 각 픽셀을 왼쪽 + 위쪽 - 왼쪽위 로 예측한다.
            for (int r = 0; r < size; r++) {
                int row = base + r * stride;
                int left = (plane[row - 1] & 0xff) - topLeft;
                // 한 행의 모든 열을 위쪽 픽셀과 기울기로 예측한다.
                for (int c = 0; c < size; c++) {
                    plane[row + c] = (byte) clamp255(left + (plane[above + c] & 0xff));
                }
            }
            break;
        }
        }
    }

    /**
     * Predicts a 4x4 luma subblock from its neighbour pixels (RFC 6386 section 12.3). The result goes to out[row * 4 + column].
     * <p>
     * 4x4 휘도 서브블록을 이웃 픽셀로 예측한다(RFC 6386 12.3절). 결과는 out[행 * 4 + 열] 에 들어간다.
     *
     * @param mode subblock mode B_DC_PRED..B_HU_PRED / 서브블록 모드 B_DC_PRED..B_HU_PRED
     * @param a    the 8 pixels above and above-right / 위와 위-오른쪽의 8픽셀
     * @param l    the 4 pixels to the left / 왼쪽의 4픽셀
     * @param p    the pixel above-left / 왼쪽 위 픽셀
     * @param out  16 predicted pixels / 예측된 16픽셀
     */
    static void predict4x4(int mode, int[] a, int[] l, int p, int[] out) {
        switch (mode) {
        case Vp8ModeTables.B_DC_PRED: {
            int dc = (a[0] + a[1] + a[2] + a[3] + l[0] + l[1] + l[2] + l[3] + 4) >> 3;
            // 16개 모두 평균값으로 채운다.
            for (int i = 0; i < 16; i++) {
                out[i] = dc;
            }
            break;
        }
        case Vp8ModeTables.B_TM_PRED:
            // 각 픽셀을 왼쪽 + 위쪽 - 왼쪽위 로 예측한다.
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    out[r * 4 + c] = clamp255(l[r] + a[c] - p);
                }
            }
            break;
        case Vp8ModeTables.B_VE_PRED: {
            // 위쪽 행을 [1 2 1] 필터로 다듬어 모든 행에 복사한다.
            for (int c = 0; c < 4; c++) {
                int prev = c == 0 ? p : a[c - 1];
                int v = (prev + 2 * a[c] + a[c + 1] + 2) >> 2;
                // 네 행에 같은 값을 쓴다.
                for (int r = 0; r < 4; r++) {
                    out[r * 4 + c] = v;
                }
            }
            break;
        }
        case Vp8ModeTables.B_HE_PRED: {
            int h0 = (p + 2 * l[0] + l[1] + 2) >> 2;
            int h1 = (l[0] + 2 * l[1] + l[2] + 2) >> 2;
            int h2 = (l[1] + 2 * l[2] + l[3] + 2) >> 2;
            int h3 = (l[2] + 2 * l[3] + l[3] + 2) >> 2;
            // 왼쪽 열을 다듬은 값을 각 행에 채운다.
            for (int c = 0; c < 4; c++) {
                out[c] = h0;
                out[4 + c] = h1;
                out[8 + c] = h2;
                out[12 + c] = h3;
            }
            break;
        }
        case Vp8ModeTables.B_LD_PRED: {
            int p0 = (a[0] + 2 * a[1] + a[2] + 2) >> 2;
            int p1 = (a[1] + 2 * a[2] + a[3] + 2) >> 2;
            int p2 = (a[2] + 2 * a[3] + a[4] + 2) >> 2;
            int p3 = (a[3] + 2 * a[4] + a[5] + 2) >> 2;
            int p4 = (a[4] + 2 * a[5] + a[6] + 2) >> 2;
            int p5 = (a[5] + 2 * a[6] + a[7] + 2) >> 2;
            int p6 = (a[6] + 2 * a[7] + a[7] + 2) >> 2;
            fill(out, p0, p1, p2, p3, p1, p2, p3, p4, p2, p3, p4, p5, p3, p4, p5, p6);
            break;
        }
        case Vp8ModeTables.B_RD_PRED: {
            int p0 = (l[0] + 2 * p + a[0] + 2) >> 2;
            int p1 = (p + 2 * a[0] + a[1] + 2) >> 2;
            int p2 = (a[0] + 2 * a[1] + a[2] + 2) >> 2;
            int p3 = (a[1] + 2 * a[2] + a[3] + 2) >> 2;
            int p4 = (l[1] + 2 * l[0] + p + 2) >> 2;
            int p5 = (l[2] + 2 * l[1] + l[0] + 2) >> 2;
            int p6 = (l[3] + 2 * l[2] + l[1] + 2) >> 2;
            fill(out, p0, p1, p2, p3, p4, p0, p1, p2, p5, p4, p0, p1, p6, p5, p4, p0);
            break;
        }
        case Vp8ModeTables.B_VR_PRED: {
            int p0 = (p + a[0] + 1) >> 1;
            int p1 = (a[0] + a[1] + 1) >> 1;
            int p2 = (a[1] + a[2] + 1) >> 1;
            int p3 = (a[2] + a[3] + 1) >> 1;
            int p4 = (l[0] + 2 * p + a[0] + 2) >> 2;
            int p5 = (p + 2 * a[0] + a[1] + 2) >> 2;
            int p6 = (a[0] + 2 * a[1] + a[2] + 2) >> 2;
            int p7 = (a[1] + 2 * a[2] + a[3] + 2) >> 2;
            int p8 = (l[1] + 2 * l[0] + p + 2) >> 2;
            int p9 = (l[2] + 2 * l[1] + l[0] + 2) >> 2;
            fill(out, p0, p1, p2, p3, p4, p5, p6, p7, p8, p0, p1, p2, p9, p4, p5, p6);
            break;
        }
        case Vp8ModeTables.B_VL_PRED: {
            int p0 = (a[0] + a[1] + 1) >> 1;
            int p1 = (a[1] + a[2] + 1) >> 1;
            int p2 = (a[2] + a[3] + 1) >> 1;
            int p3 = (a[3] + a[4] + 1) >> 1;
            int p4 = (a[0] + 2 * a[1] + a[2] + 2) >> 2;
            int p5 = (a[1] + 2 * a[2] + a[3] + 2) >> 2;
            int p6 = (a[2] + 2 * a[3] + a[4] + 2) >> 2;
            int p7 = (a[3] + 2 * a[4] + a[5] + 2) >> 2;
            int p8 = (a[4] + 2 * a[5] + a[6] + 2) >> 2;
            int p9 = (a[5] + 2 * a[6] + a[7] + 2) >> 2;
            fill(out, p0, p1, p2, p3, p4, p5, p6, p7, p1, p2, p3, p8, p5, p6, p7, p9);
            break;
        }
        case Vp8ModeTables.B_HD_PRED: {
            int p0 = (l[0] + p + 1) >> 1;
            int p1 = (l[0] + 2 * p + a[0] + 2) >> 2;
            int p2 = (p + 2 * a[0] + a[1] + 2) >> 2;
            int p3 = (a[0] + 2 * a[1] + a[2] + 2) >> 2;
            int p4 = (l[1] + l[0] + 1) >> 1;
            int p5 = (l[1] + 2 * l[0] + p + 2) >> 2;
            int p6 = (l[2] + l[1] + 1) >> 1;
            int p7 = (l[2] + 2 * l[1] + l[0] + 2) >> 2;
            int p8 = (l[3] + l[2] + 1) >> 1;
            int p9 = (l[3] + 2 * l[2] + l[1] + 2) >> 2;
            fill(out, p0, p1, p2, p3, p4, p5, p0, p1, p6, p7, p4, p5, p8, p9, p6, p7);
            break;
        }
        default: {
            int p0 = (l[0] + l[1] + 1) >> 1;
            int p1 = (l[0] + 2 * l[1] + l[2] + 2) >> 2;
            int p2 = (l[1] + l[2] + 1) >> 1;
            int p3 = (l[1] + 2 * l[2] + l[3] + 2) >> 2;
            int p4 = (l[2] + l[3] + 1) >> 1;
            int p5 = (l[2] + 2 * l[3] + l[3] + 2) >> 2;
            int p6 = l[3];
            fill(out, p0, p1, p2, p3, p2, p3, p4, p5, p4, p5, p6, p6, p6, p6, p6, p6);
            break;
        }
        }
    }

    /**
     * Stores 16 values into an array.
     * <p>
     * 값 16개를 배열에 저장한다.
     *
     * @param o   destination / 목적지
     * @param v0  value 0 / 값 0
     * @param v1  value 1 / 값 1
     * @param v2  value 2 / 값 2
     * @param v3  value 3 / 값 3
     * @param v4  value 4 / 값 4
     * @param v5  value 5 / 값 5
     * @param v6  value 6 / 값 6
     * @param v7  value 7 / 값 7
     * @param v8  value 8 / 값 8
     * @param v9  value 9 / 값 9
     * @param v10 value 10 / 값 10
     * @param v11 value 11 / 값 11
     * @param v12 value 12 / 값 12
     * @param v13 value 13 / 값 13
     * @param v14 value 14 / 값 14
     * @param v15 value 15 / 값 15
     */
    private static void fill(int[] o, int v0, int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8, int v9, int v10, int v11,
            int v12, int v13, int v14, int v15) {
        o[0] = v0;
        o[1] = v1;
        o[2] = v2;
        o[3] = v3;
        o[4] = v4;
        o[5] = v5;
        o[6] = v6;
        o[7] = v7;
        o[8] = v8;
        o[9] = v9;
        o[10] = v10;
        o[11] = v11;
        o[12] = v12;
        o[13] = v13;
        o[14] = v14;
        o[15] = v15;
    }

    /**
     * Inverse Walsh-Hadamard transform of the Y2 block: the 16 results are the DC coefficients of the 16 luma blocks (RFC 6386 section 14.3).
     * <p>
     * Y2 블록의 역 월시-아다마르 변환: 16개 결과는 16개 휘도 블록의 DC 계수이다(RFC 6386 14.3절).
     *
     * @param in  16 dequantized Y2 coefficients in raster order, at offset inOff / 래스터 순서의 역양자화된 Y2 계수 16개(inOff 위치)
     * @param inOff offset of the first coefficient / 첫 계수의 위치
     * @param out 16 results / 결과 16개
     */
    static void inverseWalsh(int[] in, int inOff, int[] out) {
        int[] tmp = new int[16];
        // 첫 단계: 열 방향으로 변환한다.
        for (int i = 0; i < 4; i++) {
            int a1 = in[inOff + i] + in[inOff + 12 + i];
            int b1 = in[inOff + 4 + i] + in[inOff + 8 + i];
            int c1 = in[inOff + 4 + i] - in[inOff + 8 + i];
            int d1 = in[inOff + i] - in[inOff + 12 + i];
            tmp[i] = a1 + b1;
            tmp[4 + i] = c1 + d1;
            tmp[8 + i] = a1 - b1;
            tmp[12 + i] = d1 - c1;
        }
        // 둘째 단계: 행 방향으로 변환하고 반올림해 3비트 줄인다.
        for (int i = 0; i < 4; i++) {
            int a1 = tmp[4 * i] + tmp[4 * i + 3];
            int b1 = tmp[4 * i + 1] + tmp[4 * i + 2];
            int c1 = tmp[4 * i + 1] - tmp[4 * i + 2];
            int d1 = tmp[4 * i] - tmp[4 * i + 3];
            out[4 * i] = (a1 + b1 + 3) >> 3;
            out[4 * i + 1] = (c1 + d1 + 3) >> 3;
            out[4 * i + 2] = (a1 - b1 + 3) >> 3;
            out[4 * i + 3] = (d1 - c1 + 3) >> 3;
        }
    }

    /**
     * Inverse DCT of one 4x4 block of dequantized coefficients, added to the prediction that is already in the plane (RFC 6386 section 14.4).
     * <p>
     * 역양자화된 4x4 계수 블록의 역 DCT 를 평면에 이미 있는 예측값에 더한다(RFC 6386 14.4절).
     *
     * @param c      coefficients in raster order / 래스터 순서의 계수
     * @param cOff   offset of the first coefficient / 첫 계수의 위치
     * @param plane  plane with the prediction; receives the result / 예측값이 든 평면. 결과를 받는다
     * @param base   index of the top-left pixel of the block / 블록의 왼쪽 위 픽셀 색인
     * @param stride row distance / 행 사이 거리
     */
    static void inverseDctAdd(int[] c, int cOff, byte[] plane, int base, int stride) {
        int[] tmp = new int[16];
        // 첫 단계: 열 방향으로 변환한다.
        for (int i = 0; i < 4; i++) {
            int i0 = c[cOff + i];
            int i4 = c[cOff + 4 + i];
            int i8 = c[cOff + 8 + i];
            int i12 = c[cOff + 12 + i];
            int a1 = i0 + i8;
            int b1 = i0 - i8;
            int t1 = (i4 * 35468) >> 16;
            int t2 = i12 + ((i12 * 20091) >> 16);
            int c1 = t1 - t2;
            t1 = i4 + ((i4 * 20091) >> 16);
            t2 = (i12 * 35468) >> 16;
            int d1 = t1 + t2;
            tmp[i] = a1 + d1;
            tmp[12 + i] = a1 - d1;
            tmp[4 + i] = b1 + c1;
            tmp[8 + i] = b1 - c1;
        }
        // 둘째 단계: 행 방향으로 변환해 예측값에 더한다.
        for (int i = 0; i < 4; i++) {
            int t0 = tmp[4 * i];
            int t1v = tmp[4 * i + 1];
            int t2v = tmp[4 * i + 2];
            int t3 = tmp[4 * i + 3];
            int a1 = t0 + t2v;
            int b1 = t0 - t2v;
            int x1 = (t1v * 35468) >> 16;
            int x2 = t3 + ((t3 * 20091) >> 16);
            int c1 = x1 - x2;
            x1 = t1v + ((t1v * 20091) >> 16);
            x2 = (t3 * 35468) >> 16;
            int d1 = x1 + x2;
            int row = base + i * stride;
            plane[row] = (byte) clamp255((plane[row] & 0xff) + ((a1 + d1 + 4) >> 3));
            plane[row + 3] = (byte) clamp255((plane[row + 3] & 0xff) + ((a1 - d1 + 4) >> 3));
            plane[row + 1] = (byte) clamp255((plane[row + 1] & 0xff) + ((b1 + c1 + 4) >> 3));
            plane[row + 2] = (byte) clamp255((plane[row + 2] & 0xff) + ((b1 - c1 + 4) >> 3));
        }
    }

    /**
     * Adds the effect of a block that has only a DC coefficient (the same result as {@link #inverseDctAdd}, faster).
     * <p>
     * DC 계수만 있는 블록의 효과를 더한다({@link #inverseDctAdd} 와 결과가 같고 더 빠르다).
     *
     * @param dc     the dequantized DC coefficient / 역양자화된 DC 계수
     * @param plane  plane with the prediction / 예측값이 든 평면
     * @param base   index of the top-left pixel of the block / 블록의 왼쪽 위 픽셀 색인
     * @param stride row distance / 행 사이 거리
     */
    static void dcOnlyAdd(int dc, byte[] plane, int base, int stride) {
        int add = (dc + 4) >> 3;
        // 4x4 픽셀 모두에 같은 값을 더한다.
        for (int r = 0; r < 4; r++) {
            int row = base + r * stride;
            // 한 행의 네 픽셀에 잔차를 더한다.
            for (int c = 0; c < 4; c++) {
                plane[row + c] = (byte) clamp255((plane[row + c] & 0xff) + add);
            }
        }
    }
}
