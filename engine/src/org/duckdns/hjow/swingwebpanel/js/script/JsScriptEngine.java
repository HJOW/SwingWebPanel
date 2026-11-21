package org.duckdns.hjow.swingwebpanel.js.script;

import java.io.IOException;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ParsedScript;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHandle;
import org.duckdns.hjow.swingwebpanel.js.embed.JsScriptError;

/**
 * The Java Scripting API (JSR 223) engine of this project, built on {@link JsEngine}: it implements <code>ScriptEngine</code> (through
 * <code>AbstractScriptEngine</code>), <code>Compilable</code> and <code>Invocable</code>. Obtain it with
 * <code>new ScriptEngineManager().getEngineByName("swingwebpanel-js")</code> or from {@link JsScriptEngineFactory#getScriptEngine()}.
 * <h3>Variables and contexts</h3>
 * The <code>ENGINE_SCOPE</code> bindings of the default context are a live view of the global object ({@link JsGlobalBindings}):
 * <code>engine.put("x", 1)</code> makes <code>x</code> a global variable, and the <code>var</code>s and functions that a script declares are visible through
 * <code>engine.get("x")</code>. When <code>eval</code> gets other <code>ENGINE_SCOPE</code> bindings (a <code>Bindings</code> argument, or a context that was given
 * other bindings), those bindings get a global scope of their own: this script engine keeps one complete, independent global object (realm) for every
 * <code>Bindings</code> object it has seen, copies the entries of the bindings into it before the script runs, and copies the globals that the script
 * created or changed back afterwards; the same <code>Bindings</code> object therefore keeps its variables from one <code>eval</code> to the next, and different
 * bindings never see each other's variables. The <code>GLOBAL_SCOPE</code> bindings (shared by the engines of a <code>ScriptEngineManager</code>) are consulted for
 * a name that the script scope does not have; they are read only (an assignment goes to the engine scope). Values follow the conversion rules of
 * {@link JsEngine}, except that undefined becomes <code>null</code>; a Java object that is not convertible is refused (<code>ScriptException</code> for an
 * <code>eval</code>), because scripts never get Java objects. To let scripts call Java code put a {@link org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction}
 * into the bindings. <code>let</code>, <code>const</code> and <code>class</code> declarations at the top level of a script are not entries of the bindings (they live
 * in the declarative scope of the realm), but later scripts of the same scope see them; declaring the same name twice is a SyntaxError, as in a browser.
 * <h3>Output</h3>
 * <code>print</code> and <code>console.log/info/debug</code> write a line to <code>ScriptContext.getWriter()</code>, <code>console.warn/error</code> to
 * <code>getErrorWriter()</code> of the context that runs the script.
 * <h3>Errors</h3>
 * An error of the script is a <code>ScriptException</code> with the message <code>Name: message</code>, the script name (the attribute
 * <code>ScriptEngine.FILENAME</code> of the context, or <code>&lt;eval&gt;</code>), the line and the column (-1 when unknown) and the {@link JsScriptError} as its cause.
 * A script that the engine stopped (see {@link #getJsEngine()} for the limits) is a <code>ScriptException</code> whose cause is the
 * {@link ScriptAbortedException}.
 * <h3>Results</h3>
 * The result of <code>eval</code> is the completion value converted by the rules of {@link JsEngine}: a number is always a <code>Double</code>, an object is a live
 * <code>Map</code>, an array a live <code>List</code>, a function a {@link org.duckdns.hjow.swingwebpanel.js.embed.JsCallable}; <code>undefined</code> and a script without a
 * value give <code>null</code>. Promise jobs run before <code>eval</code> returns; there are no timers.
 * <h3>Threads</h3>
 * The engine may be used from several threads (<code>THREADING</code> is <code>MULTITHREADED</code>); the operations on one global scope are serialized.
 * <p>
 * 이 프로젝트의 Java Scripting API(JSR 223) 엔진. {@link JsEngine} 위에 만들었으며 <code>ScriptEngine</code>(<code>AbstractScriptEngine</code> 로),
 * <code>Compilable</code>, <code>Invocable</code> 을 구현한다. <code>new ScriptEngineManager().getEngineByName("swingwebpanel-js")</code> 나
 * {@link JsScriptEngineFactory#getScriptEngine()} 으로 얻는다.
 * <h3>변수와 컨텍스트</h3>
 * 기본 컨텍스트의 <code>ENGINE_SCOPE</code> 바인딩은 전역 객체의 살아 있는 보기({@link JsGlobalBindings})이다. <code>engine.put("x", 1)</code> 은 <code>x</code> 를 전역
 * 변수로 만들고, 스크립트가 선언한 <code>var</code> 와 함수는 <code>engine.get("x")</code> 로 보인다. <code>eval</code> 이 다른 <code>ENGINE_SCOPE</code> 바인딩(<code>Bindings</code>
 * 인수, 또는 다른 바인딩을 준 컨텍스트)을 받으면 그 바인딩은 자신만의 전역 스코프를 가진다. 이 스크립트 엔진은 본 <code>Bindings</code> 객체마다 완전하고 독립된
 * 전역 객체(realm)를 하나씩 두고, 스크립트를 실행하기 전에 바인딩의 항목을 그 안으로 복사하고, 실행한 뒤 스크립트가 만들거나 바꾼 전역을 다시 복사해 온다. 그래서
 * 같은 <code>Bindings</code> 객체는 <code>eval</code> 사이에 변수를 유지하고 다른 바인딩은 서로의 변수를 보지 못한다. <code>GLOBAL_SCOPE</code> 바인딩
 * (<code>ScriptEngineManager</code> 의 엔진들이 공유)은 스크립트 스코프에 없는 이름을 찾을 때 참조하며 읽기 전용이다(대입은 엔진 스코프로 간다). 값은 {@link JsEngine}
 * 의 변환 규칙을 따르되 undefined 는 <code>null</code> 이 된다. 변환할 수 없는 Java 객체는 거부한다(<code>eval</code> 에서는 <code>ScriptException</code>). 스크립트는
 * Java 객체를 받지 않기 때문이다. 스크립트가 Java 코드를 호출하게 하려면 바인딩에 {@link org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction} 을 넣는다. 스크립트 최상위의
 * <code>let</code>, <code>const</code>, <code>class</code> 선언은 바인딩의 항목이 아니지만(realm 의 선언적 스코프에 있다) 같은 스코프의 이후 스크립트는 그것을 본다. 같은 이름을
 * 두 번 선언하면 브라우저처럼 SyntaxError 이다.
 * <h3>출력</h3>
 * <code>print</code> 와 <code>console.log/info/debug</code> 는 스크립트를 실행하는 컨텍스트의 <code>ScriptContext.getWriter()</code> 에, <code>console.warn/error</code> 는
 * <code>getErrorWriter()</code> 에 한 줄씩 쓴다.
 * <h3>오류</h3>
 * 스크립트의 오류는 <code>Name: message</code> 형태의 메시지, 스크립트 이름(컨텍스트의 <code>ScriptEngine.FILENAME</code> 속성, 없으면 <code>&lt;eval&gt;</code>), 줄과 열(모르면 -1),
 * 원인 {@link JsScriptError} 를 가진 <code>ScriptException</code> 이다. 엔진이 멈춘 스크립트(제한은 {@link #getJsEngine()} 참조)는 원인이 {@link ScriptAbortedException} 인
 * <code>ScriptException</code> 이다.
 * <h3>결과</h3>
 * <code>eval</code> 의 결과는 {@link JsEngine} 의 규칙으로 바꾼 완료 값이다: 숫자는 항상 <code>Double</code>, 객체는 살아 있는 <code>Map</code>, 배열은 살아 있는
 * <code>List</code>, 함수는 {@link org.duckdns.hjow.swingwebpanel.js.embed.JsCallable}. <code>undefined</code> 와 값이 없는 스크립트는 <code>null</code> 이다. promise 작업은
 * <code>eval</code> 이 반환하기 전에 실행되며 타이머는 없다.
 * <h3>스레드</h3>
 * 엔진은 여러 스레드에서 쓸 수 있다(<code>THREADING</code> 은 <code>MULTITHREADED</code>). 같은 전역 스코프의 연산은 직렬화된다.
 */
