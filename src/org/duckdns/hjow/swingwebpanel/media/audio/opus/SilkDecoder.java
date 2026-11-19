package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The SILK decoder of Opus (RFC 6716 section 4.2): it decodes the frames of one packet for one or two channels, handles the
 * redundant low-bitrate frames, the mid/side stereo prediction and the conversion to the output sampling rate
 * (<code>silk/dec_API.c</code>, <code>stereo_decode_pred.c</code>, <code>stereo_MS_to_LR.c</code>). The output is 16-bit PCM.
 * Integer arithmetic exactly as in the reference decoder. Not thread safe.
 * <p>
 * Opus 의 SILK 디코더(RFC 6716 4.2): 패킷 하나의 프레임을 채널 하나 또는 둘에 대해 디코딩하고, 중복 저비트율 프레임, 미드/사이드 스테레오 예측,
 * 출력 샘플링 주파수로의 변환을 처리한다(<code>silk/dec_API.c</code>, <code>stereo_decode_pred.c</code>, <code>stereo_MS_to_LR.c</code>).
 * 출력은 16비트 PCM 이다. 참조 디코더와 똑같은 정수 연산을 쓴다. 스레드에 안전하지 않다.
 */
final class SilkDecoder {

    /**
     * Largest frame length of one channel at the internal rate (20 ms at 16 kHz) plus two samples of history.
     * <p>
     * 내부 주파수에서 채널 하나의 최대 프레임 길이(16 kHz 20 ms)에 이력 2 샘플을 더한 값.
     */
    private static final int TMP_LENGTH = 16 * 20 + 2;

    /**
     * Duration of the stereo prediction interpolation in milliseconds.
     * <p>
     * 스테레오 예측 보간 구간의 길이(밀리초).
     */
    private static final int STEREO_INTERP_LEN_MS = 8;

    /**
     * The channels (mid or left, and side or right).
     * <p>
     * 채널(미드 또는 왼쪽, 사이드 또는 오른쪽).
     */
    private final SilkChannel[] channels = { new SilkChannel(), new SilkChannel() };

    /**
     * Stereo prediction weights of the previous frame (Q13).
     * <p>
     * 이전 프레임의 스테레오 예측 가중치(Q13).
     */
    private final int[] predPrevQ13 = new int[2];

    /**
     * Last two mid samples of the previous frame.
     * <p>
     * 이전 프레임의 마지막 미드 샘플 둘.
     */
    private final short[] sMid = new short[2];

    /**
     * Last two side samples of the previous frame.
     * <p>
     * 이전 프레임의 마지막 사이드 샘플 둘.
     */
    private final short[] sSide = new short[2];

    /**
     * Number of output channels of the previous call.
     * <p>
     * 이전 호출의 출력 채널 수.
     */
    private int nChannelsApi;

    /**
     * Number of coded channels of the previous call.
     * <p>
     * 이전 호출의 부호화된 채널 수.
     */
    private int nChannelsInternal;

    /**
     * Whether the side channel was absent in the previous frame.
     * <p>
     * 이전 프레임에서 사이드 채널이 없었는지.
     */
    private boolean prevDecodeOnlyMiddle;

    /**
     * Parameters of a decoding call (the equivalent of <code>silk_DecControlStruct</code>).
     * <p>
     * 디코딩 호출의 매개변수(<code>silk_DecControlStruct</code> 에 해당).
     */
    static final class Control {

        /**
         * Number of output channels.
         * <p>
         * 출력 채널 수.
         */
        int nChannelsApi;

        /**
         * Number of coded channels.
         * <p>
         * 부호화된 채널 수.
         */
        int nChannelsInternal;

        /**
         * Output sampling rate in Hz.
         * <p>
         * 출력 샘플링 주파수(Hz).
         */
        int apiSampleRate;

