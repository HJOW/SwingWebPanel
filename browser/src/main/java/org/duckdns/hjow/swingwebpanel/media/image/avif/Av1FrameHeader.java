package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The uncompressed header of an AV1 frame (plan item P18-03; AV1 specification section 5.9): the frame type and size, the reference frames and
 * their order hints, motion vector and warp parameters, tile layout, quantizer, segmentation, delta quantizer/loop filter signalling, loop filter,
 * CDEF, loop restoration and transform mode parameters, and the film grain parameters. All frame types are read (key, inter, intra-only and
 * switch frames, shown existing frames, frames with a primary reference frame); the state that a frame inherits comes from the reference slots.
 * <p>
 * AV1 프레임의 비압축 헤더(계획 P18-03; AV1 규격 5.9절): 프레임 형식과 크기, 참조 프레임과 순서 힌트, 움직임 벡터와 워프 매개변수, 타일 배치, 양자화,
 * 분할, 델타 양자화/루프 필터 신호, 루프 필터, CDEF, 루프 복원, 변환 모드 매개변수와 필름 그레인 매개변수. 모든 프레임 형식(키, 인터, 인트라 전용, 전환
 * 프레임, 기존 프레임 표시, 주 참조 프레임이 있는 프레임)을 읽으며 프레임이 물려받는 상태는 참조 슬롯에서 온다.
 */
final class Av1FrameHeader {

    /**
     * Frame type of a key frame.
     * <p>
     * 키 프레임의 프레임 형식.
     */
    static final int KEY_FRAME = 0;

    /**
     * Frame type of an intra-only frame.
     * <p>
     * 인트라 전용 프레임의 프레임 형식.
     */
    static final int INTRA_ONLY_FRAME = 2;

    /**
     * Transform mode: only 4x4 transforms (lossless).
     * <p>
     * 변환 모드: 4x4 변환만(무손실).
     */
    static final int ONLY_4X4 = 0;

    /**
     * Transform mode: the largest transform of the block.
     * <p>
     * 변환 모드: 블록에서 가장 큰 변환.
     */
    static final int TX_MODE_LARGEST = 1;

    /**
     * Transform mode: the transform size is signalled per block.
     * <p>
     * 변환 모드: 블록마다 변환 크기를 보낸다.
     */
    static final int TX_MODE_SELECT = 2;

    /**
     * Loop restoration type: none.
     * <p>
     * 루프 복원 종류: 없음.
     */
    static final int RESTORE_NONE = 0;

    /**
     * Loop restoration type: Wiener filter.
     * <p>
     * 루프 복원 종류: 위너 필터.
     */
    static final int RESTORE_WIENER = 1;

    /**
     * Loop restoration type: self guided filter.
     * <p>
     * 루프 복원 종류: 자기 유도 필터.
     */
    static final int RESTORE_SGRPROJ = 2;

    /**
     * Loop restoration type: chosen per restoration unit.
     * <p>
     * 루프 복원 종류: 복원 단위마다 선택.
     */
    static final int RESTORE_SWITCHABLE = 3;

    /**
     * Frame type: an inter frame.
     * <p>
     * 프레임 형식: 인터 프레임.
     */
    static final int INTER_FRAME = 1;

    /**
     * Frame type: a switch frame.
     * <p>
     * 프레임 형식: 전환 프레임.
     */
    static final int SWITCH_FRAME = 3;

    /**
     * The value of primary_ref_frame that means that there is no primary reference frame.
     * <p>
     * 주 참조 프레임이 없음을 뜻하는 primary_ref_frame 값.
     */
    static final int PRIMARY_REF_NONE = 7;

    /**
     * The interpolation filter value that means that the filter is chosen for every block.
     * <p>
     * 보간 필터를 블록마다 고른다는 뜻의 값.
     */
    static final int SWITCHABLE = 4;

    /**
     * True if this header only shows a frame of the reference store again (show_existing_frame).
     * <p>
     * 이 헤더가 참조 저장소의 프레임을 다시 표시하기만 하면 true(show_existing_frame).
     */
    boolean showExistingFrame;

    /**
     * The slot of the frame to show again.
     * <p>
     * 다시 표시할 프레임의 슬롯.
     */
    int frameToShowMapIdx;

    /**
     * True for key frames and intra-only frames.
     * <p>
     * 키 프레임과 인트라 전용 프레임이면 true.
     */
    boolean frameIsIntra;

    /**
     * True if a frame that is not shown can be shown later.
     * <p>
     * 표시하지 않는 프레임을 나중에 표시할 수 있으면 true.
     */
    boolean showableFrame;

    /**
     * True if the frame is coded so that it can be decoded without the state of earlier frames.
     * <p>
     * 이전 프레임의 상태 없이 디코딩할 수 있게 부호화된 프레임이면 true.
     */
    boolean errorResilientMode;

    /**
     * The frame id of the frame (0 if the sequence has no frame ids).
     * <p>
     * 프레임의 프레임 id(시퀀스에 프레임 id 가 없으면 0).
     */
    int currentFrameId;

    /**
     * The order hint of the frame (the least significant bits of its output order).
     * <p>
     * 프레임의 순서 힌트(출력 순서의 하위 비트).
     */
    int orderHint;

    /**
     * The index into the reference list (0..6) whose frame the current frame loads its state from, or {@link #PRIMARY_REF_NONE}.
     * <p>
     * 현재 프레임이 상태를 불러오는 프레임의 참조 목록 번호(0..6), 또는 {@link #PRIMARY_REF_NONE}.
     */
    int primaryRefFrame = PRIMARY_REF_NONE;

    /**
     * The reference slot that the frame loads its state from (null if there is none).
     * <p>
     * 프레임이 상태를 불러오는 참조 슬롯(없으면 null).
     */
    Av1RefFrame primaryRef;

    /**
     * The global motion parameters of the primary reference frame (null for the default).
     * <p>
     * 주 참조 프레임의 전역 움직임 매개변수(기본값이면 null).
     */
    int[][] prevGmParams;

    /**
     * The mask of the reference slots that the frame replaces with its picture.
     * <p>
     * 프레임이 자신의 화면으로 바꾸는 참조 슬롯의 마스크.
     */
    int refreshFrameFlags;

    /**
     * The slot of each of the seven references (LAST_FRAME is index 0).
     * <p>
     * 참조 일곱 개의 슬롯(LAST_FRAME 이 0번).
     */
    final int[] refFrameIdx = new int[7];

    /**
     * The interpolation filter of the frame (0..3, or {@link #SWITCHABLE}).
     * <p>
     * 프레임의 보간 필터(0..3, 또는 {@link #SWITCHABLE}).
     */
    int interpolationFilter;

    /**
     * True if blocks may use overlapped or warped motion compensation.
     * <p>
     * 블록이 겹침 또는 워프 움직임 보상을 쓸 수 있으면 true.
     */
    boolean isMotionModeSwitchable;

    /**
     * The order hints of the references, indexed by the reference frame (1..7).
     * <p>
     * 참조의 순서 힌트, 참조 프레임(1..7)으로 색인.
     */
    final int[] orderHints = new int[8];

    /**
     * True if the CDFs that the tiles adapted are not saved at the end of the frame.
     * <p>
     * 타일이 적응시킨 CDF 를 프레임 끝에서 저장하지 않으면 true.
     */
    boolean disableFrameEndUpdateCdf;

    /**
     * True if the segmentation map is coded in this frame.
     * <p>
     * 분할 맵을 이 프레임에서 부호화하면 true.
     */
    boolean segmentationUpdateMap;

    /**
     * True if the segmentation map is predicted from the previous frame.
     * <p>
     * 분할 맵을 이전 프레임에서 예측하면 true.
     */
    boolean segmentationTemporalUpdate;

    /**
     * True if the segmentation features are coded in this frame.
     * <p>
     * 분할 기능을 이 프레임에서 부호화하면 true.
     */
    boolean segmentationUpdateData;

    /**
     * True if blocks may use compound prediction.
     * <p>
     * 블록이 복합 예측을 쓸 수 있으면 true.
     */
    boolean referenceSelect;

