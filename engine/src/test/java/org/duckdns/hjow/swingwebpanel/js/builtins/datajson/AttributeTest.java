package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Table-driven check of name, length and property attributes of everything the data library installs (plan item P14-09): Date, RegExp,
 * JSON, Map, Set, WeakMap, WeakSet and the Map/Set iterator prototypes. For every owner object the test also verifies that no property
 * exists that is not in the table, so a method that is added or forgotten is noticed.
 * <p>
 * 데이터 라이브러리가 설치하는 모든 것의 이름, length, 프로퍼티 속성을 표로 검사하는 시험(계획 P14-09): Date, RegExp, JSON, Map, Set, WeakMap,
 * WeakSet 과 Map/Set 이터레이터 프로토타입. 소유 객체마다 표에 없는 프로퍼티가 없는지도 확인하므로 추가되거나 빠진 메서드를 알아챌 수 있다.
 */
public class AttributeTest {

    /**
     * Kind of a table row: a writable, configurable, non-enumerable method.
     * <p>
     * 표 행의 종류: 쓰기 가능, configurable, 열거 불가인 메서드.
     */
    private static final int METHOD = 0;

    /**
     * Kind of a table row: an accessor property with a getter only.
     * <p>
     * 표 행의 종류: getter 만 있는 접근자 프로퍼티.
     */
    private static final int GETTER = 1;

    /**
     * Kind of a table row: a read-only, configurable function property (such as Date.prototype[@@toPrimitive]).
     * <p>
     * 표 행의 종류: 읽기 전용, configurable 인 함수 프로퍼티(Date.prototype[@@toPrimitive] 등).
     */
    private static final int READONLY_METHOD = 2;

    /**
     * Environment of the current case.
     * <p>
     * 현재 사례의 환경.
     */
    private Env env;

    /**
     * The expected properties: owner path, property key, kind, function name and function length.
     * <p>
     * 기대하는 프로퍼티: 소유 객체 경로, 프로퍼티 키, 종류, 함수 이름, 함수 length.
     */
    private List<Object[]> rows;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AttributeTest() {
    }

    /**
     * Creates a fresh realm and builds the expectation table.
     * <p>
     * 새 realm 을 만들고 기대 표를 구성한다.
     */
    public void setUp() {
        env = new Env();
        rows = new ArrayList<Object[]>();
        buildDate();
        buildRegExp();
        buildJson();
        buildMap();
        buildSet();
        buildWeak();
    }

    /**
     * Adds a row to the table.
     * <p>
     * 표에 행을 추가한다.
     *
     * @param owner  owner path / 소유 객체 경로
     * @param key    property key (String or JsSymbol) / 프로퍼티 키(String 또는 JsSymbol)
     * @param kind   row kind / 행 종류
     * @param name   expected function name / 기대 함수 이름
     * @param length expected function length / 기대 함수 length
     */
    private void row(String owner, Object key, int kind, String name, int length) {
        rows.add(new Object[] { owner, key, Integer.valueOf(kind), name, Integer.valueOf(length) });
    }

    /**
     * Adds a plain method row whose function name equals the key.
     * <p>
     * 함수 이름이 키와 같은 일반 메서드 행을 추가한다.
     *
     * @param owner  owner path / 소유 객체 경로
     * @param name   method name / 메서드 이름
     * @param length expected length / 기대 length
     */
    private void method(String owner, String name, int length) {
        row(owner, name, METHOD, name, length);
    }

    /**
     * Adds a getter-only accessor row.
     * <p>
     * getter 만 있는 접근자 행을 추가한다.
     *
     * @param owner owner path / 소유 객체 경로
     * @param name  accessor name / 접근자 이름
     */
    private void getter(String owner, String name) {
        row(owner, name, GETTER, "get " + name, 0);
    }

