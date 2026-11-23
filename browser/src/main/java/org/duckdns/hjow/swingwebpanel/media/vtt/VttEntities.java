package org.duckdns.hjow.swingwebpanel.media.vtt;

import org.duckdns.hjow.swingwebpanel.html.NamedCharacterReferences;

/**
 * The HTML character references of cue text (WebVTT 6.4: "attempt to consume an HTML character reference", with no additional allowed
 * character in the data and <code>&gt;</code> in an annotation): the named references of the HTML table (with the legacy names that need no
 * semicolon) and the numeric references with the replacements of HTML 13.2.5.80. Not being a reference is reported by a result of null, so the
 * caller keeps the ampersand as it is.
 * <p>
 * cue 텍스트의 HTML 문자 참조(WebVTT 6.4: "attempt to consume an HTML character reference". 데이터에는 추가 허용 문자가 없고 주석에서는
 * <code>&gt;</code>): HTML 표의 이름 있는 참조(세미콜론이 필요 없는 옛 이름 포함)와 HTML 13.2.5.80 의 대체가 있는 숫자 참조. 참조가 아니면 결과가 null 이므로
 * 호출자가 앰퍼샌드를 그대로 둔다.
 */
final class VttEntities {

    /**
     * The characters that the references 0x80 to 0x9F stand for (0 means that the C1 control stays).
     * <p>
     * 참조 0x80~0x9F 가 나타내는 문자(0 이면 C1 제어 문자 그대로).
     */
    private static final int[] C1 = { 0x20AC, 0, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0, 0x017D, 0, 0,
        0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0, 0x017E, 0x0178 };

    /**
     * The result of consuming a reference: the characters and the position after the reference.
     * <p>
     * 참조를 소비한 결과: 문자와 참조 뒤의 위치.
     */
    static final class Reference {
        /**
         * The characters that the reference stands for.
         * <p>
         * 참조가 나타내는 문자.
         */
        final String text;

        /**
         * The position after the reference.
         * <p>
         * 참조 뒤의 위치.
         */
        final int end;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param text the characters / 문자
         * @param end  the position after the reference / 참조 뒤의 위치
         */
        Reference(String text, int end) {
            this.text = text;
            this.end = end;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private VttEntities() {
    }

    /**
     * Tells whether a character is an ASCII letter or digit.
     * <p>
     * 문자가 ASCII 알파벳이나 숫자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true if so / 그러면 true
     */
    private static boolean isAlnum(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Consumes the character reference that starts after an ampersand.
     * <p>
     * 앰퍼샌드 뒤에서 시작하는 문자 참조를 소비한다.
     *
     * @param in       the text / 텍스트
     * @param start    the position after the ampersand / 앰퍼샌드 뒤의 위치
     * @param inAnnot  true inside an annotation: a legacy reference without a semicolon is not one if an equals sign or an alphanumeric
     *                 follows / 주석 안이면 true: 세미콜론이 없는 옛 참조 뒤에 등호나 영숫자가 오면 참조가 아니다
     * @return the reference, or null if there is none / 참조. 없으면 null
     */
    static Reference consume(String in, int start, boolean inAnnot) {
        int n = in.length();
        if (start >= n) {
            return null;
        }
        char c = in.charAt(start);
        if (c == '#') {
            return numeric(in, start + 1);
        }
        if (!isAlnum(c)) {
            return null;
        }
        int p = start;
        int limit = Math.min(n, start + NamedCharacterReferences.MAX_NAME_LENGTH);
        // 영숫자가 이어지는 구간(이름의 최대 길이까지)을 찾는다.
        while (p < limit && isAlnum(in.charAt(p))) {
            p++;
        }
        int run = p - start;
        String replacement = null;
        int matched = 0;
        // 세미콜론으로 끝나는 이름이 표에 있는지 먼저 본다.
        if (p < n && in.charAt(p) == ';' && run < NamedCharacterReferences.MAX_NAME_LENGTH) {
            replacement = NamedCharacterReferences.lookup(in.substring(start, p + 1));
            if (replacement != null) {
                matched = run + 1;
            }
        }
        // 없으면 세미콜론 없는 옛 이름 중 가장 긴 접두사를 찾는다.
        if (replacement == null) {
            for (int len = run; len >= 2; len--) {
                String r = NamedCharacterReferences.lookup(in.substring(start, start + len));
                if (r != null) {
                    replacement = r;
                    matched = len;
                    break;
                }
            }
        }
        if (replacement == null) {
            return null;
        }
        boolean semicolon = in.charAt(start + matched - 1) == ';';
        if (!semicolon && inAnnot) {
            int after = start + matched;
            if (after < n && (in.charAt(after) == '=' || isAlnum(in.charAt(after)))) {
                return null;
            }
        }
        return new Reference(replacement, start + matched);
    }

    /**
     * Consumes a numeric character reference (the position is after the number sign).
     * <p>
     * 숫자 문자 참조를 소비한다(위치는 숫자 기호 뒤).
     *
     * @param in    the text / 텍스트
     * @param start the position after the number sign / 숫자 기호 뒤의 위치
     * @return the reference, or null if no digit follows / 참조. 숫자가 없으면 null
     */
    private static Reference numeric(String in, int start) {
        int n = in.length();
        int p = start;
        boolean hex = false;
        if (p < n && (in.charAt(p) == 'x' || in.charAt(p) == 'X')) {
            hex = true;
            p++;
        }
        int digits = p;
        long code = 0;
        // 숫자를 읽으며 값을 모으고 유니코드 범위를 넘으면 값을 고정한다.
        while (p < n) {
            char d = in.charAt(p);
            int v;
            if (d >= '0' && d <= '9') {
                v = d - '0';
            } else if (hex && d >= 'a' && d <= 'f') {
                v = d - 'a' + 10;
            } else if (hex && d >= 'A' && d <= 'F') {
                v = d - 'A' + 10;
            } else {
                break;
            }
            code = code * (hex ? 16 : 10) + v;
            if (code > 0x110000) {
                code = 0x110000;
            }
            p++;
        }
        if (p == digits) {
            return null;
        }
        if (p < n && in.charAt(p) == ';') {
            p++;
        }
        int cp = (int) code;
        if (cp == 0 || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
            cp = 0xFFFD;
        } else if (cp >= 0x80 && cp <= 0x9F && C1[cp - 0x80] != 0) {
            cp = C1[cp - 0x80];
        }
        return new Reference(new String(Character.toChars(cp)), p);
    }
}
