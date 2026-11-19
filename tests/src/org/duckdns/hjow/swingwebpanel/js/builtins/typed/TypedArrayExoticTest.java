package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the integer-indexed exotic behavior of typed arrays (ECMA-262 6th edition, 9.4.5; plan item P14-06): element access through the
 * property protocol, canonical numeric strings, ordinary properties that only look numeric, the prototype chain, descriptors, deletion and
 * enumeration, and that typed arrays are not arrays.
 * <p>
 * 형식화 배열의 정수 인덱스 이국적 동작 시험(ECMA-262 6판, 9.4.5, 계획 P14-06): 프로퍼티 규약을 통한 요소 접근, 정규 숫자 문자열, 숫자처럼 보일 뿐인
 * 일반 프로퍼티, 프로토타입 체인, 기술자, 삭제와 열거, 형식화 배열이 배열이 아님.
 */
public class TypedArrayExoticTest {

    /**
     * The realm used by a case.
     * <p>
     * 사례가 쓰는 realm.
     */
    private JsRealm realm;

    /**
     * The array under test: Uint8Array [1, 2, 3].
     * <p>
     * 시험 대상 배열: Uint8Array [1, 2, 3].
     */
    private TypedArrayObject a;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TypedArrayExoticTest() {
    }

    /**
     * Creates a fresh realm and array before each case.
     * <p>
     * 사례마다 새 realm 과 배열을 만든다.
     */
    public void setUp() {
        realm = TypedTestSupport.newRealm();
        a = TypedTestSupport.ta(realm, "Uint8Array", 1, 2, 3);
    }

    /**
     * Valid indices read and write the elements with conversion; the length never changes.
     * <p>
     * 올바른 인덱스는 변환하며 요소를 읽고 쓴다. 길이는 변하지 않는다.
     */
    public void testElementAccess() {
        TypedTestSupport.eq(1, TypedTestSupport.num(a.get("0")), "a[0]");
        TypedTestSupport.eq(3, TypedTestSupport.num(a.get("2")), "a[2]");
        Check.isTrue(a.set("1", TypedTestSupport.n(300)), "set returns true");
        TypedTestSupport.eq(44, TypedTestSupport.num(a.get("1")), "300 wraps to 44");
        a.set("0", TypedTestSupport.s("9"));
        TypedTestSupport.eq(9, TypedTestSupport.num(a.get("0")), "strings are converted with ToNumber");
        Check.isTrue(a.get("3").isUndefined(), "a[3] is undefined");
        Check.isTrue(a.set("3", TypedTestSupport.n(7)), "writing out of range is ignored without failure");
        Check.isTrue(a.get("3").isUndefined(), "a[3] is still undefined");
        Check.equal(3, a.getLength(), "length is unchanged");
        Check.isFalse(a.hasOwnProperty("3"), "no own property 3");
        a.setStrict("2", TypedTestSupport.n(5));
        a.setStrict("100", TypedTestSupport.n(5));
        TypedTestSupport.checkElements(new double[] { 9, 44, 5 }, a, "contents");
    }

