package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * The tokenizer of CSS Syntax Module Level 3 (section 4, "Tokenization"), written as an explicit state machine over the preprocessed input:
 * carriage returns and form feeds become line feeds, NUL and lone surrogates become U+FFFD, comments are skipped. Every parse error of
 * the specification is recovered the way the specification says (bad strings and bad urls become their own tokens, an unterminated comment or
 * string ends at the end of input). The older <code>unicode-range</code> token is recognised unless it is switched off.
 * <p>
 * CSS Syntax Module Level 3 의 토큰화기(4절 "Tokenization"). 전처리한 입력 위의 명시적 상태 기계로 작성했다: 캐리지 리턴과 폼 피드는 줄바꿈이 되고,
 * NUL 과 홀로 있는 서로게이트는 U+FFFD 가 되며, 주석은 건너뛴다. 규격의 모든 파싱 오류는 규격이 정한 대로 복구한다(잘못된 문자열과 잘못된
 * url 은 별도 토큰, 끝나지 않은 주석이나 문자열은 입력 끝에서 끝남). 이전의 <code>unicode-range</code> 토큰은 끄지 않으면 인식한다.
 */
public final class CssTokenizer {

    /** Preprocessed input. / 전처리한 입력. */
    private final String in;

    /** Current offset. / 현재 위치. */
    private int pos;

    /** Whether unicode-range tokens are recognised. / unicode-range 토큰을 인식할지 여부. */
    private final boolean unicodeRange;

    /**
     * Creates a tokenizer that recognises unicode-range tokens.
     * <p>
     * unicode-range 토큰을 인식하는 토큰화기를 만든다.
     *
     * @param text CSS text / CSS 문자열
     */
    public CssTokenizer(String text) {
        this(text, true);
    }

    /**
     * Creates a tokenizer.
     * <p>
     * 토큰화기를 만든다.
     *
     * @param text         CSS text / CSS 문자열
     * @param unicodeRange true to recognise unicode-range tokens / unicode-range 토큰을 인식하려면 true
     */
    public CssTokenizer(String text, boolean unicodeRange) {
        this.in = preprocess(text == null ? "" : text);
        this.unicodeRange = unicodeRange;
    }

    /**
     * Tokenizes a whole text. The end-of-input token is not part of the result.
     * <p>
     * 텍스트 전체를 토큰화한다. 입력 끝 토큰은 결과에 포함하지 않는다.
     *
     * @param text CSS text / CSS 문자열
     * @return the tokens / 토큰 목록
     */
    public static List<CssToken> tokenize(String text) {
        CssTokenizer t = new CssTokenizer(text);
        List<CssToken> list = new ArrayList<CssToken>();
        // 입력 끝 토큰이 나올 때까지 토큰을 모은다.
        while (true) {
            CssToken tok = t.next();
            if (tok.is(CssToken.EOF)) {
                return list;
            }
            list.add(tok);
        }
    }

    /**
     * Preprocesses the input stream as the specification requires.
     * <p>
     * 규격이 요구하는 대로 입력 스트림을 전처리한다.
     *
     * @param text raw text / 원문
     * @return preprocessed text / 전처리한 문자열
     */
    static String preprocess(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        int n = text.length();
        // 줄바꿈 정규화, NUL 과 홀로 있는 서로게이트 교체를 한 글자씩 수행한다.
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                sb.append('\n');
                if (i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == '\f') {
                sb.append('\n');
            } else if (c == 0) {
                sb.append((char) 0xFFFD);
            } else if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(text.charAt(i + 1))) {
                sb.append(c).append(text.charAt(++i));
            } else if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                sb.append((char) 0xFFFD);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the character at an offset from the current position, or -1 beyond the end.
     * <p>
     * 현재 위치에서 오프셋만큼 떨어진 문자를 반환한다. 끝을 넘으면 -1.
     *
     * @param off offset / 오프셋
     * @return character or -1 / 문자 또는 -1
     */
    private int cp(int off) {
        int i = pos + off;
        return i < in.length() ? in.charAt(i) : -1;
    }

    /**
     * Tells whether a character can start a name.
     * <p>
     * 문자가 이름의 첫 글자가 될 수 있는지 알려 준다.
     *
     * @param c character or -1 / 문자 또는 -1
     * @return true if it can / 될 수 있으면 true
     */
    static boolean isNameStart(int c) {
        return c >= 0 && (c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c >= 0x80);
    }

