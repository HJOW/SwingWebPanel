package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.JsFunction;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Table driven check of everything that {@link StringArrayInstaller} installs (plan item P14-09): the exact set of properties, and for every
 * function the <code>name</code>, <code>length</code> and property attributes (writable, non-enumerable, configurable), including the
 * properties of the function objects themselves.
 * <p>
 * {@link StringArrayInstaller} 가 설치하는 모든 것을 표로 검사한다(계획 P14-09): 정확한 프로퍼티 집합과, 모든 함수의 <code>name</code>,
 * <code>length</code>, 프로퍼티 속성(writable, 열거 불가, configurable). 함수 객체 자체의 프로퍼티도 포함한다.
 */
public class PropertyTableTest {

    /**
     * Test environment.
     * <p>
     * 시험 환경.
     */
    private Env e;

    /**
     * ES2015 String.prototype methods: name, length.
     * <p>
     * ES2015 String.prototype 메서드: 이름, length.
     */
    static final String[][] STRING_PROTOTYPE = { { "charAt", "1" }, { "charCodeAt", "1" }, { "codePointAt", "1" }, { "concat", "1" },
        { "endsWith", "1" }, { "includes", "1" }, { "indexOf", "1" }, { "lastIndexOf", "1" }, { "localeCompare", "1" }, { "match", "1" },
        { "normalize", "0" }, { "repeat", "1" }, { "replace", "2" }, { "search", "1" }, { "slice", "2" }, { "split", "2" }, { "startsWith", "1" },
        { "substring", "2" }, { "toLocaleLowerCase", "0" }, { "toLocaleUpperCase", "0" }, { "toLowerCase", "0" }, { "toString", "0" },
        { "toUpperCase", "0" }, { "trim", "0" }, { "valueOf", "0" } };

    /**
     * Annex B String.prototype methods: name, length.
     * <p>
     * 부속서 B String.prototype 메서드: 이름, length.
     */
    static final String[][] STRING_ANNEX_B = { { "substr", "2" }, { "anchor", "1" }, { "big", "0" }, { "blink", "0" }, { "bold", "0" },
        { "fixed", "0" }, { "fontcolor", "1" }, { "fontsize", "1" }, { "italics", "0" }, { "link", "1" }, { "small", "0" }, { "strike", "0" },
        { "sub", "0" }, { "sup", "0" } };

    /**
     * Documented post-ES2015 extensions of String.prototype.
     * <p>
     * 문서화한 ES2015 이후 String.prototype 확장.
     */
    static final String[][] STRING_EXTENSIONS = { { "trimStart", "0" }, { "trimEnd", "0" }, { "padStart", "1" }, { "padEnd", "1" },
        { "at", "1" }, { "replaceAll", "2" }, { "matchAll", "1" } };

    /**
     * ES2015 Array.prototype methods: name, length.
     * <p>
     * ES2015 Array.prototype 메서드: 이름, length.
     */
    static final String[][] ARRAY_PROTOTYPE = { { "concat", "1" }, { "copyWithin", "2" }, { "entries", "0" }, { "every", "1" },
        { "fill", "1" }, { "filter", "1" }, { "find", "1" }, { "findIndex", "1" }, { "forEach", "1" }, { "indexOf", "1" }, { "join", "1" },
        { "keys", "0" }, { "lastIndexOf", "1" }, { "map", "1" }, { "pop", "0" }, { "push", "1" }, { "reduce", "1" }, { "reduceRight", "1" },
        { "reverse", "0" }, { "shift", "0" }, { "slice", "2" }, { "some", "1" }, { "sort", "1" }, { "splice", "2" }, { "toLocaleString", "0" },
        { "toString", "0" }, { "unshift", "1" }, { "values", "0" } };

