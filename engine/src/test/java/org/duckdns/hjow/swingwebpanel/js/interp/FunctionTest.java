package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of functions, closures, <code>this</code>, <code>arguments</code>, call/apply/bind, <code>new</code> and prototypes
 * (plan item P13-05).
 * <p>
 * 함수, 클로저, <code>this</code>, <code>arguments</code>, call/apply/bind, <code>new</code>, 프로토타입 시험(계획 항목 P13-05).
 */
public class FunctionTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public FunctionTest() {
    }

    /**
     * All function related behaviors through the assertion prelude.
     * <p>
     * 단언 서문을 통한 함수 관련 모든 동작.
     */
    public void testFunctions() {
        Scripts.checkResource("functions.js", 140);
    }

    /**
     * Functions defined by one script stay callable from the host after the script ends and keep their closure state.
     * <p>
     * 한 스크립트가 정의한 함수는 스크립트가 끝난 뒤에도 호스트에서 호출할 수 있고 클로저 상태를 유지한다.
     */
    public void testFunctionsSurviveTheScript() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("var counter = (function () { var n = 0; return function () { return ++n; }; })();", "a.js");
        Check.equal("1", Ops.toString(r.evaluateScript("counter()", "b.js")), "first call");
        Check.equal("2", Ops.toString(r.evaluateScript("counter()", "c.js")), "state is kept");
    }

    /**
     * Checks the exact source of function and method definitions, including comments and class member boundaries.
     * <p>
     * 주석과 클래스 멤버 경계를 포함한 함수와 메서드 정의의 정확한 원문을 확인한다.
     */
    public void testFunctionToStringSource() {
        String fn = "function /* note */ f ( a ) {\r\n  return a; /* tail */\r\n}";
        String arrow = "(x /* note */) => (x + 1)";
        String async = "async function /* note */ f(){ return 1; }";
        String generator = "function /* note */ *g(){ yield 1; }";
        String method = "['m'] ( ) { /* note */ }";
        String accessor = "get /* note */ x () { return 1; }";
        String cls = "class /* note */ C { constructor /* note */ (){} }";
        String implicit = "class C { static a(){} }";
        String staticMethod = "async * /* note */ m () { yield 1; }";
        Check.equal(fn, Scripts.eval(fn + "\nFunction.prototype.toString.call(f)"), "function declaration");
        Check.equal(arrow, Scripts.eval("var f=(" + arrow + "); f.toString()"), "arrow expression");
        Check.equal(async, Scripts.eval("(" + async + ").toString()"), "async function");
        Check.equal(generator, Scripts.eval("(" + generator + ").toString()"), "generator function");
        Check.equal(method, Scripts.eval("({" + method + "}).m.toString()"), "computed method");
        Check.equal(accessor, Scripts.eval("Object.getOwnPropertyDescriptor({" + accessor + "}, 'x').get.toString()"), "getter");
        Check.equal(cls, Scripts.eval("(" + cls + ").toString()"), "class with explicit constructor");
        Check.equal(implicit, Scripts.eval("(" + implicit + ").toString()"), "class with implicit constructor");
        Check.equal(staticMethod, Scripts.eval("(class { static /* outside */ " + staticMethod + " }).m.toString()"), "static modifier is outside the method");
        Check.equal("function anonymous(a,b /* note */\n) {\n  return a + b;\n}", Scripts.eval(
                "new Function('a', 'b /* note */', '  return a + b;').toString()"), "synthesized dynamic function source");
    }

    /**
     * Checks native placeholders and verifies that a built-in function is not inspected through mutable properties.
     * <p>
     * 네이티브 자리표시자를 검증하고 내장 함수에서 변경 가능한 프로퍼티를 읽지 않는지 확인한다.
     */
    public void testFunctionToStringNative() {
        String empty = "function () { [native code] }";
        Check.equal(empty, Scripts.eval("((a)=>a).bind(null).toString()"), "bound function");
        Check.equal(empty, Scripts.eval("Function.prototype.toString.call(new Proxy(function(){}, {}))"), "callable proxy");
        Check.equal(empty, Scripts.eval("var p=Proxy.revocable(function(){}, {}); p.revoke(); Function.prototype.toString.call(p.proxy)"), "revoked callable proxy");
        Check.equal("function [Symbol.hasInstance]() { [native code] }",
                Scripts.eval("Function.prototype[Symbol.hasInstance].toString()"), "well-known symbol name");
        Check.equal("function get size() { [native code] }", Scripts.eval(
                "Object.getOwnPropertyDescriptor(Map.prototype,'size').get.toString()"), "accessor name");
        Check.equal("function isArray() { [native code] }", Scripts.eval("var f=Array.isArray;"
                + " Object.defineProperty(f,'name',{get:function(){throw new Error('name read')}}); f.toString()"),
                "initial name remains available with no getter call");
    }
}
