package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * Code point oriented helpers behind <code>String.prototype.codePointAt</code>, <code>String.fromCodePoint</code>,
 * <code>String.fromCharCode</code> and <code>String.raw</code>. JavaScript strings are sequences of UTF-16 code units
 * (lone surrogates are legal), so these helpers never reject lone surrogates unless the specification says so.
 * All methods are static and thread-safe.
 * <p>
 * <code>String.prototype.codePointAt</code>, <code>String.fromCodePoint</code>, <code>String.fromCharCode</code>,
 * <code>String.raw</code> 의 바탕이 되는 코드 포인트 도우미. JavaScript 문자열은 UTF-16 코드 단위의 나열이며(짝 없는 서로게이트도 허용)
 * 규격이 요구하지 않는 한 짝 없는 서로게이트를 거부하지 않는다. 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class CodePoints {

    /**
     * Largest Unicode code point.
     * <p>
     * 유니코드 최대 코드 포인트.
     */
    public static final int MAX_CODE_POINT = 0x10FFFF;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CodePoints() {
    }

    /**
     * String.prototype.codePointAt(index): the code point starting at the index, a lone surrogate as it is,
     * or -1 (undefined in JavaScript) when the index is outside the string.
     * <p>
     * String.prototype.codePointAt(index): 해당 위치에서 시작하는 코드 포인트(짝 없는 서로게이트는 그대로).
     * 위치가 문자열 밖이면 -1(JavaScript 의 undefined).
     *
     * @param s     string / 문자열
     * @param index code unit index / 코드 단위 위치
     * @return code point or -1 / 코드 포인트 또는 -1
     */
    public static int codePointAt(String s, int index) {
        if (index < 0 || index >= s.length()) {
            return -1;
        }
        char first = s.charAt(index);
        if (Character.isHighSurrogate(first) && index + 1 < s.length() && Character.isLowSurrogate(s.charAt(index + 1))) {
            return Character.toCodePoint(first, s.charAt(index + 1));
        }
        return first;
    }

    /**
     * Number of code points in the string, counting a lone surrogate as one.
     * <p>
     * 문자열의 코드 포인트 수(짝 없는 서로게이트는 1 로 센다).
     *
     * @param s string / 문자열
     * @return code point count / 코드 포인트 수
     */
    public static int codePointCount(String s) {
        return s.codePointCount(0, s.length());
    }

    /**
     * String.fromCodePoint(...codePoints): every argument must be an integer from 0 to 0x10FFFF.
     * <p>
     * String.fromCodePoint(...codePoints): 모든 인수는 0 이상 0x10FFFF 이하의 정수여야 한다.
     *
     * @param codePoints numeric arguments (already converted with ToNumber) / 숫자 인수(ToNumber 를 거친 값)
     * @return the string / 문자열
     * @throws TextRangeException if an argument is not a valid code point / 올바른 코드 포인트가 아니면
     */
    public static String fromCodePoint(double... codePoints) {
        StringBuilder sb = new StringBuilder();
        // 인수를 하나씩 검사하며 UTF-16 으로 붙인다.
        for (int i = 0; i < codePoints.length; i++) {
            double d = codePoints[i];
            boolean integral = !Double.isNaN(d) && !Double.isInfinite(d) && d == Math.floor(d);
            if (!integral || d < 0 || d > MAX_CODE_POINT) {
                throw new TextRangeException("Invalid code point " + d);
            }
            sb.appendCodePoint((int) d);
        }
        return sb.toString();
    }

    /**
     * String.fromCharCode(...codeUnits): each argument is converted with ToUint16.
     * <p>
     * String.fromCharCode(...codeUnits): 각 인수를 ToUint16 으로 바꾼다.
     *
     * @param codeUnits numeric arguments (already converted with ToNumber) / 숫자 인수(ToNumber 를 거친 값)
     * @return the string / 문자열
     */
    public static String fromCharCode(double... codeUnits) {
        StringBuilder sb = new StringBuilder(codeUnits.length);
        // 각 값을 2^16 으로 나눈 나머지 코드 단위로 바꾼다.
        for (int i = 0; i < codeUnits.length; i++) {
            double d = codeUnits[i];
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append((char) 0);
            } else {
                double t = d < 0 ? Math.ceil(d) : Math.floor(d);
                double m = t % 65536.0;
                sb.append((char) (int) (long) m);
            }
        }
        return sb.toString();
    }

    /**
     * String.raw core: joins the raw template segments with the already converted substitutions, using as many
     * substitutions as there are gaps (missing substitutions contribute nothing).
     * <p>
     * String.raw 의 핵심: 원본 템플릿 조각들을 이미 문자열로 바꾼 치환 값으로 이어 붙인다. 조각 사이 틈의 수만큼만 치환 값을 쓰며
     * 부족한 치환 값은 아무것도 붙이지 않는다.
     *
     * @param rawSegments   raw literal segments / 원본 리터럴 조각
     * @param substitutions substitution strings / 치환 문자열
     * @return joined string / 이어 붙인 문자열
     */
    public static String raw(String[] rawSegments, String[] substitutions) {
        int n = rawSegments.length;
        if (n <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // 조각과 치환 값을 번갈아 붙인다.
        for (int i = 0; i < n; i++) {
            sb.append(rawSegments[i]);
            if (i + 1 == n) {
                break;
            }
            if (i < substitutions.length) {
                sb.append(substitutions[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Tests whether the string has only complete surrogate pairs.
     * <p>
     * 서로게이트가 모두 완전한 쌍을 이루는지 검사한다.
     *
     * @param s string / 문자열
     * @return true if well formed UTF-16 / 올바른 UTF-16 이면 true
     */
    public static boolean isWellFormed(String s) {
        return unpairedFrom(s, 0) < 0;
    }

    /**
     * Replaces unpaired surrogates by U+FFFD while preserving complete pairs and other code units.
     * <p>
     * 짝 없는 서로게이트를 U+FFFD로 바꾸고 완전한 쌍과 그 밖의 코드 단위는 유지한다.
     *
     * @param s the source string / 원본 문자열
     * @return the well-formed string / 정합한 문자열
     */
    public static String toWellFormed(String s) {
        int index = unpairedFrom(s, 0);
        if (index < 0) {
            return s;
        }
        char[] units = s.toCharArray();
        // 처음 발견한 위치부터 짝 없는 코드 단위만 하나씩 바꾼다.
        do {
            units[index] = (char) 0xFFFD;
            index = unpairedFrom(s, index + 1);
        } while (index >= 0);
        return new String(units);
    }

    /**
     * Finds the first unpaired surrogate at or after a code-unit boundary.
     * <p>
     * 주어진 코드 단위 경계부터 처음 나타난 짝 없는 서로게이트 위치를 찾는다.
     *
     * @param s the source string / 원본 문자열
     * @param start offset after the preceding pair or unpaired unit / 이전 쌍 또는 짝 없는 단위 다음 위치
     * @return the unpaired unit offset or -1 / 짝 없는 단위 위치 또는 -1
     */
    private static int unpairedFrom(String s, int start) {
        int n = s.length();
        // 앞에서부터 서로게이트 쌍을 확인한다.
        for (int i = start; i < n; i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= n || !Character.isLowSurrogate(s.charAt(i + 1))) {
                    return i;
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                return i;
            }
        }
        return -1;
    }
}
