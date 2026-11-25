package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Installs the Reflect object (ECMA-262 17th edition 28.1). Every function takes the target object first and throws a TypeError
 * if it is not an object; the functions return booleans where the internal methods do, never throwing for ordinary failures.
 * <p>
 * Reflect 객체를 설치한다(ECMA-262 제17판 28.1). 모든 함수는 첫 인수로 대상 객체를 받으며 객체가 아니면 TypeError 를 던진다.
 * 내부 메서드가 불리언을 돌려주는 경우 함수도 불리언을 돌려준다.
 */
final class ReflectBuiltins {

    /**
     * Name of the intrinsic that holds the Reflect object.
     * <p>
     * Reflect 객체를 담는 내장 객체 이름.
     */
    static final String INTRINSIC_NAME = "%Reflect%";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ReflectBuiltins() {
    }

    /**
     * Requires an object argument.
     * <p>
     * 객체 인수를 요구한다.
     *
     * @param v    the argument / 인수
     * @param name function name for the message / 메시지에 쓸 함수 이름
     * @return the object / 객체
     */
    private static JsObject target(JsValue v, String name) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError("Reflect." + name + " called on non-object");
        }
        return (JsObject) v;
    }

    /**
     * Creates the Reflect object and defines it on the global object.
     * <p>
     * Reflect 객체를 만들어 전역 객체에 정의한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject reflect = realm.newObject();
        reflect.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("Reflect"), false, false, true);

        // Reflect.apply(target, thisArgument, argumentsList)
        realm.method(reflect, "apply", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue t = Js.arg(args, 0);
                if (!t.isCallable()) {
                    throw JsException.typeError("Reflect.apply target is not callable: " + JsException.describe(t));
                }
                JsValue[] list = Ops.createListFromArrayLike(Js.arg(args, 2));
                return ((JsObject) t).call(Js.arg(args, 1), list);
            }
        });

        // Reflect.construct(target, argumentsList [, newTarget])
        realm.method(reflect, "construct", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue t = Js.arg(args, 0);
                if (!Ops.isConstructor(t)) {
                    throw JsException.typeError("Reflect.construct target is not a constructor: " + JsException.describe(t));
                }
                JsValue nt = args.length >= 3 ? args[2] : t;
                if (!Ops.isConstructor(nt)) {
                    throw JsException.typeError("Reflect.construct newTarget is not a constructor: " + JsException.describe(nt));
                }
                JsValue[] list = Ops.createListFromArrayLike(Js.arg(args, 1));
                return ((JsObject) t).construct(list, (JsObject) nt);
            }
        });

        // Reflect.defineProperty(target, propertyKey, attributes)
        realm.method(reflect, "defineProperty", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "defineProperty");
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                return Js.bool(t.defineOwnProperty(key, Descriptors.toDescriptor(Js.arg(args, 2))));
            }
        });

        // Reflect.deleteProperty(target, propertyKey)
        realm.method(reflect, "deleteProperty", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "deleteProperty");
                return Js.bool(t.delete(Ops.toPropertyKey(Js.arg(args, 1))));
            }
        });

        // Reflect.get(target, propertyKey [, receiver])
        realm.method(reflect, "get", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "get");
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                return t.get(key, args.length >= 3 ? args[2] : t);
            }
        });

        // Reflect.getOwnPropertyDescriptor(target, propertyKey)
        realm.method(reflect, "getOwnPropertyDescriptor", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "getOwnPropertyDescriptor");
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                return Descriptors.fromDescriptor(r, t.getOwnProperty(key));
            }
        });

        // Reflect.getPrototypeOf(target)
        realm.method(reflect, "getPrototypeOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject p = target(Js.arg(args, 0), "getPrototypeOf").getPrototypeOf();
                return p == null ? Js.NULL : p;
            }
        });

        // Reflect.has(target, propertyKey)
        realm.method(reflect, "has", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "has");
                return Js.bool(t.hasProperty(Ops.toPropertyKey(Js.arg(args, 1))));
            }
        });

        // Reflect.isExtensible(target)
        realm.method(reflect, "isExtensible", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(target(Js.arg(args, 0), "isExtensible").isExtensible());
            }
        });

        // Reflect.ownKeys(target): array of strings and symbols
        realm.method(reflect, "ownKeys", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                List<Object> keys = target(Js.arg(args, 0), "ownKeys").ownKeys();
                List<JsValue> values = new ArrayList<JsValue>(keys.size());
                // 키마다 문자열 값 또는 심볼 값으로 바꿔 배열에 담는다.
                for (int i = 0; i < keys.size(); i++) {
                    values.add(PropertyKeys.toValue(keys.get(i)));
                }
                return r.newArray(values);
            }
        });

        // Reflect.preventExtensions(target)
        realm.method(reflect, "preventExtensions", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(target(Js.arg(args, 0), "preventExtensions").preventExtensions());
            }
        });

        // Reflect.set(target, propertyKey, V [, receiver])
        realm.method(reflect, "set", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "set");
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                return Js.bool(t.set(key, Js.arg(args, 2), args.length >= 4 ? args[3] : t));
            }
        });

        // Reflect.setPrototypeOf(target, proto): proto must be an object or null
        realm.method(reflect, "setPrototypeOf", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject t = target(Js.arg(args, 0), "setPrototypeOf");
                JsValue proto = Js.arg(args, 1);
                if (!(proto instanceof JsObject) && !proto.isNull()) {
                    throw JsException.typeError("Object prototype may only be an Object or null: " + JsException.describe(proto));
                }
                return Js.bool(t.setPrototypeOf(proto instanceof JsObject ? (JsObject) proto : null));
            }
        });

        realm.getGlobalObject().defineBuiltin("Reflect", reflect);
        realm.setIntrinsic(INTRINSIC_NAME, reflect);
    }
}
