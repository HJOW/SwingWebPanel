package org.duckdns.hjow.swingwebpanel.js.script;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.embed.JsCallable;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction;
import org.duckdns.hjow.swingwebpanel.js.embed.JsScriptError;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Java Scripting API engine {@link JsScriptEngine} (plan items P26-04 and P26-05): <code>eval</code> with the three kinds of input,
 * <code>Bindings</code> and <code>ScriptContext</code> scopes, the writers, <code>Compilable</code>, <code>Invocable</code> with <code>getInterface</code>, the positions of
 * <code>ScriptException</code>, independence of engines, limits and threads. The engine is always created through <code>ScriptEngineManager</code> by its unique
 * name, and every test that needs it checks that the engine is an instance of the class of this project.
 * <p>
 * Java Scripting API 엔진 {@link JsScriptEngine} 의 시험(계획 항목 P26-04, P26-05): 세 가지 입력의 <code>eval</code>, <code>Bindings</code> 와
 * <code>ScriptContext</code> 스코프, 라이터, <code>Compilable</code>, <code>getInterface</code> 를 포함한 <code>Invocable</code>, <code>ScriptException</code> 의 위치, 엔진 사이의
 * 독립성, 제한, 스레드. 엔진은 항상 <code>ScriptEngineManager</code> 로 고유한 이름으로 만들며, 엔진이 필요한 모든 시험은 그것이 이 프로젝트의 클래스의 인스턴스임을 확인한다.
 */
