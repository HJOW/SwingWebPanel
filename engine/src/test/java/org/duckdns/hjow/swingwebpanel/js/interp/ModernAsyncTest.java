package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Execution tests for async functions and await (plan item P28-03-c).
 * <p>
 * async 함수와 await 실행 시험(계획 P28-03-c).
 */
public class ModernAsyncTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModernAsyncTest() {
    }

    /**
     * Checks synchronous prefix execution, Promise return and fulfillment.
     * <p>
     * 동기 접두 실행, Promise 반환, 이행을 검사한다.
     */
    public void testBasicAsync() {
        Scripts.checkInline(
                "var log = ''; async function f(){ log += 'b'; await 0; log += 'c'; return 3; }"
                + "var p = (log += 'a', f()); log += 'd';"
                + "t('promise', p instanceof Promise, true); t('sync prefix', log, 'abd');"
                + "p.then(function(v){ t('value', v, 3); t('after await', log, 'abdc'); });",
                4);
    }

    /**
     * Checks rejection, try/catch and throw conversion into the returned Promise.
     * <p>
     * 거부, try/catch, throw 가 반환 Promise 로 변환되는지 검사한다.
     */
    public void testAsyncErrors() {
        Scripts.checkInline(
                "async function throws(){ throw new RangeError('x'); }"
                + "throws().then(function(){ t('unreached', 0, 1); }, function(e){ t('range', e.name, 'RangeError'); });"
                + "async function catches(){ try { await Promise.reject('no'); } catch(e) { return e + '!'; } }"
                + "catches().then(function(v){ t('caught', v, 'no!'); });"
                + "async function finallyRun(){ try { await 1; return 4; } finally { globalThis.finished = true; } }"
                + "finallyRun().then(function(v){ t('return', v, 4); t('finally', globalThis.finished, true); });",
                4);
    }

    /**
     * Checks ES2022 await Promise-resolution and microtask order.
     * <p>
     * ES2022 await 의 Promise 해결과 마이크로태스크 순서를 검사한다.
     */
    public void testAwaitOrder() {
        Scripts.checkInline(
                "var order = ''; Promise.resolve().then(function(){ order += '1'; });"
                + "async function f(){ await 0; order += 'a'; } f();"
                + "Promise.resolve().then(function(){ order += '2'; });"
                + "Promise.resolve().then(function(){ t('order', order, '1a2'); });"
                + "var thenable = { then: function(resolve){ order += 't'; resolve(9); } };"
                + "async function use(){ var v = await thenable; return v; }"
                + "use().then(function(v){ t('thenable', v, 9); t('thenable order', order, '1a2t'); });",
                3);
    }

    /**
     * Checks async arrows, methods, promise adoption and constructor prohibition.
     * <p>
     * async 화살표, 메서드, Promise 채택, 생성자 금지를 검사한다.
     */
    public void testAsyncFunctionsAndMethods() {
        Scripts.checkInline(
                "var arrow = async x => await Promise.resolve(x + 1);"
                + "arrow(4).then(function(v){ t('arrow', v, 5); });"
                + "var o = { async method(v){ return await v; } };"
                + "o.method(7).then(function(v){ t('method', v, 7); });"
                + "async function adopt(){ return Promise.resolve(11); }"
                + "adopt().then(function(v){ t('adopt', v, 11); });"
                + "async function AsyncCtor(){}"
                + "terr('new async', function(){ new AsyncCtor(); }, 'TypeError');",
                4);
    }

    /**
     * Checks await in a binary/unary position and nested rejection recovery.
     * <p>
     * 이항·단항 위치의 await 와 중첩 거부 회복을 검사한다.
     */
    public void testAwaitExpressions() {
        Scripts.checkInline(
                "async function f(a){ return (a || await Promise.resolve(3)) + await 5; }"
                + "f(0).then(function(v){ t('unary and binary await', v, 8); });"
                + "async function nested(){ try { await (async function(){ throw 'x'; })(); }"
                + "catch(e){ return await Promise.resolve(e + '!'); } }"
                + "nested().then(function(v){ t('nested', v, 'x!'); });",
                2);
    }

    /**
     * Checks that cancellation and step limits apply again after await resumes.
     * <p>
     * await 재개 뒤에도 취소와 단계 제한이 다시 적용되는지 검사한다.
     */
    public void testLimitsAfterAwait() {
        JsRealm realm = Scripts.realm(new Scripts.Capture());
        realm.getLimits().setMaxSteps(100);
        realm.evaluateScript("async function f(){ await 0; for (;;) {} } f();", "limit.js");
        try {
            realm.runJobs();
            Check.fail("resumed async function did not hit the step limit");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, e.getReason(), "await continuation step limit");
        }
    }
}

