package org.duckdns.hjow.swingwebpanel.js.num;

import java.math.BigInteger;

/**
 * The shortest decimal digit string that uniquely identifies a positive finite double, as required by the
 * ECMAScript Number::toString algorithm: <code>value = 0.d1d2...dk * 10^pointPosition</code> where k is as small as
 * possible and, among candidates of that length, the one closest to the value (ties: even last digit) is chosen.
 * Computed with exact big integer arithmetic (Burger and Dybvig free-format algorithm); never uses
 * <code>Double.toString</code>, which is not always shortest on Java 6. Instances are immutable.
 * <p>
 * 양의 유한 double 을 유일하게 식별하는 가장 짧은 10진 숫자열. ECMAScript Number::toString 알고리즘이 요구한다:
 * <code>값 = 0.d1d2...dk * 10^pointPosition</code> 이며 k 는 가능한 한 작고, 같은 길이 후보 중 값에 가장 가까운 것(동점이면 끝자리가 짝수)을 고른다.
 * 정확한 큰 정수 연산(Burger-Dybvig 자유 형식 알고리즘)으로 계산하며 Java 6 에서 항상 최단이 아닌 <code>Double.toString</code> 은 사용하지 않는다.
 * 인스턴스는 불변이다.
 */
public final class DecimalDigits {

    /**
     * Decimal digits without leading or trailing zeros (except the single digit "0" is never produced).
     * <p>
     * 앞뒤 0 이 없는 10진 숫자열.
     */
    private final String digits;

    /**
     * Position of the decimal point: the value equals 0.digits * 10^pointPosition (ECMAScript's n).
     * <p>
     * 소수점 위치: 값 = 0.digits * 10^pointPosition (ECMAScript 의 n).
     */
    private final int pointPosition;

    /**
     * Creates an immutable result.
     * <p>
     * 불변 결과를 만든다.
     *
     * @param digits        digit string / 숫자열
     * @param pointPosition decimal point position / 소수점 위치
     */
    private DecimalDigits(String digits, int pointPosition) {
        this.digits = digits;
        this.pointPosition = pointPosition;
    }

    /**
     * Returns the digit string d1..dk.
     * <p>
     * 숫자열 d1..dk 를 반환한다.
     *
     * @return digits / 숫자열
     */
    public String getDigits() {
        return digits;
    }

    /**
     * Returns n such that the value is 0.d1..dk * 10^n.
     * <p>
     * 값이 0.d1..dk * 10^n 이 되는 n 을 반환한다.
     *
     * @return decimal point position / 소수점 위치
     */
    public int getPointPosition() {
        return pointPosition;
    }

