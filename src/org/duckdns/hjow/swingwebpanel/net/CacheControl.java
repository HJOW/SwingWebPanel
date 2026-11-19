package org.duckdns.hjow.swingwebpanel.net;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The parsed directives of one or more <code>Cache-Control</code> header lines (RFC 9111 section 5.2): comma separated tokens that are
 * either a bare name or <code>name=value</code> where the value may be a quoted string. Names are case-insensitive; the value of a repeated
 * directive is the first one seen. Unknown directives are kept and ignored by the cache.
 * <p>
 * 하나 이상의 <code>Cache-Control</code> 헤더 줄의 해석된 지시어(RFC 9111 5.2): 이름만 있거나 <code>name=value</code> 인 쉼표 구분 토큰이며
 * 값은 따옴표 문자열일 수 있다. 이름은 대소문자를 구분하지 않고 반복된 지시어는 처음 값을 쓴다. 모르는 지시어는 보관하되 캐시가 무시한다.
 */
final class CacheControl {

    /**
     * Directives by lower-case name; a directive without a value maps to the empty string.
     * <p>
     * 소문자 이름별 지시어. 값이 없는 지시어는 빈 문자열이다.
     */
    private final Map<String, String> directives = new HashMap<String, String>();

    /**
     * Creates an empty set of directives.
     * <p>
     * 빈 지시어 집합을 만든다.
     */
    private CacheControl() {
    }

    /**
     * Parses the values of all <code>Cache-Control</code> header lines.
     * <p>
     * 모든 <code>Cache-Control</code> 헤더 줄의 값을 해석한다.
     *
     * @param lines the header values, may be null or empty / 헤더 값들. null 이거나 비어 있어도 된다
     * @return the directives (never null) / 지시어(null 이 아님)
     */
    static CacheControl parse(List<String> lines) {
        CacheControl cc = new CacheControl();
        if (lines == null) {
            return cc;
        }
        // 헤더 줄마다 토큰을 나눈다.
        for (String line : lines) {
            cc.parseLine(line);
        }
        return cc;
    }

    /**
     * Splits one header line at the commas that are not inside a quoted string and records every token.
     * <p>
     * 헤더 한 줄을 따옴표 밖의 쉼표에서 나누고 모든 토큰을 기록한다.
     *
     * @param line the header value / 헤더 값
     */
    private void parseLine(String line) {
        if (line == null) {
            return;
        }
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        // 문자를 하나씩 보며 따옴표 안팎을 추적하고 쉼표에서 토큰을 끝낸다.
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                token.append(c);
            } else if (c == '\\' && quoted && i + 1 < line.length()) {
                token.append(c).append(line.charAt(++i));
            } else if (c == ',' && !quoted) {
                record(token.toString());
                token.setLength(0);
            } else {
                token.append(c);
            }
        }
        record(token.toString());
    }

    /**
     * Records one token.
     * <p>
     * 토큰 하나를 기록한다.
     *
     * @param token <code>name</code> or <code>name=value</code> / <code>name</code> 또는 <code>name=value</code>
     */
    private void record(String token) {
        String t = token.trim();
        if (t.length() == 0) {
            return;
        }
        String name = t;
        String value = "";
        int eq = t.indexOf('=');
        if (eq >= 0) {
            name = t.substring(0, eq).trim();
            value = t.substring(eq + 1).trim();
            // 따옴표로 감싼 값은 따옴표를 벗긴다.
            if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                value = value.substring(1, value.length() - 1);
            }
        }
        name = name.toLowerCase(Locale.ENGLISH);
        if (name.length() > 0 && !directives.containsKey(name)) {
            directives.put(name, value);
        }
    }

    /**
     * Tells whether a directive is present.
     * <p>
     * 지시어가 있는지 알려 준다.
     *
     * @param name lower-case directive name / 소문자 지시어 이름
     * @return true if present (with or without a value) / 있으면 true(값 유무와 무관)
     */
    boolean has(String name) {
        return directives.containsKey(name);
    }

    /**
     * Returns the value of a directive.
     * <p>
     * 지시어의 값을 반환한다.
     *
     * @param name lower-case directive name / 소문자 지시어 이름
     * @return the value, the empty string if it has none, null if absent / 값. 값이 없으면 빈 문자열, 지시어가 없으면 null
     */
    String get(String name) {
        return directives.get(name);
    }

    /**
     * Returns a delta-seconds directive such as <code>max-age</code> (RFC 9111 section 1.2.2): digits only; a value that is too large is
     * capped at {@link Integer#MAX_VALUE} seconds as the RFC allows.
     * <p>
     * <code>max-age</code> 같은 delta-seconds 지시어(RFC 9111 1.2.2)를 반환한다. 숫자만 허용하며 너무 큰 값은 RFC 가 허용하는 대로
     * {@link Integer#MAX_VALUE} 초로 제한한다.
     *
     * @param name lower-case directive name / 소문자 지시어 이름
     * @return seconds, or -1 if the directive is absent or not a valid number / 초. 지시어가 없거나 올바른 숫자가 아니면 -1
     */
    long getSeconds(String name) {
        String v = directives.get(name);
        if (v == null || v.length() == 0) {
            return -1;
        }
        long n = 0;
        // 숫자만 허용하고 상한을 넘으면 상한으로 제한한다.
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
            if (n > Integer.MAX_VALUE) {
                n = Integer.MAX_VALUE;
            }
        }
        return n;
    }
}
