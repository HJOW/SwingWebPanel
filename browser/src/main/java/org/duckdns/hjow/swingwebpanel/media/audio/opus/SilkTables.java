package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Constant tables of the SILK decoder: entropy coding distributions (inverse cumulative, in units of 1/256), gain, LTP, pitch and stereo tables.
 * The values were extracted mechanically from the reference decoder source of RFC 6716 appendix A (silk/tables_*.c, pitch_est_tables.c, table_LSF_cos.c; BSD license,
 * copyright Skype Limited, Microsoft Corporation, Xiph.Org Foundation, Octasic, Jean-Marc Valin, Timothy B. Terriberry, CSIRO,
 * Gregory Maxwell, Mark Borgerding, Erik de Castro Lopo; see docs/progress/media-opus.md). Two-dimensional tables are flattened
 * row by row.
 * <p>
 * SILK 디코더의 상수 표: 엔트로피 부호화 분포(역 누적, 1/256 단위), 이득, LTP, 피치, 스테레오 표.
 * 값은 RFC 6716 부록 A 의 참조 디코더 소스(silk/tables_*.c, pitch_est_tables.c, table_LSF_cos.c)에서 기계적으로 추출했다(BSD 라이선스. 저작권자는 위 영문 목록.
 * docs/progress/media-opus.md 참조). 2차원 표는 행 순서로 펼쳤다.
 */
final class SilkTables {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private SilkTables() {
    }

    /**
     * Distribution of the most significant bits of the first gain per signal type (3 x 8).
     * <p>
     * 신호 종류별 첫 이득 상위 비트의 분포(3 x 8).
     */
    static final int[] GAIN_ICDF = {
        224, 112, 44, 15, 3, 2, 1, 0, 254, 237, 192, 132, 70, 23, 4, 0,
        255, 252, 226, 155, 61, 11, 2, 0 };

    /**
     * Distribution of the delta gain index (41 entries).
     * <p>
     * 이득 차분 번호의 분포(41개).
     */
    static final int[] DELTA_GAIN_ICDF = {
        250, 245, 234, 203, 71, 50, 42, 38, 35, 33, 31, 29, 28, 27, 26, 25,
        24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9,
        8, 7, 6, 5, 4, 3, 2, 1, 0 };

    /**
     * Distribution of the LTP codebook (periodicity) index.
     * <p>
     * LTP 부호책(주기성) 번호의 분포.
     */
    static final int[] LTP_PER_INDEX_ICDF = {
        179, 99, 0 };

    /**
     * Distribution of the LTP gain index, codebook 0 (8 vectors).
     * <p>
     * LTP 이득 번호의 분포, 부호책 0(벡터 8개).
     */
    static final int[] LTP_GAIN_ICDF_0 = {
        71, 56, 43, 30, 21, 12, 6, 0 };

    /**
     * Distribution of the LTP gain index, codebook 1 (16 vectors).
     * <p>
     * LTP 이득 번호의 분포, 부호책 1(벡터 16개).
     */
    static final int[] LTP_GAIN_ICDF_1 = {
        199, 165, 144, 124, 109, 96, 84, 71, 61, 51, 42, 32, 23, 15, 8, 0 };

    /**
     * Distribution of the LTP gain index, codebook 2 (32 vectors).
     * <p>
     * LTP 이득 번호의 분포, 부호책 2(벡터 32개).
     */
    static final int[] LTP_GAIN_ICDF_2 = {
        241, 225, 211, 199, 187, 175, 164, 153, 142, 132, 123, 114, 105, 96, 88, 80,
        72, 64, 57, 50, 44, 38, 33, 29, 24, 20, 16, 12, 9, 5, 2, 0 };

    /**
     * LTP filter vectors in Q7, codebook 0 (8 x 5).
     * <p>
     * Q7 LTP 필터 벡터, 부호책 0(8 x 5).
     */
    static final int[] LTP_VQ_0 = {
        4, 6, 24, 7, 5, 0, 0, 2, 0, 0, 12, 28, 41, 13, -4, -9,
        15, 42, 25, 14, 1, -2, 62, 41, -9, -10, 37, 65, -4, 3, -6, 4,
        66, 7, -8, 16, 14, 38, -3, 33 };

