package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Fixed-point arithmetic primitives of the SILK decoder: the saturating and rounding multiply-accumulate macros of
 * <code>silk/macros.h</code> and <code>silk/SigProc_FIX.h</code>, the approximate logarithm, exponential, square root and division
 * of <code>silk/Inlines.h</code>, and the bandwidth expansion of an LPC filter. All values are 32-bit two's complement integers
 * whose overflow wraps around exactly like in the reference decoder (Java int arithmetic), and 16-bit values are passed as
 * <code>int</code> or <code>short</code> as noted. The suffix <code>Q</code><i>n</i> in names denotes a fixed-point format with
 * <i>n</i> fractional bits.
 * <p>
 * SILK 디코더의 고정소수점 연산 기본 함수: <code>silk/macros.h</code>, <code>silk/SigProc_FIX.h</code> 의 포화/반올림 곱셈-누산 매크로,
 * <code>silk/Inlines.h</code> 의 근사 로그, 지수, 제곱근, 나눗셈, LPC 필터의 대역폭 확장. 모든 값은 32비트 2의 보수 정수이며 오버플로는
 * 참조 디코더와 똑같이 되감긴다(Java int 연산). 16비트 값은 표기에 따라 <code>int</code> 또는 <code>short</code> 로 전달한다. 이름의
 * <code>Q</code><i>n</i> 은 소수 비트가 <i>n</i> 개인 고정소수점 형식을 뜻한다.
 */
final class SilkMath {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private SilkMath() {
    }

    /**
     * (a32 * (int16) b32) >> 16.
     * <p>
     * 32비트 값과 16비트 값의 곱을 16비트 내린 값.
     *
     * @param a32 32-bit value / 32비트 값
     * @param b32 value whose low 16 bits are used / 하위 16비트를 쓰는 값
     * @return the product / 곱
     */
    static int smulwb(int a32, int b32) {
        return ((a32 >> 16) * (short) b32) + (((a32 & 0x0000FFFF) * (short) b32) >> 16);
    }

    /**
     * a32 + ((b32 * (int16) c32) >> 16).
     * <p>
     * a32 에 smulwb(b32, c32) 를 더한 값.
     *
     * @param a32 accumulator / 누산 값
     * @param b32 32-bit value / 32비트 값
     * @param c32 value whose low 16 bits are used / 하위 16비트를 쓰는 값
     * @return the sum / 합
     */
    static int smlawb(int a32, int b32, int c32) {
        return a32 + (((b32 >> 16) * (short) c32) + (((b32 & 0x0000FFFF) * (short) c32) >> 16));
    }

    /**
     * (a32 * b32) >> 16 with both operands full 32-bit.
     * <p>
     * 두 피연산자가 모두 32비트인 (a32 * b32) >> 16.
     *
     * @param a32 first operand / 첫 피연산자
     * @param b32 second operand / 둘째 피연산자
     * @return the product / 곱
     */
    static int smulww(int a32, int b32) {
        return smulwb(a32, b32) + a32 * rshiftRound(b32, 16);
    }

    /**
     * a32 + smulww(b32, c32).
     * <p>
     * a32 에 smulww(b32, c32) 를 더한 값.
     *
     * @param a32 accumulator / 누산 값
     * @param b32 first operand / 첫 피연산자
     * @param c32 second operand / 둘째 피연산자
     * @return the sum / 합
     */
    static int smlaww(int a32, int b32, int c32) {
        return smlawb(a32, b32, c32) + b32 * rshiftRound(c32, 16);
    }

    /**
     * Product of the low 16 bits of both operands.
     * <p>
     * 두 피연산자의 하위 16비트의 곱.
     *
     * @param a32 first operand / 첫 피연산자
     * @param b32 second operand / 둘째 피연산자
     * @return the product / 곱
     */
    static int smulbb(int a32, int b32) {
        return (short) a32 * (short) b32;
    }

    /**
     * a32 plus the product of the low 16 bits of b32 and c32.
     * <p>
     * a32 에 b32 와 c32 의 하위 16비트의 곱을 더한 값.
     *
     * @param a32 accumulator / 누산 값
     * @param b32 first operand / 첫 피연산자
     * @param c32 second operand / 둘째 피연산자
     * @return the sum / 합
     */
    static int smlabb(int a32, int b32, int c32) {
        return a32 + (short) b32 * (short) c32;
    }

    /**
     * Upper 32 bits of the 64-bit product.
     * <p>
     * 64비트 곱의 상위 32비트.
     *
     * @param a32 first operand / 첫 피연산자
     * @param b32 second operand / 둘째 피연산자
     * @return (a32 * b32) >> 32
     */
    static int smmul(int a32, int b32) {
        return (int) (((long) a32 * (long) b32) >> 32);
    }

    /**
     * Arithmetic shift right with rounding to nearest.
     * <p>
     * 가장 가까운 값으로 반올림하는 산술 오른쪽 이동.
     *
     * @param a     the value / 값
     * @param shift number of bits, at least 1 / 비트 수(1 이상)
     * @return the rounded value / 반올림한 값
     */
    static int rshiftRound(int a, int shift) {
        return shift == 1 ? (a >> 1) + (a & 1) : ((a >> (shift - 1)) + 1) >> 1;
    }

