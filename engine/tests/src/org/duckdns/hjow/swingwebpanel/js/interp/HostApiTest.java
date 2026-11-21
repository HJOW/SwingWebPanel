package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.JobQueue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the host API of a realm (plan items P13-01 and P13-12): evaluation entry points, host objects and functions, the
 * console hook, the job queue hook and error transport between script and Java.
 * <p>
 * realm 의 호스트 API 시험(계획 항목 P13-01, P13-12): 실행 진입점, 호스트 객체와 함수, 콘솔 연결, 작업 큐 연결, 스크립트와 Java 사이의
 * 오류 전달.
 */
public class HostApiTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public HostApiTest() {
    }

    /**
     * Results of evaluateScript: completion values, undefined for declarations, and independence between realms.
     * <p>
     * evaluateScript 의 결과: 완료 값, 선언은 undefined, realm 사이의 독립성.
     */
    public void testEvaluateScript() {
        JsRealm a = Scripts.realm(new Scripts.Capture());
        JsRealm b = Scripts.realm(new Scripts.Capture());
        Check.equal("7", Ops.toString(a.evaluateScript("var shared = 7; shared", "a.js")), "value");
        Check.equal("undefined", Ops.toString(b.evaluateScript("typeof shared", "b.js")), "realms do not share globals");
        Check.isTrue(a.getGlobalObject() != b.getGlobalObject(), "separate global objects");
        Check.isTrue(a.getObjectPrototype() != b.getObjectPrototype(), "separate intrinsics");
        Check.equal("undefined", Ops.toString(a.evaluateScript("", "empty.js")), "empty script");
        Check.equal("undefined", Ops.toString(a.evaluateScript("function f() {}", "decl.js")), "declaration only");
    }

    /**
     * Objects created in one realm are not instances of another realm's intrinsics.
     * <p>
     * 한 realm 에서 만든 객체는 다른 realm 의 내장 객체의 인스턴스가 아니다.
     */
    public void testRealmsAreIsolated() {
        JsRealm a = Scripts.realm(new Scripts.Capture());
        JsRealm b = Scripts.realm(new Scripts.Capture());
        a.evaluateScript("var arr = [1, 2];", "a.js");
        b.getGlobalObject().set("foreign", a.getGlobalObject().get("arr"));
        Check.equal("false", Ops.toString(b.evaluateScript("foreign instanceof Array", "b.js")), "arrays of another realm have another prototype");
        Check.equal("true", Ops.toString(b.evaluateScript("Array.isArray(foreign)", "c.js")), "but are still arrays");
    }

    /**
     * A host function and host object are visible to scripts and receive converted arguments.
     * <p>
     * 호스트 함수와 호스트 객체는 스크립트에서 보이며 변환된 인수를 받는다.
     */
    public void testHostFunctionsAndObjects() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        final List<String> calls = new ArrayList<String>();
        JsObject host = r.newObject();
        r.method(host, "add", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                calls.add("add");
                return Js.num(Ops.toNumber(Js.arg(args, 0)) + Ops.toNumber(Js.arg(args, 1)));
            }
        });
        r.getter(host, "version", new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.str("1.0");
            }
        });
        r.getGlobalObject().defineBuiltin("host", host);
        Check.equal("5", Ops.toString(r.evaluateScript("host.add(2, '3')", "a.js")), "host function with conversion");
        Check.equal("1.0", Ops.toString(r.evaluateScript("host.version", "b.js")), "host getter");
        Check.equal("function", Ops.toString(r.evaluateScript("typeof host.add", "c.js")), "function type");
        Check.equal("add", Ops.toString(r.evaluateScript("host.add.name", "d.js")), "function name");
        Check.equal("2", Ops.toString(r.evaluateScript("host.add.length", "e.js")), "function length");
        Check.equal(1, calls.size(), "called once");
        Check.equal("NaN", Ops.toString(r.evaluateScript("host.add()", "f.js")), "missing arguments are undefined");
    }

    /**
     * Script exceptions reach Java as {@link JsException} and carry the thrown value; native errors are catchable by scripts.
     * <p>
     * 스크립트 예외는 {@link JsException} 으로 Java 에 전달되며 던진 값을 담는다. 기본 오류는 스크립트가 잡을 수 있다.
     */
    public void testErrorsCrossTheBoundary() {
        final JsRealm r = Scripts.realm(new Scripts.Capture());
        JsObject host = r.newObject();
        r.method(host, "fail", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("host says no");
            }
        });
        r.method(host, "crash", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                throw new IllegalStateException("java failure");
            }
        });
        r.getGlobalObject().defineBuiltin("host", host);
        Check.equal("TypeError:host says no", Ops.toString(r.evaluateScript("try { host.fail(); } catch (e) { e.name + ':' + e.message; }", "a.js")), "native errors are catchable");
        try {
            r.evaluateScript("try { host.crash(); } catch (e) { 'swallowed'; }", "b.js");
            Check.fail("a Java exception must not be catchable by script code");
        } catch (IllegalStateException e) {
            Check.equal("java failure", e.getMessage(), "the Java exception reaches the host unchanged");
        }
        try {
            r.evaluateScript("throw { code: 42 };", "c.js");
            Check.fail("uncaught throw must reach the host");
        } catch (JsException e) {
            Check.isTrue(e.getValue(r) instanceof JsObject, "the thrown value is carried");
            Check.equal("42", Ops.toString(((JsObject) e.getValue(r)).get("code")), "thrown value content");
        }
        try {
            r.evaluateScript("var x = ;", "d.js");
            Check.fail("syntax error expected");
        } catch (JsException e) {
            Check.contains(e.getMessage(), "SyntaxError", "syntax errors are SyntaxError exceptions");
            Check.contains(e.getMessage(), "d.js:1", "the position includes the source name and line");
        }
    }

    /**
     * Console and print output goes through the hook with the level, and the default hook does not fail.
     * <p>
     * 콘솔과 print 출력은 수준과 함께 연결을 통해 전달되며, 기본 연결은 실패하지 않는다.
     */
    public void testConsoleHook() {
        Scripts.Capture c = new Scripts.Capture();
        JsRealm r = Scripts.realm(c);
        r.evaluateScript("console.log('a', 1, true); console.info('i'); console.warn('w'); console.error('e'); console.debug('d'); print('p', 2);", "a.js");
        Check.equal("[log: a 1 true, info: i, warn: w, error: e, debug: d, log: p 2]", c.leveled.toString(), "levels and text");
        c.leveled.clear();
        c.lines.clear();
        r.evaluateScript("console.log();console.log('');console.log(undefined, null);", "b.js");
        Check.equal("[log: , log: , log: undefined null]", c.leveled.toString(), "empty and nullish output");
        JsRealm plain = JsRealm.create();
        Check.isTrue(plain.getConsole() != null, "a default console hook exists");
    }

    /**
     * The compact object formatting used by console output.
     * <p>
     * 콘솔 출력이 쓰는 간결한 객체 서식.
     */
    public void testConsoleFormatting() {
        Check.equal("[ 1, 2, 3 ]", Scripts.output("console.log([1, 2, 3])"), "array");
        Check.equal("{ a: 1, b: 'x' }", Scripts.output("console.log({ a: 1, b: 'x' })"), "object");
        Check.equal("{}", Scripts.output("console.log({})"), "empty object");
        Check.equal("[]", Scripts.output("console.log([])"), "empty array");
        Check.equal("-0", Scripts.output("console.log(-0)"), "negative zero");
        Check.equal("[Function: f]", Scripts.output("function f() {} console.log(f)"), "function");
        Check.equal("[Function]", Scripts.output("console.log(function () {})"), "anonymous function");
        Check.equal("{ a: [ 1, { b: 2 } ] }", Scripts.output("console.log({ a: [1, { b: 2 }] })"), "nesting");
        Check.equal("{ self: [Object] }", Scripts.output("var o = {}; o.self = o; console.log(o)"), "cycle");
        Check.contains(Scripts.output("console.log(new TypeError('bad'))"), "TypeError: bad", "error");
        Check.equal("text", Scripts.output("console.log('text')"), "a bare string");
        Check.equal("[ 'q' ]", Scripts.output("console.log(['q'])"), "strings inside containers are quoted");
        Check.equal("Symbol(s)", Scripts.output("console.log(Symbol('s'))"), "symbol");
        Check.equal("{ g: [Getter/Setter] }", Scripts.output("console.log({ get g() { return 1; } })"), "accessors are not invoked");
    }

    /**
     * A host job queue receives the jobs; the engine never runs them on its own.
     * <p>
     * 호스트 작업 큐가 작업을 받으며, 엔진은 스스로 작업을 실행하지 않는다.
     */
    public void testCustomJobQueue() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        final List<Runnable> queued = new ArrayList<Runnable>();
        r.setJobQueue(new JobQueue() {
            public void enqueue(Runnable job) {
                queued.add(job);
            }
        });
        final StringBuilder order = new StringBuilder();
        r.enqueueJob(new Runnable() {
            public void run() {
                order.append("1");
            }
        });
        r.enqueueJob(new Runnable() {
            public void run() {
                order.append("2");
            }
        });
        r.runJobs();
        Check.equal(2, queued.size(), "both jobs reached the host queue");
        Check.equal("", order.toString(), "the engine does not run jobs of a host queue by itself");
        // 호스트가 큐를 직접 비운다.
        for (int i = 0; i < queued.size(); i++) {
            queued.get(i).run();
        }
        Check.equal("12", order.toString(), "the host runs the jobs in order");
    }

    /**
     * The default job queue runs jobs in FIFO order after the script, including jobs queued by jobs.
     * <p>
     * 기본 작업 큐는 스크립트 뒤에 작업을 선입선출로 실행하며, 작업이 넣은 작업도 실행한다.
     */
    public void testDefaultJobQueue() {
        final JsRealm r = Scripts.realm(new Scripts.Capture());
        final StringBuilder order = new StringBuilder();
        r.enqueueJob(new Runnable() {
            public void run() {
                order.append("a");
                r.enqueueJob(new Runnable() {
                    public void run() {
                        order.append("c");
                    }
                });
            }
        });
        r.enqueueJob(new Runnable() {
            public void run() {
                order.append("b");
            }
        });
        Check.equal("", order.toString(), "jobs do not run on enqueue");
        r.runJobs();
        Check.equal("abc", order.toString(), "FIFO including nested jobs");
        r.runJobs();
        Check.equal("abc", order.toString(), "running again with an empty queue does nothing");
    }

    /**
     * Native functions are first class values: they can be passed to scripts, stored and called back.
     * <p>
     * 네이티브 함수는 일급 값이다: 스크립트에 넘기고, 저장하고, 다시 호출할 수 있다.
     */
    public void testCallbacksFromJava() {
        JsRealm r = Scripts.realm(new Scripts.Capture());
        JsValue f = r.evaluateScript("(function (a, b) { return this.base + a * b; })", "a.js");
        Check.isTrue(f instanceof JsObject && ((JsObject) f).isCallable(), "a script function is callable from Java");
        JsObject self = r.newObject();
        self.set("base", Js.num(100));
        JsValue result = ((JsObject) f).call(self, new JsValue[] { Js.num(6), Js.num(7) });
        Check.equal("142", Ops.toString(result), "call with this and arguments");
        JsValue ctor = r.evaluateScript("(function P(x) { this.x = x; })", "b.js");
        JsObject made = ((JsObject) ctor).construct(new JsValue[] { Js.num(9) }, (JsObject) ctor);
        Check.equal("9", Ops.toString(made.get("x")), "construct from Java");
        try {
            ((JsObject) r.evaluateScript("(function () { throw new RangeError('cb'); })", "c.js")).call(Js.UNDEFINED, new JsValue[0]);
            Check.fail("exception expected");
        } catch (JsException e) {
            Check.equal("RangeError: cb", e.getMessage(), "an exception from a script callback");
        }
    }

    /**
     * Evaluation entry points reject null arguments with an IllegalArgumentException or NullPointerException-free message.
     * <p>
     * 실행 진입점은 null 인수를 NullPointerException 없이 명확한 예외로 거부한다.
     */
    public void testNullArguments() {
        final JsRealm r = Scripts.realm(new Scripts.Capture());
        Check.throwsException(RuntimeException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                r.evaluateScript(null, "x.js");
            }
        }, "null source");
        Check.equal("3", Ops.toString(r.evaluateScript("1 + 2", null)), "a null source name is tolerated");
    }

    /**
     * evaluateScript can be called again from a native function while a script runs (re-entrancy).
     * <p>
     * 스크립트가 실행되는 동안 네이티브 함수에서 evaluateScript 를 다시 호출할 수 있다(재진입).
     */
    public void testReentrantEvaluation() {
        final JsRealm r = Scripts.realm(new Scripts.Capture());
        r.method(r.getGlobalObject(), "nested", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return rr.evaluateScript(Ops.toString(Js.arg(args, 0)), "nested.js");
            }
        });
        Check.equal("15", Ops.toString(r.evaluateScript("var x = 5; nested('x * 2') + x", "a.js")), "nested evaluation sees the same globals");
        Check.equal("RangeError", Ops.toString(r.evaluateScript("function deep() { nested('deep()'); } try { deep(); } catch (e) { e.name; }", "b.js")), "re-entrant recursion is limited");
    }
}