    /**
     * LTP filter vectors in Q7, codebook 1 (16 x 5).
     * <p>
     * Q7 LTP 필터 벡터, 부호책 1(16 x 5).
     */
    static final int[] LTP_VQ_1 = {
        13, 22, 39, 23, 12, -1, 36, 64, 27, -6, -7, 10, 55, 43, 17, 1,
        1, 8, 1, 1, 6, -11, 74, 53, -9, -12, 55, 76, -12, 8, -3, 3,
        93, 27, -4, 26, 39, 59, 3, -8, 2, 0, 77, 11, 9, -8, 22, 44,
        -6, 7, 40, 9, 26, 3, 9, -7, 20, 101, -7, 4, 3, -8, 42, 26,
        0, -15, 33, 68, 2, 23, -2, 55, 46, -2, 15, 3, -1, 21, 16, 41 };

    /**
     * LTP filter vectors in Q7, codebook 2 (32 x 5).
     * <p>
     * Q7 LTP 필터 벡터, 부호책 2(32 x 5).
     */
    static final int[] LTP_VQ_2 = {
        -6, 27, 61, 39, 5, -11, 42, 88, 4, 1, -2, 60, 65, 6, -4, -1,
        -5, 73, 56, 1, -9, 19, 94, 29, -9, 0, 12, 99, 6, 4, 8, -19,
        102, 46, -13, 3, 2, 13, 3, 2, 9, -21, 84, 72, -18, -11, 46, 104,
        -22, 8, 18, 38, 48, 23, 0, -16, 70, 83, -21, 11, 5, -11, 117, 22,
        -8, -6, 23, 117, -12, 3, 3, -8, 95, 28, 4, -10, 15, 77, 60, -15,
        -1, 4, 124, 2, -4, 3, 38, 84, 24, -25, 2, 13, 42, 13, 31, 21,
        -4, 56, 46, -1, -1, 35, 79, -13, 19, -7, 65, 88, -9, -14, 20, 4,
        81, 49, -29, 20, 0, 75, 3, -17, 5, -9, 44, 92, -8, 1, -3, 22,
        69, 31, -6, 95, 41, -12, 5, 39, 67, 16, -4, 1, 0, -6, 120, 55,
        -36, -13, 44, 122, 4, -24, 81, 5, 11, 3, 7, 2, 0, 9, 10, 88 };

    /**
     * LTP scaling factors in Q14.
     * <p>
     * Q14 LTP 크기 조정 계수.
     */
    static final int[] LTP_SCALES_Q14 = {
        15565, 12288, 8192 };

    /**
     * Quantization levels of the stereo prediction in Q13.
     * <p>
     * Q13 스테레오 예측의 양자화 수준.
     */
    static final int[] STEREO_PRED_QUANT_Q13 = {
        -13732, -10050, -8266, -7526, -6500, -5000, -2950, -820, 820, 2950, 5000, 6500, 7526, 8266, 10050, 13732 };

    /**
     * Joint distribution of the stereo prediction indices (25 entries).
     * <p>
     * 스테레오 예측 번호의 결합 분포(25개).
     */
    static final int[] STEREO_PRED_JOINT_ICDF = {
        249, 247, 246, 245, 244, 234, 210, 202, 201, 200, 197, 174, 82, 59, 56, 55,
        54, 46, 22, 12, 11, 10, 9, 7, 0 };

    /**
     * Distribution of the mid-only flag.
     * <p>
     * 미드 전용 플래그의 분포.
     */
    static final int[] STEREO_ONLY_CODE_MID_ICDF = {
        64, 0 };

    /**
     * Distribution of the LBRR flags of a 2-frame packet.
     * <p>
     * 프레임 2개 패킷의 LBRR 플래그 분포.
     */
    static final int[] LBRR_FLAGS_2_ICDF = {
        203, 150, 0 };

    /**
     * Distribution of the LBRR flags of a 3-frame packet.
     * <p>
     * 프레임 3개 패킷의 LBRR 플래그 분포.
     */
    static final int[] LBRR_FLAGS_3_ICDF = {
        215, 195, 166, 125, 110, 82, 0 };

    /**
     * Distribution of the least significant bits of large pulse magnitudes.
     * <p>
     * 큰 펄스 크기의 하위 비트 분포.
     */
    static final int[] LSB_ICDF = {
        120, 0 };

