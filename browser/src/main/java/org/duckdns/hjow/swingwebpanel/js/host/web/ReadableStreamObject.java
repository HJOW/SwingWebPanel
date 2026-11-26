package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayDeque;
import java.util.Deque;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * The state shared by a readable stream, its source and its single default reader. Use it on its realm's thread.
 * <p>
 * 읽기 스트림, 소스와 단일 기본 리더가 공유하는 상태. 해당 렐름의 스레드에서 사용한다.
 */
final class ReadableStreamObject extends JsObject {
    /**
     * The source's pull and cancel operations.
     * <p>
     * 소스의 pull과 cancel 동작.
     */
    interface Source {
        /** Requests data if the source is ready. / 소스가 준비되면 데이터를 요청한다. */
        void request();

        /**
         * Cancels the source after the stream closes.
         * <p>
         * 스트림이 닫힌 뒤 소스를 취소한다.
         *
         * @param reason cancellation reason / 취소 이유
         * @return a promise for source cleanup / 소스의 정리 프라미스
         */
        JsValue cancel(JsValue reason);
    }

    /** A pending read's steps, executed on the realm thread. / 렐름 스레드에서 실행하는 대기 읽기의 단계. */
    interface ReadRequest {
        /** Delivers the given chunk. / 주어진 청크를 전달한다. */
        void onChunk(JsValue chunk);

        /** Reports the end of the stream. / 스트림의 끝을 알린다. */
        void onEnd();

        /** Reports the stored failure or release error. / 보관한 실패 또는 해제 오류를 알린다. */
        void onError(JsValue reason);
    }

    /** One queued chunk and its size in the queuing strategy. / 대기 중인 청크와 큐 전략에서 정한 크기. */
    private static final class Chunk {
        /** Chunk value. / 청크 값. */
        final JsValue value;
        /** Size for backpressure. / 흐름 제어에 쓰는 크기. */
        final double size;

        /**
         * Creates a queued chunk.
         * <p>
         * 대기 청크를 만든다.
         *
         * @param value chunk value / 청크 값
         * @param size queue size / 큐 크기
         */
        Chunk(JsValue value, double size) {
            this.value = value;
            this.size = size;
        }
    }

    /**
     * The currently attached reader or a reader released by its caller.
     * <p>
     * 현재 붙은 리더 또는 호출자가 해제한 리더.
     */
    static final class Reader extends JsObject {
        /** Stream while locked by this reader. / 이 리더로 잠근 동안의 스트림. */
        ReadableStreamObject stream;
        /** Closure or failure promise, replaced when a settled reader is released. / 닫힘 또는 실패 프라미스. 확정 뒤 리더를 해제할 때 교체한다. */
        Promises.Deferred closed;

        /**
         * Locks the given stream and records its closure state.
         * <p>
         * 스트림을 잠그고 닫힘 상태를 기록한다.
         *
         * @param prototype reader prototype / 리더의 프로토타입
         * @param stream stream to lock / 잠글 스트림
         */
        Reader(JsObject prototype, ReadableStreamObject stream) {
            super(prototype);
            // 이미 리더가 붙은 경우 기존 잠금을 건드리지 않는다.
            if (stream.reader != null) {
                throw JsException.typeError("The stream is already locked to a reader");
            }
            this.stream = stream;
            closed = Promises.create(stream.realm);
            stream.reader = this;
            if (stream.failure != null) {
                stream.rejectClosed(this, stream.failure);
            } else if (stream.closed) {
                closed.resolve(Js.UNDEFINED);
            }
        }
    }

    /** Realm that creates the result objects and promises. / 결과 객체와 프라미스를 만드는 렐름. */
    final JsRealm realm;
    /** Chunks awaiting reads. / 읽기를 기다리는 청크. */
    private final Deque<Chunk> chunks = new ArrayDeque<Chunk>();
    /** Reads made by the current reader without an available chunk. / 당장 받을 청크가 없는 현재 리더의 읽기. */
    private final Deque<ReadRequest> reads = new ArrayDeque<ReadRequest>();
    /** The target queue size. / 목표 큐 크기. */
    private final double highWaterMark;
    /** Total size of buffered chunks. / 대기 청크의 총 크기. */
    private double queueSize;
    /** Source while the stream is open. / 스트림이 열린 동안의 소스. */
    Source source;
    /** Reader with exclusive access. / 독점 접근 리더. */
    Reader reader;
    /** Whether reading or cancellation has begun. / 읽기나 취소가 시작되었는지 여부. */
    boolean disturbed;
    /** Whether this stream will close when its queue empties. / 대기 큐가 비면 스트림이 닫히는지 여부. */
    private boolean closeRequested;
    /** Whether readers have reached the end. / 리더가 끝에 도달했는지 여부. */
    private boolean closed;
    /** Stored error, or Java null while there is no error. / 저장된 오류. 오류가 없으면 Java null. */
    private JsValue failure;

