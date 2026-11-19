package org.duckdns.hjow.swingwebpanel.css;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Robustness tests with broken input (plan items P08-08, P22-09 in advance): thousands of random and mutated strings are given to every entry point of the
 * CSS engine; none of them may throw, hang or overflow the stack, and whatever is parsed must serialize and be usable again. The random generator has a
 * fixed seed, so a failure can be reproduced.
 * <p>
 * 깨진 입력에 대한 견고성 시험(계획 P08-08, 앞서 하는 P22-09): 무작위 문자열과 변형한 문자열 수천 개를 CSS 엔진의 모든 진입점에 주며, 예외를 던지거나 멈추거나 스택을
 * 넘치게 해서는 안 되고 파싱된 것은 직렬화하고 다시 쓸 수 있어야 한다. 난수 생성기의 시드가 고정이므로 실패를 재현할 수 있다.
 */
public class CssFuzzTest {

    /** Pieces from which random CSS is assembled. / 무작위 CSS 를 이어 붙이는 조각. */
    private static final String[] PIECES = {"a", "div", ".c", "#i", "[x]", "[x=\"y\" i]", ":hover", "::before", ":not(", ":is(a,", ":nth-child(2n+1 of ", ")", "(", "{", "}", ";",
        ":", ",", " ", "  ", "\n", "/*", "*/", "\"", "'", "\\", "url(", "url(a b)", "@media", "@import", "@supports", "@font-face", "@keyframes", "@namespace", "@charset", "!important",
        "!", "red", "#fff", "#12", "rgb(", "calc(", "1px", "50%", "-", "+", "*", "/", ">", "~", "|", "--x", "var(--x)", "var(", "color", "margin", "font", "background", "border", "0",
        "1e999", "-0", ".5", "U+0025-00FF", "<!--", "-->", "\u0000".substring(0, 0) + "x", "inherit", "auto", "none", "screen", "and", "(min-width: 1px)", "not", "only", "or",
        "min(", "clamp(1px, 2px, 3px)", "attr(x)", "from", "to", "100%"};

    /** The random generator with a fixed seed. / 시드가 고정된 난수 생성기. */
    private final Random rnd = new Random(20261116L);

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public CssFuzzTest() {
    }

    /**
     * Builds a random string from the pieces. 조각으로 무작위 문자열을 만든다.
     *
     * @param maxPieces maximum number of pieces / 조각의 최대 수
     * @return the string / 문자열
     */
    private String random(int maxPieces) {
        StringBuilder sb = new StringBuilder();
        int n = 1 + rnd.nextInt(maxPieces);
        // 조각을 무작위로 골라 이어 붙인다.
        for (int i = 0; i < n; i++) {
            sb.append(PIECES[rnd.nextInt(PIECES.length)]);
        }
        return sb.toString();
    }

    /**
     * Garbage in every parser entry point. 모든 파서 진입점에 쓰레기를 준다.
     */
    public void testRandomInputNeverThrows() {
        PropertyRegistry reg = PropertyRegistry.getInstance();
        List<String> names = reg.names();
        long t0 = System.currentTimeMillis();
        // 무작위 문자열을 모든 진입점에 넣어 예외가 없는지 본다.
        for (int i = 0; i < 4000; i++) {
            String s = random(14);
            String ctx = "input #" + i + ": " + s;
            try {
                StyleSheet sheet = StyleSheet.parse(s, "http://example.com/a.css", StyleSheet.ORIGIN_AUTHOR, null, new CssDiagnostics());
                sheet.toCssText();
                CssParser.parseDeclarationList(s, null);
                MediaQueryList.parse(s).toCssText();
                SupportsCondition.evaluate(s);
                CssColor.parse(s);
                CssStyleDeclaration d = CssStyleDeclaration.parse(s, null, null);
                d.getCssText();
                Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
                reg.parse(names.get(rnd.nextInt(names.size())), s, out);
                try {
                    SelectorParser.parse(s, null).toCssText();
                } catch (SelectorSyntaxException e) {
                    // 올바르지 않은 선택자는 정상적인 거부이다.
                    Check.notNull(e.getMessage(), "an error message");
                }
            } catch (RuntimeException e) {
                Check.fail(ctx + " threw " + e);
            } catch (StackOverflowError e) {
                Check.fail(ctx + " overflowed the stack");
            }
        }
        Check.isTrue(System.currentTimeMillis() - t0 < 120000, "4000 random inputs are processed in bounded time");
    }

