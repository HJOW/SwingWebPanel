package org.duckdns.hjow.swingwebpanel.css.syntax;

/**
 * A token of the CSS Syntax Module Level 3. Besides the type it carries the data of the token: the text of identifiers, strings,
 * at-keywords, functions and hashes, the numeric value and unit of numbers, percentages and dimensions, the "id" flag of hash tokens
 * and the source position.
 * <p>
 * CSS Syntax Module Level 3 의 토큰. 종류 외에 토큰의 데이터를 가진다: 식별자, 문자열, at-키워드, 함수, 해시의 문자열, 숫자/백분율/차원의 값과 단위,
 * 해시 토큰의 "id" 플래그, 소스 위치.
 */
public final class CssToken extends ComponentValue {

    /** Token type: identifier. / 토큰 종류: 식별자. */
    public static final int IDENT = 1;
    /** Token type: function name followed by an opening parenthesis. / 토큰 종류: 여는 소괄호가 붙은 함수 이름. */
    public static final int FUNCTION = 2;
    /** Token type: at-keyword. / 토큰 종류: at-키워드. */
    public static final int AT_KEYWORD = 3;
    /** Token type: hash. / 토큰 종류: 해시. */
    public static final int HASH = 4;
    /** Token type: string. / 토큰 종류: 문자열. */
    public static final int STRING = 5;
    /** Token type: bad string (unterminated at a newline). / 토큰 종류: 잘못된 문자열(줄바꿈에서 끝남). */
    public static final int BAD_STRING = 6;
    /** Token type: url token. / 토큰 종류: url 토큰. */
    public static final int URL = 7;
    /** Token type: bad url. / 토큰 종류: 잘못된 url. */
    public static final int BAD_URL = 8;
    /** Token type: delimiter (a single code point). / 토큰 종류: 구분 문자(코드 포인트 하나). */
    public static final int DELIM = 9;
    /** Token type: number. / 토큰 종류: 숫자. */
    public static final int NUMBER = 10;
    /** Token type: percentage. / 토큰 종류: 백분율. */
    public static final int PERCENTAGE = 11;
    /** Token type: dimension (number with unit). / 토큰 종류: 차원(단위가 붙은 숫자). */
    public static final int DIMENSION = 12;
    /** Token type: whitespace. / 토큰 종류: 공백. */
    public static final int WHITESPACE = 13;
    /** Token type: CDO (the characters lt ! - -). / 토큰 종류: CDO. */
    public static final int CDO = 14;
    /** Token type: CDC (the characters - - gt). / 토큰 종류: CDC. */
    public static final int CDC = 15;
    /** Token type: colon. / 토큰 종류: 콜론. */
    public static final int COLON = 16;
    /** Token type: semicolon. / 토큰 종류: 세미콜론. */
    public static final int SEMICOLON = 17;
    /** Token type: comma. / 토큰 종류: 쉼표. */
    public static final int COMMA = 18;
    /** Token type: left square bracket. / 토큰 종류: 여는 대괄호. */
    public static final int LEFT_BRACKET = 19;
    /** Token type: right square bracket. / 토큰 종류: 닫는 대괄호. */
    public static final int RIGHT_BRACKET = 20;
    /** Token type: left parenthesis. / 토큰 종류: 여는 소괄호. */
    public static final int LEFT_PAREN = 21;
    /** Token type: right parenthesis. / 토큰 종류: 닫는 소괄호. */
    public static final int RIGHT_PAREN = 22;
    /** Token type: left curly brace. / 토큰 종류: 여는 중괄호. */
    public static final int LEFT_BRACE = 23;
    /** Token type: right curly brace. / 토큰 종류: 닫는 중괄호. */
    public static final int RIGHT_BRACE = 24;
    /** Token type: unicode-range (the older Syntax 3 token). / 토큰 종류: unicode-range(이전 Syntax 3 토큰). */
    public static final int UNICODE_RANGE = 25;
    /** Token type: end of input. / 토큰 종류: 입력의 끝. */
    public static final int EOF = 26;

    /** Token type constant. / 토큰 종류 상수. */
    private final int type;

    /** Text of identifiers, strings, urls, names; the delimiter for DELIM. / 식별자, 문자열, url, 이름의 문자열. DELIM 은 구분 문자. */
    private final String value;

    /** Numeric value of numbers, percentages and dimensions. / 숫자, 백분율, 차원의 값. */
    private final double number;

    /** True if the number was written as an integer. / 숫자가 정수로 쓰였으면 true. */
    private final boolean integer;

    /** Unit of a dimension (original case), or null. / 차원의 단위(원래 대소문자). 없으면 null. */
    private final String unit;

    /** True for hash tokens whose value is a valid identifier ("id" type flag). / 값이 올바른 식별자인 해시 토큰이면 true("id" 종류 플래그). */
    private final boolean hashId;

    /** Original text of a number (kept for serialization), or null. / 숫자의 원래 문자열(직렬화용). 없으면 null. */
    private final String repr;

    /** Source offset where the token starts. / 토큰이 시작하는 소스 위치. */
    private final int start;

