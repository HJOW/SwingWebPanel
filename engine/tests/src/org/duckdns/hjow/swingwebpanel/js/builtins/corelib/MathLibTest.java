package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Math object: constants, every function, special values (NaN, infinities, negative zero), argument coercion and
 * numerically delicate cases (plan item P14-01, Math part).
 * <p>
 * Math 객체의 시험: 상수, 모든 함수, 특수 값(NaN, 무한대, 음의 0), 인수 변환, 수치적으로 까다로운 경우(계획 P14-01 의 Math 부분).
 */
public class MathLibTest {

    /**
     * The environment of the current case.
     * <p>
     * 현재 사례의 환경.
     */
    private LibEnv env;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MathLibTest() {
    }

    /**
     * Creates a fresh environment for every case.
     * <p>
     * 사례마다 새 환경을 만든다.
     */
    public void setUp() {
        env = new LibEnv();
    }

    /**
     * Calls Math.name with number arguments and returns the number.
     * <p>
     * 숫자 인수로 Math.name 을 호출해 숫자를 돌려준다.
     *
     * @param name the function name / 함수 이름
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    private double m(String name, double... args) {
        JsValue[] values = new JsValue[args.length];
        for (int i = 0; i < args.length; i++) { // 인수를 JavaScript 숫자 값으로 바꾼다.
            values[i] = Js.num(args[i]);
        }
        return ((JsNumber) env.call("Math." + name, values)).getValue();
    }

    /**
     * Checks a result exactly, distinguishing +0 from -0 and treating NaN as equal to NaN.
     * <p>
     * 결과를 정확히 확인한다. +0 과 -0 을 구분하고 NaN 은 NaN 과 같다고 본다.
     *
     * @param expected expected value / 기대 값
     * @param actual   actual value / 실제 값
     * @param what     description / 설명
     */
    private static void same(double expected, double actual, String what) {
        boolean equal = Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)
                || (Double.isNaN(expected) && Double.isNaN(actual));
        Check.isTrue(equal, what + " (expected " + expected + " but was " + actual + ")");
    }

    /**
     * Checks a result with a relative tolerance (for transcendental functions).
     * <p>
     * 결과를 상대 오차 안에서 확인한다(초월 함수용).
     *
     * @param expected expected value / 기대 값
     * @param actual   actual value / 실제 값
     * @param what     description / 설명
     */
    private static void close(double expected, double actual, String what) {
        double tolerance = Math.max(1e-15, Math.abs(expected) * 1e-14);
        Check.isTrue(Math.abs(expected - actual) <= tolerance, what + " (expected " + expected + " but was " + actual + ")");
    }

    /**
     * The constants and the string tag.
     * <p>
     * 상수와 문자열 태그.
     */
    public void testConstantsAndTag() {
        same(Math.E, ((JsNumber) env.get("Math.E")).getValue(), "E");
        same(Math.PI, ((JsNumber) env.get("Math.PI")).getValue(), "PI");
        close(Math.log(10), ((JsNumber) env.get("Math.LN10")).getValue(), "LN10");
        close(Math.log(2), ((JsNumber) env.get("Math.LN2")).getValue(), "LN2");
        close(1 / Math.log(10), ((JsNumber) env.get("Math.LOG10E")).getValue(), "LOG10E");
        close(1 / Math.log(2), ((JsNumber) env.get("Math.LOG2E")).getValue(), "LOG2E");
        close(Math.sqrt(0.5), ((JsNumber) env.get("Math.SQRT1_2")).getValue(), "SQRT1_2");
        close(Math.sqrt(2), ((JsNumber) env.get("Math.SQRT2")).getValue(), "SQRT2");
        String[] names = { "E", "LN10", "LN2", "LOG10E", "LOG2E", "PI", "SQRT1_2", "SQRT2" };
        JsObject math = (JsObject) env.get("Math");
        for (int i = 0; i < names.length; i++) { // 상수의 속성은 모두 false 여야 한다.
            PropertyDescriptor d = math.getOwnProperty(names[i]);
            Check.isFalse(d.isWritableTrue() || d.isEnumerableTrue() || d.isConfigurableTrue(), "Math." + names[i] + " attributes");
        }
        PropertyDescriptor tag = math.getOwnProperty(WellKnownSymbols.TO_STRING_TAG);
        Check.equal("Math", LibEnv.str(tag.value), "@@toStringTag");
        Check.isTrue(tag.isConfigurableTrue() && !tag.isWritableTrue() && !tag.isEnumerableTrue(), "@@toStringTag attributes");
        Check.isFalse(math.isCallable(), "Math is not a function");
        Check.isFalse(math.isConstructor(), "Math is not a constructor");
    }

    /**
     * abs, ceil, floor, round, trunc, sign and sqrt including negative zero.
     * <p>
     * 음의 0 을 포함한 abs, ceil, floor, round, trunc, sign, sqrt.
     */
    public void testRoundingAndSigns() {
        same(0.0, m("abs", -0.0), "abs(-0)");
        same(3, m("abs", -3), "abs(-3)");
        same(Double.POSITIVE_INFINITY, m("abs", Double.NEGATIVE_INFINITY), "abs(-Infinity)");
        same(Double.NaN, m("abs", Double.NaN), "abs(NaN)");
        same(-0.0, m("ceil", -0.5), "ceil(-0.5) is -0");
        same(-0.0, m("floor", -0.0), "floor(-0)");
        same(-1, m("floor", -0.5), "floor(-0.5)");
        same(3, m("round", 2.5), "round(2.5)");
        same(-2, m("round", -2.5), "round(-2.5) rounds toward +Infinity");
        same(2, m("round", 1.5), "round(1.5)");
        same(-1, m("round", -1.5), "round(-1.5)");
        same(-0.0, m("round", -0.5), "round(-0.5) is -0");
        same(-0.0, m("round", -0.2), "round(-0.2) is -0");
        same(0.0, m("round", 0.2), "round(0.2)");
        same(0.0, m("round", 0.49999999999999994), "round of the largest number below 0.5 is 0 (no floor(x+0.5) bug)");
        same(4503599627370497.0, m("round", 4503599627370497.0), "round of 2^52+1 is unchanged");
        same(9007199254740991.0, m("round", 9007199254740991.0), "round of 2^53-1 is unchanged");
        same(Double.POSITIVE_INFINITY, m("round", Double.POSITIVE_INFINITY), "round(Infinity)");
        same(-0.0, m("round", -0.0), "round(-0)");
        same(-0.0, m("trunc", -0.9), "trunc(-0.9) is -0");
        same(4, m("trunc", 4.7), "trunc(4.7)");
        same(-4, m("trunc", -4.7), "trunc(-4.7)");
        same(-1, m("sign", -3), "sign(-3)");
        same(1, m("sign", 0.1), "sign(0.1)");
        same(0.0, m("sign", 0.0), "sign(+0)");
        same(-0.0, m("sign", -0.0), "sign(-0)");
        same(Double.NaN, m("sign", Double.NaN), "sign(NaN)");
        same(2, m("sqrt", 4), "sqrt(4)");
        same(-0.0, m("sqrt", -0.0), "sqrt(-0)");
        same(Double.NaN, m("sqrt", -1), "sqrt(-1)");
        same(-2, m("cbrt", -8), "cbrt(-8)");
        same(3, m("cbrt", 27), "cbrt(27)");
    }

    /**
     * The 32-bit integer functions clz32, imul and fround.
     * <p>
     * 32비트 정수 함수 clz32, imul, fround.
     */
    public void testIntegerAndFloatFunctions() {
        same(32, m("clz32", 0), "clz32(0)");
        same(31, m("clz32", 1), "clz32(1)");
        same(0, m("clz32", -1), "clz32(-1)");
        same(32, m("clz32", 0.5), "clz32(0.5)");
        same(32, m("clz32", 4294967296.0), "clz32(2^32) is clz32(0)");
        same(32, m("clz32", Double.NaN), "clz32(NaN)");
        same(24, m("clz32", 255), "clz32(255)");
        same(-5, m("imul", 4294967295.0, 5), "imul(0xffffffff, 5)");
        same(8, m("imul", 2, 4), "imul(2, 4)");
        same(-60, m("imul", -5, 12), "imul(-5, 12)");
        same(0, m("imul", 2147483648.0, 2), "imul(2^31, 2) wraps to 0");
        same(5.5, m("fround", 5.5), "fround(5.5)");
        same(5.050000190734863, m("fround", 5.05), "fround(5.05)");
        same(Double.POSITIVE_INFINITY, m("fround", 3.5e38), "fround overflows to Infinity");
        same(0.0, m("fround", 1e-46), "fround underflows to 0");
        same(-0.0, m("fround", -0.0), "fround(-0)");
        same(Double.NaN, m("fround", Double.NaN), "fround(NaN)");
    }

    /**
     * Exponential and logarithm functions including exact results for powers of two and ten.
     * <p>
     * 2 와 10 의 거듭제곱에서 정확한 결과를 내는 지수 및 로그 함수.
     */
    public void testExponentialAndLogarithms() {
        same(1, m("exp", 0), "exp(0)");
        close(Math.E, m("exp", 1), "exp(1)");
        same(0.0, m("expm1", 0.0), "expm1(0)");
        same(-0.0, m("expm1", -0.0), "expm1(-0)");
        close(1.00000000005e-10, m("expm1", 1e-10), "expm1(1e-10) is precise");
        same(Double.NEGATIVE_INFINITY, m("log", 0), "log(0)");
        same(Double.NaN, m("log", -1), "log(-1)");
        same(0.0, m("log", 1), "log(1)");
        same(3, m("log10", 1000), "log10(1000) is exact");
        same(-3, m("log10", 0.001), "log10(0.001)");
        same(3, m("log2", 8), "log2(8) is exact");
        same(0.0, m("log2", 1), "log2(1)");
        same(-1, m("log2", 0.5), "log2(0.5)");
        same(1023, m("log2", Math.pow(2, 1023)), "log2(2^1023)");
        same(-1074, m("log2", Double.MIN_VALUE), "log2 of the smallest denormal");
        close(1.584962500721156, m("log2", 3), "log2(3)");
        same(Double.NEGATIVE_INFINITY, m("log2", 0), "log2(0)");
        same(Double.NaN, m("log2", -2), "log2(-2)");
        same(0.0, m("log1p", 0.0), "log1p(0)");
        same(-0.0, m("log1p", -0.0), "log1p(-0)");
        same(Double.NEGATIVE_INFINITY, m("log1p", -1), "log1p(-1)");
        same(Double.NaN, m("log1p", -2), "log1p(-2)");
        close(9.999995000003334E-7, m("log1p", 1e-6), "log1p(1e-6)");
    }

    /**
     * Trigonometric and hyperbolic functions including the inverse hyperbolic functions written in this package.
     * <p>
     * 이 패키지에서 직접 만든 역쌍곡 함수를 포함한 삼각 함수와 쌍곡 함수.
     */
    public void testTrigonometryAndHyperbolic() {
        same(0.0, m("sin", 0.0), "sin(0)");
        same(-0.0, m("sin", -0.0), "sin(-0)");
        same(1, m("cos", 0), "cos(0)");
        same(Double.NaN, m("sin", Double.POSITIVE_INFINITY), "sin(Infinity)");
        close(Math.PI / 4, m("atan", 1), "atan(1)");
        same(Double.NaN, m("asin", 2), "asin(2)");
        same(Double.NaN, m("acos", 2), "acos(2)");
        close(Math.PI / 4, m("atan2", 1, 1), "atan2(1, 1)");
        same(-Math.PI, m("atan2", -0.0, -0.0), "atan2(-0, -0)");
        same(Math.PI, m("atan2", 0.0, -0.0), "atan2(+0, -0)");
        same(0.0, m("sinh", 0.0), "sinh(0)");
        same(1, m("cosh", 0), "cosh(0)");
        same(1, m("tanh", Double.POSITIVE_INFINITY), "tanh(Infinity)");
        same(-0.0, m("tanh", -0.0), "tanh(-0)");
        same(0.0, m("acosh", 1), "acosh(1)");
        same(Double.NaN, m("acosh", 0.5), "acosh(0.5)");
        same(Double.POSITIVE_INFINITY, m("acosh", Double.POSITIVE_INFINITY), "acosh(Infinity)");
        close(1.3169578969248166, m("acosh", 2), "acosh(2)");
        close(691.4686750787736, m("acosh", 1e300), "acosh(1e300) does not overflow");
        same(0.0, m("asinh", 0.0), "asinh(0)");
        same(-0.0, m("asinh", -0.0), "asinh(-0)");
        close(0.881373587019543, m("asinh", 1), "asinh(1)");
        close(-0.881373587019543, m("asinh", -1), "asinh(-1)");
        close(691.4686750787736, m("asinh", 1e300), "asinh(1e300)");
        close(1e-10, m("asinh", 1e-10), "asinh of a tiny number is itself");
        same(Double.NEGATIVE_INFINITY, m("asinh", Double.NEGATIVE_INFINITY), "asinh(-Infinity)");
        same(-0.0, m("atanh", -0.0), "atanh(-0)");
        close(0.5493061443340548, m("atanh", 0.5), "atanh(0.5)");
        same(Double.POSITIVE_INFINITY, m("atanh", 1), "atanh(1)");
        same(Double.NEGATIVE_INFINITY, m("atanh", -1), "atanh(-1)");
        same(Double.NaN, m("atanh", 2), "atanh(2)");
        close(1e-10, m("atanh", 1e-10), "atanh of a tiny number is itself");
    }

    /**
     * hypot, including overflow safety and the rules for NaN and Infinity.
     * <p>
     * 넘침 방지와 NaN/Infinity 규칙을 포함한 hypot.
     */
    public void testHypot() {
        same(0.0, m("hypot"), "hypot()");
        same(5, m("hypot", 3, 4), "hypot(3, 4)");
        same(0.0, m("hypot", -0.0), "hypot(-0) is +0");
        same(7, m("hypot", -7), "hypot(-7)");
        same(Double.POSITIVE_INFINITY, m("hypot", Double.NaN, Double.POSITIVE_INFINITY), "Infinity wins over NaN");
        same(Double.POSITIVE_INFINITY, m("hypot", 1, Double.NEGATIVE_INFINITY), "-Infinity");
        same(Double.NaN, m("hypot", Double.NaN, 1), "NaN");
        close(1.4142135623730951e200, m("hypot", 1e200, 1e200), "no overflow");
        close(1.4142135623730951e-200, m("hypot", 1e-200, 1e-200), "no underflow");
        close(3.7416573867739413, m("hypot", 1, 2, 3), "three arguments");
        same(5, ((JsNumber) env.call("Math.hypot", LibEnv.s("3"), LibEnv.s("4"))).getValue(), "arguments are converted");
    }

    /**
     * max and min: empty argument lists, NaN, signed zeros and the conversion of all arguments.
     * <p>
     * max 와 min: 빈 인수 목록, NaN, 부호 있는 0, 모든 인수의 변환.
     */
    public void testMaxAndMin() {
        same(Double.NEGATIVE_INFINITY, m("max"), "max()");
        same(Double.POSITIVE_INFINITY, m("min"), "min()");
        same(3, m("max", 1, 2, 3), "max(1, 2, 3)");
        same(1, m("min", 1, 2, 3), "min(1, 2, 3)");
        same(Double.NaN, m("max", 1, Double.NaN), "max with NaN");
        same(Double.NaN, m("min", Double.NaN, 1), "min with NaN");
        same(0.0, m("max", -0.0, 0.0), "max(-0, +0) is +0");
        same(0.0, m("max", 0.0, -0.0), "max(+0, -0) is +0");
        same(-0.0, m("min", 0.0, -0.0), "min(+0, -0) is -0");
        same(-0.0, m("min", -0.0, 0.0), "min(-0, +0) is -0");
        same(3, ((JsNumber) env.call("Math.max", LibEnv.s("3"), LibEnv.n(2))).getValue(), "max('3', 2)");
        final int[] calls = new int[1];
        JsObject counting = env.realm.newObject();
        counting.defineBuiltin("valueOf", new NativeFunction(env.realm, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls[0]++;
                return Js.num(1);
            }
        }));
        same(Double.NaN, ((JsNumber) env.call("Math.max", LibEnv.n(Double.NaN), counting)).getValue(), "max(NaN, obj)");
        Check.equal(1, calls[0], "all arguments are converted even after a NaN");
    }

    /**
     * pow with the ECMAScript special cases.
     * <p>
     * ECMAScript 특수 경우를 따르는 pow.
     */
    public void testPow() {
        same(1024, m("pow", 2, 10), "pow(2, 10)");
        same(1, m("pow", Double.NaN, 0), "pow(NaN, 0)");
        same(1, m("pow", 5, -0.0), "pow(5, -0)");
        same(Double.NaN, m("pow", 1, Double.POSITIVE_INFINITY), "pow(1, Infinity) is NaN");
        same(Double.NaN, m("pow", -1, Double.NEGATIVE_INFINITY), "pow(-1, -Infinity) is NaN");
        same(Double.NaN, m("pow", 2, Double.NaN), "pow(2, NaN)");
        same(Double.POSITIVE_INFINITY, m("pow", 0, -1), "pow(0, -1)");
        same(Double.NEGATIVE_INFINITY, m("pow", -0.0, -1), "pow(-0, -1)");
        same(Double.NaN, m("pow", -8, 1.0 / 3), "pow(-8, 1/3)");
        same(0.25, m("pow", 2, -2), "pow(2, -2)");
        same(Double.POSITIVE_INFINITY, m("pow", 2, 1024), "pow overflow");
    }

    /**
     * random stays in [0, 1).
     * <p>
     * random 은 [0, 1) 안에 머문다.
     */
    public void testRandom() {
        for (int i = 0; i < 1000; i++) { // 여러 번 호출해 범위를 확인한다.
            double r = m("random");
            Check.isTrue(r >= 0 && r < 1, "random in [0, 1): " + r);
        }
    }

    /**
     * Argument conversion: strings, null, undefined, objects and symbols.
     * <p>
     * 인수 변환: 문자열, null, undefined, 객체, 심볼.
     */
    public void testArgumentConversion() {
        same(5, ((JsNumber) env.call("Math.abs", LibEnv.s("-5"))).getValue(), "abs('-5')");
        same(0.0, ((JsNumber) env.call("Math.abs", Js.NULL)).getValue(), "abs(null)");
        same(Double.NaN, ((JsNumber) env.call("Math.abs", Js.UNDEFINED)).getValue(), "abs(undefined)");
        same(Double.NaN, ((JsNumber) env.call("Math.abs")).getValue(), "abs()");
        same(1, ((JsNumber) env.call("Math.abs", Js.TRUE)).getValue(), "abs(true)");
        same(Double.NaN, ((JsNumber) env.call("Math.abs", env.obj())).getValue(), "abs({})");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Math.abs", new JsSymbol("s"));
            }
        }, "abs(symbol)");
    }
}
