package org.duckdns.hjow.swingwebpanel.core;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.content.ContentClassifier;
import org.duckdns.hjow.swingwebpanel.content.ContentKind;
import org.duckdns.hjow.swingwebpanel.content.ContentType;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.HostMessageValues;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.CancelledException;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager;
import org.duckdns.hjow.swingwebpanel.storage.SessionStorage;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;

/**
 * All state of one browser component: user agent, extra headers, JavaScript flag, the confirmed page, history, status text and
 * progress. It is the heart of the behavior described in <code>docs/API_CONTRACT.md</code>; {@code SwingWebPanel} is only a thin
 * Swing wrapper around it. Every method may be called from any thread. Navigation is asynchronous: a request number is
 * taken, a {@link PageLoader} runs on the component's document thread, and the result is committed only if its number is
 * still the current one. The confirmed page (URL, source, history position) is replaced in one step, so readers never see a
 * half-updated state. Observers are told on the event dispatch thread.
 * <p>
 * 브라우저 컴포넌트 하나의 모든 상태: User-Agent, 추가 헤더, JavaScript 설정, 확정된 페이지, 이력, 상태 메시지, 진행률.
 * <code>docs/API_CONTRACT.md</code> 에 적힌 동작의 핵심이며 {@code SwingWebPanel} 은 이것을 감싼 얇은 Swing 껍데기이다.
 * 모든 메서드는 어느 스레드에서나 호출할 수 있다. 이동은 비동기이다: 요청 번호를 받고, {@link PageLoader} 가 컴포넌트의 문서 스레드에서
 * 실행되며, 결과의 번호가 여전히 현재일 때만 확정한다. 확정된 페이지(URL, 원문, 이력 위치)는 한 번에 교체되므로 읽는 쪽이 반쯤 바뀐
 * 상태를 볼 수 없다. 관찰자에게는 이벤트 디스패치 스레드에서 알린다.
 */
public final class BrowserSession implements FormSubmitSink {

    /**
     * Default user agent used when none was set (API contract 5.1).
     * <p>
     * 지정하지 않았을 때 쓰는 기본 User-Agent(API 계약 5.1).
     */
    public static final String DEFAULT_USER_AGENT = "swingwebpanel/1.0";

    /**
     * Guards the confirmed page, the history and the navigation start.
     * <p>
     * 확정된 페이지, 이력, 이동 시작을 보호하는 잠금.
     */
    private final Object lock = new Object();

    /** Shared-memory cluster token for this component. / 이 컴포넌트의 공유 메모리 클러스터 토큰. */
    private final Object sharedMemoryCluster = new Object();

    /**
     * Strategy that loads pages.
     * <p>
     * 페이지를 읽는 전략.
     */
    private final PageLoader loader;
    /** The component's registered object URLs when standard services are attached. / 표준 서비스가 연결된 컴포넌트의 등록 객체 URL. */
    private volatile BlobUrlStore blobUrls;

    /**
     * Shares the resource loader's object URL store with navigation.
     * <p>
     * 리소스 로더의 객체 URL 저장소를 이동에 연결한다.
     *
     * @param store the store for this component / 이 컴포넌트의 저장소
     */
    void bindBlobUrls(BlobUrlStore store) {
        blobUrls = store;
    }

    /**
     * Request numbering and the document thread.
     * <p>
     * 요청 번호 발급과 문서 스레드.
     */
    private final NavigationController nav = new NavigationController();

    /**
     * Observer of state changes, or null.
     * <p>
     * 상태 변경 관찰자. 없으면 null.
     */
    private volatile SessionListener listener;

    /**
     * The dispatcher that receives the console output of the pages of this session, or null when nobody listens (a session without a
     * component); then the pages keep the default console, which writes to standard output.
     * <p>
     * 이 세션의 페이지가 쓰는 콘솔 출력을 받는 통지기. 듣는 쪽이 없으면(컴포넌트 없는 세션) null 이며, 그러면 페이지는 표준 출력에 쓰는
     * 기본 콘솔을 그대로 둔다.
     */
    private volatile ConsoleDispatcher consoles;

    /** Dispatcher shared by documents of the component. / 컴포넌트의 문서들이 공유하는 메시지 통지기. */
    private volatile WebMessageDispatcher webMessages;

    /**
     * Installs the component's dispatcher for its subsequent documents.
     * <p>
     * 이후 문서에 적용할 컴포넌트 통지기를 설치한다.
     *
     * @param dispatcher component message dispatcher / 컴포넌트 메시지 통지기
     */
    public void setWebMessageDispatcher(WebMessageDispatcher dispatcher) {
        webMessages = dispatcher;
    }

    /** Returns the dispatcher for installed documents. / 설치한 문서의 메시지 통지기를 반환한다. */
    WebMessageDispatcher webMessageDispatcher() {
        return webMessages;
    }

    /**
     * Sets the dispatcher that receives the console output of the pages of this session; pages created later use it.
     * <p>
     * 이 세션의 페이지가 쓰는 콘솔 출력을 받는 통지기를 지정한다. 그 뒤에 만들어지는 페이지가 사용한다.
     *
     * @param dispatcher the dispatcher or null / 통지기 또는 null
     */
    public void setConsoleDispatcher(ConsoleDispatcher dispatcher) {
        this.consoles = dispatcher;
    }

    /**
     * Returns the dispatcher of the console output, or null.
     * <p>
     * 콘솔 출력의 통지기를 반환한다. 없으면 null.
     *
     * @return the dispatcher or null / 통지기 또는 null
     */
    ConsoleDispatcher consoleDispatcher() {
        return consoles;
    }

    /**
     * Cookies of this component; they survive <code>clean()</code> and are forgotten by <code>dispose()</code>.
     * <p>
     * 이 컴포넌트의 쿠키. <code>clean()</code> 후에도 유지되고 <code>dispose()</code> 로 잊는다.
     */
    private final CookieStore cookies;

    /**
     * The sessionStorage of this component (one area per origin, in memory).
     * <p>
     * 이 컴포넌트의 sessionStorage(오리진마다 영역 하나, 메모리).
     */
    private final SessionStorage sessionStorage = new SessionStorage();

    /**
     * The state of the mouse pointer (buttons, active document, pointer capture), shared by the input handler and the scripts of the pages.
     * <p>
     * 마우스 포인터의 상태(버튼, 활성 문서, 포인터 캡처). 입력 처리기와 페이지의 스크립트가 공유한다.
     */
    private final PagePointers pointers = new PagePointers();

    /**
     * Whether the documents count as visible to the user (the component is on the screen); it starts true and follows the showing state of the
     * component after its first change.
     * <p>
     * 문서를 사용자에게 보이는 것으로 치는지(컴포넌트가 화면에 있는지). true 로 시작하며 컴포넌트의 보임 상태가 처음 바뀐 뒤부터 그것을 따른다.
     */
    private volatile boolean documentVisible = true;

    /**
     * Whether the host says that it has a network connection (<code>navigator.onLine</code>); it starts true.
     * <p>
     * 호스트가 네트워크 연결이 있다고 하는지(<code>navigator.onLine</code>). true 로 시작한다.
     */
    private volatile boolean online = true;

    /**
     * The process-wide localStorage manager.
     * <p>
     * 프로세스 공용 localStorage 관리자.
     */
    private final LocalStorageManager localStorage;

    /**
     * Receives navigations that must be downloaded, or null.
     * <p>
     * 다운로드해야 하는 이동을 받는 핸들러. 없으면 null.
     */
    private volatile DownloadHandler downloadHandler;

    /** System clipboard writer installed by the component. / 컴포넌트가 설치한 시스템 클립보드 쓰기. */
    private volatile ClipboardSink clipboardSink;
    /** System clipboard reader installed by the component. / 컴포넌트가 설치한 시스템 클립보드 읽기. */
    private volatile ClipboardSource clipboardSource;

    /**
     * Extra request headers (user agent excluded).
     * <p>
     * 추가 요청 헤더(User-Agent 제외).
     */
    private final RequestHeaders headers = new RequestHeaders();

    /**
     * The history; its last confirmed position is the current page.
     * <p>
     * 이력. 확정된 현재 위치가 현재 페이지이다.
     */
    private final History history = new History(PageSnapshot.blank());

    /**
     * The confirmed page.
     * <p>
     * 확정된 페이지.
     */
    private volatile PageSnapshot current = PageSnapshot.blank();

    /** The top-level browsing context of this component across document changes. / 문서가 바뀌어도 유지되는 컴포넌트 최상위 브라우징 문맥. */
    private final TopContext topContext = new TopContext(this);

    /**
     * User agent sent with requests.
     * <p>
     * 요청에 실어 보내는 User-Agent.
     */
    private volatile String userAgent = DEFAULT_USER_AGENT;

    /**
     * JavaScript flag.
     * <p>
     * JavaScript 설정.
     */
    private volatile boolean javascriptEnabled = true;

    /**
     * The request number of the navigation that produced the confirmed page. While it is still the current request number nothing else is
     * loading, so a navigation inside the page keeps the page's own request (and so the page's scripts) alive. Guarded by the lock.
     * <p>
     * 확정된 페이지를 만든 이동의 요청 번호. 이것이 아직 현재 요청 번호이면 다른 읽기가 진행 중이지 않으므로, 페이지 안의 이동은 페이지 자신의
     * 요청(과 그 스크립트)을 살려 둔다. 잠금으로 보호한다.
     */
    private volatile int confirmedRequestId = -1;

    /** Current and still-parsing script hosts, guarded by lock. / lock으로 보호하는 현재 및 파싱 중 스크립트 호스트. */
    private final Set<PageRuntime> runtimes = new HashSet<PageRuntime>();

    /** Most recently installed top-level script runtime, guarded by lock. / 최근 설치한 최상위 스크립트 환경. lock으로 보호한다. */
    private PageRuntime installedMessageRuntime;

    /** Navigation id of the installed message runtime. / 설치한 메시지 환경의 이동 번호. */
    private int installedMessageRequest;

    /**
     * Status bar text.
     * <p>
     * 상태바 메시지.
     */
    private volatile String statusText = "";

    /** Optional hover overlay, leaving the loading status intact. / 로딩 상태를 보존하는 선택적 링크 호버 표시. */
    private volatile String hoverText;

    /** Counts requested navigations, even before their pages commit. / 페이지 확정 전까지 포함한 이동 요청 횟수. */
    private volatile long navigationCount;

    /**
     * Progress: 0 to 100 or -1 when indeterminate.
     * <p>
     * 진행률: 0~100, 불확정이면 -1.
     */
    private volatile int progress = 100;

    /**
     * The progress at which a confirmed page starts to load its resources: the body (0 to 50) was received and parsed.
     * <p>
     * 확정된 페이지가 리소스를 로딩하기 시작하는 진행률: 본문(0~50)을 받아 해석했다.
     */
    private static final int LOAD_START_PERCENT = 50;

    /**
     * The highest progress while resources are still loading; the progress reaches 100 only when everything ended.
     * <p>
     * 리소스를 아직 로딩하는 동안의 최고 진행률. 모두 끝나야만 100 에 이른다.
     */
    private static final int LOAD_END_PERCENT = 94;

    /**
     * True once a {@link PageView} reports the loading of confirmed pages to this session; without a view nobody could finish the progress
     * of a page after confirmation, so the progress ends at confirmation.
     * <p>
     * {@link PageView} 가 확정된 페이지의 로딩을 이 세션에 보고하면 true. 뷰가 없으면 확정 뒤에 페이지의 진행률을 마무리할 곳이 없으므로
     * 진행률은 확정 때 끝난다.
     */
    private volatile boolean viewAttached;

    /**
     * Asked before a form is sent again, or null.
     * <p>
     * 폼을 다시 보내기 전에 묻는 확인기. 없으면 null.
     */
    private volatile ResubmitConfirmer resubmitConfirmer;

