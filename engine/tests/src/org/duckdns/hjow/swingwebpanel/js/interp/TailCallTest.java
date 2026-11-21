package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of proper tail calls in strict mode code (ECMA-262 6th edition 14.6): deep recursion in tail position does not hit the
 * call depth limit, other positions keep it, and the call semantics stay unchanged.
 * <p>
 * 엄격 모드 코드의 꼬리 호출 시험(ECMA-262 6판 14.6): 꼬리 위치의 깊은 재귀는 호출 깊이 제한에 걸리지 않고, 다른 위치는 제한을 유지하며,
 * 호출 의미는 바뀌지 않는다.
 */
public class TailCallTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public TailCallTest() {
    }

    /**
     * Tail positions, call semantics and the positions that must not be tail calls.
     * <p>
     * 꼬리 위치, 호출 의미, 꼬리 호출이면 안 되는 위치.
     */
    public void testTailCalls() {
        Scripts.checkResource("tailcalls.js", 50);
    }

    /**
     * An endless tail recursion never grows the stack but is still stopped by the step limit and by cancellation.
     * <p>
     * 끝없는 꼬리 재귀는 스택을 키우지 않지만 단계 수 제한과 취소로 여전히 멈춘다.
     */
    public void testEndlessTailRecursionIsStopped() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(200000L);
        try {
            r.evaluateScript("function f() { 'use strict'; return f(); } f();", "a.js");
            Check.fail("endless tail recursion must be aborted");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, e.getReason(), "step limit reason");
        }
        r.getLimits().setMaxSteps(0L);
        r.getLimits().cancel();
        try {
            r.evaluateScript("function g() { 'use strict'; return g(); } g();", "b.js");
            Check.fail("cancellation must stop endless tail recursion");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.CANCELLED, e.getReason(), "cancel reason");
        }
        r.getLimits().reset();
        Check.equal("1", Ops.toString(r.evaluateScript("1", "c.js")), "usable afterwards");
    }

    /**
     * A million tail calls finish in reasonable time and the call depth counter returns to zero afterwards.
     * <p>
     * 백만 번의 꼬리 호출이 적당한 시간에 끝나며 이후 호출 깊이 카운터가 0 으로 돌아온다.
     */
    public void testMillionTailCalls() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        long start = System.currentTimeMillis();
        Check.equal("done", Ops.toString(r.evaluateScript("function f(n) { 'use strict'; return n === 0 ? 'done' : f(n - 1); } f(1000000)", "a.js")), "one million tail calls");
        Check.isTrue(System.currentTimeMillis() - start < 60000L, "finished in reasonable time");
        Check.equal("RangeError", Ops.toString(r.evaluateScript("function d(n) { return n == 0 ? 0 : 1 + d(n - 1); } try { d(100000); } catch (e) { e.name; }", "b.js")), "the depth limit still works");
        Check.equal("390", Ops.toString(r.evaluateScript("d(390)", "c.js")), "the depth counter was restored");
    }
}
