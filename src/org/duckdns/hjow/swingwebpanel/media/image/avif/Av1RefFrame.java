package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * One slot of the reference frame store of the AV1 decoder (plan item P18-03; AV1 specification sections 7.20 and 7.21): the decoded picture
 * (after all filters, at the upscaled width) and everything else that later frames load from a reference: the size and format, the order
 * hints, the motion vectors that are projected into later frames, the segmentation map and parameters, the loop filter deltas, the global
 * motion parameters, the film grain parameters and the adapted CDFs.
 * <p>
 * AV1 디코더의 참조 프레임 저장소의 슬롯 하나(계획 P18-03; AV1 규격 7.20, 7.21절): 디코딩된 화면(모든 필터 뒤, 확대된 너비)과 이후 프레임이 참조에서
 * 읽어 오는 나머지 전부, 즉 크기와 형식, 순서 힌트, 이후 프레임에 투영되는 움직임 벡터, 분할 맵과 매개변수, 루프 필터 변화량, 전역 움직임
 * 매개변수, 필름 그레인 매개변수, 적응된 CDF.
 */
final class Av1RefFrame {

    /**
     * The frame id (current_frame_id) of the frame.
     * <p>
     * 프레임의 프레임 id(current_frame_id).
     */
    int frameId;

    /**
     * The width after the super resolution upscaling.
     * <p>
     * 초해상도 확대 뒤의 너비.
     */
    int upscaledWidth;

    /**
     * The width as coded (before the upscaling).
     * <p>
     * 부호화된 너비(확대 전).
     */
    int frameWidth;

    /**
     * The height of the frame.
     * <p>
     * 프레임 높이.
     */
    int frameHeight;

    /**
     * The render width.
     * <p>
     * 렌더 너비.
     */
    int renderWidth;

    /**
     * The render height.
     * <p>
     * 렌더 높이.
     */
    int renderHeight;

    /**
     * The number of 4x4 columns of the frame.
     * <p>
     * 프레임의 4x4 열 수.
     */
    int miCols;

    /**
     * The number of 4x4 rows of the frame.
     * <p>
     * 프레임의 4x4 행 수.
     */
    int miRows;

    /**
     * The frame type (key, inter, intra-only, switch).
     * <p>
     * 프레임 형식(키, 인터, 인트라 전용, 전환).
     */
    int frameType;

    /**
     * The horizontal chroma subsampling.
     * <p>
     * 가로 색차 하위 표본화.
     */
    int subX;

    /**
     * The vertical chroma subsampling.
     * <p>
     * 세로 색차 하위 표본화.
     */
    int subY;

    /**
     * The bit depth.
     * <p>
     * 비트 깊이.
     */
    int bitDepth;

    /**
     * The number of planes.
     * <p>
     * 평면 수.
     */
    int numPlanes;

    /**
     * The order hint of the frame.
     * <p>
     * 프레임의 순서 힌트.
     */
    int orderHint;

    /**
     * True if the frame may be shown later with show_existing_frame.
     * <p>
     * 나중에 show_existing_frame 으로 표시할 수 있으면 true.
     */
    boolean showable;

    /**
     * The order hints of the references of the frame, indexed by the reference frame (1..7).
     * <p>
     * 프레임이 쓴 참조들의 순서 힌트(참조 프레임 1..7로 색인).
     */
    final int[] savedOrderHints = new int[8];

    /**
     * The picture after all filters (upscaled), one array per plane.
     * <p>
     * 모든 필터 뒤의 화면(확대됨), 평면마다 배열 하나.
     */
    short[][] planes;

    /**
     * The row stride of each plane.
     * <p>
     * 평면별 행 간격.
     */
    int[] stride;

    /**
     * The reference frame of the stored motion vector of each 4x4 unit (0 or less if none), see the motion vector storage process.
     * <p>
     * 4x4 단위마다 저장한 움직임 벡터의 참조 프레임(없으면 0 이하). 움직임 벡터 저장 과정 참조.
     */
    byte[] savedRefFrames;

    /**
     * The stored motion vectors of each 4x4 unit: row, column.
     * <p>
     * 4x4 단위마다 저장한 움직임 벡터: 행, 열.
     */
    short[] savedMvs;

    /**
     * The row stride of the arrays {@link #savedRefFrames}, {@link #savedMvs} (two values per unit) and {@link #savedSegmentIds}.
     * <p>
     * 배열 {@link #savedRefFrames}, {@link #savedMvs}(단위당 두 값), {@link #savedSegmentIds} 의 행 간격.
     */
    int miStride;

    /**
     * The global motion parameters of the frame.
     * <p>
     * 프레임의 전역 움직임 매개변수.
     */
    int[][] savedGmParams;

    /**
     * The segmentation map of the frame, one value per 4x4 unit.
     * <p>
     * 프레임의 분할 맵, 4x4 단위마다 값 하나.
     */
    byte[] savedSegmentIds;

    /**
     * The adapted CDFs at the end of the frame.
     * <p>
     * 프레임이 끝날 때 적응된 CDF.
     */
    Av1Cdfs cdfs;

    /**
     * The film grain parameters of the frame (null if the frame has none).
     * <p>
     * 프레임의 필름 그레인 매개변수(없으면 null).
     */
    Av1FilmGrain filmGrain;

    /**
     * The loop filter deltas by reference frame.
     * <p>
     * 참조 프레임별 루프 필터 변화량.
     */
    int[] lfRefDeltas = new int[8];

    /**
     * The loop filter deltas by mode.
     * <p>
     * 모드별 루프 필터 변화량.
     */
    int[] lfModeDeltas = new int[2];

    /**
     * The segmentation features that are enabled: [segment][feature].
     * <p>
     * 사용 중인 분할 기능: [세그먼트][기능].
     */
    boolean[][] featureEnabled = new boolean[8][8];

    /**
     * The data of the segmentation features: [segment][feature].
     * <p>
     * 분할 기능의 데이터: [세그먼트][기능].
     */
    int[][] featureData = new int[8][8];
}
