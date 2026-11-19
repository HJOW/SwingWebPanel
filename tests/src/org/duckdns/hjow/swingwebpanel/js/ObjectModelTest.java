package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the object model (plan item P12-03): own properties, prototypes, data and accessor properties and their
 * writable/enumerable/configurable attributes, the common property operations, key order and extensibility.
 * <p>
 * 객체 모델 시험(계획 P12-03): 자체 프로퍼티, 프로토타입, 데이터/접근자 프로퍼티와 writable/enumerable/configurable 속성,
 * 공통 프로퍼티 연산, 키 순서, 확장 가능성.
 */
public class ObjectModelTest {

    /**
     * The realm used by the cases.
     * <p>
     * 사례가 쓰는 realm.
     */
    private final JsRealm realm = new JsRealm();

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ObjectModelTest() {
    }

    /**
     * Properties defined with default attributes are read-only, hidden and permanent.
     * <p>
     * 기본 속성으로 정의한 프로퍼티는 읽기 전용, 숨김, 영구이다.
     */
    public void testDefaultAttributesAreAllFalse() {
        JsObject o = realm.newObject();
        PropertyDescriptor d = new PropertyDescriptor();
        d.value = Js.num(1);
        Check.isTrue(o.defineOwnProperty("x", d), "define succeeds");
        PropertyDescriptor got = o.getOwnProperty("x");
        Check.isFalse(got.isWritableTrue(), "writable false");
        Check.isFalse(got.isEnumerableTrue(), "enumerable false");
        Check.isFalse(got.isConfigurableTrue(), "configurable false");
        Check.isFalse(o.set("x", Js.num(2)), "assignment is rejected");
        Check.equal(1, (long) ((JsNumber) o.get("x")).getValue(), "value unchanged");
        Check.isFalse(o.delete("x"), "delete of a non-configurable property fails");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                o_setStrict();
            }

