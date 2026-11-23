package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The names of the interfaces of the objects that a page already has but that were plain objects: <code>Navigator</code>, <code>Location</code>,
 * <code>History</code>, <code>Screen</code>, <code>DOMStringMap</code> (the <code>dataset</code> of an element) and <code>MutationRecord</code> (WebIDL "interface
 * object": a global function with a <code>prototype</code> whose <code>constructor</code> is the function and whose <code>Symbol.toStringTag</code> is the name).
 * A page can read <code>navigator instanceof Navigator</code>, <code>Object.prototype.toString.call(history)</code> (<code>[object History]</code>) and
 * <code>Navigator.prototype</code>; calling or constructing the interface object throws <code>TypeError: Illegal constructor</code>, as in a browser. The existing
 * object keeps its own members: its prototype is the new interface prototype, which has none of them (a browser has them on the prototype; the differences are visible
 * only to code that inspects own properties).
 * <p>
 * 이미 있지만 보통 객체였던 객체의 인터페이스 이름: <code>Navigator</code>, <code>Location</code>, <code>History</code>, <code>Screen</code>,
 * <code>DOMStringMap</code>(요소의 <code>dataset</code>), <code>MutationRecord</code>(WebIDL "인터페이스 객체": <code>prototype</code> 을 가진 전역 함수이고 그 프로토타입의
 * <code>constructor</code> 는 그 함수이며 <code>Symbol.toStringTag</code> 는 이름이다). 페이지는 <code>navigator instanceof Navigator</code>,
 * <code>Object.prototype.toString.call(history)</code>(<code>[object History]</code>), <code>Navigator.prototype</code> 을 읽을 수 있고, 인터페이스 객체를 부르거나
 * 만들면 브라우저처럼 <code>TypeError: Illegal constructor</code> 를 던진다. 기존 객체는 자기 멤버를 그대로 가진다: 그 프로토타입은 새 인터페이스 프로토타입이며
 * 멤버가 없다(브라우저는 멤버가 프로토타입에 있다. 차이는 자기 프로퍼티를 살피는 코드에만 보인다).
 */
final class InterfaceNames {

    /**
     * Not instantiable: a static helper.
     * <p>
     * 인스턴스를 만들 수 없다: 정적 도우미.
     */
    private InterfaceNames() {
    }

    /**
     * Defines the interface object <code>name</code> on the global object and returns its prototype.
     * <p>
     * 전역 객체에 인터페이스 객체 <code>name</code> 을 정의하고 그 프로토타입을 반환한다.
     *
     * @param page the page / 페이지
     * @param g    the global object / 전역 객체
     * @param name the interface name / 인터페이스 이름
     * @return the prototype / 프로토타입
     */
    static JsObject define(PageScripts page, JsObject g, String name) {
        JsRealm realm = page.getRealm();
        JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, name, 0, new NativeFunction.Call() {
            /**
             * An interface object that has no constructor cannot be called.
             * <p>
             * 생성자가 없는 인터페이스 객체는 함수로 부를 수 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Illegal constructor");
            }
        }, new NativeFunction.Construct() {
            /**
             * An interface object that has no constructor cannot be constructed.
             * <p>
             * 생성자가 없는 인터페이스 객체는 만들 수 없다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return never / 반환하지 않음
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("Illegal constructor");
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(Js.str(name), false, false, true));
        g.defineBuiltin(name, ctor);
        return proto;
    }

    /**
     * Defines the interface object <code>name</code> and makes it the interface of an object that the page already has (the prototype of the object becomes the
     * prototype of the interface).
     * <p>
     * 인터페이스 객체 <code>name</code> 을 정의하고 페이지에 이미 있는 객체의 인터페이스로 삼는다(객체의 프로토타입이 인터페이스의 프로토타입이 된다).
     *
     * @param page   the page / 페이지
     * @param g      the global object / 전역 객체
     * @param name   the interface name / 인터페이스 이름
     * @param target the object that has the interface, or a non-object (then nothing is adopted) / 인터페이스를 가진 객체. 객체가 아니면 아무것도 하지 않는다
     */
    static void adopt(PageScripts page, JsObject g, String name, JsValue target) {
        JsObject proto = define(page, g, name);
        if (target instanceof JsObject) {
            ((JsObject) target).setPrototypeOf(proto);
        }
    }
}
