package org.duckdns.hjow.swingwebpanel.examples.script;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction;

/**
 * Shows the engine of this project through the standard Java Scripting API (JSR 223, <code>javax.script</code>) in a program that has no other
 * dependency than the engine classes: the engine is found by its <b>unique</b> name <code>swingwebpanel-js</code> (the common name
 * <code>javascript</code> belongs to the engine of the Java runtime, Rhino or Nashorn, if it has one, and is never answered by this engine), then the example
 * evaluates scripts, uses <code>Bindings</code> and the writer of the context, compiles a script, calls functions and methods through <code>Invocable</code>, implements a Java
 * interface with script functions, and reads the position of a <code>ScriptException</code>. Run it with
 * <code>SWP_CLASSES=build/engine-classes examples/run.sh script.ScriptEngineExample</code>; with <code>--smoke</code> it checks every result and prints one line, and the
 * exit code is 1 if a check fails.
 * <p>
 * 이 프로젝트의 엔진을 표준 Java Scripting API(JSR 223, <code>javax.script</code>)로 쓰는 방법을 엔진 클래스 외에는 의존하는 것이 없는 프로그램으로 보여 준다. 엔진은
 * <b>고유한</b> 이름 <code>swingwebpanel-js</code> 로 찾는다(흔한 이름 <code>javascript</code> 는 Java 런타임의 엔진, 즉 Rhino 나 Nashorn 이 있으면 그것의 몫이며 이 엔진은 절대 대답하지
 * 않는다). 그 다음 스크립트를 평가하고, <code>Bindings</code> 와 컨텍스트의 라이터를 쓰고, 스크립트를 컴파일하고, <code>Invocable</code> 로 함수와 메서드를 호출하고, 스크립트 함수로
 * Java 인터페이스를 구현하고, <code>ScriptException</code> 의 위치를 읽는다. <code>SWP_CLASSES=build/engine-classes examples/run.sh script.ScriptEngineExample</code> 로 실행하며,
 * <code>--smoke</code> 를 주면 모든 결과를 확인하고 한 줄을 출력하며 확인이 실패하면 종료 코드가 1 이다.
 */
public final class ScriptEngineExample {

    /**
     * Private constructor: this class only has a main method.
     * <p>
     * 비공개 생성자: 이 클래스에는 main 메서드만 있다.
     */
    private ScriptEngineExample() {
    }

    /**
     * Program entry point.
     * <p>
     * 프로그램 진입점.
     *
     * @param args <code>--smoke</code> for the automatic check / 자동 확인이면 <code>--smoke</code>
     * @throws Exception on failure / 실패 시
     */
    public static void main(String[] args) throws Exception {
        boolean smoke = args.length > 0 && "--smoke".equals(args[0]);
        List<String> problems = new ArrayList<String>();
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("swingwebpanel-js");
        if (engine == null) {
            System.out.println("the engine swingwebpanel-js is not on the class path (is META-INF/services in the jar or the class directory?)");
            System.exit(1);
        }
        String engineClass = engine.getClass().getName();
        expect(problems, Boolean.TRUE, Boolean.valueOf(engineClass.startsWith("org.duckdns.hjow.swingwebpanel.js.script.")), "the engine is the one of this project: " + engineClass);

        // 1) 문자열을 평가한다. 숫자는 항상 Double 이고 undefined 는 null 이다.
        expect(problems, Double.valueOf(42), engine.eval("(x => x * 2)(21)"), "eval");

        // 2) 엔진 스코프 바인딩은 전역 객체의 살아 있는 보기이다.
        engine.put("who", "world");
        expect(problems, "hello world", engine.eval("'hello ' + who"), "put and eval");
        engine.eval("var counter = 1; function bump() { return ++counter; }");
        expect(problems, Double.valueOf(1), engine.get("counter"), "get");

        // 3) 컨텍스트의 라이터가 print 와 console 의 출력을 받는다.
        StringWriter out = new StringWriter();
        engine.getContext().setWriter(out);
        engine.eval("print('printed'); console.log('logged', 2);");
        String nl = System.getProperty("line.separator");
        expect(problems, "printed" + nl + "logged 2" + nl, out.toString(), "writer of the context");

        // 4) 다른 바인딩은 자신만의 전역 스코프를 가진다.
        Bindings own = engine.createBindings();
        own.put("x", Integer.valueOf(20));
        expect(problems, Double.valueOf(22), engine.eval("x + 2", own), "other bindings");
        expect(problems, "undefined", engine.eval("typeof x"), "the default scope does not see them");

        // 5) 한 번 컴파일해서 여러 번 실행한다.
        CompiledScript compiled = ((Compilable) engine).compile("typeof n === 'undefined' ? (n = 1) : ++n");
        compiled.eval();
        expect(problems, Double.valueOf(2), compiled.eval(), "compiled script");

        // 6) Invocable: 함수 호출과 Java 인터페이스 구현.
        Invocable invocable = (Invocable) engine;
        expect(problems, Double.valueOf(2), invocable.invokeFunction("bump"), "invokeFunction");
        engine.eval("function run() { ran = true; } var ran = false;");
        Runnable runnable = invocable.getInterface(Runnable.class);
        runnable.run();
        expect(problems, Boolean.TRUE, engine.get("ran"), "getInterface");

        // 7) 스크립트가 Java 코드를 부르게 하려면 JsHostFunction 을 바인딩에 넣는다.
        engine.put("javaAdd", new JsHostFunction() {
            public Object call(Object thisValue, Object[] callArgs) {
                return Double.valueOf(((Number) callArgs[0]).doubleValue() + ((Number) callArgs[1]).doubleValue());
            }
        });
        expect(problems, Double.valueOf(5), engine.eval("javaAdd(2, 3)"), "host function");

        // 8) ScriptException 에는 파일 이름, 줄, 열이 있다.
        engine.getContext().setAttribute(ScriptEngine.FILENAME, "demo.js", ScriptContext.ENGINE_SCOPE);
        try {
            engine.eval("var a = 1;\nvar b = a.missing.deep;\n");
            problems.add("no error");
        } catch (ScriptException e) {
            expect(problems, "demo.js", e.getFileName(), "file name of the error");
            expect(problems, Integer.valueOf(2), Integer.valueOf(e.getLineNumber()), "line of the error");
        }

        // 9) 엔진은 서로 독립이다.
        ScriptEngine other = new ScriptEngineManager().getEngineByName("swingwebpanel-js");
        expect(problems, "undefined", other.eval("typeof counter"), "a second engine shares nothing");

        if (smoke) {
            if (problems.isEmpty()) {
                System.out.println("smoke OK engine=" + engineClass + " version=" + engine.getFactory().getEngineVersion());
            } else {
                System.out.println("smoke FAILED " + problems);
                System.exit(1);
            }
            return;
        }
        System.out.println("engine : " + engine.getFactory().getEngineName() + " " + engine.getFactory().getEngineVersion() + " (" + engineClass + ")");
        System.out.println("names  : " + engine.getFactory().getNames());
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
