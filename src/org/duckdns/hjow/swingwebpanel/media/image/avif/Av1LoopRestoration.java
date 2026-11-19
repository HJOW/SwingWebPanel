package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The loop restoration filters of AV1 (plan item P18-03; AV1 specification section 7.17): per restoration unit either a separable Wiener
 * filter or the self guided filter (two box filters whose outputs are projected with coded weights) is applied to the CDEF output. The
 * filter works in horizontal stripes of 64 luma rows (offset up by 8 rows); samples of other stripes come from the frame before CDEF, as the
 * specification requires. The specification describes 4x4 blocks; because the output is the same for any larger block that stays inside one
 * stripe and one unit, this implementation filters whole stripe by unit rectangles.
 * <p>
 * AV1 의 루프 복원 필터(계획 P18-03; AV1 규격 7.17절): 복원 단위마다 분리형 위너 필터 또는 자기 유도 필터(부호화된 가중치로 두 상자 필터의 출력을
 * 투영)를 CDEF 출력에 적용한다. 필터는 64 휘도 행(8행 위로 이동) 높이의 가로 줄무늬 단위로 동작하며 다른 줄무늬의 샘플은 규격대로 CDEF 이전
 * 프레임에서 가져온다. 규격은 4x4 블록으로 설명하지만 한 줄무늬와 한 단위 안에 있는 더 큰 블록에서도 출력이 같으므로 이 구현은 줄무늬와 단위가
 * 만나는 사각형 전체를 한 번에 필터링한다.
 */
final class Av1LoopRestoration {

    /**
     * The margin of the source window around a region (the largest filter reach).
     * <p>
     * 영역 주변 원본 창의 여백(필터가 닿는 가장 먼 거리).
     */
    private static final int MARGIN = 3;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스.
     */
    private Av1LoopRestoration() {
    }

    /**
     * Restores the frame and returns the restored planes.
     * <p>
     * 프레임을 복원하여 복원된 평면을 반환한다.
     *
     * @param f    the frame (it holds the restoration parameters) / 프레임(복원 매개변수를 가진다)
     * @param curr the planes before CDEF / CDEF 이전 평면
     * @param cdef the planes after CDEF / CDEF 이후 평면
     * @return the restored planes (the CDEF planes if no plane is restored) / 복원된 평면(복원하는 평면이 없으면 CDEF 평면)
     */
    static short[][] apply(Av1Frame f, short[][] curr, short[][] cdef) {
        Av1FrameHeader hdr = f.hdr;
        if (!hdr.usesLr) {
            return cdef;
        }
        short[][] out = new short[3][];
        // 복원을 쓰는 평면만 복사하여 필터링하고 나머지는 그대로 공유한다.
        for (int plane = 0; plane < f.numPlanes; plane++) {
            if (hdr.frameRestorationType[plane] == Av1FrameHeader.RESTORE_NONE) {
                out[plane] = cdef[plane];
                continue;
            }
            out[plane] = cdef[plane].clone();
            restorePlane(f, plane, curr[plane], cdef[plane], out[plane]);
        }
        return out;
    }

    /**
     * Restores one plane stripe by stripe and unit by unit.
     * <p>
     * 평면 하나를 줄무늬별, 단위별로 복원한다.
     *
     * @param f     the frame / 프레임
     * @param plane the plane / 평면
     * @param curr  the samples before CDEF / CDEF 이전 샘플
     * @param cdef  the samples after CDEF / CDEF 이후 샘플
     * @param out   the output samples (a copy of cdef) / 출력 샘플(cdef 의 복사본)
     */
    private static void restorePlane(Av1Frame f, int plane, short[] curr, short[] cdef, short[] out) {
        int subY = plane == 0 ? 0 : f.seq.subsamplingY;
        int unitSize = f.hdr.loopRestorationSize[plane];
        int unitRows = f.lrUnitRows[plane];
        int unitCols = f.lrUnitCols[plane];
        int planeW = f.planeWidth[plane];
        int planeH = f.planeHeight[plane];
        // 줄무늬를 위에서 아래로 처리한다.
        for (int stripeNum = 0;; stripeNum++) {
            int stripeStartY = (-8 + stripeNum * 64) >> subY;
            int stripeEndY = stripeStartY + (64 >> subY) - 1;
            int y0 = Math.max(0, stripeStartY);
            int y1 = Math.min(planeH - 1, stripeEndY);
            if (y0 > planeH - 1) {
                break;
            }
            int unitRow = Math.min(unitRows - 1, ((stripeNum * 64) >> subY) / unitSize);
            // 줄무늬 안에서 복원 단위 열마다 사각형을 처리한다.
            for (int unitCol = 0; unitCol < unitCols; unitCol++) {
                int x0 = unitCol * unitSize;
                int x1 = unitCol == unitCols - 1 ? planeW - 1 : Math.min(planeW - 1, x0 + unitSize - 1);
                if (x0 > planeW - 1) {
                    break;
                }
                int unit = unitRow * unitCols + unitCol;
                int type = f.lrType[plane][unit];
                if (type == Av1FrameHeader.RESTORE_NONE) {
                    continue;
                }
                int w = x1 - x0 + 1;
                int h = y1 - y0 + 1;
                int[] win = window(f, plane, curr, cdef, x0, y0, w, h, stripeStartY, stripeEndY);
                if (type == Av1FrameHeader.RESTORE_WIENER) {
                    wiener(f, plane, unit, win, out, x0, y0, w, h);
                } else {
                    selfGuided(f, plane, unit, win, cdef, out, x0, y0, w, h);
                }
            }
        }
    }

