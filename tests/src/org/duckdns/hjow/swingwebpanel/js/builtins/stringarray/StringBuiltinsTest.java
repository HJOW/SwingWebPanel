package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
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
 * Tests of the String built-in at the Java level (ECMA-262 6th edition 21.1): constructor, static functions, all prototype methods with Unicode
 * input, pattern delegation, the Annex B HTML methods, the iterator and the documented extensions.
 * <p>
 * String 내장 객체의 Java 수준 시험(ECMA-262 6판 21.1): 생성자, 정적 함수, 유니코드 입력을 포함한 모든 프로토타입 메서드, 패턴 위임, 부속서 B 의
 * HTML 메서드, 이터레이터, 문서화한 확장.
 */
public class StringBuiltinsTest {

    /**
     * Test environment.
     * <p>
     * 시험 환경.
     */
    private Env e;

    /**
     * String.prototype of the environment.
     * <p>
     * 환경의 String.prototype.
     */
    private JsObject sp;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StringBuiltinsTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new Env();
        sp = e.realm.getStringPrototype();
    }

    /**
     * Calls a String.prototype method on a Java string.
     * <p>
     * Java 문자열에 String.prototype 메서드를 호출한다.
     *
     * @param s    the string / 문자열
     * @param name method / 메서드
     * @param args arguments / 인수
     * @return the result / 결과
     */
    private JsValue m(String s, String name, Object... args) {
        return e.call(s, name, args);
    }

    /**
     * Returns a string with one supplementary character (U+1F600).
     * <p>
     * 보충 문자 하나(U+1F600)로 된 문자열을 돌려준다.
     *
     * @return the string / 문자열
     */
    private static String emoji() {
        return new String(Character.toChars(0x1F600));
    }

    /**
     * The constructor: call form returns a primitive, construct form a String object, Symbols are described when called but rejected with new.
     * <p>
     * 생성자: 호출 형태는 원시 값을, 생성 형태는 String 객체를 돌려주며, 심볼은 호출하면 설명 문자열이 되지만 new 로는 거부된다.
     */
    public void testConstructor() {
        final JsObject string = e.global("String");
        Check.equal("", Ops.toString(Ops.call(string, Js.UNDEFINED, Js.NO_ARGS)), "String()");
        Check.equal("123", Ops.toString(Ops.call(string, Js.UNDEFINED, e.vs(123))), "String(123)");
        Check.equal("true", Ops.toString(Ops.call(string, Js.UNDEFINED, e.vs(Boolean.TRUE))), "String(true)");
        Check.equal("null", Ops.toString(Ops.call(string, Js.UNDEFINED, new JsValue[] { Js.NULL })), "String(null)");
        Check.isTrue(Ops.call(string, Js.UNDEFINED, e.vs("x")) instanceof org.duckdns.hjow.swingwebpanel.js.JsString, "the call form returns a primitive");
        final JsSymbol sym = new JsSymbol("desc");
        Check.equal("Symbol(desc)", Ops.toString(Ops.call(string, Js.UNDEFINED, new JsValue[] { sym })), "String(symbol)");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                Ops.construct(string, new JsValue[] { sym });
            }
        }), "new String(symbol)");
        JsObject obj = Ops.construct(string, e.vs("abc"));
        Check.isTrue(obj instanceof JsStringObject, "the construct form returns a String object");
        Check.same(sp, obj.getPrototypeOf(), "prototype of the String object");
        Check.equal(3, Env.i(obj.get("length")), "length of the String object");
        Check.equal("b", Ops.toString(obj.get("1")), "indexed property of the String object");
        Check.equal(1, Env.i(string.get("length")), "String.length");
        Check.same(string, sp.get("constructor"), "String.prototype.constructor");
        Check.same(sp, string.get("prototype"), "String.prototype");
    }

    /**
     * fromCharCode converts each argument with ToUint16.
     * <p>
     * fromCharCode 는 각 인수에 ToUint16 을 적용한다.
     */
    public void testFromCharCode() {
        JsObject string = e.global("String");
        Check.equal("AB", Env.str(e.call(string, "fromCharCode", 65, 66)), "A B");
        Check.equal("A", Env.str(e.call(string, "fromCharCode", 0x10041)), "wraps around 16 bits");
        Check.equal("", Env.str(e.call(string, "fromCharCode")), "no arguments");
        Check.equal(0, Env.str(e.call(string, "fromCharCode", Double.NaN)).charAt(0), "NaN becomes code unit 0");
        Check.equal(emoji(), Env.str(e.call(string, "fromCharCode", 0xD83D, 0xDE00)), "a surrogate pair stays a pair");
    }

    /**
     * fromCodePoint accepts integers 0 to 0x10FFFF only.
     * <p>
     * fromCodePoint 는 0~0x10FFFF 의 정수만 받는다.
     */
    public void testFromCodePoint() {
        final JsObject string = e.global("String");
        Check.equal(emoji(), Env.str(e.call(string, "fromCodePoint", 0x1F600)), "U+1F600");
        Check.equal("A" + emoji() + "B", Env.str(e.call(string, "fromCodePoint", 65, 0x1F600, 66)), "mixed");
        Check.equal("", Env.str(e.call(string, "fromCodePoint")), "no arguments");
        Object[] bad = { Integer.valueOf(-1), Double.valueOf(1.5), Integer.valueOf(0x110000), Double.valueOf(Double.NaN),
            Double.valueOf(Double.POSITIVE_INFINITY), "x" };
        // 범위를 벗어나거나 정수가 아닌 값은 모두 RangeError 이다.
        for (int i = 0; i < bad.length; i++) {
            final Object arg = bad[i];
            Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    e.call(string, "fromCodePoint", arg);
                }
            }), "invalid code point " + arg);
        }
    }

    /**
     * String.raw joins the raw segments with the substitutions.
     * <p>
     * String.raw 는 원본 조각과 대체 값을 번갈아 잇는다.
     */
    public void testRaw() {
        JsObject string = e.global("String");
        JsObject template = e.realm.newObject();
        template.createDataProperty("raw", e.array("a", "b", "c"));
        Check.equal("axbyc", Env.str(e.call(string, "raw", template, "x", "y")), "full substitutions");
        Check.equal("axbc", Env.str(e.call(string, "raw", template, "x")), "missing substitutions are skipped");
        JsObject empty = e.realm.newObject();
        empty.createDataProperty("raw", e.array());
        Check.equal("", Env.str(e.call(string, "raw", empty)), "no segments");
        JsObject arrayLike = e.realm.newObject();
        arrayLike.createDataProperty("raw", e.arrayLike("x\\n", "y"));
        Check.equal("x\\n1y", Env.str(e.call(string, "raw", arrayLike, 1)), "array-like raw, backslashes kept");
    }

    /**
     * charAt, charCodeAt and codePointAt with out of range positions, Hangul and surrogate pairs.
     * <p>
     * 범위를 벗어난 위치, 한글, 서로게이트 쌍을 포함한 charAt, charCodeAt, codePointAt.
     */
    public void testCharAccess() {
        String s = "가a" + emoji();
        Check.equal("가", Env.str(m(s, "charAt", 0)), "charAt(0)");
        Check.equal("a", Env.str(m(s, "charAt", 1.9)), "charAt truncates");
        Check.equal("", Env.str(m(s, "charAt", -1)), "charAt(-1)");
        Check.equal("", Env.str(m(s, "charAt", 99)), "charAt(99)");
        Check.equal("가", Env.str(m(s, "charAt", Double.NaN)), "charAt(NaN) is position 0");
        Check.equal(0xAC00, Env.i(m(s, "charCodeAt", 0)), "charCodeAt(0)");
        Check.isTrue(Double.isNaN(Ops.toNumber(m(s, "charCodeAt", 10))), "charCodeAt out of range is NaN");
        Check.equal(0xD83D, Env.i(m(s, "charCodeAt", 2)), "high surrogate");
        Check.equal(0x1F600, Env.i(m(s, "codePointAt", 2)), "codePointAt of a pair");
        Check.equal(0xDE00, Env.i(m(s, "codePointAt", 3)), "codePointAt of the low half");
        Check.isTrue(m(s, "codePointAt", 4).isUndefined(), "codePointAt out of range is undefined");
        Check.equal(0xD83D, Env.i(m(String.valueOf((char) 0xD83D), "codePointAt", 0)), "lone high surrogate");
    }

    /**
     * concat, slice, substring and substr.
     * <p>
     * concat, slice, substring, substr 시험.
     */
    public void testSlicing() {
        Check.equal("ab1truenull", Env.str(m("ab", "concat", 1, Boolean.TRUE, Js.NULL)), "concat converts arguments");
        Check.equal("한글", Env.str(m("한글", "concat")), "concat without arguments");
        Check.equal("cde", Env.str(m("abcdef", "slice", 2, -1)), "slice(2,-1)");
        Check.equal("ef", Env.str(m("abcdef", "slice", -2)), "slice(-2)");
        Check.equal("", Env.str(m("abcdef", "slice", 4, 2)), "slice with end before start");
        Check.equal("abcdef", Env.str(m("abcdef", "slice")), "slice()");
        Check.equal("bc", Env.str(m("abcdef", "substring", 3, 1)), "substring swaps its arguments");
        Check.equal("abcdef", Env.str(m("abcdef", "substring", -5, 99)), "substring clamps");
        Check.equal("", Env.str(m("abcdef", "substring", Double.NaN, Double.NaN)), "substring(NaN,NaN)");
        Check.equal("cde", Env.str(m("abcdef", "substr", 2, 3)), "substr(2,3)");
        Check.equal("ef", Env.str(m("abcdef", "substr", -2)), "substr(-2)");
        Check.equal("de", Env.str(m("abcdef", "substr", -3, 2)), "substr(-3,2)");
        Check.equal("", Env.str(m("abcdef", "substr", 2, -1)), "substr with negative length");
        Check.equal("abcdef", Env.str(m("abcdef", "substr", Double.NEGATIVE_INFINITY)), "substr(-Infinity)");
    }

    /**
     * indexOf, lastIndexOf, includes, startsWith and endsWith, including empty search strings and positions.
     * <p>
     * 빈 검색 문자열과 위치를 포함한 indexOf, lastIndexOf, includes, startsWith, endsWith.
     */
    public void testSearching() {
        Check.equal(1, Env.i(m("abcabc", "indexOf", "bc")), "indexOf");
        Check.equal(4, Env.i(m("abcabc", "indexOf", "bc", 2)), "indexOf with position");
        Check.equal(-1, Env.i(m("abc", "indexOf", "x")), "indexOf not found");
        Check.equal(2, Env.i(m("abc", "indexOf", "", 2)), "indexOf empty string at position");
        Check.equal(3, Env.i(m("abc", "indexOf", "", 99)), "indexOf empty string clamps");
        Check.equal(4, Env.i(m("abcabc", "lastIndexOf", "bc")), "lastIndexOf");
        Check.equal(1, Env.i(m("abcabc", "lastIndexOf", "bc", 3)), "lastIndexOf with position");
        Check.equal(4, Env.i(m("abcabc", "lastIndexOf", "bc", Double.NaN)), "lastIndexOf with NaN position searches from the end");
        Check.equal(3, Env.i(m("abc", "lastIndexOf", "")), "lastIndexOf empty string");
        Check.isTrue(Ops.toBoolean(m("한글 문자열", "includes", "문자")), "includes");
        Check.isFalse(Ops.toBoolean(m("abc", "includes", "b", 2)), "includes with position");
        Check.isTrue(Ops.toBoolean(m("abc", "startsWith", "ab")), "startsWith");
        Check.isTrue(Ops.toBoolean(m("abc", "startsWith", "bc", 1)), "startsWith with position");
        Check.isFalse(Ops.toBoolean(m("abc", "startsWith", "abcd")), "startsWith longer string");
        Check.isTrue(Ops.toBoolean(m("abc", "endsWith", "bc")), "endsWith");
        Check.isTrue(Ops.toBoolean(m("abc", "endsWith", "ab", 2)), "endsWith with end position");
        Check.isFalse(Ops.toBoolean(m("abc", "endsWith", "abcd")), "endsWith longer string");
        Check.isTrue(Ops.toBoolean(m("abc", "endsWith", "", 0)), "endsWith empty string");
        Check.isTrue(Ops.toBoolean(m("abc", "includes", emoji().substring(0, 0))), "includes empty string");
    }

    /**
     * includes, startsWith and endsWith reject regular expression arguments (objects whose @@match is truthy) and accept them when it is false.
     * <p>
     * includes, startsWith, endsWith 는 정규식 인수(@@match 가 참인 객체)를 거부하며 @@match 가 거짓이면 받아들인다.
     */
    public void testRegExpArgumentIsRejected() {
        final JsObject fake = e.realm.newObject();
        fake.createDataProperty(WellKnownSymbols.MATCH, Js.TRUE);
        String[] names = { "includes", "startsWith", "endsWith" };
        // 세 메서드 모두 같은 규칙으로 정규식 인수를 거부한다.
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    e.call("abc", name, fake);
                }
            }), name + " with a regular expression");
        }
        fake.createDataProperty(WellKnownSymbols.MATCH, Js.FALSE);
        fake.createDataProperty("toString", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str("b");
            }
        }));
        Check.isTrue(Ops.toBoolean(m("abc", "includes", fake)), "@@match false: the object is converted to a string");
    }

    /**
     * localeCompare is deterministic and treats canonically equivalent strings as equal.
     * <p>
     * localeCompare 는 결정적이며 정규 동치 문자열을 같다고 본다.
     */
    public void testLocaleCompare() {
        Check.equal(-1, Env.i(m("a", "localeCompare", "b")), "a < b");
        Check.equal(1, Env.i(m("b", "localeCompare", "a")), "b > a");
        Check.equal(0, Env.i(m("a", "localeCompare", "a")), "equal");
        String composed = String.valueOf((char) 0xE9);
        String decomposed = "e" + (char) 0x301;
        Check.equal(0, Env.i(m(composed, "localeCompare", decomposed)), "canonical equivalence");
        Check.equal(-1, Env.i(m("가", "localeCompare", "나")), "Hangul order");
    }

    /**
     * normalize supports the four forms, defaults to NFC and rejects other forms with a RangeError.
     * <p>
     * normalize 는 네 형식을 지원하고 기본값은 NFC 이며 다른 형식은 RangeError 로 거부한다.
     */
    public void testNormalize() {
        final String composed = String.valueOf((char) 0xE9);
        String decomposed = "e" + (char) 0x301;
        Check.equal(decomposed, Env.str(m(composed, "normalize", "NFD")), "NFD");
        Check.equal(composed, Env.str(m(decomposed, "normalize")), "default NFC");
        Check.equal(composed, Env.str(m(decomposed, "normalize", "NFC")), "NFC");
        Check.equal("A", Env.str(m(String.valueOf((char) 0xFF21), "normalize", "NFKC")), "NFKC maps full width A");
        Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(composed, "normalize", "nfc");
            }
        }), "invalid form");
        Check.equal(composed, Env.str(m(composed, "normalize", (Object) null)), "undefined form is NFC");
    }

    /**
     * repeat: counts, zero, negative and too large values.
     * <p>
     * repeat: 횟수, 0, 음수, 너무 큰 값.
     */
    public void testRepeat() {
        Check.equal("ababab", Env.str(m("ab", "repeat", 3)), "repeat(3)");
        Check.equal("", Env.str(m("ab", "repeat", 0)), "repeat(0)");
        Check.equal("", Env.str(m("ab", "repeat", Double.NaN)), "repeat(NaN)");
        Check.equal("aa", Env.str(m("a", "repeat", 2.9)), "repeat truncates");
        Check.equal("", Env.str(m("", "repeat", 1e9)), "an empty string can be repeated any number of times");
        Object[] bad = { Integer.valueOf(-1), Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(1e12) };
        // 음수, 무한대, 너무 긴 결과는 RangeError 이다.
        for (int i = 0; i < bad.length; i++) {
            final Object count = bad[i];
            Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    m("ab", "repeat", count);
                }
            }), "repeat(" + count + ")");
        }
    }

    /**
     * Case conversion with special casing, Hangul and the final sigma; the locale variants are locale independent.
     * <p>
     * 특수 대소문자 규칙, 한글, 마지막 시그마를 포함한 대소문자 변환. 로케일 변형은 로케일에 독립적이다.
     */
    public void testCaseConversion() {
        Check.equal("SS", Env.str(m(String.valueOf((char) 0xDF), "toUpperCase")), "sharp s");
        Check.equal("ABC한글", Env.str(m("abc한글", "toUpperCase")), "Hangul is unchanged");
        Check.equal("abc", Env.str(m("ABC", "toLowerCase")), "lower case");
        String sigmaWord = "" + (char) 0x391 + (char) 0x3A3;
        Check.equal("" + (char) 0x3B1 + (char) 0x3C2, Env.str(m(sigmaWord, "toLowerCase")), "final sigma");
        Check.equal("i" + (char) 0x307, Env.str(m(String.valueOf((char) 0x130), "toLowerCase")), "dotted capital I");
        Check.equal("I", Env.str(m("i", "toLocaleUpperCase")), "toLocaleUpperCase is locale independent");
        Check.equal("i", Env.str(m("I", "toLocaleLowerCase")), "toLocaleLowerCase is locale independent");
    }

    /**
     * trim removes WhiteSpace and LineTerminator characters of the specification only.
     * <p>
     * trim 은 규격의 WhiteSpace 와 LineTerminator 문자만 제거한다.
     */
    public void testTrim() {
        String ws = "" + (char) 0x9 + (char) 0xA + (char) 0xB + (char) 0xC + (char) 0xD + (char) 0x20 + (char) 0xA0 + (char) 0x1680
                + (char) 0x2000 + (char) 0x200A + (char) 0x2028 + (char) 0x2029 + (char) 0x202F + (char) 0x205F + (char) 0x3000 + (char) 0xFEFF;
        Check.equal("x y", Env.str(m(ws + "x y" + ws, "trim")), "all white space characters are trimmed");
        Check.equal((char) 0x200B + "x", Env.str(m((char) 0x200B + "x", "trim")), "zero width space is not white space");
        Check.equal("", Env.str(m(ws, "trim")), "only white space");
        Check.equal("x" + ws.substring(0, 1), Env.str(m(ws + "x" + ws.substring(0, 1), "trimStart")), "trimStart");
        Check.equal(ws.substring(0, 1) + "x", Env.str(m(ws.substring(0, 1) + "x" + ws, "trimEnd")), "trimEnd");
        Check.same(sp.get("trimStart"), sp.get("trimLeft"), "trimLeft is trimStart");
        Check.same(sp.get("trimEnd"), sp.get("trimRight"), "trimRight is trimEnd");
    }

    /**
     * split with strings, limits, the empty separator and surrogate pairs.
     * <p>
     * 문자열 구분자, 개수 제한, 빈 구분자, 서로게이트 쌍을 포함한 split.
     */
    public void testSplit() {
        Check.equal(Arrays.asList("a", "b", "c"), Env.list(m("a,b,c", "split", ",")), "split(,)");
        Check.equal(Arrays.asList("a", "b"), Env.list(m("a,b,c", "split", ",", 2)), "split with limit");
        Check.equal(Arrays.asList("a", "", "b", ""), Env.list(m("a,,b,", "split", ",")), "empty pieces are kept");
        Check.equal(Arrays.asList("a,b,c"), Env.list(m("a,b,c", "split")), "split()");
        Check.equal(Arrays.asList("a,b,c"), Env.list(m("a,b,c", "split", (Object) null)), "split(undefined)");
        Check.equal(Arrays.asList(), Env.list(m("a,b,c", "split", ",", 0)), "limit 0");
        Check.equal(Arrays.asList("a", "b", "c"), Env.list(m("abc", "split", "")), "split into code units");
        Check.equal(Arrays.asList(), Env.list(m("", "split", "")), "empty string split by empty string");
        Check.equal(Arrays.asList(""), Env.list(m("", "split", "x")), "empty string split by a separator");
        Check.equal(Arrays.asList("a", "b"), Env.list(m("a--b", "split", "--")), "multi character separator");
        Check.equal(Arrays.asList("a", "b", "c"), Env.list(m("a1b1c", "split", 1)), "separator converted with ToString");
        Check.equal(2, Env.list(m(emoji(), "split", "")).size(), "a surrogate pair is split into two code units");
        Check.equal(Arrays.asList("a", "b"), Env.list(m("a,b,c", "split", ",", 4294967298.0)), "limit is ToUint32 (2^32+2 is 2)");
    }

    /**
     * replace with string patterns: substitution patterns, a function replacer and the not-found case.
     * <p>
     * 문자열 패턴의 replace: 치환 패턴, 함수 치환, 찾지 못한 경우.
     */
    public void testReplaceWithStrings() {
        Check.equal("aXcabc", Env.str(m("abcabc", "replace", "b", "X")), "first occurrence only");
        Check.equal("abcabc", Env.str(m("abcabc", "replace", "z", "X")), "not found");
        Check.equal("a[b]cabc", Env.str(m("abcabc", "replace", "b", "[$&]")), "$&");
        Check.equal("a[a]cabc", Env.str(m("abcabc", "replace", "b", "[$`]")), "$`");
        Check.equal("a[cabc]cabc", Env.str(m("abcabc", "replace", "b", "[$']")), "$'");
        Check.equal("a$cabc", Env.str(m("abcabc", "replace", "b", "$$")), "$$");
        Check.equal("a$1cabc", Env.str(m("abcabc", "replace", "b", "$1")), "$1 without captures stays literal");
        Check.equal("a$cabc", Env.str(m("abcabc", "replace", "b", "$")), "a lone dollar");
        Check.equal("a$xcabc", Env.str(m("abcabc", "replace", "b", "$x")), "an unknown $x pattern");
        Check.equal("x", Env.str(m("nullx", "replace", Js.NULL, "")), "null is searched as the string null");
        final Object[] seen = new Object[3];
        JsValue replacer = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen[0] = Env.describe(args[0]);
                seen[1] = Env.describe(args[1]);
                seen[2] = Env.describe(args[2]);
                return Js.str("<" + args.length + ">");
            }
        });
        Check.equal("a<3>cabc", Env.str(m("abcabc", "replace", "b", replacer)), "function replacer result");
        Check.equal("b", seen[0], "matched substring");
        Check.equal("1", seen[1], "position");
        Check.equal("abcabc", seen[2], "whole string");
        final JsValue throwing = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw org.duckdns.hjow.swingwebpanel.js.JsException.typeError("replacer failed");
            }
        });
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m("abc", "replace", "b", throwing);
            }
        }), "an exception of the replacer propagates");
    }

    /**
     * Substitution patterns with captures (used by the RegExp built-in through the public helper).
     * <p>
     * 캡처가 있는 치환 패턴(RegExp 내장 객체가 public 도우미로 사용).
     */
    public void testSubstitutionWithCaptures() {
        JsValue[] caps = { Js.str("X"), Js.UNDEFINED, Js.str("Z") };
        Check.equal("X|Z|", Substitution.expand("abc", "0abc9", 1, caps, "$1|$3|$2"), "single digit captures, undefined is empty");
        Check.equal("X0", Substitution.expand("abc", "0abc9", 1, caps, "$10"), "$10 with one valid digit capture is $1 followed by 0");
        Check.equal("Z", Substitution.expand("abc", "0abc9", 1, caps, "$03"), "$03 is the third capture");
        Check.equal("$4", Substitution.expand("abc", "0abc9", 1, caps, "$4"), "a capture that does not exist stays literal");
        Check.equal("$0", Substitution.expand("abc", "0abc9", 1, caps, "$0"), "$0 stays literal");
        Check.equal("0|9", Substitution.expand("abc", "0abc9", 1, caps, "$`|$'"), "prefix and suffix");
    }

    /**
     * match, search and split delegate to the Symbol methods of an object argument; replace passes the string and the replacement.
     * <p>
     * match, search, split 은 객체 인수의 심볼 메서드에 위임하고, replace 는 문자열과 치환 값을 넘긴다.
     */
    public void testPatternMethodsDelegateToSymbolMethods() {
        final StringBuilder log = new StringBuilder();
        JsObject matcher = e.realm.newObject();
        e.realm.method(matcher, WellKnownSymbols.MATCH, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append("match(").append(Env.describe(args[0])).append(");");
                return Js.str("M");
            }
        });
        e.realm.method(matcher, WellKnownSymbols.SEARCH, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append("search(").append(Env.describe(args[0])).append(");");
                return Js.num(7);
            }
        });
        e.realm.method(matcher, WellKnownSymbols.REPLACE, 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append("replace(").append(Env.describe(args[0])).append(",").append(Env.describe(args[1])).append(");");
                return Js.str("R");
            }
        });
        e.realm.method(matcher, WellKnownSymbols.SPLIT, 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.append("split(").append(Env.describe(args[0])).append(",").append(Env.describe(args[1])).append(");");
                return Js.str("S");
            }
        });
        Check.equal("M", Env.str(m("abc", "match", matcher)), "match result");
        Check.equal(7, Env.i(m("abc", "search", matcher)), "search result");
        Check.equal("R", Env.str(m("abc", "replace", matcher, "x")), "replace result");
        Check.equal("S", Env.str(m("abc", "split", matcher, 3)), "split result");
        Check.equal("match(abc);search(abc);replace(abc,x);split(abc,3);", log.toString(), "arguments passed to the symbol methods");
    }

    /**
     * match and search with a plain string create a regular expression with the realm's RegExp constructor; without one they fail with a TypeError.
     * <p>
     * 일반 문자열로 match 와 search 를 하면 realm 의 RegExp 생성자로 정규식을 만들며, 생성자가 없으면 TypeError 로 실패한다.
     */
    public void testMatchWithStringCreatesRegExp() {
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m("abc", "match", "b");
            }
        }), "no RegExp constructor available");
        final StringBuilder log = new StringBuilder();
        NativeFunction fake = new NativeFunction(e.realm, "RegExp", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                log.append("new RegExp(").append(Env.describe(args[0])).append(");");
                JsObject rx = r.newObject();
                r.method(rx, WellKnownSymbols.MATCH, 1, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] a) {
                        log.append("@@match(").append(Env.describe(a[0])).append(");");
                        return Js.str("found");
                    }
                });
                r.method(rx, WellKnownSymbols.SEARCH, 1, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] a) {
                        log.append("@@search(").append(Env.describe(a[0])).append(");");
                        return Js.num(1);
                    }
                });
                return rx;
            }
        });
        e.realm.setIntrinsic("%RegExp%", fake);
        Check.equal("found", Env.str(m("abc", "match", "b")), "match through the created regular expression");
        Check.equal(1, Env.i(m("abc", "search", "b")), "search through the created regular expression");
        Check.equal("new RegExp(b);@@match(abc);new RegExp(b);@@search(abc);", log.toString(), "the string is passed to the constructor, the subject to the method");
    }

    /**
     * Annex B HTML methods build markup and escape quotes in attribute values.
     * <p>
     * 부속서 B HTML 메서드는 마크업을 만들고 속성 값의 따옴표를 이스케이프한다.
     */
    public void testHtmlMethods() {
        Check.equal("<a name=\"a&quot;b\">x</a>", Env.str(m("x", "anchor", "a\"b")), "anchor");
        Check.equal("<big>x</big>", Env.str(m("x", "big")), "big");
        Check.equal("<blink>x</blink>", Env.str(m("x", "blink")), "blink");
        Check.equal("<b>x</b>", Env.str(m("x", "bold")), "bold");
        Check.equal("<tt>x</tt>", Env.str(m("x", "fixed")), "fixed");
        Check.equal("<font color=\"red\">x</font>", Env.str(m("x", "fontcolor", "red")), "fontcolor");
        Check.equal("<font size=\"3\">x</font>", Env.str(m("x", "fontsize", 3)), "fontsize");
        Check.equal("<i>x</i>", Env.str(m("x", "italics")), "italics");
        Check.equal("<a href=\"http://a/\">x</a>", Env.str(m("x", "link", "http://a/")), "link");
        Check.equal("<small>x</small>", Env.str(m("x", "small")), "small");
        Check.equal("<strike>x</strike>", Env.str(m("x", "strike")), "strike");
        Check.equal("<sub>x</sub>", Env.str(m("x", "sub")), "sub");
        Check.equal("<sup>x</sup>", Env.str(m("x", "sup")), "sup");
        Check.equal("<a name=\"undefined\">x</a>", Env.str(m("x", "anchor")), "missing attribute argument becomes undefined");
    }

    /**
     * Methods are generic over this (numbers, booleans, objects with toString) and reject null and undefined.
     * <p>
     * 메서드는 this 에 대해 일반적이며(숫자, 불리언, toString 이 있는 객체) null 과 undefined 는 거부한다.
     */
    public void testGenericThisValue() {
        Check.equal("2", Env.str(e.callOn(sp, "charAt", 123, 1)), "number this");
        Check.equal("true", Env.str(e.callOn(sp, "toString", "true")), "string this");
        JsObject o = e.realm.newObject();
        o.createDataProperty("toString", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str("obj");
            }
        }));
        Check.equal("OBJ", Env.str(e.callOn(sp, "toUpperCase", o)), "object this uses toString");
        String[] names = { "charAt", "concat", "indexOf", "slice", "toUpperCase", "trim", "match", "replace", "split", "normalize", "repeat",
            "anchor", "padStart" };
        // 모든 메서드는 undefined 와 null 을 this 로 받으면 TypeError 이다.
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    e.callOn(sp, name, Js.UNDEFINED);
                }
            }), name + " on undefined");
            Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    e.callOn(sp, name, Js.NULL);
                }
            }), name + " on null");
        }
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.callOn(sp, "toString", 5);
            }
        }), "toString on a number");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.callOn(sp, "valueOf", Js.NULL);
            }
        }), "valueOf on null");
        Check.equal("abc", Env.str(e.callOn(sp, "valueOf", Ops.construct(e.global("String"), e.vs("abc")))), "valueOf of a String object");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.callOn(sp, "charAt", new JsSymbol("s"));
            }
        }), "a Symbol this cannot be converted");
    }

    /**
     * The String iterator walks code points and keeps lone surrogates.
     * <p>
     * String 이터레이터는 코드 포인트 단위로 걸으며 외톨이 서로게이트를 그대로 둔다.
     */
    public void testStringIterator() {
        String text = "a" + emoji() + "b" + (char) 0xD83D + "c";
        JsObject it = (JsObject) Ops.invoke(e.realm, e.v(text), WellKnownSymbols.ITERATOR, Js.NO_ARGS);
        List<JsValue> pieces = IteratorHelper.toList(e.realm, e.v(text));
        Check.equal(5, pieces.size(), "number of code points");
        Check.equal(emoji(), Env.str(pieces.get(1)), "a pair is one element");
        Check.equal("" + (char) 0xD83D, Env.str(pieces.get(3)), "a lone surrogate is one element");
        Check.same(e.realm.getIntrinsic("%StringIteratorPrototype%"), it.getPrototypeOf(), "prototype");
        Check.same(e.realm.getIteratorPrototype(), e.realm.getIntrinsic("%StringIteratorPrototype%").getPrototypeOf(), "inherits %IteratorPrototype%");
        Check.equal("String Iterator", Env.str(it.get(WellKnownSymbols.TO_STRING_TAG)), "@@toStringTag");
        JsObject r1 = (JsObject) e.call(it, "next");
        Check.equal("a", Env.str(r1.get("value")), "first value");
        Check.isFalse(Ops.toBoolean(r1.get("done")), "not done");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                Ops.call(e.realm.getIntrinsic("%StringIteratorPrototype%").get("next"), e.realm.newObject(), Js.NO_ARGS);
            }
        }), "next on a foreign object");
        JsObject empty = (JsObject) Ops.invoke(e.realm, e.v(""), WellKnownSymbols.ITERATOR, Js.NO_ARGS);
        JsObject end = (JsObject) e.call(empty, "next");
        Check.isTrue(Ops.toBoolean(end.get("done")) && end.get("value").isUndefined(), "an empty string finishes at once");
        Check.isTrue(Ops.toBoolean(((JsObject) e.call(empty, "next")).get("done")), "and stays finished");
    }

    /**
     * The documented extensions padStart and padEnd.
     * <p>
     * 문서화한 확장 padStart 와 padEnd.
     */
    public void testPad() {
        Check.equal("  ab", Env.str(m("ab", "padStart", 4)), "padStart with spaces");
        Check.equal("xyab", Env.str(m("ab", "padStart", 4, "xyz")), "padStart truncates the filler");
        Check.equal("abxy", Env.str(m("ab", "padEnd", 4, "xyz")), "padEnd");
        Check.equal("ab", Env.str(m("ab", "padEnd", 1)), "no padding needed");
        Check.equal("ab", Env.str(m("ab", "padStart", 5, "")), "empty filler");
        Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m("ab", "padStart", 1e12, "x");
            }
        }), "too long");
    }

    /**
     * Strings longer than the limit are rejected with a RangeError by the methods that build long strings.
     * <p>
     * 긴 문자열을 만드는 메서드는 제한을 넘으면 RangeError 로 거부한다.
     */
    public void testStringLengthLimit() {
        Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m("abcdefgh", "repeat", Support.MAX_STRING_LENGTH);
            }
        }), "repeat beyond the limit");
        Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m("x", "padEnd", Support.MAX_STRING_LENGTH + 1.0);
            }
        }), "padEnd beyond the limit");
    }
}
