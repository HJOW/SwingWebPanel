package org.duckdns.hjow.swingwebpanel.js.builtins;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The Object constructor, its static functions and Object.prototype (ECMA-262 6th edition 19.1), including the Annex B
 * <code>__proto__</code> accessor. This is the base the language needs; completeness work (Object.entries is not ES2015, but
 * getOwnPropertyDescriptors-like extras are) belongs to the full library installers.
 * <p>
 * Object 생성자, 정적 함수, Object.prototype(ECMA-262 6판 19.1)과 부속서 B 의 <code>__proto__</code> 접근자. 언어가 필요로 하는
 * 기반이며, 완성도 작업은 전체 라이브러리 설치기의 몫이다.
 */
final class ObjectBuiltins implements BuiltinInstaller {

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    ObjectBuiltins() {
    }

    /**
     * Installs Object and Object.prototype.
     * <p>
     * Object 와 Object.prototype 을 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        final JsObject proto = realm.getObjectPrototype();
        final NativeFunction[] self = new NativeFunction[1];
        NativeFunction ctor = new NativeFunction(realm, "Object", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return v.isNullOrUndefined() ? r.newObject() : Ops.toObject(r, v);
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue v = Js.arg(args, 0);
                if (newTarget != null && newTarget != self[0]) {
                    return new JsObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getObjectPrototype()));
                }
                return v.isNullOrUndefined() ? r.newObject() : Ops.toObject(r, v);
            }
        });
        self[0] = ctor;
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("Object", ctor);
        realm.setIntrinsic("%Object%", ctor);
        installStatics(realm, ctor);
        installPrototype(realm, proto);
    }

    /**
     * Converts a key list to an array of String or symbol values.
     * <p>
     * 키 목록을 String 또는 심볼 값의 배열로 바꾼다.
     *
     * @param realm   the realm / realm
     * @param keys    the keys / 키
     * @param symbols true to take symbols, false to take strings / 심볼을 고르려면 true, 문자열을 고르려면 false
     * @return the array / 배열
     */
    private static JsArray keyArray(JsRealm realm, List<Object> keys, boolean symbols) {
        JsArray a = realm.newArray();
        // 문자열 키와 심볼 키 중 요청한 종류만 담는다.
        for (Object k : keys) {
            if (symbols == (k instanceof JsSymbol)) {
                a.add(symbols ? (JsSymbol) k : JsString.of((String) k));
            }
        }
        return a;
    }