    /**
     * True if blocks may use the skip mode (a compound prediction without any other information).
     * <p>
     * 블록이 건너뛰기 모드(다른 정보 없는 복합 예측)를 쓸 수 있으면 true.
     */
    boolean skipModePresent;

    /**
     * The two reference frames of the skip mode.
     * <p>
     * 건너뛰기 모드의 두 참조 프레임.
     */
    final int[] skipModeFrame = new int[2];

    /**
     * True if blocks may use the local warped motion.
     * <p>
     * 블록이 국소 워프 움직임을 쓸 수 있으면 true.
     */
    boolean allowWarpedMotion;

    /**
     * The temporal layer of the OBU that carried the header.
     * <p>
     * 헤더를 실은 OBU 의 시간 계층.
     */
    int temporalId;

    /**
     * The spatial layer of the OBU that carried the header.
     * <p>
     * 헤더를 실은 OBU 의 공간 계층.
     */
    int spatialId;

    /**
     * Frame type (key frame or intra-only frame).
     * <p>
     * 프레임 형식(키 프레임 또는 인트라 전용 프레임).
     */
    int frameType;

    /**
     * True if the frame is shown.
     * <p>
     * 프레임을 표시하면 true.
     */
    boolean showFrame = true;

    /**
     * True if the adaptation of the cumulative distributions is disabled.
     * <p>
     * 누적 분포의 적응을 끄면 true.
     */
    boolean disableCdfUpdate;

    /**
     * True if screen content coding tools (palette, intra block copy) may be used.
     * <p>
     * 화면 콘텐츠 부호화 도구(팔레트, 인트라 블록 복사)를 쓸 수 있으면 true.
     */
    boolean allowScreenContentTools;

    /**
     * True if intra block copy may be used.
     * <p>
     * 인트라 블록 복사를 쓸 수 있으면 true.
     */
    boolean allowIntrabc;

    /**
     * True if all motion vectors are integers (always true in intra frames).
     * <p>
     * 모든 움직임 벡터가 정수이면 true(인트라 프레임에서는 항상 true).
     */
    boolean forceIntegerMv;

    /**
     * True if motion vectors may have 1/8 sample precision (false in intra frames).
     * <p>
     * 움직임 벡터가 1/8 샘플 정밀도일 수 있으면 true(인트라 프레임에서는 false).
     */
    boolean allowHighPrecisionMv;

    /**
     * True if the motion vectors of earlier frames are projected for the motion vector prediction.
     * <p>
     * 움직임 벡터 예측에 이전 프레임의 움직임 벡터를 투영해 쓰면 true.
     */
    boolean useRefFrameMvs;

    /**
     * The type of the global motion of each reference frame (0 identity, 1 translation, 2 rotation and zoom, 3 affine).
     * <p>
     * 참조 프레임별 전역 움직임 종류(0 항등, 1 이동, 2 회전과 확대, 3 아핀).
     */
    final int[] gmType = new int[8];

    /**
     * The parameters of the global motion model of each reference frame.
     * <p>
     * 참조 프레임별 전역 움직임 모델의 매개변수.
     */
    final int[][] gmParams = identityParams();

    /**
     * True for each reference frame that is after the current frame in the display order.
     * <p>
     * 표시 순서에서 현재 프레임 뒤에 있는 참조 프레임마다 true.
     */
    final boolean[] refFrameSignBias = new boolean[8];

    /**
     * Returns the identity global motion parameters of all eight reference frames.
     * <p>
     * 참조 프레임 여덟 개의 항등 전역 움직임 매개변수를 반환한다.
     *
     * @return the parameters / 매개변수
     */
    static int[][] identityParams() {
        int[][] g = new int[8][6];
        // 모든 참조 프레임의 모델을 항등 변환으로 둔다.
        for (int i = 0; i < 8; i++) {
            g[i][2] = 1 << 16;
            g[i][5] = 1 << 16;
        }
        return g;
    }

    /**
     * The width of the frame as coded (before super resolution).
     * <p>
     * 부호화된 프레임 너비(슈퍼 해상도 전).
     */
    int frameWidth;

    /**
     * The height of the frame.
     * <p>
     * 프레임 높이.
     */
    int frameHeight;

    /**
     * The width after super resolution upscaling.
     * <p>
     * 슈퍼 해상도 확대 뒤의 너비.
     */
    int upscaledWidth;

    /**
     * The render width signalled in the header.
     * <p>
     * 헤더에 신호된 렌더 너비.
     */
    int renderWidth;

    /**
     * The render height signalled in the header.
     * <p>
     * 헤더에 신호된 렌더 높이.
     */
    int renderHeight;

    /**
     * True if super resolution is used.
     * <p>
     * 슈퍼 해상도를 쓰면 true.
     */
    boolean useSuperres;

    /**
     * The super resolution denominator (8 means no scaling).
     * <p>
     * 슈퍼 해상도 분모(8 이면 확대 없음).
     */
    int superresDenom = 8;

    /**
     * Number of 4x4 columns (the width in units of 4 samples, rounded up to a multiple of 8 samples).
     * <p>
     * 4x4 열 수(너비를 4 샘플 단위로, 8 샘플의 배수로 올림).
     */
    int miCols;

    /**
     * Number of 4x4 rows.
     * <p>
     * 4x4 행 수.
     */
    int miRows;

    /**
     * Number of tile columns.
     * <p>
     * 타일 열 수.
     */
    int tileCols;

    /**
     * Number of tile rows.
     * <p>
     * 타일 행 수.
     */
    int tileRows;

    /**
     * Base 2 logarithm of the tile columns.
     * <p>
     * 타일 열 수의 밑 2 로그.
     */
    int tileColsLog2;

    /**
     * Base 2 logarithm of the tile rows.
     * <p>
     * 타일 행 수의 밑 2 로그.
     */
    int tileRowsLog2;

    /**
     * The first 4x4 column of each tile column (plus the end).
     * <p>
     * 타일 열마다 첫 4x4 열(끝 포함).
     */
    int[] miColStarts = new int[65];

    /**
     * The first 4x4 row of each tile row (plus the end).
     * <p>
     * 타일 행마다 첫 4x4 행(끝 포함).
     */
    int[] miRowStarts = new int[65];

    /**
     * The tile whose final CDFs are saved for later frames.
     * <p>
     * 최종 CDF 를 이후 프레임용으로 저장하는 타일.
     */
    int contextUpdateTileId;

    /**
     * Number of bytes of a tile size field.
     * <p>
     * 타일 크기 필드의 바이트 수.
     */
    int tileSizeBytes = 4;

    /**
     * The base quantizer index.
     * <p>
     * 기본 양자화 지수.
     */
    int baseQIdx;

    /**
     * Quantizer delta of the luma DC coefficient.
     * <p>
     * 휘도 DC 계수의 양자화 변화량.
     */
    int deltaQYDc;

    /**
     * Quantizer delta of the U DC coefficient.
     * <p>
     * U DC 계수의 양자화 변화량.
     */
    int deltaQUDc;

    /**
     * Quantizer delta of the U AC coefficients.
     * <p>
     * U AC 계수의 양자화 변화량.
     */
    int deltaQUAc;

    /**
     * Quantizer delta of the V DC coefficient.
     * <p>
     * V DC 계수의 양자화 변화량.
     */
    int deltaQVDc;

    /**
     * Quantizer delta of the V AC coefficients.
     * <p>
     * V AC 계수의 양자화 변화량.
     */
    int deltaQVAc;

    /**
     * True if quantizer matrices are used.
     * <p>
     * 양자화 행렬을 쓰면 true.
     */
    boolean usingQmatrix;

    /**
     * The quantizer matrix level of the luma plane (qm_y).
     * <p>
     * 휘도 평면의 양자화 행렬 수준(qm_y).
     */
    int qmY;

    /**
     * The quantizer matrix level of the U plane (qm_u).
     * <p>
     * U 평면의 양자화 행렬 수준(qm_u).
     */
    int qmU;

    /**
     * The quantizer matrix level of the V plane (qm_v).
     * <p>
     * V 평면의 양자화 행렬 수준(qm_v).
     */
    int qmV;

    /**
     * True if segmentation is enabled.
     * <p>
     * 분할(segmentation)을 쓰면 true.
     */
    boolean segmentationEnabled;

