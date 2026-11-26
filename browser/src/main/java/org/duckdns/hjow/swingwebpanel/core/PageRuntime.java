package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.content.ContentType;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.js.ModuleRequest;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.CorsImageRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.ImageRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.MediaRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleScriptSource;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptSource;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptText;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.HostMessageValues;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.OriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.RequestPurpose;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.ResourceRequest;
import org.duckdns.hjow.swingwebpanel.net.ResourceResponse;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.ImageResource;
import org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager;
import org.duckdns.hjow.swingwebpanel.storage.SessionStorage;

/**
 * One page's scripting host. The loader owns it through parsing, then the document queue owns tasks after confirmation.
 * Cancellation is nonblocking and final: enabling JavaScript again requires a new page. No navigation/history snapshot
 * is retained. Only the cleanup worker waits for a running script's lock (P15-08-b-2).
 * <p>페이지별 스크립트 호스트. 파싱 중에는 로더가 소유하고 확정 뒤에는 문서 큐에서 작업한다. 취소는 기다리지 않고 영구 적용되며
 * JavaScript를 다시 켜도 새 페이지가 필요하다. 이동 요청·이력 스냅샷을 보관하지 않고 해제 작업자만 실행 중인 스크립트 잠금을 기다린다.
 */
final class PageRuntime extends HostEnvironment implements ModuleScriptSource.WithAttributes, ScriptSource.WithResponse, CancelToken.Hook {
    /** Owning component state. / 소유 컴포넌트 상태. */
    private final BrowserSession session;
    /** Component view with document-queue query hooks. / 문서 큐 조회 훅이 있는 컴포넌트 뷰. */
    private final PageView view;
    /** Presentation shared by parser queries and confirmed drawing. / 파서 조회와 확정된 그리기가 공유하는 표현 상태. */
    private final PagePresentation presentation;
    /** Common component request policy. / 컴포넌트 공통 요청 정책. */
    private final ResourceFetcher fetcher;
    /** Component cookie jar. / 컴포넌트 쿠키 저장소. */
    private final CookieStore cookies;
    /** Copied request configuration. / 복사한 요청 설정. */
    private final PageRequestContext context;
    /** Actual final document URL, independent of base elements. / base 요소와 독립적인 최종 문서 URL. */
    private final String pageUrl;
    /** Time allowed for each script task of this document. / 이 문서의 스크립트 작업별 시간 한도. */
    private final long taskTimeLimitMillis;
    /** Object URLs of this document in the component store. / 컴포넌트 저장소의 이 문서 객체 URL. */
    private final BlobUrlStore.Scope blobUrls;
    /** Navigation cancellation, with no reference to a history entry. / 이력 항목 참조 없는 이동 취소. */
    private final CancelToken parent;
    /** Script resource cancellation independent of the main document. / 주 문서와 독립적인 스크립트 리소스 취소. */
    private final CancelToken resources = new CancelToken();
    /** Requests of XMLHttpRequest and fetch, cancelled together with the other script resources. / XMLHttpRequest 와 fetch 의 요청. 다른 스크립트 리소스와 함께 취소된다. */
    private final PageWebRequests webRequests;
    /** Document queue timers, buffered until confirmation. / 확정 전 보류하는 문서 큐 타이머. */
    private final DocumentScheduler scheduler;
    /** Single script realm and DOM bridge. / 단일 스크립트 영역과 DOM 연결. */
    private final PageScripts scripts;
    /** The console hook of this page, or null when the component has no console dispatcher. / 이 페이지의 콘솔 연결. 컴포넌트에 콘솔 통지기가 없으면 null. */
    private final PageConsole console;
    /** Component message dispatcher, or null without a Swing host. / 컴포넌트 메시지 통지기. Swing 호스트가 없으면 null. */
    private final WebMessageDispatcher hostMessages;
    /** Coalesces delivery checkpoints for sender tasks without a regular checkpoint. / 일반 검사점 없는 발신 작업의 전달 검사를 합친다. */
    private final AtomicBoolean hostCheckpointPending = new AtomicBoolean();
    /** Final lifetime flag, also used by racing constructors and setters. / 생성자와 설정 호출 경합에도 쓰는 영구 수명 표시. */
    private final AtomicBoolean closed = new AtomicBoolean();
    /** Parser ownership barrier before final realm cleanup. / 최종 영역 해제 전 파서 소유권 장벽. */
    private final CountDownLatch parsed = new CountDownLatch(1);
    /** In-document navigations (URL and replace flag) requested before the page was confirmed; guarded by itself. / 확정 전에 요청된 문서 안의 이동(URL과 replace 표시). 자기 자신으로 보호한다. */
    private final List<Object[]> waitingFragments = new ArrayList<Object[]>();
    /** True once the session confirmed this page; guarded by {@link #waitingFragments}. / 세션이 이 페이지를 확정한 뒤 true. {@link #waitingFragments} 로 보호한다. */
    private boolean confirmed;
    /** Kind of the navigation that produced this page (history length before confirmation). / 이 페이지를 만든 이동의 종류(확정 전의 이력 길이에 쓴다). */
    private final NavigationType type;
    /** The nested browsing context that this page is the document of, or null for the top-level document. / 이 페이지가 문서인 중첩 브라우징 문맥. 최상위 문서이면 null. */
    private final FrameContext frame;
    /** The browsing context that the scripts of this page see: the frame, or the top-level context. / 이 페이지의 스크립트가 보는 브라우징 문맥: 프레임, 또는 최상위 문맥. */
    private final org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext browsingContext;
    /** The permission to use the isolated agent capabilities. / 격리된 에이전트 권한의 사용 허용 상태. */
    private final boolean crossOriginIsolated;
    /** The name assigned by this top-level document. / 이 최상위 문서에서 설정한 이름. */
    private volatile String topWindowName = "";

    /**
     * Installs script services before the parser starts; call close on every abandoned parse.
     * <p>파서 시작 전에 스크립트 서비스를 설치하며 버린 파싱에서는 반드시 close를 호출한다.
     * @param session owning session / 소유 세션
     * @param fetcher common fetcher / 공통 가져오기 객체
     * @param cookies component cookies / 컴포넌트 쿠키
     * @param document new unshared DOM / 새 독립 DOM
     * @param request navigation settings, copied rather than retained / 보관하지 않고 복사할 이동 설정
     * @param context resource settings / 리소스 설정
     * @param view component presentation / 컴포넌트 표현
     * @param presentation page-owned state / 페이지 소유 상태
     */
    PageRuntime(BrowserSession session, ResourceFetcher fetcher, CookieStore cookies, Document document,
            NavigationRequest request, PageRequestContext context, PageView view, PagePresentation presentation) {
        this.session = session;
        this.view = view;
        this.presentation = presentation;
        this.fetcher = fetcher;
        this.cookies = cookies;
        this.context = context;
        frame = request.getFrame();
        browsingContext = frame != null ? frame : session.topContext();
        pageUrl = document.getOriginUrl();
        taskTimeLimitMillis = frame == null ? view.scriptTaskTimeLimitMillis(document.getUrl(), DEFAULT_TASK_TIME_LIMIT_MILLIS)
                : DEFAULT_TASK_TIME_LIMIT_MILLIS;
        BlobUrlStore.Entry entry = request.getBlobUrlEntry();
        // 정상적으로 상속하는 문서는 URL 생성자의 오리진을 받고 샌드박스 문서는 새 불투명 범위를 쓴다.
        blobUrls = entry != null && (frame == null || frame.flags().allowsSameOrigin())
                ? entry.openDocumentScope() : fetcher.getBlobUrlStore().open(pageUrl);
        Origin origin = Origin.of(pageUrl);
        CrossOriginPolicy policy = context.crossOriginPolicy;
        crossOriginIsolated = origin.isPotentiallyTrustworthy() && policy.getEmbedder() != CrossOriginPolicy.Embedder.UNSAFE_NONE
                && policy.allowsIsolation(origin)
                && (frame == null ? "same-origin".equals(policy.getOpener()) : frame.inheritsIsolation(origin));
        webRequests = new PageWebRequests(fetcher, cookies, context, pageUrl, resources, blobUrls);
        presentation.setBlobUrlScope(blobUrls);
        parent = request.getCancelToken();
        scheduler = new DocumentScheduler(session);
        hostMessages = session.webMessageDispatcher();
        scripts = new PageScripts(document, this);
        if (hostMessages != null) {
            scripts.onTaskEnd(new Runnable() {
                /** Releases the task's messages after its microtasks. / 마이크로태스크 뒤에 작업 메시지를 전달 가능하게 한다. */
                public void run() {
                    hostMessages.finishTask(PageRuntime.this);
                }
            });
        }
        // 컴포넌트가 콘솔 통지기를 가졌으면 이 페이지의 콘솔 출력(스크립트, 프레임, worker, 호스트의 보고)을 그리로 보낸다.
        ConsoleDispatcher consoles = session.consoleDispatcher();
        if (consoles != null) {
            console = new PageConsole(consoles, pageUrl, frame != null);
            scripts.getRealm().setConsole(console);
        } else {
            console = null;
        }
        // 렌더러가 캔버스 그림을 스크립트 호스트에서 읽게 한다.
        presentation.setCanvasSource(scripts.canvasSource());
        // 오디오 컨트롤 막대도 같은 방식으로 요소의 상태를 스크립트 호스트에서 읽는다.
        presentation.setMediaControlSource(scripts.mediaControlSource());
        type = request.getType();
        // 이력 이동과 새로고침으로 다시 읽은 문서는 그 이력 항목의 상태 객체를 history.state 로 갖고 시작한다.
        if ((type == NavigationType.HISTORY || type == NavigationType.REFRESH) && request.getRestore() != null) {
            scripts.setInitialHistoryState(request.getRestore().getHistoryState());
        }
        // 리소스가 끝나면 스크립트 호스트가 load 이벤트를 다시 판단하게 한다. 문서 큐에 작업만 올리므로 작업자 스레드에서 불러도 안전하다.
        presentation.setResourceObserver(new Runnable() {
            /** Tells the script host that a resource ended. / 리소스가 끝났음을 스크립트 호스트에 알린다. */
            public void run() {
                scripts.resourcesChanged();
            }
        });
        parent.addHook(this);
        if (!session.registerRuntime(this, request.getId())) {
            close();
        }
    }

