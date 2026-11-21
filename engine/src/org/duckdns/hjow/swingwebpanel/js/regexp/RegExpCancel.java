package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * Cancellation hook polled periodically by the matcher. The script engine implements it to stop a long match when
 * the page is left or the execution budget is exhausted. Implementations must be fast and thread-safe.
 * <p>
 * 일치 검색기가 주기적으로 확인하는 취소 훅. 페이지를 떠나거나 실행 예산이 바닥났을 때 스크립트 엔진이 긴 일치 검색을 멈추기 위해 구현한다.
 * 구현은 빠르고 스레드 안전해야 한다.
 */
public interface RegExpCancel {

    /**
     * Asks whether the running match should stop.
     * <p>
     * 실행 중인 일치 검색을 멈춰야 하는지 묻는다.
     *
     * @return true to abort with a {@link RegExpLimitException} / true 이면 {@link RegExpLimitException} 으로 중단한다
     */
    boolean isCancelled();
}
