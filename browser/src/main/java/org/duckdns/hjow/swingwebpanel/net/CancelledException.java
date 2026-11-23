package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;

/**
 * Signals that a resource request was cancelled through its {@link CancelToken}. It is an {@link IOException} so that
 * stream code can propagate it, and the fetch layer converts it into a response with failure kind
 * {@link FailureKind#CANCELLED}.
 * <p>
 * 리소스 요청이 {@link CancelToken} 으로 취소되었음을 나타낸다. 스트림 코드가 전파할 수 있도록 {@link IOException} 이며,
 * 가져오기 계층은 이를 실패 종류 {@link FailureKind#CANCELLED} 인 응답으로 바꾼다.
 */
public class CancelledException extends IOException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     */
    public CancelledException() {
        super("request cancelled");
    }
}
