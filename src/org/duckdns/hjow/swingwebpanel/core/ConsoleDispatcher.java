package org.duckdns.hjow.swingwebpanel.core;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;

/**
 * Registry and dispatcher of the host's console listeners (API contract 5.20). Registration follows the usual Swing behavior: the same
 * listener added twice is notified twice and a removal removes one registration. Messages come from the threads that run page scripts and
 * are handed to the listeners on the event dispatch thread, one at a time in the order they were published; the script thread never waits for
 * the event dispatch thread. At most {@link #MAX_PENDING_EVENTS} messages wait for delivery: when the listeners cannot keep up the next
 * messages are dropped and counted, and a {@link ConsoleEvent.Kind#MESSAGES_DROPPED} event tells the listeners how many were lost.
 * Delivery happens in batches of {@link #BATCH_SIZE} so that other events of the event dispatch thread are not starved.
 * <p>
 * 호스트의 콘솔 리스너 등록부이자 통지기(API 계약 5.20). 등록은 일반적인 Swing 동작을 따른다: 같은 리스너를 두 번 등록하면 두 번 통지하고
 * 제거는 등록 하나를 제거한다. 메시지는 페이지 스크립트를 실행하는 스레드에서 오며, 이벤트 디스패치 스레드에서 발행된 순서대로 하나씩 리스너에
 * 전달된다. 스크립트 스레드는 이벤트 디스패치 스레드를 기다리지 않는다. 전달을 기다리는 메시지는 최대 {@link #MAX_PENDING_EVENTS} 개이며,
 * 리스너가 따라가지 못하면 다음 메시지는 버리고 개수를 세어 {@link ConsoleEvent.Kind#MESSAGES_DROPPED} 이벤트로 몇 개를 잃었는지 알린다.
 * 이벤트 디스패치 스레드의 다른 이벤트가 굶지 않도록 전달은 {@link #BATCH_SIZE} 개씩 나눠서 한다.
 */
public final class ConsoleDispatcher {

    /**
     * The largest number of messages that may wait for delivery to the listeners; later messages are dropped until the queue has room.
     * <p>
     * 리스너에 전달되기를 기다릴 수 있는 메시지의 최대 개수. 큐에 자리가 날 때까지 그 뒤의 메시지는 버린다.
     */
    public static final int MAX_PENDING_EVENTS = 1000;

    /**
     * The number of messages delivered in one run on the event dispatch thread before the rest is rescheduled.
     * <p>
     * 이벤트 디스패치 스레드에서 한 번에 전달하는 메시지 수. 나머지는 다시 예약한다.
     */
    public static final int BATCH_SIZE = 100;

    /**
     * The object that is the source of the events (the component).
     * <p>
     * 이벤트의 source 가 되는 객체(컴포넌트).
     */
    private final Object source;

    /**
     * Registered listeners in registration order.
     * <p>
     * 등록 순서대로의 리스너 목록.
     */
    private final CopyOnWriteArrayList<ConsoleListener> listeners = new CopyOnWriteArrayList<ConsoleListener>();

    /**
     * Events waiting for delivery, in publishing order.
     * <p>
     * 전달을 기다리는 이벤트(발행 순서).
     */
    private final ConcurrentLinkedQueue<ConsoleEvent> queue = new ConcurrentLinkedQueue<ConsoleEvent>();

    /**
     * The number of events in {@link #queue}.
     * <p>
     * {@link #queue} 의 이벤트 수.
     */
    private final AtomicInteger pending = new AtomicInteger();

    /**
     * The number of messages dropped since the last notice.
     * <p>
     * 마지막 알림 이후 버려진 메시지 수.
     */
    private final AtomicInteger dropped = new AtomicInteger();

    /**
     * The document URL of the latest dropped message (for the notice).
     * <p>
     * 마지막으로 버려진 메시지의 문서 URL(알림용).
     */
    private volatile String droppedUrl = "";

    /**
     * True while a delivery run is scheduled on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에 전달 작업이 예약되어 있는 동안 true.
     */
    private final AtomicBoolean scheduled = new AtomicBoolean();

    /**
     * The serial number of the latest message.
     * <p>
     * 마지막 메시지의 일련번호.
     */
    private final AtomicLong sequence = new AtomicLong();

    /**
     * True after {@link #close()}.
     * <p>
     * {@link #close()} 뒤에 true.
     */
    private volatile boolean closed;

    /**
     * The delivery run.
     * <p>
     * 전달 작업.
     */
    private final Runnable deliveryRun = new Runnable() {
        /**
         * Delivers a batch of messages on the event dispatch thread.
         * <p>
         * 이벤트 디스패치 스레드에서 메시지를 한 묶음 전달한다.
         */
        public void run() {
            deliverBatch();
        }
    };

    /**
     * Creates an empty dispatcher.
     * <p>
     * 비어 있는 통지기를 만든다.
     *
     * @param source the object the events name as their source, not null / 이벤트의 source 로 삼을 객체(null 불가)
     * @throws IllegalArgumentException if the source is null / source 가 null 일 때
     */
    public ConsoleDispatcher(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.source = source;
    }