    /**
     * Mutations of valid style sheets. 올바른 스타일 시트의 변형.
     */
    public void testMutatedSheetsStillWork() {
        String valid = "@import 'x.css'; @namespace svg url(http://www.w3.org/2000/svg); a, b > c:hover::before { color: red; margin: 1px 2px !important; background: url(a.png) no-repeat }"
                + " @media (min-width: 600px) { p { font: bold 12px/1.5 Arial, sans-serif } } @supports (display: grid) { .g { display: grid } }"
                + " @keyframes k { from { opacity: 0 } 50%, 75% { opacity: .5 } to { opacity: 1 } } @font-face { font-family: F; src: url(f.woff) format('woff') }"
                + " :root { --x: 1px } li:nth-child(2n+1 of .y):not(.z) { width: calc(100% - var(--x)) }";
        Document doc = MiniHtml.parse("<html><body><div id=\"d\" class=\"c\"><a>1</a><b>2</b><p class=\"g y\">3</p><ul><li class=\"y\">4</li></ul></div></body></html>");
        // 올바른 시트의 글자를 지우거나 바꾸거나 끼워 넣어 변형한 뒤 파싱하고 리졸버에 적용한다.
        for (int i = 0; i < 600; i++) {
            StringBuilder sb = new StringBuilder(valid);
            int edits = 1 + rnd.nextInt(6);
            // edits 의 항목을 하나씩 처리한다.
            for (int k = 0; k < edits; k++) {
                int pos = rnd.nextInt(sb.length());
                int kind = rnd.nextInt(3);
                if (kind == 0) {
                    sb.deleteCharAt(pos);
                } else if (kind == 1) {
                    sb.setCharAt(pos, "{}()[];:,\"'\\/*!@#.>+~ x1".charAt(rnd.nextInt(24)));
                } else {
                    sb.insert(pos, PIECES[rnd.nextInt(PIECES.length)]);
                }
            }
            String text = sb.toString();
            try {
                StyleSheet sheet = StyleSheet.parse(text, "http://example.com/a.css", StyleSheet.ORIGIN_AUTHOR, null, null);
                StyleResolver r = new StyleResolver(doc, new MediaEnvironment(800, 600));
                r.setStyleSheets(java.util.Arrays.asList(sheet));
                // 문서의 모든 요소의 스타일과 의사 요소 스타일을 계산해 본다.
                for (Node n = doc; n != null; n = Node.nextInTree(n, doc)) {
                    if (n instanceof Element) {
                        ComputedStyle s = r.getComputedStyle((Element) n);
                        Check.notNull(s.get("width"), "a computed style always has values");
                        r.getPseudoStyle((Element) n, "before");
                    }
                }
                r.dispose();
                StyleSheet again = StyleSheet.parse(sheet.toCssText(), "http://example.com/a.css", StyleSheet.ORIGIN_AUTHOR, null, null);
                Check.equal(sheet.countStyleRules(), again.countStyleRules(), "a parsed sheet serializes to text with the same number of style rules");
            } catch (RuntimeException e) {
                Check.fail("mutation #" + i + " threw " + e + " for: " + text);
            } catch (StackOverflowError e) {
                Check.fail("mutation #" + i + " overflowed the stack for: " + text);
            }
        }
    }