            private void o_setStrict() {
                JsObject p = realm.newObject();
                p.defineData("y", Js.num(1), false, true, true);
                p.setStrict("y", Js.num(2));
            }
        }, "strict assignment to a read-only property throws a TypeError");
    }

    /**
     * Lookup follows the prototype chain and assignment creates an own property on the receiver.
     * <p>
     * 조회는 프로토타입 체인을 따르고, 대입은 receiver 에 자체 프로퍼티를 만든다.
     */
    public void testPrototypeChainLookupAndShadowing() {
        JsObject proto = realm.newObject();
        proto.createDataProperty("a", Js.num(1));
        JsObject child = new JsObject(proto);
        Check.equal(1, (long) ((JsNumber) child.get("a")).getValue(), "inherited value");
        Check.isFalse(child.hasOwnProperty("a"), "not own");
        Check.isTrue(child.hasProperty("a"), "but present");
        Check.isTrue(child.set("a", Js.num(2)), "assignment succeeds");
        Check.isTrue(child.hasOwnProperty("a"), "shadowing own property created");
        Check.equal(1, (long) ((JsNumber) proto.get("a")).getValue(), "prototype value unchanged");
        Check.isTrue(child.delete("a"), "delete own");
        Check.equal(1, (long) ((JsNumber) child.get("a")).getValue(), "inherited value visible again");
    }

    /**
     * A read-only property on the prototype blocks assignment to the derived object.
     * <p>
     * 프로토타입의 읽기 전용 프로퍼티는 파생 객체에 대한 대입을 막는다.
     */
    public void testInheritedReadOnlyBlocksAssignment() {
        JsObject proto = realm.newObject();
        proto.defineData("ro", Js.num(1), false, true, true);
        JsObject child = new JsObject(proto);
        Check.isFalse(child.set("ro", Js.num(2)), "inherited read-only property");
        Check.isFalse(child.hasOwnProperty("ro"), "no own property was created");
    }

    /**
     * Getters and setters receive the original object as this, including through the prototype chain.
     * <p>
     * getter 와 setter 는 프로토타입 체인을 거쳐도 원래 객체를 this 로 받는다.
     */
    public void testAccessorsUseReceiver() {
        final JsObject proto = realm.newObject();
        NativeFunction getter = realm.newFunction("get v", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return ((JsObject) thisValue).get("_v");
            }
        });
        NativeFunction setter = realm.newFunction("set v", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                ((JsObject) thisValue).createDataProperty("_v", args[0]);
                return Js.UNDEFINED;
            }
        });
        proto.defineAccessor("v", getter, setter, true, true);
        JsObject child = new JsObject(proto);
        Check.isTrue(child.set("v", Js.num(7)), "setter runs");
        Check.isTrue(child.hasOwnProperty("_v"), "setter stored on the receiver");
        Check.isFalse(proto.hasOwnProperty("_v"), "not on the prototype");
        Check.equal(7, (long) ((JsNumber) child.get("v")).getValue(), "getter reads from the receiver");
        Check.isTrue(proto.get("v").isUndefined(), "the prototype itself has no _v");
        proto.defineAccessor("onlyGet", getter, null, true, true);
        Check.isFalse(child.set("onlyGet", Js.num(1)), "accessor without setter rejects assignment");
    }

    /**
     * Rules for redefining non-configurable properties (ValidateAndApplyPropertyDescriptor).
     * <p>
     * 설정 불가 프로퍼티 재정의 규칙(ValidateAndApplyPropertyDescriptor).
     */
    public void testRedefinitionRules() {
        JsObject o = realm.newObject();
        o.defineData("k", Js.num(1), true, true, false);
        PropertyDescriptor d = new PropertyDescriptor();
        d.configurable = Boolean.TRUE;
        Check.isFalse(o.defineOwnProperty("k", d), "cannot become configurable");
        d = new PropertyDescriptor();
        d.enumerable = Boolean.FALSE;
        Check.isFalse(o.defineOwnProperty("k", d), "cannot change enumerable");
        d = PropertyDescriptor.accessor(null, null, true, false);
        Check.isFalse(o.defineOwnProperty("k", d), "cannot change data into accessor");
        d = PropertyDescriptor.valueOnly(Js.num(5));
        Check.isTrue(o.defineOwnProperty("k", d), "writable data property can change value");
        d = new PropertyDescriptor();
        d.writable = Boolean.FALSE;
        Check.isTrue(o.defineOwnProperty("k", d), "writable can go from true to false");
        d = PropertyDescriptor.valueOnly(Js.num(6));
        Check.isFalse(o.defineOwnProperty("k", d), "now read-only: value cannot change");
        d = PropertyDescriptor.valueOnly(Js.num(5));
        Check.isTrue(o.defineOwnProperty("k", d), "same value is allowed (SameValue)");
        d = new PropertyDescriptor();
        d.writable = Boolean.TRUE;
        Check.isFalse(o.defineOwnProperty("k", d), "read-only cannot become writable again");
    }

    /**
     * A configurable data property can become an accessor and keeps its enumerable and configurable attributes.
     * <p>
     * 설정 가능한 데이터 프로퍼티는 접근자로 바뀔 수 있고 enumerable, configurable 속성은 유지된다.
     */
    public void testDataToAccessorConversion() {
        JsObject o = realm.newObject();
        o.defineData("k", Js.num(1), true, true, true);
        NativeFunction g = realm.newFunction("g", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(42);
            }
        });
        PropertyDescriptor d = new PropertyDescriptor();
        d.get = g;
        Check.isTrue(o.defineOwnProperty("k", d), "conversion allowed");
        PropertyDescriptor got = o.getOwnProperty("k");
        Check.isTrue(got.isAccessor(), "now an accessor");
        Check.isTrue(got.isEnumerableTrue() && got.isConfigurableTrue(), "attributes are kept");
        Check.isTrue(got.set == Js.UNDEFINED, "setter is undefined");
        Check.equal(42, (long) ((JsNumber) o.get("k")).getValue(), "getter works");
    }

    /**
     * Own keys: integer indices ascending (numerically), strings in creation order, then symbols.
     * <p>
     * 자체 키: 정수 인덱스(숫자 오름차순), 문자열(생성 순서), 심볼 순서.
     */
    public void testOwnKeysOrder() {
        JsObject o = realm.newObject();
        JsSymbol s1 = new JsSymbol("s1");
        o.createDataProperty("b", Js.num(0));
        o.createDataProperty(s1, Js.num(0));
        o.createDataProperty("10", Js.num(0));
        o.createDataProperty("a", Js.num(0));
        o.createDataProperty("9", Js.num(0));
        o.createDataProperty("01", Js.num(0));
        o.createDataProperty("2", Js.num(0));
        Check.equal("[2, 9, 10, b, a, 01, Symbol(s1)]", o.ownKeys().toString(), "key order");
        Check.equal("[2, 9, 10, b, a, 01]", Ops.enumerableOwnKeys(o).toString(), "string keys only");
    }

    /**
     * preventExtensions blocks new properties but keeps existing ones usable.
     * <p>
     * preventExtensions 는 새 프로퍼티를 막지만 기존 프로퍼티는 계속 쓸 수 있다.
     */
    public void testPreventExtensions() {
        JsObject o = realm.newObject();
        o.createDataProperty("x", Js.num(1));
        Check.isTrue(o.preventExtensions(), "prevent");
        Check.isFalse(o.isExtensible(), "not extensible");
        Check.isFalse(o.createDataProperty("y", Js.num(2)), "new property rejected");
        Check.isTrue(o.set("x", Js.num(3)), "existing property still writable");
        Check.isTrue(o.delete("x"), "existing configurable property can be deleted");
        Check.isFalse(o.setPrototypeOf(null), "prototype cannot change on a non-extensible object");
        Check.isTrue(o.setPrototypeOf(realm.getObjectPrototype()), "setting the same prototype is allowed");
    }

    /**
     * Prototype cycles are rejected.
     * <p>
     * 프로토타입 순환은 거부된다.
     */
    public void testPrototypeCycleRejected() {
        JsObject a = realm.newObject();
        JsObject b = new JsObject(a);
        Check.isFalse(a.setPrototypeOf(b), "a -> b -> a would be a cycle");
        Check.isFalse(a.setPrototypeOf(a), "self cycle");
        Check.isTrue(b.getPrototypeOf() == a, "unchanged");
    }

    /**
     * delete and hasProperty behave as specified.
     * <p>
     * delete 와 hasProperty 가 규격대로 동작한다.
     */
    public void testDeleteAndHas() {
        JsObject o = realm.newObject();
        Check.isTrue(o.delete("missing"), "deleting a missing property succeeds");
        Check.isFalse(o.hasProperty("toString"), "a bare realm has no builtin methods yet");
        o.createDataProperty("p", Js.TRUE);
        Check.isTrue(o.hasProperty("p") && o.hasOwnProperty("p"), "present");
        Check.isTrue(o.delete("p") && !o.hasProperty("p"), "deleted");
    }

    /**
     * Descriptor helper methods classify descriptors.
     * <p>
     * 기술자 도우미 메서드가 기술자를 분류한다.
     */
    public void testDescriptorClassification() {
        PropertyDescriptor empty = new PropertyDescriptor();
        Check.isTrue(empty.isEmpty() && empty.isGeneric(), "empty is generic");
        Check.isTrue(PropertyDescriptor.valueOnly(Js.num(1)).isData(), "value makes it data");
        Check.isTrue(PropertyDescriptor.accessor(null, null, true, true).isAccessor(), "accessor");
        Check.isFalse(PropertyDescriptor.accessor(null, null, true, true).isData(), "accessor is not data");
    }
}
