package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsStringObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.NumberText;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The minimal constructors and prototypes of the primitive wrappers Boolean, Number and String (ECMA-262 6th edition 19.3,
 * 20.1, 21.1): conversion calls, wrapper construction, <code>valueOf</code>, <code>toString</code> and the string iterator
 * <code>[Symbol.iterator]</code> that iterates code points. The rest of the String and Number libraries belongs to the full
 * built-in library installers.
 * <p>
 * 원시 값 래퍼 Boolean, Number, String 의 최소 생성자와 프로토타입(ECMA-262 6판 19.3, 20.1, 21.1): 변환 호출, 래퍼 생성,
 * <code>valueOf</code>, <code>toString</code>, 코드 포인트를 순회하는 문자열 이터레이터 <code>[Symbol.iterator]</code>.
 * String 과 Number 라이브러리의 나머지는 전체 내장 라이브러리 설치기의 몫이다.
 */
final class PrimitiveBuiltins implements BuiltinInstaller {

    /**
     * A String Iterator instance: yields the code points of a string.
     * <p>
     * 문자열 이터레이터 인스턴스: 문자열의 코드 포인트를 내보낸다.
     */
    private static final class StringIteratorObject extends JsObject {
        /**
         * The string.
         * <p>
         * 문자열.
         */
        final String text;
        /**
         * The next code unit index.
         * <p>
         * 다음 코드 단위 인덱스.
         */
        int pos;

        /**
         * Creates the iterator.
         * <p>
         * 이터레이터를 만든다.
         *
         * @param proto the prototype / 프로토타입
         * @param text  the string / 문자열
         */
        StringIteratorObject(JsObject proto, String text) {
            super(proto);
            this.text = text;
        }
    }

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    PrimitiveBuiltins() {
    }

    /**
     * Returns the primitive of a wrapper or primitive value of the expected kind.
     * <p>
     * 기대한 종류의 원시 값이나 래퍼에서 원시 값을 꺼낸다.
     *
     * @param v    the value / 값
     * @param tag  "Boolean" or "Number" / "Boolean" 또는 "Number"
     * @param type the Java class of the primitive / 원시 값의 Java 클래스
     * @return the primitive / 원시 값
     */
    private static JsValue thisPrimitive(JsValue v, String tag, Class<?> type) {
        if (type.isInstance(v)) {
            return v;
        }
        if (v instanceof JsPrimitiveObject && tag.equals(((JsPrimitiveObject) v).className())) {
            return ((JsPrimitiveObject) v).getPrimitive();
        }
        throw JsException.typeError(tag + ".prototype method called on incompatible receiver " + JsException.describe(v));
    }

