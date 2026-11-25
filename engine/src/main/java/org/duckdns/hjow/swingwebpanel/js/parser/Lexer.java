package org.duckdns.hjow.swingwebpanel.js.parser;

import java.math.BigInteger;
import org.duckdns.hjow.swingwebpanel.js.JsChars;
import org.duckdns.hjow.swingwebpanel.js.JsException;

/**
 * The lexical analyzer (ECMA-262 6th edition clause 11). It converts source text into {@link Token}s on demand:
 * Unicode identifiers with escapes, numeric literals (decimal, <code>0x</code>, <code>0o</code>, <code>0b</code>, legacy octal),
 * string literals with all escapes, template literal parts, comments, line terminators (LF, CR, CRLF, LS, PS) and punctuators.
 * <p>
 * Two things depend on the syntactic context and are therefore requested by the parser: a regular expression literal
 * ({@link #rescanRegExp(Token)} re-reads a <code>/</code> or <code>/=</code> token as a regex) and the continuation of a template
 * after <code>}</code> ({@link #rescanTemplateContinuation(Token)}). {@link #saveState()} and {@link #restoreState(State)} support
 * the parser's speculation and lookahead. Lexical errors are thrown as SyntaxError {@link JsException}s carrying line and column.
 * <p>
 * ECMAScript 2016-2022 additions recognized by the grammar include the exponentiation punctuators, optional chaining,
 * nullish operators, numeric separators and BigInt literals.
 * <p>
 * 어휘 분석기(ECMA-262 6판 11장). 소스 텍스트를 필요할 때마다 {@link Token} 으로 바꾼다: 이스케이프가 있는 유니코드 식별자,
 * 숫자 리터럴(10진, <code>0x</code>, <code>0o</code>, <code>0b</code>, 레거시 8진), 모든 이스케이프가 있는 문자열 리터럴,
 * 템플릿 리터럴 조각, 주석, 줄 끝(LF, CR, CRLF, LS, PS), 구두점.
 * <p>
 * 구문 문맥에 따라 달라지는 두 가지는 파서가 요청한다: 정규식 리터럴({@link #rescanRegExp(Token)} 이 <code>/</code> 또는 <code>/=</code> 토큰을
 * 정규식으로 다시 읽음)과 <code>}</code> 뒤의 템플릿 이어짐({@link #rescanTemplateContinuation(Token)}). {@link #saveState()},
 * {@link #restoreState(State)} 는 파서의 추측 파싱과 미리 보기에 쓴다. 어휘 오류는 줄/열을 가진 SyntaxError {@link JsException} 으로 던진다.
 * <p>
 * 문법이 인식하는 ECMAScript 2016~2022 추가 요소에는 거듭제곱 구두점, optional chaining, nullish 연산자,
 * 숫자 구분자와 BigInt 리터럴이 포함된다.
 */
public final class Lexer {

    /**
     * A snapshot of the lexer position used for backtracking.
     * <p>
     * 되돌리기에 쓰는 렉서 위치 스냅샷.
     */
    public static final class State {
        /**
         * Offset in the source.
         * <p>
         * 소스 오프셋.
         */
        final int pos;
        /**
         * Current line.
         * <p>
         * 현재 줄.
         */
        final int line;
        /**
         * Offset of the first character of the current line.
         * <p>
         * 현재 줄 첫 글자의 오프셋.
         */
        final int lineStart;
        /**
         * Number of tokens produced so far.
         * <p>
         * 지금까지 만든 토큰 수.
         */
        final int tokenCount;

        /**
         * Creates a snapshot.
         * <p>
         * 스냅샷을 만든다.
         *
         * @param pos        offset / 오프셋
         * @param line       line / 줄
         * @param lineStart  line start offset / 줄 시작 오프셋
         * @param tokenCount token count / 토큰 수
         */
        State(int pos, int line, int lineStart, int tokenCount) {
            this.pos = pos;
            this.line = line;
            this.lineStart = lineStart;
            this.tokenCount = tokenCount;
        }
    }

    /**
     * The source text.
     * <p>
     * 소스 텍스트.
     */
    private final String src;

    /**
     * The source name used in error messages.
     * <p>
     * 오류 메시지에 쓰는 소스 이름.
     */
    private final String sourceName;

    /**
     * The source length.
     * <p>
     * 소스 길이.
     */
    private final int len;

    /**
     * Current offset.
     * <p>
     * 현재 오프셋.
     */
    private int pos;

