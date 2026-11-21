package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;

/**
 * Decoder of AAC raw data blocks (ISO/IEC 14496-3 subpart 4): syntactic elements (SCE, CPE, LFE, DSE, PCE, FIL), section and scale factor
 * data, Huffman spectral data, pulse data, inverse quantization, perceptual noise substitution, M/S and intensity stereo, temporal noise
 * shaping, and the inverse MDCT filter bank with sine and Kaiser-Bessel windows and window switching. Main profile prediction, long term
 * prediction, gain control, coupling channels and the SBR/PS extensions are not decoded (frames that need them raise {@link AacException};
 * SBR data in extension payloads is skipped, so HE-AAC plays as its AAC-LC core). The structure follows the JAAD decoder (public domain,
 * net.sourceforge.jaad.aac); one instance keeps the overlap state of one stream. Not thread-safe.
 * <p>
 * AAC 원시 데이터 블록 디코더(ISO/IEC 14496-3 4부): 구문 요소(SCE, CPE, LFE, DSE, PCE, FIL), 구간과 스케일팩터 데이터, 허프만 스펙트럼 데이터,
 * 펄스 데이터, 역양자화, 지각 잡음 대체, M/S 와 인텐시티 스테레오, 시간 잡음 성형, 사인/카이저-베셀 윈도와 윈도 전환을 쓰는 역 MDCT 필터 뱅크.
 * Main 프로파일 예측, 장기 예측, 이득 제어, 결합 채널, SBR/PS 확장은 해독하지 않는다(필요로 하는 프레임은 {@link AacException} 을 일으키며 확장
 * 페이로드의 SBR 데이터는 건너뛰므로 HE-AAC 는 AAC-LC 핵심으로 재생된다). 구조는 JAAD 디코더(퍼블릭 도메인, net.sourceforge.jaad.aac)를 따르며
 * 인스턴스 하나가 스트림 하나의 겹침 상태를 가진다. 스레드 안전하지 않다.
 */
final class AacFrameDecoder {

    /** Zero codebook (no data). / 영 코드북(데이터 없음). */
    private static final int ZERO_HCB = 0;
    /** Noise substitution pseudo codebook. / 잡음 대체 가상 코드북. */
    private static final int NOISE_HCB = 13;
    /** Intensity pseudo codebook, out of phase. / 인텐시티 가상 코드북(역위상). */
    private static final int INTENSITY_HCB2 = 14;
    /** Intensity pseudo codebook, in phase. / 인텐시티 가상 코드북(동위상). */
    private static final int INTENSITY_HCB = 15;
    /** Number of spectral lines per frame. / 프레임당 스펙트럼 선 수. */
    static final int FRAME = 1024;
    /** Longest TNS filter order. / TNS 필터의 최대 차수. */
    private static final int TNS_MAX_ORDER = 20;

    /** x^(4/3) for the quantized magnitudes below 8207. / 8207 미만 양자화 크기의 x^(4/3). */
    private static final float[] POW43 = new float[8207];
    /** Sine window halves: [0] long (1024 rising samples), [1] short (128). / 사인 윈도 절반: [0] 긴 것(상승 1024개), [1] 짧은 것(128개). */
    private static final float[][] SINE = new float[2][];
    /** Kaiser-Bessel window halves: [0] long, [1] short. / 카이저-베셀 윈도 절반: [0] 긴 것, [1] 짧은 것. */
    private static final float[][] KBD = new float[2][];

    static {
        for (int i = 0; i < POW43.length; i++) { // 4/3 거듭제곱 표를 만든다
            POW43[i] = (float) Math.pow(i, 4.0 / 3.0);
        }
        SINE[0] = new float[1024];
        SINE[1] = new float[128];
        for (int i = 0; i < 1024; i++) { // 긴 사인 윈도의 상승 절반
            SINE[0][i] = (float) Math.sin(Math.PI * (i + 0.5) / 2048.0);
        }
        for (int i = 0; i < 128; i++) { // 짧은 사인 윈도의 상승 절반
            SINE[1][i] = (float) Math.sin(Math.PI * (i + 0.5) / 256.0);
        }
        KBD[0] = kaiserBesselDerived(1024, 4.0);
        KBD[1] = kaiserBesselDerived(128, 6.0);
    }

    /**
     * Computes the rising half of a Kaiser-Bessel derived window.
     * <p>
     * 카이저-베셀 파생 윈도의 상승 절반을 계산한다.
     *
     * @param half  number of samples of the half (N/2) / 절반의 샘플 수(N/2)
     * @param alpha window parameter / 윈도 매개변수
     * @return the rising half / 상승 절반
     */
    static float[] kaiserBesselDerived(int half, double alpha) {
        double[] w = new double[half + 1];
        double sum = 0;
        for (int j = 0; j <= half; j++) { // 카이저 창의 누적 합을 구한다
            double r = 2.0 * j / half - 1.0;
            w[j] = bessel0(Math.PI * alpha * Math.sqrt(Math.max(0.0, 1.0 - r * r)));
            sum += w[j];
        }
        float[] out = new float[half];
        double acc = 0;
        for (int n = 0; n < half; n++) { // 누적 합의 제곱근으로 윈도를 만든다
            acc += w[n];
            out[n] = (float) Math.sqrt(acc / sum);
        }
        return out;
    }

    /**
     * Zeroth order modified Bessel function of the first kind.
     * <p>
     * 0차 제1종 변형 베셀 함수.
     *
     * @param x argument / 인수
     * @return value / 값
     */
    private static double bessel0(double x) {
        double sum = 1.0;
        double term = 1.0;
        double q = x * x / 4.0;
        for (int k = 1; k < 60; k++) { // 급수가 충분히 작아질 때까지 더한다
            term *= q / ((double) k * k);
            sum += term;
            if (term < 1e-12 * sum) {
                break;
            }
        }
        return sum;
    }

