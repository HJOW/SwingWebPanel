package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.ErrorEvent;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * The Java side of a <code>Worker</code> object of the page (HTML Standard "Web workers", interface <code>Worker</code>): the event target at
 * which the page receives the <code>message</code> events of the worker and its <code>error</code> events, and the handle with which the page
 * posts messages to the worker and terminates it. It lives on the page; the worker is a separate {@link WorkerScope}. The calls from the
 * worker arrive on the worker thread and are handed over to the page as page tasks, so an event of the worker is dispatched on the thread that
 * owns the page and never while a page script runs. After {@link #terminate()} nothing is dispatched any more, also not what was already
 * posted to the page.
 * <p>
 * 페이지의 <code>Worker</code> 객체(HTML 표준 "Web workers", 인터페이스 <code>Worker</code>)의 Java 쪽: 페이지가 worker 의 <code>message</code> 이벤트와
 * <code>error</code> 이벤트를 받는 이벤트 대상이며, 페이지가 worker 에 메시지를 보내고 종료하는 핸들이다. 페이지에 있으며 worker 는 별개의
 * {@link WorkerScope} 이다. worker 에서 오는 호출은 worker 스레드에 도착해 페이지 작업으로 넘겨지므로 worker 의 이벤트는 페이지를 소유한 스레드에서,
 * 페이지 스크립트가 실행되는 동안이 아닐 때 전달된다. {@link #terminate()} 뒤에는 이미 페이지에 올려 보낸 것도 포함해 아무것도 전달되지 않는다.
 */
public final class WorkerHandle extends EventTarget implements WorkerParent {

    /**
     * The list of workers that this handle belongs to.
     * <p>
     * 이 핸들이 속한 worker 목록의 소유자.
     */
    private final WorkerApi api;

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The page as a script context (to post tasks to it).
     * <p>
     * 스크립트 문맥으로서의 페이지(작업을 올려 보낸다).
     */
    private final ScriptContext pageContext;

    /**
     * The worker; set once right after creation.
     * <p>
     * worker. 만든 직후 한 번 지정한다.
     */
    private volatile WorkerScope scope;

    /**
     * True once the page terminated the worker (or the page ended): no event is dispatched any more.
     * <p>
     * 페이지가 worker 를 종료했으면(또는 페이지가 끝났으면) true. 더 이상 이벤트를 전달하지 않는다.
     */
    private volatile boolean cut;

    /**
     * The number of messages of the worker that wait for delivery to the page (a flood limit).
     * <p>
     * 페이지로의 전달을 기다리는 worker 의 메시지 수(폭주 한도).
     */
    private final AtomicInteger pendingOut = new AtomicInteger();

    /**
     * Creates the handle.
     * <p>
     * 핸들을 만든다.
     *
     * @param api         the owner list / 소유 목록
     * @param page        the page / 페이지
     * @param pageContext the page as a context / 문맥으로서의 페이지
     */
    WorkerHandle(WorkerApi api, PageScripts page, ScriptContext pageContext) {
        this.api = api;
        this.page = page;
        this.pageContext = pageContext;
    }

    /**
     * Connects the worker to the handle (once, before the worker starts).
     * <p>
     * worker 를 핸들에 연결한다(worker 가 시작하기 전에 한 번).
     *
     * @param worker the worker / worker
     */
    void attach(WorkerScope worker) {
        this.scope = worker;
    }

    /**
     * Returns the worker.
     * <p>
     * worker 를 반환한다.
     *
     * @return the worker / worker
     */
    WorkerScope getScope() {
        return scope;
    }

    /**
     * Tells whether the worker still runs (it was not terminated, did not close itself and did not fail).
     * <p>
     * worker 가 아직 실행 중인지(종료되지 않았고, 스스로 닫지 않았으며, 실패하지 않았는지) 알려 준다.
     *
     * @return true while it runs / 실행 중이면 true
     */
    boolean isAlive() {
        WorkerScope s = scope;
        return !cut && s != null && !s.isTerminated();
    }

    /**
     * Terminates the worker at once (<code>Worker.terminate()</code>): the running script is cancelled, no message and no event is delivered
     * any more, and its thread ends. Calling it again does nothing.
     * <p>
     * worker 를 즉시 종료한다(<code>Worker.terminate()</code>): 실행 중인 스크립트는 취소되고, 더 이상 메시지도 이벤트도 전달되지 않으며, 스레드가
     * 끝난다. 다시 불러도 아무 일도 없다.
     */
    public void terminate() {
        cut = true;
        WorkerScope s = scope;
        if (s != null) {
            s.terminate();
        }
        api.forget(this);
    }

    /**
     * Posts a serialized message of the page to the worker; it is dropped after the worker was terminated.
     * <p>
     * 페이지의 직렬화된 메시지를 worker 에 보낸다. worker 가 종료된 뒤에는 버린다.
     *
     * @param message the serialized message / 직렬화된 메시지
     */
    void postToWorker(ClonedValue message) {
        WorkerScope s = scope;
        if (!cut && s != null) {
            s.deliver(message);
        }
    }

    /**
     * Dispatches an event at this object as a task of the page.
     * <p>
     * 이 객체에 이벤트를 페이지의 작업으로 전달한다.
     *
     * @param event the event / 이벤트
     */
    private void fire(Event event) {
        try {
            dispatchTrustedEvent(event);
        } catch (DomException e) {
            throw page.getBridge().domError(e);
        }
    }

    /**
     * Posts a message of the worker to the page: a <code>message</code> event follows in a task of the page.
     * <p>
     * worker 의 메시지를 페이지에 올려 보낸다: 페이지의 작업으로 <code>message</code> 이벤트가 뒤따른다.
     *
     * @param data the serialized message / 직렬화된 메시지
     */
    public void messageFromWorker(final ClonedValue data) {
        if (pendingOut.incrementAndGet() > WorkerLimits.MAX_PENDING_MESSAGES) {
            pendingOut.decrementAndGet();
            return;
        }
        pageContext.post(new Runnable() {
            /**
             * Dispatches the message unless the worker was terminated.
             * <p>
             * worker 가 종료되지 않았으면 메시지를 전달한다.
             */
            public void run() {
                pendingOut.decrementAndGet();
                if (cut) {
                    return;
                }
                MessageEvent event = Messages.event(pageContext, data, "");
                fire(event);
            }
        });
    }

    /**
     * Reports an uncaught error of the worker: an <code>error</code> event (cancelable, with the message, file and position, without the
     * thrown value) at the <code>Worker</code> object; if nobody cancels it, it is written to the console of the page.
     * <p>
     * worker 의 잡히지 않은 오류를 보고한다: <code>Worker</code> 객체에 <code>error</code> 이벤트(취소 가능, 메시지, 파일, 위치를 가지며 던져진 값은
     * 없음)를 전달한다. 아무도 취소하지 않으면 페이지의 콘솔에 기록한다.
     *
     * @param message  the message / 메시지
     * @param filename the script URL / 스크립트 URL
     * @param line     the line / 줄
     * @param column   the column / 열
     */
    public void errorFromWorker(final String message, final String filename, final int line, final int column) {
        pageContext.post(new Runnable() {
            /**
             * Dispatches the error event unless the worker was terminated.
             * <p>
             * worker 가 종료되지 않았으면 오류 이벤트를 전달한다.
             */
            public void run() {
                if (cut) {
                    return;
                }
                ErrorEvent event = new ErrorEvent("error", false, true);
                event.setContent(message, filename, line, column, null);
                fire(event);
                if (!event.isDefaultPrevented()) {
                    page.getRealm().getConsole().message("error", "Uncaught " + message + " (worker " + filename + ":" + line + ":" + column + ")");
                }
            }
        });
    }

    /**
     * Reports that the worker could not start: a plain <code>error</code> event at the <code>Worker</code> object and a console message.
     * <p>
     * worker 를 시작할 수 없음을 보고한다: <code>Worker</code> 객체에 일반 <code>error</code> 이벤트를 전달하고 콘솔에 메시지를 남긴다.
     *
     * @param reason the reason / 이유
     */
    public void loadFailed(final String reason) {
        pageContext.post(new Runnable() {
            /**
             * Dispatches the error event unless the worker was terminated by the page.
             * <p>
             * 페이지가 worker 를 종료하지 않았으면 오류 이벤트를 전달한다.
             */
            public void run() {
                if (cut) {
                    return;
                }
                page.getRealm().getConsole().message("error", reason);
                fire(new Event("error", false, false));
            }
        });
    }

    /**
     * Writes the console output of the worker to the console of the page.
     * <p>
     * worker 의 콘솔 출력을 페이지의 콘솔에 기록한다.
     *
     * @param level the level / 수준
     * @param text  the text / 문자열
     */
    public void console(String level, String text) {
        if (!cut) {
            page.getRealm().getConsole().message(level, text);
        }
    }
}
