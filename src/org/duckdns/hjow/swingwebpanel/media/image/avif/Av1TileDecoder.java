package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Decodes one tile of an AV1 frame (plan item P18-03; AV1 specification sections 5.11 "Tile group OBU syntax", 7.10 "Motion vector prediction
 * processes", 7.11 "Prediction processes" and 7.12 "Reconstruction and dequantization"): the partition tree, the mode info of every block of
 * intra frames and inter frames, the palette, the transform sizes and coefficients, the intra prediction, intra block copy, the inter prediction
 * (single and compound references, overlapped block motion compensation, local and global warped motion, inter-intra, wedge, difference weighted
 * and distance weighted compound prediction, scaled references) and the reconstruction into the sample planes of the frame.
 * <p>
 * AV1 프레임의 타일 하나를 디코딩한다(계획 P18-03; AV1 규격 5.11 "Tile group OBU syntax", 7.10 "Motion vector prediction processes",
 * 7.11 "Prediction processes", 7.12 "Reconstruction and dequantization"): 분할 트리, 인트라 프레임과 인터 프레임 모든 블록의 모드 정보, 팔레트, 변환
 * 크기와 계수, 인트라 예측, 인트라 블록 복사, 인터 예측(단일/복합 참조, 겹침 블록 움직임 보상, 국소/전역 워프, 인터인트라, 쐐기·차이 가중·거리
 * 가중 복합 예측, 크기가 다른 참조), 프레임 샘플 평면으로의 복원.
 */
final class Av1TileDecoder {

    /**
     * The block size enumeration value of BLOCK_8X8.
     * <p>
     * BLOCK_8X8 의 블록 크기 열거 값.
     */
    private static final int BLOCK_8X8 = 3;

    /**
     * The block size enumeration value of BLOCK_64X64.
     * <p>
     * BLOCK_64X64 의 블록 크기 열거 값.
     */
    private static final int BLOCK_64X64 = 12;

    /**
     * The block size enumeration value of BLOCK_128X128.
     * <p>
     * BLOCK_128X128 의 블록 크기 열거 값.
     */
    private static final int BLOCK_128X128 = 15;

    /**
     * The partition type of a block that is not split.
     * <p>
     * 분할하지 않은 블록의 분할 종류.
     */
    private static final int PARTITION_NONE = 0;

    /**
     * The horizontal partition.
     * <p>
     * 가로 분할.
     */
    private static final int PARTITION_HORZ = 1;

    /**
     * The vertical partition.
     * <p>
     * 세로 분할.
     */
    private static final int PARTITION_VERT = 2;

    /**
     * The split into four quarters.
     * <p>
     * 네 조각 분할.
     */
    private static final int PARTITION_SPLIT = 3;

    /**
     * The horizontal partition with two split blocks on the top.
     * <p>
     * 위쪽이 두 조각인 가로 분할.
     */
    private static final int PARTITION_HORZ_A = 4;

    /**
     * The horizontal partition with two split blocks at the bottom.
     * <p>
     * 아래쪽이 두 조각인 가로 분할.
     */
    private static final int PARTITION_HORZ_B = 5;

    /**
     * The vertical partition with two split blocks on the left.
     * <p>
     * 왼쪽이 두 조각인 세로 분할.
     */
    private static final int PARTITION_VERT_A = 6;

    /**
     * The vertical partition with two split blocks on the right.
     * <p>
     * 오른쪽이 두 조각인 세로 분할.
     */
    private static final int PARTITION_VERT_B = 7;

    /**
     * The partition into four horizontal stripes.
     * <p>
     * 가로 줄 네 개로의 분할.
     */
    private static final int PARTITION_HORZ_4 = 8;

    /**
     * The index of the chroma from luma prediction mode.
     * <p>
     * 휘도로부터의 색차 예측 모드의 번호.
     */
    private static final int UV_CFL_PRED = 13;

    /**
     * The row stride of the block decoded flags (34 entries are needed, 35 are allocated).
     * <p>
     * 블록 디코딩 완료 플래그의 행 간격(34개가 필요하며 35개를 할당한다).
     */
    private static final int BD_STRIDE = 35;

    /**
     * The mapping of a palette color context hash to the context of the colour index (negative entries are never used).
     * <p>
     * 팔레트 색 문맥 해시에서 색 색인 문맥으로의 대응(음수는 쓰지 않는다).
     */
    private static final int[] PALETTE_COLOR_CONTEXT = { -1, -1, 0, -1, -1, 4, 3, 2, 1 };

    /**
     * The transform size after one split (<code>Split_Tx_Size</code>).
     * <p>
     * 한 번 분할한 뒤의 변환 크기(<code>Split_Tx_Size</code>).
     */
    private static final int[] SPLIT_TX_SIZE = { 0, 0, 1, 2, 3, 0, 0, 1, 1, 2, 2, 3, 3, 5, 6, 7, 8, 9, 10 };

    /**
     * The frame that is decoded.
     * <p>
     * 디코딩하는 프레임.
     */
    final Av1Frame frame;

    /**
     * The frame header.
     * <p>
     * 프레임 헤더.
     */
    final Av1FrameHeader hdr;

    /**
     * The sequence header.
     * <p>
     * 시퀀스 헤더.
     */
    final Av1Sequence seq;

    /**
     * The distributions of this tile (a private copy that is adapted while decoding).
     * <p>
     * 이 타일의 분포(디코딩하며 적응시키는 개별 복사본).
     */
    final Av1Cdfs cdfs;

    /**
     * The symbol decoder of this tile.
     * <p>
     * 이 타일의 심볼 디코더.
     */
    final Av1SymbolDecoder sd;

    /**
     * The coefficient reader of this tile.
     * <p>
     * 이 타일의 계수 읽기 객체.
     */
    private final Av1CoeffReader coeff;

    /**
     * The first 4x4 row of the tile.
     * <p>
     * 타일의 첫 4x4 행.
     */
    private final int miRowStart;

    /**
     * The 4x4 row after the last one of the tile.
     * <p>
     * 타일의 마지막 4x4 행 다음.
     */
    private final int miRowEnd;

    /**
     * The first 4x4 column of the tile.
     * <p>
     * 타일의 첫 4x4 열.
     */
    private final int miColStart;

    /**
     * The 4x4 column after the last one of the tile.
     * <p>
     * 타일의 마지막 4x4 열 다음.
     */
    private final int miColEnd;

    /**
     * The current quantizer index (changes with the delta quantizer syntax).
     * <p>
     * 현재 양자화 지수(델타 양자화 구문에 따라 변한다).
     */
    private int currentQIndex;

    /**
     * The current loop filter deltas.
     * <p>
     * 현재 루프 필터 변화량.
     */
    private final int[] deltaLfState = new int[4];

    /**
     * True while the deltas of the superblock may still be read.
     * <p>
     * 슈퍼블록의 변화량을 아직 읽을 수 있는 동안 true.
     */
    private boolean readDeltas;

    /**
     * The reference Wiener coefficients of each plane and pass.
     * <p>
     * 평면과 패스마다의 기준 위너 계수.
     */
    private final int[][][] refLrWiener = new int[3][2][3];

    /**
     * The reference self guided coefficients of each plane.
     * <p>
     * 평면마다의 기준 자기 유도 계수.
     */
    private final int[][] refSgrXqd = new int[3][2];

    /**
     * The block decoded flags of each plane around the current superblock.
     * <p>
     * 현재 슈퍼블록 주변의 평면별 블록 디코딩 완료 플래그.
     */
    private final boolean[][] blockDecoded = new boolean[3][BD_STRIDE * BD_STRIDE];

    /**
     * The row of the current block in 4x4 units.
     * <p>
     * 현재 블록의 4x4 단위 행.
     */
    int miRow;

    /**
     * The column of the current block in 4x4 units.
     * <p>
     * 현재 블록의 4x4 단위 열.
     */
    int miCol;

    /**
     * The size of the current block.
     * <p>
     * 현재 블록의 크기.
     */
    int miSize;

    /**
     * True if the current block has chroma samples.
     * <p>
     * 현재 블록이 색차 샘플을 가지면 true.
     */
    private boolean hasChroma;

    /**
     * True if the block above is inside the tile.
     * <p>
     * 위쪽 블록이 타일 안에 있으면 true.
     */
    private boolean availU;

    /**
     * True if the block to the left is inside the tile.
     * <p>
     * 왼쪽 블록이 타일 안에 있으면 true.
     */
    private boolean availL;

    /**
     * True if the chroma block above is available.
     * <p>
     * 위쪽 색차 블록을 쓸 수 있으면 true.
     */
    private boolean availUChroma;

    /**
     * True if the chroma block to the left is available.
     * <p>
     * 왼쪽 색차 블록을 쓸 수 있으면 true.
     */
    private boolean availLChroma;

    /**
     * True if the current block is skipped (no residual).
     * <p>
     * 현재 블록이 skip(잔차 없음)이면 true.
     */
    private boolean skip;

    /**
     * The segment id of the current block.
     * <p>
     * 현재 블록의 세그먼트 id.
     */
    int segmentId;

    /**
     * True if the current block is coded losslessly.
     * <p>
     * 현재 블록이 무손실로 부호화되었으면 true.
     */
    boolean lossless;

    /**
     * The luma mode of the current block.
     * <p>
     * 현재 블록의 휘도 모드.
     */
    int yMode;

    /**
     * The chroma mode of the current block.
     * <p>
     * 현재 블록의 색차 모드.
     */
    int uvMode;

    /**
     * The angle delta of the luma prediction.
     * <p>
     * 휘도 예측의 각도 변화.
     */
    private int angleDeltaY;

    /**
     * The angle delta of the chroma prediction.
     * <p>
     * 색차 예측의 각도 변화.
     */
    private int angleDeltaUv;

    /**
     * True if the recursive filter intra predictor is used for luma.
     * <p>
     * 휘도에 재귀 필터 인트라 예측기를 쓰면 true.
     */
    boolean useFilterIntra;

    /**
     * The filter intra mode.
     * <p>
     * 필터 인트라 모드.
     */
    int filterIntraMode;

    /**
     * The alpha of the CfL prediction of the U plane.
     * <p>
     * U 평면 CfL 예측의 알파.
     */
    private int cflAlphaU;

    /**
     * The alpha of the CfL prediction of the V plane.
     * <p>
     * V 평면 CfL 예측의 알파.
     */
    private int cflAlphaV;

    /**
     * The luma palette size of the current block.
     * <p>
     * 현재 블록의 휘도 팔레트 크기.
     */
    private int paletteSizeY;

    /**
     * The chroma palette size of the current block.
     * <p>
     * 현재 블록의 색차 팔레트 크기.
     */
    private int paletteSizeUv;

    /**
     * The luma palette colours of the current block.
     * <p>
     * 현재 블록의 휘도 팔레트 색.
     */
    private short[] paletteColorsY;

    /**
     * The U palette colours of the current block.
     * <p>
     * 현재 블록의 U 팔레트 색.
     */
    private short[] paletteColorsU;

    /**
     * The V palette colours of the current block.
     * <p>
     * 현재 블록의 V 팔레트 색.
     */
    private short[] paletteColorsV;

    /**
     * The luma colour index map of the current block (row stride 64).
     * <p>
     * 현재 블록의 휘도 색 색인 지도(행 간격 64).
     */
    private final byte[] colorMapY = new byte[64 * 64];

    /**
     * The chroma colour index map of the current block (row stride 64).
     * <p>
     * 현재 블록의 색차 색 색인 지도(행 간격 64).
     */
    private final byte[] colorMapUv = new byte[64 * 64];

    /**
     * The transform size of the current block.
     * <p>
     * 현재 블록의 변환 크기.
     */
    private int txSize;

    /**
     * The right end of the reconstructed luma of the block (for the CfL prediction).
     * <p>
     * 블록에서 복원된 휘도의 오른쪽 끝(CfL 예측용).
     */
    private int maxLumaW;

    /**
     * The bottom end of the reconstructed luma of the block.
     * <p>
     * 블록에서 복원된 휘도의 아래쪽 끝.
     */
    private int maxLumaH;

    /**
     * The residual of a transform block.
     * <p>
     * 변환 블록의 잔차.
     */
    private final int[] residual = new int[64 * 64];

    /**
     * The dequantized coefficients (row i and column j at <code>i * 64 + j</code>), all zero between uses.
     * <p>
     * 역양자화된 계수(행 i, 열 j 는 <code>i * 64 + j</code>). 쓰지 않을 때는 모두 0.
     */
    private final int[] dequant = new int[64 * 64];

    /**
     * True if the current block is an inter block (an intra block copy block in an intra frame).
     * <p>
     * 현재 블록이 인터 블록이면 true(인트라 프레임에서는 인트라 블록 복사 블록).
     */
    boolean isInter;

    /**
     * True if the current block copies a part of the current frame (intra block copy).
     * <p>
     * 현재 블록이 현재 프레임의 일부를 복사하면 true(인트라 블록 복사).
     */
    boolean useIntrabc;

    /**
     * The first reference frame of the current block (0 for an intra block and for intra block copy).
     * <p>
     * 현재 블록의 첫 참조 프레임(인트라 블록과 인트라 블록 복사는 0).
     */
    private int refFrame0;

    /**
     * The second reference frame of the current block (-1 if there is none).
     * <p>
     * 현재 블록의 둘째 참조 프레임(없으면 -1).
     */
    private int refFrame1 = -1;

    /**
     * The motion vectors of the current block: list * 2 + component, in 1/8 samples.
     * <p>
     * 현재 블록의 움직임 벡터: 목록 * 2 + 성분, 1/8 샘플 단위.
     */
    private final int[] curMv = new int[4];

    /**
     * The predicted motion vectors of the current block: list * 2 + component.
     * <p>
     * 현재 블록의 예측 움직임 벡터: 목록 * 2 + 성분.
     */
    private final int[] predMv = new int[4];

    /**
     * The interpolation filter of the vertical direction of the current block (interp_filter[0]).
     * <p>
     * 현재 블록의 세로 방향 보간 필터(interp_filter[0]).
     */
    private int interpFilter0;

    /**
     * The interpolation filter of the horizontal direction of the current block (interp_filter[1]).
     * <p>
     * 현재 블록의 가로 방향 보간 필터(interp_filter[1]).
     */
    private int interpFilter1;

    /**
     * The predictor of the motion vectors of this tile.
     * <p>
     * 이 타일의 움직임 벡터 예측기.
     */
    private final Av1MvStack mvStack;

    /**
     * The interpolator of the motion compensation.
     * <p>
     * 움직임 보상의 보간기.
     */
    private final Av1InterPred interPred = new Av1InterPred();

    /**
     * The scaled position of a motion compensated block.
     * <p>
     * 움직임 보상 블록의 스케일된 위치.
     */
    private final Av1InterPred.Position interPos = new Av1InterPred.Position();

    /**
     * The predicted samples of a block (before the rounding to the sample range).
     * <p>
     * 블록의 예측 샘플(샘플 범위로 반올림하기 전).
     */
    private final int[] predBuf = new int[128 * 128];

    /**
     * The stride of the mask array.
     * <p>
     * 마스크 배열의 행 간격.
     */
    private static final int MASK_STRIDE = 128;

    /**
     * Compound type: a wedge mask.
     * <p>
     * 복합 종류: 쐐기 마스크.
     */
    static final int COMPOUND_WEDGE = 0;

    /**
     * Compound type: a mask from the difference of the predictions.
     * <p>
     * 복합 종류: 예측의 차이로 만든 마스크.
     */
    static final int COMPOUND_DIFFWTD = 1;

    /**
     * Compound type: the average of the two predictions.
     * <p>
     * 복합 종류: 두 예측의 평균.
     */
    static final int COMPOUND_AVERAGE = 2;

    /**
     * Compound type: the inter prediction is mixed with an intra prediction.
     * <p>
     * 복합 종류: 인터 예측을 인트라 예측과 섞는다.
     */
    static final int COMPOUND_INTRA = 3;

    /**
     * Compound type: a weighted average with weights from the distance of the frames.
     * <p>
     * 복합 종류: 프레임 거리로 정한 가중치의 가중 평균.
     */
    static final int COMPOUND_DISTANCE = 4;

    /**
     * The two predictions of a compound block (or the only one), one value per sample of the region, row stride = width.
     * <p>
     * 복합 블록의 두 예측(또는 하나뿐인 예측), 영역의 샘플마다 값 하나, 행 간격 = 너비.
     */
    private final int[][] preds = new int[2][128 * 128];

    /**
     * The mask of the blending of two predictions, row stride {@link #MASK_STRIDE}.
     * <p>
     * 두 예측을 섞는 마스크, 행 간격 {@link #MASK_STRIDE}.
     */
    private final int[] maskArr = new int[128 * MASK_STRIDE];

    /**
     * True if the current block uses the skip mode.
     * <p>
     * 현재 블록이 건너뛰기 모드를 쓰면 true.
     */
    private boolean skipMode;

    /**
     * How the two predictions of the block are blended (one of the COMPOUND constants).
     * <p>
     * 블록의 두 예측을 섞는 방법(COMPOUND 상수 중 하나).
     */
    private int compoundType = COMPOUND_AVERAGE;

    /**
     * The flag comp_group_idx of the block.
     * <p>
     * 블록의 comp_group_idx 플래그.
     */
    private int compGroupIdx;

    /**
     * The flag compound_idx of the block.
     * <p>
     * 블록의 compound_idx 플래그.
     */
    private int compoundIdx = 1;

    /**
     * True if the block mixes the inter prediction with an intra prediction.
     * <p>
     * 블록이 인터 예측을 인트라 예측과 섞으면 true.
     */
    private boolean interintra;

    /**
     * The intra mode of the interintra prediction (0 DC, 1 vertical, 2 horizontal, 3 smooth).
     * <p>
     * 인터인트라 예측의 인트라 모드(0 DC, 1 수직, 2 수평, 3 스무스).
     */
    private int interintraMode;

    /**
     * True if the interintra prediction is blended with a wedge mask.
     * <p>
     * 인터인트라 예측을 쐐기 마스크로 섞으면 true.
     */
    private boolean wedgeInterintra;

    /**
     * The wedge index of the block.
     * <p>
     * 블록의 쐐기 번호.
     */
    private int wedgeIndex;

    /**
     * The sign of the wedge mask.
     * <p>
     * 쐐기 마스크의 부호.
     */
    private int wedgeSign;

    /**
     * The type of the difference weighted mask (0 or 1).
     * <p>
     * 차이 가중 마스크의 종류(0 또는 1).
     */
    private int maskType;

    /**
     * The motion mode of the block (0 translation, 1 overlapped, 2 local warp).
     * <p>
     * 블록의 움직임 모드(0 이동, 1 겹침, 2 국소 워프).
     */
    private int motionMode;

    /**
     * The index of the chosen candidate of the motion vector stack.
     * <p>
     * 움직임 벡터 스택에서 고른 후보의 번호.
     */
    private int refMvIdx;

    /**
     * The number of valid samples for the local warp estimation.
     * <p>
     * 국소 워프 추정에 쓸 올바른 표본 수.
     */
    private int numSamples;

    /**
     * The number of samples that were scanned (including the invalid ones).
     * <p>
     * 훑은 표본 수(올바르지 않은 것 포함).
     */
    private int numSamplesScanned;

    /**
     * The samples for the local warp estimation: four values (source and destination position) per sample.
     * <p>
     * 국소 워프 추정용 표본: 표본마다 값 네 개(원본과 목적 위치).
     */
    private final int[] candList = new int[8 * 4];

    /**
     * The parameters of the local warp model.
     * <p>
     * 국소 워프 모델의 매개변수.
     */
    private final int[] localWarpParams = new int[6];

    /**
     * The shear parameters of the local warp model (alpha, beta, gamma, delta).
     * <p>
     * 국소 워프 모델의 전단 매개변수(alpha, beta, gamma, delta).
     */
    private final int[] localShear = new int[4];

    /**
     * The shear parameters of a global warp model.
     * <p>
     * 전역 워프 모델의 전단 매개변수.
     */
    private final int[] globalShear = new int[4];

    /**
     * True if the local warp model of the block can be used.
     * <p>
     * 블록의 국소 워프 모델을 쓸 수 있으면 true.
     */
    private boolean localValid;

    /**
     * The weight of the first prediction of the distance weighted compound prediction.
     * <p>
     * 거리 가중 복합 예측에서 첫째 예측의 가중치.
     */
    private int fwdWeight;

    /**
     * The weight of the second prediction of the distance weighted compound prediction.
     * <p>
     * 거리 가중 복합 예측에서 둘째 예측의 가중치.
     */
    private int bckWeight;

    /**
     * The reference frame of the block to the left (the intra frame if there is none).
     * <p>
     * 왼쪽 블록의 첫 참조 프레임(없으면 인트라 프레임).
     */
    private int leftRef0;

    /**
     * The second reference frame of the block to the left.
     * <p>
     * 왼쪽 블록의 둘째 참조 프레임.
     */
    private int leftRef1;

    /**
     * The first reference frame of the block above.
     * <p>
     * 위쪽 블록의 첫 참조 프레임.
     */
    private int aboveRef0;

    /**
     * The second reference frame of the block above.
     * <p>
     * 위쪽 블록의 둘째 참조 프레임.
     */
    private int aboveRef1;

    /**
     * True if the block to the left is an intra block or missing.
     * <p>
     * 왼쪽 블록이 인트라 블록이거나 없으면 true.
     */
    private boolean leftIntra;

    /**
     * True if the block above is an intra block or missing.
     * <p>
     * 위쪽 블록이 인트라 블록이거나 없으면 true.
     */
    private boolean aboveIntra;

    /**
     * True if the block to the left does not use compound prediction.
     * <p>
     * 왼쪽 블록이 복합 예측을 쓰지 않으면 true.
     */
    private boolean leftSingle;

    /**
     * True if the block above does not use compound prediction.
     * <p>
     * 위쪽 블록이 복합 예측을 쓰지 않으면 true.
     */
    private boolean aboveSingle;

    /**
     * The context of the prediction of the segment id above (one flag per 4x4 column).
     * <p>
     * 위쪽 세그먼트 id 예측의 문맥(4x4 열마다 플래그 하나).
     */
    private byte[] aboveSegPred;

    /**
     * The context of the prediction of the segment id to the left (one flag per 4x4 row).
     * <p>
     * 왼쪽 세그먼트 id 예측의 문맥(4x4 행마다 플래그 하나).
     */
    private byte[] leftSegPred;

    /**
     * Creates the decoder of one tile.
     * <p>
     * 타일 하나의 디코더를 만든다.
     *
     * @param frame      the frame / 프레임
     * @param cdfs       the distributions of this tile / 이 타일의 분포
     * @param data       the data / 데이터
     * @param offset     the first byte of the tile data / 타일 데이터의 첫 바이트
     * @param size       the size of the tile data / 타일 데이터의 크기
     * @param miRowStart the first 4x4 row of the tile / 타일의 첫 4x4 행
     * @param miRowEnd   the 4x4 row after the tile / 타일 다음의 4x4 행
     * @param miColStart the first 4x4 column of the tile / 타일의 첫 4x4 열
     * @param miColEnd   the 4x4 column after the tile / 타일 다음의 4x4 열
     */
    Av1TileDecoder(Av1Frame frame, Av1Cdfs cdfs, byte[] data, int offset, int size, int miRowStart, int miRowEnd, int miColStart,
            int miColEnd) {
        this.frame = frame;
        this.hdr = frame.hdr;
        this.seq = frame.seq;
        this.cdfs = cdfs;
        this.sd = new Av1SymbolDecoder(data, offset, size, hdr.disableCdfUpdate);
        this.miRowStart = miRowStart;
        this.miRowEnd = miRowEnd;
        this.miColStart = miColStart;
        this.miColEnd = miColEnd;
        this.coeff = new Av1CoeffReader(this);
        this.mvStack = new Av1MvStack(frame, miRowStart, miRowEnd, miColStart, miColEnd);
        this.aboveSegPred = new byte[frame.miCols + 32];
        this.leftSegPred = new byte[frame.miRows + 32];
    }

    /**
     * Decodes the whole tile (<code>decode_tile</code>).
     * <p>
     * 타일 전체를 디코딩한다(<code>decode_tile</code>).
     *
     * @throws MediaException CORRUPT for damaged data, CANCELLED if interrupted / 손상된 데이터면 CORRUPT, 인터럽트되면 CANCELLED
     */
    void decode() throws MediaException {
        currentQIndex = hdr.baseQIdx;
        // 평면과 패스마다 기준 복원 계수를 초기값으로 되돌린다.
        for (int plane = 0; plane < seq.numPlanes; plane++) {
            for (int pass = 0; pass < 2; pass++) {
                refSgrXqd[plane][pass] = AvifTables.SGRPROJ_XQD_MID[pass];
                // 세 위너 계수를 초기값으로 되돌린다.
                for (int i = 0; i < 3; i++) {
                    refLrWiener[plane][pass][i] = AvifTables.WIENER_TAPS_MID[i];
                }
            }
        }
        int sbSize = seq.use128x128Superblock ? BLOCK_128X128 : BLOCK_64X64;
        int sbSize4 = AvifTables.NUM_4X4_BLOCKS_WIDE[sbSize];
        // 슈퍼블록 행을 위에서 아래로 처리한다.
        for (int r = miRowStart; r < miRowEnd; r += sbSize4) {
            coeff.clearLeft();
            java.util.Arrays.fill(leftSegPred, (byte) 0);
            // 한 행의 슈퍼블록을 왼쪽에서 오른쪽으로 처리한다.
            for (int c = miColStart; c < miColEnd; c += sbSize4) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new MediaException(MediaException.Kind.CANCELLED, "AV1: decoding was cancelled");
                }
                readDeltas = hdr.deltaQPresent;
                clearCdef(r, c);
                clearBlockDecodedFlags(r, c, sbSize4);
                readLr(r, c, sbSize);
                decodePartition(r, c, sbSize);
            }
        }
    }

    /**
     * Tells if a 4x4 position is inside the tile (<code>is_inside</code>).
     * <p>
     * 4x4 위치가 타일 안에 있는지 알려 준다(<code>is_inside</code>).
     *
     * @param r the row / 행
     * @param c the column / 열
     * @return true if inside / 안이면 true
     */
    private boolean isInside(int r, int c) {
        return c >= miColStart && c < miColEnd && r >= miRowStart && r < miRowEnd;
    }

    /**
     * Marks the CDEF index of the 64x64 blocks of a superblock as not coded (<code>clear_cdef</code>).
     * <p>
     * 슈퍼블록의 64x64 블록의 CDEF 색인을 부호화되지 않음으로 표시한다(<code>clear_cdef</code>).
     *
     * @param r the first row of the superblock / 슈퍼블록의 첫 행
     * @param c the first column of the superblock / 슈퍼블록의 첫 열
     */
    private void clearCdef(int r, int c) {
        int i = (r >> 4) * frame.cdefStride + (c >> 4);
        frame.cdefIdx[i] = -1;
        if (seq.use128x128Superblock) {
            frame.cdefIdx[i + 1] = -1;
            frame.cdefIdx[i + frame.cdefStride] = -1;
            frame.cdefIdx[i + frame.cdefStride + 1] = -1;
        }
    }

    /**
     * Sets the block decoded flags of a superblock (<code>clear_block_decoded_flags</code>).
     * <p>
     * 슈퍼블록의 블록 디코딩 완료 플래그를 설정한다(<code>clear_block_decoded_flags</code>).
     *
     * @param r       the first row / 첫 행
     * @param c       the first column / 첫 열
     * @param sbSize4 the superblock size in 4x4 units / 4x4 단위 슈퍼블록 크기
     */
    private void clearBlockDecodedFlags(int r, int c, int sbSize4) {
        // 평면마다 슈퍼블록 주변의 플래그를 위쪽/왼쪽은 참, 나머지는 거짓으로 채운다.
        for (int plane = 0; plane < seq.numPlanes; plane++) {
            int subX = plane > 0 ? seq.subsamplingX : 0;
            int subY = plane > 0 ? seq.subsamplingY : 0;
            int sbWidth4 = (miColEnd - c) >> subX;
            int sbHeight4 = (miRowEnd - r) >> subY;
            boolean[] bd = blockDecoded[plane];
            // 블록 디코딩 플래그의 행을 처리한다.
            for (int y = -1; y <= (sbSize4 >> subY); y++) {
                // 블록 디코딩 플래그의 열을 처리한다.
                for (int x = -1; x <= (sbSize4 >> subX); x++) {
                    boolean v;
                    if (y < 0 && x < sbWidth4) {
                        v = true;
                    } else if (x < 0 && y < sbHeight4) {
                        v = true;
                    } else {
                        v = false;
                    }
                    bd[(y + 1) * BD_STRIDE + x + 1] = v;
                }
            }
            bd[((sbSize4 >> subY) + 1) * BD_STRIDE] = false;
        }
    }

    /**
     * Reads the loop restoration units of a superblock (<code>read_lr</code>).
     * <p>
     * 슈퍼블록의 루프 복원 단위를 읽는다(<code>read_lr</code>).
     *
     * @param r     the first row / 첫 행
     * @param c     the first column / 첫 열
     * @param bSize the superblock size / 슈퍼블록 크기
     */
    private void readLr(int r, int c, int bSize) {
        int w = AvifTables.NUM_4X4_BLOCKS_WIDE[bSize];
        int h = AvifTables.NUM_4X4_BLOCKS_HIGH[bSize];
        // 평면마다 이 슈퍼블록에 시작점이 있는 복원 단위를 읽는다.
        for (int plane = 0; plane < seq.numPlanes; plane++) {
            if (hdr.frameRestorationType[plane] == Av1FrameHeader.RESTORE_NONE) {
                continue;
            }
            int subX = plane == 0 ? 0 : seq.subsamplingX;
            int subY = plane == 0 ? 0 : seq.subsamplingY;
            int unitSize = hdr.loopRestorationSize[plane];
            int unitRows = frame.lrUnitRows[plane];
            int unitCols = frame.lrUnitCols[plane];
            int unitRowStart = (r * (4 >> subY) + unitSize - 1) / unitSize;
            int unitRowEnd = Math.min(unitRows, ((r + h) * (4 >> subY) + unitSize - 1) / unitSize);
            // 초해상도에서는 열 위치를 확대 전의 너비에 맞게 환산한다.
            int numerator = (4 >> subX) * (hdr.useSuperres ? hdr.superresDenom : 1);
            int denominator = unitSize * (hdr.useSuperres ? 8 : 1);
            int unitColStart = (c * numerator + denominator - 1) / denominator;
            int unitColEnd = Math.min(unitCols, ((c + w) * numerator + denominator - 1) / denominator);
            // 복원 단위 행을 차례로 처리한다.
            for (int unitRow = unitRowStart; unitRow < unitRowEnd; unitRow++) {
                // 복원 단위 열을 차례로 읽는다.
                for (int unitCol = unitColStart; unitCol < unitColEnd; unitCol++) {
                    readLrUnit(plane, unitRow, unitCol);
                }
            }
        }
    }

    /**
     * Reads one loop restoration unit (<code>read_lr_unit</code>).
     * <p>
     * 루프 복원 단위 하나를 읽는다(<code>read_lr_unit</code>).
     *
     * @param plane   the plane / 평면
     * @param unitRow the row of the unit / 단위의 행
     * @param unitCol the column of the unit / 단위의 열
     */
    private void readLrUnit(int plane, int unitRow, int unitCol) {
        int type;
        int frameType = hdr.frameRestorationType[plane];
        if (frameType == Av1FrameHeader.RESTORE_WIENER) {
            type = sd.symbol(cdfs.useWiener, 0, 2) != 0 ? Av1FrameHeader.RESTORE_WIENER : Av1FrameHeader.RESTORE_NONE;
        } else if (frameType == Av1FrameHeader.RESTORE_SGRPROJ) {
            type = sd.symbol(cdfs.useSgrproj, 0, 2) != 0 ? Av1FrameHeader.RESTORE_SGRPROJ : Av1FrameHeader.RESTORE_NONE;
        } else {
            type = sd.symbol(cdfs.restorationType, 0, 3);
        }
        int unit = unitRow * frame.lrUnitCols[plane] + unitCol;
        frame.lrType[plane][unit] = (byte) type;
        if (type == Av1FrameHeader.RESTORE_WIENER) {
            // 두 패스의 위너 계수를 기준 값에 대한 서브지수 부호로 읽는다.
            for (int pass = 0; pass < 2; pass++) {
                int firstCoeff = 0;
                if (plane > 0) {
                    firstCoeff = 1;
                    frame.lrWiener[plane][unit * 6 + pass * 3] = 0;
                }
                // 위너 계수를 차례로 읽는다.
                for (int j = firstCoeff; j < 3; j++) {
                    int min = AvifTables.WIENER_TAPS_MIN[j];
                    int max = AvifTables.WIENER_TAPS_MAX[j];
                    int k = AvifTables.WIENER_TAPS_K[j];
                    int v = decodeSignedSubexp(min, max + 1, k, refLrWiener[plane][pass][j]);
                    frame.lrWiener[plane][unit * 6 + pass * 3 + j] = v;
                    refLrWiener[plane][pass][j] = v;
                }
            }
        } else if (type == Av1FrameHeader.RESTORE_SGRPROJ) {
            int set = sd.literal(4);
            frame.lrSgrSet[plane][unit] = (byte) set;
            // 두 투영 계수를 읽거나 반경이 0 이면 유도한다.
            for (int i = 0; i < 2; i++) {
                int radius = AvifTables.SGR_PARAMS[set * 4 + i * 2];
                int min = AvifTables.SGRPROJ_XQD_MIN[i];
                int max = AvifTables.SGRPROJ_XQD_MAX[i];
                int v;
                if (radius != 0) {
                    v = decodeSignedSubexp(min, max + 1, 4, refSgrXqd[plane][i]);
                } else {
                    v = 0;
                    if (i == 1) {
                        v = Math.max(min, Math.min(max, (1 << 7) - refSgrXqd[plane][0]));
                    }
                }
                frame.lrSgrXqd[plane][unit * 2 + i] = v;
                refSgrXqd[plane][i] = v;
            }
        }
    }

    /**
     * Decodes a signed sub-exponential value relative to a reference (<code>decode_signed_subexp_with_ref_bool</code>).
     * <p>
     * 기준 값에 상대적인 부호 있는 서브지수 값을 디코딩한다(<code>decode_signed_subexp_with_ref_bool</code>).
     *
     * @param low  the lowest value / 최소값
     * @param high the value above the highest / 최대값 다음 값
     * @param k    the parameter k / 매개변수 k
     * @param r    the reference / 기준 값
     * @return the value / 값
     */
    private int decodeSignedSubexp(int low, int high, int k, int r) {
        int mx = high - low;
        int rr = r - low;
        int v = decodeSubexp(mx, k);
        int x;
        if ((rr << 1) <= mx) {
            x = inverseRecenter(rr, v);
        } else {
            x = mx - 1 - inverseRecenter(mx - 1 - rr, v);
        }
        return x + low;
    }

    /**
     * Decodes a sub-exponential value (<code>decode_subexp_bool</code>).
     * <p>
     * 서브지수 값을 디코딩한다(<code>decode_subexp_bool</code>).
     *
     * @param numSyms the number of values / 값의 수
     * @param k       the parameter k / 매개변수 k
     * @return the value / 값
     */
    private int decodeSubexp(int numSyms, int k) {
        int i = 0;
        int mk = 0;
        // 구간을 넓혀 가며 값이 속한 구간을 찾는다.
        while (true) {
            int b2 = i != 0 ? k + i - 1 : k;
            int a = 1 << b2;
            if (numSyms <= mk + 3 * a) {
                return sd.uniform(numSyms - mk) + mk;
            }
            if (sd.literal(1) != 0) {
                i++;
                mk += a;
            } else {
                return sd.literal(b2) + mk;
            }
        }
    }

    /**
     * The inverse recentering function.
     * <p>
     * 역 재중심 함수.
     *
     * @param r the reference / 기준 값
     * @param v the value / 값
     * @return the result / 결과
     */
    private static int inverseRecenter(int r, int v) {
        if (v > 2 * r) {
            return v;
        }
        if ((v & 1) != 0) {
            return r - ((v + 1) >> 1);
        }
        return r + (v >> 1);
    }

    /**
     * Decodes the partition of a block and its sub blocks (<code>decode_partition</code>).
     * <p>
     * 블록의 분할과 하위 블록을 디코딩한다(<code>decode_partition</code>).
     *
     * @param r     the row / 행
     * @param c     the column / 열
     * @param bSize the block size / 블록 크기
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터면 CORRUPT
     */
    private void decodePartition(int r, int c, int bSize) throws MediaException {
        if (r >= frame.miRows || c >= frame.miCols) {
            return;
        }
        availU = isInside(r - 1, c);
        availL = isInside(r, c - 1);
        int num4x4 = AvifTables.NUM_4X4_BLOCKS_WIDE[bSize];
        int half = num4x4 >> 1;
        int quarter = half >> 1;
        boolean hasRows = (r + half) < frame.miRows;
        boolean hasCols = (c + half) < frame.miCols;
        int partition;
        // 블록이 프레임 안에 얼마나 들어오는지에 따라 분할을 읽는 방식이 달라진다.
        if (bSize < BLOCK_8X8) {
            partition = PARTITION_NONE;
        } else if (hasRows && hasCols) {
            partition = readPartition(r, c, bSize);
        } else if (hasCols) {
            partition = readSplitOr(r, c, bSize, true) != 0 ? PARTITION_SPLIT : PARTITION_HORZ;
        } else if (hasRows) {
            partition = readSplitOr(r, c, bSize, false) != 0 ? PARTITION_SPLIT : PARTITION_VERT;
        } else {
            partition = PARTITION_SPLIT;
        }
        int subSize = AvifTables.PARTITION_SUBSIZE[partition * 22 + bSize];
        int splitSize = AvifTables.PARTITION_SUBSIZE[PARTITION_SPLIT * 22 + bSize];
        if (partition == PARTITION_NONE) {
            decodeBlock(r, c, subSize);
        } else if (partition == PARTITION_HORZ) {
            decodeBlock(r, c, subSize);
            if (hasRows) {
                decodeBlock(r + half, c, subSize);
            }
        } else if (partition == PARTITION_VERT) {
            decodeBlock(r, c, subSize);
            if (hasCols) {
                decodeBlock(r, c + half, subSize);
            }
        } else if (partition == PARTITION_SPLIT) {
            decodePartition(r, c, subSize);
            decodePartition(r, c + half, subSize);
            decodePartition(r + half, c, subSize);
            decodePartition(r + half, c + half, subSize);
        } else if (partition == PARTITION_HORZ_A) {
            decodeBlock(r, c, splitSize);
            decodeBlock(r, c + half, splitSize);
            decodeBlock(r + half, c, subSize);
        } else if (partition == PARTITION_HORZ_B) {
            decodeBlock(r, c, subSize);
            decodeBlock(r + half, c, splitSize);
            decodeBlock(r + half, c + half, splitSize);
        } else if (partition == PARTITION_VERT_A) {
            decodeBlock(r, c, splitSize);
            decodeBlock(r + half, c, splitSize);
            decodeBlock(r, c + half, subSize);
        } else if (partition == PARTITION_VERT_B) {
            decodeBlock(r, c, subSize);
            decodeBlock(r, c + half, splitSize);
            decodeBlock(r + half, c + half, splitSize);
        } else if (partition == PARTITION_HORZ_4) {
            decodeBlock(r, c, subSize);
            decodeBlock(r + quarter, c, subSize);
            decodeBlock(r + quarter * 2, c, subSize);
            if (r + quarter * 3 < frame.miRows) {
                decodeBlock(r + quarter * 3, c, subSize);
            }
        } else {
            decodeBlock(r, c, subSize);
            decodeBlock(r, c + quarter, subSize);
            decodeBlock(r, c + quarter * 2, subSize);
            if (c + quarter * 3 < frame.miCols) {
                decodeBlock(r, c + quarter * 3, subSize);
            }
        }
    }

    /**
     * The context of the partition symbols.
     * <p>
     * 분할 심볼의 문맥.
     *
     * @param r     the row / 행
     * @param c     the column / 열
     * @param bSize the block size / 블록 크기
     * @return the context 0..3 / 문맥 0~3
     */
    private int partitionContext(int r, int c, int bSize) {
        int bsl = AvifTables.MI_WIDTH_LOG2[bSize];
        int stride = frame.miStride;
        int above = (availU && AvifTables.MI_WIDTH_LOG2[frame.miSize[(r - 1) * stride + c]] < bsl) ? 1 : 0;
        int left = (availL && AvifTables.MI_HEIGHT_LOG2[frame.miSize[r * stride + c - 1]] < bsl) ? 1 : 0;
        return left * 2 + above;
    }

    /**
     * The array of the partition distributions and the symbol count for a block size.
     * <p>
     * 블록 크기에 맞는 분할 분포 배열.
     *
     * @param bSize the block size / 블록 크기
     * @return the array / 배열
     */
    private int[] partitionCdfArray(int bSize) {
        int bsl = AvifTables.MI_WIDTH_LOG2[bSize];
        if (bsl == 1) {
            return cdfs.partitionW8;
        }
        if (bsl == 2) {
            return cdfs.partitionW16;
        }
        if (bsl == 3) {
            return cdfs.partitionW32;
        }
        if (bsl == 4) {
            return cdfs.partitionW64;
        }
        return cdfs.partitionW128;
    }

    /**
     * Reads the partition symbol.
     * <p>
     * 분할 심볼을 읽는다.
     *
     * @param r     the row / 행
     * @param c     the column / 열
     * @param bSize the block size / 블록 크기
     * @return the partition type / 분할 종류
     */
    private int readPartition(int r, int c, int bSize) {
        int ctx = partitionContext(r, c, bSize);
        int bsl = AvifTables.MI_WIDTH_LOG2[bSize];
        int n = bsl == 1 ? 4 : (bsl == 5 ? 8 : 10);
        return sd.symbol(partitionCdfArray(bSize), ctx * (n + 1), n);
    }

    /**
     * Reads the split_or_horz or split_or_vert symbol of a block at the frame edge.
     * <p>
     * 프레임 가장자리 블록의 split_or_horz 또는 split_or_vert 심볼을 읽는다.
     *
     * @param r          the row / 행
     * @param c          the column / 열
     * @param bSize      the block size / 블록 크기
     * @param horizontal true for split_or_horz, false for split_or_vert / split_or_horz 면 true, split_or_vert 면 false
     * @return 1 if the block is split / 분할하면 1
     */
    private int readSplitOr(int r, int c, int bSize, boolean horizontal) {
        int ctx = partitionContext(r, c, bSize);
        int bsl = AvifTables.MI_WIDTH_LOG2[bSize];
        int n = bsl == 5 ? 8 : 10;
        int[] pc = partitionCdfArray(bSize);
        int base = ctx * (n + 1);
        int psum;
        if (horizontal) {
            psum = prob(pc, base, PARTITION_VERT) + prob(pc, base, PARTITION_SPLIT) + prob(pc, base, PARTITION_HORZ_A)
                    + prob(pc, base, PARTITION_VERT_A) + prob(pc, base, PARTITION_VERT_B);
            if (bSize != BLOCK_128X128) {
                psum += prob(pc, base, 9);
            }
        } else {
            psum = prob(pc, base, PARTITION_HORZ) + prob(pc, base, PARTITION_SPLIT) + prob(pc, base, PARTITION_HORZ_A)
                    + prob(pc, base, PARTITION_HORZ_B) + prob(pc, base, PARTITION_VERT_A);
            if (bSize != BLOCK_128X128) {
                psum += prob(pc, base, PARTITION_HORZ_4);
            }
        }
        int[] tmp = new int[3];
        tmp[0] = (1 << 15) - psum;
        tmp[1] = 1 << 15;
        return sd.symbol(tmp, 0, 2);
    }

    /**
     * The probability mass of one symbol of a cumulative distribution.
     * <p>
     * 누적 분포에서 심볼 하나의 확률 질량.
     *
     * @param cdf  the array / 배열
     * @param base the start of the distribution / 분포의 시작
     * @param sym  the symbol (at least 1) / 심볼(1 이상)
     * @return the mass / 질량
     */
    private static int prob(int[] cdf, int base, int sym) {
        return cdf[base + sym] - cdf[base + sym - 1];
    }

    /**
     * Decodes one block (<code>decode_block</code>) of an intra frame.
     * <p>
     * 인트라 프레임의 블록 하나를 디코딩한다(<code>decode_block</code>).
     *
     * @param r       the row / 행
     * @param c       the column / 열
     * @param subSize the block size / 블록 크기
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터면 CORRUPT
     */
    private void decodeBlock(int r, int c, int subSize) throws MediaException {
        miRow = r;
        miCol = c;
        miSize = subSize;
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[subSize];
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[subSize];
        // 4xN 블록의 홀수 번째가 색차를 대표하므로 짝수 번째는 색차가 없다.
        if (bh4 == 1 && seq.subsamplingY != 0 && (miRow & 1) == 0) {
            hasChroma = false;
        } else if (bw4 == 1 && seq.subsamplingX != 0 && (miCol & 1) == 0) {
            hasChroma = false;
        } else {
            hasChroma = seq.numPlanes > 1;
        }
        availU = isInside(r - 1, c);
        availL = isInside(r, c - 1);
        availUChroma = availU;
        availLChroma = availL;
        if (hasChroma) {
            if (seq.subsamplingY != 0 && bh4 == 1) {
                availUChroma = isInside(r - 2, c);
            }
            if (seq.subsamplingX != 0 && bw4 == 1) {
                availLChroma = isInside(r, c - 2);
            }
        } else {
            availUChroma = false;
            availLChroma = false;
        }
        // 4:2:2 에서 일부 블록 크기는 색차 블록 크기가 없으므로 손상된 스트림이다.
        if (hasChroma && AvifTables.SUBSAMPLED_SIZE[subSize * 4 + seq.subsamplingX * 2 + seq.subsamplingY] >= 22) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the block size has no chroma block size");
        }
        if (hdr.frameIsIntra) {
            intraFrameModeInfo();
        } else {
            interFrameModeInfo();
        }
        paletteTokens();
        readBlockTxSize();
        if (skip) {
            resetBlockContext(bw4, bh4);
        }
        int stride = frame.miStride;
        // 블록이 덮는 모든 4x4 위치에 모드 정보를 기록한다.
        for (int y = 0; y < bh4; y++) {
            int row = (r + y) * stride + c;
            // 블록이 덮는 한 행의 4x4 위치를 처리한다.
            for (int x = 0; x < bw4; x++) {
                int i = row + x;
                frame.yMode[i] = (byte) yMode;
                if (hasChroma) {
                    frame.uvMode[i] = (byte) uvMode;
                }
                frame.miSize[i] = (byte) subSize;
                frame.skip[i] = (byte) (skip ? 1 : 0);
                frame.segmentId[i] = (byte) segmentId;
                frame.paletteSizeY[i] = (byte) paletteSizeY;
                frame.paletteSizeUv[i] = (byte) paletteSizeUv;
                frame.isInter[i] = (byte) (isInter ? 1 : 0);
                frame.skipMode[i] = (byte) (skipMode ? 1 : 0);
                frame.compGroupIdx[i] = (byte) (isInter && !useIntrabc ? compGroupIdx : 0);
                frame.compoundIdx[i] = (byte) (isInter && !useIntrabc ? compoundIdx : 0);
                frame.refFrame0[i] = (byte) refFrame0;
                frame.refFrame1[i] = (byte) refFrame1;
                // 인터 블록이면 움직임 벡터와 보간 필터도 기록한다.
                if (isInter) {
                    frame.interpFilter0[i] = (byte) interpFilter0;
                    frame.interpFilter1[i] = (byte) interpFilter1;
                    // 벡터의 네 성분을 프레임에 기록한다.
                    for (int k = 0; k < 4; k++) {
                        frame.mvs[i * 4 + k] = (short) curMv[k];
                    }
                }
                frame.paletteColorsY[i] = paletteSizeY > 0 ? paletteColorsY : null;
                frame.paletteColorsU[i] = paletteSizeUv > 0 ? paletteColorsU : null;
                if (frame.deltaLf != null) {
                    // 네 개의 루프 필터 변화량을 기록한다.
                    for (int k = 0; k < 4; k++) {
                        frame.deltaLf[i * 4 + k] = (byte) deltaLfState[k];
                    }
                }
            }
        }
        computePrediction();
        residual();
    }

    /**
     * Zeroes the coefficient contexts of a skipped block (<code>reset_block_context</code>).
     * <p>
     * skip 블록의 계수 문맥을 0 으로 만든다(<code>reset_block_context</code>).
     *
     * @param bw4 the width in 4x4 units / 4x4 단위 너비
     * @param bh4 the height in 4x4 units / 4x4 단위 높이
     */
    private void resetBlockContext(int bw4, int bh4) {
        int planes = hasChroma ? 3 : 1;
        // 휘도(와 색차)의 위쪽/왼쪽 수준 문맥과 DC 문맥을 0 으로 만든다.
        for (int plane = 0; plane < planes; plane++) {
            int subX = plane > 0 ? seq.subsamplingX : 0;
            int subY = plane > 0 ? seq.subsamplingY : 0;
            // 위쪽 문맥을 0 으로 만든다.
            for (int i = miCol >> subX; i < ((miCol + bw4) >> subX); i++) {
                coeff.aboveLevel[plane][i] = 0;
                coeff.aboveDc[plane][i] = 0;
            }
            // 왼쪽 문맥을 0 으로 만든다.
            for (int i = miRow >> subY; i < ((miRow + bh4) >> subY); i++) {
                coeff.leftLevel[plane][i] = 0;
                coeff.leftDc[plane][i] = 0;
            }
        }
    }

    /**
     * Tells if a segment feature is active for the current block.
     * <p>
     * 현재 블록에서 세그먼트 기능이 활성인지 알려 준다.
     *
     * @param feature the feature number / 기능 번호
     * @return true if active / 활성이면 true
     */
    private boolean segFeatureActive(int feature) {
        return hdr.segmentationEnabled && hdr.featureEnabled[segmentId][feature];
    }

    /**
     * Reads the mode info of a block of an intra frame (<code>intra_frame_mode_info</code>).
     * <p>
     * 인트라 프레임 블록의 모드 정보를 읽는다(<code>intra_frame_mode_info</code>).
     */
    private void intraFrameModeInfo() throws MediaException {
        skip = false;
        if (hdr.segIdPreSkip) {
            intraSegmentId();
        }
        readSkip();
        if (!hdr.segIdPreSkip) {
            intraSegmentId();
        }
        readCdef();
        readDeltaQIndex();
        readDeltaLf();
        readDeltas = false;
        refFrame0 = 0;
        refFrame1 = -1;
        isInter = false;
        useIntrabc = hdr.allowIntrabc && sd.symbol(cdfs.intrabc, 0, 2) != 0;
        if (useIntrabc) {
            // 인트라 블록 복사: 현재 프레임에서 이미 디코딩된 영역을 정수 움직임 벡터로 복사하는 인터 블록이다.
            isInter = true;
            yMode = 0;
            uvMode = 0;
            angleDeltaY = 0;
            angleDeltaUv = 0;
            cflAlphaU = 0;
            cflAlphaV = 0;
            paletteSizeY = 0;
            paletteSizeUv = 0;
            useFilterIntra = false;
            filterIntraMode = 0;
            interpFilter0 = 3;
            interpFilter1 = 3;
            mvStack.find(miRow, miCol, miSize, 0, -1);
            assignMvIntrabc();
            return;
        }
        int stride = frame.miStride;
        int aboveMode = AvifTables.INTRA_MODE_CONTEXT[availU ? frame.yMode[(miRow - 1) * stride + miCol] : 0];
        int leftMode = AvifTables.INTRA_MODE_CONTEXT[availL ? frame.yMode[miRow * stride + miCol - 1] : 0];
        yMode = sd.symbol(cdfs.intraFrameYMode, (aboveMode * 5 + leftMode) * 14, 13);
        readIntraModes();
    }

    /**
     * Reads the rest of the modes of an intra block after the luma mode: the angle deltas, the chroma mode, the palette and the filter intra
     * mode (shared by the intra frames and the intra blocks of inter frames).
     * <p>
     * 인트라 블록에서 휘도 모드 뒤의 나머지 모드를 읽는다: 각도 변화량, 색차 모드, 팔레트, 필터 인트라 모드(인트라 프레임과 인터 프레임 안의
     * 인트라 블록이 공유한다).
     */
    private void readIntraModes() {
        angleDeltaY = 0;
        if (miSize >= BLOCK_8X8 && Av1IntraPred.isDirectional(yMode)) {
            angleDeltaY = sd.symbol(cdfs.angleDelta, (yMode - 1) * 8, 7) - 3;
        }
        uvMode = 0;
        angleDeltaUv = 0;
        cflAlphaU = 0;
        cflAlphaV = 0;
        if (hasChroma) {
            int bw = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4;
            int bh = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4;
            boolean cflAllowed;
            if (lossless && AvifTables.SUBSAMPLED_SIZE[miSize * 4 + seq.subsamplingX * 2 + seq.subsamplingY] == 0) {
                cflAllowed = true;
            } else {
                cflAllowed = !lossless && Math.max(bw, bh) <= 32;
            }
            if (cflAllowed) {
                uvMode = sd.symbol(cdfs.uvModeCflAllowed, yMode * 15, 14);
            } else {
                uvMode = sd.symbol(cdfs.uvModeCflNotAllowed, yMode * 14, 13);
            }
            if (uvMode == UV_CFL_PRED) {
                readCflAlphas();
            }
            if (miSize >= BLOCK_8X8 && Av1IntraPred.isDirectional(uvMode)) {
                angleDeltaUv = sd.symbol(cdfs.angleDelta, (uvMode - 1) * 8, 7) - 3;
            }
        }
        paletteSizeY = 0;
        paletteSizeUv = 0;
        if (miSize >= BLOCK_8X8 && AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] <= 16 && AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] <= 16
                && hdr.allowScreenContentTools) {
            paletteModeInfo();
        }
        filterIntraModeInfo();
    }

    /**
     * Reads or derives the segment id (<code>intra_segment_id</code>).
     * <p>
     * 세그먼트 id 를 읽거나 유도한다(<code>intra_segment_id</code>).
     */
    private void intraSegmentId() {
        if (hdr.segmentationEnabled) {
            readSegmentId();
        } else {
            segmentId = 0;
        }
        int q = hdr.qindexOfSegment(segmentId);
        lossless = q == 0 && hdr.deltaQYDc == 0 && hdr.deltaQUAc == 0 && hdr.deltaQUDc == 0 && hdr.deltaQVAc == 0 && hdr.deltaQVDc == 0;
    }

    /**
     * Reads the segment id with the spatial prediction (<code>read_segment_id</code>).
     * <p>
     * 공간 예측을 써서 세그먼트 id 를 읽는다(<code>read_segment_id</code>).
     */
    private void readSegmentId() {
        int stride = frame.miStride;
        int prevUL = (availU && availL) ? frame.segmentId[(miRow - 1) * stride + miCol - 1] : -1;
        int prevU = availU ? frame.segmentId[(miRow - 1) * stride + miCol] : -1;
        int prevL = availL ? frame.segmentId[miRow * stride + miCol - 1] : -1;
        int pred;
        if (prevU == -1) {
            pred = prevL == -1 ? 0 : prevL;
        } else if (prevL == -1) {
            pred = prevU;
        } else {
            pred = prevUL == prevU ? prevU : prevL;
        }
        if (skip) {
            segmentId = pred;
        } else {
            int ctx;
            if (prevUL < 0) {
                ctx = 0;
            } else if (prevUL == prevU && prevUL == prevL) {
                ctx = 2;
            } else if (prevUL == prevU || prevUL == prevL || prevU == prevL) {
                ctx = 1;
            } else {
                ctx = 0;
            }
            int v = sd.symbol(cdfs.segmentId, ctx * 9, 8);
            segmentId = negDeinterleave(v, pred, hdr.lastActiveSegId + 1);
            segmentId = Math.max(0, Math.min(hdr.lastActiveSegId, segmentId));
        }
    }

    /**
     * The inverse of the interleaving of the segment id difference (<code>neg_deinterleave</code>).
     * <p>
     * 세그먼트 id 차이의 교차 배열의 역 연산(<code>neg_deinterleave</code>).
     *
     * @param diff the coded difference / 부호화된 차이
     * @param ref  the predicted id / 예측한 id
     * @param max  the number of segments / 세그먼트 수
     * @return the id / id
     */
    private static int negDeinterleave(int diff, int ref, int max) {
        if (ref == 0) {
            return diff;
        }
        if (ref >= max - 1) {
            return max - diff - 1;
        }
        if (2 * ref < max) {
            if (diff <= 2 * ref) {
                if ((diff & 1) != 0) {
                    return ref + ((diff + 1) >> 1);
                }
                return ref - (diff >> 1);
            }
            return diff;
        }
        if (diff <= 2 * (max - ref - 1)) {
            if ((diff & 1) != 0) {
                return ref + ((diff + 1) >> 1);
            }
            return ref - (diff >> 1);
        }
        return max - (diff + 1);
    }

    /**
     * Reads the skip flag (<code>read_skip</code>).
     * <p>
     * skip 플래그를 읽는다(<code>read_skip</code>).
     */
    private void readSkip() {
        if (hdr.segIdPreSkip && segFeatureActive(6)) {
            skip = true;
            return;
        }
        int ctx = 0;
        int stride = frame.miStride;
        if (availU) {
            ctx += frame.skip[(miRow - 1) * stride + miCol];
        }
        if (availL) {
            ctx += frame.skip[miRow * stride + miCol - 1];
        }
        skip = sd.symbol(cdfs.skip, ctx * 3, 2) != 0;
    }

    /**
     * Reads the CDEF index of the 64x64 block (<code>read_cdef</code>).
     * <p>
     * 64x64 블록의 CDEF 색인을 읽는다(<code>read_cdef</code>).
     */
    private void readCdef() {
        if (skip || hdr.codedLossless || !seq.enableCdef || hdr.allowIntrabc) {
            return;
        }
        int idx = (miRow >> 4) * frame.cdefStride + (miCol >> 4);
        if (frame.cdefIdx[idx] == -1) {
            int v = sd.literal(hdr.cdefBits);
            frame.cdefIdx[idx] = v;
            int w4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
            int h4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
            // 블록이 여러 64x64 영역에 걸치면 같은 색인을 모두에 기록한다.
            for (int i = 0; i < h4; i += 16) {
                for (int j = 0; j < w4; j += 16) {
                    frame.cdefIdx[idx + (i >> 4) * frame.cdefStride + (j >> 4)] = v;
                }
            }
        }
    }

    /**
     * Reads the quantizer index delta (<code>read_delta_qindex</code>).
     * <p>
     * 양자화 지수 변화량을 읽는다(<code>read_delta_qindex</code>).
     */
    private void readDeltaQIndex() {
        int sbSize = seq.use128x128Superblock ? BLOCK_128X128 : BLOCK_64X64;
        if (miSize == sbSize && skip) {
            return;
        }
        if (readDeltas) {
            int abs = sd.symbol(cdfs.deltaQ, 0, 4);
            if (abs == 3) {
                int remBits = sd.literal(3) + 1;
                int absBits = sd.literal(remBits);
                abs = absBits + (1 << remBits) + 1;
            }
            if (abs != 0) {
                int sign = sd.literal(1);
                int reduced = sign != 0 ? -abs : abs;
                currentQIndex = Math.max(1, Math.min(255, currentQIndex + (reduced << hdr.deltaQRes)));
            }
        }
    }

    /**
     * Reads the loop filter level deltas (<code>read_delta_lf</code>).
     * <p>
     * 루프 필터 수준 변화량을 읽는다(<code>read_delta_lf</code>).
     */
    private void readDeltaLf() {
        int sbSize = seq.use128x128Superblock ? BLOCK_128X128 : BLOCK_64X64;
        if (miSize == sbSize && skip) {
            return;
        }
        if (readDeltas && hdr.deltaLfPresent) {
            int frameLfCount = 1;
            if (hdr.deltaLfMulti) {
                frameLfCount = seq.numPlanes > 1 ? 4 : 2;
            }
            // 사용하는 변화량마다 크기와 부호를 읽는다.
            for (int i = 0; i < frameLfCount; i++) {
                int[] cdf = hdr.deltaLfMulti ? cdfs.deltaLfMulti[i] : cdfs.deltaLf;
                int abs = sd.symbol(cdf, 0, 4);
                int deltaLfAbs;
                if (abs == 3) {
                    int n = sd.literal(3) + 1;
                    int absBits = sd.literal(n);
                    deltaLfAbs = absBits + (1 << n) + 1;
                } else {
                    deltaLfAbs = abs;
                }
                if (deltaLfAbs != 0) {
                    int sign = sd.literal(1);
                    int reduced = sign != 0 ? -deltaLfAbs : deltaLfAbs;
                    deltaLfState[i] = Math.max(-63, Math.min(63, deltaLfState[i] + (reduced << hdr.deltaLfRes)));
                }
            }
        }
    }

    /**
     * Reads the alphas of the chroma from luma prediction (<code>read_cfl_alphas</code>).
     * <p>
     * 휘도로부터의 색차 예측의 알파를 읽는다(<code>read_cfl_alphas</code>).
     */
    private void readCflAlphas() {
        int signs = sd.symbol(cdfs.cflSign, 0, 8);
        int signU = (signs + 1) / 3;
        int signV = (signs + 1) % 3;
        if (signU != 0) {
            int ctx = (signU - 1) * 3 + signV;
            cflAlphaU = 1 + sd.symbol(cdfs.cflAlpha, ctx * 17, 16);
            if (signU == 1) {
                cflAlphaU = -cflAlphaU;
            }
        } else {
            cflAlphaU = 0;
        }
        if (signV != 0) {
            int ctx = (signV - 1) * 3 + signU;
            cflAlphaV = 1 + sd.symbol(cdfs.cflAlpha, ctx * 17, 16);
            if (signV == 1) {
                cflAlphaV = -cflAlphaV;
            }
        } else {
            cflAlphaV = 0;
        }
    }

    /**
     * Reads the filter intra flag and mode (<code>filter_intra_mode_info</code>).
     * <p>
     * 필터 인트라 플래그와 모드를 읽는다(<code>filter_intra_mode_info</code>).
     */
    private void filterIntraModeInfo() {
        useFilterIntra = false;
        filterIntraMode = 0;
        if (seq.enableFilterIntra && yMode == 0 && paletteSizeY == 0
                && Math.max(AvifTables.NUM_4X4_BLOCKS_WIDE[miSize], AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]) <= 8) {
            useFilterIntra = sd.symbol(cdfs.filterIntra, miSize * 3, 2) != 0;
            if (useFilterIntra) {
                filterIntraMode = sd.symbol(cdfs.filterIntraMode, 0, 5);
            }
        }
    }

    /**
     * Reads the palette sizes and colours (<code>palette_mode_info</code>).
     * <p>
     * 팔레트 크기와 색을 읽는다(<code>palette_mode_info</code>).
     */
    private void paletteModeInfo() {
        int bsizeCtx = AvifTables.MI_WIDTH_LOG2[miSize] + AvifTables.MI_HEIGHT_LOG2[miSize] - 2;
        int bitDepth = seq.bitDepth;
        int stride = frame.miStride;
        if (yMode == 0) {
            int ctx = 0;
            if (availU && frame.paletteSizeY[(miRow - 1) * stride + miCol] > 0) {
                ctx++;
            }
            if (availL && frame.paletteSizeY[miRow * stride + miCol - 1] > 0) {
                ctx++;
            }
            if (sd.symbol(cdfs.paletteYMode, (bsizeCtx * 3 + ctx) * 3, 2) != 0) {
                paletteSizeY = sd.symbol(cdfs.paletteYSize, bsizeCtx * 8, 7) + 2;
                paletteColorsY = readPaletteColors(0, paletteSizeY, bitDepth, 1);
            }
        }
        if (hasChroma && uvMode == 0) {
            int ctx = paletteSizeY > 0 ? 1 : 0;
            if (sd.symbol(cdfs.paletteUvMode, ctx * 3, 2) != 0) {
                paletteSizeUv = sd.symbol(cdfs.paletteUvSize, bsizeCtx * 8, 7) + 2;
                paletteColorsU = readPaletteColors(1, paletteSizeUv, bitDepth, 0);
                paletteColorsV = new short[paletteSizeUv];
                // V 평면의 색은 델타 부호화 여부에 따라 두 가지 방식으로 읽는다.
                if (sd.literal(1) != 0) {
                    int minBits = bitDepth - 4;
                    int maxVal = 1 << bitDepth;
                    int paletteBits = minBits + sd.literal(2);
                    paletteColorsV[0] = (short) sd.literal(bitDepth);
                    // V 팔레트 색을 차례로 읽는다.
                    for (int idx = 1; idx < paletteSizeUv; idx++) {
                        int delta = sd.literal(paletteBits);
                        if (delta != 0 && sd.literal(1) != 0) {
                            delta = -delta;
                        }
                        int val = paletteColorsV[idx - 1] + delta;
                        if (val < 0) {
                            val += maxVal;
                        }
                        if (val >= maxVal) {
                            val -= maxVal;
                        }
                        paletteColorsV[idx] = (short) Math.max(0, Math.min(maxVal - 1, val));
                    }
                } else {
                    // V 팔레트 색을 차례로 읽는다.
                    for (int idx = 0; idx < paletteSizeUv; idx++) {
                        paletteColorsV[idx] = (short) sd.literal(bitDepth);
                    }
                }
            }
        }
    }

    /**
     * Reads the colours of a luma or U palette: the colours reused from the cache followed by the new ones coded as deltas.
     * <p>
     * 휘도 또는 U 팔레트의 색을 읽는다: 캐시에서 재사용한 색과 델타로 부호화한 새 색.
     *
     * @param plane    0 for luma, 1 for chroma / 휘도면 0, 색차면 1
     * @param size     the palette size / 팔레트 크기
     * @param bitDepth the bit depth / 비트 깊이
     * @param deltaAdd the value added to the coded delta (1 for luma, 0 for chroma) / 부호화된 델타에 더하는 값(휘도 1, 색차 0)
     * @return the sorted colours / 정렬된 색
     */
    private short[] readPaletteColors(int plane, int size, int bitDepth, int deltaAdd) {
        short[] colors = new short[size];
        int[] cache = new int[16];
        int cacheN = paletteCache(plane, cache);
        int idx = 0;
        // 캐시의 색을 쓸지 하나씩 묻는다.
        for (int i = 0; i < cacheN && idx < size; i++) {
            if (sd.literal(1) != 0) {
                colors[idx++] = (short) cache[i];
            }
        }
        if (idx < size) {
            colors[idx++] = (short) sd.literal(bitDepth);
        }
        int paletteBits = 0;
        if (idx < size) {
            paletteBits = bitDepth - 3 + sd.literal(2);
        }
        int maxVal = (1 << bitDepth) - 1;
        // 나머지 색은 이전 색에 대한 차이로 읽는다.
        while (idx < size) {
            int delta = sd.literal(paletteBits) + deltaAdd;
            int v = Math.min(maxVal, colors[idx - 1] + delta);
            colors[idx] = (short) v;
            int range = (1 << bitDepth) - v - deltaAdd;
            paletteBits = Math.min(paletteBits, ceilLog2(range));
            idx++;
        }
        // 정렬: 값이 몇 개 안 되므로 삽입 정렬을 쓴다.
        for (int i = 1; i < size; i++) {
            short key = colors[i];
            int j = i - 1;
            // 키보다 큰 값을 한 칸씩 뒤로 민다.
            while (j >= 0 && colors[j] > key) {
                colors[j + 1] = colors[j];
                j--;
            }
            colors[j + 1] = key;
        }
        return colors;
    }

    /**
     * The base 2 logarithm rounded up.
     * <p>
     * 올림한 밑 2 로그.
     *
     * @param x the value / 값
     * @return the logarithm / 로그
     */
    private static int ceilLog2(int x) {
        if (x < 2) {
            return 0;
        }
        int i = 1;
        int p = 2;
        // 2 의 거듭제곱이 x 이상이 될 때까지 올린다.
        while (p < x) {
            i++;
            p <<= 1;
        }
        return i;
    }

    /**
     * Builds the palette cache from the palettes above and to the left (<code>get_palette_cache</code>).
     * <p>
     * 위쪽과 왼쪽 팔레트에서 팔레트 캐시를 만든다(<code>get_palette_cache</code>).
     *
     * @param plane 0 for luma, 1 for chroma / 휘도면 0, 색차면 1
     * @param cache receives the colours / 색을 받는다
     * @return the number of colours / 색의 수
     */
    private int paletteCache(int plane, int[] cache) {
        int stride = frame.miStride;
        int aboveN = 0;
        short[] aboveColors = null;
        if (((miRow * 4) % 64) != 0) {
            int i = (miRow - 1) * stride + miCol;
            aboveN = plane == 0 ? frame.paletteSizeY[i] : frame.paletteSizeUv[i];
            aboveColors = plane == 0 ? frame.paletteColorsY[i] : frame.paletteColorsU[i];
        }
        int leftN = 0;
        short[] leftColors = null;
        if (availL) {
            int i = miRow * stride + miCol - 1;
            leftN = plane == 0 ? frame.paletteSizeY[i] : frame.paletteSizeUv[i];
            leftColors = plane == 0 ? frame.paletteColorsY[i] : frame.paletteColorsU[i];
        }
        int aboveIdx = 0;
        int leftIdx = 0;
        int n = 0;
        // 두 팔레트를 병합하며 중복을 제거한다.
        while (aboveIdx < aboveN && leftIdx < leftN) {
            int aboveC = aboveColors[aboveIdx];
            int leftC = leftColors[leftIdx];
            if (leftC < aboveC) {
                if (n == 0 || leftC != cache[n - 1]) {
                    cache[n++] = leftC;
                }
                leftIdx++;
            } else {
                if (n == 0 || aboveC != cache[n - 1]) {
                    cache[n++] = aboveC;
                }
                aboveIdx++;
                if (leftC == aboveC) {
                    leftIdx++;
                }
            }
        }
        // 위쪽 팔레트의 남은 색을 캐시에 추가한다.
        while (aboveIdx < aboveN) {
            int val = aboveColors[aboveIdx++];
            if (n == 0 || val != cache[n - 1]) {
                cache[n++] = val;
            }
        }
        // 왼쪽 팔레트의 남은 색을 캐시에 추가한다.
        while (leftIdx < leftN) {
            int val = leftColors[leftIdx++];
            if (n == 0 || val != cache[n - 1]) {
                cache[n++] = val;
            }
        }
        return n;
    }

    /**
     * Reads the palette colour index maps of the block (<code>palette_tokens</code>).
     * <p>
     * 블록의 팔레트 색 색인 지도를 읽는다(<code>palette_tokens</code>).
     */
    private void paletteTokens() {
        int blockHeight = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4;
        int blockWidth = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4;
        int onscreenHeight = Math.min(blockHeight, (frame.miRows - miRow) * 4);
        int onscreenWidth = Math.min(blockWidth, (frame.miCols - miCol) * 4);
        if (paletteSizeY > 0) {
            readColorMap(colorMapY, paletteSizeY, blockWidth, blockHeight, onscreenWidth, onscreenHeight, true);
        }
        if (paletteSizeUv > 0) {
            blockHeight >>= seq.subsamplingY;
            blockWidth >>= seq.subsamplingX;
            onscreenHeight >>= seq.subsamplingY;
            onscreenWidth >>= seq.subsamplingX;
            if (blockWidth < 4) {
                blockWidth += 2;
                onscreenWidth += 2;
            }
            if (blockHeight < 4) {
                blockHeight += 2;
                onscreenHeight += 2;
            }
            readColorMap(colorMapUv, paletteSizeUv, blockWidth, blockHeight, onscreenWidth, onscreenHeight, false);
        }
    }

    /**
     * Reads one colour index map in wavefront order and replicates it over the area outside the frame.
     * <p>
     * 색 색인 지도 하나를 대각선 순서로 읽고 프레임 밖 영역으로 복제한다.
     *
     * @param map          the map (row stride 64) / 지도(행 간격 64)
     * @param n            the palette size / 팔레트 크기
     * @param blockWidth   the block width / 블록 너비
     * @param blockHeight  the block height / 블록 높이
     * @param onscreenW    the part of the width that is inside the frame / 프레임 안에 있는 너비
     * @param onscreenH    the part of the height that is inside the frame / 프레임 안에 있는 높이
     * @param luma         true for the luma map / 휘도 지도면 true
     */
    private void readColorMap(byte[] map, int n, int blockWidth, int blockHeight, int onscreenW, int onscreenH, boolean luma) {
        map[0] = (byte) sd.uniform(n);
        int[] scores = new int[8];
        int[] order = new int[8];
        // 대각선 순서(i = 행 + 열)로 색 색인을 읽는다.
        for (int i = 1; i < onscreenH + onscreenW - 1; i++) {
            for (int j = Math.min(i, onscreenW - 1); j >= Math.max(0, i - onscreenH + 1); j--) {
                int hash = paletteColorContext(map, i - j, j, n, scores, order);
                int[] cdf = luma ? cdfs.paletteYColor[n] : cdfs.paletteUvColor[n];
                int ctx = PALETTE_COLOR_CONTEXT[hash];
                int idx = sd.symbol(cdf, ctx * (n + 1), n);
                map[(i - j) * 64 + j] = (byte) order[idx];
            }
        }
        // 프레임 밖의 오른쪽 열을 마지막 열 값으로 복제한다.
        for (int i = 0; i < onscreenH; i++) {
            for (int j = onscreenW; j < blockWidth; j++) {
                map[i * 64 + j] = map[i * 64 + onscreenW - 1];
            }
        }
        // 프레임 밖의 아래쪽 행을 마지막 행으로 복제한다.
        for (int i = onscreenH; i < blockHeight; i++) {
            for (int j = 0; j < blockWidth; j++) {
                map[i * 64 + j] = map[(onscreenH - 1) * 64 + j];
            }
        }
    }

    /**
     * Computes the colour order and the context hash of a position (<code>get_palette_color_context</code>).
     * <p>
     * 위치의 색 순서와 문맥 해시를 계산한다(<code>get_palette_color_context</code>).
     *
     * @param map    the colour map / 색 지도
     * @param r      the row / 행
     * @param c      the column / 열
     * @param n      the palette size / 팔레트 크기
     * @param scores scratch array of 8 / 크기 8 의 임시 배열
     * @param order  receives the colour order / 색 순서를 받는다
     * @return the context hash / 문맥 해시
     */
    private static int paletteColorContext(byte[] map, int r, int c, int n, int[] scores, int[] order) {
        // 점수와 색 순서를 초기화한다.
        for (int i = 0; i < 8; i++) {
            scores[i] = 0;
            order[i] = i;
        }
        if (c > 0) {
            scores[map[r * 64 + c - 1]] += 2;
        }
        if (r > 0 && c > 0) {
            scores[map[(r - 1) * 64 + c - 1]] += 1;
        }
        if (r > 0) {
            scores[map[(r - 1) * 64 + c]] += 2;
        }
        // 점수가 높은 색을 앞으로 옮겨 세 이웃 순서를 정한다.
        for (int i = 0; i < 3; i++) {
            int maxScore = scores[i];
            int maxIdx = i;
            // 남은 색 중 점수가 가장 높은 것을 찾는다.
            for (int j = i + 1; j < n; j++) {
                if (scores[j] > maxScore) {
                    maxScore = scores[j];
                    maxIdx = j;
                }
            }
            if (maxIdx != i) {
                maxScore = scores[maxIdx];
                int maxColorOrder = order[maxIdx];
                // 점수가 높은 색을 앞으로 옮기며 나머지를 뒤로 민다.
                for (int k = maxIdx; k > i; k--) {
                    scores[k] = scores[k - 1];
                    order[k] = order[k - 1];
                }
                scores[i] = maxScore;
                order[i] = maxColorOrder;
            }
        }
        int hash = 0;
        // 세 이웃의 점수로 문맥 해시를 만든다.
        for (int i = 0; i < 3; i++) {
            hash += scores[i] * AvifTables.PALETTE_COLOR_HASH_MULTIPLIERS[i];
        }
        return hash;
    }

    /**
     * Reads the transform size of the block (<code>read_block_tx_size</code>): inter blocks may use a tree of transform sizes.
     * <p>
     * 블록의 변환 크기를 읽는다(<code>read_block_tx_size</code>): 인터 블록은 변환 크기 트리를 쓸 수 있다.
     */
    private void readBlockTxSize() {
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        if (hdr.txMode == Av1FrameHeader.TX_MODE_SELECT && miSize > 0 && isInter && !skip && !lossless) {
            int maxTxSz = AvifTables.MAX_TX_SIZE_RECT[miSize];
            int txW4 = AvifTables.TX_WIDTH[maxTxSz] / 4;
            int txH4 = AvifTables.TX_HEIGHT[maxTxSz] / 4;
            // 블록을 최대 변환 크기 단위로 나누어 변환 크기 트리를 읽는다.
            for (int row = miRow; row < miRow + bh4; row += txH4) {
                for (int col = miCol; col < miCol + bw4; col += txW4) {
                    readVarTxSize(row, col, maxTxSz, 0);
                }
            }
            return;
        }
        readTxSize(!skip || !isInter);
        int stride = frame.miStride;
        // 블록이 덮는 모든 위치에 변환 크기를 기록한다.
        for (int row = miRow; row < miRow + bh4; row++) {
            for (int col = miCol; col < miCol + bw4; col++) {
                frame.interTxSize[row * stride + col] = (byte) txSize;
            }
        }
    }

    /**
     * Reads the transform size tree of an inter block (<code>read_var_tx_size</code>).
     * <p>
     * 인터 블록의 변환 크기 트리를 읽는다(<code>read_var_tx_size</code>).
     *
     * @param row   the row in 4x4 units / 4x4 단위 행
     * @param col   the column in 4x4 units / 4x4 단위 열
     * @param txSz  the transform size of this node / 이 노드의 변환 크기
     * @param depth the depth in the tree / 트리의 깊이
     */
    private void readVarTxSize(int row, int col, int txSz, int depth) {
        if (row >= frame.miRows || col >= frame.miCols) {
            return;
        }
        boolean split;
        if (txSz == 0 || depth == 2) {
            split = false;
        } else {
            int above = aboveTxWidth(row, col) < AvifTables.TX_WIDTH[txSz] ? 1 : 0;
            int left = leftTxHeight(row, col) < AvifTables.TX_HEIGHT[txSz] ? 1 : 0;
            int size = Math.min(64, Math.max(AvifTables.NUM_4X4_BLOCKS_WIDE[miSize], AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]) * 4);
            int maxTxSz = findTxSize(size, size);
            int ctx = (AvifTables.TX_SIZE_SQR_UP[txSz] != maxTxSz ? 3 : 0) + (5 - 1 - maxTxSz) * 6 + above + left;
            split = sd.symbol(cdfs.txfmSplit, ctx * 3, 2) != 0;
        }
        int w4 = AvifTables.TX_WIDTH[txSz] / 4;
        int h4 = AvifTables.TX_HEIGHT[txSz] / 4;
        if (split) {
            int subTxSz = SPLIT_TX_SIZE[txSz];
            int stepW = AvifTables.TX_WIDTH[subTxSz] / 4;
            int stepH = AvifTables.TX_HEIGHT[subTxSz] / 4;
            // 분할하면 하위 변환 크기로 네 조각 또는 두 조각을 재귀적으로 읽는다.
            for (int i = 0; i < h4; i += stepH) {
                for (int j = 0; j < w4; j += stepW) {
                    readVarTxSize(row + i, col + j, subTxSz, depth + 1);
                }
            }
        } else {
            int stride = frame.miStride;
            // 분할하지 않으면 이 노드가 덮는 모든 위치에 변환 크기를 기록한다.
            for (int i = 0; i < h4; i++) {
                for (int j = 0; j < w4; j++) {
                    frame.interTxSize[(row + i) * stride + col + j] = (byte) txSz;
                }
            }
            txSize = txSz;
        }
    }

    /**
     * Returns the width of the transform above a position (<code>get_above_tx_width</code>).
     * <p>
     * 위치 위쪽 변환의 너비를 반환한다(<code>get_above_tx_width</code>).
     *
     * @param row the row in 4x4 units / 4x4 단위 행
     * @param col the column in 4x4 units / 4x4 단위 열
     * @return the width in samples / 샘플 단위 너비
     */
    private int aboveTxWidth(int row, int col) {
        int stride = frame.miStride;
        if (row == miRow) {
            if (!availU) {
                return 64;
            }
            int i = (row - 1) * stride + col;
            if (frame.skip[i] != 0 && frame.isInter[i] != 0) {
                return AvifTables.NUM_4X4_BLOCKS_WIDE[frame.miSize[i]] * 4;
            }
        }
        return AvifTables.TX_WIDTH[frame.interTxSize[(row - 1) * stride + col]];
    }

    /**
     * Returns the height of the transform left of a position (<code>get_left_tx_height</code>).
     * <p>
     * 위치 왼쪽 변환의 높이를 반환한다(<code>get_left_tx_height</code>).
     *
     * @param row the row in 4x4 units / 4x4 단위 행
     * @param col the column in 4x4 units / 4x4 단위 열
     * @return the height in samples / 샘플 단위 높이
     */
    private int leftTxHeight(int row, int col) {
        int stride = frame.miStride;
        if (col == miCol) {
            if (!availL) {
                return 64;
            }
            int i = row * stride + col - 1;
            if (frame.skip[i] != 0 && frame.isInter[i] != 0) {
                return AvifTables.NUM_4X4_BLOCKS_HIGH[frame.miSize[i]] * 4;
            }
        }
        return AvifTables.TX_HEIGHT[frame.interTxSize[row * stride + col - 1]];
    }

    /**
     * Finds the transform size with the given width and height (<code>find_tx_size</code>).
     * <p>
     * 주어진 너비와 높이의 변환 크기를 찾는다(<code>find_tx_size</code>).
     *
     * @param w the width in samples / 샘플 단위 너비
     * @param h the height in samples / 샘플 단위 높이
     * @return the transform size / 변환 크기
     */
    private static int findTxSize(int w, int h) {
        int txSz = 0;
        // 너비와 높이가 같은 변환 크기를 찾는다.
        while (txSz < 19 && (AvifTables.TX_WIDTH[txSz] != w || AvifTables.TX_HEIGHT[txSz] != h)) {
            txSz++;
        }
        return txSz;
    }

    /**
     * Reads the transform size of a block that has a single transform size (<code>read_tx_size</code>).
     * <p>
     * 변환 크기가 하나뿐인 블록의 변환 크기를 읽는다(<code>read_tx_size</code>).
     *
     * @param allowSelect true if the size may be signalled / 크기를 신호할 수 있으면 true
     */
    private void readTxSize(boolean allowSelect) {
        if (lossless) {
            txSize = 0;
            return;
        }
        int maxRect = AvifTables.MAX_TX_SIZE_RECT[miSize];
        int maxDepth = AvifTables.MAX_TX_DEPTH[miSize];
        txSize = maxRect;
        if (miSize > 0 && allowSelect && hdr.txMode == Av1FrameHeader.TX_MODE_SELECT) {
            int maxW = AvifTables.TX_WIDTH[maxRect];
            int maxH = AvifTables.TX_HEIGHT[maxRect];
            int stride = frame.miStride;
            int aboveW = 0;
            if (availU) {
                int i = (miRow - 1) * stride + miCol;
                aboveW = frame.isInter[i] != 0 ? AvifTables.NUM_4X4_BLOCKS_WIDE[frame.miSize[i]] * 4 : aboveTxWidth(miRow, miCol);
            }
            int leftH = 0;
            if (availL) {
                int i = miRow * stride + miCol - 1;
                leftH = frame.isInter[i] != 0 ? AvifTables.NUM_4X4_BLOCKS_HIGH[frame.miSize[i]] * 4 : leftTxHeight(miRow, miCol);
            }
            int ctx = (aboveW >= maxW ? 1 : 0) + (leftH >= maxH ? 1 : 0);
            int depth;
            if (maxDepth == 4) {
                depth = sd.symbol(cdfs.tx64x64, ctx * 4, 3);
            } else if (maxDepth == 3) {
                depth = sd.symbol(cdfs.tx32x32, ctx * 4, 3);
            } else if (maxDepth == 2) {
                depth = sd.symbol(cdfs.tx16x16, ctx * 4, 3);
            } else {
                depth = sd.symbol(cdfs.tx8x8, ctx * 3, 2);
            }
            // 깊이만큼 변환 크기를 줄인다.
            for (int i = 0; i < depth; i++) {
                txSize = SPLIT_TX_SIZE[txSize];
            }
        }
    }

    /**
     * Returns the transform size of a plane of the block (<code>get_tx_size</code>).
     * <p>
     * 블록의 평면별 변환 크기를 반환한다(<code>get_tx_size</code>).
     *
     * @param plane the plane / 평면
     * @return the transform size / 변환 크기
     */
    private int planeTxSize(int plane) {
        if (plane == 0) {
            return txSize;
        }
        int uvTx = AvifTables.MAX_TX_SIZE_RECT[AvifTables.SUBSAMPLED_SIZE[miSize * 4 + seq.subsamplingX * 2 + seq.subsamplingY]];
        if (AvifTables.TX_WIDTH[uvTx] == 64 || AvifTables.TX_HEIGHT[uvTx] == 64) {
            if (AvifTables.TX_WIDTH[uvTx] == 16) {
                return 9;
            }
            if (AvifTables.TX_HEIGHT[uvTx] == 16) {
                return 10;
            }
            return 3;
        }
        return uvTx;
    }

    /**
     * Predicts and reconstructs all transform blocks of the current block (<code>residual</code>).
     * <p>
     * 현재 블록의 모든 변환 블록을 예측하고 복원한다(<code>residual</code>).
     *
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터면 CORRUPT
     */
    private void residual() throws MediaException {
        int widthChunks = Math.max(1, (AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4) >> 6);
        int heightChunks = Math.max(1, (AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4) >> 6);
        int miSizeChunk = (widthChunks > 1 || heightChunks > 1) ? BLOCK_64X64 : miSize;
        // 64x64 조각마다, 평면마다 변환 블록을 순서대로 처리한다.
        for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
            for (int chunkX = 0; chunkX < widthChunks; chunkX++) {
                int miRowChunk = miRow + (chunkY << 4);
                int miColChunk = miCol + (chunkX << 4);
                int planes = hasChroma ? 3 : 1;
                // 휘도(와 색차) 평면을 차례로 처리한다.
                for (int plane = 0; plane < planes; plane++) {
                    int txSz = lossless ? 0 : planeTxSize(plane);
                    int stepX = AvifTables.TX_WIDTH[txSz] >> 2;
                    int stepY = AvifTables.TX_HEIGHT[txSz] >> 2;
                    int subX = plane > 0 ? seq.subsamplingX : 0;
                    int subY = plane > 0 ? seq.subsamplingY : 0;
                    int planeSz = AvifTables.SUBSAMPLED_SIZE[miSizeChunk * 4 + subX * 2 + subY];
                    int num4x4W = AvifTables.NUM_4X4_BLOCKS_WIDE[planeSz];
                    int num4x4H = AvifTables.NUM_4X4_BLOCKS_HIGH[planeSz];
                    int baseXBlock = (miCol >> subX) * 4;
                    int baseYBlock = (miRow >> subY) * 4;
                    // 변환 블록 행을 처리한다.
                    // 인터 블록의 휘도는 변환 크기 트리를 따라, 나머지는 래스터 순서로 변환 블록을 처리한다.
                    if (isInter && !lossless && plane == 0) {
                        transformTree((miColChunk >> subX) * 4, (miRowChunk >> subY) * 4, num4x4W * 4, num4x4H * 4);
                        continue;
                    }
                    for (int y = 0; y < num4x4H; y += stepY) {
                        // 변환 블록 열을 처리한다.
                        for (int x = 0; x < num4x4W; x += stepX) {
                            transformBlock(plane, baseXBlock, baseYBlock, txSz, x + ((chunkX << 4) >> subX), y + ((chunkY << 4) >> subY));
                        }
                    }
                }
            }
        }
    }

    /**
     * Predicts, reads and reconstructs one transform block (<code>transform_block</code>).
     * <p>
     * 변환 블록 하나를 예측하고 읽고 복원한다(<code>transform_block</code>).
     *
     * @param plane  the plane / 평면
     * @param baseX  the x position of the block in samples / 블록의 x 위치(샘플)
     * @param baseY  the y position of the block in samples / 블록의 y 위치(샘플)
     * @param txSz   the transform size / 변환 크기
     * @param x      the x offset in 4x4 units / 4x4 단위 x 오프셋
     * @param y      the y offset in 4x4 units / 4x4 단위 y 오프셋
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터면 CORRUPT
     */
    private void transformBlock(int plane, int baseX, int baseY, int txSz, int x, int y) throws MediaException {
        int startX = baseX + 4 * x;
        int startY = baseY + 4 * y;
        int subX = plane > 0 ? seq.subsamplingX : 0;
        int subY = plane > 0 ? seq.subsamplingY : 0;
        int row = (startY << subY) >> 2;
        int col = (startX << subX) >> 2;
        int sbMask = seq.use128x128Superblock ? 31 : 15;
        int subBlockMiRow = row & sbMask;
        int subBlockMiCol = col & sbMask;
        int stepX = AvifTables.TX_WIDTH[txSz] >> 2;
        int stepY = AvifTables.TX_HEIGHT[txSz] >> 2;
        int maxX = (frame.miCols * 4) >> subX;
        int maxY = (frame.miRows * 4) >> subY;
        if (startX >= maxX || startY >= maxY) {
            return;
        }
        short[] pl = frame.planes[plane];
        int stride = frame.stride[plane];
        boolean[] bd = blockDecoded[plane];
        if (isInter) {
            // 인터 블록의 예측은 블록 전체에 대해 이미 끝났다.
            // (아래 인트라 예측 단계를 건너뛴다.)
        } else if ((plane == 0 && paletteSizeY > 0) || (plane != 0 && paletteSizeUv > 0)) {
            predictPalette(plane, startX, startY, x, y, txSz);
        } else {
            boolean isCfl = plane > 0 && uvMode == UV_CFL_PRED;
            int mode;
            if (plane == 0) {
                mode = yMode;
            } else {
                mode = isCfl ? 0 : uvMode;
            }
            int log2W = AvifTables.TX_WIDTH_LOG2[txSz];
            int log2H = AvifTables.TX_HEIGHT_LOG2[txSz];
            boolean haveLeft = (plane == 0 ? availL : availLChroma) || x > 0;
            boolean haveAbove = (plane == 0 ? availU : availUChroma) || y > 0;
            boolean haveAboveRight = bd[(((subBlockMiRow >> subY) - 1) + 1) * BD_STRIDE + (subBlockMiCol >> subX) + stepX + 1];
            boolean haveBelowLeft = bd[(((subBlockMiRow >> subY) + stepY) + 1) * BD_STRIDE + (subBlockMiCol >> subX) - 1 + 1];
            int angleDelta = plane == 0 ? angleDeltaY : angleDeltaUv;
            int filterType = Av1IntraPred.isDirectional(mode) ? filterType(plane) : 0;
            Av1IntraPred.predict(pl, stride, startX, startY, log2W, log2H, mode, haveLeft, haveAbove, haveAboveRight, haveBelowLeft,
                    maxX - 1, maxY - 1, angleDelta, seq.enableIntraEdgeFilter, filterType, plane == 0 && useFilterIntra, filterIntraMode,
                    seq.bitDepth);
            if (isCfl) {
                predictChromaFromLuma(plane, startX, startY, txSz);
            }
        }
        if (plane == 0 && !isInter) {
            maxLumaW = startX + stepX * 4;
            maxLumaH = startY + stepY * 4;
        }
        if (!skip) {
            int eob = coeff.coeffs(plane, startX, startY, txSz);
            if (eob > 0) {
                reconstruct(plane, startX, startY, txSz);
            }
        }
        // 이 변환 블록이 덮는 위치에 루프 필터용 변환 크기와 디코딩 완료 표시를 기록한다.
        for (int i = 0; i < stepY; i++) {
            for (int j = 0; j < stepX; j++) {
                frame.lfTxSize[plane][((row >> subY) + i) * frame.miStride + (col >> subX) + j] = (byte) txSz;
                bd[(((subBlockMiRow >> subY) + i) + 1) * BD_STRIDE + (subBlockMiCol >> subX) + j + 1] = true;
            }
        }
    }

    /**
     * Reads the transform blocks of the luma plane of an inter block along the transform size tree (<code>transform_tree</code>).
     * <p>
     * 인터 블록 휘도 평면의 변환 블록을 변환 크기 트리를 따라 읽는다(<code>transform_tree</code>).
     *
     * @param startX the x position in samples / 샘플 단위 x 위치
     * @param startY the y position in samples / 샘플 단위 y 위치
     * @param w      the width of the region in samples / 영역의 너비(샘플)
     * @param h      the height of the region in samples / 영역의 높이(샘플)
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터면 CORRUPT
     */
    private void transformTree(int startX, int startY, int w, int h) throws MediaException {
        if (startX >= frame.miCols * 4 || startY >= frame.miRows * 4) {
            return;
        }
        int row = startY >> 2;
        int col = startX >> 2;
        int lumaTxSz = frame.interTxSize[row * frame.miStride + col];
        int lumaW = AvifTables.TX_WIDTH[lumaTxSz];
        int lumaH = AvifTables.TX_HEIGHT[lumaTxSz];
        if (w <= lumaW && h <= lumaH) {
            transformBlock(0, startX, startY, findTxSize(w, h), 0, 0);
        } else if (w > h) {
            transformTree(startX, startY, w / 2, h);
            transformTree(startX + w / 2, startY, w / 2, h);
        } else if (w < h) {
            transformTree(startX, startY, w, h / 2);
            transformTree(startX, startY + h / 2, w, h / 2);
        } else {
            transformTree(startX, startY, w / 2, h / 2);
            transformTree(startX + w / 2, startY, w / 2, h / 2);
            transformTree(startX, startY + h / 2, w / 2, h / 2);
            transformTree(startX + w / 2, startY + h / 2, w / 2, h / 2);
        }
    }

    /**
     * Derives the motion vector of an intra block copy block and reads its difference (<code>assign_mv</code> with use_intrabc).
     * <p>
     * 인트라 블록 복사 블록의 움직임 벡터를 정하고 차이를 읽는다(use_intrabc 일 때의 <code>assign_mv</code>).
     *
     * @throws MediaException CORRUPT if the vector points to a part that is not available / 쓸 수 없는 영역을 가리키면 CORRUPT
     */
    private void assignMvIntrabc() throws MediaException {
        int pr = mvStack.mv[0];
        int pc = mvStack.mv[1];
        // 스택의 첫째, 둘째 후보가 모두 0 이면 슈퍼블록 크기에 따른 기본 벡터를 쓴다.
        if (pr == 0 && pc == 0) {
            pr = mvStack.mv[4];
            pc = mvStack.mv[5];
        }
        if (pr == 0 && pc == 0) {
            int sbSize = seq.use128x128Superblock ? BLOCK_128X128 : BLOCK_64X64;
            int sbSize4 = AvifTables.NUM_4X4_BLOCKS_HIGH[sbSize];
            if (miRow - sbSize4 < miRowStart) {
                pr = 0;
                pc = -(sbSize4 * 4 + 256) * 8;
            } else {
                pr = -(sbSize4 * 4 * 8);
                pc = 0;
            }
        }
        predMv[0] = pr;
        predMv[1] = pc;
        readMv(0);
        if (!isMvValidIntrabc()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: invalid intra block copy vector");
        }
    }

    /**
     * Reads a motion vector difference and adds it to the predicted vector (<code>read_mv</code>).
     * <p>
     * 움직임 벡터 차이를 읽어 예측 벡터에 더한다(<code>read_mv</code>).
     *
     * @param ref the list of the vector / 벡터의 목록
     */
    private void readMv(int ref) {
        int ctx = useIntrabc ? 1 : 0;
        int joint = sd.symbol(cdfs.mvJoint[ctx], 0, 4);
        int dRow = 0;
        int dCol = 0;
        if (joint == 2 || joint == 3) {
            dRow = readMvComponent(ctx, 0);
        }
        if (joint == 1 || joint == 3) {
            dCol = readMvComponent(ctx, 1);
        }
        curMv[ref * 2] = predMv[ref * 2] + dRow;
        curMv[ref * 2 + 1] = predMv[ref * 2 + 1] + dCol;
    }

    /**
     * Reads one component of a motion vector difference (<code>read_mv_component</code>).
     * <p>
     * 움직임 벡터 차이의 성분 하나를 읽는다(<code>read_mv_component</code>).
     *
     * @param ctx  the motion vector context (0 inter, 1 intra block copy) / 움직임 벡터 문맥(0 인터, 1 인트라 블록 복사)
     * @param comp the component (0 row, 1 column) / 성분(0 행, 1 열)
     * @return the signed difference / 부호 있는 차이
     */
    private int readMvComponent(int ctx, int comp) {
        int sign = sd.symbol(cdfs.mvSign[ctx], comp * 3, 2);
        int cls = sd.symbol(cdfs.mvClass[ctx], comp * 12, 11);
        int mag;
        if (cls == 0) {
            int bit = sd.symbol(cdfs.mvClass0Bit[ctx], comp * 3, 2);
            int fr = hdr.forceIntegerMv ? 3 : sd.symbol(cdfs.mvClass0Fr[ctx], (comp * 2 + bit) * 5, 4);
            int hp = hdr.allowHighPrecisionMv ? sd.symbol(cdfs.mvClass0Hp[ctx], comp * 3, 2) : 1;
            mag = ((bit << 3) | (fr << 1) | hp) + 1;
        } else {
            int d = 0;
            // 종류 번호만큼의 정수 비트를 읽는다.
            for (int i = 0; i < cls; i++) {
                d |= sd.symbol(cdfs.mvBit[ctx], (comp * 10 + i) * 3, 2) << i;
            }
            mag = 2 << (cls + 2);
            int fr = hdr.forceIntegerMv ? 3 : sd.symbol(cdfs.mvFr[ctx], comp * 5, 4);
            int hp = hdr.allowHighPrecisionMv ? sd.symbol(cdfs.mvHp[ctx], comp * 3, 2) : 1;
            mag += ((d << 3) | (fr << 1) | hp) + 1;
        }
        return sign != 0 ? -mag : mag;
    }

    /**
     * Checks that the vector of an intra block copy block is valid (<code>is_mv_valid</code> of the specification, the part of
     * intra block copy): an integer vector that copies a part of the tile that was decoded and is not too close to the current block.
     * <p>
     * 인트라 블록 복사 블록의 벡터가 올바른지 확인한다(규격의 <code>is_mv_valid</code> 중 인트라 블록 복사 부분): 이미 디코딩되었고 현재 블록에서
     * 너무 가깝지 않은 타일 영역을 복사하는 정수 벡터여야 한다.
     *
     * @return true if valid / 올바르면 true
     */
    private boolean isMvValidIntrabc() {
        if (Math.abs(curMv[0]) >= (1 << 14) || Math.abs(curMv[1]) >= (1 << 14)) {
            return false;
        }
        int bw = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4;
        int bh = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4;
        if ((curMv[0] & 7) != 0 || (curMv[1] & 7) != 0) {
            return false;
        }
        int deltaRow = curMv[0] >> 3;
        int deltaCol = curMv[1] >> 3;
        int srcTopEdge = miRow * 4 + deltaRow;
        int srcLeftEdge = miCol * 4 + deltaCol;
        int srcBottomEdge = srcTopEdge + bh;
        int srcRightEdge = srcLeftEdge + bw;
        if (hasChroma) {
            if (bw < 8 && seq.subsamplingX != 0) {
                srcLeftEdge -= 4;
            }
            if (bh < 8 && seq.subsamplingY != 0) {
                srcTopEdge -= 4;
            }
        }
        if (srcTopEdge < miRowStart * 4 || srcLeftEdge < miColStart * 4 || srcBottomEdge > miRowEnd * 4 || srcRightEdge > miColEnd * 4) {
            return false;
        }
        int sbSize = seq.use128x128Superblock ? BLOCK_128X128 : BLOCK_64X64;
        int sbH = AvifTables.NUM_4X4_BLOCKS_HIGH[sbSize] * 4;
        int activeSbRow = (miRow * 4) / sbH;
        int activeSb64Col = (miCol * 4) >> 6;
        int srcSbRow = (srcBottomEdge - 1) / sbH;
        int srcSb64Col = (srcRightEdge - 1) >> 6;
        int totalSb64PerRow = ((miColEnd - miColStart - 1) >> 4) + 1;
        int activeSb64 = activeSbRow * totalSb64PerRow + activeSb64Col;
        int srcSb64 = srcSbRow * totalSb64PerRow + srcSb64Col;
        if (srcSb64 >= activeSb64 - 4) {
            return false;
        }
        int gradient = 1 + 4 + (seq.use128x128Superblock ? 1 : 0);
        int wfOffset = gradient * (activeSbRow - srcSbRow);
        return !(srcSbRow > activeSbRow || srcSb64Col >= activeSb64Col - 4 + wfOffset);
    }

    /**
     * Reads the mode info of a block of an inter frame (<code>inter_frame_mode_info</code>).
     * <p>
     * 인터 프레임 블록의 모드 정보를 읽는다(<code>inter_frame_mode_info</code>).
     *
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void interFrameModeInfo() throws MediaException {
        useIntrabc = false;
        int stride = frame.miStride;
        int li = miRow * stride + miCol - 1;
        int ai = (miRow - 1) * stride + miCol;
        leftRef0 = availL ? frame.refFrame0[li] : 0;
        aboveRef0 = availU ? frame.refFrame0[ai] : 0;
        leftRef1 = availL ? frame.refFrame1[li] : -1;
        aboveRef1 = availU ? frame.refFrame1[ai] : -1;
        leftIntra = leftRef0 <= 0;
        aboveIntra = aboveRef0 <= 0;
        leftSingle = leftRef1 <= 0;
        aboveSingle = aboveRef1 <= 0;
        skip = false;
        skipMode = false;
        interSegmentId(true);
        readSkipMode();
        if (skipMode) {
            skip = true;
        } else {
            readSkip();
        }
        if (!hdr.segIdPreSkip) {
            interSegmentId(false);
        }
        lossless = losslessOf(segmentId);
        readCdef();
        readDeltaQIndex();
        readDeltaLf();
        readDeltas = false;
        readIsInter();
        if (isInter) {
            interBlockModeInfo();
        } else {
            intraBlockModeInfo();
        }
    }

    /**
     * Tells if a segment uses the lossless coding.
     * <p>
     * 세그먼트가 무손실 부호화를 쓰는지 알려 준다.
     *
     * @param segment the segment / 세그먼트
     * @return true if lossless / 무손실이면 true
     */
    private boolean losslessOf(int segment) {
        int q = hdr.qindexOfSegment(segment);
        return q == 0 && hdr.deltaQYDc == 0 && hdr.deltaQUAc == 0 && hdr.deltaQUDc == 0 && hdr.deltaQVAc == 0 && hdr.deltaQVDc == 0;
    }

    /**
     * Reads or derives the segment id of a block of an inter frame (<code>inter_segment_id</code>).
     * <p>
     * 인터 프레임 블록의 세그먼트 id 를 읽거나 구한다(<code>inter_segment_id</code>).
     *
     * @param preSkip true before the skip flag was read, false after it / skip 플래그를 읽기 전이면 true, 읽은 뒤면 false
     */
    private void interSegmentId(boolean preSkip) {
        if (!hdr.segmentationEnabled) {
            segmentId = 0;
            return;
        }
        int predicted = predictedSegmentId();
        if (!hdr.segmentationUpdateMap) {
            segmentId = predicted;
            return;
        }
        if (preSkip && !hdr.segIdPreSkip) {
            segmentId = 0;
            return;
        }
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        if (!preSkip && skip) {
            // skip 이면 세그먼트 id 를 예측값으로 정하고 예측 문맥을 0 으로 둔다.
            for (int i = 0; i < bw4; i++) {
                aboveSegPred[miCol + i] = 0;
            }
            // 블록이 덮는 행마다 왼쪽 예측 문맥을 0 으로 둔다.
            for (int i = 0; i < bh4; i++) {
                leftSegPred[miRow + i] = 0;
            }
            readSegmentId();
            return;
        }
        if (hdr.segmentationTemporalUpdate) {
            int ctx = leftSegPred[miRow] + aboveSegPred[miCol];
            boolean predictedFlag = sd.symbol(cdfs.segmentIdPredicted, ctx * 3, 2) != 0;
            if (predictedFlag) {
                segmentId = predicted;
            } else {
                readSegmentId();
            }
            // 예측 문맥을 블록이 덮는 위치에 기록한다.
            for (int i = 0; i < bw4; i++) {
                aboveSegPred[miCol + i] = (byte) (predictedFlag ? 1 : 0);
            }
            // 블록이 덮는 행마다 왼쪽 예측 문맥을 기록한다.
            for (int i = 0; i < bh4; i++) {
                leftSegPred[miRow + i] = (byte) (predictedFlag ? 1 : 0);
            }
        } else {
            readSegmentId();
        }
    }

    /**
     * Returns the smallest segment id of the previous frame's segmentation map inside the block (<code>get_segment_id</code>).
     * <p>
     * 블록 안에서 이전 프레임 분할 맵의 가장 작은 세그먼트 id 를 반환한다(<code>get_segment_id</code>).
     *
     * @return the predicted segment id / 예측한 세그먼트 id
     */
    private int predictedSegmentId() {
        int xMis = Math.min(frame.miCols - miCol, AvifTables.NUM_4X4_BLOCKS_WIDE[miSize]);
        int yMis = Math.min(frame.miRows - miRow, AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]);
        int seg = 7;
        if (frame.prevSegmentIds == null) {
            return 0;
        }
        // 블록이 덮는 화면 안의 위치에서 가장 작은 값을 찾는다.
        for (int y = 0; y < yMis; y++) {
            for (int x = 0; x < xMis; x++) {
                seg = Math.min(seg, frame.prevSegmentIds[(miRow + y) * frame.miStride + miCol + x]);
            }
        }
        return seg;
    }

    /**
     * Reads the flag that tells that the block uses the skip mode (<code>read_skip_mode</code>).
     * <p>
     * 블록이 건너뛰기 모드를 쓰는지 알리는 플래그를 읽는다(<code>read_skip_mode</code>).
     */
    private void readSkipMode() {
        int bw = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4;
        int bh = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4;
        if (segFeatureActive(6) || segFeatureActive(5) || segFeatureActive(7) || !hdr.skipModePresent || bw < 8 || bh < 8) {
            skipMode = false;
            return;
        }
        int ctx = 0;
        int stride = frame.miStride;
        if (availU) {
            ctx += frame.skipMode[(miRow - 1) * stride + miCol];
        }
        if (availL) {
            ctx += frame.skipMode[miRow * stride + miCol - 1];
        }
        skipMode = sd.symbol(cdfs.skipMode, ctx * 3, 2) != 0;
    }

    /**
     * Reads whether the block is an inter block (<code>read_is_inter</code>).
     * <p>
     * 블록이 인터 블록인지 읽는다(<code>read_is_inter</code>).
     */
    private void readIsInter() {
        if (skipMode) {
            isInter = true;
        } else if (segFeatureActive(5)) {
            isInter = hdr.featureData[segmentId][5] != 0;
        } else if (segFeatureActive(7)) {
            isInter = true;
        } else {
            int ctx;
            if (availU && availL) {
                ctx = (leftIntra && aboveIntra) ? 3 : ((leftIntra || aboveIntra) ? 1 : 0);
            } else if (availU || availL) {
                ctx = 2 * ((availU ? aboveIntra : leftIntra) ? 1 : 0);
            } else {
                ctx = 0;
            }
            isInter = sd.symbol(cdfs.isInter, ctx * 3, 2) != 0;
        }
    }

    /**
     * Reads the modes of an intra block of an inter frame (<code>intra_block_mode_info</code>).
     * <p>
     * 인터 프레임 안 인트라 블록의 모드를 읽는다(<code>intra_block_mode_info</code>).
     */
    private void intraBlockModeInfo() {
        refFrame0 = 0;
        refFrame1 = -1;
        useIntrabc = false;
        yMode = sd.symbol(cdfs.yMode, AvifTables.SIZE_GROUP[miSize] * 14, 13);
        readIntraModes();
    }

    /**
     * Reads the modes of an inter block (<code>inter_block_mode_info</code>).
     * <p>
     * 인터 블록의 모드를 읽는다(<code>inter_block_mode_info</code>).
     *
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void interBlockModeInfo() throws MediaException {
        paletteSizeY = 0;
        paletteSizeUv = 0;
        useFilterIntra = false;
        angleDeltaY = 0;
        angleDeltaUv = 0;
        uvMode = 0;
        cflAlphaU = 0;
        cflAlphaV = 0;
        readRefFrames();
        boolean isCompound = refFrame1 > 0;
        mvStack.find(miRow, miCol, miSize, refFrame0, refFrame1);
        if (skipMode) {
            yMode = Av1MvStack.NEAREST_NEARESTMV;
        } else if (segFeatureActive(6) || segFeatureActive(7)) {
            yMode = Av1MvStack.GLOBALMV;
        } else if (isCompound) {
            int ctx = AvifInterTables.COMPOUND_MODE_CTX_MAP[(mvStack.refMvContext >> 1) * 5 + Math.min(mvStack.newMvContext, 4)];
            yMode = Av1MvStack.NEAREST_NEARESTMV + sd.symbol(cdfs.compoundMode, ctx * 9, 8);
        } else if (sd.symbol(cdfs.newMv, mvStack.newMvContext * 3, 2) == 0) {
            yMode = Av1MvStack.NEWMV;
        } else if (sd.symbol(cdfs.zeroMv, mvStack.zeroMvContext * 3, 2) == 0) {
            yMode = Av1MvStack.GLOBALMV;
        } else {
            yMode = sd.symbol(cdfs.refMv, mvStack.refMvContext * 3, 2) == 0 ? Av1MvStack.NEARESTMV : Av1MvStack.NEARMV;
        }
        refMvIdx = 0;
        if (yMode == Av1MvStack.NEWMV || yMode == Av1MvStack.NEW_NEWMV) {
            // 후보가 있으면 drl_mode 로 후보 번호를 최대 2번 늘린다.
            for (int idx = 0; idx < 2; idx++) {
                if (mvStack.numMvFound > idx + 1) {
                    if (sd.symbol(cdfs.drlMode, mvStack.drlCtxStack[idx] * 3, 2) == 0) {
                        refMvIdx = idx;
                        break;
                    }
                    refMvIdx = idx + 1;
                }
            }
        } else if (hasNearMv()) {
            refMvIdx = 1;
            // NEAR 모드는 둘째 후보부터 시작해 후보 번호를 늘린다.
            for (int idx = 1; idx < 3; idx++) {
                if (mvStack.numMvFound > idx + 1) {
                    if (sd.symbol(cdfs.drlMode, mvStack.drlCtxStack[idx] * 3, 2) == 0) {
                        refMvIdx = idx;
                        break;
                    }
                    refMvIdx = idx + 1;
                }
            }
        }
        assignMv(isCompound);
        readInterintraMode(isCompound);
        readMotionMode(isCompound);
        readCompoundType(isCompound);
        if (hdr.interpolationFilter == Av1FrameHeader.SWITCHABLE) {
            int dirs = seq.enableDualFilter ? 2 : 1;
            int[] filters = new int[2];
            // 방향마다 보간 필터를 읽는다(이중 필터가 아니면 한 방향만 읽는다).
            for (int dir = 0; dir < dirs; dir++) {
                filters[dir] = needsInterpFilter() ? sd.symbol(cdfs.interpFilter, interpFilterContext(dir) * 4, 3) : 0;
            }
            if (!seq.enableDualFilter) {
                filters[1] = filters[0];
            }
            interpFilter0 = filters[0];
            interpFilter1 = filters[1];
        } else {
            interpFilter0 = hdr.interpolationFilter;
            interpFilter1 = hdr.interpolationFilter;
        }
    }

    /**
     * True if the mode uses the NEAR candidate (<code>has_nearmv</code>).
     * <p>
     * 모드가 NEAR 후보를 쓰면 true(<code>has_nearmv</code>).
     *
     * @return true if the mode uses the NEAR candidate / NEAR 후보를 쓰면 true
     */
    private boolean hasNearMv() {
        return yMode == Av1MvStack.NEARMV || yMode == Av1MvStack.NEAR_NEARMV || yMode == Av1MvStack.NEAR_NEWMV || yMode == Av1MvStack.NEW_NEARMV;
    }

    /**
     * True if the interpolation filter of the block is signalled (<code>needs_interp_filter</code>).
     * <p>
     * 블록의 보간 필터를 신호하면 true(<code>needs_interp_filter</code>).
     *
     * @return true if the filter is read / 필터를 읽으면 true
     */
    private boolean needsInterpFilter() {
        boolean large = Math.min(AvifTables.NUM_4X4_BLOCKS_WIDE[miSize], AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]) >= 2;
        if (skipMode || motionMode == 2) {
            return false;
        } else if (large && yMode == Av1MvStack.GLOBALMV) {
            return hdr.gmType[refFrame0] == Av1MvStack.TRANSLATION;
        } else if (large && yMode == Av1MvStack.GLOBAL_GLOBALMV) {
            return hdr.gmType[refFrame0] == Av1MvStack.TRANSLATION || hdr.gmType[refFrame1] == Av1MvStack.TRANSLATION;
        }
        return true;
    }

    /**
     * Computes the context of the syntax element interp_filter.
     * <p>
     * 구문 요소 interp_filter 의 문맥을 계산한다.
     *
     * @param dir the direction (0 vertical, 1 horizontal) / 방향(0 세로, 1 가로)
     * @return the context / 문맥
     */
    private int interpFilterContext(int dir) {
        int ctx = ((dir & 1) * 2 + (refFrame1 > 0 ? 1 : 0)) * 4;
        int leftType = 3;
        int aboveType = 3;
        int stride = frame.miStride;
        if (availL) {
            int i = miRow * stride + miCol - 1;
            if (frame.refFrame0[i] == refFrame0 || frame.refFrame1[i] == refFrame0) {
                leftType = dir == 0 ? frame.interpFilter0[i] : frame.interpFilter1[i];
            }
        }
        if (availU) {
            int i = (miRow - 1) * stride + miCol;
            if (frame.refFrame0[i] == refFrame0 || frame.refFrame1[i] == refFrame0) {
                aboveType = dir == 0 ? frame.interpFilter0[i] : frame.interpFilter1[i];
            }
        }
        if (leftType == aboveType) {
            ctx += leftType;
        } else if (leftType == 3) {
            ctx += aboveType;
        } else if (aboveType == 3) {
            ctx += leftType;
        } else {
            ctx += 3;
        }
        return ctx;
    }

    /**
     * Counts how many of the neighbours above and to the left use a reference frame (<code>count_refs</code>).
     * <p>
     * 위쪽과 왼쪽 이웃 중 몇 개가 참조 프레임을 쓰는지 센다(<code>count_refs</code>).
     *
     * @param frameType the reference frame / 참조 프레임
     * @return the count / 개수
     */
    private int countRefs(int frameType) {
        int c = 0;
        if (availU) {
            if (aboveRef0 == frameType) {
                c++;
            }
            if (aboveRef1 == frameType) {
                c++;
            }
        }
        if (availL) {
            if (leftRef0 == frameType) {
                c++;
            }
            if (leftRef1 == frameType) {
                c++;
            }
        }
        return c;
    }

    /**
     * The context from two counts (<code>ref_count_ctx</code>).
     * <p>
     * 두 개수로 정한 문맥(<code>ref_count_ctx</code>).
     *
     * @param c0 the first count / 첫 개수
     * @param c1 the second count / 둘째 개수
     * @return the context / 문맥
     */
    private static int refCountCtx(int c0, int c1) {
        return c0 < c1 ? 0 : (c0 == c1 ? 1 : 2);
    }

    /**
     * Tells if a reference frame is a backward one (<code>check_backward</code>).
     * <p>
     * 참조 프레임이 후방 참조인지 알려 준다(<code>check_backward</code>).
     *
     * @param refFrame the reference frame / 참조 프레임
     * @return true for BWDREF, ALTREF2 and ALTREF / BWDREF, ALTREF2, ALTREF 이면 true
     */
    private static boolean checkBackward(int refFrame) {
        return refFrame >= 5 && refFrame <= 7;
    }

    /**
     * Reads the reference frames of an inter block (<code>read_ref_frames</code>).
     * <p>
     * 인터 블록의 참조 프레임을 읽는다(<code>read_ref_frames</code>).
     */
    private void readRefFrames() {
        if (skipMode) {
            refFrame0 = hdr.skipModeFrame[0];
            refFrame1 = hdr.skipModeFrame[1];
            return;
        }
        if (segFeatureActive(5)) {
            refFrame0 = hdr.featureData[segmentId][5];
            refFrame1 = -1;
            return;
        }
        if (segFeatureActive(6) || segFeatureActive(7)) {
            refFrame0 = 1;
            refFrame1 = -1;
            return;
        }
        int bw4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int bh4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        boolean compound = false;
        if (hdr.referenceSelect && Math.min(bw4, bh4) >= 2) {
            compound = sd.symbol(cdfs.compMode, compModeContext() * 3, 2) != 0;
        }
        if (compound) {
            int compRefType = sd.symbol(cdfs.compRefType, compRefTypeContext() * 3, 2);
            if (compRefType == 0) {
                // 같은 방향의 두 참조 프레임을 읽는다.
                if (sd.symbol(cdfs.uniCompRef, (singleRefP1Context() * 3 + 0) * 3, 2) != 0) {
                    refFrame0 = 5;
                    refFrame1 = 7;
                } else {
                    int ctx = refCountCtx(countRefs(2), countRefs(3) + countRefs(4));
                    if (sd.symbol(cdfs.uniCompRef, (ctx * 3 + 1) * 3, 2) != 0) {
                        int ctx2 = refCountCtx(countRefs(3), countRefs(4));
                        refFrame0 = 1;
                        refFrame1 = sd.symbol(cdfs.uniCompRef, (ctx2 * 3 + 2) * 3, 2) != 0 ? 4 : 3;
                    } else {
                        refFrame0 = 1;
                        refFrame1 = 2;
                    }
                }
            } else {
                // 전방 참조 하나와 후방 참조 하나를 읽는다.
                int ctx = refCountCtx(countRefs(1) + countRefs(2), countRefs(3) + countRefs(4));
                if (sd.symbol(cdfs.compRef, (ctx * 3 + 0) * 3, 2) == 0) {
                    int ctx1 = refCountCtx(countRefs(1), countRefs(2));
                    refFrame0 = sd.symbol(cdfs.compRef, (ctx1 * 3 + 1) * 3, 2) != 0 ? 2 : 1;
                } else {
                    int ctx2 = refCountCtx(countRefs(3), countRefs(4));
                    refFrame0 = sd.symbol(cdfs.compRef, (ctx2 * 3 + 2) * 3, 2) != 0 ? 4 : 3;
                }
                int bctx = refCountCtx(countRefs(5) + countRefs(6), countRefs(7));
                if (sd.symbol(cdfs.compBwdRef, (bctx * 2 + 0) * 3, 2) == 0) {
                    int bctx1 = refCountCtx(countRefs(5), countRefs(6));
                    refFrame1 = sd.symbol(cdfs.compBwdRef, (bctx1 * 2 + 1) * 3, 2) != 0 ? 6 : 5;
                } else {
                    refFrame1 = 7;
                }
            }
            return;
        }
        // 단일 참조: 전방/후방 그룹부터 차례로 좁혀 간다.
        if (sd.symbol(cdfs.singleRef, (singleRefP1Context() * 6 + 0) * 3, 2) != 0) {
            int ctx2 = refCountCtx(countRefs(5) + countRefs(6), countRefs(7));
            if (sd.symbol(cdfs.singleRef, (ctx2 * 6 + 1) * 3, 2) == 0) {
                int ctx6 = refCountCtx(countRefs(5), countRefs(6));
                refFrame0 = sd.symbol(cdfs.singleRef, (ctx6 * 6 + 5) * 3, 2) != 0 ? 6 : 5;
            } else {
                refFrame0 = 7;
            }
        } else {
            int ctx3 = refCountCtx(countRefs(1) + countRefs(2), countRefs(3) + countRefs(4));
            if (sd.symbol(cdfs.singleRef, (ctx3 * 6 + 2) * 3, 2) != 0) {
                int ctx5 = refCountCtx(countRefs(3), countRefs(4));
                refFrame0 = sd.symbol(cdfs.singleRef, (ctx5 * 6 + 4) * 3, 2) != 0 ? 4 : 3;
            } else {
                int ctx4 = refCountCtx(countRefs(1), countRefs(2));
                refFrame0 = sd.symbol(cdfs.singleRef, (ctx4 * 6 + 3) * 3, 2) != 0 ? 2 : 1;
            }
        }
        refFrame1 = -1;
    }

    /**
     * The context of single_ref_p1 and uni_comp_ref (forward against backward references).
     * <p>
     * single_ref_p1 과 uni_comp_ref 의 문맥(전방 참조와 후방 참조 비교).
     *
     * @return the context / 문맥
     */
    private int singleRefP1Context() {
        int fwd = countRefs(1) + countRefs(2) + countRefs(3) + countRefs(4);
        int bwd = countRefs(5) + countRefs(6) + countRefs(7);
        return refCountCtx(fwd, bwd);
    }

    /**
     * The context of the syntax element comp_mode.
     * <p>
     * 구문 요소 comp_mode 의 문맥.
     *
     * @return the context / 문맥
     */
    private int compModeContext() {
        if (availU && availL) {
            if (aboveSingle && leftSingle) {
                return (checkBackward(aboveRef0) ? 1 : 0) ^ (checkBackward(leftRef0) ? 1 : 0);
            } else if (aboveSingle) {
                return 2 + ((checkBackward(aboveRef0) || aboveIntra) ? 1 : 0);
            } else if (leftSingle) {
                return 2 + ((checkBackward(leftRef0) || leftIntra) ? 1 : 0);
            }
            return 4;
        } else if (availU) {
            return aboveSingle ? (checkBackward(aboveRef0) ? 1 : 0) : 3;
        } else if (availL) {
            return leftSingle ? (checkBackward(leftRef0) ? 1 : 0) : 3;
        }
        return 1;
    }

    /**
     * The context of the syntax element comp_ref_type.
     * <p>
     * 구문 요소 comp_ref_type 의 문맥.
     *
     * @return the context / 문맥
     */
    private int compRefTypeContext() {
        boolean aboveCompInter = availU && !aboveIntra && !aboveSingle;
        boolean leftCompInter = availL && !leftIntra && !leftSingle;
        boolean aboveUniComp = aboveCompInter && sameDirRefPair(aboveRef0, aboveRef1);
        boolean leftUniComp = leftCompInter && sameDirRefPair(leftRef0, leftRef1);
        if (availU && !aboveIntra && availL && !leftIntra) {
            int samedir = sameDirRefPair(aboveRef0, leftRef0) ? 1 : 0;
            if (!aboveCompInter && !leftCompInter) {
                return 1 + 2 * samedir;
            } else if (!aboveCompInter) {
                return !leftUniComp ? 1 : 3 + samedir;
            } else if (!leftCompInter) {
                return !aboveUniComp ? 1 : 3 + samedir;
            } else if (!aboveUniComp && !leftUniComp) {
                return 0;
            } else if (!aboveUniComp || !leftUniComp) {
                return 2;
            }
            return 3 + (((aboveRef0 == 5) == (leftRef0 == 5)) ? 1 : 0);
        } else if (availU && availL) {
            if (aboveCompInter) {
                return 1 + 2 * (aboveUniComp ? 1 : 0);
            } else if (leftCompInter) {
                return 1 + 2 * (leftUniComp ? 1 : 0);
            }
            return 2;
        } else if (aboveCompInter) {
            return 4 * (aboveUniComp ? 1 : 0);
        } else if (leftCompInter) {
            return 4 * (leftUniComp ? 1 : 0);
        }
        return 2;
    }

    /**
     * Tells if two reference frames are in the same group (both forward or both backward).
     * <p>
     * 두 참조 프레임이 같은 그룹(둘 다 전방 또는 둘 다 후방)인지 알려 준다.
     *
     * @param ref0 the first reference frame / 첫 참조 프레임
     * @param ref1 the second reference frame / 둘째 참조 프레임
     * @return true if in the same group / 같은 그룹이면 true
     */
    private static boolean sameDirRefPair(int ref0, int ref1) {
        return (ref0 >= 5) == (ref1 >= 5);
    }

    /**
     * Derives the motion vectors of an inter block and reads their differences (<code>assign_mv</code>).
     * <p>
     * 인터 블록의 움직임 벡터를 정하고 차이를 읽는다(<code>assign_mv</code>).
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void assignMv(boolean isCompound) {
        // 예측 목록마다 모드에 따라 예측 벡터를 정하고 NEW 모드이면 차이를 읽는다.
        for (int i = 0; i < (isCompound ? 2 : 1); i++) {
            int compMode = modeOfList(i);
            if (compMode == Av1MvStack.GLOBALMV) {
                predMv[i * 2] = mvStack.globalMv[i * 2];
                predMv[i * 2 + 1] = mvStack.globalMv[i * 2 + 1];
            } else {
                int pos = compMode == Av1MvStack.NEARESTMV ? 0 : refMvIdx;
                if (compMode == Av1MvStack.NEWMV && mvStack.numMvFound <= 1) {
                    pos = 0;
                }
                predMv[i * 2] = mvStack.mv[(pos * 2 + i) * 2];
                predMv[i * 2 + 1] = mvStack.mv[(pos * 2 + i) * 2 + 1];
            }
            if (compMode == Av1MvStack.NEWMV) {
                readMv(i);
            } else {
                curMv[i * 2] = predMv[i * 2];
                curMv[i * 2 + 1] = predMv[i * 2 + 1];
            }
        }
    }

    /**
     * Returns the single prediction mode that a list of the block uses (<code>get_mode</code>).
     * <p>
     * 블록의 목록 하나가 쓰는 단일 예측 모드를 반환한다(<code>get_mode</code>).
     *
     * @param refList the list 0 or 1 / 목록 0 또는 1
     * @return the mode / 모드
     */
    private int modeOfList(int refList) {
        int m = yMode;
        if (refList == 0) {
            if (m < Av1MvStack.NEAREST_NEARESTMV) {
                return m;
            } else if (m == Av1MvStack.NEW_NEWMV || m == Av1MvStack.NEW_NEARESTMV || m == Av1MvStack.NEW_NEARMV) {
                return Av1MvStack.NEWMV;
            } else if (m == Av1MvStack.NEAREST_NEARESTMV || m == Av1MvStack.NEAREST_NEWMV) {
                return Av1MvStack.NEARESTMV;
            } else if (m == Av1MvStack.NEAR_NEARMV || m == Av1MvStack.NEAR_NEWMV) {
                return Av1MvStack.NEARMV;
            }
            return Av1MvStack.GLOBALMV;
        }
        if (m == Av1MvStack.NEW_NEWMV || m == Av1MvStack.NEAREST_NEWMV || m == Av1MvStack.NEAR_NEWMV) {
            return Av1MvStack.NEWMV;
        } else if (m == Av1MvStack.NEAREST_NEARESTMV || m == Av1MvStack.NEW_NEARESTMV) {
            return Av1MvStack.NEARESTMV;
        } else if (m == Av1MvStack.NEAR_NEARMV || m == Av1MvStack.NEW_NEARMV) {
            return Av1MvStack.NEARMV;
        }
        return Av1MvStack.GLOBALMV;
    }

    /**
     * Reads whether and how the inter prediction is mixed with an intra prediction (<code>read_interintra_mode</code>).
     * <p>
     * 인터 예측을 인트라 예측과 섞는지와 방법을 읽는다(<code>read_interintra_mode</code>).
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void readInterintraMode(boolean isCompound) {
        interintra = false;
        wedgeInterintra = false;
        if (!skipMode && seq.enableInterintraCompound && !isCompound && miSize >= BLOCK_8X8 && miSize <= 9) {
            int ctx = AvifTables.SIZE_GROUP[miSize] - 1;
            interintra = sd.symbol(cdfs.interIntra, ctx * 3, 2) != 0;
            if (interintra) {
                interintraMode = sd.symbol(cdfs.interIntraMode, ctx * 5, 4);
                refFrame1 = 0;
                angleDeltaY = 0;
                angleDeltaUv = 0;
                useFilterIntra = false;
                wedgeInterintra = sd.symbol(cdfs.wedgeInterIntra, miSize * 3, 2) != 0;
                if (wedgeInterintra) {
                    wedgeIndex = sd.symbol(cdfs.wedgeIndex, miSize * 17, 16);
                    wedgeSign = 0;
                }
            }
        }
    }

    /**
     * Reads the motion mode of the block: translation, overlapped or local warp (<code>read_motion_mode</code>).
     * <p>
     * 블록의 움직임 모드(이동, 겹침, 국소 워프)를 읽는다(<code>read_motion_mode</code>).
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void readMotionMode(boolean isCompound) {
        motionMode = 0;
        numSamples = 0;
        numSamplesScanned = 0;
        if (skipMode || !hdr.isMotionModeSwitchable) {
            return;
        }
        if (Math.min(AvifTables.NUM_4X4_BLOCKS_WIDE[miSize], AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]) * 4 < 8) {
            return;
        }
        if (!hdr.forceIntegerMv && (yMode == Av1MvStack.GLOBALMV || yMode == Av1MvStack.GLOBAL_GLOBALMV)) {
            if (hdr.gmType[refFrame0] > Av1MvStack.TRANSLATION) {
                return;
            }
        }
        if (isCompound || refFrame1 == 0 || !hasOverlappableCandidates()) {
            return;
        }
        findWarpSamples();
        if (hdr.forceIntegerMv || numSamples == 0 || !hdr.allowWarpedMotion || isScaled(refFrame0)) {
            motionMode = sd.symbol(cdfs.useObmc, miSize * 3, 2);
        } else {
            motionMode = sd.symbol(cdfs.motionMode, miSize * 4, 3);
        }
    }

    /**
     * Tells if a reference frame has another size than the current frame (<code>is_scaled</code>).
     * <p>
     * 참조 프레임의 크기가 현재 프레임과 다른지 알려 준다(<code>is_scaled</code>).
     *
     * @param refFrame the reference frame (1..7) / 참조 프레임(1..7)
     * @return true if the reference is scaled / 참조가 스케일되면 true
     */
    boolean isScaled(int refFrame) {
        Av1RefFrame ref = frame.refList[refFrame];
        int xScale = ((ref.upscaledWidth << 14) + (hdr.frameWidth / 2)) / hdr.frameWidth;
        int yScale = ((ref.frameHeight << 14) + (hdr.frameHeight / 2)) / hdr.frameHeight;
        return xScale != (1 << 14) || yScale != (1 << 14);
    }

    /**
     * Tells if a neighbour above or to the left is an inter block (<code>has_overlappable_candidates</code>).
     * <p>
     * 위쪽이나 왼쪽 이웃에 인터 블록이 있는지 알려 준다(<code>has_overlappable_candidates</code>).
     *
     * @return true if overlapped prediction is possible / 겹침 예측이 가능하면 true
     */
    private boolean hasOverlappableCandidates() {
        int stride = frame.miStride;
        if (availU) {
            int w4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
            // 위쪽 행을 8x8 간격으로 훑는다.
            for (int x4 = miCol; x4 < Math.min(frame.miCols, miCol + w4); x4 += 2) {
                if (frame.refFrame0[(miRow - 1) * stride + (x4 | 1)] > 0) {
                    return true;
                }
            }
        }
        if (availL) {
            int h4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
            // 왼쪽 열을 8x8 간격으로 훑는다.
            for (int y4 = miRow; y4 < Math.min(frame.miRows, miRow + h4); y4 += 2) {
                if (frame.refFrame0[(y4 | 1) * stride + miCol - 1] > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Collects the motion vectors of the neighbours for the estimation of a local warp (<code>find_warp_samples</code>).
     * <p>
     * 국소 워프 추정에 쓸 이웃의 움직임 벡터를 모은다(<code>find_warp_samples</code>).
     */
    private void findWarpSamples() {
        numSamples = 0;
        numSamplesScanned = 0;
        int w4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
        int h4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
        int stride = frame.miStride;
        boolean doTopLeft = true;
        boolean doTopRight = true;
        if (availU) {
            int srcSize = frame.miSize[(miRow - 1) * stride + miCol];
            int srcW = AvifTables.NUM_4X4_BLOCKS_WIDE[srcSize];
            if (w4 <= srcW) {
                int colOffset = -(miCol & (srcW - 1));
                if (colOffset < 0) {
                    doTopLeft = false;
                }
                if (colOffset + srcW > w4) {
                    doTopRight = false;
                }
                addSample(-1, 0);
            } else {
                int miStep;
                // 위쪽 이웃이 현재 블록보다 좁으면 이웃마다 표본을 더한다.
                for (int i = 0; i < Math.min(w4, frame.miCols - miCol); i += miStep) {
                    srcSize = frame.miSize[(miRow - 1) * stride + miCol + i];
                    srcW = AvifTables.NUM_4X4_BLOCKS_WIDE[srcSize];
                    miStep = Math.min(w4, srcW);
                    addSample(-1, i);
                }
            }
        }
        if (availL) {
            int srcSize = frame.miSize[miRow * stride + miCol - 1];
            int srcH = AvifTables.NUM_4X4_BLOCKS_HIGH[srcSize];
            if (h4 <= srcH) {
                int rowOffset = -(miRow & (srcH - 1));
                if (rowOffset < 0) {
                    doTopLeft = false;
                }
                addSample(0, -1);
            } else {
                int miStep;
                // 왼쪽 이웃이 현재 블록보다 낮으면 이웃마다 표본을 더한다.
                for (int i = 0; i < Math.min(h4, frame.miRows - miRow); i += miStep) {
                    srcSize = frame.miSize[(miRow + i) * stride + miCol - 1];
                    srcH = AvifTables.NUM_4X4_BLOCKS_HIGH[srcSize];
                    miStep = Math.min(h4, srcH);
                    addSample(i, -1);
                }
            }
        }
        if (doTopLeft) {
            addSample(-1, -1);
        }
        if (doTopRight && Math.max(w4, h4) <= 16) {
            addSample(-1, w4);
        }
        if (numSamples == 0 && numSamplesScanned > 0) {
            numSamples = 1;
        }
    }

    /**
     * Adds a sample for the warp estimation if the neighbour is a valid candidate (the add sample process).
     * <p>
     * 이웃이 올바른 후보이면 워프 추정용 표본을 더한다(표본 추가 과정).
     *
     * @param deltaRow the row offset (4x4 units) / 행 오프셋(4x4 단위)
     * @param deltaCol the column offset / 열 오프셋
     */
    private void addSample(int deltaRow, int deltaCol) {
        if (numSamplesScanned >= 8) {
            return;
        }
        int mvRow = miRow + deltaRow;
        int mvCol = miCol + deltaCol;
        if (!isInside(mvRow, mvCol)) {
            return;
        }
        int stride = frame.miStride;
        int i = mvRow * stride + mvCol;
        if (frame.refFrame0[i] == Av1Frame.NOT_WRITTEN || frame.refFrame0[i] != refFrame0 || frame.refFrame1[i] != -1) {
            return;
        }
        int candSz = frame.miSize[i];
        int candW4 = AvifTables.NUM_4X4_BLOCKS_WIDE[candSz];
        int candH4 = AvifTables.NUM_4X4_BLOCKS_HIGH[candSz];
        int candRow = mvRow & ~(candH4 - 1);
        int candCol = mvCol & ~(candW4 - 1);
        int midY = candRow * 4 + candH4 * 2 - 1;
        int midX = candCol * 4 + candW4 * 2 - 1;
        int threshold = Math.max(16, Math.min(112, Math.max(AvifTables.NUM_4X4_BLOCKS_WIDE[miSize], AvifTables.NUM_4X4_BLOCKS_HIGH[miSize]) * 4));
        int ci = candRow * stride + candCol;
        int cmr = frame.mvs[ci * 4];
        int cmc = frame.mvs[ci * 4 + 1];
        int mvDiffRow = Math.abs(cmr - curMv[0]);
        int mvDiffCol = Math.abs(cmc - curMv[1]);
        boolean valid = (mvDiffRow + mvDiffCol) <= threshold;
        numSamplesScanned++;
        if (!valid && numSamplesScanned > 1) {
            return;
        }
        candList[numSamples * 4] = midY * 8;
        candList[numSamples * 4 + 1] = midX * 8;
        candList[numSamples * 4 + 2] = midY * 8 + cmr;
        candList[numSamples * 4 + 3] = midX * 8 + cmc;
        if (valid) {
            numSamples++;
        }
    }

    /**
     * Reads how the two predictions of a compound block are blended (<code>read_compound_type</code>).
     * <p>
     * 복합 블록의 두 예측을 어떻게 섞는지 읽는다(<code>read_compound_type</code>).
     *
     * @param isCompound true for compound prediction / 복합 예측이면 true
     */
    private void readCompoundType(boolean isCompound) {
        compGroupIdx = 0;
        compoundIdx = 1;
        wedgeSign = 0;
        maskType = 0;
        if (skipMode) {
            compoundType = COMPOUND_AVERAGE;
            return;
        }
        if (isCompound) {
            int n = AvifInterTables.WEDGE_BITS[miSize];
            if (seq.enableMaskedCompound) {
                compGroupIdx = sd.symbol(cdfs.compGroupIdx, compGroupIdxContext() * 3, 2);
            }
            if (compGroupIdx == 0) {
                if (seq.enableJntComp) {
                    compoundIdx = sd.symbol(cdfs.compoundIdx, compoundIdxContext() * 3, 2);
                    compoundType = compoundIdx != 0 ? COMPOUND_AVERAGE : COMPOUND_DISTANCE;
                } else {
                    compoundType = COMPOUND_AVERAGE;
                }
            } else if (n == 0) {
                compoundType = COMPOUND_DIFFWTD;
            } else {
                compoundType = sd.symbol(cdfs.compoundType, miSize * 3, 2);
            }
            if (compoundType == COMPOUND_WEDGE) {
                wedgeIndex = sd.symbol(cdfs.wedgeIndex, miSize * 17, 16);
                wedgeSign = sd.literal(1);
            } else if (compoundType == COMPOUND_DIFFWTD) {
                maskType = sd.literal(1);
            }
        } else if (interintra) {
            compoundType = wedgeInterintra ? COMPOUND_WEDGE : COMPOUND_INTRA;
        } else {
            compoundType = COMPOUND_AVERAGE;
        }
    }

    /**
     * The context of the syntax element comp_group_idx.
     * <p>
     * 구문 요소 comp_group_idx 의 문맥.
     *
     * @return the context / 문맥
     */
    private int compGroupIdxContext() {
        int ctx = 0;
        int stride = frame.miStride;
        if (availU) {
            if (!aboveSingle) {
                ctx += frame.compGroupIdx[(miRow - 1) * stride + miCol];
            } else if (aboveRef0 == 7) {
                ctx += 3;
            }
        }
        if (availL) {
            if (!leftSingle) {
                ctx += frame.compGroupIdx[miRow * stride + miCol - 1];
            } else if (leftRef0 == 7) {
                ctx += 3;
            }
        }
        return Math.min(5, ctx);
    }

    /**
     * The context of the syntax element compound_idx.
     * <p>
     * 구문 요소 compound_idx 의 문맥.
     *
     * @return the context / 문맥
     */
    private int compoundIdxContext() {
        int fwd = Math.abs(Av1FrameHeader.relativeDist(seq, hdr.orderHints[refFrame0], hdr.orderHint));
        int bck = Math.abs(Av1FrameHeader.relativeDist(seq, hdr.orderHints[refFrame1], hdr.orderHint));
        int ctx = fwd == bck ? 3 : 0;
        int stride = frame.miStride;
        if (availU) {
            if (!aboveSingle) {
                ctx += frame.compoundIdx[(miRow - 1) * stride + miCol];
            } else if (aboveRef0 == 7) {
                ctx++;
            }
        }
        if (availL) {
            if (!leftSingle) {
                ctx += frame.compoundIdx[miRow * stride + miCol - 1];
            } else if (leftRef0 == 7) {
                ctx++;
            }
        }
        return ctx;
    }

    /**
     * Counts the coding tools that the current inter block uses ({@link Av1ToolStats}).
     * <p>
     * 현재 인터 블록이 쓰는 부호화 도구를 센다({@link Av1ToolStats}).
     */
    private void countTools() {
        if (skipMode) {
            Av1ToolStats.count(Av1ToolStats.SKIP_MODE);
        }
        if (segmentId != 0) {
            Av1ToolStats.count(Av1ToolStats.SEGMENT);
        }
        if (refFrame1 > 0) {
            Av1ToolStats.count(Av1ToolStats.COMPOUND_BLOCKS);
            if (compoundType == COMPOUND_WEDGE) {
                Av1ToolStats.count(Av1ToolStats.COMPOUND_WEDGE);
            } else if (compoundType == COMPOUND_DIFFWTD) {
                Av1ToolStats.count(Av1ToolStats.COMPOUND_DIFFWTD);
            } else if (compoundType == COMPOUND_DISTANCE) {
                Av1ToolStats.count(Av1ToolStats.COMPOUND_DISTANCE);
            } else {
                Av1ToolStats.count(Av1ToolStats.COMPOUND_AVERAGE);
            }
        }
        if (refFrame1 == 0) {
            Av1ToolStats.count(wedgeInterintra ? Av1ToolStats.INTERINTRA_WEDGE : Av1ToolStats.INTERINTRA);
        }
        if (motionMode == 1) {
            Av1ToolStats.count(Av1ToolStats.OBMC);
        } else if (motionMode == 2) {
            Av1ToolStats.count(Av1ToolStats.LOCAL_WARP);
        }
        if (interpFilter0 != interpFilter1) {
            Av1ToolStats.count(Av1ToolStats.DUAL_FILTER);
        }
        if (isScaled(refFrame0)) {
            Av1ToolStats.count(Av1ToolStats.SCALED_REFERENCE);
        }
    }

    /**
     * Predicts the samples of an inter block (<code>compute_prediction</code> for inter blocks): every plane is predicted for the whole block
     * before the residual is added.
     * <p>
     * 인터 블록의 샘플을 예측한다(인터 블록의 <code>compute_prediction</code>): 잔차를 더하기 전에 평면마다 블록 전체를 예측한다.
     *
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void computePrediction() throws MediaException {
        if (!isInter) {
            if (!hdr.frameIsIntra) {
                Av1ToolStats.count(Av1ToolStats.INTRA_IN_INTER);
            }
            return;
        }
        if (!useIntrabc) {
            countTools();
        }
        int planes = hasChroma ? 3 : 1;
        int stride = frame.miStride;
        int sbMask = seq.use128x128Superblock ? 31 : 15;
        int subBlockMiRow = miRow & sbMask;
        int subBlockMiCol = miCol & sbMask;
        boolean isInterIntra = !useIntrabc && refFrame1 == 0;
        // 평면마다 예측한다.
        for (int plane = 0; plane < planes; plane++) {
            int subX = plane > 0 ? seq.subsamplingX : 0;
            int subY = plane > 0 ? seq.subsamplingY : 0;
            int planeSz = AvifTables.SUBSAMPLED_SIZE[miSize * 4 + subX * 2 + subY];
            int num4x4W = AvifTables.NUM_4X4_BLOCKS_WIDE[planeSz];
            int num4x4H = AvifTables.NUM_4X4_BLOCKS_HIGH[planeSz];
            int baseX = (miCol >> subX) * 4;
            int baseY = (miRow >> subY) * 4;
            int candRow = (miRow >> subY) << subY;
            int candCol = (miCol >> subX) << subX;
            if (isInterIntra) {
                // 인터인트라: 먼저 인트라 예측을 프레임에 쓰고 이후 인터 예측과 마스크로 섞는다.
                int mode = interintraMode == 0 ? 0 : (interintraMode == 1 ? 1 : (interintraMode == 2 ? 2 : 9));
                int log2W = 2 + AvifTables.MI_WIDTH_LOG2[planeSz];
                int log2H = 2 + AvifTables.MI_HEIGHT_LOG2[planeSz];
                boolean[] bd = blockDecoded[plane];
                boolean haveAboveRight = bd[(((subBlockMiRow >> subY) - 1) + 1) * BD_STRIDE + (subBlockMiCol >> subX) + num4x4W + 1];
                boolean haveBelowLeft = bd[(((subBlockMiRow >> subY) + num4x4H) + 1) * BD_STRIDE + (subBlockMiCol >> subX) - 1 + 1];
                int maxX = (frame.miCols * 4) >> subX;
                int maxY = (frame.miRows * 4) >> subY;
                Av1IntraPred.predict(frame.planes[plane], frame.stride[plane], baseX, baseY, log2W, log2H, mode, plane == 0 ? availL : availLChroma,
                        plane == 0 ? availU : availUChroma, haveAboveRight, haveBelowLeft, maxX - 1, maxY - 1, 0, seq.enableIntraEdgeFilter, 0, false, 0,
                        seq.bitDepth);
            }
            int predW = (AvifTables.NUM_4X4_BLOCKS_WIDE[miSize] * 4) >> subX;
            int predH = (AvifTables.NUM_4X4_BLOCKS_HIGH[miSize] * 4) >> subY;
            boolean someUseIntra = false;
            // 색차 블록이 덮는 휘도 블록 중 인트라 블록이 있는지 확인한다.
            for (int r = 0; r < (num4x4H << subY); r++) {
                for (int c = 0; c < (num4x4W << subX); c++) {
                    if (frame.refFrame0[(candRow + r) * stride + candCol + c] == 0) {
                        someUseIntra = true;
                    }
                }
            }
            if (someUseIntra) {
                predW = num4x4W * 4;
                predH = num4x4H * 4;
                candRow = miRow;
                candCol = miCol;
            }
            int r = 0;
            // 예측 단위로 나누어 블록의 각 부분을 해당 움직임 정보로 예측한다.
            for (int y = 0; y < num4x4H * 4; y += predH) {
                int c = 0;
                // 블록의 가로 방향 예측 단위를 차례로 예측한다.
                for (int x = 0; x < num4x4W * 4; x += predW) {
                    predictInter(plane, baseX + x, baseY + y, predW, predH, candRow + r, candCol + c);
                    c++;
                }
                r++;
            }
        }
    }

    /**
     * Predicts a region of a plane from the motion information of a 4x4 unit (the inter prediction process, specification 7.11.3).
     * <p>
     * 4x4 단위의 움직임 정보로 평면의 영역을 예측한다(인터 예측 과정, 규격 7.11.3).
     *
     * @param plane   the plane / 평면
     * @param x       the x position of the region in the plane / 평면에서 영역의 x 위치
     * @param y       the y position of the region / 영역의 y 위치
     * @param w       the width of the region / 영역의 너비
     * @param h       the height of the region / 영역의 높이
     * @param candRow the row of the unit with the motion information / 움직임 정보를 가진 단위의 행
     * @param candCol the column of the unit / 단위의 열
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void predictInter(int plane, int x, int y, int w, int h, int candRow, int candCol) throws MediaException {
        int ci = candRow * frame.miStride + candCol;
        boolean isCompound = frame.refFrame1[ci] > 0;
        int bitDepth = seq.bitDepth;
        int round0 = 3;
        int round1 = isCompound ? 7 : 11;
        if (bitDepth == 12) {
            round0 += 2;
            if (!isCompound) {
                round1 -= 2;
            }
        }
        int postRound = 14 - (round0 + round1);
        int subX = plane > 0 ? seq.subsamplingX : 0;
        int subY = plane > 0 ? seq.subsamplingY : 0;
        if (plane == 0 && motionMode == 2) {
            localValid = Av1Warp.estimate(candList, numSamples, miRow, miCol, AvifTables.NUM_4X4_BLOCKS_WIDE[miSize],
                    AvifTables.NUM_4X4_BLOCKS_HIGH[miSize], curMv[0], curMv[1], localWarpParams);
            if (localValid) {
                localValid = Av1Warp.setupShear(localWarpParams, localShear);
            }
        }
        boolean globalMode = yMode == Av1MvStack.GLOBALMV || yMode == Av1MvStack.GLOBAL_GLOBALMV;
        // 참조 목록마다(복합이면 둘) 예측 배열을 만든다.
        for (int refList = 0; refList < (isCompound ? 2 : 1); refList++) {
            int refFrame = refList == 0 ? frame.refFrame0[ci] : frame.refFrame1[ci];
            boolean globalValid = false;
            boolean warpGlobal = globalMode && refFrame > 0 && hdr.gmType[refFrame] > Av1MvStack.TRANSLATION;
            if (warpGlobal) {
                globalValid = Av1Warp.setupShear(hdr.gmParams[refFrame], globalShear);
            }
            int useWarp;
            if (w < 8 || h < 8 || hdr.forceIntegerMv) {
                useWarp = 0;
            } else if (motionMode == 2 && localValid) {
                useWarp = 1;
            } else if (warpGlobal && !isScaled(refFrame) && globalValid) {
                useWarp = 2;
                Av1ToolStats.count(Av1ToolStats.GLOBAL_WARP);
            } else {
                useWarp = 0;
            }
            int mvRow = frame.mvs[ci * 4 + refList * 2];
            int mvCol = frame.mvs[ci * 4 + refList * 2 + 1];
            short[] refPlane;
            int refStride;
            int lastX;
            int lastY;
            int refUpWidth;
            int refHeight;
            if (useIntrabc) {
                // 인트라 블록 복사는 아직 필터링되지 않은 현재 프레임에서 복사한다(참조 크기는 모드 정보 단위로 올린 크기이다).
                refPlane = frame.planes[plane];
                refStride = frame.stride[plane];
                refUpWidth = hdr.upscaledWidth;
                refHeight = hdr.frameHeight;
                lastX = ((frame.miCols * 4 + subX) >> subX) - 1;
                lastY = ((frame.miRows * 4 + subY) >> subY) - 1;
            } else {
                Av1RefFrame ref = frame.refList[refFrame];
                refPlane = ref.planes[plane];
                refStride = ref.stride[plane];
                refUpWidth = ref.upscaledWidth;
                refHeight = ref.frameHeight;
                lastX = ((ref.upscaledWidth + subX) >> subX) - 1;
                lastY = ((ref.frameHeight + subY) >> subY) - 1;
            }
            int[] pred = preds[refList];
            if (useWarp != 0) {
                int[] wp = useWarp == 1 ? localWarpParams : hdr.gmParams[refFrame];
                int[] shear = useWarp == 1 ? localShear : globalShear;
                // 8x8 조각마다 워프 예측을 한다.
                for (int i8 = 0; i8 <= ((h - 1) >> 3); i8++) {
                    for (int j8 = 0; j8 <= ((w - 1) >> 3); j8++) {
                        Av1Warp.blockWarp(refPlane, refStride, lastX, lastY, wp, shear, subX, subY, x, y, i8, j8, w, h, round0, round1, pred);
                    }
                }
            } else {
                Av1InterPred.scale(interPos, subX, subY, x, y, mvRow, mvCol, refUpWidth, refHeight, hdr.frameWidth, hdr.frameHeight);
                interPred.predict(refPlane, refStride, lastX, lastY, interPos.startX, interPos.startY, interPos.stepX, interPos.stepY, w, h,
                        frame.interpFilter1[ci], frame.interpFilter0[ci], round0, round1, pred);
            }
        }
        // 혼합에 쓸 마스크를 만든다. 쐐기와 차이 가중 마스크는 휘도에서 만들어 색차에서 줄여 쓴다.
        if (compoundType == COMPOUND_WEDGE && plane == 0) {
            byte[] wm = Av1WedgeMasks.get(miSize, wedgeSign, wedgeIndex);
            // 쐐기 마스크를 휘도 마스크 배열로 옮긴다.
            for (int i = 0; i < h; i++) {
                // 한 행의 열을 차례로 옮긴다.
                for (int j = 0; j < w; j++) {
                    maskArr[i * MASK_STRIDE + j] = wm[i * w + j];
                }
            }
        } else if (compoundType == COMPOUND_INTRA) {
            int sizeScale = 128 / Math.max(h, w);
            // 인터인트라 모드에 따라 위쪽, 왼쪽 또는 둘 다에서 멀어질수록 인트라 비중이 줄어드는 마스크를 만든다.
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    int m;
                    if (interintraMode == 1) {
                        m = AvifInterTables.II_WEIGHTS_1D[i * sizeScale];
                    } else if (interintraMode == 2) {
                        m = AvifInterTables.II_WEIGHTS_1D[j * sizeScale];
                    } else if (interintraMode == 3) {
                        m = AvifInterTables.II_WEIGHTS_1D[Math.min(i, j) * sizeScale];
                    } else {
                        m = 32;
                    }
                    maskArr[i * MASK_STRIDE + j] = m;
                }
            }
        } else if (compoundType == COMPOUND_DIFFWTD && plane == 0) {
            // 두 예측의 차이가 클수록 한쪽에 가중치를 더 주는 마스크를 만든다.
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    int diff = Math.abs(preds[0][i * w + j] - preds[1][i * w + j]);
                    int sh = (bitDepth - 8) + postRound;
                    diff = sh == 0 ? diff : (diff + (1 << (sh - 1))) >> sh;
                    int m = Math.max(0, Math.min(64, 38 + diff / 16));
                    maskArr[i * MASK_STRIDE + j] = maskType != 0 ? 64 - m : m;
                }
            }
        }
        short[] dst = frame.planes[plane];
        int dstStride = frame.stride[plane];
        int maxVal = (1 << bitDepth) - 1;
        boolean isInterIntra = !useIntrabc && refFrame1 == 0;
        if (!isCompound && !isInterIntra) {
            int[] p0 = preds[0];
            // 예측 값을 샘플 범위로 자르며 프레임에 쓴다.
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    int v = p0[i * w + j];
                    dst[(y + i) * dstStride + x + j] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
                }
            }
        } else if (compoundType == COMPOUND_AVERAGE) {
            int[] p0 = preds[0];
            int[] p1 = preds[1];
            int sh = 1 + postRound;
            // 두 예측의 평균을 쓴다.
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    int v = (p0[i * w + j] + p1[i * w + j] + (1 << (sh - 1))) >> sh;
                    dst[(y + i) * dstStride + x + j] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
                }
            }
        } else if (compoundType == COMPOUND_DISTANCE) {
            int[] p0 = preds[0];
            int[] p1 = preds[1];
            distanceWeights(candRow, candCol);
            int sh = 4 + postRound;
            // 참조 프레임까지의 거리에 따른 가중 평균을 쓴다.
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    int v = (fwdWeight * p0[i * w + j] + bckWeight * p1[i * w + j] + (1 << (sh - 1))) >> sh;
                    dst[(y + i) * dstStride + x + j] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
                }
            }
        } else {
            maskBlend(plane, dst, dstStride, x, y, w, h, postRound, maxVal);
        }
        if (motionMode == 1) {
            overlappedMotionCompensation(plane, w, h, round0, round1);
        }
    }

    /**
     * Blends the predictions with the mask (the mask blend process, specification 7.11.3.14).
     * <p>
     * 마스크로 예측을 섞는다(마스크 혼합 과정, 규격 7.11.3.14).
     *
     * @param plane     the plane / 평면
     * @param dst       the samples of the plane / 평면의 샘플
     * @param dstStride the row stride of the plane / 평면의 행 간격
     * @param dstX      the x position of the region / 영역의 x 위치
     * @param dstY      the y position of the region / 영역의 y 위치
     * @param w         the width / 너비
     * @param h         the height / 높이
     * @param postRound the final rounding / 마지막 반올림
     * @param maxVal    the largest sample value / 샘플 최댓값
     */
    private void maskBlend(int plane, short[] dst, int dstStride, int dstX, int dstY, int w, int h, int postRound, int maxVal) {
        int subX = plane > 0 ? seq.subsamplingX : 0;
        int subY = plane > 0 ? seq.subsamplingY : 0;
        boolean ii = refFrame1 == 0;
        // 샘플마다 마스크 값을 정하고(색차는 휘도 마스크를 줄여서) 두 예측을 섞는다.
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int m;
                if ((subX == 0 && subY == 0) || (ii && !wedgeInterintra)) {
                    m = maskArr[yy * MASK_STRIDE + xx];
                } else if (subX != 0 && subY == 0) {
                    m = (maskArr[yy * MASK_STRIDE + 2 * xx] + maskArr[yy * MASK_STRIDE + 2 * xx + 1] + 1) >> 1;
                } else {
                    m = (maskArr[2 * yy * MASK_STRIDE + 2 * xx] + maskArr[2 * yy * MASK_STRIDE + 2 * xx + 1]
                            + maskArr[(2 * yy + 1) * MASK_STRIDE + 2 * xx] + maskArr[(2 * yy + 1) * MASK_STRIDE + 2 * xx + 1] + 2) >> 2;
                }
                int pos = (dstY + yy) * dstStride + dstX + xx;
                if (ii) {
                    int p0 = preds[0][yy * w + xx];
                    p0 = postRound == 0 ? p0 : (p0 + (1 << (postRound - 1))) >> postRound;
                    p0 = p0 < 0 ? 0 : (p0 > maxVal ? maxVal : p0);
                    int p1 = dst[pos];
                    dst[pos] = (short) ((m * p1 + (64 - m) * p0 + 32) >> 6);
                } else {
                    int sh = 6 + postRound;
                    int v = (m * preds[0][yy * w + xx] + (64 - m) * preds[1][yy * w + xx] + (1 << (sh - 1))) >> sh;
                    dst[pos] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
                }
            }
        }
    }

    /**
     * Computes the weights of the two predictions from the distance of their frames (the distance weights process, specification 7.11.3.15).
     * <p>
     * 두 프레임의 거리로 두 예측의 가중치를 구한다(거리 가중치 과정, 규격 7.11.3.15).
     *
     * @param candRow the row of the unit with the motion information / 움직임 정보를 가진 단위의 행
     * @param candCol the column of the unit / 단위의 열
     */
    private void distanceWeights(int candRow, int candCol) {
        int ci = candRow * frame.miStride + candCol;
        int[] dist = new int[2];
        // 두 참조 프레임의 현재 프레임까지 거리를 구한다.
        for (int refList = 0; refList < 2; refList++) {
            int ref = refList == 0 ? frame.refFrame0[ci] : frame.refFrame1[ci];
            int hint = hdr.orderHints[ref];
            dist[refList] = Math.max(0, Math.min(31, Math.abs(Av1FrameHeader.relativeDist(seq, hint, hdr.orderHint))));
        }
        int d0 = dist[1];
        int d1 = dist[0];
        int order = d0 <= d1 ? 1 : 0;
        int[] lookup = AvifInterTables.QUANT_DIST_LOOKUP;
        int[] weight = AvifInterTables.QUANT_DIST_WEIGHT;
        if (d0 == 0 || d1 == 0) {
            fwdWeight = lookup[3 * 2 + order];
            bckWeight = lookup[3 * 2 + 1 - order];
        } else {
            int i;
            // 거리 비에 맞는 가중치 줄을 찾는다.
            for (i = 0; i < 3; i++) {
                int c0 = weight[i * 2 + order];
                int c1 = weight[i * 2 + 1 - order];
                if (order != 0) {
                    if (d0 * c0 > d1 * c1) {
                        break;
                    }
                } else if (d0 * c0 < d1 * c1) {
                    break;
                }
            }
            fwdWeight = lookup[i * 2 + order];
            bckWeight = lookup[i * 2 + 1 - order];
        }
    }

    /**
     * Blends the prediction of the neighbours above and to the left into the prediction of the block (the overlapped motion compensation
     * process, specification 7.11.3.9).
     * <p>
     * 위쪽과 왼쪽 이웃의 예측을 블록의 예측에 섞는다(겹침 움직임 보상 과정, 규격 7.11.3.9).
     *
     * @param plane  the plane / 평면
     * @param w      the width of the block in the plane / 평면에서 블록의 너비
     * @param h      the height of the block in the plane / 평면에서 블록의 높이
     * @param round0 the rounding after the horizontal filter / 가로 필터 뒤의 반올림
     * @param round1 the rounding after the vertical filter / 세로 필터 뒤의 반올림
     */
    private void overlappedMotionCompensation(int plane, int w, int h, int round0, int round1) {
        int subX = plane > 0 ? seq.subsamplingX : 0;
        int subY = plane > 0 ? seq.subsamplingY : 0;
        int stride = frame.miStride;
        if (availU && AvifTables.SUBSAMPLED_SIZE[miSize * 4 + subX * 2 + subY] >= BLOCK_8X8) {
            int w4 = AvifTables.NUM_4X4_BLOCKS_WIDE[miSize];
            int x4 = miCol;
            int nCount = 0;
            int nLimit = Math.min(4, AvifTables.MI_WIDTH_LOG2[miSize]);
            // 위쪽 이웃을 왼쪽에서 오른쪽으로 최대 nLimit 개까지 처리한다.
            while (nCount < nLimit && x4 < Math.min(frame.miCols, miCol + w4)) {
                int candRow = miRow - 1;
                int candCol = x4 | 1;
                int candSz = frame.miSize[candRow * stride + candCol];
                int step4 = Math.max(2, Math.min(16, AvifTables.NUM_4X4_BLOCKS_WIDE[candSz]));
                if (frame.refFrame0[candRow * stride + candCol] > 0) {
                    nCount++;
                    int predW = Math.min(w, (step4 * 4) >> subX);
                    int predH = Math.min(h >> 1, 32 >> subY);
                    predictOverlap(plane, candRow, candCol, (x4 * 4) >> subX, (miRow * 4) >> subY, predW, predH, 0, obmcMask(predH), round0, round1);
                }
                x4 += step4;
            }
        }
        if (availL) {
            int h4 = AvifTables.NUM_4X4_BLOCKS_HIGH[miSize];
            int y4 = miRow;
            int nCount = 0;
            int nLimit = Math.min(4, AvifTables.MI_HEIGHT_LOG2[miSize]);
            // 왼쪽 이웃을 위에서 아래로 최대 nLimit 개까지 처리한다.
            while (nCount < nLimit && y4 < Math.min(frame.miRows, miRow + h4)) {
                int candCol = miCol - 1;
                int candRow = y4 | 1;
                int candSz = frame.miSize[candRow * stride + candCol];
                int step4 = Math.max(2, Math.min(16, AvifTables.NUM_4X4_BLOCKS_HIGH[candSz]));
                if (frame.refFrame0[candRow * stride + candCol] > 0) {
                    nCount++;
                    int predW = Math.min(w >> 1, 32 >> subX);
                    int predH = Math.min(h, (step4 * 4) >> subY);
                    predictOverlap(plane, candRow, candCol, (miCol * 4) >> subX, (y4 * 4) >> subY, predW, predH, 1, obmcMask(predW), round0, round1);
                }
                y4 += step4;
            }
        }
    }

    /**
     * Returns the blending mask of an overlap of the given length (<code>get_obmc_mask</code>).
     * <p>
     * 주어진 길이의 겹침에 쓰는 혼합 마스크를 반환한다(<code>get_obmc_mask</code>).
     *
     * @param length the length of the overlap / 겹침의 길이
     * @return the mask / 마스크
     */
    private static int[] obmcMask(int length) {
        if (length == 2) {
            return AvifInterTables.OBMC_MASK_2;
        } else if (length == 4) {
            return AvifInterTables.OBMC_MASK_4;
        } else if (length == 8) {
            return AvifInterTables.OBMC_MASK_8;
        } else if (length == 16) {
            return AvifInterTables.OBMC_MASK_16;
        }
        return AvifInterTables.OBMC_MASK_32;
    }

    /**
     * Predicts an overlap region with the vector of a neighbour and blends it into the frame (<code>predict_overlap</code> and the overlap
     * blending process, specification 7.11.3.10).
     * <p>
     * 이웃의 벡터로 겹침 영역을 예측하여 프레임에 섞는다(<code>predict_overlap</code> 와 겹침 혼합 과정, 규격 7.11.3.10).
     *
     * @param plane   the plane / 평면
     * @param candRow the row of the neighbour / 이웃의 행
     * @param candCol the column of the neighbour / 이웃의 열
     * @param predX   the x position of the region in the plane / 평면에서 영역의 x 위치
     * @param predY   the y position of the region / 영역의 y 위치
     * @param predW   the width of the region / 영역의 너비
     * @param predH   the height of the region / 영역의 높이
     * @param pass    0 when blending from above, 1 when blending from the left / 위쪽에서 섞으면 0, 왼쪽에서 섞으면 1
     * @param mask    the blending weights / 혼합 가중치
     * @param round0  the rounding after the horizontal filter / 가로 필터 뒤의 반올림
     * @param round1  the rounding after the vertical filter / 세로 필터 뒤의 반올림
     */
    private void predictOverlap(int plane, int candRow, int candCol, int predX, int predY, int predW, int predH, int pass, int[] mask, int round0,
            int round1) {
        int subX = plane > 0 ? seq.subsamplingX : 0;
        int subY = plane > 0 ? seq.subsamplingY : 0;
        int ci = candRow * frame.miStride + candCol;
        Av1RefFrame ref = frame.refList[frame.refFrame0[ci]];
        int lastX = ((ref.upscaledWidth + subX) >> subX) - 1;
        int lastY = ((ref.frameHeight + subY) >> subY) - 1;
        Av1InterPred.scale(interPos, subX, subY, predX, predY, frame.mvs[ci * 4], frame.mvs[ci * 4 + 1], ref.upscaledWidth, ref.frameHeight,
                hdr.frameWidth, hdr.frameHeight);
        int[] pred = preds[0];
        interPred.predict(ref.planes[plane], ref.stride[plane], lastX, lastY, interPos.startX, interPos.startY, interPos.stepX, interPos.stepY, predW,
                predH, frame.interpFilter1[ci], frame.interpFilter0[ci], round0, round1, pred);
        short[] dst = frame.planes[plane];
        int dstStride = frame.stride[plane];
        int maxVal = (1 << seq.bitDepth) - 1;
        // 이웃 벡터로 예측한 샘플을 마스크 가중치로 현재 예측에 섞는다.
        for (int i = 0; i < predH; i++) {
            for (int j = 0; j < predW; j++) {
                int o = pred[i * predW + j];
                o = o < 0 ? 0 : (o > maxVal ? maxVal : o);
                int m = pass == 0 ? mask[i] : mask[j];
                int pos = (predY + i) * dstStride + predX + j;
                dst[pos] = (short) ((m * dst[pos] + (64 - m) * o + 32) >> 6);
            }
        }
    }

    /**
     * Tells if the block above or to the left uses a smooth prediction mode (<code>get_filter_type</code>).
     * <p>
     * 위쪽 또는 왼쪽 블록이 스무스 예측 모드를 쓰는지 알려 준다(<code>get_filter_type</code>).
     *
     * @param plane the plane / 평면
     * @return 1 if smooth, otherwise 0 / 스무스면 1, 아니면 0
     */
    private int filterType(int plane) {
        boolean aboveSmooth = false;
        boolean leftSmooth = false;
        if (plane == 0 ? availU : availUChroma) {
            int r = miRow - 1;
            int c = miCol;
            if (plane > 0) {
                if (seq.subsamplingX != 0 && (miCol & 1) == 0) {
                    c++;
                }
                if (seq.subsamplingY != 0 && (miRow & 1) != 0) {
                    r--;
                }
            }
            aboveSmooth = isSmooth(r, c, plane);
        }
        if (plane == 0 ? availL : availLChroma) {
            int r = miRow;
            int c = miCol - 1;
            if (plane > 0) {
                if (seq.subsamplingX != 0 && (miCol & 1) != 0) {
                    c--;
                }
                if (seq.subsamplingY != 0 && (miRow & 1) == 0) {
                    r++;
                }
            }
            leftSmooth = isSmooth(r, c, plane);
        }
        return (aboveSmooth || leftSmooth) ? 1 : 0;
    }

    /**
     * Tells if the block at a position uses a smooth prediction mode.
     * <p>
     * 위치의 블록이 스무스 예측 모드를 쓰는지 알려 준다.
     *
     * @param row   the row / 행
     * @param col   the column / 열
     * @param plane the plane / 평면
     * @return true if smooth / 스무스면 true
     */
    private boolean isSmooth(int row, int col, int plane) {
        int i = row * frame.miStride + col;
        if (plane != 0 && frame.refFrame0[i] > 0) {
            return false;
        }
        int mode = plane == 0 ? frame.yMode[i] : frame.uvMode[i];
        return mode == Av1IntraPred.SMOOTH_PRED || mode == Av1IntraPred.SMOOTH_V_PRED || mode == Av1IntraPred.SMOOTH_H_PRED;
    }

    /**
     * Predicts a transform block from the palette (<code>predict_palette</code>).
     * <p>
     * 팔레트로 변환 블록을 예측한다(<code>predict_palette</code>).
     *
     * @param plane  the plane / 평면
     * @param startX the x position in samples / x 위치(샘플)
     * @param startY the y position in samples / y 위치(샘플)
     * @param x      the x offset in 4x4 units of the block / 블록 안의 4x4 단위 x 오프셋
     * @param y      the y offset in 4x4 units of the block / 블록 안의 4x4 단위 y 오프셋
     * @param txSz   the transform size / 변환 크기
     */
    private void predictPalette(int plane, int startX, int startY, int x, int y, int txSz) {
        int w = AvifTables.TX_WIDTH[txSz];
        int h = AvifTables.TX_HEIGHT[txSz];
        short[] palette = plane == 0 ? paletteColorsY : (plane == 1 ? paletteColorsU : paletteColorsV);
        byte[] map = plane == 0 ? colorMapY : colorMapUv;
        short[] pl = frame.planes[plane];
        int stride = frame.stride[plane];
        // 색 지도의 색인으로 팔레트 색을 찾아 샘플을 채운다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                pl[(startY + i) * stride + startX + j] = palette[map[(y * 4 + i) * 64 + x * 4 + j]];
            }
        }
    }

    /**
     * Adds the chroma from luma prediction to the DC predicted chroma samples (<code>predict_chroma_from_luma</code>).
     * <p>
     * DC 로 예측한 색차 샘플에 휘도로부터의 색차 예측을 더한다(<code>predict_chroma_from_luma</code>).
     *
     * @param plane  the chroma plane / 색차 평면
     * @param startX the x position in samples / x 위치(샘플)
     * @param startY the y position in samples / y 위치(샘플)
     * @param txSz   the transform size / 변환 크기
     */
    private void predictChromaFromLuma(int plane, int startX, int startY, int txSz) {
        int w = AvifTables.TX_WIDTH[txSz];
        int h = AvifTables.TX_HEIGHT[txSz];
        int subX = seq.subsamplingX;
        int subY = seq.subsamplingY;
        int alpha = plane == 1 ? cflAlphaU : cflAlphaV;
        short[] luma = frame.planes[0];
        int lstride = frame.stride[0];
        int[] lum = new int[w * h];
        int lumaAvg = 0;
        // 복원된 휘도를 색차 해상도로 줄여 평균을 구한다.
        for (int i = 0; i < h; i++) {
            int lumaY = (startY + i) << subY;
            lumaY = Math.min(lumaY, maxLumaH - (1 << subY));
            // 한 행의 샘플을 처리한다.
            for (int j = 0; j < w; j++) {
                int lumaX = (startX + j) << subX;
                lumaX = Math.min(lumaX, maxLumaW - (1 << subX));
                int tt = 0;
                // 세로로 합칠 휘도 행을 처리한다.
                for (int dy = 0; dy <= subY; dy++) {
                    // 가로로 합칠 휘도 열을 처리한다.
                    for (int dx = 0; dx <= subX; dx++) {
                        tt += luma[(lumaY + dy) * lstride + lumaX + dx];
                    }
                }
                int v = tt << (3 - subX - subY);
                lum[i * w + j] = v;
                lumaAvg += v;
            }
        }
        int shift = AvifTables.TX_WIDTH_LOG2[txSz] + AvifTables.TX_HEIGHT_LOG2[txSz];
        lumaAvg = (lumaAvg + (1 << (shift - 1))) >> shift;
        short[] pl = frame.planes[plane];
        int stride = frame.stride[plane];
        int maxVal = (1 << seq.bitDepth) - 1;
        // 휘도의 고주파 성분을 알파 배율로 더한다.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int dc = pl[(startY + i) * stride + startX + j];
                int scaled = alpha * (lum[i * w + j] - lumaAvg);
                int scaledLuma = scaled >= 0 ? (scaled + 32) >> 6 : -((-scaled + 32) >> 6);
                pl[(startY + i) * stride + startX + j] = (short) Math.max(0, Math.min(maxVal, dc + scaledLuma));
            }
        }
    }

    /**
     * The quantizer index of the current block (<code>get_qindex</code> with ignoreDeltaQ equal to 0).
     * <p>
     * 현재 블록의 양자화 지수(ignoreDeltaQ 가 0 인 <code>get_qindex</code>).
     *
     * @return the index / 지수
     */
    private int blockQIndex() {
        if (hdr.segmentationEnabled && hdr.featureEnabled[segmentId][0]) {
            int data = hdr.featureData[segmentId][0];
            int q = hdr.baseQIdx + data;
            if (hdr.deltaQPresent) {
                q = currentQIndex + data;
            }
            return Math.max(0, Math.min(255, q));
        }
        if (hdr.deltaQPresent) {
            return currentQIndex;
        }
        return hdr.baseQIdx;
    }

    /**
     * Looks up the quantizer step of a quantizer index (<code>dc_q</code> or <code>ac_q</code>).
     * <p>
     * 양자화 지수의 양자화 단계를 찾는다(<code>dc_q</code> 또는 <code>ac_q</code>).
     *
     * @param table the lookup table / 조회 표
     * @param b     the index (clipped to 0..255) / 지수(0~255 로 제한한다)
     * @return the step / 단계
     */
    private int quantStep(int[] table, int b) {
        int i = Math.max(0, Math.min(255, b));
        return table[((seq.bitDepth - 8) >> 1) * 256 + i];
    }

    /**
     * Dequantizes, inverse transforms and adds the residual of a transform block (<code>reconstruct</code>).
     * <p>
     * 변환 블록의 계수를 역양자화하고 역변환하여 잔차를 더한다(<code>reconstruct</code>).
     *
     * @param plane  the plane / 평면
     * @param x      the x position in samples / x 위치(샘플)
     * @param y      the y position in samples / y 위치(샘플)
     * @param txSz   the transform size / 변환 크기
     */
    private void reconstruct(int plane, int x, int y, int txSz) {
        int dqDenom;
        if (txSz == 3 || txSz == 9 || txSz == 10 || txSz == 17 || txSz == 18) {
            dqDenom = 2;
        } else if (txSz == 4 || txSz == 11 || txSz == 12) {
            dqDenom = 4;
        } else {
            dqDenom = 1;
        }
        int log2W = AvifTables.TX_WIDTH_LOG2[txSz];
        int log2H = AvifTables.TX_HEIGHT_LOG2[txSz];
        int w = 1 << log2W;
        int h = 1 << log2H;
        int tw = Math.min(32, w);
        int th = Math.min(32, h);
        int type = coeff.planeTxType;
        boolean flipUD = type == 4 || type == 8 || type == 14 || type == 6;
        boolean flipLR = type == 5 || type == 7 || type == 15 || type == 6;
        int q = blockQIndex();
        int dcDelta = plane == 0 ? hdr.deltaQYDc : (plane == 1 ? hdr.deltaQUDc : hdr.deltaQVDc);
        int acDelta = plane == 0 ? 0 : (plane == 1 ? hdr.deltaQUAc : hdr.deltaQVAc);
        int dcQ = quantStep(AvifTables.DC_QLOOKUP, q + dcDelta);
        int acQ = quantStep(AvifTables.AC_QLOOKUP, q + acDelta);
        int bitDepth = seq.bitDepth;
        int lim = 1 << (7 + bitDepth);
        int[] qv = coeff.quant;
        int qmLevel = hdr.qmLevel(plane, segmentId);
        boolean useQm = qmLevel < 15 && type < 9;
        int qmBase = AvifTables.QM_OFFSET[txSz];
        int qmChroma = plane > 0 ? 1 : 0;
        // 0 이 아닌 계수만 역양자화하여 Dequant 배열에 넣는다.
        for (int i = 0; i < th; i++) {
            for (int j = 0; j < tw; j++) {
                int v = qv[i * tw + j];
                if (v == 0) {
                    continue;
                }
                int step = (i == 0 && j == 0) ? dcQ : acQ;
                if (useQm) {
                    step = (step * AvifQmTable.weight(qmLevel, qmChroma, qmBase + i * tw + j) + 16) >> 5;
                }
                long dq = (long) v * step;
                long mag = (dq < 0 ? -dq : dq) & 0xFFFFFF;
                long dq2 = mag / dqDenom;
                if (dq < 0) {
                    dq2 = -dq2;
                }
                dequant[i * 64 + j] = (int) Math.max(-lim, Math.min(lim - 1, dq2));
            }
        }
        Av1InvTransform.inverse2d(dequant, log2W, log2H, type, lossless, AvifTables.TRANSFORM_ROW_SHIFT[txSz], bitDepth, residual);
        // 다음 변환 블록을 위해 Dequant 배열의 사용한 영역을 다시 0 으로 만든다.
        for (int i = 0; i < th; i++) {
            java.util.Arrays.fill(dequant, i * 64, i * 64 + tw, 0);
        }
        short[] pl = frame.planes[plane];
        int stride = frame.stride[plane];
        int maxVal = (1 << bitDepth) - 1;
        // 잔차를 (뒤집기를 반영하여) 예측 샘플에 더한다.
        for (int i = 0; i < h; i++) {
            int yy = flipUD ? h - i - 1 : i;
            // 한 행의 샘플을 처리한다.
            for (int j = 0; j < w; j++) {
                int xx = flipLR ? w - j - 1 : j;
                int idx = (y + yy) * stride + x + xx;
                int v = pl[idx] + residual[i * w + j];
                pl[idx] = (short) (v < 0 ? 0 : (v > maxVal ? maxVal : v));
            }
        }
    }
}
