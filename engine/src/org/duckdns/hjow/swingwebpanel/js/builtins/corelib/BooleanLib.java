package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Installs the <code>Boolean</code> constructor and <code>Boolean.prototype</code> (ECMA-262 6th edition 19.3): conversion when called,
 * wrapper objects when constructed, and the methods <code>toString</code> and <code>valueOf</code>.
 * <p>
 * <code>Boolean</code> 생성자와 <code>Boolean.prototype</code>(ECMA-262 6판 19.3)을 설치한다: 호출하면 변환, 생성하면 래퍼 객체,
 * 메서드 <code>toString</code> 과 <code>valueOf</code>.
 */
final class BooleanLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BooleanLib() {
    }

    /**
     * Installs Boolean into the realm.
     * <p>
     * realm 에 Boolean 을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject proto = realm.getBooleanPrototype();
        LibSupport.defineConstructor(realm, "Boolean", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Ops.toBoolean(Js.arg(args, 0)));
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsBoolean b = Js.bool(Ops.toBoolean(Js.arg(args, 0)));
                return new JsPrimitiveObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getBooleanPrototype()), b, "Boolean");
            }
        }, proto, null);
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(thisBooleanValue(thisValue) ? "true" : "false");
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisBooleanValue(thisValue));
            }
        });
    }

    /**
     * thisBooleanValue (19.3.3): the Boolean value of a Boolean primitive or wrapper; anything else is a TypeError.
     * <p>
     * thisBooleanValue (19.3.3): Boolean 원시 값 또는 래퍼의 불리언 값. 그 밖의 값은 TypeError.
     *
     * @param v the receiver / 수신 값
     * @return the boolean / 불리언
     */
    private static boolean thisBooleanValue(JsValue v) {
        if (v instanceof JsBoolean) {
            return ((JsBoolean) v).getValue();
        }
        if (v instanceof JsPrimitiveObject && ((JsPrimitiveObject) v).getPrimitive() instanceof JsBoolean) {
            return ((JsBoolean) ((JsPrimitiveObject) v).getPrimitive()).getValue();
        }
        throw JsException.typeError("Boolean.prototype method called on incompatible receiver " + LibSupport.describe(v));
    }
}
