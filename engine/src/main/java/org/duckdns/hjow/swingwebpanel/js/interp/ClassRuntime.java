package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;

/**
 * Runtime storage for class fields and private elements. It is separate from ordinary object properties so private names,
 * brand checks and static private storage cannot be observed through property reflection.
 * <p>
 * 클래스 필드와 private 요소의 실행 저장소이다. 일반 객체 프로퍼티와 분리되어 private 이름, brand 검사,
 * static private 저장소를 프로퍼티 리플렉션으로 관찰할 수 없다.
 */
final class ClassRuntime {

    /**
     * Private slot values of live objects, indexed by object identity and class brand.
     * <p>
     * 살아 있는 객체의 private 슬롯 값. 객체와 클래스 brand 로 찾는다.
     */
    private static final Map<JsObject, Map<Brand, Map<String, JsValue>>> INSTANCES =
            new WeakHashMap<JsObject, Map<Brand, Map<String, JsValue>>>();

    /**
     * No instances.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private ClassRuntime() {
    }

    /**
     * One class private environment and its ordered instance fields.
     * <p>
     * 클래스 하나의 private 환경과 순서가 있는 인스턴스 필드.
     */
    static final class Brand {

        /**
         * Enclosing lexical class brand.
         * <p>
         * 바깥 렉시컬 클래스 brand.
         */
        final Brand parent;

        /**
         * Private declarations owned by this class.
         * <p>
         * 이 클래스가 소유한 private 선언.
         */
        final Map<String, PrivateDefinition> privates = new HashMap<String, PrivateDefinition>();

        /**
         * Instance field definitions in source order.
         * <p>
         * 소스 순서의 인스턴스 필드 정의.
         */
        final List<Field> fields = new ArrayList<Field>();

        /**
         * Class constructor that owns static private slots.
         * <p>
         * static private 슬롯을 소유하는 클래스 생성자.
         */
        ScriptFunction constructor;

        /**
         * Creates a brand.
         * <p>
         * brand 를 만든다.
         *
         * @param parent outer lexical brand / 바깥 brand
         */
        Brand(Brand parent) {
            this.parent = parent;
        }

        /**
         * Resolves a private name through the lexical class chain.
         * <p>
         * 렉시컬 클래스 체인에서 private 이름을 해석한다.
         *
         * @param name private name / private 이름
         * @return owning brand, or null / 소유 brand. 없으면 null
         */
        Brand resolve(String name) {
            Brand b = this;
            // 안쪽 클래스부터 바깥 클래스까지 같은 private 이름 선언을 찾는다.
            while (b != null) {
                if (b.privates.containsKey(name)) {
                    return b;
                }
                b = b.parent;
            }
            return null;
        }
    }

    /**
     * One evaluated instance field definition.
     * <p>
     * 평가가 끝난 인스턴스 필드 정의 하나.
     */
    static final class Field {

        /**
         * Evaluated string/symbol key, or private name.
         * <p>
         * 평가한 문자열/심볼 키 또는 private 이름.
         */
        final Object key;

        /**
         * True for a private field.
         * <p>
         * private 필드이면 true.
         */
        final boolean privateField;

        /**
         * Initializer expression, or null.
         * <p>
         * 초기화 식. 없으면 null.
         */
        final Node initializer;

        /**
         * Creates a field definition.
         * <p>
         * 필드 정의를 만든다.
         *
         * @param key          evaluated key / 평가한 키
         * @param privateField true for private / private 이면 true
         * @param initializer  initializer / 초기화 식
         */
        Field(Object key, boolean privateField, Node initializer) {
            this.key = key;
            this.privateField = privateField;
            this.initializer = initializer;
        }
    }

    /**
     * One private declaration and its shared runtime state.
     * <p>
     * private 선언 하나와 공유 실행 상태.
     */
    static final class PrivateDefinition {

        /**
         * True for static storage.
         * <p>
         * static 저장소이면 true.
         */
        final boolean staticElement;

        /**
         * Field value for static fields.
         * <p>
         * static 필드 값.
         */
        JsValue staticValue = Js.UNDEFINED;

        /**
         * Method, getter or field-present marker.
         * <p>
         * 메서드, getter 또는 필드 존재 표시.
         */
        ScriptFunction getter;

        /**
         * Setter function.
         * <p>
         * setter 함수.
         */
        ScriptFunction setter;

        /**
         * Private method function.
         * <p>
         * private 메서드 함수.
         */
        ScriptFunction method;

        /**
         * True for a private data field.
         * <p>
         * private 데이터 필드이면 true.
         */
        boolean field;

        /**
         * Creates a declaration.
         * <p>
         * 선언을 만든다.
         *
         * @param staticElement true for static / static 이면 true
         */
        PrivateDefinition(boolean staticElement) {
            this.staticElement = staticElement;
        }
    }

