package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Function.prototype methods, Boolean and Symbol (plan item P14-01, parts Function, Boolean and Symbol).
 * <p>
 * Function.prototype 메서드, Boolean, Symbol 의 시험(계획 P14-01 의 Function, Boolean, Symbol 부분).
 */
public class FunctionBooleanSymbolTest {

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
    public FunctionBooleanSymbolTest() {
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
     * Creates a function "probe" (length 2) that returns an array of its this value followed by its arguments.
     * <p>
     * this 값과 인수들을 담은 배열을 돌려주는 함수 "probe"(length 2)를 만든다.
     *
     * @return the function / 함수
     */
    private NativeFunction probe() {
        return new NativeFunction(env.realm, "probe", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue[] all = new JsValue[args.length + 1];
                all[0] = thisValue;
                System.arraycopy(args, 0, all, 1, args.length);
                return r.newArray(all);
            }
        });
    }

    /**
     * Function.prototype.call passes this and the remaining arguments.
     * <p>
     * Function.prototype.call 은 this 와 나머지 인수를 전달한다.
     */
    public void testCall() {
        JsObject thisArg = env.obj();
        JsValue r = env.callWith("Function.prototype.call", probe(), thisArg, LibEnv.n(1), LibEnv.s("b"));
        Check.same(thisArg, ((JsObject) r).get("0"), "this");
        Check.equal("1", LibEnv.str(((JsObject) r).get("1")), "first argument");
        Check.equal("b", LibEnv.str(((JsObject) r).get("2")), "second argument");
        Check.equal(3.0, ((JsNumber) ((JsObject) r).get("length")).getValue(), "this plus two arguments");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Function.prototype.call", env.obj());
            }
        }, "call on a non-function");
    }

    /**
     * Function.prototype.apply with array-likes, undefined, null and invalid argument lists.
     * <p>
     * 유사 배열, undefined, null, 잘못된 인수 목록을 쓰는 Function.prototype.apply.
     */
    public void testApply() {
        JsObject list = env.obj("length", LibEnv.n(2), "0", LibEnv.s("x"), "1", LibEnv.s("y"));
        JsObject r = (JsObject) env.callWith("Function.prototype.apply", probe(), LibEnv.n(7), list);
        Check.equal("7", LibEnv.str(r.get("0")), "this is the first argument");
        Check.equal("x", LibEnv.str(r.get("1")), "argument 0");
        Check.equal("y", LibEnv.str(r.get("2")), "argument 1");
        JsObject none = (JsObject) env.callWith("Function.prototype.apply", probe(), LibEnv.n(7), Js.NULL);
        Check.equal(1.0, ((JsNumber) none.get("length")).getValue(), "null argument list");
        JsObject none2 = (JsObject) env.callWith("Function.prototype.apply", probe(), LibEnv.n(7));
        Check.equal(1.0, ((JsNumber) none2.get("length")).getValue(), "missing argument list");
        JsObject arr = env.realm.newArray(new JsValue[] { LibEnv.n(1), LibEnv.n(2), LibEnv.n(3) });
        JsObject r3 = (JsObject) env.callWith("Function.prototype.apply", probe(), Js.UNDEFINED, arr);
        Check.equal(4.0, ((JsNumber) r3.get("length")).getValue(), "array argument list");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Function.prototype.apply", probe(), Js.UNDEFINED, LibEnv.n(5));
            }
        }, "a number is not an argument list");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Function.prototype.apply", LibEnv.s("x"));
            }
        }, "apply on a non-function");
    }

    /**
     * Function.prototype.bind: bound this and arguments, name, length and the prototype of the bound function.
     * <p>
     * Function.prototype.bind: 고정된 this 와 인수, 이름, length, 바운드 함수의 프로토타입.
     */
    public void testBind() {
        NativeFunction p = probe();
        JsObject thisArg = env.obj();
        JsObject bound = (JsObject) env.callWith("Function.prototype.bind", p, thisArg, LibEnv.n(1));
        JsObject r = (JsObject) bound.call(LibEnv.n(99), new JsValue[] { LibEnv.n(2) });
        Check.same(thisArg, r.get("0"), "the bound this wins");
        Check.equal("1", LibEnv.str(r.get("1")), "bound argument first");
        Check.equal("2", LibEnv.str(r.get("2")), "call argument second");
        Check.equal("bound probe", LibEnv.str(bound.get("name")), "name");
        Check.equal(1.0, ((JsNumber) bound.get("length")).getValue(), "length is reduced by the bound arguments");
        PropertyDescriptor ld = bound.getOwnProperty("length");
        Check.isFalse(ld.isWritableTrue() || ld.isEnumerableTrue(), "length is read-only");
        Check.isTrue(ld.isConfigurableTrue(), "length is configurable");
        Check.equal("length,name", LibEnv.join(env.call("Object.getOwnPropertyNames", bound)), "own properties in order");
        Check.same(p.getPrototypeOf(), bound.getPrototypeOf(), "prototype of the target");
        JsObject twice = (JsObject) env.callWith("Function.prototype.bind", bound, env.obj());
        Check.equal("bound bound probe", LibEnv.str(twice.get("name")), "bound twice");
        JsObject tooMany = (JsObject) env.callWith("Function.prototype.bind", p, Js.UNDEFINED, LibEnv.n(1), LibEnv.n(2), LibEnv.n(3));
        Check.equal(0.0, ((JsNumber) tooMany.get("length")).getValue(), "length never goes below 0");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Function.prototype.bind", env.obj());
            }
        }, "bind on a non-function");
    }

    /**
     * bind with unusual length and name properties of the target.
     * <p>
     * 대상의 length 와 name 프로퍼티가 특이한 경우의 bind.
     */
    public void testBindOfUnusualTargets() {
        NativeFunction inf = probe();
        inf.defineOwnProperty("length", PropertyDescriptor.data(Js.num(Double.POSITIVE_INFINITY), false, false, true));
        JsObject b1 = (JsObject) env.callWith("Function.prototype.bind", inf, Js.UNDEFINED, LibEnv.n(1));
        Check.equal(Double.POSITIVE_INFINITY, ((JsNumber) b1.get("length")).getValue(), "Infinity stays Infinity");
        NativeFunction neg = probe();
        neg.defineOwnProperty("length", PropertyDescriptor.data(Js.num(Double.NEGATIVE_INFINITY), false, false, true));
        JsObject b2 = (JsObject) env.callWith("Function.prototype.bind", neg, Js.UNDEFINED);
        Check.equal(0.0, ((JsNumber) b2.get("length")).getValue(), "-Infinity becomes 0");
        NativeFunction str = probe();
        str.defineOwnProperty("length", PropertyDescriptor.data(LibEnv.s("5"), false, false, true));
        str.defineOwnProperty("name", PropertyDescriptor.data(Js.num(1), false, false, true));
        JsObject b3 = (JsObject) env.callWith("Function.prototype.bind", str, Js.UNDEFINED);
        Check.equal(0.0, ((JsNumber) b3.get("length")).getValue(), "a non-number length counts as 0");
        Check.equal("bound ", LibEnv.str(b3.get("name")), "a non-string name counts as empty");
        NativeFunction frac = probe();
        frac.defineOwnProperty("length", PropertyDescriptor.data(Js.num(3.7), false, false, true));
        JsObject b4 = (JsObject) env.callWith("Function.prototype.bind", frac, Js.UNDEFINED, LibEnv.n(1));
        Check.equal(2.0, ((JsNumber) b4.get("length")).getValue(), "ToInteger(3.7) - 1");
    }

    /**
     * A bound constructor constructs the target and ignores the bound this value.
     * <p>
     * 바운드 생성자는 대상을 생성하며 고정된 this 값은 무시한다.
     */
    public void testBoundConstructor() {
        JsObject bound = (JsObject) env.callWith("Function.prototype.bind", env.fn("Error"), env.obj(), LibEnv.s("from bind"));
        Check.isTrue(bound.isConstructor(), "a bound constructor is a constructor");
        JsObject e = bound.construct(Js.NO_ARGS, bound);
        Check.equal("from bind", LibEnv.str(e.get("message")), "the bound argument reached the target");
        Check.same(env.get("Error.prototype"), e.getPrototypeOf(), "object of the target constructor");
        Check.isTrue(Ops.instanceOf(bound, e), "instanceof works with a bound function");
        Check.isTrue(Ops.instanceOf(env.fn("Error"), e), "and with the target");
        JsObject plain = (JsObject) env.callWith("Function.prototype.bind", probe(), Js.UNDEFINED);
        Check.isFalse(plain.isConstructor(), "a bound non-constructor is not a constructor");
    }

    /**
     * Function.prototype.toString for native functions, bound functions and non-functions.
     * <p>
     * 네이티브 함수, 바운드 함수, 함수가 아닌 값에 대한 Function.prototype.toString.
     */
    public void testToString() {
        Check.equal("function probe() { [native code] }", LibEnv.str(env.callWith("Function.prototype.toString", probe())), "native function");
        JsObject bound = (JsObject) env.callWith("Function.prototype.bind", probe(), Js.UNDEFINED);
        Check.equal("function () { [native code] }", LibEnv.str(env.callWith("Function.prototype.toString", bound)), "bound function");
        Check.equal("function () { [native code] }", LibEnv.str(env.callWith("Function.prototype.toString", env.realm.getFunctionPrototype())), "Function.prototype");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Function.prototype.toString", env.obj());
            }
        }, "toString on a plain object");
    }

    /**
     * Function.prototype[Symbol.hasInstance] and its attributes.
     * <p>
     * Function.prototype[Symbol.hasInstance] 와 그 속성.
     */
    public void testHasInstance() {
        JsObject fp = env.realm.getFunctionPrototype();
        PropertyDescriptor d = fp.getOwnProperty(WellKnownSymbols.HAS_INSTANCE);
        Check.isFalse(d.isWritableTrue() || d.isEnumerableTrue() || d.isConfigurableTrue(), "all attributes are false");
        JsObject fn = (JsObject) d.value;
        Check.equal("[Symbol.hasInstance]", LibEnv.str(fn.get("name")), "name");
        Check.equal(1.0, ((JsNumber) fn.get("length")).getValue(), "length");
        JsObject err = env.construct("TypeError");
        Check.same(Js.TRUE, fn.call(env.fn("TypeError"), new JsValue[] { err }), "an instance");
        Check.same(Js.FALSE, fn.call(env.fn("RangeError"), new JsValue[] { err }), "not an instance");
        Check.same(Js.FALSE, fn.call(env.obj(), new JsValue[] { err }), "a non-callable receiver");
        Check.same(Js.FALSE, fn.call(env.fn("Error"), new JsValue[] { LibEnv.n(1) }), "a primitive");
    }

    /**
     * Boolean conversions, wrapper objects and methods.
     * <p>
     * Boolean 변환, 래퍼 객체, 메서드.
     */
    public void testBoolean() {
        Check.same(Js.FALSE, env.call("Boolean"), "Boolean()");
        Check.same(Js.FALSE, env.call("Boolean", LibEnv.n(0)), "0");
        Check.same(Js.FALSE, env.call("Boolean", LibEnv.n(-0.0)), "-0");
        Check.same(Js.FALSE, env.call("Boolean", LibEnv.n(Double.NaN)), "NaN");
        Check.same(Js.FALSE, env.call("Boolean", LibEnv.s("")), "empty string");
        Check.same(Js.FALSE, env.call("Boolean", Js.NULL), "null");
        Check.same(Js.TRUE, env.call("Boolean", LibEnv.s("0")), "'0' is true");
        Check.same(Js.TRUE, env.call("Boolean", env.obj()), "object");
        Check.same(Js.TRUE, env.call("Boolean", new JsSymbol("s")), "symbol");
        JsObject wrapper = env.construct("Boolean", Js.FALSE);
        Check.equal("Boolean", wrapper.className(), "wrapper");
        Check.same(Js.TRUE, Js.bool(Ops.toBoolean(wrapper)), "a wrapper object is truthy");
        Check.same(Js.FALSE, env.callWith("Boolean.prototype.valueOf", wrapper), "valueOf");
        Check.equal("false", LibEnv.str(env.callWith("Boolean.prototype.toString", wrapper)), "toString of the wrapper");
        Check.equal("true", LibEnv.str(env.callWith("Boolean.prototype.toString", Js.TRUE)), "toString of a primitive");
        Check.equal("false", LibEnv.str(env.callWith("Boolean.prototype.toString", env.get("Boolean.prototype"))), "Boolean.prototype is a Boolean object for false");
        Check.same(env.get("Boolean"), ((JsObject) env.get("Boolean.prototype")).get("constructor"), "constructor link");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Boolean.prototype.valueOf", LibEnv.n(1));
            }
        }, "valueOf on a number");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Boolean.prototype.toString", env.obj());
            }
        }, "toString on a plain object");
    }

    /**
     * Symbol(): descriptions, uniqueness, no construction and conversions.
     * <p>
     * Symbol(): 설명, 유일성, 생성 불가, 변환.
     */
    public void testSymbolFunction() {
        JsSymbol a = (JsSymbol) env.call("Symbol", LibEnv.s("d"));
        JsSymbol b = (JsSymbol) env.call("Symbol", LibEnv.s("d"));
        Check.isTrue(a != b, "every symbol is unique");
        Check.equal("d", a.getDescription(), "description");
        Check.isNull(((JsSymbol) env.call("Symbol")).getDescription(), "no description");
        Check.equal("5", ((JsSymbol) env.call("Symbol", LibEnv.n(5))).getDescription(), "the description is converted to a string");
        Check.equal("Symbol(d)", LibEnv.str(env.callWith("Symbol.prototype.toString", a)), "toString");
        Check.equal("Symbol()", LibEnv.str(env.callWith("Symbol.prototype.toString", env.call("Symbol"))), "toString without description");
        Check.same(a, env.callWith("Symbol.prototype.valueOf", a), "valueOf");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.construct("Symbol");
            }
        }, "new Symbol()");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Symbol", new JsSymbol("inner"));
            }
        }, "a symbol cannot be converted to a description string");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Symbol.prototype.toString", LibEnv.s("x"));
            }
        }, "toString on a string");
        JsObject wrapper = Ops.toObject(env.realm, a);
        Check.same(a, env.callWith("Symbol.prototype.valueOf", wrapper), "valueOf of a wrapper");
        Check.same(a, Ops.toPrimitive(wrapper, Ops.HINT_STRING), "ToPrimitive of a wrapper uses @@toPrimitive");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.toString(env.call("Symbol"));
            }
        }, "implicit conversion to string");
        Check.equal(0.0, ((JsNumber) env.get("Symbol.length")).getValue(), "Symbol.length");
    }

    /**
     * The symbol registry and the well-known symbols.
     * <p>
     * 심볼 레지스트리와 잘 알려진 심볼.
     */
    public void testSymbolRegistryAndWellKnown() {
        JsValue x1 = env.call("Symbol.for", LibEnv.s("shared"));
        JsValue x2 = env.call("Symbol.for", LibEnv.s("shared"));
        Check.same(x1, x2, "Symbol.for returns the same symbol");
        Check.equal("shared", LibEnv.str(env.call("Symbol.keyFor", x1)), "keyFor");
        Check.isTrue(env.call("Symbol.keyFor", env.call("Symbol", LibEnv.s("shared"))).isUndefined(), "an unregistered symbol has no key");
        Check.isTrue(env.call("Symbol.keyFor", WellKnownSymbols.ITERATOR).isUndefined(), "a well-known symbol has no key");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.call("Symbol.keyFor", LibEnv.s("shared"));
            }
        }, "keyFor of a string");
        Check.same(env.realm.symbolFor("undefined"), env.call("Symbol.for"), "Symbol.for() uses the string 'undefined'");
        String[] names = { "hasInstance", "isConcatSpreadable", "iterator", "match", "replace", "search", "species", "split", "toPrimitive",
            "toStringTag", "unscopables" };
        JsSymbol[] symbols = { WellKnownSymbols.HAS_INSTANCE, WellKnownSymbols.IS_CONCAT_SPREADABLE, WellKnownSymbols.ITERATOR,
            WellKnownSymbols.MATCH, WellKnownSymbols.REPLACE, WellKnownSymbols.SEARCH, WellKnownSymbols.SPECIES, WellKnownSymbols.SPLIT,
            WellKnownSymbols.TO_PRIMITIVE, WellKnownSymbols.TO_STRING_TAG, WellKnownSymbols.UNSCOPABLES };
        JsObject ctor = (JsObject) env.get("Symbol");
        for (int i = 0; i < names.length; i++) { // 잘 알려진 심볼마다 값과 속성(모두 false)을 확인한다.
            PropertyDescriptor d = ctor.getOwnProperty(names[i]);
            Check.notNull(d, "Symbol." + names[i] + " must exist");
            Check.same(symbols[i], d.value, "Symbol." + names[i] + " value");
            Check.isFalse(d.isWritableTrue() || d.isEnumerableTrue() || d.isConfigurableTrue(), "Symbol." + names[i] + " attributes");
        }
        JsObject proto = env.realm.getSymbolPrototype();
        PropertyDescriptor tag = proto.getOwnProperty(WellKnownSymbols.TO_STRING_TAG);
        Check.equal("Symbol", LibEnv.str(tag.value), "@@toStringTag");
        Check.isTrue(tag.isConfigurableTrue() && !tag.isWritableTrue(), "@@toStringTag attributes");
        PropertyDescriptor tp = proto.getOwnProperty(WellKnownSymbols.TO_PRIMITIVE);
        Check.isTrue(tp.isConfigurableTrue() && !tp.isWritableTrue() && !tp.isEnumerableTrue(), "@@toPrimitive attributes");
        Check.equal("[Symbol.toPrimitive]", LibEnv.str(((JsObject) tp.value).get("name")), "@@toPrimitive function name");
    }
}
