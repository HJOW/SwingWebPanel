package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * An arguments exotic object (ECMA-262 6th edition 9.4.4). In sloppy mode functions with simple parameter lists get a
 * <em>mapped</em> arguments object: <code>arguments[i]</code> and the parameter variable are two views of the same binding
 * until the property is deleted, redefined as an accessor or made read-only. Strict mode functions get an unmapped object.
 * <p>
 * arguments 이국적 객체(ECMA-262 6판 9.4.4). 느슨한 모드에서 단순 매개변수 목록을 가진 함수는 <em>매핑된</em> arguments 객체를
 * 받는다: 프로퍼티가 삭제되거나 접근자로 재정의되거나 읽기 전용이 되기 전까지 <code>arguments[i]</code> 와 매개변수 변수는
 * 같은 바인딩의 두 가지 보기이다. 엄격 모드 함수는 매핑되지 않은 객체를 받는다.
 */
final class ArgumentsObject extends JsObject {

    /**
     * For each argument index the parameter binding it is mapped to, or null.
     * <p>
     * 인수 인덱스마다 매핑된 매개변수 바인딩. 없으면 null.
     */
    private final Binding[] mapped;

    /**
     * Creates an arguments object.
     * <p>
     * arguments 객체를 만든다.
     *
     * @param proto  the prototype (Object.prototype) / 프로토타입(Object.prototype)
     * @param mapped the mapped bindings (length = number of arguments, null entries are unmapped) or null for an unmapped object /
     *               매핑된 바인딩(길이는 인수 개수, null 항목은 매핑 없음). 매핑되지 않은 객체이면 null
     */
    ArgumentsObject(JsObject proto, Binding[] mapped) {
        super(proto);
        this.mapped = mapped;
    }

    /**
     * Returns "Arguments".
     * <p>
     * "Arguments" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Arguments";
    }

    /**
     * Returns the mapped index for a key, or -1.
     * <p>
     * 키에 해당하는 매핑된 인덱스를 반환한다. 없으면 -1.
     *
     * @param key the key / 키
     * @return the index or -1 / 인덱스 또는 -1
     */
    private int mappedIndex(Object key) {
        if (mapped == null || !(key instanceof String)) {
            return -1;
        }
        long idx = PropertyKeys.arrayIndex((String) key);
        if (idx >= 0 && idx < mapped.length && mapped[(int) idx] != null) {
            return (int) idx;
        }
        return -1;
    }

    /**
     * Reads the current value of a mapped element from its parameter binding.
     * <p>
     * 매핑된 요소의 현재 값을 매개변수 바인딩에서 읽는다.
     *
     * @param key the key / 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        Property p = super.getOwnPropertySlot(key);
        int i = mappedIndex(key);
        if (p != null && i >= 0 && !p.accessor) {
            p.value = mapped[i].value;
        }
        return p;
    }

    /**
     * Defines a property and keeps the parameter mapping consistent.
     * <p>
     * 프로퍼티를 정의하고 매개변수 매핑을 일관되게 유지한다.
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        int i = mappedIndex(key);
        PropertyDescriptor d = desc;
        if (i >= 0 && !desc.isAccessor() && desc.value == null && desc.writable != null && !desc.writable.booleanValue()) {
            // 값 없이 writable 만 끄는 경우 현재 매핑된 값을 고정한다.
            d = new PropertyDescriptor();
            d.writable = desc.writable;
            d.enumerable = desc.enumerable;
            d.configurable = desc.configurable;
            d.value = mapped[i].value;
        }
        if (!super.defineOwnProperty(key, d)) {
            return false;
        }
        if (i >= 0) {
            if (desc.isAccessor()) {
                mapped[i] = null;
            } else {
                if (desc.value != null) {
                    mapped[i].value = desc.value;
                }
                if (desc.writable != null && !desc.writable.booleanValue()) {
                    mapped[i] = null;
                }
            }
        }
        return true;
    }

    /**
     * Deletes a property and removes its mapping.
     * <p>
     * 프로퍼티를 지우고 매핑을 해제한다.
     *
     * @param key the key / 키
     * @return true if gone afterwards / 삭제 후 없으면 true
     */
    public boolean delete(Object key) {
        int i = mappedIndex(key);
        boolean ok = super.delete(key);
        if (ok && i >= 0) {
            mapped[i] = null;
        }
        return ok;
    }

    /**
     * Returns a plain value of an element for internal use.
     * <p>
     * 내부용으로 요소의 값을 반환한다.
     *
     * @param i the index / 인덱스
     * @return the value / 값
     */
    JsValue element(int i) {
        return get(PropertyKeys.fromIndex(i));
    }
}
