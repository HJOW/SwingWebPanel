package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The document thread of one or more components: a single thread that runs their document tasks (page confirmation, script tasks,
 * layout) one after the other. A component normally has a loop of its own. A window that a page opens with <code>window.open</code>
 * shares the loop of the window that opened it (plan item P31-08-a), as browsers put windows that can reach each other on one event
 * loop: the opener can then use the document of the popup directly, because no two of their tasks ever run at the same time.
 * <p>
 * The loop counts its users. Every {@link NavigationController} that uses it calls {@link #retain()} once and {@link #release()} when
 * its component is disposed; the thread ends when the last user released the loop. A user that leaves does not stop the tasks of the
 * others: its own queued tasks are skipped by its controller.
 * <p>
 * 컴포넌트 하나 또는 여럿의 문서 스레드: 그들의 문서 작업(페이지 확정, 스크립트 작업, 배치)을 차례로 실행하는 단일 스레드. 컴포넌트는 보통 자기
 * 루프를 가진다. 페이지가 <code>window.open</code> 으로 연 창은 자신을 연 창의 루프를 함께 쓴다(계획 P31-08-a). 브라우저가 서로 닿을 수 있는 창을
 * 하나의 이벤트 루프에 두는 것과 같다: 두 창의 작업이 동시에 실행되는 일이 없으므로 여는 창이 팝업의 문서를 직접 쓸 수 있다.
 * <p>
 * 루프는 사용자 수를 센다. 루프를 쓰는 {@link NavigationController} 마다 {@link #retain()} 을 한 번 부르고 컴포넌트가 폐기될 때
 * {@link #release()} 를 부른다. 마지막 사용자가 놓으면 스레드가 끝난다. 떠나는 사용자는 다른 사용자의 작업을 멈추지 않는다: 자기가 줄 세운 작업은
 * 자기 제어기가 건너뛴다.
 */
public final class DocumentLoop {

    /** Seconds an idle document thread stays alive. / 유휴 문서 스레드가 유지되는 초. */
    private static final long IDLE_SECONDS = 5L;

    /** Numbers the document threads of the process. / 프로세스의 문서 스레드에 번호를 붙인다. */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    /** The single thread that runs the tasks. / 작업을 실행하는 단일 스레드. */
    private final ThreadPoolExecutor executor;

    /** The thread that runs the tasks now (it is created again after an idle time). / 지금 작업을 실행하는 스레드(유휴 시간 뒤에는 다시 만들어진다). */
    private volatile Thread owner;

    /** The number of controllers that use the loop. / 루프를 쓰는 제어기의 수. */
    private int users;

    /** True once the last user released the loop. / 마지막 사용자가 루프를 놓은 뒤 true. */
    private boolean ended;

    /**
     * Creates a loop without users; its thread starts with the first task.
     * <p>
     * 사용자가 없는 루프를 만든다. 스레드는 첫 작업과 함께 시작한다.
     */
    DocumentLoop() {
        ThreadFactory factory = new ThreadFactory() {
            /**
             * Creates the daemon document thread and remembers it.
             * <p>
             * 데몬 문서 스레드를 만들고 기억한다.
             *
             * @param r the work of the thread / 스레드의 일
             * @return the thread / 스레드
             */
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "swingwebpanel-document-" + THREAD_COUNTER.incrementAndGet());
                t.setDaemon(true);
                owner = t;
                return t;
            }
        };
        executor = new ThreadPoolExecutor(1, 1, IDLE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), factory);
        executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Counts one more user of the loop.
     * <p>
     * 루프의 사용자를 하나 더 센다.
     *
     * @return false if the loop has already ended and cannot be used / 루프가 이미 끝나 쓸 수 없으면 false
     */
    synchronized boolean retain() {
        if (ended) {
            return false;
        }
        users++;
        return true;
    }

    /**
     * Counts one user less. When the last user leaves, the thread is stopped: queued tasks are dropped and the running task is
     * interrupted, as it was when every component had a thread of its own.
     * <p>
     * 사용자를 하나 줄인다. 마지막 사용자가 떠나면 스레드를 멈춘다: 줄 선 작업은 버리고 실행 중인 작업은 인터럽트한다. 컴포넌트마다 스레드가 따로
     * 있던 때와 같다.
     */
    void release() {
        boolean last;
        synchronized (this) {
            if (ended || users == 0) {
                return;
            }
            users--;
            last = users == 0;
            if (last) {
                ended = true;
            }
        }
        if (last) {
            executor.shutdownNow();
        }
    }

    /**
     * Tells how many controllers use the loop.
     * <p>
     * 루프를 쓰는 제어기의 수를 알려 준다.
     *
     * @return the number of users / 사용자 수
     */
    synchronized int users() {
        return users;
    }

    /**
     * Queues a task.
     * <p>
     * 작업을 줄에 넣는다.
     *
     * @param work the task / 작업
     * @return false if the loop has ended / 루프가 끝났으면 false
     */
    boolean execute(Runnable work) {
        try {
            executor.execute(work);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    /**
     * Tells whether the calling thread is the thread of this loop.
     * <p>
     * 호출한 스레드가 이 루프의 스레드인지 알려 준다.
     *
     * @return true on the document thread / 문서 스레드이면 true
     */
    boolean isLoopThread() {
        return Thread.currentThread() == owner;
    }
}
