package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.html.NamedCharacterReferences;

/**
 * Text helpers shared by the Markdown parsers and the renderer: HTML escaping, backslash and entity unescaping, URL normalization,
 * reference label normalization and the character classes of the CommonMark specification (ASCII punctuation, Unicode punctuation,
 * Unicode whitespace). Named character references are looked up in the HTML named character reference table of the HTML parser package.
 * <p>
 * Markdown 파서와 렌더러가 함께 쓰는 문자열 도우미: HTML 이스케이프, 백슬래시/엔티티 해제, URL 정규화, 참조 라벨 정규화, CommonMark 규격의 문자
 * 분류(ASCII 구두점, 유니코드 구두점, 유니코드 공백). 이름 있는 문자 참조는 HTML 파서 패키지의 표에서 찾는다.
 */
final class MdText {

    /** Characters that stay unchanged in a normalized URL besides letters and digits. 정규화된 URL 에서 영숫자 외에 그대로 두는 문자. */
    private static final String URL_KEEP = ";/?:@&=+$,-_.!~*'()#";

    /** Hex digits for percent encoding. 퍼센트 인코딩에 쓰는 16진 숫자. */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private MdText() {
    }

    /**
     * Tells whether a character is an ASCII punctuation character (the characters a backslash can escape).
     * <p>
     * 문자가 ASCII 구두점인지(백슬래시로 이스케이프할 수 있는 문자인지) 알려 준다.
     *
     * @param c the character / 문자
     * @return true for ASCII punctuation / ASCII 구두점이면 true
     */
    static boolean isAsciiPunctuation(int c) {
        return (c >= 33 && c <= 47) || (c >= 58 && c <= 64) || (c >= 91 && c <= 96) || (c >= 123 && c <= 126);
    }