    /**
     * 64-bit arithmetic shift right with rounding to nearest.
     * <p>
     * 가장 가까운 값으로 반올림하는 64비트 산술 오른쪽 이동.
     *
     * @param a     the value / 값
     * @param shift number of bits, at least 1 / 비트 수(1 이상)
     * @return the rounded value / 반올림한 값
     */
    static long rshiftRound64(long a, int shift) {
        return shift == 1 ? (a >> 1) + (a & 1) : ((a >> (shift - 1)) + 1) >> 1;
    }

    /**
     * Subtraction with saturation to the 32-bit range.
     * <p>
     * 32비트 범위로 포화하는 뺄셈.
     *
     * @param a minuend / 피감수
     * @param b subtrahend / 감수
     * @return a - b, limited to the int range / int 범위로 제한한 a - b
     */
    static int subSat32(int a, int b) {
        long d = (long) a - (long) b;
        return d > Integer.MAX_VALUE ? Integer.MAX_VALUE : (d < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) d);
    }

    /**
     * Saturates to the range of a 16-bit integer.
     * <p>
     * 16비트 정수 범위로 포화한다.
     *
     * @param a the value / 값
     * @return the saturated value / 포화한 값
     */
    static int sat16(int a) {
        return a > 32767 ? 32767 : (a < -32768 ? -32768 : a);
    }

    /**
     * Number of leading zero bits of a 32-bit value.
     * <p>
     * 32비트 값의 앞쪽 0 비트 수.
     *
     * @param in32 the value / 값
     * @return the count, 32 for zero / 개수(0 이면 32)
     */
    static int clz32(int in32) {
        return Integer.numberOfLeadingZeros(in32);
    }

    /**
     * Left shift with saturation to the 32-bit range.
     * <p>
     * 32비트 범위로 포화하는 왼쪽 이동.
     *
     * @param a     the value / 값
     * @param shift number of bits / 비트 수
     * @return the shifted value / 이동한 값
     */
    static int lshiftSat32(int a, int shift) {
        int lo = Integer.MIN_VALUE >> shift;
        int hi = Integer.MAX_VALUE >> shift;
        int v = a > hi ? hi : (a < lo ? lo : a);
        return v << shift;
    }

    /**
     * Restricts a value to an interval (the limits may be in either order).
     * <p>
     * 값을 구간으로 제한한다(두 한계의 순서는 상관없다).
     *
     * @param a      the value / 값
     * @param limit1 first limit / 첫 한계
     * @param limit2 second limit / 둘째 한계
     * @return the limited value / 제한한 값
     */
    static int limit(int a, int limit1, int limit2) {
        if (limit1 > limit2) {
            return a > limit1 ? limit1 : (a < limit2 ? limit2 : a);
        }
        return a > limit2 ? limit2 : (a < limit1 ? limit1 : a);
    }

    /**
     * Pseudo-random number generator step of the decoder.
     * <p>
     * 디코더의 의사 난수 발생기 한 단계.
     *
     * @param seed the previous state / 이전 상태
     * @return the next state / 다음 상태
     */
    static int rand(int seed) {
        return 907633515 + seed * 196314165;
    }

    /**
     * Base-2 logarithm in Q7.
     * <p>
     * Q7 밑 2 로그.
     *
     * @param inLin a positive value / 양수
     * @return log2 in Q7 / Q7 로그
     */
    static int lin2log(int inLin) {
        int lz = clz32(inLin);
        int fracQ7 = ror32(inLin, 24 - lz) & 0x7f;
        return ((31 - lz) << 7) + smlawb(fracQ7, fracQ7 * (128 - fracQ7), 179);
    }

    /**
     * Rotates right (left for a negative count).
     * <p>
     * 오른쪽으로 회전한다(음수면 왼쪽).
     *
     * @param a32 the value / 값
     * @param rot number of bits / 비트 수
     * @return the rotated value / 회전한 값
     */
    static int ror32(int a32, int rot) {
        if (rot == 0) {
            return a32;
        }
        if (rot < 0) {
            int m = -rot;
            return (a32 << m) | (a32 >>> (32 - m));
        }
        return (a32 << (32 - rot)) | (a32 >>> rot);
    }

    /**
     * Converts a Q7 base-2 logarithm to a linear value.
     * <p>
     * Q7 밑 2 로그를 선형 값으로 바꾼다.
     *
     * @param inLogQ7 the logarithm in Q7 / Q7 로그
     * @return 2^(in/128)
     */
    static int log2lin(int inLogQ7) {
        if (inLogQ7 < 0) {
            return 0;
        }
        int out = 1 << (inLogQ7 >> 7);
        int fracQ7 = inLogQ7 & 0x7F;
        if (inLogQ7 < 2048) {
            out = out + ((out * smlawb(fracQ7, smulbb(fracQ7, 128 - fracQ7), -174)) >> 7);
        } else {
            out = out + (out >> 7) * smlawb(fracQ7, smulbb(fracQ7, 128 - fracQ7), -174);
        }
        return out;
    }

    /**
     * Approximate square root.
     * <p>
     * 근사 제곱근.
     *
     * @param x the value / 값
     * @return the root, 0 for non-positive values / 제곱근(0 이하이면 0)
     */
    static int sqrtApprox(int x) {
        if (x <= 0) {
            return 0;
        }
        int lz = clz32(x);
        int fracQ7 = ror32(x, 24 - lz) & 0x7f;
        int y;
        if ((lz & 1) != 0) {
            y = 32768;
        } else {
            y = 46214;
        }
        y >>= lz >> 1;
        y = smlawb(y, y, smulbb(213, fracQ7));
        return y;
    }

    /**
     * Division of two 32-bit values with a result in a given Q format.
     * <p>
     * 두 32비트 값의 나눗셈. 결과는 주어진 Q 형식이다.
     *
     * @param a32  dividend / 피제수
     * @param b32  divisor, not zero / 제수(0 이 아님)
     * @param qres number of fractional bits of the result / 결과의 소수 비트 수
     * @return a32 / b32 in Q(qres)
     */
    static int div32VarQ(int a32, int b32, int qres) {
        int aHeadrm = clz32(Math.abs(a32)) - 1;
        int a32Nrm = a32 << aHeadrm;
        int bHeadrm = clz32(Math.abs(b32)) - 1;
        int b32Nrm = b32 << bHeadrm;
        int b32Inv = (Integer.MAX_VALUE >> 2) / (short) (b32Nrm >> 16);
        int result = smulwb(a32Nrm, b32Inv);
        a32Nrm = a32Nrm - (smmul(b32Nrm, result) << 3);
        result = smlawb(result, a32Nrm, b32Inv);
        int lshift = 29 + aHeadrm - bHeadrm - qres;
        if (lshift < 0) {
            return lshiftSat32(result, -lshift);
        }
        if (lshift < 32) {
            return result >> lshift;
        }
        return 0;
    }

    /**
     * Inverse of a 32-bit value in a given Q format.
     * <p>
     * 32비트 값의 역수. 결과는 주어진 Q 형식이다.
     *
     * @param b32  the value, not zero / 값(0 이 아님)
     * @param qres number of fractional bits of the result / 결과의 소수 비트 수
     * @return 1 / b32 in Q(qres)
     */
    static int inverse32VarQ(int b32, int qres) {
        int bHeadrm = clz32(Math.abs(b32)) - 1;
        int b32Nrm = b32 << bHeadrm;
        int b32Inv = (Integer.MAX_VALUE >> 2) / (short) (b32Nrm >> 16);
        int result = b32Inv << 16;
        int errQ32 = ((1 << 29) - smulwb(b32Nrm, b32Inv)) << 3;
        result = smlaww(result, errQ32, b32Inv);
        int lshift = 61 - bHeadrm - qres;
        if (lshift <= 0) {
            return lshiftSat32(result, -lshift);
        }
        if (lshift < 32) {
            return result >> lshift;
        }
        return 0;
    }

    /**
     * Bandwidth expansion of a 16-bit LPC filter: scales the coefficient k by chirp^(k+1).
     * <p>
     * 16비트 LPC 필터의 대역폭 확장: k 번째 계수에 chirp^(k+1) 을 곱한다.
     *
     * @param ar       coefficients, modified in place / 계수. 제자리에서 바뀐다
     * @param off      start of the coefficients / 계수의 시작
     * @param d        number of coefficients / 계수 개수
     * @param chirpIn  chirp factor in Q16 / Q16 chirp 계수
     */
    static void bwexpander(short[] ar, int off, int d, int chirpIn) {
        int chirpQ16 = chirpIn;
        int chirpMinusOneQ16 = chirpQ16 - 65536;
        // 계수마다 chirp 를 곱하고 chirp 를 갱신한다.
        for (int i = 0; i < d - 1; i++) {
            ar[off + i] = (short) rshiftRound(chirpQ16 * ar[off + i], 16);
            chirpQ16 += rshiftRound(chirpQ16 * chirpMinusOneQ16, 16);
        }
        ar[off + d - 1] = (short) rshiftRound(chirpQ16 * ar[off + d - 1], 16);
    }

    /**
     * Bandwidth expansion of a 32-bit LPC filter.
     * <p>
     * 32비트 LPC 필터의 대역폭 확장.
     *
     * @param ar      coefficients, modified in place / 계수. 제자리에서 바뀐다
     * @param d       number of coefficients / 계수 개수
     * @param chirpIn chirp factor in Q16 / Q16 chirp 계수
     */
    static void bwexpander32(int[] ar, int d, int chirpIn) {
        int chirpQ16 = chirpIn;
        int chirpMinusOneQ16 = chirpQ16 - 65536;
        // 계수마다 chirp 를 곱하고 chirp 를 갱신한다.
        for (int i = 0; i < d - 1; i++) {
            ar[i] = smulww(chirpQ16, ar[i]);
            chirpQ16 += rshiftRound(chirpQ16 * chirpMinusOneQ16, 16);
        }
        ar[d - 1] = smulww(chirpQ16, ar[d - 1]);
    }
}
