package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Differential tests with random patterns.
 * <p>
 * <code>testAgainstReference</code> generates patterns over the whole ECMAScript 2015 grammar subset (captures inside
 * quantifiers, empty-matching quantified bodies, back references including forward references, lookaheads, multiline
 * anchors, word boundaries, i and m flags) and checks that the compiled virtual machine returns exactly what the
 * naive specification-style {@link ReferenceMatcher} returns for the same syntax tree.
 * <p>
 * <code>testAgainstJavaRegex</code> generates a restricted subset on which <code>java.util.regex</code> has the same semantics
 * and compares with it. Java's regex engine has known bugs (the JRE 6 version returned no match for a pattern that can
 * match the empty string), so a disagreement counts as a failure only when the reference matcher agrees with Java;
 * disagreements where the reference sides with this engine are counted and bounded.
 * The random seeds are fixed so failures are reproducible; failure messages contain pattern, flags and input.
 * <p>
 * 무작위 패턴을 쓰는 차분 시험.
 * <p>
 * <code>testAgainstReference</code> 는 ECMAScript 2015 문법 부분집합 전체(수량자 안의 캡처, 빈 문자열과 일치하는 수량자 본문, 앞쪽 참조를 포함한 역참조,
 * 선행 탐색, 여러 줄 단언, 단어 경계, i/m 플래그)에서 패턴을 만들고, 컴파일된 가상 기계가 같은 구문 트리에 대한 규격 방식의 단순한
 * {@link ReferenceMatcher} 와 정확히 같은 결과를 내는지 확인한다.
 * <p>
 * <code>testAgainstJavaRegex</code> 는 <code>java.util.regex</code> 와 의미가 같은 제한된 부분집합을 만들어 비교한다. Java 의 정규식 엔진에는 알려진 버그가
 * 있어서(JRE 6 은 빈 문자열과 일치할 수 있는 패턴에 일치 없음을 반환한 적이 있음) 참조 일치 검색기가 Java 와 같은 쪽일 때만 불일치를 실패로 보고,
 * 참조가 이 엔진 편이면 세어 두고 상한을 둔다. 난수 시드가 고정되어 실패를 재현할 수 있으며 실패 메시지에 패턴, 플래그, 입력이 들어 있다.
 */
public class RegExpFuzzTest {

    /**
     * Random source of the generator.
     * <p>
     * 생성기의 난수원.
     */
    private Random rnd;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RegExpFuzzTest() {
    }

    /**
     * Picks a random literal of the alphabet.
     * <p>
     * 알파벳에서 임의의 리터럴을 고른다.
     *
     * @return one of a, b, c / a, b, c 중 하나
     */
    private String lit() {
        return String.valueOf((char) ('a' + rnd.nextInt(3)));
    }

    /**
     * Generates an atom for the restricted grammar that always consumes at least one character.
     * <p>
     * 제한된 문법에서 항상 한 글자 이상 소비하는 원자를 만든다.
     *
     * @param depth      remaining nesting depth / 남은 중첩 깊이
     * @param canCapture whether capturing groups are allowed here / 여기서 캡처 그룹을 쓸 수 있는지
     * @return atom text / 원자 문자열
     */
    private String atom(int depth, boolean canCapture) {
        int r = rnd.nextInt(depth > 0 ? 7 : 4);
        switch (r) {
        case 0:
        case 1:
            return lit();
        case 2:
            return ".";
        case 3: {
            String[] classes = { "[ab]", "[^a]", "[a-c]", "[^bc]", "[b-c]" };
            return classes[rnd.nextInt(classes.length)];
        }
        default: {
            boolean capture = canCapture && rnd.nextBoolean();
            return (capture ? "(" : "(?:") + alternation(depth - 1, canCapture && capture, false) + ")";
        }
        }
    }

