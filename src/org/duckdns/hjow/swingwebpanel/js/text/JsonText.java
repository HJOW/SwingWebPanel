package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * JSON string quoting for <code>JSON.stringify</code>, independent of JavaScript values. {@link #quote(String)}
 * implements the ECMAScript 2015 behavior: only the quotation mark, the backslash and code units below U+0020 are
 * escaped (\b \f \n \r \t or a lower case four digit unicode escape), lone surrogates pass through unchanged.
 * {@link #quoteWellFormed(String)} implements the ES2019 "well-formed JSON.stringify" extension that also escapes lone
 * surrogates. U+2028 and U+2029 are never escaped. All methods are static and thread-safe.
 * <p>
 * <code>JSON.stringify</code> 용 JSON 문자열 인용. JavaScript 값과 무관하다. {@link #quote(String)} 은 ECMAScript 2015 동작이다:
 * 따옴표, 역슬래시, U+0020 미만의 코드 단위만 escape(\b \f \n \r \t 또는 소문자 4자리 유니코드 escape)하고 짝 없는 서로게이트는 그대로 둔다.
 * {@link #quoteWellFormed(String)} 은 짝 없는 서로게이트도 escape 하는 ES2019 "well-formed JSON.stringify" 확장이다.
 * U+2028, U+2029 는 escape 하지 않는다. 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class JsonText {

    /**
     * Lower case hexadecimal digits.
     * <p>
     * 소문자 16진수 문자.
     */
    private static final String HEX = "0123456789abcdef";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private JsonText() {
    }

    /**
     * QuoteJSONString per ECMAScript 2015 (lone surrogates are copied unchanged).
     * <p>
     * ECMAScript 2015 의 QuoteJSONString (짝 없는 서로게이트는 그대로 복사한다).
     *
     * @param s string / 문자열
     * @return the quoted string including the surrounding quotation marks / 앞뒤 따옴표를 포함한 인용 문자열
     */
    public static String quote(String s) {
        return quote(s, false);
    }

    /**
     * QuoteJSONString with the ES2019 well-formed extension (lone surrogates become lower case unicode escapes).
     * <p>
     * ES2019 well-formed 확장을 적용한 QuoteJSONString (짝 없는 서로게이트는 소문자 유니코드 escape 가 된다).
     *
     * @param s string / 문자열
     * @return the quoted string including the surrounding quotation marks / 앞뒤 따옴표를 포함한 인용 문자열
     */
    public static String quoteWellFormed(String s) {
        return quote(s, true);
    }

    /**
     * Shared implementation of both quoting modes.
     * <p>
     * 두 인용 방식의 공통 구현.
     *
     * @param s          string / 문자열
     * @param wellFormed true to escape lone surrogates / 짝 없는 서로게이트를 escape 하려면 true
     * @return quoted string / 인용 문자열
     */
    private static String quote(String s, boolean wellFormed) {
        StringBuilder out = new StringBuilder(s.length() + 2);
        out.append('"');
        int n = s.length();
        // 코드 단위마다 escape 가 필요한지 판단한다.
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
                out.append('\\').append('"');
                break;
            case '\\':
                out.append('\\').append('\\');
                break;
            case '\b':
                out.append('\\').append('b');
                break;
            case '\f':
                out.append('\\').append('f');
                break;
            case '\n':
                out.append('\\').append('n');
                break;
            case '\r':
                out.append('\\').append('r');
                break;
            case '\t':
                out.append('\\').append('t');
                break;
            default:
                if (c < 0x20) {
                    appendUnicodeEscape(out, c);
                } else if (wellFormed && Character.isHighSurrogate(c)) {
                    // 올바른 쌍이면 그대로, 아니면 escape 한다.
                    if (i + 1 < n && Character.isLowSurrogate(s.charAt(i + 1))) {
                        out.append(c).append(s.charAt(i + 1));
                        i++;
                    } else {
                        appendUnicodeEscape(out, c);
                    }
                } else if (wellFormed && Character.isLowSurrogate(c)) {
                    appendUnicodeEscape(out, c);
                } else {
                    out.append(c);
                }
                break;
            }
        }
        out.append('"');
        return out.toString();
    }

    /**
     * Appends a backslash, the letter u and four lower case hexadecimal digits.
     * <p>
     * 역슬래시, 글자 u, 소문자 16진수 네 자리를 덧붙인다.
     *
     * @param out output / 출력
     * @param c   code unit / 코드 단위
     */
    private static void appendUnicodeEscape(StringBuilder out, char c) {
        out.append('\\').append('u').append(HEX.charAt((c >> 12) & 0xF)).append(HEX.charAt((c >> 8) & 0xF))
                .append(HEX.charAt((c >> 4) & 0xF)).append(HEX.charAt(c & 0xF));
    }
}
