package org.duckdns.hjow.swingwebpanel.css;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.GrammarRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;
import org.duckdns.hjow.swingwebpanel.css.values.ValueGrammar;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the value definition grammar (plan item P08-05): keywords, types and ranges, every combinator and multiplier, functions, named
 * definitions, backtracking, prefix matching and url resolution.
 * <p>
 * 값 정의 문법 시험(계획 P08-05): 키워드, 형식과 범위, 모든 결합자와 반복 표기, 함수, 이름 있는 정의, 역추적, 접두사 일치, url 해석.
 */
public class GrammarTest {

    /** Named definitions used by the cases. / 사례가 쓰는 이름 있는 정의. */
    private final Map<String, ValueGrammar> defs = new HashMap<String, ValueGrammar>();

    /** The environment with the named definitions. / 이름 있는 정의가 든 환경. */
    private final ParseEnv env = new ParseEnv(new GrammarRegistry() {
        public ValueGrammar get(String name) {
            return defs.get(name);
        }
    }, "http://example.com/css/site.css");

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public GrammarTest() {
        defs.put("line-width", ValueGrammar.compile("thin | medium | thick | <length [0,inf]>"));
    }

    /**
     * Matches a value against a definition and returns the serialized result or the word NULL. 값을 정의에 맞춰 직렬화한 결과 또는 NULL 을 반환한다.
     *
     * @param grammar definition / 정의
     * @param value   value text / 값 문자열
     * @return serialized match or NULL / 직렬화한 일치 결과 또는 NULL
     */
    private String m(String grammar, String value) {
        CssValue v = ValueGrammar.compile(grammar).match(CssParser.parseComponentValues(value), env);
        return v == null ? "NULL" : v.toCssText();
    }

    /**
     * Keywords and alternatives. 키워드와 대안.
     */
    public void testKeywordsAndAlternatives() {
        Check.equal("auto", m("auto | none", "AUTO"), "keyword, case-insensitive");
        Check.equal("none", m("auto | none", "none"), "second alternative");
        Check.equal("NULL", m("auto | none", "other"), "no alternative matches");
        Check.equal("NULL", m("auto | none", ""), "empty value");
        Check.equal("NULL", m("auto | none", "auto none"), "trailing component");
        Check.equal("10px", m("auto | <length> | <percentage>", "10px"), "length alternative");
        Check.equal("50%", m("auto | <length> | <percentage>", "50%"), "percentage alternative");
    }

    /**
     * Types and ranges. 형식과 범위.
     */
    public void testTypesAndRanges() {
        Check.equal("0px", m("<length>", "0"), "unitless zero is a length");
        Check.equal("NULL", m("<length>", "5"), "a plain number is not a length");
        Check.equal("NULL", m("<length [0,inf]>", "-1px"), "negative length out of range");
        Check.equal("1px", m("<length [0,inf]>", "1px"), "length in range");
        Check.equal("0.5", m("<number [0,1]>", "0.5"), "number in range");
        Check.equal("NULL", m("<number [0,1]>", "2"), "number above range");
        Check.equal("3", m("<integer [1,inf]>", "3"), "integer in range");
        Check.equal("NULL", m("<integer>", "1.5"), "a real number is not an integer");
        Check.equal("45deg", m("<angle>", "45deg"), "angle");
        Check.equal("NULL", m("<angle>", "0"), "unitless zero is not an angle");
        Check.equal("200ms", m("<time>", "200ms"), "time");
        Check.equal("NULL", m("<time>", "5px"), "wrong dimension");
        Check.equal("red", m("<color>", "red"), "color");
        Check.equal("NULL", m("<color>", "reddish"), "not a color");
        Check.equal("\"a b\"", m("<string>", "'a b'"), "string");
        Check.equal("Foo", m("<custom-ident>", "Foo"), "custom identifier keeps its case");
        Check.equal("NULL", m("<custom-ident>", "inherit"), "a custom identifier is not a CSS-wide keyword");
        Check.equal("1fr", m("<flex>", "1fr"), "flex");
        Check.equal("--x", m("<dashed-ident>", "--x"), "dashed ident");
    }