    /** Decoded information of one individual channel stream. / 개별 채널 스트림 하나의 해독 정보. */
    private static final class Ics {
        /** Window sequence 0 long, 1 start, 2 eight short, 3 stop. / 윈도 순서. */
        int seq;
        /** Window shape of this frame (0 sine, 1 Kaiser-Bessel). / 이 프레임의 윈도 모양. */
        int shape;
        /** max_sfb. / 최대 스케일팩터 대역. */
        int maxSfb;
        /** Number of window groups. / 윈도 그룹 수. */
        int groups;
        /** Windows in each group. / 그룹별 윈도 수. */
        final int[] groupLen = new int[8];
        /** Number of windows (1 or 8). / 윈도 수(1 또는 8). */
        int windows;
        /** Scale factor band offsets. / 스케일팩터 대역 오프셋. */
        int[] swb;
        /** Number of scale factor bands of the window type. / 윈도 종류의 스케일팩터 대역 수. */
        int swbCount;
        /** Codebook by group and band (g * maxSfb + sfb). / 그룹과 대역별 코드북. */
        final int[] cb = new int[8 * 64];
        /** Scale factor gain by group and band. / 그룹과 대역별 스케일팩터 이득. */
        final float[] sf = new float[8 * 64];
        /** global_gain. / 전체 이득. */
        int globalGain;
        /** True if TNS data is present. / TNS 데이터가 있으면 true. */
        boolean tns;
        /** TNS filters per window. / 윈도별 TNS 필터 수. */
        final int[] tnsFilters = new int[8];
        /** TNS filter length by window and filter. / 윈도와 필터별 TNS 길이. */
        final int[][] tnsLength = new int[8][4];
        /** TNS filter order. / TNS 필터 차수. */
        final int[][] tnsOrder = new int[8][4];
        /** TNS filter direction. / TNS 필터 방향. */
        final boolean[][] tnsDown = new boolean[8][4];
        /** TNS reflection coefficients. / TNS 반사 계수. */
        final float[][][] tnsCoef = new float[8][4][TNS_MAX_ORDER];
        /** Quantized and dequantized spectrum. / 양자화, 역양자화된 스펙트럼. */
        final float[] spec = new float[FRAME];
        /** Quantized values. / 양자화 값. */
        final int[] q = new int[FRAME];

        /**
         * Copies the window information (not the data) from another stream. / 다른 스트림에서 윈도 정보(데이터는 제외)를 복사한다.
         *
         * @param o source / 원본
         */
        void copyInfo(Ics o) {
            seq = o.seq;
            shape = o.shape;
            maxSfb = o.maxSfb;
            groups = o.groups;
            System.arraycopy(o.groupLen, 0, groupLen, 0, 8);
            windows = o.windows;
            swb = o.swb;
            swbCount = o.swbCount;
        }
    }

    /** Sampling frequency index. / 샘플링 주파수 인덱스. */
    private final int rateIndex;
    /** Channel configuration (0 when unknown). / 채널 구성(모르면 0). */
    private int channelConfig;
    /** Decoded channels of the current frame. / 현재 프레임의 해독된 채널. */
    private final float[][] chOut = new float[8][FRAME];
    /** Overlap buffers per channel. / 채널별 겹침 버퍼. */
    private final float[][] overlap = new float[8][FRAME];
    /** Window shape of the previous frame per channel. / 채널별 앞 프레임의 윈도 모양. */
    private final int[] prevShape = new int[8];
    /** Long inverse transform. / 긴 역변환. */
    private final AacImdct longT = new AacImdct(2048);
    /** Short inverse transform. / 짧은 역변환. */
    private final AacImdct shortT = new AacImdct(256);
    /** Work buffer for the filter bank (2048 samples). / 필터 뱅크 작업 버퍼(2048개). */
    private final float[] buf = new float[2048];
    /** Work buffer of one short transform. / 짧은 변환 하나의 작업 버퍼. */
    private final float[] sbuf = new float[256];
    /** State of the noise generator. / 잡음 발생기 상태. */
    private int noiseState = 0x1F2E3D4C;
    /** Number of channels decoded in the current frame. / 현재 프레임에서 해독한 채널 수. */
    private int decoded;

    /**
     * Mixer of the speaker roles of the channel configuration (null until the first multi channel block).
     * <p>
     * 채널 구성의 스피커 역할로 만든 믹서(첫 다채널 블록 전에는 null).
     */
    private ChannelMixer mixer;

    /**
     * Channel configuration for which {@link #mixer} was built.
     * <p>
     * {@link #mixer} 를 만든 채널 구성.
     */
    private int mixerConfig = -1;

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param rateIndex     sampling frequency index / 샘플링 주파수 인덱스
     * @param channelConfig channel configuration, 0 if unknown / 채널 구성, 모르면 0
     */
    AacFrameDecoder(int rateIndex, int channelConfig) {
        this.rateIndex = rateIndex;
        this.channelConfig = channelConfig;
    }

    /**
     * Returns the number of channels decoded in the last block. / 마지막 블록에서 해독한 채널 수를 반환한다.
     *
     * @return channels / 채널 수
     */
    int lastChannelCount() {
        return decoded;
    }

    /**
     * Forgets the overlap state and restarts the noise generator of perceptual noise substitution (after a seek), so that decoding from the start gives
     * the same samples as a new decoder; after a seek into the middle the substituted noise is a different (equally random) sequence than in a sequential
     * decoding.
     * <p>
     * 겹침 상태를 버리고 지각 잡음 대체의 잡음 발생기를 다시 시작한다(탐색 뒤). 그래서 처음부터 디코딩하면 새 디코더와 같은 샘플이 나온다. 중간으로 탐색하면
     * 대체된 잡음은 순차 디코딩과 다른(똑같이 무작위인) 수열이다.
     */
    void reset() {
        for (int c = 0; c < 8; c++) { // 채널마다 겹침 버퍼를 지운다
            java.util.Arrays.fill(overlap[c], 0f);
            prevShape[c] = 0;
        }
        noiseState = 0x1F2E3D4C;
    }

