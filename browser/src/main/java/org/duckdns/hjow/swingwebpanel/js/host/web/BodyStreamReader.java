package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;

/**
 * Reads a Fetch body incrementally for internal consumers, without recursive reads or script calls per chunk.
 * <p>
 * 청크마다 읽기를 재귀적으로 호출하거나 스크립트를 실행하지 않고 내부 소비자에게 Fetch 본문을 점진적으로 읽어 준다.
 */
abstract class BodyStreamReader implements ReadableStreamObject.ReadRequest {
    /** Locked input stream. / 잠긴 입력 스트림. */
    final ReadableStreamObject input;
    /** Whether a read or synchronous read loop is active. / 읽기나 동기 읽기 반복이 진행 중인지 여부. */
    private boolean waiting;
    /** Whether a synchronous read loop is active. / 동기 읽기 반복이 실행 중인지 여부. */
    private boolean reading;
    /** Whether the input has already ended. / 입력이 이미 종료됐는지 여부. */
    private boolean ended;

    /**
     * Takes ownership of an input stream's lock.
     * <p>
     * 입력 스트림의 잠금을 보유한다.
     *
     * @param streams reader factory / 리더 팩토리
     * @param input input stream / 입력 스트림
     */
    BodyStreamReader(ReadableStreams streams, ReadableStreamObject input) {
        this.input = input;
        streams.lock(input);
    }

    /**
     * Starts an outstanding read when the consumer can accept more data.
     * <p>
     * 소비자가 데이터를 더 받을 수 있을 때 읽기를 시작한다.
     */
    final void request() {
        if (reading) {
            return;
        }
        reading = true;
        try {
            // 큐가 차 있으면 바로 이어서 읽고 청크가 아직 없으면 결과를 기다린다.
            while (!ended && !waiting && needsData()) {
                waiting = true;
                input.read(this);
            }
        } finally {
            reading = false;
        }
    }

    /**
     * Indicates whether the consumer needs another chunk.
     * <p>
     * 소비자에게 청크가 더 필요한지 알려 준다.
     *
     * @return true when another chunk is needed / 청크가 더 필요하면 true
     */
    abstract boolean needsData();

    /**
     * Accepts copied bytes from the input.
     * <p>
     * 입력에서 복사한 바이트를 받는다.
     *
     * @param bytes take ownership of these bytes / 이 바이트의 소유권을 받음
     */
    abstract void accept(byte[] bytes);

    /**
     * Receives the final outcome.
     * <p>
     * 최종 결과를 받는다.
     *
     * @param error failure, or Java null on success / 실패 값. 성공하면 Java null
     */
    abstract void finish(JsValue error);

    /**
     * Validates and copies the input chunk for internal consumption.
     * <p>
     * 내부 소비용으로 입력 청크를 검사하고 복사한다.
     *
     * @param chunk stream data / 스트림 데이터
     */
    public final void onChunk(JsValue chunk) {
        waiting = false;
        try {
            // Fetch 본문 소비는 필수 Uint8Array 바이트 청크만 받아들인다.
            if (!(chunk instanceof TypedArrayObject) || ((TypedArrayObject) chunk).getType() != ElementType.UINT8) {
                throw JsException.typeError("A Fetch body chunk must be a Uint8Array");
            }
            accept(TypedArrays.copyBytes(chunk));
        } catch (JsException e) {
            JsValue error = e.getValue(input.realm);
            onError(error);
            PromiseInterop.await(input.realm, input.cancel(error), Js.UNDEFINED, Js.UNDEFINED);
        }
        request();
    }

    /** Reports a successful end of input. / 입력의 성공 종료를 알린다. */
    public final void onEnd() {
        ended = true;
        finish(null);
    }

    /** Reports a failed input. / 입력 실패를 알린다. */
    public final void onError(JsValue error) {
        ended = true;
        finish(error);
    }
}