    /**
     * Distribution of the LTP scaling index.
     * <p>
     * LTP 크기 조정 번호의 분포.
     */
    static final int[] LTP_SCALE_ICDF = {
        128, 64, 0 };

    /**
     * Distribution of signal type and quantization offset for active frames.
     * <p>
     * 활성 프레임의 신호 종류와 양자화 오프셋 분포.
     */
    static final int[] TYPE_OFFSET_VAD_ICDF = {
        232, 158, 10, 0 };

    /**
     * Distribution of signal type and quantization offset for inactive frames.
     * <p>
     * 비활성 프레임의 신호 종류와 양자화 오프셋 분포.
     */
    static final int[] TYPE_OFFSET_NO_VAD_ICDF = {
        230, 0 };

    /**
     * Distribution of the NLSF interpolation factor.
     * <p>
     * NLSF 보간 계수의 분포.
     */
    static final int[] NLSF_INTERP_ICDF = {
        243, 221, 192, 181, 0 };

    /**
     * Quantization offsets in Q10 indexed by signal type and offset type (2 x 2).
     * <p>
     * 신호 종류와 오프셋 종류별 Q10 양자화 오프셋(2 x 2).
     */
    static final int[] QUANT_OFFSETS_Q10 = {
        100, 240, 32, 100 };

    /**
     * Uniform distribution over 3 values.
     * <p>
     * 값 3개의 균등 분포.
     */
    static final int[] UNIFORM3_ICDF = {
        171, 85, 0 };

    /**
     * Uniform distribution over 4 values.
     * <p>
     * 값 4개의 균등 분포.
     */
    static final int[] UNIFORM4_ICDF = {
        192, 128, 64, 0 };

    /**
     * Uniform distribution over 5 values.
     * <p>
     * 값 5개의 균등 분포.
     */
    static final int[] UNIFORM5_ICDF = {
        205, 154, 102, 51, 0 };

    /**
     * Uniform distribution over 6 values.
     * <p>
     * 값 6개의 균등 분포.
     */
    static final int[] UNIFORM6_ICDF = {
        213, 171, 128, 85, 43, 0 };

    /**
     * Uniform distribution over 8 values.
     * <p>
     * 값 8개의 균등 분포.
     */
    static final int[] UNIFORM8_ICDF = {
        224, 192, 160, 128, 96, 64, 32, 0 };

    /**
     * Distribution of the extension of large NLSF residuals.
     * <p>
     * 큰 NLSF 잔차 확장의 분포.
     */
    static final int[] NLSF_EXT_ICDF = {
        100, 40, 16, 7, 3, 1, 0 };

    /**
     * Distribution of the high part of the absolute pitch lag (32 entries).
     * <p>
     * 절대 피치 지연 상위 부분의 분포(32개).
     */
    static final int[] PITCH_LAG_ICDF = {
        253, 250, 244, 233, 212, 182, 150, 131, 120, 110, 98, 85, 72, 60, 49, 40,
        32, 25, 19, 15, 13, 11, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };

    /**
     * Distribution of the pitch lag delta (21 entries).
     * <p>
     * 피치 지연 차분의 분포(21개).
     */
    static final int[] PITCH_DELTA_ICDF = {
        210, 208, 206, 203, 199, 193, 183, 168, 142, 104, 74, 52, 37, 27, 20, 14,
        10, 6, 4, 2, 0 };

    /**
     * Distribution of the pitch contour, 20 ms frames, wideband (34 entries).
     * <p>
     * 피치 윤곽의 분포, 20 ms 프레임, 광대역(34개).
     */
    static final int[] PITCH_CONTOUR_ICDF = {
        223, 201, 183, 167, 152, 138, 124, 111, 98, 88, 79, 70, 62, 56, 50, 44,
        39, 35, 31, 27, 24, 21, 18, 16, 14, 12, 10, 8, 6, 4, 3, 2,
        1, 0 };

    /**
     * Distribution of the pitch contour, 20 ms frames, narrowband (11 entries).
     * <p>
     * 피치 윤곽의 분포, 20 ms 프레임, 협대역(11개).
     */
    static final int[] PITCH_CONTOUR_NB_ICDF = {
        188, 176, 155, 138, 119, 97, 67, 43, 26, 10, 0 };

