package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The constrained directional enhancement filter (CDEF) of AV1 (plan item P18-03; AV1 specification section 7.15): it finds the dominant
 * direction of every 8x8 block and smooths the samples along (primary) and across (secondary) that direction while it limits the change of
 * each sample, which removes ringing around edges. The input is the deblocked frame; the result is written to separate planes.
 * <p>
 * AV1 의 제한 방향성 강화 필터(CDEF)(계획 P18-03; AV1 규격 7.15절): 8x8 블록마다 지배적인 방향을 찾아 그 방향을 따라(주) 그리고 가로질러(보조)
 * 샘플을 부드럽게 하되 샘플마다 변화량을 제한하여 경계 주변의 링잉을 없앤다. 입력은 블록 경계를 제거한 프레임이며 결과는 별도의 평면에 쓴다.
 */
final class Av1Cdef {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스.
     */
    private Av1Cdef() {
    }

    /**
     * Filters the frame and returns the filtered planes.
     * <p>
     * 프레임을 필터링하여 필터링된 평면을 반환한다.
     *
     * @param f the frame with the deblocked planes / 블록 경계를 제거한 평면을 가진 프레임
     * @return the filtered planes (the planes of the frame itself if CDEF is off) / 필터링된 평면(CDEF 가 꺼져 있으면 프레임의 평면 그대로)
     */
    static short[][] apply(Av1Frame f) {
        Av1FrameHeader hdr = f.hdr;
        Av1Sequence seq = f.seq;
        if (!seq.enableCdef || hdr.codedLossless || hdr.allowIntrabc) {
            return f.planes;
        }
        short[][] out = new short[3][];
        // 출력 평면은 입력의 복사본으로 시작하여 필터링되는 블록만 덮어쓴다.
        for (int p = 0; p < f.numPlanes; p++) {
            out[p] = f.planes[p].clone();
        }
        int[] pri = new int[2];
        int[] sec = new int[2];
        // 8x8 휘도 블록마다 CDEF 매개변수를 찾아 필터링한다.
        for (int r = 0; r < f.miRows; r += 2) {
            for (int c = 0; c < f.miCols; c += 2) {
                int idx = f.cdefIdx[(r >> 4) * f.cdefStride + (c >> 4)];
                if (idx == -1) {
                    continue;
                }
                int s = r * f.miStride + c;
                boolean skip = f.skip[s] != 0 && f.skip[s + f.miStride] != 0 && f.skip[s + 1] != 0 && f.skip[s + f.miStride + 1] != 0;
                if (skip) {
                    continue;
                }
                pri[0] = hdr.cdefYPriStrength[idx];
                sec[0] = hdr.cdefYSecStrength[idx];
                pri[1] = hdr.cdefUvPriStrength[idx];
                sec[1] = hdr.cdefUvSecStrength[idx];
                if (pri[0] == 0 && sec[0] == 0 && (f.numPlanes == 1 || (pri[1] == 0 && sec[1] == 0))) {
                    continue;
                }
                block(f, out, r, c, pri, sec);
            }
        }
        return out;
    }

    /**
     * Filters one 8x8 block of every plane (<code>CDEF block process</code>).
     * <p>
     * 모든 평면의 8x8 블록 하나를 필터링한다(<code>CDEF block process</code>).
     *
     * @param f   the frame / 프레임
     * @param out the output planes / 출력 평면
     * @param r   the row in 4x4 units / 4x4 단위 행
     * @param c   the column in 4x4 units / 4x4 단위 열
     * @param pri the primary strengths of luma and chroma / 휘도와 색차의 주 강도
     * @param sec the secondary strengths of luma and chroma / 휘도와 색차의 보조 강도
     */
    private static void block(Av1Frame f, short[][] out, int r, int c, int[] pri, int[] sec) {
        int bd = f.seq.bitDepth;
        int coeffShift = bd - 8;
        long[] dirVar = direction(f, r, c);
        int yDir = (int) dirVar[0];
        int var = (int) dirVar[1];
        int priStr = pri[0] << coeffShift;
        int secStr = sec[0] << coeffShift;
        int dir = priStr == 0 ? 0 : yDir;
        int varStr = (var >> 6) != 0 ? Math.min(floorLog2(var >> 6), 12) : 0;
        priStr = var != 0 ? (priStr * (4 + varStr) + 8) >> 4 : 0;
        int damping = f.hdr.cdefDamping + coeffShift;
        filter(f, out, 0, r, c, priStr, secStr, damping, dir);
        if (f.numPlanes == 1) {
            return;
        }
        priStr = pri[1] << coeffShift;
        secStr = sec[1] << coeffShift;
        dir = priStr == 0 ? 0 : AvifTables.CDEF_UV_DIR[f.seq.subsamplingX * 16 + f.seq.subsamplingY * 8 + yDir];
        damping = f.hdr.cdefDamping + coeffShift - 1;
        filter(f, out, 1, r, c, priStr, secStr, damping, dir);
        filter(f, out, 2, r, c, priStr, secStr, damping, dir);
    }

