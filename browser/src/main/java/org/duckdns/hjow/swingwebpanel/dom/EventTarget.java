package org.duckdns.hjow.swingwebpanel.dom;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An object that can receive events and have listeners (DOM Standard section 2.7): every node, and later the window and other
 * script-visible objects. It stores the listeners and implements the dispatch algorithm: the event path is built with
 * {@link #getEventParent(Event)}, the event goes through the capture phase (root to parent), the target phase and, if it bubbles, the
 * bubble phase. A failing listener is reported to the {@link ListenerErrorHandler} and the remaining listeners still run. All methods must be
 * called on the document thread of the owning document.
 * <p>
 * 이벤트를 받고 리스너를 가질 수 있는 객체(DOM 표준 2.7절): 모든 노드이며 이후 window 와 다른 스크립트에 보이는 객체도 해당한다. 리스너를 보관하고
 * 전달 알고리즘을 구현한다: {@link #getEventParent(Event)} 로 이벤트 경로를 만들고, 이벤트는 캡처 단계(루트에서 부모까지), 대상 단계, 그리고
 * 버블하면 버블 단계를 거친다. 실패한 리스너는 {@link ListenerErrorHandler} 에 보고되고 나머지 리스너는 계속 실행된다. 모든 메서드는 소유
 * 문서의 문서 스레드에서 호출해야 한다.
 */
public abstract class EventTarget {

    /**
     * Handler used when no document handler is available: prints the stack trace.
     * <p>
     * 문서 핸들러를 쓸 수 없을 때 쓰는 핸들러: 스택 추적을 출력한다.
     */
    public static final ListenerErrorHandler PRINTING_HANDLER = new ListenerErrorHandler() {
        /**
         * Prints the failure to the standard error stream.
         * <p>
         * 실패를 표준 오류 스트림에 출력한다.
         *
         * @param target  failing target / 실패한 대상
         * @param event   event / 이벤트
         * @param failure failure / 실패
         */
        public void listenerFailed(EventTarget target, Event event, Throwable failure) {
            System.err.println("DOM listener failed for '" + (event == null ? "(mutation)" : event.getType()) + "': " + failure);
            failure.printStackTrace();
        }
    };

    /**
     * Abort signal operations used by DOM listener registration on the document thread.
     * <p>
     * 문서 스레드에서 DOM 리스너를 등록할 때 쓰는 중단 신호의 동작.
     */
    public interface ListenerSignal {
        /**
         * Tells whether the signal has already aborted.
         * <p>
         * 신호가 이미 중단되었는지 알려 준다.
         *
         * @return true after abort / 중단 후 true
         */
        boolean isAborted();

        /**
         * Registers work to run before the abort event.
         * <p>
         * abort 이벤트 전에 실행할 작업을 등록한다.
         *
         * @param algorithm abort work / 중단 작업
         */
        void addAlgorithm(Runnable algorithm);

        /**
         * Unregisters work that is no longer needed.
         * <p>
         * 더 이상 필요하지 않은 작업을 해제한다.
         *
         * @param algorithm abort work / 중단 작업
         */
        void removeAlgorithm(Runnable algorithm);
    }

    /**
     * One registered listener.
     * <p>
     * 등록된 리스너 하나.
     */
    private static final class Entry {
        /**
         * The callback.
         * <p>
         * 콜백.
         */
        final EventListener listener;

        /**
         * Capture flag.
         * <p>
         * 캡처 플래그.
         */
        final boolean capture;

        /**
         * Once flag: the listener is removed before it runs for the first time.
         * <p>
         * once 플래그: 처음 실행되기 전에 리스너가 제거된다.
         */
        final boolean once;

        /**
         * Passive flag: preventDefault is ignored.
         * <p>
         * passive 플래그: preventDefault 가 무시된다.
         */
        final boolean passive;

        /**
         * Set when the listener is removed so that a running dispatch skips it.
         * <p>
         * 리스너가 제거되면 설정되어 진행 중인 전달이 건너뛰게 한다.
         */
        boolean removed;

        /** Abort hook for this registration, if supplied. / 이 등록에 지정된 중단 작업. */
        ListenerAbort abort;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param listener callback / 콜백
         * @param capture  capture flag / 캡처 플래그
         * @param once     once flag / once 플래그
         * @param passive  passive flag / passive 플래그
         */
        Entry(EventListener listener, boolean capture, boolean once, boolean passive) {
            this.listener = listener;
            this.capture = capture;
            this.once = once;
            this.passive = passive;
        }

        /**
         * Marks this entry as removed and detaches its abort hook.
         * <p>
         * 제거 표시를 하고 중단 신호에서 작업을 해제한다.
         */
        void deactivate() {
            removed = true;
            // 수동 제거와 once 뒤에 신호가 이 등록을 추적할 필요가 없다.
            if (abort != null) {
                abort.signal.removeAlgorithm(abort);
                abort = null;
            }
        }
    }

    /**
     * Removes a specific registration without retaining the target or callback through a long-lived signal.
     * <p>
     * 오래 사는 신호가 대상이나 콜백을 보유하지 않도록 약한 참조로 특정 등록을 제거한다.
     */
    private static final class ListenerAbort implements Runnable {
        /** Signal that owns the abort hook. / 중단 작업을 보유한 신호. */
        final ListenerSignal signal;
        /** Event type used to find the registration. / 등록을 찾는 이벤트 종류. */
        private final String type;
        /** Target to remove the registration from. / 등록을 제거할 대상. */
        private final WeakReference<EventTarget> target;
        /** Registration owned by the target. / 대상이 보유한 등록. */
        private final WeakReference<Entry> entry;

        /**
         * Creates a hook for a registered entry.
         * <p>
         * 등록된 항목의 중단 작업을 만든다.
         *
         * @param signal the signal / 신호
         * @param type event type / 이벤트 종류
         * @param target registration target / 등록 대상
         * @param entry exact registration / 지정한 등록
         */
        ListenerAbort(ListenerSignal signal, String type, EventTarget target, Entry entry) {
            this.signal = signal;
            this.type = type;
            this.target = new WeakReference<EventTarget>(target);
            this.entry = new WeakReference<Entry>(entry);
        }

        /**
         * Removes the entry if it still exists.
         * <p>
         * 등록이 남아 있으면 제거한다.
         */
        public void run() {
            EventTarget owner = target.get();
            Entry registration = entry.get();
            // 제거 후 다시 등록한 동일 콜백에 이전 신호가 영향을 주지 않는다.
            if (owner != null && registration != null && !registration.removed) {
                owner.removeEntry(type, registration);
            }
        }
    }

    /**
     * Listeners by event type; created on first use.
     * <p>
     * 이벤트 종류별 리스너. 처음 쓸 때 만든다.
     */
    private Map<String, List<Entry>> listeners;

    /**
     * Adds a bubble-phase listener.
     * <p>
     * 버블 단계 리스너를 추가한다.
     *
     * @param type     event type / 이벤트 종류
     * @param listener callback, null is ignored / 콜백(null 은 무시)
     */
    public void addEventListener(String type, EventListener listener) {
        addEventListener(type, listener, false, false, false);
    }

    /**
     * Adds a listener for the capture or the bubble phase.
     * <p>
     * 캡처 또는 버블 단계의 리스너를 추가한다.
     *
     * @param type     event type / 이벤트 종류
     * @param listener callback, null is ignored / 콜백(null 은 무시)
     * @param capture  true to listen in the capture phase / 캡처 단계에서 들으려면 true
     */
    public void addEventListener(String type, EventListener listener, boolean capture) {
        addEventListener(type, listener, capture, false, false);
    }

    /**
     * Adds a listener with all options. A listener that is already registered with the same type, callback (<code>equals</code>) and
     * capture flag is not added again.
     * <p>
     * 모든 옵션과 함께 리스너를 추가한다. 종류, 콜백(<code>equals</code>), 캡처 플래그가 같은 리스너가 이미 있으면 다시 추가하지 않는다.
     *
     * @param type     event type, not null / 이벤트 종류(null 불가)
     * @param listener callback, null is ignored / 콜백(null 은 무시)
     * @param capture  capture phase flag / 캡처 단계 플래그
     * @param once     remove the listener when it first runs / 처음 실행될 때 제거
     * @param passive  ignore preventDefault in this listener / 이 리스너에서 preventDefault 무시
     */
    public void addEventListener(String type, EventListener listener, boolean capture, boolean once, boolean passive) {
        addEventListener(type, listener, capture, once, passive, null);
    }

    /**
     * Registers a listener that can be removed by an abort signal.
     * <p>
     * 중단 신호로 제거할 수 있는 리스너를 등록한다.
     *
     * @param type event type / 이벤트 종류
     * @param listener callback / 콜백
     * @param capture capture phase flag / 캡처 단계 플래그
     * @param once remove before the first callback / 첫 콜백 전에 제거
     * @param passive ignore preventDefault in the listener / 리스너에서 preventDefault 무시
     * @param signal optional signal / 선택 중단 신호
     */
    public void addEventListener(String type, EventListener listener, boolean capture, boolean once, boolean passive,
            ListenerSignal signal) {
        // 이미 중단된 신호는 새 등록을 만들지 않는다.
        if (listener == null || (signal != null && signal.isAborted())) {
            return;
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (listeners == null) {
            listeners = new LinkedHashMap<String, List<Entry>>();
        }
        List<Entry> list = listeners.get(type);
        if (list == null) {
            list = new ArrayList<Entry>(2);
            listeners.put(type, list);
        }
        // 같은 종류/콜백/캡처 플래그의 리스너가 이미 있으면 무시한다.
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            if (e.capture == capture && e.listener.equals(listener)) {
                return;
            }
        }
        Entry entry = new Entry(listener, capture, once, passive);
        list.add(entry);
        if (signal != null) {
            entry.abort = new ListenerAbort(signal, type, this, entry);
            signal.addAlgorithm(entry.abort);
        }
    }

    /**
     * Removes a bubble-phase listener.
     * <p>
     * 버블 단계 리스너를 제거한다.
     *
     * @param type     event type / 이벤트 종류
     * @param listener callback / 콜백
     */
    public void removeEventListener(String type, EventListener listener) {
        removeEventListener(type, listener, false);
    }

    /**
     * Removes a listener; nothing happens if it is not registered with this type and capture flag.
     * <p>
     * 리스너를 제거한다. 이 종류와 캡처 플래그로 등록되어 있지 않으면 아무 일도 없다.
     *
     * @param type     event type / 이벤트 종류
     * @param listener callback / 콜백
     * @param capture  capture phase flag / 캡처 단계 플래그
     */
    public void removeEventListener(String type, EventListener listener, boolean capture) {
        if (listeners == null || listener == null || type == null) {
            return;
        }
        List<Entry> list = listeners.get(type);
        if (list == null) {
            return;
        }
        // 일치하는 항목을 찾아 제거 표시를 하고 목록에서 뺀다.
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            if (e.capture == capture && e.listener.equals(listener)) {
                removeEntry(type, e);
                return;
            }
        }
    }

    /**
     * Removes the exact entry and drops the type when no listener remains.
     * <p>
     * 지정한 항목을 제거하고 리스너가 없는 이벤트 종류를 정리한다.
     *
     * @param type event type / 이벤트 종류
     * @param entry active registration / 활성 등록
     */
    private void removeEntry(String type, Entry entry) {
        List<Entry> list = listeners.get(type);
        entry.deactivate();
        list.remove(entry);
        if (list.isEmpty()) {
            listeners.remove(type);
        }
    }

    /**
     * Removes every listener of this target (used when a page or an element is released).
     * <p>
     * 이 대상의 모든 리스너를 제거한다(페이지나 요소를 해제할 때 쓴다).
     */
    public void removeAllEventListeners() {
        if (listeners == null) {
            return;
        }
        // 진행 중인 전달이 건너뛰도록 모든 항목에 제거 표시를 한다.
        for (List<Entry> list : listeners.values()) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).deactivate();
            }
        }
        listeners = null;
    }

    /**
     * Tells whether any listener is registered for a type.
     * <p>
     * 종류에 등록된 리스너가 있는지 알려 준다.
     *
     * @param type event type / 이벤트 종류
     * @return true if there is a listener / 리스너가 있으면 true
     */
    public boolean hasEventListeners(String type) {
        return listeners != null && listeners.containsKey(type);
    }

    /**
     * Returns the number of registered listeners (all types).
     * <p>
     * 등록된 리스너의 수(모든 종류)를 반환한다.
     *
     * @return count / 개수
     */
    public int getEventListenerCount() {
        int n = 0;
        if (listeners != null) {
            // 모든 종류의 목록 크기를 더한다.
            for (List<Entry> list : listeners.values()) {
                n += list.size();
            }
        }
        return n;
    }

    /**
     * Dispatches an event as script would (<code>dispatchEvent</code>): the event is marked untrusted.
     * <p>
     * 스크립트처럼 이벤트를 전달한다(<code>dispatchEvent</code>). 이벤트는 신뢰되지 않은 것으로 표시된다.
     *
     * @param event event to dispatch, not null / 전달할 이벤트(null 불가)
     * @return false if a listener called preventDefault on a cancelable event / 취소 가능한 이벤트를 리스너가 취소했으면 false
     * @throws DomException InvalidStateError if the event is already being dispatched / 이벤트가 이미 전달 중이면 InvalidStateError
     */
    public boolean dispatchEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        event.setTrusted(false);
        return dispatch(event);
    }

    /**
     * Dispatches an event created by the user agent itself (real input, load events): the event is marked trusted.
     * <p>
     * 사용자 에이전트가 직접 만든 이벤트(실제 입력, load 이벤트)를 전달한다. 이벤트는 신뢰된 것으로 표시된다.
     *
     * @param event event to dispatch, not null / 전달할 이벤트(null 불가)
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    public boolean dispatchTrustedEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        event.setTrusted(true);
        return dispatch(event);
    }

    /**
     * Runs the dispatch algorithm.
     * <p>
     * 전달 알고리즘을 실행한다.
     *
     * @param event event / 이벤트
     * @return true unless the event was cancelled / 취소되지 않았으면 true
     */
    private boolean dispatch(Event event) {
        if (event.dispatching) {
            throw DomException.invalidState("the event is already being dispatched");
        }
        event.dispatching = true;
        event.dispatched = true;
        event.target = this;
        List<EventTarget> path = new ArrayList<EventTarget>();
        EventTarget t = this;
        // 대상에서 시작해 바깥 조상으로 올라가며 이벤트 경로를 만든다. 순환을 막기 위해 길이를 제한한다.
        while (t != null && path.size() < 100000) {
            path.add(t);
            t = t.getEventParent(event);
        }
        event.path = path;
        try {
            // 캡처 단계: 가장 바깥에서 대상의 부모까지.
            for (int i = path.size() - 1; i >= 1; i--) {
                event.eventPhase = Event.CAPTURING_PHASE;
                path.get(i).invoke(event, true);
            }
            // 대상 단계: 먼저 캡처 리스너, 그다음 버블 리스너.
            event.eventPhase = Event.AT_TARGET;
            invoke(event, true);
            invoke(event, false);
            // 버블 단계: 이벤트가 버블하면 대상의 부모부터 바깥으로.
            if (event.isBubbles()) {
                for (int i = 1; i < path.size(); i++) {
                    event.eventPhase = Event.BUBBLING_PHASE;
                    path.get(i).invoke(event, false);
                }
            }
        } finally {
            event.eventPhase = Event.NONE;
            event.currentTarget = null;
            event.dispatching = false;
            event.stopPropagation = false;
            event.stopImmediate = false;
            event.path = Collections.emptyList();
        }
        return !event.canceled;
    }

    /**
     * Runs the listeners of this target for the current phase.
     * <p>
     * 이 대상의 리스너를 현재 단계에 맞게 실행한다.
     *
     * @param event       event / 이벤트
     * @param captureOnly true to run capture listeners, false to run the others / 캡처 리스너를 실행하려면 true, 나머지는 false
     */
    private void invoke(Event event, boolean captureOnly) {
        if (event.stopPropagation) {
            return;
        }
        event.currentTarget = this;
        if (listeners == null) {
            return;
        }
        List<Entry> list = listeners.get(event.getType());
        if (list == null) {
            return;
        }
        List<Entry> snapshot = new ArrayList<Entry>(list);
        // 실행 중 추가/제거가 있어도 안전하도록 복사본을 순회하고 제거된 항목은 건너뛴다.
        for (int i = 0; i < snapshot.size(); i++) {
            Entry e = snapshot.get(i);
            if (e.removed || e.capture != captureOnly) {
                continue;
            }
            if (e.once) {
                removeEntry(event.getType(), e);
            }
            event.inPassiveListener = e.passive;
            try {
                e.listener.handleEvent(event);
            } catch (RuntimeException failure) {
                getListenerErrorHandler().listenerFailed(this, event, failure);
            } catch (Error failure) {
                getListenerErrorHandler().listenerFailed(this, event, failure);
            } finally {
                event.inPassiveListener = false;
            }
            if (event.stopImmediate) {
                break;
            }
        }
    }

    /**
     * Returns the next target in the event path (the parent), or null at the end. Nodes return their parent; a document returns
     * its window, except for the <code>load</code> event.
     * <p>
     * 이벤트 경로의 다음 대상(부모)을 반환한다. 끝이면 null. 노드는 부모를 돌려주고, 문서는 <code>load</code> 이벤트를 제외하고 window 를 돌려준다.
     *
     * @param event the event being dispatched / 전달 중인 이벤트
     * @return parent target or null / 부모 대상 또는 null
     */
    protected EventTarget getEventParent(Event event) {
        return null;
    }

    /**
     * Returns the handler that receives listener failures.
     * <p>
     * 리스너 실패를 받는 핸들러를 반환한다.
     *
     * @return the handler / 핸들러
     */
    protected ListenerErrorHandler getListenerErrorHandler() {
        return PRINTING_HANDLER;
    }
}
