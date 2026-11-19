package org.duckdns.hjow.swingwebpanel.js.text;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link JsonText} and {@link JsonTokenizer}. JSON escapes are written with a doubled backslash in the Java
 * source so that the text reaching the code contains a backslash followed by the letter u.
 * <p>
 * {@link JsonText} 와 {@link JsonTokenizer} 의 시험. JSON escape 는 Java 소스에서 역슬래시를 두 번 써서
 * 코드에 도달하는 텍스트가 역슬래시와 글자 u 로 이루어지게 한다.
 */
public class JsonTextTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public JsonTextTest() {
    }

    /**
     * Checks QuoteJSONString in both modes.
     * <p>
     * 두 방식의 QuoteJSONString 을 확인한다.
     */
    public void testQuote() {
        Check.equal("\"abc\"", JsonText.quote("abc"), "plain");
        Check.equal("\"a\\\"b\\\\c\\n\\t\\b\\f\\r\"", JsonText.quote("a\"b\\c\n\t\b\f\r"), "named escapes");
        Check.equal("\"\\u0001\\u001f\"", JsonText.quote("" + (char) 1 + (char) 0x1F), "control characters use lower case hex");
        Check.equal("\"/\"", JsonText.quote("/"), "slash is not escaped");
        Check.equal("\"" + (char) 0x2028 + (char) 0x2029 + "\"", JsonText.quote("" + (char) 0x2028 + (char) 0x2029),
                "line and paragraph separators are not escaped");
        Check.equal("\"" + (char) 0xD800 + "\"", JsonText.quote("" + (char) 0xD800), "ES2015 keeps lone surrogates");
        Check.equal("\"\\ud800\"", JsonText.quoteWellFormed("" + (char) 0xD800), "well formed escapes a lone high surrogate");
        Check.equal("\"\\udc00\"", JsonText.quoteWellFormed("" + (char) 0xDC00), "well formed escapes a lone low surrogate");
        String pair = new String(Character.toChars(0x1F600));
        Check.equal("\"" + pair + "\"", JsonText.quoteWellFormed(pair), "valid pairs are kept");
        Check.equal("\"\"", JsonText.quote(""), "empty");
    }

    /**
     * Reads all tokens of a text into a compact signature string.
     * <p>
     * 텍스트의 모든 토큰을 간단한 서명 문자열로 읽는다.
     *
     * @param json JSON text / JSON 텍스트
     * @return signature such as "[1,s:a]" / "[1,s:a]" 같은 서명
     */
    private static String tokens(String json) {
        JsonTokenizer t = new JsonTokenizer(json);
        StringBuilder sb = new StringBuilder();
        // 입력 끝(EOF)까지 토큰을 읽어 종류별 표시를 남긴다.
        while (true) {
            int k = t.next();
            if (k == JsonTokenizer.EOF) {
                break;
            }
            switch (k) {
            case JsonTokenizer.BEGIN_OBJECT:
                sb.append('{');
                break;
            case JsonTokenizer.END_OBJECT:
                sb.append('}');
                break;
            case JsonTokenizer.BEGIN_ARRAY:
                sb.append('[');
                break;
            case JsonTokenizer.END_ARRAY:
                sb.append(']');
                break;
            case JsonTokenizer.COLON:
                sb.append(':');
                break;
            case JsonTokenizer.COMMA:
                sb.append(',');
                break;
            case JsonTokenizer.STRING:
                sb.append("s(").append(t.getStringValue()).append(')');
                break;
            case JsonTokenizer.NUMBER:
                sb.append("n(").append(t.getNumberText()).append(')');
                break;
            case JsonTokenizer.TRUE:
                sb.append('T');
                break;
            case JsonTokenizer.FALSE:
                sb.append('F');
                break;
            default:
                sb.append('N');
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Checks token production and string decoding.
     * <p>
     * 토큰 생성과 문자열 해독을 확인한다.
     */
    public void testTokens() {
        Check.equal("{s(a):[n(1),n(-2.5e+3),T,F,N]}", tokens("{\"a\" : [1, -2.5e+3, true, false, null]}"), "mixed tokens");
        Check.equal("s(aA\n/)", tokens("\"a\\u0041\\n\\/\""), "escapes decode");
        Check.equal("s(" + (char) 0xD83D + (char) 0xDE00 + ")", tokens("\"\\ud83d\\ude00\""), "surrogate escapes decode to a pair");
        Check.equal("s(\"\\)", tokens("\"\\\"\\\\\""), "quote and backslash");
        Check.equal("n(0)n(-0)n(0.5E+10)", tokens("0 -0 0.5E+10"), "numbers");
        Check.equal("s(" + (char) 0x2028 + ")", tokens("\"" + (char) 0x2028 + "\""), "U+2028 is allowed unescaped in strings");
        Check.equal("", tokens(" \t\r\n "), "only white space");
    }

    /**
     * Expects a JSON syntax error from validate.
     * <p>
     * validate 가 JSON 구문 오류를 내는지 확인한다.
     *
     * @param json invalid text / 올바르지 않은 텍스트
     */
    private static void bad(final String json) {
        Check.throwsException(JsonSyntaxException.class, new Block() {
            /**
             * Validates the invalid text.
             * <p>
             * 올바르지 않은 텍스트를 검사한다.
             */
            public void run() {
                JsonTokenizer.validate(json);
            }
        }, "must be invalid: " + json);
    }

    /**
     * Checks the strict grammar through validate.
     * <p>
     * validate 로 엄격한 문법을 확인한다.
     */
    public void testValidate() {
        String[] good = { "[]", "{}", "0", "-0", "1e5", "\"\"", "null", "true", " [ 1 , 2.5e-3 , \"x\" , true , false , null , {\"a\":[{}]} ] ",
            "{\"a\":{\"b\":[1,[2,[3]]]}}", "[[],[[]],{}]" };
        // 올바른 각 텍스트가 오류 없이 검사를 통과하는지 확인한다.
        for (int i = 0; i < good.length; i++) {
            JsonTokenizer.validate(good[i]);
        }
        String[] invalid = { "", " ", "01", "1.", ".5", "+1", "'a'", "[1,]", "{\"a\":1,}", "[1 2]", "{\"a\" 1}", "{1:2}", "{\"a\":}",
            "\"abc", "\"a\\x\"", "\"a\\u12\"", "\"a\nb\"", "nul", "tru", "falsy", "-", "1e", "1e+", "[", "{", "[1", "]", "}", "[}", "{]",
            "1 2", "[1],", "{\"a\":1}}", String.valueOf((char) 0), "\"\\u00zz\"" };
        // 올바르지 않은 각 텍스트가 오류를 내는지 확인한다.
        for (int i = 0; i < invalid.length; i++) {
            bad(invalid[i]);
        }
        bad("" + (char) 0xA0 + "1");
        bad("\"" + (char) 0x1F + "\"");
        bad("[\"a\",\"b\"" + (char) 0xFEFF + "]");
    }

    /**
     * Checks error positions and messages.
     * <p>
     * 오류 위치와 메시지를 확인한다.
     */
    public void testErrorPositions() {
        try {
            JsonTokenizer.validate("[1, x]");
            Check.fail("expected a syntax error");
        } catch (JsonSyntaxException e) {
            Check.equal(4, e.getPosition(), "position of the bad token");
            Check.equal("Unexpected token x in JSON at position 4", e.getMessage(), "message");
        }
        try {
            JsonTokenizer.validate("[1,");
            Check.fail("expected a syntax error");
        } catch (JsonSyntaxException e) {
            Check.equal(3, e.getPosition(), "position at the end");
            Check.equal("Unexpected end of JSON input", e.getMessage(), "end message");
        }
    }

    /**
     * Checks that deep nesting does not overflow the stack.
     * <p>
     * 깊은 중첩이 스택을 넘치게 하지 않는지 확인한다.
     */
    public void testDeepNesting() {
        int depth = 100000;
        StringBuilder sb = new StringBuilder();
        // 대괄호를 깊이만큼 열고 닫는다.
        for (int i = 0; i < depth; i++) {
            sb.append('[');
        }
        // 닫는 대괄호를 깊이만큼 붙인다.
        for (int i = 0; i < depth; i++) {
            sb.append(']');
        }
        JsonTokenizer.validate(sb.toString());
        bad(sb.substring(1));
    }
}
