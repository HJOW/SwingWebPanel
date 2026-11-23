package org.duckdns.hjow.swingwebpanel.media.audio.testkit;

import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;

/**
 * Test-only bit writer of Vorbis packets (least significant bit first) with the helper functions of the Vorbis specification: the
 * <code>ilog</code> function and the packing of floats into the 32-bit format of codebooks.
 * <p>
 * Vorbis 패킷용 시험 전용 비트 쓰기 객체(최하위 비트부터)와 Vorbis 규격의 도우미 함수: <code>ilog</code> 함수와 코드북의 32비트 형식으로 float 를
 * 담는 함수.
 */
public final class VorbisBits {

    /**
     * Bytes completed so far.
     * <p>
     * 지금까지 완성한 바이트.
     */
    private final Out out = new Out();

    /**
     * Bits waiting for a full byte.
     * <p>
     * 한 바이트가 찰 때까지 대기 중인 비트.
     */
    private int acc;

    /**
     * Number of waiting bits.
     * <p>
     * 대기 중인 비트 수.
     */
    private int n;

    /**
     * Creates an empty writer.
     * <p>
     * 빈 쓰기 객체를 만든다.
     */
    public VorbisBits() {
    }

    /**
     * Writes up to 32 bits; the low bit of the value is written first.
     * <p>
     * 최대 32비트를 쓴다. 값의 낮은 비트가 먼저 쓰인다.
     *
     * @param v    value / 값
     * @param bits number of bits / 비트 수
     */
    public void put(long v, int bits) {
        // 비트를 하나씩 대기열에 넣고 8개가 차면 바이트로 내보낸다.
        for (int i = 0; i < bits; i++) {
            acc |= (int) ((v >> i) & 1) << n;
            n++;
            if (n == 8) {
                out.u8(acc);
                acc = 0;
                n = 0;
            }
        }
    }

    /**
     * Returns the bytes, padding the last byte with zero bits.
     * <p>
     * 마지막 바이트를 0 비트로 채워 바이트들을 반환한다.
     *
     * @return the packet bytes / 패킷 바이트
     */
    public byte[] bytes() {
        Out copy = new Out();
        copy.bytes(out.toByteArray());
        if (n > 0) {
            copy.u8(acc);
        }
        return copy.toByteArray();
    }

    /**
     * Returns the number of bits written.
     * <p>
     * 쓴 비트 수를 반환한다.
     *
     * @return bits / 비트 수
     */
    public long length() {
        return (long) out.size() * 8 + n;
    }

    /**
     * The ilog function of the specification: the number of bits needed to represent a non-negative integer.
     * <p>
     * 규격의 ilog 함수: 음이 아닌 정수를 표현하는 데 필요한 비트 수.
     *
     * @param x the number / 수
     * @return the bit count / 비트 수
     */
    public static int ilog(int x) {
        int r = 0;
        int v = x;
        // 값이 0 이 될 때까지 오른쪽으로 밀며 센다.
        while (v > 0) {
            r++;
            v >>= 1;
        }
        return r;
    }

    /**
     * Packs a float into the 32-bit codebook format (sign, 10-bit biased exponent, 21-bit mantissa).
     * <p>
     * float 를 32비트 코드북 형식(부호, 10비트 편향 지수, 21비트 가수)으로 담는다.
     *
     * @param val the value / 값
     * @return the 32 bits / 32비트
     */
    public static int pack(float val) {
        if (val == 0f) {
            return 0;
        }
        int sign = val < 0 ? 0x80000000 : 0;
        double a = Math.abs((double) val);
        int exp = (int) Math.floor(Math.log(a) / Math.log(2));
        long mant = Math.round(Math.scalb(a, 20 - exp));
        if (mant >= (1L << 21)) {
            mant >>= 1;
            exp++;
        }
        return sign | ((exp + 768) << 21) | (int) mant;
    }

    /**
     * Unpacks the 32-bit codebook float format.
     * <p>
     * 32비트 코드북 부동소수점 형식을 푼다.
     *
     * @param x the 32 bits / 32비트
     * @return the value / 값
     */
    public static float unpack(int x) {
        long v = x & 0xFFFFFFFFL;
        double mant = v & 0x1FFFFF;
        int exp = (int) ((v & 0x7FE00000L) >> 21);
        if ((v & 0x80000000L) != 0) {
            mant = -mant;
        }
        return (float) Math.scalb(mant, exp - 788);
    }
}
