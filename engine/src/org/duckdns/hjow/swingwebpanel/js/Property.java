package org.duckdns.hjow.swingwebpanel.js;

/**
 * Stored form of an own property: either a data property (value + writable) or an accessor property (getter + setter),
 * plus the enumerable and configurable attributes. Instances live inside the property table of a {@link JsObject}
 * and are mutated in place; built-in authors normally use {@link PropertyDescriptor} and the helper methods of {@link JsObject}
 * instead of touching this class.
 * <p>
 * 자체 프로퍼티의 저장 형태: 데이터 프로퍼티(값 + writable) 또는 접근자 프로퍼티(getter + setter)와 enumerable, configurable 속성.
 * 인스턴스는 {@link JsObject} 의 프로퍼티 표 안에 있으며 제자리에서 바뀐다. 내장 객체 작성자는 보통 이 클래스 대신
 * {@link PropertyDescriptor} 와 {@link JsObject} 의 도우미 메서드를 사용한다.
 */
public final class Property {

    /**
     * Value of a data property (null for accessor properties).
     * <p>
     * 데이터 프로퍼티의 값(접근자 프로퍼티는 null).
     */
    public JsValue value;

    /**
     * Getter function of an accessor property, or null for undefined.
     * <p>
     * 접근자 프로퍼티의 getter 함수. undefined 이면 null.
     */
    public JsObject getter;

    /**
     * Setter function of an accessor property, or null for undefined.
     * <p>
     * 접근자 프로퍼티의 setter 함수. undefined 이면 null.
     */
    public JsObject setter;

    /**
     * True for an accessor property, false for a data property.
     * <p>
     * 접근자 프로퍼티이면 true, 데이터 프로퍼티이면 false.
     */
    public boolean accessor;

    /**
     * The writable attribute (data properties only).
     * <p>
     * writable 속성(데이터 프로퍼티 전용).
     */
    public boolean writable;

    /**
     * The enumerable attribute.
     * <p>
     * enumerable 속성.
     */
    public boolean enumerable;

    /**
     * The configurable attribute.
     * <p>
     * configurable 속성.
     */
    public boolean configurable;

    /**
     * Creates an empty data property (value undefined, all attributes false).
     * <p>
     * 빈 데이터 프로퍼티(값 undefined, 모든 속성 false)를 만든다.
     */
    public Property() {
        this.value = Js.UNDEFINED;
    }

    /**
     * Creates a data property.
     * <p>
     * 데이터 프로퍼티를 만든다.
     *
     * @param value        the value / 값
     * @param writable     writable attribute / writable 속성
     * @param enumerable   enumerable attribute / enumerable 속성
     * @param configurable configurable attribute / configurable 속성
     * @return the property / 프로퍼티
     */
    public static Property data(JsValue value, boolean writable, boolean enumerable, boolean configurable) {
        Property p = new Property();
        p.value = value;
        p.writable = writable;
        p.enumerable = enumerable;
        p.configurable = configurable;
        return p;
    }

    /**
     * Creates an accessor property.
     * <p>
     * 접근자 프로퍼티를 만든다.
     *
     * @param getter       getter function or null / getter 함수 또는 null
     * @param setter       setter function or null / setter 함수 또는 null
     * @param enumerable   enumerable attribute / enumerable 속성
     * @param configurable configurable attribute / configurable 속성
     * @return the property / 프로퍼티
     */
    public static Property accessor(JsObject getter, JsObject setter, boolean enumerable, boolean configurable) {
        Property p = new Property();
        p.accessor = true;
        p.value = null;
        p.getter = getter;
        p.setter = setter;
        p.enumerable = enumerable;
        p.configurable = configurable;
        return p;
    }

    /**
     * Converts to a complete property descriptor (FromPropertyDescriptor input, ECMA-262 6th edition 6.2.4).
     * <p>
     * 완전한 프로퍼티 기술자로 변환한다(ECMA-262 6판 6.2.4).
     *
     * @return a new complete descriptor / 새 완전 기술자
     */
    public PropertyDescriptor toDescriptor() {
        PropertyDescriptor d = new PropertyDescriptor();
        if (accessor) {
            d.get = getter == null ? Js.UNDEFINED : getter;
            d.set = setter == null ? Js.UNDEFINED : setter;
        } else {
            d.value = value;
            d.writable = Boolean.valueOf(writable);
        }
        d.enumerable = Boolean.valueOf(enumerable);
        d.configurable = Boolean.valueOf(configurable);
        return d;
    }
}
