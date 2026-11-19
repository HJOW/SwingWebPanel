package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The state and the frame decoding of one SILK channel (RFC 6716 section 4.2): the side information (frame type, gains, line
 * spectral frequencies, pitch lag, long-term prediction filters), the excitation pulses with the shell coder, and the synthesis
 * through the long-term and short-term prediction filters, together with the packet loss concealment and the comfort noise
 * generation (<code>silk/decode_frame.c</code>, <code>decode_indices.c</code>, <code>decode_parameters.c</code>,
 * <code>decode_pulses.c</code>, <code>shell_coder.c</code>, <code>code_signs.c</code>, <code>decode_core.c</code>,
 * <code>decoder_set_fs.c</code>, <code>PLC.c</code>, <code>CNG.c</code>). Integer arithmetic exactly as in the reference
 * decoder; the fields mirror the reference decoder state. Not thread safe.
 * <p>
 * SILK 채널 하나의 상태와 프레임 디코딩(RFC 6716 4.2): 부가 정보(프레임 종류, 이득, 선 스펙트럼 주파수, 피치 지연, 장기 예측 필터), 셸 부호기로
 * 부호화한 여기 펄스, 장기/단기 예측 필터를 통한 합성, 그리고 패킷 손실 은닉과 편안한 잡음 생성(<code>silk/decode_frame.c</code>,
 * <code>decode_indices.c</code>, <code>decode_parameters.c</code>, <code>decode_pulses.c</code>, <code>shell_coder.c</code>,
 * <code>code_signs.c</code>, <code>decode_core.c</code>, <code>decoder_set_fs.c</code>, <code>PLC.c</code>, <code>CNG.c</code>).
 * 참조 디코더와 똑같은 정수 연산을 쓰며 필드는 참조 디코더의 상태를 따른다. 스레드에 안전하지 않다.
 */
final class SilkChannel {

    /**
     * Frame decoding mode: normal.
     * <p>
     * 프레임 디코딩 모드: 정상.
     */
    static final int FLAG_DECODE_NORMAL = 0;

    /**
     * Frame decoding mode: the packet was lost.
     * <p>
     * 프레임 디코딩 모드: 패킷 손실.
     */
    static final int FLAG_PACKET_LOST = 1;

    /**
     * Frame decoding mode: decode the redundant low-bitrate copy.
     * <p>
     * 프레임 디코딩 모드: 중복 저비트율 사본을 디코딩.
     */
    static final int FLAG_DECODE_LBRR = 2;

    /**
     * Frame coding: independent of the previous frame.
     * <p>
     * 프레임 부호화: 이전 프레임과 독립.
     */
    static final int CODE_INDEPENDENTLY = 0;

    /**
     * Frame coding: independent, without LTP scaling.
     * <p>
     * 프레임 부호화: 독립, LTP 크기 조정 없음.
     */
    static final int CODE_INDEPENDENTLY_NO_LTP_SCALING = 1;

    /**
     * Frame coding: conditional on the previous frame.
     * <p>
     * 프레임 부호화: 이전 프레임에 조건부.
     */
    static final int CODE_CONDITIONALLY = 2;

    /**
     * Signal type: no voice activity.
     * <p>
     * 신호 종류: 음성 활동 없음.
     */
    static final int TYPE_NO_VOICE_ACTIVITY = 0;

    /**
     * Signal type: unvoiced.
     * <p>
     * 신호 종류: 무성음.
     */
    static final int TYPE_UNVOICED = 1;

    /**
     * Signal type: voiced.
     * <p>
     * 신호 종류: 유성음.
     */
    static final int TYPE_VOICED = 2;

    /**
     * Largest frame length in samples.
     * <p>
     * 최대 프레임 길이(샘플).
     */
    static final int MAX_FRAME_LENGTH = 320;

    /**
     * Largest sub-frame length in samples.
     * <p>
     * 최대 서브프레임 길이(샘플).
     */
    static final int MAX_SUB_FRAME_LENGTH = 80;

    /**
     * Largest LPC order.
     * <p>
     * 최대 LPC 차수.
     */
    static final int MAX_LPC_ORDER = 16;

    /**
     * Number of taps of the long-term predictor.
     * <p>
     * 장기 예측기의 탭 수.
     */
    static final int LTP_ORDER = 5;

    /**
     * Largest number of sub-frames per frame.
     * <p>
     * 프레임당 최대 서브프레임 수.
     */
    static final int MAX_NB_SUBFR = 4;

    /**
     * Number of samples in a block of the shell coder.
     * <p>
     * 셸 부호기 블록의 샘플 수.
     */
    private static final int SHELL_CODEC_FRAME_LENGTH = 16;

    /**
     * Largest pulse count of a block before the extension bits.
     * <p>
     * 확장 비트 전 블록의 최대 펄스 수.
     */
    private static final int MAX_PULSES = 16;

    /**
     * Number of rate levels.
     * <p>
     * 속도 수준의 수.
     */
    private static final int N_RATE_LEVELS = 10;

    /**
     * Gain scale constants of the gain dequantization.
     * <p>
     * 이득 역양자화의 상수.
     */
    private static final int GAIN_OFFSET = (2 * 128) / 6 + 16 * 128;

    /**
     * Inverse scale of the gain dequantization in Q16.
     * <p>
     * Q16 이득 역양자화의 역 크기.
     */
    private static final int INV_SCALE_Q16 = (65536 * (((88 - 2) * 128) / 6)) / (64 - 1);

    /**
     * Bandwidth expansion after a loss in Q16.
     * <p>
     * 손실 뒤 대역폭 확장(Q16).
     */
    private static final int BWE_AFTER_LOSS_Q16 = 63570;

    /**
     * Attenuation of the harmonic part of the concealment per lost frame (Q15).
     * <p>
     * 손실 프레임마다 은닉의 하모닉 성분 감쇠(Q15).
     */
    private static final int[] HARM_ATT_Q15 = { 32440, 31130 };

    /**
     * Attenuation of the noise part of the concealment for voiced frames (Q15).
     * <p>
     * 유성음 프레임 은닉의 잡음 성분 감쇠(Q15).
     */
    private static final int[] PLC_RAND_ATTENUATE_V_Q15 = { 31130, 26214 };

    /**
     * Attenuation of the noise part of the concealment for unvoiced frames (Q15).
     * <p>
     * 무성음 프레임 은닉의 잡음 성분 감쇠(Q15).
     */
    private static final int[] PLC_RAND_ATTENUATE_UV_Q15 = { 32440, 29491 };

    /**
     * Gain of the previous sub-frame in Q16.
     * <p>
     * 이전 서브프레임의 이득(Q16).
     */
    int prevGainQ16;

    /**
     * Excitation signal of the frame in Q14.
     * <p>
     * 프레임의 여기 신호(Q14).
     */
    final int[] excQ14 = new int[MAX_FRAME_LENGTH];

    /**
     * Last LPC synthesis states.
     * <p>
     * 마지막 LPC 합성 상태.
     */
    final int[] sLpcQ14Buf = new int[MAX_LPC_ORDER];

    /**
     * Output history for the long-term predictor.
     * <p>
     * 장기 예측기를 위한 출력 이력.
     */
    final short[] outBuf = new short[MAX_FRAME_LENGTH + 2 * MAX_SUB_FRAME_LENGTH];

    /**
     * Pitch lag of the last sub-frame.
     * <p>
     * 마지막 서브프레임의 피치 지연.
     */
    int lagPrev;

    /**
     * Index of the last gain.
     * <p>
     * 마지막 이득의 번호.
     */
    int lastGainIndex;

    /**
     * Internal sampling rate in kHz (8, 12 or 16).
     * <p>
     * 내부 샘플링 주파수(kHz, 8, 12, 16).
     */
    int fsKHz;

    /**
     * Output sampling rate in Hz.
     * <p>
     * 출력 샘플링 주파수(Hz).
     */
    int fsApiHz;

    /**
     * Number of sub-frames per frame (4, or 2 for 10 ms frames).
     * <p>
     * 프레임당 서브프레임 수(4, 10 ms 프레임은 2).
     */
    int nbSubfr;

    /**
     * Frame length in samples.
     * <p>
     * 프레임 길이(샘플).
     */
    int frameLength;

    /**
     * Sub-frame length in samples.
     * <p>
     * 서브프레임 길이(샘플).
     */
    int subfrLength;

    /**
     * Length of the long-term prediction memory in samples.
     * <p>
     * 장기 예측 메모리의 길이(샘플).
     */
    int ltpMemLength;

    /**
     * LPC order (10 or 16).
     * <p>
     * LPC 차수(10 또는 16).
     */
    int lpcOrder;

    /**
     * NLSFs of the previous frame.
     * <p>
     * 이전 프레임의 NLSF.
     */
    final int[] prevNlsfQ15 = new int[MAX_LPC_ORDER];

    /**
     * True for the first frame after a reset.
     * <p>
     * 리셋 뒤 첫 프레임이면 true.
     */
    boolean firstFrameAfterReset;

    /**
     * Distribution of the low bits of the pitch lag.
     * <p>
     * 피치 지연 하위 비트의 분포.
     */
    int[] pitchLagLowBitsIcdf;

    /**
     * Distribution of the pitch contour.
     * <p>
     * 피치 윤곽의 분포.
     */
    int[] pitchContourIcdf;

