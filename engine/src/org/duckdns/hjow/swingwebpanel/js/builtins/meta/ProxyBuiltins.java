package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;

/**
 * Installs the Proxy constructor and <code>Proxy.revocable</code> (ECMA-262 6th edition 26.2). The constructor has length 2, can only
 * be called with <code>new</code> and has no <code>prototype</code> property.
 * <p>
 * Proxy 생성자와 <code>Proxy.revocable</code> 을 설치한다(ECMA-262 6판 26.2). 생성자의 length 는 2 이고 <code>new</code> 로만 호출할 수
 * 있으며 <code>prototype</code> 프로퍼티가 없다.
 */
final class ProxyBuiltins {

    /**
     * Name of the intrinsic that holds the Proxy constructor.
     * <p>
     * Proxy 생성자를 담는 내장 객체 이름.
     */
    static final String INTRINSIC_NAME = "%Proxy%";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ProxyBuiltins() {
    }

    /**
     * ProxyCreate (9.5.15): both arguments must be objects.
     * <p>
     * ProxyCreate (9.5.15): 두 인수는 모두 객체여야 한다.
     *
     * @param realm   the realm / realm
     * @param target  target argument / target 인수
     * @param handler handler argument / handler 인수
     * @return the new proxy / 새 프록시
     */
    static ProxyObject create(JsRealm realm, JsValue target, JsValue handler) {
        if (!(target instanceof JsObject) || !(handler instanceof JsObject)) {
            throw JsException.typeError("Cannot create proxy with a non-object as target or handler");
        }
        return new ProxyObject(realm, (JsObject) target, (JsObject) handler);
    }

    /**
     * Creates the constructor and defines it on the global object.
     * <p>
     * 생성자를 만들어 전역 객체에 정의한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        NativeFunction ctor = new NativeFunction(realm, "Proxy", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Constructor Proxy requires 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return create(r, Js.arg(args, 0), Js.arg(args, 1));
            }
        });
        // Proxy.revocable(target, handler): { proxy, revoke } (26.2.2.1)
        realm.method(ctor, "revocable", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                final ProxyObject proxy = create(r, Js.arg(args, 0), Js.arg(args, 1));
                NativeFunction revoke = new NativeFunction(r, "", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm r2, JsValue t, JsValue[] a) {
                        proxy.revoke();
                        return Js.UNDEFINED;
                    }
                });
                JsObject result = r.newObject();
                result.createDataProperty("proxy", proxy);
                result.createDataProperty("revoke", revoke);
                return result;
            }
        });
        realm.getGlobalObject().defineBuiltin("Proxy", ctor);
        realm.setIntrinsic(INTRINSIC_NAME, ctor);
    }
}
