package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The Symbol constructor and Symbol.prototype (ECMA-262 6th edition 19.4): <code>Symbol()</code>, the global symbol registry
 * (<code>Symbol.for</code>, <code>Symbol.keyFor</code>), the well-known symbols and the prototype methods.
 * <p>
 * Symbol 생성자와 Symbol.prototype(ECMA-262 6판 19.4): <code>Symbol()</code>, 전역 심볼 레지스트리(<code>Symbol.for</code>,
 * <code>Symbol.keyFor</code>), 잘 알려진 심볼, 프로토타입 메서드.
 */
final class SymbolBuiltins implements BuiltinInstaller {

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    SymbolBuiltins() {
    }

    /**
     * Returns the symbol of a Symbol primitive or wrapper (thisSymbolValue).
     * <p>
     * Symbol 원시 값이나 래퍼의 심볼을 반환한다(thisSymbolValue).
     *
     * @param v the value / 값
     * @return the symbol / 심볼
     */
    static JsSymbol thisSymbol(JsValue v) {
        if (v instanceof JsSymbol) {
            return (JsSymbol) v;
        }
        if (v instanceof JsPrimitiveObject && ((JsPrimitiveObject) v).getPrimitive() instanceof JsSymbol) {
            return (JsSymbol) ((JsPrimitiveObject) v).getPrimitive();
        }
        throw JsException.typeError("Symbol.prototype method called on incompatible receiver");
    }

    /**
     * Installs Symbol.
     * <p>
     * Symbol 을 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        final JsObject proto = realm.getSymbolPrototype();
        NativeFunction ctor = new NativeFunction(realm, "Symbol", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue d = Js.arg(args, 0);
                return new JsSymbol(d.isUndefined() ? null : Ops.toString(d));
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("Symbol is not a constructor");
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("Symbol", ctor);
        realm.setIntrinsic("%Symbol%", ctor);
        JsSymbol[] wk = { WellKnownSymbols.HAS_INSTANCE, WellKnownSymbols.IS_CONCAT_SPREADABLE, WellKnownSymbols.ITERATOR,
            WellKnownSymbols.MATCH, WellKnownSymbols.REPLACE, WellKnownSymbols.SEARCH, WellKnownSymbols.SPECIES,
            WellKnownSymbols.SPLIT, WellKnownSymbols.TO_PRIMITIVE, WellKnownSymbols.TO_STRING_TAG, WellKnownSymbols.UNSCOPABLES };
        // 잘 알려진 심볼을 Symbol.hasInstance 같은 읽기 전용 프로퍼티로 노출한다.
        for (int i = 0; i < wk.length; i++) {
            String d = wk[i].getDescription();
            ctor.defineData(d.substring("Symbol.".length()), wk[i], false, false, false);
        }
        realm.method(ctor, "for", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return r.symbolFor(Ops.toString(Js.arg(args, 0)));
            }
        });
        realm.method(ctor, "keyFor", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue s = Js.arg(args, 0);
                if (!(s instanceof JsSymbol)) {
                    throw JsException.typeError(JsException.describe(s) + " is not a symbol");
                }
                String k = ((JsSymbol) s).getRegistryKey();
                return k == null ? Js.UNDEFINED : JsString.of(k);
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsString.of(thisSymbol(thisValue).toString());
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisSymbol(thisValue);
            }
        });
        NativeFunction toPrim = new NativeFunction(realm, "[Symbol.toPrimitive]", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisSymbol(thisValue);
            }
        });
        proto.defineData(WellKnownSymbols.TO_PRIMITIVE, toPrim, false, false, true);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("Symbol"), false, false, true);
    }
}