    /**
     * Registers a listener.
     * <p>
     * 리스너를 등록한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     */
    public void add(ConsoleListener listener) {
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
    public void remove(ConsoleListener listener) {
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
     * Returns a copy of the registrations in registration order.
     * <p>
     * 등록 순서대로의 등록 목록 복사본을 반환한다.
     *
     * @return the listeners / 리스너
     */
    public ConsoleListener[] getListeners() {
        return listeners.toArray(new ConsoleListener[0]);
    }

    /**
     * Returns whether a message would reach a listener; the script side skips the work of building the message otherwise.
     * <p>
     * 메시지가 리스너에 닿는지를 반환한다. 아니면 스크립트 쪽은 메시지를 만드는 일을 건너뛴다.
     *
     * @return true if a listener is registered and the dispatcher is open / 리스너가 있고 통지기가 열려 있으면 true
     */
    public boolean isListening() {
        return !closed && !listeners.isEmpty();
    }

    /**
     * Ends the dispatcher: drops the listeners and the waiting messages; later messages are ignored. It is the part of the component's
     * disposal that concerns the console.
     * <p>
     * 통지기를 끝낸다: 리스너와 대기 중인 메시지를 버리고, 그 뒤의 메시지는 무시한다. 컴포넌트 폐기 중 콘솔에 관한 부분이다.
     */
    public void close() {
        closed = true;
        listeners.clear();
        queue.clear();
        pending.set(0);
        dropped.set(0);
    }

    /**
     * Publishes one message. It returns at once on any thread: the message is queued and handed to the listeners on the event dispatch
     * thread. Without a listener nothing is built or queued. When {@link #MAX_PENDING_EVENTS} messages already wait, the message is dropped
     * and counted.
     * <p>
     * 메시지 하나를 발행한다. 어느 스레드에서든 곧바로 반환한다: 메시지는 큐에 넣고 이벤트 디스패치 스레드에서 리스너에 전달한다. 리스너가
     * 없으면 아무것도 만들거나 넣지 않는다. 이미 {@link #MAX_PENDING_EVENTS} 개가 기다리고 있으면 메시지를 버리고 센다.
     *
     * @param level       the severity / 심각도
     * @param kind        what produced it / 만든 것
     * @param method      the method name or "" / 메서드 이름 또는 ""
     * @param message     the message / 메시지
     * @param arguments   the argument texts / 인수 문자열
     * @param origin      who wrote it / 쓴 주체
     * @param documentUrl the URL of the document / 문서 URL
     * @param scriptUrl   the script URL or "" / 스크립트 URL 또는 ""
     * @param line        the line or 0 / 줄 또는 0
     * @param column      the column or 0 / 열 또는 0
     */
    public void publish(ConsoleEvent.Level level, ConsoleEvent.Kind kind, String method, String message, List<String> arguments,
            ConsoleEvent.Origin origin, String documentUrl, String scriptUrl, int line, int column) {
        // 리스너가 없거나 이미 끝난 통지기이면 이벤트를 만들 필요가 없다.
        if (!isListening()) {
            return;
        }
        ConsoleEvent e = new ConsoleEvent(source, sequence.incrementAndGet(), System.currentTimeMillis(), level, kind, method, message,
                arguments, origin, documentUrl, scriptUrl, line, column, 0);
        // 전달을 기다리는 메시지가 한도에 이르렀으면 이 메시지는 버리고 개수만 센다.
        if (pending.incrementAndGet() > MAX_PENDING_EVENTS) {
            pending.decrementAndGet();
            droppedUrl = documentUrl == null ? "" : documentUrl;
            dropped.incrementAndGet();
        } else {
            queue.add(e);
        }
        schedule();
    }

    /**
     * Schedules a delivery run on the event dispatch thread unless one is already scheduled.
     * <p>
     * 이미 예약된 것이 없으면 이벤트 디스패치 스레드에 전달 작업을 예약한다.
     */
    private void schedule() {
        if (scheduled.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(deliveryRun);
        }
    }

    /**
     * Delivers up to {@link #BATCH_SIZE} waiting messages, then the notice about dropped messages once the queue is empty, and
     * reschedules itself while there is more to deliver.
     * <p>
     * 기다리는 메시지를 {@link #BATCH_SIZE} 개까지 전달하고, 큐가 비면 버려진 메시지의 알림을 전달한다. 전달할 것이 남아 있으면 다시
     * 예약한다.
     */
    private void deliverBatch() {
        int n = 0;
        // 한 번에 일정 개수만 전달해 이벤트 디스패치 스레드를 오래 붙잡지 않는다.
        while (n < BATCH_SIZE) {
            ConsoleEvent e = queue.poll();
            if (e == null) {
                break;
            }
            pending.decrementAndGet();
            deliver(e);
            n++;
        }
        // 큐가 비었고 버려진 메시지가 있으면 몇 개를 잃었는지 알린다.
        if (queue.isEmpty() && !closed) {
            int lost = dropped.getAndSet(0);
            if (lost > 0) {
                deliver(new ConsoleEvent(source, sequence.incrementAndGet(), System.currentTimeMillis(), ConsoleEvent.Level.WARN,
                        ConsoleEvent.Kind.MESSAGES_DROPPED, "", lost + " console messages were dropped because the listeners could not keep up",
                        null, ConsoleEvent.Origin.DOCUMENT, droppedUrl, "", 0, 0, lost));
            }
        }
        scheduled.set(false);
        // 아직 전달할 것이 남았거나 그 사이 새로 들어왔으면 다시 예약한다.
        if (!closed && (!queue.isEmpty() || dropped.get() > 0)) {
            schedule();
        }
    }

    /**
     * Calls every listener; a failing listener does not prevent the others from being notified.
     * <p>
     * 모든 리스너를 호출한다. 한 리스너가 실패해도 다른 리스너 통지는 막지 않는다.
     *
     * @param event event to deliver / 전달할 이벤트
     */
    private void deliver(ConsoleEvent event) {
        // 복사본을 순회하므로 통지 중 등록/제거가 있어도 안전하다.
        for (ConsoleListener l : listeners) {
            try {
                l.consoleMessage(event);
            } catch (RuntimeException ex) {
                Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, ex);
            }
        }
    }
}
