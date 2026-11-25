package org.duckdns.hjow.swingwebpanel.js.embed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.ParsedScript;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the stand-alone engine API {@link JsEngine} (plan item P26-02): running scripts and modules, globals, calling functions, promise
 * jobs, parsing in advance, errors with positions, console output, limits and cancellation, independence of engines and the threading rules.
 * <p>
 * 독립 엔진 API {@link JsEngine} 의 시험(계획 항목 P26-02): 스크립트와 모듈 실행, 전역 변수, 함수 호출, promise 작업, 미리 파싱, 위치가 있는 오류,
 * 콘솔 출력, 제한과 취소, 엔진 사이의 독립성, 스레드 규칙.
 */
public class JsEngineBasicsTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public JsEngineBasicsTest() {
    }

    /**
     * The completion value of a script and the language level (ES2015 syntax and built-ins work without any host object).
     * <p>
     * 스크립트의 완료 값과 언어 수준(호스트 객체 없이 ES2015 구문과 내장 객체가 동작한다).
     */
    public void testEvalReturnsCompletionValueAndRunsEs6() {
        JsEngine e = new JsEngine();
        Check.equal(Double.valueOf(3), e.eval("1 + 2"), "number");
        Check.equal("HELLO WORLD", e.eval("`hello ${'world'}`.toUpperCase()"), "template string");
        Check.same(JsEngine.UNDEFINED, e.eval("var x = 1;"), "a declaration has no value");
        Check.same(JsEngine.UNDEFINED, e.eval(""), "an empty script has no value");
        Check.equal(null, e.eval("null"), "script null is Java null");
        Check.equal(Boolean.TRUE, e.eval("[1, 2, 3].map(x => x * 2).join() === '2,4,6'"), "arrow function and array built-ins");
        Check.equal("Z", e.eval("class A { static z() { return 'Z'; } } A.z()"), "class");
        Check.equal(Double.valueOf(6), e.eval("let [a, b] = [1, 2]; const {c} = {c: 3}; a + b + c"), "destructuring");
        Check.equal("1,2,3", e.eval("Array.from(new Set([1, 2, 2, 3])).join()"), "Set");
        Check.equal("{\"a\":[1,2]}", e.eval("JSON.stringify({a: [1, 2]})"), "JSON");
        Check.equal(Boolean.TRUE, e.eval("typeof window === 'undefined' && typeof document === 'undefined' && typeof setTimeout === 'undefined' && typeof queueMicrotask === 'undefined' && typeof fetch === 'undefined' && typeof URL === 'undefined'"),
                "no browser objects and no timers in stand-alone mode");
        Check.equal(Boolean.TRUE, e.eval("typeof console === 'object' && typeof print === 'function' && globalThis === this"), "console, print and globalThis exist");
    }

    /**
     * Engines are independent: globals, built-ins and state are not shared.
     * <p>
     * 엔진은 서로 독립이다. 전역 변수, 내장 객체, 상태를 공유하지 않는다.
     */
    public void testEnginesAreIndependent() {
        JsEngine a = new JsEngine();
        JsEngine b = new JsEngine();
        a.eval("var shared = 7; Array.prototype.marker = 1;");
        Check.equal("undefined", b.eval("typeof shared"), "globals are not shared");
        Check.equal("undefined", b.eval("typeof [].marker"), "built-ins are not shared");
        Check.isTrue(a.getRealm() != b.getRealm(), "two realms");
    }

    /**
     * Checks the descriptor and replacement or deletion of globalThis in an independent engine.
     * <p>
     * 독립 엔진의 globalThis 기술자와 값 교체 및 삭제를 확인한다.
     */
    public void testGlobalThis() {
        JsEngine engine = new JsEngine();
        Check.equal(Boolean.TRUE, engine.eval("var d=Object.getOwnPropertyDescriptor(this,'globalThis');"
                + " d.value===this && d.value===globalThis && d.writable && !d.enumerable && d.configurable"),
                "script global value and descriptor");
        Check.equal(Boolean.TRUE, engine.eval("var original=globalThis; globalThis=42;"
                + " globalThis===42 && this===original && delete this.globalThis && typeof globalThis==='undefined'"),
                "replacement and deletion preserve the realm's this binding");
    }

    /**
     * Checks a realm whose host supplies a distinct global this value.
     * <p>
     * 호스트가 별도의 전역 this 값을 지정한 realm을 검사한다.
     */
    public void testGlobalThisHostValue() {
        JsObject hostThis = new JsObject(null);
        JsRealm realm = JsRealm.create(hostThis);
        Check.same(hostThis, realm.getGlobalObject().get("globalThis"), "the initial property uses the host value");
        Check.same(hostThis, realm.evaluateScript("this", "host.js"), "classic script this");
        Check.same(hostThis, realm.evaluateScript("function f(){return this}; f.call(null)", "host.js"), "sloppy call this");
        Check.same(hostThis, realm.evaluateScript("eval('this')", "host.js"), "direct eval shares classic script this");
        Check.same(hostThis, realm.evaluateScript("(0, eval)('this')", "host.js"), "indirect eval uses its realm's global this");
        Check.same(Js.UNDEFINED, realm.evaluateModule("host.mjs", "export const topValue = this;").get("topValue"),
                "module top-level this stays undefined");
        Check.equal("42", Ops.toString(realm.evaluateScript("var property=42; property", "host.js")), "global binding lookup");
        Check.same(Js.UNDEFINED, hostThis.get("property"), "global declarations use the separate global object");
        Check.isTrue(realm.getGlobalObject().hasOwnProperty("property"), "the global object holds the variable");
    }

    /**
     * Checks which realm is active during property access and after unwinding a cross-realm call.
     * <p>
     * 프로퍼티 접근 중과 다른 realm 호출이 끝난 뒤에 활성 realm을 확인한다.
     */
    public void testExecutingRealm() {
        final JsRealm a = JsRealm.create();
        final JsRealm b = JsRealm.create();
        JsObject probe = new JsObject(null) {
            @Override
            public JsValue get(Object key, JsValue receiver) {
                return Js.str(RealmExecution.current() == a ? "a" : RealmExecution.current() == b ? "b" : "outside");
            }
        };
        a.getGlobalObject().defineBuiltin("probe", probe);
        b.getGlobalObject().defineBuiltin("probe", probe);
        b.evaluateScript("function other(){return probe.active;} function boom(){throw probe.active;}"
                + " function* values(){yield probe.active;} function Maker(){this.active=probe.active;}", "b.js");
        a.getGlobalObject().defineBuiltin("other", b.getGlobalObject().get("other"));
        a.getGlobalObject().defineBuiltin("boom", b.getGlobalObject().get("boom"));
        a.getGlobalObject().defineBuiltin("values", b.getGlobalObject().get("values"));
        a.getGlobalObject().defineBuiltin("Maker", b.getGlobalObject().get("Maker"));
        Check.equal("a,b,b,b,b,b,a", Ops.toString(a.evaluateScript("var thrown;"
                + "try{boom();}catch(e){thrown=e;}"
                + "function strictTail(){'use strict';return other();}"
                + "[probe.active,other(),strictTail(),new Maker().active,values().next().value,thrown,probe.active].join()",
                "a.js")), "script, ordinary call, strict tail, construct, generator, throw, and restoration");
        Check.equal("b", Ops.toString(b.evaluateModule("case.mjs", "export const active=probe.active;").get("active")),
                "synchronous module execution");
        Check.isNull(RealmExecution.current(), "outside JavaScript no realm is bound to the thread");
    }

    /**
     * Reading and writing global variables, the names, and deleting.
     * <p>
     * 전역 변수 읽기와 쓰기, 이름, 삭제.
     */
    public void testGlobals() {
        JsEngine e = new JsEngine();
        e.put("n", Integer.valueOf(5));
        e.put("s", "text");
        e.put("b", Boolean.TRUE);
        e.put("nothing", null);
        e.put("undef", JsEngine.UNDEFINED);
        Check.equal(Double.valueOf(5), e.get("n"), "an Integer becomes a number that comes back as Double");
        Check.equal("text", e.get("s"), "string");
        Check.equal(Boolean.TRUE, e.get("b"), "boolean");
        Check.isNull(e.get("nothing"), "null");
        Check.isTrue(e.has("nothing"), "a global that holds null exists");
        Check.same(JsEngine.UNDEFINED, e.get("undef"), "a global that holds undefined");
        Check.isNull(e.get("missing"), "a missing global is null");
        Check.isFalse(e.has("missing"), "has of a missing global");
        Check.equal("5 text true", e.eval("n + ' ' + s + ' ' + b"), "scripts see the globals");
        e.eval("var made = 'by script'; function f() { return 1; } let lexical = 2;");
        Check.equal("by script", e.get("made"), "a var of a script is a global");
        Check.isTrue(e.isFunction("f"), "a function declaration is a global function");
        Check.isFalse(e.isFunction("n"), "a number is not a function");
        Check.isFalse(e.has("lexical"), "a global let is not a property of the global object");
        Check.equal(Double.valueOf(2), e.eval("lexical"), "but a script sees it");
        Check.isTrue(e.globalNames().contains("made") && e.globalNames().contains("n"), "enumerable globals: " + e.globalNames());
        Check.isFalse(e.globalNames().contains("Math"), "built-ins are not enumerable");
        Check.isTrue(e.get("Math") instanceof JsObjectHandle, "a built-in object is readable");
        Check.isTrue(e.remove("n"), "an assigned global can be deleted");
        Check.isFalse(e.has("n"), "and is gone");
        Check.isFalse(e.remove("made"), "a var of a script cannot be deleted");
    }

    /**
     * Calling global functions and methods with conversion of the arguments.
     * <p>
     * 인수를 변환하며 전역 함수와 메서드를 호출한다.
     */
    public void testCall() {
        final JsEngine e = new JsEngine();
        e.eval("function twice(x) { return x * 2; } function who() { return this === undefined ? 'undef' : typeof this; }"
                + " var calc = { base: 10, add: function(x) { return this.base + x; } };");
        Check.equal(Double.valueOf(42), e.call("twice", Integer.valueOf(21)), "call a global function");
        Check.equal(Double.valueOf(7.5), e.call("twice", Float.valueOf(3.75f)), "a Float argument");
        Check.equal(Double.valueOf(15), e.invokeMethod(e.get("calc"), "add", Integer.valueOf(5)), "call a method of an object");
        Check.equal(Double.valueOf(15), ((JsHandle) e.get("calc")).callMember("add", Integer.valueOf(5)), "callMember");
        Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.call("missing");
            }
        }, "calling a missing function is a TypeError of the script");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                e.invokeMethod("not a handle", "x");
            }
        }, "the target of invokeMethod must be a handle");
        Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.invokeMethod(e.get("calc"), "base");
            }
        }, "a property that is not a function cannot be called");
        Check.equal(Double.valueOf(3), e.call("eval", "1 + 2"), "the built-in eval is a function too");
    }

    /**
     * Checks malformed template escapes, raw text, and the exact position of substitution values.
     * <p>
     * 잘못된 템플릿 이스케이프, 원문 문자열, 치환 값의 위치를 확인한다.
     */
    public void testTaggedTemplateEscapes() {
        final JsEngine engine = new JsEngine();
        engine.eval("function tag(s){return s}");
        String[] invalid = { "\\01", "\\8", "\\9", "\\xG", "\\u{}", "\\u{110000}", "\\u0" };
        // 각 잘못된 이스케이프는 태그가 있을 때에만 허용하며 원문은 보존한다.
        for (String escape : invalid) {
            final String source = "`" + escape + "`";
            engine.put("rawEscape", escape);
            Check.equal(Boolean.TRUE, engine.eval("var t=tag" + source
                    + "; t[0] === undefined && t.hasOwnProperty(0) && t.raw[0] === rawEscape"), escape);
            JsScriptError error = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
                public void run() {
                    engine.compile(source, "template.js");
                }
            }, escape);
            Check.equal("SyntaxError", error.getErrorName(), escape);
        }
        Check.equal(Boolean.TRUE, engine.eval("function parts(s,a,b){return s[0]==='a\\n' && a===1 && s[1]===undefined"
                + " && s.raw[1]==='\\\\xG' && b===2 && s[2]==='b' && Object.isFrozen(s) && Object.isFrozen(s.raw);}"
                + " parts`a\r\n${1}\\xG${2}b`"), "independent cooked and raw segments");
    }

    /**
     * Verifies that alternating evaluations of the same parse node in two realms preserve both template identities.
     * <p>
     * 같은 파싱 노드를 두 realm에서 번갈아 실행해도 각 템플릿 객체가 유지되는지 확인한다.
     */
    public void testTaggedTemplateCachePerRealm() {
        JsEngine first = new JsEngine();
        JsEngine second = new JsEngine();
        String source = "function tag(s){return s} tag`site`";
        ParsedScript shared = first.compile(source, "shared.js");
        first.put("seen", first.eval(shared));
        second.put("seen", second.eval(shared));
        first.put("again", first.eval(shared));
        second.put("again", second.eval(shared));
        Check.equal(Boolean.TRUE, first.eval("seen === again"), "first realm keeps its template");
        Check.equal(Boolean.TRUE, second.eval("seen === again"), "second realm keeps its template");
        first.put("reparsed", first.eval(first.compile(source, "shared.js")));
        Check.equal(Boolean.FALSE, first.eval("seen === reparsed"), "separately parsed templates are distinct");
        Check.equal(Boolean.FALSE, first.eval("tag`site` === tag`site`"), "different source positions are distinct");
    }

    /**
     * A script that was parsed in advance runs any number of times, in any engine, and a syntax error is reported at once.
     * <p>
     * 미리 파싱한 스크립트는 몇 번이든, 어느 엔진에서든 실행되며, 구문 오류는 바로 보고된다.
     */
    public void testCompileOnceRunManyTimes() {
        final JsEngine e = new JsEngine();
        ParsedScript p = e.compile("var counter = (typeof counter === 'undefined' ? 0 : counter) + 1; counter", "count.js");
        Check.equal("count.js", p.getSourceName(), "name");
        Check.equal(Double.valueOf(1), e.eval(p), "first run");
        Check.equal(Double.valueOf(2), e.eval(p), "second run");
        JsEngine other = new JsEngine();
        Check.equal(Double.valueOf(1), other.eval(p), "another engine runs the same parsed script with its own state");
        ParsedScript tagged = e.compile("function tag(s) { return s; } var t = []; for (var i = 0; i < 2; i++) { t.push(tag`a${i}b`); } t[0] === t[1]", "tag.js");
        Check.equal(Boolean.TRUE, e.eval(tagged), "a tagged template site has one template object per realm");
        JsScriptError err = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.compile("var = 1;", "bad.js");
            }
        }, "a syntax error is thrown by compile");
        Check.equal("SyntaxError", err.getErrorName(), "syntax error name");
        Check.equal("bad.js", err.getSourceName(), "syntax error source");
        Check.equal(1, err.getLine(), "syntax error line");
        Check.throwsException(NullPointerException.class, new Block() {
            public void run() {
                e.compile(null, "x");
            }
        }, "null source");
    }

    /**
     * Errors of the script reach Java as {@link JsScriptError} with name, message and position, for syntax errors, runtime errors and
     * <code>throw</code>.
     * <p>
     * 스크립트의 오류는 이름, 메시지, 위치와 함께 {@link JsScriptError} 로 Java 에 닿는다. 구문 오류, 런타임 오류, <code>throw</code> 모두.
     */
    public void testErrorsHavePositions() {
        final JsEngine e = new JsEngine();
        JsScriptError syntax = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("var a = 1;\nvar b = ;\n", "syn.js");
            }
        }, "syntax error");
        Check.equal("SyntaxError", syntax.getErrorName(), "syntax name");
        Check.equal("syn.js", syntax.getSourceName(), "syntax source");
        Check.equal(2, syntax.getLine(), "syntax line");
        Check.isTrue(syntax.getColumn() > 0, "syntax column");
        Check.contains(syntax.getMessage(), "SyntaxError", "message has the name");
        Check.contains(syntax.getMessage(), "syn.js:2:", "message has the position");

        JsScriptError runtime = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("var ok = 1;\nvar o = null;\nvar v = o.field;\n", "run.js");
            }
        }, "runtime error");
        Check.equal("TypeError", runtime.getErrorName(), "runtime name");
        Check.equal("run.js", runtime.getSourceName(), "runtime source");
        Check.equal(3, runtime.getLine(), "runtime line");
        Check.isTrue(runtime.getColumn() > 0, "runtime column");

        JsScriptError thrown = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("\n\n  throw new RangeError('far out');", "thr.js");
            }
        }, "throw");
        Check.equal("RangeError", thrown.getErrorName(), "thrown name");
        Check.equal("far out", thrown.getErrorMessage(), "thrown message");
        Check.equal(3, thrown.getLine(), "thrown line");
        Check.isTrue(thrown.getThrownValue() instanceof JsObjectHandle, "the thrown error object is a handle");
        Check.equal("far out", ((JsObjectHandle) thrown.getThrownValue()).get("message"), "its message");

        JsScriptError value = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("throw 42;", "val.js");
            }
        }, "throw of a number");
        Check.equal(Double.valueOf(42), value.getThrownValue(), "the thrown number");
        Check.equal("Error", value.getErrorName(), "a non-object is reported as Error");
        Check.equal("42", value.getErrorMessage(), "and its text is the message");

        JsScriptError inFunction = (JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("function boom() {\n  return undefinedName;\n}\nboom();", "fn.js");
            }
        }, "reference error inside a function");
        Check.equal("ReferenceError", inFunction.getErrorName(), "reference error name");
        Check.equal(2, inFunction.getLine(), "the line inside the function");
        Check.isTrue(inFunction.getJsException() instanceof JsException, "the engine exception is available");
        Check.equal(7, ((JsScriptError) Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.eval("1;\n2;\n3;\n4;\n5;\n6;\nnull.x;", "last.js");
            }
        }, "line seven")).getLine(), "line of the seventh statement");
        Check.equal(Double.valueOf(1), e.eval("1"), "the engine is usable after errors");
    }

    /**
     * A script that has a catch block handles the error itself, so nothing reaches Java, and positions never change the messages that a script sees.
     * <p>
     * catch 블록이 있는 스크립트는 오류를 스스로 처리하므로 Java 에는 아무것도 닿지 않으며, 위치 정보는 스크립트가 보는 메시지를 바꾸지 않는다.
     */
    public void testCaughtErrorsStayInTheScript() {
        JsEngine e = new JsEngine();
        Check.equal("TypeError:Cannot read property 'x' of null", e.eval("try { null.x } catch (err) { err.name + ':' + err.message }"),
                "the message of a caught error has no position");
        Check.equal("boom", e.eval("try { throw new Error('boom') } catch (err) { err.message }"), "throw and catch");
    }

    /**
     * Promise jobs run before a top-level call returns, in the order of the specification.
     * <p>
     * promise 작업은 최상위 호출이 반환되기 전에 규격의 순서대로 실행된다.
     */
    public void testPromiseJobsRunAfterEachTopLevelOperation() {
        JsEngine e = new JsEngine();
        e.eval("var log = []; Promise.resolve(1).then(function (v) { log.push('then' + v); }); log.push('sync');");
        Check.equal("sync,then1", e.eval("log.join()"), "the job ran after the script, before eval returned");
        e.eval("var settled; Promise.resolve(5).then(function (v) { return v + 1; }).then(function (v) { settled = v; });");
        Check.equal(Double.valueOf(6), e.get("settled"), "a chain of promise reactions finishes before eval returns");
        e.eval("var q = []; Promise.resolve().then(function () { q.push(1); return Promise.resolve(); }).then(function () { q.push(3); });"
                + " Promise.resolve().then(function () { q.push(2); });");
        Check.equal("1,2,3", e.eval("q.join()"), "microtask order");
        e.eval("function later() { Promise.resolve().then(function () { done = true; }); } var done = false;");
        e.call("later");
        Check.equal(Boolean.TRUE, e.get("done"), "jobs created by call() also run");
    }

    /**
     * A promise that is rejected and that nobody handles is reported to the console hook after the jobs ran; a rejection that gets a handler in time
     * is not reported.
     * <p>
     * 거부되었고 아무도 처리하지 않은 promise 는 작업이 끝난 뒤 콘솔 연결에 보고된다. 제때 처리기를 얻은 거부는 보고하지 않는다.
     */
    public void testUnhandledRejectionsAreReported() {
        JsEngine e = new JsEngine();
        final List<String> lines = new ArrayList<String>();
        e.setConsole(new ConsoleHook() {
            public void message(String level, String text) {
                lines.add(level + ":" + text);
            }
        });
        e.eval("Promise.reject(new Error('nobody handles this'));");
        Check.equal("error:Uncaught (in promise) Error: nobody handles this", join(lines), "an unhandled rejection is reported as an error");
        lines.clear();
        e.eval("Promise.reject(1).catch(function () { });");
        e.eval("var late = Promise.reject(2); Promise.resolve().then(function () { late.catch(function () { }); });");
        Check.equal("", join(lines), "rejections that are handled (also during the same jobs) are not reported");
        e.eval("Promise.reject('plain text');");
        Check.equal("error:Uncaught (in promise) plain text", join(lines), "a rejection with a non-error reason");
    }

    /**
     * Modules: an exported value, an import through the loader, and the error for an import when no loader is set.
     * <p>
     * 모듈: export 한 값, 로더를 통한 import, 로더가 없을 때 import 의 오류.
     */
    public void testModules() {
        final JsEngine e = new JsEngine();
        JsObjectHandle ns = e.evalModule("self.js", "export var answer = 42; export function hi(n) { return 'hi ' + n; } export default 'dflt';");
        Check.equal(Double.valueOf(42), ns.get("answer"), "export");
        Check.equal("dflt", ns.get("default"), "default export");
        Check.equal("hi you", ((JsFunctionHandle) ns.get("hi")).call("you"), "an exported function");
        Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.evalModule("needs.js", "import {x} from 'dep.js'; export var y = x;");
            }
        }, "an import without a module loader fails");
        final Map<String, String> sources = new HashMap<String, String>();
        sources.put("lib/a.js", "import {b} from './b.js'; export var a = 'a' + b;");
        sources.put("lib/b.js", "export var b = 'b';");
        e.setModuleLoader(new ModuleLoader() {
            public String resolve(String specifier, String referrer) {
                if (specifier.startsWith("./") && referrer != null) {
                    return referrer.substring(0, referrer.lastIndexOf('/') + 1) + specifier.substring(2);
                }
                return specifier;
            }

            public String fetch(String key) {
                String s = sources.get(key);
                if (s == null) {
                    throw JsException.typeError("no such module: " + key);
                }
                return s;
            }
        });
        Check.equal("ab", e.evalModule("lib/a.js", null).get("a"), "modules imported through the loader");
        Check.equal("ab", e.evalModule("lib/a.js", null).get("a"), "the same key is the same module");
        Check.throwsException(JsScriptError.class, new Block() {
            public void run() {
                e.evalModule("lib/missing.js", null);
            }
        }, "the loader reports a failure");
    }

    /**
     * The output of <code>console</code> and <code>print</code> goes to the console hook, by level.
     * <p>
     * <code>console</code> 과 <code>print</code> 의 출력은 수준별로 콘솔 연결로 간다.
     */
    public void testConsoleHook() {
        JsEngine e = new JsEngine();
        final List<String> lines = new ArrayList<String>();
        e.setConsole(new ConsoleHook() {
            public void message(String level, String text) {
                lines.add(level + ":" + text);
            }
        });
        e.eval("console.log('a', 1); console.error('bad'); console.warn('w'); console.info('i'); print('p', 2);");
        Check.equal("log:a 1,error:bad,warn:w,info:i,log:p 2", join(lines), "console output by level");
        Check.isTrue(e.getConsole() != null, "the hook is readable");
    }

    /**
     * Joins strings with commas.
     * <p>
     * 문자열을 쉼표로 잇는다.
     *
     * @param items the strings / 문자열
     * @return the joined text / 이은 문자열
     */
    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        // 항목을 쉼표로 구분해 하나씩 붙인다.
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    /**
     * An endless loop is stopped by the step limit, and by the time limit; the engine works again afterwards. A call that Java starts is a task of
     * its own, so the limits apply to calls too.
     * <p>
     * 끝없는 반복은 단계 수 제한과 시간 제한으로 멈추며, 그 뒤에도 엔진은 동작한다. Java 가 시작하는 호출도 작업 하나이므로 제한이 호출에도 적용된다.
     */
    public void testLimitsStopRunawayScripts() {
        final JsEngine e = new JsEngine();
        e.getLimits().setMaxSteps(100000L);
        ScriptAbortedException steps = (ScriptAbortedException) Check.throwsException(ScriptAbortedException.class, new Block() {
            public void run() {
                e.eval("while (true) { }");
            }
        }, "step limit");
        Check.equal(ScriptAbortedException.Reason.STEP_LIMIT, steps.getReason(), "step limit reason");
        Check.equal(Double.valueOf(2), e.eval("1 + 1"), "the next task starts with fresh counters");
        e.eval("function spin() { while (true) { } }");
        Check.throwsException(ScriptAbortedException.class, new Block() {
            public void run() {
                e.call("spin");
            }
        }, "the step limit applies to calls from Java");
        e.getLimits().setMaxSteps(0L);
        e.getLimits().setMaxTimeMillis(150L);
        ScriptAbortedException time = (ScriptAbortedException) Check.throwsException(ScriptAbortedException.class, new Block() {
            public void run() {
                e.eval("while (true) { }");
            }
        }, "time limit");
        Check.equal(ScriptAbortedException.Reason.TIME_LIMIT, time.getReason(), "time limit reason");
        e.getLimits().setMaxTimeMillis(0L);
        e.getLimits().setMaxSteps(ExecutionLimits.DEFAULT_MAX_STEPS);
        Check.equal(Double.valueOf(3), e.eval("1 + 2"), "usable after the abort");
    }

    /**
     * {@link JsEngine#cancel()} may be called from another thread without locking and stops the running script.
     * <p>
     * {@link JsEngine#cancel()} 은 다른 스레드에서 잠금 없이 호출할 수 있으며 실행 중인 스크립트를 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelFromAnotherThread() throws Exception {
        final JsEngine e = new JsEngine();
        Thread canceller = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ex) {
                    return;
                }
                e.cancel();
            }
        }, "swingwebpanel-test-canceller");
        canceller.setDaemon(true);
        canceller.start();
        ScriptAbortedException cancelled = (ScriptAbortedException) Check.throwsException(ScriptAbortedException.class, new Block() {
            public void run() {
                e.eval("while (true) { }");
            }
        }, "cancel");
        Check.equal(ScriptAbortedException.Reason.CANCELLED, cancelled.getReason(), "cancel reason");
        canceller.join(5000L);
        e.resetCancel();
        Check.equal(Double.valueOf(1), e.eval("1"), "usable after resetCancel");
    }

    /**
     * Several threads may use one engine: the operations are serialized, so a counter that scripts increment in parallel is exact.
     * <p>
     * 여러 스레드가 한 엔진을 쓸 수 있다. 연산이 직렬화되므로 스크립트가 병렬로 올리는 카운터가 정확하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOneEngineFromManyThreads() throws Exception {
        final JsEngine e = new JsEngine();
        e.eval("var count = 0; function inc() { var c = count; count = c + 1; }");
        final int threads = 4;
        final int each = 300;
        final Throwable[] failure = new Throwable[1];
        Thread[] workers = new Thread[threads];
        // 스레드마다 같은 함수를 여러 번 호출한다.
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(new Runnable() {
                public void run() {
                    try {
                        // 정해진 횟수만큼 증가 함수를 호출한다.
                        for (int i = 0; i < each; i++) {
                            e.call("inc");
                        }
                    } catch (Throwable ex) {
                        failure[0] = ex;
                    }
                }
            }, "swingwebpanel-test-worker-" + t);
            workers[t].setDaemon(true);
            workers[t].start();
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (int t = 0; t < threads; t++) {
            workers[t].join(30000L);
        }
        Check.isNull(failure[0], "no failure: " + failure[0]);
        Check.equal(Double.valueOf(threads * each), e.get("count"), "every increment counted once");
    }
}