    /**
     * Current 1-based line.
     * <p>
     * 현재 줄(1부터).
     */
    private int line = 1;

    /**
     * Offset of the first character of the current line.
     * <p>
     * 현재 줄 첫 글자의 오프셋.
     */
    private int lineStart;

    /**
     * True for the Module goal (no HTML-like comments).
     * <p>
     * Module 목표이면 true(HTML 유사 주석 없음).
     */
    private boolean module;

    /**
     * Number of tokens produced so far.
     * <p>
     * 지금까지 만든 토큰 수.
     */
    private int tokenCount;

    /**
     * Set while scanning when a legacy octal escape or number was seen.
     * <p>
     * 레거시 8진 이스케이프나 숫자를 만나면 설정된다.
     */
    private boolean sawLegacyOctal;

    /**
     * Creates a lexer for a source text.
     * <p>
     * 소스 텍스트용 렉서를 만든다.
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName name used in errors (file name or URL) / 오류에 쓰는 이름(파일 이름이나 URL)
     */
    public Lexer(String source, String sourceName) {
        this.src = source;
        this.sourceName = sourceName;
        this.len = source.length();
    }

    /**
     * Selects the Module goal: HTML-like comments are then not recognized.
     * <p>
     * Module 목표를 선택한다. 이때 HTML 유사 주석은 인식하지 않는다.
     *
     * @param module true for modules / 모듈이면 true
     */
    public void setModule(boolean module) {
        this.module = module;
    }

    /**
     * Returns the source text.
     * <p>
     * 소스 텍스트를 반환한다.
     *
     * @return the source / 소스
     */
    public String getSource() {
        return src;
    }

    /**
     * Returns the source name.
     * <p>
     * 소스 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Captures the current position.
     * <p>
     * 현재 위치를 저장한다.
     *
     * @return a snapshot / 스냅샷
     */
    public State saveState() {
        return new State(pos, line, lineStart, tokenCount);
    }

    /**
     * Returns to a saved position.
     * <p>
     * 저장한 위치로 돌아간다.
     *
     * @param s a snapshot from {@link #saveState()} / {@link #saveState()} 의 스냅샷
     */
    public void restoreState(State s) {
        pos = s.pos;
        line = s.line;
        lineStart = s.lineStart;
        tokenCount = s.tokenCount;
    }

    /**
     * Positions the lexer right after a token so that the next token is read from there.
     * <p>
     * 렉서를 토큰 바로 뒤에 놓아 다음 토큰을 거기서부터 읽게 한다.
     *
     * @param t the token / 토큰
     */
    public void resetAfter(Token t) {
        pos = t.end;
        line = t.endLine;
        lineStart = t.endLineStart;
    }

    /**
     * Creates a SyntaxError for the current position.
     * <p>
     * 현재 위치의 SyntaxError 를 만든다.
     *
     * @param message the message / 메시지
     * @return the exception to throw / 던질 예외
     */
    public JsException error(String message) {
        return JsException.syntaxError(message, sourceName, line, pos - lineStart + 1);
    }

    /**
     * Creates a SyntaxError for a given position.
     * <p>
     * 지정한 위치의 SyntaxError 를 만든다.
     *
     * @param message the message / 메시지
     * @param atLine  1-based line / 줄(1부터)
     * @param atCol   1-based column / 열(1부터)
     * @return the exception to throw / 던질 예외
     */
    public JsException error(String message, int atLine, int atCol) {
        return JsException.syntaxError(message, sourceName, atLine, atCol);
    }

    // ------------------------------------------------------------------ trivia

    /**
     * Skips white space and comments and reports whether a line terminator was crossed.
     * <p>
     * 공백과 주석을 건너뛰고 줄 끝을 지났는지 알려 준다.
     *
     * @return true if a line terminator was skipped / 줄 끝을 건너뛰었으면 true
     */
    private boolean skipTrivia() {
        boolean newline = false;
        // 공백, 줄 끝, 주석이 이어지는 동안 계속 건너뛴다.
        while (pos < len) {
            char c = src.charAt(pos);
            if (c == '\n' || c == 0x2028 || c == 0x2029) {
                pos++;
                line++;
                lineStart = pos;
                newline = true;
            } else if (c == '\r') {
                pos++;
                if (pos < len && src.charAt(pos) == '\n') {
                    pos++;
                }
                line++;
                lineStart = pos;
                newline = true;
            } else if (c == ' ' || c == '\t' || (c > 0x7F ? JsChars.isWhiteSpace(c) : (c == 0x0B || c == 0x0C))) {
                pos++;
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                skipLineComment();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                if (skipBlockComment()) {
                    newline = true;
                }
            } else if (!module && c == '<' && src.startsWith("<!--", pos)) {
                skipLineComment();
            } else if (!module && c == '-' && (newline || tokenCount == 0) && src.startsWith("-->", pos)) {
                skipLineComment();
            } else {
                break;
            }
        }
        return newline;
    }

