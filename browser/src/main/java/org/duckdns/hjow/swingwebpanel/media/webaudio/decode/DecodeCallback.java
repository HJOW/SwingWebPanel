package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;

/**
 * The receiver of the result of an asynchronous decoding (<code>DecodeSuccessCallback</code> and <code>DecodeErrorCallback</code> of the
 * specification; the script layer turns the pair into the resolution or the rejection of a promise). Exactly one of the two methods is called,
 * once, for a request that is not cancelled, on the thread of the callback executor given with the request or, without one, on the decoding thread.
 * An exception that a method throws is caught and dropped by the service: it does not affect other requests. A callback that wants to be told when a
 * buffer holds only the part of a damaged or cut-off file that could be decoded implements {@link DecodeWarningCallback}.
 * <p>
 * 비동기 디코딩의 결과를 받는 쪽(규격의 <code>DecodeSuccessCallback</code> 과 <code>DecodeErrorCallback</code>. 스크립트 계층이 이 한 쌍을 promise 의
 * 이행이나 거부로 바꾼다). 취소되지 않은 요청에 대해 두 메서드 중 정확히 하나가 한 번 불리며, 요청과 함께 준 콜백 실행기의 스레드에서, 없으면 디코딩 스레드에서
 * 불린다. 메서드가 던진 예외는 서비스가 잡아서 버리며 다른 요청에 영향을 주지 않는다. 손상되었거나 잘린 파일에서 디코딩할 수 있었던 부분만 버퍼에 담겼을 때 그것을
 * 알고 싶은 콜백은 {@link DecodeWarningCallback} 을 구현한다.
 */
public interface DecodeCallback {

    /**
     * Called with the decoded buffer.
     * <p>
     * 디코딩한 버퍼와 함께 불린다.
     *
     * @param buffer the buffer at the sample rate that was asked for / 요청한 표본율의 버퍼
     */
    void onSuccess(AudioBuffer buffer);

    /**
     * Called when the data could not be decoded.
     * <p>
     * 데이터를 디코딩할 수 없을 때 불린다.
     *
     * @param error an <code>EncodingError</code> with the cause attached (or, for a problem of the service such as a memory shortage, the
     *              error that describes it) / 원인을 붙인 <code>EncodingError</code>(메모리 부족처럼 서비스의 문제이면 그것을 설명하는 오류)
     */
    void onError(AudioException error);
}
