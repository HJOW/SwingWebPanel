package org.duckdns.hjow.swingwebpanel.net;

/**
 * A parsed <code>Content-Type</code> header value: the media type (lower-cased, without parameters) and the
 * <code>charset</code> parameter. Parameter names are case-insensitive, values may be quoted, and whitespace around
 * separators is ignored. A value that is not of the form <code>type/subtype</code> yields an empty media type.
 * <p>
 * Use: <code>ContentType.parse("TEXT/HTML ; Charset=\"UTF-8\"").getMime()</code> is <code>text/html</code>.
 * <p>
 * 해석한 <code>Content-Type</code> 헤더 값. 매개변수를 뺀 소문자 미디어 타입과 <code>charset</code> 매개변수를 가진다.
 * 매개변수 이름은 대소문자를 구분하지 않고, 값은 따옴표로 감쌀 수 있으며, 구분자 주위의 공백은 무시한다.
 * <code>type/subtype</code> 형식이 아니면 미디어 타입은 빈 문자열이다.
 * <p>
 * 사용: <code>ContentType.parse("TEXT/HTML ; Charset=\"UTF-8\"").getMime()</code> 은 <code>text/html</code> 이다.
 */
public final class ContentType {

    /**
     * Lower-case media type or "".
     * <p>
     * 소문자 미디어 타입 또는 "".
     */
    private final String mime;
    /**
     * Charset parameter value or null.
     * <p>
     * charset 매개변수 값 또는 null.
     */
    private final String charset;

    /**
     * Creates a parsed value.
     * <p>
     * 해석된 값을 만든다.
     *
     * @param mime    media type / 미디어 타입
     * @param charset charset or null / 문자셋 또는 null
     */
    private ContentType(String mime, String charset) {
        this.mime = mime;
        this.charset = charset;
    }

    /**
     * Parses a header value; null and malformed values give an empty media type.
     * <p>
     * 헤더 값을 해석한다. null 이나 잘못된 값은 빈 미디어 타입이 된다.
     *
     * @param raw header value or null / 헤더 값 또는 null
     * @return parsed value / 해석된 값
     */
    public static ContentType parse(String raw) {
        if (raw == null) {
            return new ContentType("", null);
        }
        String[] parts = splitParameters(raw);
        String type = parts[0].trim().toLowerCase();
        int slash = type.indexOf('/');
        if (slash <= 0 || slash == type.length() - 1 || type.indexOf(' ') >= 0) {
            type = "";
        }
        String cs = null;
        // 첫 조각은 타입이므로 건너뛰고, 나머지 매개변수에서 charset 을 찾는다.
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            int eq = p.indexOf('=');
            if (eq > 0 && "charset".equalsIgnoreCase(p.substring(0, eq).trim())) {
                String v = p.substring(eq + 1).trim();
                if (v.length() >= 2 && v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
                    v = v.substring(1, v.length() - 1);
                }
                if (v.length() > 0) {
                    cs = v;
                }
                break;
            }
        }
        return new ContentType(type, cs);
    }

    /**
     * Splits a header value at semicolons that are outside of quoted strings.
     * <p>
     * 헤더 값을 따옴표 밖의 세미콜론에서 나눈다.
     *
     * @param raw header value / 헤더 값
     * @return pieces / 조각들
     */
    private static String[] splitParameters(String raw) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        // 글자를 하나씩 보며 따옴표 안에서는 세미콜론을 구분자로 보지 않는다.
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if (c == ';' && !quoted) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[out.size()]);
    }

    /**
     * Returns the lower-case media type, "" if absent or malformed.
     * <p>
     * 소문자 미디어 타입을 돌려준다. 없거나 잘못되면 "".
     *
     * @return media type / 미디어 타입
     */
    public String getMime() {
        return mime;
    }

    /**
     * Returns the charset parameter, or null.
     * <p>
     * charset 매개변수를 돌려준다. 없으면 null.
     *
     * @return charset name or null / 문자셋 이름 또는 null
     */
    public String getCharset() {
        return charset;
    }
}
