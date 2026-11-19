package org.duckdns.hjow.swingwebpanel.html;

/**
 * The encoding sniffing algorithm of the HTML Standard ("determining the character encoding") for documents that arrive as bytes.
 * The order of decision is:
 * <ol>
 * <li>a byte order mark (UTF-8, UTF-16LE, UTF-16BE) - certain;</li>
 * <li>the <code>charset</code> label of the transport layer (HTTP Content-Type) - certain, if the label is known;</li>
 * <li>the prescan of the first 1024 bytes for <code>&lt;meta charset&gt;</code> or <code>&lt;meta http-equiv="content-type"&gt;</code> -
 * tentative (the tree builder confirms or corrects it later);</li>
 * <li>an extension of this implementation: if none of the above applies and the whole text is valid UTF-8 that contains multi-byte
 * sequences, UTF-8 is assumed (tentative); this avoids garbled Korean text in pages that declare nothing;</li>
 * <li>the fallback encoding, <code>windows-1252</code> unless the caller names another one - tentative.</li>
 * </ol>
 * Decoding never fails: malformed input becomes U+FFFD. Pages declared as UTF-16 by a <code>meta</code> element are decoded as UTF-8
 * and <code>x-user-defined</code> as windows-1252, as the standard requires.
 * <p>
 * 바이트로 도착하는 문서에 대한 HTML 표준의 인코딩 판정 알고리즘("determining the character encoding"). 판정 순서는 다음과 같다.
 * <ol>
 * <li>바이트 순서 표시(UTF-8, UTF-16LE, UTF-16BE) - 확실;</li>
 * <li>전송 계층(HTTP Content-Type)의 <code>charset</code> 라벨 - 라벨을 알 수 있으면 확실;</li>
 * <li>처음 1024 바이트에서 <code>&lt;meta charset&gt;</code> 또는 <code>&lt;meta http-equiv="content-type"&gt;</code> 를 찾는 사전 검사 - 잠정적
 * (트리 생성기가 나중에 확인하거나 바로잡는다);</li>
 * <li>이 구현의 확장: 위 어느 것도 해당하지 않고 전체 텍스트가 멀티바이트 순서가 있는 올바른 UTF-8 이면 UTF-8 로 가정(잠정적).
 * 아무것도 선언하지 않은 페이지에서 한글이 깨지는 것을 막는다;</li>
 * <li>대체 인코딩. 호출자가 다른 것을 지정하지 않으면 <code>windows-1252</code> - 잠정적.</li>
 * </ol>
 * 디코딩은 실패하지 않는다: 잘못된 입력은 U+FFFD 가 된다. <code>meta</code> 요소가 UTF-16 으로 선언한 페이지는 표준대로 UTF-8 로,
 * <code>x-user-defined</code> 는 windows-1252 로 디코딩한다.
 */
public final class EncodingSniffer {

    /**
     * Number of bytes that the meta prescan looks at.
     * <p>
     * meta 사전 검사가 보는 바이트 수.
     */
    public static final int PRESCAN_LIMIT = 1024;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EncodingSniffer() {
    }

    /**
     * Decodes a document with the default fallback encoding (windows-1252).
     * <p>
     * 기본 대체 인코딩(windows-1252)으로 문서를 디코딩한다.
     *
     * @param bytes       document bytes / 문서 바이트
     * @param httpCharset the <code>charset</code> parameter value of the Content-Type header, or null / Content-Type 헤더의
     *                    <code>charset</code> 매개변수 값 또는 null
     * @return the decoded text with the decision / 판정이 담긴 디코딩 결과
     */
    public static DecodedText decode(byte[] bytes, String httpCharset) {
        return decode(bytes, httpCharset, null);
    }

