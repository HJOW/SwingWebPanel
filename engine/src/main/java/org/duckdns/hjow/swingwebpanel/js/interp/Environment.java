package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.HashMap;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * A lexical environment (ECMA-262 6th edition 8.1) with its environment record. One class covers the record kinds:
 * declarative records (a map of {@link Binding}s), object records (the global object and <code>with</code> objects) and
 * the global record (declarative part first, then the global object). Function, global and module environments also carry
 * the <code>this</code> binding, the function object and new.target.
 * <p>
 * 렉시컬 환경(ECMA-262 6판 8.1)과 그 환경 레코드. 하나의 클래스가 선언적 레코드({@link Binding} 맵), 객체 레코드(전역 객체와
 * <code>with</code> 객체), 전역 레코드(선언적 부분 우선, 그다음 전역 객체)를 모두 나타낸다. 함수, 전역, 모듈 환경은
 * <code>this</code> 바인딩, 함수 객체, new.target 도 가진다.
 */
final class Environment {

    /**
     * The enclosing environment.
     * <p>
     * 바깥 환경.
     */
    final Environment outer;

    /**
     * The declarative bindings (null until the first declaration).
     * <p>
     * 선언적 바인딩(첫 선언 전에는 null).
     */
    HashMap<String, Binding> bindings;

    /**
     * The object of an object record (global object or with object), or null.
     * <p>
     * 객체 레코드의 객체(전역 객체 또는 with 객체). 없으면 null.
     */
    JsObject objectRecord;

    /**
     * True for the object record of a with statement (honours Symbol.unscopables).
     * <p>
     * with 문의 객체 레코드이면 true(Symbol.unscopables 를 따른다).
     */
    boolean withEnv;

    /**
     * True if this environment has a this binding (function, global and module environments except arrow functions).
     * <p>
     * 이 환경이 this 바인딩을 가지면 true(화살표 함수를 제외한 함수, 전역, 모듈 환경).
     */
    boolean hasThis;

    /**
     * The this value once bound.
     * <p>
     * 바인딩된 this 값.
     */
    JsValue thisValue;

    /**
     * False while the this binding is uninitialized (derived constructors before super()).
     * <p>
     * this 바인딩이 초기화되지 않은 동안 false(super() 이전의 파생 생성자).
     */
    boolean thisBound;

    /**
     * The function whose call created this environment (for super and new.target), or null.
     * <p>
     * 이 환경을 만든 호출의 함수(super 와 new.target 용). 없으면 null.
     */
    ScriptFunction function;

    /**
     * The new.target of the call, or null when called without new.
     * <p>
     * 호출의 new.target. new 없이 호출했으면 null.
     */
    JsObject newTarget;

    /**
     * True for static class field, method or block evaluation where <code>super</code> starts from the constructor's prototype.
     * <p>
     * static 클래스 필드·메서드·블록 평가에서 <code>super</code> 가 생성자의 프로토타입에서 시작하면 true.
     */
    boolean classStaticContext;

    /**
     * The module this environment belongs to (module top-level environments), or null.
     * <p>
     * 이 환경이 속한 모듈(모듈 최상위 환경). 없으면 null.
     */
    ModuleRecord module;

    /**
     * Creates an environment.
     * <p>
     * 환경을 만든다.
     *
     * @param outer the enclosing environment or null / 바깥 환경 또는 null
     */
    Environment(Environment outer) {
        this.outer = outer;
    }

    /**
     * Creates a declarative binding.
     * <p>
     * 선언적 바인딩을 만든다.
     *
     * @param name        the name / 이름
     * @param mutable     true if assignable / 대입 가능하면 true
     * @param initialized false to start in the temporal dead zone / 일시적 사각지대에서 시작하려면 false
     * @param value       the initial value (ignored if not initialized) / 초기 값(초기화하지 않으면 무시)
     * @return the binding / 바인딩
     */
    Binding declare(String name, boolean mutable, boolean initialized, JsValue value) {
        if (bindings == null) {
            bindings = new HashMap<String, Binding>();
        }
        Binding b = new Binding();
        b.mutable = mutable;
        b.initialized = initialized;
        b.value = initialized ? value : null;
        bindings.put(name, b);
        return b;
    }

    /**
     * Returns the declarative binding of a name in this environment only.
     * <p>
     * 이 환경에서만 이름의 선언적 바인딩을 반환한다.
     *
     * @param name the name / 이름
     * @return the binding or null / 바인딩 또는 null
     */
    Binding own(String name) {
        return bindings == null ? null : bindings.get(name);
    }

