package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Registry and dispatcher of the host's hyperlink listeners. Registration follows the usual Swing behavior: the same
 * listener added twice is notified twice, and a removal removes one registration. Notification always happens on the
 * event dispatch thread. The browser's own link navigation does not go through this class, so removing all listeners
 * never disables link following (API contract 5.12).
 * <p>
 * 호스트의 하이퍼링크 리스너 등록부이자 통지기. 등록은 일반적인 Swing 동작을 따른다: 같은 리스너를 두 번 등록하면 두 번 통지하고,
 * 제거는 등록 하나를 제거한다. 통지는 항상 이벤트 디스패치 스레드에서 한다. 브라우저 자체의 링크 이동은 이 클래스를 거치지 않으므로
 * 모든 리스너를 제거해도 링크 이동은 멈추지 않는다(API 계약 5.12).
 */
public final class HyperlinkDispatcher {

    /**
     * Registered listeners in registration order.
     * <p>
     * 등록 순서대로의 리스너 목록.
     */
    private final CopyOnWriteArrayList<HyperlinkListener> listeners = new CopyOnWriteArrayList<HyperlinkListener>();

    /**
     * Creates an empty dispatcher.
     * <p>
     * 비어 있는 통지기를 만든다.
     */
    public HyperlinkDispatcher() {
    }

    /**
     * Registers a listener.
     * <p>
     * 리스너를 등록한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     */
    public void add(HyperlinkListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Removes one registration of a listener; unknown listeners are ignored.
     * <p>
     * 리스너의 등록 하나를 제거한다. 등록되지 않은 리스너는 무시한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     */
    public void remove(HyperlinkListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.remove(listener);
    }

    /**
     * Removes every registered listener.
     * <p>
     * 등록된 모든 리스너를 제거한다.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Returns the number of registrations.
     * <p>
     * 등록 수를 반환한다.
     *
     * @return registration count / 등록 수
     */
    public int size() {
        return listeners.size();
    }

    /**
     * Notifies all listeners of an event on the event dispatch thread (immediately if already there).
     * <p>
     * 모든 리스너에게 이벤트를 이벤트 디스패치 스레드에서 통지한다(이미 그 스레드이면 즉시).
     *
     * @param event event to deliver / 전달할 이벤트
     */
    public void fire(final HyperlinkEvent event) {
        if (listeners.isEmpty()) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            deliver(event);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /**
                 * Delivers the event on the EDT.
                 * <p>
                 * EDT 에서 이벤트를 전달한다.
                 */
                public void run() {
                    deliver(event);
                }
            });
        }
    }

    /**
     * Calls every listener; a failing listener does not prevent the others from being notified.
     * <p>
     * 모든 리스너를 호출한다. 한 리스너가 실패해도 다른 리스너 통지는 막지 않는다.
     *
     * @param event event to deliver / 전달할 이벤트
     */
    private void deliver(HyperlinkEvent event) {
        // 복사본을 순회하므로 통지 중 등록/제거가 있어도 안전하다.
        for (HyperlinkListener l : listeners) {
            try {
                l.hyperlinkUpdate(event);
            } catch (RuntimeException e) {
                Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, e);
            }
        }
    }
}