    /**
     * Decodes one raw data block and returns the PCM as two channels at most (more channels are mixed down).
     * <p>
     * 원시 데이터 블록 하나를 해독해 최대 두 채널의 PCM 을 반환한다(채널이 더 많으면 섞는다).
     *
     * @param in    input / 입력
     * @param out   output [channel][1024] in the range -1 to 1 / 출력
     * @param wanted number of output channels (1 or 2) / 출력 채널 수
     * @throws AacException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    void decode(AacBits in, float[][] out, int wanted) throws AacException {
        parse(in);
        mixDown(out, wanted);
    }

    /**
     * Decodes one raw data block and returns every decoded channel (the wide path, plan item P25-05): the samples of the channels in the range -1 to
     * 1 as the decoder produces them, in the canonical channel order (front left, right, centre, LFE, back left, right, side left, right: AAC codes the
     * centre first, then left and right, then the back channels and the LFE; the order of the elements is mapped with the speaker roles of the channel
     * configuration; where the configuration does not define the roles the order of the elements is kept). Output channels that the block did not
     * decode are silent.
     * <p>
     * 원시 데이터 블록 하나를 해독해 해독한 모든 채널을 반환한다(넓은 경로, 계획 P25-05). 채널의 표본은 디코더가 만든 그대로 -1 ~ 1 범위이며 표준 채널 순서(앞 왼쪽, 오른쪽, 중앙,
     * LFE, 뒤 왼쪽, 오른쪽, 옆 왼쪽, 오른쪽)이다. AAC 는 중앙을 먼저, 그다음 왼쪽과 오른쪽, 뒤 채널과 LFE 를 부호화한다. 요소의 순서를 채널 구성의 스피커 역할로 바꾸며
     * 구성이 역할을 정하지 않으면 요소의 순서를 유지한다. 블록이 해독하지 않은 출력 채널은 무음이다.
     *
     * @param in  input / 입력
     * @param out output [channel][1024]; its length is the number of channels that the caller wants / 출력. 길이가 호출한 쪽이 원하는 채널 수이다
     * @return the number of channels that the block decoded / 블록이 해독한 채널 수
     * @throws AacException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    int decodeWide(AacBits in, float[][] out) throws AacException {
        parse(in);
        final float scale = 1.0f / 32768.0f;
        int n = decoded;
        // 한두 채널만 해독했는데 출력 채널 수가 다르면 16비트 경로와 같게 맞춘다(모노를 두 채널에 복제, 스테레오를 모노로 평균).
        if (n >= 1 && n <= 2 && out.length <= 2 && out.length != n) {
            for (int i = 0; i < FRAME; i++) {
                float l = chOut[0][i] * scale;
                float r = (n == 2 ? chOut[1][i] : chOut[0][i]) * scale;
                if (out.length == 1) {
                    out[0][i] = n == 2 ? (l + r) * 0.5f : l;
                } else {
                    out[0][i] = l;
                    out[1][i] = r;
                }
            }
            return n;
        }
        int[] order = wideOrder(n);
        // 출력 채널마다 해독한 채널을 표준 순서로 복사하거나 없으면 무음으로 채운다.
        for (int c = 0; c < out.length; c++) {
            if (c >= n) {
                java.util.Arrays.fill(out[c], 0f);
                continue;
            }
            float[] src = chOut[order[c]];
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < FRAME; i++) {
                out[c][i] = src[i] * scale;
            }
        }
        return n;
    }

    /**
     * Returns for every channel of the canonical order the index of the decoded channel (the identity for one or two channels and where the channel
     * configuration does not define the speaker roles).
     * <p>
     * 표준 순서의 채널마다 해독한 채널의 인덱스를 반환한다(채널이 한두 개이거나 채널 구성이 스피커 역할을 정하지 않으면 항등).
     *
     * @param n the number of decoded channels / 해독한 채널 수
     * @return the order / 순서
     */
    private int[] wideOrder(int n) {
        int[] roles = n > 2 ? rolesOf(channelConfig) : null;
        // 역할이 해독한 채널 수와 맞으면 역할 순으로 정렬한 순서를 쓴다.
        if (roles != null && roles.length == n) {
            return ChannelMixer.forRoles(roles).canonicalOrder();
        }
        int[] o = new int[n];
        // 아니면 요소의 순서를 그대로 쓴다.
        for (int i = 0; i < n; i++) {
            o[i] = i;
        }
        return o;
    }