    /**
     * Creates a token.
     * <p>
     * 토큰을 만든다.
     *
     * @param type    token type / 토큰 종류
     * @param value   text / 문자열
     * @param number  numeric value / 숫자 값
     * @param integer integer flag / 정수 플래그
     * @param unit    unit or null / 단위 또는 null
     * @param hashId  id flag of hash tokens / 해시 토큰의 id 플래그
     * @param repr    original number text or null / 원래 숫자 문자열 또는 null
     * @param start   source offset / 소스 위치
     */
    public CssToken(int type, String value, double number, boolean integer, String unit, boolean hashId, String repr, int start) {
        this.type = type;
        this.value = value;
        this.number = number;
        this.integer = integer;
        this.unit = unit;
        this.hashId = hashId;
        this.repr = repr;
        this.start = start;
    }

    /**
     * Creates a token that only has a type and a text (no number data).
     * <p>
     * 종류와 문자열만 있는 토큰(숫자 데이터 없음)을 만든다.
     *
     * @param type  token type / 토큰 종류
     * @param value text / 문자열
     * @param start source offset / 소스 위치
     */
    public CssToken(int type, String value, int start) {
        this(type, value, 0, false, null, false, null, start);
    }

    /**
     * Returns the token type.
     * <p>
     * 토큰 종류를 반환한다.
     *
     * @return type constant / 종류 상수
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the text value (identifier name, string content, url, function name, at-keyword name, hash text, delimiter).
     * <p>
     * 문자열 값을 반환한다(식별자 이름, 문자열 내용, url, 함수 이름, at-키워드 이름, 해시 문자열, 구분 문자).
     *
     * @return text / 문자열
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the numeric value.
     * <p>
     * 숫자 값을 반환한다.
     *
     * @return number / 숫자
     */
    public double getNumber() {
        return number;
    }

    /**
     * Tells whether the number was an integer.
     * <p>
     * 숫자가 정수였는지 알려 준다.
     *
     * @return true for integers / 정수이면 true
     */
    public boolean isInteger() {
        return integer;
    }

    /**
     * Returns the unit of a dimension.
     * <p>
     * 차원의 단위를 반환한다.
     *
     * @return unit or null / 단위 또는 null
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Tells whether a hash token is an identifier-like hash (usable as id selector).
     * <p>
     * 해시 토큰이 식별자 형태(id 선택자로 쓸 수 있음)인지 알려 준다.
     *
     * @return true for "id" hashes / "id" 해시이면 true
     */
    public boolean isHashId() {
        return hashId;
    }

    /**
     * Returns the source offset.
     * <p>
     * 소스 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getStart() {
        return start;
    }

    /**
     * Tells whether the token has a type.
     * <p>
     * 토큰이 주어진 종류인지 알려 준다.
     *
     * @param t type constant / 종류 상수
     * @return true if equal / 같으면 true
     */
    public boolean is(int t) {
        return type == t;
    }

    /**
     * Tells whether the token is an identifier with the given name (ASCII case-insensitive).
     * <p>
     * 토큰이 주어진 이름의 식별자인지 알려 준다(ASCII 대소문자 무시).
     *
     * @param name lower-case name / 소문자 이름
     * @return true if it matches / 일치하면 true
     */
    public boolean isIdent(String name) {
        return type == IDENT && value.equalsIgnoreCase(name);
    }

    /**
     * Tells whether the token is the given delimiter.
     * <p>
     * 토큰이 주어진 구분 문자인지 알려 준다.
     *
     * @param c delimiter character / 구분 문자
     * @return true if it matches / 일치하면 true
     */
    public boolean isDelim(char c) {
        return type == DELIM && value.length() == 1 && value.charAt(0) == c;
    }

    /**
     * Serializes the token.
     * <p>
     * 토큰을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        switch (type) {
        case IDENT:
            return CssEscape.ident(value);
        case FUNCTION:
            return CssEscape.ident(value) + "(";
        case AT_KEYWORD:
            return "@" + CssEscape.ident(value);
        case HASH:
            return "#" + (hashId ? CssEscape.ident(value) : escapeHashName(value));
        case STRING:
            return CssEscape.string(value);
        case BAD_STRING:
            return "\"" + value;
        case URL:
            return "url(" + CssEscape.string(value) + ")";
        case BAD_URL:
            return "url(" + value + ")";
        case DELIM:
            return value;
        case NUMBER:
            return repr != null ? repr : CssEscape.number(number);
        case PERCENTAGE:
            return (repr != null ? repr : CssEscape.number(number)) + "%";
        case DIMENSION:
            return (repr != null ? repr : CssEscape.number(number)) + CssEscape.ident(unit);
        case WHITESPACE:
            return " ";
        case CDO:
            return "<!--";
        case CDC:
            return "-->";
        case COLON:
            return ":";
        case SEMICOLON:
            return ";";
        case COMMA:
            return ",";
        case LEFT_BRACKET:
            return "[";
        case RIGHT_BRACKET:
            return "]";
        case LEFT_PAREN:
            return "(";
        case RIGHT_PAREN:
            return ")";
        case LEFT_BRACE:
            return "{";
        case RIGHT_BRACE:
            return "}";
        case UNICODE_RANGE:
            return value;
        default:
            return "";
        }
    }

    /**
     * Escapes the characters of a hash name that are not name characters.
     * <p>
     * 해시 이름에서 이름 문자가 아닌 문자를 이스케이프한다.
     *
     * @param s hash name / 해시 이름
     * @return escaped name / 이스케이프한 이름
     */
    private static String escapeHashName(String s) {
        StringBuilder sb = new StringBuilder();
        // 이름 문자가 아닌 글자만 역슬래시로 이스케이프한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x80 || c == '-' || c == '_' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                sb.append(c);
            } else {
                sb.append('\\').append(c);
            }
        }
        return sb.toString();
    }
}
