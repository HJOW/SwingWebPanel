package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The state of the frame that is being decoded (plan item P18-03): the sample planes, the per 4x4 block (mode info unit) arrays that the
 * tiles fill and the neighbours read, and the loop restoration parameters. It only holds data; the tile decoder fills it and the post
 * filters read it. Sample planes keep the samples of every bit depth up to 12 in <code>short</code> values (always non-negative).
 * <p>
 * 디코딩 중인 프레임의 상태(계획 P18-03): 샘플 평면, 타일이 채우고 이웃이 읽는 4x4 블록(모드 정보 단위)별 배열, 루프 복원 매개변수. 데이터만 담으며
 * 타일 디코더가 채우고 후처리 필터가 읽는다. 샘플 평면은 12비트까지의 모든 비트 깊이의 샘플을 <code>short</code> 값(항상 0 이상)으로 둔다.
 */
final class Av1Frame {

    /**
     * The sequence header.
     * <p>
     * 시퀀스 헤더.
     */
    final Av1Sequence seq;

    /**
     * The frame header.
     * <p>
     * 프레임 헤더.
     */
    final Av1FrameHeader hdr;

    /**
     * The number of 4x4 columns of the frame.
     * <p>
     * 프레임의 4x4 열 수.
     */
    final int miCols;

    /**
     * The number of 4x4 rows of the frame.
     * <p>
     * 프레임의 4x4 행 수.
     */
    final int miRows;

    /**
     * The row stride of the per 4x4 block arrays (at least the aligned number of columns).
     * <p>
     * 4x4 블록별 배열의 행 간격(정렬된 열 수 이상).
     */
    final int miStride;

    /**
     * The number of allocated rows of the per 4x4 block arrays.
     * <p>
     * 4x4 블록별 배열에 할당한 행 수.
     */
    final int miAllocRows;

    /**
     * The number of planes (1 or 3).
     * <p>
     * 평면 수(1 또는 3).
     */
    final int numPlanes;

    /**
     * The sample planes.
     * <p>
     * 샘플 평면.
     */
    final short[][] planes;

    /**
     * The row stride of each plane.
     * <p>
     * 각 평면의 행 간격.
     */
    final int[] stride;

    /**
     * The visible width of each plane.
     * <p>
     * 각 평면의 보이는 너비.
     */
    final int[] planeWidth;

    /**
     * The visible height of each plane.
     * <p>
     * 각 평면의 보이는 높이.
     */
    final int[] planeHeight;

    /**
     * The luma prediction mode of each 4x4 block.
     * <p>
     * 각 4x4 블록의 휘도 예측 모드.
     */
    final byte[] yMode;

    /**
     * The chroma prediction mode of each 4x4 block.
     * <p>
     * 각 4x4 블록의 색차 예측 모드.
     */
    final byte[] uvMode;

    /**
     * The block size of each 4x4 block.
     * <p>
     * 각 4x4 블록의 블록 크기.
     */
    final byte[] miSize;

    /**
     * The skip flag of each 4x4 block.
     * <p>
     * 각 4x4 블록의 skip 플래그.
     */
    final byte[] skip;

    /**
     * The transform size that the block signals (<code>InterTxSizes</code>) of each 4x4 block.
     * <p>
     * 각 4x4 블록에서 블록이 알린 변환 크기(<code>InterTxSizes</code>).
     */
    final byte[] interTxSize;

    /**
     * The segment id of each 4x4 block.
     * <p>
     * 각 4x4 블록의 세그먼트 id.
     */
    final byte[] segmentId;

    /**
     * The luma palette size of each 4x4 block.
     * <p>
     * 각 4x4 블록의 휘도 팔레트 크기.
     */
    final byte[] paletteSizeY;

    /**
     * The chroma palette size of each 4x4 block.
     * <p>
     * 각 4x4 블록의 색차 팔레트 크기.
     */
    final byte[] paletteSizeUv;

    /**
     * The luma palette colours of each 4x4 block (the array of the block that covers it, null without palette).
     * <p>
     * 각 4x4 블록의 휘도 팔레트 색(그 블록을 덮는 블록의 배열. 팔레트가 없으면 null).
     */
    final short[][] paletteColorsY;

    /**
     * The first chroma palette colours of each 4x4 block.
     * <p>
     * 각 4x4 블록의 첫째 색차 팔레트 색.
     */
    final short[][] paletteColorsU;

    /**
     * 1 if the block of the 4x4 unit is an inter block (including intra block copy), else 0.
     * <p>
     * 4x4 단위 블록이 인터 블록(인트라 블록 복사 포함)이면 1, 아니면 0.
     */
    final byte[] isInter;

    /**
     * The first reference frame of the block of each 4x4 unit: 0 intra frame, 1..7 LAST..ALTREF, and {@link #NOT_WRITTEN} before the block
     * was decoded.
     * <p>
     * 4x4 단위 블록의 첫 참조 프레임: 0 인트라 프레임, 1..7 LAST..ALTREF, 블록을 디코딩하기 전에는 {@link #NOT_WRITTEN}.
     */
    final byte[] refFrame0;

