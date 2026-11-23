package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

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
 * Tests of the invariants that the Proxy internal methods enforce on trap results (ECMA-262 6th edition 9.5.1 - 9.5.12): for every trap
 * both the violation (a TypeError) and the nearest legal answer (no error) are checked, using targets with non-configurable and
 * non-writable properties, non-extensible targets and key lists.
 * <p>
 * Proxy 내부 메서드가 trap 결과에 강제하는 불변 조건의 시험(ECMA-262 6판 9.5.1 - 9.5.12): trap 마다 위반(TypeError)과 가장 가까운
 * 적법한 답(오류 없음)을 모두 확인한다. 설정 불가/쓰기 불가 프로퍼티가 있는 대상, 확장 불가 대상, 키 목록을 사용한다.
 */
public class ProxyInvariantTest {

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
    public ProxyInvariantTest() {
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
     * Creates a proxy whose only trap always answers the given value.
     * <p>
     * 하나뿐인 trap 이 항상 주어진 값으로 답하는 프록시를 만든다.
     *
     * @param target the target / 대상
     * @param trap   the trap name / trap 이름
     * @param result the answer / 답
     * @return the proxy / 프록시
     */
    private ProxyObject p(JsObject target, String trap, final JsValue result) {
        JsObject handler = e.obj();
        handler.createDataProperty(trap, e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return result;
            }
        }));
        return (ProxyObject) Ops.construct(e.global("Proxy"), MetaTestSupport.args(target, handler));
    }

    /**
     * Creates a target with a frozen data property "fixed" (1), a non-configurable accessor "acc" without getter and setter, a
     * non-configurable but writable property "locked" and a plain property "plain".
     * <p>
     * 얼려진 데이터 프로퍼티 "fixed"(1), getter/setter 가 없는 설정 불가 접근자 "acc", 설정 불가이지만 writable 인 "locked", 평범한 "plain" 을
     * 가진 대상을 만든다.
     *
     * @return the target / 대상
     */
    private JsObject target() {
        JsObject t = e.obj("plain", MetaTestSupport.n(0));
        t.defineData("fixed", MetaTestSupport.n(1), false, true, false);
        t.defineAccessor("acc", null, null, true, false);
        t.defineData("locked", MetaTestSupport.n(1), true, true, false);
        return t;
    }

    /**
     * Creates a non-extensible target with the properties "x" (configurable) and "y" (non-configurable).
     * <p>
     * 확장 불가이며 프로퍼티 "x"(설정 가능)와 "y"(설정 불가)를 가진 대상을 만든다.
     *
     * @return the target / 대상
     */
    private JsObject sealedTarget() {
        JsObject t = e.obj();
        t.defineData("x", MetaTestSupport.n(1), true, true, true);
        t.defineData("y", MetaTestSupport.n(1), true, true, false);
        t.preventExtensions();
        return t;
    }

    /**
     * Fails the case if the block does not throw a TypeError.
     * <p>
     * 코드가 TypeError 를 던지지 않으면 사례를 실패시킨다.
     *
     * @param what  description / 설명
     * @param block the code / 코드
     */
    private void bad(String what, Block block) {
        MetaTestSupport.typeError(block, what);
    }

    /**
     * 9.5.8 [[Get]]: a non-configurable non-writable data property must be reported with its real value; a non-configurable accessor
     * without getter must give undefined.
     * <p>
     * 9.5.8 [[Get]]: 설정 불가이고 쓰기 불가인 데이터 프로퍼티는 실제 값으로 보고해야 하며 getter 없는 설정 불가 접근자는 undefined 여야 한다.
     */
    public void testGetInvariants() {
        final ProxyObject lie = p(target(), "get", MetaTestSupport.n(2));
        bad("get: different value for a frozen property", new Block() {
            public void run() {
                lie.get("fixed");
            }
        });
        bad("get: value for an accessor without getter", new Block() {
            public void run() {
                lie.get("acc");
            }
        });
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) lie.get("plain")).getValue(), "get: any value for a plain property");
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) lie.get("locked")).getValue(), "get: any value for a writable non-configurable property");
        Check.equal(2.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) lie.get("missing")).getValue(), "get: any value for a missing property");
        ProxyObject truth = p(target(), "get", MetaTestSupport.n(1));
        Check.equal(1.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) truth.get("fixed")).getValue(), "get: the real value is fine");
        ProxyObject undef = p(target(), "get", Js.UNDEFINED);
        Check.isTrue(undef.get("acc").isUndefined(), "get: undefined for an accessor without getter is fine");
    }

    /**
     * 9.5.9 [[Set]]: a successful set must not contradict a frozen data property (other value) or an accessor without setter; a failed
     * set (false) is always legal.
     * <p>
     * 9.5.9 [[Set]]: 성공한 set 은 얼려진 데이터 프로퍼티(다른 값)나 setter 없는 접근자와 모순되면 안 되며 실패한 set(false)은 항상 적법하다.
     */
    public void testSetInvariants() {
        final ProxyObject yes = p(target(), "set", Js.TRUE);
        bad("set: true for a frozen property with a different value", new Block() {
            public void run() {
                yes.set("fixed", MetaTestSupport.n(2), yes);
            }
        });
        bad("set: true for an accessor without setter", new Block() {
            public void run() {
                yes.set("acc", MetaTestSupport.n(2), yes);
            }
        });
        Check.isTrue(yes.set("fixed", MetaTestSupport.n(1), yes), "set: true with the same value is fine");
        Check.isTrue(yes.set("locked", MetaTestSupport.n(9), yes), "set: true for a writable property is fine");
        Check.isTrue(yes.set("plain", MetaTestSupport.n(9), yes), "set: true for a plain property is fine");
        ProxyObject no = p(target(), "set", Js.FALSE);
        Check.isFalse(no.set("fixed", MetaTestSupport.n(2), no), "set: false is always legal");
    }

    /**
     * 9.5.7 [[HasProperty]]: false is not allowed for non-configurable properties nor for any property of a non-extensible target.
     * <p>
     * 9.5.7 [[HasProperty]]: 설정 불가 프로퍼티나 확장 불가 대상의 어떤 프로퍼티에 대해서도 false 는 허용되지 않는다.
     */
    public void testHasInvariants() {
        final ProxyObject no = p(target(), "has", Js.FALSE);
        bad("has: false for a non-configurable property", new Block() {
            public void run() {
                no.hasProperty("fixed");
            }
        });
        Check.isFalse(no.hasProperty("plain"), "has: false for a configurable property of an extensible target is fine");
        Check.isFalse(no.hasProperty("missing"), "has: false for a missing property is fine");
        final ProxyObject noSealed = p(sealedTarget(), "has", Js.FALSE);
        bad("has: false for a configurable property of a non-extensible target", new Block() {
            public void run() {
                noSealed.hasProperty("x");
            }
        });
        Check.isFalse(noSealed.hasProperty("missing"), "has: false for a missing property of a non-extensible target is fine");
        ProxyObject yes = p(target(), "has", Js.TRUE);
        Check.isTrue(yes.hasProperty("missing"), "has: true is always legal");
    }

    /**
     * 9.5.10 [[Delete]]: true is not allowed for non-configurable properties.
     * <p>
     * 9.5.10 [[Delete]]: 설정 불가 프로퍼티에 대해서는 true 가 허용되지 않는다.
     */
    public void testDeleteInvariants() {
        final ProxyObject yes = p(target(), "deleteProperty", Js.TRUE);
        bad("deleteProperty: true for a non-configurable property", new Block() {
            public void run() {
                yes.delete("fixed");
            }
        });
        Check.isTrue(yes.delete("plain"), "deleteProperty: true for a configurable property is fine");
        Check.isTrue(yes.delete("missing"), "deleteProperty: true for a missing property is fine");
        ProxyObject no = p(target(), "deleteProperty", Js.FALSE);
        Check.isFalse(no.delete("fixed"), "deleteProperty: false is always legal");
    }

    /**
     * 9.5.1 [[GetPrototypeOf]]: the result must be an object or null, and for a non-extensible target it must be the real prototype.
     * <p>
     * 9.5.1 [[GetPrototypeOf]]: 결과는 객체 또는 null 이어야 하고 확장 불가 대상에서는 실제 프로토타입이어야 한다.
     */
    public void testGetPrototypeOfInvariants() {
        final ProxyObject number = p(e.obj(), "getPrototypeOf", MetaTestSupport.n(1));
        bad("getPrototypeOf: number result", new Block() {
            public void run() {
                number.getPrototypeOf();
            }
        });
        final ProxyObject undef = p(e.obj(), "getPrototypeOf", Js.UNDEFINED);
        bad("getPrototypeOf: undefined result", new Block() {
            public void run() {
                undef.getPrototypeOf();
            }
        });
        Check.isNull(p(e.obj(), "getPrototypeOf", Js.NULL).getPrototypeOf(), "getPrototypeOf: null is fine for an extensible target");
        JsObject sealed = e.obj();
        sealed.preventExtensions();
        final ProxyObject other = p(sealed, "getPrototypeOf", e.obj());
        bad("getPrototypeOf: another prototype for a non-extensible target", new Block() {
            public void run() {
                other.getPrototypeOf();
            }
        });
        Check.same(e.realm.getObjectPrototype(), p(sealed, "getPrototypeOf", e.realm.getObjectPrototype()).getPrototypeOf(),
                "getPrototypeOf: the real prototype of a non-extensible target is fine");
    }

    /**
     * 9.5.2 [[SetPrototypeOf]]: true for a non-extensible target requires that the target already has that prototype.
     * <p>
     * 9.5.2 [[SetPrototypeOf]]: 확장 불가 대상에서 true 는 대상이 이미 그 프로토타입을 가질 것을 요구한다.
     */
    public void testSetPrototypeOfInvariants() {
        JsObject sealed = e.obj();
        sealed.preventExtensions();
        final ProxyObject yes = p(sealed, "setPrototypeOf", Js.TRUE);
        bad("setPrototypeOf: true with a different prototype on a non-extensible target", new Block() {
            public void run() {
                yes.setPrototypeOf(e.obj());
            }
        });
        Check.isTrue(yes.setPrototypeOf(e.realm.getObjectPrototype()), "setPrototypeOf: true with the existing prototype is fine");
        ProxyObject no = p(sealed, "setPrototypeOf", Js.FALSE);
        Check.isFalse(no.setPrototypeOf(e.obj()), "setPrototypeOf: false is always legal");
        Check.isTrue(p(e.obj(), "setPrototypeOf", Js.TRUE).setPrototypeOf(e.obj()), "setPrototypeOf: true for an extensible target is fine");
    }

    /**
     * 9.5.3 [[IsExtensible]]: the answer must equal the target's extensibility.
     * <p>
     * 9.5.3 [[IsExtensible]]: 답은 대상의 확장 가능 여부와 같아야 한다.
     */
    public void testIsExtensibleInvariants() {
        final ProxyObject no = p(e.obj(), "isExtensible", Js.FALSE);
        bad("isExtensible: false for an extensible target", new Block() {
            public void run() {
                no.isExtensible();
            }
        });
        JsObject sealed = e.obj();
        sealed.preventExtensions();
        final ProxyObject yes = p(sealed, "isExtensible", Js.TRUE);
        bad("isExtensible: true for a non-extensible target", new Block() {
            public void run() {
                yes.isExtensible();
            }
        });
        Check.isTrue(p(e.obj(), "isExtensible", Js.TRUE).isExtensible(), "isExtensible: true for an extensible target");
        Check.isFalse(p(sealed, "isExtensible", Js.FALSE).isExtensible(), "isExtensible: false for a non-extensible target");
    }

    /**
     * 9.5.4 [[PreventExtensions]]: true requires that the target is no longer extensible.
     * <p>
     * 9.5.4 [[PreventExtensions]]: true 는 대상이 더 이상 확장 가능하지 않을 것을 요구한다.
     */
    public void testPreventExtensionsInvariants() {
        final ProxyObject yes = p(e.obj(), "preventExtensions", Js.TRUE);
        bad("preventExtensions: true while the target is extensible", new Block() {
            public void run() {
                yes.preventExtensions();
            }
        });
        Check.isFalse(p(e.obj(), "preventExtensions", Js.FALSE).preventExtensions(), "preventExtensions: false is always legal");
        JsObject sealed = e.obj();
        sealed.preventExtensions();
        Check.isTrue(p(sealed, "preventExtensions", Js.TRUE).preventExtensions(), "preventExtensions: true for an already sealed target");
    }

    /**
     * 9.5.5 [[GetOwnProperty]]: undefined, descriptors and their compatibility with the target.
     * <p>
     * 9.5.5 [[GetOwnProperty]]: undefined, 기술자, 대상과의 호환성.
     */
    public void testGetOwnPropertyDescriptorInvariants() {
        final ProxyObject number = p(target(), "getOwnPropertyDescriptor", MetaTestSupport.n(1));
        bad("getOwnPropertyDescriptor: number result", new Block() {
            public void run() {
                number.getOwnProperty("plain");
            }
        });
        final ProxyObject undef = p(target(), "getOwnPropertyDescriptor", Js.UNDEFINED);
        bad("getOwnPropertyDescriptor: undefined for a non-configurable property", new Block() {
            public void run() {
                undef.getOwnProperty("fixed");
            }
        });
        Check.isNull(undef.getOwnProperty("plain"), "getOwnPropertyDescriptor: undefined for a configurable property is fine");
        Check.isNull(undef.getOwnProperty("missing"), "getOwnPropertyDescriptor: undefined for a missing property is fine");
        final ProxyObject undefSealed = p(sealedTarget(), "getOwnPropertyDescriptor", Js.UNDEFINED);
        bad("getOwnPropertyDescriptor: undefined for an existing property of a non-extensible target", new Block() {
            public void run() {
                undefSealed.getOwnProperty("x");
            }
        });
        Check.isNull(undefSealed.getOwnProperty("missing"), "getOwnPropertyDescriptor: undefined for a missing property of a sealed target");

        JsObject nonConfigurable = e.obj("value", MetaTestSupport.n(1), "configurable", Js.FALSE);
        final ProxyObject claimsLocked = p(target(), "getOwnPropertyDescriptor", nonConfigurable);
        bad("getOwnPropertyDescriptor: non-configurable for a missing property", new Block() {
            public void run() {
                claimsLocked.getOwnProperty("missing");
            }
        });
        bad("getOwnPropertyDescriptor: non-configurable for a configurable property", new Block() {
            public void run() {
                claimsLocked.getOwnProperty("plain");
            }
        });
        final ProxyObject differentValue = p(target(), "getOwnPropertyDescriptor",
                e.obj("value", MetaTestSupport.n(2), "writable", Js.FALSE, "enumerable", Js.TRUE, "configurable", Js.FALSE));
        bad("getOwnPropertyDescriptor: incompatible value for a frozen property", new Block() {
            public void run() {
                differentValue.getOwnProperty("fixed");
            }
        });
        ProxyObject same = p(target(), "getOwnPropertyDescriptor",
                e.obj("value", MetaTestSupport.n(1), "writable", Js.FALSE, "enumerable", Js.TRUE, "configurable", Js.FALSE));
        PropertyDescriptor ok = same.getOwnProperty("fixed");
        Check.equal(Boolean.FALSE, ok.configurable, "getOwnPropertyDescriptor: a compatible descriptor is returned");
        final ProxyObject addToSealed = p(sealedTarget(), "getOwnPropertyDescriptor", e.obj("value", MetaTestSupport.n(1), "configurable", Js.TRUE));
        bad("getOwnPropertyDescriptor: new property on a non-extensible target", new Block() {
            public void run() {
                addToSealed.getOwnProperty("fresh");
            }
        });
        ProxyObject invented = p(target(), "getOwnPropertyDescriptor", e.obj("value", MetaTestSupport.n(7), "configurable", Js.TRUE));
        Check.equal(7.0, ((org.duckdns.hjow.swingwebpanel.js.JsNumber) invented.getOwnProperty("anything").value).getValue(), "getOwnPropertyDescriptor: invented configurable property");
        final ProxyObject mixed = p(target(), "getOwnPropertyDescriptor", e.obj("value", MetaTestSupport.n(1), "get", Js.UNDEFINED, "configurable", Js.TRUE));
        bad("getOwnPropertyDescriptor: value together with get", new Block() {
            public void run() {
                mixed.getOwnProperty("plain");
            }
        });
        final ProxyObject badGetter = p(target(), "getOwnPropertyDescriptor", e.obj("get", MetaTestSupport.n(1), "configurable", Js.TRUE));
        bad("getOwnPropertyDescriptor: getter that is not callable", new Block() {
            public void run() {
                badGetter.getOwnProperty("plain");
            }
        });
    }

    /**
     * 9.5.6 [[DefineOwnProperty]]: true must be consistent with the target's property and extensibility; false is always legal.
     * <p>
     * 9.5.6 [[DefineOwnProperty]]: true 는 대상의 프로퍼티와 확장 가능 여부와 일관되어야 하며 false 는 항상 적법하다.
     */
    public void testDefinePropertyInvariants() {
        final ProxyObject yes = p(target(), "defineProperty", Js.TRUE);
        PropertyDescriptor plainDesc = PropertyDescriptor.data(MetaTestSupport.n(1), true, true, true);
        Check.isTrue(yes.defineOwnProperty("plain", plainDesc), "defineProperty: compatible definition is fine");
        final PropertyDescriptor lockDesc = PropertyDescriptor.data(MetaTestSupport.n(1), true, true, false);
        bad("defineProperty: non-configurable definition of a missing property", new Block() {
            public void run() {
                yes.defineOwnProperty("missing", lockDesc);
            }
        });
        bad("defineProperty: non-configurable definition of a configurable property", new Block() {
            public void run() {
                yes.defineOwnProperty("plain", lockDesc);
            }
        });
        final PropertyDescriptor changeFixed = PropertyDescriptor.valueOnly(MetaTestSupport.n(5));
        bad("defineProperty: incompatible change of a frozen property", new Block() {
            public void run() {
                yes.defineOwnProperty("fixed", changeFixed);
            }
        });
        final ProxyObject yesSealed = p(sealedTarget(), "defineProperty", Js.TRUE);
        bad("defineProperty: adding a property to a non-extensible target", new Block() {
            public void run() {
                yesSealed.defineOwnProperty("fresh", PropertyDescriptor.data(MetaTestSupport.n(1), true, true, true));
            }
        });
        Check.isTrue(yesSealed.defineOwnProperty("x", PropertyDescriptor.valueOnly(MetaTestSupport.n(1))), "defineProperty: existing property of a sealed target");
        ProxyObject no = p(target(), "defineProperty", Js.FALSE);
        Check.isFalse(no.defineOwnProperty("fixed", changeFixed), "defineProperty: false is always legal");
        Check.isTrue(yes.defineOwnProperty("fixed", PropertyDescriptor.valueOnly(MetaTestSupport.n(1))), "defineProperty: same value for the frozen property is fine");
    }

    /**
     * 9.5.12 [[OwnPropertyKeys]]: element types, duplicates, non-configurable keys and non-extensible targets.
     * <p>
     * 9.5.12 [[OwnPropertyKeys]]: 요소 형식, 중복, 설정 불가 키, 확장 불가 대상.
     */
    public void testOwnKeysInvariants() {
        final ProxyObject notObject = p(target(), "ownKeys", MetaTestSupport.n(1));
        bad("ownKeys: non-object result", new Block() {
            public void run() {
                notObject.ownKeys();
            }
        });
        final ProxyObject numberElement = p(target(), "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.n(1))));
        bad("ownKeys: number element", new Block() {
            public void run() {
                numberElement.ownKeys();
            }
        });
        final ProxyObject objectElement = p(target(), "ownKeys", e.realm.newArray(MetaTestSupport.args(e.obj())));
        bad("ownKeys: object element", new Block() {
            public void run() {
                objectElement.ownKeys();
            }
        });
        final ProxyObject duplicates = p(e.obj(), "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("a"), MetaTestSupport.s("a"))));
        bad("ownKeys: duplicate keys", new Block() {
            public void run() {
                duplicates.ownKeys();
            }
        });
        final ProxyObject missingNonConfigurable = p(target(), "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("plain"))));
        bad("ownKeys: a non-configurable key is missing", new Block() {
            public void run() {
                missingNonConfigurable.ownKeys();
            }
        });
        ProxyObject allNonConfigurable = p(target(), "ownKeys",
                e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("fixed"), MetaTestSupport.s("acc"), MetaTestSupport.s("locked"), MetaTestSupport.s("extra"))));
        Check.equal(4, allNonConfigurable.ownKeys().size(), "ownKeys: all non-configurable keys plus extras for an extensible target is fine");

        final ProxyObject extra = p(sealedTarget(), "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("x"), MetaTestSupport.s("y"), MetaTestSupport.s("z"))));
        bad("ownKeys: an extra key for a non-extensible target", new Block() {
            public void run() {
                extra.ownKeys();
            }
        });
        final ProxyObject lacking = p(sealedTarget(), "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("y"))));
        bad("ownKeys: a configurable key is missing for a non-extensible target", new Block() {
            public void run() {
                lacking.ownKeys();
            }
        });
        ProxyObject exact = p(sealedTarget(), "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("y"), MetaTestSupport.s("x"))));
        Check.equal("[y, x]", exact.ownKeys().toString(), "ownKeys: exactly the keys of a non-extensible target in any order");
        JsSymbol sym = new JsSymbol("k");
        JsObject withSymbol = e.obj();
        withSymbol.defineData(sym, Js.TRUE, true, true, false);
        ProxyObject symbolKept = p(withSymbol, "ownKeys", e.realm.newArray(MetaTestSupport.args(sym)));
        Check.same(sym, symbolKept.ownKeys().get(0), "ownKeys: a symbol key is accepted");
        final ProxyObject symbolMissing = p(withSymbol, "ownKeys", e.realm.newArray(MetaTestSupport.args(MetaTestSupport.s("other"))));
        bad("ownKeys: a non-configurable symbol key is missing", new Block() {
            public void run() {
                symbolMissing.ownKeys();
            }
        });
    }

    /**
     * 9.5.14 [[Construct]]: the construct trap must return an object.
     * <p>
     * 9.5.14 [[Construct]]: construct trap 은 객체를 돌려주어야 한다.
     */
    public void testConstructInvariant() {
        final NativeFunction ctor = new NativeFunction(e.realm, "C", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        });
        final ProxyObject number = p(ctor, "construct", MetaTestSupport.n(1));
        bad("construct: number result", new Block() {
            public void run() {
                Ops.construct(number, Js.NO_ARGS);
            }
        });
        final ProxyObject undef = p(ctor, "construct", Js.UNDEFINED);
        bad("construct: undefined result", new Block() {
            public void run() {
                Ops.construct(undef, Js.NO_ARGS);
            }
        });
        JsObject made = e.obj();
        Check.same(made, Ops.construct(p(ctor, "construct", made), Js.NO_ARGS), "construct: an object result is fine");
    }
}
