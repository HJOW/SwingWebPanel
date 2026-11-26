package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.media.audio.MediaData;

/**
 * The service through which a page asks the component to fetch the resource of an <code>audio</code> or <code>video</code> element (plan
 * items P18-05-c, P18-08). The component fetches the whole resource with the common request layer (user agent, extra headers, cookies, the SSL
 * extension point, the scheme restrictions and the HTTP cache) and hands over the encoded bytes with the MIME type that the server declared; the
 * decoding and the playback happen in the page, outside of this service, and the bytes never reach scripts. A page without such a service (a
 * test host) cannot play media: the resource fails like an unsupported one.
 * <p>
 * 페이지가 <code>audio</code> 또는 <code>video</code> 요소의 리소스를 컴포넌트에 가져오게 하는 서비스(계획 P18-05-c, P18-08). 컴포넌트가 공통 요청
 * 계층(User-Agent, 추가 헤더, 쿠키, SSL 확장 지점, 스키마 제한, HTTP 캐시)으로 리소스 전체를 가져와 인코딩된 바이트를 서버가 선언한 MIME 타입과 함께
 * 넘긴다. 디코딩과 재생은 이 서비스 밖의 페이지에서 일어나며 바이트는 스크립트에 전달되지 않는다. 이 서비스가 없는 페이지(시험 호스트)는 미디어를
 * 재생할 수 없으며 리소스는 지원하지 않는 것처럼 실패한다.
 */
public interface MediaRequestSource {

    /**
     * Receives the result of one media request.
     * <p>
     * 미디어 요청 하나의 결과를 받는다.
     */
    interface Callback {
        /**
         * Reports the result, exactly once unless the request was cancelled or the page was closed. It may be called on any thread, also
         * before {@link MediaRequestSource#request(String, Callback)} returned.
         * <p>
         * 결과를 알린다. 요청이 취소되었거나 페이지가 닫히지 않는 한 정확히 한 번 호출된다. 어느 스레드에서나, 그리고
         * {@link MediaRequestSource#request(String, Callback)} 가 돌아오기 전에도 호출될 수 있다.
         *
         * @param data     the encoded resource, null on failure / 인코딩된 리소스. 실패하면 null
         * @param mimeType the MIME type that the server declared (without parameters is fine), may be null / 서버가 선언한 MIME 타입(매개변수 없어도 됨). null 가능
         * @param error    null on success, else a short description of the failure (network error, status code, blocked address, too large) /
         *                 성공하면 null, 아니면 실패의 짧은 설명(네트워크 오류, 상태 코드, 차단된 주소, 너무 큼)
         */
        void done(byte[] data, String mimeType, String error);
    }

    /**
     * A receiver that is also told how far the transfer has come (plan item P18-08: the <code>progress</code> and <code>stalled</code>
     * events of the media element). A request whose callback is not a <code>ProgressCallback</code> simply reports the end.
     * <p>
     * 전송이 어디까지 왔는지도 알려 받는 받는 쪽(계획 P18-08: 미디어 요소의 <code>progress</code>, <code>stalled</code> 이벤트). 콜백이
     * <code>ProgressCallback</code> 이 아닌 요청은 끝만 알린다.
     */
    interface ProgressCallback extends Callback {
        /**
         * Reports the bytes that arrived so far, while the resource is on its way. It is called on the thread that fetches, possibly many
         * times, never after {@link Callback#done(byte[], String, String)} and never after the request was cancelled or the page closed.
         * <p>
         * 리소스가 오는 동안 지금까지 도착한 바이트를 알린다. 가져오는 스레드에서, 여러 번 호출될 수 있으며, {@link Callback#done(byte[], String, String)}
         * 뒤에는 호출되지 않고 요청이 취소되었거나 페이지가 닫힌 뒤에도 호출되지 않는다.
         *
         * @param received the bytes received so far / 지금까지 받은 바이트
         * @param total    the expected total, or -1 if not known / 예상 전체 크기. 모르면 -1
         */
        void progress(long received, long total);
    }

    /**
     * Accepts an audio result backed by memory or a temporary file. Older request services can still use the array callback.
     * <p>
     * 메모리나 임시 파일을 사용하는 오디오 결과를 받는다. 기존 요청 서비스는 배열 콜백도 사용할 수 있다.
     */
    interface AudioCallback extends ProgressCallback {
        /**
         * Completes an audio request and transfers ownership of the encoded resource.
         * <p>
         * 오디오 요청을 완료하고 인코딩 리소스의 소유권을 받는 쪽으로 옮긴다.
         * @param data the owned resource or null on failure / 소유하는 리소스. 실패 시 null
         * @param mimeType the response MIME type / 응답 MIME 타입
         * @param error failure text or null / 실패 설명 또는 null
         */
        void doneAudio(MediaData data, String mimeType, String error);
    }

    /**
     * A running request that can be cancelled.
     * <p>
     * 취소할 수 있는 진행 중인 요청.
     */
    interface Request {
        /**
         * Cancels the request: the network stream is closed and the callback is not called any more. Harmless after completion.
         * <p>
         * 요청을 취소한다: 네트워크 스트림을 닫고 콜백은 더 이상 호출되지 않는다. 완료된 뒤에는 무해하다.
         */
        void cancel();
    }

    /**
     * Starts fetching a media resource.
     * <p>
     * 미디어 리소스를 가져오기 시작한다.
     *
     * @param absoluteUrl the absolute URL of the resource / 리소스의 절대 URL
     * @param callback    the receiver of the result / 결과를 받는 쪽
     * @return the handle for cancelling / 취소용 핸들
     */
    Request request(String absoluteUrl, Callback callback);
}
