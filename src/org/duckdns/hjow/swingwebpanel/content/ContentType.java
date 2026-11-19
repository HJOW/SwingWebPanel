package org.duckdns.hjow.swingwebpanel.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A parsed <code>Content-Type</code> value (plan item P17-01): the lower-cased <code>type</code> and <code>subtype</code> and all
 * parameters. The algorithm is the "parse a MIME type" algorithm of the WHATWG MIME Sniffing Standard
 * (<code>https://mimesniff.spec.whatwg.org/#parse-a-mime-type</code>): leading and trailing HTTP white space is removed, the type and
 * the subtype must consist of HTTP token characters, parameter names are lower-cased, parameter values may be quoted strings with
 * backslash escapes, parameters without a valid value are skipped and the <em>first</em> parameter of a name wins. A value that does not
 * follow this form (no slash, empty parts, white space inside a part, <code>*&#47;*</code>, several comma separated types) is not valid:
 * {@link #isValid()} is false and {@link #getMime()} is the empty string, which the content layer treats as "the type is missing".
 * <p>
 * Use: <code>ContentType.parse("TEXT/HTML ; Charset=\"UTF-8\"").getMime()</code> is <code>text/html</code> and
 * <code>getCharset()</code> is <code>UTF-8</code>. Instances are immutable and thread safe. The older, smaller
 * <code>org.duckdns.hjow.swingwebpanel.net.ContentType</code> keeps only the media type and the charset; this class additionally keeps every parameter
 * and tells the structured suffix (<code>+xml</code>).
 * <p>
 * 해석한 <code>Content-Type</code> 값(계획 P17-01): 소문자 <code>type</code>, <code>subtype</code> 과 모든 매개변수. 알고리즘은 WHATWG MIME Sniffing
 * 표준의 "parse a MIME type"(<code>https://mimesniff.spec.whatwg.org/#parse-a-mime-type</code>)이다: 앞뒤 HTTP 공백을 제거하고, type 과 subtype 은
 * HTTP 토큰 문자로만 이루어져야 하며, 매개변수 이름은 소문자로 바꾸고, 값은 백슬래시 이스케이프가 있는 따옴표 문자열일 수 있으며, 올바른 값이 없는
 * 매개변수는 건너뛰고 같은 이름은 <em>처음 것</em>이 이긴다. 이 형식을 따르지 않는 값(슬래시 없음, 빈 부분, 부분 안의 공백, <code>*&#47;*</code>,
 * 쉼표로 이은 여러 타입)은 올바르지 않다: {@link #isValid()} 는 false 이고 {@link #getMime()} 은 빈 문자열이며 콘텐츠 계층은 이를 "타입이 없음"으로 본다.
 * <p>
 * 사용: <code>ContentType.parse("TEXT/HTML ; Charset=\"UTF-8\"").getMime()</code> 은 <code>text/html</code> 이고 <code>getCharset()</code> 은
 * <code>UTF-8</code> 이다. 인스턴스는 불변이며 스레드에 안전하다. 더 작은 <code>org.duckdns.hjow.swingwebpanel.net.ContentType</code> 은 미디어 타입과 charset 만
 * 보관하지만, 이 클래스는 모든 매개변수를 보관하고 구조적 접미사(<code>+xml</code>)를 알려 준다.
 */
public final class ContentType {

    /**
     * The value that stands for a missing or malformed header.
     * <p>
     * 없거나 잘못된 헤더를 나타내는 값.
     */
    private static final ContentType INVALID = new ContentType("", "", Collections.<String, String>emptyMap(), false);

    /**
     * Characters (besides letters and digits) that are HTTP token characters.
     * <p>
     * 글자와 숫자 외에 HTTP 토큰 문자인 문자들.
     */
    private static final String TOKEN_SYMBOLS = "!#$%&'*+-.^_`|~";

    /**
     * Lower-case type, "" if invalid.
     * <p>
     * 소문자 type. 올바르지 않으면 "".
     */
    private final String type;

    /**
     * Lower-case subtype, "" if invalid.
     * <p>
     * 소문자 subtype. 올바르지 않으면 "".
     */
    private final String subtype;

    /**
     * Parameters in order of appearance, names in lower case (read only).
     * <p>
     * 나타난 순서의 매개변수. 이름은 소문자(읽기 전용).
     */
    private final Map<String, String> parameters;

    /**
     * True if the header value could be parsed.
     * <p>
     * 헤더 값을 해석할 수 있었으면 true.
     */
    private final boolean valid;

    /**
     * Creates a value.
     * <p>
     * 값을 만든다.
     *
     * @param type       lower-case type / 소문자 type
     * @param subtype    lower-case subtype / 소문자 subtype
     * @param parameters parameters (will be wrapped read only) / 매개변수(읽기 전용으로 감싼다)
     * @param valid      validity flag / 유효 여부
     */
    private ContentType(String type, String subtype, Map<String, String> parameters, boolean valid) {
        this.type = type;
        this.subtype = subtype;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.valid = valid;
    }

    /**
     * Parses a header value. A null, empty or malformed value gives an invalid result (never an exception).
     * <p>
     * 헤더 값을 해석한다. null, 빈 값, 잘못된 값은 예외 없이 올바르지 않은 결과가 된다.
     *
     * @param header header value or null / 헤더 값 또는 null
     * @return the parsed value, never null / 해석한 값(null 아님)
     */
    public static ContentType parse(String header) {
        if (header == null) {
            return INVALID;
        }
        int end = header.length();
        int pos = 0;
        // 앞쪽 HTTP 공백을 건너뛰고 뒤쪽 HTTP 공백을 잘라 낸다.
        while (pos < end && isHttpSpace(header.charAt(pos))) {
            pos++;
        }
        // 뒤쪽의 HTTP 공백을 잘라 낸다.
        while (end > pos && isHttpSpace(header.charAt(end - 1))) {
            end--;
        }
        int slash = header.indexOf('/', pos);
        if (slash < 0 || slash >= end || slash == pos) {
            return INVALID;
        }
        String type = header.substring(pos, slash);
        if (!isToken(type)) {
            return INVALID;
        }
        int semi = header.indexOf(';', slash + 1);
        int subEnd = semi < 0 || semi > end ? end : semi;
        int subTrim = subEnd;
        // subtype 뒤쪽의 HTTP 공백은 제거한다.
        while (subTrim > slash + 1 && isHttpSpace(header.charAt(subTrim - 1))) {
            subTrim--;
        }
        String subtype = header.substring(slash + 1, subTrim);
        if (!isToken(subtype)) {
            return INVALID;
        }
        type = lower(type);
        subtype = lower(subtype);
        if ("*".equals(type) && "*".equals(subtype)) {
            return INVALID;
        }
        Map<String, String> params = new LinkedHashMap<String, String>();
        parseParameters(header, subEnd, end, params);
        return new ContentType(type, subtype, params, true);
    }

    /**
     * Parses the parameter section that starts at a semicolon.
     * <p>
     * 세미콜론에서 시작하는 매개변수 부분을 해석한다.
     *
     * @param s      the whole header value / 헤더 값 전체
     * @param from   index of the first semicolon (or the end if there is none) / 첫 세미콜론의 위치(없으면 끝)
     * @param end    end of the trimmed value / 잘라 낸 값의 끝
     * @param params receives the parameters / 매개변수를 받는 맵
     */
    private static void parseParameters(String s, int from, int end, Map<String, String> params) {
        int pos = from;
        // 매개변수를 하나씩 읽는다. 각 매개변수는 세미콜론으로 시작한다.
        while (pos < end) {
            pos++;
            // 매개변수 이름 앞의 공백을 건너뛴다.
            while (pos < end && isHttpSpace(s.charAt(pos))) {
                pos++;
            }
            int nameStart = pos;
            // 이름은 ';' 또는 '=' 까지이다.
            while (pos < end && s.charAt(pos) != ';' && s.charAt(pos) != '=') {
                pos++;
            }
            String name = lower(s.substring(nameStart, pos));
            if (pos >= end) {
                break;
            }
            if (s.charAt(pos) == ';') {
                // 값이 없는 매개변수는 건너뛴다.
                continue;
            }
            pos++;
            String value;
            boolean quoted = false;
            if (pos < end && s.charAt(pos) == '"') {
                quoted = true;
                StringBuilder sb = new StringBuilder();
                pos++;
                // 닫는 따옴표까지 읽으며 백슬래시 뒤의 글자는 그대로 취한다.
                while (pos < end) {
                    char c = s.charAt(pos);
                    pos++;
                    if (c == '\\') {
                        if (pos < end) {
                            sb.append(s.charAt(pos));
                            pos++;
                        } else {
                            sb.append(c);
                        }
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                value = sb.toString();
                // 닫는 따옴표 뒤에서 다음 ';' 까지는 무시한다.
                while (pos < end && s.charAt(pos) != ';') {
                    pos++;
                }
            } else {
                int valueStart = pos;
                // 따옴표가 없는 값은 ';' 까지이다.
                while (pos < end && s.charAt(pos) != ';') {
                    pos++;
                }
                int valueEnd = pos;
                // 값 뒤쪽의 공백을 잘라 낸다.
                while (valueEnd > valueStart && isHttpSpace(s.charAt(valueEnd - 1))) {
                    valueEnd--;
                }
                value = s.substring(valueStart, valueEnd);
                if (value.length() == 0) {
                    continue;
                }
            }
            boolean valueOk = quoted ? isQuotedStringContent(value) : isToken(value);
            if (name.length() > 0 && isToken(name) && valueOk && !params.containsKey(name)) {
                params.put(name, value);
            }
        }
    }

    /**
     * Tells whether a character is HTTP white space (space, tab, line feed, carriage return).
     * <p>
     * 문자가 HTTP 공백(스페이스, 탭, 줄 바꿈, 캐리지 리턴)인지 알려 준다.
     *
     * @param c character / 문자
     * @return true for white space / 공백이면 true
     */
    private static boolean isHttpSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Tells whether a string is a non-empty sequence of HTTP token characters.
     * <p>
     * 문자열이 비어 있지 않고 HTTP 토큰 문자로만 이루어졌는지 알려 준다.
     *
     * @param s string / 문자열
     * @return true for a token / 토큰이면 true
     */
    private static boolean isToken(String s) {
        if (s.length() == 0) {
            return false;
        }
        // 모든 글자가 토큰 문자인지 확인한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || TOKEN_SYMBOLS.indexOf(c) >= 0;
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the content of a quoted string uses only allowed characters (tab, printable ASCII and the 0x80 to 0xFF range).
     * <p>
     * 따옴표 문자열의 내용이 허용된 문자(탭, 출력 가능한 ASCII, 0x80~0xFF)만 쓰는지 알려 준다.
     *
     * @param s content without the quotes / 따옴표를 뺀 내용
     * @return true if all characters are allowed / 모두 허용되면 true
     */
    private static boolean isQuotedStringContent(String s) {
        // 글자마다 제어 문자와 0xFF 를 넘는 문자가 없는지 확인한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = c == '\t' || c >= 0x20 && c <= 0x7E || c >= 0x80 && c <= 0xFF;
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lower-cases ASCII letters only (the locale and non-ASCII letters never matter for MIME types).
     * <p>
     * ASCII 글자만 소문자로 바꾼다(MIME 타입에서는 로케일과 ASCII 가 아닌 글자는 상관없다).
     *
     * @param s string / 문자열
     * @return the lower-cased string / 소문자로 바꾼 문자열
     */
    private static String lower(String s) {
        StringBuilder sb = null;
        // 대문자 ASCII 글자를 만나면 그때부터 복사본을 만들어 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + 32));
            }
        }
        return sb == null ? s : sb.toString();
    }

    /**
     * Returns the media type of a header value in the form <code>type/subtype</code> in lower case, or "" for a missing or malformed value.
     * This is the shortcut for <code>parse(header).getMime()</code>.
     * <p>
     * 헤더 값의 미디어 타입을 소문자 <code>type/subtype</code> 로 반환한다. 없거나 잘못된 값이면 "". <code>parse(header).getMime()</code> 의 줄임이다.
     *
     * @param header header value or null / 헤더 값 또는 null
     * @return the media type or "" / 미디어 타입 또는 ""
     */
    public static String mimeOf(String header) {
        return parse(header).getMime();
    }

    /**
     * Tells whether the header value could be parsed.
     * <p>
     * 헤더 값을 해석할 수 있었는지 알려 준다.
     *
     * @return true if valid / 올바르면 true
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the lower-case <code>type</code> (for example <code>text</code>), or "" if the value is not valid.
     * <p>
     * 소문자 <code>type</code>(예: <code>text</code>)을 반환한다. 올바르지 않으면 "".
     *
     * @return the type / type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the lower-case <code>subtype</code> (for example <code>xhtml+xml</code>), or "" if the value is not valid.
     * <p>
     * 소문자 <code>subtype</code>(예: <code>xhtml+xml</code>)을 반환한다. 올바르지 않으면 "".
     *
     * @return the subtype / subtype
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * Returns the media type <code>type/subtype</code> in lower case without parameters, or "" if the value is not valid.
     * <p>
     * 매개변수를 뺀 소문자 미디어 타입 <code>type/subtype</code> 을 반환한다. 올바르지 않으면 "".
     *
     * @return the media type or "" / 미디어 타입 또는 ""
     */
    public String getMime() {
        return valid ? type + "/" + subtype : "";
    }

    /**
     * Returns the structured syntax suffix of the subtype without the plus sign, for example <code>xml</code> for
     * <code>application/xhtml+xml</code>, or "" if there is none.
     * <p>
     * subtype 의 구조적 구문 접미사를 더하기 기호 없이 반환한다. 예: <code>application/xhtml+xml</code> 이면 <code>xml</code>. 없으면 "".
     *
     * @return the suffix or "" / 접미사 또는 ""
     */
    public String getSuffix() {
        int plus = subtype.lastIndexOf('+');
        return plus < 0 ? "" : subtype.substring(plus + 1);
    }

    /**
     * Returns the value of a parameter.
     * <p>
     * 매개변수의 값을 반환한다.
     *
     * @param name parameter name, compared ignoring case / 매개변수 이름(대소문자 무시)
     * @return the value (quotes removed) or null / 값(따옴표 제거). 없으면 null
     */
    public String getParameter(String name) {
        return name == null ? null : parameters.get(lower(name));
    }

    /**
     * Returns all parameters (names in lower case, in order of appearance) as a read only map.
     * <p>
     * 모든 매개변수(이름은 소문자, 나타난 순서)를 읽기 전용 맵으로 반환한다.
     *
     * @return the parameters / 매개변수
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Returns the <code>charset</code> parameter, or null if there is none or it is empty. The value is returned as written (a label such
     * as <code>EUC-KR</code>, not yet a canonical encoding name).
     * <p>
     * <code>charset</code> 매개변수를 반환한다. 없거나 비어 있으면 null. 값은 쓰인 그대로 반환한다(<code>EUC-KR</code> 같은 라벨이며 표준 인코딩
     * 이름이 아직 아니다).
     *
     * @return the charset label or null / charset 라벨 또는 null
     */
    public String getCharset() {
        String v = parameters.get("charset");
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.length() == 0 ? null : t;
    }

    /**
     * Tells whether the media type equals a given media type.
     * <p>
     * 미디어 타입이 주어진 미디어 타입과 같은지 알려 준다.
     *
     * @param mime media type in any letter case / 미디어 타입(대소문자 무관)
     * @return true if equal / 같으면 true
     */
    public boolean isMime(String mime) {
        return valid && mime != null && getMime().equals(lower(mime.trim()));
    }

    /**
     * Tells whether this is an XML media type: <code>application/xml</code>, <code>text/xml</code> or any type with the suffix
     * <code>+xml</code>.
     * <p>
     * XML 미디어 타입인지 알려 준다: <code>application/xml</code>, <code>text/xml</code>, 또는 접미사가 <code>+xml</code> 인 모든 타입.
     *
     * @return true for an XML type / XML 타입이면 true
     */
    public boolean isXml() {
        if (!valid) {
            return false;
        }
        boolean base = "xml".equals(subtype) && ("application".equals(type) || "text".equals(type));
        return base || "xml".equals(getSuffix());
    }

    /**
     * Tells whether this is a JSON media type: <code>application/json</code>, <code>text/json</code> or any type with the suffix
     * <code>+json</code>.
     * <p>
     * JSON 미디어 타입인지 알려 준다: <code>application/json</code>, <code>text/json</code>, 또는 접미사가 <code>+json</code> 인 모든 타입.
     *
     * @return true for a JSON type / JSON 타입이면 true
     */
    public boolean isJson() {
        if (!valid) {
            return false;
        }
        boolean base = "json".equals(subtype) && ("application".equals(type) || "text".equals(type));
        return base || "json".equals(getSuffix());
    }

    /**
     * Returns the canonical form: <code>type/subtype</code> followed by <code>; name=value</code> for every parameter; a value is
     * quoted if it is empty or contains a character that is not a token character. An invalid value gives the empty string.
     * <p>
     * 정규 형태를 반환한다: <code>type/subtype</code> 뒤에 매개변수마다 <code>; name=value</code>. 값이 비었거나 토큰 문자가 아닌 문자를 포함하면
     * 따옴표로 감싼다. 올바르지 않은 값은 빈 문자열이다.
     *
     * @return the canonical text / 정규 형태의 문자열
     */
    public String toString() {
        if (!valid) {
            return "";
        }
        StringBuilder sb = new StringBuilder(getMime());
        // 매개변수를 순서대로 덧붙인다. 토큰이 아닌 값은 따옴표로 감싸고 따옴표와 백슬래시를 이스케이프한다.
        for (Map.Entry<String, String> e : parameters.entrySet()) {
            sb.append("; ").append(e.getKey()).append('=');
            String v = e.getValue();
            if (isToken(v)) {
                sb.append(v);
            } else {
                sb.append('"');
                // 값의 글자를 하나씩 쓰며 따옴표와 백슬래시는 이스케이프한다.
                for (int i = 0; i < v.length(); i++) {
                    char c = v.charAt(i);
                    if (c == '"' || c == '\\') {
                        sb.append('\\');
                    }
                    sb.append(c);
                }
                sb.append('"');
            }
        }
        return sb.toString();
    }
}
