package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The Array constructor and its static functions (ECMA-262 6th edition 22.1.1 and 22.1.2): <code>Array(...)</code> in call and construct form,
 * <code>Array.from</code>, <code>Array.of</code>, <code>Array.isArray</code> and the <code>@@species</code> getter, plus ArraySpeciesCreate
 * (9.4.2.3) that the prototype methods use. The array exotic object itself (<code>length</code> semantics, sparse storage) is the core
 * <code>JsArray</code>.
 * <p>
 * Array 생성자와 정적 함수(ECMA-262 6판 22.1.1, 22.1.2): 호출/생성 두 형태의 <code>Array(...)</code>, <code>Array.from</code>,
 * <code>Array.of</code>, <code>Array.isArray</code>, <code>@@species</code> getter 와, 프로토타입 메서드가 쓰는 ArraySpeciesCreate(9.4.2.3).
 * 배열 이국적 객체 자체(<code>length</code> 의미, 희소 저장)는 코어의 <code>JsArray</code> 이다.
 */
final class ArrayBuiltins {

    /**
     * Name of the intrinsic that holds the Array constructor.
     * <p>
     * Array 생성자를 담는 내장 객체 이름.
     */
    static final String ARRAY_NAME = "%Array%";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ArrayBuiltins() {
    }

    /**
     * Installs the Array constructor, replacing a minimal constructor of the core if there is one, and wires it to Array.prototype.
     * <p>
     * Array 생성자를 설치한다. 코어의 최소 생성자가 있으면 교체하며 Array.prototype 과 연결한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = realm.getArrayPrototype();
        final NativeFunction[] self = new NativeFunction[1];
        NativeFunction array = new NativeFunction(realm, "Array", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return construct(r, args, self[0]);
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return ArrayBuiltins.construct(r, args, newTarget);
            }
        });
        self[0] = array;
        array.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", array);
        realm.getGlobalObject().defineBuiltin("Array", array);
        realm.setIntrinsic(ARRAY_NAME, array);
        Support.def(realm, array, "isArray", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Ops.isArray(Js.arg(args, 0)));
            }
        });
        Support.def(realm, array, "of", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return of(r, thisValue, args);
            }
        });
        Support.def(realm, array, "from", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return from(r, thisValue, args);
            }
        });
        realm.getter(array, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
    }

    /**
     * The Array constructor behavior (22.1.1.1 to 22.1.1.3): no argument, one argument (a length when it is a number, otherwise the only
     * element) or several elements.
     * <p>
     * Array 생성자 동작(22.1.1.1~22.1.1.3): 인수 없음, 인수 하나(숫자이면 길이, 아니면 유일한 요소), 여러 요소.
     *
     * @param realm     the realm / realm
     * @param args      the arguments / 인수
     * @param newTarget the constructor that <code>new</code> was applied to / <code>new</code> 가 적용된 생성자
     * @return the new array / 새 배열
     */
    private static JsObject construct(JsRealm realm, JsValue[] args, JsObject newTarget) {
        JsObject proto = Ops.getPrototypeFromConstructor(realm, newTarget, realm.getArrayPrototype());
        JsArray a = new JsArray(proto);
        if (args.length == 0) {
            return a;
        }
        if (args.length == 1) {
            JsValue len = args[0];
            if (!(len instanceof JsNumber)) {
                a.add(len);
                return a;
            }
            double d = ((JsNumber) len).getValue();
            long intLen = Ops.toUint32(d);
            if (intLen != d) {
                throw JsException.rangeError("Invalid array length");
            }
            a.defineOwnProperty("length", PropertyDescriptor.valueOnly(Js.num(intLen)));
            return a;
        }
        // 인수가 둘 이상이면 모두 요소가 된다.
        for (int i = 0; i < args.length; i++) {
            a.add(args[i]);
        }
        return a;
    }

    /**
     * Array.of (22.1.2.3).
     * <p>
     * Array.of(22.1.2.3).
     *
     * @param realm     the realm / realm
     * @param thisValue the constructor / 생성자
     * @param items     the elements / 요소
     * @return the new array / 새 배열
     */
    private static JsValue of(JsRealm realm, JsValue thisValue, JsValue[] items) {
        JsObject a = Ops.isConstructor(thisValue) ? Ops.construct(thisValue, new JsValue[] { Js.num(items.length) })
                : Support.arrayCreate(realm, items.length);
        // 인수를 차례로 새 배열의 요소로 만든다.
        for (int k = 0; k < items.length; k++) {
            Support.createAt(a, k, items[k]);
        }
        Support.setLength(a, items.length);
        return a;
    }

