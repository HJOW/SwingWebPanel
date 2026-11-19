package org.duckdns.hjow.swingwebpanel.net;

/**
 * Timeouts and size limits of the fetch layer (documented defaults of API_CONTRACT section 9). Instances are immutable;
 * use the <code>with...</code> methods to derive a changed copy (tests use small values).
 * <table border="1">
 * <tr><th>Limit / 제한</th><th>Default / 기본값</th><th>On exceed / 초과 시</th></tr>
 * <tr><td>connect timeout</td><td>15 s</td><td>{@link FailureKind#TIMEOUT}</td></tr>
 * <tr><td>read timeout</td><td>30 s</td><td>{@link FailureKind#TIMEOUT}</td></tr>
 * <tr><td>redirects</td><td>20</td><td>{@link FailureKind#TOO_MANY_REDIRECTS}</td></tr>
 * <tr><td>in-memory response body</td><td>16 MiB (after decompression)</td><td>{@link FailureKind#TOO_LARGE}</td></tr>
 * <tr><td>streamed download</td><td>unlimited</td><td>-</td></tr>
 * </table>
 * <p>
 * 가져오기 계층의 제한 시간과 크기 제한(API_CONTRACT 9절의 기본값). 인스턴스는 불변이며 <code>with...</code> 메서드로 바꾼
 * 복사본을 만든다(시험은 작은 값을 쓴다). 위 표 참조: 연결 15초, 읽기 30초, 리디렉션 20회, 메모리 응답 본문 16 MiB(압축 해제 후),
 * 스트리밍 다운로드는 제한 없음.
 */
public final class FetchLimits {

    /**
     * Default connect timeout in milliseconds.
     * <p>
     * 기본 연결 제한 시간(밀리초).
     */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 15000;

    /**
     * Default read timeout in milliseconds.
     * <p>
     * 기본 읽기 제한 시간(밀리초).
     */
    public static final int DEFAULT_READ_TIMEOUT_MS = 30000;

    /**
     * Default maximum number of redirects.
     * <p>
     * 기본 최대 리디렉션 횟수.
     */
    public static final int DEFAULT_MAX_REDIRECTS = 20;

    /**
     * Default maximum size of an in-memory response body (16 MiB).
     * <p>
     * 메모리 응답 본문의 기본 최대 크기(16 MiB).
     */
    public static final long DEFAULT_MAX_MEMORY_BYTES = 16L * 1024 * 1024;

    /**
     * Connect timeout in milliseconds.
     * <p>
     * 연결 제한 시간(밀리초).
     */
    private final int connectTimeoutMs;
    /**
     * Read timeout in milliseconds.
     * <p>
     * 읽기 제한 시간(밀리초).
     */
    private final int readTimeoutMs;
    /**
     * Maximum redirects.
     * <p>
     * 최대 리디렉션 횟수.
     */
    private final int maxRedirects;
    /**
     * Maximum in-memory body bytes.
     * <p>
     * 메모리 본문 최대 바이트.
     */
    private final long maxMemoryBytes;

    /**
     * The default limits.
     * <p>
     * 기본 제한.
     */
    public static final FetchLimits DEFAULT = new FetchLimits(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS,
            DEFAULT_MAX_REDIRECTS, DEFAULT_MAX_MEMORY_BYTES);

    /**
     * Creates limits.
     * <p>
     * 제한을 만든다.
     *
     * @param connectTimeoutMs connect timeout / 연결 제한 시간
     * @param readTimeoutMs    read timeout / 읽기 제한 시간
     * @param maxRedirects     maximum redirects / 최대 리디렉션
     * @param maxMemoryBytes   maximum in-memory body / 메모리 본문 최대
     */
    public FetchLimits(int connectTimeoutMs, int readTimeoutMs, int maxRedirects, long maxMemoryBytes) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.maxRedirects = maxRedirects;
        this.maxMemoryBytes = maxMemoryBytes;
    }

    /**
     * Returns the connect timeout.
     * <p>
     * 연결 제한 시간을 돌려준다.
     *
     * @return milliseconds / 밀리초
     */
    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * Returns the read timeout.
     * <p>
     * 읽기 제한 시간을 돌려준다.
     *
     * @return milliseconds / 밀리초
     */
    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /**
     * Returns the maximum redirects.
     * <p>
     * 최대 리디렉션 횟수를 돌려준다.
     *
     * @return count / 횟수
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * Returns the maximum in-memory body size.
     * <p>
     * 메모리 본문 최대 크기를 돌려준다.
     *
     * @return bytes / 바이트
     */
    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    /**
     * Returns a copy with other timeouts.
     * <p>
     * 제한 시간을 바꾼 복사본을 돌려준다.
     *
     * @param connectMs connect timeout / 연결 제한 시간
     * @param readMs    read timeout / 읽기 제한 시간
     * @return copy / 복사본
     */
    public FetchLimits withTimeouts(int connectMs, int readMs) {
        return new FetchLimits(connectMs, readMs, maxRedirects, maxMemoryBytes);
    }

    /**
     * Returns a copy with another redirect limit.
     * <p>
     * 리디렉션 한도를 바꾼 복사본을 돌려준다.
     *
     * @param redirects maximum redirects / 최대 리디렉션
     * @return copy / 복사본
     */
    public FetchLimits withMaxRedirects(int redirects) {
        return new FetchLimits(connectTimeoutMs, readTimeoutMs, redirects, maxMemoryBytes);
    }

    /**
     * Returns a copy with another in-memory body limit.
     * <p>
     * 메모리 본문 한도를 바꾼 복사본을 돌려준다.
     *
     * @param bytes maximum bytes / 최대 바이트
     * @return copy / 복사본
     */
    public FetchLimits withMaxMemoryBytes(long bytes) {
        return new FetchLimits(connectTimeoutMs, readTimeoutMs, maxRedirects, bytes);
    }
}
