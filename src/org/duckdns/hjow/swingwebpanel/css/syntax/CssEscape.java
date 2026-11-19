package org.duckdns.hjow.swingwebpanel.css.syntax;

/**
 * Serialization helpers of the CSSOM: escaping of identifiers and strings, and the number format of the standard. They are used by token
 * serialization, selector serialization and value serialization.
 * <p>
 * CSSOM 의 직렬화 도우미: 식별자와 문자열의 이스케이프, 표준의 숫자 형식. 토큰, 선택자, 값의 직렬화가 사용한다.
 */
public final class CssEscape {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CssEscape() {
    }

    /**
     * Serializes an identifier (the algorithm of <code>CSS.escape</code>).
     * <p>
     * 식별자를 직렬화한다(<code>CSS.escape</code> 의 알고리즘).
     *
     * @param s identifier text / 식별자 문자열
     * @return escaped identifier / 이스케이프한 식별자
     */
    public static String ident(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        int n = s.length();
        // 문자마다 규칙에 따라 그대로 쓰거나 이스케이프한다.
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == 0) {
                sb.append((char) 0xFFFD);
            } else if ((c >= 1 && c <= 0x1F) || c == 0x7F) {
                hex(sb, c);
            } else if (i == 0 && c >= '0' && c <= '9') {
                hex(sb, c);
            } else if (i == 1 && c >= '0' && c <= '9' && s.charAt(0) == '-') {
                hex(sb, c);
            } else if (i == 0 && c == '-' && n == 1) {
                sb.append('\\').append(c);
            } else if (c >= 0x80 || c == '-' || c == '_' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                sb.append(c);
            } else {
                sb.append('\\').append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Appends a code point as hexadecimal escape followed by a space.
     * <p>
     * 코드 포인트를 16진수 이스케이프와 공백으로 덧붙인다.
     *
     * @param sb output / 출력
     * @param c  character / 문자
     */
    private static void hex(StringBuilder sb, char c) {
        sb.append('\\').append(Integer.toHexString(c)).append(' ');
    }

    /**
     * Serializes a string with double quotes.
     * <p>
     * 문자열을 큰따옴표로 직렬화한다.
     *
     * @param s string content / 문자열 내용
     * @return quoted string / 따옴표로 감싼 문자열
     */
    public static String string(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        // 따옴표, 역슬래시, 제어 문자를 이스케이프한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 0) {
                sb.append((char) 0xFFFD);
            } else if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\a ");
            } else if ((c >= 1 && c <= 0x1F) || c == 0x7F) {
                hex(sb, c);
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /**
     * Serializes a number: integers without fraction, other values with up to six decimals and no trailing zeros.
     * <p>
     * 숫자를 직렬화한다: 정수는 소수부 없이, 그 밖의 값은 소수 여섯 자리까지 끝의 0 을 뺀다.
     *
     * @param d number / 숫자
     * @return text / 문자열
     */
    public static String number(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        java.math.BigDecimal bd = new java.math.BigDecimal(d).setScale(6, java.math.RoundingMode.HALF_EVEN);
        // JRE 6 의 stripTrailingZeros 는 0 에 효과가 없으므로 0 은 따로 처리한다.
        if (bd.signum() == 0) {
            return "0";
        }
        return bd.stripTrailingZeros().toPlainString();
    }
}