    /**
     * Collects the source samples of a region and its margin (<code>get source sample process</code>).
     * <p>
     * 영역과 그 여백의 원본 샘플을 모은다(<code>get source sample process</code>).
     *
     * @param f           the frame / 프레임
     * @param plane       the plane / 평면
     * @param curr        the samples before CDEF / CDEF 이전 샘플
     * @param cdef        the samples after CDEF / CDEF 이후 샘플
     * @param x0          the left of the region / 영역의 왼쪽
     * @param y0          the top of the region / 영역의 위쪽
     * @param w           the width / 너비
     * @param h           the height / 높이
     * @param stripeStart the first row of the stripe / 줄무늬의 첫 행
     * @param stripeEnd   the last row of the stripe / 줄무늬의 마지막 행
     * @return the window of (w + 6) x (h + 6) samples / (w + 6) x (h + 6) 샘플의 창
     */
    private static int[] window(Av1Frame f, int plane, short[] curr, short[] cdef, int x0, int y0, int w, int h, int stripeStart,
            int stripeEnd) {
        int ww = w + 2 * MARGIN;
        int[] win = new int[ww * (h + 2 * MARGIN)];
        int stride = f.stride[plane];
        int endX = f.planeWidth[plane] - 1;
        int endY = f.planeHeight[plane] - 1;
        // 창의 행마다 평면 경계와 줄무늬 경계 규칙에 따라 원본 행을 정한다.
        for (int r = 0; r < h + 2 * MARGIN; r++) {
            int y = Math.max(0, Math.min(endY, y0 + r - MARGIN));
            short[] src;
            if (y < stripeStart) {
                y = Math.max(stripeStart - 2, y);
                src = curr;
            } else if (y > stripeEnd) {
                y = Math.min(stripeEnd + 2, y);
                src = curr;
            } else {
                src = cdef;
            }
            int rowBase = y * stride;
            // 한 행의 샘플을 창에 복사한다.
            for (int c = 0; c < ww; c++) {
                int x = Math.max(0, Math.min(endX, x0 + c - MARGIN));
                win[r * ww + c] = src[rowBase + x];
            }
        }
        return win;
    }

