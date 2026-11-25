package org.duckdns.hjow.swingwebpanel.js;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.ProxyObject;

/**
 * The abstract operations of ECMA-262 6th edition clauses 7.1 (type conversion), 7.2 (testing and comparison) and 7.3
 * (operations on objects), implemented once so that the interpreter and every built-in share the same JavaScript semantics.
 * Java <code>equals</code>, int casts and string concatenation must not replace these operations.
 * Methods that may run user code (valueOf, toString, getters) can throw {@link JsException}.
 * <p>
 * ECMA-262 6판 7.1(형 변환), 7.2(검사와 비교), 7.3(객체 연산)의 추상 연산을 한 곳에 구현하여 인터프리터와 모든 내장 객체가
 * 같은 JavaScript 의미를 공유하게 한다. Java 의 <code>equals</code>, int 캐스트, 문자열 연결로 이 연산을 대신해서는 안 된다.
 * 사용자 코드(valueOf, toString, getter)를 실행할 수 있는 메서드는 {@link JsException} 을 던질 수 있다.
 */
public final class Ops {

    /**
     * ToPrimitive hint "default".
     * <p>
     * ToPrimitive 힌트 "default".
     */
    public static final int HINT_DEFAULT = 0;

    /**
     * ToPrimitive hint "number".
     * <p>
     * ToPrimitive 힌트 "number".
     */
    public static final int HINT_NUMBER = 1;

    /**
     * ToPrimitive hint "string".
     * <p>
     * ToPrimitive 힌트 "string".
     */
    public static final int HINT_STRING = 2;

    /**
     * The value 2^32 as a double.
     * <p>
     * 2의 32제곱 값(double).
     */
    private static final double TWO_32 = 4294967296.0;

    /**
     * 2^53 - 1, the largest safe integer (and the maximum length).
     * <p>
     * 2^53 - 1. 가장 큰 안전한 정수이자 길이의 최댓값.
     */
    public static final double MAX_SAFE_INTEGER = 9007199254740991.0;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Ops() {
    }

    // ------------------------------------------------------------------ 7.1 type conversion

    /**
     * ToPrimitive (7.1.1).
     * <p>
     * ToPrimitive (7.1.1).
     *
     * @param input the value / 값
     * @param hint  {@link #HINT_DEFAULT}, {@link #HINT_NUMBER} or {@link #HINT_STRING} / 힌트
     * @return a primitive value / 원시 값
     */
    public static JsValue toPrimitive(JsValue input, int hint) {
        if (!(input instanceof JsObject)) {
            return input;
        }
        JsObject o = (JsObject) input;
        JsValue exotic = getMethod(o, WellKnownSymbols.TO_PRIMITIVE);
        if (!exotic.isUndefined()) {
            String h = hint == HINT_STRING ? "string" : hint == HINT_NUMBER ? "number" : "default";
            JsValue result = call(exotic, o, new JsValue[] { JsString.of(h) });
            if (!(result instanceof JsObject)) {
                return result;
            }
            throw JsException.typeError("Cannot convert object to primitive value");
        }
        return ordinaryToPrimitive(o, hint == HINT_STRING);
    }

    /**
     * OrdinaryToPrimitive (7.1.1.1).
     * <p>
     * OrdinaryToPrimitive (7.1.1.1).
     *
     * @param o            the object / 객체
     * @param preferString true to try toString first / toString 을 먼저 시도하려면 true
     * @return a primitive value / 원시 값
     */
    public static JsValue ordinaryToPrimitive(JsObject o, boolean preferString) {
        String first = preferString ? "toString" : "valueOf";
        String second = preferString ? "valueOf" : "toString";
        // 첫 번째 메서드가 원시 값을 주면 끝, 아니면 두 번째 메서드를 시도한다.
        JsValue m = o.get(first);
        if (m.isCallable()) {
            JsValue r = ((JsObject) m).call(o, Js.NO_ARGS);
            if (!(r instanceof JsObject)) {
                return r;
            }
        }
        m = o.get(second);
        if (m.isCallable()) {
            JsValue r = ((JsObject) m).call(o, Js.NO_ARGS);
            if (!(r instanceof JsObject)) {
                return r;
            }
        }
        throw JsException.typeError("Cannot convert object to primitive value");
    }

    /**
     * ToBoolean (7.1.2).
     * <p>
     * ToBoolean (7.1.2).
     *
     * @param v the value / 값
     * @return the boolean / 불리언
     */
    public static boolean toBoolean(JsValue v) {
        if (v instanceof JsBoolean) {
            return ((JsBoolean) v).getValue();
        }
        if (v instanceof JsObject) {
            return !v.hasIsHtmlDda();
        }
        if (v instanceof JsNumber) {
            double d = ((JsNumber) v).getValue();
            return d == d && d != 0;
        }
        if (v instanceof JsBigInt) {
            return ((JsBigInt) v).getValue().signum() != 0;
        }
        if (v instanceof JsString) {
            return ((JsString) v).length() != 0;
        }
        if (v instanceof JsSymbol) {
            return true;
        }
        return false;
    }

