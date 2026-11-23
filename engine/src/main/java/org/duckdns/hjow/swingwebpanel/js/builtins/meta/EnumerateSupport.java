package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * The [[Enumerate]] internal method of ordinary objects in ECMA-262 6th edition (9.1.11) and the for-in iterator object it
 * returns (13.7.5.9 %ForInIteratorPrototype%). It lists the enumerable string keys of an object and then of its prototype chain,
 * skipping keys that were already seen (even when the earlier property was not enumerable) and keys that were deleted while
 * iterating. The interpreter may use it for for-in over any object, <code>Reflect.enumerate</code> returns the same iterator.
 * <p>
 * ECMA-262 6판의 일반 객체 [[Enumerate]] 내부 메서드(9.1.11)와 그것이 돌려주는 for-in 이터레이터 객체(13.7.5.9
 * %ForInIteratorPrototype%). 객체와 그 프로토타입 체인의 열거 가능한 문자열 키를 나열하되, 이미 본 키(앞의 프로퍼티가 열거 불가여도)와
 * 순회 중 삭제된 키는 건너뛴다. 인터프리터가 모든 객체의 for-in 에 쓸 수 있고 <code>Reflect.enumerate</code> 도 같은 이터레이터를 돌려준다.
 */
public final class EnumerateSupport {

    /**
     * Name of the intrinsic that holds %ForInIteratorPrototype%.
     * <p>
     * %ForInIteratorPrototype% 을 담는 내장 객체 이름.
     */
    public static final String INTRINSIC_NAME = "%ForInIteratorPrototype%";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EnumerateSupport() {
    }

    /**
     * Returns an iterator over the enumerable string keys of an object and its prototype chain. A proxy answers through its
     * <code>enumerate</code> trap.
     * <p>
     * 객체와 프로토타입 체인의 열거 가능한 문자열 키에 대한 이터레이터를 반환한다. 프록시는 <code>enumerate</code> trap 으로 답한다.
     *
     * @param realm the realm / realm
     * @param o     the object / 객체
     * @return the iterator object / 이터레이터 객체
     */
    public static JsObject enumerate(JsRealm realm, JsObject o) {
        if (o instanceof ProxyObject) {
            return ((ProxyObject) o).enumerate();
        }
        return new ForInIterator(forInPrototype(realm), o);
    }

    /**
     * Returns (creating it on first use) %ForInIteratorPrototype%: it inherits %IteratorPrototype% and has a <code>next</code> method.
     * <p>
     * %ForInIteratorPrototype% 을 반환한다(처음 쓸 때 만든다). %IteratorPrototype% 을 상속하며 <code>next</code> 메서드를 가진다.
     *
     * @param realm the realm / realm
     * @return the prototype object / 프로토타입 객체
     */
    public static JsObject forInPrototype(JsRealm realm) {
        JsObject p = realm.getIntrinsic(INTRINSIC_NAME);
        if (p != null) {
            return p;
        }
        p = new JsObject(realm.getIteratorPrototype());
        realm.method(p, "next", 0, new org.duckdns.hjow.swingwebpanel.js.NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof ForInIterator)) {
                    throw JsException.typeError("next method called on incompatible receiver " + JsException.describe(thisValue));
                }
                String key = ((ForInIterator) thisValue).nextKey();
                if (key == null) {
                    return IteratorHelper.createResult(r, Js.UNDEFINED, true);
                }
                return IteratorHelper.createResult(r, Js.str(key), false);
            }
        });
        realm.setIntrinsic(INTRINSIC_NAME, p);
        return p;
    }

    /**
     * The for-in iterator object: it walks the prototype chain lazily and re-checks every key against the object that owns it.
     * <p>
     * for-in 이터레이터 객체: 프로토타입 체인을 느리게 따라가며 모든 키를 소유 객체에 다시 확인한다.
     */
    private static final class ForInIterator extends JsObject {
        /**
         * The object whose keys are being listed now.
         * <p>
         * 지금 키를 나열하는 객체.
         */
        private JsObject current;

        /**
         * Own keys of the current object (strings and symbols).
         * <p>
         * 현재 객체의 자체 키(문자열과 심볼).
         */
        private List<Object> keys;

        /**
         * Position in the key list.
         * <p>
         * 키 목록에서의 위치.
         */
        private int index;

        /**
         * String keys that were already visited (also non-enumerable ones, which shadow later keys).
         * <p>
         * 이미 본 문자열 키(뒤의 키를 가리는 열거 불가 키 포함).
         */
        private final Set<String> visited = new HashSet<String>();

        /**
         * Creates the iterator.
         * <p>
         * 이터레이터를 만든다.
         *
         * @param proto %ForInIteratorPrototype% / %ForInIteratorPrototype%
         * @param start the object to enumerate / 열거할 객체
         */
        ForInIterator(JsObject proto, JsObject start) {
            super(proto);
            this.current = start;
        }

        /**
         * Finds the next enumerable string key.
         * <p>
         * 다음 열거 가능한 문자열 키를 찾는다.
         *
         * @return the key, or null at the end / 키. 끝이면 null
         */
        String nextKey() {
            // 키가 나올 때까지, 또는 프로토타입 체인이 끝날 때까지 계속 찾는다.
            while (current != null) {
                if (keys == null) {
                    keys = current.ownKeys();
                    index = 0;
                }
                if (index >= keys.size()) {
                    current = current.getPrototypeOf();
                    keys = null;
                    continue;
                }
                Object k = keys.get(index++);
                if (!(k instanceof String)) {
                    continue;
                }
                String key = (String) k;
                if (visited.contains(key)) {
                    continue;
                }
                PropertyDescriptor d = current.getOwnProperty(key);
                if (d == null) {
                    continue;
                }
                visited.add(key);
                if (d.isEnumerableTrue()) {
                    return key;
                }
            }
            return null;
        }
    }
}
