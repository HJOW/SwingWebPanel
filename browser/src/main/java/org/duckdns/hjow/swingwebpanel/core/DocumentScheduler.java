package org.duckdns.hjow.swingwebpanel.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;

/**
 * Page timer scheduler for the component's document queue (P15-08-a). Parsing may register timers, but none are delivered
 * until activate is called for the confirmed snapshot. The shared daemon clock holds only weak pending-task references;
 * it cannot keep an abandoned page, realm, session or component alive. Close cancels queued/delayed work without waiting
 * for either the document thread or the EDT. All methods are thread-safe.
 * <p>컴포넌트 문서 큐용 페이지 타이머 스케줄러. 파싱 중 등록할 수 있지만 확정된 스냅샷으로 activate하기 전에는 전달하지 않는다.
 * 공용 데몬 시계는 대기 작업을 약하게만 참조하여 버린 페이지·realm·세션·컴포넌트를 붙잡지 않는다. close는 문서 스레드나 EDT를 기다리지 않고
 * 예약·대기 작업을 취소한다. 모든 메서드는 스레드 안전하다.
 */
final class DocumentScheduler implements Scheduler {
    /** Injectable timer time source for deterministic queue-order tests. / 결정적 큐 순서 시험을 위한 타이머 시각 공급원. */
    interface Clock {
        /** Returns current milliseconds. / 현재 밀리초를 반환한다.
         * @return milliseconds / 밀리초 */
        long now();
    }
    /** Matches the page script timer limit. / 페이지 스크립트의 타이머 한도와 같은 값. */
    private static final int MAX_PENDING = 100000;
    /** Weak pending references whose owners were collected. / 소유자가 수거된 대기 작업의 약한 참조 큐. */
    private static final ReferenceQueue<Pending> COLLECTED = new ReferenceQueue<Pending>();
    /** Process clock, with a static factory that retains no component. / 컴포넌트를 붙잡지 않는 정적 팩토리를 쓰는 프로세스 시계. */
    private static final ScheduledThreadPoolExecutor CLOCK = createClock();
    /** Inert handle returned after close; it retains no callback. / close 후 반환하며 콜백을 보관하지 않는 비활성 핸들. */
    private static final Handle CANCELLED = new CancelledHandle();
    /** Session and its document queue, owned only by this page graph. / 이 페이지 객체 그래프 안에서만 소유하는 세션과 문서 큐. */
    private final BrowserSession session;
    /** Optional deterministic clock; null uses wall-clock time. / 선택적인 결정적 시계이며 null은 실제 시각을 쓴다. */
    private final Clock time;
    /** Pending tasks, guarded by this scheduler. / 스케줄러로 보호하는 대기 작업. */
    private final Set<Pending> pending = new LinkedHashSet<Pending>();
    /** Confirmed snapshot for callback identity checks. / 콜백 동일성 확인용 확정 스냅샷. */
    private PageSnapshot snapshot;
    /** Whether parsing ownership was handed to the document queue. / 파싱 소유권이 문서 큐로 전달되었는지 여부. */
    private boolean activated;
    /** Whether the page was closed. / 페이지가 닫혔는지 여부. */
    private boolean closed;

    /**
     * Creates an inactive scheduler. The owning page must activate it after confirmation and close it on replacement.
     * <p>비활성 스케줄러를 만든다. 소유 페이지가 확정 후 활성화하고 교체 시 닫아야 한다.
     * @param session owning session / 소유 세션
     */
    DocumentScheduler(BrowserSession session) {
        this(session, null);
    }

