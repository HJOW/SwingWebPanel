package org.duckdns.hjow.swingwebpanel.js.embed;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ParsedScript;
import org.duckdns.hjow.swingwebpanel.js.Property;

/**
 * A self contained ECMAScript 2015 (ES6) engine that any Java program can use, with or without the browser component. It needs nothing
 * but the JRE (Java 6 or later): no external library, no native code, no other script engine. One <code>JsEngine</code> owns one realm: a
 * global object with the complete set of standard built-in objects (<code>Object</code>, <code>Array</code>, <code>Promise</code>,
 * <code>Map</code>, <code>JSON</code>, <code>RegExp</code>, typed arrays, <code>Proxy</code> ...). Engines are independent of each other.
 * <pre>
 * JsEngine engine = new JsEngine();
 * engine.put("name", "world");
 * Object r = engine.eval("`hello ${name}`.toUpperCase()");        // "HELLO WORLD"
 * engine.eval("function twice(x) { return x * 2; }");
 * Object d = engine.call("twice", Integer.valueOf(21));           // Double 42.0
 * </pre>
 * <h3>Global names (stand-alone mode)</h3>
 * The global object has the standard built-ins of ECMA-262 6th edition, <code>globalThis</code>, <code>console</code> (see below) and
 * <code>print(...)</code> (a convenience that writes the arguments as one line of level <code>log</code>). It has <b>no</b>
 * <code>window</code>, <code>document</code>, DOM, <code>fetch</code>, <code>XMLHttpRequest</code>, storage, and <b>no timers</b>
 * (<code>setTimeout</code> and friends): a stand-alone engine has no event loop. A host that needs timers registers its own functions with
 * {@link #putFunction(String, JsHostFunction)} and runs its own loop; the engine only guarantees that promise jobs (microtasks) run: after
 * every top-level <code>eval</code>, <code>call</code> and <code>evalModule</code> the engine runs all pending jobs, like the microtask checkpoint
 * of a browser. A script reaches Java code only through functions that the host registered.
 * <h3>Conversion between Java values and script values</h3>
 * <table border="1" cellpadding="3">
 * <tr><th>Java to script</th><th>Result</th></tr>
 * <tr><td><code>null</code></td><td>null</td></tr>
 * <tr><td>{@link #UNDEFINED}</td><td>undefined</td></tr>
 * <tr><td><code>Boolean</code></td><td>boolean</td></tr>
 * <tr><td><code>Byte Short Integer Long Float Double BigInteger BigDecimal</code> and every other <code>Number</code></td><td>number (through
 * <code>doubleValue()</code>; a <code>long</code> above 2<sup>53</sup> loses precision)</td></tr>
 * <tr><td><code>String</code>, any <code>CharSequence</code>, <code>Character</code>, <code>Enum</code> (its <code>name()</code>)</td><td>string</td></tr>
 * <tr><td><code>Map</code></td><td>a new plain object (keys through <code>String.valueOf</code>, values converted recursively; a copy)</td></tr>
 * <tr><td><code>Collection</code>, any array (also of primitives)</td><td>a new array (a copy)</td></tr>
 * <tr><td>{@link JsHostFunction}</td><td>a function that calls it</td></tr>
 * <tr><td>{@link JsHandle} (object, array, function handle)</td><td>the script object itself (no copy)</td></tr>
 * <tr><td>{@link JsValue}</td><td>itself (advanced use)</td></tr>
 * <tr><td>anything else</td><td><code>IllegalArgumentException</code>: no Java object, class, date or stream crosses to a script</td></tr>
 * <tr><th>Script to Java</th><th>Result</th></tr>
 * <tr><td>undefined</td><td>{@link #UNDEFINED}</td></tr>
 * <tr><td>null</td><td><code>null</code></td></tr>
 * <tr><td>boolean</td><td><code>Boolean</code></td></tr>
 * <tr><td>number</td><td>always <code>Double</code> (never <code>Integer</code> or <code>Long</code>; <code>NaN</code>, infinities and -0 are kept)</td></tr>
 * <tr><td>string</td><td><code>String</code> (UTF-16 code units, as in the script)</td></tr>
 * <tr><td>symbol</td><td>its description as a <code>String</code> such as <code>Symbol(x)</code> (a symbol cannot be handed over)</td></tr>
 * <tr><td>array</td><td>{@link JsArrayHandle} (a live <code>List</code>)</td></tr>
 * <tr><td>function</td><td>{@link JsFunctionHandle} (a live <code>Map</code> and {@link JsCallable})</td></tr>
 * <tr><td>any other object (also <code>Date</code>, <code>RegExp</code>, <code>Map</code>, a promise, an error object)</td><td>{@link JsObjectHandle} (a live <code>Map</code>)</td></tr>
 * </table>
 * {@link #toPlain(Object)} copies a result deeply into <code>List</code> and <code>Map</code> objects when a snapshot is wanted.
 * <h3>Errors</h3>
 * A syntax error, a <code>throw</code> and a runtime error of the script that nobody catches are thrown as {@link JsScriptError} (name, message,
 * script name, line and column, thrown value). A host function that throws an exception gives the script an <code>Error</code>; if the script
 * does not catch it, it reaches the caller as {@link JsScriptError} whose cause is the original exception. A script that the engine stops
 * (step limit, time limit, {@link #cancel()}) throws {@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException}, which a script cannot
 * catch.
 * <h3>Threads</h3>
 * Every method (and every handle) locks the engine, which is the monitor of this object: the engine can be used from any thread but runs
 * one script operation at a time, and operations on the same thread may nest (a host function may call the engine again). A caller that
 * needs several calls to be one atomic step can <code>synchronized (engine)</code> around them. Only {@link #cancel()} does not lock: it
 * may be called from any thread to stop a running script.
 * <h3>Limits</h3>
 * {@link #getLimits()} returns the {@link ExecutionLimits} of the engine: a step limit per task (default 200 million), an optional time limit
 * (default none), the nesting of calls (default 400) and the length of strings. Set a time limit before running code that you do not trust.
 * <p>
 * 브라우저 컴포넌트 유무와 관계없이 모든 Java 프로그램이 쓸 수 있는 자체 완결형 ECMAScript 2015(ES6) 엔진. JRE(Java 6 이상) 외에는 아무것도 필요 없다.
 * 외부 라이브러리, 네이티브 코드, 다른 스크립트 엔진을 쓰지 않는다. <code>JsEngine</code> 하나는 realm 하나를 가진다. 표준 내장 객체
 * (<code>Object</code>, <code>Array</code>, <code>Promise</code>, <code>Map</code>, <code>JSON</code>, <code>RegExp</code>, 타입 배열, <code>Proxy</code> ...)를 모두 갖춘
 * 전역 객체이며 엔진끼리는 서로 독립이다. 사용 예는 위 코드를 본다.
 * <h3>전역 이름(독립 실행 모드)</h3>
 * 전역 객체에는 ECMA-262 6판의 표준 내장 객체, <code>globalThis</code>, <code>console</code>(아래 참조), <code>print(...)</code>(인수를 <code>log</code> 수준의 한 줄로
 * 쓰는 편의 함수)가 있다. <code>window</code>, <code>document</code>, DOM, <code>fetch</code>, <code>XMLHttpRequest</code>, 저장소는 <b>없고</b>
 * <b>타이머도 없다</b>(<code>setTimeout</code> 등). 독립 실행 엔진에는 이벤트 루프가 없기 때문이다. 타이머가 필요한 호스트는
 * {@link #putFunction(String, JsHostFunction)} 로 자기 함수를 등록하고 자기 루프를 돌린다. 엔진이 보장하는 것은 promise 작업(마이크로태스크)의 실행뿐이다:
 * 최상위 <code>eval</code>, <code>call</code>, <code>evalModule</code> 이 끝날 때마다 대기 중인 작업을 모두 실행한다(브라우저의 마이크로태스크 체크포인트와 같다).
 * 스크립트는 호스트가 등록한 함수로만 Java 코드에 닿는다.
 * <h3>Java 값과 스크립트 값의 변환</h3>
 * 위 표를 따른다. Java 에서 스크립트로: null, {@link #UNDEFINED}, <code>Boolean</code>, 모든 <code>Number</code>(<code>doubleValue()</code> 로 숫자. 2<sup>53</sup> 을 넘는
 * <code>long</code> 은 정밀도 손실), 문자열류와 <code>Character</code>와 <code>Enum</code>(<code>name()</code>)은 문자열, <code>Map</code> 은 새 일반 객체(복사), <code>Collection</code> 과 모든
 * 배열은 새 배열(복사), {@link JsHostFunction} 은 함수, {@link JsHandle} 은 스크립트 객체 자체, 그 밖의 것은 <code>IllegalArgumentException</code>(Java 객체, 클래스,
 * 날짜, 스트림은 스크립트로 넘어가지 않는다). 스크립트에서 Java 로: undefined 는 {@link #UNDEFINED}, null 은 <code>null</code>, 불리언은 <code>Boolean</code>, 숫자는 항상
 * <code>Double</code>(<code>Integer</code>/<code>Long</code> 이 아님. <code>NaN</code>, 무한대, -0 유지), 문자열은 <code>String</code>, 심볼은 <code>Symbol(x)</code> 형태의 문자열, 배열은
 * {@link JsArrayHandle}(살아 있는 <code>List</code>), 함수는 {@link JsFunctionHandle}, 그 밖의 객체(<code>Date</code>, <code>RegExp</code>, <code>Map</code>, promise, 오류 객체 포함)는
 * {@link JsObjectHandle}(살아 있는 <code>Map</code>). 복사본이 필요하면 {@link #toPlain(Object)} 가 <code>List</code> 와 <code>Map</code> 으로 깊게 복사한다.
 * <h3>오류</h3>
 * 아무도 잡지 않은 스크립트의 구문 오류, <code>throw</code>, 런타임 오류는 {@link JsScriptError}(이름, 메시지, 스크립트 이름, 줄, 열, 던진 값)로 던져진다. 예외를 던진 호스트
 * 함수는 스크립트에 <code>Error</code> 를 주고, 스크립트가 잡지 않으면 원래 예외를 원인으로 가진 {@link JsScriptError} 로 호출자에게 닿는다. 엔진이 멈춘 스크립트(단계 수
 * 제한, 시간 제한, {@link #cancel()})는 스크립트가 잡을 수 없는 {@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException} 을 던진다.
 * <h3>스레드</h3>
 * 모든 메서드(와 모든 핸들)는 엔진, 즉 이 객체의 모니터를 잠근다. 엔진은 어느 스레드에서나 쓸 수 있지만 스크립트 연산을 한 번에 하나씩 실행하며, 같은 스레드의 연산은 중첩될 수
 * 있다(호스트 함수가 엔진을 다시 호출해도 된다). 여러 호출을 한 단계로 묶으려면 <code>synchronized (engine)</code> 으로 감싼다. {@link #cancel()} 만 잠그지 않으며, 실행 중인
 * 스크립트를 멈추려고 어느 스레드에서나 호출할 수 있다.
 * <h3>제한</h3>
 * {@link #getLimits()} 는 엔진의 {@link ExecutionLimits} 를 돌려준다: 작업당 단계 수(기본 2억), 선택적 시간 제한(기본 없음), 호출 중첩(기본 400), 문자열 길이. 믿을 수 없는 코드를
 * 실행하기 전에 시간 제한을 지정한다.
 */
