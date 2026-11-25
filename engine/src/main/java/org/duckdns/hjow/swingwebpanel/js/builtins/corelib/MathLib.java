package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.num.Float16;

/**
 * Installs the <code>Math</code> object (ECMA-262 6th edition 20.2): the eight constants, all thirty-five functions including the ES2015
 * additions (<code>acosh</code>, <code>asinh</code>, <code>atanh</code>, <code>cbrt</code>, <code>clz32</code>, <code>cosh</code>,
 * <code>expm1</code>, <code>fround</code>, <code>hypot</code>, <code>imul</code>, <code>log10</code>, <code>log1p</code>,
 * <code>log2</code>, <code>sign</code>, <code>sinh</code>, <code>tanh</code>, <code>trunc</code>) and the <code>@@toStringTag</code>.
 * Results follow the specification for the special values (NaN, infinities, negative zero). The functions the JRE does not provide
 * (<code>acosh</code>, <code>asinh</code>, <code>atanh</code>, <code>log2</code>, <code>hypot</code> without overflow, <code>round</code>)
 * are written here with numerically careful formulas.
 * <p>
 * <code>Math</code> 객체(ECMA-262 6판 20.2)를 설치한다: 상수 여덟 개, ES2015 추가분(<code>acosh</code>, <code>asinh</code>,
 * <code>atanh</code>, <code>cbrt</code>, <code>clz32</code>, <code>cosh</code>, <code>expm1</code>, <code>fround</code>,
 * <code>hypot</code>, <code>imul</code>, <code>log10</code>, <code>log1p</code>, <code>log2</code>, <code>sign</code>,
 * <code>sinh</code>, <code>tanh</code>, <code>trunc</code>)을 포함한 서른다섯 개의 함수, <code>@@toStringTag</code>. 특수 값(NaN, 무한대,
 * 음의 0)에 대한 결과는 규격을 따른다. JRE 가 제공하지 않는 함수(<code>acosh</code>, <code>asinh</code>, <code>atanh</code>,
 * <code>log2</code>, 넘침 없는 <code>hypot</code>, <code>round</code>)는 수치적으로 조심스러운 공식으로 여기서 구현한다.
 */
final class MathLib {

    /**
     * Operation code: abs.
     * <p>
     * 연산 코드: abs.
     */
    private static final int ABS = 0;

    /**
     * Operation code: acos.
     * <p>
     * 연산 코드: acos.
     */
    private static final int ACOS = 1;

    /**
     * Operation code: acosh.
     * <p>
     * 연산 코드: acosh.
     */
    private static final int ACOSH = 2;

    /**
     * Operation code: asin.
     * <p>
     * 연산 코드: asin.
     */
    private static final int ASIN = 3;

    /**
     * Operation code: asinh.
     * <p>
     * 연산 코드: asinh.
     */
    private static final int ASINH = 4;

    /**
     * Operation code: atan.
     * <p>
     * 연산 코드: atan.
     */
    private static final int ATAN = 5;

    /**
     * Operation code: atanh.
     * <p>
     * 연산 코드: atanh.
     */
    private static final int ATANH = 6;

    /**
     * Operation code: cbrt.
     * <p>
     * 연산 코드: cbrt.
     */
    private static final int CBRT = 7;

    /**
     * Operation code: ceil.
     * <p>
     * 연산 코드: ceil.
     */
    private static final int CEIL = 8;

    /**
     * Operation code: clz32.
     * <p>
     * 연산 코드: clz32.
     */
    private static final int CLZ32 = 9;

    /**
     * Operation code: cos.
     * <p>
     * 연산 코드: cos.
     */
    private static final int COS = 10;

    /**
     * Operation code: cosh.
     * <p>
     * 연산 코드: cosh.
     */
    private static final int COSH = 11;

    /**
     * Operation code: exp.
     * <p>
     * 연산 코드: exp.
     */
    private static final int EXP = 12;

    /**
     * Operation code: expm1.
     * <p>
     * 연산 코드: expm1.
     */
    private static final int EXPM1 = 13;