    /**
     * Builds the Date rows.
     * <p>
     * Date 행을 구성한다.
     */
    private void buildDate() {
        method("Date", "now", 0);
        method("Date", "parse", 1);
        method("Date", "UTC", 7);
        String p = "Date.prototype";
        String[] fields = { "Date", "Day", "FullYear", "Hours", "Milliseconds", "Minutes", "Month", "Seconds" };
        // 각 필드의 지역/UTC getter 를 등록한다.
        for (int i = 0; i < fields.length; i++) {
            method(p, "get" + fields[i], 0);
            method(p, "getUTC" + fields[i], 0);
        }
        method(p, "getTime", 0);
        method(p, "valueOf", 0);
        method(p, "getTimezoneOffset", 0);
        method(p, "getYear", 0);
        String[] setters = { "Milliseconds", "Seconds", "Minutes", "Hours", "Date", "Month", "FullYear" };
        int[] lengths = { 1, 2, 3, 4, 1, 2, 3 };
        // 각 필드의 지역/UTC setter 를 등록한다(length 는 규격의 인수 개수).
        for (int i = 0; i < setters.length; i++) {
            method(p, "set" + setters[i], lengths[i]);
            method(p, "setUTC" + setters[i], lengths[i]);
        }
        method(p, "setTime", 1);
        method(p, "setYear", 1);
        method(p, "toString", 0);
        method(p, "toDateString", 0);
        method(p, "toTimeString", 0);
        method(p, "toUTCString", 0);
        row(p, "toGMTString", METHOD, "toUTCString", 0);
        method(p, "toISOString", 0);
        method(p, "toLocaleString", 0);
        method(p, "toLocaleDateString", 0);
        method(p, "toLocaleTimeString", 0);
        method(p, "toJSON", 1);
        row(p, WellKnownSymbols.TO_PRIMITIVE, READONLY_METHOD, "[Symbol.toPrimitive]", 1);
    }

    /**
     * Builds the RegExp rows.
     * <p>
     * RegExp 행을 구성한다.
     */
    private void buildRegExp() {
        method("RegExp", "escape", 1);
        String p = "RegExp.prototype";
        method(p, "exec", 1);
        method(p, "test", 1);
        method(p, "toString", 0);
        method(p, "compile", 2);
        String[] flags = { "hasIndices", "global", "ignoreCase", "multiline", "dotAll", "unicode", "unicodeSets", "sticky", "flags", "source" };
        // 플래그 접근자들을 등록한다.
        for (int i = 0; i < flags.length; i++) {
            getter(p, flags[i]);
        }
        row(p, WellKnownSymbols.MATCH, METHOD, "[Symbol.match]", 1);
        row(p, WellKnownSymbols.MATCH_ALL, METHOD, "[Symbol.matchAll]", 1);
        row(p, WellKnownSymbols.REPLACE, METHOD, "[Symbol.replace]", 2);
        row(p, WellKnownSymbols.SEARCH, METHOD, "[Symbol.search]", 1);
        row(p, WellKnownSymbols.SPLIT, METHOD, "[Symbol.split]", 2);
        row("RegExp", WellKnownSymbols.SPECIES, GETTER, "get [Symbol.species]", 0);
    }

    /**
     * Builds the JSON rows.
     * <p>
     * JSON 행을 구성한다.
     */
    private void buildJson() {
        method("JSON", "parse", 2);
        method("JSON", "stringify", 3);
    }

    /**
     * Builds the Map rows.
     * <p>
     * Map 행을 구성한다.
     */
    private void buildMap() {
        String p = "Map.prototype";
        method(p, "clear", 0);
        method(p, "delete", 1);
        method(p, "entries", 0);
        method(p, "forEach", 1);
        method(p, "get", 1);
        method(p, "getOrInsert", 2);
        method(p, "getOrInsertComputed", 2);
        method(p, "has", 1);
        method(p, "keys", 0);
        method(p, "set", 2);
        getter(p, "size");
        method(p, "values", 0);
        row(p, WellKnownSymbols.ITERATOR, METHOD, "entries", 0);
        row("Map", WellKnownSymbols.SPECIES, GETTER, "get [Symbol.species]", 0);
        method("Map", "groupBy", 2);
        method("%MapIteratorPrototype%", "next", 0);
        method("%SetIteratorPrototype%", "next", 0);
    }

    /**
     * Builds the Set rows.
     * <p>
     * Set 행을 구성한다.
     */
    private void buildSet() {
        String p = "Set.prototype";
        method(p, "add", 1);
        method(p, "clear", 0);
        method(p, "delete", 1);
        method(p, "difference", 1);
        method(p, "entries", 0);
        method(p, "forEach", 1);
        method(p, "has", 1);
        method(p, "intersection", 1);
        method(p, "isDisjointFrom", 1);
        method(p, "isSubsetOf", 1);
        method(p, "isSupersetOf", 1);
        row(p, "keys", METHOD, "values", 0);
        getter(p, "size");
        method(p, "symmetricDifference", 1);
        method(p, "union", 1);
        method(p, "values", 0);
        row(p, WellKnownSymbols.ITERATOR, METHOD, "values", 0);
        row("Set", WellKnownSymbols.SPECIES, GETTER, "get [Symbol.species]", 0);
    }