    /**
     * Guards the progress of the loading after a page was confirmed ({@link #loadTracked}, {@link #loadDone}, {@link #loadPercent}).
     * <p>
     * 페이지가 확정된 뒤의 로딩 진행률({@link #loadTracked}, {@link #loadDone}, {@link #loadPercent})을 보호한다.
     */
    private final Object loadLock = new Object();

    /**
     * The document whose loading progress is being reported (compared by identity), or null.
     * <p>
     * 로딩 진행률을 보고 중인 문서(동일성으로 비교). 없으면 null.
     */
    private Object loadTracked;

    /**
     * The document whose loading ended; later reports for it are ignored, so a script that starts an image after the load event does not
     * bring the progress back.
     * <p>
     * 로딩이 끝난 문서. 이 문서에 대한 이후의 보고는 무시하므로, load 이벤트 뒤에 스크립트가 이미지를 시작해도 진행률이 되돌아가지 않는다.
     */
    private Object loadDone;

    /**
     * The progress of the document being tracked; it never decreases while the page loads.
     * <p>
     * 추적 중인 문서의 진행률. 페이지를 로딩하는 동안 줄어들지 않는다.
     */
    private int loadPercent;

    /**
     * Set once the session was disposed.
     * <p>
     * 세션이 폐기되면 true.
     */
    private volatile boolean disposed;

    /**
     * Progress sink handed to loaders.
     * <p>
     * 로더에 건네는 진행률 대상.
     */
    private final ProgressSink sink = new ProgressSink() {
        /**
         * Applies a report if its request is still current.
         * <p>
         * 요청이 여전히 현재이면 보고를 반영한다.
         *
         * @param requestId  request number / 요청 번호
         * @param statusText status text / 상태 메시지
         * @param progress   progress / 진행률
         */
        public void report(int requestId, String statusText, int progress) {
            if (nav.isCurrent(requestId) && !disposed) {
                updateStatus(statusText, progress);
            }
        }
    };

    /**
     * Creates a session with its own cookie store.
     * <p>
     * 자기만의 쿠키 저장소를 가진 세션을 만든다.
     *
     * @param loader   strategy that loads pages, not null / 페이지를 읽는 전략(null 불가)
     * @param listener observer or null / 관찰자 또는 null
     */
    public BrowserSession(PageLoader loader, SessionListener listener) {
        this(loader, listener, new CookieStore());
    }

    /**
     * Creates a session that uses the given cookie store (the same store that its page loader uses).
     * <p>
     * 주어진 쿠키 저장소(페이지 로더가 쓰는 것과 같은 저장소)를 쓰는 세션을 만든다.
     *
     * @param loader   strategy that loads pages, not null / 페이지를 읽는 전략(null 불가)
     * @param listener observer or null / 관찰자 또는 null
     * @param cookies  cookie store of the component, not null / 컴포넌트의 쿠키 저장소(null 불가)
     */
    public BrowserSession(PageLoader loader, SessionListener listener, CookieStore cookies) {
        this(loader, listener, cookies, LocalStorageManager.getInstance());
    }

    /**
     * Creates a session with its own cookie store and a given localStorage manager (used by tests to avoid touching the process-wide one).
     * <p>
     * 쿠키 저장소와 주어진 localStorage 관리자를 쓰는 세션을 만든다(시험이 프로세스 공용 관리자를 건드리지 않도록 쓴다).
     *
     * @param loader   strategy that loads pages, not null / 페이지를 읽는 전략(null 불가)
     * @param listener observer or null / 관찰자 또는 null
     * @param cookies  cookie store of the component, not null / 컴포넌트의 쿠키 저장소(null 불가)
     * @param local    localStorage manager, not null / localStorage 관리자(null 불가)
     */
    public BrowserSession(PageLoader loader, SessionListener listener, CookieStore cookies, LocalStorageManager local) {
        if (loader == null || cookies == null || local == null) {
            throw new IllegalArgumentException("loader, cookies and localStorage manager must not be null");
        }
        this.loader = loader;
        this.listener = listener;
        this.cookies = cookies;
        this.localStorage = local;
    }

    /**
     * Returns the sessionStorage of this component (for the script layer).
     * <p>
     * 이 컴포넌트의 sessionStorage 를 반환한다(스크립트 계층용).
     *
     * @return sessionStorage / sessionStorage
     */
    public SessionStorage getSessionStorageStore() {
        return sessionStorage;
    }

    /**
     * Returns the cluster token shared by this component's documents and workers.
     * <p>
     * 이 컴포넌트의 문서와 Worker가 공유하는 클러스터 토큰을 반환한다.
     *
     * @return the standalone identity token / 독립 식별 토큰
     */
    Object sharedMemoryCluster() {
        return sharedMemoryCluster;
    }

    /**
     * Returns the component's stable top-level context.
     * <p>
     * 컴포넌트의 고정 최상위 문맥을 반환한다.
     *
     * @return the top-level context / 최상위 문맥
     */
    TopContext topContext() {
        return topContext;
    }

    /**
     * Returns the state of the mouse pointer of this component (for the input handler and the script layer).
     * <p>
     * 이 컴포넌트의 마우스 포인터 상태를 반환한다(입력 처리기와 스크립트 계층용).
     *
     * @return the pointer state / 포인터 상태
     */
    PagePointers getPointers() {
        return pointers;
    }

    /**
     * Returns the process-wide localStorage manager (for the script layer).
     * <p>
     * 프로세스 공용 localStorage 관리자를 반환한다(스크립트 계층용).
     *
     * @return manager / 관리자
     */
    public LocalStorageManager getLocalStorageManager() {
        return localStorage;
    }

    /**
     * Returns a read-only copy of all localStorage items of all origins (API contract 5.17).
     * <p>
     * 모든 오리진의 모든 localStorage 항목의 읽기 전용 복사본을 반환한다(API 계약 5.17).
     *
     * @return entries sorted by origin and key / 오리진, 키 순으로 정렬된 항목
     */
    public List<StorageEntry> getLocalStorage() {
        return disposed ? new ArrayList<StorageEntry>() : localStorage.entries();
    }

    /**
     * Returns a read-only copy of all sessionStorage items of all origins of this component (API contract 5.17).
     * <p>
     * 이 컴포넌트의 모든 오리진의 모든 sessionStorage 항목의 읽기 전용 복사본을 반환한다(API 계약 5.17).
     *
     * @return entries sorted by origin and key / 오리진, 키 순으로 정렬된 항목
     */
    public List<StorageEntry> getSessionStorage() {
        return sessionStorage.entries();
    }

    /**
     * Returns all cookies of this component as "name=value; name=value" (API contract 5.16).
     * <p>
     * 이 컴포넌트의 모든 쿠키를 "name=value; name=value" 로 반환한다(API 계약 5.16).
     *
     * @return cookies, empty if none / 쿠키. 없으면 빈 문자열
     */
    public String getCookies() {
        return cookies.getCookies();
    }

    /**
     * Installs the handler that receives navigations which must be downloaded; null removes it.
     * <p>
     * 다운로드해야 하는 이동을 받는 핸들러를 설치한다. null 이면 제거한다.
     *
     * @param handler handler or null / 핸들러 또는 null
     */
    public void setDownloadHandler(DownloadHandler handler) {
        this.downloadHandler = handler;
    }

    /**
     * Shares the component's clipboard service with its document runtimes.
     * <p>
     * 컴포넌트의 클립보드 서비스를 문서 런타임과 공유한다.
     *
     * @param writer plain text writer / 일반 텍스트 쓰기
     * @param reader clipboard reader / 클립보드 읽기
     */
    public void setClipboard(ClipboardSink writer, ClipboardSource reader) {
        clipboardSink = writer;
        clipboardSource = reader;
    }

    /**
     * Executes a plain text clipboard operation on the Swing event thread.
     * <p>
     * Swing 이벤트 스레드에서 일반 텍스트 클립보드 작업을 실행한다.
     *
     * @param write text to write or null to read / 쓸 텍스트. 읽으면 null
     * @return the text, or an empty string after a write or when there is no text / 읽은 텍스트. 쓰기 뒤나 텍스트가 없으면 빈 문자열
     * @throws Exception when the clipboard is unavailable / 클립보드를 사용할 수 없으면
     */
    String clipboardText(String write) throws Exception {
        // 쓰기는 일반 텍스트 종류만 전달하며 읽기는 플랫폼의 문자열 형식만 요청한다.
        if (write != null) {
            ClipboardSink writer = clipboardSink;
            if (writer == null) {
                throw new IllegalStateException("Clipboard is unavailable");
            }
            writer.write(write, null);
            return "";
        }
        ClipboardSource reader = clipboardSource;
        if (reader == null) {
            throw new IllegalStateException("Clipboard is unavailable");
        }
        Transferable data = reader.read();
        if (data == null || !data.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return "";
        }
        Object value = data.getTransferData(DataFlavor.stringFlavor);
        return value instanceof String ? (String) value : "";
    }

    /**
     * Throws if the session was disposed.
     * <p>
     * 세션이 폐기되었으면 예외를 던진다.
     *
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    private void checkAlive() {
        if (disposed) {
            throw new IllegalStateException("component is disposed");
        }
    }

    /**
     * Sets the user agent used by later requests.
     * <p>
     * 이후 요청에 쓸 User-Agent 를 지정한다.
     *
     * @param agent user agent: not null, not blank, without CR/LF / User-Agent(null, 공백뿐, CR/LF 포함 불가)
     * @throws IllegalArgumentException for an invalid value / 잘못된 값일 때
     * @throws IllegalStateException    if disposed / 폐기되었을 때
     */
    public void setUserAgent(String agent) {
        checkAlive();
        if (agent == null || agent.trim().length() == 0) {
            throw new IllegalArgumentException("user agent must not be null or blank");
        }
        if (agent.indexOf('\r') >= 0 || agent.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("user agent must not contain CR or LF");
        }
        userAgent = agent;
    }

    /**
     * Returns the user agent (the last value that was set, or the default).
     * <p>
     * User-Agent 를 반환한다(마지막으로 지정한 값 또는 기본값).
     *
     * @return user agent / User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Adds extra headers with overwrite semantics (API contract 5.15).
     * <p>
     * 추가 헤더를 덮어쓰기 방식으로 더한다(API 계약 5.15).
     *
     * @param newHeaders header map / 헤더 맵
     * @throws IllegalArgumentException for null maps or invalid names/values / null 맵이나 잘못된 이름/값일 때
     * @throws IllegalStateException    if disposed / 폐기되었을 때
     */
    public void addHeaders(Map<String, String> newHeaders) {
        checkAlive();
        headers.addAll(newHeaders);
    }

    /**
     * Returns a copy of the extra headers (the user agent is not included).
     * <p>
     * 추가 헤더의 복사본을 반환한다(User-Agent 는 포함되지 않음).
     *
     * @return header copy / 헤더 복사본
     */
    public Map<String, String> getHeaders() {
        return headers.toMap();
    }

    /**
     * Sets the JavaScript flag. Disabling cancels current and parsing hosts; enabling applies only to new pages.
     * <p>
     * JavaScript 설정을 바꾼다. 끄면 현재·파싱 중 호스트를 취소하며 켜면 새 페이지부터 적용한다.
     *
     * @param enabled new value / 새 값
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    public void setJavascriptEnabled(boolean enabled) {
        List<PageRuntime> stop;
        synchronized (lock) {
            checkAlive();
            javascriptEnabled = enabled;
            stop = enabled ? null : new ArrayList<PageRuntime>(runtimes);
        }
        if (stop != null) {
            // 실행 잠금을 기다리지 않고 현재 페이지와 아직 확정되지 않은 페이지를 모두 중단한다.
            for (PageRuntime runtime : stop) {
                runtime.close();
            }
        }
    }

    /** Registers only a still-current, enabled host, including parsing pages. / 파싱 중 페이지까지 포함해 현재·허용 상태인 호스트만 등록한다.
     * @param runtime new page host / 새 페이지 호스트
     * @param requestId navigation serial / 이동 번호
     * @return whether registration succeeded / 등록 성공 여부 */
    boolean registerRuntime(PageRuntime runtime, int requestId) {
        synchronized (lock) {
            if (disposed || !javascriptEnabled || runtime.isClosed() || !nav.isCurrent(requestId)) {
                return false;
            }
            runtimes.add(runtime);
            // 프레임 설치는 Java 발신의 최상위 수신 문서를 바꾸지 않는다.
            if (runtime.getBrowsingContext() == topContext) {
                installedMessageRuntime = runtime;
                installedMessageRequest = requestId;
            }
            return true;
        }
    }