    /**
     * ToNumber (7.1.3).
     * <p>
     * ToNumber (7.1.3).
     *
     * @param v the value / 값
     * @return the number / 숫자
     */
    public static double toNumber(JsValue v) {
        if (v instanceof JsNumber) {
            return ((JsNumber) v).getValue();
        }
        if (v instanceof JsString) {
            return NumberText.parse(((JsString) v).getValue());
        }
        if (v instanceof JsBoolean) {
            return ((JsBoolean) v).getValue() ? 1 : 0;
        }
        if (v instanceof JsUndefined) {
            return Double.NaN;
        }
        if (v instanceof JsNull) {
            return 0;
        }
        if (v instanceof JsSymbol) {
            throw JsException.typeError("Cannot convert a Symbol value to a number");
        }
        if (v instanceof JsBigInt) {
            throw JsException.typeError("Cannot convert a BigInt value to a number");
        }
        return toNumber(toPrimitive(v, HINT_NUMBER));
    }

    /**
     * ToNumeric (6.1.6.2): ToPrimitive with number hint, then an unchanged BigInt or a Number.
     * <p>
     * ToNumeric (6.1.6.2): number 힌트로 원시 값으로 바꾼 뒤 BigInt 는 그대로, 나머지는 Number 로 바꾼다.
     *
     * @param v the value / 값
     * @return a Number or BigInt / Number 또는 BigInt
     */
    public static JsValue toNumeric(JsValue v) {
        JsValue p = toPrimitive(v, HINT_NUMBER);
        return p instanceof JsBigInt ? p : JsNumber.of(toNumber(p));
    }

    /**
     * ToBigInt (7.1.13): converts Boolean, BigInt and integer text, rejecting Number, Undefined, Null and Symbol.
     * <p>
     * ToBigInt (7.1.13): Boolean, BigInt, 정수 문자열을 바꾸고 Number, Undefined, Null, Symbol 을 거부한다.
     *
     * @param v the value / 값
     * @return the BigInt integer / BigInt 정수
     */
    public static BigInteger toBigInt(JsValue v) {
        JsValue p = toPrimitive(v, HINT_NUMBER);
        if (p instanceof JsBigInt) {
            return ((JsBigInt) p).getValue();
        }
        if (p instanceof JsBoolean) {
            return ((JsBoolean) p).getValue() ? BigInteger.ONE : BigInteger.ZERO;
        }
        if (p instanceof JsString) {
            return parseBigInt(((JsString) p).getValue());
        }
        throw JsException.typeError("Cannot convert " + JsException.describe(p) + " to a BigInt");
    }

    /**
     * Converts the argument of the BigInt function, whose integer Number special case is allowed in addition to ToBigInt.
     * <p>
     * ToBigInt 외에 정수 Number 특례를 허용하는 BigInt 함수 인수를 변환한다.
     *
     * @param v the value / 값
     * @return the integer / 정수
     */
    public static BigInteger bigIntFunctionValue(JsValue v) {
        JsValue p = toPrimitive(v, HINT_NUMBER);
        if (p instanceof JsNumber) {
            double n = ((JsNumber) p).getValue();
            if (n != n || Double.isInfinite(n) || n != Math.floor(n)) {
                throw JsException.rangeError("The number " + NumberText.toString(n) + " cannot be converted to a BigInt because it is not an integer");
            }
            return new BigDecimal(n).toBigInteger();
        }
        return toBigInt(p);
    }

    /**
     * Parses StringIntegerLiteral for BigInt conversion.
     * <p>
     * BigInt 변환용 StringIntegerLiteral 을 해석한다.
     *
     * @param text source text / 원문
     * @return the integer / 정수
     */
    private static BigInteger parseBigInt(String text) {
        int from = 0;
        int to = text.length();
        // StringIntegerLiteral 의 앞뒤 StrWhiteSpace/줄 끝을 JRE 버전과 무관한 표로 제거한다.
        while (from < to && JsChars.isSpace(text.charAt(from))) {
            from++;
        }
        // 뒤쪽 경계도 같은 공백 표로 줄여 BigInt 문자열 본문만 남긴다.
        while (to > from && JsChars.isSpace(text.charAt(to - 1))) {
            to--;
        }
        String s = text.substring(from, to);
        if (s.length() == 0) {
            return BigInteger.ZERO;
        }
        int sign = 1;
        int start = 0;
        char first = s.charAt(0);
        if (first == '+' || first == '-') {
            sign = first == '-' ? -1 : 1;
            start = 1;
        }
        if (start >= s.length()) {
            throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
        }
        int radix = 10;
        int digits = start;
        if (s.length() - start >= 2 && s.charAt(start) == '0') {
            char p = s.charAt(start + 1);
            if (p == 'x' || p == 'X') {
                if (start != 0) {
                    throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
                }
                radix = 16;
                digits += 2;
            } else if (p == 'o' || p == 'O') {
                if (start != 0) {
                    throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
                }
                radix = 8;
                digits += 2;
            } else if (p == 'b' || p == 'B') {
                if (start != 0) {
                    throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
                }
                radix = 2;
                digits += 2;
            }
        }
        if (digits >= s.length()) {
            throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
        }
        // 모든 글자가 고른 진법의 숫자인지 확인한다.
        for (int i = digits; i < s.length(); i++) {
            int d = Character.digit(s.charAt(i), radix);
            if (d < 0) {
                throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
            }
        }
        try {
            BigInteger n = new BigInteger(s.substring(digits), radix);
            return sign < 0 ? n.negate() : n;
        } catch (NumberFormatException e) {
            throw JsException.syntaxError("Cannot convert " + text + " to a BigInt");
        }
    }

