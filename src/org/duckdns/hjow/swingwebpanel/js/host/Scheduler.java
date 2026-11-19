package org.duckdns.hjow.swingwebpanel.js.host;

/**
 * The clock and timer service of a page. The component provides one that runs the tasks on the thread that owns the page's JavaScript
 * (tests use a manual clock). Tasks of a page that is replaced or disposed are cancelled by the page itself; the scheduler only has to
 * run what it is given at the right time.
 * <p>
 * 페이지의 시계와 타이머 서비스. 컴포넌트는 페이지의 JavaScript 를 소유한 스레드에서 작업을 실행하는 것을 제공한다(시험은 수동 시계를 쓴다).
 * 교체되거나 폐기된 페이지의 작업은 페이지가 직접 취소하므로 스케줄러는 받은 것을 제때 실행하기만 하면 된다.
 */
public interface Scheduler {

    /**
     * A scheduled task that can be cancelled.
     * <p>
     * 취소할 수 있는 예약된 작업.
     */
    interface Handle {
        /**
         * Cancels the task if it has not run yet.
         * <p>
         * 아직 실행되지 않았으면 작업을 취소한다.
         */
        void cancel();
    }

    /**
     * Returns the current time in milliseconds (monotonic enough for timers).
     * <p>
     * 현재 시각(밀리초)을 반환한다(타이머에 충분히 단조로움).
     *
     * @return milliseconds / 밀리초
     */
    long now();

    /**
     * Runs a task after a delay.
     * <p>
     * 지연 뒤에 작업을 실행한다.
     *
     * @param task    the task / 작업
     * @param delayMs the delay in milliseconds, 0 or more / 지연(밀리초, 0 이상)
     * @return the handle / 핸들
     */
    Handle post(Runnable task, long delayMs);
}
