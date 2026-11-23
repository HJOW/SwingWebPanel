package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Small helpers shared by the built-in objects of this package: receiver checks, constructor wiring and common attribute patterns.
 * <p>
 * 이 패키지의 내장 객체들이 공유하는 작은 도우미: 수신 객체 검사, 생성자 연결, 자주 쓰는 속성 패턴.
 */
final class Support {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Support() {
    }

    /**
     * Requires an object as receiver.
     * <p>
     * 수신 객체가 객체여야 함을 확인한다.
     *
     * @param v      the this value / this 값
     * @param method name used in the error message / 오류 메시지에 쓸 이름
     * @return the object / 객체
     * @throws JsException TypeError if the value is not an object / 객체가 아니면 TypeError
     */
    static JsObject requireObject(JsValue v, String method) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError(method + " called on non-object");
        }
        return (JsObject) v;
    }

    /**
     * Creates a constructor function and wires it up with its prototype object, its global binding and the intrinsic registry:
     * <code>ctor.prototype</code> (not writable, not enumerable, not configurable), <code>proto.constructor</code>, the global property and
     * the intrinsics <code>%Name%</code> and <code>%NamePrototype%</code>.
     * <p>
     * 생성자 함수를 만들고 프로토타입 객체, 전역 바인딩, 내장 객체 등록부에 연결한다: <code>ctor.prototype</code>(쓰기/열거/구성 불가),
     * <code>proto.constructor</code>, 전역 프로퍼티, 내장 객체 <code>%Name%</code> 과 <code>%NamePrototype%</code>.
     *
     * @param realm     the realm / realm
     * @param name      constructor name / 생성자 이름
     * @param length    expected argument count / 기대 인수 개수
     * @param proto     the prototype object / 프로토타입 객체
     * @param call      behaviour when called as a function / 함수로 호출될 때의 동작
     * @param construct behaviour when constructed / 생성될 때의 동작
     * @return the constructor / 생성자
     */
    static NativeFunction makeConstructor(JsRealm realm, String name, int length, JsObject proto, NativeFunction.Call call,
            NativeFunction.Construct construct) {
        NativeFunction ctor = new NativeFunction(realm, name, length, call, construct);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin(name, ctor);
        realm.setIntrinsic("%" + name + "%", ctor);
        realm.setIntrinsic("%" + name + "Prototype%", proto);
        return ctor;
    }


    /**
     * SpeciesConstructor (ECMA-262 7.3.20): the constructor to use for derived objects of <code>o</code>.
     * <p>
     * SpeciesConstructor (ECMA-262 7.3.20): <code>o</code> 에서 파생되는 객체에 쓸 생성자.
     *
     * @param o           the object / 객체
     * @param defaultCtor the constructor used when no species is defined / species 가 정의되지 않았을 때 쓰는 생성자
     * @return the constructor / 생성자
     */
    static JsObject speciesConstructor(JsObject o, JsObject defaultCtor) {
        JsValue c = o.get("constructor");
        if (c.isUndefined()) {
            return defaultCtor;
        }
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("object.constructor is not an object");
        }
        JsValue s = ((JsObject) c).get(WellKnownSymbols.SPECIES);
        if (s.isNullOrUndefined()) {
            return defaultCtor;
        }
        if (Ops.isConstructor(s)) {
            return (JsObject) s;
        }
        throw JsException.typeError("object.constructor[Symbol.species] is not a constructor");
    }
    /**
     * Defines the <code>[Symbol.toStringTag]</code> property (not writable, not enumerable, configurable).
     * <p>
     * <code>[Symbol.toStringTag]</code> 프로퍼티를 정의한다(쓰기 불가, 열거 불가, 구성 가능).
     *
     * @param target the object / 객체
     * @param tag    the tag text / 태그 문자열
     */
    static void defineToStringTag(JsObject target, String tag) {
        target.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str(tag), false, false, true);
    }

    /**
     * Defines the <code>get [Symbol.species]</code> accessor of a constructor (returns this).
     * <p>
     * 생성자의 <code>get [Symbol.species]</code> 접근자를 정의한다(this 를 반환).
     *
     * @param realm the realm / realm
     * @param ctor  the constructor / 생성자
     */
    static void defineSpecies(JsRealm realm, NativeFunction ctor) {
        realm.getter(ctor, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            /**
             * Returns the receiver.
             * <p>
             * 수신 객체를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue receiver / 수신 객체
             * @param args      arguments / 인수
             * @return the receiver / 수신 객체
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
    }
}
