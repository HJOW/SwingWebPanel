package entitygen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny JSON reader for the build tool: objects become {@link LinkedHashMap}, arrays {@link ArrayList}, strings {@link String},
 * integers {@link Long}, other numbers {@link Double}, <code>true/false</code> {@link Boolean} and <code>null</code> null.
 * <p>
 * 빌드 도구용 작은 JSON 읽기 도구: 객체는 {@link LinkedHashMap}, 배열은 {@link ArrayList}, 문자열은 {@link String}, 정수는 {@link Long},
 * 그 밖의 숫자는 {@link Double}, <code>true/false</code> 는 {@link Boolean}, <code>null</code> 은 null 이 된다.
 */
public final class MiniJson {

    /**
     * Text being read.
     * <p>
     * 읽는 중인 문자열.
     */
    private final String text;

    /**
     * Current read position.
     * <p>
     * 현재 읽는 위치.
     */
    private int pos;

    /**
     * Creates a reader.
     * <p>
     * 읽기 객체를 만든다.
     *
     * @param text JSON text / JSON 문자열
     */
    private MiniJson(String text) {
        this.text = text;
    }

    /**
     * Parses a JSON document.
     * <p>
     * JSON 문서를 해석한다.
     *
     * @param text JSON text / JSON 문자열
     * @return the root value / 최상위 값
     * @throws IllegalArgumentException if the text is not valid JSON / JSON 이 올바르지 않을 때
     */
    public static Object parse(String text) {
        MiniJson j = new MiniJson(text);
        Object v = j.value();
        j.skip();
        if (j.pos != text.length()) {
            throw j.error("trailing characters");
        }
        return v;
    }

    /**
     * Builds an error with the current position.
     * <p>
     * 현재 위치가 담긴 오류를 만든다.
     *
     * @param message description / 설명
     * @return the exception / 예외
     */
    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON error at " + pos + ": " + message);
    }

    /**
     * Skips white space.
     * <p>
     * 공백을 건너뛴다.
     */
    private void skip() {
        // 공백 문자가 끝날 때까지 위치를 앞으로 옮긴다.
        while (pos < text.length() && text.charAt(pos) <= ' ') {
            pos++;
        }
    }

    /**
     * Reads one value.
     * <p>
     * 값 하나를 읽는다.
     *
     * @return the value / 값
     */
    private Object value() {
        skip();
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        char c = text.charAt(pos);
        if (c == '{') {
            return object();
        }
        if (c == '[') {
            return array();
        }
        if (c == '"') {
            return string();
        }
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        return number();
    }

    /**
     * Reads an object.
     * <p>
     * 객체를 읽는다.
     *
     * @return the map / 맵
     */
    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        pos++;
        skip();
        if (text.charAt(pos) == '}') {
            pos++;
            return m;
        }
        // 쉼표로 구분된 "키": 값 쌍을 닫는 중괄호까지 읽는다.
        while (true) {
            skip();
            String key = string();
            skip();
            if (text.charAt(pos) != ':') {
                throw error("':' expected");
            }
            pos++;
            m.put(key, value());
            skip();
            char c = text.charAt(pos++);
            if (c == '}') {
                return m;
            }
            if (c != ',') {
                throw error("',' or '}' expected");
            }
        }
    }

    /**
     * Reads an array.
     * <p>
     * 배열을 읽는다.
     *
     * @return the list / 목록
     */
    private List<Object> array() {
        List<Object> list = new ArrayList<Object>();
        pos++;
        skip();
        if (text.charAt(pos) == ']') {
            pos++;
            return list;
        }
        // 쉼표로 구분된 값을 닫는 대괄호까지 읽는다.
        while (true) {
            list.add(value());
            skip();
            char c = text.charAt(pos++);
            if (c == ']') {
                return list;
            }
            if (c != ',') {
                throw error("',' or ']' expected");
            }
        }
    }

    /**
     * Reads a string literal.
     * <p>
     * 문자열 리터럴을 읽는다.
     *
     * @return the string / 문자열
     */
    private String string() {
        if (text.charAt(pos) != '"') {
            throw error("string expected");
        }
        pos++;
        StringBuilder sb = new StringBuilder();
        // 닫는 따옴표를 만날 때까지 문자와 이스케이프를 읽는다.
        while (true) {
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = text.charAt(pos++);
            switch (e) {
            case 'n':
                sb.append('\n');
                break;
            case 't':
                sb.append('\t');
                break;
            case 'r':
                sb.append('\r');
                break;
            case 'b':
                sb.append('\b');
                break;
            case 'f':
                sb.append('\f');
                break;
            case 'u':
                sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                pos += 4;
                break;
            default:
                sb.append(e);
                break;
            }
        }
    }

    /**
     * Reads a number.
     * <p>
     * 숫자를 읽는다.
     *
     * @return {@link Long} or {@link Double} / {@link Long} 또는 {@link Double}
     */
    private Object number() {
        int start = pos;
        // 숫자를 이루는 문자가 이어지는 동안 위치를 옮긴다.
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw error("value expected");
        }
        String s = text.substring(start, pos);
        if (s.indexOf('.') < 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            return Long.valueOf(Long.parseLong(s));
        }
        return Double.valueOf(Double.parseDouble(s));
    }
}
