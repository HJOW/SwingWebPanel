package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.text.LegacyEscape;

/**
 * Installs the global functions <code>escape</code> and <code>unescape</code> of ECMA-262 6th edition Annex B.2.1. Both have length 1,
 * convert their argument to a string and work on UTF-16 code units: characters above 255 become <code>%uXXXX</code>, the characters
 * <code>A-Za-z0-9@*_+-./</code> stay as they are.
 * <p>
 * ECMA-262 6판 부속서 B.2.1 의 전역 함수 <code>escape</code>, <code>unescape</code> 를 설치한다. 둘 다 length 는 1 이고 인수를 문자열로
 * 바꾼 뒤 UTF-16 코드 단위로 동작한다. 255 를 넘는 문자는 <code>%uXXXX</code> 가 되고 <code>A-Za-z0-9@*_+-./</code> 는 그대로 둔다.
 */
final class LegacyBuiltins {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private LegacyBuiltins() {
    }

    /**
     * Defines the two functions on the global object.
     * <p>
     * 두 함수를 전역 객체에 정의한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        realm.method(realm.getGlobalObject(), "escape", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(LegacyEscape.escape(Ops.toString(Js.arg(args, 0))));
            }
        });
        realm.method(realm.getGlobalObject(), "unescape", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(LegacyEscape.unescape(Ops.toString(Js.arg(args, 0))));
            }
        });
    }
}
