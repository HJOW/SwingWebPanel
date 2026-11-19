package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The set of cumulative distribution functions (CDFs) that the intra frame decoder adapts while it decodes a tile (plan item P18-03; AV1
 * specification sections 6.8.2 and 8.2). A new set starts as a copy of the default tables (the coefficient tables are chosen by the base
 * quantizer index), and every tile works on its own copy. Each CDF array is flattened row-major; a distribution has N + 1 entries (see
 * {@link Av1SymbolDecoder}).
 * <p>
 * intra 프레임 디코더가 타일을 디코딩하며 적응시키는 누적 분포 함수(CDF)의 집합(계획 P18-03; AV1 규격 6.8.2, 8.2절). 새 집합은 기본 표의 복사본으로
 * 시작하며(계수 표는 기본 양자화 지수로 고른다) 타일마다 자기 복사본으로 동작한다. CDF 배열은 행 우선으로 편 것이며 분포 하나는 N + 1 개의 값을 가진다
 * ({@link Av1SymbolDecoder} 참조).
 */
final class Av1Cdfs {

    /**
     * The luma intra mode of intra frames [5][5][14].
     * <p>
     * 인트라 프레임의 휘도 인트라 모드 [5][5][14].
     */
    int[] intraFrameYMode;

    /**
     * The chroma mode without CfL [13][14].
     * <p>
     * CfL 이 없는 색차 모드 [13][14].
     */
    int[] uvModeCflNotAllowed;

    /**
     * The chroma mode with CfL [13][15].
     * <p>
     * CfL 이 있는 색차 모드 [13][15].
     */
    int[] uvModeCflAllowed;

    /**
     * The angle delta [8][8].
     * <p>
     * 각도 변화 [8][8].
     */
    int[] angleDelta;

    /**
     * The partition of 8x8 blocks [4][5].
     * <p>
     * 8x8 블록의 분할 [4][5].
     */
    int[] partitionW8;

    /**
     * The partition of 16x16 blocks [4][11].
     * <p>
     * 16x16 블록의 분할 [4][11].
     */
    int[] partitionW16;

    /**
     * The partition of 32x32 blocks [4][11].
     * <p>
     * 32x32 블록의 분할 [4][11].
     */
    int[] partitionW32;

    /**
     * The partition of 64x64 blocks [4][11].
     * <p>
     * 64x64 블록의 분할 [4][11].
     */
    int[] partitionW64;

    /**
     * The partition of 128x128 blocks [4][9].
     * <p>
     * 128x128 블록의 분할 [4][9].
     */
    int[] partitionW128;

    /**
     * The transform depth of 8x8 blocks [3][3].
     * <p>
     * 8x8 블록의 변환 깊이 [3][3].
     */
    int[] tx8x8;

    /**
     * The transform depth of 16x16 blocks [3][4].
     * <p>
     * 16x16 블록의 변환 깊이 [3][4].
     */
    int[] tx16x16;

    /**
     * The transform depth of 32x32 blocks [3][4].
     * <p>
     * 32x32 블록의 변환 깊이 [3][4].
     */
    int[] tx32x32;

    /**
     * The transform depth of 64x64 blocks [3][4].
     * <p>
     * 64x64 블록의 변환 깊이 [3][4].
     */
    int[] tx64x64;

    /**
     * The filter intra mode [6].
     * <p>
     * 필터 인트라 모드 [6].
     */
    int[] filterIntraMode;

    /**
     * The use of filter intra by block size [22][3].
     * <p>
     * 블록 크기별 필터 인트라 사용 여부 [22][3].
     */
    int[] filterIntra;

    /**
     * The segment id [3][9].
     * <p>
     * 세그먼트 id [3][9].
     */
    int[] segmentId;

    /**
     * The skip flag [3][3].
     * <p>
     * skip 플래그 [3][3].
     */
    int[] skip;

    /**
     * The use of a luma palette [7][3][3].
     * <p>
     * 휘도 팔레트 사용 여부 [7][3][3].
     */
    int[] paletteYMode;

    /**
     * The use of a chroma palette [2][3].
     * <p>
     * 색차 팔레트 사용 여부 [2][3].
     */
    int[] paletteUvMode;

    /**
     * The size of a luma palette [7][8].
     * <p>
     * 휘도 팔레트의 크기 [7][8].
     */
    int[] paletteYSize;

    /**
     * The size of a chroma palette [7][8].
     * <p>
     * 색차 팔레트의 크기 [7][8].
     */
    int[] paletteUvSize;

    /**
     * The luma palette colour indices, one array per palette size 2..8 [5][size + 1].
     * <p>
     * 휘도 팔레트 색 색인. 팔레트 크기 2~8 마다 배열 하나 [5][크기 + 1].
     */
    int[][] paletteYColor = new int[9][];

    /**
     * The chroma palette colour indices, one array per palette size 2..8.
     * <p>
     * 색차 팔레트 색 색인. 팔레트 크기 2~8 마다 배열 하나.
     */
    int[][] paletteUvColor = new int[9][];