    /**
     * Tells whether a character can be part of a name.
     * <p>
     * 문자가 이름 안에 올 수 있는지 알려 준다.
     *
     * @param c character or -1 / 문자 또는 -1
     * @return true if it can / 올 수 있으면 true
     */
    static boolean isNameChar(int c) {
        return isNameStart(c) || (c >= '0' && c <= '9') || c == '-';
    }

    /**
     * Tells whether a character is a digit.
     * <p>
     * 문자가 숫자인지 알려 준다.
     *
     * @param c character or -1 / 문자 또는 -1
     * @return true for ASCII digits / ASCII 숫자이면 true
     */
    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Tells whether a character is a hexadecimal digit.
     * <p>
     * 문자가 16진수 숫자인지 알려 준다.
     *
     * @param c character or -1 / 문자 또는 -1
     * @return true for hex digits / 16진수 숫자이면 true
     */
    private static boolean isHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Tells whether a character is whitespace (after preprocessing: space, tab, line feed).
     * <p>
     * 문자가 공백인지 알려 준다(전처리 후: 스페이스, 탭, 줄바꿈).
     *
     * @param c character or -1 / 문자 또는 -1
     * @return true for whitespace / 공백이면 true
     */
    static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    /**
     * Tells whether two characters form a valid escape.
     * <p>
     * 두 문자가 올바른 이스케이프인지 알려 준다.
     *
     * @param a first character / 첫 문자
     * @param b second character / 둘째 문자
     * @return true if valid / 올바르면 true
     */
    private static boolean validEscape(int a, int b) {
        return a == '\\' && b != '\n';
    }

    /**
     * Tells whether three characters at an offset would start an identifier.
     * <p>
     * 오프셋의 세 문자가 식별자를 시작하는지 알려 준다.
     *
     * @param off offset from the current position / 현재 위치로부터의 오프셋
     * @return true if an identifier starts / 식별자가 시작하면 true
     */
    private boolean wouldStartIdentifier(int off) {
        int c0 = cp(off);
        int c1 = cp(off + 1);
        int c2 = cp(off + 2);
        if (c0 == '-') {
            return isNameStart(c1) || c1 == '-' || validEscape(c1, c2);
        }
        return isNameStart(c0) || validEscape(c0, c1);
    }

    /**
     * Tells whether three characters at an offset would start a number.
     * <p>
     * 오프셋의 세 문자가 숫자를 시작하는지 알려 준다.
     *
     * @param off offset from the current position / 현재 위치로부터의 오프셋
     * @return true if a number starts / 숫자가 시작하면 true
     */
    private boolean wouldStartNumber(int off) {
        int c0 = cp(off);
        int c1 = cp(off + 1);
        int c2 = cp(off + 2);
        if (c0 == '+' || c0 == '-') {
            return isDigit(c1) || (c1 == '.' && isDigit(c2));
        }
        if (c0 == '.') {
            return isDigit(c1);
        }
        return isDigit(c0);
    }

    /**
     * Consumes an escape (the backslash is already consumed) and returns the code point.
     * <p>
     * 이스케이프를 소비하고(역슬래시는 이미 소비됨) 코드 포인트를 반환한다.
     *
     * @return code point / 코드 포인트
     */
    private int consumeEscape() {
        int c = cp(0);
        if (c < 0) {
            return 0xFFFD;
        }
        pos++;
        if (isHex(c)) {
            int v = Character.digit(c, 16);
            int n = 1;
            // 최대 여섯 자리까지 16진수 숫자를 이어서 읽는다.
            while (n < 6 && isHex(cp(0))) {
                v = v * 16 + Character.digit(cp(0), 16);
                pos++;
                n++;
            }
            if (isWhitespace(cp(0))) {
                pos++;
            }
            if (v == 0 || (v >= 0xD800 && v <= 0xDFFF) || v > 0x10FFFF) {
                return 0xFFFD;
            }
            return v;
        }
        return c;
    }