    /**
     * Array.from (22.1.2.1): builds an array from an iterable or an array-like, optionally mapping each element.
     * <p>
     * Array.from(22.1.2.1): 이터러블이나 유사 배열에서 배열을 만들며 요소마다 매핑 함수를 적용할 수 있다.
     *
     * @param realm     the realm / realm
     * @param thisValue the constructor / 생성자
     * @param args      items, mapFn, thisArg / items, mapFn, thisArg
     * @return the new array / 새 배열
     */
    private static JsValue from(JsRealm realm, JsValue thisValue, JsValue[] args) {
        JsValue items = Js.arg(args, 0);
        JsValue mapFn = Js.arg(args, 1);
        JsValue thisArg = Js.arg(args, 2);
        boolean mapping = !mapFn.isUndefined();
        if (mapping) {
            Support.requireCallable(mapFn, "Array.from: when provided, the second argument");
        }
        JsValue usingIterator = Ops.getMethod(realm, items, WellKnownSymbols.ITERATOR);
        if (!usingIterator.isUndefined()) {
            JsObject a = Ops.isConstructor(thisValue) ? Ops.construct(thisValue, Js.NO_ARGS) : Support.arrayCreate(realm, 0);
            JsObject iterator = IteratorHelper.getIteratorFromMethod(items, usingIterator);
            double k = 0;
            // 이터레이터가 끝날 때까지 값을 꺼내 요소로 만든다. 도중에 예외가 나면 이터레이터를 닫는다.
            while (true) {
                JsObject next = IteratorHelper.step(iterator);
                if (next == null) {
                    Support.setLength(a, k);
                    return a;
                }
                JsValue value = IteratorHelper.value(next);
                try {
                    JsValue mapped = mapping ? Ops.call(mapFn, thisArg, new JsValue[] { value, Js.num(k) }) : value;
                    Support.createAt(a, k, mapped);
                } catch (RuntimeException e) {
                    IteratorHelper.closeQuietly(iterator);
                    throw e;
                }
                k++;
                Support.poll(k);
            }
        }
        JsObject arrayLike = Ops.toObject(realm, items);
        double len = Ops.lengthOf(arrayLike);
        JsObject a = Ops.isConstructor(thisValue) ? Ops.construct(thisValue, new JsValue[] { Js.num(len) }) : Support.arrayCreate(realm, len);
        // 유사 배열의 0..len-1 요소를 읽어 요소로 만든다.
        for (double k = 0; k < len; k++) {
            JsValue value = Support.getAt(arrayLike, k);
            JsValue mapped = mapping ? Ops.call(mapFn, thisArg, new JsValue[] { value, Js.num(k) }) : value;
            Support.createAt(a, k, mapped);
            Support.poll(k);
        }
        Support.setLength(a, len);
        return a;
    }

    /**
     * ArraySpeciesCreate (9.4.2.3): creates the array that a method such as map or slice returns, using the constructor named by the
     * <code>@@species</code> of the original array's constructor.
     * <p>
     * ArraySpeciesCreate(9.4.2.3): map, slice 같은 메서드가 돌려주는 배열을 원래 배열의 생성자의 <code>@@species</code> 가 가리키는 생성자로 만든다.
     *
     * @param realm    the realm / realm
     * @param original the array the method was called on / 메서드를 호출한 대상
     * @param length   the length of the new array / 새 배열의 길이
     * @return the new array-like object / 새 배열류 객체
     */
    static JsObject speciesCreate(JsRealm realm, JsObject original, double length) {
        if (!Ops.isArray(original)) {
            return Support.arrayCreate(realm, length);
        }
        JsValue c = original.get("constructor");
        if (c instanceof JsObject) {
            c = ((JsObject) c).get(WellKnownSymbols.SPECIES);
            if (c.isNull()) {
                c = Js.UNDEFINED;
            }
        }
        if (c.isUndefined() || c == realm.getIntrinsic(ARRAY_NAME)) {
            return Support.arrayCreate(realm, length);
        }
        if (!Ops.isConstructor(c)) {
            throw JsException.typeError("object.constructor[Symbol.species] is not a constructor");
        }
        return Ops.construct(c, new JsValue[] { Js.num(length) });
    }
}
