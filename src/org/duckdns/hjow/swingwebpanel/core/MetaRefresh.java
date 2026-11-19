package org.duckdns.hjow.swingwebpanel.core;

/**
 * The timed redirect of a <code>&lt;meta http-equiv="refresh" content="..."&gt;</code> element (HTML5 REC 2014-10-28 section 4.2.5.3,
 * "Refresh state", https://www.w3.org/TR/2014/REC-html5-20141028/document-metadata.html#attr-meta-http-equiv-refresh, retrieved
 * 2026-11-17). The value of <code>content</code> is a number of seconds, optionally followed by <code>;</code> or <code>,</code> and
 * <code>url=</code> and an address, with or without quotes. Without an address the page is loaded again. The parsing follows the
 * algorithm of the specification step by step, which accepts much more than the conformance rules allow (<code>1.5; URL = 'a.html'</code>,
 * <code>3;a.html</code> that is a reload because the part after the separator is not <code>url=</code>); a value that has no number is
 * ignored.
 * <p>
 * <code>&lt;meta http-equiv="refresh" content="..."&gt;</code> 요소의 예약된 이동(HTML5 REC 2014-10-28 §4.2.5.3 "Refresh state"). <code>content</code> 의 값은 초 수이고
 * 그 뒤에 <code>;</code> 나 <code>,</code>, <code>url=</code>, 따옴표가 있거나 없는 주소가 올 수 있다. 주소가 없으면 페이지를 다시 읽는다. 해석은 규격의 알고리즘을
 * 한 단계씩 따르므로 적합성 규칙이 허용하는 것보다 훨씬 많이 받아들이며(<code>1.5; URL = 'a.html'</code>, 구분자 뒤가 <code>url=</code> 이 아니라서 다시 읽기가 되는
 * <code>3;a.html</code>), 숫자가 없는 값은 무시한다.
 */
final class MetaRefresh {

    /**
     * The seconds to wait (saturated at the largest value of an int).
     * <p>
     * 기다릴 초(int 의 최댓값으로 포화).
     */
    private final int seconds;

    /**
     * The address as written, or null for the address of the current page.
     * <p>
     * 쓰인 대로의 주소. 현재 페이지의 주소이면 null.
     */
    private final String url;

    /**
     * Creates the result.
     * <p>
     * 결과를 만든다.
     *
     * @param seconds the seconds / 초
     * @param url     the address as written or null / 쓰인 대로의 주소 또는 null
     */
    private MetaRefresh(int seconds, String url) {
        this.seconds = seconds;
        this.url = url;
    }

    /**
     * Returns the seconds to wait after the document has completely loaded.
     * <p>
     * 문서가 완전히 로드된 뒤 기다릴 초를 반환한다.
     *
     * @return the seconds, not negative / 초. 음수가 아니다
     */
    int getSeconds() {
        return seconds;
    }

    /**
     * Returns the address as written (it is resolved against the base URL of the document when the refresh is scheduled).
     * <p>
     * 쓰인 대로의 주소를 반환한다(예약할 때 문서의 기준 URL 로 해석한다).
     *
     * @return the address, or null for the page itself / 주소. 페이지 자신이면 null
     */
    String getUrl() {
        return url;
    }

    /**
     * Parses the value of the <code>content</code> attribute.
     * <p>
     * <code>content</code> 속성의 값을 해석한다.
     *
     * @param content the value, may be null / 값. null 일 수 있다
     * @return the result, or null if the value has no number and so no refresh / 결과. 숫자가 없어 새로고침이 없으면 null
     */
    static MetaRefresh parse(String content) {
        if (content == null || content.length() == 0) {
            return null;
        }
        int n = content.length();
        int pos = skipSpace(content, 0);
        long time = 0;
        int digits = 0;
        // 숫자를 모은다(오버플로를 막으려고 int 최댓값에서 멈춘다).
        while (pos < n && content.charAt(pos) >= '0' && content.charAt(pos) <= '9') {
            time = Math.min(time * 10 + (content.charAt(pos) - '0'), Integer.MAX_VALUE);
            digits++;
            pos++;
        }
        if (digits == 0) {
            return null;
        }
        // 숫자와 "." 로 된 나머지(소수 부분)는 버린다.
        while (pos < n && ((content.charAt(pos) >= '0' && content.charAt(pos) <= '9') || content.charAt(pos) == '.')) {
            pos++;
        }
        pos = skipSpace(content, pos);
        if (pos >= n || (content.charAt(pos) != ';' && content.charAt(pos) != ',')) {
            return new MetaRefresh((int) time, null);
        }
        pos = skipSpace(content, pos + 1);
        // "url" 은 대소문자를 구분하지 않고 한 글자씩 읽으며 하나라도 다르면 현재 페이지를 다시 읽는다.
        String word = "url";
        for (int i = 0; i < word.length(); i++) {
            if (pos >= n || Character.toLowerCase(content.charAt(pos)) != word.charAt(i)) {
                return new MetaRefresh((int) time, null);
            }
            pos++;
        }
        pos = skipSpace(content, pos);
        if (pos >= n || content.charAt(pos) != '=') {
            return new MetaRefresh((int) time, null);
        }
        pos = skipSpace(content, pos + 1);
        char quote = 0;
        if (pos < n && (content.charAt(pos) == '\'' || content.charAt(pos) == '"')) {
            quote = content.charAt(pos);
            pos++;
        }
        String address = content.substring(pos);
        // 따옴표가 있으면 닫는 따옴표에서 자른다.
        if (quote != 0) {
            int close = address.indexOf(quote);
            if (close >= 0) {
                address = address.substring(0, close);
            }
        }
        int end = address.length();
        // 끝의 공백 문자를 지운다.
        while (end > 0 && isSpace(address.charAt(end - 1))) {
            end--;
        }
        address = address.substring(0, end);
        StringBuilder clean = new StringBuilder();
        // 탭, 줄 바꿈, 캐리지 리턴을 지운다.
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (c != '\t' && c != '\n' && c != '\r') {
                clean.append(c);
            }
        }
        return new MetaRefresh((int) time, clean.toString());
    }

    /**
     * Skips the white space (space, tab, line feed, form feed, carriage return) from a position.
     * <p>
     * 위치에서부터 공백(스페이스, 탭, 줄 바꿈, 폼 피드, 캐리지 리턴)을 건너뛴다.
     *
     * @param s   the text / 문자열
     * @param pos the position / 위치
     * @return the position of the first character that is not white space / 공백이 아닌 첫 글자의 위치
     */
    private static int skipSpace(String s, int pos) {
        int i = pos;
        // 공백이 아닌 글자를 만날 때까지 나아간다.
        while (i < s.length() && isSpace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * Tells whether a character is white space for the algorithm.
     * <p>
     * 알고리즘에서 글자가 공백인지 알려 준다.
     *
     * @param c the character / 글자
     * @return true for space, tab, line feed, form feed and carriage return / 스페이스, 탭, 줄 바꿈, 폼 피드, 캐리지 리턴이면 true
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }
}
