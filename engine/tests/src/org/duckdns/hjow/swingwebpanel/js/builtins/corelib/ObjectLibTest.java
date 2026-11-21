package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Object constructor, its static methods, Object.prototype and the Annex B members (plan item P14-01, Object part).
 * <p>
 * Object 생성자, 정적 메서드, Object.prototype, 부속서 B 멤버의 시험(계획 P14-01 의 Object 부분).
 */
public class ObjectLibTest {

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
    public ObjectLibTest() {
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
     * Object() and new Object(): conversion, wrapper objects and subclass construction.
     * <p>
     * Object() 와 new Object(): 변환, 래퍼 객체, 하위 클래스 생성.
     */
    public void testConstructor() {
        JsValue empty = env.call("Object");
        Check.isTrue(empty instanceof JsObject && ((JsObject) empty).getPrototypeOf() == env.realm.getObjectPrototype(), "Object() makes an ordinary object");
        Check.isTrue(env.call("Object", Js.NULL) instanceof JsObject, "Object(null)");
        JsObject wrapped = (JsObject) env.call("Object", LibEnv.n(5));
        Check.equal("Number", wrapped.className(), "Object(5) is a Number wrapper");
        JsObject same = env.obj();
        Check.same(same, env.call("Object", same), "Object(obj) returns obj");
        JsObject created = env.construct("Object", LibEnv.s("x"));
        Check.equal("String", created.className(), "new Object('x') is a String wrapper");
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) env.get("Object.length")).getValue(), "Object.length is 1");
        // new.target 이 다른 생성자이면 그 프로토타입을 쓴다.
        JsObject subProto = env.realm.newObject();
        NativeFunction other = new NativeFunction(env.realm, "Other", 0, new NativeFunction.Call() {
            public JsValue call(org.duckdns.hjow.swingwebpanel.js.JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(org.duckdns.hjow.swingwebpanel.js.JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        });
        other.defineData("prototype", subProto, false, false, false);
        JsObject viaSub = env.fn("Object").construct(Js.NO_ARGS, other);
        Check.same(subProto, viaSub.getPrototypeOf(), "prototype comes from new.target");
    }

    /**
     * Object.keys / getOwnPropertyNames / getOwnPropertySymbols: key kinds, enumerability and order.
     * <p>
     * Object.keys / getOwnPropertyNames / getOwnPropertySymbols: 키 종류, 열거 가능성, 순서.
     */
    public void testKeysAndNamesOrder() {
        JsObject o = env.realm.newObject();
        o.createDataProperty("b", LibEnv.n(1));
        o.createDataProperty("a", LibEnv.n(2));
        o.createDataProperty("2", LibEnv.n(3));
        o.createDataProperty("1", LibEnv.n(4));
        JsSymbol sym = new JsSymbol("s");
        o.createDataProperty(sym, LibEnv.n(5));
        o.defineData("hidden", LibEnv.n(6), true, false, true);
        Check.equal("1,2,b,a", LibEnv.join(env.call("Object.keys", o)), "integer keys first, then insertion order, no symbols");
        Check.equal("1,2,b,a,hidden", LibEnv.join(env.call("Object.getOwnPropertyNames", o)), "names include non-enumerable");
        JsValue syms = env.call("Object.getOwnPropertySymbols", o);
        Check.equal(1, (long) org.duckdns.hjow.swingwebpanel.js.Ops.lengthOf((JsObject) syms), "one symbol");
        Check.same(sym, ((JsObject) syms).get("0"), "the symbol");
        Check.equal("0,1,2", LibEnv.join(env.call("Object.keys", LibEnv.s("abc"))), "string indices");
        Check.equal("0,1,2,length", LibEnv.join(env.call("Object.getOwnPropertyNames", LibEnv.s("abc"))), "string names include length");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.keys", Js.UNDEFINED);
            }
        }, "Object.keys(undefined)");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.getOwnPropertyNames", Js.NULL);
            }
        }, "Object.getOwnPropertyNames(null)");
    }

    /**
     * Object.create with and without a property map.
     * <p>
     * 프로퍼티 맵이 있을 때와 없을 때의 Object.create.
     */
    public void testCreate() {
        JsObject proto = env.obj("p", LibEnv.n(1));
        JsObject desc = env.obj("value", LibEnv.n(7), "enumerable", Js.TRUE);
        JsObject props = env.obj("x", desc);
        JsObject o = (JsObject) env.call("Object.create", proto, props);
        Check.same(proto, o.getPrototypeOf(), "prototype");
        Check.equal(7.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) o.get("x")).getValue(), "defined value");
        PropertyDescriptor d = o.getOwnProperty("x");
        Check.isFalse(d.isWritableTrue(), "omitted attributes default to false");
        Check.isFalse(d.isConfigurableTrue(), "omitted configurable defaults to false");
        JsObject bare = (JsObject) env.call("Object.create", Js.NULL);
        Check.isNull(bare.getPrototypeOf(), "null prototype");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.create", LibEnv.n(1));
            }
        }, "Object.create(1)");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.create", Js.UNDEFINED);
            }
        }, "Object.create(undefined)");
    }

    /**
     * Object.defineProperty: descriptor conversion, defaults and errors.
     * <p>
     * Object.defineProperty: 기술자 변환, 기본값, 오류.
     */
    public void testDefineProperty() {
        final JsObject o = env.realm.newObject();
        env.call("Object.defineProperty", o, LibEnv.s("a"), env.obj("value", LibEnv.n(1)));
        PropertyDescriptor d = o.getOwnProperty("a");
        Check.isFalse(d.isWritableTrue() || d.isEnumerableTrue() || d.isConfigurableTrue(), "attributes default to false");
        final JsObject getter = env.fn("Object.is");
        env.call("Object.defineProperty", o, LibEnv.s("acc"), env.obj("get", getter, "configurable", Js.TRUE));
        PropertyDescriptor acc = o.getOwnProperty("acc");
        Check.isTrue(acc.isAccessor(), "accessor property");
        Check.same(getter, acc.get, "getter");
        Check.isTrue(acc.set.isUndefined(), "missing setter is undefined");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.defineProperty", o, LibEnv.s("bad"), env.obj("get", getter, "value", LibEnv.n(1)));
            }
        }, "accessor and value together");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.defineProperty", o, LibEnv.s("bad"), env.obj("get", LibEnv.n(5)));
            }
        }, "non-callable getter");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.defineProperty", o, LibEnv.s("a"), env.obj("value", LibEnv.n(2)));
            }
        }, "redefining a non-configurable property");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.defineProperty", LibEnv.n(1), LibEnv.s("a"), env.obj());
            }
        }, "non-object target");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.defineProperty", o, LibEnv.s("c"), LibEnv.n(3));
            }
        }, "non-object descriptor");
        env.call("Object.defineProperty", o, LibEnv.s("a"), env.obj("value", LibEnv.n(1)));
    }

    /**
     * Object.defineProperties validates all descriptors before defining any property.
     * <p>
     * Object.defineProperties 는 어떤 프로퍼티를 정의하기 전에 모든 기술자를 검증한다.
     */
    public void testDefinePropertiesIsValidatedFirst() {
        final JsObject o = env.realm.newObject();
        final JsObject props = env.obj("good", env.obj("value", LibEnv.n(1)), "bad", LibEnv.n(5));
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.defineProperties", o, props);
            }
        }, "a bad descriptor");
        Check.isFalse(o.hasOwnProperty("good"), "nothing is defined when a descriptor is invalid");
        JsObject ok = env.obj("good", env.obj("value", LibEnv.n(1), "enumerable", Js.TRUE));
        Check.same(o, env.call("Object.defineProperties", o, ok), "returns the object");
        Check.isTrue(o.hasOwnProperty("good"), "defined");
        JsObject hiddenProps = env.realm.newObject();
        hiddenProps.defineData("skipped", env.obj("value", LibEnv.n(1)), true, false, true);
        env.call("Object.defineProperties", o, hiddenProps);
        Check.isFalse(o.hasOwnProperty("skipped"), "non-enumerable entries of the map are ignored");
    }

    /**
     * Object.assign: enumerable own properties incl. symbols, getters, skipping null sources and strict failures.
     * <p>
     * Object.assign: 열거 가능한 자체 프로퍼티(심볼 포함), getter, null 원본 건너뛰기, 엄격한 실패.
     */
    public void testAssign() {
        JsSymbol sym = new JsSymbol("k");
        JsObject src = env.obj("a", LibEnv.n(1));
        src.createDataProperty(sym, LibEnv.n(2));
        src.defineData("hidden", LibEnv.n(3), true, false, true);
        JsObject target = env.realm.newObject();
        JsValue result = env.call("Object.assign", target, src, Js.NULL, Js.UNDEFINED, LibEnv.s("xy"), LibEnv.n(5));
        Check.same(target, result, "returns the target");
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) target.get("a")).getValue(), "copied");
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) target.get(sym)).getValue(), "symbol copied");
        Check.isFalse(target.hasOwnProperty("hidden"), "non-enumerable not copied");
        Check.equal("x", LibEnv.str(target.get("0")), "string source copies indices");
        Check.equal("y", LibEnv.str(target.get("1")), "string source copies indices");
        final JsObject frozen = env.realm.newObject();
        frozen.defineData("a", LibEnv.n(0), false, true, true);
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.assign", frozen, env.obj("a", LibEnv.n(1)));
            }
        }, "assigning to a read-only property throws");
        Check.isTrue(env.call("Object.assign", LibEnv.n(1)) instanceof JsObject, "a primitive target is wrapped");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.assign", Js.UNDEFINED);
            }
        }, "undefined target");
    }

    /**
     * freeze / seal / preventExtensions and their predicates, including arrays and primitives.
     * <p>
     * freeze / seal / preventExtensions 와 판별 함수. 배열과 원시 값을 포함한다.
     */
    public void testIntegrityLevels() {
        final JsObject o = env.obj("a", LibEnv.n(1));
        o.defineAccessor("acc", env.fn("Object.is"), null, true, true);
        Check.isFalse(env.call("Object.isExtensible", LibEnv.n(1)).equals(Js.TRUE), "primitives are not extensible");
        Check.same(Js.TRUE, env.call("Object.isFrozen", LibEnv.n(1)), "primitives are frozen");
        Check.same(Js.TRUE, env.call("Object.isSealed", LibEnv.s("x")), "primitives are sealed");
        Check.same(Js.FALSE, env.call("Object.isFrozen", o), "not frozen yet");
        Check.same(o, env.call("Object.seal", o), "seal returns the object");
        Check.same(Js.TRUE, env.call("Object.isSealed", o), "sealed");
        Check.same(Js.FALSE, env.call("Object.isFrozen", o), "sealed but writable: not frozen");
        Check.isFalse(o.isExtensible(), "sealing prevents extensions");
        env.call("Object.freeze", o);
        Check.same(Js.TRUE, env.call("Object.isFrozen", o), "frozen");
        Check.isFalse(o.getOwnProperty("a").isWritableTrue(), "data property no longer writable");
        Check.isTrue(o.getOwnProperty("acc").isAccessor(), "accessors stay accessors");
        Check.isFalse(o.getOwnProperty("acc").isConfigurableTrue(), "accessor not configurable");
        Check.same(Js.TRUE, env.call("Object.isFrozen", env.call("Object.preventExtensions", env.realm.newObject())), "empty non-extensible object is frozen");
        Check.same(Js.FALSE, env.call("Object.isSealed", env.obj("x", LibEnv.n(1))), "extensible object is not sealed");
        JsArray arr = env.realm.newArray(new JsValue[] { LibEnv.n(1), LibEnv.n(2) });
        env.call("Object.freeze", arr);
        Check.same(Js.TRUE, env.call("Object.isFrozen", arr), "frozen array");
        Check.isFalse(arr.set("0", LibEnv.n(9)), "frozen array elements cannot be changed");
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) arr.get("0")).getValue(), "element unchanged");
        Check.isFalse(arr.set("5", LibEnv.n(9)), "frozen array cannot grow");
        Check.equal("5", LibEnv.str(env.call("Object.freeze", LibEnv.n(5))), "freeze of a primitive returns it");
    }

    /**
     * Object.getOwnPropertyDescriptor for data, accessor, missing and primitive subjects.
     * <p>
     * 데이터, 접근자, 없는 프로퍼티, 원시 값에 대한 Object.getOwnPropertyDescriptor.
     */
    public void testGetOwnPropertyDescriptor() {
        JsObject o = env.obj("a", LibEnv.n(1));
        JsObject d = (JsObject) env.call("Object.getOwnPropertyDescriptor", o, LibEnv.s("a"));
        Check.equal("value,writable,enumerable,configurable", LibEnv.join(env.call("Object.keys", d)), "data descriptor keys in spec order");
        Check.same(Js.TRUE, d.get("writable"), "writable");
        o.defineAccessor("g", env.fn("Object.is"), null, false, true);
        JsObject a = (JsObject) env.call("Object.getOwnPropertyDescriptor", o, LibEnv.s("g"));
        Check.equal("get,set,enumerable,configurable", LibEnv.join(env.call("Object.keys", a)), "accessor descriptor keys");
        Check.isTrue(a.get("set").isUndefined(), "missing setter is undefined");
        Check.isTrue(env.call("Object.getOwnPropertyDescriptor", o, LibEnv.s("nope")).isUndefined(), "missing property");
        JsObject len = (JsObject) env.call("Object.getOwnPropertyDescriptor", LibEnv.s("abc"), LibEnv.s("length"));
        Check.equal(3.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) len.get("value")).getValue(), "string length");
        Check.same(Js.FALSE, len.get("writable"), "string length is read only");
        JsSymbol sym = new JsSymbol("q");
        o.defineData(sym, LibEnv.n(1), true, true, true);
        Check.isTrue(env.call("Object.getOwnPropertyDescriptor", o, sym) instanceof JsObject, "symbol keys work");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.getOwnPropertyDescriptor", Js.NULL, LibEnv.s("a"));
            }
        }, "null subject");
    }

    /**
     * Object.getPrototypeOf and setPrototypeOf with their error cases.
     * <p>
     * 오류 경우를 포함한 Object.getPrototypeOf 와 setPrototypeOf.
     */
    public void testPrototypeAccess() {
        JsObject p = env.obj();
        final JsObject o = env.realm.newObject();
        Check.same(env.realm.getObjectPrototype(), env.call("Object.getPrototypeOf", o), "default prototype");
        Check.same(env.realm.getStringPrototype(), env.call("Object.getPrototypeOf", LibEnv.s("x")), "primitive prototype");
        Check.same(Js.NULL, env.call("Object.getPrototypeOf", env.call("Object.create", Js.NULL)), "null prototype is null");
        Check.same(o, env.call("Object.setPrototypeOf", o, p), "returns the object");
        Check.same(p, o.getPrototypeOf(), "prototype changed");
        env.call("Object.setPrototypeOf", o, Js.NULL);
        Check.isNull(o.getPrototypeOf(), "null prototype");
        Check.equal("5", LibEnv.str(env.call("Object.setPrototypeOf", LibEnv.n(5), Js.NULL)), "primitive is returned unchanged");
        final JsObject a = env.realm.newObject();
        final JsObject b = (JsObject) env.call("Object.create", a);
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.setPrototypeOf", a, b);
            }
        }, "prototype cycle");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.setPrototypeOf", a, LibEnv.n(1));
            }
        }, "non-object prototype");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.setPrototypeOf", Js.UNDEFINED, Js.NULL);
            }
        }, "undefined subject");
        final JsObject locked = env.realm.newObject();
        locked.preventExtensions();
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Object.setPrototypeOf", locked, env.obj());
            }
        }, "non-extensible object");
    }

    /**
     * Object.is compares by SameValue.
     * <p>
     * Object.is 는 SameValue 로 비교한다.
     */
    public void testIs() {
        Check.same(Js.TRUE, env.call("Object.is", LibEnv.n(Double.NaN), LibEnv.n(Double.NaN)), "NaN is NaN");
        Check.same(Js.FALSE, env.call("Object.is", LibEnv.n(0.0), LibEnv.n(-0.0)), "+0 is not -0");
        Check.same(Js.TRUE, env.call("Object.is", LibEnv.s("a"), LibEnv.s("a")), "equal strings");
        Check.same(Js.FALSE, env.call("Object.is", env.obj(), env.obj()), "different objects");
        Check.same(Js.TRUE, env.call("Object.is"), "undefined is undefined");
    }

    /**
     * Object.prototype methods hasOwnProperty, isPrototypeOf, propertyIsEnumerable and valueOf.
     * <p>
     * Object.prototype 의 hasOwnProperty, isPrototypeOf, propertyIsEnumerable, valueOf 메서드.
     */
    public void testPrototypeMethods() {
        final JsObject o = env.obj("a", LibEnv.n(1));
        o.defineData("h", LibEnv.n(1), true, false, true);
        Check.same(Js.TRUE, env.callWith("Object.prototype.hasOwnProperty", o, LibEnv.s("a")), "own");
        Check.same(Js.FALSE, env.callWith("Object.prototype.hasOwnProperty", o, LibEnv.s("toString")), "inherited is not own");
        Check.same(Js.TRUE, env.callWith("Object.prototype.hasOwnProperty", LibEnv.s("abc"), LibEnv.s("length")), "primitive receiver");
        Check.same(Js.TRUE, env.callWith("Object.prototype.propertyIsEnumerable", o, LibEnv.s("a")), "enumerable");
        Check.same(Js.FALSE, env.callWith("Object.prototype.propertyIsEnumerable", o, LibEnv.s("h")), "not enumerable");
        Check.same(Js.FALSE, env.callWith("Object.prototype.propertyIsEnumerable", o, LibEnv.s("zz")), "missing");
        JsObject child = (JsObject) env.call("Object.create", o);
        Check.same(Js.TRUE, env.callWith("Object.prototype.isPrototypeOf", o, child), "isPrototypeOf");
        Check.same(Js.FALSE, env.callWith("Object.prototype.isPrototypeOf", child, o), "not the other way round");
        Check.same(Js.FALSE, env.callWith("Object.prototype.isPrototypeOf", o, LibEnv.n(1)), "primitive argument");
        Check.same(o, env.callWith("Object.prototype.valueOf", o), "valueOf returns the object");
        Check.equal("Number", ((JsObject) env.callWith("Object.prototype.valueOf", LibEnv.n(1))).className(), "valueOf wraps a primitive");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Object.prototype.hasOwnProperty", Js.NULL, LibEnv.s("a"));
            }
        }, "hasOwnProperty on null");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Object.prototype.valueOf", Js.UNDEFINED);
            }
        }, "valueOf on undefined");
    }

    /**
     * Object.prototype.toString: built-in tags, @@toStringTag and toLocaleString.
     * <p>
     * Object.prototype.toString: 내장 태그, @@toStringTag, toLocaleString.
     */
    public void testToString() {
        Check.equal("[object Undefined]", LibEnv.str(env.callWith("Object.prototype.toString", Js.UNDEFINED)), "undefined");
        Check.equal("[object Null]", LibEnv.str(env.callWith("Object.prototype.toString", Js.NULL)), "null");
        Check.equal("[object Object]", LibEnv.str(env.callWith("Object.prototype.toString", env.obj())), "plain object");
        Check.equal("[object Array]", LibEnv.str(env.callWith("Object.prototype.toString", env.realm.newArray())), "array");
        Check.equal("[object Function]", LibEnv.str(env.callWith("Object.prototype.toString", env.fn("Object"))), "function");
        Check.equal("[object Error]", LibEnv.str(env.callWith("Object.prototype.toString", env.construct("TypeError"))), "error object");
        Check.equal("[object Number]", LibEnv.str(env.callWith("Object.prototype.toString", LibEnv.n(1))), "number");
        Check.equal("[object String]", LibEnv.str(env.callWith("Object.prototype.toString", LibEnv.s("s"))), "string");
        Check.equal("[object Boolean]", LibEnv.str(env.callWith("Object.prototype.toString", Js.TRUE)), "boolean");
        Check.equal("[object Symbol]", LibEnv.str(env.callWith("Object.prototype.toString", new JsSymbol("x"))), "symbol via @@toStringTag");
        Check.equal("[object Math]", LibEnv.str(env.callWith("Object.prototype.toString", env.get("Math"))), "Math via @@toStringTag");
        JsObject tagged = env.obj();
        tagged.createDataProperty(WellKnownSymbols.TO_STRING_TAG, LibEnv.s("Custom"));
        Check.equal("[object Custom]", LibEnv.str(env.callWith("Object.prototype.toString", tagged)), "custom tag");
        JsObject notString = env.obj();
        notString.createDataProperty(WellKnownSymbols.TO_STRING_TAG, LibEnv.n(1));
        Check.equal("[object Object]", LibEnv.str(env.callWith("Object.prototype.toString", notString)), "non-string tag is ignored");
        JsObject withToString = env.obj("toString", env.fn("Object.prototype.valueOf"));
        Check.same(withToString, env.callWith("Object.prototype.toLocaleString", withToString), "toLocaleString calls toString");
        Check.equal("[object Object]", LibEnv.str(env.callWith("Object.prototype.toLocaleString", env.obj())), "toLocaleString default");
    }

    /**
     * Annex B: the __proto__ accessor.
     * <p>
     * 부속서 B: __proto__ 접근자.
     */
    public void testProtoAccessor() {
        JsObject o = env.realm.newObject();
        JsObject p = env.obj();
        PropertyDescriptor d = env.realm.getObjectPrototype().getOwnProperty("__proto__");
        Check.isTrue(d.isAccessor(), "accessor");
        Check.isFalse(d.isEnumerableTrue(), "not enumerable");
        Check.isTrue(d.isConfigurableTrue(), "configurable");
        JsObject getter = (JsObject) d.get;
        JsObject setter = (JsObject) d.set;
        Check.same(env.realm.getObjectPrototype(), getter.call(o, Js.NO_ARGS), "getter reads the prototype");
        setter.call(o, new JsValue[] { p });
        Check.same(p, o.getPrototypeOf(), "setter changes the prototype");
        setter.call(o, new JsValue[] { LibEnv.n(1) });
        Check.same(p, o.getPrototypeOf(), "a non-object value is ignored");
        setter.call(LibEnv.n(5), new JsValue[] { p });
        setter.call(o, new JsValue[] { Js.NULL });
        Check.isNull(o.getPrototypeOf(), "null prototype through the setter");
        Check.same(Js.NULL, getter.call(o, Js.NO_ARGS), "getter returns null");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                ((JsObject) env.realm.getObjectPrototype().getOwnProperty("__proto__").set).call(Js.UNDEFINED, new JsValue[] { Js.NULL });
            }
        }, "setter on undefined");
        Check.equal("get __proto__", LibEnv.str(getter.get("name")), "getter name");
        Check.equal("set __proto__", LibEnv.str(setter.get("name")), "setter name");
    }

    /**
     * Annex B: __defineGetter__, __defineSetter__, __lookupGetter__, __lookupSetter__.
     * <p>
     * 부속서 B: __defineGetter__, __defineSetter__, __lookupGetter__, __lookupSetter__.
     */
    public void testLegacyAccessorMethods() {
        final JsObject o = env.realm.newObject();
        JsObject g = env.fn("Object.is");
        JsObject s = env.fn("Object.keys");
        Check.same(Js.UNDEFINED, env.callWith("Object.prototype.__defineGetter__", o, LibEnv.s("x"), g), "returns undefined");
        env.callWith("Object.prototype.__defineSetter__", o, LibEnv.s("x"), s);
        PropertyDescriptor d = o.getOwnProperty("x");
        Check.isTrue(d.isAccessor() && d.isEnumerableTrue() && d.isConfigurableTrue(), "enumerable configurable accessor");
        Check.same(g, d.get, "getter");
        Check.same(s, d.set, "setter");
        JsObject child = (JsObject) env.call("Object.create", o);
        Check.same(g, env.callWith("Object.prototype.__lookupGetter__", child, LibEnv.s("x")), "lookup through the chain");
        Check.same(s, env.callWith("Object.prototype.__lookupSetter__", child, LibEnv.s("x")), "setter lookup");
        Check.same(Js.UNDEFINED, env.callWith("Object.prototype.__lookupGetter__", child, LibEnv.s("none")), "missing");
        o.defineData("plain", LibEnv.n(1), true, true, true);
        Check.same(Js.UNDEFINED, env.callWith("Object.prototype.__lookupGetter__", o, LibEnv.s("plain")), "data property has no getter");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Object.prototype.__defineGetter__", o, LibEnv.s("y"), LibEnv.n(1));
            }
        }, "non-callable getter");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Object.prototype.__defineSetter__", Js.NULL, LibEnv.s("y"), env.fn("Object.is"));
            }
        }, "null receiver");
    }

    /**
     * The ES2017 methods must not exist (ES2015 conformance: nothing from later editions is added to Object).
     * <p>
     * ES2017 메서드는 없어야 한다(ES2015 적합성: 이후 판본의 것을 Object 에 더하지 않는다).
     */
    public void testNoLaterEditionMethods() {
        Check.isTrue(env.get("Object.entries").isUndefined(), "Object.entries is ES2017");
        Check.isTrue(env.get("Object.values").isUndefined(), "Object.values is ES2017");
        Check.isTrue(env.get("Object.getOwnPropertyDescriptors").isUndefined(), "getOwnPropertyDescriptors is ES2017");
    }

}