    /**
     * Installs the three wrappers and the string iterator.
     * <p>
     * 세 가지 래퍼와 문자열 이터레이터를 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        installBoolean(realm);
        installNumber(realm);
        installString(realm);
    }

    /**
     * Installs Boolean.
     * <p>
     * Boolean 을 설치한다.
     *
     * @param realm the realm / realm
     */
    private void installBoolean(JsRealm realm) {
        JsObject proto = realm.getBooleanPrototype();
        NativeFunction ctor = new NativeFunction(realm, "Boolean", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsBoolean.of(Ops.toBoolean(Js.arg(args, 0)));
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return new JsPrimitiveObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getBooleanPrototype()),
                        JsBoolean.of(Ops.toBoolean(Js.arg(args, 0))), "Boolean");
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("Boolean", ctor);
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsString.of(thisPrimitive(thisValue, "Boolean", JsBoolean.class).toString());
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisPrimitive(thisValue, "Boolean", JsBoolean.class);
            }
        });
    }

    /**
     * Installs Number.
     * <p>
     * Number 를 설치한다.
     *
     * @param realm the realm / realm
     */
    private void installNumber(JsRealm realm) {
        JsObject proto = realm.getNumberPrototype();
        NativeFunction ctor = new NativeFunction(realm, "Number", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return args.length == 0 ? JsNumber.ZERO : JsNumber.of(Ops.toNumber(args[0]));
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue n = args.length == 0 ? JsNumber.ZERO : JsNumber.of(Ops.toNumber(args[0]));
                return new JsPrimitiveObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getNumberPrototype()), n, "Number");
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("Number", ctor);
        ctor.defineData("MAX_SAFE_INTEGER", JsNumber.of(9007199254740991.0), false, false, false);
        ctor.defineData("MIN_SAFE_INTEGER", JsNumber.of(-9007199254740991.0), false, false, false);
        ctor.defineData("EPSILON", JsNumber.of(Math.pow(2, -52)), false, false, false);
        ctor.defineData("MAX_VALUE", JsNumber.of(Double.MAX_VALUE), false, false, false);
        ctor.defineData("MIN_VALUE", JsNumber.of(Double.MIN_VALUE), false, false, false);
        ctor.defineData("NaN", JsNumber.NAN, false, false, false);
        ctor.defineData("POSITIVE_INFINITY", JsNumber.of(Double.POSITIVE_INFINITY), false, false, false);
        ctor.defineData("NEGATIVE_INFINITY", JsNumber.of(Double.NEGATIVE_INFINITY), false, false, false);
        realm.method(proto, "toString", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double d = ((JsNumber) thisPrimitive(thisValue, "Number", JsNumber.class)).getValue();
                JsValue radix = Js.arg(args, 0);
                int rd = radix.isUndefined() ? 10 : (int) Ops.toInteger(radix);
                if (rd < 2 || rd > 36) {
                    throw JsException.rangeError("toString() radix must be between 2 and 36");
                }
                return JsString.of(NumberText.toString(d, rd));
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisPrimitive(thisValue, "Number", JsNumber.class);
            }
        });
    }

    /**
     * Returns the string of a String primitive or wrapper.
     * <p>
     * String 원시 값이나 래퍼의 문자열을 반환한다.
     *
     * @param v the value / 값
     * @return the string / 문자열
     */
    private static String thisString(JsValue v) {
        if (v instanceof JsString) {
            return ((JsString) v).getValue();
        }
        if (v instanceof JsStringObject) {
            return ((JsStringObject) v).getStringValue();
        }
        throw JsException.typeError("String.prototype method called on incompatible receiver " + JsException.describe(v));
    }

    /**
     * Installs String and its iterator.
     * <p>
     * String 과 그 이터레이터를 설치한다.
     *
     * @param realm the realm / realm
     */
    private void installString(JsRealm realm) {
        JsObject proto = realm.getStringPrototype();
        NativeFunction ctor = new NativeFunction(realm, "String", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (args.length == 0) {
                    return JsString.EMPTY;
                }
                if (args[0] instanceof JsSymbol) {
                    return JsString.of(args[0].toString());
                }
                return JsString.of(Ops.toString(args[0]));
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                String s = args.length == 0 ? "" : Ops.toString(args[0]);
                return new JsStringObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getStringPrototype()), s);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("String", ctor);
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsString.of(thisString(thisValue));
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsString.of(thisString(thisValue));
            }
        });
        JsObject iterProto = new JsObject(realm.getIteratorPrototype());
        realm.setIntrinsic("%StringIteratorPrototype%", iterProto);
        realm.method(iterProto, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof StringIteratorObject)) {
                    throw JsException.typeError("next method called on incompatible receiver " + JsException.describe(thisValue));
                }
                StringIteratorObject it = (StringIteratorObject) thisValue;
                if (it.pos >= it.text.length()) {
                    return IteratorHelper.createResult(r, Js.UNDEFINED, true);
                }
                int cp = it.text.codePointAt(it.pos);
                int n = Character.charCount(cp);
                String piece = it.text.substring(it.pos, it.pos + n);
                it.pos += n;
                return IteratorHelper.createResult(r, JsString.of(piece), false);
            }
        });
        iterProto.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("String Iterator"), false, false, true);
        realm.method(proto, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Ops.requireObjectCoercible(thisValue);
                return new StringIteratorObject(r.getIntrinsic("%StringIteratorPrototype%"), Ops.toString(thisValue));
            }
        });
    }
}
