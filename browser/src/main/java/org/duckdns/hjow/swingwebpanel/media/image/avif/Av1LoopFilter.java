package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The deblocking loop filter of AV1 (plan item P18-03; AV1 specification section 7.14): it smooths the vertical and then the horizontal
 * block and transform boundaries of the reconstructed frame in place, with a strength that depends on the frame level, the segment, the
 * block deltas and the sharpness. Only the parts used by intra frames are implemented (every block is an intra block).
 * <p>
 * AV1 의 블록 경계 제거 루프 필터(계획 P18-03; AV1 규격 7.14절): 복원된 프레임의 세로 경계와 가로 경계를 순서대로 제자리에서 부드럽게 한다. 세기는
 * 프레임 수준, 세그먼트, 블록 변화량, 선명도에 따라 정해진다. 인트라 프레임이 쓰는 부분만 구현한다(모든 블록이 인트라 블록).
 */
final class Av1LoopFilter {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스.
     */
    private Av1LoopFilter() {
    }

    /**
     * Filters the whole frame (<code>loop filter process</code>).
     * <p>
     * 프레임 전체를 필터링한다(<code>loop filter process</code>).
     *
     * @param f the frame whose planes are modified / 평면을 수정할 프레임
     */
    static void apply(Av1Frame f) {
        Av1FrameHeader hdr = f.hdr;
        if (hdr.loopFilterLevel[0] == 0 && hdr.loopFilterLevel[1] == 0) {
            return;
        }
        // 평면마다 세로 경계(0)를 모두 처리한 뒤 가로 경계(1)를 처리한다.
        for (int plane = 0; plane < f.numPlanes; plane++) {
            if (plane != 0 && hdr.loopFilterLevel[1 + plane] == 0) {
                continue;
            }
            int subX = plane == 0 ? 0 : f.seq.subsamplingX;
            int subY = plane == 0 ? 0 : f.seq.subsamplingY;
            // 세로 경계(패스 0)를 모두 처리한 뒤 가로 경계(패스 1)를 처리한다.
            for (int pass = 0; pass < 2; pass++) {
                int rowStep = plane == 0 ? 1 : (1 << subY);
                int colStep = plane == 0 ? 1 : (1 << subX);
                // 프레임의 4x4 행을 위에서 아래로 처리한다.
                for (int row = 0; row < f.miRows; row += rowStep) {
                    // 한 줄의 4x4 위치를 왼쪽에서 오른쪽으로 처리한다.
                    for (int col = 0; col < f.miCols; col += colStep) {
                        edge(f, plane, pass, row, col, subX, subY);
                    }
                }
            }
        }
    }

