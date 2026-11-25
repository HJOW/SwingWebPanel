package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.js.text.CharClass;

/**
 * Character classification for the lexer and other ECMAScript code using the shared Unicode tables.
 * <p>
 * 공유 유니코드 표를 사용하는 렉서와 ECMAScript 코드의 문자 분류.
 */
public final class JsChars {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private JsChars() {
    }

    /**
     * Tests for a line terminator: LF, CR, LS (U+2028) and PS (U+2029).
     * <p>
     * 줄 끝 문자(LF, CR, LS U+2028, PS U+2029)인지 검사한다.
     *
     * @param c code unit / 코드 단위
     * @return true for a line terminator / 줄 끝이면 true
     */
    public static boolean isLineTerminator(int c) {
        return CharClass.isLineTerminator(c);
    }

    /**
     * Tests for White Space, including the Unicode Zs category and the explicitly specified ECMAScript characters.
     * <p>
     * 유니코드 Zs 범주와 ECMAScript가 명시한 공백 문자인지 검사한다.
     *
     * @param c code unit / 코드 단위
     * @return true for white space / 공백이면 true
     */
    public static boolean isWhiteSpace(int c) {
        return CharClass.isWhiteSpace(c);
    }

    /**
     * Tests for white space or line terminator (the characters trimmed by String.prototype.trim and StringToNumber).
     * <p>
     * 공백 또는 줄 끝인지 검사한다(String.prototype.trim 과 StringToNumber 가 제거하는 문자).
     *
     * @param c code unit / 코드 단위
     * @return true if white space or line terminator / 공백 또는 줄 끝이면 true
     */
    public static boolean isSpace(int c) {
        return CharClass.isWhiteSpaceOrLineTerminator(c);
    }

    /**
     * Tests whether a code point may start an IdentifierName: $, _ or a Unicode ID_Start character.
     * <p>
     * 코드 포인트가 IdentifierName 의 시작이 될 수 있는지 검사한다: $, _ 또는 유니코드 ID_Start 문자.
     *
     * @param cp code point / 코드 포인트
     * @return true if it can start an identifier / 식별자 시작이 가능하면 true
     */
    public static boolean isIdStart(int cp) {
        return CharClass.isIdentifierStart(cp);
    }

    /**
     * Tests whether a code point may continue an IdentifierName: ID_Continue, $, ZWNJ or ZWJ.
     * <p>
     * 코드 포인트가 IdentifierName 의 일부가 될 수 있는지 검사한다: ID_Continue, $, ZWNJ, ZWJ.
     *
     * @param cp code point / 코드 포인트
     * @return true if it can continue an identifier / 식별자 구성이 가능하면 true
     */
    public static boolean isIdPart(int cp) {
        return CharClass.isIdentifierPart(cp);
    }

    /**
     * Tests for an ASCII letter.
     * <p>
     * ASCII 영문자인지 검사한다.
     *
     * @param c character / 문자
     * @return true for A-Z or a-z / A-Z 또는 a-z 이면 true
     */
    public static boolean isAsciiLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Returns the value of a hexadecimal digit, or -1 if the character is not one.
     * <p>
     * 16진수 숫자의 값을 반환한다. 16진수 숫자가 아니면 -1.
     *
     * @param c character / 문자
     * @return 0..15 or -1 / 0..15 또는 -1
     */
    public static int hexValue(int c) {
        return CharClass.hexValue(c);
    }
}