public final class JsEngine {

    /**
     * The type of {@link #UNDEFINED}.
     * <p>
     * {@link #UNDEFINED} 의 타입.
     */
    public static final class Undefined {

        /**
         * Private constructor: the only instance is {@link JsEngine#UNDEFINED}.
         * <p>
         * 비공개 생성자: 유일한 인스턴스는 {@link JsEngine#UNDEFINED} 이다.
         */
        private Undefined() {
        }

        /**
         * Returns the text <code>undefined</code>.
         * <p>
         * 문자열 <code>undefined</code> 를 반환한다.
         *
         * @return the text / 문자열
         */
        public String toString() {
            return "undefined";
        }
    }

    /**
     * The Java value of the script value <code>undefined</code>: the result of a script that has no value, of a missing argument and of a property
     * that holds <code>undefined</code>. (JavaScript <code>null</code> is the Java <code>null</code>.)
     * <p>
     * 스크립트 값 <code>undefined</code> 의 Java 값: 값이 없는 스크립트의 결과, 없는 인수, <code>undefined</code> 를 담은 프로퍼티의 값이다. (JavaScript
     * <code>null</code> 은 Java <code>null</code> 이다.)
     */
    public static final Undefined UNDEFINED = new Undefined();

    /**
     * The name that a script gets when the caller does not give one.
     * <p>
     * 호출자가 이름을 주지 않을 때 스크립트가 받는 이름.
     */
    public static final String DEFAULT_SCRIPT_NAME = "<eval>";

