package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of environments, hoisting and declaration instantiation (plan items P13-01 and P13-02): script resources plus
 * cross-script global declaration rules that need several evaluations in one realm.
 * <p>
 * 환경, 호이스팅, 선언 인스턴스화 시험(계획 항목 P13-01, P13-02): 스크립트 리소스와, 한 realm 에서 여러 번 실행해야 하는 스크립트 간
 * 전역 선언 규칙.
 */
public class ScopeTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ScopeTest() {
    }

    /**
     * var, function, let, const, TDZ, closures, block functions, with and global bindings in one script.
     * <p>
     * var, 함수, let, const, TDZ, 클로저, 블록 함수, with, 전역 바인딩을 한 스크립트에서 시험한다.
     */
    public void testHoistingAndBlockScopes() {
        Scripts.checkResource("scope-hoisting.js", 80);
    }

    /**
     * Global lexical declarations are shared by later scripts and conflicting redeclarations are early errors of the later
     * script (GlobalDeclarationInstantiation).
     * <p>
     * 전역 렉시컬 선언은 이후 스크립트에서 공유되며, 충돌하는 재선언은 나중 스크립트의 조기 오류이다(GlobalDeclarationInstantiation).
     */
    public void testGlobalDeclarationInstantiation() {
        final JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("let shared = 1; const fixed = 2; var viaVar = 3; function viaFn() { return 4; }", "a.js");
        Check.equal("1,2,3,4", Ops.toString(r.evaluateScript("shared + ',' + fixed + ',' + viaVar + ',' + viaFn()", "b.js")), "later script reads earlier lexicals");
        r.evaluateScript("shared = 10;", "c.js");
        Check.equal("10", Ops.toString(r.evaluateScript("shared", "d.js")), "later script assigns earlier let");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                r.evaluateScript("fixed = 5;", "e.js");
            }
        }, "const stays constant across scripts");
        String err = expectError(r, "var shared;");
        Check.contains(err, "SyntaxError", "var conflicts with an earlier let: " + err);
        err = expectError(r, "let viaVar;");
        Check.contains(err, "SyntaxError", "let conflicts with an earlier var: " + err);
        err = expectError(r, "let shared;");
        Check.contains(err, "SyntaxError", "let conflicts with an earlier let: " + err);
        err = expectError(r, "function shared() { }");
        Check.contains(err, "SyntaxError", "function conflicts with an earlier let: " + err);
        r.evaluateScript("var viaVar; function viaFn() { return 5; }", "f.js");
        Check.equal("3,5", Ops.toString(r.evaluateScript("viaVar + ',' + viaFn()", "g.js")), "var redeclaration keeps the value, function replaces");
        err = expectError(r, "let ok1 = 1; let viaVar2 = 1; var ok1;");
        Check.contains(err, "SyntaxError", "conflict inside one script: " + err);
        Check.equal("undefined", Ops.toString(r.evaluateScript("typeof ok1", "h.js")), "a failed script declares nothing");
    }

    /**
     * A global lexical binding shadows a configurable global property and a failed script leaves no half-created bindings.
     * <p>
     * 전역 렉시컬 바인딩은 구성 가능한 전역 프로퍼티를 가리며, 실패한 스크립트는 반쯤 만든 바인딩을 남기지 않는다.
     */
    public void testGlobalLexicalShadowing() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("globalThis.prop = 'object';", "a.js");
        r.evaluateScript("let prop = 'lexical';", "b.js");
        Check.equal("lexical", Ops.toString(r.evaluateScript("prop", "c.js")), "lexical binding wins over the property");
        Check.equal("object", Ops.toString(r.evaluateScript("globalThis.prop", "d.js")), "the property is still there");
        r.evaluateScript("Object.defineProperty(globalThis, 'fixedProp', { value: 1, configurable: false });", "e.js");
        String err = expectError(r, "let fixedProp = 2;");
        Check.contains(err, "SyntaxError", "lexical declaration over a non-configurable property: " + err);
    }

    /**
     * Function declarations at the top level must be definable on the global object.
     * <p>
     * 최상위의 함수 선언은 전역 객체에 정의할 수 있어야 한다.
     */
    public void testGlobalFunctionRules() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("Object.defineProperty(globalThis, 'ro', { value: 1, writable: false, enumerable: false, configurable: false });", "a.js");
        String err = expectError(r, "function ro() { }");
        Check.contains(err, "TypeError", "cannot redefine a read-only global as a function: " + err);
        r.evaluateScript("globalThis.cfg = 1;", "b.js");
        r.evaluateScript("function cfg() { return 'fn'; }", "c.js");
        Check.equal("function", Ops.toString(r.evaluateScript("typeof cfg", "d.js")), "configurable global property is replaced");
    }

    /**
     * Evaluates source expecting a script exception and returns its description.
     * <p>
     * 스크립트 예외를 기대하며 소스를 실행하고 그 설명을 반환한다.
     *
     * @param r      the realm / realm
     * @param source the source / 소스
     * @return the exception text / 예외 문자열
     */
    static String expectError(JsRealm r, String source) {
        try {
            JsValue v = r.evaluateScript(source, "x.js");
            Check.fail("no error for " + source + ", value " + Ops.toString(v));
        } catch (JsException e) {
            return e.getMessage();
        }
        return null;
    }
}
