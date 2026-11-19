package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link HyperlinkDispatcher}: Swing-style registration, EDT delivery and failure isolation (contract case C-LINK).
 * <p>
 * {@link HyperlinkDispatcher} 시험: Swing 방식의 등록, EDT 전달, 실패 격리(계약 사례 C-LINK).
 */
public class HyperlinkDispatcherTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HyperlinkDispatcherTest() {
    }

    /**
     * A listener that records the thread type of each delivery.
     * <p>
     * 전달마다 스레드 종류를 기록하는 리스너.
     */
    private static final class Counting implements HyperlinkListener {
        /**
         * One entry per delivery: true if it ran on the EDT.
         * <p>
         * 전달마다 하나: EDT 에서 실행되었으면 true.
         */
        final List<Boolean> calls = Collections.synchronizedList(new ArrayList<Boolean>());

        /**
         * Records the delivery.
         * <p>
         * 전달을 기록한다.
         *
         * @param e event / 이벤트
         */
        public void hyperlinkUpdate(HyperlinkEvent e) {
            calls.add(Boolean.valueOf(SwingUtilities.isEventDispatchThread()));
        }
    }

    /**
     * Creates an activated event for tests.
     * <p>
     * 시험용 ACTIVATED 이벤트를 만든다.
     *
     * @return event / 이벤트
     */
    private static HyperlinkEvent event() {
        return new HyperlinkEvent("source", HyperlinkEvent.EventType.ACTIVATED, null, "http://x.test/");
    }

    /**
     * C-LINK-1..3: null is rejected; duplicates notify twice; remove removes one registration; unknown listeners are ignored.
     * <p>
     * C-LINK-1..3: null 은 거부된다. 중복 등록은 두 번 통지하고, 제거는 등록 하나를 제거하며, 모르는 리스너는 무시한다.
     */
    public void testRegistrationFollowsSwingBehavior() {
        final HyperlinkDispatcher d = new HyperlinkDispatcher();
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds null.
             * <p>
             * null 을 등록한다.
             */
            public void run() {
                d.add(null);
            }
        }, "add(null)");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Removes null.
             * <p>
             * null 을 제거한다.
             */
            public void run() {
                d.remove(null);
            }
        }, "remove(null)");
        Counting c = new Counting();
        d.add(c);
        d.add(c);
        d.fire(event());
        TestSupport.flushEdt();
        Check.equal(2, c.calls.size(), "registered twice: notified twice");
        d.remove(c);
        d.fire(event());
        TestSupport.flushEdt();
        Check.equal(3, c.calls.size(), "one registration was removed: notified once more");
        d.remove(new Counting());
        d.clear();
        d.fire(event());
        TestSupport.flushEdt();
        Check.equal(3, c.calls.size(), "after clear nothing is delivered");
        Check.equal(0, d.size(), "no registrations left");
    }

    /**
     * C-LINK-4: delivery always happens on the EDT, even when fire is called from another thread.
     * <p>
     * C-LINK-4: fire 를 다른 스레드에서 호출해도 전달은 항상 EDT 에서 이루어진다.
     */
    public void testDeliveryHappensOnTheEdt() {
        HyperlinkDispatcher d = new HyperlinkDispatcher();
        Counting c = new Counting();
        d.add(c);
        d.fire(event());
        TestSupport.flushEdt();
        Check.equal(1, c.calls.size(), "delivered");
        Check.isTrue(c.calls.get(0).booleanValue(), "delivered on the EDT");
    }

    /**
     * A listener that throws must not prevent later listeners from being notified.
     * <p>
     * 예외를 던지는 리스너가 뒤 리스너의 통지를 막으면 안 된다.
     */
    public void testFailingListenerDoesNotBlockOthers() {
        HyperlinkDispatcher d = new HyperlinkDispatcher();
        d.add(new HyperlinkListener() {
            /**
             * Always fails.
             * <p>
             * 항상 실패한다.
             *
             * @param e event / 이벤트
             */
            public void hyperlinkUpdate(HyperlinkEvent e) {
                throw new IllegalStateException("expected failure of a test listener");
            }
        });
        Counting c = new Counting();
        d.add(c);
        final Thread.UncaughtExceptionHandler old = Thread.currentThread().getUncaughtExceptionHandler();
        d.fire(event());
        TestSupport.flushEdt();
        Check.equal(1, c.calls.size(), "the second listener is still notified");
        Check.notNull(old, "handler lookup works");
    }
}
