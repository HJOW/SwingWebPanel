package org.duckdns.hjow.swingwebpanel.js.host;

/**
 * The streaming companion of {@link WebRequestSource} for requests whose answer is not a finished body but a stream that stays open and is
 * read while it arrives (the event streams of <code>EventSource</code>). A component that implements {@link WebRequestSource} also implements
 * this interface when it can stream; a page checks it with <code>instanceof</code> and an <code>EventSource</code> of a page without it fails like a
 * connection that cannot be made. The request goes through the same common request layer as every other page request (User-Agent, extra
 * headers, cookies according to the credentials mode, SSL hook, scheme limits, same-origin policy and CORS, cancellation), but it does not use up
 * one of the few concurrent request slots for as long as it stays open, and it is not read into memory.
 * <p>
 * {@link WebRequestSource} 의 스트리밍 짝. 응답이 끝난 본문이 아니라 열려 있으면서 도착하는 대로 읽는 스트림인 요청(<code>EventSource</code> 의
 * 이벤트 스트림)을 위한 것이다. {@link WebRequestSource} 를 구현한 컴포넌트가 스트리밍할 수 있으면 이 인터페이스도 구현하며, 페이지는
 * <code>instanceof</code> 로 확인한다. 이것이 없는 페이지의 <code>EventSource</code> 는 만들 수 없는 연결처럼 실패한다. 요청은 다른 모든 페이지
 * 요청과 같은 공통 요청 계층(User-Agent, 추가 헤더, 자격 증명 모드에 따른 쿠키, SSL 훅, 스킴 제한, 동일 출처 정책과 CORS, 취소)을 거치지만 열려
 * 있는 동안 소수의 동시 요청 칸 하나를 차지하지 않고 메모리에 읽지도 않는다.
 */
public interface StreamingWebRequestSource {

    /**
     * Receives a stream on a request thread; every method must only hand the data over (no JavaScript runs here).
     * <p>
     * 요청 스레드에서 스트림을 받는다. 모든 메서드는 데이터를 넘기기만 해야 한다(여기서 JavaScript 를 실행하지 않는다).
     */
    interface Listener {
        /**
         * Called once when the response head of the final non-error response (status below 400) is known and passed the page rules (the
         * same-origin policy and CORS): the listener decides whether the body is wanted.
         * <p>
         * 최종 비오류 응답(상태 400 미만)의 응답 머리를 알고 페이지 규칙(동일 출처 정책과 CORS)을 통과했을 때 한 번 호출된다. 본문을 받을지 정한다.
         *
         * @param head the response without a body / 본문 없는 응답
         * @return true to receive the body through {@link #onData(byte[], int)}; false to refuse it (the stream then ends with
         *         {@link #onEnd(WebResponse)} carrying this head) / 본문을 {@link #onData(byte[], int)} 로 받으려면 true. 거부하면 false 이며
         *         스트림은 이 머리를 담은 {@link #onEnd(WebResponse)} 로 끝난다
         */
        boolean onHeaders(WebResponse head);

        /**
         * Called each time a piece of the body arrives (already decompressed).
         * <p>
         * 본문의 조각이 도착할 때마다 호출된다(이미 압축이 풀려 있다).
         *
         * @param data   the bytes; the array belongs to the listener / 바이트. 배열은 받는 쪽의 것이다
         * @param length how many bytes of the array are valid / 배열에서 유효한 바이트 수
         */
        void onData(byte[] data, int length);

        /**
         * Called exactly once when the stream ended: with a network error (<code>isError()</code>; <code>isCancelled()</code> tells a cancelled
         * request), with the head that the listener refused or an error status (a response that is not an error), or with the final response
         * of a body that ended normally (it carries the status and the headers, no body).
         * <p>
         * 스트림이 끝났을 때 정확히 한 번 호출된다: 네트워크 오류(<code>isError()</code>, <code>isCancelled()</code> 는 취소된 요청), 받는 쪽이 거부한 머리
         * 또는 오류 상태(오류가 아닌 응답), 또는 정상적으로 끝난 본문의 최종 응답(상태와 헤더를 담고 본문은 없다).
         *
         * @param end the end of the stream / 스트림의 끝
         */
        void onEnd(WebResponse end);
    }

    /**
     * Starts a streaming GET request on a request thread of the component and returns at once.
     * <p>
     * 컴포넌트의 요청 스레드에서 스트리밍 GET 요청을 시작하고 곧바로 돌아온다.
     *
     * @param request  the request / 요청
     * @param listener the receiver / 받는 쪽
     * @return the handle for cancelling; cancelling ends the stream with an error that <code>isCancelled()</code> / 취소용 핸들. 취소하면 스트림은
     *         <code>isCancelled()</code> 인 오류로 끝난다
     */
    WebRequestSource.Call startStream(WebRequest request, Listener listener);
}
