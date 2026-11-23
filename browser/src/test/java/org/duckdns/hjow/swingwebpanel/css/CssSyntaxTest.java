package org.duckdns.hjow.swingwebpanel.css;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssTokenizer;
import org.duckdns.hjow.swingwebpanel.css.syntax.DeclarationBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxDeclaration;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxRule;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the CSS tokenizer and parser (plan item P08-01): every token kind, escapes, numbers, urls, error recovery and rule structure.
 * <p>
 * CSS 토큰화기와 파서 시험(계획 P08-01): 모든 토큰 종류, 이스케이프, 숫자, url, 오류 복구, 규칙 구조.
 */
public class CssSyntaxTest {

    /** The replacement character as a string. 대체 문자를 문자열로 만든 것. */
    private static final String RC = String.valueOf((char) 0xFFFD);

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public CssSyntaxTest() {
    }

    /**
     * Describes the tokens of a text in a compact notation, keeping whitespace tokens. 공백 토큰을 포함해 텍스트의 토큰을 간단한 표기로 설명한다.
     *
     * @param css CSS text / CSS 문자열
     * @return description / 설명
     */
    private static String tokens(String css) {
        StringBuilder sb = new StringBuilder();
        List<CssToken> list = CssTokenizer.tokenize(css);
        // 토큰마다 종류와 값을 이어 붙인다.
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(name(list.get(i)));
        }
        return sb.toString();
    }

    /**
     * Describes the tokens of a text without whitespace tokens. 공백 토큰을 빼고 텍스트의 토큰을 설명한다.
     *
     * @param css CSS text / CSS 문자열
     * @return description / 설명
     */
    private static String tok(String css) {
        StringBuilder sb = new StringBuilder();
        List<CssToken> list = CssTokenizer.tokenize(css);
        // 공백이 아닌 토큰만 이어 붙인다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).is(CssToken.WHITESPACE)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(name(list.get(i)));
        }
        return sb.toString();
    }

    /**
     * Names one token. 토큰 하나의 이름을 만든다.
     *
     * @param t token / 토큰
     * @return name with value / 값이 붙은 이름
     */
    private static String name(CssToken t) {
        switch (t.getType()) {
        case CssToken.IDENT:
            return "ident(" + t.getValue() + ")";
        case CssToken.FUNCTION:
            return "func(" + t.getValue() + ")";
        case CssToken.AT_KEYWORD:
            return "at(" + t.getValue() + ")";
        case CssToken.HASH:
            return (t.isHashId() ? "id(" : "hash(") + t.getValue() + ")";
        case CssToken.STRING:
            return "str(" + t.getValue() + ")";
        case CssToken.BAD_STRING:
            return "badstr(" + t.getValue() + ")";
        case CssToken.URL:
            return "url(" + t.getValue() + ")";
        case CssToken.BAD_URL:
            return "badurl";
        case CssToken.DELIM:
            return "delim(" + t.getValue() + ")";
        case CssToken.NUMBER:
            return (t.isInteger() ? "int(" : "num(") + t.getNumber() + ")";
        case CssToken.PERCENTAGE:
            return "pct(" + t.getNumber() + ")";
        case CssToken.DIMENSION:
            return "dim(" + t.getNumber() + t.getUnit() + ")";
        case CssToken.WHITESPACE:
            return "ws";
        case CssToken.CDO:
            return "cdo";
        case CssToken.CDC:
            return "cdc";
        case CssToken.COLON:
            return ":";
        case CssToken.SEMICOLON:
            return ";";
        case CssToken.COMMA:
            return ",";
        case CssToken.LEFT_BRACKET:
            return "[";
        case CssToken.RIGHT_BRACKET:
            return "]";
        case CssToken.LEFT_PAREN:
            return "(";
        case CssToken.RIGHT_PAREN:
            return ")";
        case CssToken.LEFT_BRACE:
            return "{";
        case CssToken.RIGHT_BRACE:
            return "}";
        case CssToken.UNICODE_RANGE:
            return "range(" + t.getValue() + ")";
        default:
            return "?";
        }
    }

    /**
     * Basic token kinds. 기본 토큰 종류.
     */
    public void testBasicTokens() {
        Check.equal("ident(a) ws ident(b-c) ws ident(_d1)", tokens("a b-c _d1"), "identifiers");
        Check.equal("func(rgb) int(1.0) , int(2.0) ) ;", tok("rgb(1,2);"), "function");
        Check.equal("at(media) ident(screen) { }", tok("@media screen { }"), "at-keyword");
        Check.equal("id(abc) hash(1a) delim(#)", tok("#abc #1a #"), "hash tokens and the id flag");
        Check.equal("[ ] ( ) { } : ; ,", tok("[]() {}:;,"), "punctuation");
        Check.equal("cdo cdc", tok("<!-- -->"), "CDO and CDC");
        Check.equal("delim(+) delim(-) delim(.) delim(<) delim(@) delim(!)", tok("+ - . < @ !"), "delimiters");
        Check.equal("ws", tokens(" \t\n  "), "whitespace runs collapse into one token");
    }

    /**
     * Comments are skipped, an unterminated comment runs to the end. 주석은 건너뛰며 끝나지 않은 주석은 끝까지 간다.
     */
    public void testComments() {
        Check.equal("ident(a) ident(b)", tokens("a/* x */b"), "comment between tokens");
        Check.equal("ident(a) ws", tokens("a /* open"), "unterminated comment");
        Check.equal("", tokens("/**/"), "empty comment");
        Check.equal("ident(a) delim(/) ident(b)", tokens("a/b"), "a slash is a delimiter");
    }

    /**
     * Numbers, percentages and dimensions. 숫자, 백분율, 차원.
     */
    public void testNumbers() {
        Check.equal("int(12.0) num(1.5) num(0.5) int(-3.0) int(7.0)", tok("12 1.5 .5 -3 +7"), "plain numbers");
        Check.equal("num(5.0) num(1000.0) num(150.0)", tok("+.5e1 1e3 1.5E+2"), "exponents");
        Check.near(0.001, CssTokenizer.tokenize("+.1e-2").get(0).getNumber(), 1e-12, "a negative exponent");
        Check.equal("dim(10.0px) pct(50.0) dim(1.0e) dim(1.0x)", tok("10px 50% 1e 1x"), "units");
        Check.equal("int(1.0) delim(.) ident(e3)", tokens("1.e3"), "a dot without digits ends the number");
        Check.equal("dim(1.0e-)", tokens("1e-"), "e followed by a hyphen is a unit");
        Check.equal("num(-0.0)", tokens("-0.0"), "negative zero is preserved");
        Check.equal("dim(5.0EM)", tokens("5EM"), "the unit keeps its original case");
    }

    /**
     * Escapes in identifiers and strings. 식별자와 문자열의 이스케이프.
     */
    public void testEscapes() {
        Check.equal("ident(AB)", tokens("\\41 B"), "a hex escape consumes one whitespace");
        Check.equal("ident(" + RC + ")", tokens("\\0 "), "a NUL escape is the replacement character");
        Check.equal("ident(" + RC + ")", tokens("\\110000 "), "an out of range escape");
        Check.equal("ident(a.b)", tokens("a\\.b"), "an escaped delimiter");
        Check.equal("str(ab)", tokens("\"a\\\nb\""), "an escaped newline continues a string");
        Check.equal("str(a\"b)", tokens("'a\\\"b'"), "an escaped quote");
        Check.equal("str(" + new String(Character.toChars(0x1F600)) + ")", tokens("\"\\1F600\""), "a supplementary code point escape");
        Check.equal("delim(\\) ws", tokens("\\\n"), "a backslash before a newline is a delimiter");
    }

    /**
     * Strings, bad strings and the input preprocessing. 문자열, 잘못된 문자열, 입력 전처리.
     */
    public void testStrings() {
        Check.equal("str(abc)", tokens("\"abc\""), "double quoted");
        Check.equal("str(a b)", tokens("'a b'"), "single quoted");
        Check.equal("str(abc)", tokens("\"abc"), "unterminated at the end of input");
        Check.equal("badstr(abc) ws ident(d)", tokens("\"abc\nd"), "a newline ends a bad string");
        Check.equal("ident(a) ws ident(b) ws ident(c)", tokens("a\r\nb\fc"), "CRLF and FF become whitespace");
        Check.equal("str(" + RC + ")", tokens("\"" + (char) 0 + "\""), "NUL becomes the replacement character");
    }

    /**
     * The url token and bad urls. url 토큰과 잘못된 url.
     */
    public void testUrls() {
        Check.equal("url(a.png)", tokens("url(a.png)"), "plain url");
        Check.equal("url(a.png)", tokens("URL(  a.png  )"), "whitespace and case");
        Check.equal("func(url) str(a b) )", tok("url(\"a b\")"), "a quoted url is a function");
        Check.equal("func(url) ws str(a) ws )", tokens("url( 'a' )"), "a quoted url after whitespace");
        Check.equal("badurl ident(x)", tok("url(a b) x"), "whitespace inside a url");
        Check.equal("badurl", tokens("url(a\"b)"), "a quote inside a url");
        Check.equal("badurl", tokens("url(a(b)"), "a paren inside a url");
        Check.equal("url(a)", tokens("url(a"), "a url at the end of input");
        Check.equal("url(a))", tokens("url(a\\))"), "an escaped paren belongs to the url");
        Check.equal("url()", tokens("url()"), "an empty url");
        Check.equal("func(urlx)", tokens("urlx("), "other function names");
    }

    /**
     * Identifier edge cases with hyphens and custom properties. 하이픈과 사용자 정의 속성 식별자 경계.
     */
    public void testHyphenIdentifiers() {
        Check.equal("ident(--foo) ident(-a) delim(-) int(1.0)", tok("--foo -a - 1"), "hyphen identifiers");
        Check.equal("int(-1.0)", tokens("-1"), "a negative number");
        Check.equal("cdc", tokens("-->"), "CDC");
        Check.equal("ident(a--) delim(>)", tokens("a-->"), "CDC inside an identifier is part of it");
    }

    /**
     * The older unicode-range token. 이전의 unicode-range 토큰.
     */
    public void testUnicodeRange() {
        Check.equal("range(U+0025-00FF)", tokens("U+0025-00ff"), "a range");
        Check.equal("range(U+4??)", tokens("u+4??"), "a wildcard range");
        Check.equal("range(U+26)", tokens("U+26"), "a single code point");
        Check.equal("ident(u) delim(+) ident(g)", tokens("u+g"), "not a range");
    }

    /**
     * A stylesheet with ordinary rules. 평범한 규칙들의 스타일 시트.
     */
    public void testStylesheetRules() {
        List<SyntaxRule> rules = CssParser.parseStylesheet("a { color: red } b, c > d { margin: 0 } @import 'x.css'; @media screen { e { f: g } }", null);
        Check.equal(4, rules.size(), "rule count");
        Check.isFalse(rules.get(0).isAtRule(), "the first is a qualified rule");
        Check.equal("a", rules.get(0).getPreludeText(), "first prelude");
        Check.equal("b, c > d", rules.get(1).getPreludeText(), "second prelude");
        Check.isTrue(rules.get(2).isAtRule(), "import is an at-rule");
        Check.equal("import", rules.get(2).getName(), "name");
        Check.isNull(rules.get(2).getBlock(), "import has no block");
        Check.equal("media", rules.get(3).getName(), "media name");
        Check.equal("screen", rules.get(3).getPreludeText(), "media prelude");
        List<SyntaxRule> inner = CssParser.parseRuleList(rules.get(3).getBlock(), false, null);
        Check.equal(1, inner.size(), "nested rules");
        Check.equal("e", inner.get(0).getPreludeText(), "nested prelude");
    }

    /**
     * Error recovery at rule level. 규칙 수준 오류 복구.
     */
    public void testRuleRecovery() {
        // 끝나지 않은 블록은 입력 끝에서 닫힌다.
        List<SyntaxRule> r1 = CssParser.parseStylesheet("a { color: red; b { c: d", null);
        Check.equal(1, r1.size(), "an unclosed block at the end of input is closed");
        // 블록 없는 규칙은 버린다.
        CssDiagnostics d = new CssDiagnostics();
        List<SyntaxRule> r2 = CssParser.parseStylesheet("a { x: y } orphan", d);
        Check.equal(1, r2.size(), "a rule without a block is dropped");
        Check.equal(1, d.count(CssDiagnostics.INVALID), "and reported");
        // CDO/CDC 는 최상위에서 무시한다.
        List<SyntaxRule> r3 = CssParser.parseStylesheet("<!-- a { x: y } --> b { x: y }", null);
        Check.equal(2, r3.size(), "CDO and CDC are skipped");
        Check.equal("b", r3.get(1).getPreludeText(), "second rule");
        // 알 수 없는 at-규칙은 파서가 그대로 넘기고 이웃 규칙은 영향받지 않는다.
        List<SyntaxRule> r4 = CssParser.parseStylesheet("@unknown foo; a { x: y } @other { z } b { x: y }", null);
        Check.equal(4, r4.size(), "unknown at-rules do not disturb their neighbours");
        // 전문에 짝 없는 닫는 괄호가 있어도 다음 규칙은 살아남는다.
        List<SyntaxRule> r5 = CssParser.parseStylesheet("a ) { x: y } b { x: y }", null);
        Check.equal(2, r5.size(), "a stray paren stays in the prelude");
        Check.equal("a )", r5.get(0).getPreludeText(), "prelude with a stray token");
        List<SyntaxRule> r6 = CssParser.parseStylesheet("@media screen { a { x: y }", null);
        Check.equal(1, r6.size(), "an unterminated at-rule block");
        Check.equal(1, CssParser.parseRuleList(r6.get(0).getBlock(), false, null).size(), "its rule survives");
    }

    /**
     * Declarations: names, values and the important flag. 선언: 이름, 값, important 플래그.
     */
    public void testDeclarations() {
        List<SyntaxDeclaration> list = CssParser.parseDeclarationList("Color : Red ; margin:0 auto!important;padding: 1px ! IMPORTANT ; --My-Var:  a  b ; width:", null);
        Check.equal(5, list.size(), "declaration count");
        Check.equal("color", list.get(0).getName(), "property names are lower-cased");
        Check.equal("Red", list.get(0).getValueText(), "the value keeps its case");
        Check.isFalse(list.get(0).isImportant(), "normal");
        Check.isTrue(list.get(1).isImportant(), "!important without a space");
        Check.equal("0 auto", list.get(1).getValueText(), "value without important");
        Check.isTrue(list.get(2).isImportant(), "! important with spaces and case");
        Check.equal("1px", list.get(2).getValueText(), "value");
        Check.equal("--My-Var", list.get(3).getName(), "a custom property keeps its case");
        Check.isTrue(list.get(3).isCustomProperty(), "custom flag");
        Check.equal("a b", list.get(3).getValueText(), "custom value (whitespace is collapsed by the tokenizer)");
        Check.equal(0, list.get(4).getValue().size(), "an empty value is kept for the property layer to judge");
    }

    /**
     * Declaration recovery: bad declarations are skipped alone. 선언 복구: 잘못된 선언만 건너뛴다.
     */
    public void testDeclarationRecovery() {
        CssDiagnostics d = new CssDiagnostics();
        List<SyntaxDeclaration> list = CssParser.parseDeclarationList("a: b; ; : c; d e; 5: f; *zoom: 1; g: h; i", d);
        Check.equal(2, list.size(), "valid declarations only");
        Check.equal("a", list.get(0).getName(), "first");
        Check.equal("g", list.get(1).getName(), "the last survives the garbage before it");
        Check.equal(5, d.count(CssDiagnostics.INVALID), "five bad declarations are reported");
    }

    /**
     * Semicolons inside blocks, functions and strings do not split declarations. 블록, 함수, 문자열 안의 세미콜론은 선언을 나누지 않는다.
     */
    public void testNestedSemicolons() {
        List<SyntaxDeclaration> list = CssParser.parseDeclarationList("background: url(a;b); content: \";\"; --x: { a: b; c: d }; y: z(1; 2)", null);
        Check.equal(4, list.size(), "four declarations");
        Check.equal("url(\"a;b\")", list.get(0).getValueText(), "url with a semicolon");
        Check.equal("\";\"", list.get(1).getValueText(), "string with a semicolon");
        Check.equal("{ a: b; c: d }", list.get(2).getValueText(), "block value");
        Check.equal("z(1; 2)", list.get(3).getValueText(), "function value");
    }

    /**
     * At-rules inside a declaration block. 선언 블록 안의 at-규칙.
     */
    public void testBlockWithAtRules() {
        DeclarationBlock b = CssParser.parseDeclarationBlock(CssParser.parseComponentValues("size: A4; @top-left { content: 'x' } margin: 1cm"), null);
        Check.equal(2, b.getDeclarations().size(), "declarations");
        Check.equal(1, b.getRules().size(), "at-rules");
        Check.equal("top-left", b.getRules().get(0).getName(), "at-rule name");
    }

    /**
     * Single declaration parsing as used by supports and the declaration object. supports 와 선언 객체에서 쓰는 단일 선언 파싱.
     */
    public void testParseDeclaration() {
        SyntaxDeclaration d = CssParser.parseDeclaration(" Display : none !important ");
        Check.notNull(d, "valid");
        Check.equal("display", d.getName(), "name");
        Check.isTrue(d.isImportant(), "important");
        Check.isNull(CssParser.parseDeclaration("a: b; c: d"), "two declarations are rejected");
        Check.isNull(CssParser.parseDeclaration("a b"), "a missing colon");
        Check.isNull(CssParser.parseDeclaration(""), "empty text");
    }

    /**
     * Component values: nesting, comma splitting and serialization. 컴포넌트 값: 중첩, 쉼표 분리, 직렬화.
     */
    public void testComponentValues() {
        List<ComponentValue> list = CssParser.parseComponentValues("a(b, [c d], {e}) f");
        Check.equal(3, list.size(), "function, whitespace, identifier");
        Check.isTrue(list.get(0) instanceof CssFunction, "function");
        CssFunction f = (CssFunction) list.get(0);
        Check.equal("a", f.getName(), "function name");
        List<List<ComponentValue>> parts = CssParser.splitCommas(f.getArguments());
        Check.equal(3, parts.size(), "comma parts");
        Check.isTrue(parts.get(1).get(0) instanceof CssBlock, "bracket block");
        Check.equal("[c d]", parts.get(1).get(0).toCssText(), "block text");
        Check.equal(1, CssParser.splitCommas(CssParser.parseComponentValues("")).size(), "empty input gives one empty part");
        Check.equal("a(b, [c d], {e}) f", CssBlock.join(list), "serialization round trip");
    }

    /**
     * Mismatched closers are plain tokens and deep nesting does not overflow the stack. 짝이 맞지 않는 닫는 기호와 깊은 중첩.
     */
    public void testNestingRobustness() {
        List<ComponentValue> list = CssParser.parseComponentValues("[a ) b]");
        Check.equal(1, list.size(), "one block");
        Check.equal(5, ((CssBlock) list.get(0)).getContents().size(), "the paren is just a token inside brackets");
        StringBuilder sb = new StringBuilder();
        // 20만 겹의 괄호를 열어 스택 오버플로가 없는지 본다.
        for (int i = 0; i < 200000; i++) {
            sb.append('(');
        }
        List<ComponentValue> deep = CssParser.parseComponentValues(sb.toString());
        Check.equal(1, deep.size(), "deeply nested input is parsed iteratively");
        CssDiagnostics d = new CssDiagnostics();
        Check.equal(0, CssParser.parseStylesheet(sb.toString(), d).size(), "and dropped as a rule without a block");
        Check.equal(1, d.count(CssDiagnostics.INVALID), "with a diagnostic");
    }
}