    /**
     * The realm of the engine.
     * <p>
     * 엔진의 realm.
     */
    private final JsRealm realm;

    /**
     * Creates an engine with a new realm that has the complete standard built-in library.
     * <p>
     * 완전한 표준 내장 라이브러리를 가진 새 realm 으로 엔진을 만든다.
     */
    public JsEngine() {
        this.realm = JsRealm.create();
    }

    /**
     * Returns the realm of the engine (an advanced hook for code that works with the classes of the engine directly, for example to
     * install own built-in objects; such code must lock the engine itself).
     * <p>
     * 엔진의 realm 을 반환한다(엔진 클래스를 직접 다루는 코드를 위한 고급 연결 지점. 예: 자체 내장 객체 설치. 그런 코드는 엔진 잠금을 스스로 책임진다).
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return realm;
    }

    // ------------------------------------------------------------------ configuration

    /**
     * Returns the execution limits of the engine.
     * <p>
     * 엔진의 실행 제한을 반환한다.
     *
     * @return the limits / 제한
     */
    public ExecutionLimits getLimits() {
        return realm.getLimits();
    }

    /**
     * Asks the running script to stop at its next step; callable from any thread without locking. The stopped operation throws
     * {@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException}. Call {@link #resetCancel()} before the next operation.
     * <p>
     * 실행 중인 스크립트에 다음 단계에서 멈추라고 요청한다. 잠금 없이 어느 스레드에서나 호출할 수 있다. 멈춘 연산은
     * {@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException} 을 던진다. 다음 연산 전에 {@link #resetCancel()} 을 호출한다.
     */
    public void cancel() {
        realm.getLimits().cancel();
    }

    /**
     * Clears the cancellation request so that the engine can run scripts again.
     * <p>
     * 취소 요청을 지워 엔진이 다시 스크립트를 실행할 수 있게 한다.
     */
    public void resetCancel() {
        realm.getLimits().reset();
    }

    /**
     * Sets the hook that receives the output of <code>console</code> and <code>print</code> (the default writes to standard output, and
     * the levels <code>warn</code> and <code>error</code> to standard error). A hook that implements
     * {@link org.duckdns.hjow.swingwebpanel.js.ConsoleRecordHook} also gets the structured form of every message.
     * <p>
     * <code>console</code> 과 <code>print</code> 의 출력을 받는 연결을 지정한다(기본값은 표준 출력이고 <code>warn</code>, <code>error</code> 수준은 표준 오류).
     * {@link org.duckdns.hjow.swingwebpanel.js.ConsoleRecordHook} 를 구현한 연결은 모든 메시지의 구조화된 형태도 받는다.
     *
     * @param hook the hook / 연결
     */
    public synchronized void setConsole(ConsoleHook hook) {
        realm.setConsole(hook);
    }

