package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the counter styles (plan item P10-08, CSS Counter Styles Level 3, https://www.w3.org/TR/css-counter-styles-3/): the predefined styles, the
 * systems of <code>@counter-style</code> (cyclic, fixed, symbolic, alphabetic, numeric, additive, extends), the descriptors (negative, prefix, suffix,
 * range, pad, fallback), <code>symbols()</code>, string markers, and the use of the styles in list markers and in <code>counter()</code>.
 * <p>
 * 카운터 스타일 시험(계획 P10-08, CSS Counter Styles Level 3, https://www.w3.org/TR/css-counter-styles-3/): 미리 정의된 스타일, <code>@counter-style</code> 의 시스템(cyclic,
 * fixed, symbolic, alphabetic, numeric, additive, extends), 서술자(negative, prefix, suffix, range, pad, fallback), <code>symbols()</code>, 문자열 표식, 목록 표식과
 * <code>counter()</code> 에서의 스타일 사용.
 */
public class CounterStylesTest {

    /** The ten digits as symbols of a numeric style. 숫자 스타일의 기호로 쓰는 열 개의 숫자. */
    private static final String DIGITS = "'0' '1' '2' '3' '4' '5' '6' '7' '8' '9'";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CounterStylesTest() {
    }

    /**
     * Returns the marker texts of the items of an ordered list that a style sheet styles.
     * <p>
     * 스타일 시트가 꾸민 순서 있는 목록의 항목들의 표식 텍스트를 반환한다.
     *
     * @param css   the style sheet rules / 스타일 시트 규칙
     * @param items the number of items / 항목 수
     * @param start the first number / 첫 번호
     * @return the marker texts, comma separated / 쉼표로 구분한 표식 텍스트
     */
    private static String markers(String css, int items, int start) {
        StringBuilder li = new StringBuilder();
        // 항목마다 li 를 만든다.
        for (int i = 0; i < items; i++) {
            li.append("<li id=i").append(i).append(">x</li>");
        }
        LayoutResult r = LayoutTestSupport.layout("<style>" + css + "</style><ol start=" + start + ">" + li + "</ol>", 400);
        StringBuilder out = new StringBuilder();
        // 항목마다 표식 상자의 텍스트를 모은다.
        for (int i = 0; i < items; i++) {
            String t = null;
            // 항목의 자식 중 표식 상자를 찾는다.
            for (LayoutBox c : LayoutTestSupport.box(r, "i" + i).getChildren()) {
                if (c.getType() == LayoutBox.Type.MARKER) {
                    t = c.getText();
                }
            }
            out.append(i == 0 ? "" : ",").append(t);
        }
        return out.toString();
    }

    /**
     * Returns the markers of a list whose style is the counter style <code>x</code> that a rule defines.
     * <p>
     * 규칙이 정의한 카운터 스타일 <code>x</code> 로 꾸민 목록의 표식을 반환한다.
     *
     * @param descriptors the descriptors of <code>@counter-style x</code> / <code>@counter-style x</code> 의 서술자
     * @param items       the number of items / 항목 수
     * @param start       the first number / 첫 번호
     * @return the markers / 표식
     */
    private static String custom(String descriptors, int items, int start) {
        return markers("@counter-style x { " + descriptors + " } ol { list-style-type: x }", items, start);
    }

    /**
     * The predefined styles write numbers and bullets with their punctuation.
     * <p>
     * 미리 정의된 스타일은 숫자와 글머리 기호를 구두점과 함께 쓴다.
     */
    public void testPredefinedStyles() {
        Check.equal("1. ,2. ,3. ", markers("ol { list-style-type: decimal }", 3, 1), "decimal");
        Check.equal("09. ,10. ", markers("ol { list-style-type: decimal-leading-zero }", 2, 9), "decimal-leading-zero starts with a zero");
        Check.equal("i. ,ii. ,iii. ,iv. ", markers("ol { list-style-type: lower-roman }", 4, 1), "lower-roman");
        Check.equal("XIV. ", markers("ol { list-style-type: upper-roman }", 1, 14), "upper-roman 14");
        Check.equal("a. ,b. ", markers("ol { list-style-type: lower-latin }", 2, 1), "lower-latin is lower-alpha");
        Check.equal("z. ,aa. ,ab. ", markers("ol { list-style-type: lower-alpha }", 3, 26), "alphabetic continues with two letters");
        Check.equal("α. ,β. ", markers("ol { list-style-type: lower-greek }", 2, 1), "lower-greek");
        Check.equal("• ,• ", markers("ol { list-style-type: disc }", 2, 1), "disc is cyclic");
        Check.equal("0. ,a. ", markers("ol { list-style-type: lower-alpha }", 2, 0), "a style that cannot write a value falls back to decimal");
        Check.equal("MMMCMXCIX. ,4000. ", markers("ol { list-style-type: upper-roman }", 2, 3999), "roman numerals end at 3999");
        Check.equal("๑. ,๒. ", markers("ol { list-style-type: thai }", 2, 1), "Thai digits");
        Check.equal("二三、", markers("ol { list-style-type: cjk-decimal }", 1, 23), "cjk-decimal is numeric with its own suffix");
        Check.equal("이십삼, ", markers("ol { list-style-type: korean-hangul-formal }", 1, 23), "Korean numerals");
        Check.equal("十二, ", markers("ol { list-style-type: korean-hanja-informal }", 1, 12), "Korean hanja without the leading one");
        Check.equal("あ、,い、", markers("ol { list-style-type: hiragana }", 2, 1), "hiragana");
    }

    /**
     * <code>@counter-style</code> with the systems cyclic, fixed, symbolic, alphabetic, numeric and additive.
     * <p>
     * <code>@counter-style</code> 의 cyclic, fixed, symbolic, alphabetic, numeric, additive 시스템.
     */
    public void testSystems() {
        Check.equal("* ,+ ,* ", custom("system: cyclic; symbols: '*' '+'; suffix: ' ';", 3, 1), "cyclic");
        Check.equal("a. ,b. ,c. ,5. ", custom("system: fixed 2; symbols: a b c;", 4, 2), "fixed from 2, and the fallback outside the symbols");
        Check.equal("1. ,2. ", custom("system: fixed 5; symbols: a;", 2, 1), "fixed: values below the first use the fallback");
        Check.equal("A. ,B. ,AA. ,BB. ", custom("system: symbolic; symbols: A B;", 4, 1), "symbolic repeats the symbols");
        Check.equal("x. ,y. ,xx. ,xy. ", custom("system: alphabetic; symbols: x y;", 4, 1), "alphabetic with two symbols");
        Check.equal("ba. ,bb. ,baa. ", custom("system: numeric; symbols: a b;", 3, 2), "numeric with two symbols is binary");
        Check.equal("X. ,XI. ", custom("system: additive; additive-symbols: 10 X, 5 V, 1 I;", 2, 10), "additive");
        Check.equal("V. ,VI. ", custom("system: additive; additive-symbols: 10 X, 5 V, 1 I;", 2, 5), "additive without the tens");
        Check.equal("0. ,I. ", custom("system: additive; additive-symbols: 5 V, 1 I;", 2, 0), "additive has no zero without a zero weight: fallback decimal");
    }

    /**
     * The descriptors: prefix, suffix, negative, range, pad and fallback, and <code>extends</code>.
     * <p>
     * 서술자: prefix, suffix, negative, range, pad, fallback, 그리고 <code>extends</code>.
     */
    public void testDescriptors() {
        String num = "system: numeric; symbols: " + DIGITS + ";";
        Check.equal("(1)", custom(num + " prefix: '('; suffix: ')';", 1, 1), "prefix and suffix");
        Check.equal("[05]", custom(num + " prefix: '['; suffix: ']'; pad: 2 '0';", 1, 5), "pad");
        Check.equal("neg 3()", custom(num + " negative: 'neg ' '()'; suffix: '';", 1, -3), "negative prefix and suffix");
        Check.equal("a. ,b. ", custom("system: cyclic; symbols: a b c; range: 1 2; fallback: lower-alpha;", 2, 1), "values inside the range");
        Check.equal("3. ", custom("system: cyclic; symbols: a b c; range: 1 2; fallback: decimal;", 1, 3), "outside the range: the fallback decimal");
        Check.equal("c. ", custom("system: cyclic; symbols: a b c; range: 1 2; fallback: lower-alpha;", 1, 3), "outside the range: the fallback lower-alpha");
        Check.equal("» i. ", custom("system: extends lower-roman; prefix: '» ';", 1, 1), "extends takes the system and changes a descriptor");
        Check.equal("III. ", custom("system: extends upper-roman;", 1, 3), "extends keeps the rest");
        Check.equal("1. ", custom("system: extends nonexistent;", 1, 1), "extending an unknown style is invalid: decimal");
        Check.equal("1. ", custom("system: cyclic;", 1, 1), "cyclic without symbols is invalid: decimal");
        Check.equal("1. ", custom("system: alphabetic; symbols: a;", 1, 1), "alphabetic needs two symbols");
        Check.equal("• ", markers("@counter-style disc { system: cyclic; symbols: z; } ol { list-style-type: disc }", 1, 1), "disc cannot be redefined");
        Check.equal("Z. ", markers("@counter-style a { system: cyclic; symbols: z; } @counter-style a { system: cyclic; symbols: Z; } ol { list-style-type: a }", 1, 1),
                "a later definition replaces an earlier one");
    }

    /**
     * <code>symbols()</code>, a string and <code>counter()</code> with counter styles in generated content.
     * <p>
     * <code>symbols()</code>, 문자열, 생성 콘텐츠의 카운터 스타일을 쓰는 <code>counter()</code>.
     */
    public void testSymbolsStringsAndCounters() {
        Check.equal("→ ,→ ", markers("ol { list-style-type: '→ ' }", 2, 1), "a string is the marker itself");
        Check.equal("* ,** ,*** ", markers("ol { list-style-type: symbols('*') }", 3, 1), "symbols() is symbolic by default");
        Check.equal("a ,b ", markers("ol { list-style-type: symbols(cyclic 'a' 'b') }", 2, 1), "symbols(cyclic ...)");
        LayoutResult r = LayoutTestSupport.layout("<style>@counter-style z { system: cyclic; symbols: 'Z'; } p { counter-reset: n 7 }"
                + " p::before { content: counter(n, z) '/' counter(n, upper-roman) '/' counter(n, symbols(numeric 'a' 'b')) }</style><p id=p>x</p>", 400);
        StringBuilder text = new StringBuilder();
        // 문단의 줄마다 텍스트를 모은다(::before 의 내용이 첫머리에 온다).
        for (LayoutBox line : LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"))) {
            text.append(LayoutTestSupport.lineText(line));
        }
        Check.isTrue(text.toString().startsWith("Z/VII/bbb"), "counter() with a defined style, a predefined style and symbols(), got " + text);
    }
}