    /**
     * Segmentation features enabled per segment: [segment][feature].
     * <p>
     * 세그먼트별 사용 중인 분할 기능: [세그먼트][기능].
     */
    boolean[][] featureEnabled = new boolean[8][8];

    /**
     * The data of the segmentation features: [segment][feature].
     * <p>
     * 분할 기능의 데이터: [세그먼트][기능].
     */
    int[][] featureData = new int[8][8];

    /**
     * True if the segment id is read before the skip flag.
     * <p>
     * 세그먼트 id 를 skip 플래그보다 먼저 읽으면 true.
     */
    boolean segIdPreSkip;

    /**
     * The largest segment id that has an active feature.
     * <p>
     * 활성 기능을 가진 가장 큰 세그먼트 id.
     */
    int lastActiveSegId;

    /**
     * True if the quantizer index may change per superblock.
     * <p>
     * 슈퍼블록마다 양자화 지수가 바뀔 수 있으면 true.
     */
    boolean deltaQPresent;

    /**
     * The resolution (shift) of the quantizer index deltas.
     * <p>
     * 양자화 지수 변화량의 해상도(시프트).
     */
    int deltaQRes;

    /**
     * True if the loop filter level may change per superblock.
     * <p>
     * 슈퍼블록마다 루프 필터 수준이 바뀔 수 있으면 true.
     */
    boolean deltaLfPresent;

    /**
     * The resolution (shift) of the loop filter level deltas.
     * <p>
     * 루프 필터 수준 변화량의 해상도(시프트).
     */
    int deltaLfRes;

    /**
     * True if separate loop filter deltas are sent for each edge direction and plane.
     * <p>
     * 가장자리 방향과 평면마다 따로 루프 필터 변화량을 보내면 true.
     */
    boolean deltaLfMulti;

    /**
     * True if all segments are lossless.
     * <p>
     * 모든 세그먼트가 무손실이면 true.
     */
    boolean codedLossless;

    /**
     * True if the frame is lossless and not scaled.
     * <p>
     * 프레임이 무손실이고 확대되지 않았으면 true.
     */
    boolean allLossless;

    /**
     * The loop filter levels: vertical luma, horizontal luma, U, V.
     * <p>
     * 루프 필터 수준: 휘도 세로, 휘도 가로, U, V.
     */
    int[] loopFilterLevel = new int[4];

    /**
     * The loop filter sharpness (0 to 7).
     * <p>
     * 루프 필터 선명도(0~7).
     */
    int loopFilterSharpness;

    /**
     * True if loop filter level deltas by reference frame and mode are enabled.
     * <p>
     * 참조 프레임과 모드에 따른 루프 필터 수준 변화량을 쓰면 true.
     */
    boolean loopFilterDeltaEnabled;

    /**
     * The loop filter level deltas by reference frame.
     * <p>
     * 참조 프레임별 루프 필터 수준 변화량.
     */
    int[] loopFilterRefDeltas = { 1, 0, 0, 0, -1, 0, -1, -1 };

    /**
     * The loop filter level deltas by mode type.
     * <p>
     * 모드 종류별 루프 필터 수준 변화량.
     */
    int[] loopFilterModeDeltas = new int[2];

    /**
     * The number of bits of the CDEF index of a 64x64 block.
     * <p>
     * 64x64 블록의 CDEF 색인의 비트 수.
     */
    int cdefBits;

    /**
     * The CDEF damping.
     * <p>
     * CDEF 감쇠.
     */
    int cdefDamping = 3;

    /**
     * Luma primary strengths of the CDEF presets.
     * <p>
     * CDEF 프리셋의 휘도 주 강도.
     */
    int[] cdefYPriStrength = new int[8];

    /**
     * Luma secondary strengths of the CDEF presets.
     * <p>
     * CDEF 프리셋의 휘도 보조 강도.
     */
    int[] cdefYSecStrength = new int[8];

    /**
     * Chroma primary strengths of the CDEF presets.
     * <p>
     * CDEF 프리셋의 색차 주 강도.
     */
    int[] cdefUvPriStrength = new int[8];

    /**
     * Chroma secondary strengths of the CDEF presets.
     * <p>
     * CDEF 프리셋의 색차 보조 강도.
     */
    int[] cdefUvSecStrength = new int[8];

    /**
     * The loop restoration type of each plane.
     * <p>
     * 평면별 루프 복원 종류.
     */
    int[] frameRestorationType = new int[3];

    /**
     * True if any plane uses loop restoration.
     * <p>
     * 어느 평면이든 루프 복원을 쓰면 true.
     */
    boolean usesLr;

    /**
     * The size of a loop restoration unit of each plane in samples.
     * <p>
     * 평면별 루프 복원 단위의 크기(샘플).
     */
    int[] loopRestorationSize = new int[3];

    /**
     * The transform mode.
     * <p>
     * 변환 모드.
     */
    int txMode;

    /**
     * True if the reduced set of transform types is used.
     * <p>
     * 줄인 변환 종류 집합을 쓰면 true.
     */
    boolean reducedTxSet;

    /**
     * True if film grain is to be applied to the output.
     * <p>
     * 출력에 필름 그레인을 적용해야 하면 true.
     */
    boolean applyGrain;

    /**
     * The film grain parameters of the frame (null if no grain is applied).
     * <p>
     * 프레임의 필름 그레인 매개변수(그레인을 적용하지 않으면 null).
     */
    Av1FilmGrain filmGrain;

    /**
     * The quantizer index of a segment (the specification function get_qindex with ignoreDeltaQ equal to 1).
     * <p>
     * 세그먼트의 양자화 지수(ignoreDeltaQ 가 1 인 규격의 get_qindex 함수).
     *
     * @param segmentId the segment / 세그먼트
     * @return the index 0..255 / 지수 0..255
     */
    int qindexOfSegment(int segmentId) {
        if (segmentationEnabled && featureEnabled[segmentId][0]) {
            int q = baseQIdx + featureData[segmentId][0];
            return q < 0 ? 0 : (q > 255 ? 255 : q);
        }
        return baseQIdx;
    }

