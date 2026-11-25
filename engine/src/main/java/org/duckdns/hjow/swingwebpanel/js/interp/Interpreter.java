package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.ModuleGraph;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ParsedScript;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.ProgramInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.ScopeInfo;
import org.duckdns.hjow.swingwebpanel.js.parser.Parser;

/**
 * The interpreter of one realm: it runs scripts, eval code and modules over the syntax tree produced by
 * {@link Parser}. This class owns the global environment, function objects and calls (FunctionDeclarationInstantiation,
 * [[Call]] and [[Construct]] of script functions), and the dynamic code entry points; expressions, statements, destructuring,
 * classes, generators and modules are implemented by the collaborating classes of this package.
 * <p>
 * The interpreter must only be used from one thread at a time (the realm's owner thread). It is not re-entrant across threads.
 * <p>
 * realm 하나의 인터프리터: {@link Parser} 가 만든 구문 트리 위에서 스크립트, eval 코드, 모듈을 실행한다. 이 클래스는 전역 환경,
 * 함수 객체와 호출(FunctionDeclarationInstantiation, 스크립트 함수의 [[Call]], [[Construct]]), 동적 코드 진입점을 담당하며,
 * 표현식, 문장, 구조 분해, 클래스, 제너레이터, 모듈은 이 패키지의 협력 클래스가 구현한다.
 * <p>
 * 인터프리터는 한 번에 한 스레드(realm 의 소유 스레드)에서만 써야 한다. 스레드 사이에서 재진입할 수 없다.
 */
public final class Interpreter {

    /**
     * The realm that owns this component.
     * <p>
     * 이 구성 요소를 소유한 realm.
     */
    final JsRealm realm;

    /**
     * The limits of the realm.
     * <p>
     * realm 의 실행 제한.
     */
    final ExecutionLimits limits;

    /**
     * The global environment (declarative part for let/const/class plus the global object).
     * <p>
     * 전역 환경(let/const/class 용 선언적 부분과 전역 객체).
     */
    final Environment globalEnv;

    /**
     * The expression evaluator.
     * <p>
     * 표현식 평가기.
     */
    final Expressions ex;

    /**
     * The statement executor.
     * <p>
     * 문장 실행기.
     */
    final Statements st;

    /**
     * The destructuring helper.
     * <p>
     * 구조 분해 도우미.
     */
    final Patterns pat;

    /**
     * The class evaluator.
     * <p>
     * 클래스 평가기.
     */
    final Classes cls;

    /**
     * The current nesting of script function calls.
     * <p>
     * 현재 스크립트 함수 호출 중첩 깊이.
     */
    int depth;

    /**
     * The loaded modules by key (shared by all imports in this realm).
     * <p>
     * 키로 찾는 불러온 모듈(이 realm 의 모든 import 가 공유).
     */
    final java.util.HashMap<String, ModuleRecord> modules = new java.util.HashMap<String, ModuleRecord>();

    /**
     * Creates the interpreter of a realm.
     * <p>
     * realm 의 인터프리터를 만든다.
     *
     * @param realm the realm / realm
     */
    public Interpreter(JsRealm realm) {
        this.realm = realm;
        this.limits = realm.getLimits();
        this.globalEnv = new Environment(null);
        this.globalEnv.objectRecord = realm.getGlobalObject();
        this.globalEnv.hasThis = true;
        this.globalEnv.thisValue = realm.getGlobalObject();
        this.globalEnv.thisBound = true;
        this.ex = new Expressions(this);
        this.st = new Statements(this);
        this.pat = new Patterns(this);
        this.cls = new Classes(this);
    }

    // ------------------------------------------------------------------ scripts

    /**
     * Parses and runs a script in the global environment (ScriptEvaluation, ECMA-262 6th edition 15.1.7).
     * <p>
     * 전역 환경에서 스크립트를 파싱하고 실행한다(ScriptEvaluation, ECMA-262 6판 15.1.7).
     *
     * @param source the source text / 소스 텍스트
     * @param name   the source name for errors / 오류에 쓰는 소스 이름
     * @return the completion value of the last statement, or undefined / 마지막 문장의 완료 값. 없으면 undefined
     */
    public JsValue evaluateScript(String source, String name) {
        return run(Parser.parseScript(source, name), name);
    }

    /**
     * Parses a script without running it (ScriptEvaluation is split into parsing and running, ECMA-262 6th edition 15.1.7).
     * <p>
     * 스크립트를 실행하지 않고 파싱한다(ScriptEvaluation 을 파싱과 실행으로 나눈 것, ECMA-262 6판 15.1.7).
     *
     * @param source the source text / 소스 텍스트
     * @param name   the source name for errors / 오류에 쓰는 소스 이름
     * @return the parsed script / 파싱된 스크립트
     */
    public ParsedScript parseScript(String source, String name) {
        return new ParsedProgram(Parser.parseScript(source, name), name);
    }