    /**
     * Captures one target before copying the caller's value and schedules the message.
     * <p>
     * 호출자의 값을 복사하기 전에 수신 문서 하나를 정하고 메시지 작업을 예약한다.
     *
     * @param message supported Java message data / 지원하는 Java 메시지 데이터
     * @param targetOrigin a wildcard or an absolute URL / 와일드카드 또는 절대 주소
     * @throws IllegalArgumentException for invalid origin or data / 잘못된 주소 또는 값이면
     * @throws IllegalStateException when disposed / 폐기되었으면
     */
    public void postMessage(Object message, String targetOrigin) {
        PageRuntime target;
        synchronized (lock) {
            checkAlive();
            target = messageRuntime();
        }
        Origin origin = "*".equals(targetOrigin) ? null : Origin.ofAbsoluteMessageUrl(targetOrigin);
        ClonedValue copied = HostMessageValues.fromJava(message);
        // 복사 중 이동이 일어나도 잡아 둔 문서의 큐에만 보낸다.
        if (target != null) {
            target.postHostMessage(copied, origin);
        }
    }

    /**
     * Selects the installed top-level runtime, falling back to the confirmed document.
     * The caller holds the session lock.
     * <p>
     * 설치된 최상위 실행 환경을 택하며, 없으면 확정 문서를 확인한다. 호출자가 세션 잠금을 쥔다.
     *
     * @return one target, or null when scripts cannot run / 수신 환경 하나. 스크립트를 실행할 수 없으면 null
     */
    private PageRuntime messageRuntime() {
        if (!javascriptEnabled) {
            return null;
        }
        if (installedMessageRuntime != null && nav.isCurrent(installedMessageRequest) && !installedMessageRuntime.isClosed()) {
            return installedMessageRuntime;
        }
        PageDocument page = current.getDocument();
        return page instanceof HtmlPageDocument ? ((HtmlPageDocument) page).runtime() : null;
    }

    /**
     * Reports a change of the viewport (the size of the visible area or the scroll position) to the pages with scripts: they get the
     * <code>resize</code> and <code>scroll</code> events and their media query lists are evaluated again. It may be called on any thread; the
     * events run on the document queue.
     * <p>
     * 뷰포트(보이는 영역의 크기나 스크롤 위치)의 변화를 스크립트가 있는 페이지에 알린다: 페이지는 <code>resize</code>, <code>scroll</code> 이벤트를
     * 받고 미디어 질의 목록이 다시 평가된다. 어느 스레드에서나 부를 수 있고 이벤트는 문서 큐에서 실행된다.
     *
     * @param resized  true if the viewport size changed / 뷰포트 크기가 바뀌었으면 true
     * @param scrolled true if the scroll position changed / 스크롤 위치가 바뀌었으면 true
     */
    public void viewportChanged(boolean resized, boolean scrolled) {
        List<PageRuntime> live;
        synchronized (lock) {
            if (disposed || runtimes.isEmpty()) {
                return;
            }
            live = new ArrayList<PageRuntime>(runtimes);
        }
        // 등록된 페이지마다 문서 큐에 이벤트 전달을 맡긴다.
        for (PageRuntime runtime : live) {
            runtime.viewportChanged(resized, scrolled);
        }
    }

    /**
     * Tells whether the documents count as visible to the user.
     * <p>
     * 문서를 사용자에게 보이는 것으로 치는지 알려 준다.
     *
     * @return true while visible / 보이는 동안 true
     */
    boolean isDocumentVisible() {
        return documentVisible;
    }

    /**
     * Tells whether the host says that it is online.
     * <p>
     * 호스트가 연결되어 있다고 하는지 알려 준다.
     *
     * @return true while online / 연결된 동안 true
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Tells the pages whether the component is on the screen (Page Visibility: <code>document.visibilityState</code> becomes <code>visible</code> or
     * <code>hidden</code> and <code>visibilitychange</code> fires at the document). A call that does not change the state does nothing. It may be called on any
     * thread; the events run on the document queue.
     * <p>
     * 컴포넌트가 화면에 있는지 페이지에 알린다(Page Visibility: <code>document.visibilityState</code> 가 <code>visible</code> 또는 <code>hidden</code> 이 되고 문서에
     * <code>visibilitychange</code> 가 발생한다). 상태가 바뀌지 않는 호출은 아무것도 하지 않는다. 어느 스레드에서나 부를 수 있고 이벤트는 문서 큐에서 실행된다.
     *
     * @param visible true if the component is shown / 컴포넌트가 보이면 true
     */
    public void setDocumentVisible(boolean visible) {
        if (documentVisible == visible) {
            return;
        }
        documentVisible = visible;
        // 살아 있는 페이지마다 가시성이 바뀌었음을 알린다.
        for (PageRuntime runtime : liveRuntimes()) {
            runtime.lifecycleChanged(0);
        }
    }

    /**
     * Tells the pages whether the host has a network connection (<code>navigator.onLine</code>; the <code>online</code> and <code>offline</code> events fire at the window of
     * the pages). A call that does not change the state does nothing. It may be called on any thread; the events run on the document queue.
     * <p>
     * 호스트에 네트워크 연결이 있는지 페이지에 알린다(<code>navigator.onLine</code>. 페이지의 window 에 <code>online</code>, <code>offline</code> 이벤트가 발생한다).
     * 상태가 바뀌지 않는 호출은 아무것도 하지 않는다. 어느 스레드에서나 부를 수 있고 이벤트는 문서 큐에서 실행된다.
     *
     * @param value true if the host is online / 호스트가 연결되어 있으면 true
     */
    public void setOnline(boolean value) {
        if (online == value) {
            return;
        }
        online = value;
        // 살아 있는 페이지마다 네트워크 상태가 바뀌었음을 알린다.
        for (PageRuntime runtime : liveRuntimes()) {
            runtime.lifecycleChanged(1);
        }
    }

    /**
     * Returns a copy of the list of the registered page hosts.
     * <p>
     * 등록된 페이지 호스트 목록의 복사본을 반환한다.
     *
     * @return the hosts / 호스트
     */
    private List<PageRuntime> liveRuntimes() {
        synchronized (lock) {
            if (disposed || runtimes.isEmpty()) {
                return new ArrayList<PageRuntime>();
            }
            return new ArrayList<PageRuntime>(runtimes);
        }
    }

    /**
     * Lets the page that is shown now run its unloading events (<code>beforeunload</code>, <code>pagehide</code>, <code>unload</code>) before it is replaced by a blank page
     * (<code>clean()</code>, <code>dispose()</code>). It waits for the handlers on the calling thread (at most {@link #UNLOAD_WAIT_MS}), because the page is closed right
     * afterwards; a page without such handlers costs nothing. A navigation does the same without waiting, when the old page is cancelled.
     * <p>
     * 지금 보이는 페이지가 빈 페이지로 바뀌기(<code>clean()</code>, <code>dispose()</code>) 전에 언로드 이벤트(<code>beforeunload</code>, <code>pagehide</code>,
     * <code>unload</code>)를 실행하게 한다. 직후에 페이지를 닫으므로 호출한 스레드에서 핸들러를 기다린다(최대 {@link #UNLOAD_WAIT_MS}). 그런 핸들러가 없는
     * 페이지는 비용이 없다. 이동은 옛 페이지가 취소될 때 기다리지 않고 같은 일을 한다.
     */
    private void unloadCurrentPage() {
        PageSnapshot shown;
        synchronized (lock) {
            shown = current;
        }
        if (shown != null && shown.getDocument() instanceof HtmlPageDocument) {
            ((HtmlPageDocument) shown.getDocument()).unloadNow(UNLOAD_WAIT_MS);
        }
    }

    /**
     * The longest time in milliseconds that <code>clean()</code> and <code>dispose()</code> wait for the unloading handlers of the page.
     * <p>
     * <code>clean()</code> 과 <code>dispose()</code> 가 페이지의 언로드 핸들러를 기다리는 가장 긴 시간(밀리초).
     */
    static final long UNLOAD_WAIT_MS = 500;

    /** Removes a cancelled host without waiting for script cleanup. / 스크립트 해제를 기다리지 않고 취소된 호스트를 제거한다.
     * @param runtime cancelled host / 취소된 호스트 */
    void unregisterRuntime(PageRuntime runtime) {
        synchronized (lock) {
            runtimes.remove(runtime);
            if (installedMessageRuntime == runtime) {
                installedMessageRuntime = null;
            }
        }
    }

    /**
     * Returns the JavaScript flag.
     * <p>
     * JavaScript 설정을 반환한다.
     *
     * @return true if enabled / 켜져 있으면 true
     */
    public boolean isJavascriptEnabled() {
        return javascriptEnabled;
    }

    /**
     * Starts loading an address (API contract 5.2). An address that differs from the shown page only in the fragment is a navigation inside the same document:
     * the page is not requested again and does not hear its unloading events, the history gets an entry, the target scrolls into view and <code>hashchange</code>
     * fires. The same address as the shown one is loaded again as a new entry.
     * <p>
     * 주소 읽기를 시작한다(API 계약 5.2). 보이는 페이지와 프래그먼트만 다른 주소는 같은 문서 안의 이동이다. 페이지를 다시 요청하지 않고 언로드 이벤트도 듣지
     * 않으며, 이력에 항목을 추가하고, 대상이 보이도록 스크롤하며, <code>hashchange</code> 가 발생한다. 보이는 페이지와 완전히 같은 주소는 새 항목으로 다시 읽는다.
     *
     * @param url absolute http, https, ftp, classpath or about:blank address / 절대 http, https, ftp, classpath 또는 about:blank 주소
     * @throws IllegalArgumentException for null, blank, relative, unparsable and about:text addresses / null, 공백, 상대, 해석 불가, about:text 주소일 때
     * @throws IllegalStateException    if disposed / 폐기되었을 때
     */
    public void setPage(String url) {
        checkAlive();
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        if (kind == SchemePolicy.Kind.INVALID) {
            throw new IllegalArgumentException("not an absolute address: " + url);
        }
        if (kind == SchemePolicy.Kind.ABOUT_TEXT) {
            throw new IllegalArgumentException("about:text cannot be navigated to; use setContent");
        }
        String target = url.trim();
        if (kind != SchemePolicy.Kind.BLOCKED) {
            target = canonical(target);
        }
        synchronized (lock) {
            // 현재 문서와 프래그먼트만 다른 주소는 같은 문서 안의 이동이다(문서를 다시 받지 않고 언로드 이벤트도 없다).
            // 주소가 완전히 같은 연속 이동은 계약대로 별도 항목으로 다시 읽고, 그 밖에도 새 페이지를 요청한다.
            if (kind == SchemePolicy.Kind.BLOCKED || target.equals(current.getUrl()) || !startFragmentLocked(target, false)) {
                startLocked(NavigationType.NAVIGATE, target, null, null, false, null, -1);
            }
        }
    }

    /**
     * Normalizes an accepted address (dot segments, default ports...); falls back to the trimmed text on failure.
     * <p>
     * 받아들인 주소를 정규화한다(점 세그먼트, 기본 포트 등). 실패하면 공백만 지운 문자열을 쓴다.
     *
     * @param url trimmed address / 공백을 지운 주소
     * @return canonical address / 정규화한 주소
     */
    private static String canonical(String url) {
        try {
            String n = UrlUtil.normalize(url);
            return n == null ? url : n;
        } catch (RuntimeException e) {
            return url;
        }
    }

