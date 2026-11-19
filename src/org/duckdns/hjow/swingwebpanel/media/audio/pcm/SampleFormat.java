package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

/**
 * The sample encodings of the uncompressed and companded audio formats (WAV, AIFF, AU).
 * <p>
 * 비압축 및 압신(companded) 오디오 형식(WAV, AIFF, AU)의 샘플 인코딩.
 */
public enum SampleFormat {

    /**
     * 8-bit unsigned (WAV).
     * <p>
     * 8비트 부호 없음(WAV).
     */
    U8(1),

    /**
     * 8-bit signed (AIFF, AU).
     * <p>
     * 8비트 부호 있음(AIFF, AU).
     */
    S8(1),

    /**
     * 16-bit signed little-endian.
     * <p>
     * 16비트 부호 있는 리틀 엔디언.
     */
    S16LE(2),

    /**
     * 16-bit signed big-endian.
     * <p>
     * 16비트 부호 있는 빅 엔디언.
     */
    S16BE(2),

    /**
     * 24-bit signed little-endian.
     * <p>
     * 24비트 부호 있는 리틀 엔디언.
     */
    S24LE(3),

    /**
     * 24-bit signed big-endian.
     * <p>
     * 24비트 부호 있는 빅 엔디언.
     */
    S24BE(3),

    /**
     * 32-bit signed little-endian.
     * <p>
     * 32비트 부호 있는 리틀 엔디언.
     */
    S32LE(4),

    /**
     * 32-bit signed big-endian.
     * <p>
     * 32비트 부호 있는 빅 엔디언.
     */
    S32BE(4),

    /**
     * 32-bit IEEE float little-endian.
     * <p>
     * 32비트 IEEE 부동소수점 리틀 엔디언.
     */
    F32LE(4),

    /**
     * 32-bit IEEE float big-endian.
     * <p>
     * 32비트 IEEE 부동소수점 빅 엔디언.
     */
    F32BE(4),

    /**
     * 64-bit IEEE float little-endian.
     * <p>
     * 64비트 IEEE 부동소수점 리틀 엔디언.
     */
    F64LE(8),

    /**
     * 64-bit IEEE float big-endian.
     * <p>
     * 64비트 IEEE 부동소수점 빅 엔디언.
     */
    F64BE(8),

    /**
     * G.711 mu-law companded bytes.
     * <p>
     * G.711 mu-law 압신 바이트.
     */
    ULAW(1),

    /**
     * G.711 A-law companded bytes.
     * <p>
     * G.711 A-law 압신 바이트.
     */
    ALAW(1);

    /**
     * Bytes of one sample.
     * <p>
     * 샘플 하나의 바이트 수.
     */
    private final int bytes;

    /**
     * Creates a constant.
     * <p>
     * 상수를 만든다.
     *
     * @param bytes bytes per sample / 샘플당 바이트 수
     */
    SampleFormat(int bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns the number of bytes of one sample.
     * <p>
     * 샘플 하나의 바이트 수를 반환한다.
     *
     * @return bytes / 바이트 수
     */
    public int getWidth() {
        return bytes;
    }
}
