package org.duckdns.hjow.swingwebpanel.testing;

/**
 * Thrown by {@link Check#skip(String)} to mark a test as skipped. A skip must always carry a reason,
 * because unexplained skips are not allowed to count as completion (plan item P14-11).
 * <p>
 * {@link Check#skip(String)} 가 시험을 건너뜀으로 표시하기 위해 던지는 예외. 설명 없는 건너뜀은 완료로 인정하지 않으므로
 * (계획 P14-11) 건너뜀에는 항상 이유가 있어야 한다.
 */
public class SkipException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a skip signal.
     * <p>
     * 건너뜀 신호를 만든다.
     *
     * @param reason non-empty reason / 비어 있지 않은 이유
     */
    public SkipException(String reason) {
        super(reason);
    }
}
