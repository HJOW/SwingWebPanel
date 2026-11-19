package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;

/**
 * An FTP protocol failure: the server answered with an unexpected reply code. {@link #getCode()} tells which one so that
 * callers can decide on fallbacks (for example from MLSD to LIST).
 * <p>
 * FTP 프로토콜 실패: 서버가 예상하지 못한 응답 코드를 돌려주었다. {@link #getCode()} 로 코드를 알 수 있어 호출자가 대체 동작
 * (예: MLSD 에서 LIST 로)을 정할 수 있다.
 */
public class FtpException extends IOException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The reply code, or 0 when there was no reply.
     * <p>
     * 응답 코드. 응답이 없으면 0.
     */
    private final int code;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param code    reply code or 0 / 응답 코드 또는 0
     * @param message description / 설명
     */
    public FtpException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the reply code.
     * <p>
     * 응답 코드를 돌려준다.
     *
     * @return code or 0 / 코드 또는 0
     */
    public int getCode() {
        return code;
    }
}