    /** Returns the component that owns this page. / 이 페이지를 소유한 컴포넌트를 반환한다.
     * @return the component state / 컴포넌트 상태 */
    BrowserSession session() {
        return session;
    }

    /**
     * Returns the window name of this top-level document.
     * <p>
     * 이 최상위 문서의 창 이름을 반환한다.
     *
     * @return the current name / 현재 이름
     */
    String getTopWindowName() {
        return topWindowName;
    }

    /**
     * Stores the name for this document only.
     * <p>
     * 이 문서의 창 이름을 저장한다.
     *
     * @param value the name, or null to clear / 이름. null이면 비운다
     */
    void setTopWindowName(String value) {
        topWindowName = value == null ? "" : value;
    }

    /**
     * Returns the live frame set of this document, including during parsing.
     * <p>
     * 파싱 중인 경우를 포함하여 이 문서의 현재 프레임 집합을 반환한다.
     *
     * @return the frame set or null / 프레임 집합 또는 null
     */
    FrameSet frameSet() {
        return presentation.frameSet();
    }

    /** Returns the page bridge on its owning thread. / 소유 스레드에서 페이지 연결을 반환한다.
     * @return script host / 스크립트 호스트 */
    PageScripts scripts() {
        return scripts;
    }

    /**
     * Enqueues a Java message only for this document's origin.
     * <p>
     * 이 문서의 오리진이 일치할 때만 Java 메시지를 큐에 올린다.
     *
     * @param data captured host value / 호스트에서 복사한 값
     * @param expected target origin, or null for a wildcard / 대상 오리진. 와일드카드면 null
     */
    void postHostMessage(ClonedValue data, Origin expected) {
        if (acceptsHostMessages() && (expected == null || expected.isSameOrigin(Origin.of(pageUrl)))) {
            scripts.receiveHostMessage(data);
        }
    }

    /** Returns the component's fixed host origin. / 컴포넌트의 고정 호스트 오리진을 반환한다. */
    @Override
    public String getHostMessageOrigin() {
        return hostMessages == null ? null : SwingWebPanel.HOST_ORIGIN;
    }

    /** Checks whether this runtime may still exchange host messages. / 이 환경에서 아직 호스트 메시지를 주고받을 수 있는지 확인한다. */
    boolean acceptsHostMessages() {
        return hostMessages != null && !unloading.get() && isScriptingEnabled();
    }

    /**
     * Captures sender metadata before asynchronous Java delivery.
     * <p>
     * Java에 비동기 전달하기 전에 발신자의 메타데이터를 확보한다.
     *
     * @param data copied Java data / 복사한 Java 데이터
     */
    @Override
    public void publishHostMessage(HostMessageValues.JavaMessage data) {
        if (!acceptsHostMessages()) {
            return;
        }
        WebMessageEvent.SourceKind type = frame == null ? WebMessageEvent.SourceKind.DOCUMENT : WebMessageEvent.SourceKind.FRAME;
        // 일반 검사점이 없는 작업도 다음 문서 큐 실행에서 이미 접수한 메시지를 전달한다.
        if (hostMessages.enqueue(this, data, Origin.of(pageUrl), scripts.getDocument().getUrl(), type)
                && hostCheckpointPending.compareAndSet(false, true)) {
            scheduler.post(new Runnable() {
                /** Releases messages after the current document task has returned. / 현재 문서 작업이 반환된 뒤 메시지를 전달 가능하게 한다. */
                public void run() {
                    hostCheckpointPending.set(false);
                    hostMessages.finishTask(PageRuntime.this);
                }
            }, 0);
        }
    }

    /** Activates buffered timers after ownership handoff and then applies the in-document navigations that the page requested while it was
     * still being parsed. / 소유권 이전 뒤 보류 타이머를 활성화하고, 파싱 중에 페이지가 요청한 문서 안의 이동을 적용한다.
     * @param snapshot confirmed page / 확정된 페이지
     * @return whether the document is active / 문서의 활성화 여부 */
    boolean activate(PageSnapshot snapshot) {
        // 프레임의 문서는 뷰를 직접 준비하지 않는다(뷰는 최상위 페이지의 것이고 프레임은 그 DisplayList 안에 그려진다).
        if (frame == null) {
            view.preparePage(snapshot);
        }
        if (!scheduler.activate(snapshot)) {
            return false;
        }
        List<Object[]> waiting;
        synchronized (waitingFragments) {
            if (closed.get()) {
                return false;
            }
            confirmed = true;
            waiting = new ArrayList<Object[]>(waitingFragments);
            waitingFragments.clear();
        }
        // 확정되기 전에 요청된 같은 문서 안의 이동과 이력 상태 변경을 요청된 순서대로 적용한다.
        for (Object[] w : waiting) {
            if (closed.get()) {
                continue;
            }
            if (w.length == 3) {
                applyHistoryState((ClonedValue) w[0], (String) w[1], ((Boolean) w[2]).booleanValue());
            } else {
                applyNavigation((String) w[0], ((Boolean) w[1]).booleanValue());
            }
        }
        return !closed.get();
    }

    /**
     * Applies a history state change of the page: the component history for the top-level document, the local history of the frame for
     * the document of a frame.
     * <p>
     * 페이지의 이력 상태 변경을 적용한다: 최상위 문서는 컴포넌트 이력, 프레임의 문서는 프레임의 지역 이력.
     *
     * @param state   the serialized state or null / 직렬화된 상태 또는 null
     * @param url     the new absolute URL / 새 절대 URL
     * @param replace true to replace the entry / 항목을 바꾸려면 true
     */
    private void applyHistoryState(ClonedValue state, String url, boolean replace) {
        if (frame != null) {
            frame.scriptUpdateHistory(state, url, replace);
        } else {
            session.updateHistoryFromPage(state, url, replace);
        }
    }