    /**
     * Decodes a document.
     * <p>
     * 문서를 디코딩한다.
     *
     * @param bytes       document bytes / 문서 바이트
     * @param httpCharset the <code>charset</code> parameter value of the Content-Type header, or null / Content-Type 헤더의
     *                    <code>charset</code> 매개변수 값 또는 null
     * @param fallback    label of the encoding to use when nothing else decides (for example <code>euc-kr</code> for a Korean user
     *                    locale), or null for windows-1252 / 아무것도 정하지 못했을 때 쓸 인코딩의 라벨(예: 한국어 로케일의 <code>euc-kr</code>),
     *                    windows-1252 이면 null
     * @return the decoded text with the decision / 판정이 담긴 디코딩 결과
     */
    public static DecodedText decode(byte[] bytes, String httpCharset, String fallback) {
        // 1단계: 바이트 순서 표시가 있으면 그것이 확실한 판정이다.
        String bom = bomEncoding(bytes);
        if (bom != null) {
            return new DecodedText(EncodingLabels.decode(bytes, bomLength(bytes), bom), bytes, bom, true, DecodedText.SOURCE_BOM);
        }
        // 2단계: 전송 계층의 charset 라벨이 알려진 것이면 확실한 판정이다.
        String http = EncodingLabels.canonicalName(httpCharset);
        if (http != null && !EncodingLabels.REPLACEMENT.equals(http) && EncodingLabels.isDecodable(http)) {
            return new DecodedText(EncodingLabels.decode(bytes, 0, http), bytes, http, true, DecodedText.SOURCE_HTTP);
        }
        // 3단계: 처음 1024 바이트에서 meta 선언을 찾는다(잠정적).
        String meta = prescan(bytes);
        if (meta != null && EncodingLabels.isDecodable(meta)) {
            return new DecodedText(EncodingLabels.decode(bytes, 0, meta), bytes, meta, false, DecodedText.SOURCE_META);
        }
        // 4단계: 멀티바이트가 있는 올바른 UTF-8 이면 UTF-8 로 추측한다(이 구현의 확장).
        if (looksLikeUtf8(bytes)) {
            return new DecodedText(EncodingLabels.decode(bytes, 0, EncodingLabels.UTF_8), bytes, EncodingLabels.UTF_8, false,
                    DecodedText.SOURCE_GUESS);
        }
        // 5단계: 대체 인코딩을 쓴다.
        String fb = EncodingLabels.canonicalName(fallback);
        if (fb == null || !EncodingLabels.isDecodable(fb)) {
            fb = EncodingLabels.WINDOWS_1252;
        }
        return new DecodedText(EncodingLabels.decode(bytes, 0, fb), bytes, fb, false, DecodedText.SOURCE_DEFAULT);
    }

