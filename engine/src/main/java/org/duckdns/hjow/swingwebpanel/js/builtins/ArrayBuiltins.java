package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The minimal Array built-in the language needs: the constructor, <code>Array.isArray</code>, <code>Array.of</code> and the
 * prototype methods <code>push</code>, <code>pop</code>, <code>shift</code>, <code>join</code>, <code>toString</code>,
 * <code>slice</code>, <code>indexOf</code>, <code>forEach</code>, <code>map</code> and the iterator methods
 * <code>values</code>, <code>keys</code>, <code>entries</code> and <code>[Symbol.iterator]</code>, all generic over array-like
 * objects. The complete Array library belongs to the full built-in library installers, which may replace any of these
 * methods.
 * <p>
 * 언어가 필요로 하는 최소 Array 내장 객체: 생성자, <code>Array.isArray</code>, <code>Array.of</code>, 프로토타입 메서드
 * <code>push</code>, <code>pop</code>, <code>shift</code>, <code>join</code>, <code>toString</code>, <code>slice</code>,
 * <code>indexOf</code>, <code>forEach</code>, <code>map</code>, 이터레이터 메서드 <code>values</code>, <code>keys</code>,
 * <code>entries</code>, <code>[Symbol.iterator]</code> 이며 모두 유사 배열 객체에 대해 일반적으로 동작한다.
 * 완전한 Array 라이브러리는 전체 내장 라이브러리 설치기의 몫이며, 그쪽에서 이 메서드들을 교체할 수 있다.
 */
final class ArrayBuiltins implements BuiltinInstaller {

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    ArrayBuiltins() {
    }

    /**
     * Returns a property key for an index.
     * <p>
     * 인덱스의 프로퍼티 키를 반환한다.
     *
     * @param i the index / 인덱스
     * @return the key / 키
     */
    private static String key(double i) {
        return i == (long) i ? Long.toString((long) i) : org.duckdns.hjow.swingwebpanel.js.NumberText.toString(i);
    }

    /**
     * Resolves a relative index argument (negative counts from the end) into 0..len.
     * <p>
     * 상대 인덱스 인수(음수는 끝에서부터)를 0..len 으로 바꾼다.
     *
     * @param v        the argument / 인수
     * @param len      the length / 길이
     * @param defaultV the value when the argument is undefined / 인수가 undefined 일 때의 값
     * @return the absolute index / 절대 인덱스
     */
    private static double relative(JsValue v, double len, double defaultV) {
        if (v.isUndefined()) {
            return defaultV;
        }
        double rel = Ops.toInteger(v);
        return rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
    }