    /**
     * ToInteger (7.1.4): NaN becomes 0, other values are truncated toward zero (infinities are kept).
     * <p>
     * ToInteger (7.1.4): NaN 은 0, 그 밖의 값은 0 쪽으로 버린다(무한대는 유지).
     *
     * @param v the value / 값
     * @return the integer-valued double / 정수 값 double
     */
    public static double toInteger(JsValue v) {
        return toInteger(toNumber(v));
    }

    /**
     * ToInteger for a double.
     * <p>
     * double 에 대한 ToInteger.
     *
     * @param d the number / 숫자
     * @return the integer-valued double / 정수 값 double
     */
    public static double toInteger(double d) {
        if (d != d) {
            return 0;
        }
        if (Double.isInfinite(d) || d == 0) {
            return d;
        }
        return d < 0 ? Math.ceil(d) : Math.floor(d);
    }

    /**
     * ToInt32 (7.1.5): modulo 2^32 into the signed 32-bit range.
     * <p>
     * ToInt32 (7.1.5): 2^32 로 나눈 나머지를 부호 있는 32비트 범위로 옮긴다.
     *
     * @param v the value / 값
     * @return the int / int 값
     */
    public static int toInt32(JsValue v) {
        return toInt32(toNumber(v));
    }

    /**
     * ToInt32 for a double.
     * <p>
     * double 에 대한 ToInt32.
     *
     * @param d the number / 숫자
     * @return the int / int 값
     */
    public static int toInt32(double d) {
        if (d != d || Double.isInfinite(d)) {
            return 0;
        }
        double t = d < 0 ? Math.ceil(d) : Math.floor(d);
        double m = t % TWO_32;
        return (int) (long) m;
    }

    /**
     * ToUint32 (7.1.6).
     * <p>
     * ToUint32 (7.1.6).
     *
     * @param v the value / 값
     * @return the unsigned 32-bit value as a long / 부호 없는 32비트 값(long)
     */
    public static long toUint32(JsValue v) {
        return toUint32(toNumber(v));
    }

    /**
     * ToUint32 for a double.
     * <p>
     * double 에 대한 ToUint32.
     *
     * @param d the number / 숫자
     * @return the unsigned 32-bit value as a long / 부호 없는 32비트 값(long)
     */
    public static long toUint32(double d) {
        return toInt32(d) & 0xFFFFFFFFL;
    }

    /**
     * ToUint16 (7.1.8).
     * <p>
     * ToUint16 (7.1.8).
     *
     * @param v the value / 값
     * @return value 0..65535 / 0..65535 값
     */
    public static int toUint16(JsValue v) {
        return toInt32(toNumber(v)) & 0xFFFF;
    }

    /**
     * ToString (7.1.12) returning a Java string. Symbols throw a TypeError.
     * <p>
     * ToString (7.1.12). Java 문자열을 반환한다. 심볼이면 TypeError.
     *
     * @param v the value / 값
     * @return the string / 문자열
     */
    public static String toString(JsValue v) {
        if (v instanceof JsString) {
            return ((JsString) v).getValue();
        }
        if (v instanceof JsNumber) {
            return NumberText.toString(((JsNumber) v).getValue());
        }
        if (v instanceof JsBigInt) {
            return ((JsBigInt) v).getValue().toString();
        }
        if (v instanceof JsBoolean) {
            return ((JsBoolean) v).getValue() ? "true" : "false";
        }
        if (v instanceof JsUndefined) {
            return "undefined";
        }
        if (v instanceof JsNull) {
            return "null";
        }
        if (v instanceof JsSymbol) {
            throw JsException.typeError("Cannot convert a Symbol value to a string");
        }
        return toString(toPrimitive(v, HINT_STRING));
    }

    /**
     * ToString returning a String value.
     * <p>
     * String 값을 반환하는 ToString.
     *
     * @param v the value / 값
     * @return the String value / String 값
     */
    public static JsString toStringValue(JsValue v) {
        return v instanceof JsString ? (JsString) v : JsString.of(toString(v));
    }

    /**
     * ToPropertyKey (7.1.14): a symbol stays a symbol, everything else becomes a string.
     * <p>
     * ToPropertyKey (7.1.14): 심볼은 그대로, 나머지는 문자열이 된다.
     *
     * @param v the value / 값
     * @return a {@link java.lang.String} or {@link JsSymbol} / {@link java.lang.String} 또는 {@link JsSymbol}
     */
    public static Object toPropertyKey(JsValue v) {
        if (v instanceof JsString) {
            return ((JsString) v).getValue();
        }
        if (v instanceof JsSymbol) {
            return v;
        }
        JsValue prim = toPrimitive(v, HINT_STRING);
        if (prim instanceof JsSymbol) {
            return prim;
        }
        return toString(prim);
    }

