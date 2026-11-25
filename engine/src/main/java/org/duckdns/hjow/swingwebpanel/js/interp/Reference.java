package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * The ECMA-262 Reference specification type (6th edition 6.2.3): the result of evaluating an identifier or a property access
 * that has not been read or written yet. A reference has a base (an environment for an identifier that resolves to a binding,
 * a value for a property reference, nothing for an unresolvable reference), a referenced name, and a strict flag. The
 * operations GetValue, PutValue and delete (together with the derivations IsPropertyReference, IsUnresolvableReference,
 * IsSuperReference and GetThisValue) are the only way the interpreter reads, assigns, deletes or derives the this value of a call
 * from an identifier or a member expression.
 * <p>
 * Property keys of element references (<code>base[key]</code>) are converted with ToPropertyKey when the reference is first read
 * or written, that is after the right hand side of an assignment has been evaluated (the behavior of Chromium; ES2015 converts
 * earlier). References are created for one evaluation and are not shared between threads.
 * <p>
 * ECMA-262 Reference 명세 자료형(6판 6.2.3): 아직 읽거나 쓰지 않은 식별자 또는 프로퍼티 접근의 평가 결과. 참조는 기준(바인딩으로 해석된
 * 식별자이면 환경, 프로퍼티 참조이면 값, 해석할 수 없는 참조이면 없음), 참조 이름, 엄격 모드 표시를 가진다. GetValue, PutValue, delete 연산과
 * 파생 연산(IsPropertyReference, IsUnresolvableReference, IsSuperReference, GetThisValue)이 인터프리터가 식별자나 멤버 표현식을
 * 읽고, 대입하고, 삭제하고, 호출의 this 값을 구하는 유일한 방법이다.
 * <p>
 * 요소 참조(<code>base[key]</code>)의 프로퍼티 키는 참조를 처음 읽거나 쓸 때, 즉 대입의 오른쪽을 평가한 뒤 ToPropertyKey 로 변환한다
 * (Chromium 의 동작이며 ES2015 는 더 일찍 변환한다). 참조는 한 번의 평가를 위해 만들며 스레드 사이에서 공유하지 않는다.
 */
final class Reference {

    /**
     * Kind: an identifier that resolves to no binding (the base is undefined).
     * <p>
     * 종류: 어떤 바인딩으로도 해석되지 않는 식별자(기준이 undefined).
     */
    static final int UNRESOLVABLE = 0;

    /**
     * Kind: an identifier that resolves to a binding of an environment.
     * <p>
     * 종류: 환경의 바인딩으로 해석된 식별자.
     */
    static final int ENVIRONMENT = 1;

    /**
     * Kind: a property of a value (<code>base.name</code> or <code>base[key]</code>).
     * <p>
     * 종류: 값의 프로퍼티(<code>base.name</code> 또는 <code>base[key]</code>).
     */
    static final int PROPERTY = 2;

    /**
     * Kind: a super property (<code>super.name</code>), with an explicit this value.
     * <p>
     * 종류: super 프로퍼티(<code>super.name</code>). this 값을 따로 가진다.
     */
    static final int SUPER = 3;
    /**
     * Kind: a private class element reference.
     * <p>
     * 종류: 클래스 private 요소 참조.
     */
    static final int PRIVATE = 4;

    /**
     * The kind of this reference.
     * <p>
     * 이 참조의 종류.
     */
    private final int kind;

    /**
     * The expression helpers used to read and write values.
     * <p>
     * 값을 읽고 쓰는 데 쓰는 표현식 도우미.
     */
    private final Expressions ex;

    /**
     * The environment holding the binding (ENVIRONMENT) or null.
     * <p>
     * 바인딩을 가진 환경(ENVIRONMENT). 아니면 null.
     */
    private final Environment env;

    /**
     * The identifier name or the dotted property name; null for computed keys.
     * <p>
     * 식별자 이름이나 점 표기 프로퍼티 이름. 계산된 키이면 null.
     */
    private final String name;