    /**
     * Filters one 4 sample long edge segment (<code>edge loop filter process</code>).
     * <p>
     * 4샘플 길이의 경계 조각 하나를 필터링한다(<code>edge loop filter process</code>).
     *
     * @param f     the frame / 프레임
     * @param plane the plane / 평면
     * @param pass  0 for vertical edges, 1 for horizontal edges / 세로 경계면 0, 가로 경계면 1
     * @param row0  the row in 4x4 luma units / 4x4 휘도 단위 행
     * @param col0  the column in 4x4 luma units / 4x4 휘도 단위 열
     * @param subX  the horizontal subsampling of the plane / 평면의 가로 하위 표본화
     * @param subY  the vertical subsampling of the plane / 평면의 세로 하위 표본화
     */
    private static void edge(Av1Frame f, int plane, int pass, int row0, int col0, int subX, int subY) {
        int dx = pass == 0 ? 1 : 0;
        int dy = pass == 0 ? 0 : 1;
        int x = col0 * 4;
        int y = row0 * 4;
        int row = row0 | subY;
        int col = col0 | subX;
        if (x >= f.hdr.frameWidth || y >= f.hdr.frameHeight) {
            return;
        }
        if ((pass == 0 && x == 0) || (pass == 1 && y == 0)) {
            return;
        }
        int xP = x >> subX;
        int yP = y >> subY;
        int prevRow = row - (dy << subY);
        int prevCol = col - (dx << subX);
        int stride = f.miStride;
        int mi = row * stride + col;
        int miSize = f.miSize[mi];
        int txSz = f.lfTxSize[plane][(row >> subY) * stride + (col >> subX)];
        int planeSize = AvifTables.SUBSAMPLED_SIZE[miSize * 4 + subX * 2 + subY];
        int prevTxSz = f.lfTxSize[plane][(prevRow >> subY) * stride + (prevCol >> subX)];
        boolean isBlockEdge;
        boolean isTxEdge;
        if (pass == 0) {
            isBlockEdge = xP % (AvifTables.NUM_4X4_BLOCKS_WIDE[planeSize] * 4) == 0;
            isTxEdge = xP % AvifTables.TX_WIDTH[txSz] == 0;
        } else {
            isBlockEdge = yP % (AvifTables.NUM_4X4_BLOCKS_HIGH[planeSize] * 4) == 0;
            isTxEdge = yP % AvifTables.TX_HEIGHT[txSz] == 0;
        }
        // 변환 경계가 아니면 필터링하지 않는다. 인트라 블록은 모든 변환 경계를 필터링한다.
        if (!isTxEdge) {
            return;
        }
        // 블록 경계가 아닌 변환 경계는 건너뛰기 인터 블록 안에서는 필터링하지 않는다.
        boolean skipBlock = f.skip[mi] != 0;
        boolean isIntra = f.refFrame0[mi] <= 0;
        if (!isBlockEdge && skipBlock && !isIntra) {
            return;
        }
        int baseSize = pass == 0 ? Math.min(AvifTables.TX_WIDTH[prevTxSz], AvifTables.TX_WIDTH[txSz])
                : Math.min(AvifTables.TX_HEIGHT[prevTxSz], AvifTables.TX_HEIGHT[txSz]);
        int filterSize = plane == 0 ? Math.min(16, baseSize) : Math.min(8, baseSize);
        int lvl = level(f, row, col, plane, pass);
        if (lvl == 0) {
            lvl = level(f, prevRow, prevCol, plane, pass);
        }
        if (lvl == 0) {
            return;
        }
        int sharp = f.hdr.loopFilterSharpness;
        int shift = sharp > 4 ? 2 : (sharp > 0 ? 1 : 0);
        int limit;
        if (sharp > 0) {
            limit = Math.max(1, Math.min(9 - sharp, lvl >> shift));
        } else {
            limit = Math.max(1, lvl >> shift);
        }
        int blimit = 2 * (lvl + 2) + limit;
        int thresh = lvl >> 4;
        short[] pl = f.planes[plane];
        int pstride = f.stride[plane];
        int step = pass == 0 ? 1 : pstride;
        // 경계를 따라 4개 샘플 위치를 차례로 필터링한다.
        for (int i = 0; i < 4; i++) {
            int pos = (yP + dx * i) * pstride + xP + dy * i;
            sample(pl, pos, step, plane, limit, blimit, thresh, filterSize, f.seq.bitDepth);
        }
    }

    /**
     * The filter level of a block (<code>adaptive filter strength process</code>) of an intra block.
     * <p>
     * 인트라 블록의 필터 수준(<code>adaptive filter strength process</code>).
     *
     * @param f     the frame / 프레임
     * @param row   the row in 4x4 units / 4x4 단위 행
     * @param col   the column in 4x4 units / 4x4 단위 열
     * @param plane the plane / 평면
     * @param pass  the pass / 패스
     * @return the level 0..63 / 수준 0~63
     */
    private static int level(Av1Frame f, int row, int col, int plane, int pass) {
        Av1FrameHeader hdr = f.hdr;
        int mi = row * f.miStride + col;
        int segment = f.segmentId[mi];
        int i = plane == 0 ? pass : plane + 1;
        int deltaLf = 0;
        if (f.deltaLf != null) {
            deltaLf = f.deltaLf[mi * 4 + (hdr.deltaLfMulti ? i : 0)];
        }
        int lvlSeg = Math.max(0, Math.min(63, deltaLf + hdr.loopFilterLevel[i]));
        int feature = 1 + i;
        if (hdr.segmentationEnabled && hdr.featureEnabled[segment][feature]) {
            lvlSeg = Math.max(0, Math.min(63, hdr.featureData[segment][feature] + lvlSeg));
        }
        if (hdr.loopFilterDeltaEnabled) {
            int nShift = lvlSeg >> 5;
            int ref = f.refFrame0[mi];
            int mode = f.yMode[mi];
            // 인터 블록의 일부 모드(전역 움직임이 아닌 NEAREST 이상)는 모드 변화량도 더한다.
            int modeType = mode >= Av1MvStack.NEARESTMV && mode != Av1MvStack.GLOBALMV && mode != Av1MvStack.GLOBAL_GLOBALMV ? 1 : 0;
            if (ref <= 0) {
                lvlSeg += hdr.loopFilterRefDeltas[0] << nShift;
            } else {
                lvlSeg += (hdr.loopFilterRefDeltas[ref] << nShift) + (hdr.loopFilterModeDeltas[modeType] << nShift);
            }
            lvlSeg = Math.max(0, Math.min(63, lvlSeg));
        }
        return lvlSeg;
    }