    /**
     * Installs Array.
     * <p>
     * Array 를 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        final JsObject proto = realm.getArrayPrototype();
        final NativeFunction.Construct construct = new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsObject p = Ops.getPrototypeFromConstructor(r, newTarget, r.getArrayPrototype());
                JsArray a = new JsArray(p);
                if (args.length == 1 && args[0] instanceof JsNumber) {
                    double d = ((JsNumber) args[0]).getValue();
                    if (Ops.toUint32(d) != d) {
                        throw JsException.rangeError("Invalid array length");
                    }
                    a.set("length", args[0]);
                } else {
                    // 인수를 순서대로 배열 요소로 넣는다.
                    for (int i = 0; i < args.length; i++) {
                        a.add(args[i]);
                    }
                }
                return a;
            }
        };
        final NativeFunction[] self = new NativeFunction[1];
        NativeFunction ctor = new NativeFunction(realm, "Array", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return construct.construct(r, args, self[0]);
            }
        }, construct);
        self[0] = ctor;
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("Array", ctor);
        realm.setIntrinsic("%Array%", ctor);
        realm.getter(ctor, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
        realm.method(ctor, "isArray", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsBoolean.of(Ops.isArray(Js.arg(args, 0)));
            }
        });
        realm.method(ctor, "of", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsArray a = r.newArray();
                // 인수를 순서대로 배열 요소로 넣는다.
                for (int i = 0; i < args.length; i++) {
                    a.add(args[i]);
                }
                return a;
            }
        });
        installMethods(realm, proto);
        installIteration(realm, proto);
    }

    /**
     * Installs the element methods of Array.prototype.
     * <p>
     * Array.prototype 의 요소 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private void installMethods(JsRealm realm, JsObject proto) {
        realm.method(proto, "push", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                // 인수를 끝에 차례로 붙인다.
                for (int i = 0; i < args.length; i++) {
                    o.setStrict(key(len), args[i]);
                    len++;
                }
                o.setStrict("length", JsNumber.of(len));
                return JsNumber.of(len);
            }
        });
        realm.method(proto, "pop", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    o.setStrict("length", JsNumber.ZERO);
                    return Js.UNDEFINED;
                }
                String k = key(len - 1);
                JsValue v = o.get(k);
                if (!o.delete(k)) {
                    throw JsException.typeError("Cannot delete property '" + k + "'");
                }
                o.setStrict("length", JsNumber.of(len - 1));
                return v;
            }
        });
        realm.method(proto, "shift", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    o.setStrict("length", JsNumber.ZERO);
                    return Js.UNDEFINED;
                }
                JsValue first = o.get("0");
                // 나머지 요소를 한 칸씩 앞으로 옮기고 마지막 칸을 지운다.
                for (double k = 1; k < len; k++) {
                    String from = key(k);
                    String to = key(k - 1);
                    if (o.hasProperty(from)) {
                        o.setStrict(to, o.get(from));
                    } else if (!o.delete(to)) {
                        throw JsException.typeError("Cannot delete property '" + to + "'");
                    }
                }
                o.delete(key(len - 1));
                o.setStrict("length", JsNumber.of(len - 1));
                return first;
            }
        });
        realm.method(proto, "join", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                String sep = Js.arg(args, 0).isUndefined() ? "," : Ops.toString(args[0]);
                StringBuilder sb = new StringBuilder();
                // 요소를 구분자로 이어 붙인다. undefined 와 null 은 빈 문자열이다.
                for (double k = 0; k < len; k++) {
                    if (k > 0) {
                        sb.append(sep);
                    }
                    JsValue e = o.get(key(k));
                    if (!e.isNullOrUndefined()) {
                        sb.append(Ops.toString(e));
                    }
                    r.getLimits().checkStringLength(sb.length());
                }
                return JsString.of(sb.toString());
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                JsValue join = o.get("join");
                if (join.isCallable()) {
                    return ((JsObject) join).call(o, Js.NO_ARGS);
                }
                return Ops.invoke(r, r.getObjectPrototype(), "toString", Js.NO_ARGS);
            }
        });
        realm.method(proto, "slice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                double k = relative(Js.arg(args, 0), len, 0);
                double fin = relative(Js.arg(args, 1), len, len);
                JsArray a = r.newArray();
                double n = 0;
                // 시작부터 끝 직전까지 존재하는 요소만 복사한다.
                for (; k < fin; k++, n++) {
                    String pk = key(k);
                    if (o.hasProperty(pk)) {
                        a.createDataPropertyOrThrow(key(n), o.get(pk));
                    }
                }
                a.setStrict("length", JsNumber.of(n));
                return a;
            }
        });
        realm.method(proto, "indexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    return JsNumber.of(-1);
                }
                double n = Ops.toInteger(Js.arg(args, 1));
                double k = n >= 0 ? n : Math.max(len + n, 0);
                // 엄격한 동등으로 처음 일치하는 위치를 찾는다.
                for (; k < len; k++) {
                    String pk = key(k);
                    if (o.hasProperty(pk) && Ops.strictEquals(o.get(pk), Js.arg(args, 0))) {
                        return JsNumber.of(k);
                    }
                }
                return JsNumber.of(-1);
            }
        });
        realm.method(proto, "forEach", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError(JsException.describe(cb) + " is not a function");
                }
                // 존재하는 요소마다 콜백을 호출한다.
                for (double k = 0; k < len; k++) {
                    String pk = key(k);
                    if (o.hasProperty(pk)) {
                        ((JsObject) cb).call(Js.arg(args, 1), new JsValue[] { o.get(pk), JsNumber.of(k), o });
                    }
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "map", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError(JsException.describe(cb) + " is not a function");
                }
                JsArray a = r.newArray();
                a.setStrict("length", JsNumber.of(len));
                // 존재하는 요소마다 콜백 결과를 같은 위치에 넣는다.
                for (double k = 0; k < len; k++) {
                    String pk = key(k);
                    if (o.hasProperty(pk)) {
                        JsValue v = ((JsObject) cb).call(Js.arg(args, 1), new JsValue[] { o.get(pk), JsNumber.of(k), o });
                        a.createDataPropertyOrThrow(pk, v);
                    }
                }
                return a;
            }
        });
    }

    /**
     * Installs <code>values</code>, <code>keys</code>, <code>entries</code> and <code>[Symbol.iterator]</code>.
     * <p>
     * <code>values</code>, <code>keys</code>, <code>entries</code>, <code>[Symbol.iterator]</code> 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private void installIteration(JsRealm realm, JsObject proto) {
        NativeFunction values = realm.method(proto, "values", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return IteratorBuiltins.createArrayIterator(r, Ops.toObject(r, thisValue), IteratorBuiltins.VALUES);
            }
        });
        realm.method(proto, "keys", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return IteratorBuiltins.createArrayIterator(r, Ops.toObject(r, thisValue), IteratorBuiltins.KEYS);
            }
        });
        realm.method(proto, "entries", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return IteratorBuiltins.createArrayIterator(r, Ops.toObject(r, thisValue), IteratorBuiltins.ENTRIES);
            }
        });
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, values);
        realm.setIntrinsic("%ArrayProto_values%", values);
    }

}
