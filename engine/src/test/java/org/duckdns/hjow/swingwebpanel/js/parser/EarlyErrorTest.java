package org.duckdns.hjow.swingwebpanel.js.parser;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the early errors (plan item P12-08): redeclarations, strict mode restrictions, labels and jumps, classes,
 * generators, accessors, parameters, destructuring, the Script versus Module goals, and syntax that is not part of ES2015
 * (which must be reported as a SyntaxError rather than ignored). A second list contains sloppy-mode code that must be accepted.
 * <p>
 * 조기 오류 시험(계획 P12-08): 재선언, 엄격 모드 제한, 레이블과 점프, 클래스, 제너레이터, 접근자, 매개변수, 구조 분해,
 * Script 와 Module 목표의 차이, ES2015 에 없는 문법(무시하지 않고 SyntaxError 로 보고해야 함). 두 번째 목록은 느슨한 모드에서
 * 받아들여야 하는 코드이다.
 */
public class EarlyErrorTest {

    /**
     * Scripts that must be rejected with a SyntaxError.
     * <p>
     * SyntaxError 로 거부해야 하는 스크립트.
     */
    private static final String[] SCRIPT_ERRORS = {
        // 재선언
        "let a; let a;", "let a; var a;", "var a; let a;", "{ let a; var a; }", "{ var a; let a; }", "{ { var a; } let a; }",
        "function f(a) { let a; }", "function f() { let a; { var a; } }", "for (const x of y) { var x; }",
        "try {} catch (e) { let e; }", "try {} catch ([e]) { var e; }", "let let = 1", "const let = 1", "class A {} class A {}",
        "let a; function a() {}", "function f() {} let f;", "{ function f() {} let f; }", "'use strict'; { function f() {} function f() {} }",
        // 초기값
        "const a;", "let [a];", "var {a};", "const {a};",
        // 엄격 모드
        "function f(a, a) { 'use strict'; }", "'use strict'; function f(a, a) {}", "function f() { 'use strict'; var eval; }",
        "function eval() { 'use strict'; }", "'use strict'; var yield;", "'use strict'; with (a) {}", "'use strict'; 010",
        "'use strict'; '\\01'", "function f() { '\\01'; 'use strict'; }", "'use strict'; delete x;", "'use strict'; eval = 1;",
        "'use strict'; arguments++;", "'use strict'; ({eval} = x);", "'use strict'; function arguments() {}",
        "'use strict'; var let;", "'use strict'; var static;", "function f(a = 1) { 'use strict'; }", "(a = 1) => { 'use strict'; }",
        "function f(a, a) { 'use strict'; }", "'use strict'; if (1) function f() {}",
        // 레이블과 점프
        "break;", "continue;", "a: a: ;", "while (1) { break b; }", "a: { continue a; }", "return;", "function f() {} return",
        "x: while (1) { function f() { break x; } }", "while (1) { function f() { break; } }", "a: { break b; }",
        // 클래스와 super
        "class A { constructor(){} constructor(){} }", "class A { get constructor(){} }", "class A { *constructor(){} }",
        "class A { static prototype(){} }", "class A extends B { m() { super(); } }", "class A { constructor() { super(); } }",
        "function f() { super.x; }", "({ a() { super(); } })", "class A extends B { constructor() { function f() { super(); } } }",
        "class A extends B { constructor() { (function() { super.x; }); } }", "new.target", "x = () => new.target",
        // 제너레이터와 yield
        "function* g() { var yield; }", "function* g(a = yield) {}", "function* g() { function h() { yield 1; } }",
        "function* yield() { 'use strict'; }", "(function* yield() {})",
        // 접근자와 매개변수
        "({ get a(x) {} })", "({ set a() {} })", "({ set a(...x) {} })", "function f(...a, b) {}", "function f(...a = []) {}",
        "(a, a) => 1", "function f(a = 1, a) {}", "(a, ...b, c) => 1", "({a, a}) => 1",
        // 구조 분해
        "({a = 1})", "x = {__proto__: 1, __proto__: 2}", "[...a, b] = c", "[...a,] = b", "[...a = 1] = c", "([a]) = 1", "({a}) = 1",
        "({a: 1} = 1)", "[a + 1] = b", "for ([a + 1] of b);", "for (let x = 1 of y);", "for (var x = 1 of y);", "for (var i, j of x);",
        // 대입 대상
        "1 = 2", "'a' = 1", "this = 1", "a + b = c", "++1", "(a, b) = 1", "[a, b]++", "f() = 1", "typeof a = 1", "a++ = 1",
        // 문장 형태
        "if (1) class C {}", "if (1) let [a] = b;", "switch (a) { default: default: }", "try {}", "throw\n1", "do x; while (1) y; )",
        "for (;)", "for (a;b)", "label: class A {}", "while (1) let [a] = 1;",
        // 템플릿과 리터럴
        "`\\u{zz}`", "`abc", "`a${b", "'abc", "/abc", "/a/gg", "0x", "3in []", "'\\u{110000}'",
        // 예약어
        "var if", "var class = 1", "var enum", "var null", "var true", "var \\u0069f", "\\u0069f (a) b", "({ if })", "({ if = 1 } = x)",
        // ES2015 에 없는 문법: 무시하지 않고 오류로 보고한다
        "async function f() {}", "x?.y", "a ?? b", "class A { x = 1 }", "class A { #x }", "({...a})",
        "import('x')", "f(a, ...)", "class A { static { } }",
    };