    /**
     * Reads the syntactic elements of one raw data block and decodes the channels into {@link #chOut}.
     * <p>
     * 원시 데이터 블록 하나의 구문 요소를 읽고 채널을 {@link #chOut} 으로 해독한다.
     *
     * @param in input / 입력
     * @throws AacException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    private void parse(AacBits in) throws AacException {
        decoded = 0;
        boolean end = false;
        // 요소를 END 가 나올 때까지 읽는다.
        while (!end) {
            int id = in.bits(3);
            switch (id) {
            case 0:
            case 3:
                readSingle(in);
                break;
            case 1:
                readPair(in);
                break;
            case 2:
                throw new AacException("coupling channel elements are not supported");
            case 4:
                readDse(in);
                break;
            case 5:
                AacConfig.readPce(in);
                break;
            case 6:
                readFil(in);
                break;
            default:
                end = true;
                break;
            }
            if (in.overrun()) {
                throw new AacException("frame data is truncated");
            }
        }
    }

    /**
     * Converts the decoded channels to the wanted number of output channels.
     * <p>
     * 해독한 채널을 원하는 출력 채널 수로 바꾼다.
     *
     * @param out    output / 출력
     * @param wanted channels 1 or 2 / 채널 수 1 또는 2
     */
    private void mixDown(float[][] out, int wanted) {
        final float scale = 1.0f / 32768.0f;
        int n = decoded;
        if (n == 0) {
            for (int c = 0; c < wanted; c++) { // 출력 채널을 0 으로 채운다
                java.util.Arrays.fill(out[c], 0f);
            }
            return;
        }
        if (n <= 2) {
            for (int i = 0; i < FRAME; i++) { // 모노/스테레오는 그대로 옮기거나 채널을 맞춘다
                float l = chOut[0][i] * scale;
                float r = (n == 2 ? chOut[1][i] : chOut[0][i]) * scale;
                if (wanted == 1) {
                    out[0][i] = n == 2 ? (l + r) * 0.5f : l;
                } else {
                    out[0][i] = l;
                    out[1][i] = r;
                }
            }
            return;
        }
        int[] roles = rolesOf(channelConfig);
        // 채널 구성이 알려 준 스피커 역할이 해독한 채널 수와 맞으면 공통 믹서 정책(중앙 -3 dB, 서라운드 -3 dB, LFE 버림)으로 섞는다.
        if (roles != null && roles.length == n) {
            if (mixer == null || mixerConfig != channelConfig) {
                mixer = ChannelMixer.forRoles(roles);
                mixerConfig = channelConfig;
            }
            float[] frame = new float[n];
            float[] mixed = new float[2];
            for (int i = 0; i < FRAME; i++) { // 샘플마다 채널을 섞는다
                for (int c = 0; c < n; c++) { // 채널의 샘플을 풀 스케일 float 로 바꾼다
                    frame[c] = chOut[c][i] * scale;
                }
                mixer.mix(frame, mixed);
                if (wanted == 1) {
                    out[0][i] = (mixed[0] + mixed[1]) * 0.5f;
                } else {
                    out[0][i] = mixed[0];
                    out[1][i] = mixed[1];
                }
            }
            return;
        }
        // 다채널은 중앙, 서라운드를 -3 dB 로 섞고 LFE 는 버린다(채널 순서: 중앙, 왼쪽, 오른쪽, 서라운드...).
        float[] wl = new float[8];
        float[] wr = new float[8];
        int cc = channelConfig;
        wl[0] = 0.70710678f;
        wr[0] = 0.70710678f;
        wl[1] = 1f;
        wr[2] = 1f;
        for (int c = 3; c < n; c++) { // 나머지 채널의 가중치를 정한다
            boolean lfe = (cc == 6 && c == 5) || (cc == 7 && c == 7);
            if (lfe) {
                continue;
            }
            boolean leftSide = c % 2 == 1;
            if (cc == 4 && c == 3) {
                wl[c] = 0.5f;
                wr[c] = 0.5f;
            } else if (leftSide) {
                wl[c] = 0.70710678f;
            } else {
                wr[c] = 0.70710678f;
            }
        }
        float sumL = 0f;
        float sumR = 0f;
        for (int c = 0; c < n; c++) { // 가중치 합으로 정규화해 넘침을 막는다
            sumL += wl[c];
            sumR += wr[c];
        }
        float norm = 1.0f / Math.max(sumL, sumR);
        for (int i = 0; i < FRAME; i++) { // 샘플마다 섞는다
            float l = 0f;
            float r = 0f;
            for (int c = 0; c < n; c++) { // 채널별 가중치를 합산한다
                l += chOut[c][i] * wl[c];
                r += chOut[c][i] * wr[c];
            }
            l *= norm * scale;
            r *= norm * scale;
            if (wanted == 1) {
                out[0][i] = (l + r) * 0.5f;
            } else {
                out[0][i] = l;
                out[1][i] = r;
            }
        }
    }

    /**
     * Reads a single channel element or an LFE element.
     * <p>
     * 단일 채널 요소나 LFE 요소를 읽는다.
     *
     * @param in input / 입력
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readSingle(AacBits in) throws AacException {
        in.skip(4);
        Ics ics = new Ics();
        readIcs(in, ics, false);
        if (ics.tns) {
            applyTns(ics);
        }
        filterBank(ics, decoded);
        decoded++;
    }

    /**
     * Reads a channel pair element.
     * <p>
     * 채널 쌍 요소를 읽는다.
     *
     * @param in input / 입력
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readPair(AacBits in) throws AacException {
        in.skip(4);
        boolean common = in.flag();
        Ics l = new Ics();
        Ics r = new Ics();
        int msType = 0;
        boolean[] msUsed = new boolean[8 * 64];
        if (common) {
            readIcsInfo(in, l);
            r.copyInfo(l);
            msType = in.bits(2);
            if (msType == 1) {
                for (int i = 0; i < l.groups * l.maxSfb; i++) { // 그룹과 대역마다 MS 사용 표시를 읽는다
                    msUsed[i] = in.flag();
                }
            } else if (msType == 2) {
                java.util.Arrays.fill(msUsed, true);
            } else if (msType == 3) {
                throw new AacException("reserved MS mask type");
            }
        }
        readIcsBody(in, l, common);
        readIcsBody(in, r, common);
        if (msType != 0) {
            applyMs(l, r, msUsed);
        }
        applyIntensity(l, r, msType != 0, msUsed);
        if (l.tns) {
            applyTns(l);
        }
        if (r.tns) {
            applyTns(r);
        }
        filterBank(l, decoded);
        filterBank(r, decoded + 1);
        decoded += 2;
    }

    /**
     * Skips a data stream element.
     * <p>
     * 데이터 스트림 요소를 건너뛴다.
     *
     * @param in input / 입력
     */
    private static void readDse(AacBits in) {
        in.skip(4);
        boolean align = in.flag();
        int count = in.bits(8);
        if (count == 255) {
            count += in.bits(8);
        }
        if (align) {
            in.align();
        }
        in.skip(8 * count);
    }

    /**
     * Skips a fill element (SBR and dynamic range payloads are not decoded).
     * <p>
     * 채움 요소를 건너뛴다(SBR 과 동적 범위 페이로드는 해독하지 않는다).
     *
     * @param in input / 입력
     */
    private static void readFil(AacBits in) {
        int count = in.bits(4);
        if (count == 15) {
            count += in.bits(8) - 1;
        }
        in.skip(8 * count);
    }

