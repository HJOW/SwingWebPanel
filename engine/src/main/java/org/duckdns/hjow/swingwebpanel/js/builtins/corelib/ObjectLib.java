package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.DescriptorOps;

/**
 * Installs the <code>Object</code> constructor with its static methods, <code>Object.prototype</code> and the Annex B accessor and
 * helper methods (<code>__proto__</code>, <code>__defineGetter__</code>, <code>__defineSetter__</code>, <code>__lookupGetter__</code>,
 * <code>__lookupSetter__</code>) of ECMA-262 6th edition section 19.1 and B.2.2, with the later ES2022 static methods.
 * The property descriptor conversion and integrity level helpers are package visible because other installers need them.
 * <p>
 * ECMA-262 6판 19.1 절과 부속서 B.2.2 의 <code>Object</code> 생성자와 정적 메서드, <code>Object.prototype</code>, 부속서 B 의 접근자와
 * 도우미 메서드(<code>__proto__</code>, <code>__defineGetter__</code>, <code>__defineSetter__</code>, <code>__lookupGetter__</code>,
 * <code>__lookupSetter__</code>)와 ES2022 까지 추가된 정적 메서드를 설치한다. 프로퍼티 기술자 변환과 무결성 수준 도우미는 다른 설치기가
 * 필요로 하므로 패키지 공개이다.
 */
