package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * The loop filter of VP8 (RFC 6386 section 15): after the whole frame is reconstructed, the edges between macroblocks and between the
 * subblocks inside a macroblock are smoothed, macroblock by macroblock in raster order, each macroblock left edge, inner vertical edges, top
 * edge and inner horizontal edges in this order. Two variants exist, the simple filter (luma only) and the normal filter (luma and chroma).
 * The arithmetic follows the reference decoder of the specification.
 * <p>
 * VP8 의 루프 필터(RFC 6386 15절): 프레임 전체가 복원된 뒤 매크로블록 사이와 매크로블록 안 서브블록 사이의 경계를 매크로블록 단위로 래스터
 * 순서로 다듬는다. 각 매크로블록에서 왼쪽 경계, 안쪽 세로 경계, 위쪽 경계, 안쪽 가로 경계 순서이다. 단순 필터(휘도만)와 일반 필터(휘도와 색차)가
 * 있다. 산술은 규격의 참조 디코더를 따른다.
 */
final class Vp8LoopFilter {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Vp8LoopFilter() {
    }

    /**
     * Saturates a value to the range of a signed byte.
     * <p>
     * 값을 부호 있는 바이트 범위로 포화시킨다.
     *
     * @param x value / 값
     * @return -128..127 / -128..127
     */
    private static int s8(int x) {
        return x < -128 ? -128 : (x > 127 ? 127 : x);
    }

    /**
     * Saturates a value to the range of an unsigned byte.
     * <p>
     * 값을 부호 없는 바이트 범위로 포화시킨다.
     *
     * @param x value / 값
     * @return 0..255 / 0..255
     */
    private static int u8(int x) {
        return x < 0 ? 0 : (x > 255 ? 255 : x);
    }

    /**
     * Filters a whole frame.
     * <p>
     * 프레임 전체를 필터링한다.
     *
     * @param f         the reconstructed frame, changed in place / 복원된 프레임(그 자리에서 바뀜)
     * @param simple    true for the simple filter / 단순 필터이면 true
     * @param sharpness sharpness level 0..7 of the frame header / 프레임 헤더의 선명도 0..7
     * @param levels    filter level 0..63 of every macroblock (raster order); 0 skips the macroblock / 매크로블록별 필터 수준 0..63(래스터 순서). 0 이면 건너뜀
     * @param inner     for every macroblock whether the edges between its subblocks are filtered / 매크로블록별로 서브블록 사이 경계를 필터링하는지 여부
     */
    static void filterFrame(Vp8Frame f, boolean simple, int sharpness, int[] levels, boolean[] inner) {
        // 매크로블록을 래스터 순서로 처리한다.
        for (int mby = 0; mby < f.mbRows; mby++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            // 행 안의 매크로블록을 왼쪽부터 처리한다.
            for (int mbx = 0; mbx < f.mbCols; mbx++) {
                int mb = mby * f.mbCols + mbx;
                int level = levels[mb];
                if (level == 0) {
                    continue;
                }
                int interior = level;
                if (sharpness != 0) {
                    interior >>= sharpness > 4 ? 2 : 1;
                    if (interior > 9 - sharpness) {
                        interior = 9 - sharpness;
                    }
                }
                if (interior < 1) {
                    interior = 1;
                }
                int hev = level >= 40 ? 2 : (level >= 15 ? 1 : 0);
                if (simple) {
                    filterMbSimple(f, mbx, mby, level, interior, inner[mb]);
                } else {
                    filterMbNormal(f, mbx, mby, level, interior, hev, inner[mb]);
                }
            }
        }
    }