    /**
     * Reads an individual channel stream complete with its own ics_info.
     * <p>
     * 자신의 ics_info 가 있는 개별 채널 스트림을 읽는다.
     *
     * @param in     input / 입력
     * @param ics    target / 대상
     * @param common true if the info was already read (common window) / 정보를 이미 읽었으면(공통 윈도) true
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readIcs(AacBits in, Ics ics, boolean common) throws AacException {
        ics.globalGain = in.bits(8);
        readIcsInfo(in, ics);
        readIcsData(in, ics);
    }

    /**
     * Reads the body of an individual channel stream: global gain, optional window info, sections, scale factors and spectrum.
     * <p>
     * 개별 채널 스트림의 본문을 읽는다: 전체 이득, (필요하면) 윈도 정보, 구간, 스케일팩터, 스펙트럼.
     *
     * @param in     input / 입력
     * @param ics    target / 대상
     * @param common true if the window info is shared and was read already / 윈도 정보를 공유해 이미 읽었으면 true
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readIcsBody(AacBits in, Ics ics, boolean common) throws AacException {
        ics.globalGain = in.bits(8);
        if (!common) {
            readIcsInfo(in, ics);
        }
        readIcsData(in, ics);
    }

    /**
     * Reads ics_info (window sequence, shape, grouping).
     * <p>
     * ics_info(윈도 순서, 모양, 그룹)를 읽는다.
     *
     * @param in  input / 입력
     * @param ics target / 대상
     * @throws AacException on unsupported prediction / 지원하지 않는 예측일 때
     */
    private void readIcsInfo(AacBits in, Ics ics) throws AacException {
        in.skip(1);
        ics.seq = in.bits(2);
        ics.shape = in.bit();
        ics.groups = 1;
        ics.groupLen[0] = 1;
        if (ics.seq == 2) {
            ics.maxSfb = in.bits(4);
            int grouping = in.bits(7);
            for (int i = 0; i < 7; i++) { // 그룹 비트마다 같은 그룹이면 길이를 늘리고 아니면 새 그룹을 시작한다
                if (((grouping >> (6 - i)) & 1) != 0) {
                    ics.groupLen[ics.groups - 1]++;
                } else {
                    ics.groups++;
                    ics.groupLen[ics.groups - 1] = 1;
                }
            }
            ics.windows = 8;
            ics.swb = AacBands.SHORT[rateIndex];
        } else {
            ics.maxSfb = in.bits(6);
            ics.windows = 1;
            ics.swb = AacBands.LONG[rateIndex];
            if (in.flag()) {
                throw new AacException("AAC Main/LTP prediction is not supported");
            }
        }
        ics.swbCount = ics.swb.length - 1;
        if (ics.maxSfb > ics.swbCount) {
            throw new AacException("max_sfb is out of range");
        }
    }

    /**
     * Reads sections, scale factors, pulses, TNS data and the spectral data, then dequantizes.
     * <p>
     * 구간, 스케일팩터, 펄스, TNS 데이터, 스펙트럼 데이터를 읽고 역양자화한다.
     *
     * @param in  input / 입력
     * @param ics target / 대상
     * @throws AacException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    private void readIcsData(AacBits in, Ics ics) throws AacException {
        readSections(in, ics);
        readScaleFactors(in, ics);
        int pulseCount = 0;
        int[] pulsePos = new int[4];
        int[] pulseAmp = new int[4];
        if (in.flag()) {
            if (ics.seq == 2) {
                throw new AacException("pulse data in short blocks");
            }
            pulseCount = in.bits(2) + 1;
            int startSfb = in.bits(6);
            if (startSfb >= ics.swbCount) {
                throw new AacException("pulse start band is out of range");
            }
            int pos = ics.swb[startSfb];
            for (int i = 0; i < pulseCount; i++) { // 펄스마다 위치 증분과 진폭을 읽는다
                pos += in.bits(5);
                pulseAmp[i] = in.bits(4);
                pulsePos[i] = pos;
                if (pos >= FRAME) {
                    throw new AacException("pulse position is out of range");
                }
            }
        }
        ics.tns = in.flag();
        if (ics.tns) {
            readTns(in, ics);
        }
        if (in.flag()) {
            throw new AacException("gain control is not supported");
        }
        readSpectrum(in, ics, pulseCount, pulsePos, pulseAmp);
    }

    /**
     * Reads the section data: the codebook of each run of bands.
     * <p>
     * 구간 데이터를 읽는다: 대역 묶음마다의 코드북.
     *
     * @param in  input / 입력
     * @param ics target / 대상
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readSections(AacBits in, Ics ics) throws AacException {
        java.util.Arrays.fill(ics.cb, 0);
        int bits = ics.seq == 2 ? 3 : 5;
        int esc = (1 << bits) - 1;
        for (int g = 0; g < ics.groups; g++) { // 그룹마다 대역을 구간으로 나눠 읽는다
            int k = 0;
            while (k < ics.maxSfb) {
                int cb = in.bits(4);
                if (cb == 12) {
                    throw new AacException("invalid codebook 12");
                }
                int len = 0;
                int incr;
                do { // 이스케이프 값이 나오는 동안 길이를 더한다
                    incr = in.bits(bits);
                    len += incr;
                } while (incr == esc && !in.overrun());
                int end = k + len;
                if (end > ics.maxSfb) {
                    throw new AacException("section is too long");
                }
                for (; k < end; k++) { // 구간의 대역에 코드북을 지정한다
                    ics.cb[g * ics.maxSfb + k] = cb;
                }
            }
        }
    }

    /**
     * Reads the scale factors (and the noise and intensity parameters) of all bands.
     * <p>
     * 모든 대역의 스케일팩터(와 잡음, 인텐시티 매개변수)를 읽는다.
     *
     * @param in  input / 입력
     * @param ics target / 대상
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readScaleFactors(AacBits in, Ics ics) throws AacException {
        int gain = ics.globalGain;
        int noise = ics.globalGain - 90;
        int intensity = 0;
        boolean firstNoise = true;
        for (int g = 0; g < ics.groups; g++) { // 그룹과 대역마다 차분을 읽어 누적한다
            for (int sfb = 0; sfb < ics.maxSfb; sfb++) {
                int idx = g * ics.maxSfb + sfb;
                int cb = ics.cb[idx];
                if (cb == ZERO_HCB) {
                    ics.sf[idx] = 0f;
                } else if (cb == INTENSITY_HCB || cb == INTENSITY_HCB2) {
                    intensity += AacHuffman.scaleFactor(in) - 60;
                    int t = Math.min(Math.max(intensity, -155), 100);
                    ics.sf[idx] = (float) Math.pow(2.0, -t / 4.0);
                } else if (cb == NOISE_HCB) {
                    if (firstNoise) {
                        noise += in.bits(9) - 256;
                        firstNoise = false;
                    } else {
                        noise += AacHuffman.scaleFactor(in) - 60;
                    }
                    int t = Math.min(Math.max(noise, -100), 155);
                    ics.sf[idx] = (float) Math.pow(2.0, t / 4.0);
                } else {
                    gain += AacHuffman.scaleFactor(in) - 60;
                    if (gain < 0 || gain > 255) {
                        throw new AacException("scale factor is out of range");
                    }
                    ics.sf[idx] = (float) Math.pow(2.0, (gain - 100) / 4.0);
                }
            }
        }
    }

    /**
     * Reads the TNS filter parameters.
     * <p>
     * TNS 필터 매개변수를 읽는다.
     *
     * @param in  input / 입력
     * @param ics target / 대상
     * @throws AacException on an invalid filter order / 올바르지 않은 필터 차수일 때
     */
    private void readTns(AacBits in, Ics ics) throws AacException {
        boolean shortW = ics.seq == 2;
        int nBits = shortW ? 1 : 2;
        int lenBits = shortW ? 4 : 6;
        int orderBits = shortW ? 3 : 5;
        for (int w = 0; w < ics.windows; w++) { // 윈도마다 필터를 읽는다
            ics.tnsFilters[w] = in.bits(nBits);
            if (ics.tnsFilters[w] == 0) {
                continue;
            }
            int res = in.bit();
            for (int f = 0; f < ics.tnsFilters[w]; f++) { // 필터마다 길이, 차수, 방향, 계수를 읽는다
                ics.tnsLength[w][f] = in.bits(lenBits);
                int order = in.bits(orderBits);
                if (order > TNS_MAX_ORDER) {
                    throw new AacException("TNS filter order is out of range");
                }
                ics.tnsOrder[w][f] = order;
                if (order == 0) {
                    continue;
                }
                ics.tnsDown[w][f] = in.flag();
                int compress = in.bit();
                int resBits = res + 3;
                int coefBits = resBits - compress;
                double iqPos = ((1 << (resBits - 1)) - 0.5) / (Math.PI / 2.0);
                double iqNeg = ((1 << (resBits - 1)) + 0.5) / (Math.PI / 2.0);
                for (int i = 0; i < order; i++) { // 부호 있는 정수를 반사 계수로 바꾼다
                    int raw = in.bits(coefBits);
                    int v = raw >= (1 << (coefBits - 1)) ? raw - (1 << coefBits) : raw;
                    ics.tnsCoef[w][f][i] = (float) Math.sin(v / (v >= 0 ? iqPos : iqNeg));
                }
            }
        }
    }