    /**
     * Number of frames of the packet decoded so far.
     * <p>
     * 지금까지 디코딩한 패킷 내 프레임 수.
     */
    int nFramesDecoded;

    /**
     * Number of frames in the packet.
     * <p>
     * 패킷의 프레임 수.
     */
    int nFramesPerPacket;

    /**
     * Signal type of the previous frame as seen by the entropy decoder.
     * <p>
     * 엔트로피 디코더가 본 이전 프레임의 신호 종류.
     */
    int ecPrevSignalType;

    /**
     * Pitch lag index of the previous frame.
     * <p>
     * 이전 프레임의 피치 지연 번호.
     */
    int ecPrevLagIndex;

    /**
     * Voice activity flag per frame.
     * <p>
     * 프레임별 음성 활동 플래그.
     */
    final int[] vadFlags = new int[3];

    /**
     * Whether the packet carries redundant low-bitrate frames.
     * <p>
     * 패킷에 중복 저비트율 프레임이 있는지.
     */
    int lbrrFlag;

    /**
     * Redundant frame flag per frame.
     * <p>
     * 프레임별 중복 프레임 플래그.
     */
    final int[] lbrrFlags = new int[3];

    /**
     * The resampler of this channel.
     * <p>
     * 이 채널의 리샘플러.
     */
    final SilkResampler resampler = new SilkResampler();

    /**
     * NLSF codebook of the current rate.
     * <p>
     * 현재 주파수의 NLSF 부호책.
     */
    SilkNlsf.Codebook nlsfCb;

    /**
     * Gain indices of the sub-frames.
     * <p>
     * 서브프레임의 이득 번호.
     */
    final int[] gainsIndices = new int[MAX_NB_SUBFR];

    /**
     * LTP filter indices of the sub-frames.
     * <p>
     * 서브프레임의 LTP 필터 번호.
     */
    final int[] ltpIndex = new int[MAX_NB_SUBFR];

    /**
     * NLSF indices: stage 1 followed by the residuals.
     * <p>
     * NLSF 번호: 1단계 번호와 잔차.
     */
    final int[] nlsfIndices = new int[MAX_LPC_ORDER + 1];

    /**
     * Pitch lag index.
     * <p>
     * 피치 지연 번호.
     */
    int lagIndex;

    /**
     * Pitch contour index.
     * <p>
     * 피치 윤곽 번호.
     */
    int contourIndex;

    /**
     * Signal type of the frame.
     * <p>
     * 프레임의 신호 종류.
     */
    int signalType;

    /**
     * Quantization offset type of the frame.
     * <p>
     * 프레임의 양자화 오프셋 종류.
     */
    int quantOffsetType;

    /**
     * NLSF interpolation factor in Q2 (4 means no interpolation).
     * <p>
     * Q2 NLSF 보간 계수(4 는 보간 없음).
     */
    int nlsfInterpCoefQ2;

    /**
     * Index of the LTP filter family (periodicity).
     * <p>
     * LTP 필터 군(주기성)의 번호.
     */
    int perIndex;

    /**
     * Index of the LTP scaling.
     * <p>
     * LTP 크기 조정 번호.
     */
    int ltpScaleIndex;

    /**
     * Seed of the excitation noise.
     * <p>
     * 여기 잡음의 시드.
     */
    int seed;

    /**
     * Comfort noise: excitation memory.
     * <p>
     * 편안한 잡음: 여기 메모리.
     */
    final int[] cngExcBufQ14 = new int[MAX_FRAME_LENGTH];

    /**
     * Comfort noise: smoothed NLSFs.
     * <p>
     * 편안한 잡음: 평활한 NLSF.
     */
    final int[] cngSmthNlsfQ15 = new int[MAX_LPC_ORDER];

    /**
     * Comfort noise: synthesis filter state.
     * <p>
     * 편안한 잡음: 합성 필터 상태.
     */
    final int[] cngSynthState = new int[MAX_LPC_ORDER];

    /**
     * Comfort noise: smoothed gain.
     * <p>
     * 편안한 잡음: 평활한 이득.
     */
    int cngSmthGainQ16;

    /**
     * Comfort noise: noise seed.
     * <p>
     * 편안한 잡음: 잡음 시드.
     */
    int cngRandSeed;

    /**
     * Comfort noise: rate it was set up for.
     * <p>
     * 편안한 잡음: 설정된 주파수.
     */
    int cngFsKHz;

    /**
     * Number of consecutive lost frames.
     * <p>
     * 연속으로 손실된 프레임 수.
     */
    int lossCnt;

    /**
     * Signal type of the previous frame.
     * <p>
     * 이전 프레임의 신호 종류.
     */
    int prevSignalType;

    /**
     * Concealment: pitch lag in Q8.
     * <p>
     * 은닉: Q8 피치 지연.
     */
    int plcPitchLQ8;

    /**
     * Concealment: LTP filter.
     * <p>
     * 은닉: LTP 필터.
     */
    final int[] plcLtpCoefQ14 = new int[LTP_ORDER];

    /**
     * Concealment: LPC filter of the last good frame.
     * <p>
     * 은닉: 마지막 정상 프레임의 LPC 필터.
     */
    final short[] plcPrevLpcQ12 = new short[MAX_LPC_ORDER];

    /**
     * Concealment: whether the last frame was concealed.
     * <p>
     * 은닉: 마지막 프레임이 은닉된 것이었는지.
     */
    boolean plcLastFrameLost;

    /**
     * Concealment: noise seed.
     * <p>
     * 은닉: 잡음 시드.
     */
    int plcRandSeed;

    /**
     * Concealment: scale of the noise in Q14.
     * <p>
     * 은닉: Q14 잡음 크기.
     */
    int plcRandScaleQ14;

    /**
     * Concealment: energy of the concealed frame.
     * <p>
     * 은닉: 은닉 프레임의 에너지.
     */
    int plcConcEnergy;

    /**
     * Concealment: shift of that energy.
     * <p>
     * 은닉: 그 에너지의 shift.
     */
    int plcConcEnergyShift;

    /**
     * Concealment: LTP scaling of the last good frame.
     * <p>
     * 은닉: 마지막 정상 프레임의 LTP 크기 조정.
     */
    int plcPrevLtpScaleQ14;

    /**
     * Concealment: gains of the last two sub-frames.
     * <p>
     * 은닉: 마지막 서브프레임 둘의 이득.
     */
    final int[] plcPrevGainQ16 = new int[2];

    /**
     * Concealment: rate it was set up for.
     * <p>
     * 은닉: 설정된 주파수.
     */
    int plcFsKHz;

    /**
     * Concealment: number of sub-frames.
     * <p>
     * 은닉: 서브프레임 수.
     */
    int plcNbSubfr;

    /**
     * Concealment: sub-frame length.
     * <p>
     * 은닉: 서브프레임 길이.
     */
    int plcSubfrLength;

    /**
     * Control data: pitch lags of the sub-frames.
     * <p>
     * 제어 데이터: 서브프레임의 피치 지연.
     */
    final int[] pitchL = new int[MAX_NB_SUBFR];

    /**
     * Control data: gains of the sub-frames in Q16.
     * <p>
     * 제어 데이터: 서브프레임의 이득(Q16).
     */
    final int[] gainsQ16 = new int[MAX_NB_SUBFR];

    /**
     * Control data: LPC coefficients of the two halves of the frame in Q12 (2 x 16).
     * <p>
     * 제어 데이터: 프레임 앞뒤 절반의 LPC 계수(Q12, 2 x 16).
     */
    final short[] predCoefQ12 = new short[2 * MAX_LPC_ORDER];

    /**
     * Control data: LTP filters of the sub-frames in Q14 (4 x 5).
     * <p>
     * 제어 데이터: 서브프레임의 LTP 필터(Q14, 4 x 5).
     */
    final short[] ltpCoefQ14 = new short[LTP_ORDER * MAX_NB_SUBFR];

    /**
     * Control data: LTP scaling in Q14.
     * <p>
     * 제어 데이터: LTP 크기 조정(Q14).
     */
    int ltpScaleQ14;

    /**
     * Creates a channel in the initial state.
     * <p>
     * 초기 상태의 채널을 만든다.
     */
    SilkChannel() {
        init();
    }