    /**
     * Filters one macroblock with the simple filter (luma plane only).
     * <p>
     * 매크로블록 하나를 단순 필터로 필터링한다(휘도 평면만).
     *
     * @param f        frame / 프레임
     * @param mbx      macroblock column / 매크로블록 열
     * @param mby      macroblock row / 매크로블록 행
     * @param level    filter level / 필터 수준
     * @param interior interior limit / 내부 한계
     * @param inner    whether inner edges are filtered / 안쪽 경계를 필터링하는지
     */
    private static void filterMbSimple(Vp8Frame f, int mbx, int mby, int level, int interior, boolean inner) {
        int stride = f.yStride;
        int base = f.yIndex(mbx * 16, mby * 16);
        int mbLimit = (level + 2) * 2 + interior;
        int subLimit = level * 2 + interior;
        if (mbx > 0) {
            simpleEdge(f.y, base, 1, stride, mbLimit);
        }
        if (inner) {
            simpleEdge(f.y, base + 4, 1, stride, subLimit);
            simpleEdge(f.y, base + 8, 1, stride, subLimit);
            simpleEdge(f.y, base + 12, 1, stride, subLimit);
        }
        if (mby > 0) {
            simpleEdge(f.y, base, stride, 1, mbLimit);
        }
        if (inner) {
            simpleEdge(f.y, base + 4 * stride, stride, 1, subLimit);
            simpleEdge(f.y, base + 8 * stride, stride, 1, subLimit);
            simpleEdge(f.y, base + 12 * stride, stride, 1, subLimit);
        }
    }

    /**
     * Filters one macroblock with the normal filter (luma and both chroma planes).
     * <p>
     * 매크로블록 하나를 일반 필터로 필터링한다(휘도와 두 색차 평면).
     *
     * @param f        frame / 프레임
     * @param mbx      macroblock column / 매크로블록 열
     * @param mby      macroblock row / 매크로블록 행
     * @param level    filter level / 필터 수준
     * @param interior interior limit / 내부 한계
     * @param hev      high edge variance threshold / 높은 경계 분산 임계값
     * @param inner    whether inner edges are filtered / 안쪽 경계를 필터링하는지
     */
    private static void filterMbNormal(Vp8Frame f, int mbx, int mby, int level, int interior, int hev, boolean inner) {
        int ys = f.yStride;
        int cs = f.uvStride;
        int yb = f.yIndex(mbx * 16, mby * 16);
        int ub = f.uvIndex(mbx * 8, mby * 8);
        int mbEdge = level + 2;
        if (mbx > 0) {
            mbEdgeFilter(f.y, yb, 1, ys, 16, mbEdge, interior, hev);
            mbEdgeFilter(f.u, ub, 1, cs, 8, mbEdge, interior, hev);
            mbEdgeFilter(f.v, ub, 1, cs, 8, mbEdge, interior, hev);
        }
        if (inner) {
            subEdgeFilter(f.y, yb + 4, 1, ys, 16, level, interior, hev);
            subEdgeFilter(f.y, yb + 8, 1, ys, 16, level, interior, hev);
            subEdgeFilter(f.y, yb + 12, 1, ys, 16, level, interior, hev);
            subEdgeFilter(f.u, ub + 4, 1, cs, 8, level, interior, hev);
            subEdgeFilter(f.v, ub + 4, 1, cs, 8, level, interior, hev);
        }
        if (mby > 0) {
            mbEdgeFilter(f.y, yb, ys, 1, 16, mbEdge, interior, hev);
            mbEdgeFilter(f.u, ub, cs, 1, 8, mbEdge, interior, hev);
            mbEdgeFilter(f.v, ub, cs, 1, 8, mbEdge, interior, hev);
        }
        if (inner) {
            subEdgeFilter(f.y, yb + 4 * ys, ys, 1, 16, level, interior, hev);
            subEdgeFilter(f.y, yb + 8 * ys, ys, 1, 16, level, interior, hev);
            subEdgeFilter(f.y, yb + 12 * ys, ys, 1, 16, level, interior, hev);
            subEdgeFilter(f.u, ub + 4 * cs, cs, 1, 8, level, interior, hev);
            subEdgeFilter(f.v, ub + 4 * cs, cs, 1, 8, level, interior, hev);
        }
    }

