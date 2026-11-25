package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Reflect object (ECMA-262 6th edition 26.1): every function with normal and error arguments, receiver semantics of
 * get and set, newTarget of construct, and the shape of the object (names, lengths, attributes).
 * <p>
 * Reflect 객체 시험(ECMA-262 6판 26.1): 모든 함수의 정상/오류 인수, get/set 의 receiver 의미, construct 의 newTarget, 객체의 모양(이름,
 * length, 속성).
 */
public class ReflectTest {

    /**
     * The environment of the running case.
     * <p>
     * 실행 중인 사례의 환경.
     */
    private MetaTestSupport.Env e;

    /**
     * The Reflect object.
     * <p>
     * Reflect 객체.
     */
    private JsObject reflect;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ReflectTest() {
    }

    /**
     * Creates a fresh realm for each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new MetaTestSupport.Env();
        reflect = (JsObject) e.global("Reflect");
    }

    /**
     * Calls a Reflect function.
     * <p>
     * Reflect 함수를 호출한다.
     *
     * @param name the function name / 함수 이름
     * @param a    the arguments / 인수
     * @return the result / 결과
     */
    private JsValue r(String name, JsValue... a) {
        return Ops.invoke(e.realm, reflect, name, a);
    }

    /**
     * Fails unless calling the Reflect function throws a TypeError.
     * <p>
     * Reflect 함수 호출이 TypeError 를 던지지 않으면 실패시킨다.
     *
     * @param name the function name / 함수 이름
     * @param a    the arguments / 인수
     */
    private void typeErrorFor(final String name, final JsValue... a) {
        MetaTestSupport.typeError(new Block() {
            public void run() {
                r(name, a);
            }
        }, "Reflect." + name + " with " + a.length + " arguments");
    }

    /**
     * 26.1: names, lengths and attributes of the Reflect functions; Reflect itself is neither callable nor a constructor.
     * <p>
     * 26.1: Reflect 함수의 이름, length, 속성. Reflect 자체는 호출할 수도 생성자일 수도 없다.
     */
    public void testShape() {
        String[] names = { "apply", "construct", "defineProperty", "deleteProperty", "get", "getOwnPropertyDescriptor", "getPrototypeOf",
            "has", "isExtensible", "ownKeys", "preventExtensions", "set", "setPrototypeOf" };
        int[] lengths = { 3, 2, 3, 2, 2, 2, 1, 2, 1, 1, 1, 3, 2 };
        // 모든 함수의 이름, length, 프로퍼티 속성을 확인한다.
        for (int i = 0; i < names.length; i++) {
            JsObject f = (JsObject) reflect.get(names[i]);
            Check.isTrue(f.isCallable(), names[i] + " is a function");
            Check.isFalse(f.isConstructor(), names[i] + " is not a constructor");
            Check.equal(names[i], f.getString("name"), names[i] + ".name");
            Check.equal(lengths[i], (long) ((JsNumber) f.get("length")).getValue(), names[i] + ".length");
            PropertyDescriptor d = reflect.getOwnProperty(names[i]);
            Check.isTrue(d.isWritableTrue() && d.isConfigurableTrue() && !d.isEnumerableTrue(), names[i] + " attributes");
        }
        Check.equal(names.length + 1, reflect.ownKeys().size(), "13 methods and the toStringTag");
        Check.isFalse(reflect.isCallable(), "Reflect is not callable");
        Check.isFalse(reflect.isConstructor(), "Reflect is not a constructor");
        Check.same(e.realm.getObjectPrototype(), reflect.getPrototypeOf(), "[[Prototype]] is Object.prototype");
    }

