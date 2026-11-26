package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.num.NumberConversion;
import org.duckdns.hjow.swingwebpanel.js.num.NumberRangeException;

/**
 * Installs the <code>Number</code> constructor and <code>Number.prototype</code> (ECMA-262 6th edition 20.1): the numeric constants,
 * the static predicates, the identical <code>parseInt</code>/<code>parseFloat</code> function objects, and the prototype methods
 * <code>toString(radix)</code>, <code>toFixed</code>, <code>toExponential</code>, <code>toPrecision</code>, <code>toLocaleString</code>
 * and <code>valueOf</code>. All number formatting is delegated to {@link NumberConversion}; its range errors become RangeError.
 * <code>toLocaleString</code> formats like the en-US locale (grouping with commas, at most three fraction digits).
 * <p>
 * <code>Number</code> 생성자와 <code>Number.prototype</code>(ECMA-262 6판 20.1)을 설치한다: 수치 상수, 정적 판별 함수, 같은 함수 객체인
 * <code>parseInt</code>/<code>parseFloat</code>, 프로토타입 메서드 <code>toString(radix)</code>, <code>toFixed</code>,
 * <code>toExponential</code>, <code>toPrecision</code>, <code>toLocaleString</code>, <code>valueOf</code>. 숫자 서식은 모두
 * {@link NumberConversion} 에 맡기며 그 범위 오류는 RangeError 가 된다. <code>toLocaleString</code> 은 en-US 형식(쉼표 묶음, 소수 최대 세 자리)이다.
 */
