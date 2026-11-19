package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.num.NumberConversion;
import org.duckdns.hjow.swingwebpanel.js.text.UriCodec;
import org.duckdns.hjow.swingwebpanel.js.text.UriFormatException;

/**
 * Installs the function and value properties of the global object (ECMA-262 6th edition 18): <code>NaN</code>, <code>Infinity</code>,
 * <code>undefined</code>, <code>parseInt</code>, <code>parseFloat</code>, <code>isNaN</code>, <code>isFinite</code>,
 * <code>decodeURI</code>, <code>decodeURIComponent</code>, <code>encodeURI</code>, <code>encodeURIComponent</code>. The text work is done
 * by the stand-alone libraries <code>js.num</code> and <code>js.text</code>; <code>eval</code> belongs to the engine core and
 * <code>escape</code>/<code>unescape</code> (Annex B) to the meta library. The functions are created once and remembered so that
 * <code>Number.parseInt</code> and <code>Number.parseFloat</code> can be the identical function objects.
 * <p>
 * 전역 객체의 함수와 값 프로퍼티(ECMA-262 6판 18)를 설치한다: <code>NaN</code>, <code>Infinity</code>, <code>undefined</code>,
 * <code>parseInt</code>, <code>parseFloat</code>, <code>isNaN</code>, <code>isFinite</code>, <code>decodeURI</code>,
 * <code>decodeURIComponent</code>, <code>encodeURI</code>, <code>encodeURIComponent</code>. 문자열 처리는 독립 라이브러리
 * <code>js.num</code> 과 <code>js.text</code> 가 맡고, <code>eval</code> 은 엔진 코어, <code>escape</code>/<code>unescape</code>(부속서 B)는
 * 메타 라이브러리의 몫이다. <code>Number.parseInt</code> 와 <code>Number.parseFloat</code> 이 같은 함수 객체가 될 수 있도록 함수를 한 번만
 * 만들어 기억해 둔다.
 */
final class GlobalLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private GlobalLib() {
    }

    /**
     * Installs the global properties and returns the parseInt and parseFloat function objects.
     * <p>
     * 전역 프로퍼티를 설치하고 parseInt 와 parseFloat 함수 객체를 돌려준다.
     *
     * @param realm the realm / realm
     * @return {parseFloat, parseInt} / {parseFloat, parseInt}
     */
    static JsObject[] install(JsRealm realm) {
        JsObject g = realm.getGlobalObject();
        g.defineData("NaN", JsNumber.NAN, false, false, false);
        g.defineData("Infinity", Js.num(Double.POSITIVE_INFINITY), false, false, false);
        g.defineData("undefined", Js.UNDEFINED, false, false, false);
        NativeFunction parseFloatFn = realm.method(g, "parseFloat", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(NumberConversion.parseFloat(Ops.toString(Js.arg(args, 0))));
            }
        });
        NativeFunction parseIntFn = realm.method(g, "parseInt", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = Ops.toString(Js.arg(args, 0));
                int radix = Ops.toInt32(Js.arg(args, 1));
                return Js.num(NumberConversion.parseInt(s, radix));
            }
        });
        realm.method(g, "isNaN", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double d = Ops.toNumber(Js.arg(args, 0));
                return Js.bool(d != d);
            }
        });
        realm.method(g, "isFinite", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double d = Ops.toNumber(Js.arg(args, 0));
                return Js.bool(d == d && !Double.isInfinite(d));
            }
        });
        realm.method(g, "decodeURI", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    return Js.str(UriCodec.decodeURI(Ops.toString(Js.arg(args, 0))));
                } catch (UriFormatException e) {
                    throw JsException.uriError(e.getMessage());
                }
            }
        });
        realm.method(g, "decodeURIComponent", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    return Js.str(UriCodec.decodeURIComponent(Ops.toString(Js.arg(args, 0))));
                } catch (UriFormatException e) {
                    throw JsException.uriError(e.getMessage());
                }
            }
        });
        realm.method(g, "encodeURI", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    return Js.str(UriCodec.encodeURI(Ops.toString(Js.arg(args, 0))));
                } catch (UriFormatException e) {
                    throw JsException.uriError(e.getMessage());
                }
            }
        });
        realm.method(g, "encodeURIComponent", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    return Js.str(UriCodec.encodeURIComponent(Ops.toString(Js.arg(args, 0))));
                } catch (UriFormatException e) {
                    throw JsException.uriError(e.getMessage());
                }
            }
        });
        return new JsObject[] { parseFloatFn, parseIntFn };
    }
}
