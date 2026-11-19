package org.duckdns.hjow.swingwebpanel.js.num;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link NumberConversion}, {@link DecimalDigits} and {@link ExactDecimal} against well-known
 * ECMAScript results plus randomized round-trip and shortest-digit properties.
 * <p>
 * {@link NumberConversion}, {@link DecimalDigits}, {@link ExactDecimal} 의 시험. 잘 알려진 ECMAScript 결과와
 * 무작위 왕복/최단 자릿수 속성을 확인한다.
 */
public class NumberConversionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public NumberConversionTest() {
    }

    /**
     * Checks Number::toString(10) layout rules: thresholds 1e21 and 1e-7, integers, fractions, extremes.
     * <p>
     * Number::toString(10) 의 형식 규칙(1e21, 1e-7 경계, 정수, 소수, 극단 값)을 확인한다.
     */
    public void testToStringDecimal() {
        String[][] cases = {
            { "0.1", "0.1" }, { "0.30000000000000004", "0.30000000000000004" }, { "100", "100" },
            { "1e21", "1e+21" }, { "1e20", "100000000000000000000" }, { "1.2345678901234568e20", "123456789012345680000" },
            { "1e-7", "1e-7" }, { "0.000001", "0.000001" }, { "1.5e-7", "1.5e-7" }, { "5e-324", "5e-324" },
            { "1.7976931348623157e308", "1.7976931348623157e+308" }, { "-1.5", "-1.5" }, { "4.35", "4.35" },
            { "9007199254740992", "9007199254740992" }, { "123.456", "123.456" }, { "1.2e-10", "1.2e-10" },
            { "4294967296", "4294967296" }, { "1.7976931348623157e308", "1.7976931348623157e+308" },
            { "2.2250738585072014e-308", "2.2250738585072014e-308" }, { "0.5", "0.5" }, { "1e300", "1e+300" },
            { "123456789012345678", "123456789012345680" }, { "0.000123", "0.000123" } };
        // 표의 각 사례를 문자열에서 숫자로, 다시 문자열로 바꿔 비교한다.
        for (int i = 0; i < cases.length; i++) {
            double v = NumberConversion.stringToNumber(cases[i][0]);
            Check.equal(cases[i][1], NumberConversion.toString(v), "toString(" + cases[i][0] + ")");
        }
        Check.equal("0.3333333333333333", NumberConversion.toString(1.0 / 3.0), "1/3");
        Check.equal("0.30000000000000004", NumberConversion.toString(0.1 + 0.2), "0.1+0.2");
        Check.equal("NaN", NumberConversion.toString(Double.NaN), "NaN");
        Check.equal("Infinity", NumberConversion.toString(Double.POSITIVE_INFINITY), "Infinity");
        Check.equal("-Infinity", NumberConversion.toString(Double.NEGATIVE_INFINITY), "-Infinity");
        Check.equal("0", NumberConversion.toString(-0.0), "negative zero prints 0");
    }

    /**
     * Checks toString with a radix argument.
     * <p>
     * 진법 인수가 있는 toString 을 확인한다.
     */
    public void testToStringRadix() {
        Check.equal("ff", NumberConversion.toString(255, 16), "255 hex");
        Check.equal("11111111", NumberConversion.toString(255, 2), "255 binary");
        Check.equal("0.1", NumberConversion.toString(0.5, 2), "0.5 binary");
        Check.equal("-73", NumberConversion.toString(-255, 36), "-255 base36");
        Check.equal("z", NumberConversion.toString(35, 36), "35 base36");
        Check.equal("ff.8", NumberConversion.toString(255.5, 16), "255.5 hex");
        Check.equal("0.0001100110011001100110011001100110011001100110011001101", NumberConversion.toString(0.1, 2), "0.1 binary");
        Check.equal("1" + "00000000000000000000000000000000000000000000000000000", NumberConversion.toString(9007199254740992.0, 2),
                "2^53 binary");
        Check.equal("0", NumberConversion.toString(0, 7), "zero");
        Check.equal("NaN", NumberConversion.toString(Double.NaN, 2), "NaN");
        Check.equal("10", NumberConversion.toString(10, 10.9), "radix 10.9 -> ToInteger 10");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * Radix 1 is invalid.
             * <p>
             * 진법 1 은 올바르지 않다.
             */
            public void run() {
                NumberConversion.toString(1, 1);
            }
        }, "radix 1");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * Radix 37 is invalid.
             * <p>
             * 진법 37 은 올바르지 않다.
             */
            public void run() {
                NumberConversion.toString(1, 37);
            }
        }, "radix 37");
    }

    /**
     * Checks toFixed rounding (exact binary values), the 1e21 switch and range errors.
     * <p>
     * toFixed 의 반올림(정확한 2진 값), 1e21 전환, 범위 오류를 확인한다.
     */
    public void testToFixed() {
        Check.equal("1.00", NumberConversion.toFixed(1.005, 2), "1.005 is below the tie");
        Check.equal("1.4", NumberConversion.toFixed(1.45, 1), "1.45");
        Check.equal("3", NumberConversion.toFixed(2.5, 0), "tie picks larger n");
        Check.equal("-3", NumberConversion.toFixed(-2.5, 0), "negative tie");
        Check.equal("0.00", NumberConversion.toFixed(0, 2), "zero");
        Check.equal("123.5", NumberConversion.toFixed(123.456, 1), "123.456");
        Check.equal("1e+21", NumberConversion.toFixed(1e21, 2), "1e21 uses ToString");
        Check.equal("0.0000010", NumberConversion.toFixed(0.000001, 7), "1e-6");
        Check.equal("-0.00", NumberConversion.toFixed(-0.0000001, 2), "small negative keeps the sign");
        Check.equal("0.00", NumberConversion.toFixed(-0.0, 2), "-0 prints no sign");
        Check.equal("10.23", NumberConversion.toFixed(10.235, 2), "10.235");
        Check.equal("8.35", NumberConversion.toFixed(8.345, 2), "8.345");
        Check.equal("NaN", NumberConversion.toFixed(Double.NaN, 2), "NaN");
        Check.equal("Infinity", NumberConversion.toFixed(Double.POSITIVE_INFINITY, 2), "Infinity");
        Check.equal("1.00000000000000000000", NumberConversion.toFixed(1, 20), "20 digits");
        Check.equal("0.10000000000000000555", NumberConversion.toFixed(0.1, 20), "0.1 exact expansion");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * 21 digits is out of range.
             * <p>
             * 21 자리는 범위 밖이다.
             */
            public void run() {
                NumberConversion.toFixed(1, 21);
            }
        }, "toFixed(21)");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * Negative digits are out of range.
             * <p>
             * 음수 자릿수는 범위 밖이다.
             */
            public void run() {
                NumberConversion.toFixed(1, -1);
            }
        }, "toFixed(-1)");
    }

    /**
     * Checks toExponential with and without an argument.
     * <p>
     * 인수가 있는 경우와 없는 경우의 toExponential 을 확인한다.
     */
    public void testToExponential() {
        Check.equal("1.23e+5", NumberConversion.toExponential(123456, 2), "123456");
        Check.equal("0e+0", NumberConversion.toExponential(0.0), "zero undefined");
        Check.equal("0.00e+0", NumberConversion.toExponential(0.0, 2), "zero 2");
        Check.equal("1.23456e+5", NumberConversion.toExponential(123456.0), "undefined digits");
        Check.equal("1.5e-4", NumberConversion.toExponential(0.00015, 1), "0.00015");
        Check.equal("1.3e+0", NumberConversion.toExponential(1.25, 1), "tie picks larger n");
        Check.equal("-2e+0", NumberConversion.toExponential(-1.5, 0), "-1.5");
        Check.equal("1e+0", NumberConversion.toExponential(1, 0), "1");
        Check.equal("NaN", NumberConversion.toExponential(Double.NaN, 99), "NaN is checked before the range");
        Check.equal("Infinity", NumberConversion.toExponential(Double.POSITIVE_INFINITY, 99), "Infinity before the range");
        Check.equal("5e-324", NumberConversion.toExponential(Double.MIN_VALUE), "min value");
        Check.equal("1.7976931348623157e+308", NumberConversion.toExponential(Double.MAX_VALUE), "max value");
        Check.equal("1e+1", NumberConversion.toExponential(9.99, 0), "carry into a new digit");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * 21 fraction digits is out of range.
             * <p>
             * 소수 21 자리는 범위 밖이다.
             */
            public void run() {
                NumberConversion.toExponential(1, 21);
            }
        }, "toExponential(21)");
    }

    /**
     * Checks toPrecision in fixed and exponential layouts.
     * <p>
     * 고정 형식과 지수 형식의 toPrecision 을 확인한다.
     */
    public void testToPrecision() {
        Check.equal("123.5", NumberConversion.toPrecision(123.456, 4), "123.456 p4");
        Check.equal("0.00012", NumberConversion.toPrecision(0.000123, 2), "0.000123 p2");
        Check.equal("1.2e+5", NumberConversion.toPrecision(123456, 2), "123456 p2");
        Check.equal("1.00e+21", NumberConversion.toPrecision(1e21, 3), "1e21 p3");
        Check.equal("0.00", NumberConversion.toPrecision(0, 3), "zero p3");
        Check.equal("1.00e-7", NumberConversion.toPrecision(0.0000001, 3), "1e-7 p3");
        Check.equal("1.4", NumberConversion.toPrecision(1.45, 2), "1.45 p2");
        Check.equal("100", NumberConversion.toPrecision(100, 3), "100 p3");
        Check.equal("1.0e+2", NumberConversion.toPrecision(100, 2), "100 p2");
        Check.equal("100", NumberConversion.toPrecision(99.99, 3), "99.99 p3 carries");
        Check.equal("1", NumberConversion.toPrecision(1, 1), "1 p1");
        Check.equal("0.00001", NumberConversion.toPrecision(0.00001, 1), "1e-5 p1");
        Check.equal("0.000001", NumberConversion.toPrecision(0.000001, 1), "1e-6 p1 is still fixed");
        Check.equal("1e-7", NumberConversion.toPrecision(0.0000001, 1), "1e-7 p1 is exponential");
        Check.equal("123.456", NumberConversion.toPrecision(123.456), "undefined precision is ToString");
        Check.equal("NaN", NumberConversion.toPrecision(Double.NaN, 99), "NaN before range");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * Precision 0 is out of range.
             * <p>
             * 정밀도 0 은 범위 밖이다.
             */
            public void run() {
                NumberConversion.toPrecision(1, 0);
            }
        }, "toPrecision(0)");
        Check.throwsException(NumberRangeException.class, new Block() {
            /**
             * Precision 22 is out of range.
             * <p>
             * 정밀도 22 는 범위 밖이다.
             */
            public void run() {
                NumberConversion.toPrecision(1, 22);
            }
        }, "toPrecision(22)");
    }

    /**
     * Checks StringToNumber on valid and invalid literals.
     * <p>
     * 올바른/올바르지 않은 리터럴에 대한 StringToNumber 를 확인한다.
     */
    public void testStringToNumber() {
        Check.equal(12.0, Double.valueOf(NumberConversion.stringToNumber(" 12 ")), "trim");
        Check.equal(0.0, Double.valueOf(NumberConversion.stringToNumber("")), "empty");
        Check.equal(0.0, Double.valueOf(NumberConversion.stringToNumber("  \n\t ")), "blank");
        Check.equal(31.0, Double.valueOf(NumberConversion.stringToNumber("0x1F")), "hex");
        Check.equal(5.0, Double.valueOf(NumberConversion.stringToNumber("0b101")), "binary");
        Check.equal(15.0, Double.valueOf(NumberConversion.stringToNumber("0o17")), "octal");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber("-0x1")), "signed hex is NaN");
        Check.equal(1000.0, Double.valueOf(NumberConversion.stringToNumber("1e3")), "exponent");
        Check.equal(0.5, Double.valueOf(NumberConversion.stringToNumber(".5")), ".5");
        Check.equal(5.0, Double.valueOf(NumberConversion.stringToNumber("5.")), "5.");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber(".")), "dot only");
        Check.equal(0.05, Double.valueOf(NumberConversion.stringToNumber("+.5e-1")), "+.5e-1");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(NumberConversion.stringToNumber("Infinity")), "Infinity");
        Check.equal(Double.NEGATIVE_INFINITY, Double.valueOf(NumberConversion.stringToNumber("-Infinity")), "-Infinity");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber("infinity")), "case sensitive");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber("1_0")), "no separators");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber("12px")), "trailing junk");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber("0x")), "0x alone");
        Check.isTrue(Double.isNaN(NumberConversion.stringToNumber("1e")), "dangling exponent");
        Check.equal(12.0, Double.valueOf(NumberConversion.stringToNumber("" + (char) 0xA0 + "12" + (char) 0x2029)), "unicode white space");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(NumberConversion.stringToNumber("1e1000")), "overflow");
        Check.equal(0.0, Double.valueOf(NumberConversion.stringToNumber("1e-1000")), "underflow");
        Check.equal(Double.NEGATIVE_INFINITY, Double.valueOf(1.0 / NumberConversion.stringToNumber("-0")), "-0 keeps its sign");
        Check.equal(9007199254740992.0, Double.valueOf(NumberConversion.stringToNumber("9007199254740993")), "tie to even (down)");
        Check.equal(9007199254740996.0, Double.valueOf(NumberConversion.stringToNumber("9007199254740995")), "tie to even (up)");
        Check.equal(Double.MIN_NORMAL, Double.valueOf(NumberConversion.stringToNumber("2.2250738585072012e-308")), "the famous hang input");
        Check.equal(Double.longBitsToDouble(0x000FFFFFFFFFFFFFL), Double.valueOf(NumberConversion.stringToNumber("2.2250738585072011e-308")),
                "largest subnormal");
        Check.equal(Double.MIN_VALUE, Double.valueOf(NumberConversion.stringToNumber("4.9406564584124654e-324")), "min value");
        Check.equal(0.0, Double.valueOf(NumberConversion.stringToNumber("2.4703282292062327e-324")), "just below half of min value");
        Check.equal(Double.MIN_VALUE, Double.valueOf(NumberConversion.stringToNumber("2.4703282292062328e-324")), "just above half");
        Check.equal(Double.MAX_VALUE, Double.valueOf(NumberConversion.stringToNumber("1.7976931348623158e308")), "below the overflow tie");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(NumberConversion.stringToNumber("1.7976931348623159e308")), "above the overflow tie");
    }

    /**
     * Checks parseInt and parseFloat.
     * <p>
     * parseInt 와 parseFloat 를 확인한다.
     */
    public void testParseIntAndFloat() {
        Check.equal(42.0, Double.valueOf(NumberConversion.parseInt("  42abc", 0)), "parseInt prefix");
        Check.equal(31.0, Double.valueOf(NumberConversion.parseInt("0x1f", 0)), "hex prefix auto");
        Check.equal(31.0, Double.valueOf(NumberConversion.parseInt("0x1f", 16)), "hex prefix radix 16");
        Check.equal(0.0, Double.valueOf(NumberConversion.parseInt("0x1f", 10)), "no stripping for radix 10");
        Check.equal(35.0, Double.valueOf(NumberConversion.parseInt("z", 36)), "base 36");
        Check.equal(2.0, Double.valueOf(NumberConversion.parseInt("10", 2)), "binary");
        Check.isTrue(Double.isNaN(NumberConversion.parseInt("12", 1)), "radix 1");
        Check.isTrue(Double.isNaN(NumberConversion.parseInt("12", 37)), "radix 37");
        Check.isTrue(Double.isNaN(NumberConversion.parseInt("", 0)), "empty");
        Check.equal(Double.NEGATIVE_INFINITY, Double.valueOf(1.0 / NumberConversion.parseInt("-0", 0)), "-0");
        Check.equal(1.2345678901234568e29, Double.valueOf(NumberConversion.parseInt("123456789012345678901234567890", 10)), "long digits");
        Check.equal(9007199254740992.0, Double.valueOf(NumberConversion.parseInt("9007199254740993", 10)), "rounding");
        Check.equal(0.0, Double.valueOf(NumberConversion.parseInt("0b11", 0)), "binary prefix is not stripped");
        Check.equal(1.0, Double.valueOf(NumberConversion.parseInt("1e3", 0)), "no exponent in parseInt");
        Check.equal(3.14, Double.valueOf(NumberConversion.parseFloat("3.14abc")), "parseFloat prefix");
        Check.equal(0.5, Double.valueOf(NumberConversion.parseFloat(".5.5")), "second dot stops");
        Check.equal(1.0, Double.valueOf(NumberConversion.parseFloat("1e")), "dangling e");
        Check.equal(1.0, Double.valueOf(NumberConversion.parseFloat("1e+")), "dangling e+");
        Check.equal(-0.005, Double.valueOf(NumberConversion.parseFloat("-.5e-2x")), "complex prefix");
        Check.isTrue(Double.isNaN(NumberConversion.parseFloat("abc")), "no number");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(NumberConversion.parseFloat("  Infinityx")), "Infinity prefix");
        Check.equal(0.0, Double.valueOf(NumberConversion.parseFloat("0x10")), "hex is not parsed");
        Check.equal(1.0, Double.valueOf(NumberConversion.parseFloat("1.")), "trailing dot");
    }

    /**
     * Checks the integer conversion operations.
     * <p>
     * 정수 변환 연산을 확인한다.
     */
    public void testIntegerConversions() {
        Check.equal(5, NumberConversion.toInt32(4294967296.0 + 5), "2^32+5");
        Check.equal(-2147483648, NumberConversion.toInt32(2147483648.0), "2^31 wraps");
        Check.equal(-1, NumberConversion.toInt32(-1), "-1");
        Check.equal(-1, NumberConversion.toInt32(4294967295.0), "2^32-1");
        Check.equal(0, NumberConversion.toInt32(Double.NaN), "NaN");
        Check.equal(0, NumberConversion.toInt32(Double.POSITIVE_INFINITY), "Infinity");
        Check.equal(1661992960, NumberConversion.toInt32(1e20), "1e20 mod 2^32");
        Check.equal(3, NumberConversion.toInt32(3.9), "truncation");
        Check.equal(-3, NumberConversion.toInt32(-3.9), "truncation negative");
        Check.equal(4294967295L, NumberConversion.toUint32(-1), "uint32 -1");
        Check.equal(1, NumberConversion.toUint16(65537), "uint16");
        Check.equal(0.0, Double.valueOf(NumberConversion.toInteger(Double.NaN)), "ToInteger NaN");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(NumberConversion.toInteger(Double.POSITIVE_INFINITY)), "ToInteger Infinity");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(1.0 / NumberConversion.toInteger(-0.5)), "ToInteger never returns -0");
        Check.equal(9007199254740991.0, Double.valueOf(NumberConversion.toLength(1e300)), "ToLength clamp");
        Check.equal(0.0, Double.valueOf(NumberConversion.toLength(-5)), "ToLength negative");
        Check.isTrue(NumberConversion.isInteger(3.0), "isInteger");
        Check.isFalse(NumberConversion.isInteger(3.5), "isInteger fraction");
        Check.isFalse(NumberConversion.isInteger(Double.POSITIVE_INFINITY), "isInteger Infinity");
        Check.isTrue(NumberConversion.isSafeInteger(9007199254740991.0), "safe");
        Check.isFalse(NumberConversion.isSafeInteger(9007199254740992.0), "unsafe");
        Check.isTrue(NumberConversion.sameValue(Double.NaN, Double.NaN), "SameValue NaN");
        Check.isFalse(NumberConversion.sameValue(0.0, -0.0), "SameValue zeros differ");
        Check.isTrue(NumberConversion.sameValueZero(0.0, -0.0), "SameValueZero zeros equal");
    }

    /**
     * Checks the shortest digit generator on boundary values.
     * <p>
     * 경계 값에서 최단 숫자열 생성기를 확인한다.
     */
    public void testDecimalDigitsBoundaries() {
        DecimalDigits d = DecimalDigits.shortest(0.1);
        Check.equal("1", d.getDigits(), "0.1 digits");
        Check.equal(0, d.getPointPosition(), "0.1 position");
        d = DecimalDigits.shortest(123.456);
        Check.equal("123456", d.getDigits(), "123.456 digits");
        Check.equal(3, d.getPointPosition(), "123.456 position");
        d = DecimalDigits.shortest(Double.MIN_VALUE);
        Check.equal("5", d.getDigits(), "min value digits");
        Check.equal(-323, d.getPointPosition(), "min value position");
        d = DecimalDigits.shortest(Double.MAX_VALUE);
        Check.equal("17976931348623157", d.getDigits(), "max digits");
        Check.equal(309, d.getPointPosition(), "max position");
        d = DecimalDigits.shortest(9007199254740992.0);
        Check.equal("9007199254740992", d.getDigits(), "2^53 digits");
        d = DecimalDigits.shortest(1.0);
        Check.equal("1", d.getDigits(), "one digits");
        Check.equal(1, d.getPointPosition(), "one position");
        d = DecimalDigits.shortest(5e-324 * 3);
        Check.equal("15", d.getDigits(), "3 * min value is 1.5e-323");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Zero is not accepted.
             * <p>
             * 0 은 받지 않는다.
             */
            public void run() {
                DecimalDigits.shortest(0.0);
            }
        }, "zero rejected");
    }

    /**
     * Randomized properties: toString round trips through StringToNumber and is never longer than Java's digits.
     * <p>
     * 무작위 속성: toString 결과가 StringToNumber 로 되돌아오고 Java 의 자릿수보다 길지 않다.
     */
    public void testRandomRoundTripAndShortest() {
        Random rnd = new Random(20261116L);
        int checked = 0;
        // 무작위 비트 패턴 20000 개로 왕복과 최단성을 확인한다.
        for (int i = 0; i < 20000; i++) {
            double v = Double.longBitsToDouble(rnd.nextLong());
            if (Double.isNaN(v) || Double.isInfinite(v) || v == 0.0) {
                continue;
            }
            String s = NumberConversion.toString(v);
            double back = NumberConversion.stringToNumber(s);
            Check.isTrue(back == v, "round trip of " + s + " (bits " + Long.toHexString(Double.doubleToLongBits(v)) + ")");
            int ours = digitCount(NumberConversion.toString(Math.abs(v)));
            int javas = digitCount(Double.toString(Math.abs(v)));
            Check.isTrue(ours <= javas, "shortest: " + s + " vs " + Double.toString(v));
            checked++;
        }
        // 사람이 쓰는 값(적은 자릿수)도 왕복한다.
        for (int i = 0; i < 5000; i++) {
            double v = rnd.nextInt(2000000) / Math.pow(10, rnd.nextInt(12));
            String s = NumberConversion.toString(v);
            Check.isTrue(NumberConversion.stringToNumber(s) == v, "round trip of " + s);
            Check.isTrue(digitCount(s) <= 7, "short decimal stays short: " + s);
            checked++;
        }
        Check.isTrue(checked > 20000, "enough values checked");
    }

    /**
     * Randomized comparison of ExactDecimal with BigDecimal arithmetic through the exact bit pattern.
     * <p>
     * ExactDecimal 을 BigDecimal 연산으로 얻은 정확한 비트 패턴과 무작위로 비교한다.
     */
    public void testExactDecimalAgainstBigDecimal() {
        Random rnd = new Random(7L);
        // 무작위 double 의 정확한 10진 전개를 다시 double 로 바꾸면 원래 값이어야 한다.
        for (int i = 0; i < 3000; i++) {
            double v = Double.longBitsToDouble(rnd.nextLong() & 0x7FFFFFFFFFFFFFFFL);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                continue;
            }
            BigDecimal exact = new BigDecimal(v);
            double back = ExactDecimal.toDouble(exact.unscaledValue(), -exact.scale());
            Check.isTrue(back == v, "exact expansion of " + v);
        }
        // 두 double 의 중점(정확히 동점)은 짝수 가수 쪽으로 반올림되어야 한다.
        for (int i = 0; i < 500; i++) {
            long bits = (rnd.nextLong() & 0x000FFFFFFFFFFFFFL) | ((long) (1 + rnd.nextInt(2000)) << 52);
            double lo = Double.longBitsToDouble(bits);
            double hi = Double.longBitsToDouble(bits + 1);
            BigDecimal mid = new BigDecimal(lo).add(new BigDecimal(hi)).divide(new BigDecimal(2));
            double r = ExactDecimal.toDouble(mid.unscaledValue(), -mid.scale());
            double expected = (bits & 1L) == 0 ? lo : hi;
            Check.isTrue(r == expected, "tie at " + lo);
            // 중점보다 아주 조금 위/아래는 각각 hi/lo 로 간다.
            BigDecimal tiny = new BigDecimal(BigInteger.ONE, 400);
            BigDecimal above = mid.add(tiny);
            BigDecimal below = mid.subtract(tiny);
            Check.isTrue(ExactDecimal.toDouble(above.unscaledValue(), -above.scale()) == hi, "above tie at " + lo);
            Check.isTrue(ExactDecimal.toDouble(below.unscaledValue(), -below.scale()) == lo, "below tie at " + lo);
        }
    }

    /**
     * Counts significant decimal digits of a decimal or exponent string.
     * <p>
     * 10진 또는 지수 표기 문자열의 유효 숫자 수를 센다.
     *
     * @param s number text without sign / 부호 없는 숫자 문자열
     * @return count of significant digits / 유효 숫자 수
     */
    private static int digitCount(String s) {
        int e = s.indexOf('e');
        if (e < 0) {
            e = s.indexOf('E');
        }
        String m = e >= 0 ? s.substring(0, e) : s;
        StringBuilder d = new StringBuilder();
        // 소수점을 빼고 숫자만 모은다.
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            if (c >= '0' && c <= '9') {
                d.append(c);
            }
        }
        int b = 0;
        int en = d.length();
        // 앞쪽의 0 을 센 범위에서 제외한다.
        while (b < en - 1 && d.charAt(b) == '0') {
            b++;
        }
        // 끝쪽의 0 을 센 범위에서 제외한다.
        while (en > b + 1 && d.charAt(en - 1) == '0') {
            en--;
        }
        return en - b;
    }
}