    /**
     * The base value of a property reference, or the this value of a super reference.
     * <p>
     * 프로퍼티 참조의 기준 값, 또는 super 참조의 this 값.
     */
    private final JsValue base;

    /**
     * The unconverted key value of an element reference (<code>base[keyValue]</code>), or null.
     * <p>
     * 요소 참조(<code>base[keyValue]</code>)의 변환 전 키 값. 아니면 null.
     */
    private final JsValue keyValue;

    /**
     * The object where the lookup of a super reference starts (may be null).
     * <p>
     * super 참조의 조회가 시작되는 객체(null 일 수 있다).
     */
    private final JsObject superBase;

    /**
     * The converted property key of a super reference.
     * <p>
     * super 참조의 변환된 프로퍼티 키.
     */
    private final Object superKey;

    /**
     * The active private class brand, or null.
     * <p>
     * 실행 중 private 클래스 brand. 없으면 null.
     */
    private final ClassRuntime.Brand brand;

    /**
     * True if the reference was created in strict mode code.
     * <p>
     * 엄격 모드 코드에서 만든 참조이면 true.
     */
    private final boolean strict;

    /**
     * Creates a reference (use the static factories).
     * <p>
     * 참조를 만든다(정적 팩터리를 쓴다).
     *
     * @param kind      the kind / 종류
     * @param ex        the expression helpers / 표현식 도우미
     * @param env       the environment or null / 환경 또는 null
     * @param name      the name or null / 이름 또는 null
     * @param base      the base or this value / 기준 값 또는 this 값
     * @param keyValue  the unconverted key or null / 변환 전 키 또는 null
     * @param superBase the super lookup start or null / super 조회 시작 객체 또는 null
     * @param superKey  the super key or null / super 키 또는 null
     * @param strict    the strict flag / 엄격 모드 표시
     */
    private Reference(int kind, Expressions ex, Environment env, String name, JsValue base, JsValue keyValue, JsObject superBase,
            Object superKey, ClassRuntime.Brand brand, boolean strict) {
        this.kind = kind;
        this.ex = ex;
        this.env = env;
        this.name = name;
        this.base = base;
        this.keyValue = keyValue;
        this.superBase = superBase;
        this.superKey = superKey;
        this.brand = brand;
        this.strict = strict;
    }

    /**
     * Creates the reference for an identifier already resolved with {@link Environment#lookup(String)} (ResolveBinding).
     * <p>
     * {@link Environment#lookup(String)} 로 이미 해석한 식별자의 참조를 만든다(ResolveBinding).
     *
     * @param ex     the expression helpers / 표현식 도우미
     * @param env    the environment that has the binding, or null if there is none / 바인딩을 가진 환경. 없으면 null
     * @param name   the identifier / 식별자
     * @param strict true in strict mode code / 엄격 모드 코드이면 true
     * @return the reference / 참조
     */
    static Reference resolved(Expressions ex, Environment env, String name, boolean strict) {
        if (env == null) {
            return new Reference(UNRESOLVABLE, ex, null, name, Js.UNDEFINED, null, null, null, null, strict);
        }
        return new Reference(ENVIRONMENT, ex, env, name, null, null, null, null, null, strict);
    }

    /**
     * Creates the reference for an identifier looked up from a scope (ResolveBinding).
     * <p>
     * 스코프에서 찾은 식별자의 참조를 만든다(ResolveBinding).
     *
     * @param ex     the expression helpers / 표현식 도우미
     * @param scope  the environment to start from / 찾기를 시작할 환경
     * @param name   the identifier / 식별자
     * @param strict true in strict mode code / 엄격 모드 코드이면 true
     * @return the reference / 참조
     */
    static Reference identifier(Expressions ex, Environment scope, String name, boolean strict) {
        return resolved(ex, scope.lookup(name), name, strict);
    }