    /**
     * Reads the Huffman coded spectrum, applies the pulses and dequantizes, with noise substitution.
     * <p>
     * 허프만 부호화된 스펙트럼을 읽고 펄스를 적용해 역양자화한다(잡음 대체 포함).
     *
     * @param in         input / 입력
     * @param ics        target / 대상
     * @param pulseCount number of pulses / 펄스 수
     * @param pulsePos   pulse positions / 펄스 위치
     * @param pulseAmp   pulse amplitudes / 펄스 진폭
     * @throws AacException on damaged data / 손상된 데이터일 때
     */
    private void readSpectrum(AacBits in, Ics ics, int pulseCount, int[] pulsePos, int[] pulseAmp) throws AacException {
        java.util.Arrays.fill(ics.q, 0);
        int[] buf4 = new int[4];
        int groupOff = 0;
        for (int g = 0; g < ics.groups; g++) { // 그룹마다
            int groupLen = ics.groupLen[g];
            for (int sfb = 0; sfb < ics.maxSfb; sfb++) { // 대역마다
                int cb = ics.cb[g * ics.maxSfb + sfb];
                int width = ics.swb[sfb + 1] - ics.swb[sfb];
                int off = groupOff + ics.swb[sfb];
                if (cb == ZERO_HCB || cb >= NOISE_HCB) {
                    continue;
                }
                int num = cb >= 5 ? 2 : 4;
                for (int w = 0; w < groupLen; w++, off += 128) { // 그룹의 윈도마다 대역의 값을 읽는다
                    for (int k = 0; k < width; k += num) {
                        AacHuffman.spectral(in, cb, buf4, 0);
                        for (int j = 0; j < num; j++) { // 해독한 값을 출력 배열에 복사한다
                            ics.q[off + k + j] = buf4[j];
                        }
                    }
                    if (in.overrun()) {
                        throw new AacException("spectral data is truncated");
                    }
                }
            }
            groupOff += groupLen << 7;
        }
        for (int i = 0; i < pulseCount; i++) { // 펄스를 양자화 값에 더한다
            int p = pulsePos[i];
            ics.q[p] += ics.q[p] > 0 ? pulseAmp[i] : -pulseAmp[i];
        }
        groupOff = 0;
        for (int g = 0; g < ics.groups; g++) { // 역양자화와 잡음 대체
            int groupLen = ics.groupLen[g];
            for (int sfb = 0; sfb < ics.maxSfb; sfb++) {
                int idx = g * ics.maxSfb + sfb;
                int cb = ics.cb[idx];
                int width = ics.swb[sfb + 1] - ics.swb[sfb];
                int off = groupOff + ics.swb[sfb];
                for (int w = 0; w < groupLen; w++, off += 128) { // 윈도마다
                    if (cb == NOISE_HCB) {
                        fillNoise(ics.spec, off, width, ics.sf[idx]);
                    } else if (cb == ZERO_HCB || cb >= INTENSITY_HCB2) {
                        for (int k = 0; k < width; k++) { // 대역의 값을 0 으로 채운다
                            ics.spec[off + k] = 0f;
                        }
                    } else {
                        for (int k = 0; k < width; k++) { // 값마다 4/3 거듭제곱과 이득을 곱한다
                            int v = ics.q[off + k];
                            int a = Math.abs(v);
                            float mag = a < POW43.length ? POW43[a] : (float) Math.pow(a, 4.0 / 3.0);
                            ics.spec[off + k] = (v < 0 ? -mag : mag) * ics.sf[idx];
                        }
                    }
                }
            }
            groupOff += groupLen << 7;
        }
    }

