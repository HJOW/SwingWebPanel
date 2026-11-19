package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;

/**
 * Hands out navigation request numbers, runs page loads on background threads and cancels superseded work. Every navigation
 * gets a number that grows by one; a completion whose number is not the current one is stale and must be discarded by the
 * caller (plan item P03-03). Each load runs on its own daemon thread from a small cache, so a loader that ignores cancellation
 * (for example a blocking network read) can never delay a newer navigation. Threads end after a few idle seconds, so an idle
 * component keeps neither a thread nor itself alive. The result of a load is confirmed under the session lock, which
 * serializes all changes of the confirmed page.
 * <p>
 * 이동 요청 번호를 발급하고, 페이지 읽기를 백그라운드 스레드에서 실행하며, 밀려난 작업을 취소한다. 이동마다 1씩 증가하는 번호를 받고,
 * 번호가 현재와 다른 완료는 오래된 것이므로 호출자가 폐기해야 한다(계획 P03-03). 읽기는 작은 캐시에서 가져온 자기만의 데몬 스레드에서
 * 실행되므로 취소를 무시하는 로더(예: 블로킹 네트워크 읽기)가 더 새로운 이동을 지연시킬 수 없다. 스레드는 몇 초 놀면 끝나므로 쉬는
 * 컴포넌트는 스레드도 자기 자신도 붙잡지 않는다. 읽기 결과의 확정은 세션 잠금 아래에서 이루어져 확정된 페이지의 모든 변경이 직렬화된다.
 */
final class NavigationController {

    /**
     * Seconds an idle loader thread stays alive.
     * <p>
     * 놀고 있는 읽기 스레드가 살아 있는 시간(초).
     */
    private static final long IDLE_SECONDS = 5L;

    /**
     * Counter used to give loader threads distinct names.
     * <p>
     * 읽기 스레드에 서로 다른 이름을 주는 카운터.
     */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    /**
     * Pool that runs page loads.
     * <p>
     * 페이지 읽기를 실행하는 풀.
     */
    private final ThreadPoolExecutor loaders;

    /**
     * The component's single document thread: every change of the confirmed page and of document state is applied here, one task at a
     * time (plan item P03-04). Like the loader threads it ends after a few idle seconds.
     * <p>
     * 컴포넌트의 단일 문서 스레드: 확정된 페이지와 문서 상태의 모든 변경이 여기서 한 번에 하나씩 적용된다(계획 P03-04).
     * 읽기 스레드와 마찬가지로 몇 초 놀면 끝난다.
     */
    private final ThreadPoolExecutor documentThread;

    /**
     * The thread that the document thread pool created last (the pool has one thread at a time), so that a caller can tell whether it runs
     * on the document thread.
     * <p>
     * 문서 스레드 풀이 마지막으로 만든 스레드(풀에는 한 번에 스레드가 하나뿐). 호출자가 문서 스레드에서 실행 중인지 알 수 있게 한다.
     */
    private volatile Thread documentOwner;

    /**
     * Last number that was handed out.
     * <p>
     * 마지막으로 발급한 번호.
     */
    private final AtomicInteger counter = new AtomicInteger();

    /**
     * Number of loads that were submitted and have not finished yet.
     * <p>
     * 제출되었지만 아직 끝나지 않은 읽기의 수.
     */
    private final AtomicInteger active = new AtomicInteger();

    /**
     * Number of the current navigation (a fresh number when none is current).
     * <p>
     * 현재 이동의 번호(현재 이동이 없으면 새 번호).
     */
    private volatile int currentId;

    /**
     * Cancel token of the current navigation.
     * <p>
     * 현재 이동의 취소 토큰.
     */
    private volatile CancelToken currentToken;