    /**
     * Tests whether this environment (not its outer ones) has a binding for the name.
     * <p>
     * 이 환경(바깥 환경 제외)에 이름의 바인딩이 있는지 검사한다.
     *
     * @param name the name / 이름
     * @return true if bound / 바인딩이 있으면 true
     */
    boolean has(String name) {
        if (bindings != null && bindings.containsKey(name)) {
            return true;
        }
        if (objectRecord != null && objectRecord.hasProperty(name)) {
            if (withEnv) {
                JsValue unscopables = objectRecord.get(WellKnownSymbols.UNSCOPABLES);
                if (unscopables instanceof JsObject && Ops.toBoolean(((JsObject) unscopables).get(name))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Finds the nearest environment that has a binding for the name (ResolveBinding).
     * <p>
     * 이름의 바인딩을 가진 가장 가까운 환경을 찾는다(ResolveBinding).
     *
     * @param name the name / 이름
     * @return the environment or null if the reference is unresolvable / 환경. 찾을 수 없으면 null
     */
    Environment lookup(String name) {
        Environment e = this;
        // 바깥 환경으로 올라가며 이름의 바인딩을 찾는다.
        while (e != null) {
            if (e.has(name)) {
                return e;
            }
            e = e.outer;
        }
        return null;
    }

    /**
     * Reads a binding of this environment (GetBindingValue).
     * <p>
     * 이 환경의 바인딩을 읽는다(GetBindingValue).
     *
     * @param name   the name / 이름
     * @param strict true in strict mode code / 엄격 모드 코드이면 true
     * @return the value / 값
     */
    JsValue get(String name, boolean strict) {
        Binding b = bindings == null ? null : bindings.get(name);
        if (b != null) {
            return b.read(name);
        }
        if (withEnv && !objectRecord.hasProperty(name)) {
            if (strict) {
                throw JsException.referenceError(name + " is not defined");
            }
            return Js.UNDEFINED;
        }
        return objectRecord.get(name);
    }

    /**
     * Assigns a binding of this environment (SetMutableBinding).
     * <p>
     * 이 환경의 바인딩에 대입한다(SetMutableBinding).
     *
     * @param name   the name / 이름
     * @param v      the value / 값
     * @param strict true in strict mode code / 엄격 모드 코드이면 true
     */
    void set(String name, JsValue v, boolean strict) {
        Binding b = bindings == null ? null : bindings.get(name);
        if (b != null) {
            Binding target = b;
            // import 바인딩은 읽기 전용이다.
            if (b.link != null) {
                throw JsException.typeError("Assignment to constant variable.");
            }
            if (!target.initialized) {
                throw JsException.referenceError("Cannot access '" + name + "' before initialization");
            }
            if (!target.mutable) {
                if (target.silent && !strict) {
                    return;
                }
                throw JsException.typeError("Assignment to constant variable.");
            }
            target.value = v;
            return;
        }
        if (!objectRecord.set(name, v) && strict) {
            throw JsException.typeError("Cannot assign to read only property '" + name + "' of object");
        }
    }

    /**
     * Initializes a binding that is in the temporal dead zone (InitializeBinding).
     * <p>
     * 일시적 사각지대에 있는 바인딩을 초기화한다(InitializeBinding).
     *
     * @param name the name / 이름
     * @param v    the value / 값
     */
    void initialize(String name, JsValue v) {
        Binding b = bindings.get(name);
        b.value = v;
        b.initialized = true;
    }

    /**
     * Returns the nearest environment that has a this binding (GetThisEnvironment).
     * <p>
     * this 바인딩을 가진 가장 가까운 환경을 반환한다(GetThisEnvironment).
     *
     * @return the environment / 환경
     */
    Environment thisEnvironment() {
        Environment e = this;
        // 화살표 함수 환경은 this 바인딩이 없으므로 바깥으로 올라간다.
        while (e != null && !e.hasThis) {
            e = e.outer;
        }
        return e;
    }

    /**
     * Returns the this value of this environment, throwing a ReferenceError if it is not yet bound.
     * <p>
     * 이 환경의 this 값을 반환한다. 아직 바인딩되지 않았으면 ReferenceError 를 던진다.
     *
     * @return the this value / this 값
     */
    JsValue getThis() {
        if (!thisBound) {
            throw JsException.referenceError("Must call super constructor in derived class before accessing 'this' or returning from derived constructor");
        }
        return thisValue;
    }

    /**
     * Binds the this value once (BindThisValue).
     * <p>
     * this 값을 한 번 바인딩한다(BindThisValue).
     *
     * @param v the value / 값
     */
    void bindThis(JsValue v) {
        if (thisBound) {
            throw JsException.referenceError("Super constructor may only be called once");
        }
        thisValue = v;
        thisBound = true;
    }
}