    /**
     * Starts a navigation that the page asked for: of the component for the top-level document, of the frame for the document of a frame.
     * <p>
     * 페이지가 요청한 이동을 시작한다: 최상위 문서는 컴포넌트의, 프레임의 문서는 프레임의 이동.
     *
     * @param url     the absolute target address / 절대 대상 주소
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    private void applyNavigation(String url, boolean replace) {
        if (frame != null) {
            frame.scriptNavigate(url, replace);
        } else {
            session.navigateFromPage(url, replace);
        }
    }

    /**
     * Applies <code>history.pushState</code> or <code>replaceState</code> of this page: the history entry of the page takes the new URL and
     * state (no request, no navigation). A call during parsing waits until the page has been confirmed, in order with the in-document
     * navigations that wait too.
     * <p>이 페이지의 <code>history.pushState</code>, <code>replaceState</code> 를 적용한다: 페이지의 이력 항목이 새 URL과 상태를 갖는다(요청도 이동도
     * 없다). 파싱 중의 호출은 이 페이지가 확정될 때까지 기다리며, 마찬가지로 기다리는 문서 안의 이동과 순서를 지킨다.
     * @param state the serialized state or null / 직렬화된 상태 또는 null
     * @param url the new absolute URL / 새 절대 URL
     * @param replace true to replace the entry / 항목을 바꾸려면 true
     */
    public void updateHistoryState(ClonedValue state, String url, boolean replace) {
        if (!isScriptingEnabled()) {
            return;
        }
        synchronized (waitingFragments) {
            // 최상위 문서의 이력 항목은 페이지가 확정되어야 생기므로 기다린다. 프레임의 지역 이력 항목은 이동을 시작할 때 이미 있으므로 곧바로
            // 적용한다(앞서 기다리는 이동이 있으면 순서를 지키려고 그 뒤에 둔다).
            if (!confirmed && (frame == null || !waitingFragments.isEmpty())) {
                waitingFragments.add(new Object[] { state, url, Boolean.valueOf(replace) });
                return;
            }
        }
        applyHistoryState(state, url, replace);
    }

    /**
     * Returns the number of entries that <code>history.length</code> shows.
     * <p>
     * <code>history.length</code> 가 보여 주는 항목 수를 반환한다.
     *
     * @return the number of history entries / 이력 항목 수
     */
    public int getHistoryLength() {
        if (frame != null) {
            return frame.historyLength();
        }
        boolean done;
        synchronized (waitingFragments) {
            done = confirmed;
        }
        return session.historyLength(done, type);
    }

    /**
     * Returns the component-selected time budget of this page.
     * <p>
     * 컴포넌트가 선택한 페이지의 시간 한도를 반환한다.
     *
     * @return the positive duration in milliseconds / 양수 시간(밀리초)
     */
    public long getTaskTimeLimitMillis() {
        return taskTimeLimitMillis;
    }

    /**
     * Starts a navigation that a script of this page asked for (<code>location.href</code>, <code>assign</code>, <code>replace</code>). The
     * navigation policy of the component decides: only the allowed schemes, and a network page never reaches the class path. A move that
     * only changes the fragment waits until this page has been confirmed, because it is a navigation inside the confirmed document.
     * <p>이 페이지의 스크립트가 요청한 이동(<code>location.href</code>, <code>assign</code>, <code>replace</code>)을 시작한다. 컴포넌트의 이동 정책이
     * 판정한다: 허용된 스킴만 가능하고 네트워크 페이지는 클래스패스에 닿지 못한다. 프래그먼트만 바꾸는 이동은 확정된 문서 안의 이동이므로 이 페이지가
     * 확정될 때까지 기다린다.
     * @param url absolute target address / 절대 대상 주소
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    public void navigate(String url, boolean replace) {
        if (!isScriptingEnabled()) {
            return;
        }
        // 샌드박스된 프레임의 스크립트는 자기와 자손 말고는(allow-top-navigation 이 있으면 최상위도) 이동시킬 수 없다. 같은 오리진 window 의
        // location 대입도 이 길을 지나므로 호출한 문서(entry)의 샌드박스를 여기서 확인한다.
        if (!FrameNavigator.permitted(browsingContext)) {
            scripts.getRealm().getConsole().message("error", "Navigation to " + url + " is blocked by the sandbox of the navigating frame");
            return;
        }
        String source = pageUrl;
        // 프레임의 문서는 오리진 주소(샌드박스에서는 불투명)가 아니라 실제 네트워크 문서의 주소로 이동 정책을 판정한다.
        if (frame != null) {
            String own = scripts.getDocument().getUrl();
            source = own != null && !own.startsWith("about:") ? own : frame.owner.sourceUrl();
        }
        // 다른 창의 Location을 쓰는 경우에도 파일 권한은 호출한 문서에서 얻는다.
        if (UrlUtil.SCHEME_FILE.equals(UrlUtil.getScheme(url))) {
            org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext entry = PageScripts.entryContext();
            source = entry == null ? pageUrl : entry.getOriginUrl();
        }
        if (!NavigationPolicy.allowed(source, url)) {
            scripts.getRealm().getConsole().message("error", "Navigation to " + url + " is not allowed");
            return;
        }
        // 같은 문서 안의 이동인지는 문서의 현재 URL(history.pushState 로 바뀌었을 수 있다)과 비교한다.
        // 프레임 안의 스크립트는 프레임 자기 자신을 이동시킨다(같은 문서 안의 이동은 프레임이 처리한다). 최상위는 컴포넌트가 처리한다.
        if (frame != null) {
            // 프레임의 문서가 확정되기 전(파싱 중)의 같은 문서 안의 이동은 확정될 때까지 기다린다. 그러지 않으면 프레임을 다시 읽어 스크립트를 되풀이한다.
            if (NavigationPolicy.hasFragment(url) && NavigationPolicy.sameDocument(scripts.getDocument().getUrl(), url)) {
                synchronized (waitingFragments) {
                    if (!confirmed) {
                        waitingFragments.add(new Object[] { url, Boolean.valueOf(replace) });
                        return;
                    }
                }
            }
            frame.scriptNavigate(url, replace);
            return;
        }
        if (NavigationPolicy.hasFragment(url) && NavigationPolicy.sameDocument(scripts.getDocument().getUrl(), url)) {
            synchronized (waitingFragments) {
                if (!confirmed) {
                    waitingFragments.add(new Object[] { url, Boolean.valueOf(replace) });
                    return;
                }
            }
            // 같은 문서 안의 이동은 URL이 곧바로 바뀐다. 스크립트가 바로 다음 문장에서 location.href 를 읽어도 새 주소가 보여야 한다.
            String canonical = url;
            try {
                String normalized = UrlUtil.normalize(url);
                canonical = normalized == null ? url : normalized;
            } catch (RuntimeException e) {
                canonical = url;
            }
            // 주소가 달라지면 새 항목은 상태가 없으므로 history.state 도 곧바로 null 이 된다(popstate 는 항목이 확정될 때 문서 큐에서 온다).
            if (!canonical.equals(scripts.getDocument().getUrl())) {
                scripts.clearHistoryState();
            }
            scripts.getDocument().setUrl(canonical);
        }
        session.navigateFromPage(url, replace);
    }

    /**
     * Resolves the object of an anchor click, including an anchor that is detached from the tree.
     * <p>
     * 트리에 연결하지 않은 앵커를 포함하여 클릭한 앵커의 객체를 해석한다.
     *
     * @param link anchor with a download attribute / 다운로드 속성이 있는 앵커
     */
    public void activateObjectDownload(Element link) {
        if (!isScriptingEnabled() || link.getOwnerDocument() != scripts.getDocument()
                || frame != null && !frame.flags().allowsDownloads()) {
            return;
        }
        String url = PageInput.resolve(link);
        BlobUrlStore.Entry blob = "blob".equals(UrlUtil.getScheme(url)) ? blobUrls.resolve(url) : null;
        if (blob != null) {
            session.downloadBlobFromPage(url, link.getAttribute("download"), blob);
        }
    }

    /**
     * Reloads the page for a script (<code>location.reload()</code>).
     * <p>스크립트를 위해 페이지를 다시 읽는다(<code>location.reload()</code>).
     */
    public void reload() {
        if (isScriptingEnabled()) {
            if (frame != null) {
                frame.scriptReload();
            } else {
                session.reloadFromPage();
            }
        }
    }

    /**
     * Moves in the history for a script (<code>history.go</code>, <code>back</code>, <code>forward</code>).
     * <p>스크립트를 위해 이력에서 이동한다(<code>history.go</code>, <code>back</code>, <code>forward</code>).
     * @param delta number of entries, negative for back / 칸 수(뒤로는 음수)
     */
    public void historyGo(int delta) {
        if (isScriptingEnabled()) {
            if (frame != null) {
                frame.scriptHistoryGo(delta);
            } else {
                session.historyGoFromPage(delta);
            }
        }
    }

