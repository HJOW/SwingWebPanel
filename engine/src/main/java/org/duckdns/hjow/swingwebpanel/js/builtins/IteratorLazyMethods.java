package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.IteratorRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The single-source lazy Iterator prototype methods.
 * <p>
 * 하나의 원본을 순회하는 Iterator 프로토타입의 지연 메서드.
 */
final class IteratorLazyMethods {

    /**
     * Operations over a single input iterator.
     * <p>
     * 입력 이터레이터 하나를 대상으로 하는 연산.
     */
    private enum Operation {
        /** Transforms each value. / 각 값을 변환한다. */
        MAP,
        /** Yields values that pass a predicate. / 조건을 만족한 값을 반환한다. */
        FILTER,
        /** Stops after a given number of values. / 주어진 개수의 값에서 멈춘다. */
        TAKE,
        /** Discards a prefix and yields the remaining values. / 앞부분을 버리고 남은 값을 반환한다. */
        DROP
    }

    /**
     * The continuation of a single-source helper.
     * <p>
     * 원본 하나를 순회하는 헬퍼의 재개 상태.
     */
    private static final class Pipeline extends IteratorHelperObject {
        /** The original iterator, until completion. / 완료 전 원본 이터레이터. */
        private IteratorRecord source;

        /** The mapping or filter callback, when used. / 매핑 또는 필터 콜백. 사용하는 연산에서만 설정한다. */
        private JsValue callback;

        /** Which step algorithm this helper uses. / 헬퍼에서 사용하는 단계 알고리즘. */
        private final Operation operation;

        /** The count still to yield or ignore for take and drop. / take·drop에서 반환하거나 건너뛸 남은 개수. */
        private double remaining;

        /** The next callback index for map or filter. / map·filter의 다음 콜백 인덱스. */
        private long index;

        /**
         * Stores the source, operation and its arguments without advancing the source.
         * <p>
         * 원본을 진행하지 않고 이터레이터, 연산과 인수를 저장한다.
         *
         * @param realm source operation realm / 원본 연산의 realm
         * @param source original iterator / 원본 이터레이터
         * @param operation step operation / 단계 연산
         * @param callback mapping or filtering callback or null / 매핑·필터 콜백 또는 null
         * @param count maximum count for take or drop / take·drop의 최대 개수
         */
        Pipeline(JsRealm realm, IteratorRecord source, Operation operation, JsValue callback, double count) {
            super(realm);
            this.source = source;
            this.operation = operation;
            this.callback = callback;
            this.remaining = count;
        }

        /** {@inheritDoc} Advances one lazy operation. / 지연 연산 한 단계를 진행한다. */
        @Override
        JsValue advance(JsRealm realm) {
            switch (operation) {
            case MAP:
                JsValue value = source.stepValue();
                return value == null ? null : callback(realm, value);
            case FILTER:
                JsValue item;
                // 첫 일치까지 읽어 반환하며 탈락한 요소도 콜백 인덱스에 포함한다.
                while ((item = source.stepValue()) != null) {
                    if (Ops.toBoolean(callback(realm, item))) {
                        return item;
                    }
                }
                return null;
            case TAKE:
                // 제한에 도달한 다음 next에서는 원본을 읽기 전에 닫는다.
                if (remaining == 0) {
                    source.close();
                    return null;
                }
                remaining--;
                return source.stepValue();
            case DROP:
                // 제한만큼 완전한 요소를 건너뛰며 무한 제한이면 원본 소진까지 진행한다.
                while (remaining > 0) {
                    realm.getLimits().tick();
                    remaining--;
                    if (source.step() == null) {
                        return null;
                    }
                }
                return source.stepValue();
            default:
                throw new AssertionError(operation);
            }
        }

        /**
         * Invokes the callback for the current item.
         * <p>
         * 현재 요소의 콜백을 호출한다.
         *
         * @param realm the execution realm / 실행 realm
         * @param item current item / 현재 요소
         * @return callback result / 콜백 반환값
         */
        private JsValue callback(JsRealm realm, JsValue item) {
            realm.getLimits().tick();
            return IteratorMethodSupport.invoke(source, callback, new JsValue[] { item, Js.num(index++) });
        }

        /** {@inheritDoc} Closes the original iterator. / 원본 이터레이터를 닫는다. */
        @Override
        void closeSources() {
            source.close();
        }

        /** {@inheritDoc} Releases the iterator and callback. / 이터레이터와 콜백을 해제한다. */
        @Override
        void discard() {
            source = null;
            callback = null;
        }
    }

    /** Uses static installation methods only. / 정적 설치 메서드만 사용한다. */
    private IteratorLazyMethods() {
    }

    /**
     * Installs lazy single-source helpers over the common helper prototype.
     * <p>
     * 공통 헬퍼 프로토타입을 사용하는 원본 하나의 지연 메서드를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype Iterator prototype / Iterator 프로토타입
     */
    static void install(JsRealm realm, JsObject prototype) {
        IteratorHelperObject.install(realm);
        installCallback(realm, prototype, "map", Operation.MAP);
        installCallback(realm, prototype, "filter", Operation.FILTER);
        installCount(realm, prototype, "take", Operation.TAKE);
        installCount(realm, prototype, "drop", Operation.DROP);
    }

    /**
     * Installs a lazily evaluated callback operation.
     * <p>
     * 콜백을 지연 실행하는 연산을 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype Iterator prototype / Iterator 프로토타입
     * @param name property name / 프로퍼티 이름
     * @param operation callback operation / 콜백 연산
     */
    private static void installCallback(JsRealm realm, JsObject prototype, String name, final Operation operation) {
        realm.method(prototype, name, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsValue callback = Js.arg(args, 0);
                return new Pipeline(r, IteratorMethodSupport.withCallback(receiver, callback), operation, callback, 0);
            }
        });
    }

    /**
     * Installs a lazy operation that uses a non-negative count.
     * <p>
     * 음이 아닌 개수를 사용하는 지연 연산을 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype Iterator prototype / Iterator 프로토타입
     * @param name property name / 프로퍼티 이름
     * @param operation count operation / 개수 연산
     */
    private static void installCount(JsRealm realm, JsObject prototype, String name, final Operation operation) {
        realm.method(prototype, name, 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsObject source = IteratorMethodSupport.requireObject(receiver);
                double count;
                try {
                    double numeric = Ops.toNumber(Js.arg(args, 0));
                    count = Ops.toInteger(numeric);
                    if (Double.isNaN(numeric) || count < 0) {
                        throw JsException.rangeError("Iterator limit must be a non-negative integer or Infinity");
                    }
                } catch (JsException e) {
                    // next를 읽기 전에 한계의 변환·검사 오류가 나면 원본을 닫는다.
                    IteratorHelper.closeQuietly(source);
                    throw e;
                }
                return new Pipeline(r, new IteratorRecord(source), operation, null, count);
            }
        });
    }
}
