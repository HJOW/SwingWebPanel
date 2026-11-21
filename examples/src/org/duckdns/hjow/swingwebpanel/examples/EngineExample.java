package org.duckdns.hjow.swingwebpanel.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.embed.JsArrayHandle;
import org.duckdns.hjow.swingwebpanel.js.embed.JsCallable;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction;
import org.duckdns.hjow.swingwebpanel.js.embed.JsObjectHandle;
import org.duckdns.hjow.swingwebpanel.js.embed.JsScriptError;

/**
 * Shows the stand-alone JavaScript engine {@link JsEngine} without any browser component, window or AWT class: running an ECMAScript 2015 script, giving
 * values and a Java function to it, reading results as live <code>Map</code> and <code>List</code> views, calling a script function from Java, promise jobs, a
 * timer that the host implements itself (a stand-alone engine has no event loop), the position of a script error, and a time limit that stops an endless
 * loop. The program needs only the engine classes: <code>SWP_CLASSES=build/engine-classes examples/run.sh EngineExample</code>. With
 * <code>--smoke</code> it checks every result and prints one line (used for automatic checks); the exit code is 1 if a check fails.
 * <p>
 * 브라우저 컴포넌트, 창, AWT 클래스 없이 독립 JavaScript 엔진 {@link JsEngine} 을 쓰는 방법을 보여 준다: ECMAScript 2015 스크립트 실행, 스크립트에 값과 Java 함수 주기,
 * 결과를 살아 있는 <code>Map</code>, <code>List</code> 보기로 읽기, Java 에서 스크립트 함수 호출, promise 작업, 호스트가 직접 구현하는 타이머(독립 엔진에는 이벤트
 * 루프가 없다), 스크립트 오류의 위치, 끝없는 반복을 멈추는 시간 제한. 이 프로그램은 엔진 클래스만 있으면 된다:
 * <code>SWP_CLASSES=build/engine-classes examples/run.sh EngineExample</code>. <code>--smoke</code> 를 주면 모든 결과를 확인하고 한 줄을 출력하며(자동 확인용) 확인이
 * 실패하면 종료 코드가 1 이다.
 */
public final class EngineExample {

    /**
     * Private constructor: this class only has a main method.
     * <p>
     * 비공개 생성자: 이 클래스에는 main 메서드만 있다.
     */
    private EngineExample() {
    }