    /**
     * The second reference frame of the block of each 4x4 unit: -1 none, 0 inter-intra, 1..7 a reference frame.
     * <p>
     * 4x4 단위 블록의 두 번째 참조 프레임: -1 없음, 0 인터-인트라, 1..7 참조 프레임.
     */
    final byte[] refFrame1;

    /**
     * The motion vectors of each 4x4 unit: four values per unit (row and column of the first, then of the second vector), in 1/8 sample
     * units.
     * <p>
     * 4x4 단위마다 움직임 벡터: 단위당 4개 값(첫째 벡터의 행과 열, 둘째 벡터의 행과 열), 1/8 샘플 단위.
     */
    final short[] mvs;

    /**
     * The interpolation filter of the vertical direction (interp_filter[0]) of the block of each 4x4 unit.
     * <p>
     * 4x4 단위 블록의 세로 방향 보간 필터(interp_filter[0]).
     */
    final byte[] interpFilter0;

    /**
     * The interpolation filter of the horizontal direction (interp_filter[1]) of the block of each 4x4 unit.
     * <p>
     * 4x4 단위 블록의 가로 방향 보간 필터(interp_filter[1]).
     */
    final byte[] interpFilter1;

    /**
     * 1 if the block of each 4x4 unit uses the skip mode (a compound prediction without further information).
     * <p>
     * 4x4 단위 블록이 건너뛰기 모드(추가 정보 없는 복합 예측)를 쓰면 1.
     */
    final byte[] skipMode;

    /**
     * The flag comp_group_idx of the block of each 4x4 unit.
     * <p>
     * 4x4 단위 블록의 comp_group_idx 플래그.
     */
    final byte[] compGroupIdx;

    /**
     * The flag compound_idx of the block of each 4x4 unit.
     * <p>
     * 4x4 단위 블록의 compound_idx 플래그.
     */
    final byte[] compoundIdx;

    /**
     * The reference frames of the seven references (index 1..7; the slots of the reference store at the start of the frame; null for an
     * intra frame).
     * <p>
     * 참조 일곱 개의 참조 프레임(인덱스 1..7. 프레임 시작 때 참조 저장소의 슬롯. 인트라 프레임은 null).
     */
    final Av1RefFrame[] refList = new Av1RefFrame[8];

    /**
     * The segmentation map of the previous frame (one value per 4x4 unit with the row stride {@link #miStride}), or null for all zero.
     * <p>
     * 이전 프레임의 분할 맵(4x4 단위마다 값 하나, 행 간격 {@link #miStride}). 전부 0 이면 null.
     */
    byte[] prevSegmentIds;

    /**
     * The projected motion field per reference frame (index 1..7): per 8x8 luma block the row and the column of the vector, or
     * {@link Av1MvStack#INVALID_MV}; null if the frame does not use projected vectors.
     * <p>
     * 참조 프레임(인덱스 1..7)별 투영된 움직임 필드: 8x8 휘도 블록마다 벡터의 행과 열, 또는 {@link Av1MvStack#INVALID_MV}. 투영된 벡터를 쓰지
     * 않는 프레임이면 null.
     */
    short[][] motionFieldMvs;

    /**
     * The value of {@link #refFrame0} of a unit whose block has not been decoded yet.
     * <p>
     * 블록을 아직 디코딩하지 않은 단위의 {@link #refFrame0} 값.
     */
    static final byte NOT_WRITTEN = -2;

    /**
     * The transform type of each luma 4x4 block.
     * <p>
     * 각 휘도 4x4 블록의 변환 종류.
     */
    final byte[] txType;

    /**
     * The transform size used for the loop filter of every plane, in units of 4x4 samples of the plane.
     * <p>
     * 모든 평면의 루프 필터에 쓰는 변환 크기. 평면의 4x4 샘플 단위.
     */
    final byte[][] lfTxSize;

    /**
     * The loop filter level deltas of each 4x4 block (four values per block) or null if they are not signalled.
     * <p>
     * 각 4x4 블록의 루프 필터 수준 변화량(블록당 네 값). 알리지 않으면 null.
     */
    final byte[] deltaLf;

    /**
     * The CDEF index of each 64x64 block (-1: not coded).
     * <p>
     * 각 64x64 블록의 CDEF 색인(-1: 부호화되지 않음).
     */
    final int[] cdefIdx;

    /**
     * The row stride of {@link #cdefIdx}.
     * <p>
     * {@link #cdefIdx} 의 행 간격.
     */
    final int cdefStride;

    /**
     * The number of restoration unit rows of each plane.
     * <p>
     * 각 평면의 복원 단위 행 수.
     */
    final int[] lrUnitRows = new int[3];

    /**
     * The number of restoration unit columns of each plane.
     * <p>
     * 각 평면의 복원 단위 열 수.
     */
    final int[] lrUnitCols = new int[3];

    /**
     * The restoration type of each unit of every plane.
     * <p>
     * 모든 평면의 각 복원 단위의 복원 종류.
     */
    final byte[][] lrType = new byte[3][];