    /**
     * Applies the simple filter along one edge of 16 pixels.
     * <p>
     * 16픽셀 경계 하나에 단순 필터를 적용한다.
     *
     * @param px     plane / 평면
     * @param pos    index of the first pixel on the far side of the edge (q0) / 경계 뒤쪽 첫 픽셀(q0)의 색인
     * @param across distance between pixels across the edge / 경계를 가로지르는 방향의 픽셀 간 거리
     * @param along  distance between the pixels along the edge / 경계를 따르는 방향의 픽셀 간 거리
     * @param limit  edge limit / 경계 한계
     */
    static void simpleEdge(byte[] px, int pos, int across, int along, int limit) {
        // 경계를 따라 16개 위치를 차례로 본다.
        for (int i = 0; i < 16; i++, pos += along) {
            int p1 = px[pos - 2 * across] & 0xff;
            int p0 = px[pos - across] & 0xff;
            int q0 = px[pos] & 0xff;
            int q1 = px[pos + across] & 0xff;
            if (Math.abs(p0 - q0) * 2 + (Math.abs(p1 - q1) >> 1) <= limit) {
                commonAdjust(px, pos, across, true);
            }
        }
    }

    /**
     * Applies the normal filter along an edge between two macroblocks.
     * <p>
     * 두 매크로블록 사이 경계를 따라 일반 필터를 적용한다.
     *
     * @param px       plane / 평면
     * @param pos      index of q0 of the first position / 첫 위치의 q0 색인
     * @param across   distance across the edge / 경계를 가로지르는 거리
     * @param along    distance along the edge / 경계를 따르는 거리
     * @param count    number of positions along the edge / 경계를 따르는 위치 수
     * @param edge     edge limit (filter level plus 2) / 경계 한계(필터 수준 + 2)
     * @param interior interior limit / 내부 한계
     * @param hev      high edge variance threshold / 높은 경계 분산 임계값
     */
    private static void mbEdgeFilter(byte[] px, int pos, int across, int along, int count, int edge, int interior, int hev) {
        // 경계를 따라 위치마다 임계값을 확인하고 필터를 적용한다.
        for (int i = 0; i < count; i++, pos += along) {
            if (!normalThreshold(px, pos, across, edge, interior)) {
                continue;
            }
            if (highEdgeVariance(px, pos, across, hev)) {
                commonAdjust(px, pos, across, true);
            } else {
                mbAdjust(px, pos, across);
            }
        }
    }

    /**
     * Applies the normal filter along an edge between subblocks of one macroblock.
     * <p>
     * 한 매크로블록 안의 서브블록 사이 경계를 따라 일반 필터를 적용한다.
     *
     * @param px       plane / 평면
     * @param pos      index of q0 of the first position / 첫 위치의 q0 색인
     * @param across   distance across the edge / 경계를 가로지르는 거리
     * @param along    distance along the edge / 경계를 따르는 거리
     * @param count    number of positions along the edge / 경계를 따르는 위치 수
     * @param edge     edge limit (filter level) / 경계 한계(필터 수준)
     * @param interior interior limit / 내부 한계
     * @param hev      high edge variance threshold / 높은 경계 분산 임계값
     */
    private static void subEdgeFilter(byte[] px, int pos, int across, int along, int count, int edge, int interior, int hev) {
        // 경계를 따라 위치마다 임계값을 확인하고 필터를 적용한다.
        for (int i = 0; i < count; i++, pos += along) {
            if (normalThreshold(px, pos, across, edge, interior)) {
                commonAdjust(px, pos, across, highEdgeVariance(px, pos, across, hev));
            }
        }
    }

    /**
     * Tells whether the pixels at one position are smooth enough to be filtered with the normal filter.
     * <p>
     * 한 위치의 픽셀이 일반 필터로 필터링할 만큼 매끄러운지 알려 준다.
     *
     * @param px       plane / 평면
     * @param pos      index of q0 / q0 색인
     * @param s        distance across the edge / 경계를 가로지르는 거리
     * @param edge     edge limit / 경계 한계
     * @param interior interior limit / 내부 한계
     * @return true if the position is filtered / 필터링하면 true
     */
    private static boolean normalThreshold(byte[] px, int pos, int s, int edge, int interior) {
        int p3 = px[pos - 4 * s] & 0xff;
        int p2 = px[pos - 3 * s] & 0xff;
        int p1 = px[pos - 2 * s] & 0xff;
        int p0 = px[pos - s] & 0xff;
        int q0 = px[pos] & 0xff;
        int q1 = px[pos + s] & 0xff;
        int q2 = px[pos + 2 * s] & 0xff;
        int q3 = px[pos + 3 * s] & 0xff;
        return Math.abs(p0 - q0) * 2 + (Math.abs(p1 - q1) >> 1) <= 2 * edge + interior
                && Math.abs(p3 - p2) <= interior && Math.abs(p2 - p1) <= interior && Math.abs(p1 - p0) <= interior
                && Math.abs(q3 - q2) <= interior && Math.abs(q2 - q1) <= interior && Math.abs(q1 - q0) <= interior;
    }