    /**
     * Builds the WeakMap and WeakSet rows.
     * <p>
     * WeakMap 과 WeakSet 행을 구성한다.
     */
    private void buildWeak() {
        method("WeakMap.prototype", "delete", 1);
        method("WeakMap.prototype", "get", 1);
        method("WeakMap.prototype", "getOrInsert", 2);
        method("WeakMap.prototype", "getOrInsertComputed", 2);
        method("WeakMap.prototype", "has", 1);
        method("WeakMap.prototype", "set", 2);
        method("WeakSet.prototype", "add", 1);
        method("WeakSet.prototype", "delete", 1);
        method("WeakSet.prototype", "has", 1);
    }

    /**
     * Resolves an owner path; names in percent signs are intrinsics.
     * <p>
     * 소유 객체 경로를 해석한다. 퍼센트 기호로 둘러싼 이름은 내장 객체이다.
     *
     * @param path the path / 경로
     * @return the object / 객체
     */
    private JsObject owner(String path) {
        if (path.startsWith("%")) {
            return env.realm.getIntrinsic(path);
        }
        return env.obj(path);
    }

    /**
     * Describes a key for messages.
     * <p>
     * 메시지용으로 키를 설명한다.
     *
     * @param key the key / 키
     * @return the description / 설명
     */
    private String keyText(Object key) {
        if (key instanceof JsSymbol) {
            return "@@" + ((JsSymbol) key).getDescription();
        }
        return String.valueOf(key);
    }

    /**
     * Checks a data property that holds a non-writable, non-enumerable, configurable value (function name and length).
     * <p>
     * 쓰기 불가, 열거 불가, configurable 인 값을 가진 데이터 프로퍼티(함수의 name, length)를 검사한다.
     *
     * @param fn       the function / 함수
     * @param key      "name" or "length" / "name" 또는 "length"
     * @param expected the expected value / 기대 값
     * @param what     description for messages / 메시지용 설명
     */
    private void checkFunctionMeta(JsObject fn, String key, String expected, String what) {
        PropertyDescriptor d = fn.getOwnProperty(key);
        Check.notNull(d, what + " has own " + key);
        Check.isTrue(d.isData(), what + "." + key + " is a data property");
        Check.isFalse(d.isWritableTrue(), what + "." + key + " is not writable");
        Check.isFalse(d.isEnumerableTrue(), what + "." + key + " is not enumerable");
        Check.isTrue(d.isConfigurableTrue(), what + "." + key + " is configurable");
        Check.equal(expected, Ops.toString(d.value), what + "." + key);
    }

    /**
     * Checks name, length and attributes of every row.
     * <p>
     * 모든 행의 이름, length, 속성을 검사한다.
     */
    public void testNamesLengthsAndAttributes() {
        // 표의 모든 행을 하나씩 검사한다.
        for (int i = 0; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            String ownerPath = (String) r[0];
            Object key = r[1];
            int kind = ((Integer) r[2]).intValue();
            String name = (String) r[3];
            int length = ((Integer) r[4]).intValue();
            String what = ownerPath + "." + keyText(key);
            JsObject o = owner(ownerPath);
            PropertyDescriptor d = o.getOwnProperty(key);
            Check.notNull(d, what + " exists");
            Check.isFalse(d.isEnumerableTrue(), what + " is not enumerable");
            Check.isTrue(d.isConfigurableTrue(), what + " is configurable");
            JsObject fn;
            if (kind == GETTER) {
                Check.isTrue(d.isAccessor(), what + " is an accessor");
                Check.isTrue(d.get instanceof JsObject, what + " has a getter function");
                Check.isTrue(d.set == null || d.set.isUndefined(), what + " has no setter");
                fn = (JsObject) d.get;
            } else {
                Check.isTrue(d.isData(), what + " is a data property");
                Check.isTrue(d.value instanceof JsObject, what + " holds a function");
                if (kind == READONLY_METHOD) {
                    Check.isFalse(d.isWritableTrue(), what + " is not writable");
                } else {
                    Check.isTrue(d.isWritableTrue(), what + " is writable");
                }
                fn = (JsObject) d.value;
            }
            Check.isTrue(fn.isCallable(), what + " is callable");
            Check.isFalse(Ops.isConstructor(fn), what + " is not a constructor");
            checkFunctionMeta(fn, "name", name, what);
            checkFunctionMeta(fn, "length", String.valueOf(length), what);
            Check.same(env.realm.getFunctionPrototype(), fn.getPrototypeOf(), what + " inherits Function.prototype");
        }
    }

