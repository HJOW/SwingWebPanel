package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
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
 * Tests of the Proxy constructor and of every trap (ECMA-262 6th edition 26.2 and 9.5): the argument lists of the traps, the receiver,
 * forwarding without traps, callable and constructor proxies, revocation, and the enumerate hook. The invariant checks are tested in
 * {@link ProxyInvariantTest}.
 * <p>
 * Proxy 생성자와 모든 trap 의 시험(ECMA-262 6판 26.2, 9.5): trap 의 인수 목록, receiver, trap 이 없을 때의 전달, 호출 가능/생성자 프록시,
 * 해제, enumerate 확장 지점. 불변 조건 검사는 {@link ProxyInvariantTest} 에서 시험한다.
 */
public class ProxyTest {

    /**
     * The environment of the running case.
     * <p>
     * 실행 중인 사례의 환경.
     */
    private MetaTestSupport.Env e;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ProxyTest() {
    }

    /**
     * Creates a fresh realm for each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new MetaTestSupport.Env();
    }

    /**
     * Creates a proxy through the global constructor.
     * <p>
     * 전역 생성자로 프록시를 만든다.
     *
     * @param target  the target / 대상
     * @param handler the handler / 핸들러
     * @return the proxy / 프록시
     */
    private JsObject proxy(JsObject target, JsObject handler) {
        return Ops.construct(e.global("Proxy"), MetaTestSupport.args(target, handler));
    }

