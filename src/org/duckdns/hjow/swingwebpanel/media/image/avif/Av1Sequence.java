package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The sequence header of an AV1 stream (plan item P18-03; AV1 specification section 5.5): the profile, the maximum frame size, the coding
 * tools that the frames may use and the colour configuration. {@link #parse} reads the syntax of the specification (including the timing
 * and decoder model parts, which are only needed to read the following frame headers correctly).
 * <p>
 * AV1 스트림의 시퀀스 헤더(계획 P18-03; AV1 규격 5.5절): 프로파일, 최대 프레임 크기, 프레임이 쓸 수 있는 부호화 도구, 색 구성. {@link #parse} 는
 * 규격의 구문을 읽는다(이어지는 프레임 헤더를 올바르게 읽는 데에만 필요한 타이밍과 디코더 모델 부분도 포함한다).
 */
final class Av1Sequence {

    /**
     * Value of seq_force_screen_content_tools / seq_force_integer_mv that means "chosen per frame".
     * <p>
     * 프레임마다 선택함을 뜻하는 seq_force_screen_content_tools / seq_force_integer_mv 의 값.
     */
    static final int SELECT = 2;

    /**
     * Profile: 0 main (4:2:0 and monochrome), 1 high (4:4:4), 2 professional (4:2:2 and 12 bit).
     * <p>
     * 프로파일: 0 main(4:2:0 과 흑백), 1 high(4:4:4), 2 professional(4:2:2 와 12비트).
     */
    int profile;

    /**
     * True if the stream is a still picture.
     * <p>
     * 스트림이 정지 영상이면 true.
     */
    boolean stillPicture;

    /**
     * True if the frame header is the reduced header of a still picture.
     * <p>
     * 프레임 헤더가 정지 영상의 간략 헤더이면 true.
     */
    boolean reducedStillPictureHeader;

    /**
     * True if timing information is present.
     * <p>
     * 타이밍 정보가 있으면 true.
     */
    boolean timingInfoPresent;

    /**
     * True if the pictures have equal intervals.
     * <p>
     * 화면 간격이 같으면 true.
     */
    boolean equalPictureInterval;

    /**
     * True if the decoder model information is present.
     * <p>
     * 디코더 모델 정보가 있으면 true.
     */
    boolean decoderModelInfoPresent;

    /**
     * Length of buffer removal times minus one.
     * <p>
     * 버퍼 제거 시각 길이 - 1.
     */
    int bufferRemovalTimeLengthMinus1;

    /**
     * Length of frame presentation times minus one.
     * <p>
     * 프레임 표시 시각 길이 - 1.
     */
    int framePresentationTimeLengthMinus1;

    /**
     * Number of operating points minus one.
     * <p>
     * 동작 지점 수 - 1.
     */
    int operatingPointsCntMinus1;

    /**
     * The idc of each operating point.
     * <p>
     * 동작 지점별 idc.
     */
    int[] operatingPointIdc = new int[32];

    /**
     * Whether the decoder model parameters exist for each operating point.
     * <p>
     * 동작 지점별 디코더 모델 매개변수 유무.
     */
    boolean[] decoderModelPresentForThisOp = new boolean[32];

    /**
     * Number of bits of the frame width field minus one.
     * <p>
     * 프레임 너비 필드의 비트 수 - 1.
     */
    int frameWidthBitsMinus1;

    /**
     * Number of bits of the frame height field minus one.
     * <p>
     * 프레임 높이 필드의 비트 수 - 1.
     */
    int frameHeightBitsMinus1;

    /**
     * Maximum frame width minus one.
     * <p>
     * 최대 프레임 너비 - 1.
     */
    int maxFrameWidthMinus1;

    /**
     * Maximum frame height minus one.
     * <p>
     * 최대 프레임 높이 - 1.
     */
    int maxFrameHeightMinus1;

    /**
     * True if frame ids are signalled.
     * <p>
     * 프레임 id 를 보내면 true.
     */
    boolean frameIdNumbersPresent;

    /**
     * Length of the frame id delta minus two.
     * <p>
     * 프레임 id 차 길이 - 2.
     */
    int deltaFrameIdLengthMinus2;

    /**
     * Additional frame id length minus one.
     * <p>
     * 추가 프레임 id 길이 - 1.
     */
    int additionalFrameIdLengthMinus1;

    /**
     * True for 128x128 superblocks, false for 64x64.
     * <p>
     * 128x128 슈퍼블록이면 true, 64x64 이면 false.
     */
    boolean use128x128Superblock;

    /**
     * True if filter intra prediction may be used.
     * <p>
     * 필터 인트라 예측을 쓸 수 있으면 true.
     */
    boolean enableFilterIntra;

    /**
     * True if the intra edge filter may be used.
     * <p>
     * 인트라 가장자리 필터를 쓸 수 있으면 true.
     */
    boolean enableIntraEdgeFilter;

    /**
     * True if inter frames may predict a block from a mix of an inter and an intra prediction (interintra).
     * <p>
     * 인터 프레임이 인터 예측과 인트라 예측을 섞어 블록을 예측할 수 있으면 true(인터인트라).
     */
    boolean enableInterintraCompound;

    /**
     * True if compound prediction may use masks (wedge and difference weighted).
     * <p>
     * 복합 예측이 마스크(쐐기, 차이 가중)를 쓸 수 있으면 true.
     */
    boolean enableMaskedCompound;

    /**
     * True if local warped motion may be used.
     * <p>
     * 국소 워프 움직임을 쓸 수 있으면 true.
     */
    boolean enableWarpedMotion;

    /**
     * True if the two directions of the interpolation filter may differ.
     * <p>
     * 보간 필터의 두 방향이 서로 다를 수 있으면 true.
     */
    boolean enableDualFilter;

    /**
     * True if compound prediction may weight the two predictions by the distance of their frames.
     * <p>
     * 복합 예측이 두 예측을 프레임 거리로 가중할 수 있으면 true.
     */
    boolean enableJntComp;

    /**
     * True if the motion vectors of reference frames may be projected into the current frame.
     * <p>
     * 참조 프레임의 움직임 벡터를 현재 프레임으로 투영해 쓸 수 있으면 true.
     */
    boolean enableRefFrameMvs;

    /**
     * True if order hints are used.
     * <p>
     * 순서 힌트를 쓰면 true.
     */
    boolean enableOrderHint;

    /**
     * The number of bits of an order hint.
     * <p>
     * 순서 힌트의 비트 수.
     */
    int orderHintBits;

    /**
     * Screen content tools: 0 off, 1 on, {@link #SELECT} per frame.
     * <p>
     * 화면 콘텐츠 도구: 0 끔, 1 켬, {@link #SELECT} 는 프레임마다.
     */
    int forceScreenContentTools = SELECT;

    /**
     * Integer motion vectors: 0 off, 1 on, {@link #SELECT} per frame.
     * <p>
     * 정수 움직임 벡터: 0 끔, 1 켬, {@link #SELECT} 는 프레임마다.
     */
    int forceIntegerMv = SELECT;

    /**
     * True if super resolution may be used.
     * <p>
     * 슈퍼 해상도를 쓸 수 있으면 true.
     */
    boolean enableSuperres;

    /**
     * True if the constrained directional enhancement filter may be used.
     * <p>
     * CDEF 를 쓸 수 있으면 true.
     */
    boolean enableCdef;

    /**
     * True if loop restoration may be used.
     * <p>
     * 루프 복원을 쓸 수 있으면 true.
     */
    boolean enableRestoration;

    /**
     * Bit depth of the samples: 8, 10 or 12.
     * <p>
     * 샘플의 비트 깊이: 8, 10, 12.
     */
    int bitDepth = 8;

    /**
     * True for a monochrome stream.
     * <p>
     * 흑백 스트림이면 true.
     */
    boolean monoChrome;

    /**
     * Number of planes: 1 or 3.
     * <p>
     * 평면 수: 1 또는 3.
     */
    int numPlanes = 3;

    /**
     * CICP colour primaries.
     * <p>
     * CICP 색 원색.
     */
    int colorPrimaries = 2;

    /**
     * CICP transfer characteristics.
     * <p>
     * CICP 전달 특성.
     */
    int transferCharacteristics = 2;

    /**
     * CICP matrix coefficients.
     * <p>
     * CICP 행렬 계수.
     */
    int matrixCoefficients = 2;

    /**
     * Colour range: 0 limited (studio swing), 1 full.
     * <p>
     * 색 범위: 0 제한(스튜디오 스윙), 1 전체.
     */
    int colorRange;

    /**
     * Horizontal chroma subsampling (1 if the chroma width is halved).
     * <p>
     * 가로 색차 서브샘플링(색차 너비가 절반이면 1).
     */
    int subsamplingX = 1;

    /**
     * Vertical chroma subsampling.
     * <p>
     * 세로 색차 서브샘플링.
     */
    int subsamplingY = 1;

    /**
     * True if U and V have separate quantizer deltas.
     * <p>
     * U 와 V 의 양자화 변화량이 따로이면 true.
     */
    boolean separateUvDeltaQ;

    /**
     * True if film grain parameters may be present in the frame headers.
     * <p>
     * 프레임 헤더에 필름 그레인 매개변수가 있을 수 있으면 true.
     */
    boolean filmGrainParamsPresent;

    /**
     * Parses a sequence header OBU payload.
     * <p>
     * 시퀀스 헤더 OBU 페이로드를 해석한다.
     *
     * @param r the reader positioned at the payload / 페이로드 위치의 읽기
     * @return the header / 헤더
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    static Av1Sequence parse(Av1BitReader r) throws MediaException {
        Av1Sequence s = new Av1Sequence();
        s.profile = r.bits(3);
        s.stillPicture = r.flag();
        s.reducedStillPictureHeader = r.flag();
        if (s.reducedStillPictureHeader) {
            s.operatingPointIdc[0] = 0;
            r.bits(5);
        } else {
            s.timingInfoPresent = r.flag();
            int bufferDelayLength = 0;
            if (s.timingInfoPresent) {
                r.bitsLong(32);
                r.bitsLong(32);
                s.equalPictureInterval = r.flag();
                if (s.equalPictureInterval) {
                    r.uvlc();
                }
                s.decoderModelInfoPresent = r.flag();
                if (s.decoderModelInfoPresent) {
                    bufferDelayLength = r.bits(5) + 1;
                    r.bitsLong(32);
                    s.bufferRemovalTimeLengthMinus1 = r.bits(5);
                    s.framePresentationTimeLengthMinus1 = r.bits(5);
                }
            }
            boolean initialDisplayDelayPresent = r.flag();
            s.operatingPointsCntMinus1 = r.bits(5);
            // 동작 지점마다 idc, 레벨, 티어, 디코더 모델, 초기 표시 지연을 읽는다.
            for (int i = 0; i <= s.operatingPointsCntMinus1; i++) {
                s.operatingPointIdc[i] = r.bits(12);
                int level = r.bits(5);
                if (level > 7) {
                    r.bit();
                }
                if (s.decoderModelInfoPresent) {
                    s.decoderModelPresentForThisOp[i] = r.flag();
                    if (s.decoderModelPresentForThisOp[i]) {
                        r.bitsLong(bufferDelayLength);
                        r.bitsLong(bufferDelayLength);
                        r.bit();
                    }
                }
                if (initialDisplayDelayPresent) {
                    if (r.flag()) {
                        r.bits(4);
                    }
                }
            }
        }
        s.frameWidthBitsMinus1 = r.bits(4);
        s.frameHeightBitsMinus1 = r.bits(4);
        s.maxFrameWidthMinus1 = r.bits(s.frameWidthBitsMinus1 + 1);
        s.maxFrameHeightMinus1 = r.bits(s.frameHeightBitsMinus1 + 1);
        if (!s.reducedStillPictureHeader) {
            s.frameIdNumbersPresent = r.flag();
        }
        if (s.frameIdNumbersPresent) {
            s.deltaFrameIdLengthMinus2 = r.bits(4);
            s.additionalFrameIdLengthMinus1 = r.bits(3);
        }
        s.use128x128Superblock = r.flag();
        s.enableFilterIntra = r.flag();
        s.enableIntraEdgeFilter = r.flag();
        if (!s.reducedStillPictureHeader) {
            s.enableInterintraCompound = r.flag();
            s.enableMaskedCompound = r.flag();
            s.enableWarpedMotion = r.flag();
            s.enableDualFilter = r.flag();
            s.enableOrderHint = r.flag();
            if (s.enableOrderHint) {
                s.enableJntComp = r.flag();
                s.enableRefFrameMvs = r.flag();
            }
            if (r.flag()) {
                s.forceScreenContentTools = SELECT;
            } else {
                s.forceScreenContentTools = r.bit();
            }
            if (s.forceScreenContentTools > 0) {
                if (r.flag()) {
                    s.forceIntegerMv = SELECT;
                } else {
                    s.forceIntegerMv = r.bit();
                }
            } else {
                s.forceIntegerMv = SELECT;
            }
            if (s.enableOrderHint) {
                s.orderHintBits = r.bits(3) + 1;
            }
        }
        s.enableSuperres = r.flag();
        s.enableCdef = r.flag();
        s.enableRestoration = r.flag();
        s.readColorConfig(r);
        s.filmGrainParamsPresent = r.flag();
        return s;
    }

    /**
     * Reads the colour configuration (section 5.5.2).
     * <p>
     * 색 구성(5.5.2절)을 읽는다.
     *
     * @param r the reader / 읽기
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void readColorConfig(Av1BitReader r) throws MediaException {
        boolean highBitdepth = r.flag();
        if (profile == 2 && highBitdepth) {
            bitDepth = r.flag() ? 12 : 10;
        } else {
            bitDepth = highBitdepth ? 10 : 8;
        }
        monoChrome = profile == 1 ? false : r.flag();
        numPlanes = monoChrome ? 1 : 3;
        if (r.flag()) {
            colorPrimaries = r.bits(8);
            transferCharacteristics = r.bits(8);
            matrixCoefficients = r.bits(8);
        } else {
            colorPrimaries = 2;
            transferCharacteristics = 2;
            matrixCoefficients = 2;
        }
        if (monoChrome) {
            colorRange = r.bit();
            subsamplingX = 1;
            subsamplingY = 1;
            separateUvDeltaQ = false;
            return;
        }
        if (colorPrimaries == 1 && transferCharacteristics == 13 && matrixCoefficients == 0) {
            colorRange = 1;
            subsamplingX = 0;
            subsamplingY = 0;
        } else {
            colorRange = r.bit();
            if (profile == 0) {
                subsamplingX = 1;
                subsamplingY = 1;
            } else if (profile == 1) {
                subsamplingX = 0;
                subsamplingY = 0;
            } else if (bitDepth == 12) {
                subsamplingX = r.bit();
                subsamplingY = subsamplingX != 0 ? r.bit() : 0;
            } else {
                subsamplingX = 1;
                subsamplingY = 0;
            }
            if (subsamplingX != 0 && subsamplingY != 0) {
                r.bits(2);
            }
        }
        separateUvDeltaQ = r.flag();
    }
}
