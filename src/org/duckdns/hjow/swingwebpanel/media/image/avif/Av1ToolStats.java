package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * Counters of the coding tools that the AV1 decoder meets while it decodes (plan item P18-03). The tests use them to check that a test stream
 * really uses the tool that it is meant to cover (a bit exact result says little if the tool never occurred). The counters are plain static
 * integers that are only incremented; concurrent decoders may lose counts, which does not matter for their purpose.
 * <p>
 * AV1 디코더가 디코딩하며 만나는 부호화 도구의 카운터(계획 P18-03). 시험 스트림이 덮으려는 도구를 실제로 쓰는지 확인하는 데 시험이 쓴다(도구가 한 번도
 * 나오지 않으면 비트 단위 일치는 의미가 적다). 카운터는 증가만 하는 단순한 정적 정수이며 동시에 디코딩하면 일부를 놓칠 수 있으나 용도에는 문제가 없다.
 */
final class Av1ToolStats {

    /**
     * The index of the counter of overlapped block motion compensation blocks.
     * <p>
     * 겹침 블록 움직임 보상 블록 카운터의 번호.
     */
    static final int OBMC = 0;

    /**
     * The index of the counter of blocks with local warped motion.
     * <p>
     * 국소 워프 움직임 블록 카운터의 번호.
     */
    static final int LOCAL_WARP = 1;

    /**
     * The index of the counter of the predictions with a global motion model.
     * <p>
     * 전역 움직임 모델 예측 카운터의 번호.
     */
    static final int GLOBAL_WARP = 2;

    /**
     * The index of the counter of inter-intra blocks without a wedge.
     * <p>
     * 쐐기 없는 인터인트라 블록 카운터의 번호.
     */
    static final int INTERINTRA = 3;

    /**
     * The index of the counter of inter-intra blocks with a wedge.
     * <p>
     * 쐐기 있는 인터인트라 블록 카운터의 번호.
     */
    static final int INTERINTRA_WEDGE = 4;

    /**
     * The index of the counter of wedge compound blocks.
     * <p>
     * 쐐기 복합 블록 카운터의 번호.
     */
    static final int COMPOUND_WEDGE = 5;

    /**
     * The index of the counter of difference weighted compound blocks.
     * <p>
     * 차이 가중 복합 블록 카운터의 번호.
     */
    static final int COMPOUND_DIFFWTD = 6;

    /**
     * The index of the counter of distance weighted compound blocks.
     * <p>
     * 거리 가중 복합 블록 카운터의 번호.
     */
    static final int COMPOUND_DISTANCE = 7;

    /**
     * The index of the counter of averaged compound blocks.
     * <p>
     * 평균 복합 블록 카운터의 번호.
     */
    static final int COMPOUND_AVERAGE = 8;

    /**
     * The index of the counter of blocks that use the skip mode.
     * <p>
     * 건너뛰기 모드 블록 카운터의 번호.
     */
    static final int SKIP_MODE = 9;

    /**
     * The index of the counter of predictions from a scaled reference frame.
     * <p>
     * 스케일된 참조 프레임 예측 카운터의 번호.
     */
    static final int SCALED_REFERENCE = 10;

    /**
     * The index of the counter of blocks with different filters in the two directions.
     * <p>
     * 두 방향의 필터가 다른 블록 카운터의 번호.
     */
    static final int DUAL_FILTER = 11;

    /**
     * The index of the counter of the temporal candidates of the motion vector prediction.
     * <p>
     * 움직임 벡터 예측의 시간 후보 카운터의 번호.
     */
    static final int TEMPORAL_MV = 12;

    /**
     * The index of the counter of intra blocks in inter frames.
     * <p>
     * 인터 프레임 안 인트라 블록 카운터의 번호.
     */
    static final int INTRA_IN_INTER = 13;

    /**
     * The index of the counter of blocks with a segment id other than 0.
     * <p>
     * 세그먼트 id 가 0 이 아닌 블록 카운터의 번호.
     */
    static final int SEGMENT = 14;

    /**
     * The index of the counter of blocks with compound prediction (any blending).
     * <p>
     * 복합 예측 블록(혼합 방식 무관) 카운터의 번호.
     */
    static final int COMPOUND_BLOCKS = 15;

    /**
     * The number of counters.
     * <p>
     * 카운터 수.
     */
    static final int COUNT = 16;

    /**
     * The counters.
     * <p>
     * 카운터.
     */
    private static final int[] COUNTS = new int[COUNT];

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스.
     */
    private Av1ToolStats() {
    }

    /**
     * Counts one use of a tool.
     * <p>
     * 도구 사용을 한 번 센다.
     *
     * @param tool the index of the counter / 카운터 번호
     */
    static void count(int tool) {
        COUNTS[tool]++;
    }

    /**
     * Returns the number of uses of a tool since the last reset.
     * <p>
     * 마지막 초기화 이후 도구 사용 횟수를 반환한다.
     *
     * @param tool the index of the counter / 카운터 번호
     * @return the count / 횟수
     */
    static int get(int tool) {
        return COUNTS[tool];
    }

    /**
     * Sets all counters to 0.
     * <p>
     * 모든 카운터를 0 으로 한다.
     */
    static void reset() {
        java.util.Arrays.fill(COUNTS, 0);
    }
}
