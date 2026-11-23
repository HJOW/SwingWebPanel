package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Final page cleanup independent of a disposed document executor. Only pending tasks retain pages; the idle daemon expires.
 * <p>폐기된 문서 실행기와 독립적인 최종 페이지 해제. 대기 작업만 페이지를 보관하며 유휴 데몬은 종료된다.
 */
final class PageCleanup {
    /** Shared cleanup worker with no component-capturing factory. / 컴포넌트를 붙잡지 않는 팩토리를 쓰는 공용 해제 작업자. */
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                /** Creates a short-lived daemon. / 유휴 후 종료하는 데몬을 만든다.
                 * @param work cleanup work / 해제 작업
                 * @return daemon thread / 데몬 스레드 */
                public Thread newThread(Runnable work) {
                    Thread thread = new Thread(work, "swingwebpanel-page-cleanup");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    /** Prevents constructing a utility. / 도우미 객체의 생성을 막는다. */
    private PageCleanup() {
    }

    /** Schedules final cleanup without waiting on the caller. / 호출자를 기다리게 하지 않고 최종 해제를 예약한다.
     * @param work cleanup action / 해제 동작 */
    static void submit(Runnable work) {
        WORKER.execute(work);
    }
}
