package org.duckdns.hjow.swingwebpanel.js;

/**
 * Character classification used by the lexer and by string-to-number conversion
 * (ECMA-262 6th edition 11.2 White Space, 11.3 Line Terminators, 11.6 Identifier Names).
 * <p>
 * Unicode limitation: identifier characters are decided by {@link Character#isUnicodeIdentifierStart(int)} and
 * {@link Character#isUnicodeIdentifierPart(int)}, so they follow the Unicode version of the running JRE
 * (JRE 6 implements Unicode 4.0). Characters added by later Unicode versions are rejected on JRE 6.
 * <p>
 * 렉서와 문자열-숫자 변환이 쓰는 문자 분류(ECMA-262 6판 11.2 공백, 11.3 줄 끝, 11.6 식별자 이름).
 * <p>
 * 유니코드 제한: 식별자 문자는 {@link Character#isUnicodeIdentifierStart(int)}, {@link Character#isUnicodeIdentifierPart(int)}
 * 로 판정하므로 실행 중인 JRE 의 유니코드 버전을 따른다(JRE 6 은 유니코드 4.0). 이후 버전에서 추가된 문자는 JRE 6 에서 거부된다.
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
        return c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029;
    }

    /**
     * Tests for White Space: TAB, VT, FF, SP, NBSP, ZWNBSP (BOM) and the characters of the Unicode "Zs" category as listed
     * explicitly in this method (U+1680, U+2000..U+200A, U+202F, U+205F, U+3000), independent of the JRE's Unicode version.
     * <p>
     * 공백 문자(TAB, VT, FF, SP, NBSP, ZWNBSP(BOM)와 이 메서드에 직접 나열한 유니코드 "Zs" 범주 문자: U+1680, U+2000..U+200A,
     * U+202F, U+205F, U+3000)인지 검사한다. JRE 의 유니코드 버전과 무관하다.
     *
     * @param c code unit / 코드 단위
     * @return true for white space / 공백이면 true
     */
    public static boolean isWhiteSpace(int c) {
        if (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20 || c == 0xA0 || c == 0xFEFF) {
            return true;
        }
        // "Zs" 범주를 JRE 의 유니코드 표에 맡기지 않고 직접 나열한다.
        // (JRE 6 은 유니코드 4.0 이라 U+200B 를 공백으로, U+180E 를 공백으로 분류하지만 ES6 이후의 엔진은 둘 다 공백이 아니다.)
        return c == 0x1680 || (c >= 0x2000 && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000;
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
        return isWhiteSpace(c) || isLineTerminator(c);
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
        return cp == '$' || cp == '_' || (cp > 0x7F ? Character.isUnicodeIdentifierStart(cp) : isAsciiLetter(cp));
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
        if (cp == '$' || cp == '_' || cp == 0x200C || cp == 0x200D) {
            return true;
        }
        if (cp < 0x80) {
            return isAsciiLetter(cp) || (cp >= '0' && cp <= '9');
        }
        return Character.isUnicodeIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp);
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
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