    /**
     * Operation code: floor.
     * <p>
     * 연산 코드: floor.
     */
    private static final int FLOOR = 14;

    /**
     * Operation code: fround.
     * <p>
     * 연산 코드: fround.
     */
    private static final int FROUND = 15;

    /**
     * Operation code: log.
     * <p>
     * 연산 코드: log.
     */
    private static final int LOG = 16;

    /**
     * Operation code: log1p.
     * <p>
     * 연산 코드: log1p.
     */
    private static final int LOG1P = 17;

    /**
     * Operation code: log10.
     * <p>
     * 연산 코드: log10.
     */
    private static final int LOG10 = 18;

    /**
     * Operation code: log2.
     * <p>
     * 연산 코드: log2.
     */
    private static final int LOG2 = 19;

    /**
     * Operation code: round.
     * <p>
     * 연산 코드: round.
     */
    private static final int ROUND = 20;

    /**
     * Operation code: sign.
     * <p>
     * 연산 코드: sign.
     */
    private static final int SIGN = 21;

    /**
     * Operation code: sin.
     * <p>
     * 연산 코드: sin.
     */
    private static final int SIN = 22;

    /**
     * Operation code: sinh.
     * <p>
     * 연산 코드: sinh.
     */
    private static final int SINH = 23;

    /**
     * Operation code: sqrt.
     * <p>
     * 연산 코드: sqrt.
     */
    private static final int SQRT = 24;

    /**
     * Operation code: tan.
     * <p>
     * 연산 코드: tan.
     */
    private static final int TAN = 25;

    /**
     * Operation code: tanh.
     * <p>
     * 연산 코드: tanh.
     */
    private static final int TANH = 26;

    /**
     * Operation code: trunc.
     * <p>
     * 연산 코드: trunc.
     */
    private static final int TRUNC = 27;

    /** Operation code: f16round. / 연산 코드: f16round. */
    private static final int F16ROUND = 28;

    /**
     * Names of the one-argument functions in the order of the operation codes.
     * <p>
     * 연산 코드 순서에 맞춘 인수 하나짜리 함수 이름.
     */
    private static final String[] UNARY_NAMES = { "abs", "acos", "acosh", "asin", "asinh", "atan", "atanh", "cbrt", "ceil", "clz32", "cos",
        "cosh", "exp", "expm1", "floor", "fround", "log", "log1p", "log10", "log2", "round", "sign", "sin", "sinh", "sqrt", "tan", "tanh",
        "trunc", "f16round" };

    /**
     * Natural logarithm of 2.
     * <p>
     * 2 의 자연로그.
     */
    private static final double LN2 = 0.6931471805599453;

    /**
     * 2 to the power 52: from this magnitude on every double is an integer.
     * <p>
     * 2 의 52 제곱: 이 크기부터는 모든 double 이 정수이다.
     */
    private static final double TWO_52 = 4503599627370496.0;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private MathLib() {
    }

    /**
     * A function of one number argument identified by an operation code.
     * <p>
     * 연산 코드로 구분하는 숫자 인수 하나짜리 함수.
     */
    private static final class Unary implements NativeFunction.Call {
        /**
         * The operation code.
         * <p>
         * 연산 코드.
         */
        private final int op;

        /**
         * Creates the behaviour for one operation.
         * <p>
         * 연산 하나의 동작을 만든다.
         *
         * @param op operation code / 연산 코드
         */
        Unary(int op) {
            this.op = op;
        }