    /**
     * Distribution of the pitch contour, 10 ms frames, wideband (12 entries).
     * <p>
     * 피치 윤곽의 분포, 10 ms 프레임, 광대역(12개).
     */
    static final int[] PITCH_CONTOUR_10MS_ICDF = {
        165, 119, 80, 61, 47, 35, 27, 20, 14, 9, 4, 0 };

    /**
     * Distribution of the pitch contour, 10 ms frames, narrowband (3 entries).
     * <p>
     * 피치 윤곽의 분포, 10 ms 프레임, 협대역(3개).
     */
    static final int[] PITCH_CONTOUR_10MS_NB_ICDF = {
        113, 63, 0 };

    /**
     * Distributions of the pulse count of a shell block per rate level (10 x 18).
     * <p>
     * 속도 수준별 셸 블록의 펄스 수 분포(10 x 18).
     */
    static final int[] PULSES_PER_BLOCK_ICDF = {
        125, 51, 26, 18, 15, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2,
        1, 0, 198, 105, 45, 22, 15, 12, 11, 10, 9, 8, 7, 6, 5, 4,
        3, 2, 1, 0, 213, 162, 116, 83, 59, 43, 32, 24, 18, 15, 12, 9,
        7, 6, 5, 3, 2, 0, 239, 187, 116, 59, 28, 16, 11, 10, 9, 8,
        7, 6, 5, 4, 3, 2, 1, 0, 250, 229, 188, 135, 86, 51, 30, 19,
        13, 10, 8, 6, 5, 4, 3, 2, 1, 0, 249, 235, 213, 185, 156, 128,
        103, 83, 66, 53, 42, 33, 26, 21, 17, 13, 10, 0, 254, 249, 235, 206,
        164, 118, 77, 46, 27, 16, 10, 7, 5, 4, 3, 2, 1, 0, 255, 253,
        249, 239, 220, 191, 156, 119, 85, 57, 37, 23, 15, 10, 6, 4, 2, 0,
        255, 253, 251, 246, 237, 223, 203, 179, 152, 124, 98, 75, 55, 40, 29, 21,
        15, 0, 255, 254, 253, 247, 220, 162, 106, 67, 42, 28, 18, 12, 9, 6,
        4, 3, 2, 0 };

    /**
     * Distribution of the rate level per signal class (2 x 9).
     * <p>
     * 신호 종류별 속도 수준의 분포(2 x 9).
     */
    static final int[] RATE_LEVELS_ICDF = {
        241, 190, 178, 132, 87, 74, 41, 14, 0, 223, 193, 157, 140, 106, 57, 39,
        18, 0 };

    /**
     * Shell coder split distributions, level 0 (pairs of coefficients).
     * <p>
     * 셸 부호기 분할 분포, 수준 0(계수 쌍).
     */
    static final int[] SHELL_CODE_TABLE0 = {
        128, 0, 214, 42, 0, 235, 128, 21, 0, 244, 184, 72, 11, 0, 248, 214,
        128, 42, 7, 0, 248, 225, 170, 80, 25, 5, 0, 251, 236, 198, 126, 54,
        18, 3, 0, 250, 238, 211, 159, 82, 35, 15, 5, 0, 250, 231, 203, 168,
        128, 88, 53, 25, 6, 0, 252, 238, 216, 185, 148, 108, 71, 40, 18, 4,
        0, 253, 243, 225, 199, 166, 128, 90, 57, 31, 13, 3, 0, 254, 246, 233,
        212, 183, 147, 109, 73, 44, 23, 10, 2, 0, 255, 250, 240, 223, 198, 166,
        128, 90, 58, 33, 16, 6, 1, 0, 255, 251, 244, 231, 210, 181, 146, 110,
        75, 46, 25, 12, 5, 1, 0, 255, 253, 248, 238, 221, 196, 164, 128, 92,
        60, 35, 18, 8, 3, 1, 0, 255, 253, 249, 242, 229, 208, 180, 146, 110,
        76, 48, 27, 14, 7, 3, 1, 0 };

