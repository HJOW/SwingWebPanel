package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * Conversions between JavaScript property descriptor objects and {@link PropertyDescriptor} records, and the descriptor
 * algorithms that the Proxy invariants and Reflect need (ECMA-262 6th edition 6.2.4): ToPropertyDescriptor,
 * FromPropertyDescriptor, CompletePropertyDescriptor and IsCompatiblePropertyDescriptor.
 * <p>
 * JavaScript 프로퍼티 기술자 객체와 {@link PropertyDescriptor} 기록 사이의 변환, 그리고 Proxy 불변 조건과 Reflect 에 필요한
 * 기술자 알고리즘(ECMA-262 6판 6.2.4): ToPropertyDescriptor, FromPropertyDescriptor, CompletePropertyDescriptor,
 * IsCompatiblePropertyDescriptor.
 */
public final class Descriptors {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Descriptors() {
    }

    /**
     * ToPropertyDescriptor (6.2.4.5): reads a descriptor object. Fields are read in the order enumerable, configurable, value,
     * writable, get, set, and only if the object has the property.
     * <p>
     * ToPropertyDescriptor (6.2.4.5): 기술자 객체를 읽는다. 필드는 enumerable, configurable, value, writable, get, set 순서로
     * 객체에 프로퍼티가 있을 때만 읽는다.
     *
     * @param v the descriptor object / 기술자 객체
     * @return the descriptor record / 기술자 기록
     * @throws JsException a TypeError for non-objects, non-callable accessors and mixed descriptors / 객체가 아니거나
     *                     접근자가 호출 가능하지 않거나 데이터와 접근자가 섞인 경우 TypeError
     */
    public static PropertyDescriptor toDescriptor(JsValue v) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError("Property description must be an object: " + JsException.describe(v));
        }
        JsObject o = (JsObject) v;
        PropertyDescriptor d = new PropertyDescriptor();
        if (o.hasProperty("enumerable")) {
            d.enumerable = Boolean.valueOf(Ops.toBoolean(o.get("enumerable")));
        }
        if (o.hasProperty("configurable")) {
            d.configurable = Boolean.valueOf(Ops.toBoolean(o.get("configurable")));
        }
        if (o.hasProperty("value")) {
            d.value = o.get("value");
        }
        if (o.hasProperty("writable")) {
            d.writable = Boolean.valueOf(Ops.toBoolean(o.get("writable")));
        }
        if (o.hasProperty("get")) {
            JsValue g = o.get("get");
            if (!g.isUndefined() && !g.isCallable()) {
                throw JsException.typeError("Getter must be a function: " + JsException.describe(g));
            }
            d.get = g;
        }
        if (o.hasProperty("set")) {
            JsValue s = o.get("set");
            if (!s.isUndefined() && !s.isCallable()) {
                throw JsException.typeError("Setter must be a function: " + JsException.describe(s));
            }
            d.set = s;
        }
        if ((d.get != null || d.set != null) && (d.value != null || d.writable != null)) {
            throw JsException.typeError("Invalid property descriptor. Cannot both specify accessors and a value or writable attribute");
        }
        return d;
    }

    /**
     * FromPropertyDescriptor (6.2.4.4): creates a descriptor object with the present fields in the order value, writable, get,
     * set, enumerable, configurable. A null descriptor gives undefined.
     * <p>
     * FromPropertyDescriptor (6.2.4.4): 있는 필드를 value, writable, get, set, enumerable, configurable 순서로 담은 기술자 객체를
     * 만든다. 기술자가 null 이면 undefined.
     *
     * @param realm the realm of the new object / 새 객체의 realm
     * @param d     the descriptor or null / 기술자 또는 null
     * @return the descriptor object or undefined / 기술자 객체 또는 undefined
     */
    public static JsValue fromDescriptor(JsRealm realm, PropertyDescriptor d) {
        if (d == null) {
            return Js.UNDEFINED;
        }
        JsObject o = realm.newObject();
        if (d.value != null) {
            o.createDataProperty("value", d.value);
        }
        if (d.writable != null) {
            o.createDataProperty("writable", Js.bool(d.writable.booleanValue()));
        }
        if (d.get != null) {
            o.createDataProperty("get", d.get);
        }
        if (d.set != null) {
            o.createDataProperty("set", d.set);
        }
        if (d.enumerable != null) {
            o.createDataProperty("enumerable", Js.bool(d.enumerable.booleanValue()));
        }
        if (d.configurable != null) {
            o.createDataProperty("configurable", Js.bool(d.configurable.booleanValue()));
        }
        return o;
    }

    /**
     * CompletePropertyDescriptor (6.2.4.6): fills the absent fields with their defaults, in place.
     * <p>
     * CompletePropertyDescriptor (6.2.4.6): 없는 필드를 기본값으로 채운다(제자리).
     *
     * @param d the descriptor to complete / 완성할 기술자
     * @return the same descriptor / 같은 기술자
     */
    public static PropertyDescriptor complete(PropertyDescriptor d) {
        if (d.isAccessor()) {
            if (d.get == null) {
                d.get = Js.UNDEFINED;
            }
            if (d.set == null) {
                d.set = Js.UNDEFINED;
            }
        } else {
            if (d.value == null) {
                d.value = Js.UNDEFINED;
            }
            if (d.writable == null) {
                d.writable = Boolean.FALSE;
            }
        }
        if (d.enumerable == null) {
            d.enumerable = Boolean.FALSE;
        }
        if (d.configurable == null) {
            d.configurable = Boolean.FALSE;
        }
        return d;
    }

    /**
     * IsCompatiblePropertyDescriptor (9.1.6.2): tells whether a descriptor could be applied to a property whose current state is
     * given, without applying it.
     * <p>
     * IsCompatiblePropertyDescriptor (9.1.6.2): 현재 상태가 주어진 프로퍼티에 기술자를 적용할 수 있는지를, 적용하지 않고 알려 준다.
     *
     * @param extensible whether the object is extensible / 객체가 확장 가능한지
     * @param d          the new descriptor / 새 기술자
     * @param current    the current complete descriptor or null / 현재 완전 기술자 또는 null
     * @return true if compatible / 호환되면 true
     */
    public static boolean isCompatible(boolean extensible, PropertyDescriptor d, PropertyDescriptor current) {
        if (current == null) {
            return extensible;
        }
        if (d.isEmpty()) {
            return true;
        }
        boolean currentConfigurable = current.isConfigurableTrue();
        if (!currentConfigurable) {
            if (d.isConfigurableTrue()) {
                return false;
            }
            if (d.enumerable != null && d.enumerable.booleanValue() != current.isEnumerableTrue()) {
                return false;
            }
        }
        if (d.isGeneric()) {
            return true;
        }
        if (d.isAccessor() != current.isAccessor()) {
            // 데이터와 접근자 사이의 변환은 설정 가능할 때만 허용된다.
            return currentConfigurable;
        }
        if (!current.isAccessor()) {
            if (!currentConfigurable && !current.isWritableTrue()) {
                if (d.isWritableTrue()) {
                    return false;
                }
                if (d.value != null && !Ops.sameValue(d.value, current.value)) {
                    return false;
                }
            }
            return true;
        }
        if (!currentConfigurable) {
            if (d.set != null && !Ops.sameValue(d.set, current.set)) {
                return false;
            }
            if (d.get != null && !Ops.sameValue(d.get, current.get)) {
                return false;
            }
        }
        return true;
    }
}