    /**
     * Shows content given by the host (API contract 5.10).
     * <p>
     * 호스트가 준 콘텐츠를 보여 준다(API 계약 5.10).
     *
     * @param content     source text kept unchanged; empty is allowed / 그대로 보관하는 원문(빈 문자열 허용)
     * @param contentType MIME type, parameters and case are normalized / MIME 타입(매개변수와 대소문자는 정규화)
     * @throws IllegalArgumentException for null values or an unsupported MIME type / null 이거나 지원하지 않는 MIME 타입일 때
     * @throws IllegalStateException    if disposed / 폐기되었을 때
     */
    public void setContent(String content, String contentType) {
        checkAlive();
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        String mime = ContentClassifier.requireSetContentMime(contentType);
        start(NavigationType.SET_CONTENT, SchemePolicy.ABOUT_TEXT, content, mime, false, null, -1);
    }

    /**
     * Goes back n entries; does nothing if exactly n entries back do not exist (API contract 5.5).
     * <p>
     * 이력을 n 칸 뒤로 이동한다. 정확히 n 칸 뒤가 없으면 아무 동작도 하지 않는다(API 계약 5.5).
     *
     * @param n number of entries / 칸 수
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    public void back(int n) {
        checkAlive();
        if (n <= 0) {
            return;
        }
        synchronized (lock) {
            int target = history.index() - n;
            if (target < 0) {
                return;
            }
            // 같은 문서 안의 항목(프래그먼트만 다른 항목)이면 다시 읽지 않고 현재 문서에서 이동한다.
            if (!startTraversalLocked(history.get(target), target)) {
                startLocked(NavigationType.HISTORY, null, null, null, false, history.get(target), target);
            }
        }
    }

    /**
     * Goes forward n entries; does nothing if exactly n entries forward do not exist (API contract 5.5).
     * <p>
     * 이력을 n 칸 앞으로 이동한다. 정확히 n 칸 앞이 없으면 아무 동작도 하지 않는다(API 계약 5.5).
     *
     * @param n number of entries / 칸 수
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    public void forward(int n) {
        checkAlive();
        if (n <= 0) {
            return;
        }
        synchronized (lock) {
            long target = (long) history.index() + n;
            if (target >= history.size()) {
                return;
            }
            if (!startTraversalLocked(history.get((int) target), (int) target)) {
                startLocked(NavigationType.HISTORY, null, null, null, false, history.get((int) target), (int) target);
            }
        }
    }

    /**
     * Shows a short message in the status text without changing the progress (used for downloads, which never change the page, the
     * history or the progress). Does nothing after disposal.
     * <p>
     * 진행률을 바꾸지 않고 상태 메시지에 짧은 문구를 보인다(페이지, 이력, 진행률을 바꾸지 않는 다운로드가 쓴다). 폐기된 뒤에는 아무것도 하지 않는다.
     *
     * @param text the message / 메시지
     */
    public void setStatusMessage(String text) {
        if (!disposed && text != null) {
            final String oldText;
            final String visibleText;
            synchronized (this) {
                oldText = getStatusText();
                statusText = text;
                visibleText = getStatusText();
            }
            fireStatusText(oldText, visibleText);
        }
    }

    /**
     * Starts a navigation that the page itself asked for (the default action of a link, <code>location.assign</code>,
     * <code>location.replace</code>, assigning <code>location.href</code>). An address that differs from the current page only in the
     * fragment is a navigation inside the same document: the page is not requested again, the history gets an entry (or the current entry
     * changes for <code>replace</code>), the target scrolls into view and <code>hashchange</code> fires. Addresses outside the allowed
     * schemes are ignored. Never throws, also after disposal.
     * <p>
     * 페이지가 스스로 요청한 이동을 시작한다(링크의 기본 동작, <code>location.assign</code>, <code>location.replace</code>,
     * <code>location.href</code> 대입). 현재 페이지와 프래그먼트만 다른 주소는 같은 문서 안의 이동이다. 페이지를 다시 요청하지 않고, 이력에 항목을
     * 추가하며(<code>replace</code> 이면 현재 항목을 바꾼다), 대상이 보이도록 스크롤하고, <code>hashchange</code> 를 보낸다. 허용된 스킴 밖의
     * 주소는 무시한다. 폐기된 뒤에도 예외를 던지지 않는다.
     *
     * @param url     absolute target address / 절대 대상 주소
     * @param replace true to replace the current history entry / 현재 이력 항목을 바꾸려면 true
     */
    void navigateFromPage(String url, boolean replace) {
        if (disposed || url == null) {
            return;
        }
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        if (kind == SchemePolicy.Kind.INVALID || kind == SchemePolicy.Kind.ABOUT_TEXT || kind == SchemePolicy.Kind.BLOCKED) {
            return;
        }
        String target = canonical(url.trim());
        synchronized (lock) {
            if (disposed) {
                return;
            }
            if (!startFragmentLocked(target, replace)) {
                startLocked(replace ? NavigationType.REPLACE : NavigationType.NAVIGATE, target, null, null, false, null, -1);
            }
        }
    }

    /**
     * Starts the download of a link with the <code>download</code> attribute (same origin): the response is saved as a file whatever its type
     * and the page, the URL and the history stay as they are. An address outside the allowed schemes is ignored. Never throws, also after
     * disposal.
     * <p>
     * <code>download</code> 속성이 있는 링크(같은 오리진)의 다운로드를 시작한다: 응답은 형식과 관계없이 파일로 저장되며 페이지, URL, 이력은 그대로이다.
     * 허용된 스킴 밖의 주소는 무시한다. 폐기된 뒤에도 예외를 던지지 않는다.
     *
     * @param url  absolute address of the file / 파일의 절대 주소
     * @param name the suggested file name (empty for none) / 제안하는 파일 이름(없으면 빈 문자열)
     */
    void downloadFromPage(String url, String name) {
        if (disposed || url == null) {
            return;
        }
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        if (kind == SchemePolicy.Kind.INVALID || kind == SchemePolicy.Kind.ABOUT_TEXT || kind == SchemePolicy.Kind.BLOCKED) {
            return;
        }
        String target = canonical(url.trim());
        synchronized (lock) {
            if (disposed) {
                return;
            }
            startLocked(NavigationType.NAVIGATE, target, null, null, false, null, -1, null, name == null ? "" : name);
        }
    }

    /**
     * Sends a captured object URL to the download manager without starting a navigation.
     * <p>
     * 이동을 시작하지 않고 확보한 객체 URL을 다운로드 관리자에게 넘긴다.
     *
     * @param url the clicked URL / 클릭한 URL
     * @param name link's suggested name, or null / 링크가 제안한 이름 또는 null
     * @param blob the entry resolved in the initiating document's scope / 시작 문서의 범위에서 확인한 객체
     */
    void downloadBlobFromPage(String url, String name, BlobUrlStore.Entry blob) {
        synchronized (lock) {
            if (!disposed) {
                enqueueBlobDownloadLocked(canonical(url), name, blob, DownloadRequiredException.Reason.LINK_DOWNLOAD);
            }
        }
    }

    /**
     * Shares a local download with the Swing handler, preserving the current document and progress.
     * <p>
     * 현재 문서와 진행률을 유지하면서 로컬 다운로드를 Swing 처리기에 넘긴다.
     *
     * @param url the requested URL / 요청 URL
     * @param name suggested file name, if any / 제안 파일명. 없으면 null
     * @param blob the captured data / 확보한 데이터
     * @param reason why the response is being saved / 응답을 저장하는 이유
     */
    private void enqueueBlobDownloadLocked(String url, String name, BlobUrlStore.Entry blob, DownloadRequiredException.Reason reason) {
        final NavigationRequest request = new NavigationRequest(nav.currentId(), NavigationType.NAVIGATE, url, null, null,
                false, null, -1, userAgent, headers.copy(), javascriptEnabled, new CancelToken(), FRAME_SINK, null, name).withBlobUrlEntry(blob);
        final DownloadRequiredException download = new DownloadRequiredException(url, url, blobMime(blob),
                NetworkPageLoader.attachmentName(name), blob.length(), reason);
        SwingUtilities.invokeLater(new Runnable() {
            /** Hands off the captured bytes without holding the session lock. / 세션 잠금 없이 확보한 바이트를 넘긴다. */
            public void run() {
                handleDownload(request, download);
            }
        });
    }

    /**
     * Returns the media type of a blob navigation.
     * <p>
     * 객체 URL 이동의 미디어 타입을 반환한다.
     *
     * @param blob captured blob / 확보한 객체
     * @return media type, with the loader's binary default / 로더의 기본 바이너리 타입을 포함한 미디어 타입
     */
    private static String blobMime(BlobUrlStore.Entry blob) {
        return blob.getMimeType().length() == 0 ? "application/octet-stream" : ContentType.parse(blob.getMimeType()).getMime();
    }

    /** Discards the progress of the navigations of frames, whose status the page itself reports. / 프레임 이동의 진행률은 버린다. 페이지 자체가 상태를 보고한다. */
    private static final ProgressSink FRAME_SINK = new ProgressSink() {
        /**
         * Ignores the report.
         * <p>
         * 보고를 무시한다.
         *
         * @param requestId  the request / 요청
         * @param statusText the status text / 상태 문구
         * @param progress   the progress / 진행률
         */
        public void report(int requestId, String statusText, int progress) {
            return;
        }
    };

    /**
     * Builds the request that loads a nested browsing context with the settings of the component (user agent, extra headers, JavaScript flag).
     * The request carries the number of the navigation of the page that owns the frame, so it ends together with that page.
     * <p>
     * 컴포넌트의 설정(User-Agent, 추가 헤더, JavaScript 설정)으로 중첩 브라우징 문맥을 로드하는 요청을 만든다. 요청은 프레임을 소유한 페이지의 이동 번호를
     * 가지므로 그 페이지와 함께 끝난다.
     *
     * @param requestId the navigation number of the owning page / 소유 페이지의 이동 번호
     * @param url       the address to load / 로드할 주소
     * @param force     true to bypass the caches / 캐시를 우회하려면 true
     * @param cancel    the cancellation token of this load / 이 로드의 취소 토큰
     * @param frame     the frame / 프레임
     * @param post      the form body or null / 폼 본문 또는 null
     * @param scripting false if the sandbox forbids scripts / 샌드박스가 스크립트를 막으면 false
     * @return the request / 요청
     */
    NavigationRequest frameRequest(int requestId, String url, boolean force, CancelToken cancel, FrameContext frame, PostData post,
            boolean scripting) {
        synchronized (lock) {
            return new NavigationRequest(requestId, NavigationType.NAVIGATE, url, null, null, force, null, -1, userAgent, headers.copy(),
                    scripting && javascriptEnabled, cancel, FRAME_SINK, post, null).withFrame(frame);
        }
    }

    /**
     * Queues a read (a network load of a frame) on the loader threads of the component.
     * <p>
     * 컴포넌트의 읽기 스레드에 읽기(프레임의 네트워크 로드)를 올린다.
     *
     * @param work the work / 작업
     * @return false if the component was shut down / 컴포넌트가 종료되었으면 false
     */
    boolean submitRead(Runnable work) {
        return nav.submit(work);
    }

    /**
     * Tells whether a navigation number is still the current one of the component.
     * <p>
     * 이동 번호가 컴포넌트의 현재 번호인지 알려 준다.
     *
     * @param id the number / 번호
     * @return true if current / 현재이면 true
     */
    boolean isCurrentRequest(int id) {
        return nav.isCurrent(id);
    }

    /**
     * Hands the response of a frame that has to be saved as a file to the download handling of the component; the frame keeps its document.
     * <p>
     * 파일로 저장해야 하는 프레임의 응답을 컴포넌트의 다운로드 처리에 넘긴다. 프레임은 문서를 그대로 둔다.
     *
     * @param request  the request of the frame / 프레임의 요청
     * @param download the description of the download / 다운로드 설명
     */
    void frameDownload(NavigationRequest request, DownloadRequiredException download) {
        handleDownload(request, download);
    }