    /**
     * Checks the constructors: name, length, prototype attributes, the constructor back reference and global binding attributes.
     * <p>
     * 생성자를 검사한다: 이름, length, prototype 속성, constructor 역참조, 전역 바인딩 속성.
     */
    public void testConstructors() {
        String[] names = { "Date", "RegExp", "Map", "Set", "WeakMap", "WeakSet" };
        int[] lengths = { 7, 2, 0, 0, 0, 0 };
        // 생성자마다 같은 항목들을 검사한다.
        for (int i = 0; i < names.length; i++) {
            String n = names[i];
            JsObject ctor = env.obj(n);
            Check.isTrue(Ops.isConstructor(ctor), n + " is a constructor");
            checkFunctionMeta(ctor, "name", n, n);
            checkFunctionMeta(ctor, "length", String.valueOf(lengths[i]), n);
            PropertyDescriptor pd = ctor.getOwnProperty("prototype");
            Check.notNull(pd, n + ".prototype exists");
            Check.isTrue(pd.isData() && !pd.isWritableTrue() && !pd.isEnumerableTrue() && !pd.isConfigurableTrue(),
                    n + ".prototype is {writable:false, enumerable:false, configurable:false}");
            PropertyDescriptor cd = ((JsObject) pd.value).getOwnProperty("constructor");
            Check.notNull(cd, n + ".prototype.constructor exists");
            Check.same(ctor, cd.value, n + ".prototype.constructor is the constructor");
            Check.isTrue(cd.isWritableTrue() && !cd.isEnumerableTrue() && cd.isConfigurableTrue(),
                    n + ".prototype.constructor is {writable:true, enumerable:false, configurable:true}");
            PropertyDescriptor gd = env.realm.getGlobalObject().getOwnProperty(n);
            Check.notNull(gd, "global " + n + " exists");
            Check.isTrue(gd.isWritableTrue() && !gd.isEnumerableTrue() && gd.isConfigurableTrue(),
                    "global " + n + " is {writable:true, enumerable:false, configurable:true}");
            Check.same(env.realm.getFunctionPrototype(), ctor.getPrototypeOf(), n + " inherits Function.prototype");
        }
        PropertyDescriptor jd = env.realm.getGlobalObject().getOwnProperty("JSON");
        Check.notNull(jd, "global JSON exists");
        Check.isTrue(jd.isWritableTrue() && !jd.isEnumerableTrue() && jd.isConfigurableTrue(), "global JSON attributes");
        Check.same(env.realm.getObjectPrototype(), env.obj("JSON").getPrototypeOf(), "JSON inherits Object.prototype");
        Check.same(env.realm.getObjectPrototype(), env.obj("Date.prototype").getPrototypeOf(), "Date.prototype inherits Object.prototype");
        Check.same(env.realm.getObjectPrototype(), env.obj("Map.prototype").getPrototypeOf(), "Map.prototype inherits Object.prototype");
        Check.same(env.realm.getIteratorPrototype(), env.realm.getIntrinsic("%SetIteratorPrototype%").getPrototypeOf(),
                "%SetIteratorPrototype% inherits %IteratorPrototype%");
    }

    /**
     * Checks the string tags (not writable, not enumerable, configurable).
     * <p>
     * 문자열 태그를 검사한다(쓰기 불가, 열거 불가, configurable).
     */
    public void testToStringTags() {
        String[][] tags = { { "JSON", "JSON" }, { "Map.prototype", "Map" }, { "Set.prototype", "Set" }, { "WeakMap.prototype", "WeakMap" },
                { "WeakSet.prototype", "WeakSet" }, { "%MapIteratorPrototype%", "Map Iterator" }, { "%SetIteratorPrototype%", "Set Iterator" } };
        // 태그가 있는 객체마다 값과 속성을 검사한다.
        for (int i = 0; i < tags.length; i++) {
            PropertyDescriptor d = owner(tags[i][0]).getOwnProperty(WellKnownSymbols.TO_STRING_TAG);
            Check.notNull(d, tags[i][0] + " has a toStringTag");
            Check.isTrue(d.isData() && !d.isWritableTrue() && !d.isEnumerableTrue() && d.isConfigurableTrue(), tags[i][0] + " tag attributes");
            Check.equal(tags[i][1], Ops.toString(d.value), tags[i][0] + " tag value");
        }
        Check.isNull(env.obj("Date.prototype").getOwnProperty(WellKnownSymbols.TO_STRING_TAG), "Date.prototype has no tag");
        Check.isNull(env.obj("RegExp.prototype").getOwnProperty(WellKnownSymbols.TO_STRING_TAG), "RegExp.prototype has no tag");
    }