    /**
     * Consumes a name.
     * <p>
     * 이름을 소비한다.
     *
     * @return the name / 이름
     */
    private String consumeName() {
        StringBuilder sb = new StringBuilder();
        // 이름 문자나 올바른 이스케이프가 이어지는 동안 읽는다.
        while (true) {
            int c = cp(0);
            if (isNameChar(c)) {
                sb.append((char) c);
                pos++;
            } else if (validEscape(c, cp(1))) {
                pos++;
                sb.appendCodePoint(consumeEscape());
            } else {
                return sb.toString();
            }
        }
    }

    /**
     * Consumes the next token.
     * <p>
     * 다음 토큰을 소비한다.
     *
     * @return the token, an EOF token at the end / 토큰. 끝에서는 EOF 토큰
     */
    public CssToken next() {
        // 토큰 앞의 주석을 모두 건너뛴다. 끝나지 않은 주석은 입력 끝까지이다.
        while (in.startsWith("/*", pos)) {
            int e = in.indexOf("*/", pos + 2);
            pos = e < 0 ? in.length() : e + 2;
        }
        int start = pos;
        int c = cp(0);
        if (c < 0) {
            return new CssToken(CssToken.EOF, "", start);
        }
        if (isWhitespace(c)) {
            // 연속된 공백을 하나의 공백 토큰으로 합친다.
            while (isWhitespace(cp(0))) {
                pos++;
            }
            return new CssToken(CssToken.WHITESPACE, " ", start);
        }
        switch (c) {
        case '"':
        case '\'':
            pos++;
            return consumeString(c, start);
        case '#':
            pos++;
            if (isNameChar(cp(0)) || validEscape(cp(0), cp(1))) {
                boolean id = wouldStartIdentifier(0);
                String name = consumeName();
                return new CssToken(CssToken.HASH, name, 0, false, null, id, null, start);
            }
            return new CssToken(CssToken.DELIM, "#", start);
        case '(':
            pos++;
            return new CssToken(CssToken.LEFT_PAREN, "(", start);
        case ')':
            pos++;
            return new CssToken(CssToken.RIGHT_PAREN, ")", start);
        case ',':
            pos++;
            return new CssToken(CssToken.COMMA, ",", start);
        case ':':
            pos++;
            return new CssToken(CssToken.COLON, ":", start);
        case ';':
            pos++;
            return new CssToken(CssToken.SEMICOLON, ";", start);
        case '[':
            pos++;
            return new CssToken(CssToken.LEFT_BRACKET, "[", start);
        case ']':
            pos++;
            return new CssToken(CssToken.RIGHT_BRACKET, "]", start);
        case '{':
            pos++;
            return new CssToken(CssToken.LEFT_BRACE, "{", start);
        case '}':
            pos++;
            return new CssToken(CssToken.RIGHT_BRACE, "}", start);
        case '+':
            if (wouldStartNumber(0)) {
                return consumeNumeric(start);
            }
            pos++;
            return new CssToken(CssToken.DELIM, "+", start);
        case '-':
            if (wouldStartNumber(0)) {
                return consumeNumeric(start);
            }
            if (cp(1) == '-' && cp(2) == '>') {
                pos += 3;
                return new CssToken(CssToken.CDC, "-->", start);
            }
            if (wouldStartIdentifier(0)) {
                return consumeIdentLike(start);
            }
            pos++;
            return new CssToken(CssToken.DELIM, "-", start);
        case '.':
            if (wouldStartNumber(0)) {
                return consumeNumeric(start);
            }
            pos++;
            return new CssToken(CssToken.DELIM, ".", start);
        case '<':
            if (in.startsWith("<!--", pos)) {
                pos += 4;
                return new CssToken(CssToken.CDO, "<!--", start);
            }
            pos++;
            return new CssToken(CssToken.DELIM, "<", start);
        case '@':
            pos++;
            if (wouldStartIdentifier(0)) {
                return new CssToken(CssToken.AT_KEYWORD, consumeName(), start);
            }
            return new CssToken(CssToken.DELIM, "@", start);
        case '\\':
            if (validEscape(c, cp(1))) {
                return consumeIdentLike(start);
            }
            pos++;
            return new CssToken(CssToken.DELIM, "\\", start);
        default:
            break;
        }
        if (isDigit(c)) {
            return consumeNumeric(start);
        }
        if (unicodeRange && (c == 'u' || c == 'U') && cp(1) == '+' && (isHex(cp(2)) || cp(2) == '?')) {
            return consumeUnicodeRange(start);
        }
        if (isNameStart(c)) {
            return consumeIdentLike(start);
        }
        pos++;
        if (Character.isHighSurrogate((char) c) && Character.isLowSurrogate((char) cp(0))) {
            pos++;
            return new CssToken(CssToken.DELIM, in.substring(start, pos), start);
        }
        return new CssToken(CssToken.DELIM, String.valueOf((char) c), start);
    }