        /**
         * Internal sampling rate in Hz (8000, 12000 or 16000).
         * <p>
         * 내부 샘플링 주파수(Hz, 8000, 12000, 16000).
         */
        int internalSampleRate;

        /**
         * Duration of the packet payload in ms (10, 20, 40 or 60).
         * <p>
         * 패킷 페이로드의 길이(ms, 10, 20, 40, 60).
         */
        int payloadSizeMs;

        /**
         * Pitch lag of the last frame in samples at the output rate, 0 if unvoiced.
         * <p>
         * 출력 주파수 샘플 단위로 마지막 프레임의 피치 지연. 무성음이면 0.
         */
        int prevPitchLag;
    }

    /**
     * Creates a decoder in the initial state.
     * <p>
     * 초기 상태의 디코더를 만든다.
     */
    SilkDecoder() {
        init();
    }

    /**
     * Resets the decoder (<code>silk_InitDecoder</code>).
     * <p>
     * 디코더를 리셋한다(<code>silk_InitDecoder</code>).
     */
    void init() {
        channels[0].init();
        channels[1].init();
        java.util.Arrays.fill(predPrevQ13, 0);
        java.util.Arrays.fill(sMid, (short) 0);
        java.util.Arrays.fill(sSide, (short) 0);
        nChannelsApi = 0;
        nChannelsInternal = 0;
        prevDecodeOnlyMiddle = false;
    }

    /**
     * Decodes the stereo prediction weights.
     * <p>
     * 스테레오 예측 가중치를 디코딩한다.
     *
     * @param rc     the range decoder / 범위 디코더
     * @param predQ13 receives the two weights in Q13 / Q13 가중치 둘을 받는 배열
     */
    private static void stereoDecodePred(OpusRangeDecoder rc, int[] predQ13) {
        int[][] ix = new int[2][3];
        int n = rc.icdf(SilkTables.STEREO_PRED_JOINT_ICDF, 8);
        ix[0][2] = n / 5;
        ix[1][2] = n - 5 * ix[0][2];
        // 두 가중치마다 세부 번호를 읽는다.
        for (n = 0; n < 2; n++) {
            ix[n][0] = rc.icdf(SilkTables.UNIFORM3_ICDF, 8);
            ix[n][1] = rc.icdf(SilkTables.UNIFORM5_ICDF, 8);
        }
        // 번호를 양자화 수준 사이의 값으로 바꾼다.
        for (n = 0; n < 2; n++) {
            ix[n][0] += 3 * ix[n][2];
            int lowQ13 = SilkTables.STEREO_PRED_QUANT_Q13[ix[n][0]];
            int stepQ13 = SilkMath.smulwb(SilkTables.STEREO_PRED_QUANT_Q13[ix[n][0] + 1] - lowQ13, (int) (0.5 / 5 * 65536 + 0.5));
            predQ13[n] = SilkMath.smlabb(lowQ13, stepQ13, 2 * ix[n][1] + 1);
        }
        predQ13[0] -= predQ13[1];
    }