    /**
     * Creates an open stream.
     * <p>
     * 열린 스트림을 만든다.
     *
     * @param realm owner realm / 소유 렐름
     * @param prototype stream prototype / 스트림 프로토타입
     * @param highWaterMark target queue size / 목표 큐 크기
     */
    ReadableStreamObject(JsRealm realm, JsObject prototype, double highWaterMark) {
        super(prototype);
        this.realm = realm;
        this.highWaterMark = highWaterMark;
    }

    /**
     * Builds a read result in this realm.
     * <p>
     * 이 렐름에 읽기 결과를 만든다.
     *
     * @param realm result realm / 결과 렐름
     * @param value chunk or undefined / 청크 또는 undefined
     * @param done end of stream flag / 스트림 끝 여부
     * @return the result / 결과
     */
    static JsObject result(JsRealm realm, JsValue value, boolean done) {
        JsObject result = realm.newObject();
        result.defineData("value", value, true, true, true);
        result.defineData("done", Js.bool(done), true, true, true);
        return result;
    }

    /**
     * Determines whether controller operations may add more data.
     * <p>
     * 컨트롤러 동작이 데이터를 더 추가할 수 있는지 확인한다.
     *
     * @return true before close or error / 닫힘이나 오류 전이면 true
     */
    boolean acceptsChunks() {
        return !closeRequested && failure == null && !closed;
    }

    /**
     * Returns the amount of capacity desired by the queue.
     * <p>
     * 큐에 필요한 용량을 반환한다.
     *
     * @return available capacity, null on error / 여유 용량. 오류 시 null
     */
    JsValue desiredSize() {
        return failure != null ? Js.NULL : Js.num(closed ? 0 : highWaterMark - queueSize);
    }

    /**
     * Tests whether to request data from the underlying source.
     * <p>
     * 하위 소스에 데이터를 요청할지 검사한다.
     *
     * @return true when the open stream has demand / 열린 스트림이 데이터를 필요로 하면 true
     */
    boolean needsData() {
        return acceptsChunks() && (!reads.isEmpty() || highWaterMark > queueSize);
    }

    /**
     * Determines whether a reader is already waiting for the next chunk.
     * <p>
     * 다음 청크를 기다리는 리더가 있는지 확인한다.
     *
     * @return true when a read would bypass the queue / 읽기가 큐를 우회하면 true
     */
    boolean hasPendingRead() {
        return !reads.isEmpty();
    }

    /**
     * Delivers a chunk, or buffers it until a reader requests it.
     * <p>
     * 청크를 전달하거나 리더가 요청할 때까지 버퍼에 보관한다.
     *
     * @param value the chunk / 청크
     * @param size queue size if no read is pending / 대기 중인 읽기가 없을 때의 큐 크기
     */
    void enqueue(JsValue value, double size) {
        // 대기 중인 읽기는 큐에 넣지 않고 바로 확정한다.
        if (hasPendingRead()) {
            reads.removeFirst().onChunk(value);
        } else {
            chunks.addLast(new Chunk(value, size));
            queueSize += size;
        }
        requestData();
    }

    /**
     * Closes the source, retaining chunks already queued for reading.
     * <p>
     * 소스를 닫고 이미 대기 중인 청크는 읽을 수 있게 남긴다.
     */
    void close() {
        closeRequested = true;
        if (chunks.isEmpty()) {
            finish();
        }
    }

    /**
     * Commits a closed stream and settles the pending reads.
     * <p>
     * 스트림을 닫고 대기 중인 읽기를 확정한다.
     */
    private void finish() {
        closed = true;
        source = null;
        if (reader != null) {
            reader.closed.resolve(Js.UNDEFINED);
        }
        // 닫힐 때 기다리던 읽기에 끝을 각각 알린다.
        while (!reads.isEmpty()) {
            reads.removeFirst().onEnd();
        }
    }

    /**
     * Stores an error and discards queued chunks.
     * <p>
     * 오류를 저장하고 대기 청크를 비운다.
     *
     * @param reason the error or rejection reason / 오류나 거부 이유
     */
    void error(JsValue reason) {
        // 닫혔거나 이미 오류가 난 스트림의 최종 상태를 보존한다.
        if (closed || failure != null) {
            return;
        }
        failure = reason;
        source = null;
        chunks.clear();
        queueSize = 0;
        if (reader != null) {
            rejectClosed(reader, reason);
        }
        rejectReads(reason);
    }