    /**
     * Verifies that the owner objects have exactly the properties of the table plus the structural ones (no unexpected extras).
     * <p>
     * 소유 객체에 표의 프로퍼티와 구조적 프로퍼티만 있는지(예상 밖의 추가가 없는지) 확인한다.
     */
    public void testNoUnexpectedProperties() {
        List<String> owners = new ArrayList<String>();
        // 표에 나온 소유 객체 경로를 중복 없이 모은다.
        for (int i = 0; i < rows.size(); i++) {
            String o = (String) rows.get(i)[0];
            if (!owners.contains(o)) {
                owners.add(o);
            }
        }
        String[] extraOwners = { "Date.prototype", "RegExp", "Map", "Set", "WeakMap", "WeakSet", "JSON", "Map.prototype", "Set.prototype",
                "WeakMap.prototype", "WeakSet.prototype", "RegExp.prototype", "Date", "%MapIteratorPrototype%", "%SetIteratorPrototype%" };
        // 표에 행이 없는 소유 객체도 검사 대상에 넣는다.
        for (int i = 0; i < extraOwners.length; i++) {
            if (!owners.contains(extraOwners[i])) {
                owners.add(extraOwners[i]);
            }
        }
        // 소유 객체마다 허용 키 집합과 실제 키를 비교한다.
        for (int i = 0; i < owners.size(); i++) {
            String path = owners.get(i);
            Set<Object> allowed = new HashSet<Object>();
            // 표의 행에서 이 소유 객체의 키를 허용한다.
            for (int j = 0; j < rows.size(); j++) {
                if (path.equals(rows.get(j)[0])) {
                    allowed.add(rows.get(j)[1]);
                }
            }
            JsObject o = owner(path);
            if (o.isCallable()) {
                allowed.add("length");
                allowed.add("name");
                allowed.add("prototype");
            }
            if (path.endsWith(".prototype")) {
                allowed.add("constructor");
            }
            if (path.equals("Date.prototype")) {
                allowed.add(WellKnownSymbols.TO_PRIMITIVE);
            }
            if (path.equals("RegExp") || path.equals("Map") || path.equals("Set")) {
                allowed.add(WellKnownSymbols.SPECIES);
            }
            if (path.equals("JSON") || path.equals("Map.prototype") || path.equals("Set.prototype") || path.equals("WeakMap.prototype")
                    || path.equals("WeakSet.prototype") || path.startsWith("%")) {
                allowed.add(WellKnownSymbols.TO_STRING_TAG);
            }
            List<Object> actual = o.ownKeys();
            // 실제 키가 모두 허용 집합에 있는지 확인한다.
            for (int j = 0; j < actual.size(); j++) {
                Check.isTrue(allowed.contains(actual.get(j)), path + " has the unexpected property " + keyText(actual.get(j)));
            }
            // 허용된 키가 모두 실제로 있는지도 확인한다(필수 항목이 빠지지 않았는지).
            for (Object k : allowed) {
                Check.isTrue(actual.contains(k), path + " lacks the property " + keyText(k));
            }
        }
    }

    /**
     * Same function objects: toGMTString/toUTCString, Map entries/@@iterator, Set values/keys/@@iterator.
     * <p>
     * 같은 함수 객체: toGMTString/toUTCString, Map entries/@@iterator, Set values/keys/@@iterator.
     */
    public void testSharedFunctionObjects() {
        JsObject dp = env.obj("Date.prototype");
        Check.same(dp.get("toUTCString"), dp.get("toGMTString"), "toGMTString is toUTCString");
        JsObject mp = env.obj("Map.prototype");
        Check.same(mp.get("entries"), mp.get(WellKnownSymbols.ITERATOR), "Map @@iterator is entries");
        JsObject sp = env.obj("Set.prototype");
        Check.same(sp.get("values"), sp.get("keys"), "Set keys is values");
        Check.same(sp.get("values"), sp.get(WellKnownSymbols.ITERATOR), "Set @@iterator is values");
        JsValue a = env.obj("Map.prototype").get("get");
        JsValue b = env.obj("WeakMap.prototype").get("get");
        Check.isFalse(a == b, "Map.prototype.get and WeakMap.prototype.get are different functions");
    }
}
