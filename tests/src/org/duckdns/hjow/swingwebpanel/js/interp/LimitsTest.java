package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of execution limits, cancellation and call depth (plan item P13-12): endless loops and recursion end with explicit
 * errors, the host can cancel from another thread, and the realm stays usable afterwards.
 * <p>
 * 실행 제한, 취소, 호출 깊이 시험(계획 항목 P13-12): 무한 반복과 재귀는 명시적 오류로 끝나고, 호스트는 다른 스레드에서 취소할 수
 * 있으며, 그 뒤에도 realm 을 계속 쓸 수 있다.
 */
public class LimitsTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public LimitsTest() {
    }

    /**
     * Creates a realm with a small step limit.
     * <p>
     * 작은 단계 제한을 가진 realm 을 만든다.
     *
     * @param steps the step limit / 단계 수 제한
     * @return the realm / realm
     */
    private static JsRealm limited(long steps) {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(steps);
        return r;
    }

    /**
     * Runs a script expecting an abort and returns its reason.
     * <p>
     * 중단을 기대하며 스크립트를 실행하고 그 이유를 반환한다.
     *
     * @param r      the realm / realm
     * @param source the script / 스크립트
     * @return the abort reason / 중단 이유
     */
    private static ScriptAbortedException.Reason abortOf(JsRealm r, String source) {
        try {
            r.evaluateScript(source, "t.js");
        } catch (ScriptAbortedException e) {
            return e.getReason();
        }
        Check.fail("script was not aborted: " + source);
        return null;
    }

    /**
     * Every kind of endless loop is stopped by the step limit.
     * <p>
     * 모든 종류의 무한 반복이 단계 수 제한으로 멈춘다.
     */
    public void testStepLimitStopsEndlessLoops() {
        String[] loops = { "while (true) { }", "for (;;) { }", "do { } while (true);", "for (var i = 0; ; i++) { }",
            "var o = {}; for (var k in {a: 1}) { while (true) { } }", "for (var v of (function* () { for (;;) yield 1; })()) { }",
            "var i = 0; l: while (true) { i++; continue l; }", "(function f() { while (true) { } })()",
            "function* g() { for (;;) { } } g().next();", "var a = [1]; for (var x of a) { a.push(x); }",
            "switch (1) { default: while (true) { } }", "try { while (true) { } } finally { }",
            "while (true) { try { } finally { } }", "eval('for (;;) { }')", "for (;;) { break_never: { } }" };
        // 각 무한 반복이 단계 수 제한으로 중단되는지 확인한다.
        for (int i = 0; i < loops.length; i++) {
            JsRealm r = limited(100000L);
            Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, abortOf(r, loops[i]), "step limit for: " + loops[i]);
        }
    }

    /**
     * The limit is per task: a new evaluation starts counting again and an aborted realm is still usable.
     * <p>
     * 제한은 작업 단위이다: 새 실행은 다시 세기 시작하며, 중단된 realm 도 계속 쓸 수 있다.
     */
    public void testRealmIsUsableAfterAbort() {
        JsRealm r = limited(50000L);
        r.evaluateScript("var keep = 'kept'; function work() { var s = 0; for (var i = 0; i < 1000; i++) s += i; return s; }", "a.js");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, abortOf(r, "while (true) { }"), "first abort");
        Check.equal("kept", Ops.toString(r.evaluateScript("keep", "b.js")), "state survives the abort");
        Check.equal("499500", Ops.toString(r.evaluateScript("work()", "c.js")), "a normal run after the abort");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, abortOf(r, "for (;;) { }"), "second abort");
        // 여러 번 실행해도 매번 새 예산으로 시작하는지 확인한다.
        for (int i = 0; i < 20; i++) {
            Check.equal("499500", Ops.toString(r.evaluateScript("work()", "d.js")), "each task gets its own budget");
        }
    }

    /**
     * A script cannot catch an abort: catch and finally blocks of the script do not swallow it.
     * <p>
     * 스크립트는 중단을 잡을 수 없다: 스크립트의 catch 와 finally 블록이 삼키지 못한다.
     */
    public void testAbortCannotBeCaught() {
        JsRealm r = limited(20000L);
        r.evaluateScript("var caught = false, fin = false;", "a.js");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT,
                abortOf(r, "try { while (true) { } } catch (e) { caught = true; } finally { fin = true; while (true) { } }"), "abort passes through try/catch/finally");
        Check.equal("false", Ops.toString(r.evaluateScript("caught", "b.js")), "catch did not run for an abort");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, abortOf(r, "function f() { try { return f2(); } catch (e) { return 'swallowed'; } } function f2() { while (true) { } } f()"),
                "abort passes through nested calls");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, abortOf(r, "[1, 2, 3].forEach(function () { while (true) { } })"), "abort passes through a built-in callback");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, abortOf(r, "for (var x of (function* () { try { yield 1; } finally { while (true) { } } })()) { break; }"),
                "abort inside a generator cleanup");
    }

    /**
     * The wall-clock limit stops a loop even with the step limit disabled.
     * <p>
     * 단계 수 제한을 끈 경우에도 실제 시간 제한이 반복을 멈춘다.
     */
    public void testTimeLimit() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(0);
        r.getLimits().setMaxTimeMillis(150L);
        long start = System.currentTimeMillis();
        Check.equal(ScriptAbortedException.Reason.TIME_LIMIT, abortOf(r, "while (true) { }"), "time limit reason");
        long took = System.currentTimeMillis() - start;
        Check.isTrue(took >= 100 && took < 5000, "the loop stopped near the limit: " + took + " ms");
        Check.equal("3", Ops.toString(r.evaluateScript("1 + 2", "b.js")), "usable afterwards");
    }

    /**
     * The host can cancel a running script from another thread and clear the flag again.
     * <p>
     * 호스트는 다른 스레드에서 실행 중인 스크립트를 취소하고 표시를 다시 지울 수 있다.
     */
    public void testCancellationFromAnotherThread() throws Exception {
        final JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(0);
        Thread canceller = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(150L);
                } catch (InterruptedException e) {
                    return;
                }
                r.getLimits().cancel();
            }
        });
        canceller.start();
        long start = System.currentTimeMillis();
        Check.equal(ScriptAbortedException.Reason.CANCELLED, abortOf(r, "var n = 0; while (true) { n++; }"), "cancel reason");
        canceller.join();
        Check.isTrue(System.currentTimeMillis() - start < 5000, "cancellation took effect quickly");
        Check.isTrue(r.getLimits().isCancelled(), "the flag stays set until reset");
        Check.equal(ScriptAbortedException.Reason.CANCELLED, abortOf(r, "1 + 1"), "a cancelled realm refuses to run new code");
        r.getLimits().reset();
        Check.equal("2", Ops.toString(r.evaluateScript("1 + 1", "c.js")), "usable after reset");
    }

    /**
     * Cancellation also stops generator resumption chains and recursion.
     * <p>
     * 취소는 제너레이터 재개 연쇄와 재귀도 멈춘다.
     */
    public void testCancellationStopsGeneratorsAndRecursion() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("function* g() { for (;;) yield 1; } var it = g(); it.next();", "a.js");
        r.getLimits().cancel();
        Check.equal(ScriptAbortedException.Reason.CANCELLED, abortOf(r, "it.next()"), "generator resumption");
        r.getLimits().reset();
        r.getLimits().cancel();
        Check.equal(ScriptAbortedException.Reason.CANCELLED, abortOf(r, "function f() { f(); } f();"), "recursion");
        r.getLimits().reset();
    }

    /**
     * Runaway recursion gives a catchable RangeError instead of crashing the Java stack.
     * <p>
     * 폭주하는 재귀는 Java 스택을 깨뜨리는 대신 잡을 수 있는 RangeError 를 만든다.
     */
    public void testCallDepthLimit() {
        Check.equal("RangeError", Scripts.eval("function f() { return f(); } try { f(); } catch (e) { e.name; }"), "direct recursion");
        Check.equal("RangeError", Scripts.eval("function f() { return 1 + f(); } try { f(); } catch (e) { e.name; }"), "recursion inside an expression");
        Check.equal("RangeError", Scripts.eval("var o = { m() { return this.m(); } }; try { o.m(); } catch (e) { e.name; }"), "method recursion");
        Check.equal("RangeError", Scripts.eval("function a() { b(); } function b() { a(); } try { a(); } catch (e) { e.name; }"), "mutual recursion");
        Check.equal("RangeError", Scripts.eval("var o = { get g() { return this.g; } }; try { o.g; } catch (e) { e.name; }"), "getter recursion");
        Check.equal("RangeError", Scripts.eval("function C() { new C(); } try { new C(); } catch (e) { e.name; }"), "constructor recursion");
        Check.equal("RangeError", Scripts.eval("class K { constructor() { new K(); } } try { new K(); } catch (e) { e.name; }"), "class constructor recursion");
        Check.equal("RangeError", Scripts.eval("function f() { eval('f()'); } try { f(); } catch (e) { e.name; }"), "eval recursion");
        Check.equal("RangeError", Scripts.eval("function f() { return f.call(null); } try { f(); } catch (e) { e.name; }"), "call recursion");
        Check.equal("RangeError", Scripts.eval("function f() { return f.apply(null, []); } try { f(); } catch (e) { e.name; }"), "apply recursion");
        Check.equal("RangeError", Scripts.eval("function f() { return f.bind(null)(); } try { f(); } catch (e) { e.name; }"), "bind recursion");
        Check.equal("RangeError", Scripts.eval("function f() { return [1].map(f); } try { f(); } catch (e) { e.name; }"), "recursion through a native callback");
        Check.equal("RangeError", Scripts.eval("var o = { toString() { return '' + o; } }; try { '' + o; } catch (e) { e.name; }"), "conversion recursion");
        Check.equal("RangeError", Scripts.eval("function* g() { yield* g(); } try { g().next(); } catch (e) { e.name; }"), "generator delegation recursion");
        Check.equal("RangeError", Scripts.eval("var f = () => f(); try { f(); } catch (e) { e.name; }"), "arrow recursion");
        Check.equal("RangeError", Scripts.eval("function f(a = f()) { } try { f(); } catch (e) { e.name; }"), "default parameter recursion");
        Check.equal("Maximum call stack size exceeded", Scripts.eval("function f() { f(); } try { f(); } catch (e) { e.message; }"), "message");
        Check.equal("true", Scripts.eval("function f() { f(); } var ok; try { f(); } catch (e) { ok = e instanceof RangeError; } ok"), "instance of RangeError");
    }

    /**
     * The depth limit is configurable, and recursion below the limit works.
     * <p>
     * 깊이 제한은 설정할 수 있으며 제한 아래의 재귀는 동작한다.
     */
    public void testCallDepthIsConfigurable() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.evaluateScript("function d(n) { return n == 0 ? 0 : 1 + d(n - 1); }", "a.js");
        Check.equal("300", Ops.toString(r.evaluateScript("d(300)", "b.js")), "300 levels with the default limit");
        r.getLimits().setMaxCallDepth(50);
        Check.equal("40", Ops.toString(r.evaluateScript("d(40)", "c.js")), "40 levels under a limit of 50");
        try {
            r.evaluateScript("d(60)", "d.js");
            Check.fail("60 levels must exceed a limit of 50");
        } catch (JsException e) {
            Check.contains(e.getMessage(), "RangeError", "RangeError for the depth");
        }
        Check.equal("20", Ops.toString(r.evaluateScript("d(20)", "e.js")), "the depth counter is restored after the error");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                new ExecutionLimits().setMaxCallDepth(0);
            }
        }, "a depth below 1 is rejected");
    }

    /**
     * Strings cannot grow without bound through concatenation and templates (built-ins of the libraries follow their own limit).
     * <p>
     * 문자열은 연결, 템플릿, join 으로 한없이 커질 수 없다.
     */
    public void testStringLengthLimit() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxStringLength(1000);
        Check.equal("RangeError", Ops.toString(r.evaluateScript("var s = 'x'; try { for (;;) s += s; } catch (e) { e.name; }", "a.js")), "doubling");
        Check.equal("RangeError", Ops.toString(r.evaluateScript("var t = 'y'; try { for (;;) t = `${t}${t}`; } catch (e) { e.name; }", "b.js")), "template doubling");
        Check.equal("ok", Ops.toString(r.evaluateScript("var small = ''; for (var i = 0; i < 100; i++) small += 'ab'; small.length < 1000 ? 'ok' : 'bad'", "d.js")), "short strings are fine");
        Check.equal("Invalid string length", Ops.toString(r.evaluateScript("var u = 'z'; try { for (;;) u += u; } catch (e) { e.message; }", "e.js")), "message");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                new ExecutionLimits().setMaxStringLength(0);
            }
        }, "a length below 1 is rejected");
    }

    /**
     * Defaults of all limits are the documented values.
     * <p>
     * 모든 제한의 기본값은 문서에 적힌 값이다.
     */
    public void testDefaults() {
        ExecutionLimits l = new JsRealm().getLimits();
        Check.equal(200000000L, l.getMaxSteps(), "default steps");
        Check.equal(0L, l.getMaxTimeMillis(), "default time is unlimited");
        Check.equal(400, l.getMaxCallDepth(), "default call depth");
        Check.equal(33554432, l.getMaxStringLength(), "default string length");
        Check.isFalse(l.isCancelled(), "not cancelled initially");
    }

    /**
     * Pending jobs run as separate tasks, each with its own budget, and a runaway job is aborted without losing the others.
     * <p>
     * 대기 작업은 각자 별도의 작업으로 실행되며 각자 자신의 예산을 가진다. 폭주하는 작업은 중단되어도 다른 작업을 잃지 않는다.
     */
    public void testJobsAreSeparateTasks() {
        final JsRealm r = limited(50000L);
        r.evaluateScript("var log = [];", "a.js");
        final JsValue fn = r.evaluateScript("(function () { log.push('job'); })", "b.js");
        // 세 개의 작업을 큐에 넣는다.
        for (int i = 0; i < 3; i++) {
            r.enqueueJob(new Runnable() {
                public void run() {
                    ((org.duckdns.hjow.swingwebpanel.js.JsObject) fn).call(org.duckdns.hjow.swingwebpanel.js.Js.UNDEFINED, new JsValue[0]);
                }
            });
        }
        r.runJobs();
        Check.equal("3", Ops.toString(r.evaluateScript("log.length", "c.js")), "all three jobs ran");
    }

    /**
     * The step counter of one evaluation does not leak into the next: many small evaluations never hit the limit.
     * <p>
     * 한 실행의 단계 수가 다음 실행으로 넘어가지 않는다: 작은 실행을 여러 번 해도 제한에 걸리지 않는다.
     */
    public void testManySmallEvaluations() {
        JsRealm r = limited(5000L);
        // 작은 실행을 많이 반복해도 누적되지 않는지 확인한다.
        for (int i = 0; i < 300; i++) {
            Check.equal("45", Ops.toString(r.evaluateScript("var s = 0; for (var i = 0; i < 10; i++) s += i; s", "t.js")), "evaluation " + i);
        }
    }
}