    /**
     * Resets the channel to the initial state (<code>silk_init_decoder</code>).
     * <p>
     * 채널을 초기 상태로 되돌린다(<code>silk_init_decoder</code>).
     */
    void init() {
        prevGainQ16 = 65536;
        java.util.Arrays.fill(excQ14, 0);
        java.util.Arrays.fill(sLpcQ14Buf, 0);
        java.util.Arrays.fill(outBuf, (short) 0);
        lagPrev = 0;
        lastGainIndex = 0;
        fsKHz = 0;
        fsApiHz = 0;
        nbSubfr = 0;
        frameLength = 0;
        subfrLength = 0;
        ltpMemLength = 0;
        lpcOrder = 0;
        java.util.Arrays.fill(prevNlsfQ15, 0);
        firstFrameAfterReset = true;
        pitchLagLowBitsIcdf = null;
        pitchContourIcdf = null;
        nFramesDecoded = 0;
        nFramesPerPacket = 0;
        ecPrevSignalType = 0;
        ecPrevLagIndex = 0;
        java.util.Arrays.fill(vadFlags, 0);
        lbrrFlag = 0;
        java.util.Arrays.fill(lbrrFlags, 0);
        resampler.init(8000, 8000);
        nlsfCb = null;
        java.util.Arrays.fill(gainsIndices, 0);
        java.util.Arrays.fill(ltpIndex, 0);
        java.util.Arrays.fill(nlsfIndices, 0);
        lagIndex = 0;
        contourIndex = 0;
        signalType = 0;
        quantOffsetType = 0;
        nlsfInterpCoefQ2 = 0;
        perIndex = 0;
        ltpScaleIndex = 0;
        seed = 0;
        java.util.Arrays.fill(cngExcBufQ14, 0);
        java.util.Arrays.fill(cngSynthState, 0);
        cngFsKHz = 0;
        lossCnt = 0;
        prevSignalType = 0;
        plcRandSeed = 0;
        plcRandScaleQ14 = 0;
        plcLastFrameLost = false;
        plcConcEnergy = 0;
        plcConcEnergyShift = 0;
        plcPrevLtpScaleQ14 = 0;
        plcFsKHz = 0;
        java.util.Arrays.fill(plcLtpCoefQ14, 0);
        java.util.Arrays.fill(plcPrevLpcQ12, (short) 0);
        java.util.Arrays.fill(pitchL, 0);
        java.util.Arrays.fill(gainsQ16, 0);
        java.util.Arrays.fill(predCoefQ12, (short) 0);
        java.util.Arrays.fill(ltpCoefQ14, (short) 0);
        ltpScaleQ14 = 0;
        cngReset();
        plcReset();
    }

    /**
     * Resets the comfort noise generator.
     * <p>
     * 편안한 잡음 발생기를 리셋한다.
     */
    private void cngReset() {
        int nlsfStepQ15 = 32767 / (lpcOrder + 1);
        int nlsfAccQ15 = 0;
        java.util.Arrays.fill(cngSmthNlsfQ15, 0);
        // NLSF 를 0 과 1 사이에 균등하게 배치한다.
        for (int i = 0; i < lpcOrder; i++) {
            nlsfAccQ15 += nlsfStepQ15;
            cngSmthNlsfQ15[i] = nlsfAccQ15;
        }
        cngSmthGainQ16 = 0;
        cngRandSeed = 3176576;
    }

    /**
     * Resets the packet loss concealment.
     * <p>
     * 패킷 손실 은닉을 리셋한다.
     */
    private void plcReset() {
        plcPitchLQ8 = frameLength << (8 - 1);
        plcPrevGainQ16[0] = 65536;
        plcPrevGainQ16[1] = 65536;
        plcSubfrLength = 20;
        plcNbSubfr = 2;
    }

    /**
     * Sets the internal and the output sampling rate (<code>silk_decoder_set_fs</code>).
     * <p>
     * 내부 주파수와 출력 주파수를 정한다(<code>silk_decoder_set_fs</code>).
     *
     * @param fsKHzNew internal rate in kHz, 8, 12 or 16 / 내부 주파수(kHz, 8, 12, 16)
     * @param fsApiHzNew output rate in Hz / 출력 주파수(Hz)
     * @return 0 on success / 성공이면 0
     */
    int setFs(int fsKHzNew, int fsApiHzNew) {
        int ret = 0;
        subfrLength = 5 * fsKHzNew;
        int newFrameLength = nbSubfr * subfrLength;
        if (fsKHz != fsKHzNew || fsApiHz != fsApiHzNew) {
            ret += resampler.init(fsKHzNew * 1000, fsApiHzNew);
            fsApiHz = fsApiHzNew;
        }
        if (fsKHz != fsKHzNew || newFrameLength != frameLength) {
            if (fsKHzNew == 8) {
                pitchContourIcdf = nbSubfr == MAX_NB_SUBFR ? SilkTables.PITCH_CONTOUR_NB_ICDF : SilkTables.PITCH_CONTOUR_10MS_NB_ICDF;
            } else {
                pitchContourIcdf = nbSubfr == MAX_NB_SUBFR ? SilkTables.PITCH_CONTOUR_ICDF : SilkTables.PITCH_CONTOUR_10MS_ICDF;
            }
            if (fsKHz != fsKHzNew) {
                ltpMemLength = 20 * fsKHzNew;
                if (fsKHzNew == 8 || fsKHzNew == 12) {
                    lpcOrder = 10;
                    nlsfCb = SilkNlsf.CB_NB_MB;
                } else {
                    lpcOrder = 16;
                    nlsfCb = SilkNlsf.CB_WB;
                }
                if (fsKHzNew == 16) {
                    pitchLagLowBitsIcdf = SilkTables.UNIFORM8_ICDF;
                } else if (fsKHzNew == 12) {
                    pitchLagLowBitsIcdf = SilkTables.UNIFORM6_ICDF;
                } else {
                    pitchLagLowBitsIcdf = SilkTables.UNIFORM4_ICDF;
                }
                firstFrameAfterReset = true;
                lagPrev = 100;
                lastGainIndex = 10;
                prevSignalType = TYPE_NO_VOICE_ACTIVITY;
                java.util.Arrays.fill(outBuf, (short) 0);
                java.util.Arrays.fill(sLpcQ14Buf, 0);
            }
            fsKHz = fsKHzNew;
            frameLength = newFrameLength;
        }
        return ret;
    }

    /**
     * Decodes the side information of a frame (<code>silk_decode_indices</code>).
     * <p>
     * 프레임의 부가 정보를 디코딩한다(<code>silk_decode_indices</code>).
     *
     * @param rc          the range decoder / 범위 디코더
     * @param frameIndex  index of the frame in the packet / 패킷 안의 프레임 번호
     * @param decodeLbrr  true when decoding a redundant frame / 중복 프레임을 디코딩하면 true
     * @param condCoding  the coding mode of the frame / 프레임의 부호화 모드
     */
    void decodeIndices(OpusRangeDecoder rc, int frameIndex, boolean decodeLbrr, int condCoding) {
        int ix;
        if (decodeLbrr || vadFlags[frameIndex] != 0) {
            ix = rc.icdf(SilkTables.TYPE_OFFSET_VAD_ICDF, 8) + 2;
        } else {
            ix = rc.icdf(SilkTables.TYPE_OFFSET_NO_VAD_ICDF, 8);
        }
        signalType = ix >> 1;
        quantOffsetType = ix & 1;
        if (condCoding == CODE_CONDITIONALLY) {
            gainsIndices[0] = rc.icdf(SilkTables.DELTA_GAIN_ICDF, 8);
        } else {
            gainsIndices[0] = rc.icdf(SilkTables.GAIN_ICDF, signalType * 8, 8) << 3;
            gainsIndices[0] += rc.icdf(SilkTables.UNIFORM8_ICDF, 8);
        }
        // 나머지 서브프레임의 이득은 차분으로 읽는다.
        for (int i = 1; i < nbSubfr; i++) {
            gainsIndices[i] = rc.icdf(SilkTables.DELTA_GAIN_ICDF, 8);
        }
        nlsfIndices[0] = rc.icdf(nlsfCb.cb1Icdf, (signalType >> 1) * nlsfCb.nVectors, 8);
        int[] ecIx = new int[MAX_LPC_ORDER];
        int[] predQ8 = new int[MAX_LPC_ORDER];
        SilkNlsf.unpack(ecIx, predQ8, nlsfCb, nlsfIndices[0]);
        // 계수마다 NLSF 잔차 번호를 읽는다(큰 값은 확장 기호를 더 읽는다).
        for (int i = 0; i < nlsfCb.order; i++) {
            ix = rc.icdf(nlsfCb.ecIcdf, ecIx[i], 8);
            if (ix == 0) {
                ix -= rc.icdf(SilkTables.NLSF_EXT_ICDF, 8);
            } else if (ix == 2 * SilkNlsf.NLSF_QUANT_MAX_AMPLITUDE) {
                ix += rc.icdf(SilkTables.NLSF_EXT_ICDF, 8);
            }
            nlsfIndices[i + 1] = ix - SilkNlsf.NLSF_QUANT_MAX_AMPLITUDE;
        }
        if (nbSubfr == MAX_NB_SUBFR) {
            nlsfInterpCoefQ2 = rc.icdf(SilkTables.NLSF_INTERP_ICDF, 8);
        } else {
            nlsfInterpCoefQ2 = 4;
        }
        if (signalType == TYPE_VOICED) {
            boolean decodeAbsoluteLagIndex = true;
            if (condCoding == CODE_CONDITIONALLY && ecPrevSignalType == TYPE_VOICED) {
                int deltaLagIndex = rc.icdf(SilkTables.PITCH_DELTA_ICDF, 8);
                if (deltaLagIndex > 0) {
                    deltaLagIndex = deltaLagIndex - 9;
                    lagIndex = (short) (ecPrevLagIndex + deltaLagIndex);
                    decodeAbsoluteLagIndex = false;
                }
            }
            if (decodeAbsoluteLagIndex) {
                lagIndex = (short) (rc.icdf(SilkTables.PITCH_LAG_ICDF, 8) * (fsKHz >> 1));
                lagIndex += (short) rc.icdf(pitchLagLowBitsIcdf, 8);
            }
            ecPrevLagIndex = lagIndex;
            contourIndex = rc.icdf(pitchContourIcdf, 8);
            perIndex = rc.icdf(SilkTables.LTP_PER_INDEX_ICDF, 8);
            int[] ltpGainIcdf = perIndex == 0 ? SilkTables.LTP_GAIN_ICDF_0 : (perIndex == 1 ? SilkTables.LTP_GAIN_ICDF_1 : SilkTables.LTP_GAIN_ICDF_2);
            // 서브프레임마다 LTP 필터 번호를 읽는다.
            for (int k = 0; k < nbSubfr; k++) {
                ltpIndex[k] = rc.icdf(ltpGainIcdf, 8);
            }
            if (condCoding == CODE_INDEPENDENTLY) {
                ltpScaleIndex = rc.icdf(SilkTables.LTP_SCALE_ICDF, 8);
            } else {
                ltpScaleIndex = 0;
            }
        }
        ecPrevSignalType = signalType;
        seed = rc.icdf(SilkTables.UNIFORM4_ICDF, 8);
    }