    /**
     * Canonical numeric strings that are not valid indices are absent and never become properties.
     * <p>
     * 올바른 인덱스가 아닌 정규 숫자 문자열은 존재하지 않으며 프로퍼티가 되지 않는다.
     */
    public void testInvalidCanonicalNumericKeys() {
        String[] keys = { "-1", "1.5", "-0", "NaN", "Infinity", "-Infinity", "4294967295", "3", "100" };
        // 숫자의 정규 문자열 모양인 키는 모두 요소가 아니며 프로퍼티로 저장되지도 않는다.
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            Check.isTrue(a.get(k).isUndefined(), "get " + k);
            Check.isFalse(a.hasProperty(k), "has " + k);
            Check.isFalse(a.hasOwnProperty(k), "hasOwn " + k);
            Check.isTrue(a.set(k, TypedTestSupport.n(1)), "set " + k + " is ignored");
            Check.isNull(a.getOwnProperty(k), "descriptor of " + k);
            Check.isFalse(a.defineOwnProperty(k, PropertyDescriptor.data(TypedTestSupport.n(1), true, true, true)), "define " + k);
        }
        Check.isNull(a.getOwnPropertySlot("-0"), "-0 is canonical numeric but not an index");
        TypedTestSupport.checkElements(new double[] { 1, 2, 3 }, a, "contents are untouched");
        Check.equal(3, a.ownKeys().size(), "no extra own keys");
    }

    /**
     * Keys that merely look numeric but are not canonical are ordinary properties.
     * <p>
     * 숫자처럼 보이지만 정규 형태가 아닌 키는 일반 프로퍼티이다.
     */
    public void testNonCanonicalKeysAreOrdinaryProperties() {
        String[] keys = { "01", "+1", "1e3", " 1", "1.0", "0x1", "00" };
        // 정규 문자열이 아닌 숫자 모양 키는 일반 프로퍼티로 저장된다.
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            Check.isTrue(a.set(k, TypedTestSupport.n(77)), "set " + k);
            Check.isTrue(a.hasOwnProperty(k), "own property " + k);
            TypedTestSupport.eq(77, TypedTestSupport.num(a.get(k)), "get " + k);
        }
        TypedTestSupport.checkElements(new double[] { 1, 2, 3 }, a, "elements are untouched");
        List<Object> keysList = a.ownKeys();
        Check.equal("0", keysList.get(0), "indices come first");
        Check.equal("2", keysList.get(2), "indices come first");
        Check.equal("01", keysList.get(3), "then the ordinary keys in creation order");
    }

    /**
     * Numeric keys never consult the prototype chain; other keys do.
     * <p>
     * 숫자 키는 프로토타입 체인을 보지 않고 다른 키는 본다.
     */
    public void testPrototypeIsNotConsultedForNumericKeys() {
        JsObject proto = realm.getIntrinsic("%Uint8ArrayPrototype%");
        proto.set("5", TypedTestSupport.n(99));
        proto.set("1.5", TypedTestSupport.n(98));
        proto.set("foo", TypedTestSupport.n(97));
        Check.isTrue(a.get("5").isUndefined(), "a[5] does not see Uint8Array.prototype[5]");
        Check.isFalse(a.hasProperty("5"), "5 in a is false");
        Check.isTrue(a.get("1.5").isUndefined(), "a[1.5]");
        TypedTestSupport.eq(97, TypedTestSupport.num(a.get("foo")), "other keys are inherited");
        Check.isTrue(a.hasProperty("foo"), "foo in a is true");
    }

    /**
     * Element descriptors are writable, enumerable and not configurable.
     * <p>
     * 요소 기술자는 writable, 열거 가능, configurable 아님이다.
     */
    public void testGetOwnPropertyDescriptor() {
        PropertyDescriptor d = a.getOwnProperty("1");
        Check.notNull(d, "descriptor of a valid index");
        TypedTestSupport.eq(2, TypedTestSupport.num(d.value), "value");
        Check.isTrue(d.writable.booleanValue(), "writable");
        Check.isTrue(d.enumerable.booleanValue(), "enumerable");
        Check.isFalse(d.configurable.booleanValue(), "not configurable");
        Check.isNull(a.getOwnProperty("3"), "no descriptor out of range");
    }

    /**
     * defineProperty accepts only descriptors that keep the element writable, enumerable and not configurable.
     * <p>
     * defineProperty 는 요소를 writable, 열거 가능, configurable 아님으로 유지하는 기술자만 받아들인다.
     */
    public void testDefineOwnProperty() {
        Check.isTrue(a.defineOwnProperty("0", PropertyDescriptor.valueOnly(TypedTestSupport.n(50))), "value only");
        TypedTestSupport.eq(50, TypedTestSupport.num(a.get("0")), "value was stored");
        Check.isTrue(a.defineOwnProperty("0", PropertyDescriptor.data(TypedTestSupport.n(51), true, true, false)), "same attributes");
        TypedTestSupport.eq(51, TypedTestSupport.num(a.get("0")), "value was stored again");
        Check.isTrue(a.defineOwnProperty("0", new PropertyDescriptor()), "an empty descriptor is accepted");
        Check.isFalse(a.defineOwnProperty("0", PropertyDescriptor.data(TypedTestSupport.n(1), true, true, true)), "configurable true");
        Check.isFalse(a.defineOwnProperty("0", PropertyDescriptor.data(TypedTestSupport.n(1), true, false, false)), "enumerable false");
        Check.isFalse(a.defineOwnProperty("0", PropertyDescriptor.data(TypedTestSupport.n(1), false, true, false)), "writable false");
        Check.isFalse(a.defineOwnProperty("0", PropertyDescriptor.accessor(null, null, true, false)), "accessor");
        Check.isFalse(a.defineOwnProperty("7", PropertyDescriptor.valueOnly(TypedTestSupport.n(1))), "out of range index");
        TypedTestSupport.eq(51, TypedTestSupport.num(a.get("0")), "rejected definitions changed nothing");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                a.definePropertyOrThrow("1", PropertyDescriptor.data(TypedTestSupport.n(1), false, true, false));
            }
        }, "Object.defineProperty with writable false");
        a.definePropertyOrThrow("foo", PropertyDescriptor.data(TypedTestSupport.n(1), true, true, true));
        Check.isTrue(a.hasOwnProperty("foo"), "ordinary properties can be defined");
    }

    /**
     * Elements cannot be deleted; deleting out-of-range keys succeeds trivially.
     * <p>
     * 요소는 삭제할 수 없다. 범위 밖 키의 삭제는 그냥 성공한다.
     */
    public void testDelete() {
        Check.isFalse(a.delete("0"), "delete a[0] fails (not configurable)");
        Check.isTrue(a.delete("10"), "delete a[10] succeeds");
        Check.isTrue(a.delete("-1"), "delete a[-1] succeeds");
        a.set("foo", Js.TRUE);
        Check.isTrue(a.delete("foo"), "ordinary properties can be deleted");
        TypedTestSupport.checkElements(new double[] { 1, 2, 3 }, a, "contents");
    }

    /**
     * Own keys: indices, then string keys, then symbols; elements are enumerable.
     * <p>
     * 자체 키: 인덱스, 문자열 키, 심볼 순서. 요소는 열거 가능하다.
     */
    public void testOwnKeysAndEnumeration() {
        JsSymbol sym = new JsSymbol("s");
        a.set("zeta", Js.TRUE);
        a.set(sym, Js.TRUE);
        a.set("alpha", Js.TRUE);
        List<Object> keys = a.ownKeys();
        Check.equal(6, keys.size(), "key count");
        Check.equal("0", keys.get(0), "first key");
        Check.equal("2", keys.get(2), "last index key");
        Check.equal("zeta", keys.get(3), "string keys in creation order");
        Check.equal("alpha", keys.get(4), "string keys in creation order");
        Check.same(sym, keys.get(5), "symbols last");
        List<String> enumerable = Ops.enumerableOwnKeys(a);
        Check.equal(5, enumerable.size(), "enumerable string keys: 3 indices and 2 named");
        Check.equal("0", enumerable.get(0), "enumeration starts with the indices");
    }

    /**
     * Typed arrays are not arrays and do not inherit from Array.prototype.
     * <p>
     * 형식화 배열은 배열이 아니며 Array.prototype 을 상속하지 않는다.
     */
    public void testNotAnArray() {
        Check.isFalse(Ops.isArray(a), "IsArray");
        Check.isFalse(a.isArray(), "isArray()");
        JsObject p = a.getPrototypeOf();
        Check.same(realm.getIntrinsic("%Uint8ArrayPrototype%"), p, "first prototype");
        p = p.getPrototypeOf();
        Check.same(realm.getIntrinsic("%TypedArrayPrototype%"), p, "second prototype");
        p = p.getPrototypeOf();
        Check.same(realm.getObjectPrototype(), p, "third prototype is Object.prototype");
        Check.isNull(p.getPrototypeOf(), "end of the chain");
        Check.isTrue(a.get("length") instanceof org.duckdns.hjow.swingwebpanel.js.JsNumber, "length comes from the prototype getter");
        Check.isFalse(a.hasOwnProperty("length"), "length is not an own property");
        Check.equal("Uint8Array", a.className(), "class name");
    }

    /**
     * After preventExtensions new properties are refused but elements stay writable; elements can never be made read-only.
     * <p>
     * preventExtensions 뒤에는 새 프로퍼티가 거부되지만 요소는 계속 쓸 수 있다. 요소를 읽기 전용으로 만들 수는 없다.
     */
    public void testExtensibility() {
        Check.isTrue(a.preventExtensions(), "preventExtensions");
        Check.isFalse(a.defineOwnProperty("foo", PropertyDescriptor.data(Js.TRUE, true, true, true)), "no new properties");
        a.set("1", TypedTestSupport.n(8));
        TypedTestSupport.eq(8, TypedTestSupport.num(a.get("1")), "elements are still writable");
        Check.isFalse(a.defineOwnProperty("1", PropertyDescriptor.data(TypedTestSupport.n(8), false, true, false)), "elements cannot be frozen");
    }

    /**
     * A detached buffer makes the array look empty.
     * <p>
     * 버퍼가 분리되면 배열이 비어 보인다.
     */
    public void testDetachedArrayLooksEmpty() {
        a.getBuffer().detach();
        Check.equal(0, a.getLength(), "length");
        Check.equal(0, a.getByteLength(), "byteLength");
        Check.equal(0, a.getByteOffset(), "byteOffset");
        Check.isTrue(a.get("0").isUndefined(), "get");
        Check.isFalse(a.hasProperty("0"), "has");
        Check.isTrue(a.set("0", TypedTestSupport.n(1)), "set is ignored");
        Check.equal(0, a.ownKeys().size(), "no keys");
    }

    /**
     * The assigned value is converted with ToNumber even when the index is invalid (observable side effects).
     * <p>
     * 인덱스가 올바르지 않아도 대입한 값은 ToNumber 로 변환된다(관찰 가능한 부수 효과).
     */
    public void testValueIsConvertedEvenForInvalidIndex() {
        final int[] calls = new int[1];
        JsObject v = realm.newObject();
        v.defineData("valueOf", TypedTestSupport.fn(realm, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls[0]++;
                return TypedTestSupport.n(6);
            }
        }), true, false, true);
        a.set("1", v);
        a.set("50", v);
        a.set("-1", v);
        Check.equal(3, calls[0], "valueOf was called for the valid and the invalid indices");
        TypedTestSupport.eq(6, TypedTestSupport.num(a.get("1")), "the valid write stored 6");
    }
}