    /**
     * Runs a script that was parsed with {@link #parseScript(String, String)} in the global environment.
     * <p>
     * {@link #parseScript(String, String)} 로 파싱한 스크립트를 전역 환경에서 실행한다.
     *
     * @param script the parsed script / 파싱된 스크립트
     * @return the completion value of the last statement, or undefined / 마지막 문장의 완료 값. 없으면 undefined
     * @throws IllegalArgumentException if the script was not created by an interpreter / 인터프리터가 만든 것이 아니면
     */
    public JsValue evaluateScript(ParsedScript script) {
        if (!(script instanceof ParsedProgram)) {
            throw new IllegalArgumentException("not a script parsed by this engine: " + script);
        }
        ParsedProgram p = (ParsedProgram) script;
        return run(p.program, p.name);
    }

    /**
     * Runs a parsed PROGRAM node as one task.
     * <p>
     * 파싱된 PROGRAM 노드를 작업 하나로 실행한다.
     *
     * @param prog the PROGRAM node / PROGRAM 노드
     * @param name the source name / 소스 이름
     * @return the completion value of the last statement, or undefined / 마지막 문장의 완료 값. 없으면 undefined
     */
    private JsValue run(Node prog, String name) {
        limits.beginTask();
        try {
            ProgramInfo pi = (ProgramInfo) prog.value;
            Ctx ctx = new Ctx(pi.strict, globalEnv, null);
            ctx.sourceName = name == null ? "" : name;
            globalDeclarationInstantiation(prog.scope, globalEnv, ctx);
            Completion c = st.execList(prog.list, globalEnv, ctx);
            return c.value == null ? Js.UNDEFINED : c.value;
        } finally {
            limits.endTask();
        }
    }

    /**
     * Loads, links and evaluates a module and returns its namespace object.
     * <p>
     * 모듈을 불러와 링크하고 평가한 뒤 네임스페이스 객체를 반환한다.
     *
     * @param key    the module key / 모듈 키
     * @param source the source text or null to fetch it with the module loader / 소스 텍스트. null 이면 모듈 로더로 가져온다
     * @return the namespace object / 네임스페이스 객체
     */
    public JsObject importModule(String key, String source) {
        limits.beginTask();
        try {
            ModuleRecord m = ModuleRecord.load(this, key, source);
            m.link();
            m.evaluate();
            return m.namespaceObject();
        } finally {
            limits.endTask();
        }
    }