    /**
     * Shell coder split distributions, level 1.
     * <p>
     * 셸 부호기 분할 분포, 수준 1.
     */
    static final int[] SHELL_CODE_TABLE1 = {
        129, 0, 207, 50, 0, 236, 129, 20, 0, 245, 185, 72, 10, 0, 249, 213,
        129, 42, 6, 0, 250, 226, 169, 87, 27, 4, 0, 251, 233, 194, 130, 62,
        20, 4, 0, 250, 236, 207, 160, 99, 47, 17, 3, 0, 255, 240, 217, 182,
        131, 81, 41, 11, 1, 0, 255, 254, 233, 201, 159, 107, 61, 20, 2, 1,
        0, 255, 249, 233, 206, 170, 128, 86, 50, 23, 7, 1, 0, 255, 250, 238,
        217, 186, 148, 108, 70, 39, 18, 6, 1, 0, 255, 252, 243, 226, 200, 166,
        128, 90, 56, 30, 13, 4, 1, 0, 255, 252, 245, 231, 209, 180, 146, 110,
        76, 47, 25, 11, 4, 1, 0, 255, 253, 248, 237, 219, 194, 163, 128, 93,
        62, 37, 19, 8, 3, 1, 0, 255, 254, 250, 241, 226, 205, 177, 145, 111,
        79, 51, 30, 15, 6, 2, 1, 0 };

    /**
     * Shell coder split distributions, level 2.
     * <p>
     * 셸 부호기 분할 분포, 수준 2.
     */
    static final int[] SHELL_CODE_TABLE2 = {
        129, 0, 203, 54, 0, 234, 129, 23, 0, 245, 184, 73, 10, 0, 250, 215,
        129, 41, 5, 0, 252, 232, 173, 86, 24, 3, 0, 253, 240, 200, 129, 56,
        15, 2, 0, 253, 244, 217, 164, 94, 38, 10, 1, 0, 253, 245, 226, 189,
        132, 71, 27, 7, 1, 0, 253, 246, 231, 203, 159, 105, 56, 23, 6, 1,
        0, 255, 248, 235, 213, 179, 133, 85, 47, 19, 5, 1, 0, 255, 254, 243,
        221, 194, 159, 117, 70, 37, 12, 2, 1, 0, 255, 254, 248, 234, 208, 171,
        128, 85, 48, 22, 8, 2, 1, 0, 255, 254, 250, 240, 220, 189, 149, 107,
        67, 36, 16, 6, 2, 1, 0, 255, 254, 251, 243, 227, 201, 166, 128, 90,
        55, 29, 13, 5, 2, 1, 0, 255, 254, 252, 246, 234, 213, 183, 147, 109,
        73, 43, 22, 10, 4, 2, 1, 0 };

    /**
     * Shell coder split distributions, level 3.
     * <p>
     * 셸 부호기 분할 분포, 수준 3.
     */
    static final int[] SHELL_CODE_TABLE3 = {
        130, 0, 200, 58, 0, 231, 130, 26, 0, 244, 184, 76, 12, 0, 249, 214,
        130, 43, 6, 0, 252, 232, 173, 87, 24, 3, 0, 253, 241, 203, 131, 56,
        14, 2, 0, 254, 246, 221, 167, 94, 35, 8, 1, 0, 254, 249, 232, 193,
        130, 65, 23, 5, 1, 0, 255, 251, 239, 211, 162, 99, 45, 15, 4, 1,
        0, 255, 251, 243, 223, 186, 131, 74, 33, 11, 3, 1, 0, 255, 252, 245,
        230, 202, 158, 105, 57, 24, 8, 2, 1, 0, 255, 253, 247, 235, 214, 179,
        132, 84, 44, 19, 7, 2, 1, 0, 255, 254, 250, 240, 223, 196, 159, 112,
        69, 36, 15, 6, 2, 1, 0, 255, 254, 253, 245, 231, 209, 176, 136, 93,
        55, 27, 11, 3, 2, 1, 0, 255, 254, 253, 252, 239, 221, 194, 158, 117,
        76, 42, 18, 4, 3, 2, 1, 0 };

    /**
     * Offsets of the distributions in the shell code tables per pulse count.
     * <p>
     * 펄스 수별 셸 부호 표 안의 분포 위치.
     */
    static final int[] SHELL_CODE_TABLE_OFFSETS = {
        0, 0, 2, 5, 9, 14, 20, 27, 35, 44, 54, 65, 77, 90, 104, 119,
        135 };