    /**
     * Decodes the pulses of the excitation (<code>silk_decode_pulses</code>).
     * <p>
     * 여기 신호의 펄스를 디코딩한다(<code>silk_decode_pulses</code>).
     *
     * @param rc            the range decoder / 범위 디코더
     * @param pulses        receives the signed pulses / 부호 있는 펄스를 받는 배열
     * @param signalTypeIn  signal type of the frame / 프레임의 신호 종류
     * @param quantOffset   quantization offset type / 양자화 오프셋 종류
     * @param length        frame length / 프레임 길이
     */
    static void decodePulses(OpusRangeDecoder rc, int[] pulses, int signalTypeIn, int quantOffset, int length) {
        int[] sumPulses = new int[MAX_FRAME_LENGTH / SHELL_CODEC_FRAME_LENGTH];
        int[] nLshifts = new int[MAX_FRAME_LENGTH / SHELL_CODEC_FRAME_LENGTH];
        int rateLevelIndex = rc.icdf(SilkTables.RATE_LEVELS_ICDF, (signalTypeIn >> 1) * 9, 8);
        int iter = length >> 4;
        if (iter * SHELL_CODEC_FRAME_LENGTH < length) {
            iter++;
        }
        int cdfOff = rateLevelIndex * 18;
        // 블록마다 펄스 합계를 읽고, 최대값이면 확장 비트 수를 늘리며 다시 읽는다.
        for (int i = 0; i < iter; i++) {
            nLshifts[i] = 0;
            sumPulses[i] = rc.icdf(SilkTables.PULSES_PER_BLOCK_ICDF, cdfOff, 8);
            // 조건이 만족되는 동안 반복해서 처리한다.
            while (sumPulses[i] == MAX_PULSES + 1) {
                nLshifts[i]++;
                sumPulses[i] = rc.icdf(SilkTables.PULSES_PER_BLOCK_ICDF, (N_RATE_LEVELS - 1) * 18 + (nLshifts[i] == 10 ? 1 : 0), 8);
            }
        }
        // 블록마다 셸 부호로 펄스를 나눠 읽는다.
        for (int i = 0; i < iter; i++) {
            if (sumPulses[i] > 0) {
                shellDecoder(pulses, i * SHELL_CODEC_FRAME_LENGTH, rc, sumPulses[i]);
            } else {
                java.util.Arrays.fill(pulses, i * SHELL_CODEC_FRAME_LENGTH, (i + 1) * SHELL_CODEC_FRAME_LENGTH, 0);
            }
        }
        // 확장 비트가 있는 블록은 하위 비트를 읽어 붙인다.
        for (int i = 0; i < iter; i++) {
            if (nLshifts[i] > 0) {
                int nls = nLshifts[i];
                int base = i * SHELL_CODEC_FRAME_LENGTH;
                // k 를 0 부터 SHELL_CODEC_FRAME_LENGTH 까지 늘려 가며 항목마다 처리한다.
                for (int k = 0; k < SHELL_CODEC_FRAME_LENGTH; k++) {
                    int absQ = pulses[base + k];
                    // j 를 0 부터 nls 까지 늘려 가며 항목마다 처리한다.
                    for (int j = 0; j < nls; j++) {
                        absQ <<= 1;
                        absQ += rc.icdf(SilkTables.LSB_ICDF, 8);
                    }
                    pulses[base + k] = absQ;
                }
                sumPulses[i] |= nls << 5;
            }
        }
        decodeSigns(rc, pulses, length, signalTypeIn, quantOffset, sumPulses);
    }

    /**
     * Splits the pulse count of a shell block into its two halves.
     * <p>
     * 셸 블록의 펄스 수를 두 절반으로 나눈다.
     *
     * @param out    receives the two halves / 두 절반을 받는 배열
     * @param outOff index of the first half / 첫 절반의 위치
     * @param rc     the range decoder / 범위 디코더
     * @param p      the pulse count to split / 나눌 펄스 수
     * @param table  the distribution table / 분포 표
     */
    private static void decodeSplit(int[] out, int outOff, OpusRangeDecoder rc, int p, int[] table) {
        if (p > 0) {
            out[outOff] = rc.icdf(table, SilkTables.SHELL_CODE_TABLE_OFFSETS[p], 8);
            out[outOff + 1] = p - out[outOff];
        } else {
            out[outOff] = 0;
            out[outOff + 1] = 0;
        }
    }

    /**
     * Decodes the 16 pulse magnitudes of a block with the binary-tree shell code.
     * <p>
     * 이진 트리 셸 부호로 블록의 펄스 크기 16개를 디코딩한다.
     *
     * @param pulses0 receives the magnitudes / 크기를 받는 배열
     * @param off     index of the block / 블록의 위치
     * @param rc      the range decoder / 범위 디코더
     * @param pulses4 the total number of pulses of the block / 블록의 펄스 총수
     */
    private static void shellDecoder(int[] pulses0, int off, OpusRangeDecoder rc, int pulses4) {
        int[] p3 = new int[2];
        int[] p2 = new int[4];
        int[] p1 = new int[8];
        int[] t0 = SilkTables.SHELL_CODE_TABLE0;
        int[] t1 = SilkTables.SHELL_CODE_TABLE1;
        int[] t2 = SilkTables.SHELL_CODE_TABLE2;
        int[] t3 = SilkTables.SHELL_CODE_TABLE3;
        decodeSplit(p3, 0, rc, pulses4, t3);
        decodeSplit(p2, 0, rc, p3[0], t2);
        decodeSplit(p1, 0, rc, p2[0], t1);
        decodeSplit(pulses0, off + 0, rc, p1[0], t0);
        decodeSplit(pulses0, off + 2, rc, p1[1], t0);
        decodeSplit(p1, 2, rc, p2[1], t1);
        decodeSplit(pulses0, off + 4, rc, p1[2], t0);
        decodeSplit(pulses0, off + 6, rc, p1[3], t0);
        decodeSplit(p2, 2, rc, p3[1], t2);
        decodeSplit(p1, 4, rc, p2[2], t1);
        decodeSplit(pulses0, off + 8, rc, p1[4], t0);
        decodeSplit(pulses0, off + 10, rc, p1[5], t0);
        decodeSplit(p1, 6, rc, p2[3], t1);
        decodeSplit(pulses0, off + 12, rc, p1[6], t0);
        decodeSplit(pulses0, off + 14, rc, p1[7], t0);
    }

    /**
     * Decodes the signs of the pulses (<code>silk_decode_signs</code>).
     * <p>
     * 펄스의 부호를 디코딩한다(<code>silk_decode_signs</code>).
     *
     * @param rc          the range decoder / 범위 디코더
     * @param pulses      the magnitudes, which become signed / 크기. 부호가 붙는다
     * @param lengthIn    frame length / 프레임 길이
     * @param signalTypeIn signal type / 신호 종류
     * @param quantOffset quantization offset type / 양자화 오프셋 종류
     * @param sumPulses   pulse count per block (with the extension bits) / 블록당 펄스 수(확장 비트 포함)
     */
    private static void decodeSigns(OpusRangeDecoder rc, int[] pulses, int lengthIn, int signalTypeIn, int quantOffset, int[] sumPulses) {
        int[] icdf = new int[2];
        icdf[1] = 0;
        int iPtr = 7 * (quantOffset + (signalTypeIn << 1));
        int length = (lengthIn + SHELL_CODEC_FRAME_LENGTH / 2) >> 4;
        int q = 0;
        // 블록마다 0 이 아닌 펄스의 부호를 읽는다.
        for (int i = 0; i < length; i++) {
            int p = sumPulses[i];
            if (p > 0) {
                icdf[0] = SilkTables.SIGN_ICDF[iPtr + Math.min(p & 0x1F, 6)];
                // j 를 0 부터 SHELL_CODEC_FRAME_LENGTH 까지 늘려 가며 항목마다 처리한다.
                for (int j = 0; j < SHELL_CODEC_FRAME_LENGTH; j++) {
                    if (pulses[q + j] > 0) {
                        pulses[q + j] *= (rc.icdf(icdf, 8) << 1) - 1;
                    }
                }
            }
            q += SHELL_CODEC_FRAME_LENGTH;
        }
    }

