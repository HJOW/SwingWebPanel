package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;

/**
 * Small helpers shared by the installers of this package: creating constructor functions with their standard wiring (the
 * <code>prototype</code> and <code>constructor</code> properties, the global binding and the intrinsic registration) and checking
 * arguments.
 * <p>
 * 이 패키지의 설치기들이 공유하는 작은 도우미: 생성자 함수를 표준 연결(<code>prototype</code>, <code>constructor</code> 프로퍼티,
 * 전역 바인딩, 내장 객체 등록)과 함께 만들고 인수를 검사한다.
 */
final class LibSupport {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private LibSupport() {
    }

    /**
     * Creates a constructor function and wires it up: <code>F.prototype</code> (not writable, not enumerable, not configurable),
     * <code>prototype.constructor</code> (writable, not enumerable, configurable), the global property <code>name</code> and the
     * intrinsic <code>%name%</code>. An existing global binding or intrinsic of the same name is replaced.
     * <p>
     * 생성자 함수를 만들고 연결한다: <code>F.prototype</code>(writable, 열거, configurable 모두 아님), <code>prototype.constructor</code>
     * (writable, 열거 아님, configurable), 전역 프로퍼티 <code>name</code>, 내장 객체 <code>%name%</code>.
     * 같은 이름의 기존 전역 바인딩과 내장 객체는 교체한다.
     *
     * @param realm     the realm / realm
     * @param name      constructor name / 생성자 이름
     * @param length    the "length" of the constructor / 생성자의 "length"
     * @param call      behaviour when called / 호출 동작
     * @param construct behaviour when constructed / 생성 동작
     * @param proto     the prototype object / 프로토타입 객체
     * @param fnProto   the [[Prototype]] of the constructor, or null for Function.prototype / 생성자의 [[Prototype]]. Function.prototype 이면 null
     * @return the constructor / 생성자
     */
    static NativeFunction defineConstructor(JsRealm realm, String name, int length, NativeFunction.Call call,
            NativeFunction.Construct construct, JsObject proto, JsObject fnProto) {
        NativeFunction ctor = new NativeFunction(realm, fnProto == null ? realm.getFunctionPrototype() : fnProto, name, length, call,
                construct);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin(name, ctor);
        realm.setIntrinsic("%" + name + "%", ctor);
        return ctor;
    }

    /**
     * Returns the value as an object or throws a TypeError.
     * <p>
     * 값을 객체로 돌려주고 객체가 아니면 TypeError 를 던진다.
     *
     * @param v       the value / 값
     * @param message the error message / 오류 메시지
     * @return the object / 객체
     */
    static JsObject requireObject(JsValue v, String message) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError(message);
        }
        return (JsObject) v;
    }

    /**
     * Describes a value for an error message.
     * <p>
     * 오류 메시지용으로 값을 설명한다.
     *
     * @param v the value / 값
     * @return text / 문자열
     */
    static String describe(JsValue v) {
        return JsException.describe(v);
    }
}
