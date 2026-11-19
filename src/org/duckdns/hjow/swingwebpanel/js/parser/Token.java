package org.duckdns.hjow.swingwebpanel.js.parser;

/**
 * A token with its value and source position. Tokens are plain data holders created by the {@link Lexer}.
 * Lines and columns are 1-based; columns count UTF-16 code units.
 * <p>
 * 값과 소스 위치를 가진 토큰. 토큰은 {@link Lexer} 가 만드는 단순한 자료 보관 객체이다.
 * 줄과 열은 1부터 시작하며 열은 UTF-16 코드 단위로 센다.
 */
public final class Token {

    /**
     * The token kind.
     * <p>
     * 토큰 종류.
     */
    public TokenType type;

    /**
     * The value: identifier name (escapes resolved), punctuator text, cooked string value, regex pattern or
     * cooked template text (null for a template part with an invalid escape).
     * <p>
     * 값: 식별자 이름(이스케이프 해석 후), 구두점 문자열, 해석된 문자열 값, 정규식 패턴, 또는 해석된 템플릿 텍스트
     * (잘못된 이스케이프가 있는 템플릿 조각은 null).
     */
    public String value;

    /**
     * The source slice of the token; for templates the raw text with line endings normalized to LF.
     * <p>
     * 토큰의 원본 소스 조각. 템플릿은 줄 끝을 LF 로 정규화한 원문.
     */
    public String raw;

    /**
     * The numeric value of a NUMBER token.
     * <p>
     * NUMBER 토큰의 숫자 값.
     */
    public double number;

    /**
     * The flags of a REGEXP token.
     * <p>
     * REGEXP 토큰의 플래그.
     */
    public String flags;

    /**
     * Offset of the first character in the source.
     * <p>
     * 소스에서 첫 글자의 위치(오프셋).
     */
    public int start;

    /**
     * Offset just after the last character.
     * <p>
     * 마지막 글자 바로 다음 위치.
     */
    public int end;

    /**
     * 1-based line of the first character.
     * <p>
     * 첫 글자의 줄 번호(1부터).
     */
    public int line;

    /**
     * 1-based column of the first character.
     * <p>
     * 첫 글자의 열 번호(1부터).
     */
    public int column;

    /**
     * The line number at the end of the token (it differs from {@link #line} for templates and strings with continuations).
     * <p>
     * 토큰 끝의 줄 번호(템플릿이나 줄 이음이 있는 문자열은 {@link #line} 과 다르다).
     */
    public int endLine;

    /**
     * Offset of the first character of the line containing the end of the token.
     * <p>
     * 토큰 끝이 있는 줄의 첫 글자 오프셋.
     */
    public int endLineStart;

    /**
     * True if a line terminator appears between the previous token and this one (needed for automatic semicolon insertion).
     * <p>
     * 이전 토큰과 이 토큰 사이에 줄 끝 문자가 있으면 true(자동 세미콜론 삽입에 필요).
     */
    public boolean newlineBefore;

    /**
     * True if an identifier contained a Unicode escape (such a name is never a keyword).
     * <p>
     * 식별자가 유니코드 이스케이프를 포함하면 true(그런 이름은 키워드가 될 수 없다).
     */
    public boolean escaped;

    /**
     * True if a string or number used a legacy octal form (a SyntaxError in strict mode).
     * <p>
     * 문자열이나 숫자가 레거시 8진 형태를 쓰면 true(엄격 모드에서는 SyntaxError).
     */
    public boolean legacyOctal;

    /**
     * For TEMPLATE tokens: true if this part ends the template (closing backtick).
     * <p>
     * TEMPLATE 토큰: 이 조각이 템플릿을 끝내면(닫는 백틱) true.
     */
    public boolean tail;

    /**
     * For TEMPLATE tokens with an invalid escape: the error message (the cooked value is then null); otherwise null.
     * <p>
     * 잘못된 이스케이프가 있는 TEMPLATE 토큰의 오류 메시지(이때 해석 값은 null). 없으면 null.
     */
    public String templateError;

    /**
     * Line of the template escape error.
     * <p>
     * 템플릿 이스케이프 오류의 줄.
     */
    public int templateErrorLine;

    /**
     * Column of the template escape error.
     * <p>
     * 템플릿 이스케이프 오류의 열.
     */
    public int templateErrorColumn;

    /**
     * Tests for a punctuator with the given text.
     * <p>
     * 주어진 문자열의 구두점인지 검사한다.
     *
     * @param text the punctuator / 구두점
     * @return true if this token is that punctuator / 그 구두점이면 true
     */
    public boolean isPunct(String text) {
        return type == TokenType.PUNCT && value.equals(text);
    }

    /**
     * Tests for an unescaped name with the given text (a keyword check).
     * <p>
     * 주어진 문자열의 이스케이프 없는 이름인지 검사한다(키워드 검사).
     *
     * @param text the keyword or contextual keyword / 키워드 또는 문맥 키워드
     * @return true if this token is that name without escapes / 이스케이프 없는 그 이름이면 true
     */
    public boolean isName(String text) {
        return type == TokenType.NAME && !escaped && value.equals(text);
    }

    /**
     * Returns a short description for error messages.
     * <p>
     * 오류 메시지용 짧은 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        switch (type) {
        case EOF:
            return "end of input";
        case STRING:
            return "string " + raw;
        case NUMBER:
            return "number " + raw;
        case TEMPLATE:
            return "template";
        case REGEXP:
            return "regular expression";
        default:
            return "'" + value + "'";
        }
    }
}
