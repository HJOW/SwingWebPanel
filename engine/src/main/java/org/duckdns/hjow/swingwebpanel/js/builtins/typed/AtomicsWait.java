package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The Atomics synchronization methods on shared memory.
 * <p>
 * 공유 메모리를 대상으로 하는 Atomics 동기화 메서드.
 */
final class AtomicsWait {

    /** Installs only static operations. / 정적 연산만 설치한다. */
    private AtomicsWait() {
    }

    /**
     * Installs wait, waitAsync and notify on the Atomics namespace.
     * <p>
     * Atomics 네임스페이스에 wait, waitAsync, notify를 설치한다.
     *
     * @param realm installing realm / 설치할 realm
     * @param atomics Atomics namespace / Atomics 네임스페이스
     */
    static void install(JsRealm realm, JsObject atomics) {
        realm.method(atomics, "wait", 4, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return waitFor(r, args, false);
            }
        });
        realm.method(atomics, "waitAsync", 4, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return waitFor(r, args, true);
            }
        });
        realm.method(atomics, "notify", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject array = AtomicsLib.validateIntegerArray(Js.arg(args, 0), true);
                int offset = AtomicsLib.accessOffset(array, Js.arg(args, 1));
                JsValue countArg = Js.arg(args, 2);
                double count = countArg.isUndefined() ? Double.POSITIVE_INFINITY : Math.max(Ops.toInteger(countArg), 0);
                SharedByteBlock block = array.getBuffer().sharedBlock();
                // 일반 ArrayBuffer에 통지할 때도 순서대로 인수를 변환한 뒤 0을 반환한다.
                return Js.num(block == null ? 0 : block.waiters().notifyAt(r, offset, count));
            }
        });
    }

    /**
     * Validates one request before entering the wait lists of its shared storage.
     * <p>
     * 공유 저장소의 대기열에 들어가기 전에 대기 요청을 검사한다.
     *
     * @param realm the waiting realm / 대기할 realm
     * @param args JavaScript arguments / JavaScript 인수
     * @param async whether to return an asynchronous result object / 비동기 결과 객체 반환 여부
     * @return a status or an asynchronous result / 상태 또는 비동기 결과
     */
    private static JsValue waitFor(JsRealm realm, JsValue[] args, boolean async) {
        TypedArrayObject array = AtomicsLib.validateIntegerArray(Js.arg(args, 0), true);
        SharedByteBlock block = array.getBuffer().sharedBlock();
        // 공유 버퍼 검사를 인덱스와 값의 변환보다 먼저 수행한다.
        if (block == null) {
            throw JsException.typeError("Atomics wait requires a SharedArrayBuffer");
        }
        int offset = AtomicsLib.accessOffset(array, Js.arg(args, 1));
        ElementType type = array.getType();
        long expected = type.bitsFromValue(Js.arg(args, 2));
        long timeout = timeoutNanos(Ops.toNumber(Js.arg(args, 3)));
        if (!async && !realm.isAtomicsWaitAllowed()) {
            throw JsException.typeError("This agent cannot suspend in Atomics.wait");
        }
        SharedWaiters waiters = block.waiters();
        SharedWaiters.Waiter waiter = waiters.register(realm, block, offset, type.getSize(), expected, timeout, async);
        if (!async) {
            return Js.str(waiters.await(waiter));
        }
        boolean pending = waiter.promise != null;
        JsObject result = realm.newObject();
        result.defineData("async", Js.bool(pending), true, true, true);
        result.defineData("value", pending ? waiter.promise.getPromise() : Js.str(waiter.status), true, true, true);
        return result;
    }

    /**
     * Converts timeout milliseconds into a monotonic waiting interval.
     * <p>
     * 타임아웃 밀리초를 단조 시계로 잴 대기 시간으로 변환한다.
     *
     * @param millis converted milliseconds / 변환한 밀리초
     * @return nanoseconds, or -1 for an unlimited wait / 나노초. 무한 대기이면 -1
     */
    private static long timeoutNanos(double millis) {
        if (Double.isNaN(millis) || millis == Double.POSITIVE_INFINITY) {
            return -1;
        }
        return (long) Math.ceil(Math.max(millis, 0) * 1000000);
    }
}