    /** Returns the copied viewport width. / 복사한 뷰포트 폭을 반환한다.
     * @return CSS pixel width / CSS 픽셀 폭 */
    public int getViewportWidth() {
        return frame != null ? frame.viewportWidth() : view.viewportWidth();
    }

    /** Returns the copied viewport height. / 복사한 뷰포트 높이를 반환한다.
     * @return CSS pixel height / CSS 픽셀 높이 */
    public int getViewportHeight() {
        return frame != null ? frame.viewportHeight() : view.viewportHeight();
    }

    /** Flushes current styles on the parser or document owner. / 파서 또는 문서 소유자에서 현재 스타일을 반영한다.
     * @return active resolver, or null after retirement / 활성 리졸버이며 폐기 후에는 null */
    public StyleResolver getStyleResolver() {
        return presentation.styleResolver(getViewportWidth(), getViewportHeight());
    }

    /** Returns the CSS engine of the page (the CSSOM reads and edits its style sheets) with the pending document changes applied. / 페이지의 CSS 엔진(CSSOM 이 그 스타일 시트를 읽고 고친다)을 문서의 대기 중인 변경을 적용하여 반환한다.
     * @return the engine, or null after retirement / 엔진이며 폐기 후에는 null */
    public CssEngine getCssEngine() {
        return presentation.cssEngine(getViewportWidth(), getViewportHeight());
    }

    /** Returns the current element rectangle in viewport coordinates. / 뷰포트 좌표로 현재 요소 사각형을 반환한다.
     * @param element element to query / 조회할 요소
     * @return x, y, width, height / x·y·폭·높이 */
    public double[] getElementBounds(Element element) {
        return presentation.bounds(element, getViewportWidth(), getViewportHeight(),
                presentation.isParsing() ? new java.awt.Point() : frame != null ? frame.scrollPosition() : view.viewportPosition());
    }

    /** Returns the client rectangles of a range in viewport coordinates. / 뷰포트 좌표로 범위의 클라이언트 사각형을 반환한다.
     * @param range the range / 범위
     * @return rectangles of x, y, width, height / x·y·폭·높이 사각형 목록 */
    public double[][] getRangeRects(org.duckdns.hjow.swingwebpanel.dom.Range range) {
        return presentation.rangeRects(range, getViewportWidth(), getViewportHeight(),
                presentation.isParsing() ? new java.awt.Point() : frame != null ? frame.scrollPosition() : view.viewportPosition());
    }

    /** Returns the text of a range as it is copied. / 범위의 복사될 때의 텍스트를 반환한다.
     * @param range the range / 범위
     * @return the text or null / 텍스트. 없으면 null */
    public String getRangeText(org.duckdns.hjow.swingwebpanel.dom.Range range) {
        return presentation.rangeText(range, getViewportWidth(), getViewportHeight());
    }

    /**
     * Returns the rendered text after applying pending layout changes. / 대기 중인 배치 변경을 적용한 뒤 렌더링 텍스트를 반환한다.
     * @param element the queried element / 조회할 요소
     * @return the text or null after retirement / 텍스트. 폐기 후에는 null
     */
    public String getInnerText(Element element) {
        return presentation.innerText(element, getViewportWidth(), getViewportHeight());
    }

    /** Returns the border boxes of an element in viewport coordinates, one per box. / 뷰포트 좌표로 요소의 border box 를 상자마다 하나씩 반환한다.
     * @param element queried element / 조회할 요소
     * @return rectangles of x, y, width, height / 사각형 목록 */
    public double[][] getElementRects(Element element) {
        return presentation.elementRects(element, getViewportWidth(), getViewportHeight(),
                presentation.isParsing() ? new java.awt.Point() : frame != null ? frame.scrollPosition() : view.viewportPosition());
    }

    /** The scroll position that a script asked for and the time of the request; it stands in for the copied viewport position until the viewport has moved. / 스크립트가 요청한 스크롤 위치와 요청 시각. 뷰포트가 움직일 때까지 복사한 뷰포트 위치를 대신한다. */
    private volatile double[] requestedScroll;

    /** How long a requested scroll position stands in for the real one (milliseconds). / 요청한 스크롤 위치가 실제 위치를 대신하는 시간(밀리초). */
    private static final long REQUEST_HOLD_MILLIS = 250;

    /** Returns the requested position if it is still held, else null. / 요청한 위치가 아직 유효하면 반환하고 아니면 null. */
    private double[] heldScroll() {
        double[] r = requestedScroll;
        if (r != null && System.currentTimeMillis() - (long) r[2] > REQUEST_HOLD_MILLIS) {
            requestedScroll = null;
            return null;
        }
        return r;
    }

    /** Returns the horizontal scroll position (<code>scrollX</code>). / 가로 스크롤 위치를 반환한다(<code>scrollX</code>).
     * @return the position / 위치 */
    public int getScrollX() {
        double[] r = heldScroll();
        return r != null ? (int) Math.round(r[0]) : view.viewportPosition().x;
    }

    /** Returns the vertical scroll position (<code>scrollY</code>). / 세로 스크롤 위치를 반환한다(<code>scrollY</code>).
     * @return the position / 위치 */
    public int getScrollY() {
        double[] r = heldScroll();
        return r != null ? (int) Math.round(r[1]) : view.viewportPosition().y;
    }

    /** Asks the component to scroll (the hook of <code>window.scrollTo</code>). / 컴포넌트에 스크롤을 요청한다(<code>window.scrollTo</code> 훅).
     * @param x x in document pixels / 문서 픽셀의 x
     * @param y y in document pixels / 문서 픽셀의 y */
    public void scrollTo(double x, double y) {
        double nx = Math.max(0, x);
        double ny = Math.max(0, y);
        requestedScroll = new double[] { nx, ny, System.currentTimeMillis() };
        session.requestScroll(nx, ny);
    }

    /** Tells whether a media query matches the page now (the hook of <code>window.matchMedia</code>). / 미디어 질의가 지금 페이지에 일치하는지 알려 준다(<code>window.matchMedia</code> 훅).
     * @param query media query list text / 미디어 질의 목록 문자열
     * @return true if it matches / 일치하면 true */
    public boolean matchesMedia(String query) {
        return presentation.matchesMedia(query, view.viewportWidth(), view.viewportHeight());
    }

    /** Reports a viewport change to the page as <code>resize</code>/<code>scroll</code> events and media query changes, on the document queue after the load event. / 뷰포트 변화를 load 이벤트 뒤 문서 큐에서 <code>resize</code>/<code>scroll</code> 이벤트와 미디어 질의 변화로 페이지에 알린다.
     * @param resized true if the size changed / 크기가 바뀌었으면 true
     * @param scrolled true if the scroll position changed / 스크롤 위치가 바뀌었으면 true */
    void viewportChanged(final boolean resized, final boolean scrolled) {
        if (closed.get()) {
            return;
        }
        session.submitDocumentTask(new Runnable() {
            /** Fires the events if the page is still live and loaded. / 페이지가 살아 있고 로드되었으면 이벤트를 발생시킨다. */
            public void run() {
                if (scrolled) {
                    requestedScroll = null;
                }
                if (!closed.get() && scripts.isLoadComplete()) {
                    scripts.viewportChanged(resized, scrolled);
                }
            }
        });
    }

    /** Tells whether the document counts as visible: the component that shows it is on the screen. / 문서가 보이는 것으로 치는지 알려 준다: 문서를 보여 주는 컴포넌트가 화면에 있다.
     * @return true while visible / 보이는 동안 true */
    public boolean isDocumentVisible() {
        return session.isDocumentVisible();
    }

    /** Tells what kind of navigation made the page: <code>reload</code> for a refresh, <code>back_forward</code> for a move in the history, else <code>navigate</code> (Navigation Timing). / 페이지를 만든 이동의 종류를 알려 준다: 새로고침은 <code>reload</code>, 이력 이동은 <code>back_forward</code>, 그 밖에는 <code>navigate</code>(Navigation Timing).
     * @return the type / 종류 */
    public String getNavigationType() {
        return type == NavigationType.REFRESH ? "reload" : type == NavigationType.HISTORY ? "back_forward" : "navigate";
    }