    /**
     * Creates the reference <code>base.name</code>.
     * <p>
     * <code>base.name</code> 참조를 만든다.
     *
     * @param ex     the expression helpers / 표현식 도우미
     * @param base   the base value / 기준 값
     * @param name   the property name / 프로퍼티 이름
     * @param strict true in strict mode code / 엄격 모드 코드이면 true
     * @return the reference / 참조
     */
    static Reference property(Expressions ex, JsValue base, String name, boolean strict) {
        return new Reference(PROPERTY, ex, null, name, base, null, null, null, null, strict);
    }

    /**
     * Creates the reference <code>base[keyValue]</code>; the key is converted when the reference is used.
     * <p>
     * <code>base[keyValue]</code> 참조를 만든다. 키는 참조를 쓸 때 변환한다.
     *
     * @param ex       the expression helpers / 표현식 도우미
     * @param base     the base value / 기준 값
     * @param keyValue the key value before ToPropertyKey / ToPropertyKey 전의 키 값
     * @param strict   true in strict mode code / 엄격 모드 코드이면 true
     * @return the reference / 참조
     */
    static Reference element(Expressions ex, JsValue base, JsValue keyValue, boolean strict) {
        return new Reference(PROPERTY, ex, null, null, base, keyValue, null, null, null, strict);
    }

    /**
     * Creates the reference <code>super.key</code>.
     * <p>
     * <code>super.key</code> 참조를 만든다.
     *
     * @param ex        the expression helpers / 표현식 도우미
     * @param thisValue the this value of the running function / 실행 중인 함수의 this 값
     * @param superBase the object where the lookup starts, the prototype of the home object (may be null) / 조회가 시작되는 객체, 홈 객체의 프로토타입(null 가능)
     * @param key       the converted property key / 변환된 프로퍼티 키
     * @param strict    true in strict mode code / 엄격 모드 코드이면 true
     * @return the reference / 참조
     */
    static Reference superProperty(Expressions ex, JsValue thisValue, JsObject superBase, Object key, boolean strict) {
        return new Reference(SUPER, ex, null, null, thisValue, null, superBase, key, null, strict);
    }

    /**
     * Creates a private element reference.
     * <p>
     * private 요소 참조를 만든다.
     *
     * @param ex expression helpers / 표현식 도우미
     * @param base receiver / 수신자
     * @param name private name including hash / 해시 포함 private 이름
     * @param brand active class brand / 실행 중 클래스 brand
     * @param strict strict mode / 엄격 모드
     * @return reference / 참조
     */
    static Reference privateProperty(Expressions ex, JsValue base, String name, ClassRuntime.Brand brand, boolean strict) {
        return new Reference(PRIVATE, ex, null, name, base, null, null, null, brand, strict);
    }

    // ------------------------------------------------------------------ derived operations (ES2015 6.2.3.1)

    /**
     * IsPropertyReference: true for property and super references.
     * <p>
     * IsPropertyReference: 프로퍼티 참조와 super 참조이면 true.
     *
     * @return the result / 결과
     */
    boolean isPropertyReference() {
        return kind == PROPERTY || kind == SUPER || kind == PRIVATE;
    }

    /**
     * IsUnresolvableReference: true if the identifier resolved to no binding.
     * <p>
     * IsUnresolvableReference: 식별자가 어떤 바인딩으로도 해석되지 않았으면 true.
     *
     * @return the result / 결과
     */
    boolean isUnresolvableReference() {
        return kind == UNRESOLVABLE;
    }

    /**
     * IsSuperReference: true for <code>super.x</code>.
     * <p>
     * IsSuperReference: <code>super.x</code> 이면 true.
     *
     * @return the result / 결과
     */
    boolean isSuperReference() {
        return kind == SUPER;
    }