    /**
     * Tells whether a code point is a Unicode punctuation character in the sense of CommonMark 0.31 (general category P or S).
     * <p>
     * 코드 포인트가 CommonMark 0.31 의 유니코드 구두점(일반 범주 P 또는 S)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for punctuation or symbols / 구두점이나 기호이면 true
     */
    static boolean isPunctuation(int cp) {
        if (cp < 128) {
            return isAsciiPunctuation(cp);
        }
        switch (Character.getType(cp)) {
            case Character.CONNECTOR_PUNCTUATION:
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.MATH_SYMBOL:
            case Character.CURRENCY_SYMBOL:
            case Character.MODIFIER_SYMBOL:
            case Character.OTHER_SYMBOL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Tells whether a code point is Unicode whitespace in the sense of CommonMark (category Zs, tab, line feed, form feed, carriage return).
     * <p>
     * 코드 포인트가 CommonMark 의 유니코드 공백(Zs 범주, 탭, 줄 바꿈, 폼 피드, 캐리지 리턴)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for whitespace / 공백이면 true
     */
    static boolean isUnicodeWhitespace(int cp) {
        if (cp == ' ' || cp == '\t' || cp == '\n' || cp == '\f' || cp == '\r') {
            return true;
        }
        return cp >= 128 && Character.getType(cp) == Character.SPACE_SEPARATOR;
    }

    /**
     * Tells whether a character is a space or tab.
     * <p>
     * 문자가 공백 또는 탭인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for space or tab / 공백이나 탭이면 true
     */
    static boolean isSpaceOrTab(char c) {
        return c == ' ' || c == '\t';
    }

    /**
     * Tells whether a character is one of the ASCII whitespace characters of the HTML grammar (space, tab, line feed, vertical tab,
     * form feed, carriage return).
     * <p>
     * 문자가 HTML 문법의 ASCII 공백(공백, 탭, 줄 바꿈, 수직 탭, 폼 피드, 캐리지 리턴)인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for whitespace / 공백이면 true
     */
    static boolean isAsciiWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r';
    }

    /**
     * Tells whether a string contains only spaces, tabs and line breaks.
     * <p>
     * 문자열이 공백, 탭, 줄 바꿈으로만 이루어졌는지 알려 준다.
     *
     * @param s the string / 문자열
     * @return true if blank / 비어 있거나 공백뿐이면 true
     */
    static boolean isBlank(String s) {
        // 공백이 아닌 문자가 하나라도 있으면 빈 줄이 아니다.
        for (int i = 0; i < s.length(); i++) {
            if (!isAsciiWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes leading and trailing spaces, tabs and line breaks (not other Unicode whitespace such as the no-break space).
     * <p>
     * 앞뒤의 공백, 탭, 줄 바꿈을 지운다(no-break space 같은 다른 유니코드 공백은 지우지 않는다).
     *
     * @param s the string / 문자열
     * @return trimmed string / 다듬은 문자열
     */
    static String trim(String s) {
        int b = 0;
        int e = s.length();
        // 앞쪽의 공백을 건너뛴다.
        while (b < e && isAsciiWhitespace(s.charAt(b))) {
            b++;
        }
        // 뒤쪽의 공백을 건너뛴다.
        while (e > b && isAsciiWhitespace(s.charAt(e - 1))) {
            e--;
        }
        return s.substring(b, e);
    }

    /**
     * Escapes the characters <code>&amp; &lt; &gt; "</code> for HTML text and attribute values.
     * <p>
     * HTML 텍스트와 속성값을 위해 <code>&amp; &lt; &gt; "</code> 문자를 이스케이프한다.
     *
     * @param s the text / 텍스트
     * @return escaped text / 이스케이프한 텍스트
     */
    static String escapeHtml(String s) {
        int i = 0;
        // 이스케이프할 문자가 없으면 원래 문자열을 그대로 돌려준다.
        while (i < s.length() && !needsEscape(s.charAt(i))) {
            i++;
        }
        if (i == s.length()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        escapeHtml(sb, s);
        return sb.toString();
    }

    /**
     * Appends the HTML-escaped text to a builder.
     * <p>
     * 이스케이프한 HTML 텍스트를 빌더에 덧붙인다.
     *
     * @param sb the builder / 빌더
     * @param s  the text / 텍스트
     */
    static void escapeHtml(StringBuilder sb, String s) {
        // 문자마다 이스케이프가 필요한지 보고 필요한 것만 엔티티로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
    }

    /**
     * Tells whether a character must be escaped in HTML output.
     * <p>
     * 문자가 HTML 출력에서 이스케이프해야 하는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true if it needs escaping / 이스케이프가 필요하면 true
     */
    private static boolean needsEscape(char c) {
        return c == '&' || c == '<' || c == '>' || c == '"';
    }

    /**
     * Matches a character reference (<code>&amp;name;</code>, <code>&amp;#123;</code>, <code>&amp;#x1F;</code>) at a position.
     * <p>
     * 위치에서 문자 참조(<code>&amp;name;</code>, <code>&amp;#123;</code>, <code>&amp;#x1F;</code>)를 찾는다.
     *
     * @param s the text / 텍스트
     * @param i position of the ampersand / 앰퍼샌드의 위치
     * @return index after the semicolon, or -1 if there is no well-formed reference / 세미콜론 다음 위치. 올바른 참조가 아니면 -1
     */
    static int matchEntity(String s, int i) {
        int n = s.length();
        int p = i + 1;
        if (p >= n) {
            return -1;
        }
        char c = s.charAt(p);
        if (c == '#') {
            p++;
            if (p < n && (s.charAt(p) == 'x' || s.charAt(p) == 'X')) {
                p++;
                int start = p;
                // 16진 숫자를 최대 6자리까지 읽는다.
                while (p < n && p - start < 7 && isHex(s.charAt(p))) {
                    p++;
                }
                int len = p - start;
                return len >= 1 && len <= 6 && p < n && s.charAt(p) == ';' ? p + 1 : -1;
            }
            int start = p;
            // 10진 숫자를 최대 7자리까지 읽는다.
            while (p < n && p - start < 8 && s.charAt(p) >= '0' && s.charAt(p) <= '9') {
                p++;
            }
            int len = p - start;
            return len >= 1 && len <= 7 && p < n && s.charAt(p) == ';' ? p + 1 : -1;
        }
        if (!isAsciiLetter(c)) {
            return -1;
        }
        int start = p;
        // 이름은 영문자로 시작하고 영숫자가 이어지며 전체 2~32자이다.
        while (p < n && p - start < 33 && isAsciiAlnum(s.charAt(p))) {
            p++;
        }
        int len = p - start;
        return len >= 2 && len <= 32 && p < n && s.charAt(p) == ';' ? p + 1 : -1;
    }

    /**
     * Decodes a character reference that {@link #matchEntity} accepted.
     * <p>
     * {@link #matchEntity} 가 받아들인 문자 참조를 해독한다.
     *
     * @param ref the reference including ampersand and semicolon / 앰퍼샌드와 세미콜론을 포함한 참조
     * @return the decoded text, or the reference itself if the name is unknown / 해독한 텍스트. 알 수 없는 이름이면 참조 자체
     */
    static String decodeEntity(String ref) {
        if (ref.charAt(1) == '#') {
            int cp;
            char x = ref.charAt(2);
            try {
                if (x == 'x' || x == 'X') {
                    cp = Integer.parseInt(ref.substring(3, ref.length() - 1), 16);
                } else {
                    cp = Integer.parseInt(ref.substring(2, ref.length() - 1), 10);
                }
            } catch (NumberFormatException e) {
                cp = 0xFFFD;
            }
            // 0, 서로게이트 범위, 유니코드 범위 밖은 대체 문자로 바꾼다.
            if (cp == 0 || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
                cp = 0xFFFD;
            }
            return new String(Character.toChars(cp));
        }
        String v = NamedCharacterReferences.lookup(ref.substring(1));
        return v == null ? ref : v;
    }

    /**
     * Removes backslash escapes and decodes character references, as done for link destinations, titles and code block info strings.
     * <p>
     * 링크 목적지, 제목, 코드 블록 정보 문자열에 하듯이 백슬래시 이스케이프를 제거하고 문자 참조를 해독한다.
     *
     * @param s the raw text / 원본 텍스트
     * @return unescaped text / 해제한 텍스트
     */
    static String unescapeString(String s) {
        if (s.indexOf('\\') < 0 && s.indexOf('&') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        // 앞에서부터 이스케이프와 문자 참조를 찾아 바꾼다.
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n && isAsciiPunctuation(s.charAt(i + 1))) {
                sb.append(s.charAt(i + 1));
                i += 2;
            } else if (c == '&') {
                int end = matchEntity(s, i);
                if (end < 0) {
                    sb.append(c);
                    i++;
                } else {
                    sb.append(decodeEntity(s.substring(i, end)));
                    i = end;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Percent-encodes the characters of a URL that must not appear in a link destination, keeping existing valid percent escapes
     * and the reserved characters (the behaviour of the reference implementation).
     * <p>
     * 링크 목적지에 올 수 없는 URL 문자를 퍼센트 인코딩하며 이미 올바른 퍼센트 이스케이프와 예약 문자는 그대로 둔다(참조 구현과 같은 동작).
     *
     * @param s the destination text / 목적지 텍스트
     * @return normalized URL / 정규화한 URL
     */
    static String normalizeUri(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        int n = s.length();
        // 문자마다 그대로 둘지 UTF-8 로 퍼센트 인코딩할지 정한다.
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < n && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                sb.append('%');
            } else if (c < 128 && (isAsciiAlnum(c) || URL_KEEP.indexOf(c) >= 0)) {
                sb.append(c);
            } else if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(s.charAt(i + 1))) {
                percentUtf8(sb, Character.toCodePoint(c, s.charAt(i + 1)));
                i++;
            } else if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                percentUtf8(sb, 0xFFFD);
            } else {
                percentUtf8(sb, c);
            }
        }
        return sb.toString();
    }

    /**
     * Appends the UTF-8 bytes of a code point as percent escapes.
     * <p>
     * 코드 포인트의 UTF-8 바이트를 퍼센트 이스케이프로 덧붙인다.
     *
     * @param sb the builder / 빌더
     * @param cp the code point / 코드 포인트
     */
    private static void percentUtf8(StringBuilder sb, int cp) {
        if (cp < 0x80) {
            pct(sb, cp);
        } else if (cp < 0x800) {
            pct(sb, 0xC0 | (cp >> 6));
            pct(sb, 0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            pct(sb, 0xE0 | (cp >> 12));
            pct(sb, 0x80 | ((cp >> 6) & 0x3F));
            pct(sb, 0x80 | (cp & 0x3F));
        } else {
            pct(sb, 0xF0 | (cp >> 18));
            pct(sb, 0x80 | ((cp >> 12) & 0x3F));
            pct(sb, 0x80 | ((cp >> 6) & 0x3F));
            pct(sb, 0x80 | (cp & 0x3F));
        }
    }

    /**
     * Appends one byte as a percent escape.
     * <p>
     * 바이트 하나를 퍼센트 이스케이프로 덧붙인다.
     *
     * @param sb the builder / 빌더
     * @param b  the byte value 0..255 / 바이트 값 0~255
     */
    private static void pct(StringBuilder sb, int b) {
        sb.append('%').append(HEX[(b >> 4) & 15]).append(HEX[b & 15]);
    }

    /**
     * Normalizes a link reference label for matching: trims, collapses runs of whitespace to one space and folds the case
     * (lower case, then upper case, so that for example the German sharp s matches "SS").
     * <p>
     * 링크 참조 라벨을 비교용으로 정규화한다: 앞뒤를 다듬고, 연속한 공백을 공백 하나로 줄이고, 대소문자를 접는다(소문자 후 대문자로 바꾸므로
     * 예를 들어 독일어 ß 는 "SS" 와 같아진다).
     *
     * @param label the label text without brackets / 대괄호를 뺀 라벨 텍스트
     * @return normalized label (empty if it has no non-whitespace character) / 정규화한 라벨(공백뿐이면 빈 문자열)
     */
    static String normalizeReference(String label) {
        StringBuilder sb = new StringBuilder(label.length());
        boolean pendingSpace = false;
        // 공백 문자들을 공백 하나로 합치며 라벨 앞뒤의 공백은 버린다.
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pendingSpace = sb.length() > 0;
            } else {
                if (pendingSpace) {
                    sb.append(' ');
                    pendingSpace = false;
                }
                sb.append(c);
            }
        }
        // JRE 6 의 유니코드 4.0 표에는 대문자 ß(U+1E9E)가 없으므로 직접 SS 로 접는다.
        return sb.toString().replace("" + (char) 0x1E9E, "ss").toLowerCase(Locale.ENGLISH).toUpperCase(Locale.ENGLISH);
    }

    /**
     * Tells whether a normalized destination uses a scheme that must not be followed from a Markdown page.
     * <p>
     * 정규화된 목적지가 Markdown 페이지에서 따라가면 안 되는 스킴을 쓰는지 알려 준다.
     *
     * @param url normalized destination / 정규화된 목적지
     * @return true for javascript:, vbscript:, data: and file: / javascript:, vbscript:, data:, file: 이면 true
     */
    static boolean isUnsafeUrl(String url) {
        return startsWithIgnoreCase(url, "javascript:") || startsWithIgnoreCase(url, "vbscript:") || startsWithIgnoreCase(url, "data:")
                || startsWithIgnoreCase(url, "file:");
    }

    /**
     * Tells whether a string starts with a prefix, ignoring ASCII case.
     * <p>
     * ASCII 대소문자를 무시하고 문자열이 접두어로 시작하는지 알려 준다.
     *
     * @param s      the string / 문자열
     * @param prefix the lower-case prefix / 소문자 접두어
     * @return true if it starts with the prefix / 접두어로 시작하면 true
     */
    static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Tells whether a character is an ASCII letter.
     * <p>
     * 문자가 ASCII 영문자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for A-Z and a-z / A-Z, a-z 이면 true
     */
    static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Tells whether a character is an ASCII letter or digit.
     * <p>
     * 문자가 ASCII 영문자나 숫자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for letters and digits / 영문자나 숫자이면 true
     */
    static boolean isAsciiAlnum(char c) {
        return isAsciiLetter(c) || (c >= '0' && c <= '9');
    }

    /**
     * Tells whether a character is a hexadecimal digit.
     * <p>
     * 문자가 16진 숫자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for 0-9, a-f, A-F / 0-9, a-f, A-F 이면 true
     */
    static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
