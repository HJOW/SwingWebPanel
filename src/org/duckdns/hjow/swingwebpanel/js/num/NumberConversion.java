package org.duckdns.hjow.swingwebpanel.js.num;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import org.duckdns.hjow.swingwebpanel.js.text.CharClass;

/**
 * ECMAScript 2015 number conversions on plain Java doubles: Number::toString (radix 10 and other radixes),
 * Number.prototype.toFixed / toExponential / toPrecision, StringToNumber, parseInt, parseFloat and the integer
 * conversion abstract operations ToInteger, ToInt32, ToUint32, ToUint16 and ToLength.
 * <p>
 * Assumptions (the ECMA-262 text could not be fetched, so these follow the ES2015 algorithms from knowledge):
 * toString radix other than 10 follows the V8 algorithm (the spec leaves it implementation dependent);
 * toFixed checks the digit range before NaN while toExponential/toPrecision check NaN and Infinity first;
 * ties in toFixed/toExponential/toPrecision choose the larger n (round half up on the magnitude).
 * Methods that the spec defines as RangeError throw {@link NumberRangeException}. All methods are static, stateless
 * and thread-safe.
 * <p>
 * 순수 Java double 위의 ECMAScript 2015 숫자 변환: Number::toString(10진 및 다른 진법), Number.prototype.toFixed / toExponential /
 * toPrecision, StringToNumber, parseInt, parseFloat, 정수 변환 추상 연산 ToInteger, ToInt32, ToUint32, ToUint16, ToLength.
 * <p>
 * 가정(ECMA-262 본문을 조회하지 못해 ES2015 알고리즘에 대한 지식을 따랐다): 10진이 아닌 진법의 toString 은 V8 알고리즘을 따른다(규격은 구현 정의).
 * toFixed 는 자릿수 범위를 NaN 보다 먼저 검사하고, toExponential/toPrecision 은 NaN 과 Infinity 를 먼저 검사한다.
 * toFixed/toExponential/toPrecision 의 동점은 큰 n 을 고른다(크기에 대한 반올림). 규격이 RangeError 로 정한 경우
 * {@link NumberRangeException} 을 던진다. 모든 메서드는 정적이고 상태가 없으며 스레드 안전하다.
 */
public final class NumberConversion {

    /**
     * Characters used for digits in radix conversions.
     * <p>
     * 진법 변환에 쓰는 숫자 문자.
     */
    private static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz";

    /**
     * 2^32 as a double, used for modular integer conversions.
     * <p>
     * 모듈러 정수 변환에 쓰는 2^32 (double).
     */
    private static final double TWO_32 = 4294967296.0;

    /**
     * 2^53 - 1, the largest safe integer.
     * <p>
     * 안전한 최대 정수 2^53 - 1.
     */
    public static final double MAX_SAFE_INTEGER = 9007199254740991.0;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private NumberConversion() {
    }

    // ------------------------------------------------------------------ 정수 변환 (integer conversions)

    /**
     * ToInteger: NaN becomes +0, infinities stay, other values are truncated toward zero (never returns -0).
     * <p>
     * ToInteger: NaN 은 +0, 무한대는 그대로, 나머지는 0 쪽으로 버림한다(-0 은 반환하지 않는다).
     *
     * @param d value / 값
     * @return integral value / 정수 값
     */
    public static double toInteger(double d) {
        if (d != d) {
            return 0.0;
        }
        if (Double.isInfinite(d)) {
            return d;
        }
        double t = d < 0 ? Math.ceil(d) : Math.floor(d);
        return t + 0.0;
    }

    /**
     * ToInt32: modulo 2^32 mapped into the signed 32-bit range.
     * <p>
     * ToInt32: 2^32 로 나눈 나머지를 부호 있는 32비트 범위로 대응시킨다.
     *
     * @param d value / 값
     * @return int result / int 결과
     */
    public static int toInt32(double d) {
        if (d != d || Double.isInfinite(d)) {
            return 0;
        }
        double t = d < 0 ? Math.ceil(d) : Math.floor(d);
        double m = t % TWO_32;
        return (int) (long) m;
    }

