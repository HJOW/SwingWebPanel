package org.duckdns.hjow.swingwebpanel.js.parser;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the lexer (plan item P12-05): identifiers (Unicode and escapes), numbers, strings, templates, regular expression
 * literals, comments, line terminators, punctuators and error positions.
 * Java string literals use a double backslash before "u" when the JavaScript text must contain a Unicode escape.
 * <p>
 * 렉서 시험(계획 P12-05): 식별자(유니코드와 이스케이프), 숫자, 문자열, 템플릿, 정규식 리터럴, 주석, 줄 끝, 구두점, 오류 위치.
 * JavaScript 텍스트에 유니코드 이스케이프가 있어야 하는 Java 문자열 리터럴은 "u" 앞에 역슬래시를 두 개 쓴다.
 */
public class LexerTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public LexerTest() {
    }

    /**
     * Lexes a whole source text (slashes stay punctuators).
     * <p>
     * 소스 텍스트 전체를 토큰으로 나눈다(슬래시는 구두점으로 남는다).
     *
     * @param src the source / 소스
     * @return the tokens without the final EOF / 마지막 EOF 를 제외한 토큰들
     */
    private List<Token> lex(String src) {
        Lexer lx = new Lexer(src, "test.js");
        List<Token> out = new ArrayList<Token>();
        // EOF 토큰이 나올 때까지 계속 읽는다.
        while (true) {
            Token t = lx.next();
            if (t.type == TokenType.EOF) {
                return out;
            }
            out.add(t);
        }
    }

    /**
     * Returns a compact text form of tokens: NAME as name, punctuators as is, numbers as N(value), strings as S(value).
     * <p>
     * 토큰의 간단한 문자열 형태를 반환한다: 이름은 그대로, 구두점은 그대로, 숫자는 N(값), 문자열은 S(값).
     *
     * @param src the source / 소스
     * @return the form joined by spaces / 공백으로 이은 문자열
     */
    private String forms(String src) {
        StringBuilder sb = new StringBuilder();
        for (Token t : lex(src)) { // 각 토큰을 이어 붙여 하나의 문자열로 만든다.
            if (sb.length() > 0) {
                sb.append(' ');
            }
            switch (t.type) {
            case NUMBER:
                sb.append("N(").append(t.raw).append(')');
                break;
            case BIGINT:
                sb.append("I(").append(t.bigInteger.toString()).append(')');
                break;
            case STRING:
                sb.append("S(").append(t.value).append(')');
                break;
            default:
                sb.append(t.value);
            }
        }
        return sb.toString();
    }

    /**
     * Expects a SyntaxError for a source text and returns it.
     * <p>
     * 소스 텍스트가 SyntaxError 를 내는지 확인하고 그 예외를 반환한다.
     *
     * @param src the source / 소스
     * @return the exception / 예외
     */
    private JsException fails(final String src) {
        return (JsException) Check.throwsException(JsException.class, new Block() {
            public void run() {
                lex(src);
            }
        }, "lexing must fail: " + src);
    }

    /**
     * Basic statement tokens.
     * <p>
     * 기본 문장의 토큰.
     */
    public void testBasicTokens() {
        Check.equal("var a = N(1) ;", forms("var a = 1;"), "simple declaration");
        Check.equal("f ( x , S(s) ) { }", forms("f(x, 's') {}"), "calls and braces");
        List<Token> toks = lex("let x");
        Check.equal(TokenType.NAME, toks.get(0).type, "name type");
        Check.isTrue(toks.get(0).isName("let"), "isName");
        Check.isFalse(toks.get(0).isPunct("let"), "isPunct on a name");
    }

    /**
     * Longest-match punctuators, including the ES2016 and ES2020 additions.
     * <p>
     * ES2016, ES2020 추가 구두점을 포함한 가장 긴 일치.
     */
    public void testPunctuators() {
        Check.equal(">>>= >>> >>= >> >= >", forms(">>>= >>> >>= >> >= >"), "greater-than family");
        Check.equal("=== == => =", forms("=== == => ="), "equals family");
        Check.equal("!== != !", forms("!== != !"), "bang family");
        Check.equal("<<= << <= <", forms("<<= << <= <"), "less-than family");
        Check.equal("... . ++ += + -- -= -", forms("... . ++ += + -- -= -"), "dots and signs");
        Check.equal("&& &&= &= & || ||= |= | ^= ^ %= % **= ** *= * /= /", forms("&& &&= &= & || ||= |= | ^= ^ %= % **= ** *= * /= /"),
                "bit and arithmetic");
        Check.equal("a ** b", forms("a ** b"), "exponentiation");
        Check.equal("a ?. b a ?? b a ??= b", forms("a?.b a??b a??=b"), "optional chaining and nullish");
        Check.equal("a ? N(.3) : b", forms("a?.3:b"), "optional chaining punctuator is not formed before a digit");
    }

    /**
     * Private identifiers are single tokens, including escaped identifier parts.
     * <p>
     * private 식별자는 이스케이프가 있는 식별자 부분까지 하나의 토큰이다.
     */
    public void testPrivateNames() {
        List<Token> names = lex("#x #\\u0079");
        Check.equal(TokenType.PRIVATE_NAME, names.get(0).type, "private type");
        Check.equal("x", names.get(0).value, "private value excludes hash");
        Check.equal("#x", names.get(0).raw, "private raw includes hash");
        Check.equal("y", names.get(1).value, "escaped private name");
        Check.isTrue(names.get(1).escaped, "escaped flag");
        fails("#");
        fails("#1x");
        fails("# if");
        Check.equal("a . . . b", forms("a . . . b"), "spaced dots stay separate");
    }

    /**
     * Identifiers: Unicode letters, supplementary characters and escapes; escaped names are marked.
     * <p>
     * 식별자: 유니코드 글자, 보충 문자, 이스케이프. 이스케이프된 이름은 표시된다.
     */
    public void testIdentifiers() {
        String korean = new String(new char[] { (char) 0xBCC0, (char) 0xC218 });
        Check.equal(korean + " $_a1", forms(korean + " $_a1"), "Korean and ASCII identifiers");
        String astral = new String(Character.toChars(0x20BB7));
        Check.equal(astral + "x", forms(astral + "x"), "supplementary letter is an identifier start");
        List<Token> t = lex("\\u0061bc a\\u{62}c");
        Check.equal("abc", t.get(0).value, "escape at the start");
        Check.isTrue(t.get(0).escaped, "escaped flag");
        Check.equal("abc", t.get(1).value, "braced escape inside");
        Check.isFalse(t.get(0).isName("abc"), "an escaped name is never a keyword match");
        Check.equal("if", lex("\\u0069f").get(0).value, "escaped keyword text");
        Check.isFalse(lex("\\u0069f").get(0).isName("if"), "escaped keyword is not the keyword");
        fails("\\u0030a");
        fails("a\\u0020b");
        fails("\\x61");
        fails("\\u{110000}");
        fails("\\u12");
    }

    /**
     * Numeric literal forms and values.
     * <p>
     * 숫자 리터럴의 형태와 값.
     */
    public void testNumbers() {
        Check.equal(0.0, Double.valueOf(lex("0").get(0).number), "zero");
        Check.equal(123.0, Double.valueOf(lex("123").get(0).number), "integer");
        Check.equal(1.5, Double.valueOf(lex("1.5").get(0).number), "fraction");
        Check.equal(0.5, Double.valueOf(lex(".5").get(0).number), "leading dot");
        Check.equal(5.0, Double.valueOf(lex("5.").get(0).number), "trailing dot");
        Check.equal(1000.0, Double.valueOf(lex("1e3").get(0).number), "exponent");
        Check.equal(0.001, Double.valueOf(lex("1E-3").get(0).number), "negative exponent");
        Check.equal(31.0, Double.valueOf(lex("0x1F").get(0).number), "hex");
        Check.equal(15.0, Double.valueOf(lex("0o17").get(0).number), "octal");
        Check.equal(5.0, Double.valueOf(lex("0b101").get(0).number), "binary");
        Check.equal(1.8446744073709552E19, Double.valueOf(lex("0xFFFFFFFFFFFFFFFF").get(0).number), "large hex rounds to nearest");
        Token legacy = lex("017").get(0);
        Check.equal(15.0, Double.valueOf(legacy.number), "legacy octal value");
        Check.isTrue(legacy.legacyOctal, "legacy octal flag");
        Token nonOctal = lex("089").get(0);
        Check.equal(89.0, Double.valueOf(nonOctal.number), "non-octal decimal with leading zero");
        Check.isTrue(nonOctal.legacyOctal, "flagged as legacy for strict mode");
        Check.isFalse(lex("0").get(0).legacyOctal, "plain zero is not legacy");
        Check.equal("N(1.) . toString ( )", forms("1..toString()"), "1. followed by a member access");
        Check.equal("N(1) . toString ( )", forms("1 .toString()"), "space before the dot");
        Check.equal("N(0.5e1)", forms("0.5e1"), "fraction with exponent");
        Check.equal("I(1000)", forms("1_000n"), "decimal BigInt separator");
        Check.equal("I(51966)", forms("0xCA_FEn"), "hex BigInt separator");
        Check.equal("I(83)", forms("0o123n"), "octal BigInt");
        Check.equal("I(5)", forms("0b101n"), "binary BigInt");
        Check.equal(1000.0, Double.valueOf(lex("1_000").get(0).number), "decimal separator");
        Check.equal(0.125, Double.valueOf(lex("0.1_25").get(0).number), "fraction separator");
        Check.equal(4095.0, Double.valueOf(lex("0xF_FF").get(0).number), "hex separator");
    }

    /**
     * Invalid numeric literals.
     * <p>
     * 올바르지 않은 숫자 리터럴.
     */
    public void testNumberErrors() {
        fails("3in");
        fails("1.toString()");
        fails("0x");
        fails("0b2");
        fails("0o8");
        fails("1e");
        fails("1e+");
        fails("0x1g");
        fails("12abc");
        fails("07a");
        fails("1__0");
        fails("1_");
        fails("0x_FF");
        fails("0x1_");
        fails("1e1_");
        fails("0_1");
        fails("01n");
        fails("1.0n");
        fails("1e2n");
    }

    /**
     * String literal escapes.
     * <p>
     * 문자열 리터럴 이스케이프.
     */
    public void testStringEscapes() {
        Check.equal("a\nb\tc\rd", lex("'a\\nb\\tc\\rd'").get(0).value, "single escapes");
        Check.equal("A", lex("'\\x41'").get(0).value, "hex escape");
        Check.equal("A", lex("'\\u0041'").get(0).value, "unicode escape");
        Check.equal(new String(Character.toChars(0x1F600)), lex("'\\u{1F600}'").get(0).value, "braced escape makes a surrogate pair");
        Check.equal("😀", lex("'\\uD83D\\uDE00'").get(0).value, "two escapes make the same pair");
        Check.equal("\0", lex("'\\0'").get(0).value, "NUL escape");
        Check.equal("'\"\\", lex("'\\'\\\"\\\\'").get(0).value, "quote and backslash escapes");
        Check.equal("ab", lex("'a\\\nb'").get(0).value, "line continuation with LF");
        Check.equal("ab", lex("'a\\\r\nb'").get(0).value, "line continuation with CRLF");
        Check.equal("q", lex("'\\q'").get(0).value, "identity escape");
        Check.equal("\b\f" + (char) 0x0B, lex("'\\b\\f\\v'").get(0).value, "control escapes");
        Check.equal("'ab'", lex("'ab'").get(0).raw, "raw keeps quotes");
        Check.equal("x\"y", lex("\"x\\\"y\"").get(0).value, "double quoted");
        Token oct = lex("'\\101'").get(0);
        Check.equal("A", oct.value, "legacy octal escape");
        Check.isTrue(oct.legacyOctal, "legacy octal flag for strict mode");
        Check.equal(String.valueOf((char) 1), lex("'\\01'").get(0).value, "backslash-01 is a legacy octal escape for char 1");
        Check.isTrue(lex("'\\01'").get(0).legacyOctal, "\\01 flagged");
        Check.isFalse(lex("'\\0'").get(0).legacyOctal, "\\0 alone is fine");
        Check.isTrue(lex("'\\8'").get(0).legacyOctal, "\\8 flagged");
        Check.equal(String.valueOf((char) 0xFF), lex("'\\377'").get(0).value, "octal escape up to 377");
        Check.equal(String.valueOf((char) 0x1F) + "8", lex("'\\378'").get(0).value, "octal stops at a non-octal digit");
    }

    /**
     * Invalid strings.
     * <p>
     * 올바르지 않은 문자열.
     */
    public void testStringErrors() {
        fails("'abc");
        fails("'abc\nd'");
        fails("'a\\x4'");
        fails("'a\\u00'");
        fails("'a\\u{}'");
        fails("'a\\u{110000}'");
        fails("'a" + (char) 0x2028 + "b'");
        fails("'\\");
    }

    /**
     * Template literal parts: cooked and raw values, substitutions and line ending normalization.
     * <p>
     * 템플릿 리터럴 조각: 해석 값과 원문, 치환, 줄 끝 정규화.
     */
    public void testTemplates() {
        Lexer lx = new Lexer("`a${x}b${y}c`", "t");
        Token head = lx.next();
        Check.equal(TokenType.TEMPLATE, head.type, "head type");
        Check.equal("a", head.value, "head cooked");
        Check.isFalse(head.tail, "head is not the tail");
        Token x = lx.next();
        Check.equal("x", x.value, "substitution tokens are normal tokens");
        Token rb = lx.next();
        Check.isTrue(rb.isPunct("}"), "closing brace");
        Token mid = lx.rescanTemplateContinuation(rb);
        Check.equal("b", mid.value, "middle cooked");
        Check.isFalse(mid.tail, "middle");
        Token y = lx.next();
        Check.equal("y", y.value, "second substitution");
        Token tail = lx.rescanTemplateContinuation(lx.next());
        Check.equal("c", tail.value, "tail cooked");
        Check.isTrue(tail.tail, "tail flag");
        Check.equal(TokenType.EOF, lx.next().type, "end");

        Token noSubst = new Lexer("`line1\r\nline2\rline3\\n`", "t").next();
        Check.equal("line1\nline2\nline3\n", noSubst.value, "cooked: CR and CRLF become LF, escapes are resolved");
        Check.equal("line1\nline2\nline3\\n", noSubst.raw, "raw: CR and CRLF become LF, escapes stay");
        Check.isTrue(noSubst.tail, "no substitutions means a tail");
        Token dollar = new Lexer("`$ {} $`", "t").next();
        Check.equal("$ {} $", dollar.value, "a dollar not followed by a brace is plain text");
    }

    /**
     * Invalid escapes in templates make the cooked value null and record the error (tagged templates allow them).
     * <p>
     * 템플릿의 잘못된 이스케이프는 해석 값을 null 로 만들고 오류를 기록한다(태그 템플릿은 허용).
     */
    public void testTemplateInvalidEscapes() {
        Token t = new Lexer("`a\\u{zz}b`", "t").next();
        Check.isNull(t.value, "cooked is undefined");
        Check.notNull(t.templateError, "error is recorded");
        Check.equal("a\\u{zz}b", t.raw, "raw is intact");
        Token oct = new Lexer("`\\01`", "t").next();
        Check.isNull(oct.value, "legacy octal is invalid in templates");
        Token ok = new Lexer("`\\0`", "t").next();
        Check.equal("\0", ok.value, "\\0 is valid");
        fails("`abc");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Lexer l = new Lexer("`a${b}", "t");
                l.next();
                l.next();
                l.rescanTemplateContinuation(l.next());
            }
        }, "a template continuation without a closing backtick");
    }

    /**
     * Comments, line terminators and the newlineBefore flag.
     * <p>
     * 주석, 줄 끝, newlineBefore 플래그.
     */
    public void testCommentsAndNewlines() {
        List<Token> t = lex("a // comment\nb /* x */ c /* multi\nline */ d");
        Check.equal(4, t.size(), "comments produce no tokens");
        Check.isFalse(t.get(0).newlineBefore, "a");
        Check.isTrue(t.get(1).newlineBefore, "b after a line comment and LF");
        Check.isFalse(t.get(2).newlineBefore, "c after a single-line block comment");
        Check.isTrue(t.get(3).newlineBefore, "d after a multi-line block comment");
        t = lex("a\r\nb\rc" + (char) 0x2028 + "d" + (char) 0x2029 + "e");
        Check.equal(5, t.size(), "five names");
        Check.equal("1,2,3,4,5", t.get(0).line + "," + t.get(1).line + "," + t.get(2).line + "," + t.get(3).line + "," + t.get(4).line,
                "CRLF counts once, CR, LS and PS are line terminators");
        fails("/* never closed");
    }

    /**
     * HTML-like comments exist in scripts but not in modules.
     * <p>
     * HTML 유사 주석은 스크립트에는 있고 모듈에는 없다.
     */
    public void testHtmlLikeComments() {
        Check.equal("a b", forms("a <!-- hidden\nb"), "<!-- comments to the end of the line");
        Check.equal("a b", forms("a\n--> hidden\nb"), "--> at the start of a line is a comment");
        Check.equal("a -- > b", forms("a --> b"), "--> in the middle of a line is decrement and greater-than");
        Lexer lx = new Lexer("a <!-- b", "m");
        lx.setModule(true);
        Check.equal("a", lx.next().value, "name");
        Check.equal("<", lx.next().value, "module goal: < is a plain operator");
    }

    /**
     * Regular expression literals are produced on request by rescanning a slash token.
     * <p>
     * 정규식 리터럴은 슬래시 토큰을 다시 읽어 요청할 때 만들어진다.
     */
    public void testRegExpRescan() {
        Lexer lx = new Lexer("/ab+c/gi.test(x)", "t");
        Token slash = lx.next();
        Check.isTrue(slash.isPunct("/"), "first the slash is a punctuator");
        Token re = lx.rescanRegExp(slash);
        Check.equal(TokenType.REGEXP, re.type, "regexp type");
        Check.equal("ab+c", re.value, "pattern");
        Check.equal("gi", re.flags, "flags");
        Check.equal(".", lx.next().value, "scanning continues after the literal");
        Lexer cls = new Lexer("/[/\\]]\\//y", "t");
        Token r2 = cls.rescanRegExp(cls.next());
        Check.equal("[/\\]]\\/", r2.value, "slash in a class and escaped slash do not end the literal");
        Check.equal("y", r2.flags, "flag y");
        Lexer eq = new Lexer("/=a/", "t");
        Token t1 = eq.next();
        Check.isTrue(t1.isPunct("/="), "/= is lexed as an operator first");
        Check.equal("=a", eq.rescanRegExp(t1).value, "and re-read as a regexp");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Lexer l = new Lexer("/a/gg", "t");
                l.rescanRegExp(l.next());
            }
        }, "duplicate flag");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Lexer l = new Lexer("/a/x", "t");
                l.rescanRegExp(l.next());
            }
        }, "unknown flag");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Lexer l = new Lexer("/abc\n/", "t");
                l.rescanRegExp(l.next());
            }
        }, "newline in a regexp literal");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Lexer l = new Lexer("/abc", "t");
                l.rescanRegExp(l.next());
            }
        }, "unterminated regexp");
    }

    /**
     * Positions: line and column of tokens and of errors.
     * <p>
     * 위치: 토큰과 오류의 줄과 열.
     */
    public void testPositions() {
        List<Token> t = lex("ab  cd\n  ef");
        Check.equal("1:1", t.get(0).line + ":" + t.get(0).column, "ab");
        Check.equal("1:5", t.get(1).line + ":" + t.get(1).column, "cd");
        Check.equal("2:3", t.get(2).line + ":" + t.get(2).column, "ef");
        Check.equal(0, t.get(0).start, "start offset");
        Check.equal(2, t.get(0).end, "end offset");
        JsException e = fails("a\n  @");
        Check.equal(2, e.getLine(), "error line");
        Check.equal(3, e.getColumn(), "error column");
        Check.equal("test.js", e.getSourceName(), "source name");
        Check.equal(ErrorKind.SYNTAX_ERROR, e.getKind(), "SyntaxError kind");
        JsException u = fails("x = 'abc\n");
        Check.equal(1, u.getLine(), "unterminated string reports the start line");
        Check.equal(5, u.getColumn(), "and the start column");
        Check.contains(e.getMessage(), "test.js:2:3", "message contains the position");
    }

    /**
     * State save and restore allow re-reading tokens.
     * <p>
     * 상태 저장과 복원으로 토큰을 다시 읽을 수 있다.
     */
    public void testSaveAndRestore() {
        Lexer lx = new Lexer("a b\nc", "t");
        lx.next();
        Lexer.State s = lx.saveState();
        Token b = lx.next();
        Token c = lx.next();
        Check.equal(2, c.line, "c is on line 2");
        lx.restoreState(s);
        Token b2 = lx.next();
        Check.equal(b.value, b2.value, "same token again");
        Check.equal(b.column, b2.column, "same column again");
        Token c2 = lx.next();
        Check.equal(2, c2.line, "line numbers are restored too");
        lx.resetAfter(b);
        Check.equal("c", lx.next().value, "resetAfter continues after a given token");
    }

    /**
     * White space characters of the specification are skipped.
     * <p>
     * 규격에 정의된 공백 문자는 건너뛴다.
     */
    public void testWhiteSpaceCharacters() {
        String ws = "\t" + (char) 0x0B + (char) 0x0C + " " + (char) 0xA0 + (char) 0xFEFF + (char) 0x2003 + (char) 0x3000;
        Check.equal("a b", forms("a" + ws + "b"), "all white space characters separate tokens");
        fails("a" + (char) 0x200B + "b");
    }
}