    /**
     * Program entry point.
     * <p>
     * 프로그램 진입점.
     *
     * @param args <code>--smoke</code> for the automatic check / 자동 확인이면 <code>--smoke</code>
     */
    public static void main(String[] args) {
        boolean smoke = args.length > 0 && "--smoke".equals(args[0]);
        List<String> problems = new ArrayList<String>();
        JsEngine engine = new JsEngine();
        final List<String> output = new ArrayList<String>();
        // console 과 print 의 출력을 목록에 모은다. 기본 연결은 표준 출력에 쓴다.
        engine.setConsole(new ConsoleHook() {
            public void message(String level, String text) {
                output.add(level + ": " + text);
            }
        });

        // 1) 값을 주고 ES6 스크립트를 실행한다.
        engine.put("name", "world");
        Object greeting = engine.eval("`hello ${name}`.toUpperCase()", "greeting.js");
        expect(problems, "HELLO WORLD", greeting, "template string");

        // 2) Java 함수를 스크립트에 준다. 스크립트가 Java 코드에 닿는 유일한 방법이다.
        engine.putFunction("add", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                return Double.valueOf(((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue());
            }
        });
        expect(problems, Double.valueOf(7), engine.eval("add(3, 4)"), "host function");

        // 3) 결과를 살아 있는 Map 과 List 로 읽는다. 숫자는 항상 Double 이다.
        JsObjectHandle config = (JsObjectHandle) engine.eval("({title: 'demo', sizes: [1, 2, 3], nested: {ok: true}})");
        JsArrayHandle sizes = (JsArrayHandle) config.get("sizes");
        sizes.add(Integer.valueOf(4));
        expect(problems, "demo", config.get("title"), "Map view");
        engine.put("config", config);
        expect(problems, "1,2,3,4", engine.eval("config.sizes.join()"), "the script sees the change made through the List view");
        Object copy = engine.toPlain(config);
        expect(problems, Boolean.TRUE, Boolean.valueOf(copy instanceof LinkedHashMap), "a plain copy");

        // 4) Java 에서 스크립트 함수를 호출한다.
        engine.eval("function twice(x) { return x * 2; }");
        expect(problems, Double.valueOf(42), engine.call("twice", Integer.valueOf(21)), "call a global function");
        JsCallable twice = (JsCallable) engine.get("twice");
        expect(problems, Double.valueOf(10), twice.call(Integer.valueOf(5)), "call through a function handle");

        // 5) promise 작업은 최상위 호출이 끝나기 전에 실행된다.
        engine.eval("var steps = []; Promise.resolve(1).then(function (v) { steps.push('then ' + v); }); steps.push('sync');");
        expect(problems, "sync,then 1", engine.eval("steps.join()"), "promise jobs");

        // 6) 타이머는 독립 엔진에 없다. 호스트가 자기 함수로 만들고 자기 루프에서 호출한다.
        final List<Object> pending = new ArrayList<Object>();
        engine.putFunction("setTimeout", new JsHostFunction() {
            public Object call(Object thisValue, Object[] args) {
                pending.add(args[0]);
                return Integer.valueOf(pending.size());
            }
        });
        engine.eval("var ticks = 0; setTimeout(function () { ticks++; }, 10); setTimeout(function () { ticks += 10; }, 20);");
        // 호스트의 루프: 모아 둔 콜백을 차례로 호출한다.
        for (int i = 0; i < pending.size(); i++) {
            ((JsCallable) pending.get(i)).call();
        }
        expect(problems, Double.valueOf(11), engine.get("ticks"), "a timer implemented by the host");

        // 7) 오류에는 이름, 메시지, 스크립트 이름과 줄, 열이 있다.
        try {
            engine.eval("var a = 1;\nvar b = a.field.x;\n", "broken.js");
            problems.add("no error for a.field.x");
        } catch (JsScriptError e) {
            expect(problems, "TypeError", e.getErrorName(), "error name");
            expect(problems, "broken.js", e.getSourceName(), "error source");
            expect(problems, Integer.valueOf(2), Integer.valueOf(e.getLine()), "error line");
        }

        // 8) 시간 제한이 끝없는 반복을 멈춘다. 제한은 스크립트가 잡을 수 없다.
        engine.getLimits().setMaxTimeMillis(200L);
        try {
            engine.eval("while (true) { }");
            problems.add("the endless loop was not stopped");
        } catch (ScriptAbortedException e) {
            expect(problems, ScriptAbortedException.Reason.TIME_LIMIT, e.getReason(), "time limit");
        }
        engine.getLimits().setMaxTimeMillis(0L);

        // 9) console 출력은 연결로 간다.
        engine.eval("console.log('a', 1); console.warn('careful'); print('printed');");
        expect(problems, Arrays.asList("log: a 1", "warn: careful", "log: printed"), output, "console output");

        if (smoke) {
            if (problems.isEmpty()) {
                System.out.println("smoke OK engine=" + JsEngine.class.getName() + " greeting=" + greeting + " output=" + output.size());
            } else {
                System.out.println("smoke FAILED " + problems);
                System.exit(1);
            }
            return;
        }
        System.out.println("greeting     : " + greeting);
        System.out.println("config copy  : " + copy);
        System.out.println("console lines: " + output);
        System.out.println(problems.isEmpty() ? "all checks passed" : "problems: " + problems);
    }

    /**
     * Compares an expected and an actual value and records a problem.
     * <p>
     * 기대한 값과 실제 값을 비교하고 다르면 문제로 기록한다.
     *
     * @param problems the list of problems / 문제 목록
     * @param expected the expected value / 기대한 값
     * @param actual   the actual value / 실제 값
     * @param what     what is checked / 확인하는 것
     */
    private static void expect(List<String> problems, Object expected, Object actual, String what) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            problems.add(what + ": expected " + expected + " but was " + actual);
        }
    }
}
