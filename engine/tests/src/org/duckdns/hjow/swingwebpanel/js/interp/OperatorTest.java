package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of expression evaluation (plan item P13-03): operators, references, evaluation order and getter/setter call counts.
 * <p>
 * 표현식 평가 시험(계획 항목 P13-03): 연산자, 참조, 평가 순서, getter/setter 호출 횟수.
 */
public class OperatorTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public OperatorTest() {
    }

    /**
     * All operator groups through the assertion prelude.
     * <p>
     * 단언 서문을 통한 모든 연산자 묶음.
     */
    public void testOperators() {
        Scripts.checkResource("operators.js", 200);
    }

    /**
     * Completion values and simple results of single expressions evaluated from Java.
     * <p>
     * Java 에서 실행한 단일 표현식의 완료 값과 결과.
     */
    public void testExpressionResultsFromJava() {
        Check.equal("3", Scripts.eval("1 + 2"), "addition");
        Check.equal("ab", Scripts.eval("'a' + 'b'"), "concatenation");
        Check.equal("undefined", Scripts.eval("var x = 1;"), "a declaration has no value");
        Check.equal("5", Scripts.eval("var x = 1; x = 5"), "last expression value");
        Check.equal("NaN", Scripts.eval("0 / 0"), "NaN text");
        Check.equal("-Infinity", Scripts.eval("-1 / 0"), "negative infinity text");
        Check.equal("0", Scripts.eval("-0"), "negative zero prints as 0");
        Check.equal("1e+21", Scripts.eval("1e21"), "large number text");
        Check.equal("0.000001", Scripts.eval("0.000001"), "small number text");
        Check.equal("1e-7", Scripts.eval("0.0000001"), "tiny number text");
    }

    /**
     * Error messages name the failing expression kind (useful diagnostics for page authors).
     * <p>
     * 오류 메시지는 실패한 표현식의 종류를 알려 준다(페이지 작성자를 위한 진단).
     */
    public void testErrorMessages() {
        Check.equal("TypeError: undefined is not a function", Scripts.thrown("var o = {}; o.missing()").replaceAll("o\\.missing", "undefined"), "calling undefined");
        Check.contains(Scripts.thrown("var o = {}; o.missing()"), "not a function", "call message");
        Check.contains(Scripts.thrown("undefined.x"), "undefined", "property of undefined names the value");
        Check.contains(Scripts.thrown("missingVariable"), "missingVariable is not defined", "reference error names the variable");
        Check.contains(Scripts.thrown("null.x = 1"), "TypeError", "assignment to null property");
        Check.contains(Scripts.thrown("var x = 1; x()"), "TypeError", "calling a number");
    }
}
