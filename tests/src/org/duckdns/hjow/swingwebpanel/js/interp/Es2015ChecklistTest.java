package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The ES2015 language checklist (plan item P13-13): one case per syntax production family and per execution semantics area of
 * ECMA-262 6th edition clauses 6 to 15, 18.2.1 (eval) and the language related parts of Annex B. Every case is evaluated in a
 * fresh realm and compared with the expected result text. The clause numbers in the case names are the cross reference between
 * the specification and the tests; the table in <code>docs/progress/js-core.md</code> lists the same areas.
 * <p>
 * ES2015 언어 점검표(계획 항목 P13-13): ECMA-262 6판 6~15절, 18.2.1(eval), 부속서 B 의 언어 관련 부분의 구문 계열과 실행 의미 영역마다
 * 사례를 하나씩 둔다. 모든 사례는 새 realm 에서 실행해 기대 결과 문자열과 비교한다. 사례 이름의 절 번호가 규격과 시험의 대조표이며,
 * <code>docs/progress/js-core.md</code> 의 표가 같은 영역을 나열한다.
 */
public class Es2015ChecklistTest {

    /**
     * The failures collected while running a group of cases.
     * <p>
     * 사례 묶음을 실행하며 모은 실패.
     */
    private final List<String> failures = new ArrayList<String>();

    /**
     * The number of cases run by this suite instance.
     * <p>
     * 이 스위트 인스턴스가 실행한 사례 수.
     */
    private int cases;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public Es2015ChecklistTest() {
    }

    /**
     * Evaluates one case and records a failure when the result text differs.
     * <p>
     * 사례 하나를 실행하고 결과 문자열이 다르면 실패를 기록한다.
     *
     * @param clause   the ECMA-262 6th edition clause / ECMA-262 6판 절 번호
     * @param name     the feature name / 기능 이름
     * @param code     the script; its completion value is converted to a string / 스크립트. 완료 값을 문자열로 바꾼다
     * @param expected the expected string / 기대 문자열
     */
    private void c(String clause, String name, String code, String expected) {
        cases++;
        String actual;
        try {
            actual = Scripts.eval(code);
        } catch (RuntimeException e) {
            actual = "THROWS " + e;
        }
        if (!expected.equals(actual)) {
            failures.add(clause + " " + name + ": expected [" + expected + "] but was [" + actual + "]");
        }
    }