    /**
     * IsStrictReference: true if the reference comes from strict mode code.
     * <p>
     * IsStrictReference: 엄격 모드 코드에서 온 참조이면 true.
     *
     * @return the result / 결과
     */
    boolean isStrictReference() {
        return strict;
    }

    /**
     * Returns the kind of this reference ({@link #UNRESOLVABLE}, {@link #ENVIRONMENT}, {@link #PROPERTY} or {@link #SUPER}).
     * <p>
     * 이 참조의 종류({@link #UNRESOLVABLE}, {@link #ENVIRONMENT}, {@link #PROPERTY}, {@link #SUPER} 중 하나)를 반환한다.
     *
     * @return the kind / 종류
     */
    int getKind() {
        return kind;
    }

    /**
     * Returns the base value of a property reference (the this value for a super reference), or undefined for identifiers.
     * <p>
     * 프로퍼티 참조의 기준 값(super 참조는 this 값)을 반환한다. 식별자 참조이면 undefined.
     *
     * @return the base value / 기준 값
     */
    JsValue getBaseValue() {
        return kind == PROPERTY || kind == SUPER || kind == PRIVATE ? base : Js.UNDEFINED;
    }

    /**
     * Returns the environment that holds the binding of an identifier reference, or null.
     * <p>
     * 식별자 참조의 바인딩을 가진 환경을 반환한다. 없으면 null.
     *
     * @return the environment / 환경
     */
    Environment getEnvironment() {
        return env;
    }

    /**
     * GetReferencedName: the identifier name, or the property key (a String or a symbol; for element references the key value is
     * converted now).
     * <p>
     * GetReferencedName: 식별자 이름 또는 프로퍼티 키(String 이나 심볼. 요소 참조는 지금 키 값을 변환한다).
     *
     * @return the name or key / 이름 또는 키
     */
    Object getReferencedName() {
        if (kind == SUPER) {
            return superKey;
        }
        if (keyValue != null) {
            return Ops.toPropertyKey(keyValue);
        }
        return name;
    }

    /**
     * GetThisValue (ES2015 6.2.3.3): the base value for property references, the explicit this value for super references.
     * <p>
     * GetThisValue(ES2015 6.2.3.3): 프로퍼티 참조는 기준 값, super 참조는 따로 저장한 this 값.
     *
     * @return the this value / this 값
     */
    JsValue getThisValue() {
        return base;
    }

    /**
     * Derives the this value of a call whose callee is evaluated to this reference (ES2015 12.3.4.1 steps 6 to 7): the base for a
     * property reference, the binding object for an identifier found in a <code>with</code> object environment, otherwise
     * undefined.
     * <p>
     * 이 참조로 평가되는 callee 를 가진 호출의 this 값을 구한다(ES2015 12.3.4.1 6~7단계): 프로퍼티 참조는 기준 값, <code>with</code> 객체
     * 환경에서 찾은 식별자는 그 객체, 그 밖에는 undefined.
     *
     * @return the this value for the call / 호출의 this 값
     */
    JsValue thisValueForCall() {
        if (kind == PROPERTY || kind == SUPER || kind == PRIVATE) {
            return base;
        }
        if (kind == ENVIRONMENT && env.withEnv) {
            return env.objectRecord;
        }
        return Js.UNDEFINED;
    }

    // ------------------------------------------------------------------ GetValue, PutValue, delete

    /**
     * GetValue (ES2015 6.2.3.1): reads the value the reference points to.
     * <p>
     * GetValue(ES2015 6.2.3.1): 참조가 가리키는 값을 읽는다.
     *
     * @return the value / 값
     * @throws JsException a ReferenceError for unresolvable references and bindings in their temporal dead zone, a TypeError for
     *                     properties of undefined and null / 해석되지 않는 참조와 일시적 사각지대의 바인딩이면 ReferenceError, undefined 와 null 의 프로퍼티이면 TypeError
     */
    JsValue getValue() {
        switch (kind) {
        case UNRESOLVABLE:
            throw JsException.referenceError(name + " is not defined");
        case ENVIRONMENT:
            return env.get(name, strict);
        case SUPER:
            return superBase == null ? Js.UNDEFINED : superBase.get(superKey, base);
        case PRIVATE:
            return ClassRuntime.get(brand, name, base, base);
        default:
            return keyValue != null ? ex.getElement(base, keyValue) : ex.getPropertyChecked(base, name);
        }
    }