    /**
     * Creates an inactive scheduler with an optional time source for diagnostics.
     * <p>진단용 선택적 시각 공급원으로 비활성 스케줄러를 만든다.
     * @param session owning session / 소유 세션
     * @param time clock or null for system time / 시계 또는 시스템 시각이면 null
     */
    DocumentScheduler(BrowserSession session, Clock time) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        this.session = session;
        this.time = time;
    }

    /**
     * Creates one daemon clock and a weak-reference reaper. JRE 6 lacks remove-on-cancel policy, so cancelled futures are
     * explicitly removed and collected owners are pruned once per second, even for timers with very distant deadlines.
     * <p>데몬 시계 하나와 약한 참조 정리를 만든다. JRE 6에는 취소 시 자동 제거 정책이 없어 취소된 future를 명시적으로 제거하고 먼 기한의 작업도
     * 소유자가 수거되면 1초마다 정리한다.
     * @return shared executor / 공용 실행기
     */
    private static ScheduledThreadPoolExecutor createClock() {
        ScheduledThreadPoolExecutor clock = new ScheduledThreadPoolExecutor(1, new ClockThreads());
        clock.scheduleWithFixedDelay(new Reaper(), 1, 1, TimeUnit.SECONDS);
        return clock;
    }

    /** Static clock thread factory with no page reference. / 페이지 참조 없는 정적 시계 스레드 팩토리. */
    private static final class ClockThreads implements ThreadFactory {
        /** Creates the factory. / 팩토리를 만든다. */
        ClockThreads() {
        }
        /** Creates a daemon clock thread. / 데몬 시계 스레드를 만든다.
         * @param work executor work / 실행기 작업
         * @return daemon thread / 데몬 스레드 */
        public Thread newThread(Runnable work) {
            Thread thread = new Thread(work, "swingwebpanel-clock");
            thread.setDaemon(true);
            return thread;
        }
    }

    /** Inert shared handle. / 공용 비활성 핸들. */
    private static final class CancelledHandle implements Handle {
        /** Creates the inert handle. / 비활성 핸들을 만든다. */
        CancelledHandle() {
        }
        /** Has no task to cancel. / 취소할 작업이 없다. */
        public void cancel() {
        }
    }

    /** Reference carrying only the removable clock future, never a strong owner. / 강한 소유자 참조 없이 제거할 시계 future만 보관하는 참조. */
    private static final class PendingReference extends WeakReference<Pending> {
        /** Associated future; cleared when cancelled or reaped. / 연관 future이며 취소·수거 정리 시 비운다. */
        volatile ScheduledFuture<?> future;
        /** Creates the weak clock reference. / 약한 시계 참조를 만든다.
         * @param task pending task / 대기 작업 */
        PendingReference(Pending task) {
            super(task, COLLECTED);
        }
        /** Removes the clock entry without dereferencing the page. / 페이지를 참조하지 않고 시계 항목을 제거한다. */
        void remove() {
            ScheduledFuture<?> scheduled = future;
            future = null;
            if (scheduled != null) {
                scheduled.cancel(false);
                if (scheduled instanceof Runnable) {
                    CLOCK.remove((Runnable) scheduled);
                }
            }
        }
    }

    /** Periodically prunes weak clock entries whose page graph was collected. / 페이지 객체 그래프가 수거된 약한 시계 항목을 주기적으로 제거한다. */
    private static final class Reaper implements Runnable {
        /** Creates the reaper. / 참조 정리기를 만든다. */
        Reaper() {
        }
        /** Removes all currently collected tasks without holding a page reference. / 페이지를 붙잡지 않고 현재 수거된 작업을 모두 제거한다. */
        public void run() {
            PendingReference reference;
            // 수거 큐가 빌 때까지 원거리 타이머의 시계 항목도 함께 제거한다.
            while ((reference = (PendingReference) COLLECTED.poll()) != null) {
                reference.remove();
            }
        }
    }

    /** A clock wakeup that retains only a weak reference. / 약한 참조만 보관하는 시계 깨우기. */
    private static final class Tick implements Runnable {
        /** Weak pending task. / 대기 작업의 약한 참조. */
        private final PendingReference reference;
        /** Creates a clock wakeup. / 시계 깨우기를 만든다.
         * @param reference weak pending task / 대기 작업의 약한 참조 */
        Tick(PendingReference reference) {
            this.reference = reference;
        }
        /** Transfers a live task to the owning document queue. / 살아 있는 작업을 소유 문서 큐로 전달한다. */
        public void run() {
            Pending task = reference.get();
            if (task != null) {
                task.enqueue();
            }
        }
    }

    /** Weak document-queue delivery, so a blocked queue does not itself root the page. / 막힌 큐 자체가 페이지를 붙잡지 않는 약한 문서 큐 전달. */
    private static final class Delivery implements Runnable {
        /** Weak task reference. / 작업의 약한 참조. */
        private final WeakReference<Pending> reference;
        /** Creates queued delivery. / 큐 전달을 만든다.
         * @param task pending task / 대기 작업 */
        Delivery(Pending task) {
            reference = new WeakReference<Pending>(task);
        }
        /** Runs only a still-live and still-current task. / 살아 있고 여전히 현재인 작업만 실행한다. */
        public void run() {
            Pending task = reference.get();
            if (task != null) {
                task.deliver();
            }
        }
    }

    /** Returns wall-clock milliseconds used for timer deadlines. / 타이머 기한용 현재 밀리초를 반환한다.
     * @return current milliseconds / 현재 밀리초 */
    public long now() {
        return time == null ? System.currentTimeMillis() : time.now();
    }

    /**
     * Registers a one-shot task. Before activation, deadlines are remembered without clock delivery.
     * <p>일회 작업을 등록한다. 활성화 전에는 시계에서 전달하지 않고 기한만 보관한다.
     * @param task callback, not null / 콜백(null 불가)
     * @param delayMs nonnegative delay; negative values become zero / 음이 아닌 지연. 음수는 0
     * @return cancellation handle, inert after close / 취소 핸들. close 후에는 비활성
     * @throws IllegalArgumentException for null / null이면
     * @throws RejectedExecutionException if the pending-task limit is exceeded / 대기 작업 한도 초과 시
     */
    public synchronized Handle post(Runnable task, long delayMs) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        if (closed || session.isDisposed()) {
            return CANCELLED;
        }
        if (pending.size() >= MAX_PENDING) {
            throw new RejectedExecutionException("Page timer limit exceeded");
        }
        long current = now();
        long delay = Math.max(0, delayMs);
        long due = delay > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + delay;
        Pending scheduled = new Pending(task, due);
        pending.add(scheduled);
        if (activated) {
            schedule(scheduled);
        }
        return scheduled;
    }

    /**
     * Enables delivery for the confirmed snapshot. Expired parse-time deadlines become immediate document tasks.
     * <p>확정된 스냅샷의 전달을 활성화한다. 파싱 중 기한이 지난 작업은 즉시 문서 작업이 된다.
     * @param confirmed snapshot that owns this scheduler / 스케줄러를 소유한 확정 스냅샷
     * @return true if activated, including an already active current page / 이미 활성인 현재 페이지를 포함해 활성화됐으면 true
     * @throws IllegalArgumentException for a null snapshot / 스냅샷이 null이면
     */
    synchronized boolean activate(PageSnapshot confirmed) {
        if (confirmed == null) {
            throw new IllegalArgumentException("scheduler needs the confirmed page");
        }
        // 세션의 확정 잠금이 풀린 뒤 clean/dispose가 먼저 페이지를 교체할 수 있다.
        if (closed || session.getSnapshot() != confirmed) {
            return false;
        }
        if (activated) {
            return true;
        }
        snapshot = confirmed;
        activated = true;
        List<Pending> ordered = new ArrayList<Pending>(pending);
        // 만료된 작업도 원래 기한순으로, 기한이 같으면 등록순으로 시계에 넣는다.
        Collections.sort(ordered, new Comparator<Pending>() {
            /** Orders deadlines while keeping ties stable. / 같은 기한의 순서를 보존하며 기한순으로 정렬한다.
             * @param first first task / 첫 작업
             * @param second second task / 둘째 작업
             * @return deadline ordering / 기한 순서 */
            public int compare(Pending first, Pending second) {
                return first.due < second.due ? -1 : first.due == second.due ? 0 : 1;
            }
        });
        // 정렬된 모든 일회 작업을 원래 기한에 맞춰 시계에 넣는다.
        for (Pending task : ordered) {
            schedule(task);
        }
        return true;
    }

    /**
     * Enqueues a weak clock wakeup; caller holds this scheduler's lock.
     * <p>약한 시계 깨우기를 예약하며 호출자가 스케줄러 잠금을 소유한다.
     * @param task pending task / 대기 작업
     */
    private void schedule(Pending task) {
        PendingReference reference = new PendingReference(task);
        task.clockReference = reference;
        reference.future = CLOCK.schedule(new Tick(reference), Math.max(0, task.due - now()), TimeUnit.MILLISECONDS);
    }

    /** Checks page lifetime under the scheduler lock. / 스케줄러 잠금 아래 페이지 수명을 확인한다.
     * @return true while this confirmed page is current / 확정된 페이지가 현재이면 true */
    private boolean current() {
        return !closed && activated && !session.isDisposed() && session.getSnapshot() == snapshot;
    }

    /** Returns pending task count for internal diagnostics. / 내부 진단용 대기 작업 수를 반환한다.
     * @return outstanding task count / 미완료 작업 수 */
    synchronized int pendingCount() {
        return pending.size();
    }

    /** Cancels every outstanding task and releases callbacks without waiting on their execution thread. / 실행 스레드를 기다리지 않고 모든 미완료 작업을 취소하고 콜백을 놓는다. */
    synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<Pending> copy = new ArrayList<Pending>(pending);
        // 복사본을 순회하여 취소 중 집합 변경과 충돌하지 않게 한다.
        for (Pending task : copy) {
            task.cancel();
        }
        pending.clear();
        snapshot = null;
    }

    /** Page-owned pending task; the global clock never holds it strongly. / 페이지 소유 대기 작업이며 전역 시계는 강하게 참조하지 않는다. */
    private final class Pending implements Handle {
        /** Callback released after delivery or cancellation. / 전달·취소 후 놓는 콜백. */
        private Runnable task;
        /** Absolute wall-clock deadline. / 절대 현재 시각 기준 기한. */
        private final long due;
        /** Weak clock entry to cancel/remove. / 취소·제거할 약한 시계 항목. */
        private PendingReference clockReference;
        /** Whether the handle has finished. / 핸들이 끝났는지 여부. */
        private boolean done;
        /** Creates the page-owned handle. / 페이지 소유 핸들을 만든다.
         * @param task callback / 콜백
         * @param due deadline / 기한 */
        Pending(Runnable task, long due) {
            this.task = task;
            this.due = due;
        }
        /** Posts a weak delivery to the document queue. / 약한 전달을 문서 큐에 게시한다. */
        void enqueue() {
            synchronized (DocumentScheduler.this) {
                if (done || !current() || !session.submitDocumentTask(new Delivery(this))) {
                    cancel();
                }
            }
        }
        /** Takes and executes the callback only after checking page identity on the document queue. / 문서 큐에서 페이지 동일성을 확인한 후 콜백을 꺼내 실행한다. */
        void deliver() {
            Runnable callback;
            synchronized (DocumentScheduler.this) {
                if (done || !current()) {
                    cancel();
                    return;
                }
                callback = task;
                cancel();
            }
            callback.run();
        }
        /** Cancels delayed or queued delivery and drops the callback reference. / 지연·대기 전달을 취소하고 콜백 참조를 놓는다. */
        public void cancel() {
            synchronized (DocumentScheduler.this) {
                done = true;
                task = null;
                pending.remove(this);
                if (clockReference != null) {
                    clockReference.remove();
                    clockReference = null;
                }
            }
        }
    }
}