    /**
     * Fails the test with every recorded failure and checks the number of cases.
     * <p>
     * 기록된 모든 실패와 함께 시험을 실패시키고 사례 수를 확인한다.
     *
     * @param minCases the minimum number of cases expected / 기대하는 최소 사례 수
     */
    private void finish(int minCases) {
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder("checklist failures:");
            // 실패한 사례를 줄마다 나열한다.
            for (int i = 0; i < failures.size(); i++) {
                sb.append('\n').append(failures.get(i));
            }
            Check.fail(sb.toString());
        }
        Check.isTrue(cases >= minCases, "only " + cases + " cases ran, expected at least " + minCases);
    }

    /**
     * Clause 11: lexical grammar (literals, identifiers, comments, automatic semicolon insertion).
     * <p>
     * 11절: 어휘 문법(리터럴, 식별자, 주석, 자동 세미콜론 삽입).
     */
    public void testLexicalGrammar() {
        c("11.4", "single line comment", "1 // comment\n + 2", "3");
        c("11.4", "multi line comment", "1 /* c */ + /* \n */ 2", "3");
        c("11.4", "HTML-like open comment (B.1.3)", "var r = 1; <!-- comment\nr", "1");
        c("11.4", "HTML-like close comment at line start (B.1.3)", "var r = 1;\n--> comment\nr", "1");
        c("11.6", "unicode escape in identifier", "var a\\u0062c = 1; abc", "1");
        c("11.6", "code point escape in identifier", "var \\u{61}bc = 2; abc", "2");
        c("11.6", "non ASCII identifier", "var 변수 = 3; 변수", "3");
        c("11.6.2", "contextual keywords as identifiers", "var of = 1, get = 2, set = 3, static = 4, async = 5; of + get + set + static + async", "15");
        c("11.6.2", "keywords as property names", "({ if: 1, for: 2, class: 3 }).class", "3");
        c("11.8.3", "decimal literals", "[0, 1.5, .5, 5., 1e3, 1E-3, 0.1e2].join()", "0,1.5,0.5,5,1000,0.001,10");
        c("11.8.3", "hex literals", "[0xff, 0XFF, 0xAbC].join()", "255,255,2748");
        c("11.8.3", "binary literals (ES2015)", "[0b101, 0B11].join()", "5,3");
        c("11.8.3", "octal literals (ES2015)", "[0o17, 0O7].join()", "15,7");
        c("B.1.1", "legacy octal literals", "[010, 08, 09.5].join()", "8,8,9.5");
        c("11.8.4", "string escapes", "'a\\tb\\nc\\\\d\\'e\\\"f\\x41\\u0042'.length", "13");
        c("11.8.4", "code point escape in strings (ES2015)", "'\\u{1F600}'.length + ',' + '\\u{41}'", "2,A");
        c("11.8.4", "line continuation in strings", "'a\\\nb'", "ab");
        c("B.1.2", "legacy octal escapes in strings", "'\\101\\7'.length", "2");
        c("11.8.6", "template literal", "`a${1 + 1}b`", "a2b");
        c("11.8.6", "template literal with newline", "`a\nb`.length", "3");
        c("11.8.6", "nested templates", "`${`${1}`}`", "1");
        c("11.9", "ASI at end of line", "var a = 1\nvar b = 2\na + b", "3");
        c("11.9", "ASI before closing brace", "(function () { return 1 })()", "1");
        c("11.9", "ASI after return", "(function () { return\n1; })()", "undefined");
        c("11.9", "ASI does not apply before parenthesis", "var f = function () { return 1; }\n;(function () { })\nf()", "1");
        c("11.9", "ASI restricted production postfix", "var a = 1, b = 1\na\n++\nb\na + ',' + b", "1,2");
        c("11.9", "ASI after break (restricted production)", "var r = 0; l: for (;;) { r = 1; break\nl; } r", "1");
        c("11.9", "ASI do-while without semicolon", "var i = 0; do i++; while (i < 3) i", "3");
        c("11.8.5", "regular expression literal needs RegExp support or reports a SyntaxError", "typeof RegExp === 'function' ? typeof /a/g : 'object'", "object");
        c("11.7", "punctuators", "var a = 5; a >>>= 1; a <<= 2; a ^= 1; a |= 2; a &= 7; a %= 4; a", "3");
        finish(25);
    }

    /**
     * Clause 12: expressions.
     * <p>
     * 12절: 표현식.
     */
    public void testExpressions() {
        c("12.2.1", "this at script level", "this === globalThis", "true");
        c("12.2.4", "array literal with holes and spread", "var a = [1, , ...[2, 3]]; a.length + ',' + (1 in a)", "4,false");
        c("12.2.5", "object literal shorthand and computed", "var k = 'x', v = 1; var o = { v, [k + 1]: 2, m() { return 3; } }; o.v + o.x1 + o.m()", "6");
        c("12.2.5", "object literal accessors", "var o = { get a() { return 1; }, set a(v) { this._ = v; } }; o.a = 5; o.a + o._", "6");
        c("12.2.5", "object literal __proto__ (B.3.1)", "Object.getPrototypeOf({ __proto__: Array.prototype }) === Array.prototype", "true");
        c("12.2.6", "function expression", "(function f() { return typeof f; })()", "function");
        c("12.2.7", "generator expression", "var g = function* () { yield 1; }; g().next().value", "1");
        c("12.2.8", "class expression", "(class { static x() { return 1; } }).x()", "1");
        c("12.2.9", "regexp literal evaluates when a RegExp built-in exists", "typeof RegExp === 'function' ? typeof /a/ : 'object'", "object");
        c("12.2.10", "template literals", "var n = 3; `n=${n}`", "n=3");
        c("12.2.11", "parenthesized expression keeps references", "var o = { f() { return this === o; } }; (o.f)()", "true");
        c("12.3.2", "member access dot and bracket", "var o = { a: { b: 1 } }; o.a.b + o['a']['b']", "2");
        c("12.3.3", "new with and without arguments", "function P(x) { this.x = x; } new P(1).x + (new P).x", "NaN");
        c("12.3.4", "function calls with spread", "function f(a, b, c) { return a + b + c; } f(...[1, 2], 3)", "6");
        c("12.3.5", "super property access", "var b = { m() { return 'b'; } }; var o = { __proto__: b, m() { return super.m() + 'o'; } }; o.m()", "bo");
        c("12.3.6", "super call", "class A { constructor() { this.a = 1; } } class B extends A { constructor() { super(); this.b = 2; } } var x = new B(); x.a + x.b", "3");
        c("12.3.7", "new.target", "function F() { return new.target === F; } new F() instanceof F", "true");
        c("12.3.7", "new.target in plain call", "function F() { return new.target; } F()", "undefined");
        c("12.3.8", "tagged templates", "function t(s, v) { return s[0] + '|' + s.raw[0] + '|' + v; } t`a\\n${1}`", "a\n|a\\n|1");
        c("12.4", "postfix and prefix", "var a = 1; (a++ + ++a) + ',' + a", "4,3");
        c("12.5.1", "delete", "var o = { a: 1 }; delete o.a; 'a' in o", "false");
        c("12.5.2", "void", "void 0", "undefined");
        c("12.5.3", "typeof", "typeof Symbol() + typeof null + typeof function () { } + typeof undefined", "symbolobjectfunctionundefined");
        c("12.5.5", "unary plus minus", "+'3' + -'2'", "1");
        c("12.5.7", "bitwise not", "~~3.7", "3");
        c("12.5.8", "logical not", "!!'' + ',' + !!'0'", "false,true");
        c("12.6", "multiplicative", "7 % 3 * 2 / 4", "0.5");
        c("12.7", "additive", "1 + '2' - 1", "11");
        c("12.8", "shifts", "(1 << 3) + (-8 >> 1) + (-1 >>> 31)", "5");
        c("12.9", "relational", "(1 < 2) + ',' + ('b' > 'a') + ',' + ('a' in { a: 1 }) + ',' + ([] instanceof Array)", "true,true,true,true");
        c("12.10", "equality", "(1 == '1') + ',' + (null == undefined) + ',' + (NaN == NaN) + ',' + (1 === 1)", "true,true,false,true");
        c("12.11", "bitwise logic", "(5 & 3) + (5 | 3) + (5 ^ 3)", "14");
        c("12.12", "logical and or", "(0 || 'a') + (1 && 'b')", "ab");
        c("12.13", "conditional", "true ? 'y' : 'n'", "y");
        c("12.14", "assignment operators", "var a = 2; a += 3; a *= 2; a -= 1; a /= 3; a", "3");
        c("12.14.5", "array destructuring assignment", "var a, b; [a, b = 5] = [1]; a + b", "6");
        c("12.14.5", "object destructuring assignment", "var a, b; ({ a, b: [b] } = { a: 1, b: [2] }); a + b", "3");
        c("12.14.5", "destructuring assignment to members", "var o = {}; [o.x, o['y']] = [1, 2]; o.x + o.y", "3");
        c("12.15", "comma operator", "(1, 2, 3)", "3");
        c("14.2", "arrow functions", "var f = (a, b = 2) => a + b; f(1)", "3");
        c("14.2", "arrow lexical this", "var o = { v: 1, f() { return (() => this.v)(); } }; o.f()", "1");
        c("14.4", "yield expression", "function* g() { var x = yield 1; return x; } var i = g(); i.next(); i.next('r').value", "r");
        c("14.4", "yield delegation", "function* a() { yield 1; return 2; } function* b() { var r = yield* a(); yield r; } [...b()].join()", "1,2");
        finish(40);
    }

    /**
     * Clause 13: statements and declarations.
     * <p>
     * 13절: 문장과 선언.
     */
    public void testStatementsAndDeclarations() {
        c("13.1", "block scoped let", "let a = 1; { let a = 2; } a", "1");
        c("13.1", "block scoped function", "{ function f() { return 1; } } typeof f", "function");
        c("13.2.1", "let temporal dead zone", "try { x; let x = 1; } catch (e) { e.name }", "ReferenceError");
        c("13.2.1", "const assignment", "const c = 1; try { c = 2; } catch (e) { e.name }", "TypeError");
        c("13.2.1", "let destructuring", "let [a, { b }] = [1, { b: 2 }]; a + b", "3");
        c("13.3.2", "var hoisting", "var r = typeof h; var h = 1; r", "undefined");
        c("13.3.3", "var destructuring", "var { a, b: [c] } = { a: 1, b: [2] }; a + c", "3");
        c("13.4", "empty statement", ";;; 5", "5");
        c("13.6", "if else", "var r; if (0) r = 'a'; else if (1) r = 'b'; else r = 'c'; r", "b");
        c("13.7.2", "do while", "var i = 0; do { i++; } while (i < 3); i", "3");
        c("13.7.3", "while", "var i = 0; while (i < 3) i++; i", "3");
        c("13.7.4", "for", "var s = 0; for (var i = 0; i < 4; i++) s += i; s", "6");
        c("13.7.4", "for with let closure per iteration", "var f = []; for (let i = 0; i < 3; i++) f.push(() => i); f.map(g => g()).join()", "0,1,2");
        c("13.7.5", "for in", "var k = []; for (var p in { a: 1, b: 2 }) k.push(p); k.join()", "a,b");
        c("13.7.5", "for of", "var s = 0; for (var v of [1, 2, 3]) s += v; s", "6");
        c("13.7.5", "for of with destructuring", "var s = ''; for (var [k, v] of [['a', 1], ['b', 2]]) s += k + v; s", "a1b2");
        c("13.7.5", "for of over iterators with closing", "var closed = 0; var it = { [Symbol.iterator]() { return { next() { return { value: 1, done: false }; }, return() { closed++; return {}; } }; } }; for (var x of it) break; closed", "1");
        c("13.8", "continue", "var s = ''; for (var i = 0; i < 4; i++) { if (i == 1) continue; s += i; } s", "023");
        c("13.9", "break", "var i = 0; while (true) { if (++i == 3) break; } i", "3");
        c("13.10", "return", "(function () { return 1; return 2; })()", "1");
        c("13.11", "with", "var o = { a: 5 }; with (o) { a + 1 }", "6");
        c("13.12", "switch", "switch (2) { case 1: 'a'; break; case 2: 'b'; case 3: 'c'; break; default: 'd'; }", "c");
        c("13.12", "switch with default in the middle", "var r = ''; switch (9) { case 1: r += 1; default: r += 'd'; case 2: r += 2; } r", "d2");
        c("13.13", "labelled statements", "var s = ''; a: for (var i = 0; i < 3; i++) { for (var j = 0; j < 3; j++) { if (j == 1) continue a; s += i + '' + j; } } s", "001020");
        c("13.13", "labelled block break", "var r = 0; b: { r = 1; break b; } r", "1");
        c("B.3.2", "labelled function declaration", "a: function f() { return 1; } f()", "1");
        c("13.14", "throw", "try { throw 5; } catch (e) { e }", "5");
        c("13.15", "try catch finally order", "var s = ''; try { s += 't'; throw 1; } catch (e) { s += 'c'; } finally { s += 'f'; } s", "tcf");
        c("13.15", "finally overrides return", "(function () { try { return 1; } finally { return 2; } })()", "2");
        c("13.15", "catch destructuring", "try { throw { m: 'x' }; } catch ({ m }) { m }", "x");
        c("13.16", "debugger statement is a no-op", "debugger; 1", "1");
        c("13.6", "completion value of if", "1; if (true) { 2; }", "2");
        c("13.6", "completion value of an empty block statement", "3; {}", "3");
        c("13.7.4", "completion value of a loop", "for (var i = 0; i < 3; i++) { i; }", "2");
        finish(30);
    }

    /**
     * Clause 14: functions, generators and classes.
     * <p>
     * 14절: 함수, 제너레이터, 클래스.
     */
    public void testFunctionsGeneratorsAndClasses() {
        c("14.1", "function declaration hoisting", "f(); function f() { return 1; }", "1");
        c("14.1", "default parameters", "(function (a, b = a + 1) { return b; })(1)", "2");
        c("14.1", "default parameter scope", "var x = 'outer'; (function (a = x) { var x = 'inner'; return a; })()", "outer");
        c("14.1", "rest parameters", "(function (a, ...r) { return r.length; })(1, 2, 3)", "2");
        c("14.1", "destructuring parameters", "(function ({ a }, [b]) { return a + b; })({ a: 1 }, [2])", "3");
        c("14.1", "function length and name", "function f(a, b = 1, c) { } f.length + f.name", "1f");
        c("14.1", "strict directive", "(function () { 'use strict'; return this; })()", "undefined");
        c("14.1", "arguments mapping in sloppy mode", "(function (a) { arguments[0] = 2; return a; })(1)", "2");
        c("14.1", "arguments in strict mode is unmapped", "(function (a) { 'use strict'; arguments[0] = 2; return a; })(1)", "1");
        c("14.2", "arrow functions have no own this arguments or prototype", "var f = () => 1; typeof f.prototype", "undefined");
        c("14.2", "arrow functions cannot be constructed", "try { new (() => 1)(); } catch (e) { e.name }", "TypeError");
        c("14.3", "method definitions", "var o = { m() { return 1; }, get g() { return 2; }, *gen() { yield 3; }, ['c' + 1]() { return 4; } }; o.m() + o.g + o.gen().next().value + o.c1()", "10");
        c("14.3", "methods are not constructors", "try { new ({ m() { } }).m(); } catch (e) { e.name }", "TypeError");
        c("14.4", "generator functions", "function* g() { yield 1; yield 2; } [...g()].join()", "1,2");
        c("14.4", "generator return value", "function* g() { yield 1; return 2; } var i = g(); i.next(); var r = i.next(); r.value + ',' + r.done", "2,true");
        c("14.4", "generator throw", "function* g() { try { yield 1; } catch (e) { yield 'c' + e; } } var i = g(); i.next(); i.throw('x').value", "cx");
        c("14.4", "generator return method", "function* g() { try { yield 1; } finally { yield 'f'; } } var i = g(); i.next(); var a = i.return(9); var b = i.next(); a.value + ',' + b.value + ',' + b.done", "f,9,true");
        c("14.4", "generators are not constructors", "function* g() { } try { new g(); } catch (e) { e.name }", "TypeError");
        c("14.5", "class declaration", "class A { m() { return 1; } } new A().m()", "1");
        c("14.5", "class expressions and names", "var C = class { }; C.name", "C");
        c("14.5", "class inheritance and super", "class A { f() { return 'A'; } } class B extends A { f() { return super.f() + 'B'; } } new B().f()", "AB");
        c("14.5", "class static members", "class A { static s() { return 's'; } } class B extends A { } B.s()", "s");
        c("14.5", "class accessors", "class A { get v() { return 1; } set v(x) { this._ = x; } } var a = new A(); a.v = 5; a.v + a._", "6");
        c("14.5", "class computed members", "var n = 'm'; class A { [n + '1']() { return 1; } static [n + '2']() { return 2; } } new A().m1() + A.m2()", "3");
        c("14.5", "class constructor must be called with new", "class A { } try { A(); } catch (e) { e.name }", "TypeError");
        c("14.5", "class declarations are in the TDZ", "try { new A(); class A { } } catch (e) { e.name }", "ReferenceError");
        c("14.5", "extends null", "class A extends null { } Object.getPrototypeOf(A.prototype)", "null");
        c("14.5", "extending built-ins", "class A extends Array { } var a = new A(); a.push(1); a.length + ',' + (a instanceof A) + ',' + Array.isArray(a)", "1,true,true");
        c("14.5", "derived default constructor", "class A { constructor(x) { this.x = x; } } class B extends A { } new B(7).x", "7");
        c("14.5", "this before super", "class A { } class B extends A { constructor() { try { this; } catch (e) { var n = e.name; } super(); this.n = n; } } new B().n", "ReferenceError");
        c("14.5", "class bodies are strict", "class A { m() { return this; } } var m = new A().m; m()", "undefined");
        finish(30);
    }

    /**
     * Clause 15: scripts and modules, and clause 18.2.1: eval.
     * <p>
     * 15절: 스크립트와 모듈, 18.2.1절: eval.
     */
    public void testScriptsAndEval() {
        c("15.1", "script global var is a property of the global object", "var gv = 1; globalThis.gv", "1");
        c("15.1", "script global let is not", "let gl = 1; globalThis.gl", "undefined");
        c("15.1", "script global function is a property", "function gf() { } typeof globalThis.gf", "function");
        c("15.1.8", "global declaration conflicts are early errors", "try { eval('let a; var a;'); } catch (e) { e.name }", "SyntaxError");
        c("18.2.1", "direct eval sees locals", "(function () { var x = 2; return eval('x * 2'); })()", "4");
        c("18.2.1", "indirect eval is global", "var x = 'g'; (function () { var x = 'l'; return (0, eval)('x'); })()", "g");
        c("18.2.1", "eval var leaks to the function", "(function () { eval('var v = 1'); return v; })()", "1");
        c("18.2.1", "strict eval does not leak", "(function () { 'use strict'; eval('var v = 1'); return typeof v; })()", "undefined");
        c("18.2.1", "eval let is local", "(function () { eval('let l = 1'); return typeof l; })()", "undefined");
        c("19.2.1", "Function constructor", "new Function('a', 'b', 'return a + b')(1, 2)", "3");
        c("15.2", "modules are evaluated by the host API (see ModuleTest)", "typeof globalThis.import", "undefined");
        finish(10);
    }

    /**
     * Clauses 6 to 9: types, conversions, environments and the object internal methods as seen from script code.
     * <p>
     * 6~9절: 자료형, 변환, 환경, 객체 내부 메서드를 스크립트 코드 쪽에서 본 모습.
     */
    public void testTypesConversionsAndObjectModel() {
        c("6.1.1", "undefined", "typeof undefined + String(undefined)", "undefinedundefined");
        c("6.1.2", "null", "typeof null + String(null)", "objectnull");
        c("6.1.3", "booleans", "typeof true + (true === !false)", "booleantrue");
        c("6.1.4", "strings are UTF-16", "'\\u{1F600}'.length + ',' + 'abc'[1]", "2,b");
        c("6.1.5", "symbols", "var s = Symbol('d'); typeof s + ',' + (s === s) + ',' + (Symbol('d') === s) + ',' + s.toString()", "symbol,true,false,Symbol(d)");
        c("6.1.5", "well-known symbols", "typeof Symbol.iterator + typeof Symbol.hasInstance + typeof Symbol.toPrimitive + typeof Symbol.toStringTag", "symbolsymbolsymbolsymbol");
        c("6.1.6", "numbers", "(0.1 + 0.2) + ',' + (1 / 0) + ',' + (-1 / 0) + ',' + (0 / 0) + ',' + (1 / -0)", "0.30000000000000004,Infinity,-Infinity,NaN,-Infinity");
        c("6.1.7", "objects and property keys", "var o = {}; o[1] = 'a'; o['1'] += 'b'; o.x = 1; Object.keys(o).join() + o[1]", "1,xab");
        c("6.1.7.2", "property attributes", "var o = {}; Object.defineProperty(o, 'p', { value: 1 }); var d = Object.getOwnPropertyDescriptor(o, 'p'); d.writable + ',' + d.enumerable + ',' + d.configurable", "false,false,false");
        c("6.1.7.3", "property enumeration order", "Object.keys({ b: 1, 2: 1, a: 1, 1: 1 }).join()", "1,2,b,a");
        c("7.1.1", "ToPrimitive with Symbol.toPrimitive", "var o = { [Symbol.toPrimitive](h) { return h; } }; `${o}` + (o + '') + (+{ [Symbol.toPrimitive]() { return 5; } })", "string" + "default5");
        c("7.1.1", "ToPrimitive valueOf then toString", "var o = { valueOf() { return 1; }, toString() { return 's'; } }; (o + 1) + ',' + `${o}`", "2,s");
        c("7.1.3", "ToNumber of strings", "[+'  12  ', +'0x10', +'1e2', +'', +'abc', +'Infinity'].join()", "12,16,100,0,NaN,Infinity");
        c("7.1.12", "ToString of numbers", "[1e21, 1e-7, 123456789012345680000, 0.5, -0].join()", "1e+21,1e-7,123456789012345680000,0.5,0");
        c("7.2.1", "equality algorithms", "[null == 0, null == undefined, '' == 0, '0' == false, [] == '', [0] == false].join()", "false,true,true,true,true,true");
        c("7.2.9", "SameValue through Object.is", "[Object.is(NaN, NaN), Object.is(0, -0), Object.is(1, 1)].join()", "true,false,true");
        c("8.1", "environment chains and closures", "function mk() { var n = 0; return () => ++n; } var a = mk(), b = mk(); a(); a(); b() + ',' + a()", "1,3");
        c("8.1.1.1", "with object environments and unscopables", "var o = { a: 1, b: 2, [Symbol.unscopables]: { a: true } }; var a = 'outer'; with (o) { a + b }", "outer2");
        c("9.1", "prototype chain lookup", "var p = { x: 1 }, o = Object.create(p); o.x + ',' + o.hasOwnProperty('x') + ',' + ('x' in o)", "1,false,true");
        c("9.1.9", "receiver of setters", "var p = { set v(x) { this._v = x; } }, o = Object.create(p); o.v = 3; o.hasOwnProperty('_v') + ',' + p.hasOwnProperty('_v')", "true,false");
        c("9.1.9", "non-writable inherited property blocks assignment", "var p = Object.freeze({ x: 1 }), o = Object.create(p); o.x = 2; o.x", "1");
        c("9.2", "function objects are callable objects with prototype", "function F() { } typeof F.prototype + (F.prototype.constructor === F)", "objecttrue");
        c("9.4.1", "bound function exotic objects", "function f(a, b) { return this.v + a + b; } var g = f.bind({ v: 1 }, 2); g(3) + ',' + g.name + ',' + g.length", "6,bound f,1");
        c("9.4.2", "array exotic objects", "var a = [1, 2, 3]; a.length = 1; a[4] = 5; a.length + ',' + (3 in a)", "5,false");
        c("9.4.3", "string exotic objects", "var s = new String('ab'); s.length + ',' + s[1] + ',' + Object.keys(s).join()", "2,b,0,1");
        c("9.4.4", "arguments exotic objects", "(function (a) { a = 5; return arguments[0]; })(1)", "5");
        c("9.4.6", "module namespace exotic objects (see ModuleTest)", "typeof Object.keys", "function");
        c("9.5", "Proxy requires the built-in library or reports it is absent", "typeof Proxy === 'function' || typeof Proxy === 'undefined'", "true");
        finish(25);
    }

    /**
     * Annex B language features that scripts on the web rely on.
     * <p>
     * 웹 스크립트가 의존하는 부속서 B 언어 기능.
     */
    public void testAnnexB() {
        c("B.1.1", "legacy octal and decimal-like octal", "[017, 018].join()", "15,18");
        c("B.1.2", "legacy octal escapes", "'\\101'", "A");
        c("B.1.3", "HTML-like comments", "var a = 1; <!-- x\na", "1");
        c("B.3.1", "__proto__ in literals", "({ __proto__: { z: 1 } }).z", "1");
        c("B.3.2", "labelled function declarations", "l: function lf() { return 1; } lf()", "1");
        c("B.3.3", "function declarations in blocks (sloppy)", "{ function bf() { return 2; } } bf()", "2");
        c("B.3.3", "function declarations in if statements (sloppy)", "if (true) function ifn() { return 3; } ifn()", "3");
        c("B.3.3", "function declarations in switch cases", "switch (1) { case 1: function sw() { return 4; } } sw()", "4");
        c("B.3.3", "block functions do not escape in strict code", "(function () { 'use strict'; { function sf() { } } return typeof sf; })()", "undefined");
        c("B.3.4", "catch parameter redeclared by var", "try { throw 1; } catch (e) { var e = 2; } typeof e", "undefined");
        c("B.3.5", "__proto__ accessor", "var o = {}; o.__proto__ = Array.prototype; o instanceof Array", "true");
        finish(10);
    }
}