    /**
     * Reloads the current page for a script (<code>location.reload()</code>). Does nothing after disposal.
     * <p>
     * 스크립트를 위해 현재 페이지를 다시 읽는다(<code>location.reload()</code>). 폐기된 뒤에는 아무것도 하지 않는다.
     */
    void reloadFromPage() {
        if (!disposed) {
            refresh(false);
        }
    }

    /**
     * Moves in the history for a script (<code>history.go</code>, <code>back</code>, <code>forward</code>); 0 reloads, and an impossible
     * distance does nothing. Does nothing after disposal.
     * <p>
     * 스크립트를 위해 이력에서 이동한다(<code>history.go</code>, <code>back</code>, <code>forward</code>). 0 이면 새로고침이고 불가능한 거리는
     * 아무것도 하지 않는다. 폐기된 뒤에는 아무것도 하지 않는다.
     *
     * @param delta number of entries, negative for back / 칸 수(뒤로는 음수)
     */
    void historyGoFromPage(final int delta) {
        if (disposed) {
            return;
        }
        if (delta == 0) {
            refresh(false);
            return;
        }
        // 이동할 위치는 문서 큐가 이 작업에 닿았을 때 정한다. 앞서 요청한 같은 문서 안의 이동(location.hash 등)이 먼저 이력에 반영되어야
        // 스크립트가 본 순서대로 뒤로/앞으로 이동한다(세션 이력 이동은 문서 큐에서 직렬로 처리한다는 규격의 순서).
        nav.submitDocumentTask(new Runnable() {
            /**
             * Moves in the history once the earlier in-document navigations have been applied.
             * <p>
             * 앞선 문서 안의 이동이 반영된 뒤 이력에서 이동한다.
             */
            public void run() {
                if (disposed) {
                    return;
                }
                try {
                    if (delta < 0) {
                        back(-delta);
                    } else {
                        forward(delta);
                    }
                } catch (IllegalStateException e) {
                    // 그 사이 컴포넌트가 폐기되었으면 이동하지 않는다.
                    return;
                }
            }
        });
    }

    /**
     * Applies <code>history.pushState</code> or <code>replaceState</code> of the confirmed page: the entry of the page gets the new URL and
     * the state object, and for <code>pushState</code> the old entry stays behind it and the forward entries are dropped. The document is not
     * requested again and no navigation starts. A pushed or replaced entry is a plain GET entry, so a form submission that the entry
     * represented is forgotten (HTML5 2014 REC 5.5.2). Never throws, also after disposal.
     * <p>
     * 확정된 페이지의 <code>history.pushState</code>, <code>replaceState</code> 를 적용한다: 페이지의 항목이 새 URL과 상태 객체를 갖고,
     * <code>pushState</code> 이면 이전 항목이 그 뒤에 남으며 앞쪽 항목은 버려진다. 문서를 다시 요청하지 않고 이동도 시작하지 않는다. 푸시하거나 바꾼
     * 항목은 일반 GET 항목이므로 항목이 나타내던 폼 제출은 잊는다(HTML5 2014 REC 5.5.2). 폐기된 뒤에도 예외를 던지지 않는다.
     *
     * @param state   the serialized state object or null / 직렬화된 상태 객체 또는 null
     * @param url     the new absolute URL (the same document origin) / 새 절대 URL(문서와 같은 오리진)
     * @param replace true to replace the current entry, false to add one / 현재 항목을 바꾸려면 true, 추가하려면 false
     */
    void updateHistoryFromPage(ClonedValue state, String url, boolean replace) {
        if (disposed || url == null) {
            return;
        }
        String target = canonical(url);
        List<PageSnapshot> dropped = null;
        PageSnapshot base;
        String oldUrl;
        synchronized (lock) {
            base = current;
            if (disposed || !(base.getDocument() instanceof HtmlPageDocument)) {
                return;
            }
            oldUrl = base.getUrl();
            if (replace) {
                base.changeLocation(target, state, null);
                history.replaceCurrent(base);
            } else {
                // 지금 항목은 문서 없는 복사본으로 남기고, 문서를 가진 현재 스냅샷이 새 항목이 된다.
                history.set(history.index(), base.withoutDocument());
                base.changeLocation(target, state, null);
                dropped = history.push(base);
            }
        }
        if (dropped != null) {
            // 앞쪽 이력에서 지워진 항목의 살아 있는 객체도 해제한다.
            for (PageSnapshot d : dropped) {
                d.dispose();
            }
        }
        fireFragmentChanged(oldUrl, base, null);
    }

    /**
     * Returns the number of entries that <code>history.length</code> reports to the page. Before the page is confirmed its own entry is not in
     * the history yet: a navigation adds one entry (and drops the forward entries), the other kinds reuse an entry.
     * <p>
     * <code>history.length</code> 가 페이지에 알리는 항목 수를 반환한다. 페이지가 확정되기 전에는 자기 항목이 아직 이력에 없다. 새 이동은 항목 하나를
     * 추가하고(앞쪽 항목은 버림) 다른 종류는 항목을 재사용한다.
     *
     * @param confirmed true if the page is confirmed / 페이지가 확정되었으면 true
     * @param type      the kind of the navigation that produced the page / 페이지를 만든 이동의 종류
     * @return the number of entries / 항목 수
     */
    int historyLength(boolean confirmed, NavigationType type) {
        synchronized (lock) {
            if (!confirmed && (type == NavigationType.NAVIGATE || type == NavigationType.SET_CONTENT)) {
                return history.index() + 2;
            }
            return history.size();
        }
    }

    /**
     * Starts a navigation inside the confirmed document if the target differs from the current address only in the fragment (the caller
     * holds the lock). The current page keeps its document; only its URL and the history change, on the document thread.
     * <p>
     * 대상이 현재 주소와 프래그먼트만 다르면 확정된 문서 안의 이동을 시작한다(호출자가 잠금을 쥐고 있다). 현재 페이지는 문서를 그대로
     * 유지하며 문서 스레드에서 URL과 이력만 바뀐다.
     *
     * @param target  the canonical target address / 정규화한 대상 주소
     * @param replace true to replace the current entry / 현재 항목을 바꾸려면 true
     * @return true if the navigation was handled inside the document / 문서 안의 이동으로 처리했으면 true
     */
    private boolean startFragmentLocked(final String target, final boolean replace) {
        final PageSnapshot base = current;
        if (!(base.getDocument() instanceof HtmlPageDocument) || !NavigationPolicy.hasFragment(target)
                || !NavigationPolicy.sameDocument(base.getUrl(), target)) {
            return false;
        }
        // 주소가 완전히 같으면(프래그먼트도 같으면) 항목을 더하지 않고 스크롤만 다시 한다.
        final boolean sameEntry = replace || target.equals(base.getUrl());
        navigationCount++;
        final int id = claimRequestLocked();
        boolean queued = nav.submitDocumentTask(new Runnable() {
            /**
             * Changes the URL and the history on the document thread.
             * <p>
             * 문서 스레드에서 URL과 이력을 바꾼다.
             */
            public void run() {
                commitFragment(id, base, target, sameEntry, null, -1);
            }
        });
        if (!queued) {
            nav.cancelCurrent();
        }
        return true;
    }

    /**
     * Returns the request number for a navigation inside the confirmed document (the caller holds the lock). When nothing else is loading,
     * that is the number of the request that produced the page, so the page and its scripts stay alive; when another navigation is in
     * flight, that navigation is cancelled by starting a new request.
     * <p>
     * 확정된 문서 안의 이동에 쓸 요청 번호를 반환한다(호출자가 잠금을 쥐고 있다). 다른 읽기가 없으면 페이지를 만든 요청의 번호이므로 페이지와 그
     * 스크립트가 살아 있고, 다른 이동이 진행 중이면 새 요청을 시작하여 그 이동을 취소한다.
     *
     * @return the request number / 요청 번호
     */
    private int claimRequestLocked() {
        if (nav.currentId() == confirmedRequestId) {
            return confirmedRequestId;
        }
        return nav.begin().id;
    }

    /**
     * Starts the move to a history entry of the same loaded document (the entries differ in the fragment, or in the address and state that
     * <code>history.pushState</code> gave them); the caller holds the lock. The entries of one document share the document identifier, so
     * the document is never requested again for such a move.
     * <p>
     * 같은 문서의 이력 항목(프래그먼트가 다르거나 <code>history.pushState</code> 가 준 주소와 상태가 다른 항목)으로의 이동을 시작한다. 호출자가 잠금을
     * 쥐고 있다. 한 문서의 항목은 문서 식별자를 공유하므로 이런 이동에서는 문서를 다시 요청하지 않는다.
     *
     * @param entry       the target entry / 대상 항목
     * @param targetIndex its position / 그 위치
     * @return true if handled inside the document / 문서 안의 이동으로 처리했으면 true
     */
    private boolean startTraversalLocked(final PageSnapshot entry, final int targetIndex) {
        final PageSnapshot base = current;
        if (!(base.getDocument() instanceof HtmlPageDocument) || entry.getDocumentId() != base.getDocumentId()) {
            return false;
        }
        final String target = entry.getUrl();
        navigationCount++;
        final int id = claimRequestLocked();
        boolean queued = nav.submitDocumentTask(new Runnable() {
            /**
             * Moves inside the document on the document thread.
             * <p>
             * 문서 스레드에서 문서 안의 이동을 한다.
             */
            public void run() {
                commitFragment(id, base, target, true, entry, targetIndex);
            }
        });
        if (!queued) {
            nav.cancelCurrent();
        }
        return true;
    }

    /**
     * Applies a navigation inside the document on the document thread: the current snapshot (and its document) takes the new URL, the
     * history records it, and the observer scrolls to the target.
     * <p>
     * 문서 안의 이동을 문서 스레드에서 적용한다: 현재 스냅샷(과 그 문서)이 새 URL을 갖고, 이력에 기록되며, 관찰자가 대상으로 스크롤한다.
     *
     * @param id          request number / 요청 번호
     * @param base        the page that the move started from / 이동을 시작한 페이지
     * @param target      the new URL / 새 URL
     * @param sameEntry   true if the current entry is replaced instead of a new one being added / 새 항목을 더하지 않고 현재 항목을 바꾸면 true
     * @param entry       the history entry that a traversal moves to (its URL and state become those of the page), or null / 이력 이동이 향하는
     *                    항목(그 URL과 상태가 페이지의 것이 된다). 이동이 아니면 null
     * @param targetIndex the history position to move to, or -1 for a navigation / 이동할 이력 위치. 이동이면 -1
     */
    private void commitFragment(int id, PageSnapshot base, String target, boolean sameEntry, PageSnapshot entry, int targetIndex) {
        List<PageSnapshot> dropped = null;
        String oldUrl;
        ClonedValue state;
        boolean popState;
        synchronized (lock) {
            if (disposed || !nav.isCurrent(id) || current != base) {
                return;
            }
            oldUrl = base.getUrl();
            if (targetIndex >= 0) {
                // 이력 이동: 지금 항목은 문서 없는 복사본으로 남기고 대상 위치로 옮긴 뒤 그 자리에 문서를 가진 현재 스냅샷을 둔다.
                // 대상 항목의 URL, 상태, 폼 제출이 페이지의 것이 된다(항목이 다르므로 popstate 를 보낸다).
                history.set(history.index(), base.withoutDocument());
                history.moveTo(targetIndex);
                state = entry.getHistoryState();
                base.changeLocation(entry.getUrl(), state, entry.getPost());
                history.replaceCurrent(base);
                popState = true;
            } else if (sameEntry) {
                // 같은 주소로 다시 가는 이동은 항목을 그대로 두고, 주소가 다른 replace 는 상태 없는 새 항목으로 바꾼다.
                boolean sameUrl = target.equals(oldUrl);
                state = sameUrl ? base.getHistoryState() : null;
                base.changeLocation(target, state, sameUrl ? base.getPost() : null);
                history.replaceCurrent(base);
                popState = !sameUrl;
            } else {
                // 프래그먼트 이동이 만드는 새 항목은 상태가 없다.
                history.set(history.index(), base.withoutDocument());
                state = null;
                base.changeLocation(target, null, null);
                dropped = history.push(base);
                popState = true;
            }
        }
        double[] where = ((HtmlPageDocument) base.getDocument()).locationChanged(oldUrl, base.getUrl(), state, popState);
        if (dropped != null) {
            // 앞쪽 이력에서 지워진 항목의 살아 있는 객체도 해제한다.
            for (PageSnapshot d : dropped) {
                d.dispose();
            }
        }
        fireFragmentChanged(oldUrl, base, where);
        updateStatus("Done", 100);
    }

