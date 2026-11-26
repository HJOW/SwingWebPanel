package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.OutputStream;

/**
 * The asynchronous request service of a page, parallel to {@link ScriptSource}: the component implements it with its common request
 * layer, so User-Agent, extra headers, cookies, SSL hook, scheme limits, cache, same-origin policy and cancellation apply to
 * <code>XMLHttpRequest</code> and <code>fetch()</code> exactly as they do to scripts, styles and images. A request never runs on the thread
 * that owns the page's JavaScript: {@link #start(WebRequest, Callback)} returns at once and the {@link Callback} methods are called on a
 * request thread; the page layer moves the results to its own thread as page tasks. A request that the page cancels (or that the page
 * replacement cancels) must still end with one {@link Callback#onComplete(WebResponse)} call, but the page ignores that late result.
 * <p>
 * 페이지의 비동기 요청 서비스. {@link ScriptSource} 와 나란히 있다. 컴포넌트가 공통 요청 계층으로 구현하므로 User-Agent, 추가 헤더, 쿠키, SSL 훅, 스킴 제한,
 * 캐시, 동일 출처 정책, 취소가 <code>XMLHttpRequest</code> 와 <code>fetch()</code> 에도 스크립트·스타일·이미지와 똑같이 적용된다. 요청은 페이지의
 * JavaScript 를 소유한 스레드에서 실행되지 않는다. {@link #start(WebRequest, Callback)} 는 곧바로 돌아오고 {@link Callback} 메서드는 요청 스레드에서
 * 호출되며, 결과는 페이지 계층이 자기 스레드의 페이지 작업으로 옮긴다. 페이지가 취소한(또는 페이지 교체가 취소한) 요청도 {@link Callback#onComplete(WebResponse)}
 * 를 한 번 호출하며 끝나야 하지만 페이지는 그 늦은 결과를 무시한다.
 */
public interface WebRequestSource {

    /**
     * A started request that can be cancelled.
     * <p>
     * 취소할 수 있는 시작된 요청.
     */
    interface Call {
        /**
         * Cancels the request; callable from any thread and more than once.
         * <p>
         * 요청을 취소한다. 어느 스레드에서나 여러 번 불러도 된다.
         */
        void cancel();
    }

    /**
     * Receives the progress and the result of a request; every method is called on a request thread and must only hand the data over.
     * <p>
     * 요청의 진행과 결과를 받는다. 모든 메서드는 요청 스레드에서 호출되며 데이터를 넘기기만 해야 한다.
     */
    interface Callback {
        /**
         * Called when the status line and the headers of an acceptable response are known (before the body is read). It is optional:
         * a request may report everything with {@link #onComplete(WebResponse)} only (error statuses, cached answers, small bodies).
         * <p>
         * 받아들일 수 있는 응답의 상태 줄과 헤더를 알게 되었을 때(본문을 읽기 전) 호출된다. 선택 사항이다. 요청은 {@link #onComplete(WebResponse)}
         * 만으로 모든 것을 알릴 수도 있다(오류 상태, 캐시 응답, 작은 본문).
         *
         * @param head the response without a body / 본문 없는 응답
         */
        void onHeaders(WebResponse head);

        /**
         * Called while the body arrives.
         * <p>
         * 본문이 도착하는 동안 호출된다.
         *
         * @param loaded the body bytes received so far / 지금까지 받은 본문 바이트
         * @param total  the declared length or -1 / 선언된 길이 또는 -1
         */
        void onProgress(long loaded, long total);

        /**
         * Called exactly once when the request ended, with the response or the network error.
         * <p>
         * 요청이 끝났을 때 응답 또는 네트워크 오류와 함께 정확히 한 번 호출된다.
         *
         * @param response the final response, possibly an error response / 최종 응답. 오류 응답일 수 있다
         */
        void onComplete(WebResponse response);
    }

    /**
     * Optional extension for a receiver that can consume filtered HTTP body chunks before completion.
     * <p>
     * 완료 전에 걸러진 HTTP 본문 청크를 읽을 수 있는 수신 측의 선택 확장이다.
     */
    interface BodyCallback extends Callback {
        /**
         * Chooses a destination for the decoded body as it arrives. The destination and this callback run on the request thread.
         * <p>
         * 압축을 푼 본문이 도착할 목적지를 선택한다. 목적지와 콜백은 요청 스레드에서 실행한다.
         *
         * @param head filtered final response head / 걸러진 최종 응답의 머리
         * @return a sink or null for ordinary buffered delivery / 본문 목적지. 버퍼로 전달하려면 null
         */
        OutputStream onBody(WebResponse head);
    }

    /**
     * Starts a request on a request thread of the component and returns at once.
     * <p>
     * 컴포넌트의 요청 스레드에서 요청을 시작하고 곧바로 돌아온다.
     *
     * @param request  the request / 요청
     * @param callback the receiver / 받는 쪽
     * @return the handle for cancelling / 취소용 핸들
     */
    Call start(WebRequest request, Callback callback);
}