public final class JsScriptEngine extends AbstractScriptEngine implements Compilable, Invocable {

    /**
     * The name that the Java Scripting API reports for a script without a file name.
     * <p>
     * 파일 이름이 없는 스크립트에 대해 Java Scripting API 가 보고하는 이름.
     */
    public static final String DEFAULT_FILE_NAME = JsEngine.DEFAULT_SCRIPT_NAME;

    /**
     * One global scope: an engine and the console hook that serves the context that is running in it.
     * <p>
     * 전역 스코프 하나: 엔진과, 그 안에서 실행되는 컨텍스트를 위한 콘솔 연결.
     */
    private static final class Scope {

        /**
         * The engine of the scope.
         * <p>
         * 스코프의 엔진.
         */
        final JsEngine engine;

        /**
         * The console hook of the engine.
         * <p>
         * 엔진의 콘솔 연결.
         */
        final ContextConsole console;

        /**
         * True for the scope of the live <code>ENGINE_SCOPE</code> bindings of the default context (no copying is needed).
         * <p>
         * 기본 컨텍스트의 살아 있는 <code>ENGINE_SCOPE</code> 바인딩의 스코프이면 true(복사가 필요 없다).
         */
        final boolean live;

        /**
         * The lookup of the global bindings of the context that runs a script in this scope now, or null; kept so that a script that runs another script
         * (a host function that calls <code>eval</code>) gets the lookup of the outer script back when the inner one ends. Guarded by the lock of the engine.
         * <p>
         * 지금 이 스코프에서 스크립트를 실행하는 컨텍스트의 전역 바인딩 조회. 없으면 null. 스크립트가 다른 스크립트를 실행하는 경우(<code>eval</code> 을 부르는
         * 호스트 함수) 안쪽이 끝나면 바깥 스크립트의 조회를 되돌려 주려고 기억한다. 엔진의 잠금으로 보호한다.
         */
        JsRealm.NamedGlobals lookup;