    /**
     * 26.2.1, 26.2.2: shape of the constructor: length 2, name "Proxy", no prototype property, TypeError without new or with non-object
     * arguments.
     * <p>
     * 26.2.1, 26.2.2: 생성자의 모양: length 2, 이름 "Proxy", prototype 프로퍼티 없음, new 없이 호출하거나 인수가 객체가 아니면 TypeError.
     */
    public void testConstructorShape() {
        final JsObject ctor = (JsObject) e.global("Proxy");
        Check.isTrue(ctor.isConstructor(), "Proxy is a constructor");
        Check.equal("Proxy", ctor.getString("name"), "name");
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) ctor.get("length")).getValue(), "length");
        Check.isFalse(ctor.hasOwnProperty("prototype"), "Proxy has no prototype property");
        PropertyDescriptor d = e.realm.getGlobalObject().getOwnProperty("Proxy");
        Check.isTrue(d.isWritableTrue() && d.isConfigurableTrue() && !d.isEnumerableTrue(), "global attributes: writable, configurable, not enumerable");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(ctor, Js.UNDEFINED, MetaTestSupport.args(e.obj(), e.obj()));
            }
        }, "Proxy without new");
        JsValue[][] bad = { { Js.num(1), e.obj() }, { e.obj(), Js.num(1) }, { Js.UNDEFINED, e.obj() }, { e.obj(), Js.NULL }, { e.obj() }, {} };
        // 대상이나 핸들러가 객체가 아니면 모두 TypeError 이다.
        for (int i = 0; i < bad.length; i++) {
            final JsValue[] a = bad[i];
            MetaTestSupport.typeError(new Block() {
                public void run() {
                    Ops.construct(ctor, a);
                }
            }, "Proxy with invalid arguments #" + i);
        }
        JsObject revocable = (JsObject) ctor.get("revocable");
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) revocable.get("length")).getValue(), "revocable.length");
    }

    /**
     * 9.5: with an empty handler every internal method forwards to the target.
     * <p>
     * 9.5: 핸들러가 비어 있으면 모든 내부 메서드가 대상에 전달된다.
     */
    public void testForwardingWithoutTraps() {
        JsObject target = e.obj("a", MetaTestSupport.n(1));
        JsObject p = proxy(target, e.obj());
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) p.get("a")).getValue(), "get");
        Check.isTrue(p.set("b", MetaTestSupport.n(2)), "set");
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) target.get("b")).getValue(), "set reached the target");
        Check.isTrue(p.hasProperty("a"), "has");
        Check.isFalse(p.hasProperty("zzz"), "has missing");
        Check.equal("[a, b]", p.ownKeys().toString(), "ownKeys");
        Check.isTrue(p.getOwnProperty("a").isConfigurableTrue(), "getOwnProperty");
        Check.isTrue(p.defineOwnProperty("c", PropertyDescriptor.data(MetaTestSupport.n(3), true, true, true)), "defineProperty");
        Check.isTrue(target.hasOwnProperty("c"), "defined on the target");
        Check.isTrue(p.delete("c"), "delete");
        Check.isFalse(target.hasOwnProperty("c"), "deleted on the target");
        Check.same(e.realm.getObjectPrototype(), p.getPrototypeOf(), "getPrototypeOf");
        JsObject other = e.obj();
        Check.isTrue(p.setPrototypeOf(other), "setPrototypeOf");
        Check.same(other, target.getPrototypeOf(), "prototype set on the target");
        Check.isTrue(p.isExtensible(), "isExtensible");
        Check.isTrue(p.preventExtensions(), "preventExtensions");
        Check.isFalse(target.isExtensible(), "the target is no longer extensible");
        Check.isFalse(p.isExtensible(), "proxy follows the target");
    }

    /**
     * 9.5.8: the get trap receives (target, key, receiver) with the handler as this value, symbols are passed as symbols and the
     * result is returned.
     * <p>
     * 9.5.8: get trap 은 핸들러를 this 로 하고 (target, key, receiver) 를 받으며, 심볼은 심볼로 전달되고 결과가 그대로 반환된다.
     */
    public void testGetTrapArguments() {
        final JsObject target = e.obj();
        final JsObject handler = e.obj();
        final JsValue[] seen = new JsValue[5];
        final JsObject[] proxyRef = new JsObject[1];
        handler.createDataProperty("get", e.fn(3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen[0] = thisValue;
                seen[1] = args[0];
                seen[2] = args[1];
                seen[3] = args[2];
                seen[4] = Js.num(args.length);
                return MetaTestSupport.s("trap:" + (args[1] instanceof JsSymbol ? "symbol" : Ops.toString(args[1])));
            }
        }));
        JsObject p = proxy(target, handler);
        proxyRef[0] = p;
        Check.equal("trap:x", ((org.duckdns.hjow.swingwebpanel.js.JsString) p.get("x")).getValue(), "result of the trap");
        Check.same(handler, seen[0], "this value is the handler");
        Check.same(target, seen[1], "first argument is the target");
        Check.equal("x", ((org.duckdns.hjow.swingwebpanel.js.JsString) seen[2]).getValue(), "second argument is the key");
        Check.same(p, seen[3], "third argument is the receiver (the proxy)");
        Check.equal(3.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) seen[4]).getValue(), "exactly three arguments");
        JsSymbol sym = new JsSymbol("tag");
        Check.equal("trap:symbol", ((org.duckdns.hjow.swingwebpanel.js.JsString) p.get(sym)).getValue(), "symbol keys are passed as symbols");
        Check.same(sym, seen[2], "the very same symbol");
    }

    /**
     * 9.5.8: when the proxy is on the prototype chain of another object, the receiver is that object.
     * <p>
     * 9.5.8: 프록시가 다른 객체의 프로토타입 체인에 있으면 receiver 는 그 객체이다.
     */
    public void testReceiverWhenProxyIsAPrototype() {
        final JsValue[] receiver = new JsValue[1];
        JsObject handler = e.obj();
        handler.createDataProperty("get", e.fn(3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                receiver[0] = args[2];
                return MetaTestSupport.n(42);
            }
        }));
        JsObject p = proxy(e.obj(), handler);
        JsObject child = new JsObject(p);
        Check.equal(42.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) child.get("anything")).getValue(), "inherited through the proxy");
        Check.same(child, receiver[0], "receiver is the child object");
    }

    /**
     * 9.5.9: the set trap receives (target, key, value, receiver); a falsish result makes the assignment fail.
     * <p>
     * 9.5.9: set trap 은 (target, key, value, receiver) 를 받으며 거짓 같은 결과는 대입을 실패시킨다.
     */
    public void testSetTrap() {
        final List<String> calls = new ArrayList<String>();
        final JsValue[] answer = new JsValue[] { Js.TRUE };
        JsObject target = e.obj();
        JsObject handler = e.obj();
        handler.createDataProperty("set", e.fn(4, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add(Ops.toString(args[1]) + "=" + Ops.toString(args[2]) + "/" + args.length);
                return answer[0];
            }
        }));
        final JsObject p = proxy(target, handler);
        Check.isTrue(p.set("k", MetaTestSupport.n(7)), "truish result");
        answer[0] = MetaTestSupport.n(0);
        Check.isFalse(p.set("k", MetaTestSupport.n(8)), "falsish result (0)");
        answer[0] = Js.UNDEFINED;
        Check.isFalse(p.set("k", MetaTestSupport.n(9)), "falsish result (undefined)");
        Check.equal("[k=7/4, k=8/4, k=9/4]", calls.toString(), "arguments of the calls");
        Check.isFalse(target.hasOwnProperty("k"), "the trap replaced the default behavior");
        // 엄격 모드 대입은 false 결과를 TypeError 로 바꾼다.
        MetaTestSupport.typeError(new Block() {
            public void run() {
                p.setStrict("k", Js.UNDEFINED);
            }
        }, "strict assignment of a falsish trap result");
    }

    /**
     * 9.5.7, 9.5.10: has and deleteProperty traps convert their results to booleans.
     * <p>
     * 9.5.7, 9.5.10: has 와 deleteProperty trap 은 결과를 불리언으로 바꾼다.
     */
    public void testHasAndDeleteTraps() {
        final List<String> calls = new ArrayList<String>();
        JsObject handler = e.obj();
        handler.createDataProperty("has", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add("has:" + Ops.toString(args[1]));
                return MetaTestSupport.s("yes");
            }
        }));
        handler.createDataProperty("deleteProperty", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add("delete:" + Ops.toString(args[1]));
                return MetaTestSupport.s("");
            }
        }));
        JsObject p = proxy(e.obj(), handler);
        Check.isTrue(p.hasProperty("q"), "non-empty string is truish");
        Check.isFalse(p.delete("q"), "empty string is falsish");
        Check.equal("[has:q, delete:q]", calls.toString(), "calls");
    }

    /**
     * 9.5.12: ownKeys returns the trap's list in its order, keeping strings and symbols.
     * <p>
     * 9.5.12: ownKeys 는 trap 의 목록을 그 순서대로, 문자열과 심볼을 유지해 반환한다.
     */
    public void testOwnKeysTrap() {
        final JsSymbol sym = new JsSymbol("s");
        JsObject handler = e.obj();
        handler.createDataProperty("ownKeys", e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return r.newArray(MetaTestSupport.args(MetaTestSupport.s("z"), sym, MetaTestSupport.s("a")));
            }
        }));
        JsObject p = proxy(e.obj(), handler);
        List<Object> keys = p.ownKeys();
        Check.equal(3, keys.size(), "three keys");
        Check.equal("z", keys.get(0), "first key");
        Check.same(sym, keys.get(1), "symbol key");
        Check.equal("a", keys.get(2), "third key");
    }

    /**
     * 9.5.5, 9.5.6: getOwnPropertyDescriptor results are completed; defineProperty receives a descriptor object with only the given
     * fields.
     * <p>
     * 9.5.5, 9.5.6: getOwnPropertyDescriptor 결과는 완성되고, defineProperty 는 주어진 필드만 가진 기술자 객체를 받는다.
     */
    public void testDescriptorTraps() {
        final JsObject[] received = new JsObject[1];
        JsObject handler = e.obj();
        handler.createDataProperty("getOwnPropertyDescriptor", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return e.obj("value", MetaTestSupport.n(5), "configurable", Js.TRUE);
            }
        }));
        handler.createDataProperty("defineProperty", e.fn(3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                received[0] = (JsObject) args[2];
                return Js.TRUE;
            }
        }));
        JsObject p = proxy(e.obj(), handler);
        PropertyDescriptor d = p.getOwnProperty("anything");
        Check.equal(5.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) d.value).getValue(), "value");
        Check.equal(Boolean.FALSE, d.writable, "writable defaulted to false");
        Check.equal(Boolean.FALSE, d.enumerable, "enumerable defaulted to false");
        Check.equal(Boolean.TRUE, d.configurable, "configurable kept");
        PropertyDescriptor in = new PropertyDescriptor();
        in.value = MetaTestSupport.n(1);
        in.enumerable = Boolean.TRUE;
        Check.isTrue(p.defineOwnProperty("k", in), "defineProperty");
        Check.equal("[value, enumerable]", received[0].ownKeys().toString(), "only the present fields are in the descriptor object");
    }

    /**
     * 9.5.1 - 9.5.4: 프로토타입과 확장 가능 여부 trap 시험: getPrototypeOf, setPrototypeOf, isExtensible, preventExtensions.
     * <p>
     * 9.5.1 - 9.5.4: getPrototypeOf, setPrototypeOf, isExtensible, preventExtensions trap.
     */
    public void testPrototypeAndExtensibilityTraps() {
        final JsObject fakeProto = e.obj();
        final List<String> calls = new ArrayList<String>();
        JsObject target = e.obj();
        JsObject handler = e.obj();
        handler.createDataProperty("getPrototypeOf", e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add("getPrototypeOf");
                return fakeProto;
            }
        }));
        handler.createDataProperty("setPrototypeOf", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add("setPrototypeOf:" + (args[1].isNull() ? "null" : "object"));
                return Js.TRUE;
            }
        }));
        handler.createDataProperty("isExtensible", e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add("isExtensible");
                return Js.TRUE;
            }
        }));
        handler.createDataProperty("preventExtensions", e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls.add("preventExtensions");
                return Js.FALSE;
            }
        }));
        JsObject p = proxy(target, handler);
        Check.same(fakeProto, p.getPrototypeOf(), "getPrototypeOf result");
        Check.isTrue(p.setPrototypeOf(null), "setPrototypeOf(null)");
        Check.isTrue(p.setPrototypeOf(e.obj()), "setPrototypeOf(object)");
        Check.isTrue(p.isExtensible(), "isExtensible");
        Check.isFalse(p.preventExtensions(), "preventExtensions returns the trap result false");
        Check.equal("[getPrototypeOf, setPrototypeOf:null, setPrototypeOf:object, isExtensible, preventExtensions]", calls.toString(), "calls");
        Check.isTrue(target.isExtensible(), "the target was not touched");
    }

    /**
     * 9.5.13, 9.5.14: proxies of functions are callable (typeof function); the apply trap gets (target, this, argsArray) and the construct
     * trap gets (target, argsArray, newTarget) and must return an object.
     * <p>
     * 9.5.13, 9.5.14: 함수의 프록시는 호출 가능하다(typeof function). apply trap 은 (target, this, 인수 배열), construct trap 은
     * (target, 인수 배열, newTarget) 을 받고 객체를 돌려주어야 한다.
     */
    public void testApplyAndConstructTraps() {
        final NativeFunction target = new NativeFunction(e.realm, "target", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return MetaTestSupport.s("target-called");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        });
        Check.isTrue(proxy(target, e.obj()).isCallable(), "forwarding proxy is callable");
        Check.equal("function", proxy(target, e.obj()).typeOf(), "typeof");
        Check.equal("target-called", ((org.duckdns.hjow.swingwebpanel.js.JsString) Ops.call(proxy(target, e.obj()), Js.UNDEFINED, Js.NO_ARGS)).getValue(), "no trap: forwarded");

        final JsValue[] seen = new JsValue[4];
        JsObject handler = e.obj();
        handler.createDataProperty("apply", e.fn(3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen[0] = args[0];
                seen[1] = args[1];
                seen[2] = args[2];
                return MetaTestSupport.s("apply-trap");
            }
        }));
        final JsObject made = e.obj("made", Js.TRUE);
        handler.createDataProperty("construct", e.fn(3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen[3] = args[2];
                return seen[3] == null ? Js.UNDEFINED : made;
            }
        }));
        JsObject p = proxy(target, handler);
        JsValue thisArg = e.obj();
        Check.equal("apply-trap", ((org.duckdns.hjow.swingwebpanel.js.JsString) Ops.call(p, thisArg, MetaTestSupport.args(MetaTestSupport.n(1), MetaTestSupport.n(2)))).getValue(), "apply result");
        Check.same(target, seen[0], "apply target");
        Check.same(thisArg, seen[1], "apply this");
        Check.isTrue(seen[2] instanceof JsArray && ((JsArray) seen[2]).getLength() == 2, "arguments array of length 2");
        Check.same(made, Ops.construct(p, Js.NO_ARGS), "construct trap result");
        Check.same(p, seen[3], "new.target is the proxy");
        Check.isTrue(p.isConstructor(), "proxy of a constructor is a constructor");
        JsObject notCallable = proxy(e.obj(), e.obj());
        Check.isFalse(notCallable.isCallable(), "proxy of a plain object is not callable");
        Check.isFalse(notCallable.isConstructor(), "proxy of a plain object is not a constructor");
        Check.equal("object", notCallable.typeOf(), "typeof");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(proxy(e.obj(), e.obj()), Js.UNDEFINED, Js.NO_ARGS);
            }
        }, "calling a non-callable proxy");
        JsObject arrowLike = new NativeFunction(e.realm, "fn", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        Check.isFalse(proxy(arrowLike, e.obj()).isConstructor(), "proxy of a non-constructor function is not a constructor");
    }

    /**
     * 7.2.2 and 19.1.3.6: IsArray follows the target, the class name is Array for array proxies and Function for callable ones.
     * <p>
     * 7.2.2, 19.1.3.6: IsArray 는 대상을 따르고, 배열 프록시의 클래스 이름은 Array, 호출 가능한 프록시는 Function 이다.
     */
    public void testIsArrayAndClassName() {
        JsObject arrayProxy = proxy(e.realm.newArray(), e.obj());
        Check.isTrue(arrayProxy.isArray(), "proxy of an array is an array");
        Check.equal("Array", arrayProxy.className(), "class name");
        Check.isTrue(Ops.isArray(arrayProxy), "Ops.isArray");
        JsObject nested = proxy(arrayProxy, e.obj());
        Check.isTrue(nested.isArray(), "proxy of a proxy of an array");
        Check.isFalse(proxy(e.obj(), e.obj()).isArray(), "proxy of an object is not an array");
        Check.equal("Object", proxy(e.obj(), e.obj()).className(), "object class name");
    }

    /**
     * 26.2.2.1: Proxy.revocable returns {proxy, revoke}; after revoke every operation is a TypeError, repeated revoke is harmless.
     * <p>
     * 26.2.2.1: Proxy.revocable 은 {proxy, revoke} 를 돌려주며 revoke 뒤에는 모든 연산이 TypeError 이고 되풀이 revoke 는 무해하다.
     */
    public void testRevocableProxy() {
        JsObject result = (JsObject) e.invoke(e.global("Proxy"), "revocable", e.obj("a", MetaTestSupport.n(1)), e.obj());
        final ProxyObject p = (ProxyObject) result.get("proxy");
        JsObject revoke = (JsObject) result.get("revoke");
        Check.equal("[proxy, revoke]", result.ownKeys().toString(), "result shape");
        Check.equal("", revoke.getString("name"), "revoke function name is empty");
        Check.equal(0.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) revoke.get("length")).getValue(), "revoke function length");
        Check.isFalse(revoke.isConstructor(), "revoke is not a constructor");
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) p.get("a")).getValue(), "works before revocation");
        Ops.call(revoke, Js.UNDEFINED, Js.NO_ARGS);
        Ops.call(revoke, Js.UNDEFINED, Js.NO_ARGS);
        Check.isTrue(p.isRevoked(), "revoked");
        Check.isNull(p.getProxyTarget(), "target dropped");
        Block[] blocks = { new Block() {
            public void run() {
                p.get("a");
            }
        }, new Block() {
            public void run() {
                p.set("a", Js.UNDEFINED);
            }
        }, new Block() {
            public void run() {
                p.hasProperty("a");
            }
        }, new Block() {
            public void run() {
                p.delete("a");
            }
        }, new Block() {
            public void run() {
                p.ownKeys();
            }
        }, new Block() {
            public void run() {
                p.getOwnProperty("a");
            }
        }, new Block() {
            public void run() {
                p.defineOwnProperty("a", PropertyDescriptor.valueOnly(Js.UNDEFINED));
            }
        }, new Block() {
            public void run() {
                p.getPrototypeOf();
            }
        }, new Block() {
            public void run() {
                p.setPrototypeOf(null);
            }
        }, new Block() {
            public void run() {
                p.isExtensible();
            }
        }, new Block() {
            public void run() {
                p.preventExtensions();
            }
        }, new Block() {
            public void run() {
                p.isArray();
            }
        }, new Block() {
            public void run() {
                new EnumerateSupport.ForInIterator(e.realm, p).nextKey();
            }
        } };
        // 해제된 프록시에서는 모든 내부 메서드가 TypeError 이다.
        for (int i = 0; i < blocks.length; i++) {
            MetaTestSupport.typeError(blocks[i], "revoked proxy operation #" + i);
        }
        Check.equal("[object Object]", p.toString(), "toString never throws");
        JsObject fnResult = (JsObject) e.invoke(e.global("Proxy"), "revocable", new NativeFunction(e.realm, "f", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        }), e.obj());
        final JsObject fp = (JsObject) fnResult.get("proxy");
        Ops.call(fnResult.get("revoke"), Js.UNDEFINED, Js.NO_ARGS);
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(fp, Js.UNDEFINED, Js.NO_ARGS);
            }
        }, "calling a revoked function proxy");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.construct(fp, Js.NO_ARGS);
            }
        }, "constructing a revoked function proxy");
        Check.isTrue(fp.isCallable(), "a revoked function proxy keeps its callability");
    }

    /**
     * 7.3.9 GetMethod: a trap that is neither undefined/null nor callable is a TypeError; undefined and null mean "no trap".
     * <p>
     * 7.3.9 GetMethod: undefined/null 도 호출 가능 값도 아닌 trap 은 TypeError 이고 undefined 와 null 은 "trap 없음"을 뜻한다.
     */
    public void testNonCallableTrap() {
        JsObject target = e.obj("a", MetaTestSupport.n(1));
        JsObject handler = e.obj("get", Js.NULL);
        final JsObject p = proxy(target, handler);
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) p.get("a")).getValue(), "null trap forwards");
        handler.createDataProperty("has", Js.UNDEFINED);
        Check.isTrue(p.hasProperty("a"), "undefined trap forwards");
        handler.createDataProperty("get", MetaTestSupport.n(5));
        MetaTestSupport.typeError(new Block() {
            public void run() {
                p.get("a");
            }
        }, "number as trap");
        handler.createDataProperty("get", e.obj());
        MetaTestSupport.typeError(new Block() {
            public void run() {
                p.get("a");
            }
        }, "object as trap");
    }

    /**
     * An exception thrown by a trap propagates unchanged.
     * <p>
     * trap 이 던진 예외는 그대로 전파된다.
     */
    public void testExceptionInTrapPropagates() {
        final JsValue thrown = e.obj("custom", Js.TRUE);
        JsObject handler = e.obj();
        handler.createDataProperty("get", e.fn(3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw new JsException(thrown);
            }
        }));
        final JsObject p = proxy(e.obj(), handler);
        JsException ex = (JsException) Check.throwsException(JsException.class, new Block() {
            public void run() {
                p.get("x");
            }
        }, "trap exception");
        Check.same(thrown, ex.getValue(e.realm), "the same value is thrown");
    }

    /**
     * For-in walks the proxy's current own keys and prototype chain without invoking an obsolete enumerate trap.
     * <p>
     * for-in은 폐기된 enumerate 트랩을 호출하지 않고 프록시의 현재 자체 키와 프로토타입 체인을 순회한다.
     */
    public void testForInEnumeration() {
        JsObject proto = e.obj("inherited", MetaTestSupport.n(1), "shadowed", MetaTestSupport.n(1));
        JsObject target = new JsObject(proto);
        target.createDataProperty("own", MetaTestSupport.n(1));
        target.defineData("hidden", MetaTestSupport.n(1), true, false, true);
        target.defineData("shadowed", MetaTestSupport.n(2), true, false, true);
        JsObject handler = e.obj();
        handler.createDataProperty("enumerate", e.fn(1, new NativeFunction.Call() {
            /** Fails if legacy enumeration is consulted. / 예전 열거 트랩을 호출하면 실패한다. */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("obsolete enumerate trap");
            }
        }));
        e.realm.getGlobalObject().defineBuiltin("enumerationTarget", proxy(target, handler));
        JsValue result = e.realm.evaluateScript(
                "var keys='';for(var key in enumerationTarget)keys+=(keys?',':'')+key;keys;", "proxy-for-in.js");
        Check.equal("own,inherited", Ops.toString(result), "own and inherited keys follow the ordinary for-in rules");
    }
}