    /**
     * ToLength (7.1.15): integer clamped to 0 .. 2^53 - 1.
     * <p>
     * ToLength (7.1.15): 정수로 바꿔 0 .. 2^53 - 1 범위로 제한한다.
     *
     * @param v the value / 값
     * @return the length as a double / double 형 길이
     */
    public static double toLength(JsValue v) {
        double len = toInteger(v);
        if (len <= 0) {
            return 0;
        }
        return Math.min(len, MAX_SAFE_INTEGER);
    }

    /**
     * ToObject (7.1.13) using the wrapper prototypes of a realm. Undefined and null throw a TypeError.
     * <p>
     * realm 의 래퍼 프로토타입을 써서 ToObject (7.1.13)를 수행한다. undefined 와 null 은 TypeError.
     *
     * @param realm the realm / realm
     * @param v     the value / 값
     * @return the object / 객체
     */
    public static JsObject toObject(JsRealm realm, JsValue v) {
        if (v instanceof JsObject) {
            return (JsObject) v;
        }
        if (v instanceof JsString) {
            return new JsStringObject(realm.getStringPrototype(), ((JsString) v).getValue());
        }
        if (v instanceof JsNumber) {
            return new JsPrimitiveObject(realm.getNumberPrototype(), v, "Number");
        }
        if (v instanceof JsBigInt) {
            return new JsPrimitiveObject(realm.getBigIntPrototype(), v, "BigInt");
        }
        if (v instanceof JsBoolean) {
            return new JsPrimitiveObject(realm.getBooleanPrototype(), v, "Boolean");
        }
        if (v instanceof JsSymbol) {
            return new JsPrimitiveObject(realm.getSymbolPrototype(), v, "Symbol");
        }
        throw JsException.typeError("Cannot convert undefined or null to object");
    }

    /**
     * RequireObjectCoercible (7.2.1): throws a TypeError for undefined and null.
     * <p>
     * RequireObjectCoercible (7.2.1): undefined 와 null 이면 TypeError 를 던진다.
     *
     * @param v the value / 값
     * @return the same value / 같은 값
     */
    public static JsValue requireObjectCoercible(JsValue v) {
        if (v.isNullOrUndefined()) {
            throw JsException.typeError("Cannot convert undefined or null to object");
        }
        return v;
    }

    // ------------------------------------------------------------------ 7.2 testing and comparison

    /**
     * Strict equality (7.2.13): the === operator.
     * <p>
     * 엄격한 동등 (7.2.13): === 연산자.
     *
     * @param a left / 왼쪽
     * @param b right / 오른쪽
     * @return the result / 결과
     */
    public static boolean strictEquals(JsValue a, JsValue b) {
        if (a == b) {
            // 같은 참조이면 NaN 만 예외이다.
            return !(a instanceof JsNumber) || ((JsNumber) a).getValue() == ((JsNumber) a).getValue();
        }
        if (a instanceof JsNumber && b instanceof JsNumber) {
            return ((JsNumber) a).getValue() == ((JsNumber) b).getValue();
        }
        if (a instanceof JsBigInt && b instanceof JsBigInt) {
            return ((JsBigInt) a).getValue().equals(((JsBigInt) b).getValue());
        }
        if (a instanceof JsString && b instanceof JsString) {
            return ((JsString) a).getValue().equals(((JsString) b).getValue());
        }
        return false;
    }