    /**
     * 26.1.1 Reflect.apply: the target must be callable, the argument list array-like.
     * <p>
     * 26.1.1 Reflect.apply: 대상은 호출 가능해야 하고 인수 목록은 배열 유사 객체여야 한다.
     */
    public void testApply() {
        NativeFunction sum = e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                double t = 0;
                // 모든 인수를 더하고 this 가 숫자이면 그것도 더한다.
                for (int i = 0; i < args.length; i++) {
                    t += Ops.toNumber(args[i]);
                }
                return Js.num(t + (thisValue instanceof JsNumber ? ((JsNumber) thisValue).getValue() : 0));
            }
        });
        JsValue result = r("apply", sum, MetaTestSupport.n(100), e.realm.newArray(MetaTestSupport.args(MetaTestSupport.n(1), MetaTestSupport.n(2))));
        Check.equal(103.0, ((JsNumber) result).getValue(), "this and arguments reach the function");
        JsObject arrayLike = e.obj("length", MetaTestSupport.n(2), "0", MetaTestSupport.n(5), "1", MetaTestSupport.n(6));
        Check.equal(11.0, ((JsNumber) r("apply", sum, Js.UNDEFINED, arrayLike)).getValue(), "array-like argument list");
        typeErrorFor("apply", e.obj(), Js.UNDEFINED, e.realm.newArray());
        typeErrorFor("apply", sum, Js.UNDEFINED, MetaTestSupport.n(1));
        typeErrorFor("apply", sum, Js.UNDEFINED);
        typeErrorFor("apply");
    }

    /**
     * 26.1.2 Reflect.construct: new.target defaults to the target; both must be constructors.
     * <p>
     * 26.1.2 Reflect.construct: new.target 의 기본값은 대상이며 둘 다 생성자여야 한다.
     */
    public void testConstruct() {
        final JsValue[] seenNewTarget = new JsValue[1];
        final NativeFunction ctor = new NativeFunction(e.realm, "C", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm rr, JsValue[] args, JsObject newTarget) {
                seenNewTarget[0] = newTarget;
                JsObject o = new JsObject(Ops.getPrototypeFromConstructor(rr, newTarget, rr.getObjectPrototype()));
                o.createDataProperty("argc", Js.num(args.length));
                return o;
            }
        });
        JsObject made = (JsObject) r("construct", ctor, e.realm.newArray(MetaTestSupport.args(MetaTestSupport.n(1), MetaTestSupport.n(2))));
        Check.equal(2.0, ((JsNumber) made.get("argc")).getValue(), "arguments");
        Check.same(ctor, seenNewTarget[0], "new.target defaults to the target");
        final JsObject protoForNew = e.obj("marker", Js.TRUE);
        NativeFunction other = new NativeFunction(e.realm, "Other", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm rr, JsValue[] args, JsObject newTarget) {
                return rr.newObject();
            }
        });
        other.defineData("prototype", protoForNew, false, false, false);
        JsObject viaNewTarget = (JsObject) r("construct", ctor, e.realm.newArray(), other);
        Check.same(other, seenNewTarget[0], "explicit new.target");
        Check.same(protoForNew, viaNewTarget.getPrototypeOf(), "the prototype comes from new.target");
        NativeFunction plain = e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        typeErrorFor("construct", plain, e.realm.newArray());
        typeErrorFor("construct", ctor, e.realm.newArray(), plain);
        typeErrorFor("construct", ctor, MetaTestSupport.n(1));
        typeErrorFor("construct", e.obj(), e.realm.newArray());
        typeErrorFor("construct");
    }

    /**
     * 26.1.3, 26.1.4, 26.1.10, 26.1.9: 프로퍼티 함수 시험: defineProperty, deleteProperty, has, getOwnPropertyDescriptor.
     * <p>
     * 26.1.3, 26.1.4, 26.1.10, 26.1.9: defineProperty, deleteProperty, has, getOwnPropertyDescriptor.
     */
    public void testPropertyFunctions() {
        JsObject t = e.obj();
        Check.equal(Js.TRUE, r("defineProperty", t, MetaTestSupport.s("a"),
                e.obj("value", MetaTestSupport.n(1), "writable", Js.FALSE, "enumerable", Js.TRUE, "configurable", Js.FALSE)), "defineProperty returns true");
        Check.equal(Js.FALSE, r("defineProperty", t, MetaTestSupport.s("a"), e.obj("value", MetaTestSupport.n(2))), "redefining a frozen property returns false, no exception");
        Check.equal(Js.FALSE, r("deleteProperty", t, MetaTestSupport.s("a")), "deleting a non-configurable property returns false");
        Check.equal(Js.TRUE, r("deleteProperty", t, MetaTestSupport.s("missing")), "deleting a missing property returns true");
        Check.equal(Js.TRUE, r("has", t, MetaTestSupport.s("a")), "has own");
        Check.equal(Js.FALSE, r("has", t, MetaTestSupport.s("nope")), "has missing");
        JsObject d = (JsObject) r("getOwnPropertyDescriptor", t, MetaTestSupport.s("a"));
        Check.equal("[value, writable, enumerable, configurable]", d.ownKeys().toString(), "descriptor object keys and order");
        Check.equal(Js.FALSE, d.get("writable"), "writable false");
        Check.isTrue(r("getOwnPropertyDescriptor", t, MetaTestSupport.s("missing")).isUndefined(), "missing property: undefined");
        JsSymbol sym = new JsSymbol("s");
        r("defineProperty", t, sym, e.obj("get", e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return MetaTestSupport.n(9);
            }
        }), "configurable", Js.TRUE));
        JsObject accessor = (JsObject) r("getOwnPropertyDescriptor", t, sym);
        Check.equal("[get, set, enumerable, configurable]", accessor.ownKeys().toString(), "accessor descriptor keys");
        Check.isTrue(accessor.get("set").isUndefined(), "missing setter is undefined");
        Check.equal(Js.TRUE, r("has", t, sym), "symbol keys work");
        typeErrorFor("defineProperty", MetaTestSupport.n(1), MetaTestSupport.s("a"), e.obj());
        typeErrorFor("defineProperty", t, MetaTestSupport.s("a"), MetaTestSupport.n(1));
        typeErrorFor("defineProperty", t, MetaTestSupport.s("a"), e.obj("get", MetaTestSupport.n(1)));
        typeErrorFor("defineProperty", t, MetaTestSupport.s("a"), e.obj("get", e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }), "value", MetaTestSupport.n(1)));
        typeErrorFor("deleteProperty", Js.NULL, MetaTestSupport.s("a"));
        typeErrorFor("has", MetaTestSupport.s("str"), MetaTestSupport.s("length"));
        typeErrorFor("getOwnPropertyDescriptor", Js.UNDEFINED, MetaTestSupport.s("a"));
    }

    /**
     * 26.1.5, 26.1.12: get and set with and without receiver; getters and setters see the receiver.
     * <p>
     * 26.1.5, 26.1.12: receiver 가 있을 때와 없을 때의 get, set. getter 와 setter 는 receiver 를 본다.
     */
    public void testGetAndSetWithReceiver() {
        final JsValue[] seenThis = new JsValue[2];
        JsObject t = e.obj("data", MetaTestSupport.n(1));
        t.defineAccessor("acc", e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                seenThis[0] = thisValue;
                return MetaTestSupport.n(7);
            }
        }), e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                seenThis[1] = thisValue;
                return Js.UNDEFINED;
            }
        }), true, true);
        Check.equal(1.0, ((JsNumber) r("get", t, MetaTestSupport.s("data"))).getValue(), "get data");
        Check.isTrue(r("get", t, MetaTestSupport.s("missing")).isUndefined(), "get missing");
        JsObject receiver = e.obj();
        Check.equal(7.0, ((JsNumber) r("get", t, MetaTestSupport.s("acc"))).getValue(), "get accessor");
        Check.same(t, seenThis[0], "getter this defaults to the target");
        r("get", t, MetaTestSupport.s("acc"), receiver);
        Check.same(receiver, seenThis[0], "getter this is the receiver");
        Check.equal(Js.TRUE, r("set", t, MetaTestSupport.s("acc"), MetaTestSupport.n(1), receiver), "set through a setter");
        Check.same(receiver, seenThis[1], "setter this is the receiver");
        // 데이터 프로퍼티의 set 은 receiver 에 프로퍼티를 만든다.
        Check.equal(Js.TRUE, r("set", t, MetaTestSupport.s("data"), MetaTestSupport.n(5), receiver), "set data with another receiver");
        Check.equal(5.0, ((JsNumber) receiver.get("data")).getValue(), "the property was created on the receiver");
        Check.equal(1.0, ((JsNumber) t.get("data")).getValue(), "the target is unchanged");
        Check.equal(Js.TRUE, r("set", t, MetaTestSupport.s("data"), MetaTestSupport.n(6)), "set without receiver");
        Check.equal(6.0, ((JsNumber) t.get("data")).getValue(), "set on the target");
        JsObject frozen = e.obj();
        frozen.defineData("ro", MetaTestSupport.n(1), false, true, true);
        Check.equal(Js.FALSE, r("set", frozen, MetaTestSupport.s("ro"), MetaTestSupport.n(2)), "set of a read-only property returns false, no exception");
        Check.equal(Js.FALSE, r("set", t, MetaTestSupport.s("data"), MetaTestSupport.n(2), MetaTestSupport.n(3)), "a non-object receiver makes the set fail");
        JsObject readOnlyAccessor = e.obj();
        readOnlyAccessor.defineAccessor("g", e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }), null, true, true);
        Check.equal(Js.FALSE, r("set", readOnlyAccessor, MetaTestSupport.s("g"), MetaTestSupport.n(1)), "accessor without setter");
        typeErrorFor("get", MetaTestSupport.n(1), MetaTestSupport.s("a"));
        typeErrorFor("set", Js.NULL, MetaTestSupport.s("a"), MetaTestSupport.n(1));
    }

    /**
     * 26.1.8, 26.1.14: getPrototypeOf and setPrototypeOf, including null and error cases.
     * <p>
     * 26.1.8, 26.1.14: getPrototypeOf 와 setPrototypeOf. null 과 오류 경우 포함.
     */
    public void testPrototypeFunctions() {
        JsObject proto = e.obj();
        JsObject t = new JsObject(proto);
        Check.same(proto, r("getPrototypeOf", t), "getPrototypeOf");
        Check.isTrue(r("getPrototypeOf", new JsObject(null)).isNull(), "null prototype gives null");
        Check.equal(Js.TRUE, r("setPrototypeOf", t, Js.NULL), "setPrototypeOf(null)");
        Check.isTrue(r("getPrototypeOf", t).isNull(), "now null");
        JsObject cycle = new JsObject(t);
        Check.equal(Js.FALSE, r("setPrototypeOf", t, cycle), "a cycle makes setPrototypeOf return false");
        t.preventExtensions();
        Check.equal(Js.FALSE, r("setPrototypeOf", t, proto), "non-extensible target");
        Check.equal(Js.TRUE, r("setPrototypeOf", t, Js.NULL), "same prototype on a non-extensible target is true");
        typeErrorFor("getPrototypeOf", MetaTestSupport.n(1));
        typeErrorFor("setPrototypeOf", MetaTestSupport.s("x"), Js.NULL);
        typeErrorFor("setPrototypeOf", t, MetaTestSupport.n(1));
        typeErrorFor("setPrototypeOf", t, Js.UNDEFINED);
        typeErrorFor("setPrototypeOf", t);
    }

    /**
     * 26.1.7, 26.1.11, 26.1.13: isExtensible, preventExtensions and ownKeys (indices first, then strings, then symbols).
     * <p>
     * 26.1.7, 26.1.11, 26.1.13: isExtensible, preventExtensions, ownKeys(인덱스, 문자열, 심볼 순).
     */
    public void testExtensibilityAndOwnKeys() {
        JsObject t = e.obj();
        JsSymbol sym = new JsSymbol("sy");
        t.createDataProperty(sym, Js.TRUE);
        t.createDataProperty("b", Js.TRUE);
        t.createDataProperty("2", Js.TRUE);
        t.createDataProperty("a", Js.TRUE);
        t.createDataProperty("1", Js.TRUE);
        JsArray keys = (JsArray) r("ownKeys", t);
        Check.equal(5L, keys.getLength(), "five keys");
        Check.equal("1", ((JsString) keys.get("0")).getValue(), "indices first, ascending (1)");
        Check.equal("2", ((JsString) keys.get("1")).getValue(), "indices first, ascending (2)");
        Check.equal("b", ((JsString) keys.get("2")).getValue(), "strings in creation order (b)");
        Check.equal("a", ((JsString) keys.get("3")).getValue(), "strings in creation order (a)");
        Check.same(sym, keys.get("4"), "symbols last");
        Check.equal(Js.TRUE, r("isExtensible", t), "isExtensible");
        Check.equal(Js.TRUE, r("preventExtensions", t), "preventExtensions");
        Check.equal(Js.FALSE, r("isExtensible", t), "not extensible any more");
        Check.equal(Js.TRUE, r("preventExtensions", t), "preventExtensions is idempotent");
        typeErrorFor("isExtensible", MetaTestSupport.n(1));
        typeErrorFor("preventExtensions", Js.NULL);
        typeErrorFor("ownKeys", MetaTestSupport.s("string"));
    }

    /**
     * Reflect functions work on proxies and report their traps' answers (this is how a proxy handler forwards to the default behavior).
     * <p>
     * Reflect 함수는 프록시에도 동작하며 trap 의 답을 보고한다(프록시 핸들러가 기본 동작으로 전달하는 방식).
     */
    public void testReflectOnProxy() {
        final JsObject target = e.obj("a", MetaTestSupport.n(1));
        JsObject handler = e.obj();
        // 모든 trap 을 Reflect 의 같은 이름 함수로 전달하는 핸들러를 만든다.
        String[][] pairs = { { "get", "get" }, { "set", "set" }, { "has", "has" }, { "deleteProperty", "deleteProperty" }, { "ownKeys", "ownKeys" } };
        for (int i = 0; i < pairs.length; i++) {
            final String name = pairs[i][1];
            handler.createDataProperty(pairs[i][0], e.fn(0, new NativeFunction.Call() {
                public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                    e.log.add(name);
                    return Ops.invoke(rr, reflect, name, args);
                }
            }));
        }
        JsObject p = Ops.construct(e.global("Proxy"), MetaTestSupport.args(target, handler));
        Check.equal(1.0, ((JsNumber) p.get("a")).getValue(), "get through the forwarding handler");
        Check.isTrue(p.set("b", MetaTestSupport.n(2)), "set through the forwarding handler");
        Check.equal(2.0, ((JsNumber) target.get("b")).getValue(), "the target received the value");
        Check.isTrue(p.hasProperty("a"), "has");
        Check.isTrue(p.delete("b"), "delete");
        Check.equal(1, p.ownKeys().size(), "ownKeys");
        Check.equal("get,set,has,deleteProperty,ownKeys", e.logText(), "traps were called in order");
        Check.equal(Js.TRUE, r("has", p, MetaTestSupport.s("a")), "Reflect.has on a proxy");
        List<Object> keys = p.ownKeys();
        Check.equal("a", keys.get(0), "first key");
    }
}
