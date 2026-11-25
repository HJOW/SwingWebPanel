package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * Pull tokenizer for strict JSON text (RFC 8259 / ECMA-404 grammar as used by <code>JSON.parse</code>), independent
 * of JavaScript values. White space is limited to space, tab, line feed and carriage return. Numbers must match
 * <code>-?(0|[1-9][0-9]*)(.[0-9]+)?([eE][+-]?[0-9]+)?</code>; strings reject unescaped control characters and unknown
 * escapes. The tokenizer returns tokens only; the caller (or {@link #validate(String)}) checks the structure.
 * The number lexeme is returned as text so that the caller converts it with its own number conversion.
 * Instances are not thread-safe.
 * <p>
 * 엄격한 JSON 텍스트(<code>JSON.parse</code> 가 쓰는 RFC 8259 / ECMA-404 문법)용 당겨 읽기 토큰화기. JavaScript 값과 무관하다.
 * 공백은 스페이스, 탭, 줄 바꿈, 캐리지 리턴뿐이다. 숫자는 <code>-?(0|[1-9][0-9]*)(.[0-9]+)?([eE][+-]?[0-9]+)?</code> 여야 하고,
 * 문자열은 escape 되지 않은 제어 문자와 알 수 없는 escape 를 거부한다. 토큰만 돌려주며 구조 검사는 호출자(또는 {@link #validate(String)})가 한다.
 * 숫자는 호출자가 자기 숫자 변환으로 바꾸도록 텍스트 그대로 반환한다. 인스턴스는 스레드 안전하지 않다.
 */
public final class JsonTokenizer {

    /**
     * Token type: end of input.
     * <p>
     * 토큰 종류: 입력 끝.
     */
    public static final int EOF = 0;

    /**
     * Token type: left brace.
     * <p>
     * 토큰 종류: 왼쪽 중괄호.
     */
    public static final int BEGIN_OBJECT = 1;

    /**
     * Token type: right brace.
     * <p>
     * 토큰 종류: 오른쪽 중괄호.
     */
    public static final int END_OBJECT = 2;

    /**
     * Token type: left bracket.
     * <p>
     * 토큰 종류: 왼쪽 대괄호.
     */
    public static final int BEGIN_ARRAY = 3;

    /**
     * Token type: right bracket.
     * <p>
     * 토큰 종류: 오른쪽 대괄호.
     */
    public static final int END_ARRAY = 4;

    /**
     * Token type: colon.
     * <p>
     * 토큰 종류: 콜론.
     */
    public static final int COLON = 5;

    /**
     * Token type: comma.
     * <p>
     * 토큰 종류: 쉼표.
     */
    public static final int COMMA = 6;

    /**
     * Token type: string (value via {@link #getStringValue()}).
     * <p>
     * 토큰 종류: 문자열(값은 {@link #getStringValue()}).
     */
    public static final int STRING = 7;

    /**
     * Token type: number (lexeme via {@link #getNumberText()}).
     * <p>
     * 토큰 종류: 숫자(어휘는 {@link #getNumberText()}).
     */
    public static final int NUMBER = 8;

    /**
     * Token type: the literal true.
     * <p>
     * 토큰 종류: 리터럴 true.
     */
    public static final int TRUE = 9;

    /**
     * Token type: the literal false.
     * <p>
     * 토큰 종류: 리터럴 false.
     */
    public static final int FALSE = 10;

    /**
     * Token type: the literal null.
     * <p>
     * 토큰 종류: 리터럴 null.
     */
    public static final int NULL = 11;

    /**
     * The JSON text.
     * <p>
     * JSON 텍스트.
     */
    private final String text;

    /**
     * Current scan offset.
     * <p>
     * 현재 읽는 위치.
     */
    private int pos;

    /**
     * Start offset of the current token.
     * <p>
     * 현재 토큰의 시작 위치.
     */
    private int tokenStart;

    /**
     * Decoded value of the current STRING token.
     * <p>
     * 현재 STRING 토큰의 해독한 값.
     */
    private String stringValue;

    /**
     * Lexeme of the current NUMBER token.
     * <p>
     * 현재 NUMBER 토큰의 어휘.
     */
    private String numberText;

    /**
     * Creates a tokenizer over a text.
     * <p>
     * 텍스트 위의 토큰화기를 만든다.
     *
     * @param text JSON text / JSON 텍스트
     */
    public JsonTokenizer(String text) {
        this.text = text;
    }

    /**
     * Returns the start offset of the current token.
     * <p>
     * 현재 토큰의 시작 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getTokenStart() {
        return tokenStart;
    }

    /**
     * Returns the exclusive end offset of the current token.
     * <p>
     * 현재 토큰의 끝 다음 위치를 반환한다.
     *
     * @return exclusive offset / 끝 다음 위치
     */
    public int getTokenEnd() {
        return pos;
    }

    /**
     * Returns the decoded string of the current STRING token.
     * <p>
     * 현재 STRING 토큰의 해독한 문자열을 반환한다.
     *
     * @return string value / 문자열 값
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Returns the lexeme of the current NUMBER token.
     * <p>
     * 현재 NUMBER 토큰의 어휘를 반환한다.
     *
     * @return number text / 숫자 텍스트
     */
    public String getNumberText() {
        return numberText;
    }

    /**
     * Reads the next token.
     * <p>
     * 다음 토큰을 읽는다.
     *
     * @return token type constant / 토큰 종류 상수
     * @throws JsonSyntaxException on invalid text / 올바르지 않은 텍스트이면
     */
    public int next() {
        int n = text.length();
        // JSON 공백(스페이스, 탭, 줄 바꿈, 캐리지 리턴)을 건너뛴다.
        while (pos < n) {
            char w = text.charAt(pos);
            if (w == ' ' || w == '\t' || w == '\n' || w == '\r') {
                pos++;
            } else {
                break;
            }
        }
        tokenStart = pos;
        if (pos >= n) {
            return EOF;
        }
        char c = text.charAt(pos);
        switch (c) {
        case '{':
            pos++;
            return BEGIN_OBJECT;
        case '}':
            pos++;
            return END_OBJECT;
        case '[':
            pos++;
            return BEGIN_ARRAY;
        case ']':
            pos++;
            return END_ARRAY;
        case ':':
            pos++;
            return COLON;
        case ',':
            pos++;
            return COMMA;
        case '"':
            readString();
            return STRING;
        case 't':
            expectWord("true");
            return TRUE;
        case 'f':
            expectWord("false");
            return FALSE;
        case 'n':
            expectWord("null");
            return NULL;
        default:
            if (c == '-' || (c >= '0' && c <= '9')) {
                readNumber();
                return NUMBER;
            }
            throw unexpected(pos);
        }
    }

    /**
     * Builds the exception for an unexpected character or end of input.
     * <p>
     * 예상하지 못한 문자 또는 입력 끝에 대한 예외를 만든다.
     *
     * @param at offset / 위치
     * @return exception to throw / 던질 예외
     */
    private JsonSyntaxException unexpected(int at) {
        if (at >= text.length()) {
            return new JsonSyntaxException("Unexpected end of JSON input", text.length());
        }
        return new JsonSyntaxException("Unexpected token " + text.charAt(at) + " in JSON at position " + at, at);
    }

    /**
     * Checks that a literal word follows and consumes it.
     * <p>
     * 리터럴 단어가 이어지는지 확인하고 소비한다.
     *
     * @param word expected word / 기대하는 단어
     */
    private void expectWord(String word) {
        // 단어의 각 글자가 차례로 일치하는지 확인한다.
        for (int i = 0; i < word.length(); i++) {
            if (pos + i >= text.length() || text.charAt(pos + i) != word.charAt(i)) {
                throw unexpected(pos + i);
            }
        }
        pos += word.length();
    }

    /**
     * Reads a number lexeme with the strict JSON grammar.
     * <p>
     * 엄격한 JSON 문법으로 숫자 어휘를 읽는다.
     */
    private void readNumber() {
        int start = pos;
        int n = text.length();
        if (text.charAt(pos) == '-') {
            pos++;
        }
        if (pos >= n) {
            throw unexpected(pos);
        }
        char c = text.charAt(pos);
        if (c == '0') {
            pos++;
        } else if (c >= '1' && c <= '9') {
            // 0 이 아닌 숫자로 시작하면 숫자가 이어지는 동안 읽는다.
            while (pos < n && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') {
                pos++;
            }
        } else {
            throw unexpected(pos);
        }
        if (pos < n && text.charAt(pos) == '.') {
            pos++;
            requireDigits();
        }
        if (pos < n && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            pos++;
            if (pos < n && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            requireDigits();
        }
        numberText = text.substring(start, pos);
    }

    /**
     * Consumes one or more decimal digits.
     * <p>
     * 10진 숫자를 하나 이상 소비한다.
     */
    private void requireDigits() {
        int n = text.length();
        if (pos >= n || text.charAt(pos) < '0' || text.charAt(pos) > '9') {
            throw unexpected(pos);
        }
        // 숫자가 이어지는 동안 읽는다.
        while (pos < n && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') {
            pos++;
        }
    }

    /**
     * Reads a string literal and decodes its escapes.
     * <p>
     * 문자열 리터럴을 읽고 escape 를 해독한다.
     */
    private void readString() {
        int n = text.length();
        pos++;
        StringBuilder sb = new StringBuilder();
        // 닫는 따옴표까지 문자를 하나씩 읽으며 escape 를 해독한다.
        while (true) {
            if (pos >= n) {
                throw unexpected(pos);
            }
            char c = text.charAt(pos);
            if (c == '"') {
                pos++;
                stringValue = sb.toString();
                return;
            }
            if (c < 0x20) {
                throw unexpected(pos);
            }
            if (c != '\\') {
                sb.append(c);
                pos++;
                continue;
            }
            pos++;
            if (pos >= n) {
                throw unexpected(pos);
            }
            char e = text.charAt(pos);
            switch (e) {
            case '"':
                sb.append('"');
                break;
            case '\\':
                sb.append('\\');
                break;
            case '/':
                sb.append('/');
                break;
            case 'b':
                sb.append('\b');
                break;
            case 'f':
                sb.append('\f');
                break;
            case 'n':
                sb.append('\n');
                break;
            case 'r':
                sb.append('\r');
                break;
            case 't':
                sb.append('\t');
                break;
            case 'u':
                if (pos + 4 >= n) {
                    throw unexpected(n);
                }
                int v = 0;
                // 16진수 네 자리를 읽어 코드 단위로 만든다.
                for (int i = 1; i <= 4; i++) {
                    int h = CharClass.hexValue(text.charAt(pos + i));
                    if (h < 0) {
                        throw unexpected(pos + i);
                    }
                    v = (v << 4) | h;
                }
                sb.append((char) v);
                pos += 4;
                break;
            default:
                throw unexpected(pos);
            }
            pos++;
        }
    }

    /**
     * Validates a whole JSON text (grammar and structure) without building any value. Nesting depth is not limited
     * because the check is iterative.
     * <p>
     * JSON 텍스트 전체(문법과 구조)를 값을 만들지 않고 검사한다. 반복 방식이므로 중첩 깊이에 제한이 없다.
     *
     * @param text JSON text / JSON 텍스트
     * @throws JsonSyntaxException if the text is not valid JSON / 올바른 JSON 이 아니면
     */
    public static void validate(String text) {
        final int expectValue = 0;
        final int expectValueOrEnd = 1;
        final int expectKeyOrEnd = 2;
        final int expectKey = 3;
        final int expectColon = 4;
        final int afterValue = 5;
        JsonTokenizer t = new JsonTokenizer(text);
        byte[] stack = new byte[64];
        int sp = 0;
        int state = expectValue;
        // 토큰을 하나씩 읽으며 상태 기계로 구조를 검사한다. 스택은 배열(1)과 객체(2)를 구분한다.
        while (true) {
            int tok = t.next();
            if (tok == EOF) {
                if (state == afterValue && sp == 0) {
                    return;
                }
                throw t.unexpected(text.length());
            }
            if (state == expectValue || state == expectValueOrEnd) {
                if (tok == END_ARRAY && state == expectValueOrEnd) {
                    sp--;
                    state = afterValue;
                } else if (tok == BEGIN_OBJECT || tok == BEGIN_ARRAY) {
                    if (sp == stack.length) {
                        byte[] bigger = new byte[sp * 2];
                        System.arraycopy(stack, 0, bigger, 0, sp);
                        stack = bigger;
                    }
                    stack[sp++] = (byte) (tok == BEGIN_ARRAY ? 1 : 2);
                    state = tok == BEGIN_ARRAY ? expectValueOrEnd : expectKeyOrEnd;
                } else if (tok == STRING || tok == NUMBER || tok == TRUE || tok == FALSE || tok == NULL) {
                    state = afterValue;
                } else {
                    throw t.unexpected(t.tokenStart);
                }
            } else if (state == expectKeyOrEnd || state == expectKey) {
                if (tok == STRING) {
                    state = expectColon;
                } else if (tok == END_OBJECT && state == expectKeyOrEnd) {
                    sp--;
                    state = afterValue;
                } else {
                    throw t.unexpected(t.tokenStart);
                }
            } else if (state == expectColon) {
                if (tok != COLON) {
                    throw t.unexpected(t.tokenStart);
                }
                state = expectValue;
            } else {
                if (sp == 0) {
                    throw t.unexpected(t.tokenStart);
                }
                if (stack[sp - 1] == 1) {
                    if (tok == COMMA) {
                        state = expectValue;
                    } else if (tok == END_ARRAY) {
                        sp--;
                    } else {
                        throw t.unexpected(t.tokenStart);
                    }
                } else {
                    if (tok == COMMA) {
                        state = expectKey;
                    } else if (tok == END_OBJECT) {
                        sp--;
                    } else {
                        throw t.unexpected(t.tokenStart);
                    }
                }
            }
        }
    }
}
