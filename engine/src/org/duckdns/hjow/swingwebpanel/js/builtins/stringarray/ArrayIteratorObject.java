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

/**
 * An Array Iterator object (ECMA-262 6th edition 22.1.5): it walks the indexed elements of any array-like object and yields keys, values or
 * [key, value] pairs. {@link #create(JsRealm, JsObject, int)} is public so that other built-ins with array-like semantics (typed arrays,
 * arguments objects) can create iterators with the same prototype <code>%ArrayIteratorPrototype%</code>. The prototype is created and registered
 * under that intrinsic name by {@link StringArrayInstaller}; an object created by it is the only kind of object that the prototype's
 * <code>next</code> accepts.
 * <p>
 * Array Iterator 객체(ECMA-262 6판 22.1.5): 임의의 유사 배열 객체의 인덱스 요소를 걸으며 키, 값, [키, 값] 쌍을 돌려준다.
 * {@link #create(JsRealm, JsObject, int)} 는 public 이므로 유사 배열 의미를 가진 다른 내장 객체(타입 배열, arguments 객체)도 같은 프로토타입
 * <code>%ArrayIteratorPrototype%</code> 의 이터레이터를 만들 수 있다. 프로토타입은 {@link StringArrayInstaller} 가 만들어 그 내장 객체
 * 이름으로 등록하며, 프로토타입의 <code>next</code> 는 이 클래스로 만든 객체만 받아들인다.
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
     * Installs <code>%ArrayIteratorPrototype%</code> with its <code>next</code> method and <code>@@toStringTag</code>. An existing
     * prototype of the same intrinsic name (from a minimal core implementation) is replaced, because the iterator objects created by this
     * class are only understood by the prototype defined here.
     * <p>
     * <code>next</code> 메서드와 <code>@@toStringTag</code> 를 가진 <code>%ArrayIteratorPrototype%</code> 을 설치한다. 같은 이름의 기존
     * 프로토타입(코어의 최소 구현)은 교체한다. 이 클래스가 만든 이터레이터 객체는 여기서 정의한 프로토타입만 이해하기 때문이다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
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
        double len = Ops.lengthOf(target);
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
