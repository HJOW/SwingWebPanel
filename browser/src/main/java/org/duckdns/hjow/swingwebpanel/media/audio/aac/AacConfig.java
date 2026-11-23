package org.duckdns.hjow.swingwebpanel.media.audio.aac;

/**
 * The stream parameters of an AAC stream: the audio object type, the sampling frequency and the channel configuration, read from an
 * AudioSpecificConfig (MP4 'esds'), an ADTS header or a program config element (ADIF). Only the AAC-LC core is decoded; HE-AAC (SBR, PS) is
 * recognized and its AAC-LC core is decoded without the bandwidth extension.
 * <p>
 * AAC 스트림의 매개변수: 오디오 객체 종류, 샘플링 주파수, 채널 구성. AudioSpecificConfig(MP4 'esds'), ADTS 헤더, 프로그램 구성 요소(ADIF)에서
 * 읽는다. AAC-LC 핵심만 해독하며 HE-AAC(SBR, PS)는 인식하되 대역 확장 없이 AAC-LC 핵심만 해독한다.
 */
final class AacConfig {

    /** Number of channels by channel configuration 0 to 7 (0 = defined by a program config element). / 채널 구성 0~7 별 채널 수(0 은 PCE 가 정의). */
    private static final int[] CHANNELS = { 0, 1, 2, 3, 4, 5, 6, 8 };

    /** Audio object type (1 main, 2 LC, 5 SBR, 29 PS). / 오디오 객체 종류. */
    final int objectType;
    /** Sampling frequency index 0 to 11. / 샘플링 주파수 인덱스. */
    final int rateIndex;
    /** Sampling frequency of the AAC-LC core in Hz. / AAC-LC 핵심의 샘플링 주파수(Hz). */
    final int sampleRate;
    /** Channel configuration 1 to 7 (0 when unknown yet). / 채널 구성(아직 모르면 0). */
    final int channelConfig;
    /** True if the stream signals SBR (HE-AAC), which this decoder skips. / 스트림이 SBR(HE-AAC)을 알리면 true. 이 디코더는 건너뛴다. */
    final boolean sbr;

    /**
     * Creates the configuration.
     * <p>
     * 구성을 만든다.
     *
     * @param objectType    object type / 객체 종류
     * @param rateIndex     sampling frequency index / 샘플링 주파수 인덱스
     * @param channelConfig channel configuration / 채널 구성
     * @param sbr           SBR signalled / SBR 표시
     */
    AacConfig(int objectType, int rateIndex, int channelConfig, boolean sbr) {
        this.objectType = objectType;
        this.rateIndex = rateIndex;
        this.sampleRate = rateIndex >= 0 && rateIndex < 13 ? AacBands.SAMPLE_RATES[rateIndex] : 0;
        this.channelConfig = channelConfig;
        this.sbr = sbr;
    }

    /**
     * Returns the number of channels of the configuration.
     * <p>
     * 구성의 채널 수를 반환한다.
     *
     * @return channels, 0 if unknown / 채널 수. 모르면 0
     */
    int channels() {
        if (channelConfig == 11) {
            return 7;
        }
        if (channelConfig == 12 || channelConfig == 14) {
            return 8;
        }
        return channelConfig >= 0 && channelConfig < 8 ? CHANNELS[channelConfig] : 0;
    }