    /** Tells whether the host says that it is online (<code>navigator.onLine</code>). / 호스트가 연결되어 있다고 하는지 알려 준다(<code>navigator.onLine</code>).
     * @return true while online / 연결된 동안 true */
    public boolean isOnline() {
        return session.isOnline();
    }

    /** Tells the page that the visibility (0) or the network state (1) of the host changed; the events run on the document queue. / 호스트의 가시성(0)이나 네트워크 상태(1)가 바뀌었음을 페이지에 알린다. 이벤트는 문서 큐에서 실행된다.
     * @param kind 0 for visibility, 1 for the network state / 0 이면 가시성, 1 이면 네트워크 상태 */
    void lifecycleChanged(final int kind) {
        if (closed.get()) {
            return;
        }
        session.submitDocumentTask(new Runnable() {
            /** Fires the event if the page is still live and loaded. / 페이지가 살아 있고 로드되었으면 이벤트를 발생시킨다. */
            public void run() {
                if (!closed.get() && scripts.isLoadComplete()) {
                    if (kind == 0) {
                        scripts.getLifecycle().visibilityChanged();
                    } else {
                        scripts.getLifecycle().onlineChanged();
                    }
                }
            }
        });
    }

    /** Tells the page that the component published a new layout (the geometry observers look at the page again). / 컴포넌트가 새 레이아웃을 게시했음을 페이지에 알린다(기하 관찰자가 페이지를 다시 본다). */
    void layoutPublished() {
        if (!closed.get()) {
            scripts.layoutPublished();
        }
    }

    /** Fires <code>focus</code> or <code>blur</code> at the window of the page; call it on the document queue. / 페이지의 window 에 <code>focus</code> 또는 <code>blur</code> 를 발생시킨다. 문서 큐에서 부른다.
     * @param gained true if the window gained the focus / 포커스를 얻었으면 true */
    void windowFocus(boolean gained) {
        if (!closed.get() && scripts.isLoadComplete()) {
            scripts.getLifecycle().windowFocus(gained);
        }
    }