    /**
     * PutValue (ES2015 6.2.3.2): assigns the value the reference points to. Unresolvable references create a global property in
     * sloppy mode and throw a ReferenceError in strict mode; failed property writes throw a TypeError in strict mode.
     * <p>
     * PutValue(ES2015 6.2.3.2): 참조가 가리키는 곳에 값을 대입한다. 해석되지 않는 참조는 느슨한 모드에서 전역 프로퍼티를 만들고 엄격
     * 모드에서는 ReferenceError 를 던지며, 프로퍼티 쓰기가 실패하면 엄격 모드에서 TypeError 를 던진다.
     *
     * @param value the value / 값
     */
    void putValue(JsValue value) {
        switch (kind) {
        case UNRESOLVABLE:
            if (strict) {
                throw JsException.referenceError(name + " is not defined");
            }
            ex.realm.getGlobalObject().set(name, value);
            return;
        case ENVIRONMENT:
            env.set(name, value, strict);
            return;
        case SUPER:
            if (superBase != null && !superBase.set(superKey, value, base) && strict) {
                throw JsException.typeError("Cannot assign to read only property '" + PropertyKeys.describe(superKey) + "' of object");
            }
            return;
        case PRIVATE:
            ClassRuntime.put(brand, name, base, base, value);
            return;
        default:
            if (keyValue != null) {
                ex.putElement(base, keyValue, value, strict);
                return;
            }
            if (base.isNullOrUndefined()) {
                throw JsException.typeError("Cannot set property '" + name + "' of " + base);
            }
            ex.putV(base, name, value, strict);
        }
    }

    /**
     * The delete operator on this reference (ES2015 12.5.3.2): unresolvable references give true, property references delete the
     * property (a failure is a TypeError in strict mode), identifier references delete deletable bindings (those created by eval
     * code) and properties of <code>with</code> objects, and super references are a ReferenceError.
     * <p>
     * 이 참조에 대한 delete 연산자(ES2015 12.5.3.2): 해석되지 않는 참조는 true, 프로퍼티 참조는 프로퍼티를 삭제하고(엄격 모드에서 실패하면
     * TypeError), 식별자 참조는 삭제 가능한 바인딩(eval 코드가 만든 것)과 <code>with</code> 객체의 프로퍼티를 삭제하며, super 참조는
     * ReferenceError 이다.
     *
     * @return true if the reference is gone / 참조가 없어졌으면 true
     */
    boolean delete() {
        switch (kind) {
        case UNRESOLVABLE:
            return true;
        case SUPER:
            throw JsException.referenceError("Unsupported reference to 'super'");
        case ENVIRONMENT: {
            if (env.objectRecord != null && env.own(name) == null) {
                return env.objectRecord.delete(name);
            }
            Binding b = env.own(name);
            if (b != null && b.deletable) {
                env.bindings.remove(name);
                return true;
            }
            return false;
        }
        case PRIVATE:
            throw JsException.syntaxError("Private fields cannot be deleted");
        default: {
            if (base.isNullOrUndefined()) {
                throw JsException.typeError("Cannot convert undefined or null to object");
            }
            Object key = keyValue != null ? Ops.toPropertyKey(keyValue) : name;
            JsObject o = Ops.toObject(ex.realm, base);
            boolean ok = o.delete(key);
            if (!ok && strict) {
                throw JsException.typeError("Cannot delete property '" + PropertyKeys.describe(key) + "'");
            }
            return ok;
        }
        }
    }
}
