package org.duckdns.hjow.swingwebpanel.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * DocumentScheduler queue/lifetime tests (P15-08-a): parse-time gating, deadline order, cancellation and weak ownership.
 * <p>DocumentScheduler 큐·수명 시험: 파싱 중 보류·기한 순서·취소·약한 소유권.
 */
public class DocumentSchedulerTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public DocumentSchedulerTest() {
    }

    /** Manually advanced time makes expired-deadline ordering deterministic. / 수동 시각으로 만료 기한 순서를 결정적으로 만든다. */
    private static final class Clock implements DocumentScheduler.Clock {
        /** Current test milliseconds. / 현재 시험 밀리초. */
        long current = 1000;
        /** Creates the clock. / 시계를 만든다. */
        Clock() {
        }
        /** Returns manually controlled milliseconds. / 수동 밀리초를 반환한다.
         * @return current time / 현재 시각 */
        public long now() {
            return current;
        }
    }

    /**
     * Parsing may create timers, but callbacks run only after confirmation and always on the session's document thread.
     * <p>파싱이 타이머를 만들 수 있지만 콜백은 확정 후에만 세션 문서 스레드에서 실행된다.
     * @throws Exception on failure / 실패 시
     */
    public void testGatedUntilActivationAndDocumentThread() throws Exception {
        BrowserSession session = new BrowserSession(new DefaultPageLoader(), null);
        DocumentScheduler scheduler = new DocumentScheduler(session);
        final CountDownLatch ran = new CountDownLatch(1);
        final AtomicReference<String> thread = new AtomicReference<String>();
        try {
            scheduler.post(new Runnable() {
                /** Records the execution thread. / 실행 스레드를 기록한다. */
                public void run() {
                    thread.set(Thread.currentThread().getName());
                    ran.countDown();
                }
            }, 0);
            Check.isTrue(!ran.await(50, TimeUnit.MILLISECONDS), "no callback while parsing owns the page");
            scheduler.activate(session.getSnapshot());
            Check.isTrue(ran.await(5, TimeUnit.SECONDS), "callback after activation");
            Check.isTrue(thread.get().startsWith("swingwebpanel-document-"), "document queue owns callbacks");
            Check.equal(0, scheduler.pendingCount(), "one-shot callback released");
        } finally {
            scheduler.close();
            session.dispose();
        }
    }

    /**
     * Rejects late activation after a confirmed snapshot has been cleaned. / 확정 스냅샷을 비운 뒤의 늦은 활성화를 거부한다.
     * @throws Exception on failure / 실패 시
     */
    public void testLateActivationOfReplacedSnapshot() throws Exception {
        BrowserSession session = new BrowserSession(new DefaultPageLoader(), null);
        DocumentScheduler scheduler = new DocumentScheduler(session);
        final CountDownLatch ran = new CountDownLatch(1);
        try {
            session.setContent("<!doctype html><p>before cleanup</p>", "text/html");
            TestSupport.waitForPage(session, "about:text");
            PageSnapshot obsolete = session.getSnapshot();
            scheduler.post(record(new ArrayList<Integer>(), ran, 1), 0);
            session.clean();
            Check.isFalse(obsolete == session.getSnapshot(), "clean replaced the snapshot");
            Check.isFalse(scheduler.activate(obsolete), "a cleaned snapshot cannot activate");
            Check.isFalse(ran.await(100, TimeUnit.MILLISECONDS), "its parse-time task stays out of the document queue");
            scheduler.close();
            Check.isFalse(scheduler.activate(session.getSnapshot()), "closing still wins over activation");
        } finally {
            scheduler.close();
            session.dispose();
        }
    }

    /**
     * Overdue timers are ordered by original deadline, preserving registration order for ties.
     * <p>기한이 지난 타이머는 원래 기한순이며 같은 기한은 등록 순서를 보존한다.
     * @throws Exception on failure / 실패 시
     */
    public void testOverdueDeadlineAndStableOrder() throws Exception {
        BrowserSession session = new BrowserSession(new DefaultPageLoader(), null);
        Clock clock = new Clock();
        DocumentScheduler scheduler = new DocumentScheduler(session, clock);
        final List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
        final CountDownLatch ran = new CountDownLatch(3);
        try {
            scheduler.post(record(order, ran, 3), 100);
            scheduler.post(record(order, ran, 1), 0);
            scheduler.post(record(order, ran, 2), 0);
            clock.current += 200;
            scheduler.activate(session.getSnapshot());
            Check.isTrue(ran.await(5, TimeUnit.SECONDS), "all overdue callbacks ran");
            Check.equal(Integer.valueOf(1), order.get(0), "earliest first");
            Check.equal(Integer.valueOf(2), order.get(1), "same-deadline order stable");
            Check.equal(Integer.valueOf(3), order.get(2), "later deadline last");
        } finally {
            scheduler.close();
            session.dispose();
        }
    }

    /**
     * Creates a callback that records an ordinal and completes a latch.
     * <p>번호를 기록하고 대기를 해제하는 콜백을 만든다.
     * @param order observed order / 관측 순서
     * @param ran completion latch / 완료 대기
     * @param ordinal recorded value / 기록할 값
     * @return callback / 콜백
     */
    private static Runnable record(final List<Integer> order, final CountDownLatch ran, final int ordinal) {
        return new Runnable() {
            /** Records this callback's order. / 콜백의 순서를 기록한다. */
            public void run() {
                order.add(Integer.valueOf(ordinal));
                ran.countDown();
            }
        };
    }

    /**
     * Cancelling a timer while document work is blocked prevents delayed or already queued delivery.
     * <p>문서 작업이 막힌 동안 타이머를 취소하면 지연·이미 대기 중인 전달을 막는다.
     * @throws Exception on failure / 실패 시
     */
    public void testCancelWhileDocumentQueueBlocked() throws Exception {
        BrowserSession session = new BrowserSession(new DefaultPageLoader(), null);
        DocumentScheduler scheduler = new DocumentScheduler(session);
        BrowserSession witnessSession = new BrowserSession(new DefaultPageLoader(), null);
        DocumentScheduler witness = new DocumentScheduler(witnessSession);
        final CountDownLatch blocked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger callbacks = new AtomicInteger();
        final CountDownLatch clockPassed = new CountDownLatch(1);
        try {
            session.submitDocumentTask(new Runnable() {
                /** Holds document delivery without holding the clock. / 시계는 붙잡지 않고 문서 전달을 대기시킨다. */
                public void run() {
                    blocked.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            Check.isTrue(blocked.await(5, TimeUnit.SECONDS), "document queue blocked");
            scheduler.activate(session.getSnapshot());
            Scheduler.Handle handle = scheduler.post(new Runnable() {
                /** Counts an incorrectly delivered task. / 잘못 전달된 작업을 센다. */
                public void run() {
                    callbacks.incrementAndGet();
                }
            }, 0);
            witness.activate(witnessSession.getSnapshot());
            witness.post(new Runnable() {
                /** Proves the shared single clock passed the earlier target wakeup. / 공용 단일 시계가 앞선 대상 깨우기를 처리했음을 입증한다. */
                public void run() {
                    clockPassed.countDown();
                }
            }, 0);
            Check.isTrue(clockPassed.await(5, TimeUnit.SECONDS), "target wakeup already transferred to blocked document queue");
            handle.cancel();
            handle.cancel();
            release.countDown();
            TestSupport.settle(session);
            Check.equal(0, callbacks.get(), "cancelled task not delivered");
            Check.equal(0, scheduler.pendingCount(), "cancelled task released");
        } finally {
            release.countDown();
            scheduler.close();
            session.dispose();
            witness.close();
            witnessSession.dispose();
        }
    }

    /**
     * Replacement suppresses late tasks, and closing cancels outstanding futures plus any new registrations.
     * <p>교체가 늦은 작업을 막고 close가 미완료 future와 새 등록을 취소한다.
     * @throws Exception on failure / 실패 시
     */
    public void testReplacementAndClose() throws Exception {
        BrowserSession session = new BrowserSession(new DefaultPageLoader(), null);
        DocumentScheduler scheduler = new DocumentScheduler(session);
        final AtomicInteger callbacks = new AtomicInteger();
        Runnable callback = new Runnable() {
            /** Counts any stale delivery. / 오래된 전달을 센다. */
            public void run() {
                callbacks.incrementAndGet();
            }
        };
        try {
            scheduler.activate(session.getSnapshot());
            scheduler.post(callback, 100);
            session.setContent("new page", "text/plain");
            TestSupport.settle(session);
            long deadline = System.currentTimeMillis() + 5000;
            // 시계가 이전 작업을 취소 처리할 때까지 기다리되 특정 실행 시각을 가정하지 않는다.
            while (scheduler.pendingCount() != 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.equal(0, scheduler.pendingCount(), "stale timer discarded");
            Check.equal(0, callbacks.get(), "no callback on replaced page");
            scheduler.close();
            scheduler.close();
            scheduler.post(callback, 0).cancel();
            Check.equal(0, scheduler.pendingCount(), "closed scheduler retains no callback");
        } finally {
            scheduler.close();
            session.dispose();
        }
    }

    /**
     * The shared clock does not keep a page graph alive, even with a callback that strongly captures its session.
     * <p>콜백이 세션을 강하게 캡처해도 공용 시계는 페이지 객체 그래프를 붙잡지 않는다.
     * @throws Exception on failure / 실패 시
     */
    public void testAbandonedSessionIsCollectable() throws Exception {
        ReferenceQueue<BrowserSession> queue = new ReferenceQueue<BrowserSession>();
        WeakReference<BrowserSession> reference = abandoned(queue);
        long deadline = System.currentTimeMillis() + 30000;
        // 특정 GC 시점을 보장하지 않고 제한 시간 내 수거와 큐 통지를 관측한다.
        while (reference.get() != null && System.currentTimeMillis() < deadline) {
            System.gc();
            Thread.sleep(25);
        }
        Check.isNull(reference.get(), "clock does not root session/callback cycle");
        Check.isTrue(queue.remove(1000) == reference, "collection observed by ReferenceQueue");
    }

    /**
     * Creates a deliberately cyclic page graph in a separate frame, without disposing it.
     * <p>별도 호출에서 의도적인 페이지 순환 참조를 만들고 dispose하지 않는다.
     * @param queue collection observation queue / 수거 관측 큐
     * @return weak session reference / 세션 약한 참조
     */
    private static WeakReference<BrowserSession> abandoned(ReferenceQueue<BrowserSession> queue) {
        final BrowserSession session = new BrowserSession(new DefaultPageLoader(), null);
        DocumentScheduler scheduler = new DocumentScheduler(session);
        scheduler.activate(session.getSnapshot());
        scheduler.post(new Runnable() {
            /** Strongly captures the abandoned session. / 버린 세션을 강하게 캡처한다. */
            public void run() {
                session.getPage();
            }
        }, Long.MAX_VALUE);
        return new WeakReference<BrowserSession>(session, queue);
    }
}