    /**
     * Abstract equality (7.2.12): the == operator.
     * <p>
     * 추상 동등 (7.2.12): == 연산자.
     *
     * @param a left / 왼쪽
     * @param b right / 오른쪽
     * @return the result / 결과
     */
    public static boolean looseEquals(JsValue a, JsValue b) {
        // 같은 형식이면 엄격 동등과 같다.
        if (a.type() == b.type()) {
            return strictEquals(a, b);
        }
        if (a.isNullOrUndefined() && b.isNullOrUndefined()) {
            return true;
        }
        // 부속서 B의 레거시 컬렉션은 느슨한 비교에서 null과 undefined에만 이 예외를 적용한다.
        if ((a.isNullOrUndefined() && b.hasIsHtmlDda()) || (b.isNullOrUndefined() && a.hasIsHtmlDda())) {
            return true;
        }
        if (a instanceof JsNumber && b instanceof JsString) {
            return ((JsNumber) a).getValue() == toNumber(b);
        }
        if (a instanceof JsString && b instanceof JsNumber) {
            return toNumber(a) == ((JsNumber) b).getValue();
        }
        if (a instanceof JsBigInt && b instanceof JsString) {
            return bigIntStringEquals(((JsBigInt) a).getValue(), ((JsString) b).getValue());
        }
        if (a instanceof JsString && b instanceof JsBigInt) {
            return bigIntStringEquals(((JsBigInt) b).getValue(), ((JsString) a).getValue());
        }
        if (a instanceof JsBigInt && b instanceof JsNumber) {
            return bigIntNumberEquals(((JsBigInt) a).getValue(), ((JsNumber) b).getValue());
        }
        if (a instanceof JsNumber && b instanceof JsBigInt) {
            return bigIntNumberEquals(((JsBigInt) b).getValue(), ((JsNumber) a).getValue());
        }
        if (a instanceof JsBigInt && b instanceof JsBoolean) {
            return ((JsBigInt) a).getValue().equals(((JsBoolean) b).getValue() ? BigInteger.ONE : BigInteger.ZERO);
        }
        if (a instanceof JsBoolean && b instanceof JsBigInt) {
            return ((JsBigInt) b).getValue().equals(((JsBoolean) a).getValue() ? BigInteger.ONE : BigInteger.ZERO);
        }
        if (a instanceof JsBoolean) {
            return looseEquals(JsNumber.of(toNumber(a)), b);
        }
        if (b instanceof JsBoolean) {
            return looseEquals(a, JsNumber.of(toNumber(b)));
        }
        if ((a instanceof JsString || a instanceof JsNumber || a instanceof JsBigInt || a instanceof JsSymbol) && b instanceof JsObject) {
            return looseEquals(a, toPrimitive(b, HINT_DEFAULT));
        }
        if (a instanceof JsObject && (b instanceof JsString || b instanceof JsNumber || b instanceof JsBigInt || b instanceof JsSymbol)) {
            return looseEquals(toPrimitive(a, HINT_DEFAULT), b);
        }
        return false;
    }

    /**
     * Compares a BigInt with a JavaScript string without throwing on invalid StringIntegerLiteral text.
     * <p>
     * 잘못된 StringIntegerLiteral 에 오류를 던지지 않고 BigInt 와 문자열을 비교한다.
     */
    private static boolean bigIntStringEquals(BigInteger integer, String text) {
        try {
            return integer.equals(parseBigInt(text));
        } catch (JsException e) {
            return false;
        }
    }

    /**
     * Compares a BigInt and the exact mathematical value of a JavaScript Number.
     * <p>
     * BigInt 와 JavaScript Number 의 정확한 수학적 값을 비교한다.
     */
    private static boolean bigIntNumberEquals(BigInteger integer, double number) {
        if (number != number || Double.isInfinite(number)) {
            return false;
        }
        try {
            return integer.equals(new BigDecimal(number).toBigIntegerExact());
        } catch (ArithmeticException e) {
            return false;
        }
    }

    /**
     * CanBeHeldWeakly (ECMA-262 §9.13): objects and symbols outside the global symbol registry are eligible.
     * <p>
     * CanBeHeldWeakly (ECMA-262 §9.13): 객체와 전역 레지스트리에 등록되지 않은 심볼을 약하게 참조할 수 있다.
     *
     * @param value ECMAScript value / ECMAScript 값
     * @return true for an eligible weak reference target / 약한 참조의 대상이 될 수 있으면 true
     */
    public static boolean canBeHeldWeakly(JsValue value) {
        return value instanceof JsObject || (value instanceof JsSymbol && ((JsSymbol) value).getRegistryKey() == null);
    }

    /**
     * SameValue (7.2.9): like === but NaN equals NaN and +0 differs from -0 (Object.is).
     * <p>
     * SameValue (7.2.9): === 와 비슷하지만 NaN 은 NaN 과 같고 +0 과 -0 은 다르다(Object.is).
     *
     * @param a left / 왼쪽
     * @param b right / 오른쪽
     * @return the result / 결과
     */
    public static boolean sameValue(JsValue a, JsValue b) {
        if (a instanceof JsNumber && b instanceof JsNumber) {
            double x = ((JsNumber) a).getValue();
            double y = ((JsNumber) b).getValue();
            if (x != x) {
                return y != y;
            }
            if (x == 0 && y == 0) {
                return (1 / x) == (1 / y);
            }
            return x == y;
        }
        return strictEquals(a, b);
    }

    /**
     * SameValueZero (7.2.10): like SameValue but +0 equals -0 (used by Map, Set and includes).
     * <p>
     * SameValueZero (7.2.10): SameValue 와 같지만 +0 과 -0 이 같다(Map, Set, includes 가 사용).
     *
     * @param a left / 왼쪽
     * @param b right / 오른쪽
     * @return the result / 결과
     */
    public static boolean sameValueZero(JsValue a, JsValue b) {
        if (a instanceof JsNumber && b instanceof JsNumber) {
            double x = ((JsNumber) a).getValue();
            double y = ((JsNumber) b).getValue();
            return x == y || (x != x && y != y);
        }
        return strictEquals(a, b);
    }

