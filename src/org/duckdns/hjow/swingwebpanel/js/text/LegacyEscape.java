package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * ECMAScript Annex B legacy functions <code>escape</code> and <code>unescape</code> (B.2.1, B.2.2).
 * <code>escape</code> keeps ASCII letters, digits and the characters <code>@*_+-./</code>; code units below 256 become
 * percent plus two upper case hex digits; other code units become percent, the letter u and four upper case hex digits.
 * <code>unescape</code> reverses both forms and leaves malformed sequences untouched. All methods are static and thread-safe.
 * <p>
 * ECMAScript 부속서 B 의 레거시 함수 <code>escape</code>, <code>unescape</code>(B.2.1, B.2.2).
 * <code>escape</code> 는 ASCII 글자, 숫자, <code>@*_+-./</code> 를 그대로 두고, 256 미만 코드 단위는 퍼센트와 대문자 16진수 두 자리로,
 * 그 밖의 코드 단위는 퍼센트, 글자 u, 대문자 16진수 네 자리로 바꾼다. <code>unescape</code> 는 두 형태를 되돌리며 올바르지 않은 시퀀스는 그대로 둔다.
 * 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class LegacyEscape {

    /**
     * Characters (besides ASCII letters and digits) that escape leaves unchanged.
     * <p>
     * escape 가 그대로 두는 문자(ASCII 글자와 숫자 제외).
     */
    private static final String UNESCAPED = "@*_+-./";

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
    private LegacyEscape() {
    }

    /**
     * escape(string).
     * <p>
     * escape(string).
     *
     * @param s string / 문자열
     * @return escaped string / escape 한 문자열
     */
    public static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        // 코드 단위마다 그대로 둘지, %XX 또는 %uXXXX 로 바꿀지 결정한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || UNESCAPED.indexOf(c) >= 0) {
                out.append(c);
            } else if (c < 256) {
                out.append('%').append(HEX.charAt(c >> 4)).append(HEX.charAt(c & 0xF));
            } else {
                out.append("%u").append(HEX.charAt(c >> 12)).append(HEX.charAt((c >> 8) & 0xF))
                        .append(HEX.charAt((c >> 4) & 0xF)).append(HEX.charAt(c & 0xF));
            }
        }
        return out.toString();
    }

    /**
     * unescape(string).
     * <p>
     * unescape(string).
     *
     * @param s string / 문자열
     * @return unescaped string / unescape 한 문자열
     */
    public static String unescape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int len = s.length();
        int k = 0;
        // 퍼센트 뒤에 u 와 4자리, 또는 2자리 16진수가 있으면 그 문자로 바꾼다.
        while (k < len) {
            char c = s.charAt(k);
            if (c == '%') {
                if (k <= len - 6 && s.charAt(k + 1) == 'u' && isHex(s, k + 2, 4)) {
                    c = (char) Integer.parseInt(s.substring(k + 2, k + 6), 16);
                    k += 5;
                } else if (k <= len - 3 && isHex(s, k + 1, 2)) {
                    c = (char) Integer.parseInt(s.substring(k + 1, k + 3), 16);
                    k += 2;
                }
            }
            out.append(c);
            k++;
        }
        return out.toString();
    }

    /**
     * Tests that count characters from a start index are all hexadecimal digits.
     * <p>
     * 시작 위치부터 count 개의 문자가 모두 16진수인지 검사한다.
     *
     * @param s     string / 문자열
     * @param from  start index / 시작 위치
     * @param count number of characters / 문자 수
     * @return true if all are hex digits / 모두 16진수이면 true
     */
    private static boolean isHex(String s, int from, int count) {
        // 지정한 범위의 문자를 하나씩 확인한다.
        for (int i = from; i < from + count; i++) {
            if (CharClass.hexValue(s.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