    /**
     * Applies the separable Wiener filter to a region (<code>Wiener filter process</code>).
     * <p>
     * 영역에 분리형 위너 필터를 적용한다(<code>Wiener filter process</code>).
     *
     * @param f     the frame / 프레임
     * @param plane the plane / 평면
     * @param unit  the restoration unit / 복원 단위
     * @param win   the source window / 원본 창
     * @param out   the output samples / 출력 샘플
     * @param x0    the left of the region / 영역의 왼쪽
     * @param y0    the top of the region / 영역의 위쪽
     * @param w     the width / 너비
     * @param h     the height / 높이
     */
    private static void wiener(Av1Frame f, int plane, int unit, int[] win, short[] out, int x0, int y0, int w, int h) {
        int bitDepth = f.seq.bitDepth;
        int round0 = bitDepth == 12 ? 5 : 3;
        int round1 = bitDepth == 12 ? 9 : 11;
        int[] coefs = f.lrWiener[plane];
        int[] vfilter = wienerTaps(coefs, unit * 6);
        int[] hfilter = wienerTaps(coefs, unit * 6 + 3);
        int ww = w + 2 * MARGIN;
        int offset = 1 << (bitDepth + 7 - round0 - 1);
        int limit = (1 << (bitDepth + 1 + 7 - round0)) - 1;
        int[] inter = new int[(h + 6) * w];
        // 가로 필터를 먼저 적용하여 중간 배열을 만든다.
        for (int r = 0; r < h + 6; r++) {
            for (int c = 0; c < w; c++) {
                int s = 0;
                // 일곱 탭 필터로 가중합을 구한다.
                for (int t = 0; t < 7; t++) {
                    s += hfilter[t] * win[r * ww + c + t];
                }
                int v = (s + (1 << (round0 - 1))) >> round0;
                inter[r * w + c] = Math.max(-offset, Math.min(limit - offset, v));
            }
        }
        int stride = f.stride[plane];
        int maxVal = (1 << bitDepth) - 1;
        // 세로 필터를 적용하여 출력 샘플을 만든다.
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                int s = 0;
                // 일곱 탭 필터로 가중합을 구한다.
                for (int t = 0; t < 7; t++) {
                    s += vfilter[t] * inter[(r + t) * w + c];
                }
                int v = (s + (1 << (round1 - 1))) >> round1;
                out[(y0 + r) * stride + x0 + c] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
            }
        }
    }

    /**
     * Expands the three coded Wiener coefficients to the seven symmetric taps (<code>Wiener coefficient process</code>).
     * <p>
     * 부호화된 위너 계수 세 개를 대칭인 일곱 탭으로 확장한다(<code>Wiener coefficient process</code>).
     *
     * @param coefs the coefficients of the unit / 단위의 계수
     * @param off   the index of the first coefficient of the pass / 패스의 첫 계수 위치
     * @return the seven taps / 일곱 탭
     */
    private static int[] wienerTaps(int[] coefs, int off) {
        int[] filter = new int[7];
        filter[3] = 128;
        // 대칭인 계수 세 개를 양쪽 탭에 넣는다.
        for (int i = 0; i < 3; i++) {
            int c = coefs[off + i];
            filter[i] = c;
            filter[6 - i] = c;
            filter[3] -= 2 * c;
        }
        return filter;
    }

    /**
     * Applies the self guided filter to a region (<code>self guided filter process</code>).
     * <p>
     * 영역에 자기 유도 필터를 적용한다(<code>self guided filter process</code>).
     *
     * @param f     the frame / 프레임
     * @param plane the plane / 평면
     * @param unit  the restoration unit / 복원 단위
     * @param win   the source window / 원본 창
     * @param cdef  the samples after CDEF / CDEF 이후 샘플
     * @param out   the output samples / 출력 샘플
     * @param x0    the left of the region / 영역의 왼쪽
     * @param y0    the top of the region / 영역의 위쪽
     * @param w     the width / 너비
     * @param h     the height / 높이
     */
    private static void selfGuided(Av1Frame f, int plane, int unit, int[] win, short[] cdef, short[] out, int x0, int y0, int w, int h) {
        int set = f.lrSgrSet[plane][unit];
        int[] flt0 = boxFilter(f, win, cdef, plane, x0, y0, w, h, set, 0);
        int[] flt1 = boxFilter(f, win, cdef, plane, x0, y0, w, h, set, 1);
        int w0 = f.lrSgrXqd[plane][unit * 2];
        int w1 = f.lrSgrXqd[plane][unit * 2 + 1];
        int w2 = (1 << 7) - w0 - w1;
        int r0 = AvifTables.SGR_PARAMS[set * 4];
        int r1 = AvifTables.SGR_PARAMS[set * 4 + 2];
        int stride = f.stride[plane];
        int maxVal = (1 << f.seq.bitDepth) - 1;
        // 두 필터 출력을 부호화된 가중치로 투영하여 최종 샘플을 만든다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int u = cdef[(y0 + i) * stride + x0 + j] << 4;
                long v = (long) w1 * u;
                v += r0 != 0 ? (long) w0 * flt0[i * w + j] : (long) w0 * u;
                v += r1 != 0 ? (long) w2 * flt1[i * w + j] : (long) w2 * u;
                int s = (int) ((v + (1 << 10)) >> 11);
                out[(y0 + i) * stride + x0 + j] = (short) (s < 0 ? 0 : (s > maxVal ? maxVal : s));
            }
        }
    }

    /**
     * Computes one output of the self guided filter (<code>box filter process</code>).
     * <p>
     * 자기 유도 필터의 출력 하나를 계산한다(<code>box filter process</code>).
     *
     * @param f     the frame / 프레임
     * @param win   the source window / 원본 창
     * @param cdef  the samples after CDEF / CDEF 이후 샘플
     * @param plane the plane / 평면
     * @param x0    the left of the region / 영역의 왼쪽
     * @param y0    the top of the region / 영역의 위쪽
     * @param w     the width / 너비
     * @param h     the height / 높이
     * @param set   the parameter set / 매개변수 집합
     * @param pass  0 or 1 / 0 또는 1
     * @return the filtered values (h rows of w) or null if the radius of the pass is 0 / 필터링된 값(너비 w 인 h 개의 행). 패스의 반경이 0 이면 null
     */
    private static int[] boxFilter(Av1Frame f, int[] win, short[] cdef, int plane, int x0, int y0, int w, int h, int set, int pass) {
        int r = AvifTables.SGR_PARAMS[set * 4 + pass * 2];
        if (r == 0) {
            return null;
        }
        int eps = AvifTables.SGR_PARAMS[set * 4 + pass * 2 + 1];
        int bitDepth = f.seq.bitDepth;
        int ww = w + 2 * MARGIN;
        int aw = w + 2;
        int n = (2 * r + 1) * (2 * r + 1);
        long n2e = (long) n * n * eps;
        long s = ((1L << 20) + n2e / 2) / n2e;
        long oneOverN = ((1 << 12) + (n / 2)) / n;
        int[] aArr = new int[aw * (h + 2)];
        int[] bArr = new int[aw * (h + 2)];
        // 가로 방향 합을 먼저 구해 둔다(행마다 폭 w + 2 의 합과 제곱합).
        int rows = h + 2 * MARGIN;
        long[] hs = new long[rows * aw];
        long[] hq = new long[rows * aw];
        // 가로 방향 합과 제곱합을 구한다.
        for (int rr = 0; rr < rows; rr++) {
            // 가로 방향 합과 제곱합을 구한다.
            for (int j = -1; j <= w; j++) {
                long sum = 0;
                long sq = 0;
                // 가로 창의 샘플을 더한다.
                for (int dx = -r; dx <= r; dx++) {
                    int c = win[rr * ww + j + dx + MARGIN];
                    sum += c;
                    sq += (long) c * c;
                }
                hs[rr * aw + j + 1] = sum;
                hq[rr * aw + j + 1] = sq;
            }
        }
        // 세로 방향으로 합쳐서 각 위치의 A, B 값을 계산한다.
        for (int i = -1; i < h + 1; i++) {
            for (int j = -1; j < w + 1; j++) {
                long a = 0;
                long b = 0;
                // 세로 창의 합을 더한다.
                for (int dy = -r; dy <= r; dy++) {
                    int rr = i + dy + MARGIN;
                    a += hq[rr * aw + j + 1];
                    b += hs[rr * aw + j + 1];
                }
                int sh = 2 * (bitDepth - 8);
                a = sh == 0 ? a : (a + (1L << (sh - 1))) >> sh;
                int sh2 = bitDepth - 8;
                long d = sh2 == 0 ? b : (b + (1L << (sh2 - 1))) >> sh2;
                long p = Math.max(0L, a * n - d * d);
                long z = (p * s + (1L << 19)) >> 20;
                long a2;
                if (z >= 255) {
                    a2 = 256;
                } else if (z == 0) {
                    a2 = 1;
                } else {
                    a2 = ((z << 8) + (z / 2)) / (z + 1);
                }
                long b2 = (256 - a2) * b * oneOverN;
                aArr[(i + 1) * aw + j + 1] = (int) a2;
                bArr[(i + 1) * aw + j + 1] = (int) ((b2 + (1L << 11)) >> 12);
            }
        }
        int[] flt = new int[w * h];
        int stride = f.stride[plane];
        // A, B 를 이웃 가중치로 섞어 필터 출력을 만든다.
        for (int i = 0; i < h; i++) {
            int shift = 5;
            if (pass == 0 && (i & 1) != 0) {
                shift = 4;
            }
            // 한 행의 샘플마다 필터 출력을 만든다.
            for (int j = 0; j < w; j++) {
                long a = 0;
                long b = 0;
                // 세 행의 이웃에 가중치를 곱해 더한다.
                for (int dy = -1; dy <= 1; dy++) {
                    // 세 열의 이웃에 가중치를 곱해 더한다.
                    for (int dx = -1; dx <= 1; dx++) {
                        int weight;
                        if (pass == 0) {
                            if (((i + dy) & 1) != 0) {
                                weight = dx == 0 ? 6 : 5;
                            } else {
                                weight = 0;
                            }
                        } else {
                            weight = (dx == 0 || dy == 0) ? 4 : 3;
                        }
                        if (weight != 0) {
                            a += weight * aArr[(i + dy + 1) * aw + j + dx + 1];
                            b += weight * bArr[(i + dy + 1) * aw + j + dx + 1];
                        }
                    }
                }
                long v = a * cdef[(y0 + i) * stride + x0 + j] + b;
                int sh = 8 + shift - 4;
                flt[i * w + j] = (int) ((v + (1L << (sh - 1))) >> sh);
            }
        }
        return flt;
    }
}
