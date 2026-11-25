package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

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
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Installs the <code>Symbol</code> function and <code>Symbol.prototype</code> (ECMA-262 6th edition 19.4): the callable-only constructor
 * (calling it with <code>new</code> is a TypeError), the global symbol registry (<code>Symbol.for</code>, <code>Symbol.keyFor</code>),
 * the eleven well-known symbols as read-only properties, and the prototype methods.
 * <p>
 * <code>Symbol</code> 함수와 <code>Symbol.prototype</code>(ECMA-262 6판 19.4)을 설치한다: 호출만 가능한 생성자(<code>new</code> 로 호출하면
 * TypeError), 전역 심볼 레지스트리(<code>Symbol.for</code>, <code>Symbol.keyFor</code>), 읽기 전용 프로퍼티인 11개의 잘 알려진 심볼,
 * 프로토타입 메서드.
 */
final class SymbolLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SymbolLib() {
    }

    /**
     * Installs Symbol into the realm.
     * <p>
     * realm 에 Symbol 을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject proto = realm.getSymbolPrototype();
        NativeFunction ctor = LibSupport.defineConstructor(realm, "Symbol", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue d = Js.arg(args, 0);
                return new JsSymbol(d.isUndefined() ? null : Ops.toString(d));
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("Symbol is not a constructor");
            }
        }, proto, null);
        realm.method(ctor, "for", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return r.symbolFor(Ops.toString(Js.arg(args, 0)));
            }
        });
        realm.method(ctor, "keyFor", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue s = Js.arg(args, 0);
                if (!(s instanceof JsSymbol)) {
                    throw JsException.typeError(LibSupport.describe(s) + " is not a symbol");
                }
                String key = ((JsSymbol) s).getRegistryKey();
                return key == null ? Js.UNDEFINED : Js.str(key);
            }
        });
        defineWellKnown(ctor, "hasInstance", WellKnownSymbols.HAS_INSTANCE);
        defineWellKnown(ctor, "isConcatSpreadable", WellKnownSymbols.IS_CONCAT_SPREADABLE);
        defineWellKnown(ctor, "iterator", WellKnownSymbols.ITERATOR);
        defineWellKnown(ctor, "asyncIterator", WellKnownSymbols.ASYNC_ITERATOR);
        defineWellKnown(ctor, "match", WellKnownSymbols.MATCH);
        defineWellKnown(ctor, "matchAll", WellKnownSymbols.MATCH_ALL);
        defineWellKnown(ctor, "replace", WellKnownSymbols.REPLACE);
        defineWellKnown(ctor, "search", WellKnownSymbols.SEARCH);
        defineWellKnown(ctor, "species", WellKnownSymbols.SPECIES);
        defineWellKnown(ctor, "split", WellKnownSymbols.SPLIT);
        defineWellKnown(ctor, "toPrimitive", WellKnownSymbols.TO_PRIMITIVE);
        defineWellKnown(ctor, "toStringTag", WellKnownSymbols.TO_STRING_TAG);
        defineWellKnown(ctor, "unscopables", WellKnownSymbols.UNSCOPABLES);

        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(thisSymbolValue(thisValue).toString());
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisSymbolValue(thisValue);
            }
        });
        realm.getter(proto, "description", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String description = thisSymbolValue(thisValue).getDescription();
                return description == null ? Js.UNDEFINED : Js.str(description);
            }
        });
        NativeFunction toPrimitive = new NativeFunction(realm, "[Symbol.toPrimitive]", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisSymbolValue(thisValue);
            }
        });
        proto.defineData(WellKnownSymbols.TO_PRIMITIVE, toPrimitive, false, false, true);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(JsString.of("Symbol"), false, false, true));
    }

    /**
     * Defines a well-known symbol as a non-writable, non-enumerable, non-configurable property of the Symbol function.
     * <p>
     * 잘 알려진 심볼을 Symbol 함수의 writable, 열거, configurable 모두 아닌 프로퍼티로 정의한다.
     *
     * @param ctor   the Symbol function / Symbol 함수
     * @param name   the property name / 프로퍼티 이름
     * @param symbol the symbol / 심볼
     */
    private static void defineWellKnown(JsObject ctor, String name, JsSymbol symbol) {
        ctor.defineData(name, symbol, false, false, false);
    }

    /**
     * thisSymbolValue (19.4.3): the symbol of a Symbol primitive or wrapper; anything else is a TypeError.
     * <p>
     * thisSymbolValue (19.4.3): Symbol 원시 값 또는 래퍼의 심볼. 그 밖의 값은 TypeError.
     *
     * @param v the receiver / 수신 값
     * @return the symbol / 심볼
     */
    private static JsSymbol thisSymbolValue(JsValue v) {
        if (v instanceof JsSymbol) {
            return (JsSymbol) v;
        }
        if (v instanceof JsPrimitiveObject && ((JsPrimitiveObject) v).getPrimitive() instanceof JsSymbol) {
            return (JsSymbol) ((JsPrimitiveObject) v).getPrimitive();
        }
        throw JsException.typeError("Symbol.prototype method called on incompatible receiver " + LibSupport.describe(v));
    }
}
