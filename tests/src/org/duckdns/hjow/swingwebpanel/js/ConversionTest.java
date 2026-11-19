package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the abstract operations in {@link Ops} (plan item P12-04): ToPrimitive, ToNumber, ToString, ToInt32,
 * equality, relational comparison and the + operator, with JavaScript (not Java) semantics.
 * <p>
 * {@link Ops} 의 추상 연산 시험(계획 P12-04): ToPrimitive, ToNumber, ToString, ToInt32, 동등 비교, 관계 비교, + 연산을
 * Java 가 아닌 JavaScript 의미로 확인한다.
 */
public class ConversionTest {

    /**
     * The realm used by the cases.
     * <p>
     * 사례가 쓰는 realm.
     */
    private final JsRealm realm = new JsRealm();

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ConversionTest() {
    }

    /**
     * Converts a string to a number with ToNumber.
     * <p>
     * ToNumber 로 문자열을 숫자로 바꾼다.
     *
     * @param s the string / 문자열
     * @return the number / 숫자
     */
    private double num(String s) {
        return Ops.toNumber(Js.str(s));
    }

    /**
     * Creates an object whose valueOf and toString return fixed values and count calls.
     * <p>
     * valueOf 와 toString 이 고정 값을 돌려주고 호출 횟수를 세는 객체를 만든다.
     *
     * @param valueOf  result of valueOf or null to return the object itself / valueOf 결과. null 이면 객체 자신을 반환
     * @param toString result of toString / toString 결과
     * @param log      receives the call order / 호출 순서를 받는 버퍼
     * @return the object / 객체
     */
    private JsObject objectWith(final JsValue valueOf, final JsValue toString, final StringBuilder log) {
        final JsObject o = realm.newObject();
        realm.method(o, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append("V");
                return valueOf == null ? thisValue : valueOf;
            }
        });
        realm.method(o, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append("S");
                return toString;
            }
        });
        return o;
    }

    /**
     * ToNumber of strings follows StringToNumber, not Java parsing.
     * <p>
     * 문자열의 ToNumber 는 Java 파싱이 아니라 StringToNumber 를 따른다.
     */
    public void testToNumberOfStrings() {
        Check.equal(12.0, Double.valueOf(num("  12  ")), "surrounding white space");
        Check.equal(0.0, Double.valueOf(num("")), "empty string is 0");
        Check.equal(0.0, Double.valueOf(num(" \t\n ")), "blank string is 0");
        Check.equal(31.0, Double.valueOf(num("0x1F")), "hex");
        Check.equal(5.0, Double.valueOf(num("0b101")), "binary");
        Check.equal(15.0, Double.valueOf(num("0o17")), "octal");
        Check.equal(1000.0, Double.valueOf(num("1e3")), "exponent");
        Check.equal(0.5, Double.valueOf(num(".5")), "leading dot");
        Check.equal(5.0, Double.valueOf(num("5.")), "trailing dot");
        Check.equal(5.0, Double.valueOf(num("+5")), "plus sign");
        Check.equal(-5.0, Double.valueOf(num("-5")), "minus sign");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(num("Infinity")), "Infinity");
        Check.equal(Double.NEGATIVE_INFINITY, Double.valueOf(num(" -Infinity ")), "-Infinity");
        Check.equal(7.0, Double.valueOf(num((char) 0xA0 + " 7 " + (char) 0xFEFF)), "NBSP and BOM are white space");
        Check.isTrue(Double.isNaN(num("infinity")), "case matters");
        Check.isTrue(Double.isNaN(num("1d")), "Java suffix d is not JavaScript");
        Check.isTrue(Double.isNaN(num("1f")), "Java suffix f is not JavaScript");
        Check.isTrue(Double.isNaN(num("1_0")), "no numeric separators in ES6");
        Check.isTrue(Double.isNaN(num("0x")), "bare prefix");
        Check.isTrue(Double.isNaN(num("-0x10")), "signed hex is not allowed");
        Check.isTrue(Double.isNaN(num("1 2")), "inner white space");
        Check.isTrue(Double.isNaN(num("NaN")), "NaN text is just invalid");
        Check.isTrue(Double.isNaN(num("e5")), "no digits");
        Check.isTrue(Double.isNaN(num("1e")), "dangling exponent");
        double nz = num("-0");
        Check.isTrue(nz == 0 && 1 / nz < 0, "-0 string keeps the sign");
    }

    /**
     * ToNumber of the other primitive types.
     * <p>
     * 그 밖의 원시 형식의 ToNumber.
     */
    public void testToNumberOfPrimitives() {
        Check.isTrue(Double.isNaN(Ops.toNumber(Js.UNDEFINED)), "undefined");
        Check.equal(0.0, Double.valueOf(Ops.toNumber(Js.NULL)), "null");
        Check.equal(1.0, Double.valueOf(Ops.toNumber(Js.TRUE)), "true");
        Check.equal(0.0, Double.valueOf(Ops.toNumber(Js.FALSE)), "false");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.toNumber(new JsSymbol("x"));
            }
        }, "Symbol throws a TypeError");
    }

    /**
     * Number to string conversion (decimal notation thresholds and exponent forms).
     * <p>
     * 숫자-문자열 변환(10진 표기 한계와 지수 표기).
     */
    public void testNumberToString() {
        Check.equal("0", Ops.toString(Js.num(0)), "zero");
        Check.equal("0", Ops.toString(Js.num(-0.0)), "negative zero");
        Check.equal("1", Ops.toString(Js.num(1)), "one");
        Check.equal("-1.5", Ops.toString(Js.num(-1.5)), "negative fraction");
        Check.equal("100", Ops.toString(Js.num(100)), "no exponent for 100");
        Check.equal("100000000000000000000", Ops.toString(Js.num(1e20)), "1e20 is still decimal");
        Check.equal("1e+21", Ops.toString(Js.num(1e21)), "1e21 uses an exponent");
        Check.equal("123456789012345680000", Ops.toString(Js.num(123456789012345680000.0)), "20 digit integer");
        Check.equal("0.000001", Ops.toString(Js.num(0.000001)), "1e-6 is decimal");
        Check.equal("1e-7", Ops.toString(Js.num(1e-7)), "1e-7 uses an exponent");
        Check.equal("0.30000000000000004", Ops.toString(Js.num(0.1 + 0.2)), "0.1 + 0.2");
        Check.equal("0.3333333333333333", Ops.toString(Js.num(1.0 / 3)), "one third");
        Check.equal("1.7976931348623157e+308", Ops.toString(Js.num(Double.MAX_VALUE)), "max double");
        Check.equal("NaN", Ops.toString(Js.num(Double.NaN)), "NaN");
        Check.equal("Infinity", Ops.toString(Js.num(Double.POSITIVE_INFINITY)), "Infinity");
        Check.equal("-Infinity", Ops.toString(Js.num(Double.NEGATIVE_INFINITY)), "-Infinity");
        Check.equal("true", Ops.toString(Js.TRUE), "true");
        Check.equal("undefined", Ops.toString(Js.UNDEFINED), "undefined");
        Check.equal("null", Ops.toString(Js.NULL), "null");
    }

    /**
     * Number.prototype.toString with a radix.
     * <p>
     * 진법을 지정한 Number.prototype.toString.
     */
    public void testNumberToStringRadix() {
        Check.equal("ff", NumberText.toString(255, 16), "hex");
        Check.equal("-11111111", NumberText.toString(-255, 2), "negative binary");
        Check.equal("73", NumberText.toString(255, 36), "base 36");
        Check.equal("0.1", NumberText.toString(0.5, 2), "binary fraction");
        Check.equal("1010.01", NumberText.toString(10.25, 2), "binary mixed");
    }

    /**
     * ToInt32 and ToUint32 wrap modulo 2^32.
     * <p>
     * ToInt32 와 ToUint32 는 2^32 로 감싼다.
     */
    public void testIntegerConversions() {
        Check.equal(-2147483648L, (long) Ops.toInt32(2147483648.0), "2^31 wraps to negative");
        Check.equal(0, (long) Ops.toInt32(4294967296.0), "2^32 wraps to 0");
        Check.equal(-1, (long) Ops.toInt32(4294967295.0), "2^32 - 1 is -1");
        Check.equal(1, (long) Ops.toInt32(1.9), "truncation toward zero");
        Check.equal(-1, (long) Ops.toInt32(-1.9), "truncation toward zero for negatives");
        Check.equal(0, (long) Ops.toInt32(Double.NaN), "NaN");
        Check.equal(0, (long) Ops.toInt32(Double.POSITIVE_INFINITY), "Infinity");
        Check.equal(1, (long) Ops.toInt32(4294967297.0), "2^32 + 1");
        Check.equal(4294967295L, Ops.toUint32(-1.0), "ToUint32(-1)");
        Check.equal(1, (long) Ops.toUint16(Js.num(65537)), "ToUint16");
        Check.equal(3.0, Double.valueOf(Ops.toInteger(Js.str("3.9"))), "ToInteger truncates");
        Check.equal(0.0, Double.valueOf(Ops.toInteger(Js.str("abc"))), "ToInteger of NaN is 0");
        Check.equal(0.0, Double.valueOf(Ops.toLength(Js.num(-5))), "ToLength clamps below");
        Check.equal(9007199254740991.0, Double.valueOf(Ops.toLength(Js.num(1e300))), "ToLength clamps above");
    }

    /**
     * ToBoolean truthiness table.
     * <p>
     * ToBoolean 참/거짓 표.
     */
    public void testToBoolean() {
        Check.isFalse(Ops.toBoolean(Js.str("")), "empty string");
        Check.isTrue(Ops.toBoolean(Js.str("0")), "\"0\" is truthy");
        Check.isTrue(Ops.toBoolean(Js.str("false")), "\"false\" is truthy");
        Check.isFalse(Ops.toBoolean(Js.num(0)), "0");
        Check.isFalse(Ops.toBoolean(Js.num(-0.0)), "-0");
        Check.isFalse(Ops.toBoolean(Js.num(Double.NaN)), "NaN");
        Check.isTrue(Ops.toBoolean(Js.num(-1)), "-1");
        Check.isFalse(Ops.toBoolean(Js.NULL), "null");
        Check.isFalse(Ops.toBoolean(Js.UNDEFINED), "undefined");
        Check.isTrue(Ops.toBoolean(realm.newObject()), "objects are truthy");
        Check.isTrue(Ops.toBoolean(new JsSymbol("s")), "symbols are truthy");
    }

    /**
     * ToPrimitive calls valueOf first for number/default hints and toString first for the string hint.
     * <p>
     * ToPrimitive 는 number/default 힌트에서 valueOf 를 먼저, string 힌트에서 toString 을 먼저 호출한다.
     */
    public void testToPrimitiveOrder() {
        StringBuilder log = new StringBuilder();
        JsObject o = objectWith(Js.num(5), Js.str("str"), log);
        Check.equal(5.0, Double.valueOf(Ops.toNumber(o)), "number conversion uses valueOf");
        Check.equal("V", log.toString(), "only valueOf was needed");
        log.setLength(0);
        Check.equal("str", Ops.toString(o), "string conversion uses toString");
        Check.equal("S", log.toString(), "only toString was needed");
        log.setLength(0);
        JsObject p = objectWith(null, Js.str("fallback"), log);
        Check.equal("fallback", Ops.toString(Ops.toPrimitive(p, Ops.HINT_NUMBER)), "valueOf returned an object so toString is used");
        Check.equal("VS", log.toString(), "valueOf then toString");
        log.setLength(0);
        Check.equal("5x", Ops.toString(Ops.add(objectWith(Js.num(5), Js.str("s"), log), Js.str("x"))), "default hint uses valueOf");
    }

    /**
     * A TypeError is thrown when no conversion yields a primitive; Symbol.toPrimitive wins and gets the hint.
     * <p>
     * 어떤 변환도 원시 값을 주지 못하면 TypeError. Symbol.toPrimitive 가 우선하며 힌트를 받는다.
     */
    public void testToPrimitiveFailureAndSymbolHook() {
        final JsObject bad = objectWith(null, null, new StringBuilder());
        // toString 도 객체를 반환하게 만든다.
        realm.method(bad, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
        JsException e = (JsException) Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.toNumber(bad);
            }
        }, "no primitive result");
        Check.equal(ErrorKind.TYPE_ERROR, e.getKind(), "TypeError");

        final StringBuilder hints = new StringBuilder();
        JsObject o = realm.newObject();
        realm.method(o, WellKnownSymbols.TO_PRIMITIVE, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                hints.append(Ops.toString(args[0])).append(';');
                return Js.num(9);
            }
        });
        Check.equal(9.0, Double.valueOf(Ops.toNumber(o)), "hook result");
        Ops.toString(o);
        Ops.add(o, Js.num(1));
        Check.equal("number;string;default;", hints.toString(), "hints passed to @@toPrimitive");
    }

    /**
     * Loose equality (==) table.
     * <p>
     * 느슨한 동등(==) 표.
     */
    public void testLooseEquality() {
        Check.isTrue(Ops.looseEquals(Js.str("1"), Js.num(1)), "\"1\" == 1");
        Check.isTrue(Ops.looseEquals(Js.num(1), Js.TRUE), "1 == true");
        Check.isFalse(Ops.looseEquals(Js.str("true"), Js.TRUE), "\"true\" != true");
        Check.isFalse(Ops.looseEquals(Js.NULL, Js.num(0)), "null != 0");
        Check.isFalse(Ops.looseEquals(Js.UNDEFINED, Js.num(0)), "undefined != 0");
        Check.isFalse(Ops.looseEquals(Js.NULL, Js.FALSE), "null != false");
        Check.isFalse(Ops.looseEquals(Js.num(Double.NaN), Js.num(Double.NaN)), "NaN != NaN");
        Check.isTrue(Ops.looseEquals(Js.str(""), Js.num(0)), "\"\" == 0");
        Check.isTrue(Ops.looseEquals(Js.str("0"), Js.FALSE), "\"0\" == false");
        Check.isTrue(Ops.looseEquals(objectWith(Js.num(5), Js.str("x"), new StringBuilder()), Js.num(5)), "object == number uses valueOf");
        Check.isTrue(Ops.looseEquals(objectWith(Js.num(5), Js.str("x"), new StringBuilder()), Js.str("5")), "object == string");
        JsObject o = realm.newObject();
        Check.isTrue(Ops.looseEquals(o, o), "same object");
        Check.isFalse(Ops.looseEquals(o, realm.newObject()), "different objects");
        Check.isFalse(Ops.looseEquals(o, Js.NULL), "object != null");
    }

    /**
     * Strict equality, SameValue and SameValueZero.
     * <p>
     * 엄격한 동등, SameValue, SameValueZero.
     */
    public void testStrictEqualityAndSameValue() {
        Check.isFalse(Ops.strictEquals(Js.num(1), Js.str("1")), "1 !== \"1\"");
        Check.isFalse(Ops.strictEquals(Js.num(Double.NaN), Js.num(Double.NaN)), "NaN !== NaN");
        Check.isTrue(Ops.strictEquals(Js.num(0), Js.num(-0.0)), "0 === -0");
        Check.isTrue(Ops.strictEquals(Js.str("ab"), Js.str("a" + "b")), "strings compare by value");
        Check.isTrue(Ops.sameValue(Js.num(Double.NaN), Js.num(Double.NaN)), "SameValue(NaN, NaN)");
        Check.isFalse(Ops.sameValue(Js.num(0), Js.num(-0.0)), "SameValue(0, -0)");
        Check.isTrue(Ops.sameValueZero(Js.num(0), Js.num(-0.0)), "SameValueZero(0, -0)");
        Check.isTrue(Ops.sameValueZero(Js.num(Double.NaN), Js.num(Double.NaN)), "SameValueZero(NaN, NaN)");
    }

    /**
     * Relational comparison: strings compare by code units, anything with NaN is undefined (false).
     * <p>
     * 관계 비교: 문자열은 코드 단위로 비교하고 NaN 이 끼면 undefined(거짓)이다.
     */
    public void testRelationalComparison() {
        Check.equal(1, Ops.lessThan(Js.str("a"), Js.str("b"), true), "\"a\" < \"b\"");
        Check.equal(1, Ops.lessThan(Js.str("10"), Js.str("9"), true), "\"10\" < \"9\" as strings");
        Check.equal(0, Ops.lessThan(Js.num(10), Js.str("9"), true), "10 < \"9\" numerically");
        Check.equal(-1, Ops.lessThan(Js.num(Double.NaN), Js.num(1), true), "NaN < 1");
        Check.equal(-1, Ops.lessThan(Js.str("abc"), Js.num(1), true), "\"abc\" < 1");
        Check.equal(-1, Ops.lessThan(Js.UNDEFINED, Js.num(1), true), "undefined < 1");
        Check.equal(1, Ops.lessThan(Js.NULL, Js.num(1), true), "null < 1");
        String high = new String(Character.toChars(0x1F600));
        Check.equal(1, Ops.lessThan(Js.str(high), Js.str(String.valueOf((char) 0xFFFF)), true), "UTF-16 code unit order, not code point order");
        StringBuilder log = new StringBuilder();
        Ops.lessThan(tagged("L", 1, log), tagged("R", 2, log), true);
        Check.equal("LR", log.toString(), "leftFirst=true converts the left operand first");
        log.setLength(0);
        Ops.lessThan(tagged("L", 1, log), tagged("R", 2, log), false);
        Check.equal("RL", log.toString(), "leftFirst=false converts the right operand first");
    }

    /**
     * Creates an object whose valueOf appends a tag to a log and returns a number.
     * <p>
     * valueOf 가 로그에 표식을 추가하고 숫자를 반환하는 객체를 만든다.
     *
     * @param tag   the tag / 표식
     * @param value the number returned / 반환할 숫자
     * @param log   the log / 로그
     * @return the object / 객체
     */
    private JsObject tagged(final String tag, final double value, final StringBuilder log) {
        JsObject o = realm.newObject();
        realm.method(o, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append(tag);
                return Js.num(value);
            }
        });
        return o;
    }

    /**
     * The + operator: string concatenation versus numeric addition.
     * <p>
     * + 연산: 문자열 연결과 숫자 덧셈.
     */
    public void testAddOperator() {
        Check.equal("12", Ops.toString(Ops.add(Js.num(1), Js.str("2"))), "1 + \"2\"");
        Check.equal(3.0, Double.valueOf(Ops.toNumber(Ops.add(Js.num(1), Js.num(2)))), "1 + 2");
        Check.equal("anull", Ops.toString(Ops.add(Js.str("a"), Js.NULL)), "\"a\" + null");
        Check.equal(2.0, Double.valueOf(Ops.toNumber(Ops.add(Js.TRUE, Js.num(1)))), "true + 1");
        Check.isTrue(Double.isNaN(Ops.toNumber(Ops.add(Js.UNDEFINED, Js.num(1)))), "undefined + 1");
        Check.equal(7.0, Double.valueOf(Ops.toNumber(Ops.add(objectWith(Js.num(3), Js.str("x"), new StringBuilder()), Js.num(4)))), "object uses valueOf");
    }

    /**
     * ToObject creates wrappers; ToPropertyKey converts keys.
     * <p>
     * ToObject 는 래퍼를 만들고 ToPropertyKey 는 키를 변환한다.
     */
    public void testToObjectAndToPropertyKey() {
        JsObject n = Ops.toObject(realm, Js.num(1));
        Check.equal("Number", n.className(), "number wrapper");
        Check.isTrue(n.getPrototypeOf() == realm.getNumberPrototype(), "number prototype");
        Check.equal("Boolean", Ops.toObject(realm, Js.TRUE).className(), "boolean wrapper");
        Check.equal("String", Ops.toObject(realm, Js.str("a")).className(), "string wrapper");
        Check.equal("Symbol", Ops.toObject(realm, new JsSymbol("a")).className(), "symbol wrapper");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.toObject(realm, Js.NULL);
            }
        }, "null cannot become an object");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.toObject(realm, Js.UNDEFINED);
            }
        }, "undefined cannot become an object");
        Check.equal("1.5", Ops.toPropertyKey(Js.num(1.5)), "number key");
        Check.equal("true", Ops.toPropertyKey(Js.TRUE), "boolean key");
        Check.equal("k", Ops.toPropertyKey(objectWith(null, Js.str("k"), new StringBuilder())), "object key via toString first");
    }

    /**
     * instanceof with ordinary functions, bound functions and Symbol.hasInstance.
     * <p>
     * 일반 함수, 바운드 함수, Symbol.hasInstance 를 사용한 instanceof.
     */
    public void testInstanceOf() {
        final JsObject proto = realm.newObject();
        NativeFunction f = realm.newFunction("F", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        f.defineData("prototype", proto, true, false, false);
        JsObject inst = new JsObject(proto);
        Check.isTrue(Ops.instanceOf(f, inst), "instance");
        Check.isFalse(Ops.instanceOf(f, realm.newObject()), "plain object");
        Check.isFalse(Ops.instanceOf(f, Js.num(1)), "primitives are never instances");
        BoundFunction bound = new BoundFunction(realm, f, Js.UNDEFINED, Js.NO_ARGS);
        Check.isTrue(Ops.instanceOf(bound, inst), "bound functions delegate to the target");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.instanceOf(Js.num(1), realm.newObject());
            }
        }, "right side must be an object");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.instanceOf(realm.newObject(), realm.newObject());
            }
        }, "right side must be callable");
        JsObject custom = realm.newObject();
        realm.method(custom, WellKnownSymbols.HAS_INSTANCE, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.TRUE;
            }
        });
        Check.isTrue(Ops.instanceOf(custom, Js.num(3)), "Symbol.hasInstance decides");
    }

    /**
     * Calling a non-function throws a TypeError; createListFromArrayLike reads length-based objects.
     * <p>
     * 함수가 아닌 값을 호출하면 TypeError. createListFromArrayLike 는 length 기반 객체를 읽는다.
     */
    public void testCallAndArrayLike() {
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                Ops.call(Js.num(1), Js.UNDEFINED, Js.NO_ARGS);
            }
        }, "calling a number");
        JsObject like = realm.newObject();
        like.createDataProperty("length", Js.num(2));
        like.createDataProperty("0", Js.str("a"));
        JsValue[] list = Ops.createListFromArrayLike(like);
        Check.equal(2, list.length, "length");
        Check.equal("a", Ops.toString(list[0]), "element 0");
        Check.isTrue(list[1].isUndefined(), "missing element is undefined");
    }
}
