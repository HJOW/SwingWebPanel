package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;

/**
 * Small string helpers of the HTML form layer: ASCII white space, newline normalization and the parsing of integers the way the HTML
 * Standard describes them. All methods are static and never throw.
 * <p>
 * HTML 폼 계층의 작은 문자열 도우미: ASCII 공백, 줄 끝 정규화, HTML 표준이 설명하는 정수 해석. 모든 메서드는 정적이며 예외를 던지지 않는다.
 */
final class HtmlText {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HtmlText() {
    }

    /**
     * Tells whether a character is ASCII white space (space, tab, LF, FF, CR).
     * <p>
     * 문자가 ASCII 공백(스페이스, 탭, LF, FF, CR)인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for ASCII white space / ASCII 공백이면 true
     */
    static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Tells whether a character is an ASCII digit.
     * <p>
     * 문자가 ASCII 숫자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for 0-9 / 0-9 이면 true
     */
    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Strips leading and trailing ASCII white space.
     * <p>
     * 앞뒤의 ASCII 공백을 제거한다.
     *
     * @param s the text / 문자열
     * @return the stripped text / 제거한 문자열
     */
    static String strip(String s) {
        int a = 0;
        int b = s.length();
        // 앞쪽 공백을 건너뛴다.
        while (a < b && isSpace(s.charAt(a))) {
            a++;
        }
        // 뒤쪽 공백을 건너뛴다.
        while (b > a && isSpace(s.charAt(b - 1))) {
            b--;
        }
        return s.substring(a, b);
    }

    /**
     * Removes every LF and CR character.
     * <p>
     * 모든 LF 와 CR 문자를 제거한다.
     *
     * @param s the text / 문자열
     * @return the text without newlines / 줄 끝이 없는 문자열
     */
    static String stripNewlines(String s) {
        if (s.indexOf('\n') < 0 && s.indexOf('\r') < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        // 줄 끝이 아닌 문자만 모은다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\n' && c != '\r') {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Normalizes every CRLF and every lone CR to LF.
     * <p>
     * 모든 CRLF 와 단독 CR 을 LF 로 바꾼다.
     *
     * @param s the text / 문자열
     * @return the text with LF line ends / LF 줄 끝의 문자열
     */
    static String normalizeNewlines(String s) {
        if (s.indexOf('\r') < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        // CR 은 LF 로 바꾸고 바로 뒤의 LF 는 건너뛴다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                b.append('\n');
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Converts every line end (LF, CR or CRLF) to CRLF.
     * <p>
     * 모든 줄 끝(LF, CR, CRLF)을 CRLF 로 바꾼다.
     *
     * @param s the text / 문자열
     * @return the text with CRLF line ends / CRLF 줄 끝의 문자열
     */
    static String toCrlf(String s) {
        if (s.indexOf('\n') < 0 && s.indexOf('\r') < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length() + 8);
        // 단독 CR 이나 단독 LF 도 CRLF 로 바꾸고 이미 CRLF 인 쌍은 그대로 둔다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                b.append("\r\n");
            } else if (c == '\n') {
                b.append("\r\n");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Applies the "rules for parsing non-negative integers": optional leading white space, digits, anything after them is ignored.
     * <p>
     * "음이 아닌 정수 해석 규칙"을 적용한다: 앞 공백은 건너뛰고 숫자를 읽으며 그 뒤는 무시한다.
     *
     * @param s the text, may be null / 문자열(null 가능)
     * @return the value, or -1 on error (or when it exceeds the int range) / 값. 오류(또는 int 범위 초과)이면 -1
     */
    static int parseNonNegative(String s) {
        if (s == null) {
            return -1;
        }
        int i = 0;
        // 앞쪽 공백을 건너뛴다.
        while (i < s.length() && isSpace(s.charAt(i))) {
            i++;
        }
        if (i < s.length() && s.charAt(i) == '+') {
            i++;
        }
        int start = i;
        long v = 0;
        // 연속한 숫자를 읽는다.
        while (i < s.length() && isDigit(s.charAt(i))) {
            v = v * 10 + (s.charAt(i) - '0');
            if (v > Integer.MAX_VALUE) {
                return -1;
            }
            i++;
        }
        return i == start ? -1 : (int) v;
    }

    /**
     * Applies the "rules for parsing integers" (optional sign allowed). The result is a long so that callers can detect an error.
     * <p>
     * "정수 해석 규칙"을 적용한다(부호 허용). 호출자가 오류를 알아챌 수 있도록 결과는 Long 이다.
     *
     * @param s the text, may be null / 문자열(null 가능)
     * @return the value or null on error / 값. 오류이면 null
     */
    static Long parseInteger(String s) {
        if (s == null) {
            return null;
        }
        int i = 0;
        // 앞쪽 공백을 건너뛴다.
        while (i < s.length() && isSpace(s.charAt(i))) {
            i++;
        }
        boolean negative = false;
        if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            negative = s.charAt(i) == '-';
            i++;
        }
        int start = i;
        long v = 0;
        // 연속한 숫자를 읽는다.
        while (i < s.length() && isDigit(s.charAt(i))) {
            v = v * 10 + (s.charAt(i) - '0');
            if (v > 0x7FFFFFFFFFFFFFL) {
                return null;
            }
            i++;
        }
        return i == start ? null : Long.valueOf(negative ? -v : v);
    }

    /**
     * Splits text at ASCII white space into tokens (no empty tokens).
     * <p>
     * 텍스트를 ASCII 공백에서 나눠 토큰을 만든다(빈 토큰 없음).
     *
     * @param s the text / 문자열
     * @return the tokens / 토큰
     */
    static List<String> splitSpaces(String s) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        // 공백이 아닌 연속 구간을 하나씩 잘라 낸다.
        while (i < s.length()) {
            // 앞쪽 공백을 건너뛴다.
            while (i < s.length() && isSpace(s.charAt(i))) {
                i++;
            }
            int start = i;
            // 공백이 아닌 구간의 끝을 찾는다.
            while (i < s.length() && !isSpace(s.charAt(i))) {
                i++;
            }
            if (i > start) {
                out.add(s.substring(start, i));
            }
        }
        return out;
    }

    /**
     * Lower-cases ASCII letters only.
     * <p>
     * ASCII 문자만 소문자로 바꾼다.
     *
     * @param s the text / 문자열
     * @return the lower-cased text / 소문자로 바꾼 문자열
     */
    static String lower(String s) {
        StringBuilder b = null;
        // 대문자 ASCII 가 있을 때만 새 문자열을 만든다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (b == null) {
                    b = new StringBuilder(s);
                }
                b.setCharAt(i, (char) (c + 32));
            }
        }
        return b == null ? s : b.toString();
    }
}
