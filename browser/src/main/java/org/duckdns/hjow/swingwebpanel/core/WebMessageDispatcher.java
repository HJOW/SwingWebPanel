package org.duckdns.hjow.swingwebpanel.core;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.WebMessageListener;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.HostMessageValues;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * Owns the component's bounded page-to-Java message queue and EDT listener registry.
 * <p>
 * 컴포넌트의 유한 페이지→Java 메시지 큐와 EDT 리스너 등록부를 관리한다.
 */
public final class WebMessageDispatcher {

    /** Maximum waiting messages for the component. / 컴포넌트의 최대 대기 메시지 수. */
    static final int MAX_PENDING_EVENTS = 1000;
    /** Estimated footprint limit for all waiting data. / 대기 데이터 전체의 예상 점유 한도. */
    static final long MAX_PENDING_BYTES = 16L * 1024L * 1024L;
    /** Maximum callbacks before yielding to other Swing work. / 다른 Swing 작업에 양보하기 전 최대 메시지 수. */
    private static final int BATCH_SIZE = 64;

    /** Weak owner of all events. / 모든 이벤트의 약한 소유자. */
    private final WeakReference<SwingWebPanel> panel;
    /** Console receiver for dropped message notices. / 버린 메시지 수 알림의 콘솔 수신점. */
    private final ConsoleDispatcher console;
    /** Protects the queue, registry and delivery flags. / 큐, 등록부, 전달 표시를 보호한다. */
    private final Object lock = new Object();
    /** Registrations in the order received. / 등록 순서의 리스너. */
    private final List<WebMessageListener> listeners = new ArrayList<WebMessageListener>();
    /** Captured messages in send order. / 송신 순서의 메시지. */
    private final ArrayDeque<Pending> queue = new ArrayDeque<Pending>();
    /** Estimated size of the waiting payloads. / 대기 데이터의 예상 크기. */
    private long pendingBytes;
    /** Last sequence assigned. / 마지막 할당 일련번호. */
    private long sequence;
    /** Overflowed messages since the previous notice. / 이전 알림 이후 한도로 버린 메시지 수. */
    private long dropped;
    /** Number of active component resets. / 진행 중인 컴포넌트 초기화 횟수. */
    private int resetting;
    /** Whether a delivery run is already pending. / 전달 작업이 이미 대기 중인지 여부. */
    private boolean scheduled;
    /** Whether the registry was disposed. / 등록부가 폐기되었는지 여부. */
    private boolean closed;

    /**
     * Creates an empty dispatcher.
     * <p>
     * 빈 통지기를 만든다.
     *
     * @param panel component source / 컴포넌트 원천
     * @param console console notification channel / 콘솔 알림 경로
     */
    public WebMessageDispatcher(SwingWebPanel panel, ConsoleDispatcher console) {
        this.panel = new WeakReference<SwingWebPanel>(panel);
        this.console = console;
    }

    /**
     * Adds one listener registration.
     * <p>
     * 리스너 등록 하나를 추가한다.
     *
     * @param listener non-null listener / null이 아닌 리스너
     */
    public void add(WebMessageListener listener) {
        synchronized (lock) {
            checkRegistry(listener);
            listeners.add(listener);
        }
    }

    /**
     * Removes one registration of the listener if present.
     * <p>
     * 리스너 등록이 있으면 하나를 제거한다.
     *
     * @param listener non-null listener / null이 아닌 리스너
     */
    public void remove(WebMessageListener listener) {
        synchronized (lock) {
            checkRegistry(listener);
            listeners.remove(listener);
            // 듣는 사람이 없어지면 나중에 추가할 리스너를 위해 남은 메시지를 보관하지 않는다.
            if (listeners.isEmpty()) {
                clearQueue();
            }
        }
    }