    /**
     * Consumes a string token (the opening quote is consumed).
     * <p>
     * 문자열 토큰을 소비한다(여는 따옴표는 이미 소비됨).
     *
     * @param ending ending quote character / 닫는 따옴표 문자
     * @param start  token start / 토큰 시작
     * @return string or bad-string token / 문자열 또는 잘못된 문자열 토큰
     */
    private CssToken consumeString(int ending, int start) {
        StringBuilder sb = new StringBuilder();
        // 닫는 따옴표, 줄바꿈, 입력 끝 중 하나를 만날 때까지 읽는다.
        while (true) {
            int c = cp(0);
            if (c < 0) {
                return new CssToken(CssToken.STRING, sb.toString(), start);
            }
            if (c == ending) {
                pos++;
                return new CssToken(CssToken.STRING, sb.toString(), start);
            }
            if (c == '\n') {
                return new CssToken(CssToken.BAD_STRING, sb.toString(), start);
            }
            if (c == '\\') {
                int n = cp(1);
                if (n < 0) {
                    pos++;
                } else if (n == '\n') {
                    pos += 2;
                } else {
                    pos++;
                    sb.appendCodePoint(consumeEscape());
                }
            } else {
                sb.append((char) c);
                pos++;
            }
        }
    }

    /**
     * Consumes a numeric token: number, percentage or dimension.
     * <p>
     * 숫자 토큰(숫자, 백분율, 차원)을 소비한다.
     *
     * @param start token start / 토큰 시작
     * @return the token / 토큰
     */
    private CssToken consumeNumeric(int start) {
        int s = pos;
        boolean integer = true;
        if (cp(0) == '+' || cp(0) == '-') {
            pos++;
        }
        // 정수부의 숫자를 읽는다.
        while (isDigit(cp(0))) {
            pos++;
        }
        if (cp(0) == '.' && isDigit(cp(1))) {
            pos += 2;
            integer = false;
            // 소수부의 숫자를 읽는다.
            while (isDigit(cp(0))) {
                pos++;
            }
        }
        int e = cp(0);
        if (e == 'e' || e == 'E') {
            int c1 = cp(1);
            int c2 = cp(2);
            if (isDigit(c1) || ((c1 == '+' || c1 == '-') && isDigit(c2))) {
                pos += isDigit(c1) ? 1 : 2;
                integer = false;
                // 지수부의 숫자를 읽는다.
                while (isDigit(cp(0))) {
                    pos++;
                }
            }
        }
        String repr = in.substring(s, pos);
        double v = Double.parseDouble(repr);
        if (wouldStartIdentifier(0)) {
            String unit = consumeName();
            return new CssToken(CssToken.DIMENSION, "", v, integer, unit, false, repr, start);
        }
        if (cp(0) == '%') {
            pos++;
            return new CssToken(CssToken.PERCENTAGE, "", v, integer, null, false, repr, start);
        }
        return new CssToken(CssToken.NUMBER, "", v, integer, null, false, repr, start);
    }

    /**
     * Consumes an ident-like token: identifier, function, url or bad url.
     * <p>
     * 식별자 형태의 토큰(식별자, 함수, url, 잘못된 url)을 소비한다.
     *
     * @param start token start / 토큰 시작
     * @return the token / 토큰
     */
    private CssToken consumeIdentLike(int start) {
        String name = consumeName();
        if (name.equalsIgnoreCase("url") && cp(0) == '(') {
            pos++;
            // 공백이 둘 이상 이어지면 하나만 남기고 소비한다.
            while (isWhitespace(cp(0)) && isWhitespace(cp(1))) {
                pos++;
            }
            int c0 = cp(0);
            int c1 = cp(1);
            if (c0 == '"' || c0 == '\'' || (isWhitespace(c0) && (c1 == '"' || c1 == '\''))) {
                return new CssToken(CssToken.FUNCTION, name, start);
            }
            return consumeUrl(start);
        }
        if (cp(0) == '(') {
            pos++;
            return new CssToken(CssToken.FUNCTION, name, start);
        }
        return new CssToken(CssToken.IDENT, name, start);
    }