    /**
     * Fills a band with noise of the given energy (perceptual noise substitution).
     * <p>
     * 주어진 에너지의 잡음으로 대역을 채운다(지각 잡음 대체).
     *
     * @param spec   spectrum / 스펙트럼
     * @param off    first line / 첫 선
     * @param width  number of lines / 선 수
     * @param amount amplitude 2^(noise_energy / 4) / 진폭
     */
    private void fillNoise(float[] spec, int off, int width, float amount) {
        double energy = 0;
        for (int k = 0; k < width; k++) { // 선형 합동 발생기로 값을 만들고 에너지를 구한다
            noiseState = noiseState * 1664525 + 1013904223;
            spec[off + k] = noiseState;
            energy += (double) spec[off + k] * spec[off + k];
        }
        float scale = energy > 0 ? (float) (amount / Math.sqrt(energy)) : 0f;
        for (int k = 0; k < width; k++) { // 에너지를 맞춘다
            spec[off + k] *= scale;
        }
    }

    /**
     * Applies M/S stereo to the bands that use it.
     * <p>
     * M/S 스테레오를 쓰는 대역에 적용한다.
     *
     * @param l      left channel / 왼쪽 채널
     * @param r      right channel / 오른쪽 채널
     * @param msUsed band flags / 대역 표시
     */
    private void applyMs(Ics l, Ics r, boolean[] msUsed) {
        int groupOff = 0;
        for (int g = 0; g < l.groups; g++) { // 그룹과 대역마다
            for (int sfb = 0; sfb < l.maxSfb; sfb++) {
                int idx = g * l.maxSfb + sfb;
                if (!msUsed[idx] || l.cb[idx] >= NOISE_HCB || r.cb[idx] >= NOISE_HCB) {
                    continue;
                }
                int width = l.swb[sfb + 1] - l.swb[sfb];
                for (int w = 0; w < l.groupLen[g]; w++) { // 윈도마다 선을 변환한다
                    int off = groupOff + w * 128 + l.swb[sfb];
                    for (int k = 0; k < width; k++) {
                        float a = l.spec[off + k];
                        float b = r.spec[off + k];
                        l.spec[off + k] = a + b;
                        r.spec[off + k] = a - b;
                    }
                }
            }
            groupOff += l.groupLen[g] * 128;
        }
    }

    /**
     * Applies intensity stereo to the bands of the right channel that carry the intensity codebooks.
     * <p>
     * 인텐시티 코드북을 가진 오른쪽 채널의 대역에 인텐시티 스테레오를 적용한다.
     *
     * @param l       left channel / 왼쪽 채널
     * @param r       right channel / 오른쪽 채널
     * @param msFlag  true if an MS mask is present / MS 마스크가 있으면 true
     * @param msUsed  MS flags / MS 표시
     */
    private void applyIntensity(Ics l, Ics r, boolean msFlag, boolean[] msUsed) {
        int groupOff = 0;
        for (int g = 0; g < r.groups; g++) { // 그룹과 대역마다
            for (int sfb = 0; sfb < r.maxSfb; sfb++) {
                int idx = g * r.maxSfb + sfb;
                int cb = r.cb[idx];
                if (cb != INTENSITY_HCB && cb != INTENSITY_HCB2) {
                    continue;
                }
                float c = cb == INTENSITY_HCB ? 1f : -1f;
                if (msFlag && msUsed[idx]) {
                    c = -c;
                }
                float scale = c * r.sf[idx];
                int width = r.swb[sfb + 1] - r.swb[sfb];
                for (int w = 0; w < r.groupLen[g]; w++) { // 윈도마다 오른쪽 선을 왼쪽에서 만든다
                    int off = groupOff + w * 128 + r.swb[sfb];
                    for (int k = 0; k < width; k++) {
                        r.spec[off + k] = l.spec[off + k] * scale;
                    }
                }
            }
            groupOff += r.groupLen[g] * 128;
        }
    }

    /**
     * Applies the TNS synthesis filters to the dequantized spectrum.
     * <p>
     * 역양자화된 스펙트럼에 TNS 합성 필터를 적용한다.
     *
     * @param ics channel stream / 채널 스트림
     */
    private void applyTns(Ics ics) {
        boolean shortW = ics.seq == 2;
        int tnsMax = shortW ? AacBands.TNS_MAX_SHORT[rateIndex] : AacBands.TNS_MAX_LONG[rateIndex];
        float[] lpc = new float[TNS_MAX_ORDER + 1];
        float[] tmp = new float[TNS_MAX_ORDER + 1];
        float[] state = new float[TNS_MAX_ORDER];
        for (int w = 0; w < ics.windows; w++) { // 윈도마다
            int bottom = ics.swbCount;
            for (int f = 0; f < ics.tnsFilters[w]; f++) { // 필터를 높은 대역부터 처리한다
                int top = bottom;
                bottom = Math.max(top - ics.tnsLength[w][f], 0);
                int order = ics.tnsOrder[w][f];
                if (order == 0) {
                    continue;
                }
                int start = Math.min(Math.min(bottom, tnsMax), ics.maxSfb);
                int end = Math.min(Math.min(top, tnsMax), ics.maxSfb);
                int startIdx = ics.swb[start];
                int endIdx = ics.swb[end];
                int size = endIdx - startIdx;
                if (size <= 0) {
                    continue;
                }
                lpc[0] = 1f;
                for (int m = 1; m <= order; m++) { // 반사 계수에서 선형 예측 계수를 만든다
                    float k = ics.tnsCoef[w][f][m - 1];
                    for (int i = 1; i < m; i++) {
                        tmp[i] = lpc[i] + k * lpc[m - i];
                    }
                    for (int i = 1; i < m; i++) { // 이전 단계의 계수로 새 계수를 계산한다
                        lpc[i] = tmp[i];
                    }
                    lpc[m] = k;
                }
                java.util.Arrays.fill(state, 0f);
                int base = w * 128;
                int pos = ics.tnsDown[w][f] ? endIdx - 1 : startIdx;
                int inc = ics.tnsDown[w][f] ? -1 : 1;
                for (int i = 0; i < size; i++, pos += inc) { // 전극 필터를 적용한다
                    float y = ics.spec[base + pos];
                    for (int j = 0; j < order; j++) {
                        y -= lpc[j + 1] * state[j];
                    }
                    for (int j = order - 1; j > 0; j--) { // 필터 상태를 한 칸씩 민다
                        state[j] = state[j - 1];
                    }
                    state[0] = y;
                    ics.spec[base + pos] = y;
                }
            }
        }
    }

