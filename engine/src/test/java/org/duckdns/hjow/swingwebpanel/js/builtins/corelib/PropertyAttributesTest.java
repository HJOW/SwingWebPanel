package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Table-driven check of everything the core library installs (plan item P14-09, part for these objects): for every object the exact set of
 * function properties with the specified <code>length</code>, the <code>name</code> of each function, the attributes of the properties
 * (writable, not enumerable, configurable for methods), the absence of a <code>prototype</code> on non-constructors, and the wiring of
 * the constructors (global binding, <code>prototype</code>, <code>constructor</code>, intrinsic name).
 * <p>
 * 핵심 라이브러리가 설치하는 모든 것의 표 기반 확인(계획 P14-09 의 이 객체들 부분): 객체마다 지정된 <code>length</code> 를 가진 함수 프로퍼티의
 * 정확한 집합, 각 함수의 <code>name</code>, 프로퍼티 속성(메서드는 writable, 열거 아님, configurable), 비생성자에 <code>prototype</code>
 * 이 없음, 생성자의 연결(전역 바인딩, <code>prototype</code>, <code>constructor</code>, 내장 객체 이름).
 */
public class PropertyAttributesTest {

    /**
     * The expected function properties per owner object: the first entry is the path of the owner, the others are "name:length"
     * (symbol-keyed functions are written "@@hasInstance").
     * <p>
     * 소유 객체별 기대하는 함수 프로퍼티: 첫 항목은 소유 객체의 경로, 나머지는 "이름:length"(심볼 키 함수는 "@@hasInstance" 형식).
     */
    private static final String[][] TABLE = {
        { "Object", "assign:2", "create:2", "defineProperties:2", "defineProperty:3", "freeze:1", "groupBy:2", "getOwnPropertyDescriptor:2",
            "entries:1", "fromEntries:1", "getOwnPropertyDescriptors:1", "getOwnPropertyNames:1", "getOwnPropertySymbols:1", "getPrototypeOf:1",
            "hasOwn:2", "is:2", "isExtensible:1", "isFrozen:1", "isSealed:1", "keys:1", "preventExtensions:1", "seal:1",
            "setPrototypeOf:2", "values:1" },
        { "Object.prototype", "__defineGetter__:2", "__defineSetter__:2", "__lookupGetter__:1", "__lookupSetter__:1", "hasOwnProperty:1",
            "isPrototypeOf:1", "propertyIsEnumerable:1", "toLocaleString:0", "toString:0", "valueOf:0" },
        { "Function.prototype", "apply:2", "bind:1", "call:1", "toString:0", "@@hasInstance:1" },
        { "Boolean.prototype", "toString:0", "valueOf:0" },
        { "Symbol", "for:1", "keyFor:1" },
        { "Symbol.prototype", "toString:0", "valueOf:0", "@@toPrimitive:1" },
        { "Error.prototype", "toString:0" },
        { "EvalError.prototype" }, { "RangeError.prototype" }, { "ReferenceError.prototype" }, { "SyntaxError.prototype" },
        { "TypeError.prototype" }, { "URIError.prototype" }, { "AggregateError.prototype" },
        { "Number", "isFinite:1", "isInteger:1", "isNaN:1", "isSafeInteger:1", "parseFloat:1", "parseInt:2" },
        { "Number.prototype", "toExponential:1", "toFixed:1", "toLocaleString:0", "toPrecision:1", "toString:1", "valueOf:0" },
        { "BigInt", "asIntN:2", "asUintN:2" },
        { "BigInt.prototype", "toLocaleString:0", "toString:0", "valueOf:0" },
        { "Math", "abs:1", "acos:1", "acosh:1", "asin:1", "asinh:1", "atan:1", "atanh:1", "atan2:2", "cbrt:1", "ceil:1", "clz32:1", "cos:1",
            "cosh:1", "exp:1", "expm1:1", "f16round:1", "floor:1", "fround:1", "hypot:2", "imul:2", "log:1", "log1p:1", "log10:1", "log2:1",
            "max:2", "min:2",
            "pow:2", "random:0", "round:1", "sign:1", "sin:1", "sinh:1", "sqrt:1", "sumPrecise:1", "tan:1", "tanh:1", "trunc:1" },
    };

    /**
     * The constructors with their "length".
     * <p>
     * 생성자와 "length".
     */
    private static final String[] CONSTRUCTORS = { "Object:1", "Boolean:1", "Symbol:0", "Error:1", "EvalError:1", "RangeError:1",
        "ReferenceError:1", "SyntaxError:1", "TypeError:1", "URIError:1", "AggregateError:2", "Number:1", "BigInt:1" };

    /**
     * The global functions with their "length" (besides the constructors).
     * <p>
     * (생성자를 뺀) 전역 함수와 "length".
     */
    private static final String[] GLOBAL_FUNCTIONS = { "parseFloat:1", "parseInt:2", "isNaN:1", "isFinite:1", "decodeURI:1",
        "decodeURIComponent:1", "encodeURI:1", "encodeURIComponent:1" };

