package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The motion field of the AV1 decoder (plan item P18-03; AV1 specification sections 7.9 and 7.19): the projection of the motion vectors that
 * were stored with earlier frames onto the current frame (<code>motion field estimation</code>), which gives the temporal candidates of the
 * motion vector prediction, and the selection of the motion vectors of the current frame that are stored for later frames
 * (<code>motion field motion vector storage</code>). Vectors are kept for 8x8 luma blocks: the block at the odd 4x4 position inside it.
 * <p>
 * AV1 디코더의 움직임 필드(계획 P18-03; AV1 규격 7.9, 7.19절): 이전 프레임과 함께 저장한 움직임 벡터를 현재 프레임에 투영하여
 * (<code>motion field estimation</code>) 움직임 벡터 예측의 시간 후보를 얻고, 현재 프레임의 움직임 벡터 중 이후 프레임용으로 저장할 것을 고른다
 * (<code>motion field motion vector storage</code>). 벡터는 8x8 휘도 블록마다 그 안에서 홀수 번째 4x4 위치의 것을 보관한다.
 */
final class Av1MotionField {

    /**
     * The largest distance of two frames that is used.
     * <p>
     * 쓰는 두 프레임 거리의 최댓값.
     */
    private static final int MAX_FRAME_DISTANCE = 31;

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 가진다.
     */
    private Av1MotionField() {
    }

    /**
     * Runs the motion field estimation process (specification 7.9) and stores the result in the frame.
     * <p>
     * 움직임 필드 추정 과정(규격 7.9)을 실행하여 결과를 프레임에 저장한다.
     *
     * @param f the frame whose header has been read and whose reference list is set / 헤더를 읽었고 참조 목록을 정한 프레임
     */
    static void estimate(Av1Frame f) {
        Av1FrameHeader hdr = f.hdr;
        Av1Sequence seq = f.seq;
        int w8 = f.miCols >> 1;
        int h8 = f.miRows >> 1;
        f.motionFieldMvs = new short[8][];
        // 참조 프레임마다 움직임 필드를 무효 값으로 채운다.
        for (int ref = 1; ref <= 7; ref++) {
            short[] m = new short[w8 * h8 * 2];
            java.util.Arrays.fill(m, (short) Av1MvStack.INVALID_MV);
            f.motionFieldMvs[ref] = m;
        }
        int curGoldOrderHint = hdr.orderHints[4];
        int lastAltOrderHint = f.refList[1].savedOrderHints[7];
        boolean useLast = lastAltOrderHint != curGoldOrderHint;
        if (useLast) {
            project(f, seq, 1, -1);
        }
        int refStamp = 3 - 2;
        boolean useBwd = Av1FrameHeader.relativeDist(seq, hdr.orderHints[5], hdr.orderHint) > 0;
        if (useBwd && project(f, seq, 5, 1)) {
            refStamp--;
        }
        boolean useAlt2 = Av1FrameHeader.relativeDist(seq, hdr.orderHints[6], hdr.orderHint) > 0;
        if (useAlt2 && project(f, seq, 6, 1)) {
            refStamp--;
        }
        boolean useAlt = Av1FrameHeader.relativeDist(seq, hdr.orderHints[7], hdr.orderHint) > 0;
        if (useAlt && refStamp >= 0 && project(f, seq, 7, 1)) {
            refStamp--;
        }
        if (refStamp >= 0) {
            project(f, seq, 2, -1);
        }
    }

