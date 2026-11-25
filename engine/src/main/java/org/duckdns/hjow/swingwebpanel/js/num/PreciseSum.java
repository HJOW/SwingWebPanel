package org.duckdns.hjow.swingwebpanel.js.num;

import java.math.BigInteger;

/**
 * Sums binary64 values without intermediate rounding.
 * Add numbers with {@link #add(double)} and obtain the rounded result with {@link #value()}.
 * <p>
 * 중간 반올림 없이 binary64 값을 합산한다.
 * {@link #add(double)}로 수를 더하고 {@link #value()}로 반올림한 결과를 얻는다.
 */
public final class PreciseSum {

    /** Number of stored fraction bits. / 저장한 가수 비트 수. */
    private static final int FRACTION_BITS = 52;

    /** Implicit leading significand bit of normal numbers. / 정규 수의 암시된 선두 유효 비트. */
    private static final long HIDDEN_BIT = 1L << FRACTION_BITS;

    /** Mask for the stored trailing significand. / 저장한 가수 하위 비트의 마스크. */
    private static final long FRACTION_MASK = HIDDEN_BIT - 1;

    /** Exponent field reserved for infinities and NaN. / 무한대와 NaN에 예약된 지수 필드. */
    private static final int NONFINITE_EXPONENT = 0x7FF;

    /** Exact sum in units of the smallest positive subnormal, 2^-1074. / 최소 양의 비정규 수 2^-1074 단위의 정확한 합. */
    private BigInteger units = BigInteger.ZERO;

    /** Zero while finite, otherwise the combined infinity or NaN. / 유한 상태에서는 0, 그 밖에는 결합한 무한대 또는 NaN. */
    private double nonfinite;

    /** Whether all values so far were negative zero. / 지금까지의 값이 모두 음의 0인지 여부. */
    private boolean onlyNegativeZero = true;

    /** Starts a fresh sum. / 새로운 합계를 시작한다. */
    public PreciseSum() {
    }

    /**
     * Adds a binary64 value, keeping finite inputs exact.
     * <p>
     * binary64 값을 더하며 유한 입력은 정확하게 유지한다.
     *
     * @param number value to add / 더할 값
     */
    public void add(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            // 같은 방향의 무한대는 유지하고 반대 방향의 무한대나 NaN을 만나면 NaN으로 바뀐다.
            nonfinite += number;
            return;
        }
        if (nonfinite != 0.0) {
            return;
        }
        long bits = Double.doubleToRawLongBits(number);
        if (number == 0.0) {
            onlyNegativeZero &= bits != 0;
            return;
        }
        onlyNegativeZero = false;
        int exponent = (int) ((bits >>> FRACTION_BITS) & NONFINITE_EXPONENT);
        long significand = bits & FRACTION_MASK;
        if (exponent != 0) {
            significand |= HIDDEN_BIT;
        }
        BigInteger signed = BigInteger.valueOf(bits < 0 ? -significand : significand);
        units = units.add(signed.shiftLeft(Math.max(0, exponent - 1)));
    }

    /**
     * Returns the sum rounded once to nearest binary64, with ties rounded to even.
     * <p>
     * 가장 가까운 binary64 값으로 한 번만 반올림한 합을 반환한다. 중간값은 짝수로 반올림한다.
     *
     * @return current sum / 현재 합
     */
    public double value() {
        if (nonfinite != 0.0) {
            return nonfinite;
        }
        if (onlyNegativeZero) {
            return -0.0;
        }
        BigInteger magnitude = units.abs();
        long sign = units.signum() < 0 ? Long.MIN_VALUE : 0L;
        int shift = magnitude.bitLength() - (FRACTION_BITS + 1);
        if (shift <= 0) {
            // 비정규 수와 가장 작은 지수의 정규 수는 정수의 절댓값이 그대로 비트열이다.
            return Double.longBitsToDouble(sign | magnitude.longValue());
        }
        long retained = magnitude.shiftRight(shift).longValue();
        int halfwayBit = shift - 1;
        if (magnitude.testBit(halfwayBit) && (magnitude.getLowestSetBit() < halfwayBit || (retained & 1L) != 0)) {
            retained++;
            if (retained == (HIDDEN_BIT << 1)) {
                // 반올림 올림으로 자리수가 늘면 다음 지수로 정규화한다.
                retained >>>= 1;
                shift++;
            }
        }
        int exponent = shift + 1;
        if (exponent >= NONFINITE_EXPONENT) {
            return sign == 0L ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        return Double.longBitsToDouble(sign | ((long) exponent << FRACTION_BITS) | (retained & FRACTION_MASK));
    }
}