    /**
     * Creates the controller and its pool.
     * <p>
     * 컨트롤러와 풀을 만든다.
     */
    NavigationController() {
        ThreadFactory factory = new ThreadFactory() {
            /**
             * Creates a named daemon thread.
             * <p>
             * 이름이 있는 데몬 스레드를 만든다.
             *
             * @param r work to run / 실행할 작업
             * @return the thread / 스레드
             */
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "swingwebpanel-loader-" + THREAD_COUNTER.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        loaders = new ThreadPoolExecutor(0, Integer.MAX_VALUE, IDLE_SECONDS, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), factory);
        ThreadFactory docFactory = new ThreadFactory() {
            /**
             * Creates the named daemon document thread.
             * <p>
             * 이름이 있는 데몬 문서 스레드를 만든다.
             *
             * @param r work to run / 실행할 작업
             * @return the thread / 스레드
             */
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "swingwebpanel-document-" + THREAD_COUNTER.incrementAndGet());
                t.setDaemon(true);
                documentOwner = t;
                return t;
            }
        };
        documentThread = new ThreadPoolExecutor(1, 1, IDLE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), docFactory);
        documentThread.allowCoreThreadTimeOut(true);
    }

    /**
     * A started navigation: its number and cancel token.
     * <p>
     * 시작된 이동: 번호와 취소 토큰.
     */
    static final class Ticket {
        /**
         * Request number.
         * <p>
         * 요청 번호.
         */
        final int id;

        /**
         * Cancel token.
         * <p>
         * 취소 토큰.
         */
        final CancelToken token;

        /**
         * Creates a ticket.
         * <p>
         * 표를 만든다.
         *
         * @param id    request number / 요청 번호
         * @param token cancel token / 취소 토큰
         */
        Ticket(int id, CancelToken token) {
            this.id = id;
            this.token = token;
        }
    }

    /**
     * Starts a new navigation: cancels the current one and returns the new number and token.
     * <p>
     * 새 이동을 시작한다: 현재 이동을 취소하고 새 번호와 토큰을 돌려준다.
     *
     * @return the new ticket / 새 표
     */
    synchronized Ticket begin() {
        if (currentToken != null) {
            currentToken.cancel();
        }
        int id = counter.incrementAndGet();
        CancelToken token = new CancelToken();
        currentId = id;
        currentToken = token;
        return new Ticket(id, token);
    }

    /**
     * Invalidates the current navigation without starting a new one (stop, clean, dispose).
     * <p>
     * 새 이동을 시작하지 않고 현재 이동을 무효화한다(stop, clean, dispose).
     */
    synchronized void cancelCurrent() {
        if (currentToken != null) {
            currentToken.cancel();
        }
        currentToken = null;
        currentId = counter.incrementAndGet();
    }

    /**
     * Returns the number of the current navigation request.
     * <p>
     * 현재 이동 요청의 번호를 반환한다.
     *
     * @return the request number / 요청 번호
     */
    int currentId() {
        return currentId;
    }

    /**
     * Tells whether a request number is the current navigation.
     * <p>
     * 요청 번호가 현재 이동인지 알려 준다.
     *
     * @param id request number / 요청 번호
     * @return true if current / 현재이면 true
     */
    boolean isCurrent(int id) {
        return id == currentId;
    }

    /**
     * Runs a page load on a background thread.
     * <p>
     * 페이지 읽기를 백그라운드 스레드에서 실행한다.
     *
     * @param work work to run / 실행할 작업
     * @return false if the pool was already shut down / 풀이 이미 종료되었으면 false
     */
    boolean submit(final Runnable work) {
        return execute(loaders, work);
    }

    /**
     * Runs a task on the component's single document thread: confirming a loaded page, and later building and changing the DOM and
     * running scripts. Tasks run one at a time in submission order.
     * <p>
     * 컴포넌트의 단일 문서 스레드에서 작업을 실행한다: 읽은 페이지의 확정, 이후에는 DOM 구성과 변경, 스크립트 실행.
     * 작업은 제출한 순서대로 한 번에 하나씩 실행된다.
     *
     * @param work work to run / 실행할 작업
     * @return false if the thread was already shut down / 문서 스레드가 이미 종료되었으면 false
     */
    boolean submitDocumentTask(final Runnable work) {
        return execute(documentThread, work);
    }

    /**
     * Tells whether the calling thread is the document thread of this component.
     * <p>
     * 호출한 스레드가 이 컴포넌트의 문서 스레드인지 알려 준다.
     *
     * @return true on the document thread / 문서 스레드이면 true
     */
    boolean isDocumentThread() {
        return Thread.currentThread() == documentOwner;
    }

    /**
     * Submits work to an executor and keeps the count of unfinished work.
     * <p>
     * 실행기에 작업을 제출하고 끝나지 않은 작업의 수를 관리한다.
     *
     * @param executor target executor / 대상 실행기
     * @param work     work to run / 실행할 작업
     * @return false if the executor rejected the work / 실행기가 거부하면 false
     */
    private boolean execute(ThreadPoolExecutor executor, final Runnable work) {
        active.incrementAndGet();
        try {
            executor.execute(new Runnable() {
                /**
                 * Runs the work and counts it as finished afterwards.
                 * <p>
                 * 작업을 실행하고 끝난 것으로 센다.
                 */
                public void run() {
                    try {
                        work.run();
                    } finally {
                        active.decrementAndGet();
                    }
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            active.decrementAndGet();
            return false;
        }
    }

    /**
     * Waits until all submitted work (loads and document tasks) has finished (used by tests and diagnostics).
     * <p>
     * 제출된 작업(읽기와 문서 작업)이 모두 끝날 때까지 기다린다(시험과 진단용).
     *
     * @param timeoutMs maximum wait in milliseconds / 최대 대기 시간(밀리초)
     * @return true if everything finished in time / 제때 모두 끝났으면 true
     */
    boolean awaitIdle(long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        // 진행 중인 읽기가 없어질 때까지 짧게 쉬며 확인한다.
        while (active.get() > 0) {
            if (System.currentTimeMillis() >= end) {
                return false;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * Shuts both executors down and interrupts running work.
     * <p>
     * 두 실행기를 종료하고 실행 중인 작업을 인터럽트한다.
     */
    void shutdown() {
        loaders.shutdownNow();
        documentThread.shutdownNow();
    }
}