    /**
     * Returns the console hook.
     * <p>
     * 콘솔 연결을 반환한다.
     *
     * @return the hook / 연결
     */
    public synchronized ConsoleHook getConsole() {
        return realm.getConsole();
    }

    /**
     * Sets the hook that resolves and loads the modules that a module imports. Without it only {@link #evalModule(String, String)} with source
     * text works and every <code>import</code> of another module fails.
     * <p>
     * 모듈이 import 하는 모듈을 해석하고 불러오는 연결을 지정한다. 지정하지 않으면 소스 텍스트를 준 {@link #evalModule(String, String)} 만 동작하고
     * 다른 모듈의 <code>import</code> 는 모두 실패한다.
     *
     * @param loader the loader or null / 로더. 없으면 null
     */
    public synchronized void setModuleLoader(ModuleLoader loader) {
        realm.setModuleLoader(loader);
    }

    /**
     * Returns the module loader, or null.
     * <p>
     * 모듈 로더를 반환한다. 없으면 null.
     *
     * @return the loader / 로더
     */
    public synchronized ModuleLoader getModuleLoader() {
        return realm.getModuleLoader();
    }

    // ------------------------------------------------------------------ running code

    /**
     * Runs a script (the Script goal: sloppy mode unless the script says <code>"use strict"</code>) in the global scope and returns its
     * completion value. Pending promise jobs run before this method returns.
     * <p>
     * 스크립트(Script 목표: <code>"use strict"</code> 가 없으면 느슨한 모드)를 전역 스코프에서 실행하고 완료 값을 반환한다. 대기 중인 promise 작업은 이 메서드가
     * 반환하기 전에 실행된다.
     *
     * @param source the source text / 소스 텍스트
     * @return the completion value converted to a Java value; {@link #UNDEFINED} if the script has no value / Java 값으로 바꾼 완료 값. 값이 없으면 {@link #UNDEFINED}
     * @throws NullPointerException if the source is null / 소스가 null 이면
     * @throws JsScriptError        for a syntax error or an uncaught error of the script / 구문 오류나 잡히지 않은 스크립트 오류
     */
    public Object eval(String source) {
        return eval(source, DEFAULT_SCRIPT_NAME);
    }

