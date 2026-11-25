package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Table-driven check of everything the binary data library installs (plan item P14-09): the exact set of own properties of every object, the
 * name and length of every function, whether it is a constructor, and the attributes (writable, enumerable, configurable) of every property.
 * A property or function that is missing, extra or wrongly described fails the test.
 * <p>
 * 이진 데이터 라이브러리가 설치하는 모든 것을 표 기반으로 점검한다(계획 P14-09): 모든 객체의 자체 프로퍼티 집합, 모든 함수의 이름과 length, 생성자 여부,
 * 모든 프로퍼티의 속성(writable, enumerable, configurable). 빠졌거나 더 있거나 잘못 기술된 프로퍼티와 함수는 시험을 실패시킨다.
 */
public class BuiltinPropertiesTest {

    /**
     * The realm used by a case.
     * <p>
     * 사례가 쓰는 realm.
     */
    private JsRealm realm;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BuiltinPropertiesTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        realm = TypedTestSupport.newRealm();
    }

    /**
     * Checks a built-in method: writable, not enumerable, configurable; a non-constructor function with the given name and length.
     * <p>
     * 내장 메서드를 점검한다: writable, 열거 불가, configurable 이며 주어진 이름과 length 를 가진 비생성자 함수.
     *
     * @param label  description / 설명
     * @param o      the owner / 소유 객체
     * @param key    the property key / 프로퍼티 키
     * @param name   the expected function name / 기대하는 함수 이름
     * @param length the expected function length / 기대하는 함수 length
     */
    private void method(String label, JsObject o, Object key, String name, int length) {
        PropertyDescriptor d = o.getOwnProperty(key);
        Check.notNull(d, label + ": property exists");
        Check.isTrue(d.isData() && d.writable.booleanValue() && !d.enumerable.booleanValue() && d.configurable.booleanValue(), label + ": attributes (w, !e, c)");
        function(label, d.value, name, length, false);
    }

    /**
     * Checks a function object: callable, constructor or not, name and length with their own attributes.
     * <p>
     * 함수 객체를 점검한다: 호출 가능, 생성자 여부, 이름과 length 및 그 속성.
     *
     * @param label       description / 설명
     * @param v           the function value / 함수 값
     * @param name        the expected name / 기대하는 이름
     * @param length      the expected length / 기대하는 length
     * @param constructor whether it must be a constructor / 생성자여야 하는지 여부
     */
    private void function(String label, JsValue v, String name, int length, boolean constructor) {
        Check.isTrue(v instanceof JsObject && v.isCallable(), label + ": is a function");
        JsObject f = (JsObject) v;
        Check.equal(constructor, f.isConstructor(), label + ": isConstructor");
        PropertyDescriptor n = f.getOwnProperty("name");
        Check.notNull(n, label + ": own name");
        Check.equal(name, TypedTestSupport.str(n.value), label + ": name");
        Check.isTrue(!n.writable.booleanValue() && !n.enumerable.booleanValue() && n.configurable.booleanValue(), label + ": name attributes (!w, !e, c)");
        PropertyDescriptor l = f.getOwnProperty("length");
        Check.notNull(l, label + ": own length");
        TypedTestSupport.eq(length, TypedTestSupport.num(l.value), label + ": length");
        Check.isTrue(!l.writable.booleanValue() && !l.enumerable.booleanValue() && l.configurable.booleanValue(), label + ": length attributes (!w, !e, c)");
    }

    /**
     * Checks a getter-only accessor: not enumerable, configurable, getter named "get NAME" with length 0.
     * <p>
     * getter 만 있는 접근자를 점검한다: 열거 불가, configurable, 이름이 "get NAME" 이고 length 가 0 인 getter.
     *
     * @param label description / 설명
     * @param o     the owner / 소유 객체
     * @param key   the property key / 프로퍼티 키
     * @param name  the expected getter name / 기대하는 getter 이름
     */
    private void getter(String label, JsObject o, Object key, String name) {
        PropertyDescriptor d = o.getOwnProperty(key);
        Check.notNull(d, label + ": property exists");
        Check.isTrue(d.isAccessor(), label + ": is an accessor");
        Check.isTrue(!d.enumerable.booleanValue() && d.configurable.booleanValue(), label + ": attributes (!e, c)");
        Check.isTrue(d.set == null || d.set.isUndefined(), label + ": no setter");
        function(label + " getter", d.get, name, 0, false);
    }

    /**
     * Checks a data property with the given attributes.
     * <p>
     * 주어진 속성을 가진 데이터 프로퍼티를 점검한다.
     *
     * @param label description / 설명
     * @param o     the owner / 소유 객체
     * @param key   the property key / 프로퍼티 키
     * @param attrs "wec" letters that must be set (others must be clear) / 설정되어야 하는 "wec" 글자(나머지는 해제되어야 함)
     */
    private void data(String label, JsObject o, Object key, String attrs) {
        PropertyDescriptor d = o.getOwnProperty(key);
        Check.notNull(d, label + ": property exists");
        Check.isTrue(d.isData(), label + ": is a data property");
        Check.equal(attrs.indexOf('w') >= 0, d.writable.booleanValue(), label + ": writable");
        Check.equal(attrs.indexOf('e') >= 0, d.enumerable.booleanValue(), label + ": enumerable");
        Check.equal(attrs.indexOf('c') >= 0, d.configurable.booleanValue(), label + ": configurable");
    }

    /**
     * Checks that an object has exactly the expected string keys and the expected number of symbol keys.
     * <p>
     * 객체가 정확히 기대한 문자열 키와 기대한 수의 심볼 키를 가지는지 점검한다.
     *
     * @param label   description / 설명
     * @param o       the object / 객체
     * @param strings the expected string keys / 기대하는 문자열 키
     * @param symbols the expected number of symbol keys / 기대하는 심볼 키 수
     */
    private void keys(String label, JsObject o, String[] strings, int symbols) {
        List<String> actual = new ArrayList<String>();
        int symbolCount = 0;
        // 자체 키를 문자열과 심볼로 나눈다.
        for (Object k : o.ownKeys()) {
            if (k instanceof String) {
                actual.add((String) k);
            } else {
                symbolCount++;
            }
        }
        List<String> expected = new ArrayList<String>(Arrays.asList(strings));
        Collections.sort(actual);
        Collections.sort(expected);
        Check.equal(expected, actual, label + ": own string keys");
        Check.equal(symbols, symbolCount, label + ": number of own symbol keys");
    }

    /**
     * ArrayBuffer and its prototype.
     * <p>
     * ArrayBuffer 와 그 프로토타입.
     */
    public void testArrayBuffer() {
        JsObject ctor = TypedTestSupport.ctor(realm, "ArrayBuffer");
        JsObject proto = (JsObject) ctor.get("prototype");
        data("global ArrayBuffer", realm.getGlobalObject(), "ArrayBuffer", "wc");
        function("ArrayBuffer", ctor, "ArrayBuffer", 1, true);
        keys("ArrayBuffer", ctor, new String[] { "length", "name", "prototype", "isView" }, 1);
        data("ArrayBuffer.prototype", ctor, "prototype", "");
        method("ArrayBuffer.isView", ctor, "isView", "isView", 1);
        getter("ArrayBuffer[@@species]", ctor, WellKnownSymbols.SPECIES, "get [Symbol.species]");
        keys("ArrayBuffer.prototype", proto, new String[] { "constructor", "byteLength", "maxByteLength", "resizable", "detached", "resize",
            "transfer", "transferToFixedLength", "slice" }, 1);
        data("ArrayBuffer.prototype.constructor", proto, "constructor", "wc");
        getter("ArrayBuffer.prototype.byteLength", proto, "byteLength", "get byteLength");
        getter("ArrayBuffer.prototype.maxByteLength", proto, "maxByteLength", "get maxByteLength");
        getter("ArrayBuffer.prototype.resizable", proto, "resizable", "get resizable");
        getter("ArrayBuffer.prototype.detached", proto, "detached", "get detached");
        method("ArrayBuffer.prototype.resize", proto, "resize", "resize", 1);
        method("ArrayBuffer.prototype.transfer", proto, "transfer", "transfer", 0);
        method("ArrayBuffer.prototype.transferToFixedLength", proto, "transferToFixedLength", "transferToFixedLength", 0);
        method("ArrayBuffer.prototype.slice", proto, "slice", "slice", 2);
        data("ArrayBuffer.prototype[@@toStringTag]", proto, WellKnownSymbols.TO_STRING_TAG, "c");
        Check.equal("ArrayBuffer", TypedTestSupport.str(proto.get(WellKnownSymbols.TO_STRING_TAG)), "tag text");
    }

    /**
     * DataView and its prototype.
     * <p>
     * DataView 와 그 프로토타입.
     */
    public void testDataView() {
        JsObject ctor = TypedTestSupport.ctor(realm, "DataView");
        JsObject proto = (JsObject) ctor.get("prototype");
        data("global DataView", realm.getGlobalObject(), "DataView", "wc");
        function("DataView", ctor, "DataView", 3, true);
        keys("DataView", ctor, new String[] { "length", "name", "prototype" }, 0);
        data("DataView.prototype", ctor, "prototype", "");
        String[] suffixes = { "Int8", "Uint8", "Int16", "Uint16", "Int32", "Uint32", "Float16", "Float32", "Float64", "BigInt64", "BigUint64" };
        List<String> expected = new ArrayList<String>(Arrays.asList("constructor", "buffer", "byteLength", "byteOffset"));
        // 형식마다 getXxx(length 1), setXxx(length 2) 를 점검한다.
        for (int i = 0; i < suffixes.length; i++) {
            method("DataView.prototype.get" + suffixes[i], proto, "get" + suffixes[i], "get" + suffixes[i], 1);
            method("DataView.prototype.set" + suffixes[i], proto, "set" + suffixes[i], "set" + suffixes[i], 2);
            expected.add("get" + suffixes[i]);
            expected.add("set" + suffixes[i]);
        }
        keys("DataView.prototype", proto, expected.toArray(new String[expected.size()]), 1);
        data("DataView.prototype.constructor", proto, "constructor", "wc");
        getter("DataView.prototype.buffer", proto, "buffer", "get buffer");
        getter("DataView.prototype.byteLength", proto, "byteLength", "get byteLength");
        getter("DataView.prototype.byteOffset", proto, "byteOffset", "get byteOffset");
        data("DataView.prototype[@@toStringTag]", proto, WellKnownSymbols.TO_STRING_TAG, "c");
        Check.equal("DataView", TypedTestSupport.str(proto.get(WellKnownSymbols.TO_STRING_TAG)), "tag text");
    }

    /**
     * The abstract %TypedArray% constructor and %TypedArray%.prototype.
     * <p>
     * 추상 %TypedArray% 생성자와 %TypedArray%.prototype.
     */
    public void testTypedArrayIntrinsic() {
        JsObject ctor = realm.getIntrinsic("%TypedArray%");
        JsObject proto = realm.getIntrinsic("%TypedArrayPrototype%");
        function("%TypedArray%", ctor, "TypedArray", 0, true);
        Check.same(proto, ctor.get("prototype"), "%TypedArray%.prototype");
        keys("%TypedArray%", ctor, new String[] { "length", "name", "prototype", "from", "of" }, 1);
        data("%TypedArray%.prototype", ctor, "prototype", "");
        method("%TypedArray%.from", ctor, "from", "from", 1);
        method("%TypedArray%.of", ctor, "of", "of", 0);
        getter("%TypedArray%[@@species]", ctor, WellKnownSymbols.SPECIES, "get [Symbol.species]");
        String[][] methods = { { "at", "1" }, { "copyWithin", "2" }, { "entries", "0" }, { "every", "1" }, { "fill", "1" }, { "filter", "1" }, { "find", "1" },
            { "findIndex", "1" }, { "findLast", "1" }, { "findLastIndex", "1" }, { "forEach", "1" }, { "indexOf", "1" }, { "join", "1" },
            { "keys", "0" }, { "lastIndexOf", "1" }, { "map", "1" },
            { "reduce", "1" }, { "reduceRight", "1" }, { "reverse", "0" }, { "set", "1" }, { "slice", "2" }, { "some", "1" }, { "sort", "1" },
            { "subarray", "2" }, { "toLocaleString", "0" }, { "toReversed", "0" }, { "toSorted", "1" }, { "values", "0" }, { "with", "2" } };
        List<String> expected = new ArrayList<String>(Arrays.asList("constructor", "buffer", "byteLength", "byteOffset", "length", "toString"));
        // 메서드마다 이름, length, 속성을 점검한다.
        for (int i = 0; i < methods.length; i++) {
            method("%TypedArray%.prototype." + methods[i][0], proto, methods[i][0], methods[i][0], Integer.parseInt(methods[i][1]));
            expected.add(methods[i][0]);
        }
        keys("%TypedArray%.prototype", proto, expected.toArray(new String[expected.size()]), 2);
        data("%TypedArray%.prototype.constructor", proto, "constructor", "wc");
        Check.same(ctor, proto.get("constructor"), "constructor");
        getter("%TypedArray%.prototype.buffer", proto, "buffer", "get buffer");
        getter("%TypedArray%.prototype.byteLength", proto, "byteLength", "get byteLength");
        getter("%TypedArray%.prototype.byteOffset", proto, "byteOffset", "get byteOffset");
        getter("%TypedArray%.prototype.length", proto, "length", "get length");
        getter("%TypedArray%.prototype[@@toStringTag]", proto, WellKnownSymbols.TO_STRING_TAG, "get [Symbol.toStringTag]");
        method("%TypedArray%.prototype.toString", proto, "toString", "toString", 0);
        Check.same(proto.get("values"), proto.get(WellKnownSymbols.ITERATOR), "[Symbol.iterator] is the values function");
        data("%TypedArray%.prototype[@@iterator]", proto, WellKnownSymbols.ITERATOR, "wc");
        Check.equal("values", TypedTestSupport.str(((JsObject) proto.get(WellKnownSymbols.ITERATOR)).get("name")), "the name of [Symbol.iterator] is 'values'");
    }

    /**
     * The concrete typed array constructors and their prototypes.
     * <p>
     * 구체 형식화 배열 생성자와 프로토타입.
     */
    public void testConcreteConstructors() {
        ElementType[] types = ElementType.values();
        // 요소 형식마다 생성자와 프로토타입의 프로퍼티를 점검한다.
        for (int i = 0; i < types.length; i++) {
            String name = types[i].getConstructorName();
            JsObject ctor = TypedTestSupport.ctor(realm, name);
            JsObject proto = (JsObject) ctor.get("prototype");
            data("global " + name, realm.getGlobalObject(), name, "wc");
            function(name, ctor, name, 3, true);
            data(name + ".prototype", ctor, "prototype", "");
            data(name + ".BYTES_PER_ELEMENT", ctor, "BYTES_PER_ELEMENT", "");
            if (types[i] == ElementType.UINT8) {
                keys(name, ctor, new String[] { "length", "name", "prototype", "BYTES_PER_ELEMENT", "fromHex", "fromBase64" }, 0);
                keys(name + ".prototype", proto, new String[] { "constructor", "BYTES_PER_ELEMENT", "toHex", "setFromHex", "toBase64", "setFromBase64" }, 0);
                method("Uint8Array.fromHex", ctor, "fromHex", "fromHex", 1);
                method("Uint8Array.prototype.toHex", proto, "toHex", "toHex", 0);
                method("Uint8Array.prototype.setFromHex", proto, "setFromHex", "setFromHex", 1);
                method("Uint8Array.fromBase64", ctor, "fromBase64", "fromBase64", 1);
                method("Uint8Array.prototype.toBase64", proto, "toBase64", "toBase64", 0);
                method("Uint8Array.prototype.setFromBase64", proto, "setFromBase64", "setFromBase64", 1);
            } else {
                keys(name, ctor, new String[] { "length", "name", "prototype", "BYTES_PER_ELEMENT" }, 0);
                keys(name + ".prototype", proto, new String[] { "constructor", "BYTES_PER_ELEMENT" }, 0);
            }
            data(name + ".prototype.constructor", proto, "constructor", "wc");
            data(name + ".prototype.BYTES_PER_ELEMENT", proto, "BYTES_PER_ELEMENT", "");
            Check.same(realm.getIntrinsic("%" + name + "%"), ctor, "intrinsic %" + name + "%");
            Check.same(realm.getIntrinsic("%" + name + "Prototype%"), proto, "intrinsic %" + name + "Prototype%");
        }
    }

    /**
     * The iterator prototype used by typed arrays.
     * <p>
     * 형식화 배열이 쓰는 이터레이터 프로토타입.
     */
    public void testIteratorPrototype() {
        JsObject proto = realm.getIntrinsic("%TypedArrayIteratorPrototype%");
        Check.same(realm.getIteratorPrototype(), proto.getPrototypeOf(), "inherits %IteratorPrototype%");
        keys("iterator prototype", proto, new String[] { "next" }, 1);
        method("iterator.next", proto, "next", "next", 0);
        data("iterator[@@toStringTag]", proto, WellKnownSymbols.TO_STRING_TAG, "c");
        JsSymbol tag = WellKnownSymbols.TO_STRING_TAG;
        Check.equal("Array Iterator", TypedTestSupport.str(proto.get(tag)), "tag text");
    }

    /**
     * When Array.prototype.toString exists at install time, %TypedArray%.prototype.toString is the very same function object.
     * <p>
     * 설치 시점에 Array.prototype.toString 이 있으면 %TypedArray%.prototype.toString 은 바로 그 함수 객체이다.
     */
    public void testToStringIsTheArrayFunction() {
        JsRealm withArray = new JsRealm();
        JsObject arrayToString = withArray.newFunction("toString", 0, new org.duckdns.hjow.swingwebpanel.js.NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.s("array toString");
            }
        });
        withArray.getArrayPrototype().defineBuiltin("toString", arrayToString);
        new TypedLibInstaller().install(withArray);
        Check.same(arrayToString, withArray.getIntrinsic("%TypedArrayPrototype%").get("toString"), "same function object");
    }

    /**
     * Installing twice into different realms gives independent objects.
     * <p>
     * 다른 realm 에 두 번 설치하면 서로 독립적인 객체가 생긴다.
     */
    public void testRealmsAreIndependent() {
        JsRealm other = TypedTestSupport.newRealm();
        Check.isTrue(TypedTestSupport.ctor(realm, "Uint8Array") != TypedTestSupport.ctor(other, "Uint8Array"), "separate constructors");
        Check.isTrue(realm.getIntrinsic("%TypedArray%") != other.getIntrinsic("%TypedArray%"), "separate intrinsics");
    }
}