    /**
     * The base 2 logarithm rounded down.
     * <p>
     * 내림한 밑 2 로그.
     *
     * @param x a positive number / 양수
     * @return the logarithm / 로그
     */
    private static int floorLog2(int x) {
        return 31 - Integer.numberOfLeadingZeros(x);
    }

    /**
     * Finds the direction and the variance of an 8x8 luma block (<code>CDEF direction process</code>).
     * <p>
     * 8x8 휘도 블록의 방향과 분산을 찾는다(<code>CDEF direction process</code>).
     *
     * @param f the frame / 프레임
     * @param r the row in 4x4 units / 4x4 단위 행
     * @param c the column in 4x4 units / 4x4 단위 열
     * @return the direction and the variance / 방향과 분산
     */
    private static long[] direction(Av1Frame f, int r, int c) {
        long[] cost = new long[8];
        int[][] partial = new int[8][15];
        int x0 = c << 2;
        int y0 = r << 2;
        short[] pl = f.planes[0];
        int stride = f.stride[0];
        int shift = f.seq.bitDepth - 8;
        // 8x8 샘플을 여덟 방향의 선을 따라 부분 합으로 모은다.
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int x = (pl[(y0 + i) * stride + x0 + j] >> shift) - 128;
                partial[0][i + j] += x;
                partial[1][i + j / 2] += x;
                partial[2][i] += x;
                partial[3][3 + i - j / 2] += x;
                partial[4][7 + i - j] += x;
                partial[5][3 - i / 2 + j] += x;
                partial[6][j] += x;
                partial[7][i / 2 + j] += x;
            }
        }
        int[] div = AvifTables.DIV_TABLE;
        // 수직/수평 방향의 부분 합을 제곱하여 비용에 더한다.
        for (int i = 0; i < 8; i++) {
            cost[2] += (long) partial[2][i] * partial[2][i];
            cost[6] += (long) partial[6][i] * partial[6][i];
        }
        cost[2] *= div[8];
        cost[6] *= div[8];
        // 대각선 방향의 양 끝 부분 합을 가중하여 비용에 더한다.
        for (int i = 0; i < 7; i++) {
            cost[0] += ((long) partial[0][i] * partial[0][i] + (long) partial[0][14 - i] * partial[0][14 - i]) * div[i + 1];
            cost[4] += ((long) partial[4][i] * partial[4][i] + (long) partial[4][14 - i] * partial[4][14 - i]) * div[i + 1];
        }
        cost[0] += (long) partial[0][7] * partial[0][7] * div[8];
        cost[4] += (long) partial[4][7] * partial[4][7] * div[8];
        // 홀수 방향(대각선에 가까운 방향)의 비용을 계산한다.
        for (int i = 1; i < 8; i += 2) {
            for (int j = 0; j < 5; j++) {
                cost[i] += (long) partial[i][3 + j] * partial[i][3 + j];
            }
            cost[i] *= div[8];
            // 홀수 방향의 바깥쪽 부분 합을 가중하여 비용에 더한다.
            for (int j = 0; j < 3; j++) {
                cost[i] += ((long) partial[i][j] * partial[i][j] + (long) partial[i][10 - j] * partial[i][10 - j]) * div[2 * j + 2];
            }
        }
        long bestCost = 0;
        int yDir = 0;
        // 비용이 가장 큰 방향을 고른다.
        for (int i = 0; i < 8; i++) {
            if (cost[i] > bestCost) {
                bestCost = cost[i];
                yDir = i;
            }
        }
        long var = (bestCost - cost[(yDir + 4) & 7]) >> 10;
        return new long[] { yDir, var };
    }

    /**
     * Applies the filter to the 8x8 luma block (or the matching chroma block) of one plane (<code>CDEF filter process</code>).
     * <p>
     * 한 평면의 8x8 휘도 블록(또는 대응하는 색차 블록)에 필터를 적용한다(<code>CDEF filter process</code>).
     *
     * @param f       the frame / 프레임
     * @param out     the output planes / 출력 평면
     * @param plane   the plane / 평면
     * @param r       the row in 4x4 luma units / 4x4 휘도 단위 행
     * @param c       the column in 4x4 luma units / 4x4 휘도 단위 열
     * @param priStr  the primary strength / 주 강도
     * @param secStr  the secondary strength / 보조 강도
     * @param damping the damping / 감쇠
     * @param dir     the direction / 방향
     */
    private static void filter(Av1Frame f, short[][] out, int plane, int r, int c, int priStr, int secStr, int damping, int dir) {
        if (priStr == 0 && secStr == 0) {
            return;
        }
        int coeffShift = f.seq.bitDepth - 8;
        int subX = plane > 0 ? f.seq.subsamplingX : 0;
        int subY = plane > 0 ? f.seq.subsamplingY : 0;
        int x0 = (c * 4) >> subX;
        int y0 = (r * 4) >> subY;
        int w = 8 >> subX;
        int h = 8 >> subY;
        short[] src = f.planes[plane];
        short[] dst = out[plane];
        int stride = f.stride[plane];
        int maxX = (f.miCols * 4) >> subX;
        int maxY = (f.miRows * 4) >> subY;
        int[] dirs = AvifTables.CDEF_DIRECTIONS;
        int tapSel = (priStr >> coeffShift) & 1;
        int priAdj = priStr == 0 ? 0 : Math.max(0, damping - floorLog2(priStr));
        int secAdj = secStr == 0 ? 0 : Math.max(0, damping - floorLog2(secStr));
        // 블록의 샘플마다 주 방향과 보조 방향의 이웃을 모아 제한된 차이의 합을 구한다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int sum = 0;
                int x = src[(y0 + i) * stride + x0 + j];
                int max = x;
                int min = x;
                // 거리 1 과 거리 2 의 이웃을 차례로 처리한다.
                for (int k = 0; k < 2; k++) {
                    // 양쪽(+/-) 방향의 이웃을 처리한다.
                    for (int sign = -1; sign <= 1; sign += 2) {
                        int yy = y0 + i + sign * dirs[dir * 4 + k * 2];
                        int xx = x0 + j + sign * dirs[dir * 4 + k * 2 + 1];
                        if (yy >= 0 && yy < maxY && xx >= 0 && xx < maxX) {
                            int p = src[yy * stride + xx];
                            sum += AvifTables.CDEF_PRI_TAPS[tapSel * 2 + k] * constrain(p - x, priStr, priAdj);
                            max = Math.max(p, max);
                            min = Math.min(p, min);
                        }
                        // 주 방향에서 양옆으로 어긋난 보조 방향 둘을 처리한다.
                        for (int dirOff = -2; dirOff <= 2; dirOff += 4) {
                            int d2 = (dir + dirOff) & 7;
                            int sy = y0 + i + sign * dirs[d2 * 4 + k * 2];
                            int sx = x0 + j + sign * dirs[d2 * 4 + k * 2 + 1];
                            if (sy >= 0 && sy < maxY && sx >= 0 && sx < maxX) {
                                int s = src[sy * stride + sx];
                                sum += AvifTables.CDEF_SEC_TAPS[tapSel * 2 + k] * constrain(s - x, secStr, secAdj);
                                max = Math.max(s, max);
                                min = Math.min(s, min);
                            }
                        }
                    }
                }
                int v = x + ((8 + sum - (sum < 0 ? 1 : 0)) >> 4);
                dst[(y0 + i) * stride + x0 + j] = (short) (v < min ? min : (v > max ? max : v));
            }
        }
    }

    /**
     * Limits a difference to the threshold (<code>constrain</code>).
     * <p>
     * 차이를 문턱값으로 제한한다(<code>constrain</code>).
     *
     * @param diff       the difference / 차이
     * @param threshold  the strength (0 means no change) / 강도(0 이면 변화 없음)
     * @param dampingAdj the adjusted damping shift / 조정된 감쇠 시프트
     * @return the constrained difference / 제한된 차이
     */
    private static int constrain(int diff, int threshold, int dampingAdj) {
        if (threshold == 0) {
            return 0;
        }
        int ad = diff < 0 ? -diff : diff;
        int v = threshold - (ad >> dampingAdj);
        v = v < 0 ? 0 : (v > ad ? ad : v);
        return diff < 0 ? -v : v;
    }
}