    /**
     * Converts a decoded mid/side pair to left/right (<code>silk_stereo_MS_to_LR</code>).
     * <p>
     * 디코딩한 미드/사이드 쌍을 좌/우로 바꾼다(<code>silk_stereo_MS_to_LR</code>).
     *
     * @param x1          mid signal, two samples of history in front; becomes left / 미드 신호(앞에 이력 2 샘플). 왼쪽이 된다
     * @param x2          side signal, two samples of history in front; becomes right / 사이드 신호(앞에 이력 2 샘플). 오른쪽이 된다
     * @param predQ13     prediction weights of this frame / 이 프레임의 예측 가중치
     * @param fsKHz       internal rate in kHz / 내부 주파수(kHz)
     * @param frameLength number of samples / 샘플 수
     */
    private void stereoMsToLr(short[] x1, short[] x2, int[] predQ13, int fsKHz, int frameLength) {
        System.arraycopy(sMid, 0, x1, 0, 2);
        System.arraycopy(sSide, 0, x2, 0, 2);
        System.arraycopy(x1, frameLength, sMid, 0, 2);
        System.arraycopy(x2, frameLength, sSide, 0, 2);
        int pred0Q13 = predPrevQ13[0];
        int pred1Q13 = predPrevQ13[1];
        int denomQ16 = (1 << 16) / (STEREO_INTERP_LEN_MS * fsKHz);
        int delta0Q13 = SilkMath.rshiftRound(SilkMath.smulbb(predQ13[0] - predPrevQ13[0], denomQ16), 16);
        int delta1Q13 = SilkMath.rshiftRound(SilkMath.smulbb(predQ13[1] - predPrevQ13[1], denomQ16), 16);
        int interp = STEREO_INTERP_LEN_MS * fsKHz;
        // 처음 8 ms 동안은 예측 가중치를 이전 값에서 새 값으로 보간한다.
        for (int n = 0; n < interp; n++) {
            pred0Q13 += delta0Q13;
            pred1Q13 += delta1Q13;
            int sum = ((x1[n] + x1[n + 2]) + (x1[n + 1] << 1)) << 9;
            sum = SilkMath.smlawb(x2[n + 1] << 8, sum, pred0Q13);
            sum = SilkMath.smlawb(sum, x1[n + 1] << 11, pred1Q13);
            x2[n + 1] = (short) SilkMath.sat16(SilkMath.rshiftRound(sum, 8));
        }
        pred0Q13 = predQ13[0];
        pred1Q13 = predQ13[1];
        // 나머지 구간은 새 가중치를 그대로 쓴다.
        for (int n = interp; n < frameLength; n++) {
            int sum = ((x1[n] + x1[n + 2]) + (x1[n + 1] << 1)) << 9;
            sum = SilkMath.smlawb(x2[n + 1] << 8, sum, pred0Q13);
            sum = SilkMath.smlawb(sum, x1[n + 1] << 11, pred1Q13);
            x2[n + 1] = (short) SilkMath.sat16(SilkMath.rshiftRound(sum, 8));
        }
        predPrevQ13[0] = predQ13[0];
        predPrevQ13[1] = predQ13[1];
        // 미드와 사이드의 합과 차로 왼쪽과 오른쪽을 만든다.
        for (int n = 0; n < frameLength; n++) {
            int sum = x1[n + 1] + x2[n + 1];
            int diff = x1[n + 1] - x2[n + 1];
            x1[n + 1] = (short) SilkMath.sat16(sum);
            x2[n + 1] = (short) SilkMath.sat16(diff);
        }
    }