        /**
         * Creates a scope.
         * <p>
         * 스코프를 만든다.
         *
         * @param engine  the engine / 엔진
         * @param console the console hook / 콘솔 연결
         * @param live    whether the bindings are live / 바인딩이 살아 있는지
         */
        Scope(JsEngine engine, ContextConsole console, boolean live) {
            this.engine = engine;
            this.console = console;
            this.live = live;
        }
    }

    /**
     * The scope of a <code>Bindings</code> object, kept as long as the bindings object lives (a weak reference to it).
     * <p>
     * <code>Bindings</code> 객체의 스코프. 바인딩 객체가 사는 동안 유지한다(그것에 대한 약한 참조).
     */
    private static final class ScopedEngine extends WeakReference<Bindings> {

        /**
         * The scope.
         * <p>
         * 스코프.
         */
        final Scope scope;

        /**
         * Creates the entry.
         * <p>
         * 항목을 만든다.
         *
         * @param bindings the bindings / 바인딩
         * @param scope    the scope / 스코프
         * @param queue    the queue that learns when the bindings are collected / 바인딩이 수거될 때 알려 받는 큐
         */
        ScopedEngine(Bindings bindings, Scope scope, ReferenceQueue<Bindings> queue) {
            super(bindings, queue);
            this.scope = scope;
        }
    }

    /**
     * Looks a name up in the <code>GLOBAL_SCOPE</code> bindings of a context for the script (see {@link JsRealm.NamedGlobals}).
     * <p>
     * 스크립트를 위해 컨텍스트의 <code>GLOBAL_SCOPE</code> 바인딩에서 이름을 찾는다({@link JsRealm.NamedGlobals} 참조).
     */
    private static final class GlobalScopeLookup implements JsRealm.NamedGlobals {

        /**
         * The engine that runs the script.
         * <p>
         * 스크립트를 실행하는 엔진.
         */
        private final JsEngine engine;

        /**
         * The context whose global bindings are searched.
         * <p>
         * 전역 바인딩을 찾을 컨텍스트.
         */
        private final ScriptContext context;

        /**
         * Creates the lookup.
         * <p>
         * 조회를 만든다.
         *
         * @param engine  the engine / 엔진
         * @param context the context / 컨텍스트
         */
        GlobalScopeLookup(JsEngine engine, ScriptContext context) {
            this.engine = engine;
            this.context = context;
        }

        /**
         * Finds a name in the global bindings.
         * <p>
         * 이름을 전역 바인딩에서 찾는다.
         *
         * @param name the name / 이름
         * @return the script value, or null if the bindings have no such name / 스크립트 값. 바인딩에 그런 이름이 없으면 null
         */
        public JsValue lookup(String name) {
            Bindings g = context.getBindings(ScriptContext.GLOBAL_SCOPE);
            if (g == null || name.length() == 0 || !g.containsKey(name)) {
                return null;
            }
            try {
                return engine.toScriptValue(g.get(name));
            } catch (IllegalArgumentException e) {
                throw JsException.typeError("global binding '" + name + "': " + e.getMessage());
            }
        }
    }

