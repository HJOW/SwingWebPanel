package org.duckdns.hjow.swingwebpanel.js.parser;

import org.duckdns.hjow.swingwebpanel.js.ast.AstDump;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.ProgramInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.ScopeInfo;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of statement, function, class and module parsing (plan items P12-07 and P12-08): tree shapes, scope descriptions
 * (hoisted names, lexical declarations, Annex B function hoisting), function information, directive prologues,
 * generator yield marking and the module import/export tables.
 * <p>
 * 문장, 함수, 클래스, 모듈 파싱 시험(계획 P12-07, P12-08): 트리 모양, 스코프 설명(끌어올린 이름, 렉시컬 선언, 부속서 B 함수 끌어올림),
 * 함수 정보, 지시문 서문, 제너레이터 yield 표시, 모듈 import/export 표.
 */
public class ParserStatementTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ParserStatementTest() {
    }

    /**
     * Parses a script and dumps the program.
     * <p>
     * 스크립트를 파싱해 프로그램을 덤프한다.
     *
     * @param src the source / 소스
     * @return the S-expression / S-식
     */
    private String p(String src) {
        return AstDump.dump(Parser.parseScript(src, "t.js"));
    }

    /**
     * Joins a string array with commas.
     * <p>
     * 문자열 배열을 쉼표로 잇는다.
     *
     * @param a the array / 배열
     * @return the text / 문자열
     */
    private String join(String[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) { // 각 원소를 쉼표로 잇는다.
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    /**
     * Declarations and simple statements.
     * <p>
     * 선언과 단순 문장.
     */
    public void testDeclarationsAndSimpleStatements() {
        Check.equal("(program (var (d a 1) (d b _)) (let (d (arrpat c d) e)) (const (d (objpat (pp f f)) g)))",
                p("var a = 1, b; let [c, d] = e; const {f} = g"), "variable declarations");
        Check.equal("(program (if a (expr b) (expr c)))", p("if (a) b; else c"), "if else");
        Check.equal("(program (while a (expr b)))", p("while (a) b"), "while");
        Check.equal("(program (do (expr a) b))", p("do a; while (b)"), "do while");
        Check.equal("(program (throw a) (debugger) (with a (expr b)))", p("throw a; debugger; with (a) b"), "throw, debugger, with");
        Check.equal("(program (empty) (block))", p(";{}"), "empty statement and block");
        Check.equal("(program (switch a (case 1 (expr b) (break _)) (case _ (expr c))))",
                p("switch (a) { case 1: b; break; default: c }"), "switch");
        Check.equal("(program (try (block (expr a)) e (block (expr b)) (block (expr c))))",
                p("try { a } catch (e) { b } finally { c }"), "try catch finally");
        Check.equal("(program (try (block) (objpat (pp message message)) (block) _))", p("try {} catch ({message}) {}"), "catch pattern");
        Check.equal("(program (try (block) _ _ (block)))", p("try {} finally {}"), "try finally");
    }

    /**
     * Loop statements including the for-in/of head forms.
     * <p>
     * for-in/of 머리 형태를 포함한 반복문.
     */
    public void testLoops() {
        Check.equal("(program (for (var (d i 0)) (< i 3) (post++ i) (expr x)))", p("for (var i = 0; i < 3; i++) x"), "classic for");
        Check.equal("(program (for _ _ _ (break _)))", p("for (;;) break"), "empty for");
        Check.equal("(program (for (, (= i 0) (= j 1)) _ _ (empty)))", p("for (i = 0, j = 1;;);"), "sequence initializer");
        Check.equal("(program (for-in a b (expr c)))", p("for (a in b) c"), "for in");
        Check.equal("(program (for-in (var (d a _)) b (empty)))", p("for (var a in b);"), "for var in");
        Check.equal("(program (for-of (let (d (arrpat a b) _)) c (expr d)))", p("for (let [a, b] of c) d"), "for let of with a pattern");
        Check.equal("(program (for-of (arrpat a b) c (empty)))", p("for ([a, b] of c);"), "for of with an assignment pattern");
        Check.equal("(program (for-of (. x y) z (empty)))", p("for (x.y of z);"), "member expression as a target");
        Check.equal("(program (for-of (const (d x _)) a (empty)))", p("for (const x of a);"), "const loop variable");
        Check.equal("(program (for (let (d i 0)) _ _ (empty)))", p("for (let i = 0;;);"), "let initializer");
        Check.equal("(program (fdecl f (async-function f () (block (for-await (const (d x _)) xs (empty))))))",
                p("async function f() { for await (const x of xs); }"),
                "for-await inside an async function");
        Check.equal("(program (label l (for _ _ _ (block (continue l) (break l)))))", p("l: for (;;) { continue l; break l; }"),
                "labelled loop with continue and break");
        Check.equal("(program (label a (label b (while 1 (continue a)))))", p("a: b: while (1) continue a"),
                "continue may target an outer label of the same loop");
    }

    /**
     * Function, generator and class declarations.
     * <p>
     * 함수, 제너레이터, 클래스 선언.
     */
    public void testFunctionAndClassDeclarations() {
        Check.equal("(program (fdecl f (function f (a (default b 1) (rest c)) (block (return a)))))",
                p("function f(a, b = 1, ...c) { return a }"), "parameters");
        Check.equal("(program (fdecl g (function* g () (block (expr (yield 1)) (expr (yield* (call h))) (var (d x (yield _)))))))",
                p("function* g() { yield 1; yield* h(); var x = yield; }"), "generator");
        Check.equal("(program (cdecl A (class A B (method constructor () (block)) (static-method m (method m () (block)))"
                + " (get x (method x () (block))) (set x (method x (v) (block))) (method g (function* g () (block)))"
                + " (method [k] (method _ () (block))))))",
                p("class A extends B { constructor() {} static m() {} get x() {} set x(v) {} *g() {} [k]() {} }"), "class members");
        Check.equal("(program (expr (class _ _ _)))", p("(class {})"), "anonymous class expression");
        Check.equal("(program (fdecl f (function f () (block (fdecl g (function g () (block)))))))", p("function f() { function g() {} }"),
                "nested function");
        Check.equal("(program (label l (fdecl f (function f () (block)))))", p("l: function f() {}"), "labelled function in sloppy mode");
        Check.equal("(program (if a (block (fdecl f (function f () (block)))) _))", p("if (a) function f() {}"),
                "Annex B function as an if body is wrapped in a block");
        Check.equal("(program (cdecl A (class A _ _ (static-method static (method static () (block)))"
                + " (get get (method get () (block))) (method get (method get () (block))))))",
                p("class A { static static() {} get get() {} get() {} }"), "contextual keywords as member names");
        Check.equal("(program (fdecl f (async-function f (x) (block (expr (await x))))))",
                p("async function f(x) { await x }"), "async function declaration");
        Check.equal("(program (fdecl g (async-function* g () (block (expr (yield (await 1)))))))",
                p("async function* g() { yield await 1 }"), "async generator declaration");
        Check.isTrue(p("class A { async m() { await 1 } async *g() { yield await 1 } }").contains("(async-method"),
                "async class methods");
        Check.isTrue(AstDump.dump(Parser.parseModule("await 1", "m.js")).contains("(expr (await 1))"),
                "top-level await in a module");
        Check.isTrue(AstDump.dump(Parser.parseModule("for await (const x of xs);", "m.js")).contains("(for-await"),
                "top-level for-await in a module");
        Check.isTrue(AstDump.dump(Parser.parseModule("export default async function f() {}", "m.js")).contains("async-function"),
                "exported async function");
        Node asyncProgram = Parser.parseScript("async function f() {}", "t.js");
        Check.isTrue(asyncProgram.list[0].b.fn.async, "FunctionInfo preserves async");
    }

    /**
     * Function information computed by the parser.
     * <p>
     * 파서가 계산하는 함수 정보.
     */
    public void testFunctionInfo() {
        Node prog = Parser.parseScript("function f(a, b = 1, ...c) {}\nfunction g(x, y) { return arguments[0]; }\n"
                + "function h() { 'use strict'; }\nfunction k() { return () => arguments; }", "t.js");
        FunctionInfo f = prog.list[0].b.fn;
        Check.equal("a,b,c", join(f.paramNames), "bound parameter names");
        Check.isFalse(f.simpleParams, "default and rest make the list non-simple");
        Check.equal(1, f.length, "length counts parameters before the first default");
        Check.isTrue(f.hasParamExpressions, "default is an expression");
        Check.isFalse(f.strict, "sloppy function");
        Check.isFalse(f.usesArguments, "does not use arguments");
        FunctionInfo g = prog.list[1].b.fn;
        Check.isTrue(g.simpleParams, "plain identifiers");
        Check.equal(2, g.length, "length 2");
        Check.isTrue(g.usesArguments, "uses arguments");
        Check.isTrue(prog.list[2].b.fn.strict, "function with a use strict directive");
        Check.isFalse(prog.list[2].b.fn.usesArguments, "h does not use arguments");
        Check.isTrue(prog.list[3].b.fn.usesArguments, "an arrow function using arguments marks the enclosing function");
        Check.equal("function g(x, y) { return arguments[0]; }", g.sourceText(), "source text slice");
        Check.equal("t.js", g.sourceName, "source name");
    }

    /**
     * Scope descriptions: hoisted names, lexical declarations, block scopes and Annex B hoisting.
     * <p>
     * 스코프 설명: 끌어올린 이름, 렉시컬 선언, 블록 스코프, 부속서 B 끌어올림.
     */
    public void testScopeInfo() {
        Node prog = Parser.parseScript("var a; function f() {} let b; const c = 1; class D {} var a; for (var i = 0;;) {}", "t.js");
        ScopeInfo s = prog.scope;
        Check.equal("a,f,i", join(s.varNames), "var names and function names in order, no duplicates");
        Check.equal("b,c,D", join(s.lexNames), "lexical names");
        Check.isTrue(!s.lexConst[0] && s.lexConst[1] && !s.lexConst[2], "const flags");
        Check.equal(1, s.functions.length, "one function declaration to instantiate");

        prog = Parser.parseScript("{ let x; function g() {} } { }", "t.js");
        Node block = prog.list[0];
        Check.equal("x", join(block.scope.lexNames), "block lexical names (function names are not listed)");
        Check.equal(1, block.scope.functions.length, "block functions");
        Check.isTrue(prog.list[1].scope == null, "an empty block needs no scope");
        Check.isTrue(block.list[1].flag, "Annex B: the block function is also hoisted as a var");
        Check.equal("g", join(prog.scope.varNames), "the hoisted name is a var of the function scope");

        prog = Parser.parseScript("let g; { function g() {} }", "t.js");
        Check.isFalse(prog.list[1].list[0].flag, "no hoisting when it would conflict with a let");
        prog = Parser.parseScript("function f(g) { { function g() {} } }", "t.js");
        Check.isFalse(prog.list[0].b.fn.body.list[0].list[0].flag, "no hoisting for a parameter name");
        prog = Parser.parseScript("'use strict'; { function g() {} }", "t.js");
        Check.isFalse(prog.list[1].list[0].flag, "no hoisting in strict mode");
        Check.equal("", join(prog.scope.varNames), "and no var");

        prog = Parser.parseScript("try {} catch (e) { let x; }", "t.js");
        Check.equal("e,x", join(prog.list[0].scope.lexNames), "catch scope holds the parameter and the block's lexical names");

        prog = Parser.parseScript("for (let i = 0, j = 1;;) {} for (const k of a) {}", "t.js");
        Check.equal("i,j", join(prog.list[0].scope.lexNames), "for-let scope");
        Check.equal("k", join(prog.list[1].scope.lexNames), "for-of const scope");

        prog = Parser.parseScript("switch (a) { case 1: let z; }", "t.js");
        Check.equal("z", join(prog.list[0].scope.lexNames), "switch scope");
    }

    /**
     * Direct eval detection and the directive prologue.
     * <p>
     * eval 직접 호출 감지와 지시문 서문.
     */
    public void testDirectEvalAndDirectives() {
        Node prog = Parser.parseScript("function f() { eval('1'); } function g() { (0, eval)('1'); }", "t.js");
        Check.isTrue(prog.list[0].b.fn.hasDirectEval, "direct eval is detected");
        Check.isFalse(prog.list[1].b.fn.hasDirectEval, "indirect eval is not a direct eval");
        Check.isTrue(prog.scope.hasDirectEval, "the program scope is marked too");

        ProgramInfo pi = (ProgramInfo) Parser.parseScript("'use strict'; var x;", "t.js").value;
        Check.isTrue(pi.strict, "program level directive");
        pi = (ProgramInfo) Parser.parseScript("\"use strict\"\nvar x", "t.js").value;
        Check.isTrue(pi.strict, "double quoted directive without a semicolon");
        pi = (ProgramInfo) Parser.parseScript("var x; 'use strict';", "t.js").value;
        Check.isFalse(pi.strict, "not in the prologue");
        pi = (ProgramInfo) Parser.parseScript("('use strict'); with (a) {}", "t.js").value;
        Check.isFalse(pi.strict, "a parenthesized string is not a directive");
        pi = (ProgramInfo) Parser.parseScript("'use\\x20strict'; with (a) {}", "t.js").value;
        Check.isFalse(pi.strict, "a directive with an escape is not 'use strict'");
        pi = (ProgramInfo) Parser.parseScript("'use strict' + 1; with (a) {}", "t.js").value;
        Check.isFalse(pi.strict, "an expression is not a directive");
        Node f = Parser.parseScript("function f() { 'a'; 'use strict'; return this; }", "t.js").list[0];
        Check.isTrue(f.b.fn.strict, "a directive after another directive");
        Check.isFalse(((ProgramInfo) Parser.parseScript("function f() { 'use strict'; }", "t.js").value).strict, "strictness stays inside the function");
    }

    /**
     * A yield expression marks all nodes on its path; nested functions are not affected.
     * <p>
     * yield 식은 경로상의 모든 노드를 표시하며 중첩 함수에는 영향을 주지 않는다.
     */
    public void testYieldMarking() {
        Node prog = Parser.parseScript("function* g() { var x = 1 + (yield 2); foo(); function h() { return 1; } }\nfunction n() { return 1 + 2; }", "t.js");
        Node body = prog.list[0].b.fn.body;
        Check.isTrue(body.hasYield, "the body contains a yield");
        Check.isTrue(body.list[0].hasYield, "the declaration with the yield");
        Check.isTrue(body.list[0].list[0].b.hasYield, "the binary expression on the path");
        Check.isFalse(body.list[1].hasYield, "a statement without yield");
        Check.isFalse(body.list[2].hasYield, "a nested function is not marked");
        Check.isFalse(prog.list[1].b.fn.body.hasYield, "a normal function has no yield");
        Check.isFalse(prog.hasYield, "the program does not contain the generator's yield");
    }

    /**
     * Module parsing: import and export tables, strictness and module-level scope.
     * <p>
     * 모듈 파싱: import/export 표, 엄격 여부, 모듈 수준 스코프.
     */
    public void testModuleTables() {
        Node prog = Parser.parseModule("import a, {b as c} from 'm'; import * as ns from 'n'; import 'side';\n"
                + "export var x = 1, y = 2; export function f() {} export class C {} export default function() {}\n"
                + "export {x as z}; export {q} from 'r'; export * from 's';", "mod.js");
        ProgramInfo pi = (ProgramInfo) prog.value;
        Check.isTrue(pi.module && pi.strict, "modules are strict");
        Check.equal("[m, n, side, r, s]", pi.requestedModules.toString(), "requested modules in order");
        StringBuilder imp = new StringBuilder();
        for (ProgramInfo.ImportEntry e : pi.imports) { // import 항목을 문자열로 만든다.
            imp.append(e.moduleRequest).append(':').append(e.importName).append(':').append(e.localName).append(' ');
        }
        Check.equal("m:default:a m:b:c n:*:ns ", imp.toString(), "import entries");
        StringBuilder exp = new StringBuilder();
        for (ProgramInfo.ExportEntry e : pi.localExports) { // export 항목을 문자열로 만든다.
            exp.append(e.exportName).append('=').append(e.localName).append(' ');
        }
        Check.equal("x=x y=y f=f C=C default=*default* z=x ", exp.toString(), "local exports");
        Check.equal("q<r:q", pi.indirectExports.get(0).exportName + "<" + pi.indirectExports.get(0).moduleRequest + ":" + pi.indirectExports.get(0).importName,
                "indirect export");
        Check.equal("s", pi.starExports.get(0).moduleRequest, "star export");
        Check.equal("a,c,ns", join(prog.scope.lexNames).substring(0, 6), "import bindings are lexical constants");
        Check.isTrue(prog.scope.lexConst[0] && prog.scope.lexConst[1] && prog.scope.lexConst[2], "and immutable");
        Check.equal(NodeType.FUNCTION_DECL, prog.list[6].a.type, "export default function declaration");
        Check.equal("*default*", prog.list[6].a.a.name, "anonymous default export binding name");
        Check.equal("default", prog.list[6].a.b.fn.name, "anonymous default export function name");
        Check.equal(1, prog.list[6].op, "default export marker");

        Node dflt = Parser.parseModule("export default 1 + 2;", "m.js");
        Check.equal("(program (export (+ 1 2)))", AstDump.dump(dflt), "default expression export");
        Check.isTrue(dflt.list[0].flag, "marked as an expression");
    }

    /**
     * Eval code options and the Function constructor source.
     * <p>
     * eval 코드 옵션과 Function 생성자 소스.
     */
    public void testEvalAndFunctionConstructor() {
        Parser.EvalOptions o = new Parser.EvalOptions();
        o.allowNewTarget = true;
        Check.equal("(program (expr new.target))", AstDump.dump(Parser.parseEval("new.target", "e", o)), "new.target inside a function's eval");
        Parser.EvalOptions strict = new Parser.EvalOptions();
        strict.strict = true;
        Check.isTrue(((ProgramInfo) Parser.parseEval("var x", "e", strict).value).strict, "strict callers make strict eval code");
        Node fn = Parser.parseFunctionConstructor("a, b", "return a + b", false, "dyn");
        Check.equal(NodeType.FUNCTION, fn.type, "a FUNCTION node");
        Check.equal("function anonymous(a, b\n) {\nreturn a + b\n}", fn.fn.sourceText(), "source text of the new function");
        Check.equal("anonymous", fn.fn.name, "function name");
        Check.equal("(function anonymous (a b) (block (return (+ a b))))", AstDump.dump(fn), "tree");
        Node gen = Parser.parseFunctionConstructor("", "yield 1", true, "dyn");
        Check.isTrue(gen.fn.generator, "generator function constructor");
        Check.isTrue(gen.fn.body.hasYield, "yield is allowed in a generator body");
    }
}
