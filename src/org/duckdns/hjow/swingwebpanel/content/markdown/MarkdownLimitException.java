package org.duckdns.hjow.swingwebpanel.content.markdown;

/**
 * Thrown when a Markdown input exceeds a size limit of {@link MarkdownOptions} (currently only the maximum input length). Nesting limits
 * never throw: container blocks that would be nested deeper than the limit are shown as plain text instead.
 * <p>
 * Markdown 입력이 {@link MarkdownOptions} 의 크기 제한(현재는 최대 입력 길이뿐)을 넘으면 던진다. 중첩 제한은 예외를 던지지 않으며, 제한보다
 * 깊게 중첩될 컨테이너 블록은 대신 일반 텍스트로 표시한다.
 */
public class MarkdownLimitException extends RuntimeException {

    /** Serialization version id. 직렬화 버전 식별자. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message description / 설명
     */
    public MarkdownLimitException(String message) {
        super(message);
    }
}