    /**
     * Generates an alternation of the restricted grammar.
     * <p>
     * 제한된 문법의 선택 구문을 만든다.
     *
     * @param depth      remaining depth / 남은 깊이
     * @param canCapture whether capturing groups are allowed / 캡처 그룹을 쓸 수 있는지
     * @param mayBeEmpty whether empty alternatives are allowed / 빈 선택지를 허용하는지
     * @return alternation text / 선택 구문 문자열
     */
    private String alternation(int depth, boolean canCapture, boolean mayBeEmpty) {
        int n = 1 + rnd.nextInt(3);
        StringBuilder sb = new StringBuilder();
        // 선택지를 n 개 만들어 | 로 잇는다.
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append('|');
            }
            if (mayBeEmpty && rnd.nextInt(8) == 0) {
                continue;
            }
            sb.append(sequence(depth, canCapture, !mayBeEmpty));
        }
        return sb.toString();
    }

    /**
     * Generates a concatenation of terms of the restricted grammar.
     * <p>
     * 제한된 문법의 항 연결을 만든다.
     *
     * @param depth       remaining depth / 남은 깊이
     * @param canCapture  whether capturing groups are allowed / 캡처 그룹을 쓸 수 있는지
     * @param mustConsume whether the sequence must consume at least one character / 한 글자 이상 소비해야 하는지
     * @return sequence text / 연결 문자열
     */
    private String sequence(int depth, boolean canCapture, boolean mustConsume) {
        int n = 1 + rnd.nextInt(3);
        StringBuilder sb = new StringBuilder();
        // 항을 n 개 만든다. 소비가 필수이면 첫 항은 반드시 한 글자 이상 소비하는 형태로 만든다.
        for (int i = 0; i < n; i++) {
            boolean first = i == 0;
            int kind = rnd.nextInt(10);
            if (!(mustConsume && first) && kind == 0) {
                sb.append(rnd.nextBoolean() ? "^" : "$");
            } else if (!(mustConsume && first) && kind == 1) {
                sb.append(rnd.nextBoolean() ? "(?=" : "(?!").append(sequence(Math.max(0, depth - 1), false, true)).append(')');
            } else {
                boolean quantify = rnd.nextInt(3) == 0;
                String a = atom(depth, canCapture && !quantify);
                sb.append(a);
                if (quantify) {
                    String[] plain = { "*", "+", "?", "{1,2}", "{2}", "{0,2}", "*?", "+?", "??", "{1,2}?", "{2,}" };
                    String q = plain[rnd.nextInt(plain.length)];
                    if (mustConsume && first) {
                        String[] strong = { "+", "{1,2}", "{2}", "+?", "{1,2}?", "{2,}" };
                        q = strong[rnd.nextInt(strong.length)];
                    }
                    sb.append(q);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generates a random input over a, b, c and their upper case forms.
     * <p>
     * a, b, c 와 그 대문자로 이루어진 무작위 입력을 만든다.
     *
     * @return input text / 입력 문자열
     */
    private String input() {
        int n = rnd.nextInt(9);
        StringBuilder sb = new StringBuilder();
        // 길이 n 의 무작위 문자열을 만든다.
        for (int i = 0; i < n; i++) {
            sb.append("abcABC".charAt(rnd.nextInt(6)));
        }
        return sb.toString();
    }

    /**
     * Generates an atom of the rich grammar (anything the engine supports, empty-matching bodies included).
     * <p>
     * 풍부한 문법(엔진이 지원하는 모든 것, 빈 본문 포함)의 원자를 만든다.
     *
     * @param depth remaining depth / 남은 깊이
     * @return atom text / 원자 문자열
     */
    private String richAtom(int depth) {
        int r = rnd.nextInt(depth > 0 ? 12 : 7);
        switch (r) {
        case 0:
        case 1:
        case 2:
            return lit();
        case 3:
            return ".";
        case 4: {
            String[] classes = { "[ab]", "[^a]", "[a-c]", "\\w", "\\W", "[^\\n]", "\\s", "[]", "[^]" };
            return classes[rnd.nextInt(classes.length)];
        }
        case 5:
            return "\\" + (1 + rnd.nextInt(3));
        case 6:
            return rnd.nextBoolean() ? "\\b" : "\\B";
        case 7:
        case 8:
            return "(" + richAlternation(depth - 1) + ")";
        case 9:
            return "(?:" + richAlternation(depth - 1) + ")";
        case 10:
            return "(?=" + richAlternation(depth - 1) + ")";
        default:
            return "(?!" + richAlternation(depth - 1) + ")";
        }
    }

    /**
     * Generates an alternation of the rich grammar.
     * <p>
     * 풍부한 문법의 선택 구문을 만든다.
     *
     * @param depth remaining depth / 남은 깊이
     * @return alternation text / 선택 구문 문자열
     */
    private String richAlternation(int depth) {
        int n = 1 + rnd.nextInt(3);
        StringBuilder sb = new StringBuilder();
        // 선택지 n 개를 만든다. 각 선택지는 비어 있을 수도 있다.
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append('|');
            }
            int terms = rnd.nextInt(4);
            // 선택지 하나에 항을 0 개에서 3 개까지 이어 붙인다.
            for (int t = 0; t < terms; t++) {
                if (rnd.nextInt(8) == 0) {
                    sb.append(rnd.nextBoolean() ? "^" : "$");
                    continue;
                }
                String a = richAtom(depth);
                sb.append(a);
                boolean quantifiable = !(a.equals("\\b") || a.equals("\\B"));
                if (quantifiable && rnd.nextInt(3) == 0) {
                    String[] qs = { "*", "+", "?", "{1,2}", "{2}", "{0,2}", "*?", "+?", "??", "{0,1}?", "{2,}", "{1,}?" };
                    sb.append(qs[rnd.nextInt(qs.length)]);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generates a random input with letters, a space and a line feed.
     * <p>
     * 글자, 공백, 줄 바꿈이 섞인 무작위 입력을 만든다.
     *
     * @return input text / 입력 문자열
     */
    private String richInput() {
        int n = rnd.nextInt(9);
        StringBuilder sb = new StringBuilder();
        // 길이 n 의 무작위 문자열을 만든다.
        for (int i = 0; i < n; i++) {
            sb.append("abcAB \n".charAt(rnd.nextInt(7)));
        }
        return sb.toString();
    }

    /**
     * Compares the virtual machine with the specification-style reference matcher on random patterns of the full
     * ECMAScript 2015 subset.
     * <p>
     * 가상 기계를 ECMAScript 2015 부분집합 전체의 무작위 패턴에서 규격 방식 참조 일치 검색기와 비교한다.
     */
    public void testAgainstReference() throws Exception {
        rnd = new Random(31415926L);
        int compared = 0;
        int matched = 0;
        // 패턴 5000 개를 만들고 각 패턴마다 입력 6 개를 비교한다.
        for (int i = 0; i < 5000; i++) {
            String pattern = richAlternation(3);
            int flags = (rnd.nextInt(4) == 0 ? RegExpFlags.IGNORE_CASE : 0) | (rnd.nextInt(4) == 0 ? RegExpFlags.MULTILINE : 0);
            RegExpProgram prog;
            RegExpNode tree;
            try {
                prog = RegExpCompiler.compile(pattern, flags);
                tree = new RegExpParser(pattern, flags).parse();
            } catch (RegExpSyntaxException e) {
                Check.fail("generator produced an invalid pattern /" + pattern + "/: " + e.getMessage());
                return;
            }
            RegExpParser groupCounter = new RegExpParser(pattern, flags);
            groupCounter.parse();
            int groups = groupCounter.getGroupCount();
            Check.equal(groups, prog.groupCount(), "group count of /" + pattern + "/");
            // 패턴마다 입력 6 개를 시도한다.
            for (int k = 0; k < 6; k++) {
                String in = richInput();
                String where = "/" + pattern + "/" + RegExpFlags.toString(flags) + " on " + escape(in);
                int[] actual = prog.exec(in, 0);
                int[] expected = new ReferenceMatcher(in, flags).exec(tree, groups, 0);
                Check.equal(RegExpTest.format(expected, in), RegExpTest.format(actual, in), "mismatch for " + where);
                Check.equal(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual), "offsets for " + where);
                compared++;
                if (actual != null) {
                    matched++;
                }
            }
        }
        Check.equal(30000, compared, "all pairs compared");
        Check.isTrue(matched > 8000, "enough successful matches to be meaningful: " + matched);
    }

    /**
     * Makes line feeds visible in failure messages.
     * <p>
     * 실패 메시지에서 줄 바꿈이 보이도록 바꾼다.
     *
     * @param s text / 문자열
     * @return quoted text / 인용한 문자열
     */
    private static String escape(String s) {
        return "\"" + s.replace("\n", "\\n") + "\"";
    }

    /**
     * Compares this engine with <code>java.util.regex</code> on a restricted grammar, adjudicating disagreements with
     * the reference matcher.
     * <p>
     * 이 엔진을 제한된 문법에서 <code>java.util.regex</code> 와 비교하고, 불일치는 참조 일치 검색기로 판정한다.
     */
    public void testAgainstJavaRegex() throws Exception {
        rnd = new Random(20261116L);
        int compared = 0;
        int matched = 0;
        int javaDeviations = 0;
        // 패턴을 3000 개 만들고 각 패턴마다 입력 6 개로 비교한다.
        for (int i = 0; i < 3000; i++) {
            String pattern = alternation(3, true, true);
            boolean ignoreCase = rnd.nextInt(4) == 0;
            int flags = ignoreCase ? RegExpFlags.IGNORE_CASE : 0;
            RegExpProgram js = RegExpCompiler.compile(pattern, flags);
            RegExpParser parser = new RegExpParser(pattern, flags);
            RegExpNode tree = parser.parse();
            Pattern javaPattern = Pattern.compile(pattern, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            // 패턴마다 입력 6 개를 시도한다.
            for (int k = 0; k < 6; k++) {
                String in = input();
                String where = "/" + pattern + "/" + (ignoreCase ? "i" : "") + " on \"" + in + "\"";
                int[] caps = js.exec(in, 0);
                int[] ref = new ReferenceMatcher(in, flags).exec(tree, parser.getGroupCount(), 0);
                Check.equal(java.util.Arrays.toString(ref), java.util.Arrays.toString(caps), "engine versus reference for " + where);
                Matcher m = javaPattern.matcher(in);
                boolean found = m.find();
                compared++;
                if (caps != null) {
                    matched++;
                }
                boolean same = found == (caps != null);
                // 일치하면 전체 범위와 모든 그룹의 시작/끝도 같아야 한다.
                if (same && found) {
                    for (int g = 0; g <= m.groupCount() && same; g++) {
                        same = m.start(g) == caps[2 * g] && m.end(g) == caps[2 * g + 1];
                    }
                }
                if (!same) {
                    javaDeviations++;
                }
            }
        }
        Check.isTrue(matched > 3000, "enough successful matches to be meaningful: " + matched);
        Check.isTrue(javaDeviations * 100 <= compared, "java.util.regex deviations should be rare: " + javaDeviations + " of " + compared);
    }
}
