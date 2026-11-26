package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * The Java side of an <code>AbortSignal</code> (DOM Standard, section 3.2): an event target with an aborted flag and a reason, a list of
 * internal "abort algorithms" that <code>fetch()</code> and <code>XMLHttpRequest</code> register (they cancel the network request), and the
 * <code>abort</code> event. A signal aborts at most once. The script object is a bridge wrapper of this class, so
 * <code>addEventListener</code> and <code>onabort</code> work like on every other event target. All methods run on the thread that owns the
 * page's JavaScript.
 * <p>
 * <code>AbortSignal</code> 의 Java 쪽(DOM 표준 3.2절): 중단 플래그와 이유를 가진 이벤트 대상. <code>fetch()</code> 와 <code>XMLHttpRequest</code> 가 등록하는
 * 내부 "중단 알고리즘" 목록(네트워크 요청을 취소한다)과 <code>abort</code> 이벤트를 가진다. 신호는 많아야 한 번 중단된다. 스크립트 객체는 이 클래스의 연결
 * 래퍼이므로 <code>addEventListener</code> 와 <code>onabort</code> 가 다른 이벤트 대상과 똑같이 동작한다. 모든 메서드는 페이지 JavaScript 를 소유한
 * 스레드에서 실행된다.
 */
public final class AbortSignal extends EventTarget implements EventTarget.ListenerSignal {

    /** The page APIs (for creating the default reason and for scheduling). / 페이지 API(기본 이유 생성과 예약용). */
    private final WebApis api;

    /** True once the signal aborted. / 신호가 중단된 뒤에는 true. */
    private boolean aborted;

    /** The abort reason (undefined until aborted). / 중단 이유(중단되기 전에는 undefined). */
    private JsValue reason = Js.UNDEFINED;

    /** The internal abort algorithms. / 내부 중단 알고리즘. */
    private final List<Runnable> algorithms = new ArrayList<Runnable>();

    /**
     * Creates a signal that is not aborted.
     * <p>
     * 중단되지 않은 신호를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    AbortSignal(WebApis api) {
        this.api = api;
    }

    /**
     * Tells whether the signal aborted.
     * <p>
     * 신호가 중단되었는지 알려 준다.
     *
     * @return true after abort / 중단된 뒤에는 true
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * Returns the abort reason.
     * <p>
     * 중단 이유를 반환한다.
     *
     * @return the reason, undefined while not aborted / 이유. 중단되지 않았으면 undefined
     */
    public JsValue getReason() {
        return reason;
    }

    /**
     * Throws the abort reason if the signal aborted (<code>signal.throwIfAborted()</code>).
     * <p>
     * 신호가 중단되었으면 중단 이유를 던진다(<code>signal.throwIfAborted()</code>).
     */
    public void throwIfAborted() {
        if (aborted) {
            throw new JsException(reason);
        }
    }

    /**
     * Registers an internal algorithm that runs when the signal aborts. An already aborted signal ignores the registration, so the
     * caller checks {@link #isAborted()} first.
     * <p>
     * 신호가 중단될 때 실행할 내부 알고리즘을 등록한다. 이미 중단된 신호는 등록을 무시하므로 호출자가 먼저 {@link #isAborted()} 를 확인한다.
     *
     * @param algorithm the work / 작업
     */
    public void addAlgorithm(Runnable algorithm) {
        if (!aborted) {
            algorithms.add(algorithm);
        }
    }

    /**
     * Removes an internal algorithm (a request ended before the signal aborted).
     * <p>
     * 내부 알고리즘을 제거한다(신호가 중단되기 전에 요청이 끝났다).
     *
     * @param algorithm the work / 작업
     */
    public void removeAlgorithm(Runnable algorithm) {
        algorithms.remove(algorithm);
    }

    /**
     * Aborts the signal: sets the flag and the reason, runs the internal algorithms and fires <code>abort</code>. Does nothing if it
     * already aborted.
     * <p>
     * 신호를 중단한다: 플래그와 이유를 지정하고 내부 알고리즘을 실행하며 <code>abort</code> 를 발생시킨다. 이미 중단되었으면 아무것도 하지 않는다.
     *
     * @param why the reason, or undefined for the default <code>AbortError</code> / 이유. undefined 이면 기본 <code>AbortError</code>
     */
    void signalAbort(JsValue why) {
        if (aborted) {
            return;
        }
        aborted = true;
        reason = why == null || why.isUndefined() ? api.abort.defaultReason() : why;
        List<Runnable> copy = new ArrayList<Runnable>(algorithms);
        algorithms.clear();
        // 등록된 내부 알고리즘을 순서대로 실행한다. 하나의 실패가 나머지를 막지 않는다.
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).run();
            } catch (JsException e) {
                api.bridge().reportError(e);
            }
        }
        Event e = new Event("abort", false, false);
        dispatchTrustedEvent(e);
    }
}
