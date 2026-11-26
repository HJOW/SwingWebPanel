package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;
import org.duckdns.hjow.swingwebpanel.js.host.web.ReadableStreamObject.ReadRequest;
import org.duckdns.hjow.swingwebpanel.js.host.web.ReadableStreamObject.Reader;

/**
 * Owns a reader for a stream's asynchronous iterator and serializes next and return calls.
 * <p>
 * 스트림의 비동기 반복자가 사용하는 리더를 보관하고 next와 return 호출을 순서대로 실행한다.
 */
final class ReadableStreamIterator extends JsObject {
    /** The reader until it reaches the end or returns. / 끝에 도달하거나 반환할 때까지의 리더. */
    private final Reader reader;
    /** Realm in which to settle requests. / 요청을 확정할 렐름. */
    private final JsRealm realm;
    /** Whether early return leaves the stream available. / 조기 반환 후 스트림을 사용할 수 있게 남기는지 여부. */
    private final boolean preventCancel;
    /** Most recent queued operation, if any. / 가장 최근에 대기시킨 동작. */
    private Promises.Deferred tail;

    /**
     * Creates an iterator holding the stream lock.
     * <p>
     * 스트림 잠금을 보유한 반복자를 만든다.
     *
     * @param prototype iterator prototype / 반복자 프로토타입
     * @param reader active stream reader / 활성 스트림 리더
     * @param preventCancel whether to skip cancellation on early return / 조기 반환 때 취소를 건너뛰는지 여부
     */
    ReadableStreamIterator(JsObject prototype, Reader reader, boolean preventCancel) {
        super(prototype);
        this.reader = reader;
        this.realm = reader.stream.realm;
        this.preventCancel = preventCancel;
    }

    /**
     * Adds a read or return to the operations, even if an earlier one is still waiting for data.
     * <p>
     * 앞 동작이 아직 데이터를 기다리더라도 읽기나 반환을 동작에 추가한다.
     *
     * @param returning true for return / return이면 true
     * @param value return argument / return 인수
     * @return promise for operation result / 동작 결과 프라미스
     */
    JsObject step(final boolean returning, final JsValue value) {
        Promises.Deferred previous = tail;
        final Promises.Deferred operation = Promises.create(realm);
        tail = operation;
        // 앞선 요청이 청크나 오류를 받은 뒤에만 이 요청을 실행한다.
        if (previous == null) {
            run(operation, returning, value);
        } else {
            NativeFunction after = realm.newFunction("", 0, new NativeFunction.Call() {
                /** Runs the next queued iterator operation. / 다음 반복자 동작을 실행한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    run(operation, returning, value);
                    return Js.UNDEFINED;
                }
            });
            PromiseInterop.await(realm, previous.promise, after, after);
        }
        return operation.promise;
    }

    /**
     * Performs the queued iterator operation once it reaches the front.
     * <p>
     * 대기열의 처음에 도달한 반복자 동작을 실행한다.
     *
     * @param operation iterator operation / 반복자 동작
     * @param returning true for return / return이면 true
     * @param value return argument / return 인수
     */
    private void run(final Promises.Deferred operation, boolean returning, final JsValue value) {
        final ReadableStreamObject stream = reader.stream;
        if (stream == null) {
            settle(operation, ReadableStreamObject.result(realm, value, true), false);
            return;
        }
        try {
            if (!returning) {
                stream.read(new ReadRequest() {
                    /** Delivers the next chunk without releasing the reader. / 리더를 유지하고 다음 청크를 전달한다. */
                    public void onChunk(JsValue chunk) {
                        settle(operation, ReadableStreamObject.result(realm, chunk, false), false);
                    }

                    /** Releases the reader at the end. / 종료와 함께 리더를 해제한다. */
                    public void onEnd() {
                        stream.release();
                        settle(operation, ReadableStreamObject.result(realm, Js.UNDEFINED, true), false);
                    }

                    /** Releases the reader and rejects the failed read. / 리더를 해제하고 실패한 읽기를 거부한다. */
                    public void onError(JsValue reason) {
                        stream.release();
                        settle(operation, reason, true);
                    }
                });
                return;
            }
            // 취소 알고리즘을 호출하고 완료 프라미스를 기다리기 전에 잠금을 해제한다.
            JsValue result = preventCancel ? Js.UNDEFINED : stream.cancel(value);
            stream.release();
            PromiseInterop.await(realm, result, realm.newFunction("", 1, new NativeFunction.Call() {
                /** Returns the final iterator result. / 마지막 반복자 결과를 반환한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    settle(operation, ReadableStreamObject.result(realm, value, true), false);
                    return Js.UNDEFINED;
                }
            }), realm.newFunction("", 1, new NativeFunction.Call() {
                /** Rejects the return if cancellation failed. / 취소 실패 시 반환을 거부한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    settle(operation, Js.arg(args, 0), true);
                    return Js.UNDEFINED;
                }
            }));
        } catch (JsException e) {
            if (reader.stream != null) {
                stream.release();
            }
            settle(operation, e.getValue(realm), true);
        }
    }

    /**
     * Settles an operation and removes the queue tail when it is the last one.
     * <p>
     * 동작을 확정하고 마지막 동작이면 대기열의 끝 참조를 제거한다.
     *
     * @param operation operation to settle / 확정할 동작
     * @param value result or error / 결과 또는 오류
     * @param failed whether this operation failed / 이 동작이 실패했는지 여부
     */
    private void settle(Promises.Deferred operation, JsValue value, boolean failed) {
        if (tail == operation) {
            tail = null;
        }
        if (failed) {
            operation.reject(value);
        } else {
            operation.resolve(value);
        }
    }
}