    /**
     * Parses an AudioSpecificConfig (ISO/IEC 14496-3 1.6.2.1).
     * <p>
     * AudioSpecificConfig(ISO/IEC 14496-3 1.6.2.1)를 해석한다.
     *
     * @param d   data / 데이터
     * @param off first byte / 첫 바이트
     * @param len number of bytes / 바이트 수
     * @return the configuration / 구성
     * @throws AacException if the configuration is invalid or not supported / 구성이 올바르지 않거나 지원하지 않으면
     */
    static AacConfig fromAudioSpecificConfig(byte[] d, int off, int len) throws AacException {
        AacBits in = new AacBits(d, off, len);
        int aot = in.bits(5);
        if (aot == 31) {
            aot = 32 + in.bits(6);
        }
        int idx = in.bits(4);
        if (idx == 15) {
            in.skip(24);
            throw new AacException("explicit sampling frequency is not supported");
        }
        int cc = in.bits(4);
        boolean sbr = false;
        if (aot == 5 || aot == 29) {
            sbr = true;
            int extIdx = in.bits(4);
            if (extIdx == 15) {
                in.skip(24);
            }
            aot = in.bits(5);
            if (aot == 31) {
                aot = 32 + in.bits(6);
            }
        }
        if (aot != 1 && aot != 2) {
            throw new AacException("audio object type " + aot + " is not supported");
        }
        if (idx > 12) {
            throw new AacException("invalid sampling frequency index");
        }
        if (in.flag()) {
            throw new AacException("960-line frames are not supported");
        }
        if (in.flag()) {
            in.skip(14);
        }
        in.flag();
        if (cc == 0) {
            // 채널 구성 0 은 ASC 안의 프로그램 구성 요소가 정의한다.
            in.skip(0);
            AacConfig fromPce = readPceInAsc(in, aot, idx);
            if (fromPce != null) {
                return fromPce;
            }
            throw new AacException("channel configuration 0 without a usable program config element");
        }
        // 7 보다 큰 구성 중 6.1(11)과 7.1(12, 14)만 지원한다.
        if (cc > 7 && cc != 11 && cc != 12 && cc != 14) {
            throw new AacException("invalid channel configuration");
        }
        return new AacConfig(aot, idx, cc, sbr);
    }

    /**
     * Reads the program config element that follows the GASpecificConfig when the channel configuration is 0.
     * <p>
     * 채널 구성이 0 일 때 GASpecificConfig 뒤에 오는 프로그램 구성 요소를 읽는다.
     *
     * @param in  input / 입력
     * @param aot object type / 객체 종류
     * @param idx sampling index / 샘플링 인덱스
     * @return the configuration or null / 구성 또는 null
     */
    private static AacConfig readPceInAsc(AacBits in, int aot, int idx) {
        try {
            int ch = readPce(in);
            int cc = ch == 1 ? 1 : (ch == 2 ? 2 : (ch == 3 ? 3 : (ch == 4 ? 4 : (ch == 5 ? 5 : (ch == 6 ? 6 : 7)))));
            return new AacConfig(aot, idx, cc, false);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Parses a program config element and returns its channel count (ISO/IEC 14496-3 4.4.1.1).
     * <p>
     * 프로그램 구성 요소를 해석해 채널 수를 반환한다(ISO/IEC 14496-3 4.4.1.1).
     *
     * @param in input positioned after the element instance tag and before the object type, i.e. at the start of the element / 입력(요소의 시작)
     * @return number of channels / 채널 수
     */
    static int readPce(AacBits in) {
        in.skip(4);
        in.skip(2);
        in.skip(4);
        int front = in.bits(4);
        int side = in.bits(4);
        int back = in.bits(4);
        int lfe = in.bits(2);
        int assoc = in.bits(3);
        int cc = in.bits(4);
        if (in.flag()) {
            in.skip(4);
        }
        if (in.flag()) {
            in.skip(4);
        }
        if (in.flag()) {
            in.skip(3);
        }
        int channels = 0;
        for (int i = 0; i < front + side + back; i++) { // 앞, 옆, 뒤 요소마다 CPE 표시와 태그를 읽는다
            boolean cpe = in.flag();
            in.skip(4);
            channels += cpe ? 2 : 1;
        }
        for (int i = 0; i < lfe; i++) { // LFE 요소의 태그를 읽는다
            in.skip(4);
            channels++;
        }
        for (int i = 0; i < assoc; i++) { // 보조 데이터 요소의 태그를 건너뛴다
            in.skip(4);
        }
        for (int i = 0; i < cc; i++) { // 결합 채널 요소 정보를 건너뛴다
            in.skip(5);
        }
        in.align();
        int comment = in.bits(8);
        in.skip(8 * comment);
        return channels;
    }
}
