package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;
import org.duckdns.hjow.swingwebpanel.js.host.web.ReadableStreamObject.Reader;

/**
 * Installs the default readable stream, controller, reader and asynchronous iterator interfaces.
 * <p>
 * 기본 읽기 스트림, 컨트롤러, 리더와 비동기 반복자 인터페이스를 설치한다.
 */
final class ReadableStreams {
    /** Maximum byte count delivered in one buffered-body chunk. / 버퍼 본문의 청크 하나로 전달할 최대 바이트 수. */
    private static final int BODY_CHUNK_SIZE = 65536;

    /** Realm receiving the interfaces. / 인터페이스를 받는 렐름. */
    final JsRealm realm;
    /** ReadableStream prototype. / ReadableStream 프로토타입. */
    private JsObject streamPrototype;
    /** ReadableStreamDefaultReader prototype. / ReadableStreamDefaultReader 프로토타입. */
    private JsObject readerPrototype;
    /** ReadableStreamDefaultController prototype. / ReadableStreamDefaultController 프로토타입. */
    private JsObject controllerPrototype;
    /** Shared stream iterator prototype. / 공용 스트림 반복자 프로토타입. */
    private JsObject iteratorPrototype;

    /**
     * Delivers a buffered Fetch body on demand using independent script-visible chunks.
     * <p>
     * 요청이 있을 때 버퍼에 담긴 Fetch 본문을 스크립트에서 독립적으로 바꿀 수 있는 청크로 전달한다.
     */
    private static final class BufferedBody implements ReadableStreamObject.Source {
        /** Stream to read from. / 읽을 스트림. */
        private final ReadableStreamObject stream;
        /** Immutable input bytes. / 변경하지 않는 입력 바이트. */
        private final byte[] data;
        /** Position of the next unread byte. / 읽지 않은 다음 바이트의 위치. */
        private int position;

        /**
         * Prepares the buffered body.
         * <p>
         * 버퍼에 담긴 본문을 준비한다.
         *
         * @param stream receiving stream / 받는 스트림
         * @param data body bytes / 본문의 바이트
         */
        BufferedBody(ReadableStreamObject stream, byte[] data) {
            this.stream = stream;
            this.data = data;
        }

        /** Delivers a chunk to each waiting read, then signals end of body. / 대기하는 읽기에 청크를 전달하고 본문의 끝을 알린다. */
        public void request() {
            // 읽기 요청이 있을 때만 복사하며 복제한 응답의 원본 바이트는 공유하지 않는다.
            if (!stream.acceptsChunks() || !stream.hasPendingRead()) {
                return;
            }
            int count = Math.min(BODY_CHUNK_SIZE, data.length - position);
            if (count > 0) {
                byte[] chunk = new byte[count];
                System.arraycopy(data, position, chunk, 0, count);
                position += count;
                stream.enqueue(TypedArrays.newUint8Array(stream.realm, chunk), count);
            }
            if (position == data.length && stream.acceptsChunks()) {
                stream.close();
            }
        }

        /** Settles cancellation of a buffered source. / 버퍼 소스 취소를 확정한다. */
        public JsValue cancel(JsValue reason) {
            return Promises.resolved(stream.realm, Js.UNDEFINED);
        }
    }

    /**
     * Creates the installer for one realm.
     * <p>
     * 렐름 하나에 대한 설치기를 만든다.
     *
     * @param realm owner realm / 소유 렐름
     */
    ReadableStreams(JsRealm realm) {
        this.realm = realm;
    }

