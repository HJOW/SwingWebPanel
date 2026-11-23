package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NumberText;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script-level tests: JavaScript source text is run by the engine's own interpreter in a realm that has only the core library installed,
 * and the string form of the result is compared with the expected text. Each case is "source" and "expected" and runs in a fresh realm;
 * all mismatches of a test method are reported together. The scripts use the helper <code>J(array)</code> (a comma join written in
 * script) and template literals instead of <code>String(...)</code> and <code>Array.prototype.join</code>, because those belong to other
 * built-in libraries that are not part of this realm. Together with the Java-level tests this covers the plan items P14-01 and P14-09 for
 * the fundamental objects through real scripts.
 * <p>
 * 스크립트 수준 시험: 핵심 라이브러리만 설치한 realm 에서 엔진 자체의 인터프리터로 JavaScript 소스를 실행하고 결과의 문자열 형태를 기대한 문자열과
 * 비교한다. 사례는 "소스"와 "기대 값"이며 각각 새 realm 에서 실행하고, 한 시험 메서드의 불일치는 모아서 한 번에 보고한다. 스크립트는
 * 이 realm 에 없는 다른 내장 라이브러리(<code>String(...)</code>, <code>Array.prototype.join</code>)를 쓰지 않고 스크립트로 쓴 쉼표 연결
 * 도우미 <code>J(array)</code> 와 템플릿 리터럴을 쓴다. Java 수준 시험과 함께 실제 스크립트로 기본 객체에 대한 계획 P14-01 과 P14-09 를 다룬다.
 */
public class ScriptLevelTest {