    /**
     * Runs the unloading events of the page (<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>) and waits for them at most the given time; if the
     * calling thread is the document thread they run at once. A page that cannot run scripts, was never shown or listens to none of them does nothing. The
     * answer of <code>beforeunload</code> goes to the status text (the component has no dialog).
     * <p>
     * 페이지의 언로드 이벤트(<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>)를 실행하고 주어진 시간까지 기다린다. 호출한 스레드가 문서
     * 스레드이면 곧바로 실행한다. 스크립트를 실행할 수 없거나 한 번도 보이지 않았거나 그 어느 것도 듣지 않는 페이지는 아무것도 하지 않는다. <code>beforeunload</code> 의
     * 답은 상태 메시지로 간다(컴포넌트에는 대화상자가 없다).
     *
     * @param waitMs the longest wait in milliseconds / 가장 긴 대기 시간(밀리초)
     * @return true if the unloading events were started / 언로드 이벤트를 시작했으면 true
     */
    boolean unloadNow(long waitMs) {
        if (!canUnload() || !unloading.compareAndSet(false, true)) {
            return false;
        }
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        Runnable work = new Runnable() {
            /** Runs the unloading sequence and tells the status text. / 언로드 순서를 실행하고 상태 메시지로 알린다. */
            public void run() {
                try {
                    String confirmation = scripts.getLifecycle().unload();
                    if (confirmation != null) {
                        session.setStatusMessage(confirmation.length() == 0 ? "The page asked to confirm leaving it (beforeunload)"
                                : "The page asked to confirm leaving it (beforeunload): " + confirmation);
                    }
                } finally {
                    done.countDown();
                }
            }
        };
        // 호출한 스레드가 문서 스레드이면 곧바로, 아니면 문서 큐에 올리고 정해진 시간까지 기다린다.
        if (session.isDocumentThread()) {
            work.run();
            return true;
        }
        if (!session.submitDocumentTask(work)) {
            return false;
        }
        try {
            done.await(waitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    /** True once the unloading sequence was started, so that it runs at most once. / 언로드 순서를 시작한 뒤이면 true. 많아야 한 번 실행되게 한다. */
    private final AtomicBoolean unloading = new AtomicBoolean();

    /** Tells whether the unloading events could reach a listener: the page runs scripts, was shown and has a listener for them. / 언로드 이벤트가 리스너에 닿을 수 있는지 알려 준다: 페이지가 스크립트를 실행하고, 보였으며, 리스너가 있다.
     * @return true if a listener exists / 리스너가 있으면 true */
    private boolean canUnload() {
        return !closed.get() && isScriptingEnabled() && scripts.isPageShown() && scripts.getLifecycle().hasUnloadListeners();
    }

    /** Returns the topmost element at a viewport point (the hook of <code>document.elementFromPoint</code>). / 뷰포트 점의 최상위 요소를 반환한다(<code>document.elementFromPoint</code> 훅).
     * @param x viewport x / 뷰포트 x
     * @param y viewport y / 뷰포트 y
     * @return the element or null / 요소 또는 null */
    public Element getElementAt(double x, double y) {
        return presentation.elementAt(x, y, view.viewportWidth(), view.viewportHeight(),
                presentation.isParsing() ? new java.awt.Point() : view.viewportPosition());
    }

    /** Waits for this parser's eligible styles while allowing immediate script cancellation. / 즉시 스크립트 취소를 허용하며 이 파서의 차단 대상 스타일을 기다린다.
     * @param script parser-created script / 파서 생성 스크립트 */
    public void prepareParserScript(Element script) {
        if (isScriptingEnabled()) {
            presentation.prepareParserScript(resources, getViewportWidth(), getViewportHeight());
        }
    }

    /** Ends parser ownership on both success and failure. / 성공·실패 모두에서 파서 소유권을 끝낸다. */
    void parsingFinished() {
        parsed.countDown();
    }

    /** Returns whether cancellation is final. / 취소가 확정되었는지 반환한다.
     * @return closed flag / 닫힘 표시 */
    boolean isClosed() {
        return closed.get();
    }

    /** Cancels script work without waiting on the script lock. / 스크립트 잠금을 기다리지 않고 작업을 취소한다. */
    public void onCancel() {
        // 보인 페이지에 언로드 이벤트의 리스너가 있으면 문서 큐에서 그 이벤트를 먼저 실행하고 닫는다(이동이 시작될 때; clean 과 dispose 는 미리 기다린다).
        if (canUnload() && unloading.compareAndSet(false, true)) {
            boolean queued = session.submitDocumentTask(new Runnable() {
                /** Runs the unloading events, then closes the page. / 언로드 이벤트를 실행한 뒤 페이지를 닫는다. */
                public void run() {
                    try {
                        String confirmation = scripts.getLifecycle().unload();
                        if (confirmation != null) {
                            session.setStatusMessage(confirmation.length() == 0 ? "The page asked to confirm leaving it (beforeunload)"
                                    : "The page asked to confirm leaving it (beforeunload): " + confirmation);
                        }
                    } finally {
                        close();
                        presentation.retire();
                    }
                }
            });
            if (queued) {
                return;
            }
        }
        close();
        presentation.retire();
    }

    /** Releases callbacks immediately and finishes locked cleanup off the EDT/document queue. / 콜백을 즉시 해제하고 잠금이 필요한 정리는 EDT·문서 큐 밖에서 끝낸다. */
    void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (hostMessages != null) {
            hostMessages.discard(this);
        }
        // 페이지를 떠난 뒤 아직 도는 스크립트의 메시지가 리스너에 닿지 않도록 먼저 콘솔을 닫는다.
        if (console != null) {
            console.close();
        }
        scripts.requestDisposal();
        scheduler.close();
        resources.cancel();
        parent.removeHook(this);
        session.unregisterRuntime(this);
        // 스크립트가 더 실행될 수 없게 되었으므로 뷰가 로딩 진행률을 마무리하게 한다(스크립트를 끄면 load 를 기다리지 않는다).
        presentation.loadChanged();
        PageCleanup.submit(new Runnable() {
            /** Finishes cleanup even if navigation shut down its executor. / 이동 실행기가 종료되어도 해제를 끝낸다. */
            public void run() {
                boolean interrupted = false;
                // 파서의 대기 스크립트 목록과 해제 작업이 동시에 접근하지 않도록 소유권 반환을 기다린다.
                for (;;) {
                    try {
                        parsed.await();
                        break;
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                try {
                    scripts.dispose();
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
    }

    /** Returns the captured user agent. / 복사한 User-Agent를 반환한다.
     * @return user agent / User-Agent */
    public String getUserAgent() {
        return context.userAgent;
    }

    /** Returns the browsing context of this page: the frame that shows it, or the top-level context. / 이 페이지의 브라우징 문맥을 반환한다: 이 페이지를 보여 주는 프레임, 또는 최상위 문맥.
     * @return the context / 문맥 */
    public org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    /** Returns this host's cancellable script source. / 이 호스트의 취소 가능한 스크립트 공급원을 반환한다.
     * @return script source / 스크립트 공급원 */
    public ScriptSource getScriptSource() {
        return this;
    }

    /** Returns the pointer capture service of the component (its mouse pointer). / 컴포넌트의 포인터 캡처 서비스(마우스 포인터)를 반환한다.
     * @return the service / 서비스 */
    public org.duckdns.hjow.swingwebpanel.js.host.PointerService getPointerService() {
        return session.getPointers();
    }

    /** Returns the request service of XMLHttpRequest and fetch. / XMLHttpRequest 와 fetch 의 요청 서비스를 반환한다.
     * @return request service / 요청 서비스 */
    public WebRequestSource getWebRequestSource() {
        return webRequests;
    }

    /**
     * Returns the image loading service of the page: it loads through the resource cache of the presentation, which the renderer also
     * paints from, so one fetch serves the <code>load</code> event and the painting.
     * <p>
     * 페이지의 이미지 로딩 서비스를 반환한다. 렌더러도 그리는 데 쓰는 프레젠테이션의 리소스 캐시로 로드하므로 한 번의 가져오기가
     * <code>load</code> 이벤트와 그리기를 함께 맡는다.
     *
     * @return the service / 서비스
     */
    public ImageRequestSource getImageRequestSource() {
        return new CorsImageRequestSource() {
            /**
             * Loads the image and reports its natural size or its failure.
             * <p>
             * 이미지를 로드하고 자연 크기나 실패를 알린다.
             *
             * @param absoluteUrl the absolute URL / 절대 URL
             * @param callback    the receiver / 받는 쪽
             */
            public void request(String absoluteUrl, final Callback callback) {
                request(absoluteUrl, null, callback);
            }

            /**
             * Loads the image in CORS mode (or without CORS if the mode is null) and reports its natural size or its failure.
             * <p>
             * 이미지를 CORS 모드로(모드가 null 이면 CORS 없이) 로드하고 자연 크기나 실패를 알린다.
             *
             * @param absoluteUrl the absolute URL / 절대 URL
             * @param crossOrigin <code>anonymous</code>, <code>use-credentials</code> or null / <code>anonymous</code>, <code>use-credentials</code>, null
             * @param callback    the receiver / 받는 쪽
             */
            public void request(String absoluteUrl, String crossOrigin, final Callback callback) {
                // 닫힌 페이지는 결과를 받을 곳이 없으므로 요청하지 않는다.
                if (closed.get()) {
                    return;
                }
                presentation.requestImage(absoluteUrl, crossOrigin, new PageResources.ImageWaiter() {
                    /**
                     * Converts the final state of the cached image to the callback.
                     * <p>
                     * 캐시된 이미지의 최종 상태를 콜백으로 바꾼다.
                     *
                     * @param image the final state / 최종 상태
                     */
                    public void imageDone(ImageResource image) {
                        if (image.getState() == ImageResource.State.LOADED && image.getImage() != null) {
                            callback.done(image.getImage().getWidth(), image.getImage().getHeight(), null);
                        } else {
                            callback.done(0, 0, image.getMessage() == null || image.getMessage().length() == 0 ? "image failed" : image.getMessage());
                        }
                    }
                });
            }
        };
    }

    /**
     * Returns the service that fetches the resources of <code>audio</code> and <code>video</code> elements with the common request layer of the
     * component (a closed page does not fetch).
     * <p>
     * 공통 요청 계층으로 <code>audio</code>, <code>video</code> 요소의 리소스를 가져오는 서비스를 반환한다(닫힌 페이지는 가져오지 않는다).
     *
     * @return the service / 서비스
     */
    public MediaRequestSource getMediaRequestSource() {
        return new MediaRequestSource() {
            /**
             * Starts the fetch, or reports a failure at once when the page is closed.
             * <p>
             * 가져오기를 시작한다. 페이지가 닫혔으면 곧바로 실패를 알린다.
             *
             * @param absoluteUrl the absolute URL / 절대 URL
             * @param callback    the receiver / 받는 쪽
             * @return the handle / 핸들
             */
            public Request request(String absoluteUrl, Callback callback) {
                if (closed.get()) {
                    return new Request() {
                        /** Nothing runs. / 실행 중인 것이 없다. */
                        public void cancel() {
                            return;
                        }
                    };
                }
                return presentation.requestMedia(absoluteUrl, callback);
            }
        };
    }

    /**
     * Passes what a media element found out about its resource to the resource states that the renderer reads.
     * <p>
     * 미디어 요소가 자기 리소스에 대해 알아낸 것을 렌더러가 읽는 리소스 상태에 전달한다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param error       null if it can be played, else the reason / 재생할 수 있으면 null, 아니면 이유
     */
    public void mediaStateChanged(String absoluteUrl, String error) {
        if (!closed.get()) {
            presentation.mediaState(absoluteUrl, error);
        }
    }

    /**
     * Returns the state and pixels of an image that the page already requested, for <code>drawImage()</code> of a canvas.
     * <p>
     * 페이지가 이미 요청한 이미지의 상태와 화소를 캔버스의 <code>drawImage()</code> 를 위해 반환한다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @return the image or null if it was never requested / 이미지. 요청한 적이 없으면 null
     */
    public org.duckdns.hjow.swingwebpanel.render.ImageResource getLoadedImage(String absoluteUrl) {
        return closed.get() ? null : presentation.peekImage(absoluteUrl);
    }

    /**
     * Returns the state of an image that an element with a <code>crossorigin</code> attribute requested in CORS mode (see
     * {@link #getLoadedImage(String)}).
     * <p>
     * <code>crossorigin</code> 속성이 있는 요소가 CORS 모드로 요청한 이미지의 상태를 반환한다({@link #getLoadedImage(String)} 참고).
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param cors        the CORS mode or null / CORS 모드. 없으면 null
     * @return the image or null if it was never requested / 이미지. 요청한 적이 없으면 null
     */
    public org.duckdns.hjow.swingwebpanel.render.ImageResource getLoadedImage(String absoluteUrl, String cors) {
        return closed.get() ? null : presentation.peekImage(absoluteUrl, cors);
    }

    /**
     * Passes a change of a canvas picture to the view: the display list is rebuilt from the retained layout and painted again.
     * <p>
     * 캔버스 그림의 변경을 뷰에 전달한다: 보관한 배치로 디스플레이 리스트를 다시 만들어 다시 그린다.
     *
     * @param canvas the canvas element / 캔버스 요소
     */
    public void canvasChanged(Element canvas) {
        if (!closed.get()) {
            presentation.canvasChanged();
        }
    }

    /**
     * Passes a change of what an audio control bar shows (a new position, play or pause, the volume) to the view: the display list is rebuilt
     * from the retained layout and painted again, without a new layout.
     * <p>
     * 오디오 컨트롤 막대가 보여 주는 것(새 위치, 재생이나 일시정지, 음량)의 변경을 뷰에 전달한다: 새 배치 없이 보관한 배치로 디스플레이 리스트를
     * 다시 만들어 다시 그린다.
     *
     * @param media the <code>audio</code> element / <code>audio</code> 요소
     */
    public void mediaControlsChanged(Element media) {
        if (!closed.get()) {
            presentation.canvasChanged();
        }
    }

    /**
     * Returns the factory of the sound outputs that the component chose for its media elements (null if the view has none).
     * <p>
     * 컴포넌트가 미디어 요소를 위해 고른 소리 출력의 팩터리를 반환한다(뷰에 없으면 null).
     *
     * @return the factory or null / 팩터리 또는 null
     */
    public AudioOutputFactory getAudioOutputFactory() {
        return view.getAudioOutputFactory();
    }

    /**
     * Returns the factory of the outputs that the component chose for the real-time Web Audio contexts of its pages (null for the system output).
     * <p>
     * 컴포넌트가 페이지의 실시간 Web Audio 컨텍스트를 위해 고른 출력의 팩터리를 반환한다(시스템 출력이면 null).
     *
     * @return the factory or null / 팩터리 또는 null
     */
    @Override
    public OutputFactory getWebAudioOutputFactory() {
        return view.getWebAudioOutputFactory();
    }

    /** Returns the document scheduler. / 문서 스케줄러를 반환한다.
     * @return scheduler / 스케줄러 */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /** Returns the component cookie store. / 컴포넌트 쿠키 저장소를 반환한다.
     * @return cookie store / 쿠키 저장소 */
    public CookieStore getCookies() {
        return cookies;
    }

    /** Returns process storage with origin filtering in the bridge. / 연결 계층에서 오리진을 거르는 프로세스 저장소를 반환한다.
     * @return shared manager / 공용 관리자 */
    public LocalStorageManager getLocalStorage() {
        return session.getLocalStorageManager();
    }

    /** Returns component storage with origin filtering in the bridge. / 연결 계층에서 오리진을 거르는 컴포넌트 저장소를 반환한다.
     * @return session storage / 세션 저장소 */
    public SessionStorage getSessionStorage() {
        return session.getSessionStorageStore();
    }

    /**
     * Returns the cluster token for this component's documents and workers.
     * <p>
     * 이 컴포넌트의 문서와 Worker에 부여된 클러스터 토큰을 반환한다.
     *
     * @return stable cluster token / 고정 클러스터 토큰
     */
    @Override
    public Object getSharedMemoryCluster() {
        return session.sharedMemoryCluster();
    }

    /**
     * Returns the isolation capability computed from this document and its container.
     * <p>
     * 이 문서와 컨테이너의 정책으로 정한 격리 권한을 반환한다.
     *
     * @return whether the document is isolated / 문서가 격리되었는지 여부
     */
    public boolean isCrossOriginIsolated() {
        return crossOriginIsolated;
    }

    /**
     * Returns the policies retained from the page response.
     * <p>
     * 페이지 응답에서 보관한 정책을 반환한다.
     *
     * @return the document policies / 문서 정책
     */
    CrossOriginPolicy crossOriginPolicy() {
        return context.crossOriginPolicy;
    }

    /**
     * Returns the policy used for resources and worker response checks.
     * <p>
     * 리소스와 워커 응답 검사에 쓰는 정책을 반환한다.
     *
     * @return the document's embedder policy / 문서의 삽입 정책
     */
    public CrossOriginPolicy.Embedder getEmbedderPolicy() {
        return crossOriginPolicy().getEmbedder();
    }

    /**
     * Tells the script host whether the page still has resources (stylesheets, images) that it started and that the window
     * <code>load</code> event waits for.
     * <p>
     * 페이지가 시작했고 window <code>load</code> 이벤트가 기다려야 하는 리소스(스타일시트, 이미지)가 아직 있는지 스크립트 호스트에 알려 준다.
     *
     * @return true while one is loading / 로딩 중인 것이 있으면 true
     */
    public boolean hasPendingResources() {
        return presentation.hasPendingResources();
    }

    /**
     * Lets the view finish the progress of the load once the window <code>load</code> event fired.
     * <p>
     * window <code>load</code> 이벤트가 발생하면 뷰가 로딩 진행률을 마무리하게 한다.
     */
    public void pageLoaded() {
        presentation.loadChanged();
        // 프레임의 문서이면 iframe 요소의 load 이벤트를 보낼 수 있는지 부모의 문서 큐에서 다시 판단한다.
        if (frame != null) {
            frame.scriptsLoaded();
        }
    }

    /**
     * Tells whether the scripts of this page finished loading or can no longer run (disabled or closed), so that the progress of the load
     * does not wait for them.
     * <p>
     * 이 페이지의 스크립트 로딩이 끝났거나 더 실행될 수 없는지(비활성화되었거나 닫힘) 알려 준다. 그러면 로딩 진행률은 스크립트를 기다리지 않는다.
     *
     * @return true if nothing script-related is awaited / 스크립트와 관련해 기다리는 것이 없으면 true
     */
    boolean isLoadComplete() {
        return scripts.isLoadComplete() || !isScriptingEnabled();
    }

    /** Returns whether this page may still execute. / 이 페이지가 아직 실행 가능한지 반환한다.
     * @return true while active and enabled / 활성·허용 상태이면 true */
    public boolean isScriptingEnabled() {
        return !closed.get() && session.isJavascriptEnabled() && !session.isDisposed();
    }

    /** Returns this document's object URL scope. / 이 문서의 객체 URL 범위를 반환한다. */
    public BlobUrlStore.Scope getBlobUrlScope() {
        return blobUrls;
    }

    /**
     * Runs a clipboard operation on the EDT and returns its result to this document's queue.
     * <p>
     * EDT에서 클립보드 작업을 실행하고 이 문서의 큐에 결과를 돌려준다.
     *
     * @param write text to write or null to read / 쓸 텍스트. 읽으려면 null
     * @param result receiver in this document / 이 문서에서 결과를 받는 곳
     */
    public void requestClipboardText(final String write, final ClipboardResult result) {
        SwingUtilities.invokeLater(new Runnable() {
            /** Runs the platform access while this document remains active. / 문서가 활성일 때 플랫폼에 접근한다. */
            public void run() {
                if (!isScriptingEnabled()) {
                    return;
                }
                String text = "";
                boolean failed = false;
                try {
                    text = session.clipboardText(write);
                } catch (Exception e) {
                    failed = true;
                } catch (java.awt.AWTError e) {
                    failed = true;
                }
                completeClipboardText(result, text, failed);
            }
        });
    }

    /**
     * Schedules clipboard completion so promise reactions stay on the original document thread.
     * <p>
     * 프라미스 반응이 원래 문서 스레드에서 실행되도록 클립보드 완료를 예약한다.
     *
     * @param result receiver / 수신자
     * @param text read text or empty / 읽은 텍스트 또는 빈 문자열
     * @param failed whether clipboard access failed / 클립보드 접근 실패 여부
     */
    private void completeClipboardText(final ClipboardResult result, final String text, final boolean failed) {
        try {
            scheduler.post(new Runnable() {
                /** Settles the request in its original realm. / 원래 렐름에서 요청을 확정한다. */
                public void run() {
                    if (isScriptingEnabled()) {
                        scripts.runTask(new Runnable() {
                            /** Reports the platform result. / 플랫폼 결과를 알린다. */
                            public void run() {
                                result.complete(text, failed);
                            }
                        });
                    }
                }
            }, 0);
        } catch (RejectedExecutionException e) {
            // 문서가 교체되거나 폐기되어 큐가 닫혔으면 결과를 전달하지 않는다.
        }
    }

    /**
     * Fetches external classic/module code through the same headers, cookies, origin and SSL policy as other resources.
     * <p>다른 리소스와 같은 헤더·쿠키·오리진·SSL 정책으로 외부 일반·모듈 코드를 가져온다.
     * @param url absolute script URL / 절대 스크립트 URL
     * @return decoded script source / 디코딩한 스크립트 원문
     * @throws IOException on cancellation, policy failure or response error / 취소·정책 실패·응답 오류 시
     */
    public String fetch(String url) throws IOException {
        return fetchScript(url).getText();
    }

    /**
     * Fetches a classic script with the policy of the final response.
     * <p>
     * 일반 스크립트와 최종 응답의 정책을 가져온다.
     *
     * @param url absolute script URL / 절대 스크립트 URL
     * @return the decoded script with its response settings / 디코딩한 스크립트와 응답 설정
     * @throws IOException on request failure / 요청 실패 시
     */
    public ScriptText fetchScript(String url) throws IOException {
        return fetchScript(url, getEmbedderPolicy());
    }

    /**
     * Fetches a classic script using the supplied embedder policy.
     * <p>
     * 지정한 삽입 정책으로 일반 스크립트를 가져온다.
     *
     * @param url absolute script URL / 절대 스크립트 URL
     * @param policy the requester's embedder policy / 요청자의 삽입 정책
     * @return the decoded script and response metadata / 디코딩한 스크립트와 응답 메타데이터
     * @throws IOException on request failure / 요청 실패 시
     */
    public ScriptText fetchScript(String url, CrossOriginPolicy.Embedder policy) throws IOException {
        resources.throwIfCancelled();
        ResourceRequest request = new ResourceRequest(url).setUserAgent(context.userAgent).setExtraHeaders(context.headers())
                .setCookies(cookies).setCancelToken(resources).setReferrer(pageUrl).setInitiator(Origin.of(pageUrl))
                .setBlobUrlScope(blobUrls)
                .setEmbedderPolicy(policy)
                .setPurpose(RequestPurpose.SCRIPT)
                .setCacheMode(context.force ? ResourceRequest.CacheMode.FORCE_RELOAD : ResourceRequest.CacheMode.DEFAULT);
        ResourceResponse response = fetcher.fetch(request);
        resources.throwIfCancelled();
        if (response.isError()) {
            throw new IOException(response.getStatus() + " " + response.getFailureMessage());
        }
        String text = decode(response.getBody(), response.getCharset(), scripts.getDocument().getCharacterSet());
        String finalUrl = response.getFinalUrl() == null ? url : response.getFinalUrl();
        return new ScriptText(text, finalUrl, CrossOriginPolicy.from(response));
    }

    /**
     * Fetches a module script (or a module that a module imports) with the browser rules of modules: CORS mode, cookies for the page's own
     * origin only unless credentials were requested, a JavaScript MIME type and the final URL after redirects.
     * <p>모듈 스크립트(또는 모듈이 import 하는 모듈)를 모듈의 브라우저 규칙으로 가져온다: CORS 모드, 자격 증명을 요청하지 않으면 페이지 자신의
     * 오리진에만 쿠키 전송, JavaScript MIME 타입, 리디렉션 뒤의 최종 URL.
     * @param url absolute module URL / 절대 모듈 URL
     * @param useCredentials true to send cookies to every origin / 모든 오리진에 쿠키를 보내려면 true
     * @return text and final URL / 텍스트와 최종 URL
     * @throws IOException on cancellation, policy failure, CORS refusal, wrong MIME type or response error / 취소, 정책 실패, CORS 거부, 잘못된 MIME 타입, 응답 오류 시
     */
    public ScriptText fetchModule(String url, boolean useCredentials) throws IOException {
        return fetchModule(new ModuleRequest(url), useCredentials ? Credentials.INCLUDE : Credentials.SAME_ORIGIN, false);
    }

    /**
     * Fetches a module with CORS and the MIME type and decoding rules for its module type.
     * <p>
     * CORS와 모듈 타입에 해당하는 MIME 타입 및 디코딩 규칙을 적용해 모듈을 가져온다.
     *
     * @param module the resolved module request / 해석된 모듈 요청
     * @param credentials credentials mode / 자격 증명 모드
     * @param workerEntry true for a same-origin worker entry / 같은 오리진의 worker 진입 모듈이면 true
     * @return UTF-8 text and final URL / UTF-8 텍스트와 최종 URL
     * @throws IOException if the module cannot be loaded / 모듈을 불러올 수 없으면
     */
    public ScriptText fetchModule(ModuleRequest module, Credentials credentials, boolean workerEntry) throws IOException {
        resources.throwIfCancelled();
        String url = module.getSpecifier();
        Origin initiator = Origin.of(pageUrl);
        // 자격 증명 모드를 리디렉션마다 적용하도록 요청에 넘긴다.
        ResourceRequest request = new ResourceRequest(url).setUserAgent(context.userAgent).setExtraHeaders(context.headers())
                .setCookies(cookies).setCancelToken(resources).setReferrer(pageUrl).setInitiator(initiator)
                .setBlobUrlScope(blobUrls)
                .setPurpose(workerEntry ? RequestPurpose.WORKER_MODULE : RequestPurpose.MODULE)
                .setSendCredentials(credentials != Credentials.OMIT).setSameOriginCredentialsOnly(credentials == Credentials.SAME_ORIGIN)
                .setCacheMode(context.force ? ResourceRequest.CacheMode.FORCE_RELOAD : ResourceRequest.CacheMode.DEFAULT);
        ResourceResponse response = fetcher.fetch(request);
        resources.throwIfCancelled();
        if (response.isError()) {
            throw new IOException(response.getStatus() + " " + response.getFailureMessage());
        }
        String finalUrl = response.getFinalUrl() == null ? url : response.getFinalUrl();
        // 다른 오리진의 모듈은 서버가 CORS 로 허용해야 하고, 쿠키를 보냈다면 자격 증명까지 허용해야 한다.
        Origin target = Origin.of(finalUrl);
        boolean ownBlob = response.getBlobUrlEntry() != null;
        if (workerEntry && !ownBlob && !initiator.isSameOrigin(target)) {
            throw new IOException("worker module entry must be same-origin: " + finalUrl);
        }
        if (!ownBlob && !OriginPolicy.mayUse(initiator, target, RequestPurpose.MODULE, response, credentials == Credentials.INCLUDE)) {
            throw new IOException("blocked by CORS policy: " + finalUrl);
        }
        String mime = response.getMimeType();
        boolean json = module.isJson();
        // 지정한 모듈 타입과 서버가 제공한 MIME 타입이 일치해야 한다.
        if (json ? !ContentType.parse(mime).isJson() : !PageScripts.isJavaScriptMimeType(mime)) {
            throw new IOException("expected a " + (json ? "JSON" : "JavaScript") + " module but the server responded with a MIME type of '" + mime + "'");
        }
        String text = new String(response.getBody(), "UTF-8");
        if (text.length() > 0 && text.charAt(0) == (char) 0xfeff) {
            text = text.substring(1);
        }
        return new ScriptText(text, finalUrl, CrossOriginPolicy.from(response));
    }

    /**
     * Tells whether this JRE can decode a charset name.
     * <p>이 JRE가 문자셋 이름을 디코딩할 수 있는지 알려 준다.
     * @param name charset name, possibly null / 문자셋 이름이며 null 가능
     * @return true if usable / 쓸 수 있으면 true
     */
    private static boolean supported(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        try {
            return Charset.isSupported(name);
        } catch (IllegalArgumentException e) {
            // 잘못된 문자셋 이름은 쓸 수 없는 것으로 처리한다.
            return false;
        }
    }

    /**
     * Decodes script bytes using BOM, HTTP charset, the encoding of the document that includes the script, then UTF-8 (the order of the
     * HTML Standard for a script without a <code>charset</code> attribute); HTML meta tags inside the script never matter.
     * <p>BOM·HTTP 문자셋·스크립트를 포함한 문서의 인코딩·UTF-8 순서로 스크립트를 해독한다(<code>charset</code> 속성이 없는 스크립트에 대한 HTML 표준의
     * 순서). 스크립트 안의 HTML meta 태그는 문자셋에 영향을 주지 않는다.
     * @param bytes response bytes / 응답 바이트
     * @param charset HTTP charset, possibly null / HTTP 문자셋이며 null 가능
     * @param documentCharset encoding of the including document, possibly null / 포함한 문서의 인코딩이며 null 가능
     * @return script source / 스크립트 원문
     * @throws IOException if the required UTF charset is unavailable / 필수 UTF 문자셋을 사용할 수 없으면
     */
    private static String decode(byte[] bytes, String charset, String documentCharset) throws IOException {
        int offset = 0;
        String encoding = "UTF-8";
        // HTTP 문자셋이 없거나 쓸 수 없으면 포함한 문서의 인코딩을, 그것도 없으면 UTF-8 을 쓴다.
        if (supported(charset)) {
            encoding = charset;
        } else if (supported(documentCharset)) {
            encoding = documentCharset;
        }
        // 명시한 HTTP 문자셋보다 바이트 순서 표시가 우선한다.
        if (bytes.length >= 3 && (bytes[0] & 255) == 239 && (bytes[1] & 255) == 187 && (bytes[2] & 255) == 191) {
            encoding = "UTF-8";
            offset = 3;
        } else if (bytes.length >= 2 && (bytes[0] & 255) == 254 && (bytes[1] & 255) == 255) {
            encoding = "UTF-16BE";
            offset = 2;
        } else if (bytes.length >= 2 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 254) {
            encoding = "UTF-16LE";
            offset = 2;
        }
        return new String(bytes, offset, bytes.length - offset, encoding);
    }
}