    /**
     * The projection process (specification 7.9.2): projects the stored vectors of one reference frame.
     * <p>
     * 투영 과정(규격 7.9.2): 참조 프레임 하나의 저장된 벡터를 투영한다.
     *
     * @param f       the frame / 프레임
     * @param seq     the sequence header / 시퀀스 헤더
     * @param src     the reference frame whose vectors are projected / 벡터를 투영할 참조 프레임
     * @param dstSign the sign of the direction (-1 or 1) / 방향의 부호(-1 또는 1)
     * @return true if the reference frame could be used / 참조 프레임을 쓸 수 있었으면 true
     */
    private static boolean project(Av1Frame f, Av1Sequence seq, int src, int dstSign) {
        Av1FrameHeader hdr = f.hdr;
        Av1RefFrame ref = f.refList[src];
        int w8 = f.miCols >> 1;
        int h8 = f.miRows >> 1;
        if (ref.miRows != f.miRows || ref.miCols != f.miCols || ref.frameType == Av1FrameHeader.INTRA_ONLY_FRAME
                || ref.frameType == Av1FrameHeader.KEY_FRAME || ref.savedRefFrames == null) {
            return false;
        }
        int[] projMv = new int[2];
        int[] pos = new int[2];
        // 참조 프레임의 8x8 블록마다 저장된 벡터를 현재 프레임의 위치로 투영한다.
        for (int y8 = 0; y8 < h8; y8++) {
            for (int x8 = 0; x8 < w8; x8++) {
                int cell = y8 * w8 + x8;
                int srcRef = ref.savedRefFrames[cell];
                if (srcRef <= 0) {
                    continue;
                }
                int refToCur = Av1FrameHeader.relativeDist(seq, hdr.orderHints[src], hdr.orderHint);
                int refOffset = Av1FrameHeader.relativeDist(seq, hdr.orderHints[src], ref.savedOrderHints[srcRef]);
                boolean posValid = Math.abs(refToCur) <= MAX_FRAME_DISTANCE && Math.abs(refOffset) <= MAX_FRAME_DISTANCE && refOffset > 0;
                if (!posValid) {
                    continue;
                }
                int mvRow = ref.savedMvs[cell * 2];
                int mvCol = ref.savedMvs[cell * 2 + 1];
                projection(mvRow, mvCol, refToCur * dstSign, refOffset, projMv);
                posValid = blockPosition(f, x8, y8, dstSign, projMv, pos);
                if (!posValid) {
                    continue;
                }
                // 현재 프레임의 각 참조 프레임까지의 거리에 맞춰 다시 투영하여 저장한다.
                for (int dst = 1; dst <= 7; dst++) {
                    int refToDst = Av1FrameHeader.relativeDist(seq, hdr.orderHint, hdr.orderHints[dst]);
                    projection(mvRow, mvCol, refToDst, refOffset, projMv);
                    int o = (pos[0] * w8 + pos[1]) * 2;
                    f.motionFieldMvs[dst][o] = (short) projMv[0];
                    f.motionFieldMvs[dst][o + 1] = (short) projMv[1];
                }
            }
        }
        return true;
    }

    /**
     * The get MV projection process (specification 7.9.3): scales a vector by the ratio of two frame distances.
     * <p>
     * 움직임 벡터 투영 과정(규격 7.9.3): 벡터를 두 프레임 거리의 비로 늘이거나 줄인다.
     *
     * @param mvRow       the vertical component / 세로 성분
     * @param mvCol       the horizontal component / 가로 성분
     * @param numerator   the number of frames that the result covers / 결과가 덮는 프레임 수
     * @param denominator the number of frames that the vector covers / 벡터가 덮는 프레임 수
     * @param out         the projected vector / 투영된 벡터
     */
    private static void projection(int mvRow, int mvCol, int numerator, int denominator, int[] out) {
        int clippedDen = Math.min(MAX_FRAME_DISTANCE, denominator);
        int clippedNum = Math.max(-MAX_FRAME_DISTANCE, Math.min(MAX_FRAME_DISTANCE, numerator));
        int mult = AvifInterTables.DIV_MULT[clippedDen];
        long a = Av1MvStack.round2Signed((long) mvRow * clippedNum * mult, 14);
        long b = Av1MvStack.round2Signed((long) mvCol * clippedNum * mult, 14);
        out[0] = (int) Math.max(-(1 << 14) + 1, Math.min((1 << 14) - 1, a));
        out[1] = (int) Math.max(-(1 << 14) + 1, Math.min((1 << 14) - 1, b));
    }

    /**
     * The get block position process (specification 7.9.4).
     * <p>
     * 블록 위치 구하기 과정(규격 7.9.4).
     *
     * @param f       the frame / 프레임
     * @param x8      the column in 8x8 units / 8x8 단위 열
     * @param y8      the row in 8x8 units / 8x8 단위 행
     * @param dstSign the sign of the direction / 방향의 부호
     * @param projMv  the projected vector / 투영된 벡터
     * @param pos     the projected row and column (8x8 units) / 투영된 행과 열(8x8 단위)
     * @return true if the position is valid / 위치가 올바르면 true
     */
    private static boolean blockPosition(Av1Frame f, int x8, int y8, int dstSign, int[] projMv, int[] pos) {
        boolean validY = projectCoordinate(y8, projMv[0], dstSign, f.miRows >> 1, 0, pos, 0);
        boolean validX = projectCoordinate(x8, projMv[1], dstSign, f.miCols >> 1, 8, pos, 1);
        return validY && validX;
    }

