package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of dynamic code: <code>eval</code>, the <code>Function</code> constructor and strict mode scoping (plan item P13-11).
 * <p>
 * 동적 코드 시험: <code>eval</code>, <code>Function</code> 생성자, 엄격 모드 스코프 규칙(계획 항목 P13-11).
 */
public class DynamicCodeTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public DynamicCodeTest() {
    }

    /**
     * Direct and indirect eval, the Function and GeneratorFunction constructors and strict mode rules.
     * <p>
     * 직접/간접 eval, Function 과 GeneratorFunction 생성자, 엄격 모드 규칙.
     */
    public void testDynamicCode() {
        Scripts.checkResource("dynamic.js", 130);
    }

    /**
     * Dynamic code uses the same limits as normal code: an endless loop created by eval or Function is aborted.
     * <p>
     * 동적 코드도 일반 코드와 같은 제한을 쓴다: eval 이나 Function 으로 만든 무한 루프는 중단된다.
     */
    public void testDynamicCodeIsLimited() {
        String[] endless = { "eval('while (true) { }')", "new Function('for (;;) { }')()", "(0, eval)('for (;;) { }')",
            "var f = new Function('f', 'f(f)'); f(f)" };
        // 동적으로 만든 무한 반복이나 재귀가 제한에 걸리는지 확인한다.
        for (int i = 0; i < endless.length; i++) {
            JsRealm r = Scripts.realm(new Scripts.Capture());
            r.getLimits().setMaxSteps(300000L);
            try {
                r.evaluateScript(endless[i], "t.js");
                Check.fail("not aborted: " + endless[i]);
            } catch (ScriptAbortedException e) {
                Check.isTrue(true, "aborted");
            } catch (org.duckdns.hjow.swingwebpanel.js.JsException e) {
                Check.contains(e.getMessage(), "RangeError", "recursion is reported as a RangeError: " + endless[i]);
            }
        }
    }

    /**
     * Eval and Function run only script code: they cannot reach Java objects, classes or the file system.
     * <p>
     * eval 과 Function 은 스크립트 코드만 실행하며 Java 객체, 클래스, 파일 시스템에 접근할 수 없다.
     */
    public void testNoJavaBridge() {
        String[] probes = { "typeof java", "typeof Packages", "typeof JavaImporter", "typeof importPackage", "typeof System", "typeof Runtime",
            "typeof require", "typeof process", "typeof Java", "typeof load", "typeof readFile", "typeof exit" };
        // 흔한 Java 연결 이름이 전역에 없는지 확인한다.
        for (int i = 0; i < probes.length; i++) {
            Check.equal("undefined", Scripts.eval(probes[i]), probes[i]);
            Check.equal("undefined", Scripts.eval("eval(" + quote(probes[i]) + ")"), "eval " + probes[i]);
            Check.equal("undefined", Scripts.eval("new Function('return " + probes[i] + "')()"), "Function " + probes[i]);
        }
        Check.equal("undefined", Scripts.eval("({}).constructor.constructor('return typeof java')()"), "the constructor chain stays inside the engine");
    }

    /**
     * Quotes text as a JavaScript string literal.
     * <p>
     * 텍스트를 JavaScript 문자열 리터럴로 만든다.
     *
     * @param s the text / 텍스트
     * @return the literal / 리터럴
     */
    private static String quote(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /**
     * Results of dynamic code seen from Java.
     * <p>
     * Java 에서 본 동적 코드의 결과.
     */
    public void testResultsFromJava() {
        Check.equal("3", Scripts.eval("eval('1 + 2')"), "eval");
        Check.equal("5", Scripts.eval("new Function('a', 'return a + 2')(3)"), "Function");
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("var made = new Function('return 7');", "a.js");
        Check.equal("7", Ops.toString(r.evaluateScript("made()", "b.js")), "functions made earlier stay callable");
        Check.contains(Scripts.thrown("eval('var x = ;')"), "SyntaxError", "syntax error text");
        Check.contains(Scripts.thrown("new Function('return +')"), "SyntaxError", "Function syntax error text");
    }
}