    /**
     * The factory that created this engine.
     * <p>
     * 이 엔진을 만든 팩터리.
     */
    private final JsScriptEngineFactory factory;

    /**
     * The scope of the live bindings of the default context.
     * <p>
     * 기본 컨텍스트의 살아 있는 바인딩의 스코프.
     */
    private final Scope primary;

    /**
     * The scopes of the other bindings that were used with this engine (guarded by itself).
     * <p>
     * 이 엔진에서 쓴 다른 바인딩의 스코프(자기 자신으로 동기화).
     */
    private final List<ScopedEngine> scopes = new ArrayList<ScopedEngine>();

    /**
     * Learns which bindings objects were collected.
     * <p>
     * 수거된 바인딩 객체를 알려 받는다.
     */
    private final ReferenceQueue<Bindings> collected = new ReferenceQueue<Bindings>();

    /**
     * Creates the engine (use the factory or the <code>ScriptEngineManager</code>).
     * <p>
     * 엔진을 만든다(팩터리나 <code>ScriptEngineManager</code> 를 쓴다).
     *
     * @param factory the factory / 팩터리
     */
    JsScriptEngine(JsScriptEngineFactory factory) {
        this.factory = factory;
        JsEngine engine = new JsEngine();
        ContextConsole console = new ContextConsole(this);
        engine.setConsole(console);
        this.primary = new Scope(engine, console, true);
        context.setBindings(new JsGlobalBindings(engine), ScriptContext.ENGINE_SCOPE);
    }

    /**
     * Returns the {@link JsEngine} of the default global scope: the place to set execution limits ({@link JsEngine#getLimits()}), a time limit before
     * running code that is not trusted, the module loader, or to cancel a script from another thread ({@link JsEngine#cancel()}).
     * <p>
     * 기본 전역 스코프의 {@link JsEngine} 을 반환한다. 실행 제한({@link JsEngine#getLimits()}), 믿을 수 없는 코드를 실행하기 전의 시간 제한, 모듈 로더를 지정하거나 다른
     * 스레드에서 스크립트를 취소({@link JsEngine#cancel()})하는 곳이다.
     *
     * @return the engine / 엔진
     */
    public JsEngine getJsEngine() {
        return primary.engine;
    }

    /**
     * Creates empty bindings (<code>SimpleBindings</code>). Bindings that you give to <code>eval</code> get a global scope of their own (see the class description).
     * <p>
     * 빈 바인딩(<code>SimpleBindings</code>)을 만든다. <code>eval</code> 에 주는 바인딩은 자신만의 전역 스코프를 가진다(클래스 설명 참조).
     *
     * @return new bindings / 새 바인딩
     */
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    /**
     * Returns the factory that created this engine.
     * <p>
     * 이 엔진을 만든 팩터리를 반환한다.
     *
     * @return the factory / 팩터리
     */
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    // ------------------------------------------------------------------ eval

    /**
     * Runs a script in a context.
     * <p>
     * 스크립트를 컨텍스트에서 실행한다.
     *
     * @param script  the script text / 스크립트 텍스트
     * @param context the context / 컨텍스트
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException for an error of the script / 스크립트 오류일 때
     */
    public Object eval(String script, ScriptContext context) throws ScriptException {
        if (script == null || context == null) {
            throw new NullPointerException("script and context must not be null");
        }
        return run(context, script, null);
    }

