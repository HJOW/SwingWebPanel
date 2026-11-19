package org.duckdns.hjow.swingwebpanel.js;

/**
 * A Property Descriptor specification type (ECMA-262 6th edition 6.2.4). Every field may be absent:
 * a Java <code>null</code> field means "absent" (not specified), unlike a JavaScript value. For <code>get</code> and
 * <code>set</code> the present values are either a callable object or {@link Js#UNDEFINED}.
 * Use {@link #isAccessor()}, {@link #isData()} and {@link #isGeneric()} to classify it.
 * <p>
 * 프로퍼티 기술자 명세 형식(ECMA-262 6판 6.2.4). 모든 필드는 없을 수 있다. Java <code>null</code> 필드는
 * "지정되지 않음"을 뜻하며 JavaScript 값이 아니다. <code>get</code>, <code>set</code> 이 있으면 호출 가능 객체 또는
 * {@link Js#UNDEFINED} 이다. 분류에는 {@link #isAccessor()}, {@link #isData()}, {@link #isGeneric()} 를 사용한다.
 */
public final class PropertyDescriptor {

    /**
     * The [[Value]] field or null if absent.
     * <p>
     * [[Value]] 필드. 없으면 null.
     */
    public JsValue value;

    /**
     * The [[Get]] field (callable object or undefined) or null if absent.
     * <p>
     * [[Get]] 필드(호출 가능 객체 또는 undefined). 없으면 null.
     */
    public JsValue get;

    /**
     * The [[Set]] field (callable object or undefined) or null if absent.
     * <p>
     * [[Set]] 필드(호출 가능 객체 또는 undefined). 없으면 null.
     */
    public JsValue set;

    /**
     * The [[Writable]] field or null if absent.
     * <p>
     * [[Writable]] 필드. 없으면 null.
     */
    public Boolean writable;

    /**
     * The [[Enumerable]] field or null if absent.
     * <p>
     * [[Enumerable]] 필드. 없으면 null.
     */
    public Boolean enumerable;

    /**
     * The [[Configurable]] field or null if absent.
     * <p>
     * [[Configurable]] 필드. 없으면 null.
     */
    public Boolean configurable;

    /**
     * Creates an empty (generic, all absent) descriptor.
     * <p>
     * 빈(일반, 모든 필드 없음) 기술자를 만든다.
     */
    public PropertyDescriptor() {
    }

    /**
     * Creates a complete data descriptor.
     * <p>
     * 완전한 데이터 기술자를 만든다.
     *
     * @param value        the value / 값
     * @param writable     writable / writable
     * @param enumerable   enumerable / enumerable
     * @param configurable configurable / configurable
     * @return the descriptor / 기술자
     */
    public static PropertyDescriptor data(JsValue value, boolean writable, boolean enumerable, boolean configurable) {
        PropertyDescriptor d = new PropertyDescriptor();
        d.value = value;
        d.writable = Boolean.valueOf(writable);
        d.enumerable = Boolean.valueOf(enumerable);
        d.configurable = Boolean.valueOf(configurable);
        return d;
    }

    /**
     * Creates a data descriptor that only carries a value (used by [[Set]] on existing properties).
     * <p>
     * 값만 가진 데이터 기술자를 만든다(기존 프로퍼티에 대한 [[Set]] 이 사용).
     *
     * @param value the value / 값
     * @return the descriptor / 기술자
     */
    public static PropertyDescriptor valueOnly(JsValue value) {
        PropertyDescriptor d = new PropertyDescriptor();
        d.value = value;
        return d;
    }

    /**
     * Creates a complete accessor descriptor.
     * <p>
     * 완전한 접근자 기술자를 만든다.
     *
     * @param getter       getter or null for undefined / getter. undefined 이면 null
     * @param setter       setter or null for undefined / setter. undefined 이면 null
     * @param enumerable   enumerable / enumerable
     * @param configurable configurable / configurable
     * @return the descriptor / 기술자
     */
    public static PropertyDescriptor accessor(JsObject getter, JsObject setter, boolean enumerable, boolean configurable) {
        PropertyDescriptor d = new PropertyDescriptor();
        d.get = getter == null ? Js.UNDEFINED : getter;
        d.set = setter == null ? Js.UNDEFINED : setter;
        d.enumerable = Boolean.valueOf(enumerable);
        d.configurable = Boolean.valueOf(configurable);
        return d;
    }

    /**
     * Returns true if the descriptor has a [[Get]] or [[Set]] field.
     * <p>
     * [[Get]] 또는 [[Set]] 필드가 있으면 true.
     *
     * @return true for accessor descriptors / 접근자 기술자이면 true
     */
    public boolean isAccessor() {
        return get != null || set != null;
    }

    /**
     * Returns true if the descriptor has a [[Value]] or [[Writable]] field.
     * <p>
     * [[Value]] 또는 [[Writable]] 필드가 있으면 true.
     *
     * @return true for data descriptors / 데이터 기술자이면 true
     */
    public boolean isData() {
        return value != null || writable != null;
    }

    /**
     * Returns true if the descriptor is neither accessor nor data.
     * <p>
     * 접근자도 데이터도 아니면 true.
     *
     * @return true for generic descriptors / 일반 기술자이면 true
     */
    public boolean isGeneric() {
        return !isAccessor() && !isData();
    }

    /**
     * Returns true if no field is present at all.
     * <p>
     * 모든 필드가 없으면 true.
     *
     * @return true if empty / 비어 있으면 true
     */
    public boolean isEmpty() {
        return value == null && get == null && set == null && writable == null && enumerable == null && configurable == null;
    }

    /**
     * Returns true if the [[Writable]] field is present and true.
     * <p>
     * [[Writable]] 필드가 있고 true 이면 true.
     *
     * @return true if writable is true / writable 이 true 이면 true
     */
    public boolean isWritableTrue() {
        return writable != null && writable.booleanValue();
    }

    /**
     * Returns true if the [[Enumerable]] field is present and true.
     * <p>
     * [[Enumerable]] 필드가 있고 true 이면 true.
     *
     * @return true if enumerable is true / enumerable 이 true 이면 true
     */
    public boolean isEnumerableTrue() {
        return enumerable != null && enumerable.booleanValue();
    }

    /**
     * Returns true if the [[Configurable]] field is present and true.
     * <p>
     * [[Configurable]] 필드가 있고 true 이면 true.
     *
     * @return true if configurable is true / configurable 이 true 이면 true
     */
    public boolean isConfigurableTrue() {
        return configurable != null && configurable.booleanValue();
    }
}
