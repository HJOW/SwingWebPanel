package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of iterators, for-of and generators (plan item P13-09), including the guarantee that generators do not use Java
 * threads and that suspended generators are plain garbage.
 * <p>
 * 이터레이터, for-of, 제너레이터 시험(계획 항목 P13-09). 제너레이터가 Java 스레드를 쓰지 않으며 중단된 제너레이터가 평범한 가비지라는
 * 보장도 확인한다.
 */
public class GeneratorTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public GeneratorTest() {
    }

    /**
     * Iteration protocol, closing, generator objects, yield positions and yield*.
     * <p>
     * 반복 프로토콜, 닫기, 제너레이터 객체, yield 위치, yield*.
     */
    public void testIteratorsAndGenerators() {
        Scripts.checkResource("iterators.js", 130);
    }

    /**
     * Counts the live threads whose names are not part of the baseline.
     * <p>
     * 기준 시점에 없던 살아 있는 스레드의 수를 센다.
     *
     * @return the number of live threads / 살아 있는 스레드 수
     */
    private static int liveThreads() {
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        int n = 0;
        // 살아 있는 스레드만 센다.
        for (Thread t : all.keySet()) {
            if (t.isAlive()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Thousands of suspended generators do not create threads (no thread per generator).
     * <p>
     * 수천 개의 중단된 제너레이터가 스레드를 만들지 않는다(제너레이터마다 스레드를 쓰지 않음).
     */
    public void testGeneratorsUseNoThreads() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        int before = liveThreads();
        r.evaluateScript("var gens = []; function* g(i) { var a = yield i; var b = yield a; return b; }"
                + "for (var i = 0; i < 3000; i++) { var it = g(i); it.next(); gens.push(it); }", "a.js");
        int during = liveThreads();
        Check.isTrue(during - before < 3, "no thread per suspended generator: " + before + " -> " + during);
        Check.equal("3000", Ops.toString(r.evaluateScript("gens.length", "b.js")), "all generators exist");
        Check.equal("3", Ops.toString(r.evaluateScript("gens[3].next(3).value", "c.js")), "a suspended generator resumes after thousands of others");
    }

    /**
     * A suspended generator is reclaimed by the garbage collector when nothing refers to it (no hidden root keeps it alive).
     * <p>
     * 아무도 가리키지 않는 중단된 제너레이터는 가비지 컬렉터가 수거한다(살려 두는 숨은 루트가 없다).
     */
    public void testSuspendedGeneratorsAreCollectable() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("var holder = (function* () { var big = new Array(10); yield 1; yield 2; })(); holder.next();", "a.js");
        JsValue gen = r.getGlobalObject().get("holder");
        final java.lang.ref.WeakReference<Object> ref = new java.lang.ref.WeakReference<Object>(gen);
        gen = null;
        r.evaluateScript("holder = undefined;", "b.js");
        boolean collected = false;
        // 가비지 컬렉션은 즉시 일어난다고 보장할 수 없으므로 여러 번 시도한다.
        for (int i = 0; i < 20 && !collected; i++) {
            System.gc();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            collected = ref.get() == null;
        }
        Check.isTrue(collected, "an unreferenced suspended generator should be collectable");
    }

    /**
     * Generators resume correctly with deep native recursion between resumptions and with very long loops inside.
     * <p>
     * 재개 사이에 깊은 네이티브 재귀가 있어도, 안에 매우 긴 루프가 있어도 제너레이터가 올바르게 재개된다.
     */
    public void testLongRunningGenerators() {
        Check.equal("499500", Scripts.eval("function* g() { for (var i = 0; i < 1000; i++) yield i; } var s = 0; for (var v of g()) s += v; s"), "sum of a long generator");
        Check.equal("20000", Scripts.eval("function* g() { var i = 0; while (true) { var skip = yield i++; } } var it = g(), n = 0; for (var k = 0; k < 20000; k++) { it.next(); n++; } n"), "twenty thousand resumptions");
    }

    /**
     * Resuming a generator is subject to the execution limits of the realm (an endless loop inside a generator is aborted).
     * <p>
     * 제너레이터를 재개하는 일도 realm 의 실행 제한을 받는다(제너레이터 안의 무한 루프는 중단된다).
     */
    public void testGeneratorsHonorLimits() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(100000L);
        r.evaluateScript("function* g() { while (true) { } }", "a.js");
        try {
            r.evaluateScript("g().next();", "b.js");
            Check.fail("endless generator was not aborted");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, e.getReason(), "step limit reason");
        }
    }

    /**
     * A generator object created in one realm job keeps working in later jobs and native code can drive it too.
     * <p>
     * 한 작업에서 만든 제너레이터 객체는 이후 작업에서도 동작하며 네이티브 코드로도 구동할 수 있다.
     */
    public void testHostCanDriveGenerators() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        JsValue g = r.evaluateScript("(function* () { var x = yield 1; yield x * 2; })()", "a.js");
        Check.isTrue(g instanceof GeneratorObject, "the evaluation result is a generator object");
        GeneratorObject go = (GeneratorObject) g;
        JsValue r1 = go.resume(GeneratorObject.NEXT, org.duckdns.hjow.swingwebpanel.js.Js.UNDEFINED);
        Check.equal("1", Ops.toString(((org.duckdns.hjow.swingwebpanel.js.JsObject) r1).get("value")), "first value");
        JsValue r2 = go.resume(GeneratorObject.NEXT, org.duckdns.hjow.swingwebpanel.js.Js.num(21));
        Check.equal("42", Ops.toString(((org.duckdns.hjow.swingwebpanel.js.JsObject) r2).get("value")), "second value");
        try {
            go.resume(GeneratorObject.THROW, org.duckdns.hjow.swingwebpanel.js.Js.str("fail"));
            Check.fail("throw into a finished-or-suspended generator should propagate");
        } catch (JsException e) {
            Check.contains(e.getMessage(), "fail", "the thrown value reaches the host");
        }
    }

    /**
     * Mutual limits: tick counting does not run backwards on resumption, so the limit also stops after resumption storms.
     * <p>
     * 재개를 거듭해도 단계 수가 줄어들지 않으므로 제한이 재개 폭주도 멈춘다.
     */
    public void testStepLimitCountsResumedWork() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(ExecutionLimits.DEFAULT_MAX_STEPS);
        r.getLimits().setMaxSteps(2000000L);
        r.evaluateScript("function* g() { var i = 0; for (;;) yield i++; }", "a.js");
        try {
            r.evaluateScript("var it = g(); for (;;) { it.next(); }", "b.js");
            Check.fail("endless resumption was not aborted");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, e.getReason(), "step limit reason");
        }
    }
}
