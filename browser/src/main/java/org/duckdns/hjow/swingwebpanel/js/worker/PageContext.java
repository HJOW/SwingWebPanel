package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The {@link ScriptContext} of a page: tasks go to the scheduler of the component and run as page tasks (under the page lock, followed by the
 * microtask checkpoint) on the thread that owns the page's document.
 * <p>
 * 페이지의 {@link ScriptContext}: 작업은 컴포넌트의 스케줄러로 가서 페이지의 문서를 소유한 스레드에서 페이지 작업(페이지 잠금 아래, 마이크로태스크
 * 검사점이 뒤따름)으로 실행된다.
 */
final class PageContext implements ScriptContext {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Creates the context of a page.
     * <p>
     * 페이지의 문맥을 만든다.
     *
     * @param page the page / 페이지
     */
    PageContext(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the realm of the page.
     * <p>
     * 페이지의 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return page.getRealm();
    }

    /**
     * Returns the DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    public DomBridge getBridge() {
        return page.getBridge();
    }

    /**
     * Posts a task to the scheduler of the component; without a scheduler nothing is delivered (like the timers of a page).
     * <p>
     * 컴포넌트의 스케줄러에 작업을 올린다. 스케줄러가 없으면 아무것도 전달되지 않는다(페이지의 타이머와 같다).
     *
     * @param task the work / 작업
     */
    public void post(final Runnable task) {
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null || !isActive()) {
            return;
        }
        s.post(new Runnable() {
            /**
             * Runs the task as a page task if the page is still active.
             * <p>
             * 페이지가 아직 활성이면 작업을 페이지 작업으로 실행한다.
             */
            public void run() {
                if (isActive()) {
                    page.runTask(task);
                }
            }
        }, 0);
    }

    /**
     * Tells whether the page lives and its scripts are enabled.
     * <p>
     * 페이지가 살아 있고 스크립트가 켜져 있는지 알려 준다.
     *
     * @return true while active / 활성이면 true
     */
    public boolean isActive() {
        return !page.isDisposed() && page.getEnvironment().isScriptingEnabled();
    }

    /**
     * Returns the origin of the document of the page.
     * <p>
     * 페이지 문서의 오리진을 반환한다.
     *
     * @return the origin text, or null for an opaque origin / 오리진 문자열. 불투명 오리진이면 null
     */
    public String getOrigin() {
        String url = page.getDocument().getUrl();
        Origin o = Origin.of(url == null ? "about:blank" : url);
        return o.isOpaque() ? null : o.serialize();
    }

    /**
     * A page prints the failures of listeners that are not script errors, like the other event targets of the page.
     * <p>
     * 페이지는 스크립트 오류가 아닌 리스너 실패를 페이지의 다른 이벤트 대상처럼 출력한다.
     *
     * @return the printing handler / 출력 핸들러
     */
    public ListenerErrorHandler getListenerErrors() {
        return EventTarget.PRINTING_HANDLER;
    }
}