    /**
     * The window value of a half window.
     * <p>
     * 반쪽 윈도의 값.
     *
     * @param shape 0 sine, 1 Kaiser-Bessel / 0 사인, 1 카이저-베셀
     * @param type  0 long, 1 short / 0 긴 것, 1 짧은 것
     * @return the rising half / 상승 절반
     */
    private static float[] rise(int shape, int type) {
        return shape == 0 ? SINE[type] : KBD[type];
    }

    /**
     * Runs the inverse transform with windowing and overlap-add for one channel and stores the 1024 output samples.
     * <p>
     * 채널 하나에 윈도를 곱한 역변환과 겹쳐 더하기를 해 출력 1024개를 저장한다.
     *
     * @param ics channel stream / 채널 스트림
     * @param ch  absolute channel number / 절대 채널 번호
     */
    private void filterBank(Ics ics, int ch) {
        float[] spec = ics.spec;
        java.util.Arrays.fill(buf, 0f);
        float[] longPrev = rise(prevShape[ch], 0);
        float[] longCur = rise(ics.shape, 0);
        float[] shortPrev = rise(prevShape[ch], 1);
        float[] shortCur = rise(ics.shape, 1);
        float[] t = new float[2048];
        if (ics.seq == 2) {
            for (int w = 0; w < 8; w++) { // 짧은 윈도 8개를 각각 변환해 겹쳐 놓는다
                shortT.inverse(spec, w * 128, sbuf);
                float[] left = w == 0 ? shortPrev : shortCur;
                for (int n = 0; n < 256; n++) { // 짧은 윈도 샘플마다 윈도를 곱해 겹쳐 놓는다
                    float win = n < 128 ? left[n] : shortCur[255 - n];
                    buf[448 + 128 * w + n] += sbuf[n] * win * (1.0f / 128.0f);
                }
            }
        } else {
            longT.inverse(spec, 0, t);
            for (int n = 0; n < 2048; n++) { // 윈도 순서에 맞는 윈도 값을 곱한다
                float win;
                if (n < 1024) {
                    if (ics.seq == 3) {
                        win = n < 448 ? 0f : (n < 576 ? shortPrev[n - 448] : 1f);
                    } else {
                        win = longPrev[n];
                    }
                } else if (ics.seq == 1) {
                    win = n < 1472 ? 1f : (n < 1600 ? shortCur[1599 - n] : 0f);
                } else {
                    win = longCur[2047 - n];
                }
                buf[n] = t[n] * win * (1.0f / 1024.0f);
            }
        }
        float[] ov = overlap[ch];
        float[] o = chOut[ch];
        for (int i = 0; i < FRAME; i++) { // 앞 프레임의 뒷부분과 더하고 이번 프레임의 뒷부분을 저장한다
            o[i] = ov[i] + buf[i];
            ov[i] = buf[1024 + i];
        }
        prevShape[ch] = ics.shape;
    }

    /**
     * Returns the speaker roles of the channels of a channel configuration in the order in which the syntactic elements are decoded (ISO/IEC 14496-3
     * table 1.19: 3 = C L R, 4 = C L R Cs, 5 = C L R Ls Rs, 6 adds the LFE, 7 and 12 = C L R Ls Rs Lrs Rrs LFE, 11 = C L R Ls Rs Cs LFE).
     * <p>
     * 채널 구성의 채널별 스피커 역할을 구문 요소를 해독하는 순서대로 반환한다(ISO/IEC 14496-3 표 1.19: 3 = C L R, 4 = C L R Cs, 5 = C L R Ls Rs, 6 은 LFE 가
     * 더해짐, 7 과 12 = C L R Ls Rs Lrs Rrs LFE, 11 = C L R Ls Rs Cs LFE).
     *
     * @param cc channel configuration / 채널 구성
     * @return role constants of {@link ChannelMixer}, or null if the configuration does not define them / {@link ChannelMixer} 의 역할 상수. 구성이 정하지 않으면 null
     */
    private static int[] rolesOf(int cc) {
        switch (cc) {
        case 3:
            return new int[] { ChannelMixer.FC, ChannelMixer.FL, ChannelMixer.FR };
        case 4:
            return new int[] { ChannelMixer.FC, ChannelMixer.FL, ChannelMixer.FR, ChannelMixer.BC };
        case 5:
            return new int[] { ChannelMixer.FC, ChannelMixer.FL, ChannelMixer.FR, ChannelMixer.BL, ChannelMixer.BR };
        case 6:
            return new int[] { ChannelMixer.FC, ChannelMixer.FL, ChannelMixer.FR, ChannelMixer.BL, ChannelMixer.BR, ChannelMixer.LFE };
        case 7:
        case 12:
            return new int[] { ChannelMixer.FC, ChannelMixer.FL, ChannelMixer.FR, ChannelMixer.SL, ChannelMixer.SR, ChannelMixer.BL, ChannelMixer.BR,
                    ChannelMixer.LFE };
        case 11:
            return new int[] { ChannelMixer.FC, ChannelMixer.FL, ChannelMixer.FR, ChannelMixer.SL, ChannelMixer.SR, ChannelMixer.BC, ChannelMixer.LFE };
        default:
            return null;
        }
    }
}