    /**
     * Runs a script that is read from a reader, in a context.
     * <p>
     * 리더에서 읽은 스크립트를 컨텍스트에서 실행한다.
     *
     * @param reader  the reader / 리더
     * @param context the context / 컨텍스트
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException for an error of the script or of the reader / 스크립트나 리더의 오류일 때
     */
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        if (reader == null || context == null) {
            throw new NullPointerException("reader and context must not be null");
        }
        return run(context, readAll(reader), null);
    }

    /**
     * Runs a script that was parsed in advance (called by {@link JsCompiledScript}).
     * <p>
     * 미리 파싱한 스크립트를 실행한다({@link JsCompiledScript} 가 호출한다).
     *
     * @param context the context / 컨텍스트
     * @param parsed  the parsed script / 파싱된 스크립트
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException for an error of the script / 스크립트 오류일 때
     */
    Object runParsed(ScriptContext context, ParsedScript parsed) throws ScriptException {
        return run(context, null, parsed);
    }

    /**
     * Reads a reader to the end.
     * <p>
     * 리더를 끝까지 읽는다.
     *
     * @param reader the reader / 리더
     * @return the text / 문자열
     * @throws ScriptException if reading fails / 읽기가 실패하면
     */
    private static String readAll(Reader reader) throws ScriptException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        try {
            int n;
            // 리더가 끝날 때까지 읽는다.
            while ((n = reader.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
        } catch (IOException e) {
            throw new ScriptException(e);
        }
        return sb.toString();
    }

    /**
     * Returns the file name for a context: the attribute <code>ScriptEngine.FILENAME</code> if it is a non-empty string, otherwise the given default.
     * <p>
     * 컨텍스트의 파일 이름을 반환한다: <code>ScriptEngine.FILENAME</code> 속성이 비어 있지 않은 문자열이면 그것, 아니면 주어진 기본값.
     *
     * @param ctx      the context / 컨텍스트
     * @param fallback the default name / 기본 이름
     * @return the file name / 파일 이름
     */
    private static String fileNameOf(ScriptContext ctx, String fallback) {
        Object f = ctx.getAttribute(ScriptEngine.FILENAME);
        if (f instanceof String && ((String) f).length() > 0) {
            return (String) f;
        }
        return fallback == null ? DEFAULT_FILE_NAME : fallback;
    }

    /**
     * Finds (or creates) the global scope that belongs to the <code>ENGINE_SCOPE</code> bindings of a context.
     * <p>
     * 컨텍스트의 <code>ENGINE_SCOPE</code> 바인딩에 해당하는 전역 스코프를 찾는다(없으면 만든다).
     *
     * @param engineScope the bindings / 바인딩
     * @return the scope / 스코프
     */
    private Scope scopeFor(Bindings engineScope) {
        if (engineScope == null) {
            throw new NullPointerException("the context has no ENGINE_SCOPE bindings");
        }
        if (engineScope instanceof JsGlobalBindings && ((JsGlobalBindings) engineScope).getEngine() == primary.engine) {
            return primary;
        }
        synchronized (scopes) {
            Object stale;
            // 수거된 바인딩의 항목을 목록에서 지운다.
            while ((stale = collected.poll()) != null) {
                scopes.remove(stale);
            }
            // 이 바인딩 객체(동일성 비교)의 스코프가 이미 있으면 그것을 쓴다.
            for (int i = 0; i < scopes.size(); i++) {
                if (scopes.get(i).get() == engineScope) {
                    return scopes.get(i).scope;
                }
            }
            JsEngine e = new JsEngine();
            copyLimits(primary.engine.getLimits(), e.getLimits());
            e.setModuleLoader(primary.engine.getModuleLoader());
            ContextConsole console = new ContextConsole(this);
            e.setConsole(console);
            Scope created = new Scope(e, console, false);
            scopes.add(new ScopedEngine(engineScope, created, collected));
            return created;
        }
    }

    /**
     * Copies the settings of execution limits.
     * <p>
     * 실행 제한 설정을 복사한다.
     *
     * @param from the source / 원본
     * @param to   the target / 대상
     */
    private static void copyLimits(ExecutionLimits from, ExecutionLimits to) {
        to.setMaxSteps(from.getMaxSteps());
        to.setMaxTimeMillis(from.getMaxTimeMillis());
        to.setMaxCallDepth(from.getMaxCallDepth());
        to.setMaxStringLength(from.getMaxStringLength());
    }

    /**
     * Runs a script (text or parsed) in a context: selects the global scope of the context, exchanges the variables with foreign bindings, sets the
     * output of the context and the lookup of the global bindings, runs and converts the result and the errors.
     * <p>
     * 스크립트(텍스트나 파싱된 것)를 컨텍스트에서 실행한다: 컨텍스트의 전역 스코프를 고르고, 다른 바인딩과 변수를 주고받고, 컨텍스트의 출력과 전역 바인딩 조회를
     * 지정하고, 실행하며 결과와 오류를 변환한다.
     *
     * @param ctx    the context / 컨텍스트
     * @param source the script text, or null when <code>parsed</code> is given / 스크립트 텍스트. <code>parsed</code> 를 주면 null
     * @param parsed the parsed script or null / 파싱된 스크립트 또는 null
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException for an error of the script / 스크립트 오류일 때
     */
    private Object run(ScriptContext ctx, String source, ParsedScript parsed) throws ScriptException {
        Bindings engineScope = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
        Scope scope = scopeFor(engineScope);
        String fileName = parsed != null ? parsed.getSourceName() : fileNameOf(ctx, null);
        JsEngine eng = scope.engine;
        synchronized (eng) {
            ScriptContext before = scope.console.enter(ctx);
            JsRealm.NamedGlobals outerLookup = scope.lookup;
            scope.lookup = new GlobalScopeLookup(eng, ctx);
            eng.getRealm().setNamedGlobals(scope.lookup);
            Map<String, JsValue> snapshot = null;
            try {
                if (!scope.live) {
                    snapshot = copyIn(eng, engineScope);
                }
                Object result = parsed != null ? eng.eval(parsed) : eng.eval(source, fileName);
                return JsGlobalBindings.visible(result);
            } catch (JsScriptError e) {
                throw toScriptException(e, fileName);
            } catch (ScriptAbortedException e) {
                throw aborted(e, fileName);
            } catch (IllegalArgumentException e) {
                ScriptException se = new ScriptException(e.getMessage(), fileName, -1, -1);
                se.initCause(e);
                throw se;
            } finally {
                scope.lookup = outerLookup;
                eng.getRealm().setNamedGlobals(outerLookup);
                if (!scope.live && snapshot != null) {
                    copyOut(eng, engineScope, snapshot);
                }
                scope.console.enter(before);
            }
        }
    }

    /**
     * Copies the entries of foreign bindings into the global object of their scope and remembers what the script will see.
     * <p>
     * 다른 바인딩의 항목을 그 스코프의 전역 객체로 복사하고 스크립트가 보게 될 값을 기억한다.
     *
     * @param eng      the engine of the scope / 스코프의 엔진
     * @param bindings the bindings / 바인딩
     * @return the script values of the entries after the copy / 복사 뒤 항목의 스크립트 값
     */
    private static Map<String, JsValue> copyIn(JsEngine eng, Bindings bindings) {
        Map<String, JsValue> snapshot = new HashMap<String, JsValue>();
        JsObject global = eng.getRealm().getGlobalObject();
        // 바인딩의 항목마다 전역 변수로 만들고 만들어진 값을 기억한다.
        for (Map.Entry<String, Object> e : new ArrayList<Map.Entry<String, Object>>(bindings.entrySet())) {
            String key = e.getKey();
            if (key == null || key.length() == 0) {
                continue;
            }
            eng.put(key, e.getValue());
            snapshot.put(key, global.get(key));
        }
        return snapshot;
    }

    /**
     * Copies the globals that a script created, changed or deleted back into foreign bindings. A value that the script did not change is not
     * written back, so an <code>Integer</code> in the bindings stays an <code>Integer</code> unless the script assigned it.
     * <p>
     * 스크립트가 만들거나 바꾸거나 지운 전역을 다른 바인딩으로 다시 복사한다. 스크립트가 바꾸지 않은 값은 다시 쓰지 않으므로, 바인딩의 <code>Integer</code> 는 스크립트가
     * 대입하지 않는 한 <code>Integer</code> 로 남는다.
     *
     * @param eng      the engine of the scope / 스코프의 엔진
     * @param bindings the bindings / 바인딩
     * @param snapshot the values from {@link #copyIn(JsEngine, Bindings)} / {@link #copyIn(JsEngine, Bindings)} 의 값
     */
    private static void copyOut(JsEngine eng, Bindings bindings, Map<String, JsValue> snapshot) {
        try {
            synchronized (eng) {
                JsObject global = eng.getRealm().getGlobalObject();
                List<String> keys = Ops.enumerableOwnKeys(global);
                // 열거 가능한 전역 중 새로 생겼거나 값이 바뀐 것을 바인딩에 쓴다.
                for (int i = 0; i < keys.size(); i++) {
                    String key = keys.get(i);
                    JsValue now = global.get(key);
                    JsValue old = snapshot.get(key);
                    if (old == null || !Ops.sameValue(old, now)) {
                        bindings.put(key, JsGlobalBindings.visible(eng.toJavaValue(now)));
                    }
                }
                Iterator<String> it = snapshot.keySet().iterator();
                // 스크립트가 지운 전역은 바인딩에서도 지운다.
                while (it.hasNext()) {
                    String key = it.next();
                    if (!global.hasProperty(key)) {
                        bindings.remove(key);
                    }
                }
            }
        } catch (RuntimeException e) {
            // 바인딩에 쓰지 못하는 값이 있어도 스크립트의 결과나 오류를 가리지 않는다.
            return;
        }
    }

    /**
     * Turns a script error into the <code>ScriptException</code> of the Java Scripting API.
     * <p>
     * 스크립트 오류를 Java Scripting API 의 <code>ScriptException</code> 으로 바꾼다.
     *
     * @param e        the error / 오류
     * @param fileName the name of the script that ran / 실행한 스크립트의 이름
     * @return the exception to throw / 던질 예외
     */
    private static ScriptException toScriptException(JsScriptError e, String fileName) {
        String file = e.getSourceName().length() > 0 ? e.getSourceName() : fileName;
        int line = e.getLine() > 0 ? e.getLine() : -1;
        int column = e.getColumn() > 0 ? e.getColumn() : -1;
        String text = e.getErrorMessage().length() > 0 ? e.getErrorName() + ": " + e.getErrorMessage() : e.getErrorName();
        ScriptException se = new ScriptException(text, file, line, column);
        se.initCause(e);
        return se;
    }

    /**
     * Turns the abort of a script into a <code>ScriptException</code>.
     * <p>
     * 스크립트의 중단을 <code>ScriptException</code> 으로 바꾼다.
     *
     * @param e        the abort / 중단
     * @param fileName the name of the script that ran / 실행한 스크립트의 이름
     * @return the exception to throw / 던질 예외
     */
    private static ScriptException aborted(ScriptAbortedException e, String fileName) {
        ScriptException se = new ScriptException("script execution aborted (" + e.getReason() + "): " + e.getMessage(), fileName, -1, -1);
        se.initCause(e);
        return se;
    }

    // ------------------------------------------------------------------ Compilable

    /**
     * Parses a script in advance.
     * <p>
     * 스크립트를 미리 파싱한다.
     *
     * @param script the script text / 스크립트 텍스트
     * @return the compiled script / 컴파일된 스크립트
     * @throws ScriptException for a syntax error (with the position) / 구문 오류(위치 포함)일 때
     */
    public CompiledScript compile(String script) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("script");
        }
        String name = fileNameOf(context, null);
        try {
            return new JsCompiledScript(this, primary.engine.compile(script, name));
        } catch (JsScriptError e) {
            throw toScriptException(e, name);
        }
    }

    /**
     * Parses a script that is read from a reader in advance.
     * <p>
     * 리더에서 읽은 스크립트를 미리 파싱한다.
     *
     * @param script the reader / 리더
     * @return the compiled script / 컴파일된 스크립트
     * @throws ScriptException for a syntax error or a failure of the reader / 구문 오류나 리더의 실패일 때
     */
    public CompiledScript compile(Reader script) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("script");
        }
        return compile(readAll(script));
    }

    // ------------------------------------------------------------------ Invocable

    /**
     * Calls a function of the global scope of the default context.
     * <p>
     * 기본 컨텍스트의 전역 스코프에 있는 함수를 호출한다.
     *
     * @param name the name of the function / 함수 이름
     * @param args the arguments / 인수
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException       for an error of the function / 함수의 오류일 때
     * @throws NoSuchMethodException if there is no such function / 그런 함수가 없으면
     */
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Scope scope = scopeFor(context.getBindings(ScriptContext.ENGINE_SCOPE));
        synchronized (scope.engine) {
            if (!scope.engine.isMethod(null, name)) {
                throw new NoSuchMethodException("no such function: " + name);
            }
            return invoke(scope, null, name, args);
        }
    }

    /**
     * Calls a method of a script object that an earlier <code>eval</code> or call returned.
     * <p>
     * 이전의 <code>eval</code> 이나 호출이 돌려준 스크립트 객체의 메서드를 호출한다.
     *
     * @param thiz the object (a {@link JsHandle}) / 객체({@link JsHandle})
     * @param name the name of the method / 메서드 이름
     * @param args the arguments / 인수
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException          for an error of the method / 메서드의 오류일 때
     * @throws NoSuchMethodException    if the object has no such method / 객체에 그런 메서드가 없으면
     * @throws IllegalArgumentException if <code>thiz</code> is null or not a script object / <code>thiz</code> 가 null 이거나 스크립트 객체가 아니면
     */
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (thiz == null) {
            throw new IllegalArgumentException("script object can not be null");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!(thiz instanceof JsHandle)) {
            throw new IllegalArgumentException("not a script object: " + thiz);
        }
        JsEngine owner = ((JsHandle) thiz).getEngine();
        synchronized (owner) {
            if (!owner.isMethod(thiz, name)) {
                throw new NoSuchMethodException("no such method: " + name);
            }
            return invoke(scopeOf(owner), thiz, name, args);
        }
    }

    /**
     * Finds the scope that an engine belongs to; an engine of another script engine gets a scope that has no console of this one.
     * <p>
     * 엔진이 속한 스코프를 찾는다. 다른 스크립트 엔진의 엔진은 이 엔진의 콘솔이 없는 스코프를 받는다.
     *
     * @param engine the engine / 엔진
     * @return the scope / 스코프
     */
    private Scope scopeOf(JsEngine engine) {
        if (engine == primary.engine) {
            return primary;
        }
        synchronized (scopes) {
            // 등록된 스코프 중 같은 엔진을 가진 것을 찾는다.
            for (int i = 0; i < scopes.size(); i++) {
                if (scopes.get(i).scope.engine == engine) {
                    return scopes.get(i).scope;
                }
            }
        }
        return new Scope(engine, new ContextConsole(this), false);
    }

    /**
     * Runs a call of a function or method and converts the errors.
     * <p>
     * 함수나 메서드의 호출을 실행하고 오류를 변환한다.
     *
     * @param scope  the scope / 스코프
     * @param target the object, or null for a global function / 객체. 전역 함수이면 null
     * @param name   the name / 이름
     * @param args   the arguments / 인수
     * @return the result (null for undefined) / 결과(undefined 이면 null)
     * @throws ScriptException for an error of the call / 호출의 오류일 때
     */
    private Object invoke(Scope scope, Object target, String name, Object[] args) throws ScriptException {
        ScriptContext before = scope.console.enter(context);
        try {
            return JsGlobalBindings.visible(scope.engine.invokeMethod(target, name, args));
        } catch (JsScriptError e) {
            throw toScriptException(e, null);
        } catch (ScriptAbortedException e) {
            throw aborted(e, name);
        } catch (IllegalArgumentException e) {
            ScriptException se = new ScriptException(e.getMessage(), name, -1, -1);
            se.initCause(e);
            throw se;
        } finally {
            scope.console.enter(before);
        }
    }

    /**
     * Returns an implementation of a Java interface by the global functions of the default context.
     * <p>
     * 기본 컨텍스트의 전역 함수로 구현한 Java 인터페이스 구현을 반환한다.
     *
     * @param clasz the interface / 인터페이스
     * @param <T>   the type / 타입
     * @return an implementation, or null if a function for a method of the interface is missing / 구현. 인터페이스의 메서드에 해당하는 함수가 없으면 null
     * @throws IllegalArgumentException if the class is null or not an interface / 클래스가 null 이거나 인터페이스가 아니면
     */
    public <T> T getInterface(Class<T> clasz) {
        if (clasz == null) {
            throw new IllegalArgumentException("interface Class object is null");
        }
        Scope scope = scopeFor(context.getBindings(ScriptContext.ENGINE_SCOPE));
        return scope.engine.getInterface(null, clasz);
    }

    /**
     * Returns an implementation of a Java interface by the functions of a script object.
     * <p>
     * 스크립트 객체의 함수로 구현한 Java 인터페이스 구현을 반환한다.
     *
     * @param thiz   the object (a {@link JsHandle}) / 객체({@link JsHandle})
     * @param clasz  the interface / 인터페이스
     * @param <T>    the type / 타입
     * @return an implementation, or null if a function for a method of the interface is missing / 구현. 인터페이스의 메서드에 해당하는 함수가 없으면 null
     * @throws IllegalArgumentException if the class is null or not an interface, or <code>thiz</code> is null or not a script object / 클래스가 null 이거나 인터페이스가 아니거나,
     *                                  <code>thiz</code> 가 null 이거나 스크립트 객체가 아니면
     */
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (clasz == null) {
            throw new IllegalArgumentException("interface Class object is null");
        }
        if (thiz == null) {
            throw new IllegalArgumentException("script object can not be null");
        }
        if (!(thiz instanceof JsHandle)) {
            throw new IllegalArgumentException("not a script object: " + thiz);
        }
        return ((JsHandle) thiz).getEngine().getInterface(thiz, clasz);
    }
}