    /**
     * The quantizer index delta [5].
     * <p>
     * 양자화 지수 변화량 [5].
     */
    int[] deltaQ;

    /**
     * The loop filter delta [5].
     * <p>
     * 루프 필터 변화량 [5].
     */
    int[] deltaLf;

    /**
     * The loop filter deltas of the four directions and planes [4][5].
     * <p>
     * 방향과 평면 네 가지의 루프 필터 변화량 [4][5].
     */
    int[][] deltaLfMulti = new int[4][];

    /**
     * The intra transform type of set 1 [2][13][8].
     * <p>
     * 집합 1 의 인트라 변환 종류 [2][13][8].
     */
    int[] intraTxTypeSet1;

    /**
     * The intra transform type of set 2 [3][13][6].
     * <p>
     * 집합 2 의 인트라 변환 종류 [3][13][6].
     */
    int[] intraTxTypeSet2;

    /**
     * The signs of the CfL alphas [9].
     * <p>
     * CfL 알파의 부호 [9].
     */
    int[] cflSign;

    /**
     * The CfL alpha [6][17].
     * <p>
     * CfL 알파 [6][17].
     */
    int[] cflAlpha;

    /**
     * The use of the Wiener filter [3].
     * <p>
     * 위너 필터 사용 여부 [3].
     */
    int[] useWiener;

    /**
     * The use of the self guided filter [3].
     * <p>
     * 자기 유도 필터 사용 여부 [3].
     */
    int[] useSgrproj;

    /**
     * The restoration type [4].
     * <p>
     * 복원 종류 [4].
     */
    int[] restorationType;

    /**
     * The all-zero flag of a transform block [5][13][3].
     * <p>
     * 변환 블록의 전부 0 플래그 [5][13][3].
     */
    int[] txbSkip;

    /**
     * The end of block position class of 16 coefficients [2][2][6].
     * <p>
     * 계수 16개의 끝 위치 종류 [2][2][6].
     */
    int[] eobPt16;

    /**
     * The end of block position class of 32 coefficients [2][2][7].
     * <p>
     * 계수 32개의 끝 위치 종류 [2][2][7].
     */
    int[] eobPt32;

    /**
     * The end of block position class of 64 coefficients [2][2][8].
     * <p>
     * 계수 64개의 끝 위치 종류 [2][2][8].
     */
    int[] eobPt64;

    /**
     * The end of block position class of 128 coefficients [2][2][9].
     * <p>
     * 계수 128개의 끝 위치 종류 [2][2][9].
     */
    int[] eobPt128;

    /**
     * The end of block position class of 256 coefficients [2][2][10].
     * <p>
     * 계수 256개의 끝 위치 종류 [2][2][10].
     */
    int[] eobPt256;

    /**
     * The end of block position class of 512 coefficients [2][11].
     * <p>
     * 계수 512개의 끝 위치 종류 [2][11].
     */
    int[] eobPt512;

    /**
     * The end of block position class of 1024 coefficients [2][12].
     * <p>
     * 계수 1024개의 끝 위치 종류 [2][12].
     */
    int[] eobPt1024;

    /**
     * The extra bit of the end of block position [5][2][9][3].
     * <p>
     * 끝 위치의 추가 비트 [5][2][9][3].
     */
    int[] eobExtra;

    /**
     * The sign of the DC coefficient [2][3][3].
     * <p>
     * DC 계수의 부호 [2][3][3].
     */
    int[] dcSign;

    /**
     * The base level of the last coefficient [5][2][4][4].
     * <p>
     * 마지막 계수의 기본 수준 [5][2][4][4].
     */
    int[] coeffBaseEob;

    /**
     * The base level of the other coefficients [5][2][42][5].
     * <p>
     * 나머지 계수의 기본 수준 [5][2][42][5].
     */
    int[] coeffBase;

    /**
     * The range above the base level [5][2][21][5].
     * <p>
     * 기본 수준 위의 범위 [5][2][21][5].
     */
    int[] coeffBr;

    /**
     * The flag of intra block copy [3].
     * <p>
     * 인트라 블록 복사 플래그 [3].
     */
    int[] intrabc;

    /**
     * The split flag of the variable transform size tree [21][3].
     * <p>
     * 가변 변환 크기 트리의 분할 플래그 [21][3].
     */
    int[] txfmSplit;

    /**
     * The transform type of inter blocks, set 1 [2][17].
     * <p>
     * 인터 블록의 변환 종류, 집합 1 [2][17].
     */
    int[] interTxTypeSet1;

    /**
     * The transform type of inter blocks, set 2 [13].
     * <p>
     * 인터 블록의 변환 종류, 집합 2 [13].
     */
    int[] interTxTypeSet2;

    /**
     * The transform type of inter blocks, set 3 [4][3].
     * <p>
     * 인터 블록의 변환 종류, 집합 3 [4][3].
     */
    int[] interTxTypeSet3;

