package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.ArrayList;
import java.util.List;

/**
 * A cursor over a string for the SVG micro syntaxes (plan item P18-04): numbers as in the SVG grammar (<code>10-20</code> is two numbers,
 * <code>1.5.5</code> is two numbers), separators (white space and commas), identifiers and single characters. It never throws; reading
 * methods return false or null when the text does not match and leave the position where it was.
 * <p>
 * SVG 의 작은 문법(계획 P18-04)을 위한 문자열 커서: SVG 문법의 수(<code>10-20</code> 은 수 두 개, <code>1.5.5</code> 도 수 두 개), 구분자(공백과
 * 쉼표), 식별자, 한 글자. 예외를 던지지 않는다. 읽기 메서드는 텍스트가 맞지 않으면 false 또는 null 을 반환하고 위치를 그대로 둔다.
 */
final class SvgScanner {

    /**
     * The text.
     * <p>
     * 텍스트.
     */
    final String s;

    /**
     * The current position.
     * <p>
     * 현재 위치.
     */
    int p;

    /**
     * The value of the last number read by {@link #number()}.
     * <p>
     * {@link #number()} 가 마지막으로 읽은 수의 값.
     */
    double value;

    /**
     * Creates a scanner at the start of a text.
     * <p>
     * 텍스트의 처음에서 시작하는 스캐너를 만든다.
     *
     * @param s the text / 텍스트
     */
    SvgScanner(String s) {
        this.s = s;
    }

    /**
     * Tells whether the end of the text was reached.
     * <p>
     * 텍스트의 끝에 도달했는지 알려 준다.
     *
     * @return true at the end / 끝이면 true
     */
    boolean atEnd() {
        return p >= s.length();
    }

    /**
     * Returns the current character, or 0 at the end.
     * <p>
     * 현재 문자를 반환하며 끝이면 0 이다.
     *
     * @return character / 문자
     */
    char peek() {
        return p < s.length() ? s.charAt(p) : 0;
    }

    /**
     * Tells whether a character is white space of the SVG grammar.
     * <p>
     * 문자가 SVG 문법의 공백인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for space, tab, CR, LF, form feed / 스페이스, 탭, CR, LF, 폼 피드이면 true
     */
    static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f';
    }

    /**
     * Skips white space.
     * <p>
     * 공백을 건너뛴다.
     */
    void skipWs() {
        // 공백이 이어지는 동안 건너뛴다.
        while (p < s.length() && isWs(s.charAt(p))) {
            p++;
        }
    }

    /**
     * Skips white space and at most one comma (the separator of number lists).
     * <p>
     * 공백과 쉼표 하나(수 목록의 구분자)를 건너뛴다.
     */
    void skipSep() {
        skipWs();
        if (p < s.length() && s.charAt(p) == ',') {
            p++;
            skipWs();
        }
    }

    /**
     * Skips white space and any number of commas.
     * <p>
     * 공백과 모든 쉼표를 건너뛴다.
     */
    void skipWsAndCommas() {
        // 공백이나 쉼표가 이어지는 동안 건너뛴다.
        while (p < s.length() && (isWs(s.charAt(p)) || s.charAt(p) == ',')) {
            p++;
        }
    }

    /**
     * Reads a number at the current position into {@link #value}.
     * <p>
     * 현재 위치의 수를 {@link #value} 로 읽는다.
     *
     * @return true if a number was read / 수를 읽었으면 true
     */
    boolean number() {
        int q = p;
        int n = s.length();
        if (q < n && (s.charAt(q) == '+' || s.charAt(q) == '-')) {
            q++;
        }
        int digits = 0;
        // 정수부 숫자를 읽는다.
        while (q < n && s.charAt(q) >= '0' && s.charAt(q) <= '9') {
            q++;
            digits++;
        }
        if (q < n && s.charAt(q) == '.') {
            int save = q;
            q++;
            int frac = 0;
            // 소수부 숫자를 읽는다.
            while (q < n && s.charAt(q) >= '0' && s.charAt(q) <= '9') {
                q++;
                frac++;
            }
            if (frac == 0 && digits == 0) {
                q = save;
            }
            digits += frac;
        }
        if (digits == 0) {
            return false;
        }
        // 지수부 'e' 다음에 숫자가 있을 때만 지수로 읽는다(단위 em, ex 와 구분).
        if (q < n && (s.charAt(q) == 'e' || s.charAt(q) == 'E')) {
            int e = q + 1;
            if (e < n && (s.charAt(e) == '+' || s.charAt(e) == '-')) {
                e++;
            }
            int ed = 0;
            // 지수의 숫자를 읽는다.
            while (e < n && s.charAt(e) >= '0' && s.charAt(e) <= '9') {
                e++;
                ed++;
            }
            if (ed > 0) {
                q = e;
            }
        }
        try {
            value = Double.parseDouble(s.substring(p, q));
        } catch (NumberFormatException ex) {
            return false;
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return false;
        }
        p = q;
        return true;
    }

    /**
     * Reads a run of letters (and percent signs) and returns it (empty if none).
     * <p>
     * 알파벳(과 퍼센트 기호) 문자열을 읽어 반환한다(없으면 빈 문자열).
     *
     * @return the letters / 문자열
     */
    String letters() {
        int q = p;
        // 영문자와 '%' 가 이어지는 동안 읽는다.
        while (q < s.length() && (Character.isLetter(s.charAt(q)) || s.charAt(q) == '%')) {
            q++;
        }
        String r = s.substring(p, q);
        p = q;
        return r;
    }

    /**
     * Consumes a character if it is the next one.
     * <p>
     * 다음 문자가 같으면 소비한다.
     *
     * @param c the expected character / 기대하는 문자
     * @return true if consumed / 소비했으면 true
     */
    boolean accept(char c) {
        if (p < s.length() && s.charAt(p) == c) {
            p++;
            return true;
        }
        return false;
    }

    /**
     * Parses a whole text as a list of numbers separated by white space and commas.
     * <p>
     * 텍스트 전체를 공백과 쉼표로 구분된 수 목록으로 해석한다.
     *
     * @param text the text / 텍스트
     * @return the numbers, or null if the text contains anything else / 수 배열. 다른 내용이 있으면 null
     */
    static double[] numberList(String text) {
        if (text == null) {
            return null;
        }
        SvgScanner sc = new SvgScanner(text);
        List<Double> list = new ArrayList<Double>();
        sc.skipWsAndCommas();
        // 수를 끝까지 하나씩 읽는다.
        while (!sc.atEnd()) {
            if (!sc.number()) {
                return null;
            }
            list.add(Double.valueOf(sc.value));
            sc.skipWsAndCommas();
        }
        double[] r = new double[list.size()];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < r.length; i++) {
            r[i] = list.get(i).doubleValue();
        }
        return r;
    }
}
