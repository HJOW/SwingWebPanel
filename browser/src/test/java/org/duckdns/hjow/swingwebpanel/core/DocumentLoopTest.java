package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The document loop that windows of one group share (plan item P31-08-a): two navigation controllers run their document tasks on
 * one thread, each counts its own pending work, closing one of them drops only its own queued tasks, and the thread ends when the
 * last one is closed. A controller without a shared loop keeps a loop of its own.
 * <p>
 * 한 그룹의 창이 함께 쓰는 문서 루프(계획 P31-08-a): 이동 제어기 둘이 문서 작업을 한 스레드에서 실행하고, 각자 자기의 대기 작업을 세며, 하나를
 * 닫으면 그것이 줄 세운 작업만 버려지고, 마지막 것을 닫으면 스레드가 끝난다. 공유 루프가 없는 제어기는 자기 루프를 가진다.
 */
public class DocumentLoopTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DocumentLoopTest() {
    }

    /**
     * Two controllers share one thread, and the one that is closed first leaves the other one working.
     * <p>
     * 제어기 둘이 스레드 하나를 함께 쓰고, 먼저 닫힌 쪽은 다른 쪽이 계속 일하게 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTwoControllersShareOneThread() throws Exception {
        final NavigationController first = new NavigationController();
        final DocumentLoop loop = first.loop();
        final NavigationController second = new NavigationController(loop);
        Check.same(loop, second.loop(), "the second controller uses the loop that it was given");
        Check.equal(2, loop.users(), "the loop counts both controllers");
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final Thread[] threads = new Thread[2];
        final CountDownLatch gate = new CountDownLatch(1);
        final CountDownLatch entered = new CountDownLatch(1);
        first.submitDocumentTask(new Runnable() {
            /** Holds the thread until the test queued the other tasks. / 시험이 다른 작업을 줄 세울 때까지 스레드를 잡아 둔다. */
            public void run() {
                threads[0] = Thread.currentThread();
                order.add("first-1");
                entered.countDown();
                try {
                    gate.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        Check.isTrue(entered.await(10, TimeUnit.SECONDS), "the first task runs");
        first.submitDocumentTask(new Runnable() {
            /** A task of the controller that is closed before it runs. / 실행 전에 닫히는 제어기의 작업. */
            public void run() {
                order.add("first-2");
            }
        });
        second.submitDocumentTask(new Runnable() {
            /** A task of the other controller. / 다른 제어기의 작업. */
            public void run() {
                threads[1] = Thread.currentThread();
                order.add("second-1");
                Check.isTrue(second.isDocumentThread() && first.isDocumentThread(), "both controllers see the thread as their document thread");
            }
        });
        Check.isFalse(first.isDocumentThread(), "the test thread is not the document thread");
        first.shutdown();
        Check.equal(1, loop.users(), "closing a controller leaves the loop to the other one");
        gate.countDown();
        Check.isTrue(second.awaitIdle(10000), "the other controller finishes its work");
        Check.equal("[first-1, second-1]", order.toString(), "the queued task of the closed controller is dropped, the other one runs");
        Check.same(threads[0], threads[1], "both controllers ran on the same thread");
        Check.isFalse(first.submitDocumentTask(new Runnable() {
            /** Never runs. / 실행되지 않는다. */
            public void run() {
                order.add("late");
            }
        }), "a closed controller takes no more tasks");
        final CountDownLatch more = new CountDownLatch(1);
        Check.isTrue(second.submitDocumentTask(new Runnable() {
            /** Shows that the thread still works. / 스레드가 아직 일함을 보인다. */
            public void run() {
                more.countDown();
            }
        }), "the other controller still takes tasks");
        Check.isTrue(more.await(10, TimeUnit.SECONDS), "and runs them");
        second.shutdown();
        Check.equal(0, loop.users(), "the last controller left");
        threads[0].join(10000);
        Check.isFalse(threads[0].isAlive(), "the thread ends with the last controller");
        Check.isFalse(loop.retain(), "an ended loop takes no new users");
        NavigationController third = new NavigationController(loop);
        try {
            Check.isTrue(third.loop() != loop, "a controller that is given an ended loop makes a loop of its own");
        } finally {
            third.shutdown();
        }
    }

    /**
     * A controller without a shared loop has a loop of its own that ends with it.
     * <p>
     * 공유 루프가 없는 제어기는 자기 루프를 가지며, 그 루프는 제어기와 함께 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOwnLoopEndsWithItsController() throws Exception {
        NavigationController a = new NavigationController();
        NavigationController b = new NavigationController();
        Check.isTrue(a.loop() != b.loop(), "each controller has its own loop");
        final Thread[] thread = new Thread[1];
        final CountDownLatch ran = new CountDownLatch(1);
        a.submitDocumentTask(new Runnable() {
            /** Notes the thread. / 스레드를 기록한다. */
            public void run() {
                thread[0] = Thread.currentThread();
                ran.countDown();
            }
        });
        Check.isTrue(ran.await(10, TimeUnit.SECONDS), "the task runs");
        a.shutdown();
        b.shutdown();
        thread[0].join(10000);
        Check.isFalse(thread[0].isAlive(), "the thread of the loop ends");
    }
}