    /**
     * Runs a script and tells the engine its name, which appears in {@link JsScriptError} and in the call position of console messages.
     * <p>
     * 스크립트를 실행하며 엔진에 이름을 알려 준다. 이름은 {@link JsScriptError} 와 콘솔 메시지의 호출 위치에 나타난다.
     *
     * @param source the source text / 소스 텍스트
     * @param name   the name of the script, for example a file name; null means {@link #DEFAULT_SCRIPT_NAME} / 스크립트 이름(예: 파일 이름). null 이면 {@link #DEFAULT_SCRIPT_NAME}
     * @return the completion value converted to a Java value / Java 값으로 바꾼 완료 값
     * @throws NullPointerException if the source is null / 소스가 null 이면
     * @throws JsScriptError        for a syntax error or an uncaught error of the script / 구문 오류나 잡히지 않은 스크립트 오류
     */
    public synchronized Object eval(String source, String name) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        try {
            JsValue v = realm.evaluateScript(source, name == null ? DEFAULT_SCRIPT_NAME : name);
            Object result = JavaValues.toJava(this, v);
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Parses a script without running it, to find syntax errors early or to run the same script many times without parsing it again
     * ({@link #eval(ParsedScript)}).
     * <p>
     * 스크립트를 실행하지 않고 파싱한다. 구문 오류를 일찍 찾거나 같은 스크립트를 다시 파싱하지 않고 여러 번 실행하려는 것이다({@link #eval(ParsedScript)}).
     *
     * @param source the source text / 소스 텍스트
     * @param name   the name of the script, or null for {@link #DEFAULT_SCRIPT_NAME} / 스크립트 이름. null 이면 {@link #DEFAULT_SCRIPT_NAME}
     * @return the parsed script / 파싱된 스크립트
     * @throws NullPointerException if the source is null / 소스가 null 이면
     * @throws JsScriptError        for a syntax error (with its position) / 구문 오류(위치 포함)
     */
    public synchronized ParsedScript compile(String source, String name) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        try {
            return realm.parseScript(source, name == null ? DEFAULT_SCRIPT_NAME : name);
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Runs a script that was parsed by {@link #compile(String, String)}.
     * <p>
     * {@link #compile(String, String)} 로 파싱한 스크립트를 실행한다.
     *
     * @param script the parsed script / 파싱된 스크립트
     * @return the completion value converted to a Java value / Java 값으로 바꾼 완료 값
     * @throws JsScriptError for an uncaught error of the script / 잡히지 않은 스크립트 오류
     */
    public synchronized Object eval(ParsedScript script) {
        try {
            JsValue v = realm.evaluateScript(script);
            Object result = JavaValues.toJava(this, v);
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Loads, links and runs an ES2015 module (the Module goal: always strict, <code>import</code> and <code>export</code> allowed) together with the
     * modules it imports, which the {@link #setModuleLoader(ModuleLoader) module loader} supplies. The same key always denotes the same module
     * in one engine, and a module is run once.
     * <p>
     * ES2015 모듈(Module 목표: 항상 엄격 모드, <code>import</code> 와 <code>export</code> 허용)과 그것이 import 하는 모듈({@link #setModuleLoader(ModuleLoader) 모듈 로더}가 준다)을
     * 불러와 링크하고 실행한다. 한 엔진에서 같은 키는 항상 같은 모듈이며 모듈은 한 번만 실행된다.
     *
     * @param key    the unique key of the module, for example its URL / 모듈의 고유한 키(예: URL)
     * @param source the source text of the module, or null to fetch it through the module loader / 모듈의 소스 텍스트. null 이면 모듈 로더로 가져온다
     * @return the namespace object of the module (its exports) / 모듈의 네임스페이스 객체(export 한 것들)
     * @throws JsScriptError for a failure of the loader, a syntax error or an uncaught error / 로더의 실패, 구문 오류, 잡히지 않은 오류
     */
    public synchronized JsObjectHandle evalModule(String key, String source) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        try {
            JsObject ns = realm.evaluateModule(key, source);
            JsObjectHandle result = new JsObjectHandle(this, ns);
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Runs all pending promise jobs (microtasks). The engine does it after every top-level operation by itself; call it after you changed
     * something from outside, for example after a host function resolved a promise.
     * <p>
     * 대기 중인 모든 promise 작업(마이크로태스크)을 실행한다. 엔진이 최상위 연산이 끝날 때마다 스스로 하므로, 바깥에서 무언가를 바꾼 뒤(예: 호스트 함수가 promise 를
     * 해결한 뒤)에 호출한다.
     *
     * @throws JsScriptError for an uncaught error of a job / 작업의 잡히지 않은 오류
     */
    public synchronized void runJobs() {
        try {
            realm.runJobs();
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    // ------------------------------------------------------------------ global variables

    /**
     * Reads a global variable (a property of the global object: <code>var</code>, function declarations, assignments to undeclared names and
     * the built-in objects). Global <code>let</code>, <code>const</code> and <code>class</code> declarations live in a declarative scope and are not
     * visible here; read them with <code>eval("name")</code>.
     * <p>
     * 전역 변수(전역 객체의 프로퍼티: <code>var</code>, 함수 선언, 선언 없는 이름에 대한 대입, 내장 객체)를 읽는다. 전역 <code>let</code>, <code>const</code>,
     * <code>class</code> 선언은 선언적 스코프에 있어 여기서 보이지 않으므로 <code>eval("name")</code> 으로 읽는다.
     *
     * @param name the name / 이름
     * @return the value converted to a Java value; null if there is no such global (and for JavaScript <code>null</code>) / Java 값으로 바꾼 값. 그런 전역이 없으면(그리고
     *         JavaScript <code>null</code> 이면) null
     */
    public synchronized Object get(String name) {
        JsObject g = realm.getGlobalObject();
        try {
            if (!g.hasProperty(name)) {
                return null;
            }
            return JavaValues.toJava(this, g.get(name));
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Tells whether a global variable exists.
     * <p>
     * 전역 변수가 있는지 알려 준다.
     *
     * @param name the name / 이름
     * @return true if the global object has the property / 전역 객체에 프로퍼티가 있으면 true
     */
    public synchronized boolean has(String name) {
        try {
            return realm.getGlobalObject().hasProperty(name);
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Tells whether a global variable exists and is a function that can be called.
     * <p>
     * 전역 변수가 있고 호출할 수 있는 함수인지 알려 준다.
     *
     * @param name the name / 이름
     * @return true if it is a function / 함수이면 true
     */
    public synchronized boolean isFunction(String name) {
        try {
            JsObject g = realm.getGlobalObject();
            return g.hasProperty(name) && g.get(name).isCallable();
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Assigns a global variable (creates it if needed). A {@link JsHostFunction} value becomes a function named after the variable.
     * <p>
     * 전역 변수에 대입한다(필요하면 만든다). {@link JsHostFunction} 값은 변수 이름을 이름으로 가진 함수가 된다.
     *
     * @param name  the name / 이름
     * @param value the value, converted by the rules in the class description / 값. 클래스 설명의 규칙대로 바꾼다
     * @throws IllegalArgumentException if the value cannot cross to a script / 값이 스크립트로 넘어갈 수 없으면
     * @throws JsScriptError            if the assignment fails (for example for a read-only global) / 대입이 실패하면(예: 읽기 전용 전역)
     */
    public synchronized void put(String name, Object value) {
        try {
            realm.getGlobalObject().setStrict(name, JavaValues.toJs(this, value, name));
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Gives scripts a function that calls Java code: the only way for a script to run Java code (see {@link JsHostFunction}).
     * <p>
     * 스크립트에 Java 코드를 호출하는 함수를 준다. 스크립트가 Java 코드를 실행하는 유일한 방법이다({@link JsHostFunction} 참조).
     *
     * @param name     the global name of the function / 함수의 전역 이름
     * @param function the Java implementation / Java 구현
     */
    public void putFunction(String name, JsHostFunction function) {
        put(name, function);
    }

    /**
     * Deletes a global variable.
     * <p>
     * 전역 변수를 삭제한다.
     *
     * @param name the name / 이름
     * @return true if the variable does not exist any more; false if it cannot be deleted (a <code>var</code> or a function declaration of a script is
     *         not deletable) / 변수가 더 이상 없으면 true. 삭제할 수 없으면 false(스크립트의 <code>var</code> 나 함수 선언은 삭제할 수 없다)
     */
    public synchronized boolean remove(String name) {
        try {
            return realm.getGlobalObject().delete(name);
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Returns the names of the enumerable global variables (what a script declared or assigned; the built-in objects are not enumerable).
     * <p>
     * 열거 가능한 전역 변수의 이름을 반환한다(스크립트가 선언하거나 대입한 것. 내장 객체는 열거되지 않는다).
     *
     * @return the names in the order of creation / 만든 순서대로의 이름
     */
    public synchronized List<String> globalNames() {
        try {
            return new ArrayList<String>(Ops.enumerableOwnKeys(realm.getGlobalObject()));
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Returns a live view of the global object as a <code>Map</code>.
     * <p>
     * 전역 객체를 <code>Map</code> 으로 보여 주는 살아 있는 보기를 반환한다.
     *
     * @return the handle of the global object / 전역 객체의 핸들
     */
    public JsObjectHandle getGlobalObject() {
        return new JsObjectHandle(this, realm.getGlobalObject());
    }

    // ------------------------------------------------------------------ calling functions

    /**
     * Calls a global function.
     * <p>
     * 전역 함수를 호출한다.
     *
     * @param functionName the name of the function / 함수 이름
     * @param args         the arguments, converted by the rules in the class description / 인수. 클래스 설명의 규칙대로 바꾼다
     * @return the result converted to a Java value / Java 값으로 바꾼 결과
     * @throws IllegalArgumentException if an argument cannot cross to a script / 인수가 스크립트로 넘어갈 수 없으면
     * @throws JsScriptError            if there is no such function or the call throws / 그런 함수가 없거나 호출이 예외를 던지면
     */
    public synchronized Object call(String functionName, Object... args) {
        try {
            JsObject g = realm.getGlobalObject();
            JsValue f = g.get(functionName);
            if (!f.isCallable()) {
                throw JsException.typeError(functionName + " is not a function");
            }
            Object result = JavaValues.toJava(this, callValue(f, Js.UNDEFINED, toJsArgs(args)));
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Calls a method of a script object: <code>target[name](args)</code>; with a null target a global function.
     * <p>
     * 스크립트 객체의 메서드를 호출한다: <code>target[name](args)</code>. target 이 null 이면 전역 함수.
     *
     * @param target the {@link JsHandle} of the object, or null for the global object / 객체의 {@link JsHandle}. 전역 객체이면 null
     * @param name   the property name of the method / 메서드의 프로퍼티 이름
     * @param args   the arguments / 인수
     * @return the result converted to a Java value / Java 값으로 바꾼 결과
     * @throws IllegalArgumentException if the target is not a handle of this engine / target 이 이 엔진의 핸들이 아니면
     * @throws JsScriptError            if the property is not a function or the call throws / 프로퍼티가 함수가 아니거나 호출이 예외를 던지면
     */
    public synchronized Object invokeMethod(Object target, String name, Object... args) {
        JsObject t = target == null ? realm.getGlobalObject() : handleObject(target);
        try {
            Object result = JavaValues.toJava(this, invokeValue(t, name, toJsArgs(args)));
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Calls a function that a handle refers to.
     * <p>
     * 핸들이 가리키는 함수를 호출한다.
     *
     * @param function  the function handle / 함수 핸들
     * @param thisValue the <code>this</code> value / <code>this</code> 값
     * @param args      the arguments / 인수
     * @return the result converted to a Java value / Java 값으로 바꾼 결과
     * @throws JsScriptError if the call throws / 호출이 예외를 던지면
     */
    synchronized Object callFunction(JsHandle function, Object thisValue, Object[] args) {
        JsObject f = handleObject(function);
        try {
            Object result = JavaValues.toJava(this, callValue(f, JavaValues.toJs(this, thisValue), toJsArgs(args)));
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Calls a function that a handle refers to as a constructor.
     * <p>
     * 핸들이 가리키는 함수를 생성자로 호출한다.
     *
     * @param function the function handle / 함수 핸들
     * @param args     the arguments / 인수
     * @return the new object converted to a Java value / Java 값으로 바꾼 새 객체
     * @throws JsScriptError if the function is not a constructor or throws / 함수가 생성자가 아니거나 예외를 던지면
     */
    synchronized Object construct(JsHandle function, Object[] args) {
        JsObject f = handleObject(function);
        ExecutionLimits limits = realm.getLimits();
        try {
            limits.beginTask();
            Object result;
            try {
                result = JavaValues.toJava(this, Ops.construct(f, toJsArgs(args)));
            } finally {
                limits.endTask();
            }
            runJobs();
            return result;
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Calls a method of a script object as one task (steps and time are counted from zero) and returns the script value.
     * <p>
     * 스크립트 객체의 메서드를 작업 하나로 호출하고(단계 수와 시간을 0 부터 센다) 스크립트 값을 반환한다.
     *
     * @param target the object / 객체
     * @param name   the method name / 메서드 이름
     * @param args   the arguments / 인수
     * @return the script value / 스크립트 값
     */
    JsValue invokeValue(JsObject target, String name, JsValue[] args) {
        JsValue f = target.get(name);
        if (!f.isCallable()) {
            throw JsException.typeError(name + " is not a function");
        }
        return callValue(f, target, args);
    }

    /**
     * Calls a function as one task: the steps and the time of the limits are counted from zero for it, so that the limits apply to every call
     * that Java code starts.
     * <p>
     * 함수를 작업 하나로 호출한다. 제한의 단계 수와 시간을 이 호출에서 0 부터 세므로, Java 코드가 시작하는 모든 호출에 제한이 적용된다.
     *
     * @param f         the function / 함수
     * @param thisValue the this value / this 값
     * @param args      the arguments / 인수
     * @return the script value / 스크립트 값
     */
    JsValue callValue(JsValue f, JsValue thisValue, JsValue[] args) {
        ExecutionLimits limits = realm.getLimits();
        limits.beginTask();
        try {
            return Ops.call(f, thisValue, args);
        } finally {
            limits.endTask();
        }
    }

    /**
     * Converts the Java arguments of a call to script values.
     * <p>
     * 호출의 Java 인수를 스크립트 값으로 바꾼다.
     *
     * @param args the Java arguments, may be null / Java 인수. null 일 수 있다
     * @return the script values / 스크립트 값
     */
    private JsValue[] toJsArgs(Object[] args) {
        if (args == null) {
            return Js.NO_ARGS;
        }
        JsValue[] out = new JsValue[args.length];
        // 인수를 하나씩 스크립트 값으로 바꾼다.
        for (int i = 0; i < args.length; i++) {
            out[i] = JavaValues.toJs(this, args[i]);
        }
        return out;
    }

    /**
     * Returns the script object of a handle of this engine.
     * <p>
     * 이 엔진의 핸들이 가리키는 스크립트 객체를 반환한다.
     *
     * @param handle the handle / 핸들
     * @return the script object / 스크립트 객체
     * @throws IllegalArgumentException if the argument is not a {@link JsHandle} / 인수가 {@link JsHandle} 이 아니면
     */
    private JsObject handleObject(Object handle) {
        if (!(handle instanceof JsHandle)) {
            throw new IllegalArgumentException("not a script object (a JsHandle that this engine returned): " + handle);
        }
        return ((JsHandle) handle).getJsObject();
    }

    // ------------------------------------------------------------------ objects, arrays and conversions

    /**
     * Creates an empty script object and returns a live view of it; put values and {@link JsHostFunction}s into it to build an object that
     * you can hand to a script with {@link #put(String, Object)}.
     * <p>
     * 빈 스크립트 객체를 만들고 그 살아 있는 보기를 반환한다. 값과 {@link JsHostFunction} 을 넣어 {@link #put(String, Object)} 로 스크립트에 줄 객체를 만든다.
     *
     * @return the new object / 새 객체
     */
    public synchronized JsObjectHandle newObject() {
        return new JsObjectHandle(this, realm.newObject());
    }

    /**
     * Creates an empty script array and returns a live view of it.
     * <p>
     * 빈 스크립트 배열을 만들고 그 살아 있는 보기를 반환한다.
     *
     * @return the new array / 새 배열
     */
    public synchronized JsArrayHandle newArray() {
        return new JsArrayHandle(this, realm.newArray());
    }

    /**
     * Copies a script object or array deeply into plain Java objects: arrays become <code>ArrayList</code>, objects become
     * <code>LinkedHashMap</code> (enumerable own string keys), everything else (functions stay handles) converts as usual. A cyclic structure
     * keeps its shape. Any other value is returned unchanged.
     * <p>
     * 스크립트 객체나 배열을 순수 Java 객체로 깊게 복사한다. 배열은 <code>ArrayList</code>, 객체는 <code>LinkedHashMap</code>(열거 가능한 고유 문자열 키)이 되고, 나머지(함수는
     * 핸들로 남는다)는 평소처럼 바뀐다. 순환 구조는 모양을 유지한다. 다른 값은 그대로 반환한다.
     *
     * @param value a value that this engine returned / 이 엔진이 돌려준 값
     * @return the plain Java copy / 순수 Java 복사본
     */
    public synchronized Object toPlain(Object value) {
        if (!(value instanceof JsHandle)) {
            return value;
        }
        try {
            return JavaValues.toPlain(this, ((JsHandle) value).getJsObject(), new IdentityHashMap<JsObject, Object>());
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Converts a value to a Java type the way a call of a Java interface method would: a number to <code>int</code>, <code>long</code>,
     * <code>double</code>... by the script rules (<code>ToInt32</code> and so on), anything to <code>String</code> by <code>ToString</code>, a truthy value
     * to <code>boolean</code>, a script object to a Java interface (its functions implement the methods), anything else only if it already is
     * an instance of the type.
     * <p>
     * 값을 Java 인터페이스 메서드 호출이 하는 방식으로 Java 타입에 맞게 바꾼다. 숫자는 스크립트 규칙(<code>ToInt32</code> 등)으로 <code>int</code>, <code>long</code>, <code>double</code>...,
     * 무엇이든 <code>ToString</code> 으로 <code>String</code>, 참으로 평가되는 값은 <code>boolean</code>, 스크립트 객체는 Java 인터페이스(그 함수들이 메서드를 구현한다)로 바꾸며, 나머지는
     * 이미 그 타입의 인스턴스일 때만 가능하다.
     *
     * @param value the value (Java value or {@link JsValue}) / 값(Java 값 또는 {@link JsValue})
     * @param type  the wanted type / 원하는 타입
     * @param <T>   the type / 타입
     * @return the converted value (a boxed value for a primitive type) / 바뀐 값(기본형이면 박싱된 값)
     * @throws ClassCastException if the value cannot be converted / 값을 바꿀 수 없으면
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T convert(Object value, Class<T> type) {
        try {
            return (T) JavaValues.coerce(this, JavaValues.toJs(this, value), type);
        } catch (JsException e) {
            throw wrap(e);
        }
    }

    /**
     * Implements a Java interface with the functions of a script object (or with the global functions): a call of an interface method calls
     * the function of the same name. A script function that is itself the target implements an interface with a single method.
     * <p>
     * Java 인터페이스를 스크립트 객체의 함수(또는 전역 함수)로 구현한다. 인터페이스 메서드를 호출하면 같은 이름의 함수를 호출한다. 대상이 스크립트 함수 자신이면 메서드가 하나뿐인
     * 인터페이스를 구현한다.
     *
     * @param target the {@link JsHandle} of the object, or null for the global functions / 객체의 {@link JsHandle}. 전역 함수이면 null
     * @param iface  the interface / 인터페이스
     * @param <T>    the interface type / 인터페이스 타입
     * @return an implementation, or null if the script does not have a function for every method of the interface / 구현. 인터페이스의 모든 메서드에 해당하는 함수가 스크립트에
     *         없으면 null
     * @throws IllegalArgumentException if the type is not an interface or the target is not a handle of this engine / 타입이 인터페이스가 아니거나 target 이 이 엔진의 핸들이 아니면
     */
    public synchronized <T> T getInterface(Object target, Class<T> iface) {
        JsObject t = target == null ? null : handleObject(target);
        return iface.cast(InterfaceProxy.create(this, t, iface, true));
    }

    // ------------------------------------------------------------------ errors

    /**
     * Turns an engine level exception into the {@link JsScriptError} that Java callers get.
     * <p>
     * 엔진 수준의 예외를 Java 호출자가 받는 {@link JsScriptError} 로 바꾼다.
     *
     * @param e the exception / 예외
     * @return the error to throw / 던질 오류
     */
    JsScriptError wrap(JsException e) {
        String name = "Error";
        String message = "";
        JsValue thrown = e.getValue(realm);
        if (e.getKind() != null) {
            name = e.getKind().getErrorName();
            message = e.getErrorMessage() == null ? "" : e.getErrorMessage();
        } else if (thrown instanceof JsObject) {
            String n = dataString((JsObject) thrown, "name");
            String m = dataString((JsObject) thrown, "message");
            name = n == null || n.length() == 0 ? "Error" : n;
            message = m == null ? "" : m;
        } else {
            message = JsException.describe(thrown);
        }
        // 구문 오류는 오류 위치를, 그 밖의 오류는 처음 관찰된 위치를 보고한다.
        boolean syntax = e.getLine() > 0;
        String source = syntax ? e.getSourceName() : e.getThrowSourceName();
        int line = syntax ? e.getLine() : e.getThrowLine();
        int column = syntax ? e.getColumn() : e.getThrowColumn();
        return new JsScriptError(name, message, source, line, column, JavaValues.toJava(this, thrown), e);
    }

    /**
     * Reads a string data property along the prototype chain without calling getters (so that describing an error never runs script code).
     * <p>
     * getter 를 호출하지 않고 프로토타입 체인에서 문자열 데이터 프로퍼티를 읽는다(오류를 설명하는 것이 스크립트 코드를 실행하지 않도록).
     *
     * @param o   the object / 객체
     * @param key the property name / 프로퍼티 이름
     * @return the string or null / 문자열 또는 null
     */
    private static String dataString(JsObject o, String key) {
        JsObject cur = o;
        // 프로토타입 체인을 따라가며 데이터 프로퍼티를 찾는다.
        for (int depth = 0; cur != null && depth < 64; depth++) {
            Property p = cur.getOwnPropertySlot(key);
            if (p != null) {
                if (!p.accessor && p.value instanceof JsString) {
                    return ((JsString) p.value).getValue();
                }
                return null;
            }
            cur = cur.getPrototypeOf();
        }
        return null;
    }
}