    /**
     * Repeats a string. 문자열을 반복한다.
     *
     * @param s the string / 문자열
     * @param n the count / 횟수
     * @return the repetition / 반복한 문자열
     */
    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        // 문자열을 n 번 이어 붙인다.
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Very deep nesting in values, functions, blocks and at-rules is cut off instead of overflowing the stack. 값, 함수, 블록, at-규칙의 아주 깊은 중첩은
     * 스택을 넘치게 하지 않고 잘린다.
     */
    public void testDeepNesting() {
        String[] hostile = {
            "a { width: calc(" + repeat("(", 5000) + "1px" + repeat(")", 5000) + ") }",
            "a { width: " + repeat("calc(", 5000) + "1px" + repeat(")", 5000) + " }",
            "a { color: " + repeat("var(--x, ", 5000) + "red" + repeat(")", 5000) + " }",
            "a { content: " + repeat("f(", 5000) + "1" + repeat(")", 5000) + " }",
            "a { width: " + repeat("[", 5000) + "1" + repeat("]", 5000) + " }",
            repeat("@media screen { ", 3000) + "a { color: red }" + repeat(" }", 3000),
            repeat("a:is(", 3000) + "b" + repeat(")", 3000) + " { color: red }",
            repeat("{", 5000),
            repeat("a { b: ", 3000),
            "--x: " + repeat("(", 5000),
            "a { transform: " + repeat("translate(", 5000) + "1px" + repeat(")", 5000) + " }",
            "@supports " + repeat("(not ", 3000) + "(color: red)" + repeat(")", 3000) + " { a { color: red } }",
            "@media " + repeat("(not ", 3000) + "(color)" + repeat(")", 3000) + " { a { color: red } }",
        };
        // 각 적대적 입력이 예외나 스택 넘침 없이 처리되는지 본다.
        for (int i = 0; i < hostile.length; i++) {
            try {
                StyleSheet s = StyleSheet.parse(hostile[i], "http://example.com/a.css", StyleSheet.ORIGIN_AUTHOR, null, new CssDiagnostics());
                s.toCssText();
                CssStyleDeclaration.parse(hostile[i], null, new CssDiagnostics()).getCssText();
                MediaQueryList.parse(hostile[i]).toCssText();
                SupportsCondition.evaluate(hostile[i]);
                Document doc = MiniHtml.parse("<html><body><p id=\"p\" style=\"" + hostile[i].replace("\"", "'") + "\">x</p></body></html>");
                StyleResolver r = new StyleResolver(doc, new MediaEnvironment());
                r.setStyleSheets(java.util.Arrays.asList(s));
                r.getComputedStyle(doc.getElementById("p")).getPropertyValue("width");
                r.dispose();
            } catch (RuntimeException e) {
                Check.fail("hostile input #" + i + " threw " + e);
            } catch (StackOverflowError e) {
                Check.fail("hostile input #" + i + " overflowed the stack");
            }
        }
        Check.equal(1, CssParser.parseComponentValues(repeat("(", 5000)).size(), "deep nesting is cut off, not rejected");
    }

    /**
     * Pathological sizes. 비정상적으로 큰 입력.
     */
    public void testPathologicalSizes() {
        StringBuilder sb = new StringBuilder();
        // 선언 2만 개가 든 규칙 하나를 만든다.
        for (int i = 0; i < 20000; i++) {
            sb.append("color: red; ");
        }
        long t0 = System.currentTimeMillis();
        StyleSheet s = StyleSheet.parse("a { " + sb + " }", null, StyleSheet.ORIGIN_AUTHOR, null, null);
        Check.equal(1, s.getRules().size(), "one rule with many declarations");
        StringBuilder rules = new StringBuilder();
        // 규칙 2만 개가 든 시트를 만든다.
        for (int i = 0; i < 20000; i++) {
            rules.append(".c").append(i % 500).append(" > a:hover { color: red } ");
        }
        StyleSheet big = StyleSheet.parse(rules.toString(), null, StyleSheet.ORIGIN_AUTHOR, null, null);
        Check.equal(20000, big.countStyleRules(), "twenty thousand rules");
        StringBuilder deepSel = new StringBuilder();
        // 선택자를 5000겹으로 중첩한다.
        for (int i = 0; i < 5000; i++) {
            deepSel.append(":is(");
        }
        deepSel.append("a");
        // 항목 의 항목을 하나씩 처리한다.
        for (int i = 0; i < 5000; i++) {
            deepSel.append(')');
        }
        try {
            SelectorParser.parse(deepSel.toString(), null);
        } catch (SelectorSyntaxException e) {
            Check.notNull(e.getMessage(), "rejecting is fine");
        } catch (StackOverflowError e) {
            Check.fail("deeply nested selectors overflow the stack");
        }
        StringBuilder var = new StringBuilder("--a0: 1;");
        // 서로 참조하는 사용자 정의 속성 2000개를 사슬로 만든다.
        for (int i = 1; i < 2000; i++) {
            var.append("--a").append(i).append(": var(--a").append(i - 1).append(") var(--a").append(i - 1).append(");");
        }
        Document doc = MiniHtml.parse("<html><body><p id=\"p\" style=\"" + var + " width: var(--a1999, 5px)\">x</p></body></html>");
        StyleResolver r = new StyleResolver(doc, new MediaEnvironment());
        ComputedStyle cs = r.getComputedStyle(doc.getElementById("p"));
        Check.near(5, cs.px(org.duckdns.hjow.swingwebpanel.css.props.Prop.WIDTH), 1e-9, "an exponential var() expansion is cut off and the fallback is used");
        r.dispose();
        Check.isTrue(System.currentTimeMillis() - t0 < 120000, "pathological inputs are handled in bounded time");
    }
}
