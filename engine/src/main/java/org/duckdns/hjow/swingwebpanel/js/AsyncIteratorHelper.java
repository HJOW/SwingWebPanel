package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * Async iterator operations used by <code>for await</code> and <code>Array.fromAsync</code>. A synchronous iterator is wrapped by CreateAsyncFromSyncIterator
 * and each <code>next</code>/<code>return</code> result value is Promise-resolved.
 * <p>
 * <code>for await</code>와 <code>Array.fromAsync</code>가 쓰는 비동기 이터레이터 연산. 동기 이터레이터는 CreateAsyncFromSyncIterator로 감싸고
 * <code>next</code>/<code>return</code> 결과 값을 Promise 해결한다.
 */
public final class AsyncIteratorHelper {

    /** Name of the async-from-sync intrinsic. / 동기 이터레이터를 감싼 비동기 어댑터의 내장 이름. */
    private static final String PROTOTYPE_NAME = "%AsyncFromSyncIteratorPrototype%";

    /**
     * An adapter with a captured synchronous iterator record.
     * <p>
     * 획득한 동기 이터레이터 기록을 보관하는 어댑터.
     */
    private static final class SyncAdapter extends JsObject {

        /** The wrapped iterator and its captured next method. / 감싼 이터레이터와 저장한 next 메서드. */
        final IteratorRecord sync;

        /**
         * Creates the internal adapter.
         * <p>
         * 내부 어댑터를 만든다.
         *
         * @param prototype the adapter prototype / 어댑터 프로토타입
         * @param iterator the synchronous iterator / 동기 이터레이터
         */
        SyncAdapter(JsObject prototype, JsObject iterator) {
            super(prototype);
            sync = new IteratorRecord(iterator);
        }
    }

    /**
     * Prevents construction.
     * <p>
     * 생성을 막는다.
     */
    private AsyncIteratorHelper() {
    }

    /**
     * Gets an async iterator, falling back to a wrapped synchronous iterator.
     * <p>
     * 비동기 이터레이터를 찾고 없으면 동기 이터레이터를 감싼다.
     *
     * @param realm realm / realm
     * @param iterable iterable / 반복 가능 값
     * @return async iterator / 비동기 이터레이터
     */
    public static JsObject getAsyncIterator(JsRealm realm, JsValue iterable) {
        JsObject iterator = getAsyncIteratorOrNull(realm, iterable);
        if (iterator == null) {
            throw JsException.typeError("Value is not iterable");
        }
        return iterator;
    }

    /**
     * Acquires an async iterator and captures its next method before the first step.
     * <p>
     * 비동기 이터레이터를 획득하고 첫 순회 전에 next 메서드를 저장한다.
     *
     * @param realm the realm / 렐름
     * @param iterable the iterable / 순회 가능한 값
     * @return the iterator record / 이터레이터 기록
     */
    public static IteratorRecord getAsyncIteratorRecord(JsRealm realm, JsValue iterable) {
        return new IteratorRecord(getAsyncIterator(realm, iterable));
    }

    /**
     * Gets an async iterator or wraps a sync iterator. Null means neither method was provided.
     * <p>
     * 비동기 이터레이터를 얻거나 동기 이터레이터를 감싼다. 두 메서드가 모두 없으면 null을 반환한다.
     *
     * @param realm the realm / realm
     * @param iterable candidate iterable / 반복 가능한 값 후보
     * @return the iterator or null / 이터레이터 또는 null
     */
    public static JsObject getAsyncIteratorOrNull(JsRealm realm, JsValue iterable) {
        JsValue async = Ops.getMethod(realm, iterable, WellKnownSymbols.ASYNC_ITERATOR);
        if (!async.isUndefined()) {
            JsValue it = Ops.call(async, iterable, Js.NO_ARGS);
            if (!(it instanceof JsObject)) {
                throw JsException.typeError("Result of the Symbol.asyncIterator method is not an object");
            }
            return (JsObject) it;
        }
        JsValue sync = Ops.getMethod(realm, iterable, WellKnownSymbols.ITERATOR);
        return sync.isUndefined() ? null : new SyncAdapter(realm.getIntrinsic(PROTOTYPE_NAME),
                IteratorHelper.getIteratorFromMethod(iterable, sync));
    }

