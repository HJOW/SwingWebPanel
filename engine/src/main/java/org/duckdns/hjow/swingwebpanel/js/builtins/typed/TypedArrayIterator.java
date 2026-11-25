package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The iterator object returned by <code>keys()</code>, <code>values()</code>, <code>entries()</code> and <code>[Symbol.iterator]()</code> of a
 * typed array (an "Array Iterator" in ECMAScript 2015, 22.1.5). It lists the indices, the elements or [index, element] pairs and ends when
 * the index reaches the length of the array. Assumption/deviation: it has its own prototype object <code>%TypedArrayIteratorPrototype%</code>
 * (which inherits <code>%IteratorPrototype%</code>, has a <code>next</code> method and the toStringTag "Array Iterator") instead of sharing
 * <code>%ArrayIteratorPrototype%</code> with arrays, because the core does not yet offer a way to create its array iterators for other objects;
 * the two prototypes can be told apart by script (<code>Object.getPrototypeOf</code>) but behave identically.
 * <p>
 * 형식화 배열의 <code>keys()</code>, <code>values()</code>, <code>entries()</code>, <code>[Symbol.iterator]()</code> 가 돌려주는 이터레이터 객체
 * (ECMAScript 2015 의 "Array Iterator", 22.1.5). 인덱스, 요소, [인덱스, 요소] 쌍을 나열하고 인덱스가 배열 길이에 닿으면 끝난다.
 * 가정/차이: 배열과 <code>%ArrayIteratorPrototype%</code> 을 공유하지 않고 자기만의 프로토타입 객체 <code>%TypedArrayIteratorPrototype%</code>
 * (<code>%IteratorPrototype%</code> 을 상속하고, <code>next</code> 메서드와 toStringTag "Array Iterator" 를 가짐)를 쓴다. 코어가 다른 객체용 배열
 * 이터레이터를 만드는 방법을 아직 제공하지 않기 때문이다. 두 프로토타입은 스크립트에서(<code>Object.getPrototypeOf</code>) 구별되지만 동작은 같다.
 */
final class TypedArrayIterator extends JsObject {

    /**
     * Kind: the iterator gives indices.
     * <p>
     * 종류: 인덱스를 내놓는다.
     */
    static final int KEYS = 0;

    /**
     * Kind: the iterator gives elements.
     * <p>
     * 종류: 요소를 내놓는다.
     */
    static final int VALUES = 1;

    /**
     * Kind: the iterator gives [index, element] pairs.
     * <p>
     * 종류: [인덱스, 요소] 쌍을 내놓는다.
     */
    static final int ENTRIES = 2;

    /**
     * The typed array being iterated, or null once the iteration is finished.
     * <p>
     * 순회 중인 형식화 배열. 순회가 끝나면 null.
     */
    private TypedArrayObject target;

    /**
     * The next index.
     * <p>
     * 다음 인덱스.
     */
    private int index;

    /**
     * What to produce (KEYS, VALUES or ENTRIES).
     * <p>
     * 무엇을 내놓을지(KEYS, VALUES, ENTRIES).
     */
    private final int kind;

    /**
     * Creates an iterator.
     * <p>
     * 이터레이터를 만든다.
     *
     * @param prototype the iterator prototype / 이터레이터 프로토타입
     * @param target    the typed array / 형식화 배열
     * @param kind      KEYS, VALUES or ENTRIES / KEYS, VALUES, ENTRIES
     */
    TypedArrayIterator(JsObject prototype, TypedArrayObject target, int kind) {
        super(prototype);
        this.target = target;
        this.kind = kind;
    }

    /**
     * Creates an iterator over a typed array.
     * <p>
     * 형식화 배열에 대한 이터레이터를 만든다.
     *
     * @param realm  the realm / realm
     * @param array  the typed array / 형식화 배열
     * @param kind   KEYS, VALUES or ENTRIES / KEYS, VALUES, ENTRIES
     * @return the iterator / 이터레이터
     */
    static JsObject create(JsRealm realm, TypedArrayObject array, int kind) {
        return new TypedArrayIterator(realm.getIntrinsic("%TypedArrayIteratorPrototype%"), array, kind);
    }

    /**
     * Installs <code>%TypedArrayIteratorPrototype%</code>.
     * <p>
     * <code>%TypedArrayIteratorPrototype%</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject proto = new JsObject(realm.getIteratorPrototype());
        realm.setIntrinsic("%TypedArrayIteratorPrototype%", proto);
        realm.method(proto, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof TypedArrayIterator)) {
                    throw JsException.typeError("next method called on an incompatible receiver");
                }
                return ((TypedArrayIterator) thisValue).step(r);
            }
        });
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("Array Iterator"), false, false, true);
    }

    /**
     * Produces the next iterator result (22.1.5.2.1 %ArrayIteratorPrototype%.next).
     * <p>
     * 다음 이터레이터 결과를 만든다(22.1.5.2.1 %ArrayIteratorPrototype%.next).
     *
     * @param realm the realm / realm
     * @return the result object / 결과 객체
     */
    private JsValue step(JsRealm realm) {
        TypedArrayObject a = target;
        if (a == null) {
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        if (a.getBuffer().isDetached()) {
            throw JsException.typeError("typed array buffer is detached");
        }
        int i = index;
        if (i >= a.getLength()) {
            target = null;
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        index = i + 1;
        if (kind == KEYS) {
            return IteratorHelper.createResult(realm, Js.num(i), false);
        }
        JsValue element = a.getElementValue(i);
        if (kind == VALUES) {
            return IteratorHelper.createResult(realm, element, false);
        }
        return IteratorHelper.createResult(realm, realm.newArray(new JsValue[] { Js.num(i), element }), false);
    }
}
