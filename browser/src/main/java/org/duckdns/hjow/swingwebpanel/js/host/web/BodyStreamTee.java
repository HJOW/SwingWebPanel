package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;

/**
 * Splits a live Fetch body into two independently readable byte streams for response cloning.
 * <p>
 * 응답을 복제할 때 도착하는 Fetch 본문을 각각 읽을 수 있는 두 바이트 스트림으로 나눈다.
 */
final class BodyStreamTee extends BodyStreamReader {
    /** First branch of the body. / 본문의 첫 번째 갈래. */
    final Branch first;
    /** Second branch of the body. / 본문의 두 번째 갈래. */
    final Branch second;
    /** Cancellation result shared by both branches once one cancels. / 한 갈래가 취소하면 두 갈래가 공유하는 취소 결과. */
    private Promises.Deferred cancellation;
    /** Whether the source already ended. / 소스가 이미 끝났는지 여부. */
    private boolean finished;
    /** Whether both branches handed cancellation to the source. / 두 갈래가 소스에 취소를 넘겼는지 여부. */
    private boolean cancellingInput;

    /**
     * A source that forwards data or cancellation for one branch.
     * <p>
     * 갈래 하나의 데이터나 취소를 전달하는 소스.
     */
    final class Branch implements ReadableStreamObject.Source {
        /** The script-visible stream. / 스크립트에서 볼 수 있는 스트림. */
        final ReadableStreamObject stream;
        /** The cancellation reason, or Java null if this branch is active. / 취소 이유. 활성 갈래이면 Java null. */
        private JsValue cancelled;

        /**
         * Creates one branch of the tee.
         * <p>
         * 분기 스트림의 갈래 하나를 만든다.
         *
         * @param streams stream factory / 스트림 팩토리
         */
        Branch(ReadableStreams streams) {
            stream = streams.incoming();
            stream.source = this;
        }

        /** Requests another read when either branch needs data. / 갈래 중 하나가 데이터를 필요로 하면 다시 읽기를 요청한다. */
        public void request() {
            BodyStreamTee.this.request();
        }

        /**
         * Stops this branch and cancels the input once both branches have cancelled.
         * <p>
         * 이 갈래를 중단한다. 두 갈래가 모두 취소됐을 때 입력을 취소한다.
         *
         * @param reason cancellation reason / 취소 이유
         * @return the shared cancellation promise / 공용 취소 프라미스
         */
        public JsValue cancel(JsValue reason) {
            cancelled = reason;
            if (cancellation == null) {
                cancellation = Promises.create(input.realm);
            }
            if (finished) {
                cancellation.resolve(Js.UNDEFINED);
            } else if (first.cancelled != null && second.cancelled != null) {
                JsValue reasons = input.realm.newArray(new JsValue[] { first.cancelled, second.cancelled });
                cancellingInput = true;
                cancellation.resolve(input.cancel(reasons));
            }
            return cancellation.promise;
        }

        /**
         * Enqueues this branch's independent byte chunk.
         * <p>
         * 이 갈래의 독립적인 바이트 청크를 큐에 넣는다.
         *
         * @param bytes the branch's own bytes / 갈래 자신의 바이트
         */
        void accept(byte[] bytes) {
            if (cancelled == null) {
                stream.enqueue(TypedArrays.newUint8Array(input.realm, bytes), bytes.length);
            }
        }

        /**
         * Ends this branch with the result of the input.
         * <p>
         * 입력 결과와 함께 이 갈래를 끝낸다.
         *
         * @param error reason, or Java null on success / 이유. 성공하면 Java null
         */
        void finish(JsValue error) {
            if (cancelled == null) {
                if (error == null) {
                    stream.close();
                } else {
                    stream.error(error);
                }
            }
        }
    }

    /**
     * Locks the original body and creates both branches.
     * <p>
     * 원본 본문을 잠그고 양쪽 갈래를 만든다.
     *
     * @param streams reader and stream factory / 리더와 스트림 팩토리
     * @param input original body / 원본 본문
     */
    BodyStreamTee(ReadableStreams streams, ReadableStreamObject input) {
        super(streams, input);
        first = new Branch(streams);
        second = new Branch(streams);
    }

    /** Requests input only when a branch is waiting. / 갈래가 기다릴 때만 입력을 요청한다. */
    boolean needsData() {
        return first.stream.hasPendingRead() || second.stream.hasPendingRead();
    }

    /** Delivers independently mutable copies to the branches. / 갈래에 따로 바꿀 수 있는 사본을 전달한다. */
    void accept(byte[] bytes) {
        byte[] secondBytes = first.cancelled == null && second.cancelled == null ? bytes.clone() : bytes;
        first.accept(bytes);
        second.accept(secondBytes);
    }

    /** Finishes both branches. / 두 갈래를 끝낸다. */
    void finish(JsValue error) {
        finished = true;
        first.finish(error);
        second.finish(error);
        if (cancellation != null && !cancellingInput) {
            cancellation.resolve(Js.UNDEFINED);
        }
    }
}
