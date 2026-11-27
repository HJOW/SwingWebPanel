package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;

/**
 * The component's top-level browsing context. Its identity lasts across navigations and its properties refer to the active document.
 * <p>
 * 컴포넌트의 최상위 브라우징 문맥. 이동해도 정체성을 유지하며 각 속성은 현재 문서를 가리킨다.
 */
final class TopContext implements BrowsingContext {

    /** The owner of the confirmed page. / 확정된 페이지를 소유한 세션. */
    private final BrowserSession session;

    /**
     * Creates the context.
     * <p>
     * 문맥을 만든다.
     *
     * @param session the component session / 컴포넌트 세션
     */
    TopContext(BrowserSession session) {
        this.session = session;
    }

    /**
     * Finds a top-level runtime already executing on this thread, including a page still being parsed.
     * <p>
     * 아직 파싱 중인 페이지를 포함하여 현재 스레드에서 실행하는 최상위 문서 환경을 찾는다.
     *
     * @return the local runtime or null / 현재 실행 환경 또는 null
     */
    private PageRuntime executingRuntime() {
        HostEnvironment env = PageScripts.currentHostEnvironment();
        if (env instanceof PageRuntime && env.getBrowsingContext() == this) {
            return (PageRuntime) env;
        }
        return null;
    }

    /**
     * Returns the confirmed HTML document and its page state.
     * <p>
     * 확정된 HTML 문서와 그 문서의 상태를 반환한다.
     *
     * @return the confirmed HTML page or null / 확정된 HTML 페이지 또는 null
     */
    private HtmlPageDocument confirmedPage() {
        PageDocument page = session.getSnapshot().getDocument();
        return page instanceof HtmlPageDocument ? (HtmlPageDocument) page : null;
    }

    /**
     * Returns the runtime visible to the caller.
     * <p>
     * 호출자가 볼 수 있는 실행 환경을 반환한다.
     *
     * @return the local or confirmed runtime or null / 현재 실행 중이거나 확정된 실행 환경 또는 null
     */
    private PageRuntime activeRuntime() {
        PageRuntime running = executingRuntime();
        if (running != null) {
            return running;
        }
        HtmlPageDocument page = confirmedPage();
        return page == null ? null : page.runtime();
    }

    /**
     * Returns the script host of the document.
     * <p>
     * 문서의 스크립트 호스트를 반환한다.
     *
     * @return the script host / 스크립트 호스트
     */
    public PageScripts getScripts() {
        PageRuntime runtime = activeRuntime();
        return runtime == null ? null : runtime.scripts();
    }

    /**
     * Returns the script host that receives the messages of other windows: the page whose scripts were installed last, even before
     * it is confirmed, like a message of the Java host.
     * <p>
     * 다른 창의 메시지를 받는 스크립트 호스트를 반환한다: 확정 전이라도 스크립트가 가장 최근에 설치된 페이지이며, Java 호스트의 메시지와 같다.
     *
     * @return the script host or null / 스크립트 호스트 또는 null
     */
    public PageScripts getMessageScripts() {
        PageRuntime running = executingRuntime();
        PageRuntime runtime = running != null ? running : session.messageTargetRuntime();
        return runtime == null ? null : runtime.scripts();
    }

    /**
     * Returns the DOM document.
     * <p>
     * DOM 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        PageRuntime running = executingRuntime();
        // 파싱 중인 스크립트는 아직 확정되지 않은 자기 문서를 보아야 한다.
        if (running != null) {
            return running.scripts().getDocument();
        }
        HtmlPageDocument page = confirmedPage();
        return page == null ? null : page.getDocument();
    }

    /**
     * The top-level context has no parent.
     * <p>
     * 최상위 문맥에는 부모가 없다.
     *
     * @return null / null
     */
    public BrowsingContext getParent() {
        return null;
    }

    /**
     * The top-level context is its own top.
     * <p>
     * 최상위 문맥은 자기가 최상위이다.
     *
     * @return this context / 이 문맥
     */
    public BrowsingContext getTop() {
        return this;
    }

    /**
     * Returns the contexts of the frames of the document.
     * <p>
     * 문서의 프레임의 문맥을 반환한다.
     *
     * @return a new list / 새 목록
     */
    public List<BrowsingContext> getChildren() {
        PageRuntime running = executingRuntime();
        HtmlPageDocument page = running == null ? confirmedPage() : null;
        FrameSet set = running != null ? running.frameSet() : page == null ? null : page.frames();
        List<BrowsingContext> out = new ArrayList<BrowsingContext>();
        // 프레임 집합의 문맥을 문서 순서대로 옮겨 담는다.
        if (set != null) {
            out.addAll(set.contexts());
        }
        return out;
    }

    /**
     * The top-level document is not inside a frame element.
     * <p>
     * 최상위 문서는 프레임 요소 안에 있지 않다.
     *
     * @return null / null
     */
    public Element getFrameElement() {
        return null;
    }