    /**
     * Creates or returns an instance slot map and records the class brand.
     * <p>
     * 인스턴스 슬롯 표를 만들거나 반환하고 클래스 brand 를 기록한다.
     *
     * @param object receiver / 수신자
     * @param brand  class brand / 클래스 brand
     * @return slot map / 슬롯 표
     */
    static Map<String, JsValue> enter(JsObject object, Brand brand) {
        Map<Brand, Map<String, JsValue>> brands = INSTANCES.get(object);
        if (brands == null) {
            brands = new IdentityHashMap<Brand, Map<String, JsValue>>();
            INSTANCES.put(object, brands);
        }
        Map<String, JsValue> slots = brands.get(brand);
        if (slots == null) {
            slots = new HashMap<String, JsValue>();
            brands.put(brand, slots);
        }
        return slots;
    }

    /**
     * Tests the private in operator.
     * <p>
     * private in 연산자를 검사한다.
     *
     * @param active active class brand / 실행 중 클래스 brand
     * @param name   private name / private 이름
     * @param value  candidate receiver / 후보 수신자
     * @return true when the brand is present / brand 가 있으면 true
     */
    static boolean has(Brand active, String name, JsValue value) {
        Brand owner = resolve(active, name);
        PrivateDefinition def = owner.privates.get(name);
        if (def.staticElement) {
            return value == owner.constructor;
        }
        if (!(value instanceof JsObject)) {
            return false;
        }
        Map<Brand, Map<String, JsValue>> brands = INSTANCES.get(value);
        return brands != null && brands.containsKey(owner);
    }

    /**
     * Reads a private field, method or accessor.
     * <p>
     * private 필드, 메서드, 접근자를 읽는다.
     *
     * @param active active brand / 실행 중 brand
     * @param name   private name / private 이름
     * @param receiver receiver / 수신자
     * @param callThis this value for a getter / getter this 값
     * @return value / 값
     */
    static JsValue get(Brand active, String name, JsValue receiver, JsValue callThis) {
        Brand owner = resolve(active, name);
        PrivateDefinition def = owner.privates.get(name);
        checkBrand(owner, def, receiver);
        if (def.getter != null) {
            return def.getter.call(callThis, new JsValue[0]);
        }
        if (def.method != null) {
            return def.method;
        }
        if (def.field) {
            if (def.staticElement) {
                return def.staticValue;
            }
            return slots((JsObject) receiver, owner).get(name);
        }
        return Js.UNDEFINED;
    }

    /**
     * Writes a private field or calls a private setter.
     * <p>
     * private 필드를 쓰거나 setter 를 호출한다.
     *
     * @param active active brand / 실행 중 brand
     * @param name private name / private 이름
     * @param receiver receiver / 수신자
     * @param callThis this value / this 값
     * @param value value to write / 쓸 값
     */
    static void put(Brand active, String name, JsValue receiver, JsValue callThis, JsValue value) {
        Brand owner = resolve(active, name);
        PrivateDefinition def = owner.privates.get(name);
        checkBrand(owner, def, receiver);
        if (def.setter != null) {
            def.setter.call(callThis, new JsValue[] { value });
            return;
        }
        if (def.field) {
            if (def.staticElement) {
                def.staticValue = value;
            } else {
                slots((JsObject) receiver, owner).put(name, value);
            }
            return;
        }
        throw JsException.typeError("Private member '#" + name + "' is not writable");
    }

    /**
     * Returns the slot map of an already-branded object.
     * <p>
     * 이미 brand 가 있는 객체의 슬롯 표를 반환한다.
     */
    private static Map<String, JsValue> slots(JsObject object, Brand owner) {
        Map<Brand, Map<String, JsValue>> brands = INSTANCES.get(object);
        Map<String, JsValue> slots = brands == null ? null : brands.get(owner);
        if (slots == null) {
            throw brandError();
        }
        return slots;
    }

    /**
     * Checks a private receiver brand.
     * <p>
     * private 수신자의 brand 를 검사한다.
     */
    private static void checkBrand(Brand owner, PrivateDefinition def, JsValue receiver) {
        if (def.staticElement) {
            if (receiver != owner.constructor) {
                throw brandError();
            }
            return;
        }
        if (!(receiver instanceof JsObject)) {
            throw brandError();
        }
        Map<Brand, Map<String, JsValue>> brands = INSTANCES.get(receiver);
        if (brands == null || !brands.containsKey(owner)) {
            throw brandError();
        }
    }

    /**
     * Resolves a declaration or throws a TypeError.
     * <p>
     * 선언을 해석하고 없으면 TypeError 를 던진다.
     */
    private static Brand resolve(Brand active, String name) {
        Brand owner = active == null ? null : active.resolve(name);
        if (owner == null) {
            throw JsException.typeError("Private name '#" + name + "' is not declared in this class");
        }
        return owner;
    }

    /**
     * Creates the private brand TypeError.
     * <p>
     * private brand TypeError 를 만든다.
     */
    private static JsException brandError() {
        return JsException.typeError("Receiver is not branded for this private member");
    }
}