    /**
     * Parses the frame header (<code>uncompressed_header</code>, specification 5.9) of any frame type. The state of the reference frames that the
     * header refers to (sizes, order hints, loop filter and segmentation parameters, global motion parameters, film grain parameters) is read from
     * <code>refs</code>; the loading of the reference state is done here too, so the returned header holds the values that the frame uses.
     * <p>
     * 모든 프레임 형식의 프레임 헤더(<code>uncompressed_header</code>, 규격 5.9)를 해석한다. 헤더가 가리키는 참조 프레임의 상태(크기, 순서 힌트, 루프 필터와
     * 분할 매개변수, 전역 움직임 매개변수, 필름 그레인 매개변수)는 <code>refs</code> 에서 읽는다. 참조 상태의 불러오기도 여기서 하므로 반환한 헤더가 프레임이
     * 쓰는 값을 가진다.
     *
     * @param r          the reader positioned at the header / 헤더 위치의 읽기
     * @param seq        the sequence header / 시퀀스 헤더
     * @param refs       the reference frame store (null entries are empty slots) / 참조 프레임 저장소(null 은 빈 슬롯)
     * @param temporalId the temporal layer of the OBU / OBU 의 시간 계층
     * @param spatialId  the spatial layer of the OBU / OBU 의 공간 계층
     * @return the header / 헤더
     * @throws MediaException CORRUPT for damaged data or references to empty slots / 손상된 데이터나 빈 슬롯 참조는 CORRUPT
     */
    static Av1FrameHeader parse(Av1BitReader r, Av1Sequence seq, Av1RefFrame[] refs, int temporalId, int spatialId) throws MediaException {
        Av1FrameHeader h = new Av1FrameHeader();
        h.temporalId = temporalId;
        h.spatialId = spatialId;
        int idLen = seq.frameIdNumbersPresent ? seq.additionalFrameIdLengthMinus1 + seq.deltaFrameIdLengthMinus2 + 3 : 0;
        boolean showable = false;
        if (seq.reducedStillPictureHeader) {
            h.frameType = KEY_FRAME;
            h.showFrame = true;
            h.errorResilientMode = true;
        } else {
            if (r.flag()) {
                // 이미 디코딩한 프레임을 다시 표시한다.
                h.showExistingFrame = true;
                h.frameToShowMapIdx = r.bits(3);
                if (seq.decoderModelInfoPresent && !seq.equalPictureInterval) {
                    r.bitsLong(seq.framePresentationTimeLengthMinus1 + 1);
                }
                if (seq.frameIdNumbersPresent) {
                    r.bits(idLen);
                }
                Av1RefFrame shown = refs[h.frameToShowMapIdx];
                if (shown == null) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: show_existing_frame of an empty reference slot");
                }
                h.frameType = shown.frameType;
                h.refreshFrameFlags = h.frameType == KEY_FRAME ? 0xFF : 0;
                if (seq.filmGrainParamsPresent) {
                    h.filmGrain = shown.filmGrain;
                    h.applyGrain = shown.filmGrain != null;
                }
                h.showFrame = true;
                return h;
            }
            h.frameType = r.bits(2);
            h.showFrame = r.flag();
            if (h.showFrame && seq.decoderModelInfoPresent && !seq.equalPictureInterval) {
                r.bitsLong(seq.framePresentationTimeLengthMinus1 + 1);
            }
            showable = h.showFrame ? h.frameType != KEY_FRAME : r.flag();
            h.errorResilientMode = h.frameType == SWITCH_FRAME || (h.frameType == KEY_FRAME && h.showFrame) ? true : r.flag();
        }
        h.showableFrame = showable;
        h.frameIsIntra = h.frameType == KEY_FRAME || h.frameType == INTRA_ONLY_FRAME;
        if (h.frameType == KEY_FRAME && h.showFrame) {
            // 표시되는 키 프레임은 모든 참조를 무효로 한다.
            java.util.Arrays.fill(refs, null);
        }
        h.disableCdfUpdate = r.flag();
        if (seq.forceScreenContentTools == Av1Sequence.SELECT) {
            h.allowScreenContentTools = r.flag();
        } else {
            h.allowScreenContentTools = seq.forceScreenContentTools != 0;
        }
        if (h.allowScreenContentTools) {
            h.forceIntegerMv = seq.forceIntegerMv == Av1Sequence.SELECT ? r.flag() : seq.forceIntegerMv != 0;
        }
        if (h.frameIsIntra) {
            h.forceIntegerMv = true;
        }
        if (seq.frameIdNumbersPresent) {
            h.currentFrameId = r.bits(idLen);
        }
        boolean frameSizeOverride;
        if (h.frameType == SWITCH_FRAME) {
            frameSizeOverride = true;
        } else if (seq.reducedStillPictureHeader) {
            frameSizeOverride = false;
        } else {
            frameSizeOverride = r.flag();
        }
        if (seq.orderHintBits > 0) {
            h.orderHint = r.bits(seq.orderHintBits);
        }
        h.primaryRefFrame = h.frameIsIntra || h.errorResilientMode ? PRIMARY_REF_NONE : r.bits(3);
        if (seq.decoderModelInfoPresent) {
            if (r.flag()) {
                // 동작 지점마다 해당하는 버퍼 제거 시각을 건너뛴다.
                for (int op = 0; op <= seq.operatingPointsCntMinus1; op++) {
                    if (seq.decoderModelPresentForThisOp[op]) {
                        int idc = seq.operatingPointIdc[op];
                        boolean inTemporal = ((idc >> temporalId) & 1) != 0;
                        boolean inSpatial = ((idc >> (spatialId + 8)) & 1) != 0;
                        if (idc == 0 || (inTemporal && inSpatial)) {
                            r.bitsLong(seq.bufferRemovalTimeLengthMinus1 + 1);
                        }
                    }
                }
            }
        }
        if (h.frameType == SWITCH_FRAME || (h.frameType == KEY_FRAME && h.showFrame)) {
            h.refreshFrameFlags = 0xFF;
        } else {
            h.refreshFrameFlags = r.bits(8);
        }
        if (h.frameType == INTRA_ONLY_FRAME && h.refreshFrameFlags == 0xFF) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: an intra-only frame refreshes all reference slots");
        }
        if (!h.frameIsIntra || h.refreshFrameFlags != 0xFF) {
            if (h.errorResilientMode && seq.enableOrderHint) {
                // 오류 복원 모드에서는 참조 슬롯의 기대 순서 힌트를 보낸다. 다르면 그 슬롯은 쓸 수 없다.
                for (int i = 0; i < 8; i++) {
                    int hint = r.bits(seq.orderHintBits);
                    if (refs[i] == null || refs[i].orderHint != hint) {
                        refs[i] = null;
                    }
                }
            }
        }
        if (h.frameIsIntra) {
            h.readFrameSize(r, seq, frameSizeOverride);
            h.readRenderSize(r);
            if (h.allowScreenContentTools && h.upscaledWidth == h.frameWidth) {
                h.allowIntrabc = r.flag();
            }
        } else {
            boolean shortSignaling = false;
            if (seq.enableOrderHint) {
                shortSignaling = r.flag();
                if (shortSignaling) {
                    int lastFrameIdx = r.bits(3);
                    int goldFrameIdx = r.bits(3);
                    h.setFrameRefs(seq, refs, lastFrameIdx, goldFrameIdx);
                }
            }
            // 참조 프레임 일곱 개의 슬롯 번호를 읽는다.
            for (int i = 0; i < 7; i++) {
                if (!shortSignaling) {
                    h.refFrameIdx[i] = r.bits(3);
                }
                if (seq.frameIdNumbersPresent) {
                    r.bits(seq.deltaFrameIdLengthMinus2 + 2);
                }
                if (refs[h.refFrameIdx[i]] == null) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: a reference to an empty reference slot");
                }
            }
            if (frameSizeOverride && !h.errorResilientMode) {
                h.readFrameSizeWithRefs(r, seq, refs);
            } else {
                h.readFrameSize(r, seq, frameSizeOverride);
                h.readRenderSize(r);
            }
            h.allowHighPrecisionMv = h.forceIntegerMv ? false : r.flag();
            h.interpolationFilter = r.flag() ? SWITCHABLE : r.bits(2);
            h.isMotionModeSwitchable = r.flag();
            h.useRefFrameMvs = h.errorResilientMode || !seq.enableRefFrameMvs ? false : r.flag();
            // 참조 프레임별 순서 힌트와 방향(부호 편향)을 정한다.
            for (int i = 0; i < 7; i++) {
                int hint = refs[h.refFrameIdx[i]].orderHint;
                h.orderHints[1 + i] = hint;
                h.refFrameSignBias[1 + i] = seq.enableOrderHint && relativeDist(seq, hint, h.orderHint) > 0;
            }
        }
        h.disableFrameEndUpdateCdf = seq.reducedStillPictureHeader || h.disableCdfUpdate ? true : r.flag();
        Av1RefFrame prev = null;
        // 이전 프레임의 상태를 불러온다(주 참조 프레임이 없으면 기본값).
        if (h.primaryRefFrame == PRIMARY_REF_NONE) {
            h.loopFilterDeltaEnabled = true;
        } else {
            prev = refs[h.refFrameIdx[h.primaryRefFrame]];
            h.prevGmParams = prev.savedGmParams;
            System.arraycopy(prev.lfRefDeltas, 0, h.loopFilterRefDeltas, 0, 8);
            System.arraycopy(prev.lfModeDeltas, 0, h.loopFilterModeDeltas, 0, 2);
            // 이전 프레임의 분할 기능을 복사한다.
            for (int i = 0; i < 8; i++) {
                System.arraycopy(prev.featureEnabled[i], 0, h.featureEnabled[i], 0, 8);
                System.arraycopy(prev.featureData[i], 0, h.featureData[i], 0, 8);
            }
        }
        h.primaryRef = prev;
        h.readTileInfo(r, seq);
        h.readQuantizationParams(r, seq);
        h.readSegmentationParams(r);
        if (h.baseQIdx > 0) {
            h.deltaQPresent = r.flag();
        }
        if (h.deltaQPresent) {
            h.deltaQRes = r.bits(2);
            if (!h.allowIntrabc) {
                h.deltaLfPresent = r.flag();
            }
            if (h.deltaLfPresent) {
                h.deltaLfRes = r.bits(2);
                h.deltaLfMulti = r.flag();
            }
        }
        h.codedLossless = true;
        // 모든 세그먼트가 무손실인지 확인한다.
        for (int s = 0; s < 8; s++) {
            int q = h.qindexOfSegment(s);
            boolean lossless = q == 0 && h.deltaQYDc == 0 && h.deltaQUAc == 0 && h.deltaQUDc == 0 && h.deltaQVAc == 0 && h.deltaQVDc == 0;
            if (!lossless) {
                h.codedLossless = false;
            }
        }
        h.allLossless = h.codedLossless && h.frameWidth == h.upscaledWidth;
        h.readLoopFilterParams(r, seq);
        h.readCdefParams(r, seq);
        h.readLrParams(r, seq);
        if (h.codedLossless) {
            h.txMode = ONLY_4X4;
        } else {
            h.txMode = r.flag() ? TX_MODE_SELECT : TX_MODE_LARGEST;
        }
        h.referenceSelect = h.frameIsIntra ? false : r.flag();
        h.readSkipModeParams(r, seq, refs);
        h.allowWarpedMotion = h.frameIsIntra || h.errorResilientMode || !seq.enableWarpedMotion ? false : r.flag();
        h.reducedTxSet = r.flag();
        h.readGlobalMotionParams(r);
        h.readFilmGrainParams(r, seq, showable, refs);
        return h;
    }

    /**
     * The distance between two order hints (<code>get_relative_dist</code>).
     * <p>
     * 두 순서 힌트의 거리(<code>get_relative_dist</code>).
     *
     * @param seq the sequence header / 시퀀스 헤더
     * @param a   the first hint / 첫 힌트
     * @param b   the second hint / 둘째 힌트
     * @return the signed distance / 부호 있는 거리
     */
    static int relativeDist(Av1Sequence seq, int a, int b) {
        if (!seq.enableOrderHint) {
            return 0;
        }
        int diff = a - b;
        int m = 1 << (seq.orderHintBits - 1);
        return (diff & (m - 1)) - (diff & m);
    }

    /**
     * The set frame refs process (specification 7.8): derives the slots of the seven references from the two that are signalled.
     * <p>
     * 참조 프레임 설정 과정(규격 7.8): 신호된 두 참조로부터 일곱 참조의 슬롯을 정한다.
     *
     * @param seq          the sequence header / 시퀀스 헤더
     * @param refs         the reference slots / 참조 슬롯
     * @param lastFrameIdx the slot of LAST_FRAME / LAST_FRAME 의 슬롯
     * @param goldFrameIdx the slot of GOLDEN_FRAME / GOLDEN_FRAME 의 슬롯
     * @throws MediaException CORRUPT if a slot is empty / 슬롯이 비었으면 CORRUPT
     */
    private void setFrameRefs(Av1Sequence seq, Av1RefFrame[] refs, int lastFrameIdx, int goldFrameIdx) throws MediaException {
        // 참조 일곱 개를 아직 정해지지 않은 상태로 초기화한다.
        for (int i = 0; i < 7; i++) {
            refFrameIdx[i] = -1;
        }
        refFrameIdx[0] = lastFrameIdx;
        refFrameIdx[3] = goldFrameIdx;
        boolean[] used = new boolean[8];
        used[lastFrameIdx] = true;
        used[goldFrameIdx] = true;
        int curFrameHint = 1 << (seq.orderHintBits - 1);
        int[] shifted = new int[8];
        // 현재 프레임의 힌트가 curFrameHint 가 되도록 각 슬롯의 힌트를 옮긴다.
        for (int i = 0; i < 8; i++) {
            if (refs[i] == null) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AV1: set_frame_refs with an empty reference slot");
            }
            shifted[i] = curFrameHint + relativeDist(seq, refs[i].orderHint, orderHint);
        }
        int ref = findBackward(shifted, used, curFrameHint, true);
        if (ref >= 0) {
            refFrameIdx[6] = ref;
            used[ref] = true;
        }
        ref = findBackward(shifted, used, curFrameHint, false);
        if (ref >= 0) {
            refFrameIdx[4] = ref;
            used[ref] = true;
        }
        ref = findBackward(shifted, used, curFrameHint, false);
        if (ref >= 0) {
            refFrameIdx[5] = ref;
            used[ref] = true;
        }
        int[] list = { 2, 3, 5, 6, 7 };
        // 남은 참조는 시간 순서가 늦은 전방 참조부터 차례로 채운다.
        for (int i = 0; i < 5; i++) {
            int refFrame = list[i];
            if (refFrameIdx[refFrame - 1] < 0) {
                ref = -1;
                int latest = 0;
                // 사용하지 않은 슬롯 중 현재보다 앞서고 가장 늦은 것을 찾는다.
                for (int j = 0; j < 8; j++) {
                    int hint = shifted[j];
                    if (!used[j] && hint < curFrameHint && (ref < 0 || hint >= latest)) {
                        ref = j;
                        latest = hint;
                    }
                }
                if (ref >= 0) {
                    refFrameIdx[refFrame - 1] = ref;
                    used[ref] = true;
                }
            }
        }
        ref = -1;
        int earliest = 0;
        // 마지막으로 남은 참조에는 출력 순서가 가장 빠른 슬롯을 쓴다.
        for (int i = 0; i < 8; i++) {
            if (ref < 0 || shifted[i] < earliest) {
                ref = i;
                earliest = shifted[i];
            }
        }
        // 정해지지 않은 참조는 출력 순서가 가장 이른 슬롯으로 채운다.
        for (int i = 0; i < 7; i++) {
            if (refFrameIdx[i] < 0) {
                refFrameIdx[i] = ref;
            }
        }
    }

    /**
     * Finds the unused slot with the latest or the earliest backward reference (<code>find_latest_backward</code>,
     * <code>find_earliest_backward</code>).
     * <p>
     * 사용하지 않은 슬롯 중 가장 늦거나 가장 이른 후방 참조를 찾는다(<code>find_latest_backward</code>, <code>find_earliest_backward</code>).
     *
     * @param shifted      the shifted order hints / 옮긴 순서 힌트
     * @param used         the slots that are used already / 이미 쓴 슬롯
     * @param curFrameHint the hint of the current frame / 현재 프레임의 힌트
     * @param latest       true for the latest, false for the earliest / 가장 늦은 것이면 true, 가장 이른 것이면 false
     * @return the slot or -1 / 슬롯 또는 -1
     */
    private static int findBackward(int[] shifted, boolean[] used, int curFrameHint, boolean latest) {
        int ref = -1;
        int best = 0;
        // 현재 프레임 뒤에 표시되는 슬롯 중에서 고른다.
        for (int i = 0; i < 8; i++) {
            int hint = shifted[i];
            if (!used[i] && hint >= curFrameHint) {
                if (ref < 0 || (latest ? hint >= best : hint < best)) {
                    ref = i;
                    best = hint;
                }
            }
        }
        return ref;
    }

    /**
     * Reads frame_size_with_refs().
     * <p>
     * frame_size_with_refs() 를 읽는다.
     *
     * @param r    the reader / 읽기
     * @param seq  the sequence header / 시퀀스 헤더
     * @param refs the reference slots / 참조 슬롯
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readFrameSizeWithRefs(Av1BitReader r, Av1Sequence seq, Av1RefFrame[] refs) throws MediaException {
        boolean found = false;
        // 참조 프레임 중 크기를 그대로 쓰는 것이 있는지 읽는다.
        for (int i = 0; i < 7; i++) {
            if (r.flag()) {
                Av1RefFrame ref = refs[refFrameIdx[i]];
                upscaledWidth = ref.upscaledWidth;
                frameWidth = upscaledWidth;
                frameHeight = ref.frameHeight;
                renderWidth = ref.renderWidth;
                renderHeight = ref.renderHeight;
                found = true;
                break;
            }
        }
        if (!found) {
            readFrameSize(r, seq, true);
            readRenderSize(r);
        } else {
            readSuperresParams(r, seq);
        }
    }

    /**
     * Reads frame_size() and superres_params().
     * <p>
     * frame_size() 와 superres_params() 를 읽는다.
     *
     * @param r        the reader / 읽기
     * @param seq      the sequence header / 시퀀스 헤더
     * @param override true if the size is signalled in the frame header / 크기를 프레임 헤더에서 보내면 true
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readFrameSize(Av1BitReader r, Av1Sequence seq, boolean override) throws MediaException {
        if (override) {
            frameWidth = r.bits(seq.frameWidthBitsMinus1 + 1) + 1;
            frameHeight = r.bits(seq.frameHeightBitsMinus1 + 1) + 1;
        } else {
            frameWidth = seq.maxFrameWidthMinus1 + 1;
            frameHeight = seq.maxFrameHeightMinus1 + 1;
        }
        readSuperresParams(r, seq);
    }

    /**
     * Reads superres_params() and computes the image size in 4x4 units.
     * <p>
     * superres_params() 를 읽고 4x4 단위 이미지 크기를 계산한다.
     *
     * @param r   the reader / 읽기
     * @param seq the sequence header / 시퀀스 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readSuperresParams(Av1BitReader r, Av1Sequence seq) throws MediaException {
        useSuperres = seq.enableSuperres && r.flag();
        if (useSuperres) {
            superresDenom = r.bits(3) + 9;
        } else {
            superresDenom = 8;
        }
        upscaledWidth = frameWidth;
        frameWidth = (upscaledWidth * 8 + (superresDenom / 2)) / superresDenom;
        miCols = 2 * ((frameWidth + 7) >> 3);
        miRows = 2 * ((frameHeight + 7) >> 3);
    }

    /**
     * Reads render_size().
     * <p>
     * render_size() 를 읽는다.
     *
     * @param r the reader / 읽기
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readRenderSize(Av1BitReader r) throws MediaException {
        if (r.flag()) {
            renderWidth = r.bits(16) + 1;
            renderHeight = r.bits(16) + 1;
        } else {
            renderWidth = upscaledWidth;
            renderHeight = frameHeight;
        }
    }

    /**
     * Reads skip_mode_params().
     * <p>
     * skip_mode_params() 를 읽는다.
     *
     * @param r    the reader / 읽기
     * @param seq  the sequence header / 시퀀스 헤더
     * @param refs the reference slots / 참조 슬롯
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readSkipModeParams(Av1BitReader r, Av1Sequence seq, Av1RefFrame[] refs) throws MediaException {
        boolean skipModeAllowed;
        if (frameIsIntra || !referenceSelect || !seq.enableOrderHint) {
            skipModeAllowed = false;
        } else {
            int forwardIdx = -1;
            int backwardIdx = -1;
            int forwardHint = 0;
            int backwardHint = 0;
            // 참조 프레임 일곱 개 중 가장 가까운 전방 참조와 후방 참조를 찾는다.
            for (int i = 0; i < 7; i++) {
                int refHint = refs[refFrameIdx[i]].orderHint;
                if (relativeDist(seq, refHint, orderHint) < 0) {
                    if (forwardIdx < 0 || relativeDist(seq, refHint, forwardHint) > 0) {
                        forwardIdx = i;
                        forwardHint = refHint;
                    }
                } else if (relativeDist(seq, refHint, orderHint) > 0) {
                    if (backwardIdx < 0 || relativeDist(seq, refHint, backwardHint) < 0) {
                        backwardIdx = i;
                        backwardHint = refHint;
                    }
                }
            }
            if (forwardIdx < 0) {
                skipModeAllowed = false;
            } else if (backwardIdx >= 0) {
                skipModeAllowed = true;
                skipModeFrame[0] = 1 + Math.min(forwardIdx, backwardIdx);
                skipModeFrame[1] = 1 + Math.max(forwardIdx, backwardIdx);
            } else {
                int secondForwardIdx = -1;
                int secondForwardHint = 0;
                // 후방 참조가 없으면 두 번째로 가까운 전방 참조를 찾는다.
                for (int i = 0; i < 7; i++) {
                    int refHint = refs[refFrameIdx[i]].orderHint;
                    if (relativeDist(seq, refHint, forwardHint) < 0) {
                        if (secondForwardIdx < 0 || relativeDist(seq, refHint, secondForwardHint) > 0) {
                            secondForwardIdx = i;
                            secondForwardHint = refHint;
                        }
                    }
                }
                if (secondForwardIdx < 0) {
                    skipModeAllowed = false;
                } else {
                    skipModeAllowed = true;
                    skipModeFrame[0] = 1 + Math.min(forwardIdx, secondForwardIdx);
                    skipModeFrame[1] = 1 + Math.max(forwardIdx, secondForwardIdx);
                }
            }
        }
        skipModePresent = skipModeAllowed && r.flag();
    }

    /**
     * Reads global_motion_params().
     * <p>
     * global_motion_params() 를 읽는다.
     *
     * @param r the reader / 읽기
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readGlobalMotionParams(Av1BitReader r) throws MediaException {
        if (frameIsIntra) {
            return;
        }
        int[][] prevGm = prevGmParams != null ? prevGmParams : identityParams();
        // 참조 프레임마다 전역 움직임 모델의 종류와 매개변수를 읽는다.
        for (int ref = 1; ref <= 7; ref++) {
            int type;
            if (r.flag()) {
                if (r.flag()) {
                    type = 2;
                } else {
                    type = r.flag() ? 1 : 3;
                }
            } else {
                type = 0;
            }
            gmType[ref] = type;
            if (type >= 2) {
                gmParams[ref][2] = readGlobalParam(r, type, ref, 2, prevGm);
                gmParams[ref][3] = readGlobalParam(r, type, ref, 3, prevGm);
                if (type == 3) {
                    gmParams[ref][4] = readGlobalParam(r, type, ref, 4, prevGm);
                    gmParams[ref][5] = readGlobalParam(r, type, ref, 5, prevGm);
                } else {
                    gmParams[ref][4] = -gmParams[ref][3];
                    gmParams[ref][5] = gmParams[ref][2];
                }
            }
            if (type >= 1) {
                gmParams[ref][0] = readGlobalParam(r, type, ref, 0, prevGm);
                gmParams[ref][1] = readGlobalParam(r, type, ref, 1, prevGm);
            }
        }
    }

    /**
     * Reads one global motion parameter (<code>read_global_param</code>).
     * <p>
     * 전역 움직임 매개변수 하나를 읽는다(<code>read_global_param</code>).
     *
     * @param r      the reader / 읽기
     * @param type   the model type / 모델 종류
     * @param ref    the reference frame / 참조 프레임
     * @param idx    the parameter index / 매개변수 번호
     * @param prevGm the parameters of the previous frame / 이전 프레임의 매개변수
     * @return the parameter / 매개변수
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private int readGlobalParam(Av1BitReader r, int type, int ref, int idx, int[][] prevGm) throws MediaException {
        int absBits = 12;
        int precBits = 15;
        if (idx < 2) {
            if (type == 1) {
                absBits = 9 - (allowHighPrecisionMv ? 0 : 1);
                precBits = 3 - (allowHighPrecisionMv ? 0 : 1);
            } else {
                absBits = 12;
                precBits = 6;
            }
        }
        int precDiff = 16 - precBits;
        int round = (idx % 3) == 2 ? (1 << 16) : 0;
        int sub = (idx % 3) == 2 ? (1 << precBits) : 0;
        int mx = 1 << absBits;
        int rr = (prevGm[ref][idx] >> precDiff) - sub;
        return (decodeSignedSubexpWithRef(r, -mx, mx + 1, rr) << precDiff) + round;
    }

    /**
     * Reads a signed value coded relative to a reference value (<code>decode_signed_subexp_with_ref</code>).
     * <p>
     * 기준 값에 대한 상대 값으로 부호화된 부호 있는 값을 읽는다(<code>decode_signed_subexp_with_ref</code>).
     *
     * @param r    the reader / 읽기
     * @param low  the smallest value / 최솟값
     * @param high the largest value plus one / 최댓값 + 1
     * @param ref  the reference value / 기준 값
     * @return the value / 값
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private static int decodeSignedSubexpWithRef(Av1BitReader r, int low, int high, int ref) throws MediaException {
        int mx = high - low;
        int rr = ref - low;
        int v = decodeSubexp(r, mx);
        int x;
        if ((rr << 1) <= mx) {
            x = inverseRecenter(rr, v);
        } else {
            x = mx - 1 - inverseRecenter(mx - 1 - rr, v);
        }
        return x + low;
    }

    /**
     * Reads a value of the sub-exponential code (<code>decode_subexp</code>).
     * <p>
     * 지수 부분 부호의 값을 읽는다(<code>decode_subexp</code>).
     *
     * @param r       the reader / 읽기
     * @param numSyms the number of symbols / 기호 수
     * @return the value / 값
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private static int decodeSubexp(Av1BitReader r, int numSyms) throws MediaException {
        int i = 0;
        int mk = 0;
        int k = 3;
        // 값이 들어갈 구간을 찾을 때까지 구간 크기를 키운다.
        while (true) {
            int b2 = i != 0 ? k + i - 1 : k;
            int a = 1 << b2;
            if (numSyms <= mk + 3 * a) {
                return r.ns(numSyms - mk) + mk;
            }
            if (r.flag()) {
                i++;
                mk += a;
            } else {
                return r.bits(b2) + mk;
            }
        }
    }

    /**
     * The inverse recentering function of the specification.
     * <p>
     * 규격의 역 재중심화 함수.
     *
     * @param r the reference / 기준
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
     * The smallest k such that blkSize shifted left by k is not less than target (tile_log2).
     * <p>
     * blkSize 를 k 만큼 왼쪽으로 민 값이 target 이상이 되는 가장 작은 k(tile_log2).
     *
     * @param blkSize the block size / 블록 크기
     * @param target  the target / 목표
     * @return k / k
     */
    private static int tileLog2(int blkSize, int target) {
        int k = 0;
        // 목표를 넘을 때까지 k 를 늘린다.
        while ((blkSize << k) < target) {
            k++;
        }
        return k;
    }

    /**
     * Reads tile_info().
     * <p>
     * tile_info() 를 읽는다.
     *
     * @param r   the reader / 읽기
     * @param seq the sequence header / 시퀀스 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readTileInfo(Av1BitReader r, Av1Sequence seq) throws MediaException {
        int sbCols = seq.use128x128Superblock ? ((miCols + 31) >> 5) : ((miCols + 15) >> 4);
        int sbRows = seq.use128x128Superblock ? ((miRows + 31) >> 5) : ((miRows + 15) >> 4);
        int sbShift = seq.use128x128Superblock ? 5 : 4;
        int sbSize = sbShift + 2;
        int maxTileWidthSb = 4096 >> sbSize;
        int maxTileAreaSb = (4096 * 2304) >> (2 * sbSize);
        int minLog2TileCols = tileLog2(maxTileWidthSb, sbCols);
        int maxLog2TileCols = tileLog2(1, Math.min(sbCols, 64));
        int maxLog2TileRows = tileLog2(1, Math.min(sbRows, 64));
        int minLog2Tiles = Math.max(minLog2TileCols, tileLog2(maxTileAreaSb, sbRows * sbCols));
        boolean uniform = r.flag();
        if (uniform) {
            tileColsLog2 = minLog2TileCols;
            // 타일 열 수의 로그를 하나씩 늘릴 수 있다.
            while (tileColsLog2 < maxLog2TileCols) {
                if (r.flag()) {
                    tileColsLog2++;
                } else {
                    break;
                }
            }
            int tileWidthSb = (sbCols + (1 << tileColsLog2) - 1) >> tileColsLog2;
            int i = 0;
            // 균일한 폭으로 타일 열의 시작 위치를 정한다.
            for (int startSb = 0; startSb < sbCols; startSb += tileWidthSb) {
                miColStarts[i++] = startSb << sbShift;
            }
            miColStarts[i] = miCols;
            tileCols = i;
            int minLog2TileRows = Math.max(minLog2Tiles - tileColsLog2, 0);
            tileRowsLog2 = minLog2TileRows;
            // 타일 행 수의 로그도 하나씩 늘릴 수 있다.
            while (tileRowsLog2 < maxLog2TileRows) {
                if (r.flag()) {
                    tileRowsLog2++;
                } else {
                    break;
                }
            }
            int tileHeightSb = (sbRows + (1 << tileRowsLog2) - 1) >> tileRowsLog2;
            i = 0;
            // 균일한 높이로 타일 행의 시작 위치를 정한다.
            for (int startSb = 0; startSb < sbRows; startSb += tileHeightSb) {
                miRowStarts[i++] = startSb << sbShift;
            }
            miRowStarts[i] = miRows;
            tileRows = i;
        } else {
            int widestTileSb = 0;
            int startSb = 0;
            int i;
            // 타일 열마다 폭(슈퍼블록 수)을 읽는다.
            for (i = 0; startSb < sbCols; i++) {
                if (i >= 64) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "too many AV1 tile columns");
                }
                miColStarts[i] = startSb << sbShift;
                int maxWidth = Math.min(sbCols - startSb, maxTileWidthSb);
                int sizeSb = r.ns(maxWidth) + 1;
                widestTileSb = Math.max(sizeSb, widestTileSb);
                startSb += sizeSb;
            }
            miColStarts[i] = miCols;
            tileCols = i;
            tileColsLog2 = tileLog2(1, tileCols);
            int maxArea = minLog2Tiles > 0 ? (sbRows * sbCols) >> (minLog2Tiles + 1) : sbRows * sbCols;
            int maxTileHeightSb = Math.max(maxArea / widestTileSb, 1);
            startSb = 0;
            // 타일 행마다 높이(슈퍼블록 수)를 읽는다.
            for (i = 0; startSb < sbRows; i++) {
                if (i >= 64) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "too many AV1 tile rows");
                }
                miRowStarts[i] = startSb << sbShift;
                int maxHeight = Math.min(sbRows - startSb, maxTileHeightSb);
                int sizeSb = r.ns(maxHeight) + 1;
                startSb += sizeSb;
            }
            miRowStarts[i] = miRows;
            tileRows = i;
            tileRowsLog2 = tileLog2(1, tileRows);
        }
        if (tileColsLog2 > 0 || tileRowsLog2 > 0) {
            contextUpdateTileId = r.bits(tileRowsLog2 + tileColsLog2);
            tileSizeBytes = r.bits(2) + 1;
        } else {
            contextUpdateTileId = 0;
        }
    }

    /**
     * Reads a quantizer delta, <code>read_delta_q()</code>.
     * <p>
     * 양자화 변화량 <code>read_delta_q()</code> 를 읽는다.
     *
     * @param r the reader / 읽기
     * @return the delta / 변화량
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private static int readDeltaQ(Av1BitReader r) throws MediaException {
        return r.flag() ? r.su(7) : 0;
    }

    /**
     * Reads quantization_params().
     * <p>
     * quantization_params() 를 읽는다.
     *
     * @param r   the reader / 읽기
     * @param seq the sequence header / 시퀀스 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readQuantizationParams(Av1BitReader r, Av1Sequence seq) throws MediaException {
        baseQIdx = r.bits(8);
        deltaQYDc = readDeltaQ(r);
        if (seq.numPlanes > 1) {
            boolean diffUvDelta = seq.separateUvDeltaQ && r.flag();
            deltaQUDc = readDeltaQ(r);
            deltaQUAc = readDeltaQ(r);
            if (diffUvDelta) {
                deltaQVDc = readDeltaQ(r);
                deltaQVAc = readDeltaQ(r);
            } else {
                deltaQVDc = deltaQUDc;
                deltaQVAc = deltaQUAc;
            }
        }
        usingQmatrix = r.flag();
        if (usingQmatrix) {
            qmY = r.bits(4);
            qmU = r.bits(4);
            qmV = seq.separateUvDeltaQ ? r.bits(4) : qmU;
        }
    }

    /**
     * The level of the quantizer matrix of a plane and a segment (<code>SegQMLevel</code>; 15 means that no matrix is applied).
     * <p>
     * 평면과 세그먼트의 양자화 행렬 수준(<code>SegQMLevel</code>. 15 는 행렬을 적용하지 않음을 뜻한다).
     *
     * @param plane     the plane / 평면
     * @param segmentId the segment / 세그먼트
     * @return the level 0..15 / 수준 0~15
     */
    int qmLevel(int plane, int segmentId) {
        if (!usingQmatrix) {
            return 15;
        }
        boolean lossless = qindexOfSegment(segmentId) == 0 && deltaQYDc == 0 && deltaQUAc == 0 && deltaQUDc == 0 && deltaQVAc == 0
                && deltaQVDc == 0;
        if (lossless) {
            return 15;
        }
        return plane == 0 ? qmY : (plane == 1 ? qmU : qmV);
    }

    /**
     * Reads segmentation_params() for a frame without a primary reference frame.
     * <p>
     * 주 참조 프레임이 없는 프레임의 segmentation_params() 를 읽는다.
     *
     * @param r the reader / 읽기
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readSegmentationParams(Av1BitReader r) throws MediaException {
        segmentationEnabled = r.flag();
        if (segmentationEnabled) {
            if (primaryRefFrame == PRIMARY_REF_NONE) {
                segmentationUpdateMap = true;
                segmentationTemporalUpdate = false;
                segmentationUpdateData = true;
            } else {
                segmentationUpdateMap = r.flag();
                if (segmentationUpdateMap) {
                    segmentationTemporalUpdate = r.flag();
                }
                segmentationUpdateData = r.flag();
            }
            if (segmentationUpdateData) {
                int[] bitsOf = { 8, 6, 6, 6, 6, 3, 0, 0 };
                boolean[] signed = { true, true, true, true, true, false, false, false };
                int[] max = { 255, 63, 63, 63, 63, 7, 0, 0 };
                // 세그먼트마다 기능 8개의 사용 여부와 값을 읽는다.
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        boolean enabled = r.flag();
                        featureEnabled[i][j] = enabled;
                        int clipped = 0;
                        if (enabled) {
                            if (signed[j]) {
                                int v = r.su(1 + bitsOf[j]);
                                clipped = Math.max(-max[j], Math.min(max[j], v));
                            } else {
                                int v = r.bits(bitsOf[j]);
                                clipped = Math.max(0, Math.min(max[j], v));
                            }
                        }
                        featureData[i][j] = clipped;
                    }
                }
            }
        } else {
            // 분할을 쓰지 않으면 모든 기능을 끈다.
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    featureEnabled[i][j] = false;
                    featureData[i][j] = 0;
                }
            }
        }
        segIdPreSkip = false;
        lastActiveSegId = 0;
        // 활성 기능이 있는 마지막 세그먼트와 skip 이전 읽기 여부를 정한다.
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (featureEnabled[i][j]) {
                    lastActiveSegId = i;
                    if (j >= 5) {
                        segIdPreSkip = true;
                    }
                }
            }
        }
    }

    /**
     * Reads loop_filter_params().
     * <p>
     * loop_filter_params() 를 읽는다.
     *
     * @param r   the reader / 읽기
     * @param seq the sequence header / 시퀀스 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readLoopFilterParams(Av1BitReader r, Av1Sequence seq) throws MediaException {
        if (codedLossless || allowIntrabc) {
            // 필터를 쓰지 않는 프레임은 변화량을 기본값으로 되돌린다(이후 프레임이 불러올 수 있다).
            int[] defaults = { 1, 0, 0, 0, -1, 0, -1, -1 };
            System.arraycopy(defaults, 0, loopFilterRefDeltas, 0, 8);
            loopFilterModeDeltas[0] = 0;
            loopFilterModeDeltas[1] = 0;
            return;
        }
        loopFilterLevel[0] = r.bits(6);
        loopFilterLevel[1] = r.bits(6);
        if (seq.numPlanes > 1 && (loopFilterLevel[0] != 0 || loopFilterLevel[1] != 0)) {
            loopFilterLevel[2] = r.bits(6);
            loopFilterLevel[3] = r.bits(6);
        }
        loopFilterSharpness = r.bits(3);
        loopFilterDeltaEnabled = r.flag();
        if (loopFilterDeltaEnabled) {
            if (r.flag()) {
                // 참조 프레임별 변화량과 모드별 변화량을 갱신할 수 있다.
                for (int i = 0; i < 8; i++) {
                    if (r.flag()) {
                        loopFilterRefDeltas[i] = r.su(7);
                    }
                }
                // 두 가지 항목(예: 필터 방향)마다 같은 값을 읽는다.
                for (int i = 0; i < 2; i++) {
                    if (r.flag()) {
                        loopFilterModeDeltas[i] = r.su(7);
                    }
                }
            }
        }
    }

    /**
     * Reads cdef_params().
     * <p>
     * cdef_params() 를 읽는다.
     *
     * @param r   the reader / 읽기
     * @param seq the sequence header / 시퀀스 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readCdefParams(Av1BitReader r, Av1Sequence seq) throws MediaException {
        if (codedLossless || allowIntrabc || !seq.enableCdef) {
            cdefBits = 0;
            cdefDamping = 3;
            return;
        }
        cdefDamping = r.bits(2) + 3;
        cdefBits = r.bits(2);
        // 프리셋마다 휘도와 색차의 주/보조 강도를 읽는다.
        for (int i = 0; i < (1 << cdefBits); i++) {
            cdefYPriStrength[i] = r.bits(4);
            cdefYSecStrength[i] = r.bits(2);
            if (cdefYSecStrength[i] == 3) {
                cdefYSecStrength[i]++;
            }
            if (seq.numPlanes > 1) {
                cdefUvPriStrength[i] = r.bits(4);
                cdefUvSecStrength[i] = r.bits(2);
                if (cdefUvSecStrength[i] == 3) {
                    cdefUvSecStrength[i]++;
                }
            }
        }
    }

    /**
     * Reads lr_params().
     * <p>
     * lr_params() 를 읽는다.
     *
     * @param r   the reader / 읽기
     * @param seq the sequence header / 시퀀스 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readLrParams(Av1BitReader r, Av1Sequence seq) throws MediaException {
        usesLr = false;
        if (allLossless || allowIntrabc || !seq.enableRestoration) {
            return;
        }
        int[] remap = { RESTORE_NONE, RESTORE_SWITCHABLE, RESTORE_WIENER, RESTORE_SGRPROJ };
        boolean usesChromaLr = false;
        // 평면마다 복원 종류를 읽는다.
        for (int i = 0; i < seq.numPlanes; i++) {
            frameRestorationType[i] = remap[r.bits(2)];
            if (frameRestorationType[i] != RESTORE_NONE) {
                usesLr = true;
                if (i > 0) {
                    usesChromaLr = true;
                }
            }
        }
        if (usesLr) {
            int shift;
            if (seq.use128x128Superblock) {
                shift = r.bit() + 1;
            } else {
                shift = r.bit();
                if (shift != 0) {
                    shift += r.bit();
                }
            }
            loopRestorationSize[0] = 256 >> (2 - shift);
            int uvShift = 0;
            if (seq.subsamplingX != 0 && seq.subsamplingY != 0 && usesChromaLr) {
                uvShift = r.bit();
            }
            loopRestorationSize[1] = loopRestorationSize[0] >> uvShift;
            loopRestorationSize[2] = loopRestorationSize[0] >> uvShift;
        }
    }

    /**
     * Reads film_grain_params(): when the frame applies film grain, the parameters are kept in {@link #filmGrain} and the decoder adds the grain
     * to the output picture.
     * <p>
     * film_grain_params() 를 읽는다. 프레임이 필름 그레인을 적용하면 매개변수를 {@link #filmGrain} 에 보관하고 디코더가 출력 화면에 그레인을 더한다.
     *
     * @param r        the reader / 읽기
     * @param seq      the sequence header / 시퀀스 헤더
     * @param showable true if the frame is showable / 프레임이 표시 가능하면 true
     * @param refs     the reference slots / 참조 슬롯
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readFilmGrainParams(Av1BitReader r, Av1Sequence seq, boolean showable, Av1RefFrame[] refs) throws MediaException {
        applyGrain = false;
        filmGrain = null;
        if (!seq.filmGrainParamsPresent || (!showFrame && !showable)) {
            return;
        }
        applyGrain = r.flag();
        if (applyGrain) {
            filmGrain = Av1FilmGrain.parse(r, seq, frameType == INTER_FRAME, refs);
            applyGrain = filmGrain != null;
        }
    }
}
