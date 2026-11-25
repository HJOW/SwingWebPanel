package org.duckdns.hjow.swingwebpanel.js.parser;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.ast.AstDump;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of expression parsing (plan items P12-06 and P12-07): regular expression versus division, automatic semicolon
 * insertion, operator precedence and associativity, member/call/new forms, literals, templates, arrow functions,
 * destructuring assignment and error reporting for bad expressions. Trees are compared as S-expressions (see AstDump).
 * <p>
 * 표현식 파싱 시험(계획 P12-06, P12-07): 정규식과 나눗셈의 구분, 자동 세미콜론 삽입, 연산자 우선순위와 결합 방향,
 * 멤버/호출/new 형태, 리터럴, 템플릿, 화살표 함수, 구조 분해 대입, 잘못된 표현식의 오류 보고. 트리는 S-식(AstDump 참조)으로 비교한다.
 */
public class ParserExpressionTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ParserExpressionTest() {
    }

    /**
     * Parses a script and dumps the expression of its first statement.
     * <p>
     * 스크립트를 파싱해 첫 문장의 표현식을 덤프한다.
     *
     * @param src the source / 소스
     * @return the S-expression / S-식
     */
    private String e(String src) {
        Node p = Parser.parseScript(src, "t.js");
        Check.isTrue(p.list.length >= 1, "at least one statement in: " + src);
        return AstDump.dump(p.list[0].a);
    }

    /**
     * Parses a script and dumps the whole program.
     * <p>
     * 스크립트를 파싱해 프로그램 전체를 덤프한다.
     *
     * @param src the source / 소스
     * @return the S-expression / S-식
     */
    private String p(String src) {
        return AstDump.dump(Parser.parseScript(src, "t.js"));
    }

    /**
     * Expects a SyntaxError for a source text.
     * <p>
     * 소스 텍스트가 SyntaxError 를 내는지 확인한다.
     *
     * @param src the source / 소스
     * @return the exception / 예외
     */
    private JsException bad(final String src) {
        JsException ex = (JsException) Check.throwsException(JsException.class, new Block() {
            public void run() {
                Parser.parseScript(src, "t.js");
            }
        }, "must be a SyntaxError: " + src);
        Check.equal(ErrorKind.SYNTAX_ERROR, ex.getKind(), "kind for: " + src);
        return ex;
    }

    /**
     * Operator precedence and left/right associativity.
     * <p>
     * 연산자 우선순위와 좌/우 결합.
     */
    public void testPrecedenceAndAssociativity() {
        Check.equal("(+ 1 (* 2 3))", e("1 + 2 * 3"), "multiplication binds tighter");
        Check.equal("(+ (* 1 2) 3)", e("1 * 2 + 3"), "multiplication first");
        Check.equal("(- (- a b) c)", e("a - b - c"), "left associative");
        Check.equal("(= a (= b c))", e("a = b = c"), "assignment is right associative");
        Check.equal("(?: a b (?: c d e))", e("a ? b : c ? d : e"), "conditional is right associative");
        Check.equal("(|| a (&& b c))", e("a || b && c"), "&& binds tighter than ||");
        Check.equal("(| a (^ b (& c d)))", e("a | b ^ c & d"), "bitwise precedence");
        Check.equal("(== a (< b c))", e("a == b < c"), "relational binds tighter than equality");
        Check.equal("(<< a (+ b c))", e("a << b + c"), "additive binds tighter than shift");
        Check.equal("(< (<< a b) c)", e("a << b < c"), "shift binds tighter than relational");
        Check.equal("(* (- a) b)", e("-a * b"), "unary binds tighter than multiplication");
        Check.equal("(+ (typeof a) b)", e("typeof a + b"), "typeof is a prefix operator");
        Check.equal("(! (! a))", e("!!a"), "nested unary");
        Check.equal("(, a b c)", e("a, b, c"), "comma expression");
        Check.equal("(instanceof a B)", e("a instanceof B"), "instanceof");
        Check.equal("(in \"x\" o)", e("'x' in o"), "in");
        Check.equal("(+ (post++ a) (pre++ b))", e("a++ + ++b"), "postfix and prefix update");
        Check.equal("(= a (+ b c))", e("a = b + c"), "assignment is lowest");
        Check.equal("(+= a (* b c))", e("a += b * c"), "compound assignment");
        Check.equal("(>>>= a 1)", e("a >>>= 1"), "unsigned shift assignment");
        Check.equal("(- (- a))", e("- -a"), "double negation with space");
        Check.equal("(&& (&& a b) c)", e("a && b && c"), "logical and is left associative");
        Check.equal("(delete (. a b))", e("delete a.b"), "delete");
        Check.equal("(void 0)", e("void 0"), "void");
        Check.equal("(** 2 (** 3 2))", e("2 ** 3 ** 2"), "exponentiation is right associative");
        Check.equal("(** 2 (+ 3 4))", e("2 ** (3 + 4)"), "parenthesized exponent");
        Check.equal("(** 2 (- 2))", e("2 ** -2"), "unary exponent is allowed on the right");
        Check.equal("(**= a 3)", e("a **= 3"), "exponentiation assignment");
        Check.equal("(** (- 2) 3)", e("(-2) ** 3"), "parenthesized unary is allowed on the left");
        Check.equal("51966", e("0xCAFEn"), "BigInt literal");
        bad("-2 ** 3");
        Check.equal("(?? a b)", e("a ?? b"), "nullish coalescing");
        Check.equal("(?? (?? a b) c)", e("a ?? b ?? c"), "nullish is left associative");
        Check.equal("(??= a b)", e("a ??= b"), "nullish assignment");
        Check.equal("(&&= a b)", e("a &&= b"), "logical-and assignment");
        Check.equal("(||= a b)", e("a ||= b"), "logical-or assignment");
        bad("a ?? b || c");
        bad("a || b ?? c");
        bad("a ?? b && c");
        Check.equal("(?? a (&& b c))", e("a ?? (b && c)"), "parentheses permit the logical mix");
        Check.equal("(chain (.? a b))", e("a?.b"), "optional member");
        Check.equal("(chain (. (.? a b) c))", e("a?.b.c"), "normal tail remains in the chain scope");
        Check.equal("(chain ([]? a k))", e("a?.[k]"), "optional computed member");
        Check.equal("(chain (call? a x))", e("a?.(x)"), "optional call");
        Check.equal("(chain (call (.? a b) x))", e("a?.b(x)"), "normal call in optional chain");
        Check.equal("(call (chain (.? a b)) c)", e("(a?.b)(c)"), "parentheses end the chain scope");
        Check.equal("(async-arrow (x) (await x))", e("async x => await x"), "async identifier arrow");
        Check.equal("(async-arrow (x) (await x))", e("async (x) => await x"), "async parenthesized arrow");
        Check.equal("(async-function f () (block (expr (await 1))))", e("(async function f() { await 1 })"),
                "async function expression");
        Check.equal("(async-function* _ () (block (expr (yield (await 1)))))", e("(async function* () { yield await 1 })"),
                "async generator expression");
        Check.equal("(object (method m (async-method m () (block (expr (await x)))))"
                + " (method g (async-function* g () (block (expr (yield (await x)))))))",
                e("({async m() { await x }, async *g() { yield await x }})"), "async object methods");
        Check.equal("(call async x)", e("async(x)"), "async remains an identifier for an ordinary call");
        Check.equal("(arrow (async) async)", e("async => async"), "async is an ordinary arrow parameter without a second operand");
        bad("async x\n=> x");
        bad("async (await) => 0");
        bad("(async function(await) {})");
        bad("async function await() {}");
        bad("async\n(x) => x");
        bad("class A { async constructor() {} }");
        bad("a?.b = c");
        bad("a?.b++");
        bad("a?.b`x`");
        bad("new a?.b()");
        Check.equal("(object (... a) (init b 1) (... c))", e("({...a, b: 1, ...c})"), "object spread");
        Check.equal("(= (objpat (pp a a) (rest b)) c)", e("({a, ...b} = c)"), "object assignment rest");
        Check.equal("(import-call (+ \"a\" x))", e("import('a' + x)"), "dynamic import expression");
        bad("({...a, ...b, c} = d)");
        bad("import(x, y)");
        bad("import(x,)");
        bad("import.meta");
    }

    /**
     * Member, call and new expressions.
     * <p>
     * 멤버, 호출, new 표현식.
     */
    public void testMemberCallNew() {
        Check.equal("([] (call (. (. a b) c) d) e)", e("a.b.c(d)[e]"), "chain");
        Check.equal("(. (new Foo a) bar)", e("new Foo(a).bar"), "new with arguments then member");
        Check.equal("(new (new X))", e("new new X()()"), "nested new");
        Check.equal("(new (. (. a b) c))", e("new a.b.c"), "new without arguments takes the whole member chain");
        Check.equal("(call (. (new a) b))", e("new a().b()"), "call after new with arguments");
        Check.equal("(. a if)", e("a.if"), "keywords are valid property names");
        Check.equal("(call f (... a) b)", e("f(...a, b)"), "spread argument");
        Check.equal("(call f a b)", e("f(a, b,)"), "trailing comma in arguments is accepted (ES2017 extension)");
        Check.equal("(call (call f))", e("f()()"), "call of call");
        Check.equal("(new f)", e("new f"), "new without parentheses");
        Check.equal("(call (. (call f) g))", e("f().g()"), "member of call result");
    }

    /**
     * Literals and property names.
     * <p>
     * 리터럴과 프로퍼티 이름.
     */
    public void testLiterals() {
        Check.equal("1.5", e("1.5"), "number");
        Check.equal("\"s\"", e("'s'"), "string");
        Check.equal("\"a\nb\"", e("'a\\nb'"), "string escape is resolved");
        Check.equal("true", e("true"), "true");
        Check.equal("null", e("null"), "null");
        Check.equal("1000", e("1000n"), "BigInt is a literal node");
        Check.equal("1000", e("1_000n"), "separated BigInt");
        Check.equal("this", e("this"), "this");
        Check.equal("(array 1 _ 2 (... a))", e("[1,,2,...a]"), "array literal with a hole and spread");
        Check.equal("(array)", e("[]"), "empty array");
        Check.equal("(array _)", e("[,]"), "array with a single hole");
        Check.equal("(object (short a a) (init b 1) (init [c] 2) (init d 3) (init 4 5))", e("({a, b: 1, [c]: 2, 'd': 3, 4: 5})"),
                "object literal forms");
        Check.equal("(object (init 1 a) (init 0.5 b) (init 255 c))", e("({1: a, .5: b, 0xff: c})"), "numeric keys are converted to strings");
        String o = e("({ get e() { return 1 }, set e(v) {}, f() {}, *g() {} })");
        Check.equal("(object (get e (method e () (block (return 1)))) (set e (method e (v) (block))) (method f (method f () (block)))"
                + " (method g (function* g () (block))))", o, "accessors and methods");
        Check.equal("(object (init __proto__ a) (init [\"__proto__\"] b))", e("({__proto__: a, ['__proto__']: b})"),
                "a computed __proto__ is not a duplicate");
    }

    /**
     * A slash is a regular expression where an expression may start and a division after an operand.
     * <p>
     * 슬래시는 표현식이 시작될 수 있는 곳에서는 정규식, 피연산자 뒤에서는 나눗셈이다.
     */
    public void testRegExpVersusDivision() {
        Check.equal("(call (. /ab+/g test) x)", e("/ab+/g.test(x)"), "regexp at the start of a statement");
        Check.equal("(/ (/ a b) c)", e("a / b / c"), "division chain");
        Check.equal("(/ (post++ a) 2)", e("a++ / 2"), "division after a postfix operator");
        Check.equal("(/ a 2)", e("a / 2"), "division after an identifier");
        Check.equal("(/ a b)", e("(a) / b"), "division after a parenthesized expression");
        Check.equal("(= x /=/)", e("x = /=/"), "regexp starting with equals after an assignment operator");
        Check.equal("(/= a 2)", e("a /= 2"), "compound division assignment");
        Check.equal("(array /a/ /b/i)", e("[/a/, /b/i]"), "regexps in an array");
        Check.equal("(/ (/ x y) g)", e("x\n/y/g"), "a newline does not start a regexp after an operand");
        Check.equal("(program (if a (expr (call (. /re/ test) b)) _))", p("if (a) /re/.test(b)"), "regexp after the parenthesis of an if");
        Check.equal("(function f () (block (return /x/)))", e("(function f() { return /x/ })"), "regexp after return");
        Check.equal("(typeof /x/)", e("typeof /x/"), "regexp after typeof");
        Check.equal("(?: a /b/ /c/)", e("a ? /b/ : /c/"), "regexp after ? and :");
        Check.equal("(object (init a /b/))", e("({a: /b/})"), "regexp as a property value");
        bad("/abc");
        bad("/a/gg");
        bad("/a/x");
    }

    /**
     * Template literals and tagged templates.
     * <p>
     * 템플릿 리터럴과 태그 템플릿.
     */
    public void testTemplates() {
        Check.equal("(template \"a\" b \"c\" d \"e\")", e("`a${b}c${d}e`"), "substitutions");
        Check.equal("(template \"\")", e("``"), "empty template");
        Check.equal("(tagged f (template \"x\" y \"\"))", e("f`x${y}`"), "tagged template");
        Check.equal("(template \"\" (template \"\" a \"\") \"\")", e("`${`${a}`}`"), "nested template");
        Check.equal("(template \"\" (object (init a 1)) \"\")", e("`${ {a: 1} }`"), "object literal inside a substitution");
        Check.equal("(tagged (. a b) (template \"\"))", e("a.b``"), "tag with a member expression");
        Check.equal("(tagged f (template undefined))", e("f`\\u{zz}`"), "invalid escapes are allowed in tagged templates");
        bad("`\\u{zz}`");
        bad("`abc");
        bad("`a${b");
    }

    /**
     * Arrow functions in all parameter forms.
     * <p>
     * 모든 매개변수 형태의 화살표 함수.
     */
    public void testArrowFunctions() {
        Check.equal("(arrow (x) x)", e("x => x"), "single identifier");
        Check.equal("(arrow (a b) (+ a b))", e("(a, b) => a + b"), "two parameters");
        Check.equal("(arrow () (block))", e("() => {}"), "no parameters, empty block");
        Check.equal("(arrow ((default a 1) (objpat (pp b b)) (arrpat c) (rest d)) 0)", e("(a = 1, {b}, [c], ...d) => 0"),
                "default, patterns and rest");
        Check.equal("(arrow (x) (arrow (y) z))", e("x => y => z"), "curried arrows");
        Check.equal("(arrow (x) (object (init a 1)))", e("x => ({a: 1})"), "object literal body needs parentheses");
        Check.equal("(arrow ((objpat (pp a (default a 1)))) 0)", e("({a = 1}) => 0"), "shorthand default in a parameter pattern");
        Check.equal("(, a b)", e("(a, b)"), "a parenthesized expression is not an arrow");
        Check.equal("a", e("(a)"), "parenthesized identifier");
        Check.equal("(call f (arrow (x) x))", e("f(x => x)"), "arrow as an argument");
        Check.equal("(?: a (arrow (b) b) c)", e("a ? b => b : c"), "arrow in a conditional branch");
        Check.equal("(program (expr (arrow (a) (block (return a)))))", p("a => { return a }"), "block body with return");
        bad("(a, b)\n=> 1");
        bad("a\n=> 1");
        bad("(a, a) => 1");
        bad("(a, ...b, c) => 1");
        bad("() => {} ()");
        bad("(1) => 1");
        bad("(a + b) => 1");
    }

    /**
     * Destructuring assignment (cover grammar) and invalid targets.
     * <p>
     * 구조 분해 대입(커버 문법)과 잘못된 대상.
     */
    public void testDestructuringAssignment() {
        Check.equal("(= (arrpat a b) (array b a))", e("[a, b] = [b, a]"), "array pattern swap");
        Check.equal("(= (objpat (pp a a) (pp b (arrpat c)) (pp d (default d 1))) e)", e("({a, b: [c], d = 1} = e)"), "object pattern");
        Check.equal("(= (arrpat (default a 1) (rest b)) c)", e("[a = 1, ...b] = c"), "defaults and rest");
        Check.equal("(= (arrpat (. a b) ([] c 0)) d)", e("[a.b, c[0]] = d"), "member expressions as targets");
        Check.equal("(= (arrpat _ a) b)", e("[, a] = b"), "elision");
        Check.equal("(= (objpat (pp [k] v)) o)", e("({[k]: v} = o)"), "computed key");
        Check.equal("(= (arrpat (arrpat a) (objpat (pp b b))) c)", e("[[a], {b}] = c"), "nested patterns");
        Check.equal("(= (objpat (pp __proto__ a) (pp __proto__ b)) c)", e("({__proto__: a, __proto__: b} = c)"),
                "duplicate __proto__ is fine in a pattern");
        bad("1 = 2");
        bad("a + b = c");
        bad("++1");
        bad("(a, b) = 1");
        bad("[a, b]++");
        bad("({a: 1} = 1)");
        bad("({a = 1})");
        bad("({a = 1}).b");
        bad("[...a, b] = c");
        bad("[...a = 1] = c");
        bad("([a]) = 1");
        bad("({a}) = 1");
        bad("f() = 1");
        bad("a ? b : c = d => ");
        bad("x = {__proto__: a, __proto__: b}");
    }

    /**
     * Automatic semicolon insertion rules.
     * <p>
     * 자동 세미콜론 삽입 규칙.
     */
    public void testAutomaticSemicolonInsertion() {
        Check.equal("(program (var (d a 1)) (var (d b 2)))", p("var a = 1\nvar b = 2"), "newline ends a declaration");
        Check.equal("(program (expr a) (expr (pre++ b)))", p("a\n++b"), "a newline before ++ makes it a prefix operator of the next statement");
        Check.equal("(program (expr i) (expr (pre++ j)))", p("i\n++\nj"), "postfix ++ may not follow a newline");
        Check.equal("(program (expr (call a b)))", p("a\n(b)"), "no insertion before a parenthesis");
        Check.equal("(program (expr (= a (call b c))))", p("a = b\n(c)"), "call continues the expression");
        Check.equal("(program (expr ([] a b)))", p("a\n[b]"), "no insertion before a bracket");
        Check.equal("(program (expr (/ (/ x y) g)))", p("x\n/y/g"), "no insertion before a slash");
        Check.equal("(program (fdecl f (function f () (block (return _) (expr 1)))))", p("function f() { return\n1 }"), "return is a restricted production");
        Check.equal("(program (fdecl f (function f () (block (return a) (expr (pre++ b))))))", p("function f() { return a\n++b }"),
                "return then a prefix operator on the next line");
        Check.equal("(program (while a (break _)) (expr b))", p("while (a) break\nb"), "break ends at a newline");
        Check.equal("(program (do (empty) 0) (expr y))", p("do ; while (0) y"), "a semicolon after do-while is optional even on the same line");
        Check.equal("(program (if a (expr b) (expr c)))", p("if (a) b\nelse c"), "else after a newline");
        Check.equal("(program (expr a) (expr b))", p("a; b"), "explicit semicolons");
        Check.equal("(program (block (expr a)))", p("{ a }"), "closing brace ends the statement");
        bad("a b");
        bad("a = 1 b = 2");
        bad("throw\n1");
        bad("var a = 1 var b = 2");
        bad("if (a) b else c");
        bad("for (;)");
        bad("x = ");
        bad("a +");
        bad("(a");
        bad("a)");
        bad("{");
    }

    /**
     * Error positions: line and column of the offending token.
     * <p>
     * 오류 위치: 문제가 된 토큰의 줄과 열.
     */
    public void testErrorPositions() {
        JsException e = bad("var a = 1;\nvar b = ;");
        Check.equal(2, e.getLine(), "line of the unexpected semicolon");
        Check.equal(9, e.getColumn(), "column of the unexpected semicolon");
        Check.contains(e.getMessage(), "Unexpected token ';'", "message");
        e = bad("x = 1 +\n  ");
        Check.contains(e.getMessage(), "Unexpected end of input", "end of input message");
        e = bad("a b");
        Check.equal("1:3", e.getLine() + ":" + e.getColumn(), "position of b");
        Check.contains(e.getMessage(), "Unexpected identifier 'b'", "identifier message");
        e = bad("let x = 1;\nlet x = 2;");
        Check.equal("2:5", e.getLine() + ":" + e.getColumn(), "position of the redeclaration");
        Check.contains(e.getMessage(), "Identifier 'x' has already been declared", "redeclaration message");
        Check.equal("t.js", e.getSourceName(), "source name");
    }

    /**
     * Valid and invalid uses of reserved words and keywords as identifiers.
     * <p>
     * 예약어와 키워드를 식별자로 쓰는 올바른/잘못된 사용.
     */
    public void testReservedWords() {
        Check.equal("(program (var (d yield 1)))", p("var yield = 1"), "yield is an identifier in sloppy code");
        Check.equal("(program (var (d let 1)))", p("var let = 1"), "let is an identifier in sloppy code");
        Check.equal("(program (var (d static 1)))", p("var static = 1"), "static is an identifier in sloppy code");
        bad("var if = 1");
        bad("var class = 1");
        bad("var enum = 1");
        bad("var null = 1");
        bad("var true = 1");
        bad("'use strict'; var let = 1");
        bad("'use strict'; var yield = 1");
        bad("'use strict'; var static = 1");
        bad("'use strict'; var implements");
        bad("'use strict'; var eval");
        bad("'use strict'; var arguments");
        bad("var \\u0069f");
        bad("\\u0069f (a) b");
        Check.equal("(. a if)", e("a.\\u0069f"), "escaped keywords are fine as property names");
    }
}
