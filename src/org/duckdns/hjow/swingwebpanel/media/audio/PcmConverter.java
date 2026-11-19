package org.duckdns.hjow.swingwebpanel.media.audio;

/**
 * Sample conversions shared by the decoders: clipping a full-scale float to 16 bits, narrowing integer samples of any width to 16 bits
 * with rounding, and the ITU-T G.711 mu-law and A-law expansion tables.
 * <p>
 * 디코더들이 공유하는 샘플 변환: 풀 스케일 float 를 16비트로 자르기, 임의 폭의 정수 샘플을 반올림해 16비트로 줄이기, ITU-T G.711 mu-law 와 A-law
 * 확장 표.
 */
public final class PcmConverter {

    /**
     * 16-bit expansion of every mu-law byte.
     * <p>
     * mu-law 바이트마다의 16비트 확장 값.
     */
    private static final short[] ULAW = new short[256];

    /**
     * 16-bit expansion of every A-law byte.
     * <p>
     * A-law 바이트마다의 16비트 확장 값.
     */
    private static final short[] ALAW = new short[256];

    static {
        // 표준 G.711 알고리즘으로 두 표를 채운다.
        for (int i = 0; i < 256; i++) {
            ULAW[i] = (short) expandUlaw(i);
            ALAW[i] = (short) expandAlaw(i);
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PcmConverter() {
    }

    /**
     * G.711 mu-law expansion of one byte (reference algorithm of the ITU-T recommendation).
     * <p>
     * 바이트 하나의 G.711 mu-law 확장(권고안의 기준 알고리즘).
     *
     * @param code mu-law byte / mu-law 바이트
     * @return linear value / 선형 값
     */
    private static int expandUlaw(int code) {
        int u = ~code & 0xFF;
        int t = ((u & 0x0F) << 3) + 0x84;
        t <<= (u & 0x70) >> 4;
        return (u & 0x80) != 0 ? 0x84 - t : t - 0x84;
    }

    /**
     * G.711 A-law expansion of one byte (reference algorithm of the ITU-T recommendation).
     * <p>
     * 바이트 하나의 G.711 A-law 확장(권고안의 기준 알고리즘).
     *
     * @param code A-law byte / A-law 바이트
     * @return linear value / 선형 값
     */
    private static int expandAlaw(int code) {
        int a = (code & 0xFF) ^ 0x55;
        int t = (a & 0x0F) << 4;
        int seg = (a & 0x70) >> 4;
        if (seg == 0) {
            t += 8;
        } else {
            t += 0x108;
            if (seg > 1) {
                t <<= seg - 1;
            }
        }
        return (a & 0x80) != 0 ? t : -t;
    }

    /**
     * Expands a mu-law byte to a 16-bit sample.
     * <p>
     * mu-law 바이트를 16비트 샘플로 확장한다.
     *
     * @param code mu-law byte (0..255) / mu-law 바이트(0..255)
     * @return sample / 샘플
     */
    public static short ulaw(int code) {
        return ULAW[code & 0xFF];
    }

    /**
     * Expands an A-law byte to a 16-bit sample.
     * <p>
     * A-law 바이트를 16비트 샘플로 확장한다.
     *
     * @param code A-law byte (0..255) / A-law 바이트(0..255)
     * @return sample / 샘플
     */
    public static short alaw(int code) {
        return ALAW[code & 0xFF];
    }

    /**
     * Converts a full-scale float sample (1.0 = 32768) to 16 bits: rounds half up and clips; NaN becomes 0.
     * <p>
     * 풀 스케일 float 샘플(1.0 = 32768)을 16비트로 바꾼다: 반올림(.5 는 올림)하고 잘라 내며 NaN 은 0 이 된다.
     *
     * @param v sample / 샘플
     * @return 16-bit sample / 16비트 샘플
     */
    public static short clip16(float v) {
        if (v != v) {
            return 0;
        }
        float s = v * 32768f;
        if (s >= 32767f) {
            return 32767;
        }
        if (s <= -32768f) {
            return -32768;
        }
        return (short) (int) Math.floor(s + 0.5f);
    }

    /**
     * Converts a double sample (1.0 = 32768) to 16 bits like {@link #clip16(float)}.
     * <p>
     * double 샘플(1.0 = 32768)을 {@link #clip16(float)} 처럼 16비트로 바꾼다.
     *
     * @param v sample / 샘플
     * @return 16-bit sample / 16비트 샘플
     */
    public static short clip16(double v) {
        if (v != v) {
            return 0;
        }
        double s = v * 32768.0;
        if (s >= 32767.0) {
            return 32767;
        }
        if (s <= -32768.0) {
            return -32768;
        }
        return (short) (int) Math.floor(s + 0.5);
    }

    /**
     * Narrows a signed integer sample of <code>bits</code> bits (1..32) to 16 bits: narrower samples are shifted up, wider samples are
     * rounded (half up) and clipped. No dither is added.
     * <p>
     * <code>bits</code> 비트(1..32)의 부호 있는 정수 샘플을 16비트로 바꾼다: 더 좁은 샘플은 위로 밀고, 더 넓은 샘플은 반올림(.5 는 올림)하고 잘라 낸다.
     * 디더는 더하지 않는다.
     *
     * @param sample signed sample / 부호 있는 샘플
     * @param bits   width of the sample in bits / 샘플의 비트 폭
     * @return 16-bit sample / 16비트 샘플
     */
    public static short narrow(long sample, int bits) {
        if (bits <= 16) {
            return (short) (sample << (16 - bits));
        }
        int shift = bits - 16;
        long v = (sample + (1L << (shift - 1))) >> shift;
        if (v > 32767) {
            return 32767;
        }
        if (v < -32768) {
            return -32768;
        }
        return (short) v;
    }
}
