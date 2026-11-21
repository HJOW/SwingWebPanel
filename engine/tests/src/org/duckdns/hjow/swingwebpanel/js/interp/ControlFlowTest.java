package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of statements and completion records (plan item P13-04).
 * <p>
 * 문장과 완료 레코드 시험(계획 항목 P13-04).
 */
public class ControlFlowTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ControlFlowTest() {
    }

    /**
     * Every statement kind and the completion value rules.
     * <p>
     * 모든 문장 종류와 완료 값 규칙.
     */
    public void testStatements() {
        Scripts.checkResource("control.js", 100);
    }

    /**
     * Uncaught throw of any value surfaces to the host as an exception that carries the thrown value.
     * <p>
     * 잡히지 않은 throw 는 던진 값을 담은 예외로 호스트에 전달된다.
     */
    public void testUncaughtThrowReachesHost() {
        Check.equal("Error: boom", Scripts.thrown("throw new Error('boom')"), "error object text");
        Check.contains(Scripts.thrown("throw 5"), "5", "primitive thrown value text");
        Check.contains(Scripts.thrown("throw 'text'"), "text", "string thrown value text");
        Check.contains(Scripts.thrown("function f() { f2(); } function f2() { throw new RangeError('r'); } f()"), "RangeError: r", "nested throw");
    }
}
