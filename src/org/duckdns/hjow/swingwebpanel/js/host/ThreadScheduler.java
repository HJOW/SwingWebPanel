package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Scheduler} that runs the tasks on one daemon thread. {@link PageScripts} serializes all JavaScript with a lock, so the thread
 * that runs the tasks does not have to be the one that loaded the page. {@link #shutdown()} ends the thread (dispose of the component).
 * <p>
 * 작업을 데몬 스레드 하나에서 실행하는 {@link Scheduler}. {@link PageScripts} 가 모든 JavaScript 를 잠금으로 직렬화하므로 작업을 실행하는 스레드가
 * 페이지를 읽은 스레드일 필요는 없다. {@link #shutdown()} 이 스레드를 끝낸다(컴포넌트 폐기).
 */
public final class ThreadScheduler implements Scheduler {

    /**
     * The executor with one daemon thread.
     * <p>
     * 데몬 스레드 하나를 가진 실행기.
     */
    private final ScheduledThreadPoolExecutor executor;

    /**
     * Creates the scheduler and its thread (started lazily by the executor).
     * <p>
     * 스케줄러와 스레드를 만든다(스레드는 실행기가 필요할 때 시작한다).
     */
    public ThreadScheduler() {
        executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            /**
             * Creates the daemon thread.
             * <p>
             * 데몬 스레드를 만든다.
             *
             * @param r the runnable / 실행할 작업
             * @return the thread / 스레드
             */
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "swingwebpanel-timers");
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * Returns the system time.
     * <p>
     * 시스템 시각을 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    public long now() {
        return System.currentTimeMillis();
    }

    /**
     * Schedules a task.
     * <p>
     * 작업을 예약한다.
     *
     * @param task    the task / 작업
     * @param delayMs the delay / 지연
     * @return the handle / 핸들
     */
    public Handle post(Runnable task, long delayMs) {
        final ScheduledFuture<?> f = executor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        return new Handle() {
            /**
             * Cancels the task.
             * <p>
             * 작업을 취소한다.
             */
            public void cancel() {
                f.cancel(false);
            }
        };
    }

    /**
     * Stops the thread; pending tasks are dropped.
     * <p>
     * 스레드를 멈춘다. 대기 중인 작업은 버린다.
     */
    public void shutdown() {
        executor.shutdownNow();
    }
}