    /**
     * Abstract relational comparison (7.2.11) of x &lt; y.
     * <p>
     * 추상 관계 비교 (7.2.11) 의 x &lt; y.
     *
     * @param x         left operand / 왼쪽 피연산자
     * @param y         right operand / 오른쪽 피연산자
     * @param leftFirst true to convert x before y (the order of side effects) / x 를 y 보다 먼저 변환하려면 true(부수 효과 순서)
     * @return 1 for true, 0 for false, -1 for undefined (a NaN was involved) / 참이면 1, 거짓이면 0, undefined(NaN 관여)이면 -1
     */
    public static int lessThan(JsValue x, JsValue y, boolean leftFirst) {
        JsValue px;
        JsValue py;
        // 피연산자 변환 순서는 leftFirst 에 따른다.
        if (leftFirst) {
            px = toPrimitive(x, HINT_NUMBER);
            py = toPrimitive(y, HINT_NUMBER);
        } else {
            py = toPrimitive(y, HINT_NUMBER);
            px = toPrimitive(x, HINT_NUMBER);
        }
        if (px instanceof JsString && py instanceof JsString) {
            return ((JsString) px).getValue().compareTo(((JsString) py).getValue()) < 0 ? 1 : 0;
        }
        if (px instanceof JsBigInt || py instanceof JsBigInt) {
            if (px instanceof JsBigInt && py instanceof JsBigInt) {
                return ((JsBigInt) px).getValue().compareTo(((JsBigInt) py).getValue()) < 0 ? 1 : 0;
            }
            double nx = px instanceof JsBigInt ? 0 : toNumber(px);
            double ny = py instanceof JsBigInt ? 0 : toNumber(py);
            BigDecimal dx;
            BigDecimal dy;
            if (px instanceof JsBigInt) {
                if (ny != ny) {
                    return -1;
                }
                if (Double.isInfinite(ny)) {
                    return ny > 0 ? 1 : 0;
                }
                dx = new BigDecimal(((JsBigInt) px).getValue());
                dy = new BigDecimal(ny);
            } else {
                if (nx != nx) {
                    return -1;
                }
                if (Double.isInfinite(nx)) {
                    return nx < 0 ? 1 : 0;
                }
                dx = new BigDecimal(nx);
                dy = new BigDecimal(((JsBigInt) py).getValue());
            }
            return dx.compareTo(dy) < 0 ? 1 : 0;
        }
        double nx = toNumber(px);
        double ny = toNumber(py);
        if (nx != nx || ny != ny) {
            return -1;
        }
        return nx < ny ? 1 : 0;
    }

    /**
     * The + operator on two values (12.7.3.1).
     * <p>
     * 두 값에 대한 + 연산 (12.7.3.1).
     *
     * @param a left / 왼쪽
     * @param b right / 오른쪽
     * @return the sum or concatenation / 합 또는 연결 문자열
     */
    public static JsValue add(JsValue a, JsValue b) {
        if (a instanceof JsNumber && b instanceof JsNumber) {
            return JsNumber.of(((JsNumber) a).getValue() + ((JsNumber) b).getValue());
        }
        if (a instanceof JsBigInt && b instanceof JsBigInt) {
            return JsBigInt.of(((JsBigInt) a).getValue().add(((JsBigInt) b).getValue()));
        }
        JsValue pa = toPrimitive(a, HINT_DEFAULT);
        JsValue pb = toPrimitive(b, HINT_DEFAULT);
        if (pa instanceof JsString || pb instanceof JsString) {
            return JsString.of(toString(pa).concat(toString(pb)));
        }
        if (pa instanceof JsBigInt && pb instanceof JsBigInt) {
            return JsBigInt.of(((JsBigInt) pa).getValue().add(((JsBigInt) pb).getValue()));
        }
        if (pa instanceof JsBigInt || pb instanceof JsBigInt) {
            throw JsException.typeError("Cannot mix BigInt and other types, use explicit conversions");
        }
        return JsNumber.of(toNumber(pa) + toNumber(pb));
    }

    // ------------------------------------------------------------------ 7.3 operations on objects

    /**
     * GetV (7.3.2): reads a property of any value (primitives use their wrapper prototype).
     * <p>
     * GetV (7.3.2): 임의의 값의 프로퍼티를 읽는다(원시 값은 래퍼 프로토타입을 사용).
     *
     * @param realm the realm / realm
     * @param v     the base value / 기준 값
     * @param key   the key / 키
     * @return the value / 값
     */
    public static JsValue getV(JsRealm realm, JsValue v, Object key) {
        if (v instanceof JsObject) {
            return ((JsObject) v).get(key, v);
        }
        return toObject(realm, v).get(key, v);
    }

    /**
     * GetMethod (7.3.9): returns undefined if the property is null or undefined, throws a TypeError if it is not callable.
     * Only objects are supported here; use {@link #getMethod(JsRealm, JsValue, Object)} for primitives.
     * <p>
     * GetMethod (7.3.9): 프로퍼티가 null/undefined 이면 undefined 를 반환하고 호출할 수 없으면 TypeError 를 던진다.
     * 여기서는 객체만 지원하며 원시 값에는 {@link #getMethod(JsRealm, JsValue, Object)} 를 쓴다.
     *
     * @param o   the object / 객체
     * @param key the key / 키
     * @return the function or undefined / 함수 또는 undefined
     */
    public static JsValue getMethod(JsObject o, Object key) {
        JsValue f = o.get(key, o);
        if (f.isNullOrUndefined()) {
            return Js.UNDEFINED;
        }
        if (!f.isCallable()) {
            throw JsException.typeError(PropertyKeys.describe(key) + " is not a function");
        }
        return f;
    }