public class JsScriptEngineTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public JsScriptEngineTest() {
    }

    /**
     * Creates an engine through the manager, by the unique name, and checks that it is ours.
     * <p>
     * 매니저로 고유한 이름의 엔진을 만들고 이 프로젝트의 것인지 확인한다.
     *
     * @return the engine / 엔진
     */
    private static ScriptEngine newEngine() {
        ScriptEngine e = new ScriptEngineManager().getEngineByName("swingwebpanel-js");
        Check.isTrue(e instanceof JsScriptEngine, "the engine of this project: " + (e == null ? null : e.getClass().getName()));
        return e;
    }

    /**
     * <code>eval</code> of a string, of a reader and with a null result.
     * <p>
     * 문자열과 리더의 <code>eval</code>, 그리고 null 결과.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEvalStringAndReader() throws Exception {
        ScriptEngine e = newEngine();
        Check.equal(Double.valueOf(3), e.eval("1 + 2"), "a string");
        Check.equal("rd", e.eval(new StringReader("'r' + 'd'")), "a reader");
        Check.isNull(e.eval("var v = 1;"), "a script without a value gives null");
        Check.isNull(e.eval("undefined"), "undefined gives null");
        Check.isNull(e.eval("null"), "null gives null");
        Check.equal(Boolean.TRUE, e.eval("[1, 2, 3].indexOf(2) === 1"), "a boolean");
        Check.isTrue(e.eval("({a: 1})") instanceof Map, "an object is a Map");
        Check.isTrue(e.eval("[1, 2]") instanceof List, "an array is a List");
        Check.isTrue(e.eval("(function () {})") instanceof JsCallable, "a function is a JsCallable");
        Check.throwsException(NullPointerException.class, new Block() {
            public void run() throws Exception {
                newEngine().eval((String) null);
            }
        }, "a null script");
        Check.throwsException(NullPointerException.class, new Block() {
            public void run() throws Exception {
                newEngine().eval("1", (ScriptContext) null);
            }
        }, "a null context");
    }

    /**
     * The default <code>ENGINE_SCOPE</code> is a live view of the global object: put, get, scripts, types, host functions and the refusal of Java objects.
     * <p>
     * 기본 <code>ENGINE_SCOPE</code> 는 전역 객체의 살아 있는 보기이다: put, get, 스크립트, 타입, 호스트 함수, Java 객체의 거부.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEngineScopeIsLive() throws Exception {
        final ScriptEngine e = newEngine();
        e.put("n", Integer.valueOf(4));
        e.put("s", "str");
        e.put("nothing", null);
        Check.equal(Double.valueOf(5), e.eval("n + 1"), "a put value is a global");
        Check.equal("str!", e.eval("s + '!'"), "string");
        Check.equal(Double.valueOf(4), e.get("n"), "get returns the script's Double");
        e.eval("var made = [1, 2]; function f(x) { return x + 1; } var undef;");
        Check.isTrue(e.get("made") instanceof List, "a var of a script is visible");
        Check.isTrue(e.get("f") instanceof JsCallable, "a function declaration is visible");
        Check.isNull(e.get("undef"), "undefined reads as null");
        Check.isTrue(e.getBindings(ScriptContext.ENGINE_SCOPE).containsKey("undef"), "but the key exists");
        Check.isNull(e.get("missing"), "a missing name");
        Check.isTrue(e.getBindings(ScriptContext.ENGINE_SCOPE) instanceof JsGlobalBindings, "the live bindings");
        Check.equal(Double.valueOf(4), e.getBindings(ScriptContext.ENGINE_SCOPE).get("n"), "the same through the bindings");
        e.eval("n = 100");
        Check.equal(Double.valueOf(100), e.get("n"), "an assignment of a script");
        e.getBindings(ScriptContext.ENGINE_SCOPE).remove("n");
        Check.equal("undefined", e.eval("typeof n"), "remove of an assigned global");
        e.put("add", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return Double.valueOf(((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue());
            }
        });
        Check.equal(Double.valueOf(7), e.eval("add(3, 4)"), "a host function in the bindings is how scripts call Java");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                e.put("o", new Object());
            }
        }, "a Java object that cannot cross is refused");
        Check.throwsException(NullPointerException.class, new Block() {
            public void run() {
                e.put(null, "x");
            }
        }, "a null key");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                e.put("", "x");
            }
        }, "an empty key");
        Check.equal("undefinedundefined", e.eval("typeof java + typeof Packages"), "a script has no way to Java");
    }

    /**
     * Other bindings get a global scope of their own: variables persist per bindings object, bindings do not see each other, the default scope
     * is untouched, unchanged values keep their Java type, and changes come back.
     * <p>
     * 다른 바인딩은 자신만의 전역 스코프를 가진다: 변수는 바인딩 객체마다 유지되고, 바인딩끼리 서로 보지 못하며, 기본 스코프는 건드리지 않고, 바뀌지 않은 값은 Java 타입을
     * 유지하며, 바뀐 것은 돌아온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOtherBindingsHaveTheirOwnScope() throws Exception {
        ScriptEngine e = newEngine();
        e.put("shared", "default scope");
        Bindings a = e.createBindings();
        Bindings b = e.createBindings();
        a.put("i", Integer.valueOf(1));
        a.put("keep", Integer.valueOf(9));
        Check.equal(Double.valueOf(2), e.eval("var made = i + 1; i = i + 1; made", a), "the script sees the entries of the bindings");
        Check.equal(Double.valueOf(2), a.get("made"), "a var of the script is copied back");
        Check.equal(Double.valueOf(2), a.get("i"), "a changed entry is copied back");
        Check.equal(Integer.valueOf(9), a.get("keep"), "an entry that the script did not change keeps its Java type");
        Check.equal(Double.valueOf(3), e.eval("made + 1", a), "the same bindings object keeps its variables from one eval to the next");
        Check.equal("undefined", e.eval("typeof made", b), "other bindings do not see them");
        Check.equal("undefined", e.eval("typeof shared", a), "the scope of the bindings is not the default scope");
        Check.equal("undefined", e.eval("typeof made"), "the default scope does not see them either");
        Check.equal("default scope", e.get("shared"), "the default scope is untouched");
        a.put("added", "from java");
        Check.equal("from java", e.eval("added", a), "an entry that Java added between two evals is seen");
        e.eval("tmp = 5;", a);
        Check.equal(Double.valueOf(5), a.get("tmp"), "an implicit global of the script is copied back");
        e.eval("delete globalThis.tmp", a);
        Check.isFalse(a.containsKey("tmp"), "a global that the script deleted is removed from the bindings: " + a.keySet());
        Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                Bindings bad = new SimpleBindings();
                bad.put("o", new Object());
                newEngine().eval("1", bad);
            }
        }, "a value that cannot cross is a ScriptException");
    }

    /**
     * The <code>GLOBAL_SCOPE</code> bindings are searched for names that the engine scope does not have, and are never written.
     * <p>
     * <code>GLOBAL_SCOPE</code> 바인딩은 엔진 스코프에 없는 이름을 찾는 데 쓰이며 쓰이지는 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGlobalScopeIsAFallback() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        manager.put("fromManager", "global scope value");
        manager.put("both", "global");
        ScriptEngine e = manager.getEngineByName("swingwebpanel-js");
        Check.isTrue(e instanceof JsScriptEngine, "ours");
        e.put("both", "engine");
        Check.equal("global scope value", e.eval("fromManager"), "a name of the manager is visible");
        Check.equal("engine", e.eval("both"), "the engine scope wins");
        Check.equal("string", e.eval("typeof fromManager"), "typeof sees it");
        e.eval("fromManager = 'changed by script'");
        Check.equal("global scope value", manager.get("fromManager"), "the global scope is never written");
        Check.equal("changed by script", e.eval("fromManager"), "an assignment goes to the engine scope");
        ScriptEngine other = manager.getEngineByName("swingwebpanel-js");
        Check.equal("global scope value", other.eval("fromManager"), "another engine of the same manager shares the global scope");
        Check.equal("global", other.eval("both"), "and not the engine scope of the first engine");
    }

    /**
     * <code>print</code> and <code>console</code> write lines to the writers of the context that runs the script.
     * <p>
     * <code>print</code> 와 <code>console</code> 은 스크립트를 실행하는 컨텍스트의 라이터에 줄을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOutputGoesToTheWritersOfTheContext() throws Exception {
        ScriptEngine e = newEngine();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        e.getContext().setWriter(out);
        e.getContext().setErrorWriter(err);
        e.eval("print('p', 1); console.log('l'); console.info('i'); console.debug('d'); console.warn('w'); console.error('e');");
        String nl = System.getProperty("line.separator");
        Check.equal("p 1" + nl + "l" + nl + "i" + nl + "d" + nl, out.toString(), "log, info, debug and print go to the writer");
        Check.equal("w" + nl + "e" + nl, err.toString(), "warn and error go to the error writer");
        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(e.createBindings(), ScriptContext.ENGINE_SCOPE);
        StringWriter other = new StringWriter();
        ctx.setWriter(other);
        e.eval("print('to the other context')", ctx);
        Check.equal("to the other context" + nl, other.toString(), "the context that runs the script is used");
        Check.equal("p 1" + nl + "l" + nl + "i" + nl + "d" + nl, out.toString(), "the writer of the default context did not get it");
        e.eval("function later() { print('from a call'); }");
        ((Invocable) e).invokeFunction("later");
        Check.contains(out.toString(), "from a call", "a call through Invocable writes to the writer of the default context");
    }

    /**
     * A script that was compiled in advance runs in any context, and compile reports syntax errors.
     * <p>
     * 미리 컴파일한 스크립트는 어느 컨텍스트에서나 실행되고, compile 은 구문 오류를 보고한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompilable() throws Exception {
        final ScriptEngine e = newEngine();
        Check.isTrue(e instanceof Compilable, "Compilable");
        CompiledScript cs = ((Compilable) e).compile("typeof counter === 'undefined' ? (counter = 1) : ++counter");
        Check.same(e, cs.getEngine(), "the engine of the compiled script");
        Check.equal(Double.valueOf(1), cs.eval(), "first run in the default context");
        Check.equal(Double.valueOf(2), cs.eval(), "second run");
        Check.equal(Double.valueOf(1), cs.eval(e.createBindings()), "a run with other bindings has its own scope");
        Bindings b = e.createBindings();
        b.put("counter", Integer.valueOf(41));
        Check.equal(Double.valueOf(42), cs.eval(b), "with an entry");
        Check.equal("rd", ((Compilable) e).compile(new StringReader("'r' + 'd'")).eval(), "a reader");
        ScriptException se = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                ((Compilable) e).compile("var a = 1;\nvar = 2;");
            }
        }, "compile reports a syntax error");
        Check.equal(2, se.getLineNumber(), "line of the syntax error");
        Check.isTrue(se.getColumnNumber() > 0, "column of the syntax error");
        Check.contains(se.getMessage(), "SyntaxError", "message of the syntax error");
    }

    /**
     * The positions of <code>ScriptException</code>: file name from the context attribute, line, column and the cause.
     * <p>
     * <code>ScriptException</code> 의 위치: 컨텍스트 속성의 파일 이름, 줄, 열, 원인.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptExceptionPositions() throws Exception {
        final ScriptEngine e = newEngine();
        e.put(ScriptEngine.FILENAME, "pos.js");
        ScriptException syntax = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("var ok = 1;\n\nvar = 3;");
            }
        }, "syntax error");
        Check.equal("pos.js", syntax.getFileName(), "file name");
        Check.equal(3, syntax.getLineNumber(), "line");
        Check.isTrue(syntax.getColumnNumber() > 0, "column: " + syntax.getColumnNumber());
        Check.isTrue(syntax.getCause() instanceof JsScriptError, "the cause is the error of the engine");
        Check.contains(syntax.getMessage(), "SyntaxError", "message");
        Check.contains(syntax.getMessage(), "pos.js", "the file name is in the message");
        ScriptException runtime = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("var a = 1;\nvar b = a.x.y;\n");
            }
        }, "runtime error");
        Check.equal("pos.js", runtime.getFileName(), "file name of a runtime error");
        Check.equal(2, runtime.getLineNumber(), "line of a runtime error");
        Check.contains(runtime.getMessage(), "TypeError", "runtime message");
        ScriptException thrown = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("\n\nthrow new Error('custom');");
            }
        }, "throw");
        Check.equal(3, thrown.getLineNumber(), "line of a throw statement");
        Check.contains(thrown.getMessage(), "custom", "message of a throw");
        e.getContext().removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
        ScriptException unnamed = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("null.x");
            }
        }, "no file name");
        Check.equal("<eval>", unnamed.getFileName(), "the default name of a script");
    }

    /**
     * <code>Invocable</code>: functions, methods of script objects, interfaces, and the errors of each.
     * <p>
     * <code>Invocable</code>: 함수, 스크립트 객체의 메서드, 인터페이스, 그리고 각각의 오류.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvocable() throws Exception {
        final ScriptEngine e = newEngine();
        final Invocable inv = (Invocable) e;
        e.eval("function twice(x) { return x * 2; } function fails() { throw new RangeError('bad'); }"
                + " var counter = { n: 0, inc: function () { this.n++; return this.n; } };"
                + " function run() { runCalled = true; } var ran = false; function name() { return 'global'; }");
        Check.equal(Double.valueOf(42), inv.invokeFunction("twice", Integer.valueOf(21)), "invokeFunction");
        Object counter = e.get("counter");
        Check.equal(Double.valueOf(1), inv.invokeMethod(counter, "inc"), "invokeMethod");
        Check.equal(Double.valueOf(2), inv.invokeMethod(counter, "inc"), "the state of the object");
        Check.equal(Double.valueOf(2), e.eval("counter.n"), "is the state of the script");
        Check.throwsException(NoSuchMethodException.class, new Block() {
            public void run() throws Exception {
                inv.invokeFunction("noSuchFunction");
            }
        }, "a missing function");
        Check.throwsException(NoSuchMethodException.class, new Block() {
            public void run() throws Exception {
                inv.invokeMethod(e.get("counter"), "n");
            }
        }, "a property that is not a function");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                inv.invokeMethod(null, "inc");
            }
        }, "a null object");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                inv.invokeMethod("a Java string", "length");
            }
        }, "an object that is not a script object");
        ScriptException se = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                inv.invokeFunction("fails");
            }
        }, "the error of a function");
        Check.contains(se.getMessage(), "RangeError: bad", "message");
    }

    /**
     * <code>getInterface</code> with global functions, with a script object, and for incomplete implementations.
     * <p>
     * 전역 함수, 스크립트 객체, 불완전한 구현에 대한 <code>getInterface</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGetInterface() throws Exception {
        final ScriptEngine e = newEngine();
        Invocable inv = (Invocable) e;
        e.eval("function run() { ran = 'global run'; } var ran;"
                + " var worker = { run: function () { ran = 'object run'; }, toString: function () { return 'w'; } };"
                + " var half = { other: 1 };");
        Runnable viaGlobals = inv.getInterface(Runnable.class);
        Check.notNull(viaGlobals, "a global function run() implements Runnable");
        viaGlobals.run();
        Check.equal("global run", e.get("ran"), "the global function ran");
        Runnable viaObject = inv.getInterface(e.get("worker"), Runnable.class);
        viaObject.run();
        Check.equal("object run", e.get("ran"), "the method of the object ran");
        Check.isNull(inv.getInterface(e.get("half"), Runnable.class), "null for an object without the method");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                ((Invocable) e).getInterface((Class<Runnable>) null);
            }
        }, "a null class");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                ((Invocable) e).getInterface(e.get("worker"), String.class);
            }
        }, "a class that is not an interface");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                ((Invocable) e).getInterface(null, Runnable.class);
            }
        }, "a null object");
    }

    /**
     * Engines of one factory and of different managers share nothing.
     * <p>
     * 한 팩터리의 엔진들과 서로 다른 매니저의 엔진들은 아무것도 공유하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEnginesAreIndependent() throws Exception {
        ScriptEngine a = newEngine();
        ScriptEngine b = newEngine();
        Check.isTrue(a != b, "two engines");
        a.eval("var mine = 1; Array.prototype.marker = true;");
        Check.equal("undefined", b.eval("typeof mine"), "globals are not shared");
        Check.equal("undefined", b.eval("typeof [].marker"), "built-in objects are not shared");
        Check.isTrue(((JsScriptEngine) a).getJsEngine() != ((JsScriptEngine) b).getJsEngine(), "two realms");
    }

    /**
     * The limits of the engine and the abort of a runaway script.
     * <p>
     * 엔진의 제한과 폭주하는 스크립트의 중단.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLimitsAbortScripts() throws Exception {
        final ScriptEngine e = newEngine();
        JsEngine js = ((JsScriptEngine) e).getJsEngine();
        js.getLimits().setMaxSteps(50000L);
        ScriptException se = (ScriptException) Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("while (true) { }");
            }
        }, "an endless loop");
        Check.isTrue(se.getCause() instanceof ScriptAbortedException, "the cause is the abort: " + se.getCause());
        Check.contains(se.getMessage(), "aborted", "message");
        js.getLimits().setMaxSteps(0L);
        js.getLimits().setMaxTimeMillis(100L);
        Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("while (true) { }");
            }
        }, "the time limit");
        js.getLimits().setMaxTimeMillis(0L);
        Check.equal(Double.valueOf(2), e.eval("1 + 1"), "usable afterwards");
        Bindings other = e.createBindings();
        js.getLimits().setMaxSteps(20000L);
        Check.throwsException(ScriptException.class, new Block() {
            public void run() throws Exception {
                e.eval("while (true) { }", e.createBindings());
            }
        }, "the limits are copied to the scope of other bindings");
        Check.equal(Double.valueOf(3), e.eval("1 + 2", other), "a fresh scope works");
        js.getLimits().setMaxSteps(org.duckdns.hjow.swingwebpanel.js.ExecutionLimits.DEFAULT_MAX_STEPS);
    }

    /**
     * Several threads may use one engine; the operations are serialized.
     * <p>
     * 여러 스레드가 한 엔진을 쓸 수 있다. 연산은 직렬화된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThreads() throws Exception {
        final ScriptEngine e = newEngine();
        e.eval("var count = 0; function inc() { var c = count; count = c + 1; }");
        final Invocable inv = (Invocable) e;
        final int threads = 4;
        final int each = 200;
        final Throwable[] failure = new Throwable[1];
        Thread[] workers = new Thread[threads];
        // 스레드마다 같은 함수를 여러 번 호출한다.
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(new Runnable() {
                public void run() {
                    try {
                        // 정해진 횟수만큼 호출한다.
                        for (int i = 0; i < each; i++) {
                            inv.invokeFunction("inc");
                        }
                    } catch (Throwable ex) {
                        failure[0] = ex;
                    }
                }
            }, "swingwebpanel-test-script-" + t);
            workers[t].setDaemon(true);
            workers[t].start();
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (int t = 0; t < threads; t++) {
            workers[t].join(30000L);
        }
        Check.isNull(failure[0], "no failure: " + failure[0]);
        Check.equal(Double.valueOf(threads * each), e.get("count"), "every call counted once");
    }

    /**
     * Promise jobs run before <code>eval</code> returns, ES2015 modules are available through the engine, and a promise result can be inspected.
     * <p>
     * promise 작업은 <code>eval</code> 이 반환하기 전에 실행되며, ES2015 모듈은 엔진으로 쓸 수 있고, promise 결과를 검사할 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPromiseJobsAndModules() throws Exception {
        ScriptEngine e = newEngine();
        e.eval("var order = []; Promise.resolve().then(function () { order.push('job'); }); order.push('sync');");
        Check.equal("sync,job", e.eval("order.join()"), "the job ran before eval returned");
        JsEngine js = ((JsScriptEngine) e).getJsEngine();
        Check.equal(Double.valueOf(42), js.evalModule("m.js", "export var answer = 42;").get("answer"), "modules through the JsEngine of the script engine");
    }
}
