package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The intra prediction of AV1 (plan item P18-03; AV1 specification section 7.11.2): the edge preparation, the DC, vertical, horizontal,
 * directional (with the intra edge filter and upsampling), smooth, Paeth and recursive filter intra predictors. The predicted samples are
 * written into the plane of the current frame.
 * <p>
 * AV1 의 인트라 예측(계획 P18-03; AV1 규격 7.11.2절): 가장자리 준비, DC, 수직, 수평, 방향성(인트라 가장자리 필터와 업샘플링 포함), 스무스, Paeth,
 * 재귀 필터 인트라 예측기. 예측한 샘플은 현재 프레임의 평면에 쓴다.
 */
final class Av1IntraPred {

    /**
     * The offset of index 0 in the edge arrays (the arrays are indexed from -16).
     * <p>
     * 가장자리 배열에서 인덱스 0 의 오프셋(배열은 -16 부터 인덱스를 쓴다).
     */
    private static final int OFF = 16;

    /**
     * Prediction mode numbers (V_PRED to PAETH_PRED); DC_PRED is 0.
     * <p>
     * 예측 모드 번호(V_PRED~PAETH_PRED). DC_PRED 는 0.
     */
    static final int V_PRED = 1;

    /**
     * Mode number of H_PRED.
     * <p>
     * H_PRED 의 모드 번호.
     */
    static final int H_PRED = 2;

    /**
     * Mode number of D67_PRED, the last directional mode.
     * <p>
     * 마지막 방향성 모드인 D67_PRED 의 모드 번호.
     */
    static final int D67_PRED = 8;

    /**
     * Mode number of SMOOTH_PRED.
     * <p>
     * SMOOTH_PRED 의 모드 번호.
     */
    static final int SMOOTH_PRED = 9;

    /**
     * Mode number of SMOOTH_V_PRED.
     * <p>
     * SMOOTH_V_PRED 의 모드 번호.
     */
    static final int SMOOTH_V_PRED = 10;

    /**
     * Mode number of SMOOTH_H_PRED.
     * <p>
     * SMOOTH_H_PRED 의 모드 번호.
     */
    static final int SMOOTH_H_PRED = 11;

    /**
     * Mode number of PAETH_PRED.
     * <p>
     * PAETH_PRED 의 모드 번호.
     */
    static final int PAETH_PRED = 12;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Av1IntraPred() {
    }

    /**
     * Tells whether a mode is a directional mode.
     * <p>
     * 모드가 방향성 모드인지 알려 준다.
     *
     * @param mode the prediction mode / 예측 모드
     * @return true for V_PRED to D67_PRED / V_PRED~D67_PRED 이면 true
     */
    static boolean isDirectional(int mode) {
        return mode >= V_PRED && mode <= D67_PRED;
    }

    /**
     * Returns the weights of the smooth predictors for a size.
     * <p>
     * 크기에 해당하는 스무스 예측기의 가중치를 반환한다.
     *
     * @param log2 the base 2 logarithm of the size (2 to 6) / 크기의 밑 2 로그(2~6)
     * @return the weights / 가중치
     */
    private static int[] smWeights(int log2) {
        switch (log2) {
        case 2:
            return AvifTables.SM_WEIGHTS_TX_4X4;
        case 3:
            return AvifTables.SM_WEIGHTS_TX_8X8;
        case 4:
            return AvifTables.SM_WEIGHTS_TX_16X16;
        case 5:
            return AvifTables.SM_WEIGHTS_TX_32X32;
        default:
            return AvifTables.SM_WEIGHTS_TX_64X64;
        }
    }

