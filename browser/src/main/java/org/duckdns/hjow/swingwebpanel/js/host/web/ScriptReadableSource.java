package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * The default controller for a script-created stream with an optional underlying source.
 * <p>
 * 선택 하위 소스로 스크립트에서 만든 스트림의 기본 컨트롤러.
 */
final class ScriptReadableSource extends JsObject implements ReadableStreamObject.Source {
    /** Stream receiving the data. / 데이터를 받는 스트림. */
    final ReadableStreamObject stream;
    /** Receiver of the underlying source methods. / 하위 소스 메서드의 수신 객체. */
    private final JsValue underlying;
    /** Source pull callback. / 소스 pull 콜백. */
    private final JsValue pull;
    /** Source cancel callback. / 소스 cancel 콜백. */
    private final JsValue cancel;
    /** Strategy's optional size algorithm. / 전략의 선택 크기 알고리즘. */
    private final JsValue size;
    /** True after the start callback's result resolves. / start 콜백 결과가 이행된 뒤 true. */
    private boolean started;
    /** True while the current pull is pending. / 현재 pull이 대기 중이면 true. */
    private boolean pulling;
    /** Whether another pull was requested before the current pull settled. / 현재 pull 확정 전에 다음 pull을 요청했는지 여부. */
    private boolean pullAgain;

    /**
     * Captures the source's callbacks for this stream.
     * <p>
     * 이 스트림의 소스 콜백을 보관한다.
     *
     * @param prototype controller prototype / 컨트롤러 프로토타입
     * @param stream owner stream / 소유 스트림
     * @param underlying source receiver / 소스 수신 객체
     * @param pull pull callback or undefined / pull 콜백 또는 undefined
     * @param cancel cancel callback or undefined / cancel 콜백 또는 undefined
     * @param size size callback or undefined / size 콜백 또는 undefined
     */
    ScriptReadableSource(JsObject prototype, ReadableStreamObject stream, JsValue underlying, JsValue pull, JsValue cancel, JsValue size) {
        super(prototype);
        this.stream = stream;
        this.underlying = underlying;
        this.pull = pull;
        this.cancel = cancel;
        this.size = size;
    }

    /**
     * Calls the source's start hook and waits for it before pulling.
     * <p>
     * 소스 start 콜백을 실행하고 이행을 기다린 뒤 pull을 시작한다.
     *
     * @param start the callback or undefined / 콜백 또는 undefined
     */
    void start(JsValue start) {
        JsValue result = start.isUndefined() ? Js.UNDEFINED : Ops.call(start, underlying, new JsValue[] { this });
        PromiseInterop.await(stream.realm, result, stream.realm.newFunction("", 1, new NativeFunction.Call() {
            /** Enables data pulls after start. / 시작 후 데이터 pull을 허용한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                started = true;
                request();
                return Js.UNDEFINED;
            }
        }), errorHandler());
    }

    /**
     * Adds the chunk, computing the size only when it needs to wait in the queue.
     * <p>
     * 청크를 추가한다. 큐에서 기다려야 할 때만 크기를 계산한다.
     *
     * @param chunk value to enqueue / 큐에 넣을 값
     */
    void enqueue(JsValue chunk) {
        if (!stream.acceptsChunks()) {
            throw JsException.typeError("The stream controller cannot enqueue");
        }
        double measured = 1;
        // 대기 중인 읽기에는 크기 전략을 호출하지 않고 바로 전달한다.
        if (!size.isUndefined() && !stream.hasPendingRead()) {
            try {
                measured = Ops.toNumber(Ops.call(size, Js.UNDEFINED, new JsValue[] { chunk }));
                if (Double.isNaN(measured) || Double.isInfinite(measured) || measured < 0) {
                    throw JsException.rangeError("The stream chunk size must be finite and nonnegative");
                }
            } catch (JsException e) {
                stream.error(e.getValue(stream.realm));
                throw e;
            }
        }
        if (stream.acceptsChunks()) {
            stream.enqueue(chunk, measured);
        }
    }

    /** Pulls more data when demand exists and no other pull is running. / 필요량이 있고 실행 중인 pull이 없으면 데이터를 더 가져온다. */
    public void request() {
        // 시작 중이거나 대기 용량이 없으면 pull하지 않는다.
        if (!started || pull.isUndefined() || !stream.needsData()) {
            return;
        }
        if (pulling) {
            pullAgain = true;
            return;
        }
        pulling = true;
        try {
            JsValue result = Ops.call(pull, underlying, new JsValue[] { this });
            PromiseInterop.await(stream.realm, result, stream.realm.newFunction("", 1, new NativeFunction.Call() {
                /** Allows another pull after the current one settles. / 현재 pull 확정 뒤 다음 pull을 허용한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    pulling = false;
                    if (pullAgain) {
                        pullAgain = false;
                        request();
                    }
                    return Js.UNDEFINED;
                }
            }), errorHandler());
        } catch (JsException e) {
            stream.error(e.getValue(stream.realm));
        }
    }

    /**
     * Calls the source's cancel hook. Only its completion or failure is exposed to the reader.
     * <p>
     * 소스 cancel 콜백을 호출한다. 완료 또는 실패만 리더에 알린다.
     *
     * @param reason cancellation reason / 취소 이유
     * @return promise for cancellation / 취소 프라미스
     */
    public JsValue cancel(JsValue reason) {
        if (cancel.isUndefined()) {
            return Promises.resolved(stream.realm, Js.UNDEFINED);
        }
        final Promises.Deferred done = Promises.create(stream.realm);
        try {
            JsValue result = Ops.call(cancel, underlying, new JsValue[] { reason });
            PromiseInterop.await(stream.realm, result, stream.realm.newFunction("", 1, new NativeFunction.Call() {
                /** Completes cancellation. / 취소를 완료한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    done.resolve(Js.UNDEFINED);
                    return Js.UNDEFINED;
                }
            }), stream.realm.newFunction("", 1, new NativeFunction.Call() {
                /** Passes the source's cancellation error to the reader. / 소스의 취소 오류를 리더에 전달한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    done.reject(Js.arg(args, 0));
                    return Js.UNDEFINED;
                }
            }));
        } catch (JsException e) {
            done.reject(e.getValue(stream.realm));
        }
        return done.promise;
    }

    /**
     * Builds a rejection handler to error the stream if start or pull failed.
     * <p>
     * start 또는 pull 실패 시 스트림에 오류를 지정하는 거부 핸들러를 만든다.
     *
     * @return rejection handler / 거부 핸들러
     */
    private NativeFunction errorHandler() {
        return stream.realm.newFunction("", 1, new NativeFunction.Call() {
            /** Records the asynchronous error. / 비동기 오류를 기록한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                stream.error(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
    }
}