    /**
     * ToUint32: modulo 2^32 as a non-negative long.
     * <p>
     * ToUint32: 2^32 로 나눈 나머지를 음이 아닌 long 으로 반환한다.
     *
     * @param d value / 값
     * @return 0 to 4294967295 / 0 이상 4294967295 이하
     */
    public static long toUint32(double d) {
        return ((long) toInt32(d)) & 0xFFFFFFFFL;
    }

    /**
     * ToUint16: modulo 2^16 as a non-negative int.
     * <p>
     * ToUint16: 2^16 로 나눈 나머지를 음이 아닌 int 로 반환한다.
     *
     * @param d value / 값
     * @return 0 to 65535 / 0 이상 65535 이하
     */
    public static int toUint16(double d) {
        return toInt32(d) & 0xFFFF;
    }

    /**
     * ToLength: clamps ToInteger(d) into 0..2^53-1.
     * <p>
     * ToLength: ToInteger(d) 를 0..2^53-1 범위로 제한한다.
     *
     * @param d value / 값
     * @return length value / 길이 값
     */
    public static double toLength(double d) {
        double len = toInteger(d);
        if (len <= 0) {
            return 0.0;
        }
        return Math.min(len, MAX_SAFE_INTEGER);
    }

    /**
     * Number.isInteger: finite and equal to its truncation.
     * <p>
     * Number.isInteger: 유한하고 버림한 값과 같은지 검사한다.
     *
     * @param d value / 값
     * @return true if an integral number / 정수이면 true
     */
    public static boolean isInteger(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d) && (d < 0 ? Math.ceil(d) : Math.floor(d)) == d;
    }

    /**
     * Number.isSafeInteger: an integer whose magnitude is at most 2^53-1.
     * <p>
     * Number.isSafeInteger: 크기가 2^53-1 이하인 정수인지 검사한다.
     *
     * @param d value / 값
     * @return true if a safe integer / 안전한 정수이면 true
     */
    public static boolean isSafeInteger(double d) {
        return isInteger(d) && Math.abs(d) <= MAX_SAFE_INTEGER;
    }

    /**
     * SameValue: like === but NaN equals NaN and +0 differs from -0.
     * <p>
     * SameValue: === 와 비슷하지만 NaN 은 NaN 과 같고 +0 과 -0 은 다르다.
     *
     * @param a first value / 첫 값
     * @param b second value / 둘째 값
     * @return true if the same value / 같은 값이면 true
     */
    public static boolean sameValue(double a, double b) {
        if (a != a) {
            return b != b;
        }
        if (a == 0 && b == 0) {
            return (1.0 / a) == (1.0 / b);
        }
        return a == b;
    }

    /**
     * SameValueZero: like SameValue but +0 equals -0.
     * <p>
     * SameValueZero: SameValue 와 같지만 +0 과 -0 을 같게 본다.
     *
     * @param a first value / 첫 값
     * @param b second value / 둘째 값
     * @return true if equal under SameValueZero / SameValueZero 로 같으면 true
     */
    public static boolean sameValueZero(double a, double b) {
        if (a != a) {
            return b != b;
        }
        return a == b;
    }

    // ------------------------------------------------------------------ Number::toString

    /**
     * Number::toString(x) for radix 10: shortest round-trip digits formatted per ECMA-262 7.1.12.1.
     * <p>
     * 10진 Number::toString(x): ECMA-262 7.1.12.1 에 따른 가장 짧은 왕복 가능 숫자 형식.
     *
     * @param v number / 숫자
     * @return string form / 문자열 형태
     */
    public static String toString(double v) {
        if (v != v) {
            return "NaN";
        }
        if (v == 0.0) {
            return "0";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "Infinity" : "-Infinity";
        }
        if (v < 0) {
            return "-" + toString(-v);
        }
        DecimalDigits dd = DecimalDigits.shortest(v);
        return formatDecimal(dd.getDigits(), dd.getPointPosition());
    }

    /**
     * Formats digits d1..dk with decimal point position n using the Number::toString layout rules
     * (plain notation for 1e-7 &lt; v &lt; 1e21, exponent notation otherwise).
     * <p>
     * 숫자열 d1..dk 와 소수점 위치 n 을 Number::toString 배치 규칙(1e-7 &lt; v &lt; 1e21 은 일반 표기, 그 밖은 지수 표기)으로 만든다.
     *
     * @param digits digit string / 숫자열
     * @param n      decimal point position / 소수점 위치
     * @return formatted text of a positive value / 양수 값의 문자열
     */
    private static String formatDecimal(String digits, int n) {
        int k = digits.length();
        if (k <= n && n <= 21) {
            return digits + zeros(n - k);
        }
        if (0 < n && n <= 21) {
            return digits.substring(0, n) + "." + digits.substring(n);
        }
        if (-6 < n && n <= 0) {
            return "0." + zeros(-n) + digits;
        }
        int e = n - 1;
        String exp = "e" + (e < 0 ? "-" : "+") + Math.abs(e);
        if (k == 1) {
            return digits + exp;
        }
        return digits.charAt(0) + "." + digits.substring(1) + exp;
    }

    /**
     * Returns a string of n zeros.
     * <p>
     * 0 이 n 개인 문자열을 반환한다.
     *
     * @param n count (may be 0) / 개수(0 가능)
     * @return zeros / 0 문자열
     */
    private static String zeros(int n) {
        StringBuilder sb = new StringBuilder(n);
        // n 번 0 을 붙인다.
        for (int i = 0; i < n; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    /**
     * Number.prototype.toString(radix) for radix 2..36. Radix 10 is the shortest round-trip form; other radixes use
     * the V8 algorithm that emits as many fraction digits as are needed to identify the double.
     * <p>
     * Number.prototype.toString(radix) (진법 2..36). 10진은 가장 짧은 왕복 형태이고, 다른 진법은 double 을 식별하는 데 필요한 만큼의 소수 자릿수를
     * 내는 V8 알고리즘을 쓴다.
     *
     * @param v     number / 숫자
     * @param radix radix (already ToInteger'd) / 진법(ToInteger 를 거친 값)
     * @return string form / 문자열 형태
     * @throws NumberRangeException if the radix is outside 2..36 / 진법이 2..36 밖이면
     */
    public static String toString(double v, double radix) {
        double r = toInteger(radix);
        if (r < 2 || r > 36) {
            throw new NumberRangeException("toString() radix must be between 2 and 36");
        }
        int rad = (int) r;
        if (rad == 10) {
            return toString(v);
        }
        if (v != v) {
            return "NaN";
        }
        if (v == 0.0) {
            return "0";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "Infinity" : "-Infinity";
        }
        if (v < 0) {
            return "-" + toRadixPositive(-v, rad);
        }
        return toRadixPositive(v, rad);
    }

    /**
     * Converts a positive finite value to a radix string (port of the V8 DoubleToRadixCString algorithm).
     * <p>
     * 양의 유한 값을 진법 문자열로 바꾼다(V8 DoubleToRadixCString 알고리즘을 옮김).
     *
     * @param value positive finite value / 양의 유한 값
     * @param radix 2..36 except 10 / 10 을 제외한 2..36
     * @return radix string / 진법 문자열
     */
    private static String toRadixPositive(double value, int radix) {
        double integer = Math.floor(value);
        double fraction = value - integer;
        double delta = 0.5 * (Math.nextUp(value) - value);
        delta = Math.max(Double.MIN_VALUE, delta);
        char[] frac = new char[1100];
        int fc = 0;
        if (fraction >= delta) {
            // 소수 부분은 한 자리씩 만들고, 정밀도 오차(delta) 안에 들어오면 반올림하며 멈춘다.
            do {
                fraction *= radix;
                delta *= radix;
                int digit = (int) fraction;
                frac[fc++] = DIGITS.charAt(digit);
                fraction -= digit;
                if (fraction > 0.5 || (fraction == 0.5 && (digit & 1) != 0)) {
                    if (fraction + delta > 1) {
                        // 올림하면서 자리 올림을 앞 자리로 전파한다.
                        while (true) {
                            fc--;
                            if (fc < 0) {
                                fc = 0;
                                integer += 1;
                                break;
                            }
                            int dg = DIGITS.indexOf(frac[fc]);
                            if (dg + 1 < radix) {
                                frac[fc++] = DIGITS.charAt(dg + 1);
                                break;
                            }
                        }
                        break;
                    }
                }
            } while (fraction >= delta && fc < frac.length - 1);
        }
        StringBuilder rev = new StringBuilder();
        // 2^53 이상인 정수는 정확한 자릿수를 알 수 없으므로 0 을 붙여 나눈다(V8 과 같은 방식).
        while (Math.getExponent(integer) - 52 > 0) {
            integer /= radix;
            rev.append('0');
        }
        // 정수 부분은 나머지를 구해 낮은 자리부터 숫자를 만든다.
        do {
            double remainder = integer % radix;
            rev.append(DIGITS.charAt((int) remainder));
            integer = (integer - remainder) / radix;
        } while (integer > 0);
        StringBuilder out = rev.reverse();
        if (fc > 0) {
            out.append('.').append(frac, 0, fc);
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ toFixed / toExponential / toPrecision

    /**
     * Number.prototype.toFixed(fractionDigits).
     * <p>
     * Number.prototype.toFixed(fractionDigits).
     *
     * @param x              number / 숫자
     * @param fractionDigits number of fraction digits 0..20 (ToInteger is applied) / 소수 자릿수 0..20(ToInteger 적용)
     * @return fixed-point text / 고정 소수점 문자열
     * @throws NumberRangeException if fractionDigits is outside 0..20 / 자릿수가 0..20 밖이면
     */
    public static String toFixed(double x, double fractionDigits) {
        double f = toInteger(fractionDigits);
        if (f < 0 || f > 20) {
            throw new NumberRangeException("toFixed() digits argument must be between 0 and 20");
        }
        if (x != x) {
            return "NaN";
        }
        if (Math.abs(x) >= 1e21) {
            return toString(x);
        }
        BigDecimal bd = new BigDecimal(Math.abs(x)).setScale((int) f, RoundingMode.HALF_UP);
        String text = bd.toPlainString();
        return x < 0 ? "-" + text : text;
    }

    /**
     * Number.prototype.toExponential() with the argument undefined: as many digits as necessary (shortest).
     * <p>
     * 인수가 undefined 인 Number.prototype.toExponential(): 필요한 만큼의 자릿수(가장 짧은 표현).
     *
     * @param x number / 숫자
     * @return exponential text / 지수 표기 문자열
     */
    public static String toExponential(double x) {
        if (x != x) {
            return "NaN";
        }
        String sign = "";
        if (x < 0) {
            sign = "-";
            x = -x;
        }
        if (Double.isInfinite(x)) {
            return sign + "Infinity";
        }
        if (x == 0.0) {
            return "0e+0";
        }
        DecimalDigits dd = DecimalDigits.shortest(x);
        return sign + exponentialText(dd.getDigits(), dd.getPointPosition() - 1);
    }

    /**
     * Number.prototype.toExponential(fractionDigits).
     * <p>
     * Number.prototype.toExponential(fractionDigits).
     *
     * @param x              number / 숫자
     * @param fractionDigits fraction digits 0..20 (ToInteger is applied) / 소수 자릿수 0..20(ToInteger 적용)
     * @return exponential text / 지수 표기 문자열
     * @throws NumberRangeException if fractionDigits is outside 0..20 / 자릿수가 0..20 밖이면
     */
    public static String toExponential(double x, double fractionDigits) {
        double f = toInteger(fractionDigits);
        if (x != x) {
            return "NaN";
        }
        String sign = "";
        if (x < 0) {
            sign = "-";
            x = -x;
        }
        if (Double.isInfinite(x)) {
            return sign + "Infinity";
        }
        if (f < 0 || f > 20) {
            throw new NumberRangeException("toExponential() argument must be between 0 and 20");
        }
        int p = (int) f + 1;
        if (x == 0.0) {
            return sign + exponentialText(zeros(p), 0);
        }
        Rounded r = round(x, p);
        return sign + exponentialText(r.digits, r.exponent);
    }

    /**
     * Number.prototype.toPrecision() with the argument undefined: same as ToString.
     * <p>
     * 인수가 undefined 인 Number.prototype.toPrecision(): ToString 과 같다.
     *
     * @param x number / 숫자
     * @return string form / 문자열 형태
     */
    public static String toPrecision(double x) {
        return toString(x);
    }

    /**
     * Number.prototype.toPrecision(precision).
     * <p>
     * Number.prototype.toPrecision(precision).
     *
     * @param x         number / 숫자
     * @param precision significant digits 1..21 (ToInteger is applied) / 유효 숫자 수 1..21(ToInteger 적용)
     * @return text in fixed or exponential notation / 고정 또는 지수 표기 문자열
     * @throws NumberRangeException if precision is outside 1..21 / 정밀도가 1..21 밖이면
     */
    public static String toPrecision(double x, double precision) {
        double pd = toInteger(precision);
        if (x != x) {
            return "NaN";
        }
        String sign = "";
        if (x < 0) {
            sign = "-";
            x = -x;
        }
        if (Double.isInfinite(x)) {
            return sign + "Infinity";
        }
        if (pd < 1 || pd > 21) {
            throw new NumberRangeException("toPrecision() argument must be between 1 and 21");
        }
        int p = (int) pd;
        String m;
        int e;
        if (x == 0.0) {
            m = zeros(p);
            e = 0;
        } else {
            Rounded r = round(x, p);
            m = r.digits;
            e = r.exponent;
        }
        // 지수가 너무 작거나 정밀도 이상이면 지수 표기를 쓴다.
        if (e < -6 || e >= p) {
            return sign + exponentialText(m, e);
        }
        if (e == p - 1) {
            return sign + m;
        }
        if (e >= 0) {
            return sign + m.substring(0, e + 1) + "." + m.substring(e + 1);
        }
        return sign + "0." + zeros(-(e + 1)) + m;
    }

    /**
     * Formats digits and exponent as <code>d.ddde+x</code>.
     * <p>
     * 숫자열과 지수를 <code>d.ddde+x</code> 형태로 만든다.
     *
     * @param digits digits (at least one) / 숫자열(1자리 이상)
     * @param e      decimal exponent of the first digit / 첫 자리의 10진 지수
     * @return exponential text / 지수 표기 문자열
     */
    private static String exponentialText(String digits, int e) {
        String exp = "e" + (e < 0 ? "-" : "+") + Math.abs(e);
        if (digits.length() == 1) {
            return digits + exp;
        }
        return digits.charAt(0) + "." + digits.substring(1) + exp;
    }

    /**
     * Result of rounding to a number of significant digits.
     * <p>
     * 유효 숫자 수로 반올림한 결과.
     */
    private static final class Rounded {
        /**
         * Exactly the requested number of digits.
         * <p>
         * 요청한 개수의 숫자열.
         */
        String digits;
        /**
         * Decimal exponent of the first digit.
         * <p>
         * 첫 숫자의 10진 지수.
         */
        int exponent;
    }

    /**
     * Rounds a positive finite value to p significant digits, ties toward the larger magnitude.
     * <p>
     * 양의 유한 값을 p 개의 유효 숫자로 반올림한다(동점은 크기가 큰 쪽).
     *
     * @param x positive finite value / 양의 유한 값
     * @param p digits 1..21 / 숫자 수 1..21
     * @return digits and exponent / 숫자열과 지수
     */
    private static Rounded round(double x, int p) {
        BigDecimal r = new BigDecimal(x).round(new MathContext(p, RoundingMode.HALF_UP));
        String u = r.unscaledValue().abs().toString();
        Rounded out = new Rounded();
        out.exponent = u.length() - 1 - r.scale();
        StringBuilder sb = new StringBuilder(u);
        // 자릿수가 모자라면 오른쪽에 0 을 채운다.
        while (sb.length() < p) {
            sb.append('0');
        }
        out.digits = sb.toString();
        return out;
    }

    // ------------------------------------------------------------------ StringToNumber / parseInt / parseFloat

    /**
     * StringToNumber: converts a string to a number with the StrNumericLiteral grammar (surrounding white space and
     * line terminators are ignored; empty becomes 0; decimal, Infinity, 0x, 0o and 0b literals; otherwise NaN).
     * Results are correctly rounded.
     * <p>
     * StringToNumber: 문자열을 StrNumericLiteral 문법으로 숫자로 바꾼다(앞뒤 공백/줄 끝 문자 무시, 빈 문자열은 0, 10진/Infinity/0x/0o/0b, 그 밖은 NaN).
     * 결과는 올바르게 반올림된다.
     *
     * @param s string / 문자열
     * @return number (NaN if invalid) / 숫자(올바르지 않으면 NaN)
     */
    public static double stringToNumber(String s) {
        int b = 0;
        int e = s.length();
        // 앞뒤의 공백과 줄 끝 문자를 걷어낸다.
        while (b < e && CharClass.isWhiteSpaceOrLineTerminator(s.charAt(b))) {
            b++;
        }
        // 뒤쪽의 공백과 줄 끝 문자를 걷어낸다.
        while (e > b && CharClass.isWhiteSpaceOrLineTerminator(s.charAt(e - 1))) {
            e--;
        }
        if (b == e) {
            return 0.0;
        }
        if (e - b > 2 && s.charAt(b) == '0') {
            char c = s.charAt(b + 1);
            int radix = (c == 'x' || c == 'X') ? 16 : (c == 'o' || c == 'O') ? 8 : (c == 'b' || c == 'B') ? 2 : 0;
            if (radix != 0) {
                // 진법 접두사 뒤에는 부호 없는 해당 진법 숫자만 올 수 있다.
                for (int i = b + 2; i < e; i++) {
                    if (CharClass.digitValue(s.charAt(i), radix) < 0) {
                        return Double.NaN;
                    }
                }
                return ExactDecimal.ratioToDouble(new BigInteger(s.substring(b + 2, e), radix), BigInteger.ONE);
            }
        }
        int end = scanDecimalLiteral(s, b, e);
        if (end != e) {
            return Double.NaN;
        }
        return decimalLiteralToDouble(s, b, e);
    }

    /**
     * parseFloat: parses the longest prefix that is a StrDecimalLiteral after skipping leading white space.
     * <p>
     * parseFloat: 앞의 공백을 건너뛴 뒤 StrDecimalLiteral 인 가장 긴 접두사를 해석한다.
     *
     * @param s string / 문자열
     * @return number, or NaN if no prefix is valid / 숫자. 올바른 접두사가 없으면 NaN
     */
    public static double parseFloat(String s) {
        int b = 0;
        int n = s.length();
        // 앞쪽의 공백과 줄 끝 문자를 건너뛴다.
        while (b < n && CharClass.isWhiteSpaceOrLineTerminator(s.charAt(b))) {
            b++;
        }
        int end = scanDecimalLiteral(s, b, n);
        if (end < 0) {
            return Double.NaN;
        }
        return decimalLiteralToDouble(s, b, end);
    }

    /**
     * parseInt(string, radix): the radix has already been converted with ToInt32 (0 means "not given").
     * Results are exact for any digit count (correctly rounded).
     * <p>
     * parseInt(string, radix): 진법은 이미 ToInt32 를 거친 값이다(0 은 "지정하지 않음"). 자릿수와 무관하게 정확하다(올바르게 반올림).
     *
     * @param s     string / 문자열
     * @param radix radix after ToInt32 / ToInt32 를 거친 진법
     * @return number or NaN / 숫자 또는 NaN
     */
    public static double parseInt(String s, int radix) {
        int i = 0;
        int n = s.length();
        // 앞쪽의 공백과 줄 끝 문자를 건너뛴다.
        while (i < n && CharClass.isWhiteSpaceOrLineTerminator(s.charAt(i))) {
            i++;
        }
        boolean neg = false;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            neg = s.charAt(i) == '-';
            i++;
        }
        int r = radix;
        boolean strip = true;
        if (r != 0) {
            if (r < 2 || r > 36) {
                return Double.NaN;
            }
            if (r != 16) {
                strip = false;
            }
        } else {
            r = 10;
        }
        if (strip && i + 1 < n && s.charAt(i) == '0' && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
            i += 2;
            r = 16;
        }
        int start = i;
        // 진법에 맞는 숫자가 이어지는 동안 읽는다.
        while (i < n && CharClass.digitValue(s.charAt(i), r) >= 0) {
            i++;
        }
        if (i == start) {
            return Double.NaN;
        }
        double v = ExactDecimal.ratioToDouble(new BigInteger(s.substring(start, i), r), BigInteger.ONE);
        return neg ? -v : v;
    }

    /**
     * Scans the longest StrDecimalLiteral that starts at <code>from</code> (sign, Infinity, digits, fraction, exponent).
     * <p>
     * <code>from</code> 에서 시작하는 가장 긴 StrDecimalLiteral(부호, Infinity, 숫자, 소수, 지수)을 훑는다.
     *
     * @param s    text / 문자열
     * @param from start index / 시작 위치
     * @param to   end limit (exclusive) / 끝 한계(미포함)
     * @return index just after the literal, or -1 if there is none / 리터럴 바로 다음 위치. 없으면 -1
     */
    private static int scanDecimalLiteral(String s, int from, int to) {
        int i = from;
        if (i < to && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        if (s.startsWith("Infinity", i) && i + 8 <= to) {
            return i + 8;
        }
        int intDigits = 0;
        // 정수부 숫자가 이어지는 동안 센다.
        while (i < to && CharClass.isDecimalDigit(s.charAt(i))) {
            i++;
            intDigits++;
        }
        int fracDigits = 0;
        if (i < to && s.charAt(i) == '.') {
            int j = i + 1;
            // 소수부 숫자가 이어지는 동안 센다.
            while (j < to && CharClass.isDecimalDigit(s.charAt(j))) {
                j++;
                fracDigits++;
            }
            // 정수부와 소수부 어느 쪽이든 숫자가 있어야 '.' 을 소비한다.
            if (intDigits > 0 || fracDigits > 0) {
                i = j;
            }
        }
        if (intDigits == 0 && fracDigits == 0) {
            return -1;
        }
        if (i < to && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < to && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            int expDigits = 0;
            // 지수 부분의 숫자가 이어지는 동안 센다.
            while (j < to && CharClass.isDecimalDigit(s.charAt(j))) {
                j++;
                expDigits++;
            }
            if (expDigits > 0) {
                i = j;
            }
        }
        return i;
    }

    /**
     * Converts a decimal literal already validated by {@link #scanDecimalLiteral(String, int, int)} to a double.
     * <p>
     * {@link #scanDecimalLiteral(String, int, int)} 로 검증한 10진 리터럴을 double 로 바꾼다.
     *
     * @param s    text / 문자열
     * @param from literal start / 리터럴 시작
     * @param to   literal end (exclusive) / 리터럴 끝(미포함)
     * @return the value / 값
     */
    private static double decimalLiteralToDouble(String s, int from, int to) {
        int i = from;
        boolean neg = false;
        if (s.charAt(i) == '+' || s.charAt(i) == '-') {
            neg = s.charAt(i) == '-';
            i++;
        }
        if (s.startsWith("Infinity", i)) {
            return neg ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        StringBuilder digits = new StringBuilder();
        long exp10 = 0;
        // 정수부 숫자를 가수에 이어 붙인다.
        while (i < to && CharClass.isDecimalDigit(s.charAt(i))) {
            digits.append(s.charAt(i));
            i++;
        }
        if (i < to && s.charAt(i) == '.') {
            i++;
            // 소수 자리는 가수에 이어 붙이고 지수를 자리 수만큼 낮춘다.
            while (i < to && CharClass.isDecimalDigit(s.charAt(i))) {
                digits.append(s.charAt(i));
                exp10--;
                i++;
            }
        }
        if (i < to && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            i++;
            boolean eneg = false;
            if (s.charAt(i) == '+' || s.charAt(i) == '-') {
                eneg = s.charAt(i) == '-';
                i++;
            }
            long ev = 0;
            while (i < to && CharClass.isDecimalDigit(s.charAt(i))) {
                // 지수가 너무 크면 결과가 0 이나 Infinity 로 정해지므로 값을 제한한다.
                if (ev < 100000000L) {
                    ev = ev * 10 + (s.charAt(i) - '0');
                }
                i++;
            }
            exp10 += eneg ? -ev : ev;
        }
        // 뒤쪽 0 은 지수로 옮겨 큰 수 계산을 줄인다.
        int len = digits.length();
        while (len > 1 && digits.charAt(len - 1) == '0') {
            len--;
            exp10++;
        }
        double v = ExactDecimal.toDouble(new BigInteger(digits.substring(0, len)), exp10);
        return neg ? -v : v;
    }
}