    /**
     * The motion vector joint type per motion vector context (0 inter blocks, 1 intra block copy), 5 entries each.
     * <p>
     * 움직임 벡터 문맥(0 인터 블록, 1 인트라 블록 복사)별 움직임 벡터 결합 종류, 각 5개.
     */
    final int[][] mvJoint = new int[2][];

    /**
     * The motion vector class per context: [comp][12] flattened.
     * <p>
     * 문맥별 움직임 벡터 종류: [성분][12] 을 편 것.
     */
    final int[][] mvClass = new int[2][];

    /**
     * The class 0 integer bit per context: [comp][3] flattened.
     * <p>
     * 문맥별 종류 0 정수 비트: [성분][3] 을 편 것.
     */
    final int[][] mvClass0Bit = new int[2][];

    /**
     * The class 0 fractional bits per context: [comp][class0 bit][5] flattened.
     * <p>
     * 문맥별 종류 0 소수 비트: [성분][종류 0 비트][5] 를 편 것.
     */
    final int[][] mvClass0Fr = new int[2][];

    /**
     * The class 0 high precision bit per context: [comp][3] flattened.
     * <p>
     * 문맥별 종류 0 고정밀 비트: [성분][3] 을 편 것.
     */
    final int[][] mvClass0Hp = new int[2][];

    /**
     * The sign of the motion vector difference per context: [comp][3] flattened.
     * <p>
     * 문맥별 움직임 벡터 차이의 부호: [성분][3] 을 편 것.
     */
    final int[][] mvSign = new int[2][];

    /**
     * The integer bits of the motion vector difference per context: [comp][10][3] flattened.
     * <p>
     * 문맥별 움직임 벡터 차이의 정수 비트: [성분][10][3] 을 편 것.
     */
    final int[][] mvBit = new int[2][];

    /**
     * The fractional bits of the motion vector difference per context: [comp][5] flattened.
     * <p>
     * 문맥별 움직임 벡터 차이의 소수 비트: [성분][5] 를 편 것.
     */
    final int[][] mvFr = new int[2][];

    /**
     * The high precision bit of the motion vector difference per context: [comp][3] flattened.
     * <p>
     * 문맥별 움직임 벡터 차이의 고정밀 비트: [성분][3] 을 편 것.
     */
    final int[][] mvHp = new int[2][];

    /**
     * The luma intra mode of intra blocks in inter frames [4][14].
     * <p>
     * 인터 프레임 안 인트라 블록의 휘도 인트라 모드 [4][14].
     */
    int[] yMode;

    /**
     * The flag new_mv [6][3].
     * <p>
     * new_mv 플래그 [6][3].
     */
    int[] newMv;

    /**
     * The flag zero_mv [2][3].
     * <p>
     * zero_mv 플래그 [2][3].
     */
    int[] zeroMv;

    /**
     * The flag ref_mv [6][3].
     * <p>
     * ref_mv 플래그 [6][3].
     */
    int[] refMv;

    /**
     * The flag drl_mode [3][3].
     * <p>
     * drl_mode 플래그 [3][3].
     */
    int[] drlMode;

    /**
     * The flag is_inter [4][3].
     * <p>
     * is_inter 플래그 [4][3].
     */
    int[] isInter;

    /**
     * The flag comp_mode [5][3].
     * <p>
     * comp_mode 플래그 [5][3].
     */
    int[] compMode;

    /**
     * The flag skip_mode [3][3].
     * <p>
     * skip_mode 플래그 [3][3].
     */
    int[] skipMode;

    /**
     * The flags comp_ref, comp_ref_p1, comp_ref_p2 [3][3][3].
     * <p>
     * comp_ref, comp_ref_p1, comp_ref_p2 플래그 [3][3][3].
     */
    int[] compRef;

    /**
     * The flags comp_bwdref, comp_bwdref_p1 [3][2][3].
     * <p>
     * comp_bwdref, comp_bwdref_p1 플래그 [3][2][3].
     */
    int[] compBwdRef;

    /**
     * The flags single_ref_p1..p6 [3][6][3].
     * <p>
     * single_ref_p1..p6 플래그 [3][6][3].
     */
    int[] singleRef;

    /**
     * The compound prediction mode [8][9].
     * <p>
     * 복합 예측 모드 [8][9].
     */
    int[] compoundMode;

    /**
     * The interpolation filter [16][4].
     * <p>
     * 보간 필터 [16][4].
     */
    int[] interpFilter;

    /**
     * The motion mode [22][4].
     * <p>
     * 움직임 모드 [22][4].
     */
    int[] motionMode;

    /**
     * The flag compound_idx [6][3].
     * <p>
     * compound_idx 플래그 [6][3].
     */
    int[] compoundIdx;

    /**
     * The flag comp_group_idx [6][3].
     * <p>
     * comp_group_idx 플래그 [6][3].
     */
    int[] compGroupIdx;

    /**
     * The compound type (wedge or difference weighted) [22][3].
     * <p>
     * 복합 종류(쐐기 또는 차이 가중) [22][3].
     */
    int[] compoundType;

    /**
     * The flag interintra [3][3].
     * <p>
     * interintra 플래그 [3][3].
     */
    int[] interIntra;

