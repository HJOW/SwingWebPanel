package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.BeforeUnloadEvent;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.FocusEvent;
import org.duckdns.hjow.swingwebpanel.dom.PageTransitionEvent;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * The life cycle events of a page that the host reports (HTML Standard "Page lifecycle": browsing the web, unloading documents; Page Visibility Level 2;
 * the window <code>focus</code>/<code>blur</code> events and the <code>online</code>/<code>offline</code> events of the network state):
 * <ul>
 * <li><code>pageshow</code> is fired by the page itself after <code>load</code>; this class remembers that it happened ({@link PageScripts#isPageShown()}), because a page
 * that was never shown has nothing to unload;</li>
 * <li>{@link #unload()} runs the unloading sequence of a page that is left (a navigation, <code>clean()</code>, <code>dispose()</code>): <code>beforeunload</code> at the window
 * (a <code>BeforeUnloadEvent</code> that can be cancelled), <code>pagehide</code> (<code>PageTransitionEvent</code>, <code>persisted</code> false), <code>visibilitychange</code> at the document
 * (the state becomes hidden) and <code>unload</code>. The page cannot hold the navigation back: this component has no dialog, so the answer of <code>beforeunload</code> is only
 * reported back to the host, which tells it in the status text;</li>
 * <li>{@link #visibilityChanged()} fires <code>visibilitychange</code> at the document when the host says that the page became visible or hidden (the component is
 * shown or hidden on screen);</li>
 * <li>{@link #windowFocus(boolean)} fires <code>focus</code> or <code>blur</code> at the window (they do not bubble);</li>
 * <li>{@link #onlineChanged()} fires <code>online</code> or <code>offline</code> at the window when the host's network state changed.</li>
 * </ul>
 * Every method must be called on the document queue (they are page tasks); none of them throws.
 * <p>
 * 호스트가 알리는 페이지 수명 이벤트(HTML Standard "Page lifecycle": 웹 탐색, 문서 언로드, Page Visibility Level 2, window 의 <code>focus</code>/<code>blur</code> 이벤트,
 * 네트워크 상태의 <code>online</code>/<code>offline</code> 이벤트):
 * <ul>
 * <li><code>pageshow</code> 는 페이지가 <code>load</code> 뒤 스스로 발생시킨다. 이 클래스는 그 사실을 기억하며({@link PageScripts#isPageShown()}) 한 번도 보이지 않은
 * 페이지는 언로드할 것이 없다.</li>
 * <li>{@link #unload()} 는 떠나는 페이지(이동, <code>clean()</code>, <code>dispose()</code>)의 언로드 순서를 실행한다: window 의 <code>beforeunload</code>(취소할 수 있는
 * <code>BeforeUnloadEvent</code>), <code>pagehide</code>(<code>PageTransitionEvent</code>, <code>persisted</code> false), 문서의 <code>visibilitychange</code>(상태가 hidden 이 됨),
 * <code>unload</code>. 페이지는 이동을 붙잡을 수 없다. 이 컴포넌트에는 대화상자가 없으므로 <code>beforeunload</code> 의 답은 호스트에 알리기만 하며 호스트가
 * 상태 메시지로 알린다.</li>
 * <li>{@link #visibilityChanged()} 는 호스트가 페이지가 보이게/가려졌다고 알릴 때(컴포넌트가 화면에 나타나거나 사라질 때) 문서에 <code>visibilitychange</code> 를 발생시킨다.</li>
 * <li>{@link #windowFocus(boolean)} 는 window 에 <code>focus</code> 또는 <code>blur</code> 를 발생시킨다(버블링하지 않는다).</li>
 * <li>{@link #onlineChanged()} 는 호스트의 네트워크 상태가 바뀌었을 때 window 에 <code>online</code> 또는 <code>offline</code> 을 발생시킨다.</li>
 * </ul>
 * 모든 메서드는 문서 큐에서 불러야 한다(페이지 작업이다). 어느 것도 예외를 던지지 않는다.
 */
public final class PageLifecycle {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The visibility that the page has seen last (the state of <code>document.visibilityState</code> reports what the host says now; this is what the last
     * <code>visibilitychange</code> event announced).
     * <p>
     * 페이지가 마지막으로 본 가시성(<code>document.visibilityState</code> 는 호스트가 지금 말하는 것을 보고한다. 이것은 마지막 <code>visibilitychange</code> 이벤트가 알린 것).
     */
    private boolean visibleAnnounced = true;

    /**
     * The network state that the page has seen last.
     * <p>
     * 페이지가 마지막으로 본 네트워크 상태.
     */
    private boolean onlineAnnounced = true;

    /**
     * True once the unloading sequence ran (it runs at most once).
     * <p>
     * 언로드 순서가 실행된 뒤이면 true(많아야 한 번 실행된다).
     */
    private boolean unloaded;

    /**
     * True from the point of the unloading sequence where the document becomes hidden (after <code>pagehide</code>).
     * <p>
     * 언로드 순서에서 문서가 숨겨지는 시점(<code>pagehide</code> 뒤)부터 true.
     */
    private volatile boolean hiddenByUnload;

    /**
     * Tells whether the document is visible now: the host says so and the page is not being unloaded past <code>pagehide</code>.
     * <p>
     * 문서가 지금 보이는지 알려 준다: 호스트가 그렇다고 하고 페이지가 <code>pagehide</code> 이후의 언로드 중이 아니다.
     *
     * @return true while visible / 보이는 동안 true
     */
    public boolean isDocumentVisible() {
        return !hiddenByUnload && page.getEnvironment().isDocumentVisible();
    }

    /**
     * Creates the life cycle reporter of a page.
     * <p>
     * 페이지의 수명 이벤트 보고자를 만든다.
     *
     * @param page the page / 페이지
     */
    PageLifecycle(PageScripts page) {
        this.page = page;
    }

    /**
     * Installs the <code>BeforeUnloadEvent</code> interface (the other events use the interfaces that exist already).
     * <p>
     * <code>BeforeUnloadEvent</code> 인터페이스를 설치한다(다른 이벤트는 이미 있는 인터페이스를 쓴다).
     */
    void install() {
        // 페이지가 시작할 때 호스트가 말하는 상태가 페이지가 처음 본 상태이다(숨겨진 컴포넌트에서 읽은 페이지는 hidden 으로 시작하고, 나중에 보이게 되면 알려야 한다).
        visibleAnnounced = page.getEnvironment().isDocumentVisible();
        onlineAnnounced = page.getEnvironment().isOnline();
        DomBridge bridge = page.getBridge();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a before-unload event.
             * <p>
             * before-unload 이벤트의 인터페이스를 고른다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object target) {
                return target instanceof BeforeUnloadEvent ? "BeforeUnloadEvent" : null;
            }
        });
        bridge.defineInterface("BeforeUnloadEvent", "Event", BeforeUnloadEvent.class, new String[] { "a:returnValue=" });
    }

    /**
     * Tells whether the page has a listener or a handler for one of the events that the unloading sequence fires, so that a page without any is closed at
     * once, without a task of the document queue.
     * <p>
     * 페이지에 언로드 순서가 발생시키는 이벤트의 리스너나 핸들러가 있는지 알려 준다. 하나도 없는 페이지는 문서 큐의 작업 없이 곧바로 닫는다.
     *
     * @return true if some listener would hear it / 어떤 리스너가 듣는다면 true
     */
    public boolean hasUnloadListeners() {
        // window 의 리스너 외에, 문서에서 듣는 visibilitychange 도 떠나는 페이지가 듣는 이벤트이다(가장 흔한 "떠날 때 저장" 방식).
        return page.getWindow().hasUnloadListener() || page.getDocument().hasVisibilityListener();
    }

    /**
     * Runs the unloading sequence of a page that is left: <code>beforeunload</code>, <code>pagehide</code>, <code>visibilitychange</code> (hidden), <code>unload</code>. A page
     * that was never shown (no <code>pageshow</code>), or that was unloaded before, or that cannot run scripts, hears nothing.
     * <p>
     * 떠나는 페이지의 언로드 순서를 실행한다: <code>beforeunload</code>, <code>pagehide</code>, <code>visibilitychange</code>(hidden), <code>unload</code>. 한 번도 보이지 않은 페이지
     * (<code>pageshow</code> 없음), 전에 언로드한 페이지, 스크립트를 실행할 수 없는 페이지는 아무것도 듣지 않는다.
     *
     * @return the text of the confirmation that <code>beforeunload</code> asked for (the empty string if it only cancelled the event), or null if it did not ask /
     *         <code>beforeunload</code> 가 요청한 확인 문구(이벤트만 취소했으면 빈 문자열). 요청하지 않았으면 null
     */
    public String unload() {
        if (unloaded || page.isDisposed() || !page.isPageShown() || !page.getEnvironment().isScriptingEnabled()) {
            return null;
        }
        unloaded = true;
        BeforeUnloadEvent before = new BeforeUnloadEvent();
        page.dispatchTrustedEvent(page.getWindow(), before);
        page.dispatchTrustedEvent(page.getWindow(), new PageTransitionEvent("pagehide", false, false));
        // 문서가 숨겨진다: pagehide 뒤 document.visibilityState 는 hidden 이고, 알린 가시성이 보이는 상태였으면 visibilitychange 를 보낸다.
        hiddenByUnload = true;
        if (visibleAnnounced) {
            visibleAnnounced = false;
            page.dispatchTrustedEvent(page.getDocument(), new Event("visibilitychange", true, false));
        }
        page.dispatchTrustedEvent(page.getWindow(), new Event("unload", false, false));
        return before.isConfirmationRequested() ? before.getReturnValue() : null;
    }

    /**
     * Fires <code>visibilitychange</code> if the visibility that the host reports now differs from the one that the page heard about last.
     * <p>
     * 호스트가 지금 알리는 가시성이 페이지가 마지막으로 들은 것과 다르면 <code>visibilitychange</code> 를 발생시킨다.
     */
    public void visibilityChanged() {
        boolean now = page.getEnvironment().isDocumentVisible();
        if (unloaded || now == visibleAnnounced || page.isDisposed() || !page.getEnvironment().isScriptingEnabled()) {
            return;
        }
        visibleAnnounced = now;
        page.dispatchTrustedEvent(page.getDocument(), new Event("visibilitychange", true, false));
    }

    /**
     * Fires <code>focus</code> or <code>blur</code> at the window (it does not bubble and cannot be cancelled).
     * <p>
     * window 에 <code>focus</code> 또는 <code>blur</code> 를 발생시킨다(버블링하지 않고 취소할 수 없다).
     *
     * @param gained true if the window gained the focus / window 가 포커스를 얻었으면 true
     */
    public void windowFocus(boolean gained) {
        if (unloaded || page.isDisposed() || !page.getEnvironment().isScriptingEnabled()) {
            return;
        }
        page.dispatchTrustedEvent(page.getWindow(), new FocusEvent(gained ? "focus" : "blur", false, false));
    }

    /**
     * Fires <code>online</code> or <code>offline</code> at the window if the network state of the host differs from the one that the page heard about last.
     * <p>
     * 호스트의 네트워크 상태가 페이지가 마지막으로 들은 것과 다르면 window 에 <code>online</code> 또는 <code>offline</code> 을 발생시킨다.
     */
    public void onlineChanged() {
        boolean now = page.getEnvironment().isOnline();
        if (unloaded || now == onlineAnnounced || page.isDisposed() || !page.getEnvironment().isScriptingEnabled()) {
            return;
        }
        onlineAnnounced = now;
        page.dispatchTrustedEvent(page.getWindow(), new Event(now ? "online" : "offline", false, false));
    }
}