final class ObjectLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ObjectLib() {
    }

    /**
     * Installs the Object constructor and Object.prototype into the realm.
     * <p>
     * realm 에 Object 생성자와 Object.prototype 을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = realm.getObjectPrototype();
        final NativeFunction[] self = new NativeFunction[1];
        self[0] = LibSupport.defineConstructor(realm, "Object", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return v.isNullOrUndefined() ? r.newObject() : Ops.toObject(r, v);
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                // new.target 이 Object 자신이 아니면 하위 클래스 생성이므로 그 프로토타입으로 만든다.
                if (newTarget != null && newTarget != self[0]) {
                    return new JsObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getObjectPrototype()));
                }
                JsValue v = Js.arg(args, 0);
                return v.isNullOrUndefined() ? r.newObject() : Ops.toObject(r, v);
            }
        }, proto, null);
        installStatics(realm, self[0]);
        installPrototype(realm, proto);
        installAnnexB(realm, proto);
    }

    /**
     * Installs the static methods of Object.
     * <p>
     * Object 의 정적 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param ctor  the Object constructor / Object 생성자
     */
    private static void installStatics(JsRealm realm, JsObject ctor) {
        realm.method(ctor, "assign", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject to = Ops.toObject(r, Js.arg(args, 0));
                // 두 번째 인수부터 원본 객체를 차례로 읽어 열거 가능한 자체 프로퍼티를 복사한다.
                for (int i = 1; i < args.length; i++) {
                    if (args[i].isNullOrUndefined()) {
                        continue;
                    }
                    JsObject from = Ops.toObject(r, args[i]);
                    List<Object> keys = from.ownKeys();
                    for (int k = 0; k < keys.size(); k++) { // 열거 가능한 키마다 값을 읽어 대상에 엄격하게 설정한다.
                        Object key = keys.get(k);
                        PropertyDescriptor d = from.getOwnProperty(key);
                        if (d != null && d.isEnumerableTrue()) {
                            to.setStrict(key, from.get(key));
                        }
                    }
                }
                return to;
            }
        });
        realm.method(ctor, "create", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue p = Js.arg(args, 0);
                if (!(p instanceof JsObject) && !p.isNull()) {
                    throw JsException.typeError("Object prototype may only be an Object or null: " + LibSupport.describe(p));
                }
                JsObject o = new JsObject(p.isNull() ? null : (JsObject) p);
                JsValue props = Js.arg(args, 1);
                if (!props.isUndefined()) {
                    defineProperties(r, o, props);
                }
                return o;
            }
        });
        realm.method(ctor, "entries", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return enumerableKeyValueArray(r, Ops.toObject(r, Js.arg(args, 0)), false);
            }
        });
        realm.method(ctor, "fromEntries", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return fromEntries(r, Js.arg(args, 0));
            }
        });
        realm.method(ctor, "defineProperties", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = LibSupport.requireObject(Js.arg(args, 0), "Object.defineProperties called on non-object");
                defineProperties(r, o, Js.arg(args, 1));
                return o;
            }
        });
        realm.method(ctor, "defineProperty", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = LibSupport.requireObject(Js.arg(args, 0), "Object.defineProperty called on non-object");
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                PropertyDescriptor desc = DescriptorOps.toDescriptor(Js.arg(args, 2));
                o.definePropertyOrThrow(key, desc);
                return o;
            }
        });
        realm.method(ctor, "freeze", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                if (v instanceof JsObject && !setIntegrityLevel((JsObject) v, true)) {
                    throw JsException.typeError("Cannot freeze");
                }
                return v;
            }
        });
        realm.method(ctor, "getOwnPropertyDescriptor", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, Js.arg(args, 0));
                Object key = Ops.toPropertyKey(Js.arg(args, 1));
                return DescriptorOps.fromDescriptor(r, o.getOwnProperty(key));
            }
        });
        realm.method(ctor, "getOwnPropertyDescriptors", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, Js.arg(args, 0));
                JsObject result = r.newObject();
                List<Object> keys = o.ownKeys();
                for (int i = 0; i < keys.size(); i++) {
                    Object key = keys.get(i);
                    PropertyDescriptor desc = o.getOwnProperty(key);
                    if (desc != null) {
                        result.createDataPropertyOrThrow(key, DescriptorOps.fromDescriptor(r, desc));
                    }
                }
                return result;
            }
        });
        realm.method(ctor, "getOwnPropertyNames", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return ownKeysArray(r, Ops.toObject(r, Js.arg(args, 0)), false);
            }
        });
        realm.method(ctor, "getOwnPropertySymbols", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return ownKeysArray(r, Ops.toObject(r, Js.arg(args, 0)), true);
            }
        });
        realm.method(ctor, "getPrototypeOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject p = Ops.toObject(r, Js.arg(args, 0)).getPrototypeOf();
                return p == null ? Js.NULL : p;
            }
        });
        realm.method(ctor, "is", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Ops.sameValue(Js.arg(args, 0), Js.arg(args, 1)));
            }
        });
        realm.method(ctor, "isExtensible", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(v instanceof JsObject && ((JsObject) v).isExtensible());
            }
        });
        realm.method(ctor, "isFrozen", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(!(v instanceof JsObject) || testIntegrityLevel((JsObject) v, true));
            }
        });
        realm.method(ctor, "isSealed", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(!(v instanceof JsObject) || testIntegrityLevel((JsObject) v, false));
            }
        });
        realm.method(ctor, "keys", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, Js.arg(args, 0));
                List<JsValue> names = new ArrayList<JsValue>();
                List<Object> keys = o.ownKeys();
                for (int i = 0; i < keys.size(); i++) { // 문자열 키 중 열거 가능한 것만 모은다.
                    Object key = keys.get(i);
                    if (key instanceof String) {
                        PropertyDescriptor d = o.getOwnProperty(key);
                        if (d != null && d.isEnumerableTrue()) {
                            names.add(JsString.of((String) key));
                        }
                    }
                }
                return r.newArray(names);
            }
        });
        realm.method(ctor, "hasOwn", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, Js.arg(args, 0));
                return Js.bool(o.hasOwnProperty(Ops.toPropertyKey(Js.arg(args, 1))));
            }
        });
        realm.method(ctor, "preventExtensions", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                if (v instanceof JsObject && !((JsObject) v).preventExtensions()) {
                    throw JsException.typeError("Cannot prevent extensions");
                }
                return v;
            }
        });
        realm.method(ctor, "seal", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                if (v instanceof JsObject && !setIntegrityLevel((JsObject) v, false)) {
                    throw JsException.typeError("Cannot seal");
                }
                return v;
            }
        });
        realm.method(ctor, "setPrototypeOf", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(Js.arg(args, 0));
                JsValue p = Js.arg(args, 1);
                if (!(p instanceof JsObject) && !p.isNull()) {
                    throw JsException.typeError("Object prototype may only be an Object or null: " + LibSupport.describe(p));
                }
                if (!(o instanceof JsObject)) {
                    return o;
                }
                if (!((JsObject) o).setPrototypeOf(p.isNull() ? null : (JsObject) p)) {
                    throw JsException.typeError("Cannot set prototype");
                }
                return o;
            }
        });
        realm.method(ctor, "values", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return enumerableKeyValueArray(r, Ops.toObject(r, Js.arg(args, 0)), true);
            }
        });
    }

    /**
     * Creates the array used by Object.entries or Object.values from enumerable own string keys.
     * <p>
     * 열거 가능한 자체 문자열 키에서 Object.entries 또는 Object.values 가 돌려줄 배열을 만든다.
     *
     * @param realm the realm / realm
     * @param object the source object / 원본 객체
     * @param values true for values only / 값만 모으려면 true
     * @return the entries or values array / 항목 또는 값 배열
     */
    private static JsValue enumerableKeyValueArray(JsRealm realm, JsObject object, boolean values) {
        List<JsValue> out = new ArrayList<JsValue>();
        List<Object> keys = object.ownKeys();
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);
            if (!(key instanceof String)) {
                continue;
            }
            PropertyDescriptor desc = object.getOwnProperty(key);
            if (desc == null || !desc.isEnumerableTrue()) {
                continue;
            }
            JsValue value = object.get(key);
            if (values) {
                out.add(value);
            } else {
                out.add(realm.newArray(new JsValue[] { JsString.of((String) key), value }));
            }
        }
        return realm.newArray(out);
    }

    /**
     * Implements Object.fromEntries, including iterator closing after a malformed pair or a property definition failure.
     * <p>
     * Object.fromEntries 를 구현한다. 잘못된 쌍이나 프로퍼티 정의 실패 뒤에는 이터레이터를 닫는다.
     *
     * @param realm the realm / realm
     * @param iterable the iterable / 이터러블
     * @return the new object / 새 객체
     */
    private static JsValue fromEntries(JsRealm realm, JsValue iterable) {
        JsObject result = realm.newObject();
        JsObject iterator = org.duckdns.hjow.swingwebpanel.js.IteratorHelper.getIterator(realm, iterable);
        boolean done = false;
        try {
            while (true) {
                JsObject next = org.duckdns.hjow.swingwebpanel.js.IteratorHelper.step(iterator);
                if (next == null) {
                    done = true;
                    return result;
                }
                JsValue entryValue = org.duckdns.hjow.swingwebpanel.js.IteratorHelper.value(next);
                if (!(entryValue instanceof JsObject)) {
                    throw JsException.typeError("Object.fromEntries iterator value is not an entry object");
                }
                JsObject entry = (JsObject) entryValue;
                JsValue key = entry.get("0");
                JsValue value = entry.get("1");
                result.createDataPropertyOrThrow(Ops.toPropertyKey(key), value);
            }
        } catch (JsException e) {
            if (!done) {
                org.duckdns.hjow.swingwebpanel.js.IteratorHelper.closeQuietly(iterator);
            }
            throw e;
        }
    }

    /**
     * Installs the methods of Object.prototype that are part of the ES2015 core.
     * <p>
     * ES2015 본문에 속하는 Object.prototype 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Object.prototype / Object.prototype
     */
    private static void installPrototype(JsRealm realm, JsObject proto) {
        realm.method(proto, "hasOwnProperty", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Object key = Ops.toPropertyKey(Js.arg(args, 0));
                JsObject o = Ops.toObject(r, thisValue);
                return Js.bool(o.hasOwnProperty(key));
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
                // 프로토타입 체인을 끝까지 따라가며 o 와 같은 객체를 찾는다.
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
                JsObject o = Ops.toObject(r, thisValue);
                PropertyDescriptor d = o.getOwnProperty(key);
                return Js.bool(d != null && d.isEnumerableTrue());
            }
        });
        realm.method(proto, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Ops.invoke(r, thisValue, "toString", Js.NO_ARGS);
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(objectToString(r, thisValue));
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Ops.toObject(r, thisValue);
            }
        });
    }

    /**
     * Installs the Annex B members of Object.prototype.
     * <p>
     * Object.prototype 의 부속서 B 멤버를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Object.prototype / Object.prototype
     */
    private static void installAnnexB(JsRealm realm, JsObject proto) {
        NativeFunction getter = new NativeFunction(realm, "get __proto__", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject p = Ops.toObject(r, thisValue).getPrototypeOf();
                return p == null ? Js.NULL : p;
            }
        });
        NativeFunction setter = new NativeFunction(realm, "set __proto__", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue p = Js.arg(args, 0);
                if ((!(p instanceof JsObject) && !p.isNull()) || !(o instanceof JsObject)) {
                    return Js.UNDEFINED;
                }
                if (!((JsObject) o).setPrototypeOf(p.isNull() ? null : (JsObject) p)) {
                    throw JsException.typeError("Cannot set prototype");
                }
                return Js.UNDEFINED;
            }
        });
        proto.defineAccessor("__proto__", getter, setter, false, true);
        realm.method(proto, "__defineGetter__", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                JsValue g = Js.arg(args, 1);
                if (!g.isCallable()) {
                    throw JsException.typeError("Object.prototype.__defineGetter__: Expecting function");
                }
                PropertyDescriptor d = new PropertyDescriptor();
                d.get = g;
                d.enumerable = Boolean.TRUE;
                d.configurable = Boolean.TRUE;
                o.definePropertyOrThrow(Ops.toPropertyKey(Js.arg(args, 0)), d);
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "__defineSetter__", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                JsValue s = Js.arg(args, 1);
                if (!s.isCallable()) {
                    throw JsException.typeError("Object.prototype.__defineSetter__: Expecting function");
                }
                PropertyDescriptor d = new PropertyDescriptor();
                d.set = s;
                d.enumerable = Boolean.TRUE;
                d.configurable = Boolean.TRUE;
                o.definePropertyOrThrow(Ops.toPropertyKey(Js.arg(args, 0)), d);
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "__lookupGetter__", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return lookupAccessor(r, thisValue, Js.arg(args, 0), true);
            }
        });
        realm.method(proto, "__lookupSetter__", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return lookupAccessor(r, thisValue, Js.arg(args, 0), false);
            }
        });
    }

    /**
     * Implements __lookupGetter__ and __lookupSetter__: walks the prototype chain for the first own property with that key.
     * <p>
     * __lookupGetter__ 와 __lookupSetter__ 를 구현한다: 프로토타입 체인에서 그 키를 가진 첫 자체 프로퍼티를 찾는다.
     *
     * @param realm     the realm / realm
     * @param thisValue the receiver / 수신 값
     * @param keyValue  the key argument / 키 인수
     * @param getter    true to return the getter, false for the setter / getter 를 돌려주려면 true, setter 면 false
     * @return the function or undefined / 함수 또는 undefined
     */
    private static JsValue lookupAccessor(JsRealm realm, JsValue thisValue, JsValue keyValue, boolean getter) {
        JsObject o = Ops.toObject(realm, thisValue);
        Object key = Ops.toPropertyKey(keyValue);
        // 체인을 따라 올라가며 키를 가진 첫 객체의 접근자를 확인한다.
        while (o != null) {
            PropertyDescriptor d = o.getOwnProperty(key);
            if (d != null) {
                if (d.isAccessor()) {
                    JsValue f = getter ? d.get : d.set;
                    return f == null ? Js.UNDEFINED : f;
                }
                return Js.UNDEFINED;
            }
            o = o.getPrototypeOf();
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements Object.prototype.toString (19.1.3.6): "[object Tag]" with the built-in tag or the Symbol.toStringTag string.
     * <p>
     * Object.prototype.toString (19.1.3.6)을 구현한다: 내장 태그 또는 Symbol.toStringTag 문자열을 쓴 "[object Tag]".
     *
     * @param realm     the realm / realm
     * @param thisValue the this value / this 값
     * @return the string / 문자열
     */
    static String objectToString(JsRealm realm, JsValue thisValue) {
        if (thisValue.isUndefined()) {
            return "[object Undefined]";
        }
        if (thisValue.isNull()) {
            return "[object Null]";
        }
        JsObject o = Ops.toObject(realm, thisValue);
        String builtin = "Object";
        String cn = o.className();
        if (Ops.isArray(o)) {
            builtin = "Array";
        } else if (o.isCallable()) {
            builtin = "Function";
        } else if ("Arguments".equals(cn) || "Error".equals(cn) || "Boolean".equals(cn) || "Number".equals(cn) || "String".equals(cn)
                || "Date".equals(cn) || "RegExp".equals(cn)) {
            builtin = cn;
        }
        JsValue tag = o.get(WellKnownSymbols.TO_STRING_TAG);
        if (tag instanceof JsString) {
            builtin = ((JsString) tag).getValue();
        }
        return "[object " + builtin + "]";
    }

    /**
     * Converts the own keys of an object into an array of strings or of symbols.
     * <p>
     * 객체의 자체 키를 문자열 배열 또는 심볼 배열로 바꾼다.
     *
     * @param realm   the realm / realm
     * @param o       the object / 객체
     * @param symbols true for the symbol keys, false for the string keys / 심볼 키이면 true, 문자열 키이면 false
     * @return a new array / 새 배열
     */
    private static JsValue ownKeysArray(JsRealm realm, JsObject o, boolean symbols) {
        List<JsValue> out = new ArrayList<JsValue>();
        List<Object> keys = o.ownKeys();
        // 요청한 종류(문자열/심볼)의 키만 순서대로 모은다.
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);
            if (symbols ? key instanceof JsSymbol : key instanceof String) {
                out.add(PropertyKeys.toValue(key));
            }
        }
        return realm.newArray(out);
    }

    /**
     * ObjectDefineProperties (19.1.2.3.1): defines every enumerable own property of a descriptor map on an object. All descriptors are
     * validated first; only then are the properties defined.
     * <p>
     * ObjectDefineProperties (19.1.2.3.1): 기술자 맵의 열거 가능한 자체 프로퍼티를 모두 객체에 정의한다. 기술자를 모두 검증한 뒤에 정의한다.
     *
     * @param realm      the realm / realm
     * @param o          the target object / 대상 객체
     * @param properties the descriptor map value / 기술자 맵 값
     */
    static void defineProperties(JsRealm realm, JsObject o, JsValue properties) {
        JsObject props = Ops.toObject(realm, properties);
        List<Object> names = new ArrayList<Object>();
        List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        List<Object> keys = props.ownKeys();
        for (int i = 0; i < keys.size(); i++) { // 열거 가능한 키마다 기술자 객체를 읽어 변환한다.
            Object key = keys.get(i);
            PropertyDescriptor pd = props.getOwnProperty(key);
            if (pd != null && pd.isEnumerableTrue()) {
                descriptors.add(DescriptorOps.toDescriptor(props.get(key)));
                names.add(key);
            }
        }
        for (int i = 0; i < names.size(); i++) { // 검증이 끝난 기술자를 차례로 정의한다.
            o.definePropertyOrThrow(names.get(i), descriptors.get(i));
        }
    }

    /**
     * SetIntegrityLevel (7.3.14): makes an object non-extensible and its properties non-configurable (sealed) and, for data properties,
     * non-writable (frozen).
     * <p>
     * SetIntegrityLevel (7.3.14): 객체를 확장 불가로 만들고 프로퍼티를 configurable 아님(sealed)으로, 데이터 프로퍼티는 writable 아님(frozen)으로 만든다.
     *
     * @param o      the object / 객체
     * @param frozen true for frozen, false for sealed / frozen 이면 true, sealed 면 false
     * @return false if the object could not be made non-extensible / 확장 불가로 만들 수 없으면 false
     */
    static boolean setIntegrityLevel(JsObject o, boolean frozen) {
        if (!o.preventExtensions()) {
            return false;
        }
        List<Object> keys = o.ownKeys();
        for (int i = 0; i < keys.size(); i++) { // 자체 키마다 configurable(과 데이터 프로퍼티의 writable)을 끈다.
            Object key = keys.get(i);
            PropertyDescriptor d = new PropertyDescriptor();
            d.configurable = Boolean.FALSE;
            if (frozen) {
                PropertyDescriptor current = o.getOwnProperty(key);
                if (current == null) {
                    continue;
                }
                if (current.isData()) {
                    d.writable = Boolean.FALSE;
                }
            }
            o.definePropertyOrThrow(key, d);
        }
        return true;
    }

    /**
     * TestIntegrityLevel (7.3.15): tells whether an object is sealed or frozen.
     * <p>
     * TestIntegrityLevel (7.3.15): 객체가 sealed 또는 frozen 인지 알려 준다.
     *
     * @param o      the object / 객체
     * @param frozen true to test for frozen, false for sealed / frozen 을 검사하려면 true, sealed 면 false
     * @return true if the object has that level / 그 수준이면 true
     */
    static boolean testIntegrityLevel(JsObject o, boolean frozen) {
        if (o.isExtensible()) {
            return false;
        }
        List<Object> keys = o.ownKeys();
        for (int i = 0; i < keys.size(); i++) { // 하나라도 configurable(또는 frozen 일 때 writable 데이터)이면 그 수준이 아니다.
            PropertyDescriptor d = o.getOwnProperty(keys.get(i));
            if (d != null) {
                if (d.isConfigurableTrue()) {
                    return false;
                }
                if (frozen && d.isData() && d.isWritableTrue()) {
                    return false;
                }
            }
        }
        return true;
    }
}
