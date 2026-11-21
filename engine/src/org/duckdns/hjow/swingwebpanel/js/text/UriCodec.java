package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * ECMAScript 2015 URI handling functions (section 18.2.6): encodeURI, encodeURIComponent, decodeURI and
 * decodeURIComponent operating on UTF-16 strings. Characters are encoded as UTF-8 percent sequences with upper case
 * hexadecimal digits; decoding validates UTF-8 strictly (no overlong forms, no surrogates, at most U+10FFFF) and, for
 * decodeURI, keeps escapes of reserved characters and "#" unchanged. Malformed input throws {@link UriFormatException}.
 * All methods are static and thread-safe.
 * <p>
 * UTF-16 문자열 위의 ECMAScript 2015 URI 처리 함수(18.2.6절): encodeURI, encodeURIComponent, decodeURI, decodeURIComponent.
 * 문자는 대문자 16진수를 쓰는 UTF-8 퍼센트 시퀀스로 부호화한다. 복호화는 UTF-8 을 엄격하게 검사하고(과도하게 긴 형태, 서로게이트, U+10FFFF 초과 불가)
 * decodeURI 는 예약 문자와 "#" 의 escape 를 그대로 둔다. 올바르지 않은 입력은 {@link UriFormatException} 을 던진다.
 * 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class UriCodec {

    /**
     * Reserved URI characters (uriReserved).
     * <p>
     * 예약된 URI 문자(uriReserved).
     */
    private static final String RESERVED = ";/?:@&=+$,";

    /**
     * Unreserved punctuation (uriMark).
     * <p>
     * 예약되지 않은 구두점(uriMark).
     */
    private static final String MARK = "-_.!~*'()";

    /**
     * Upper case hexadecimal digits.
     * <p>
     * 대문자 16진수 문자.
     */
    private static final String HEX = "0123456789ABCDEF";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private UriCodec() {
    }

    /**
     * encodeURI: keeps reserved characters, unreserved characters and "#".
     * <p>
     * encodeURI: 예약 문자, 예약되지 않은 문자, "#" 은 그대로 둔다.
     *
     * @param s string / 문자열
     * @return encoded string / 부호화한 문자열
     * @throws UriFormatException if s contains a lone surrogate / s 에 짝 없는 서로게이트가 있으면
     */
    public static String encodeURI(String s) {
        return encode(s, RESERVED + "#");
    }

    /**
     * encodeURIComponent: keeps only unreserved characters.
     * <p>
     * encodeURIComponent: 예약되지 않은 문자만 그대로 둔다.
     *
     * @param s string / 문자열
     * @return encoded string / 부호화한 문자열
     * @throws UriFormatException if s contains a lone surrogate / s 에 짝 없는 서로게이트가 있으면
     */
    public static String encodeURIComponent(String s) {
        return encode(s, "");
    }

    /**
     * decodeURI: does not decode escapes of reserved characters and "#".
     * <p>
     * decodeURI: 예약 문자와 "#" 의 escape 는 복호화하지 않는다.
     *
     * @param s string / 문자열
     * @return decoded string / 복호화한 문자열
     * @throws UriFormatException on a malformed escape sequence / 올바르지 않은 escape 시퀀스이면
     */
    public static String decodeURI(String s) {
        return decode(s, RESERVED + "#");
    }

    /**
     * decodeURIComponent: decodes every escape.
     * <p>
     * decodeURIComponent: 모든 escape 를 복호화한다.
     *
     * @param s string / 문자열
     * @return decoded string / 복호화한 문자열
     * @throws UriFormatException on a malformed escape sequence / 올바르지 않은 escape 시퀀스이면
     */
    public static String decodeURIComponent(String s) {
        return decode(s, "");
    }

    /**
     * Tests uriAlpha, DecimalDigit and uriMark.
     * <p>
     * uriAlpha, 10진 숫자, uriMark 인지 검사한다.
     *
     * @param c character / 문자
     * @return true if unreserved / 예약되지 않은 문자이면 true
     */
    private static boolean isUnreserved(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || MARK.indexOf(c) >= 0;
    }

    /**
     * Encode algorithm (18.2.6.5).
     * <p>
     * Encode 알고리즘(18.2.6.5).
     *
     * @param s         string / 문자열
     * @param unescaped additional characters left unescaped / 추가로 그대로 두는 문자
     * @return encoded string / 부호화한 문자열
     */
    private static String encode(String s, String unescaped) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        int len = s.length();
        int k = 0;
        // 문자를 하나씩 보며 그대로 둘 수 없는 것은 UTF-8 바이트로 풀어 %XX 로 쓴다.
        while (k < len) {
            char c = s.charAt(k);
            if (isUnreserved(c) || unescaped.indexOf(c) >= 0) {
                out.append(c);
                k++;
                continue;
            }
            int cp;
            if (Character.isLowSurrogate(c)) {
                throw new UriFormatException("URI malformed");
            }
            if (Character.isHighSurrogate(c)) {
                k++;
                if (k >= len || !Character.isLowSurrogate(s.charAt(k))) {
                    throw new UriFormatException("URI malformed");
                }
                cp = Character.toCodePoint(c, s.charAt(k));
            } else {
                cp = c;
            }
            appendUtf8Escapes(out, cp);
            k++;
        }
        return out.toString();
    }

    /**
     * Appends a code point as percent-escaped UTF-8.
     * <p>
     * 코드 포인트를 퍼센트로 escape 한 UTF-8 로 덧붙인다.
     *
     * @param out output / 출력
     * @param cp  code point / 코드 포인트
     */
    private static void appendUtf8Escapes(StringBuilder out, int cp) {
        if (cp < 0x80) {
            percent(out, cp);
        } else if (cp < 0x800) {
            percent(out, 0xC0 | (cp >> 6));
            percent(out, 0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            percent(out, 0xE0 | (cp >> 12));
            percent(out, 0x80 | ((cp >> 6) & 0x3F));
            percent(out, 0x80 | (cp & 0x3F));
        } else {
            percent(out, 0xF0 | (cp >> 18));
            percent(out, 0x80 | ((cp >> 12) & 0x3F));
            percent(out, 0x80 | ((cp >> 6) & 0x3F));
            percent(out, 0x80 | (cp & 0x3F));
        }
    }

    /**
     * Appends %XX for one byte.
     * <p>
     * 바이트 하나를 %XX 로 덧붙인다.
     *
     * @param out output / 출력
     * @param b   byte value 0..255 / 바이트 값 0..255
     */
    private static void percent(StringBuilder out, int b) {
        out.append('%').append(HEX.charAt((b >> 4) & 0xF)).append(HEX.charAt(b & 0xF));
    }

    /**
     * Decode algorithm (18.2.6.5).
     * <p>
     * Decode 알고리즘(18.2.6.5).
     *
     * @param s           string / 문자열
     * @param reservedSet characters whose escapes are kept as they are / escape 를 그대로 두는 문자
     * @return decoded string / 복호화한 문자열
     */
    private static String decode(String s, String reservedSet) {
        StringBuilder out = new StringBuilder(s.length());
        int len = s.length();
        int k = 0;
        // 퍼센트가 아닌 문자는 그대로 옮기고, 퍼센트 시퀀스는 UTF-8 로 해석한다.
        while (k < len) {
            char c = s.charAt(k);
            if (c != '%') {
                out.append(c);
                k++;
                continue;
            }
            int start = k;
            int b = hexByte(s, k);
            k += 2;
            if ((b & 0x80) == 0) {
                char ch = (char) b;
                if (reservedSet.indexOf(ch) >= 0) {
                    out.append(s, start, k + 1);
                } else {
                    out.append(ch);
                }
            } else {
                int n = 0;
                // 선행 1 비트의 개수로 UTF-8 바이트 수를 구한다.
                while (((b << n) & 0x80) != 0 && n < 8) {
                    n++;
                }
                if (n == 1 || n > 4) {
                    throw new UriFormatException("URI malformed");
                }
                int cp = b & (0xFF >> (n + 1));
                // 이어지는 바이트는 모두 %10xxxxxx 여야 한다.
                for (int j = 1; j < n; j++) {
                    k++;
                    if (k >= len || s.charAt(k) != '%') {
                        throw new UriFormatException("URI malformed");
                    }
                    int nb = hexByte(s, k);
                    if ((nb & 0xC0) != 0x80) {
                        throw new UriFormatException("URI malformed");
                    }
                    k += 2;
                    cp = (cp << 6) | (nb & 0x3F);
                }
                boolean overlong = (n == 2 && cp < 0x80) || (n == 3 && cp < 0x800) || (n == 4 && cp < 0x10000);
                if (overlong || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
                    throw new UriFormatException("URI malformed");
                }
                out.appendCodePoint(cp);
            }
            k++;
        }
        return out.toString();
    }

    /**
     * Reads the two hexadecimal digits after the percent sign at index k.
     * <p>
     * 인덱스 k 의 퍼센트 기호 뒤 16진수 두 자리를 읽는다.
     *
     * @param s string / 문자열
     * @param k index of the percent sign / 퍼센트 기호의 위치
     * @return byte value / 바이트 값
     */
    private static int hexByte(String s, int k) {
        if (k + 2 >= s.length()) {
            throw new UriFormatException("URI malformed");
        }
        int h = CharClass.hexValue(s.charAt(k + 1));
        int l = CharClass.hexValue(s.charAt(k + 2));
        if (h < 0 || l < 0) {
            throw new UriFormatException("URI malformed");
        }
        return (h << 4) | l;
    }
}