    /**
     * Validates a registration request under the registry lock.
     * <p>
     * 등록부 잠금 아래에서 등록 관련 인수를 검증한다.
     *
     * @param listener requested listener / 등록 관련 리스너
     */
    private void checkRegistry(WebMessageListener listener) {
        if (closed) {
            throw new IllegalStateException("The SwingWebPanel has been disposed");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
    }

    /** Returns the registered listeners as a new array. / 등록된 리스너를 새 배열에 담아 반환한다. */
    public WebMessageListener[] getListeners() {
        synchronized (lock) {
            return listeners.toArray(new WebMessageListener[listeners.size()]);
        }
    }

    /**
     * Captures a message before its originating script task has released it.
     * <p>
     * 발신 스크립트 작업이 끝나기 전에 메시지를 접수한다.
     *
     * @param page the exact sender / 실제 송신 문서
     * @param data copied Java data / 복사된 Java 데이터
     * @param origin sender origin / 송신 오리진
     * @param url sender URL / 송신 문서 URL
     * @param kind sender context kind / 송신 문맥 종류
     * @return true if a message was queued / 큐에 넣었으면 true
     */
    boolean enqueue(PageRuntime page, HostMessageValues.JavaMessage data, Origin origin, String url, WebMessageEvent.SourceKind kind) {
        synchronized (lock) {
            if (closed || resetting != 0 || listeners.isEmpty() || !page.acceptsHostMessages()) {
                return false;
            }
            // 스크립트 스레드는 느린 EDT를 기다리지 않고 넘친 메시지를 버린다.
            if (queue.size() >= MAX_PENDING_EVENTS || data.getSize() > MAX_PENDING_BYTES - pendingBytes) {
                dropped++;
                return false;
            }
            queue.addLast(new Pending(page, data, origin, url, kind, ++sequence, System.currentTimeMillis()));
            pendingBytes += data.getSize();
            return true;
        }
    }

    /**
     * Makes one document's messages eligible after its task and microtasks.
     * <p>
     * 문서의 작업과 마이크로태스크 뒤 그 문서의 메시지가 전달될 수 있도록 표시한다.
     *
     * @param page the completed document's runtime / 작업을 마친 문서의 실행 환경
     */
    void finishTask(PageRuntime page) {
        synchronized (lock) {
            // 문서 전체 큐의 송신 순서대로 유지하면서 완료된 문서의 메시지만 전달 가능하게 한다.
            for (Pending pending : queue) {
                if (pending.page.get() == page) {
                    pending.ready = true;
                }
            }
            schedule();
        }
    }

    /**
     * Removes messages whose sending document closed.
     * <p>
     * 닫힌 문서가 보낸 메시지를 제거한다.
     *
     * @param page closing runtime / 닫히는 실행 환경
     */
    void discard(PageRuntime page) {
        synchronized (lock) {
            Iterator<Pending> entries = queue.iterator();
            // 닫힌 문서 또는 이미 수거된 문서의 메시지를 제거해 후속 전달을 막지 않게 한다.
            while (entries.hasNext()) {
                Pending pending = entries.next();
                PageRuntime source = pending.page.get();
                if (source == null || source == page) {
                    pendingBytes -= pending.data.getSize();
                    entries.remove();
                }
            }
            schedule();
        }
    }

    /** Drops pending messages and prevents new delivery during a reset. / 초기화 중 대기 메시지를 버리고 새 전달을 막는다. */
    public void beginReset() {
        synchronized (lock) {
            resetting++;
            clearQueue();
        }
    }

    /** Permits delivery again after the corresponding reset. / 대응하는 초기화가 끝나면 전달을 다시 허용한다. */
    public void endReset() {
        synchronized (lock) {
            resetting--;
        }
    }

    /** Closes the dispatcher and removes registrations and pending messages. / 통지기를 닫고 등록과 대기 메시지를 제거한다. */
    public void close() {
        synchronized (lock) {
            closed = true;
            listeners.clear();
            clearQueue();
        }
    }

    /** Drops all queued data and accounting under the lock. / 잠금 아래에서 대기 데이터와 사용량을 버린다. */
    private void clearQueue() {
        queue.clear();
        pendingBytes = 0;
        dropped = 0;
    }

    /**
     * Requests one EDT batch when eligible work is at the head. The caller holds the lock.
     * <p>
     * 맨 앞에 전달 가능한 작업이 있으면 EDT 실행을 예약한다. 호출자가 잠금을 쥔다.
     */
    private void schedule() {
        if (closed || scheduled || resetting != 0 || (queue.isEmpty() ? dropped == 0 : !queue.peekFirst().ready)) {
            return;
        }
        scheduled = true;
        SwingUtilities.invokeLater(new Runnable() {
            /** Runs a batch on the EDT. / EDT에서 대기 메시지를 일부 처리한다. */
            public void run() {
                deliverBatch();
            }
        });
    }

    /**
     * Runs listener calls outside the registry lock and releases the EDT after one batch.
     * <p>
     * 등록부 잠금 밖에서 리스너를 부르고 한 묶음 뒤 EDT를 양보한다.
     */
    private void deliverBatch() {
        long lost = 0;
        try {
            // 준비된 메시지를 일정 수만큼 꺼낸 뒤 EDT의 다른 작업에 양보한다.
            for (int i = 0; i < BATCH_SIZE; i++) {
                Pending pending;
                WebMessageListener[] current;
                synchronized (lock) {
                    if (closed || resetting != 0 || queue.isEmpty() || !queue.peekFirst().ready) {
                        break;
                    }
                    pending = queue.removeFirst();
                    pendingBytes -= pending.data.getSize();
                    current = listeners.toArray(new WebMessageListener[listeners.size()]);
                }
                deliver(pending, current);
            }
        } finally {
            synchronized (lock) {
                scheduled = false;
                if (!closed && resetting == 0 && queue.isEmpty()) {
                    lost = dropped;
                    dropped = 0;
                }
                schedule();
            }
        }
        if (lost != 0 && console != null) {
            console.publish(ConsoleEvent.Level.WARN, ConsoleEvent.Kind.HOST_ERROR, "postMessage",
                    lost + " web messages were dropped because the host queue was full", Collections.<String>emptyList(),
                    ConsoleEvent.Origin.DOCUMENT, "", "", 0, 0);
        }
    }

    /**
     * Delivers one still-live message to each captured listener.
     * <p>
     * 여전히 유효한 메시지를 캡처한 모든 리스너에 전달한다.
     *
     * @param pending the captured message / 접수한 메시지
     * @param current the listener snapshot / 리스너 스냅샷
     */
    private void deliver(Pending pending, WebMessageListener[] current) {
        SwingWebPanel source = panel.get();
        PageRuntime page = pending.page.get();
        if (source == null || source.isDisposed() || page == null || !page.acceptsHostMessages()) {
            return;
        }
        WebMessageEvent event = new WebMessageEvent(source, pending.data.getData(), pending.origin.serialize(), pending.url, pending.kind,
                pending.sequence, pending.timestamp, pending);
        // 사용자 리스너 오류를 다음 리스너나 발신 스크립트에 전파하지 않는다.
        for (WebMessageListener listener : current) {
            if (source.isDisposed() || !page.acceptsHostMessages()) {
                return;
            }
            try {
                listener.messageReceived(event);
            } catch (RuntimeException ignored) {
                // 다른 리스너의 호출을 이어 간다.
            } catch (Error e) {
                // 실행 환경 자체의 치명적 오류는 EDT의 복구 경로에 맡긴다.
                if (e instanceof VirtualMachineError || e instanceof ThreadDeath) {
                    throw e;
                }
            }
        }
    }

    /**
     * Captured payload, weak sender identity, and later reply endpoint.
     * <p>
     * 접수한 데이터, 약한 송신 문서 정체성, 이후 응답 수신점.
     */
    private static final class Pending implements WebMessageEvent.Reply {
        /** Weak exact document. / 실제 문서의 약한 참조. */
        final WeakReference<PageRuntime> page;
        /** Copied Java payload. / 복사된 Java 데이터. */
        final HostMessageValues.JavaMessage data;
        /** Sender origin. / 송신 오리진. */
        final Origin origin;
        /** Sender document URL. / 송신 문서 URL. */
        final String url;
        /** Sender context kind. / 송신 문맥 종류. */
        final WebMessageEvent.SourceKind kind;
        /** Component's acceptance sequence. / 컴포넌트 접수 일련번호. */
        final long sequence;
        /** Acceptance timestamp. / 접수 시각. */
        final long timestamp;
        /** Whether the sending task finished. / 송신 작업이 끝났는지 여부. */
        boolean ready;

        /**
         * Captures the message for asynchronous delivery.
         * <p>
         * 비동기 전달할 메시지를 접수한다.
         *
         * @param page exact source / 실제 발신 문서
         * @param data copied data / 복사 데이터
         * @param origin source origin / 송신 오리진
         * @param url source URL / 송신 URL
         * @param kind context kind / 문맥 종류
         * @param sequence sequence number / 일련번호
         * @param timestamp time in milliseconds / 밀리초 시각
         */
        Pending(PageRuntime page, HostMessageValues.JavaMessage data, Origin origin, String url, WebMessageEvent.SourceKind kind,
                long sequence, long timestamp) {
            this.page = new WeakReference<PageRuntime>(page);
            this.data = data;
            this.origin = origin;
            this.url = url;
            this.kind = kind;
            this.sequence = sequence;
            this.timestamp = timestamp;
        }

        /**
         * Posts a reply to the original document rather than its replacement.
         * <p>
         * 같은 창으로 이동한 새 문서 대신 원래 문서에 응답을 보낸다.
         *
         * @param message copied Java reply / 복사한 Java 응답
         */
        public void send(ClonedValue message) {
            PageRuntime target = page.get();
            // 불투명 오리진도 같은 실행 환경을 직접 가리키는 경우에만 답한다.
            if (target != null && target.acceptsHostMessages()) {
                target.postHostMessage(message, origin.isOpaque() ? null : origin);
            }
        }
    }
}