    /**
     * The interintra mode [3][5].
     * <p>
     * 인터인트라 모드 [3][5].
     */
    int[] interIntraMode;

    /**
     * The wedge index [22][17].
     * <p>
     * 쐐기 번호 [22][17].
     */
    int[] wedgeIndex;

    /**
     * The flag wedge_interintra [22][3].
     * <p>
     * wedge_interintra 플래그 [22][3].
     */
    int[] wedgeInterIntra;

    /**
     * The flag use_obmc [22][3].
     * <p>
     * use_obmc 플래그 [22][3].
     */
    int[] useObmc;

    /**
     * The flag comp_ref_type [5][3].
     * <p>
     * comp_ref_type 플래그 [5][3].
     */
    int[] compRefType;

    /**
     * The flags uni_comp_ref, uni_comp_ref_p1, uni_comp_ref_p2 [3][3][3].
     * <p>
     * uni_comp_ref, uni_comp_ref_p1, uni_comp_ref_p2 플래그 [3][3][3].
     */
    int[] uniCompRef;

    /**
     * The flag seg_id_predicted [3][3].
     * <p>
     * seg_id_predicted 플래그 [3][3].
     */
    int[] segmentIdPredicted;

    /**
     * Creates the default set for a quantizer index.
     * <p>
     * 양자화 지수에 맞는 기본 집합을 만든다.
     *
     * @param baseQIdx the base quantizer index of the frame / 프레임의 기본 양자화 지수
     */
    Av1Cdfs(int baseQIdx) {
        intraFrameYMode = AvifCdfTables.DEFAULT_INTRA_FRAME_Y_MODE_CDF.clone();
        uvModeCflNotAllowed = AvifCdfTables.DEFAULT_UV_MODE_CFL_NOT_ALLOWED_CDF.clone();
        uvModeCflAllowed = AvifCdfTables.DEFAULT_UV_MODE_CFL_ALLOWED_CDF.clone();
        angleDelta = AvifCdfTables.DEFAULT_ANGLE_DELTA_CDF.clone();
        partitionW8 = AvifCdfTables.DEFAULT_PARTITION_W8_CDF.clone();
        partitionW16 = AvifCdfTables.DEFAULT_PARTITION_W16_CDF.clone();
        partitionW32 = AvifCdfTables.DEFAULT_PARTITION_W32_CDF.clone();
        partitionW64 = AvifCdfTables.DEFAULT_PARTITION_W64_CDF.clone();
        partitionW128 = AvifCdfTables.DEFAULT_PARTITION_W128_CDF.clone();
        tx8x8 = AvifCdfTables.DEFAULT_TX_8X8_CDF.clone();
        tx16x16 = AvifCdfTables.DEFAULT_TX_16X16_CDF.clone();
        tx32x32 = AvifCdfTables.DEFAULT_TX_32X32_CDF.clone();
        tx64x64 = AvifCdfTables.DEFAULT_TX_64X64_CDF.clone();
        filterIntraMode = AvifCdfTables.DEFAULT_FILTER_INTRA_MODE_CDF.clone();
        filterIntra = AvifCdfTables.DEFAULT_FILTER_INTRA_CDF.clone();
        segmentId = AvifCdfTables.DEFAULT_SEGMENT_ID_CDF.clone();
        skip = AvifCdfTables.DEFAULT_SKIP_CDF.clone();
        paletteYMode = AvifCdfTables.DEFAULT_PALETTE_Y_MODE_CDF.clone();
        paletteUvMode = AvifCdfTables.DEFAULT_PALETTE_UV_MODE_CDF.clone();
        paletteYSize = AvifCdfTables.DEFAULT_PALETTE_Y_SIZE_CDF.clone();
        paletteUvSize = AvifCdfTables.DEFAULT_PALETTE_UV_SIZE_CDF.clone();
        paletteYColor[2] = AvifCdfTables.DEFAULT_PALETTE_SIZE_2_Y_COLOR_CDF.clone();
        paletteYColor[3] = AvifCdfTables.DEFAULT_PALETTE_SIZE_3_Y_COLOR_CDF.clone();
        paletteYColor[4] = AvifCdfTables.DEFAULT_PALETTE_SIZE_4_Y_COLOR_CDF.clone();
        paletteYColor[5] = AvifCdfTables.DEFAULT_PALETTE_SIZE_5_Y_COLOR_CDF.clone();
        paletteYColor[6] = AvifCdfTables.DEFAULT_PALETTE_SIZE_6_Y_COLOR_CDF.clone();
        paletteYColor[7] = AvifCdfTables.DEFAULT_PALETTE_SIZE_7_Y_COLOR_CDF.clone();
        paletteYColor[8] = AvifCdfTables.DEFAULT_PALETTE_SIZE_8_Y_COLOR_CDF.clone();
        paletteUvColor[2] = AvifCdfTables.DEFAULT_PALETTE_SIZE_2_UV_COLOR_CDF.clone();
        paletteUvColor[3] = AvifCdfTables.DEFAULT_PALETTE_SIZE_3_UV_COLOR_CDF.clone();
        paletteUvColor[4] = AvifCdfTables.DEFAULT_PALETTE_SIZE_4_UV_COLOR_CDF.clone();
        paletteUvColor[5] = AvifCdfTables.DEFAULT_PALETTE_SIZE_5_UV_COLOR_CDF.clone();
        paletteUvColor[6] = AvifCdfTables.DEFAULT_PALETTE_SIZE_6_UV_COLOR_CDF.clone();
        paletteUvColor[7] = AvifCdfTables.DEFAULT_PALETTE_SIZE_7_UV_COLOR_CDF.clone();
        paletteUvColor[8] = AvifCdfTables.DEFAULT_PALETTE_SIZE_8_UV_COLOR_CDF.clone();
        deltaQ = AvifCdfTables.DEFAULT_DELTA_Q_CDF.clone();
        deltaLf = AvifCdfTables.DEFAULT_DELTA_LF_CDF.clone();
        // 방향/평면마다 따로 쓰는 루프 필터 변화량 분포는 기본 분포의 복사본이다.
        for (int i = 0; i < 4; i++) {
            deltaLfMulti[i] = AvifCdfTables.DEFAULT_DELTA_LF_CDF.clone();
        }
        intraTxTypeSet1 = AvifCdfTables.DEFAULT_INTRA_TX_TYPE_SET1_CDF.clone();
        intraTxTypeSet2 = AvifCdfTables.DEFAULT_INTRA_TX_TYPE_SET2_CDF.clone();
        cflSign = AvifCdfTables.DEFAULT_CFL_SIGN_CDF.clone();
        cflAlpha = AvifCdfTables.DEFAULT_CFL_ALPHA_CDF.clone();
        useWiener = AvifCdfTables.DEFAULT_USE_WIENER_CDF.clone();
        useSgrproj = AvifCdfTables.DEFAULT_USE_SGRPROJ_CDF.clone();
        restorationType = AvifCdfTables.DEFAULT_RESTORATION_TYPE_CDF.clone();
        int idx = baseQIdx <= 20 ? 0 : (baseQIdx <= 60 ? 1 : (baseQIdx <= 120 ? 2 : 3));
        txbSkip = slice(AvifCdfTables.DEFAULT_TXB_SKIP_CDF, idx);
        eobPt16 = slice(AvifCdfTables.DEFAULT_EOB_PT_16_CDF, idx);
        eobPt32 = slice(AvifCdfTables.DEFAULT_EOB_PT_32_CDF, idx);
        eobPt64 = slice(AvifCdfTables.DEFAULT_EOB_PT_64_CDF, idx);
        eobPt128 = slice(AvifCdfTables.DEFAULT_EOB_PT_128_CDF, idx);
        eobPt256 = slice(AvifCdfTables.DEFAULT_EOB_PT_256_CDF, idx);
        eobPt512 = slice(AvifCdfTables.DEFAULT_EOB_PT_512_CDF, idx);
        eobPt1024 = slice(AvifCdfTables.DEFAULT_EOB_PT_1024_CDF, idx);
        eobExtra = slice(AvifCdfTables.DEFAULT_EOB_EXTRA_CDF, idx);
        dcSign = slice(AvifCdfTables.DEFAULT_DC_SIGN_CDF, idx);
        coeffBaseEob = slice(AvifCdfTables.DEFAULT_COEFF_BASE_EOB_CDF, idx);
        coeffBase = slice(AvifCdfTables.DEFAULT_COEFF_BASE_CDF, idx);
        coeffBr = slice(AvifCdfTables.DEFAULT_COEFF_BR_CDF, idx);
        intrabc = AvifCdfTables.DEFAULT_INTRABC_CDF.clone();
        txfmSplit = AvifCdfTables.DEFAULT_TXFM_SPLIT_CDF.clone();
        interTxTypeSet1 = AvifInterCdfTables.DEFAULT_INTER_TX_TYPE_SET1_CDF.clone();
        interTxTypeSet2 = AvifInterCdfTables.DEFAULT_INTER_TX_TYPE_SET2_CDF.clone();
        interTxTypeSet3 = AvifInterCdfTables.DEFAULT_INTER_TX_TYPE_SET3_CDF.clone();
        yMode = AvifInterCdfTables.DEFAULT_Y_MODE_CDF.clone();
        newMv = AvifInterCdfTables.DEFAULT_NEW_MV_CDF.clone();
        zeroMv = AvifInterCdfTables.DEFAULT_ZERO_MV_CDF.clone();
        refMv = AvifInterCdfTables.DEFAULT_REF_MV_CDF.clone();
        drlMode = AvifInterCdfTables.DEFAULT_DRL_MODE_CDF.clone();
        isInter = AvifInterCdfTables.DEFAULT_IS_INTER_CDF.clone();
        compMode = AvifInterCdfTables.DEFAULT_COMP_MODE_CDF.clone();
        skipMode = AvifInterCdfTables.DEFAULT_SKIP_MODE_CDF.clone();
        compRef = AvifInterCdfTables.DEFAULT_COMP_REF_CDF.clone();
        compBwdRef = AvifInterCdfTables.DEFAULT_COMP_BWD_REF_CDF.clone();
        singleRef = AvifInterCdfTables.DEFAULT_SINGLE_REF_CDF.clone();
        compoundMode = AvifInterCdfTables.DEFAULT_COMPOUND_MODE_CDF.clone();
        interpFilter = AvifInterCdfTables.DEFAULT_INTERP_FILTER_CDF.clone();
        motionMode = AvifInterCdfTables.DEFAULT_MOTION_MODE_CDF.clone();
        compoundIdx = AvifInterCdfTables.DEFAULT_COMPOUND_IDX_CDF.clone();
        compGroupIdx = AvifInterCdfTables.DEFAULT_COMP_GROUP_IDX_CDF.clone();
        compoundType = AvifInterCdfTables.DEFAULT_COMPOUND_TYPE_CDF.clone();
        interIntra = AvifInterCdfTables.DEFAULT_INTER_INTRA_CDF.clone();
        interIntraMode = AvifInterCdfTables.DEFAULT_INTER_INTRA_MODE_CDF.clone();
        wedgeIndex = AvifInterCdfTables.DEFAULT_WEDGE_INDEX_CDF.clone();
        wedgeInterIntra = AvifInterCdfTables.DEFAULT_WEDGE_INTER_INTRA_CDF.clone();
        useObmc = AvifInterCdfTables.DEFAULT_USE_OBMC_CDF.clone();
        compRefType = AvifInterCdfTables.DEFAULT_COMP_REF_TYPE_CDF.clone();
        uniCompRef = AvifInterCdfTables.DEFAULT_UNI_COMP_REF_CDF.clone();
        segmentIdPredicted = AvifCdfTables.DEFAULT_SEGMENT_ID_PREDICTED_CDF.clone();
        // 움직임 벡터 문맥(인터 블록, 인트라 블록 복사)마다 기본 분포를 복사한다. 성분(행, 열)별 표는 두 번 복사해 이어 붙인다.
        for (int ctx = 0; ctx < 2; ctx++) {
            mvJoint[ctx] = AvifInterCdfTables.DEFAULT_MV_JOINT_CDF.clone();
            mvClass[ctx] = AvifInterCdfTables.DEFAULT_MV_CLASS_CDF.clone();
            mvClass0Bit[ctx] = twice(AvifInterCdfTables.DEFAULT_MV_CLASS0_BIT_CDF);
            mvClass0Fr[ctx] = AvifInterCdfTables.DEFAULT_MV_CLASS0_FR_CDF.clone();
            mvClass0Hp[ctx] = twice(AvifInterCdfTables.DEFAULT_MV_CLASS0_HP_CDF);
            mvSign[ctx] = twice(AvifInterCdfTables.DEFAULT_MV_SIGN_CDF);
            mvBit[ctx] = twice(AvifInterCdfTables.DEFAULT_MV_BIT_CDF);
            mvFr[ctx] = AvifInterCdfTables.DEFAULT_MV_FR_CDF.clone();
            mvHp[ctx] = twice(AvifInterCdfTables.DEFAULT_MV_HP_CDF);
        }
    }