    /**
     * Calls async iterator next and returns its Promise/thenable result.
     * <p>
     * 비동기 이터레이터 next 를 호출해 Promise/thenable 결과를 반환한다.
     *
     * @param iterator iterator / 이터레이터
     * @return next result promise / next 결과 Promise
     */
    public static JsValue next(JsObject iterator) {
        return Ops.call(iterator.get("next"), iterator, Js.NO_ARGS);
    }

    /**
     * Calls an async iterator return method if present.
     * <p>
     * 있으면 비동기 이터레이터 return 메서드를 호출한다.
     *
     * @param iterator iterator / 이터레이터
     * @return return result or null when absent / 반환 결과. 없으면 null
     */
    public static JsValue close(JsObject iterator) {
        JsValue method = Ops.getMethod(iterator, "return");
        return method.isUndefined() ? null : Ops.call(method, iterator, Js.NO_ARGS);
    }

    /**
     * Installs the shared prototype for wrapping synchronous iterators.
     * <p>
     * 동기 이터레이터를 감쌀 때 쓰는 공용 프로토타입을 설치한다.
     *
     * @param realm the receiving realm / 설치할 렐름
     * @param asyncIteratorPrototype the %AsyncIteratorPrototype% object / %AsyncIteratorPrototype% 객체
     */
    public static void installPrototype(JsRealm realm, JsObject asyncIteratorPrototype) {
        JsObject proto = new JsObject(asyncIteratorPrototype);
        realm.setIntrinsic(PROTOTYPE_NAME, proto);
        realm.method(proto, "next", 0, new NativeFunction.Call() {
            /**
             * Calls the captured next and assimilates the synchronous iterator's result value.
             * <p>
             * 저장한 next를 호출하고 동기 이터레이터의 결과값을 Promise로 해결한다.
             *
             * @param r the realm / 렐름
             * @param thisValue the adapter / 어댑터
             * @param args the optional value / 선택적 인수
             * @return the result Promise / 결과 Promise
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    IteratorRecord sync = requireSync(thisValue);
                    JsObject result = requireResult(sync.callNext(args.length == 0 ? null : args[0]));
                    return wrapResult(r, sync.getIterator(), result, true);
                } catch (JsException e) {
                    return rejectedPromise(r, e);
                }
            }
        });
        realm.method(proto, "return", 0, new NativeFunction.Call() {
            /**
             * Looks up and calls the synchronous iterator's currently visible return method.
             * <p>
             * 동기 이터레이터의 현재 return 메서드를 조회하고 호출한다.
             *
             * @param r the realm / 렐름
             * @param thisValue the adapter / 어댑터
             * @param args the optional value / 선택적 인수
             * @return the result Promise / 결과 Promise
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    JsObject sync = requireSync(thisValue).getIterator();
                    JsValue method = Ops.getMethod(sync, "return");
                    if (method.isUndefined()) {
                        return promiseResult(r, Js.arg(args, 0), true);
                    }
                    return wrapResult(r, sync, callResult(sync, method, args), false);
                } catch (JsException e) {
                    return rejectedPromise(r, e);
                }
            }
        });
        realm.method(proto, "throw", 0, new NativeFunction.Call() {
            /**
             * Forwards a throw request or closes the iterator when it has no throw method.
             * <p>
             * throw 요청을 전달한다. 메서드가 없으면 이터레이터를 닫는다.
             *
             * @param r the realm / 렐름
             * @param thisValue the adapter / 어댑터
             * @param args the optional value / 선택적 인수
             * @return the result Promise / 결과 Promise
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    JsObject sync = requireSync(thisValue).getIterator();
                    JsValue method = Ops.getMethod(sync, "throw");
                    if (method.isUndefined()) {
                        IteratorHelper.close(sync);
                        throw JsException.typeError("The iterator does not provide a 'throw' method");
                    }
                    return wrapResult(r, sync, callResult(sync, method, args), true);
                } catch (JsException e) {
                    return rejectedPromise(r, e);
                }
            }
        });
    }

    /**
     * Returns the synchronous iterator record stored by an adapter.
     * <p>
     * 어댑터가 저장한 동기 이터레이터 기록을 반환한다.
     *
     * @param receiver the candidate adapter / 어댑터 후보
     * @return the iterator record / 이터레이터 기록
     */
    private static IteratorRecord requireSync(JsValue receiver) {
        if (!(receiver instanceof SyncAdapter)) {
            throw JsException.typeError("Incompatible async-from-sync iterator receiver");
        }
        return ((SyncAdapter) receiver).sync;
    }

    /**
     * Calls a captured iterator method with at most one argument and checks its result.
     * <p>
     * 미리 읽은 이터레이터 메서드를 인수 최대 하나로 호출하고 결과를 검사한다.
     *
     * @param iterator the sync iterator / 동기 이터레이터
     * @param method its return or throw method / return 또는 throw 메서드
     * @param args the wrapper's arguments / 래퍼의 인수
     * @return iterator result / 이터레이터 결과
     */
    private static JsObject callResult(JsObject iterator, JsValue method, JsValue[] args) {
        return requireResult(Ops.call(method, iterator, args.length == 0 ? Js.NO_ARGS : new JsValue[] { args[0] }));
    }

    /**
     * Validates the object returned by a synchronous iterator method.
     * <p>
     * 동기 이터레이터 메서드가 반환한 객체를 검증한다.
     *
     * @param result the returned value / 반환값
     * @return the iterator result object / 이터레이터 결과 객체
     */
    private static JsObject requireResult(JsValue result) {
        if (!(result instanceof JsObject)) {
            throw JsException.typeError("Iterator result is not an object");
        }
        return (JsObject) result;
    }

    /**
     * Promise-resolves the value of one synchronous result.
     * <p>
     * 동기 결과의 value 하나를 Promise 해결한다.
     */
    private static JsValue wrapResult(final JsRealm realm, final JsObject iterator, JsObject result, boolean closeOnRejection) {
        final boolean done = IteratorHelper.isDone(result);
        final boolean shouldClose = !done && closeOnRejection;
        JsValue value = IteratorHelper.value(result);
        final PromiseCapability cap = PromiseInterop.newCapability(realm);
        NativeFunction fulfilled = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                cap.resolve(IteratorHelper.createResult(r, Js.arg(args, 0), done));
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (shouldClose) {
                    IteratorHelper.closeQuietly(iterator);
                }
                cap.reject(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        try {
            PromiseInterop.await(realm, value, fulfilled, rejected);
        } catch (JsException e) {
            if (shouldClose) {
                IteratorHelper.closeQuietly(iterator);
            }
            cap.reject(e.getValue(realm));
        }
        return cap.getPromise();
    }

    /**
     * Returns an intrinsic promise rejected with the exception.
     * <p>
     * 예외로 거부된 내장 Promise를 반환한다.
     *
     * @param realm the current realm / 현재 realm
     * @param error the exception / 예외
     * @return rejected promise / 거부된 Promise
     */
    private static JsValue rejectedPromise(JsRealm realm, JsException error) {
        PromiseCapability cap = PromiseInterop.newCapability(realm);
        cap.reject(error.getValue(realm));
        return cap.getPromise();
    }

    /**
     * Returns an immediately fulfilled iterator-result Promise.
     * <p>
     * 즉시 이행하는 이터레이터 결과 Promise 를 만든다.
     */
    private static JsValue promiseResult(JsRealm realm, JsValue value, boolean done) {
        PromiseCapability cap = PromiseInterop.newCapability(realm);
        cap.resolve(IteratorHelper.createResult(realm, value, done));
        return cap.getPromise();
    }
}