    /**
     * Dequantizes the gains (<code>silk_gains_dequant</code>).
     * <p>
     * 이득을 역양자화한다(<code>silk_gains_dequant</code>).
     *
     * @param conditional true if the first gain is coded as a delta / 첫 이득을 차분으로 부호화했으면 true
     */
    private void gainsDequant(boolean conditional) {
        // 서브프레임마다 이득 번호를 누적하고 선형 이득으로 바꾼다.
        for (int k = 0; k < nbSubfr; k++) {
            if (k == 0 && !conditional) {
                lastGainIndex = Math.max(gainsIndices[k], lastGainIndex - 16);
            } else {
                int indTmp = gainsIndices[k] - 4;
                int doubleStepSizeThreshold = 2 * 36 - 64 + lastGainIndex;
                if (indTmp > doubleStepSizeThreshold) {
                    lastGainIndex += (indTmp << 1) - doubleStepSizeThreshold;
                } else {
                    lastGainIndex += indTmp;
                }
            }
            lastGainIndex = SilkMath.limit(lastGainIndex, 0, 63);
            gainsQ16[k] = SilkMath.log2lin(Math.min(SilkMath.smulwb(INV_SCALE_Q16, lastGainIndex) + GAIN_OFFSET, 3967));
        }
    }

    /**
     * Decodes the filter parameters from the indices (<code>silk_decode_parameters</code>).
     * <p>
     * 번호로부터 필터 매개변수를 디코딩한다(<code>silk_decode_parameters</code>).
     *
     * @param condCoding the coding mode of the frame / 프레임의 부호화 모드
     */
    private void decodeParameters(int condCoding) {
        int[] nlsfQ15 = new int[MAX_LPC_ORDER];
        int[] nlsf0Q15 = new int[MAX_LPC_ORDER];
        gainsDequant(condCoding == CODE_CONDITIONALLY);
        SilkNlsf.decode(nlsfQ15, nlsfIndices, nlsfCb);
        SilkNlsf.nlsf2a(predCoefQ12, MAX_LPC_ORDER, nlsfQ15, lpcOrder);
        if (firstFrameAfterReset) {
            nlsfInterpCoefQ2 = 4;
        }
        if (nlsfInterpCoefQ2 < 4) {
            // 이전 NLSF 와 보간해 프레임 앞쪽 절반의 필터를 만든다.
            for (int i = 0; i < lpcOrder; i++) {
                nlsf0Q15[i] = prevNlsfQ15[i] + ((nlsfInterpCoefQ2 * (nlsfQ15[i] - prevNlsfQ15[i])) >> 2);
            }
            SilkNlsf.nlsf2a(predCoefQ12, 0, nlsf0Q15, lpcOrder);
        } else {
            System.arraycopy(predCoefQ12, MAX_LPC_ORDER, predCoefQ12, 0, lpcOrder);
        }
        System.arraycopy(nlsfQ15, 0, prevNlsfQ15, 0, lpcOrder);
        if (lossCnt != 0) {
            SilkMath.bwexpander(predCoefQ12, 0, lpcOrder, BWE_AFTER_LOSS_Q16);
            SilkMath.bwexpander(predCoefQ12, MAX_LPC_ORDER, lpcOrder, BWE_AFTER_LOSS_Q16);
        }
        if (signalType == TYPE_VOICED) {
            decodePitch();
            int[] cbk = perIndex == 0 ? SilkTables.LTP_VQ_0 : (perIndex == 1 ? SilkTables.LTP_VQ_1 : SilkTables.LTP_VQ_2);
            // 서브프레임마다 LTP 필터 벡터를 Q14 로 옮긴다.
            for (int k = 0; k < nbSubfr; k++) {
                int ix = ltpIndex[k];
                // i 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < LTP_ORDER; i++) {
                    ltpCoefQ14[k * LTP_ORDER + i] = (short) (cbk[ix * LTP_ORDER + i] << 7);
                }
            }
            ltpScaleQ14 = SilkTables.LTP_SCALES_Q14[ltpScaleIndex];
        } else {
            java.util.Arrays.fill(pitchL, 0, nbSubfr, 0);
            java.util.Arrays.fill(ltpCoefQ14, 0, LTP_ORDER * nbSubfr, (short) 0);
            perIndex = 0;
            ltpScaleQ14 = 0;
        }
    }

    /**
     * Computes the pitch lags of the sub-frames from the lag and contour indices (<code>silk_decode_pitch</code>).
     * <p>
     * 지연 번호와 윤곽 번호로 서브프레임의 피치 지연을 계산한다(<code>silk_decode_pitch</code>).
     */
    private void decodePitch() {
        int[] lagCb;
        int cbkSize;
        if (fsKHz == 8) {
            if (nbSubfr == MAX_NB_SUBFR) {
                lagCb = SilkTables.CB_LAGS_STAGE2;
                cbkSize = 11;
            } else {
                lagCb = SilkTables.CB_LAGS_STAGE2_10MS;
                cbkSize = 3;
            }
        } else {
            if (nbSubfr == MAX_NB_SUBFR) {
                lagCb = SilkTables.CB_LAGS_STAGE3;
                cbkSize = 34;
            } else {
                lagCb = SilkTables.CB_LAGS_STAGE3_10MS;
                cbkSize = 12;
            }
        }
        int minLag = 2 * fsKHz;
        int maxLag = 18 * fsKHz;
        int lag = minLag + lagIndex;
        // 서브프레임마다 윤곽 오프셋을 더하고 범위로 제한한다.
        for (int k = 0; k < nbSubfr; k++) {
            pitchL[k] = lag + lagCb[k * cbkSize + contourIndex];
            pitchL[k] = SilkMath.limit(pitchL[k], minLag, maxLag);
        }
    }

    /**
     * LPC analysis (whitening) filter.
     * <p>
     * LPC 분석(백색화) 필터.
     *
     * @param out    receives the residual / 잔차를 받는 배열
     * @param outOff index of the first output sample / 첫 출력 샘플의 위치
     * @param in     the signal / 신호
     * @param inOff  index of the first input sample / 첫 입력 샘플의 위치
     * @param b      the filter coefficients in Q12 / Q12 필터 계수
     * @param bOff   index of the coefficients / 계수의 위치
     * @param len    number of samples / 샘플 수
     * @param d      the order / 차수
     */
    private static void lpcAnalysisFilter(short[] out, int outOff, short[] in, int inOff, short[] b, int bOff, int len, int d) {
        // 처음 d 개를 제외한 샘플마다 예측을 빼 잔차를 구한다.
        for (int ix = d; ix < len; ix++) {
            int p = inOff + ix - 1;
            int out32Q12 = SilkMath.smulbb(in[p], b[bOff]);
            out32Q12 = SilkMath.smlabb(out32Q12, in[p - 1], b[bOff + 1]);
            out32Q12 = SilkMath.smlabb(out32Q12, in[p - 2], b[bOff + 2]);
            out32Q12 = SilkMath.smlabb(out32Q12, in[p - 3], b[bOff + 3]);
            out32Q12 = SilkMath.smlabb(out32Q12, in[p - 4], b[bOff + 4]);
            out32Q12 = SilkMath.smlabb(out32Q12, in[p - 5], b[bOff + 5]);
            // j 를 6 부터 d 까지 늘려 가며 항목마다 처리한다.
            for (int j = 6; j < d; j += 2) {
                out32Q12 = SilkMath.smlabb(out32Q12, in[p - j], b[bOff + j]);
                out32Q12 = SilkMath.smlabb(out32Q12, in[p - j - 1], b[bOff + j + 1]);
            }
            out32Q12 = (in[p + 1] << 12) - out32Q12;
            int out32 = SilkMath.rshiftRound(out32Q12, 12);
            out[outOff + ix] = (short) SilkMath.sat16(out32);
        }
        java.util.Arrays.fill(out, outOff, outOff + d, (short) 0);
    }

    /**
     * Synthesizes the output of a frame from the excitation pulses and the filters (<code>silk_decode_core</code>).
     * <p>
     * 여기 펄스와 필터로 프레임의 출력을 합성한다(<code>silk_decode_core</code>).
     *
     * @param xq     receives the output samples / 출력 샘플을 받는 배열
     * @param xqOff  index of the first output sample / 첫 출력 샘플의 위치
     * @param pulses the excitation pulses / 여기 펄스
     */
    private void decodeCore(short[] xq, int xqOff, int[] pulses) {
        short[] sLtp = new short[MAX_FRAME_LENGTH];
        int[] sLtpQ15 = new int[2 * MAX_FRAME_LENGTH];
        int[] resQ14 = new int[MAX_SUB_FRAME_LENGTH];
        int[] sLpcQ14 = new int[MAX_SUB_FRAME_LENGTH + MAX_LPC_ORDER];
        short[] aTmp = new short[MAX_LPC_ORDER];
        int offsetQ10 = SilkTables.QUANT_OFFSETS_Q10[(signalType >> 1) * 2 + quantOffsetType];
        boolean nlsfInterpolationFlag = nlsfInterpCoefQ2 < (1 << 2);
        int randSeed = seed;
        // 펄스를 여기 신호로 바꾸며 의사 난수로 부호를 뒤집는다.
        for (int i = 0; i < frameLength; i++) {
            randSeed = SilkMath.rand(randSeed);
            excQ14[i] = pulses[i] << 14;
            if (excQ14[i] > 0) {
                excQ14[i] -= 80 << 4;
            } else if (excQ14[i] < 0) {
                excQ14[i] += 80 << 4;
            }
            excQ14[i] += offsetQ10 << 4;
            if (randSeed < 0) {
                excQ14[i] = -excQ14[i];
            }
            randSeed = randSeed + pulses[i];
        }
        System.arraycopy(sLpcQ14Buf, 0, sLpcQ14, 0, MAX_LPC_ORDER);
        int pexc = 0;
        int pxq = xqOff;
        int sLtpBufIdx = ltpMemLength;
        // 서브프레임마다 합성한다.
        for (int k = 0; k < nbSubfr; k++) {
            int aOff = (k >> 1) * MAX_LPC_ORDER;
            System.arraycopy(predCoefQ12, aOff, aTmp, 0, lpcOrder);
            int bOff = k * LTP_ORDER;
            int sigType = signalType;
            int gainQ10 = gainsQ16[k] >> 6;
            int invGainQ31 = SilkMath.inverse32VarQ(gainsQ16[k], 47);
            int gainAdjQ16;
            if (gainsQ16[k] != prevGainQ16) {
                gainAdjQ16 = SilkMath.div32VarQ(prevGainQ16, gainsQ16[k], 16);
                // i 를 0 부터 MAX_LPC_ORDER 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < MAX_LPC_ORDER; i++) {
                    sLpcQ14[i] = SilkMath.smulww(gainAdjQ16, sLpcQ14[i]);
                }
            } else {
                gainAdjQ16 = 1 << 16;
            }
            prevGainQ16 = gainsQ16[k];
            // 직전에 손실이 있었고 유성음에서 무성음으로 바뀌면 약한 LTP 필터를 쓴다.
            if (lossCnt != 0 && prevSignalType == TYPE_VOICED && signalType != TYPE_VOICED && k < MAX_NB_SUBFR / 2) {
                java.util.Arrays.fill(ltpCoefQ14, bOff, bOff + LTP_ORDER, (short) 0);
                ltpCoefQ14[bOff + LTP_ORDER / 2] = (short) (0.25 * (1 << 14) + 0.5);
                sigType = TYPE_VOICED;
                pitchL[k] = lagPrev;
            }
            int lag = 0;
            if (sigType == TYPE_VOICED) {
                lag = pitchL[k];
                if (k == 0 || (k == 2 && nlsfInterpolationFlag)) {
                    int startIdx = ltpMemLength - lag - lpcOrder - LTP_ORDER / 2;
                    if (k == 2) {
                        System.arraycopy(xq, xqOff, outBuf, ltpMemLength, 2 * subfrLength);
                    }
                    lpcAnalysisFilter(sLtp, startIdx, outBuf, startIdx + k * subfrLength, predCoefQ12, aOff, ltpMemLength - startIdx, lpcOrder);
                    if (k == 0) {
                        invGainQ31 = SilkMath.smulwb(invGainQ31, ltpScaleQ14) << 2;
                    }
                    // 잔차를 이득으로 정규화해 LTP 버퍼에 넣는다.
                    for (int i = 0; i < lag + LTP_ORDER / 2; i++) {
                        sLtpQ15[sLtpBufIdx - i - 1] = SilkMath.smulwb(invGainQ31, sLtp[ltpMemLength - i - 1]);
                    }
                } else if (gainAdjQ16 != 1 << 16) {
                    // 이득이 바뀌었으면 LTP 버퍼를 새 이득으로 다시 조정한다.
                    for (int i = 0; i < lag + LTP_ORDER / 2; i++) {
                        sLtpQ15[sLtpBufIdx - i - 1] = SilkMath.smulww(gainAdjQ16, sLtpQ15[sLtpBufIdx - i - 1]);
                    }
                }
            }
            int[] pres;
            int presOff;
            if (sigType == TYPE_VOICED) {
                int predLag = sLtpBufIdx - lag + LTP_ORDER / 2;
                // 샘플마다 장기 예측을 여기 신호에 더한다.
                for (int i = 0; i < subfrLength; i++) {
                    int ltpPredQ13 = 2;
                    ltpPredQ13 = SilkMath.smlawb(ltpPredQ13, sLtpQ15[predLag], ltpCoefQ14[bOff]);
                    ltpPredQ13 = SilkMath.smlawb(ltpPredQ13, sLtpQ15[predLag - 1], ltpCoefQ14[bOff + 1]);
                    ltpPredQ13 = SilkMath.smlawb(ltpPredQ13, sLtpQ15[predLag - 2], ltpCoefQ14[bOff + 2]);
                    ltpPredQ13 = SilkMath.smlawb(ltpPredQ13, sLtpQ15[predLag - 3], ltpCoefQ14[bOff + 3]);
                    ltpPredQ13 = SilkMath.smlawb(ltpPredQ13, sLtpQ15[predLag - 4], ltpCoefQ14[bOff + 4]);
                    predLag++;
                    resQ14[i] = excQ14[pexc + i] + (ltpPredQ13 << 1);
                    sLtpQ15[sLtpBufIdx] = resQ14[i] << 1;
                    sLtpBufIdx++;
                }
                pres = resQ14;
                presOff = 0;
            } else {
                pres = excQ14;
                presOff = pexc;
            }
            // 샘플마다 단기 예측(LPC 합성)을 적용해 출력을 만든다.
            for (int i = 0; i < subfrLength; i++) {
                int lpcPredQ10 = lpcOrder >> 1;
                // j 를 0 부터 lpcOrder 까지 늘려 가며 항목마다 처리한다.
                for (int j = 0; j < lpcOrder; j++) {
                    lpcPredQ10 = SilkMath.smlawb(lpcPredQ10, sLpcQ14[MAX_LPC_ORDER + i - 1 - j], aTmp[j]);
                }
                sLpcQ14[MAX_LPC_ORDER + i] = pres[presOff + i] + (lpcPredQ10 << 4);
                xq[pxq + i] = (short) SilkMath.sat16(SilkMath.rshiftRound(SilkMath.smulww(sLpcQ14[MAX_LPC_ORDER + i], gainQ10), 8));
            }
            System.arraycopy(sLpcQ14, subfrLength, sLpcQ14, 0, MAX_LPC_ORDER);
            pexc += subfrLength;
            pxq += subfrLength;
        }
        System.arraycopy(sLpcQ14, 0, sLpcQ14Buf, 0, MAX_LPC_ORDER);
    }

    /**
     * Sum of squares of 16-bit samples with an adaptive shift that keeps the sum in 32 bits.
     * <p>
     * 합이 32비트에 들도록 적응적으로 shift 하며 16비트 샘플의 제곱합을 구한다.
     *
     * @param x     the samples / 샘플
     * @param xOff  index of the first sample / 첫 샘플의 위치
     * @param lenIn number of samples / 샘플 수
     * @param res   receives the energy (index 0) and the shift (index 1) / 에너지(0)와 shift(1)를 받는 배열
     */
    private static void sumSqrShift(short[] x, int xOff, int lenIn, int[] res) {
        int len = lenIn - 1;
        int nrg = 0;
        int shft = 0;
        int i;
        // 오버플로가 일어나기 전까지 두 샘플씩 제곱을 더한다.
        for (i = 0; i < len; i += 2) {
            nrg = SilkMath.smlabb(nrg, x[xOff + i], x[xOff + i]);
            nrg = SilkMath.smlabb(nrg, x[xOff + i + 1], x[xOff + i + 1]);
            if (nrg < 0) {
                nrg = nrg >>> 2;
                shft = 2;
                break;
            }
        }
        // 오버플로가 나면 shift 를 늘려 가며 나머지를 더한다.
        for (; i < len; i += 2) {
            int nrgTmp = SilkMath.smulbb(x[xOff + i], x[xOff + i]);
            nrgTmp = SilkMath.smlabb(nrgTmp, x[xOff + i + 1], x[xOff + i + 1]);
            nrg = nrg + (nrgTmp >>> shft);
            if (nrg < 0) {
                nrg = nrg >>> 2;
                shft += 2;
            }
        }
        if (i == len) {
            int nrgTmp = SilkMath.smulbb(x[xOff + i], x[xOff + i]);
            nrg = nrg + (nrgTmp >>> shft);
        }
        if ((nrg & 0xC0000000) != 0) {
            nrg = nrg >>> 2;
            shft += 2;
        }
        res[0] = nrg;
        res[1] = shft;
    }

    /**
     * Updates the concealment parameters after a good frame, or conceals a lost frame (<code>silk_PLC</code>).
     * <p>
     * 정상 프레임 뒤에 은닉 매개변수를 갱신하거나 손실된 프레임을 은닉한다(<code>silk_PLC</code>).
     *
     * @param frame    the frame output / 프레임 출력
     * @param frameOff index of the frame / 프레임의 위치
     * @param lost     true to conceal a lost frame / 손실 프레임을 은닉하면 true
     */
    private void plc(short[] frame, int frameOff, boolean lost) {
        if (fsKHz != plcFsKHz) {
            plcReset();
            plcFsKHz = fsKHz;
        }
        if (lost) {
            plcConceal(frame, frameOff);
            lossCnt++;
        } else {
            plcUpdate();
        }
    }

    /**
     * Stores the parameters of a good frame for later concealment.
     * <p>
     * 이후의 은닉을 위해 정상 프레임의 매개변수를 저장한다.
     */
    private void plcUpdate() {
        prevSignalType = signalType;
        int ltpGainQ14 = 0;
        if (signalType == TYPE_VOICED) {
            // 피치 주기를 덮는 서브프레임 중 LTP 이득이 가장 큰 것을 찾는다.
            for (int j = 0; j * subfrLength < pitchL[nbSubfr - 1]; j++) {
                if (j == nbSubfr) {
                    break;
                }
                int tempLtpGainQ14 = 0;
                // i 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < LTP_ORDER; i++) {
                    tempLtpGainQ14 += ltpCoefQ14[(nbSubfr - 1 - j) * LTP_ORDER + i];
                }
                if (tempLtpGainQ14 > ltpGainQ14) {
                    ltpGainQ14 = tempLtpGainQ14;
                    // i 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
                    for (int i = 0; i < LTP_ORDER; i++) {
                        plcLtpCoefQ14[i] = ltpCoefQ14[(nbSubfr - 1 - j) * LTP_ORDER + i];
                    }
                    plcPitchLQ8 = pitchL[nbSubfr - 1 - j] << 8;
                }
            }
            java.util.Arrays.fill(plcLtpCoefQ14, 0);
            plcLtpCoefQ14[LTP_ORDER / 2] = ltpGainQ14;
            if (ltpGainQ14 < 11469) {
                int scaleQ10 = (11469 << 10) / Math.max(ltpGainQ14, 1);
                // i 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < LTP_ORDER; i++) {
                    plcLtpCoefQ14[i] = (short) ((SilkMath.smulbb(plcLtpCoefQ14[i], scaleQ10)) >> 10);
                }
            } else if (ltpGainQ14 > 15565) {
                int scaleQ14 = (15565 << 14) / Math.max(ltpGainQ14, 1);
                // i 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < LTP_ORDER; i++) {
                    plcLtpCoefQ14[i] = (short) ((SilkMath.smulbb(plcLtpCoefQ14[i], scaleQ14)) >> 14);
                }
            }
        } else {
            plcPitchLQ8 = (SilkMath.smulbb(fsKHz, 18)) << 8;
            java.util.Arrays.fill(plcLtpCoefQ14, 0);
        }
        System.arraycopy(predCoefQ12, MAX_LPC_ORDER, plcPrevLpcQ12, 0, lpcOrder);
        plcPrevLtpScaleQ14 = ltpScaleQ14;
        plcPrevGainQ16[0] = gainsQ16[nbSubfr - 2];
        plcPrevGainQ16[1] = gainsQ16[nbSubfr - 1];
        plcSubfrLength = subfrLength;
        plcNbSubfr = nbSubfr;
    }

    /**
     * Synthesizes a lost frame from the excitation and the filters of the last good frame.
     * <p>
     * 마지막 정상 프레임의 여기 신호와 필터로 손실된 프레임을 합성한다.
     *
     * @param frame    receives the frame / 프레임을 받는 배열
     * @param frameOff index of the frame / 프레임의 위치
     */
    private void plcConceal(short[] frame, int frameOff) {
        short[] excBuf = new short[2 * MAX_SUB_FRAME_LENGTH];
        short[] aQ12 = new short[MAX_LPC_ORDER];
        short[] sLtp = new short[MAX_FRAME_LENGTH];
        int[] sLtpQ14 = new int[2 * MAX_FRAME_LENGTH];
        int[] tmp2 = new int[2];
        int[] prevGainQ10 = new int[2];
        prevGainQ10[0] = plcPrevGainQ16[0] >> 6;
        prevGainQ10[1] = plcPrevGainQ16[1] >> 6;
        if (firstFrameAfterReset) {
            java.util.Arrays.fill(plcPrevLpcQ12, (short) 0);
        }
        int excOff = 0;
        // 마지막 두 서브프레임의 여기 신호를 이득을 곱해 16비트로 모은다.
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < plcSubfrLength; i++) {
                excBuf[excOff + i] = (short) SilkMath.sat16(SilkMath.smulww(excQ14[i + (k + plcNbSubfr - 2) * plcSubfrLength], prevGainQ10[k]) >> 8);
            }
            excOff += plcSubfrLength;
        }
        sumSqrShift(excBuf, 0, plcSubfrLength, tmp2);
        int energy1 = tmp2[0];
        int shift1 = tmp2[1];
        sumSqrShift(excBuf, plcSubfrLength, plcSubfrLength, tmp2);
        int energy2 = tmp2[0];
        int shift2 = tmp2[1];
        int randPtr;
        // 둘 중 에너지가 큰 서브프레임 쪽에서 잡음 소스를 잡는다.
        if ((energy1 >> shift2) < (energy2 >> shift1)) {
            randPtr = Math.max(0, (plcNbSubfr - 1) * plcSubfrLength - 128);
        } else {
            randPtr = Math.max(0, plcNbSubfr * plcSubfrLength - 128);
        }
        int[] bQ14 = plcLtpCoefQ14;
        int randScaleQ14 = plcRandScaleQ14;
        int harmGainQ15 = HARM_ATT_Q15[Math.min(1, lossCnt)];
        int randGainQ15;
        if (prevSignalType == TYPE_VOICED) {
            randGainQ15 = PLC_RAND_ATTENUATE_V_Q15[Math.min(1, lossCnt)];
        } else {
            randGainQ15 = PLC_RAND_ATTENUATE_UV_Q15[Math.min(1, lossCnt)];
        }
        SilkMath.bwexpander(plcPrevLpcQ12, 0, lpcOrder, (int) (0.99 * 65536 + 0.5));
        System.arraycopy(plcPrevLpcQ12, 0, aQ12, 0, lpcOrder);
        if (lossCnt == 0) {
            randScaleQ14 = 1 << 14;
            if (prevSignalType == TYPE_VOICED) {
                // i 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < LTP_ORDER; i++) {
                    randScaleQ14 -= bQ14[i];
                }
                randScaleQ14 = Math.max(3277, randScaleQ14);
                randScaleQ14 = (SilkMath.smulbb(randScaleQ14, plcPrevLtpScaleQ14)) >> 14;
            } else {
                int invGainQ30 = SilkNlsf.lpcInversePredGain(plcPrevLpcQ12, 0, lpcOrder);
                int downScaleQ30 = Math.min((1 << 30) >> 3, invGainQ30);
                downScaleQ30 = Math.max((1 << 30) >> 8, downScaleQ30);
                downScaleQ30 = downScaleQ30 << 3;
                randGainQ15 = SilkMath.smulwb(downScaleQ30, randGainQ15) >> 14;
            }
        }
        int randSeedLocal = plcRandSeed;
        int lag = SilkMath.rshiftRound(plcPitchLQ8, 8);
        int sLtpBufIdx = ltpMemLength;
        int idx = ltpMemLength - lag - lpcOrder - LTP_ORDER / 2;
        lpcAnalysisFilter(sLtp, idx, outBuf, idx, aQ12, 0, ltpMemLength - idx, lpcOrder);
        int invGainQ30 = SilkMath.inverse32VarQ(plcPrevGainQ16[1], 46);
        invGainQ30 = Math.min(invGainQ30, Integer.MAX_VALUE >> 1);
        // 백색화한 이력을 이득으로 정규화해 LTP 버퍼에 넣는다.
        for (int i = idx + lpcOrder; i < ltpMemLength; i++) {
            sLtpQ14[i] = SilkMath.smulwb(invGainQ30, sLtp[i]);
        }
        // 서브프레임마다 LTP 예측과 감쇠하는 잡음으로 여기 신호를 만든다.
        for (int k = 0; k < nbSubfr; k++) {
            int predLag = sLtpBufIdx - lag + LTP_ORDER / 2;
            // i 를 0 부터 subfrLength 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < subfrLength; i++) {
                int ltpPredQ12 = 2;
                ltpPredQ12 = SilkMath.smlawb(ltpPredQ12, sLtpQ14[predLag], bQ14[0]);
                ltpPredQ12 = SilkMath.smlawb(ltpPredQ12, sLtpQ14[predLag - 1], bQ14[1]);
                ltpPredQ12 = SilkMath.smlawb(ltpPredQ12, sLtpQ14[predLag - 2], bQ14[2]);
                ltpPredQ12 = SilkMath.smlawb(ltpPredQ12, sLtpQ14[predLag - 3], bQ14[3]);
                ltpPredQ12 = SilkMath.smlawb(ltpPredQ12, sLtpQ14[predLag - 4], bQ14[4]);
                predLag++;
                randSeedLocal = SilkMath.rand(randSeedLocal);
                idx = (randSeedLocal >> 25) & 127;
                sLtpQ14[sLtpBufIdx] = SilkMath.smlawb(ltpPredQ12, excQ14[randPtr + idx], randScaleQ14) << 2;
                sLtpBufIdx++;
            }
            // j 를 0 부터 LTP_ORDER 까지 늘려 가며 항목마다 처리한다.
            for (int j = 0; j < LTP_ORDER; j++) {
                bQ14[j] = (SilkMath.smulbb(harmGainQ15, bQ14[j])) >> 15;
            }
            randScaleQ14 = (SilkMath.smulbb(randScaleQ14, randGainQ15)) >> 15;
            plcPitchLQ8 = SilkMath.smlawb(plcPitchLQ8, plcPitchLQ8, 655);
            plcPitchLQ8 = Math.min(plcPitchLQ8, (SilkMath.smulbb(18, fsKHz)) << 8);
            lag = SilkMath.rshiftRound(plcPitchLQ8, 8);
        }
        int lpcBase = ltpMemLength - MAX_LPC_ORDER;
        System.arraycopy(sLpcQ14Buf, 0, sLtpQ14, lpcBase, MAX_LPC_ORDER);
        // 샘플마다 단기 예측 필터로 합성한다.
        for (int i = 0; i < frameLength; i++) {
            int lpcPredQ10 = lpcOrder >> 1;
            // j 를 0 부터 lpcOrder 까지 늘려 가며 항목마다 처리한다.
            for (int j = 0; j < lpcOrder; j++) {
                lpcPredQ10 = SilkMath.smlawb(lpcPredQ10, sLtpQ14[lpcBase + MAX_LPC_ORDER + i - 1 - j], aQ12[j]);
            }
            sLtpQ14[lpcBase + MAX_LPC_ORDER + i] = sLtpQ14[lpcBase + MAX_LPC_ORDER + i] + (lpcPredQ10 << 4);
            frame[frameOff + i] = (short) SilkMath.sat16(SilkMath.sat16(SilkMath.rshiftRound(SilkMath.smulww(sLtpQ14[lpcBase + MAX_LPC_ORDER + i], prevGainQ10[1]), 8)));
        }
        System.arraycopy(sLtpQ14, lpcBase + frameLength, sLpcQ14Buf, 0, MAX_LPC_ORDER);
        plcRandSeed = randSeedLocal;
        plcRandScaleQ14 = randScaleQ14;
        // 은닉 프레임의 피치 지연을 제어 데이터에 남긴다.
        for (int i = 0; i < MAX_NB_SUBFR; i++) {
            pitchL[i] = lag;
        }
    }

    /**
     * Fades the first good frame after a loss in from the energy of the concealed frame (<code>silk_PLC_glue_frames</code>).
     * <p>
     * 손실 뒤 첫 정상 프레임을 은닉 프레임의 에너지에서 서서히 이어 붙인다(<code>silk_PLC_glue_frames</code>).
     *
     * @param frame    the frame / 프레임
     * @param frameOff index of the frame / 프레임의 위치
     * @param length   frame length / 프레임 길이
     */
    private void plcGlueFrames(short[] frame, int frameOff, int length) {
        int[] res = new int[2];
        if (lossCnt != 0) {
            sumSqrShift(frame, frameOff, length, res);
            plcConcEnergy = res[0];
            plcConcEnergyShift = res[1];
            plcLastFrameLost = true;
        } else {
            if (plcLastFrameLost) {
                sumSqrShift(frame, frameOff, length, res);
                int energy = res[0];
                int energyShift = res[1];
                if (energyShift > plcConcEnergyShift) {
                    plcConcEnergy = plcConcEnergy >> (energyShift - plcConcEnergyShift);
                } else if (energyShift < plcConcEnergyShift) {
                    energy = energy >> (plcConcEnergyShift - energyShift);
                }
                if (energy > plcConcEnergy) {
                    int lz = SilkMath.clz32(plcConcEnergy) - 1;
                    plcConcEnergy = plcConcEnergy << lz;
                    energy = energy >> Math.max(24 - lz, 0);
                    int fracQ24 = plcConcEnergy / Math.max(energy, 1);
                    int gainQ16 = SilkMath.sqrtApprox(fracQ24) << 4;
                    int slopeQ16 = ((1 << 16) - gainQ16) / length;
                    slopeQ16 = slopeQ16 << 2;
                    // 이득을 1 로 키워 가며 프레임을 점점 키운다.
                    for (int i = 0; i < length; i++) {
                        frame[frameOff + i] = (short) SilkMath.smulwb(gainQ16, frame[frameOff + i]);
                        gainQ16 += slopeQ16;
                        if (gainQ16 > 1 << 16) {
                            break;
                        }
                    }
                }
            }
            plcLastFrameLost = false;
        }
    }

    /**
     * Adds comfort noise during frames without voice activity and after losses (<code>silk_CNG</code>).
     * <p>
     * 음성 활동이 없는 프레임과 손실 뒤에 편안한 잡음을 더한다(<code>silk_CNG</code>).
     *
     * @param frame    the frame / 프레임
     * @param frameOff index of the frame / 프레임의 위치
     * @param length   frame length / 프레임 길이
     */
    private void cng(short[] frame, int frameOff, int length) {
        short[] aQ12 = new short[MAX_LPC_ORDER];
        int[] cngSigQ10 = new int[MAX_FRAME_LENGTH + MAX_LPC_ORDER];
        if (fsKHz != cngFsKHz) {
            cngReset();
            cngFsKHz = fsKHz;
        }
        if (lossCnt == 0 && prevSignalType == TYPE_NO_VOICE_ACTIVITY) {
            // 음성 활동이 없는 정상 프레임에서 잡음의 스펙트럼과 이득과 여기 신호를 학습한다.
            for (int i = 0; i < lpcOrder; i++) {
                cngSmthNlsfQ15[i] += SilkMath.smulwb(prevNlsfQ15[i] - cngSmthNlsfQ15[i], 16348);
            }
            int maxGainQ16 = 0;
            int subfr = 0;
            // i 를 0 부터 nbSubfr 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < nbSubfr; i++) {
                if (gainsQ16[i] > maxGainQ16) {
                    maxGainQ16 = gainsQ16[i];
                    subfr = i;
                }
            }
            System.arraycopy(cngExcBufQ14, 0, cngExcBufQ14, subfrLength, (nbSubfr - 1) * subfrLength);
            System.arraycopy(excQ14, subfr * subfrLength, cngExcBufQ14, 0, subfrLength);
            // i 를 0 부터 nbSubfr 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < nbSubfr; i++) {
                cngSmthGainQ16 += SilkMath.smulwb(gainsQ16[i] - cngSmthGainQ16, 4634);
            }
        }
        if (lossCnt != 0) {
            int excMask = 255;
            // 마스크를 프레임 길이 이하의 2^n - 1 로 줄인다.
            while (excMask > length) {
                excMask >>= 1;
            }
            int seedLocal = cngRandSeed;
            // 저장된 여기 신호에서 무작위로 골라 잡음 여기 신호를 만든다.
            for (int i = 0; i < length; i++) {
                seedLocal = SilkMath.rand(seedLocal);
                int idx = (seedLocal >> 24) & excMask;
                cngSigQ10[MAX_LPC_ORDER + i] = (short) SilkMath.sat16(SilkMath.smulww(cngExcBufQ14[idx], cngSmthGainQ16 >> 4));
            }
            cngRandSeed = seedLocal;
            SilkNlsf.nlsf2a(aQ12, 0, cngSmthNlsfQ15, lpcOrder);
            System.arraycopy(cngSynthState, 0, cngSigQ10, 0, MAX_LPC_ORDER);
            // 샘플마다 합성 필터를 거쳐 프레임에 더한다.
            for (int i = 0; i < length; i++) {
                int sumQ6 = lpcOrder >> 1;
                // j 를 0 부터 lpcOrder 까지 늘려 가며 항목마다 처리한다.
                for (int j = 0; j < lpcOrder; j++) {
                    sumQ6 = SilkMath.smlawb(sumQ6, cngSigQ10[MAX_LPC_ORDER + i - 1 - j], aQ12[j]);
                }
                cngSigQ10[MAX_LPC_ORDER + i] = cngSigQ10[MAX_LPC_ORDER + i] + (sumQ6 << 4);
                frame[frameOff + i] = (short) SilkMath.sat16(frame[frameOff + i] + SilkMath.rshiftRound(sumQ6, 6));
            }
            System.arraycopy(cngSigQ10, length, cngSynthState, 0, MAX_LPC_ORDER);
        } else {
            java.util.Arrays.fill(cngSynthState, 0, lpcOrder, 0);
        }
    }

    /**
     * Decodes one frame of this channel (<code>silk_decode_frame</code>).
     * <p>
     * 이 채널의 프레임 하나를 디코딩한다(<code>silk_decode_frame</code>).
     *
     * @param rc         the range decoder / 범위 디코더
     * @param pOut       receives the output samples / 출력 샘플을 받는 배열
     * @param pOutOff    index of the first output sample / 첫 출력 샘플의 위치
     * @param lostFlag   frame decoding mode / 프레임 디코딩 모드
     * @param condCoding the coding mode of the frame / 프레임의 부호화 모드
     * @return the number of samples of the frame / 프레임의 샘플 수
     */
    int decodeFrame(OpusRangeDecoder rc, short[] pOut, int pOutOff, int lostFlag, int condCoding) {
        int[] pulses = new int[MAX_FRAME_LENGTH];
        int l = frameLength;
        ltpScaleQ14 = 0;
        if (lostFlag == FLAG_DECODE_NORMAL || (lostFlag == FLAG_DECODE_LBRR && lbrrFlags[nFramesDecoded] == 1)) {
            decodeIndices(rc, nFramesDecoded, lostFlag == FLAG_DECODE_LBRR, condCoding);
            decodePulses(rc, pulses, signalType, quantOffsetType, frameLength);
            decodeParameters(condCoding);
            l = frameLength;
            decodeCore(pOut, pOutOff, pulses);
            plc(pOut, pOutOff, false);
            lossCnt = 0;
            prevSignalType = signalType;
            firstFrameAfterReset = false;
        } else {
            plc(pOut, pOutOff, true);
        }
        int mvLen = ltpMemLength - frameLength;
        System.arraycopy(outBuf, frameLength, outBuf, 0, mvLen);
        System.arraycopy(pOut, pOutOff, outBuf, mvLen, frameLength);
        plcGlueFrames(pOut, pOutOff, l);
        cng(pOut, pOutOff, l);
        lagPrev = pitchL[nbSubfr - 1];
        return l;
    }
}
