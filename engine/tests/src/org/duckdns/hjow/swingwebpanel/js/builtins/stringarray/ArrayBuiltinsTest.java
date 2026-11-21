package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Array constructor, <code>Array.isArray</code>, <code>Array.of</code>, <code>Array.from</code> and species handling (ECMA-262 6th
 * edition 22.1.1, 22.1.2 and 9.4.2.3).
 * <p>
 * Array 생성자, <code>Array.isArray</code>, <code>Array.of</code>, <code>Array.from</code>, species 처리 시험(ECMA-262 6판 22.1.1, 22.1.2, 9.4.2.3).
 */
public class ArrayBuiltinsTest {

    /**
     * Test environment.
     * <p>
     * 시험 환경.
     */
    private Env e;

    /**
     * The Array constructor.
     * <p>
     * Array 생성자.
     */
    private JsObject array;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ArrayBuiltinsTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new Env();
        array = e.global("Array");
    }

    /**
     * Constructs an array with the Array constructor.
     * <p>
     * Array 생성자로 배열을 만든다.
     *
     * @param args arguments / 인수
     * @return the array / 배열
     */
    private JsObject construct(Object... args) {
        return Ops.construct(array, e.vs(args));
    }

    /**
     * Number and element forms of the constructor, in call and construct form.
     * <p>
     * 호출/생성 형태의 생성자에서 숫자 인수와 요소 인수.
     */
    public void testConstructorForms() {
        Check.equal(0, Env.i(construct().get("length")), "new Array()");
        JsObject three = construct(3);
        Check.equal(3, Env.i(three.get("length")), "new Array(3)");
        Check.isFalse(three.hasOwnProperty("0"), "new Array(3) has holes");
        Check.equal(Arrays.asList("3"), Env.list(construct("3")), "a non-number argument is the only element");
        Check.equal(Arrays.asList("1", "2", "3"), Env.list(construct(1, 2, 3)), "several elements");
        Check.equal(Arrays.asList("undefined"), Env.list(construct((Object) null)), "undefined as the only element");
        Check.equal(Arrays.asList("<hole>", "<hole>"), Env.list(Ops.call(array, Js.UNDEFINED, e.vs(2))), "Array(2) called as a function");
        Check.equal(4294967295L, Env.i(construct(4294967295.0).get("length")), "largest length");
        Check.same(e.realm.getArrayPrototype(), construct().getPrototypeOf(), "prototype");
        Check.same(array, e.realm.getArrayPrototype().get("constructor"), "Array.prototype.constructor");
        Check.same(array, e.realm.getIntrinsic("%Array%"), "intrinsic");
        Object[] bad = { Double.valueOf(2.5), Integer.valueOf(-1), Double.valueOf(4294967296.0), Double.valueOf(Double.NaN),
            Double.valueOf(Double.POSITIVE_INFINITY) };
        // 올바른 배열 길이가 아닌 숫자는 RangeError 이다.
        for (int i = 0; i < bad.length; i++) {
            final Object len = bad[i];
            Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    construct(len);
                }
            }), "new Array(" + len + ")");
        }
    }

    /**
     * new.target decides the prototype of the new array (subclassing).
     * <p>
     * new.target 이 새 배열의 프로토타입을 정한다(하위 클래스).
     */
    public void testNewTargetPrototype() {
        JsObject subProto = new JsObject(e.realm.getArrayPrototype());
        NativeFunction sub = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        sub.defineData("prototype", subProto, false, false, false);
        JsObject made = ((NativeFunction) array).construct(e.vs(1, 2), sub);
        Check.same(subProto, made.getPrototypeOf(), "prototype taken from new.target");
        Check.equal(2, Env.i(made.get("length")), "length");
        Check.isTrue(made.isArray(), "still an array exotic object");
    }

    /**
     * Tests of Array.isArray.
     * <p>
     * Array.isArray 시험.
     */
    public void testIsArray() {
        Check.isTrue(Ops.toBoolean(e.call(array, "isArray", e.array(1))), "array");
        Check.isTrue(Ops.toBoolean(e.call(array, "isArray", e.realm.getArrayPrototype())), "Array.prototype is an array");
        Check.isFalse(Ops.toBoolean(e.call(array, "isArray", e.arrayLike(1, 2))), "array-like object");
        Check.isFalse(Ops.toBoolean(e.call(array, "isArray", "abc")), "string");
        Check.isFalse(Ops.toBoolean(e.call(array, "isArray")), "no argument");
    }

    /**
     * Array.of with the default constructor and with a custom this value.
     * <p>
     * 기본 생성자와 사용자 정의 this 값으로 Array.of.
     */
    public void testOf() {
        Check.equal(Arrays.asList("1", "2", "3"), Env.list(e.call(array, "of", 1, 2, 3)), "Array.of(1,2,3)");
        Check.equal(Arrays.asList("3"), Env.list(e.call(array, "of", 3)), "Array.of(3) makes an element, not a length");
        Check.equal(0, Env.list(e.call(array, "of")).size(), "Array.of()");
        final List<String> log = new ArrayList<String>();
        NativeFunction ctor = new NativeFunction(e.realm, "C", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                log.add("new C(" + Env.describe(args[0]) + ")");
                return r.newObject();
            }
        });
        JsObject made = (JsObject) Ops.call(array.get("of"), ctor, e.vs("a", "b"));
        Check.equal("new C(2)", log.get(0), "the constructor receives the length");
        Check.equal("b", Env.str(made.get("1")), "elements are defined as data properties");
        Check.equal(2, Env.i(made.get("length")), "length is set at the end");
        JsObject plain = (JsObject) Ops.call(array.get("of"), Js.UNDEFINED, e.vs("x"));
        Check.isTrue(plain.isArray(), "a non-constructor this gives an Array");
    }

    /**
     * Array.from with strings, array-likes, mapping and a custom this value.
     * <p>
     * 문자열, 유사 배열, 매핑, 사용자 정의 this 값으로 Array.from.
     */
    public void testFrom() {
        String text = "a" + new String(Character.toChars(0x1F600)) + "b";
        Check.equal(3, Env.list(e.call(array, "from", text)).size(), "a string is iterated by code points");
        Check.equal(Arrays.asList("a", "b", "undefined"), Env.list(e.call(array, "from", e.arrayLike("a", "b", Env.HOLE))), "holes of array-likes become undefined");
        Check.equal(0, Env.list(e.call(array, "from", 5)).size(), "a number has no length");
        Check.equal(0, Env.list(e.call(array, "from", e.realm.newObject())).size(), "an object without length");
        final List<String> calls = new ArrayList<String>();
        final JsObject thisArg = e.realm.newObject();
        JsValue mapFn = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add(Env.describe(args[0]) + "@" + Env.describe(args[1]) + "/" + (thisValue == thisArg ? "T" : "other"));
                return Js.num(Ops.toNumber(args[0]) * 2);
            }
        });
        Check.equal(Arrays.asList("2", "4"), Env.list(e.call(array, "from", e.array(1, 2), mapFn, thisArg)), "mapFn result");
        Check.equal(Arrays.asList("1@0/T", "2@1/T"), calls, "mapFn arguments and thisArg");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.call(array, "from", e.array(1), "not a function");
            }
        }), "mapFn must be callable");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.call(array, "from", Js.NULL);
            }
        }), "null items");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.call(array, "from");
            }
        }), "undefined items");
    }

    /**
     * Array.from over a custom iterable: values come from the iterator and the iterator is closed when the mapping function throws.
     * <p>
     * 사용자 정의 이터러블에 대한 Array.from: 값은 이터레이터에서 오고 매핑 함수가 던지면 이터레이터를 닫는다.
     */
    public void testFromIterableClosesTheIterator() {
        final int[] state = new int[2];
        JsObject iterable = e.realm.newObject();
        e.realm.method(iterable, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject it = r.newObject();
                r.method(it, "next", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue t, JsValue[] a) {
                        JsObject res = rr.newObject();
                        res.createDataProperty("value", Js.num(++state[0]));
                        res.createDataProperty("done", Js.bool(state[0] > 3));
                        return res;
                    }
                });
                r.method(it, "return", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue t, JsValue[] a) {
                        state[1]++;
                        return rr.newObject();
                    }
                });
                return it;
            }
        });
        Check.equal(Arrays.asList("1", "2", "3"), Env.list(e.call(array, "from", iterable)), "values of the iterator");
        Check.equal(0, state[1], "a finished iterator is not closed");
        state[0] = 0;
        final JsValue failing = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("map failed");
            }
        });
        final JsObject it2 = iterable;
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                e.call(array, "from", it2, failing);
            }
        }), "the exception of mapFn propagates");
        Check.equal(1, state[1], "the iterator is closed once");
    }

    /**
     * Array.from with a custom constructor creates the result with it and sets the length.
     * <p>
     * 사용자 정의 생성자를 쓰는 Array.from 은 그 생성자로 결과를 만들고 length 를 지정한다.
     */
    public void testFromWithCustomConstructor() {
        final List<String> log = new ArrayList<String>();
        NativeFunction ctor = new NativeFunction(e.realm, "C", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                log.add("new C(" + args.length + ")");
                return r.newObject();
            }
        });
        JsObject made = (JsObject) Ops.call(array.get("from"), ctor, e.vs(e.array("a", "b")));
        Check.equal("new C(0)", log.get(0), "an iterable result is constructed without arguments");
        Check.equal("b", Env.str(made.get("1")), "elements");
        Check.equal(2, Env.i(made.get("length")), "length");
        JsObject made2 = (JsObject) Ops.call(array.get("from"), ctor, e.vs(e.arrayLike("a", "b", "c")));
        Check.equal("new C(1)", log.get(1), "an array-like result is constructed with the length");
        Check.equal(3, Env.i(made2.get("length")), "length of the array-like result");
    }

    /**
     * The @@species getter returns this.
     * <p>
     * @@species getter 는 this 를 돌려준다.
     */
    public void testSpeciesGetter() {
        Check.same(array, array.get(WellKnownSymbols.SPECIES), "Array[Symbol.species]");
        JsObject other = e.realm.newObject();
        Check.same(other, Ops.call(array.getOwnProperty(WellKnownSymbols.SPECIES).get, other, Js.NO_ARGS), "getter returns this");
    }

    /**
     * Array methods that create arrays use the constructor named by @@species of the receiver's constructor.
     * <p>
     * 배열을 만드는 메서드는 수신자 생성자의 @@species 가 가리키는 생성자를 쓴다.
     */
    public void testSpeciesCreate() {
        final List<String> log = new ArrayList<String>();
        final NativeFunction species = new NativeFunction(e.realm, "S", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                log.add("new S(" + Env.describe(args[0]) + ")");
                return r.newObject();
            }
        });
        JsArray source = e.array(1, 2, 3);
        JsObject ctor = e.realm.newObject();
        ctor.defineData(WellKnownSymbols.SPECIES, species, true, false, true);
        source.createDataProperty("constructor", ctor);
        JsObject mapped = (JsObject) e.call(source, "map", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return args[0];
            }
        }));
        Check.equal(Arrays.asList("new S(3)"), log, "map asks the species constructor for an array of the length");
        Check.equal("3", Env.str(mapped.get("2")), "elements are stored with CreateDataProperty");
        e.call(source, "slice", 1);
        e.call(source, "filter", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.TRUE;
            }
        }));
        e.call(source, "splice", 0, 1);
        e.call(source, "concat", 5);
        Check.equal(Arrays.asList("new S(3)", "new S(2)", "new S(0)", "new S(1)", "new S(0)"), log, "slice, filter, splice and concat use the species constructor");
        ctor.defineData(WellKnownSymbols.SPECIES, Js.NULL, true, false, true);
        Check.isTrue(((JsObject) e.call(source, "slice")).getPrototypeOf() == e.realm.getArrayPrototype(), "null species gives a plain array");
        ctor.defineData(WellKnownSymbols.SPECIES, e.realm.newObject(), true, false, true);
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                JsArray bad = e.array(1);
                JsObject c2 = e.realm.newObject();
                c2.defineData(WellKnownSymbols.SPECIES, e.realm.newObject(), true, false, true);
                bad.createDataProperty("constructor", c2);
                e.call(bad, "slice");
            }
        }), "a species that is not a constructor");
    }

    /**
     * Methods called on a non-array object create plain arrays.
     * <p>
     * 배열이 아닌 객체에서 호출한 메서드는 일반 배열을 만든다.
     */
    public void testNonArrayReceiverGivesPlainArray() {
        JsObject like = e.arrayLike("a", "b", "c");
        JsObject result = (JsObject) e.callOn(e.realm.getArrayPrototype(), "slice", like, 1);
        Check.isTrue(result.isArray(), "result is an Array");
        Check.equal(Arrays.asList("b", "c"), Env.list(result), "elements");
    }
}
