package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;

/**
 * The Java side of a <code>SharedWorker</code> object of a page (HTML Standard "Shared workers and the SharedWorker interface"): it holds the
 * outside port of the connection (<code>port</code>) and is the event target at which the page receives the <code>error</code> event when the
 * shared worker could not be loaded or when the constructor arguments do not fit the shared worker that already exists. The messages travel
 * through the port, not through this object.
 * <p>
 * 페이지의 <code>SharedWorker</code> 객체의 Java 쪽(HTML 표준 "Shared workers and the SharedWorker interface"): 연결의 바깥 포트(<code>port</code>)를 가지며,
 * 공유 worker 를 가져올 수 없거나 생성자 인수가 이미 있는 공유 worker 와 맞지 않을 때 페이지가 <code>error</code> 이벤트를 받는 이벤트 대상이다. 메시지는 이
 * 객체가 아니라 포트를 거쳐 오간다.
 */
public final class SharedHandle extends EventTarget {

    /**
     * The page that owns this object.
     * <p>
     * 이 객체를 소유한 페이지.
     */
    private final WorkerApi owner;

    /**
     * The page as a script context (to post tasks to it).
     * <p>
     * 스크립트 문맥으로서의 페이지(작업을 올려 보낸다).
     */
    private final ScriptContext context;

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The outside port of the connection.
     * <p>
     * 연결의 바깥 포트.
     */
    private final PortTarget port;

    /**
     * Creates the object of a connection.
     * <p>
     * 연결의 객체를 만든다.
     *
     * @param owner   the worker API of the page / 페이지의 worker API
     * @param page    the page / 페이지
     * @param context the page as a script context / 스크립트 문맥으로서의 페이지
     * @param port    the outside port / 바깥 포트
     */
    SharedHandle(WorkerApi owner, PageScripts page, ScriptContext context, PortTarget port) {
        this.owner = owner;
        this.page = page;
        this.context = context;
        this.port = port;
    }

    /**
     * Returns the worker API of the page that owns this object.
     * <p>
     * 이 객체를 소유한 페이지의 worker API 를 반환한다.
     *
     * @return the owner / 소유자
     */
    WorkerApi getOwner() {
        return owner;
    }

    /**
     * Returns the outside port of the connection (<code>SharedWorker.port</code>).
     * <p>
     * 연결의 바깥 포트를 반환한다(<code>SharedWorker.port</code>).
     *
     * @return the port / 포트
     */
    public PortTarget getPort() {
        return port;
    }

    /**
     * Tells the page that the shared worker could not be used: the reason goes to the console and an <code>error</code> event follows at this
     * object, as a task of the page.
     * <p>
     * 공유 worker 를 쓸 수 없음을 페이지에 알린다: 이유는 콘솔로 가고, 이 객체에 <code>error</code> 이벤트가 페이지의 작업으로 뒤따른다.
     *
     * @param reason the reason / 이유
     */
    void loadFailed(final String reason) {
        context.post(new Runnable() {
            /**
             * Dispatches the error event at the shared worker object.
             * <p>
             * 공유 worker 객체에 오류 이벤트를 전달한다.
             */
            public void run() {
                page.getRealm().getConsole().message("error", reason);
                try {
                    dispatchTrustedEvent(new Event("error", false, false));
                } catch (DomException e) {
                    throw page.getBridge().domError(e);
                }
            }
        });
    }

    /**
     * Writes console output of the shared worker to the console of this page, if the page is still there.
     * <p>
     * 페이지가 아직 있으면 공유 worker 의 콘솔 출력을 이 페이지의 콘솔에 기록한다.
     *
     * @param level the level / 수준
     * @param text  the text / 문자열
     * @return true if the console of the page took it / 페이지의 콘솔이 받았으면 true
     */
    boolean console(String level, String text) {
        if (!context.isActive()) {
            return false;
        }
        page.getRealm().getConsole().message(level, text);
        return true;
    }
}