    /**
     * Returns two copies of a table one after the other (a table that is the same for both motion vector components).
     * <p>
     * 표의 복사본 둘을 이어 붙여 반환한다(두 움직임 벡터 성분에 같은 표).
     *
     * @param t the table / 표
     * @return the two copies / 복사본 둘
     */
    private static int[] twice(int[] t) {
        int[] r = new int[t.length * 2];
        System.arraycopy(t, 0, r, 0, t.length);
        System.arraycopy(t, 0, r, t.length, t.length);
        return r;
    }

    /**
     * Copies the part of a coefficient table that belongs to one quantizer context (the first index of the table has four values).
     * <p>
     * 계수 표에서 양자화 문맥 하나에 속하는 부분을 복사한다(표의 첫 인덱스는 네 값을 가진다).
     *
     * @param all the whole table / 표 전체
     * @param idx the quantizer context 0..3 / 양자화 문맥 0..3
     * @return the copy of that part / 그 부분의 복사본
     */
    private static int[] slice(int[] all, int idx) {
        int n = all.length / 4;
        int[] r = new int[n];
        System.arraycopy(all, idx * n, r, 0, n);
        return r;
    }

    /**
     * Creates an empty set (used by {@link #copy}).
     * <p>
     * 빈 집합을 만든다({@link #copy} 가 쓴다).
     */
    private Av1Cdfs() {
    }

