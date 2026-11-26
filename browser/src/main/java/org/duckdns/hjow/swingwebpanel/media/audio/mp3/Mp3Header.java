package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * The 4-byte header of an MPEG audio frame (ISO/IEC 11172-3 and 13818-3). Only Layer III is accepted by the decoder; Layer I and II
 * headers are parsed so that the caller can report them as unsupported. Immutable.
 * <p>
 * MPEG 오디오 프레임의 4바이트 헤더(ISO/IEC 11172-3, 13818-3). 디코더는 Layer III 만 받아들이며 Layer I, II 헤더는 호출자가 미지원으로
 * 보고할 수 있도록 해석만 한다. 불변 객체이다.
 */
final class Mp3Header {

    /**
     * Layer III bit rates in kbit/s for MPEG-1 (index 1 to 14).
     * <p>
     * MPEG-1 Layer III 비트레이트(kbit/s, 인덱스 1~14).
     */
    private static final int[] BITRATE_V1_L3 = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 };

    /**
     * Layer III bit rates in kbit/s for MPEG-2 and MPEG-2.5 (index 1 to 14).
     * <p>
     * MPEG-2, MPEG-2.5 Layer III 비트레이트(kbit/s, 인덱스 1~14).
     */
    private static final int[] BITRATE_V2_L3 = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 };

    /**
     * Layer I bit rates for MPEG-1 (kbit/s), used only to compute frame lengths of skipped Layer I frames.
     * <p>
     * MPEG-1 Layer I 비트레이트(kbit/s). 건너뛰는 Layer I 프레임 길이 계산에만 쓴다.
     */
    private static final int[] BITRATE_V1_L1 = { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448 };

    /**
     * Layer II bit rates for MPEG-1 (kbit/s).
     * <p>
     * MPEG-1 Layer II 비트레이트(kbit/s).
     */
    private static final int[] BITRATE_V1_L2 = { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384 };

    /**
     * Sample rates of MPEG-1 by sampling frequency index.
     * <p>
     * 샘플링 주파수 인덱스별 MPEG-1 샘플링 주파수.
     */
    private static final int[] RATE_V1 = { 44100, 48000, 32000 };

    /**
     * Version: 1 = MPEG-1, 2 = MPEG-2, 25 = MPEG-2.5.
     * <p>
     * 버전: 1 = MPEG-1, 2 = MPEG-2, 25 = MPEG-2.5.
     */
    final int version;

    /**
     * Layer: 1, 2 or 3.
     * <p>
     * 계층: 1, 2, 3.
     */
    final int layer;

    /**
     * Bit rate in bit/s (0 for free format).
     * <p>
     * 비트레이트(bit/s). 자유 형식이면 0.
     */
    final int bitrate;

    /**
     * Sampling frequency in Hz.
     * <p>
     * 샘플링 주파수(Hz).
     */
    final int sampleRate;

    /**
     * True if the frame has a padding byte.
     * <p>
     * 프레임에 패딩 바이트가 있으면 true.
     */
    final boolean padding;

    /**
     * Channel mode: 0 stereo, 1 joint stereo, 2 dual channel, 3 mono.
     * <p>
     * 채널 모드: 0 스테레오, 1 조인트 스테레오, 2 이중 채널, 3 모노.
     */
    final int mode;

    /**
     * Mode extension (bit 0 intensity stereo, bit 1 MS stereo in Layer III).
     * <p>
     * 모드 확장(Layer III 에서 비트 0 은 인텐시티 스테레오, 비트 1 은 MS 스테레오).
     */
    final int modeExtension;

    /**
     * True if a 16-bit CRC follows the header.
     * <p>
     * 헤더 뒤에 16비트 CRC 가 있으면 true.
     */
    final boolean crc;

    /**
     * Length of the whole frame in bytes (0 for free format, which is not supported).
     * <p>
     * 프레임 전체 길이(바이트). 자유 형식은 지원하지 않으며 0 이다.
     */
    final int frameBytes;

    /**
     * Creates a header from its fields.
     * <p>
     * 필드로 헤더를 만든다.
     *
     * @param version   version / 버전
     * @param layer     layer / 계층
     * @param bitrate   bit rate in bit/s / 비트레이트(bit/s)
     * @param sampleRate sampling frequency / 샘플링 주파수
     * @param padding   padding flag / 패딩 여부
     * @param mode      channel mode / 채널 모드
     * @param modeExtension mode extension / 모드 확장
     * @param crc       CRC flag / CRC 여부
     * @param frameBytes frame length / 프레임 길이
     */
    private Mp3Header(int version, int layer, int bitrate, int sampleRate, boolean padding, int mode, int modeExtension, boolean crc,
            int frameBytes) {
        this.version = version;
        this.layer = layer;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.padding = padding;
        this.mode = mode;
        this.modeExtension = modeExtension;
        this.crc = crc;
        this.frameBytes = frameBytes;
    }

    /**
     * Parses a header at a position. Returns null if the bytes are not a valid header (bad sync, reserved values, free format).
     * <p>
     * 위치의 헤더를 해석한다. 올바른 헤더가 아니면(동기 불일치, 예약 값, 자유 형식) null 을 반환한다.
     *
     * @param d   data / 데이터
     * @param off position / 위치
     * @return header or null / 헤더 또는 null
     */
    static Mp3Header parse(byte[] d, int off) {
        return parse(new ArrayByteSource(d), off);
    }

    /**
     * Parses four header bytes from a seekable input. / 탐색 가능한 입력의 헤더 4바이트를 해석한다.
     * @param d encoded input / 인코딩 입력
     * @param off header position / 헤더 위치
     * @return header or null / 헤더 또는 null
     */
    static Mp3Header parse(ByteSource d, int off) {
        // 동기 바이트가 없거나 헤더가 잘렸으면 뒤의 바이트를 읽지 않는다.
        if (!Bytes.has(d, off, 4) || d.get(off) != 0xFF) {
            return null;
        }
        int b1 = d.get(off + 1);
        // 11비트 동기 워드가 모두 1 이어야 한다.
        if ((b1 & 0xE0) != 0xE0) {
            return null;
        }
        int b2 = d.get(off + 2);
        int b3 = d.get(off + 3);
        int verBits = (b1 >> 3) & 3;
        int layerBits = (b1 >> 1) & 3;
        int brIndex = (b2 >> 4) & 15;
        int srIndex = (b2 >> 2) & 3;
        // 예약된 버전, 계층, 비트레이트 15, 예약된 샘플링 주파수는 거부한다.
        if (verBits == 1 || layerBits == 0 || brIndex == 15 || srIndex == 3) {
            return null;
        }
        int version = verBits == 3 ? 1 : (verBits == 2 ? 2 : 25);
        int layer = 4 - layerBits;
        int rate = RATE_V1[srIndex];
        if (version == 2) {
            rate /= 2;
        } else if (version == 25) {
            rate /= 4;
        }
        int kbps;
        if (version == 1) {
            kbps = layer == 3 ? BITRATE_V1_L3[brIndex] : (layer == 2 ? BITRATE_V1_L2[brIndex] : BITRATE_V1_L1[brIndex]);
        } else {
            kbps = BITRATE_V2_L3[brIndex];
        }
        boolean pad = (b2 & 2) != 0;
        if (brIndex == 0) {
            // 자유 형식은 지원하지 않는다.
            return null;
        }
        int bytes;
        if (layer == 1) {
            bytes = (12 * kbps * 1000 / rate + (pad ? 1 : 0)) * 4;
        } else if (layer == 3 && version != 1) {
            bytes = 72 * kbps * 1000 / rate + (pad ? 1 : 0);
        } else {
            bytes = 144 * kbps * 1000 / rate + (pad ? 1 : 0);
        }
        return new Mp3Header(version, layer, kbps * 1000, rate, pad, (b3 >> 6) & 3, (b3 >> 4) & 3, (b1 & 1) == 0, bytes);
    }

    /**
     * Returns the number of channels (1 for mono, otherwise 2).
     * <p>
     * 채널 수를 반환한다(모노는 1, 그 밖에는 2).
     *
     * @return channels / 채널 수
     */
    int channels() {
        return mode == 3 ? 1 : 2;
    }

    /**
     * Returns the number of PCM frames (samples per channel) in one frame: 1152 for MPEG-1, 576 for MPEG-2 and 2.5.
     * <p>
     * 프레임 하나의 PCM 프레임 수(채널당 샘플 수)를 반환한다: MPEG-1 은 1152, MPEG-2, 2.5 는 576.
     *
     * @return samples per channel / 채널당 샘플 수
     */
    int samplesPerFrame() {
        return layer == 1 ? 384 : (version == 1 ? 1152 : 576);
    }

    /**
     * Returns the number of granules per frame (2 for MPEG-1, 1 otherwise).
     * <p>
     * 프레임당 그래뉼 수를 반환한다(MPEG-1 은 2, 그 밖에는 1).
     *
     * @return granules / 그래뉼 수
     */
    int granules() {
        return version == 1 ? 2 : 1;
    }

    /**
     * Returns the size of the side information in bytes.
     * <p>
     * 부가 정보의 크기(바이트)를 반환한다.
     *
     * @return side information size / 부가 정보 크기
     */
    int sideInfoBytes() {
        if (version == 1) {
            return mode == 3 ? 17 : 32;
        }
        return mode == 3 ? 9 : 17;
    }

    /**
     * Tells whether another header has the same stream parameters (version, layer, sampling frequency), as consecutive frames must.
     * <p>
     * 다른 헤더가 같은 스트림 매개변수(버전, 계층, 샘플링 주파수)를 가졌는지 알려 준다(연속한 프레임은 그래야 한다).
     *
     * @param o other header / 다른 헤더
     * @return true if compatible / 호환되면 true
     */
    boolean sameStream(Mp3Header o) {
        return o != null && o.version == version && o.layer == layer && o.sampleRate == sampleRate;
    }
}