    /**
     * Filters the samples across the edge at one position (<code>sample filtering process</code>).
     * <p>
     * 한 위치에서 경계를 가로지르는 샘플을 필터링한다(<code>sample filtering process</code>).
     *
     * @param pl         the plane / 평면
     * @param pos        the index of the first sample after the edge (q0) / 경계 다음 첫 샘플(q0)의 위치
     * @param step       the distance between samples across the edge / 경계를 가로지르는 샘플 간격
     * @param plane      the plane number / 평면 번호
     * @param limit      the limit / 한계
     * @param blimit     the block limit / 블록 한계
     * @param thresh     the high edge variance threshold / 높은 가장자리 분산 문턱값
     * @param filterSize the maximum filter size / 최대 필터 크기
     * @param bitDepth   the bit depth / 비트 깊이
     */
    private static void sample(short[] pl, int pos, int step, int plane, int limit, int blimit, int thresh, int filterSize, int bitDepth) {
        int bd = bitDepth - 8;
        int q0 = pl[pos];
        int q1 = pl[pos + step];
        int p0 = pl[pos - step];
        int p1 = pl[pos - 2 * step];
        int threshBd = thresh << bd;
        boolean hev = Math.abs(p1 - p0) > threshBd || Math.abs(q1 - q0) > threshBd;
        int filterLen;
        if (filterSize == 4) {
            filterLen = 4;
        } else if (plane != 0) {
            filterLen = 6;
        } else if (filterSize == 8) {
            filterLen = 8;
        } else {
            filterLen = 16;
        }
        int limitBd = limit << bd;
        int blimitBd = blimit << bd;
        boolean mask = Math.abs(p1 - p0) > limitBd || Math.abs(q1 - q0) > limitBd
                || Math.abs(p0 - q0) * 2 + Math.abs(p1 - q1) / 2 > blimitBd;
        int p2 = 0;
        int q2 = 0;
        int p3 = 0;
        int q3 = 0;
        if (filterLen >= 6) {
            p2 = pl[pos - 3 * step];
            q2 = pl[pos + 2 * step];
            mask |= Math.abs(p2 - p1) > limitBd || Math.abs(q2 - q1) > limitBd;
        }
        if (filterLen >= 8) {
            p3 = pl[pos - 4 * step];
            q3 = pl[pos + 3 * step];
            mask |= Math.abs(p3 - p2) > limitBd || Math.abs(q3 - q2) > limitBd;
        }
        if (mask) {
            return;
        }
        int one = 1 << bd;
        boolean flat = false;
        if (filterSize >= 8) {
            boolean m = Math.abs(p1 - p0) > one || Math.abs(q1 - q0) > one || Math.abs(p2 - p0) > one || Math.abs(q2 - q0) > one;
            if (filterLen >= 8) {
                m |= Math.abs(p3 - p0) > one || Math.abs(q3 - q0) > one;
            }
            flat = !m;
        }
        boolean flat2 = false;
        if (filterSize >= 16) {
            int p4 = pl[pos - 5 * step];
            int p5 = pl[pos - 6 * step];
            int p6 = pl[pos - 7 * step];
            int q4 = pl[pos + 4 * step];
            int q5 = pl[pos + 5 * step];
            int q6 = pl[pos + 6 * step];
            boolean m = Math.abs(p6 - p0) > one || Math.abs(q6 - q0) > one || Math.abs(p5 - p0) > one || Math.abs(q5 - q0) > one
                    || Math.abs(p4 - p0) > one || Math.abs(q4 - q0) > one;
            flat2 = !m;
        }
        if (filterSize == 4 || !flat) {
            narrow(pl, pos, step, hev, bitDepth);
        } else if (filterSize == 8 || !flat2) {
            wide(pl, pos, step, plane, 3);
        } else {
            wide(pl, pos, step, plane, 4);
        }
    }

