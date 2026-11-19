package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsStringObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the JSON built-in (plan item P14-04): parsing of all value kinds and of malformed text, deep nesting, __proto__ keys, the
 * reviver walk, stringify with replacers, indentation, toJSON, wrapper objects, cycles and the depth limit.
 * <p>
 * JSON 내장 객체 시험(계획 P14-04): 모든 값 종류와 잘못된 텍스트의 해석, 깊은 중첩, __proto__ 키, reviver 순회, replacer, 들여쓰기, toJSON,
 * 래퍼 객체, 순환, 깊이 제한이 있는 stringify.
 */
public class JsonTest {

    /**
     * Environment of the current case.
     * <p>
     * 현재 사례의 환경.
     */
    private Env env;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public JsonTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        env = new Env();
    }

    /**
     * Calls JSON.parse.
     * <p>
     * JSON.parse 를 호출한다.
     *
     * @param text    JSON text / JSON 텍스트
     * @param reviver reviver or null / reviver 또는 null
     * @return the result / 결과
     */
    private JsValue parse(String text, JsValue reviver) {
        return reviver == null ? env.call("JSON.parse", env.get("JSON"), Env.s(text)) : env.call("JSON.parse", env.get("JSON"), Env.s(text), reviver);
    }

    /**
     * Calls JSON.stringify and returns the text, or null for undefined.
     * <p>
     * JSON.stringify 를 호출하여 문자열을 돌려주고, undefined 이면 null 을 돌려준다.
     *
     * @param v     the value / 값
     * @param extra optional replacer and space / 선택적인 replacer 와 space
     * @return text or null / 문자열 또는 null
     */
    private String stringify(JsValue v, JsValue... extra) {
        JsValue[] args = new JsValue[1 + extra.length];
        args[0] = v;
        System.arraycopy(extra, 0, args, 1, extra.length);
        JsValue r = env.call("JSON.stringify", env.get("JSON"), args);
        return r.isUndefined() ? null : Env.str(r);
    }


    /**
     * Returns a string of n spaces.
     * <p>
     * 공백 n 개로 된 문자열을 반환한다.
     *
     * @param n the count / 개수
     * @return the spaces / 공백
     */
    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder();
        // 요청한 개수만큼 공백을 붙인다.
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
    /**
     * Makes the wrapper prototypes usable: valueOf/toString that read the internal slot (the bare realm has no Number/String library).
     * <p>
     * 래퍼 프로토타입을 쓸 수 있게 한다: 내부 슬롯을 읽는 valueOf/toString(빈 realm 에는 Number/String 라이브러리가 없다).
     */
    private void installWrapperMethods() {
        NativeFunction.Call prim = new NativeFunction.Call() {
            /**
             * Returns the wrapped primitive.
             * <p>
             * 감싼 원시 값을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the wrapper / 래퍼
             * @param args      arguments / 인수
             * @return the primitive / 원시 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (thisValue instanceof JsStringObject) {
                    return Js.str(((JsStringObject) thisValue).getStringValue());
                }
                return ((JsPrimitiveObject) thisValue).getPrimitive();
            }
        };
        env.realm.method(env.realm.getNumberPrototype(), "valueOf", 0, prim);
        env.realm.method(env.realm.getBooleanPrototype(), "valueOf", 0, prim);
        env.realm.method(env.realm.getStringPrototype(), "toString", 0, prim);
        env.realm.method(env.realm.getStringPrototype(), "valueOf", 0, prim);
    }

    /**
     * Parsing of primitives, whitespace, escapes and number forms.
     * <p>
     * 원시 값, 공백, escape, 숫자 형태의 해석.
     */
    public void testParsePrimitives() {
        Check.same(Js.NULL, parse("null", null), "null");
        Check.same(Js.TRUE, parse(" true ", null), "true with surrounding white space");
        Check.same(Js.FALSE, parse("\t\r\nfalse\n", null), "false");
        Check.equal(42.0, Env.d(parse("42", null)), "integer");
        Check.equal(-1.5e3, Env.d(parse("-1.5e3", null)), "fraction and exponent");
        Check.equal(0.0, Env.d(parse("0", null)), "zero");
        Check.equal(Double.NEGATIVE_INFINITY, 1.0 / Env.d(parse("-0", null)), "negative zero keeps its sign");
        Check.equal(Double.POSITIVE_INFINITY, Env.d(parse("1e999", null)), "overflow gives Infinity");
        Check.equal(0.0, Env.d(parse("1e-999", null)), "underflow gives 0");
        Check.equal(0.1, Env.d(parse("0.1", null)), "decimal fractions round correctly");
        Check.equal("a\"b\\c/d\b\f\n\r\t", Env.str(parse("\"a\\\"b\\\\c\\/d\\b\\f\\n\\r\\t\"", null)), "simple escapes");
        Check.equal("\u00e9\uD55C", Env.str(parse("\"\\u00e9\\ud55c\"", null)), "unicode escapes");
        Check.equal("\ud83d\ude00", Env.str(parse("\"\\ud83d\\ude00\"", null)), "surrogate pair escapes");
        Check.equal("\ud800", Env.str(parse("\"\\ud800\"", null)), "a lone surrogate is kept");
        Check.equal("", Env.str(parse("\"\"", null)), "empty string");
    }

    /**
     * Parsing of arrays and objects: order, duplicates, nesting, __proto__.
     * <p>
     * 배열과 객체의 해석: 순서, 중복, 중첩, __proto__.
     */
    public void testParseStructures() {
        JsObject arr = (JsObject) parse("[1, [2, 3], {\"a\": []}, null]", null);
        Check.isTrue(arr.isArray(), "array");
        Check.equal(4.0, Env.d(arr.get("length")), "length");
        Check.equal(1.0, Env.d(arr.get("0")), "first element");
        Check.equal(3.0, Env.d(((JsObject) arr.get("1")).get("1")), "nested element");
        Check.equal(0.0, Env.d(((JsObject) ((JsObject) arr.get("2")).get("a")).get("length")), "empty array in object");
        Check.same(Js.NULL, arr.get("3"), "null element");
        Check.equal(0.0, Env.d(((JsObject) parse("[]", null)).get("length")), "empty array");
        JsObject obj = (JsObject) parse("{\"b\":1,\"a\":2,\"b\":3,\"2\":\"x\",\"1\":\"y\"}", null);
        Check.equal(Arrays.<Object>asList("1", "2", "b", "a"), obj.ownKeys(), "integer keys first, duplicates keep the first position");
        Check.equal(3.0, Env.d(obj.get("b")), "the last duplicate wins");
        JsObject proto = (JsObject) parse("{\"__proto__\": {\"polluted\": true}}", null);
        Check.same(env.realm.getObjectPrototype(), proto.getPrototypeOf(), "__proto__ in JSON does not change the prototype");
        Check.isTrue(proto.hasOwnProperty("__proto__"), "it becomes an own data property");
        Check.same(Js.UNDEFINED, proto.get("polluted"), "nothing is inherited from it");
        Check.equal(0, ((JsObject) parse("{}", null)).ownKeys().size(), "empty object");
    }

    /**
     * Malformed text raises SyntaxError with a position.
     * <p>
     * 잘못된 텍스트는 위치가 있는 SyntaxError 를 일으킨다.
     */
    public void testParseErrors() {
        String[] bad = { "", "  ", "{", "[1,", "[1,]", "{\"a\":1,}", "{a:1}", "'x'", "01", "1.", ".5", "+1", "NaN", "Infinity", "undefined",
            "[1 2]", "{\"a\" 1}", "{\"a\":}", "\"abc", "\"\\x\"", "\"\t\"", "tru", "nul", "1 2", "[]]", "{}}", "[}", "{]" };
        // 잘못된 텍스트마다 SyntaxError 가 나는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            final String text = bad[i];
            Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
                /**
                 * Parses the malformed text.
                 * <p>
                 * 잘못된 텍스트를 해석한다.
                 */
                public void run() {
                    parse(text, null);
                }
            }, "malformed JSON: " + text);
        }
        JsException e = Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Parses text with an unexpected token.
             * <p>
             * 예상치 못한 토큰이 있는 텍스트를 해석한다.
             */
            public void run() {
                parse("{\"a\": ]}", null);
            }
        }, "unexpected token");
        Check.contains(e.getErrorMessage(), "Unexpected token ] in JSON at position 6", "message with token and position");
        JsException end = Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Parses truncated text.
             * <p>
             * 잘린 텍스트를 해석한다.
             */
            public void run() {
                parse("[1, 2", null);
            }
        }, "truncated text");
        Check.contains(end.getErrorMessage(), "Unexpected end of JSON input", "message at the end of the input");
        Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Parses the string "undefined" (the result of converting an undefined argument).
             * <p>
             * 문자열 "undefined"(undefined 인수를 변환한 결과)를 해석한다.
             */
            public void run() {
                env.call("JSON.parse", env.get("JSON"));
            }
        }, "JSON.parse() converts undefined to the text \"undefined\"");
    }

    /**
     * Parsing is iterative: very deep nesting works.
     * <p>
     * 해석은 반복 방식이라 매우 깊은 중첩도 동작한다.
     */
    public void testParseDeepNesting() {
        int depth = 100000;
        StringBuilder sb = new StringBuilder(depth * 2);
        // 열린 대괄호와 닫는 대괄호를 깊이만큼 만든다.
        for (int i = 0; i < depth; i++) {
            sb.append('[');
        }
        // 같은 수만큼 닫는 대괄호를 붙인다.
        for (int i = 0; i < depth; i++) {
            sb.append(']');
        }
        JsValue v = parse(sb.toString(), null);
        Check.isTrue(v instanceof JsArray, "the outermost value is an array");
        int levels = 0;
        JsValue cur = v;
        // 가장 안쪽까지 내려가며 깊이를 센다.
        while (cur instanceof JsArray && ((JsArray) cur).getLength() > 0) {
            cur = ((JsArray) cur).get("0");
            levels++;
        }
        Check.equal(depth - 1, levels, "all levels were created");
    }

    /**
     * The reviver is applied bottom-up in key order, with the holder as this, and can delete or replace values.
     * <p>
     * reviver 는 키 순서로 아래에서 위로 적용되고 this 가 홀더이며 값을 삭제하거나 바꿀 수 있다.
     */
    public void testReviver() {
        final StringBuilder log = new StringBuilder();
        NativeFunction logger = new NativeFunction(env.realm, "reviver", 2, new NativeFunction.Call() {
            /**
             * Records the key and returns the value unchanged.
             * <p>
             * 키를 기록하고 값을 그대로 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the holder / 홀더
             * @param args      key and value / 키와 값
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append(Ops.toString(args[0])).append(',');
                return args[1];
            }
        });
        parse("{\"a\":[1,2],\"b\":{\"c\":3}}", logger);
        Check.equal("0,1,a,c,b,,", log.toString(), "bottom-up in key order, root last with the empty key");
        NativeFunction doubler = new NativeFunction(env.realm, "reviver", 2, new NativeFunction.Call() {
            /**
             * Doubles numbers, deletes the key "drop".
             * <p>
             * 숫자를 두 배로 하고 키 "drop" 을 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the holder / 홀더
             * @param args      key and value / 키와 값
             * @return the new value / 새 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if ("drop".equals(Ops.toString(args[0]))) {
                    return Js.UNDEFINED;
                }
                if (args[1] instanceof org.duckdns.hjow.swingwebpanel.js.JsNumber) {
                    return Js.num(((org.duckdns.hjow.swingwebpanel.js.JsNumber) args[1]).getValue() * 2);
                }
                return args[1];
            }
        });
        JsObject res = (JsObject) parse("{\"x\":1,\"drop\":2,\"arr\":[5,6]}", doubler);
        Check.equal(2.0, Env.d(res.get("x")), "number doubled");
        Check.isFalse(res.hasOwnProperty("drop"), "undefined from the reviver deletes the property");
        Check.equal(12.0, Env.d(((JsObject) res.get("arr")).get("1")), "array element doubled");
        NativeFunction holderCheck = new NativeFunction(env.realm, "reviver", 2, new NativeFunction.Call() {
            /**
             * Checks that this is the object that holds the key.
             * <p>
             * this 가 키를 가진 객체인지 확인한다.
             *
             * @param r         realm / realm
             * @param thisValue the holder / 홀더
             * @param args      key and value / 키와 값
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Check.isTrue(thisValue instanceof JsObject && ((JsObject) thisValue).hasOwnProperty(Ops.toString(args[0])), "this holds the key");
                return args[1];
            }
        });
        parse("[{\"k\":1}]", holderCheck);
        Check.equal(1.0, Env.d(parse("1", doubler)) / 2, "a primitive root is revived too");
    }

    /**
     * Stringify of primitives, quoting, and values that JSON cannot represent.
     * <p>
     * 원시 값, 인용, JSON 으로 나타낼 수 없는 값의 stringify.
     */
    public void testStringifyPrimitives() {
        Check.equal("null", stringify(Js.NULL), "null");
        Check.equal("true", stringify(Js.TRUE), "true");
        Check.equal("42", stringify(Env.n(42)), "number");
        Check.equal("0", stringify(Env.n(-0.0)), "negative zero is 0");
        Check.equal("null", stringify(Env.n(Double.NaN)), "NaN is null");
        Check.equal("null", stringify(Env.n(Double.POSITIVE_INFINITY)), "Infinity is null");
        Check.equal("1.5", stringify(Env.n(1.5)), "fraction");
        Check.equal("\"a\\\"b\\\\c\\n\\u0001\"", stringify(Env.s("a\"b\\c\n\u0001")), "quoting");
        Check.equal("\"é한" + (char) 0x2028 + "\"", stringify(Env.s("é한" + (char) 0x2028)), "non-ASCII characters are kept");
        Check.isNull(stringify(Js.UNDEFINED), "undefined gives undefined");
        Check.isNull(stringify(env.realm.newFunction("f", 0, new NativeFunction.Call() {
            /**
             * Does nothing.
             * <p>
             * 아무것도 하지 않는다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        })), "functions give undefined");
        Check.isNull(stringify(new JsSymbol("s")), "symbols give undefined");
    }

    /**
     * Object and array serialization: skipped members, null for unrepresentable array elements, key order.
     * <p>
     * 객체와 배열의 직렬화: 건너뛰는 멤버, 나타낼 수 없는 배열 요소는 null, 키 순서.
     */
    public void testStringifyStructures() {
        JsObject o = env.realm.newObject();
        o.createDataProperty("b", Env.n(1));
        o.createDataProperty("a", Js.UNDEFINED);
        o.createDataProperty("2", Env.s("x"));
        o.createDataProperty("1", Js.TRUE);
        o.createDataProperty(new JsSymbol("hidden"), Env.n(9));
        o.defineData("secret", Env.n(1), true, false, true);
        o.createDataProperty("f", env.realm.newFunction("f", 0, new NativeFunction.Call() {
            /**
             * Does nothing.
             * <p>
             * 아무것도 하지 않는다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }));
        Check.equal("{\"1\":true,\"2\":\"x\",\"b\":1}", stringify(o), "integer keys first; undefined, symbol, function and non-enumerable members are skipped");
        JsArray a = env.realm.newArray(new JsValue[] { Js.UNDEFINED, Env.n(1), Js.NULL });
        Check.equal("[null,1,null]", stringify(a), "undefined array elements become null");
        Check.equal("{}", stringify(env.realm.newObject()), "empty object");
        Check.equal("[]", stringify(env.realm.newArray()), "empty array");
        JsObject inherited = env.realm.newObject();
        JsObject base = env.realm.newObject();
        base.createDataProperty("p", Env.n(1));
        inherited.setPrototypeOf(base);
        inherited.createDataProperty("own", Env.n(2));
        Check.equal("{\"own\":2}", stringify(inherited), "inherited properties are not serialized");
    }

    /**
     * Indentation: numbers, strings, clamping, wrapper objects and empty containers.
     * <p>
     * 들여쓰기: 숫자, 문자열, 상한, 래퍼 객체, 빈 컨테이너.
     */
    public void testStringifyIndent() {
        installWrapperMethods();
        JsObject o = (JsObject) parse("{\"a\":[1,{\"b\":2}],\"c\":{},\"d\":[]}", null);
        Check.equal("{\n  \"a\": [\n    1,\n    {\n      \"b\": 2\n    }\n  ],\n  \"c\": {},\n  \"d\": []\n}", stringify(o, Js.NULL, Env.n(2)), "two spaces");
        Check.equal("{\n\t\"a\": 1\n}", stringify(parse("{\"a\":1}", null), Js.NULL, Env.s("\t")), "tab string");
        Check.equal("[\n--1\n]", stringify(parse("[1]", null), Js.NULL, Env.s("--")), "string gap");
        Check.equal("[\n" + spaces(10) + "1\n]", stringify(parse("[1]", null), Js.NULL, Env.n(100)), "number gap is clamped to 10");
        Check.equal("[\n01234567891\n]", stringify(parse("[1]", null), Js.NULL, Env.s("0123456789ABC")), "string gap is clamped to 10 characters");
        Check.equal("[1]", stringify(parse("[1]", null), Js.NULL, Env.n(0)), "zero gap");
        Check.equal("[1]", stringify(parse("[1]", null), Js.NULL, Env.n(0.9)), "fractional gap counts as zero");
        Check.equal("[\n  1\n]", stringify(parse("[1]", null), Js.NULL, Ops.toObject(env.realm, Env.n(2))), "Number object as gap");
        Check.equal("[\n 1\n]", stringify(parse("[1]", null), Js.NULL, Ops.toObject(env.realm, Env.s(" "))), "String object as gap");
    }

    /**
     * Replacer function and replacer property list.
     * <p>
     * replacer 함수와 replacer 속성 목록.
     */
    public void testStringifyReplacers() {
        installWrapperMethods();
        final StringBuilder keys = new StringBuilder();
        NativeFunction fn = new NativeFunction(env.realm, "replacer", 2, new NativeFunction.Call() {
            /**
             * Records keys, drops "skip", doubles numbers.
             * <p>
             * 키를 기록하고 "skip" 은 버리고 숫자는 두 배로 한다.
             *
             * @param r         realm / realm
             * @param thisValue the holder / 홀더
             * @param args      key and value / 키와 값
             * @return the replacement / 대체 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String k = Ops.toString(args[0]);
                keys.append(k).append('|');
                if ("skip".equals(k)) {
                    return Js.UNDEFINED;
                }
                if (args[1] instanceof org.duckdns.hjow.swingwebpanel.js.JsNumber) {
                    return Js.num(((org.duckdns.hjow.swingwebpanel.js.JsNumber) args[1]).getValue() * 2);
                }
                return args[1];
            }
        });
        Check.equal("{\"a\":2,\"c\":[4,6]}", stringify(parse("{\"a\":1,\"skip\":5,\"c\":[2,3]}", null), fn), "function replacer");
        Check.equal("|a|skip|c|0|1|", keys.toString(), "the root key is empty, then members in order, array keys are indices");
        JsObject list = env.realm.newArray(new JsValue[] { Env.s("b"), Env.n(1), Env.s("b"), Env.s("zz"), Ops.toObject(env.realm, Env.s("a")), Js.TRUE });
        Check.equal("{\"b\":2,\"1\":9,\"a\":1}", stringify(parse("{\"a\":1,\"b\":2,\"1\":9,\"c\":3}", null), list), "property list: order of the list, duplicates removed, other types ignored");
        Check.equal("[{\"a\":1}]", stringify(parse("[{\"a\":1,\"b\":2}]", null), env.realm.newArray(new JsValue[] { Env.s("a") })), "the list applies to nested objects but not to arrays");
        Check.equal("{\"a\":1}", stringify(parse("{\"a\":1}", null), Env.n(5)), "a non-callable non-array replacer is ignored");
    }

    /**
     * toJSON, Date serialization and unwrapping of wrapper objects.
     * <p>
     * toJSON, Date 직렬화, 래퍼 객체 풀기.
     */
    public void testToJsonAndWrappers() {
        installWrapperMethods();
        JsObject withToJson = env.realm.newObject();
        withToJson.createDataProperty("toJSON", env.realm.newFunction("toJSON", 1, new NativeFunction.Call() {
            /**
             * Returns a description that includes the key.
             * <p>
             * 키를 포함한 설명을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the object / 객체
             * @param args      the key / 키
             * @return replacement value / 대체 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str("json:" + Ops.toString(args[0]));
            }
        }));
        JsObject holder = env.realm.newObject();
        holder.createDataProperty("k", withToJson);
        Check.equal("{\"k\":\"json:k\"}", stringify(holder), "toJSON is called with the key");
        Check.equal("\"json:\"", stringify(withToJson), "root key is the empty string");
        JsObject date = env.construct("Date", Env.n(1456795230040.0));
        Check.equal("\"2016-03-01T01:20:30.040Z\"", stringify(date), "dates serialize through toJSON");
        Check.equal("null", stringify(env.construct("Date", Env.n(Double.NaN))), "an invalid date serializes as null");
        Check.equal("[1,\"s\",true]", stringify(env.realm.newArray(new JsValue[] { Ops.toObject(env.realm, Env.n(1)),
            Ops.toObject(env.realm, Env.s("s")), Ops.toObject(env.realm, Js.TRUE) })), "wrapper objects are unwrapped");
    }

    /**
     * Cycles raise TypeError; shared (non-cyclic) references are fine; excessive depth raises RangeError.
     * <p>
     * 순환은 TypeError 를 일으키고, 공유된(순환이 아닌) 참조는 괜찮으며, 지나친 깊이는 RangeError 를 일으킨다.
     */
    public void testCyclesAndDepth() {
        final JsObject a = env.realm.newObject();
        a.createDataProperty("self", a);
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Serializes a self-referencing object.
             * <p>
             * 자기 자신을 참조하는 객체를 직렬화한다.
             */
            public void run() {
                stringify(a);
            }
        }, "direct cycle");
        final JsObject x = env.realm.newObject();
        final JsObject y = env.realm.newObject();
        x.createDataProperty("y", y);
        y.createDataProperty("x", x);
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Serializes a two-object cycle.
             * <p>
             * 두 객체의 순환을 직렬화한다.
             */
            public void run() {
                stringify(x);
            }
        }, "indirect cycle");
        JsObject shared = env.realm.newObject();
        shared.createDataProperty("v", Env.n(1));
        JsObject twice = env.realm.newArray(new JsValue[] { shared, shared });
        Check.equal("[{\"v\":1},{\"v\":1}]", stringify(twice), "shared references are not cycles");
        JsObject deep = env.realm.newArray();
        JsObject cur = deep;
        // 허용 깊이를 넘도록 배열을 겹쳐 만든다.
        for (int i = 0; i < JsonBuiltins.MAX_DEPTH + 10; i++) {
            JsObject next = env.realm.newArray();
            ((JsArray) cur).add(next);
            cur = next;
        }
        final JsObject tooDeep = deep;
        Env.expect(ErrorKind.RANGE_ERROR, new Block() {
            /**
             * Serializes a structure deeper than the limit.
             * <p>
             * 제한보다 깊은 구조를 직렬화한다.
             */
            public void run() {
                stringify(tooDeep);
            }
        }, "depth limit");
    }

    /**
     * Shape of the JSON object itself: attributes, tag, names and lengths.
     * <p>
     * JSON 객체 자체의 모양: 속성, 태그, 이름, length.
     */
    public void testJsonObjectShape() {
        JsObject json = env.obj("JSON");
        Check.isFalse(json.isCallable(), "JSON is not a function");
        Check.same(env.realm.getObjectPrototype(), json.getPrototypeOf(), "prototype");
        Check.equal("JSON", Env.str(json.get(WellKnownSymbols.TO_STRING_TAG)), "toStringTag");
        Check.isFalse(json.getOwnProperty(WellKnownSymbols.TO_STRING_TAG).isWritableTrue(), "toStringTag is read-only");
        Check.isTrue(json.getOwnProperty(WellKnownSymbols.TO_STRING_TAG).isConfigurableTrue(), "toStringTag is configurable");
        Check.isTrue(env.realm.getGlobalObject().getOwnProperty("JSON").isWritableTrue(), "global JSON is writable");
        Check.isFalse(env.realm.getGlobalObject().getOwnProperty("JSON").isEnumerableTrue(), "global JSON is not enumerable");
        Check.equal(2.0, Env.d(env.obj("JSON.parse").get("length")), "parse length");
        Check.equal(3.0, Env.d(env.obj("JSON.stringify").get("length")), "stringify length");
        Check.equal("parse", Env.str(env.obj("JSON.parse").get("name")), "parse name");
        Check.equal("stringify", Env.str(env.obj("JSON.stringify").get("name")), "stringify name");
    }
}
