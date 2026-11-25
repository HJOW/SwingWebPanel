package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.IteratorRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The global Iterator constructor and its native wrapper for non-native iterators.
 * <p>
 * 전역 Iterator 생성자와 사용자 정의 이터레이터를 감싸는 내장 래퍼.
 */
final class IteratorConstructor {

    /**
     * A wrapper for an iterator that does not inherit the current realm's Iterator prototype.
     * <p>
     * 현재 realm의 Iterator 프로토타입을 상속하지 않는 이터레이터의 래퍼.
     */
    private static final class WrappedIterator extends JsObject {
        /** Underlying iterator and captured next method. / 기반 이터레이터와 획득할 때 읽은 next 메서드. */
        final IteratorRecord record;

        /**
         * Creates a wrapper with the realm's wrapper prototype.
         * <p>
         * realm의 래퍼 프로토타입으로 객체를 만든다.
         *
         * @param prototype the wrapper prototype / 래퍼 프로토타입
         * @param record the captured iterator record / 읽어 둔 이터레이터 기록
         */
        WrappedIterator(JsObject prototype, IteratorRecord record) {
            super(prototype);
            this.record = record;
        }
    }

    /** Provides only static installation methods. / 정적 설치 메서드만 제공한다. */
    private IteratorConstructor() {
    }

    /**
     * Installs Iterator on the existing iterator prototype.
     * <p>
     * 기존 이터레이터 프로토타입을 이용해 Iterator를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     */
    static void install(JsRealm realm) {
        final JsObject prototype = realm.getIteratorPrototype();
        final NativeFunction[] self = new NativeFunction[1];
        self[0] = new NativeFunction(realm, "Iterator", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                throw JsException.typeError("Iterator must be constructed through a derived constructor");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (newTarget == null || newTarget == self[0]) {
                    throw JsException.typeError("Iterator must be constructed through a derived constructor");
                }
                return new JsObject(Ops.getPrototypeFromConstructor(r, newTarget, prototype));
            }
        });
        final NativeFunction constructor = self[0];
        constructor.defineData("prototype", prototype, false, false, false);
        realm.setIntrinsic("%Iterator%", constructor);
        realm.getGlobalObject().defineBuiltin("Iterator", constructor);
        installAccessor(realm, prototype, "constructor", "constructor", constructor);
        installAccessor(realm, prototype, WellKnownSymbols.TO_STRING_TAG, "[Symbol.toStringTag]", Js.str("Iterator"));

        final JsObject wrapper = new JsObject(prototype);
        realm.setIntrinsic("%WrapForValidIteratorPrototype%", wrapper);
        realm.method(wrapper, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return requireWrap(receiver).record.callNext();
            }
        });
        realm.method(wrapper, "return", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsObject iterator = requireWrap(receiver).record.getIterator();
                JsValue method = Ops.getMethod(iterator, "return");
                return method.isUndefined() ? IteratorHelper.createResult(r, Js.UNDEFINED, true)
                        : Ops.call(method, iterator, Js.NO_ARGS);
            }
        });
        realm.method(constructor, "from", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsValue input = Js.arg(args, 0);
                if (!(input instanceof JsObject) && !(input instanceof JsString)) {
                    throw JsException.typeError("Iterator.from expects an object or string");
                }
                JsValue method = Ops.getMethod(r, input, WellKnownSymbols.ITERATOR);
                JsObject iterator;
                if (method.isUndefined()) {
                    // 문자열은 @@iterator가 사라지면 그 자체를 이터레이터로 사용할 수 없다.
                    if (!(input instanceof JsObject)) {
                        throw JsException.typeError("Expected an iterator object");
                    }
                    iterator = (JsObject) input;
                } else {
                    iterator = IteratorHelper.getIteratorFromMethod(input, method);
                }
                IteratorRecord record = new IteratorRecord(iterator);
                return Ops.ordinaryHasInstance(constructor, iterator) ? iterator : new WrappedIterator(wrapper, record);
            }
        });
        IteratorEagerMethods.install(realm, prototype);
    }

    /**
     * Installs a getter and a setter that can create or update an own receiver property.
     * <p>
     * getter와 수신 객체의 자체 프로퍼티를 만들거나 갱신하는 setter를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype the home iterator prototype / 메서드가 설치된 이터레이터 프로토타입
     * @param key the property key / 프로퍼티 키
     * @param name property name in function descriptors / 함수 기술자에서 사용하는 프로퍼티 이름
     * @param initial fixed value returned by the accessor / 접근자가 반환하는 고정 값
     */
    private static void installAccessor(JsRealm realm, final JsObject prototype, final Object key, String name, final JsValue initial) {
        JsObject get = realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return initial;
            }
        });
        JsObject set = realm.newFunction("set " + name, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                if (!(receiver instanceof JsObject) || receiver == prototype) {
                    throw JsException.typeError("Cannot set the Iterator prototype property on this receiver");
                }
                JsObject object = (JsObject) receiver;
                JsValue value = Js.arg(args, 0);
                // 상속된 접근자는 수신 객체에 데이터 프로퍼티를 만들고 자체 속성이 있으면 일반 Set을 호출한다.
                boolean success = object.getOwnProperty(key) == null ? object.createDataProperty(key, value) : object.set(key, value);
                if (!success) {
                    throw JsException.typeError("Cannot set the Iterator property");
                }
                return Js.UNDEFINED;
            }
        });
        prototype.defineAccessor(key, get, set, false, true);
    }

    /**
     * Requires a wrapper receiver.
     * <p>
     * 수신 객체가 이터레이터 래퍼인지 검사한다.
     *
     * @param receiver the receiver to check / 검사할 수신 객체
     * @return the wrapper / 래퍼
     */
    private static WrappedIterator requireWrap(JsValue receiver) {
        if (!(receiver instanceof WrappedIterator)) {
            throw JsException.typeError("Expected an Iterator.from wrapper");
        }
        return (WrappedIterator) receiver;
    }
}