    /**
     * Distributions of the pulse signs (42 entries).
     * <p>
     * 펄스 부호의 분포(42개).
     */
    static final int[] SIGN_ICDF = {
        254, 49, 67, 77, 82, 93, 99, 198, 11, 18, 24, 31, 36, 45, 255, 46,
        66, 78, 87, 94, 104, 208, 14, 21, 32, 42, 51, 66, 255, 94, 104, 109,
        112, 115, 118, 248, 53, 69, 80, 88, 95, 102 };

    /**
     * Pitch contour offsets of stage 2, 10 ms frames (2 x 3).
     * <p>
     * 2단계 피치 윤곽 오프셋, 10 ms 프레임(2 x 3).
     */
    static final int[] CB_LAGS_STAGE2_10MS = {
        0, 1, 0, 0, 0, 1 };

    /**
     * Pitch contour offsets of stage 3, 10 ms frames (2 x 12).
     * <p>
     * 3단계 피치 윤곽 오프셋, 10 ms 프레임(2 x 12).
     */
    static final int[] CB_LAGS_STAGE3_10MS = {
        0, 0, 1, -1, 1, -1, 2, -2, 2, -2, 3, -3, 0, 1, 0, 1,
        -1, 2, -1, 2, -2, 3, -2, 3 };

    /**
     * Pitch contour offsets of stage 2, 20 ms frames (4 x 11).
     * <p>
     * 2단계 피치 윤곽 오프셋, 20 ms 프레임(4 x 11).
     */
    static final int[] CB_LAGS_STAGE2 = {
        0, 2, -1, -1, -1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0,
        0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0,
        0, 0, -1, 2, 1, 0, 1, 1, 0, 0, -1, -1 };

    /**
     * Pitch contour offsets of stage 3, 20 ms frames (4 x 34).
     * <p>
     * 3단계 피치 윤곽 오프셋, 20 ms 프레임(4 x 34).
     */
    static final int[] CB_LAGS_STAGE3 = {
        0, 0, 1, -1, 0, 1, -1, 0, -1, 1, -2, 2, -2, -2, 2, -3,
        2, 3, -3, -4, 3, -4, 4, 4, -5, 5, -6, -5, 6, -7, 6, 5,
        8, -9, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0,
        1, -1, 0, 1, -1, -1, 1, -1, 2, 1, -1, 2, -2, -2, 2, -2,
        2, 2, 3, -3, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0,
        0, 1, -1, 1, 0, 0, 2, 1, -1, 2, -1, -1, 2, -1, 2, 2,
        -1, 3, -2, -2, -2, 3, 0, 1, 0, 0, 1, 0, 1, -1, 2, -1,
        2, -1, 2, 3, -2, 3, -2, -2, 4, 4, -3, 5, -3, -4, 6, -4,
        6, 5, -5, 8, -6, -5, -7, 9 };

    /**
     * Cosine table in Q12 for the conversion of line spectral frequencies (129 entries).
     * <p>
     * 선 스펙트럼 주파수 변환용 Q12 코사인 표(129개).
     */
    static final int[] LSF_COS_TAB_Q12 = {
        8192, 8190, 8182, 8170, 8152, 8130, 8104, 8072, 8034, 7994, 7946, 7896, 7840, 7778, 7714, 7644,
        7568, 7490, 7406, 7318, 7226, 7128, 7026, 6922, 6812, 6698, 6580, 6458, 6332, 6204, 6070, 5934,
        5792, 5648, 5502, 5352, 5198, 5040, 4880, 4718, 4552, 4382, 4212, 4038, 3862, 3684, 3502, 3320,
        3136, 2948, 2760, 2570, 2378, 2186, 1990, 1794, 1598, 1400, 1202, 1002, 802, 602, 402, 202,
        0, -202, -402, -602, -802, -1002, -1202, -1400, -1598, -1794, -1990, -2186, -2378, -2570, -2760, -2948,
        -3136, -3320, -3502, -3684, -3862, -4038, -4212, -4382, -4552, -4718, -4880, -5040, -5198, -5352, -5502, -5648,
        -5792, -5934, -6070, -6204, -6332, -6458, -6580, -6698, -6812, -6922, -7026, -7128, -7226, -7318, -7406, -7490,
        -7568, -7644, -7714, -7778, -7840, -7896, -7946, -7994, -8034, -8072, -8104, -8130, -8152, -8170, -8182, -8190,
        -8192 };

}
