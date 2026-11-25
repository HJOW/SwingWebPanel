package org.duckdns.hjow.swingwebpanel.js.regexp;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the regular expression engine: semantics taken from ECMA-262 examples and well-known JavaScript results,
 * syntax errors, flags, case-insensitive and unicode matching, resource limits and large inputs.
 * In the Java source, regular expression text that contains a unicode escape is written with a doubled backslash so that
 * the engine receives a backslash followed by the letter u.
 * <p>
 * 정규식 엔진의 시험: ECMA-262 예제와 잘 알려진 JavaScript 결과에서 가져온 의미, 구문 오류, 플래그, 대소문자 무시와 유니코드 일치,
 * 자원 제한, 큰 입력. Java 소스에서 유니코드 escape 가 들어 있는 정규식 문자열은 엔진이 역슬래시와 글자 u 를 받도록 역슬래시를 두 번 쓴다.
 */
public class RegExpTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RegExpTest() {
    }

    /**
     * Executes a pattern and formats the result as "index:group0|group1|..." with "~" for groups that did not
     * participate, or "null" when there is no match.
     * <p>
     * 패턴을 실행하여 결과를 "위치:그룹0|그룹1|..." 형태로 만든다. 참여하지 않은 그룹은 "~", 일치가 없으면 "null".
     *
     * @param pattern pattern text / 패턴
     * @param flags   flags text / 플래그
     * @param input   input text / 입력
     * @return formatted result / 형식화된 결과
     */
    static String run(String pattern, String flags, String input) {
        try {
            RegExpProgram p = RegExpCompiler.compile(pattern, flags);
            return format(p.exec(input, 0), input);
        } catch (RegExpSyntaxException e) {
            return "SyntaxError: " + e.getReason();
        }
    }

    /**
     * Formats capture offsets as described at {@link #run(String, String, String)}.
     * <p>
     * {@link #run(String, String, String)} 에서 설명한 형태로 캡처 위치를 문자열로 만든다.
     *
     * @param caps  capture offsets or null / 캡처 위치 또는 null
     * @param input the input text / 입력 문자열
     * @return formatted result / 형식화된 결과
     */
    static String format(int[] caps, String input) {
        if (caps == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(caps[0]).append(':');
        // 그룹마다 캡처된 문자열(또는 ~)을 | 로 이어 붙인다.
        for (int g = 0; g < caps.length / 2; g++) {
            if (g > 0) {
                sb.append('|');
            }
            if (caps[2 * g] < 0) {
                sb.append('~');
            } else {
                sb.append(input.substring(caps[2 * g], caps[2 * g + 1]));
            }
        }
        return sb.toString();
    }

    /**
     * Asserts the result of running a pattern.
     * <p>
     * 패턴 실행 결과를 검증한다.
     *
     * @param expected expected formatted result / 기대하는 형식화된 결과
     * @param pattern  pattern / 패턴
     * @param flags    flags / 플래그
     * @param input    input / 입력
     */
    static void is(String expected, String pattern, String flags, String input) {
        Check.equal(expected, run(pattern, flags, input), "/" + pattern + "/" + flags + ".exec(" + input + ")");
    }

    /**
     * Asserts that compiling the pattern fails with a reason containing the text.
     * <p>
     * 패턴 컴파일이 해당 문구를 포함한 원인으로 실패하는지 검증한다.
     *
     * @param pattern pattern / 패턴
     * @param flags   flags / 플래그
     * @param reason  expected fragment of the reason / 기대하는 원인의 일부
     */
    static void bad(String pattern, String flags, String reason) {
        String r = run(pattern, flags, "");
        Check.isTrue(r.startsWith("SyntaxError: ") && r.indexOf(reason) >= 0, "/" + pattern + "/" + flags + " should fail with '" + reason + "' but gave " + r);
    }

    /**
     * Checks the examples of ECMA-262 21.2.2 about backtracking, captures and empty matches.
     * <p>
     * 역추적, 캡처, 빈 일치에 대한 ECMA-262 21.2.2 의 예제를 확인한다.
     */
    public void testSpecExamples() {
        is("0:abc|a|a|~|bc|~|bc", "((a)|(ab))((c)|(bc))", "", "abc");
        is("0:a", "a|ab", "", "abc");
        is("0:abcde", "a[a-z]{2,4}", "", "abcdefghi");
        is("0:abc", "a[a-z]{2,4}?", "", "abcdefghi");
        is("0:aaba|ba", "(aa|aabaac|ba|b|c)*", "", "aabaac");
        is("0:zaacbbbcac|z|ac|a|~|c", "(z)((a+)?(b+)?(c))*", "", "zaacbbbcac");
        is("0:|~", "(a*)*", "", "b");
        is("0:b|", "(a*)b\\1+", "", "baaaac");
        is("1:|aaa", "(?=(a+))", "", "baaabac");
        is("3:aba|a", "(?=(a+))a*b\\1", "", "baaabac");
        is("0:baaabaac|ba|~|abaac", "(.*?)a(?!(a+)b\\2c)\\2(.*)", "", "baaabaac");
        is("0:a|a", "\\1(a)", "", "aa");
    }

    /**
     * Checks capture reset per iteration, greedy and lazy quantifiers, and backtracking.
     * <p>
     * 반복마다 캡처 초기화, 탐욕/게으른 수량자, 역추적을 확인한다.
     */
    public void testQuantifiersAndCaptures() {
        is("0:b|~", "(a)|b", "", "b");
        is("0:ab|~", "(?:(a)|b)*", "", "ab");
        is("0:aab|~", "^(?:(a)|b)*$", "", "aab");
        is("0:aa|a", "(a)\\1", "", "aa");
        is("0:abcd|", "(?:a|ab)(?:c|bcd)(d*)", "", "abcd");
        is("0:aa|aa|~", "(a+)(b+)?", "", "aac");
        is("4:010-1234|010|1234", "(\\d+)-(\\d+)", "", "tel 010-1234");
        is("0:<a>", "<.+?>", "", "<a><b>");
        is("0:<a><b>", "<.+>", "", "<a><b>");
        is("0:aaa", "a{2,3}", "", "aaaa");
        is("0:aa", "a{2,3}?", "", "aaaa");
        is("0:aaaa", "a{2,}", "", "aaaa");
        is("0:aa", "a{2}", "", "aaaa");
        is("0:aaab", "a*ab", "", "aaab");
        is("0:aab", ".*?b", "", "aab");
        is("0:ababc", "(?:ab)*abc", "", "ababc");
        is("0:ababab|ab", "(ab)*", "", "abababc");
        is("0:ab", "(?:ab)+?", "", "ababab");
        is("0:abab|ab", "(ab){2}", "", "ababab");
        is("0:|~", "(a?)*?", "", "aaa");
        is("0:", "(?:)", "", "abc");
        is("0:", "", "", "abc");
        is("0:", "^$", "", "");
        is("3:", "$", "", "abc");
        is("0:a", "a|", "", "a");
        is("0:", "|a", "", "a");
        is("0:xy|y", "x(y)?", "", "xy");
        is("0:x|~", "x(y)?", "", "xz");
        is("0:abc|a|b|c", "(a)(b)(c)", "", "abc");
        is("0:a|a", "(?:(a)|b)", "", "a");
        is("1:a", "(?!b)a", "", "ba");
        is("0:ab|~", "ab(?!(c))", "", "abd");
    }

    /**
     * Checks anchors, word boundaries, line terminators and the dot.
     * <p>
     * 위치 단언, 단어 경계, 줄 끝 문자, 점을 확인한다.
     */
    public void testAssertionsAndDot() {
        is("2:b", "^b", "m", "a\nb");
        is("null", "^b", "", "a\nb");
        is("0:a", "a$", "m", "a\nb");
        is("null", "a$", "", "a\nb");
        is("1:a", "^a", "m", "\ra");
        is("1:a", "^a", "m", "" + (char) 0x2028 + "a");
        is("2:foo", "\\bfoo\\b", "", "a foo b");
        is("null", "\\bfoo\\b", "", "afoo b");
        is("1:oo", "\\Boo", "", "foo");
        is("0:a", ".", "", "a");
        is("1:b", ".", "", "\nb");
        is("1:b", ".", "", "\rb");
        is("1:b", ".", "", "" + (char) 0x2028 + "b");
        is("1:b", ".", "", "" + (char) 0x2029 + "b");
        is("0:\n", ".", "s", "\n");
        is("0:" + new String(Character.toChars(0x1F600)), "^.$", "u", new String(Character.toChars(0x1F600)));
        is("null", "^.$", "", new String(Character.toChars(0x1F600)));
        is("0:" + new String(Character.toChars(0x1F600)), "^..$", "", new String(Character.toChars(0x1F600)));
    }

    /**
     * Checks character classes, class escapes and Annex B class quirks.
     * <p>
     * 문자 클래스, 클래스 escape, 부속서 B 의 클래스 특이 동작을 확인한다.
     */
    public void testCharacterClasses() {
        is("null", "[]", "", "abc");
        is("0:\n", "[^]", "", "\n");
        is("0:-", "[a-]", "", "-");
        is("0:a", "[a-]", "", "a");
        is("0:5", "[\\d-x]", "", "5");
        is("0:-", "[\\d-x]", "", "-");
        is("0:x", "[\\d-x]", "", "x");
        is("1:a", "[^\\d]", "", "5a");
        is("1:-", "[\\W]", "", "a-");
        is("0:a", "[\\w]", "", "a");
        is("0: ", "[\\s]", "", " ");
        is("0:" + (char) 0xA0, "\\s", "", "" + (char) 0xA0);
        is("0:" + (char) 0xFEFF, "\\s", "", "" + (char) 0xFEFF);
        is("null", "\\s", "", "" + (char) 0x180E);
        is("0:b", "[a-c&&b]", "", "b");
        is("0:]", "[\\]]", "", "]");
        is("0:^", "[\\^]", "", "^");
        is("0:\b", "[\\b]", "", "\b");
        is("0:a", "[^\\n]", "", "a");
        is("0:" + (char) 1, "[\\x01]", "", "" + (char) 1);
        is("1:\n", "[\\cJ]", "", "a\n");
        is("0:" + (char) 17, "[\\c1]", "", "" + (char) 17);
        bad("[z-a]", "", "Range out of order");
        bad("[a", "", "Unterminated character class");
        bad("[\\w-a]", "u", "Invalid character class");
        bad("[\\1]", "u", "Invalid class escape");
    }

    /**
     * Checks escapes: control, hex, unicode, octal, identity and the Annex B leniency.
     * <p>
     * escape(제어, 16진, 유니코드, 8진, identity)와 부속서 B 의 관대한 규칙을 확인한다.
     */
    public void testEscapes() {
        is("0:\n", "\\cJ", "", "\n");
        is("0:\\c", "\\c", "", "\\c");
        is("0:" + (char) 0, "\\0", "", "" + (char) 0);
        is("0:\n", "\\012", "", "\n");
        is("0:8", "\\8", "", "8");
        is("0:A", "\\x41", "", "A");
        is("0:x4", "\\x4", "", "x4");
        is("0:k", "\\k", "", "k");
        is("0:A", "\\u0041", "", "A");
        is("0:u41", "\\u41", "", "u41");
        is("0:\b", "\\10", "", "\b");
        is("0:aa|a", "(a)\\1", "", "aa");
        is("0:a", "\\a", "", "a");
        is("0:/", "\\/", "", "/");
        is("0:{", "{", "", "{");
        is("0:a{", "a{", "", "a{");
        is("0:a{1", "a{1", "", "a{1");
        is("0:a{,2}", "a{,2}", "", "a{,2}");
        is("0:}", "}", "", "}");
        is("0:]", "]", "", "]");
        is("0:t", "(?:t)", "", "t");
        bad("\\", "", "\\ at end of pattern");
        bad("\\k", "u", "Invalid escape");
        bad("\\-", "u", "Invalid escape");
        bad("\\c", "u", "Invalid unicode escape");
        bad("\\x4", "u", "Invalid escape");
        bad("\\u41", "u", "Invalid Unicode escape");
        bad("\\1", "u", "Invalid escape");
        bad("{", "u", "Nothing to repeat");
        bad("}", "u", "Lone quantifier brackets");
        bad("]", "u", "Lone quantifier brackets");
        bad("a{", "u", "Incomplete quantifier");
        bad("\\00", "u", "Invalid decimal escape");
    }

    /**
     * Checks the syntax errors of the grammar.
     * <p>
     * 문법의 구문 오류를 확인한다.
     */
    public void testSyntaxErrors() {
        bad("(", "", "Unterminated group");
        bad(")", "", "Unmatched ')'");
        bad("a)", "", "Unmatched ')'");
        bad("(?", "", "Invalid group");
        bad("(?x)", "", "Invalid group");
        bad("(?:a", "", "Unterminated group");
        bad("*a", "", "Nothing to repeat");
        bad("+", "", "Nothing to repeat");
        bad("?", "", "Nothing to repeat");
        bad("a**", "", "Nothing to repeat");
        bad("a+*", "", "Nothing to repeat");
        bad("a{1}{2}", "", "Nothing to repeat");
        bad("{1}", "", "Nothing to repeat");
        bad("^*", "", "Nothing to repeat");
        bad("$+", "", "Nothing to repeat");
        bad("\\b+", "", "Nothing to repeat");
        bad("a{2,1}", "", "numbers out of order");
        bad("(?=a)*", "u", "Nothing to repeat");
        bad("[", "", "Unterminated character class");
        bad("(?<n>a)(?<n>b)", "", "Duplicate capture group name");
        bad("(?<1a>a)", "", "Invalid capture group name");
        bad("(?<n>a)\\k<m>", "", "Invalid named capture referenced");
        bad("\\u{110000}", "u", "Invalid Unicode escape");
        bad("\\u{}", "u", "Invalid Unicode escape");
        is("0:aa", "(?=a)*aa", "", "aa");
        is("0:a|a", "(?=(a))a", "", "a");
    }

    /**
     * Checks flag parsing and the early error validator.
     * <p>
     * 플래그 해석과 조기 오류 검사기를 확인한다.
     */
    public void testFlagsAndValidate() throws Exception {
        Check.equal(RegExpFlags.GLOBAL | RegExpFlags.IGNORE_CASE, RegExpFlags.parse("gi"), "gi");
        Check.equal("gimsuy", RegExpFlags.toString(RegExpFlags.parse("yusmig")), "canonical order");
        Check.equal(0, RegExpFlags.parse(null), "null flags");
        Check.throwsException(RegExpSyntaxException.class, new Block() {
            /**
             * A repeated flag is invalid.
             * <p>
             * 반복된 플래그는 올바르지 않다.
             */
            public void run() throws Exception {
                RegExpFlags.parse("gg");
            }
        }, "repeated flag");
        Check.throwsException(RegExpSyntaxException.class, new Block() {
            /**
             * An unknown flag is invalid.
             * <p>
             * 알 수 없는 플래그는 올바르지 않다.
             */
            public void run() throws Exception {
                RegExpFlags.parse("x");
            }
        }, "unknown flag");
        RegExpCompiler.validate("a(b|c)*[d-f]", "gi");
        Check.throwsException(RegExpSyntaxException.class, new Block() {
            /**
             * An invalid pattern fails validation.
             * <p>
             * 올바르지 않은 패턴은 검사에서 실패한다.
             */
            public void run() throws Exception {
                RegExpCompiler.validate("a(", "");
            }
        }, "validate pattern");
        Check.throwsException(RegExpSyntaxException.class, new Block() {
            /**
             * An invalid flag fails validation.
             * <p>
             * 올바르지 않은 플래그는 검사에서 실패한다.
             */
            public void run() throws Exception {
                RegExpCompiler.validate("a", "q");
            }
        }, "validate flags");
        try {
            RegExpCompiler.compile("ab(", "");
            Check.fail("expected a syntax error");
        } catch (RegExpSyntaxException e) {
            Check.equal("Invalid regular expression: /ab(/: Unterminated group", e.getMessage(), "message");
            Check.equal(2, e.getPosition(), "position of the open parenthesis");
            Check.equal("ab(", e.getSource(), "source");
        }
    }

    /**
     * Checks case-insensitive matching with and without the unicode flag.
     * <p>
     * u 플래그가 있을 때와 없을 때의 대소문자 무시 일치를 확인한다.
     */
    public void testIgnoreCase() {
        is("0:ABC", "abc", "i", "ABC");
        is("0:ABC", "[a-z]+", "i", "ABC");
        is("0:abc", "[A-Z]+", "i", "abc");
        is("null", "[^a]", "i", "A");
        is("0:b", "[^a]", "i", "b");
        is("0:Aa|A", "(a)\\1", "i", "Aa");
        is("0:" + (char) 0x1C5, "\u01c6", "i", "" + (char) 0x1C5);
        is("0:" + (char) 0x3BC, "[\\u00b5]", "i", "" + (char) 0x3BC);
        is("null", "\\u212a", "i", "k");
        is("0:k", "\\u212a", "iu", "k");
        is("0:" + (char) 0x212A, "k", "iu", "" + (char) 0x212A);
        is("null", "s", "i", "" + (char) 0x17F);
        is("0:" + (char) 0x17F, "s", "iu", "" + (char) 0x17F);
        is("null", "\\u00df", "i", "SS");
        is("0:Σ", "σ", "i", "Σ");
        is("0:ς", "Σ", "i", "ς");
        is("0:" + new String(Character.toChars(0x10428)), "\\u{10400}", "iu", new String(Character.toChars(0x10428)));
        is("0:xK|K", "x(k)", "i", "xK");
        is("null", "[^\\w]", "i", "a");
    }

    /**
     * Checks unicode mode: code point matching, escapes, surrogate pairs and classes.
     * <p>
     * 유니코드 모드를 확인한다: 코드 포인트 일치, escape, 서로게이트 쌍, 클래스.
     */
    public void testUnicodeMode() {
        String smile = new String(Character.toChars(0x1F600));
        is("0:" + smile, "\\u{1F600}", "u", smile);
        is("0:" + smile, "\\ud83d\\ude00", "u", smile);
        is("0:" + smile, "^[\\ud83d\\ude00]$", "u", smile);
        is("0:" + smile, "^[^a]$", "u", smile);
        is("null", "^[^a]$", "", smile);
        is("0:" + smile + smile, "(?:" + smile + ")+", "u", smile + smile);
        is("0:" + smile, "[" + smile + "]", "u", smile);
        is("0:" + smile, "\\W", "u", smile);
        is("null", "^\\W$", "", smile);
        is("0:" + (char) 0xD83D, "\\ud83d", "", smile);
        is("null", "\\ud83d", "u", smile);
        is("0:" + smile, "^.{1}$", "u", smile);
        is("0:" + smile, "^.+$", "u", smile);
        is("1:" + (char) 0xDE00, "[\\ude00]", "", smile);
        is("null", "[\\ude00]", "u", smile);
        is("0:" + smile, "[\\u{1F600}-\\u{1F64F}]", "u", smile);
        is("0:a", "a", "u", "a");
        RegExpProgram p;
        try {
            p = RegExpCompiler.compile(".", "u");
            // 서로게이트 쌍의 가운데에서는 검색을 앞으로 진행해도 쌍을 쪼개지 않아야 한다.
            int[] r = p.exec("x" + smile + "y", 1);
            Check.equal("1:" + smile, format(r, "x" + smile + "y"), "pair stays whole");
            r = p.exec("x" + smile + "y", 3);
            Check.equal("3:y", format(r, "x" + smile + "y"), "after the pair");
        } catch (RegExpSyntaxException e) {
            Check.fail(e.getMessage());
        }
    }

    /**
     * Checks lookahead semantics: atomic body, captures kept, negative lookahead undoes captures.
     * <p>
     * 선행 탐색 의미를 확인한다: 본문은 원자적, 캡처는 유지, 부정 선행 탐색은 캡처를 되돌린다.
     */
    public void testLookahead() {
        is("0:a|a", "(?=(a))a", "", "a");
        is("0:|aab", "(?=(a*b))", "", "aab");
        is("0:ab", "a(?=b)b", "", "ab");
        is("null", "a(?!b)", "", "ab");
        is("1:a|~", "(?!(b))a", "", "ba");
        is("0:a|~", "a(?!(x))", "", "a");
        is("null", "(?=a)b", "", "ab");
        is("0:aaaa|aaaa", "(?=(a+))a*", "", "aaaa");
        is("0:aaaa|aaaa", "(?=(a+))\\1", "", "aaaa");
        is("0:ab", "(?=ab)ab", "", "ab");
        is("0:abc", "(?=a(?=b))abc", "", "abc");
        is("0:", "(?!(?!a))", "", "a");
        is("1:", "(?!(?=a))", "", "a");
        is("0:a", "(?=a)*a", "", "a");
        is("0:aab|~", "(?=(a))*aab", "", "aab");
    }

    /**
     * Checks named groups and named back references (ES2018 extension).
     * <p>
     * 이름 있는 그룹과 이름 있는 역참조를 확인한다(ES2018 확장).
     */
    public void testNamedGroups() throws Exception {
        is("0:2016-03|2016|03", "(?<y>\\d{4})-(?<m>\\d\\d)", "", "2016-03");
        is("0:abab|ab", "(?<x>ab)\\k<x>", "", "abab");
        RegExpProgram p = RegExpCompiler.compile("(?<y>\\d{4})-(?<m>\\d\\d)", "");
        Check.equal("y", p.getGroupName(1), "name of group 1");
        Check.equal("m", p.getGroupName(2), "name of group 2");
        Check.isNull(p.getGroupName(3), "no third group");
        Check.equal(2, p.groupCount(), "group count");
        Check.isNull(RegExpCompiler.compile("(a)", "").getGroupName(1), "unnamed group");
    }

    /**
     * Checks the start index, scanning, sticky style matching and anchoring optimizations.
     * <p>
     * 시작 위치, 앞쪽 탐색, 고정 위치 일치, 시작 고정 최적화를 확인한다.
     */
    public void testStartIndexAndSticky() throws Exception {
        RegExpProgram a = RegExpCompiler.compile("a", "");
        Check.equal("1:a", format(a.exec("xaa", 1), "xaa"), "scan from 1");
        Check.equal("2:a", format(a.exec("xaa", 2), "xaa"), "scan from 2");
        Check.isNull(a.exec("xaa", 3), "scan from the end");
        Check.isNull(a.exec("xaa", 4), "start beyond the end");
        Check.equal("0:a", format(a.exec("a", -5), "a"), "negative start is 0");
        Check.isNull(a.matchAt("xaa", 0), "sticky at 0 fails");
        Check.equal("1:a", format(a.matchAt("xaa", 1), "xaa"), "sticky at 1");
        Check.isNull(a.matchAt("xaa", 9), "sticky beyond the end");
        RegExpProgram anchored = RegExpCompiler.compile("^a", "");
        Check.equal("0:a", format(anchored.exec("ab", 0), "ab"), "anchored at 0");
        Check.isNull(anchored.exec("ab", 1), "anchored with start 1");
        Check.isNull(anchored.exec("ba", 0), "anchored no match");
        RegExpProgram empty = RegExpCompiler.compile("", "");
        Check.equal("3:", format(empty.exec("abc", 3), "abc"), "empty match at the end");
        RegExpProgram alt = RegExpCompiler.compile("^a|b", "");
        Check.equal("1:b", format(alt.exec("ab", 1), "ab"), "alternation with one anchored branch");
    }

    /**
     * Checks protection against catastrophic backtracking and cancellation.
     * <p>
     * 치명적인 역추적 방어와 취소를 확인한다.
     */
    public void testResourceLimits() throws Exception {
        final RegExpProgram evil = RegExpCompiler.compile("(a+)+$", "");
        evil.setStepLimit(200000L);
        final String input = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab";
        RegExpLimitException ex = (RegExpLimitException) Check.throwsException(RegExpLimitException.class, new Block() {
            /**
             * A catastrophic pattern must stop at the step limit.
             * <p>
             * 치명적인 패턴은 단계 한도에서 멈춰야 한다.
             */
            public void run() {
                evil.exec(input, 0);
            }
        }, "step limit");
        Check.isFalse(ex.isCancelled(), "limit is not a cancellation");
        final RegExpProgram p = RegExpCompiler.compile("(a+)+$", "");
        RegExpLimitException cancelled = (RegExpLimitException) Check.throwsException(RegExpLimitException.class, new Block() {
            /**
             * A cancellation hook that always cancels must stop the match.
             * <p>
             * 항상 취소하는 훅은 일치 검색을 멈춰야 한다.
             */
            public void run() {
                p.exec(input, 0, new RegExpCancel() {
                    /**
                     * Always cancels.
                     * <p>
                     * 항상 취소한다.
                     *
                     * @return true / true
                     */
                    public boolean isCancelled() {
                        return true;
                    }
                });
            }
        }, "cancel hook");
        Check.isTrue(cancelled.isCancelled(), "cancellation flag");
        final RegExpProgram assertion = RegExpCompiler.compile("(?<=a+)b", "");
        assertion.setStepLimit(20L);
        Check.throwsException(RegExpLimitException.class, new Block() {
            /**
             * The reverse repetition consumes the parent matcher's step budget.
             * <p>
             * 역방향 반복은 상위 검색기의 단계 한도를 소비한다.
             */
            public void run() {
                assertion.matchAt(input, input.length() - 1);
            }
        }, "step limit covers reverse assertion repetition");
        final RegExpProgram loop = RegExpCompiler.compile("(?:ab)*c", "");
        loop.setMaxBacktrackEntries(1000);
        final StringBuilder big = new StringBuilder();
        // 일반 반복문은 반복마다 항목을 쌓으므로 작은 스택 한도에서 실패해야 한다.
        for (int i = 0; i < 20000; i++) {
            big.append("ab");
        }
        Check.throwsException(RegExpLimitException.class, new Block() {
            /**
             * A tiny backtracking stack limit must be reported.
             * <p>
             * 아주 작은 역추적 스택 한도는 보고되어야 한다.
             */
            public void run() {
                loop.exec(big.toString(), 0);
            }
        }, "stack limit");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Non-positive limits are rejected.
             * <p>
             * 양수가 아닌 한도는 거부한다.
             */
            public void run() {
                evil.setStepLimit(0);
            }
        }, "limit validation");
    }

    /**
     * Checks that very long inputs neither overflow the Java stack nor take quadratic memory.
     * <p>
     * 아주 긴 입력이 Java 스택을 넘치게 하거나 제곱 메모리를 쓰지 않는지 확인한다.
     */
    public void testLargeInputs() throws Exception {
        StringBuilder sb = new StringBuilder();
        // 백만 개의 a 를 만든다.
        for (int i = 0; i < 1000000; i++) {
            sb.append('a');
        }
        String a = sb.toString();
        Check.equal(1000000, RegExpCompiler.compile("a*", "").exec(a, 0)[1], "a* over one million characters");
        Check.equal(1000000, RegExpCompiler.compile("a*?$", "").exec(a, 0)[1], "lazy repeat with a final anchor");
        Check.equal(1000000, RegExpCompiler.compile("(?:a)+", "").exec(a, 0)[1], "non-capturing single atom repeat");
        Check.equal(1000000, RegExpCompiler.compile("[ab]*", "").exec(a, 0)[1], "class repeat");
        StringBuilder ab = new StringBuilder();
        // 5만 번의 ab 반복을 만든다.
        for (int i = 0; i < 50000; i++) {
            ab.append("ab");
        }
        RegExpProgram p = RegExpCompiler.compile("(?:ab)*c", "");
        Check.isNull(p.matchAt(ab.toString(), 0), "no match but no overflow");
        RegExpProgram q = RegExpCompiler.compile("(a|b)*", "");
        int[] r = q.exec(ab.toString(), 0);
        Check.equal(100000, r[1], "alternation loop over 100000 characters");
        Check.equal(99999, r[2], "last iteration capture start");
    }

    /**
     * Checks a mix of everyday patterns.
     * <p>
     * 일상적인 패턴들을 섞어서 확인한다.
     */
    public void testEverydayPatterns() {
        is("0:user@example.com|user|example.com", "^([\\w.]+)@([\\w.]+)$", "", "user@example.com");
        is("0:2016-03-01|2016|03|01", "^(\\d{4})-(\\d{2})-(\\d{2})$", "", "2016-03-01");
        is("6:world", "w\\w+", "", "hello world");
        is("0:#FF00AA|FF00AA", "^#([0-9a-f]{6})$", "i", "#FF00AA");
        is("0:  a b  |~", "\\s*a\\s+b\\s*(c)?", "", "  a b  ");
        is("0:a,b,c", "[a-z](?:,[a-z])*", "", "a,b,c");
        is("0:<b>x</b>|b", "<(\\w+)>[^<]*</\\1>", "", "<b>x</b>");
        is("null", "<(\\w+)>[^<]*</\\1>", "", "<b>x</i>");
        is("0:한글|한글", "([가-힣]+)", "", "한글");
        is("0:", "x*?", "", "x");
        is("0:abc", "abc", "", "abc");
    }
}