    /**
     * Consumes an unquoted url token (after <code>url(</code>).
     * <p>
     * 따옴표 없는 url 토큰을 소비한다(<code>url(</code> 뒤).
     *
     * @param start token start / 토큰 시작
     * @return url or bad-url token / url 또는 잘못된 url 토큰
     */
    private CssToken consumeUrl(int start) {
        StringBuilder sb = new StringBuilder();
        // 앞쪽 공백을 건너뛴다.
        while (isWhitespace(cp(0))) {
            pos++;
        }
        // 닫는 괄호나 입력 끝까지 url 문자를 읽는다.
        while (true) {
            int c = cp(0);
            if (c < 0) {
                return new CssToken(CssToken.URL, sb.toString(), start);
            }
            if (c == ')') {
                pos++;
                return new CssToken(CssToken.URL, sb.toString(), start);
            }
            if (isWhitespace(c)) {
                // 공백 뒤에는 닫는 괄호나 끝만 올 수 있다.
                while (isWhitespace(cp(0))) {
                    pos++;
                }
                if (cp(0) == ')' || cp(0) < 0) {
                    if (cp(0) == ')') {
                        pos++;
                    }
                    return new CssToken(CssToken.URL, sb.toString(), start);
                }
                consumeBadUrlRemnants();
                return new CssToken(CssToken.BAD_URL, sb.toString(), start);
            }
            if (c == '"' || c == '\'' || c == '(' || (c >= 0 && c <= 8) || c == 0x0B || (c >= 0x0E && c <= 0x1F) || c == 0x7F) {
                consumeBadUrlRemnants();
                return new CssToken(CssToken.BAD_URL, sb.toString(), start);
            }
            if (c == '\\') {
                if (validEscape(c, cp(1))) {
                    pos++;
                    sb.appendCodePoint(consumeEscape());
                } else {
                    consumeBadUrlRemnants();
                    return new CssToken(CssToken.BAD_URL, sb.toString(), start);
                }
            } else {
                sb.append((char) c);
                pos++;
            }
        }
    }

    /**
     * Consumes the rest of a bad url up to and including the closing parenthesis.
     * <p>
     * 잘못된 url 의 나머지를 닫는 괄호까지(포함) 소비한다.
     */
    private void consumeBadUrlRemnants() {
        // 닫는 괄호나 입력 끝을 만날 때까지 소비하며 이스케이프는 한 덩어리로 처리한다.
        while (true) {
            int c = cp(0);
            if (c < 0) {
                return;
            }
            pos++;
            if (c == ')') {
                return;
            }
            if (validEscape(c, cp(0))) {
                consumeEscape();
            }
        }
    }

    /**
     * Consumes a unicode-range token such as <code>U+0025-00FF</code> or <code>U+4??</code>.
     * <p>
     * <code>U+0025-00FF</code>, <code>U+4??</code> 같은 unicode-range 토큰을 소비한다.
     *
     * @param start token start / 토큰 시작
     * @return the token / 토큰
     */
    private CssToken consumeUnicodeRange(int start) {
        pos += 2;
        int digits = 0;
        // 최대 여섯 자리의 16진수 숫자를 읽는다.
        while (digits < 6 && isHex(cp(0))) {
            pos++;
            digits++;
        }
        boolean question = false;
        // 남은 자리를 물음표로 채울 수 있다.
        while (digits < 6 && cp(0) == '?') {
            pos++;
            digits++;
            question = true;
        }
        if (!question && cp(0) == '-' && isHex(cp(1))) {
            pos++;
            int d2 = 0;
            // 범위 끝의 16진수 숫자를 읽는다.
            while (d2 < 6 && isHex(cp(0))) {
                pos++;
                d2++;
            }
        }
        return new CssToken(CssToken.UNICODE_RANGE, in.substring(start, pos).toUpperCase(), start);
    }
}