    /**
     * Installs the interfaces for the realm's global object.
     * <p>
     * 렐름의 전역 객체에 인터페이스를 설치한다.
     *
     * @param global global object / 전역 객체
     */
    void install(JsObject global) {
        streamPrototype = Natives.defineClass(realm, global, "ReadableStream", 0, null, new NativeFunction.Construct() {
            /** Constructs a stream from its source and strategy. / 소스와 전략에서 스트림을 만든다. */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return constructStream(Ops.getPrototypeFromConstructor(r, newTarget, streamPrototype), args);
            }
        }).prototype;
        readerPrototype = Natives.defineClass(realm, global, "ReadableStreamDefaultReader", 1, null, new NativeFunction.Construct() {
            /** Locks the supplied stream. / 지정한 스트림을 잠근다. */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return new Reader(Ops.getPrototypeFromConstructor(r, newTarget, readerPrototype),
                        Natives.self(Js.arg(args, 0), ReadableStreamObject.class));
            }
        }).prototype;
        controllerPrototype = Natives.defineClass(realm, global, "ReadableStreamDefaultController", 0, null, null).prototype;
        iteratorPrototype = new JsObject((JsObject) realm.getIntrinsic("%AsyncIteratorPrototype%"));
        Natives.getter(realm, streamPrototype, "locked", this, "locked");
        Natives.method(realm, streamPrototype, "getReader", 0, this, "getReader");
        Natives.method(realm, streamPrototype, "cancel", 0, this, "cancelStream");
        NativeFunction values = Natives.function(realm, "values", 0, this, "values");
        streamPrototype.defineData("values", values, true, true, true);
        streamPrototype.defineData(WellKnownSymbols.ASYNC_ITERATOR, values, true, false, true);
        Natives.getter(realm, readerPrototype, "closed", this, "closed");
        Natives.method(realm, readerPrototype, "read", 0, this, "read");
        Natives.method(realm, readerPrototype, "cancel", 0, this, "cancelReader");
        Natives.method(realm, readerPrototype, "releaseLock", 0, this, "releaseLock");
        Natives.getter(realm, controllerPrototype, "desiredSize", this, "desiredSize");
        Natives.method(realm, controllerPrototype, "enqueue", 0, this, "enqueue");
        Natives.method(realm, controllerPrototype, "close", 0, this, "close");
        Natives.method(realm, controllerPrototype, "error", 0, this, "error");
        iteratorPrototype.defineData("next", Natives.function(realm, "next", 0, this, "next"), true, false, true);
        iteratorPrototype.defineData("return", Natives.function(realm, "return", 0, this, "returnValue"), true, false, true);
    }

    /**
     * Creates the stream of an existing immutable byte buffer.
     * <p>
     * 기존의 변경하지 않는 바이트 버퍼에서 스트림을 만든다.
     *
     * @param bytes immutable body bytes / 변경하지 않는 본문 바이트
     * @param used true if the body was already consumed / 본문을 이미 소비했으면 true
     * @return the stream / 스트림
     */
    ReadableStreamObject buffered(byte[] bytes, boolean used) {
        ReadableStreamObject stream = new ReadableStreamObject(realm, streamPrototype, 0);
        if (used) {
            consumeBuffered(stream);
        } else if (bytes.length == 0) {
            stream.close();
        } else {
            stream.source = new BufferedBody(stream, bytes);
        }
        return stream;
    }

    /**
     * Locks a stream with a reader owned by an internal algorithm.
     * <p>
     * 내부 알고리즘이 보유하는 리더로 스트림을 잠근다.
     *
     * @param stream stream to lock / 잠글 스트림
     */
    void lock(ReadableStreamObject stream) {
        new Reader(readerPrototype, stream);
    }

    /**
     * Marks a host-buffered stream used after the same bytes were consumed through a body method.
     * <p>
     * 본문 메서드가 같은 바이트를 소비하면 호스트에서 버퍼링한 스트림을 사용된 것으로 표시한다.
     *
     * @param stream stream backed by the consumed bytes / 소비한 바이트를 사용하는 스트림
     */
    void consumeBuffered(ReadableStreamObject stream) {
        lock(stream);
        stream.cancel(Js.UNDEFINED);
    }

    /**
     * Creates a default stream after converting the source and strategy arguments.
     * <p>
     * 소스와 전략 인수를 변환하고 기본 스트림을 만든다.
     *
     * @param proto stream prototype / 스트림 프로토타입
     * @param args source and strategy / 소스와 전략
     * @return new stream / 새 스트림
     */
    private ReadableStreamObject constructStream(JsObject proto, JsValue[] args) {
        JsValue source = Js.arg(args, 0);
        if (!source.isUndefined() && !source.isObject()) {
            throw JsException.typeError("The stream source must be an object");
        }
        JsValue cancel = callback(source, "cancel");
        JsValue pull = callback(source, "pull");
        JsValue start = callback(source, "start");
        JsValue type = member(source, "type");
        if (!type.isUndefined()) {
            throw JsException.typeError("Only default stream sources are supported");
        }
        JsValue strategy = Js.arg(args, 1);
        JsValue limit = member(strategy, "highWaterMark");
        double highWaterMark = limit.isUndefined() ? 1 : Ops.toNumber(limit);
        JsValue size = callback(strategy, "size");
        if (Double.isNaN(highWaterMark) || highWaterMark < 0) {
            throw JsException.rangeError("The stream highWaterMark must be nonnegative");
        }
        ReadableStreamObject stream = new ReadableStreamObject(realm, proto, highWaterMark);
        ScriptReadableSource controller = new ScriptReadableSource(controllerPrototype, stream, source, pull, cancel, size);
        stream.source = controller;
        controller.start(start);
        return stream;
    }

    /**
     * Reads a dictionary property if an object is provided.
     * <p>
     * 객체를 제공하면 사전 프로퍼티를 읽는다.
     *
     * @param object dictionary argument / 사전 인수
     * @param name property name / 프로퍼티 이름
     * @return value or undefined / 값 또는 undefined
     */
    private static JsValue member(JsValue object, String name) {
        if (object.isNullOrUndefined()) {
            return Js.UNDEFINED;
        }
        if (!(object instanceof JsObject)) {
            throw JsException.typeError("Stream options must be an object");
        }
        return ((JsObject) object).get(name);
    }

    /**
     * Gets an optional callback from a dictionary.
     * <p>
     * 사전에서 선택 콜백을 구한다.
     *
     * @param object dictionary / 사전
     * @param name callback name / 콜백 이름
     * @return function or undefined / 함수 또는 undefined
     */
    private static JsValue callback(JsValue object, String name) {
        JsValue fn = member(object, name);
        if (!fn.isUndefined() && !fn.isCallable()) {
            throw JsException.typeError("Stream callback " + name + " must be callable");
        }
        return fn;
    }

    /** Reads the stream's lock flag. / 스트림의 잠금 플래그를 읽는다. */
    JsValue locked(JsValue self, JsValue[] args) {
        return Js.bool(Natives.self(self, ReadableStreamObject.class).reader != null);
    }

    /** Locks a stream with a new default reader. / 새 기본 리더로 스트림을 잠근다. */
    JsValue getReader(JsValue self, JsValue[] args) {
        ReadableStreamObject stream = Natives.self(self, ReadableStreamObject.class);
        JsValue mode = member(Js.arg(args, 0), "mode");
        if (!mode.isUndefined()) {
            throw JsException.typeError("Unsupported stream reader mode: " + Ops.toString(mode));
        }
        return new Reader(readerPrototype, stream);
    }

    /** Cancels an unlocked stream. / 잠기지 않은 스트림을 취소한다. */
    JsValue cancelStream(JsValue self, JsValue[] args) {
        if (!(self instanceof ReadableStreamObject) || ((ReadableStreamObject) self).reader != null) {
            return Promises.rejectedTypeError(realm, "Cancelling requires an unlocked readable stream");
        }
        return ((ReadableStreamObject) self).cancel(Js.arg(args, 0));
    }

    /** Locks a stream for async iteration. / 비동기 반복을 위해 스트림을 잠근다. */
    JsValue values(JsValue self, JsValue[] args) {
        ReadableStreamObject stream = Natives.self(self, ReadableStreamObject.class);
        boolean preventCancel = Ops.toBoolean(member(Js.arg(args, 0), "preventCancel"));
        return new ReadableStreamIterator(iteratorPrototype, new Reader(readerPrototype, stream), preventCancel);
    }

    /** Returns the reader's closure promise. / 리더의 닫힘 프라미스를 반환한다. */
    JsValue closed(JsValue self, JsValue[] args) {
        return self instanceof Reader ? ((Reader) self).closed.promise : Promises.rejectedTypeError(realm, "Invalid stream reader");
    }

    /** Reads one chunk. / 청크 하나를 읽는다. */
    JsValue read(JsValue self, JsValue[] args) {
        if (!(self instanceof Reader) || ((Reader) self).stream == null) {
            return Promises.rejectedTypeError(realm, "Reading requires an active stream reader");
        }
        return ((Reader) self).stream.read();
    }

    /** Cancels the reader's stream. / 리더의 스트림을 취소한다. */
    JsValue cancelReader(JsValue self, JsValue[] args) {
        if (!(self instanceof Reader) || ((Reader) self).stream == null) {
            return Promises.rejectedTypeError(realm, "Cancelling requires an active stream reader");
        }
        return ((Reader) self).stream.cancel(Js.arg(args, 0));
    }

    /** Releases the lock held by the reader. / 리더가 보유한 잠금을 해제한다. */
    JsValue releaseLock(JsValue self, JsValue[] args) {
        Reader reader = Natives.self(self, Reader.class);
        if (reader.stream != null) {
            reader.stream.release();
        }
        return Js.UNDEFINED;
    }

    /** Reads the controller's desired queue size. / 컨트롤러 큐의 필요 크기를 읽는다. */
    JsValue desiredSize(JsValue self, JsValue[] args) {
        return Natives.self(self, ScriptReadableSource.class).stream.desiredSize();
    }

    /** Enqueues a chunk on the controller. / 컨트롤러에 청크를 넣는다. */
    JsValue enqueue(JsValue self, JsValue[] args) {
        Natives.self(self, ScriptReadableSource.class).enqueue(Js.arg(args, 0));
        return Js.UNDEFINED;
    }

    /** Closes the controller. / 컨트롤러를 닫는다. */
    JsValue close(JsValue self, JsValue[] args) {
        ReadableStreamObject stream = Natives.self(self, ScriptReadableSource.class).stream;
        if (!stream.acceptsChunks()) {
            throw JsException.typeError("The stream controller cannot close");
        }
        stream.close();
        return Js.UNDEFINED;
    }

    /** Errors the controller. / 컨트롤러에 오류를 지정한다. */
    JsValue error(JsValue self, JsValue[] args) {
        Natives.self(self, ScriptReadableSource.class).stream.error(Js.arg(args, 0));
        return Js.UNDEFINED;
    }

    /** Returns the next asynchronous iterator result. / 다음 비동기 반복자 결과를 반환한다. */
    JsValue next(JsValue self, JsValue[] args) {
        if (!(self instanceof ReadableStreamIterator)) {
            return Promises.rejectedTypeError(realm, "Invalid stream iterator");
        }
        return ((ReadableStreamIterator) self).step(false, Js.UNDEFINED);
    }

    /** Returns early from a stream iterator. / 스트림 반복자를 조기에 마친다. */
    JsValue returnValue(JsValue self, JsValue[] args) {
        if (!(self instanceof ReadableStreamIterator)) {
            return Promises.rejectedTypeError(realm, "Invalid stream iterator");
        }
        return ((ReadableStreamIterator) self).step(true, Js.arg(args, 0));
    }
}
