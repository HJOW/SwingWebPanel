package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.IteratorRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The Iterator prototype methods that consume their receiver synchronously.
 * <p>
 * 수신 이터레이터를 동기적으로 소비하는 Iterator 프로토타입 메서드.
 */
final class IteratorEagerMethods {

    /**
     * Predicate-based operations with distinct early-exit and exhaustion results.
     * <p>
     * 조기 종료 및 끝까지 소진했을 때 반환값이 서로 다른 조건 검사 연산.
     */
    private enum Predicate {
        /** Stops on a matching item and returns true. / 일치한 요소가 나오면 true를 반환한다. */
        SOME("some", true, Js.FALSE),
        /** Stops on a non-matching item and returns false. / 일치하지 않는 요소가 나오면 false를 반환한다. */
        EVERY("every", false, Js.TRUE),
        /** Stops on a matching item and returns the item. / 일치한 요소가 나오면 그 요소를 반환한다. */
        FIND("find", true, Js.UNDEFINED);

        /** The method name on Iterator.prototype. / Iterator.prototype에 설치할 메서드 이름. */
        final String method;

        /** Predicate result that closes the iterator. / 이터레이터를 닫는 조건 결과. */
        final boolean stopWhen;

        /** Value when the source is exhausted. / 원본을 소진했을 때 반환할 값. */
        final JsValue exhausted;

        /**
         * Stores this predicate method's observable behavior.
         * <p>
         * 조건 검사 메서드에서 관찰할 수 있는 동작을 저장한다.
         *
         * @param method method name / 메서드 이름
         * @param stopWhen predicate result that stops iteration / 순회를 멈추는 조건 결과
         * @param exhausted result returned upon exhaustion / 원본을 소진했을 때 반환할 값
         */
        Predicate(String method, boolean stopWhen, JsValue exhausted) {
            this.method = method;
            this.stopWhen = stopWhen;
            this.exhausted = exhausted;
        }
    }

    /** Uses static methods only. / 정적 메서드만 사용한다. */
    private IteratorEagerMethods() {
    }

    /**
     * Installs the synchronous consumption methods on Iterator.prototype.
     * <p>
     * Iterator.prototype에 동기 소비 메서드를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype iterator prototype / 이터레이터 프로토타입
     */
    static void install(JsRealm realm, JsObject prototype) {
        realm.method(prototype, "toArray", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                IteratorRecord iterator = new IteratorRecord(IteratorMethodSupport.requireObject(receiver));
                JsArray output = r.newArray();
                JsValue item;
                // 원본 값을 순서대로 배열에 넣는다.
                while ((item = iterator.stepValue()) != null) {
                    r.getLimits().tick();
                    output.add(item);
                }
                return output;
            }
        });
        realm.method(prototype, "forEach", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsValue callback = Js.arg(args, 0);
                IteratorRecord iterator = IteratorMethodSupport.withCallback(receiver, callback);
                long index = 0;
                JsValue item;
                // 각 요소와 현재 인덱스를 넘기고 콜백 반환값은 사용하지 않는다.
                while ((item = iterator.stepValue()) != null) {
                    r.getLimits().tick();
                    IteratorMethodSupport.invoke(iterator, callback, new JsValue[] { item, Js.num(index++) });
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(prototype, "reduce", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsValue callback = Js.arg(args, 0);
                IteratorRecord iterator = IteratorMethodSupport.withCallback(receiver, callback);
                return reduce(r, iterator, callback, args.length > 1 ? args[1] : null);
            }
        });
        // 같은 순회 프로토콜을 사용하는 세 조건 검사 메서드를 설치한다.
        for (final Predicate predicate : Predicate.values()) {
            realm.method(prototype, predicate.method, 1, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                    JsValue callback = Js.arg(args, 0);
                    IteratorRecord iterator = IteratorMethodSupport.withCallback(receiver, callback);
                    return test(r, iterator, callback, predicate);
                }
            });
        }
    }

    /**
     * Combines the items with an optional initial accumulator.
     * <p>
     * 선택적 초기 누산값으로 요소들을 결합한다.
     *
     * @param realm current realm / 현재 realm
     * @param iterator source iterator / 원본 이터레이터
     * @param callback reducer callback / 합산 콜백
     * @param initial initial value or Java null if omitted / 초기값. 생략하면 Java null
     * @return final accumulator / 최종 누산값
     */
    private static JsValue reduce(JsRealm realm, IteratorRecord iterator, JsValue callback, JsValue initial) {
        JsValue accumulator = initial;
        long index = 0;
        if (accumulator == null) {
            accumulator = iterator.stepValue();
            if (accumulator == null) {
                throw JsException.typeError("Cannot reduce an empty iterator without an initial value");
            }
            index = 1;
        }
        JsValue item;
        // 현재 누산값, 요소 및 인덱스를 넘겨 반환값을 다음 누산값으로 사용한다.
        while ((item = iterator.stepValue()) != null) {
            realm.getLimits().tick();
            accumulator = IteratorMethodSupport.invoke(iterator, callback, new JsValue[] { accumulator, item, Js.num(index++) });
        }
        return accumulator;
    }

    /**
     * Tests successive items until the predicate method can terminate or the source is exhausted.
     * <p>
     * 조건 검사 메서드가 답을 얻거나 원본을 소진할 때까지 차례로 요소를 검사한다.
     *
     * @param realm current realm / 현재 realm
     * @param iterator source iterator / 원본 이터레이터
     * @param callback predicate callback / 조건 검사 콜백
     * @param predicate method being executed / 실행 중인 메서드
     * @return the selected element, boolean or undefined / 선택한 요소, 불리언 또는 undefined
     */
    private static JsValue test(JsRealm realm, IteratorRecord iterator, JsValue callback, Predicate predicate) {
        long index = 0;
        JsValue item;
        // 결론에 필요한 첫 요소를 찾으면 return을 호출한 뒤 결과를 반환한다.
        while ((item = iterator.stepValue()) != null) {
            realm.getLimits().tick();
            JsValue match = IteratorMethodSupport.invoke(iterator, callback, new JsValue[] { item, Js.num(index++) });
            if (Ops.toBoolean(match) == predicate.stopWhen) {
                iterator.close();
                return predicate == Predicate.FIND ? item : Js.bool(predicate.stopWhen);
            }
        }
        return predicate.exhausted;
    }
}