    /**
     * Returns a copy for a frame that loads the distributions of a reference frame (<code>load_cdfs</code>): the counters of all
     * distributions are set to 0.
     * <p>
     * 참조 프레임의 분포를 불러오는 프레임을 위한 복사본을 반환한다(<code>load_cdfs</code>): 모든 분포의 카운터를 0 으로 한다.
     *
     * @return the copy / 복사본
     */
    Av1Cdfs copyForLoad() {
        Av1Cdfs c = copy();
        c.clearCounters();
        return c;
    }

    /**
     * Sets the counter (the last entry) of every distribution to 0.
     * <p>
     * 모든 분포의 카운터(마지막 항목)를 0 으로 한다.
     */
    private void clearCounters() {
        clear(intraFrameYMode, 14);
        clear(uvModeCflNotAllowed, 14);
        clear(uvModeCflAllowed, 15);
        clear(angleDelta, 8);
        clear(partitionW8, 5);
        clear(partitionW16, 11);
        clear(partitionW32, 11);
        clear(partitionW64, 11);
        clear(partitionW128, 9);
        clear(tx8x8, 3);
        clear(tx16x16, 4);
        clear(tx32x32, 4);
        clear(tx64x64, 4);
        clear(filterIntraMode, 6);
        clear(filterIntra, 3);
        clear(segmentId, 9);
        clear(skip, 3);
        clear(paletteYMode, 3);
        clear(paletteUvMode, 3);
        clear(paletteYSize, 8);
        clear(paletteUvSize, 8);
        // 팔레트 색 색인 분포는 크기마다 한 분포의 길이가 다르다.
        for (int n = 2; n <= 8; n++) {
            clear(paletteYColor[n], n + 1);
            clear(paletteUvColor[n], n + 1);
        }
        clear(deltaQ, 5);
        clear(deltaLf, 5);
        // 델타 루프 필터 다중 분포 네 개를 모두 처리한다.
        for (int i = 0; i < 4; i++) {
            clear(deltaLfMulti[i], 5);
        }
        clear(intraTxTypeSet1, 8);
        clear(intraTxTypeSet2, 6);
        clear(cflSign, 9);
        clear(cflAlpha, 17);
        clear(useWiener, 3);
        clear(useSgrproj, 3);
        clear(restorationType, 4);
        clear(txbSkip, 3);
        clear(eobPt16, 6);
        clear(eobPt32, 7);
        clear(eobPt64, 8);
        clear(eobPt128, 9);
        clear(eobPt256, 10);
        clear(eobPt512, 11);
        clear(eobPt1024, 12);
        clear(eobExtra, 3);
        clear(dcSign, 3);
        clear(coeffBaseEob, 4);
        clear(coeffBase, 5);
        clear(coeffBr, 5);
        clear(intrabc, 3);
        clear(txfmSplit, 3);
        clear(interTxTypeSet1, 17);
        clear(interTxTypeSet2, 13);
        clear(interTxTypeSet3, 3);
        clear(yMode, 14);
        clear(newMv, 3);
        clear(zeroMv, 3);
        clear(refMv, 3);
        clear(drlMode, 3);
        clear(isInter, 3);
        clear(compMode, 3);
        clear(skipMode, 3);
        clear(compRef, 3);
        clear(compBwdRef, 3);
        clear(singleRef, 3);
        clear(compoundMode, 9);
        clear(interpFilter, 4);
        clear(motionMode, 4);
        clear(compoundIdx, 3);
        clear(compGroupIdx, 3);
        clear(compoundType, 3);
        clear(interIntra, 3);
        clear(interIntraMode, 5);
        clear(wedgeIndex, 17);
        clear(wedgeInterIntra, 3);
        clear(useObmc, 3);
        clear(compRefType, 3);
        clear(uniCompRef, 3);
        clear(segmentIdPredicted, 3);
        // 움직임 벡터 분포는 문맥(인터, 인트라 블록 복사)마다 따로 있다.
        for (int ctx = 0; ctx < 2; ctx++) {
            clear(mvJoint[ctx], 5);
            clear(mvClass[ctx], 12);
            clear(mvClass0Bit[ctx], 3);
            clear(mvClass0Fr[ctx], 5);
            clear(mvClass0Hp[ctx], 3);
            clear(mvSign[ctx], 3);
            clear(mvBit[ctx], 3);
            clear(mvFr[ctx], 5);
            clear(mvHp[ctx], 3);
        }
    }

