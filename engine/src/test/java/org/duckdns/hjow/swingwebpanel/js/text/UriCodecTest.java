package org.duckdns.hjow.swingwebpanel.js.text;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link UriCodec} and {@link LegacyEscape}.
 * <p>
 * {@link UriCodec} 과 {@link LegacyEscape} 의 시험.
 */
public class UriCodecTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public UriCodecTest() {
    }

    /**
     * Builds a string from one code point.
     * <p>
     * 코드 포인트 하나로 문자열을 만든다.
     *
     * @param cp code point / 코드 포인트
     * @return the string / 문자열
     */
    private static String str(int cp) {
        return new String(Character.toChars(cp));
    }

    /**
     * Runs encodeURIComponent and expects a URIError.
     * <p>
     * encodeURIComponent 를 실행하고 URIError 를 기대한다.
     *
     * @param s    input / 입력
     * @param what description / 설명
     */
    private static void expectEncodeError(final String s, String what) {
        Check.throwsException(UriFormatException.class, new Block() {
            /**
             * Encodes the malformed input.
             * <p>
             * 올바르지 않은 입력을 부호화한다.
             */
            public void run() {
                UriCodec.encodeURIComponent(s);
            }
        }, what);
    }

    /**
     * Runs decodeURIComponent and expects a URIError.
     * <p>
     * decodeURIComponent 를 실행하고 URIError 를 기대한다.
     *
     * @param s    input / 입력
     * @param what description / 설명
     */
    private static void expectDecodeError(final String s, String what) {
        Check.throwsException(UriFormatException.class, new Block() {
            /**
             * Decodes the malformed input.
             * <p>
             * 올바르지 않은 입력을 복호화한다.
             */
            public void run() {
                UriCodec.decodeURIComponent(s);
            }
        }, what);
    }

    /**
     * Checks the encode functions on reserved, unreserved, non-ASCII and astral characters.
     * <p>
     * 예약/비예약/비ASCII/보충 평면 문자에 대한 부호화 함수를 확인한다.
     */
    public void testEncode() {
        Check.equal("a%20b%26c%3Dd%2Fe%3Ff%23g", UriCodec.encodeURIComponent("a b&c=d/e?f#g"), "component keeps only unreserved");
        Check.equal("http://x/a%20b?q=1&r=%ED%95%9C#h", UriCodec.encodeURI("http://x/a b?q=1&r=한#h"), "encodeURI keeps reserved and #");
        Check.equal("%ED%95%9C", UriCodec.encodeURIComponent("한"), "Hangul");
        Check.equal("%F0%9F%98%80", UriCodec.encodeURIComponent(str(0x1F600)), "astral");
        Check.equal("%C3%A9", UriCodec.encodeURIComponent(str(0xE9)), "e acute");
        Check.equal("-_.!~*'()", UriCodec.encodeURIComponent("-_.!~*'()"), "marks");
        Check.equal("AZaz09", UriCodec.encodeURIComponent("AZaz09"), "alphanumerics");
        Check.equal("%00%7F", UriCodec.encodeURIComponent("" + (char) 0 + (char) 0x7F), "controls");
        Check.equal("", UriCodec.encodeURI(""), "empty");
        expectEncodeError("" + (char) 0xD800, "lone high surrogate");
        expectEncodeError("" + (char) 0xDC00, "lone low surrogate");
        expectEncodeError("" + (char) 0xD800 + "a", "high surrogate followed by a letter");
        expectEncodeError("a" + (char) 0xD800, "high surrogate at the end");
    }

    /**
     * Checks the decode functions including reserved handling and the strict UTF-8 validation.
     * <p>
     * 예약 문자 처리와 엄격한 UTF-8 검사를 포함한 복호화 함수를 확인한다.
     */
    public void testDecode() {
        Check.equal("한", UriCodec.decodeURIComponent("%ED%95%9C"), "Hangul");
        Check.equal(str(0x1F600), UriCodec.decodeURIComponent("%F0%9F%98%80"), "astral");
        Check.equal("%3B%2F%3F%3A%40%26%3D%2B%24%2C%23", UriCodec.decodeURI("%3B%2F%3F%3A%40%26%3D%2B%24%2C%23"),
                "decodeURI keeps reserved escapes");
        Check.equal("A b", UriCodec.decodeURI("%41%20b"), "decodeURI decodes others");
        Check.equal(";", UriCodec.decodeURIComponent("%3B"), "component decodes reserved");
        Check.equal("%3b", UriCodec.decodeURI("%3b"), "lower case reserved escape is kept as written");
        Check.equal("abc", UriCodec.decodeURIComponent("abc"), "no escapes");
        expectDecodeError("%", "lone percent");
        expectDecodeError("%4", "short escape");
        expectDecodeError("%zz", "non hex");
        expectDecodeError("%C3", "incomplete sequence");
        expectDecodeError("%C3%28", "bad continuation byte");
        expectDecodeError("%C0%80", "overlong two bytes");
        expectDecodeError("%E0%80%80", "overlong three bytes");
        expectDecodeError("%ED%A0%80", "surrogate code point");
        expectDecodeError("%F4%90%80%80", "above U+10FFFF");
        expectDecodeError("%FF", "invalid lead byte");
        expectDecodeError("%80", "continuation byte alone");
        expectDecodeError("%F8%88%80%80%80", "five byte form");
    }

    /**
     * Randomized round trip of encodeURIComponent and decodeURIComponent over valid code points.
     * <p>
     * 올바른 코드 포인트에 대한 encodeURIComponent/decodeURIComponent 무작위 왕복 시험.
     */
    public void testRoundTrip() {
        Random rnd = new Random(42L);
        // 서로게이트 영역을 피한 무작위 코드 포인트 문자열 1000 개를 왕복한다.
        for (int i = 0; i < 1000; i++) {
            StringBuilder sb = new StringBuilder();
            int len = rnd.nextInt(12);
            // 각 코드 포인트를 문자열 끝에 붙인다.
            for (int j = 0; j < len; j++) {
                int cp = rnd.nextInt(0x110000);
                if (cp >= 0xD800 && cp <= 0xDFFF) {
                    cp = 0x41 + rnd.nextInt(26);
                }
                sb.appendCodePoint(cp);
            }
            String s = sb.toString();
            Check.equal(s, UriCodec.decodeURIComponent(UriCodec.encodeURIComponent(s)), "round trip " + i);
            Check.equal(s, UriCodec.decodeURI(UriCodec.encodeURI(s)).length() == s.length() ? s : "differs", "encodeURI length " + i);
        }
    }

    /**
     * Checks the Annex B escape and unescape functions.
     * <p>
     * 부속서 B 의 escape/unescape 함수를 확인한다.
     */
    public void testLegacyEscape() {
        Check.equal("a%20b", LegacyEscape.escape("a b"), "space");
        Check.equal("%E9", LegacyEscape.escape(str(0xE9)), "below 256");
        Check.equal("%uD55C", LegacyEscape.escape("한"), "above 255");
        Check.equal("@*_+-./", LegacyEscape.escape("@*_+-./"), "unescaped set");
        Check.equal("%21%7E%27", LegacyEscape.escape("!~'"), "marks that are not in the set");
        Check.equal("%uD83D%uDE00", LegacyEscape.escape(str(0x1F600)), "surrogates escape separately");
        Check.equal("AA%zz%u12", LegacyEscape.unescape("%u0041%41%zz%u12"), "malformed sequences stay");
        Check.equal(str(0xE9), LegacyEscape.unescape("%E9"), "two digit form");
        Check.equal("한", LegacyEscape.unescape("%uD55C"), "four digit form");
        Check.equal("%", LegacyEscape.unescape("%"), "lone percent");
        Check.equal("%u00", LegacyEscape.unescape("%u00"), "short unicode form");
        Check.equal("a b", LegacyEscape.unescape("a%20b"), "space");
        String all = "abc " + (char) 0x7F + (char) 0xFF + (char) 0x100 + (char) 0xD800 + "~";
        Check.equal(all, LegacyEscape.unescape(LegacyEscape.escape(all)), "round trip");
    }
}
