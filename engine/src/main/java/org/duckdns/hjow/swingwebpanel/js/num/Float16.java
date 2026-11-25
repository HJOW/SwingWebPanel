package org.duckdns.hjow.swingwebpanel.js.num;

/**
 * IEEE 754 binary16 conversions from and to binary64, with round-to-nearest, ties-to-even.
 * <p>
 * IEEE 754 binary16과 binary64 사이를 변환한다. 가장 가까운 값으로 반올림하고 동점은 짝수로 고른다.
 */
public final class Float16 {

    /** The binary64 fraction mask. / binary64의 가수 비트 마스크. */
    private static final long DOUBLE_FRACTION = 0x000fffffffffffffL;

    /** The implicit leading bit of a normal binary64 number. / 정규 binary64 수의 숨은 최상위 비트. */
    private static final long DOUBLE_IMPLICIT = 0x0010000000000000L;

    /** Uses static conversion methods only. / 정적 변환 메서드만 사용한다. */
    private Float16() {
    }

    /**
     * Encodes a double as an unsigned 16-bit representation.
     * <p>
     * double을 부호 없는 16비트 표현으로 인코딩한다.
     *
     * @param value the binary64 value / binary64 값
     * @return binary16 sign, exponent and fraction / binary16 부호, 지수와 가수
     */
    public static int encode(double value) {
        long bits = Double.doubleToRawLongBits(value);
        int sign = (int) (bits >>> 48) & 0x8000;
        int exponent = (int) (bits >>> 52) & 0x7ff;
        long fraction = bits & DOUBLE_FRACTION;
        // 무한대와 NaN의 부호 및 남길 수 있는 페이로드 비트를 그대로 옮긴다.
        if (exponent == 0x7ff) {
            int payload = fraction == 0 ? 0 : Math.max(1, (int) (fraction >>> 42));
            return sign | 0x7c00 | payload;
        }
        int power = exponent - 1023;
        if (power < -25) {
            return sign;
        }
        if (power > 15) {
            return sign | 0x7c00;
        }
        // 비정규수는 고정 단위 2^-24로, 정규수는 지수별로 가수를 한 번만 반올림한다.
        long significant = DOUBLE_IMPLICIT | fraction;
        if (power < -14) {
            return sign | roundRight(significant, 28 - power);
        }
        int rounded = roundRight(significant, 42);
        return sign | (((power + 14) << 10) + rounded);
    }

    /**
     * Rounds a positive significand shifted right by the given distance.
     * <p>
     * 양의 가수를 지정한 거리만큼 오른쪽으로 옮기고 짝수 방향 동점 처리로 반올림한다.
     *
     * @param significant an unsigned binary64 significand / 부호 없는 binary64 가수
     * @param shift the number of bits to remove / 제거할 비트 수
     * @return the rounded significand / 반올림한 가수
     */
    private static int roundRight(long significant, int shift) {
        int kept = (int) (significant >>> shift);
        long halfway = 1L << (shift - 1);
        long discarded = significant & ((halfway << 1) - 1);
        return discarded > halfway || (discarded == halfway && (kept & 1) != 0) ? kept + 1 : kept;
    }

    /**
     * Decodes the low 16 bits as a double with exactly representable finite values.
     * <p>
     * 하위 16비트를 double로 해석한다. 유한 값은 정확히 표현된다.
     *
     * @param encoded binary16 bits / binary16 비트
     * @return the binary64 value / binary64 값
     */
    public static double decode(int encoded) {
        int exponent = (encoded >>> 10) & 0x1f;
        int fraction = encoded & 0x3ff;
        if (exponent == 0) {
            double value = Math.scalb((double) fraction, -24);
            return (encoded & 0x8000) == 0 ? value : -value;
        }
        long sign = (long) (encoded & 0x8000) << 48;
        long doubleExponent = exponent == 0x1f ? 0x7ffL : exponent + 1008L;
        return Double.longBitsToDouble(sign | (doubleExponent << 52) | ((long) fraction << 42));
    }
}
