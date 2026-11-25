package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsFunction;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;

/**
 * A function object created from JavaScript source (an ECMAScript function object, ECMA-262 6th edition 9.2): it keeps the
 * parsed {@link FunctionInfo}, the environment it closes over and, for methods, its home object. Normal functions and generator
 * functions receive their <code>prototype</code> property lazily on first access to keep closure creation cheap.
 * <p>
 * JavaScript 소스로 만든 함수 객체(ECMAScript 함수 객체, ECMA-262 6판 9.2). 파싱된 {@link FunctionInfo}, 닫아 둔 환경, 메서드의 홈 객체를
 * 가진다. 일반 함수와 제너레이터 함수는 클로저 생성을 가볍게 하려고 <code>prototype</code> 프로퍼티를 처음 접근할 때 만든다.
 */
public final class ScriptFunction extends JsFunction {

    /**
     * The function description from the parser.
     * <p>
     * 파서가 만든 함수 설명.
     */
    final FunctionInfo info;

    /**
     * The environment the function closes over.
     * <p>
     * 함수가 닫아 둔 환경.
     */
    final Environment scope;

    /** The defining script or module key, or null. / 이 함수를 정의한 스크립트 또는 모듈 키. 없으면 null. */
    final String scriptSource;

    /**
     * The interpreter that runs the function.
     * <p>
     * 함수를 실행하는 인터프리터.
     */
    final Interpreter interp;

    /**
     * The home object used for super property lookups (methods), or null.
     * <p>
     * super 프로퍼티 조회에 쓰는 홈 객체(메서드). 없으면 null.
     */
    JsObject homeObject;

    /**
     * The private class environment of this constructor or method, or null.
     * <p>
     * 이 생성자나 메서드의 private 클래스 환경. 없으면 null.
     */
    ClassRuntime.Brand classBrand;

    /**
     * True for class constructors (they cannot be called without new).
     * <p>
     * 클래스 생성자이면 true(new 없이 호출할 수 없다).
     */
    boolean classConstructor;

    /**
     * True for the implicit constructor of a class without an explicit constructor.
     * <p>
     * 명시적 생성자가 없는 클래스의 암시적 생성자이면 true.
     */
    boolean defaultConstructor;

    /**
     * True once the prototype property has been created (or is known not to be needed).
     * <p>
     * prototype 프로퍼티를 만들었거나(또는 필요 없음이 확정되었으면) true.
     */
    private boolean protoMaterialized;

    /**
     * Creates a function object.
     * <p>
     * 함수 객체를 만든다.
     *
     * @param interp the interpreter / 인터프리터
     * @param realm  the realm / realm
     * @param proto  the [[Prototype]] (Function.prototype, or the generator function prototype) / [[Prototype]]
     * @param info   the function description / 함수 설명
     * @param scope  the closure environment / 클로저 환경
     * @param name   the function name / 함수 이름
     */
    ScriptFunction(Interpreter interp, JsRealm realm, JsObject proto, FunctionInfo info, Environment scope, String name) {
        super(realm, proto);
        this.interp = interp;
        this.info = info;
        this.scope = scope;
        // 모듈의 최상위 함수 선언은 해당 모듈을 실행하기 전 링크 단계에서도 만들어진다.
        scriptSource = scope.module == null ? RealmExecution.currentScriptSource() : scope.module.key;
        defineNameAndLength(name, info.length);
        protoMaterialized = info.arrow || info.async || (info.method && !info.classConstructor && !info.generator);
    }

    /**
     * Returns true if the function can be called with new.
     * <p>
     * new 로 호출할 수 있으면 true.
     *
     * @return true for constructors / 생성자이면 true
     */
    public boolean isConstructor() {
        if (classConstructor) {
            return true;
        }
        return !info.arrow && !info.method && !info.generator && !info.async;
    }

    /**
     * Calls the function.
     * <p>
     * 함수를 호출한다.
     *
     * @param thisValue the this value / this 값
     * @param args      the arguments / 인수
     * @return the result / 결과
     */
    public JsValue call(JsValue thisValue, JsValue[] args) {
        if (classConstructor) {
            throw JsException.typeError("Class constructor " + nameForMessages() + " cannot be invoked without 'new'");
        }
        return interp.callFunction(this, thisValue, args);
    }

