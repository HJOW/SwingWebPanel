package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;

/**
 * An Array Iterator object (ECMA-262 6th edition 22.1.5): it walks the indexed elements of any array-like object and yields keys, values or
 * [key, value] pairs. Arrays, typed arrays and arguments objects share the same <code>%ArrayIteratorPrototype%</code>.
 * <p>
 * Array Iterator 객체(ECMA-262 6판 22.1.5): 임의의 유사 배열 객체의 인덱스 요소를 걸으며 키, 값, [키, 값] 쌍을 돌려준다.
 * 배열, 형식화 배열, arguments 객체가 같은 <code>%ArrayIteratorPrototype%</code>을 공유한다.
 */
public final class ArrayIteratorObject extends JsObject {

    /**
     * Name of the intrinsic that holds the prototype.
     * <p>
     * 프로토타입을 담는 내장 객체 이름.
     */
    public static final String PROTOTYPE_NAME = "%ArrayIteratorPrototype%";

    /**
     * Kind: yield the keys (indices).
     * <p>
     * 종류: 키(인덱스)를 돌려준다.
     */
    public static final int KEYS = 0;

    /**
     * Kind: yield the values.
     * <p>
     * 종류: 값을 돌려준다.
     */
    public static final int VALUES = 1;

    /**
     * Kind: yield [key, value] pairs.
     * <p>
     * 종류: [키, 값] 쌍을 돌려준다.
     */
    public static final int ENTRIES = 2;

    /**
     * The iterated object, or null once the iterator is exhausted.
     * <p>
     * 순회 대상 객체. 이터레이터가 끝나면 null.
     */
    private JsObject target;

    /**
     * Next index.
     * <p>
     * 다음 인덱스.
     */
    private double index;

    /**
     * One of {@link #KEYS}, {@link #VALUES}, {@link #ENTRIES}.
     * <p>
     * {@link #KEYS}, {@link #VALUES}, {@link #ENTRIES} 중 하나.
     */
    private final int kind;

    /**
     * Creates an iterator object.
     * <p>
     * 이터레이터 객체를 만든다.
     *
     * @param proto  the prototype / 프로토타입
     * @param target the iterated object / 순회 대상
     * @param kind   what to yield / 돌려줄 것
     */
    private ArrayIteratorObject(JsObject proto, JsObject target, int kind) {
        super(proto);
        this.target = target;
        this.kind = kind;
    }

    /**
     * Returns the class name for Object.prototype.toString.
     * <p>
     * Object.prototype.toString 용 클래스 이름을 돌려준다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Array Iterator";
    }

    /**
     * Creates an Array Iterator (CreateArrayIterator).
     * <p>
     * Array Iterator 를 만든다(CreateArrayIterator).
     *
     * @param realm  the realm / realm
     * @param target the object whose indexed elements are walked / 인덱스 요소를 걸을 객체
     * @param kind   {@link #KEYS}, {@link #VALUES} or {@link #ENTRIES} / 돌려줄 것
     * @return the iterator object / 이터레이터 객체
     */
    public static JsObject create(JsRealm realm, JsObject target, int kind) {
        return new ArrayIteratorObject(realm.getIntrinsic(PROTOTYPE_NAME), target, kind);
    }

    /**
     * Installs the shared <code>%ArrayIteratorPrototype%</code> if the realm has not yet created it.
     * <p>
     * 렐름에 아직 없으면 공용 <code>%ArrayIteratorPrototype%</code>을 설치한다.
     *
     * @param realm the realm / realm
     */
    public static void install(JsRealm realm) {
        if (realm.getIntrinsic(PROTOTYPE_NAME) != null) {
            return;
        }
        JsObject proto = new JsObject(realm.getIteratorPrototype());
        Support.def(realm, proto, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof ArrayIteratorObject)) {
                    throw JsException.typeError("next method called on incompatible receiver (not an Array Iterator)");
                }
                return ((ArrayIteratorObject) thisValue).step(r);
            }
        });
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(Js.str("Array Iterator"), false, false, true));
        realm.setIntrinsic(PROTOTYPE_NAME, proto);
    }

    /**
     * Produces the next iterator result (%ArrayIteratorPrototype%.next).
     * <p>
     * 다음 이터레이터 결과를 만든다(%ArrayIteratorPrototype%.next).
     *
     * @param realm the realm / realm
     * @return the result object / 결과 객체
     */
    private JsValue step(JsRealm realm) {
        if (target == null) {
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        double len;
        // 형식화 배열은 length 프로퍼티 대신 현재 버퍼와 뷰의 내부 길이를 확인한다.
        if (target instanceof TypedArrayObject) {
            TypedArrayObject typed = (TypedArrayObject) target;
            if (typed.isOutOfBounds()) {
                throw JsException.typeError("TypedArray iterator is outside its buffer");
            }
            len = typed.getLength();
        } else {
            len = Ops.lengthOf(target);
        }
        if (index >= len) {
            target = null;
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        double i = index;
        index = i + 1;
        if (kind == KEYS) {
            return IteratorHelper.createResult(realm, Js.num(i), false);
        }
        JsValue element = Support.getAt(target, i);
        if (kind == VALUES) {
            return IteratorHelper.createResult(realm, element, false);
        }
        return IteratorHelper.createResult(realm, realm.newArray(new JsValue[] { Js.num(i), element }), false);
    }
}
