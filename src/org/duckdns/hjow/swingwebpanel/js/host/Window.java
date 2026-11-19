package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;

/**
 * The Java side of the <code>window</code> object: an event target that is the end of every document event path (the DOM calls it through
 * {@link org.duckdns.hjow.swingwebpanel.dom.Document#setEventWindow(EventTarget)}). In scripts the window is the global object of the realm; the bridge maps
 * this Java object to it, so <code>event.currentTarget === window</code> holds.
 * <p>
 * <code>window</code> 객체의 Java 쪽: 모든 문서 이벤트 경로의 끝인 이벤트 대상이다(DOM 이 {@link org.duckdns.hjow.swingwebpanel.dom.Document#setEventWindow(EventTarget)}
 * 로 이를 부른다). 스크립트에서 window 는 realm 의 전역 객체이며 브리지가 이 Java 객체를 그것에 대응시키므로
 * <code>event.currentTarget === window</code> 가 성립한다.
 */
public final class Window extends EventTarget {

    /**
     * True once a listener was registered for an event of the unloading sequence (<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>) or for
     * <code>visibilitychange</code>. It is a volatile flag so that any thread can decide whether a page that is left needs the (queued) unloading
     * sequence at all, without reading the listener map of the page.
     * <p>
     * 언로드 순서의 이벤트(<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>)나 <code>visibilitychange</code> 의 리스너가 등록된 적이 있으면 true.
     * 어느 스레드든 페이지의 리스너 맵을 읽지 않고, 떠나는 페이지에 (큐에 올리는) 언로드 순서가 필요한지 정할 수 있도록 volatile 표시로 둔다.
     */
    private volatile boolean unloadListener;

    /**
     * Creates the window target.
     * <p>
     * window 대상을 만든다.
     */
    public Window() {
        super();
    }

    /**
     * Adds a listener and remembers that the page listens to an event of the unloading sequence.
     * <p>
     * 리스너를 추가하고 페이지가 언로드 순서의 이벤트를 듣는다는 것을 기억한다.
     *
     * @param type     the event type / 이벤트 종류
     * @param listener the callback / 콜백
     * @param capture  the capture flag / 캡처 플래그
     * @param once     the once flag / once 플래그
     * @param passive  the passive flag / passive 플래그
     */
    public void addEventListener(String type, org.duckdns.hjow.swingwebpanel.dom.EventListener listener, boolean capture, boolean once, boolean passive) {
        if (listener != null && ("beforeunload".equals(type) || "pagehide".equals(type) || "unload".equals(type) || "visibilitychange".equals(type))) {
            unloadListener = true;
        }
        super.addEventListener(type, listener, capture, once, passive);
    }

    /**
     * Tells whether a listener for an event of the unloading sequence was ever registered at the window (readable from any thread).
     * <p>
     * 언로드 순서의 이벤트 리스너가 window 에 등록된 적이 있는지 알려 준다(어느 스레드에서나 읽을 수 있다).
     *
     * @return true if one was registered / 등록된 적이 있으면 true
     */
    public boolean hasUnloadListener() {
        return unloadListener;
    }
}