    /**
     * Skips to the end of the line (the line terminator itself is not consumed).
     * <p>
     * 줄 끝까지 건너뛴다(줄 끝 문자 자체는 소비하지 않는다).
     */
    private void skipLineComment() {
        // 줄 끝 문자를 만날 때까지 한 글자씩 건너뛴다.
        while (pos < len && !JsChars.isLineTerminator(src.charAt(pos))) {
            pos++;
        }
    }

    /**
     * Skips a block comment and reports whether it contained a line terminator.
     * <p>
     * 블록 주석을 건너뛰고 줄 끝을 포함했는지 알려 준다.
     *
     * @return true if the comment spans lines / 여러 줄이면 true
     */
    private boolean skipBlockComment() {
        int startLine = line;
        int startCol = pos - lineStart + 1;
        pos += 2;
        boolean newline = false;
        // "*/" 를 찾을 때까지 이동하며 줄 번호를 센다.
        while (true) {
            if (pos >= len) {
                throw error("Unterminated comment", startLine, startCol);
            }
            char c = src.charAt(pos);
            if (c == '*' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                pos += 2;
                return newline;
            }
            if (c == '\r') {
                pos++;
                if (pos < len && src.charAt(pos) == '\n') {
                    pos++;
                }
                line++;
                lineStart = pos;
                newline = true;
            } else if (c == '\n' || c == 0x2028 || c == 0x2029) {
                pos++;
                line++;
                lineStart = pos;
                newline = true;
            } else {
                pos++;
            }
        }
    }

    // ------------------------------------------------------------------ main entry