    /**
     * GetMethod for any base value.
     * <p>
     * 임의의 기준 값에 대한 GetMethod.
     *
     * @param realm the realm / realm
     * @param v     the base value / 기준 값
     * @param key   the key / 키
     * @return the function or undefined / 함수 또는 undefined
     */
    public static JsValue getMethod(JsRealm realm, JsValue v, Object key) {
        JsValue f = getV(realm, v, key);
        if (f.isNullOrUndefined()) {
            return Js.UNDEFINED;
        }
        if (!f.isCallable()) {
            throw JsException.typeError(PropertyKeys.describe(key) + " is not a function");
        }
        return f;
    }

    /**
     * Call (7.3.12): calls a function value or throws a TypeError if it is not callable.
     * <p>
     * Call (7.3.12): 함수 값을 호출하며 호출할 수 없으면 TypeError 를 던진다.
     *
     * @param f         the function value / 함수 값
     * @param thisValue the this value / this 값
     * @param args      arguments / 인수
     * @return the result / 결과
     */
    public static JsValue call(JsValue f, JsValue thisValue, JsValue[] args) {
        if (!(f instanceof JsObject) || !f.isCallable()) {
            throw JsException.typeError(JsException.describe(f) + " is not a function");
        }
        return ((JsObject) f).call(thisValue, args);
    }

    /**
     * Invoke (7.3.18): gets a method of a value and calls it with the value as this.
     * <p>
     * Invoke (7.3.18): 값의 메서드를 찾아 그 값을 this 로 호출한다.
     *
     * @param realm the realm / realm
     * @param v     the receiver value / 대상 값
     * @param key   the method key / 메서드 키
     * @param args  arguments / 인수
     * @return the result / 결과
     */
    public static JsValue invoke(JsRealm realm, JsValue v, Object key, JsValue[] args) {
        JsValue f = getV(realm, v, key);
        if (!f.isCallable()) {
            throw JsException.typeError(PropertyKeys.describe(key) + " is not a function");
        }
        return ((JsObject) f).call(v, args);
    }

    /**
     * Construct (7.3.13): constructs with new.target equal to the constructor.
     * <p>
     * Construct (7.3.13): new.target 이 생성자 자신인 상태로 생성한다.
     *
     * @param f    the constructor value / 생성자 값
     * @param args arguments / 인수
     * @return the new object / 새 객체
     */
    public static JsObject construct(JsValue f, JsValue[] args) {
        if (!(f instanceof JsObject) || !((JsObject) f).isConstructor()) {
            throw JsException.typeError(JsException.describe(f) + " is not a constructor");
        }
        return ((JsObject) f).construct(args, (JsObject) f);
    }

    /**
     * GetPrototypeFromConstructor (9.1.14): reads newTarget.prototype and falls back to an intrinsic prototype if it is not an object.
     * <p>
     * GetPrototypeFromConstructor (9.1.14): newTarget.prototype 을 읽고 객체가 아니면 내장 프로토타입을 쓴다.
     *
     * @param realm    the realm / realm
     * @param newTarget the new.target / new.target
     * @param fallback the intrinsic default prototype / 기본 내장 프로토타입
     * @return the prototype to use / 사용할 프로토타입
     */
    public static JsObject getPrototypeFromConstructor(JsRealm realm, JsObject newTarget, JsObject fallback) {
        if (newTarget == null) {
            return fallback;
        }
        JsValue p = newTarget.get("prototype");
        return p instanceof JsObject ? (JsObject) p : fallback;
    }

    /**
     * Looks up a constructor's prototype, with an intrinsic from the constructor's realm as the fallback.
     * <p>
     * 생성자의 프로토타입을 조회한다. 기본값으로는 생성자 렐름의 내장 객체를 사용한다.
     *
     * @param realm the current realm / 현재 렐름
     * @param newTarget the constructor / 생성자
     * @param fallbackIntrinsic the name of the default prototype / 기본 프로토타입 내장 이름
     * @return the prototype / 프로토타입
     */
    public static JsObject getIntrinsicPrototypeFromConstructor(JsRealm realm, JsObject newTarget, String fallbackIntrinsic) {
        JsValue proto = newTarget.get("prototype");
        return proto instanceof JsObject ? (JsObject) proto : functionRealm(realm, newTarget).getIntrinsic(fallbackIntrinsic);
    }

    /**
     * Finds the realm of a function, following a bound target or an active proxy target.
     * <p>
     * 바인딩 대상 또는 해제되지 않은 프록시 대상을 따라 함수의 렐름을 찾는다.
     *
     * @param current the caller's realm / 호출자 렐름
     * @param function the function / 함수
     * @return the function's realm / 함수의 렐름
     */
    private static JsRealm functionRealm(JsRealm current, JsObject function) {
        JsObject target = function;
        // 바인딩과 프록시는 함수가 정의된 실제 렐름까지 따라간다.
        for (;;) {
            if (target instanceof BoundFunction) {
                target = ((BoundFunction) target).getTarget();
            } else if (target instanceof ProxyObject) {
                target = ((ProxyObject) target).getProxyTarget();
                if (target == null) {
                    throw JsException.typeError("Cannot get the realm of a revoked proxy");
                }
            } else {
                return target instanceof JsFunction ? ((JsFunction) target).getRealm() : current;
            }
        }
    }