    /**
     * Notifies the observer of a navigation inside the document.
     * <p>
     * 관찰자에게 문서 안의 이동을 알린다.
     *
     * @param oldUrl   the URL before / 이전 URL
     * @param snapshot the page with its new URL / 새 URL을 가진 페이지
     * @param where    the target box in document coordinates or null / 문서 좌표의 대상 상자. 없으면 null
     */
    private void fireFragmentChanged(final String oldUrl, final PageSnapshot snapshot, final double[] where) {
        final SessionListener l = listener;
        if (l == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Notifies the URL change and the scroll target on the EDT.
             * <p>
             * EDT 에서 URL 변경과 스크롤 대상을 알린다.
             */
            public void run() {
                l.fragmentChanged(oldUrl, snapshot, where);
                if (!oldUrl.equals(snapshot.getUrl())) {
                    l.pageChanged(oldUrl, snapshot.getUrl());
                }
            }
        });
    }

    /**
     * Forwards a scroll request of a script to the component (on the EDT), see {@link SessionListener#scrollRequested(double, double)}.
     * <p>
     * 스크립트의 스크롤 요청을 컴포넌트에 전달한다(EDT 에서). {@link SessionListener#scrollRequested(double, double)} 참조.
     *
     * @param x the x in document pixels / 문서 픽셀의 x
     * @param y the y in document pixels / 문서 픽셀의 y
     */
    void requestScroll(final double x, final double y) {
        final SessionListener l = listener;
        if (l == null || disposed) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Passes the request to the listener on the EDT.
             * <p>
             * EDT 에서 리스너에 요청을 넘긴다.
             */
            public void run() {
                if (!disposed) {
                    l.scrollRequested(x, y);
                }
            }
        });
    }

    /**
     * Reloads the current page (API contract 5.8).
     * <p>
     * 현재 페이지를 다시 읽는다(API 계약 5.8).
     *
     * @param force true to bypass caches / 캐시를 우회하려면 true
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    public void refresh(boolean force) {
        checkAlive();
        PageSnapshot shown = current;
        // 폼 제출이 만든 페이지는 자동으로 다시 보내지 않고 사용자가 동의한 뒤에만 다시 보낸다.
        if (shown.getPost() != null) {
            resubmit(shown, force);
            return;
        }
        synchronized (lock) {
            PageSnapshot cur = current;
            startLocked(NavigationType.REFRESH, null, null, null, force, cur, history.index());
        }
    }

    /**
     * Installs the confirmer that is asked before a form is sent again (a refresh of a page that a <code>POST</code> produced). Without
     * one the form is never sent again.
     * <p>
     * 폼을 다시 보내기 전에(<code>POST</code> 가 만든 페이지의 새로고침) 묻는 확인기를 설치한다. 없으면 폼을 다시 보내지 않는다.
     *
     * @param confirmer the confirmer or null / 확인기 또는 null
     */
    public void setResubmitConfirmer(ResubmitConfirmer confirmer) {
        this.resubmitConfirmer = confirmer;
    }

    /**
     * Sends the form of a shown page again if the user agrees: the question is asked on the event dispatch thread, a form whose body was
     * too large to keep is never sent again, and nothing is sent when the user went elsewhere in the meantime.
     * <p>
     * 사용자가 동의하면 보이는 페이지의 폼을 다시 보낸다: 질문은 이벤트 디스패치 스레드에서 하고, 본문이 너무 커서 보관하지 않은 폼은 다시 보내지
     * 않으며, 그 사이 사용자가 다른 곳으로 갔으면 아무것도 보내지 않는다.
     *
     * @param page  the page that a form produced / 폼이 만든 페이지
     * @param force true to bypass caches / 캐시를 우회하려면 true
     */
    private void resubmit(final PageSnapshot page, final boolean force) {
        final PostData data = page.getPost();
        final ResubmitConfirmer asker = resubmitConfirmer;
        if (!data.isResendable()) {
            setStatusMessage("The form cannot be sent again: its data was too large to keep");
            return;
        }
        if (asker == null) {
            setStatusMessage("The form was not sent again: no confirmation is available");
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Asks the user and sends the form again on a yes.
             * <p>
             * 사용자에게 묻고 승낙하면 폼을 다시 보낸다.
             */
            public void run() {
                if (disposed) {
                    return;
                }
                boolean yes;
                try {
                    yes = asker.confirmResubmit(page.getUrl());
                } catch (RuntimeException e) {
                    yes = false;
                }
                if (!yes) {
                    setStatusMessage("The form was not sent again");
                    return;
                }
                synchronized (lock) {
                    // 묻는 동안 다른 페이지로 이동했거나 폐기되었으면 보내지 않는다.
                    if (disposed || current != page) {
                        return;
                    }
                    startLocked(NavigationType.REFRESH, null, null, null, force, page, history.index(), data, null);
                }
            }
        });
    }

    /**
     * Receives a form submission of the page and starts the navigation that it asks for (the {@link FormSubmitSink} contract; the call
     * arrives on the document thread of the page and does not block it).
     * <p>
     * 페이지의 폼 제출을 받아 그것이 요구하는 이동을 시작한다({@link FormSubmitSink} 계약. 호출은 페이지의 문서 스레드에서 오며 이 스레드를 막지 않는다).
     *
     * @param submission the submission / 제출 정보
     */
    public void submit(org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission submission) {
        submitForm(submission);
    }

    /**
     * Starts the navigation that a form submission asks for (called by the form host of the page on the document thread). A
     * <code>GET</code> is an ordinary navigation to the address with the query; a <code>POST</code> sends the body to an http or https
     * address and the page that answers keeps the submission, so that the history shows it from its stored source and a refresh asks the
     * user. The navigation policy of links applies: only the allowed schemes, never from a network page to the class path. The
     * <code>target</code> is not used (there is one browsing context). Never throws, also after disposal.
     * <p>
     * 폼 제출이 요구하는 이동을 시작한다(페이지의 폼 호스트가 문서 스레드에서 부른다). <code>GET</code> 은 쿼리가 붙은 주소로의 일반 이동이고,
     * <code>POST</code> 는 http/https 주소로 본문을 보내며 응답한 페이지는 제출을 보관하여 이력은 저장한 원문으로 보여 주고 새로고침은 사용자에게
     * 묻는다. 링크의 이동 정책이 적용된다: 허용된 스킴만, 네트워크 페이지에서 클래스패스로는 가지 않는다. <code>target</code> 은 쓰지 않는다(브라우징
     * 문맥이 하나다). 폐기된 뒤에도 예외를 던지지 않는다.
     *
     * @param submission the submission / 제출 정보
     */
    void submitForm(org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission submission) {
        if (disposed || submission == null || submission.getUrl() == null) {
            return;
        }
        String url = submission.getUrl().trim();
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        if (kind == SchemePolicy.Kind.INVALID || kind == SchemePolicy.Kind.ABOUT_TEXT || kind == SchemePolicy.Kind.BLOCKED
                || !NavigationPolicy.allowed(current.getUrl(), url)) {
            return;
        }
        boolean post = org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission.POST.equals(submission.getMethod());
        if (post && kind != SchemePolicy.Kind.HTTP && kind != SchemePolicy.Kind.HTTPS) {
            setStatusMessage("A form can only be sent with POST to an http or https address");
            return;
        }
        String target = canonical(url);
        PostData data = null;
        if (post) {
            byte[] body = submission.getBody();
            data = new PostData(submission.getContentType(), body == null ? new byte[0] : body, current.getUrl());
        }
        synchronized (lock) {
            if (disposed) {
                return;
            }
            // GET 이 현재 문서의 프래그먼트로만 가는 경우는 같은 문서 안의 이동이다. 그 밖에는 새 페이지를 요청한다.
            if (post || !startFragmentLocked(target, false)) {
                startLocked(NavigationType.NAVIGATE, target, null, null, false, null, -1, data, null);
            }
        }
    }

    /**
     * Cancels the running navigation, if any, and resets progress (extension method).
     * <p>
     * 진행 중인 이동이 있으면 취소하고 진행률을 되돌린다(확장 메서드).
     *
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    public void stop() {
        checkAlive();
        synchronized (lock) {
            nav.cancelCurrent();
        }
        updateStatus("", 100);
    }

    /**
     * Starts a navigation (takes the lock).
     * <p>
     * 이동을 시작한다(잠금을 잡는다).
     *
     * @param type        navigation type / 이동 종류
     * @param url         target URL / 대상 URL
     * @param content     source for set-content / set-content 의 원문
     * @param mime        MIME type for set-content / set-content 의 MIME 타입
     * @param force       bypass caches / 캐시 우회
     * @param restore     stored entry or null / 보관된 항목 또는 null
     * @param targetIndex history position or -1 / 이력 위치 또는 -1
     */
    private void start(NavigationType type, String url, String content, String mime, boolean force, PageSnapshot restore, int targetIndex) {
        synchronized (lock) {
            startLocked(type, url, content, mime, force, restore, targetIndex);
        }
    }

    /**
     * Starts a navigation while holding the lock: takes a request number, cancels the previous work and queues the load.
     * <p>
     * 잠금을 잡은 채 이동을 시작한다: 요청 번호를 받고 이전 작업을 취소한 뒤 읽기를 대기열에 넣는다.
     *
     * @param type        navigation type / 이동 종류
     * @param url         target URL, or null to take it from the stored entry / 대상 URL. 보관된 항목에서 가져오려면 null
     * @param content     source for set-content / set-content 의 원문
     * @param mime        MIME type for set-content / set-content 의 MIME 타입
     * @param force       bypass caches / 캐시 우회
     * @param restore     stored entry or null / 보관된 항목 또는 null
     * @param targetIndex history position or -1 / 이력 위치 또는 -1
     */
    private void startLocked(NavigationType type, String url, String content, String mime, boolean force, PageSnapshot restore,
            int targetIndex) {
        startLocked(type, url, content, mime, force, restore, targetIndex, null, null);
    }

    /**
     * Starts a navigation while holding the lock, optionally sending a form (<code>POST</code>) or saving the response as a file.
     * <p>
     * 잠금을 잡은 채 이동을 시작한다. 폼을 보내거나(<code>POST</code>) 응답을 파일로 저장할 수 있다.
     *
     * @param type        navigation type / 이동 종류
     * @param url         target URL, or null to take it from the stored entry / 대상 URL. 보관된 항목에서 가져오려면 null
     * @param content     source for set-content / set-content 의 원문
     * @param mime        MIME type for set-content / set-content 의 MIME 타입
     * @param force       bypass caches / 캐시 우회
     * @param restore     stored entry or null / 보관된 항목 또는 null
     * @param targetIndex history position or -1 / 이력 위치 또는 -1
     * @param post        the form to send or null / 보낼 폼 또는 null
     * @param downloadName the suggested file name to save the response as a file (empty for none), or null / 응답을 파일로 저장할 때 제안하는 이름(없으면 빈 문자열). 일반 이동이면 null
     */
    private void startLocked(NavigationType type, String url, String content, String mime, boolean force, PageSnapshot restore,
            int targetIndex, PostData post, String downloadName) {
        String target = url;

        String text = content;
        String textMime = mime;
        // 이력/새로고침은 보관된 항목에서 주소와(텍스트면) 원문을 가져온다.
        if (restore != null && target == null) {
            target = restore.getUrl();
            if (restore.getKind() == PageKind.TEXT) {
                text = restore.getContent();
                textMime = restore.getMimeType();
            }
        }
        // URL을 검증한 뒤에 nav.begin이 기존 생성 문서의 수명을 끝내도 읽을 바이트를 유지한다.
        BlobUrlStore store = blobUrls;
        BlobUrlStore.Entry blob = store != null && "blob".equals(UrlUtil.getScheme(target)) ? store.resolve(target, null, null, true) : null;
        // 이미 확인된 바이너리 객체를 저장할 때는 현재 문서의 이동 번호와 실행 범위를 그대로 유지한다.
        if (blob != null && post == null && ContentClassifier.kindOfMime(blobMime(blob)) == ContentKind.DOWNLOAD) {
            enqueueBlobDownloadLocked(target, downloadName, blob, DownloadRequiredException.Reason.NOT_DISPLAYABLE);
            return;
        }
        NavigationController.Ticket ticket = nav.begin();
        navigationCount++;
        final NavigationRequest request = new NavigationRequest(ticket.id, type, target, text, textMime, force, restore, targetIndex,
                userAgent, headers.copy(), javascriptEnabled, ticket.token, sink, post, downloadName).withBlobUrlEntry(blob);
        updateStatus("Loading " + target, 0);
        boolean queued = nav.submit(new Runnable() {
            /**
             * Loads the page on the document thread and commits it if the request is still current.
             * <p>
             * 문서 스레드에서 페이지를 읽고 요청이 여전히 현재이면 확정한다.
             */
            public void run() {
                runNavigation(request);
            }
        });
        if (!queued) {
            nav.cancelCurrent();
        }
    }

    /**
     * Runs the loading part of one navigation on a loader thread, then hands the result to the document thread.
     * <p>
     * 이동 하나의 읽기 부분을 읽기 스레드에서 실행하고 결과를 문서 스레드에 넘긴다.
     *
     * @param request the request / 요청
     */
    private void runNavigation(NavigationRequest request) {
        if (!nav.isCurrent(request.getId())) {
            return;
        }
        PageSnapshot snapshot;
        try {
            snapshot = loader.load(request);
            if (snapshot == null) {
                throw new IllegalStateException("page loader returned null");
            }
        } catch (CancelledException e) {
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (DownloadRequiredException e) {
            handleDownload(request, e);
            return;
        } catch (Throwable t) {
            if (request.getCancelToken().isCancelled()) {
                return;
            }
            snapshot = PageSnapshot.error(request.getUrl() == null ? "about:blank" : request.getUrl(),
                    "<!DOCTYPE html><html><body><h1>Error</h1><p>" + HtmlText.escape(String.valueOf(t)) + "</p></body></html>", "text/html", 500);
        }
        final PageSnapshot loaded = snapshot;
        final NavigationRequest finished = request;
        // 확정은 컴포넌트의 단일 문서 스레드에서 직렬로 수행한다. 문서 스레드가 이미 종료되었으면 읽은 페이지를 버린다.
        boolean queued = nav.submitDocumentTask(new Runnable() {
            /**
             * Confirms the loaded page on the document thread.
             * <p>
             * 문서 스레드에서 읽은 페이지를 확정한다.
             */
            public void run() {
                commit(finished, loaded);
            }
        });
        if (!queued) {
            loaded.dispose();
        }
    }

    /**
     * Handles a navigation whose response must be downloaded: the page, URL and history stay as they are (API contract section 7).
     * The installed {@link DownloadHandler} gets the download; without one the status text reports it.
     * <p>
     * 응답을 내려받아야 하는 이동을 처리한다: 페이지, URL, 이력은 그대로 둔다(API 계약 7절). 설치된 {@link DownloadHandler} 가 다운로드를 받고,
     * 없으면 상태 메시지로 알린다.
     *
     * @param request  the navigation request / 이동 요청
     * @param download description of the download / 다운로드 설명
     */
    private void handleDownload(NavigationRequest request, DownloadRequiredException download) {
        if (disposed || !nav.isCurrent(request.getId())) {
            return;
        }
        DownloadHandler h = downloadHandler;
        if (h == null) {
            reportDownloadStatus(request, "Download required: " + download.getFinalUrl() + " (download is not available in this build)");
            return;
        }
        // 폼 제출(POST)의 응답을 파일로 저장하려면 폼을 한 번 더 보내야 하므로 사용자가 동의해야 한다.
        if (request.getPost() != null) {
            askAndDownload(h, request, download);
            return;
        }
        reportDownloadStatus(request, "Download: " + download.getFinalUrl());
        try {
            h.downloadRequired(request, download);
        } catch (RuntimeException e) {
            reportDownloadStatus(request, "Download failed: " + e);
        }
    }

    /**
     * Reports the handoff while preserving any active page progress for a local object.
     * <p>
     * 로컬 객체를 넘길 때 실행 중인 페이지의 진행률을 보존하며 상태를 보고한다.
     *
     * @param request download source request / 다운로드 출처 요청
     * @param message download status / 다운로드 상태
     */
    private void reportDownloadStatus(NavigationRequest request, String message) {
        if (request.getBlobUrlEntry() == null) {
            updateStatus(message, 100);
        } else {
            setStatusMessage(message);
        }
    }

    /**
     * Saves the response of a form submission as a file after the user agreed that the form is sent a second time (the first response was
     * dropped because it has to be downloaded, and the transfer asks for the response again).
     * <p>
     * 폼 제출의 응답을 사용자가 폼을 두 번째로 보내는 데 동의한 뒤 파일로 저장한다(첫 응답은 내려받아야 하므로 버려졌고, 전송이 응답을 다시 요청한다).
     *
     * @param h        the download handler / 다운로드 핸들러
     * @param request  the navigation that sent the form / 폼을 보낸 이동
     * @param download the description of the response / 응답의 설명
     */
    private void askAndDownload(final DownloadHandler h, final NavigationRequest request, final DownloadRequiredException download) {
        final ResubmitConfirmer asker = resubmitConfirmer;
        if (asker == null || !request.getPost().isResendable()) {
            updateStatus("The response of the form cannot be saved: the form would have to be sent again", 100);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Asks the user and starts the download on a yes.
             * <p>
             * 사용자에게 묻고 승낙하면 다운로드를 시작한다.
             */
            public void run() {
                if (disposed) {
                    return;
                }
                boolean yes;
                try {
                    yes = asker.confirmResubmit(download.getUrl());
                } catch (RuntimeException e) {
                    yes = false;
                }
                if (!yes) {
                    setStatusMessage("The response of the form was not saved");
                    return;
                }
                updateStatus("Download: " + download.getFinalUrl(), 100);
                try {
                    h.downloadRequired(request, download);
                } catch (RuntimeException e) {
                    updateStatus("Download failed: " + e, 100);
                }
            }
        });
    }


    /**
     * Confirms a loaded page if its request is still current; otherwise the page is discarded.
     * <p>
     * 읽은 페이지의 요청이 여전히 현재이면 확정하고, 아니면 폐기한다.
     *
     * @param request  request that produced the page / 페이지를 만든 요청
     * @param snapshot the loaded page / 읽은 페이지
     */
    private void commit(NavigationRequest request, PageSnapshot snapshot) {
        PageSnapshot old;
        List<PageSnapshot> dropped = null;
        synchronized (lock) {
            if (disposed || !nav.isCurrent(request.getId())) {
                snapshot.dispose();
                return;
            }
            old = current;
            // 이력 이동과 새로고침으로 다시 읽은 문서는 그 항목의 상태 객체(history.state)를 이어받는다.
            if ((request.getType() == NavigationType.HISTORY || request.getType() == NavigationType.REFRESH) && request.getRestore() != null) {
                snapshot.setHistoryState(request.getRestore().getHistoryState());
            }
            // 이동 종류에 따라 이력에 기록하는 방법이 다르다.
            switch (request.getType()) {
            case NAVIGATE:
            case SET_CONTENT:
                history.set(history.index(), old.withoutDocument());
                dropped = history.push(snapshot);
                break;
            case HISTORY:
                history.set(history.index(), old.withoutDocument());
                history.moveTo(request.getTargetIndex());
                history.replaceCurrent(snapshot);
                break;
            default:
                history.replaceCurrent(snapshot);
                break;
            }
            current = snapshot;
            confirmedRequestId = request.getId();
        }
        old.dispose();
        if (snapshot.getDocument() instanceof HtmlPageDocument) {
            ((HtmlPageDocument) snapshot.getDocument()).activate(snapshot);
        }
        if (dropped != null) {
            // 앞쪽 이력에서 지워진 항목의 살아 있는 객체도 해제한다.
            for (PageSnapshot d : dropped) {
                d.dispose();
            }
        }
        firePageConfirmed(old, snapshot);
        if (snapshot.getKind() == PageKind.ERROR) {
            updateStatus("Error " + snapshot.getStatusCode(), 100);
        } else if (tracksLoad(snapshot)) {
            // 스타일시트, 이미지, 스크립트가 남아 있을 수 있는 문서는 뷰가 배치를 마치고 알려 줄 때까지 진행률을 끝내지 않는다.
            beginLoadProgress(snapshot);
        } else {
            updateStatus("Done", 100);
        }
    }

    /**
     * Tells whether the loading of a confirmed page continues after confirmation and is reported by the view: a document of the component
     * with page-owned resources, while a view is attached.
     * <p>
     * 확정된 페이지의 로딩이 확정 뒤에도 이어지고 뷰가 보고하는지 알려 준다: 뷰가 붙어 있는 동안 페이지 소유 리소스가 있는 컴포넌트의 문서.
     *
     * @param snapshot the confirmed page / 확정된 페이지
     * @return true if the progress ends later / 진행률이 나중에 끝나면 true
     */
    private boolean tracksLoad(PageSnapshot snapshot) {
        Object doc = snapshot.getDocument();
        return viewAttached && doc instanceof HtmlPageDocument && ((HtmlPageDocument) doc).getPresentation() != null;
    }

    /**
     * Marks that a view is attached and will report the loading of confirmed pages ({@link #pageLoadProgress}).
     * <p>
     * 뷰가 붙어 확정된 페이지의 로딩을 보고할 것임을 표시한다({@link #pageLoadProgress}).
     */
    void attachView() {
        viewAttached = true;
    }

    /**
     * Starts the progress after confirmation at the point where the body was received (50), unless the view already reported more.
     * <p>
     * 본문 수신이 끝난 지점(50)에서 확정 뒤의 진행률을 시작한다. 뷰가 이미 더 많이 보고했으면 그대로 둔다.
     *
     * @param snapshot the confirmed page / 확정된 페이지
     */
    private void beginLoadProgress(PageSnapshot snapshot) {
        Object doc = ((HtmlPageDocument) snapshot.getDocument()).loadToken();
        synchronized (loadLock) {
            // 뷰의 보고가 이 메서드보다 먼저 도착했으면(끝났거나 진행 중) 되돌리지 않는다.
            if (loadDone == doc || loadTracked == doc || snapshot != current || disposed) {
                return;
            }
            loadTracked = doc;
            loadPercent = LOAD_START_PERCENT;
            updateStatus("Loading resources (0/0)", LOAD_START_PERCENT);
        }
    }

    /**
     * Receives the loading state of the confirmed page from its view after each layout: how many page resources (stylesheets, images) ended
     * of the started ones, and whether the scripts finished (the window <code>load</code> event). The progress runs from 50 to 94 with the
     * ended share of the resources and becomes "Done" (100) when everything ended, a failed resource counting as ended. Reports of a page
     * that is no longer current, of an error page and of a page whose loading already ended are ignored.
     * <p>
     * 확정된 페이지의 뷰가 배치할 때마다 보내는 로딩 상태를 받는다: 시작한 페이지 리소스(스타일시트, 이미지) 중 몇 개가 끝났는지와 스크립트가
     * 끝났는지(window <code>load</code> 이벤트). 진행률은 끝난 리소스 비율에 따라 50에서 94까지 가고, 모두 끝나면(실패한 리소스도 끝난 것으로 센다)
     * "Done"(100)이 된다. 더 이상 현재가 아닌 페이지, 오류 페이지, 이미 로딩이 끝난 페이지의 보고는 무시한다.
     *
     * @param snapshot the page the view shows / 뷰가 보여 주는 페이지
     * @param finished the number of resources that ended / 끝난 리소스 수
     * @param started  the number of resources that were started / 시작한 리소스 수
     * @param scripts  true if the scripts of the page finished loading / 페이지의 스크립트 로딩이 끝났으면 true
     */
    void pageLoadProgress(PageSnapshot snapshot, int finished, int started, boolean scripts) {
        if (!(snapshot.getDocument() instanceof HtmlPageDocument) || snapshot.getKind() == PageKind.ERROR) {
            return;
        }
        // 이전 문서를 붙잡지 않도록 문서 자체가 아니라 문서마다 하나인 작은 표식으로 비교한다.
        Object doc = ((HtmlPageDocument) snapshot.getDocument()).loadToken();
        synchronized (loadLock) {
            // 더 새로운 이동이 진행 중이면 상태와 진행률은 그 이동의 것이다. 이전 페이지의 늦은 보고가 그것을 덮어쓰면 안 된다.
            if (disposed || snapshot != current || loadDone == doc || nav.currentId() != confirmedRequestId) {
                return;
            }
            if (loadTracked != doc) {
                loadTracked = doc;
                loadPercent = LOAD_START_PERCENT;
            }
            // 리소스와 스크립트가 모두 끝나면 로딩이 끝난다.
            if (scripts && finished >= started) {
                loadDone = doc;
                updateStatus("Done", 100);
                return;
            }
            // 끝나지 않은 스크립트는 리소스 한 개로 센다. 그러면 마지막 리소스가 끝나도 94 를 넘지 않는다.
            int total = started + (scripts ? 0 : 1);
            int percent = LOAD_START_PERCENT + (LOAD_END_PERCENT - LOAD_START_PERCENT) * finished / total;
            // 진행률은 줄어들지 않는다.
            if (percent < loadPercent) {
                percent = loadPercent;
            }
            loadPercent = percent;
            updateStatus("Loading resources (" + finished + "/" + started + ")", percent);
        }
    }

    /**
     * Returns the URL of the confirmed page.
     * <p>
     * 확정된 페이지의 URL 을 반환한다.
     *
     * @return URL / URL
     */
    public String getPage() {
        return current.getUrl();
    }

    /**
     * Returns the source text of the confirmed page.
     * <p>
     * 확정된 페이지의 원문을 반환한다.
     *
     * @return source text / 원문
     */
    public String getContent() {
        return current.getContent();
    }

    /**
     * Returns the MIME type of the confirmed page.
     * <p>
     * 확정된 페이지의 MIME 타입을 반환한다.
     *
     * @return MIME type / MIME 타입
     */
    public String getMimeType() {
        return current.getMimeType();
    }

    /**
     * Returns the title of the confirmed page.
     * <p>
     * 확정된 페이지의 제목을 반환한다.
     *
     * @return title, empty if unknown / 제목(모르면 빈 문자열)
     */
    public String getTitle() {
        return current.getTitle();
    }

    /**
     * Returns the confirmed page snapshot.
     * <p>
     * 확정된 페이지 스냅샷을 반환한다.
     *
     * @return snapshot / 스냅샷
     */
    public PageSnapshot getSnapshot() {
        return current;
    }

    /**
     * Returns a copy of all history URLs.
     * <p>
     * 모든 이력 URL 의 복사본을 반환한다.
     *
     * @return URLs, oldest first / URL 목록(오래된 것이 앞)
     */
    public Vector<String> getHistories() {
        return history.urls();
    }

    /**
     * Returns the current history position (0 based).
     * <p>
     * 현재 이력 위치(0 기반)를 반환한다.
     *
     * @return position / 위치
     */
    public int getHistoryIndex() {
        return history.index();
    }

    /**
     * Returns the status bar text.
     * <p>
     * 상태바 메시지를 반환한다.
     *
     * @return status text / 상태 메시지
     */
    public String getStatusText() {
        String hover = hoverText;
        return hover == null ? statusText : hover;
    }

    /**
     * Returns the navigation serial so default actions can detect a listener-initiated move before it commits.
     * <p>리스너가 시작한 이동을 확정 전에도 기본 동작이 감지하도록 이동 번호를 반환한다.
     * @return number of started navigations / 시작한 이동 수
     */
    public long getNavigationCount() {
        return navigationCount;
    }

    /**
     * Sets or clears a link-hover overlay without changing progress or losing the underlying load status.
     * <p>진행률이나 원래 로딩 상태를 잃지 않고 링크 호버 표시를 설정·해제한다.
     * @param href hovered address, or null to restore loading status / 호버 주소. null이면 원래 상태 복원
     */
    public void setHoverText(String href) {
        String before;
        String after;
        synchronized (this) {
            before = getStatusText();
            hoverText = disposed ? null : href;
            after = getStatusText();
        }
        fireStatusText(before, after);
    }

    /**
     * Returns the progress value: 0 to 100, or -1 when indeterminate.
     * <p>
     * 진행률을 반환한다: 0~100, 불확정이면 -1.
     *
     * @return progress / 진행률
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Cancels everything, shows the blank page, resets the history and clears this component's caches and storage
     * (API contract 5.13). User agent, headers, JavaScript flag and listeners are kept.
     * <p>
     * 모든 작업을 취소하고 빈 페이지를 보여 주며 이력을 초기화하고 이 컴포넌트의 캐시와 저장소를 비운다(API 계약 5.13).
     * User-Agent, 헤더, JavaScript 설정, 리스너는 유지한다.
     *
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    public void clean() {
        checkAlive();
        resetToBlank();
    }

    /**
     * Resets to the blank page without checking the disposed flag.
     * <p>
     * 폐기 여부를 확인하지 않고 빈 페이지로 되돌린다.
     */
    private void resetToBlank() {
        // 보이던 페이지의 언로드 이벤트를 먼저 실행한다(잠금 밖에서 기다린다).
        unloadCurrentPage();
        PageSnapshot old;
        List<PageSnapshot> dropped;
        synchronized (lock) {
            nav.cancelCurrent();
            hoverText = null;
            old = current;
            current = PageSnapshot.blank();
            dropped = history.reset(PageSnapshot.blank());
            sessionStorage.clear();
            localStorage.clearAll();
            // 로더가 컴포넌트의 캐시(HTTP 캐시 등)를 가지고 있으면 함께 비운다.
            if (loader instanceof CacheOwner) {
                ((CacheOwner) loader).clearCache();
            }
        }
        old.dispose();
        // 이력에 있던 나머지 항목의 살아 있는 객체를 해제한다. 현재 항목은 위에서 이미 해제했으므로 건너뛴다.
        for (PageSnapshot d : dropped) {
            if (d != old) {
                d.dispose();
            }
        }
        firePageConfirmed(old, current);
        updateStatus("", 100);
    }

    /**
     * Disposes the session: cleans up, stops the document thread, forgets headers and the observer. Calling it again does nothing.
     * <p>
     * 세션을 폐기한다: 정리하고, 문서 스레드를 멈추며, 헤더와 관찰자를 잊는다. 다시 호출해도 아무 일도 없다.
     */
    public void dispose() {
        synchronized (lock) {
            if (disposed) {
                return;
            }
        }
        resetToBlank();
        synchronized (lock) {
            disposed = true;
        }
        nav.shutdown();
        headers.clear();
        cookies.clear();
        listener = null;
    }

    /**
     * Tells whether the session was disposed.
     * <p>
     * 세션이 폐기되었는지 알려 준다.
     *
     * @return true if disposed / 폐기되었으면 true
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Waits until the document thread has no queued work (for tests and diagnostics).
     * <p>
     * 문서 스레드의 대기 작업이 없어질 때까지 기다린다(시험과 진단용).
     *
     * @param timeoutMs maximum wait / 최대 대기 시간(밀리초)
     * @return true if idle in time / 제때 비었으면 true
     */
    public boolean awaitIdle(long timeoutMs) {
        return nav.awaitIdle(timeoutMs);
    }

    /**
     * Queues internal DOM/style/layout work on this session's single document thread. Work must check that its page is
     * still current before reading the DOM and before publishing results; it must never wait for the EDT.
     * <p>
     * 내부 DOM·스타일·배치 작업을 세션의 단일 문서 스레드에 넣는다. 작업은 DOM 접근 전과 게시 전에 자기 페이지가 여전히 현재인지 확인하며 EDT를 기다리면 안 된다.
     * @param work internal work, not null / 내부 작업(null 불가)
     * @return false after disposal or executor shutdown / 폐기 또는 실행기 종료 후에는 false
     * @throws IllegalArgumentException for null / null이면
     */
    public boolean submitDocumentTask(Runnable work) {
        if (work == null) {
            throw new IllegalArgumentException("document work must not be null");
        }
        return !disposed && nav.submitDocumentTask(work);
    }

    /**
     * Tells whether the calling thread is the document thread of this component, which owns the DOM of every document of the page.
     * <p>
     * 호출한 스레드가 이 컴포넌트의 문서 스레드(페이지의 모든 문서의 DOM 을 소유함)인지 알려 준다.
     *
     * @return true on the document thread / 문서 스레드이면 true
     */
    boolean isDocumentThread() {
        return nav.isDocumentThread();
    }

    /**
     * Changes the status text and progress and notifies the observer of real changes.
     * <p>
     * 상태 메시지와 진행률을 바꾸고 실제로 달라진 것을 관찰자에게 알린다.
     *
     * @param text  new status text / 새 상태 메시지
     * @param value new progress / 새 진행률
     */
    private void updateStatus(final String text, final int value) {
        final String oldText;
        final String visibleText;
        final int oldValue;
        synchronized (this) {
            oldText = getStatusText();
            oldValue = progress;
            statusText = text;
            progress = value;
            visibleText = getStatusText();
        }
        final SessionListener l = listener;
        if (l == null) {
            return;
        }
        fireStatusText(oldText, visibleText);
        if (oldValue != value) {
            SwingUtilities.invokeLater(new Runnable() {
                /**
                 * Notifies the progress change on the EDT.
                 * <p>
                 * EDT 에서 진행률 변경을 알린다.
                 */
                public void run() {
                    l.progressChanged(oldValue, value);
                }
            });
        }
    }

    /**
     * Announces a visible status change on the EDT.
     * <p>보이는 상태 변경을 EDT에서 알린다.
     * @param before previous visible status / 이전 표시 상태
     * @param after new visible status / 새 표시 상태
     */
    private void fireStatusText(final String before, final String after) {
        final SessionListener observer = listener;
        if (observer != null && !before.equals(after)) {
            SwingUtilities.invokeLater(new Runnable() {
                /** Delivers the visible text without waiting for Swing. / Swing을 기다리지 않고 표시 문자열을 전달한다. */
                public void run() {
                    observer.statusTextChanged(before, after);
                }
            });
        }
    }

    /**
     * Notifies the observer that a page was confirmed, plus URL and title changes.
     * <p>
     * 관찰자에게 페이지 확정과 URL, 제목 변경을 알린다.
     *
     * @param old      the page that was replaced / 교체된 이전 페이지
     * @param snapshot the confirmed page / 확정된 페이지
     */
    private void firePageConfirmed(final PageSnapshot old, final PageSnapshot snapshot) {
        final SessionListener l = listener;
        if (l == null) {
            return;
        }
        final String oldUrl = old.getUrl();
        final String oldTitle = old.getTitle();
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Notifies the page, URL and title changes on the EDT.
             * <p>
             * EDT 에서 페이지, URL, 제목 변경을 알린다.
             */
            public void run() {
                l.pageConfirmed(oldUrl, snapshot);
                if (!oldUrl.equals(snapshot.getUrl())) {
                    l.pageChanged(oldUrl, snapshot.getUrl());
                }
                if (!oldTitle.equals(snapshot.getTitle())) {
                    l.titleChanged(oldTitle, snapshot.getTitle());
                }
            }
        });
    }
}