final class NumberLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private NumberLib() {
    }

    /**
     * Applies Number(value) conversion, including explicit BigInt-to-Number rounding (ECMA-262 17th edition 21.1.1.1).
     * <p>
     * BigInt를 Number로 명시적으로 반올림하는 Number(value) 변환을 적용한다(ECMA-262 제17판 21.1.1.1절).
     *
     * @param args arguments of Number / Number의 인수
     * @return the converted Number value / 변환한 Number 값
     */
    private static JsNumber numberArgument(JsValue[] args) {
        if (args.length == 0) {
            return Js.num(0.0);
        }
        // ToNumeric은 객체의 숫자 힌트를 한 번 적용하며 BigInt 값은 명시적으로 반올림한다.
        JsValue numeric = Ops.toNumeric(args[0]);
        return numeric instanceof JsBigInt ? Js.num(((JsBigInt) numeric).getValue().doubleValue()) : (JsNumber) numeric;
    }

    /**
     * Installs Number into the realm.
     * <p>
     * realm 에 Number 를 설치한다.
     *
     * @param realm   the realm / realm
     * @param parsers {parseFloat, parseInt} function objects of the global object / 전역 객체의 {parseFloat, parseInt} 함수 객체
     */
    static void install(JsRealm realm, JsObject[] parsers) {
        JsObject proto = realm.getNumberPrototype();
        NativeFunction ctor = LibSupport.defineConstructor(realm, "Number", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return numberArgument(args);
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsNumber value = numberArgument(args);
                return new JsPrimitiveObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getNumberPrototype()), value, "Number");
            }
        }, proto, null);
        ctor.defineData("EPSILON", Js.num(Math.pow(2, -52)), false, false, false);
        ctor.defineData("MAX_SAFE_INTEGER", Js.num(9007199254740991.0), false, false, false);
        ctor.defineData("MAX_VALUE", Js.num(Double.MAX_VALUE), false, false, false);
        ctor.defineData("MIN_SAFE_INTEGER", Js.num(-9007199254740991.0), false, false, false);
        ctor.defineData("MIN_VALUE", Js.num(Double.MIN_VALUE), false, false, false);
        ctor.defineData("NaN", JsNumber.NAN, false, false, false);
        ctor.defineData("NEGATIVE_INFINITY", Js.num(Double.NEGATIVE_INFINITY), false, false, false);
        ctor.defineData("POSITIVE_INFINITY", Js.num(Double.POSITIVE_INFINITY), false, false, false);
        realm.method(ctor, "isFinite", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(v instanceof JsNumber && !Double.isNaN(((JsNumber) v).getValue()) && !Double.isInfinite(((JsNumber) v).getValue()));
            }
        });
        realm.method(ctor, "isInteger", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(v instanceof JsNumber && NumberConversion.isInteger(((JsNumber) v).getValue()));
            }
        });
        realm.method(ctor, "isNaN", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(v instanceof JsNumber && Double.isNaN(((JsNumber) v).getValue()));
            }
        });
        realm.method(ctor, "isSafeInteger", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                return Js.bool(v instanceof JsNumber && NumberConversion.isSafeInteger(((JsNumber) v).getValue()));
            }
        });
        ctor.defineBuiltin("parseFloat", parsers[0]);
        ctor.defineBuiltin("parseInt", parsers[1]);

        realm.method(proto, "toExponential", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double x = thisNumberValue(thisValue);
                JsValue fd = Js.arg(args, 0);
                double f = Ops.toInteger(fd);
                try {
                    return Js.str(fd.isUndefined() ? NumberConversion.toExponential(x) : NumberConversion.toExponential(x, f));
                } catch (NumberRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
            }
        });
        realm.method(proto, "toFixed", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double x = thisNumberValue(thisValue);
                double f = Ops.toInteger(Js.arg(args, 0));
                try {
                    return Js.str(NumberConversion.toFixed(x, f));
                } catch (NumberRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
            }
        });
        realm.method(proto, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(toLocaleString(thisNumberValue(thisValue)));
            }
        });
        realm.method(proto, "toPrecision", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double x = thisNumberValue(thisValue);
                JsValue p = Js.arg(args, 0);
                if (p.isUndefined()) {
                    return Js.str(NumberConversion.toPrecision(x));
                }
                double digits = Ops.toInteger(p);
                try {
                    return Js.str(NumberConversion.toPrecision(x, digits));
                } catch (NumberRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
            }
        });
        realm.method(proto, "toString", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double x = thisNumberValue(thisValue);
                JsValue rv = Js.arg(args, 0);
                double radix = rv.isUndefined() ? 10 : Ops.toInteger(rv);
                try {
                    return Js.str(NumberConversion.toString(x, radix));
                } catch (NumberRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
            }
        });
        realm.method(proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisNumberValue(thisValue));
            }
        });
    }

    /**
     * thisNumberValue (20.1.3): the number of a Number primitive or wrapper; anything else is a TypeError.
     * <p>
     * thisNumberValue (20.1.3): Number 원시 값 또는 래퍼의 숫자. 그 밖의 값은 TypeError.
     *
     * @param v the receiver / 수신 값
     * @return the number / 숫자
     */
    private static double thisNumberValue(JsValue v) {
        if (v instanceof JsNumber) {
            return ((JsNumber) v).getValue();
        }
        if (v instanceof JsPrimitiveObject && ((JsPrimitiveObject) v).getPrimitive() instanceof JsNumber) {
            return ((JsNumber) ((JsPrimitiveObject) v).getPrimitive()).getValue();
        }
        throw JsException.typeError("Number.prototype method called on incompatible receiver " + LibSupport.describe(v));
    }

    /**
     * Formats a number like the en-US locale: NaN, the infinity sign, commas between groups of three integer digits, and at most three
     * fraction digits (rounded half away from zero, trailing zeros removed).
     * <p>
     * 숫자를 en-US 형식으로 만든다: NaN, 무한대 기호, 정수부 세 자리마다 쉼표, 소수 최대 세 자리(0 에서 먼 쪽으로 반올림, 끝의 0 제거).
     *
     * @param x the number / 숫자
     * @return the formatted text / 서식을 적용한 문자열
     */
    static String toLocaleString(double x) {
        if (Double.isNaN(x)) {
            return "NaN";
        }
        if (Double.isInfinite(x)) {
            return x > 0 ? "∞" : "-∞";
        }
        boolean negative = x < 0 || (x == 0 && 1 / x < 0);
        BigDecimal v = new BigDecimal(Math.abs(x)).setScale(3, RoundingMode.HALF_UP);
        // JRE 6 의 stripTrailingZeros 는 0 을 "0.000" 으로 남기므로 0 은 따로 처리한다.
        String plain = v.signum() == 0 ? "0" : v.stripTrailingZeros().toPlainString();
        int dot = plain.indexOf('.');
        String intPart = dot < 0 ? plain : plain.substring(0, dot);
        String frac = dot < 0 ? "" : plain.substring(dot);
        StringBuilder sb = new StringBuilder();
        // 정수부를 오른쪽부터 세 자리씩 끊어 쉼표를 넣는다.
        for (int i = 0; i < intPart.length(); i++) {
            if (i > 0 && (intPart.length() - i) % 3 == 0) {
                sb.append(',');
            }
            sb.append(intPart.charAt(i));
        }
        return (negative ? "-" : "") + sb + frac;
    }
}