    /**
     * IsArray (7.2.2).
     * <p>
     * IsArray (7.2.2).
     *
     * @param v the value / 값
     * @return true for arrays / 배열이면 true
     */
    public static boolean isArray(JsValue v) {
        return v instanceof JsObject && ((JsObject) v).isArray();
    }

    /**
     * IsConstructor (7.2.4).
     * <p>
     * IsConstructor (7.2.4).
     *
     * @param v the value / 값
     * @return true for constructors / 생성자이면 true
     */
    public static boolean isConstructor(JsValue v) {
        return v instanceof JsObject && ((JsObject) v).isConstructor();
    }

    /**
     * LengthOfArrayLike: ToLength(Get(o, "length")).
     * <p>
     * LengthOfArrayLike: ToLength(Get(o, "length")).
     *
     * @param o the object / 객체
     * @return the length / 길이
     */
    public static double lengthOf(JsObject o) {
        return toLength(o.get("length"));
    }

    /**
     * CreateListFromArrayLike (7.3.17): reads an array-like object into a Java array.
     * <p>
     * CreateListFromArrayLike (7.3.17): 유사 배열 객체를 Java 배열로 읽는다.
     *
     * @param v the array-like value / 유사 배열 값
     * @return the elements / 요소들
     */
    public static JsValue[] createListFromArrayLike(JsValue v) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError("CreateListFromArrayLike called on non-object");
        }
        JsObject o = (JsObject) v;
        double len = lengthOf(o);
        if (len > 10000000) {
            throw JsException.rangeError("Invalid array length");
        }
        JsValue[] list = new JsValue[(int) len];
        // 0 부터 length-1 까지 요소를 읽는다.
        for (int i = 0; i < list.length; i++) {
            list[i] = o.get(PropertyKeys.fromIndex(i));
        }
        return list;
    }

    /**
     * OrdinaryHasInstance (7.3.19).
     * <p>
     * OrdinaryHasInstance (7.3.19).
     *
     * @param c the constructor / 생성자
     * @param o the value to test / 검사할 값
     * @return true if o is an instance of c / o 가 c 의 인스턴스이면 true
     */
    public static boolean ordinaryHasInstance(JsValue c, JsValue o) {
        if (!c.isCallable()) {
            return false;
        }
        if (c instanceof BoundFunction) {
            return instanceOf(((BoundFunction) c).getTarget(), o);
        }
        if (!(o instanceof JsObject)) {
            return false;
        }
        JsValue p = ((JsObject) c).get("prototype");
        if (!(p instanceof JsObject)) {
            throw JsException.typeError("Function has non-object prototype in instanceof check");
        }
        JsObject proto = ((JsObject) o).getPrototypeOf();
        // 프로토타입 체인을 끝까지 따라가며 비교한다.
        while (proto != null) {
            if (proto == p) {
                return true;
            }
            proto = proto.getPrototypeOf();
        }
        return false;
    }

    /**
     * InstanceofOperator (12.9.4): the instanceof operator.
     * <p>
     * InstanceofOperator (12.9.4): instanceof 연산자.
     *
     * @param target the right operand / 오른쪽 피연산자
     * @param v      the left operand / 왼쪽 피연산자
     * @return the result / 결과
     */
    public static boolean instanceOf(JsValue target, JsValue v) {
        if (!(target instanceof JsObject)) {
            throw JsException.typeError("Right-hand side of 'instanceof' is not an object");
        }
        JsObject t = (JsObject) target;
        JsValue handler = getMethod(t, WellKnownSymbols.HAS_INSTANCE);
        if (!handler.isUndefined()) {
            return toBoolean(call(handler, t, new JsValue[] { v }));
        }
        if (!t.isCallable()) {
            throw JsException.typeError("Right-hand side of 'instanceof' is not callable");
        }
        return ordinaryHasInstance(t, v);
    }

    /**
     * Collects the own enumerable string keys of an object as Java strings (the order of Object.keys).
     * <p>
     * 객체의 자체 열거 가능 문자열 키를 Java 문자열로 모은다(Object.keys 의 순서).
     *
     * @param o the object / 객체
     * @return the keys / 키 목록
     */
    public static List<String> enumerableOwnKeys(JsObject o) {
        List<String> out = new ArrayList<String>();
        // 자체 키를 순서대로 훑으며 열거 가능한 문자열 키만 고른다.
        for (Object k : o.ownKeys()) {
            if (k instanceof String) {
                Property p = o.getOwnPropertySlot(k);
                if (p != null && p.enumerable) {
                    out.add((String) k);
                }
            }
        }
        return out;
    }
}
