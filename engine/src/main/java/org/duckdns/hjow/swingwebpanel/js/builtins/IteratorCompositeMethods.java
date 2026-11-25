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
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Iterator operations that cycle through several source iterators.
 * <p>
 * 여러 원본 이터레이터의 수명을 관리하는 Iterator 연산.
 */
final class IteratorCompositeMethods {

    /**
     * A flatMap continuation with an outer input and at most one current inner iterator.
     * <p>
     * 바깥 입력과 현재 안쪽 이터레이터 하나를 유지하는 flatMap 재개 상태.
     */
    private static final class FlatMap extends IteratorHelperObject {
        /** Source of outer values. / 바깥 값의 원본. */
        private IteratorRecord outer;

        /** The iterator of the current mapping result. / 현재 매핑 결과의 이터레이터. */
        private IteratorRecord inner;

        /** Callback used to map outer values. / 바깥 값을 매핑하는 콜백. */
        private JsValue callback;

        /** Next index for a mapping call. / 매핑 호출의 다음 인덱스. */
        private long index;

        /**
         * Captures the outer iterator and mapping function without resuming either.
         * <p>
         * 바깥 이터레이터와 매핑 함수를 실행하지 않고 보관한다.
         *
         * @param realm the helper realm / 헬퍼 realm
         * @param outer source iterator / 원본 이터레이터
         * @param callback mapping function / 매핑 함수
         */
        FlatMap(JsRealm realm, IteratorRecord outer, JsValue callback) {
            super(realm);
            this.outer = outer;
            this.callback = callback;
        }

        /** {@inheritDoc} Advances the active inner iterator or obtains another one. / 활성 안쪽 이터레이터를 진행하거나 다른 이터레이터를 얻는다. */
        @Override
        JsValue advance(JsRealm realm) {
            // 현재 안쪽 입력이 끝나는 경우에만 다음 바깥 값을 읽는다.
            for (;;) {
                realm.getLimits().tick();
                if (inner != null) {
                    try {
                        JsValue value = inner.stepValue();
                        if (value != null) {
                            return value;
                        }
                    } catch (JsException e) {
                        outer.closeOnError();
                        throw e;
                    }
                    inner = null;
                }
                JsValue item = outer.stepValue();
                if (item == null) {
                    return null;
                }
                JsValue mapped = IteratorMethodSupport.invoke(outer, callback, new JsValue[] { item, Js.num(index++) });
                try {
                    inner = IteratorMethodSupport.flattenable(realm, mapped, false);
                } catch (JsException e) {
                    outer.closeOnError();
                    throw e;
                }
            }
        }

        /** {@inheritDoc} Closes inner then outer, preserving the first failure. / 안쪽, 바깥쪽 순서로 닫고 첫 오류를 유지한다. */
        @Override
        void closeSources() {
            if (inner != null) {
                try {
                    inner.close();
                } catch (JsException e) {
                    outer.closeOnError();
                    throw e;
                }
            }
            outer.close();
        }

        /** {@inheritDoc} Releases both sources and the mapper. / 두 원본과 매핑 함수를 해제한다. */
        @Override
        void discard() {
            outer = null;
            inner = null;
            callback = null;
        }
    }

    /**
     * Concatenation with captured input methods that are invoked only when needed.
     * <p>
     * 입력 메서드를 읽어 두고 필요할 때만 호출하는 연결 헬퍼.
     */
    private static final class Concat extends IteratorHelperObject {
        /** Input objects in order. / 순서대로 보관한 입력 객체. */
        private JsObject[] items;

        /** Iterator methods captured at construction. / 생성 시 읽어 둔 이터레이터 메서드. */
        private JsValue[] methods;

        /** Input to open next. / 다음에 여는 입력의 위치. */
        private int index;

        /** The single opened iterator, if any. / 열려 있는 이터레이터 하나. 없으면 null. */
        private IteratorRecord active;

        /**
         * Records the items and their iterator methods.
         * <p>
         * 입력과 각 입력의 이터레이터 메서드를 보관한다.
         *
         * @param realm the helper realm / 헬퍼 realm
         * @param items input objects / 입력 객체
         * @param methods input iterator methods / 각 입력의 이터레이터 메서드
         */
        Concat(JsRealm realm, JsObject[] items, JsValue[] methods) {
            super(realm);
            this.items = items;
            this.methods = methods;
        }

        /** {@inheritDoc} Opens only the next needed source and reads from it. / 필요한 다음 원본만 열어 값을 읽는다. */
        @Override
        JsValue advance(JsRealm realm) {
            // 빈 입력을 건너뛰더라도 모든 열기와 순회는 입력 순서를 따른다.
            for (;;) {
                realm.getLimits().tick();
                if (active == null) {
                    if (index == items.length) {
                        return null;
                    }
                    active = new IteratorRecord(IteratorHelper.getIteratorFromMethod(items[index], methods[index]));
                    items[index] = null;
                    methods[index++] = null;
                }
                JsValue item = active.stepValue();
                if (item != null) {
                    return item;
                }
                active = null;
            }
        }

        /** {@inheritDoc} Closes the one active iterator without opening future sources. / 이후 원본을 열지 않고 현재 이터레이터만 닫는다. */
        @Override
        void closeSources() {
            if (active != null) {
                active.close();
            }
        }

        /** {@inheritDoc} Releases the source and remaining inputs. / 원본과 남은 입력을 해제한다. */
        @Override
        void discard() {
            items = null;
            methods = null;
            active = null;
        }
    }

    /** Installs static built-in operations. / 정적 내장 연산을 설치한다. */
    private IteratorCompositeMethods() {
    }

    /**
     * Installs flatMap and Iterator.concat.
     * <p>
     * flatMap과 Iterator.concat을 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param constructor Iterator constructor / Iterator 생성자
     * @param prototype Iterator prototype / Iterator 프로토타입
     */
    static void install(JsRealm realm, JsObject constructor, JsObject prototype) {
        realm.method(prototype, "flatMap", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsValue callback = Js.arg(args, 0);
                return new FlatMap(r, IteratorMethodSupport.withCallback(receiver, callback), callback);
            }
        });
        realm.method(constructor, "concat", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                JsObject[] sources = new JsObject[args.length];
                JsValue[] methods = new JsValue[args.length];
                // 모든 입력 메서드는 지금 검사하되 실제 이터레이터는 next를 받을 때 연다.
                for (int i = 0; i < args.length; i++) {
                    if (!(args[i] instanceof JsObject)) {
                        throw JsException.typeError("Iterator.concat requires iterable objects");
                    }
                    JsObject source = (JsObject) args[i];
                    JsValue method = Ops.getMethod(source, WellKnownSymbols.ITERATOR);
                    if (method.isUndefined()) {
                        throw JsException.typeError("Iterator.concat input is not iterable");
                    }
                    sources[i] = source;
                    methods[i] = method;
                }
                return new Concat(r, sources, methods);
            }
        });
    }
}