    /**
     * Starts a dynamic import and returns its Promise.
     * <p>
     * 동적 import 를 시작하고 Promise 를 반환한다.
     *
     * @param capability returned Promise capability / 반환 Promise capability
     * @param specifier module specifier / 모듈 지정자
     * @param referrer referring module or script name / 참조하는 모듈/스크립트 이름
     * @return import Promise / import Promise
     */
    JsObject dynamicImport(final PromiseCapability capability, final String specifier, final String referrer) {
        org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseJobs.enqueue(realm, new Runnable() {
            public void run() {
                try {
                    ModuleLoader loader = realm.getModuleLoader();
                    if (loader == null) {
                        throw JsException.typeError("Cannot import module '" + specifier + "': no module loader is configured");
                    }
                    String key = loader.resolve(specifier, referrer);
                    ModuleRecord module = ModuleRecord.load(Interpreter.this, key, null);
                    module.link();
                    JsObject evaluation = module.evaluateAsync();
                    NativeFunction fulfilled = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                            Ops.call(capability.getResolveFunction(), Js.UNDEFINED, new JsValue[] { Js.arg(args, 0) });
                            return Js.UNDEFINED;
                        }
                    });
                    NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                            Ops.call(capability.getRejectFunction(), Js.UNDEFINED, new JsValue[] { Js.arg(args, 0) });
                            return Js.UNDEFINED;
                        }
                    });
                    PromiseInterop.await(realm, evaluation, fulfilled, rejected);
                } catch (JsException e) {
                    Ops.call(capability.getRejectFunction(), Js.UNDEFINED, new JsValue[] { e.getValue(realm) });
                }
            }
        });
        return capability.getPromise();
    }

    /**
     * Fetches and parses a module and its dependencies without linking or evaluating them. A failure of the loader or a syntax error
     * is thrown here; the returned graph links and evaluates when asked.
     * <p>
     * 모듈과 그 의존 모듈을 링크나 평가 없이 가져와 파싱한다. 로더의 실패와 구문 오류는 여기서 던져지며, 반환된 그래프는 요청할 때 링크하고 평가한다.
     *
     * @param key    the module key / 모듈 키
     * @param source the source text or null to fetch it with the module loader / 소스 텍스트. null 이면 모듈 로더로 가져온다
     * @return the loaded graph / 불러온 그래프
     */
    public ModuleGraph loadModule(String key, String source) {
        limits.beginTask();
        final ModuleRecord m;
        try {
            m = ModuleRecord.load(this, key, source);
        } finally {
            limits.endTask();
        }
        return new ModuleGraph() {
            /**
             * Links and evaluates the loaded graph.
             * <p>
             * 불러온 그래프를 링크하고 평가한다.
             *
             * @return the namespace object / 네임스페이스 객체
             */
            public JsObject evaluate() {
                limits.beginTask();
                try {
                    m.link();
                    m.evaluate();
                    return m.namespaceObject();
                } finally {
                    limits.endTask();
                }
            }

            /**
             * Links and asynchronously evaluates the loaded graph.
             * <p>
             * 불러온 그래프를 링크하고 비동기로 평가한다.
             *
             * @return Promise / Promise
             */
            public JsObject evaluateAsync() {
                limits.beginTask();
                try {
                    m.link();
                    return m.evaluateAsync();
                } finally {
                    limits.endTask();
                }
            }
        };
    }

    /**
     * GlobalDeclarationInstantiation (ECMA-262 6th edition 15.1.8): creates the var, function and lexical bindings of a script.
     * <p>
     * GlobalDeclarationInstantiation(ECMA-262 6판 15.1.8): 스크립트의 var, 함수, 렉시컬 바인딩을 만든다.
     *
     * @param scope the scope description / 스코프 설명
     * @param genv  the global environment / 전역 환경
     * @param ctx   the context / 문맥
     */
    void globalDeclarationInstantiation(ScopeInfo scope, Environment genv, Ctx ctx) {
        JsObject global = realm.getGlobalObject();
        // 렉시컬 이름이 기존 전역 렉시컬/변수와 충돌하면 SyntaxError 이다.
        for (int i = 0; i < scope.lexNames.length; i++) {
            String n = scope.lexNames[i];
            if (genv.own(n) != null) {
                throw JsException.syntaxError("Identifier '" + n + "' has already been declared");
            }
            Property p = global.getOwnPropertySlot(n);
            if (p != null && !p.configurable) {
                throw JsException.syntaxError("Identifier '" + n + "' has already been declared");
            }
        }
        // var 이름이 전역 렉시컬 선언과 충돌하는지 확인한다.
        for (int i = 0; i < scope.varNames.length; i++) {
            if (genv.own(scope.varNames[i]) != null) {
                throw JsException.syntaxError("Identifier '" + scope.varNames[i] + "' has already been declared");
            }
        }
        // 함수 선언을 만들 수 있는지(CanDeclareGlobalFunction)를 어떤 바인딩도 만들기 전에 모두 확인한다.
        for (int i = 0; i < scope.functions.length; i++) {
            String name = scope.functions[i].a.name;
            Property cur = global.getOwnPropertySlot(name);
            boolean ok;
            if (cur == null) {
                ok = global.isExtensible();
            } else {
                ok = cur.configurable || (!cur.accessor && cur.writable && cur.enumerable);
            }
            if (!ok) {
                throw JsException.typeError("Cannot declare global function '" + name + "'");
            }
        }
        // var 이름도 마찬가지로 CanDeclareGlobalVar 를 먼저 확인한다.
        for (int i = 0; i < scope.varNames.length; i++) {
            String name = scope.varNames[i];
            if (global.getOwnPropertySlot(name) == null && !global.isExtensible()) {
                throw JsException.typeError("Cannot declare global variable '" + name + "'");
            }
        }
        // 함수 선언: 같은 이름이 여러 번이면 마지막 것이 이긴다(CreateGlobalFunctionBinding).
        for (int i = 0; i < scope.functions.length; i++) {
            Node decl = scope.functions[i];
            String name = decl.a.name;
            ScriptFunction f = createClosure(decl.b, genv, null);
            Property cur = global.getOwnPropertySlot(name);
            if (cur == null || cur.configurable) {
                global.defineOwnProperty(name, PropertyDescriptor.data(f, true, true, false));
            } else {
                global.set(name, f);
            }
        }
        // var 이름: 전역 객체에 아직 없으면 undefined 로 만든다.
        for (int i = 0; i < scope.varNames.length; i++) {
            String name = scope.varNames[i];
            if (global.getOwnPropertySlot(name) == null) {
                global.defineOwnProperty(name, PropertyDescriptor.data(Js.UNDEFINED, true, true, false));
            }
        }
        declareLexicals(scope, genv);
    }

    /**
     * Creates the uninitialized let, const and class bindings of a scope.
     * <p>
     * 스코프의 초기화되지 않은 let, const, class 바인딩을 만든다.
     *
     * @param scope the scope description / 스코프 설명
     * @param env   the environment receiving the bindings / 바인딩을 받는 환경
     */
    void declareLexicals(ScopeInfo scope, Environment env) {
        // 이름마다 일시적 사각지대 상태의 바인딩을 만든다.
        for (int i = 0; i < scope.lexNames.length; i++) {
            env.declare(scope.lexNames[i], !scope.lexConst[i], false, null).lexical = true;
        }
    }

    /**
     * Instantiates the block-level function declarations of a scope in an environment.
     * <p>
     * 환경에 스코프의 블록 수준 함수 선언을 인스턴스화한다.
     *
     * @param scope the scope description / 스코프 설명
     * @param env   the environment / 환경
     */
    void instantiateBlockFunctions(ScopeInfo scope, Environment env) {
        // 블록의 함수 선언은 블록에 들어갈 때 모두 만들어 둔다(호이스팅).
        for (int i = 0; i < scope.functions.length; i++) {
            Node decl = scope.functions[i];
            ScriptFunction f = createClosure(decl.b, env, null);
            Binding b = env.own(decl.a.name);
            if (b == null) {
                env.declare(decl.a.name, true, true, f);
            } else {
                b.value = f;
                b.initialized = true;
            }
        }
    }

    // ------------------------------------------------------------------ functions

    /**
     * Creates a function object for a FUNCTION node closing over an environment (InstantiateFunctionObject).
     * <p>
     * 환경을 닫아 두는 FUNCTION 노드의 함수 객체를 만든다(InstantiateFunctionObject).
     *
     * @param fnNode   the FUNCTION node / FUNCTION 노드
     * @param env      the environment to close over / 닫아 둘 환경
     * @param nameHint the inferred name for anonymous functions or null / 익명 함수에 추론한 이름 또는 null
     * @return the function / 함수
     */
    ScriptFunction createClosure(Node fnNode, Environment env, String nameHint) {
        FunctionInfo info = fnNode.fn;
        JsObject proto = realm.getFunctionPrototype();
        if (info.generator && realm.getIntrinsic("%GeneratorFunction.prototype%") != null) {
            proto = realm.getIntrinsic("%GeneratorFunction.prototype%");
        }
        String name = info.name.length() > 0 ? info.name : (nameHint != null ? nameHint : "");
        return new ScriptFunction(this, realm, proto, info, env, name);
    }

    /**
     * Returns the this value for a call (OrdinaryCallBindThis).
     * <p>
     * 호출의 this 값을 구한다(OrdinaryCallBindThis).
     *
     * @param strict true for strict functions / 엄격 함수이면 true
     * @param thisArg the passed this / 전달된 this
     * @return the this value to bind / 바인딩할 this 값
     */
    JsValue coerceThis(boolean strict, JsValue thisArg) {
        if (strict) {
            return thisArg;
        }
        if (thisArg.isNullOrUndefined()) {
            return realm.getGlobalObject();
        }
        return Ops.toObject(realm, thisArg);
    }

    /**
     * Counts a call: enforces the step and call depth limits.
     * <p>
     * 호출을 센다: 단계 수와 호출 깊이 제한을 검사한다.
     */
    void enter() {
        limits.tick();
        if (depth >= limits.getMaxCallDepth()) {
            throw JsException.rangeError("Maximum call stack size exceeded");
        }
        depth++;
    }

    /**
     * Calls a script function ([[Call]], ECMA-262 6th edition 9.2.1).
     * <p>
     * 스크립트 함수를 호출한다([[Call]], ECMA-262 6판 9.2.1).
     *
     * @param f        the function / 함수
     * @param thisArg  the this value / this 값
     * @param args     the arguments / 인수
     * @return the result / 결과
     */
    JsValue callFunction(ScriptFunction f, JsValue thisArg, JsValue[] args) {
        enter();
        try {
            ScriptFunction cur = f;
            JsValue curThis = thisArg;
            JsValue[] curArgs = args;
            // 꼬리 호출이 요청되면 같은 호출 깊이에서 대상 함수를 이어서 실행한다(ES2015 14.6, 트램펄린).
            for (;;) {
                Ctx ctx = prepareCall(cur, curThis, curArgs, null);
                if (cur.info.async && !cur.info.generator) {
                    return AsyncFunction.start(this, cur, ctx);
                }
                if (cur.info.async && cur.info.generator) {
                    return AsyncGeneratorObject.create(this, cur, ctx);
                }
                if (cur.info.generator) {
                    return GeneratorObject.create(this, cur, ctx);
                }
                ctx.tailAllowed = cur.info.strict;
                JsValue r = runBody(cur, ctx);
                if (ctx.tailTarget == null) {
                    return r;
                }
                cur = ctx.tailTarget;
                curThis = ctx.tailThis;
                curArgs = ctx.tailArgs;
                limits.tick();
            }
        } catch (StackOverflowError e) {
            throw JsException.rangeError("Maximum call stack size exceeded");
        } finally {
            depth--;
        }
    }

    /**
     * Constructs an object with a script function ([[Construct]], ECMA-262 6th edition 9.2.2).
     * <p>
     * 스크립트 함수로 객체를 만든다([[Construct]], ECMA-262 6판 9.2.2).
     *
     * @param f         the constructor / 생성자
     * @param args      the arguments / 인수
     * @param newTarget the new.target / new.target
     * @return the new object / 새 객체
     */
    JsObject constructFunction(ScriptFunction f, JsValue[] args, JsObject newTarget) {
        enter();
        try {
            boolean derived = f.info.derived;
            if (f.defaultConstructor) {
                return constructDefault(f, args, newTarget);
            }
            JsObject obj = null;
            if (!derived) {
                obj = new JsObject(Ops.getPrototypeFromConstructor(realm, newTarget, realm.getObjectPrototype()));
            }
            Ctx ctx = prepareCall(f, obj == null ? Js.UNDEFINED : obj, args, newTarget);
            if (!derived && obj != null) {
                cls.initializeInstanceFields(f, obj);
            }
            JsValue r = runBody(f, ctx);
            if (r instanceof JsObject) {
                return (JsObject) r;
            }
            if (!derived) {
                return obj;
            }
            if (!r.isUndefined()) {
                throw JsException.typeError("Derived constructors may only return object or undefined");
            }
            return (JsObject) ctx.funcEnv.getThis();
        } catch (StackOverflowError e) {
            throw JsException.rangeError("Maximum call stack size exceeded");
        } finally {
            depth--;
        }
    }

    /**
     * Runs the implicit constructor of a class without explicit constructor.
     * <p>
     * 명시적 생성자가 없는 클래스의 암시적 생성자를 실행한다.
     *
     * @param f         the class constructor / 클래스 생성자
     * @param args      the arguments / 인수
     * @param newTarget the new.target / new.target
     * @return the new object / 새 객체
     */
    private JsObject constructDefault(ScriptFunction f, JsValue[] args, JsObject newTarget) {
        if (!f.info.derived) {
            JsObject object = new JsObject(Ops.getPrototypeFromConstructor(realm, newTarget, realm.getObjectPrototype()));
            cls.initializeInstanceFields(f, object);
            return object;
        }
        JsObject parent = f.getPrototypeOf();
        if (parent == null || !parent.isConstructor()) {
            throw JsException.typeError("Super constructor " + JsException.describe(parent) + " of anonymous class is not a constructor");
        }
        JsObject object = parent.construct(args, newTarget);
        cls.initializeInstanceFields(f, object);
        return object;
    }

    /**
     * Creates the function environment and instantiates declarations (PrepareForOrdinaryCall and
     * FunctionDeclarationInstantiation).
     * <p>
     * 함수 환경을 만들고 선언을 인스턴스화한다(PrepareForOrdinaryCall, FunctionDeclarationInstantiation).
     *
     * @param f         the function / 함수
     * @param thisArg   the this value (ignored for derived constructors and arrow functions) / this 값(파생 생성자와 화살표 함수는 무시)
     * @param args      the arguments / 인수
     * @param newTarget the new.target or null / new.target 또는 null
     * @return the execution context / 실행 문맥
     */
    private Ctx prepareCall(ScriptFunction f, JsValue thisArg, JsValue[] args, JsObject newTarget) {
        FunctionInfo info = f.info;
        Environment fenv = new Environment(f.scope);
        fenv.function = f;
        fenv.newTarget = newTarget;
        if (!info.arrow) {
            fenv.hasThis = true;
            if (info.derived && newTarget != null) {
                fenv.thisBound = false;
            } else {
                fenv.thisValue = coerceThis(info.strict, thisArg);
                fenv.thisBound = true;
            }
        }
        Ctx ctx = new Ctx(info.strict, fenv, f);
        ctx.sourceName = info.sourceName == null ? "" : info.sourceName;
        ctx.funcEnv = fenv;
        if (info.generator || info.async) {
            ctx.gen = new GenState();
        }
        functionDeclarationInstantiation(f, fenv, args, ctx);
        return ctx;
    }

    /**
     * Runs the body of a prepared function call.
     * <p>
     * 준비된 함수 호출의 본문을 실행한다.
     *
     * @param f   the function / 함수
     * @param ctx the context / 문맥
     * @return the returned value / 반환 값
     */
    JsValue runBody(ScriptFunction f, Ctx ctx) {
        FunctionInfo info = f.info;
        if (info.body == null) {
            return Js.UNDEFINED;
        }
        if (info.exprBody) {
            return ctx.tailAllowed ? ex.evalTail(info.body, ctx.varEnv, ctx) : ex.eval(info.body, ctx.varEnv, ctx);
        }
        Completion c = st.exec(info.body, ctx.varEnv, ctx);
        return c.type == Completion.RETURN ? c.value : Js.UNDEFINED;
    }

    /**
     * FunctionDeclarationInstantiation (ECMA-262 6th edition 9.2.12): binds parameters, creates the arguments object, hoists
     * var names and function declarations and prepares lexical declarations.
     * <p>
     * FunctionDeclarationInstantiation(ECMA-262 6판 9.2.12): 매개변수를 바인딩하고, arguments 객체를 만들고, var 이름과 함수 선언을
     * 끌어올리고, 렉시컬 선언을 준비한다.
     *
     * @param f    the function / 함수
     * @param fenv the function environment / 함수 환경
     * @param args the arguments / 인수
     * @param ctx  the context (its variable environment is set here) / 문맥(변수 환경을 여기서 정한다)
     */
    private void functionDeclarationInstantiation(ScriptFunction f, Environment fenv, JsValue[] args, Ctx ctx) {
        FunctionInfo info = f.info;
        if (f.defaultConstructor || info.body == null) {
            return;
        }
        String[] params = info.paramNames;
        boolean simple = info.simpleParams;
        // 매개변수 이름을 바인딩한다. 단순 목록이면 바로 초기화하고 아니면 사각지대로 시작한다.
        for (int i = 0; i < params.length; i++) {
            if (fenv.own(params[i]) == null) {
                fenv.declare(params[i], true, simple, simple ? Js.UNDEFINED : null);
            }
        }
        ScopeInfo scope = info.scope;
        boolean needArguments = !info.arrow && (info.usesArguments || info.hasDirectEval);
        if (needArguments) {
            needArguments = !contains(params, "arguments") && !contains(scope.lexNames, "arguments") && !functionNamed(scope, "arguments");
        }
        if (needArguments) {
            JsObject argsObj = createArguments(f, args, fenv);
            Binding ab = fenv.declare("arguments", !info.strict, true, argsObj);
            ab.mutable = !info.strict;
        }
        // 매개변수 값을 넣는다.
        if (simple) {
            for (int i = 0; i < params.length; i++) {
                fenv.own(params[i]).value = i < args.length ? args[i] : Js.UNDEFINED;
            }
        } else {
            bindParameters(info.params, args, fenv, ctx);
        }
        // 본문의 var 이름과 함수 선언을 끌어올린다.
        Environment varEnv = fenv;
        if (info.hasParamExpressions) {
            varEnv = new Environment(fenv);
            // 매개변수 식이 있으면 본문 var 환경을 따로 만들고 같은 이름의 매개변수 값을 복사한다.
            for (int i = 0; i < scope.varNames.length; i++) {
                String n = scope.varNames[i];
                Binding pb = fenv.own(n);
                varEnv.declare(n, true, true, pb != null ? pb.value : Js.UNDEFINED);
            }
        } else {
            // var 이름마다 아직 없으면 undefined 로 선언한다.
            for (int i = 0; i < scope.varNames.length; i++) {
                if (fenv.own(scope.varNames[i]) == null) {
                    fenv.declare(scope.varNames[i], true, true, Js.UNDEFINED);
                }
            }
        }
        ctx.varEnv = varEnv;
        declareLexicals(scope, varEnv);
        // 함수 선언은 var 환경의 이름에 할당한다.
        for (int i = 0; i < scope.functions.length; i++) {
            Node decl = scope.functions[i];
            ScriptFunction fn = createClosure(decl.b, varEnv, null);
            Binding b = varEnv.own(decl.a.name);
            if (b == null) {
                varEnv.declare(decl.a.name, true, true, fn);
            } else {
                b.value = fn;
                b.initialized = true;
            }
        }
    }

    /**
     * Tests whether a name array contains a name.
     * <p>
     * 이름 배열에 이름이 있는지 검사한다.
     *
     * @param names the names / 이름들
     * @param name  the name / 이름
     * @return true if contained / 있으면 true
     */
    private static boolean contains(String[] names, String name) {
        // 이름 배열에 해당 이름이 있는지 찾는다.
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether the scope declares a function with a name.
     * <p>
     * 스코프가 이름의 함수를 선언하는지 검사한다.
     *
     * @param scope the scope / 스코프
     * @param name  the name / 이름
     * @return true if a function declaration has the name / 함수 선언 중 그 이름이 있으면 true
     */
    private static boolean functionNamed(ScopeInfo scope, String name) {
        // 스코프의 함수 선언 중 같은 이름이 있는지 찾는다.
        for (int i = 0; i < scope.functions.length; i++) {
            if (scope.functions[i].a.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Binds a non-simple parameter list: defaults, destructuring patterns and the rest parameter.
     * <p>
     * 단순하지 않은 매개변수 목록을 바인딩한다: 기본값, 구조 분해 패턴, 나머지 매개변수.
     *
     * @param params the parameter nodes / 매개변수 노드
     * @param args   the arguments / 인수
     * @param fenv   the function environment / 함수 환경
     * @param ctx    the context / 문맥
     */
    private void bindParameters(Node[] params, JsValue[] args, Environment fenv, Ctx ctx) {
        // 매개변수를 앞에서부터 차례로 인수와 짝지어 초기화한다.
        for (int i = 0; i < params.length; i++) {
            Node p = params[i];
            if (p.type == NodeType.REST) {
                JsArray rest = realm.newArray();
                // 나머지 인수를 rest 배열에 모은다.
                for (int k = i; k < args.length; k++) {
                    rest.add(args[k]);
                }
                pat.bind(p.a, rest, fenv, ctx, Patterns.INIT);
                break;
            }
            JsValue v = i < args.length ? args[i] : Js.UNDEFINED;
            pat.bindElement(p, v, fenv, ctx, Patterns.INIT);
        }
    }

    /**
     * Creates the arguments object (CreateMappedArgumentsObject and CreateUnmappedArgumentsObject).
     * <p>
     * arguments 객체를 만든다(CreateMappedArgumentsObject, CreateUnmappedArgumentsObject).
     *
     * @param f    the function / 함수
     * @param args the arguments / 인수
     * @param fenv the function environment / 함수 환경
     * @return the arguments object / arguments 객체
     */
    private JsObject createArguments(ScriptFunction f, JsValue[] args, Environment fenv) {
        FunctionInfo info = f.info;
        boolean mappedKind = !info.strict && info.simpleParams;
        Binding[] mapped = null;
        if (mappedKind) {
            mapped = new Binding[args.length];
            String[] names = info.paramNames;
            // 같은 이름이 중복되면 마지막 매개변수가 매핑된다.
            for (int i = Math.min(args.length, names.length) - 1; i >= 0; i--) {
                boolean taken = false;
                // 뒤에 같은 이름의 매개변수가 있는지 확인한다.
                for (int k = i + 1; k < names.length; k++) {
                    if (names[k].equals(names[i])) {
                        taken = true;
                    }
                }
                if (!taken) {
                    mapped[i] = fenv.own(names[i]);
                }
            }
        }
        ArgumentsObject a = new ArgumentsObject(realm.getObjectPrototype(), mapped);
        a.defineData("length", JsNumber.of(args.length), true, false, true);
        // 전달된 인수를 인덱스 프로퍼티로 만든다.
        for (int i = 0; i < args.length; i++) {
            a.createDataProperty(PropertyKeys.fromIndex(i), args[i]);
        }
        JsObject values = realm.getIntrinsic("%ArrayProto_values%");
        if (values != null) {
            a.defineBuiltin(WellKnownSymbols.ITERATOR, values);
        }
        if (mappedKind) {
            a.defineBuiltin("callee", f);
        } else {
            JsObject thrower = realm.getIntrinsic("%ThrowTypeError%");
            a.defineAccessor("callee", thrower, thrower, false, false);
        }
        return a;
    }

    // ------------------------------------------------------------------ dynamic code

    /**
     * Runs source text as indirect eval code in the global environment (the behavior of the global <code>eval</code> function
     * when it is not called directly by name).
     * <p>
     * 소스 텍스트를 전역 환경의 간접 eval 코드로 실행한다(이름으로 직접 호출되지 않은 전역 <code>eval</code> 함수의 동작).
     *
     * @param source the source text / 소스 텍스트
     * @return the completion value / 완료 값
     */
    public JsValue evalIndirect(String source) {
        return performEval(source, false, null, null);
    }

    /**
     * Runs eval code. Direct eval uses the caller's environment; indirect eval uses the global environment (PerformEval,
     * ECMA-262 6th edition 18.2.1.1).
     * <p>
     * eval 코드를 실행한다. 직접 eval 은 호출자의 환경을, 간접 eval 은 전역 환경을 쓴다(PerformEval, ECMA-262 6판 18.2.1.1).
     *
     * @param source the source text / 소스 텍스트
     * @param direct true for a direct eval call / 직접 eval 호출이면 true
     * @param env    the caller's environment (direct eval only) / 호출자의 환경(직접 eval 전용)
     * @param ctx    the caller's context (direct eval only) / 호출자의 문맥(직접 eval 전용)
     * @return the completion value / 완료 값
     */
    JsValue performEval(String source, boolean direct, Environment env, Ctx ctx) {
        Parser.EvalOptions o = new Parser.EvalOptions();
        Environment callerEnv = direct ? env : globalEnv;
        if (direct) {
            o.strict = ctx.strict;
            Environment te = env.thisEnvironment();
            if (te != null && te.function != null) {
                o.allowNewTarget = true;
                o.allowSuperProp = te.function.homeObject != null;
                o.allowSuperCall = te.function.info.derived && te.function.info.classConstructor;
            }
        }
        Node prog = Parser.parseEval(source, "eval", o);
        ProgramInfo pi = (ProgramInfo) prog.value;
        boolean strict = pi.strict;
        Environment lexEnv = new Environment(callerEnv);
        Environment varEnv;
        Ctx evalCtx;
        if (strict) {
            varEnv = lexEnv;
            evalCtx = new Ctx(true, varEnv, direct ? ctx.function : null);
        } else {
            varEnv = direct ? ctx.varEnv : globalEnv;
            evalCtx = new Ctx(false, varEnv, direct ? ctx.function : null);
        }
        evalCtx.funcEnv = direct ? ctx.funcEnv : null;
        // eval 코드의 줄 번호는 eval 문자열 안의 위치이므로 호출 위치의 스크립트 이름과 섞지 않는다.
        evalCtx.sourceName = "eval";
        evalDeclarationInstantiation(prog.scope, callerEnv, varEnv, lexEnv, strict);
        Completion c = st.execList(prog.list, lexEnv, evalCtx);
        return c.value == null ? Js.UNDEFINED : c.value;
    }

    /**
     * EvalDeclarationInstantiation (ECMA-262 6th edition 18.2.1.2): var and function declarations of non-strict eval code
     * go to the variable environment (as deletable bindings); everything is local to the eval in strict mode.
     * <p>
     * EvalDeclarationInstantiation(ECMA-262 6판 18.2.1.2): 느슨한 eval 코드의 var 와 함수 선언은 변수 환경에 (삭제 가능한 바인딩으로)
     * 들어가며, 엄격 모드에서는 모두 eval 안에서만 유효하다.
     *
     * @param scope     the scope description / 스코프 설명
     * @param callerEnv the environment the eval code runs in / eval 코드가 실행되는 환경
     * @param varEnv    the variable environment / 변수 환경
     * @param lexEnv    the environment for lexical declarations / 렉시컬 선언용 환경
     * @param strict    true for strict eval code / 엄격 eval 코드이면 true
     */
    private void evalDeclarationInstantiation(ScopeInfo scope, Environment callerEnv, Environment varEnv, Environment lexEnv, boolean strict) {
        if (!strict) {
            checkEvalVarConflicts(scope, callerEnv, varEnv);
        }
        declareLexicals(scope, lexEnv);
        // eval 코드의 함수 선언을 변수 환경에 만든다.
        for (int i = 0; i < scope.functions.length; i++) {
            Node decl = scope.functions[i];
            ScriptFunction f = createClosure(decl.b, lexEnv, null);
            declareEvalVar(varEnv, decl.a.name, f, true, strict);
        }
        // eval 코드의 var 이름을 변수 환경에 만든다.
        for (int i = 0; i < scope.varNames.length; i++) {
            declareEvalVar(varEnv, scope.varNames[i], Js.UNDEFINED, false, strict);
        }
    }

    /**
     * Checks that the var and function names of non-strict eval code do not collide with a let, const or class binding between
     * the eval code and its variable environment (EvalDeclarationInstantiation step 5, ECMA-262 6th edition 18.2.1.2).
     * <p>
     * 느슨한 eval 코드의 var 와 함수 이름이 eval 코드와 변수 환경 사이의 let, const, class 바인딩과 충돌하지 않는지 확인한다
     * (EvalDeclarationInstantiation 5단계, ECMA-262 6판 18.2.1.2).
     *
     * @param scope     the scope description of the eval code / eval 코드의 스코프 설명
     * @param callerEnv the environment the eval code runs in / eval 코드가 실행되는 환경
     * @param varEnv    the variable environment / 변수 환경
     */
    private void checkEvalVarConflicts(ScopeInfo scope, Environment callerEnv, Environment varEnv) {
        // eval 이 실행되는 환경에서 변수 환경까지 바깥쪽으로 올라가며 확인한다. with 환경은 렉시컬 선언을 가질 수 없어 건너뛴다.
        for (Environment e = callerEnv; e != null; e = e.outer) {
            if (!e.withEnv) {
                // 이 환경의 let/const/class 바인딩과 같은 이름이 있으면 오류이다.
                for (int i = 0; i < scope.varNames.length; i++) {
                    Binding b = e.own(scope.varNames[i]);
                    if (b != null && b.lexical) {
                        throw JsException.syntaxError("Identifier '" + scope.varNames[i] + "' has already been declared");
                    }
                }
                // eval 코드의 함수 선언 이름이 렉시컬 바인딩과 충돌하는지 확인한다.
                for (int i = 0; i < scope.functions.length; i++) {
                    Binding b = e.own(scope.functions[i].a.name);
                    if (b != null && b.lexical) {
                        throw JsException.syntaxError("Identifier '" + scope.functions[i].a.name + "' has already been declared");
                    }
                }
            }
            if (e == varEnv) {
                break;
            }
        }
    }

    /**
     * Declares a var or function binding created by eval code.
     * <p>
     * eval 코드가 만든 var 또는 함수 바인딩을 선언한다.
     *
     * @param varEnv   the variable environment / 변수 환경
     * @param name     the name / 이름
     * @param value    the value / 값
     * @param override true to replace the value of an existing binding (functions) / 기존 바인딩의 값을 바꾸려면 true(함수)
     * @param strict   true for strict eval code / 엄격 eval 코드이면 true
     */
    private void declareEvalVar(Environment varEnv, String name, JsValue value, boolean override, boolean strict) {
        if (varEnv == globalEnv) {
            JsObject g = realm.getGlobalObject();
            Property p = g.getOwnPropertySlot(name);
            if (p == null) {
                g.defineOwnProperty(name, PropertyDescriptor.data(value, true, true, !strict));
            } else if (override) {
                if (p.configurable) {
                    g.defineOwnProperty(name, PropertyDescriptor.data(value, true, true, true));
                } else {
                    g.set(name, value);
                }
            }
            return;
        }
        Binding b = varEnv.own(name);
        if (b == null) {
            Binding nb = varEnv.declare(name, true, true, value);
            nb.deletable = !strict;
        } else if (override) {
            b.value = value;
            b.initialized = true;
        }
    }

    /**
     * Creates a function from source text for the Function and GeneratorFunction constructors (CreateDynamicFunction).
     * <p>
     * Function 과 GeneratorFunction 생성자를 위해 소스 텍스트로 함수를 만든다(CreateDynamicFunction).
     *
     * @param args      the constructor arguments: parameter names followed by the body / 생성자 인수: 매개변수 이름들과 마지막에 본문
     * @param generator true for GeneratorFunction / GeneratorFunction 이면 true
     * @return the function / 함수
     */
    public JsObject createDynamicFunction(JsValue[] args, boolean generator) {
        StringBuilder params = new StringBuilder();
        // 마지막 인수 앞의 인수를 쉼표로 이어 매개변수 목록을 만든다.
        for (int i = 0; i + 1 < args.length; i++) {
            if (i > 0) {
                params.append(',');
            }
            params.append(Ops.toString(args[i]));
        }
        String body = args.length == 0 ? "" : Ops.toString(args[args.length - 1]);
        Node fn = Parser.parseFunctionConstructor(params.toString(), body, generator, "anonymous");
        return createClosure(fn, globalEnv, "anonymous");
    }

    /**
     * Converts a thrown Java exception into the script value to give to a catch clause.
     * <p>
     * 던져진 Java 예외를 catch 절에 줄 스크립트 값으로 바꾼다.
     *
     * @param e the exception / 예외
     * @return the thrown value / 던져진 값
     */
    JsValue thrownValue(JsException e) {
        return e.getValue(realm);
    }

    /**
     * Returns the realm.
     * <p>
     * realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return realm;
    }

    /**
     * Returns a short text of a value for error messages without running script code.
     * <p>
     * 스크립트 코드를 실행하지 않고 오류 메시지용 값 설명을 반환한다.
     *
     * @param v the value / 값
     * @return the text / 문자열
     */
    static String describe(JsValue v) {
        if (v instanceof JsString) {
            return "\"" + ((JsString) v).getValue() + "\"";
        }
        return JsException.describe(v);
    }
}