        /**
         * Converts the argument with ToNumber and applies the operation.
         * <p>
         * 인수를 ToNumber 로 바꾸고 연산을 적용한다.
         *
         * @param realm     the realm / realm
         * @param thisValue the this value (ignored) / this 값(무시)
         * @param args      the arguments / 인수
         * @return the result / 결과
         */
        public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
            return Js.num(apply(op, Ops.toNumber(Js.arg(args, 0))));
        }
    }

    /**
     * Installs Math into the realm.
     * <p>
     * realm 에 Math 를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject math = realm.newObject();
        realm.getGlobalObject().defineBuiltin("Math", math);
        realm.setIntrinsic("%Math%", math);
        math.defineData("E", Js.num(Math.E), false, false, false);
        math.defineData("LN10", Js.num(2.302585092994046), false, false, false);
        math.defineData("LN2", Js.num(LN2), false, false, false);
        math.defineData("LOG10E", Js.num(0.4342944819032518), false, false, false);
        math.defineData("LOG2E", Js.num(1.4426950408889634), false, false, false);
        math.defineData("PI", Js.num(Math.PI), false, false, false);
        math.defineData("SQRT1_2", Js.num(0.7071067811865476), false, false, false);
        math.defineData("SQRT2", Js.num(1.4142135623730951), false, false, false);
        // 인수 하나짜리 함수는 이름 표와 연산 코드로 한꺼번에 만든다.
        for (int i = 0; i < UNARY_NAMES.length; i++) {
            realm.method(math, UNARY_NAMES[i], 1, new Unary(i));
        }
        realm.method(math, "atan2", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double y = Ops.toNumber(Js.arg(args, 0));
                double x = Ops.toNumber(Js.arg(args, 1));
                return Js.num(Math.atan2(y, x));
            }
        });
        realm.method(math, "hypot", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double[] v = new double[args.length];
                for (int i = 0; i < args.length; i++) { // 모든 인수를 먼저 숫자로 바꾼다(부수 효과 순서 유지).
                    v[i] = Ops.toNumber(args[i]);
                }
                return Js.num(hypot(v));
            }
        });
        realm.method(math, "imul", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                int a = Ops.toInt32(Js.arg(args, 0));
                int b = Ops.toInt32(Js.arg(args, 1));
                return Js.num(a * b);
            }
        });
        realm.method(math, "max", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double[] v = new double[args.length];
                for (int i = 0; i < args.length; i++) { // 모든 인수를 먼저 숫자로 바꾼다.
                    v[i] = Ops.toNumber(args[i]);
                }
                double result = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < v.length; i++) { // NaN 이 있으면 NaN, 아니면 가장 큰 값(+0 이 -0 보다 크다).
                    if (v[i] != v[i]) {
                        return Js.num(Double.NaN);
                    }
                    if (v[i] > result || (v[i] == 0 && result == 0 && 1 / result < 0)) {
                        result = v[i];
                    }
                }
                return Js.num(result);
            }
        });
        realm.method(math, "min", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double[] v = new double[args.length];
                for (int i = 0; i < args.length; i++) { // 모든 인수를 먼저 숫자로 바꾼다.
                    v[i] = Ops.toNumber(args[i]);
                }
                double result = Double.POSITIVE_INFINITY;
                for (int i = 0; i < v.length; i++) { // NaN 이 있으면 NaN, 아니면 가장 작은 값(-0 이 +0 보다 작다).
                    if (v[i] != v[i]) {
                        return Js.num(Double.NaN);
                    }
                    if (v[i] < result || (v[i] == 0 && result == 0 && 1 / v[i] < 0)) {
                        result = v[i];
                    }
                }
                return Js.num(result);
            }
        });
        realm.method(math, "pow", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double x = Ops.toNumber(Js.arg(args, 0));
                double y = Ops.toNumber(Js.arg(args, 1));
                return Js.num(pow(x, y));
            }
        });
        realm.method(math, "random", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(Math.random());
            }
        });
        math.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(JsString.of("Math"), false, false, true));
    }

    /**
     * Applies a one-argument operation.
     * <p>
     * 인수 하나짜리 연산을 적용한다.
     *
     * @param op operation code / 연산 코드
     * @param x  the argument / 인수
     * @return the result / 결과
     */
    static double apply(int op, double x) {
        switch (op) {
        case ABS:
            return Math.abs(x);
        case ACOS:
            return Math.acos(x);
        case ACOSH:
            return acosh(x);
        case ASIN:
            return Math.asin(x);
        case ASINH:
            return asinh(x);
        case ATAN:
            return Math.atan(x);
        case ATANH:
            return atanh(x);
        case CBRT:
            return Math.cbrt(x);
        case CEIL:
            return Math.ceil(x);
        case CLZ32:
            return Integer.numberOfLeadingZeros(Ops.toInt32(x));
        case COS:
            return Math.cos(x);
        case COSH:
            return Math.cosh(x);
        case EXP:
            return Math.exp(x);
        case EXPM1:
            return Math.expm1(x);
        case FLOOR:
            return Math.floor(x);
        case FROUND:
            return (double) (float) x;
        case F16ROUND:
            return Float16.decode(Float16.encode(x));
        case LOG:
            return Math.log(x);
        case LOG1P:
            return Math.log1p(x);
        case LOG10:
            return Math.log10(x);
        case LOG2:
            return log2(x);
        case ROUND:
            return round(x);
        case SIGN:
            return x != x || x == 0 ? x : (x > 0 ? 1 : -1);
        case SIN:
            return Math.sin(x);
        case SINH:
            return Math.sinh(x);
        case SQRT:
            return Math.sqrt(x);
        case TAN:
            return Math.tan(x);
        case TANH:
            return Math.tanh(x);
        case TRUNC:
            return x < 0 ? Math.ceil(x) : Math.floor(x);
        default:
            throw new IllegalArgumentException("unknown operation " + op);
        }
    }

    /**
     * Math.round (20.2.2.28): rounds half up toward positive infinity, keeping negative zero for arguments in [-0.5, -0] and exact
     * for large magnitudes (no precision loss as with floor(x + 0.5)).
     * <p>
     * Math.round (20.2.2.28): 0.5 를 양의 무한대 쪽으로 반올림한다. [-0.5, -0] 범위의 인수는 음의 0 을 유지하며 큰 값에서도 정확하다
     * (floor(x + 0.5) 같은 정밀도 손실이 없다).
     *
     * @param x the argument / 인수
     * @return the rounded value / 반올림한 값
     */
    private static double round(double x) {
        if (x != x || Double.isInfinite(x) || x == 0 || Math.abs(x) >= TWO_52) {
            return x;
        }
        double floor = Math.floor(x);
        double result = x - floor >= 0.5 ? floor + 1 : floor;
        if (result == 0 && (x < 0 || 1 / x < 0)) {
            return -0.0;
        }
        return result;
    }

    /**
     * Math.log2: exact for powers of two, otherwise the quotient of natural logarithms.
     * <p>
     * Math.log2: 2 의 거듭제곱에서는 정확하고 그 밖에는 자연로그의 비이다.
     *
     * @param x the argument / 인수
     * @return the base-2 logarithm / 밑이 2 인 로그
     */
    private static double log2(double x) {
        if (x != x || x < 0) {
            return Double.NaN;
        }
        double r = Math.log(x) / LN2;
        double n = Math.rint(r);
        // 결과가 정수에 아주 가까우면 2 의 그 거듭제곱이 x 와 정확히 같은지 확인해 정확한 정수를 돌려준다.
        if (Math.abs(r - n) < 1e-9 && n > -1075 && n < 1024 && Math.pow(2, n) == x) {
            return n;
        }
        return r;
    }

    /**
     * Math.acosh: NaN below 1, exact at 1, and a cancellation-free formula elsewhere.
     * <p>
     * Math.acosh: 1 미만은 NaN, 1 에서는 정확한 0, 그 밖에는 상쇄가 없는 공식.
     *
     * @param x the argument / 인수
     * @return the inverse hyperbolic cosine / 역쌍곡 코사인
     */
    private static double acosh(double x) {
        if (x != x || x < 1) {
            return Double.NaN;
        }
        if (Double.isInfinite(x)) {
            return x;
        }
        if (x > 268435456.0) {
            return Math.log(x) + LN2;
        }
        double t = x - 1;
        return Math.log1p(t + Math.sqrt(t * (x + 1)));
    }

    /**
     * Math.asinh: odd function, keeps the sign of zero, and uses a stable formula for small and large magnitudes.
     * <p>
     * Math.asinh: 홀함수이고 0 의 부호를 유지하며 작은 값과 큰 값 모두에서 안정적인 공식을 쓴다.
     *
     * @param x the argument / 인수
     * @return the inverse hyperbolic sine / 역쌍곡 사인
     */
    private static double asinh(double x) {
        if (x != x || x == 0 || Double.isInfinite(x)) {
            return x;
        }
        double a = Math.abs(x);
        double r;
        if (a > 268435456.0) {
            r = Math.log(a) + LN2;
        } else {
            r = Math.log1p(a + a * a / (1 + Math.sqrt(1 + a * a)));
        }
        return x < 0 ? -r : r;
    }

    /**
     * Math.atanh: NaN outside [-1, 1], infinities at the ends, and a stable formula inside; keeps the sign of zero.
     * <p>
     * Math.atanh: [-1, 1] 밖은 NaN, 양 끝은 무한대, 안쪽은 안정적인 공식이며 0 의 부호를 유지한다.
     *
     * @param x the argument / 인수
     * @return the inverse hyperbolic tangent / 역쌍곡 탄젠트
     */
    private static double atanh(double x) {
        if (x != x || Math.abs(x) > 1) {
            return Double.NaN;
        }
        if (x == 1) {
            return Double.POSITIVE_INFINITY;
        }
        if (x == -1) {
            return Double.NEGATIVE_INFINITY;
        }
        return 0.5 * Math.log1p(2 * x / (1 - x));
    }

    /**
     * Math.hypot (20.2.2.18): coerces all arguments, gives +Infinity if any is infinite, NaN if any is NaN, +0 if all are zero, and scales
     * by the largest magnitude to avoid overflow and underflow.
     * <p>
     * Math.hypot (20.2.2.18): 모든 인수를 숫자로 바꾸고, 하나라도 무한대면 +Infinity, NaN 이 있으면 NaN, 모두 0 이면 +0 이며,
     * 넘침과 언더플로를 피하려고 가장 큰 크기로 나누어 계산한다.
     *
     * @param v the numbers / 숫자들
     * @return the Euclidean norm / 유클리드 노름
     */
    private static double hypot(double[] v) {
        boolean nan = false;
        double max = 0;
        for (int i = 0; i < v.length; i++) { // 무한대와 최댓값을 먼저 찾는다(무한대는 NaN 보다 우선한다).
            double a = Math.abs(v[i]);
            if (Double.isInfinite(a)) {
                return Double.POSITIVE_INFINITY;
            }
            if (a != a) {
                nan = true;
            } else if (a > max) {
                max = a;
            }
        }
        if (nan) {
            return Double.NaN;
        }
        if (max == 0) {
            return 0;
        }
        double sum = 0;
        double compensation = 0;
        for (int i = 0; i < v.length; i++) { // 크기로 나눈 제곱을 Kahan 합으로 더한다.
            double r = v[i] / max;
            double term = r * r - compensation;
            double t = sum + term;
            compensation = (t - sum) - term;
            sum = t;
        }
        return Math.sqrt(sum) * max;
    }

    /**
     * Math.pow (20.2.2.26) with the ECMAScript rule that 1 raised to an infinite power and NaN exponents give NaN.
     * <p>
     * Math.pow (20.2.2.26). 1 의 무한 거듭제곱과 NaN 지수는 NaN 이라는 ECMAScript 규칙을 따른다.
     *
     * @param x the base / 밑
     * @param y the exponent / 지수
     * @return the power / 거듭제곱
     */
    private static double pow(double x, double y) {
        if (y != y) {
            return Double.NaN;
        }
        if (y == 0) {
            return 1;
        }
        if ((x == 1 || x == -1) && Double.isInfinite(y)) {
            return Double.NaN;
        }
        return Math.pow(x, y);
    }
}