    /**
     * Modules that must be rejected with a SyntaxError.
     * <p>
     * SyntaxError 로 거부해야 하는 모듈.
     */
    private static final String[] MODULE_ERRORS = {
        "export { x };", "export var a; export var a;", "import {a} from 'm'; var a;", "import {a, a} from 'm';",
        "export default 1; export default 2;", "var await;", "with (a) {}", "function f() { with (a) {} }",
        "{ import a from 'm'; }", "function f() { export var a; }", "export default function f() {} export default function g() {}",
        "<!-- x", "010", "import a from 'm'; let a;", "export {a as b, c as b}; var a, c;", "import {if} from 'm';",
        "import from 'm';", "export * as ns from 'm';", "export default;", "let a; export {a as default}; export default 1;",
    };

    /**
     * Sloppy-mode and other code that must be accepted.
     * <p>
     * 느슨한 모드 등 받아들여야 하는 코드.
     */
    private static final String[] VALID_SCRIPTS = {
        "var a; var a;", "function f(){} function f(){}", "function f(a, a) {}", "var yield; yield = 1; var let; let = 2;",
        "label: function f(){}", "if (1) function f(){} else function g(){}", "010; 0o10; 0b1; 0x1; 089;", "with (a) b;",
        "eval = 1; arguments = 2;", "try {} catch (e) { var e; }", "{ function f(){} function f(){} }",
        "x = {__proto__: a, ['__proto__']: b};", "({ __proto__: a, __proto__: b } = c);", "for (var i = 0, n = 2; i < n; i++);",
        "<!-- html comment\nvar a;\n--> another", "(function(){ 'use strict'; })", "a\n++\nb",
        "class A { static static() {} static get get() {} get(){} set(){} }", "({ get: 1, set: 2, static: 3, async: 4, await: 5 })",
        "function f(){ new.target }", "function* g() { yield\n/re/ }", "var \\u0061bc = 1; abc;", "var o = { if: 1, class: 2 }; o.if + o.class;",
        "label1: label2: for(;;) { break label1; }", "function f(a, b,) {}", "if (a) ; else ;", "({} = a); [] = a;",
        "(a, b) => ({}); a => b => c;", "'use strict'; var o = {eval: 1}; o.eval;", "`a${b}c`; tag`\\u{zz}`;",
        "x = a ? b : c ? d : e;", "for (let [a, b] of c);", "for (const {x, y = 2} in obj);", "[a, b] = [b, a];",
        "var f = function* () { var x = yield 1; yield* g(); };", "class A extends (B, C) { constructor() { super(); } m() { super.m(); } }",
        "switch (1) { case 1: let x; break; default: let y; }", "do ; while (0) x",
        "function f() { return\n1 }", "var x = { get a() { return 1 }, set a(v) {} };", "a = b ? c : d => e;",
        "function f() { 'use strict'; return typeof this; }", "(function() { return arguments; })", "x = /[/]\\//.test(y);",
        "var let; let\nx = 1;".replace("let\nx", "x"), "label: { break label; }", "outer: for (;;) { inner: for (;;) { continue outer; } }",
        "0, function(){ with ({}) {} }",
    };

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public EarlyErrorTest() {
    }

    /**
     * Every script in {@link #SCRIPT_ERRORS} must fail with a SyntaxError that has a position.
     * <p>
     * {@link #SCRIPT_ERRORS} 의 모든 스크립트는 위치가 있는 SyntaxError 로 실패해야 한다.
     */
    public void testScriptEarlyErrors() {
        StringBuilder accepted = new StringBuilder();
        // 모든 잘못된 스크립트가 SyntaxError 를 내는지 하나씩 확인한다.
        for (int i = 0; i < SCRIPT_ERRORS.length; i++) {
            String src = SCRIPT_ERRORS[i];
            try {
                Parser.parseScript(src, "t.js");
                accepted.append("\n  accepted: ").append(src.replace("\n", "\\n"));
            } catch (JsException e) {
                if (e.getKind() != ErrorKind.SYNTAX_ERROR || e.getLine() < 1 || e.getColumn() < 1) {
                    accepted.append("\n  wrong error for ").append(src.replace("\n", "\\n")).append(": ").append(e.getMessage());
                }
            }
        }
        Check.equal("", accepted.toString(), "scripts that must be rejected");
    }