    /**
     * Tells whether the edge has a high variance at one position.
     * <p>
     * 한 위치에서 경계의 분산이 큰지 알려 준다.
     *
     * @param px  plane / 평면
     * @param pos index of q0 / q0 색인
     * @param s   distance across the edge / 경계를 가로지르는 거리
     * @param hev threshold / 임계값
     * @return true for a high variance / 분산이 크면 true
     */
    private static boolean highEdgeVariance(byte[] px, int pos, int s, int hev) {
        int p1 = px[pos - 2 * s] & 0xff;
        int p0 = px[pos - s] & 0xff;
        int q0 = px[pos] & 0xff;
        int q1 = px[pos + s] & 0xff;
        return Math.abs(p1 - p0) > hev || Math.abs(q1 - q0) > hev;
    }

    /**
     * The common adjustment of the two pixels next to the edge (and of the next two if the outer taps are not used).
     * <p>
     * 경계 옆 두 픽셀의 공통 조정(바깥쪽 탭을 쓰지 않으면 그 다음 두 픽셀도 조정한다).
     *
     * @param px       plane / 평면
     * @param pos      index of q0 / q0 색인
     * @param s        distance across the edge / 경계를 가로지르는 거리
     * @param outerTaps whether p1 and q1 take part in the filter value / p1, q1 이 필터 값에 참여하는지
     */
    private static void commonAdjust(byte[] px, int pos, int s, boolean outerTaps) {
        int p1 = px[pos - 2 * s] & 0xff;
        int p0 = px[pos - s] & 0xff;
        int q0 = px[pos] & 0xff;
        int q1 = px[pos + s] & 0xff;
        int a = 3 * (q0 - p0);
        if (outerTaps) {
            a += s8(p1 - q1);
        }
        a = s8(a);
        int f1 = (a + 4 > 127 ? 127 : a + 4) >> 3;
        int f2 = (a + 3 > 127 ? 127 : a + 3) >> 3;
        px[pos - s] = (byte) u8(p0 + f2);
        px[pos] = (byte) u8(q0 - f1);
        if (!outerTaps) {
            a = (f1 + 1) >> 1;
            px[pos - 2 * s] = (byte) u8(p1 + a);
            px[pos + s] = (byte) u8(q1 - a);
        }
    }

    /**
     * The stronger adjustment of the three pixels on each side of a macroblock edge.
     * <p>
     * 매크로블록 경계 양쪽 세 픽셀씩에 대한 더 강한 조정.
     *
     * @param px  plane / 평면
     * @param pos index of q0 / q0 색인
     * @param s   distance across the edge / 경계를 가로지르는 거리
     */
    private static void mbAdjust(byte[] px, int pos, int s) {
        int p2 = px[pos - 3 * s] & 0xff;
        int p1 = px[pos - 2 * s] & 0xff;
        int p0 = px[pos - s] & 0xff;
        int q0 = px[pos] & 0xff;
        int q1 = px[pos + s] & 0xff;
        int q2 = px[pos + 2 * s] & 0xff;
        int w = s8(s8(p1 - q1) + 3 * (q0 - p0));
        int a = (27 * w + 63) >> 7;
        px[pos - s] = (byte) u8(p0 + a);
        px[pos] = (byte) u8(q0 - a);
        a = (18 * w + 63) >> 7;
        px[pos - 2 * s] = (byte) u8(p1 + a);
        px[pos + s] = (byte) u8(q1 - a);
        a = (9 * w + 63) >> 7;
        px[pos - 3 * s] = (byte) u8(p2 + a);
        px[pos + 2 * s] = (byte) u8(q2 - a);
    }
}