    /**
     * Property names that are not functions and that the global object must have, besides functions and constructors.
     * <p>
     * 함수와 생성자 외에 전역 객체가 가져야 하는 함수가 아닌 프로퍼티 이름.
     */
    private static final String[] GLOBAL_VALUES = { "NaN", "Infinity", "undefined", "Math" };

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
    public PropertyAttributesTest() {
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
     * Converts a property key into the notation of the table.
     * <p>
     * 프로퍼티 키를 표의 표기로 바꾼다.
     *
     * @param key the key / 키
     * @return "name" or "@@symbol" / "이름" 또는 "@@심볼"
     */
    private static String keyText(Object key) {
        if (key instanceof JsSymbol) {
            String d = ((JsSymbol) key).getDescription();
            return "@@" + (d.startsWith("Symbol.") ? d.substring("Symbol.".length()) : d);
        }
        return (String) key;
    }

    /**
     * Returns the key for a table name.
     * <p>
     * 표의 이름에 해당하는 키를 돌려준다.
     *
     * @param text "name" or "@@symbol" / "이름" 또는 "@@심볼"
     * @return the key / 키
     */
    private static Object keyOf(String text) {
        if (!text.startsWith("@@")) {
            return text;
        }
        String s = text.substring(2);
        if ("hasInstance".equals(s)) {
            return WellKnownSymbols.HAS_INSTANCE;
        }
        if ("toPrimitive".equals(s)) {
            return WellKnownSymbols.TO_PRIMITIVE;
        }
        throw new IllegalArgumentException(text);
    }

    /**
     * Checks one function property: attributes, name, length, absence of a prototype.
     * <p>
     * 함수 프로퍼티 하나를 확인한다: 속성, 이름, length, prototype 없음.
     *
     * @param ownerPath the owner path for messages / 메시지용 소유 객체 경로
     * @param owner     the owner object / 소유 객체
     * @param key       the key / 키
     * @param length    the expected length / 기대하는 length
     */
    private void checkFunction(String ownerPath, JsObject owner, Object key, int length) {
        String where = ownerPath + "." + keyText(key);
        PropertyDescriptor d = owner.getOwnProperty(key);
        Check.notNull(d, where + " must exist");
        Check.isTrue(d.isData(), where + " must be a data property");
        boolean readOnly = key == WellKnownSymbols.HAS_INSTANCE;
        boolean configurable = key != WellKnownSymbols.HAS_INSTANCE;
        if (key == WellKnownSymbols.TO_PRIMITIVE) {
            readOnly = true;
            configurable = true;
        }
        Check.equal(!readOnly, d.isWritableTrue(), where + " writable");
        Check.isFalse(d.isEnumerableTrue(), where + " must not be enumerable");
        Check.equal(configurable, d.isConfigurableTrue(), where + " configurable");
        Check.isTrue(d.value instanceof JsObject && d.value.isCallable(), where + " must be a function");
        JsObject f = (JsObject) d.value;
        String expectedName = key instanceof JsSymbol ? "[" + ((JsSymbol) key).getDescription() + "]" : (String) key;
        Check.equal(expectedName, f.getString("name"), where + " name");
        Check.equal((double) length, ((JsNumber) f.get("length")).getValue(), where + " length");
        List<Object> own = f.ownKeys();
        Check.equal("length", own.get(0), where + " first own property");
        Check.equal("name", own.get(1), where + " second own property");
        PropertyDescriptor ld = f.getOwnProperty("length");
        PropertyDescriptor nd = f.getOwnProperty("name");
        Check.isTrue(!ld.isWritableTrue() && !ld.isEnumerableTrue() && ld.isConfigurableTrue(), where + " length attributes");
        Check.isTrue(!nd.isWritableTrue() && !nd.isEnumerableTrue() && nd.isConfigurableTrue(), where + " name attributes");
        Check.isFalse(f.hasOwnProperty("prototype"), where + " prototype property");
        Check.isFalse(f.isConstructor(), where + " must not be a constructor");
    }

    /**
     * For every owner object, the function properties are exactly the expected ones.
     * <p>
     * 모든 소유 객체에서 함수 프로퍼티가 정확히 기대한 것과 같다.
     */
    public void testFunctionPropertiesOfEveryObject() {
        for (int t = 0; t < TABLE.length; t++) { // 표의 행마다 소유 객체를 찾아 함수 프로퍼티를 확인한다.
            String ownerPath = TABLE[t][0];
            JsObject owner = (JsObject) env.get(ownerPath);
            Set<Object> expectedKeys = new HashSet<Object>();
            for (int i = 1; i < TABLE[t].length; i++) { // 기대 항목마다 이름과 length 를 해석해 확인한다.
                String entry = TABLE[t][i];
                int colon = entry.lastIndexOf(':');
                Object key = keyOf(entry.substring(0, colon));
                expectedKeys.add(key);
                checkFunction(ownerPath, owner, key, Integer.parseInt(entry.substring(colon + 1)));
            }
            List<Object> keys = owner.ownKeys();
            for (int i = 0; i < keys.size(); i++) { // 표에 없는 함수가 설치되어 있지 않은지 본다.
                Object key = keys.get(i);
                PropertyDescriptor d = owner.getOwnProperty(key);
                boolean isFunction = d.isData() && d.value instanceof JsObject && d.value.isCallable();
                if (isFunction && !"constructor".equals(key)) {
                    Check.isTrue(expectedKeys.contains(key), ownerPath + "." + keyText(key) + " is installed but not in the table");
                }
            }
        }
    }

    /**
     * The constructors are wired as the specification requires.
     * <p>
     * 생성자가 규격대로 연결되어 있다.
     */
    public void testConstructorWiring() {
        JsObject global = env.realm.getGlobalObject();
        for (int i = 0; i < CONSTRUCTORS.length; i++) { // 생성자마다 전역 바인딩, prototype, constructor, 내장 객체 이름을 확인한다.
            String entry = CONSTRUCTORS[i];
            int colon = entry.indexOf(':');
            String name = entry.substring(0, colon);
            int length = Integer.parseInt(entry.substring(colon + 1));
            PropertyDescriptor g = global.getOwnProperty(name);
            Check.isTrue(g.isWritableTrue() && !g.isEnumerableTrue() && g.isConfigurableTrue(), name + " global binding attributes");
            JsObject ctor = (JsObject) g.value;
            Check.isTrue(ctor.isConstructor() && ctor.isCallable(), name + " is a constructor");
            Check.equal(name, ctor.getString("name"), name + " name");
            Check.equal((double) length, ((JsNumber) ctor.get("length")).getValue(), name + " length");
            Check.same(ctor, env.realm.getIntrinsic("%" + name + "%"), name + " intrinsic");
            PropertyDescriptor p = ctor.getOwnProperty("prototype");
            Check.isTrue(!p.isWritableTrue() && !p.isEnumerableTrue() && !p.isConfigurableTrue(), name + ".prototype attributes");
            JsObject proto = (JsObject) p.value;
            PropertyDescriptor c = proto.getOwnProperty("constructor");
            Check.same(ctor, c.value, name + ".prototype.constructor");
            Check.isTrue(c.isWritableTrue() && !c.isEnumerableTrue() && c.isConfigurableTrue(), name + ".prototype.constructor attributes");
        }
    }

    /**
     * The global object has exactly the expected own properties of this library.
     * <p>
     * 전역 객체가 이 라이브러리의 기대한 자체 프로퍼티만 정확히 가진다.
     */
    public void testGlobalObjectProperties() {
        JsObject global = env.realm.getGlobalObject();
        Set<String> expected = new HashSet<String>();
        for (int i = 0; i < GLOBAL_FUNCTIONS.length; i++) { // 전역 함수는 함수 프로퍼티 규칙으로 확인한다.
            String e = GLOBAL_FUNCTIONS[i];
            String name = e.substring(0, e.indexOf(':'));
            expected.add(name);
            checkFunction("global", global, name, Integer.parseInt(e.substring(e.indexOf(':') + 1)));
        }
        // 생성자 이름을 기대 집합에 더한다.
        for (int i = 0; i < CONSTRUCTORS.length; i++) {
            expected.add(CONSTRUCTORS[i].substring(0, CONSTRUCTORS[i].indexOf(':')));
        }
        expected.addAll(Arrays.asList(GLOBAL_VALUES));
        expected.add("Function");
        List<String> unexpected = new ArrayList<String>();
        List<Object> keys = global.ownKeys();
        for (int i = 0; i < keys.size(); i++) { // 표에 없는 전역 프로퍼티가 있으면 모은다.
            if (!expected.contains(keys.get(i))) {
                unexpected.add(String.valueOf(keys.get(i)));
            }
        }
        Check.isTrue(unexpected.isEmpty(), "unexpected global properties: " + unexpected);
        for (int i = 0; i < GLOBAL_VALUES.length; i++) { // 값 프로퍼티는 모두 존재해야 한다.
            Check.isTrue(global.hasOwnProperty(GLOBAL_VALUES[i]), GLOBAL_VALUES[i] + " must exist");
        }
    }

    /**
     * The accessor __proto__ of Object.prototype has the specified attributes and function names.
     * <p>
     * Object.prototype 의 접근자 __proto__ 가 규격의 속성과 함수 이름을 가진다.
     */
    public void testProtoAccessorAttributes() {
        PropertyDescriptor d = env.realm.getObjectPrototype().getOwnProperty("__proto__");
        Check.isTrue(d.isAccessor(), "accessor");
        Check.isTrue(!d.isEnumerableTrue() && d.isConfigurableTrue(), "attributes");
        JsValue g = d.get;
        JsValue s = d.set;
        Check.equal(0.0, ((JsNumber) ((JsObject) g).get("length")).getValue(), "getter length");
        Check.equal(1.0, ((JsNumber) ((JsObject) s).get("length")).getValue(), "setter length");
        Check.isFalse(((JsObject) g).isConstructor() || ((JsObject) s).isConstructor(), "accessor functions are not constructors");
    }
}