    /**
     * Juxtaposition, optional parts and counted repetition. 나열, 선택 부분, 횟수 반복.
     */
    public void testSequences() {
        Check.equal("1px", m("<length> <length>?", "1px"), "optional absent");
        Check.equal("1px 2px", m("<length> <length>?", "1px 2px"), "optional present");
        Check.equal("NULL", m("<length> <length>?", "1px 2px 3px"), "too many");
        Check.equal("1px 2px", m("<length>? <length> <length>", "1px 2px"), "backtracking over the optional part");
        Check.equal("auto", m("[<length> | auto]? auto", "auto"), "backtracking over a group");
        Check.equal("1px 2px 3px", m("[<length> | auto]{1,4}", "1px 2px 3px"), "counted repetition");
        Check.equal("NULL", m("[<length> | auto]{1,4}", "1px 2px 3px 4px 5px"), "more than the maximum");
        Check.equal("NULL", m("<length>{2}", "1px"), "fewer than the minimum");
        Check.equal("1px 2px", m("<length>{2}", "1px 2px"), "exact count");
        Check.equal("1px 2px 3px", m("<length>+", "1px 2px 3px"), "one or more");
        Check.equal("NULL", m("<length>+", ""), "one or more needs one");
        Check.equal("1px", m("<length>*", "1px"), "zero or more");
    }

    /**
     * Any-order combinators. 순서 무관 결합자.
     */
    public void testAnyOrder() {
        Check.equal("thin red 2px", m("<line-width> || <color> || <length>", "thin red 2px"), "double bar, source order is kept");
        Check.equal("red thin", m("<line-width> || <color>", "red thin"), "double bar, other order");
        Check.equal("red", m("<line-width> || <color>", "red"), "double bar, one part");
        Check.equal("NULL", m("<line-width> || <color>", ""), "double bar needs one part");
        Check.equal("red blue", m("<color> || <color>", "red blue"), "each part is used at most once");
        Check.equal("NULL", m("<color> || <color>", "red blue green"), "a third color has no part left");
        Check.equal("1 \"a\"", m("<number> && <string>", "1 'a'"), "double ampersand");
        Check.equal("\"a\" 1", m("<number> && <string>", "'a' 1"), "double ampersand, other order");
        Check.equal("NULL", m("<number> && <string>", "1"), "double ampersand needs all parts");
        Check.equal("NULL", m("<number> && <string>", "1 2"), "wrong part");
        Check.equal("red inset 1px 2px", m("<color>? && [<length>{2}] && inset?", "red inset 1px 2px"), "shadow-like definition");
        Check.equal("1px 2px", m("<color>? && [<length>{2}] && inset?", "1px 2px"), "optional parts may be absent");
    }

    /**
     * Comma separated repetition and literals. 쉼표 구분 반복과 리터럴.
     */
    public void testCommasAndSlashes() {
        Check.equal("1px, 2px", m("<length>#", "1px, 2px"), "comma list");
        Check.equal("1px", m("<length>#", "1px"), "comma list with one item");
        Check.equal("NULL", m("<length>#", "1px 2px"), "items must be comma separated");
        Check.equal("NULL", m("<length>#", "1px,"), "trailing comma");
        Check.equal("NULL", m("<length>#", ",1px"), "leading comma");
        Check.equal("1px, 2px", m("<length>#{1,2}", "1px, 2px"), "comma list with a count");
        Check.equal("NULL", m("<length>#{1,2}", "1px, 2px, 3px"), "comma list above the count");
        Check.equal("16 / 9", m("<number> / <number>", "16/9"), "slash literal");
        Check.equal("16", m("<number> [/ <number>]?", "16"), "optional slash part");
        Check.equal("NULL", m("<number> / <number>", "16 9"), "slash is required");
        Check.equal("a, b", m("a , b", "a, b"), "comma literal");
        Check.equal("true", String.valueOf(ValueGrammar.compile("<length>#").match(CssParser.parseComponentValues("1px, 2px"), env) instanceof CssList
                && ((CssList) ValueGrammar.compile("<length>#").match(CssParser.parseComponentValues("1px, 2px"), env)).isComma()), "comma lists are flagged");
    }

    /**
     * Non-empty groups. 비어 있지 않은 그룹.
     */
    public void testNonEmpty() {
        Check.equal("NULL", m("[<length>? <color>?]!", ""), "empty value is rejected by !");
        Check.equal("1px", m("[<length>? <color>?]!", "1px"), "one part satisfies !");
        Check.equal("red", m("[<length>? <color>?]!", "red"), "other part satisfies !");
    }

