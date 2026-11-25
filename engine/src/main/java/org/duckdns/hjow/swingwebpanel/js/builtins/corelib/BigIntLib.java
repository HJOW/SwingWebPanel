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
 * Installs BigInt and its prototype (ECMA-262, 17th edition, section 21.2).
 * <p>
 * BigInt와 그 프로토타입을 설치한다(ECMA-262 제17판 §21.2).
 */
final class BigIntLib {

    /** The largest width accepted by ToIndex. / ToIndex가 허용하는 최대 폭. */
    private static final long MAX_WIDTH = 9007199254740991L;

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
                return JsBigInt.of(Ops.bigIntFunctionValue(Js.arg(args, 0)));
            }
        }, new NativeFunction.Construct() {
            /**
             * Throws whenever BigInt is invoked with a NewTarget.
             * <p>
             * NewTarget을 지정해 BigInt를 호출하면 예외를 던진다.
             *
             * @param r the realm / realm
             * @param args the arguments / 인수
             * @param newTarget the new target / NewTarget
             * @return never / 반환하지 않음
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("BigInt is not a constructor");
            }
        }, proto, null);
        realm.setIntrinsic("%BigInt.prototype%", proto);

        realm.method(ctor, "asIntN", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                long bits = width(Js.arg(args, 0));
                return JsBigInt.of(asSigned(bits, Ops.toBigInt(Js.arg(args, 1))));
            }
        });
        realm.method(ctor, "asUintN", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                long bits = width(Js.arg(args, 0));
                return JsBigInt.of(asUnsigned(bits, Ops.toBigInt(Js.arg(args, 1))));
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                BigInteger value = thisBigIntValue(thisValue);
                JsValue radixValue = Js.arg(args, 0);
                int radix = radixValue.isUndefined() ? 10 : (int) Ops.toInteger(radixValue);
                if (radix < 2 || radix > 36) {
                    throw JsException.rangeError("toString() radix argument must be between 2 and 36");
                }
                return Js.str(value.toString(radix));
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
     * Converts the width argument using ToIndex.
     * <p>
     * 폭 인수에 ToIndex를 적용한다.
     */
    private static long width(JsValue value) {
        double n = Ops.toInteger(value);
        if (n < 0 || n > MAX_WIDTH) {
            throw JsException.rangeError("Invalid BigInt width");
        }
        return (long) n;
    }

    /**
     * Applies unsigned modulo 2^width, including width zero.
     * <p>
     * 폭이 0 인 경우를 포함해 2^width 로 부호 없는 나머지를 구한다.
     */
    private static BigInteger asUnsigned(long bits, BigInteger value) {
        if (bits == 0) {
            return BigInteger.ZERO;
        }
        // 이미 범위에 맞는 값은 폭이 커도 그대로 반환한다.
        if (value.signum() >= 0 && value.bitLength() <= bits) {
            return value;
        }
        if (bits >= Integer.MAX_VALUE) {
            throw JsException.rangeError("BigInt result exceeds the supported bit length");
        }
        return value.mod(BigInteger.ONE.shiftLeft((int) bits));
    }

    /**
     * Applies signed modulo 2^width.
     * <p>
     * 2^width 기준의 부호 있는 나머지를 구한다.
     */
    private static BigInteger asSigned(long bits, BigInteger value) {
        if (bits == 0) {
            return BigInteger.ZERO;
        }
        // 부호 비트를 포함해 이미 범위에 맞으면 마스크를 만들지 않는다.
        if (value.bitLength() < bits) {
            return value;
        }
        int width = (int) bits;
        BigInteger modulo = BigInteger.ONE.shiftLeft(width);
        BigInteger unsigned = value.mod(modulo);
        return unsigned.testBit(width - 1) ? unsigned.subtract(modulo) : unsigned;
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
