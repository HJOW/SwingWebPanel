package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

/**
 * A flag that a long decoding polls between blocks to learn that its result is no longer wanted (the request was cancelled, the context was
 * closed, the page was left). The decoder checks it once per block of a few thousand frames, so a cancelled decoding ends within a
 * few milliseconds; it also stops when its own thread is interrupted. The implementation must be thread-safe: it is read by the decoding
 * thread and set by another one.
 * <p>
 * 긴 디코딩이 블록 사이에 확인하여 결과가 더는 필요하지 않음(요청이 취소되었거나 컨텍스트가 닫혔거나 페이지를 떠남)을 아는 플래그. 디코더는 수천 프레임의
 * 블록마다 한 번씩 확인하므로 취소된 디코딩은 몇 밀리초 안에 끝난다. 디코딩 스레드가 인터럽트되어도 멈춘다. 구현은 스레드 안전해야 한다. 디코딩 스레드가
 * 읽고 다른 스레드가 설정한다.
 */
public interface DecodeCancellation {

    /**
     * Tells whether the decoding should stop.
     * <p>
     * 디코딩을 멈춰야 하는지 알려 준다.
     *
     * @return true if the result is no longer wanted / 결과가 더는 필요하지 않으면 true
     */
    boolean isCancelled();
}
