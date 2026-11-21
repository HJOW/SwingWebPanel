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
}