    /**
     * Predicts a block and writes it into the plane.
     * <p>
     * 블록을 예측해 평면에 쓴다.
     *
     * @param pl             the plane samples / 평면 샘플
     * @param stride         the row stride / 행 간격
     * @param x              left sample of the block / 블록의 왼쪽 샘플
     * @param y              top sample of the block / 블록의 위쪽 샘플
     * @param log2W          base 2 logarithm of the width / 너비의 밑 2 로그
     * @param log2H          base 2 logarithm of the height / 높이의 밑 2 로그
     * @param mode           the prediction mode / 예측 모드
     * @param haveLeft       true if samples to the left are available / 왼쪽 샘플이 있으면 true
     * @param haveAbove      true if samples above are available / 위쪽 샘플이 있으면 true
     * @param haveAboveRight true if samples above and to the right are available / 오른쪽 위 샘플이 있으면 true
     * @param haveBelowLeft  true if samples below and to the left are available / 왼쪽 아래 샘플이 있으면 true
     * @param maxX           the largest valid x of the plane / 평면에서 가장 큰 유효 x
     * @param maxY           the largest valid y of the plane / 평면에서 가장 큰 유효 y
     * @param angleDelta     the angle delta (-3..3) for directional modes / 방향성 모드의 각도 변화(-3..3)
     * @param edgeFilter     true if the intra edge filter is enabled / 인트라 가장자리 필터를 쓰면 true
     * @param filterType     1 if a neighbouring block uses a smooth mode (edge filter strength) / 이웃 블록이 스무스 모드를 쓰면 1(가장자리 필터 강도)
     * @param filterIntra    true to use the recursive filter intra predictor (luma only) / 재귀 필터 인트라 예측기를 쓰면 true(휘도만)
     * @param filterMode     the filter intra mode / 필터 인트라 모드
     * @param bitDepth       the bit depth / 비트 깊이
     */
    static void predict(short[] pl, int stride, int x, int y, int log2W, int log2H, int mode, boolean haveLeft, boolean haveAbove,
            boolean haveAboveRight, boolean haveBelowLeft, int maxX, int maxY, int angleDelta, boolean edgeFilter, int filterType,
            boolean filterIntra, int filterMode, int bitDepth) {
        int w = 1 << log2W;
        int h = 1 << log2H;
        int[] above = new int[OFF + 2 * (w + h) + 32];
        int[] left = new int[OFF + 2 * (w + h) + 32];
        int base = 1 << (bitDepth - 1);
        // 위쪽 줄 AboveRow[0..w+h-1] 을 준비한다.
        if (!haveAbove && haveLeft) {
            int v = pl[y * stride + x - 1];
            // 줄 전체를 같은 값으로 채운다.
            for (int i = 0; i < w + h; i++) {
                above[OFF + i] = v;
            }
        } else if (!haveAbove) {
            // 줄 전체를 같은 값으로 채운다.
            for (int i = 0; i < w + h; i++) {
                above[OFF + i] = base - 1;
            }
        } else {
            int limit = Math.min(maxX, x + (haveAboveRight ? 2 * w : w) - 1);
            // 가용한 이웃 샘플을 읽어 줄을 채운다(범위 밖은 마지막 값을 반복).
            for (int i = 0; i < w + h; i++) {
                above[OFF + i] = pl[(y - 1) * stride + Math.min(limit, x + i)];
            }
        }
        // 왼쪽 열 LeftCol[0..w+h-1] 을 준비한다.
        if (!haveLeft && haveAbove) {
            int v = pl[(y - 1) * stride + x];
            // 줄 전체를 같은 값으로 채운다.
            for (int i = 0; i < w + h; i++) {
                left[OFF + i] = v;
            }
        } else if (!haveLeft) {
            // 줄 전체를 같은 값으로 채운다.
            for (int i = 0; i < w + h; i++) {
                left[OFF + i] = base + 1;
            }
        } else {
            int limit = Math.min(maxY, y + (haveBelowLeft ? 2 * h : h) - 1);
            // 가용한 이웃 샘플을 읽어 줄을 채운다(범위 밖은 마지막 값을 반복).
            for (int i = 0; i < w + h; i++) {
                left[OFF + i] = pl[Math.min(limit, y + i) * stride + x - 1];
            }
        }
        int corner;
        if (haveAbove && haveLeft) {
            corner = pl[(y - 1) * stride + x - 1];
        } else if (haveAbove) {
            corner = pl[(y - 1) * stride + x];
        } else if (haveLeft) {
            corner = pl[y * stride + x - 1];
        } else {
            corner = base;
        }
        above[OFF - 1] = corner;
        left[OFF - 1] = corner;
        int[] pred = new int[w * h];
        if (filterIntra) {
            recursive(pred, w, h, above, left, filterMode, bitDepth);
        } else if (isDirectional(mode)) {
            directional(pred, w, h, above, left, mode, angleDelta, haveLeft, haveAbove, x, y, maxX, maxY, edgeFilter, filterType, bitDepth);
        } else if (mode == SMOOTH_PRED || mode == SMOOTH_V_PRED || mode == SMOOTH_H_PRED) {
            smooth(pred, w, h, log2W, log2H, above, left, mode);
        } else if (mode == 0) {
            dc(pred, w, h, log2W, log2H, above, left, haveLeft, haveAbove, bitDepth);
        } else {
            paeth(pred, w, h, above, left);
        }
        // 예측 결과를 프레임 평면에 쓴다.
        for (int i = 0; i < h; i++) {
            int row = (y + i) * stride + x;
            // 한 행의 샘플을 채운다.
            for (int j = 0; j < w; j++) {
                pl[row + j] = (short) pred[i * w + j];
            }
        }
    }

