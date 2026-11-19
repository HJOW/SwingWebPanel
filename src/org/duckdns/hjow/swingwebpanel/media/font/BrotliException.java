package org.duckdns.hjow.swingwebpanel.media.font;

import java.io.IOException;

/**
 * Thrown by {@link Brotli} when compressed data is not valid Brotli (RFC 7932: the stream is "rejected as invalid"), is cut off, or would produce more
 * output than the caller allows.
 * <p>
 * 압축 데이터가 올바른 Brotli 가 아니거나(RFC 7932: 스트림을 "잘못된 것으로 거부"), 중간에 끊겼거나, 호출자가 허용한 것보다 많은 출력을 만들 때 {@link Brotli} 가 던진다.
 */
public class BrotliException extends IOException {

    /** Serialization identifier. 직렬화 식별자. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message the reason / 이유
     */
    public BrotliException(String message) {
        super(message);
    }
}