    /**
     * The Wiener coefficients of each unit (two passes of three coefficients).
     * <p>
     * 각 단위의 위너 계수(세 계수씩 두 패스).
     */
    final int[][] lrWiener = new int[3][];

    /**
     * The self guided parameter set of each unit.
     * <p>
     * 각 단위의 자기 유도 매개변수 집합.
     */
    final byte[][] lrSgrSet = new byte[3][];

    /**
     * The self guided projection coefficients of each unit (two values).
     * <p>
     * 각 단위의 자기 유도 투영 계수(두 값).
     */
    final int[][] lrSgrXqd = new int[3][];

    /**
     * Allocates the frame state.
     * <p>
     * 프레임 상태를 할당한다.
     *
     * @param seq the sequence header / 시퀀스 헤더
     * @param hdr the frame header / 프레임 헤더
     */
    Av1Frame(Av1Sequence seq, Av1FrameHeader hdr) {
        this.seq = seq;
        this.hdr = hdr;
        this.miCols = hdr.miCols;
        this.miRows = hdr.miRows;
        this.numPlanes = seq.numPlanes;
        this.miStride = ((miCols + 31) & ~31) + 32;
        this.miAllocRows = ((miRows + 31) & ~31) + 32;
        int total = miStride * miAllocRows;
        yMode = new byte[total];
        uvMode = new byte[total];
        miSize = new byte[total];
        skip = new byte[total];
        interTxSize = new byte[total];
        isInter = new byte[total];
        refFrame0 = new byte[total];
        java.util.Arrays.fill(refFrame0, NOT_WRITTEN);
        refFrame1 = new byte[total];
        mvs = new short[total * 4];
        skipMode = new byte[total];
        compGroupIdx = new byte[total];
        compoundIdx = new byte[total];
        interpFilter0 = new byte[total];
        interpFilter1 = new byte[total];
        segmentId = new byte[total];
        paletteSizeY = new byte[total];
        paletteSizeUv = new byte[total];
        paletteColorsY = new short[total][];
        paletteColorsU = new short[total][];
        txType = new byte[total];
        lfTxSize = new byte[3][];
        deltaLf = hdr.deltaLfPresent ? new byte[total * 4] : null;
        int alignedW = (miCols * 4 + 127) & ~127;
        int alignedH = (miRows * 4 + 127) & ~127;
        planes = new short[3][];
        stride = new int[3];
        planeWidth = new int[3];
        planeHeight = new int[3];
        // 평면마다 하위 표본화를 반영해 크기를 정하고 정렬된 버퍼를 할당한다.
        for (int p = 0; p < numPlanes; p++) {
            int sx = p == 0 ? 0 : seq.subsamplingX;
            int sy = p == 0 ? 0 : seq.subsamplingY;
            stride[p] = alignedW >> sx;
            planeWidth[p] = (hdr.upscaledWidth + sx) >> sx;
            planeHeight[p] = (hdr.frameHeight + sy) >> sy;
            planes[p] = new short[stride[p] * (alignedH >> sy)];
            lfTxSize[p] = new byte[total];
        }
        cdefStride = ((miCols + 15) >> 4) + 2;
        cdefIdx = new int[cdefStride * (((miRows + 15) >> 4) + 2)];
        java.util.Arrays.fill(cdefIdx, -1);
    }

    /**
     * Allocates the loop restoration unit parameters of the planes that use loop restoration.
     * <p>
     * 루프 복원을 쓰는 평면의 복원 단위 매개변수를 할당한다.
     */
    void allocateRestoration() {
        // 평면마다 복원 단위 수를 세어 배열을 할당한다.
        for (int p = 0; p < numPlanes; p++) {
            if (hdr.frameRestorationType[p] == Av1FrameHeader.RESTORE_NONE) {
                continue;
            }
            int sx = p == 0 ? 0 : seq.subsamplingX;
            int sy = p == 0 ? 0 : seq.subsamplingY;
            int unitSize = hdr.loopRestorationSize[p];
            lrUnitRows[p] = countUnits(unitSize, (hdr.frameHeight + sy) >> sy);
            lrUnitCols[p] = countUnits(unitSize, (hdr.upscaledWidth + sx) >> sx);
            int n = lrUnitRows[p] * lrUnitCols[p];
            lrType[p] = new byte[n];
            lrWiener[p] = new int[n * 6];
            lrSgrSet[p] = new byte[n];
            lrSgrXqd[p] = new int[n * 2];
        }
    }

    /**
     * The number of loop restoration units that cover a frame dimension (<code>count_units_in_frame</code>).
     * <p>
     * 프레임 한 방향을 덮는 루프 복원 단위의 수(<code>count_units_in_frame</code>).
     *
     * @param unitSize  the unit size / 단위 크기
     * @param frameSize the frame size in samples / 프레임 크기(샘플)
     * @return the number of units / 단위 수
     */
    static int countUnits(int unitSize, int frameSize) {
        return Math.max((frameSize + (unitSize >> 1)) / unitSize, 1);
    }
}