    /**
     * Returns the encoding indicated by a byte order mark at the start of the bytes ("BOM sniffing").
     * <p>
     * 바이트 앞의 바이트 순서 표시가 나타내는 인코딩을 반환한다("BOM sniffing").
     *
     * @param b bytes / 바이트
     * @return <code>UTF-8</code>, <code>UTF-16BE</code>, <code>UTF-16LE</code> or null / 해당 이름 또는 null
     */
    public static String bomEncoding(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
            return EncodingLabels.UTF_8;
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
            return EncodingLabels.UTF_16BE;
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
            return EncodingLabels.UTF_16LE;
        }
        return null;
    }

    /**
     * Returns the length in bytes of the byte order mark at the start of the bytes, or 0 if there is none.
     * <p>
     * 바이트 앞의 바이트 순서 표시의 길이(바이트)를 반환한다. 없으면 0.
     *
     * @param b bytes / 바이트
     * @return 3, 2 or 0 / 3, 2 또는 0
     */
    public static int bomLength(byte[] b) {
        String e = bomEncoding(b);
        if (e == null) {
            return 0;
        }
        return EncodingLabels.UTF_8.equals(e) ? 3 : 2;
    }

    /**
     * Extracts the <code>charset</code> parameter value of a Content-Type header value, for example <code>euc-kr</code> from
     * <code>text/html; charset="EUC-KR"</code>.
     * <p>
     * Content-Type 헤더 값에서 <code>charset</code> 매개변수 값을 뽑는다. 예: <code>text/html; charset="EUC-KR"</code> 에서 <code>EUC-KR</code>.
     *
     * @param contentType header value or null / 헤더 값 또는 null
     * @return the label (quotes removed) or null / 라벨(따옴표 제거) 또는 null
     */
    public static String charsetOfContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String s = contentType;
        int i = 0;
        // 세미콜론으로 구분된 매개변수를 차례로 살펴 charset 을 찾는다.
        while (true) {
            int semi = s.indexOf(';', i);
            if (semi < 0) {
                return null;
            }
            int start = semi + 1;
            int end = s.indexOf(';', start);
            String param = end < 0 ? s.substring(start) : s.substring(start, end);
            int eq = param.indexOf('=');
            if (eq > 0 && "charset".equalsIgnoreCase(param.substring(0, eq).trim())) {
                String v = param.substring(eq + 1).trim();
                if (v.length() >= 2 && (v.charAt(0) == '"' || v.charAt(0) == '\'') && v.charAt(v.length() - 1) == v.charAt(0)) {
                    v = v.substring(1, v.length() - 1);
                }
                return v.length() == 0 ? null : v;
            }
            if (end < 0) {
                return null;
            }
            i = end;
        }
    }

    /**
     * Tells whether the bytes are valid UTF-8 that contains at least one multi-byte sequence.
     * <p>
     * 바이트가 멀티바이트 순서를 하나 이상 포함한 올바른 UTF-8 인지 알려 준다.
     *
     * @param b bytes / 바이트
     * @return true if so / 그러면 true
     */
    static boolean looksLikeUtf8(byte[] b) {
        boolean multi = false;
        int i = 0;
        // 바이트를 앞에서부터 UTF-8 순서 단위로 검사한다.
        while (i < b.length) {
            int c = b[i] & 0xFF;
            if (c < 0x80) {
                i++;
                continue;
            }
            int need;
            int min;
            if (c >= 0xC2 && c <= 0xDF) {
                need = 1;
                min = 0x80;
            } else if (c >= 0xE0 && c <= 0xEF) {
                need = 2;
                min = 0x800;
            } else if (c >= 0xF0 && c <= 0xF4) {
                need = 3;
                min = 0x10000;
            } else {
                return false;
            }
            if (i + need >= b.length) {
                // 순서가 중간에 끊긴 입력이다.
                return false;
            }
            int cp = c & (0x3F >> need);
            // 이어지는 바이트가 10xxxxxx 형식인지 확인하며 값을 모은다.
            for (int k = 1; k <= need; k++) {
                int cc = b[i + k] & 0xFF;
                if ((cc & 0xC0) != 0x80) {
                    return false;
                }
                cp = (cp << 6) | (cc & 0x3F);
            }
            if (cp < min || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
                return false;
            }
            multi = true;
            i += need + 1;
        }
        return multi;
    }

    /**
     * Runs the prescan algorithm of the HTML Standard ("prescan a byte stream to determine its encoding") over the first
     * {@link #PRESCAN_LIMIT} bytes and returns the encoding declared by the first usable <code>meta</code> element.
     * <p>
     * HTML 표준의 사전 검사 알고리즘("prescan a byte stream to determine its encoding")을 처음 {@link #PRESCAN_LIMIT} 바이트에 실행해, 처음으로
     * 쓸 수 있는 <code>meta</code> 요소가 선언한 인코딩을 반환한다.
     *
     * @param bytes document bytes / 문서 바이트
     * @return canonical encoding name or null / 표준 인코딩 이름 또는 null
     */
    public static String prescan(byte[] bytes) {
        int len = Math.min(bytes.length, PRESCAN_LIMIT);
        int pos = 0;
        // 위치를 앞으로 옮기며 주석, meta, 다른 태그, 선언을 구분해 처리한다.
        while (pos < len) {
            if (bytes[pos] != '<') {
                pos++;
                continue;
            }
            if (startsWith(bytes, pos, len, "<!--")) {
                // 주석: 첫 "-->" 의 끝까지 건너뛴다(여는 "<!--" 의 대시도 "-->" 의 일부가 될 수 있다).
                int p = pos + 2;
                int end = -1;
                // '-->' 를 찾을 때까지 한 바이트씩 앞으로 간다.
                while (p + 2 < len) {
                    if (bytes[p] == '-' && bytes[p + 1] == '-' && bytes[p + 2] == '>') {
                        end = p + 2;
                        break;
                    }
                    p++;
                }
                if (end < 0) {
                    return null;
                }
                pos = end + 1;
                continue;
            }
            if (startsWithIgnoreCase(bytes, pos, len, "<meta") && pos + 5 < len && isSpaceOrSlash(bytes[pos + 5])) {
                int[] cursor = {pos + 5};
                String found = prescanMeta(bytes, len, cursor);
                if (found != null) {
                    return found;
                }
                pos = cursor[0];
                continue;
            }
            if (pos + 1 < len && isAsciiAlpha(bytes[pos + 1]) || (pos + 2 < len && bytes[pos + 1] == '/' && isAsciiAlpha(bytes[pos + 2]))) {
                // 그 밖의 태그: 이름을 건너뛰고 속성들을 읽어 넘긴다.
                int p = bytes[pos + 1] == '/' ? pos + 2 : pos + 1;
                // 태그 이름은 공백, '>' 또는 '/' 를 만날 때까지이다.
                while (p < len && !isSpace(bytes[p]) && bytes[p] != '>') {
                    p++;
                }
                int[] cursor = {p};
                // 속성이 더 없을 때까지 하나씩 읽는다.
                while (readAttribute(bytes, len, cursor, null)) {
                    continue;
                }
                pos = cursor[0];
                continue;
            }
            if (pos + 1 < len && (bytes[pos + 1] == '!' || bytes[pos + 1] == '/' || bytes[pos + 1] == '?')) {
                // 선언, 잘못된 종료 태그, 처리 명령: 첫 '>' 까지 건너뛴다.
                int p = pos + 1;
                while (p < len && bytes[p] != '>') {
                    p++;
                }
                pos = p + 1;
                continue;
            }
            pos++;
        }
        return null;
    }

    /**
     * Handles one <code>&lt;meta</code> element during the prescan: it reads the attributes and applies the
     * <code>charset</code>/<code>http-equiv</code>/<code>content</code> rules.
     * <p>
     * 사전 검사 중 <code>&lt;meta</code> 요소 하나를 처리한다: 속성을 읽고 <code>charset</code>/<code>http-equiv</code>/<code>content</code> 규칙을 적용한다.
     *
     * @param bytes  bytes / 바이트
     * @param len    number of bytes to consider / 대상 바이트 수
     * @param cursor one element array with the position; updated / 위치가 든 한 칸짜리 배열(갱신됨)
     * @return the canonical encoding name, or null if this element declares none / 표준 인코딩 이름. 이 요소가 선언하지 않았으면 null
     */
    private static String prescanMeta(byte[] bytes, int len, int[] cursor) {
        boolean gotPragma = false;
        Boolean needPragma = null;
        String charset = null;
        StringBuilder seen = new StringBuilder(",");
        String[] nv = new String[2];
        // 속성을 하나씩 읽되 같은 이름은 처음 것만 쓴다.
        while (readAttribute(bytes, len, cursor, nv)) {
            String name = nv[0];
            String value = nv[1];
            if (seen.indexOf("," + name + ",") >= 0) {
                continue;
            }
            seen.append(name).append(',');
            if ("http-equiv".equals(name)) {
                if ("content-type".equals(value)) {
                    gotPragma = true;
                }
            } else if ("content".equals(name)) {
                String ext = extractFromMetaContent(value);
                if (charset == null && ext != null) {
                    charset = ext;
                    needPragma = Boolean.TRUE;
                }
            } else if ("charset".equals(name)) {
                charset = EncodingLabels.canonicalName(value);
                needPragma = Boolean.FALSE;
            }
        }
        if (needPragma == null || (needPragma.booleanValue() && !gotPragma) || charset == null) {
            return null;
        }
        if (EncodingLabels.UTF_16BE.equals(charset) || EncodingLabels.UTF_16LE.equals(charset)) {
            return EncodingLabels.UTF_8;
        }
        if (EncodingLabels.X_USER_DEFINED.equals(charset)) {
            return EncodingLabels.WINDOWS_1252;
        }
        return charset;
    }

    /**
     * Extracts the canonical encoding from the <code>content</code> attribute value of a <code>meta</code> element ("algorithm for
     * extracting a character encoding from a meta element"), for example from <code>text/html; charset=euc-kr</code>.
     * <p>
     * <code>meta</code> 요소의 <code>content</code> 속성 값에서 표준 인코딩을 뽑는다("algorithm for extracting a character encoding from a meta
     * element"). 예: <code>text/html; charset=euc-kr</code>.
     *
     * @param value attribute value / 속성 값
     * @return canonical encoding name or null / 표준 인코딩 이름 또는 null
     */
    public static String extractFromMetaContent(String value) {
        String lower = value.toLowerCase(java.util.Locale.ENGLISH);
        int pos = 0;
        // "charset" 이 나오는 모든 위치에서 뒤에 '=' 가 이어지는지 확인한다.
        while (true) {
            int at = lower.indexOf("charset", pos);
            if (at < 0) {
                return null;
            }
            int p = at + 7;
            // 'charset' 뒤의 공백을 건너뛴다.
            while (p < value.length() && isAsciiWhitespace(value.charAt(p))) {
                p++;
            }
            if (p >= value.length() || value.charAt(p) != '=') {
                pos = at + 1;
                continue;
            }
            p++;
            // '=' 뒤의 공백을 건너뛴다.
            while (p < value.length() && isAsciiWhitespace(value.charAt(p))) {
                p++;
            }
            if (p >= value.length()) {
                return null;
            }
            char q = value.charAt(p);
            String label;
            if (q == '"' || q == '\'') {
                int end = value.indexOf(q, p + 1);
                if (end < 0) {
                    return null;
                }
                label = value.substring(p + 1, end);
            } else {
                int end = p;
                // 따옴표가 없으면 공백이나 세미콜론 앞까지가 값이다.
                while (end < value.length() && !isAsciiWhitespace(value.charAt(end)) && value.charAt(end) != ';') {
                    end++;
                }
                label = value.substring(p, end);
            }
            return EncodingLabels.canonicalName(label);
        }
    }

    /**
     * Reads one attribute ("get an attribute") during the prescan. Names and values are lower-cased.
     * <p>
     * 사전 검사 중 속성 하나를 읽는다("get an attribute"). 이름과 값은 소문자로 바뀐다.
     *
     * @param b      bytes / 바이트
     * @param len    number of bytes to consider / 대상 바이트 수
     * @param cursor one element array with the position; updated / 위치가 든 한 칸짜리 배열(갱신됨)
     * @param out    two element array that receives name and value, or null / 이름과 값을 받는 두 칸 배열 또는 null
     * @return true if an attribute was read, false at the end of the tag / 속성을 읽었으면 true, 태그의 끝이면 false
     */
    private static boolean readAttribute(byte[] b, int len, int[] cursor, String[] out) {
        int p = cursor[0];
        // 속성 앞의 공백과 슬래시를 건너뛴다.
        while (p < len && (isSpace(b[p]) || b[p] == '/')) {
            p++;
        }
        if (p >= len || b[p] == '>') {
            cursor[0] = p >= len ? len : p + 1;
            return false;
        }
        StringBuilder name = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean hasValue = false;
        // 이름을 읽는다: '=' 를 만나면 값으로, 공백이면 '=' 가 뒤따르는지 보고, '/' 나 '>' 에서 끝낸다.
        while (true) {
            if (p >= len) {
                cursor[0] = len;
                return false;
            }
            int c = b[p] & 0xFF;
            if (c == '=' && name.length() > 0) {
                p++;
                hasValue = true;
                break;
            }
            if (isSpace(b[p])) {
                // 공백 뒤에 '=' 가 없으면 값 없는 속성이다.
                while (p < len && isSpace(b[p])) {
                    p++;
                }
                if (p < len && b[p] == '=') {
                    p++;
                    hasValue = true;
                }
                break;
            }
            if (c == '/' || c == '>') {
                break;
            }
            name.append(toLower(c));
            p++;
        }
        if (hasValue) {
            // 값 앞의 공백을 건너뛰고 따옴표 여부에 따라 값을 읽는다.
            while (p < len && isSpace(b[p])) {
                p++;
            }
            if (p < len && (b[p] == '"' || b[p] == '\'')) {
                byte q = b[p++];
                // 같은 따옴표가 나올 때까지가 값이다.
                while (p < len && b[p] != q) {
                    value.append(toLower(b[p] & 0xFF));
                    p++;
                }
                p++;
            } else {
                // 따옴표가 없으면 공백이나 '>' 앞까지가 값이다.
                while (p < len && !isSpace(b[p]) && b[p] != '>') {
                    value.append(toLower(b[p] & 0xFF));
                    p++;
                }
            }
        }
        cursor[0] = Math.min(p, len);
        if (out != null) {
            out[0] = name.toString();
            out[1] = value.toString();
        }
        return true;
    }

    /**
     * Lower-cases an ASCII byte value into a char (other values are returned unchanged as the Latin-1 character).
     * <p>
     * ASCII 바이트 값을 소문자 char 로 바꾼다(다른 값은 그대로 Latin-1 문자가 된다).
     *
     * @param c byte value / 바이트 값
     * @return character / 문자
     */
    private static char toLower(int c) {
        return (char) (c >= 'A' && c <= 'Z' ? c + 32 : c);
    }

    /**
     * Tells whether a byte is ASCII white space or a slash used as a separator in the prescan.
     * <p>
     * 바이트가 ASCII 공백이거나 사전 검사에서 구분자로 쓰이는 슬래시인지 알려 준다.
     *
     * @param b byte / 바이트
     * @return true if so / 그러면 true
     */
    private static boolean isSpaceOrSlash(byte b) {
        return isSpace(b) || b == '/';
    }

    /**
     * Tells whether a byte is ASCII white space (tab, LF, FF, CR, space).
     * <p>
     * 바이트가 ASCII 공백(탭, LF, FF, CR, 공백)인지 알려 준다.
     *
     * @param b byte / 바이트
     * @return true if so / 그러면 true
     */
    private static boolean isSpace(byte b) {
        return b == ' ' || b == '\t' || b == '\n' || b == '\f' || b == '\r';
    }

    /**
     * Tells whether a character is ASCII white space (tab, LF, FF, CR, space).
     * <p>
     * 문자가 ASCII 공백(탭, LF, FF, CR, 공백)인지 알려 준다.
     *
     * @param c character / 문자
     * @return true if so / 그러면 true
     */
    private static boolean isAsciiWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Tells whether a byte is an ASCII letter.
     * <p>
     * 바이트가 ASCII 알파벳인지 알려 준다.
     *
     * @param b byte / 바이트
     * @return true if so / 그러면 true
     */
    private static boolean isAsciiAlpha(byte b) {
        return (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z');
    }

    /**
     * Tells whether the bytes at a position start with an ASCII string (case-sensitive).
     * <p>
     * 위치의 바이트가 ASCII 문자열로 시작하는지 알려 준다(대소문자 구분).
     *
     * @param b   bytes / 바이트
     * @param pos position / 위치
     * @param len number of bytes to consider / 대상 바이트 수
     * @param s   ASCII string / ASCII 문자열
     * @return true if it matches / 일치하면 true
     */
    private static boolean startsWith(byte[] b, int pos, int len, String s) {
        if (pos + s.length() > len) {
            return false;
        }
        // 문자열의 글자를 하나씩 비교한다.
        for (int i = 0; i < s.length(); i++) {
            if (b[pos + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the bytes at a position start with a lower-case ASCII string, ignoring ASCII case.
     * <p>
     * 위치의 바이트가 소문자 ASCII 문자열로 시작하는지 ASCII 대소문자를 무시하고 알려 준다.
     *
     * @param b   bytes / 바이트
     * @param pos position / 위치
     * @param len number of bytes to consider / 대상 바이트 수
     * @param s   lower-case ASCII string / 소문자 ASCII 문자열
     * @return true if it matches / 일치하면 true
     */
    private static boolean startsWithIgnoreCase(byte[] b, int pos, int len, String s) {
        if (pos + s.length() > len) {
            return false;
        }
        // 문자열의 글자를 소문자로 맞춰 하나씩 비교한다.
        for (int i = 0; i < s.length(); i++) {
            if (toLower(b[pos + i] & 0xFF) != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
