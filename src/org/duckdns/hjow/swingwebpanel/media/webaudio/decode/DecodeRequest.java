package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

/**
 * The handle of an asynchronous decoding request returned by {@link AudioDecodeService#decode}: it can cancel the request and tell its
 * state. The methods may be called from any thread.
 * <p>
 * {@link AudioDecodeService#decode} 가 반환하는 비동기 디코딩 요청의 핸들. 요청을 취소하고 상태를 알려 줄 수 있다. 메서드는 어느 스레드에서나 부를 수 있다.
 */
public interface DecodeRequest {

    /**
     * Cancels the request. A request that has not started is removed, a running decoding is interrupted and ends at its next block, and a
     * result that is waiting for its callback is dropped. After this method returned <code>true</code> no callback is started for the request
     * (a callback that is already running cannot be stopped; then the method returns <code>false</code>).
     * <p>
     * 요청을 취소한다. 시작하지 않은 요청은 제거하고, 실행 중인 디코딩은 인터럽트하여 다음 블록에서 끝나며, 콜백을 기다리는 결과는 버린다. 이 메서드가
     * <code>true</code> 를 반환한 뒤에는 그 요청의 콜백이 시작되지 않는다(이미 실행 중인 콜백은 멈출 수 없으며 그때는 <code>false</code> 를 반환한다).
     *
     * @return true if the request was cancelled before its callback started / 콜백이 시작되기 전에 요청이 취소되었으면 true
     */
    boolean cancel();

    /**
     * Tells whether the request was cancelled (by {@link #cancel()} or by the closing of the service).
     * <p>
     * 요청이 취소되었는지 알려 준다({@link #cancel()} 이나 서비스를 닫아서).
     *
     * @return true if it was cancelled / 취소되었으면 true
     */
    boolean isCancelled();

    /**
     * Tells whether the request is over: its callback has returned or it was cancelled.
     * <p>
     * 요청이 끝났는지 알려 준다: 콜백이 반환했거나 취소되었다.
     *
     * @return true if nothing more will happen for the request / 그 요청에 더는 아무 일도 일어나지 않으면 true
     */
    boolean isDone();
}
