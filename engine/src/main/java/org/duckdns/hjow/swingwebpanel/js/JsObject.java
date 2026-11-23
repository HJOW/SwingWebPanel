package org.duckdns.hjow.swingwebpanel.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * An ECMAScript object with the ordinary object internal methods of ECMA-262 6th edition 9.1:
 * [[GetPrototypeOf]], [[SetPrototypeOf]], [[IsExtensible]], [[PreventExtensions]], [[GetOwnProperty]],
 * [[DefineOwnProperty]], [[HasProperty]], [[Get]], [[Set]], [[Delete]], [[OwnPropertyKeys]] (and [[Call]]/[[Construct]]
 * for function subclasses). Exotic objects (arrays, string wrappers, arguments, proxies, host objects) subclass this class and
 * override the internal methods; every other part of the engine goes through these methods only.
 * <p>
 * Property keys are {@link java.lang.String} or {@link JsSymbol} objects held in a Java <code>Object</code> reference.
 * Own keys are listed in the order required by 9.1.12: array indices ascending, then strings in creation order,
 * then symbols in creation order.
 * <p>
 * Built-in authors mostly use the convenience methods: {@link #get(Object)}, {@link #set(Object, JsValue)},
 * {@link #setStrict(Object, JsValue)}, {@link #defineBuiltin(Object, JsValue)}, {@link #defineData(Object, JsValue, boolean, boolean, boolean)},
 * {@link #defineAccessor(Object, JsObject, JsObject, boolean, boolean)}, {@link #createDataProperty(Object, JsValue)}.
 * <p>
 * ECMA-262 6판 9.1 의 일반 객체 내부 메서드를 가진 ECMAScript 객체: [[GetPrototypeOf]], [[SetPrototypeOf]], [[IsExtensible]],
 * [[PreventExtensions]], [[GetOwnProperty]], [[DefineOwnProperty]], [[HasProperty]], [[Get]], [[Set]], [[Delete]],
 * [[OwnPropertyKeys]] (함수 하위 클래스는 [[Call]]/[[Construct]] 도). 이국적 객체(배열, 문자열 래퍼, arguments, 프록시, 호스트 객체)는
 * 이 클래스를 상속해 내부 메서드를 재정의하며, 엔진의 다른 부분은 이 메서드들만 거쳐 접근한다.
 * <p>
 * 프로퍼티 키는 Java <code>Object</code> 참조에 담긴 {@link java.lang.String} 또는 {@link JsSymbol} 이다.
 * 자체 키 나열 순서는 9.1.12 를 따른다: 배열 인덱스 오름차순, 문자열(생성 순서), 심볼(생성 순서).
 * <p>
 * 내장 객체 작성자는 주로 편의 메서드를 쓴다: {@link #get(Object)}, {@link #set(Object, JsValue)}, {@link #setStrict(Object, JsValue)},
 * {@link #defineBuiltin(Object, JsValue)}, {@link #defineData(Object, JsValue, boolean, boolean, boolean)},
 * {@link #defineAccessor(Object, JsObject, JsObject, boolean, boolean)}, {@link #createDataProperty(Object, JsValue)}.
 */
public class JsObject extends JsValue {

    /**
     * The [[Prototype]] internal slot (null for none).
     * <p>
     * [[Prototype]] 내부 슬롯(없으면 null).
     */
    protected JsObject prototype;

    /**
     * The [[Extensible]] internal slot.
     * <p>
     * [[Extensible]] 내부 슬롯.
     */
    protected boolean extensible = true;

    /**
     * Own property table in creation order (allocated on first use).
     * <p>
     * 생성 순서를 유지하는 자체 프로퍼티 표(처음 쓸 때 만든다).
     */
    protected LinkedHashMap<Object, Property> properties;

    /**
     * Creates an object without a prototype.
     * <p>
     * 프로토타입이 없는 객체를 만든다.
     */
    public JsObject() {
    }

    /**
     * Creates an object with a prototype.
     * <p>
     * 프로토타입이 있는 객체를 만든다.
     *
     * @param prototype the prototype or null / 프로토타입 또는 null
     */
    public JsObject(JsObject prototype) {
        this.prototype = prototype;
    }

    /**
     * Returns {@link JsType#OBJECT}.
     * <p>
     * {@link JsType#OBJECT} 를 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.OBJECT;
    }

    /**
     * Returns the builtinTag used by Object.prototype.toString: "Object" for ordinary objects; subclasses return
     * "Array", "Function", "Error", "Boolean", "Number", "String", "Date", "RegExp", "Arguments".
     * <p>
     * Object.prototype.toString 이 쓰는 builtinTag 를 반환한다. 일반 객체는 "Object"이며 하위 클래스는
     * "Array", "Function", "Error", "Boolean", "Number", "String", "Date", "RegExp", "Arguments" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Object";
    }

    /**
     * Returns true if the object is an Array exotic object (IsArray, 7.2.2). Proxies forward to their target.
     * <p>
     * 배열 이국적 객체이면 true (IsArray, 7.2.2). 프록시는 대상으로 전달한다.
     *
     * @return true for arrays / 배열이면 true
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Returns true if the object has a [[Call]] internal method.
     * <p>
     * [[Call]] 내부 메서드가 있으면 true.
     *
     * @return true if callable / 호출 가능하면 true
     */
    public boolean isCallable() {
        return false;
    }

    /**
     * Returns true if the object has a [[Construct]] internal method.
     * <p>
     * [[Construct]] 내부 메서드가 있으면 true.
     *
     * @return true if it is a constructor / 생성자이면 true
     */
    public boolean isConstructor() {
        return false;
    }

    /**
     * The [[Call]] internal method. Ordinary objects are not callable.
     * <p>
     * [[Call]] 내부 메서드. 일반 객체는 호출할 수 없다.
     *
     * @param thisValue the this value / this 값
     * @param args      arguments / 인수
     * @return the result / 결과
     */
    public JsValue call(JsValue thisValue, JsValue[] args) {
        throw JsException.typeError("object is not a function");
    }

    /**
     * The [[Construct]] internal method. Ordinary objects are not constructors.
     * <p>
     * [[Construct]] 내부 메서드. 일반 객체는 생성자가 아니다.
     *
     * @param args      arguments / 인수
     * @param newTarget the new.target constructor / new.target 생성자
     * @return the new object / 새 객체
     */
    public JsObject construct(JsValue[] args, JsObject newTarget) {
        throw JsException.typeError("object is not a constructor");
    }

    // ------------------------------------------------------------------ [[GetPrototypeOf]] ...

    /**
     * The [[GetPrototypeOf]] internal method.
     * <p>
     * [[GetPrototypeOf]] 내부 메서드.
     *
     * @return the prototype or null / 프로토타입 또는 null
     */
    public JsObject getPrototypeOf() {
        return prototype;
    }

    /**
     * The [[SetPrototypeOf]] internal method (9.1.2). Fails for non-extensible objects and for cycles.
     * <p>
     * [[SetPrototypeOf]] 내부 메서드(9.1.2). 확장 불가 객체나 순환이면 실패한다.
     *
     * @param proto new prototype or null / 새 프로토타입 또는 null
     * @return true on success / 성공하면 true
     */
    public boolean setPrototypeOf(JsObject proto) {
        if (proto == prototype) {
            return true;
        }
        if (!extensible) {
            return false;
        }
        JsObject p = proto;
        // 새 프로토타입 체인에 자기 자신이 있으면 순환이므로 거부한다.
        while (p != null) {
            if (p == this) {
                return false;
            }
            p = p.prototype;
        }
        prototype = proto;
        return true;
    }

    /**
     * The [[IsExtensible]] internal method.
     * <p>
     * [[IsExtensible]] 내부 메서드.
     *
     * @return true if extensible / 확장 가능하면 true
     */
    public boolean isExtensible() {
        return extensible;
    }

    /**
     * The [[PreventExtensions]] internal method.
     * <p>
     * [[PreventExtensions]] 내부 메서드.
     *
     * @return true on success / 성공하면 true
     */
    public boolean preventExtensions() {
        extensible = false;
        return true;
    }

    // ------------------------------------------------------------------ own properties

    /**
     * Returns the stored own property for a key, or null if there is none. Exotic objects override this to expose
     * virtual properties (array elements, string indices, array length). The returned object may be a temporary
     * snapshot for exotic objects; callers must not rely on mutating it.
     * <p>
     * 키에 해당하는 자체 프로퍼티를 반환한다. 없으면 null. 이국적 객체는 가상 프로퍼티(배열 요소, 문자열 인덱스, 배열 length)를
     * 보이도록 이 메서드를 재정의한다. 이국적 객체가 돌려주는 값은 임시 사본일 수 있으므로 호출자는 이를 수정해서는 안 된다.
     *
     * @param key the property key / 프로퍼티 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        return properties == null ? null : properties.get(key);
    }

    /**
     * The [[GetOwnProperty]] internal method.
     * <p>
     * [[GetOwnProperty]] 내부 메서드.
     *
     * @param key the property key / 프로퍼티 키
     * @return a complete descriptor or null / 완전 기술자 또는 null
     */
    public PropertyDescriptor getOwnProperty(Object key) {
        Property p = getOwnPropertySlot(key);
        return p == null ? null : p.toDescriptor();
    }

    /**
     * The [[DefineOwnProperty]] internal method (9.1.6, ValidateAndApplyPropertyDescriptor).
     * <p>
     * [[DefineOwnProperty]] 내부 메서드(9.1.6, ValidateAndApplyPropertyDescriptor).
     *
     * @param key  the property key / 프로퍼티 키
     * @param desc the descriptor / 기술자
     * @return true on success, false if the definition is rejected / 성공하면 true, 거부되면 false
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        Property current = properties == null ? null : properties.get(key);
        return validateAndApply(key, desc, current);
    }

    /**
     * Implements ValidateAndApplyPropertyDescriptor for properties stored in the table. New properties are stored with
     * {@link #storeProperty(Object, Property)}.
     * <p>
     * 프로퍼티 표에 저장되는 프로퍼티에 대한 ValidateAndApplyPropertyDescriptor 구현. 새 프로퍼티는
     * {@link #storeProperty(Object, Property)} 로 저장한다.
     *
     * @param key     the key / 키
     * @param d       the descriptor / 기술자
     * @param current the current property or null / 현재 프로퍼티 또는 null
     * @return true on success / 성공하면 true
     */
    protected final boolean validateAndApply(Object key, PropertyDescriptor d, Property current) {
        if (current == null) {
            // 새 프로퍼티: 확장 가능한 객체에서만 만들 수 있다.
            if (!isExtensible()) {
                return false;
            }
            Property np = new Property();
            if (d.isAccessor()) {
                np.accessor = true;
                np.value = null;
                np.getter = d.get instanceof JsObject ? (JsObject) d.get : null;
                np.setter = d.set instanceof JsObject ? (JsObject) d.set : null;
            } else {
                np.value = d.value != null ? d.value : Js.UNDEFINED;
                np.writable = d.isWritableTrue();
            }
            np.enumerable = d.isEnumerableTrue();
            np.configurable = d.isConfigurableTrue();
            storeProperty(key, np);
            return true;
        }
        if (d.isEmpty()) {
            return true;
        }
        if (!current.configurable) {
            // 설정 불가 프로퍼티는 거의 변경할 수 없다.
            if (d.isConfigurableTrue()) {
                return false;
            }
            if (d.enumerable != null && d.enumerable.booleanValue() != current.enumerable) {
                return false;
            }
            if (!d.isGeneric() && d.isAccessor() != current.accessor) {
                return false;
            }
            if (current.accessor) {
                if (d.get != null && !Ops.sameValue(d.get, current.getter == null ? Js.UNDEFINED : current.getter)) {
                    return false;
                }
                if (d.set != null && !Ops.sameValue(d.set, current.setter == null ? Js.UNDEFINED : current.setter)) {
                    return false;
                }
            } else if (!current.writable) {
                if (d.isWritableTrue()) {
                    return false;
                }
                if (d.value != null && !Ops.sameValue(d.value, current.value)) {
                    return false;
                }
            }
        }
        // 데이터 <-> 접근자 변환: enumerable, configurable 은 유지한다.
        if (!d.isGeneric() && d.isAccessor() != current.accessor) {
            if (current.accessor) {
                current.accessor = false;
                current.getter = null;
                current.setter = null;
                current.value = Js.UNDEFINED;
                current.writable = false;
            } else {
                current.accessor = true;
                current.value = null;
                current.writable = false;
                current.getter = null;
                current.setter = null;
            }
        }
        if (d.value != null) {
            current.value = d.value;
        }
        if (d.writable != null) {
            current.writable = d.writable.booleanValue();
        }
        if (d.get != null) {
            current.getter = d.get instanceof JsObject ? (JsObject) d.get : null;
        }
        if (d.set != null) {
            current.setter = d.set instanceof JsObject ? (JsObject) d.set : null;
        }
        if (d.enumerable != null) {
            current.enumerable = d.enumerable.booleanValue();
        }
        if (d.configurable != null) {
            current.configurable = d.configurable.booleanValue();
        }
        return true;
    }

    /**
     * Stores a new property in the table (creation order is kept).
     * <p>
     * 새 프로퍼티를 표에 저장한다(생성 순서 유지).
     *
     * @param key the key / 키
     * @param p   the property / 프로퍼티
     */
    protected void storeProperty(Object key, Property p) {
        if (properties == null) {
            properties = new LinkedHashMap<Object, Property>();
        }
        properties.put(key, p);
    }

    /**
     * Removes a property from the table.
     * <p>
     * 표에서 프로퍼티를 제거한다.
     *
     * @param key the key / 키
     */
    protected void removeProperty(Object key) {
        if (properties != null) {
            properties.remove(key);
        }
    }

    /**
     * The [[HasProperty]] internal method (9.1.7).
     * <p>
     * [[HasProperty]] 내부 메서드(9.1.7).
     *
     * @param key the key / 키
     * @return true if the object or its prototype chain has the property / 객체나 프로토타입 체인에 있으면 true
     */
    public boolean hasProperty(Object key) {
        if (getOwnPropertySlot(key) != null) {
            return true;
        }
        JsObject parent = getPrototypeOf();
        return parent != null && parent.hasProperty(key);
    }

    /**
     * Returns true if the object has an own property for the key.
     * <p>
     * 객체가 해당 키의 자체 프로퍼티를 가지면 true.
     *
     * @param key the key / 키
     * @return true if own property exists / 자체 프로퍼티가 있으면 true
     */
    public boolean hasOwnProperty(Object key) {
        return getOwnPropertySlot(key) != null;
    }

    /**
     * The [[Get]] internal method (9.1.8).
     * <p>
     * [[Get]] 내부 메서드(9.1.8).
     *
     * @param key      the key / 키
     * @param receiver the receiver used as this for getters / getter 의 this 로 쓰이는 receiver
     * @return the value or undefined / 값 또는 undefined
     */
    public JsValue get(Object key, JsValue receiver) {
        Property p = getOwnPropertySlot(key);
        if (p == null) {
            JsObject parent = getPrototypeOf();
            if (parent == null) {
                return Js.UNDEFINED;
            }
            return parent.get(key, receiver);
        }
        if (!p.accessor) {
            return p.value;
        }
        if (p.getter == null) {
            return Js.UNDEFINED;
        }
        return p.getter.call(receiver, Js.NO_ARGS);
    }

    /**
     * Gets a property with this object as receiver.
     * <p>
     * 이 객체를 receiver 로 프로퍼티를 읽는다.
     *
     * @param key the key (String or JsSymbol) / 키(String 또는 JsSymbol)
     * @return the value or undefined / 값 또는 undefined
     */
    public final JsValue get(Object key) {
        return get(key, this);
    }

    /**
     * The [[Set]] internal method (OrdinarySet, 9.1.9).
     * <p>
     * [[Set]] 내부 메서드(OrdinarySet, 9.1.9).
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / receiver
     * @return false if the assignment is rejected (strict code throws a TypeError) / 거부되면 false(엄격 모드 코드는 TypeError)
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        Property own = getOwnPropertySlot(key);
        if (own == null) {
            JsObject parent = getPrototypeOf();
            if (parent != null) {
                return parent.set(key, value, receiver);
            }
            own = Property.data(Js.UNDEFINED, true, true, true);
        }
        if (!own.accessor) {
            if (!own.writable) {
                return false;
            }
            if (!(receiver instanceof JsObject)) {
                return false;
            }
            JsObject r = (JsObject) receiver;
            // receiver 에 이미 같은 키가 있으면 값만 바꾸고, 없으면 새 데이터 프로퍼티를 만든다.
            PropertyDescriptor existing = r.getOwnProperty(key);
            if (existing != null) {
                if (existing.isAccessor()) {
                    return false;
                }
                if (!existing.isWritableTrue()) {
                    return false;
                }
                return r.defineOwnProperty(key, PropertyDescriptor.valueOnly(value));
            }
            return r.defineOwnProperty(key, PropertyDescriptor.data(value, true, true, true));
        }
        if (own.setter == null) {
            return false;
        }
        own.setter.call(receiver, new JsValue[] { value });
        return true;
    }

    /**
     * Sets a property with this object as receiver, ignoring rejection.
     * <p>
     * 이 객체를 receiver 로 프로퍼티를 설정한다. 거부되어도 무시한다.
     *
     * @param key   the key / 키
     * @param value the value / 값
     * @return true if the assignment succeeded / 성공하면 true
     */
    public final boolean set(Object key, JsValue value) {
        return set(key, value, this);
    }

    /**
     * Sets a property and throws a TypeError if the assignment is rejected (strict mode semantics).
     * <p>
     * 프로퍼티를 설정하고 거부되면 TypeError 를 던진다(엄격 모드 의미).
     *
     * @param key   the key / 키
     * @param value the value / 값
     */
    public final void setStrict(Object key, JsValue value) {
        if (!set(key, value, this)) {
            throw JsException.typeError("Cannot assign to read only property '" + PropertyKeys.describe(key) + "' of object");
        }
    }

    /**
     * The [[Delete]] internal method (9.1.10).
     * <p>
     * [[Delete]] 내부 메서드(9.1.10).
     *
     * @param key the key / 키
     * @return true if the property is gone afterwards / 삭제 후 프로퍼티가 없으면 true
     */
    public boolean delete(Object key) {
        Property p = getOwnPropertySlot(key);
        if (p == null) {
            return true;
        }
        if (p.configurable) {
            removeProperty(key);
            return true;
        }
        return false;
    }

    /**
     * The [[OwnPropertyKeys]] internal method (9.1.12): indices ascending, strings, then symbols.
     * <p>
     * [[OwnPropertyKeys]] 내부 메서드(9.1.12): 인덱스 오름차순, 문자열, 심볼 순서.
     *
     * @return a new list of keys / 키의 새 목록
     */
    public List<Object> ownKeys() {
        List<Object> result = new ArrayList<Object>();
        if (properties == null || properties.isEmpty()) {
            return result;
        }
        List<Long> indices = null;
        List<Object> symbols = null;
        // 키를 인덱스, 문자열, 심볼로 나눈다.
        for (Object k : properties.keySet()) {
            if (k instanceof String) {
                long idx = PropertyKeys.arrayIndex((String) k);
                if (idx >= 0) {
                    if (indices == null) {
                        indices = new ArrayList<Long>();
                    }
                    indices.add(Long.valueOf(idx));
                } else {
                    result.add(k);
                }
            } else {
                if (symbols == null) {
                    symbols = new ArrayList<Object>();
                }
                symbols.add(k);
            }
        }
        if (indices != null) {
            Collections.sort(indices);
            List<Object> ordered = new ArrayList<Object>(indices.size() + result.size());
            for (int i = 0; i < indices.size(); i++) { // 정렬된 인덱스를 문자열 키로 바꿔 앞에 둔다.
                ordered.add(PropertyKeys.fromIndex(indices.get(i).longValue()));
            }
            ordered.addAll(result);
            result = ordered;
        }
        if (symbols != null) {
            result.addAll(symbols);
        }
        return result;
    }

    // ------------------------------------------------------------------ convenience helpers for built-in authors

    /**
     * CreateDataProperty (7.3.4): defines a writable, enumerable, configurable data property.
     * <p>
     * CreateDataProperty (7.3.4): writable, enumerable, configurable 인 데이터 프로퍼티를 정의한다.
     *
     * @param key   the key / 키
     * @param value the value / 값
     * @return true on success / 성공하면 true
     */
    public final boolean createDataProperty(Object key, JsValue value) {
        return defineOwnProperty(key, PropertyDescriptor.data(value, true, true, true));
    }

    /**
     * CreateDataPropertyOrThrow (7.3.6).
     * <p>
     * CreateDataPropertyOrThrow (7.3.6).
     *
     * @param key   the key / 키
     * @param value the value / 값
     */
    public final void createDataPropertyOrThrow(Object key, JsValue value) {
        if (!createDataProperty(key, value)) {
            throw JsException.typeError("Cannot define property " + PropertyKeys.describe(key));
        }
    }

    /**
     * DefinePropertyOrThrow (7.3.7).
     * <p>
     * DefinePropertyOrThrow (7.3.7).
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     */
    public final void definePropertyOrThrow(Object key, PropertyDescriptor desc) {
        if (!defineOwnProperty(key, desc)) {
            throw JsException.typeError("Cannot redefine property: " + PropertyKeys.describe(key));
        }
    }

    /**
     * Defines a built-in style property: writable, non-enumerable, configurable (the attributes of
     * standard methods and constructors). Meant for setting up built-ins; the object must be extensible.
     * <p>
     * 내장 객체 스타일 프로퍼티를 정의한다: writable, 열거 불가, configurable (표준 메서드와 생성자의 속성).
     * 내장 객체를 구성할 때 쓰며 객체는 확장 가능해야 한다.
     *
     * @param key   the key / 키
     * @param value the value / 값
     */
    public final void defineBuiltin(Object key, JsValue value) {
        defineOwnProperty(key, PropertyDescriptor.data(value, true, false, true));
    }

    /**
     * Defines a data property with explicit attributes.
     * <p>
     * 속성을 직접 지정해 데이터 프로퍼티를 정의한다.
     *
     * @param key          the key / 키
     * @param value        the value / 값
     * @param writable     writable / writable
     * @param enumerable   enumerable / enumerable
     * @param configurable configurable / configurable
     */
    public final void defineData(Object key, JsValue value, boolean writable, boolean enumerable, boolean configurable) {
        defineOwnProperty(key, PropertyDescriptor.data(value, writable, enumerable, configurable));
    }

    /**
     * Defines an accessor property with explicit attributes.
     * <p>
     * 속성을 직접 지정해 접근자 프로퍼티를 정의한다.
     *
     * @param key          the key / 키
     * @param getter       getter function or null / getter 함수 또는 null
     * @param setter       setter function or null / setter 함수 또는 null
     * @param enumerable   enumerable / enumerable
     * @param configurable configurable / configurable
     */
    public final void defineAccessor(Object key, JsObject getter, JsObject setter, boolean enumerable, boolean configurable) {
        defineOwnProperty(key, PropertyDescriptor.accessor(getter, setter, enumerable, configurable));
    }

    /**
     * Returns the value of an own or inherited data/accessor property as a Java string if it is a string, else null.
     * Convenience for tests and host code; does not convert types.
     * <p>
     * 자체 또는 상속 프로퍼티의 값이 문자열이면 Java 문자열로 반환하고 아니면 null 을 반환한다. 시험과 호스트 코드용 편의 메서드이며
     * 형 변환은 하지 않는다.
     *
     * @param key the key / 키
     * @return string or null / 문자열 또는 null
     */
    public final String getString(Object key) {
        JsValue v = get(key);
        return v instanceof JsString ? ((JsString) v).getValue() : null;
    }

    /**
     * Returns a short description for debugging.
     * <p>
     * 디버깅용 짧은 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        return "[object " + className() + "]";
    }
}
