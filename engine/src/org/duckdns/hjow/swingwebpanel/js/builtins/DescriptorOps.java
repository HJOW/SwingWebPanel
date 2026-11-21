package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * Conversion between property descriptor objects of scripts (<code>{ value, writable, get, set, enumerable, configurable }</code>)
 * and the {@link PropertyDescriptor} specification type: ToPropertyDescriptor and FromPropertyDescriptor (ECMA-262 6th edition
 * 6.2.4.4 and 6.2.4.5). Used by Object.defineProperty, Reflect.defineProperty and similar built-ins.
 * <p>
 * 스크립트의 프로퍼티 기술자 객체(<code>{ value, writable, get, set, enumerable, configurable }</code>)와 명세 형식
 * {@link PropertyDescriptor} 사이의 변환: ToPropertyDescriptor 와 FromPropertyDescriptor(ECMA-262 6판 6.2.4.4, 6.2.4.5).
 * Object.defineProperty, Reflect.defineProperty 같은 내장 함수가 쓴다.
 */
public final class DescriptorOps {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DescriptorOps() {
    }

    /**
     * ToPropertyDescriptor: converts a script object to a descriptor, validating accessor functions.
     * <p>
     * ToPropertyDescriptor: 스크립트 객체를 기술자로 바꾸며 접근자 함수를 검증한다.
     *
     * @param v the descriptor object / 기술자 객체
     * @return the descriptor / 기술자
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
     * FromPropertyDescriptor: converts a descriptor to a script object (undefined for null).
     * <p>
     * FromPropertyDescriptor: 기술자를 스크립트 객체로 바꾼다(null 이면 undefined).
     *
     * @param realm the realm / realm
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
            o.createDataProperty("writable", JsBoolean.of(d.writable.booleanValue()));
        }
        if (d.get != null) {
            o.createDataProperty("get", d.get);
        }
        if (d.set != null) {
            o.createDataProperty("set", d.set);
        }
        if (d.enumerable != null) {
            o.createDataProperty("enumerable", JsBoolean.of(d.enumerable.booleanValue()));
        }
        if (d.configurable != null) {
            o.createDataProperty("configurable", JsBoolean.of(d.configurable.booleanValue()));
        }
        return o;
    }
}