    /**
     * Reads once with the active reader and requests more data if needed.
     * <p>
     * 활성 리더로 한 번 읽고 필요하면 데이터를 더 요청한다.
     *
     * @return the read promise / 읽기 프라미스
     */
    JsObject read() {
        final Promises.Deferred promise = Promises.create(realm);
        read(new ReadRequest() {
            /** Resolves with a chunk. / 청크로 이행한다. */
            public void onChunk(JsValue chunk) {
                promise.resolve(result(realm, chunk, false));
            }

            /** Resolves with the end result. / 종료 결과로 이행한다. */
            public void onEnd() {
                promise.resolve(result(realm, Js.UNDEFINED, true));
            }

            /** Rejects with the stream or lock failure. / 스트림 또는 잠금 오류로 거부한다. */
            public void onError(JsValue reason) {
                promise.reject(reason);
            }
        });
        return promise.promise;
    }

    /**
     * Applies the caller's read steps to the next chunk, end or failure.
     * <p>
     * 다음 청크, 종료 또는 실패에 호출자의 읽기 단계를 적용한다.
     *
     * @param request the read steps / 읽기 단계
     */
    void read(ReadRequest request) {
        disturbed = true;
        if (failure != null) {
            request.onError(failure);
            return;
        }
        if (closed) {
            request.onEnd();
            return;
        }
        Chunk chunk = chunks.pollFirst();
        // 이미 큐에 든 청크를 먼저 돌려주고 소스에서 더 받을지 결정한다.
        if (chunk == null) {
            reads.addLast(request);
        } else {
            queueSize = chunks.isEmpty() ? 0 : Math.max(0, queueSize - chunk.size);
            if (closeRequested && chunks.isEmpty()) {
                finish();
            }
            request.onChunk(chunk.value);
        }
        requestData();
    }

    /**
     * Cancels the stream and returns the source's cancellation result.
     * <p>
     * 스트림을 취소하고 소스의 취소 결과를 반환한다.
     *
     * @param reason cancellation reason / 취소 이유
     * @return cancellation promise / 취소 프라미스
     */
    JsValue cancel(JsValue reason) {
        disturbed = true;
        if (failure != null) {
            return Promises.rejected(realm, failure);
        }
        if (closed) {
            return Promises.resolved(realm, Js.UNDEFINED);
        }
        Source current = source;
        chunks.clear();
        queueSize = 0;
        finish();
        return current == null ? Promises.resolved(realm, Js.UNDEFINED) : current.cancel(reason);
    }

    /**
     * Releases the reader and rejects its pending reads, keeping buffered chunks available for a new reader.
     * <p>
     * 리더를 해제하고 그 읽기를 거부한다. 대기 청크는 새 리더가 읽을 수 있다.
     */
    void release() {
        Reader old = reader;
        // 이미 확정된 closed 프라미스는 바꿀 수 없으므로 다음 조회에 새 거부를 돌려준다.
        if (closed || failure != null) {
            old.closed = Promises.create(realm);
        }
        JsValue reason = JsException.typeError("The reader lock was released").getValue(realm);
        reader = null;
        old.stream = null;
        rejectClosed(old, reason);
        rejectReads(reason);
    }

    /**
     * Marks the internal closed promise handled and rejects it.
     * <p>
     * 내부 closed 프라미스를 처리된 것으로 표시하고 거부한다.
     *
     * @param owner the reader / 리더
     * @param reason rejection reason / 거부 이유
     */
    private void rejectClosed(Reader owner, JsValue reason) {
        PromiseInterop.await(realm, owner.closed.promise, Js.UNDEFINED, Js.UNDEFINED);
        owner.closed.reject(reason);
    }

    /**
     * Rejects and removes all outstanding read promises.
     * <p>
     * 대기 중인 모든 읽기 프라미스를 거부하고 제거한다.
     *
     * @param reason rejection reason / 거부 이유
     */
    private void rejectReads(JsValue reason) {
        // 대기 순서대로 각 읽기를 거부한다.
        while (!reads.isEmpty()) {
            reads.removeFirst().onError(reason);
        }
    }

    /** Requests a source pull when a source is still attached. / 소스가 연결되어 있으면 pull을 요청한다. */
    private void requestData() {
        if (source != null) {
            source.request();
        }
    }
}