    /**
     * Constructs an object with the function.
     * <p>
     * 함수로 객체를 만든다.
     *
     * @param args      the arguments / 인수
     * @param newTarget the new.target / new.target
     * @return the new object / 새 객체
     */
    public JsObject construct(JsValue[] args, JsObject newTarget) {
        if (!isConstructor()) {
            throw JsException.typeError(nameForMessages() + " is not a constructor");
        }
        return interp.constructFunction(this, args, newTarget == null ? this : newTarget);
    }

    /**
     * Returns the source text of the function for Function.prototype.toString.
     * <p>
     * Function.prototype.toString 용 함수 소스 텍스트를 반환한다.
     *
     * @return the source text / 소스 텍스트
     */
    public String getSourceText() {
        return info.sourceText();
    }

    /**
     * Returns the function name for error messages.
     * <p>
     * 오류 메시지용 함수 이름을 반환한다.
     *
     * @return the name or "anonymous" / 이름 또는 "anonymous"
     */
    String nameForMessages() {
        Property p = super.getOwnPropertySlot("name");
        if (p != null && !p.accessor && p.value instanceof org.duckdns.hjow.swingwebpanel.js.JsString) {
            String n = ((org.duckdns.hjow.swingwebpanel.js.JsString) p.value).getValue();
            return n.length() == 0 ? "anonymous" : n;
        }
        return "anonymous";
    }

    /**
     * Returns the home object of the function (for super lookups).
     * <p>
     * 함수의 홈 객체를 반환한다(super 조회용).
     *
     * @return the home object or null / 홈 객체 또는 null
     */
    public JsObject getHomeObject() {
        return homeObject;
    }

    /**
     * Marks the prototype property as handled (class constructors define their own).
     * <p>
     * prototype 프로퍼티를 처리된 것으로 표시한다(클래스 생성자는 직접 정의한다).
     */
    void prototypeHandled() {
        protoMaterialized = true;
    }

    /**
     * Creates the prototype property of normal and generator functions on first use.
     * <p>
     * 일반 함수와 제너레이터 함수의 prototype 프로퍼티를 처음 쓸 때 만든다.
     */
    private void materializePrototype() {
        protoMaterialized = true;
        if (info.generator) {
            JsObject gp = new JsObject(realm.getIntrinsic("%GeneratorPrototype%"));
            super.defineOwnProperty("prototype", PropertyDescriptor.data(gp, true, false, false));
        } else if (isConstructor()) {
            JsObject p = realm.newObject();
            p.defineBuiltin("constructor", this);
            super.defineOwnProperty("prototype", PropertyDescriptor.data(p, true, false, false));
        }
    }

    /**
     * Exposes the lazily created prototype property.
     * <p>
     * 늦게 만드는 prototype 프로퍼티를 보여 준다.
     *
     * @param key the key / 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        if (!protoMaterialized && "prototype".equals(key)) {
            materializePrototype();
        }
        return super.getOwnPropertySlot(key);
    }

    /**
     * Defines a property, creating the prototype first if needed.
     * <p>
     * 프로퍼티를 정의한다. 필요하면 prototype 을 먼저 만든다.
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        if (!protoMaterialized && "prototype".equals(key)) {
            materializePrototype();
        }
        return super.defineOwnProperty(key, desc);
    }

    /**
     * Deletes a property, creating the prototype first if needed.
     * <p>
     * 프로퍼티를 지운다. 필요하면 prototype 을 먼저 만든다.
     *
     * @param key the key / 키
     * @return true if gone afterwards / 삭제 후 없으면 true
     */
    public boolean delete(Object key) {
        if (!protoMaterialized && "prototype".equals(key)) {
            materializePrototype();
        }
        return super.delete(key);
    }

    /**
     * Lists the own keys, creating the prototype first so that it is included.
     * <p>
     * 자체 키를 나열한다. prototype 이 포함되도록 먼저 만든다.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        if (!protoMaterialized) {
            materializePrototype();
        }
        return super.ownKeys();
    }

    /**
     * Returns "Function".
     * <p>
     * "Function" 을 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Function";
    }

    /**
     * Returns undefined for a missing optional value (helper for callers in this package).
     * <p>
     * 없는 선택 값에 undefined 를 반환한다(이 패키지 호출자용 도우미).
     *
     * @param v the value or null / 값 또는 null
     * @return the value or undefined / 값 또는 undefined
     */
    static JsValue orUndefined(JsValue v) {
        return v == null ? Js.UNDEFINED : v;
    }
}