    /**
     * Returns the name of the window.
     * <p>
     * window 의 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getName() {
        PageRuntime runtime = activeRuntime();
        String own = runtime == null ? "" : runtime.getTopWindowName();
        // 페이지가 이름을 정하지 않았으면 창을 열 때 준 이름이 창의 이름이다.
        return own.length() == 0 ? session.windowName() : own;
    }

    /**
     * Sets the name of the window.
     * <p>
     * window 의 이름을 정한다.
     *
     * @param value the name / 이름
     */
    public void setName(String value) {
        PageRuntime runtime = activeRuntime();
        if (runtime != null) {
            runtime.setTopWindowName(value);
        }
    }

    /**
     * Returns the address that the origin of the document is computed from.
     * <p>
     * 문서의 오리진을 계산하는 원천 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public String getOriginUrl() {
        Document document = getDocument();
        return document == null ? "about:blank" : document.getOriginUrl();
    }

    /**
     * Navigates the top-level document through the navigation policy of the component.
     * <p>
     * 컴포넌트의 이동 정책을 거쳐 최상위 문서를 이동시킨다.
     *
     * @param url     the absolute address / 절대 주소
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    public void navigate(String url, boolean replace) {
        PageRuntime runtime = activeRuntime();
        // 그룹이 갈라진 창의 스크립트는 이 창을 이동시킬 수 없다.
        if (runtime != null && !isClosed()) {
            runtime.navigate(url, replace);
        }
    }

    /**
     * Opens a window for a script of the top-level document (<code>window.open</code>).
     * <p>
     * 최상위 문서의 스크립트를 위해 창을 연다(<code>window.open</code>).
     *
     * @param url    the absolute address or an empty string / 절대 주소 또는 빈 문자열
     * @param target the target name / 대상 이름
     * @return the context that the target selects, or null / 대상이 고르는 문맥. 없으면 null
     */
    public BrowsingContext open(String url, String target) {
        return open(url, target, "");
    }

    /**
     * Opens a window for a script of the top-level document, with the window features text.
     * <p>
     * 최상위 문서의 스크립트를 위해 창 기능 문자열과 함께 창을 연다.
     *
     * @param url      the absolute address or an empty string / 절대 주소 또는 빈 문자열
     * @param target   the target name / 대상 이름
     * @param features the window features text / 창 기능 문자열
     * @return the context that the target selects or the new window, or null / 대상이 고르는 문맥이나 새 창. 없으면 null
     */
    public BrowsingContext open(String url, String target, String features) {
        PageRuntime runtime = activeRuntime();
        return runtime == null ? null : FrameNavigator.open(session, runtime.frameSet(), null, this, target, url, features);
    }

    /**
     * Returns the context of the document that opened this window.
     * <p>
     * 이 창을 연 문서의 문맥을 반환한다.
     *
     * @return the opener, or null / opener. 없으면 null
     */
    public BrowsingContext getOpener() {
        return session.openerContext();
    }

    /**
     * The top-level context is not sandboxed: it may navigate any context.
     * <p>
     * 최상위 문맥은 샌드박스가 아니다: 어떤 문맥이든 이동시킬 수 있다.
     *
     * @param target the context / 문맥
     * @return true / true
     */
    public boolean mayNavigate(BrowsingContext target) {
        return true;
    }

    /**
     * Tells whether the component closed this browsing context.
     * <p>
     * 컴포넌트에서 이 브라우징 문맥을 닫았는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    public boolean isClosed() {
        if (session.isDisposed()) {
            return true;
        }
        // 다른 창의 스크립트가 묻는 경우: 브라우징 문맥 그룹이 갈라졌으면(COOP) 그 스크립트에게 이 창은 닫힌 창이다.
        HostEnvironment env = PageScripts.currentHostEnvironment();
        BrowsingContext from = env == null ? null : env.getBrowsingContext();
        BrowsingContext top = from == null ? null : from.getTop();
        return top instanceof TopContext && top != this && !((TopContext) top).sharesGroupWith(session);
    }

    /**
     * Tells whether the window of this context and the window of a session are in the same browsing context group.
     * <p>
     * 이 문맥의 창과 어떤 세션의 창이 같은 브라우징 문맥 그룹에 있는지 알려 준다.
     *
     * @param other the session of the other window / 다른 창의 세션
     * @return true if they share a group / 그룹이 같으면 true
     */
    boolean sharesGroupWith(BrowserSession other) {
        return session.sharesGroupWith(other);
    }

    /**
     * Tells whether a script asked to close the window of the component and the request was accepted.
     * <p>
     * 스크립트가 컴포넌트의 창을 닫으라고 요청했고 그 요청이 받아들여졌는지 알려 준다.
     *
     * @return true while closing / 닫는 중이면 true
     */
    public boolean isClosing() {
        return session.isWindowClosing();
    }

    /**
     * Runs <code>window.close()</code> on the window of the component: the component decides whether scripts may close it.
     * <p>
     * 컴포넌트의 창에 <code>window.close()</code> 를 실행한다. 스크립트가 닫을 수 있는지는 컴포넌트가 판정한다.
     *
     * @param caller the context of the calling script / 호출한 스크립트의 문맥
     * @return null if accepted or silently ignored, otherwise the reason for the console / 받았거나 조용히 무시했으면 null. 아니면 콘솔에 알릴 이유
     */
    public String close(BrowsingContext caller) {
        return session.closeWindowFromPage(caller);
    }
}
