package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.js.num.NumberConversion;

/**
 * Number to string and string to number conversion used by the engine core. It is a thin facade over the exact algorithms of
 * <code>org.duckdns.hjow.swingwebpanel.js.num.NumberConversion</code> (shortest round-trip digits, radix conversion, correctly rounded
 * parsing), so the core never calls the JDK number formatting directly and
 * <code>Number.MIN_VALUE</code> prints as <code>5e-324</code>.
 * <p>
 * 엔진 코어가 쓰는 숫자-문자열 변환. <code>org.duckdns.hjow.swingwebpanel.js.num.NumberConversion</code> 의 정확한 알고리즘(최단 왕복 자릿수, 진법 변환,
 * 올바르게 반올림하는 파싱) 위에 얹은 얇은 외관이며, 그래서 코어는 JDK 숫자 서식을 직접 호출하지 않고
 * <code>Number.MIN_VALUE</code> 는 <code>5e-324</code> 로 출력된다.
 */
public final class NumberText {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private NumberText() {
    }

    /**
     * Number::toString(x) with radix 10 (ECMA-262 6th edition 7.1.12.1).
     * <p>
     * 10진수 Number::toString(x) (ECMA-262 6판 7.1.12.1).
     *
     * @param d the number / 숫자
     * @return the string / 문자열
     */
    public static String toString(double d) {
        return NumberConversion.toString(d);
    }

    /**
     * Number.prototype.toString(radix) for a radix of 2 to 36 (other values are the caller's responsibility to reject).
     * <p>
     * 진법 2~36 의 Number.prototype.toString(radix) (그 밖의 값을 거부하는 일은 호출자의 몫이다).
     *
     * @param d     the number / 숫자
     * @param radix the radix, 2 to 36 / 진법. 2~36
     * @return the string / 문자열
     */
    public static String toString(double d, int radix) {
        return NumberConversion.toString(d, radix);
    }

    /**
     * StringToNumber (ECMA-262 6th edition 7.1.3.1): trims white space, accepts decimal literals, Infinity,
     * and 0x / 0o / 0b integer literals; returns NaN for anything else. An empty or all-space string is 0.
     * <p>
     * StringToNumber (ECMA-262 6판 7.1.3.1): 공백을 제거하고 10진 리터럴, Infinity, 0x / 0o / 0b 정수 리터럴을 받아들이며
     * 그 밖은 NaN 을 반환한다. 빈 문자열이나 공백뿐인 문자열은 0.
     *
     * @param str the string / 문자열
     * @return the number / 숫자
     */
    public static double parse(String str) {
        return NumberConversion.stringToNumber(str);
    }
}
