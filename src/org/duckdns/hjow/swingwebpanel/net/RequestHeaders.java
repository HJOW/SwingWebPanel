package org.duckdns.hjow.swingwebpanel.net;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The additional request headers of one component. Names are matched case-insensitively and a new value overwrites the old
 * one without removing other headers (the position and the spelling of the latest name are kept). The <code>User-Agent</code>
 * header (in any letter case) is never accepted here: it is silently ignored and never reported, because it is managed with
 * setUserAgent. Input is validated as a whole: <code>null</code> maps or entries, keys/values that are not
 * <code>String</code>, empty or illegal names, and values containing CR, LF or NUL reject the complete call with
 * {@link IllegalArgumentException} and nothing is applied. {@link #toMap()} returns a copy.
 * <p>
 * 한 컴포넌트의 추가 요청 헤더. 이름은 대소문자를 구분하지 않고 매칭하며, 새 값은 다른 헤더를 지우지 않고 기존 값을 덮어쓴다
 * (위치는 유지하고 이름 표기는 마지막 입력을 따른다). <code>User-Agent</code> 헤더(대소문자 불문)는 setUserAgent 로 관리하므로
 * 여기서는 받지 않고 조용히 무시하며 조회 결과에도 나오지 않는다. 입력은 전체를 한꺼번에 검증한다. <code>null</code> 맵/항목,
 * <code>String</code> 이 아닌 키/값, 빈 이름이나 잘못된 이름, CR/LF/NUL 을 포함한 값이 하나라도 있으면
 * {@link IllegalArgumentException} 으로 호출 전체를 거부하고 아무것도 적용하지 않는다. {@link #toMap()} 은 복사본을 돌려준다.
 */
public final class RequestHeaders {

    /**
     * Entries keyed by lower-case name; the value array holds the display name and the value.
     * <p>
     * 소문자 이름을 키로 하는 항목. 값 배열은 표시용 이름과 값을 가진다.
     */
    private final Map<String, String[]> entries = new LinkedHashMap<String, String[]>();

    /**
     * Creates an empty header set.
     * <p>
     * 빈 헤더 집합을 만든다.
     */
    public RequestHeaders() {
    }

    /**
     * Applies headers: validates all of them first, then overwrites by case-insensitive name. User-Agent is ignored.
     * <p>
     * 헤더를 적용한다. 먼저 전부 검증한 뒤 대소문자 구분 없는 이름으로 덮어쓴다. User-Agent 는 무시한다.
     *
     * @param headers name/value map (both must be Strings) / 이름/값 맵(둘 다 String 이어야 함)
     * @throws IllegalArgumentException for invalid input; nothing is applied / 잘못된 입력. 아무것도 적용되지 않는다
     */
    public synchronized void addAll(Map<?, ?> headers) {
        if (headers == null) {
            throw new IllegalArgumentException("headers is null");
        }
        // 1단계: 부분 적용을 막기 위해 모든 항목을 먼저 검증한다.
        for (Map.Entry<?, ?> e : headers.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (!(k instanceof String) || !(v instanceof String)) {
                throw new IllegalArgumentException("header name and value must be non-null Strings: " + k + "=" + v);
            }
            validateName((String) k);
            validateValue((String) k, (String) v);
        }
        // 2단계: 검증을 통과한 항목을 적용한다. User-Agent 는 건너뛴다.
        for (Map.Entry<?, ?> e : headers.entrySet()) {
            String name = (String) e.getKey();
            String lower = name.toLowerCase();
            if ("user-agent".equals(lower)) {
                continue;
            }
            entries.put(lower, new String[] { name, (String) e.getValue() });
        }
    }

    /**
     * Validates a header name (an RFC 7230 token).
     * <p>
     * 헤더 이름(RFC 7230 토큰)을 검증한다.
     *
     * @param name header name / 헤더 이름
     */
    private static void validateName(String name) {
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty header name");
        }
        // 토큰에 쓸 수 없는 문자(제어 문자, 공백, 구분자)가 있으면 거부한다.
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c <= 0x20 || c >= 0x7F || "()<>@,;:\\\"/[]?={}".indexOf(c) >= 0) {
                throw new IllegalArgumentException("illegal character in header name: " + name);
            }
        }
    }

    /**
     * Validates a header value (no CR, LF or NUL).
     * <p>
     * 헤더 값을 검증한다(CR, LF, NUL 금지).
     *
     * @param name  header name for messages / 메시지용 헤더 이름
     * @param value header value / 헤더 값
     */
    private static void validateValue(String name, String value) {
        // 헤더 분할 공격을 막기 위해 줄바꿈과 NUL 이 있는 값을 거부한다.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == 0) {
                throw new IllegalArgumentException("illegal control character in value of header " + name);
            }
        }
    }

    /**
     * Returns the value of a header by case-insensitive name.
     * <p>
     * 대소문자 구분 없는 이름으로 헤더 값을 돌려준다.
     *
     * @param name header name / 헤더 이름
     * @return value or null / 값 또는 null
     */
    public synchronized String get(String name) {
        String[] e = entries.get(name.toLowerCase());
        return e == null ? null : e[1];
    }

    /**
     * Returns a copy of the headers in insertion order with the latest spelling of each name.
     * <p>
     * 삽입 순서대로, 이름은 마지막 표기로 된 헤더 복사본을 돌려준다.
     *
     * @return copy of the headers / 헤더 복사본
     */
    public synchronized Map<String, String> toMap() {
        Map<String, String> copy = new LinkedHashMap<String, String>();
        // 표시용 이름과 값을 새 맵에 옮겨 담는다.
        for (String[] e : entries.values()) {
            copy.put(e[0], e[1]);
        }
        return copy;
    }

    /**
     * Returns a copy of this header set.
     * <p>
     * 이 헤더 집합의 복사본을 돌려준다.
     *
     * @return copy / 복사본
     */
    public synchronized RequestHeaders copy() {
        RequestHeaders c = new RequestHeaders();
        // 항목 배열도 복제해 서로 영향이 없게 한다.
        for (Map.Entry<String, String[]> e : entries.entrySet()) {
            c.entries.put(e.getKey(), new String[] { e.getValue()[0], e.getValue()[1] });
        }
        return c;
    }

    /**
     * Removes all headers.
     * <p>
     * 모든 헤더를 지운다.
     */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * Returns the number of headers.
     * <p>
     * 헤더 수를 돌려준다.
     *
     * @return count / 개수
     */
    public synchronized int size() {
        return entries.size();
    }
}
