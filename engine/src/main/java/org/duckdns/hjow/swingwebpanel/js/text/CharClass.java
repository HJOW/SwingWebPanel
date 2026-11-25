package org.duckdns.hjow.swingwebpanel.js.text;

/**
 * Character classification used by the ECMAScript lexer, regular expressions and string conversions:
 * WhiteSpace, LineTerminator, decimal/hex digits and the ID_Start / ID_Continue identifier classes.
 * <p>
 * Identifier and space separator data is shared with regular expressions through {@link UnicodeProperties}.
 * <p>
 * ECMAScript 렉서, 정규식, 문자열 변환에서 쓰는 문자 분류: WhiteSpace, LineTerminator, 10진/16진 숫자,
 * ID_Start / ID_Continue 식별자 분류.
 * <p>
 * 식별자와 공백 구분자 데이터는 {@link UnicodeProperties}를 통해 정규식과 공유한다.
 */
public final class CharClass {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CharClass() {
    }

    /**
     * Tests ECMAScript WhiteSpace: TAB, VT, FF, SP, NBSP, ZWNBSP (U+FEFF) and any other Unicode space separator (Zs).
     * Line terminators are not included; see {@link #isLineTerminator(int)}.
     * <p>
     * ECMAScript WhiteSpace 인지 검사한다: TAB, VT, FF, SP, NBSP, ZWNBSP(U+FEFF)와 그 밖의 유니코드 공백 구분자(Zs).
     * 줄 끝 문자는 포함하지 않는다. {@link #isLineTerminator(int)} 참조.
     *
     * @param c UTF-16 code unit or code point / UTF-16 코드 단위 또는 코드 포인트
     * @return true if white space / 공백이면 true
     */
    public static boolean isWhiteSpace(int c) {
        switch (c) {
        case 0x09:
        case 0x0B:
        case 0x0C:
        case 0x20:
        case 0xA0:
        case 0xFEFF:
            return true;
        default:
            break;
        }
        return c >= 0x80 && UnicodeProperties.isSpaceSeparator(c);
    }

    /**
     * Tests ECMAScript LineTerminator: LF, CR, LS (U+2028) and PS (U+2029).
     * <p>
     * ECMAScript LineTerminator 인지 검사한다: LF, CR, LS(U+2028), PS(U+2029).
     *
     * @param c UTF-16 code unit or code point / UTF-16 코드 단위 또는 코드 포인트
     * @return true if line terminator / 줄 끝 문자이면 true
     */
    public static boolean isLineTerminator(int c) {
        return c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029;
    }

    /**
     * Tests WhiteSpace or LineTerminator (the set trimmed by StringToNumber and used by regular expression \s).
     * <p>
     * WhiteSpace 또는 LineTerminator 인지 검사한다(StringToNumber 가 제거하는 문자 집합이며 정규식 \s 에 쓰인다).
     *
     * @param c UTF-16 code unit or code point / UTF-16 코드 단위 또는 코드 포인트
     * @return true if either / 둘 중 하나이면 true
     */
    public static boolean isWhiteSpaceOrLineTerminator(int c) {
        return isWhiteSpace(c) || isLineTerminator(c);
    }

    /**
     * Tests a decimal digit 0-9 (ASCII only, as ECMAScript requires).
     * <p>
     * 10진 숫자 0-9 인지 검사한다(ECMAScript 규칙대로 ASCII 만).
     *
     * @param c character / 문자
     * @return true if 0-9 / 0-9 이면 true
     */
    public static boolean isDecimalDigit(int c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns the value of a hexadecimal digit, or -1 if the character is not one.
     * <p>
     * 16진 숫자의 값을 반환한다. 16진 숫자가 아니면 -1.
     *
     * @param c character / 문자
     * @return 0-15 or -1 / 0-15 또는 -1
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

    /**
     * Returns the value of a digit in the given radix (0-9, a-z, A-Z), or -1 if invalid for that radix.
     * <p>
     * 주어진 진법에서 숫자 문자의 값(0-9, a-z, A-Z)을 반환한다. 그 진법에 맞지 않으면 -1.
     *
     * @param c     character / 문자
     * @param radix 2 to 36 / 2 이상 36 이하의 진법
     * @return digit value or -1 / 숫자 값 또는 -1
     */
    public static int digitValue(int c, int radix) {
        int v;
        if (c >= '0' && c <= '9') {
            v = c - '0';
        } else if (c >= 'a' && c <= 'z') {
            v = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            v = c - 'A' + 10;
        } else {
            return -1;
        }
        return v < radix ? v : -1;
    }

    /**
     * Tests the Unicode ID_Start derived property.
     * <p>
     * 유니코드 파생 속성 ID_Start인지 검사한다.
     *
     * @param cp code point / 코드 포인트
     * @return true if ID_Start / ID_Start 이면 true
     */
    public static boolean isIdStart(int cp) {
        if (cp < 0x80) {
            return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z');
        }
        return UnicodeProperties.isIdStart(cp);
    }

    /**
     * Tests the Unicode ID_Continue derived property.
     * <p>
     * 유니코드 파생 속성 ID_Continue인지 검사한다.
     *
     * @param cp code point / 코드 포인트
     * @return true if ID_Continue / ID_Continue 이면 true
     */
    public static boolean isIdContinue(int cp) {
        if (cp < 0x80) {
            return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z') || (cp >= '0' && cp <= '9') || cp == '_';
        }
        return UnicodeProperties.isIdContinue(cp);
    }

    /**
     * Tests ECMAScript IdentifierStart (without escapes): ID_Start, <code>$</code> or <code>_</code>.
     * <p>
     * ECMAScript IdentifierStart 인지 검사한다(escape 제외): ID_Start, <code>$</code>, <code>_</code>.
     *
     * @param cp code point / 코드 포인트
     * @return true if it can start an identifier / 식별자 시작 문자이면 true
     */
    public static boolean isIdentifierStart(int cp) {
        return cp == '$' || cp == '_' || isIdStart(cp);
    }

    /**
     * Tests ECMAScript IdentifierPart (without escapes): ID_Continue, <code>$</code>, ZWNJ (U+200C) or ZWJ (U+200D).
     * <p>
     * ECMAScript IdentifierPart 인지 검사한다(escape 제외): ID_Continue, <code>$</code>, ZWNJ(U+200C), ZWJ(U+200D).
     *
     * @param cp code point / 코드 포인트
     * @return true if it can continue an identifier / 식별자 구성 문자이면 true
     */
    public static boolean isIdentifierPart(int cp) {
        return cp == '$' || cp == 0x200C || cp == 0x200D || isIdContinue(cp);
    }
}