    /**
     * Reads the next token. A slash is always returned as a division punctuator; use {@link #rescanRegExp(Token)} where a
     * regular expression is allowed.
     * <p>
     * 다음 토큰을 읽는다. 슬래시는 항상 나눗셈 구두점으로 반환하며, 정규식이 올 수 있는 자리에서는 {@link #rescanRegExp(Token)} 를 쓴다.
     *
     * @return the token (type EOF at the end) / 토큰(끝이면 EOF)
     */
    public Token next() {
        boolean newline = skipTrivia();
        Token t = new Token();
        t.newlineBefore = newline;
        t.start = pos;
        t.line = line;
        t.column = pos - lineStart + 1;
        sawLegacyOctal = false;
        if (pos >= len) {
            t.type = TokenType.EOF;
            t.value = "";
            t.raw = "";
            return finish(t);
        }
        char c = src.charAt(pos);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '$' || c == '_' || c == '\\'
                || (c >= 0x80 && JsChars.isIdStart(src.codePointAt(pos)))) {
            scanIdentifier(t);
        } else if ((c >= '0' && c <= '9') || (c == '.' && pos + 1 < len && src.charAt(pos + 1) >= '0' && src.charAt(pos + 1) <= '9')) {
            scanNumber(t);
        } else if (c == '"' || c == '\'') {
            scanString(t, c);
        } else if (c == '`') {
            pos++;
            t.type = TokenType.TEMPLATE;
            scanTemplatePart(t);
        } else {
            scanPunctuator(t);
        }
        t.legacyOctal = sawLegacyOctal;
        return finish(t);
    }

    /**
     * Completes a token: end position and raw text.
     * <p>
     * 토큰을 마무리한다: 끝 위치와 원문.
     *
     * @param t the token / 토큰
     * @return the same token / 같은 토큰
     */
    private Token finish(Token t) {
        t.end = pos;
        t.endLine = line;
        t.endLineStart = lineStart;
        if (t.raw == null) {
            t.raw = src.substring(t.start, t.end);
        }
        tokenCount++;
        return t;
    }

    // ------------------------------------------------------------------ identifiers

    /**
     * Scans an IdentifierName including Unicode escapes.
     * <p>
     * 유니코드 이스케이프를 포함한 IdentifierName 을 읽는다.
     *
     * @param t the token to fill / 채울 토큰
     */
    private void scanIdentifier(Token t) {
        t.type = TokenType.NAME;
        int begin = pos;
        // ASCII 식별자 문자는 빠르게 훑는다.
        while (pos < len) {
            char ch = src.charAt(pos);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '$' || ch == '_') {
                pos++;
            } else {
                break;
            }
        }
        if (pos >= len || (src.charAt(pos) != '\\' && src.charAt(pos) < 0x80)) {
            t.value = src.substring(begin, pos);
            return;
        }
        // 이스케이프나 비 ASCII 문자가 나오면 느린 경로로 전환한다.
        StringBuilder sb = new StringBuilder(src.substring(begin, pos));
        while (pos < len) {
            char ch = src.charAt(pos);
            int cp;
            if (ch == '\\') {
                if (pos + 1 >= len || src.charAt(pos + 1) != 'u') {
                    throw error("Invalid Unicode escape sequence");
                }
                pos += 2;
                cp = readUnicodeEscapeBody();
                t.escaped = true;
                boolean ok = sb.length() == 0 ? JsChars.isIdStart(cp) : JsChars.isIdPart(cp);
                if (!ok) {
                    throw error("Invalid Unicode escape sequence in identifier");
                }
            } else {
                cp = src.codePointAt(pos);
                boolean ok = sb.length() == 0 ? JsChars.isIdStart(cp) : JsChars.isIdPart(cp);
                if (!ok) {
                    break;
                }
                pos += Character.charCount(cp);
            }
            sb.appendCodePoint(cp);
        }
        t.value = sb.toString();
    }

    /**
     * Reads the part of a Unicode escape after "\\u": four hex digits or a braced code point.
     * <p>
     * 유니코드 이스케이프에서 "\\u" 뒤의 부분(16진수 4자리 또는 중괄호 코드 포인트)을 읽는다.
     *
     * @return the code point / 코드 포인트
     */
    private int readUnicodeEscapeBody() {
        if (pos < len && src.charAt(pos) == '{') {
            pos++;
            long v = 0;
            int digits = 0;
            // 닫는 중괄호까지 16진수 숫자를 읽는다.
            while (pos < len && src.charAt(pos) != '}') {
                int h = JsChars.hexValue(src.charAt(pos));
                if (h < 0) {
                    throw error("Invalid Unicode escape sequence");
                }
                v = v * 16 + h;
                if (v > 0x10FFFF) {
                    throw error("Undefined Unicode code-point");
                }
                digits++;
                pos++;
            }
            if (pos >= len || digits == 0) {
                throw error("Invalid Unicode escape sequence");
            }
            pos++;
            return (int) v;
        }
        if (pos + 4 > len) {
            throw error("Invalid Unicode escape sequence");
        }
        int v = 0;
        // 정확히 16진수 4자리를 읽는다.
        for (int i = 0; i < 4; i++) {
            int h = JsChars.hexValue(src.charAt(pos + i));
            if (h < 0) {
                throw error("Invalid Unicode escape sequence");
            }
            v = v * 16 + h;
        }
        pos += 4;
        return v;
    }

    // ------------------------------------------------------------------ numbers

    /**
     * Tests for a decimal digit.
     * <p>
     * 10진수 숫자인지 검사한다.
     *
     * @param c character / 문자
     * @return true for 0-9 / 0-9 이면 true
     */
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Scans a numeric literal.
     * <p>
     * 숫자 리터럴을 읽는다.
     *
     * @param t the token to fill / 채울 토큰
     */
    private void scanNumber(Token t) {
        t.type = TokenType.NUMBER;
        int begin = pos;
        char c = src.charAt(pos);
        if (c == '0' && pos + 1 < len) {
            char n = src.charAt(pos + 1);
            int radix = (n == 'x' || n == 'X') ? 16 : (n == 'o' || n == 'O') ? 8 : (n == 'b' || n == 'B') ? 2 : 0;
            if (radix != 0) {
                pos += 2;
                String digits = scanSeparatedDigits(radix);
                if (pos < len && src.charAt(pos) == 'n') {
                    pos++;
                    t.type = TokenType.BIGINT;
                    t.bigInteger = new BigInteger(digits, radix);
                } else {
                    t.number = new BigInteger(digits, radix).doubleValue();
                }
                checkAfterNumber();
                return;
            }
        }

        boolean leadingDot = c == '.';
        String integerDigits = "";
        if (leadingDot) {
            pos++;
        } else {
            integerDigits = scanSeparatedDigits(10);
        }
        boolean exponent = false;
        boolean dot = leadingDot;
        StringBuilder normalized = new StringBuilder(integerDigits);
        if (pos < len && src.charAt(pos) == '.') {
            dot = true;
            pos++;
            normalized.append('.');
            if (pos < len && isDigit(src.charAt(pos))) {
                normalized.append(scanSeparatedDigits(10));
            } else if (leadingDot) {
                throw error("Missing digits after decimal point");
            }
        } else if (leadingDot) {
            normalized.insert(0, "0.");
            normalized.append(scanSeparatedDigits(10));
        }
        if (pos < len && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            exponent = true;
            normalized.append(src.charAt(pos));
            pos++;
            if (pos < len && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                normalized.append(src.charAt(pos));
                pos++;
            }
            normalized.append(scanSeparatedDigits(10));
        }
        boolean separators = src.substring(begin, pos).indexOf('_') >= 0;
        if (!leadingDot && integerDigits.length() > 1 && integerDigits.charAt(0) == '0') {
            sawLegacyOctal = true;
            if (separators) {
                throw error("Numeric separators are not allowed in a legacy octal literal");
            }
            boolean octal = true;
            for (int i = 0; i < integerDigits.length(); i++) {
                if (integerDigits.charAt(i) > '7') {
                    octal = false;
                    break;
                }
            }
            if (octal && !dot && !exponent && !(pos < len && src.charAt(pos) == 'n')) {
                t.number = new BigInteger(integerDigits, 8).doubleValue();
                checkAfterNumber();
                return;
            }
        }
        if (pos < len && src.charAt(pos) == 'n') {
            if (leadingDot || dot || exponent || (integerDigits.length() > 1 && integerDigits.charAt(0) == '0')) {
                throw error("Invalid BigInt literal");
            }
            pos++;
            t.type = TokenType.BIGINT;
            t.bigInteger = new BigInteger(integerDigits, 10);
        } else {
            t.number = Double.parseDouble(normalized.toString());
        }
        checkAfterNumber();
    }

    /**
     * Reads digits in the selected radix while validating numeric separators between digits.
     * <p>
     * 선택한 진법의 숫자를 읽고 숫자 사이의 구분선을 검증한다.
     *
     * @param radix radix 2, 8, 10 or 16 / 진법 2, 8, 10 또는 16
     * @return digits without separators / 구분선을 뺀 숫자
     */
    private String scanSeparatedDigits(int radix) {
        StringBuilder out = new StringBuilder();
        boolean digit = false;
        // 유효 숫자와 그 사이의 밑줄만 읽는다. 밑줄 앞뒤는 모두 같은 진법의 숫자여야 한다.
        while (pos < len) {
            char ch = src.charAt(pos);
            int value = JsChars.hexValue(ch);
            if (value >= 0 && value < radix) {
                out.append(ch);
                digit = true;
                pos++;
            } else if (ch == '_') {
                if (!digit || pos + 1 >= len) {
                    throw error("Invalid numeric separator");
                }
                char next = src.charAt(pos + 1);
                int nextValue = JsChars.hexValue(next);
                if (nextValue < 0 || nextValue >= radix) {
                    throw error("Invalid numeric separator");
                }
                pos++;
                digit = false;
            } else {
                break;
            }
        }
        if (out.length() == 0) {
            throw error(radix == 10 ? "Missing digits in numeric literal" : "Missing digits after the radix prefix of a numeric literal");
        }
        return out.toString();
    }

    /**
     * A numeric literal must not be followed directly by an identifier start or digit.
     * <p>
     * 숫자 리터럴 바로 뒤에 식별자 시작 문자나 숫자가 오면 안 된다.
     */
    private void checkAfterNumber() {
        if (pos < len) {
            char ch = src.charAt(pos);
            if (isDigit(ch) || ch == '\\' || JsChars.isIdStart(src.codePointAt(pos))) {
                throw error("Identifier starts immediately after numeric literal");
            }
        }
    }

    // ------------------------------------------------------------------ strings and templates

    /**
     * Scans a string literal.
     * <p>
     * 문자열 리터럴을 읽는다.
     *
     * @param t     the token to fill / 채울 토큰
     * @param quote the quote character / 따옴표 문자
     */
    private void scanString(Token t, char quote) {
        t.type = TokenType.STRING;
        int begin = pos;
        pos++;
        StringBuilder sb = null;
        int runStart = pos;
        // 닫는 따옴표까지 문자를 읽으며 이스케이프를 해석한다.
        while (true) {
            if (pos >= len) {
                throw error("Unterminated string literal", t.line, t.column);
            }
            char ch = src.charAt(pos);
            if (ch == quote) {
                break;
            }
            if (ch == '\\') {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(src, runStart, pos);
                pos++;
                String err = parseEscape(sb, false);
                if (err != null) {
                    throw error(err);
                }
                runStart = pos;
            } else if (ch == '\n' || ch == '\r' || ch == 0x2028 || ch == 0x2029) {
                throw error("Unterminated string literal", t.line, t.column);
            } else {
                pos++;
            }
        }
        if (sb == null) {
            t.value = src.substring(runStart, pos);
        } else {
            sb.append(src, runStart, pos);
            t.value = sb.toString();
        }
        pos++;
        t.raw = src.substring(begin, pos);
    }

    /**
     * Parses an escape sequence; the position is just after the backslash. Returns an error message or null.
     * <p>
     * 이스케이프 시퀀스를 해석한다. 위치는 역슬래시 바로 뒤이다. 오류 메시지를 반환하며 정상이면 null.
     *
     * @param out      receives the cooked characters / 해석된 문자를 받는다
     * @param template true inside templates (legacy octal escapes are errors) / 템플릿 안이면 true(레거시 8진 이스케이프는 오류)
     * @return error message or null / 오류 메시지 또는 null
     */
    private String parseEscape(StringBuilder out, boolean template) {
        if (pos >= len) {
            return "Unterminated string literal";
        }
        char c = src.charAt(pos);
        switch (c) {
        case 'n':
            out.append('\n');
            pos++;
            return null;
        case 'r':
            out.append('\r');
            pos++;
            return null;
        case 't':
            out.append('\t');
            pos++;
            return null;
        case 'b':
            out.append('\b');
            pos++;
            return null;
        case 'f':
            out.append('\f');
            pos++;
            return null;
        case 'v':
            out.append((char) 0x0B);
            pos++;
            return null;
        case '\r':
            pos++;
            if (pos < len && src.charAt(pos) == '\n') {
                pos++;
            }
            line++;
            lineStart = pos;
            return null;
        case '\n':
        case 0x2028:
        case 0x2029:
            pos++;
            line++;
            lineStart = pos;
            return null;
        case 'x':
            pos++;
            if (pos + 2 <= len && JsChars.hexValue(src.charAt(pos)) >= 0 && JsChars.hexValue(src.charAt(pos + 1)) >= 0) {
                out.append((char) (JsChars.hexValue(src.charAt(pos)) * 16 + JsChars.hexValue(src.charAt(pos + 1))));
                pos += 2;
                return null;
            }
            return "Invalid hexadecimal escape sequence";
        case 'u':
            pos++;
            try {
                out.appendCodePoint(readUnicodeEscapeBody());
            } catch (JsException e) {
                return e.getErrorMessage();
            }
            return null;
        default:
            break;
        }
        if (c >= '0' && c <= '9') {
            if (c == '0' && !(pos + 1 < len && isDigit(src.charAt(pos + 1)))) {
                out.append('\0');
                pos++;
                return null;
            }
            if (template) {
                pos++;
                return "Octal escape sequences are not allowed in template strings";
            }
            sawLegacyOctal = true;
            if (c >= '8') {
                out.append(c);
                pos++;
                return null;
            }
            // 레거시 8진 이스케이프: 최대 3자리, 값은 0377 이하.
            int value = c - '0';
            pos++;
            if (pos < len && src.charAt(pos) >= '0' && src.charAt(pos) <= '7') {
                value = value * 8 + (src.charAt(pos) - '0');
                pos++;
                if (c <= '3' && pos < len && src.charAt(pos) >= '0' && src.charAt(pos) <= '7') {
                    value = value * 8 + (src.charAt(pos) - '0');
                    pos++;
                }
            }
            out.append((char) value);
            return null;
        }
        out.append(c);
        pos++;
        return null;
    }

    /**
     * Scans template characters up to the next <code>${</code> or the closing backtick. The position is after the opening
     * backtick or after the closing brace of a substitution.
     * <p>
     * 다음 <code>${</code> 또는 닫는 백틱까지 템플릿 문자를 읽는다. 위치는 여는 백틱 뒤이거나 치환의 닫는 중괄호 뒤이다.
     *
     * @param t the token to fill / 채울 토큰
     */
    private void scanTemplatePart(Token t) {
        t.type = TokenType.TEMPLATE;
        StringBuilder cooked = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        // 닫는 백틱이나 ${ 를 만날 때까지 문자를 읽는다.
        while (true) {
            if (pos >= len) {
                throw error("Unterminated template literal", t.line, t.column);
            }
            char c = src.charAt(pos);
            if (c == '`') {
                pos++;
                t.tail = true;
                break;
            }
            if (c == '$' && pos + 1 < len && src.charAt(pos + 1) == '{') {
                pos += 2;
                t.tail = false;
                break;
            }
            if (c == '\\') {
                int escStart = pos;
                int escLine = line;
                int escCol = pos - lineStart + 1;
                pos++;
                String err = parseEscape(cooked, true);
                String slice = src.substring(escStart, pos).replace("\r\n", "\n").replace('\r', '\n');
                raw.append(slice);
                if (err != null && t.templateError == null) {
                    t.templateError = err;
                    t.templateErrorLine = escLine;
                    t.templateErrorColumn = escCol;
                }
            } else if (c == '\r') {
                pos++;
                if (pos < len && src.charAt(pos) == '\n') {
                    pos++;
                }
                line++;
                lineStart = pos;
                cooked.append('\n');
                raw.append('\n');
            } else if (c == '\n' || c == 0x2028 || c == 0x2029) {
                pos++;
                line++;
                lineStart = pos;
                cooked.append(c);
                raw.append(c);
            } else {
                cooked.append(c);
                raw.append(c);
                pos++;
            }
        }
        t.value = t.templateError == null ? cooked.toString() : null;
        t.raw = raw.toString();
    }

    /**
     * Re-reads the continuation of a template after the closing brace of a substitution.
     * <p>
     * 치환의 닫는 중괄호 뒤에서 템플릿의 이어지는 부분을 다시 읽는다.
     *
     * @param rbrace the <code>}</code> token that ended the substitution / 치환을 끝낸 <code>}</code> 토큰
     * @return the TEMPLATE token (middle or tail) / TEMPLATE 토큰(중간 또는 꼬리)
     */
    public Token rescanTemplateContinuation(Token rbrace) {
        pos = rbrace.start + 1;
        line = rbrace.line;
        lineStart = rbrace.start - (rbrace.column - 1);
        Token t = new Token();
        t.type = TokenType.TEMPLATE;
        t.newlineBefore = rbrace.newlineBefore;
        t.start = rbrace.start;
        t.line = rbrace.line;
        t.column = rbrace.column;
        scanTemplatePart(t);
        t.end = pos;
        t.endLine = line;
        t.endLineStart = lineStart;
        return t;
    }

    // ------------------------------------------------------------------ regular expressions

    /**
     * Re-reads a <code>/</code> or <code>/=</code> token as a regular expression literal (called by the parser where an
     * expression may start). The pattern is not validated here; flags are checked.
     * <p>
     * <code>/</code> 또는 <code>/=</code> 토큰을 정규식 리터럴로 다시 읽는다(표현식이 시작될 수 있는 자리에서 파서가 호출).
     * 패턴은 여기서 검증하지 않고 플래그만 검사한다.
     *
     * @param slash the slash token / 슬래시 토큰
     * @return the REGEXP token / REGEXP 토큰
     */
    public Token rescanRegExp(Token slash) {
        pos = slash.start;
        line = slash.line;
        lineStart = slash.start - (slash.column - 1);
        Token t = new Token();
        t.type = TokenType.REGEXP;
        t.newlineBefore = slash.newlineBefore;
        t.start = pos;
        t.line = line;
        t.column = slash.column;
        pos++;
        int bodyStart = pos;
        boolean inClass = false;
        // 닫는 / 를 찾는다. 문자 클래스 [...] 안과 이스케이프 뒤의 / 는 끝이 아니다.
        while (true) {
            if (pos >= len || JsChars.isLineTerminator(src.charAt(pos))) {
                throw error("Unterminated regular expression literal", t.line, t.column);
            }
            char ch = src.charAt(pos);
            if (ch == '\\') {
                pos++;
                if (pos >= len || JsChars.isLineTerminator(src.charAt(pos))) {
                    throw error("Unterminated regular expression literal", t.line, t.column);
                }
                pos++;
                continue;
            }
            if (ch == '[') {
                inClass = true;
            } else if (ch == ']') {
                inClass = false;
            } else if (ch == '/' && !inClass) {
                break;
            }
            pos++;
        }
        t.value = src.substring(bodyStart, pos);
        pos++;
        int flagStart = pos;
        // 플래그는 식별자 문자들이다.
        while (pos < len && JsChars.isIdPart(src.codePointAt(pos)) && src.charAt(pos) != '\\') {
            pos += Character.charCount(src.codePointAt(pos));
        }
        if (pos < len && src.charAt(pos) == '\\') {
            throw error("Invalid regular expression flags");
        }
        t.flags = src.substring(flagStart, pos);
        String known = "gimsuy";
        // 알려진 플래그만 허용하고 중복을 금지한다.
        for (int i = 0; i < t.flags.length(); i++) {
            char f = t.flags.charAt(i);
            if (known.indexOf(f) < 0 || t.flags.indexOf(f) != i) {
                throw error("Invalid regular expression flags", t.line, t.column);
            }
        }
        t.end = pos;
        t.endLine = line;
        t.endLineStart = lineStart;
        t.raw = src.substring(t.start, t.end);
        return t;
    }

    // ------------------------------------------------------------------ punctuators

    /**
     * Scans a punctuator using the longest match.
     * <p>
     * 가장 긴 일치로 구두점을 읽는다.
     *
     * @param t the token to fill / 채울 토큰
     */
    private void scanPunctuator(Token t) {
        t.type = TokenType.PUNCT;
        char c = src.charAt(pos);
        char c1 = pos + 1 < len ? src.charAt(pos + 1) : 0;
        char c2 = pos + 2 < len ? src.charAt(pos + 2) : 0;
        char c3 = pos + 3 < len ? src.charAt(pos + 3) : 0;
        String p;
        switch (c) {
        case '{':
            p = "{";
            break;
        case '}':
            p = "}";
            break;
        case '(':
            p = "(";
            break;
        case ')':
            p = ")";
            break;
        case '[':
            p = "[";
            break;
        case ']':
            p = "]";
            break;
        case ';':
            p = ";";
            break;
        case ',':
            p = ",";
            break;
        case '~':
            p = "~";
            break;
        case ':':
            p = ":";
            break;
        case '?':
            if (c1 == '?') {
                p = c2 == '=' ? "??=" : "??";
            } else if (c1 == '.' && !(c2 >= '0' && c2 <= '9')) {
                p = "?.";
            } else {
                p = "?";
            }
            break;
        case '.':
            p = (c1 == '.' && c2 == '.') ? "..." : ".";
            break;
        case '<':
            p = c1 == '<' ? (c2 == '=' ? "<<=" : "<<") : (c1 == '=' ? "<=" : "<");
            break;
        case '>':
            if (c1 == '>') {
                p = c2 == '>' ? (c3 == '=' ? ">>>=" : ">>>") : (c2 == '=' ? ">>=" : ">>");
            } else {
                p = c1 == '=' ? ">=" : ">";
            }
            break;
        case '=':
            p = c1 == '=' ? (c2 == '=' ? "===" : "==") : (c1 == '>' ? "=>" : "=");
            break;
        case '!':
            p = c1 == '=' ? (c2 == '=' ? "!==" : "!=") : "!";
            break;
        case '+':
            p = c1 == '+' ? "++" : (c1 == '=' ? "+=" : "+");
            break;
        case '-':
            p = c1 == '-' ? "--" : (c1 == '=' ? "-=" : "-");
            break;
        case '*':
            p = c1 == '*' ? (c2 == '=' ? "**=" : "**") : (c1 == '=' ? "*=" : "*");
            break;
        case '/':
            p = c1 == '=' ? "/=" : "/";
            break;
        case '%':
            p = c1 == '=' ? "%=" : "%";
            break;
        case '&':
            p = c1 == '&' ? (c2 == '=' ? "&&=" : "&&") : (c1 == '=' ? "&=" : "&");
            break;
        case '|':
            p = c1 == '|' ? (c2 == '=' ? "||=" : "||") : (c1 == '=' ? "|=" : "|");
            break;
        case '^':
            p = c1 == '=' ? "^=" : "^";
            break;
        default:
            throw error("Invalid or unexpected token");
        }
        pos += p.length();
        t.value = p;
    }
}
