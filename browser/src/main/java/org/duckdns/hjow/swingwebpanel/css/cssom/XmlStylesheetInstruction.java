package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pseudo-attributes of an <code>xml-stylesheet</code> processing instruction (Associating Style Sheets with XML documents 1.0,
 * https://www.w3.org/TR/xml-stylesheet/, W3C Recommendation 2010-10-28), which XHTML documents use to link a style sheet:
 * <code>&lt;?xml-stylesheet type="text/css" href="style.css" media="screen" title="Main" alternate="no"?&gt;</code>. The data of the
 * instruction is a sequence of <code>name="value"</code> pairs (single or double quotes), the five predefined entities and the character
 * references are decoded in the values, and an unreadable instruction has no pseudo-attributes at all.
 * <p>
 * <code>xml-stylesheet</code> 처리 명령의 의사 속성(Associating Style Sheets with XML documents 1.0, W3C 권고안 2010-10-28). XHTML 문서가 스타일 시트를 연결하는 데 쓴다:
 * <code>&lt;?xml-stylesheet type="text/css" href="style.css" media="screen" title="Main" alternate="no"?&gt;</code>. 처리 명령의 데이터는 <code>name="value"</code> 쌍(작은따옴표나
 * 큰따옴표)의 나열이며, 값의 미리 정의된 엔티티 다섯 개와 문자 참조는 풀어 주고, 읽을 수 없는 처리 명령은 의사 속성이 하나도 없다.
 */
public final class XmlStylesheetInstruction {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private XmlStylesheetInstruction() {
    }

    /**
     * Parses the data of the instruction into its pseudo-attributes. The first occurrence of a name wins; the names are case-sensitive.
     * <p>
     * 처리 명령의 데이터를 의사 속성으로 해석한다. 같은 이름은 처음 것이 이기며 이름은 대소문자를 구분한다.
     *
     * @param data the data of the processing instruction, may be null / 처리 명령의 데이터. null 일 수 있다
     * @return the pseudo-attributes in order, empty if the data is not a sequence of name-value pairs / 순서대로의 의사 속성. 데이터가 이름-값 쌍의 나열이 아니면 비어 있다
     */
    public static Map<String, String> parse(String data) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (data == null) {
            return out;
        }
        int i = 0;
        int n = data.length();
        // 쌍을 하나씩 읽는다: 공백, 이름, =, 따옴표로 둘러싼 값.
        while (true) {
            while (i < n && isSpace(data.charAt(i))) {
                i++;
            }
            if (i >= n) {
                return out;
            }
            int start = i;
            // 이름은 = 나 공백 앞까지이다.
            while (i < n && data.charAt(i) != '=' && !isSpace(data.charAt(i))) {
                i++;
            }
            String name = data.substring(start, i);
            // 이름 뒤의 공백을 건너뛴다.
            while (i < n && isSpace(data.charAt(i))) {
                i++;
            }
            if (name.length() == 0 || i >= n || data.charAt(i) != '=') {
                return new LinkedHashMap<String, String>();
            }
            i++;
            // = 뒤의 공백을 건너뛴다.
            while (i < n && isSpace(data.charAt(i))) {
                i++;
            }
            if (i >= n || (data.charAt(i) != '"' && data.charAt(i) != '\'')) {
                return new LinkedHashMap<String, String>();
            }
            char quote = data.charAt(i++);
            int end = data.indexOf(quote, i);
            if (end < 0) {
                return new LinkedHashMap<String, String>();
            }
            if (!out.containsKey(name)) {
                out.put(name, decode(data.substring(i, end)));
            }
            i = end + 1;
        }
    }

    /**
     * Tells whether a character is XML white space.
     * <p>
     * 글자가 XML 공백인지 알려 준다.
     *
     * @param c the character / 글자
     * @return true for space, tab, line feed and carriage return / 공백, 탭, 줄 바꿈, 캐리지 리턴이면 true
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Decodes the predefined entities and the character references of a pseudo-attribute value; a reference that cannot be read stays as it is.
     * <p>
     * 의사 속성 값의 미리 정의된 엔티티와 문자 참조를 푼다. 읽을 수 없는 참조는 그대로 둔다.
     *
     * @param s the value as written / 쓰인 대로의 값
     * @return the decoded value / 푼 값
     */
    static String decode(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        // 글자를 하나씩 보며 & 로 시작하는 참조를 푼다.
        while (i < s.length()) {
            char c = s.charAt(i);
            int semi = c == '&' ? s.indexOf(';', i) : -1;
            if (semi < 0) {
                sb.append(c);
                i++;
                continue;
            }
            String ref = s.substring(i + 1, semi);
            String value = null;
            if ("lt".equals(ref)) {
                value = "<";
            } else if ("gt".equals(ref)) {
                value = ">";
            } else if ("amp".equals(ref)) {
                value = "&";
            } else if ("quot".equals(ref)) {
                value = "\"";
            } else if ("apos".equals(ref)) {
                value = "'";
            } else if (ref.length() > 1 && ref.charAt(0) == '#') {
                try {
                    int cp = ref.charAt(1) == 'x' ? Integer.parseInt(ref.substring(2), 16) : Integer.parseInt(ref.substring(1));
                    value = new String(Character.toChars(cp));
                } catch (IllegalArgumentException e) {
                    value = null;
                }
            }
            if (value == null) {
                sb.append(c);
                i++;
            } else {
                sb.append(value);
                i = semi + 1;
            }
        }
        return sb.toString();
    }
}
