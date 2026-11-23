package org.duckdns.hjow.swingwebpanel.dom.html;

import java.math.BigDecimal;
import org.duckdns.hjow.swingwebpanel.js.NumberText;

/**
 * The microsyntax of floating-point numbers in form controls (HTML Standard, "valid floating-point number"): strict parsing into an exact
 * {@link BigDecimal} (so that step arithmetic with values such as 0.1 is exact) and serialization. Like Chromium, numbers whose magnitude
 * exceeds the range of a single-precision float are treated as errors.
 * <p>
 * 폼 컨트롤의 부동소수점 수 마이크로 구문(HTML 표준의 "valid floating-point number"): 엄격한 해석으로 정확한 {@link BigDecimal} 을 만들고(0.1 같은 값의
 * step 계산이 정확하도록) 직렬화한다. Chromium 처럼 단정밀도 float 범위를 넘는 수는 오류로 취급한다.
 */
final class NumberSyntax {

    /**
     * The largest magnitude that is accepted (the maximum of a single-precision float, as in Chromium).
     * <p>
     * 허용하는 가장 큰 크기(Chromium 처럼 단정밀도 float 의 최댓값).
     */
    private static final BigDecimal LIMIT = new BigDecimal("3.4028234663852886E38");

    /**
     * The largest exponent that is read before the value is rejected (keeps {@link BigDecimal} small).
     * <p>
     * 값을 거부하기 전에 읽는 가장 큰 지수({@link BigDecimal} 을 작게 유지한다).
     */
    private static final int MAX_EXPONENT = 400;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private NumberSyntax() {
    }

    /**
     * Tells whether a string is a valid floating-point number: an optional minus sign, digits with an optional fraction (or only a fraction)
     * and an optional exponent. No white space, no plus sign.
     * <p>
     * 문자열이 올바른 부동소수점 수인지 알려 준다: 선택적 마이너스, 소수부가 있을 수 있는 숫자(또는 소수부만), 선택적 지수. 공백과 플러스 기호는 안 된다.
     *
     * @param s the text / 문자열
     * @return true if valid / 올바르면 true
     */
    static boolean isValidFloat(String s) {
        int n = s.length();
        int i = 0;
        if (i < n && s.charAt(i) == '-') {
            i++;
        }
        int intStart = i;
        // 정수부의 숫자를 읽는다.
        while (i < n && HtmlText.isDigit(s.charAt(i))) {
            i++;
        }
        boolean hasInt = i > intStart;
        if (i < n && s.charAt(i) == '.') {
            i++;
            int fracStart = i;
            // 소수부의 숫자를 읽는다(최소 한 자리).
            while (i < n && HtmlText.isDigit(s.charAt(i))) {
                i++;
            }
            if (i == fracStart) {
                return false;
            }
        } else if (!hasInt) {
            return false;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            i++;
            if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                i++;
            }
            int expStart = i;
            // 지수의 숫자를 읽는다(최소 한 자리).
            while (i < n && HtmlText.isDigit(s.charAt(i))) {
                i++;
            }
            if (i == expStart) {
                return false;
            }
        }
        return i == n;
    }

    /**
     * Parses a valid floating-point number into an exact decimal.
     * <p>
     * 올바른 부동소수점 수를 정확한 십진수로 해석한다.
     *
     * @param s the text, may be null / 문자열(null 가능)
     * @return the value, or null if the text is not valid or out of range / 값. 올바르지 않거나 범위를 넘으면 null
     */
    static BigDecimal parse(String s) {
        if (s == null || s.length() == 0 || !isValidFloat(s)) {
            return null;
        }
        int e = Math.max(s.indexOf('e'), s.indexOf('E'));
        if (e >= 0) {
            String exp = s.substring(e + 1);
            if (exp.startsWith("+")) {
                exp = exp.substring(1);
            }
            boolean neg = exp.startsWith("-");
            String digits = neg ? exp.substring(1) : exp;
            // 앞의 0 을 지운 지수 자릿수가 너무 크면 범위를 넘는 값이다.
            String trimmed = digits.replaceFirst("^0+", "");
            if (trimmed.length() > 4 || (trimmed.length() > 0 && Integer.parseInt(trimmed) > MAX_EXPONENT)) {
                return neg ? BigDecimal.ZERO : null;
            }
        }
        BigDecimal v = new BigDecimal(s);
        return v.abs().compareTo(LIMIT) > 0 ? null : v;
    }

    /**
     * Serializes a decimal as the shortest valid floating-point number without an exponent.
     * <p>
     * 십진수를 지수 없는 가장 짧은 올바른 부동소수점 수 문자열로 직렬화한다.
     *
     * @param v the value / 값
     * @return the text / 문자열
     */
    static String format(BigDecimal v) {
        if (v.signum() == 0) {
            return "0";
        }
        return v.stripTrailingZeros().toPlainString();
    }

    /**
     * Serializes a double the way JavaScript does (the best representation as a floating-point number).
     * <p>
     * double 값을 JavaScript 방식으로 직렬화한다(부동소수점 수로서의 가장 좋은 표현).
     *
     * @param d the value, finite / 값(유한)
     * @return the text / 문자열
     */
    static String format(double d) {
        return NumberText.toString(d);
    }
}
