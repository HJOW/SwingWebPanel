package org.duckdns.hjow.swingwebpanel.js;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A realm (ECMA-262 6th edition 8.2): a global object together with a complete, independent set of built-in objects.
 * One realm is created per web page by the host. This class is the host-facing facade of the engine and the registry of the
 * intrinsic objects that built-ins and the interpreter need.
 * <p>
 * Built-in authors: create functions with {@link #newFunction(String, int, NativeFunction.Call)}, attach them with
 * {@link #method(JsObject, String, int, NativeFunction.Call)}, throw errors with {@link JsException}, look up prototypes
 * with the getters of this class or {@link #getIntrinsic(String)}, and install whole groups through a
 * {@link BuiltinInstaller}.
 * <p>
 * realm (ECMA-262 6판 8.2): 전역 객체와 독립적이고 완전한 내장 객체 집합. 호스트가 웹 페이지마다 realm 하나를 만든다.
 * 이 클래스는 엔진의 호스트용 창구이며, 내장 객체와 인터프리터가 필요로 하는 내장(intrinsic) 객체의 등록부이다.
 * <p>
 * 내장 객체 작성자: {@link #newFunction(String, int, NativeFunction.Call)} 로 함수를 만들고
 * {@link #method(JsObject, String, int, NativeFunction.Call)} 로 붙이며, 오류는 {@link JsException} 으로 던지고,
 * 프로토타입은 이 클래스의 getter 나 {@link #getIntrinsic(String)} 으로 찾고, 묶음 설치는 {@link BuiltinInstaller} 로 한다.
 */
public class JsRealm {

    /**
     * The global object.
     * <p>
     * 전역 객체.
     */
    private final JsObject globalObject;

    /**
     * The intrinsic %ObjectPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %ObjectPrototype%.
     */
    private final JsObject objectPrototype;

    /**
     * The intrinsic %FunctionPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %FunctionPrototype%.
     */
    private final JsObject functionPrototype;

    /**
     * The intrinsic %ArrayPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %ArrayPrototype%.
     */
    private final JsObject arrayPrototype;

    /**
     * The intrinsic %StringPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %StringPrototype%.
     */
    private final JsObject stringPrototype;

    /**
     * The intrinsic %NumberPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %NumberPrototype%.
     */
    private final JsObject numberPrototype;

    /**
     * The intrinsic %BooleanPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %BooleanPrototype%.
     */
    private final JsObject booleanPrototype;

    /**
     * The intrinsic %SymbolPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %SymbolPrototype%.
     */
    private final JsObject symbolPrototype;

    /**
     * The intrinsic %IteratorPrototype% object of this realm.
     * <p>
     * 이 realm 의 내장 객체 %IteratorPrototype%.
     */
    private final JsObject iteratorPrototype;

    /**
     * Prototypes of the native error types (the ERROR entry is %ErrorPrototype%).
     * <p>
     * 기본 오류 형식의 프로토타입(ERROR 항목이 %ErrorPrototype%).
     */
    private final Map<ErrorKind, JsObject> errorPrototypes = new EnumMap<ErrorKind, JsObject>(ErrorKind.class);

    /**
     * Named intrinsic objects registered by built-in groups (for example "%IteratorPrototype%").
     * <p>
     * 내장 묶음이 등록한 이름 있는 내장 객체(예: "%IteratorPrototype%").
     */
    private final Map<String, JsObject> intrinsics = new HashMap<String, JsObject>();

    /**
     * Global symbol registry used by Symbol.for and Symbol.keyFor.
     * <p>
     * Symbol.for, Symbol.keyFor 가 쓰는 전역 심볼 레지스트리.
     */
    private final Map<String, JsSymbol> symbolRegistry = new HashMap<String, JsSymbol>();

    /**
     * Execution limits and cancellation.
     * <p>
     * 실행 제한과 취소.
     */
    private final ExecutionLimits limits = new ExecutionLimits();

    /**
     * The queue of microtasks.
     * <p>
     * 마이크로태스크 큐.
     */
    private JobQueue jobQueue = new SimpleJobQueue();

    /**
     * The module loader hook (null if not set).
     * <p>
     * 모듈 로더 연결(지정하지 않았으면 null).
     */
    private ModuleLoader moduleLoader;

    /**
     * The console hook (defaults to standard output).
     * <p>
     * 콘솔 연결(기본값은 표준 출력).
     */
    private ConsoleHook console = new ConsoleHook() {
        public void message(String level, String text) {
            if ("error".equals(level) || "warn".equals(level)) {
                System.err.println(text);
            } else {
                System.out.println(text);
            }
        }
    };

    /**
     * The name of the script that made the latest call of a native function (see {@link #setCallSite(String, int, int)}).
     * <p>
     * 기본 함수를 마지막으로 호출한 스크립트의 이름({@link #setCallSite(String, int, int)} 참조).
     */
    private String callSourceName = "";

    /**
     * The line of the latest call of a native function, or 0.
     * <p>
     * 기본 함수의 마지막 호출의 줄. 없으면 0.
     */
    private int callLine;

    /**
     * The column of the latest call of a native function, or 0.
     * <p>
     * 기본 함수의 마지막 호출의 열. 없으면 0.
     */
    private int callColumn;

    /**
     * The interpreter (created on first use).
     * <p>
     * 인터프리터(처음 쓸 때 만든다).
     */
    private org.duckdns.hjow.swingwebpanel.js.interp.Interpreter interpreter;

    /**
     * Creates a realm with the fundamental prototype objects but without any built-in library.
     * Built-ins are added by {@link BuiltinInstaller}s (see the subclass or factory that creates fully initialised realms).
     * <p>
     * 기본 프로토타입 객체는 있으나 내장 라이브러리는 없는 realm 을 만든다. 내장 객체는 {@link BuiltinInstaller} 가 추가한다
     * (완전히 초기화된 realm 을 만드는 하위 클래스나 팩터리를 참조).
     */
    public JsRealm() {
        objectPrototype = new JsObject(null);
        functionPrototype = new NativeFunction(this, objectPrototype, "", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, null);
        arrayPrototype = new JsArray(objectPrototype);
        stringPrototype = new JsStringObject(objectPrototype, "");
        numberPrototype = new JsPrimitiveObject(objectPrototype, JsNumber.ZERO, "Number");
        booleanPrototype = new JsPrimitiveObject(objectPrototype, JsBoolean.FALSE, "Boolean");
        symbolPrototype = new JsObject(objectPrototype);
        JsObject errorProto = new JsObject(objectPrototype);
        errorPrototypes.put(ErrorKind.ERROR, errorProto);
        ErrorKind[] kinds = ErrorKind.values();
        // 기본 오류 형식마다 Error.prototype 을 상속하는 프로토타입을 만든다.
        for (int i = 0; i < kinds.length; i++) {
            if (kinds[i] != ErrorKind.ERROR) {
                errorPrototypes.put(kinds[i], new JsObject(errorProto));
            }
        }
        globalObject = new GlobalObject(objectPrototype);
        // %IteratorPrototype% 은 [Symbol.iterator]() 가 자기 자신을 돌려주는 모든 이터레이터의 공통 조상이다.
        iteratorPrototype = new JsObject(objectPrototype);
        method(iteratorPrototype, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
        setIntrinsic("%IteratorPrototype%", iteratorPrototype);
    }

    /**
     * The names that a host gives to the global object besides its properties (the named properties of the <code>Window</code> object of a
     * page: the ids of its elements and the names of some of them). A name is looked up only if the global object and its prototype chain
     * do not have it, so a property that exists (a variable, a function, a built-in) is never hidden, and a script that declares a variable
     * with such a name gets its own property.
     * <p>
     * 호스트가 전역 객체에 프로퍼티 외에 주는 이름(페이지의 <code>Window</code> 객체의 이름 있는 프로퍼티: 요소의 id 와 일부 요소의 name). 전역 객체와 그 프로토타입 체인에 이름이
     * 없을 때만 찾으므로 있는 프로퍼티(변수, 함수, 내장 객체)는 가려지지 않고, 그런 이름으로 변수를 선언하는 스크립트는 자신의 프로퍼티를 얻는다.
     */
    public interface NamedGlobals {
        /**
         * Looks a name up.
         * <p>
         * 이름을 찾는다.
         *
         * @param name the name / 이름
         * @return the value, or null if the host has nothing under the name / 값. 호스트에 그 이름이 없으면 null
         */
        JsValue lookup(String name);
    }

    /**
     * The global object: an ordinary object that falls back to the {@link NamedGlobals} of the host for a name that nothing else has.
     * <p>
     * 전역 객체: 다른 곳에 없는 이름은 호스트의 {@link NamedGlobals} 로 돌아가는 보통 객체.
     */
    private final class GlobalObject extends JsObject {
        /**
         * Creates the global object.
         * <p>
         * 전역 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        GlobalObject(JsObject proto) {
            super(proto);
        }

        /**
         * Tells whether the global object has a property or the host has a name.
         * <p>
         * 전역 객체에 프로퍼티가 있거나 호스트에 이름이 있는지 알려 준다.
         *
         * @param key the key / 키
         * @return true if it is found / 찾으면 true
         */
        public boolean hasProperty(Object key) {
            if (super.hasProperty(key)) {
                return true;
            }
            NamedGlobals named = namedGlobals;
            return named != null && key instanceof String && named.lookup((String) key) != null;
        }

        /**
         * Returns the value of a property, or what the host has under the name if nothing else has it.
         * <p>
         * 프로퍼티의 값을 반환하며 다른 곳에 없으면 호스트가 그 이름으로 가진 것을 반환한다.
         *
         * @param key      the key / 키
         * @param receiver the receiver / receiver
         * @return the value / 값
         */
        public JsValue get(Object key, JsValue receiver) {
            JsValue value = super.get(key, receiver);
            NamedGlobals named = namedGlobals;
            // 값이 undefined 이고 호스트의 이름 조회가 있으면 프로퍼티가 정말 없는지 확인한 뒤 호스트에 묻는다.
            if (!value.isUndefined() || named == null || !(key instanceof String) || super.hasProperty(key)) {
                return value;
            }
            JsValue found = named.lookup((String) key);
            return found == null ? value : found;
        }
    }

    /**
     * Sets the names that the host gives to the global object (see {@link NamedGlobals}); null removes them.
     * <p>
     * 호스트가 전역 객체에 주는 이름을 정한다({@link NamedGlobals} 참조). null 이면 없앤다.
     *
     * @param named the lookup or null / 이름 조회. 없으면 null
     */
    public void setNamedGlobals(NamedGlobals named) {
        this.namedGlobals = named;
    }

    /**
     * The names that the host gives to the global object, or null.
     * <p>
     * 호스트가 전역 객체에 주는 이름. 없으면 null.
     */
    private volatile NamedGlobals namedGlobals;

    /**
     * Returns %IteratorPrototype%, the common ancestor of all iterator prototypes (it only has a [Symbol.iterator] method
     * that returns this). Also registered as the intrinsic "%IteratorPrototype%".
     * <p>
     * %IteratorPrototype% 을 반환한다. 모든 이터레이터 프로토타입의 공통 조상이며 this 를 돌려주는 [Symbol.iterator] 메서드만 가진다.
     * 내장 객체 "%IteratorPrototype%" 으로도 등록되어 있다.
     *
     * @return the iterator prototype / 이터레이터 프로토타입
     */
    public JsObject getIteratorPrototype() {
        return iteratorPrototype;
    }

    /**
     * Returns the global object.
     * <p>
     * 전역 객체를 반환한다.
     *
     * @return the global object / 전역 객체
     */
    public JsObject getGlobalObject() {
        return globalObject;
    }

    /**
     * Returns %ObjectPrototype%.
     * <p>
     * %ObjectPrototype% 을 반환한다.
     *
     * @return Object.prototype / Object.prototype
     */
    public JsObject getObjectPrototype() {
        return objectPrototype;
    }

    /**
     * Returns %FunctionPrototype%.
     * <p>
     * %FunctionPrototype% 을 반환한다.
     *
     * @return Function.prototype / Function.prototype
     */
    public JsObject getFunctionPrototype() {
        return functionPrototype;
    }

    /**
     * Returns %ArrayPrototype%.
     * <p>
     * %ArrayPrototype% 을 반환한다.
     *
     * @return Array.prototype / Array.prototype
     */
    public JsObject getArrayPrototype() {
        return arrayPrototype;
    }

    /**
     * Returns %StringPrototype%.
     * <p>
     * %StringPrototype% 을 반환한다.
     *
     * @return String.prototype / String.prototype
     */
    public JsObject getStringPrototype() {
        return stringPrototype;
    }

    /**
     * Returns %NumberPrototype%.
     * <p>
     * %NumberPrototype% 을 반환한다.
     *
     * @return Number.prototype / Number.prototype
     */
    public JsObject getNumberPrototype() {
        return numberPrototype;
    }

    /**
     * Returns %BooleanPrototype%.
     * <p>
     * %BooleanPrototype% 을 반환한다.
     *
     * @return Boolean.prototype / Boolean.prototype
     */
    public JsObject getBooleanPrototype() {
        return booleanPrototype;
    }

    /**
     * Returns %SymbolPrototype%.
     * <p>
     * %SymbolPrototype% 을 반환한다.
     *
     * @return Symbol.prototype / Symbol.prototype
     */
    public JsObject getSymbolPrototype() {
        return symbolPrototype;
    }

    /**
     * Returns the prototype of a native error type (the prototype of Error itself for {@link ErrorKind#ERROR}).
     * <p>
     * 기본 오류 형식의 프로토타입을 반환한다({@link ErrorKind#ERROR} 는 Error 자신의 프로토타입).
     *
     * @param kind the error kind / 오류 종류
     * @return the prototype / 프로토타입
     */
    public JsObject getErrorPrototype(ErrorKind kind) {
        return errorPrototypes.get(kind);
    }

    /**
     * Registers (or replaces) a named intrinsic object, for example "%IteratorPrototype%".
     * <p>
     * 이름 있는 내장 객체를 등록한다(이미 있으면 교체). 예: "%IteratorPrototype%".
     *
     * @param name the name including the percent signs / 퍼센트 기호를 포함한 이름
     * @param obj  the object / 객체
     */
    public void setIntrinsic(String name, JsObject obj) {
        intrinsics.put(name, obj);
    }

    /**
     * Returns a named intrinsic object or null if none is registered.
     * <p>
     * 이름 있는 내장 객체를 반환한다. 등록된 것이 없으면 null.
     *
     * @param name the name including the percent signs / 퍼센트 기호를 포함한 이름
     * @return the object or null / 객체 또는 null
     */
    public JsObject getIntrinsic(String name) {
        return intrinsics.get(name);
    }

    /**
     * Returns the global symbol registry entry for a key, creating it if needed (Symbol.for).
     * <p>
     * 키에 해당하는 전역 심볼 레지스트리 항목을 반환한다. 없으면 만든다(Symbol.for).
     *
     * @param key the registry key / 레지스트리 키
     * @return the registered symbol / 등록된 심볼
     */
    public JsSymbol symbolFor(String key) {
        JsSymbol s = symbolRegistry.get(key);
        if (s == null) {
            s = new JsSymbol(key, key);
            symbolRegistry.put(key, s);
        }
        return s;
    }

    // ------------------------------------------------------------------ factories and helpers for built-ins

    /**
     * Creates an ordinary object with Object.prototype.
     * <p>
     * Object.prototype 을 프로토타입으로 갖는 일반 객체를 만든다.
     *
     * @return a new object / 새 객체
     */
    public JsObject newObject() {
        return new JsObject(objectPrototype);
    }

    /**
     * Creates an empty array with Array.prototype.
     * <p>
     * Array.prototype 을 프로토타입으로 갖는 빈 배열을 만든다.
     *
     * @return a new array / 새 배열
     */
    public JsArray newArray() {
        return new JsArray(arrayPrototype);
    }

    /**
     * Creates an array holding the given elements.
     * <p>
     * 주어진 요소를 담은 배열을 만든다.
     *
     * @param elements the elements / 요소들
     * @return a new array / 새 배열
     */
    public JsArray newArray(JsValue[] elements) {
        JsValue[] copy = new JsValue[elements.length];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        return new JsArray(arrayPrototype, copy);
    }

    /**
     * Creates an array holding the elements of a list.
     * <p>
     * 목록의 요소를 담은 배열을 만든다.
     *
     * @param elements the elements / 요소들
     * @return a new array / 새 배열
     */
    public JsArray newArray(List<? extends JsValue> elements) {
        return newArray(elements.toArray(new JsValue[elements.size()]));
    }

    /**
     * Creates a native function with this realm's Function.prototype.
     * <p>
     * 이 realm 의 Function.prototype 을 쓰는 네이티브 함수를 만든다.
     *
     * @param name   function name / 함수 이름
     * @param length expected number of arguments / 기대 인수 개수
     * @param call   the behaviour / 동작
     * @return the function / 함수
     */
    public NativeFunction newFunction(String name, int length, NativeFunction.Call call) {
        return new NativeFunction(this, name, length, call);
    }

    /**
     * Creates a native function and defines it on an object as a built-in method (writable, non-enumerable, configurable).
     * For symbol-keyed methods pass the symbol as key; the function name is then "[description]".
     * <p>
     * 네이티브 함수를 만들어 내장 메서드(writable, 열거 불가, configurable)로 객체에 정의한다.
     * 심볼 키 메서드는 키에 심볼을 넘기며 함수 이름은 "[설명]" 이 된다.
     *
     * @param target the object receiving the method / 메서드를 받을 객체
     * @param key    the key: a String or a {@link JsSymbol} / 키: String 또는 {@link JsSymbol}
     * @param length expected number of arguments / 기대 인수 개수
     * @param call   the behaviour / 동작
     * @return the function / 함수
     */
    public NativeFunction method(JsObject target, Object key, int length, NativeFunction.Call call) {
        String name = key instanceof JsSymbol ? "[" + ((JsSymbol) key).getDescription() + "]" : (String) key;
        NativeFunction f = new NativeFunction(this, name, length, call);
        target.defineBuiltin(key, f);
        return f;
    }

    /**
     * Defines a read-only accessor property (getter only; non-enumerable, configurable) such as <code>Map.prototype.size</code>.
     * <p>
     * 읽기 전용 접근자 프로퍼티(getter 만, 열거 불가, configurable)를 정의한다. 예: <code>Map.prototype.size</code>.
     *
     * @param target the object / 객체
     * @param key    the key / 키
     * @param call   the getter behaviour / getter 동작
     * @return the getter function / getter 함수
     */
    public NativeFunction getter(JsObject target, Object key, NativeFunction.Call call) {
        String name = "get " + (key instanceof JsSymbol ? "[" + ((JsSymbol) key).getDescription() + "]" : (String) key);
        NativeFunction f = new NativeFunction(this, name, 0, call);
        target.defineOwnProperty(key, PropertyDescriptor.accessor(f, null, false, true));
        return f;
    }

    // ------------------------------------------------------------------ host API

    /**
     * Creates a realm with the complete standard built-in library installed (the normal way to get a ready-to-use realm).
     * <p>
     * 완전한 표준 내장 라이브러리가 설치된 realm 을 만든다(바로 쓸 수 있는 realm 을 얻는 일반적인 방법).
     *
     * @return the new realm / 새 realm
     */
    public static JsRealm create() {
        JsRealm r = new JsRealm();
        org.duckdns.hjow.swingwebpanel.js.builtins.StandardBuiltins.installAll(r);
        return r;
    }

    /**
     * Returns the execution limits and the cancellation switch of this realm.
     * <p>
     * 이 realm 의 실행 제한과 취소 스위치를 반환한다.
     *
     * @return the limits / 제한
     */
    public ExecutionLimits getLimits() {
        return limits;
    }

    /**
     * Returns the interpreter of this realm, creating it on first use.
     * <p>
     * 이 realm 의 인터프리터를 반환한다. 처음 쓸 때 만든다.
     *
     * @return the interpreter / 인터프리터
     */
    public org.duckdns.hjow.swingwebpanel.js.interp.Interpreter getInterpreter() {
        if (interpreter == null) {
            interpreter = new org.duckdns.hjow.swingwebpanel.js.interp.Interpreter(this);
        }
        return interpreter;
    }

    /**
     * Parses and runs a script in the global scope. Syntax errors and uncaught script exceptions are thrown as
     * {@link JsException}; an aborted run (limits, cancellation) throws {@link ScriptAbortedException}.
     * Must be called on the thread that owns the realm.
     * <p>
     * 전역 스코프에서 스크립트를 파싱하고 실행한다. 구문 오류와 잡히지 않은 스크립트 예외는 {@link JsException} 으로, 제한이나 취소로 중단되면
     * {@link ScriptAbortedException} 으로 던진다. realm 을 소유한 스레드에서 호출해야 한다.
     *
     * @param source the source text / 소스 텍스트
     * @param name   the source name (file name or URL) used in errors / 오류에 쓰는 소스 이름(파일 이름이나 URL)
     * @return the completion value of the last statement, or undefined / 마지막 문장의 완료 값. 없으면 undefined
     */
    public JsValue evaluateScript(String source, String name) {
        return getInterpreter().evaluateScript(source, name);
    }

    /**
     * Parses a script without running it. A syntax error is thrown as {@link JsException} (with the position of the error). Run the
     * result any number of times with {@link #evaluateScript(ParsedScript)}.
     * <p>
     * 스크립트를 실행하지 않고 파싱한다. 구문 오류는 오류 위치와 함께 {@link JsException} 으로 던진다. 결과는
     * {@link #evaluateScript(ParsedScript)} 로 몇 번이든 실행할 수 있다.
     *
     * @param source the source text / 소스 텍스트
     * @param name   the source name (file name or URL) used in errors / 오류에 쓰는 소스 이름(파일 이름이나 URL)
     * @return the parsed script / 파싱된 스크립트
     */
    public ParsedScript parseScript(String source, String name) {
        return getInterpreter().parseScript(source, name);
    }

    /**
     * Runs a script that was parsed with {@link #parseScript(String, String)} in the global scope, with the same error and thread rules as
     * {@link #evaluateScript(String, String)}.
     * <p>
     * {@link #parseScript(String, String)} 로 파싱한 스크립트를 전역 스코프에서 실행한다. 오류와 스레드 규칙은
     * {@link #evaluateScript(String, String)} 과 같다.
     *
     * @param script the parsed script / 파싱된 스크립트
     * @return the completion value of the last statement, or undefined / 마지막 문장의 완료 값. 없으면 undefined
     */
    public JsValue evaluateScript(ParsedScript script) {
        return getInterpreter().evaluateScript(script);
    }

    /**
     * Loads, links and evaluates a module (and everything it imports through the {@link ModuleLoader}) and returns its
     * namespace object.
     * <p>
     * 모듈(과 {@link ModuleLoader} 로 import 하는 모든 모듈)을 불러와 링크하고 평가한 뒤 네임스페이스 객체를 반환한다.
     *
     * @param key    the unique module key (for example the absolute URL) / 고유한 모듈 키(예: 절대 URL)
     * @param source the module source text, or null to fetch it through the loader / 모듈 소스 텍스트. null 이면 로더로 가져온다
     * @return the namespace object / 네임스페이스 객체
     */
    public JsObject evaluateModule(String key, String source) {
        return getInterpreter().importModule(key, source);
    }

    /**
     * Fetches and parses a module and everything it imports through the {@link ModuleLoader}, without running any of it. A failure of
     * the loader or a syntax error is thrown here; call {@link ModuleGraph#evaluate()} on the result to link and run the modules.
     * <p>
     * 모듈과 {@link ModuleLoader} 로 import 하는 모든 모듈을 실행하지 않고 가져와 파싱한다. 로더의 실패나 구문 오류는 여기서 던져지며,
     * 결과의 {@link ModuleGraph#evaluate()} 를 불러 모듈을 링크하고 실행한다.
     *
     * @param key    the unique module key (for example the absolute URL) / 고유한 모듈 키(예: 절대 URL)
     * @param source the module source text, or null to fetch it through the loader / 모듈 소스 텍스트. null 이면 로더로 가져온다
     * @return the loaded graph / 불러온 그래프
     */
    public ModuleGraph loadModule(String key, String source) {
        return getInterpreter().loadModule(key, source);
    }

    /**
     * Returns the job queue that receives microtasks of this realm.
     * <p>
     * 이 realm 의 마이크로태스크를 받는 작업 큐를 반환한다.
     *
     * @return the queue / 큐
     */
    public JobQueue getJobQueue() {
        return jobQueue;
    }

    /**
     * Replaces the job queue (a browser host connects its event loop here).
     * <p>
     * 작업 큐를 바꾼다(브라우저 호스트가 이곳에 이벤트 루프를 연결한다).
     *
     * @param queue the queue / 큐
     */
    public void setJobQueue(JobQueue queue) {
        this.jobQueue = queue;
    }

    /**
     * Enqueues a job (used by Promise and other built-ins; the host decides when it runs).
     * <p>
     * 작업을 큐에 넣는다(Promise 등 내장 객체가 사용. 실행 시점은 호스트가 정한다).
     *
     * @param job the job / 작업
     */
    public void enqueueJob(Runnable job) {
        jobQueue.enqueue(job);
    }

    /**
     * Runs all pending jobs when the default {@link SimpleJobQueue} is in use (each job is a separate task for the limits).
     * With a host-provided queue the host drains it itself and this method does nothing.
     * <p>
     * 기본 {@link SimpleJobQueue} 를 쓰는 경우 대기 중인 모든 작업을 실행한다(작업마다 제한 측정은 별개의 작업이다).
     * 호스트가 준 큐를 쓰면 호스트가 직접 비우므로 이 메서드는 아무 일도 하지 않는다.
     */
    public void runJobs() {
        if (jobQueue instanceof SimpleJobQueue) {
            SimpleJobQueue q = (SimpleJobQueue) jobQueue;
            // 작업 하나마다 단계 수와 시간 측정을 새로 시작한다.
            while (!q.isEmpty()) {
                limits.beginTask();
                try {
                    q.runOne();
                } finally {
                    limits.endTask();
                }
            }
        }
    }

    /**
     * Returns the module loader hook, or null if none was set.
     * <p>
     * 모듈 로더 연결을 반환한다. 지정하지 않았으면 null.
     *
     * @return the loader or null / 로더 또는 null
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Sets the hook that resolves and loads modules for <code>import</code>.
     * <p>
     * <code>import</code> 를 위해 모듈을 해석하고 불러오는 연결을 지정한다.
     *
     * @param loader the loader / 로더
     */
    public void setModuleLoader(ModuleLoader loader) {
        this.moduleLoader = loader;
    }

    /**
     * Returns the console hook.
     * <p>
     * 콘솔 연결을 반환한다.
     *
     * @return the hook / 연결
     */
    public ConsoleHook getConsole() {
        return console;
    }

    /**
     * Sets the hook that receives <code>console</code> and <code>print</code> output (the default writes to standard
     * output).
     * <p>
     * <code>console</code> 과 <code>print</code> 출력을 받는 연결을 지정한다(기본값은 표준 출력).
     *
     * @param hook the hook / 연결
     */
    public void setConsole(ConsoleHook hook) {
        this.console = hook;
    }

    /**
     * Remembers where the script code is that calls a native function. The interpreter calls it just before each call of a native
     * function, so a native function (for example <code>console.log</code>) can tell where it was called from. The values are only valid
     * while the native function runs, on the thread that runs the script.
     * <p>
     * 기본 함수를 호출하는 스크립트 코드의 위치를 기억한다. 인터프리터가 기본 함수를 호출하기 직전마다 부르므로, 기본 함수(예:
     * <code>console.log</code>)가 자신이 어디서 호출되었는지 알 수 있다. 값은 기본 함수가 실행되는 동안, 스크립트를 실행하는 스레드에서만
     * 유효하다.
     *
     * @param sourceName the script name or null / 스크립트 이름 또는 null
     * @param line       the 1-based line / 1부터 시작하는 줄
     * @param column     the 1-based column / 1부터 시작하는 열
     */
    public void setCallSite(String sourceName, int line, int column) {
        this.callSourceName = sourceName == null ? "" : sourceName;
        this.callLine = line;
        this.callColumn = column;
    }

    /**
     * Returns the name of the script that made the latest call of a native function, or the empty string.
     * <p>
     * 기본 함수를 마지막으로 호출한 스크립트의 이름을 반환한다. 없으면 빈 문자열.
     *
     * @return the script name / 스크립트 이름
     */
    public String getCallSourceName() {
        return callSourceName;
    }

    /**
     * Returns the line of the latest call of a native function, or 0.
     * <p>
     * 기본 함수의 마지막 호출의 줄을 반환한다. 없으면 0.
     *
     * @return the line / 줄
     */
    public int getCallLine() {
        return callLine;
    }

    /**
     * Returns the column of the latest call of a native function, or 0.
     * <p>
     * 기본 함수의 마지막 호출의 열을 반환한다. 없으면 0.
     *
     * @return the column / 열
     */
    public int getCallColumn() {
        return callColumn;
    }

    /**
     * Creates an error object of a native kind (used when a {@link JsException} is caught by script code).
     * <p>
     * 기본 종류의 오류 객체를 만든다({@link JsException} 을 스크립트 코드가 잡을 때 사용).
     *
     * @param kind    error kind / 오류 종류
     * @param message message or null / 메시지 또는 null
     * @return the error object / 오류 객체
     */
    public JsObject createError(ErrorKind kind, String message) {
        JsErrorObject e = new JsErrorObject(errorPrototypes.get(kind));
        if (message != null) {
            e.defineOwnProperty("message", PropertyDescriptor.data(JsString.of(message), true, false, true));
        }
        return e;
    }
}