    /**
     * Function notations. 함수 표기.
     */
    public void testFunctions() {
        String g = "translate(<length-percentage> [, <length-percentage>]?)";
        Check.equal("translate(10px)", m(g, "translate(10px)"), "one argument");
        Check.equal("translate(10px, 5%)", m(g, "translate(10px, 5%)"), "two arguments");
        Check.equal("NULL", m(g, "translate(10px 5px)"), "missing comma");
        Check.equal("NULL", m(g, "translate()"), "no argument");
        Check.equal("NULL", m(g, "scale(10px)"), "other function");
        Check.equal("translate(10px, 5px)", m(g, "TRANSLATE(10px,5px)"), "function names are case-insensitive");
        CssFunctionValue f = (CssFunctionValue) ValueGrammar.compile(g).match(CssParser.parseComponentValues("translate(1px, 2px)"), env);
        Check.equal(2, f.getArguments().size(), "arguments are split at the commas");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", m("matrix(<number>#{6})", "matrix(1,0,0,1,0,0)"), "comma list inside a function");
        Check.equal("cubic-bezier(0.1, 0.2, 0.3, 1)", m("cubic-bezier(<number [0,1]>, <number>, <number [0,1]>, <number>)", "cubic-bezier(.1,.2,.3,1)"), "typed arguments");
        Check.equal("rotate(45deg)", m("rotate(<angle> | <zero>)", "rotate(45deg)"), "alternatives inside a function");
        Check.equal("rotate(0)", m("rotate(<angle> | <zero>)", "rotate(0)"), "zero inside a function");
    }

    /**
     * Named definitions and errors in definitions. 이름 있는 정의와 정의의 오류.
     */
    public void testNamedDefinitions() {
        Check.equal("thin", m("<line-width>", "thin"), "keyword of a named definition");
        Check.equal("2px solid red", m("<line-width> || solid || <color>", "2px solid red"), "named definition in a combination");
        Check.equal("NULL", m("<line-width>", "-2px"), "named definition with a range");
        Check.throwsException(IllegalStateException.class, new Block() {
            public void run() {
                m("<nothing-defined>", "x");
            }
        }, "undefined definitions are reported");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                ValueGrammar.compile("<length");
            }
        }, "malformed definition: unterminated type");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                ValueGrammar.compile("a |");
            }
        }, "malformed definition: empty alternative");
    }

    /**
     * Prefix matching for any-order shorthands. 순서 무관 단축 속성을 위한 접두사 일치.
     */
    public void testPrefixMatching() {
        ValueGrammar g = ValueGrammar.compile("<length> || <color>");
        List<ComponentValue> items = ValueGrammar.prepare(CssParser.parseComponentValues("red 2px foo"));
        ValueGrammar.Prefix p = g.matchPrefix(items, 0, env);
        Check.notNull(p, "a prefix matches");
        Check.equal(2, p.end, "the longest prefix");
        Check.equal("red 2px", p.value.toCssText(), "captured value");
        ValueGrammar.Prefix q = g.matchPrefix(items, 2, env);
        Check.isNull(q, "an any-order group needs one part, so the unmatched rest has no prefix");
        ValueGrammar.Prefix r = ValueGrammar.compile("<color>?").matchPrefix(items, 2, env);
        Check.equal(2, r.end, "an optional part matches the empty prefix");
    }

    /**
     * The url type and image functions. url 형식과 이미지 함수.
     */
    public void testUrlsAndImages() {
        CssValue u = ValueGrammar.compile("<url>").match(CssParser.parseComponentValues("url(img/a.png)"), env);
        Check.isTrue(u instanceof CssUrl, "url token");
        Check.equal("http://example.com/css/img/a.png", ((CssUrl) u).getResolvedUrl(), "resolved against the style sheet URL");
        Check.equal("img/a.png", ((CssUrl) u).getUrl(), "the written URL is kept");
        Check.equal("url(\"a b.png\")", m("<url>", "url(\"a b.png\")"), "quoted url");
        Check.equal("NULL", m("<url>", "url(a b)"), "bad url token");
        Check.equal("none", m("none | <image>", "none"), "none alternative");
        Check.equal("url(\"a.png\")", m("none | <image>", "url(a.png)"), "image as url");
        Check.equal("linear-gradient(red, blue)", m("none | <image>", "linear-gradient(red, blue)"), "gradient is a function value");
        Check.equal("NULL", m("<image>", "foo(red)"), "unknown function is not an image");
        Check.equal("calc(1px + 2px)", m("<length>", "calc(1px + 2px)"), "calc as a length");
        Check.equal("NULL", m("<length>", "calc(1px + 2)"), "invalid calc is rejected");
        Check.equal("calc(1 + 2)", m("<number>", "calc(1 + 2)"), "calc as a number");
        Check.equal("NULL", m("<number>", "calc(1px)"), "a length calc is not a number");
        Check.equal("calc(100% - 1px)", m("<length-percentage>", "calc(100% - 1px)"), "length-percentage calc");
        Check.equal("NULL", m("<length>", "calc(100% - 1px)"), "a length does not take a percentage calc");
    }
}
