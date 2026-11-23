package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the value model (plan items P12-01 and P12-02): the seven types, NaN/Infinity/-0, UTF-16 strings and symbols.
 * <p>
 * 값 모델 시험(계획 P12-01, P12-02): 일곱 가지 형식, NaN/Infinity/-0, UTF-16 문자열, 심볼.
 */
public class ValueModelTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ValueModelTest() {
    }

    /**
     * JavaScript null and undefined are different singleton values and are not equal with ===.
     * <p>
     * JavaScript 의 null 과 undefined 는 서로 다른 싱글턴 값이며 === 로 같지 않다.
     */
    public void testUndefinedAndNullAreDistinct() {
        Check.isTrue(Js.UNDEFINED != Js.NULL, "different instances");
        Check.equal(JsType.UNDEFINED, Js.UNDEFINED.type(), "undefined type");
        Check.equal(JsType.NULL, Js.NULL.type(), "null type");
        Check.isTrue(Js.UNDEFINED.isUndefined() && !Js.UNDEFINED.isNull(), "undefined flags");
        Check.isTrue(Js.NULL.isNull() && !Js.NULL.isUndefined(), "null flags");
        Check.isTrue(Js.NULL.isNullOrUndefined() && Js.UNDEFINED.isNullOrUndefined(), "nullish");
        Check.isFalse(Ops.strictEquals(Js.UNDEFINED, Js.NULL), "undefined !== null");
        Check.isTrue(Ops.looseEquals(Js.UNDEFINED, Js.NULL), "undefined == null");
    }

    /**
     * Number keeps NaN, Infinity and the sign of zero.
     * <p>
     * Number 는 NaN, Infinity, 0 의 부호를 보존한다.
     */
    public void testNumberKeepsNaNInfinityAndNegativeZero() {
        double nz = JsNumber.of(-0.0).getValue();
        Check.isTrue(nz == 0 && 1 / nz < 0, "negative zero preserved");
        Check.isTrue(1 / JsNumber.of(0.0).getValue() > 0, "positive zero preserved");
        Check.isTrue(JsNumber.of(-0.0) != JsNumber.ZERO, "negative zero is never the cached zero");
        Check.isTrue(Double.isNaN(JsNumber.NAN.getValue()), "NaN");
        Check.isTrue(Double.isInfinite(JsNumber.of(Double.NEGATIVE_INFINITY).getValue()), "Infinity");
        Check.isTrue(JsNumber.of(5) == JsNumber.of(5.0), "small integers are cached");
        Check.equal("NaN", JsNumber.NAN.toString(), "NaN text");
        Check.equal("0", JsNumber.of(-0.0).toString(), "-0 prints as 0");
    }

    /**
     * typeof results for all kinds of values.
     * <p>
     * 모든 종류의 값에 대한 typeof 결과.
     */
    public void testTypeOf() {
        JsRealm realm = new JsRealm();
        Check.equal("undefined", Js.UNDEFINED.typeOf(), "typeof undefined");
        Check.equal("object", Js.NULL.typeOf(), "typeof null");
        Check.equal("boolean", Js.TRUE.typeOf(), "typeof true");
        Check.equal("number", Js.num(1).typeOf(), "typeof 1");
        Check.equal("string", Js.str("a").typeOf(), "typeof 'a'");
        Check.equal("symbol", new JsSymbol("s").typeOf(), "typeof symbol");
        Check.equal("object", realm.newObject().typeOf(), "typeof {}");
        Check.equal("function", realm.getFunctionPrototype().typeOf(), "typeof function");
    }

    /**
     * Boolean values are shared singletons.
     * <p>
     * Boolean 값은 공유 싱글턴이다.
     */
    public void testBooleanSingletons() {
        Check.isTrue(JsBoolean.of(true) == JsBoolean.TRUE && JsBoolean.of(false) == JsBoolean.FALSE, "shared");
        Check.isTrue(Js.TRUE == JsBoolean.TRUE, "Js.TRUE");
    }

    /**
     * Strings count UTF-16 code units, not code points.
     * <p>
     * 문자열은 코드 포인트가 아니라 UTF-16 코드 단위를 센다.
     */
    public void testStringIsUtf16CodeUnits() {
        String emoji = new String(Character.toChars(0x1F600));
        JsString s = JsString.of("a" + emoji + "b");
        Check.equal(4, s.length(), "an astral character takes two code units");
        Check.isTrue(Character.isHighSurrogate(s.charAt(1)) && Character.isLowSurrogate(s.charAt(2)), "surrogate pair");
        Check.equal(0x1F600, s.codePointAt(1), "codePointAt combines the pair");
        Check.equal(0xDE00, s.codePointAt(2), "codePointAt on the low half returns the lone surrogate");
        Check.isTrue(JsString.of("") == JsString.EMPTY, "empty string is shared");
        Check.isTrue(JsString.of("x").equals(JsString.of("x")), "string value equality");
    }

    /**
     * A String wrapper exposes code units as index properties and length counts code units.
     * <p>
     * String 래퍼는 코드 단위를 인덱스 프로퍼티로 보여 주고 length 는 코드 단위를 센다.
     */
    public void testStringWrapperIndicesAndLength() {
        JsRealm realm = new JsRealm();
        String emoji = new String(Character.toChars(0x1F600));
        JsObject w = Ops.toObject(realm, Js.str("x" + emoji));
        Check.equal(3, (long) ((JsNumber) w.get("length")).getValue(), "length counts code units");
        Check.equal("x", w.getString("0"), "index 0");
        Check.equal(String.valueOf(emoji.charAt(0)), w.getString("1"), "index 1 is a high surrogate");
        Check.equal(Js.UNDEFINED, w.get("3"), "beyond the end");
        PropertyDescriptor d = w.getOwnProperty("0");
        Check.isTrue(!d.isWritableTrue() && d.isEnumerableTrue() && !d.isConfigurableTrue(), "index attributes");
        Check.isFalse(w.delete("0"), "indices cannot be deleted");
        Check.isFalse(w.set("0", Js.str("y")), "indices are read-only");
        Check.equal("[0, 1, 2, length]", w.ownKeys().toString(), "own keys order");
    }

    /**
     * Symbols are unique values even with equal descriptions and are different from string keys.
     * <p>
     * 심볼은 설명이 같아도 고유한 값이며 문자열 키와 다르다.
     */
    public void testSymbolsAreUniqueAndSeparateFromStrings() {
        JsRealm realm = new JsRealm();
        JsSymbol a = new JsSymbol("foo");
        JsSymbol b = new JsSymbol("foo");
        Check.isFalse(Ops.strictEquals(a, b), "equal descriptions are different symbols");
        Check.isTrue(Ops.strictEquals(a, a), "a symbol equals itself");
        Check.equal("Symbol(foo)", a.toString(), "descriptive string");
        Check.equal("Symbol()", new JsSymbol(null).toString(), "no description");
        JsObject o = realm.newObject();
        o.createDataProperty(a, Js.num(1));
        o.createDataProperty("Symbol(foo)", Js.num(2));
        Check.equal(1, (long) ((JsNumber) o.get(a)).getValue(), "symbol key value");
        Check.equal(2, (long) ((JsNumber) o.get("Symbol(foo)")).getValue(), "string key value");
        Check.isTrue(o.get(b).isUndefined(), "another symbol with the same description is another key");
        Check.throwsException(org.duckdns.hjow.swingwebpanel.js.JsException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                Ops.toString(new JsSymbol("x"));
            }
        }, "symbols cannot be converted to strings implicitly");
        Check.equal("symbol", Ops.toPropertyKey(a) instanceof JsSymbol ? "symbol" : "string", "symbol stays a symbol as key");
    }

    /**
     * The registry returns the same symbol for the same key and carries the key.
     * <p>
     * 레지스트리는 같은 키에 같은 심볼을 돌려주며 키를 가진다.
     */
    public void testSymbolRegistry() {
        JsRealm realm = new JsRealm();
        JsSymbol a = realm.symbolFor("k");
        Check.isTrue(a == realm.symbolFor("k"), "same key same symbol");
        Check.equal("k", a.getRegistryKey(), "registry key");
        Check.isTrue(new JsSymbol("k").getRegistryKey() == null, "unregistered symbol has no key");
    }
}
