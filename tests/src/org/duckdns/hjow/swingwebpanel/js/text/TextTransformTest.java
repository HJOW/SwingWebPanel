package org.duckdns.hjow.swingwebpanel.js.text;

import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link TextTransform}, {@link CodePoints} and {@link CharClass}.
 * <p>
 * {@link TextTransform}, {@link CodePoints}, {@link CharClass} 의 시험.
 */
public class TextTransformTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextTransformTest() {
    }

    /**
     * Builds a string from char codes.
     * <p>
     * char 코드로 문자열을 만든다.
     *
     * @param codes UTF-16 code units / UTF-16 코드 단위
     * @return the string / 문자열
     */
    private static String units(int... codes) {
        StringBuilder sb = new StringBuilder();
        // 코드 단위를 순서대로 붙인다.
        for (int i = 0; i < codes.length; i++) {
            sb.append((char) codes[i]);
        }
        return sb.toString();
    }

    /**
     * Checks normalize for each form and the RangeError case.
     * <p>
     * 각 정규화 형식과 RangeError 경우를 확인한다.
     */
    public void testNormalize() {
        Check.equal(units(0xE9), TextTransform.normalize(units(0x65, 0x301), null), "default form is NFC");
        Check.equal(units(0xE9), TextTransform.normalize(units(0x65, 0x301), "NFC"), "NFC composes");
        Check.equal(units(0x65, 0x301), TextTransform.normalize(units(0xE9), "NFD"), "NFD decomposes");
        Check.equal("fi", TextTransform.normalize(units(0xFB01), "NFKC"), "NFKC expands the ligature");
        Check.equal("fi", TextTransform.normalize(units(0xFB01), "NFKD"), "NFKD expands the ligature");
        Check.equal(units(0xFB01), TextTransform.normalize(units(0xFB01), "NFC"), "NFC keeps the ligature");
        Check.throwsException(TextRangeException.class, new Block() {
            /**
             * An unknown form is a RangeError.
             * <p>
             * 알 수 없는 형식은 RangeError 이다.
             */
            public void run() {
                TextTransform.normalize("a", "nfc");
            }
        }, "form names are case sensitive");
    }

    /**
     * Checks case conversion including special casing.
     * <p>
     * 특수 대소문자 규칙을 포함한 대소문자 변환을 확인한다.
     */
    public void testCaseConversion() {
        Check.equal("ABC", TextTransform.toUpperCase("abc"), "ASCII upper");
        Check.equal("abc", TextTransform.toLowerCase("ABC"), "ASCII lower");
        Check.equal("SS", TextTransform.toUpperCase(units(0xDF)), "sharp s becomes SS");
        Check.equal(units(0x3B1, 0x3C2), TextTransform.toLowerCase(units(0x391, 0x3A3)), "final sigma");
        Check.equal(units(0x3C3), TextTransform.toLowerCase(units(0x3A3)), "lone sigma is not final");
        Check.equal(units(0x69, 0x307), TextTransform.toLowerCase(units(0x130)), "capital I with dot above");
        Check.equal(units(0x130), TextTransform.toLocaleUpperCase("i", new Locale("tr")), "Turkish upper case i");
        Check.equal("FI", TextTransform.toUpperCase(units(0xFB01)), "ligature fi uppercases to FI");
        Check.equal(units(0x2BC, 0x4E), TextTransform.toUpperCase(units(0x149)), "n preceded by apostrophe uppercases to two characters");
        Check.equal(units(0x3A3), TextTransform.toUpperCase(units(0x3C2)), "final sigma uppercases to capital sigma");
        Check.equal(new String(Character.toChars(0x10400)), TextTransform.toUpperCase(new String(Character.toChars(0x10428))),
                "supplementary plane (Deseret)");
    }

    /**
     * Checks trim, trimStart, trimEnd and repeat.
     * <p>
     * trim, trimStart, trimEnd, repeat 를 확인한다.
     */
    public void testTrimAndRepeat() {
        String ws = units(0x20, 0x09, 0xA0, 0xFEFF, 0x2028, 0x3000, 0x0A);
        Check.equal("a b", TextTransform.trim(ws + "a b" + ws), "trim all white space kinds");
        Check.equal("a" + ws, TextTransform.trimStart(ws + "a" + ws), "trimStart");
        Check.equal(ws + "a", TextTransform.trimEnd(ws + "a" + ws), "trimEnd");
        Check.equal(units(0x180E) + "a", TextTransform.trim(units(0x180E) + "a"), "U+180E is not white space");
        Check.equal("", TextTransform.trim(ws), "all white space");
        Check.equal("abab", TextTransform.repeat("ab", 2), "repeat");
        Check.equal("", TextTransform.repeat("ab", 0), "repeat zero");
        Check.equal("", TextTransform.repeat("", 1000000), "repeat empty string");
        Check.equal("aa", TextTransform.repeat("a", 2.9), "fraction is truncated");
        Check.throwsException(TextRangeException.class, new Block() {
            /**
             * Negative counts are a RangeError.
             * <p>
             * 음수 횟수는 RangeError 이다.
             */
            public void run() {
                TextTransform.repeat("a", -1);
            }
        }, "negative count");
        Check.throwsException(TextRangeException.class, new Block() {
            /**
             * Infinite counts are a RangeError.
             * <p>
             * 무한 횟수는 RangeError 이다.
             */
            public void run() {
                TextTransform.repeat("a", Double.POSITIVE_INFINITY);
            }
        }, "infinite count");
        Check.throwsException(TextRangeException.class, new Block() {
            /**
             * A result that is too long is a RangeError.
             * <p>
             * 너무 긴 결과는 RangeError 이다.
             */
            public void run() {
                TextTransform.repeat("abc", 1e9);
            }
        }, "too long");
    }

    /**
     * Checks the code point helpers.
     * <p>
     * 코드 포인트 도우미를 확인한다.
     */
    public void testCodePoints() {
        String s = "a" + new String(Character.toChars(0x1F600)) + (char) 0xD800;
        Check.equal(0x61, CodePoints.codePointAt(s, 0), "ASCII");
        Check.equal(0x1F600, CodePoints.codePointAt(s, 1), "pair");
        Check.equal(0xDE00, CodePoints.codePointAt(s, 2), "low half of the pair alone");
        Check.equal(0xD800, CodePoints.codePointAt(s, 3), "lone high surrogate");
        Check.equal(-1, CodePoints.codePointAt(s, 4), "out of range");
        Check.equal(-1, CodePoints.codePointAt(s, -1), "negative index");
        Check.equal(3, CodePoints.codePointCount(s), "code point count");
        Check.equal(2, CodePoints.fromCodePoint(0x1F600).length(), "fromCodePoint astral");
        Check.equal("AB", CodePoints.fromCodePoint(65, 66), "fromCodePoint several");
        Check.equal("", CodePoints.fromCodePoint(), "fromCodePoint none");
        Check.equal("A", CodePoints.fromCharCode(0x10041), "fromCharCode uses ToUint16");
        Check.equal("A", CodePoints.fromCharCode(65.9), "fromCharCode truncates");
        Check.equal("" + (char) 0, CodePoints.fromCharCode(Double.NaN), "fromCharCode NaN");
        double[] bad = { -1, 1.5, 0x110000, Double.NaN, Double.POSITIVE_INFINITY };
        // 올바르지 않은 각 값이 RangeError 를 내는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            final double v = bad[i];
            Check.throwsException(TextRangeException.class, new Block() {
                /**
                 * An invalid code point is a RangeError.
                 * <p>
                 * 올바르지 않은 코드 포인트는 RangeError 이다.
                 */
                public void run() {
                    CodePoints.fromCodePoint(v);
                }
            }, "fromCodePoint(" + v + ")");
        }
        Check.equal("a1b2c", CodePoints.raw(new String[] { "a", "b", "c" }, new String[] { "1", "2" }), "raw with substitutions");
        Check.equal("ab", CodePoints.raw(new String[] { "a", "b" }, new String[0]), "raw without substitutions");
        Check.equal("a1b", CodePoints.raw(new String[] { "a", "b" }, new String[] { "1", "2" }), "extra substitutions are ignored");
        Check.equal("", CodePoints.raw(new String[0], new String[] { "1" }), "raw of nothing");
        Check.isTrue(CodePoints.isWellFormed(s.substring(0, 3)), "well formed");
        Check.isFalse(CodePoints.isWellFormed(s), "lone surrogate");
    }

    /**
     * Checks character classification.
     * <p>
     * 문자 분류를 확인한다.
     */
    public void testCharClass() {
        Check.isTrue(CharClass.isWhiteSpace(0x20), "space");
        Check.isTrue(CharClass.isWhiteSpace(0xA0), "nbsp");
        Check.isTrue(CharClass.isWhiteSpace(0xFEFF), "bom");
        Check.isTrue(CharClass.isWhiteSpace(0x2003), "em space");
        Check.isTrue(CharClass.isWhiteSpace(0x3000), "ideographic space");
        Check.isFalse(CharClass.isWhiteSpace(0x180E), "mongolian vowel separator");
        Check.isFalse(CharClass.isWhiteSpace(0x0A), "line feed is a line terminator");
        Check.isTrue(CharClass.isLineTerminator(0x2028), "LS");
        Check.isTrue(CharClass.isLineTerminator(0x2029), "PS");
        Check.isFalse(CharClass.isLineTerminator(0x85), "NEL is not a line terminator in ECMAScript");
        Check.isTrue(CharClass.isWhiteSpaceOrLineTerminator(0x0D), "CR");
        Check.isTrue(CharClass.isIdentifierStart('$'), "dollar");
        Check.isTrue(CharClass.isIdentifierStart('_'), "underscore");
        Check.isTrue(CharClass.isIdentifierStart(0xD55C), "Hangul syllable");
        Check.isFalse(CharClass.isIdentifierStart('1'), "digit does not start");
        Check.isTrue(CharClass.isIdentifierPart('1'), "digit continues");
        Check.isTrue(CharClass.isIdentifierPart(0x200C), "ZWNJ");
        Check.isTrue(CharClass.isIdentifierPart(0x200D), "ZWJ");
        Check.isTrue(CharClass.isIdentifierPart(0x0301), "combining acute");
        Check.isFalse(CharClass.isIdentifierPart('-'), "hyphen");
        Check.isTrue(CharClass.isIdStart(0x2118), "Other_ID_Start");
        Check.isTrue(CharClass.isIdContinue(0x00B7), "Other_ID_Continue middle dot");
        Check.equal(10, CharClass.hexValue('a'), "hex a");
        Check.equal(-1, CharClass.hexValue('g'), "hex g");
        Check.equal(35, CharClass.digitValue('z', 36), "base 36 z");
        Check.equal(-1, CharClass.digitValue('z', 35), "z is not a base 35 digit");
    }
}