    /**
     * Every module in {@link #MODULE_ERRORS} must fail with a SyntaxError.
     * <p>
     * {@link #MODULE_ERRORS} 의 모든 모듈은 SyntaxError 로 실패해야 한다.
     */
    public void testModuleEarlyErrors() {
        StringBuilder accepted = new StringBuilder();
        // 모든 잘못된 모듈이 SyntaxError 를 내는지 하나씩 확인한다.
        for (int i = 0; i < MODULE_ERRORS.length; i++) {
            String src = MODULE_ERRORS[i];
            try {
                Parser.parseModule(src, "m.js");
                accepted.append("\n  accepted: ").append(src.replace("\n", "\\n"));
            } catch (JsException e) {
                if (e.getKind() != ErrorKind.SYNTAX_ERROR) {
                    accepted.append("\n  wrong error for ").append(src).append(": ").append(e.getMessage());
                }
            }
        }
        Check.equal("", accepted.toString(), "modules that must be rejected");
    }

    /**
     * Every script in {@link #VALID_SCRIPTS} must be accepted.
     * <p>
     * {@link #VALID_SCRIPTS} 의 모든 스크립트는 받아들여야 한다.
     */
    public void testValidSloppyCode() {
        StringBuilder rejected = new StringBuilder();
        // 올바른 스크립트가 거부되지 않는지 하나씩 확인한다.
        for (int i = 0; i < VALID_SCRIPTS.length; i++) {
            String src = VALID_SCRIPTS[i];
            try {
                Parser.parseScript(src, "t.js");
            } catch (JsException e) {
                rejected.append("\n  rejected: ").append(src.replace("\n", "\\n")).append(" -> ").append(e.getMessage());
            }
        }
        Check.equal("", rejected.toString(), "scripts that must be accepted");
    }

    /**
     * The same code is accepted as a script but rejected in strict mode or as a module where the specification says so.
     * <p>
     * 같은 코드가 스크립트에서는 받아들여지지만 규격에 따라 엄격 모드나 모듈에서는 거부된다.
     */
    public void testGoalAndStrictDifferences() {
        String[] sloppyOnly = { "with (a) b;", "010;", "var yield;", "function f(a, a) {}", "delete x;", "var let;", "arguments = 1;" };
        // 느슨한 모드에서만 허용되는 코드가 엄격 모드와 모듈에서 거부되는지 확인한다.
        for (int i = 0; i < sloppyOnly.length; i++) {
            Parser.parseScript(sloppyOnly[i], "t.js");
            boolean strictRejected = false;
            try {
                Parser.parseScript("'use strict'; " + sloppyOnly[i], "t.js");
            } catch (JsException e) {
                strictRejected = true;
            }
            boolean moduleRejected = false;
            try {
                Parser.parseModule(sloppyOnly[i], "m.js");
            } catch (JsException e) {
                moduleRejected = true;
            }
            Check.isTrue(strictRejected, "strict mode rejects: " + sloppyOnly[i]);
            Check.isTrue(moduleRejected, "module rejects: " + sloppyOnly[i]);
        }
        Parser.parseModule("import a from 'm'; export default a;", "m.js");
        Check.throwsException(JsException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                Parser.parseScript("import a from 'm';", "t.js");
            }
        }, "import is not valid in a script");
    }

    /**
     * Error messages and positions of typical early errors.
     * <p>
     * 대표적인 조기 오류의 메시지와 위치.
     */
    public void testMessagesAndPositions() {
        JsException e = parseError("function f() {\n  'use strict';\n  var eval;\n}");
        Check.equal("3:7", e.getLine() + ":" + e.getColumn(), "position of eval");
        Check.contains(e.getMessage(), "eval or arguments", "strict message");
        e = parseError("while (1) {\n  break lbl;\n}");
        Check.equal("2:9", e.getLine() + ":" + e.getColumn(), "position of the label");
        Check.contains(e.getMessage(), "Undefined label 'lbl'", "label message");
        e = parseError("class A {\n  constructor() {}\n  constructor() {}\n}");
        Check.contains(e.getMessage(), "only have one constructor", "constructor message");
        Check.equal(3, e.getLine(), "second constructor line");
        e = parseError("function f(a, a) { 'use strict'; }");
        Check.contains(e.getMessage(), "Duplicate parameter name", "duplicate parameter message");
        e = parseError("let a = 1;\n{ var a; }");
        Check.contains(e.getMessage(), "Identifier 'a' has already been declared", "redeclaration message");
        Check.equal(2, e.getLine(), "line of the second declaration");
        e = parseError("return 1");
        Check.contains(e.getMessage(), "Illegal return", "return message");
        e = parseError("var x = 5 +;");
        Check.contains(e.getMessage(), "Unexpected token ';'", "unexpected token message");
    }

    /**
     * Parses a script that must fail and returns the exception.
     * <p>
     * 실패해야 하는 스크립트를 파싱하고 예외를 반환한다.
     *
     * @param src the source / 소스
     * @return the exception / 예외
     */
    private JsException parseError(String src) {
        try {
            Parser.parseScript(src, "t.js");
        } catch (JsException e) {
            return e;
        }
        Check.fail("expected a SyntaxError for: " + src);
        return null;
    }
}