    /**
     * Decodes (or conceals) one frame of the packet for all channels (<code>silk_Decode</code>).
     * <p>
     * 패킷의 프레임 하나를 모든 채널에 대해 디코딩(또는 은닉)한다(<code>silk_Decode</code>).
     *
     * @param ctl          the parameters of the call / 호출의 매개변수
     * @param lostFlag     0 normal, 1 packet lost, 2 decode the redundant frames / 0 정상, 1 패킷 손실, 2 중복 프레임 디코딩
     * @param newPacket    true for the first call of a packet / 패킷의 첫 호출이면 true
     * @param rc           the range decoder positioned at the SILK data / SILK 데이터 위치의 범위 디코더
     * @param samplesOut   receives the interleaved output samples / 인터리브된 출력 샘플을 받는 배열
     * @param outOff       index of the first output sample / 첫 출력 샘플의 위치
     * @param nSamplesOut  receives the number of samples per channel at index 0 / 인덱스 0 에 채널당 샘플 수를 받는 배열
     * @return 0 on success, negative on an invalid configuration / 성공이면 0, 설정이 올바르지 않으면 음수
     */
    int decode(Control ctl, int lostFlag, boolean newPacket, OpusRangeDecoder rc, short[] samplesOut, int outOff, int[] nSamplesOut) {
        int ret = 0;
        int decodeOnlyMiddle = 0;
        int[] msPredQ13 = new int[2];
        short[][] samplesOut1 = new short[2][TMP_LENGTH];
        int[] nSamplesOutDec = new int[1];
        SilkChannel c0 = channels[0];
        SilkChannel c1 = channels[1];
        if (newPacket) {
            // 새 패킷이면 채널마다 디코딩한 프레임 수를 0 으로 센다.
            for (int n = 0; n < ctl.nChannelsInternal; n++) {
                channels[n].nFramesDecoded = 0;
            }
        }
        if (ctl.nChannelsInternal > nChannelsInternal) {
            c1.init();
        }
        boolean stereoToMono = ctl.nChannelsInternal == 1 && nChannelsInternal == 2 && (ctl.internalSampleRate == 1000 * c0.fsKHz);
        if (c0.nFramesDecoded == 0) {
            // 패킷의 첫 프레임이면 프레임 크기와 내부 주파수를 정한다.
            for (int n = 0; n < ctl.nChannelsInternal; n++) {
                SilkChannel c = channels[n];
                if (ctl.payloadSizeMs == 0 || ctl.payloadSizeMs == 10) {
                    c.nFramesPerPacket = 1;
                    c.nbSubfr = 2;
                } else if (ctl.payloadSizeMs == 20) {
                    c.nFramesPerPacket = 1;
                    c.nbSubfr = 4;
                } else if (ctl.payloadSizeMs == 40) {
                    c.nFramesPerPacket = 2;
                    c.nbSubfr = 4;
                } else if (ctl.payloadSizeMs == 60) {
                    c.nFramesPerPacket = 3;
                    c.nbSubfr = 4;
                } else {
                    return -1;
                }
                int fsKHzDec = (ctl.internalSampleRate >> 10) + 1;
                if (fsKHzDec != 8 && fsKHzDec != 12 && fsKHzDec != 16) {
                    return -2;
                }
                ret += c.setFs(fsKHzDec, ctl.apiSampleRate);
            }
        }
        if (ctl.nChannelsApi == 2 && ctl.nChannelsInternal == 2 && (nChannelsApi == 1 || nChannelsInternal == 1)) {
            java.util.Arrays.fill(predPrevQ13, 0);
            java.util.Arrays.fill(sSide, (short) 0);
            c1.resampler.copyFrom(c0.resampler);
        }
        nChannelsApi = ctl.nChannelsApi;
        nChannelsInternal = ctl.nChannelsInternal;
        if (ctl.apiSampleRate > 48000 || ctl.apiSampleRate < 8000) {
            return -2;
        }
        if (lostFlag != SilkChannel.FLAG_PACKET_LOST && c0.nFramesDecoded == 0) {
            // 패킷의 처음에서 음성 활동 플래그와 중복 프레임 플래그를 읽는다.
            for (int n = 0; n < ctl.nChannelsInternal; n++) {
                SilkChannel c = channels[n];
                // i 를 0 부터 c.nFramesPerPacket 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < c.nFramesPerPacket; i++) {
                    c.vadFlags[i] = rc.bitLogp(1);
                }
                c.lbrrFlag = rc.bitLogp(1);
            }
            // n 를 0 부터 ctl.nChannelsInternal 까지 늘려 가며 항목마다 처리한다.
            for (int n = 0; n < ctl.nChannelsInternal; n++) {
                SilkChannel c = channels[n];
                java.util.Arrays.fill(c.lbrrFlags, 0);
                if (c.lbrrFlag != 0) {
                    if (c.nFramesPerPacket == 1) {
                        c.lbrrFlags[0] = 1;
                    } else {
                        int[] tbl = c.nFramesPerPacket == 2 ? SilkTables.LBRR_FLAGS_2_ICDF : SilkTables.LBRR_FLAGS_3_ICDF;
                        int lbrrSymbol = rc.icdf(tbl, 8) + 1;
                        // i 를 0 부터 c.nFramesPerPacket 까지 늘려 가며 항목마다 처리한다.
                        for (int i = 0; i < c.nFramesPerPacket; i++) {
                            c.lbrrFlags[i] = (lbrrSymbol >> i) & 1;
                        }
                    }
                }
            }
            if (lostFlag == SilkChannel.FLAG_DECODE_NORMAL) {
                int[] pulses = new int[SilkChannel.MAX_FRAME_LENGTH];
                // 중복 프레임은 건너뛰기 위해 읽기만 한다.
                for (int i = 0; i < c0.nFramesPerPacket; i++) {
                    for (int n = 0; n < ctl.nChannelsInternal; n++) {
                        SilkChannel c = channels[n];
                        if (c.lbrrFlags[i] != 0) {
                            if (ctl.nChannelsInternal == 2 && n == 0) {
                                stereoDecodePred(rc, msPredQ13);
                                if (c1.lbrrFlags[i] == 0) {
                                    rc.icdf(SilkTables.STEREO_ONLY_CODE_MID_ICDF, 8);
                                }
                            }
                            int condCoding = i > 0 && c.lbrrFlags[i - 1] != 0 ? SilkChannel.CODE_CONDITIONALLY : SilkChannel.CODE_INDEPENDENTLY;
                            c.decodeIndices(rc, i, true, condCoding);
                            SilkChannel.decodePulses(rc, pulses, c.signalType, c.quantOffsetType, c.frameLength);
                        }
                    }
                }
            }
        }
        if (ctl.nChannelsInternal == 2) {
            if (lostFlag == SilkChannel.FLAG_DECODE_NORMAL
                    || (lostFlag == SilkChannel.FLAG_DECODE_LBRR && c0.lbrrFlags[c0.nFramesDecoded] == 1)) {
                stereoDecodePred(rc, msPredQ13);
                if ((lostFlag == SilkChannel.FLAG_DECODE_NORMAL && c1.vadFlags[c0.nFramesDecoded] == 0)
                        || (lostFlag == SilkChannel.FLAG_DECODE_LBRR && c1.lbrrFlags[c0.nFramesDecoded] == 0)) {
                    decodeOnlyMiddle = rc.icdf(SilkTables.STEREO_ONLY_CODE_MID_ICDF, 8);
                } else {
                    decodeOnlyMiddle = 0;
                }
            } else {
                msPredQ13[0] = predPrevQ13[0];
                msPredQ13[1] = predPrevQ13[1];
            }
        }
        if (ctl.nChannelsInternal == 2 && decodeOnlyMiddle == 0 && prevDecodeOnlyMiddle) {
            java.util.Arrays.fill(c1.outBuf, (short) 0);
            java.util.Arrays.fill(c1.sLpcQ14Buf, 0);
            c1.lagPrev = 100;
            c1.lastGainIndex = 10;
            c1.prevSignalType = SilkChannel.TYPE_NO_VOICE_ACTIVITY;
            c1.firstFrameAfterReset = true;
        }
        boolean hasSide;
        if (lostFlag == SilkChannel.FLAG_DECODE_NORMAL) {
            hasSide = decodeOnlyMiddle == 0;
        } else {
            hasSide = !prevDecodeOnlyMiddle
                    || (ctl.nChannelsInternal == 2 && lostFlag == SilkChannel.FLAG_DECODE_LBRR && c1.lbrrFlags[c1.nFramesDecoded] == 1);
        }
        // 채널마다 프레임을 디코딩한다(사이드 채널이 없으면 0 으로 채운다).
        for (int n = 0; n < ctl.nChannelsInternal; n++) {
            SilkChannel c = channels[n];
            if (n == 0 || hasSide) {
                int frameIndex = c0.nFramesDecoded - n;
                int condCoding;
                if (frameIndex <= 0) {
                    condCoding = SilkChannel.CODE_INDEPENDENTLY;
                } else if (lostFlag == SilkChannel.FLAG_DECODE_LBRR) {
                    condCoding = c.lbrrFlags[frameIndex - 1] != 0 ? SilkChannel.CODE_CONDITIONALLY : SilkChannel.CODE_INDEPENDENTLY;
                } else if (n > 0 && prevDecodeOnlyMiddle) {
                    condCoding = SilkChannel.CODE_INDEPENDENTLY_NO_LTP_SCALING;
                } else {
                    condCoding = SilkChannel.CODE_CONDITIONALLY;
                }
                ret += c.decodeFrame(rc, samplesOut1[n], 2, lostFlag, condCoding) < 0 ? -1 : 0;
                nSamplesOutDec[0] = c.frameLength;
            } else {
                java.util.Arrays.fill(samplesOut1[n], 2, 2 + nSamplesOutDec[0], (short) 0);
            }
            c.nFramesDecoded++;
        }
        int nDec = nSamplesOutDec[0];
        if (ctl.nChannelsApi == 2 && ctl.nChannelsInternal == 2) {
            stereoMsToLr(samplesOut1[0], samplesOut1[1], msPredQ13, c0.fsKHz, nDec);
        } else {
            System.arraycopy(sMid, 0, samplesOut1[0], 0, 2);
            System.arraycopy(samplesOut1[0], nDec, sMid, 0, 2);
        }
        nSamplesOut[0] = (nDec * ctl.apiSampleRate) / (c0.fsKHz * 1000);
        short[] resampleOut;
        int resampleOff;
        if (ctl.nChannelsApi == 2) {
            resampleOut = new short[48 * 20];
            resampleOff = 0;
        } else {
            resampleOut = samplesOut;
            resampleOff = outOff;
        }
        // 채널마다 출력 주파수로 리샘플링하고 인터리브한다.
        for (int n = 0; n < Math.min(ctl.nChannelsApi, ctl.nChannelsInternal); n++) {
            channels[n].resampler.resample(resampleOut, resampleOff, samplesOut1[n], 1, nDec);
            if (ctl.nChannelsApi == 2) {
                // i 를 0 부터 nSamplesOut[0] 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < nSamplesOut[0]; i++) {
                    samplesOut[outOff + n + 2 * i] = resampleOut[i];
                }
            }
        }
        if (ctl.nChannelsApi == 2 && ctl.nChannelsInternal == 1) {
            if (stereoToMono) {
                c1.resampler.resample(resampleOut, resampleOff, samplesOut1[0], 1, nDec);
                // i 를 0 부터 nSamplesOut[0] 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < nSamplesOut[0]; i++) {
                    samplesOut[outOff + 1 + 2 * i] = resampleOut[i];
                }
            } else {
                // i 를 0 부터 nSamplesOut[0] 까지 늘려 가며 항목마다 처리한다.
                for (int i = 0; i < nSamplesOut[0]; i++) {
                    samplesOut[outOff + 1 + 2 * i] = samplesOut[outOff + 2 * i];
                }
            }
        }
        if (c0.prevSignalType == SilkChannel.TYPE_VOICED) {
            int[] multTab = { 6, 4, 3 };
            ctl.prevPitchLag = c0.lagPrev * multTab[(c0.fsKHz - 8) >> 2];
        } else {
            ctl.prevPitchLag = 0;
        }
        if (lostFlag == SilkChannel.FLAG_PACKET_LOST) {
            // 손실 뒤에는 마지막 이득 번호를 초기값으로 되돌린다.
            for (int i = 0; i < nChannelsInternal; i++) {
                channels[i].lastGainIndex = 10;
            }
        } else {
            prevDecodeOnlyMiddle = decodeOnlyMiddle != 0;
        }
        return ret;
    }
}