    /**
     * Documented post-ES2015 extensions of Array.prototype.
     * <p>
     * 문서화한 ES2015 이후 Array.prototype 확장.
     */
    static final String[][] ARRAY_EXTENSIONS = { { "includes", "1" }, { "flat", "0" }, { "flatMap", "1" }, { "at", "1" } };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PropertyTableTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new Env();
    }

    /**
     * Checks a function-valued property: attributes of the property, and name, length and attributes of the function object.
     * <p>
     * 함수 값 프로퍼티를 검사한다: 프로퍼티의 속성, 함수 객체의 name, length 와 그 속성.
     *
     * @param holder   object that has the property / 프로퍼티를 가진 객체
     * @param key      the key / 키
     * @param name     expected function name / 기대하는 함수 이름
     * @param length   expected length / 기대하는 length
     * @param describe label for messages / 메시지용 설명
     */
    private void checkFunction(JsObject holder, Object key, String name, int length, String describe) {
        PropertyDescriptor d = holder.getOwnProperty(key);
        Check.notNull(d, describe + " exists");
        Check.isTrue(d.isData() && d.isWritableTrue() && !d.isEnumerableTrue() && d.isConfigurableTrue(), describe + " attributes (writable, not enumerable, configurable)");
        Check.isTrue(d.value instanceof JsFunction, describe + " is a function");
        JsFunction f = (JsFunction) d.value;
        Check.same(e.realm.getFunctionPrototype(), f.getPrototypeOf(), describe + " inherits Function.prototype");
        PropertyDescriptor n = f.getOwnProperty("name");
        Check.equal(name, Ops.toString(n.value), describe + " name");
        Check.isTrue(!n.isWritableTrue() && !n.isEnumerableTrue() && n.isConfigurableTrue(), describe + " name attributes");
        PropertyDescriptor l = f.getOwnProperty("length");
        Check.equal(length, (long) Ops.toNumber(l.value), describe + " length");
        Check.isTrue(!l.isWritableTrue() && !l.isEnumerableTrue() && l.isConfigurableTrue(), describe + " length attributes");
        Check.isFalse(f.isConstructor(), describe + " is not a constructor");
        Check.isFalse(f.hasOwnProperty("prototype"), describe + " has no prototype property");
    }

    /**
     * Checks all table rows against a prototype and returns the names.
     * <p>
     * 표의 모든 줄을 프로토타입에 대해 검사하고 이름을 돌려준다.
     *
     * @param proto  the prototype / 프로토타입
     * @param rows   name and length rows / 이름과 length 행
     * @param prefix label prefix / 메시지 접두사
     * @param names  collects the names / 이름을 모은다
     */
    private void checkRows(JsObject proto, String[][] rows, String prefix, Set<String> names) {
        // 표의 행마다 함수와 속성 특성을 검사한다.
        for (int i = 0; i < rows.length; i++) {
            checkFunction(proto, rows[i][0], rows[i][0], Integer.parseInt(rows[i][1]), prefix + rows[i][0]);
            names.add(rows[i][0]);
        }
    }

    /**
     * Collects the string keys of an object as a set.
     * <p>
     * 객체의 문자열 키를 집합으로 모은다.
     *
     * @param o the object / 객체
     * @return string keys / 문자열 키
     */
    private static Set<String> stringKeys(JsObject o) {
        Set<String> keys = new HashSet<String>();
        List<Object> all = o.ownKeys();
        // 심볼 키는 제외하고 문자열 키만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof String) {
                keys.add((String) all.get(i));
            }
        }
        return keys;
    }

    /**
     * Collects the symbol keys of an object as a set.
     * <p>
     * 객체의 심볼 키를 집합으로 모은다.
     *
     * @param o the object / 객체
     * @return symbol keys / 심볼 키
     */
    private static Set<JsSymbol> symbolKeys(JsObject o) {
        Set<JsSymbol> keys = new HashSet<JsSymbol>();
        List<Object> all = o.ownKeys();
        // 문자열 키는 제외하고 심볼 키만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof JsSymbol) {
                keys.add((JsSymbol) all.get(i));
            }
        }
        return keys;
    }

    /**
     * Every String.prototype property: attributes, names, lengths, and no unexpected properties.
     * <p>
     * String.prototype 의 모든 프로퍼티: 속성, 이름, length, 예상하지 못한 프로퍼티가 없음.
     */
    public void testStringPrototypeTable() {
        JsObject sp = e.realm.getStringPrototype();
        Set<String> expected = new HashSet<String>();
        checkRows(sp, STRING_PROTOTYPE, "String.prototype.", expected);
        checkRows(sp, STRING_ANNEX_B, "String.prototype.", expected);
        checkRows(sp, STRING_EXTENSIONS, "String.prototype.", expected);
        expected.add("constructor");
        expected.add("length");
        expected.add("trimLeft");
        expected.add("trimRight");
        Check.equal(expected, stringKeys(sp), "exact string keys of String.prototype");
        PropertyDescriptor ctor = sp.getOwnProperty("constructor");
        Check.isTrue(ctor.isWritableTrue() && !ctor.isEnumerableTrue() && ctor.isConfigurableTrue(), "constructor attributes");
        PropertyDescriptor len = sp.getOwnProperty("length");
        Check.isTrue(!len.isWritableTrue() && !len.isEnumerableTrue() && !len.isConfigurableTrue(), "String.prototype.length attributes");
        checkFunction(sp, WellKnownSymbols.ITERATOR, "[Symbol.iterator]", 0, "String.prototype[@@iterator]");
        Check.equal(new HashSet<JsSymbol>(Arrays.asList(WellKnownSymbols.ITERATOR)), symbolKeys(sp), "exact symbol keys of String.prototype");
        Check.same(sp.get("trimStart"), sp.get("trimLeft"), "trimLeft alias");
        Check.same(sp.get("trimEnd"), sp.get("trimRight"), "trimRight alias");
        Check.equal("trimStart", Ops.toString(((JsObject) sp.get("trimLeft")).get("name")), "name of the aliased function");
    }

    /**
     * Every Array.prototype property: attributes, names, lengths, and no unexpected properties.
     * <p>
     * Array.prototype 의 모든 프로퍼티: 속성, 이름, length, 예상하지 못한 프로퍼티가 없음.
     */
    public void testArrayPrototypeTable() {
        JsObject ap = e.realm.getArrayPrototype();
        Set<String> expected = new HashSet<String>();
        checkRows(ap, ARRAY_PROTOTYPE, "Array.prototype.", expected);
        checkRows(ap, ARRAY_EXTENSIONS, "Array.prototype.", expected);
        expected.add("constructor");
        expected.add("length");
        Check.equal(expected, stringKeys(ap), "exact string keys of Array.prototype");
        PropertyDescriptor len = ap.getOwnProperty("length");
        Check.isTrue(len.isWritableTrue() && !len.isEnumerableTrue() && !len.isConfigurableTrue(), "Array.prototype.length attributes");
        PropertyDescriptor iter = ap.getOwnProperty(WellKnownSymbols.ITERATOR);
        Check.isTrue(iter.isWritableTrue() && !iter.isEnumerableTrue() && iter.isConfigurableTrue(), "@@iterator attributes");
        Check.same(ap.get("values"), iter.value, "@@iterator is the values function");
        Check.equal("values", Ops.toString(((JsObject) iter.value).get("name")), "its name is values");
        PropertyDescriptor un = ap.getOwnProperty(WellKnownSymbols.UNSCOPABLES);
        Check.isTrue(!un.isWritableTrue() && !un.isEnumerableTrue() && un.isConfigurableTrue(), "@@unscopables attributes");
        Check.equal(new HashSet<JsSymbol>(Arrays.asList(WellKnownSymbols.ITERATOR, WellKnownSymbols.UNSCOPABLES)), symbolKeys(ap), "exact symbol keys of Array.prototype");
    }

    /**
     * The constructors and their static functions.
     * <p>
     * 생성자와 정적 함수.
     */
    public void testConstructorsAndStatics() {
        JsObject string = e.global("String");
        JsObject array = e.global("Array");
        Check.equal("String", Ops.toString(string.get("name")), "String.name");
        Check.equal(1, (long) Ops.toNumber(string.get("length")), "String.length");
        Check.equal("Array", Ops.toString(array.get("name")), "Array.name");
        Check.equal(1, (long) Ops.toNumber(array.get("length")), "Array.length");
        Check.isTrue(((JsFunction) string).isConstructor() && ((JsFunction) array).isConstructor(), "both are constructors");
        PropertyDescriptor sproto = string.getOwnProperty("prototype");
        Check.isTrue(!sproto.isWritableTrue() && !sproto.isEnumerableTrue() && !sproto.isConfigurableTrue(), "String.prototype attributes");
        PropertyDescriptor aproto = array.getOwnProperty("prototype");
        Check.isTrue(!aproto.isWritableTrue() && !aproto.isEnumerableTrue() && !aproto.isConfigurableTrue(), "Array.prototype attributes");
        checkFunction(string, "fromCharCode", "fromCharCode", 1, "String.fromCharCode");
        checkFunction(string, "fromCodePoint", "fromCodePoint", 1, "String.fromCodePoint");
        checkFunction(string, "raw", "raw", 1, "String.raw");
        checkFunction(array, "isArray", "isArray", 1, "Array.isArray");
        checkFunction(array, "of", "of", 0, "Array.of");
        checkFunction(array, "from", "from", 1, "Array.from");
        Set<String> sKeys = new HashSet<String>(Arrays.asList("length", "name", "prototype", "fromCharCode", "fromCodePoint", "raw"));
        Check.equal(sKeys, stringKeys(string), "exact keys of String");
        Set<String> aKeys = new HashSet<String>(Arrays.asList("length", "name", "prototype", "isArray", "of", "from"));
        Check.equal(aKeys, stringKeys(array), "exact keys of Array");
        PropertyDescriptor species = array.getOwnProperty(WellKnownSymbols.SPECIES);
        Check.isTrue(species.isAccessor() && !species.isEnumerableTrue() && species.isConfigurableTrue(), "Array[@@species] is a configurable accessor");
        Check.isTrue(species.set == null || species.set.isUndefined(), "without a setter");
        Check.equal("get [Symbol.species]", Ops.toString(((JsObject) species.get).get("name")), "getter name");
        Check.equal(0, (long) Ops.toNumber(((JsObject) species.get).get("length")), "getter length");
        JsValue g = e.realm.getGlobalObject().getOwnProperty("String").value;
        Check.same(string, g, "String is a global");
        PropertyDescriptor gd = e.realm.getGlobalObject().getOwnProperty("Array");
        Check.isTrue(gd.isWritableTrue() && !gd.isEnumerableTrue() && gd.isConfigurableTrue(), "global Array attributes");
    }

    /**
     * The iterator prototypes.
     * <p>
     * 이터레이터 프로토타입.
     */
    public void testIteratorPrototypes() {
        String[] names = { "%ArrayIteratorPrototype%", "%StringIteratorPrototype%" };
        String[] tags = { "Array Iterator", "String Iterator" };
        // 두 이터레이터 프로토타입에 같은 규칙을 적용한다.
        for (int i = 0; i < names.length; i++) {
            JsObject p = e.realm.getIntrinsic(names[i]);
            Check.notNull(p, names[i] + " is registered");
            Check.same(e.realm.getIteratorPrototype(), p.getPrototypeOf(), names[i] + " inherits %IteratorPrototype%");
            checkFunction(p, "next", "next", 0, names[i] + ".next");
            PropertyDescriptor tag = p.getOwnProperty(WellKnownSymbols.TO_STRING_TAG);
            Check.equal(tags[i], Ops.toString(tag.value), names[i] + " @@toStringTag");
            Check.isTrue(!tag.isWritableTrue() && !tag.isEnumerableTrue() && tag.isConfigurableTrue(), names[i] + " @@toStringTag attributes");
            List<Object> keys = new ArrayList<Object>(p.ownKeys());
            Check.equal(2, keys.size(), names[i] + " has only next and @@toStringTag");
        }
    }
}
