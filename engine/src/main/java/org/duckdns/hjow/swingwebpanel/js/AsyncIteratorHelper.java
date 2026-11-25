package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * Async iterator operations used by <code>for await</code>. A synchronous iterator is wrapped by CreateAsyncFromSyncIterator
 * and each <code>next</code>/<code>return</code> result value is Promise-resolved.
 * <p>
 * <code>for await</code> 가 쓰는 비동기 이터레이터 연산. 동기 이터레이터는 CreateAsyncFromSyncIterator 로 감싸고
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
        JsValue async = Ops.getMethod(realm, iterable, WellKnownSymbols.ASYNC_ITERATOR);
        if (!async.isUndefined()) {
            JsValue it = Ops.call(async, iterable, Js.NO_ARGS);
            if (!(it instanceof JsObject)) {
                throw JsException.typeError("Result of the Symbol.asyncIterator method is not an object");
            }
            return (JsObject) it;
        }
        return wrapSync(realm, IteratorHelper.getIterator(realm, iterable));
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
        final JsObject wrapper = realm.newObject();
        wrapper.createDataProperty("next", new NativeFunction(realm, "next", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject result = IteratorHelper.next(sync, Js.arg(args, 0));
                return wrapResult(r, result);
            }
        }));
        wrapper.createDataProperty("return", new NativeFunction(realm, "return", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue method = Ops.getMethod(sync, "return");
                if (method.isUndefined()) {
                    return promiseResult(r, Js.arg(args, 0), true);
                }
                JsValue result = Ops.call(method, sync, args.length == 0 ? Js.NO_ARGS : args);
                if (!(result instanceof JsObject)) {
                    throw JsException.typeError("Iterator return() result is not an object");
                }
                return wrapResult(r, (JsObject) result);
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
     * Promise-resolves the value of one synchronous result.
     * <p>
     * 동기 결과의 value 하나를 Promise 해결한다.
     */
    private static JsValue wrapResult(final JsRealm realm, final JsObject result) {
        final PromiseCapability cap = PromiseInterop.newCapability(realm);
        NativeFunction fulfilled = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject next = r.newObject();
                next.createDataProperty("value", Js.arg(args, 0));
                next.createDataProperty("done", result.get("done"));
                Ops.call(cap.getResolveFunction(), Js.UNDEFINED, new JsValue[] { next });
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                Ops.call(cap.getRejectFunction(), Js.UNDEFINED, new JsValue[] { Js.arg(args, 0) });
                return Js.UNDEFINED;
            }
        });
        PromiseInterop.await(realm, result.get("value"), fulfilled, rejected);
        return cap.getPromise();
    }

    /**
     * Returns an immediately fulfilled iterator-result Promise.
     * <p>
     * 즉시 이행하는 이터레이터 결과 Promise 를 만든다.
     */
    private static JsValue promiseResult(JsRealm realm, JsValue value, boolean done) {
        PromiseCapability cap = PromiseInterop.newCapability(realm);
        Ops.call(cap.getResolveFunction(), Js.UNDEFINED,
                new JsValue[] { IteratorHelper.createResult(realm, value, done) });
        return cap.getPromise();
    }
}