    /**
     * The <code>project</code> function of the specification: moves a position by a vector and checks that it stays near.
     * <p>
     * 규격의 <code>project</code> 함수: 위치를 벡터만큼 옮기고 너무 멀지 않은지 확인한다.
     *
     * @param v8      the position in 8x8 units / 8x8 단위 위치
     * @param delta   the vector component / 벡터 성분
     * @param dstSign the sign of the direction / 방향의 부호
     * @param max8    the number of 8x8 units of the frame in this direction / 이 방향 프레임의 8x8 단위 수
     * @param maxOff8 the largest offset from the 64x64 block / 64x64 블록에서 허용하는 최대 오프셋
     * @param pos     the output array / 출력 배열
     * @param idx     the index in the output array / 출력 배열의 번호
     * @return true if the position is valid / 위치가 올바르면 true
     */
    private static boolean projectCoordinate(int v8, int delta, int dstSign, int max8, int maxOff8, int[] pos, int idx) {
        int base8 = (v8 >> 3) << 3;
        int offset8 = delta >= 0 ? delta >> (3 + 1 + 2) : -((-delta) >> (3 + 1 + 2));
        v8 += dstSign * offset8;
        pos[idx] = v8;
        return !(v8 < 0 || v8 >= max8 || v8 < base8 - maxOff8 || v8 >= base8 + 8 + maxOff8);
    }

    /**
     * The motion field motion vector storage process (specification 7.19): chooses for each 8x8 block the vector that later frames can
     * project, and keeps it in the reference frame.
     * <p>
     * 움직임 필드 움직임 벡터 저장 과정(규격 7.19): 8x8 블록마다 이후 프레임이 투영할 수 있는 벡터를 골라 참조 프레임에 보관한다.
     *
     * @param f   the finished frame / 완성된 프레임
     * @param seq the sequence header / 시퀀스 헤더
     * @param ref the reference frame record to fill / 채울 참조 프레임 기록
     */
    static void store(Av1Frame f, Av1Sequence seq, Av1RefFrame ref) {
        Av1FrameHeader hdr = f.hdr;
        int w8 = f.miCols >> 1;
        int h8 = f.miRows >> 1;
        ref.savedRefFrames = new byte[w8 * h8];
        ref.savedMvs = new short[w8 * h8 * 2];
        // 8x8 블록마다 안쪽 홀수 4x4 위치의 모드 정보를 본다.
        for (int y8 = 0; y8 < h8; y8++) {
            for (int x8 = 0; x8 < w8; x8++) {
                int i = (2 * y8 + 1) * f.miStride + 2 * x8 + 1;
                int cell = y8 * w8 + x8;
                int mfRef = -1;
                int mvRow = 0;
                int mvCol = 0;
                // 두 목록 중 후방 참조가 아닌 마지막 목록의 작은 벡터를 고른다.
                for (int list = 0; list < 2; list++) {
                    int r = list == 0 ? f.refFrame0[i] : f.refFrame1[i];
                    if (r > 0) {
                        int dist = Av1FrameHeader.relativeDist(seq, hdr.orderHints[r], hdr.orderHint);
                        if (dist < 0) {
                            int mr = f.mvs[i * 4 + list * 2];
                            int mc = f.mvs[i * 4 + list * 2 + 1];
                            if (Math.abs(mr) <= (1 << 12) - 1 && Math.abs(mc) <= (1 << 12) - 1) {
                                mfRef = r;
                                mvRow = mr;
                                mvCol = mc;
                            }
                        }
                    }
                }
                ref.savedRefFrames[cell] = (byte) mfRef;
                ref.savedMvs[cell * 2] = (short) mvRow;
                ref.savedMvs[cell * 2 + 1] = (short) mvCol;
            }
        }
    }
}
