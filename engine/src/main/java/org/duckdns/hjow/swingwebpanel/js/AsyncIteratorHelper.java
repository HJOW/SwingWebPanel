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
        return sync.isUndefined() ? null : wrapSync(realm, IteratorHelper.getIteratorFromMethod(iterable, sync));
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
     * Wraps one synchronous iterator.
     * <p>
     * 동기 이터레이터 하나를 감싼다.
     */
    private static JsObject wrapSync(final JsRealm realm, final JsObject sync) {
        final JsValue next = sync.get("next");
        final JsObject wrapper = realm.newObject();
        wrapper.createDataProperty("next", new NativeFunction(realm, "next", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    JsObject result = IteratorHelper.next(sync, next, args.length == 0 ? null : args[0]);
                    return wrapResult(r, sync, result, true);
                } catch (JsException e) {
                    return rejectedPromise(r, e);
                }
            }
        }));
        wrapper.createDataProperty("return", new NativeFunction(realm, "return", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
                    JsValue method = Ops.getMethod(sync, "return");
                    if (method.isUndefined()) {
                        return promiseResult(r, Js.arg(args, 0), true);
                    }
                    return wrapResult(r, sync, callResult(sync, method, args), false);
                } catch (JsException e) {
                    return rejectedPromise(r, e);
                }
            }
        }));
        wrapper.createDataProperty("throw", new NativeFunction(realm, "throw", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                try {
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
        }));
        wrapper.createDataProperty(WellKnownSymbols.ASYNC_ITERATOR, new NativeFunction(realm, "[Symbol.asyncIterator]", 0,
                new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        return wrapper;
                    }
                }));
        return wrapper;
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
        JsValue result = Ops.call(method, iterator, args.length == 0 ? Js.NO_ARGS : new JsValue[] { args[0] });
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

