package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The motion vector prediction of the AV1 decoder (plan item P18-03; AV1 specification section 7.10.2, "find MV stack process"): for one block
 * it scans the neighbouring blocks (and the projected motion field of earlier frames) for motion vectors that use the same reference frames,
 * collects them in a stack ordered by weight, and derives the contexts that the mode info syntax needs. Intra block copy uses the same process
 * with the intra frame as the only reference.
 * <p>
 * Usage: create one object per tile, call {@link #find} for every inter block and read {@link #numMvFound}, {@link #mv} (the stack),
 * {@link #globalMv} and the three contexts.
 * <p>
 * AV1 디코더의 움직임 벡터 예측(계획 P18-03; AV1 규격 7.10.2절 "find MV stack process"): 블록 하나에 대해 이웃 블록(과 이전 프레임의 투영된 움직임
 * 필드)에서 같은 참조 프레임을 쓰는 움직임 벡터를 찾아 가중치 순서의 스택에 모으고 모드 정보 구문이 필요로 하는 문맥을 구한다. 인트라 블록 복사는
 * 인트라 프레임을 유일한 참조로 하여 같은 과정을 쓴다.
 * <p>
 * 사용법: 타일마다 객체를 하나 만들고, 인터 블록마다 {@link #find} 를 호출한 뒤 {@link #numMvFound}, {@link #mv}(스택), {@link #globalMv}, 세 문맥을 읽는다.
 */
final class Av1MvStack {

    /**
     * The mode value NEARESTMV (YMode of an inter block).
     * <p>
     * 모드 값 NEARESTMV(인터 블록의 YMode).
     */
    static final int NEARESTMV = 14;

    /**
     * The mode value NEARMV.
     * <p>
     * 모드 값 NEARMV.
     */
    static final int NEARMV = 15;

    /**
     * The mode value GLOBALMV.
     * <p>
     * 모드 값 GLOBALMV.
     */
    static final int GLOBALMV = 16;

    /**
     * The mode value NEWMV.
     * <p>
     * 모드 값 NEWMV.
     */
    static final int NEWMV = 17;

    /**
     * The mode value NEAREST_NEARESTMV.
     * <p>
     * 모드 값 NEAREST_NEARESTMV.
     */
    static final int NEAREST_NEARESTMV = 18;

    /**
     * The mode value NEAR_NEARMV.
     * <p>
     * 모드 값 NEAR_NEARMV.
     */
    static final int NEAR_NEARMV = 19;

    /**
     * The mode value NEAREST_NEWMV.
     * <p>
     * 모드 값 NEAREST_NEWMV.
     */
    static final int NEAREST_NEWMV = 20;

    /**
     * The mode value NEW_NEARESTMV.
     * <p>
     * 모드 값 NEW_NEARESTMV.
     */
    static final int NEW_NEARESTMV = 21;

    /**
     * The mode value NEAR_NEWMV.
     * <p>
     * 모드 값 NEAR_NEWMV.
     */
    static final int NEAR_NEWMV = 22;

    /**
     * The mode value NEW_NEARMV.
     * <p>
     * 모드 값 NEW_NEARMV.
     */
    static final int NEW_NEARMV = 23;

    /**
     * The mode value GLOBAL_GLOBALMV.
     * <p>
     * 모드 값 GLOBAL_GLOBALMV.
     */
    static final int GLOBAL_GLOBALMV = 24;

    /**
     * The mode value NEW_NEWMV.
     * <p>
     * 모드 값 NEW_NEWMV.
     */
    static final int NEW_NEWMV = 25;

    /**
     * The global motion type TRANSLATION (types above it are warped models).
     * <p>
     * 전역 움직임 종류 TRANSLATION(이보다 큰 종류는 워프 모델).
     */
    static final int TRANSLATION = 1;

    /**
     * The largest number of entries of the stack of candidate vectors.
     * <p>
     * 후보 벡터 스택의 최대 항목 수.
     */
    static final int MAX_REF_MV_STACK_SIZE = 8;

    /**
     * The bonus weight of the vectors of the immediate neighbours.
     * <p>
     * 바로 이웃 블록의 벡터에 주는 가산 가중치.
     */
    private static final int REF_CAT_LEVEL = 640;

    /**
     * The border (in 1/8 samples) that vectors may point beyond the frame.
     * <p>
     * 벡터가 프레임 밖을 가리킬 수 있는 여유(1/8 샘플 단위).
     */
    private static final int MV_BORDER = 128;

    /**
     * The invalid value in the projected motion field.
     * <p>
     * 투영된 움직임 필드의 무효 값.
     */
    static final int INVALID_MV = -1 << 15;

    /**
     * The frame whose mode info is scanned.
     * <p>
     * 모드 정보를 훑는 프레임.
     */
    private final Av1Frame f;

    /**
     * The header of the frame.
     * <p>
     * 프레임의 헤더.
     */
    private final Av1FrameHeader hdr;

    /**
     * The first 4x4 row of the tile.
     * <p>
     * 타일의 첫 4x4 행.
     */
    private final int rowStart;

    /**
     * The first 4x4 row after the tile.
     * <p>
     * 타일 다음의 첫 4x4 행.
     */
    private final int rowEnd;

    /**
     * The first 4x4 column of the tile.
     * <p>
     * 타일의 첫 4x4 열.
     */
    private final int colStart;

    /**
     * The first 4x4 column after the tile.
     * <p>
     * 타일 다음의 첫 4x4 열.
     */
    private final int colEnd;

    /**
     * The candidate vectors: entry idx, list, component at <code>(idx * 2 + list) * 2 + comp</code>; entries up to {@link #numMvFound} are valid
     * (and two entries after it for a single prediction block: the global vectors).
     * <p>
     * 후보 벡터: 항목 idx, 목록, 성분은 <code>(idx * 2 + list) * 2 + comp</code> 위치. {@link #numMvFound} 까지의 항목이 유효하다(단일 예측 블록에서는
     * 그 뒤 두 항목에 전역 벡터가 들어갈 수 있다).
     */
    final int[] mv = new int[(MAX_REF_MV_STACK_SIZE + 2) * 4];

    /**
     * The weights of the candidates.
     * <p>
     * 후보의 가중치.
     */
    final int[] weight = new int[MAX_REF_MV_STACK_SIZE + 2];

    /**
     * The global motion vectors of the two reference frames: list * 2 + component.
     * <p>
     * 두 참조 프레임의 전역 움직임 벡터: 목록 * 2 + 성분.
     */
    final int[] globalMv = new int[4];

    /**
     * The context of the candidate index syntax elements per candidate.
     * <p>
     * 후보별 후보 인덱스 구문 요소의 문맥.
     */
    final int[] drlCtxStack = new int[MAX_REF_MV_STACK_SIZE + 2];

    /**
     * The number of candidates found.
     * <p>
     * 찾은 후보 수.
     */
    int numMvFound;

    /**
     * The context of the new_mv syntax element.
     * <p>
     * new_mv 구문 요소의 문맥.
     */
    int newMvContext;

    /**
     * The context of the ref_mv syntax element.
     * <p>
     * ref_mv 구문 요소의 문맥.
     */
    int refMvContext;

    /**
     * The context of the zero_mv syntax element.
     * <p>
     * zero_mv 구문 요소의 문맥.
     */
    int zeroMvContext;

    /**
     * The number of candidates that use a new motion vector.
     * <p>
     * 새 움직임 벡터를 쓰는 후보 수.
     */
    private int newMvCount;

    /**
     * True if the last scan found a candidate with the same reference frames.
     * <p>
     * 마지막 훑기가 같은 참조 프레임의 후보를 찾았으면 true.
     */
    private boolean foundMatch;

    /**
     * The number of the above and left directions with a close candidate.
     * <p>
     * 가까운 후보가 있는 위쪽/왼쪽 방향의 수.
     */
    private int closeMatches;

    /**
     * The number of the above and left directions with a candidate.
     * <p>
     * 후보가 있는 위쪽/왼쪽 방향의 수.
     */
    private int totalMatches;

    /**
     * The row of the block in 4x4 units.
     * <p>
     * 블록의 행(4x4 단위).
     */
    private int miRow;

    /**
     * The column of the block in 4x4 units.
     * <p>
     * 블록의 열(4x4 단위).
     */
    private int miCol;

    /**
     * The size of the block.
     * <p>
     * 블록 크기.
     */
    private int miSize;

    /**
     * The first reference frame of the block.
     * <p>
     * 블록의 첫 참조 프레임.
     */
    private int ref0;

    /**
     * The second reference frame of the block.
     * <p>
     * 블록의 둘째 참조 프레임.
     */
    private int ref1;

    /**
     * The number of the partially matching candidates of the same reference frame (extra search of compound prediction).
     * <p>
     * 같은 참조 프레임을 가진 부분 일치 후보 수(복합 예측의 추가 탐색).
     */
    private final int[] refIdCount = new int[2];

    /**
     * The number of the partially matching candidates of another reference frame.
     * <p>
     * 다른 참조 프레임을 가진 부분 일치 후보 수.
     */
    private final int[] refDiffCount = new int[2];

    /**
     * The partially matching candidates of the same reference frame: list, index, component.
     * <p>
     * 같은 참조 프레임을 가진 부분 일치 후보: 목록, 인덱스, 성분.
     */
    private final int[] refIdMvs = new int[2 * 2 * 2];

    /**
     * The partially matching candidates of another reference frame: list, index, component.
     * <p>
     * 다른 참조 프레임을 가진 부분 일치 후보: 목록, 인덱스, 성분.
     */
    private final int[] refDiffMvs = new int[2 * 2 * 2];

    /**
     * Creates the predictor of a tile.
     * <p>
     * 타일의 예측기를 만든다.
     *
     * @param f        the frame / 프레임
     * @param rowStart the first row of the tile / 타일의 첫 행
     * @param rowEnd   the row after the tile / 타일 다음 행
     * @param colStart the first column of the tile / 타일의 첫 열
     * @param colEnd   the column after the tile / 타일 다음 열
     */
    Av1MvStack(Av1Frame f, int rowStart, int rowEnd, int colStart, int colEnd) {
        this.f = f;
        this.hdr = f.hdr;
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
        this.colStart = colStart;
        this.colEnd = colEnd;
    }

    /**
     * True if the 4x4 position is inside the tile.
     * <p>
     * 4x4 위치가 타일 안이면 true.
     *
     * @param r the row / 행
     * @param c the column / 열
     * @return true if inside / 안이면 true
     */
    private boolean isInside(int r, int c) {
        return c >= colStart && c < colEnd && r >= rowStart && r < rowEnd;
    }

    /**
     * Runs the find MV stack process (specification 7.10.2) for a block.
     * <p>
     * 블록에 대해 find MV stack 과정(규격 7.10.2)을 실행한다.
     *
     * @param row      the row of the block (4x4 units) / 블록의 행(4x4 단위)
     * @param col      the column of the block / 블록의 열
     * @param size     the block size / 블록 크기
     * @param refFrame0 the first reference frame (0 for intra block copy) / 첫 참조 프레임(인트라 블록 복사는 0)
     * @param refFrame1 the second reference frame (-1 for a single prediction) / 둘째 참조 프레임(단일 예측은 -1)
     */
    void find(int row, int col, int size, int refFrame0, int refFrame1) {
        this.miRow = row;
        this.miCol = col;
        this.miSize = size;
        this.ref0 = refFrame0;
        this.ref1 = refFrame1;
        boolean isCompound = refFrame1 > 0;
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[size];
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[size];
        numMvFound = 0;
        newMvCount = 0;
        setupGlobalMv(0);
        if (isCompound) {
            setupGlobalMv(1);
        }
        foundMatch = false;
        scanRow(-1, isCompound);
        boolean foundAboveMatch = foundMatch;
        foundMatch = false;
        scanCol(-1, isCompound);
        boolean foundLeftMatch = foundMatch;
        foundMatch = false;
        if (Math.max(bw4, bh4) <= 16) {
            scanPoint(-1, bw4, isCompound);
        }
        if (foundMatch) {
            foundAboveMatch = true;
        }
        closeMatches = (foundAboveMatch ? 1 : 0) + (foundLeftMatch ? 1 : 0);
        int numNearest = numMvFound;
        int numNew = newMvCount;
        // 가까운 이웃에서 찾은 후보는 가중치를 더한다.
        for (int i = 0; i < numNearest; i++) {
            weight[i] += REF_CAT_LEVEL;
        }
        zeroMvContext = 0;
        if (hdr.useRefFrameMvs) {
            temporalScan(isCompound);
        }
        scanPoint(-1, -1, isCompound);
        if (foundMatch) {
            foundAboveMatch = true;
        }
        foundMatch = false;
        scanRow(-3, isCompound);
        if (foundMatch) {
            foundAboveMatch = true;
        }
        foundMatch = false;
        scanCol(-3, isCompound);
        if (foundMatch) {
            foundLeftMatch = true;
        }
        foundMatch = false;
        if (bh4 > 1) {
            scanRow(-5, isCompound);
        }
        if (foundMatch) {
            foundAboveMatch = true;
        }
        foundMatch = false;
        if (bw4 > 1) {
            scanCol(-5, isCompound);
        }
        if (foundMatch) {
            foundLeftMatch = true;
        }
        totalMatches = (foundAboveMatch ? 1 : 0) + (foundLeftMatch ? 1 : 0);
        sort(0, numNearest);
        sort(numNearest, numMvFound);
        if (numMvFound < 2) {
            extraSearch(isCompound);
        }
        contextAndClamping(isCompound, numNew);
    }

    /**
     * The setup global MV process (specification 7.10.2.1).
     * <p>
     * 전역 움직임 벡터 설정 과정(규격 7.10.2.1).
     *
     * @param refList the list 0 or 1 / 목록 0 또는 1
     */
    private void setupGlobalMv(int refList) {
        int ref = refList == 0 ? ref0 : ref1;
        int bw = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4;
        int bh = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4;
        int mvRow;
        int mvCol;
        int typ = ref > 0 ? hdr.gmType[ref] : 0;
        if (ref <= 0 || typ == 0) {
            mvRow = 0;
            mvCol = 0;
        } else if (typ == TRANSLATION) {
            mvRow = hdr.gmParams[ref][0] >> (16 - 3);
            mvCol = hdr.gmParams[ref][1] >> (16 - 3);
        } else {
            int[] gm = hdr.gmParams[ref];
            int x = miCol * 4 + bw / 2 - 1;
            int y = miRow * 4 + bh / 2 - 1;
            long xc = (long) (gm[2] - (1 << 16)) * x + (long) gm[3] * y + gm[0];
            long yc = (long) gm[4] * x + (long) (gm[5] - (1 << 16)) * y + gm[1];
            if (hdr.allowHighPrecisionMv) {
                mvRow = (int) round2Signed(yc, 16 - 3);
                mvCol = (int) round2Signed(xc, 16 - 3);
            } else {
                mvRow = (int) round2Signed(yc, 16 - 2) * 2;
                mvCol = (int) round2Signed(xc, 16 - 2) * 2;
            }
        }
        globalMv[refList * 2] = lowerPrecision(mvRow);
        globalMv[refList * 2 + 1] = lowerPrecision(mvCol);
    }

    /**
     * Round2Signed of the specification for 64 bit values.
     * <p>
     * 64비트 값에 대한 규격의 Round2Signed.
     *
     * @param x the value / 값
     * @param n the number of bits / 비트 수
     * @return the rounded value / 반올림한 값
     */
    static long round2Signed(long x, int n) {
        if (n == 0) {
            return x;
        }
        if (x >= 0) {
            return (x + (1L << (n - 1))) >> n;
        }
        return -((-x + (1L << (n - 1))) >> n);
    }

    /**
     * The lower precision process (specification 7.10.2.10) for one component.
     * <p>
     * 정밀도 낮춤 과정(규격 7.10.2.10)을 성분 하나에 적용한다.
     *
     * @param v the component / 성분
     * @return the component with the lower precision / 정밀도를 낮춘 성분
     */
    private int lowerPrecision(int v) {
        if (hdr.allowHighPrecisionMv) {
            return v;
        }
        if (hdr.forceIntegerMv) {
            int a = Math.abs(v);
            int aInt = (a + 3) >> 3;
            return v > 0 ? aInt << 3 : -(aInt << 3);
        }
        if ((v & 1) != 0) {
            return v > 0 ? v - 1 : v + 1;
        }
        return v;
    }

    /**
     * The scan row process (specification 7.10.2.2).
     * <p>
     * 행 훑기 과정(규격 7.10.2.2).
     *
     * @param deltaRow   how far above to look (4x4 units) / 얼마나 위를 볼지(4x4 단위)
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void scanRow(int deltaRow, boolean isCompound) {
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int end4 = Math.min(Math.min(bw4, f.miCols - miCol), 16);
        int deltaCol = 0;
        boolean useStep16 = bw4 >= 16;
        if (Math.abs(deltaRow) > 1) {
            deltaRow += miRow & 1;
            deltaCol = 1 - (miCol & 1);
        }
        int i = 0;
        // 위쪽 행을 따라 블록 단위로 후보를 확인한다.
        while (i < end4) {
            int mvRow = miRow + deltaRow;
            int mvCol = miCol + deltaCol + i;
            if (!isInside(mvRow, mvCol)) {
                break;
            }
            int len = Math.min(bw4, AvifTables.NUM_4X4_BLOCKS_WIDE[f.miSize[mvRow * f.miStride + mvCol]]);
            if (Math.abs(deltaRow) > 1) {
                len = Math.max(2, len);
            }
            if (useStep16) {
                len = Math.max(4, len);
            }
            addRefMvCandidate(mvRow, mvCol, isCompound, len * 2);
            i += len;
        }
    }

    /**
     * The scan col process (specification 7.10.2.3).
     * <p>
     * 열 훑기 과정(규격 7.10.2.3).
     *
     * @param deltaCol   how far to the left to look (4x4 units) / 얼마나 왼쪽을 볼지(4x4 단위)
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void scanCol(int deltaCol, boolean isCompound) {
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        int end4 = Math.min(Math.min(bh4, f.miRows - miRow), 16);
        int deltaRow = 0;
        boolean useStep16 = bh4 >= 16;
        if (Math.abs(deltaCol) > 1) {
            deltaRow = 1 - (miRow & 1);
            deltaCol += miCol & 1;
        }
        int i = 0;
        // 왼쪽 열을 따라 블록 단위로 후보를 확인한다.
        while (i < end4) {
            int mvRow = miRow + deltaRow + i;
            int mvCol = miCol + deltaCol;
            if (!isInside(mvRow, mvCol)) {
                break;
            }
            int len = Math.min(bh4, AvifTables.NUM_4X4_BLOCKS_HIGH[f.miSize[mvRow * f.miStride + mvCol]]);
            if (Math.abs(deltaCol) > 1) {
                len = Math.max(2, len);
            }
            if (useStep16) {
                len = Math.max(4, len);
            }
            addRefMvCandidate(mvRow, mvCol, isCompound, len * 2);
            i += len;
        }
    }

    /**
     * The scan point process (specification 7.10.2.4).
     * <p>
     * 점 훑기 과정(규격 7.10.2.4).
     *
     * @param deltaRow   the row offset (4x4 units) / 행 오프셋(4x4 단위)
     * @param deltaCol   the column offset / 열 오프셋
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void scanPoint(int deltaRow, int deltaCol, boolean isCompound) {
        int mvRow = miRow + deltaRow;
        int mvCol = miCol + deltaCol;
        // 후보 위치가 타일 안이고 이미 디코딩된 블록일 때만 쓴다.
        if (isInside(mvRow, mvCol) && f.refFrame0[mvRow * f.miStride + mvCol] != Av1Frame.NOT_WRITTEN) {
            addRefMvCandidate(mvRow, mvCol, isCompound, 4);
        }
    }

    /**
     * The temporal scan process (specification 7.10.2.5): candidates from the projected motion field of earlier frames.
     * <p>
     * 시간 훑기 과정(규격 7.10.2.5): 이전 프레임의 투영된 움직임 필드에서 후보를 얻는다.
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void temporalScan(boolean isCompound) {
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        int stepW4 = bw4 >= 16 ? 4 : 2;
        int stepH4 = bh4 >= 16 ? 4 : 2;
        // 블록 안을 일정한 간격으로 훑는다.
        for (int deltaRow = 0; deltaRow < Math.min(bh4, 16); deltaRow += stepH4) {
            for (int deltaCol = 0; deltaCol < Math.min(bw4, 16); deltaCol += stepW4) {
                addTplRefMv(deltaRow, deltaCol, isCompound);
            }
        }
        boolean allowExtension = bh4 >= 2 && bh4 < 16 && bw4 >= 2 && bw4 < 16;
        if (allowExtension) {
            int[] posRow = { bh4, bh4, bh4 - 2 };
            int[] posCol = { -2, bw4, bw4 };
            // 같은 64x64 안에 있는 블록 바깥 세 위치도 확인한다.
            for (int i = 0; i < 3; i++) {
                int row = (miRow & 15) + posRow[i];
                int col = (miCol & 15) + posCol[i];
                if (row >= 0 && row < 16 && col >= 0 && col < 16) {
                    addTplRefMv(posRow[i], posCol[i], isCompound);
                }
            }
        }
    }

    /**
     * The temporal sample process (specification 7.10.2.6).
     * <p>
     * 시간 샘플 과정(규격 7.10.2.6).
     *
     * @param deltaRow   the row offset (4x4 units) / 행 오프셋(4x4 단위)
     * @param deltaCol   the column offset / 열 오프셋
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void addTplRefMv(int deltaRow, int deltaCol, boolean isCompound) {
        int mvRow = (miRow + deltaRow) | 1;
        int mvCol = (miCol + deltaCol) | 1;
        if (!isInside(mvRow, mvCol)) {
            return;
        }
        int x8 = mvCol >> 1;
        int y8 = mvRow >> 1;
        int o = (y8 * (f.miCols >> 1) + x8) * 2;
        if (deltaRow == 0 && deltaCol == 0) {
            zeroMvContext = 1;
        }
        int[] cand = new int[4];
        short[] field0 = f.motionFieldMvs[ref0];
        if (field0[o] == INVALID_MV) {
            return;
        }
        cand[0] = field0[o];
        cand[1] = field0[o + 1];
        if (isCompound) {
            short[] field1 = f.motionFieldMvs[ref1];
            if (field1[o] == INVALID_MV) {
                return;
            }
            cand[2] = field1[o];
            cand[3] = field1[o + 1];
        }
        int lists = isCompound ? 2 : 1;
        // 후보 벡터의 정밀도를 낮춘다.
        for (int i = 0; i < lists * 2; i++) {
            cand[i] = lowerPrecision(cand[i]);
        }
        if (deltaRow == 0 && deltaCol == 0) {
            boolean far = Math.abs(cand[0] - globalMv[0]) >= 16 || Math.abs(cand[1] - globalMv[1]) >= 16;
            if (isCompound) {
                far = far || Math.abs(cand[2] - globalMv[2]) >= 16 || Math.abs(cand[3] - globalMv[3]) >= 16;
            }
            zeroMvContext = far ? 1 : 0;
        }
        Av1ToolStats.count(Av1ToolStats.TEMPORAL_MV);
        int idx;
        // 스택에 같은 후보가 있는지 찾는다.
        for (idx = 0; idx < numMvFound; idx++) {
            boolean same = cand[0] == mv[idx * 4] && cand[1] == mv[idx * 4 + 1];
            if (isCompound) {
                same = same && cand[2] == mv[idx * 4 + 2] && cand[3] == mv[idx * 4 + 3];
            }
            if (same) {
                break;
            }
        }
        if (idx < numMvFound) {
            weight[idx] += 2;
        } else if (numMvFound < MAX_REF_MV_STACK_SIZE) {
            // 후보 벡터의 성분을 스택 새 항목으로 복사한다.
            for (int i = 0; i < lists * 2; i++) {
                mv[numMvFound * 4 + i] = cand[i];
            }
            weight[numMvFound] = 2;
            numMvFound++;
        }
    }

    /**
     * The add reference motion vector process (specification 7.10.2.7).
     * <p>
     * 참조 움직임 벡터 추가 과정(규격 7.10.2.7).
     *
     * @param mvRow      the row of the candidate (4x4 units) / 후보의 행(4x4 단위)
     * @param mvCol      the column of the candidate / 후보의 열
     * @param isCompound true for compound prediction / 복합 예측이면 true
     * @param w          the weight / 가중치
     */
    private void addRefMvCandidate(int mvRow, int mvCol, boolean isCompound, int w) {
        int i = mvRow * f.miStride + mvCol;
        if (f.isInter[i] == 0) {
            return;
        }
        if (!isCompound) {
            // 후보의 두 목록 중 현재 참조 프레임과 같은 것을 스택에 넣는다.
            if (f.refFrame0[i] == ref0) {
                searchStack(i, 0, w);
            }
            if (f.refFrame1[i] == ref0) {
                searchStack(i, 1, w);
            }
        } else if (f.refFrame0[i] == ref0 && f.refFrame1[i] == ref1) {
            compoundSearchStack(i, w);
        }
    }

    /**
     * The search stack process (specification 7.10.2.8).
     * <p>
     * 스택 탐색 과정(규격 7.10.2.8).
     *
     * @param i        the index of the candidate block / 후보 블록의 인덱스
     * @param candList the list of the candidate that matches / 일치하는 후보의 목록
     * @param w        the weight / 가중치
     */
    private void searchStack(int i, int candList, int w) {
        int candMode = f.yMode[i];
        int candSize = f.miSize[i];
        boolean large = Math.min(AvifTables.NUM_4X4_BLOCKS_WIDE[candSize], AvifTables.NUM_4X4_BLOCKS_HIGH[candSize]) >= 2;
        int r;
        int c;
        if ((candMode == GLOBALMV || candMode == GLOBAL_GLOBALMV) && ref0 > 0 && hdr.gmType[ref0] > TRANSLATION && large) {
            r = globalMv[0];
            c = globalMv[1];
        } else {
            r = f.mvs[i * 4 + candList * 2];
            c = f.mvs[i * 4 + candList * 2 + 1];
        }
        r = lowerPrecision(r);
        c = lowerPrecision(c);
        if (hasNewMv(candMode)) {
            newMvCount++;
        }
        foundMatch = true;
        int idx;
        // 스택에서 같은 벡터를 찾는다.
        for (idx = 0; idx < numMvFound; idx++) {
            if (mv[idx * 4] == r && mv[idx * 4 + 1] == c) {
                break;
            }
        }
        if (idx < numMvFound) {
            weight[idx] += w;
        } else if (numMvFound < MAX_REF_MV_STACK_SIZE) {
            mv[numMvFound * 4] = r;
            mv[numMvFound * 4 + 1] = c;
            weight[numMvFound] = w;
            numMvFound++;
        }
    }

    /**
     * The compound search stack process (specification 7.10.2.9).
     * <p>
     * 복합 스택 탐색 과정(규격 7.10.2.9).
     *
     * @param i the index of the candidate block / 후보 블록의 인덱스
     * @param w the weight / 가중치
     */
    private void compoundSearchStack(int i, int w) {
        int[] cand = new int[4];
        // 후보 블록의 두 벡터 성분 네 개를 복사한다.
        for (int k = 0; k < 4; k++) {
            cand[k] = f.mvs[i * 4 + k];
        }
        int candMode = f.yMode[i];
        if (candMode == GLOBAL_GLOBALMV) {
            // 두 목록 각각에서 워프 전역 움직임이면 전역 벡터로 바꾼다.
            for (int refList = 0; refList < 2; refList++) {
                int ref = refList == 0 ? ref0 : ref1;
                if (hdr.gmType[ref] > TRANSLATION) {
                    cand[refList * 2] = globalMv[refList * 2];
                    cand[refList * 2 + 1] = globalMv[refList * 2 + 1];
                }
            }
        }
        // 네 성분 모두 정밀도를 낮춘다.
        for (int k = 0; k < 4; k++) {
            cand[k] = lowerPrecision(cand[k]);
        }
        foundMatch = true;
        int idx;
        // 스택에서 같은 벡터 쌍을 찾는다.
        for (idx = 0; idx < numMvFound; idx++) {
            if (cand[0] == mv[idx * 4] && cand[1] == mv[idx * 4 + 1] && cand[2] == mv[idx * 4 + 2] && cand[3] == mv[idx * 4 + 3]) {
                break;
            }
        }
        if (idx < numMvFound) {
            weight[idx] += w;
        } else if (numMvFound < MAX_REF_MV_STACK_SIZE) {
            // 벡터 쌍의 성분 네 개를 스택에 복사한다.
            for (int k = 0; k < 4; k++) {
                mv[numMvFound * 4 + k] = cand[k];
            }
            weight[numMvFound] = w;
            numMvFound++;
        }
        if (hasNewMv(candMode)) {
            newMvCount++;
        }
    }

    /**
     * True if the mode codes a new motion vector.
     * <p>
     * 모드가 새 움직임 벡터를 부호화하면 true.
     *
     * @param mode the mode / 모드
     * @return true if the mode has a new vector / 새 벡터가 있으면 true
     */
    static boolean hasNewMv(int mode) {
        return mode == NEWMV || mode == NEW_NEWMV || mode == NEAR_NEWMV || mode == NEW_NEARMV || mode == NEAREST_NEWMV || mode == NEW_NEARESTMV;
    }

    /**
     * The sorting process (specification 7.10.2.11): a stable bubble sort of a part of the stack by weight.
     * <p>
     * 정렬 과정(규격 7.10.2.11): 스택의 일부를 가중치 순으로 하는 안정적인 거품 정렬.
     *
     * @param start the first position / 첫 위치
     * @param end   the position after the last / 마지막 다음 위치
     */
    private void sort(int start, int end) {
        // 더 이상 바뀌는 항목이 없을 때까지 인접한 항목을 비교하여 교환한다.
        while (end > start) {
            int newEnd = start;
            // 인접한 두 항목의 가중치를 비교한다.
            for (int idx = start + 1; idx < end; idx++) {
                if (weight[idx - 1] < weight[idx]) {
                    int t = weight[idx - 1];
                    weight[idx - 1] = weight[idx];
                    weight[idx] = t;
                    // 두 항목의 벡터 성분을 서로 바꾼다.
                    for (int k = 0; k < 4; k++) {
                        int v = mv[(idx - 1) * 4 + k];
                        mv[(idx - 1) * 4 + k] = mv[idx * 4 + k];
                        mv[idx * 4 + k] = v;
                    }
                    newEnd = idx;
                }
            }
            end = newEnd;
        }
    }

    /**
     * The extra search process (specification 7.10.2.12).
     * <p>
     * 추가 탐색 과정(규격 7.10.2.12).
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void extraSearch(boolean isCompound) {
        refIdCount[0] = 0;
        refIdCount[1] = 0;
        refDiffCount[0] = 0;
        refDiffCount[1] = 0;
        int w4 = Math.min(16, AvifTables.NUM_4X4_BLOCKS_WIDE[miSize]);
        int h4 = Math.min(16, AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]);
        w4 = Math.min(w4, f.miCols - miCol);
        h4 = Math.min(h4, f.miRows - miRow);
        int num4x4 = Math.min(w4, h4);
        // 먼저 위쪽 행, 다음에 왼쪽 열을 훑는다.
        for (int pass = 0; pass < 2; pass++) {
            int idx = 0;
            // 후보가 두 개가 될 때까지 이웃 블록을 하나씩 확인한다.
            while (idx < num4x4 && numMvFound < 2) {
                int mvRow;
                int mvCol;
                if (pass == 0) {
                    mvRow = miRow - 1;
                    mvCol = miCol + idx;
                } else {
                    mvRow = miRow + idx;
                    mvCol = miCol - 1;
                }
                if (!isInside(mvRow, mvCol)) {
                    break;
                }
                addExtraMvCandidate(mvRow, mvCol, isCompound);
                int size = f.miSize[mvRow * f.miStride + mvCol];
                idx += pass == 0 ? AvifTables.NUM_4X4_BLOCKS_WIDE[size] : AvifTables.NUM_4X4_BLOCKS_HIGH[size];
            }
        }
        if (isCompound) {
            int[] combined = new int[8];
            // 두 목록 각각에 같은 프레임 후보, 다른 프레임 후보, 전역 벡터를 순서대로 채운다.
            for (int list = 0; list < 2; list++) {
                int compCount = 0;
                // 같은 참조 프레임의 부분 일치 후보를 먼저 넣는다.
                for (int idx = 0; idx < refIdCount[list]; idx++) {
                    combined[(compCount * 2 + list) * 2] = refIdMvs[(list * 2 + idx) * 2];
                    combined[(compCount * 2 + list) * 2 + 1] = refIdMvs[(list * 2 + idx) * 2 + 1];
                    compCount++;
                }
                // 다른 참조 프레임의 부분 일치 후보를 두 개까지 넣는다.
                for (int idx = 0; idx < refDiffCount[list] && compCount < 2; idx++) {
                    combined[(compCount * 2 + list) * 2] = refDiffMvs[(list * 2 + idx) * 2];
                    combined[(compCount * 2 + list) * 2 + 1] = refDiffMvs[(list * 2 + idx) * 2 + 1];
                    compCount++;
                }
                // 모자라는 항목은 전역 벡터로 채운다.
                while (compCount < 2) {
                    combined[(compCount * 2 + list) * 2] = globalMv[list * 2];
                    combined[(compCount * 2 + list) * 2 + 1] = globalMv[list * 2 + 1];
                    compCount++;
                }
            }
            if (numMvFound == 1) {
                int src;
                if (combined[0] == mv[0] && combined[1] == mv[1] && combined[2] == mv[2] && combined[3] == mv[3]) {
                    src = 4;
                } else {
                    src = 0;
                }
                // 선택한 후보 쌍의 네 성분을 스택에 복사한다.
                for (int k = 0; k < 4; k++) {
                    mv[numMvFound * 4 + k] = combined[src + k];
                }
                weight[numMvFound] = 2;
                numMvFound++;
            } else {
                // 후보가 없으면 두 항목을 모두 채운다.
                for (int idx = 0; idx < 2; idx++) {
                    for (int k = 0; k < 4; k++) {
                        mv[numMvFound * 4 + k] = combined[idx * 4 + k];
                    }
                    weight[numMvFound] = 2;
                    numMvFound++;
                }
            }
        } else {
            // 단일 예측은 후보 수를 늘리지 않고 전역 벡터로 두 항목을 채운다.
            for (int idx = numMvFound; idx < 2; idx++) {
                mv[idx * 4] = globalMv[0];
                mv[idx * 4 + 1] = globalMv[1];
            }
        }
    }

    /**
     * The add extra MV candidate process (specification 7.10.2.13).
     * <p>
     * 추가 움직임 벡터 후보 과정(규격 7.10.2.13).
     *
     * @param mvRow      the row of the candidate / 후보의 행
     * @param mvCol      the column of the candidate / 후보의 열
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void addExtraMvCandidate(int mvRow, int mvCol, boolean isCompound) {
        int i = mvRow * f.miStride + mvCol;
        // 후보 블록의 두 목록을 확인한다.
        for (int candList = 0; candList < 2; candList++) {
            int candRef = candList == 0 ? f.refFrame0[i] : f.refFrame1[i];
            if (candRef <= 0) {
                continue;
            }
            int cr = f.mvs[i * 4 + candList * 2];
            int cc = f.mvs[i * 4 + candList * 2 + 1];
            if (isCompound) {
                // 현재 블록의 두 참조 프레임마다 같은 프레임인지 다른 프레임인지에 따라 분류한다.
                for (int list = 0; list < 2; list++) {
                    int curRef = list == 0 ? ref0 : ref1;
                    if (candRef == curRef && refIdCount[list] < 2) {
                        refIdMvs[(list * 2 + refIdCount[list]) * 2] = cr;
                        refIdMvs[(list * 2 + refIdCount[list]) * 2 + 1] = cc;
                        refIdCount[list]++;
                    } else if (refDiffCount[list] < 2) {
                        int dr = cr;
                        int dc = cc;
                        if (hdr.refFrameSignBias[candRef] != hdr.refFrameSignBias[curRef]) {
                            dr = -dr;
                            dc = -dc;
                        }
                        refDiffMvs[(list * 2 + refDiffCount[list]) * 2] = dr;
                        refDiffMvs[(list * 2 + refDiffCount[list]) * 2 + 1] = dc;
                        refDiffCount[list]++;
                    }
                }
            } else {
                int dr = cr;
                int dc = cc;
                if (hdr.refFrameSignBias[candRef] != hdr.refFrameSignBias[ref0]) {
                    dr = -dr;
                    dc = -dc;
                }
                int idx;
                // 스택에 같은 벡터가 이미 있는지 찾는다.
                for (idx = 0; idx < numMvFound; idx++) {
                    if (dr == mv[idx * 4] && dc == mv[idx * 4 + 1]) {
                        break;
                    }
                }
                if (idx == numMvFound) {
                    mv[idx * 4] = dr;
                    mv[idx * 4 + 1] = dc;
                    weight[idx] = 2;
                    numMvFound++;
                }
            }
        }
    }

    /**
     * The context and clamping process (specification 7.10.2.14).
     * <p>
     * 문맥과 한계 설정 과정(규격 7.10.2.14).
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     * @param numNew     the number of new vector candidates among the close neighbours / 가까운 이웃의 새 벡터 후보 수
     */
    private void contextAndClamping(boolean isCompound, int numNew) {
        int bw = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4;
        int bh = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4;
        // 후보마다 다음 후보 가중치와의 관계로 문맥을 정한다.
        for (int idx = 0; idx < numMvFound; idx++) {
            int z = 0;
            if (idx + 1 < numMvFound) {
                int w0 = weight[idx];
                int w1 = weight[idx + 1];
                if (w0 >= REF_CAT_LEVEL) {
                    if (w1 < REF_CAT_LEVEL) {
                        z = 1;
                    }
                } else {
                    z = 2;
                }
            }
            drlCtxStack[idx] = z;
        }
        int numLists = isCompound ? 2 : 1;
        // 후보 벡터를 프레임 가장자리 한계 안으로 자른다.
        for (int list = 0; list < numLists; list++) {
            for (int idx = 0; idx < numMvFound; idx++) {
                int o = (idx * 2 + list) * 2;
                mv[o] = clampMvRow(mv[o], MV_BORDER + bh * 8);
                mv[o + 1] = clampMvCol(mv[o + 1], MV_BORDER + bw * 8);
            }
        }
        if (closeMatches == 0) {
            newMvContext = Math.min(totalMatches, 1);
            refMvContext = totalMatches;
        } else if (closeMatches == 1) {
            newMvContext = 3 - Math.min(numNew, 1);
            refMvContext = 2 + totalMatches;
        } else {
            newMvContext = 5 - Math.min(numNew, 1);
            refMvContext = 5;
        }
    }

    /**
     * The clamp MV row function of the specification.
     * <p>
     * 규격의 clamp_mv_row 함수.
     *
     * @param v      the vector component / 벡터 성분
     * @param border the border / 여유
     * @return the clamped component / 자른 성분
     */
    private int clampMvRow(int v, int border) {
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        int top = -((miRow * 4) * 8);
        int bottom = ((f.miRows - bh4 - miRow) * 4) * 8;
        return Math.max(top - border, Math.min(bottom + border, v));
    }

    /**
     * The clamp MV col function of the specification.
     * <p>
     * 규격의 clamp_mv_col 함수.
     *
     * @param v      the vector component / 벡터 성분
     * @param border the border / 여유
     * @return the clamped component / 자른 성분
     */
    private int clampMvCol(int v, int border) {
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int left = -((miCol * 4) * 8);
        int right = ((f.miCols - bw4 - miCol) * 4) * 8;
        return Math.max(left - border, Math.min(right + border, v));
    }
}
