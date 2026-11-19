package org.duckdns.hjow.swingwebpanel.js.num;

import java.math.BigInteger;

/**
 * Exact (correctly rounded, ties-to-even) conversion from arbitrary precision decimal or rational numbers to
 * IEEE-754 double. It exists because Java 6 <code>Double.parseDouble</code> is known to misround some inputs.
 * All methods are static and thread-safe.
 * <p>
 * 임의 정밀도 10진수 또는 유리수를 IEEE-754 double 로 정확하게(올바른 반올림, 동점이면 짝수) 변환한다.
 * Java 6 의 <code>Double.parseDouble</code> 이 일부 입력을 잘못 반올림하는 것으로 알려져 있어 직접 구현했다.
 * 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class ExactDecimal {

    /**
     * Decimal exponent above which a non-zero value certainly overflows (digits count + exponent).
     * <p>
     * 0 이 아닌 값이 반드시 넘치는 10진 지수의 경계(자릿수 + 지수).
     */
    private static final int OVERFLOW_DECIMAL_EXPONENT = 311;

    /**
     * Decimal exponent below which a value certainly underflows to zero (digits count + exponent).
     * <p>
     * 값이 반드시 0 으로 사라지는 10진 지수의 경계(자릿수 + 지수).
     */
    private static final int UNDERFLOW_DECIMAL_EXPONENT = -326;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ExactDecimal() {
    }

    /**
     * Converts <code>digits * 10^exponent10</code> to the nearest double (digits must not be negative).
     * <p>
     * <code>digits * 10^exponent10</code> 을 가장 가까운 double 로 변환한다(digits 는 음수가 아니어야 한다).
     *
     * @param digits     non-negative integer significand / 음이 아닌 정수 가수
     * @param exponent10 decimal exponent (may be large) / 10진 지수(커도 됨)
     * @return nearest double, possibly 0 or Infinity / 가장 가까운 double(0 또는 Infinity 일 수 있음)
     */
    public static double toDouble(BigInteger digits, long exponent10) {
        if (digits.signum() == 0) {
            return 0.0;
        }
        // 자릿수와 지수만으로 넘침/사라짐이 확실한 경우는 큰 수 계산을 하지 않는다.
        long magnitude = digitCount(digits) + exponent10;
        if (magnitude > OVERFLOW_DECIMAL_EXPONENT) {
            return Double.POSITIVE_INFINITY;
        }
        if (magnitude < UNDERFLOW_DECIMAL_EXPONENT) {
            return 0.0;
        }
        if (exponent10 >= 0) {
            return ratioToDouble(digits.multiply(BigInteger.TEN.pow((int) exponent10)), BigInteger.ONE);
        }
        return ratioToDouble(digits, BigInteger.TEN.pow((int) -exponent10));
    }

    /**
     * Number of decimal digits of a positive integer.
     * <p>
     * 양의 정수의 10진 자릿수.
     *
     * @param v positive integer / 양의 정수
     * @return digit count / 자릿수
     */
    private static int digitCount(BigInteger v) {
        // bitLength 로 근사한 뒤 한 번 보정한다.
        int approx = (int) (v.bitLength() * 0.30102999566398114) + 1;
        if (BigInteger.TEN.pow(approx - 1).compareTo(v) > 0) {
            return approx - 1;
        }
        if (v.compareTo(BigInteger.TEN.pow(approx)) >= 0) {
            return approx + 1;
        }
        return approx;
    }

    /**
     * Converts the fraction <code>num / den</code> (num &gt;= 0, den &gt; 0) to the nearest double with
     * round-half-even, including the subnormal range and overflow to Infinity.
     * <p>
     * 분수 <code>num / den</code>(num &gt;= 0, den &gt; 0)을 동점이면 짝수로 반올림하여 가장 가까운 double 로 변환한다.
     * 비정규 수 범위와 Infinity 로의 넘침도 처리한다.
     *
     * @param num numerator / 분자
     * @param den denominator / 분모
     * @return nearest double / 가장 가까운 double
     */
    public static double ratioToDouble(BigInteger num, BigInteger den) {
        if (num.signum() == 0) {
            return 0.0;
        }
        // 몫이 56~57 비트가 되도록 크기를 맞춘다.
        int a = num.bitLength() - den.bitLength();
        int s = 56 - a;
        BigInteger n = s >= 0 ? num.shiftLeft(s) : num;
        BigInteger d = s >= 0 ? den : den.shiftLeft(-s);
        BigInteger[] qr = n.divideAndRemainder(d);
        long q = qr[0].longValue();
        boolean sticky = qr[1].signum() != 0;
        int len = 64 - Long.numberOfLeadingZeros(q);
        int e = len - 1 - s;
        int keep = e >= -1022 ? 53 : e + 1075;
        int drop = len - keep;
        if (drop >= 64) {
            return 0.0;
        }
        long m = q >>> drop;
        long rem = q & ((1L << drop) - 1);
        long half = 1L << (drop - 1);
        // 나머지가 절반보다 크면 올림, 정확히 절반이면 이후 비트(sticky) 또는 홀짝으로 결정한다.
        boolean up = rem > half || (rem == half && (sticky || (m & 1L) == 1L));
        if (up) {
            m++;
        }
        return Math.scalb((double) m, drop - s);
    }
}
