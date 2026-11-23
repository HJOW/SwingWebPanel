package org.duckdns.hjow.swingwebpanel.media.audio.aac;

/**
 * A damaged or unsupported AAC frame; the caller replaces the frame by silence.
 * <p>
 * 손상되었거나 지원하지 않는 AAC 프레임. 호출자가 프레임을 무음으로 대체한다.
 */
final class AacException extends Exception {

    /** Serialization version id. / 직렬화 버전 식별자. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception. / 예외를 만든다.
     *
     * @param message description / 설명
     */
    AacException(String message) {
        super(message);
    }
}