    /**
     * The basic (Paeth) predictor.
     * <p>
     * 기본(Paeth) 예측기.
     *
     * @param pred  the output / 출력
     * @param w     the width / 너비
     * @param h     the height / 높이
     * @param above the above row (offset 16) / 위쪽 줄(오프셋 16)
     * @param left  the left column (offset 16) / 왼쪽 열(오프셋 16)
     */
    private static void paeth(int[] pred, int w, int h, int[] above, int[] left) {
        int tl = above[OFF - 1];
        // 샘플마다 왼쪽, 위, 왼쪽 위 중 기본값에 가장 가까운 것을 고른다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int a = above[OFF + j];
                int l = left[OFF + i];
                int b = a + l - tl;
                int pLeft = Math.abs(b - l);
                int pTop = Math.abs(b - a);
                int pTopLeft = Math.abs(b - tl);
                int v;
                if (pLeft <= pTop && pLeft <= pTopLeft) {
                    v = l;
                } else if (pTop <= pTopLeft) {
                    v = a;
                } else {
                    v = tl;
                }
                pred[i * w + j] = v;
            }
        }
    }

    /**
     * The recursive filter intra predictor: blocks of 4x2 samples are predicted from seven neighbours.
     * <p>
     * 재귀 필터 인트라 예측기: 4x2 샘플 블록을 이웃 일곱 개로 예측한다.
     *
     * @param pred     the output / 출력
     * @param w        the width / 너비
     * @param h        the height / 높이
     * @param above    the above row / 위쪽 줄
     * @param left     the left column / 왼쪽 열
     * @param mode     the filter intra mode / 필터 인트라 모드
     * @param bitDepth the bit depth / 비트 깊이
     */
    private static void recursive(int[] pred, int w, int h, int[] above, int[] left, int mode, int bitDepth) {
        int w4 = w >> 2;
        int h2 = h >> 1;
        int max = (1 << bitDepth) - 1;
        int[] p = new int[7];
        // 4x2 블록마다 이웃 일곱 개를 모아 필터를 적용한다.
        for (int i2 = 0; i2 < h2; i2++) {
            for (int j4 = 0; j4 < w4; j4++) {
                // 예측에 쓰는 이웃 일곱 개를 모은다/가중합한다.
                for (int i = 0; i < 7; i++) {
                    if (i < 5) {
                        if (i2 == 0) {
                            p[i] = above[OFF + (j4 << 2) + i - 1];
                        } else if (j4 == 0 && i == 0) {
                            p[i] = left[OFF + (i2 << 1) - 1];
                        } else {
                            p[i] = pred[((i2 << 1) - 1) * w + (j4 << 2) + i - 1];
                        }
                    } else {
                        if (j4 == 0) {
                            p[i] = left[OFF + (i2 << 1) + i - 5];
                        } else {
                            p[i] = pred[((i2 << 1) + i - 5) * w + (j4 << 2) - 1];
                        }
                    }
                }
                // 4x2 블록의 두 행을 처리한다.
                for (int i1 = 0; i1 < 2; i1++) {
                    // 4x2 블록의 네 열을 처리한다.
                    for (int j1 = 0; j1 < 4; j1++) {
                        int pr = 0;
                        // 예측에 쓰는 이웃 일곱 개를 모은다/가중합한다.
                        for (int i = 0; i < 7; i++) {
                            pr += AvifTables.INTRA_FILTER_TAPS[(mode * 8 + (i1 << 2) + j1) * 7 + i] * p[i];
                        }
                        int v = pr >= 0 ? (pr + 8) >> 4 : -((-pr + 8) >> 4);
                        pred[((i2 << 1) + i1) * w + (j4 << 2) + j1] = v < 0 ? 0 : (v > max ? max : v);
                    }
                }
            }
        }
    }

    /**
     * The DC predictor.
     * <p>
     * DC 예측기.
     *
     * @param pred      the output / 출력
     * @param w         the width / 너비
     * @param h         the height / 높이
     * @param log2W     the base 2 logarithm of the width / 너비의 밑 2 로그
     * @param log2H     the base 2 logarithm of the height / 높이의 밑 2 로그
     * @param above     the above row / 위쪽 줄
     * @param left      the left column / 왼쪽 열
     * @param haveLeft  true if the left column is available / 왼쪽 열이 있으면 true
     * @param haveAbove true if the above row is available / 위쪽 줄이 있으면 true
     * @param bitDepth  the bit depth / 비트 깊이
     */
    private static void dc(int[] pred, int w, int h, int log2W, int log2H, int[] above, int[] left, boolean haveLeft, boolean haveAbove,
            int bitDepth) {
        int v;
        if (haveLeft && haveAbove) {
            int sum = 0;
            // 왼쪽 열의 샘플을 모두 더한다.
            for (int k = 0; k < h; k++) {
                sum += left[OFF + k];
            }
            // 위쪽 줄의 샘플을 모두 더한다.
            for (int k = 0; k < w; k++) {
                sum += above[OFF + k];
            }
            sum += (w + h) >> 1;
            v = sum / (w + h);
        } else if (haveLeft) {
            int sum = 0;
            // 왼쪽 열의 샘플을 모두 더한다.
            for (int k = 0; k < h; k++) {
                sum += left[OFF + k];
            }
            v = (sum + (h >> 1)) >> log2H;
        } else if (haveAbove) {
            int sum = 0;
            // 위쪽 줄의 샘플을 모두 더한다.
            for (int k = 0; k < w; k++) {
                sum += above[OFF + k];
            }
            v = (sum + (w >> 1)) >> log2W;
        } else {
            v = 1 << (bitDepth - 1);
        }
        java.util.Arrays.fill(pred, 0, w * h, v);
    }

    /**
     * The smooth predictors.
     * <p>
     * 스무스 예측기.
     *
     * @param pred  the output / 출력
     * @param w     the width / 너비
     * @param h     the height / 높이
     * @param log2W the base 2 logarithm of the width / 너비의 밑 2 로그
     * @param log2H the base 2 logarithm of the height / 높이의 밑 2 로그
     * @param above the above row / 위쪽 줄
     * @param left  the left column / 왼쪽 열
     * @param mode  SMOOTH_PRED, SMOOTH_V_PRED or SMOOTH_H_PRED / 스무스 모드 세 가지 중 하나
     */
    private static void smooth(int[] pred, int w, int h, int log2W, int log2H, int[] above, int[] left, int mode) {
        int[] wx = smWeights(log2W);
        int[] wy = smWeights(log2H);
        // 샘플마다 모드에 맞는 가중 평균을 계산한다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int v;
                if (mode == SMOOTH_PRED) {
                    int s = wy[i] * above[OFF + j] + (256 - wy[i]) * left[OFF + h - 1] + wx[j] * left[OFF + i] + (256 - wx[j]) * above[OFF + w - 1];
                    v = (s + 256) >> 9;
                } else if (mode == SMOOTH_V_PRED) {
                    int s = wy[i] * above[OFF + j] + (256 - wy[i]) * left[OFF + h - 1];
                    v = (s + 128) >> 8;
                } else {
                    int s = wx[j] * left[OFF + i] + (256 - wx[j]) * above[OFF + w - 1];
                    v = (s + 128) >> 8;
                }
                pred[i * w + j] = v;
            }
        }
    }

    /**
     * Selects the strength of the intra edge filter.
     * <p>
     * 인트라 가장자리 필터의 강도를 고른다.
     *
     * @param w          the width / 너비
     * @param h          the height / 높이
     * @param filterType 0 or 1 / 0 또는 1
     * @param delta      the angle difference in degrees / 각도 차(도)
     * @return the strength 0..3 / 강도 0..3
     */
    private static int edgeStrength(int w, int h, int filterType, int delta) {
        int d = Math.abs(delta);
        int blkWh = w + h;
        int strength = 0;
        if (filterType == 0) {
            if (blkWh <= 8) {
                if (d >= 56) {
                    strength = 1;
                }
            } else if (blkWh <= 12) {
                if (d >= 40) {
                    strength = 1;
                }
            } else if (blkWh <= 16) {
                if (d >= 40) {
                    strength = 1;
                }
            } else if (blkWh <= 24) {
                if (d >= 8) {
                    strength = 1;
                }
                if (d >= 16) {
                    strength = 2;
                }
                if (d >= 32) {
                    strength = 3;
                }
            } else if (blkWh <= 32) {
                strength = 1;
                if (d >= 4) {
                    strength = 2;
                }
                if (d >= 32) {
                    strength = 3;
                }
            } else {
                strength = 3;
            }
        } else {
            if (blkWh <= 8) {
                if (d >= 40) {
                    strength = 1;
                }
                if (d >= 64) {
                    strength = 2;
                }
            } else if (blkWh <= 16) {
                if (d >= 20) {
                    strength = 1;
                }
                if (d >= 48) {
                    strength = 2;
                }
            } else if (blkWh <= 24) {
                if (d >= 4) {
                    strength = 3;
                }
            } else {
                strength = 3;
            }
        }
        return strength;
    }

    /**
     * Tells whether the edge is upsampled.
     * <p>
     * 가장자리를 업샘플링하는지 알려 준다.
     *
     * @param w          the width / 너비
     * @param h          the height / 높이
     * @param filterType 0 or 1 / 0 또는 1
     * @param delta      the angle difference in degrees / 각도 차(도)
     * @return true for upsampling / 업샘플링하면 true
     */
    private static boolean useUpsample(int w, int h, int filterType, int delta) {
        int d = Math.abs(delta);
        int blkWh = w + h;
        if (d <= 0 || d >= 40) {
            return false;
        }
        return filterType == 0 ? blkWh <= 16 : blkWh <= 8;
    }

    /**
     * The intra edge filter: smooths the edge array.
     * <p>
     * 인트라 가장자리 필터: 가장자리 배열을 매끄럽게 한다.
     *
     * @param buf      the edge array (offset 16) / 가장자리 배열(오프셋 16)
     * @param sz       the number of samples including the corner / 모서리를 포함한 샘플 수
     * @param strength the filter strength 1..3 / 필터 강도 1..3
     */
    private static void edgeFilter(int[] buf, int sz, int strength) {
        if (strength == 0) {
            return;
        }
        int[] edge = new int[sz];
        // 필터링 전의 값을 복사해 둔다(모서리 샘플부터).
        for (int i = 0; i < sz; i++) {
            edge[i] = buf[OFF + i - 1];
        }
        // 첫 샘플을 뺀 나머지를 5탭 커널로 필터링한다.
        for (int i = 1; i < sz; i++) {
            int s = 0;
            // 다섯 탭 커널로 가장자리 샘플을 부드럽게 한다.
            for (int j = 0; j < 5; j++) {
                int k = Math.max(0, Math.min(sz - 1, i - 2 + j));
                s += AvifTables.INTRA_EDGE_KERNEL[(strength - 1) * 5 + j] * edge[k];
            }
            buf[OFF + i - 1] = (s + 8) >> 4;
        }
    }

    /**
     * The intra edge upsampler: doubles the resolution of an edge array.
     * <p>
     * 인트라 가장자리 업샘플러: 가장자리 배열의 해상도를 두 배로 한다.
     *
     * @param buf      the edge array (offset 16) / 가장자리 배열(오프셋 16)
     * @param numPx    the number of samples / 샘플 수
     * @param bitDepth the bit depth / 비트 깊이
     */
    private static void edgeUpsample(int[] buf, int numPx, int bitDepth) {
        int[] dup = new int[numPx + 3];
        dup[0] = buf[OFF - 1];
        // 업샘플 입력을 복사한다.
        for (int i = -1; i < numPx; i++) {
            dup[i + 2] = buf[OFF + i];
        }
        dup[numPx + 2] = buf[OFF + numPx - 1];
        int max = (1 << bitDepth) - 1;
        buf[OFF - 2] = dup[0];
        // 이웃 네 샘플로 가운데 값을 보간해 사이사이에 끼워 넣는다.
        for (int i = 0; i < numPx; i++) {
            int s = -dup[i] + 9 * dup[i + 1] + 9 * dup[i + 2] - dup[i + 3];
            s = (s + 8) >> 4;
            s = s < 0 ? 0 : (s > max ? max : s);
            buf[OFF + 2 * i - 1] = s;
            buf[OFF + 2 * i] = dup[i + 2];
        }
    }

    /**
     * The directional predictor with the intra edge filter and upsampling.
     * <p>
     * 인트라 가장자리 필터와 업샘플링을 가진 방향성 예측기.
     *
     * @param pred       the output / 출력
     * @param w          the width / 너비
     * @param h          the height / 높이
     * @param above      the above row / 위쪽 줄
     * @param left       the left column / 왼쪽 열
     * @param mode       the directional mode / 방향성 모드
     * @param angleDelta the angle delta / 각도 변화
     * @param haveLeft   true if the left column is available / 왼쪽 열이 있으면 true
     * @param haveAbove  true if the above row is available / 위쪽 줄이 있으면 true
     * @param x          left sample of the block / 블록의 왼쪽 샘플
     * @param y          top sample of the block / 블록의 위쪽 샘플
     * @param maxX       the largest valid x / 가장 큰 유효 x
     * @param maxY       the largest valid y / 가장 큰 유효 y
     * @param edgeFilt   true if the edge filter is enabled / 가장자리 필터를 쓰면 true
     * @param filterType the filter type (0 or 1) / 필터 유형(0 또는 1)
     * @param bitDepth   the bit depth / 비트 깊이
     */
    private static void directional(int[] pred, int w, int h, int[] above, int[] left, int mode, int angleDelta, boolean haveLeft,
            boolean haveAbove, int x, int y, int maxX, int maxY, boolean edgeFilt, int filterType, int bitDepth) {
        int pAngle = AvifTables.MODE_TO_ANGLE[mode] + angleDelta * 3;
        int upsampleAbove = 0;
        int upsampleLeft = 0;
        if (edgeFilt) {
            if (pAngle != 90 && pAngle != 180) {
                if (pAngle > 90 && pAngle < 180 && (w + h) >= 24) {
                    int s = left[OFF] * 5 + above[OFF - 1] * 6 + above[OFF] * 5;
                    int c = (s + 8) >> 4;
                    left[OFF - 1] = c;
                    above[OFF - 1] = c;
                }
                if (haveAbove) {
                    int strength = edgeStrength(w, h, filterType, pAngle - 90);
                    int numPx = Math.min(w, maxX - x + 1) + (pAngle < 90 ? h : 0) + 1;
                    edgeFilter(above, numPx, strength);
                }
                if (haveLeft) {
                    int strength = edgeStrength(w, h, filterType, pAngle - 180);
                    int numPx = Math.min(h, maxY - y + 1) + (pAngle > 180 ? w : 0) + 1;
                    edgeFilter(left, numPx, strength);
                }
            }
            if (useUpsample(w, h, filterType, pAngle - 90)) {
                upsampleAbove = 1;
                edgeUpsample(above, w + (pAngle < 90 ? h : 0), bitDepth);
            }
            if (useUpsample(w, h, filterType, pAngle - 180)) {
                upsampleLeft = 1;
                edgeUpsample(left, h + (pAngle > 180 ? w : 0), bitDepth);
            }
        }
        int dx = 0;
        int dy = 0;
        if (pAngle < 90) {
            dx = AvifTables.DR_INTRA_DERIVATIVE[pAngle];
        } else if (pAngle > 90 && pAngle < 180) {
            dx = AvifTables.DR_INTRA_DERIVATIVE[180 - pAngle];
        }
        if (pAngle > 90 && pAngle < 180) {
            dy = AvifTables.DR_INTRA_DERIVATIVE[pAngle - 90];
        } else if (pAngle > 180) {
            dy = AvifTables.DR_INTRA_DERIVATIVE[270 - pAngle];
        }
        // 각도 범위에 따라 위쪽 줄, 왼쪽 열, 또는 둘 다에서 보간한다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int v;
                if (pAngle < 90) {
                    int idx = (i + 1) * dx;
                    int base = (idx >> (6 - upsampleAbove)) + (j << upsampleAbove);
                    int shift = ((idx << upsampleAbove) >> 1) & 0x1F;
                    int maxBaseX = (w + h - 1) << upsampleAbove;
                    if (base < maxBaseX) {
                        v = (above[OFF + base] * (32 - shift) + above[OFF + base + 1] * shift + 16) >> 5;
                    } else {
                        v = above[OFF + maxBaseX];
                    }
                } else if (pAngle > 90 && pAngle < 180) {
                    int idx = (j << 6) - (i + 1) * dx;
                    int base = idx >> (6 - upsampleAbove);
                    if (base >= -(1 << upsampleAbove)) {
                        int shift = ((idx << upsampleAbove) >> 1) & 0x1F;
                        v = (above[OFF + base] * (32 - shift) + above[OFF + base + 1] * shift + 16) >> 5;
                    } else {
                        idx = (i << 6) - (j + 1) * dy;
                        base = idx >> (6 - upsampleLeft);
                        int shift = ((idx << upsampleLeft) >> 1) & 0x1F;
                        v = (left[OFF + base] * (32 - shift) + left[OFF + base + 1] * shift + 16) >> 5;
                    }
                } else if (pAngle > 180) {
                    int idx = (j + 1) * dy;
                    int base = (idx >> (6 - upsampleLeft)) + (i << upsampleLeft);
                    int shift = ((idx << upsampleLeft) >> 1) & 0x1F;
                    v = (left[OFF + base] * (32 - shift) + left[OFF + base + 1] * shift + 16) >> 5;
                } else if (pAngle == 90) {
                    v = above[OFF + j];
                } else {
                    v = left[OFF + i];
                }
                pred[i * w + j] = v;
            }
        }
    }
}
