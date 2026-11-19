package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Small numeric helpers of the CELT decoder, translated from the reference decoder's floating-point configuration
 * (<code>celt/mathops.h</code>, <code>celt/bands.c</code>): exp2/log2 with the double-precision library functions, the
 * linear congruential noise generator, the integer square root and the bit-exact trigonometric approximations that the band
 * splitting decoder must reproduce exactly.
 * <p>
 * 참조 디코더의 부동소수점 설정(<code>celt/mathops.h</code>, <code>celt/bands.c</code>)에서 옮긴 CELT 디코더의 작은 수치 도우미:
 * 배정밀도 라이브러리 함수를 쓰는 exp2/log2, 선형 합동 잡음 발생기, 정수 제곱근, 대역 분할 디코더가 정확히 재현해야 하는
 * 비트 단위로 정확한 삼각함수 근사.
 */
final class CeltMath {

    /**
     * Single precision pi as the reference decoder defines it.
     * <p>
     * 참조 디코더가 정의한 단정밀도 원주율.
     */
    static final float PI = 3.141592653f;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private CeltMath() {
    }

    /**
     * Base-2 exponential.
     * <p>
     * 밑이 2 인 지수 함수.
     *
     * @param x exponent / 지수
     * @return 2^x
     */
    static float exp2(float x) {
        return (float) Math.exp(0.6931471805599453094 * x);
    }

    /**
     * Base-2 logarithm.
     * <p>
     * 밑이 2 인 로그.
     *
     * @param x positive value / 양수
     * @return log2(x)
     */
    static float log2(float x) {
        return (float) (1.442695040888963387 * Math.log(x));
    }

    /**
     * Square root in single precision.
     * <p>
     * 단정밀도 제곱근.
     *
     * @param x non-negative value / 0 이상의 값
     * @return the root / 제곱근
     */
    static float sqrt(float x) {
        return (float) Math.sqrt(x);
    }

    /**
     * Reciprocal square root.
     * <p>
     * 제곱근의 역수.
     *
     * @param x positive value / 양수
     * @return 1/sqrt(x)
     */
    static float rsqrt(float x) {
        return 1.f / (float) Math.sqrt(x);
    }

    /**
     * Cosine of pi/2 times a normalized argument.
     * <p>
     * 정규화된 인수에 pi/2 를 곱한 값의 코사인.
     *
     * @param x argument in units of a quarter turn / 사분원 단위 인수
     * @return the cosine / 코사인
     */
    static float cosNorm(float x) {
        return (float) Math.cos((0.5f * PI) * x);
    }

    /**
     * The noise generator of the decoder (a 32-bit linear congruential generator).
     * <p>
     * 디코더의 잡음 발생기(32비트 선형 합동 발생기).
     *
     * @param seed the previous state / 이전 상태
     * @return the next state / 다음 상태
     */
    static int lcgRand(int seed) {
        return 1664525 * seed + 1013904223;
    }

    /**
     * Integer square root of an unsigned 32-bit value (floor).
     * <p>
     * 부호 없는 32비트 값의 정수 제곱근(내림).
     *
     * @param valIn the value / 값
     * @return the root / 제곱근
     */
    static int isqrt32(long valIn) {
        long val = valIn & 0xFFFFFFFFL;
        long g = 0;
        int bshift = (OpusRangeDecoder.ilog(val) - 1) >> 1;
        long b = 1L << bshift;
        // 비트를 높은 쪽부터 하나씩 시험하며 제곱근을 구한다.
        do {
            long t = ((g << 1) + b) << bshift;
            if (t <= val) {
                g += b;
                val -= t;
            }
            b >>= 1;
            bshift--;
        } while (bshift >= 0);
        return (int) g;
    }

    /**
     * Rounded Q15 product of two 16-bit values.
     * <p>
     * 16비트 값 두 개의 Q15 반올림 곱.
     *
     * @param a first value / 첫 값
     * @param b second value / 둘째 값
     * @return (a * b + 16384) >> 15
     */
    static int fracMul16(int a, int b) {
        return (16384 + ((short) a * (short) b)) >> 15;
    }

    /**
     * Bit-exact cosine approximation used by the stereo angle decoding.
     * <p>
     * 스테레오 각도 디코딩에 쓰는 비트 단위로 정확한 코사인 근사.
     *
     * @param x angle in units of 2^-14 quarter turns, 0..16384 / 2^-14 사분원 단위 각도(0..16384)
     * @return the cosine in Q15 / Q15 코사인
     */
    static int bitexactCos(int x) {
        int tmp = (4096 + x * x) >> 13;
        int x2 = tmp;
        x2 = (32767 - x2) + fracMul16(x2, (-7651 + fracMul16(x2, (8277 + fracMul16(-626, x2)))));
        return 1 + x2;
    }

    /**
     * Bit-exact base-2 logarithm of the tangent from a sine and a cosine.
     * <p>
     * 사인과 코사인에서 탄젠트의 밑 2 로그를 비트 단위로 정확히 구한다.
     *
     * @param isinIn sine in Q15 / Q15 사인
     * @param icosIn cosine in Q15 / Q15 코사인
     * @return the logarithm in units of 2^-11 / 2^-11 단위 로그
     */
    static int bitexactLog2tan(int isinIn, int icosIn) {
        int lc = OpusRangeDecoder.ilog(icosIn);
        int ls = OpusRangeDecoder.ilog(isinIn);
        int icos = icosIn << (15 - lc);
        int isin = isinIn << (15 - ls);
        return (ls - lc) * (1 << 11) + fracMul16(isin, fracMul16(isin, -2597) + 7932) - fracMul16(icos, fracMul16(icos, -2597) + 7932);
    }
}