    /**
     * Sets every <code>stride</code>-th entry (the counter at the end of each distribution) of an array to 0.
     * <p>
     * 배열에서 <code>stride</code> 번째 항목마다(각 분포 끝의 카운터) 0 으로 한다.
     *
     * @param a      the array of consecutive distributions / 연속된 분포의 배열
     * @param stride the number of entries of one distribution including the counter / 카운터를 포함한 분포 하나의 항목 수
     */
    private static void clear(int[] a, int stride) {
        // 분포마다 마지막 항목(카운터)을 0 으로 한다.
        for (int i = stride - 1; i < a.length; i += stride) {
            a[i] = 0;
        }
    }

    /**
     * Returns a deep copy of this set.
     * <p>
     * 이 집합의 깊은 복사본을 반환한다.
     *
     * @return the copy / 복사본
     */
    Av1Cdfs copy() {
        Av1Cdfs c = new Av1Cdfs();
        c.intraFrameYMode = intraFrameYMode.clone();
        c.uvModeCflNotAllowed = uvModeCflNotAllowed.clone();
        c.uvModeCflAllowed = uvModeCflAllowed.clone();
        c.angleDelta = angleDelta.clone();
        c.partitionW8 = partitionW8.clone();
        c.partitionW16 = partitionW16.clone();
        c.partitionW32 = partitionW32.clone();
        c.partitionW64 = partitionW64.clone();
        c.partitionW128 = partitionW128.clone();
        c.tx8x8 = tx8x8.clone();
        c.tx16x16 = tx16x16.clone();
        c.tx32x32 = tx32x32.clone();
        c.tx64x64 = tx64x64.clone();
        c.filterIntraMode = filterIntraMode.clone();
        c.filterIntra = filterIntra.clone();
        c.segmentId = segmentId.clone();
        c.skip = skip.clone();
        c.paletteYMode = paletteYMode.clone();
        c.paletteUvMode = paletteUvMode.clone();
        c.paletteYSize = paletteYSize.clone();
        c.paletteUvSize = paletteUvSize.clone();
        // 팔레트 색 분포와 루프 필터 변화량 분포의 배열도 하나씩 복사한다.
        for (int i = 2; i <= 8; i++) {
            c.paletteYColor[i] = paletteYColor[i].clone();
            c.paletteUvColor[i] = paletteUvColor[i].clone();
        }
        // 네 개의 루프 필터 변화량 분포를 하나씩 복사한다.
        for (int i = 0; i < 4; i++) {
            c.deltaLfMulti[i] = deltaLfMulti[i].clone();
        }
        c.deltaQ = deltaQ.clone();
        c.deltaLf = deltaLf.clone();
        c.intraTxTypeSet1 = intraTxTypeSet1.clone();
        c.intraTxTypeSet2 = intraTxTypeSet2.clone();
        c.cflSign = cflSign.clone();
        c.cflAlpha = cflAlpha.clone();
        c.useWiener = useWiener.clone();
        c.useSgrproj = useSgrproj.clone();
        c.restorationType = restorationType.clone();
        c.txbSkip = txbSkip.clone();
        c.eobPt16 = eobPt16.clone();
        c.eobPt32 = eobPt32.clone();
        c.eobPt64 = eobPt64.clone();
        c.eobPt128 = eobPt128.clone();
        c.eobPt256 = eobPt256.clone();
        c.eobPt512 = eobPt512.clone();
        c.eobPt1024 = eobPt1024.clone();
        c.eobExtra = eobExtra.clone();
        c.dcSign = dcSign.clone();
        c.coeffBaseEob = coeffBaseEob.clone();
        c.coeffBase = coeffBase.clone();
        c.coeffBr = coeffBr.clone();
        c.intrabc = intrabc.clone();
        c.txfmSplit = txfmSplit.clone();
        c.interTxTypeSet1 = interTxTypeSet1.clone();
        c.interTxTypeSet2 = interTxTypeSet2.clone();
        c.interTxTypeSet3 = interTxTypeSet3.clone();
        c.yMode = yMode.clone();
        c.newMv = newMv.clone();
        c.zeroMv = zeroMv.clone();
        c.refMv = refMv.clone();
        c.drlMode = drlMode.clone();
        c.isInter = isInter.clone();
        c.compMode = compMode.clone();
        c.skipMode = skipMode.clone();
        c.compRef = compRef.clone();
        c.compBwdRef = compBwdRef.clone();
        c.singleRef = singleRef.clone();
        c.compoundMode = compoundMode.clone();
        c.interpFilter = interpFilter.clone();
        c.motionMode = motionMode.clone();
        c.compoundIdx = compoundIdx.clone();
        c.compGroupIdx = compGroupIdx.clone();
        c.compoundType = compoundType.clone();
        c.interIntra = interIntra.clone();
        c.interIntraMode = interIntraMode.clone();
        c.wedgeIndex = wedgeIndex.clone();
        c.wedgeInterIntra = wedgeInterIntra.clone();
        c.useObmc = useObmc.clone();
        c.compRefType = compRefType.clone();
        c.uniCompRef = uniCompRef.clone();
        c.segmentIdPredicted = segmentIdPredicted.clone();
        // 움직임 벡터 문맥마다 복사한다.
        for (int ctx = 0; ctx < 2; ctx++) {
            c.mvJoint[ctx] = mvJoint[ctx].clone();
            c.mvClass[ctx] = mvClass[ctx].clone();
            c.mvClass0Bit[ctx] = mvClass0Bit[ctx].clone();
            c.mvClass0Fr[ctx] = mvClass0Fr[ctx].clone();
            c.mvClass0Hp[ctx] = mvClass0Hp[ctx].clone();
            c.mvSign[ctx] = mvSign[ctx].clone();
            c.mvBit[ctx] = mvBit[ctx].clone();
            c.mvFr[ctx] = mvFr[ctx].clone();
            c.mvHp[ctx] = mvHp[ctx].clone();
        }
        return c;
    }
}