    /**
     * Computes the shortest round-trip digits of a positive finite double.
     * <p>
     * 양의 유한 double 의 가장 짧은 왕복 가능 숫자열을 계산한다.
     *
     * @param v positive finite value / 양의 유한 값
     * @return shortest digits / 가장 짧은 숫자열
     * @throws IllegalArgumentException if v is not positive and finite / v 가 양의 유한 값이 아니면
     */
    public static DecimalDigits shortest(double v) {
        if (!(v > 0.0) || Double.isInfinite(v)) {
            throw new IllegalArgumentException("positive finite value required: " + v);
        }
        long bits = Double.doubleToLongBits(v);
        int expBits = (int) ((bits >> 52) & 0x7FF);
        long frac = bits & ((1L << 52) - 1);
        long f;
        int e;
        // 정규 수와 비정규 수의 가수/지수를 구한다.
        if (expBits == 0) {
            f = frac;
            e = -1074;
        } else {
            f = frac | (1L << 52);
            e = expBits - 1075;
        }
        boolean evenMantissa = (f & 1L) == 0;
        boolean lowOk = evenMantissa;
        boolean highOk = evenMantissa;
        BigInteger bf = BigInteger.valueOf(f);
        BigInteger r;
        BigInteger s;
        BigInteger mPlus;
        BigInteger mMinus;
        boolean unequalGap = f == (1L << 52) && e != -1074;
        // 이웃한 double 과의 간격이 위아래로 같은지에 따라 초기값을 정한다.
        if (e >= 0) {
            BigInteger be = BigInteger.ONE.shiftLeft(e);
            if (!unequalGap) {
                r = bf.multiply(be).shiftLeft(1);
                s = BigInteger.valueOf(2);
                mPlus = be;
                mMinus = be;
            } else {
                r = bf.multiply(be).shiftLeft(2);
                s = BigInteger.valueOf(4);
                mPlus = be.shiftLeft(1);
                mMinus = be;
            }
        } else {
            if (!unequalGap) {
                r = bf.shiftLeft(1);
                s = BigInteger.ONE.shiftLeft(-e + 1);
                mPlus = BigInteger.ONE;
                mMinus = BigInteger.ONE;
            } else {
                r = bf.shiftLeft(2);
                s = BigInteger.ONE.shiftLeft(-e + 2);
                mPlus = BigInteger.valueOf(2);
                mMinus = BigInteger.ONE;
            }
        }
        int k = (int) Math.ceil(Math.log10(v) - 1e-10);
        if (k >= 0) {
            s = s.multiply(BigInteger.TEN.pow(k));
        } else {
            BigInteger scale = BigInteger.TEN.pow(-k);
            r = r.multiply(scale);
            mPlus = mPlus.multiply(scale);
            mMinus = mMinus.multiply(scale);
        }
        // 추정한 k 가 작으면 키우고(위쪽 경계가 1 이상), 크면 줄인다.
        while (highOk ? r.add(mPlus).compareTo(s) >= 0 : r.add(mPlus).compareTo(s) > 0) {
            s = s.multiply(BigInteger.TEN);
            k++;
        }
        // 추정한 k 가 크면(위쪽 경계 x10 이 아직 1 미만) 한 자리씩 줄인다.
        while (highOk ? r.add(mPlus).multiply(BigInteger.TEN).compareTo(s) < 0
                : r.add(mPlus).multiply(BigInteger.TEN).compareTo(s) <= 0) {
            r = r.multiply(BigInteger.TEN);
            mPlus = mPlus.multiply(BigInteger.TEN);
            mMinus = mMinus.multiply(BigInteger.TEN);
            k--;
        }
        StringBuilder out = new StringBuilder();
        // 한 자리씩 생성하며 아래/위쪽 경계 안에 들어오면 종료한다.
        while (true) {
            r = r.multiply(BigInteger.TEN);
            mPlus = mPlus.multiply(BigInteger.TEN);
            mMinus = mMinus.multiply(BigInteger.TEN);
            BigInteger[] qr = r.divideAndRemainder(s);
            int d = qr[0].intValue();
            r = qr[1];
            boolean tc1 = lowOk ? r.compareTo(mMinus) <= 0 : r.compareTo(mMinus) < 0;
            int cmpHigh = r.add(mPlus).compareTo(s);
            boolean tc2 = highOk ? cmpHigh >= 0 : cmpHigh > 0;
            if (!tc1 && !tc2) {
                out.append((char) ('0' + d));
                continue;
            }
            int last;
            if (tc1 && !tc2) {
                last = d;
            } else if (!tc1 && tc2) {
                last = d + 1;
            } else {
                int twice = r.shiftLeft(1).compareTo(s);
                if (twice < 0) {
                    last = d;
                } else if (twice > 0) {
                    last = d + 1;
                } else {
                    last = (d % 2 == 0) ? d : d + 1;
                }
            }
            if (last == 10) {
                // 이론상 발생하지 않지만 올림이 자리 올림을 만들면 앞 자리로 전파한다.
                k += carry(out);
                last = 0;
                out.append((char) ('0' + last));
                break;
            }
            out.append((char) ('0' + last));
            break;
        }
        // 끝의 0 은 제거한다(최단 표현이므로 보통 없다).
        int end = out.length();
        while (end > 1 && out.charAt(end - 1) == '0') {
            end--;
        }
        return new DecimalDigits(out.substring(0, end), k);
    }

    /**
     * Propagates a carry into already generated digits.
     * <p>
     * 이미 만든 숫자열에 자리 올림을 전파한다.
     *
     * @param out digits generated so far / 지금까지 만든 숫자열
     * @return 1 if an extra leading digit was added (point position grows), else 0 / 맨 앞에 숫자가 추가되면 1, 아니면 0
     */
    private static int carry(StringBuilder out) {
        int i = out.length() - 1;
        // 뒤에서부터 9 는 0 으로 바꾸고 처음 만나는 9 아닌 자리를 올린다.
        while (i >= 0) {
            char c = out.charAt(i);
            if (c == '9') {
                out.setCharAt(i, '0');
                i--;
            } else {
                out.setCharAt(i, (char) (c + 1));
                return 0;
            }
        }
        out.insert(0, '1');
        return 1;
    }
}
