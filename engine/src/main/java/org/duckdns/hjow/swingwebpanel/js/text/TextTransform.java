package org.duckdns.hjow.swingwebpanel.js.text;

import java.util.Locale;

/**
 * String transformations used by <code>String.prototype</code>: normalize, toUpperCase/toLowerCase (and the locale
 * variants), trim, repeat. Default case conversion uses the generated full Unicode mappings.
 * Normalization uses the generated Unicode 18 data. Explicit Java locales use the running JRE.
 * <p>
 * <code>String.prototype</code> 이 쓰는 문자열 변환: normalize, toUpperCase/toLowerCase(와 로캘 변형), trim, repeat.
 * 기본 대소문자 변환과 정규화는 생성한 Unicode 18 자료를 쓴다. 명시적 Java 로케일은 실행 중인 JRE에 따른다.
 * 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class TextTransform {

    /**
     * Largest string length this library will create (2^28 - 16 code units, similar to engines).
     * <p>
     * 이 라이브러리가 만드는 문자열의 최대 길이(2^28 - 16 코드 단위, 엔진과 비슷한 값).
     */
    public static final int MAX_STRING_LENGTH = (1 << 28) - 16;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TextTransform() {
    }

    /**
     * String.prototype.normalize(form): the form defaults to NFC when null.
     * <p>
     * String.prototype.normalize(form): form 이 null 이면 NFC 를 쓴다.
     *
     * @param s    string / 문자열
     * @param form "NFC", "NFD", "NFKC", "NFKD" or null / "NFC", "NFD", "NFKC", "NFKD" 또는 null
     * @return normalized string / 정규화한 문자열
     * @throws TextRangeException if the form is not one of the four names / form 이 네 이름 중 하나가 아니면
     */
    public static String normalize(String s, String form) {
        if (form == null || "NFC".equals(form)) {
            return UnicodeNormalization.normalize(s, false, true);
        }
        if ("NFD".equals(form)) {
            return UnicodeNormalization.normalize(s, false, false);
        }
        if ("NFKC".equals(form)) {
            return UnicodeNormalization.normalize(s, true, true);
        }
        if ("NFKD".equals(form)) {
            return UnicodeNormalization.normalize(s, true, false);
        }
        throw new TextRangeException("The normalization form should be one of NFC, NFD, NFKC, NFKD.");
    }

    /**
     * String.prototype.toUpperCase().
     * <p>
     * String.prototype.toUpperCase().
     *
     * @param s string / 문자열
     * @return upper case string / 대문자 문자열
     */
    public static String toUpperCase(String s) {
        return UnicodeCase.convert(s, true);
    }

    /**
     * String.prototype.toLowerCase().
     * <p>
     * String.prototype.toLowerCase().
     *
     * @param s string / 문자열
     * @return lower case string / 소문자 문자열
     */
    public static String toLowerCase(String s) {
        return UnicodeCase.convert(s, false);
    }

    /**
     * String.prototype.toLocaleUpperCase() for a given locale (for example Turkish dotted i).
     * <p>
     * 지정한 로캘의 String.prototype.toLocaleUpperCase() (예: 터키어 점 있는 i).
     *
     * @param s      string / 문자열
     * @param locale locale / 로캘
     * @return upper case string / 대문자 문자열
     */
    public static String toLocaleUpperCase(String s, Locale locale) {
        return s.toUpperCase(locale);
    }

    /**
     * String.prototype.toLocaleLowerCase() for a given locale.
     * <p>
     * 지정한 로캘의 String.prototype.toLocaleLowerCase().
     *
     * @param s      string / 문자열
     * @param locale locale / 로캘
     * @return lower case string / 소문자 문자열
     */
    public static String toLocaleLowerCase(String s, Locale locale) {
        return s.toLowerCase(locale);
    }

    /**
     * String.prototype.trim(): removes leading and trailing WhiteSpace and LineTerminator characters.
     * <p>
     * String.prototype.trim(): 앞뒤의 WhiteSpace 와 LineTerminator 문자를 제거한다.
     *
     * @param s string / 문자열
     * @return trimmed string / 다듬은 문자열
     */
    public static String trim(String s) {
        return trimEnd(trimStart(s));
    }

    /**
     * Removes leading WhiteSpace and LineTerminator characters (trimLeft / trimStart).
     * <p>
     * 앞쪽의 WhiteSpace 와 LineTerminator 문자를 제거한다(trimLeft / trimStart).
     *
     * @param s string / 문자열
     * @return trimmed string / 다듬은 문자열
     */
    public static String trimStart(String s) {
        int b = 0;
        // 앞에서부터 공백류가 아닌 문자를 만날 때까지 건너뛴다.
        while (b < s.length() && CharClass.isWhiteSpaceOrLineTerminator(s.charAt(b))) {
            b++;
        }
        return s.substring(b);
    }

    /**
     * Removes trailing WhiteSpace and LineTerminator characters (trimRight / trimEnd).
     * <p>
     * 뒤쪽의 WhiteSpace 와 LineTerminator 문자를 제거한다(trimRight / trimEnd).
     *
     * @param s string / 문자열
     * @return trimmed string / 다듬은 문자열
     */
    public static String trimEnd(String s) {
        int e = s.length();
        // 뒤에서부터 공백류가 아닌 문자를 만날 때까지 줄인다.
        while (e > 0 && CharClass.isWhiteSpaceOrLineTerminator(s.charAt(e - 1))) {
            e--;
        }
        return s.substring(0, e);
    }

    /**
     * String.prototype.repeat(count): count is ToInteger'd by the caller.
     * <p>
     * String.prototype.repeat(count): count 는 호출자가 ToInteger 를 적용한다.
     *
     * @param s     string / 문자열
     * @param count repeat count / 반복 횟수
     * @return repeated string / 반복한 문자열
     * @throws TextRangeException if count is negative or infinite, or the result would be too long / count 가 음수/무한대이거나 결과가 너무 길면
     */
    public static String repeat(String s, double count) {
        if (count != count) {
            count = 0;
        }
        if (count < 0 || Double.isInfinite(count)) {
            throw new TextRangeException("Invalid count value");
        }
        double n = Math.floor(count);
        if (n == 0 || s.length() == 0) {
            return "";
        }
        if (n * s.length() > MAX_STRING_LENGTH) {
            throw new TextRangeException("Invalid string length");
        }
        StringBuilder sb = new StringBuilder((int) (n * s.length()));
        // 지정한 횟수만큼 문자열을 이어 붙인다.
        for (int i = 0; i < (int) n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
