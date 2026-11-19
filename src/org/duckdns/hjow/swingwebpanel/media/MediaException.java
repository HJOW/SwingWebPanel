package org.duckdns.hjow.swingwebpanel.media;

/**
 * Failure of an image or audio operation (plan item P18-01). The {@link Kind} separates the three failures that the page shows
 * differently: the resource could not be fetched, its format is not supported, or the data is damaged.
 * <p>
 * 이미지/오디오 작업의 실패(계획 P18-01). {@link Kind} 는 페이지가 다르게 표시해야 하는 세 가지 실패를 구분한다: 리소스를 가져올 수 없음,
 * 형식을 지원하지 않음, 데이터가 손상됨.
 */
public class MediaException extends Exception {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The kind of failure.
     * <p>
     * 실패의 종류.
     */
    public enum Kind {
        /**
         * The resource could not be fetched (network, status code, blocked address).
         * <p>
         * 리소스를 가져올 수 없다(네트워크, 상태 코드, 차단된 주소).
         */
        ACCESS_FAILED,
        /**
         * The format is not recognized or not supported by this component.
         * <p>
         * 형식을 알 수 없거나 이 컴포넌트가 지원하지 않는다.
         */
        UNSUPPORTED_FORMAT,
        /**
         * The format is recognized but the data is damaged or truncated.
         * <p>
         * 형식은 인식했지만 데이터가 손상되었거나 잘렸다.
         */
        CORRUPT,
        /**
         * The data exceeds a limit of {@link DecodeLimits} (too large, too many frames).
         * <p>
         * 데이터가 {@link DecodeLimits} 의 제한을 넘는다(너무 큼, 프레임이 너무 많음).
         */
        TOO_LARGE,
        /**
         * The decoding thread was interrupted (navigation cancelled); the interrupt flag of the thread is kept set.
         * <p>
         * 디코딩 스레드가 인터럽트되었다(이동 취소). 스레드의 인터럽트 플래그는 켜진 채로 둔다.
         */
        CANCELLED
    }

    /**
     * Kind of this failure.
     * <p>
     * 이 실패의 종류.
     */
    private final Kind kind;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param kind    kind of failure / 실패의 종류
     * @param message description (English, for logs and the error display) / 설명(영어, 로그와 오류 표시용)
     */
    public MediaException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    /**
     * Creates the exception with a cause.
     * <p>
     * 원인과 함께 예외를 만든다.
     *
     * @param kind    kind of failure / 실패의 종류
     * @param message description / 설명
     * @param cause   underlying exception / 원인 예외
     */
    public MediaException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * Returns the kind of this failure.
     * <p>
     * 이 실패의 종류를 반환한다.
     *
     * @return kind / 종류
     */
    public Kind getKind() {
        return kind;
    }
}
