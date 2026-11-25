package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * A Proxy exotic object (ECMA-262 6th edition 9.5). Every internal method looks up the matching trap on the handler; without a
 * trap it forwards to the target. When a trap answers, the result is checked against the invariants that the spec derives from the
 * state of the target (non-configurable properties, non-extensible targets, frozen data properties, key lists), and a violated
 * invariant is a TypeError. After {@link #revoke()} every operation fails with a TypeError.
 * <p>
 * Assumptions where the 6th edition text could not be read: key lists returned by <code>ownKeys</code> must not contain duplicates
 * (the rule of later editions, stricter than ES2015); [[GetOwnProperty]] checks only the configurable invariants of ES2015 (the
 * later non-writable check is not applied); [[Delete]] does not check the extensibility of the target (later editions do).
 * <p>
 * Proxy 이국적 객체(ECMA-262 6판 9.5). 모든 내부 메서드는 핸들러에서 같은 이름의 trap 을 찾고, trap 이 없으면 대상에 전달한다.
 * trap 이 답하면 대상의 상태에서 규격이 유도한 불변 조건(설정 불가 프로퍼티, 확장 불가 대상, 얼려진 데이터 프로퍼티, 키 목록)에 맞는지
 * 검사하며 어기면 TypeError 이다. {@link #revoke()} 뒤에는 모든 연산이 TypeError 로 실패한다.
 * <p>
 * 6판 본문을 읽지 못해 둔 가정: <code>ownKeys</code> 가 돌려준 키 목록에는 중복이 없어야 한다(ES2015 보다 엄격한 후속 판의 규칙).
 * [[GetOwnProperty]] 는 ES2015 의 설정 가능 불변 조건만 검사한다(후속 판의 writable 검사는 적용하지 않음).
 * [[Delete]] 는 대상의 확장 가능 여부를 검사하지 않는다(후속 판은 검사함).
 */
public final class ProxyObject extends JsObject {

    /**
     * Realm used to create descriptor objects and argument arrays for the traps.
     * <p>
     * trap 에 넘길 기술자 객체와 인수 배열을 만드는 데 쓰는 realm.
     */
    private final JsRealm realm;

    /**
     * The target, or null after the proxy was revoked.
     * <p>
     * 대상. 프록시가 해제되면 null.
     */
    private JsObject target;

    /**
     * The handler, or null after the proxy was revoked.
     * <p>
     * 핸들러. 프록시가 해제되면 null.
     */
    private JsObject handler;

    /**
     * True if the target is callable (the proxy then has [[Call]]).
     * <p>
     * 대상이 호출 가능하면 true(프록시도 [[Call]] 을 가진다).
     */
    private final boolean callable;

    /**
     * True if the target is a constructor (the proxy then has [[Construct]]).
     * <p>
     * 대상이 생성자이면 true(프록시도 [[Construct]] 를 가진다).
     */
    private final boolean constructor;

    /**
     * Creates a proxy (ProxyCreate, 9.5.15). The caller has verified that both arguments are objects.
     * <p>
     * 프록시를 만든다(ProxyCreate, 9.5.15). 호출자가 두 인수가 객체임을 확인했다고 가정한다.
     *
     * @param realm   the realm / realm
     * @param target  the target object, not null / 대상 객체(null 불가)
     * @param handler the handler object, not null / 핸들러 객체(null 불가)
     */
    public ProxyObject(JsRealm realm, JsObject target, JsObject handler) {
        super(null);
        this.realm = realm;
        this.target = target;
        this.handler = handler;
        this.callable = target.isCallable();
        this.constructor = target.isConstructor();
    }

    /**
     * Returns the target, or null if revoked.
     * <p>
     * 대상을 반환한다. 해제되었으면 null.
     *
     * @return target or null / 대상 또는 null
     */
    public JsObject getProxyTarget() {
        return target;
    }

    /**
     * Returns the handler, or null if revoked.
     * <p>
     * 핸들러를 반환한다. 해제되었으면 null.
     *
     * @return handler or null / 핸들러 또는 null
     */
    public JsObject getProxyHandler() {
        return handler;
    }

    /**
     * Tells whether the proxy was revoked.
     * <p>
     * 프록시가 해제되었는지 알려 준다.
     *
     * @return true if revoked / 해제되었으면 true
     */
    public boolean isRevoked() {
        return handler == null;
    }

    /**
     * Revokes the proxy: target and handler are dropped and every later operation throws a TypeError.
     * <p>
     * 프록시를 해제한다: 대상과 핸들러를 버리며 이후 모든 연산은 TypeError 를 던진다.
     */
    public void revoke() {
        target = null;
        handler = null;
    }

    /**
     * Returns the handler or throws the TypeError of a revoked proxy.
     * <p>
     * 핸들러를 반환하거나 해제된 프록시의 TypeError 를 던진다.
     *
     * @param operation name of the operation for the message / 메시지에 쓸 연산 이름
     * @return the handler / 핸들러
     */
    private JsObject liveHandler(String operation) {
        JsObject h = handler;
        if (h == null) {
            throw JsException.typeError("Cannot perform '" + operation + "' on a proxy that has been revoked");
        }
        return h;
    }

    /**
     * Looks a trap up on the handler (GetMethod): undefined if absent, a callable otherwise, a TypeError for other values.
     * <p>
     * 핸들러에서 trap 을 찾는다(GetMethod): 없으면 undefined, 있으면 호출 가능 값, 그 밖의 값은 TypeError.
     *
     * @param h    the handler / 핸들러
     * @param name trap name / trap 이름
     * @return the trap or undefined / trap 또는 undefined
     */
    private static JsValue trap(JsObject h, String name) {
        return Ops.getMethod(h, name);
    }

    /**
     * Converts a property key to the value passed to traps.
     * <p>
     * 프로퍼티 키를 trap 에 넘기는 값으로 바꾼다.
     *
     * @param key String or JsSymbol key / String 또는 JsSymbol 키
     * @return the value / 값
     */
    private static JsValue keyValue(Object key) {
        return PropertyKeys.toValue(key);
    }

    /**
     * Returns "Function" for callable proxies, "Array" for proxies of arrays and "Object" otherwise. It never throws; the TypeError
     * of a revoked proxy comes from {@link #isArray()}.
     * <p>
     * 호출 가능한 프록시는 "Function", 배열의 프록시는 "Array", 그 밖에는 "Object" 를 반환한다. 예외를 던지지 않으며 해제된 프록시의
     * TypeError 는 {@link #isArray()} 에서 나온다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        if (callable) {
            return "Function";
        }
        JsObject t = target;
        return t != null && t.isArray() ? "Array" : "Object";
    }

    /**
     * IsArray for proxies (7.2.2): forwards to the target; a revoked proxy is a TypeError.
     * <p>
     * 프록시의 IsArray(7.2.2): 대상에 전달하며 해제된 프록시는 TypeError 이다.
     *
     * @return true if the target is an array / 대상이 배열이면 true
     */
    public boolean isArray() {
        JsObject t = target;
        if (handler == null) {
            throw JsException.typeError("Cannot perform 'IsArray' on a proxy that has been revoked");
        }
        return t.isArray();
    }

    /**
     * Returns true if the target is callable.
     * <p>
     * 대상이 호출 가능하면 true.
     *
     * @return true if callable / 호출 가능하면 true
     */
    public boolean isCallable() {
        return callable;
    }

    /**
     * Returns true if the target is a constructor.
     * <p>
     * 대상이 생성자이면 true.
     *
     * @return true if a constructor / 생성자이면 true
     */
    public boolean isConstructor() {
        return constructor;
    }

    /**
     * Returns a description that never calls a trap.
     * <p>
     * trap 을 호출하지 않는 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        return "[object " + className() + "]";
    }

    // ------------------------------------------------------------------ 9.5.1 [[GetPrototypeOf]]

    /**
     * [[GetPrototypeOf]] (9.5.1): the trap result must be an object or null, and equal to the target's prototype if the
     * target is not extensible.
     * <p>
     * [[GetPrototypeOf]] (9.5.1): trap 결과는 객체 또는 null 이어야 하며 대상이 확장 불가이면 대상의 프로토타입과 같아야 한다.
     *
     * @return the prototype or null / 프로토타입 또는 null
     */
    public JsObject getPrototypeOf() {
        JsObject h = liveHandler("getPrototypeOf");
        JsObject t = target;
        JsValue trap = trap(h, "getPrototypeOf");
        if (trap.isUndefined()) {
            return t.getPrototypeOf();
        }
        JsValue r = Ops.call(trap, h, new JsValue[] { t });
        if (!(r instanceof JsObject) && !r.isNull()) {
            throw JsException.typeError("'getPrototypeOf' on proxy: trap returned neither object nor null");
        }
        JsObject proto = r instanceof JsObject ? (JsObject) r : null;
        if (t.isExtensible()) {
            return proto;
        }
        if (proto != t.getPrototypeOf()) {
            throw JsException.typeError("'getPrototypeOf' on proxy: proxy target is non-extensible but the trap did not return its actual prototype");
        }
        return proto;
    }

    /**
     * [[SetPrototypeOf]] (9.5.2): a true result for a non-extensible target requires the same prototype as the target has.
     * <p>
     * [[SetPrototypeOf]] (9.5.2): 확장 불가 대상에서 true 결과는 대상이 가진 프로토타입과 같아야 한다.
     *
     * @param proto new prototype or null / 새 프로토타입 또는 null
     * @return true on success / 성공하면 true
     */
    public boolean setPrototypeOf(JsObject proto) {
        JsObject h = liveHandler("setPrototypeOf");
        JsObject t = target;
        JsValue trap = trap(h, "setPrototypeOf");
        if (trap.isUndefined()) {
            return t.setPrototypeOf(proto);
        }
        JsValue r = Ops.call(trap, h, new JsValue[] { t, proto == null ? Js.NULL : proto });
        if (!Ops.toBoolean(r)) {
            return false;
        }
        if (t.isExtensible()) {
            return true;
        }
        if (proto != t.getPrototypeOf()) {
            throw JsException.typeError("'setPrototypeOf' on proxy: trap returned truish for setting a new prototype on the non-extensible proxy target");
        }
        return true;
    }

    /**
     * [[IsExtensible]] (9.5.3): the trap result must equal the target's extensibility.
     * <p>
     * [[IsExtensible]] (9.5.3): trap 결과는 대상의 확장 가능 여부와 같아야 한다.
     *
     * @return true if extensible / 확장 가능하면 true
     */
    public boolean isExtensible() {
        JsObject h = liveHandler("isExtensible");
        JsObject t = target;
        JsValue trap = trap(h, "isExtensible");
        if (trap.isUndefined()) {
            return t.isExtensible();
        }
        boolean result = Ops.toBoolean(Ops.call(trap, h, new JsValue[] { t }));
        if (result != t.isExtensible()) {
            throw JsException.typeError("'isExtensible' on proxy: trap result does not reflect extensibility of proxy target");
        }
        return result;
    }

    /**
     * [[PreventExtensions]] (9.5.4): a true result requires that the target is no longer extensible.
     * <p>
     * [[PreventExtensions]] (9.5.4): true 결과는 대상이 더 이상 확장 가능하지 않음을 요구한다.
     *
     * @return true on success / 성공하면 true
     */
    public boolean preventExtensions() {
        JsObject h = liveHandler("preventExtensions");
        JsObject t = target;
        JsValue trap = trap(h, "preventExtensions");
        if (trap.isUndefined()) {
            return t.preventExtensions();
        }
        boolean result = Ops.toBoolean(Ops.call(trap, h, new JsValue[] { t }));
        if (result && t.isExtensible()) {
            throw JsException.typeError("'preventExtensions' on proxy: trap returned truish but the proxy target is extensible");
        }
        return result;
    }

    // ------------------------------------------------------------------ 9.5.5 [[GetOwnProperty]] and related

    /**
     * [[GetOwnProperty]] (9.5.5): the trap result must be an object or undefined, and must be compatible with the target's
     * property (a non-configurable result requires a non-configurable target property).
     * <p>
     * [[GetOwnProperty]] (9.5.5): trap 결과는 객체 또는 undefined 여야 하고 대상의 프로퍼티와 호환되어야 한다(설정 불가 결과는 대상의
     * 설정 불가 프로퍼티를 요구한다).
     *
     * @param key the key / 키
     * @return a complete descriptor or null / 완전 기술자 또는 null
     */
    public PropertyDescriptor getOwnProperty(Object key) {
        JsObject h = liveHandler("getOwnPropertyDescriptor");
        JsObject t = target;
        JsValue trap = trap(h, "getOwnPropertyDescriptor");
        if (trap.isUndefined()) {
            return t.getOwnProperty(key);
        }
        JsValue r = Ops.call(trap, h, new JsValue[] { t, keyValue(key) });
        if (!(r instanceof JsObject) && !r.isUndefined()) {
            throw JsException.typeError("'getOwnPropertyDescriptor' on proxy: trap returned neither object nor undefined for property '"
                    + PropertyKeys.describe(key) + "'");
        }
        PropertyDescriptor targetDesc = t.getOwnProperty(key);
        if (r.isUndefined()) {
            if (targetDesc == null) {
                return null;
            }
            if (!targetDesc.isConfigurableTrue()) {
                throw JsException.typeError("'getOwnPropertyDescriptor' on proxy: trap returned undefined for property '"
                        + PropertyKeys.describe(key) + "' which is non-configurable in the proxy target");
            }
            if (!t.isExtensible()) {
                throw JsException.typeError("'getOwnPropertyDescriptor' on proxy: trap returned undefined for property '"
                        + PropertyKeys.describe(key) + "' which exists in the non-extensible proxy target");
            }
            return null;
        }
        boolean extensibleTarget = t.isExtensible();
        PropertyDescriptor result = Descriptors.complete(Descriptors.toDescriptor(r));
        if (!Descriptors.isCompatible(extensibleTarget, result, targetDesc)) {
            throw JsException.typeError("'getOwnPropertyDescriptor' on proxy: trap returned descriptor for property '"
                    + PropertyKeys.describe(key) + "' that is incompatible with the existing property in the proxy target");
        }
        if (!result.isConfigurableTrue() && (targetDesc == null || targetDesc.isConfigurableTrue())) {
            throw JsException.typeError("'getOwnPropertyDescriptor' on proxy: trap reported non-configurability for property '"
                    + PropertyKeys.describe(key) + "' which is either non-existent or configurable in the proxy target");
        }
        return result;
    }

    /**
     * Returns a snapshot of the property built from the result of {@link #getOwnProperty(Object)}, or null (revoked proxies give
     * null here so that error message formatting never throws).
     * <p>
     * {@link #getOwnProperty(Object)} 결과로 만든 프로퍼티 사본을 반환한다. 없으면 null(오류 메시지를 만드는 중 예외가 나지 않도록
     * 해제된 프록시는 여기서 null 을 준다).
     *
     * @param key the key / 키
     * @return snapshot or null / 사본 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        if (handler == null) {
            return null;
        }
        PropertyDescriptor d = getOwnProperty(key);
        if (d == null) {
            return null;
        }
        Property p = new Property();
        if (d.isAccessor()) {
            p.accessor = true;
            p.value = null;
            p.getter = d.get instanceof JsObject ? (JsObject) d.get : null;
            p.setter = d.set instanceof JsObject ? (JsObject) d.set : null;
        } else {
            p.value = d.value == null ? Js.UNDEFINED : d.value;
            p.writable = d.isWritableTrue();
        }
        p.enumerable = d.isEnumerableTrue();
        p.configurable = d.isConfigurableTrue();
        return p;
    }

    /**
     * [[DefineOwnProperty]] (9.5.6): a true result must be consistent with the target (a property cannot be reported as defined
     * if the target is non-extensible and lacks it, nor as non-configurable if the target's property is configurable).
     * <p>
     * [[DefineOwnProperty]] (9.5.6): true 결과는 대상과 일관되어야 한다(확장 불가이고 프로퍼티가 없는 대상에서, 또는 대상 프로퍼티가
     * 설정 가능한데 설정 불가로 정의했다고 보고할 수 없다).
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        JsObject h = liveHandler("defineProperty");
        JsObject t = target;
        JsValue trap = trap(h, "defineProperty");
        if (trap.isUndefined()) {
            return t.defineOwnProperty(key, desc);
        }
        JsValue descObj = Descriptors.fromDescriptor(realm, desc);
        boolean result = Ops.toBoolean(Ops.call(trap, h, new JsValue[] { t, keyValue(key), descObj }));
        if (!result) {
            return false;
        }
        PropertyDescriptor targetDesc = t.getOwnProperty(key);
        boolean extensibleTarget = t.isExtensible();
        boolean settingConfigFalse = desc.configurable != null && !desc.configurable.booleanValue();
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw JsException.typeError("'defineProperty' on proxy: trap returned truish for adding property '"
                        + PropertyKeys.describe(key) + "' to the non-extensible proxy target");
            }
            if (settingConfigFalse) {
                throw JsException.typeError("'defineProperty' on proxy: trap returned truish for defining non-configurable property '"
                        + PropertyKeys.describe(key) + "' which is non-existent in the proxy target");
            }
        } else {
            if (!Descriptors.isCompatible(extensibleTarget, desc, targetDesc)) {
                throw JsException.typeError("'defineProperty' on proxy: trap returned truish for adding property '"
                        + PropertyKeys.describe(key) + "' that is incompatible with the existing property in the proxy target");
            }
            if (settingConfigFalse && targetDesc.isConfigurableTrue()) {
                throw JsException.typeError("'defineProperty' on proxy: trap returned truish for defining non-configurable property '"
                        + PropertyKeys.describe(key) + "' which is configurable in the proxy target");
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ 9.5.7 .. 9.5.10

    /**
     * [[HasProperty]] (9.5.7): a false result is not allowed for non-configurable target properties, nor for any existing
     * property of a non-extensible target.
     * <p>
     * [[HasProperty]] (9.5.7): 대상의 설정 불가 프로퍼티나 확장 불가 대상의 기존 프로퍼티에 대해서는 false 결과가 허용되지 않는다.
     *
     * @param key the key / 키
     * @return true if the property exists / 프로퍼티가 있으면 true
     */
    public boolean hasProperty(Object key) {
        JsObject h = liveHandler("has");
        JsObject t = target;
        JsValue trap = trap(h, "has");
        if (trap.isUndefined()) {
            return t.hasProperty(key);
        }
        boolean result = Ops.toBoolean(Ops.call(trap, h, new JsValue[] { t, keyValue(key) }));
        if (!result) {
            PropertyDescriptor targetDesc = t.getOwnProperty(key);
            if (targetDesc != null) {
                if (!targetDesc.isConfigurableTrue()) {
                    throw JsException.typeError("'has' on proxy: trap returned falsish for property '" + PropertyKeys.describe(key)
                            + "' which exists in the proxy target as non-configurable");
                }
                if (!t.isExtensible()) {
                    throw JsException.typeError("'has' on proxy: trap returned falsish for property '" + PropertyKeys.describe(key)
                            + "' but the proxy target is not extensible");
                }
            }
        }
        return result;
    }

    /**
     * [[Get]] (9.5.8): the result must equal the value of a non-configurable, non-writable data property of the target and must be
     * undefined for a non-configurable accessor property without getter.
     * <p>
     * [[Get]] (9.5.8): 결과는 대상의 설정 불가이고 writable 이 아닌 데이터 프로퍼티의 값과 같아야 하며, getter 가 없는 설정 불가 접근자
     * 프로퍼티에서는 undefined 여야 한다.
     *
     * @param key      the key / 키
     * @param receiver the receiver / receiver
     * @return the value / 값
     */
    public JsValue get(Object key, JsValue receiver) {
        JsObject h = liveHandler("get");
        JsObject t = target;
        JsValue trap = trap(h, "get");
        if (trap.isUndefined()) {
            return t.get(key, receiver);
        }
        JsValue result = Ops.call(trap, h, new JsValue[] { t, keyValue(key), receiver });
        PropertyDescriptor targetDesc = t.getOwnProperty(key);
        if (targetDesc != null && !targetDesc.isConfigurableTrue()) {
            if (!targetDesc.isAccessor() && !targetDesc.isWritableTrue()) {
                if (!Ops.sameValue(result, targetDesc.value)) {
                    throw JsException.typeError("'get' on proxy: property '" + PropertyKeys.describe(key)
                            + "' is a read-only and non-configurable data property on the proxy target but the proxy did not return its actual value");
                }
            } else if (targetDesc.isAccessor() && targetDesc.get.isUndefined() && !result.isUndefined()) {
                throw JsException.typeError("'get' on proxy: property '" + PropertyKeys.describe(key)
                        + "' is a non-configurable accessor property on the proxy target and does not have a getter function, but the trap did not return 'undefined'");
            }
        }
        return result;
    }

    /**
     * [[Set]] (9.5.9): a true result must not contradict a non-configurable, non-writable data property of the target
     * (different value) or a non-configurable accessor property without setter.
     * <p>
     * [[Set]] (9.5.9): true 결과는 대상의 설정 불가이고 writable 이 아닌 데이터 프로퍼티(다른 값)나 setter 가 없는 설정 불가 접근자
     * 프로퍼티와 모순되면 안 된다.
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / receiver
     * @return true on success / 성공하면 true
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        JsObject h = liveHandler("set");
        JsObject t = target;
        JsValue trap = trap(h, "set");
        if (trap.isUndefined()) {
            return t.set(key, value, receiver);
        }
        boolean result = Ops.toBoolean(Ops.call(trap, h, new JsValue[] { t, keyValue(key), value, receiver }));
        if (!result) {
            return false;
        }
        PropertyDescriptor targetDesc = t.getOwnProperty(key);
        if (targetDesc != null && !targetDesc.isConfigurableTrue()) {
            if (!targetDesc.isAccessor() && !targetDesc.isWritableTrue()) {
                if (!Ops.sameValue(value, targetDesc.value)) {
                    throw JsException.typeError("'set' on proxy: trap returned truish for property '" + PropertyKeys.describe(key)
                            + "' which exists in the proxy target as a non-configurable and non-writable data property with a different value");
                }
            } else if (targetDesc.isAccessor() && targetDesc.set.isUndefined()) {
                throw JsException.typeError("'set' on proxy: trap returned truish for property '" + PropertyKeys.describe(key)
                        + "' which exists in the proxy target as a non-configurable and non-writable accessor property without a setter");
            }
        }
        return true;
    }

    /**
     * [[Delete]] (9.5.10): a true result is not allowed for non-configurable target properties.
     * <p>
     * [[Delete]] (9.5.10): 대상의 설정 불가 프로퍼티에 대해서는 true 결과가 허용되지 않는다.
     *
     * @param key the key / 키
     * @return true if the property is gone / 프로퍼티가 없어졌으면 true
     */
    public boolean delete(Object key) {
        JsObject h = liveHandler("deleteProperty");
        JsObject t = target;
        JsValue trap = trap(h, "deleteProperty");
        if (trap.isUndefined()) {
            return t.delete(key);
        }
        boolean result = Ops.toBoolean(Ops.call(trap, h, new JsValue[] { t, keyValue(key) }));
        if (!result) {
            return false;
        }
        PropertyDescriptor targetDesc = t.getOwnProperty(key);
        if (targetDesc == null) {
            return true;
        }
        if (!targetDesc.isConfigurableTrue()) {
            throw JsException.typeError("'deleteProperty' on proxy: trap returned truish for property '" + PropertyKeys.describe(key)
                    + "' which is non-configurable in the proxy target");
        }
        return true;
    }

    /**
     * [[OwnPropertyKeys]] (9.5.12): the trap must return an array-like of strings and symbols without duplicates that contains
     * every non-configurable key of the target, and for a non-extensible target exactly the keys of the target.
     * <p>
     * [[OwnPropertyKeys]] (9.5.12): trap 은 중복 없는 문자열과 심볼의 배열 유사 객체를 돌려주어야 하며 대상의 설정 불가 키를 모두
     * 포함해야 하고, 확장 불가 대상에서는 정확히 대상의 키여야 한다.
     *
     * @return a new list of keys / 키의 새 목록
     */
    public List<Object> ownKeys() {
        JsObject h = liveHandler("ownKeys");
        JsObject t = target;
        JsValue trap = trap(h, "ownKeys");
        if (trap.isUndefined()) {
            return t.ownKeys();
        }
        JsValue arrayLike = Ops.call(trap, h, new JsValue[] { t });
        JsValue[] elements = Ops.createListFromArrayLike(arrayLike);
        List<Object> trapResult = new ArrayList<Object>(elements.length);
        Set<Object> seen = new HashSet<Object>();
        // 결과의 각 요소는 문자열 또는 심볼이어야 하고 중복이 없어야 한다.
        for (int i = 0; i < elements.length; i++) {
            Object k;
            if (elements[i] instanceof JsString) {
                k = ((JsString) elements[i]).getValue();
            } else if (elements[i] instanceof JsSymbol) {
                k = elements[i];
            } else {
                throw JsException.typeError("'ownKeys' on proxy: trap result must contain only strings and symbols");
            }
            if (!seen.add(k)) {
                throw JsException.typeError("'ownKeys' on proxy: trap returned duplicate entries");
            }
            trapResult.add(k);
        }
        boolean extensibleTarget = t.isExtensible();
        List<Object> targetKeys = t.ownKeys();
        List<Object> nonConfigurable = new ArrayList<Object>();
        List<Object> configurable = new ArrayList<Object>();
        // 대상의 키를 설정 불가와 설정 가능으로 나눈다.
        for (int i = 0; i < targetKeys.size(); i++) {
            PropertyDescriptor d = t.getOwnProperty(targetKeys.get(i));
            if (d != null && !d.isConfigurableTrue()) {
                nonConfigurable.add(targetKeys.get(i));
            } else {
                configurable.add(targetKeys.get(i));
            }
        }
        if (extensibleTarget && nonConfigurable.isEmpty()) {
            return trapResult;
        }
        Set<Object> unchecked = new HashSet<Object>(trapResult);
        // 대상의 설정 불가 키는 결과에 모두 있어야 한다.
        for (int i = 0; i < nonConfigurable.size(); i++) {
            if (!unchecked.remove(nonConfigurable.get(i))) {
                throw JsException.typeError("'ownKeys' on proxy: trap result did not include '" + PropertyKeys.describe(nonConfigurable.get(i)) + "'");
            }
        }
        if (extensibleTarget) {
            return trapResult;
        }
        // 확장 불가 대상은 설정 가능한 키도 모두 있어야 하고 그 밖의 키는 없어야 한다.
        for (int i = 0; i < configurable.size(); i++) {
            if (!unchecked.remove(configurable.get(i))) {
                throw JsException.typeError("'ownKeys' on proxy: trap result did not include '" + PropertyKeys.describe(configurable.get(i))
                        + "' of the non-extensible proxy target");
            }
        }
        if (!unchecked.isEmpty()) {
            throw JsException.typeError("'ownKeys' on proxy: trap returned extra keys but proxy target is non-extensible");
        }
        return trapResult;
    }

    // ------------------------------------------------------------------ 9.5.13 [[Call]], 9.5.14 [[Construct]], [[Enumerate]]

    /**
     * [[Call]] (9.5.13): the <code>apply</code> trap gets the target, the this value and an array of the arguments.
     * <p>
     * [[Call]] (9.5.13): <code>apply</code> trap 은 대상, this 값, 인수 배열을 받는다.
     *
     * @param thisValue the this value / this 값
     * @param args      arguments / 인수
     * @return the result / 결과
     */
    public JsValue call(JsValue thisValue, JsValue[] args) {
        if (!callable) {
            throw JsException.typeError("proxy is not a function");
        }
        JsObject h = liveHandler("apply");
        JsObject t = target;
        JsValue trap = trap(h, "apply");
        if (trap.isUndefined()) {
            return t.call(thisValue, args);
        }
        return Ops.call(trap, h, new JsValue[] { t, thisValue, realm.newArray(args) });
    }

    /**
     * [[Construct]] (9.5.14): the <code>construct</code> trap gets the target, an array of the arguments and new.target and must
     * return an object.
     * <p>
     * [[Construct]] (9.5.14): <code>construct</code> trap 은 대상, 인수 배열, new.target 을 받으며 객체를 돌려주어야 한다.
     *
     * @param args      arguments / 인수
     * @param newTarget the new.target / new.target
     * @return the new object / 새 객체
     */
    public JsObject construct(JsValue[] args, JsObject newTarget) {
        if (!constructor) {
            throw JsException.typeError("proxy is not a constructor");
        }
        JsObject h = liveHandler("construct");
        JsObject t = target;
        JsValue trap = trap(h, "construct");
        JsObject nt = newTarget == null ? this : newTarget;
        if (trap.isUndefined()) {
            return t.construct(args, nt);
        }
        JsValue r = Ops.call(trap, h, new JsValue[] { t, realm.newArray(args), nt });
        if (!(r instanceof JsObject)) {
            throw JsException.typeError("proxy [[Construct]] must return an object");
        }
        return (JsObject) r;
    }

}