    /**
     * The narrow filter (<code>narrow filter process</code>).
     * <p>
     * 좁은 필터(<code>narrow filter process</code>).
     *
     * @param pl       the plane / 평면
     * @param pos      the index of q0 / q0 의 위치
     * @param step     the distance between samples / 샘플 간격
     * @param hev      true for high edge variance / 가장자리 분산이 높으면 true
     * @param bitDepth the bit depth / 비트 깊이
     */
    private static void narrow(short[] pl, int pos, int step, boolean hev, int bitDepth) {
        int off = 0x80 << (bitDepth - 8);
        int lo = -(1 << (bitDepth - 1));
        int hi = (1 << (bitDepth - 1)) - 1;
        int ps1 = pl[pos - 2 * step] - off;
        int ps0 = pl[pos - step] - off;
        int qs0 = pl[pos] - off;
        int qs1 = pl[pos + step] - off;
        int filter = hev ? clamp(ps1 - qs1, lo, hi) : 0;
        filter = clamp(filter + 3 * (qs0 - ps0), lo, hi);
        int filter1 = clamp(filter + 4, lo, hi) >> 3;
        int filter2 = clamp(filter + 3, lo, hi) >> 3;
        pl[pos] = (short) (clamp(qs0 - filter1, lo, hi) + off);
        pl[pos - step] = (short) (clamp(ps0 + filter2, lo, hi) + off);
        if (!hev) {
            int f = (filter1 + 1) >> 1;
            pl[pos + step] = (short) (clamp(qs1 - f, lo, hi) + off);
            pl[pos - 2 * step] = (short) (clamp(ps1 + f, lo, hi) + off);
        }
    }

    /**
     * The wide low pass filter (<code>wide filter process</code>).
     * <p>
     * 넓은 저역 통과 필터(<code>wide filter process</code>).
     *
     * @param pl       the plane / 평면
     * @param pos      the index of q0 / q0 의 위치
     * @param step     the distance between samples / 샘플 간격
     * @param plane    the plane number / 평면 번호
     * @param log2Size the base 2 logarithm of the number of taps / 탭 수의 밑 2 로그
     */
    private static void wide(short[] pl, int pos, int step, int plane, int log2Size) {
        int n;
        if (log2Size == 4) {
            n = 6;
        } else if (plane == 0) {
            n = 3;
        } else {
            n = 2;
        }
        int n2 = (log2Size == 3 && plane == 0) ? 0 : 1;
        int[] out = new int[12];
        // 경계 양쪽 n 개 샘플을 가중 평균으로 다시 계산한다.
        for (int i = -n; i < n; i++) {
            int t = 0;
            // 필터 탭을 더해 평균을 구한다.
            for (int j = -n; j <= n; j++) {
                int p = Math.max(-(n + 1), Math.min(n, i + j));
                int tap = Math.abs(j) <= n2 ? 2 : 1;
                t += pl[pos + p * step] * tap;
            }
            out[i + n] = (t + (1 << (log2Size - 1))) >> log2Size;
        }
        // 계산한 필터 결과를 샘플에 되쓴다/계산한다.
        for (int i = -n; i < n; i++) {
            pl[pos + i * step] = (short) out[i + n];
        }
    }

    /**
     * Limits a value to a range.
     * <p>
     * 값을 범위로 제한한다.
     *
     * @param v  the value / 값
     * @param lo the lowest value / 최소값
     * @param hi the highest value / 최대값
     * @return the limited value / 제한된 값
     */
    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