    /**
     * Requires an object argument.
     * <p>
     * 객체 인수를 요구한다.
     *
     * @param v    the value / 값
     * @param what the description for the error / 오류 설명
     * @return the object / 객체
     */
    private static JsObject requireObject(JsValue v, String what) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError(what + " called on non-object");
        }
        return (JsObject) v;
    }

    /**
     * Applies an integrity level (SetIntegrityLevel): "sealed" or "frozen".
     * <p>
     * 무결성 수준을 적용한다(SetIntegrityLevel): "sealed" 또는 "frozen".
     *
     * @param o      the object / 객체
     * @param frozen true for frozen, false for sealed / frozen 이면 true, sealed 이면 false
     */
    private static void setIntegrity(JsObject o, boolean frozen) {
        if (!o.preventExtensions()) {
            throw JsException.typeError("Cannot prevent extensions");
        }
        // 모든 자체 프로퍼티를 설정 불가로 만들고, frozen 이면 데이터 프로퍼티를 쓰기 불가로 만든다.
        for (Object k : o.ownKeys()) {
            PropertyDescriptor cur = o.getOwnProperty(k);
            if (cur == null) {
                continue;
            }
            PropertyDescriptor d = new PropertyDescriptor();
            d.configurable = Boolean.FALSE;
            if (frozen && !cur.isAccessor()) {
                d.writable = Boolean.FALSE;
            }
            o.definePropertyOrThrow(k, d);
        }
    }

    /**
     * Tests an integrity level (TestIntegrityLevel).
     * <p>
     * 무결성 수준을 검사한다(TestIntegrityLevel).
     *
     * @param o      the object / 객체
     * @param frozen true to test frozen, false to test sealed / frozen 검사이면 true, sealed 검사이면 false
     * @return true if the object has the level / 그 수준이면 true
     */
    private static boolean testIntegrity(JsObject o, boolean frozen) {
        if (o.isExtensible()) {
            return false;
        }
        // 모든 자체 프로퍼티가 설정 불가(frozen 이면 데이터 프로퍼티는 쓰기 불가)인지 확인한다.
        for (Object k : o.ownKeys()) {
            PropertyDescriptor d = o.getOwnProperty(k);
            if (d == null) {
                continue;
            }
            if (d.isConfigurableTrue()) {
                return false;
            }
            if (frozen && d.isData() && d.isWritableTrue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Defines properties from a descriptor map object (ObjectDefineProperties).
     * <p>
     * 기술자 맵 객체로 프로퍼티를 정의한다(ObjectDefineProperties).
     *
     * @param o     the target object / 대상 객체
     * @param props the descriptor map / 기술자 맵
     * @param realm the realm / realm
     */
    private static void defineProperties(JsObject o, JsValue props, JsRealm realm) {
        JsObject p = Ops.toObject(realm, props);
        List<Object> keys = new ArrayList<Object>();
        List<PropertyDescriptor> descs = new ArrayList<PropertyDescriptor>();
        // 열거 가능한 자체 프로퍼티의 기술자를 모두 읽은 뒤에 정의한다.
        for (Object k : p.ownKeys()) {
            PropertyDescriptor pd = p.getOwnProperty(k);
            if (pd != null && pd.isEnumerableTrue()) {
                keys.add(k);
                descs.add(DescriptorOps.toDescriptor(p.get(k)));
            }
        }
        // 속성 이름마다 설명자를 적용해 프로퍼티를 정의한다.
        for (int i = 0; i < keys.size(); i++) {
            o.definePropertyOrThrow(keys.get(i), descs.get(i));
        }
    }

    /**
     * Installs the static functions of Object.
     * <p>
     * Object 의 정적 함수를 설치한다.
     *
     * @param realm the realm / realm
     * @param ctor  the Object constructor / Object 생성자
     */
    private void installStatics(final JsRealm realm, JsObject ctor) {
        realm.method(ctor, "getPrototypeOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject p = Ops.toObject(r, Js.arg(args, 0)).getPrototypeOf();
                return p == null ? Js.NULL : p;
            }
        });
        realm.method(ctor, "setPrototypeOf", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(Js.arg(args, 0));
                JsValue p = Js.arg(args, 1);
                if (!(p instanceof JsObject) && !p.isNull()) {
                    throw JsException.typeError("Object prototype may only be an Object or null: " + JsException.describe(p));
                }
                if (!(o instanceof JsObject)) {
                    return o;
                }
                if (!((JsObject) o).setPrototypeOf(p.isNull() ? null : (JsObject) p)) {
                    throw JsException.typeError("Cannot set prototype (cyclic or non-extensible)");
                }
                return o;
            }
        });
        realm.method(ctor, "create", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue p = Js.arg(args, 0);
                if (!(p instanceof JsObject) && !p.isNull()) {
                    throw JsException.typeError("Object prototype may only be an Object or null: " + JsException.describe(p));
                }
                JsObject o = new JsObject(p.isNull() ? null : (JsObject) p);
                JsValue props = Js.arg(args, 1);
                if (!props.isUndefined()) {
                    defineProperties(o, props, r);
                }
                return o;
            }
        });
        realm.method(ctor, "defineProperty", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = requireObject(Js.arg(args, 0), "Object.defineProperty");
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                o.definePropertyOrThrow(key, DescriptorOps.toDescriptor(Js.arg(args, 2)));
                return o;
            }
        });
        realm.method(ctor, "defineProperties", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = requireObject(Js.arg(args, 0), "Object.defineProperties");
                defineProperties(o, Js.arg(args, 1), r);
                return o;
            }
        });
        realm.method(ctor, "getOwnPropertyDescriptor", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, Js.arg(args, 0));
                return DescriptorOps.fromDescriptor(r, o.getOwnProperty(Ops.toPropertyKey(Js.arg(args, 1))));
            }
        });
        realm.method(ctor, "getOwnPropertyNames", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return keyArray(r, Ops.toObject(r, Js.arg(args, 0)).ownKeys(), false);
            }
        });
        realm.method(ctor, "getOwnPropertySymbols", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return keyArray(r, Ops.toObject(r, Js.arg(args, 0)).ownKeys(), true);
            }
        });
        realm.method(ctor, "keys", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsArray a = r.newArray();
                // 열거 가능한 자체 문자열 키를 순서대로 담는다.
                for (String k : Ops.enumerableOwnKeys(Ops.toObject(r, Js.arg(args, 0)))) {
                    a.add(JsString.of(k));
                }
                return a;
            }
        });
        realm.method(ctor, "preventExtensions", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                if (o instanceof JsObject && !((JsObject) o).preventExtensions()) {
                    throw JsException.typeError("Cannot prevent extensions");
                }
                return o;
            }
        });
        realm.method(ctor, "isExtensible", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                return JsBoolean.of(o instanceof JsObject && ((JsObject) o).isExtensible());
            }
        });
        realm.method(ctor, "freeze", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                if (o instanceof JsObject) {
                    setIntegrity((JsObject) o, true);
                }
                return o;
            }
        });
        realm.method(ctor, "isFrozen", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                return JsBoolean.of(!(o instanceof JsObject) || testIntegrity((JsObject) o, true));
            }
        });
        realm.method(ctor, "seal", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                if (o instanceof JsObject) {
                    setIntegrity((JsObject) o, false);
                }
                return o;
            }
        });
        realm.method(ctor, "isSealed", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                return JsBoolean.of(!(o instanceof JsObject) || testIntegrity((JsObject) o, false));
            }
        });
        realm.method(ctor, "is", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsBoolean.of(Ops.sameValue(Js.arg(args, 0), Js.arg(args, 1)));
            }
        });
        realm.method(ctor, "assign", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject to = Ops.toObject(r, Js.arg(args, 0));
                // 각 소스의 열거 가능한 자체 프로퍼티를 대상에 복사한다.
                for (int i = 1; i < args.length; i++) {
                    if (args[i].isNullOrUndefined()) {
                        continue;
                    }
                    JsObject from = Ops.toObject(r, args[i]);
                    // 원본 객체의 열거 가능한 자체 프로퍼티를 대상에 복사한다.
                    for (Object k : from.ownKeys()) {
                        PropertyDescriptor d = from.getOwnProperty(k);
                        if (d != null && d.isEnumerableTrue()) {
                            to.setStrict(k, from.get(k));
                        }
                    }
                }
                return to;
            }
        });
    }

    /**
     * Installs the methods of Object.prototype.
     * <p>
     * Object.prototype 의 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Object.prototype / Object.prototype
     */
    private void installPrototype(final JsRealm realm, JsObject proto) {
        realm.method(proto, "hasOwnProperty", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Object key = Ops.toPropertyKey(Js.arg(args, 0));
                return JsBoolean.of(Ops.toObject(r, thisValue).getOwnProperty(key) != null);
            }
        });
        realm.method(proto, "isPrototypeOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                if (!(v instanceof JsObject)) {
                    return Js.FALSE;
                }
                JsObject o = Ops.toObject(r, thisValue);
                JsObject p = ((JsObject) v).getPrototypeOf();
                // v 의 프로토타입 체인에 이 객체가 있는지 찾는다.
                while (p != null) {
                    if (p == o) {
                        return Js.TRUE;
                    }
                    p = p.getPrototypeOf();
                }
                return Js.FALSE;
            }
        });
        realm.method(proto, "propertyIsEnumerable", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Object key = Ops.toPropertyKey(Js.arg(args, 0));
                PropertyDescriptor d = Ops.toObject(r, thisValue).getOwnProperty(key);
                return JsBoolean.of(d != null && d.isEnumerableTrue());
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (thisValue.isUndefined()) {
                    return JsString.of("[object Undefined]");
                }
                if (thisValue.isNull()) {
                    return JsString.of("[object Null]");
                }
                JsObject o = Ops.toObject(r, thisValue);
                String cn = o.className();
                String tag = "Arguments".equals(cn) || "Array".equals(cn) || "Function".equals(cn) || "Error".equals(cn)
                        || "Boolean".equals(cn) || "Number".equals(cn) || "String".equals(cn) || "Date".equals(cn) || "RegExp".equals(cn)
                        ? cn : "Object";
                JsValue t = o.get(WellKnownSymbols.TO_STRING_TAG);
                if (t instanceof JsString) {
                    tag = ((JsString) t).getValue();
                }
                return JsString.of("[object " + tag + "]");
            }
        });
        realm.method(proto, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Ops.invoke(r, thisValue, "toString", Js.NO_ARGS);
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Ops.toObject(r, thisValue);
            }
        });
        NativeFunction getter = realm.newFunction("get __proto__", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject p = Ops.toObject(r, thisValue).getPrototypeOf();
                return p == null ? Js.NULL : p;
            }
        });
        NativeFunction setter = realm.newFunction("set __proto__", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Ops.requireObjectCoercible(thisValue);
                JsValue p = Js.arg(args, 0);
                if ((!(p instanceof JsObject) && !p.isNull()) || !(thisValue instanceof JsObject)) {
                    return Js.UNDEFINED;
                }
                if (!((JsObject) thisValue).setPrototypeOf(p.isNull() ? null : (JsObject) p)) {
                    throw JsException.typeError("Cannot set prototype (cyclic or non-extensible)");
                }
                return Js.UNDEFINED;
            }
        });
        proto.defineAccessor("__proto__", getter, setter, false, true);
    }
}
