package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of Number, Number.prototype and the global functions and value properties (plan item P14-01, Number and global parts).
 * <p>
 * Number, Number.prototype, 전역 함수와 전역 값 프로퍼티의 시험(계획 P14-01 의 Number 와 전역 부분).
 */
public class NumberGlobalTest {

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
    public NumberGlobalTest() {
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
     * Reads a number property by path.
     * <p>
     * 경로로 숫자 프로퍼티를 읽는다.
     *
     * @param path the path / 경로
     * @return the number / 숫자
     */
    private double num(String path) {
        return ((JsNumber) env.get(path)).getValue();
    }

    /**
     * Calls a Number.prototype method on a number and returns the string.
     * <p>
     * 숫자에 Number.prototype 메서드를 호출해 문자열을 돌려준다.
     *
     * @param x      the number / 숫자
     * @param method the method name / 메서드 이름
     * @param args   the arguments / 인수
     * @return the result text / 결과 문자열
     */
    private String fmt(double x, String method, JsValue... args) {
        return LibEnv.str(env.callWith("Number.prototype." + method, Js.num(x), args));
    }

    /**
     * The constants and their attributes.
     * <p>
     * 상수와 그 속성.
     */
    public void testConstants() {
        Check.equal(2.220446049250313E-16, num("Number.EPSILON"), "EPSILON");
        Check.equal(9007199254740991.0, num("Number.MAX_SAFE_INTEGER"), "MAX_SAFE_INTEGER");
        Check.equal(-9007199254740991.0, num("Number.MIN_SAFE_INTEGER"), "MIN_SAFE_INTEGER");
        Check.equal(Double.MAX_VALUE, num("Number.MAX_VALUE"), "MAX_VALUE");
        Check.equal(Double.MIN_VALUE, num("Number.MIN_VALUE"), "MIN_VALUE");
        Check.isTrue(Double.isNaN(num("Number.NaN")), "NaN");
        Check.equal(Double.POSITIVE_INFINITY, num("Number.POSITIVE_INFINITY"), "POSITIVE_INFINITY");
        Check.equal(Double.NEGATIVE_INFINITY, num("Number.NEGATIVE_INFINITY"), "NEGATIVE_INFINITY");
        String[] names = { "EPSILON", "MAX_SAFE_INTEGER", "MAX_VALUE", "MIN_SAFE_INTEGER", "MIN_VALUE", "NaN", "NEGATIVE_INFINITY", "POSITIVE_INFINITY" };
        JsObject ctor = env.fn("Number");
        for (int i = 0; i < names.length; i++) { // 상수마다 writable, enumerable, configurable 이 모두 false 인지 확인한다.
            PropertyDescriptor d = ctor.getOwnProperty(names[i]);
            Check.isFalse(d.isWritableTrue() || d.isEnumerableTrue() || d.isConfigurableTrue(), "Number." + names[i] + " attributes");
        }
    }

    /**
     * Number() conversions and wrapper objects.
     * <p>
     * Number() 변환과 래퍼 객체.
     */
    public void testNumberFunction() {
        Check.equal(0.0, ((JsNumber) env.call("Number")).getValue(), "Number()");
        Check.equal(12.0, ((JsNumber) env.call("Number", LibEnv.s(" 12 "))).getValue(), "string with spaces");
        Check.isTrue(Double.isNaN(((JsNumber) env.call("Number", LibEnv.s("abc"))).getValue()), "garbage");
        Check.equal(1.0, ((JsNumber) env.call("Number", Js.TRUE)).getValue(), "true");
        Check.equal(0.0, ((JsNumber) env.call("Number", Js.NULL)).getValue(), "null");
        Check.isTrue(Double.isNaN(((JsNumber) env.call("Number", Js.UNDEFINED)).getValue()), "undefined");
        Check.equal(16.0, ((JsNumber) env.call("Number", LibEnv.s("0x10"))).getValue(), "hex string");
        Check.equal(5.0, ((JsNumber) env.call("Number", LibEnv.s("0b101"))).getValue(), "binary string");
        Check.equal(8.0, ((JsNumber) env.call("Number", LibEnv.s("0o10"))).getValue(), "octal string");
        JsObject w = env.construct("Number", LibEnv.s("42"));
        Check.equal("Number", w.className(), "wrapper class");
        Check.equal(42.0, ((JsNumber) env.callWith("Number.prototype.valueOf", w)).getValue(), "valueOf");
        Check.equal(0.0, ((JsNumber) env.callWith("Number.prototype.valueOf", env.construct("Number"))).getValue(), "new Number()");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Number", new JsSymbol("s"));
            }
        }, "Number(symbol)");
        Check.equal(1.0, num("Number.length"), "length");
        Check.equal(0.0, ((JsNumber) env.callWith("Number.prototype.valueOf", env.get("Number.prototype"))).getValue(), "Number.prototype is +0");
    }

    /**
     * Number.isFinite, isInteger, isNaN, isSafeInteger and the identity of parseInt/parseFloat.
     * <p>
     * Number.isFinite, isInteger, isNaN, isSafeInteger 와 parseInt/parseFloat 의 동일성.
     */
    public void testStaticPredicates() {
        Check.same(Js.TRUE, env.call("Number.isFinite", LibEnv.n(1)), "isFinite(1)");
        Check.same(Js.FALSE, env.call("Number.isFinite", LibEnv.s("1")), "isFinite does not convert");
        Check.same(Js.FALSE, env.call("Number.isFinite", LibEnv.n(Double.POSITIVE_INFINITY)), "isFinite(Infinity)");
        Check.same(Js.TRUE, env.call("Number.isInteger", LibEnv.n(5)), "isInteger(5)");
        Check.same(Js.FALSE, env.call("Number.isInteger", LibEnv.n(5.5)), "isInteger(5.5)");
        Check.same(Js.TRUE, env.call("Number.isInteger", LibEnv.n(1e300)), "isInteger(1e300)");
        Check.same(Js.FALSE, env.call("Number.isInteger", LibEnv.n(Double.POSITIVE_INFINITY)), "isInteger(Infinity)");
        Check.same(Js.FALSE, env.call("Number.isInteger", LibEnv.s("5")), "isInteger('5')");
        Check.same(Js.TRUE, env.call("Number.isNaN", LibEnv.n(Double.NaN)), "isNaN(NaN)");
        Check.same(Js.FALSE, env.call("Number.isNaN", LibEnv.s("NaN")), "isNaN does not convert");
        Check.same(Js.TRUE, env.call("Number.isSafeInteger", LibEnv.n(9007199254740991.0)), "isSafeInteger(2^53-1)");
        Check.same(Js.FALSE, env.call("Number.isSafeInteger", LibEnv.n(9007199254740992.0)), "isSafeInteger(2^53)");
        Check.same(env.get("parseInt"), env.get("Number.parseInt"), "Number.parseInt is parseInt");
        Check.same(env.get("parseFloat"), env.get("Number.parseFloat"), "Number.parseFloat is parseFloat");
    }

    /**
     * Number.prototype.toString(radix) with several radixes, special values and range errors.
     * <p>
     * 여러 진법, 특수 값, 범위 오류를 포함한 Number.prototype.toString(radix).
     */
    public void testToStringRadix() {
        Check.equal("255", fmt(255, "toString"), "default radix");
        Check.equal("ff", fmt(255, "toString", LibEnv.n(16)), "hex");
        Check.equal("-11111111", fmt(-255, "toString", LibEnv.n(2)), "negative binary");
        Check.equal("0.1", fmt(0.5, "toString", LibEnv.n(2)), "binary fraction");
        Check.equal("zz", fmt(1295, "toString", LibEnv.n(36)), "radix 36");
        Check.equal("NaN", fmt(Double.NaN, "toString", LibEnv.n(2)), "NaN");
        Check.equal("-Infinity", fmt(Double.NEGATIVE_INFINITY, "toString", LibEnv.n(16)), "-Infinity");
        Check.equal("0", fmt(-0.0, "toString"), "-0 prints 0");
        Check.equal("100", fmt(100, "toString", Js.UNDEFINED), "undefined radix");
        Check.equal("1e+21", fmt(1e21, "toString"), "exponent form");
        Check.equal("1e-7", fmt(1e-7, "toString"), "small exponent form");
        Check.equal("0.000001", fmt(1e-6, "toString"), "small fixed form");
        Check.equal("5e-324", fmt(Double.MIN_VALUE, "toString"), "smallest denormal (shortest round trip)");
        Check.equal("1.7976931348623157e+308", fmt(Double.MAX_VALUE, "toString"), "largest double");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toString", LibEnv.n(1));
            }
        }, "radix 1");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toString", LibEnv.n(37));
            }
        }, "radix 37");
    }

    /**
     * Number.prototype.toFixed with rounding, padding and range errors.
     * <p>
     * 반올림, 자릿수 채움, 범위 오류를 포함한 Number.prototype.toFixed.
     */
    public void testToFixed() {
        Check.equal("3", fmt(2.5, "toFixed"), "toFixed() rounds half up");
        Check.equal("10.00", fmt(10, "toFixed", LibEnv.n(2)), "padding");
        Check.equal("1.00", fmt(1.005, "toFixed", LibEnv.n(2)), "1.005 is below the tie in binary");
        Check.equal("1.4", fmt(1.45, "toFixed", LibEnv.n(1)), "1.45 is below the tie in binary");
        Check.equal("-2", fmt(-1.5, "toFixed", LibEnv.n(0)), "negative half rounds away from zero");
        Check.equal("-0.00", fmt(-0.001, "toFixed", LibEnv.n(2)), "negative that rounds to zero keeps the sign");
        Check.equal("0.00", fmt(-0.0, "toFixed", LibEnv.n(2)), "-0 has no sign");
        Check.equal("1e+21", fmt(1e21, "toFixed", LibEnv.n(2)), "large numbers use ToString");
        Check.equal("NaN", fmt(Double.NaN, "toFixed", LibEnv.n(2)), "NaN");
        Check.equal("123456789012345683968.00", fmt(123456789012345680000.0, "toFixed", LibEnv.n(2)), "just below 1e21: the exact binary value is printed");
        Check.equal("0.10000000000000000555", fmt(0.1, "toFixed", LibEnv.n(20)), "20 digits show the binary value");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toFixed", LibEnv.n(21));
            }
        }, "21 digits");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toFixed", LibEnv.n(-1));
            }
        }, "-1 digits");
    }

    /**
     * Number.prototype.toExponential and toPrecision with the switch to exponent form and range errors.
     * <p>
     * 지수 표기로의 전환과 범위 오류를 포함한 Number.prototype.toExponential 및 toPrecision.
     */
    public void testExponentialAndPrecision() {
        Check.equal("1.23e+5", fmt(123456, "toExponential", LibEnv.n(2)), "toExponential(2)");
        Check.equal("1.23456e+5", fmt(123456, "toExponential"), "toExponential()");
        Check.equal("0e+0", fmt(0, "toExponential"), "zero");
        Check.equal("0.00e+0", fmt(0, "toExponential", LibEnv.n(2)), "zero with digits");
        Check.equal("-1e-7", fmt(-1e-7, "toExponential"), "negative small");
        Check.equal("NaN", fmt(Double.NaN, "toExponential", LibEnv.n(2)), "NaN");
        Check.equal("Infinity", fmt(Double.POSITIVE_INFINITY, "toExponential"), "Infinity");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toExponential", LibEnv.n(21));
            }
        }, "toExponential(21)");
        Check.equal("123.5", fmt(123.456, "toPrecision", LibEnv.n(4)), "toPrecision(4)");
        Check.equal("0.00012", fmt(0.000123, "toPrecision", LibEnv.n(2)), "small number");
        Check.equal("1.2e+5", fmt(123456, "toPrecision", LibEnv.n(2)), "large number goes exponential");
        Check.equal("123.456", fmt(123.456, "toPrecision"), "undefined precision is ToString");
        Check.equal("1.0000000000000000000", fmt(1, "toPrecision", LibEnv.n(20)), "20 digits");
        Check.equal("0.0000010", fmt(1e-6, "toPrecision", LibEnv.n(2)), "1e-6 with 2 digits");
        Check.equal("1.0e-7", fmt(1e-7, "toPrecision", LibEnv.n(2)), "1e-7 with 2 digits uses exponent");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toPrecision", LibEnv.n(0));
            }
        }, "toPrecision(0)");
        LibEnv.throwsKind(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                fmt(1, "toPrecision", LibEnv.n(22));
            }
        }, "toPrecision(22)");
    }

    /**
     * Number.prototype.toLocaleString in the en-US style.
     * <p>
     * en-US 형식의 Number.prototype.toLocaleString.
     */
    public void testToLocaleString() {
        Check.equal("1,234,567.891", fmt(1234567.891, "toLocaleString"), "grouping and three fraction digits");
        Check.equal("1,234.568", fmt(1234.5678, "toLocaleString"), "rounding to three digits");
        Check.equal("0", fmt(0.0001, "toLocaleString"), "tiny fractions disappear");
        Check.equal("-1,234.5", fmt(-1234.5, "toLocaleString"), "negative");
        Check.equal("100", fmt(100, "toLocaleString"), "no grouping below 1000");
        Check.equal("1,000", fmt(1000, "toLocaleString"), "exactly 1000");
        Check.equal("NaN", fmt(Double.NaN, "toLocaleString"), "NaN");
        Check.equal("-0", fmt(-0.0, "toLocaleString"), "negative zero keeps the sign like Intl");
        Check.equal("1,000,000,000,000,000,000,000", fmt(1e21, "toLocaleString"), "huge numbers are written out");
    }

    /**
     * Receivers that are not numbers are rejected; wrappers are accepted.
     * <p>
     * 숫자가 아닌 수신 값은 거부하고 래퍼는 받아들인다.
     */
    public void testReceivers() {
        Check.equal("5", LibEnv.str(env.callWith("Number.prototype.toString", env.construct("Number", LibEnv.n(5)))), "wrapper receiver");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Number.prototype.toString", LibEnv.s("5"));
            }
        }, "string receiver");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Number.prototype.toFixed", env.obj(), LibEnv.n(1));
            }
        }, "object receiver");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Number.prototype.valueOf", Js.UNDEFINED);
            }
        }, "undefined receiver");
    }

    /**
     * parseInt and parseFloat with prefixes, radixes, rounding and conversion of the arguments.
     * <p>
     * 접두사, 진법, 반올림, 인수 변환을 포함한 parseInt 와 parseFloat.
     */
    public void testParseFunctions() {
        Check.equal(42.0, ((JsNumber) env.call("parseInt", LibEnv.s("42abc"))).getValue(), "trailing garbage");
        Check.equal(-31.0, ((JsNumber) env.call("parseInt", LibEnv.s("  -0x1f"))).getValue(), "hex prefix with sign");
        Check.equal(5.0, ((JsNumber) env.call("parseInt", LibEnv.s("101"), LibEnv.n(2))).getValue(), "radix 2");
        Check.equal(35.0, ((JsNumber) env.call("parseInt", LibEnv.s("z"), LibEnv.n(36))).getValue(), "radix 36");
        Check.equal(3.0, ((JsNumber) env.call("parseInt", LibEnv.s("11"), LibEnv.s("2"))).getValue(), "radix is converted");
        Check.equal(3.0, ((JsNumber) env.call("parseInt", LibEnv.s("11"), LibEnv.n(2.9))).getValue(), "radix goes through ToInt32");
        Check.equal(10.0, ((JsNumber) env.call("parseInt", LibEnv.s("10"), LibEnv.n(4294967306.0))).getValue(), "radix 2^32+10 is radix 10");
        Check.isTrue(Double.isNaN(((JsNumber) env.call("parseInt", LibEnv.s(""))).getValue()), "empty string");
        Check.isTrue(Double.isNaN(((JsNumber) env.call("parseInt", LibEnv.s("12"), LibEnv.n(1))).getValue()), "radix 1");
        Check.isTrue(Double.isNaN(((JsNumber) env.call("parseInt", LibEnv.s("12"), LibEnv.n(37))).getValue()), "radix 37");
        Check.equal(0.0, ((JsNumber) env.call("parseInt", LibEnv.s("0b11"))).getValue(), "no binary prefix");
        Check.equal(1112745.0, ((JsNumber) env.call("parseInt", Js.NULL, LibEnv.n(36))).getValue(), "parseInt(null, 36)");
        Check.equal(9007199254740992.0, ((JsNumber) env.call("parseInt", LibEnv.s("9007199254740993"))).getValue(), "large values are rounded correctly");
        Check.equal(5.0, ((JsNumber) env.call("parseInt", LibEnv.n(0.0000005))).getValue(), "number is converted to '5e-7' first");
        Check.equal(3.14, ((JsNumber) env.call("parseFloat", LibEnv.s("3.14abc"))).getValue(), "parseFloat trailing garbage");
        Check.equal(0.5, ((JsNumber) env.call("parseFloat", LibEnv.s(".5"))).getValue(), "leading dot");
        Check.equal(-5.0, ((JsNumber) env.call("parseFloat", LibEnv.s("-.5e1"))).getValue(), "exponent");
        Check.equal(Double.POSITIVE_INFINITY, ((JsNumber) env.call("parseFloat", LibEnv.s("Infinityx"))).getValue(), "Infinity");
        Check.isTrue(Double.isNaN(((JsNumber) env.call("parseFloat", LibEnv.s("abc"))).getValue()), "garbage");
        Check.equal(1.0, ((JsNumber) env.call("parseFloat", LibEnv.s("1e"))).getValue(), "incomplete exponent");
        Check.equal(0.0, ((JsNumber) env.call("parseFloat", LibEnv.s("0x10"))).getValue(), "parseFloat has no hex");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("parseInt", new JsSymbol("s"));
            }
        }, "parseInt(symbol)");
    }

    /**
     * isNaN and isFinite convert their argument.
     * <p>
     * isNaN 과 isFinite 는 인수를 변환한다.
     */
    public void testIsNaNAndIsFinite() {
        Check.same(Js.TRUE, env.call("isNaN", Js.UNDEFINED), "isNaN(undefined)");
        Check.same(Js.TRUE, env.call("isNaN", LibEnv.s("abc")), "isNaN('abc')");
        Check.same(Js.FALSE, env.call("isNaN", LibEnv.s("12")), "isNaN('12')");
        Check.same(Js.FALSE, env.call("isNaN", Js.NULL), "isNaN(null)");
        Check.same(Js.TRUE, env.call("isFinite", LibEnv.s("12")), "isFinite('12')");
        Check.same(Js.FALSE, env.call("isFinite", LibEnv.n(Double.POSITIVE_INFINITY)), "isFinite(Infinity)");
        Check.same(Js.FALSE, env.call("isFinite", LibEnv.n(Double.NaN)), "isFinite(NaN)");
        Check.same(Js.TRUE, env.call("isFinite", Js.NULL), "isFinite(null)");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("isNaN", new JsSymbol("s"));
            }
        }, "isNaN(symbol)");
    }

    /**
     * The URI functions with reserved characters, non-ASCII text and error cases.
     * <p>
     * 예약 문자, 비ASCII 문자, 오류 경우를 포함한 URI 함수.
     */
    public void testUriFunctions() {
        String han = "한";
        Check.equal("a%20b%26c%2F%ED%95%9C", LibEnv.str(env.call("encodeURIComponent", LibEnv.s("a b&c/" + han))), "encodeURIComponent");
        Check.equal("http://x/a%20b?q=1&r=%ED%95%9C#h", LibEnv.str(env.call("encodeURI", LibEnv.s("http://x/a b?q=1&r=" + han + "#h"))), "encodeURI keeps reserved characters");
        Check.equal(han, LibEnv.str(env.call("decodeURIComponent", LibEnv.s("%ED%95%9C"))), "decodeURIComponent");
        Check.equal("%2F", LibEnv.str(env.call("decodeURI", LibEnv.s("%2F"))), "decodeURI keeps escaped reserved characters");
        Check.equal("A", LibEnv.str(env.call("decodeURI", LibEnv.s("%41"))), "decodeURI decodes unreserved characters");
        Check.equal("/", LibEnv.str(env.call("decodeURIComponent", LibEnv.s("%2F"))), "decodeURIComponent decodes everything");
        Check.equal("%F0%9F%98%80", LibEnv.str(env.call("encodeURIComponent", LibEnv.s(new String(Character.toChars(0x1F600))))), "surrogate pair");
        Check.equal(new String(Character.toChars(0x1F600)), LibEnv.str(env.call("decodeURIComponent", LibEnv.s("%F0%9F%98%80"))), "decode a supplementary character");
        LibEnv.throwsKind(ErrorKind.URI_ERROR, new Block() {
            public void run() {
                env.call("decodeURIComponent", LibEnv.s("%"));
            }
        }, "lone percent");
        LibEnv.throwsKind(ErrorKind.URI_ERROR, new Block() {
            public void run() {
                env.call("decodeURIComponent", LibEnv.s("%E0%A4%A"));
            }
        }, "truncated sequence");
        LibEnv.throwsKind(ErrorKind.URI_ERROR, new Block() {
            public void run() {
                env.call("decodeURIComponent", LibEnv.s("%C0%80"));
            }
        }, "overlong encoding");
        LibEnv.throwsKind(ErrorKind.URI_ERROR, new Block() {
            public void run() {
                env.call("encodeURIComponent", LibEnv.s(String.valueOf((char) 0xD800)));
            }
        }, "lone surrogate");
        LibEnv.throwsKind(ErrorKind.URI_ERROR, new Block() {
            public void run() {
                env.call("encodeURI", LibEnv.s(String.valueOf((char) 0xDC00)));
            }
        }, "lone low surrogate");
    }

    /**
     * The global value properties and their attributes.
     * <p>
     * 전역 값 프로퍼티와 그 속성.
     */
    public void testGlobalValueProperties() {
        JsObject g = env.realm.getGlobalObject();
        Check.isTrue(Double.isNaN(num("NaN")), "NaN");
        Check.equal(Double.POSITIVE_INFINITY, num("Infinity"), "Infinity");
        Check.isTrue(env.get("undefined").isUndefined(), "undefined");
        String[] names = { "NaN", "Infinity", "undefined" };
        for (int i = 0; i < names.length; i++) { // 세 값 프로퍼티는 속성이 모두 false 여야 한다.
            PropertyDescriptor d = g.getOwnProperty(names[i]);
            Check.isFalse(d.isWritableTrue() || d.isEnumerableTrue() || d.isConfigurableTrue(), names[i] + " attributes");
        }
        Check.isFalse(g.set("NaN", LibEnv.n(1)), "NaN cannot be assigned");
    }
}