    /**
     * Script run before every case: the comma-join helper.
     * <p>
     * 사례마다 먼저 실행하는 스크립트: 쉼표 연결 도우미.
     */
    private static final String PRELUDE = "function J(a) { var s = ''; for (var i = 0; i < a.length; i++) { if (i > 0) { s += ','; } s += a[i]; } return s; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScriptLevelTest() {
    }

    /**
     * Runs all cases (pairs of source and expected text) and fails once with every mismatch.
     * <p>
     * 모든 사례(소스와 기대 문자열 쌍)를 실행하고 불일치를 모아 한 번에 실패시킨다.
     *
     * @param cases source/expected pairs, flattened / 소스와 기대 값의 쌍(평탄화)
     */
    private void run(String... cases) {
        List<String> failures = new ArrayList<String>();
        for (int i = 0; i + 1 < cases.length; i += 2) { // 사례마다 새 realm 에서 실행해 결과 문자열이 기대와 같은지 비교한다.
            String src = cases[i];
            String expected = cases[i + 1];
            String actual;
            LibEnv fresh = new LibEnv();
            try {
                fresh.realm.evaluateScript(PRELUDE, "prelude");
                JsValue v = fresh.realm.evaluateScript(src, "case" + (i / 2));
                actual = Ops.toString(v);
            } catch (JsException e) {
                actual = "THROWS " + describe(fresh, e);
            } catch (RuntimeException e) {
                actual = "JAVA " + e;
            }
            if (!expected.equals(actual)) {
                failures.add("[" + src + "] expected <" + expected + "> but was <" + actual + ">");
            }
        }
        Check.isTrue(failures.isEmpty(), failures.size() + " script case(s) failed:\n  " + join(failures));
    }

    /**
     * Describes a thrown JavaScript error as "Name: message".
     * <p>
     * 던져진 JavaScript 오류를 "이름: 메시지" 로 설명한다.
     *
     * @param env the environment that threw / 던진 환경
     * @param e   the exception / 예외
     * @return the text / 문자열
     */
    private static String describe(LibEnv env, JsException e) {
        try {
            return Ops.toString(env.invoke(e.getValue(env.realm), "toString"));
        } catch (RuntimeException ex) {
            return String.valueOf(e);
        }
    }

    /**
     * Joins failure lines.
     * <p>
     * 실패 줄을 이어 붙인다.
     *
     * @param lines the lines / 줄들
     * @return the text / 문자열
     */
    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) { // 줄바꿈과 들여쓰기로 이어 붙인다.
            if (i > 0) {
                sb.append("\n  ");
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    /**
     * Object: enumeration order, descriptors, integrity levels, prototypes and tags through scripts.
     * <p>
     * 스크립트로 확인하는 Object: 열거 순서, 기술자, 무결성 수준, 프로토타입, 태그.
     */
    public void testObjectScripts() {
        run("J(Object.keys({b:1, a:2, 2:3, 1:4}))", "1,2,b,a",
            "var o = {}; Object.defineProperty(o, 'x', {value: 1}); var d = Object.getOwnPropertyDescriptor(o, 'x'); J([d.value, d.writable, d.enumerable, d.configurable])", "1,false,false,false",
            "var o = {}; Object.defineProperty(o, 'x', {get: function() { return 7; }, configurable: true}); o.x", "7",
            "var o = Object.freeze({a: 1}); o.a = 2; o.a", "1",
            "var o = Object.freeze({a: 1}); (function() { 'use strict'; try { o.a = 2; return 'no'; } catch (e) { return e.name; } })()", "TypeError",
            "var o = Object.seal({a: 1}); delete o.a; o.a", "1",
            "Object.isFrozen(Object.freeze([1, 2]))", "true",
            "J(Object.getOwnPropertyNames('ab'))", "0,1,length",
            "Object.prototype.toString.call(null) + Object.prototype.toString.call([]) + Object.prototype.toString.call(function(){})", "[object Null][object Array][object Function]",
            "Object.prototype.toString.call(new Error('x')) + Object.prototype.toString.call(1) + Object.prototype.toString.call(Symbol())", "[object Error][object Number][object Symbol]",
            "var o = {[Symbol.toStringTag]: 'T'}; `${o}`", "[object T]",
            "var p = {hi: 'there'}; var o = Object.create(p); o.hi + ' ' + Object.getPrototypeOf(o).hi + ' ' + p.isPrototypeOf(o)", "there there true",
            "var o = {}; o.__proto__ = {z: 9}; o.z", "9",
            "({}).__proto__ === Object.prototype", "true",
            "var o = {}; o.__defineGetter__('g', function() { return 3; }); o.g", "3",
            "Object.assign({a: 1}, {b: 2}, null, {c: 3}).c", "3",
            "var s = Symbol('k'); var o = {[s]: 1, z: 2}; Object.getOwnPropertySymbols(o).length + ',' + J(Object.keys(o))", "1,z",
            "Object.is(NaN, NaN) + ',' + Object.is(0, -0)", "true,false",
            "Object.setPrototypeOf({}, null).toString", "undefined",
            "try { Object.defineProperty(1, 'x', {}); } catch (e) { e instanceof TypeError }", "true",
            "Object.entries === undefined && Object.values === undefined", "true");
    }

    /**
     * Function.prototype methods with script functions, constructors and bound functions.
     * <p>
     * 스크립트 함수, 생성자, 바운드 함수에 대한 Function.prototype 메서드.
     */
    public void testFunctionScripts() {
        run("function f(a, b) { return this.x + a + b; } f.call({x: 1}, 2, 3)", "6",
            "function f(a, b) { return this.x + a + b; } f.apply({x: 1}, [2, 3])", "6",
            "function f(a, b, c) { return J([this.x, a, b, c]); } var g = f.bind({x: 9}, 1); g(2, 3)", "9,1,2,3",
            "function f(a, b, c) {} var g = f.bind(null, 1); g.length + ',' + g.name", "2,bound f",
            "function P(x) { this.x = x; } var B = P.bind(null, 5); var p = new B(); p.x + ',' + (p instanceof P) + ',' + (p instanceof B)", "5,true,true",
            "function f() { return this; } typeof f.bind(1)()", "object",
            "function f() { 'use strict'; return this; } f.bind(1)()", "1",
            "(function() { return arguments.length; }).apply(null, {length: 3})", "3",
            "(function f(a) { return a; }).toString()", "function f(a) { return a; }",
            "Object.prototype.hasOwnProperty.call((function(){}).bind(), 'prototype')", "false",
            "(function(){}).bind().toString()", "function () { [native code] }",
            "class CA {} class CB {} Function.prototype[Symbol.hasInstance].call(CA, new CA) + ',' + Function.prototype[Symbol.hasInstance].call(CA, new CB)", "true,false",
            "var Even = {[Symbol.hasInstance]: function(n) { return n % 2 === 0; }}; (2 instanceof Even) + ',' + (3 instanceof Even)", "true,false",
            "try { Function.prototype.call.call({}); } catch (e) { e.name }", "TypeError",
            "try { (function(){}).apply(null, 5); } catch (e) { e.name }", "TypeError");
    }

    /**
     * Boolean and Symbol through scripts.
     * <p>
     * 스크립트로 확인하는 Boolean 과 Symbol.
     */
    public void testBooleanAndSymbolScripts() {
        run("Boolean('') + ',' + Boolean('x') + ',' + Boolean(0) + ',' + Boolean([])", "false,true,false,true",
            "typeof new Boolean(false) + ',' + (new Boolean(false) ? 'truthy' : 'falsy')", "object,truthy",
            "new Boolean(true).valueOf() + ',' + new Boolean(0).toString()", "true,false",
            "typeof Symbol() + ',' + typeof Symbol.iterator + ',' + typeof Object(Symbol())", "symbol,symbol,object",
            "Symbol('x').toString() + ',' + Symbol().toString()", "Symbol(x),Symbol()",
            "try { Symbol() + ''; } catch (e) { e.name }", "TypeError",
            "try { new Symbol(); } catch (e) { e.name + ':' + e.message }", "TypeError:Symbol is not a constructor",
            "Symbol.for('a') === Symbol.for('a') && Symbol('a') !== Symbol('a')", "true",
            "Symbol.keyFor(Symbol.for('q')) + ',' + Symbol.keyFor(Symbol('q'))", "q,undefined",
            "var s = Symbol('d'); var o = {}; o[s] = 4; o[s] + ',' + Object.keys(o).length + ',' + (s in o)", "4,0,true",
            "Symbol.iterator.toString() + ',' + Symbol.prototype[Symbol.toStringTag]", "Symbol(Symbol.iterator),Symbol",
            "var o = {[Symbol.toPrimitive]: function(hint) { return hint === 'number' ? 42 : 'str'; }}; (+o) + ',' + (o + '') + ',' + `${o}`", "42,str,str",
            "Object(Symbol.iterator) == Symbol.iterator", "true");
    }

    /**
     * The Error family through scripts, including subclassing with class syntax.
     * <p>
     * class 문법의 하위 클래스를 포함해 스크립트로 확인하는 Error 계열.
     */
    public void testErrorScripts() {
        run("try { null.x } catch (e) { (e instanceof TypeError) + ',' + (e instanceof Error) + ',' + e.name }", "true,true,TypeError",
            "try { undefinedVariable } catch (e) { e.name + ',' + (e.constructor === ReferenceError) }", "ReferenceError,true",
            "new RangeError('too big').toString()", "RangeError: too big",
            "var e = new Error('m'); Object.keys(e).length + ',' + e.hasOwnProperty('message') + ',' + Object.getOwnPropertyDescriptor(e, 'message').enumerable", "0,true,false",
            "Error('no new') instanceof Error", "true",
            "class MyError extends Error { constructor(m) { super(m); this.name = 'MyError'; } } var e = new MyError('oops'); (e instanceof MyError) + ',' + (e instanceof Error) + ',' + e.message + ',' + e.toString()", "true,true,oops,MyError: oops",
            "class E2 extends TypeError {} var e = new E2('x'); (e instanceof TypeError) + ',' + Object.prototype.toString.call(e)", "true,[object Error]",
            "Error.prototype.toString.call({name: 'N', message: 'M'})", "N: M",
            "(new URIError).name + ',' + (new EvalError('e')).message + ',' + new SyntaxError('s').name", "URIError,e,SyntaxError",
            "try { decodeURIComponent('%'); } catch (e) { (e instanceof URIError) + ',' + e.name }", "true,URIError",
            "try { (1).toFixed(101); } catch (e) { e instanceof RangeError }", "true",
            "Object.getPrototypeOf(TypeError) === Error && Object.getPrototypeOf(TypeError.prototype) === Error.prototype", "true");
    }

    /**
     * Number and the global functions through scripts.
     * <p>
     * 스크립트로 확인하는 Number 와 전역 함수.
     */
    public void testNumberAndGlobalScripts() {
        run("0.1 + 0.2", "0.30000000000000004",
            "`${1e21}` + ',' + `${1e-7}` + ',' + `${123456789012345680000}`", "1e+21,1e-7,123456789012345680000",
            "(25).toString(2) + ',' + (255).toString(16) + ',' + (-7.5).toString(2)", "11001,ff,-111.1",
            "(1.005).toFixed(2) + ',' + (2.5).toFixed(0) + ',' + (1234.5678).toFixed(1)", "1.00,3,1234.6",
            "(123.456).toPrecision(4) + ',' + (0.00001).toPrecision(1) + ',' + (123456).toExponential(2)", "123.5,0.00001,1.23e+5",
            "(1234567.891).toLocaleString()", "1,234,567.891",
            "Number('  42  ') + ',' + Number('') + ',' + Number('1e3') + ',' + Number('0x1f') + ',' + Number('abc')", "42,0,1000,31,NaN",
            "Number.isInteger(5.0) + ',' + Number.isSafeInteger(Math.pow(2, 53)) + ',' + Number.EPSILON.toString()", "true,false,2.220446049250313e-16",
            "parseInt('08') + ',' + parseInt('0x1F') + ',' + parseInt('12px') + ',' + parseInt('x') + ',' + parseInt('z', 36)", "8,31,12,NaN,35",
            "parseFloat('3.14.15') + ',' + parseFloat('-.5') + ',' + parseFloat('1e3x')", "3.14,-0.5,1000",
            "isNaN('x') + ',' + isNaN('5') + ',' + isFinite('5') + ',' + isFinite(Infinity)", "true,false,true,false",
            "encodeURIComponent('a b/c?d=한') + ',' + decodeURIComponent('%E2%82%AC')", "a%20b%2Fc%3Fd%3D%ED%95%9C,€",
            "encodeURI('http://a/b c#d') + ',' + decodeURI('%23%41')", "http://a/b%20c#d,%23A",
            "typeof NaN + ',' + (NaN !== NaN) + ',' + (1 / Infinity) + ',' + (typeof undefined)", "number,true,0,undefined",
            "Number.parseInt === parseInt && Number.parseFloat === parseFloat", "true",
            "var n = new Number(5); (typeof n) + ',' + (n + 1) + ',' + n.toFixed(1)", "object,6,5.0",
            "Number.MAX_SAFE_INTEGER + ',' + Number.MAX_VALUE", "9007199254740991,1.7976931348623157e+308");
    }

    /**
     * The smallest denormal printed through the engine's own ToString: this needs the shortest round-trip number formatting. It is
     * skipped (with the reason) while the engine core still uses its temporary number formatter, because then it is not a property of the
     * core library; {@link NumberGlobalTest} already verifies the library path.
     * <p>
     * 엔진 자체의 ToString 으로 출력한 가장 작은 비정규 수: 가장 짧은 왕복 숫자 서식이 필요하다. 엔진 코어가 아직 임시 숫자 서식기를 쓰는 동안에는
     * 핵심 라이브러리의 속성이 아니므로 이유와 함께 건너뛴다. 라이브러리 경로는 {@link NumberGlobalTest} 가 이미 확인한다.
     */
    public void testMinValueThroughEngineToString() {
        if (!"5e-324".equals(NumberText.toString(Double.MIN_VALUE))) {
            Check.skip("the engine core's NumberText is still the temporary Double.toString based formatter (Number.MIN_VALUE prints 4.9e-324); "
                    + "the library path js.num is verified by NumberGlobalTest");
        }
        run("`${Number.MIN_VALUE}`", "5e-324");
    }

    /**
     * Math through scripts, including negative zero observed through division.
     * <p>
     * 나눗셈으로 확인하는 음의 0 을 포함해 스크립트로 확인하는 Math.
     */
    public void testMathScripts() {
        run("1 / Math.round(-0.5) + ',' + Math.round(2.5) + ',' + Math.round(-2.5) + ',' + Math.round(0.49999999999999994)", "-Infinity,3,-2,0",
            "Math.max() + ',' + Math.min() + ',' + Math.max(1, '3', 2) + ',' + Math.max(1, NaN)", "-Infinity,Infinity,3,NaN",
            "1 / Math.max(-0, 0) + ',' + 1 / Math.min(0, -0)", "Infinity,-Infinity",
            "Math.hypot(3, 4) + ',' + Math.hypot() + ',' + Math.sign(-5) + ',' + Math.trunc(-4.7) + ',' + Math.cbrt(27)", "5,0,-1,-4,3",
            "Math.pow(2, 10) + ',' + Math.pow(NaN, 0) + ',' + Math.pow(1, Infinity)", "1024,1,NaN",
            "Math.clz32(1) + ',' + Math.imul(0xffffffff, 5) + ',' + Math.fround(5.5) + ',' + Math.log2(8) + ',' + Math.log10(1000)", "31,-5,5.5,3,3",
            "Math.atan2(1, 1) === Math.PI / 4", "true",
            "Math.asinh(1).toFixed(6) + ',' + Math.acosh(2).toFixed(6) + ',' + Math.atanh(0.5).toFixed(6)", "0.881374,1.316958,0.549306",
            "`${Math}` + ',' + typeof Math + ',' + Object.prototype.toString.call(Math)", "[object Math],object,[object Math]",
            "Math.floor(-0.5) + ',' + Math.ceil(-0.5) + ',' + 1 / Math.ceil(-0.5)", "-1,0,-Infinity",
            "var r = Math.random(); r >= 0 && r < 1", "true");
    }
}
