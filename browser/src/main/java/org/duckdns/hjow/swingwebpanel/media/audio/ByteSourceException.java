package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * An input error of a {@link ByteSource}: the temporary file of a media resource could not be read (it was closed because the page or the
 * element went away, or it was removed from outside). It is unchecked so that the container parsers, which read bytes in many small helpers,
 * need no extra declarations; {@link AudioDecoders} and the players turn it into a {@link MediaException} of kind
 * {@link MediaException.Kind#ACCESS_FAILED} (see {@link #toMediaException()}).
 * <p>
 * {@link ByteSource} 의 입력 오류: 미디어 리소스의 임시 파일을 읽을 수 없다(페이지나 요소가 사라져 닫혔거나 밖에서 지워졌다). 많은 작은 도우미에서
 * 바이트를 읽는 컨테이너 해석기에 선언을 더하지 않도록 비검사 예외이며, {@link AudioDecoders} 와 재생기는 이것을 {@link MediaException.Kind#ACCESS_FAILED}
 * 종류의 {@link MediaException} 으로 바꾼다({@link #toMediaException()} 참고).
 */
public final class ByteSourceException extends RuntimeException {

    /**
     * Serialization version.
     * <p>
     * 직렬화 버전.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message what failed / 실패한 것
     * @param cause   the input error, may be null / 입력 오류. null 가능
     */
    public ByteSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the media failure that this input error means: the resource could not be read any more.
     * <p>
     * 이 입력 오류가 뜻하는 미디어 실패를 반환한다: 리소스를 더 이상 읽을 수 없다.
     *
     * @return the failure / 실패
     */
    public MediaException toMediaException() {
        return new MediaException(MediaException.Kind.ACCESS_FAILED, getMessage(), this);
    }
}
