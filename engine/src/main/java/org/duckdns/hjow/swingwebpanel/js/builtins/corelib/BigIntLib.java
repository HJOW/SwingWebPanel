package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.math.BigInteger;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Installs the BigInt function and BigInt.prototype (ECMA-262 2022).
 * <p>
 * BigInt 함수와 BigInt.prototype 을 설치한다(ECMA-262 2022).
 */
final class BigIntLib {

    /** The maximum width accepted by the two truncation helpers on Java's 32 bit shift API. / 두 잘라 내기 도우미가 Java 32비트 shift API 에서 받는 최대 폭. */
    private static final long MAX_WIDTH = Integer.MAX_VALUE;

    /** Prevents construction. / 생성을 막는다. */
    private BigIntLib() {
    }

    /**
     * Installs BigInt into a realm.
     * <p>
     * realm 에 BigInt 를 설치한다.
     *
     * @param realm realm / realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = realm.getBigIntPrototype();
        NativeFunction ctor = LibSupport.defineConstructor(realm, "BigInt", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return args.length == 0 ? JsBigInt.ZERO : JsBigInt.of(Ops.bigIntFunctionValue(args[0]));
            }
        }, null, proto, null);
        realm.setIntrinsic("%BigInt.prototype%", proto);

        realm.method(ctor, "asIntN", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                int bits = width(Js.arg(args, 0));
                return JsBigInt.of(asSigned(bits, Ops.toBigInt(Js.arg(args, 1))));
            }
        });
        realm.method(ctor, "asUintN", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                int bits = width(Js.arg(args, 0));
                return JsBigInt.of(asUnsigned(bits, Ops.toBigInt(Js.arg(args, 1))));
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue radixValue = Js.arg(args, 0);
                int radix = radixValue.isUndefined() ? 10 : (int) Ops.toInteger(radixValue);
                if (radix < 2 || radix > 36) {
                    throw JsException.rangeError("toString() radix argument must be between 2 and 36");
                }
                return Js.str(thisBigIntValue(thisValue).toString(radix));
            }
        });
        realm.method(proto, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(thisBigIntValue(thisValue).toString());
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return JsBigInt.of(thisBigIntValue(thisValue));
            }
        });
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("BigInt"), false, false, true);
    }

    /**
     * Converts a width argument to an integer supported by the Java BigInteger shifts.
     * <p>
     * 폭 인수를 Java BigInteger 이동이 지원하는 정수로 바꾼다.
     */
    private static int width(JsValue value) {
        double n = Ops.toInteger(value);
        if (n < 0 || n > MAX_WIDTH || n != Math.floor(n)) {
            throw JsException.rangeError("Invalid BigInt width");
        }
        return (int) n;
    }

    /**
     * Applies unsigned modulo 2^width, including width zero.
     * <p>
     * 폭이 0 인 경우를 포함해 2^width 로 부호 없는 나머지를 구한다.
     */
    private static BigInteger asUnsigned(int bits, BigInteger value) {
        if (bits == 0) {
            return BigInteger.ZERO;
        }
        return value.mod(BigInteger.ONE.shiftLeft(bits));
    }

    /**
     * Applies signed modulo 2^width.
     * <p>
     * 2^width 기준의 부호 있는 나머지를 구한다.
     */
    private static BigInteger asSigned(int bits, BigInteger value) {
        if (bits == 0) {
            return BigInteger.ZERO;
        }
        BigInteger modulo = BigInteger.ONE.shiftLeft(bits);
        BigInteger unsigned = value.mod(modulo);
        BigInteger sign = BigInteger.ONE.shiftLeft(bits - 1);
        return unsigned.compareTo(sign) >= 0 ? unsigned.subtract(modulo) : unsigned;
    }

    /**
     * ThisBigIntValue.
     * <p>
     * ThisBigIntValue: BigInt 원시 값 또는 래퍼를 정확한 정수로 반환한다.
     *
     * @param value the receiver / 수신 값
     * @return the integer / 정수
     */
    private static BigInteger thisBigIntValue(JsValue value) {
        if (value instanceof JsBigInt) {
            return ((JsBigInt) value).getValue();
        }
        if (value instanceof JsPrimitiveObject && ((JsPrimitiveObject) value).getPrimitive() instanceof JsBigInt) {
            return ((JsBigInt) ((JsPrimitiveObject) value).getPrimitive()).getValue();
        }
        throw JsException.typeError("BigInt.prototype method called on incompatible receiver");
    }
}
