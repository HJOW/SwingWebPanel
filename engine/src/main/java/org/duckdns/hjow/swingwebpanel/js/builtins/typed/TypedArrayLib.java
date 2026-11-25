package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

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
 * Installs the abstract <code>%TypedArray%</code> intrinsic (its static methods <code>from</code> and <code>of</code>, <code>[Symbol.species]</code>
 * and its prototype) and the eleven concrete typed array constructors (ECMA-262 2022, 23.2). <code>%TypedArray%</code> is not a global; it is
 * reachable as the prototype of the concrete constructors. Registers the intrinsics <code>%TypedArray%</code>, <code>%TypedArrayPrototype%</code>,
 * <code>%Int8Array%</code>, <code>%Int8ArrayPrototype%</code> and so on.
 * <p>
 * 추상 내장 객체 <code>%TypedArray%</code>(정적 메서드 <code>from</code>, <code>of</code>, <code>[Symbol.species]</code>, 프로토타입)와 열한 개 구체 형식화 배열
 * 생성자를 설치한다(ECMA-262 2022, 23.2). <code>%TypedArray%</code> 는 전역이 아니며 구체 생성자의 프로토타입으로 접근한다.
 * 내장 객체 <code>%TypedArray%</code>, <code>%TypedArrayPrototype%</code>, <code>%Int8Array%</code>, <code>%Int8ArrayPrototype%</code> 등을 등록한다.
 */
final class TypedArrayLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TypedArrayLib() {
    }

    /**
     * Installs %TypedArray% and the concrete constructors into a realm.
     * <p>
     * realm 에 %TypedArray% 와 구체 생성자를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        TypedArrayIterator.install(realm);
        JsObject proto = new JsObject(realm.getObjectPrototype());
        NativeFunction abstractCtor = new NativeFunction(realm, "TypedArray", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Abstract class TypedArray not directly constructable");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("Abstract class TypedArray not directly constructable");
            }
        });
        realm.setIntrinsic("%TypedArray%", abstractCtor);
        realm.setIntrinsic("%TypedArrayPrototype%", proto);
        abstractCtor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", abstractCtor);
        installStatics(realm, abstractCtor);
        TypedArrayMethods.install(realm, proto);
        ElementType[] types = ElementType.values();
        // 요소 형식마다 구체 생성자와 프로토타입을 만든다.
        for (int i = 0; i < types.length; i++) {
            installConcrete(realm, abstractCtor, proto, types[i]);
        }
    }

    /**
     * Installs <code>%TypedArray%.from</code>, <code>%TypedArray%.of</code> and <code>%TypedArray%[Symbol.species]</code>.
     * <p>
     * <code>%TypedArray%.from</code>, <code>%TypedArray%.of</code>, <code>%TypedArray%[Symbol.species]</code> 를 설치한다.
     *
     * @param realm the realm / realm
     * @param ctor  <code>%TypedArray%</code> / <code>%TypedArray%</code>
     */
    private static void installStatics(JsRealm realm, JsObject ctor) {
        realm.method(ctor, "from", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return from(r, thisValue, Js.arg(args, 0), Js.arg(args, 1), Js.arg(args, 2));
            }
        });
        realm.method(ctor, "of", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!Ops.isConstructor(thisValue)) {
                    throw JsException.typeError("this is not a constructor");
                }
                TypedArrayObject created = TypedSupport.create((JsObject) thisValue, new JsValue[] { Js.num(args.length) });
                // 받은 값을 차례로 새 배열에 저장한다.
                for (int k = 0; k < args.length; k++) {
                    created.setIndexed(k, args[k]);
                }
                return created;
            }
        });
        realm.getter(ctor, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
    }

    /**
     * %TypedArray%.from (22.2.2.1).
     * <p>
     * %TypedArray%.from (22.2.2.1).
     *
     * @param realm   the realm / realm
     * @param c       the this value (a constructor) / this 값(생성자)
     * @param source  the source / 원본
     * @param mapfn   optional mapping function / 선택적인 변환 함수
     * @param thisArg this value for the mapping function / 변환 함수의 this 값
     * @return the new typed array / 새 형식화 배열
     */
    private static JsValue from(JsRealm realm, JsValue c, JsValue source, JsValue mapfn, JsValue thisArg) {
        if (!Ops.isConstructor(c)) {
            throw JsException.typeError("this is not a constructor");
        }
        boolean mapping = !mapfn.isUndefined();
        JsObject map = mapping ? TypedSupport.callable(mapfn) : null;
        JsValue usingIterator = Ops.getMethod(realm, source, WellKnownSymbols.ITERATOR);
        if (!usingIterator.isUndefined()) {
            List<JsValue> values = TypedSupport.iterableToList(source, usingIterator);
            TypedArrayObject target = TypedSupport.create((JsObject) c, new JsValue[] { Js.num(values.size()) });
            // 값을 (필요하면 변환해서) 차례로 저장한다.
            for (int k = 0; k < values.size(); k++) {
                JsValue v = values.get(k);
                target.setIndexed(k, mapping ? Ops.call(map, thisArg, new JsValue[] { v, Js.num(k) }) : v);
            }
            return target;
        }
        JsObject arrayLike = Ops.toObject(realm, source);
        double len = Ops.lengthOf(arrayLike);
        TypedArrayObject target = TypedSupport.create((JsObject) c, new JsValue[] { Js.num(len) });
        // 유사 배열의 요소를 (필요하면 변환해서) 차례로 저장한다.
        for (int k = 0; k < (int) len; k++) {
            JsValue v = arrayLike.get(PropertyKeys.fromIndex(k));
            target.setIndexed(k, mapping ? Ops.call(map, thisArg, new JsValue[] { v, Js.num(k) }) : v);
        }
        return target;
    }

    /**
     * Creates one concrete typed array constructor with its prototype and registers it.
     * <p>
     * 구체 형식화 배열 생성자 하나를 프로토타입과 함께 만들어 등록한다.
     *
     * @param realm          the realm / realm
     * @param abstractCtor   <code>%TypedArray%</code> / <code>%TypedArray%</code>
     * @param abstractProto  <code>%TypedArray%.prototype</code> / <code>%TypedArray%.prototype</code>
     * @param type           the element type / 요소 형식
     */
    private static void installConcrete(JsRealm realm, JsObject abstractCtor, JsObject abstractProto, final ElementType type) {
        final String name = type.getConstructorName();
        JsObject proto = new JsObject(abstractProto);
        NativeFunction ctor = new NativeFunction(realm, abstractCtor, name, 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Constructor " + name + " requires 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return TypedArrayFactory.construct(r, type, args, newTarget);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        ctor.defineData("BYTES_PER_ELEMENT", Js.num(type.getSize()), false, false, false);
        proto.defineData("BYTES_PER_ELEMENT", Js.num(type.getSize()), false, false, false);
        realm.setIntrinsic("%" + name + "%", ctor);
        realm.setIntrinsic("%" + name + "Prototype%", proto);
        realm.getGlobalObject().defineBuiltin(name, ctor);
    }
}
